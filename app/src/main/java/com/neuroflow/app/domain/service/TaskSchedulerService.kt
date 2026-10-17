package com.neuroflow.app.domain.service

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SchedulableTask(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val priority: Int, // 0 = LOW, 1 = MEDIUM, 2 = HIGH
    val estimatedMinutes: Int,
    val isDurationExplicit: Boolean = false,
    val energyDemand: EnergyDemand,
    val isFrog: Boolean = false,
    val scheduledStartMillis: Long? = null,
    val deadlineMillis: Long? = null,
    val isLocked: Boolean = false,
    val parentTaskId: String? = null,
    val completedMicroSteps: Int = 0,
    val totalMicroSteps: Int = 0,
    val isCurrentlyActive: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class TaskScoreBreakdown(
    val taskId: String,
    val totalScore: Double,      // Guaranteed [0.0..100.0]
    val urgencyScore: Double,    // [0.0..100.0]
    val impactScore: Double,     // [0.0..100.0]
    val energyFitScore: Double,  // [0.0..100.0]
    val momentumScore: Double,   // [0.0..100.0]
    val bufferedDurationMinutes: Int
)

data class TimeSlotWindow(
    val startMillis: Long,
    val endMillis: Long
) {
    val durationMinutes: Int
        get() = ((endMillis - startMillis) / 60_000L).toInt()
}

/** DST-safe local wall-clock rule for recurring protected slots (Fixes Flaw #4) */
data class ProtectedLocalSlot(
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int
) {
    fun resolveForDate(date: LocalDate, zoneId: ZoneId): TimeSlotWindow {
        val startZdt = date.atTime(LocalTime.of(startHour, startMinute)).atZone(zoneId)
        val endZdt = startZdt.plusMinutes(durationMinutes.toLong())
        return TimeSlotWindow(
            startMillis = startZdt.toInstant().toEpochMilli(),
            endMillis = endZdt.toInstant().toEpochMilli()
        )
    }
}

data class ScheduledPlacement(
    val task: SchedulableTask,
    val window: TimeSlotWindow,
    val scoreBreakdown: TaskScoreBreakdown
)

interface TaskSchedulerService {
    fun evaluateTaskScore(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int = 0,
        calibrationRatio: Double = 1.20
    ): TaskScoreBreakdown

    fun updateCalibrationRatio(
        previousRatio: Double,
        estimatedMinutes: Int,
        actualMinutes: Int
    ): Double

    fun scheduleDay(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow>,
        protectedSlots: List<ProtectedLocalSlot>,
        calibrationRatio: Double = 1.20,
        forceReplan: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList()
    ): List<ScheduledPlacement>
}

@Singleton
class DefaultTaskSchedulerService @Inject constructor(
    private val circadianService: CircadianEnergyService
) : TaskSchedulerService {

    companion object {
        private const val W_URGENCY = 0.30
        private const val W_IMPACT = 0.30
        private const val W_ENERGY = 0.25
        private const val W_MOMENTUM = 0.15

        private const val EWMA_ALPHA = 0.25
        private const val HYSTERESIS_THRESHOLD = 18.0
        private const val TRANSITION_BUFFER_MINUTES = 5
    }

    override fun evaluateTaskScore(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int,
        calibrationRatio: Double
    ): TaskScoreBreakdown {
        val bufferedMins = computeBufferedDuration(task, calibrationRatio)

        // 1. Urgency Vector U in [0..100]
        val urgency = if (task.deadlineMillis != null) {
            val hoursRemaining = (task.deadlineMillis - snapshot.timestampMillis) / 3_600_000.0
            val slackHours = hoursRemaining - (bufferedMins / 60.0)
            (100.0 / (1.0 + exp(0.12 * (slackHours - 24.0)))).coerceIn(0.0, 100.0)
        } else {
            val ageDays = max(0.0, (snapshot.timestampMillis - task.createdAtMillis) / 86_400_000.0)
            min(40.0, 15.0 + 25.0 * (1.0 - exp(-ageDays / 14.0)))
        }

        // 2. Impact Vector I in [0..100]
        val basePriority = when (task.priority) {
            2 -> 65.0
            1 -> 40.0
            else -> 15.0
        }
        val frogBonus = if (task.isFrog) 25.0 else 0.0
        val dependencyBonus = 10.0 * min(3, dependentCount)
        val impact = (basePriority + frogBonus + dependencyBonus).coerceIn(0.0, 100.0)

        // 3. Energy & Category Fit Vector E in [0..100] (Fixes Flaw #2 & #10)
        val energyDiff = snapshot.usableEnergy.toDouble() - task.energyDemand.targetScore
        val demandGaussian = 100.0 * exp(-(energyDiff * energyDiff) / (2.0 * 28.0 * 28.0))
        val categoryAffinity = circadianService.computeCategoryAffinity(task.category, snapshot)
        val energyFit = (0.70 * demandGaussian + 0.30 * categoryAffinity).coerceIn(0.0, 100.0)

        // 4. Momentum Vector M in [0..100] (Fixes Flaw #13: Zero toxic distraction boosts)
        val progressFraction = if (task.totalMicroSteps > 0) {
            (task.completedMicroSteps.toDouble() / task.totalMicroSteps).coerceIn(0.0, 1.0)
        } else 0.0
        val activeBonus = if (task.isCurrentlyActive) 35.0 else 0.0
        val microStepBonus = if (task.totalMicroSteps > 0) 20.0 else 0.0
        val lowFrictionBonus = 15.0 * exp(-task.estimatedMinutes / 45.0)
        val momentum = (30.0 * progressFraction + activeBonus + microStepBonus + lowFrictionBonus)
            .coerceIn(0.0, 100.0)

        val totalScore = (
            W_URGENCY * urgency +
            W_IMPACT * impact +
            W_ENERGY * energyFit +
            W_MOMENTUM * momentum
        ).coerceIn(0.0, 100.0)

        return TaskScoreBreakdown(
            taskId = task.id,
            totalScore = totalScore,
            urgencyScore = urgency,
            impactScore = impact,
            energyFitScore = energyFit,
            momentumScore = momentum,
            bufferedDurationMinutes = bufferedMins
        )
    }

    override fun updateCalibrationRatio(
        previousRatio: Double,
        estimatedMinutes: Int,
        actualMinutes: Int
    ): Double {
        if (estimatedMinutes <= 0 || actualMinutes <= 0) return previousRatio
        val observedRatio = actualMinutes.toDouble() / estimatedMinutes.toDouble()
        // Signed EWMA ratio (Fixes Flaw #6: Finishing early decreases ratio instead of inflating)
        return ((1.0 - EWMA_ALPHA) * previousRatio + EWMA_ALPHA * observedRatio).coerceIn(0.5, 2.5)
    }

    override fun scheduleDay(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow>,
        protectedSlots: List<ProtectedLocalSlot>,
        calibrationRatio: Double,
        forceReplan: Boolean,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>
    ): List<ScheduledPlacement> {
        if (tasks.isEmpty()) return emptyList()

        // 1. Construct DST-safe day horizon [max(now, 06:00), 22:30]
        val dayStartMillis = max(
            nowMillis,
            dayDate.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli()
        )
        val dayEndMillis = dayDate.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()
        if (dayStartMillis >= dayEndMillis) return emptyList()

        // 2. Build Exact Minute-Resolution Free Intervals (Fixes Flaw #9)
        var freeIntervals = mutableListOf(TimeSlotWindow(dayStartMillis, dayEndMillis))
        val napWindows = naps.map { TimeSlotWindow(it.startMillis, it.endMillis) }
        val allBlocked = busyWindows + protectedSlots.map { it.resolveForDate(dayDate, zoneId) } + napWindows
        for (blocked in allBlocked) {
            freeIntervals = subtractInterval(freeIntervals, blocked)
        }

        // 3. Separate Locked / Hysteresis-Anchored Tasks (Fixes Flaw #12)
        val placements = mutableListOf<ScheduledPlacement>()
        val completionEndMap = mutableMapOf<String, Long>()
        val dependentsMap = tasks.groupBy { it.parentTaskId }

        val remainingTasks = mutableListOf<SchedulableTask>()
        for (task in tasks) {
            val existingStart = task.scheduledStartMillis
            val durationMins = computeBufferedDuration(task, calibrationRatio)
            val existingWindow = if (existingStart != null) {
                TimeSlotWindow(existingStart, existingStart + durationMins * 60_000L)
            } else null

            val keepAnchored = existingWindow != null &&
                (task.isLocked || (!forceReplan && existingWindow.startMillis >= nowMillis - 15 * 60_000L)) &&
                fitsInFreeIntervals(freeIntervals, existingWindow)

            if (keepAnchored && existingWindow != null) {
                val snap = if (naps.isNotEmpty()) {
                    circadianService.evaluateSnapshot(
                        existingWindow.startMillis, wakeTimeMillis, lastSleepDurationMinutes, 0, zoneId, naps
                    )
                } else {
                    circadianService.evaluateSnapshot(
                        existingWindow.startMillis, wakeTimeMillis, lastSleepDurationMinutes, 0, zoneId
                    )
                }
                val breakdown = evaluateTaskScore(
                    task, snap, dependentsMap[task.id]?.size ?: 0, calibrationRatio
                )
                placements.add(ScheduledPlacement(task, existingWindow, breakdown))
                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(
                        existingWindow.startMillis,
                        existingWindow.endMillis + TRANSITION_BUFFER_MINUTES * 60_000L
                    )
                )
                completionEndMap[task.id] = existingWindow.endMillis
            } else {
                remainingTasks.add(task)
            }
        }

        // 4. Topological Sort + Priority Scheduling for Multi-Step Workflows (Fixes Flaw #5)
        val remainingIds = remainingTasks.map { it.id }.toSet()
        val inDegree = mutableMapOf<String, Int>()
        val children = mutableMapOf<String, MutableList<SchedulableTask>>()

        for (task in remainingTasks) {
            val parentId = task.parentTaskId
            if (parentId != null && parentId in remainingIds) {
                inDegree[task.id] = 1
                children.getOrPut(parentId) { mutableListOf() }.add(task)
            } else {
                inDegree[task.id] = 0
            }
        }

        val nowSnapshot = if (naps.isNotEmpty()) {
            circadianService.evaluateSnapshot(
                dayStartMillis, wakeTimeMillis, lastSleepDurationMinutes, 0, zoneId, naps
            )
        } else {
            circadianService.evaluateSnapshot(
                dayStartMillis, wakeTimeMillis, lastSleepDurationMinutes, 0, zoneId
            )
        }
        val scoreCache = remainingTasks.associate { t ->
            t.id to evaluateTaskScore(t, nowSnapshot, dependentsMap[t.id]?.size ?: 0, calibrationRatio)
        }

        val readyQueue = PriorityQueue<SchedulableTask>(
            compareByDescending { scoreCache[it.id]?.totalScore ?: 0.0 }
        )
        remainingTasks.filter { inDegree[it.id] == 0 }.forEach { readyQueue.add(it) }

        while (readyQueue.isNotEmpty()) {
            val current = readyQueue.poll() ?: break
            val breakdown = scoreCache.getValue(current.id)
            val durationMillis = breakdown.bufferedDurationMinutes * 60_000L

            // Respect parent completion time if parent was scheduled earlier in this pass
            val parentEnd = current.parentTaskId?.let { completionEndMap[it] } ?: dayStartMillis
            val earliestStart = max(dayStartMillis, parentEnd + TRANSITION_BUFFER_MINUTES * 60_000L)

            val bestSlot = findBestCircadianWindow(
                task = current,
                durationMillis = durationMillis,
                earliestStartMillis = earliestStart,
                freeIntervals = freeIntervals,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = lastSleepDurationMinutes,
                dependentCount = dependentsMap[current.id]?.size ?: 0,
                calibrationRatio = calibrationRatio,
                zoneId = zoneId,
                naps = naps
            )

            if (bestSlot != null) {
                val (window, slotBreakdown) = bestSlot
                placements.add(ScheduledPlacement(current, window, slotBreakdown))
                completionEndMap[current.id] = window.endMillis
                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(window.startMillis, window.endMillis + TRANSITION_BUFFER_MINUTES * 60_000L)
                )

                // Unlock dependent children in the same day pass
                children[current.id]?.forEach { child ->
                    val newDeg = (inDegree[child.id] ?: 1) - 1
                    inDegree[child.id] = newDeg
                    if (newDeg <= 0) readyQueue.add(child)
                }
            }
        }

        return placements.sortedBy { it.window.startMillis }
    }

    private fun computeBufferedDuration(task: SchedulableTask, calibrationRatio: Double): Int {
        val raw = max(5, task.estimatedMinutes)
        // Fixes Flaw #11: Explicit user durations ("45m") are not silently inflated by historical calibration
        return if (task.isDurationExplicit) raw else (raw * calibrationRatio).roundToInt().coerceAtLeast(5)
    }

    private fun findBestCircadianWindow(
        task: SchedulableTask,
        durationMillis: Long,
        earliestStartMillis: Long,
        freeIntervals: List<TimeSlotWindow>,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        dependentCount: Int,
        calibrationRatio: Double,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout> = emptyList()
    ): Pair<TimeSlotWindow, TaskScoreBreakdown>? {
        var bestWindow: TimeSlotWindow? = null
        var bestBreakdown: TaskScoreBreakdown? = null
        var bestUtility = Double.NEGATIVE_INFINITY
        val stepMillis = 15 * 60_000L

        for (interval in freeIntervals) {
            var candidateStart = max(interval.startMillis, earliestStartMillis)
            while (candidateStart + durationMillis <= interval.endMillis) {
                val midpoint = candidateStart + durationMillis / 2
                val snap = if (naps.isNotEmpty()) {
                    circadianService.evaluateSnapshot(
                        midpoint, wakeTimeMillis, lastSleepDurationMinutes, 0, zoneId, naps
                    )
                } else {
                    circadianService.evaluateSnapshot(
                        midpoint, wakeTimeMillis, lastSleepDurationMinutes, 0, zoneId
                    )
                }
                val breakdown = evaluateTaskScore(task, snap, dependentCount, calibrationRatio)
                // Slight bias toward earlier placement when energy fit is equal
                val hoursFromNow = (candidateStart - earliestStartMillis) / 3_600_000.0
                val utility = breakdown.totalScore - (0.6 * hoursFromNow)

                if (utility > bestUtility) {
                    bestUtility = utility
                    bestWindow = TimeSlotWindow(candidateStart, candidateStart + durationMillis)
                    bestBreakdown = breakdown
                }
                candidateStart += stepMillis
            }
        }
        return if (bestWindow != null && bestBreakdown != null) bestWindow to bestBreakdown else null
    }

    private fun fitsInFreeIntervals(freeIntervals: List<TimeSlotWindow>, target: TimeSlotWindow): Boolean {
        return freeIntervals.any { it.startMillis <= target.startMillis && it.endMillis >= target.endMillis }
    }

    private fun subtractInterval(
        intervals: List<TimeSlotWindow>,
        blocked: TimeSlotWindow
    ): MutableList<TimeSlotWindow> {
        val result = mutableListOf<TimeSlotWindow>()
        for (current in intervals) {
            if (blocked.endMillis <= current.startMillis || blocked.startMillis >= current.endMillis) {
                result.add(current)
            } else {
                if (blocked.startMillis > current.startMillis) {
                    result.add(TimeSlotWindow(current.startMillis, blocked.startMillis))
                }
                if (blocked.endMillis < current.endMillis) {
                    result.add(TimeSlotWindow(blocked.endMillis, current.endMillis))
                }
            }
        }
        return result
    }
}
