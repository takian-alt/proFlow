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
        activeUncompletedTaskIds: Set<String> = emptySet(),
        workDayStartHour: Int? = null,
        workDayEndHour: Int? = null
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
        activeUncompletedTaskIds: Set<String> = emptySet(),
        workDayStartHour: Int? = null,
        workDayEndHour: Int? = null
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
        val lowFrictionBonus = (15.0 * exp(-max(5, task.estimatedMinutes) / 45.0)) * weights.durationWeight.coerceIn(0.2, 3.0)
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
        activeUncompletedTaskIds: Set<String>,
        workDayStartHour: Int?,
        workDayEndHour: Int?
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
        activeUncompletedTaskIds = activeUncompletedTaskIds,
        workDayStartHour = workDayStartHour,
        workDayEndHour = workDayEndHour
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
        activeUncompletedTaskIds: Set<String>,
        workDayStartHour: Int?,
        workDayEndHour: Int?
    ): SchedulingOutcome {
        if (tasks.isEmpty()) return SchedulingOutcome(emptyList())

        // 0. Cycle-Safe DAG Sanitization (ADR-020): guarantees circular dependencies never starve Kahn's queue
        val sanitizedTasks = DependencyGraphValidator.sanitizeDag(tasks).tasks

        // 1. Construct DST-safe day horizon respecting user's workDayStart and workDayEnd preferences
        val nowLocalDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val isToday = dayDate == nowLocalDate
        val isPast = dayDate < nowLocalDate

        val earliestDayStart = if (workDayStartHour != null) {
            dayDate.atTime(LocalTime.of(workDayStartHour.coerceIn(0, 23), 0)).atZone(zoneId).toInstant().toEpochMilli()
        } else {
            dayDate.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli()
        }
        val latestDayEnd = if (workDayEndHour != null) {
            val endHour = workDayEndHour.coerceIn(0, 23)
            if (endHour == 0) {
                dayDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
            } else {
                dayDate.atTime(LocalTime.of(endHour, 0)).atZone(zoneId).toInstant().toEpochMilli()
            }
        } else {
            dayDate.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()
        }
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val effectiveWakeTimeMillis = if (zonedWake.toLocalDate() != dayDate) {
            dayDate.atTime(zonedWake.toLocalTime()).atZone(zoneId).toInstant().toEpochMilli()
        } else {
            wakeTimeMillis
        }
        val effectiveWakeStart = max(earliestDayStart, effectiveWakeTimeMillis + 15 * 60_000L)
        val dayEndMillis = if (workDayEndHour != null) latestDayEnd else max(latestDayEnd, effectiveWakeStart + 4 * 3600_000L)
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
                    wakeTimeMillis = effectiveWakeTimeMillis,
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
                val isPart1 = task.id.endsWith("#part1") || task.id.endsWith("_part1") || task.title.contains("(Part 1/")
                val bufferMins = if (isPart1) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
                val nextLockedStart = sanitizedTasks.filter { it.isLocked && it.scheduledStartMillis != null }
                    .mapNotNull { it.scheduledStartMillis }
                    .filter { it >= existingWindow.endMillis }
                    .minOrNull()
                val effectiveBufferMillis = if (nextLockedStart != null) {
                    min(bufferMins * 60_000L, max(0L, nextLockedStart - existingWindow.endMillis))
                } else {
                    bufferMins * 60_000L
                }
                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(
                        existingWindow.startMillis,
                        existingWindow.endMillis + effectiveBufferMillis
                    )
                )
                completionEndMap[task.id] = existingWindow.endMillis
            } else {
                remainingTasks.add(task)
            }
        }

        // Feature 2 (ADR-024): Reverse-DAG Deadline & Locked-Dependent Ceiling Propagation
        val latestAllowedEndMap = mutableMapOf<String, Long>()
        val structuralCeilingMap = mutableMapOf<String, Long>()
        for (task in sanitizedTasks) {
            val deadlineCeiling = task.deadlineMillis ?: Long.MAX_VALUE
            val taskBufferedMins = computeBufferedDuration(task, calibrationRatio)
            val taskDurationMillis = taskBufferedMins * 60_000L
            val placedWindow = placements.firstOrNull { it.task.id == task.id }?.window
            val directCeiling = when {
                placedWindow != null -> placedWindow.endMillis
                task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!! + taskDurationMillis
                else -> deadlineCeiling
            }
            latestAllowedEndMap[task.id] = min(deadlineCeiling, directCeiling)

            val directStructural = when {
                placedWindow != null -> placedWindow.startMillis
                task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!!
                else -> Long.MAX_VALUE
            }
            structuralCeilingMap[task.id] = directStructural
        }

        var dagCeilingChanged = true
        var dagIterations = 0
        while (dagCeilingChanged && dagIterations < sanitizedTasks.size + 1) {
            dagCeilingChanged = false
            dagIterations++
            for (task in sanitizedTasks) {
                val childCeiling = latestAllowedEndMap[task.id] ?: Long.MAX_VALUE
                val childStructural = structuralCeilingMap[task.id] ?: Long.MAX_VALUE

                val childBufferedMins = computeBufferedDuration(task, calibrationRatio)
                val childDurationMillis = childBufferedMins * 60_000L
                val placedWindow = placements.firstOrNull { it.task.id == task.id }?.window
                val isLockedWithStart = task.isLocked && task.scheduledStartMillis != null
                val childLockedStart = if (isLockedWithStart) task.scheduledStartMillis!! else null

                val childLatestStart = when {
                    placedWindow != null -> placedWindow.startMillis
                    childLockedStart != null -> childLockedStart
                    childCeiling != Long.MAX_VALUE -> childCeiling - childDurationMillis
                    else -> Long.MAX_VALUE
                }

                val childStructuralStart = when {
                    placedWindow != null -> placedWindow.startMillis
                    childLockedStart != null -> childLockedStart
                    childStructural != Long.MAX_VALUE -> childStructural - childDurationMillis
                    else -> Long.MAX_VALUE
                }

                // BUG-329 Fix: Support both _part2 and #part2 (and parentTaskId) for ultradian dependencies
                val isUltradianDep = task.id.endsWith("#part2") || task.id.endsWith("_part2") ||
                    task.parentTaskId != null ||
                    task.prerequisiteIds.any { it.endsWith("#part1") || it.endsWith("_part1") }
                val bufferBeforeChild = if (isUltradianDep) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
                val bufferMillis = bufferBeforeChild * 60_000L

                val allPrereqs = task.prerequisiteIds
                for (prereqId in allPrereqs) {
                    if (childLatestStart != Long.MAX_VALUE) {
                        val parentMaxEnd = childLatestStart - bufferMillis
                        val currentParentCeiling = latestAllowedEndMap[prereqId] ?: Long.MAX_VALUE
                        if (parentMaxEnd < currentParentCeiling) {
                            latestAllowedEndMap[prereqId] = parentMaxEnd
                            dagCeilingChanged = true
                        }
                    }
                    if (childStructuralStart != Long.MAX_VALUE) {
                        val parentStructuralEnd = childStructuralStart - bufferMillis
                        val currentParentStructural = structuralCeilingMap[prereqId] ?: Long.MAX_VALUE
                        if (parentStructuralEnd < currentParentStructural) {
                            structuralCeilingMap[prereqId] = parentStructuralEnd
                            dagCeilingChanged = true
                        }
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
            wakeTimeMillis = effectiveWakeTimeMillis,
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
            val isUltradianPrereq = current.id.endsWith("#part2") || current.id.endsWith("_part2") ||
                current.parentTaskId != null ||
                allPrereqs.any { it.endsWith("#part1") || it.endsWith("_part1") }
            val prereqBufferMinutes = if (isUltradianPrereq) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
            val maxPrereqEnd = allPrereqs.mapNotNull { completionEndMap[it] }.maxOrNull()
            val earliestStart = if (maxPrereqEnd != null) {
                max(dayStartMillis, maxPrereqEnd + prereqBufferMinutes * 60_000L)
            } else {
                dayStartMillis
            }

            val taskCeiling = latestAllowedEndMap[current.id] ?: Long.MAX_VALUE
            val structuralCeiling = structuralCeilingMap[current.id] ?: Long.MAX_VALUE
            val lockedStart = current.scheduledStartMillis
            val lockedWindow = if (current.isLocked && lockedStart != null && lockedStart >= earliestStart) {
                val candidateWindow = TimeSlotWindow(lockedStart, lockedStart + durationMillis)
                if (candidateWindow.endMillis <= taskCeiling && fitsInFreeIntervals(freeIntervals, candidateWindow)) {
                    val snap = circadianService.evaluateSnapshotWithStimulants(
                        timestampMillis = candidateWindow.startMillis + durationMillis / 2,
                        wakeTimeMillis = effectiveWakeTimeMillis,
                        lastSleepDurationMinutes = lastSleepDurationMinutes,
                        continuousActiveFocusMinutes = computeSimulatedFocusLoadAt(candidateWindow.startMillis, placements),
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
                    val slotBreakdown = evaluateTaskScoreWithWeights(
                        task = current,
                        snapshot = snap,
                        dependentCount = dependentsMap[current.id]?.size ?: 0,
                        calibrationRatio = calibrationRatio,
                        weights = priorityWeights,
                        referenceNowMillis = nowMillis
                    )
                    candidateWindow to slotBreakdown
                } else null
            } else null

            val primarySlot = if (current.isLocked) {
                lockedWindow
            } else {
                findBestCircadianWindow(
                    task = current,
                    durationMillis = durationMillis,
                    earliestStartMillis = earliestStart,
                    freeIntervals = freeIntervals,
                    wakeTimeMillis = effectiveWakeTimeMillis,
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
                        wakeTimeMillis = effectiveWakeTimeMillis,
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
            }

            // BUG-247 Fix: If task cannot fit before its deadline (or is overdue), fall back to best
            // available circadian slot before structural dependent constraints with deadline breach penalty.
            val fallbackStructuralSlot = if (primarySlot == null && !current.isLocked && structuralCeiling > taskCeiling) {
                findBestCircadianWindow(
                    task = current,
                    durationMillis = durationMillis,
                    earliestStartMillis = earliestStart,
                    freeIntervals = freeIntervals,
                    wakeTimeMillis = effectiveWakeTimeMillis,
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
                    latestAllowedEndMillis = structuralCeiling,
                    referenceNowMillis = nowMillis
                ) ?: if (!current.isDurationExplicit && rawDurationMillis < durationMillis) {
                    findBestCircadianWindow(
                        task = current,
                        durationMillis = rawDurationMillis,
                        earliestStartMillis = earliestStart,
                        freeIntervals = freeIntervals,
                        wakeTimeMillis = effectiveWakeTimeMillis,
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
                        latestAllowedEndMillis = structuralCeiling,
                        referenceNowMillis = nowMillis
                    )
                } else null
            } else null

            val bestSlot = primarySlot ?: fallbackStructuralSlot

            if (bestSlot != null) {
                val (window, slotBreakdown) = bestSlot
                val finalBreakdown = if (window.durationMinutes < slotBreakdown.bufferedDurationMinutes) {
                    slotBreakdown.copy(bufferedDurationMinutes = window.durationMinutes)
                } else {
                    slotBreakdown
                }
                placements.add(ScheduledPlacement(current, window, finalBreakdown))
                completionEndMap[current.id] = window.endMillis
                val isSplitContinuation = current.id.endsWith("#part2") || current.id.endsWith("_part2")
                val parentId = if (isSplitContinuation) {
                    current.parentTaskId ?: cleanSplitTaskId(current.id)
                } else null
                if (parentId != null && parentId != current.id) {
                    completionEndMap[parentId] = window.endMillis
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

                val isPart1 = current.id.endsWith("#part1") || current.id.endsWith("_part1") || current.title.contains("(Part 1/")
                val bufferMins = if (isPart1) {
                    ULTRADIAN_RECOVERY_BUFFER_MINUTES
                } else if (contiguousCognitiveMins >= 90) {
                    15
                } else {
                    TRANSITION_BUFFER_MINUTES
                }

                val nextLockedStart = sanitizedTasks.filter { it.isLocked && it.scheduledStartMillis != null }
                    .mapNotNull { it.scheduledStartMillis }
                    .filter { it >= window.endMillis }
                    .minOrNull()
                val effectiveBufferMillis = if (nextLockedStart != null) {
                    min(bufferMins * 60_000L, max(0L, nextLockedStart - window.endMillis))
                } else {
                    bufferMins * 60_000L
                }

                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(window.startMillis, window.endMillis + effectiveBufferMillis)
                )

                // Decrement in-degree of direct children and (for split continuation parts) parent's downstream dependents
                val childrenToUnlock = (children[current.id].orEmpty() + (if (parentId != null && parentId != current.id) children[parentId].orEmpty() else emptyList())).distinctBy { it.id }
                childrenToUnlock.forEach { child ->
                    if (child.id != current.id) {
                        val newDeg = (inDegree[child.id] ?: 1) - 1
                        inDegree[child.id] = newDeg
                        if (newDeg == 0) readyQueue.add(child)
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
                    val structuralCeil = structuralCeilingMap[current.id] ?: Long.MAX_VALUE

                    fun attemptSplitPlacement(ceilingToUse: Long): Boolean {
                        // Phase 1: Test if part1 can fit in current freeIntervals.
                        // BUG-294 Fix: Require part1 to finish with enough headroom for the 15m ultradian buffer + part2 duration
                        // before ceilingToUse. Otherwise, part1 will consume the slot and make placing part2 impossible.
                        val p1BufferMins = 15
                        val p1Ceiling = if (ceilingToUse != Long.MAX_VALUE) {
                            ceilingToUse - (p1BufferMins * 60_000L + p2DurationMillis)
                        } else {
                            Long.MAX_VALUE
                        }
                        if (p1Ceiling < earliestStart + p1DurationMillis) return false

                        // BUG-250 Fix: Pass referenceNowMillis = nowMillis to prevent anchor drift
                        val p1Slot = findBestCircadianWindow(
                            task = part1,
                            durationMillis = p1DurationMillis,
                            earliestStartMillis = earliestStart,
                            freeIntervals = freeIntervals,
                            wakeTimeMillis = effectiveWakeTimeMillis,
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
                            latestAllowedEndMillis = p1Ceiling,
                            referenceNowMillis = nowMillis
                        ) ?: return false

                        val (p1Window, p1Breakdown) = p1Slot
                        // Require at least a 15-minute ultradian recovery buffer between part1 and part2
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
                            wakeTimeMillis = effectiveWakeTimeMillis,
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
                            latestAllowedEndMillis = ceilingToUse,
                            referenceNowMillis = nowMillis
                        ) ?: return false

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
                        return true
                    }

                    splitSuccess = attemptSplitPlacement(currentCeiling) ||
                        (structuralCeil > currentCeiling && attemptSplitPlacement(structuralCeil))
                }

                if (!splitSuccess) {
                    val baseId = cleanSplitTaskId(current.id)
                    if (deferrals.none { it.taskId == baseId }) {
                        val isDepBlocked = current.isLocked && earliestStart > (current.scheduledStartMillis ?: 0L)
                        val reasonCode = if (isDepBlocked) DeferralReasonCode.DEPENDENCY_BLOCKED else DeferralReasonCode.INSUFFICIENT_TIME
                        val explanation = if (current.isLocked) {
                            if (isDepBlocked) {
                                "Locked task '${current.title}' was blocked because a prerequisite completed after its pinned slot."
                            } else {
                                "Locked task '${current.title}' could not be placed in its pinned time window due to calendar conflict."
                            }
                        } else {
                            "Task '${current.title}' (${breakdown.bufferedDurationMinutes}m) could not find sufficient contiguous time or two-phase ultradian split slots today."
                        }
                        deferrals.add(
                            DeferralRationale(
                                taskId = baseId,
                                reasonCode = reasonCode,
                                explanation = explanation,
                                actionableSuggestion = if (current.isLocked) "Adjust the pinned slot or prerequisite schedules." else "Free up calendar slots or move to tomorrow's schedule."
                            )
                        )
                    }
                }
            }
        }

        // Invariant Safeguard (ADR-024): ensure no orphaned partial split remains in placements
        val orphanedPart1 = placements.filter { it.task.id.endsWith("#part1") || it.task.id.endsWith("_part1") }
            .filter { p1 ->
                val p2Id = if (p1.task.id.endsWith("#part1")) p1.task.id.replace("#part1", "#part2") else p1.task.id.replace("_part1", "_part2")
                placements.none { it.task.id == p2Id }
            }
        if (orphanedPart1.isNotEmpty()) {
            val orphanIds = orphanedPart1.map { it.task.id }.toSet()
            placements.removeAll { it.task.id in orphanIds }
            for (orphan in orphanedPart1) {
                val baseId = cleanSplitTaskId(orphan.task.id)
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

        // Bug 1 & Bug 15 Fix: Sweep remaining unplaced tasks in schedulableTodayTasks whose prerequisites failed placement or were omitted
        val schedulableTodayBaseIds = schedulableTodayTasks.map { cleanSplitTaskId(it.id) }.toSet()
        for (task in schedulableTodayTasks) {
            val baseId = cleanSplitTaskId(task.id)
            val isPlaced = placements.any { cleanSplitTaskId(it.task.id) == baseId }
            val isDeferred = deferrals.any { it.taskId == baseId }
            if (!isPlaced && !isDeferred) {
                val missingPrereq = task.prerequisiteIds.firstOrNull { prereqId ->
                    val cleanPrereq = cleanSplitTaskId(prereqId)
                    val isPlacedPrereq = placements.any { cleanSplitTaskId(it.task.id) == cleanPrereq }
                    if (isPlacedPrereq) return@firstOrNull false
                    if (activeUncompletedTaskIds.isNotEmpty()) {
                        cleanPrereq in activeUncompletedTaskIds || cleanPrereq in schedulableTodayBaseIds
                    } else {
                        true
                    }
                } ?: task.prerequisiteIds.firstOrNull { prereqId ->
                    placements.none { cleanSplitTaskId(it.task.id) == cleanSplitTaskId(prereqId) }
                }
                deferrals.add(
                    DeferralRationale(
                        taskId = baseId,
                        reasonCode = DeferralReasonCode.DEPENDENCY_BLOCKED,
                        explanation = if (missingPrereq != null) {
                            "Prerequisite task '$missingPrereq' could not be scheduled today."
                        } else {
                            "Task '${task.title}' could not be scheduled due to unmet dependency constraints."
                        },
                        actionableSuggestion = "Resolve prerequisite dependencies or adjust schedule."
                    )
                )
            }
        }

        // Clean deferral task IDs so none carry #part or _part suffixes
        val sanitizedDeferrals = deferrals.map { d ->
            if (d.taskId.contains("#part") || d.taskId.contains("_part")) d.copy(taskId = cleanSplitTaskId(d.taskId)) else d
        }.distinctBy { it.taskId }

        return SchedulingOutcome(placements.sortedBy { it.window.startMillis }, sanitizedDeferrals)
    }

    internal fun cleanSplitTaskId(id: String): String =
        id.substringBefore("#part").substringBefore("_part")

    internal fun splitIntoUltradianParts(
        task: SchedulableTask,
        totalDurationMinutes: Int
    ): Pair<SchedulableTask, SchedulableTask>? {
        if (totalDurationMinutes < 60 || task.isLocked || task.id.contains("#part") || task.id.contains("_part") || task.title.contains("(Part ") || task.parentTaskId != null) return null
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
            parentTaskId = task.id,
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
        // BUG-298 Fix: Zero-width or inverted blocked windows must not artificially fragment free intervals
        if (blocked.startMillis >= blocked.endMillis) return intervals.toMutableList()
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
