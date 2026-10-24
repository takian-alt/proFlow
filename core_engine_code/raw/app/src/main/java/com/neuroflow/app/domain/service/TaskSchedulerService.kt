package com.neuroflow.app.domain.service

import com.neuroflow.app.domain.scheduler.DeferralRationale
import com.neuroflow.app.domain.scheduler.DeferralReasonCode
import com.neuroflow.app.domain.scheduler.DependencyGraphValidator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class SchedulingOutcome(
    val placements: List<ScheduledPlacement>,
    val deferrals: List<DeferralRationale> = emptyList()
)

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
    val postponeCount: Int = 0,
    val contextTag: String = "",
    val isAnxietyTask: Boolean = false,
    val hasImplementationIntention: Boolean = false,
    val fragmentationTolerance: Float = 0.5f,
    val distractionScore: Float = 0f
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

/** DST-safe local wall-clock rule for recurring protected slots */
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
    val frogBonusMultiplier: Double = 1.0,
    val quadrantWeight: Double = 1.0,
    val priorityLevelWeight: Double = 1.0,
    val durationWeight: Double = 1.0,
    val focusModeWeight: Double = 1.0,
    val planBoostWeight: Double = 1.0,
    val enjoymentWeight: Double = 1.0
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
        weights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        referenceNowMillis: Long? = null
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
        busyWindows: List<TimeSlotWindow> = emptyList(),
        protectedSlots: List<ProtectedLocalSlot> = emptyList(),
        calibrationRatio: Double = 1.20,
        forceReplan: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        activeUncompletedTaskIds: Set<String> = emptySet()
    ): List<ScheduledPlacement>

    fun scheduleDayWithOutcome(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow> = emptyList(),
        protectedSlots: List<ProtectedLocalSlot> = emptyList(),
        calibrationRatio: Double = 1.20,
        forceReplan: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        activeUncompletedTaskIds: Set<String> = emptySet()
    ): SchedulingOutcome

    companion object {
        const val ULTRADIAN_RECOVERY_BUFFER_MINUTES = 15
    }
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
        private const val TRANSITION_BUFFER_MINUTES = 5
        const val ULTRADIAN_RECOVERY_BUFFER_MINUTES = 15
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
        weights = PriorityWeightProfile.DEFAULT,
        referenceNowMillis = null
    )

    override fun evaluateTaskScoreWithWeights(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int,
        calibrationRatio: Double,
        weights: PriorityWeightProfile,
        referenceNowMillis: Long?
    ): TaskScoreBreakdown {
        val bufferedMins = computeBufferedDuration(task, calibrationRatio)
        // Bug 1 Fix (ADR-025): Anchor urgency calculation to referenceNowMillis to prevent procrastination bias
        val nowAnchor = referenceNowMillis ?: snapshot.timestampMillis

        // 1. Urgency Vector U in [0..100] with Parkinson's Law ADHD Urgency Ignition
        val postponeBonus = min(15.0, task.postponeCount * 3.0)
        val urgency = if (task.deadlineMillis != null) {
            val hoursRemaining = (task.deadlineMillis - nowAnchor) / 3_600_000.0
            val slackHours = hoursRemaining - (bufferedMins / 60.0)
            val baseLogistic = 100.0 / (1.0 + exp(0.12 * (slackHours - 24.0)))
            val ignitionBoost = if (slackHours in 0.25..6.0) {
                12.0 * exp(-((slackHours - 2.5) * (slackHours - 2.5)) / 4.0)
            } else 0.0
            (baseLogistic + ignitionBoost + postponeBonus).coerceIn(0.0, 100.0)
        } else {
            val ageDays = max(0.0, (nowAnchor - task.createdAtMillis) / 86_400_000.0)
            min(40.0, 15.0 + 25.0 * (1.0 - exp(-ageDays / 14.0)) + postponeBonus)
        }

        // 2. Impact Vector I in [0..100]
        val rawBasePriority = when (task.priority) {
            2 -> 65.0
            1 -> 40.0
            else -> 15.0
        }
        val basePriority = (rawBasePriority * weights.priorityLevelWeight.coerceIn(0.2, 3.0)).coerceIn(0.0, 100.0)
        val impactAdj = if (task.impactScore != 50) {
            (task.impactScore - 50) * 0.30
        } else 0.0
        val frogBonus = if (task.isFrog) (25.0 * weights.frogBonusMultiplier.coerceIn(0.5, 2.0)) else 0.0
        val dependencyBonus = 10.0 * min(3, dependentCount)
        val impact = (basePriority + impactAdj + frogBonus + dependencyBonus).coerceIn(0.0, 100.0)

        // 3. Energy & Category Fit Vector E in [0..100] + Dread Desensitization Routing
        val energyDiff = snapshot.usableEnergy.toDouble() - task.energyDemand.targetScore
        val demandGaussian = 100.0 * exp(-(energyDiff * energyDiff) / (2.0 * 28.0 * 28.0))
        val rawCategoryAffinity = circadianService.computeCategoryAffinity(task.category, snapshot)
        val categoryAffinity = (rawCategoryAffinity * weights.focusModeWeight.coerceIn(0.2, 3.0)).coerceIn(0.0, 100.0)
        val dreadAlignmentAdj = if (task.isAnxietyTask || task.postponeCount >= 3) {
            when (snapshot.phase) {
                EnergyPhase.PEAK, EnergyPhase.REBOUND -> 8.0
                EnergyPhase.DIP, EnergyPhase.WIND_DOWN -> -10.0
                else -> 0.0
            }
        } else 0.0
        val distractionAdj = if (task.distractionScore > 0.3f && snapshot.phase == EnergyPhase.DIP) {
            -8.0 * task.distractionScore
        } else 0.0
        val energyFit = (0.70 * demandGaussian + 0.30 * categoryAffinity + dreadAlignmentAdj + distractionAdj).coerceIn(0.0, 100.0)

        // 4. Momentum Vector M in [0..100] with Zeigarnik Pull & WOOP Implementation Intention Shield
        val progressFraction = if (task.totalMicroSteps > 0) {
            (task.completedMicroSteps.toDouble() / task.totalMicroSteps).coerceIn(0.0, 1.0)
        } else 0.0
        val zeigarnikPull = if (progressFraction > 0.0 && progressFraction < 1.0) {
            30.0 * progressFraction + 10.0 * sin(PI * progressFraction)
        } else {
            30.0 * progressFraction
        }
        val activeBonus = if (task.isCurrentlyActive) 35.0 else 0.0
        val microStepBonus = if (task.totalMicroSteps > 0) 20.0 else 0.0
        val woopBonus = if (task.hasImplementationIntention) {
            12.0 * weights.planBoostWeight.coerceIn(0.1, 3.0)
        } else 0.0
        val lowFrictionBonus = (15.0 * exp(-task.estimatedMinutes / 45.0)) * weights.durationWeight.coerceIn(0.2, 3.0)
        val enjoymentBonus = if (task.estimatedEnjoyment != 5) {
            (task.estimatedEnjoyment - 5) * 2.0 * weights.enjoymentWeight.coerceIn(0.2, 3.0)
        } else 0.0
        val momentum = (zeigarnikPull + activeBonus + microStepBonus + woopBonus + lowFrictionBonus + enjoymentBonus)
            .coerceIn(0.0, 100.0)

        // Eisenhower Quadrant Modulation
        val isImportant = task.priority >= 1 || task.impactScore >= 50 || task.isFrog
        val isUrgent = urgency >= 50.0
        val quadrantSpread = weights.quadrantWeight.coerceIn(0.0, 3.0)
        val quadrantModifier = when {
            isImportant && isUrgent -> 1.0 + 0.15 * quadrantSpread // DO_FIRST
            isImportant && !isUrgent -> 1.0 + 0.08 * quadrantSpread // SCHEDULE
            !isImportant && isUrgent -> 1.0 - 0.08 * quadrantSpread // DELEGATE
            else -> 1.0 - 0.15 * quadrantSpread // ELIMINATE
        }.coerceIn(0.5, 2.0)

        val rawWu = W_URGENCY * weights.urgencyWeight.coerceIn(0.1, 3.0)
        val rawWi = W_IMPACT * weights.impactWeight.coerceIn(0.1, 3.0)
        val rawWe = W_ENERGY * weights.circadianMatchWeight.coerceIn(0.1, 3.0)
        val rawWm = W_MOMENTUM * weights.momentumWeight.coerceIn(0.1, 3.0)
        val weightSum = (rawWu + rawWi + rawWe + rawWm).coerceAtLeast(0.001)

        val totalScore = (
            ((rawWu * urgency + rawWi * impact + rawWe * energyFit + rawWm * momentum) / weightSum) * quadrantModifier
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
        physiologicalBlocks: List<CognitiveBlockWindow>,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double,
        activeUncompletedTaskIds: Set<String>
    ): List<ScheduledPlacement> = scheduleDayWithOutcome(
        tasks = tasks,
        dayDate = dayDate,
        nowMillis = nowMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        busyWindows = busyWindows,
        protectedSlots = protectedSlots,
        calibrationRatio = calibrationRatio,
        forceReplan = forceReplan,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        priorityWeights = priorityWeights,
        learnedProfile = learnedProfile,
        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
        physiologicalBlocks = physiologicalBlocks,
        stimulantDoses = stimulantDoses,
        socialJetlagLagHours = socialJetlagLagHours,
        activeUncompletedTaskIds = activeUncompletedTaskIds
    ).placements

    override fun scheduleDayWithOutcome(
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
        physiologicalBlocks: List<CognitiveBlockWindow>,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double,
        activeUncompletedTaskIds: Set<String>
    ): SchedulingOutcome {
        if (tasks.isEmpty()) return SchedulingOutcome(emptyList())

        // 0. Cycle-Safe DAG Sanitization (ADR-020): guarantees circular dependencies never starve Kahn's queue
        val sanitizedTasks = DependencyGraphValidator.sanitizeDag(tasks).tasks

        // 1. Construct DST-safe day horizon [max(now, 06:00, wakeTime + 15m), 22:30]
        val nowLocalDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val isToday = dayDate == nowLocalDate
        val isPast = dayDate < nowLocalDate

        val earliestDayStart = dayDate.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val latestDayEnd = dayDate.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val wakeTodayMillis = dayDate.atTime(zonedWake.toLocalTime()).atZone(zoneId).toInstant().toEpochMilli()
        val effectiveWakeStart = max(earliestDayStart, wakeTodayMillis + 15 * 60_000L)
        val dayEndMillis = max(latestDayEnd, effectiveWakeStart + 4 * 3600_000L)
        val dayStartMillis = when {
            isPast -> dayEndMillis
            isToday -> max(nowMillis + 5 * 60_000L, effectiveWakeStart)
            else -> effectiveWakeStart
        }
        if (dayStartMillis >= dayEndMillis) {
            return SchedulingOutcome(
                placements = emptyList(),
                deferrals = tasks.map {
                    DeferralRationale(
                        taskId = it.id,
                        reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                        explanation = "Past workday end for this date",
                        actionableSuggestion = "Reschedule for tomorrow or advance wake time."
                    )
                }
            )
        }

        // 2. Build Exact Minute-Resolution Free Intervals
        var freeIntervals = mutableListOf(TimeSlotWindow(dayStartMillis, dayEndMillis))
        val napWindows = naps.map { TimeSlotWindow(it.startMillis, it.endMillis) }
        val physioWindows = physiologicalBlocks.map { it.toTimeSlotWindow(dayDate, zoneId) }
        val allBlocked = busyWindows + protectedSlots.map { it.resolveForDate(dayDate, zoneId) } + napWindows + physioWindows
        for (blocked in allBlocked) {
            freeIntervals = subtractInterval(freeIntervals, blocked)
        }

        // 3. Separate Locked / Hysteresis-Anchored Tasks
        val placements = mutableListOf<ScheduledPlacement>()
        val completionEndMap = mutableMapOf<String, Long>()
        val dependentsMap = mutableMapOf<String, MutableSet<String>>()
        for (task in sanitizedTasks) {
            val allPrereqs = task.prerequisiteIds
            for (p in allPrereqs) {
                dependentsMap.getOrPut(p) { mutableSetOf() }.add(task.id)
            }
        }

        val sanitizedTaskMap = sanitizedTasks.associateBy { it.id }
        val remainingTasks = mutableListOf<SchedulableTask>()

        // Bug 5 Fix: Sort candidate anchor tasks by scheduledStartMillis ascending
        // and only anchor a task if all of its prerequisites in sanitizedTasks are already anchored
        // at an earlier timestamp (prereqEndMillis <= existingWindow.startMillis) and not blocked by active uncompleted cross-day tasks.
        val candidateAnchors = sanitizedTasks.sortedWith(
            compareBy<SchedulableTask> { it.scheduledStartMillis ?: Long.MAX_VALUE }
                .thenBy { it.id }
        )

        for (task in candidateAnchors) {
            val existingStart = task.scheduledStartMillis
            val durationMins = computeBufferedDuration(task, calibrationRatio)
            val existingWindow = if (existingStart != null) {
                TimeSlotWindow(existingStart, existingStart + durationMins * 60_000L)
            } else null

            val isHysteresisCandidate = existingWindow != null &&
                (task.isLocked || (!forceReplan && existingWindow.startMillis >= nowMillis - 15 * 60_000L)) &&
                fitsInFreeIntervals(freeIntervals, existingWindow)

            val allPrereqs = task.prerequisiteIds
            val prereqsSatisfied = isHysteresisCandidate && allPrereqs.all { prereqId ->
                if (prereqId in sanitizedTaskMap) {
                    val prereqEnd = completionEndMap[prereqId]
                    prereqEnd != null && prereqEnd <= existingWindow!!.startMillis
                } else {
                    prereqId !in activeUncompletedTaskIds
                }
            }

            if (isHysteresisCandidate && prereqsSatisfied) {
                val snap = circadianService.evaluateSnapshotWithStimulants(
                    timestampMillis = existingWindow!!.startMillis,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = 0,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    cognitiveBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
                )
                val breakdown = evaluateTaskScoreWithWeights(
                    task, snap, dependentsMap[task.id]?.size ?: 0, calibrationRatio, priorityWeights,
                    referenceNowMillis = nowMillis
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

        // Feature 2 (ADR-024): Reverse-DAG Deadline & Locked-Dependent Ceiling Propagation
        val latestAllowedEndMap = mutableMapOf<String, Long>()
        for (task in sanitizedTasks) {
            val deadlineCeiling = task.deadlineMillis ?: Long.MAX_VALUE
            val placedWindow = placements.firstOrNull { it.task.id == task.id }?.window
            val directCeiling = when {
                placedWindow != null -> placedWindow.startMillis
                task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!!
                else -> deadlineCeiling
            }
            latestAllowedEndMap[task.id] = min(deadlineCeiling, directCeiling)
        }

        var dagCeilingChanged = true
        var dagIterations = 0
        while (dagCeilingChanged && dagIterations < sanitizedTasks.size + 1) {
            dagCeilingChanged = false
            dagIterations++
            for (task in sanitizedTasks) {
                val childCeiling = latestAllowedEndMap[task.id] ?: Long.MAX_VALUE
                if (childCeiling == Long.MAX_VALUE) continue

                val childBufferedMins = computeBufferedDuration(task, calibrationRatio)
                val childDurationMillis = childBufferedMins * 60_000L
                val placedWindow = placements.firstOrNull { it.task.id == task.id }?.window
                val childLatestStart = when {
                    placedWindow != null -> placedWindow.startMillis
                    task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!!
                    else -> childCeiling - childDurationMillis
                }
                val isUltradianDep = task.id.endsWith("#part2") ||
                    task.prerequisiteIds.any { it.endsWith("#part1") }
                val bufferBeforeChild = if (isUltradianDep) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
                val parentMaxEnd = childLatestStart - bufferBeforeChild * 60_000L

                val allPrereqs = task.prerequisiteIds
                for (prereqId in allPrereqs) {
                    val currentParentCeiling = latestAllowedEndMap[prereqId] ?: Long.MAX_VALUE
                    if (parentMaxEnd < currentParentCeiling) {
                        latestAllowedEndMap[prereqId] = parentMaxEnd
                        dagCeilingChanged = true
                    }
                }
            }
        }

        // 4. Multi-Prerequisite DAG Topological Sort + Priority Scheduling
        val deferrals = mutableListOf<DeferralRationale>()
        val remainingIds = remainingTasks.map { it.id }.toSet()

        // Bug 4 Fix: exclude tasks with uncompleted cross-day prerequisites from today's active ready queue
        val schedulableTodayTasks = mutableListOf<SchedulableTask>()
        for (task in remainingTasks) {
            val allPrereqs = task.prerequisiteIds
            val blockedByPrereq = allPrereqs.firstOrNull { prereqId ->
                prereqId in activeUncompletedTaskIds &&
                prereqId !in remainingIds &&
                prereqId !in completionEndMap
            }
            if (blockedByPrereq != null) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.DEPENDENCY_BLOCKED,
                        explanation = "Prerequisite task '$blockedByPrereq' is active and pending on a future date.",
                        actionableSuggestion = "Complete prerequisite '$blockedByPrereq' first or adjust scheduled dates."
                    )
                )
            } else {
                schedulableTodayTasks.add(task)
            }
        }

        val schedulableTodayIds = schedulableTodayTasks.map { it.id }.toSet()
        val inDegree = mutableMapOf<String, Int>()
        val children = mutableMapOf<String, MutableList<SchedulableTask>>()

        for (task in schedulableTodayTasks) {
            val allPrereqs = task.prerequisiteIds.filter { it in schedulableTodayIds }.toSet()
            inDegree[task.id] = allPrereqs.size
            for (prereq in allPrereqs) {
                children.getOrPut(prereq) { mutableListOf() }.add(task)
            }
        }

        val nowSnapshot = circadianService.evaluateSnapshotWithStimulants(
            timestampMillis = dayStartMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            cognitiveBlocks = physiologicalBlocks,
            stimulantDoses = stimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours
        )
        val scoreCache = schedulableTodayTasks.associate { t ->
            t.id to evaluateTaskScoreWithWeights(t, nowSnapshot, dependentsMap[t.id]?.size ?: 0, calibrationRatio, priorityWeights, referenceNowMillis = nowMillis)
        }.toMutableMap()

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
        schedulableTodayTasks.filter { (inDegree[it.id] ?: 0) == 0 }.forEach { readyQueue.add(it) }

        while (readyQueue.isNotEmpty()) {
            val current = readyQueue.poll() ?: break
            val breakdown = scoreCache[current.id]
                ?: evaluateTaskScoreWithWeights(current, nowSnapshot, dependentsMap[current.id]?.size ?: 0, calibrationRatio, priorityWeights, referenceNowMillis = nowMillis)
            val durationMillis = breakdown.bufferedDurationMinutes * 60_000L
            val rawDurationMillis = max(5, current.estimatedMinutes) * 60_000L

            val allPrereqs = current.prerequisiteIds
            val isUltradianPrereq = current.id.endsWith("#part2") || allPrereqs.any { it.endsWith("#part1") }
            val prereqBufferMinutes = if (isUltradianPrereq) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
            val maxPrereqEnd = allPrereqs.mapNotNull { completionEndMap[it] }.maxOrNull() ?: dayStartMillis
            val earliestStart = max(dayStartMillis, maxPrereqEnd + prereqBufferMinutes * 60_000L)

            val taskCeiling = latestAllowedEndMap[current.id] ?: Long.MAX_VALUE
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
                physiologicalBlocks = physiologicalBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = socialJetlagLagHours,
                latestAllowedEndMillis = taskCeiling,
                referenceNowMillis = nowMillis
            ) ?: if (!current.isDurationExplicit && rawDurationMillis < durationMillis) {
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
                    physiologicalBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours,
                    latestAllowedEndMillis = taskCeiling,
                    referenceNowMillis = nowMillis
                )
            } else null

            if (bestSlot != null) {
                val (window, slotBreakdown) = bestSlot
                val finalBreakdown = if (window.durationMinutes < slotBreakdown.bufferedDurationMinutes) {
                    slotBreakdown.copy(bufferedDurationMinutes = window.durationMinutes)
                } else {
                    slotBreakdown
                }
                placements.add(ScheduledPlacement(current, window, finalBreakdown))
                completionEndMap[current.id] = window.endMillis
                if (current.id.endsWith("#part2")) {
                    completionEndMap[current.id.removeSuffix("#part2")] = window.endMillis
                }

                val isCognitiveHeavy = current.energyDemand == EnergyDemand.HIGH ||
                    current.category == TaskCategory.DEEP_WORK ||
                    current.category == TaskCategory.CREATIVE

                // Bug 2 Fix: compute simulated focus load on wall-clock timeline
                val contiguousCognitiveMins = if (isCognitiveHeavy) {
                    computeSimulatedFocusLoadAt(window.startMillis, placements) + window.durationMinutes
                } else {
                    0
                }

                val bufferMins = if (current.id.endsWith("#part1")) {
                    ULTRADIAN_RECOVERY_BUFFER_MINUTES
                } else if (contiguousCognitiveMins >= 90) {
                    15
                } else {
                    TRANSITION_BUFFER_MINUTES
                }

                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(window.startMillis, window.endMillis + bufferMins * 60_000L)
                )

                val unlockKey = if (current.id.endsWith("#part2")) current.id.removeSuffix("#part2") else current.id
                children[unlockKey]?.forEach { child ->
                    val newDeg = (inDegree[child.id] ?: 1) - 1
                    inDegree[child.id] = newDeg
                    if (newDeg <= 0) readyQueue.add(child)
                }
                if (unlockKey != current.id) {
                    children[current.id]?.forEach { child ->
                        val newDeg = (inDegree[child.id] ?: 1) - 1
                        inDegree[child.id] = newDeg
                        if (newDeg <= 0) readyQueue.add(child)
                    }
                }
            } else {
                // Bug 6 Fix & Feature 4 (ADR-024): Atomic Two-Phase Ultradian Split Verifier
                val splitPair = splitIntoUltradianParts(current, breakdown.bufferedDurationMinutes)
                var splitSuccess = false
                if (splitPair != null) {
                    val (part1, part2) = splitPair
                    val p1DurationMillis = max(5, part1.estimatedMinutes) * 60_000L
                    val p2DurationMillis = max(5, part2.estimatedMinutes) * 60_000L
                    val currentCeiling = latestAllowedEndMap[current.id] ?: Long.MAX_VALUE

                    // Phase 1: Test if part1 can fit in current freeIntervals
                    val p1Slot = findBestCircadianWindow(
                        task = part1,
                        durationMillis = p1DurationMillis,
                        earliestStartMillis = earliestStart,
                        freeIntervals = freeIntervals,
                        wakeTimeMillis = wakeTimeMillis,
                        lastSleepDurationMinutes = lastSleepDurationMinutes,
                        dependentCount = 1,
                        calibrationRatio = 1.0,
                        zoneId = zoneId,
                        naps = naps,
                        chronotype = chronotype,
                        manualPeakStartHour = manualPeakStartHour,
                        priorityWeights = priorityWeights,
                        placements = placements,
                        learnedProfile = learnedProfile,
                        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                        physiologicalBlocks = physiologicalBlocks,
                        stimulantDoses = stimulantDoses,
                        socialJetlagLagHours = socialJetlagLagHours,
                        latestAllowedEndMillis = currentCeiling
                    )

                    if (p1Slot != null) {
                        val (p1Window, p1Breakdown) = p1Slot
                        // Require at least a 15-minute ultradian recovery buffer between part1 and part2
                        val p1BufferMins = 15
                        val simulatedIntervalsAfterP1 = subtractInterval(
                            freeIntervals,
                            TimeSlotWindow(p1Window.startMillis, p1Window.endMillis + p1BufferMins * 60_000L)
                        )
                        val earliestP2Start = p1Window.endMillis + p1BufferMins * 60_000L
                        val simulatedPlacementsWithP1 = placements + ScheduledPlacement(part1, p1Window, p1Breakdown)

                        // Phase 2: Test if part2 can fit in simulated remaining intervals
                        val p2Slot = findBestCircadianWindow(
                            task = part2,
                            durationMillis = p2DurationMillis,
                            earliestStartMillis = earliestP2Start,
                            freeIntervals = simulatedIntervalsAfterP1,
                            wakeTimeMillis = wakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes,
                            dependentCount = dependentsMap[current.id]?.size ?: 0,
                            calibrationRatio = 1.0,
                            zoneId = zoneId,
                            naps = naps,
                            chronotype = chronotype,
                            manualPeakStartHour = manualPeakStartHour,
                            priorityWeights = priorityWeights,
                            placements = simulatedPlacementsWithP1,
                            learnedProfile = learnedProfile,
                            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                            physiologicalBlocks = physiologicalBlocks,
                            stimulantDoses = stimulantDoses,
                            socialJetlagLagHours = socialJetlagLagHours,
                            latestAllowedEndMillis = currentCeiling
                        )

                        if (p2Slot != null) {
                            // Both parts verified! Commit both atomically to placements.
                            val (p2Window, p2Breakdown) = p2Slot
                            placements.add(ScheduledPlacement(part1, p1Window, p1Breakdown))
                            placements.add(ScheduledPlacement(part2, p2Window, p2Breakdown))

                            val p2ContiguousMins = computeSimulatedFocusLoadAt(p2Window.startMillis, simulatedPlacementsWithP1) + p2Window.durationMinutes
                            val p2BufferMins = if (p2ContiguousMins >= 90) 15 else TRANSITION_BUFFER_MINUTES
                            freeIntervals = subtractInterval(
                                simulatedIntervalsAfterP1,
                                TimeSlotWindow(p2Window.startMillis, p2Window.endMillis + p2BufferMins * 60_000L)
                            )

                            completionEndMap[part1.id] = p1Window.endMillis
                            completionEndMap[part2.id] = p2Window.endMillis
                            completionEndMap[current.id] = p2Window.endMillis

                            val unlockKey = current.id
                            children[unlockKey]?.forEach { child ->
                                val newDeg = (inDegree[child.id] ?: 1) - 1
                                inDegree[child.id] = newDeg
                                if (newDeg <= 0) readyQueue.add(child)
                            }
                            splitSuccess = true
                        }
                    }
                }

                if (!splitSuccess) {
                    val baseId = current.id.substringBefore("#part")
                    if (deferrals.none { it.taskId == baseId }) {
                        deferrals.add(
                            DeferralRationale(
                                taskId = baseId,
                                reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                                explanation = "Task '${current.title}' (${breakdown.bufferedDurationMinutes}m) could not find sufficient contiguous time or two-phase ultradian split slots today.",
                                actionableSuggestion = "Free up calendar slots or move to tomorrow's schedule."
                            )
                        )
                    }
                }
            }
        }

        // Invariant Safeguard (ADR-024): ensure no orphaned partial split remains in placements
        val orphanedPart1 = placements.filter { it.task.id.endsWith("#part1") }
            .filter { p1 ->
                val p2Id = p1.task.id.replace("#part1", "#part2")
                placements.none { it.task.id == p2Id }
            }
        if (orphanedPart1.isNotEmpty()) {
            val orphanIds = orphanedPart1.map { it.task.id }.toSet()
            placements.removeAll { it.task.id in orphanIds }
            for (orphan in orphanedPart1) {
                val baseId = orphan.task.id.substringBefore("#part")
                if (deferrals.none { it.taskId == baseId }) {
                    deferrals.add(
                        DeferralRationale(
                            taskId = baseId,
                            reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                            explanation = "Task '${orphan.task.title}' had an orphaned partial placement rolled back due to unplaced part 2.",
                            actionableSuggestion = "Free up calendar slots or move to tomorrow's schedule."
                        )
                    )
                }
            }
        }

        // Clean deferral task IDs so none carry #part suffixes
        val sanitizedDeferrals = deferrals.map { d ->
            if (d.taskId.contains("#part")) d.copy(taskId = d.taskId.substringBefore("#part")) else d
        }.distinctBy { it.taskId }

        return SchedulingOutcome(placements.sortedBy { it.window.startMillis }, sanitizedDeferrals)
    }

    internal fun splitIntoUltradianParts(
        task: SchedulableTask,
        totalDurationMinutes: Int
    ): Pair<SchedulableTask, SchedulableTask>? {
        if (totalDurationMinutes < 60 || task.isLocked || task.id.contains("#part")) return null
        if (task.fragmentationTolerance < 0.15f) return null

        val part1Mins = (totalDurationMinutes / 2).coerceAtLeast(25)
        val part2Mins = (totalDurationMinutes - part1Mins).coerceAtLeast(25)
        if (part1Mins + part2Mins > totalDurationMinutes + 5) return null

        val part1Id = "${task.id}#part1"
        val part2Id = "${task.id}#part2"

        val part1 = task.copy(
            id = part1Id,
            title = "${task.title} (Part 1/2)",
            estimatedMinutes = part1Mins,
            isDurationExplicit = true
        )
        val part2 = task.copy(
            id = part2Id,
            title = "${task.title} (Part 2/2)",
            estimatedMinutes = part2Mins,
            isDurationExplicit = true,
            prerequisiteIds = task.prerequisiteIds + part1Id
        )
        return part1 to part2
    }

    internal fun computeContextTransitionDelta(
        placements: List<ScheduledPlacement>,
        candidateSlotStartMillis: Long,
        candidate: SchedulableTask,
        candidateSlotEndMillis: Long = candidateSlotStartMillis
    ): Double {
        if (placements.isEmpty()) return 0.0

        val preceding = placements
            .filter { it.window.endMillis <= candidateSlotStartMillis }
            .maxByOrNull { it.window.endMillis }

        val succeeding = placements
            .filter { it.window.startMillis >= candidateSlotEndMillis }
            .minByOrNull { it.window.startMillis }

        fun scoreNeighbor(neighborTask: SchedulableTask, gapMinutes: Long): Double {
            if (gapMinutes < 0 || gapMinutes > 45) return 0.0
            val sameContextTag = neighborTask.contextTag.isNotBlank() &&
                neighborTask.contextTag.equals(candidate.contextTag, ignoreCase = true)

            if (neighborTask.category == candidate.category && sameContextTag) {
                return 8.5
            }
            if (neighborTask.category == candidate.category || sameContextTag) {
                return 6.0
            }

            val isHighEntropy = (neighborTask.category == TaskCategory.EXERCISE && candidate.category == TaskCategory.DEEP_WORK && gapMinutes < 15) ||
                (neighborTask.category == TaskCategory.DEEP_WORK && candidate.category == TaskCategory.ADMIN && gapMinutes < 10)

            return if (isHighEntropy) -8.5 else -1.5
        }

        val precDelta = preceding?.let {
            val gap = (candidateSlotStartMillis - it.window.endMillis) / 60_000L
            scoreNeighbor(it.task, gap)
        }
        val succDelta = succeeding?.let {
            val gap = (it.window.startMillis - candidateSlotEndMillis) / 60_000L
            scoreNeighbor(it.task, gap)
        }

        return when {
            precDelta != null && succDelta != null -> ((precDelta + succDelta) * 0.75).coerceIn(-12.0, 12.0)
            precDelta != null -> precDelta
            succDelta != null -> succDelta
            else -> 0.0
        }
    }

    private fun computeBufferedDuration(task: SchedulableTask, calibrationRatio: Double): Int {
        val raw = max(5, task.estimatedMinutes)
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
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        latestAllowedEndMillis: Long = Long.MAX_VALUE,
        referenceNowMillis: Long? = null
    ): Pair<TimeSlotWindow, TaskScoreBreakdown>? {
        var bestWindow: TimeSlotWindow? = null
        var bestBreakdown: TaskScoreBreakdown? = null
        var bestUtility = Double.NEGATIVE_INFINITY
        val stepMillis = 15 * 60_000L

        for (interval in freeIntervals) {
            var candidateStart = max(interval.startMillis, earliestStartMillis)
            val effectiveIntervalEnd = min(interval.endMillis, latestAllowedEndMillis)
            while (candidateStart + durationMillis <= effectiveIntervalEnd) {
                val candidateEnd = candidateStart + durationMillis
                val midpoint = candidateStart + durationMillis / 2
                val simulatedFocus = computeSimulatedFocusLoadAt(candidateStart, placements)
                val snap = circadianService.evaluateSnapshotWithStimulants(
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
                    cognitiveBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
                )
                val breakdown = evaluateTaskScoreWithWeights(
                    task = task,
                    snapshot = snap,
                    dependentCount = dependentCount,
                    calibrationRatio = calibrationRatio,
                    weights = priorityWeights,
                    referenceNowMillis = referenceNowMillis ?: earliestStartMillis
                )
                val hourlyDecayLambda = when {
                    task.energyDemand == EnergyDemand.HIGH ||
                    task.category == TaskCategory.DEEP_WORK ||
                    task.category == TaskCategory.CREATIVE -> 0.08
                    task.energyDemand == EnergyDemand.MODERATE -> 0.22
                    else -> 0.45
                }

                val peakCannibalizationPenalty = if (
                    snap.phase == EnergyPhase.PEAK &&
                    breakdown.urgencyScore < 75.0 &&
                    (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN)
                ) {
                    16.0
                } else {
                    0.0
                }

                // Feature 1 (ADR-023): Pharmacokinetic-Aware Scheduling & Crash Shield
                val stimEffect = if (stimulantDoses.isNotEmpty()) {
                    PharmacokineticStimulantCalculator.evaluateAt(candidateStart, stimulantDoses)
                } else null

                val stimAlignmentDelta = if (stimEffect != null) {
                    val alertBoost = stimEffect.acuteAlertnessBoostPoints
                    if (task.energyDemand == EnergyDemand.HIGH || task.isFrog || task.category == TaskCategory.DEEP_WORK) {
                        when {
                            alertBoost > 4.0 -> 8.0 // Aligns deep focus with Bateman peak plasma concentration
                            alertBoost < -1.5 -> -18.0 // Crash Shield: actively penalizes deep work during adenosine rebound crash
                            else -> 0.0
                        }
                    } else if (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN || task.category == TaskCategory.MINDFULNESS) {
                        if (alertBoost < -1.5) 6.0 else 0.0 // Crash Recovery: routes light administrative work into crash window
                    } else 0.0
                } else 0.0

                // Feature 2 (ADR-023): Bidirectional Context-Cluster Scoring
                val contextDelta = computeContextTransitionDelta(placements, candidateStart, task, candidateEnd)
                val hoursFromNow = (candidateStart - earliestStartMillis) / 3_600_000.0

                // Bug 1 & Feature 1 (ADR-025): Hard Deadline Breach Penalty (-35.0 pts)
                val deadlineBreachPenalty = if (task.deadlineMillis != null && candidateEnd > task.deadlineMillis) {
                    35.0
                } else {
                    0.0
                }

                val utility = breakdown.totalScore - (hourlyDecayLambda * hoursFromNow) - peakCannibalizationPenalty + contextDelta + stimAlignmentDelta - deadlineBreachPenalty

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
