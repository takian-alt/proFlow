package com.neuroflow.app.domain.service

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class EnergyDemand(val targetScore: Double) {
    LOW(25.0),
    MODERATE(50.0),
    HIGH(80.0)
}

enum class TaskCategory {
    DEEP_WORK, CREATIVE, ADMIN, EXERCISE, MINDFULNESS, GENERAL
}

enum class EnergyPhase {
    PEAK, REBOUND, DIP, WIND_DOWN, RECOVERY_NEEDED
}

enum class ChronotypeProfile(val peakOffsetFromWakeHours: Double) {
    DEFINITE_MORNING(3.0),
    MODERATE_MORNING(3.8),
    INTERMEDIATE(4.5),
    MODERATE_EVENING(6.5),
    DEFINITE_EVENING(8.0);

    companion object {
        fun fromString(value: String?): ChronotypeProfile {
            if (value.isNullOrBlank()) return INTERMEDIATE
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: INTERMEDIATE
        }
    }
}

/**
 * Computes circular weighted average of clock hours [0..24), properly handling
 * modulo-24 wrap-around across midnight using 2D unit vector trigonometric decomposition.
 */
fun blendClockHoursCircular(
    hoursWithWeights: List<Pair<Double, Double>>
): Double {
    if (hoursWithWeights.isEmpty()) return 10.0
    var sumX = 0.0
    var sumY = 0.0
    var totalWeight = 0.0
    for ((hour, weight) in hoursWithWeights) {
        if (weight <= 0.0) continue
        val theta = (hour % 24.0) * (2.0 * Math.PI / 24.0)
        sumX += weight * kotlin.math.cos(theta)
        sumY += weight * kotlin.math.sin(theta)
        totalWeight += weight
    }
    if (totalWeight <= 0.0 || (kotlin.math.abs(sumX) < 1e-9 && kotlin.math.abs(sumY) < 1e-9)) {
        return (hoursWithWeights.first().first % 24.0 + 24.0) % 24.0
    }
    var angle = kotlin.math.atan2(sumY, sumX)
    if (angle < 0.0) {
        angle += 2.0 * Math.PI
    }
    return (angle * 24.0 / (2.0 * Math.PI)) % 24.0
}

fun computePhasePeakHour(
    wakeClockHour: Double,
    chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
    manualPeakStartHour: Int? = null,
    detectedPeakHour: Double? = null,
    detectionConfidence: Double = 0.0
): Double {
    val bioPeak = (wakeClockHour + chronotype.peakOffsetFromWakeHours) % 24.0

    val effectiveBio = if (detectedPeakHour != null && detectionConfidence > 0.0) {
        val conf = detectionConfidence.coerceIn(0.0, 1.0)
        val empiricalWeight = 0.40 * conf
        val bioWeight = 1.0 - empiricalWeight
        blendClockHoursCircular(listOf(bioPeak to bioWeight, (detectedPeakHour % 24.0) to empiricalWeight))
    } else {
        bioPeak
    }

    return if (manualPeakStartHour != null && manualPeakStartHour in 0..23) {
        blendClockHoursCircular(listOf(effectiveBio to 0.70, manualPeakStartHour.toDouble() to 0.30))
    } else {
        effectiveBio
    }
}

enum class CognitiveBlockCategory {
    MEAL, EXERCISE, FOCUS_WORK, WIND_DOWN, SLEEP
}

typealias CognitiveBlockType = CognitiveBlockCategory

data class CognitiveBlockWindow(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val category: CognitiveBlockCategory,
    val intensity: Double = 1.0,
    val label: String = "",
    val explicitStartMillis: Long? = null,
    val explicitEndMillis: Long? = null,
    val explicitDemand: EnergyDemand? = null,
    val isFrog: Boolean = label.contains("Frog", ignoreCase = true),
    val isAnxietyTask: Boolean = false,
    val isCompleted: Boolean = false
) {
    val type: CognitiveBlockCategory get() = category
    val id: String get() = label
    val title: String get() = label
    val startMillis: Long get() = explicitStartMillis ?: (startMinuteOfDay * 60_000L)
    val endMillis: Long get() = explicitEndMillis ?: (endMinuteOfDay * 60_000L)
    val demand: EnergyDemand get() = explicitDemand ?: if (intensity > 1.0) EnergyDemand.HIGH else EnergyDemand.MODERATE

    val durationMinutes: Int
        get() = if (endMinuteOfDay >= startMinuteOfDay) {
            endMinuteOfDay - startMinuteOfDay
        } else {
            1440 - startMinuteOfDay + endMinuteOfDay
        }

    fun toTimeSlotWindow(date: LocalDate, zoneId: ZoneId): TimeSlotWindow {
        val startZdt = date.atStartOfDay(zoneId).plusMinutes(startMinuteOfDay.toLong())
        val endZdt = if (endMinuteOfDay >= startMinuteOfDay) {
            date.atStartOfDay(zoneId).plusMinutes(endMinuteOfDay.toLong())
        } else {
            date.plusDays(1).atStartOfDay(zoneId).plusMinutes(endMinuteOfDay.toLong())
        }
        return TimeSlotWindow(
            startMillis = startZdt.toInstant().toEpochMilli(),
            endMillis = endZdt.toInstant().toEpochMilli()
        )
    }

    fun toTimeSlotWindow(dayStartMillis: Long): TimeSlotWindow {
        val endOffset = if (endMinuteOfDay >= startMinuteOfDay) endMinuteOfDay else endMinuteOfDay + 1440
        return TimeSlotWindow(
            startMillis = dayStartMillis + startMinuteOfDay * 60_000L,
            endMillis = dayStartMillis + endOffset * 60_000L
        )
    }
}

data class BehavioralObservation(
    val minuteOfDay: Int,
    val productivityRating: Double, // 0.0 .. 1.0
    val weight: Double = 1.0
) {
    val clockHour: Double get() = (minuteOfDay / 60.0).coerceIn(0.0, 24.0) % 24.0
}

data class LearnedCircadianProfile(
    val peakHourMean: Double,
    val peakHourVariance: Double = 4.0,
    val sampleCount: Double = 0.0,
    val circadianAmplitude: Double = 1.0,
    val morningLarkProbability: Double = 0.5,
    val confidence: Double = (sampleCount / (sampleCount + 3.0)).coerceIn(0.0, 1.0),
    val hourlyMultipliers: List<Double> = emptyList()
) {
    val empiricalPeakHour: Double get() = peakHourMean
}

object AdaptiveCircadianLearner {
    const val DEFAULT_KAPPA = 4.5
    private const val PROCESS_NOISE_Q = 0.05

    /**
     * Modified Bessel function of the first kind of order 0: I_0(x).
     * Uses Abramowitz and Stegun polynomial approximations.
     */
    fun besselI0(x: Double): Double {
        val ax = kotlin.math.abs(x)
        return if (ax < 3.75) {
            val y = (x / 3.75) * (x / 3.75)
            1.0 + y * (3.5156229 + y * (3.0899424 + y * (1.2067492 + y * (0.2659732 + y * (0.0360768 + y * 0.0045813)))))
        } else {
            val y = 3.75 / ax
            (kotlin.math.exp(ax) / kotlin.math.sqrt(ax)) * (0.39894228 + y * (0.01328592 + y * (0.00225319 + y * (-0.00157565 + y * (0.00916281 + y * (-0.02057706 + y * (0.02635537 + y * (-0.01647633 + y * 0.00392377))))))))
        }
    }

    /**
     * Evaluates the Von Mises circular kernel probability density function at [hour] given [peakHour] and [kappa].
     */
    fun computeVonMisesDensity(hour: Double, peakHour: Double, kappa: Double = DEFAULT_KAPPA): Double {
        val angle = 2.0 * Math.PI * (hour - peakHour) / 24.0
        val i0 = besselI0(kappa)
        return kotlin.math.exp(kappa * kotlin.math.cos(angle)) / (2.0 * Math.PI * i0)
    }

    /**
     * Computes 24 hourly multipliers [0..23] using the Von Mises circular smoother with peak normalized to 1.0.
     */
    fun computeVonMisesHourlyMultipliers(peakHour: Double, kappa: Double = DEFAULT_KAPPA): List<Double> {
        return (0 until 24).map { h ->
            val angle = 2.0 * Math.PI * (h - peakHour) / 24.0
            kotlin.math.exp(kappa * (kotlin.math.cos(angle) - 1.0))
        }
    }

    fun computeMorningLarkProbability(peakHour: Double): Double {
        val normalizedHour = ((peakHour % 24.0) + 24.0) % 24.0
        return when {
            normalizedHour in 6.0..11.0 -> 0.85
            normalizedHour in 11.0..14.0 -> 0.60
            normalizedHour in 14.0..17.0 -> 0.35
            else -> 0.15
        }
    }

    /**
     * 1D circular Kalman filter state update.
     * Shortest circular innovation: e_k = ((z_k - theta_k + 36.0) % 24.0) - 12.0
     */
    fun updateProfile(
        prior: LearnedCircadianProfile,
        observation: BehavioralObservation,
        kappa: Double = DEFAULT_KAPPA
    ): LearnedCircadianProfile {
        val z = (observation.minuteOfDay / 60.0).coerceIn(0.0, 24.0) % 24.0
        val priorMean = prior.peakHourMean
        val priorVar = prior.peakHourVariance.coerceAtLeast(0.1)

        val shortestDiff = (((z - priorMean + 36.0) % 24.0) - 12.0)
        val obsWeight = (observation.productivityRating * observation.weight).coerceIn(0.1, 5.0)
        val r = (1.5 / obsWeight).coerceIn(0.3, 10.0)

        val kalmanGain = priorVar / (priorVar + r)
        val updatedMean = (((priorMean + kalmanGain * shortestDiff) % 24.0) + 24.0) % 24.0
        val updatedVar = ((1.0 - kalmanGain) * priorVar + PROCESS_NOISE_Q).coerceIn(0.05, 12.0)
        val updatedSampleCount = prior.sampleCount + observation.weight

        val multipliers = computeVonMisesHourlyMultipliers(updatedMean, kappa)
        val larkProb = computeMorningLarkProbability(updatedMean)

        return LearnedCircadianProfile(
            peakHourMean = updatedMean,
            peakHourVariance = updatedVar,
            sampleCount = updatedSampleCount,
            circadianAmplitude = 1.0,
            morningLarkProbability = larkProb,
            hourlyMultipliers = multipliers
        )
    }

    fun learnProfile(
        observations: List<BehavioralObservation>,
        initialPeakHour: Double = 9.0,
        kappa: Double = DEFAULT_KAPPA
    ): LearnedCircadianProfile {
        var profile = LearnedCircadianProfile(
            peakHourMean = initialPeakHour,
            peakHourVariance = 4.0,
            sampleCount = 0.0
        )
        for (obs in observations) {
            profile = updateProfile(profile, obs, kappa)
        }
        return profile
    }
}

object PhysiologicalImpulseCalculator {
    /**
     * Computes the acute post-prandial meal dip at [minuteOfDay].
     * Produces a negative impulse peaking at ~60 minutes after meal start.
     */
    fun computeMealImpulse(minuteOfDay: Int, mealBlocks: List<CognitiveBlockWindow>): Double {
        if (mealBlocks.isEmpty()) return 0.0
        var minImpulse = 0.0
        for (meal in mealBlocks) {
            val deltaMinutes = (minuteOfDay - meal.startMinuteOfDay + 1440) % 1440
            if (deltaMinutes in 0..180) {
                val tFactor = deltaMinutes / 60.0
                val dip = -12.0 * tFactor * kotlin.math.exp(1.0 - tFactor) * meal.intensity
                if (dip < minImpulse) {
                    minImpulse = dip
                }
            }
        }
        return minImpulse
    }

    /**
     * Computes the exercise impulse at [minuteOfDay].
     * During workout: mild exertion penalty (-8.0).
     * Post-workout (within 120m after workout end): acute endorphin/focus boost (+12.0 decaying with tau=45m).
     */
    fun computeExerciseImpulse(minuteOfDay: Int, workoutBlocks: List<CognitiveBlockWindow>): Double {
        if (workoutBlocks.isEmpty()) return 0.0
        var maxBoost = 0.0
        var duringPenalty = 0.0
        for (workout in workoutBlocks) {
            val start = workout.startMinuteOfDay
            val end = workout.endMinuteOfDay
            val isDuring = if (end >= start) {
                minuteOfDay in start..end
            } else {
                minuteOfDay >= start || minuteOfDay <= end
            }
            if (isDuring) {
                duringPenalty = kotlin.math.min(duringPenalty, -8.0 * workout.intensity)
            } else {
                val deltaMinutes = (minuteOfDay - end + 1440) % 1440
                if (deltaMinutes in 0..120) {
                    val boost = 12.0 * kotlin.math.exp(-deltaMinutes / 45.0) * workout.intensity
                    if (boost > maxBoost) {
                        maxBoost = boost
                    }
                }
            }
        }
        return if (duringPenalty < 0.0) duringPenalty else maxBoost
    }

    fun computeCombinedImpulse(minuteOfDay: Int, blocks: List<CognitiveBlockWindow>): Double {
        val mealBlocks = blocks.filter { it.category == CognitiveBlockCategory.MEAL }
        val workoutBlocks = blocks.filter { it.category == CognitiveBlockCategory.EXERCISE }
        return computeMealImpulse(minuteOfDay, mealBlocks) + computeExerciseImpulse(minuteOfDay, workoutBlocks)
    }
}

/**
 * Represents an intra-day restorative nap bout occurring after the primary morning wake time.
 * Discharges Process S (homeostatic pressure) without shifting Process C (circadian phase).
 */
data class DaytimeNapBout(
    val startMillis: Long,
    val endMillis: Long
) {
    val durationMinutes: Int
        get() = max(0L, (endMillis - startMillis) / 60_000L).toInt()
}

object PolyphasicNapCalculator {
    private const val S_MIN = 0.10
    private const val TAU_WAKE_HOURS = 18.2
    private const val TAU_NAP_CLEARANCE_HOURS = 2.8
    private const val INERTIA_THRESHOLD_MINUTES = 30
    private const val INERTIA_DECAY_TAU_MINUTES = 20.0
    private const val MAX_INERTIA_PENALTY = 0.12

    data class NapAdjustedHomeostasis(
        val processS: Double,
        val sleepInertiaPenalty: Double
    )

    /**
     * Evaluates sequential Process S accumulation and nap discharge across 0..N daytime naps.
     */
    fun computeHomeostaticState(
        timestampMillis: Long,
        morningWakeMillis: Long,
        initialPressureS0: Double,
        naps: List<DaytimeNapBout>,
        tauWakeHours: Double = TAU_WAKE_HOURS
    ): NapAdjustedHomeostasis {
        if (timestampMillis <= morningWakeMillis) {
            return NapAdjustedHomeostasis(initialPressureS0.coerceIn(0.0, 1.0), 0.0)
        }

        val validNaps = naps
            .filter { it.startMillis >= morningWakeMillis && it.endMillis > it.startMillis && it.durationMinutes in 10..180 }
            .sortedBy { it.startMillis }

        var currentAnchorTime = morningWakeMillis
        var currentAnchorS = initialPressureS0.coerceIn(S_MIN, 1.0)
        var inertiaPenalty = 0.0

        for (nap in validNaps) {
            if (timestampMillis < nap.startMillis) break

            // 1. Accumulate wake pressure up to nap start
            val wakeHoursBeforeNap = max(0.0, (nap.startMillis - currentAnchorTime) / 3_600_000.0)
            val sAtNapStart = 1.0 - (1.0 - currentAnchorS) * exp(-wakeHoursBeforeNap / tauWakeHours)

            if (timestampMillis <= nap.endMillis) {
                // Currently mid-nap: decay Process S up to current timestamp
                val elapsedNapHours = max(0.0, (timestampMillis - nap.startMillis) / 3_600_000.0)
                val sMidNap = S_MIN + (sAtNapStart - S_MIN) * exp(-elapsedNapHours / TAU_NAP_CLEARANCE_HOURS)
                return NapAdjustedHomeostasis(sMidNap.coerceIn(0.0, 1.0), 0.0)
            }

            // 2. Nap completed prior to timestampMillis: discharge S and advance anchor to nap.endMillis
            val napHours = nap.durationMinutes / 60.0
            currentAnchorS = S_MIN + (sAtNapStart - S_MIN) * exp(-napHours / TAU_NAP_CLEARANCE_HOURS)
            currentAnchorTime = nap.endMillis

            // 3. Evaluate transient sleep inertia if nap exceeded 30 minutes (SWS entry)
            if (nap.durationMinutes > INERTIA_THRESHOLD_MINUTES) {
                val minsSinceNapWake = max(0.0, (timestampMillis - nap.endMillis) / 60_000.0)
                val depthFactor = min(1.0, (nap.durationMinutes - INERTIA_THRESHOLD_MINUTES) / 30.0)
                inertiaPenalty = MAX_INERTIA_PENALTY * depthFactor * exp(-minsSinceNapWake / INERTIA_DECAY_TAU_MINUTES)
            } else {
                inertiaPenalty = 0.0
            }
        }

        // Accumulate wake pressure from the latest wake anchor (morning wake or last completed nap end)
        val remainingWakeHours = max(0.0, (timestampMillis - currentAnchorTime) / 3_600_000.0)
        val finalS = (1.0 - (1.0 - currentAnchorS) * exp(-remainingWakeHours / tauWakeHours)).coerceIn(0.0, 1.0)

        return NapAdjustedHomeostasis(
            processS = finalS,
            sleepInertiaPenalty = inertiaPenalty
        )
    }
}

data class CircadianSnapshot(
    val timestampMillis: Long,
    val usableEnergy: Int,            // [0..100]
    val homeostaticPressureS: Double, // [0.0..1.0]
    val circadianAlertnessC: Double,  // [0.0..1.0]
    val focusDepletionPenalty: Double,// [0.0..0.15]
    val phase: EnergyPhase,
    val sleepInertiaPenalty: Double = 0.0,
    val physiologicalImpulse: Double = 0.0,
    val predictionConfidence: Double = 0.85,
    val learnedAdjustmentPoints: Double = 0.0,
    val baselineBiologicalEnergy: Int = usableEnergy
) {
    val circadianAlertness: Double get() = circadianAlertnessC
    val homeostaticSleepPressure: Double get() = homeostaticPressureS
}

data class DailyCircadianSummary(
    val currentSnapshot: CircadianSnapshot,
    val peakWindowStartHour: Int,
    val peakWindowEndHour: Int,
    val peakEnergyScore: Int,
    val dipWindowStartHour: Int,
    val dipWindowEndHour: Int,
    val recommendedDeepWorkMinutes: Int
)

interface CircadianEnergyService {
    fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = emptyList()
    )

    fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList()
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour = null
    )

    fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        learnedProfile = null,
        rollingAverageSleepMinutes = null,
        cognitiveBlocks = emptyList()
    )

    fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        cognitiveBlocks: List<CognitiveBlockWindow> = emptyList()
    ): CircadianSnapshot

    fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int = 30,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<CircadianSnapshot> = projectDayTimeline(
        dayStartMillis = dayStartMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        stepMinutes = stepMinutes,
        zoneId = zoneId,
        naps = emptyList()
    )

    fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>
    ): List<CircadianSnapshot> = projectDayTimeline(
        dayStartMillis = dayStartMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        stepMinutes = stepMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour = null
    )

    fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null
    ): List<CircadianSnapshot> = projectDayTimeline(
        dayStartMillis = dayStartMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        stepMinutes = stepMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        cognitiveBlocks = emptyList(),
        learnedProfile = null,
        rollingAverageSleepMinutes = null
    )

    fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int = 30,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null
    ): List<CircadianSnapshot>

    fun computeDailySummary(
        timestampMillis: Long = System.currentTimeMillis(),
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int = 480,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyCircadianSummary = computeDailySummary(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = emptyList(),
        chronotype = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour = null
    )

    fun computeDailySummary(
        timestampMillis: Long = System.currentTimeMillis(),
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int = 480,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList()
    ): DailyCircadianSummary = computeDailySummary(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour = null
    )

    fun computeDailySummary(
        timestampMillis: Long = System.currentTimeMillis(),
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int = 480,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null
    ): DailyCircadianSummary = computeDailySummary(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        cognitiveBlocks = emptyList(),
        learnedProfile = null,
        rollingAverageSleepMinutes = null
    )

    fun computeDailySummary(
        timestampMillis: Long = System.currentTimeMillis(),
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int = 480,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null
    ): DailyCircadianSummary

    fun computeCategoryAffinity(
        category: TaskCategory,
        snapshot: CircadianSnapshot
    ): Double
}

@Singleton
class DefaultCircadianEnergyService @Inject constructor() : CircadianEnergyService {

    companion object {
        private const val S_MIN = 0.10
        private const val S_MAX = 0.85
        private const val TAU_RECOVERY_HOURS = 3.6
        private const val TAU_WAKE_HOURS = 18.2
        private const val PEAK_OFFSET_FROM_WAKE_HOURS = 4.5
        private const val MAX_FOCUS_DEPLETION = 0.15
    }

    override fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = emptyList()
    )

    override fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        learnedProfile = null,
        rollingAverageSleepMinutes = null,
        cognitiveBlocks = emptyList()
    )

    override fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        cognitiveBlocks: List<CognitiveBlockWindow>
    ): CircadianSnapshot {
        // 0. Multi-Day Allostatic Sleep Debt Carryover
        val allostaticDebtHours = if (rollingAverageSleepMinutes != null && rollingAverageSleepMinutes > 0.0) {
            max(0.0, (480.0 - rollingAverageSleepMinutes) / 60.0)
        } else {
            0.0
        }
        val alphaAllostatic = min(0.50, allostaticDebtHours / 8.0)

        // 1. Continuous Sleep Recovery -> Initial Wake Pressure S0 (No 60m cliff)
        val sleepHours = max(0.0, lastSleepDurationMinutes / 60.0)
        val baseS0 = S_MIN + (S_MAX - S_MIN) * exp(-sleepHours / TAU_RECOVERY_HOURS)
        val s0 = baseS0 + alphaAllostatic * (1.0 - baseS0) * 0.35
        val effectiveTauWakeHours = TAU_WAKE_HOURS / (1.0 + alphaAllostatic)

        // 2. Continuous Homeostatic Wake Pressure with Daytime Nap Discharge
        val homeostatic = PolyphasicNapCalculator.computeHomeostaticState(
            timestampMillis = timestampMillis,
            morningWakeMillis = wakeTimeMillis,
            initialPressureS0 = s0,
            naps = naps,
            tauWakeHours = effectiveTauWakeHours
        )
        val processS = homeostatic.processS

        // 2b. Morning Sleep Inertia (first 30m after morning wake) + Nap Inertia
        val minsSinceMorningWake = ((timestampMillis - wakeTimeMillis) / 60_000.0)
        val morningInertia = if (minsSinceMorningWake in 0.0..30.0) {
            0.14 * (1.0 - (minsSinceMorningWake / 30.0))
        } else {
            0.0
        }
        val inertiaPenalty = max(homeostatic.sleepInertiaPenalty, morningInertia)

        // 3. DST-Safe Wall-Clock Extraction for Process C(t) (phase-locked with chronotype modulation)
        val zonedNow = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val clockHour = zonedNow.hour + (zonedNow.minute / 60.0)
        val wakeClockHour = zonedWake.hour + (zonedWake.minute / 60.0)
        val detectedPeak = learnedProfile?.peakHourMean
        val detectedConf = learnedProfile?.confidence ?: 0.0
        val phasePeakPhi = computePhasePeakHour(
            wakeClockHour = wakeClockHour,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            detectedPeakHour = detectedPeak,
            detectionConfidence = detectedConf
        )

        // Bimodal sinusoidal model (captures morning peak, afternoon dip, early evening rebound)
        val primaryHarmonic = 0.38 * cos((2.0 * PI * (clockHour - phasePeakPhi)) / 24.0)
        val secondaryHarmonic = 0.12 * cos((4.0 * PI * (clockHour - phasePeakPhi)) / 24.0)
        val processC = (0.50 + primaryHarmonic - secondaryHarmonic).coerceIn(0.0, 1.0)

        // 4. Synaptic Depletion from unbroken focus > 45m (Fixes Flaw #1: Never penalizes entering flow)
        val excessFocusMins = max(0, continuousActiveFocusMinutes - 45).toDouble()
        val focusDepletion = if (excessFocusMins > 0.0) {
            MAX_FOCUS_DEPLETION * (1.0 - exp(-excessFocusMins / 60.0))
        } else {
            0.0
        }

        // 4b. Physiological Impulses (Meals & Exercise)
        val minuteOfDay = zonedNow.hour * 60 + zonedNow.minute
        val physioImpulse = if (cognitiveBlocks.isNotEmpty()) {
            PhysiologicalImpulseCalculator.computeCombinedImpulse(minuteOfDay, cognitiveBlocks)
        } else {
            0.0
        }

        // 5. Unified Usable Energy [0..100] (Fixes Flaw #8: Single fatigue representation)
        val baseRawEnergy = 100.0 * (0.55 * processC + 0.45 * (1.0 - processS) - inertiaPenalty)
        val baselineBio = baseRawEnergy.roundToInt().coerceIn(0, 100)
        val rawEnergy = 100.0 * (0.55 * processC + 0.45 * (1.0 - processS) - focusDepletion - inertiaPenalty) + physioImpulse
        val usableEnergy = rawEnergy.roundToInt().coerceIn(0, 100)
        val learnedAdj = (usableEnergy - baselineBio).toDouble()
        val predConf = learnedProfile?.confidence ?: 0.85

        val hoursAwake = max(0.0, (timestampMillis - wakeTimeMillis) / 3_600_000.0)
        val phase = when {
            continuousActiveFocusMinutes >= 90 -> EnergyPhase.RECOVERY_NEEDED
            usableEnergy >= 72 -> EnergyPhase.PEAK
            hoursAwake in 6.0..9.5 && processC < 0.48 -> EnergyPhase.DIP
            hoursAwake > 9.5 && usableEnergy >= 55 -> EnergyPhase.REBOUND
            else -> EnergyPhase.WIND_DOWN
        }

        return CircadianSnapshot(
            timestampMillis = timestampMillis,
            usableEnergy = usableEnergy,
            homeostaticPressureS = processS,
            circadianAlertnessC = processC,
            focusDepletionPenalty = focusDepletion,
            phase = phase,
            sleepInertiaPenalty = inertiaPenalty,
            physiologicalImpulse = physioImpulse,
            predictionConfidence = predConf,
            learnedAdjustmentPoints = learnedAdj,
            baselineBiologicalEnergy = baselineBio
        )
    }

    override fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?
    ): List<CircadianSnapshot> = projectDayTimeline(
        dayStartMillis = dayStartMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        stepMinutes = stepMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        cognitiveBlocks = emptyList(),
        learnedProfile = null,
        rollingAverageSleepMinutes = null
    )

    override fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        cognitiveBlocks: List<CognitiveBlockWindow>,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?
    ): List<CircadianSnapshot> {
        val stepMillis = stepMinutes * 60_000L
        val steps = (24 * 60) / stepMinutes
        return List(steps) { idx ->
            val t = dayStartMillis + idx * stepMillis
            evaluateSnapshot(
                timestampMillis = t,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = lastSleepDurationMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                cognitiveBlocks = cognitiveBlocks
            )
        }
    }

    override fun computeDailySummary(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?
    ): DailyCircadianSummary = computeDailySummary(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        cognitiveBlocks = emptyList(),
        learnedProfile = null,
        rollingAverageSleepMinutes = null
    )

    override fun computeDailySummary(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        cognitiveBlocks: List<CognitiveBlockWindow>,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?
    ): DailyCircadianSummary {
        val currentSnapshot = evaluateSnapshot(
            timestampMillis = timestampMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            continuousActiveFocusMinutes = continuousActiveFocusMinutes,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            cognitiveBlocks = cognitiveBlocks
        )

        val zonedTimestamp = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
        val dayStartMillis = zonedTimestamp.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val wakeHourOfDay = zonedWake.hour + (zonedWake.minute / 60.0)
        val dayWakeMillis = dayStartMillis + (wakeHourOfDay * 3600_000).toLong()

        val timeline = projectDayTimeline(
            dayStartMillis = dayStartMillis,
            wakeTimeMillis = dayWakeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            stepMinutes = 30,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            cognitiveBlocks = cognitiveBlocks,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes
        )

        // Peak Window: argmax E_usable(t)
        val peakSnapshot = timeline.maxByOrNull { it.usableEnergy } ?: currentSnapshot
        val peakHour = Instant.ofEpochMilli(peakSnapshot.timestampMillis).atZone(zoneId).hour
        val peakEndHour = (peakHour + 3) % 24
        val peakEnergyScore = peakSnapshot.usableEnergy

        // Dip Window: argmin E_usable(t) in [wake + 4h, wake + 11h]
        val dipStartSearchMillis = dayWakeMillis + 4 * 3600_000L
        val dipEndSearchMillis = dayWakeMillis + 11 * 3600_000L
        val dipCandidates = timeline.filter { it.timestampMillis in dipStartSearchMillis..dipEndSearchMillis }
        val dipSnapshot = dipCandidates.minByOrNull { it.usableEnergy }
            ?: timeline.minByOrNull { it.usableEnergy }
            ?: currentSnapshot
        val dipHour = Instant.ofEpochMilli(dipSnapshot.timestampMillis).atZone(zoneId).hour
        val dipEndHour = (dipHour + 2) % 24

        // Recommended deep work minutes based on high energy capacity
        val highEnergySteps = timeline.count { it.usableEnergy >= 70 }
        val recommendedDeepWorkMinutes = (highEnergySteps * 30).coerceIn(60, 240)

        return DailyCircadianSummary(
            currentSnapshot = currentSnapshot,
            peakWindowStartHour = peakHour,
            peakWindowEndHour = peakEndHour,
            peakEnergyScore = peakEnergyScore,
            dipWindowStartHour = dipHour,
            dipWindowEndHour = dipEndHour,
            recommendedDeepWorkMinutes = recommendedDeepWorkMinutes
        )
    }

    override fun computeCategoryAffinity(
        category: TaskCategory,
        snapshot: CircadianSnapshot
    ): Double {
        val e = snapshot.usableEnergy.toDouble()
        return when (category) {
            TaskCategory.DEEP_WORK -> e // Linearly favors peak energy
            TaskCategory.CREATIVE -> {
                // Favors high alertness AND moderate associative disinhibition (rebound/peak)
                if (snapshot.phase == EnergyPhase.REBOUND) min(100.0, e + 15.0) else e * 0.9
            }
            TaskCategory.ADMIN -> {
                // Peaks around 45-60 energy (ideal for afternoon dip; doesn't waste peak hours)
                100.0 * exp(-((e - 50.0) * (e - 50.0)) / (2.0 * 22.0 * 22.0))
            }
            TaskCategory.EXERCISE -> {
                // Ideal in afternoon rebound or moderate energy
                100.0 * exp(-((e - 65.0) * (e - 65.0)) / (2.0 * 25.0 * 25.0))
            }
            TaskCategory.MINDFULNESS -> {
                // Best during wind-down, dip, or recovery needed
                if (snapshot.phase == EnergyPhase.RECOVERY_NEEDED) 100.0 else (100.0 - e * 0.5).coerceIn(30.0, 95.0)
            }
            TaskCategory.GENERAL -> 70.0
        }.coerceIn(0.0, 100.0)
    }
}
