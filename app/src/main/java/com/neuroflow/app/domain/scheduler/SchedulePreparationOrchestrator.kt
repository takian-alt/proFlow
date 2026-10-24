package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.PriorityWeightProfile
import com.neuroflow.app.domain.service.ProtectedLocalSlot
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import kotlin.math.roundToInt
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class OrchestrationOutcome(
    val placements: List<ScheduledPlacement>,
    val deferrals: List<DeferralRationale> = emptyList()
)

/**
 * ADR-020: Unified Foreground & Background Schedule Preparation Orchestrator.
 *
 * Eliminates the split-brain divergence between `PlanViewModelV2.autoScheduleDay` and
 * `ScheduleAutoTasksWorker.doWork` by running the exact same Bayesian calibration,
 * 32-tag profile mapping, cycle-safe DAG sanitization, and Kalman circadian learning pipeline.
 */
@Singleton
class SchedulePreparationOrchestrator @Inject constructor(
    private val taskSchedulerService: TaskSchedulerService,
    private val behavioralSupportService: BehavioralSupportService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) {
    fun orchestrateDaySchedule(
        candidateTasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity>,
        closedSessions: List<TimeSessionEntity>,
        sleepLogs: List<SleepLogEntity>,
        prefs: UserPreferences,
        dayDate: LocalDate,
        nowMillis: Long,
        busyWindows: List<TimeSlotWindow>,
        forceReplan: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activeTaskId: String? = null,
        microStepCounts: Map<String, Pair<Int, Int>> = emptyMap()
    ): List<ScheduledPlacement> = orchestrateDayScheduleWithOutcome(
        candidateTasks = candidateTasks,
        allKnownTasks = allKnownTasks,
        closedSessions = closedSessions,
        sleepLogs = sleepLogs,
        prefs = prefs,
        dayDate = dayDate,
        nowMillis = nowMillis,
        busyWindows = busyWindows,
        forceReplan = forceReplan,
        zoneId = zoneId,
        activeTaskId = activeTaskId,
        microStepCounts = microStepCounts
    ).placements

    fun orchestrateDayScheduleWithOutcome(
        candidateTasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity>,
        closedSessions: List<TimeSessionEntity>,
        sleepLogs: List<SleepLogEntity>,
        prefs: UserPreferences,
        dayDate: LocalDate,
        nowMillis: Long,
        busyWindows: List<TimeSlotWindow>,
        forceReplan: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activeTaskId: String? = null,
        microStepCounts: Map<String, Pair<Int, Int>> = emptyMap()
    ): OrchestrationOutcome {
        if (candidateTasks.isEmpty()) return OrchestrationOutcome(emptyList())

        val planningCalibration = AnalyticsEngine.PlanningCalibration.from(allKnownTasks)
        val enjoymentCalibration = AnalyticsEngine.EnjoymentCalibration.from(allKnownTasks)
        val globalCalibrationRatio = planningCalibration.globalFactor.toDouble().coerceIn(0.8, 1.6)

        val rawSchedulable = candidateTasks.map { task ->
            val resolvedCategory = when (task.determineCategory()) {
                com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK,
                com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL -> TaskCategory.DEEP_WORK
                com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE -> TaskCategory.CREATIVE
                com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE -> TaskCategory.ADMIN
                com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE,
                com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL -> TaskCategory.EXERCISE
                com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS -> TaskCategory.MINDFULNESS
                com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE -> TaskCategory.GENERAL
            }
            val tagProfiles = TaskTagSchedulingProfile.profilesFor(task.tags)
            val fragTolerance = tagProfiles.minOfOrNull { it.fragmentationTolerance } ?: 0.5f
            val inferredContext = task.contextTag.ifBlank {
                tagProfiles.firstNotNullOfOrNull { it.preferredContext }.orEmpty()
            }
            val calibratedEnjoyment = (enjoymentCalibration.forecast(task.enjoymentScore) / 10.0).roundToInt().coerceIn(0, 10)

            val expectedDuration = TaskBlockPlan.expectedMinutes(task)
            val isDurationExplicit = task.isScheduleLocked

            SchedulableTask(
                id = task.id,
                title = task.title,
                category = resolvedCategory,
                priority = when (task.priority) {
                    Priority.HIGH -> 2
                    Priority.MEDIUM -> 1
                    else -> 0
                },
                estimatedMinutes = expectedDuration,
                isDurationExplicit = isDurationExplicit,
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
                completedMicroSteps = microStepCounts[task.id]?.first ?: 0,
                totalMicroSteps = microStepCounts[task.id]?.second ?: 0,
                isCurrentlyActive = (activeTaskId != null && (task.id == activeTaskId || task.id.startsWith("$activeTaskId#"))),
                createdAtMillis = task.createdAt,
                prerequisiteIds = task.dependsOnTaskIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toSet(),
                impactScore = task.impactScore,
                estimatedEnjoyment = calibratedEnjoyment,
                postponeCount = task.postponeCount,
                contextTag = inferredContext,
                isAnxietyTask = task.isAnxietyTask,
                hasImplementationIntention = task.ifThenPlan.isNotBlank(),
                fragmentationTolerance = fragTolerance,
                distractionScore = task.distractionScore
            )
        }

        val sanitizedDag = DependencyGraphValidator.sanitizeDag(rawSchedulable).tasks
        val preparedTasks = if (forceReplan) {
            behavioralSupportService.resetScheduleAnchors(sanitizedDag)
        } else {
            sanitizedDag
        }

        val zonedNow = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var wakeAnchor = zonedNow.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
        if (wakeAnchor.toInstant().toEpochMilli() > nowMillis) {
            wakeAnchor = wakeAnchor.minusDays(1)
        }
        val baseWakeMillis = wakeAnchor.toInstant().toEpochMilli()

        val sleepSignal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )
        val wakeTimeMillis = sleepSignal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val lastSleepDurationMinutes = sleepSignal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
        val rollingAvgSleep = computeRollingDailySleepAverage(sleepLogs, nowMillis, zoneId)

        val dayStartMillis = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Bug 1 Fix: filter protected slots by day of week & add recovery buffer
        val protectedSlots = prefs.protectedTimeSlots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayStartMillis) }
            .map {
                ProtectedLocalSlot(
                    startHour = (it.startMinuteOfDay / 60).coerceIn(0, 23),
                    startMinute = (it.startMinuteOfDay % 60).coerceIn(0, 59),
                    durationMinutes = (it.durationMinutes + it.recoveryBufferMinutes).coerceIn(15, 480)
                )
            }

        // Bug 3 Fix: parse quick-logged physiological events (meals, workouts, naps) and stimulants
        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis in (dayStartMillis - 8 * 3600_000L)..(dayStartMillis + 26 * 3600_000L) }
        val quickPhysBlocks = physLogs.mapNotNull { log ->
            val logZdt = Instant.ofEpochMilli(log.timestampMillis).atZone(zoneId)
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
        val quickPhysNaps = physLogs
            .filter { it.type == PhysiologicalEventType.NAP }
            .map { log ->
                val napDuration = log.durationMinutes.coerceIn(5, 180)
                DaytimeNapBout(
                    startMillis = log.timestampMillis,
                    endMillis = log.timestampMillis + napDuration * 60_000L
                )
            }

        val allNaps = ((sleepSignal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }
        val basePhysioBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = dayDate,
            zoneId = zoneId
        ).filter { it.category == CognitiveBlockCategory.MEAL || it.category == CognitiveBlockCategory.EXERCISE }
        val physiologicalBlocks = basePhysioBlocks + quickPhysBlocks
        val stimulantDoses = (
            parseStimulantLogs(prefs.stimulantLogsJson) +
            com.neuroflow.app.domain.model.extractStimulantDosesFromSlots(prefs.protectedTimeSlots, dayDate, zoneId)
        ).distinctBy { it.timestampMillis to it.type }

        // Bug 4 Fix: pass active uncompleted task IDs to prevent premature placement of cross-day dependents
        val activeUncompletedTaskIds = allKnownTasks
            .filter { it.status == TaskStatus.ACTIVE }
            .map { it.id }
            .toSet()

        // Feature 3 (ADR-023): Social Jetlag Phase-Shift Auto-Pacing
        val sleepWindows = sleepLogs.map { TimeSlotWindow(it.startAt, it.endAt) }
        val sjlReport = SocialJetlagCalculator.evaluateFromSleepWindows(
            sleepWindows = sleepWindows,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        val socialJetlagLagHours = sjlReport.mondayMorningPhaseLagHours

        val chronotype = ChronotypeProfile.fromString(prefs.quizChronotype ?: prefs.manualChronotype)
        val manualPeakStartHour = if (prefs.manualPeakProfileEnabled) {
            (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
        } else null

        val completedTasks = allKnownTasks.filter { it.status == TaskStatus.COMPLETED }
        val observations = buildBehavioralObservations(completedTasks, closedSessions, zoneId)
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (observations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
        } else null

        val weights = PriorityWeightProfile(
            urgencyWeight = prefs.weightDeadlineUrgency.toDouble(),
            impactWeight = prefs.weightImpact.toDouble(),
            circadianMatchWeight = prefs.weightCircadianMatch.toDouble(),
            momentumWeight = prefs.weightSessionProgress.toDouble(),
            frogBonusMultiplier = prefs.weightFrogBonus.toDouble(),
            quadrantWeight = prefs.weightQuadrant.toDouble(),
            priorityLevelWeight = prefs.weightPriorityLevel.toDouble(),
            durationWeight = prefs.weightDuration.toDouble(),
            focusModeWeight = prefs.weightFocusMode.toDouble(),
            planBoostWeight = prefs.weightPlanBoost.toDouble(),
            enjoymentWeight = prefs.weightEnjoyment.toDouble()
        )

        val outcome = taskSchedulerService.scheduleDayWithOutcome(
            tasks = preparedTasks,
            dayDate = dayDate,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            busyWindows = busyWindows,
            protectedSlots = protectedSlots,
            calibrationRatio = globalCalibrationRatio,
            forceReplan = forceReplan,
            zoneId = zoneId,
            naps = allNaps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            priorityWeights = weights,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAvgSleep,
            physiologicalBlocks = physiologicalBlocks,
            stimulantDoses = stimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours,
            activeUncompletedTaskIds = activeUncompletedTaskIds
        )

        return OrchestrationOutcome(outcome.placements, outcome.deferrals)
    }
}
