package com.neuroflow.app.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.neuroflow.app.data.local.DatabaseCleaner
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.AppTheme
import com.neuroflow.app.domain.repository.EnergyMetricsRepository
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.worker.scheduleNotificationWorkers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sleepPressureRepository: SleepPressureRepository,
    private val energyMetricsRepository: EnergyMetricsRepository,
    private val databaseCleaner: DatabaseCleaner,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : ViewModel() {

    data class LaunchableApp(val packageName: String, val label: String)

    private val _launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val launchableApps: StateFlow<List<LaunchableApp>> = _launchableApps.asStateFlow()

    private val _launchableAppsLoading = MutableStateFlow(false)
    val launchableAppsLoading: StateFlow<Boolean> = _launchableAppsLoading.asStateFlow()

    /**
     * Enumerating + labelling every launcher activity is PackageManager binder work over
     * hundreds of apps, so it only happens when the distracting-app picker is opened. A failed or
     * empty read leaves the list unloadable, so it is retried on the next open instead of being
     * remembered as "already loaded".
     */
    fun loadLaunchableApps() {
        if (_launchableApps.value.isNotEmpty() || _launchableAppsLoading.value) return
        _launchableAppsLoading.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                @Suppress("DEPRECATION")
                val packages = pm.queryIntentActivities(intent, 0)
                    .asSequence()
                    .map { it.activityInfo.packageName }
                    .filter { it != context.packageName }
                    .distinct()
                    .map { pkg ->
                        val label = runCatching {
                            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                        }.getOrDefault(pkg)
                        LaunchableApp(pkg, label)
                    }
                    .sortedBy { it.label.lowercase() }
                    .toList()
                _launchableApps.value = packages
            } finally {
                _launchableAppsLoading.value = false
            }
        }
    }

    val preferences: StateFlow<UserPreferences> = preferencesDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val sleepLogs: StateFlow<List<SleepLogEntity>> = sleepPressureRepository.observeSleepLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val circadianSummary: StateFlow<DailyCircadianSummary?> = combine(
        preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
            old.wakeUpHour == new.wakeUpHour
        },
        sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())
    ) { prefs, sleepLogs ->
        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val zonedDateTime = Instant.ofEpochMilli(now).atZone(zoneId)
        var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
        if (anchor.toInstant().toEpochMilli() > now) {
            anchor = anchor.minusDays(1)
        }
        val baseWakeMillis = anchor.toInstant().toEpochMilli()

        val signal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )
        val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480

        val naps = signal?.daytimeNaps ?: emptyList()
        if (naps.isNotEmpty()) {
            circadianEnergyService.computeDailySummary(
                timestampMillis = now,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                zoneId = zoneId,
                naps = naps
            )
        } else {
            circadianEnergyService.computeDailySummary(
                timestampMillis = now,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                zoneId = zoneId
            )
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _sleepLogInputError = MutableStateFlow<String?>(null)
    val sleepLogInputError: StateFlow<String?> = _sleepLogInputError.asStateFlow()

    init {
        refreshSleepPressureNow()
    }

    fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            // Transform the value the store hands us inside its atomic edit rather than
            // rebuilding from `preferences.value`: that mirror lags behind writes made by
            // other screens, and `updatePreferences` persists every key it knows about.
            var committed: UserPreferences? = null
            var previous: UserPreferences? = null
            preferencesDataStore.updatePreferences { current ->
                val updated = update(current)
                previous = current
                committed = updated
                updated
            }
            val after = committed ?: return@launch
            val before = previous ?: return@launch

            val scheduleChanged =
                before.dailyPlanNotificationsEnabled != after.dailyPlanNotificationsEnabled ||
                    before.streakNotificationsEnabled != after.streakNotificationsEnabled ||
                    before.deadlineEscalationNotificationsEnabled != after.deadlineEscalationNotificationsEnabled ||
                    before.dailyPlanNotificationHour != after.dailyPlanNotificationHour ||
                    before.streakCheckNotificationHour != after.streakCheckNotificationHour

            if (scheduleChanged) {
                scheduleNotificationWorkers(context, after)
            }

            if (before.autonomyNudgeNotificationsEnabled && !after.autonomyNudgeNotificationsEnabled) {
                WorkManager.getInstance(context).cancelAllWorkByTag(AutonomyNudgeEngine.globalTag())
            }
            if (before.deadlineReminderNotificationsEnabled && !after.deadlineReminderNotificationsEnabled) {
                WorkManager.getInstance(context).cancelAllWorkByTag("task_reminder_all")
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        updatePreferences { it.copy(theme = theme) }
    }

    fun refreshSleepPressureNow() {
        viewModelScope.launch {
            sleepPressureRepository.refreshCurrentPressure()
        }
    }

    fun addSleepLog(startAtMillis: Long, endAtMillis: Long) {
        viewModelScope.launch {
            when (sleepPressureRepository.addSleepLog(startAtMillis, endAtMillis)) {
                is SleepPressureRepository.AddSleepLogResult.Added -> {
                    _sleepLogInputError.value = null
                }
                SleepPressureRepository.AddSleepLogResult.OverlapsExisting -> {
                    _sleepLogInputError.value = "This sleep period overlaps an existing sleep log."
                }
                is SleepPressureRepository.AddSleepLogResult.TooLong -> {
                    _sleepLogInputError.value = "Sleep log is too long. Max allowed is 16 hours."
                }
            }
        }
    }

    fun clearSleepLogInputError() {
        _sleepLogInputError.value = null
    }

    fun deleteSleepLog(logId: String) {
        viewModelScope.launch {
            sleepPressureRepository.deleteSleepLog(logId)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            databaseCleaner.clearAllDataAndInvalidateBackup()
            scheduleNotificationWorkers(context, UserPreferences())
            WorkManager.getInstance(context).cancelAllWorkByTag(AutonomyNudgeEngine.globalTag())
            WorkManager.getInstance(context).cancelAllWorkByTag("task_reminder_all")
            _sleepLogInputError.value = null
        }
    }

    fun clearEnergyTelemetry() {
        viewModelScope.launch {
            energyMetricsRepository.clearAllPredictions()
        }
    }
}
