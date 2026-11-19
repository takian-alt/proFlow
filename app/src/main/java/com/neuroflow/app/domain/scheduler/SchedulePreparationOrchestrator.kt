package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.calendar.ExternalCalendarDelta
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
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
import kotlin.math.abs
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

data class ReconciliationResult(
    val nudgedTasks: List<TaskEntity>,
    val conflictedTasks: List<TaskEntity>
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
        microStepCounts: Map<String, Pair<Int, Int>> = emptyMap(),
        dailyCognitiveCapacityMinutes: Int? = null
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
        microStepCounts = microStepCounts,
        dailyCognitiveCapacityMinutes = dailyCognitiveCapacityMinutes
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
        microStepCounts: Map<String, Pair<Int, Int>> = emptyMap(),
        dailyCognitiveCapacityMinutes: Int? = null
    ): OrchestrationOutcome {
        if (candidateTasks.isEmpty()) return OrchestrationOutcome(emptyList())

        val horizonCtx = SchedulingContextFactory.buildHorizonContext(
            allKnownTasks = allKnownTasks,
            closedSessions = closedSessions,
            sleepLogs = sleepLogs,
            prefs = prefs,
            referenceNowMillis = nowMillis,
            zoneId = zoneId
        )
        val dayCtx = SchedulingContextFactory.buildDayContext(
            horizon = horizonCtx,
            dayDate = dayDate,
            prefs = prefs,
            referenceNowMillis = nowMillis,
            zoneId = zoneId,
            sleepTelemetryRepository = sleepTelemetryRepository
        )

        val rawSchedulable = candidateTasks.map { task ->
            SchedulableTaskMapper.toSchedulableTask(
                task = task,
                allContextTasks = candidateTasks,
                dayStartMillis = dayCtx.dayStartMillis,
                enjoymentCalibration = horizonCtx.enjoymentCalibration,
                activeTaskId = activeTaskId,
                microStepCounts = microStepCounts
            )
        }

        val sanitizedDag = DependencyGraphValidator.sanitizeDag(rawSchedulable).tasks
        val preparedTasks = if (forceReplan) {
            behavioralSupportService.resetScheduleAnchors(sanitizedDag)
        } else {
            sanitizedDag
        }

        val effectiveDailyCapacity = dailyCognitiveCapacityMinutes
            ?: prefs.dailyDeepWorkTargetMinutes.takeIf { it > 0 }

        val outcome = taskSchedulerService.scheduleDayWithOutcome(
            tasks = preparedTasks,
            dayDate = dayDate,
            nowMillis = nowMillis,
            wakeTimeMillis = dayCtx.wakeTimeMillis,
            lastSleepDurationMinutes = dayCtx.lastSleepDurationMinutes,
            busyWindows = busyWindows,
            protectedSlots = dayCtx.protectedSlots,
            calibrationRatio = horizonCtx.globalCalibrationRatio,
            forceReplan = forceReplan,
            zoneId = zoneId,
            naps = dayCtx.naps,
            chronotype = horizonCtx.chronotype,
            manualPeakStartHour = horizonCtx.manualPeakStartHour,
            priorityWeights = horizonCtx.priorityWeights,
            learnedProfile = horizonCtx.learnedProfile,
            rollingAverageSleepMinutes = horizonCtx.rollingAverageSleepMinutes,
            physiologicalBlocks = dayCtx.physiologicalBlocks,
            stimulantDoses = dayCtx.stimulantDoses,
            socialJetlagLagHours = horizonCtx.socialJetlagLagHours,
            activeUncompletedTaskIds = horizonCtx.activeUncompletedTaskIds,
            workDayStartHour = prefs.workDayStart,
            workDayEndHour = prefs.workDayEnd,
            dailyCognitiveCapacityMinutes = effectiveDailyCapacity,
            rejectionPatternsJson = prefs.schedulingRejectionsJson,
            categoryCalibrationRatios = horizonCtx.categoryCalibrationRatios
        )

        return OrchestrationOutcome(outcome.placements, outcome.deferrals)
    }

    /**
     * Reconciles scheduled tasks with external calendar deltas and busy windows using a zero-jitter,
     * minimum-displacement nudge policy.
     *
     * - Tasks that do NOT overlap external events remain completely untouched (zero jitter).
     * - Overlapping tasks are nudged to the nearest contiguous gap >= task duration within wake-bed hours,
     *   minimizing |newStart - originalScheduledTime|.
     * - If no gap fits before bedtime, tasks are placed in [ReconciliationResult.conflictedTasks]
     *   non-destructively without thrashing other placements.
     */
    fun reconcileExternalCalendarDeltas(
        dayDateMillis: Long,
        tasks: List<TaskEntity>,
        deltas: List<ExternalCalendarDelta>,
        busyWindows: List<TimeSlotWindow>,
        wakeHour: Int = 7,
        bedHour: Int = 23
    ): ReconciliationResult {
        if (tasks.isEmpty()) return ReconciliationResult(emptyList(), emptyList())

        val dayStart = startOfLocalDay(dayDateMillis)
        val wakeTimeMillis = dayStart + wakeHour.coerceIn(0, 23) * 3600_000L
        val bedTimeMillis = dayStart + bedHour.coerceIn(wakeHour + 1, 24) * 3600_000L

        // Gather all external busy windows (from busyWindows and deltas)
        val externalBusyWindows = mutableListOf<TimeSlotWindow>()
        externalBusyWindows.addAll(busyWindows.filter { it.endMillis > it.startMillis })

        for (delta in deltas) {
            val start = delta.startMillis ?: delta.newStartMillis
            val end = delta.endMillis ?: delta.newEndMillis
            if (start != null && end != null && end > start) {
                externalBusyWindows.add(TimeSlotWindow(start, end))
            }
        }

        data class TimedTaskInfo(
            val task: TaskEntity,
            val startMillis: Long,
            val endMillis: Long,
            val durationMillis: Long,
            val isOverlappingExternal: Boolean
        )

        val timedInfos = tasks.mapNotNull { task ->
            if (task.scheduledTime == null) return@mapNotNull null
            val start = task.resolveScheduledEpochMillis(dayStart) ?: return@mapNotNull null
            val durationMin = TaskBlockPlan.plannedMinutes(task).coerceAtLeast(1)
            val durationMs = durationMin * 60_000L
            val end = start + durationMs

            // Check if this task overlaps with any external busy window.
            // A task does not collide with its own external MOVED delta.
            val overlapsExternal = externalBusyWindows.any { win ->
                val isOwnDelta = deltas.any { d ->
                    d.taskId.isNotBlank() && d.taskId == task.id &&
                        (d.startMillis ?: d.newStartMillis) == win.startMillis &&
                        (d.endMillis ?: d.newEndMillis) == win.endMillis
                }
                if (isOwnDelta) false
                else maxOf(start, win.startMillis) < minOf(end, win.endMillis)
            }

            TimedTaskInfo(
                task = task,
                startMillis = start,
                endMillis = end,
                durationMillis = durationMs,
                isOverlappingExternal = overlapsExternal
            )
        }

        val nonOverlappingTasks = timedInfos.filter { !it.isOverlappingExternal }
        val overlappingTasks = timedInfos.filter { it.isOverlappingExternal }

        if (overlappingTasks.isEmpty()) {
            return ReconciliationResult(emptyList(), emptyList())
        }

        // Active occupied intervals start with external busy windows + non-overlapping tasks
        val occupiedWindows = mutableListOf<TimeSlotWindow>()
        occupiedWindows.addAll(externalBusyWindows)
        for (info in nonOverlappingTasks) {
            occupiedWindows.add(TimeSlotWindow(info.startMillis, info.endMillis))
        }

        val nudgedTasks = mutableListOf<TaskEntity>()
        val conflictedTasks = mutableListOf<TaskEntity>()

        for (info in overlappingTasks) {
            val task = info.task
            val origStart = info.startMillis
            val durationMs = info.durationMillis

            // Calculate free gaps within [wakeTimeMillis, bedTimeMillis] given currently occupiedWindows
            val gaps = computeFreeGaps(wakeTimeMillis, bedTimeMillis, occupiedWindows)

            // Filter gaps that can fit this task
            val candidatePlacements = gaps.filter { gap -> (gap.endMillis - gap.startMillis) >= durationMs }
                .map { gap ->
                    val candidateStart = origStart.coerceIn(gap.startMillis, gap.endMillis - durationMs)
                    val displacement = abs(candidateStart - origStart)
                    val isForward = candidateStart >= origStart
                    Triple(candidateStart, displacement, isForward)
                }

            if (candidatePlacements.isEmpty()) {
                // No gap fits before bedtime: mark as conflicted non-destructively
                conflictedTasks.add(task)
            } else {
                // Select gap minimizing displacement; prefer forward on tie
                val best = candidatePlacements.minWith(
                    compareBy<Triple<Long, Long, Boolean>> { it.second }
                        .thenBy { if (it.third) 0 else 1 }
                        .thenBy { it.first }
                )

                val newStart = best.first
                val newScheduledTime = if (task.scheduledTime != null && task.scheduledTime < ONE_DAY_MILLIS) {
                    newStart - dayStart
                } else {
                    newStart
                }

                val nudged = task.copy(scheduledTime = newScheduledTime)
                nudgedTasks.add(nudged)

                // Add newly placed window to occupiedWindows so subsequent nudges don't collide
                occupiedWindows.add(TimeSlotWindow(newStart, newStart + durationMs))
            }
        }

        return ReconciliationResult(nudgedTasks = nudgedTasks, conflictedTasks = conflictedTasks)
    }

    private fun computeFreeGaps(
        wakeMillis: Long,
        bedMillis: Long,
        occupied: List<TimeSlotWindow>
    ): List<TimeSlotWindow> {
        if (wakeMillis >= bedMillis) return emptyList()

        // Filter and clamp occupied windows to [wakeMillis, bedMillis]
        val clamped = occupied.mapNotNull { win ->
            val s = maxOf(win.startMillis, wakeMillis)
            val e = minOf(win.endMillis, bedMillis)
            if (e > s) TimeSlotWindow(s, e) else null
        }.sortedBy { it.startMillis }

        if (clamped.isEmpty()) {
            return listOf(TimeSlotWindow(wakeMillis, bedMillis))
        }

        // Merge overlapping/contiguous blocked windows
        val mergedBlocked = mutableListOf<TimeSlotWindow>()
        var curStart = clamped[0].startMillis
        var curEnd = clamped[0].endMillis

        for (i in 1 until clamped.size) {
            val next = clamped[i]
            if (next.startMillis <= curEnd) {
                curEnd = maxOf(curEnd, next.endMillis)
            } else {
                mergedBlocked.add(TimeSlotWindow(curStart, curEnd))
                curStart = next.startMillis
                curEnd = next.endMillis
            }
        }
        mergedBlocked.add(TimeSlotWindow(curStart, curEnd))

        // Invert mergedBlocked to produce free gaps
        val gaps = mutableListOf<TimeSlotWindow>()
        var cursor = wakeMillis

        for (blocked in mergedBlocked) {
            if (blocked.startMillis > cursor) {
                gaps.add(TimeSlotWindow(cursor, blocked.startMillis))
            }
            cursor = maxOf(cursor, blocked.endMillis)
        }

        if (cursor < bedMillis) {
            gaps.add(TimeSlotWindow(cursor, bedMillis))
        }

        return gaps
    }

    private fun cleanSplitTaskId(id: String): String =
        id.replace(Regex("""[#_]part\d+.*$"""), "")
}
