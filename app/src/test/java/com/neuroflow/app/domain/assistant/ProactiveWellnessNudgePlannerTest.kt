package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProactiveWellnessNudgePlannerTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun epochAt(hour: Int, minute: Int = 0): Long {
        return LocalDate.of(2026, 6, 15)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    private fun snapshot(
        nowMillis: Long,
        energy: Int,
        phase: EnergyPhase,
        pressure: Double = 0.35
    ) = CircadianSnapshot(
        timestampMillis = nowMillis,
        usableEnergy = energy,
        homeostaticPressureS = pressure,
        circadianAlertnessC = 0.72,
        focusDepletionPenalty = 0.0,
        phase = phase
    )

    private fun calmBurnout(energy: Int = 72) = BurnoutRiskState(
        riskScore = 0.15f,
        riskLevel = BurnoutRiskLevel.CALM,
        unbrokenLoadMinutes = 20,
        consecutiveHeavyTasks = 0,
        minutesSinceLastRest = 30,
        lastRestAtMillis = 0L,
        lastRestDurationMinutes = 0,
        recommendedRestMinutes = 10,
        reasonSummary = "Energy balanced",
        usableEnergy = energy
    )

    @Test
    fun activeFocusSession_silencesAllBackgroundNudges() {
        val now = epochAt(14, 0)
        val prefs = UserPreferences(
            wellnessNudgesEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23,
            lastHydrationLoggedAtMillis = now - 150 * 60_000L
        )
        val decision = ProactiveWellnessNudgePlanner.evaluate(
            prefs = prefs,
            snapshot = snapshot(now, energy = 28, phase = EnergyPhase.CRASH),
            burnoutRisk = calmBurnout(28).copy(riskLevel = BurnoutRiskLevel.HIGH),
            recommendations = emptyList(),
            stimulants = emptyList(),
            naps = emptyList(),
            continuousFocusMinutes = 80,
            nowMillis = now,
            isFocusSessionActive = true,
            zoneId = zone
        )

        assertFalse(decision.shouldNotify)
        assertEquals("focus_session_active", decision.suppressReason)
        assertNull(decision.payload)
    }

    @Test
    fun quietSleepHours_silencesDaytimeNudges() {
        val now = epochAt(2, 30)
        val prefs = UserPreferences(
            wellnessNudgesEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23,
            lastHydrationLoggedAtMillis = now - 180 * 60_000L
        )
        val decision = ProactiveWellnessNudgePlanner.evaluate(
            prefs = prefs,
            snapshot = snapshot(now, energy = 25, phase = EnergyPhase.TROUGH),
            burnoutRisk = calmBurnout(25),
            recommendations = emptyList(),
            stimulants = emptyList(),
            naps = emptyList(),
            continuousFocusMinutes = 0,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zone
        )

        assertFalse(decision.shouldNotify)
        assertEquals("quiet_hours", decision.suppressReason)
    }

    @Test
    fun preBedtimeWindow_triggersWindDownRecoveryNudge() {
        val now = epochAt(22, 15)
        val prefs = UserPreferences(
            wellnessNudgesEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23
        )
        val decision = ProactiveWellnessNudgePlanner.evaluate(
            prefs = prefs,
            snapshot = snapshot(now, energy = 44, phase = EnergyPhase.WIND_DOWN, pressure = 0.78),
            burnoutRisk = calmBurnout(44),
            recommendations = emptyList(),
            stimulants = emptyList(),
            naps = emptyList(),
            continuousFocusMinutes = 10,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zone
        )

        assertTrue(decision.shouldNotify)
        assertEquals(WellnessNotificationCategory.WIND_DOWN, decision.payload?.category)
        assertEquals(WellnessNotificationActionType.LOG_REST_BREAK, decision.payload?.primaryActionType)
    }

    @Test
    fun lowEnergyCrash_prioritizesRecoveryOverBreakAndHydration() {
        val now = epochAt(14, 30)
        val prefs = UserPreferences(
            wellnessNudgesEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23,
            hydrationIntervalMinutes = 90,
            lastHydrationLoggedAtMillis = now - 140 * 60_000L
        )
        val decision = ProactiveWellnessNudgePlanner.evaluate(
            prefs = prefs,
            snapshot = snapshot(now, energy = 31, phase = EnergyPhase.CRASH, pressure = 0.74),
            burnoutRisk = calmBurnout(31).copy(
                riskLevel = BurnoutRiskLevel.HIGH,
                unbrokenLoadMinutes = 85,
                recommendedRestMinutes = 15
            ),
            recommendations = emptyList(),
            stimulants = emptyList(),
            naps = emptyList(),
            continuousFocusMinutes = 75,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zone
        )

        assertTrue(decision.shouldNotify)
        val payload = decision.payload
        assertNotNull(payload)
        assertEquals(WellnessNotificationCategory.RECOVERY, payload!!.category)
        assertEquals(WellnessNotificationActionType.LOG_REST_BREAK, payload.primaryActionType)
        assertEquals(15, payload.recommendedDurationMinutes)
        assertTrue(payload.title.contains("31%"))
    }

    @Test
    fun sustainedFocusStretch_triggersBreakNudge() {
        val now = epochAt(11, 20)
        val prefs = UserPreferences(
            wellnessNudgesEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23,
            focusBreakThresholdMinutes = 50,
            lastHydrationLoggedAtMillis = now - 120 * 60_000L
        )
        val decision = ProactiveWellnessNudgePlanner.evaluate(
            prefs = prefs,
            snapshot = snapshot(now, energy = 68, phase = EnergyPhase.PEAK),
            burnoutRisk = calmBurnout(68).copy(
                riskLevel = BurnoutRiskLevel.APPROACHING,
                unbrokenLoadMinutes = 60,
                recommendedRestMinutes = 10
            ),
            recommendations = emptyList(),
            stimulants = emptyList(),
            naps = emptyList(),
            continuousFocusMinutes = 58,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zone
        )

        assertTrue(decision.shouldNotify)
        assertEquals(WellnessNotificationCategory.BREAK, decision.payload?.category)
        assertEquals(WellnessNotificationActionType.LOG_REST_BREAK, decision.payload?.primaryActionType)
    }

    @Test
    fun overdueHydrationWithHealthyEnergy_triggersEnergyAwareHydrationNudge() {
        val now = epochAt(10, 30)
        val prefs = UserPreferences(
            wellnessNudgesEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23,
            hydrationIntervalMinutes = 90,
            lastHydrationLoggedAtMillis = now - 105 * 60_000L
        )
        val decision = ProactiveWellnessNudgePlanner.evaluate(
            prefs = prefs,
            snapshot = snapshot(now, energy = 82, phase = EnergyPhase.PEAK),
            burnoutRisk = calmBurnout(82),
            recommendations = emptyList(),
            stimulants = emptyList(),
            naps = emptyList(),
            continuousFocusMinutes = 20,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zone
        )

        assertTrue(decision.shouldNotify)
        val payload = decision.payload!!
        assertEquals(WellnessNotificationCategory.HYDRATION, payload.category)
        assertEquals(WellnessNotificationActionType.LOG_HYDRATION, payload.primaryActionType)
        assertTrue(payload.title.contains("82%"))
        assertEquals("Drank Water ✓", payload.primaryActionLabel)
    }

    @Test
    fun sharedCooldownAndSnooze_preventSpamNotifications() {
        val now = epochAt(15, 0)
        val prefsCooldown = UserPreferences(
            wellnessNudgesEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23,
            lastEnergyBoosterNudgeAtMillis = now - 20 * 60_000L,
            lastHydrationLoggedAtMillis = now - 140 * 60_000L
        )
        val cooledDecision = ProactiveWellnessNudgePlanner.evaluate(
            prefs = prefsCooldown,
            snapshot = snapshot(now, energy = 30, phase = EnergyPhase.CRASH),
            burnoutRisk = calmBurnout(30).copy(riskLevel = BurnoutRiskLevel.HIGH),
            recommendations = emptyList(),
            stimulants = emptyList(),
            naps = emptyList(),
            continuousFocusMinutes = 60,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zone
        )
        assertFalse(cooledDecision.shouldNotify)
        assertEquals("cooldown_active", cooledDecision.suppressReason)

        val prefsSnoozed = UserPreferences(
            wellnessNudgesEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23,
            snoozedEnergyBoosterUntilMillis = now + 30 * 60_000L
        )
        val snoozedDecision = ProactiveWellnessNudgePlanner.evaluate(
            prefs = prefsSnoozed,
            snapshot = snapshot(now, energy = 30, phase = EnergyPhase.CRASH),
            burnoutRisk = calmBurnout(30).copy(riskLevel = BurnoutRiskLevel.HIGH),
            recommendations = emptyList(),
            stimulants = emptyList(),
            naps = emptyList(),
            continuousFocusMinutes = 60,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zone
        )
        assertFalse(snoozedDecision.shouldNotify)
        assertEquals("snoozed", snoozedDecision.suppressReason)
    }

    @Test
    fun previewMode_bypassesCooldownAndReturnsLiveCheckInEvenWhenHealthy() {
        val now = epochAt(10, 0)
        val prefs = UserPreferences(
            wellnessNudgesEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23,
            lastHydrationLoggedAtMillis = now - 10 * 60_000L,
            lastEnergyBoosterNudgeAtMillis = now - 5 * 60_000L
        )
        val decision = ProactiveWellnessNudgePlanner.evaluate(
            prefs = prefs,
            snapshot = snapshot(now, energy = 86, phase = EnergyPhase.PEAK),
            burnoutRisk = calmBurnout(86),
            recommendations = emptyList(),
            stimulants = emptyList(),
            naps = emptyList(),
            continuousFocusMinutes = 5,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zone,
            ignoreCooldownForPreview = true
        )

        assertTrue(decision.shouldNotify)
        assertNotNull(decision.payload)
        assertEquals(86, decision.payload!!.usableEnergyPercent)
    }
}
