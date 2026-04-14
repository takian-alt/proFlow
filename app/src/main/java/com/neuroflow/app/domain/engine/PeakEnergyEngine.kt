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

    data class DetectionResult(
        val chronotype: MEQChronotypeDetector.Chronotype,
        val wakeUpHour: Int,
        val peakOffsetHours: Float,
        val peakHourOfDay: Int,
        val peakMinuteOfDay: Int,
        val peakValue: Int = PEAK_VALUE,
        val confidence: Float
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
        wakeUpHour: Int
    ): DetectionResult {
        val offsetHours = chronotypeOffset(meqResult.chronotype)
        val normalizedWakeHour = normalizeHour(wakeUpHour)
        val peakMinuteOfDay = ((normalizedWakeHour * 60) + (offsetHours * 60f).roundToInt()) % (24 * 60)

        return DetectionResult(
            chronotype = meqResult.chronotype,
            wakeUpHour = normalizedWakeHour,
            peakOffsetHours = offsetHours,
            peakHourOfDay = peakMinuteOfDay / 60,
            peakMinuteOfDay = peakMinuteOfDay,
            peakValue = PEAK_VALUE,
            confidence = meqResult.confidence
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

    private fun minuteOfDay(nowMillis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    private fun normalizeHour(hour: Int): Int {
        val normalized = hour % 24
        return if (normalized < 0) normalized + 24 else normalized
    }
}