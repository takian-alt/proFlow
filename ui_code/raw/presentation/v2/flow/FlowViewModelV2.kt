package com.neuroflow.app.presentation.v2.flow

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.buildNormalizedScheduleTimestamps
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
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
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
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
import com.neuroflow.app.domain.service.PharmacokineticStimulantCalculator
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import com.neuroflow.app.domain.service.TaskCategory
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class FlowUiStateV2(
    val activeTasks: List<TaskEntity> = emptyList(),
    val filteredTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val energyUi: EnergyUiModel? = null,
    val circadianSummary: DailyCircadianSummary? = null,
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
    val latestSafeStimulantCutoffFormatted: String = "16:11"
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
    private val energyMetricsRepository: EnergyMetricsRepository? = null
) : ViewModel() {

    private val _dismissedZeroGuilt = MutableStateFlow(false)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _eveningShutdownDismissed = MutableStateFlow(false)
    private val _lastCompletedTask = MutableStateFlow<TaskEntity?>(null)
    private val _lastDeferredTask = MutableStateFlow<Pair<TaskEntity, Long>?>(null)
    private val _lastActionMessage = MutableStateFlow<String?>(null)

    val recentNotes: Flow<List<NoteEntity>> = noteRepository?.allNotes ?: flowOf(emptyList())

    val uiState: StateFlow<FlowUiStateV2> = combine(
        combine(
            taskRepository.observeActiveTasks(),
            taskRepository.observeCompletedTasks(),
            preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
                old.wakeUpHour == new.wakeUpHour &&
                    old.sleepHour == new.sleepHour &&
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
                    old.manualPeakAnchorMinuteOfDay == new.manualPeakAnchorMinuteOfDay
            },
            sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())
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
        combine(
            _lastDeferredTask,
            _lastActionMessage
        ) { lastDeferred, actionMsg ->
            Pair(lastDeferred, actionMsg)
        }
    ) { (activeTasks, completedTasks, prefs, sleepLogs), (dismissedGuilt, tag, shutdownDismissed, lastCompleted), (lastDeferred, actionMsg) ->
        val now = System.currentTimeMillis()
        val currentHour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        val systemZone = java.time.ZoneId.systemDefault()
        val zonedDateTime = java.time.Instant.ofEpochMilli(now).atZone(systemZone)
        var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(systemZone)
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

        if (prefs.energyTelemetryEnabled && energyMetricsRepository != null) {
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
            task.postponeCount >= 3 ||
                (task.resolveScheduledEpochMillis()?.let { it < now - 2 * 3600_000L } == true) ||
                (task.scheduledDate != null && task.scheduledDate!! < now - 86_400_000L)
        }
        val showZeroGuilt = overdueTasks.size >= 3 && !dismissedGuilt

        val defaultTags = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin")
        val dynamicTags = (prefs.tagCatalog + activeTasks.flatMap { it.tags.split(",") })
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .map { "#$it" }
        val mergedTags = (defaultTags + dynamicTags).distinctBy { it.lowercase() }

        val matchingTasks = activeTasks.filter { it.energyLevel == currentEnergyZone }
        val candidatePool = if (matchingTasks.isNotEmpty()) matchingTasks else activeTasks
        val effectiveSnapshot = snapshot.copy(usableEnergy = battery)
        val recommended = candidatePool.maxByOrNull { task ->
            val schedulable = SchedulableTask(
                id = task.id,
                title = task.title,
                category = when (task.taskType) {
                    TaskType.ANALYTICAL -> TaskCategory.DEEP_WORK
                    TaskType.CREATIVE -> TaskCategory.CREATIVE
                    TaskType.ADMIN -> TaskCategory.ADMIN
                    TaskType.PHYSICAL -> TaskCategory.EXERCISE
                    TaskType.GENERAL -> TaskCategory.GENERAL
                    TaskType.MINDFULNESS -> TaskCategory.MINDFULNESS
                },
                priority = when (task.priority) {
                    Priority.HIGH -> 2
                    Priority.MEDIUM -> 1
                    else -> 0
                },
                estimatedMinutes = task.estimatedDurationMinutes,
                energyDemand = when (task.energyLevel) {
                    EnergyLevel.HIGH -> EnergyDemand.HIGH
                    EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                    else -> EnergyDemand.LOW
                },
                isFrog = task.isFrog,
                scheduledStartMillis = task.resolveScheduledEpochMillis(),
                deadlineMillis = task.resolveDeadlineEpochMillis(),
                isLocked = task.isScheduleLocked,
                parentTaskId = task.parentTaskId,
                prerequisiteIds = task.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet(),
                impactScore = task.impactScore,
                estimatedEnjoyment = task.enjoymentScore / 10,
                postponeCount = task.postponeCount,
                distractionScore = task.distractionScore
            )
            val baseLeverage = (if (task.isFrog) 1000 else 0) + task.impactScore
            baseLeverage + taskSchedulerService.evaluateTaskScore(schedulable, effectiveSnapshot).totalScore
        }

        val filtered = if (tag.isNullOrBlank() || tag.equals("All", ignoreCase = true)) {
            activeTasks
        } else {
            val query = tag.removePrefix("#").trim()
            activeTasks.filter { task ->
                task.tags.contains(query, ignoreCase = true) ||
                    task.title.contains(query, ignoreCase = true)
            }
        }

        val isEvening = currentHour >= 19

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

        FlowUiStateV2(
            activeTasks = activeTasks,
            filteredTasks = filtered,
            completedTasks = completedTasks,
            energyUi = energyUi,
            circadianSummary = circadianSummary,
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
            latestSafeStimulantCutoffFormatted = cutoffFormatted
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FlowUiStateV2(isLoading = true)
    )

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insert(task)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, task) }
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
            }
        }
    }

    fun clearAllPhysiologicalEvents() {
        viewModelScope.launch {
            preferencesDataStore.clearPhysiologicalLogs()
            _lastActionMessage.value = "Cleared all physiological events 🧹"
        }
    }

    fun addTaskFromNote(title: String, content: String) {
        viewModelScope.launch {
            val fullText = if (title.isNotBlank()) "$title: $content" else content
            val inferred = TaskInferenceEngine.inferFromInput(fullText)
            val now = System.currentTimeMillis()
            val newTask = TaskEntity(
                title = inferred.sanitizedTitle.ifBlank { title.ifBlank { "Quick Task" } },
                description = content,
                estimatedDurationMinutes = inferred.durationMinutes,
                priority = inferred.priority,
                energyLevel = inferred.energyDemand,
                taskType = inferred.taskType,
                quadrant = inferred.quadrant,
                tags = inferred.contextTags.joinToString(","),
                scheduledDate = inferred.scheduledDate ?: now,
                scheduledTime = inferred.scheduledTime,
                deadlineDate = inferred.deadlineDate,
                deadlineTime = inferred.deadlineTime,
                isFrog = inferred.isFrog,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insert(newTask)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, newTask) }
            _lastActionMessage.value = "Created task from note: ${newTask.title.take(24)} ⚡"
        }
    }

    fun shiftTaskToPeak(task: TaskEntity) {
        viewModelScope.launch {
            val peakHour = uiState.value.circadianSummary?.peakWindowStartHour
                ?: uiState.value.preferences.peakEnergyStart.takeIf { it > 0 } ?: 10
            val peakEndHour = uiState.value.circadianSummary?.peakWindowEndHour ?: (peakHour + 3)
            val dipEndHour = uiState.value.circadianSummary?.dipWindowEndHour ?: 16
            val usableEnergy = uiState.value.circadianSummary?.currentSnapshot?.usableEnergy ?: 60

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)

            val baseDate = task.scheduledDate ?: now
            val baseCal = Calendar.getInstance().apply {
                timeInMillis = baseDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val isToday = baseCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                baseCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

            val targetDate: Long
            val targetHour: Int
            val messageLabel: String

            if (!isToday || currentHour < peakEndHour) {
                targetDate = baseCal.timeInMillis
                targetHour = peakHour
                messageLabel = "Peak Window (${peakHour}:00)"
            } else if (currentHour < 18 && usableEnergy >= 55) {
                targetDate = baseCal.timeInMillis
                targetHour = maxOf(currentHour + 1, dipEndHour).coerceAtMost(17)
                messageLabel = "Afternoon Rebound (${targetHour}:00)"
            } else {
                baseCal.add(Calendar.DAY_OF_YEAR, 1)
                targetDate = baseCal.timeInMillis
                targetHour = peakHour
                messageLabel = "Tomorrow's Peak (${peakHour}:00)"
            }

            val millisInDay = targetHour * 3600_000L
            val updated = task.copy(
                scheduledDate = targetDate,
                scheduledTime = millisInDay,
                isScheduleLocked = true,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            _lastActionMessage.value = "Shifted '${task.title.take(20)}' to $messageLabel ⚡"
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.update(task)
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            _lastCompletedTask.value = task
            val prefs = preferencesDataStore.preferencesFlow.first()
            val affirmationSuffix = if (prefs.affirmations.isNotEmpty()) {
                " • \"${prefs.affirmations.random()}\""
            } else ""
            _lastActionMessage.value = "Completed: ${task.title}$affirmationSuffix"
            context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            taskRepository.completeAndRecur(task, System.currentTimeMillis())
        }
    }

    fun undoCompleteTask() {
        val task = _lastCompletedTask.value ?: return
        viewModelScope.launch {
            taskRepository.update(
                task.copy(
                    status = TaskStatus.ACTIVE,
                    completedAt = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastCompletedTask.value = null
            _lastActionMessage.value = "Restored: ${task.title}"
        }
    }

    fun deferTask(task: TaskEntity, hours: Int = 2) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val currentEpoch = task.resolveScheduledEpochMillis(now) ?: now
            _lastDeferredTask.value = Pair(task, task.scheduledTime ?: 0L)
            val newEpoch = currentEpoch + hours * 3600_000L
            val (newDate, newTime) = buildNormalizedScheduleTimestamps(newEpoch)
            taskRepository.update(
                task.copy(
                    scheduledDate = newDate,
                    scheduledTime = newTime,
                    postponeCount = task.postponeCount + 1,
                    updatedAt = now
                )
            )
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, task) }
            _lastActionMessage.value = "Deferred by ${hours}h: ${task.title}"
        }
    }

    fun undoDeferTask() {
        val pair = _lastDeferredTask.value ?: return
        val (originalTask, _) = pair
        viewModelScope.launch {
            taskRepository.update(
                originalTask.copy(
                    scheduledDate = originalTask.scheduledDate,
                    scheduledTime = originalTask.scheduledTime,
                    postponeCount = (originalTask.postponeCount - 1).coerceAtLeast(0),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastDeferredTask.value = null
            _lastActionMessage.value = "Undid deferral for: ${originalTask.title}"
        }
    }

    fun clearActionMessage() {
        _lastActionMessage.value = null
    }

    fun freshStartRollover() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val overdue = uiState.value.activeTasks.filter { task ->
                task.postponeCount >= 3 ||
                    (task.resolveScheduledEpochMillis()?.let { it < now - 2 * 3600_000L } == true) ||
                    (task.scheduledDate != null && task.scheduledDate!! < now - 86_400_000L)
            }
            overdue.forEach { task ->
                taskRepository.update(
                    task.copy(
                        quadrant = Quadrant.ELIMINATE,
                        scheduledTime = null,
                        scheduledDate = null,
                        postponeCount = 0,
                        updatedAt = now
                    )
                )
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
                useAi = prefs.geminiAiEnabled
            )
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
            taskRepository.update(
                task.copy(
                    ifThenPlan = serialized,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastActionMessage.value = "Decomposed '${cleanTitle}' into ${generated.size} micro-steps 🪄"
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
            taskRepository.update(
                task.copy(
                    totalTimeTrackedMinutes = newTracked,
                    sessionCount = newCount,
                    lastSessionDurationMinutes = safeMinutes,
                    actualDurationMinutes = newTracked,
                    updatedAt = now
                )
            )
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalFocusMinutes = prefs.totalFocusMinutes + safeMinutes.toInt())
            }
            _lastActionMessage.value = "Logged ${safeMinutes.toInt()}m past time for ${task.title}"
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
