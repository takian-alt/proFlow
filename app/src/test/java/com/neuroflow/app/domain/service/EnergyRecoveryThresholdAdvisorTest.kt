package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.model.HourlyEnergyPrediction
import com.neuroflow.app.domain.model.PredictedEnergyCurveUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EnergyRecoveryThresholdAdvisorTest {

    private val zoneId = ZoneId.of("UTC")

    private fun buildCurve(
        hourlyEnergies: Map<Int, Int>,
        dipStart: Int = 13,
        dipEnd: Int = 15,
        peakEnergy: Int = 88,
        confidence: Int = 70,
        hasHistory: Boolean = true,
        completionHour: Int? = 14
    ): PredictedEnergyCurveUiModel {
        val points = (0 until 24).map { h ->
            val energy = hourlyEnergies[h] ?: 65
            HourlyEnergyPrediction(
                hour = h,
                predictedEnergy = energy,
                historicalCompletionCount = if (completionHour == h) 4 else 0,
                historicalActivityScore = if (completionHour == h) 80 else 0,
                phase = when {
                    energy >= 75 -> EnergyPhase.PEAK
                    energy >= 55 -> EnergyPhase.REBOUND
                    else -> EnergyPhase.DIP
                }
            )
        }
        return PredictedEnergyCurveUiModel(
            hourlyPoints = points,
            peakStartHour = 9,
            peakEndHour = 12,
            peakEnergyValue = peakEnergy,
            dipStartHour = dipStart,
            dipEndHour = dipEnd,
            bestAutoScheduleHour = 10,
            historicalCompletionCount = if (hasHistory) 4 else 0,
            modelConfidencePercent = confidence,
            hasHistoricalData = hasHistory
        )
    }

    @Test
    fun `manual threshold mode uses clamped base threshold without personalization`() {
        val curve = buildCurve(hourlyEnergies = mapOf(14 to 38, 15 to 40))
        val profile = EnergyRecoveryThresholdAdvisor.computePersonalizedThreshold(
            hourlyPoints = curve.hourlyPoints,
            dipStartHour = 13,
            dipEndHour = 15,
            peakEnergyValue = 85,
            modelConfidencePercent = 80,
            hasHistoricalData = true,
            baseThresholdPercent = 50,
            autoPersonalized = false
        )

        assertFalse(profile.isPersonalized)
        assertEquals(50, profile.effectiveThresholdPercent)
        assertTrue(profile.summaryReason.contains("Manual threshold"))
    }

    @Test
    fun `high sleep pressure raises personalized recovery threshold for earlier break warning`() {
        val curve = buildCurve(hourlyEnergies = mapOf(13 to 52, 14 to 48, 15 to 54))
        val restedProfile = EnergyRecoveryThresholdAdvisor.computePersonalizedThreshold(
            hourlyPoints = curve.hourlyPoints,
            dipStartHour = 13,
            dipEndHour = 15,
            peakEnergyValue = 86,
            modelConfidencePercent = 60,
            hasHistoricalData = true,
            baseThresholdPercent = 45,
            autoPersonalized = true,
            sleepPressurePoints = 200
        )
        val fatiguedProfile = EnergyRecoveryThresholdAdvisor.computePersonalizedThreshold(
            hourlyPoints = curve.hourlyPoints,
            dipStartHour = 13,
            dipEndHour = 15,
            peakEnergyValue = 86,
            modelConfidencePercent = 60,
            hasHistoricalData = true,
            baseThresholdPercent = 45,
            autoPersonalized = true,
            sleepPressurePoints = 800
        )

        assertTrue(restedProfile.isPersonalized)
        assertTrue(
            "Expected fatigued threshold (${fatiguedProfile.effectiveThresholdPercent}) > rested (${restedProfile.effectiveThresholdPercent})",
            fatiguedProfile.effectiveThresholdPercent > restedProfile.effectiveThresholdPercent
        )
        assertEquals(5, fatiguedProfile.sleepPressureAdjustmentPercent)
    }

    @Test
    fun `triggers actionable short break notification when waking energy drops below threshold`() {
        val curve = buildCurve(
            hourlyEnergies = mapOf(
                10 to 86,
                13 to 44,
                14 to 36,
                15 to 48,
                16 to 68
            )
        )
        val prefs = UserPreferences(
            energyRecoveryAlertsEnabled = true,
            recoveryThresholdAutoPersonalized = false,
            recoveryThresholdBasePercent = 45,
            wakeUpHour = 7,
            sleepHour = 23,
            autoSchedulingBreakDurationMinutes = 10,
            lastRecoveryAlertSentAtMillis = 0L
        )
        val nowMillis = LocalDate.of(2026, 6, 1)
            .atTime(14, 15)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val suggestion = EnergyRecoveryThresholdAdvisor.evaluateBreakTrigger(
            curve = curve,
            prefs = prefs,
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertTrue(suggestion.shouldNotify)
        assertEquals(14, suggestion.currentHour)
        assertEquals(36, suggestion.currentEnergyPercent)
        assertEquals(45, suggestion.recoveryThresholdPercent)
        assertEquals(9, suggestion.deficitPercent)
        assertEquals(16, suggestion.nextReboundHour)
        assertEquals(68, suggestion.nextReboundEnergyPercent)
        assertTrue(suggestion.notificationTitle.contains("10-min"))
        assertTrue(suggestion.notificationMessage.contains("36%"))
        assertTrue(suggestion.notificationMessage.contains("45%"))
        assertTrue(suggestion.notificationMessage.contains("16:00"))
    }

    @Test
    fun `suppresses notification when disabled, above threshold, in cooldown, or sleeping`() {
        val curve = buildCurve(hourlyEnergies = mapOf(14 to 34, 2 to 22))
        val dayAt14 = LocalDate.of(2026, 6, 1)
            .atTime(14, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val nightAt02 = LocalDate.of(2026, 6, 1)
            .atTime(2, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val basePrefs = UserPreferences(
            energyRecoveryAlertsEnabled = true,
            recoveryThresholdAutoPersonalized = false,
            recoveryThresholdBasePercent = 45,
            wakeUpHour = 7,
            sleepHour = 23,
            lastRecoveryAlertSentAtMillis = 0L
        )

        assertEquals(
            "alerts_disabled",
            EnergyRecoveryThresholdAdvisor.evaluateBreakTrigger(
                curve, basePrefs.copy(energyRecoveryAlertsEnabled = false), dayAt14, zoneId
            ).suppressionReason
        )
        assertEquals(
            "outside_waking_hours",
            EnergyRecoveryThresholdAdvisor.evaluateBreakTrigger(
                curve, basePrefs, nightAt02, zoneId
            ).suppressionReason
        )
        assertEquals(
            "cooldown_active",
            EnergyRecoveryThresholdAdvisor.evaluateBreakTrigger(
                curve,
                basePrefs.copy(lastRecoveryAlertSentAtMillis = dayAt14 - 30 * 60_000L),
                dayAt14,
                zoneId
            ).suppressionReason
        )
        val highCurve = buildCurve(hourlyEnergies = mapOf(14 to 62))
        assertEquals(
            "above_threshold",
            EnergyRecoveryThresholdAdvisor.evaluateBreakTrigger(
                highCurve, basePrefs, dayAt14, zoneId
            ).suppressionReason
        )
    }
}
