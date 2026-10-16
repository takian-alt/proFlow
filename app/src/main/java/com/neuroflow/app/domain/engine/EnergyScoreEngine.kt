package com.neuroflow.app.domain.engine

import kotlin.math.PI
import kotlin.math.cos

/**
 * Combines peak-energy and sleep-pressure signals into a single energy score.
 */
object EnergyScoreEngine {

    private const val MINUTES_PER_DAY = 24 * 60
    private const val PERCENT_SCALE = 100f

    private val DEFAULT_WINDOWS = listOf(
        PeakEnergyEngine.PeakWindow(startMinuteOffset = 0, durationMinutes = 210, amplitude = 1.0f),
        PeakEnergyEngine.PeakWindow(startMinuteOffset = 570, durationMinutes = 150, amplitude = 0.8f),
        PeakEnergyEngine.PeakWindow(startMinuteOffset = 810, durationMinutes = 60, amplitude = 0.6f)
    )

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
        val softMaxReference: Int = SleepPressureDetector.SOFT_MAX_REFERENCE,
        val minutesSinceWake: Int? = null,
        val rollingAverageSleepMinutes: Float? = null
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

        val minutesSincePeak = if (peak != null) {
            peak.minutesSincePeak(snapshot.nowMillis)
        } else {
            0
        }

        val rawCircadian = if (peak != null) {
            circadianFactor(minutesSincePeak, peak.circadianProfile)
        } else {
            0f
        }
        // Baseline floor: 0.35f baseline so usable energy doesn't collapse to <= 50% outside peak windows
        val circadianFactor = if (peak != null) {
            val wakeMinuteOfDay = ((peak.wakeUpHour * 60) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY
            val minutesWakeToPeak = (peak.peakMinuteOfDay - wakeMinuteOfDay + MINUTES_PER_DAY) % MINUTES_PER_DAY
            val nowMinute = minuteOfDay(snapshot.nowMillis)
            val minutesSinceWake = (nowMinute - wakeMinuteOfDay + MINUTES_PER_DAY) % MINUTES_PER_DAY

            val blendedCircadian = (0.35f + 0.65f * rawCircadian).coerceIn(0f, 1f)

            if (minutesWakeToPeak in 1..(12 * 60) && minutesSinceWake < minutesWakeToPeak) {
                // Pre-peak morning ramp: circadian rhythm rises continuously from morning wake baseline (0.75f) to 1.0f
                val ramp = minutesSinceWake.toFloat() / minutesWakeToPeak.toFloat()
                (0.75f + 0.25f * ramp).coerceIn(0f, 1f)
            } else {
                // Continuous transition from morning ramp into the circadian peak window:
                // Maintain continuous baseline support so alertness connects seamlessly from the morning
                // wake ramp and stays elevated (>= 0.85f) during the peak window hours before the cosine apex
                val postPeakBaseline = if (minutesSincePeak < 105) {
                    val progress = minutesSincePeak.toFloat() / 105f
                    maxOf(0.85f, 1.0f - 0.15f * progress)
                } else {
                    0.35f
                }
                maxOf(blendedCircadian, postPeakBaseline).coerceIn(0f, 1f)
            }
        } else {
            0f
        }

        val confidenceFactor = peak?.confidence?.coerceIn(0f, 1f) ?: 0f
        val sleepPressureRatio = SleepPressureDetector.adjustedFatigueRatio(
            pressurePoints = pressurePoints,
            minutesSinceWake = snapshot.minutesSinceWake,
            rollingAverageSleepMinutes = snapshot.rollingAverageSleepMinutes,
            softMaxReference = safeSoftMax
        )

        val peakScore = (PERCENT_SCALE * reservoirFactor * circadianFactor)
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
    fun circadianFactor(
        minutesSincePeak: Int,
        profile: PeakEnergyEngine.CircadianProfile? = null
    ): Float {
        val wrapped = wrapMinutes(minutesSincePeak)
        val windows = profile?.windows ?: DEFAULT_WINDOWS
        val maxPotentialAmplitude = windows.maxOfOrNull { it.amplitude }?.coerceAtLeast(1.0f) ?: 1.0f
        val combined = windows.sumOf { window ->
            peakWindow(
                minute = wrapped,
                startMinute = window.startMinuteOffset,
                durationMinutes = window.durationMinutes,
                amplitude = window.amplitude
            ).toDouble()
        }.toFloat()
        return (combined / maxPotentialAmplitude).coerceIn(0f, 1f)
    }

    private fun peakWindow(
        minute: Int,
        startMinute: Int,
        durationMinutes: Int,
        amplitude: Float
    ): Float {
        if (durationMinutes <= 0) return 0f
        val normalizedStart = wrapMinutes(startMinute)
        val elapsed = (minute - normalizedStart + MINUTES_PER_DAY) % MINUTES_PER_DAY
        if (elapsed >= durationMinutes) return 0f

        val phase = elapsed.toFloat() / durationMinutes.toFloat()
        val raw = 0.5f * (1f - cos((2.0 * PI * phase).toFloat()))
        return (amplitude * raw).coerceAtLeast(0f)
    }

    private fun wrapMinutes(minutes: Int): Int {
        val wrapped = minutes % MINUTES_PER_DAY
        return if (wrapped < 0) wrapped + MINUTES_PER_DAY else wrapped
    }

    private fun minuteOfDay(nowMillis: Long): Int {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        return calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
    }
}
