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
    val createdAtMillis: Long = System.currentTimeMillis(),
    val prerequisiteIds: Set<String> = emptySet(),
    val impactScore: Int = 50,
    val estimatedEnjoyment: Int = 5,
    val postponeCount: Int = 0
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

data class PriorityWeightProfile(
    val urgencyWeight: Double = 1.0,
    val impactWeight: Double = 1.0,
    val circadianMatchWeight: Double = 1.0,
    val momentumWeight: Double = 1.0,
    val frogBonusMultiplier: Double = 1.0
) {
    companion object {
        val DEFAULT = PriorityWeightProfile()
    }
}

interface TaskSchedulerService {
    fun evaluateTaskScore(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int = 0,
        calibrationRatio: Double = 1.20
    ): TaskScoreBreakdown

    fun evaluateTaskScoreWithWeights(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int = 0,
        calibrationRatio: Double = 1.20,
        weights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT
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
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList()
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
    ): TaskScoreBreakdown = evaluateTaskScoreWithWeights(
        task = task,
        snapshot = snapshot,
        dependentCount = dependentCount,
        calibrationRatio = calibrationRatio,
        weights = PriorityWeightProfile.DEFAULT
    )

    override fun evaluateTaskScoreWithWeights(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int,
        calibrationRatio: Double,
        weights: PriorityWeightProfile
    ): TaskScoreBreakdown {
        val bufferedMins = computeBufferedDuration(task, calibrationRatio)

        // 1. Urgency Vector U in [0..100]
        val postponeBonus = min(15.0, task.postponeCount * 3.0)
        val urgency = if (task.deadlineMillis != null) {
            val hoursRemaining = (task.deadlineMillis - snapshot.timestampMillis) / 3_600_000.0
            val slackHours = hoursRemaining - (bufferedMins / 60.0)
            ((100.0 / (1.0 + exp(0.12 * (slackHours - 24.0)))) + postponeBonus).coerceIn(0.0, 100.0)
        } else {
            val ageDays = max(0.0, (snapshot.timestampMillis - task.createdAtMillis) / 86_400_000.0)
            min(40.0, 15.0 + 25.0 * (1.0 - exp(-ageDays / 14.0)) + postponeBonus)
        }

        // 2. Impact Vector I in [0..100]
        val basePriority = when (task.priority) {
            2 -> 65.0
            1 -> 40.0
            else -> 15.0
        }
        val impactAdj = if (task.impactScore != 50) {
            (task.impactScore - 50) * 0.30
        } else 0.0
        val frogBonus = if (task.isFrog) (25.0 * weights.frogBonusMultiplier.coerceIn(0.5, 2.0)) else 0.0
        val dependencyBonus = 10.0 * min(3, dependentCount)
        val impact = (basePriority + impactAdj + frogBonus + dependencyBonus).coerceIn(0.0, 100.0)

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
        val enjoymentBonus = if (task.estimatedEnjoyment != 5) {
            (task.estimatedEnjoyment - 5) * 2.0
        } else 0.0
        val momentum = (30.0 * progressFraction + activeBonus + microStepBonus + lowFrictionBonus + enjoymentBonus)
            .coerceIn(0.0, 100.0)

        // Normalized user weights so totalScore is strictly bounded in [0..100]
        val rawWu = W_URGENCY * weights.urgencyWeight.coerceIn(0.1, 3.0)
        val rawWi = W_IMPACT * weights.impactWeight.coerceIn(0.1, 3.0)
        val rawWe = W_ENERGY * weights.circadianMatchWeight.coerceIn(0.1, 3.0)
        val rawWm = W_MOMENTUM * weights.momentumWeight.coerceIn(0.1, 3.0)
        val weightSum = (rawWu + rawWi + rawWe + rawWm).coerceAtLeast(0.001)

        val totalScore = (
            (rawWu * urgency + rawWi * impact + rawWe * energyFit + rawWm * momentum) / weightSum
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
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        priorityWeights: PriorityWeightProfile,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        physiologicalBlocks: List<CognitiveBlockWindow>
    ): List<ScheduledPlacement> {
        if (tasks.isEmpty()) return emptyList()

        // 1. Construct DST-safe day horizon [max(now, 06:00, wakeTime + 15m), 22:30]
        val earliestDayStart = dayDate.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val latestDayEnd = dayDate.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val wakeTodayMillis = dayDate.atTime(zonedWake.toLocalTime()).atZone(zoneId).toInstant().toEpochMilli()
        val effectiveWakeStart = max(earliestDayStart, wakeTodayMillis + 15 * 60_000L)
        val dayStartMillis = if (nowMillis >= latestDayEnd) effectiveWakeStart else max(nowMillis, effectiveWakeStart)
        val dayEndMillis = latestDayEnd
        if (dayStartMillis >= dayEndMillis) return emptyList()

        // 2. Build Exact Minute-Resolution Free Intervals (Fixes Flaw #9)
        var freeIntervals = mutableListOf(TimeSlotWindow(dayStartMillis, dayEndMillis))
        val napWindows = naps.map { TimeSlotWindow(it.startMillis, it.endMillis) }
        val physioWindows = physiologicalBlocks.map { it.toTimeSlotWindow(dayDate, zoneId) }
        val allBlocked = busyWindows + protectedSlots.map { it.resolveForDate(dayDate, zoneId) } + napWindows + physioWindows
        for (blocked in allBlocked) {
            freeIntervals = subtractInterval(freeIntervals, blocked)
        }

        // 3. Separate Locked / Hysteresis-Anchored Tasks (Fixes Flaw #12)
        val placements = mutableListOf<ScheduledPlacement>()
        val completionEndMap = mutableMapOf<String, Long>()
        val dependentsMap = mutableMapOf<String, MutableSet<String>>()
        for (task in tasks) {
            val allPrereqs = (task.prerequisiteIds + listOfNotNull(task.parentTaskId)).toSet()
            for (p in allPrereqs) {
                dependentsMap.getOrPut(p) { mutableSetOf() }.add(task.id)
            }
        }

        val remainingTasks = mutableListOf<SchedulableTask>()
        for (task in tasks) {
            val existingStart = task.scheduledStartMillis
            val durationMins = computeBufferedDuration(task, calibrationRatio)
            val existingWindow = if (existingStart != null) {
                TimeSlotWindow(existingStart, existingStart + durationMins * 60_000L)
            } else null

            if (existingWindow != null &&
                (task.isLocked || (!forceReplan && existingWindow.startMillis >= nowMillis - 15 * 60_000L)) &&
                fitsInFreeIntervals(freeIntervals, existingWindow)
            ) {
                val snap = circadianService.evaluateSnapshot(
                    timestampMillis = existingWindow.startMillis,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = 0,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour
                )
                val breakdown = evaluateTaskScoreWithWeights(
                    task, snap, dependentsMap[task.id]?.size ?: 0, calibrationRatio, priorityWeights
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

        // 4. Multi-Prerequisite DAG Topological Sort + Priority Scheduling (Fixes Flaw #5)
        val remainingIds = remainingTasks.map { it.id }.toSet()
        val inDegree = mutableMapOf<String, Int>()
        val children = mutableMapOf<String, MutableList<SchedulableTask>>()

        for (task in remainingTasks) {
            val allPrereqs = (task.prerequisiteIds + listOfNotNull(task.parentTaskId)).filter { it in remainingIds }.toSet()
            inDegree[task.id] = allPrereqs.size
            for (prereq in allPrereqs) {
                children.getOrPut(prereq) { mutableListOf() }.add(task)
            }
        }

        val nowSnapshot = circadianService.evaluateSnapshot(
            timestampMillis = dayStartMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour
        )
        val scoreCache = remainingTasks.associate { t ->
            t.id to evaluateTaskScoreWithWeights(t, nowSnapshot, dependentsMap[t.id]?.size ?: 0, calibrationRatio, priorityWeights)
        }

        val readyQueue = PriorityQueue<SchedulableTask>(
            compareByDescending { task ->
                val baseScore = scoreCache[task.id]?.totalScore ?: 0.0
                val scarcityBonus = when (task.energyDemand) {
                    EnergyDemand.HIGH -> 12.0
                    EnergyDemand.MODERATE -> 4.0
                    EnergyDemand.LOW -> 0.0
                }
                baseScore + scarcityBonus
            }
        )
        remainingTasks.filter { (inDegree[it.id] ?: 0) == 0 }.forEach { readyQueue.add(it) }

        var consecutiveCognitiveMinutes = 0

        while (readyQueue.isNotEmpty()) {
            val current = readyQueue.poll() ?: break
            val breakdown = scoreCache.getValue(current.id)
            val durationMillis = breakdown.bufferedDurationMinutes * 60_000L
            val rawDurationMillis = max(5, current.estimatedMinutes) * 60_000L

            // Respect all prerequisite completion times if scheduled earlier in this pass
            val allPrereqs = current.prerequisiteIds + listOfNotNull(current.parentTaskId)
            val maxPrereqEnd = allPrereqs.mapNotNull { completionEndMap[it] }.maxOrNull() ?: dayStartMillis
            val earliestStart = max(dayStartMillis, maxPrereqEnd + TRANSITION_BUFFER_MINUTES * 60_000L)

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
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                priorityWeights = priorityWeights,
                placements = placements,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                physiologicalBlocks = physiologicalBlocks
            ) ?: if (!current.isDurationExplicit && rawDurationMillis < durationMillis) {
                // Fallback to exact uninflated duration so 25m tasks fit 25m calendar gaps
                findBestCircadianWindow(
                    task = current,
                    durationMillis = rawDurationMillis,
                    earliestStartMillis = earliestStart,
                    freeIntervals = freeIntervals,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    dependentCount = dependentsMap[current.id]?.size ?: 0,
                    calibrationRatio = calibrationRatio,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    priorityWeights = priorityWeights,
                    placements = placements,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    physiologicalBlocks = physiologicalBlocks
                )
            } else null

            if (bestSlot != null) {
                val (window, slotBreakdown) = bestSlot
                placements.add(ScheduledPlacement(current, window, slotBreakdown))
                completionEndMap[current.id] = window.endMillis

                val isCognitiveHeavy = current.energyDemand == EnergyDemand.HIGH ||
                    current.category == TaskCategory.DEEP_WORK ||
                    current.category == TaskCategory.CREATIVE

                if (isCognitiveHeavy) {
                    consecutiveCognitiveMinutes += window.durationMinutes
                } else {
                    consecutiveCognitiveMinutes = 0
                }

                // Ultradian rhythm protection: 15m recovery break after >= 90m cognitive load
                val bufferMins = if (consecutiveCognitiveMinutes >= 90) {
                    consecutiveCognitiveMinutes = 0
                    15
                } else {
                    TRANSITION_BUFFER_MINUTES
                }

                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(window.startMillis, window.endMillis + bufferMins * 60_000L)
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

    fun computeSimulatedFocusLoadAt(
        timestampMillis: Long,
        placements: List<ScheduledPlacement>
    ): Int {
        if (placements.isEmpty()) return 0
        var currentCheck = timestampMillis
        var accumulatedMins = 0
        val sortedPlacements = placements
            .filter { it.window.endMillis <= timestampMillis }
            .sortedByDescending { it.window.endMillis }

        for (p in sortedPlacements) {
            val gapMinutes = (currentCheck - p.window.endMillis) / 60_000L
            if (gapMinutes <= 15L) {
                val isCognitive = p.task.energyDemand == EnergyDemand.HIGH ||
                    p.task.category == TaskCategory.DEEP_WORK ||
                    p.task.category == TaskCategory.CREATIVE
                if (isCognitive) {
                    accumulatedMins += p.window.durationMinutes
                    currentCheck = p.window.startMillis
                } else {
                    break
                }
            } else {
                break
            }
        }
        return accumulatedMins
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
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        placements: List<ScheduledPlacement> = emptyList(),
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList()
    ): Pair<TimeSlotWindow, TaskScoreBreakdown>? {
        var bestWindow: TimeSlotWindow? = null
        var bestBreakdown: TaskScoreBreakdown? = null
        var bestUtility = Double.NEGATIVE_INFINITY
        val stepMillis = 15 * 60_000L

        for (interval in freeIntervals) {
            var candidateStart = max(interval.startMillis, earliestStartMillis)
            while (candidateStart + durationMillis <= interval.endMillis) {
                val midpoint = candidateStart + durationMillis / 2
                val simulatedFocus = computeSimulatedFocusLoadAt(candidateStart, placements)
                val snap = circadianService.evaluateSnapshot(
                    timestampMillis = midpoint,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = simulatedFocus,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    cognitiveBlocks = physiologicalBlocks
                )
                val breakdown = evaluateTaskScoreWithWeights(task, snap, dependentCount, calibrationRatio, priorityWeights)
                // Demand-elastic time decay lambda(tau)
                val hourlyDecayLambda = when {
                    task.energyDemand == EnergyDemand.HIGH ||
                    task.category == TaskCategory.DEEP_WORK ||
                    task.category == TaskCategory.CREATIVE -> 0.08
                    task.energyDemand == EnergyDemand.MODERATE -> 0.22
                    else -> 0.45
                }

                // Peak-Window Protection: penalize placing LOW energy / ADMIN tasks inside a biological PEAK window unless urgency is critical
                val peakCannibalizationPenalty = if (
                    snap.phase == EnergyPhase.PEAK &&
                    breakdown.urgencyScore < 75.0 &&
                    (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN)
                ) {
                    16.0
                } else {
                    0.0
                }

                val hoursFromNow = (candidateStart - earliestStartMillis) / 3_600_000.0
                val utility = breakdown.totalScore - (hourlyDecayLambda * hoursFromNow) - peakCannibalizationPenalty

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
