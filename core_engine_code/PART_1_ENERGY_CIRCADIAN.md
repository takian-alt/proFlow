# neuroFlow Core Engine — Part 1: Energy & Circadian Biophysics

**Description:** Continuous Borbély Two-Process Model (S(t) & C(t)), Fast Circadian Kalman Learner + Von Mises smoother, Polyphasic Nap Biophysics, Physiological Impulses (meals & workouts), Chronobiology Signal Normalizer, and Wearable Sleep Telemetry.  
**Generated for Vice Head Review:** 2026-10-23 22:05:10  
**Files:** 14  
**Lines of Code:** 4,067  
**Uncompressed Size:** 163.9 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt`](#appsrcmainjavacomneuroflowappdomainservicecircadianenergyservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizer.kt`](#appsrcmainjavacomneuroflowappdomainservicechronobiologysignalnormalizerkt)
- [`app/src/main/java/com/neuroflow/app/domain/model/EnergyUiModel.kt`](#appsrcmainjavacomneuroflowappdomainmodelenergyuimodelkt)
- [`app/src/main/java/com/neuroflow/app/domain/model/CircadianEnergySummary.kt`](#appsrcmainjavacomneuroflowappdomainmodelcircadianenergysummarykt)
- [`app/src/main/java/com/neuroflow/app/domain/model/FatigueZone.kt`](#appsrcmainjavacomneuroflowappdomainmodelfatiguezonekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/MEQChronotypeDetector.kt`](#appsrcmainjavacomneuroflowappdomainenginemeqchronotypedetectorkt)
- [`app/src/main/java/com/neuroflow/app/domain/repository/SleepPressureRepository.kt`](#appsrcmainjavacomneuroflowappdomainrepositorysleeppressurerepositorykt)
- [`app/src/main/java/com/neuroflow/app/domain/repository/PassiveSleepSyncRepository.kt`](#appsrcmainjavacomneuroflowappdomainrepositorypassivesleepsyncrepositorykt)
- [`app/src/main/java/com/neuroflow/app/domain/repository/EnergyMetricsRepository.kt`](#appsrcmainjavacomneuroflowappdomainrepositoryenergymetricsrepositorykt)
- [`app/src/main/java/com/neuroflow/app/data/sleep/SleepTelemetryDataSource.kt`](#appsrcmainjavacomneuroflowappdatasleepsleeptelemetrydatasourcekt)
- [`app/src/main/java/com/neuroflow/app/data/repository/SleepLogRepository.kt`](#appsrcmainjavacomneuroflowappdatarepositorysleeplogrepositorykt)
- [`app/src/main/java/com/neuroflow/app/data/repository/PassiveSleepSyncManager.kt`](#appsrcmainjavacomneuroflowappdatarepositorypassivesleepsyncmanagerkt)
- [`app/src/main/java/com/neuroflow/app/worker/SleepPressureRefreshWorker.kt`](#appsrcmainjavacomneuroflowappworkersleeppressurerefreshworkerkt)
- [`app/src/main/java/com/neuroflow/app/worker/EnergyTelemetryCleanupWorker.kt`](#appsrcmainjavacomneuroflowappworkerenergytelemetrycleanupworkerkt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowappdomainservicecircadianenergyservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt`
- **Lines of Code:** 1,651
- **Size:** 67,819 bytes (66.2 KB)

```kotlin
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
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicechronobiologysignalnormalizerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizer.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizer.kt`
- **Lines of Code:** 274
- **Size:** 11,276 bytes (11.0 KB)

```kotlin
package com.neuroflow.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class RawSleepTelemetry(
    val intervals: List<TimeSlotWindow>,
    val deepSleepMinutes: Double = 0.0,
    val remSleepMinutes: Double = 0.0,
    val lightSleepMinutes: Double = 0.0,
    val detectedWakeMillis: Long? = null,
    val confidence: Double = 1.0
)

data class NormalizedChronobiologySignal(
    val totalSleepMinutes: Double,
    val effectiveDurationMinutes: Double,
    val blendedDurationMinutes: Double,
    val normalizedWakeTimeMillis: Long,
    val totalWasoMinutes: Double,
    val consolidatedSessions: List<TimeSlotWindow>,
    val daytimeNaps: List<DaytimeNapBout> = emptyList()
)

/**
 * ADR-002 Chronobiology Signal Normalizer
 *
 * Implements passive chronobiology signal normalization for wearable telemetry:
 * 1. 45-minute WASO episodic coalescing (bridges nighttime micro-awakenings into unified sleep sessions).
 * 2. Sleep stage efficiency weighting (d_eff) prioritizing Slow-Wave Sleep (SWS) and REM.
 * 3. Bayesian duration blending (d_blended) across confidence spectrum c in [0.0..1.0].
 * 4. Late-sync wake shift clamping (+-90m) to eliminate ADHD schedule whiplash.
 */
@Singleton
class ChronobiologySignalNormalizer @Inject constructor() {

    companion object {
        const val MAX_WASO_GAP_MILLIS = 45 * 60_000L
        const val MAX_WAKE_SHIFT_MILLIS = 90 * 60_000L

        private const val SWS_REM_MIN_FACTOR = 0.75
        private const val SWS_REM_MAX_FACTOR = 1.25
        private const val BASE_ARCHITECTURE_FACTOR = 0.85
        private const val RESTORATIVE_SLOPE = 0.75
    }

    /**
     * Coalesce sleep intervals separated by wake episodes (WASO) <= 45 minutes.
     * Returns a pair of (consolidated non-overlapping sleep sessions, total WASO minutes inside coalesced gaps).
     */
    fun coalesceWasoEpisodes(
        intervals: List<TimeSlotWindow>,
        maxWasoGapMillis: Long = MAX_WASO_GAP_MILLIS
    ): Pair<List<TimeSlotWindow>, Long> {
        val valid = intervals.filter { it.endMillis > it.startMillis }
        if (valid.isEmpty()) return emptyList<TimeSlotWindow>() to 0L

        val sorted = valid.sortedBy { it.startMillis }
        val consolidated = mutableListOf<TimeSlotWindow>()
        var totalWasoMillis = 0L

        var curStart = sorted[0].startMillis
        var curEnd = sorted[0].endMillis

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startMillis <= curEnd) {
                // Direct overlap
                curEnd = max(curEnd, next.endMillis)
            } else {
                val gap = next.startMillis - curEnd
                if (gap <= maxWasoGapMillis) {
                    // WASO awakening: bridge the episodes into a single session
                    totalWasoMillis += gap
                    curEnd = max(curEnd, next.endMillis)
                } else {
                    consolidated.add(TimeSlotWindow(curStart, curEnd))
                    curStart = next.startMillis
                    curEnd = next.endMillis
                }
            }
        }
        consolidated.add(TimeSlotWindow(curStart, curEnd))

        val totalWasoMinutes = totalWasoMillis / 60_000L
        return consolidated to totalWasoMinutes
    }

    /**
     * Stage Architecture Restorative Hours:
     * d_eff = d_total * clamp_{[0.75, 1.25]}(0.85 + 0.75 * (d_SWS + d_REM) / d_total)
     */
    fun computeEffectiveDuration(
        totalMinutes: Double,
        deepSleepMinutes: Double,
        remSleepMinutes: Double,
        lightSleepMinutes: Double? = null
    ): Double {
        if (totalMinutes <= 0.0) return 0.0
        // Bug 1 Fix (ADR-026): When no sleep-stage breakdown is present (manual log or tracker lacking SWS/REM breakdown),
        // assume normative stage architecture (stageWeight = 1.0) instead of penalizing by 15%.
        if (lightSleepMinutes != null && deepSleepMinutes <= 0.0 && remSleepMinutes <= 0.0 && lightSleepMinutes <= 0.0) {
            return totalMinutes
        }
        val restorativeRatio = ((deepSleepMinutes + remSleepMinutes) / totalMinutes).coerceIn(0.0, 1.0)
        val stageWeight = (BASE_ARCHITECTURE_FACTOR + RESTORATIVE_SLOPE * restorativeRatio)
            .coerceIn(SWS_REM_MIN_FACTOR, SWS_REM_MAX_FACTOR)
        return totalMinutes * stageWeight
    }

    /**
     * Bayesian Duration Blending:
     * d_blended = c * d_eff + (1 - c) * d_base
     */
    fun blendDuration(
        effectiveDurationMinutes: Double,
        baseDurationMinutes: Double,
        confidence: Double
    ): Double {
        val c = confidence.coerceIn(0.0, 1.0)
        return c * effectiveDurationMinutes + (1.0 - c) * baseDurationMinutes
    }

    /**
     * Anti-Whiplash Wake-Time Clamping:
     * t_wake* = t_wake_base + clamp_{[-90m, +90m]}(c * (t_wake_wearable - t_wake_base))
     */
    fun clampWakeShift(
        baseWakeMillis: Long,
        wearableWakeMillis: Long,
        confidence: Double,
        maxShiftMillis: Long = MAX_WAKE_SHIFT_MILLIS
    ): Long {
        val c = confidence.coerceIn(0.0, 1.0)
        val rawShift = (wearableWakeMillis - baseWakeMillis).toDouble() * c
        val clampedShift = rawShift.coerceIn(-maxShiftMillis.toDouble(), maxShiftMillis.toDouble())
        return baseWakeMillis + clampedShift.roundToLong()
    }

    /**
     * Extracts secondary daytime sleep bouts (10m <= d <= 180m) occurring after the morning wake anchor.
     * Merges micro-arousals (<= 5m) without merging separate daytime rest bouts via nocturnal 45m WASO coalescing.
     */
    fun extractDaytimeNaps(
        intervals: List<TimeSlotWindow>,
        effectiveWakeTimeMillis: Long
    ): List<DaytimeNapBout> {
        val daytimeWindows = intervals
            .filter { it.startMillis >= effectiveWakeTimeMillis && it.endMillis > it.startMillis }
            .sortedBy { it.startMillis }
        if (daytimeWindows.isEmpty()) return emptyList()

        val merged = mutableListOf<TimeSlotWindow>()
        var curStart = daytimeWindows[0].startMillis
        var curEnd = daytimeWindows[0].endMillis
        for (i in 1 until daytimeWindows.size) {
            val next = daytimeWindows[i]
            if (next.startMillis <= curEnd + 5 * 60_000L) {
                curEnd = kotlin.math.max(curEnd, next.endMillis)
            } else {
                merged.add(TimeSlotWindow(curStart, curEnd))
                curStart = next.startMillis
                curEnd = next.endMillis
            }
        }
        merged.add(TimeSlotWindow(curStart, curEnd))

        return merged
            .map { DaytimeNapBout(it.startMillis, it.endMillis) }
            .filter { it.durationMinutes in 10..180 }
            .sortedBy { it.startMillis }
    }

    /**
     * Full normalization pipeline aggregating telemetry into normalized signal.
     */
    fun normalize(
        telemetry: RawSleepTelemetry,
        baseWakeMillis: Long,
        baseSleepMinutes: Double
    ): NormalizedChronobiologySignal {
        val (consolidated, wasoMinutes) = coalesceWasoEpisodes(telemetry.intervals)

        // Identify primary nocturnal sleep vs secondary daytime naps
        // Bug 7 Fix: Restrict nocturnalSessions to intervals overlapping the nocturnal window [baseWakeMillis - 16h, baseWakeMillis + 4h]
        val nocturnalWindowStart = baseWakeMillis - 16 * 3600_000L
        val nocturnalWindowEnd = baseWakeMillis + 4 * 3600_000L
        val nocturnalSessions = consolidated.filter {
            val overlapsNocturnalWindow = it.startMillis < nocturnalWindowEnd && it.endMillis > nocturnalWindowStart
            val isConsolidatedOrWakeAligned = it.endMillis >= baseWakeMillis - 6 * 3600_000L || (it.endMillis - it.startMillis) > 180 * 60_000L
            overlapsNocturnalWindow && isConsolidatedOrWakeAligned
        }

        val nocturnalWake = nocturnalSessions.maxOfOrNull { it.endMillis }
        val wearableWake = when {
            nocturnalWake != null -> {
                if (telemetry.detectedWakeMillis != null &&
                    telemetry.detectedWakeMillis <= nocturnalWake + MAX_WASO_GAP_MILLIS &&
                    telemetry.detectedWakeMillis >= nocturnalWake - MAX_WASO_GAP_MILLIS
                ) {
                    telemetry.detectedWakeMillis
                } else {
                    nocturnalWake
                }
            }
            telemetry.detectedWakeMillis != null &&
                kotlin.math.abs(telemetry.detectedWakeMillis - baseWakeMillis) <= MAX_WAKE_SHIFT_MILLIS &&
                consolidated.none { it.endMillis == telemetry.detectedWakeMillis } -> {
                telemetry.detectedWakeMillis
            }
            else -> baseWakeMillis
        }

        val normalizedWake = clampWakeShift(
            baseWakeMillis = baseWakeMillis,
            wearableWakeMillis = wearableWake,
            confidence = telemetry.confidence
        )

        // Extract secondary daytime sleep bouts (10m <= d <= 180m) after morning wake
        val daytimeNaps = extractDaytimeNaps(telemetry.intervals, nocturnalWake ?: normalizedWake)

        val napIntervalWindows = daytimeNaps.map { TimeSlotWindow(it.startMillis, it.endMillis) }
        val primarySessions = if (nocturnalSessions.isNotEmpty()) {
            consolidated.filter { s ->
                s in nocturnalSessions &&
                    napIntervalWindows.none { it.startMillis == s.startMillis && it.endMillis == s.endMillis }
            }
        } else {
            emptyList()
        }

        var effectiveWasoMinutes = wasoMinutes
        val rawSleepMinutes = if (primarySessions.isNotEmpty()) {
            val (primCoalesced, primWaso) = coalesceWasoEpisodes(
                telemetry.intervals.filter { interval ->
                    primarySessions.any { p -> interval.startMillis >= p.startMillis && interval.endMillis <= p.endMillis }
                }
            )
            effectiveWasoMinutes = primWaso
            (primCoalesced.sumOf { (it.endMillis - it.startMillis) / 60_000.0 } - primWaso).coerceAtLeast(0.0)
        } else if (telemetry.intervals.isEmpty()) {
            baseSleepMinutes
        } else {
            baseSleepMinutes
        }.coerceAtLeast(0.0)

        val effectiveDuration = computeEffectiveDuration(
            totalMinutes = rawSleepMinutes,
            deepSleepMinutes = telemetry.deepSleepMinutes,
            remSleepMinutes = telemetry.remSleepMinutes,
            lightSleepMinutes = telemetry.lightSleepMinutes
        )

        val blendedDuration = blendDuration(
            effectiveDurationMinutes = effectiveDuration,
            baseDurationMinutes = baseSleepMinutes,
            confidence = telemetry.confidence
        )

        return NormalizedChronobiologySignal(
            totalSleepMinutes = rawSleepMinutes,
            effectiveDurationMinutes = effectiveDuration,
            blendedDurationMinutes = blendedDuration,
            normalizedWakeTimeMillis = normalizedWake,
            totalWasoMinutes = effectiveWasoMinutes.toDouble(),
            consolidatedSessions = consolidated,
            daytimeNaps = daytimeNaps
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelenergyuimodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/EnergyUiModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/EnergyUiModel.kt`
- **Lines of Code:** 636
- **Size:** 29,296 bytes (28.6 KB)

```kotlin
package com.neuroflow.app.domain.model

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralObservation
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PeakWindow(
    val startMinuteOffset: Int = 0,
    val durationMinutes: Int = 180,
    val amplitude: Float = 1.0f
)

data class CircadianProfile(
    val windows: List<PeakWindow> = emptyList(),
    val phaseShiftMinutes: Int = 0
)

enum class ProfileType {
    WORKDAY,
    WEEKEND
}

data class ConfidenceComponents(
    val sleepCoverage: Float = 0.8f,
    val wakeConsistency: Float = 0.8f,
    val behaviorPerformance: Float = 0.8f,
    val overall: Float = 0.8f
)

data class EffectivePeakProfile(
    val profileType: ProfileType = ProfileType.WORKDAY,
    val anchorMinuteOfDay: Int = 150,
    val windows: List<PeakWindow> = emptyList(),
    val confidence: ConfidenceComponents = ConfidenceComponents(),
    val windowConfidences: List<Float> = emptyList(),
    val explanation: String = "",
    val driftStatus: String = "stable",
    val weeklyBacktestErrorMinutes: Float? = null,
    val confidenceGatedAbstention: Boolean = false,
    val adaptiveFreezeMode: Boolean = false,
    val abstentionReason: String = ""
)

data class EnergyUiModel(
    val availableEnergy: Int = 50,
    val rawEnergy: Float = 50f,
    val baselineRawEnergy: Float = 50f,
    val peakScore: Float = 50f,
    val fatiguePenalty: Float = 0f,
    val momentAdjustment: Float = 0f,
    val momentConfidence: Float = 1f,
    val currentPeakValue: Int = 4000,
    val peakValue: Int = 4000,
    val peakDrop: Int = 0,
    val minutesSincePeak: Int = 0,
    val minutesUntilPeakReset: Int = 0,
    val sleepPressurePoints: Int = 0,
    val fatiguePercent: Int = 0,
    val fatigueZone: FatigueZone = FatigueZone.RESTED,
    val circadianFactor: Float = 1f,
    val reservoirFactor: Float = 1f,
    val confidenceFactor: Float = 1f,
    val effectivePeakProfile: EffectivePeakProfile? = null,
    val momentSignalSummary: String = "",
    val peakDetectionAgeMillis: Long = 0L,
    val sessionDataAgeMillis: Long = 0L,
    val sleepLogDataAgeMillis: Long = 0L,
    val taskDataAgeMillis: Long = 0L,
    val overallFreshnessAgeMillis: Long = 0L,
    val hasRecentData: Boolean = true,
    val refreshedAtMillis: Long = System.currentTimeMillis()
)

fun buildEnergyUiModel(
    usableEnergy: Int,
    circadianFactor: Float = 0.85f,
    sleepPressureFactor: Float = 0.25f,
    sleepPressurePoints: Int = 0,
    fatigueZone: FatigueZone? = null,
    refreshedAtMillis: Long = System.currentTimeMillis(),
    baselineBiologicalEnergy: Int = usableEnergy,
    physiologicalImpulse: Double = 0.0,
    learnedAdjustmentPoints: Double = 0.0,
    predictionConfidence: Double = 1.0,
    effectivePeakProfile: EffectivePeakProfile? = null
): EnergyUiModel {
    val usable = usableEnergy.coerceIn(0, 100)
    val fatiguePct = (sleepPressureFactor * 100).toInt().coerceIn(0, 100)
    val effectivePressurePoints = if (sleepPressurePoints > 0) {
        sleepPressurePoints
    } else {
        (sleepPressureFactor.coerceIn(0f, 1f) * FatigueZone.SOFT_MAX_REFERENCE).roundToInt()
    }
    val resolvedZone = fatigueZone ?: if (sleepPressurePoints > 0) {
        FatigueZone.fromPressurePoints(sleepPressurePoints)
    } else {
        when {
            fatiguePct <= 24 -> FatigueZone.RESTED
            fatiguePct <= 49 -> FatigueZone.MODERATE
            fatiguePct <= 74 -> FatigueZone.HIGH
            else -> FatigueZone.CRITICAL
        }
    }
    val peakVal = 4000
    val curPeakVal = (usable * 40).coerceIn(0, 4000)
    return EnergyUiModel(
        availableEnergy = usable,
        rawEnergy = usable.toFloat(),
        baselineRawEnergy = baselineBiologicalEnergy.toFloat(),
        peakScore = usable.toFloat(),
        fatiguePenalty = (sleepPressureFactor * 100f).coerceIn(0f, 100f),
        momentAdjustment = (physiologicalImpulse + learnedAdjustmentPoints).toFloat(),
        momentConfidence = predictionConfidence.toFloat(),
        currentPeakValue = curPeakVal,
        peakValue = peakVal,
        peakDrop = maxOf(0, peakVal - curPeakVal),
        minutesSincePeak = 0,
        minutesUntilPeakReset = 0,
        sleepPressurePoints = effectivePressurePoints,
        fatiguePercent = fatiguePct,
        fatigueZone = resolvedZone,
        circadianFactor = circadianFactor.coerceIn(0f, 1f),
        reservoirFactor = 1.0f,
        confidenceFactor = predictionConfidence.toFloat(),
        effectivePeakProfile = effectivePeakProfile,
        momentSignalSummary = if (physiologicalImpulse != 0.0) String.format(java.util.Locale.ROOT, "Impulse: %+.1f pts", physiologicalImpulse) else "",
        overallFreshnessAgeMillis = maxOf(0L, System.currentTimeMillis() - refreshedAtMillis),
        hasRecentData = true,
        refreshedAtMillis = refreshedAtMillis
    )
}

fun buildDailyCognitiveBlocks(
    slots: List<ProtectedTimeSlot>,
    date: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<CognitiveBlockWindow> {
    // Bug 5 Fix (ADR-025): Populate explicitStartMillis and explicitEndMillis across both yesterday and today
    // so late-evening meals taper off smoothly across midnight without circular wrap-around phantom dips.
    val dates = listOf(date.minusDays(1), date)
    return dates.flatMap { targetDate ->
        val dayStart = targetDate.atStartOfDay(zoneId)
        val dayMillis = dayStart.toInstant().toEpochMilli()
        slots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayMillis) }
            .map { slot ->
                val category = when (slot.category) {
                    PtsCategory.MEAL -> CognitiveBlockCategory.MEAL
                    PtsCategory.EXERCISE -> CognitiveBlockCategory.EXERCISE
                    PtsCategory.WORK, PtsCategory.STUDY -> CognitiveBlockCategory.FOCUS_WORK
                    PtsCategory.BREAK, PtsCategory.MINDFULNESS -> CognitiveBlockCategory.WIND_DOWN
                    else -> CognitiveBlockCategory.WIND_DOWN
                }
                val startMin = slot.startMinuteOfDay
                val endMin = (startMin + slot.durationMinutes) % 1440
                val startZdt = targetDate.atTime((startMin / 60).coerceIn(0, 23), (startMin % 60).coerceIn(0, 59)).atZone(zoneId)
                val totalEndMinutes = startMin + slot.durationMinutes
                val endZdt = if (totalEndMinutes < 1440) {
                    targetDate.atTime((totalEndMinutes / 60).coerceIn(0, 23), (totalEndMinutes % 60).coerceIn(0, 59)).atZone(zoneId)
                } else {
                    targetDate.plusDays((totalEndMinutes / 1440).toLong())
                        .atTime(((totalEndMinutes % 1440) / 60).coerceIn(0, 23), ((totalEndMinutes % 1440) % 60).coerceIn(0, 59))
                        .atZone(zoneId)
                }
                val startMillis = startZdt.toInstant().toEpochMilli()
                val endMillis = endZdt.toInstant().toEpochMilli()
                CognitiveBlockWindow(
                    startMinuteOfDay = startMin,
                    endMinuteOfDay = endMin,
                    category = category,
                    intensity = 1.0,
                    label = slot.label,
                    explicitStartMillis = startMillis,
                    explicitEndMillis = endMillis
                )
            }
    }
}

fun buildDailyCognitiveBlocks(
    slots: List<ProtectedTimeSlot>,
    dayMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<CognitiveBlockWindow> = buildDailyCognitiveBlocks(
    slots = slots,
    date = Instant.ofEpochMilli(dayMillis).atZone(zoneId).toLocalDate(),
    zoneId = zoneId
)

fun extractStimulantDosesFromSlots(
    slots: List<ProtectedTimeSlot>,
    date: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<com.neuroflow.app.domain.service.StimulantDoseBout> {
    // Bug 3 Fix: Evaluate both yesterday and today so late-evening doses remain in 24h PK window across midnight
    val dates = listOf(date.minusDays(1), date)
    return dates.flatMap { targetDate ->
        val dayStartMillis = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        slots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayStartMillis) }
            .mapNotNull { slot ->
                val label = slot.label.lowercase()
                val words = label.split(Regex("[^a-z0-9_]+")).filter { it.isNotBlank() }.toSet()
                val type = when {
                    words.any { it in setOf("tea", "theanine", "matcha", "greentea") } && !label.contains("chamomile") && !label.contains("herbal") ->
                        com.neuroflow.app.domain.service.StimulantType.GREEN_TEA_L_THEANINE
                    words.any { it in setOf("coffee", "caffeine", "espresso", "latte", "cappuccino", "coldbrew", "americano") } ->
                        com.neuroflow.app.domain.service.StimulantType.CAFFEINE
                    words.any { it in setOf("ritalin", "methylphenidate") } || label.contains("meds ir") || label.contains("stimulant ir") ->
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_IR
                    words.any { it in setOf("adderall", "vyvanse", "concerta", "amphetamine", "dexedrine") } || label.contains("meds xr") || label.contains("stimulant xr") || words.contains("meds") ->
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_XR
                    else -> null
                } ?: return@mapNotNull null

                val doseZdt = targetDate.atTime((slot.startMinuteOfDay / 60).coerceIn(0, 23), (slot.startMinuteOfDay % 60).coerceIn(0, 59)).atZone(zoneId)
                val doseMillis = doseZdt.toInstant().toEpochMilli()
                com.neuroflow.app.domain.service.StimulantDoseBout(
                    timestampMillis = doseMillis,
                    doseMg = when (type) {
                        com.neuroflow.app.domain.service.StimulantType.CAFFEINE -> 100.0
                        com.neuroflow.app.domain.service.StimulantType.GREEN_TEA_L_THEANINE -> 50.0
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_IR -> 10.0
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_XR -> 20.0
                    },
                    type = type
                )
            }
    }.distinctBy { it.timestampMillis to it.type }
}

fun buildBehavioralObservations(
    completedTasks: List<TaskEntity> = emptyList(),
    sessions: List<TimeSessionEntity> = emptyList(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<BehavioralObservation> {
    data class TimedObservation(
        val timestampMillis: Long,
        val observation: BehavioralObservation
    )

    val timedObservations = mutableListOf<TimedObservation>()

    for (session in sessions) {
        if (session.startedAt > 0L) {
            val zdt = Instant.ofEpochMilli(session.startedAt).atZone(zoneId)
            val minuteOfDay = zdt.hour * 60 + zdt.minute
            val durationMins = if (session.endedAt != null && session.endedAt > session.startedAt) {
                ((session.endedAt - session.startedAt) / 60_000L).toDouble()
            } else if (session.durationMinutes > 0f) {
                session.durationMinutes.toDouble()
            } else {
                25.0
            }
            // Feature 2 (ADR-025): Cognitive demand weighting (2.0x for deep work or >= 45m)
            val isDeepFocus = session.sessionType == "DEEP_WORK" || durationMins >= 45.0
            val demandMultiplier = if (isDeepFocus) 2.0 else 1.0
            val weight = ((durationMins / 30.0) * demandMultiplier).coerceIn(0.5, 6.0)
            timedObservations.add(
                TimedObservation(
                    timestampMillis = session.startedAt,
                    observation = BehavioralObservation(
                        minuteOfDay = minuteOfDay,
                        productivityRating = if (isDeepFocus) 0.95 else 0.85,
                        weight = weight
                    )
                )
            )
        }
    }

    for (task in completedTasks) {
        // Bug 3 Fix (ADR-025): Exclude low-energy chores from shifting the peak-hour state estimate
        if (task.energyLevel == EnergyLevel.LOW) continue

        val completedAt = task.completedAt ?: task.updatedAt
        if (completedAt > 0L) {
            val zdt = Instant.ofEpochMilli(completedAt).atZone(zoneId)
            val minuteOfDay = zdt.hour * 60 + zdt.minute
            val weight = when (task.energyLevel) {
                EnergyLevel.HIGH -> 1.5
                EnergyLevel.MEDIUM -> 1.0
                else -> 1.0
            }
            val rating = when (task.energyLevel) {
                EnergyLevel.HIGH -> 0.95
                EnergyLevel.MEDIUM -> 0.80
                else -> 0.80
            }
            timedObservations.add(
                TimedObservation(
                    timestampMillis = completedAt,
                    observation = BehavioralObservation(
                        minuteOfDay = minuteOfDay,
                        productivityRating = rating,
                        weight = weight
                    )
                )
            )
        }
    }

    // Bug 3 Fix (ADR-025): Sort chronologically before sequential Markov-chain Kalman update
    return timedObservations.sortedBy { it.timestampMillis }.map { it.observation }
}

/**
 * Computes 7-day rolling daily sleep average by aggregating total sleep per calendar day
 * (preventing daytime naps from artificially halving the per-entry average).
 */
fun computeRollingDailySleepAverage(
    sleepLogs: List<SleepLogEntity>,
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Double {
    val sevenDaysAgo = nowMillis - 7 * 86_400_000L
    val recentSleepLogs = sleepLogs.filter { it.endAt > sevenDaysAgo && it.startAt < nowMillis && it.endAt > it.startAt }
    if (recentSleepLogs.isEmpty()) return 480.0

    // Sweep-line merge overlapping sleep intervals across the 7-day window to prevent wearable + manual double-counting
    val rawIntervals = recentSleepLogs.map { max(sevenDaysAgo, it.startAt) to min(nowMillis, it.endAt) }
        .filter { it.second > it.first }
        .sortedBy { it.first }

    val mergedIntervals = mutableListOf<Pair<Long, Long>>()
    for (interval in rawIntervals) {
        if (mergedIntervals.isEmpty()) {
            mergedIntervals.add(interval)
        } else {
            val last = mergedIntervals.last()
            if (interval.first <= last.second) {
                mergedIntervals[mergedIntervals.size - 1] = last.first to max(last.second, interval.second)
            } else {
                mergedIntervals.add(interval)
            }
        }
    }

    val minutesByLocalDay = mutableMapOf<LocalDate, Double>()
    for ((start, end) in mergedIntervals) {
        val mins = (end - start) / 60_000.0
        val wakeDay = Instant.ofEpochMilli(end).atZone(zoneId).toLocalDate()
        minutesByLocalDay[wakeDay] = (minutesByLocalDay[wakeDay] ?: 0.0) + mins
    }
    return if (minutesByLocalDay.isNotEmpty()) {
        minutesByLocalDay.values.average().coerceIn(120.0, 720.0)
    } else {
        480.0
    }
}

fun computeEnergyUiState(
    circadianEnergyService: CircadianEnergyService,
    sleepTelemetryRepository: SleepTelemetryRepository?,
    prefs: UserPreferences,
    sleepLogs: List<SleepLogEntity> = emptyList(),
    continuousActiveFocusMinutes: Int = 0,
    nowMillis: Long = System.currentTimeMillis(),
    completedTasks: List<TaskEntity> = emptyList(),
    sessions: List<TimeSessionEntity> = emptyList(),
    stimulantDoses: List<com.neuroflow.app.domain.service.StimulantDoseBout> = emptyList()
): Pair<CircadianSnapshot, EnergyUiModel> {
    val zoneId = ZoneId.systemDefault()
    val zonedDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
    if (anchor.toInstant().toEpochMilli() > nowMillis) {
        anchor = anchor.minusDays(1)
    }
    val baseWakeMillis = anchor.toInstant().toEpochMilli()

    val signal = sleepTelemetryRepository?.normalizeLogs(
        logs = sleepLogs,
        baseWakeMillis = baseWakeMillis,
        baseSleepMinutes = 480.0
    )

    val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
    val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480

    val dayStartMillis = zonedDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
    val physLogs = com.neuroflow.app.data.local.parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
        .filter { it.timestampMillis in (dayStartMillis - 8 * 3600_000L)..(dayStartMillis + 26 * 3600_000L) }

    val quickPhysBlocks = physLogs.mapNotNull { log ->
        val logZdt = Instant.ofEpochMilli(log.timestampMillis).atZone(zoneId)
        val minOfDay = logZdt.hour * 60 + logZdt.minute
        val duration = log.durationMinutes.coerceIn(5, 240)
        when (log.type) {
            com.neuroflow.app.data.local.PhysiologicalEventType.MEAL -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.MEAL,
                intensity = log.intensity,
                label = "Quick Logged Meal #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            com.neuroflow.app.data.local.PhysiologicalEventType.WORKOUT -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.EXERCISE,
                intensity = log.intensity,
                label = "Quick Logged Workout #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            com.neuroflow.app.data.local.PhysiologicalEventType.NAP -> null
        }
    }

    val quickPhysNaps = physLogs
        .filter { it.type == com.neuroflow.app.data.local.PhysiologicalEventType.NAP }
        .map { log ->
            val napDuration = log.durationMinutes.coerceIn(5, 180)
            com.neuroflow.app.domain.service.DaytimeNapBout(
                startMillis = log.timestampMillis,
                endMillis = log.timestampMillis + napDuration * 60_000L
            )
        }

    val naps = ((signal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }

    val rollingAverageSleepMinutes = computeRollingDailySleepAverage(sleepLogs, nowMillis, zoneId)

    val chronotype = if (!prefs.quizChronotype.isNullOrBlank()) {
        ChronotypeProfile.fromString(prefs.quizChronotype)
    } else if (!prefs.manualChronotype.isNullOrBlank()) {
        ChronotypeProfile.fromString(prefs.manualChronotype)
    } else if (prefs.meqScore > 0) {
        when {
            prefs.meqScore >= 70 -> ChronotypeProfile.DEFINITE_MORNING
            prefs.meqScore >= 59 -> ChronotypeProfile.MODERATE_MORNING
            prefs.meqScore >= 42 -> ChronotypeProfile.INTERMEDIATE
            prefs.meqScore >= 31 -> ChronotypeProfile.MODERATE_EVENING
            else -> ChronotypeProfile.DEFINITE_EVENING
        }
    } else {
        ChronotypeProfile.INTERMEDIATE
    }

    val manualPeakHour = if (prefs.manualPeakProfileEnabled) {
        (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
    } else null

    val cognitiveBlocks = buildDailyCognitiveBlocks(
        slots = prefs.protectedTimeSlots,
        date = zonedDateTime.toLocalDate(),
        zoneId = zoneId
    ) + quickPhysBlocks

    val observations = buildBehavioralObservations(completedTasks, sessions, zoneId)
    val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
    val learnedProfile = if (observations.isNotEmpty()) {
        AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
    } else null

    // Bug 2 Fix (ADR-030): Unify stimulant dose resolution across logs, slots, and explicit parameters
    val effectiveStimulantDoses = (
        stimulantDoses +
        com.neuroflow.app.data.local.parseStimulantLogs(prefs.stimulantLogsJson) +
        extractStimulantDosesFromSlots(prefs.protectedTimeSlots, zonedDateTime.toLocalDate(), zoneId)
    ).distinctBy { it.timestampMillis to it.type }

    // Feature 3 (ADR-023) & Bug 2 Fix: compute and pass social jetlag phase shift
    val sleepWindows = sleepLogs.map { com.neuroflow.app.domain.service.TimeSlotWindow(it.startAt, it.endAt) }
    val sjlReport = com.neuroflow.app.domain.service.SocialJetlagCalculator.evaluateFromSleepWindows(
        sleepWindows = sleepWindows,
        nowMillis = nowMillis,
        zoneId = zoneId
    )
    val socialJetlagLagHours = sjlReport.mondayMorningPhaseLagHours

    val hasAdvancedParameters = naps.isNotEmpty() ||
        cognitiveBlocks.isNotEmpty() ||
        learnedProfile != null ||
        (rollingAverageSleepMinutes != 480.0) ||
        manualPeakHour != null ||
        chronotype != ChronotypeProfile.INTERMEDIATE ||
        effectiveStimulantDoses.isNotEmpty() ||
        kotlin.math.abs(socialJetlagLagHours) > 0.0

    val snapshot = if (effectiveStimulantDoses.isNotEmpty() || kotlin.math.abs(socialJetlagLagHours) > 0.0) {
        circadianEnergyService.evaluateSnapshotWithStimulants(
            timestampMillis = nowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = sleepDurationMinutes,
            continuousActiveFocusMinutes = continuousActiveFocusMinutes,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakHour,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            cognitiveBlocks = cognitiveBlocks,
            stimulantDoses = effectiveStimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours
        )
    } else if (hasAdvancedParameters) {
        if (cognitiveBlocks.isEmpty() && learnedProfile == null && rollingAverageSleepMinutes == 480.0 && manualPeakHour == null && chronotype == ChronotypeProfile.INTERMEDIATE) {
            circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = continuousActiveFocusMinutes,
                zoneId = zoneId,
                naps = naps
            )
        } else {
            circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = continuousActiveFocusMinutes,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakHour,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                cognitiveBlocks = cognitiveBlocks
            )
        }
    } else {
        circadianEnergyService.evaluateSnapshot(
            timestampMillis = nowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = sleepDurationMinutes,
            continuousActiveFocusMinutes = continuousActiveFocusMinutes,
            zoneId = zoneId
        )
    }

    // Bug 7 Fix (ADR-026): When daytime power naps or stimulant adenosine masking are active,
    // homeostatic pressure has diverged from unadjusted static point counters. Derive fatigueZone
    // from snapshot.homeostaticSleepPressure rather than leaving it stuck on prefs.sleepPressurePoints.
    val isHomeostaticAdjusted = naps.isNotEmpty() || effectiveStimulantDoses.isNotEmpty()
    val resolvedFatigueZone = if (isHomeostaticAdjusted) {
        val pct = (snapshot.homeostaticSleepPressure * 100).toInt().coerceIn(0, 100)
        when {
            pct <= 24 -> FatigueZone.RESTED
            pct <= 49 -> FatigueZone.MODERATE
            pct <= 74 -> FatigueZone.HIGH
            else -> FatigueZone.CRITICAL
        }
    } else null

    val effectiveProfile = if (prefs.manualPeakProfileEnabled) {
        val w1 = PeakWindow(
            startMinuteOffset = prefs.manualPeakWindow1StartOffsetMinutes,
            durationMinutes = prefs.manualPeakWindow1DurationMinutes,
            amplitude = prefs.manualPeakWindow1Amplitude
        )
        val w2 = PeakWindow(
            startMinuteOffset = prefs.manualPeakWindow2StartOffsetMinutes,
            durationMinutes = prefs.manualPeakWindow2DurationMinutes,
            amplitude = prefs.manualPeakWindow2Amplitude
        )
        val w3 = PeakWindow(
            startMinuteOffset = prefs.manualPeakWindow3StartOffsetMinutes,
            durationMinutes = prefs.manualPeakWindow3DurationMinutes,
            amplitude = prefs.manualPeakWindow3Amplitude
        )
        EffectivePeakProfile(
            profileType = if (prefs.manualPeakProfileType == "WEEKEND") ProfileType.WEEKEND else ProfileType.WORKDAY,
            anchorMinuteOfDay = prefs.manualPeakAnchorMinuteOfDay,
            windows = listOf(w1, w2, w3),
            confidence = ConfidenceComponents(
                sleepCoverage = prefs.morningTuneSleepWeight,
                wakeConsistency = prefs.morningTuneWakeWeight,
                behaviorPerformance = prefs.morningTuneBehaviorWeight,
                overall = prefs.peakDetectionConfidence
            ),
            windowConfidences = listOf(0.9f, 0.85f, 0.8f),
            explanation = "Manual Peak Profile Active",
            driftStatus = "configured",
            adaptiveFreezeMode = prefs.adaptivePeakFreezeEnabled,
            confidenceGatedAbstention = prefs.peakConfidenceAbstentionEnabled,
            abstentionReason = prefs.peakConfidenceAbstentionReason
        )
    } else {
        val peakAnchorMin = prefs.detectedPeakMinuteOfDay.takeIf { it > 0 } ?: (prefs.peakEnergyStart * 60)
        val rawSpan = ((prefs.peakEnergyEnd - prefs.peakEnergyStart) % 24 + 24) % 24
        val safePeakDurationHours = (if (rawSpan == 0) 4 else rawSpan).coerceIn(1, 8)
        EffectivePeakProfile(
            profileType = ProfileType.WORKDAY,
            anchorMinuteOfDay = peakAnchorMin,
            windows = listOf(
                PeakWindow(startMinuteOffset = 0, durationMinutes = safePeakDurationHours * 60, amplitude = 1.0f)
            ),
            confidence = ConfidenceComponents(
                sleepCoverage = prefs.morningTuneSleepWeight,
                wakeConsistency = prefs.morningTuneWakeWeight,
                behaviorPerformance = prefs.morningTuneBehaviorWeight,
                overall = prefs.peakDetectionConfidence.takeIf { it > 0f } ?: 0.85f
            ),
            windowConfidences = listOf(0.85f),
            explanation = "Automated Circadian Calibration",
            driftStatus = "synchronized",
            adaptiveFreezeMode = prefs.adaptivePeakFreezeEnabled,
            confidenceGatedAbstention = prefs.peakConfidenceAbstentionEnabled,
            abstentionReason = prefs.peakConfidenceAbstentionReason
        )
    }

    val energyUi = buildEnergyUiModel(
        usableEnergy = snapshot.usableEnergy,
        circadianFactor = snapshot.circadianAlertness.toFloat(),
        sleepPressureFactor = snapshot.homeostaticSleepPressure.toFloat(),
        sleepPressurePoints = prefs.sleepPressurePoints,
        fatigueZone = resolvedFatigueZone,
        refreshedAtMillis = nowMillis,
        baselineBiologicalEnergy = snapshot.baselineBiologicalEnergy,
        physiologicalImpulse = snapshot.physiologicalImpulse,
        learnedAdjustmentPoints = snapshot.learnedAdjustmentPoints,
        predictionConfidence = snapshot.predictionConfidence,
        effectivePeakProfile = effectiveProfile
    )

    return Pair(snapshot, energyUi)
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelcircadianenergysummarykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/CircadianEnergySummary.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/CircadianEnergySummary.kt`
- **Lines of Code:** 64
- **Size:** 2,795 bytes (2.7 KB)

```kotlin
package com.neuroflow.app.domain.model

import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.EnergyPhase
import java.time.LocalTime

typealias EnergyPhase = com.neuroflow.app.domain.service.EnergyPhase

data class CircadianEnergySummary(
    val currentEnergyScore: Int,
    val currentPhase: EnergyPhase,
    val phaseConfidence: Float = 0.9f,
    val peakStart: LocalTime = LocalTime.of(9, 30),
    val peakEnd: LocalTime = LocalTime.of(12, 30),
    val dipStart: LocalTime = LocalTime.of(13, 30),
    val dipEnd: LocalTime = LocalTime.of(15, 30),
    val windDownStart: LocalTime = LocalTime.of(21, 30),
    val sleepPressureNormalized: Float = 0.45f,
    val cognitiveBudgetMinutesRemaining: Int = 240,
    val calibrationQuality: Float = 0.85f,
)

fun buildCircadianEnergySummary(
    dailySummary: DailyCircadianSummary?,
    snapshot: CircadianSnapshot,
    sleepPressurePoints: Int = 450,
    dailyDeepWorkTargetMinutes: Int? = null,
    sleepHour: Int = 23,
    completedDeepWorkMinutes: Int = 0
): CircadianEnergySummary {
    val peakStart = dailySummary?.let { LocalTime.of(it.peakWindowStartHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(9, 30)
    val peakEnd = dailySummary?.let { LocalTime.of(it.peakWindowEndHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(12, 30)
    val dipStart = dailySummary?.let { LocalTime.of(it.dipWindowStartHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(13, 30)
    val dipEnd = dailySummary?.let { LocalTime.of(it.dipWindowEndHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(15, 30)
    val targetWindDownHour = ((sleepHour - 2) % 24 + 24) % 24
    val windDownStart = LocalTime.of(targetWindDownHour, 0)
    val sleepPressureNormalized = if (snapshot.homeostaticPressureS > 0.0 && !snapshot.homeostaticPressureS.isNaN()) {
        snapshot.homeostaticPressureS.toFloat().coerceIn(0f, 1f)
    } else {
        (sleepPressurePoints / 1000f).coerceIn(0f, 1f)
    }
    val totalBudget = dailyDeepWorkTargetMinutes?.takeIf { it > 0 } ?: dailySummary?.recommendedDeepWorkMinutes ?: 240
    val cognitiveBudget = kotlin.math.max(0, totalBudget - completedDeepWorkMinutes)
    val confidence = snapshot.predictionConfidence.toFloat().coerceIn(0.1f, 1.0f)

    return CircadianEnergySummary(
        currentEnergyScore = snapshot.usableEnergy,
        currentPhase = snapshot.phase,
        phaseConfidence = confidence,
        peakStart = peakStart,
        peakEnd = peakEnd,
        dipStart = dipStart,
        dipEnd = dipEnd,
        windDownStart = windDownStart,
        sleepPressureNormalized = sleepPressureNormalized,
        cognitiveBudgetMinutesRemaining = cognitiveBudget,
        calibrationQuality = confidence
    )
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelfatiguezonekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/FatigueZone.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/FatigueZone.kt`
- **Lines of Code:** 206
- **Size:** 8,543 bytes (8.3 KB)

```kotlin
package com.neuroflow.app.domain.model

import kotlin.math.pow
import kotlin.math.roundToInt

enum class FatigueZone {
    RESTED,
    MODERATE,
    HIGH,
    CRITICAL;

    val label: String get() = label(this)

    companion object {
        const val SOFT_MAX_REFERENCE = 3000
        const val AWAKE_POINTS_PER_MINUTE = 1
        const val MAX_PRESSURE_POINTS = 14_400
        const val SLEEP_INERTIA_DURATION_MINUTES = 30
        const val DEFAULT_MAX_SLEEP_INERTIA_PENALTY = 0.35f
        const val CHRONIC_RESTRICTION_THRESHOLD_HOURS = 6.0f
        const val CHRONIC_RESTRICTION_THRESHOLD_MINUTES = 360
        const val OPTIMAL_DAILY_SLEEP_MINUTES = 480

        private const val FATIGUE_CURVE_GAMMA = 0.82f
        private const val CYCLE_MINUTES = 90
        private const val PHASE_ONE_END_MINUTE = 60
        private const val PHASE_ONE_BONUS = 126
        private const val FULL_CYCLE_RECOVERY = 279

        data class PressureResult(
            val pressurePoints: Int,
            val awakeMinutes: Int = 0,
            val sleepMinutes: Int = 0,
            val recoveryPointsEarned: Int = 0,
            val recoveryPointsApplied: Int = 0
        )

        fun pressureFromAwakeMinutes(awakeMinutes: Int): Int {
            val safeAwakeMinutes = awakeMinutes.coerceAtLeast(0)
            return (safeAwakeMinutes * AWAKE_POINTS_PER_MINUTE).coerceAtMost(MAX_PRESSURE_POINTS)
        }

        fun applyAwakeMinutes(currentPressure: Int, awakeMinutes: Int): PressureResult {
            val safeCurrentPressure = currentPressure.coerceAtLeast(0).coerceAtMost(MAX_PRESSURE_POINTS)
            val safeAwakeMinutes = awakeMinutes.coerceAtLeast(0)
            val addedPressure = pressureFromAwakeMinutes(safeAwakeMinutes)
            return PressureResult(
                pressurePoints = (safeCurrentPressure + addedPressure).coerceAtMost(MAX_PRESSURE_POINTS),
                awakeMinutes = safeAwakeMinutes
            )
        }

        fun applySleepSession(currentPressure: Int, sleepMinutes: Int): PressureResult {
            val safeCurrentPressure = currentPressure.coerceAtLeast(0).coerceAtMost(MAX_PRESSURE_POINTS)
            val safeSleepMinutes = sleepMinutes.coerceAtLeast(0)
            val recoveryPoints = recoveryForSleepMinutes(safeSleepMinutes)
            val applied = minOf(safeCurrentPressure, recoveryPoints)
            val finalPressure = (safeCurrentPressure - applied).coerceAtLeast(0)
            return PressureResult(
                pressurePoints = finalPressure,
                sleepMinutes = safeSleepMinutes,
                recoveryPointsEarned = recoveryPoints,
                recoveryPointsApplied = applied
            )
        }

        fun fromPressurePoints(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): FatigueZone {
            val percent = fatiguePercent(pressurePoints, softMaxReference)
            return when {
                percent <= 24 -> RESTED
                percent <= 49 -> MODERATE
                percent <= 74 -> HIGH
                else -> CRITICAL
            }
        }

        fun fatigueZone(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): FatigueZone =
            fromPressurePoints(pressurePoints, softMaxReference)

        fun fromUsableEnergy(usableEnergy: Int): FatigueZone {
            return when {
                usableEnergy >= 70 -> RESTED
                usableEnergy >= 45 -> MODERATE
                usableEnergy >= 25 -> HIGH
                else -> CRITICAL
            }
        }

        fun fatigueRatio(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): Float {
            val safeMax = softMaxReference.coerceAtLeast(1)
            val safePressure = pressurePoints.coerceAtLeast(0)
            val ratio = (safePressure.toFloat() / safeMax.toFloat()).coerceIn(0f, 1f)
            return ratio.pow(FATIGUE_CURVE_GAMMA).coerceIn(0f, 1f)
        }

        fun fatiguePercent(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): Int {
            return (fatigueRatio(pressurePoints, softMaxReference) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }

        fun label(zone: FatigueZone): String {
            return when (zone) {
                RESTED -> "Rested"
                MODERATE -> "Moderate"
                HIGH -> "High"
                CRITICAL -> "Critical"
            }
        }

        fun fatigueZoneLabel(zone: FatigueZone): String = label(zone)

        fun calculateSleepInertiaPenalty(
            minutesSinceWake: Int,
            maxPenalty: Float = DEFAULT_MAX_SLEEP_INERTIA_PENALTY
        ): Float {
            if (minutesSinceWake < 0 || minutesSinceWake >= SLEEP_INERTIA_DURATION_MINUTES) {
                return 0f
            }
            val safeMax = maxPenalty.coerceIn(0f, 1f)
            val decayFraction = 1f - (minutesSinceWake.toFloat() / SLEEP_INERTIA_DURATION_MINUTES.toFloat())
            return (safeMax * decayFraction).coerceIn(0f, 1f)
        }

        fun isSleepInertiaActive(minutesSinceWake: Int?): Boolean {
            if (minutesSinceWake == null) return false
            return minutesSinceWake in 0 until SLEEP_INERTIA_DURATION_MINUTES
        }

        fun calculateChronicRestrictionMultiplier(rollingAverageDailySleepMinutes: Float): Float {
            if (rollingAverageDailySleepMinutes <= 0f) {
                return 1.6f
            }
            val averageHours = rollingAverageDailySleepMinutes / 60f
            if (averageHours >= CHRONIC_RESTRICTION_THRESHOLD_HOURS) {
                return 1.0f
            }
            val deficitHours = (CHRONIC_RESTRICTION_THRESHOLD_HOURS - averageHours).coerceAtLeast(0f)
            val penalty = (deficitHours / CHRONIC_RESTRICTION_THRESHOLD_HOURS) * 0.6f
            return (1.0f + penalty).coerceIn(1.0f, 1.6f)
        }

        fun calculateChronicRestrictionMultiplierFromDays(dailySleepMinutes: List<Int>): Float {
            if (dailySleepMinutes.isEmpty()) return 1.0f
            val validDays = dailySleepMinutes.filter { it >= 0 }
            if (validDays.isEmpty()) return 1.0f
            val avg = validDays.average().toFloat()
            return calculateChronicRestrictionMultiplier(avg)
        }

        fun adjustedFatigueRatio(
            pressurePoints: Int,
            minutesSinceWake: Int? = null,
            rollingAverageSleepMinutes: Float? = null,
            softMaxReference: Int = SOFT_MAX_REFERENCE
        ): Float {
            val baseRatio = fatigueRatio(pressurePoints, softMaxReference)
            val chronicMultiplier = rollingAverageSleepMinutes?.let {
                calculateChronicRestrictionMultiplier(it)
            } ?: 1.0f
            val inertiaPenalty = minutesSinceWake?.let {
                calculateSleepInertiaPenalty(it)
            } ?: 0f

            val compounded = baseRatio * chronicMultiplier + inertiaPenalty
            return compounded.coerceIn(0f, 1f)
        }

        fun adjustedFatiguePercent(
            pressurePoints: Int,
            minutesSinceWake: Int? = null,
            rollingAverageSleepMinutes: Float? = null,
            softMaxReference: Int = SOFT_MAX_REFERENCE
        ): Int {
            return (adjustedFatigueRatio(pressurePoints, minutesSinceWake, rollingAverageSleepMinutes, softMaxReference) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }

        fun adjustedFatigueZone(
            pressurePoints: Int,
            minutesSinceWake: Int? = null,
            rollingAverageSleepMinutes: Float? = null,
            softMaxReference: Int = SOFT_MAX_REFERENCE
        ): FatigueZone {
            val percent = adjustedFatiguePercent(pressurePoints, minutesSinceWake, rollingAverageSleepMinutes, softMaxReference)
            return when {
                percent <= 24 -> RESTED
                percent <= 49 -> MODERATE
                percent <= 74 -> HIGH
                else -> CRITICAL
            }
        }

        fun recoveryForSleepMinutes(sleepMinutes: Int): Int {
            val safeSleepMinutes = sleepMinutes.coerceAtLeast(0)
            if (safeSleepMinutes == 0) return 0

            val fullCycles = safeSleepMinutes / CYCLE_MINUTES
            val remainderMinutes = safeSleepMinutes % CYCLE_MINUTES
            val remainderRecovery = (remainderMinutes * (FULL_CYCLE_RECOVERY.toDouble() / CYCLE_MINUTES.toDouble())).roundToInt()

            return (fullCycles * FULL_CYCLE_RECOVERY) + remainderRecovery
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainenginemeqchronotypedetectorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/MEQChronotypeDetector.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/MEQChronotypeDetector.kt`
- **Lines of Code:** 93
- **Size:** 3,343 bytes (3.3 KB)

```kotlin
package com.neuroflow.app.domain.engine

/**
 * Baseline chronotype detector using the Morningness-Eveningness Questionnaire (MEQ).
 *
 * This is meant to establish the user's initial energy profile before any task-history
 * based adaptation is introduced.
 */
object MEQChronotypeDetector {

    const val QUESTION_COUNT = 19
    const val MIN_TOTAL_SCORE = 16
    const val MAX_TOTAL_SCORE = 86

    enum class Chronotype {
        DEFINITE_MORNING,
        MODERATE_MORNING,
        INTERMEDIATE,
        MODERATE_EVENING,
        DEFINITE_EVENING
    }

    data class Result(
        val totalScore: Int,
        val chronotype: Chronotype,
        val baselinePeakStartHour: Int,
        val baselinePeakEndHour: Int,
        val answeredQuestions: Int,
        val confidence: Float
    )

    /**
     * Accepts the 19 MEQ question scores exactly as provided in the questionnaire.
     *
     * Each item should be the points for the selected answer, already mapped to the
     * question's scoring rules.
     */
    fun detect(answerScores: List<Int>): Result {
        require(answerScores.size == QUESTION_COUNT) {
            "MEQ requires exactly $QUESTION_COUNT answers."
        }

        val answeredCount = answerScores.count { it > 0 }
        val rawSum = answerScores.sum()
        val totalScore = when {
            answeredCount == 0 -> 48 // Default to intermediate baseline if completely unanswered
            answeredCount < QUESTION_COUNT -> kotlin.math.round((rawSum.toDouble() / answeredCount.toDouble()) * QUESTION_COUNT).toInt().coerceIn(MIN_TOTAL_SCORE, MAX_TOTAL_SCORE)
            else -> rawSum.coerceIn(MIN_TOTAL_SCORE, MAX_TOTAL_SCORE)
        }
        val chronotype = classify(totalScore)
        val (peakStart, peakEnd) = baselinePeakWindow(chronotype)
        val confidence = (answeredCount.toFloat() / QUESTION_COUNT).coerceIn(0f, 1f)

        return Result(
            totalScore = totalScore,
            chronotype = chronotype,
            baselinePeakStartHour = peakStart,
            baselinePeakEndHour = peakEnd,
            answeredQuestions = answeredCount,
            confidence = confidence
        )
    }

    fun classify(totalScore: Int): Chronotype {
        return when (totalScore) {
            in 70..MAX_TOTAL_SCORE -> Chronotype.DEFINITE_MORNING
            in 59..69 -> Chronotype.MODERATE_MORNING
            in 42..58 -> Chronotype.INTERMEDIATE
            in 31..41 -> Chronotype.MODERATE_EVENING
            else -> Chronotype.DEFINITE_EVENING
        }
    }

    fun baselinePeakWindow(chronotype: Chronotype): Pair<Int, Int> {
        return when (chronotype) {
            Chronotype.DEFINITE_MORNING -> 6 to 11
            Chronotype.MODERATE_MORNING -> 7 to 12
            Chronotype.INTERMEDIATE -> 9 to 14
            Chronotype.MODERATE_EVENING -> 13 to 18
            Chronotype.DEFINITE_EVENING -> 15 to 21
        }
    }

    fun label(chronotype: Chronotype): String {
        return when (chronotype) {
            Chronotype.DEFINITE_MORNING -> "Definite Morning Type"
            Chronotype.MODERATE_MORNING -> "Moderate Morning Type"
            Chronotype.INTERMEDIATE -> "Intermediate Type"
            Chronotype.MODERATE_EVENING -> "Moderate Evening Type"
            Chronotype.DEFINITE_EVENING -> "Definite Evening Type"
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainrepositorysleeppressurerepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/repository/SleepPressureRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/repository/SleepPressureRepository.kt`
- **Lines of Code:** 393
- **Size:** 15,857 bytes (15.5 KB)

```kotlin
package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepPressureRepository @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sleepLogRepository: SleepLogRepository
) {

    companion object {
        const val MIN_SLEEP_LOG_DURATION_MILLIS = 60_000L
        const val MAX_SLEEP_LOG_DURATION_MINUTES = 16 * 60
        const val MAX_SLEEP_LOG_DURATION_MILLIS = MAX_SLEEP_LOG_DURATION_MINUTES * 60_000L
        private const val AUTO_FALLBACK_AFTER_WAKE_MINUTES = 12 * 60
        private const val HOUR_MILLIS = 60 * 60_000L

        fun coalesceSleepIntervals(sleepIntervals: List<SleepInterval>): List<SleepInterval> {
            if (sleepIntervals.isEmpty()) return emptyList()
            val sorted = sleepIntervals.sortedBy { it.startAt }
            val merged = mutableListOf<SleepInterval>()
            var current = sorted[0]
            for (i in 1 until sorted.size) {
                val next = sorted[i]
                if (next.startAt <= current.endAt) {
                    current = current.copy(endAt = maxOf(current.endAt, next.endAt))
                } else {
                    merged.add(current)
                    current = next
                }
            }
            merged.add(current)
            return merged
        }
    }

    sealed interface AddSleepLogResult {
        data class Added(val snapshot: Snapshot) : AddSleepLogResult
        data object OverlapsExisting : AddSleepLogResult
        data class TooLong(val maxMinutes: Int = MAX_SLEEP_LOG_DURATION_MINUTES) : AddSleepLogResult
    }

    data class Snapshot(
        val pressurePoints: Int,
        val fatiguePercent: Int,
        val fatigueZone: FatigueZone,
        val refreshedAtMillis: Long,
        val minutesSinceWake: Int? = null,
        val isSleepInertiaActive: Boolean = false,
        val sleepInertiaPenalty: Float = 0f,
        val chronicRestrictionMultiplier: Float = 1.0f,
        val adjustedFatiguePercent: Int = fatiguePercent,
        val adjustedFatigueZone: FatigueZone = fatigueZone
    )

    data class SleepInterval(
        val startAt: Long,
        val endAt: Long
    )

    private val dismissedFallbackIntervals = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val refreshMutex = Mutex()

    fun observeSleepLogs(): Flow<List<SleepLogEntity>> = sleepLogRepository.observeAll()

    suspend fun addSleepLog(startAtMillis: Long, endAtMillis: Long): AddSleepLogResult {
        val safeStart = minOf(startAtMillis, endAtMillis)
        val safeEnd = maxOf(startAtMillis, endAtMillis)
        val normalizedEnd = if (safeEnd <= safeStart) safeStart + MIN_SLEEP_LOG_DURATION_MILLIS else safeEnd

        val durationMillis = normalizedEnd - safeStart
        if (durationMillis > MAX_SLEEP_LOG_DURATION_MILLIS) {
            return AddSleepLogResult.TooLong()
        }

        val overlapping = sleepLogRepository.getOverlapping(safeStart, normalizedEnd)
        val realOverlapping = overlapping.filter { it.source == "MANUAL" || it.source == "HEALTH_CONNECT" }
        if (realOverlapping.isNotEmpty()) {
            return AddSleepLogResult.OverlapsExisting
        }

        val autoOverlapping = overlapping.filter { it.source == "AUTO_DEFAULT" }
        if (autoOverlapping.isNotEmpty()) {
            val dismissedKeys = autoOverlapping.map { "${it.startAt}_${it.endAt}" }.toSet()
            dismissedFallbackIntervals.addAll(dismissedKeys)
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(dismissedSleepFallbacks = prefs.dismissedSleepFallbacks + dismissedKeys)
            }
            autoOverlapping.forEach { autoLog ->
                sleepLogRepository.deleteById(autoLog.id)
            }
        }

        sleepLogRepository.addLog(
            startAt = safeStart,
            endAt = normalizedEnd,
            source = "MANUAL"
        )

        val prefs = preferencesDataStore.preferencesFlow.first()
        val currentTrackingStart = prefs.sleepPressureTrackingStartedAtMillis
        val newTrackingStart = if (currentTrackingStart > 0L) {
            minOf(currentTrackingStart, safeStart)
        } else {
            safeStart
        }

        if (newTrackingStart != currentTrackingStart) {
            preferencesDataStore.updatePreferences {
                it.copy(sleepPressureTrackingStartedAtMillis = newTrackingStart)
            }
        }

        return AddSleepLogResult.Added(refreshCurrentPressure())
    }

    suspend fun deleteSleepLog(logId: String): Snapshot {
        val existing = sleepLogRepository.getById(logId)
        if (existing != null && existing.source == "AUTO_DEFAULT") {
            val key = "${existing.startAt}_${existing.endAt}"
            dismissedFallbackIntervals.add(key)
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(dismissedSleepFallbacks = prefs.dismissedSleepFallbacks + key)
            }
        }
        sleepLogRepository.deleteById(logId)
        return refreshCurrentPressure()
    }

    suspend fun clearSleepLogsAndReset(nowMillis: Long = System.currentTimeMillis()): Snapshot {
        dismissedFallbackIntervals.clear()
        sleepLogRepository.deleteAll()

        val prefs = preferencesDataStore.preferencesFlow.first()
        val trackingStart = defaultTrackingStartMillis(nowMillis, prefs.wakeUpHour)
        val pressure = minutesBetween(trackingStart, nowMillis)

        preferencesDataStore.updatePreferences {
            it.copy(
                sleepPressureTrackingStartedAtMillis = trackingStart,
                sleepPressureLastComputedAtMillis = nowMillis,
                sleepPressurePoints = pressure,
                dismissedSleepFallbacks = emptySet()
            )
        }

        val fatiguePercent = FatigueZone.fatiguePercent(pressure)
        val zone = FatigueZone.fatigueZone(pressure)
        val wakeMins = minutesBetween(trackingStart, nowMillis)

        return Snapshot(
            pressurePoints = pressure,
            fatiguePercent = fatiguePercent,
            fatigueZone = zone,
            refreshedAtMillis = nowMillis,
            minutesSinceWake = wakeMins,
            isSleepInertiaActive = FatigueZone.isSleepInertiaActive(wakeMins),
            sleepInertiaPenalty = FatigueZone.calculateSleepInertiaPenalty(wakeMins),
            chronicRestrictionMultiplier = 1.0f,
            adjustedFatiguePercent = fatiguePercent,
            adjustedFatigueZone = zone
        )
    }

    suspend fun refreshCurrentPressure(nowMillis: Long = System.currentTimeMillis()): Snapshot = refreshMutex.withLock {
        val prefs = preferencesDataStore.preferencesFlow.first()

        // Phase 1 boundary fix: use the preference-stored tracking start when valid
        // Otherwise default to configured wake-up hour
        val trackingStart = if (prefs.sleepPressureTrackingStartedAtMillis > 0L &&
            prefs.sleepPressureTrackingStartedAtMillis < nowMillis
        ) {
            maxOf(prefs.sleepPressureTrackingStartedAtMillis, nowMillis - 7 * 86_400_000L)
        } else {
            defaultTrackingStartMillis(nowMillis, prefs.wakeUpHour)
        }

        var pressure = 0
        var cursor = trackingStart

        val logs = sleepLogRepository.getOverlapping(trackingStart, nowMillis)
        val sleepIntervals = logs
            .map { SleepInterval(it.startAt, it.endAt) }
            .toMutableList()

        buildAutoFallbackSleepIntervals(
            prefs = prefs,
            trackingStart = trackingStart,
            nowMillis = nowMillis
        ).forEach { fallback ->
            val persisted = sleepLogRepository.addLog(
                startAt = fallback.startAt,
                endAt = fallback.endAt,
                source = "AUTO_DEFAULT"
            )
            sleepIntervals += SleepInterval(persisted.startAt, persisted.endAt)
        }

        sleepIntervals.sortBy { it.startAt }

        // Bug 4 Fix (ADR-030): Sweep-line merge overlapping or contiguous sleep intervals
        // so that multi-record sleep sessions receive continuous non-linear 90m SWS cycle recovery bonuses
        val coalescedIntervals = coalesceSleepIntervals(sleepIntervals)

        coalescedIntervals.forEach { interval ->
            val boundedStart = maxOf(cursor, interval.startAt)
            val boundedEnd = minOf(nowMillis, interval.endAt)
            if (boundedEnd <= boundedStart) {
                return@forEach
            }

            if (boundedStart > cursor) {
                val awakeMinutes = minutesBetween(cursor, boundedStart)
                pressure = FatigueZone
                    .applyAwakeMinutes(pressure, awakeMinutes)
                    .pressurePoints
            }

            val sleepMinutes = minutesBetween(boundedStart, boundedEnd)
            pressure = FatigueZone
                .applySleepSession(pressure, sleepMinutes)
                .pressurePoints

            cursor = boundedEnd
        }

        if (cursor < nowMillis) {
            val awakeMinutes = minutesBetween(cursor, nowMillis)
            pressure = FatigueZone
                .applyAwakeMinutes(pressure, awakeMinutes)
                .pressurePoints
        }

        preferencesDataStore.updatePreferences {
            it.copy(
                sleepPressurePoints = pressure,
                sleepPressureLastComputedAtMillis = nowMillis,
                sleepPressureTrackingStartedAtMillis = trackingStart
            )
        }

        val lastIntervalEnd = coalescedIntervals.map { it.endAt }.filter { it <= nowMillis }.maxOrNull()
        val wakeAnchor = lastIntervalEnd ?: trackingStart
        val minutesSinceWake = minutesBetween(wakeAnchor, nowMillis)

        val sevenDaysAgo = nowMillis - 7 * 24 * 60 * 60_000L
        val rollingLogs = sleepLogRepository.getOverlapping(sevenDaysAgo, nowMillis)
        val rollingAvgDailyMinutes = computeRollingDailySleepAverage(rollingLogs, nowMillis, ZoneId.systemDefault()).toFloat()
        val chronicMultiplier = FatigueZone.calculateChronicRestrictionMultiplier(rollingAvgDailyMinutes)
        val inertiaPenalty = FatigueZone.calculateSleepInertiaPenalty(minutesSinceWake)
        val inertiaActive = FatigueZone.isSleepInertiaActive(minutesSinceWake)

        val fatiguePercent = FatigueZone.fatiguePercent(pressure)
        val zone = FatigueZone.fatigueZone(pressure)
        val adjFatiguePercent = FatigueZone.adjustedFatiguePercent(
            pressurePoints = pressure,
            minutesSinceWake = minutesSinceWake,
            rollingAverageSleepMinutes = rollingAvgDailyMinutes
        )
        val adjZone = FatigueZone.adjustedFatigueZone(
            pressurePoints = pressure,
            minutesSinceWake = minutesSinceWake,
            rollingAverageSleepMinutes = rollingAvgDailyMinutes
        )

        Snapshot(
            pressurePoints = pressure,
            fatiguePercent = fatiguePercent,
            fatigueZone = zone,
            refreshedAtMillis = nowMillis,
            minutesSinceWake = minutesSinceWake,
            isSleepInertiaActive = inertiaActive,
            sleepInertiaPenalty = inertiaPenalty,
            chronicRestrictionMultiplier = chronicMultiplier,
            adjustedFatiguePercent = adjFatiguePercent,
            adjustedFatigueZone = adjZone
        )
    }

    private fun minutesBetween(startMillis: Long, endMillis: Long): Int {
        // Phase 1 boundary guard: prevent negative or zero duration calculations
        if (endMillis <= startMillis) return 0
        return ((endMillis - startMillis) / 60_000L).toInt().coerceAtLeast(0)
    }

    private fun defaultTrackingStartMillis(nowMillis: Long, wakeUpHour: Int): Long {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var anchor = now.toLocalDate()
            .atTime(wakeUpHour.coerceIn(0, 23), 0)
            .atZone(zoneId)

        if (anchor.toInstant().toEpochMilli() > nowMillis) {
            anchor = anchor.minusDays(1)
        }

        return anchor.toInstant().toEpochMilli()
    }

    private suspend fun buildAutoFallbackSleepIntervals(
        prefs: UserPreferences,
        trackingStart: Long,
        nowMillis: Long
    ): List<SleepInterval> {
        if (!prefs.autoFallbackSleepInsertionEnabled) {
            return emptyList()
        }

        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val wakeHour = prefs.wakeUpHour.coerceIn(0, 23)
        val sleepHour = prefs.sleepHour.coerceIn(0, 23)

        val result = mutableListOf<SleepInterval>()

        // Check up to 7 past days for missing automatic fallback sleep logs
        for (dayOffset in 0..7) {
            val targetDate = now.toLocalDate().minusDays(dayOffset.toLong())

            val defaultSleepStart = targetDate
                .atTime(sleepHour, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()

            val defaultWakeEnd = if (wakeHour <= sleepHour) {
                targetDate.plusDays(1).atTime(wakeHour, 0).atZone(zoneId).toInstant().toEpochMilli()
            } else {
                targetDate.atTime(wakeHour, 0).atZone(zoneId).toInstant().toEpochMilli()
            }

            // 12-hour post-wake delay check relative to actual default wake time
            if (nowMillis < defaultWakeEnd + (AUTO_FALLBACK_AFTER_WAKE_MINUTES * 60_000L)) {
                continue
            }

            if (defaultWakeEnd < trackingStart) {
                continue
            }

            // Bug 2 Fix (ADR-025): Scope query strictly to the target night's window
            // to prevent newer manual logs from blocking fallback sleep for older unlogged nights.
            val queryStart = defaultSleepStart - 4 * HOUR_MILLIS
            val queryEnd = defaultWakeEnd + 4 * HOUR_MILLIS
            val logsInWindow = sleepLogRepository.getOverlapping(queryStart, queryEnd)
            val hasManualLogForNight = logsInWindow.any { it.source == "MANUAL" || it.source == "HEALTH_CONNECT" }
            if (hasManualLogForNight) {
                continue
            }

            val boundedStart = defaultSleepStart
            val boundedEnd = minOf(nowMillis, defaultWakeEnd)
            if (boundedEnd <= boundedStart) {
                continue
            }

            val duration = boundedEnd - boundedStart
            if (duration < MIN_SLEEP_LOG_DURATION_MILLIS || duration > MAX_SLEEP_LOG_DURATION_MILLIS) {
                continue
            }

            val overlapsExisting = logsInWindow.any {
                it.endAt > boundedStart && it.startAt < boundedEnd
            }
            if (overlapsExisting) {
                continue
            }

            val intervalKey = "${boundedStart}_${boundedEnd}"
            if (dismissedFallbackIntervals.contains(intervalKey) || prefs.dismissedSleepFallbacks.contains(intervalKey)) {
                continue
            }

            result.add(SleepInterval(startAt = boundedStart, endAt = boundedEnd))
        }

        result.sortBy { it.startAt }
        return result
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainrepositorypassivesleepsyncrepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/repository/PassiveSleepSyncRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/repository/PassiveSleepSyncRepository.kt`
- **Lines of Code:** 59
- **Size:** 1,885 bytes (1.8 KB)

```kotlin
package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for passive sleep session synchronization.
 * Abstracts Google Health Connect, wearable providers, or platform health services
 * to complement manual sleep logs and auto-default estimations with measured sleep.
 */
interface PassiveSleepSyncRepository {

    sealed interface SyncResult {
        data class Success(
            val importedLogs: List<SleepLogEntity>,
            val skippedDuplicates: Int,
            val replacedFallbackCount: Int
        ) : SyncResult

        data class PermissionRequired(
            val missingPermissions: Set<String>
        ) : SyncResult

        data class ProviderUnavailable(
            val reason: String
        ) : SyncResult

        data class Failure(
            val throwable: Throwable
        ) : SyncResult
    }

    data class RawSleepSession(
        val externalRecordId: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val sourceAppPackage: String = SOURCE_HEALTH_CONNECT,
        val stageSummary: String? = null,
        val notes: String = ""
    )

    enum class HealthProviderStatus {
        AVAILABLE,
        NOT_INSTALLED,
        UPDATE_REQUIRED,
        NOT_SUPPORTED
    }

    companion object {
        const val SOURCE_HEALTH_CONNECT = "HEALTH_CONNECT"
        const val PERMISSION_READ_SLEEP = "android.permission.health.READ_SLEEP"
    }

    fun observeProviderStatus(): Flow<HealthProviderStatus>
    suspend fun getProviderStatus(): HealthProviderStatus
    suspend fun hasRequiredPermissions(): Boolean
    suspend fun fetchExternalSleepSessions(fromMillis: Long, toMillis: Long): List<RawSleepSession>
    suspend fun syncPassiveSleep(fromMillis: Long, toMillis: Long): SyncResult
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainrepositoryenergymetricsrepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/repository/EnergyMetricsRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/repository/EnergyMetricsRepository.kt`
- **Lines of Code:** 191
- **Size:** 7,512 bytes (7.3 KB)

```kotlin
package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.NeuroFlowDatabase
import com.neuroflow.app.data.local.entity.EnergyPredictionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Phase 2: Repository for energy telemetry and audit trails.
 *
 * Manages local energy prediction snapshots for backtesting, calibration validation,
 * and non-invasive audit trails. Enforces retention policies (30-day raw, 90-day
 * aggregated metrics) to keep storage bounded.
 */
@Singleton
class EnergyMetricsRepository @Inject constructor(
    private val database: NeuroFlowDatabase
) {
    companion object {
        private const val RAW_DATA_RETENTION_DAYS = 30L
        const val MIN_RETENTION_DAYS = 1
        const val MAX_RETENTION_DAYS = 90
        const val RAW_DATA_RETENTION_MILLIS = RAW_DATA_RETENTION_DAYS * 24 * 60 * 60 * 1000L
    }

    private val energyPredictionDao = database.energyPredictionDao()

    /**
     * Record a new energy prediction snapshot for audit and backtesting.
     * Phase 2 telemetry capture: all component breakdowns are persisted.
     */
    suspend fun recordEnergyPrediction(
        predictedAtMillis: Long,
        baselineRawEnergy: Float,
        peakScore: Float,
        fatiguePenalty: Float,
        sleepPressurePoints: Int,
        fatiguePercent: Int,
        momentAdjustment: Float,
        momentConfidence: Float,
        momentSupportScore: Float = 50f,
        momentPressureScore: Float = 50f,
        adjustedRawEnergy: Float = peakScore,
        usableEnergy: Int,
        chronotype: String = "INTERMEDIATE",
        wakeUpHour: Int = 7,
        peakMinuteOfDay: Int = 540,
        peakConfidence: Float = 0.85f,
        peakDetectionAgeMillis: Long = 0L,
        sleepLogAgeMillis: Long = 0L,
        sessionDataAgeMillis: Long = 0L,
        recentFocusMinutes: Float = 0f,
        recentInterruptionCount: Int = 0,
        recentAppSwitchCount: Int = 0,
        activeTaskCount: Int = 0,
        notificationCount: Int = 0
    ) {
        val instant = java.time.Instant.ofEpochMilli(predictedAtMillis)
            .atZone(java.time.ZoneId.systemDefault())
        val dayOfWeek = instant.dayOfWeek.value    // 1=Monday .. 7=Sunday (ISO-8601)
        val hourOfDay = instant.hour

        val entity = EnergyPredictionEntity(
            id = UUID.randomUUID().toString(),
            predictedAtMillis = predictedAtMillis,
            predictedAtDayOfWeek = dayOfWeek,
            predictedAtHourOfDay = hourOfDay,
            peakDetectionAgeMillis = peakDetectionAgeMillis,
            sleepLogAgeMillis = sleepLogAgeMillis,
            sessionDataAgeMillis = sessionDataAgeMillis,
            baselineRawEnergy = baselineRawEnergy,
            peakScore = peakScore,
            fatiguePenalty = fatiguePenalty,
            sleepPressurePoints = sleepPressurePoints,
            fatiguePercent = fatiguePercent,
            momentAdjustment = momentAdjustment,
            momentConfidence = momentConfidence,
            momentSupportScore = momentSupportScore,
            momentPressureScore = momentPressureScore,
            adjustedRawEnergy = adjustedRawEnergy,
            usableEnergy = usableEnergy,
            chronotype = chronotype,
            wakeUpHour = wakeUpHour,
            peakMinuteOfDay = peakMinuteOfDay,
            peakConfidence = peakConfidence,
            recentFocusMinutes = recentFocusMinutes,
            recentInterruptionCount = recentInterruptionCount,
            recentAppSwitchCount = recentAppSwitchCount,
            activeTaskCount = activeTaskCount,
            notificationCount = notificationCount,
            createdAtMillis = System.currentTimeMillis()
        )

        energyPredictionDao.insert(entity)
    }

    /**
     * Get recent energy predictions for diagnostics and validation.
     */
    suspend fun getRecentPredictions(limit: Int = 100): List<EnergyPredictionEntity> {
        return energyPredictionDao.getRecent(limit)
    }

    /**
     * Get energy predictions within a time range (e.g., last 7 days).
     */
    suspend fun getPredictionsInRange(startMillis: Long, endMillis: Long): List<EnergyPredictionEntity> {
        return energyPredictionDao.getInRange(startMillis, endMillis)
    }

    /**
     * Observe recent predictions as a flow (for real-time backtesting updates).
     */
    fun observeRecentPredictions(afterMillis: Long): Flow<List<EnergyPredictionEntity>> {
        return energyPredictionDao.observeAfter(afterMillis)
    }

    /**
     * Get prediction count for storage monitoring.
     */
    suspend fun getPredictionCount(): Int {
        return energyPredictionDao.count()
    }

    /**
     * Clean up old energy predictions according to retention policy.
     * Phase 2 data hygiene: keep 30 days of raw data, aggregate beyond 30 days.
     *
     * Returns: count of records deleted
     */
    suspend fun enforceRetentionPolicy(
        retentionDays: Int = RAW_DATA_RETENTION_DAYS.toInt(),
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val safeRetentionDays = retentionDays.coerceIn(MIN_RETENTION_DAYS, MAX_RETENTION_DAYS)
        val cutoffMillis = nowMillis - (safeRetentionDays * 24L * 60L * 60L * 1000L)
        return energyPredictionDao.deleteOlderThan(cutoffMillis)
    }

    suspend fun clearAllPredictions(): Int {
        return energyPredictionDao.deleteAll()
    }

    /**
     * Get statistics for recent predictions (for aggregated analytics).
     */
    suspend fun getStatisticsForRange(startMillis: Long, endMillis: Long): PredictionStatistics {
        val predictions = energyPredictionDao.getInRange(startMillis, endMillis)
        if (predictions.isEmpty()) {
            return PredictionStatistics()
        }

        val avgBaseline = predictions.map { it.baselineRawEnergy }.average().toFloat()
        val avgAdjusted = predictions.map { it.adjustedRawEnergy }.average().toFloat()
        val avgMomentAdjustment = predictions.map { it.momentAdjustment }.average().toFloat()
        val avgMomentConfidence = predictions.map { it.momentConfidence }.average().toFloat()
        val avgFatiguePct = predictions.map { it.fatiguePercent }.average().roundToInt()
        val avgUsableEnergy = predictions.map { it.usableEnergy }.average().roundToInt()

        // Minute-of-day analysis for peak clarity
        val peaksByHour = predictions.groupBy { it.predictedAtHourOfDay }
            .mapValues { (_, hourPredictions) ->
                hourPredictions.map { it.adjustedRawEnergy }.average().toFloat()
            }

        return PredictionStatistics(
            sampleCount = predictions.size,
            avgBaselineEnergy = avgBaseline,
            avgAdjustedEnergy = avgAdjusted,
            avgMomentAdjustment = avgMomentAdjustment,
            avgMomentConfidence = avgMomentConfidence,
            avgFatiguePercent = avgFatiguePct,
            avgUsableEnergy = avgUsableEnergy,
            energyByHourOfDay = peaksByHour
        )
    }

    data class PredictionStatistics(
        val sampleCount: Int = 0,
        val avgBaselineEnergy: Float = 0f,
        val avgAdjustedEnergy: Float = 0f,
        val avgMomentAdjustment: Float = 0f,
        val avgMomentConfidence: Float = 0f,
        val avgFatiguePercent: Int = 0,
        val avgUsableEnergy: Int = 0,
        val energyByHourOfDay: Map<Int, Float> = emptyMap()
    )
}
```

---

<a id="appsrcmainjavacomneuroflowappdatasleepsleeptelemetrydatasourcekt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/sleep/SleepTelemetryDataSource.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/sleep/SleepTelemetryDataSource.kt`
- **Lines of Code:** 145
- **Size:** 5,776 bytes (5.6 KB)

```kotlin
package com.neuroflow.app.data.sleep

import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.domain.service.ChronobiologySignalNormalizer
import com.neuroflow.app.domain.service.NormalizedChronobiologySignal
import com.neuroflow.app.domain.service.RawSleepTelemetry
import com.neuroflow.app.domain.service.TimeSlotWindow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Priority 2 (ADR-003): Abstract Sleep Telemetry Boundary
 *
 * Provides a clean interface and default implementation decoupling domain and UI consumers
 * from Health Connect SDK dependencies while executing passive chronobiological normalization.
 */
interface SleepTelemetryRepository {
    suspend fun getLatestNormalizedSleep(
        baseWakeMillis: Long,
        baseSleepMinutes: Double = 480.0,
        lookbackMillis: Long = 24 * 60 * 60 * 1000L
    ): NormalizedChronobiologySignal

    suspend fun getNormalizedSleepForRange(
        fromMillis: Long,
        toMillis: Long,
        baseWakeMillis: Long,
        baseSleepMinutes: Double = 480.0
    ): NormalizedChronobiologySignal

    fun observeAllSleepLogs(): Flow<List<SleepLogEntity>>

    fun normalizeLogs(
        logs: List<SleepLogEntity>,
        baseWakeMillis: Long,
        baseSleepMinutes: Double = 480.0
    ): NormalizedChronobiologySignal
}

@Singleton
class DefaultSleepTelemetryRepository @Inject constructor(
    private val sleepLogRepository: SleepLogRepository,
    private val normalizer: ChronobiologySignalNormalizer
) : SleepTelemetryRepository {

    override fun observeAllSleepLogs(): Flow<List<SleepLogEntity>> =
        sleepLogRepository.observeAll()

    override suspend fun getLatestNormalizedSleep(
        baseWakeMillis: Long,
        baseSleepMinutes: Double,
        lookbackMillis: Long
    ): NormalizedChronobiologySignal {
        val now = System.currentTimeMillis()
        val toMillis = maxOf(baseWakeMillis, now)
        val fromMillis = toMillis - lookbackMillis
        return getNormalizedSleepForRange(fromMillis, toMillis, baseWakeMillis, baseSleepMinutes)
    }

    override suspend fun getNormalizedSleepForRange(
        fromMillis: Long,
        toMillis: Long,
        baseWakeMillis: Long,
        baseSleepMinutes: Double
    ): NormalizedChronobiologySignal {
        val logs = sleepLogRepository.getOverlapping(fromMillis, toMillis)
        return normalizeLogs(logs, baseWakeMillis, baseSleepMinutes)
    }

    override fun normalizeLogs(
        logs: List<SleepLogEntity>,
        baseWakeMillis: Long,
        baseSleepMinutes: Double
    ): NormalizedChronobiologySignal {
        val validLogs = logs.filter { it.endAt > it.startAt }.sortedBy { it.startAt }

        if (validLogs.isEmpty()) {
            return normalizer.normalize(
                telemetry = RawSleepTelemetry(
                    intervals = emptyList(),
                    confidence = 0.0
                ),
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = baseSleepMinutes
            )
        }

        val intervals = validLogs.map { TimeSlotWindow(it.startAt, it.endAt) }

        var totalDeep = 0.0
        var totalRem = 0.0
        var anyStageParsed = false

        for (log in validLogs) {
            val deepParsed = extractMinutes(log.notes, "deep")
            val remParsed = extractMinutes(log.notes, "rem")
            if (deepParsed != null || remParsed != null) {
                anyStageParsed = true
                totalDeep += deepParsed ?: 0.0
                totalRem += remParsed ?: 0.0
            }
        }

        val hasWearableSource = validLogs.any {
            it.source.contains("HEALTH", ignoreCase = true) || it.source.contains("WEARABLE", ignoreCase = true)
        }
        val confidence = when {
            hasWearableSource -> 1.0
            validLogs.any { it.source.contains("MANUAL", ignoreCase = true) || it.source.contains("USER", ignoreCase = true) } -> 0.85
            validLogs.any { it.source == "AUTO_DEFAULT" } -> 0.4
            else -> 0.85
        }

        val nocturnalLogs = validLogs.filter { it.startAt < baseWakeMillis || (it.endAt - it.startAt) > 180 * 60_000L }
        val effectiveNocturnalLogs = if (nocturnalLogs.isNotEmpty()) nocturnalLogs else emptyList()
        val (nocturnalConsolidated, nocturnalWaso) = normalizer.coalesceWasoEpisodes(effectiveNocturnalLogs.map { TimeSlotWindow(it.startAt, it.endAt) })
        val nocturnalRawMinutes = (nocturnalConsolidated.sumOf { (it.endMillis - it.startMillis) / 60_000.0 } - nocturnalWaso).coerceAtLeast(0.0)

        val finalDeep = if (anyStageParsed) totalDeep else if (hasWearableSource && effectiveNocturnalLogs.isNotEmpty()) nocturnalRawMinutes * 0.20 else 0.0
        val finalRem = if (anyStageParsed) totalRem else if (hasWearableSource && effectiveNocturnalLogs.isNotEmpty()) nocturnalRawMinutes * 0.20 else 0.0
        val latestWake = nocturnalLogs.maxOfOrNull { it.endAt }

        val telemetry = RawSleepTelemetry(
            intervals = intervals,
            deepSleepMinutes = finalDeep,
            remSleepMinutes = finalRem,
            detectedWakeMillis = latestWake,
            confidence = confidence
        )

        return normalizer.normalize(
            telemetry = telemetry,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = baseSleepMinutes
        )
    }

    private fun extractMinutes(notes: String, tag: String): Double? {
        val regex = Regex("""$tag[:=]\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val match = regex.find(notes) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdatarepositorysleeplogrepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/repository/SleepLogRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/repository/SleepLogRepository.kt`
- **Lines of Code:** 51
- **Size:** 1,646 bytes (1.6 KB)

```kotlin
package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.dao.SleepLogDao
import com.neuroflow.app.data.local.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepLogRepository @Inject constructor(
    private val sleepLogDao: SleepLogDao
) {

    companion object {
        private const val MIN_DURATION_MILLIS = 60_000L
    }

    fun observeAll(): Flow<List<SleepLogEntity>> = sleepLogDao.observeAll()

    suspend fun getAll(): List<SleepLogEntity> = sleepLogDao.getAll()

    suspend fun getOverlapping(fromMillis: Long, toMillis: Long): List<SleepLogEntity> =
        sleepLogDao.getOverlapping(fromMillis, toMillis)

    suspend fun addLog(
        startAt: Long,
        endAt: Long,
        source: String = "MANUAL",
        notes: String = ""
    ): SleepLogEntity {
        val safeStart = minOf(startAt, endAt)
        val safeEnd = maxOf(startAt, endAt)
        val normalizedEnd = if (safeEnd <= safeStart) safeStart + MIN_DURATION_MILLIS else safeEnd
        val durationMinutes = ((normalizedEnd - safeStart) / 60_000L).toInt().coerceAtLeast(1)
        val log = SleepLogEntity(
            startAt = safeStart,
            endAt = normalizedEnd,
            durationMinutes = durationMinutes,
            source = source,
            notes = notes
        )
        sleepLogDao.upsert(log)
        return log
    }

    suspend fun getById(id: String): SleepLogEntity? = sleepLogDao.getById(id)

    suspend fun deleteById(id: String) = sleepLogDao.deleteById(id)

    suspend fun deleteAll() = sleepLogDao.deleteAll()
}
```

---

<a id="appsrcmainjavacomneuroflowappdatarepositorypassivesleepsyncmanagerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/repository/PassiveSleepSyncManager.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/repository/PassiveSleepSyncManager.kt`
- **Lines of Code:** 143
- **Size:** 6,116 bytes (6.0 KB)

```kotlin
package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.Companion.PERMISSION_READ_SLEEP
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.Companion.SOURCE_HEALTH_CONNECT
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.HealthProviderStatus
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.RawSleepSession
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.SyncResult
import com.neuroflow.app.domain.repository.SleepPressureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassiveSleepSyncManager @Inject constructor(
    private val sleepLogRepository: SleepLogRepository,
    private val sleepPressureRepository: SleepPressureRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : PassiveSleepSyncRepository {

    private val providerStatusFlow = MutableStateFlow(HealthProviderStatus.AVAILABLE)
    private val externalProviderSource = mutableListOf<RawSleepSession>()

    /**
     * Injects mock or bridged external sessions (e.g. from Health Connect API or wearable SDK).
     */
    fun setExternalSessions(sessions: List<RawSleepSession>) {
        externalProviderSource.clear()
        externalProviderSource.addAll(sessions)
    }

    override fun observeProviderStatus(): Flow<HealthProviderStatus> = providerStatusFlow.asStateFlow()

    override suspend fun getProviderStatus(): HealthProviderStatus = providerStatusFlow.value

    fun setProviderStatus(status: HealthProviderStatus) {
        providerStatusFlow.value = status
    }

    override suspend fun hasRequiredPermissions(): Boolean {
        return providerStatusFlow.value == HealthProviderStatus.AVAILABLE
    }

    override suspend fun fetchExternalSleepSessions(fromMillis: Long, toMillis: Long): List<RawSleepSession> {
        val safeStart = minOf(fromMillis, toMillis)
        val safeEnd = maxOf(fromMillis, toMillis)
        return externalProviderSource.filter { session ->
            session.endTimeMillis > safeStart && session.startTimeMillis < safeEnd
        }
    }

    override suspend fun syncPassiveSleep(fromMillis: Long, toMillis: Long): SyncResult {
        try {
            val status = getProviderStatus()
            if (status != HealthProviderStatus.AVAILABLE) {
                return SyncResult.ProviderUnavailable("Health provider unavailable: $status")
            }

            if (!hasRequiredPermissions()) {
                return SyncResult.PermissionRequired(setOf(PERMISSION_READ_SLEEP))
            }

            val externalSessions = fetchExternalSleepSessions(fromMillis, toMillis)
            if (externalSessions.isEmpty()) {
                return SyncResult.Success(
                    importedLogs = emptyList(),
                    skippedDuplicates = 0,
                    replacedFallbackCount = 0
                )
            }

            val imported = mutableListOf<SleepLogEntity>()
            var skipped = 0
            var replacedFallback = 0

            for (session in externalSessions) {
                val safeStart = minOf(session.startTimeMillis, session.endTimeMillis)
                val safeEnd = maxOf(session.startTimeMillis, session.endTimeMillis)
                val duration = safeEnd - safeStart

                // Bounds validation
                if (duration < SleepPressureRepository.MIN_SLEEP_LOG_DURATION_MILLIS ||
                    duration > SleepPressureRepository.MAX_SLEEP_LOG_DURATION_MILLIS
                ) {
                    skipped++
                    continue
                }

                val overlapping = sleepLogRepository.getOverlapping(safeStart, safeEnd)

                // 1. Manual sleep logs take precedence over passive detection
                val manualOverlapping = overlapping.filter { it.source == "MANUAL" }
                if (manualOverlapping.isNotEmpty()) {
                    skipped++
                    continue
                }

                // 2. Health Connect deduplication (avoid importing identical/near-identical session twice)
                val healthOverlapping = overlapping.filter { it.source == SOURCE_HEALTH_CONNECT }
                val isDuplicate = healthOverlapping.any { existing ->
                    val overlap = minOf(existing.endAt, safeEnd) - maxOf(existing.startAt, safeStart)
                    overlap > 0.8f * duration
                }
                if (isDuplicate) {
                    skipped++
                    continue
                }

                // 3. Health Connect measured sleep supersedes AUTO_DEFAULT fallback estimations
                val autoOverlapping = overlapping.filter { it.source == "AUTO_DEFAULT" }
                autoOverlapping.forEach { autoLog ->
                    sleepLogRepository.deleteById(autoLog.id)
                    replacedFallback++
                }

                val note = if (session.notes.isNotBlank()) session.notes else "Synced via Health Connect"
                val addedLog = sleepLogRepository.addLog(
                    startAt = safeStart,
                    endAt = safeEnd,
                    source = SOURCE_HEALTH_CONNECT,
                    notes = note
                )
                imported.add(addedLog)
            }

            if (imported.isNotEmpty() || replacedFallback > 0) {
                sleepPressureRepository.refreshCurrentPressure()
            }

            return SyncResult.Success(
                importedLogs = imported,
                skippedDuplicates = skipped,
                replacedFallbackCount = replacedFallback
            )
        } catch (e: Exception) {
            return SyncResult.Failure(e)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappworkersleeppressurerefreshworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/SleepPressureRefreshWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/SleepPressureRefreshWorker.kt`
- **Lines of Code:** 80
- **Size:** 2,993 bytes (2.9 KB)

```kotlin
package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.repository.SleepPressureRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Daily worker that refreshes sleep pressure and creates automatic fallback sleep logs.
 *
 * This ensures that even if the user doesn't open the app, automatic sleep logs
 * will be created based on their configured sleep/wake hours when appropriate.
 *
 * Runs once daily at a configured time (typically after wake-up time + 12 hours).
 */
@HiltWorker
class SleepPressureRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sleepPressureRepository: SleepPressureRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Refresh current pressure, which will trigger automatic fallback
            // sleep log creation if conditions are met
            sleepPressureRepository.refreshCurrentPressure()
            Result.success()
        } catch (e: Exception) {
            // Retry on failure, but don't fail permanently
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "sleep_pressure_refresh_work"
        const val WORK_TAG = "sleep_pressure_refresh"
    }
}
/**
 * Schedules the daily sleep pressure refresh worker.
 *
 * The worker runs daily at 7 PM (19:00) by default, which is typically 12+ hours
 * after most users' wake time, allowing the automatic fallback logic to trigger.
 *
 * @param context Android context
 * @param prefs User preferences (currently unused, but included for consistency with other schedulers)
 */
fun scheduleSleepPressureRefreshWorker(context: Context, prefs: UserPreferences) {
    val workManager = WorkManager.getInstance(context)

    // Schedule to run at 7 PM daily (19:00)
    // This is typically 12+ hours after wake time for most users,
    // allowing automatic fallback sleep log creation to trigger
    val targetHour = 19
    val initialDelayMs = delayUntilHour(targetHour)

    val sleepRefreshRequest = PeriodicWorkRequestBuilder<SleepPressureRefreshWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
        .addTag(SleepPressureRefreshWorker.WORK_TAG)
        .build()

    workManager.enqueueUniquePeriodicWork(
        SleepPressureRefreshWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        sleepRefreshRequest
    )
}
```

---

<a id="appsrcmainjavacomneuroflowappworkerenergytelemetrycleanupworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/EnergyTelemetryCleanupWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/EnergyTelemetryCleanupWorker.kt`
- **Lines of Code:** 81
- **Size:** 2,977 bytes (2.9 KB)

```kotlin
package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.domain.repository.EnergyMetricsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope

/**
 * Phase 2: Background worker for energy telemetry maintenance.
 * 
 * Periodically enforces retention policies on energy prediction data:
 * - Deletes raw telemetry older than 30 days
 * - Runs daily (low frequency to avoid battery drain)
 * - Automatically restarted by WorkManager on device reboot
 */
@HiltWorker
class EnergyTelemetryCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val energyMetricsRepository: EnergyMetricsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        return@coroutineScope try {
            // Enforce retention policy: delete telemetry older than 30 days
            val deletedCount = energyMetricsRepository.enforceRetentionPolicy()
            
            // Log for debugging but don't spam logs
            if (deletedCount > 0) {
                android.util.Log.d(
                    "EnergyTelemetry",
                    "Cleanup: deleted $deletedCount prediction records older than 30 days"
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("EnergyTelemetry", "Cleanup worker failed", e)
            // Retry with exponential backoff on failures
            Result.retry()
        }
    }
}

/**
 * Enqueues the energy telemetry cleanup worker.
 * Should be called once on app startup (e.g., from a One-Time Work initializer).
 */
@Singleton
class EnergyTelemetryCleanupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<EnergyTelemetryCleanupWorker>(
            24, TimeUnit.HOURS,
            5, TimeUnit.MINUTES  // Flex interval: run anytime within 5 minutes of 24hr mark
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)  // Only run when battery is sufficient
                .build()
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "energy_telemetry_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,  // Don't reschedule if already exists
            cleanupRequest
        )
    }
}
```

---

