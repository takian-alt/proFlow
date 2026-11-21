package com.neuroflow.app.presentation.v2.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.assistant.CheckInConfigEntry
import com.neuroflow.app.domain.assistant.CheckInDailyRecord
import com.neuroflow.app.domain.assistant.CheckInDailyStatus
import com.neuroflow.app.domain.assistant.CheckInLogType
import com.neuroflow.app.domain.assistant.DailyLogbookEntry
import com.neuroflow.app.domain.assistant.ProactiveCheckInCoordinator
import com.neuroflow.app.domain.assistant.SmartAssistantCodecs
import com.neuroflow.app.domain.assistant.WellnessNudgeEvaluator
import com.neuroflow.app.domain.assistant.WellnessNudgeProposal
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.StimulantType
import com.neuroflow.app.domain.time.ClockTicker
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class SmartAssistantUiState(
    val dueCheckIn: CheckInConfigEntry? = null,
    val activeWellnessNudge: WellnessNudgeProposal? = null,
    val todayLogbook: List<DailyLogbookEntry> = emptyList(),
    val todayHydrationCount: Int = 0,
    val preferences: UserPreferences = UserPreferences(),
    val toastMessage: String? = null
)

@HiltViewModel
class SmartAssistantViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val sleepLogRepository: SleepLogRepository? = null,
    private val sessionRepository: SessionRepository? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val clockTicker: ClockTicker? = null
) : ViewModel() {

    private val _sessionDismissedTypes = MutableStateFlow<Set<CheckInLogType>>(emptySet())
    private val _toastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SmartAssistantUiState> = combine(
        preferencesDataStore.preferencesFlow.onEmpty { emit(UserPreferences()) },
        (sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())).onEmpty { emit(emptyList()) },
        (sessionRepository?.observeAll() ?: flowOf(emptyList())).onEmpty { emit(emptyList()) },
        FocusService.activeSession,
        combine(
            (clockTicker?.currentMinuteTick ?: flowOf(0L)).onEmpty { emit(0L) },
            _sessionDismissedTypes,
            _toastMessage
        ) { tick, dismissed, msg -> Triple(tick, dismissed, msg) }
    ) { prefs, sleepLogs, sessions, activeFocus, (_, dismissedTypes, toast) ->
        val nowMillis = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val epochDay = SmartAssistantCodecs.currentEpochDay(nowMillis, zoneId)
        val isFocusRunning = activeFocus != null && !activeFocus.isPaused

        val rawDue = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = prefs,
            nowMillis = nowMillis,
            isFocusSessionActive = isFocusRunning,
            sleepLogs = sleepLogs,
            zoneId = zoneId
        )
        val dueCheckIn = rawDue?.takeUnless { it.type in dismissedTypes }

        val todayLogbook = ProactiveCheckInCoordinator.buildTodayLogbookEntries(
            prefs = prefs,
            nowMillis = nowMillis,
            sleepLogs = sleepLogs,
            zoneId = zoneId
        )

        val (snapshot, _) = computeEnergyUiState(
            circadianEnergyService = circadianEnergyService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            prefs = prefs,
            sleepLogs = sleepLogs,
            nowMillis = nowMillis
        )

        // Estimate continuous focus minutes in the last 2 hours
        val twoHoursAgo = nowMillis - 2 * 3600_000L
        val recentFocusMinutes = sessions
            .filter { (it.endedAt ?: nowMillis) >= twoHoursAgo }
            .sumOf { it.trackedMinutes(nowMillis).toInt() }

        val wellnessNudge = if (dueCheckIn == null) {
            WellnessNudgeEvaluator.evaluateWellnessNudge(
                prefs = prefs,
                usableEnergy = snapshot.usableEnergy,
                phase = snapshot.phase,
                isCrashWindow = snapshot.phase == EnergyPhase.CRASH,
                continuousFocusMinutes = recentFocusMinutes,
                nowMillis = nowMillis,
                isFocusSessionActive = isFocusRunning
            )
        } else {
            null
        }

        val hydrationCount = if (prefs.todayHydrationEpochDay == epochDay) prefs.todayHydrationCount else 0

        SmartAssistantUiState(
            dueCheckIn = dueCheckIn,
            activeWellnessNudge = wellnessNudge,
            todayLogbook = todayLogbook,
            todayHydrationCount = hydrationCount,
            preferences = prefs,
            toastMessage = toast
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SmartAssistantUiState()
    )

    fun logCheckInAtTime(
        type: CheckInLogType,
        occurredAtMillis: Long,
        intensityOrSubtype: String = "Normal",
        sleepHours: Float = 7.5f
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val epochDay = SmartAssistantCodecs.currentEpochDay(now, zoneId)
            val clampedTs = occurredAtMillis.coerceAtMost(now)

            when (type) {
                CheckInLogType.BREAKFAST, CheckInLogType.LUNCH, CheckInLogType.SNACKS, CheckInLogType.DINNER -> {
                    val intensity = when (intensityOrSubtype.lowercase()) {
                        "light" -> 0.65
                        "heavy" -> 1.35
                        else -> 1.0
                    }
                    val duration = if (type == CheckInLogType.SNACKS) 15 else 30
                    preferencesDataStore.recordPhysiologicalEvent(
                        type = PhysiologicalEventType.MEAL,
                        timestampMillis = clampedTs,
                        durationMinutes = duration,
                        intensity = intensity
                    )
                }
                CheckInLogType.WORKOUT -> {
                    val intensity = when (intensityOrSubtype.lowercase()) {
                        "light" -> 0.7
                        "intense", "heavy" -> 1.3
                        else -> 1.0
                    }
                    preferencesDataStore.recordPhysiologicalEvent(
                        type = PhysiologicalEventType.WORKOUT,
                        timestampMillis = clampedTs,
                        durationMinutes = 40,
                        intensity = intensity
                    )
                }
                CheckInLogType.NAP -> {
                    preferencesDataStore.recordPhysiologicalEvent(
                        type = PhysiologicalEventType.NAP,
                        timestampMillis = clampedTs,
                        durationMinutes = 20,
                        intensity = 1.0
                    )
                }
                CheckInLogType.CAFFEINE -> {
                    val stimType = when (intensityOrSubtype.lowercase()) {
                        "tea" -> StimulantType.GREEN_TEA_L_THEANINE
                        "med ir" -> StimulantType.PRESCRIPTION_STIMULANT_IR
                        "med xr" -> StimulantType.PRESCRIPTION_STIMULANT_XR
                        else -> StimulantType.CAFFEINE
                    }
                    preferencesDataStore.recordStimulantDose(
                        type = stimType,
                        timestampMillis = clampedTs
                    )
                }
                CheckInLogType.SLEEP_RECAP -> {
                    val durMs = (sleepHours.coerceIn(3f, 12f) * 3600_000L).toLong()
                    val wakeMillis = clampedTs
                    val bedMillis = wakeMillis - durMs
                    sleepLogRepository?.addLog(
                        startAt = bedMillis,
                        endAt = wakeMillis,
                        source = "MANUAL",
                        notes = "Logged via Assistant Check-In (${String.format("%.1f", sleepHours)}h)"
                    )
                }
            }

            val detail = when (type) {
                CheckInLogType.SLEEP_RECAP -> "${String.format("%.1f", sleepHours)}h"
                else -> intensityOrSubtype
            }
            preferencesDataStore.updateDailyCheckInRecord(
                epochDay = epochDay,
                record = CheckInDailyRecord(
                    type = type,
                    status = CheckInDailyStatus.LOGGED,
                    loggedAtMillis = clampedTs,
                    detailLabel = detail
                ),
                promptHandledAtMillis = now
            )
            _sessionDismissedTypes.value = _sessionDismissedTypes.value - type
            val formattedTime = SmartAssistantCodecs.formatTimestampShort(clampedTs, zoneId)
            _toastMessage.value = "${type.emoji} ${type.displayName} logged for $formattedTime"
        }
    }

    fun snoozeCheckInUntil(type: CheckInLogType, targetMillis: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val epochDay = SmartAssistantCodecs.currentEpochDay(now, zoneId)
            val safeTarget = maxOf(targetMillis, now + 5 * 60_000L)
            preferencesDataStore.updateDailyCheckInRecord(
                epochDay = epochDay,
                record = CheckInDailyRecord(
                    type = type,
                    status = CheckInDailyStatus.SNOOZED,
                    snoozedUntilMillis = safeTarget
                ),
                promptHandledAtMillis = now
            )
            _sessionDismissedTypes.value = _sessionDismissedTypes.value - type
            val formattedTime = SmartAssistantCodecs.formatTimestampShort(safeTarget, zoneId)
            _toastMessage.value = "Got it — I'll ask about ${type.displayName.lowercase()} after $formattedTime"
        }
    }

    fun skipCheckInToday(type: CheckInLogType) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val epochDay = SmartAssistantCodecs.currentEpochDay(now)
            preferencesDataStore.updateDailyCheckInRecord(
                epochDay = epochDay,
                record = CheckInDailyRecord(
                    type = type,
                    status = CheckInDailyStatus.SKIPPED
                ),
                promptHandledAtMillis = now
            )
            _sessionDismissedTypes.value = _sessionDismissedTypes.value + type
            _toastMessage.value = "Skipped ${type.displayName.lowercase()} for today"
        }
    }

    fun dismissCheckInTemporarily(type: CheckInLogType) {
        _sessionDismissedTypes.value = _sessionDismissedTypes.value + type
    }

    fun logHydrationDrink() {
        viewModelScope.launch {
            preferencesDataStore.recordHydrationDrink()
            _toastMessage.value = "💧 Hydration logged — stay sharp!"
        }
    }

    fun dismissWellnessNudge() {
        viewModelScope.launch {
            preferencesDataStore.dismissWellnessNudge()
        }
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }
}
