package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CircadianEnergyServiceTest {

    private lateinit var service: CircadianEnergyService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        service = DefaultCircadianEnergyService()
    }

    @Test
    fun testContinuousSleepRecoveryNoCliffAt60Minutes() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val testTime = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // 59 minutes sleep vs 60 minutes sleep
        val snapshot59 = service.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 59,
            zoneId = zoneId
        )

        val snapshot60 = service.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 60,
            zoneId = zoneId
        )

        val deltaUsable = kotlin.math.abs(snapshot59.usableEnergy - snapshot60.usableEnergy)
        val deltaPressureS = kotlin.math.abs(snapshot59.homeostaticPressureS - snapshot60.homeostaticPressureS)

        // Continuous recovery guarantees Delta E <= 1% and Delta S < 0.05 (no artificial cliff)
        assertTrue("Usable energy delta should be <= 1, was $deltaUsable", deltaUsable <= 1)
        assertTrue("Pressure S delta should be < 0.05, was $deltaPressureS", deltaPressureS < 0.05)
    }

    @Test
    fun testEveningEnergyAt19HoursAfter8HoursSleep() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val eveningTime = today.atTime(LocalTime.of(19, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val snapshot = service.evaluateSnapshot(
            timestampMillis = eveningTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 8 * 60,
            zoneId = zoneId
        )

        // Healthy evening wind-down (40%..56%), preventing Flaw #8 premature crashes (<40%)
        assertTrue(
            "Usable energy at 19:00 after 8h sleep should be in [40..56], was ${snapshot.usableEnergy}",
            snapshot.usableEnergy in 40..56
        )
        assertEquals(EnergyPhase.WIND_DOWN, snapshot.phase)
    }

    @Test
    fun testContinuousFocusDepletionOnlyAfter45Minutes() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val noonTime = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // 30 min focus: 0 penalty
        val snap30 = service.evaluateSnapshot(noonTime, wakeTime, 480, continuousActiveFocusMinutes = 30, zoneId = zoneId)
        assertEquals(0.0, snap30.focusDepletionPenalty, 0.001)

        // 60 min focus: small synaptic depletion penalty > 0 and <= 0.15
        val snap60 = service.evaluateSnapshot(noonTime, wakeTime, 480, continuousActiveFocusMinutes = 60, zoneId = zoneId)
        assertTrue(snap60.focusDepletionPenalty > 0.0)
        assertTrue(snap60.focusDepletionPenalty <= 0.15)

        // 90 min focus: triggers RECOVERY_NEEDED
        val snap90 = service.evaluateSnapshot(noonTime, wakeTime, 480, continuousActiveFocusMinutes = 90, zoneId = zoneId)
        assertEquals(EnergyPhase.RECOVERY_NEEDED, snap90.phase)
    }

    @Test
    fun testCategoryAffinityMatchesCircadianPhases() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val peakMorning = today.atTime(LocalTime.of(10, 30)).atZone(zoneId).toInstant().toEpochMilli()

        val snapPeak = service.evaluateSnapshot(peakMorning, wakeTime, 480, zoneId = zoneId)
        val deepWorkAffinity = service.computeCategoryAffinity(TaskCategory.DEEP_WORK, snapPeak)
        val adminAffinity = service.computeCategoryAffinity(TaskCategory.ADMIN, snapPeak)

        assertTrue(
            "Deep work affinity ($deepWorkAffinity) should exceed admin affinity ($adminAffinity) at peak energy",
            deepWorkAffinity > adminAffinity
        )
    }

    @Test
    fun testProjectDayTimelineGenerates24HourSteps() {
        val today = LocalDate.of(2026, 10, 18)
        val dayStart = today.atTime(LocalTime.of(0, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val timeline = service.projectDayTimeline(dayStart, wakeTime, 480, stepMinutes = 30, zoneId = zoneId)
        assertEquals(48, timeline.size)
        timeline.forEach {
            assertTrue(it.usableEnergy in 0..100)
            assertTrue(it.circadianAlertnessC in 0.0..1.0)
            assertTrue(it.homeostaticPressureS in 0.0..1.0)
        }
    }

    @Test
    fun testComputeDailySummaryStandardWake() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val noonTime = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val summary = service.computeDailySummary(
            timestampMillis = noonTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        // Peak window should land in morning [7..12]
        assertTrue("Peak start hour should be in [7..12], was ${summary.peakWindowStartHour}", summary.peakWindowStartHour in 7..12)
        assertEquals((summary.peakWindowStartHour + 3) % 24, summary.peakWindowEndHour)
        assertTrue("Peak energy score should be >= 70, was ${summary.peakEnergyScore}", summary.peakEnergyScore >= 70)

        // Dip window should land in [wake + 4h .. wake + 11h] = [11..18]
        assertTrue("Dip start hour should be in [11..18], was ${summary.dipWindowStartHour}", summary.dipWindowStartHour in 11..18)
        assertEquals((summary.dipWindowStartHour + 2) % 24, summary.dipWindowEndHour)

        // Recommended deep work minutes bounded in [60, 240]
        assertTrue("Recommended deep work should be in [60, 240], was ${summary.recommendedDeepWorkMinutes}", summary.recommendedDeepWorkMinutes in 60..240)
    }

    @Test
    fun testComputeDailySummaryAcrossMidnight() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeYesterday = today.minusDays(1).atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val lateNightTime = today.atTime(LocalTime.of(2, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val summary = service.computeDailySummary(
            timestampMillis = lateNightTime,
            wakeTimeMillis = wakeYesterday,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        // Summary must derive aligned day timeline without crashing on empty dip candidates
        assertTrue("Peak start hour should be in [7..12], was ${summary.peakWindowStartHour}", summary.peakWindowStartHour in 7..12)
        assertTrue("Dip start hour should be in [11..18], was ${summary.dipWindowStartHour}", summary.dipWindowStartHour in 11..18)
    }

    @Test
    fun testComputeDailySummarySleepDeprivationReducesCapacity() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val noonTime = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val rested = service.computeDailySummary(
            timestampMillis = noonTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        val sleepDeprived = service.computeDailySummary(
            timestampMillis = noonTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 180, // 3 hours
            zoneId = zoneId
        )

        assertTrue(
            "Rested peak score (${rested.peakEnergyScore}) should exceed sleep-deprived score (${sleepDeprived.peakEnergyScore})",
            rested.peakEnergyScore > sleepDeprived.peakEnergyScore
        )
        assertTrue(
            "Rested deep work minutes (${rested.recommendedDeepWorkMinutes}) should >= sleep-deprived (${sleepDeprived.recommendedDeepWorkMinutes})",
            rested.recommendedDeepWorkMinutes >= sleepDeprived.recommendedDeepWorkMinutes
        )
    }
}
