package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.assistant.BurnoutGuardEngine
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.scheduler.SchedulableTaskMapper
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class FatigueWarningLevel {
    OPTIMAL,
    RESTORATIVE,
    MODERATE_STRAIN,
    HIGH_BURNOUT_RISK
}

data class TaskFatigueScoreUi(
    val taskId: String,
    val priorityScore: Int,
    val basePriorityScore: Int,
    val fatigueAdjustment: Double,
    val fatigueIndex: Double,
    val cognitiveLoadIntensity: Double,
    val priorLongSessionCount: Int,
    val priorSessionMinutes: Int,
    val cumulativeLoadMinutes: Double,
    val warningLevel: FatigueWarningLevel,
    val warningReason: String,
    val isHighLoadTask: Boolean
)

/**
 * Pure, real-time evaluator that computes per-task Fatigue-Aware priority scores and burnout
 * warning states by combining:
 * 1. Completed and active focus sessions on the target day.
 * 2. Earlier scheduled tasks on the target day's timeline.
 * 3. The task's own duration and cognitive load intensity.
 * 4. The biological circadian snapshot at the task's scheduled midpoint (or current time).
 */
object FatigueScoreEvaluator {

    const val LONG_SESSION_THRESHOLD_MINUTES = 45

    private data class PriorWorkBlock(
        val taskId: String?,
        val startMillis: Long,
        val endMillis: Long,
        val durationMinutes: Int,
        val schedulableTask: SchedulableTask,
        val isActualSession: Boolean
    )

    fun evaluateTasks(
        tasks: List<TaskEntity>,
        allContextTasks: List<TaskEntity>,
        completedTasks: List<TaskEntity>,
        allSessions: List<TimeSessionEntity>,
        dayStartMillis: Long,
        dayEndMillis: Long,
        nowMillis: Long,
        timeline: List<CircadianSnapshot>,
        fallbackSnapshot: CircadianSnapshot,
        prefs: UserPreferences,
        taskSchedulerService: TaskSchedulerService,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Map<String, TaskFatigueScoreUi> {
        if (tasks.isEmpty()) return emptyMap()

        val planningCalibration = AnalyticsEngine.PlanningCalibration.from(completedTasks + allContextTasks)
        val enjoymentCalibration = AnalyticsEngine.EnjoymentCalibration.from(completedTasks + allContextTasks)
        val calibrationRatio = planningCalibration.globalFactor.toDouble().coerceIn(0.8, 1.6)
        val priorityWeights = SchedulableTaskMapper.toPriorityWeights(prefs)

        val allById = (allContextTasks + completedTasks + tasks).associateBy { it.id }

        val combinedTasksForDeps = (tasks + allContextTasks).distinctBy { it.id }
        val schedulablesById = combinedTasksForDeps.associate { task ->
            task.id to SchedulableTaskMapper.toSchedulableTask(
                task = task,
                allContextTasks = combinedTasksForDeps,
                dayStartMillis = dayStartMillis,
                enjoymentCalibration = enjoymentCalibration,
                usePlannedBlockForScoring = true
            )
        }
        val dependentCounts = SchedulableTaskMapper.computeDependentCounts(schedulablesById.values.toList())

        // 1. Collect actual sessions recorded within [dayStartMillis .. dayEndMillis]
        val daySessions = allSessions
            .filter { it.startedAt in dayStartMillis..dayEndMillis }
            .sortedBy { it.startedAt }

        val sessionTaskIds = daySessions.map { it.taskId }.toSet()

        val actualSessionBlocks = daySessions.mapNotNull { session ->
            val dur = session.trackedMinutes(nowMillis).roundToInt()
            if (dur <= 0) return@mapNotNull null
            val end = session.endedAt ?: max(session.startedAt + dur * 60_000L, nowMillis)
            val linkedTask = allById[session.taskId]
            val schedulable = if (linkedTask != null) {
                schedulablesById[linkedTask.id] ?: SchedulableTaskMapper.toSchedulableTask(
                    task = linkedTask,
                    allContextTasks = combinedTasksForDeps,
                    dayStartMillis = dayStartMillis,
                    enjoymentCalibration = enjoymentCalibration,
                    usePlannedBlockForScoring = true
                )
            } else {
                SchedulableTask(
                    id = session.id,
                    title = "Focus Session",
                    category = TaskCategory.DEEP_WORK,
                    priority = 1,
                    estimatedMinutes = dur,
                    energyDemand = EnergyDemand.MODERATE
                )
            }
            PriorWorkBlock(
                taskId = session.taskId,
                startMillis = session.startedAt,
                endMillis = end,
                durationMinutes = dur,
                schedulableTask = schedulable,
                isActualSession = true
            )
        }

        // Also include tasks completed today that have no explicit TimeSessionEntity rows
        val completedWithoutSessionBlocks = completedTasks.mapNotNull { completed ->
            val completionTime = completed.completedAt ?: completed.updatedAt
            if (completionTime !in dayStartMillis..dayEndMillis) return@mapNotNull null
            if (completed.id in sessionTaskIds) return@mapNotNull null
            val dur = (completed.actualDurationMinutes?.roundToInt()
                ?: completed.totalTimeTrackedMinutes.roundToInt().takeIf { it > 0 }
                ?: TaskBlockPlan.plannedMinutes(completed)).coerceAtLeast(1)
            val start = completed.resolveScheduledEpochMillis(dayStartMillis)
                ?: (completionTime - dur * 60_000L)
            val end = max(start + dur * 60_000L, completionTime)
            val schedulable = SchedulableTaskMapper.toSchedulableTask(
                task = completed,
                allContextTasks = combinedTasksForDeps,
                dayStartMillis = dayStartMillis,
                enjoymentCalibration = enjoymentCalibration,
                usePlannedBlockForScoring = true
            )
            PriorWorkBlock(
                taskId = completed.id,
                startMillis = start,
                endMillis = end,
                durationMinutes = dur,
                schedulableTask = schedulable,
                isActualSession = true
            )
        }

        val realizedBlocks = (actualSessionBlocks + completedWithoutSessionBlocks)
            .sortedBy { it.startMillis }

        // 2. Build scheduled planned blocks on this day for future-slot fatigue propagation
        val scheduledBlocks = tasks.mapNotNull { task ->
            val start = resolveTaskStartMillis(task, dayStartMillis, zoneId) ?: return@mapNotNull null
            val dur = TaskBlockPlan.plannedMinutes(task).coerceAtLeast(5)
            val schedulable = schedulablesById[task.id] ?: return@mapNotNull null
            PriorWorkBlock(
                taskId = task.id,
                startMillis = start,
                endMillis = start + dur * 60_000L,
                durationMinutes = dur,
                schedulableTask = schedulable,
                isActualSession = false
            )
        }.sortedBy { it.startMillis }

        val latestRealizedEnd = realizedBlocks.maxOfOrNull { it.endMillis } ?: nowMillis
        val todayRestLogs = BurnoutGuardEngine.collectTodayRestLogs(prefs, dayStartMillis, max(nowMillis, dayEndMillis))

        return tasks.associate { task ->
            val schedulable = schedulablesById[task.id]!!
            val taskStart = resolveTaskStartMillis(task, dayStartMillis, zoneId)
            val plannedMins = TaskBlockPlan.plannedMinutes(task).coerceAtLeast(5)
            val evalTime = taskStart ?: max(nowMillis, latestRealizedEnd)
            val evalMidpoint = evalTime + (plannedMins / 2L) * 60_000L

            // Gather all prior work blocks finishing before or at evalTime
            val priorRealized = realizedBlocks.filter {
                it.taskId != task.id && it.startMillis < evalTime
            }
            val priorScheduled = scheduledBlocks.filter {
                it.taskId != task.id &&
                    it.endMillis <= evalTime &&
                    it.taskId !in sessionTaskIds
            }
            val priorBlocks = (priorRealized + priorScheduled)
                .sortedBy { it.startMillis }

            val priorPlacements = priorBlocks.map { block ->
                ScheduledPlacement(
                    task = block.schedulableTask,
                    window = TimeSlotWindow(
                        startMillis = block.startMillis,
                        endMillis = min(block.endMillis, evalTime)
                    ),
                    score = 50.0
                )
            }

            val rawCumulativeLoad = FatigueHeuristic.computeCumulativeCognitiveLoadAt(
                timestampMillis = evalTime,
                placements = priorPlacements
            )

            // Apply explicit rest break discharge for breaks logged before evalTime
            val latestPriorBlockStart = priorBlocks.minOfOrNull { it.startMillis } ?: dayStartMillis
            val applicableRests = todayRestLogs.filter {
                it.completedAtMillis in latestPriorBlockStart..evalTime
            }
            val totalDischarged = applicableRests.sumOf { BurnoutGuardEngine.restDischargeMinutes(it.durationMinutes) }
            val latestQualifyingRestEnd = applicableRests
                .filter { it.durationMinutes >= 10 }
                .maxOfOrNull { it.completedAtMillis }
            val cumulativeLoad = (rawCumulativeLoad - totalDischarged).coerceAtLeast(0.0)

            val blocksAfterRest = if (latestQualifyingRestEnd != null) {
                priorBlocks.filter { it.endMillis > latestQualifyingRestEnd }
            } else {
                priorBlocks
            }

            val recentSeries = computeRecentSeriesStats(blocksAfterRest, evalTime)
            val priorLongSessionCount = blocksAfterRest.count { it.durationMinutes >= LONG_SESSION_THRESHOLD_MINUTES } +
                recentSeries.consolidatedShortLongBouts

            val baseSnapshot = if (timeline.isNotEmpty()) {
                timeline.minByOrNull { abs(it.timestampMillis - evalMidpoint) } ?: fallbackSnapshot
            } else {
                fallbackSnapshot
            }

            val seriesFocusDepletion = min(0.12, (recentSeries.seriesMinutes / 90.0) * 0.09)
            val depletionDelta = (seriesFocusDepletion - baseSnapshot.focusDepletionPenalty).coerceAtLeast(0.0)
            val adjustedUsable = (baseSnapshot.usableEnergy - (depletionDelta * 65.0).roundToInt()).coerceIn(5, 100)
            val effectiveSnapshot = baseSnapshot.copy(
                usableEnergy = adjustedUsable,
                focusDepletionPenalty = max(baseSnapshot.focusDepletionPenalty, seriesFocusDepletion)
            )

            val breakdown = taskSchedulerService.evaluateTaskScoreWithFatigue(
                task = schedulable,
                snapshot = effectiveSnapshot,
                dependentCount = dependentCounts[task.id] ?: 0,
                calibrationRatio = calibrationRatio,
                weights = priorityWeights,
                referenceNowMillis = nowMillis,
                cumulativeCognitiveLoadMinutes = cumulativeLoad
            )

            val loadIntensity = FatigueHeuristic.computeCognitiveLoadIntensity(schedulable)
            val fatigueIndex = FatigueHeuristic.computeFatigueIndex(effectiveSnapshot, cumulativeLoad)
            val isHighLoadTask = task.energyLevel == EnergyLevel.HIGH ||
                loadIntensity >= 0.75 ||
                (task.isFrog && task.energyLevel != EnergyLevel.LOW)

            val priorMinutes = priorBlocks.sumOf { it.durationMinutes }
            val hasLongSessionSeriesBurnout = isHighLoadTask && (
                (priorLongSessionCount >= 2 && (priorMinutes >= 85 || cumulativeLoad >= 65.0)) ||
                    (recentSeries.seriesMinutes >= 90 && cumulativeLoad >= 55.0) ||
                    cumulativeLoad >= 120.0 ||
                    breakdown.fatigueAdjustment <= -7.0 ||
                    (fatigueIndex >= 0.50 && breakdown.bufferedDurationMinutes >= 45)
                )

            val warningLevel = when {
                hasLongSessionSeriesBurnout -> FatigueWarningLevel.HIGH_BURNOUT_RISK
                breakdown.fatigueAdjustment <= -2.5 ||
                    (isHighLoadTask && (priorLongSessionCount >= 1 || cumulativeLoad >= 50.0 || fatigueIndex >= 0.30)) ->
                    FatigueWarningLevel.MODERATE_STRAIN
                breakdown.fatigueAdjustment >= 2.0 -> FatigueWarningLevel.RESTORATIVE
                else -> FatigueWarningLevel.OPTIMAL
            }

            val deltaRounded = breakdown.fatigueAdjustment.roundToInt()
            val deltaText = if (deltaRounded > 0) "+$deltaRounded" else "$deltaRounded"
            val warningReason = when (warningLevel) {
                FatigueWarningLevel.HIGH_BURNOUT_RISK -> when {
                    priorLongSessionCount >= 2 ->
                        "High load after $priorLongSessionCount long sessions (${priorMinutes}m) · Take a break or split"
                    recentSeries.seriesMinutes >= 90 ->
                        "High load after ${recentSeries.seriesMinutes}m sustained focus · Recovery break advised"
                    else ->
                        "High burnout risk (${(fatigueIndex * 100).roundToInt()}% fatigue) · Reschedule or lighten"
                }
                FatigueWarningLevel.MODERATE_STRAIN -> when {
                    priorLongSessionCount >= 1 ->
                        "Elevated load after ${priorMinutes}m session work ($deltaText pts)"
                    else ->
                        "Moderate circadian strain ($deltaText pts)"
                }
                FatigueWarningLevel.RESTORATIVE ->
                    "Restorative pacing boost ($deltaText pts)"
                FatigueWarningLevel.OPTIMAL ->
                    "Aligned with current energy & recovery"
            }

            val baseScore = (breakdown.totalScore - breakdown.fatigueAdjustment).roundToInt().coerceIn(0, 100)
            task.id to TaskFatigueScoreUi(
                taskId = task.id,
                priorityScore = breakdown.totalScore.roundToInt().coerceIn(0, 100),
                basePriorityScore = baseScore,
                fatigueAdjustment = breakdown.fatigueAdjustment,
                fatigueIndex = fatigueIndex,
                cognitiveLoadIntensity = loadIntensity,
                priorLongSessionCount = priorLongSessionCount,
                priorSessionMinutes = priorMinutes,
                cumulativeLoadMinutes = cumulativeLoad,
                warningLevel = warningLevel,
                warningReason = warningReason,
                isHighLoadTask = isHighLoadTask
            )
        }
    }

    private fun resolveTaskStartMillis(
        task: TaskEntity,
        dayStartMillis: Long,
        zoneId: ZoneId
    ): Long? {
        if (task.scheduledTime == null) return null
        val resolved = task.resolveScheduledEpochMillis(dayStartMillis)
        if (resolved != null) return resolved
        val minOfDay = task.timelineStartMinuteOfDay()
        if (minOfDay < 0) return null
        val dayZdt = java.time.Instant.ofEpochMilli(dayStartMillis).atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        return dayZdt + minOfDay * 60_000L
    }

    private data class SeriesStats(
        val seriesMinutes: Int,
        val consolidatedShortLongBouts: Int
    )

    private fun computeRecentSeriesStats(
        priorBlocks: List<PriorWorkBlock>,
        evalTime: Long
    ): SeriesStats {
        if (priorBlocks.isEmpty()) return SeriesStats(0, 0)
        // Walk backwards from the latest prior block while inter-block gaps < 35 minutes
        var cursorEnd = evalTime
        var seriesMins = 0
        var consecutiveShortBouts = 0
        var currentBoutMinutes = 0

        for (block in priorBlocks.asReversed()) {
            val gapMinutes = max(0L, (cursorEnd - block.endMillis) / 60_000L)
            if (gapMinutes >= 45L && seriesMins > 0) {
                break
            }
            seriesMins += block.durationMinutes
            if (block.durationMinutes < LONG_SESSION_THRESHOLD_MINUTES) {
                currentBoutMinutes += block.durationMinutes
                if (currentBoutMinutes >= LONG_SESSION_THRESHOLD_MINUTES) {
                    consecutiveShortBouts++
                    currentBoutMinutes = 0
                }
            } else {
                currentBoutMinutes = 0
            }
            cursorEnd = block.startMillis
        }

        return SeriesStats(
            seriesMinutes = seriesMins,
            consolidatedShortLongBouts = consecutiveShortBouts
        )
    }
}
