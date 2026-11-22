package com.neuroflow.app.presentation.v2.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.assistant.ActiveBreakTimerState
import com.neuroflow.app.domain.assistant.BurnoutGuardEngine
import com.neuroflow.app.domain.assistant.BurnoutRiskState
import com.neuroflow.app.domain.assistant.CheckInConfigEntry
import com.neuroflow.app.domain.assistant.CheckInDailyRecord
import com.neuroflow.app.domain.assistant.CheckInDailyStatus
import com.neuroflow.app.domain.assistant.CheckInLogType
import com.neuroflow.app.domain.assistant.DailyLogbookEntry
import com.neuroflow.app.domain.assistant.ProactiveCheckInCoordinator
import com.neuroflow.app.domain.assistant.RestBreakSource
import com.neuroflow.app.domain.assistant.RestCheckInProposal
import com.neuroflow.app.domain.assistant.SmartAssistantCodecs
import com.neuroflow.app.domain.assistant.WellnessNudgeEvaluator
import com.neuroflow.app.domain.assistant.WellnessNudgeProposal
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.StimulantType
import com.neuroflow.app.domain.time.ClockTicker
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

data class SmartAssistantUiState(
    val dueCheckIn: CheckInConfigEntry? = null,
    val activeRestCheckIn: RestCheckInProposal? = null,
    val burnoutRiskState: BurnoutRiskState = BurnoutRiskState(),
    val breakTimer: ActiveBreakTimerState = ActiveBreakTimerState(),
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
    private val sleepPressureRepository: SleepPressureRepository? = null,
    private val sessionRepository: SessionRepository? = null,
    private val taskRepository: TaskRepository? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val clockTicker: ClockTicker? = null
) : ViewModel() {

    private val _sessionDismissedTypes = MutableStateFlow<Set<CheckInLogType>>(emptySet())
    private val _toastMessage = MutableStateFlow<String?>(null)
    private val _breakTimer = MutableStateFlow(ActiveBreakTimerState())
    private var breakTimerJob: Job? = null

    private val tasksBundleFlow = combine(
        (taskRepository?.observeActiveTasks() ?: flowOf(emptyList())).onEmpty { emit(emptyList()) },
        (taskRepository?.observeCompletedTasks() ?: flowOf(emptyList())).onEmpty { emit(emptyList()) }
    ) { active, completed -> active to completed }

    val uiState: StateFlow<SmartAssistantUiState> = combine(
        preferencesDataStore.preferencesFlow.onEmpty { emit(UserPreferences()) },
        (sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())).onEmpty { emit(emptyList()) },
        combine(
            (sessionRepository?.observeAll() ?: flowOf(emptyList())).onEmpty { emit(emptyList()) },
            tasksBundleFlow
        ) { sessions, tasksPair -> Triple(sessions, tasksPair.first, tasksPair.second) },
        combine(
            FocusService.activeSession,
            _breakTimer
        ) { activeFocus, breakTimer -> activeFocus to breakTimer },
        combine(
            (clockTicker?.currentMinuteTick ?: flowOf(0L)).onEmpty { emit(0L) },
            _sessionDismissedTypes,
            _toastMessage
        ) { tick, dismissed, msg -> Triple(tick, dismissed, msg) }
    ) { prefs, sleepLogs, (sessions, activeTasks, completedTasks), (activeFocus, breakTimer), (_, dismissedTypes, toast) ->
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

        val burnoutRiskState = BurnoutGuardEngine.evaluateBurnoutRisk(
            prefs = prefs,
            sessions = sessions,
            completedTasks = completedTasks,
            activeTasks = activeTasks,
            snapshot = snapshot,
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        val activeRestCheckIn = if (dueCheckIn == null) {
            BurnoutGuardEngine.evaluatePersistentRestCheckIn(
                prefs = prefs,
                riskState = burnoutRiskState,
                nowMillis = nowMillis,
                isFocusSessionActive = isFocusRunning,
                isBreakTimerActive = breakTimer.isActive
            )
        } else {
            null
        }

        val twoHoursAgo = nowMillis - 2 * 3600_000L
        val recentFocusMinutes = sessions
            .filter { (it.endedAt ?: nowMillis) >= twoHoursAgo }
            .sumOf { it.trackedMinutes(nowMillis).toInt() }

        val wellnessNudge = if (dueCheckIn == null && activeRestCheckIn == null && !breakTimer.isActive) {
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
            activeRestCheckIn = activeRestCheckIn,
            burnoutRiskState = burnoutRiskState,
            breakTimer = breakTimer,
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

    /**
     * Logs an exact bedtime -> wake time sleep interval through the canonical SleepPressureRepository
     * pipeline, replacing any overlapping synthetic AUTO_DEFAULT or prior same-night manual log so
     * Sleep History, Borbély pressure, and Vault Logbook stay 100% consistent.
     */
    fun logSleepInterval(bedtimeMillis: Long, wakeMillis: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val epochDay = SmartAssistantCodecs.currentEpochDay(now, zoneId)
            val safeBed = minOf(bedtimeMillis, wakeMillis)
            val safeWake = maxOf(bedtimeMillis, wakeMillis).coerceAtMost(now + 10 * 60_000L)
            val durationMinutes = ((safeWake - safeBed) / 60_000L).toInt()
            if (durationMinutes < 15 || durationMinutes > SleepPressureRepository.MAX_SLEEP_LOG_DURATION_MINUTES) {
                _toastMessage.value = "Sleep duration must be between 15m and 16h"
                return@launch
            }

            // Clear any overlapping manual or auto-default logs for the same sleep window to prevent duplicates/mismatches
            val overlapping = sleepLogRepository?.getOverlapping(safeBed - 30 * 60_000L, safeWake + 30 * 60_000L).orEmpty()
            overlapping.forEach { existing ->
                if (existing.source == "AUTO_DEFAULT" || existing.source == "MANUAL") {
                    if (sleepPressureRepository != null) {
                        sleepPressureRepository.deleteSleepLog(existing.id)
                    } else {
                        sleepLogRepository?.deleteById(existing.id)
                    }
                }
            }

            if (sleepPressureRepository != null) {
                val res = sleepPressureRepository.addSleepLog(safeBed, safeWake)
                if (res is SleepPressureRepository.AddSleepLogResult.TooLong) {
                    _toastMessage.value = "Sleep log cannot exceed 16 hours"
                    return@launch
                }
            } else {
                sleepLogRepository?.addLog(
                    startAt = safeBed,
                    endAt = safeWake,
                    source = "MANUAL",
                    notes = "Logged via Smart Check-In"
                )
            }

            val bedLabel = SmartAssistantCodecs.formatTimestampShort(safeBed, zoneId)
            val wakeLabel = SmartAssistantCodecs.formatTimestampShort(safeWake, zoneId)
            val durLabel = SmartAssistantCodecs.formatDurationCompact(durationMinutes)

            preferencesDataStore.updateDailyCheckInRecord(
                epochDay = epochDay,
                record = CheckInDailyRecord(
                    type = CheckInLogType.SLEEP_RECAP,
                    status = CheckInDailyStatus.LOGGED,
                    loggedAtMillis = safeWake,
                    detailLabel = "$bedLabel → $wakeLabel ($durLabel)"
                ),
                promptHandledAtMillis = now
            )
            _sessionDismissedTypes.value = _sessionDismissedTypes.value - CheckInLogType.SLEEP_RECAP
            _toastMessage.value = "🌙 Sleep logged: $bedLabel → $wakeLabel ($durLabel)"
        }
    }

    fun logCheckInAtTime(
        type: CheckInLogType,
        occurredAtMillis: Long,
        intensityOrSubtype: String = "Normal",
        durationMinutesOverride: Int? = null
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
                    val duration = durationMinutesOverride ?: if (type == CheckInLogType.SNACKS) 15 else 30
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
                    val duration = (durationMinutesOverride ?: 40).coerceIn(10, 180)
                    preferencesDataStore.recordPhysiologicalEvent(
                        type = PhysiologicalEventType.WORKOUT,
                        timestampMillis = clampedTs,
                        durationMinutes = duration,
                        intensity = intensity
                    )
                }
                CheckInLogType.NAP -> {
                    val duration = (durationMinutesOverride ?: 20).coerceIn(10, 120)
                    preferencesDataStore.recordPhysiologicalEvent(
                        type = PhysiologicalEventType.NAP,
                        timestampMillis = clampedTs,
                        durationMinutes = duration,
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
                    // Fallback if called directly; prefer logSleepInterval(bedtimeMillis, wakeMillis)
                    val durMs = ((durationMinutesOverride ?: 450).coerceIn(15, 960)) * 60_000L
                    logSleepInterval(bedtimeMillis = clampedTs - durMs, wakeMillis = clampedTs)
                    return@launch
                }
            }

            val detail = when {
                type.supportsDuration && durationMinutesOverride != null -> "${durationMinutesOverride}m · $intensityOrSubtype"
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

    fun logRestBreak(
        durationMinutes: Int,
        source: RestBreakSource = RestBreakSource.MANUAL_CONFIRM
    ) {
        val safeMins = durationMinutes.coerceIn(1, 240)
        breakTimerJob?.cancel()
        breakTimerJob = null
        _breakTimer.value = ActiveBreakTimerState()
        viewModelScope.launch {
            preferencesDataStore.recordRestBreak(
                durationMinutes = safeMins,
                source = source
            )
            _toastMessage.value = "🌿 ${safeMins}m rest logged — burnout pressure eased!"
        }
    }

    fun snoozeRestCheckInNotYet(followUpMinutes: Int = BurnoutGuardEngine.REST_CHECK_IN_FOLLOW_UP_MINUTES) {
        val mins = followUpMinutes.coerceIn(20, 30)
        viewModelScope.launch {
            preferencesDataStore.snoozeRestCheckIn(followUpMinutes = mins)
            _toastMessage.value = "Got it — I'll check in again in ${mins}m"
        }
    }

    fun startBreakTimer(durationMinutes: Int) {
        val safeMins = durationMinutes.coerceIn(1, 60)
        val totalSecs = safeMins * 60
        breakTimerJob?.cancel()
        _breakTimer.value = ActiveBreakTimerState(
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs,
            isRunning = true,
            isPaused = false,
            startedAtMillis = System.currentTimeMillis()
        )
        _toastMessage.value = "🌿 ${safeMins}m break timer started — take a breather"
        breakTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                val current = _breakTimer.value
                if (!current.isActive) break
                if (current.isPaused) continue
                val nextRemaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                if (nextRemaining == 0) {
                    _breakTimer.value = ActiveBreakTimerState()
                    preferencesDataStore.recordRestBreak(
                        durationMinutes = safeMins,
                        source = RestBreakSource.BREAK_TIMER
                    )
                    _toastMessage.value = "🌿 ${safeMins}m break complete — rest logged & energy refreshed!"
                    break
                } else {
                    _breakTimer.value = current.copy(remainingSeconds = nextRemaining)
                }
            }
        }
    }

    fun togglePauseBreakTimer() {
        val current = _breakTimer.value
        if (!current.isActive) return
        _breakTimer.value = current.copy(
            isPaused = !current.isPaused,
            isRunning = current.isPaused
        )
    }

    fun finishBreakTimerEarly() {
        val current = _breakTimer.value
        breakTimerJob?.cancel()
        breakTimerJob = null
        _breakTimer.value = ActiveBreakTimerState()
        val elapsedMins = ((current.totalSeconds - current.remainingSeconds) / 60.0).let {
            if (it >= 0.75) kotlin.math.max(1, it.toInt()) else 0
        }
        if (elapsedMins > 0) {
            viewModelScope.launch {
                preferencesDataStore.recordRestBreak(
                    durationMinutes = elapsedMins,
                    source = RestBreakSource.BREAK_TIMER
                )
                _toastMessage.value = "🌿 ${elapsedMins}m rest logged — welcome back!"
            }
        } else {
            _toastMessage.value = "Break timer ended"
        }
    }

    fun cancelBreakTimer() {
        breakTimerJob?.cancel()
        breakTimerJob = null
        _breakTimer.value = ActiveBreakTimerState()
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }
}
