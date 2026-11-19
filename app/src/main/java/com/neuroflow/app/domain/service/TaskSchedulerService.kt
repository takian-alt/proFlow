package com.neuroflow.app.domain.service

import com.neuroflow.app.domain.scheduler.DeferralRationale
import com.neuroflow.app.domain.scheduler.DeferralReasonCode
import com.neuroflow.app.domain.scheduler.DependencyGraphValidator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    val schedulingRejectedSlotHour: Int? = null,
    val quadrant: com.neuroflow.app.domain.model.Quadrant? = null,
    val valueScore: Int = 50,
    val effortScore: Int = 50,
    val preferredDayStartMillis: Long? = null,
    val isWaitingFor: Boolean = false,
    val goalRiskLevel: Int = 0,
    val hasGoalAlignment: Boolean = false,
    val isPublicCommitment: Boolean = false,
    val isHabitual: Boolean = false,
    val habitStreak: Int = 0,
    val priorSessionCount: Int = 0
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
        energyDemand = energyDemand,
        quadrant = quadrant
    )
}

data class TaskScoreBreakdown(
    val taskId: String,
    val totalScore: Double,      // Guaranteed [0.0..100.0]
    val urgencyScore: Double,    // [0.0..100.0]
    val impactScore: Double,     // [0.0..100.0]
    val energyFitScore: Double,  // [0.0..100.0]
    val momentumScore: Double,   // [0.0..100.0]
    val bufferedDurationMinutes: Int,
    val fatigueAdjustment: Double = 0.0
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

    fun evaluateTaskScoreWithFatigue(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int = 0,
        calibrationRatio: Double = 1.20,
        weights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        referenceNowMillis: Long? = null,
        cumulativeCognitiveLoadMinutes: Double = 0.0
    ): TaskScoreBreakdown = evaluateTaskScoreWithWeights(
        task = task,
        snapshot = snapshot,
        dependentCount = dependentCount,
        calibrationRatio = calibrationRatio,
        weights = weights,
        referenceNowMillis = referenceNowMillis
    )

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
        rejectionPatternsJson: String = "{}",
        categoryCalibrationRatios: Map<TaskCategory, Double> = emptyMap()
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
        rejectionPatternsJson: String = "{}",
        categoryCalibrationRatios: Map<TaskCategory, Double> = emptyMap()
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
        private const val W_URGENCY = 0.34
        private const val W_IMPACT = 0.34
        private const val W_ENERGY = 0.20
        private const val W_MOMENTUM = 0.12

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
    ): TaskScoreBreakdown = evaluateTaskScoreWithFatigue(
        task = task,
        snapshot = snapshot,
        dependentCount = dependentCount,
        calibrationRatio = calibrationRatio,
        weights = weights,
        referenceNowMillis = referenceNowMillis,
        cumulativeCognitiveLoadMinutes = 0.0
    )

    override fun evaluateTaskScoreWithFatigue(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int,
        calibrationRatio: Double,
        weights: PriorityWeightProfile,
        referenceNowMillis: Long?,
        cumulativeCognitiveLoadMinutes: Double
    ): TaskScoreBreakdown {
        val bufferedMins = computeBufferedDuration(task, calibrationRatio)
        // Bug 1 Fix (ADR-025): Anchor urgency calculation to referenceNowMillis to prevent procrastination bias
        val nowAnchor = referenceNowMillis ?: snapshot.timestampMillis

        // 1. Urgency Vector U in [0..100] with Parkinson's Law ADHD Urgency Ignition
        val postponeBonus = min(12.0, task.postponeCount * 2.5)
        val urgency = if (task.deadlineMillis != null) {
            val hoursRemaining = (task.deadlineMillis - nowAnchor) / 3_600_000.0
            val slackHours = hoursRemaining - (bufferedMins / 60.0)
            val baseLogistic = 100.0 / (1.0 + exp(0.12 * (slackHours - 24.0)))
            val ignitionBoost = if (slackHours in 0.25..6.0) {
                12.0 * exp(-((slackHours - 2.5) * (slackHours - 2.5)) / 4.0)
            } else 0.0
            val overdueFloor = if (slackHours <= 0.0) 92.0 else 0.0
            max(overdueFloor, (baseLogistic + ignitionBoost + postponeBonus).coerceIn(0.0, 100.0))
        } else {
            val ageDays = max(0.0, (nowAnchor - task.createdAtMillis) / 86_400_000.0)
            val noDeadlinePostpone = min(10.0, task.postponeCount * 2.0)
            min(32.0, 10.0 + 14.0 * (1.0 - exp(-ageDays / 7.0)) + noDeadlinePostpone)
        }

        // 2. Impact Vector I in [0..100] (incorporates strategic impact, intrinsic value, ROI effort, frog, and transitive blockers)
        val rawBasePriority = when {
            task.priority >= 2 -> 38.0
            task.priority == 1 -> 22.0
            else -> 8.0
        }
        val basePriority = (rawBasePriority * weights.priorityLevelWeight.coerceIn(0.0, 3.0)).coerceIn(0.0, 100.0)
        val impactAdj = if (task.impactScore != 50) {
            (task.impactScore - 50) * 0.28
        } else 0.0
        val valueAdj = if (task.valueScore != 50) {
            (task.valueScore - 50) * 0.16
        } else 0.0
        val leverageAdj = if (task.valueScore != 50 || task.effortScore != 50) {
            ((task.valueScore - task.effortScore) * 0.08).coerceIn(-6.0, 8.0)
        } else 0.0
        val frogBonus = if (task.isFrog) (22.0 * weights.frogBonusMultiplier.coerceIn(0.0, 2.0)) else 0.0
        val dependencyBonus = 6.0 * min(3, dependentCount)
        val personalBonus = if (task.isPersonalTask) 10.0 else 0.0
        val goalRiskBonus = when {
            task.goalRiskLevel >= 2 -> 12.0
            task.goalRiskLevel == 1 -> 6.0
            task.hasGoalAlignment -> 2.5
            else -> 0.0
        }
        val commitmentBonus = if (task.isPublicCommitment) 6.0 else 0.0
        val impact = (basePriority + impactAdj + valueAdj + leverageAdj + frogBonus + dependencyBonus + personalBonus + goalRiskBonus + commitmentBonus)
            .coerceIn(0.0, 100.0)

        // 3. Energy & Category Fit Vector E in [0..100] + Dread Desensitization Routing
        val energyDiff = snapshot.usableEnergy.toDouble() - task.energyDemand.targetScore
        val demandGaussian = 100.0 * exp(-(energyDiff * energyDiff) / (2.0 * 28.0 * 28.0))
        val rawCategoryAffinity = circadianService.computeCategoryAffinity(task.category, snapshot)
        val categoryAffinity = (rawCategoryAffinity * weights.focusModeWeight.coerceIn(0.0, 3.0)).coerceIn(0.0, 100.0)
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

        // 4. Momentum Vector M in [0..100] with Zeigarnik Pull, Habit Streak, Session History & WOOP Shield
        val progressFraction = if (task.totalMicroSteps > 0) {
            (task.completedMicroSteps.toDouble() / task.totalMicroSteps).coerceIn(0.0, 1.0)
        } else 0.0
        val zeigarnikPull = if (progressFraction > 0.0 && progressFraction < 1.0) {
            32.0 * progressFraction + 10.0 * sin(PI * progressFraction)
        } else {
            32.0 * progressFraction
        }
        val activeBonus = if (task.isCurrentlyActive) 35.0 else 0.0
        val microStepBonus = if (task.totalMicroSteps > 0) 8.0 else 0.0
        val woopBonus = if (task.hasImplementationIntention) {
            10.0 * weights.planBoostWeight.coerceIn(0.0, 3.0)
        } else 0.0
        val habitStreakBonus = when {
            task.habitStreak >= 7 -> 8.0
            task.habitStreak >= 1 -> 4.5
            task.isHabitual -> 2.0
            else -> 0.0
        }
        val sessionMomentumBonus = if (task.priorSessionCount > 0 && progressFraction == 0.0) {
            min(8.0, task.priorSessionCount * 3.0)
        } else 0.0
        val lowFrictionBonus = (10.0 * exp(-max(5, task.estimatedMinutes) / 45.0)) * weights.durationWeight.coerceIn(0.0, 3.0)
        val enjoymentBonus = if (task.estimatedEnjoyment != 5) {
            (task.estimatedEnjoyment - 5) * 2.0 * weights.enjoymentWeight.coerceIn(0.0, 3.0)
        } else 0.0
        val momentum = (zeigarnikPull + activeBonus + microStepBonus + woopBonus + habitStreakBonus + sessionMomentumBonus + lowFrictionBonus + enjoymentBonus)
            .coerceIn(0.0, 100.0)

        // Eisenhower Quadrant Modulation (uses explicit quadrant when supplied; falls back cleanly for legacy fixtures)
        val quadrantSpread = weights.quadrantWeight.coerceIn(0.0, 3.0)
        val quadrantModifier = when (task.quadrant) {
            com.neuroflow.app.domain.model.Quadrant.DO_FIRST -> 1.0 + 0.16 * quadrantSpread
            com.neuroflow.app.domain.model.Quadrant.SCHEDULE -> 1.0 + 0.08 * quadrantSpread
            com.neuroflow.app.domain.model.Quadrant.DELEGATE -> 1.0 - 0.10 * quadrantSpread
            com.neuroflow.app.domain.model.Quadrant.ELIMINATE -> 1.0 - 0.22 * quadrantSpread
            null -> {
                val isImportant = task.priority >= 2 ||
                    (task.priority == 1 && task.impactScore >= 50) ||
                    task.impactScore >= 65 ||
                    task.isFrog
                val isUrgent = urgency >= 50.0
                when {
                    isImportant && isUrgent -> 1.0 + 0.16 * quadrantSpread
                    isImportant && !isUrgent -> 1.0 + 0.08 * quadrantSpread
                    !isImportant && isUrgent -> 1.0 - 0.10 * quadrantSpread
                    else -> 1.0 - 0.18 * quadrantSpread
                }
            }
        }.coerceIn(0.5, 2.0)

        val quadrantAdditiveBoost = when (task.quadrant) {
            com.neuroflow.app.domain.model.Quadrant.DO_FIRST -> 5.0 * quadrantSpread
            com.neuroflow.app.domain.model.Quadrant.SCHEDULE -> 2.0 * quadrantSpread
            com.neuroflow.app.domain.model.Quadrant.DELEGATE -> -2.5 * quadrantSpread
            com.neuroflow.app.domain.model.Quadrant.ELIMINATE -> -6.0 * quadrantSpread
            null -> 0.0
        }

        val rawWu = W_URGENCY * weights.urgencyWeight.coerceIn(0.0, 3.0)
        val rawWi = W_IMPACT * weights.impactWeight.coerceIn(0.0, 3.0)
        val rawWe = W_ENERGY * weights.circadianMatchWeight.coerceIn(0.0, 3.0)
        val rawWm = W_MOMENTUM * weights.momentumWeight.coerceIn(0.0, 3.0)
        val weightSum = (rawWu + rawWi + rawWe + rawWm).coerceAtLeast(0.001)

        val waitingPenalty = if (task.isWaitingFor && !task.isLocked) -22.0 else 0.0
        val baseCompositeScore =
            ((rawWu * urgency + rawWi * impact + rawWe * energyFit + rawWm * momentum) / weightSum) * quadrantModifier +
                quadrantAdditiveBoost +
                waitingPenalty
        val fatigueResult = FatigueHeuristic.evaluatePriorityAdjustment(
            task = task,
            bufferedDurationMinutes = bufferedMins,
            snapshot = snapshot,
            cumulativeCognitiveLoadMinutes = cumulativeCognitiveLoadMinutes,
            urgencyScore = urgency
        )
        val totalScore = (baseCompositeScore + fatigueResult.priorityDelta).coerceIn(0.0, 100.0)

        return TaskScoreBreakdown(
            taskId = task.id,
            totalScore = totalScore,
            urgencyScore = urgency,
            impactScore = impact,
            energyFitScore = energyFit,
            momentumScore = momentum,
            bufferedDurationMinutes = bufferedMins,
            fatigueAdjustment = fatigueResult.priorityDelta
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
        rejectionPatternsJson: String,
        categoryCalibrationRatios: Map<TaskCategory, Double>
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
        rejectionPatternsJson = rejectionPatternsJson,
        categoryCalibrationRatios = categoryCalibrationRatios
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
        rejectionPatternsJson: String,
        categoryCalibrationRatios: Map<TaskCategory, Double>
    ): SchedulingOutcome {
        if (tasks.isEmpty()) return SchedulingOutcome(emptyList())

        fun calibrationFor(task: SchedulableTask): Double =
            (categoryCalibrationRatios[task.category] ?: calibrationRatio).coerceIn(0.8, 1.6)

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
            val startHour = (workDayStartHour ?: 6).coerceIn(0, 23)
            if (endHour == 0) {
                dayDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
            } else if (endHour <= startHour) {
                dayDate.plusDays(1).atTime(LocalTime.of(endHour, 0)).atZone(zoneId).toInstant().toEpochMilli()
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
        val horizonStart = if (isPast) dayEndMillis else if (isToday) max(nowMillis - 15 * 60_000L, effectiveWakeStart) else effectiveWakeStart
        var freeIntervals = mutableListOf(TimeSlotWindow(horizonStart, dayEndMillis))
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

        // Feature 2 (ADR-024): Pre-Hysteresis Reverse-DAG Hard Ceiling Propagation + Transitive Criticality
        // Compute ceilings from reachable deadlines and pinned/locked tasks, preventing overdue child tasks from
        // poisoning unblocked prerequisites with past-epoch hard ceilings.
        val transitiveDependentsMap = mutableMapOf<String, Set<String>>()
        fun collectTransitiveDependents(id: String, visited: MutableSet<String>): Set<String> {
            transitiveDependentsMap[id]?.let { return it }
            val result = mutableSetOf<String>()
            for (depId in dependentsMap[id].orEmpty()) {
                if (visited.add(depId)) {
                    result.add(depId)
                    result.addAll(collectTransitiveDependents(depId, visited))
                }
            }
            transitiveDependentsMap[id] = result
            return result
        }
        for (task in sanitizedTasks) {
            collectTransitiveDependents(task.id, mutableSetOf(task.id))
        }

        val inheritedCriticalDeadlineMap = mutableMapOf<String, Long>()
        for (task in sanitizedTasks) {
            if (task.deadlineMillis != null) {
                inheritedCriticalDeadlineMap[task.id] = task.deadlineMillis
            }
        }
        var criticalPropChanged = true
        var criticalPropIter = 0
        while (criticalPropChanged && criticalPropIter < sanitizedTasks.size + 1) {
            criticalPropChanged = false
            criticalPropIter++
            for (task in sanitizedTasks) {
                val childCritDeadline = inheritedCriticalDeadlineMap[task.id] ?: continue
                val childDurMillis = computeBufferedDuration(task, calibrationFor(task)) * 60_000L
                val isUltradianDep = task.id.endsWith("#part2") || task.id.endsWith("_part2") ||
                    task.id.contains(Regex("""[#_]part[2-9]""")) ||
                    task.parentTaskId != null
                val bufferMillis = (if (isUltradianDep) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES) * 60_000L
                val parentCriticalDeadline = childCritDeadline - childDurMillis - bufferMillis
                for (prereqId in task.prerequisiteIds) {
                    val prev = inheritedCriticalDeadlineMap[prereqId] ?: Long.MAX_VALUE
                    if (parentCriticalDeadline < prev) {
                        inheritedCriticalDeadlineMap[prereqId] = parentCriticalDeadline
                        criticalPropChanged = true
                    }
                }
            }
        }

        val latestAllowedEndMap = mutableMapOf<String, Long>()
        val structuralCeilingMap = mutableMapOf<String, Long>()
        for (task in sanitizedTasks) {
            val taskBufferedMins = computeBufferedDuration(task, calibrationFor(task))
            val taskDurationMillis = taskBufferedMins * 60_000L
            val isAlreadyOverdue = task.deadlineMillis != null && task.deadlineMillis < dayStartMillis + taskDurationMillis
            val deadlineCeiling = when {
                task.deadlineMillis == null -> dayEndMillis
                isAlreadyOverdue -> dayEndMillis
                else -> min(task.deadlineMillis, dayEndMillis)
            }
            val directCeiling = when {
                task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!! + taskDurationMillis
                else -> deadlineCeiling
            }
            latestAllowedEndMap[task.id] = min(deadlineCeiling, directCeiling)

            val directStructural = when {
                task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!!
                else -> Long.MAX_VALUE
            }
            structuralCeilingMap[task.id] = directStructural
        }

        var preDagCeilingChanged = true
        var preDagIterations = 0
        while (preDagCeilingChanged && preDagIterations < sanitizedTasks.size + 1) {
            preDagCeilingChanged = false
            preDagIterations++
            for (task in sanitizedTasks) {
                val childCeiling = latestAllowedEndMap[task.id] ?: Long.MAX_VALUE
                val childStructural = structuralCeilingMap[task.id] ?: Long.MAX_VALUE

                val childBufferedMins = computeBufferedDuration(task, calibrationFor(task))
                val childDurationMillis = childBufferedMins * 60_000L
                val isLockedWithStart = task.isLocked && task.scheduledStartMillis != null
                val childLockedStart = if (isLockedWithStart) task.scheduledStartMillis!! else null

                val childLatestStart = when {
                    childLockedStart != null -> childLockedStart
                    childCeiling != Long.MAX_VALUE -> childCeiling - childDurationMillis
                    else -> Long.MAX_VALUE
                }

                val childStructuralStart = when {
                    childLockedStart != null -> childLockedStart
                    childStructural != Long.MAX_VALUE -> childStructural - childDurationMillis
                    else -> Long.MAX_VALUE
                }

                val isUltradianDep = task.id.endsWith("#part2") || task.id.endsWith("_part2") ||
                    task.id.contains(Regex("""[#_]part[2-9]""")) ||
                    task.title.contains(Regex("""\((Part|part) [2-9]/\d+\)""")) ||
                    task.parentTaskId != null ||
                    task.prerequisiteIds.any { prereq ->
                        prereq.endsWith("#part1") || prereq.endsWith("_part1") ||
                        sanitizedTaskMap[prereq]?.title?.contains("(Part 1/") == true
                    }
                val bufferBeforeChild = if (isUltradianDep) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
                val bufferMillis = bufferBeforeChild * 60_000L

                for (prereqId in task.prerequisiteIds) {
                    val prereqTask = sanitizedTaskMap[prereqId]
                    val prereqMinEnd = if (prereqTask != null) {
                        dayStartMillis + computeBufferedDuration(prereqTask, calibrationFor(prereqTask)) * 60_000L
                    } else {
                        dayStartMillis
                    }
                    if (childLatestStart != Long.MAX_VALUE) {
                        val rawParentMaxEnd = childLatestStart - bufferMillis
                        val parentMaxEnd = if (childLockedStart != null) rawParentMaxEnd else max(rawParentMaxEnd, prereqMinEnd)
                        val currentParentCeiling = latestAllowedEndMap[prereqId] ?: Long.MAX_VALUE
                        if (parentMaxEnd < currentParentCeiling) {
                            latestAllowedEndMap[prereqId] = parentMaxEnd
                            preDagCeilingChanged = true
                        }
                    }
                    if (childStructuralStart != Long.MAX_VALUE) {
                        val parentStructuralEnd = childStructuralStart - bufferMillis
                        val currentParentStructural = structuralCeilingMap[prereqId] ?: Long.MAX_VALUE
                        if (parentStructuralEnd < currentParentStructural) {
                            structuralCeilingMap[prereqId] = parentStructuralEnd
                            preDagCeilingChanged = true
                        }
                    }
                }
            }
        }

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
            val durationMins = computeBufferedDuration(task, calibrationFor(task))
            val existingWindow = if (existingStart != null) {
                TimeSlotWindow(existingStart, existingStart + durationMins * 60_000L)
            } else null

            val ceilingForTask = latestAllowedEndMap[task.id] ?: Long.MAX_VALUE
            val satisfiesCeiling = task.isLocked || (existingWindow != null && existingWindow.endMillis <= ceilingForTask)

            val isHysteresisCandidate = existingWindow != null && satisfiesCeiling &&
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
                val lockedPriorLoad = FatigueHeuristic.computeCumulativeCognitiveLoadAt(existingWindow.startMillis, placements)
                val breakdown = evaluateTaskScoreWithFatigue(
                    task = task,
                    snapshot = snap,
                    dependentCount = dependentsMap[task.id]?.size ?: 0,
                    calibrationRatio = calibrationFor(task),
                    weights = priorityWeights,
                    referenceNowMillis = nowMillis,
                    cumulativeCognitiveLoadMinutes = lockedPriorLoad
                )
                val (conf, rat) = computePlacementConfidenceAndRationale(
                    task = task,
                    window = existingWindow,
                    breakdown = breakdown,
                    rejectionPenalty = 0.0,
                    zoneId = zoneId,
                    wakeTimeMillis = effectiveWakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = 0,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    physiologicalBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
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

        // Clip any unanchored free intervals before dayStartMillis so new tasks are only scheduled in the future
        if (isToday) {
            freeIntervals = freeIntervals.mapNotNull { interval ->
                if (interval.endMillis <= dayStartMillis) null
                else TimeSlotWindow(max(interval.startMillis, dayStartMillis), interval.endMillis)
            }.toMutableList()
        }
        val freeIntervalsBeforePlacement = freeIntervals.map { it.copy() }
        val anchoredPlacementIds = placements.map { it.task.id }.toSet()

        // Feature 2 (ADR-024): Update Reverse-DAG Deadline & Locked-Dependent Ceilings with Placed Tasks
        for (task in sanitizedTasks) {
            val taskBufferedMins = computeBufferedDuration(task, calibrationFor(task))
            val taskDurationMillis = taskBufferedMins * 60_000L
            val isAlreadyOverdue = task.deadlineMillis != null && task.deadlineMillis < dayStartMillis + taskDurationMillis
            val deadlineCeiling = when {
                task.deadlineMillis == null -> dayEndMillis
                isAlreadyOverdue -> dayEndMillis
                else -> min(task.deadlineMillis, dayEndMillis)
            }
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

                val childBufferedMins = computeBufferedDuration(task, calibrationFor(task))
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

                val isUltradianDep = task.id.endsWith("#part2") || task.id.endsWith("_part2") ||
                    task.id.contains(Regex("""[#_]part[2-9]""")) ||
                    task.title.contains(Regex("""\((Part|part) [2-9]/\d+\)""")) ||
                    task.parentTaskId != null ||
                    task.prerequisiteIds.any { prereq ->
                        prereq.endsWith("#part1") || prereq.endsWith("_part1") ||
                        sanitizedTaskMap[prereq]?.title?.contains("(Part 1/") == true
                    }
                val bufferBeforeChild = if (isUltradianDep) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
                val bufferMillis = bufferBeforeChild * 60_000L

                val allPrereqs = task.prerequisiteIds
                for (prereqId in allPrereqs) {
                    val prereqTask = sanitizedTaskMap[prereqId]
                    val prereqMinEnd = if (prereqTask != null) {
                        dayStartMillis + computeBufferedDuration(prereqTask, calibrationFor(prereqTask)) * 60_000L
                    } else {
                        dayStartMillis
                    }
                    if (childLatestStart != Long.MAX_VALUE) {
                        val rawParentMaxEnd = childLatestStart - bufferMillis
                        val parentMaxEnd = if (placedWindow != null || childLockedStart != null) rawParentMaxEnd else max(rawParentMaxEnd, prereqMinEnd)
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
        fun evaluateQueueCandidate(
            task: SchedulableTask,
            fallbackSnapshot: CircadianSnapshot,
            cumulativeLoadMinutes: Double
        ): Pair<TaskScoreBreakdown, Double> {
            val transitiveDepCount = max(
                dependentsMap[task.id]?.size ?: 0,
                transitiveDependentsMap[task.id]?.size ?: 0
            )
            // Inherit downstream critical deadline & downstream urgency so blockers run ahead of non-blocking clutter
            val inheritedDeadline = inheritedCriticalDeadlineMap[task.id]
            val downstreamTasks = transitiveDependentsMap[task.id].orEmpty().mapNotNull { sanitizedTaskMap[it] }
            val maxDownstreamPriority = downstreamTasks.maxOfOrNull { it.priority } ?: task.priority
            val downstreamHasFrog = downstreamTasks.any { it.isFrog || it.quadrant == com.neuroflow.app.domain.model.Quadrant.DO_FIRST }
            val effectiveTaskForQueue = if (inheritedDeadline != null && (task.deadlineMillis == null || inheritedDeadline < task.deadlineMillis)) {
                task.copy(deadlineMillis = inheritedDeadline)
            } else {
                task
            }

            // Decouple queue ranking from arbitrary afternoon trough at freeIntervals.first():
            // evaluate at best reachable circadian point in remaining freeIntervals when intervals exist.
            val taskCalRatio = calibrationFor(task)
            val bufferedMins = computeBufferedDuration(task, taskCalRatio)
            val neededMillis = bufferedMins * 60_000L
            val reachableTimestamps = freeIntervals
                .filter { it.endMillis - max(dayStartMillis, it.startMillis) >= min(neededMillis, 30 * 60_000L) }
                .flatMap { iv ->
                    val s = max(dayStartMillis, iv.startMillis)
                    val e = iv.endMillis
                    val mid = s + (e - s) / 2L
                    listOf(s, mid)
                }
                .distinct()
                .take(8)
            val candidateSnap = if (reachableTimestamps.size > 1) {
                reachableTimestamps
                    .map { ts ->
                        circadianService.evaluateSnapshotWithStimulants(
                            timestampMillis = ts,
                            wakeTimeMillis = effectiveWakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes,
                            continuousActiveFocusMinutes = computeSimulatedFocusLoadAt(ts, placements),
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
                    }
                    .maxByOrNull { snap ->
                        evaluateTaskScoreWithFatigue(
                            task = effectiveTaskForQueue,
                            snapshot = snap,
                            dependentCount = transitiveDepCount,
                            calibrationRatio = taskCalRatio,
                            weights = priorityWeights,
                            referenceNowMillis = nowMillis,
                            cumulativeCognitiveLoadMinutes = cumulativeLoadMinutes
                        ).totalScore
                    } ?: fallbackSnapshot
            } else {
                fallbackSnapshot
            }

            val breakdown = evaluateTaskScoreWithFatigue(
                task = effectiveTaskForQueue,
                snapshot = candidateSnap,
                dependentCount = transitiveDepCount,
                calibrationRatio = taskCalRatio,
                weights = priorityWeights,
                referenceNowMillis = nowMillis,
                cumulativeCognitiveLoadMinutes = cumulativeLoadMinutes
            )
            // Compute queue-level fatigue index using cumulative load so restorative interleaving works even when a morning/rebound slot exists
            val fatigueIndex = FatigueHeuristic.computeFatigueIndex(candidateSnap, cumulativeLoadMinutes)
            val personalQueueBoost = if (task.isPersonalTask) {
                when {
                    task.priority >= 1 -> 12.0
                    task.deadlineMillis != null -> 8.0
                    else -> 3.0
                }
            } else 0.0
            val rawScarcityBonus = when (task.energyDemand) {
                EnergyDemand.HIGH -> 8.0
                EnergyDemand.MODERATE -> 3.0
                EnergyDemand.LOW -> 0.0
            }
            // Dampen peak-scarcity pull on high-demand tasks once fatigue rises so pacing stays sustainable
            val scarcityBonus = rawScarcityBonus * (1.0 - 0.85 * fatigueIndex).coerceIn(0.15, 1.0)
            // Apply direct queue cumulative fatigue tie-break when cumulative cognitive load is elevated
            val queueFatigueAdj = if (cumulativeLoadMinutes >= 90.0) {
                FatigueHeuristic.evaluatePriorityAdjustment(
                    task = task,
                    bufferedDurationMinutes = bufferedMins,
                    snapshot = candidateSnap.copy(
                        usableEnergy = min(candidateSnap.usableEnergy, fallbackSnapshot.usableEnergy + 5)
                    ),
                    cumulativeCognitiveLoadMinutes = cumulativeLoadMinutes,
                    urgencyScore = breakdown.urgencyScore
                ).priorityDelta * 0.5
            } else 0.0
            val criticalBlockerBonus = when {
                downstreamHasFrog && !task.isFrog -> 10.0
                maxDownstreamPriority > task.priority -> (maxDownstreamPriority - task.priority) * 5.0
                else -> 0.0
            }
            val effectiveDeadlineForSlack = inheritedDeadline ?: task.deadlineMillis
            val slackPullForwardBonus = if (effectiveDeadlineForSlack != null) {
                val hoursRemaining = (effectiveDeadlineForSlack - nowMillis) / 3_600_000.0
                val durHours = max(5, bufferedMins) / 60.0
                val slack = hoursRemaining - durHours
                // Smooth bounded urgency curve [0..28] avoiding discontinuous cliffs
                28.0 / (1.0 + exp(0.28 * (slack - 4.0)))
            } else 0.0
            val lockedPriority = if (task.isLocked) 25.0 else 0.0
            val preferredDayDelta = when {
                task.preferredDayStartMillis == null || task.isLocked -> 0.0
                task.preferredDayStartMillis in dayStartMillis until dayEndMillis -> 8.0
                task.preferredDayStartMillis < dayStartMillis -> 5.0
                else -> -14.0
            }
            val waitingQueuePenalty = if (task.isWaitingFor && !task.isLocked) -18.0 else 0.0
            val contextSynergyBonus = if (placements.isNotEmpty() && reachableTimestamps.isNotEmpty() && !task.isLocked) {
                val bestContext = reachableTimestamps.maxOf { ts ->
                    computeContextTransitionDelta(placements, ts, task, ts + neededMillis)
                }
                (bestContext * 0.45).coerceIn(-4.0, 4.5)
            } else 0.0
            val reachableRejectionTax = if (reachableTimestamps.isNotEmpty() && !task.isLocked) {
                val minRej = reachableTimestamps.minOf { ts ->
                    computeRejectionPenalty(task, ts, zoneId, rejectionPatternsJson, nowMillis)
                }
                (minRej * 0.40).coerceAtMost(14.0)
            } else 0.0
            return breakdown to (breakdown.totalScore + lockedPriority + personalQueueBoost + scarcityBonus + queueFatigueAdj + criticalBlockerBonus + slackPullForwardBonus + preferredDayDelta + waitingQueuePenalty + contextSynergyBonus - reachableRejectionTax)
        }

        val readyTasks = schedulableTodayTasks
            .filter { (inDegree[it.id] ?: 0) == 0 }
            .toMutableList()

        while (readyTasks.isNotEmpty()) {
            val nextFreeStart = freeIntervals.firstOrNull()?.startMillis?.let { max(dayStartMillis, it) } ?: dayStartMillis
            val simulatedFocusAtFreeStart = computeSimulatedFocusLoadAt(nextFreeStart, placements)
            val currentCumulativeLoad = max(
                FatigueHeuristic.computeCumulativeCognitiveLoadAt(nextFreeStart, placements),
                FatigueHeuristic.computeCurrentQueueCognitiveLoad(placements.filterNot { it.task.isLocked })
            )
            val queueSnapshot = if (placements.isEmpty()) {
                nowSnapshot
            } else {
                circadianService.evaluateSnapshotWithStimulants(
                    timestampMillis = nextFreeStart,
                    wakeTimeMillis = effectiveWakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = simulatedFocusAtFreeStart,
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
            }
            var bestIdx = 0
            var bestPair = evaluateQueueCandidate(readyTasks[0], queueSnapshot, currentCumulativeLoad)
            for (idx in 1 until readyTasks.size) {
                val candidate = readyTasks[idx]
                val candidatePair = evaluateQueueCandidate(candidate, queueSnapshot, currentCumulativeLoad)
                val diff = candidatePair.second - bestPair.second
                if (diff > 1e-9 || (kotlin.math.abs(diff) <= 1e-9 && candidate.id < readyTasks[bestIdx].id)) {
                    bestIdx = idx
                    bestPair = candidatePair
                }
            }
            val current = readyTasks.removeAt(bestIdx)
            val breakdown = bestPair.first
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
                    val slotPriorLoad = FatigueHeuristic.computeCumulativeCognitiveLoadAt(candidateWindow.startMillis, placements)
                    val currentCalRatio = calibrationFor(current)
                    val slotBreakdown = evaluateTaskScoreWithFatigue(
                        task = current,
                        snapshot = snap,
                        dependentCount = dependentsMap[current.id]?.size ?: 0,
                        calibrationRatio = currentCalRatio,
                        weights = priorityWeights,
                        referenceNowMillis = nowMillis,
                        cumulativeCognitiveLoadMinutes = slotPriorLoad
                    )
                    candidateWindow to slotBreakdown
                } else null
            } else null

            val currentCalRatio = calibrationFor(current)
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
                    calibrationRatio = currentCalRatio,
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
                        calibrationRatio = currentCalRatio,
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
            val isAlreadyOverdue = current.deadlineMillis != null &&
                current.deadlineMillis < earliestStart + min(durationMillis, rawDurationMillis)
            val fallbackCeiling = min(structuralCeiling, dayEndMillis)
            val fallbackStructuralSlot = if (
                primarySlot == null &&
                !current.isLocked &&
                (structuralCeiling > taskCeiling || (isAlreadyOverdue && fallbackCeiling > earliestStart))
            ) {
                findBestCircadianWindow(
                    task = current,
                    durationMillis = durationMillis,
                    earliestStartMillis = earliestStart,
                    freeIntervals = freeIntervals,
                    wakeTimeMillis = effectiveWakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    dependentCount = dependentsMap[current.id]?.size ?: 0,
                    calibrationRatio = currentCalRatio,
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
                    latestAllowedEndMillis = fallbackCeiling,
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
                        calibrationRatio = currentCalRatio,
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
                        latestAllowedEndMillis = fallbackCeiling,
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
                val rejPen = computeRejectionPenalty(current, window.startMillis, zoneId, rejectionPatternsJson, nowMillis)
                val (conf, rat) = computePlacementConfidenceAndRationale(
                    task = current,
                    window = window,
                    breakdown = finalBreakdown,
                    rejectionPenalty = rejPen,
                    zoneId = zoneId,
                    wakeTimeMillis = effectiveWakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = computeSimulatedFocusLoadAt(window.startMillis, placements),
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    physiologicalBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
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
                        if (newDeg == 0) readyTasks.add(child)
                    }
                }
            } else {
                // Shift-and-Repack Fragmentation Rescue: before splitting or deferring an unlocked task,
                // check whether compacting 1..4 flexible neighbors inside an enclosing barrier-free window
                // opens a clean contiguous block while preserving all DAG, deadline, locked, and ultradian invariants.
                val currentCeiling = latestAllowedEndMap[current.id] ?: Long.MAX_VALUE
                val structuralCeil = structuralCeilingMap[current.id] ?: Long.MAX_VALUE

                fun isImmovableForRepack(p: ScheduledPlacement): Boolean {
                    if (p.task.isLocked || p.task.id in anchoredPlacementIds) return true
                    if (p.task.parentTaskId != null || p.task.id.contains("#part") || p.task.id.contains("_part")) return true
                    return false
                }

                fun postPlacementBufferMillis(
                    placed: ScheduledPlacement,
                    priorList: List<ScheduledPlacement>
                ): Long {
                    val isHeavy = placed.task.energyDemand == EnergyDemand.HIGH ||
                        placed.task.category == TaskCategory.DEEP_WORK ||
                        placed.task.category == TaskCategory.CREATIVE
                    val contiguous = if (isHeavy) {
                        computeSimulatedFocusLoadAt(placed.window.startMillis, priorList) + placed.window.durationMinutes
                    } else 0
                    val isP1 = placed.task.id.endsWith("#part1") || placed.task.id.endsWith("_part1") || placed.task.title.contains("(Part 1/")
                    val mins = when {
                        isP1 -> ULTRADIAN_RECOVERY_BUFFER_MINUTES
                        contiguous >= 90 -> ULTRADIAN_RECOVERY_BUFFER_MINUTES
                        else -> TRANSITION_BUFFER_MINUTES
                    }
                    val nextLocked = sanitizedTasks.filter { it.isLocked && it.scheduledStartMillis != null }
                        .mapNotNull { it.scheduledStartMillis }
                        .filter { it >= placed.window.endMillis }
                        .minOrNull()
                    return if (nextLocked != null) {
                        min(mins * 60_000L, max(0L, nextLocked - placed.window.endMillis))
                    } else {
                        mins * 60_000L
                    }
                }

                fun rebuildFreeIntervals(): MutableList<TimeSlotWindow> {
                    var rebuilt = freeIntervalsBeforePlacement.map { it.copy() }.toMutableList()
                    val nonAnchored = placements.filterNot { it.task.id in anchoredPlacementIds }.sortedBy { it.window.startMillis }
                    val accumulated = placements.filter { it.task.id in anchoredPlacementIds }.toMutableList()
                    for (p in nonAnchored) {
                        val buf = postPlacementBufferMillis(p, accumulated)
                        rebuilt = subtractInterval(rebuilt, TimeSlotWindow(p.window.startMillis, p.window.endMillis + buf))
                        accumulated.add(p)
                    }
                    return rebuilt
                }

                fun attemptFragmentationRepackRescue(targetDurationMillis: Long, ceilingToUse: Long, allowBreach: Boolean): Boolean {
                    if (current.isLocked || freeIntervalsBeforePlacement.isEmpty()) return false
                    val effectiveCeiling = min(ceilingToUse, dayEndMillis)
                    if (effectiveCeiling < earliestStart + targetDurationMillis) return false

                    val totalRemainingFreeMillis = freeIntervals.sumOf { iv ->
                        val s = max(iv.startMillis, earliestStart)
                        val e = min(iv.endMillis, effectiveCeiling)
                        max(0L, e - s)
                    }
                    if (totalRemainingFreeMillis < targetDurationMillis) return false

                    data class RepackProposal(
                        val movedUpdates: List<Pair<ScheduledPlacement, TimeSlotWindow>>,
                        val rescuedWindow: TimeSlotWindow,
                        val rescuedBreakdown: TaskScoreBreakdown,
                        val netUtility: Double
                    )

                    var bestProposal: RepackProposal? = null
                    val minGapMillis = TRANSITION_BUFFER_MINUTES * 60_000L

                    for (hostInterval in freeIntervalsBeforePlacement) {
                        if (hostInterval.endMillis <= earliestStart || hostInterval.startMillis >= effectiveCeiling) continue
                        val insideHost = placements
                            .filter { it.window.startMillis >= hostInterval.startMillis && it.window.endMillis <= hostInterval.endMillis }
                            .sortedBy { it.window.startMillis }
                        if (insideHost.isEmpty()) continue

                        // Partition hostInterval by immovable placements into barrier-free sub-windows
                        val immovables = insideHost.filter { isImmovableForRepack(it) }
                        var subWindows = mutableListOf(hostInterval)
                        for (im in immovables) {
                            val buf = postPlacementBufferMillis(im, placements.filter { it.window.endMillis <= im.window.startMillis })
                            subWindows = subtractInterval(
                                subWindows,
                                TimeSlotWindow(max(hostInterval.startMillis, im.window.startMillis - minGapMillis), im.window.endMillis + buf)
                            )
                        }

                        for (sub in subWindows) {
                            val movables = insideHost
                                .filter { !isImmovableForRepack(it) && it.window.startMillis >= sub.startMillis && it.window.endMillis <= sub.endMillis }
                                .sortedBy { it.window.startMillis }
                            if (movables.isEmpty() || movables.size > 4) continue
                            val totalMovableDuration = movables.sumOf { it.window.endMillis - it.window.startMillis }
                            if (totalMovableDuration + targetDurationMillis + movables.size * minGapMillis > sub.endMillis - sub.startMillis) {
                                continue
                            }

                            val movableIds = movables.map { it.task.id }.toSet()
                            val externalPlacements = placements.filterNot { it.task.id in movableIds }

                            fun requiredGapBetween(prev: SchedulableTask, next: SchedulableTask, priorList: List<ScheduledPlacement>): Long {
                                val prevHeavy = prev.energyDemand == EnergyDemand.HIGH || prev.category == TaskCategory.DEEP_WORK || prev.category == TaskCategory.CREATIVE
                                val nextHeavy = next.energyDemand == EnergyDemand.HIGH || next.category == TaskCategory.DEEP_WORK || next.category == TaskCategory.CREATIVE
                                if (prevHeavy && nextHeavy) {
                                    val load = computeSimulatedFocusLoadAt(Long.MAX_VALUE, priorList)
                                    if (load >= 90) return ULTRADIAN_RECOVERY_BUFFER_MINUTES * 60_000L
                                }
                                return minGapMillis
                            }

                            fun validateAndScoreCandidate(
                                shiftedList: List<Pair<ScheduledPlacement, TimeSlotWindow>>,
                                freedPocket: TimeSlotWindow
                            ) {
                                val pocketStart = max(freedPocket.startMillis, earliestStart)
                                val pocketEnd = min(freedPocket.endMillis, effectiveCeiling)
                                if (pocketEnd - pocketStart < targetDurationMillis) return

                                val shiftedById = shiftedList.associate { it.first.task.id to it.second }
                                // Validate moved tasks against DAG, ceilings, deadlines, and external placements
                                for ((orig, newWin) in shiftedList) {
                                    if (newWin.startMillis < sub.startMillis || newWin.endMillis > sub.endMillis) return
                                    if (newWin.startMillis < dayStartMillis || newWin.endMillis > dayEndMillis) return
                                    val ceiling = latestAllowedEndMap[orig.task.id] ?: Long.MAX_VALUE
                                    if (newWin.endMillis > ceiling) return
                                    if (orig.task.deadlineMillis != null && newWin.endMillis > orig.task.deadlineMillis) return
                                    for (prereqId in orig.task.prerequisiteIds) {
                                        val prereqEnd = shiftedById[prereqId]?.endMillis ?: completionEndMap[prereqId]
                                        if (prereqEnd != null && prereqEnd + minGapMillis > newWin.startMillis) return
                                    }
                                    for (depId in dependentsMap[orig.task.id].orEmpty()) {
                                        val depStart = shiftedById[depId]?.startMillis
                                            ?: externalPlacements.firstOrNull { it.task.id == depId }?.window?.startMillis
                                        if (depStart != null && newWin.endMillis + minGapMillis > depStart) return
                                    }
                                    if (externalPlacements.any { max(it.window.startMillis, newWin.startMillis) < min(it.window.endMillis, newWin.endMillis) }) {
                                        return
                                    }
                                }

                                val provisionalMoved = shiftedList.map { (orig, newWin) -> orig.copy(window = newWin) }
                                val simulatedPlacements = (externalPlacements + provisionalMoved).sortedBy { it.window.startMillis }

                                val rescuedSlot = findBestCircadianWindow(
                                    task = current,
                                    durationMillis = targetDurationMillis,
                                    earliestStartMillis = pocketStart,
                                    freeIntervals = listOf(TimeSlotWindow(pocketStart, pocketEnd)),
                                    wakeTimeMillis = effectiveWakeTimeMillis,
                                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                                    dependentCount = dependentsMap[current.id]?.size ?: 0,
                                    calibrationRatio = currentCalRatio,
                                    zoneId = zoneId,
                                    naps = naps,
                                    chronotype = chronotype,
                                    manualPeakStartHour = manualPeakStartHour,
                                    priorityWeights = priorityWeights,
                                    placements = simulatedPlacements,
                                    learnedProfile = learnedProfile,
                                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                                    physiologicalBlocks = physiologicalBlocks,
                                    stimulantDoses = stimulantDoses,
                                    socialJetlagLagHours = socialJetlagLagHours,
                                    latestAllowedEndMillis = pocketEnd,
                                    referenceNowMillis = nowMillis,
                                    allowDeadlineBreach = allowBreach,
                                    rejectionPatternsJson = rejectionPatternsJson
                                ) ?: return

                                val (rescuedWin, rescuedBd) = rescuedSlot
                                // Ensure rescuedWin doesn't collide with any shifted or external placement (including transition buffer)
                                if (simulatedPlacements.any { p ->
                                        max(p.window.startMillis - minGapMillis, rescuedWin.startMillis) <
                                            min(p.window.endMillis + minGapMillis, rescuedWin.endMillis)
                                    }) {
                                    return
                                }

                                // Score total utility across moved tasks + rescued task with tiny disruption tie-breaker
                                val movedCount = shiftedList.count { (orig, newWin) -> orig.window.startMillis != newWin.startMillis }
                                var totalUtil = rescuedBd.totalScore - movedCount * 0.5
                                for ((orig, newWin) in shiftedList) {
                                    val prior = (simulatedPlacements + ScheduledPlacement(current, rescuedWin, rescuedBd))
                                        .filter { it.task.id != orig.task.id && it.window.endMillis <= newWin.startMillis }
                                    val evalMoved = evaluateCandidateSlotUtility(
                                        task = orig.task,
                                        candidateStart = newWin.startMillis,
                                        candidateEnd = newWin.endMillis,
                                        wakeTimeMillis = effectiveWakeTimeMillis,
                                        lastSleepDurationMinutes = lastSleepDurationMinutes,
                                        dependentCount = dependentsMap[orig.task.id]?.size ?: 0,
                                        calibrationRatio = calibrationFor(orig.task),
                                        zoneId = zoneId,
                                        naps = naps,
                                        chronotype = chronotype,
                                        manualPeakStartHour = manualPeakStartHour,
                                        priorityWeights = priorityWeights,
                                        placements = prior,
                                        learnedProfile = learnedProfile,
                                        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                                        physiologicalBlocks = physiologicalBlocks,
                                        stimulantDoses = stimulantDoses,
                                        socialJetlagLagHours = socialJetlagLagHours,
                                        referenceNowMillis = nowMillis,
                                        earliestStartMillis = dayStartMillis,
                                        rejectionPatternsJson = rejectionPatternsJson
                                    )
                                    totalUtil += evalMoved.utility
                                }

                                if (bestProposal == null || totalUtil > bestProposal!!.netUtility) {
                                    bestProposal = RepackProposal(
                                        movedUpdates = shiftedList,
                                        rescuedWindow = rescuedWin,
                                        rescuedBreakdown = rescuedBd,
                                        netUtility = totalUtil
                                    )
                                }
                            }

                            // Orientation 1: Pack-Left (compact movables flush-left from sub.startMillis, open right pocket for current)
                            run {
                                val leftPacked = mutableListOf<Pair<ScheduledPlacement, TimeSlotWindow>>()
                                var cursor = sub.startMillis
                                val runningPrior = externalPlacements.filter { it.window.endMillis <= sub.startMillis }.toMutableList()
                                var feasible = true
                                for (m in movables) {
                                    val dur = m.window.endMillis - m.window.startMillis
                                    val prereqFloor = m.task.prerequisiteIds.mapNotNull { pid ->
                                        leftPacked.firstOrNull { it.first.task.id == pid }?.second?.endMillis ?: completionEndMap[pid]
                                    }.maxOrNull()?.let { it + minGapMillis } ?: sub.startMillis
                                    val start = max(cursor, prereqFloor)
                                    val end = start + dur
                                    if (end > sub.endMillis) {
                                        feasible = false
                                        break
                                    }
                                    val win = TimeSlotWindow(start, end)
                                    leftPacked.add(m to win)
                                    runningPrior.add(m.copy(window = win))
                                    val gap = requiredGapBetween(m.task, current, runningPrior)
                                    cursor = end + gap
                                }
                                if (feasible && cursor + targetDurationMillis <= sub.endMillis) {
                                    validateAndScoreCandidate(leftPacked, TimeSlotWindow(cursor, sub.endMillis))
                                }
                            }

                            // Orientation 2: Pack-Right (compact movables flush-right ending at sub.endMillis, open left pocket for current)
                            run {
                                val rightPackedRev = mutableListOf<Pair<ScheduledPlacement, TimeSlotWindow>>()
                                var cursorEnd = sub.endMillis
                                var feasible = true
                                for (m in movables.asReversed()) {
                                    val dur = m.window.endMillis - m.window.startMillis
                                    val ceiling = min(
                                        latestAllowedEndMap[m.task.id] ?: sub.endMillis,
                                        m.task.deadlineMillis ?: sub.endMillis
                                    )
                                    val end = min(cursorEnd, ceiling)
                                    val start = end - dur
                                    if (start < sub.startMillis) {
                                        feasible = false
                                        break
                                    }
                                    val win = TimeSlotWindow(start, end)
                                    rightPackedRev.add(m to win)
                                    val isHeavy = m.task.energyDemand == EnergyDemand.HIGH || m.task.category == TaskCategory.DEEP_WORK || m.task.category == TaskCategory.CREATIVE
                                    val gap = if (isHeavy && (dur / 60_000L) >= 90L) ULTRADIAN_RECOVERY_BUFFER_MINUTES * 60_000L else minGapMillis
                                    cursorEnd = start - gap
                                }
                                val rightPacked = rightPackedRev.asReversed()
                                val pocketEnd = min(cursorEnd, effectiveCeiling)
                                if (feasible && pocketEnd - max(sub.startMillis, earliestStart) >= targetDurationMillis) {
                                    validateAndScoreCandidate(rightPacked, TimeSlotWindow(sub.startMillis, pocketEnd))
                                }
                            }
                        }
                    }

                    val chosen = bestProposal ?: return false
                    // Commit shifted placements with freshly evaluated breakdowns & rationales
                    val movedIds = chosen.movedUpdates.map { it.first.task.id }.toSet()
                    placements.removeAll { it.task.id in movedIds }

                    val updatedMovedPlacements = chosen.movedUpdates.map { (orig, newWin) ->
                        val prior = placements.filter { it.window.endMillis <= newWin.startMillis }
                        val eval = evaluateCandidateSlotUtility(
                            task = orig.task,
                            candidateStart = newWin.startMillis,
                            candidateEnd = newWin.endMillis,
                            wakeTimeMillis = effectiveWakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes,
                            dependentCount = dependentsMap[orig.task.id]?.size ?: 0,
                            calibrationRatio = calibrationFor(orig.task),
                            zoneId = zoneId,
                            naps = naps,
                            chronotype = chronotype,
                            manualPeakStartHour = manualPeakStartHour,
                            priorityWeights = priorityWeights,
                            placements = prior,
                            learnedProfile = learnedProfile,
                            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                            physiologicalBlocks = physiologicalBlocks,
                            stimulantDoses = stimulantDoses,
                            socialJetlagLagHours = socialJetlagLagHours,
                            referenceNowMillis = nowMillis,
                            earliestStartMillis = dayStartMillis,
                            rejectionPatternsJson = rejectionPatternsJson
                        )
                        val (conf, rat) = computePlacementConfidenceAndRationale(
                            task = orig.task,
                            window = newWin,
                            breakdown = eval.breakdown,
                            rejectionPenalty = eval.rejectionPenalty,
                            zoneId = zoneId,
                            wakeTimeMillis = effectiveWakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes,
                            continuousActiveFocusMinutes = computeSimulatedFocusLoadAt(newWin.startMillis, prior),
                            naps = naps,
                            chronotype = chronotype,
                            manualPeakStartHour = manualPeakStartHour,
                            learnedProfile = learnedProfile,
                            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                            physiologicalBlocks = physiologicalBlocks,
                            stimulantDoses = stimulantDoses,
                            socialJetlagLagHours = socialJetlagLagHours
                        )
                        ScheduledPlacement(orig.task, newWin, eval.breakdown, conf, rat)
                    }
                    placements.addAll(updatedMovedPlacements)
                    updatedMovedPlacements.forEach { completionEndMap[it.task.id] = it.window.endMillis }

                    val rescuedRej = computeRejectionPenalty(current, chosen.rescuedWindow.startMillis, zoneId, rejectionPatternsJson, nowMillis)
                    val (rescuedConf, rescuedRat) = computePlacementConfidenceAndRationale(
                        task = current,
                        window = chosen.rescuedWindow,
                        breakdown = chosen.rescuedBreakdown,
                        rejectionPenalty = rescuedRej,
                        zoneId = zoneId,
                        wakeTimeMillis = effectiveWakeTimeMillis,
                        lastSleepDurationMinutes = lastSleepDurationMinutes,
                        continuousActiveFocusMinutes = computeSimulatedFocusLoadAt(chosen.rescuedWindow.startMillis, placements),
                        naps = naps,
                        chronotype = chronotype,
                        manualPeakStartHour = manualPeakStartHour,
                        learnedProfile = learnedProfile,
                        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                        physiologicalBlocks = physiologicalBlocks,
                        stimulantDoses = stimulantDoses,
                        socialJetlagLagHours = socialJetlagLagHours
                    )
                    val finalRescuedBd = if (chosen.rescuedWindow.durationMinutes < chosen.rescuedBreakdown.bufferedDurationMinutes) {
                        chosen.rescuedBreakdown.copy(bufferedDurationMinutes = chosen.rescuedWindow.durationMinutes)
                    } else {
                        chosen.rescuedBreakdown
                    }
                    placements.add(ScheduledPlacement(current, chosen.rescuedWindow, finalRescuedBd, rescuedConf, rescuedRat))
                    completionEndMap[current.id] = chosen.rescuedWindow.endMillis
                    freeIntervals = rebuildFreeIntervals()

                    children[current.id]?.forEach { child ->
                        if (child.id != current.id) {
                            val newDeg = (inDegree[child.id] ?: 1) - 1
                            inDegree[child.id] = newDeg
                            if (newDeg == 0) readyTasks.add(child)
                        }
                    }
                    return true
                }

                val isOverdueForRepack = current.deadlineMillis != null &&
                    current.deadlineMillis < earliestStart + min(durationMillis, rawDurationMillis)
                val rescuedByRepack = attemptFragmentationRepackRescue(durationMillis, currentCeiling, allowBreach = isOverdueForRepack) ||
                    (!current.isDurationExplicit && rawDurationMillis < durationMillis &&
                        attemptFragmentationRepackRescue(rawDurationMillis, currentCeiling, allowBreach = isOverdueForRepack)) ||
                    (structuralCeil > currentCeiling && attemptFragmentationRepackRescue(durationMillis, structuralCeil, allowBreach = true))

                if (rescuedByRepack) {
                    continue
                }

                // Bug 6 Fix & Feature 4 (ADR-024): Atomic Two-Phase Ultradian Split Verifier (+ asymmetric split fallback)
                val dayIsWeekend = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.isWeekendDay(dayStartMillis, zoneId)
                val longerBlockPref = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.computeLongerBlockPreference(
                    json = rejectionPatternsJson,
                    category = current.category.name,
                    nowMillis = nowMillis,
                    targetIsWeekend = dayIsWeekend
                )
                val isUrgentDeadlineSoon = current.deadlineMillis != null && (current.deadlineMillis - nowMillis) <= 6 * 3_600_000L
                val minFragToleranceForSplit = if (isUrgentDeadlineSoon) 0.15f else (0.15f + (0.55 * longerBlockPref).toFloat()).coerceAtMost(0.75f)
                val allowDynamicSplit = current.fragmentationTolerance >= minFragToleranceForSplit

                var splitSuccess = false
                val splitCandidates = if (allowDynamicSplit) buildList {
                    splitIntoUltradianParts(current, breakdown.bufferedDurationMinutes)?.let { add(it) }
                    if (breakdown.bufferedDurationMinutes >= 60 &&
                        !current.isLocked &&
                        current.fragmentationTolerance >= minFragToleranceForSplit &&
                        !current.id.contains("#part") &&
                        !current.id.contains("_part") &&
                        !current.title.contains("(Part ") &&
                        current.parentTaskId == null
                    ) {
                        val total = breakdown.bufferedDurationMinutes
                        for (ratio in listOf(0.40, 0.60, 0.35, 0.65)) {
                            val p1Mins = (total * ratio).roundToInt().coerceAtLeast(25)
                            val p2Mins = (total - p1Mins).coerceAtLeast(25)
                            if (p1Mins != (total / 2).coerceAtLeast(25) && p1Mins + p2Mins <= total + 5) {
                                val p1Id = "${current.id}#part1"
                                val p2Id = "${current.id}#part2"
                                add(
                                    current.copy(
                                        id = p1Id,
                                        title = "${current.title} (Part 1/2)",
                                        estimatedMinutes = p1Mins,
                                        isDurationExplicit = true
                                    ) to current.copy(
                                        id = p2Id,
                                        title = "${current.title} (Part 2/2)",
                                        estimatedMinutes = p2Mins,
                                        isDurationExplicit = true,
                                        parentTaskId = current.id,
                                        prerequisiteIds = current.prerequisiteIds + p1Id
                                    )
                                )
                            }
                        }
                    }
                } else emptyList()

                for ((part1, part2) in splitCandidates) {
                    val p1DurationMillis = max(5, part1.estimatedMinutes) * 60_000L
                    val p2DurationMillis = max(5, part2.estimatedMinutes) * 60_000L

                    fun attemptSplitPlacement(ceilingToUse: Long, allowBreach: Boolean = false): Boolean {
                        val p1BufferMins = ULTRADIAN_RECOVERY_BUFFER_MINUTES
                        val p1BufferMillis = p1BufferMins * 60_000L
                        val effectiveCeiling = min(ceilingToUse, dayEndMillis)
                        val p1Ceiling = if (effectiveCeiling != Long.MAX_VALUE) {
                            effectiveCeiling - (p1BufferMillis + p2DurationMillis)
                        } else {
                            Long.MAX_VALUE
                        }
                        if (p1Ceiling < earliestStart + p1DurationMillis) return false

                        fun evaluateP2ForP1(p1Slot: Pair<TimeSlotWindow, TaskScoreBreakdown>): Pair<Pair<TimeSlotWindow, TaskScoreBreakdown>, Pair<TimeSlotWindow, TaskScoreBreakdown>>? {
                            val (p1Window, _) = p1Slot
                            val simulatedIntervalsAfterP1 = subtractInterval(
                                freeIntervals,
                                TimeSlotWindow(p1Window.startMillis, p1Window.endMillis + p1BufferMillis)
                            )
                            val earliestP2Start = p1Window.endMillis + p1BufferMillis
                            val simulatedPlacementsWithP1 = placements + ScheduledPlacement(part1, p1Window, p1Slot.second)

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
                            ) ?: return null

                            return p1Slot to p2Slot
                        }

                        // Fast path: try globally highest-utility p1 window first
                        val greedyP1Slot = findBestCircadianWindow(
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
                        )

                        val matchedPair = greedyP1Slot?.let { evaluateP2ForP1(it) } ?: run {
                            // If greedy p1 cannibalized part2's only compatible pocket (e.g., an irregular larger gap),
                            // evaluate each free interval as a candidate host for part1 (either leaving room in later intervals
                            // or finishing early enough inside the same large interval for recovery + part2).
                            var fallbackPair: Pair<Pair<TimeSlotWindow, TaskScoreBreakdown>, Pair<TimeSlotWindow, TaskScoreBreakdown>>? = null
                            for (interval in freeIntervals) {
                                if (interval.endMillis - max(interval.startMillis, earliestStart) < p1DurationMillis) continue
                                val singleIntervalCandidates = listOfNotNull(
                                    findBestCircadianWindow(
                                        task = part1,
                                        durationMillis = p1DurationMillis,
                                        earliestStartMillis = earliestStart,
                                        freeIntervals = listOf(interval),
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
                                    ),
                                    run {
                                        val sameIntervalP1Cap = min(p1Ceiling, interval.endMillis - (p1BufferMillis + p2DurationMillis))
                                        if (sameIntervalP1Cap >= max(interval.startMillis, earliestStart) + p1DurationMillis) {
                                            findBestCircadianWindow(
                                                task = part1,
                                                durationMillis = p1DurationMillis,
                                                earliestStartMillis = earliestStart,
                                                freeIntervals = listOf(interval),
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
                                                latestAllowedEndMillis = sameIntervalP1Cap,
                                                referenceNowMillis = nowMillis,
                                                allowDeadlineBreach = allowBreach,
                                                rejectionPatternsJson = rejectionPatternsJson
                                            )
                                        } else null
                                    }
                                )
                                for (candP1 in singleIntervalCandidates) {
                                    val pair = evaluateP2ForP1(candP1)
                                    if (pair != null) {
                                        val existingScore = fallbackPair?.let { it.first.second.totalScore + it.second.second.totalScore }
                                            ?: Double.NEGATIVE_INFINITY
                                        val candScore = pair.first.second.totalScore + pair.second.second.totalScore
                                        if (candScore > existingScore) {
                                            fallbackPair = pair
                                        }
                                    }
                                }
                            }
                            fallbackPair
                        } ?: return false

                        val (p1Slot, p2Slot) = matchedPair
                        val (p1Window, p1Breakdown) = p1Slot
                        val (p2Window, p2Breakdown) = p2Slot
                        val simulatedIntervalsAfterP1 = subtractInterval(
                            freeIntervals,
                            TimeSlotWindow(p1Window.startMillis, p1Window.endMillis + p1BufferMillis)
                        )
                        val simulatedPlacementsWithP1 = placements + ScheduledPlacement(part1, p1Window, p1Breakdown)

                        val p1Rej = computeRejectionPenalty(part1, p1Window.startMillis, zoneId, rejectionPatternsJson, nowMillis)
                        val (p1Conf, p1Rat) = computePlacementConfidenceAndRationale(
                            task = part1,
                            window = p1Window,
                            breakdown = p1Breakdown,
                            rejectionPenalty = p1Rej,
                            zoneId = zoneId,
                            wakeTimeMillis = effectiveWakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes,
                            continuousActiveFocusMinutes = computeSimulatedFocusLoadAt(p1Window.startMillis, placements),
                            naps = naps,
                            chronotype = chronotype,
                            manualPeakStartHour = manualPeakStartHour,
                            learnedProfile = learnedProfile,
                            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                            physiologicalBlocks = physiologicalBlocks,
                            stimulantDoses = stimulantDoses,
                            socialJetlagLagHours = socialJetlagLagHours
                        )
                        val p2Rej = computeRejectionPenalty(part2, p2Window.startMillis, zoneId, rejectionPatternsJson, nowMillis)
                        val (p2Conf, p2Rat) = computePlacementConfidenceAndRationale(
                            task = part2,
                            window = p2Window,
                            breakdown = p2Breakdown,
                            rejectionPenalty = p2Rej,
                            zoneId = zoneId,
                            wakeTimeMillis = effectiveWakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes,
                            continuousActiveFocusMinutes = computeSimulatedFocusLoadAt(p2Window.startMillis, simulatedPlacementsWithP1),
                            naps = naps,
                            chronotype = chronotype,
                            manualPeakStartHour = manualPeakStartHour,
                            learnedProfile = learnedProfile,
                            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                            physiologicalBlocks = physiologicalBlocks,
                            stimulantDoses = stimulantDoses,
                            socialJetlagLagHours = socialJetlagLagHours
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
                            if (newDeg == 0) readyTasks.add(child)
                        }
                        return true
                    }

                    val isOverdueForSplit = current.deadlineMillis != null &&
                        current.deadlineMillis < earliestStart + p1DurationMillis + ULTRADIAN_RECOVERY_BUFFER_MINUTES * 60_000L + p2DurationMillis
                    splitSuccess = attemptSplitPlacement(currentCeiling, allowBreach = isOverdueForSplit) ||
                        (structuralCeil > currentCeiling && attemptSplitPlacement(structuralCeil, allowBreach = true))
                    if (splitSuccess) break
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
            rejectionPatternsJson = rejectionPatternsJson,
            dependentsMap = dependentsMap,
            freeIntervalsBeforePlacement = freeIntervalsBeforePlacement,
            latestAllowedEndMap = latestAllowedEndMap,
            dayStartMillis = dayStartMillis,
            anchoredPlacementIds = anchoredPlacementIds,
            categoryCalibrationRatios = categoryCalibrationRatios
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
        rejectionPatternsJson: String,
        referenceNowMillis: Long = startMillis
    ): Double {
        val hour = Instant.ofEpochMilli(startMillis).atZone(zoneId).hour
        val targetIsWeekend = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.isWeekendDay(startMillis, zoneId)
        val historical = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.computePenalty(
            json = rejectionPatternsJson,
            category = task.category.name,
            hour = hour,
            nowMillis = referenceNowMillis,
            targetIsWeekend = targetIsWeekend
        )
        val direct = if (task.userSchedulingRating == -1 && task.schedulingRejectedSlotHour == hour) 30.0 else 0.0
        return historical + direct
    }

    private fun computeFeedbackUtilityDelta(
        task: SchedulableTask,
        startMillis: Long,
        zoneId: ZoneId,
        rejectionPatternsJson: String,
        referenceNowMillis: Long = startMillis
    ): Double {
        val hour = Instant.ofEpochMilli(startMillis).atZone(zoneId).hour
        val targetIsWeekend = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.isWeekendDay(startMillis, zoneId)
        val netAffinity = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.computeAffinityDelta(
            json = rejectionPatternsJson,
            category = task.category.name,
            hour = hour,
            nowMillis = referenceNowMillis,
            targetIsWeekend = targetIsWeekend
        )
        val direct = if (task.userSchedulingRating == -1 && task.schedulingRejectedSlotHour == hour) 30.0 else 0.0
        return (netAffinity - direct).coerceIn(-35.0, 12.0)
    }

    private fun computePlacementConfidenceAndRationale(
        task: SchedulableTask,
        window: TimeSlotWindow,
        breakdown: TaskScoreBreakdown,
        rejectionPenalty: Double,
        zoneId: ZoneId,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int = 0,
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0
    ): Pair<Float, String> {
        val energyFitNormalized = (breakdown.energyFitScore / 100.0).coerceIn(0.0, 1.0)
        val urgencyNormalized = (breakdown.urgencyScore / 100.0).coerceIn(0.0, 1.0)
        val impactNormalized = (breakdown.impactScore / 100.0).coerceIn(0.0, 1.0)
        val penaltyDeduction = (rejectionPenalty / 40.0).coerceIn(0.0, 0.4)
        val fatiguePenaltyDeduction = if (!task.isLocked && breakdown.fatigueAdjustment < -12.0) {
            ((-breakdown.fatigueAdjustment - 12.0) / 80.0).coerceIn(0.0, 0.12)
        } else 0.0

        val rawConfidence = ((energyFitNormalized * 0.45 + urgencyNormalized * 0.25 + impactNormalized * 0.30) - penaltyDeduction - fatiguePenaltyDeduction).toFloat()
        val confidence = rawConfidence.coerceIn(0.15f, 1.0f)

        val snap = circadianService.evaluateSnapshotWithStimulants(
            timestampMillis = window.startMillis + (window.durationMinutes * 30_000L),
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            continuousActiveFocusMinutes = continuousActiveFocusMinutes,
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
        val phaseName = snap.phase.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        val fitPct = breakdown.energyFitScore.roundToInt()
        val rationale = when {
            task.isLocked -> "Pinned to this time window by user."
            rejectionPenalty > 15.0 -> "Lower confidence: scheduled here despite historical rejection due to tight schedule constraints."
            breakdown.urgencyScore >= 80.0 -> "Prioritized here due to impending deadline ($fitPct% energy match)."
            breakdown.fatigueAdjustment >= 4.0 -> "Recommended in $phaseName window for restorative pacing ($fitPct% energy match)."
            snap.phase == EnergyPhase.PEAK || snap.phase == EnergyPhase.SURGE || snap.phase == EnergyPhase.MORNING_RAMP -> "Aligned to your $phaseName window ($fitPct% energy match)."
            else -> "Matched to $phaseName energy phase ($fitPct% match)."
        }
        return confidence to rationale
    }

    private data class EvaluatedSlot(
        val utility: Double,
        val breakdown: TaskScoreBreakdown,
        val rejectionPenalty: Double
    )

    private fun evaluateCandidateSlotUtility(
        task: SchedulableTask,
        candidateStart: Long,
        candidateEnd: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        dependentCount: Int,
        calibrationRatio: Double,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        priorityWeights: PriorityWeightProfile,
        placements: List<ScheduledPlacement>,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        physiologicalBlocks: List<CognitiveBlockWindow>,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double,
        referenceNowMillis: Long,
        earliestStartMillis: Long,
        rejectionPatternsJson: String,
        intervalStartMillis: Long? = null,
        intervalEndMillis: Long? = null
    ): EvaluatedSlot {
        val durationMillis = (candidateEnd - candidateStart).coerceAtLeast(60_000L)
        val simulatedFocus = computeSimulatedFocusLoadAt(candidateStart, placements)

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
        val cumulativeCognitiveLoad = FatigueHeuristic.computeCumulativeCognitiveLoadAt(candidateStart, placements)
        val breakdown = evaluateTaskScoreWithFatigue(
            task = task,
            snapshot = effectiveSnap,
            dependentCount = dependentCount,
            calibrationRatio = calibrationRatio,
            weights = priorityWeights,
            referenceNowMillis = referenceNowMillis,
            cumulativeCognitiveLoadMinutes = cumulativeCognitiveLoad
        )
        val baseHourlyDecay = when {
            task.energyDemand == EnergyDemand.HIGH ||
            task.category == TaskCategory.DEEP_WORK ||
            task.category == TaskCategory.CREATIVE -> 0.14
            task.energyDemand == EnergyDemand.MODERATE -> 0.12
            else -> 0.08
        }
        val hourlyDecayLambda = baseHourlyDecay * (1.0 + 0.35 * min(3, dependentCount))

        val totalRejectionPenalty = computeRejectionPenalty(task, candidateStart, zoneId, rejectionPatternsJson, referenceNowMillis)
        val feedbackUtilityDelta = computeFeedbackUtilityDelta(task, candidateStart, zoneId, rejectionPatternsJson, referenceNowMillis)
        val targetIsWeekend = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.isWeekendDay(candidateStart, zoneId)
        val longerBlockPref = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.computeLongerBlockPreference(
            json = rejectionPatternsJson,
            category = task.category.name,
            nowMillis = referenceNowMillis,
            targetIsWeekend = targetIsWeekend
        )

        // Strengthened Peak Protection: protect high-alertness PEAK/SURGE windows from low-urgency admin & moderate chores
        val peakMinutesUsed = placements
            .filter { it.task.energyDemand != EnergyDemand.HIGH && it.task.category != TaskCategory.DEEP_WORK }
            .sumOf { it.window.durationMinutes }
        val peakFraction = (peakMinutesUsed / 120.0).coerceIn(0.0, 1.0)
        val peakCannibalizationPenalty = if (
            (snap50.phase == EnergyPhase.PEAK || snap50.phase == EnergyPhase.SURGE) &&
            breakdown.urgencyScore < 75.0
        ) {
            when {
                task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN ->
                    20.0 + peakFraction * 16.0
                !task.isFrog && task.energyDemand == EnergyDemand.MODERATE &&
                    (task.category == TaskCategory.ADMIN || task.category == TaskCategory.GENERAL) ->
                    10.0 + peakFraction * 8.0
                else -> 0.0
            }
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
                    alertBoost > 4.0 -> 8.0
                    alertBoost < -1.5 -> -18.0
                    else -> 0.0
                }
            } else if (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN || task.category == TaskCategory.MINDFULNESS) {
                if (alertBoost < -1.5) 6.0 else 0.0
            } else 0.0
        } else 0.0

        val contextDelta = computeContextTransitionDelta(placements, candidateStart, task, candidateEnd)
        val hoursFromNow = max(0.0, (candidateStart - earliestStartMillis) / 3_600_000.0)
        val deadlineBreachPenalty = if (task.deadlineMillis != null && candidateEnd > task.deadlineMillis) 35.0 else 0.0

        val antiFragmentationDelta = if (intervalStartMillis != null && intervalEndMillis != null) {
            val leftRemnant = candidateStart - intervalStartMillis
            val rightRemnant = intervalEndMillis - candidateEnd
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
            val internalSplitPenalty = if (
                !isFlushLeft && !isFlushRight &&
                (intervalEndMillis - intervalStartMillis) >= 90 * 60_000L &&
                task.energyDemand != EnergyDemand.HIGH
            ) {
                3.5
            } else {
                0.0
            }
            edgePackingBonus - (leftSliverPenalty + rightSliverPenalty) - internalSplitPenalty
        } else {
            0.0
        }

        val longerBlockHeadroomDelta = if (intervalStartMillis != null && intervalEndMillis != null && longerBlockPref > 0.05) {
            val headroomMillis = (intervalEndMillis - intervalStartMillis) - durationMillis
            when {
                headroomMillis >= 30 * 60_000L -> 8.5 * longerBlockPref
                headroomMillis >= 15 * 60_000L -> 3.5 * longerBlockPref
                headroomMillis < 10 * 60_000L -> -7.0 * longerBlockPref
                else -> 0.0
            }
        } else {
            0.0
        }

        val utility = breakdown.totalScore -
            (hourlyDecayLambda * hoursFromNow) -
            peakCannibalizationPenalty +
            contextDelta +
            stimAlignmentDelta -
            deadlineBreachPenalty -
            dipBoundaryBreachPenalty +
            antiFragmentationDelta +
            longerBlockHeadroomDelta +
            feedbackUtilityDelta -
            energyDebtPenalty

        return EvaluatedSlot(utility = utility, breakdown = breakdown, rejectionPenalty = totalRejectionPenalty)
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

        val fiveMinMillis = 5 * 60_000L

        for (interval in freeIntervals) {
            val candidateStarts = mutableListOf<Long>()
            val rawCur = max(interval.startMillis, earliestStartMillis)
            val rem5 = rawCur % fiveMinMillis
            var cur = if (rem5 == 0L) rawCur else rawCur + (fiveMinMillis - rem5)
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

            val effectiveStart = max(interval.startMillis, earliestStartMillis)
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

                val evaluated = evaluateCandidateSlotUtility(
                    task = task,
                    candidateStart = candidateStart,
                    candidateEnd = candidateEnd,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    dependentCount = dependentCount,
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
                    referenceNowMillis = referenceNowMillis ?: earliestStartMillis,
                    earliestStartMillis = earliestStartMillis,
                    rejectionPatternsJson = rejectionPatternsJson,
                    intervalStartMillis = effectiveStart,
                    intervalEndMillis = effectiveIntervalEnd
                )

                if (evaluated.utility > bestUtility) {
                    bestUtility = evaluated.utility
                    bestWindow = TimeSlotWindow(candidateStart, candidateEnd)
                    bestBreakdown = evaluated.breakdown
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
        rejectionPatternsJson: String = "{}",
        dependentsMap: Map<String, Set<String>> = emptyMap(),
        freeIntervalsBeforePlacement: List<TimeSlotWindow> = emptyList(),
        latestAllowedEndMap: Map<String, Long> = emptyMap(),
        dayStartMillis: Long = Long.MIN_VALUE,
        anchoredPlacementIds: Set<String> = emptySet(),
        categoryCalibrationRatios: Map<TaskCategory, Double> = emptyMap()
    ): List<ScheduledPlacement> {
        if (placements.size < 2) return placements
        val current = placements.sortedBy { it.window.startMillis }.toMutableList()
        val effectiveAnchor = if (dayStartMillis != Long.MIN_VALUE) dayStartMillis else current.first().window.startMillis
        val minGapMillis = TRANSITION_BUFFER_MINUTES * 60_000L

        fun calibrationFor(task: SchedulableTask): Double =
            (categoryCalibrationRatios[task.category] ?: calibrationRatio).coerceIn(0.8, 1.6)

        fun isCognitiveHeavy(task: SchedulableTask): Boolean =
            task.energyDemand == EnergyDemand.HIGH ||
                task.category == TaskCategory.DEEP_WORK ||
                task.category == TaskCategory.CREATIVE

        fun isSwapEligiblePair(p1: ScheduledPlacement, p2: ScheduledPlacement): Boolean {
            if (p1.task.isLocked || p2.task.isLocked) return false
            if (p1.task.id in anchoredPlacementIds || p2.task.id in anchoredPlacementIds) return false
            if (p1.task.parentTaskId != null || p2.task.parentTaskId != null ||
                p1.task.id.contains(Regex("""[#_]part""")) || p2.task.id.contains(Regex("""[#_]part""")) ||
                p1.task.id in p2.task.prerequisiteIds || p2.task.id in p1.task.prerequisiteIds ||
                cleanSplitTaskId(p1.task.id) == cleanSplitTaskId(p2.task.id)
            ) return false
            return true
        }

        fun checkPairwiseDagCeilingAndUltradianValid(proposedList: List<ScheduledPlacement>): Boolean {
            val byId = proposedList.associateBy { it.task.id }
            for (idx in proposedList.indices) {
                val p = proposedList[idx]
                if (p.window.startMillis < effectiveAnchor) return false
                val ceiling = latestAllowedEndMap[p.task.id]
                if (ceiling != null && p.window.endMillis > ceiling) return false
                for (prereqId in p.task.prerequisiteIds) {
                    val prereqPlacement = byId[prereqId] ?: continue
                    if (prereqPlacement.window.endMillis + minGapMillis > p.window.startMillis) return false
                }
                if (idx > 0) {
                    val prev = proposedList[idx - 1]
                    if (prev.window.endMillis + minGapMillis > p.window.startMillis) return false
                    if (isCognitiveHeavy(p.task) && isCognitiveHeavy(prev.task)) {
                        val priorLoad = computeSimulatedFocusLoadAt(p.window.startMillis, proposedList.take(idx))
                        if (priorLoad >= 90 && (p.window.startMillis - prev.window.endMillis) < ULTRADIAN_RECOVERY_BUFFER_MINUTES * 60_000L) {
                            return false
                        }
                    }
                }
            }
            return true
        }

        fun findContainingFreeInterval(window: TimeSlotWindow): TimeSlotWindow? =
            freeIntervalsBeforePlacement.firstOrNull { iv ->
                iv.startMillis <= window.startMillis && iv.endMillis >= window.endMillis
            }

        /**
         * Fits [durationMillis] around [anchorStartMillis] inside [containingInterval] without colliding
         * with [leftNeighbor] or [rightNeighbor] (plus transition buffers). Enables unequal-duration
         * non-adjacent 2-opt swaps whenever slack exists before/after the original slot.
         */
        fun fitSwappedWindow(
            anchorStartMillis: Long,
            durationMillis: Long,
            containingInterval: TimeSlotWindow,
            leftNeighbor: ScheduledPlacement?,
            rightNeighbor: ScheduledPlacement?
        ): TimeSlotWindow? {
            val minStart = max(
                max(containingInterval.startMillis, effectiveAnchor),
                leftNeighbor?.let { it.window.endMillis + minGapMillis } ?: Long.MIN_VALUE
            )
            val maxEnd = min(
                containingInterval.endMillis,
                rightNeighbor?.let { it.window.startMillis - minGapMillis } ?: Long.MAX_VALUE
            )
            if (maxEnd - minStart < durationMillis) return null
            val latestStart = maxEnd - durationMillis
            val clampedStart = anchorStartMillis.coerceIn(minStart, latestStart)
            // Snap to 5-minute alignment if shifting away from anchorStartMillis while keeping within [minStart, latestStart]
            val candidateStart = if (clampedStart == anchorStartMillis) {
                clampedStart
            } else {
                val aligned = ((clampedStart / 300_000L) * 300_000L).coerceIn(minStart, latestStart)
                aligned
            }
            val candidate = TimeSlotWindow(candidateStart, candidateStart + durationMillis)
            return if (fitsInFreeIntervals(freeIntervalsBeforePlacement, candidate)) candidate else null
        }

        fun evalSlot(
            task: SchedulableTask,
            window: TimeSlotWindow,
            priorPlacements: List<ScheduledPlacement>
        ): EvaluatedSlot {
            val containing = findContainingFreeInterval(window)
            return evaluateCandidateSlotUtility(
                task = task,
                candidateStart = window.startMillis,
                candidateEnd = window.endMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = lastSleepDurationMinutes,
                dependentCount = dependentsMap[task.id]?.size ?: 0,
                calibrationRatio = calibrationFor(task),
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                priorityWeights = priorityWeights,
                placements = priorPlacements,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                physiologicalBlocks = physiologicalBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = socialJetlagLagHours,
                referenceNowMillis = referenceNowMillis,
                earliestStartMillis = effectiveAnchor,
                rejectionPatternsJson = rejectionPatternsJson,
                intervalStartMillis = containing?.startMillis,
                intervalEndMillis = containing?.endMillis
            )
        }

        var improved = true
        var passes = 0
        while (improved && passes < 3) {
            improved = false
            passes++
            var i = 0
            while (i < current.size - 1) {
                var swappedAtI = false
                for (j in i + 1 until current.size) {
                    val p1 = current[i]
                    val p2 = current[j]
                    if (!isSwapEligiblePair(p1, p2)) continue

                    val d1 = p1.window.durationMinutes * 60_000L
                    val d2 = p2.window.durationMinutes * 60_000L
                    val gapBetween = p2.window.startMillis - p1.window.endMillis
                    val isTightlyAdjacent = (j == i + 1) && gapBetween in 0L..(20 * 60_000L)

                    val (newWindow2, newWindow1) = if (isTightlyAdjacent) {
                        val startA = p1.window.startMillis
                        val newEndB = startA + d2
                        val newStartA = newEndB + gapBetween
                        val newEndA = newStartA + d1
                        if (newEndA != p2.window.endMillis) continue
                        val w2 = TimeSlotWindow(startA, newEndB)
                        val w1 = TimeSlotWindow(newStartA, newEndA)
                        if (freeIntervalsBeforePlacement.isNotEmpty() &&
                            (!fitsInFreeIntervals(freeIntervalsBeforePlacement, w2) ||
                                !fitsInFreeIntervals(freeIntervalsBeforePlacement, w1))
                        ) continue
                        w2 to w1
                    } else {
                        if (freeIntervalsBeforePlacement.isEmpty()) continue
                        val interval1 = findContainingFreeInterval(p1.window) ?: continue
                        val interval2 = findContainingFreeInterval(p2.window) ?: continue
                        val leftNeighbor1 = current.getOrNull(i - 1)
                        val rightNeighbor1 = if (j == i + 1) null else current.getOrNull(i + 1)
                        val leftNeighbor2 = if (j == i + 1) null else current.getOrNull(j - 1)
                        val rightNeighbor2 = current.getOrNull(j + 1)
                        val w2 = fitSwappedWindow(
                            anchorStartMillis = p1.window.startMillis,
                            durationMillis = d2,
                            containingInterval = interval1,
                            leftNeighbor = leftNeighbor1,
                            rightNeighbor = rightNeighbor1
                        ) ?: continue
                        val w1 = fitSwappedWindow(
                            anchorStartMillis = p2.window.startMillis,
                            durationMillis = d1,
                            containingInterval = interval2,
                            leftNeighbor = leftNeighbor2,
                            rightNeighbor = rightNeighbor2
                        ) ?: continue
                        if (w2.endMillis + minGapMillis > w1.startMillis) continue
                        w2 to w1
                    }

                    // Deadline feasibility for both swapped windows
                    if (p2.task.deadlineMillis != null && newWindow2.endMillis > p2.task.deadlineMillis) continue
                    if (p1.task.deadlineMillis != null && newWindow1.endMillis > p1.task.deadlineMillis) continue

                    val others = current.filterIndexed { idx, _ -> idx != i && idx != j }
                    val evalP1Old = evalSlot(p1.task, p1.window, others.filter { it.window.endMillis <= p1.window.startMillis })
                    val evalP2Old = evalSlot(p2.task, p2.window, (others + p1).filter { it.window.endMillis <= p2.window.startMillis })
                    val currentUtility = evalP1Old.utility + evalP2Old.utility

                    val evalP2New = evalSlot(p2.task, newWindow2, others.filter { it.window.endMillis <= newWindow2.startMillis })
                    val provisionalNewP2 = ScheduledPlacement(p2.task, newWindow2, evalP2New.breakdown)
                    val evalP1New = evalSlot(p1.task, newWindow1, (others + provisionalNewP2).filter { it.window.endMillis <= newWindow1.startMillis })
                    val swappedUtility = evalP2New.utility + evalP1New.utility

                    if (swappedUtility - currentUtility <= 1.5) continue

                    val (p2Conf, p2Rat) = computePlacementConfidenceAndRationale(
                        task = p2.task,
                        window = newWindow2,
                        breakdown = evalP2New.breakdown,
                        rejectionPenalty = evalP2New.rejectionPenalty,
                        zoneId = zoneId,
                        wakeTimeMillis = wakeTimeMillis,
                        lastSleepDurationMinutes = lastSleepDurationMinutes,
                        continuousActiveFocusMinutes = computeSimulatedFocusLoadAt(newWindow2.startMillis, others.filter { it.window.endMillis <= newWindow2.startMillis }),
                        naps = naps,
                        chronotype = chronotype,
                        manualPeakStartHour = manualPeakStartHour,
                        learnedProfile = learnedProfile,
                        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                        physiologicalBlocks = physiologicalBlocks,
                        stimulantDoses = stimulantDoses,
                        socialJetlagLagHours = socialJetlagLagHours
                    )
                    val (p1Conf, p1Rat) = computePlacementConfidenceAndRationale(
                        task = p1.task,
                        window = newWindow1,
                        breakdown = evalP1New.breakdown,
                        rejectionPenalty = evalP1New.rejectionPenalty,
                        zoneId = zoneId,
                        wakeTimeMillis = wakeTimeMillis,
                        lastSleepDurationMinutes = lastSleepDurationMinutes,
                        continuousActiveFocusMinutes = computeSimulatedFocusLoadAt(newWindow1.startMillis, (others + provisionalNewP2).filter { it.window.endMillis <= newWindow1.startMillis }),
                        naps = naps,
                        chronotype = chronotype,
                        manualPeakStartHour = manualPeakStartHour,
                        learnedProfile = learnedProfile,
                        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                        physiologicalBlocks = physiologicalBlocks,
                        stimulantDoses = stimulantDoses,
                        socialJetlagLagHours = socialJetlagLagHours
                    )
                    val newPlacement2 = ScheduledPlacement(p2.task, newWindow2, evalP2New.breakdown, p2Conf, p2Rat)
                    val newPlacement1 = ScheduledPlacement(p1.task, newWindow1, evalP1New.breakdown, p1Conf, p1Rat)
                    val proposedList = (others + newPlacement2 + newPlacement1).sortedBy { it.window.startMillis }

                    if (!checkPairwiseDagCeilingAndUltradianValid(proposedList)) continue

                    current.clear()
                    current.addAll(proposedList)
                    improved = true
                    swappedAtI = true
                    break
                }
                if (!swappedAtI) i++ else i++
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
