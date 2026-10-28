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
    private val energyMetricsRepository: EnergyMetricsRepository? = null,
    private val calendarSyncRepository: CalendarSyncRepository? = null
) : ViewModel() {

    private val _dismissedZeroGuilt = MutableStateFlow(false)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _eveningShutdownDismissed = MutableStateFlow(false)
    private val _lastCompletedTask = MutableStateFlow<TaskEntity?>(null)
    private val _lastSpawnedRecurringTaskId = MutableStateFlow<String?>(null)
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
                    old.dailyDeepWorkTargetMinutes == new.dailyDeepWorkTargetMinutes
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
            task.postponeCount >= 2 ||
                (task.scheduledTime != null && task.resolveScheduledEpochMillis()?.let { it < now - 2 * 3600_000L } == true) ||
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
        val actionableActiveTasks = activeTasks.filter { task ->
            val sDate = task.effectiveScheduledDateMillis()
                ?: task.scheduledTime?.takeIf { it >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS }?.let { com.neuroflow.app.data.local.entity.startOfLocalDay(it) }
            val dDate = task.resolveDeadlineEpochMillis()
            val isNotFuture = (sDate != null && sDate <= dayEnd) ||
                (sDate == null && dDate != null && dDate <= dayEnd) ||
                (sDate == null && dDate == null)
            val dependenciesMet = task.dependsOnTaskIds.isBlank() ||
                task.dependsOnTaskIds.split(",").map { it.trim() }.all { it in completedIds }
            val isPart2Blocked = task.id.endsWith("_part2") && (
                (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
                task.id.removeSuffix("_part2") in activeTaskIds
            )
            task.quadrant != Quadrant.ELIMINATE &&
                !isPart2Blocked &&
                task.waitingFor.isBlank() &&
                isNotFuture &&
                dependenciesMet
        }
        val matchingTasks = actionableActiveTasks.filter { it.energyLevel == currentEnergyZone }
        val candidatePool = if (matchingTasks.isNotEmpty()) matchingTasks else actionableActiveTasks
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

        val todayActiveTasks = activeTasks.filter { task ->
            val sDate = task.effectiveScheduledDateMillis()
                ?: task.scheduledTime?.takeIf { it >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS }?.let { com.neuroflow.app.data.local.entity.startOfLocalDay(it) }
            val dDate = task.resolveDeadlineEpochMillis()
            val isPart2Blocked = task.id.endsWith("_part2") && (
                (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
                task.id.removeSuffix("_part2") in activeTaskIds
            )
            task.quadrant != Quadrant.ELIMINATE &&
                ((sDate != null && sDate <= dayEnd) || (sDate == null && dDate != null && dDate <= dayEnd) || (sDate == null && dDate == null && !isPart2Blocked))
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
                ?: task.estimatedDurationMinutes).coerceAtLeast(1)
        }

        val circadianEnergySummary = buildCircadianEnergySummary(
            dailySummary = circadianSummary,
            snapshot = effectiveSnapshot,
            sleepPressurePoints = prefs.sleepPressurePoints,
            dailyDeepWorkTargetMinutes = prefs.dailyDeepWorkTargetMinutes,
            sleepHour = prefs.sleepHour,
            completedDeepWorkMinutes = todayCompletedDeepWorkMinutes
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
            latestSafeStimulantCutoffFormatted = cutoffFormatted
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FlowUiStateV2(isLoading = true)
    )

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            val dayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
            val boundTask = if (task.scheduledDate == null) {
                task.copy(
                    scheduledDate = dayStart,
                    habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) dayStart else task.habitDate
                )
            } else {
                task
            }
            taskRepository.insert(boundTask)
            val newTags = (boundTask.tags.split(",") + boundTask.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(boundTask)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, boundTask) }
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
            val stepList: List<com.neuroflow.app.domain.model.StepByStepPlanStep> = if (synthesis.firstActionStep.isNotBlank()) {
                listOf(com.neuroflow.app.domain.model.StepByStepPlanStep(synthesis.firstActionStep, completed = false))
            } else emptyList()
            val ifThenPlanStr = if (stepList.isNotEmpty()) {
                com.neuroflow.app.domain.model.StepByStepPlanCodec.serialize(stepList)
            } else ""

            val newTask = TaskEntity(
                title = synthesis.extractedTaskTitle.ifBlank { inferred.sanitizedTitle.ifBlank { "Quick Task" } },
                description = rawContent,
                estimatedDurationMinutes = if (synthesis.estimatedMinutes > 0) synthesis.estimatedMinutes else inferred.durationMinutes,
                priority = inferred.priority,
                energyLevel = inferred.energyDemand,
                taskType = inferred.taskType,
                quadrant = inferred.quadrant,
                tags = tagsCombined,
                ifThenPlan = ifThenPlanStr,
                scheduledDate = inferred.scheduledDate ?: todayStart,
                scheduledTime = inferred.scheduledTime,
                deadlineDate = inferred.deadlineDate,
                deadlineTime = inferred.deadlineTime,
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
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, newTask) }
            _lastActionMessage.value = "Created task from note: ${newTask.title.take(24)} ⚡"
        }
    }

    fun addTaskFromNote(title: String, content: String) {
        val inferred = TaskInferenceEngine.inferFromInput(if (title.isNotBlank()) "$title: $content" else content)
        val syn = com.neuroflow.app.domain.service.QuickNoteSynthesis(
            extractedTaskTitle = inferred.sanitizedTitle.ifBlank { title.ifBlank { content.take(35) } },
            suggestedCategory = inferred.taskType.name,
            estimatedMinutes = inferred.durationMinutes,
            tags = inferred.contextTags,
            firstActionStep = ""
        )
        addTaskFromNote(syn, content)
    }

    fun shiftTaskToPeak(task: TaskEntity) {
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            val summary = uiState.value.circadianSummary
            val manualPeak = if (prefs.manualPeakProfileEnabled) (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
            val peakHour = manualPeak ?: summary?.peakWindowStartHour
                ?: (if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart else prefs.peakEnergyStart).coerceIn(0, 23)
            val peakEndHour = summary?.peakWindowEndHour ?: ((peakHour + 3) % 24)
            val dipEndHour = summary?.dipWindowEndHour ?: 16
            val usableEnergy = summary?.currentSnapshot?.usableEnergy ?: 60

            val now = System.currentTimeMillis()
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
            val messageLabel: String

            val isPeakActive = if (peakHour <= peakEndHour) currentHour in peakHour until peakEndHour else (currentHour >= peakHour || currentHour < peakEndHour)

            if (baseDayStart > todayStart) {
                // Future scheduled date: preserve future day and place at peak hour
                targetDate = baseDayStart
                targetHour = peakHour
                messageLabel = "Peak Window (${peakHour}:00)"
            } else {
                // Today or overdue past task: schedule for today's active/future window, or tomorrow if passed
                if (currentHour < peakHour && peakHour <= peakEndHour) {
                    targetDate = todayStart
                    targetHour = peakHour
                    messageLabel = "Peak Window (${peakHour}:00)"
                } else if (isPeakActive) {
                    targetDate = todayStart
                    targetHour = currentHour
                    messageLabel = "Peak Window (${currentHour}:00 Active)"
                } else if (currentHour < 18 && usableEnergy >= 55) {
                    targetDate = todayStart
                    targetHour = maxOf(currentHour + 1, dipEndHour).coerceAtMost(17)
                    messageLabel = "Afternoon Rebound (${targetHour}:00)"
                } else {
                    targetDate = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart)
                    targetHour = peakHour
                    messageLabel = "Tomorrow's Peak (${peakHour}:00)"
                }
            }

            val millisInDay = targetHour * 3600_000L
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
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
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
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, task) }
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            _lastDeferredTask.value = null
            _lastCompletedTask.value = task
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
            val updated = task.copy(
                status = TaskStatus.ACTIVE,
                completedAt = null,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalTasksCompleted = maxOf(0, prefs.totalTasksCompleted - 1))
            }
            _lastCompletedTask.value = null
            _lastActionMessage.value = "Restored: ${task.title}"
        }
    }

    fun deferTask(task: TaskEntity, hours: Int = 2) {
        viewModelScope.launch {
            _lastCompletedTask.value = null
            _lastSpawnedRecurringTaskId.value = null
            val now = System.currentTimeMillis()
            val currentEpoch = task.resolveScheduledEpochMillis(now) ?: now
            _lastDeferredTask.value = Pair(task, task.scheduledTime ?: 0L)
            val newEpoch = currentEpoch + hours * 3600_000L
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
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
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
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, restored) }
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
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
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
                task.postponeCount >= 2 ||
                    (task.scheduledTime != null && task.resolveScheduledEpochMillis()?.let { it < now - 2 * 3600_000L } == true) ||
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
                taskRepository.update(
                    task.copy(
                        ifThenPlan = serialized,
                        updatedAt = System.currentTimeMillis()
                    )
                )
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
