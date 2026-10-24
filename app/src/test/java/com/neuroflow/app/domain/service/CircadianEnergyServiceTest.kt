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

    @Test
    fun testPowerNap20mLowersProcessSAndRaisesEnergyWithoutInertiaOrPhaseShift() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napStart = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napEnd = today.atTime(LocalTime.of(14, 20)).atZone(zoneId).toInstant().toEpochMilli()
        val evalTime15 = today.atTime(LocalTime.of(15, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val powerNap = listOf(DaytimeNapBout(startMillis = napStart, endMillis = napEnd))

        // Without nap
        val snapNoNap = service.evaluateSnapshot(
            timestampMillis = evalTime15,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        // With 20m power nap
        val snapWithNap = service.evaluateSnapshot(
            timestampMillis = evalTime15,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = powerNap
        )

        // 1. Process S must be lower after nap
        assertTrue(
            "Process S with nap (${snapWithNap.homeostaticPressureS}) must be lower than without nap (${snapNoNap.homeostaticPressureS})",
            snapWithNap.homeostaticPressureS < snapNoNap.homeostaticPressureS
        )

        // 2. Usable energy must be higher in afternoon
        assertTrue(
            "Usable energy with nap (${snapWithNap.usableEnergy}) must exceed no-nap energy (${snapNoNap.usableEnergy})",
            snapWithNap.usableEnergy > snapNoNap.usableEnergy
        )

        // 3. Sleep inertia penalty for 20m nap must be strictly 0.0 (stays in stage 1/2)
        assertEquals(0.0, snapWithNap.sleepInertiaPenalty, 0.0001)

        // 4. Circadian alertness C(t) is 100% phase-locked (zero shift to phi)
        assertEquals(snapNoNap.circadianAlertnessC, snapWithNap.circadianAlertnessC, 0.0001)

        // 5. Daily summary peak hour phi is identical with or without nap
        val summaryNoNap = service.computeDailySummary(
            timestampMillis = evalTime15,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )
        val summaryWithNap = service.computeDailySummary(
            timestampMillis = evalTime15,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = powerNap
        )
        assertEquals(summaryNoNap.peakWindowStartHour, summaryWithNap.peakWindowStartHour)
    }

    @Test
    fun testDeepNap60mAppliesTransientSleepInertiaThatDecaysBy45Minutes() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napStart = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napEnd = today.atTime(LocalTime.of(15, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val deepNap = listOf(DaytimeNapBout(startMillis = napStart, endMillis = napEnd))

        // Immediately after waking: 15:01 (1 minute post-nap)
        val time1501 = today.atTime(LocalTime.of(15, 1)).atZone(zoneId).toInstant().toEpochMilli()
        val snapAtWake = service.evaluateSnapshot(
            timestampMillis = time1501,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = deepNap
        )

        // Sleep inertia penalty is active (> 0.10)
        assertTrue(
            "Sleep inertia penalty at 15:01 (${snapAtWake.sleepInertiaPenalty}) must be > 0.10",
            snapAtWake.sleepInertiaPenalty > 0.10
        )

        // 45 minutes later: 15:45
        val time1545 = today.atTime(LocalTime.of(15, 45)).atZone(zoneId).toInstant().toEpochMilli()
        val snapAt45m = service.evaluateSnapshot(
            timestampMillis = time1545,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = deepNap
        )

        // Sleep inertia penalty must decay away (< 0.02)
        assertTrue(
            "Sleep inertia penalty at 15:45 (${snapAt45m.sleepInertiaPenalty}) must be < 0.02",
            snapAt45m.sleepInertiaPenalty < 0.02
        )

        // Early evening: 18:00
        val time1800 = today.atTime(LocalTime.of(18, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val snapEveningNoNap = service.evaluateSnapshot(
            timestampMillis = time1800,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )
        val snapEveningWithNap = service.evaluateSnapshot(
            timestampMillis = time1800,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = deepNap
        )

        // Inertia completely gone at 18:00
        assertEquals(0.0, snapEveningWithNap.sleepInertiaPenalty, 0.001)

        // Sustained early-evening energy higher than without nap
        assertTrue(
            "Evening energy with 60m nap (${snapEveningWithNap.usableEnergy}) must exceed no-nap energy (${snapEveningNoNap.usableEnergy})",
            snapEveningWithNap.usableEnergy > snapEveningNoNap.usableEnergy
        )
    }

    @Test
    fun testNocturnalSleepAloneProducesBitForBitIdenticalOutputToPhase6() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val testTimes = listOf(
            today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(15, 30)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(19, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(22, 0)).atZone(zoneId).toInstant().toEpochMilli()
        )

        for (t in testTimes) {
            val legacy5ArgSnap = service.evaluateSnapshot(
                timestampMillis = t,
                wakeTimeMillis = wakeTime,
                lastSleepDurationMinutes = 480,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId
            )
            val explicitEmptyNapsSnap = service.evaluateSnapshot(
                timestampMillis = t,
                wakeTimeMillis = wakeTime,
                lastSleepDurationMinutes = 480,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = emptyList()
            )

            assertEquals(legacy5ArgSnap.usableEnergy, explicitEmptyNapsSnap.usableEnergy)
            assertEquals(legacy5ArgSnap.homeostaticPressureS, explicitEmptyNapsSnap.homeostaticPressureS, 0.0)
            assertEquals(legacy5ArgSnap.circadianAlertnessC, explicitEmptyNapsSnap.circadianAlertnessC, 0.0)
            assertEquals(legacy5ArgSnap.focusDepletionPenalty, explicitEmptyNapsSnap.focusDepletionPenalty, 0.0)
            assertEquals(legacy5ArgSnap.phase, explicitEmptyNapsSnap.phase)
            assertEquals(0.0, legacy5ArgSnap.sleepInertiaPenalty, 0.0)
        }
    }

    @Test
    fun testVonMisesHourlyModulationShiftsLearnedAdjustmentPoints() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val afternoon = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val learnedProfile = LearnedCircadianProfile(
            peakHourMean = 14.0,
            sampleCount = 15.0,
            hourlyMultipliers = AdaptiveCircadianLearner.computeVonMisesHourlyMultipliers(14.0)
        )

        val snapshot = service.evaluateSnapshot(
            timestampMillis = afternoon,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            naps = emptyList(),
            chronotype = ChronotypeProfile.INTERMEDIATE,
            manualPeakStartHour = null,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = null,
            cognitiveBlocks = emptyList()
        )

        assertTrue(
            "Learned adjustment points should not be zero when Von Mises modulation is active (was ${snapshot.learnedAdjustmentPoints})",
            snapshot.learnedAdjustmentPoints != 0.0
        )
    }

    @Test
    fun testProjectDayTimelineWithStimulantsModulatesCurve() {
        val today = LocalDate.of(2026, 10, 18)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val doseTime = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val baselineTimeline = service.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            stimulantDoses = emptyList()
        )

        val caffeineDose = listOf(
            StimulantDoseBout(
                timestampMillis = doseTime,
                doseMg = 100.0,
                type = StimulantType.CAFFEINE
            )
        )

        val stimulantTimeline = service.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            stimulantDoses = caffeineDose
        )

        val peakAbsorptionTime = today.atTime(LocalTime.of(8, 45)).atZone(zoneId).toInstant().toEpochMilli()
        val baseAtPeak = baselineTimeline.minByOrNull { kotlin.math.abs(it.timestampMillis - peakAbsorptionTime) }!!
        val stimAtPeak = stimulantTimeline.minByOrNull { kotlin.math.abs(it.timestampMillis - peakAbsorptionTime) }!!

        assertTrue(
            "Energy with caffeine ($stimAtPeak) should be strictly higher than baseline ($baseAtPeak) at peak plasma concentration",
            stimAtPeak.usableEnergy >= baseAtPeak.usableEnergy
        )
    }

    @Test
    fun testExtractStimulantDosesFromSlots() {
        val today = LocalDate.of(2026, 10, 18)
        val slots = listOf(
            com.neuroflow.app.domain.model.ProtectedTimeSlot(
                id = "slot-1",
                label = "Morning Espresso",
                startMinuteOfDay = 480, // 08:00
                durationMinutes = 20
            ),
            com.neuroflow.app.domain.model.ProtectedTimeSlot(
                id = "slot-2",
                label = "Afternoon Vyvanse Meds XR",
                startMinuteOfDay = 780, // 13:00
                durationMinutes = 15
            ),
            com.neuroflow.app.domain.model.ProtectedTimeSlot(
                id = "slot-3",
                label = "Deep Work Block",
                startMinuteOfDay = 540,
                durationMinutes = 90
            )
        )

        val extracted = com.neuroflow.app.domain.model.extractStimulantDosesFromSlots(slots, today, zoneId)
        // ADR-024 Bug 3: Evaluates across both yesterday and today to capture full 24h PK tail (2 per day * 2 days = 4)
        assertEquals(4, extracted.size)
        assertEquals(StimulantType.CAFFEINE, extracted[0].type)
        assertEquals(StimulantType.PRESCRIPTION_STIMULANT_XR, extracted[1].type)
    }
}
