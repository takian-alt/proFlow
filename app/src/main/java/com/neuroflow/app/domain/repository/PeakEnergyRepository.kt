package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.domain.engine.MEQChronotypeDetector
import com.neuroflow.app.domain.engine.PeakEnergyEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that wires PeakEnergyEngine with user preferences.
 * Provides the current peak energy value based on user's chronotype and wake time.
 */
@Singleton
class PeakEnergyRepository @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore
) {
    /**
     * Provides a flow of peak energy detection results based on current preferences.
     * Updates whenever user's chronotype or wake time changes.
     */
    val peakEnergyFlow: Flow<PeakEnergyEngine.DetectionResult> = preferencesDataStore.preferencesFlow.map { prefs ->
        buildDetectionFromPreferences(
            quizPeakEnabled = prefs.quizPeakEnabled,
            quizChronotype = prefs.quizChronotype,
            manualChronotype = prefs.manualChronotype,
            wakeUpHour = prefs.wakeUpHour,
            manualPeakStart = prefs.peakEnergyStart,
            manualPeakEnd = prefs.peakEnergyEnd
        )
    }
    
    /**
        * Get the current peak energy value based on daily minute decay from peak.
     * Returns the peak value at the current moment
     */
    suspend fun getCurrentPeakEnergyValue(): Int {
        val result = getPeakEnergyDetection()
        return result.currentValueAt()
    }
    
    /**
     * Get the current effective chronotype (resolves based on quiz-peak toggle and available values)
     */
    suspend fun getCurrentChronotype(): MEQChronotypeDetector.Chronotype? {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return resolveChronotype(
            manualChronotype = prefs.manualChronotype,
            quizChronotype = prefs.quizChronotype,
            quizPeakEnabled = prefs.quizPeakEnabled
        )
    }
    
    /**
     * Get the peak energy result with all metadata (peak time, decay value, etc.)
     */
    suspend fun getPeakEnergyDetection(): PeakEnergyEngine.DetectionResult {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return buildDetectionFromPreferences(
            quizPeakEnabled = prefs.quizPeakEnabled,
            quizChronotype = prefs.quizChronotype,
            manualChronotype = prefs.manualChronotype,
            wakeUpHour = prefs.wakeUpHour,
            manualPeakStart = prefs.peakEnergyStart,
            manualPeakEnd = prefs.peakEnergyEnd
        )
    }

    private fun buildDetectionFromPreferences(
        quizPeakEnabled: Boolean,
        quizChronotype: String?,
        manualChronotype: String?,
        wakeUpHour: Int,
        manualPeakStart: Int,
        manualPeakEnd: Int
    ): PeakEnergyEngine.DetectionResult {
        val parsedQuizChronotype = parseChronotype(quizChronotype)
        if (quizPeakEnabled && parsedQuizChronotype != null) {
            val meqResult = toMeqResult(parsedQuizChronotype)
            return PeakEnergyEngine.detect(meqResult, wakeUpHour)
        }

        val fallbackChronotype = parseChronotype(manualChronotype)
            ?: parsedQuizChronotype
            ?: MEQChronotypeDetector.Chronotype.INTERMEDIATE

        val peakMinuteOfDay = midpointMinuteOfDay(manualPeakStart, manualPeakEnd)
        val safeWakeHour = normalizeHour(wakeUpHour)
        val offsetMinutes = minutesDifferenceWrapped(startMinute = safeWakeHour * 60, endMinute = peakMinuteOfDay)

        return PeakEnergyEngine.DetectionResult(
            chronotype = fallbackChronotype,
            wakeUpHour = safeWakeHour,
            peakOffsetHours = offsetMinutes / 60f,
            peakHourOfDay = peakMinuteOfDay / 60,
            peakMinuteOfDay = peakMinuteOfDay,
            peakValue = PeakEnergyEngine.PEAK_VALUE,
            confidence = 1f
        )
    }

    private fun resolveChronotype(
        manualChronotype: String?,
        quizChronotype: String?,
        quizPeakEnabled: Boolean
    ): MEQChronotypeDetector.Chronotype {
        return if (quizPeakEnabled) {
            parseChronotype(quizChronotype)
                ?: parseChronotype(manualChronotype)
                ?: MEQChronotypeDetector.Chronotype.INTERMEDIATE
        } else {
            parseChronotype(manualChronotype)
                ?: MEQChronotypeDetector.Chronotype.INTERMEDIATE
        }
    }

    private fun parseChronotype(raw: String?): MEQChronotypeDetector.Chronotype? {
        if (raw.isNullOrBlank()) return null
        return try {
            MEQChronotypeDetector.Chronotype.valueOf(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun toMeqResult(chronotype: MEQChronotypeDetector.Chronotype): MEQChronotypeDetector.Result {
        val totalScore = when (chronotype) {
            MEQChronotypeDetector.Chronotype.DEFINITE_MORNING -> 75
            MEQChronotypeDetector.Chronotype.MODERATE_MORNING -> 64
            MEQChronotypeDetector.Chronotype.INTERMEDIATE -> 50
            MEQChronotypeDetector.Chronotype.MODERATE_EVENING -> 36
            MEQChronotypeDetector.Chronotype.DEFINITE_EVENING -> 24
        }
        val (peakStart, peakEnd) = MEQChronotypeDetector.baselinePeakWindow(chronotype)
        return MEQChronotypeDetector.Result(
            totalScore = totalScore,
            chronotype = chronotype,
            baselinePeakStartHour = peakStart,
            baselinePeakEndHour = peakEnd,
            answeredQuestions = MEQChronotypeDetector.QUESTION_COUNT,
            confidence = 1f
        )
    }

    private fun midpointMinuteOfDay(startHour: Int, endHour: Int): Int {
        val startMinute = normalizeHour(startHour) * 60
        var endMinute = normalizeHour(endHour) * 60
        if (endMinute < startMinute) {
            endMinute += 24 * 60
        }
        val midpoint = (startMinute + endMinute) / 2
        return midpoint % (24 * 60)
    }

    private fun normalizeHour(hour: Int): Int {
        val normalized = hour % 24
        return if (normalized < 0) normalized + 24 else normalized
    }

    private fun minutesDifferenceWrapped(startMinute: Int, endMinute: Int): Int {
        val dayMinutes = 24 * 60
        val start = ((startMinute % dayMinutes) + dayMinutes) % dayMinutes
        val end = ((endMinute % dayMinutes) + dayMinutes) % dayMinutes
        return if (end >= start) end - start else (dayMinutes - start) + end
    }
}
