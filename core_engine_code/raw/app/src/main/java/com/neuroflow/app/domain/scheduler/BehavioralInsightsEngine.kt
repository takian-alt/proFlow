package com.neuroflow.app.domain.scheduler

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

object BehavioralInsightsEngine {
    /**
     * Bayesian shrinkage formula for duration estimation calibration.
     * shrinkageFactor = (k0 * prior + n * observedGeometricMean) / (k0 + n)
     */
    fun calculateCalibrationFactor(
        actualMinutes: List<Int>,
        estimatedMinutes: List<Int>,
        k0: Float = 4.0f,
        prior: Float = 1.0f
    ): Float {
        if (actualMinutes.isEmpty() || estimatedMinutes.isEmpty()) return prior
        val n = min(actualMinutes.size, estimatedMinutes.size)
        if (n == 0) return prior

        val ratios = (0 until n).map { i ->
            val est = estimatedMinutes[i].coerceAtLeast(1).toFloat()
            val act = actualMinutes[i].coerceAtLeast(1).toFloat()
            (act / est).coerceIn(0.4f, 3.0f)
        }
        val geometricMean = exp(ratios.map { ln(it.toDouble()) }.average()).toFloat()
        val calibrated = (k0 * prior + n * geometricMean) / (k0 + n)
        return calibrated.coerceIn(0.8f, 1.6f)
    }

    /**
     * Calculates empirical circadian density distribution across 24 hourly bins.
     */
    fun calculateHourlyCircadianDensity(focusMinutesByHour: Map<Int, Int>): List<Float> {
        val totalMinutes = focusMinutesByHour.values.sum().coerceAtLeast(1).toFloat()
        return (0..23).map { hour ->
            (focusMinutesByHour[hour] ?: 0).toFloat() / totalMinutes
        }
    }
}
