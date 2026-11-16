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
    val distractionScore: Float = 0f,
    val isPersonalTask: Boolean = false,
    val userSchedulingRating: Int = 0,
    val schedulingRejectedSlotHour: Int? = null
) {
    constructor(
        id: String,
        title: String,
        durationMinutes: Int,
        energyLevel: com.neuroflow.app.domain.model.EnergyLevel = com.neuroflow.app.domain.model.EnergyLevel.MEDIUM,
        quadrant: com.neuroflow.app.domain.model.Quadrant = com.neuroflow.app.domain.model.Quadrant.SCHEDULE,
        category: TaskCategory = TaskCategory.DEEP_WORK,
        priority: Int = when (quadrant) {
            com.neuroflow.app.domain.model.Quadrant.DO_FIRST -> 2
            com.neuroflow.app.domain.model.Quadrant.SCHEDULE -> 1
            else -> 0
        },
        estimatedMinutes: Int = durationMinutes,
        energyDemand: EnergyDemand = when (energyLevel) {
            com.neuroflow.app.domain.model.EnergyLevel.HIGH -> EnergyDemand.HIGH
            com.neuroflow.app.domain.model.EnergyLevel.LOW -> EnergyDemand.LOW
            else -> EnergyDemand.MODERATE
        }
    ) : this(
        id = id,
        title = title,
        category = category,
        priority = priority,
        estimatedMinutes = estimatedMinutes,
        energyDemand = energyDemand
    )
}

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
    val scoreBreakdown: TaskScoreBreakdown,
    val confidenceScore: Float = 1.0f,
    val rationale: String = ""
) {
    constructor(
        task: SchedulableTask,
        window: TimeSlotWindow,
        score: Double = 80.0
    ) : this(
        task = task,
        window = window,
        scoreBreakdown = TaskScoreBreakdown(
            taskId = task.id,
            totalScore = score,
            urgencyScore = score,
            impactScore = score,
            energyFitScore = score,
            momentumScore = score,
            bufferedDurationMinutes = window.durationMinutes
        )
    )
}

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
        workDayEndHour: Int? = null,
        dailyCognitiveCapacityMinutes: Int? = null,
        rejectionPatternsJson: String = "{}"
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
        workDayEndHour: Int? = null,
        dailyCognitiveCapacityMinutes: Int? = null,
        rejectionPatternsJson: String = "{}"
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
        val rawBasePriority = when {
            task.priority >= 2 -> 65.0
            task.priority == 1 -> 40.0
            else -> 15.0
        }
        val basePriority = (rawBasePriority * weights.priorityLevelWeight.coerceIn(0.2, 3.0)).coerceIn(0.0, 100.0)
        val impactAdj = if (task.impactScore != 50) {
            (task.impactScore - 50) * 0.30
        } else 0.0
        val frogBonus = if (task.isFrog) (25.0 * weights.frogBonusMultiplier.coerceIn(0.5, 2.0)) else 0.0
        val dependencyBonus = 10.0 * min(3, dependentCount)
        val personalBonus = if (task.isPersonalTask) 30.0 else 0.0
        val impact = (basePriority + impactAdj + frogBonus + dependencyBonus + personalBonus).coerceIn(0.0, 100.0)

        // 3. Energy & Category Fit Vector E in [0..100] + Dread Desensitization Routing
        val energyDiff = snapshot.usableEnergy.toDouble() - task.energyDemand.targetScore
        val demandGaussian = 100.0 * exp(-(energyDiff * energyDiff) / (2.0 * 28.0 * 28.0))
        val rawCategoryAffinity = circadianService.computeCategoryAffinity(task.category, snapshot)
        val categoryAffinity = (rawCategoryAffinity * weights.focusModeWeight.coerceIn(0.2, 3.0)).coerceIn(0.0, 100.0)
        val dreadAlignmentAdj = if (task.isAnxietyTask || task.postponeCount >= 3) {
            when (snapshot.phase) {
                EnergyPhase.SURGE -> 12.0
                EnergyPhase.PEAK, EnergyPhase.REBOUND, EnergyPhase.MORNING_RAMP -> 8.0
                EnergyPhase.DIP, EnergyPhase.WIND_DOWN -> -10.0
                EnergyPhase.INERTIA, EnergyPhase.CRASH -> -15.0
                EnergyPhase.TROUGH, EnergyPhase.RECOVERY_NEEDED, EnergyPhase.RECOVERY -> -20.0
            }
        } else 0.0
        val distractionAdj = when {
            task.distractionScore > 0.3f && snapshot.phase == EnergyPhase.DIP -> -8.0 * task.distractionScore
            task.distractionScore > 0.3f && (snapshot.phase == EnergyPhase.INERTIA || snapshot.phase == EnergyPhase.CRASH) -> -12.0 * task.distractionScore
            else -> 0.0
        }
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
        workDayEndHour: Int?,
        dailyCognitiveCapacityMinutes: Int?,
        rejectionPatternsJson: String
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
        workDayEndHour = workDayEndHour,
        dailyCognitiveCapacityMinutes = dailyCognitiveCapacityMinutes,
        rejectionPatternsJson = rejectionPatternsJson
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
        workDayEndHour: Int?,
        dailyCognitiveCapacityMinutes: Int?,
        rejectionPatternsJson: String
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
                } else if ((prereqId == task.parentTaskId || prereqId == cleanSplitTaskId(task.id)) &&
                    (completionEndMap.keys.any { (cleanSplitTaskId(it) == prereqId || sanitizedTaskMap[it]?.parentTaskId == prereqId) && it != task.id })) {
                    // BUG-416 & BUG-450 Fix: Parent's prior part was anchored; check its end time against current window start
                    val parentPartEnd = completionEndMap.filter {
                        (cleanSplitTaskId(it.key) == prereqId || sanitizedTaskMap[it.key]?.parentTaskId == prereqId) && it.key != task.id
                    }.values.maxOrNull()
                    parentPartEnd != null && parentPartEnd <= existingWindow!!.startMillis
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
                val (conf, rat) = computePlacementConfidenceAndRationale(
                    task = task,
                    window = existingWindow,
                    breakdown = breakdown,
                    rejectionPenalty = 0.0,
                    zoneId = zoneId,
                    wakeTimeMillis = effectiveWakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes
                )
                placements.add(ScheduledPlacement(task, existingWindow, breakdown, conf, rat))
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
                // BUG-373 & BUG-449 Fix: Synchronize completionEndMap[parentId] ONLY when the TRUE final continuation part is anchored
                val isPart1Anchor = task.id.endsWith("#part1") || task.id.endsWith("_part1") || task.id.contains(Regex("""[#_]part1(\D|$)""")) || task.title.contains("(Part 1/")
                val isContinuationPart = !isPart1Anchor && (
                    task.id.endsWith("#part2") || task.id.endsWith("_part2") ||
                    task.id.contains(Regex("""[#_]part[2-9]""")) ||
                    task.title.contains(Regex("""\((Part|part) [2-9]/\d+\)""")) ||
                    (task.parentTaskId != null && task.title.contains(Regex("""\((Part|part) \d+/\d+\)""")))
                )
                val parentId = if (isContinuationPart) {
                    task.parentTaskId ?: cleanSplitTaskId(task.id)
                } else null
                val isFinalPart = if (isContinuationPart && parentId != null && parentId != task.id) {
                    val siblings = sanitizedTasks.filter { (it.parentTaskId == parentId || cleanSplitTaskId(it.id) == parentId) && it.id != parentId }
                    val finalSibling = siblings.maxWithOrNull(
                        compareBy<SchedulableTask> { DependencyGraphValidator.extractContinuationPartIndex(it) }.thenBy { it.id }
                    )
                    finalSibling == null || finalSibling.id == task.id
                } else false

                if (isFinalPart && parentId != null && parentId != task.id) {
                    completionEndMap[parentId] = existingWindow.endMillis
                }
            } else {
                remainingTasks.add(task)
            }
        }

        // Feature 2 (ADR-024): Reverse-DAG Deadline & Locked-Dependent Ceiling Propagation
        val latestAllowedEndMap = mutableMapOf<String, Long>()
        val structuralCeilingMap = mutableMapOf<String, Long>()
        for (task in sanitizedTasks) {
            val deadlineCeiling = task.deadlineMillis?.let { min(it, dayEndMillis) } ?: dayEndMillis
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
                prereqId !in completionEndMap &&
                // BUG-416 & BUG-450 Fix: If prereqId is the task's parent, and any part of the parent is present in today's tasks
                // or already placed today, it is scheduled today and not pending on a future date.
                !( (prereqId == task.parentTaskId || prereqId == cleanSplitTaskId(task.id)) &&
                   (sanitizedTaskMap.values.any { it.parentTaskId == prereqId || cleanSplitTaskId(it.id) == prereqId } ||
                    completionEndMap.keys.any { cleanSplitTaskId(it) == prereqId || sanitizedTaskMap[it]?.parentTaskId == prereqId }) )
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
            compareByDescending<SchedulableTask> { it.isPersonalTask }
                .thenByDescending { task ->
                    val baseScore = scoreCache[task.id]?.totalScore ?: 0.0
                    val scarcityBonus = when (task.energyDemand) {
                        EnergyDemand.HIGH -> 12.0
                        EnergyDemand.MODERATE -> 4.0
                        EnergyDemand.LOW -> 0.0
                    }
                    val slackPullForwardBonus = if (task.deadlineMillis != null) {
                        val hoursRemaining = (task.deadlineMillis - nowMillis) / 3_600_000.0
                        val durHours = max(5, task.estimatedMinutes) / 60.0
                        val slack = hoursRemaining - durHours
                        when {
                            slack <= 2.0 -> 40.0
                            slack <= 4.0 -> 20.0
                            else -> 0.0
                        }
                    } else 0.0
                    baseScore + scarcityBonus + slackPullForwardBonus
                }.thenBy { it.id }
        )
        schedulableTodayTasks.filter { (inDegree[it.id] ?: 0) == 0 }.forEach { readyQueue.add(it) }

        while (readyQueue.isNotEmpty()) {
            val current = readyQueue.poll() ?: break
            val breakdown = scoreCache[current.id]
                ?: evaluateTaskScoreWithWeights(current, nowSnapshot, dependentsMap[current.id]?.size ?: 0, calibrationRatio, priorityWeights, referenceNowMillis = nowMillis)
            val durationMillis = breakdown.bufferedDurationMinutes * 60_000L
            val rawDurationMillis = max(5, current.estimatedMinutes) * 60_000L
            val baseId = cleanSplitTaskId(current.id)

            // Cognitive budget protection: if daily capacity is specified, non-locked tasks that exceed
            // remaining daily cognitive capacity are deferred to backlog to prevent daily overbooking.
            val currentPlacedMinutes = placements.sumOf { it.window.durationMinutes }
            val taskDurationMinutes = breakdown.bufferedDurationMinutes
            if (!current.isLocked && dailyCognitiveCapacityMinutes != null && (currentPlacedMinutes + taskDurationMinutes > dailyCognitiveCapacityMinutes)) {
                if (deferrals.none { it.taskId == baseId }) {
                    deferrals.add(
                        DeferralRationale(
                            taskId = baseId,
                            reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                            explanation = "Daily cognitive budget of ${dailyCognitiveCapacityMinutes}m reached.",
                            actionableSuggestion = "Move to backlog or schedule for tomorrow's peak focus window."
                        )
                    )
                }
                continue
            }

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
                    referenceNowMillis = nowMillis,
                    rejectionPatternsJson = rejectionPatternsJson
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
                        referenceNowMillis = nowMillis,
                        rejectionPatternsJson = rejectionPatternsJson
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
                    referenceNowMillis = nowMillis,
                    allowDeadlineBreach = true,
                    rejectionPatternsJson = rejectionPatternsJson
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
                        referenceNowMillis = nowMillis,
                        allowDeadlineBreach = true,
                        rejectionPatternsJson = rejectionPatternsJson
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
                val rejPen = computeRejectionPenalty(current, window.startMillis, zoneId, rejectionPatternsJson)
                val (conf, rat) = computePlacementConfidenceAndRationale(
                    task = current,
                    window = window,
                    breakdown = finalBreakdown,
                    rejectionPenalty = rejPen,
                    zoneId = zoneId,
                    wakeTimeMillis = effectiveWakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes
                )
                placements.add(ScheduledPlacement(current, window, finalBreakdown, conf, rat))
                completionEndMap[current.id] = window.endMillis
                // BUG-373 & BUG-449 Fix: Support parentTaskId and continuation markers, updating parentId and unlocking downstream tasks ONLY on the final continuation part
                val isPart1Current = current.id.endsWith("#part1") || current.id.endsWith("_part1") || current.id.contains(Regex("""[#_]part1(\D|$)""")) || current.title.contains("(Part 1/")
                val isContinuationPartCurrent = !isPart1Current && (
                    current.id.endsWith("#part2") || current.id.endsWith("_part2") ||
                    current.id.contains(Regex("""[#_]part[2-9]""")) ||
                    current.title.contains(Regex("""\((Part|part) [2-9]/\d+\)""")) ||
                    (current.parentTaskId != null && current.title.contains(Regex("""\((Part|part) \d+/\d+\)""")))
                )
                val parentId = if (isContinuationPartCurrent) {
                    current.parentTaskId ?: cleanSplitTaskId(current.id)
                } else null
                val isFinalPartCurrent = if (isContinuationPartCurrent && parentId != null && parentId != current.id) {
                    val siblings = sanitizedTasks.filter { (it.parentTaskId == parentId || cleanSplitTaskId(it.id) == parentId) && it.id != parentId }
                    val finalSibling = siblings.maxWithOrNull(
                        compareBy<SchedulableTask> { DependencyGraphValidator.extractContinuationPartIndex(it) }.thenBy { it.id }
                    )
                    finalSibling == null || finalSibling.id == current.id
                } else false

                if (isFinalPartCurrent && parentId != null && parentId != current.id) {
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

                // Decrement in-degree of direct children and (only when the true final continuation part completes) parent's downstream dependents
                val childrenToUnlock = (children[current.id].orEmpty() + (if (isFinalPartCurrent && parentId != null && parentId != current.id) children[parentId].orEmpty() else emptyList())).distinctBy { it.id }
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

                    fun attemptSplitPlacement(ceilingToUse: Long, allowBreach: Boolean = false): Boolean {
                        // Phase 1: Test if part1 can fit in current freeIntervals.
                        // BUG-294 & BUG-374 Fix: Require part1 to finish with enough headroom for the 15m ultradian buffer + part2 duration
                        // before ceilingToUse and dayEndMillis. Otherwise, part1 will consume the slot and make placing part2 impossible.
                        val p1BufferMins = 15
                        val effectiveCeiling = min(ceilingToUse, dayEndMillis)
                        val p1Ceiling = if (effectiveCeiling != Long.MAX_VALUE) {
                            effectiveCeiling - (p1BufferMins * 60_000L + p2DurationMillis)
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
                            referenceNowMillis = nowMillis,
                            allowDeadlineBreach = allowBreach,
                            rejectionPatternsJson = rejectionPatternsJson
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
                            referenceNowMillis = nowMillis,
                            allowDeadlineBreach = allowBreach,
                            rejectionPatternsJson = rejectionPatternsJson
                        ) ?: return false

                        // Both parts verified! Commit both atomically to placements.
                        val (p2Window, p2Breakdown) = p2Slot
                        val p1Rej = computeRejectionPenalty(part1, p1Window.startMillis, zoneId, rejectionPatternsJson)
                        val (p1Conf, p1Rat) = computePlacementConfidenceAndRationale(
                            task = part1,
                            window = p1Window,
                            breakdown = p1Breakdown,
                            rejectionPenalty = p1Rej,
                            zoneId = zoneId,
                            wakeTimeMillis = effectiveWakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes
                        )
                        val p2Rej = computeRejectionPenalty(part2, p2Window.startMillis, zoneId, rejectionPatternsJson)
                        val (p2Conf, p2Rat) = computePlacementConfidenceAndRationale(
                            task = part2,
                            window = p2Window,
                            breakdown = p2Breakdown,
                            rejectionPenalty = p2Rej,
                            zoneId = zoneId,
                            wakeTimeMillis = effectiveWakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes
                        )
                        placements.add(ScheduledPlacement(part1, p1Window, p1Breakdown, p1Conf, p1Rat))
                        placements.add(ScheduledPlacement(part2, p2Window, p2Breakdown, p2Conf, p2Rat))

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
                            if (newDeg == 0) readyQueue.add(child)
                        }
                        return true
                    }

                    splitSuccess = attemptSplitPlacement(currentCeiling) ||
                        (structuralCeil > currentCeiling && attemptSplitPlacement(structuralCeil, allowBreach = true))
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

        // Bug 1 & Bug 15 & BUG-375 Fix: Sweep remaining unplaced tasks in schedulableTodayTasks whose prerequisites failed placement or were omitted
        val schedulableTodayBaseIds = schedulableTodayTasks.map { cleanSplitTaskId(it.id) }.toSet()
        for (task in schedulableTodayTasks) {
            val baseId = cleanSplitTaskId(task.id)
            val isPlaced = placements.any {
                it.task.id == task.id ||
                (it.task.id.startsWith("${task.id}#part") || it.task.id.startsWith("${task.id}_part"))
            }
            val isDeferred = deferrals.any { it.taskId == task.id || it.taskId == baseId }
            if (!isPlaced && !isDeferred) {
                val missingPrereq = task.prerequisiteIds.firstOrNull { prereqId ->
                    val cleanPrereq = cleanSplitTaskId(prereqId)
                    val isPlacedPrereq = placements.any {
                        it.task.id == prereqId ||
                        it.task.id == "${prereqId}#part2" ||
                        it.task.id == "${prereqId}_part2" ||
                        ((cleanSplitTaskId(it.task.id) == cleanPrereq || it.task.parentTaskId == prereqId) && (
                            it.task.id.contains(Regex("""[#_]part[2-9]""")) ||
                            it.task.parentTaskId != null ||
                            it.task.title.contains(Regex("""\((Part|part) [2-9]/\d+\)"""))
                        ))
                    }
                    if (isPlacedPrereq) return@firstOrNull false
                    if (activeUncompletedTaskIds.isNotEmpty()) {
                        cleanPrereq in activeUncompletedTaskIds || cleanPrereq in schedulableTodayBaseIds
                    } else {
                        true
                    }
                } ?: task.prerequisiteIds.firstOrNull { prereqId ->
                    placements.none {
                        it.task.id == prereqId ||
                        it.task.id == "${prereqId}#part2" ||
                        it.task.id == "${prereqId}_part2" ||
                        cleanSplitTaskId(it.task.id) == cleanSplitTaskId(prereqId) ||
                        it.task.parentTaskId == prereqId
                    }
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

        // Invariant Safeguard (ADR-031): Guarantee 0 overlapping windows in placements
        val sortedCandidates = placements.sortedBy { it.window.startMillis }.toMutableList()
        val sanitizedPlacements = mutableListOf<ScheduledPlacement>()
        for (candidatePlacement in sortedCandidates) {
            val overlapping = sanitizedPlacements.firstOrNull { existing ->
                max(existing.window.startMillis, candidatePlacement.window.startMillis) <
                    min(existing.window.endMillis, candidatePlacement.window.endMillis)
            }
            if (overlapping != null) {
                // Collision violation: Keep locked/higher priority task, defer the other
                if (candidatePlacement.task.isLocked && !overlapping.task.isLocked) {
                    sanitizedPlacements.remove(overlapping)
                    sanitizedPlacements.add(candidatePlacement)
                    val baseId = cleanSplitTaskId(overlapping.task.id)
                    deferrals.add(
                        DeferralRationale(
                            taskId = baseId,
                            reasonCode = DeferralReasonCode.TASK_COLLISION,
                            explanation = "Task '${overlapping.task.title}' was displaced by locked placement '${candidatePlacement.task.title}'.",
                            actionableSuggestion = "Move to tomorrow or adjust locked slot."
                        )
                    )
                } else {
                    val baseId = cleanSplitTaskId(candidatePlacement.task.id)
                    deferrals.add(
                        DeferralRationale(
                            taskId = baseId,
                            reasonCode = DeferralReasonCode.TASK_COLLISION,
                            explanation = "Task '${candidatePlacement.task.title}' conflicted with '${overlapping.task.title}'.",
                            actionableSuggestion = "Move to tomorrow or rebalance."
                        )
                    )
                }
            } else {
                sanitizedPlacements.add(candidatePlacement)
            }
        }

        // ADR-061: Pairwise 2-Opt Schedule Utility Optimization
        val optimizedPlacements = optimizePlacementsWith2Opt(
            placements = sanitizedPlacements,
            wakeTimeMillis = effectiveWakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            zoneId = zoneId,
            priorityWeights = priorityWeights,
            referenceNowMillis = nowMillis,
            calibrationRatio = calibrationRatio,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            physiologicalBlocks = physiologicalBlocks,
            stimulantDoses = stimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours,
            rejectionPatternsJson = rejectionPatternsJson
        )

        // Clean deferral task IDs so none carry #part or _part suffixes (BUG-375: regex-safe)
        val sanitizedDeferrals = deferrals.map { d ->
            if (d.taskId.contains(Regex("""[#_]part\d+"""))) d.copy(taskId = cleanSplitTaskId(d.taskId)) else d
        }.distinctBy { it.taskId }

        return SchedulingOutcome(optimizedPlacements.sortedBy { it.window.startMillis }, sanitizedDeferrals)
    }

    internal fun cleanSplitTaskId(id: String): String =
        id.replace(Regex("""[#_]part\d+.*$"""), "")

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
        val relevant = placements
            .filter { it.window.endMillis <= timestampMillis }
            .sortedBy { it.window.startMillis }
        if (relevant.isEmpty()) return 0

        var currentLoad = 0.0
        var lastEnd = relevant.first().window.startMillis

        for (p in relevant) {
            val gapMinutes = (p.window.startMillis - lastEnd) / 60_000.0
            if (gapMinutes >= 15.0) {
                currentLoad *= exp(-gapMinutes / 20.0)
                if (gapMinutes >= 60.0 || currentLoad < 4.0) currentLoad = 0.0
            }
            val isCognitive = p.task.energyDemand == EnergyDemand.HIGH ||
                p.task.category == TaskCategory.DEEP_WORK ||
                p.task.category == TaskCategory.CREATIVE
            if (isCognitive) {
                currentLoad += p.window.durationMinutes
            } else if (p.window.durationMinutes >= 15) {
                currentLoad *= exp(-p.window.durationMinutes / 20.0)
                if (currentLoad < 4.0) currentLoad = 0.0
            }
            lastEnd = p.window.endMillis
        }

        val finalGapMinutes = (timestampMillis - lastEnd) / 60_000.0
        if (finalGapMinutes >= 15.0) {
            currentLoad *= exp(-finalGapMinutes / 20.0)
            if (finalGapMinutes >= 60.0 || currentLoad < 4.0) currentLoad = 0.0
        }
        return currentLoad.roundToInt().coerceAtLeast(0)
    }

    private fun computeRejectionPenalty(
        task: SchedulableTask,
        startMillis: Long,
        zoneId: ZoneId,
        rejectionPatternsJson: String
    ): Double {
        val hour = Instant.ofEpochMilli(startMillis).atZone(zoneId).hour
        val historical = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.computePenalty(
            json = rejectionPatternsJson,
            category = task.category.name,
            hour = hour,
            nowMillis = startMillis
        )
        val direct = if (task.userSchedulingRating == -1 && task.schedulingRejectedSlotHour == hour) 30.0 else 0.0
        return historical + direct
    }

    private fun computePlacementConfidenceAndRationale(
        task: SchedulableTask,
        window: TimeSlotWindow,
        breakdown: TaskScoreBreakdown,
        rejectionPenalty: Double,
        zoneId: ZoneId,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int
    ): Pair<Float, String> {
        val energyFitNormalized = (breakdown.energyFitScore / 100.0).coerceIn(0.0, 1.0)
        val urgencyNormalized = (breakdown.urgencyScore / 100.0).coerceIn(0.0, 1.0)
        val impactNormalized = (breakdown.impactScore / 100.0).coerceIn(0.0, 1.0)
        val penaltyDeduction = (rejectionPenalty / 40.0).coerceIn(0.0, 0.4)

        val rawConfidence = ((energyFitNormalized * 0.45 + urgencyNormalized * 0.25 + impactNormalized * 0.30) - penaltyDeduction).toFloat()
        val confidence = rawConfidence.coerceIn(0.15f, 1.0f)

        val snap = circadianService.evaluateSnapshot(
            timestampMillis = window.startMillis + (window.durationMinutes * 30_000L),
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            zoneId = zoneId
        )
        val phaseName = snap.phase.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        val fitPct = breakdown.energyFitScore.roundToInt()
        val rationale = when {
            task.isLocked -> "Pinned to this time window by user."
            rejectionPenalty > 15.0 -> "Lower confidence: scheduled here despite historical rejection due to tight schedule constraints."
            breakdown.urgencyScore >= 80.0 -> "Prioritized here due to impending deadline ($fitPct% energy match)."
            snap.phase == EnergyPhase.PEAK || snap.phase == EnergyPhase.SURGE || snap.phase == EnergyPhase.MORNING_RAMP -> "Aligned to your $phaseName window ($fitPct% energy match)."
            else -> "Matched to $phaseName energy phase ($fitPct% match)."
        }
        return confidence to rationale
    }

    internal fun findBestCircadianWindow(
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
        referenceNowMillis: Long? = null,
        allowDeadlineBreach: Boolean = false,
        rejectionPatternsJson: String = "{}"
    ): Pair<TimeSlotWindow, TaskScoreBreakdown>? {
        var bestWindow: TimeSlotWindow? = null
        var bestBreakdown: TaskScoreBreakdown? = null
        var bestUtility = Double.NEGATIVE_INFINITY
        val stepMillis = 15 * 60_000L

        for (interval in freeIntervals) {
            val candidateStarts = mutableListOf<Long>()
            var cur = max(interval.startMillis, earliestStartMillis)
            val effectiveIntervalEnd = min(interval.endMillis, latestAllowedEndMillis)
            while (cur + durationMillis <= effectiveIntervalEnd) {
                candidateStarts.add(cur)
                cur += stepMillis
            }
            // ADR-061: Boundary-aligned slot packing (snug interval end anchor before meetings/protected slots)
            val maxHorizonEnd = freeIntervals.maxOfOrNull { it.endMillis } ?: Long.MAX_VALUE
            if (effectiveIntervalEnd < maxHorizonEnd || latestAllowedEndMillis != Long.MAX_VALUE) {
                val snugEndStart = effectiveIntervalEnd - durationMillis
                if (snugEndStart >= max(interval.startMillis, earliestStartMillis) && snugEndStart !in candidateStarts) {
                    candidateStarts.add(snugEndStart)
                    candidateStarts.sort()
                }
            }

            for (candidateStart in candidateStarts) {
                val candidateEnd = candidateStart + durationMillis
                // BUG-AUDIT-002: Hard filter post-deadline candidates in primary search so they cannot displace valid slots
                if (!allowDeadlineBreach && task.deadlineMillis != null && candidateEnd > task.deadlineMillis) {
                    continue
                }

                val isCognitiveHeavy = task.energyDemand == EnergyDemand.HIGH ||
                    task.category == TaskCategory.DEEP_WORK ||
                    task.category == TaskCategory.CREATIVE

                val simulatedFocus = computeSimulatedFocusLoadAt(candidateStart, placements)

                // ADR-061: Ultradian Recovery Gap Enforcement
                // When contiguous deep-work load reaches >= 90m, candidate cognitive-heavy tasks
                // must allow at least a 15-minute recovery gap after the preceding cognitive block.
                if (isCognitiveHeavy && simulatedFocus >= 90) {
                    val lastCognitiveEnd = placements
                        .filter { it.window.endMillis <= candidateStart }
                        .filter { it.task.energyDemand == EnergyDemand.HIGH || it.task.category == TaskCategory.DEEP_WORK || it.task.category == TaskCategory.CREATIVE }
                        .maxOfOrNull { it.window.endMillis }
                    if (lastCognitiveEnd != null) {
                        val gapFromLast = (candidateStart - lastCognitiveEnd) / 60_000L
                        if (gapFromLast < 15L) {
                            continue
                        }
                    }
                }

                // ADR-061: 3-Point Simpson Gaussian Quadrature Energy Sampling (20%, 50%, 80%)
                val t20 = candidateStart + (durationMillis * 0.20).toLong()
                val t50 = candidateStart + durationMillis / 2
                val t80 = candidateStart + (durationMillis * 0.80).toLong()

                val snap50 = circadianService.evaluateSnapshotWithStimulants(
                    timestampMillis = t50,
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
                val snap20 = circadianService.evaluateSnapshotWithStimulants(
                    timestampMillis = t20,
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
                val snap80 = circadianService.evaluateSnapshotWithStimulants(
                    timestampMillis = t80,
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

                val effectiveUsableEnergy = (0.25 * snap20.usableEnergy + 0.50 * snap50.usableEnergy + 0.25 * snap80.usableEnergy).roundToInt()
                val minUsable = minOf(snap20.usableEnergy, snap50.usableEnergy, snap80.usableEnergy)
                val anyDipOrCrash = listOf(snap20.phase, snap50.phase, snap80.phase).any {
                    it == EnergyPhase.DIP || it == EnergyPhase.CRASH || it == EnergyPhase.TROUGH
                }

                val dipBoundaryBreachPenalty = if (
                    (task.energyDemand == EnergyDemand.HIGH || task.category == TaskCategory.DEEP_WORK) &&
                    (minUsable < 45 || anyDipOrCrash)
                ) {
                    25.0
                } else 0.0

                val effectiveSnap = snap50.copy(usableEnergy = effectiveUsableEnergy)
                val breakdown = evaluateTaskScoreWithWeights(
                    task = task,
                    snapshot = effectiveSnap,
                    dependentCount = dependentCount,
                    calibrationRatio = calibrationRatio,
                    weights = priorityWeights,
                    referenceNowMillis = referenceNowMillis ?: earliestStartMillis
                )
                val baseHourlyDecay = when {
                    task.energyDemand == EnergyDemand.HIGH ||
                    task.category == TaskCategory.DEEP_WORK ||
                    task.category == TaskCategory.CREATIVE -> 0.08
                    task.energyDemand == EnergyDemand.MODERATE -> 0.22
                    else -> 0.45
                }
                val hourlyDecayLambda = baseHourlyDecay * (1.0 + 0.35 * min(3, dependentCount))

                val candidateHour = Instant.ofEpochMilli(candidateStart).atZone(zoneId).hour

                // ADR-031: Historical and direct task rejection penalties
                val historicalRejectionPenalty = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.computePenalty(
                    json = rejectionPatternsJson,
                    category = task.category.name,
                    hour = candidateHour,
                    nowMillis = referenceNowMillis ?: candidateStart
                )
                val directTaskRejectionPenalty = if (
                    task.userSchedulingRating == -1 &&
                    task.schedulingRejectedSlotHour != null &&
                    task.schedulingRejectedSlotHour == candidateHour
                ) {
                    30.0
                } else 0.0
                val totalRejectionPenalty = historicalRejectionPenalty + directTaskRejectionPenalty

                // ADR-031: Proportional peak cannibalization penalty
                val peakMinutesUsed = placements
                    .filter { it.task.energyDemand != EnergyDemand.HIGH && it.task.category != TaskCategory.DEEP_WORK }
                    .sumOf { it.window.durationMinutes }
                val peakFraction = (peakMinutesUsed / 120.0).coerceIn(0.0, 1.0)
                val peakCannibalizationPenalty = if (
                    (snap50.phase == EnergyPhase.PEAK || snap50.phase == EnergyPhase.SURGE) &&
                    breakdown.urgencyScore < 75.0 &&
                    (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN)
                ) {
                    16.0 + peakFraction * 14.0
                } else {
                    0.0
                }

                // ADR-031: Cumulative high-energy debt penalty
                val priorHighEnergyMinsInBlock = placements
                    .filter { it.window.endMillis <= candidateStart && candidateStart - it.window.endMillis <= 45 * 60_000L }
                    .filter { it.task.energyDemand == EnergyDemand.HIGH || it.task.category == TaskCategory.DEEP_WORK }
                    .sumOf { it.window.durationMinutes }
                val energyDebtPenalty = if (priorHighEnergyMinsInBlock >= 90 && (task.energyDemand == EnergyDemand.HIGH || task.category == TaskCategory.DEEP_WORK)) {
                    ((priorHighEnergyMinsInBlock - 60) / 30.0) * 8.0
                } else 0.0

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

                // Feature 3: Anti-Fragmentation & Edge-Packing (Anti-Sliver)
                val effectiveStart = max(interval.startMillis, earliestStartMillis)
                val leftRemnant = candidateStart - effectiveStart
                val rightRemnant = effectiveIntervalEnd - candidateEnd
                val minUsableSliverMillis = 30 * 60_000L

                val isFlushLeft = leftRemnant == 0L
                val isFlushRight = rightRemnant == 0L
                val edgePackingBonus = when {
                    isFlushLeft && isFlushRight -> 5.0
                    isFlushLeft || isFlushRight -> 2.5
                    else -> 0.0
                }

                val leftSliverPenalty = if (leftRemnant in 1 until minUsableSliverMillis) 6.0 else 0.0
                val rightSliverPenalty = if (rightRemnant in 1 until minUsableSliverMillis) 6.0 else 0.0
                val sliverPenalty = leftSliverPenalty + rightSliverPenalty

                val internalSplitPenalty = if (
                    !isFlushLeft && !isFlushRight &&
                    (effectiveIntervalEnd - effectiveStart) >= 90 * 60_000L &&
                    task.energyDemand != EnergyDemand.HIGH
                ) {
                    3.5
                } else {
                    0.0
                }

                val antiFragmentationDelta = edgePackingBonus - sliverPenalty - internalSplitPenalty

                val utility = breakdown.totalScore - (hourlyDecayLambda * hoursFromNow) - peakCannibalizationPenalty + contextDelta + stimAlignmentDelta - deadlineBreachPenalty - dipBoundaryBreachPenalty + antiFragmentationDelta - totalRejectionPenalty - energyDebtPenalty

                if (utility > bestUtility) {
                    bestUtility = utility
                    bestWindow = TimeSlotWindow(candidateStart, candidateStart + durationMillis)
                    bestBreakdown = breakdown
                }
            }
        }
        return if (bestWindow != null && bestBreakdown != null) bestWindow to bestBreakdown else null
    }

    // ADR-061: Pairwise 2-Opt Schedule Utility Optimization
    internal fun optimizePlacementsWith2Opt(
        placements: List<ScheduledPlacement>,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        zoneId: ZoneId,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        referenceNowMillis: Long = System.currentTimeMillis(),
        calibrationRatio: Double = 1.0,
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        rejectionPatternsJson: String = "{}"
    ): List<ScheduledPlacement> {
        if (placements.size < 2) return placements
        val current = placements.sortedBy { it.window.startMillis }.toMutableList()

        fun computePlacementUtility(
            task: SchedulableTask,
            window: TimeSlotWindow,
            placedList: List<ScheduledPlacement>
        ): Double {
            val midpoint = window.startMillis + window.durationMinutes * 30_000L
            val simulatedFocus = computeSimulatedFocusLoadAt(window.startMillis, placedList)
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
                dependentCount = 0,
                calibrationRatio = calibrationRatio,
                weights = priorityWeights,
                referenceNowMillis = referenceNowMillis
            )
            val contextDelta = computeContextTransitionDelta(placedList, window.startMillis, task, window.endMillis)
            val candidateHour = Instant.ofEpochMilli(window.startMillis).atZone(zoneId).hour
            val rejPenalty = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.computePenalty(
                json = rejectionPatternsJson,
                category = task.category.name,
                hour = candidateHour,
                nowMillis = referenceNowMillis
            )
            val peakCannibalizationPenalty = if (
                (snap.phase == EnergyPhase.PEAK || snap.phase == EnergyPhase.SURGE) &&
                breakdown.urgencyScore < 75.0 &&
                (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN)
            ) {
                16.0
            } else 0.0
            return breakdown.totalScore + contextDelta - rejPenalty - peakCannibalizationPenalty
        }

        var improved = true
        var passes = 0
        while (improved && passes < 3) {
            improved = false
            passes++
            for (i in 0 until current.size - 1) {
                val p1 = current[i]
                val p2 = current[i + 1]

                // Guard 1: Never swap locked tasks
                if (p1.task.isLocked || p2.task.isLocked) continue

                // Guard 2: Never swap split continuation parts or causal dependencies
                if (p1.task.parentTaskId != null || p2.task.parentTaskId != null ||
                    p1.task.id.contains(Regex("""[#_]part""")) || p2.task.id.contains(Regex("""[#_]part""")) ||
                    p1.task.id in p2.task.prerequisiteIds || p2.task.id in p1.task.prerequisiteIds ||
                    cleanSplitTaskId(p1.task.id) == cleanSplitTaskId(p2.task.id)
                ) continue

                val gapMillis = p2.window.startMillis - p1.window.endMillis
                // Only consider adjacent blocks separated by standard transition gap (<= 20 mins)
                if (gapMillis < 0 || gapMillis > 20 * 60_000L) continue

                val d1 = p1.window.durationMinutes * 60_000L
                val d2 = p2.window.durationMinutes * 60_000L
                val startA = p1.window.startMillis
                val newEndB = startA + d2
                val newStartA = newEndB + gapMillis
                val newEndA = newStartA + d1

                // Guard 3: Total span invariance check
                if (newEndA != p2.window.endMillis) continue

                // Guard 4: Deadline feasibility for both swapped windows
                if (p2.task.deadlineMillis != null && newEndB > p2.task.deadlineMillis) continue
                if (p1.task.deadlineMillis != null && newEndA > p1.task.deadlineMillis) continue

                // Guard 5: Downstream dependent checks
                val p1HasEarlierDependent = current.drop(i + 2).any { other ->
                    p1.task.id in other.task.prerequisiteIds && other.window.startMillis < newEndA
                }
                if (p1HasEarlierDependent) continue

                val newWindow2 = TimeSlotWindow(startA, newEndB)
                val newWindow1 = TimeSlotWindow(newStartA, newEndA)

                val others = current.filterIndexed { idx, _ -> idx != i && idx != i + 1 }
                val currentUtility = computePlacementUtility(p1.task, p1.window, others) +
                    computePlacementUtility(p2.task, p2.window, others + p1)

                val swappedPlacements = others + ScheduledPlacement(p2.task, newWindow2, p2.scoreBreakdown)
                val swappedUtility = computePlacementUtility(p2.task, newWindow2, others) +
                    computePlacementUtility(p1.task, newWindow1, swappedPlacements)

                if (swappedUtility - currentUtility > 1.5) {
                    val p2Rej = computeRejectionPenalty(p2.task, newWindow2.startMillis, zoneId, rejectionPatternsJson)
                    val (p2Conf, p2Rat) = computePlacementConfidenceAndRationale(
                        task = p2.task,
                        window = newWindow2,
                        breakdown = p2.scoreBreakdown,
                        rejectionPenalty = p2Rej,
                        zoneId = zoneId,
                        wakeTimeMillis = wakeTimeMillis,
                        lastSleepDurationMinutes = lastSleepDurationMinutes
                    )
                    val p1Rej = computeRejectionPenalty(p1.task, newWindow1.startMillis, zoneId, rejectionPatternsJson)
                    val (p1Conf, p1Rat) = computePlacementConfidenceAndRationale(
                        task = p1.task,
                        window = newWindow1,
                        breakdown = p1.scoreBreakdown,
                        rejectionPenalty = p1Rej,
                        zoneId = zoneId,
                        wakeTimeMillis = wakeTimeMillis,
                        lastSleepDurationMinutes = lastSleepDurationMinutes
                    )
                    current[i] = ScheduledPlacement(p2.task, newWindow2, p2.scoreBreakdown, p2Conf, p2Rat)
                    current[i + 1] = ScheduledPlacement(p1.task, newWindow1, p1.scoreBreakdown, p1Conf, p1Rat)
                    improved = true
                }
            }
        }
        return current
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
