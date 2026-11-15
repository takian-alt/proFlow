package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class Adr028TenPhaseCircadianTest {

    private val zoneId = ZoneId.of("UTC")
    private val service = DefaultCircadianEnergyService()

    @Test
    fun testPriority1_ActiveRecoveryOverridesAll() {
        val phase = resolveEnergyPhase(
            continuousActiveFocusMinutes = 120, // would otherwise be RECOVERY_NEEDED
            usableEnergy = 92,                  // would otherwise be SURGE
            hoursAwake = 8.0,
            processC = 0.8,
            processS = 0.3,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = 0.0,
            stimAlertnessBoost = 15.0,
            isRestSessionActive = true,         // Active session
            clockHour = 14.0,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.RECOVERY, phase)
    }

    @Test
    fun testPriority2_SleepInertiaInFirst30Minutes() {
        val phase = resolveEnergyPhase(
            continuousActiveFocusMinutes = 0,
            usableEnergy = 65,
            hoursAwake = 0.25, // 15 mins awake
            processC = 0.55,
            processS = 0.2,
            sleepInertiaPenalty = 0.07, // Active inertia penalty
            physiologicalImpulse = 0.0,
            stimAlertnessBoost = 0.0,
            isRestSessionActive = false,
            clockHour = 7.25,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.INERTIA, phase)
    }

    @Test
    fun testPriority3_CaffeineAndMetabolicCrash() {
        // Caffeine crash: high processS, stimulant cleared, energy dropped < 40
        val caffeineCrash = resolveEnergyPhase(
            continuousActiveFocusMinutes = 30,
            usableEnergy = 34,
            hoursAwake = 7.0,
            processC = 0.45,
            processS = 0.68,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = -2.0,
            stimAlertnessBoost = 0.5,
            isRestSessionActive = false,
            clockHour = 14.0,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.CRASH, caffeineCrash)

        // Postprandial food coma crash: severe negative impulse <= -6.0, energy < 42
        val foodCrash = resolveEnergyPhase(
            continuousActiveFocusMinutes = 30,
            usableEnergy = 38,
            hoursAwake = 6.5,
            processC = 0.55,
            processS = 0.45,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = -7.5,
            stimAlertnessBoost = 0.0,
            isRestSessionActive = false,
            clockHour = 13.5,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.CRASH, foodCrash)
    }

    @Test
    fun testPriority4_RecoveryNeededAfter90MinutesFocus() {
        val phase = resolveEnergyPhase(
            continuousActiveFocusMinutes = 90,
            usableEnergy = 75,
            hoursAwake = 4.0,
            processC = 0.75,
            processS = 0.35,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = 0.0,
            stimAlertnessBoost = 0.0,
            isRestSessionActive = false,
            clockHour = 11.0,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.RECOVERY_NEEDED, phase)
    }

    @Test
    fun testPriority5_HyperfocusSurgeAtHighAlertnessAndStimulantPeak() {
        val surgePhase = resolveEnergyPhase(
            continuousActiveFocusMinutes = 30,
            usableEnergy = 88,
            hoursAwake = 3.0,
            processC = 0.82,
            processS = 0.25,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = 10.0,
            stimAlertnessBoost = 12.0, // High acute stimulant effect
            isRestSessionActive = false,
            clockHour = 10.0,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.SURGE, surgePhase)
    }

    @Test
    fun testPriority6_PeakCognitiveBaseline() {
        val peakPhase = resolveEnergyPhase(
            continuousActiveFocusMinutes = 20,
            usableEnergy = 78,
            hoursAwake = 3.5,
            processC = 0.80,
            processS = 0.28,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = 0.0,
            stimAlertnessBoost = 0.0,
            isRestSessionActive = false,
            clockHour = 10.5,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.PEAK, peakPhase)
    }

    @Test
    fun testPriority7_MiddayDip() {
        val dipPhase = resolveEnergyPhase(
            continuousActiveFocusMinutes = 20,
            usableEnergy = 52,
            hoursAwake = 7.5,
            processC = 0.42,
            processS = 0.48,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = -3.5,
            stimAlertnessBoost = 0.0,
            isRestSessionActive = false,
            clockHour = 14.5,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.DIP, dipPhase)
    }

    @Test
    fun testPriority8_AfternoonRebound() {
        val reboundPhase = resolveEnergyPhase(
            continuousActiveFocusMinutes = 20,
            usableEnergy = 62,
            hoursAwake = 10.5,
            processC = 0.65,
            processS = 0.52,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = 0.0,
            stimAlertnessBoost = 0.0,
            isRestSessionActive = false,
            clockHour = 17.5,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.REBOUND, reboundPhase)
    }

    @Test
    fun testPriority9_WindDownInEvening() {
        val windDownPhase = resolveEnergyPhase(
            continuousActiveFocusMinutes = 0,
            usableEnergy = 45,
            hoursAwake = 14.5,
            processC = 0.38,
            processS = 0.75,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = 0.0,
            stimAlertnessBoost = 0.0,
            isRestSessionActive = false,
            clockHour = 21.5,
            sleepHour = 23
        )
        assertEquals(EnergyPhase.WIND_DOWN, windDownPhase)
    }

    @Test
    fun testPriority10_CircadianTroughInDeepSleepNadir() {
        val troughPhase = resolveEnergyPhase(
            continuousActiveFocusMinutes = 0,
            usableEnergy = 18,
            hoursAwake = 19.0,
            processC = 0.18, // Core nadir
            processS = 0.90,
            sleepInertiaPenalty = 0.0,
            physiologicalImpulse = 0.0,
            stimAlertnessBoost = 0.0,
            isRestSessionActive = false,
            clockHour = 3.0, // 3:00 AM past bedtime
            sleepHour = 23
        )
        assertEquals(EnergyPhase.TROUGH, troughPhase)
    }

    @Test
    fun testCategoryAffinityAcrossAllTenPhases() {
        fun makeSnapshot(phase: EnergyPhase, energy: Int) = CircadianSnapshot(
            timestampMillis = 1000L,
            usableEnergy = energy,
            homeostaticPressureS = 0.3,
            circadianAlertnessC = 0.7,
            focusDepletionPenalty = 0.0,
            phase = phase
        )

        val surgeSnap = makeSnapshot(EnergyPhase.SURGE, 88)
        val deepWorkSurge = service.computeCategoryAffinity(TaskCategory.DEEP_WORK, surgeSnap)
        assertTrue("Deep work affinity during SURGE should be >= 100.0, was $deepWorkSurge", deepWorkSurge >= 100.0)

        val reboundSnap = makeSnapshot(EnergyPhase.REBOUND, 60)
        val creativeRebound = service.computeCategoryAffinity(TaskCategory.CREATIVE, reboundSnap)
        assertEquals(75.0, creativeRebound, 0.001)

        val dipSnap = makeSnapshot(EnergyPhase.DIP, 48)
        val adminDip = service.computeCategoryAffinity(TaskCategory.ADMIN, dipSnap)
        assertTrue("Admin affinity during DIP should be elevated (>95), was $adminDip", adminDip > 95.0)

        val recoverySnap = makeSnapshot(EnergyPhase.RECOVERY, 40)
        val mindRecovery = service.computeCategoryAffinity(TaskCategory.MINDFULNESS, recoverySnap)
        assertEquals(100.0, mindRecovery, 0.001)

        val troughSnap = makeSnapshot(EnergyPhase.TROUGH, 15)
        val deepWorkTrough = service.computeCategoryAffinity(TaskCategory.DEEP_WORK, troughSnap)
        assertEquals(10.0, deepWorkTrough, 0.001)
    }
}
