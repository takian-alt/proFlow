package com.neuroflow.app.presentation.v2.flow

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.buildNormalizedScheduleTimestamps
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import java.time.Instant
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.repository.EnergyMetricsRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.model.CircadianEnergySummary
import com.neuroflow.app.domain.model.buildCircadianEnergySummary
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.DailyBlueprint
import com.neuroflow.app.domain.scheduler.DailyBlueprintOrchestrator
import com.neuroflow.app.domain.scheduler.isPersonalTask
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.model.extractStimulantDosesFromSlots
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.QuickNoteSynthesis
import dagger.hilt.android.qualifiers.ApplicationContext
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.FatigueScoreEvaluator
import com.neuroflow.app.domain.service.PharmacokineticStimulantCalculator
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskFatigueScoreUi
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.domain.service.computeLatestSafeStimulantCutoffMinute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.service.SimulationOutcome
import java.time.LocalTime
import java.time.ZoneId

enum class SimulationLeverType {
    NAP,
    CAFFEINE,
    WALK,
    BEDTIME
}

data class SimulationState(
    val isActive: Boolean = false,
    val simulatedNaps: List<DaytimeNapBout> = emptyList(),
    val simulatedStimulants: List<StimulantDoseBout> = emptyList(),
    val simulatedImpulses: List<CognitiveBlockWindow> = emptyList(),
    val simulatedBedtimeDeltaMinutes: Int = 0,
    val outcome: SimulationOutcome? = null
)

data class FlowUiStateV2(
    val activeTasks: List<TaskEntity> = emptyList(),
    val filteredTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val energyUi: EnergyUiModel? = null,
    val circadianSummary: DailyCircadianSummary? = null,
    val circadianEnergySummary: CircadianEnergySummary? = null,
    val dayTimeline: List<CircadianSnapshot> = emptyList(),
    val energyTimeline: List<CircadianSnapshot> = emptyList(),
    val dailyCognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
    val daytimeNaps: List<DaytimeNapBout> = emptyList(),
    val isFallbackSleep: Boolean = false,
    val learnedSampleCount: Double = 0.0,
    val recommendedTask: TaskEntity? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val overdueTasksCount: Int = 0,
    val showZeroGuiltPrompt: Boolean = false,
    val zeroGuiltDismissed: Boolean = false,
    val selectedTag: String? = null,
    val availableTags: List<String> = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin"),
    val isEveningShutdownTime: Boolean = false,
    val eveningShutdownDismissed: Boolean = false,
    val lastCompletedTask: TaskEntity? = null,
    val lastDeferredTask: Pair<TaskEntity, Long>? = null,
    val lastActionMessage: String? = null,
    val stimulantDoses: List<StimulantDoseBout> = emptyList(),
    val isCrashWindow: Boolean = false,
    val activePlasmaMg: Double = 0.0,
    val socialJetlagLagHours: Double = 0.0,
    val predictedSwsSuppressionPercent: Int = 0,
    val latestSafeStimulantCutoffFormatted: String = "16:11",
    val dailyBlueprint: DailyBlueprint? = null,
    val isBlueprintLoading: Boolean = false,
    val blueprintIncludedTaskIds: Set<String> = emptySet(),
    val simulationState: SimulationState = SimulationState(),
    val normalizedSleepDurationMinutes: Int = 480,
    val fatigueScoresByTaskId: Map<String, TaskFatigueScoreUi> = emptyMap()
)

@HiltViewModel
class FlowViewModelV2 @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val noteRepository: NoteRepository? = null,
    private val sessionRepository: SessionRepository? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val taskSchedulerService: TaskSchedulerService = DefaultTaskSchedulerService(circadianEnergyService),
    private val behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService(),
    @ApplicationContext private val context: Context? = null,
    private val geminiAiOnlineService: GeminiAiOnlineService? = null,
    private val energyMetricsRepository: EnergyMetricsRepository? = null,
    private val calendarSyncRepository: CalendarSyncRepository? = null,
    private val dailyBlueprintOrchestrator: DailyBlueprintOrchestrator? = null,
    private val clockTicker: com.neuroflow.app.domain.time.ClockTicker? = null
) : ViewModel() {

    private val _dismissedZeroGuilt = MutableStateFlow(false)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _eveningShutdownDismissed = MutableStateFlow(false)
    private val _lastCompletedTask = MutableStateFlow<TaskEntity?>(null)
    private val _lastSpawnedRecurringTaskId = MutableStateFlow<String?>(null)
    private var _lastPreCompleteHabitStreak: Int? = null
    private var _lastPreCompleteHabitDate: Long? = null
    private var _hasLastPreCompleteHabitDate: Boolean = false
    private val _lastDeferredTask = MutableStateFlow<Pair<TaskEntity, Long>?>(null)
    private val _lastActionMessage = MutableStateFlow<String?>(null)
    private val _dailyBlueprint = MutableStateFlow<DailyBlueprint?>(null)
    private val _isBlueprintLoading = MutableStateFlow(false)
    private val _blueprintIncludedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    private val _simulationState = MutableStateFlow(SimulationState())
    private var lastPredictionRecordedAt: Long = 0L

    internal var nowProvider: () -> Long = { System.currentTimeMillis() }

    internal constructor(
        taskRepository: TaskRepository,
        preferencesDataStore: UserPreferencesDataStore,
        noteRepository: NoteRepository? = null,
        sessionRepository: SessionRepository? = null,
        circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
        sleepTelemetryRepository: SleepTelemetryRepository? = null,
        taskSchedulerService: TaskSchedulerService = DefaultTaskSchedulerService(circadianEnergyService),
        behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService(),
        context: Context? = null,
        geminiAiOnlineService: GeminiAiOnlineService? = null,
        energyMetricsRepository: EnergyMetricsRepository? = null,
        calendarSyncRepository: CalendarSyncRepository? = null,
        dailyBlueprintOrchestrator: DailyBlueprintOrchestrator? = null,
        nowProvider: () -> Long
    ) : this(
        taskRepository,
        preferencesDataStore,
        noteRepository,
        sessionRepository,
        circadianEnergyService,
        sleepTelemetryRepository,
        taskSchedulerService,
        behavioralSupportService,
        context,
        geminiAiOnlineService,
        energyMetricsRepository,
        calendarSyncRepository,
        dailyBlueprintOrchestrator,
        clockTicker = null
    ) {
        this.nowProvider = nowProvider
    }

    val recentNotes: Flow<List<NoteEntity>> = noteRepository?.allNotes ?: flowOf(emptyList())

    val uiState: StateFlow<FlowUiStateV2> = combine(
        combine(
            taskRepository.observeActiveTasks().onEmpty { emit(emptyList()) },
            taskRepository.observeCompletedTasks().onEmpty { emit(emptyList()) },
            preferencesDataStore.preferencesFlow.onEmpty { emit(UserPreferences()) }.distinctUntilChanged { old, new ->
                old.wakeUpHour == new.wakeUpHour &&
                    old.wakeTimeMinute == new.wakeTimeMinute &&
                    old.workDayStart == new.workDayStart &&
                    old.workDayEnd == new.workDayEnd &&
                    old.sleepHour == new.sleepHour &&
                    old.peakEnergyStart == new.peakEnergyStart &&
                    old.peakEnergyEnd == new.peakEnergyEnd &&
                    old.effectivePeakStart == new.effectivePeakStart &&
                    old.effectivePeakEnd == new.effectivePeakEnd &&
                    old.detectedPeakMinuteOfDay == new.detectedPeakMinuteOfDay &&
                    old.sleepPressurePoints == new.sleepPressurePoints &&
                    old.onboardingCompleted == new.onboardingCompleted &&
                    old.tagCatalog == new.tagCatalog &&
                    old.stimulantLogsJson == new.stimulantLogsJson &&
                    old.quickPhysiologicalLogsJson == new.quickPhysiologicalLogsJson &&
                    old.protectedTimeSlots == new.protectedTimeSlots &&
                    old.quizChronotype == new.quizChronotype &&
                    old.manualChronotype == new.manualChronotype &&
                    old.meqScore == new.meqScore &&
                    old.manualPeakProfileEnabled == new.manualPeakProfileEnabled &&
                    old.manualPeakAnchorMinuteOfDay == new.manualPeakAnchorMinuteOfDay &&
                    old.dailyDeepWorkTargetMinutes == new.dailyDeepWorkTargetMinutes &&
                    old.energyTelemetryEnabled == new.energyTelemetryEnabled &&
                    old.affirmations == new.affirmations &&
                    old.quizPeakEnabled == new.quizPeakEnabled &&
                    old.adaptiveSystemDirective == new.adaptiveSystemDirective &&
                    old.weightDeadlineUrgency == new.weightDeadlineUrgency &&
                    old.weightImpact == new.weightImpact &&
                    old.weightCircadianMatch == new.weightCircadianMatch &&
                    old.weightSessionProgress == new.weightSessionProgress &&
                    old.weightFrogBonus == new.weightFrogBonus &&
                    old.weightQuadrant == new.weightQuadrant &&
                    old.weightPriorityLevel == new.weightPriorityLevel &&
                    old.weightDuration == new.weightDuration &&
                    old.weightFocusMode == new.weightFocusMode &&
                    old.weightPlanBoost == new.weightPlanBoost &&
                    old.weightEnjoyment == new.weightEnjoyment
            },
            (sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())).onEmpty { emit(emptyList()) }
        ) { active, completed, prefs, sleepLogs ->
            Quad(active, completed, prefs, sleepLogs)
        },
        combine(
            _dismissedZeroGuilt,
            _selectedTag,
            _eveningShutdownDismissed,
            _lastCompletedTask
        ) { dismissedGuilt, tag, shutdownDismissed, lastCompleted ->
            Quad(dismissedGuilt, tag, shutdownDismissed, lastCompleted)
        },
        combine<Any?, List<Any?>>(
            _lastDeferredTask,
            _lastActionMessage,
            _dailyBlueprint,
            _isBlueprintLoading,
            _blueprintIncludedTaskIds,
            _simulationState,
            (sessionRepository?.observeAll() ?: flowOf(emptyList<TimeSessionEntity>())).onEmpty {
                emit(runCatching { sessionRepository?.getAllSessions() }.getOrNull() ?: emptyList())
            },
            (clockTicker?.currentMinuteTick ?: flowOf(0L)).onEmpty { emit(0L) }
        ) { args ->
            args.toList()
        }
    ) { (rawActiveTasks, completedTasks, prefs, sleepLogs), (dismissedGuilt, tag, shutdownDismissed, lastCompleted), thirdList ->
        @Suppress("UNCHECKED_CAST")
        val lastDeferred = thirdList[0] as Pair<TaskEntity, Long>?
        val actionMsg = thirdList[1] as String?
        val dailyBlueprint = thirdList[2] as DailyBlueprint?
        val isBlueprintLoading = thirdList[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val blueprintIncludedTaskIds = thirdList[4] as Set<String>
        val simState = thirdList[5] as SimulationState
        @Suppress("UNCHECKED_CAST")
        val allSessions = thirdList[6] as List<TimeSessionEntity>
        val activeTasks = rawActiveTasks.filterNot { it.id == com.neuroflow.app.data.local.entity.TaskEntity.TASK_UNTRACKED_FOCUS }
        val now = nowProvider()
        val currentHour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        val systemZone = java.time.ZoneId.systemDefault()
        val zonedDateTime = java.time.Instant.ofEpochMilli(now).atZone(systemZone)
        var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), prefs.wakeTimeMinute.coerceIn(0, 59)).atZone(systemZone)
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
        val baseNaps = signal?.daytimeNaps ?: emptyList()
        val isFallback = sleepLogs.isEmpty() || (signal != null && signal.consolidatedSessions.isEmpty())

        val rollingAverageSleepMinutes = computeRollingDailySleepAverage(sleepLogs, now, systemZone)

        val dayStart = zonedDateTime.toLocalDate().atStartOfDay(systemZone).toInstant().toEpochMilli()
        val slotBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = zonedDateTime.toLocalDate(),
            zoneId = systemZone
        )

        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis in (dayStart - 8 * 3600_000L)..(dayStart + 26 * 3600_000L) }

        val physBlocks = physLogs.mapNotNull { log ->
            val logZdt = Instant.ofEpochMilli(log.timestampMillis).atZone(systemZone)
            val minOfDay = logZdt.hour * 60 + logZdt.minute
            val duration = log.durationMinutes.coerceIn(5, 240)
            when (log.type) {
                PhysiologicalEventType.MEAL -> CognitiveBlockWindow(
                    startMinuteOfDay = minOfDay,
                    endMinuteOfDay = (minOfDay + duration) % 1440,
                    category = CognitiveBlockCategory.MEAL,
                    intensity = log.intensity,
                    label = "Quick Logged Meal #${log.timestampMillis}",
                    explicitStartMillis = log.timestampMillis,
                    explicitEndMillis = log.timestampMillis + duration * 60_000L
                )
                PhysiologicalEventType.WORKOUT -> CognitiveBlockWindow(
                    startMinuteOfDay = minOfDay,
                    endMinuteOfDay = (minOfDay + duration) % 1440,
                    category = CognitiveBlockCategory.EXERCISE,
                    intensity = log.intensity,
                    label = "Quick Logged Workout #${log.timestampMillis}",
                    explicitStartMillis = log.timestampMillis,
                    explicitEndMillis = log.timestampMillis + duration * 60_000L
                )
                PhysiologicalEventType.NAP -> null
            }
        }

        val physNaps = physLogs.filter { it.type == PhysiologicalEventType.NAP }.map { log ->
            val napDuration = log.durationMinutes.coerceIn(5, 180)
            DaytimeNapBout(
                startMillis = log.timestampMillis,
                endMillis = log.timestampMillis + napDuration * 60_000L
            )
        }

        val dailyBlocks = slotBlocks + physBlocks
        val naps = baseNaps + physNaps

        val loggedStimulants = parseStimulantLogs(prefs.stimulantLogsJson)
        val slotStimulants = extractStimulantDosesFromSlots(
            slots = prefs.protectedTimeSlots,
            date = zonedDateTime.toLocalDate(),
            zoneId = systemZone
        )
        val allStimulants = (loggedStimulants + slotStimulants)
            .filter { it.timestampMillis in (dayStart - 8 * 3600_000L)..(dayStart + 26 * 3600_000L) }
            .distinctBy { "${it.timestampMillis}_${it.type}" }

        val stimEffect = PharmacokineticStimulantCalculator.evaluateAt(now, allStimulants)
        val isCrashWindow = allStimulants.any { bout ->
            val elapsed = (now - bout.timestampMillis) / 3_600_000.0
            elapsed in (bout.type.halfLifeHours)..(bout.type.halfLifeHours + 3.5) && bout.type.crashSeverityFactor >= 0.05
        } || (allStimulants.isNotEmpty() && stimEffect.acuteAlertnessBoostPoints < -1.0)

        // Bedtime Slow-Wave Sleep (SWS) Suppression & Safe Stimulant Cutoff
        val plannedBedtimeZdt = zonedDateTime.toLocalDate()
            .atTime(prefs.sleepHour.coerceIn(0, 23), 0)
            .atZone(systemZone)
            .let { if (it.toInstant().toEpochMilli() <= now) it.plusDays(1) else it }
        val swsSuppressionPercent = (PharmacokineticStimulantCalculator.predictBedtimeSwsSuppression(
            plannedSleepMillis = plannedBedtimeZdt.toInstant().toEpochMilli(),
            doses = allStimulants
        ) * 100.0).roundToInt().coerceIn(0, 35)

        val cutoffMin = computeLatestSafeStimulantCutoffMinute(sleepHour = prefs.sleepHour)
        val cutoffFormatted = String.format(Locale.US, "%02d:%02d", cutoffMin / 60, cutoffMin % 60)

        val sleepWindows = sleepLogs.map { TimeSlotWindow(it.startAt, it.endAt) }
        val socialJetlagReport = SocialJetlagCalculator.evaluateFromSleepWindows(sleepWindows, now, systemZone)

        val behavioralObservations = buildBehavioralObservations(completedTasks = completedTasks, zoneId = systemZone)
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (behavioralObservations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(behavioralObservations, initialPeakHour = initialPeak)
        } else null
        val sampleCount = learnedProfile?.sampleCount ?: 0.0

        val (snapshot, energyUi) = computeEnergyUiState(
            circadianEnergyService = circadianEnergyService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            prefs = prefs,
            sleepLogs = sleepLogs,
            completedTasks = completedTasks,
            nowMillis = now,
            stimulantDoses = allStimulants
        )

        if (prefs.energyTelemetryEnabled && energyMetricsRepository != null && (now - lastPredictionRecordedAt >= 15 * 60_000L)) {
            lastPredictionRecordedAt = now
            viewModelScope.launch {
                runCatching {
                    energyMetricsRepository.recordEnergyPrediction(
                        predictedAtMillis = now,
                        baselineRawEnergy = snapshot.baselineBiologicalEnergy.toFloat(),
                        peakScore = snapshot.usableEnergy.toFloat(),
                        fatiguePenalty = (snapshot.homeostaticSleepPressure * 100).toFloat(),
                        sleepPressurePoints = prefs.sleepPressurePoints,
                        fatiguePercent = (snapshot.homeostaticSleepPressure * 100).toInt().coerceIn(0, 100),
                        momentAdjustment = (snapshot.physiologicalImpulse + snapshot.learnedAdjustmentPoints).toFloat(),
                        momentConfidence = snapshot.predictionConfidence.toFloat(),
                        momentSupportScore = 50f,
                        momentPressureScore = 50f,
                        adjustedRawEnergy = snapshot.usableEnergy.toFloat(),
                        usableEnergy = snapshot.usableEnergy,
                        chronotype = prefs.quizChronotype ?: prefs.manualChronotype ?: "INTERMEDIATE",
                        wakeUpHour = prefs.wakeUpHour,
                        peakMinuteOfDay = prefs.detectedPeakMinuteOfDay.takeIf { it > 0 } ?: (prefs.peakEnergyStart * 60),
                        peakConfidence = prefs.peakDetectionConfidence.takeIf { it > 0f } ?: 0.85f,
                        activeTaskCount = activeTasks.size
                    )
                }
            }
        }
        val battery = snapshot.usableEnergy
        val currentEnergyZone = when {
            battery >= 65 -> EnergyLevel.HIGH
            battery >= 35 -> EnergyLevel.MEDIUM
            else -> EnergyLevel.LOW
        }

        val overdueTasks = activeTasks.filter { task ->
            val hasExplicitScheduledDay = task.effectiveScheduledDateMillis() != null ||
                (task.scheduledTime ?: 0L) >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
            task.postponeCount >= 2 ||
                (hasExplicitScheduledDay && task.scheduledTime != null && task.resolveScheduledEpochMillis()?.let { it < now - 2 * 3600_000L } == true) ||
                (task.effectiveScheduledDateMillis() != null && task.effectiveScheduledDateMillis()!! < now - 86_400_000L) ||
                (task.deadlineDate != null && task.resolveDeadlineEpochMillis()?.let { it < now } == true)
        }
        val showZeroGuilt = overdueTasks.size >= 3 && !dismissedGuilt

        val defaultTags = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin")
        val dynamicTags = (prefs.tagCatalog + activeTasks.flatMap { task ->
            (task.tags.split(",") + task.contextTag)
        })
            .map { it.trim().removePrefix("#").removePrefix("@") }
            .filter { it.isNotBlank() }
            .map { "#$it" }
        val mergedTags = (defaultTags + dynamicTags).distinctBy { it.lowercase() }

        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(dayStart) - 1
        val completedIds = completedTasks.map { it.id }.toSet()
        val activeTaskIds = activeTasks.map { it.id }.toSet()

        fun isContinuationBlocked(task: TaskEntity, activeIds: Set<String>): Boolean {
            val partNumMatch = Regex("""[#_]part(\d+)""").find(task.id)
            val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""\(Part (\d+)/""").find(task.title)?.groupValues?.get(1)?.toIntOrNull()
            val isContinuation = (partNum != null && partNum >= 2) ||
                task.id.endsWith("_part2") ||
                task.id.contains("#part2") ||
                task.parentTaskId != null
            if (!isContinuation) return false
            val parentSlug = task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
            val isParentActive = (task.parentTaskId != null && task.parentTaskId in activeIds) ||
                (parentSlug != task.id && parentSlug in activeIds)
            val hasPartRegex = task.id.contains(Regex("""[#_]part\d+"""))
            val isPart1Active = if (hasPartRegex) {
                val part1Id = task.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1")
                part1Id != task.id && part1Id in activeIds
            } else false
            val isPrecedingPartActive = if (hasPartRegex && partNum != null) {
                (1 until partNum).any { prevPart ->
                    val prevId = task.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart")
                    prevId != task.id && prevId in activeIds
                }
            } else if (task.parentTaskId != null && partNum != null && partNum > 1) {
                activeTasks.any { other ->
                    other.id != task.id &&
                        (other.parentTaskId == task.parentTaskId || other.id == task.parentTaskId) &&
                        (Regex("""\(Part (\d+)/""").find(other.title)?.groupValues?.get(1)?.toIntOrNull() ?: 1) < partNum
                }
            } else false
            return isParentActive || isPart1Active || isPrecedingPartActive
        }

        val actionableActiveTasks = activeTasks.filter { task ->
            val sDate = task.effectiveScheduledDateMillis()
                ?: task.scheduledTime?.takeIf { it >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS }?.let { com.neuroflow.app.data.local.entity.startOfLocalDay(it) }
            val dDate = task.resolveDeadlineEpochMillis()
            val isNotFuture = (sDate != null && sDate <= dayEnd) ||
                (sDate == null && dDate != null && dDate <= dayEnd) ||
                (sDate == null && dDate == null)
            val dependenciesMet = task.dependsOnTaskIds.isBlank() ||
                task.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }.all { it in completedIds }
            val isBlocked = isContinuationBlocked(task, activeTaskIds)
            task.quadrant != Quadrant.ELIMINATE &&
                !isBlocked &&
                task.waitingFor.isBlank() &&
                isNotFuture &&
                dependenciesMet
        }
        val isWindDown = snapshot.phase == com.neuroflow.app.domain.service.EnergyPhase.WIND_DOWN
        val matchingTasks = actionableActiveTasks.filter { it.energyLevel == currentEnergyZone }
        val lowCognitiveTasks = actionableActiveTasks.filter { task ->
            task.energyLevel == EnergyLevel.LOW || (
                task.energyLevel != EnergyLevel.HIGH &&
                !task.isFrog &&
                task.taskType != TaskType.ANALYTICAL &&
                !task.tags.contains("deepwork", ignoreCase = true) &&
                !task.contextTag.equals("deepwork", ignoreCase = true)
            )
        }
        val candidatePool: List<TaskEntity> = if (isWindDown) {
            // Option 2C: When in WIND_DOWN, prioritize low-stress / low-energy tasks (EnergyLevel.LOW or non-deep tasks)
            if (lowCognitiveTasks.isNotEmpty()) lowCognitiveTasks else actionableActiveTasks
        } else if (matchingTasks.isNotEmpty()) {
            matchingTasks
        } else {
            actionableActiveTasks
        }
        val effectiveSnapshot = snapshot.copy(usableEnergy = battery)
        val planningCalibration = com.neuroflow.app.domain.engine.AnalyticsEngine.PlanningCalibration.from(completedTasks + activeTasks)
        val enjoymentCalibration = com.neuroflow.app.domain.engine.AnalyticsEngine.EnjoymentCalibration.from(completedTasks + activeTasks)
        val calibrationRatio = planningCalibration.globalFactor.toDouble().coerceIn(0.8, 1.6)
        val priorityWeights = com.neuroflow.app.domain.scheduler.SchedulableTaskMapper.toPriorityWeights(prefs)

        val candidateSchedulables = candidatePool.associate { task ->
            task.id to com.neuroflow.app.domain.scheduler.SchedulableTaskMapper.toSchedulableTask(
                task = task,
                allContextTasks = activeTasks,
                dayStartMillis = dayStart,
                enjoymentCalibration = enjoymentCalibration,
                usePlannedBlockForScoring = true
            )
        }
        val dependentCounts = com.neuroflow.app.domain.scheduler.SchedulableTaskMapper.computeDependentCounts(
            candidateSchedulables.values.toList()
        )

        val recommended = candidatePool.maxByOrNull { task: TaskEntity ->
            val schedulable = candidateSchedulables[task.id]!!
            val isPersonal = schedulable.isPersonalTask
            if (isWindDown) {
                // In WIND_DOWN, subdue high-intensity frogs & deep-work prompts; prioritize low-energy & brief wrap-up duration
                val lowEnergyBonus = when (task.energyLevel) {
                    EnergyLevel.LOW -> 200.0
                    EnergyLevel.MEDIUM -> 50.0
                    EnergyLevel.HIGH -> -100.0
                }
                val frogPenalty = if (task.isFrog) -500.0 else 0.0
                val durationPenalty = com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task).coerceAtLeast(0).toDouble()
                val personalBonus = if (isPersonal) 15.0 else 0.0
                lowEnergyBonus + frogPenalty + (task.impactScore * 0.5) + personalBonus - durationPenalty
            } else {
                val breakdown = taskSchedulerService.evaluateTaskScoreWithWeights(
                    task = schedulable,
                    snapshot = effectiveSnapshot,
                    dependentCount = dependentCounts[task.id] ?: 0,
                    calibrationRatio = calibrationRatio,
                    weights = priorityWeights,
                    referenceNowMillis = now
                )
                val frogBoost = if (task.isFrog) 8.0 * prefs.weightFrogBonus else 0.0
                val personalBoost = if (isPersonal) 6.0 else 0.0
                breakdown.totalScore + frogBoost + personalBoost
            }
        }

        val todayActiveTasks = activeTasks.filter { task ->
            val sDate = task.effectiveScheduledDateMillis()
                ?: task.scheduledTime?.takeIf { it >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS }?.let { com.neuroflow.app.data.local.entity.startOfLocalDay(it) }
            val dDate = task.resolveDeadlineEpochMillis()
            val isBlocked = isContinuationBlocked(task, activeTaskIds)
            task.quadrant != Quadrant.ELIMINATE &&
                !isBlocked &&
                ((sDate != null && sDate <= dayEnd) || (sDate == null && dDate != null && dDate <= dayEnd) || (sDate == null && dDate == null))
        }.sortedWith(
            // Timed tasks sort chronologically first; date-only untimed tasks (scheduledTime == null) sort after (BUG-235)
            compareBy<TaskEntity> { it.scheduledTime == null }
                .thenBy { it.resolveScheduledEpochMillis() ?: Long.MAX_VALUE }
                .thenByDescending { it.isFrog }
                .thenByDescending { it.createdAt }
        )

        val filtered = if (tag.isNullOrBlank() || tag.equals("All", ignoreCase = true)) {
            todayActiveTasks
        } else {
            val query = tag.removePrefix("#").removePrefix("@").trim()
            val wordRegex = Regex("""(?i)(?:^|\s)[#@]?\Q$query\E(?=\s|$)""")
            todayActiveTasks.filter { task ->
                val hasMatchingTag = (task.tags.split(",") + task.contextTag)
                    .map { it.trim().removePrefix("#").removePrefix("@") }
                    .filter { it.isNotBlank() }
                    .any { it.equals(query, ignoreCase = true) }
                hasMatchingTag || wordRegex.containsMatchIn(task.title)
            }
        }

        val isEvening = currentHour >= prefs.workDayEnd.coerceIn(12, 23)

        val manualPeakHour = if (prefs.manualPeakProfileEnabled) (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
        val chronotype = if (!prefs.quizChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.quizChronotype)
        } else if (!prefs.manualChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.manualChronotype)
        } else if (prefs.meqScore > 0) {
            when {
                prefs.meqScore >= 70 -> ChronotypeProfile.DEFINITE_MORNING
                prefs.meqScore >= 59 -> ChronotypeProfile.MODERATE_MORNING
                prefs.meqScore >= 42 -> ChronotypeProfile.INTERMEDIATE
                prefs.meqScore >= 31 -> ChronotypeProfile.MODERATE_EVENING
                else -> ChronotypeProfile.DEFINITE_EVENING
            }
        } else {
            ChronotypeProfile.INTERMEDIATE
        }

        val circadianSummary = runCatching {
            circadianEnergyService.computeDailySummary(
                timestampMillis = now,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = systemZone,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakHour,
                cognitiveBlocks = dailyBlocks,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                stimulantDoses = allStimulants,
                socialJetlagLagHours = socialJetlagReport.mondayMorningPhaseLagHours
            )
        }.getOrNull()

        val dayTimeline: List<CircadianSnapshot> = runCatching {
            circadianEnergyService.projectDayTimeline(
                dayStartMillis = dayStart,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                stepMinutes = 15,
                zoneId = systemZone,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakHour,
                cognitiveBlocks = dailyBlocks,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                stimulantDoses = allStimulants,
                socialJetlagLagHours = socialJetlagReport.mondayMorningPhaseLagHours
            )
        }.getOrElse { emptyList() }

        val todayCompletedDeepWorkMinutes = completedTasks.filter { task ->
            val completionTime = task.completedAt ?: task.updatedAt
            completionTime in dayStart..dayEnd && (
                task.energyLevel == EnergyLevel.HIGH ||
                task.isFrog ||
                task.taskType == TaskType.ANALYTICAL ||
                task.tags.contains("deepwork", ignoreCase = true) ||
                task.contextTag.equals("deepwork", ignoreCase = true)
            )
        }.sumOf { task ->
            (task.actualDurationMinutes?.roundToInt()
                ?: task.totalTimeTrackedMinutes.roundToInt().takeIf { it > 0 }
                ?: com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task)).coerceAtLeast(1)
        }

        val untrackedTodayMinutes = allSessions
            .filter { it.endedAt != null && it.startedAt in dayStart..dayEnd && it.taskId == com.neuroflow.app.data.local.entity.TaskEntity.TASK_UNTRACKED_FOCUS }
            .sumOf { (it.durationMinutes.takeIf { d -> d > 0f } ?: it.trackedMinutes(it.endedAt ?: now)).roundToInt() }
        val totalCompletedDeepWorkMinutes = todayCompletedDeepWorkMinutes + untrackedTodayMinutes

        val circadianEnergySummary = buildCircadianEnergySummary(
            dailySummary = circadianSummary,
            snapshot = effectiveSnapshot,
            sleepPressurePoints = prefs.sleepPressurePoints,
            dailyDeepWorkTargetMinutes = prefs.dailyDeepWorkTargetMinutes,
            sleepHour = prefs.sleepHour,
            completedDeepWorkMinutes = totalCompletedDeepWorkMinutes,
            currentTime = zonedDateTime.toLocalTime()
        )

        val fatigueScoresByTaskId = FatigueScoreEvaluator.evaluateTasks(
            tasks = (todayActiveTasks + actionableActiveTasks).distinctBy { it.id },
            allContextTasks = activeTasks,
            completedTasks = completedTasks,
            allSessions = allSessions,
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            nowMillis = now,
            timeline = dayTimeline,
            fallbackSnapshot = effectiveSnapshot,
            prefs = prefs,
            taskSchedulerService = taskSchedulerService,
            zoneId = systemZone
        )

        FlowUiStateV2(
            activeTasks = activeTasks,
            filteredTasks = filtered,
            completedTasks = completedTasks,
            energyUi = energyUi,
            circadianSummary = circadianSummary,
            circadianEnergySummary = circadianEnergySummary,
            dayTimeline = dayTimeline,
            energyTimeline = dayTimeline,
            dailyCognitiveBlocks = dailyBlocks,
            daytimeNaps = naps,
            isFallbackSleep = isFallback,
            learnedSampleCount = sampleCount,
            recommendedTask = recommended,
            preferences = prefs,
            isLoading = false,
            overdueTasksCount = overdueTasks.size,
            showZeroGuiltPrompt = showZeroGuilt,
            zeroGuiltDismissed = dismissedGuilt,
            selectedTag = tag,
            availableTags = mergedTags,
            isEveningShutdownTime = isEvening,
            eveningShutdownDismissed = shutdownDismissed,
            lastCompletedTask = lastCompleted,
            lastDeferredTask = lastDeferred,
            lastActionMessage = actionMsg,
            stimulantDoses = allStimulants,
            isCrashWindow = isCrashWindow,
            activePlasmaMg = stimEffect.activeEquivalentMg,
            socialJetlagLagHours = socialJetlagReport.socialJetlagHours,
            predictedSwsSuppressionPercent = swsSuppressionPercent,
            latestSafeStimulantCutoffFormatted = cutoffFormatted,
            dailyBlueprint = dailyBlueprint,
            isBlueprintLoading = isBlueprintLoading,
            blueprintIncludedTaskIds = blueprintIncludedTaskIds,
            simulationState = simState,
            normalizedSleepDurationMinutes = sleepDurationMinutes,
            fatigueScoresByTaskId = fatigueScoresByTaskId
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FlowUiStateV2(isLoading = true)
    )

    fun openDailyBlueprint(dateMillis: Long = nowProvider()) {
        viewModelScope.launch {
            _isBlueprintLoading.value = true
            try {
                val orchestrator = dailyBlueprintOrchestrator
                if (orchestrator != null) {
                    val blueprint = orchestrator.generateBlueprint(dateMillis)
                    _dailyBlueprint.value = blueprint
                    _blueprintIncludedTaskIds.value = blueprint.items.map { it.task.id }.toSet()
                }
            } catch (e: Exception) {
                _lastActionMessage.value = "Unable to build daily blueprint. Please try again."
            } finally {
                _isBlueprintLoading.value = false
            }
        }
    }

    fun toggleBlueprintTask(taskId: String, isIncluded: Boolean) {
        val current = _blueprintIncludedTaskIds.value.toMutableSet()
        if (isIncluded) {
            current.add(taskId)
        } else {
            current.remove(taskId)
        }
        _blueprintIncludedTaskIds.value = current
    }

    fun applyDailyBlueprint() {
        viewModelScope.launch {
            val blueprint = _dailyBlueprint.value ?: return@launch
            val orchestrator = dailyBlueprintOrchestrator ?: return@launch
            val included = _blueprintIncludedTaskIds.value
            val result = orchestrator.applyBlueprint(blueprint, included)
            _lastActionMessage.value = "Scheduled ${result.committedCount} tasks"
            _dailyBlueprint.value = null
        }
    }

    fun dismissDailyBlueprint() {
        _dailyBlueprint.value = null
    }

    fun toggleSimulationMode(active: Boolean? = null) {
        val current = _simulationState.value
        val newActive = active ?: !current.isActive
        if (!newActive) {
            _simulationState.value = SimulationState(isActive = false)
        } else {
            _simulationState.value = current.copy(isActive = true)
        }
    }

    fun toggleSimulatedLever(leverType: SimulationLeverType) {
        val current = _simulationState.value
        val now = nowProvider()
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

        val newNaps = current.simulatedNaps.toMutableList()
        val newStimulants = current.simulatedStimulants.toMutableList()
        val newImpulses = current.simulatedImpulses.toMutableList()
        var newBedtimeDelta = current.simulatedBedtimeDeltaMinutes

        when (leverType) {
            SimulationLeverType.NAP -> {
                if (newNaps.isNotEmpty()) {
                    newNaps.clear()
                } else {
                    val dipStart = today.atTime(LocalTime.of(13, 30)).atZone(zone).toInstant().toEpochMilli()
                    val napStart = if (now < dipStart) dipStart else now + 5 * 60_000L
                    newNaps.add(DaytimeNapBout(napStart, napStart + 20 * 60_000L))
                }
            }
            SimulationLeverType.CAFFEINE -> {
                if (newStimulants.isNotEmpty()) {
                    newStimulants.clear()
                } else {
                    newStimulants.add(StimulantDoseBout(now, 100.0, StimulantType.CAFFEINE))
                }
            }
            SimulationLeverType.WALK -> {
                if (newImpulses.isNotEmpty()) {
                    newImpulses.clear()
                } else {
                    val walkStartMin = Instant.ofEpochMilli(now).atZone(zone).hour * 60 + Instant.ofEpochMilli(now).atZone(zone).minute
                    newImpulses.add(
                        CognitiveBlockWindow(
                            startMinuteOfDay = walkStartMin,
                            endMinuteOfDay = walkStartMin + 30,
                            category = CognitiveBlockCategory.EXERCISE,
                            intensity = 1.0,
                            label = "Walk",
                            explicitStartMillis = now,
                            explicitEndMillis = now + 30 * 60_000L
                        )
                    )
                }
            }
            SimulationLeverType.BEDTIME -> {
                newBedtimeDelta = if (newBedtimeDelta > 0) 0 else 45
            }
        }

        val baseTimeline = uiState.value.energyTimeline.ifEmpty {
            val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val wakeTime = dayStart + 7 * 3600_000L
            circadianEnergyService.projectDayTimeline(dayStart, wakeTime, 480, 15, zone)
        }
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val wakeTime = baseTimeline.firstOrNull()?.timestampMillis ?: (dayStart + 7 * 3600_000L)
        val sleepDuration = uiState.value.normalizedSleepDurationMinutes.takeIf { it in 60..960 } ?: 480

        val outcome = if (newNaps.isNotEmpty() || newStimulants.isNotEmpty() || newImpulses.isNotEmpty() || newBedtimeDelta > 0) {
            circadianEnergyService.simulateDayInterventions(
                baseTimeline = baseTimeline,
                dayStartMillis = dayStart,
                wakeTimeMillis = wakeTime,
                lastSleepDurationMinutes = sleepDuration,
                stepMinutes = 15,
                zoneId = zone,
                simulatedNaps = newNaps,
                simulatedStimulants = newStimulants,
                simulatedImpulses = newImpulses,
                simulatedBedtimeDeltaMinutes = newBedtimeDelta,
                activeBlocks = uiState.value.dailyCognitiveBlocks
            )
        } else null

        _simulationState.value = current.copy(
            isActive = true,
            simulatedNaps = newNaps,
            simulatedStimulants = newStimulants,
            simulatedImpulses = newImpulses,
            simulatedBedtimeDeltaMinutes = newBedtimeDelta,
            outcome = outcome
        )
    }

    fun resetSimulation() {
        _simulationState.value = SimulationState(isActive = false)
    }

    fun applySimulationToPlan() {
        val current = _simulationState.value
        val now = nowProvider()
        val zone = ZoneId.systemDefault()

        viewModelScope.launch {
            val existingSlots = preferencesDataStore.preferencesFlow.first().protectedTimeSlots.toMutableList()

            if (current.simulatedNaps.isNotEmpty()) {
                val nap = current.simulatedNaps.first()
                val napZdt = Instant.ofEpochMilli(nap.startMillis).atZone(zone)
                val startMin = napZdt.hour * 60 + napZdt.minute
                val napSlot = ProtectedTimeSlot(
                    id = "pts_nap_${System.currentTimeMillis()}",
                    label = "Power Nap",
                    startMinuteOfDay = startMin,
                    durationMinutes = nap.durationMinutes,
                    category = PtsCategory.BREAK,
                    isEnabled = true,
                    dateMillis = napZdt.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                )
                existingSlots.add(napSlot)
            }

            if (current.simulatedImpulses.isNotEmpty()) {
                val walk = current.simulatedImpulses.first()
                val walkZdt = Instant.ofEpochMilli(walk.startMillis).atZone(zone)
                val startMin = walkZdt.hour * 60 + walkZdt.minute
                val walkSlot = ProtectedTimeSlot(
                    id = "pts_walk_${System.currentTimeMillis()}",
                    label = "Energy Walk",
                    startMinuteOfDay = startMin,
                    durationMinutes = 30,
                    category = PtsCategory.BREAK,
                    isEnabled = true,
                    dateMillis = walkZdt.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                )
                existingSlots.add(walkSlot)
            }

            if (current.simulatedNaps.isNotEmpty() || current.simulatedImpulses.isNotEmpty()) {
                preferencesDataStore.updateProtectedTimeSlots(existingSlots)
            }

            if (current.simulatedBedtimeDeltaMinutes > 0) {
                preferencesDataStore.updatePreferences { prefs ->
                    val newHour = ((prefs.sleepHour * 60 - current.simulatedBedtimeDeltaMinutes) / 60).coerceIn(18, 23)
                    prefs.copy(sleepHour = newHour)
                }
            }

            if (current.simulatedStimulants.isNotEmpty()) {
                current.simulatedStimulants.forEach { dose ->
                    preferencesDataStore.recordStimulantDose(dose.type)
                }
            }

            val hasAppliedChanges = current.simulatedNaps.isNotEmpty() ||
                current.simulatedImpulses.isNotEmpty() ||
                current.simulatedBedtimeDeltaMinutes > 0 ||
                current.simulatedStimulants.isNotEmpty()

            _simulationState.value = SimulationState(isActive = false)
            if (hasAppliedChanges) {
                _lastActionMessage.value = "Applied simulated plan adjustments ⚡"
            }
        }
    }

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            val dayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
            val boundTask = when {
                task.scheduledTime != null && task.scheduledTime >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS -> {
                    val epochStart = com.neuroflow.app.data.local.entity.startOfLocalDay(task.scheduledTime)
                    val timeOffset = task.scheduledTime - epochStart
                    task.copy(
                        scheduledDate = epochStart,
                        scheduledTime = timeOffset,
                        habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) epochStart else task.habitDate
                    )
                }
                task.scheduledDate != null -> {
                    if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE && task.habitDate == null) {
                        task.copy(habitDate = task.scheduledDate)
                    } else {
                        task
                    }
                }
                task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE -> {
                    task.copy(
                        scheduledDate = dayStart,
                        habitDate = dayStart
                    )
                }
                task.scheduledTime != null -> {
                    // Intraday timed task without date: anchor to dayStart
                    task.copy(
                        scheduledDate = dayStart,
                        habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) dayStart else task.habitDate
                    )
                }
                else -> task
            }
            val (deadDate, deadTime) = if (boundTask.deadlineTime != null && boundTask.deadlineTime >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS) {
                val dDay = com.neuroflow.app.data.local.entity.startOfLocalDay(boundTask.deadlineTime)
                dDay to (boundTask.deadlineTime - dDay)
            } else {
                boundTask.deadlineDate to boundTask.deadlineTime
            }
            val finalTask = boundTask.copy(deadlineDate = deadDate, deadlineTime = deadTime)
            taskRepository.insert(finalTask)
            val newTags = (finalTask.tags.split(",") + finalTask.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(finalTask)
            if (finalTask.quadrant != Quadrant.ELIMINATE && finalTask.status == TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, finalTask) }
            }
        }
    }

    fun logStimulant(type: StimulantType) {
        viewModelScope.launch {
            preferencesDataStore.recordStimulantDose(type)
            val label = when (type) {
                StimulantType.CAFFEINE -> "Caffeine ☕"
                StimulantType.GREEN_TEA_L_THEANINE -> "Green Tea 🍵"
                StimulantType.PRESCRIPTION_STIMULANT_IR -> "Meds IR 💊"
                StimulantType.PRESCRIPTION_STIMULANT_XR -> "Meds XR 💊"
            }
            _lastActionMessage.value = "Stimulant logged: $label — Energy curve updated"
        }
    }

    fun undoLogStimulant() {
        viewModelScope.launch {
            val undone = preferencesDataStore.undoLastStimulantDose()
            if (undone) {
                _lastActionMessage.value = "Undone: Removed last stimulant dose ↩️"
            } else {
                _lastActionMessage.value = null
            }
        }
    }

    fun clearAllStimulants() {
        viewModelScope.launch {
            preferencesDataStore.clearStimulantLogs()
            _lastActionMessage.value = "Cleared all logged stimulants 🧹"
        }
    }

    fun removeStimulantDose(timestampMillis: Long, type: StimulantType? = null) {
        viewModelScope.launch {
            val typeStr = type?.name
            val removed = preferencesDataStore.removeStimulantDose(timestampMillis, typeStr)
            if (removed) {
                _lastActionMessage.value = "Removed stimulant dose 🗑️"
            }
        }
    }

    fun logPhysiologicalEvent(type: PhysiologicalEventType) {
        viewModelScope.launch {
            preferencesDataStore.recordPhysiologicalEvent(type)
            val label = when (type) {
                PhysiologicalEventType.MEAL -> "Meal Dip 🍽️ (-12.0 pt nadir)"
                PhysiologicalEventType.WORKOUT -> "Workout 🏃 (+10 pt endorphin window)"
                PhysiologicalEventType.NAP -> "20m Power Nap 💤 (sleep pressure cleared)"
            }
            _lastActionMessage.value = "Bio-event logged: $label — Curve updated"
        }
    }

    fun undoLogPhysiologicalEvent() {
        viewModelScope.launch {
            val undone = preferencesDataStore.undoLastPhysiologicalEvent()
            if (undone) {
                _lastActionMessage.value = "Undone: Removed last physiological event ↩️"
            } else {
                _lastActionMessage.value = null
            }
        }
    }

    fun clearAllPhysiologicalEvents() {
        viewModelScope.launch {
            preferencesDataStore.clearPhysiologicalLogs()
            _lastActionMessage.value = "Cleared all physiological events 🧹"
        }
    }

    fun addTaskFromNote(
        synthesis: com.neuroflow.app.domain.service.QuickNoteSynthesis,
        rawContent: String
    ) {
        viewModelScope.launch {
            val fullText = if (synthesis.extractedTaskTitle.isNotBlank()) "${synthesis.extractedTaskTitle}: $rawContent" else rawContent
            val inferred = TaskInferenceEngine.inferFromInput(fullText)
            val now = System.currentTimeMillis()
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
            val tagsCombined = (synthesis.tags + inferred.contextTags).distinct().filter { it.isNotBlank() }.joinToString(",")
            val contextTag = inferred.contextTags.firstOrNull().orEmpty()
            val stepList: List<com.neuroflow.app.domain.model.StepByStepPlanStep> = if (synthesis.firstActionStep.isNotBlank()) {
                listOf(com.neuroflow.app.domain.model.StepByStepPlanStep(synthesis.firstActionStep, completed = false))
            } else emptyList()
            val ifThenPlanStr = if (stepList.isNotEmpty()) {
                com.neuroflow.app.domain.model.StepByStepPlanCodec.serialize(stepList)
            } else ""

            val rawScheduledTime = inferred.scheduledTime
            val (schedDate, schedTime) = if (rawScheduledTime != null && rawScheduledTime >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS) {
                val sDay = com.neuroflow.app.data.local.entity.startOfLocalDay(rawScheduledTime)
                sDay to (rawScheduledTime - sDay)
            } else {
                (inferred.scheduledDate ?: todayStart) to rawScheduledTime
            }

            val rawDeadlineTime = inferred.deadlineTime
            val (deadDate, deadTime) = if (rawDeadlineTime != null && rawDeadlineTime >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS) {
                val dDay = com.neuroflow.app.data.local.entity.startOfLocalDay(rawDeadlineTime)
                dDay to (rawDeadlineTime - dDay)
            } else {
                inferred.deadlineDate to rawDeadlineTime
            }

            val newTask = TaskEntity(
                title = synthesis.extractedTaskTitle.ifBlank { inferred.sanitizedTitle.ifBlank { "Quick Task" } },
                description = rawContent,
                estimatedDurationMinutes = if (synthesis.estimatedMinutes > 0) synthesis.estimatedMinutes else inferred.durationMinutes,
                priority = inferred.priority,
                energyLevel = inferred.energyDemand,
                taskType = inferred.taskType,
                quadrant = inferred.quadrant,
                tags = tagsCombined,
                contextTag = contextTag,
                ifThenPlan = ifThenPlanStr,
                scheduledDate = schedDate,
                scheduledTime = schedTime,
                deadlineDate = deadDate,
                deadlineTime = deadTime,
                isFrog = inferred.isFrog,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insert(newTask)
            val newTags = (newTask.tags.split(",") + newTask.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(newTask)
            if (newTask.quadrant != Quadrant.ELIMINATE && newTask.status == TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, newTask) }
            }
            _lastActionMessage.value = "Created task from note: ${newTask.title.take(24)} ⚡"
        }
    }

    fun addTaskFromNote(title: String, content: String) {
        val inferred = TaskInferenceEngine.inferFromInput(if (title.isNotBlank()) "$title: $content" else content)
        val syn = com.neuroflow.app.domain.service.QuickNoteSynthesis(
            extractedTaskTitle = title.trim().ifBlank { inferred.sanitizedTitle.ifBlank { content.take(35) } },
            suggestedCategory = inferred.taskType.name,
            estimatedMinutes = inferred.durationMinutes,
            tags = inferred.contextTags,
            firstActionStep = ""
        )
        addTaskFromNote(syn, content)
    }

    fun shiftTaskToPeak(task: TaskEntity, nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            val summary = uiState.value.circadianSummary
            val manualPeak = if (prefs.manualPeakProfileEnabled) (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
            val peakHour = manualPeak ?: summary?.peakWindowStartHour
                ?: (if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart else prefs.peakEnergyStart).coerceIn(0, 23)
            val peakEndHour = if (manualPeak != null) ((manualPeak + 3) % 24) else (summary?.peakWindowEndHour ?: ((peakHour + 3) % 24))
            val dipEndHour = summary?.dipWindowEndHour ?: 16
            val usableEnergy = summary?.currentSnapshot?.usableEnergy ?: 60

            val now = nowMillis
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)

            val baseDate = task.effectiveScheduledDateMillis()
                ?: (if (task.scheduledTime != null && task.scheduledTime >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS) task.scheduledTime else now)
            val baseCal = Calendar.getInstance().apply {
                timeInMillis = baseDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val baseDayStart = baseCal.timeInMillis

            val targetDate: Long
            val targetHour: Int
            val labelPrefix: String

            val isPeakActive = if (peakHour <= peakEndHour) currentHour in peakHour until peakEndHour else (currentHour >= peakHour || currentHour < peakEndHour)

            if (baseDayStart > todayStart) {
                // Future scheduled date: preserve future day and place at peak hour
                targetDate = baseDayStart
                targetHour = peakHour
                labelPrefix = "Peak Window"
            } else {
                // Today or overdue past task: schedule for today's active/future window, or tomorrow if passed
                val isPeakUpcoming = if (peakHour <= peakEndHour) currentHour < peakHour else (currentHour in peakEndHour until peakHour)
                if (isPeakUpcoming) {
                    targetDate = todayStart
                    targetHour = peakHour
                    labelPrefix = "Peak Window"
                } else if (isPeakActive) {
                    targetDate = todayStart
                    targetHour = currentHour
                    labelPrefix = "Peak Window"
                } else if (currentHour < 18 && usableEnergy >= 55) {
                    targetDate = todayStart
                    targetHour = maxOf(currentHour + 1, dipEndHour).coerceAtMost(17)
                    labelPrefix = "Afternoon Rebound"
                } else {
                    targetDate = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart)
                    targetHour = peakHour
                    labelPrefix = "Tomorrow's Peak"
                }
            }

            // BUG-AUDIT-010 Fix: Preserve minute/second precision from manual peak anchor minute
            val targetMinuteOfDay = if (targetHour == peakHour && prefs.manualPeakProfileEnabled) {
                prefs.manualPeakAnchorMinuteOfDay
            } else {
                targetHour * 60
            }
            val millisInDay = targetMinuteOfDay * 60_000L
            val formattedTime = if (targetMinuteOfDay % 60 != 0) {
                String.format(java.util.Locale.ROOT, "%d:%02d", targetMinuteOfDay / 60, targetMinuteOfDay % 60)
            } else {
                "${targetHour}:00"
            }
            val activeSuffix = if (targetDate == todayStart && isPeakActive && targetHour == currentHour) " Active" else ""
            val messageLabel = "$labelPrefix ($formattedTime$activeSuffix)"
            val updated = task.copy(
                scheduledDate = targetDate,
                scheduledTime = millisInDay,
                isAutoScheduled = false,
                isScheduleLocked = true,
                habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) targetDate else task.habitDate,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
            if (updated.quadrant != Quadrant.ELIMINATE && updated.status == TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
            }
            _lastActionMessage.value = "Shifted '${task.title.take(20)}' to $messageLabel ⚡"
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.update(task)
            val newTags = (task.tags.split(",") + task.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(task)
            if (task.quadrant == Quadrant.ELIMINATE || task.status != TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            } else {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, task) }
            }
        }
    }

    fun toggleMicroStep(taskId: String, stepIndex: Int) {
        viewModelScope.launch {
            val task = taskRepository.getById(taskId) ?: return@launch
            val roomSteps = taskRepository.getMicroSteps(task.id)
            if (stepIndex in roomSteps.indices) {
                taskRepository.toggleMicroStepCompleted(roomSteps[stepIndex])
            }
            val parsedPlan = StepByStepPlanCodec.parse(task.ifThenPlan).toMutableList()
            val updatedPlan = when {
                stepIndex in parsedPlan.indices -> {
                    val step = parsedPlan[stepIndex]
                    parsedPlan[stepIndex] = step.copy(completed = !step.completed)
                    parsedPlan
                }
                parsedPlan.isEmpty() && stepIndex in roomSteps.indices -> {
                    roomSteps.mapIndexed { idx, entity ->
                        StepByStepPlanStep(
                            text = entity.title,
                            completed = if (idx == stepIndex) !entity.isCompleted else entity.isCompleted
                        )
                    }
                }
                else -> parsedPlan
            }
            if (updatedPlan.isNotEmpty()) {
                val serialized = StepByStepPlanCodec.serialize(updatedPlan)
                if (serialized != task.ifThenPlan) {
                    taskRepository.update(
                        task.copy(
                            ifThenPlan = serialized,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            _lastDeferredTask.value = null
            _lastCompletedTask.value = task
            _lastPreCompleteHabitStreak = task.habitStreak
            _lastPreCompleteHabitDate = task.habitDate
            _hasLastPreCompleteHabitDate = true
            val now = System.currentTimeMillis()
            val prefs = preferencesDataStore.preferencesFlow.first()
            val affirmationSuffix = if (prefs.affirmations.isNotEmpty()) {
                " • \"${prefs.affirmations.random()}\""
            } else ""
            _lastActionMessage.value = "Completed: ${task.title}$affirmationSuffix"
            context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            val spawnedId = taskRepository.completeAndRecur(task, now)
            _lastSpawnedRecurringTaskId.value = spawnedId
            calendarSyncRepository?.handleTaskStateChanged(
                task.copy(status = TaskStatus.COMPLETED)
            )

            val newCompleted = prefs.totalTasksCompleted + 1
            val lastActive = prefs.lastActiveDate
            val todayStart = startOfLocalDay(now)
            val isNewDay = lastActive < todayStart
            val isConsecutive = lastActive >= startOfPreviousLocalDay(todayStart) && isNewDay
            val newStreak = when {
                isConsecutive -> prefs.dailyStreak + 1
                isNewDay -> 1
                else -> prefs.dailyStreak
            }
            val longest = maxOf(prefs.longestStreak, newStreak)
            preferencesDataStore.updatePreferences {
                it.copy(
                    totalTasksCompleted = newCompleted,
                    dailyStreak = newStreak,
                    longestStreak = longest,
                    lastActiveDate = now
                )
            }
        }
    }

    fun undoCompleteTask() {
        val task = _lastCompletedTask.value ?: return
        viewModelScope.launch {
            val spawnedId = _lastSpawnedRecurringTaskId.value
            if (spawnedId != null) {
                taskRepository.getById(spawnedId)?.let { taskRepository.delete(it) }
                _lastSpawnedRecurringTaskId.value = null
            }
            val restoredStreak = _lastPreCompleteHabitStreak ?: task.habitStreak
            val restoredHabitDate = if (_hasLastPreCompleteHabitDate) _lastPreCompleteHabitDate else task.habitDate
            _lastPreCompleteHabitStreak = null
            _lastPreCompleteHabitDate = null
            _hasLastPreCompleteHabitDate = false
            val updated = task.copy(
                status = TaskStatus.ACTIVE,
                completedAt = null,
                habitStreak = restoredStreak,
                habitDate = restoredHabitDate,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
            if (updated.quadrant != Quadrant.ELIMINATE && updated.status == TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
            }
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalTasksCompleted = maxOf(0, prefs.totalTasksCompleted - 1))
            }
            _lastCompletedTask.value = null
            _lastActionMessage.value = "Restored: ${task.title}"
        }
    }

    fun deferTask(task: TaskEntity, hours: Int = 2, nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            _lastCompletedTask.value = null
            _lastSpawnedRecurringTaskId.value = null
            val now = nowMillis
            val currentEpoch = task.resolveScheduledEpochMillis(now) ?: now
            _lastDeferredTask.value = Pair(task, task.scheduledTime ?: 0L)
            val baseEpoch = if (task.scheduledTime == null || (task.scheduledDate != null && currentEpoch < now)) now else currentEpoch
            val newEpoch = baseEpoch + hours * 3600_000L
            val (newDate, newTime) = buildNormalizedScheduleTimestamps(newEpoch)
            val updated = task.copy(
                scheduledDate = newDate,
                scheduledTime = newTime,
                isAutoScheduled = false,
                isScheduleLocked = true,
                postponeCount = task.postponeCount + 1,
                habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) newDate else task.habitDate,
                updatedAt = now
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
            if (updated.quadrant != Quadrant.ELIMINATE && updated.status == TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
            }
            _lastActionMessage.value = "Deferred by ${hours}h: ${task.title}"
        }
    }

    fun undoLastAction() {
        if (_lastDeferredTask.value != null) {
            undoDeferTask()
        } else if (_lastCompletedTask.value != null) {
            undoCompleteTask()
        }
    }

    fun undoDeferTask() {
        val pair = _lastDeferredTask.value ?: return
        val (originalTask, _) = pair
        viewModelScope.launch {
            val restored = originalTask.copy(
                scheduledDate = originalTask.scheduledDate,
                scheduledTime = originalTask.scheduledTime,
                postponeCount = originalTask.postponeCount,
                habitDate = originalTask.habitDate,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(restored)
            calendarSyncRepository?.handleTaskStateChanged(restored)
            if (restored.quadrant != Quadrant.ELIMINATE && restored.status == TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, restored) }
            }
            _lastDeferredTask.value = null
            _lastActionMessage.value = "Undid deferral for: ${originalTask.title}"
        }
    }

    fun clearActionMessage() {
        _lastActionMessage.value = null
    }

    fun applyEmergencyReset(resetPlan: com.neuroflow.app.domain.engine.FreshStartEngine.EmergencyResetPlan) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val todayStart = startOfLocalDay(now)
            // 1. Reschedule kept essential tasks for today and reset their postpone count (BUG-131)
            resetPlan.tasksToReschedule.forEach { task ->
                val scheduledTimeOffset = task.scheduledTime?.let { time ->
                    val resolved = task.resolveScheduledEpochMillis()
                    if (resolved != null && resolved < now) null else time
                }
                val updated = task.copy(
                    scheduledDate = todayStart,
                    scheduledTime = scheduledTimeOffset,
                    habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) todayStart else task.habitDate,
                    postponeCount = 0,
                    updatedAt = now
                )
                taskRepository.update(updated)
                calendarSyncRepository?.handleTaskStateChanged(updated)
                if (updated.quadrant != Quadrant.ELIMINATE && updated.status == TaskStatus.ACTIVE) {
                    context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
                }
            }
            // 2. Clear schedules and un-pack calendar for tasks pushed to backlog
            resetPlan.tasksPushedToBacklog.forEach { task ->
                val updated = task.copy(
                    scheduledDate = null,
                    scheduledTime = null,
                    habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) null else task.habitDate,
                    isAutoScheduled = false,
                    postponeCount = 0,
                    updatedAt = now
                )
                taskRepository.update(updated)
                calendarSyncRepository?.deleteTaskEvent(task.id)
                context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            }
            // 3. Protect user streak if streakFreezeApplied is true (BUG-131)
            if (resetPlan.streakFreezeApplied) {
                preferencesDataStore.updatePreferences { prefs ->
                    prefs.copy(lastActiveDate = now)
                }
            }
            _lastCompletedTask.value = null
            _lastDeferredTask.value = null
            _dismissedZeroGuilt.value = true
            _lastActionMessage.value = resetPlan.summaryMessage
        }
    }

    fun freshStartRollover() {
        viewModelScope.launch {
            _lastCompletedTask.value = null
            _lastDeferredTask.value = null
            val now = System.currentTimeMillis()
            val overdue = uiState.value.activeTasks.filter { task ->
                val hasExplicitScheduledDay = task.effectiveScheduledDateMillis() != null ||
                    (task.scheduledTime ?: 0L) >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
                task.postponeCount >= 2 ||
                    (hasExplicitScheduledDay && task.scheduledTime != null && task.resolveScheduledEpochMillis()?.let { it < now - 2 * 3600_000L } == true) ||
                    (task.effectiveScheduledDateMillis() != null && task.effectiveScheduledDateMillis()!! < now - 86_400_000L) ||
                    (task.deadlineDate != null && task.resolveDeadlineEpochMillis()?.let { it < now } == true)
            }
            overdue.forEach { task ->
                taskRepository.update(
                    task.copy(
                        quadrant = Quadrant.ELIMINATE,
                        scheduledTime = null,
                        scheduledDate = null,
                        habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) null else task.habitDate,
                        postponeCount = 0,
                        updatedAt = now
                    )
                )
                calendarSyncRepository?.deleteTaskEvent(task.id)
                context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            }
            _dismissedZeroGuilt.value = true
            _lastActionMessage.value = "Rolled ${overdue.size} tasks into Icebox. Fresh start active! 🌱"
        }
    }

    fun dismissZeroGuiltPrompt() {
        _dismissedZeroGuilt.value = true
    }

    fun breakDownTask(task: TaskEntity) {
        viewModelScope.launch {
            val cleanTitle = task.title.trim().take(30)
            val prefs = preferencesDataStore.preferencesFlow.first()
            val generated = behavioralSupportService.generateMicroStepsWithAi(
                taskTitle = task.title,
                estimatedMinutes = task.estimatedDurationMinutes,
                apiKey = prefs.geminiApiKey,
                useAi = prefs.geminiAiEnabled,
                userDirective = prefs.adaptiveSystemDirective
            )
            if (generated.isNotEmpty()) {
                val microStepEntities = generated.mapIndexed { index, suggestion ->
                    TaskMicroStepEntity(
                        taskId = task.id,
                        title = suggestion.title,
                        sortOrder = index,
                        estimatedMinutes = suggestion.estimatedMinutes
                    )
                }
                taskRepository.deleteMicroStepsByTaskId(task.id)
                taskRepository.insertMicroSteps(microStepEntities)

                val planSteps = generated.map { StepByStepPlanStep(it.title, false) }
                val serialized = StepByStepPlanCodec.serialize(planSteps)
                val updated = task.copy(
                    ifThenPlan = serialized,
                    updatedAt = System.currentTimeMillis()
                )
                taskRepository.update(updated)
                calendarSyncRepository?.handleTaskStateChanged(updated)
                if (updated.quadrant != Quadrant.ELIMINATE && updated.status == TaskStatus.ACTIVE) {
                    context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
                }
                _lastActionMessage.value = "Decomposed '${cleanTitle}' into ${generated.size} micro-steps 🪄"
            }
        }
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun completeEveningShutdown() {
        _eveningShutdownDismissed.value = true
        _lastActionMessage.value = "Evening shutdown complete. Time to rest and restore 🌙"
    }

    fun saveQuickNote(content: String, title: String = "") {
        if (content.isBlank() || noteRepository == null) return
        viewModelScope.launch {
            noteRepository.saveNote(
                NoteEntity(
                    title = title.ifBlank { "Quick Thought" },
                    content = content.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastActionMessage.value = "Note saved to scratchpad 📝"
        }
    }

    suspend fun synthesizeQuickNote(content: String): QuickNoteSynthesis {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return geminiAiOnlineService?.synthesizeQuickNote(
            rawContent = content,
            userDirective = prefs.adaptiveSystemDirective,
            apiKey = prefs.geminiApiKey,
            useAi = prefs.geminiAiEnabled
        ) ?: run {
            val inferred = TaskInferenceEngine.inferFromInput(content)
            QuickNoteSynthesis(
                extractedTaskTitle = inferred.sanitizedTitle.ifBlank { content.take(35) },
                suggestedCategory = inferred.taskType.name,
                estimatedMinutes = inferred.durationMinutes,
                tags = inferred.contextTags,
                firstActionStep = "Review scratchpad note and outline details"
            )
        }
    }

    suspend fun enrichTaskWithAi(taskTitle: String, durationMinutes: Int = 30): com.neuroflow.app.domain.service.TaskEnrichmentResult? {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return geminiAiOnlineService?.enrichTaskWithAi(
            title = taskTitle,
            durationMinutes = durationMinutes,
            userDirective = prefs.adaptiveSystemDirective,
            apiKey = prefs.geminiApiKey,
            useAi = prefs.geminiAiEnabled
        )
    }

    fun logPastTime(task: TaskEntity, minutes: Float) {
        if (minutes.isNaN() || minutes.isInfinite()) return
        val safeMinutes = minutes.coerceAtMost(1440f)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TimeSessionEntity(
                taskId = task.id,
                startedAt = now - (safeMinutes * 60_000f).toLong(),
                endedAt = now,
                durationMinutes = safeMinutes,
                sessionType = "MANUAL_LOG"
            )
            sessionRepository?.insert(session)
            val newTracked = task.totalTimeTrackedMinutes + safeMinutes
            val newCount = task.sessionCount + 1
            val updated = task.copy(
                totalTimeTrackedMinutes = newTracked,
                sessionCount = newCount,
                lastSessionDurationMinutes = safeMinutes,
                actualDurationMinutes = newTracked,
                updatedAt = now
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
            preferencesDataStore.updatePreferences { prefs ->
                val lastActive = prefs.lastActiveDate
                val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
                val isNewDay = lastActive < todayStart
                val isConsecutive = lastActive >= com.neuroflow.app.data.local.entity.startOfPreviousLocalDay(todayStart) && isNewDay
                val newStreak = when {
                    isConsecutive -> prefs.dailyStreak + 1
                    isNewDay -> 1
                    else -> prefs.dailyStreak
                }
                val longest = maxOf(prefs.longestStreak, newStreak)
                prefs.copy(
                    totalFocusMinutes = prefs.totalFocusMinutes + safeMinutes.toInt(),
                    lastActiveDate = now,
                    dailyStreak = newStreak,
                    longestStreak = longest
                )
            }
            _lastActionMessage.value = "Logged ${safeMinutes.toInt()}m past time for ${task.title}"
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
