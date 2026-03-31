package com.neuroflow.app

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.neuroflow.app.presentation.launcher.data.AppRepository
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.worker.DistractionSyncWorker
import com.neuroflow.app.worker.createNotificationChannels
import com.neuroflow.app.worker.scheduleNotificationWorkers
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NeuroFlowApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var userPreferencesDataStore: UserPreferencesDataStore

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    override fun onCreate() {
        super.onCreate()

        // Enable StrictMode in debug builds to catch main-thread IO violations
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyDeath()  // Crash on violations to ensure they're fixed
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

        createNotificationChannels(this)
        scheduleDailyWorkers()

        // Pre-warm AppRepository on IO thread
        applicationScope.launch {
            appRepository.loadAll()
        }
    }

    private fun scheduleDailyWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Notification workers (daily plan, streak, escalation) follow user-configured hours/toggles.
        applicationScope.launch {
            val prefs = userPreferencesDataStore.preferencesFlow.first()
            scheduleNotificationWorkers(this@NeuroFlowApplication, prefs)
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

        // DistractionSyncWorker — runs once daily to refresh per-task distraction scores
        // Silently skips if PACKAGE_USAGE_STATS permission is not granted
        DistractionSyncWorker.schedulePeriodic(this)
    }
}
