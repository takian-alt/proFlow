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
    val consolidatedSessions: List<TimeSlotWindow>
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
        remSleepMinutes: Double
    ): Double {
        if (totalMinutes <= 0.0) return 0.0
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
     * Full normalization pipeline aggregating telemetry into normalized signal.
     */
    fun normalize(
        telemetry: RawSleepTelemetry,
        baseWakeMillis: Long,
        baseSleepMinutes: Double
    ): NormalizedChronobiologySignal {
        val (consolidated, wasoMinutes) = coalesceWasoEpisodes(telemetry.intervals)
        val rawSleepMinutes = if (telemetry.intervals.isNotEmpty()) {
            consolidated.sumOf { (it.endMillis - it.startMillis) / 60_000.0 } - wasoMinutes
        } else {
            baseSleepMinutes
        }.coerceAtLeast(0.0)

        val effectiveDuration = computeEffectiveDuration(
            totalMinutes = rawSleepMinutes,
            deepSleepMinutes = telemetry.deepSleepMinutes,
            remSleepMinutes = telemetry.remSleepMinutes
        )

        val blendedDuration = blendDuration(
            effectiveDurationMinutes = effectiveDuration,
            baseDurationMinutes = baseSleepMinutes,
            confidence = telemetry.confidence
        )

        val wearableWake = telemetry.detectedWakeMillis ?: consolidated.lastOrNull()?.endMillis ?: baseWakeMillis
        val normalizedWake = clampWakeShift(
            baseWakeMillis = baseWakeMillis,
            wearableWakeMillis = wearableWake,
            confidence = telemetry.confidence
        )

        return NormalizedChronobiologySignal(
            totalSleepMinutes = rawSleepMinutes,
            effectiveDurationMinutes = effectiveDuration,
            blendedDurationMinutes = blendedDuration,
            normalizedWakeTimeMillis = normalizedWake,
            totalWasoMinutes = wasoMinutes.toDouble(),
            consolidatedSessions = consolidated
        )
    }
}
