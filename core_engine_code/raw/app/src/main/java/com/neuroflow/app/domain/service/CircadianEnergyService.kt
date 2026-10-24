package com.neuroflow.app.domain.service

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
 * Hardened Abramowitz & Stegun exponentially scaled Bessel function:
 * Returns I_0(x) * exp(-|x|), which is strictly bounded in (0, 1] for all x in (-inf, +inf)
 * and never overflows to Infinity even at kappa = 10,000+.
 */
fun besselI0Scaled(x: Double): Double {
    val ax = abs(x)
    if (ax.isNaN()) return 1.0
    if (ax.isInfinite()) return 0.0
    return if (ax < 3.75) {
        val y = (x / 3.75) * (x / 3.75)
        val raw = 1.0 + y * (3.5156229 + y * (3.0899424 + y * (1.2067492 + y * (0.2659732 + y * (0.0360768 + y * 0.0045813)))))
        raw * exp(-ax)
    } else {
        val y = 3.75 / ax
        (1.0 / sqrt(ax)) * (0.39894228 + y * (0.01328592 + y * (0.00225319 + y * (-0.00157565 + y * (0.00916281 + y * (-0.02057706 + y * (0.02635537 + y * (-0.01647633 + y * 0.00392377))))))))
    }
}

/**
 * Overflow-safe Von Mises circular probability density function.
 * Uses identity: exp(k * cos(theta)) / I_0(k) == exp(k * (cos(theta) - 1)) / (I_0(k) * exp(-k)).
 */
fun computeVonMisesDensitySafe(
    hour: Double,
    peakHour: Double,
    kappa: Double = AdaptiveCircadianLearner.DEFAULT_KAPPA
): Double {
    val safeKappa = kappa.coerceIn(0.0, 10_000.0)
    if (safeKappa <= 1e-6) return 1.0 / (2.0 * PI)
    val angle = 2.0 * PI * (hour - peakHour) / 24.0
    val scaledI0 = besselI0Scaled(safeKappa).coerceAtLeast(1e-12)
    return (exp(safeKappa * (cos(angle) - 1.0)) / (2.0 * PI * scaledI0)).coerceAtLeast(0.0)
}

/**
 * C1-Continuous Post-Prandial Meal Impulse Calculator.
 * Preserves the exact -12.0 peak at delta = 60m and smoothly tapers to 0.0 over [135m..180m]
 * using a raised cosine window, eliminating the 4.87pt cliff at 180m.
 */
fun computeSmoothMealImpulse(
    minuteOfDay: Int,
    mealBlocks: List<CognitiveBlockWindow>,
    timestampMillis: Long? = null
): Double {
    if (mealBlocks.isEmpty()) return 0.0
    var minImpulse = 0.0
    for (meal in mealBlocks) {
        val deltaMinutes = if (timestampMillis != null && meal.explicitStartMillis != null) {
            ((timestampMillis - meal.explicitStartMillis) / 60_000L).toInt()
        } else {
            (minuteOfDay - meal.startMinuteOfDay + 1440) % 1440
        }
        if (deltaMinutes in 0..180) {
            val tFactor = deltaMinutes / 60.0
            val rawDip = -12.0 * tFactor * exp(1.0 - tFactor) * meal.intensity
            val tailWindow = if (deltaMinutes <= 135) {
                1.0
            } else {
                val u = (deltaMinutes - 135.0) / 45.0
                0.5 * (1.0 + cos(PI * u))
            }
            val smoothedDip = rawDip * tailWindow
            if (smoothedDip < minImpulse) {
                minImpulse = smoothedDip
            }
        }
    }
    return minImpulse
}

/**
 * Computes the latest clock minute of the day [0..1439] at which a user can consume
 * [doseMg] of [type] without exceeding [maxResidualMgAtBedtime] at [sleepHour].
 */
fun computeLatestSafeStimulantCutoffMinute(
    sleepHour: Int,
    doseMg: Double = 100.0,
    type: StimulantType = StimulantType.CAFFEINE,
    maxResidualMgAtBedtime: Double = 35.0
): Int {
    val sleepMinuteOfDay = (sleepHour.coerceIn(0, 23)) * 60
    val effectiveDose = (doseMg / type.referenceDoseMg) * 100.0
    if (effectiveDose <= maxResidualMgAtBedtime) return sleepMinuteOfDay
    val ke = ln(2.0) / type.halfLifeHours
    val requiredClearanceHours = ln(effectiveDose / maxResidualMgAtBedtime.coerceAtLeast(1.0)) / ke
    val clearanceMinutes = (requiredClearanceHours * 60.0).roundToInt().coerceIn(0, 1440)
    return ((sleepMinuteOfDay - clearanceMinutes) % 1440 + 1440) % 1440
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
        sumX += weight * cos(theta)
        sumY += weight * sin(theta)
        totalWeight += weight
    }
    if (totalWeight <= 0.0 || (abs(sumX) < 1e-9 && abs(sumY) < 1e-9)) {
        return (hoursWithWeights.first().first % 24.0 + 24.0) % 24.0
    }
    var angle = atan2(sumY, sumX)
    if (angle < 0.0) {
        angle += 2.0 * Math.PI
    }
    return (angle * 24.0 / (2.0 * Math.PI)) % 24.0
}

/**
 * Signed shortest circular difference (a - b) in clock hours within [-12.0, +12.0].
 */
fun circularHourDifference(hourA: Double, hourB: Double): Double {
    val diff = ((hourA - hourB) % 24.0 + 36.0) % 24.0 - 12.0
    return diff.coerceIn(-12.0, 12.0)
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

/**
 * Interpolates the 24-bin Von Mises multiplier for fractional [clockHour] and blends it
 * with the bimodal harmonic curve according to [learnedProfile.confidence].
 */
fun applyLearnedVonMisesModulation(
    baseProcessC: Double,
    clockHour: Double,
    learnedProfile: LearnedCircadianProfile?
): Double {
    if (learnedProfile == null || learnedProfile.hourlyMultipliers.size != 24 || learnedProfile.confidence <= 0.05) {
        return baseProcessC
    }
    val normHour = ((clockHour % 24.0) + 24.0) % 24.0
    val h0 = normHour.toInt() % 24
    val h1 = (h0 + 1) % 24
    val frac = normHour - normHour.toInt()

    val m0 = learnedProfile.hourlyMultipliers[h0]
    val m1 = learnedProfile.hourlyMultipliers[h1]
    val interpolatedVonMises = (m0 + frac * (m1 - m0)).coerceIn(0.0, 1.0)

    // Blend up to 35% of the empirical Von Mises density shape as confidence approaches 1.0
    val blendWeight = (0.35 * learnedProfile.confidence).coerceIn(0.0, 0.35)
    return ((1.0 - blendWeight) * baseProcessC + blendWeight * interpolatedVonMises).coerceIn(0.0, 1.0)
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
        if (explicitStartMillis != null && explicitEndMillis != null) {
            return TimeSlotWindow(explicitStartMillis, explicitEndMillis)
        }
        val startZdt = date.atStartOfDay(zoneId).plusMinutes(startMinuteOfDay.toLong())
        val endZdt = if (endMinuteOfDay >= startMinuteOfDay) {
            date.atStartOfDay(zoneId).plusMinutes(endMinuteOfDay.toLong())
        } else {
            date.plusDays(1).atStartOfDay(zoneId).plusMinutes(endMinuteOfDay.toLong())
        }
        return TimeSlotWindow(
            startMillis = explicitStartMillis ?: startZdt.toInstant().toEpochMilli(),
            endMillis = explicitEndMillis ?: endZdt.toInstant().toEpochMilli()
        )
    }

    fun toTimeSlotWindow(dayStartMillis: Long): TimeSlotWindow {
        if (explicitStartMillis != null && explicitEndMillis != null) {
            return TimeSlotWindow(explicitStartMillis, explicitEndMillis)
        }
        val endOffset = if (endMinuteOfDay >= startMinuteOfDay) endMinuteOfDay else endMinuteOfDay + 1440
        return TimeSlotWindow(
            startMillis = explicitStartMillis ?: (dayStartMillis + startMinuteOfDay * 60_000L),
            endMillis = explicitEndMillis ?: (dayStartMillis + endOffset * 60_000L)
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
        val ax = abs(x)
        return if (ax < 3.75) {
            val y = (x / 3.75) * (x / 3.75)
            1.0 + y * (3.5156229 + y * (3.0899424 + y * (1.2067492 + y * (0.2659732 + y * (0.0360768 + y * 0.0045813)))))
        } else {
            val y = 3.75 / ax
            (exp(ax) / sqrt(ax)) * (0.39894228 + y * (0.01328592 + y * (0.00225319 + y * (-0.00157565 + y * (0.00916281 + y * (-0.02057706 + y * (0.02635537 + y * (-0.01647633 + y * 0.00392377))))))))
        }
    }

    /**
     * Evaluates the Von Mises circular kernel probability density function at [hour] given [peakHour] and [kappa].
     * Note: For guaranteed numerical stability across all kappa in [0, 10000], see [computeVonMisesDensitySafe].
     */
    fun computeVonMisesDensity(hour: Double, peakHour: Double, kappa: Double = DEFAULT_KAPPA): Double {
        return computeVonMisesDensitySafe(hour, peakHour, kappa)
    }

    /**
     * Computes 24 hourly multipliers [0..23] using the Von Mises circular smoother with peak normalized to 1.0.
     */
    fun computeVonMisesHourlyMultipliers(peakHour: Double, kappa: Double = DEFAULT_KAPPA): List<Double> {
        return (0 until 24).map { h ->
            val angle = 2.0 * Math.PI * (h - peakHour) / 24.0
            exp(kappa * (cos(angle) - 1.0))
        }
    }

    fun computeMorningLarkProbability(peakHour: Double): Double {
        val normalizedHour = ((peakHour % 24.0) + 24.0) % 24.0
        return when {
            normalizedHour in 4.0..11.0 -> 0.85
            normalizedHour in 11.0..14.0 -> 0.60
            normalizedHour in 14.0..17.0 -> 0.35
            normalizedHour in 17.0..22.0 -> 0.15
            else -> 0.10
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
            sampleCount = 0.0,
            hourlyMultipliers = computeVonMisesHourlyMultipliers(initialPeakHour, kappa)
        )
        for (obs in observations) {
            profile = updateProfile(profile, obs, kappa)
        }
        return profile
    }
}

/**
 * Exogenous stimulant & ADHD medication pharmacokinetics (Bateman two-compartment model).
 */
enum class StimulantType(
    val absorptionRateKaPerHour: Double,
    val halfLifeHours: Double,
    val maxAdenosineMaskingFactor: Double,
    val crashSeverityFactor: Double,
    val referenceDoseMg: Double = 100.0
) {
    CAFFEINE(
        absorptionRateKaPerHour = 2.77, // Peak plasma ~45-50 mins
        halfLifeHours = 4.5,
        maxAdenosineMaskingFactor = 0.28,
        crashSeverityFactor = 0.08,
        referenceDoseMg = 100.0
    ),
    GREEN_TEA_L_THEANINE(
        absorptionRateKaPerHour = 2.10, // Smoother onset, minimal crash
        halfLifeHours = 4.0,
        maxAdenosineMaskingFactor = 0.18,
        crashSeverityFactor = 0.01,
        referenceDoseMg = 50.0
    ),
    PRESCRIPTION_STIMULANT_IR(
        absorptionRateKaPerHour = 2.20,
        halfLifeHours = 3.5,
        maxAdenosineMaskingFactor = 0.38,
        crashSeverityFactor = 0.12,
        referenceDoseMg = 10.0
    ),
    PRESCRIPTION_STIMULANT_XR(
        absorptionRateKaPerHour = 0.85,
        halfLifeHours = 6.5,
        maxAdenosineMaskingFactor = 0.35,
        crashSeverityFactor = 0.06,
        referenceDoseMg = 20.0
    )
}

data class StimulantDoseBout(
    val timestampMillis: Long,
    val doseMg: Double = 100.0, // Normalized to 100mg caffeine equivalent
    val type: StimulantType = StimulantType.CAFFEINE
)

object PharmacokineticStimulantCalculator {
    private const val REFERENCE_DOSE_MG = 100.0

    data class StimulantEffect(
        val processSMaskingReduction: Double, // [0.0 .. 0.40] reduction in perceived Process S
        val acuteAlertnessBoostPoints: Double, // [-10.0 .. +15.0] points on 0-100 scale
        val activeEquivalentMg: Double         // Remaining active mg in plasma (normalized to caffeine equivalence)
    )

    fun evaluateAt(
        timestampMillis: Long,
        doses: List<StimulantDoseBout>
    ): StimulantEffect {
        if (doses.isEmpty()) return StimulantEffect(0.0, 0.0, 0.0)

        var totalMasking = 0.0
        var netPoints = 0.0
        var activeMg = 0.0

        for (dose in doses) {
            if (timestampMillis <= dose.timestampMillis) continue
            val elapsedHours = (timestampMillis - dose.timestampMillis) / 3_600_000.0
            if (elapsedHours > 24.0) continue

            val ka = dose.type.absorptionRateKaPerHour
            val ke = ln(2.0) / dose.type.halfLifeHours
            // Bug 1 & Feature 1 (ADR-027): Dose ratio relative to compound-specific reference dose
            val doseRatio = (dose.doseMg / dose.type.referenceDoseMg).coerceIn(0.1, 4.0)

            val tMax = ln(ka / ke) / (ka - ke)
            val peakNorm = (ka / (ka - ke)) * (exp(-ke * tMax) - exp(-ka * tMax))
            val rawCurve = (ka / (ka - ke)) * (exp(-ke * elapsedHours) - exp(-ka * elapsedHours))
            val normalizedConc = (rawCurve / peakNorm.coerceAtLeast(1e-6)).coerceIn(0.0, 1.0)

            // Normalize active milligrams to caffeine-equivalent scale (100mg baseline)
            val doseEquivalent = (dose.doseMg / dose.type.referenceDoseMg) * REFERENCE_DOSE_MG
            activeMg += doseEquivalent * exp(-ke * elapsedHours)
            totalMasking += dose.type.maxAdenosineMaskingFactor * normalizedConc * min(1.8, doseRatio)

            // Post-elimination adenosine rebound ("crash") between t_half and t_half + 4h
            val reboundWindow = if (elapsedHours in (dose.type.halfLifeHours)..(dose.type.halfLifeHours + 4.0)) {
                val progress = (elapsedHours - dose.type.halfLifeHours) / 2.0
                val rawRebound = -100.0 * dose.type.crashSeverityFactor * progress * exp(1.0 - progress) * min(1.5, doseRatio)
                if (elapsedHours > dose.type.halfLifeHours + 3.0) {
                    val taper = 0.5 * (1.0 + cos(PI * (elapsedHours - (dose.type.halfLifeHours + 3.0))))
                    rawRebound * taper
                } else {
                    rawRebound
                }
            } else {
                0.0
            }

            netPoints += (12.0 * normalizedConc * min(1.5, doseRatio)) + reboundWindow
        }

        return StimulantEffect(
            processSMaskingReduction = totalMasking.coerceIn(0.0, 0.40),
            acuteAlertnessBoostPoints = netPoints.coerceIn(-10.0, 15.0),
            activeEquivalentMg = activeMg
        )
    }

    /**
     * Predicts Slow-Wave Sleep (SWS) suppression factor [0.0 .. 0.35] at [plannedSleepMillis].
     */
    fun predictBedtimeSwsSuppression(
        plannedSleepMillis: Long,
        doses: List<StimulantDoseBout>
    ): Double {
        val bedtimeState = evaluateAt(plannedSleepMillis, doses)
        val cycleMidpointState = evaluateAt(plannedSleepMillis + 45 * 60_000L, doses)
        val peakSwsEquivalentMg = maxOf(bedtimeState.activeEquivalentMg, cycleMidpointState.activeEquivalentMg)
        return ((peakSwsEquivalentMg - 35.0) / 160.0).coerceIn(0.0, 0.35)
    }
}

/**
 * Social Jetlag & SCN Phase-Shift Detector.
 * Compares circular midpoint of sleep on workdays (MSW) vs. free days/weekends (MSF).
 */
object SocialJetlagCalculator {
    data class SocialJetlagReport(
        val midpointWorkdayHour: Double,
        val midpointFreeDayHour: Double,
        val socialJetlagHours: Double, // Signed difference MSF - MSW
        val mondayMorningPhaseLagHours: Double,
        val morningLightProtocolRecommended: Boolean
    )

    fun evaluateFromSleepWindows(
        sleepWindows: List<TimeSlotWindow>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SocialJetlagReport {
        if (sleepWindows.size < 3) {
            return SocialJetlagReport(3.5, 3.5, 0.0, 0.0, false)
        }

        // Bug 2 & Feature 2 (ADR-027): Coalesce split nocturnal bouts separated by <= 120m
        val sortedWindows = sleepWindows.filter { it.endMillis > it.startMillis }.sortedBy { it.startMillis }
        val coalesced = mutableListOf<TimeSlotWindow>()
        for (win in sortedWindows) {
            if (coalesced.isEmpty()) {
                coalesced.add(win)
            } else {
                val last = coalesced.last()
                if (win.startMillis <= last.endMillis + 120 * 60_000L) {
                    coalesced[coalesced.lastIndex] = TimeSlotWindow(last.startMillis, max(last.endMillis, win.endMillis))
                } else {
                    coalesced.add(win)
                }
            }
        }

        val workdayMidpoints = mutableListOf<Pair<Double, Double>>()
        val weekendMidpoints = mutableListOf<Pair<Double, Double>>()

        for (win in coalesced) {
            val durationHours = (win.endMillis - win.startMillis) / 3_600_000.0
            if (durationHours < 3.0) continue
            val midpointMillis = win.startMillis + (win.endMillis - win.startMillis) / 2
            val zdt = Instant.ofEpochMilli(midpointMillis).atZone(zoneId)
            val clockHour = zdt.hour + (zdt.minute / 60.0)
            // Bug 2 & Feature 2 (ADR-027): Under MCTQ, classify Free Day by wake-up day (win.endMillis), not midpoint
            val endZdt = Instant.ofEpochMilli(win.endMillis).atZone(zoneId)
            val isFreeDay = endZdt.dayOfWeek == DayOfWeek.SATURDAY || endZdt.dayOfWeek == DayOfWeek.SUNDAY
            if (isFreeDay) {
                weekendMidpoints.add(clockHour to 1.0)
            } else {
                workdayMidpoints.add(clockHour to 1.0)
            }
        }

        if (workdayMidpoints.isEmpty() || weekendMidpoints.isEmpty()) {
            return SocialJetlagReport(3.5, 3.5, 0.0, 0.0, false)
        }

        val msw = blendClockHoursCircular(workdayMidpoints)
        val msf = blendClockHoursCircular(weekendMidpoints)
        val sjl = circularHourDifference(msf, msw)

        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val dayFactor = when (nowZdt.dayOfWeek) {
            DayOfWeek.MONDAY -> 0.65
            DayOfWeek.TUESDAY -> 0.35
            else -> 0.0
        }
        val activeLag = if (abs(sjl) >= 1.25) sjl * dayFactor else 0.0

        return SocialJetlagReport(
            midpointWorkdayHour = msw,
            midpointFreeDayHour = msf,
            socialJetlagHours = sjl,
            mondayMorningPhaseLagHours = activeLag,
            morningLightProtocolRecommended = abs(sjl) >= 1.5
        )
    }
}

object PhysiologicalImpulseCalculator {
    /**
     * Computes the acute post-prandial meal dip at [minuteOfDay].
     * Produces a negative impulse peaking at ~60 minutes after meal start.
     */
    fun computeMealImpulse(
        minuteOfDay: Int,
        mealBlocks: List<CognitiveBlockWindow>,
        timestampMillis: Long? = null
    ): Double {
        if (mealBlocks.isEmpty()) return 0.0
        var minImpulse = 0.0
        for (meal in mealBlocks) {
            val deltaMinutes = if (timestampMillis != null && meal.explicitStartMillis != null) {
                ((timestampMillis - meal.explicitStartMillis) / 60_000L).toInt()
            } else {
                (minuteOfDay - meal.startMinuteOfDay + 1440) % 1440
            }
            if (deltaMinutes in 0..180) {
                val tFactor = deltaMinutes / 60.0
                val dip = -12.0 * tFactor * exp(1.0 - tFactor) * meal.intensity
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
     * Post-workout (within 120m after workout end): acute endorphin/BDNF/dopamine focus boost (+12.0 decaying with tau=45m).
     */
    fun computeExerciseImpulse(
        minuteOfDay: Int,
        workoutBlocks: List<CognitiveBlockWindow>,
        timestampMillis: Long? = null
    ): Double {
        if (workoutBlocks.isEmpty()) return 0.0
        var strongestImpulse = 0.0

        for (workout in workoutBlocks) {
            val (isDuring, deltaMinutes) = if (timestampMillis != null && workout.explicitStartMillis != null && workout.explicitEndMillis != null) {
                if (timestampMillis in workout.explicitStartMillis..workout.explicitEndMillis) {
                    true to 0
                } else if (timestampMillis > workout.explicitEndMillis) {
                    false to ((timestampMillis - workout.explicitEndMillis) / 60_000L).toInt()
                } else {
                    false to -1
                }
            } else {
                val start = workout.startMinuteOfDay
                val end = workout.endMinuteOfDay
                val isDuring = if (end >= start) {
                    minuteOfDay in start..end
                } else {
                    minuteOfDay >= start || minuteOfDay <= end
                }
                if (isDuring) {
                    true to 0
                } else {
                    val deltaMinutes = (minuteOfDay - end + 1440) % 1440
                    false to deltaMinutes
                }
            }

            val impulse = when {
                isDuring -> -8.0 * workout.intensity
                deltaMinutes in 0..120 -> {
                    // Feature 2 (ADR-026): C1-continuous autonomic recovery transition ramp over 15 minutes
                    val inWorkoutPenalty = -8.0 * workout.intensity
                    val targetBoost = 12.0 * exp(-deltaMinutes / 45.0) * workout.intensity
                    val wOnset = sin(PI * 0.5 * min(1.0, deltaMinutes / 15.0))
                    (1.0 - wOnset) * inWorkoutPenalty + wOnset * targetBoost
                }
                else -> 0.0
            }

            if (impulse < 0.0) {
                if (strongestImpulse >= 0.0 || impulse < strongestImpulse) {
                    strongestImpulse = impulse
                }
            } else if (impulse > 0.0) {
                if (strongestImpulse <= 0.0 || impulse > strongestImpulse) {
                    strongestImpulse = impulse
                }
            }
        }
        return strongestImpulse
    }

    /**
     * Uses C1-continuous meal smoothing + exercise impulse for live circadian timeline evaluation.
     */
    fun computeCombinedImpulse(
        minuteOfDay: Int,
        blocks: List<CognitiveBlockWindow>,
        timestampMillis: Long? = null
    ): Double {
        val mealBlocks = blocks.filter { it.category == CognitiveBlockCategory.MEAL }
        val workoutBlocks = blocks.filter { it.category == CognitiveBlockCategory.EXERCISE }
        return computeSmoothMealImpulse(minuteOfDay, mealBlocks, timestampMillis) +
            computeExerciseImpulse(minuteOfDay, workoutBlocks, timestampMillis)
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
     * Coalesces overlapping or duplicate nap intervals to prevent double-discharge of Process S.
     */
    fun coalesceNaps(naps: List<DaytimeNapBout>): List<DaytimeNapBout> {
        if (naps.size <= 1) return naps
        val sorted = naps.sortedBy { it.startMillis }
        val merged = mutableListOf<DaytimeNapBout>()
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startMillis <= current.endMillis) {
                current = DaytimeNapBout(current.startMillis, max(current.endMillis, next.endMillis))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

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
            val hoursBeforeWake = max(0.0, (morningWakeMillis - timestampMillis) / 3_600_000.0)
            val preWakeS = min(1.0, initialPressureS0 + (1.0 - initialPressureS0) * (1.0 - exp(-hoursBeforeWake / 2.0)))
            return NapAdjustedHomeostasis(preWakeS.coerceIn(0.0, 1.0), 0.14)
        }

        // Bug 4 & Feature 3 (ADR-025): Coalesce overlapping naps before running sequential Process S discharge
        val coalesced = coalesceNaps(naps)
        val validNaps = coalesced
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
            // Bug 4 Fix (ADR-025): Preserve maximum decaying sleep inertia across multiple bouts instead of zeroing out
            if (nap.durationMinutes > INERTIA_THRESHOLD_MINUTES) {
                val minsSinceNapWake = max(0.0, (timestampMillis - nap.endMillis) / 60_000.0)
                val depthFactor = min(1.0, (nap.durationMinutes - INERTIA_THRESHOLD_MINUTES) / 30.0)
                val boutInertia = MAX_INERTIA_PENALTY * depthFactor * exp(-minsSinceNapWake / INERTIA_DECAY_TAU_MINUTES)
                inertiaPenalty = max(inertiaPenalty, boutInertia)
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

    fun evaluateSnapshotWithStimulants(
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
        cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0
    ): CircadianSnapshot = evaluateSnapshot(
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
        rollingAverageSleepMinutes: Double? = null,
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0
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
        rollingAverageSleepMinutes = null,
        stimulantDoses = emptyList()
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
        rollingAverageSleepMinutes: Double? = null,
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0
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
    ): CircadianSnapshot = evaluateSnapshotWithStimulants(
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
        cognitiveBlocks = cognitiveBlocks,
        stimulantDoses = emptyList(),
        socialJetlagLagHours = 0.0
    )

    override fun evaluateSnapshotWithStimulants(
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
        cognitiveBlocks: List<CognitiveBlockWindow>,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double
    ): CircadianSnapshot {
        // 0. Multi-Day Allostatic Sleep Debt Carryover
        val allostaticDebtHours = if (rollingAverageSleepMinutes != null && rollingAverageSleepMinutes > 0.0) {
            max(0.0, (480.0 - rollingAverageSleepMinutes) / 60.0)
        } else {
            0.0
        }
        val alphaAllostatic = min(0.50, allostaticDebtHours / 8.0)

        // 1. Continuous Sleep Recovery -> Initial Wake Pressure S0
        // Feature 1 (ADR-024): Bedtime SWS Suppression -> Next-Day Process S0 Coupling
        val plannedBedtime = wakeTimeMillis - (lastSleepDurationMinutes * 60_000L)
        val swsSuppression = if (stimulantDoses.isNotEmpty()) {
            PharmacokineticStimulantCalculator.predictBedtimeSwsSuppression(plannedBedtime, stimulantDoses)
        } else {
            0.0
        }
        val effectiveSleepHours = max(0.0, (lastSleepDurationMinutes / 60.0) * (1.0 - swsSuppression))
        val baseS0 = S_MIN + (S_MAX - S_MIN) * exp(-effectiveSleepHours / TAU_RECOVERY_HOURS)
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

        // 2a. Apply Pharmacokinetic Adenosine Masking (if any stimulant doses logged)
        val stimEffect = PharmacokineticStimulantCalculator.evaluateAt(timestampMillis, stimulantDoses)
        val processS = (homeostatic.processS - stimEffect.processSMaskingReduction).coerceIn(S_MIN, 1.0)

        // 2b. Morning Sleep Inertia (first 30m after morning wake) + Nap Inertia
        val minsSinceMorningWake = ((timestampMillis - wakeTimeMillis) / 60_000.0)
        val morningInertia = if (minsSinceMorningWake < 0.0) {
            0.14
        } else if (minsSinceMorningWake <= 30.0) {
            0.14 * (1.0 - (minsSinceMorningWake / 30.0))
        } else {
            0.0
        }
        val inertiaPenalty = max(homeostatic.sleepInertiaPenalty, morningInertia)

        // 3. DST-Safe Wall-Clock Extraction for Process C(t)
        val zonedNow = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val clockHour = zonedNow.hour + (zonedNow.minute / 60.0)
        val wakeClockHour = zonedWake.hour + (zonedWake.minute / 60.0)

        // 3a. Pure Uncalibrated Biological Curve (for accurate baselineBiologicalEnergy comparison)
        val bioPeakPhi = computePhasePeakHour(
            wakeClockHour = wakeClockHour,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            detectedPeakHour = null,
            detectionConfidence = 0.0
        )
        val bioHarmonic1 = 0.38 * cos((2.0 * PI * (clockHour - bioPeakPhi)) / 24.0)
        val bioHarmonic2 = 0.12 * cos((4.0 * PI * (clockHour - bioPeakPhi)) / 24.0)
        val processCBio = (0.50 + bioHarmonic1 - bioHarmonic2).coerceIn(0.0, 1.0)

        // 3b. Learned + Social-Jetlag Adjusted Curve with Von Mises Hourly Kernel Modulation
        val detectedPeak = learnedProfile?.peakHourMean
        val detectedConf = learnedProfile?.confidence ?: 0.0
        val phasePeakPhi = (computePhasePeakHour(
            wakeClockHour = wakeClockHour,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            detectedPeakHour = detectedPeak,
            detectionConfidence = detectedConf
        ) + socialJetlagLagHours.coerceIn(-3.0, 3.0) + 24.0) % 24.0

        val primaryHarmonic = 0.38 * cos((2.0 * PI * (clockHour - phasePeakPhi)) / 24.0)
        val secondaryHarmonic = 0.12 * cos((4.0 * PI * (clockHour - phasePeakPhi)) / 24.0)
        val harmonicC = (0.50 + primaryHarmonic - secondaryHarmonic).coerceIn(0.0, 1.0)
        val processC = applyLearnedVonMisesModulation(harmonicC, clockHour, learnedProfile)

        // 4. Synaptic Depletion from unbroken focus > 45m
        val excessFocusMins = max(0, continuousActiveFocusMinutes - 45).toDouble()
        val focusDepletion = if (excessFocusMins > 0.0) {
            MAX_FOCUS_DEPLETION * (1.0 - exp(-excessFocusMins / 60.0))
        } else {
            0.0
        }

        // 4b. Physiological Impulses (Meals & Exercise) + Stimulant Acute Alertness
        val minuteOfDay = zonedNow.hour * 60 + zonedNow.minute
        val physioImpulse = (if (cognitiveBlocks.isNotEmpty()) {
            PhysiologicalImpulseCalculator.computeCombinedImpulse(minuteOfDay, cognitiveBlocks, timestampMillis)
        } else {
            0.0
        }) + stimEffect.acuteAlertnessBoostPoints

        // 5. Unified Usable Energy [0..100]
        val baseRawEnergy = 100.0 * (0.55 * processCBio + 0.45 * (1.0 - homeostatic.processS) - inertiaPenalty)
        val baselineBio = baseRawEnergy.roundToInt().coerceIn(0, 100)
        val rawEnergy = 100.0 * (0.55 * processC + 0.45 * (1.0 - processS) - focusDepletion - inertiaPenalty) + physioImpulse
        val usableEnergy = rawEnergy.roundToInt().coerceIn(0, 100)
        val learnedAdj = (usableEnergy - baselineBio).toDouble()
        val predConf = learnedProfile?.confidence ?: 0.85

        val hoursAwake = max(0.0, (timestampMillis - wakeTimeMillis) / 3_600_000.0)
        // Bug 5 Fix (ADR-026): Morning ramp-up phase guard
        // Prevents morning hours (hoursAwake < 6.0) from falling into WIND_DOWN
        val phase = when {
            continuousActiveFocusMinutes >= 90 -> EnergyPhase.RECOVERY_NEEDED
            usableEnergy >= 72 -> EnergyPhase.PEAK
            hoursAwake in 6.0..9.5 && processC < 0.48 -> EnergyPhase.DIP
            hoursAwake > 9.5 && usableEnergy >= 55 -> EnergyPhase.REBOUND
            hoursAwake < 6.0 && usableEnergy >= 50 -> EnergyPhase.REBOUND
            hoursAwake < 6.0 -> EnergyPhase.RECOVERY_NEEDED
            usableEnergy >= 55 -> EnergyPhase.REBOUND
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
        rollingAverageSleepMinutes: Double?,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double
    ): List<CircadianSnapshot> {
        val stepMillis = stepMinutes * 60_000L
        val steps = (24 * 60) / stepMinutes
        return List(steps) { idx ->
            val t = dayStartMillis + idx * stepMillis
            if (stimulantDoses.isNotEmpty() || socialJetlagLagHours != 0.0) {
                evaluateSnapshotWithStimulants(
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
                    cognitiveBlocks = cognitiveBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
                )
            } else {
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
        rollingAverageSleepMinutes = null,
        stimulantDoses = emptyList()
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
        rollingAverageSleepMinutes: Double?,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double
    ): DailyCircadianSummary {
        val currentSnapshot = if (stimulantDoses.isNotEmpty() || socialJetlagLagHours != 0.0) {
            evaluateSnapshotWithStimulants(
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
                cognitiveBlocks = cognitiveBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = socialJetlagLagHours
            )
        } else {
            evaluateSnapshot(
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
        }

        val zonedTimestamp = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
        val targetDate = zonedTimestamp.toLocalDate()
        val dayStartMillis = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)

        // Bug 2 Fix: Target date wake time resolved safely across DST transitions
        val targetDateWakeMillis = targetDate.atTime(zonedWake.toLocalTime()).atZone(zoneId).toInstant().toEpochMilli()

        // Preserve wakeTimeMillis directly when user is awake from prior day (post-midnight / night-owl)
        val isPostMidnightAwake = wakeTimeMillis <= timestampMillis &&
            (timestampMillis - wakeTimeMillis) < 24 * 3600_000L &&
            zonedWake.toLocalDate() < targetDate

        val timeline = projectDayTimeline(
            dayStartMillis = dayStartMillis,
            wakeTimeMillis = targetDateWakeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            stepMinutes = 30,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            cognitiveBlocks = cognitiveBlocks,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            stimulantDoses = stimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours
        ).let { baseList ->
            if (isPostMidnightAwake) {
                baseList.map { step ->
                    if (step.timestampMillis < targetDateWakeMillis) {
                        if (stimulantDoses.isNotEmpty() || socialJetlagLagHours != 0.0) {
                            evaluateSnapshotWithStimulants(
                                timestampMillis = step.timestampMillis,
                                wakeTimeMillis = wakeTimeMillis,
                                lastSleepDurationMinutes = lastSleepDurationMinutes,
                                continuousActiveFocusMinutes = 0,
                                zoneId = zoneId,
                                naps = naps,
                                chronotype = chronotype,
                                manualPeakStartHour = manualPeakStartHour,
                                cognitiveBlocks = cognitiveBlocks,
                                learnedProfile = learnedProfile,
                                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                                stimulantDoses = stimulantDoses,
                                socialJetlagLagHours = socialJetlagLagHours
                            )
                        } else {
                            evaluateSnapshot(
                                timestampMillis = step.timestampMillis,
                                wakeTimeMillis = wakeTimeMillis,
                                lastSleepDurationMinutes = lastSleepDurationMinutes,
                                continuousActiveFocusMinutes = 0,
                                zoneId = zoneId,
                                naps = naps,
                                chronotype = chronotype,
                                manualPeakStartHour = manualPeakStartHour,
                                cognitiveBlocks = cognitiveBlocks,
                                learnedProfile = learnedProfile,
                                rollingAverageSleepMinutes = rollingAverageSleepMinutes
                            )
                        }
                    } else {
                        step
                    }
                }
            } else {
                baseList
            }
        }

        // Feature 1 (ADR-026): Sliding-Window Integral Peak & Dip Detector
        // 1. Peak Window: 3-hour sliding window maximizing sum of usableEnergy
        val stepMinutes = 30
        val peakSteps = (3 * 60) / stepMinutes // 6 steps
        var bestPeakSum = -1.0
        var bestPeakStartIdx = 0

        if (timeline.size >= peakSteps) {
            for (i in 0..(timeline.size - peakSteps)) {
                val windowSum = (0 until peakSteps).sumOf { timeline[i + it].usableEnergy.toDouble() }
                if (windowSum > bestPeakSum) {
                    bestPeakSum = windowSum
                    bestPeakStartIdx = i
                }
            }
        }

        val peakSnapshot = timeline.getOrNull(bestPeakStartIdx) ?: currentSnapshot
        val peakHour = Instant.ofEpochMilli(peakSnapshot.timestampMillis).atZone(zoneId).hour
        val peakEndHour = (peakHour + 3) % 24
        val peakEnergyScore = (0 until peakSteps)
            .mapNotNull { timeline.getOrNull(bestPeakStartIdx + it)?.usableEnergy }
            .maxOrNull() ?: peakSnapshot.usableEnergy

        // 2. Dip Window: 2-hour sliding window minimizing sum of usableEnergy within wake+4h..wake+11h
        val dipSteps = (2 * 60) / stepMinutes // 4 steps
        val dipStartSearchMillis = targetDateWakeMillis + 4 * 3600_000L
        val dipEndSearchMillis = targetDateWakeMillis + 11 * 3600_000L

        var bestDipSum = Double.MAX_VALUE
        var bestDipStartIdx = -1

        if (timeline.size >= dipSteps) {
            for (i in 0..(timeline.size - dipSteps)) {
                val stepTime = timeline[i].timestampMillis
                if (stepTime in dipStartSearchMillis..dipEndSearchMillis) {
                    val windowSum = (0 until dipSteps).sumOf { timeline[i + it].usableEnergy.toDouble() }
                    if (windowSum < bestDipSum) {
                        bestDipSum = windowSum
                        bestDipStartIdx = i
                    }
                }
            }

            // Fallback if search window yielded no candidate
            if (bestDipStartIdx == -1) {
                for (i in 0..(timeline.size - dipSteps)) {
                    val windowSum = (0 until dipSteps).sumOf { timeline[i + it].usableEnergy.toDouble() }
                    if (windowSum < bestDipSum) {
                        bestDipSum = windowSum
                        bestDipStartIdx = i
                    }
                }
            }
        }

        val dipSnapshot = if (bestDipStartIdx >= 0) timeline.getOrNull(bestDipStartIdx) ?: currentSnapshot else currentSnapshot
        val dipHour = Instant.ofEpochMilli(dipSnapshot.timestampMillis).atZone(zoneId).hour
        val dipEndHour = (dipHour + 2) % 24

        val highEnergySteps = timeline.count { it.usableEnergy >= 70 }
        val recommendedDeepWorkMinutes = (highEnergySteps * stepMinutes).coerceIn(60, 240)

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
            TaskCategory.DEEP_WORK -> e
            TaskCategory.CREATIVE -> {
                if (snapshot.phase == EnergyPhase.REBOUND) min(100.0, e + 15.0) else e * 0.9
            }
            TaskCategory.ADMIN -> {
                100.0 * exp(-((e - 50.0) * (e - 50.0)) / (2.0 * 22.0 * 22.0))
            }
            TaskCategory.EXERCISE -> {
                100.0 * exp(-((e - 65.0) * (e - 65.0)) / (2.0 * 25.0 * 25.0))
            }
            TaskCategory.MINDFULNESS -> {
                if (snapshot.phase == EnergyPhase.RECOVERY_NEEDED) 100.0 else (100.0 - e * 0.5).coerceIn(30.0, 95.0)
            }
            TaskCategory.GENERAL -> 70.0
        }.coerceIn(0.0, 100.0)
    }
}
