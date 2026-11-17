package com.neuroflow.app.domain.service

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Neurobiologically grounded fatigue-aware heuristic for ADHD/chronobiology task scheduling.
 *
 * Models two complementary fatigue pressures:
 * 1. State biological fatigue from homeostatic sleep pressure (Process S), synaptic focus depletion,
 *    usable energy deficit, and circadian phase.
 * 2. Cumulative allostatic cognitive load accumulated across prior scheduled tasks in the day.
 *
 * Adjusts task priority scores smoothly based on the interaction of task duration, estimated
 * cognitive load (energy demand + category executive load), and current fatigue to prevent
 * burnout while keeping urgent deadlines safe and promoting restorative pacing.
 */
object FatigueHeuristic {

    private const val WEIGHTED_LOAD_SOFT_ONSET_MINUTES = 90.0
    private const val WEIGHTED_LOAD_SATURATION_SPAN = 210.0
    private const val ALLOSTATIC_RECOVERY_TAU_MINUTES = 240.0

    data class FatigueAdjustmentResult(
        val priorityDelta: Double,
        val fatigueIndex: Double,
        val cognitiveLoadIntensity: Double,
        val durationStrain: Double
    )

    /**
     * Estimates dimensionless cognitive load intensity in [0.08 .. 1.25] from a task's
     * energy demand, category executive demands, and dread/anxiety regulation overhead.
     */
    fun computeCognitiveLoadIntensity(task: SchedulableTask): Double {
        val baseDemand = when (task.energyDemand) {
            EnergyDemand.HIGH -> 1.00
            EnergyDemand.MODERATE -> 0.55
            EnergyDemand.LOW -> 0.20
        }
        val categoryDelta = when (task.category) {
            TaskCategory.DEEP_WORK -> 0.20
            TaskCategory.CREATIVE -> 0.15
            TaskCategory.ADMIN -> 0.0
            TaskCategory.GENERAL -> 0.0
            TaskCategory.EXERCISE -> -0.15
            TaskCategory.MINDFULNESS -> -0.15
        }
        val anxietyOverhead = if (
            task.isAnxietyTask &&
            task.category != TaskCategory.MINDFULNESS &&
            task.category != TaskCategory.EXERCISE
        ) {
            0.10
        } else {
            0.0
        }
        val raw = baseDemand + categoryDelta + anxietyOverhead
        return when (task.category) {
            TaskCategory.MINDFULNESS -> raw.coerceIn(0.08, 0.45)
            TaskCategory.EXERCISE -> raw.coerceIn(0.12, 0.75)
            else -> raw.coerceIn(0.15, 1.25)
        }
    }

    /**
     * Computes nonlinear sustained-attention duration strain in [0.05 .. 1.35].
     * Scales smoothly with duration and applies an ultradian overrun multiplier past 90 minutes.
     */
    fun computeDurationStrain(bufferedDurationMinutes: Int): Double {
        val mins = max(5, bufferedDurationMinutes)
        val baseStrain = 1.0 - exp(-mins / 70.0)
        val ultradianOverrun = if (mins > 90) {
            1.0 + (min(90, mins - 90) / 180.0)
        } else {
            1.0
        }
        return (baseStrain * ultradianOverrun).coerceIn(0.05, 1.35)
    }

    /**
     * Computes cumulative weighted cognitive load-minutes at [timestampMillis] across all
     * prior placements, accounting for gradual allostatic recovery during inter-task gaps
     * and active restorative discharge from mindfulness/exercise blocks.
     */
    fun computeCumulativeCognitiveLoadAt(
        timestampMillis: Long,
        placements: List<ScheduledPlacement>
    ): Double {
        val prior = placements
            .filter { it.window.endMillis <= timestampMillis }
            .sortedBy { it.window.startMillis }
        if (prior.isEmpty()) return 0.0

        var weightedLoad = 0.0
        var lastEnd = prior.first().window.startMillis

        for (placement in prior) {
            val gapMinutes = max(0.0, (placement.window.startMillis - lastEnd) / 60_000.0)
            if (gapMinutes > 0.0 && weightedLoad > 0.0) {
                weightedLoad *= exp(-gapMinutes / ALLOSTATIC_RECOVERY_TAU_MINUTES)
            }
            val dur = max(1, placement.window.durationMinutes).toDouble()
            val task = placement.task
            if (task.category == TaskCategory.MINDFULNESS ||
                (task.category == TaskCategory.EXERCISE && task.energyDemand != EnergyDemand.HIGH)
            ) {
                val discharge = dur * 0.45
                weightedLoad = max(0.0, weightedLoad - discharge)
            } else {
                weightedLoad += dur * computeCognitiveLoadIntensity(task)
            }
            lastEnd = placement.window.endMillis
        }

        val finalGapMinutes = max(0.0, (timestampMillis - lastEnd) / 60_000.0)
        if (finalGapMinutes > 0.0 && weightedLoad > 0.0) {
            weightedLoad *= exp(-finalGapMinutes / ALLOSTATIC_RECOVERY_TAU_MINUTES)
        }
        return max(0.0, weightedLoad)
    }

    /**
     * Evaluates current allostatic cognitive load immediately after the latest placed item
     * in [placements] for dynamic ready-queue prioritization.
     */
    fun computeCurrentQueueCognitiveLoad(placements: List<ScheduledPlacement>): Double {
        if (placements.isEmpty()) return 0.0
        val latestEnd = placements.maxOf { it.window.endMillis }
        return computeCumulativeCognitiveLoadAt(latestEnd, placements)
    }

    /**
     * Combines biological snapshot fatigue and cumulative session/day cognitive load
     * into a normalized fatigue index in [0.0 .. 1.0].
     */
    fun computeFatigueIndex(
        snapshot: CircadianSnapshot,
        cumulativeCognitiveLoadMinutes: Double = 0.0
    ): Double {
        val pressureFatigue = ((snapshot.homeostaticPressureS - 0.30) / 0.50).coerceIn(0.0, 1.0)
        val depletionFatigue = (snapshot.focusDepletionPenalty / 0.12).coerceIn(0.0, 1.0)
        val energyDeficit = ((55.0 - snapshot.usableEnergy) / 45.0).coerceIn(0.0, 1.0)

        val phaseFloor = when (snapshot.phase) {
            EnergyPhase.CRASH, EnergyPhase.TROUGH -> 0.85
            EnergyPhase.RECOVERY_NEEDED -> 0.78
            EnergyPhase.WIND_DOWN, EnergyPhase.INERTIA -> 0.45
            EnergyPhase.DIP -> 0.35
            EnergyPhase.RECOVERY -> 0.25
            else -> 0.0
        }

        val bioFatigue = max(
            phaseFloor,
            (0.45 * pressureFatigue + 0.35 * depletionFatigue + 0.20 * energyDeficit)
        ).coerceIn(0.0, 1.0)

        val sessionFatigue = (
            (cumulativeCognitiveLoadMinutes - WEIGHTED_LOAD_SOFT_ONSET_MINUTES) / WEIGHTED_LOAD_SATURATION_SPAN
        ).coerceIn(0.0, 1.0)

        return (1.0 - (1.0 - bioFatigue) * (1.0 - 0.90 * sessionFatigue)).coerceIn(0.0, 1.0)
    }

    /**
     * Computes a signed priority score adjustment in [-28.0 .. +12.0]:
     * - Penalizes long, high cognitive-load tasks proportionally to current fatigue to prevent burnout.
     * - Applies an intrinsic ultradian overrun guard for marathon (>90m) high-load blocks.
     * - Boosts restorative / low-strain tasks when fatigue is elevated to encourage healthy pacing.
     * - Softens penalties gently when urgency is critical (>= 85) so emergencies aren't starved.
     */
    fun evaluatePriorityAdjustment(
        task: SchedulableTask,
        bufferedDurationMinutes: Int,
        snapshot: CircadianSnapshot,
        cumulativeCognitiveLoadMinutes: Double = 0.0,
        urgencyScore: Double = 0.0
    ): FatigueAdjustmentResult {
        val loadIntensity = computeCognitiveLoadIntensity(task)
        val durationStrain = computeDurationStrain(bufferedDurationMinutes)
        val fatigueIndex = computeFatigueIndex(snapshot, cumulativeCognitiveLoadMinutes)
        val taskStrain = loadIntensity * durationStrain

        val isRestorative = task.category == TaskCategory.MINDFULNESS ||
            (task.category == TaskCategory.EXERCISE && task.energyDemand != EnergyDemand.HIGH)

        val rawDelta = when {
            isRestorative -> {
                if (fatigueIndex >= 0.20) {
                    val lengthMod = (1.0 - 0.35 * max(0.0, durationStrain - 0.55)).coerceIn(0.45, 1.0)
                    12.0 * fatigueIndex * lengthMod
                } else {
                    0.0
                }
            }
            loadIntensity < 0.35 -> {
                val quickWinRelief = if (fatigueIndex >= 0.20) {
                    7.0 * fatigueIndex * (1.0 - 0.50 * durationStrain).coerceIn(0.25, 1.0)
                } else {
                    0.0
                }
                val fatigueDragOnMarathonLow = if (bufferedDurationMinutes > 75 && fatigueIndex > 0.30) {
                    -6.0 * fatigueIndex * (durationStrain - 0.55).coerceAtLeast(0.0)
                } else {
                    0.0
                }
                quickWinRelief + fatigueDragOnMarathonLow
            }
            else -> {
                val fatigueBurnoutPenalty = -24.0 * fatigueIndex * taskStrain
                val marathonOverrunPenalty = if (bufferedDurationMinutes > 90 && loadIntensity >= 0.80) {
                    -((min(90, bufferedDurationMinutes - 90) / 90.0) * 8.0 * loadIntensity)
                } else {
                    0.0
                }
                val emergencyReliefFactor = if (urgencyScore >= 85.0) 0.55 else 1.0
                (fatigueBurnoutPenalty + marathonOverrunPenalty) * emergencyReliefFactor
            }
        }

        return FatigueAdjustmentResult(
            priorityDelta = rawDelta.coerceIn(-28.0, 12.0),
            fatigueIndex = fatigueIndex,
            cognitiveLoadIntensity = loadIntensity,
            durationStrain = durationStrain
        )
    }
}
