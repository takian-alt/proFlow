package com.neuroflow.app.domain.engine

import kotlin.math.PI
import kotlin.math.cos

/**
 * Combines peak-energy and sleep-pressure signals into a single energy score.
 */
object EnergyScoreEngine {

    private const val MINUTES_PER_DAY = 24 * 60
    private const val PERCENT_SCALE = 100f

    // Morning-profile circadian windows (relative to personal peak anchor).
    private const val FIRST_PEAK_START_MINUTES = 0
    private const val SECOND_PEAK_START_MINUTES = 570   // 9.5h
    private const val THIRD_PEAK_START_MINUTES = 810    // 13.5h

    private const val FIRST_PEAK_DURATION_MINUTES = 210 // 3.5h
    private const val SECOND_PEAK_DURATION_MINUTES = 150 // 2.5h
    private const val THIRD_PEAK_DURATION_MINUTES = 60  // 1h

    private const val FIRST_PEAK_AMPLITUDE = 1.0f
    private const val SECOND_PEAK_AMPLITUDE = 0.8f
    private const val THIRD_PEAK_AMPLITUDE = 0.6f

    data class ScoreResult(
        val rawEnergy: Float,
        val usableEnergy: Float,
        val peakScore: Float,
        val fatiguePenalty: Float,
        val reservoirFactor: Float,
        val circadianFactor: Float,
        val confidenceFactor: Float,
        val sleepPressureRatio: Float
    )

    data class EnergySnapshot(
        val peakEnergy: PeakEnergyEngine.DetectionResult? = null,
        val sleepPressurePoints: Int = 0,
        val nowMillis: Long = System.currentTimeMillis(),
        val softMaxReference: Int = SleepPressureDetector.SOFT_MAX_REFERENCE
    )

    /**
     * Backward-compatible entry point returning signed raw energy in [-100, 100].
     */
    fun calculate(snapshot: EnergySnapshot): Float {
        return calculateDetailed(snapshot).rawEnergy
    }

    fun calculateDetailed(snapshot: EnergySnapshot): ScoreResult {
        val peak = snapshot.peakEnergy
        val pressurePoints = snapshot.sleepPressurePoints.coerceAtLeast(0)
        val safeSoftMax = snapshot.softMaxReference.coerceAtLeast(1)

        val reservoirFactor = if (peak != null) {
            (peak.currentValueAt(snapshot.nowMillis).toFloat() / peak.peakValue.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val circadianFactor = if (peak != null) {
            val minutesSincePeak = peak.minutesSincePeak(snapshot.nowMillis)
            circadianFactor(minutesSincePeak)
        } else {
            0f
        }

        val confidenceFactor = peak?.confidence?.coerceIn(0f, 1f) ?: 0f
        val sleepPressureRatio = (pressurePoints.toFloat() / safeSoftMax.toFloat()).coerceIn(0f, 1f)

        val peakScore = (PERCENT_SCALE * reservoirFactor * circadianFactor * confidenceFactor)
            .coerceIn(0f, PERCENT_SCALE)
        val fatiguePenalty = (PERCENT_SCALE * sleepPressureRatio).coerceIn(0f, PERCENT_SCALE)
        val rawEnergy = (peakScore - fatiguePenalty).coerceIn(-PERCENT_SCALE, PERCENT_SCALE)
        val usableEnergy = ((rawEnergy + PERCENT_SCALE) / 2f).coerceIn(0f, PERCENT_SCALE)

        return ScoreResult(
            rawEnergy = rawEnergy,
            usableEnergy = usableEnergy,
            peakScore = peakScore,
            fatiguePenalty = fatiguePenalty,
            reservoirFactor = reservoirFactor,
            circadianFactor = circadianFactor,
            confidenceFactor = confidenceFactor,
            sleepPressureRatio = sleepPressureRatio
        )
    }

    /**
     * Cosine-window circadian profile with three user-defined peaks.
     */
    fun circadianFactor(minutesSincePeak: Int): Float {
        val wrapped = wrapMinutes(minutesSincePeak)

        val firstPeak = peakWindow(
            minute = wrapped,
            startMinute = FIRST_PEAK_START_MINUTES,
            durationMinutes = FIRST_PEAK_DURATION_MINUTES,
            amplitude = FIRST_PEAK_AMPLITUDE
        )
        val secondPeak = peakWindow(
            minute = wrapped,
            startMinute = SECOND_PEAK_START_MINUTES,
            durationMinutes = SECOND_PEAK_DURATION_MINUTES,
            amplitude = SECOND_PEAK_AMPLITUDE
        )
        val thirdPeak = peakWindow(
            minute = wrapped,
            startMinute = THIRD_PEAK_START_MINUTES,
            durationMinutes = THIRD_PEAK_DURATION_MINUTES,
            amplitude = THIRD_PEAK_AMPLITUDE
        )

        val combined = firstPeak + secondPeak + thirdPeak
        return combined.coerceIn(0f, 1f)
    }

    private fun peakWindow(
        minute: Int,
        startMinute: Int,
        durationMinutes: Int,
        amplitude: Float
    ): Float {
        if (durationMinutes <= 0) return 0f
        val endMinute = startMinute + durationMinutes
        if (minute < startMinute || minute > endMinute) return 0f

        val phase = (minute - startMinute).toFloat() / durationMinutes.toFloat()
        val raw = 0.5f * (1f - cos((2.0 * PI * phase).toFloat()))
        return (amplitude * raw).coerceAtLeast(0f)
    }

    private fun wrapMinutes(minutes: Int): Int {
        val wrapped = minutes % MINUTES_PER_DAY
        return if (wrapped < 0) wrapped + MINUTES_PER_DAY else wrapped
    }
}
