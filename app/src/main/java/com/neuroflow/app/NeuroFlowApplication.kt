package com.neuroflow.app

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.neuroflow.app.data.local.DatabaseCleaner
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.kiosk.DeviceOwnerKioskManager
import com.neuroflow.app.feature.hyperfocus.data.AllowlistMigrationRunner
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.worker.ScheduleAutoTasksWorker
import com.neuroflow.app.worker.DistractionSyncWorker
import com.neuroflow.app.worker.createNotificationChannels
import com.neuroflow.app.worker.scheduleNotificationWorkers
import com.neuroflow.app.worker.scheduleSleepPressureRefreshWorker
import com.neuroflow.app.worker.EnergyTelemetryCleanupScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NeuroFlowApplication : Application(), Configuration.Provider {

    companion object {
        private const val INSTALL_MARKER_FILE = "install_marker_v1"
    }

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var userPreferencesDataStore: UserPreferencesDataStore
    @Inject lateinit var hyperFocusDataStore: HyperFocusDataStore
    @Inject lateinit var hyperFocusManager: HyperFocusManager
    @Inject lateinit var allowlistMigrationRunner: AllowlistMigrationRunner
    @Inject lateinit var databaseCleaner: DatabaseCleaner
    @Inject lateinit var energyTelemetryCleanupScheduler: EnergyTelemetryCleanupScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    override fun onCreate() {
        super.onCreate()

        // Global crash logging so any startup exceptions are preserved in logcat and a file
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("NeuroFlowCrash", "FATAL CRASH in thread: ${thread.name}", throwable)
            runCatching {
                val crashFile = File(filesDir, "last_crash.txt")
                crashFile.writeText("Thread: ${thread.name}\nTimestamp: ${System.currentTimeMillis()}\nStackTrace:\n" + throwable.stackTraceToString())
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize Firebase safely
        runCatching {
            com.google.firebase.FirebaseApp.initializeApp(this)
        }

        // Enable StrictMode logging in debug builds to monitor main-thread IO violations
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        applicationScope.launch(Dispatchers.IO) {
            val firstLaunchAfterInstall = markFirstLaunchAfterInstall()
            databaseCleaner.enforceRestoreVersionPolicy(firstLaunchAfterInstall)

            // Seed the persistent tag catalog from all existing tasks after restore version policy is enforced.
            val allTags = taskRepository.getAllTasks()
                .flatMap { task -> task.tags.split(",") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
            userPreferencesDataStore.mergeTagCatalog(allTags)
        }

        createNotificationChannels(this)
        scheduleDailyWorkers()

        DeviceOwnerKioskManager.migrateStrictModeDefault(this)
        DeviceOwnerKioskManager.enableHybridProtection(this)

        // Re-sync Hyper Focus restrictions on process start.
        // This keeps them consistent even after process death/restart.
        applicationScope.launch {
            val hfPrefs = hyperFocusDataStore.current()
            DeviceOwnerKioskManager.setHyperFocusSelfProtection(this@NeuroFlowApplication, hfPrefs.isActive)
            hyperFocusManager.resyncSuspension(hfPrefs.isActive)
        }

        // One-shot denylist -> allowlist, before any surface reads the tiers. runCatching because
        // launchableApps() throws on a paused work profile, and a half-migrated install must still boot.
        applicationScope.launch {
            runCatching { allowlistMigrationRunner.runIfNeeded() }
                .onFailure { android.util.Log.w("NeuroFlowApp", "Allowlist migration did not complete", it) }
        }
    }

    private fun markFirstLaunchAfterInstall(): Boolean {
        val marker = File(noBackupFilesDir, INSTALL_MARKER_FILE)
        if (marker.exists()) return false
        return runCatching {
            marker.parentFile?.mkdirs()
            marker.writeText("1")
            true
        }.getOrDefault(false)
    }

    private fun scheduleDailyWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Notification workers (daily plan, streak, escalation) follow user-configured hours/toggles.
        applicationScope.launch {
            userPreferencesDataStore.preferencesFlow
                .distinctUntilChanged { old, new ->
                    old.dailyPlanNotificationsEnabled == new.dailyPlanNotificationsEnabled &&
                    old.dailyPlanNotificationHour == new.dailyPlanNotificationHour &&
                    old.streakNotificationsEnabled == new.streakNotificationsEnabled &&
                    old.streakCheckNotificationHour == new.streakCheckNotificationHour &&
                    old.deadlineEscalationNotificationsEnabled == new.deadlineEscalationNotificationsEnabled
                }
                .collect { prefs ->
                    scheduleNotificationWorkers(this@NeuroFlowApplication, prefs)
                }
        }

        // FocusWidgetUpdateWorker — runs every 15 minutes to keep the home screen widget fresh
        val widgetUpdateRequest = PeriodicWorkRequestBuilder<com.neuroflow.app.worker.FocusWidgetUpdateWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            "focus_widget_update",
            ExistingPeriodicWorkPolicy.KEEP,
            widgetUpdateRequest
        )

        // ScheduleAutoTasksWorker — runs periodically (configurable throttle) to auto-schedule eligible tasks
        // Gated by autoSchedulingEnabled preference
        applicationScope.launch {
            userPreferencesDataStore.preferencesFlow
                .distinctUntilChanged { old, new ->
                    old.autoSchedulingEnabled == new.autoSchedulingEnabled &&
                    old.autoSchedulingBackgroundThrottleMinutes == new.autoSchedulingBackgroundThrottleMinutes
                }
                .collect { prefs ->
                if (prefs.autoSchedulingEnabled) {
                    val throttleMinutes = prefs.autoSchedulingBackgroundThrottleMinutes
                    val autoScheduleRequest = ScheduleAutoTasksWorker.buildPeriodicWorkRequest(throttleMinutes)
                    workManager.enqueueUniquePeriodicWork(
                        ScheduleAutoTasksWorker.WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        autoScheduleRequest
                    )
                } else {
                    workManager.cancelUniqueWork(ScheduleAutoTasksWorker.WORK_NAME)
                }
            }
        }


        // DistractionSyncWorker — runs once daily to refresh per-task distraction scores
        // Silently skips if PACKAGE_USAGE_STATS permission is not granted
        DistractionSyncWorker.schedulePeriodic(this)

        // EnergyTelemetryCleanupWorker — runs once daily to enforce retention policy on prediction database
        energyTelemetryCleanupScheduler.scheduleCleanup()

        // SleepPressureRefreshWorker — runs once daily to refresh sleep pressure and create automatic fallback sleep logs
        applicationScope.launch {
            userPreferencesDataStore.preferencesFlow
                .distinctUntilChanged { old, new ->
                    old.wakeUpHour == new.wakeUpHour &&
                    old.sleepHour == new.sleepHour &&
                    old.autoFallbackSleepInsertionEnabled == new.autoFallbackSleepInsertionEnabled
                }
                .collect { prefs ->
                    scheduleSleepPressureRefreshWorker(this@NeuroFlowApplication, prefs)
                }
        }
    }
}
