package com.neuroflow.app.domain.engine

import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Baseline peak-energy engine built from the user's MEQ chronotype result.
 *
 * The peak time is derived from the user's wake-up time plus a chronotype offset:
 *  - definite morning: right after waking (0 hours)
 *  - moderate morning: 2.5 hours after waking
 *  - intermediate: 7 hours after waking
 *  - moderate evening: 10 hours after waking
 *  - definite evening: 12.6 hours after waking
 *
 * The resulting baseline peak is a single time point with a nominal peak value of 4000.
 */
object PeakEnergyEngine {

    const val PEAK_VALUE = 4000
    private const val MINUTES_PER_DAY = 24 * 60
    private const val SOFT_MAX_SLEEP_PRESSURE = 3000f
    private const val SLOT_BUCKET_MINUTES = 60

    data class PeakWindow(
        val startMinuteOffset: Int,
        val durationMinutes: Int,
        val amplitude: Float
    )

    data class CircadianProfile(
        val windows: List<PeakWindow>,
        val phaseShiftMinutes: Int = 0
    )

    enum class ProfileType {
        WORKDAY,
        WEEKEND
    }

    data class ConfidenceComponents(
        val sleepCoverage: Float,
        val wakeConsistency: Float,
        val behaviorPerformance: Float,
        val overall: Float
    )

    data class SlotPerformanceAggregate(
        val bucketStartMinute: Int,
        val qualityWeightedCompletionRate: Float,
        val abortRate: Float,
        val distractionRate: Float,
        val sampleCount: Int
    )

    data class TaskTypePerformanceAggregate(
        val taskType: String,
        val averageBestBucketMinute: Int,
        val weightedQuality: Float,
        val sampleCount: Int
    )

    data class TuningCoefficients(
        val sleepWeight: Float = 0.30f,
        val wakeWeight: Float = 0.25f,
        val behaviorWeight: Float = 0.25f,
        val baseWeight: Float = 0.20f
    )

    data class ProfileOverride(
        val enabled: Boolean = false,
        val profileType: ProfileType? = null,
        val anchorMinuteOfDay: Int? = null,
        val windows: List<PeakWindow> = emptyList()
    )

    data class MorningPersonalizationSignals(
        val averageSleepMinutes: Int? = null,
        val wakeVarianceMinutes: Int? = null,
        val sleepLogCoverage: Float = 0f,
        val profileType: ProfileType = ProfileType.WORKDAY,
        val baselineAnchorMinuteOfDay: Int = 150,
        val behaviorSlots: List<SlotPerformanceAggregate> = emptyList(),
        val taskTypePerformance: List<TaskTypePerformanceAggregate> = emptyList(),
        val behaviorCoverage: Float = 0f,
        val recentWeight: Float = 1f,
        val coldStartFactor: Float = 1f,
        val driftMinutes: Int = 0,
        val weeklyBacktestErrorMinutes: Float? = null,
        val tuning: TuningCoefficients = TuningCoefficients(),
        val profileOverride: ProfileOverride? = null
    )

    data class EffectivePeakProfile(
        val profileType: ProfileType,
        val anchorMinuteOfDay: Int,
        val windows: List<PeakWindow>,
        val confidence: ConfidenceComponents,
        val explanation: String,
        val driftStatus: String = "stable",
        val weeklyBacktestErrorMinutes: Float? = null
    )

    data class DetectionResult(
        val chronotype: MEQChronotypeDetector.Chronotype,
        val wakeUpHour: Int,
        val peakOffsetHours: Float,
        val peakHourOfDay: Int,
        val peakMinuteOfDay: Int,
        val peakValue: Int = PEAK_VALUE,
        val confidence: Float,
        val circadianProfile: CircadianProfile = defaultCircadianProfile(chronotype),
        val effectiveProfile: EffectivePeakProfile = defaultEffectiveProfile(
            chronotype = chronotype,
            peakMinuteOfDay = peakMinuteOfDay
        )
    ) {

        /**
         * Returns the current peak-energy value, decreasing by 1 point per minute after
         * the assigned peak time. The value resets to [peakValue] at the next daily peak
         * timepoint, rather than needing to decay all the way to zero.
         */
        fun currentValueAt(nowMillis: Long = System.currentTimeMillis()): Int {
            val nowMinuteOfDay = minuteOfDay(nowMillis)
            val minutesSincePeak = minutesSincePeak(nowMinuteOfDay)
            return peakValue - minutesSincePeak
        }

        /** Minutes elapsed since the most recent peak occurrence. */
        fun minutesSincePeak(nowMillis: Long = System.currentTimeMillis()): Int {
            val nowMinuteOfDay = minuteOfDay(nowMillis)
            return minutesSincePeak(nowMinuteOfDay)
        }

        private fun minutesSincePeak(nowMinuteOfDay: Int): Int {
            return if (nowMinuteOfDay >= peakMinuteOfDay) {
                nowMinuteOfDay - peakMinuteOfDay
            } else {
                nowMinuteOfDay + (24 * 60) - peakMinuteOfDay
            }
        }
    }

    fun detect(
        meqResult: MEQChronotypeDetector.Result,
        wakeUpHour: Int,
        sleepHour: Int? = null,
        sleepPressurePoints: Int = 0,
        personalizationSignals: MorningPersonalizationSignals? = null
    ): DetectionResult {
        val profile = buildCircadianProfile(
            chronotype = meqResult.chronotype,
            wakeUpHour = wakeUpHour,
            sleepHour = sleepHour,
            sleepPressurePoints = sleepPressurePoints,
            signals = personalizationSignals
        )
        val override = personalizationSignals?.profileOverride
        val offsetHours = (chronotypeOffset(meqResult.chronotype) + profile.phaseShiftMinutes / 60f)
        val normalizedWakeHour = normalizeHour(wakeUpHour)
        val peakMinuteOfDay = override?.anchorMinuteOfDay?.takeIf { override.enabled }?.let(::normalizeMinuteOfDay)
            ?: (((normalizedWakeHour * 60) + (offsetHours * 60f).roundToInt()) % MINUTES_PER_DAY)
        val confidenceComponents = confidenceComponents(
            baseConfidence = meqResult.confidence,
            chronotype = meqResult.chronotype,
            wakeUpHour = wakeUpHour,
            sleepHour = sleepHour,
            sleepPressurePoints = sleepPressurePoints,
            signals = personalizationSignals
        )
        val effectiveProfile = buildEffectiveProfile(
            chronotype = meqResult.chronotype,
            peakMinuteOfDay = peakMinuteOfDay,
            profile = profile,
            confidence = confidenceComponents,
            signals = personalizationSignals
        )

        return DetectionResult(
            chronotype = meqResult.chronotype,
            wakeUpHour = normalizedWakeHour,
            peakOffsetHours = offsetHours,
            peakHourOfDay = peakMinuteOfDay / 60,
            peakMinuteOfDay = peakMinuteOfDay,
            peakValue = PEAK_VALUE,
            confidence = confidenceComponents.overall,
            circadianProfile = profile,
            effectiveProfile = effectiveProfile
        )
    }

    fun chronotypeOffset(chronotype: MEQChronotypeDetector.Chronotype): Float {
        return when (chronotype) {
            MEQChronotypeDetector.Chronotype.DEFINITE_MORNING -> 0f
            MEQChronotypeDetector.Chronotype.MODERATE_MORNING -> 2.5f
            MEQChronotypeDetector.Chronotype.INTERMEDIATE -> 7f
            MEQChronotypeDetector.Chronotype.MODERATE_EVENING -> 10f
            MEQChronotypeDetector.Chronotype.DEFINITE_EVENING -> 12.6f
        }
    }

    fun baselinePeakMinuteOfDay(meqResult: MEQChronotypeDetector.Result, wakeUpHour: Int): Int {
        return detect(meqResult, wakeUpHour).peakMinuteOfDay
    }

    fun defaultCircadianProfile(chronotype: MEQChronotypeDetector.Chronotype): CircadianProfile {
        return when (chronotype) {
            MEQChronotypeDetector.Chronotype.DEFINITE_MORNING,
            MEQChronotypeDetector.Chronotype.MODERATE_MORNING -> CircadianProfile(
                windows = listOf(
                    PeakWindow(startMinuteOffset = 0, durationMinutes = 210, amplitude = 1.0f),
                    PeakWindow(startMinuteOffset = 570, durationMinutes = 150, amplitude = 0.8f),
                    PeakWindow(startMinuteOffset = 810, durationMinutes = 60, amplitude = 0.6f)
                )
            )
            else -> CircadianProfile(
                windows = listOf(
                    PeakWindow(startMinuteOffset = 0, durationMinutes = 180, amplitude = 1.0f),
                    PeakWindow(startMinuteOffset = 600, durationMinutes = 140, amplitude = 0.75f),
                    PeakWindow(startMinuteOffset = 840, durationMinutes = 50, amplitude = 0.55f)
                )
            )
        }
    }

    fun defaultEffectiveProfile(
        chronotype: MEQChronotypeDetector.Chronotype,
        peakMinuteOfDay: Int
    ): EffectivePeakProfile {
        val confidence = ConfidenceComponents(
            sleepCoverage = 0.5f,
            wakeConsistency = 0.5f,
            behaviorPerformance = 0.5f,
            overall = 0.5f
        )
        return EffectivePeakProfile(
            profileType = ProfileType.WORKDAY,
            anchorMinuteOfDay = normalizeMinuteOfDay(peakMinuteOfDay),
            windows = defaultCircadianProfile(chronotype).windows,
            confidence = confidence,
            explanation = "Baseline profile"
        )
    }

    private fun buildCircadianProfile(
        chronotype: MEQChronotypeDetector.Chronotype,
        wakeUpHour: Int,
        sleepHour: Int?,
        sleepPressurePoints: Int,
        signals: MorningPersonalizationSignals?
    ): CircadianProfile {
        val override = signals?.profileOverride
        val base = if (override?.enabled == true && override.windows.isNotEmpty()) {
            CircadianProfile(
                windows = override.windows.map { w ->
                    w.copy(
                        durationMinutes = w.durationMinutes.coerceIn(30, 6 * 60),
                        amplitude = w.amplitude.coerceIn(0.2f, 1f)
                    )
                }
            )
        } else {
            defaultCircadianProfile(chronotype)
        }
        if (override?.enabled == true) {
            return base.copy(phaseShiftMinutes = 0)
        }
        if (chronotype != MEQChronotypeDetector.Chronotype.DEFINITE_MORNING &&
            chronotype != MEQChronotypeDetector.Chronotype.MODERATE_MORNING
        ) {
            return base
        }

        val normalizedWake = normalizeHour(wakeUpHour)
        val normalizedSleep = sleepHour?.let(::normalizeHour)
        val preferenceSleepDurationHours = if (normalizedSleep != null) {
            (((normalizedWake - normalizedSleep + 24) % 24).toFloat()).coerceIn(3f, 12f)
        } else {
            8f
        }
        val observedSleepDurationHours = signals?.averageSleepMinutes
            ?.coerceIn(180, 720)
            ?.div(60f)
        val observedWeight = (signals?.sleepLogCoverage ?: 0f).coerceIn(0f, 1f)
        val sleepDurationHours = (
            (preferenceSleepDurationHours * (1f - observedWeight)) +
                ((observedSleepDurationHours ?: preferenceSleepDurationHours) * observedWeight)
            ).coerceIn(3f, 12f)

        val sleepDebtHours = (7.5f - sleepDurationHours).coerceAtLeast(0f)
        val oversleepHours = (sleepDurationHours - 9f).coerceAtLeast(0f)
        val pressureRatio = (sleepPressurePoints.coerceAtLeast(0).toFloat() / SOFT_MAX_SLEEP_PRESSURE).coerceIn(0f, 1f)
        val wakeVarianceRatio = (
            (signals?.wakeVarianceMinutes?.coerceAtLeast(0)?.toFloat() ?: 0f) / 120f
            ).coerceIn(0f, 1f)
        val baselineAnchor = normalizeMinuteOfDay(signals?.baselineAnchorMinuteOfDay ?: 150)
        val behaviorShift = behaviorDrivenShiftMinutes(
            slots = signals?.behaviorSlots ?: emptyList(),
            baselineAnchorMinute = baselineAnchor
        )
        val taskTypeShift = taskTypeShiftMinutes(
            perf = signals?.taskTypePerformance ?: emptyList(),
            baselineAnchorMinute = baselineAnchor
        )
        val coldStartDampening = (signals?.coldStartFactor ?: 1f).coerceIn(0.3f, 1f)
        val driftShift = (signals?.driftMinutes ?: 0).coerceIn(-35, 35)

        val phaseShiftMinutes = (
            (sleepDebtHours * 18f) +
                (pressureRatio * 20f) -
                (oversleepHours * 10f) +
                (wakeVarianceRatio * 26f) +
                ((behaviorShift + taskTypeShift) * coldStartDampening) +
                driftShift
            ).roundToInt().coerceIn(-35, 110)

        val amplitudeScale = (
            1f -
                (sleepDebtHours * 0.06f) -
                (pressureRatio * 0.1f) -
                (wakeVarianceRatio * 0.08f)
            ).coerceIn(0.65f, 1f)

        return CircadianProfile(
            windows = base.windows.mapIndexed { index, window ->
                val relativeWeight = when (index) {
                    0 -> 1f
                    1 -> 0.9f
                    else -> 0.8f
                }
                window.copy(amplitude = (window.amplitude * amplitudeScale * relativeWeight).coerceIn(0.35f, 1f))
            },
            phaseShiftMinutes = phaseShiftMinutes
        )
    }

    private fun confidenceComponents(
        baseConfidence: Float,
        chronotype: MEQChronotypeDetector.Chronotype,
        wakeUpHour: Int,
        sleepHour: Int?,
        sleepPressurePoints: Int,
        signals: MorningPersonalizationSignals?
    ): ConfidenceComponents {
        val normalizedBase = baseConfidence.coerceIn(0f, 1f)
        if (chronotype != MEQChronotypeDetector.Chronotype.DEFINITE_MORNING &&
            chronotype != MEQChronotypeDetector.Chronotype.MODERATE_MORNING
        ) {
            return ConfidenceComponents(
                sleepCoverage = normalizedBase,
                wakeConsistency = normalizedBase,
                behaviorPerformance = normalizedBase,
                overall = normalizedBase
            )
        }

        val sleepDurationHours = sleepHour?.let {
            val wake = normalizeHour(wakeUpHour)
            val sleep = normalizeHour(it)
            (((wake - sleep + 24) % 24).toFloat()).coerceIn(3f, 12f)
        } ?: 8f

        val durationPenalty = when {
            sleepDurationHours in 7f..9f -> 0f
            sleepDurationHours in 6f..10f -> 0.07f
            else -> 0.14f
        }
        val pressurePenalty = ((sleepPressurePoints.coerceAtLeast(0).toFloat() / SOFT_MAX_SLEEP_PRESSURE) * 0.12f)
            .coerceIn(0f, 0.12f)
        val sleepCoverageScore = ((signals?.sleepLogCoverage ?: 0f) * 0.65f + (1f - durationPenalty) * 0.35f)
            .coerceIn(0f, 1f)
        val wakeConsistencyScore = (1f - ((signals?.wakeVarianceMinutes ?: 180).coerceAtLeast(0).toFloat() / 180f))
            .coerceIn(0f, 1f)
        val behaviorPerformanceScore = behaviorConfidence(signals)

        val tuning = signals?.tuning ?: TuningCoefficients()
        val normalizedTuning = normalizeTuning(tuning)
        val overall = (
            normalizedBase * normalizedTuning.baseWeight +
                sleepCoverageScore * normalizedTuning.sleepWeight +
                wakeConsistencyScore * normalizedTuning.wakeWeight +
                behaviorPerformanceScore * normalizedTuning.behaviorWeight -
                pressurePenalty
            ).coerceIn(0.45f, 1f)

        return ConfidenceComponents(
            sleepCoverage = sleepCoverageScore,
            wakeConsistency = wakeConsistencyScore,
            behaviorPerformance = behaviorPerformanceScore,
            overall = overall
        )
    }

    private fun behaviorConfidence(signals: MorningPersonalizationSignals?): Float {
        if (signals == null) return 0.5f
        val coverage = signals.behaviorCoverage.coerceIn(0f, 1f)
        if (signals.behaviorSlots.isEmpty()) return (0.35f + coverage * 0.35f).coerceIn(0f, 1f)
        val qualityAvg = signals.behaviorSlots
            .map { slot ->
                (slot.qualityWeightedCompletionRate * 0.6f) +
                    ((1f - slot.abortRate) * 0.2f) +
                    ((1f - slot.distractionRate) * 0.2f)
            }
            .average()
            .toFloat()
            .coerceIn(0f, 1f)
        return (qualityAvg * 0.75f + coverage * 0.25f).coerceIn(0f, 1f)
    }

    private fun behaviorDrivenShiftMinutes(
        slots: List<SlotPerformanceAggregate>,
        baselineAnchorMinute: Int
    ): Float {
        if (slots.isEmpty()) return 0f
        val topSlot = slots.maxByOrNull {
            (it.qualityWeightedCompletionRate * 0.6f) +
                ((1f - it.abortRate) * 0.25f) +
                ((1f - it.distractionRate) * 0.15f)
        } ?: return 0f
        val delta = minuteDelta(topSlot.bucketStartMinute, baselineAnchorMinute)
        return (delta.toFloat() * 0.35f).coerceIn(-40f, 50f)
    }

    private fun taskTypeShiftMinutes(
        perf: List<TaskTypePerformanceAggregate>,
        baselineAnchorMinute: Int
    ): Float {
        if (perf.isEmpty()) return 0f
        val weighted = perf.sumOf {
            val weight = it.weightedQuality.coerceIn(0f, 1f)
            minuteDelta(it.averageBestBucketMinute, baselineAnchorMinute).toDouble() * weight
        }
        val totalWeight = perf.sumOf { it.weightedQuality.coerceIn(0f, 1f).toDouble() }.coerceAtLeast(1e-6)
        return ((weighted / totalWeight).toFloat() * 0.18f).coerceIn(-25f, 25f)
    }

    private fun normalizeTuning(input: TuningCoefficients): TuningCoefficients {
        val sleep = input.sleepWeight.coerceIn(0.1f, 0.6f)
        val wake = input.wakeWeight.coerceIn(0.1f, 0.5f)
        val behavior = input.behaviorWeight.coerceIn(0.1f, 0.6f)
        val base = input.baseWeight.coerceIn(0.05f, 0.5f)
        val total = (sleep + wake + behavior + base).coerceAtLeast(1e-6f)
        return TuningCoefficients(
            sleepWeight = sleep / total,
            wakeWeight = wake / total,
            behaviorWeight = behavior / total,
            baseWeight = base / total
        )
    }

    private fun buildEffectiveProfile(
        chronotype: MEQChronotypeDetector.Chronotype,
        peakMinuteOfDay: Int,
        profile: CircadianProfile,
        confidence: ConfidenceComponents,
        signals: MorningPersonalizationSignals?
    ): EffectivePeakProfile {
        val profileOverride = signals?.profileOverride
        val profileType = profileOverride?.profileType ?: signals?.profileType ?: ProfileType.WORKDAY
        val explanation = if (profileOverride?.enabled == true) {
            "Manual profile override active"
        } else if (isMorningChronotype(chronotype)) {
            val components = listOf(
                "sleep ${(confidence.sleepCoverage * 100).toInt()}%",
                "wake ${(confidence.wakeConsistency * 100).toInt()}%",
                "behavior ${(confidence.behaviorPerformance * 100).toInt()}%"
            )
            "Adaptive morning profile (${profileType.name.lowercase()}): ${components.joinToString(", ")}"
        } else {
            "Baseline non-morning profile"
        }
        val driftStatus = when {
            (signals?.driftMinutes ?: 0) >= 18 -> "later_drift"
            (signals?.driftMinutes ?: 0) <= -18 -> "earlier_drift"
            else -> "stable"
        }
        return EffectivePeakProfile(
            profileType = profileType,
            anchorMinuteOfDay = normalizeMinuteOfDay(peakMinuteOfDay),
            windows = profile.windows,
            confidence = confidence,
            explanation = explanation,
            driftStatus = driftStatus,
            weeklyBacktestErrorMinutes = signals?.weeklyBacktestErrorMinutes
        )
    }

    private fun isMorningChronotype(chronotype: MEQChronotypeDetector.Chronotype): Boolean {
        return chronotype == MEQChronotypeDetector.Chronotype.DEFINITE_MORNING ||
            chronotype == MEQChronotypeDetector.Chronotype.MODERATE_MORNING
    }

    fun bucketMinuteOfDay(timestampMillis: Long): Int {
        val minute = minuteOfDay(timestampMillis)
        return (minute / SLOT_BUCKET_MINUTES) * SLOT_BUCKET_MINUTES
    }

    private fun normalizeMinuteOfDay(minute: Int): Int {
        val normalized = minute % MINUTES_PER_DAY
        return if (normalized < 0) normalized + MINUTES_PER_DAY else normalized
    }

    private fun minuteDelta(targetMinute: Int, referenceMinute: Int): Int {
        val t = normalizeMinuteOfDay(targetMinute)
        val r = normalizeMinuteOfDay(referenceMinute)
        val forward = (t - r + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val backward = forward - MINUTES_PER_DAY
        return if (kotlin.math.abs(forward) <= kotlin.math.abs(backward)) forward else backward
    }

    private fun minuteOfDay(nowMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun normalizeHour(hour: Int): Int {
        val normalized = hour % 24
        return if (normalized < 0) normalized + 24 else normalized
    }
}