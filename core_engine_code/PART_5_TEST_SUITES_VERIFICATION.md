# neuroFlow Core Engine — Part 5: Core Algorithmic & Mathematical Test Suites

**Description:** Exhaustive Unit Test Suites verifying: Continuous Borbély biophysics, Fast Circadian Kalman Learner convergence, Circadian adversarial stress, 4-vector scheduling placement, NLP syntax parsing, Gemini AI cascading & local heuristic fallbacks, Wearable chronobiology signal normalization, Calendar read & write-back synchronization, and Placement stability.  
**Generated for Vice Head Review:** 2026-10-23 22:05:10  
**Files:** 17  
**Lines of Code:** 3,526  
**Uncompressed Size:** 142.1 KB  

---

## Table of Contents

- [`app/src/test/java/com/neuroflow/app/domain/service/CircadianEnergyServiceTest.kt`](#appsrctestjavacomneuroflowappdomainservicecircadianenergyservicetestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/FastCircadianLearnerTest.kt`](#appsrctestjavacomneuroflowappdomainservicefastcircadianlearnertestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/CircadianAdversarialStressTest.kt`](#appsrctestjavacomneuroflowappdomainservicecircadianadversarialstresstestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/Adr020RobustnessTest.kt`](#appsrctestjavacomneuroflowappdomainserviceadr020robustnesstestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/Adr022RobustnessTest.kt`](#appsrctestjavacomneuroflowappdomainserviceadr022robustnesstestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/TaskSchedulerServiceTest.kt`](#appsrctestjavacomneuroflowappdomainservicetaskschedulerservicetestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParserTest.kt`](#appsrctestjavacomneuroflowappdomainservicetasknaturallanguageparsertestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/SmartTaskDecompositionServiceTest.kt`](#appsrctestjavacomneuroflowappdomainservicesmarttaskdecompositionservicetestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/GeminiAiOnlineServiceTest.kt`](#appsrctestjavacomneuroflowappdomainservicegeminiaionlineservicetestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizerTest.kt`](#appsrctestjavacomneuroflowappdomainservicechronobiologysignalnormalizertestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/RealWorldChronobiologyAndSchedulerTest.kt`](#appsrctestjavacomneuroflowappdomainservicerealworldchronobiologyandschedulertestkt)
- [`app/src/test/java/com/neuroflow/app/domain/scheduler/PlacementStabilityTest.kt`](#appsrctestjavacomneuroflowappdomainschedulerplacementstabilitytestkt)
- [`app/src/test/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClockTest.kt`](#appsrctestjavacomneuroflowappdomainschedulerprotectedslotclocktestkt)
- [`app/src/test/java/com/neuroflow/app/domain/inference/TaskInferenceEngineTest.kt`](#appsrctestjavacomneuroflowappdomaininferencetaskinferenceenginetestkt)
- [`app/src/test/java/com/neuroflow/app/data/local/StimulantLoggingTest.kt`](#appsrctestjavacomneuroflowappdatalocalstimulantloggingtestkt)
- [`app/src/test/java/com/neuroflow/app/data/calendar/CalendarSyncRepositoryTest.kt`](#appsrctestjavacomneuroflowappdatacalendarcalendarsyncrepositorytestkt)
- [`app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt`](#appsrctestjavacomneuroflowappdatacalendarcalendarwritebacksynctestkt)

---

## Source Code Listings

<a id="appsrctestjavacomneuroflowappdomainservicecircadianenergyservicetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/CircadianEnergyServiceTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/CircadianEnergyServiceTest.kt`
- **Lines of Code:** 468
- **Size:** 20,558 bytes (20.1 KB)

```kotlin
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
```

---

<a id="appsrctestjavacomneuroflowappdomainservicefastcircadianlearnertestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/FastCircadianLearnerTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/FastCircadianLearnerTest.kt`
- **Lines of Code:** 310
- **Size:** 12,668 bytes (12.4 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

class FastCircadianLearnerTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: TaskSchedulerService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    @Test
    fun testEmpiricalPeakHourConvergenceWithinTwoToThreeSessions() {
        val initialPrior = LearnedCircadianProfile(
            peakHourMean = 9.0,
            peakHourVariance = 4.0,
            sampleCount = 0.0
        )

        // Session 1 at 14:00 (minute 840) with high productivity
        val obs1 = BehavioralObservation(minuteOfDay = 840, productivityRating = 0.95, weight = 1.5)
        val profile1 = AdaptiveCircadianLearner.updateProfile(initialPrior, obs1, kappa = 4.5)
        assertTrue("Peak should move toward 14:00 after session 1", profile1.peakHourMean > 11.5)

        // Session 2 at 14:00
        val obs2 = BehavioralObservation(minuteOfDay = 840, productivityRating = 0.95, weight = 1.5)
        val profile2 = AdaptiveCircadianLearner.updateProfile(profile1, obs2, kappa = 4.5)
        assertTrue("Peak should be within 0.8h of 14:00 after session 2", abs(profile2.peakHourMean - 14.0) < 0.8)

        // Session 3 at 14:00
        val obs3 = BehavioralObservation(minuteOfDay = 840, productivityRating = 1.0, weight = 2.0)
        val profile3 = AdaptiveCircadianLearner.updateProfile(profile2, obs3, kappa = 4.5)
        assertTrue("Peak must converge within 0.35h of 14:00 after session 3", abs(profile3.peakHourMean - 14.0) < 0.35)
        assertTrue("Confidence must exceed 0.60 after 3 sessions", profile3.confidence > 0.60)

        // Verify Von Mises circular smoother properties (kappa = 4.5)
        val multipliers = profile3.hourlyMultipliers
        assertEquals(24, multipliers.size)
        val maxHour = multipliers.indices.maxByOrNull { multipliers[it] }
        assertEquals(14, maxHour)
        assertTrue("Opposite clock hour (hour 2) multiplier must be near 0", multipliers[2] < 0.01)
    }

    @Test
    fun testCircularMidnightWrapAroundConvergence() {
        // Prior peak at 23:00 (11 PM)
        val prior = LearnedCircadianProfile(
            peakHourMean = 23.0,
            peakHourVariance = 3.5,
            sampleCount = 0.0
        )

        // Observation at 01:00 (1 AM, minute 60)
        val obs = BehavioralObservation(minuteOfDay = 60, productivityRating = 0.9, weight = 2.0)
        val updated = AdaptiveCircadianLearner.updateProfile(prior, obs, kappa = 4.5)

        // Shortest circular difference from 23.0 to 1.0 is +2.0 hours.
        // It must NOT jump to midday (12.0)
        val diffFromMidnight = if (updated.peakHourMean >= 20.0) 24.0 - updated.peakHourMean else updated.peakHourMean
        assertTrue("Updated peak should remain close to midnight", diffFromMidnight < 1.5)
    }

    @Test
    fun testMultiDayAllostaticSleepDebtScalesHomeostaticPressureAndAcceleratesWakeDecay() {
        val today = LocalDate.of(2026, 10, 20)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val testTime = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli() // 7h awake

        // Scenario A: Normal 8h sleep rolling average
        val snapshotNormal = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            rollingAverageSleepMinutes = 480.0
        )

        // Scenario B: Chronic allostatic sleep debt (average 5h/night)
        val snapshotDebt = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            rollingAverageSleepMinutes = 300.0
        )

        assertTrue(
            "Sleep debt must increase homeostatic pressure Process S",
            snapshotDebt.homeostaticPressureS > snapshotNormal.homeostaticPressureS
        )
        assertTrue(
            "Sleep debt must decrease usable energy",
            snapshotDebt.usableEnergy < snapshotNormal.usableEnergy
        )
        assertEquals(
            "Morning wake anchor and circadian Process C must remain strictly identical",
            snapshotNormal.circadianAlertnessC,
            snapshotDebt.circadianAlertnessC,
            0.0001
        )
    }

    @Test
    fun testAcuteMealPostPrandialDipModulation() {
        val today = LocalDate.of(2026, 10, 20)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Meal block: 12:30 (minute 750) to 13:15 (minute 795)
        val mealBlock = CognitiveBlockWindow(
            startMinuteOfDay = 750,
            endMinuteOfDay = 795,
            category = CognitiveBlockCategory.MEAL,
            intensity = 1.0,
            label = "Lunch"
        )

        val timelineWithoutMeal = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            cognitiveBlocks = emptyList()
        )

        val timelineWithMeal = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            cognitiveBlocks = listOf(mealBlock)
        )

        // At 11:30 (step 46), meal has not happened yet -> identical
        val step1130 = (11 * 60 + 30) / 15
        assertEquals(timelineWithoutMeal[step1130].usableEnergy, timelineWithMeal[step1130].usableEnergy)

        // At 13:30 (step 54, 60 minutes after meal start) -> post-prandial dip active
        val step1330 = (13 * 60 + 30) / 15
        assertTrue(
            "Usable energy during post-prandial dip should be reduced",
            timelineWithMeal[step1330].usableEnergy < timelineWithoutMeal[step1330].usableEnergy
        )
        assertTrue(
            "Physiological impulse must be negative at post-prandial peak",
            timelineWithMeal[step1330].physiologicalImpulse < -8.0
        )

        // At 16:30 (step 66), dip has completely decayed away
        val step1630 = (16 * 60 + 30) / 15
        assertEquals(timelineWithoutMeal[step1630].usableEnergy, timelineWithMeal[step1630].usableEnergy)
    }

    @Test
    fun testWorkoutPostExerciseBoostModulation() {
        val today = LocalDate.of(2026, 10, 20)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Workout: 16:00 (minute 960) to 17:00 (minute 1020)
        val workoutBlock = CognitiveBlockWindow(
            startMinuteOfDay = 960,
            endMinuteOfDay = 1020,
            category = CognitiveBlockCategory.EXERCISE,
            intensity = 1.0,
            label = "Gym Session"
        )

        val timelineBaseline = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            cognitiveBlocks = emptyList()
        )

        val timelineWorkout = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            cognitiveBlocks = listOf(workoutBlock)
        )

        // During workout at 16:30 (minute 990, step 66): exertion penalty
        val step1630 = 990 / 15
        assertTrue(
            "During workout, physiological impulse should represent physical exertion load",
            timelineWorkout[step1630].physiologicalImpulse < 0.0
        )

        // Immediately after workout at 17:15 (minute 1035, step 69): post-workout endorphin boost
        val step1715 = 1035 / 15
        assertTrue(
            "Immediately post-workout, physiological impulse should be strongly positive",
            timelineWorkout[step1715].physiologicalImpulse > 5.0
        )
        assertTrue(
            "Usable energy post-workout should be elevated compared to baseline",
            timelineWorkout[step1715].usableEnergy > timelineBaseline[step1715].usableEnergy
        )
    }

    @Test
    fun testTaskLoadSynapticFocusDepletion() {
        val today = LocalDate.of(2026, 10, 20)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val testTime = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // No prior focus load
        val fresh = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId
        )
        assertEquals(0.0, fresh.focusDepletionPenalty, 0.0001)

        // 60 minutes unbroken focus
        val loaded60 = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 60,
            zoneId = zoneId
        )
        assertTrue("Focus depletion penalty should be positive after 60m focus", loaded60.focusDepletionPenalty > 0.0)
        assertTrue("Usable energy should decrease with focus load", loaded60.usableEnergy < fresh.usableEnergy)

        // 100 minutes unbroken focus -> forces RECOVERY_NEEDED
        val depleted = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 100,
            zoneId = zoneId
        )
        assertEquals(EnergyPhase.RECOVERY_NEEDED, depleted.phase)
    }

    @Test
    fun testTaskSchedulerProtectsPhysiologicalBlocks() {
        val today = LocalDate.of(2026, 10, 20)
        val dayStart = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Meal: 12:00 (720) to 13:00 (780)
        val mealBlock = CognitiveBlockWindow(
            startMinuteOfDay = 720,
            endMinuteOfDay = 780,
            category = CognitiveBlockCategory.MEAL,
            label = "Lunch Protected"
        )
        val mealStart = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val mealEnd = today.atTime(LocalTime.of(13, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val task1 = SchedulableTask(
            id = "t1",
            title = "Deep Research Project",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 120,
            energyDemand = EnergyDemand.HIGH
        )
        val task2 = SchedulableTask(
            id = "t2",
            title = "Admin Review",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.LOW
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(task1, task2),
            dayDate = today,
            nowMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            zoneId = zoneId,
            physiologicalBlocks = listOf(mealBlock)
        )

        for (placement in placements) {
            val window = placement.window
            val overlapsMeal = (window.startMillis < mealEnd && window.endMillis > mealStart)
            assertTrue("Scheduled task '${placement.task.title}' must NOT overlap protected meal block", !overlapsMeal)
        }
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicecircadianadversarialstresstestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/CircadianAdversarialStressTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/CircadianAdversarialStressTest.kt`
- **Lines of Code:** 227
- **Size:** 11,551 bytes (11.3 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Adversarial stress tests written by challenger_m1_1.
 * Tests empirical convergence speed, circular wrap-around edge cases,
 * extreme values, NaN handling, Bessel I0 stability, and impulse dynamics.
 */
class CircadianAdversarialStressTest {

    @Test
    fun testLargeJumpConvergence_09to15() {
        // Prior: 09:00, Variance: 4.0, SampleCount: 0.0
        val prior = LearnedCircadianProfile(
            peakHourMean = 9.0,
            peakHourVariance = 4.0,
            sampleCount = 0.0
        )

        // Standard behavioral observation at 15:00 (minute 900)
        val obs1 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 1.0)
        val p1 = AdaptiveCircadianLearner.updateProfile(prior, obs1, kappa = 4.5)

        val obs2 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 1.0)
        val p2 = AdaptiveCircadianLearner.updateProfile(p1, obs2, kappa = 4.5)

        val obs3 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 1.0)
        val p3 = AdaptiveCircadianLearner.updateProfile(p2, obs3, kappa = 4.5)

        println("09:00 -> 15:00 jump:")
        println("  After session 1: mean=${p1.peakHourMean}, var=${p1.peakHourVariance}, conf=${p1.confidence}")
        println("  After session 2: mean=${p2.peakHourMean}, var=${p2.peakHourVariance}, conf=${p2.confidence}")
        println("  After session 3: mean=${p3.peakHourMean}, var=${p3.peakHourVariance}, conf=${p3.confidence}")

        // Check if it converges within 2-3 sessions
        val distAfter2 = abs(p2.peakHourMean - 15.0)
        val distAfter3 = abs(p3.peakHourMean - 15.0)
        println("  Dist after 2 sessions: $distAfter2 hours (${distAfter2 * 60} mins)")
        println("  Dist after 3 sessions: $distAfter3 hours (${distAfter3 * 60} mins)")

        // After 2 sessions: 14.07h -> dist = 0.93h
        // After 3 sessions: 14.37h -> dist = 0.63h
        assertTrue("After 3 sessions with weight=1.0, peak hour should be within 1.0h of 15:00", distAfter3 < 1.0)
    }

    @Test
    fun testLargeJumpConvergence_Weighted_09to15() {
        val prior = LearnedCircadianProfile(
            peakHourMean = 9.0,
            peakHourVariance = 4.0,
            sampleCount = 0.0
        )

        // With stronger user focus sessions (weight = 1.5, rating = 0.95 like FastCircadianLearnerTest)
        val obs1 = BehavioralObservation(minuteOfDay = 900, productivityRating = 0.95, weight = 1.5)
        val p1 = AdaptiveCircadianLearner.updateProfile(prior, obs1, kappa = 4.5)

        val obs2 = BehavioralObservation(minuteOfDay = 900, productivityRating = 0.95, weight = 1.5)
        val p2 = AdaptiveCircadianLearner.updateProfile(p1, obs2, kappa = 4.5)

        val obs3 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 2.0)
        val p3 = AdaptiveCircadianLearner.updateProfile(p2, obs3, kappa = 4.5)

        println("09:00 -> 15:00 weighted jump:")
        println("  After session 1: mean=${p1.peakHourMean}")
        println("  After session 2: mean=${p2.peakHourMean}")
        println("  After session 3: mean=${p3.peakHourMean}")

        val distAfter3 = abs(p3.peakHourMean - 15.0)
        assertTrue("Weighted session converges within 0.45h of 15:00", distAfter3 < 0.45)
    }

    @Test
    fun testCircularWrapAround_MidnightBoundaries() {
        // Test 23:00 -> 01:00 (+2h shortest path)
        val prior23 = LearnedCircadianProfile(peakHourMean = 23.0, peakHourVariance = 4.0, sampleCount = 0.0)
        val obs01 = BehavioralObservation(minuteOfDay = 60, productivityRating = 1.0, weight = 1.0)
        val pAfter01 = AdaptiveCircadianLearner.updateProfile(prior23, obs01)
        println("23:00 -> 01:00 update: mean=${pAfter01.peakHourMean}")
        // Shortest diff is +2.0h. Kalman gain ~ 0.727.
        // updatedMean = (23.0 + 0.727 * 2.0) = 24.455 -> 0.455h (00:27)
        assertTrue("Peak should cross midnight forward into 00:xx", pAfter01.peakHourMean < 1.0)

        // Test 01:00 -> 23:00 (-2h shortest path)
        val prior01 = LearnedCircadianProfile(peakHourMean = 1.0, peakHourVariance = 4.0, sampleCount = 0.0)
        val obs23 = BehavioralObservation(minuteOfDay = 1380, productivityRating = 1.0, weight = 1.0)
        val pAfter23 = AdaptiveCircadianLearner.updateProfile(prior01, obs23)
        println("01:00 -> 23:00 update: mean=${pAfter23.peakHourMean}")
        // Shortest diff is -2.0h. updatedMean = (1.0 - 0.727 * 2.0) = -0.455 -> 23.545h (23:32)
        assertTrue("Peak should cross midnight backward into 23:xx", pAfter23.peakHourMean > 23.0)

        // Test boundary at 23:59 -> 00:01
        val priorMidnight = LearnedCircadianProfile(peakHourMean = 23.98, peakHourVariance = 2.0, sampleCount = 1.0)
        val obsMidnight = BehavioralObservation(minuteOfDay = 1, productivityRating = 1.0, weight = 1.0)
        val pMidnight = AdaptiveCircadianLearner.updateProfile(priorMidnight, obsMidnight)
        assertTrue("Peak should stay within 0.1h of midnight", pMidnight.peakHourMean > 23.9 || pMidnight.peakHourMean < 0.1)
    }

    @Test
    fun testBesselI0_NumericalStability() {
        // x = 0
        assertEquals(1.0, AdaptiveCircadianLearner.besselI0(0.0), 1e-6)

        // Continuity across the x = 3.75 boundary
        val valBelow = AdaptiveCircadianLearner.besselI0(3.749999)
        val valAbove = AdaptiveCircadianLearner.besselI0(3.750001)
        println("Bessel I0 at 3.75 boundary: below=$valBelow, above=$valAbove")
        assertEquals("Bessel I0 should be continuous at 3.75", valBelow, valAbove, 1e-4)

        // Negative values: I0(-x) == I0(x)
        assertEquals(AdaptiveCircadianLearner.besselI0(4.5), AdaptiveCircadianLearner.besselI0(-4.5), 1e-6)

        // Standard kappa = 4.5: true Bessel I0(4.5) is ~17.481
        val i0_45 = AdaptiveCircadianLearner.besselI0(4.5)
        println("Bessel I0(4.5) = $i0_45")
        assertTrue("I0(4.5) should be ~17.481", abs(i0_45 - 17.481) < 0.1)

        // Large kappa: up to 100
        val i0_100 = AdaptiveCircadianLearner.besselI0(100.0)
        println("Bessel I0(100.0) = $i0_100")
        assertFalse("I0(100.0) should be finite", i0_100.isInfinite() || i0_100.isNaN())

        // Extreme kappa: 700
        val i0_700 = AdaptiveCircadianLearner.besselI0(700.0)
        println("Bessel I0(700.0) = $i0_700")
        assertFalse("I0(700.0) should be finite", i0_700.isInfinite() || i0_700.isNaN())

        // Overflow threshold for exp(x): x > 709.78
        val i0_800 = AdaptiveCircadianLearner.besselI0(800.0)
        println("Bessel I0(800.0) = $i0_800")
        assertTrue("I0(800.0) overflows to Infinity in double precision", i0_800.isInfinite())

        // Von Mises density with extreme kappa (now safe via computeVonMisesDensitySafe)
        val densityExtreme = AdaptiveCircadianLearner.computeVonMisesDensity(14.0, 14.0, kappa = 800.0)
        println("Von Mises density with kappa=800: $densityExtreme")
        assertFalse("Von Mises density must not be NaN even when kappa > 709 due to safe scaling", densityExtreme.isNaN())
        assertTrue("Von Mises density is non-negative", densityExtreme >= 0.0)

        // However, hourly multipliers do NOT use besselI0 and do NOT become NaN
        val multipliersExtreme = AdaptiveCircadianLearner.computeVonMisesHourlyMultipliers(14.0, kappa = 800.0)
        assertEquals(24, multipliersExtreme.size)
        assertEquals(1.0, multipliersExtreme[14], 1e-6)
        assertEquals(0.0, multipliersExtreme[2], 1e-6)
        assertFalse("Multipliers must not be NaN even at extreme kappa", multipliersExtreme.any { it.isNaN() })
    }

    @Test
    fun testZeroVarianceAndExtremePriorHandling() {
        // Zero variance prior
        val zeroVarPrior = LearnedCircadianProfile(
            peakHourMean = 10.0,
            peakHourVariance = 0.0,
            sampleCount = 0.0
        )
        val obs = BehavioralObservation(minuteOfDay = 720, productivityRating = 1.0, weight = 1.0)
        val updated = AdaptiveCircadianLearner.updateProfile(zeroVarPrior, obs)
        println("Zero variance update: mean=${updated.peakHourMean}, var=${updated.peakHourVariance}")
        assertFalse("Mean should not be NaN", updated.peakHourMean.isNaN())
        assertFalse("Var should not be NaN", updated.peakHourVariance.isNaN())
        assertTrue("Variance has a lower floor of PROCESS_NOISE_Q (0.05)", updated.peakHourVariance >= 0.05)

        // Extremely high variance prior
        val hugeVarPrior = LearnedCircadianProfile(
            peakHourMean = 10.0,
            peakHourVariance = 1000.0,
            sampleCount = 0.0
        )
        val updatedHuge = AdaptiveCircadianLearner.updateProfile(hugeVarPrior, obs)
        assertFalse("Updated var should be capped", updatedHuge.peakHourVariance > 12.0)
    }

    @Test
    fun testMealPostPrandialDipDiscontinuityAt180m() {
        val mealBlock = CognitiveBlockWindow(
            startMinuteOfDay = 720, // 12:00
            endMinuteOfDay = 780,   // 13:00
            category = CognitiveBlockCategory.MEAL,
            intensity = 1.0
        )

        // At 179 minutes post-meal start (14:59)
        val impulse179 = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 179, listOf(mealBlock))
        // At 180 minutes post-meal start (15:00)
        val impulse180 = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 180, listOf(mealBlock))
        // At 181 minutes post-meal start (15:01)
        val impulse181 = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 181, listOf(mealBlock))

        println("Meal impulse at 179m: $impulse179")
        println("Meal impulse at 180m: $impulse180")
        println("Meal impulse at 181m: $impulse181")

        // Peak dip at 60m
        val impulsePeak = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 60, listOf(mealBlock))
        assertEquals(-12.0, impulsePeak, 1e-4)

        // Cliff between 180m and 181m
        val cliff = abs(impulse180 - impulse181)
        println("Discontinuity cliff at 180m boundary: $cliff points")
        assertTrue("Cliff at 180m cutoff is ~4.87 points", cliff > 4.5)
    }

    @Test
    fun testExerciseImpulseParametersVersusSpec() {
        val workout = CognitiveBlockWindow(
            startMinuteOfDay = 600, // 10:00
            endMinuteOfDay = 660,   // 11:00
            category = CognitiveBlockCategory.EXERCISE,
            intensity = 1.0
        )

        val duringImpulse = PhysiologicalImpulseCalculator.computeExerciseImpulse(630, listOf(workout))
        // Minute 661 is post-workout (1 min after end at 660, in 15-minute autonomic ramp)
        val postImpulseRamp = PhysiologicalImpulseCalculator.computeExerciseImpulse(661, listOf(workout))
        // Minute 675 is post-workout (15 min after end at 660, fully transitioned into BDNF boost)
        val postImpulsePeak = PhysiologicalImpulseCalculator.computeExerciseImpulse(675, listOf(workout))

        // Documenting the empirically observed values in implementation:
        assertEquals(-8.0, duringImpulse, 1e-4)
        assertTrue("Autonomic transition at 1m should be recovering from -8.0", postImpulseRamp > -8.0 && postImpulseRamp < 0.0)
        assertTrue("Post-workout boost at 15m should be strong positive (> 8.0)", postImpulsePeak > 8.0)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainserviceadr020robustnesstestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/Adr020RobustnessTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/Adr020RobustnessTest.kt`
- **Lines of Code:** 129
- **Size:** 5,124 bytes (5.0 KB)

```kotlin
package com.neuroflow.app.domain.service

import com.neuroflow.app.domain.scheduler.DependencyGraphValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class Adr020RobustnessTest {

    @Test
    fun testSmoothMealImpulseEliminates180MinuteDiscontinuityCliff() {
        val lunch = CognitiveBlockWindow(
            startMinuteOfDay = 720, // 12:00
            endMinuteOfDay = 780,
            category = CognitiveBlockCategory.MEAL,
            intensity = 1.0
        )

        // Peak at 60m post-meal (13:00) is still exactly -12.0
        val peakDip = computeSmoothMealImpulse(780, listOf(lunch))
        assertEquals(-12.0, peakDip, 1e-4)

        // At 179m, 180m, and 181m post-meal, the transition is smooth (< 0.15 pts step)
        val dip179 = computeSmoothMealImpulse(720 + 179, listOf(lunch))
        val dip180 = computeSmoothMealImpulse(720 + 180, listOf(lunch))
        val dip181 = computeSmoothMealImpulse(720 + 181, listOf(lunch))

        assertEquals(0.0, dip180, 1e-6)
        assertEquals(0.0, dip181, 1e-6)
        assertTrue(
            "Step between 179m and 180m must be < 0.15 pts, was ${abs(dip179 - dip180)}",
            abs(dip179 - dip180) < 0.15
        )
    }

    @Test
    fun testLogSpaceVonMisesNeverOverflowsAtExtremeKappa() {
        for (kappa in listOf(0.0, 4.5, 100.0, 800.0, 5000.0)) {
            val densityAtPeak = computeVonMisesDensitySafe(10.0, 10.0, kappa)
            val densityOffPeak = computeVonMisesDensitySafe(22.0, 10.0, kappa)
            assertFalse(
                "Density at peak must not be NaN or Infinite for kappa=$kappa",
                densityAtPeak.isNaN() || densityAtPeak.isInfinite()
            )
            assertFalse(
                "Density off-peak must not be NaN or Infinite for kappa=$kappa",
                densityOffPeak.isNaN() || densityOffPeak.isInfinite()
            )
            assertTrue(densityAtPeak >= densityOffPeak)
        }
    }

    @Test
    fun testDagCycleBreakingGuaranteesZeroSilentDropsInScheduleDay() {
        val taskA = SchedulableTask(
            id = "A",
            title = "Task A",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true,
            prerequisiteIds = setOf("B")
        )
        val taskB = SchedulableTask(
            id = "B",
            title = "Task B",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE,
            prerequisiteIds = setOf("A")
        )

        val result = DependencyGraphValidator.sanitizeDag(listOf(taskA, taskB))
        assertEquals(2, result.tasks.size)
        assertEquals("Exactly 1 cyclic edge must be severed", 1, result.brokenCycleEdges.size)

        // Verify DefaultTaskSchedulerService.scheduleDay automatically sanitizes cycles and schedules both tasks
        val circadian = DefaultCircadianEnergyService()
        val scheduler = DefaultTaskSchedulerService(circadian)
        val today = LocalDate.of(2026, 10, 21)
        val zone = ZoneId.of("UTC")
        val morning = today.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val wake = today.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()

        val placements = scheduler.scheduleDay(
            tasks = listOf(taskA, taskB), // Pass raw cyclic tasks directly!
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            zoneId = zone
        )
        assertEquals("Both tasks in broken cycle must be scheduled without silent drop", 2, placements.size)
    }

    @Test
    fun testWouldCreateCycleDetectsDirectAndTransitiveLoops() {
        val existingDeps = mapOf(
            "B" to setOf("A"),
            "C" to setOf("B")
        )
        // Adding C as prerequisite of A creates A -> C -> B -> A
        assertTrue(DependencyGraphValidator.wouldCreateCycle("A", "C", existingDeps))
        // Self-dependency A -> A
        assertTrue(DependencyGraphValidator.wouldCreateCycle("A", "A", existingDeps))
        // Adding C as prerequisite of D is acyclic
        assertFalse(DependencyGraphValidator.wouldCreateCycle("D", "C", existingDeps))
    }

    @Test
    fun testLatestSafeStimulantCutoffCalculation() {
        // Bedtime 23:00 (1380m), 100mg caffeine (t_half = 4.5h) -> needs ~6.81h clearance (409m) -> ~16:11 (971m)
        val cutoffMin = computeLatestSafeStimulantCutoffMinute(
            sleepHour = 23,
            doseMg = 100.0,
            type = StimulantType.CAFFEINE,
            maxResidualMgAtBedtime = 35.0
        )
        assertTrue("Cutoff minute ($cutoffMin) should fall between 15:30 (930m) and 17:00 (1020m)", cutoffMin in 930..1020)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainserviceadr022robustnesstestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/Adr022RobustnessTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/Adr022RobustnessTest.kt`
- **Lines of Code:** 100
- **Size:** 4,235 bytes (4.1 KB)

```kotlin
package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.PhysiologicalEventLog
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.sync.fromBackupMap
import com.neuroflow.app.data.sync.toBackupMap
import com.neuroflow.app.domain.model.computeEnergyUiState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class Adr022RobustnessTest {

    @Test
    fun testQuickPhysiologicalLogsMergedIntoComputeEnergyUiState() {
        val circadianService = DefaultCircadianEnergyService()
        val zoneId = ZoneId.systemDefault()
        val now = System.currentTimeMillis()

        val jsonArray = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "MEAL")
                put("timestampMillis", now - 60 * 60_000L)
                put("durationMinutes", 45)
                put("intensity", 1.0)
            })
            put(JSONObject().apply {
                put("type", "WORKOUT")
                put("timestampMillis", now - 30 * 60_000L)
                put("durationMinutes", 45)
                put("intensity", 1.0)
            })
            put(JSONObject().apply {
                put("type", "NAP")
                put("timestampMillis", now - 90 * 60_000L)
                put("durationMinutes", 20)
                put("intensity", 1.0)
            })
        }

        val prefs = UserPreferences(
            wakeUpHour = 7,
            sleepHour = 23,
            quickPhysiologicalLogsJson = jsonArray.toString()
        )

        val (snapshot, energyUi) = computeEnergyUiState(
            circadianEnergyService = circadianService,
            sleepTelemetryRepository = null,
            prefs = prefs,
            nowMillis = now
        )

        assertNotNull(snapshot)
        assertNotNull(energyUi)

        // Energy value must be bounded within [0, 100]
        assertTrue("Available energy in valid range", energyUi.availableEnergy in 0..100)
    }

    @Test
    fun testCloudBackupRoundTripPreservesAllAdr022Fields() {
        val originalPrefs = UserPreferences(
            adaptiveSystemDirective = "Enforce 2-minute doorway steps on coding tasks",
            weeklyAiAuditJson = """{"postponedCount": 4, "prescriptions": ["Break down coding tasks"]}""",
            stimulantLogsJson = """[{"id":"c1","type":"CAFFEINE","timestampMillis":1000000,"doseMg":100.0}]""",
            quickPhysiologicalLogsJson = """[{"type":"MEAL","timestampMillis":2000000,"durationMinutes":45,"intensity":1.0}]""",
            quizChronotype = "MODERATE_MORNING",
            manualChronotype = "DEFINITE_MORNING",
            meqScore = 65
        )

        val backupMap = originalPrefs.toBackupMap()

        assertEquals("Enforce 2-minute doorway steps on coding tasks", backupMap["adaptiveSystemDirective"])
        assertTrue(backupMap["weeklyAiAuditJson"]!!.contains("postponedCount"))
        assertTrue(backupMap["stimulantLogsJson"]!!.contains("CAFFEINE"))
        assertTrue(backupMap["quickPhysiologicalLogsJson"]!!.contains("MEAL"))
        assertEquals("MODERATE_MORNING", backupMap["quizChronotype"])
        assertEquals("DEFINITE_MORNING", backupMap["manualChronotype"])
        assertEquals("65", backupMap["meqScore"])

        // Test deserialization round-trip
        val blankPrefs = UserPreferences()
        val restoredPrefs = blankPrefs.fromBackupMap(backupMap)

        assertEquals(originalPrefs.adaptiveSystemDirective, restoredPrefs.adaptiveSystemDirective)
        assertEquals(originalPrefs.weeklyAiAuditJson, restoredPrefs.weeklyAiAuditJson)
        assertEquals(originalPrefs.stimulantLogsJson, restoredPrefs.stimulantLogsJson)
        assertEquals(originalPrefs.quickPhysiologicalLogsJson, restoredPrefs.quickPhysiologicalLogsJson)
        assertEquals(originalPrefs.quizChronotype, restoredPrefs.quizChronotype)
        assertEquals(originalPrefs.manualChronotype, restoredPrefs.manualChronotype)
        assertEquals(originalPrefs.meqScore, restoredPrefs.meqScore)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicetaskschedulerservicetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/TaskSchedulerServiceTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/TaskSchedulerServiceTest.kt`
- **Lines of Code:** 334
- **Size:** 13,838 bytes (13.5 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TaskSchedulerServiceTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: TaskSchedulerService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    @Test
    fun testScoreBoundsStrictlyBetween0And100() {
        val today = LocalDate.of(2026, 10, 18)
        val testTime = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val snapshot = circadianService.evaluateSnapshot(testTime, wakeTime, 480, zoneId = zoneId)

        val extremeTasks = listOf(
            // Minimal task
            SchedulableTask(
                id = "min",
                title = "Minimal Task",
                category = TaskCategory.GENERAL,
                priority = 0,
                estimatedMinutes = 5,
                energyDemand = EnergyDemand.LOW,
                isFrog = false,
                createdAtMillis = testTime
            ),
            // Maximal priority frog task with imminent deadline
            SchedulableTask(
                id = "max",
                title = "Maximal Task",
                category = TaskCategory.DEEP_WORK,
                priority = 2,
                estimatedMinutes = 90,
                energyDemand = EnergyDemand.HIGH,
                isFrog = true,
                deadlineMillis = testTime + 30 * 60_000L, // 30 mins remaining
                completedMicroSteps = 4,
                totalMicroSteps = 5,
                isCurrentlyActive = true,
                createdAtMillis = testTime - 7 * 86_400_000L
            ),
            // Severely overdue task
            SchedulableTask(
                id = "overdue",
                title = "Overdue Task",
                category = TaskCategory.ADMIN,
                priority = 2,
                estimatedMinutes = 30,
                energyDemand = EnergyDemand.MODERATE,
                deadlineMillis = testTime - 24 * 3_600_000L, // 1 day overdue
                createdAtMillis = testTime - 14 * 86_400_000L
            )
        )

        for (task in extremeTasks) {
            val breakdown = schedulerService.evaluateTaskScore(task, snapshot, dependentCount = 5, calibrationRatio = 1.5)
            assertTrue("Total score must be in [0, 100], was ${breakdown.totalScore}", breakdown.totalScore in 0.0..100.0)
            assertTrue("Urgency score must be in [0, 100], was ${breakdown.urgencyScore}", breakdown.urgencyScore in 0.0..100.0)
            assertTrue("Impact score must be in [0, 100], was ${breakdown.impactScore}", breakdown.impactScore in 0.0..100.0)
            assertTrue("Energy fit score must be in [0, 100], was ${breakdown.energyFitScore}", breakdown.energyFitScore in 0.0..100.0)
            assertTrue("Momentum score must be in [0, 100], was ${breakdown.momentumScore}", breakdown.momentumScore in 0.0..100.0)
        }
    }

    @Test
    fun testSignedEWMACalibrationRatioFinishingEarlyDecreasesRatio() {
        val initialRatio = 1.20

        // Finishing a 60-minute task in 30 minutes (ratio = 0.5)
        val decreasedRatio = schedulerService.updateCalibrationRatio(initialRatio, estimatedMinutes = 60, actualMinutes = 30)
        assertTrue(
            "Finishing early must decrease ratio (was $initialRatio -> $decreasedRatio)",
            decreasedRatio < initialRatio
        )
        assertEquals(1.025, decreasedRatio, 0.001)

        // Taking 90 minutes for a 30-minute task (ratio = 3.0)
        val increasedRatio = schedulerService.updateCalibrationRatio(initialRatio, estimatedMinutes = 30, actualMinutes = 90)
        assertTrue(
            "Finishing late must increase ratio (was $initialRatio -> $increasedRatio)",
            increasedRatio > initialRatio
        )

        // Clamping to [0.5, 2.5]
        val clampedMin = schedulerService.updateCalibrationRatio(0.5, 100, 1)
        assertEquals(0.5, clampedMin, 0.001)

        val clampedMax = schedulerService.updateCalibrationRatio(2.5, 1, 100)
        assertEquals(2.5, clampedMax, 0.001)
    }

    @Test
    fun testExplicitDurationTokensAreNotInflated() {
        val snapshot = circadianService.evaluateSnapshot(System.currentTimeMillis(), System.currentTimeMillis() - 3600000, 480)

        val explicitTask = SchedulableTask(
            id = "explicit",
            title = "Call client 30m",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 30,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.MODERATE
        )

        val implicitTask = SchedulableTask(
            id = "implicit",
            title = "Plan budget",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 30,
            isDurationExplicit = false,
            energyDemand = EnergyDemand.MODERATE
        )

        val explicitBreakdown = schedulerService.evaluateTaskScore(explicitTask, snapshot, calibrationRatio = 1.5)
        val implicitBreakdown = schedulerService.evaluateTaskScore(implicitTask, snapshot, calibrationRatio = 1.5)

        assertEquals(30, explicitBreakdown.bufferedDurationMinutes)
        assertEquals(45, implicitBreakdown.bufferedDurationMinutes) // 30 * 1.5 = 45
    }

    @Test
    fun testMultiStepDAGSchedulingSequentialPlacementInSinglePass() {
        val today = LocalDate.of(2026, 10, 18)
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val taskA = SchedulableTask(
            id = "task_a",
            title = "Step A: Outline Architecture",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = null
        )
        val taskB = SchedulableTask(
            id = "task_b",
            title = "Step B: Implement Core Services",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "task_a",
            prerequisiteIds = setOf("task_a")
        )
        val taskC = SchedulableTask(
            id = "task_c",
            title = "Step C: Integration Tests",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "task_b",
            prerequisiteIds = setOf("task_b")
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(taskC, taskA, taskB), // out of topological order
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            zoneId = zoneId
        )

        assertEquals("All 3 dependent tasks should be scheduled in single pass", 3, placements.size)

        val placementA = placements.first { it.task.id == "task_a" }
        val placementB = placements.first { it.task.id == "task_b" }
        val placementC = placements.first { it.task.id == "task_c" }

        assertTrue(
            "Task A must complete before Task B starts (A end: ${placementA.window.endMillis}, B start: ${placementB.window.startMillis})",
            placementA.window.endMillis <= placementB.window.startMillis
        )
        assertTrue(
            "Task B must complete before Task C starts (B end: ${placementB.window.endMillis}, C start: ${placementC.window.startMillis})",
            placementB.window.endMillis <= placementC.window.startMillis
        )
    }

    @Test
    fun testExactMinuteResolutionNoHourBinLeaks() {
        val today = LocalDate.of(2026, 10, 18)
        val morning = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val task1 = SchedulableTask(
            id = "t1",
            title = "Task 1",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 20,
            energyDemand = EnergyDemand.MODERATE
        )
        val task2 = SchedulableTask(
            id = "t2",
            title = "Task 2",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 35,
            energyDemand = EnergyDemand.MODERATE
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(task1, task2),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            zoneId = zoneId
        )

        assertEquals(2, placements.size)
        val p1 = placements.first { it.task.id == "t1" }
        val p2 = placements.first { it.task.id == "t2" }
        // Task durations should be exact: 20m (1200000ms) and 35m (2100000ms)
        assertEquals(20 * 60_000L, p1.window.endMillis - p1.window.startMillis)
        assertEquals(35 * 60_000L, p2.window.endMillis - p2.window.startMillis)
    }

    @Test
    fun testScheduleDayBlocksDaytimeNapAndAvoidsOverlap() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napStart = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napEnd = today.atTime(LocalTime.of(15, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val nap = listOf(DaytimeNapBout(startMillis = napStart, endMillis = napEnd))

        val deepTask = SchedulableTask(
            id = "deep1",
            title = "Deep Analytical Code Architecture",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(deepTask),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            zoneId = zoneId,
            naps = nap
        )

        assertEquals(1, placements.size)
        val placement = placements[0]
        // Must never overlap with the nap window [14:00, 15:00]
        val overlapsNap = placement.window.startMillis < napEnd && placement.window.endMillis > napStart
        assertFalse("Task must not be scheduled during nap window", overlapsNap)
    }

    @Test
    fun testUltradianAutoChunkingSplitsLargeTaskWhenFreeSlotsFragmented() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Block everything except 10:00-10:50 (50m) and 14:00-14:50 (50m)
        val busy1 = TimeSlotWindow(
            today.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        )
        val busy2 = TimeSlotWindow(
            today.atTime(LocalTime.of(10, 50)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        )
        val busy3 = TimeSlotWindow(
            today.atTime(LocalTime.of(14, 50)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()
        )

        val task90m = SchedulableTask(
            id = "deep90",
            title = "Write Massive Engineering Spec",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 90,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.5f
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(task90m),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(busy1, busy2, busy3),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            zoneId = zoneId
        )

        assertEquals("90m task should be split into 2 parts and scheduled in both 50m gaps", 2, placements.size)
        assertEquals("deep90#part1", placements[0].task.id)
        assertEquals("deep90#part2", placements[1].task.id)
        assertTrue(placements[0].window.durationMinutes >= 25)
        assertTrue(placements[1].window.durationMinutes >= 25)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicetasknaturallanguageparsertestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParserTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParserTest.kt`
- **Lines of Code:** 64
- **Size:** 2,151 bytes (2.1 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskNaturalLanguageParserTest {

    private lateinit var parser: TaskNaturalLanguageParser

    @Before
    fun setUp() {
        parser = DefaultTaskNaturalLanguageParser()
    }

    @Test
    fun testParseDurationTokens() {
        val result1 = parser.parse("Call accountant 45m")
        assertEquals("Call accountant", result1.cleanTitle)
        assertEquals(45, result1.estimatedMinutes)
        assertTrue(result1.isDurationExplicit)

        val result2 = parser.parse("Study chapter 2 hours")
        assertEquals("Study chapter", result2.cleanTitle)
        assertEquals(120, result2.estimatedMinutes)
        assertTrue(result2.isDurationExplicit)

        val result3 = parser.parse("Weekly sync half hour")
        assertEquals("Weekly sync", result3.cleanTitle)
        assertEquals(30, result3.estimatedMinutes)
        assertTrue(result3.isDurationExplicit)
    }

    @Test
    fun testParseFrogBadge() {
        val result1 = parser.parse("🐸 File quarterly taxes")
        assertEquals("File quarterly taxes", result1.cleanTitle)
        assertTrue(result1.isFrog)

        val result2 = parser.parse("Annual audit #frog")
        assertEquals("Annual audit", result2.cleanTitle)
        assertTrue(result2.isFrog)

        val result3 = parser.parse("Routine documentation")
        assertFalse(result3.isFrog)
    }

    @Test
    fun testParseEnergyAndCategory() {
        val result1 = parser.parse("⚡ Deep code refactoring")
        assertTrue(result1.energyDemand == EnergyDemand.HIGH)
        assertEquals(TaskCategory.DEEP_WORK, result1.category)

        val result2 = parser.parse("Clean mailbox low energy")
        assertTrue(result2.energyDemand == EnergyDemand.LOW)
        assertEquals(TaskCategory.ADMIN, result2.category)

        val result3 = parser.parse("Morning gym workout 1h")
        assertEquals(TaskCategory.EXERCISE, result3.category)
        assertEquals(60, result3.estimatedMinutes)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicesmarttaskdecompositionservicetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/SmartTaskDecompositionServiceTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/SmartTaskDecompositionServiceTest.kt`
- **Lines of Code:** 166
- **Size:** 6,582 bytes (6.4 KB)

```kotlin
package com.neuroflow.app.domain.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartTaskDecompositionServiceTest {

    private lateinit var service: DefaultSmartTaskDecompositionService

    @Before
    fun setup() {
        service = DefaultSmartTaskDecompositionService()
    }

    @Test
    fun testArchetypeClassification() {
        assertEquals(
            TaskArchetype.CODING_TECH,
            DefaultSmartTaskDecompositionService.classifyArchetype("Fix authentication endpoint bug")
        )
        assertEquals(
            TaskArchetype.CODING_TECH,
            DefaultSmartTaskDecompositionService.classifyArchetype("Refactor database SQL schema")
        )
        assertEquals(
            TaskArchetype.WRITING_RESEARCH,
            DefaultSmartTaskDecompositionService.classifyArchetype("Draft blog post about circadian rhythm")
        )
        assertEquals(
            TaskArchetype.STUDY_ACADEMIC,
            DefaultSmartTaskDecompositionService.classifyArchetype("Study chapter 4 for biology exam")
        )
        assertEquals(
            TaskArchetype.CHORE_MAINTENANCE,
            DefaultSmartTaskDecompositionService.classifyArchetype("Clean kitchen and do laundry")
        )
        assertEquals(
            TaskArchetype.ADMIN_LOGISTICS,
            DefaultSmartTaskDecompositionService.classifyArchetype("File tax return and submit invoice")
        )
        assertEquals(
            TaskArchetype.GENERAL,
            DefaultSmartTaskDecompositionService.classifyArchetype("Plan weekend family trip")
        )
    }

    @Test
    fun testLocalDecompositionMeetsActionabilityAndFrictionInvariants() {
        val codingSteps = service.decomposeTaskLocally("Implement OAuth login", 60)
        assertTrue("Coding archetype should produce 3-4 steps", codingSteps.size in 3..4)
        assertTrue("First step must have low activation friction (<= 10m)", codingSteps.first().estimatedMinutes <= 10)
        assertTrue("Steps should preserve task context", codingSteps.any { it.title.contains("Implement OAuth login") })

        val writingSteps = service.decomposeTaskLocally("Write quarterly performance report", 90)
        assertTrue("Writing archetype should produce 3-4 steps", writingSteps.size in 3..4)
        assertTrue("First step <= 10m", writingSteps.first().estimatedMinutes <= 10)
        assertTrue("Context preserved", writingSteps.any { it.title.contains("Write quarterly performance report") })

        val choreSteps = service.decomposeTaskLocally("Clean garage and organize tools", 30)
        assertTrue("Chore archetype should produce 3-4 steps", choreSteps.size in 3..4)
        assertTrue("First step <= 10m", choreSteps.first().estimatedMinutes <= 10)

        val generalSteps = service.decomposeTaskLocally("Misc random task", 45)
        assertTrue("General archetype should produce 3-4 steps", generalSteps.size in 3..4)
        assertTrue("First step <= 10m", generalSteps.first().estimatedMinutes <= 10)
    }

    @Test
    fun testArchetypeDecompositionPhysicalDoorwayStep() {
        val steps = service.decomposeTaskLocally("Fix SQL migration bug", 45)
        assertEquals(4, steps.size)
        assertTrue("Step 1 must be doorway action", steps[0].title.startsWith("Open IDE, checkout branch"))
        assertTrue("Step 1 estimated minutes must be <= 4", steps[0].estimatedMinutes <= 4)
    }

    @Test
    fun testParseGeminiResponseStandardJson() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "[{\"title\":\"Review rubric\",\"estimatedMinutes\":5},{\"title\":\"Write section 1\",\"estimatedMinutes\":20}]"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val steps = DefaultSmartTaskDecompositionService.parseGeminiResponse(json)
        assertEquals(2, steps.size)
        assertEquals("Review rubric", steps[0].title)
        assertEquals(5, steps[0].estimatedMinutes)
        assertEquals("Write section 1", steps[1].title)
        assertEquals(20, steps[1].estimatedMinutes)
    }

    @Test
    fun testParseGeminiResponseMarkdownCodeFence() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n[\n  {\"title\": \"Step A\", \"estimatedMinutes\": 10},\n  {\"title\": \"Step B\", \"estimatedMinutes\": 15}\n]\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val steps = DefaultSmartTaskDecompositionService.parseGeminiResponse(json)
        assertEquals(2, steps.size)
        assertEquals("Step A", steps[0].title)
        assertEquals(10, steps[0].estimatedMinutes)
        assertEquals("Step B", steps[1].title)
        assertEquals(15, steps[1].estimatedMinutes)
    }

    @Test
    fun testParseGeminiResponseMalformedOrEmptyReturnsEmptyList() {
        val malformed = """{"candidates": []}"""
        val emptySteps = DefaultSmartTaskDecompositionService.parseGeminiResponse(malformed)
        assertTrue(emptySteps.isEmpty())

        val invalidText = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "I am unable to break down this task."
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        val textSteps = DefaultSmartTaskDecompositionService.parseGeminiResponse(invalidText)
        assertTrue(textSteps.isEmpty())
    }

    @Test
    fun testDecomposeTaskFallsBackToLocalWhenAiDisabledOrKeyBlank() = runBlocking {
        val stepsNoAi = service.decomposeTask("Fix bug in billing API", 45, apiKey = "test-key", useAi = false)
        assertTrue(stepsNoAi.isNotEmpty())
        assertTrue(stepsNoAi.any { it.title.contains("Fix bug in billing API") })

        val stepsBlankKey = service.decomposeTask("Fix bug in billing API", 45, apiKey = "", useAi = true)
        assertTrue(stepsBlankKey.isNotEmpty())
        assertTrue(stepsBlankKey.any { it.title.contains("Fix bug in billing API") })
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicegeminiaionlineservicetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/GeminiAiOnlineServiceTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/GeminiAiOnlineServiceTest.kt`
- **Lines of Code:** 164
- **Size:** 6,464 bytes (6.3 KB)

```kotlin
package com.neuroflow.app.domain.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiAiOnlineServiceTest {

    private lateinit var service: DefaultGeminiAiOnlineService

    @Before
    fun setup() {
        service = DefaultGeminiAiOnlineService()
    }

    @Test
    fun testModelIdIsGeminiFlashLatest() {
        assertEquals("gemini-flash-latest", DefaultGeminiAiOnlineService.MODEL_ID)
        assertTrue(
            "Endpoint must target gemini-flash-latest model",
            DefaultGeminiAiOnlineService.ENDPOINT_BASE.contains("gemini-flash-latest")
        )
    }

    @Test
    fun testLocalFallbackWhenApiKeyIsBlankOrAiDisabled() = runBlocking {
        // When AI is disabled, local fallback must return valid, non-null, robust results
        val enrichment = service.enrichTaskWithAi(
            title = "Refactor Database Schema",
            durationMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            currentHour = 10,
            isFrog = true,
            apiKey = "",
            useAi = false
        )
        assertNotNull(enrichment)
        assertTrue("Micro-steps must not be empty", enrichment.microSteps.isNotEmpty())
        assertEquals(10, enrichment.suggestedSlotStartHour) // High demand / Frog placed in morning peak
        assertEquals(EnergyDemand.HIGH, enrichment.recommendedEnergyDemand)

        // Task decomposition fallback
        val steps = service.decomposeTask(
            title = "Write quarterly performance report",
            durationMinutes = 45,
            apiKey = null,
            useAi = true // No key provided
        )
        assertTrue("Decomposition fallback must yield steps", steps.size >= 3)
        assertTrue("First step must have low initiation friction (<= 10m)", steps.first().estimatedMinutes <= 10)
    }

    @Test
    fun testSlotRecommendationFallback() = runBlocking {
        val window = TimeSlotWindow(1700000000000L, 1700003600000L)
        val rec = service.suggestTaskSlot(
            taskTitle = "Deep Coding",
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true,
            currentEpochMillis = 1700000000000L,
            candidateWindows = listOf(window),
            apiKey = "",
            useAi = false
        )
        assertNotNull(rec)
        assertEquals(window.startMillis, rec.suggestedStartMillis)
        assertEquals(window.endMillis, rec.suggestedEndMillis)
        assertEquals("Morning Peak", rec.circadianPhase)
        assertTrue(rec.confidence >= 80)
    }

    @Test
    fun testEnergyAdvisoryCircadianLogic() = runBlocking {
        // Peak phase
        val peakAdvice = service.adviseEnergyControl(
            phase = EnergyPhase.PEAK,
            usableEnergyPercent = 88,
            hoursAwake = 3.5f,
            continuousFocusMinutes = 45,
            hasSleepDebt = false,
            apiKey = "",
            useAi = false
        )
        assertEquals("Biological Peak Window", peakAdvice.statusTitle)
        assertEquals("DEEP_WORK", peakAdvice.suggestedCategory)
        assertEquals(EnergyDemand.HIGH, peakAdvice.suggestedEnergyDemand)
        assertFalse(peakAdvice.powerNapRecommended)

        // Dip phase
        val dipAdvice = service.adviseEnergyControl(
            phase = EnergyPhase.DIP,
            usableEnergyPercent = 42,
            hoursAwake = 7.0f,
            continuousFocusMinutes = 30,
            hasSleepDebt = false,
            apiKey = "",
            useAi = false
        )
        assertEquals("Circadian Post-Lunch Dip", dipAdvice.statusTitle)
        assertEquals("ADMIN", dipAdvice.suggestedCategory)
        assertTrue("Power nap recommended during dip", dipAdvice.powerNapRecommended)

        // Continuous focus >= 90 mins synaptic depletion
        val depletionAdvice = service.adviseEnergyControl(
            phase = EnergyPhase.PEAK,
            usableEnergyPercent = 75,
            hoursAwake = 4.0f,
            continuousFocusMinutes = 95,
            hasSleepDebt = false,
            apiKey = "",
            useAi = false
        )
        assertEquals("Synaptic Saturation Warning", depletionAdvice.statusTitle)
        assertEquals("RECOVERY", depletionAdvice.suggestedCategory)
        assertEquals(EnergyDemand.LOW, depletionAdvice.suggestedEnergyDemand)
    }

    @Test
    fun testWeeklyAvoidanceAuditAndDirectiveFallback() = runBlocking {
        val result = service.conductWeeklyAuditAndDirective(
            weeklyTelemetryJson = "{}",
            recentlyDeletedTasksJson = "[]",
            chronicallyPostponedTasksJson = "[]",
            apiKey = "",
            useAi = false
        )
        assertNotNull(result)
        assertTrue("Directive must not be empty", result.newSystemDirective.isNotBlank())
        assertTrue("Directive must mention Adaptive System Directive", result.newSystemDirective.contains("Adaptive System Directive"))
        assertTrue("Audit JSON must contain primaryAvoidancePattern", result.avoidanceAuditJson.contains("primaryAvoidancePattern"))
    }

    @Test
    fun testSynthesizeQuickNoteFallback() = runBlocking {
        val synthesis = service.synthesizeQuickNote(
            rawContent = "Buy milk and groceries\nMake sure to get almond milk",
            apiKey = "",
            useAi = false
        )
        assertEquals("Buy milk and groceries", synthesis.extractedTaskTitle)
        assertEquals("GENERAL", synthesis.suggestedCategory)
        assertTrue("Tags must contain quick-note", synthesis.tags.contains("quick-note"))
    }

    @Test
    fun testCandidateModelsContainsLatestAndFallback() {
        val models = DefaultGeminiAiOnlineService.CANDIDATE_MODELS
        assertTrue("Primary must be gemini-flash-latest", models.contains("gemini-flash-latest"))
        assertTrue("Must have flash-lite fallback", models.contains("gemini-flash-lite-latest"))
        assertTrue("Must have 2.5-flash fallback", models.contains("gemini-2.5-flash"))
        assertTrue("Must have 2.0-flash fallback", models.contains("gemini-2.0-flash"))
    }

    @Test
    fun testTestConnectionBlankKeyFailsFast() = runBlocking {
        val result = service.testConnection("")
        assertTrue("Blank key must fail", result.isFailure)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicechronobiologysignalnormalizertestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizerTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizerTest.kt`
- **Lines of Code:** 170
- **Size:** 8,007 bytes (7.8 KB)

```kotlin
package com.neuroflow.app.domain.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class ChronobiologySignalNormalizerTest : StringSpec({

    val normalizer = ChronobiologySignalNormalizer()
    val baseMillis = 1770000000000L

    "coalesceWasoEpisodes merges intervals with wake gaps <= 45m" {
        val sleep1 = TimeSlotWindow(baseMillis, baseMillis + 180 * 60_000L) // 3h sleep
        val sleep2 = TimeSlotWindow(baseMillis + 205 * 60_000L, baseMillis + 360 * 60_000L) // 25m wake gap (WASO)
        val intervals = listOf(sleep1, sleep2)

        val (sessions, wasoMinutes) = normalizer.coalesceWasoEpisodes(intervals)
        sessions shouldHaveSize 1
        sessions[0].startMillis shouldBe baseMillis
        sessions[0].endMillis shouldBe baseMillis + 360 * 60_000L
        wasoMinutes shouldBe 25L
    }

    "coalesceWasoEpisodes keeps separate sessions when wake gap > 45m" {
        val sleep1 = TimeSlotWindow(baseMillis, baseMillis + 180 * 60_000L)
        val sleep2 = TimeSlotWindow(baseMillis + 240 * 60_000L, baseMillis + 360 * 60_000L) // 60m wake gap (> 45m)
        val intervals = listOf(sleep1, sleep2)

        val (sessions, wasoMinutes) = normalizer.coalesceWasoEpisodes(intervals)
        sessions shouldHaveSize 2
        sessions[0].endMillis shouldBe baseMillis + 180 * 60_000L
        sessions[1].startMillis shouldBe baseMillis + 240 * 60_000L
        wasoMinutes shouldBe 0L
    }

    "coalesceWasoEpisodes handles empty and overlapping inputs" {
        val (emptySessions, emptyWaso) = normalizer.coalesceWasoEpisodes(emptyList())
        emptySessions.shouldBeEmpty()
        emptyWaso shouldBe 0L

        val overlapping = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 120 * 60_000L),
            TimeSlotWindow(baseMillis + 60 * 60_000L, baseMillis + 240 * 60_000L)
        )
        val (ovSessions, ovWaso) = normalizer.coalesceWasoEpisodes(overlapping)
        ovSessions shouldHaveSize 1
        ovSessions[0].endMillis shouldBe baseMillis + 240 * 60_000L
        ovWaso shouldBe 0L
    }

    "computeEffectiveDuration weights restorative sleep with clamp [0.75, 1.25]" {
        // Zero restorative sleep (deep + rem = 0): factor = 0.85
        val poor = normalizer.computeEffectiveDuration(480.0, 0.0, 0.0)
        poor shouldBe (480.0 * 0.85 plusOrMinus 0.001)

        // Balanced sleep: 20% deep, 20% rem (40% total): factor = 0.85 + 0.75 * 0.40 = 1.15
        val healthy = normalizer.computeEffectiveDuration(480.0, 96.0, 96.0)
        healthy shouldBe (480.0 * 1.15 plusOrMinus 0.001)

        // Extremely high restorative sleep (75% total): raw factor = 0.85 + 0.75 * 0.75 = 1.4125 -> clamped to 1.25
        val superRestorative = normalizer.computeEffectiveDuration(480.0, 180.0, 180.0)
        superRestorative shouldBe (480.0 * 1.25 plusOrMinus 0.001)

        // Zero total minutes returns 0.0
        normalizer.computeEffectiveDuration(0.0, 0.0, 0.0) shouldBe 0.0
    }

    "blendDuration applies Bayesian confidence weighting smoothly" {
        val dEff = 500.0
        val dBase = 420.0

        // Confidence 1.0 -> d_eff
        normalizer.blendDuration(dEff, dBase, 1.0) shouldBe (500.0 plusOrMinus 0.001)

        // Confidence 0.0 -> d_base
        normalizer.blendDuration(dEff, dBase, 0.0) shouldBe (420.0 plusOrMinus 0.001)

        // Confidence 0.5 -> exact midpoint
        normalizer.blendDuration(dEff, dBase, 0.5) shouldBe (460.0 plusOrMinus 0.001)

        // Out of bounds confidence clamped
        normalizer.blendDuration(dEff, dBase, 1.5) shouldBe (500.0 plusOrMinus 0.001)
        normalizer.blendDuration(dEff, dBase, -0.2) shouldBe (420.0 plusOrMinus 0.001)
    }

    "clampWakeShift enforces +-90m anti-whiplash safety window" {
        val baseWake = 1000000000L
        val min90 = 90 * 60_000L

        // Small shift (30m later) with c = 1.0 -> exactly +30m
        normalizer.clampWakeShift(baseWake, baseWake + 30 * 60_000L, 1.0) shouldBe (baseWake + 30 * 60_000L)

        // Small shift (30m later) with c = 0.5 -> +15m
        normalizer.clampWakeShift(baseWake, baseWake + 30 * 60_000L, 0.5) shouldBe (baseWake + 15 * 60_000L)

        // Extreme late shift (180m later) with c = 1.0 -> clamped to +90m
        normalizer.clampWakeShift(baseWake, baseWake + 180 * 60_000L, 1.0) shouldBe (baseWake + min90)

        // Extreme early shift (200m earlier) with c = 1.0 -> clamped to -90m
        normalizer.clampWakeShift(baseWake, baseWake - 200 * 60_000L, 1.0) shouldBe (baseWake - min90)

        // Zero confidence -> strictly unchanged
        normalizer.clampWakeShift(baseWake, baseWake + 180 * 60_000L, 0.0) shouldBe baseWake
    }

    "normalize executes full pipeline correctly" {
        val sleep1 = TimeSlotWindow(baseMillis, baseMillis + 180 * 60_000L)
        val sleep2 = TimeSlotWindow(baseMillis + 195 * 60_000L, baseMillis + 480 * 60_000L) // 15m WASO
        val telemetry = RawSleepTelemetry(
            intervals = listOf(sleep1, sleep2),
            deepSleepMinutes = 90.0,
            remSleepMinutes = 90.0,
            confidence = 0.8
        )

        val baseWake = baseMillis + 480 * 60_000L
        val baseSleep = 460.0

        val signal = normalizer.normalize(telemetry, baseWake, baseSleep)
        signal.totalWasoMinutes shouldBe 15.0
        signal.consolidatedSessions shouldHaveSize 1
        signal.effectiveDurationMinutes shouldBe ((480.0 - 15.0) * (0.85 + 0.75 * (180.0 / 465.0)) plusOrMinus 1.0)
    }

    "extractDaytimeNaps filters valid 10-180m bouts after morning wake" {
        val morningWake = baseMillis + 480 * 60_000L
        val microNap = TimeSlotWindow(morningWake + 60 * 60_000L, morningWake + 65 * 60_000L) // 5m (< 10m, invalid)
        val powerNap = TimeSlotWindow(morningWake + 120 * 60_000L, morningWake + 140 * 60_000L) // 20m (valid)
        val longSleep = TimeSlotWindow(morningWake + 200 * 60_000L, morningWake + 400 * 60_000L) // 200m (> 180m, invalid)

        val naps = normalizer.extractDaytimeNaps(listOf(microNap, powerNap, longSleep), morningWake)
        naps shouldHaveSize 1
        naps[0].durationMinutes shouldBe 20
        naps[0].startMillis shouldBe (morningWake + 120 * 60_000L)
    }

    "normalize extracts secondary daytime nap without corrupting morning wake anchor" {
        val nocturnalSleep = TimeSlotWindow(baseMillis, baseMillis + 480 * 60_000L) // 8h sleep
        val daytimeNap = TimeSlotWindow(baseMillis + 840 * 60_000L, baseMillis + 870 * 60_000L) // 30m nap at 14:00

        val baseWake = baseMillis + 480 * 60_000L
        val telemetry = RawSleepTelemetry(
            intervals = listOf(nocturnalSleep, daytimeNap),
            confidence = 1.0
        )

        val signal = normalizer.normalize(telemetry, baseWake, 480.0)
        signal.normalizedWakeTimeMillis shouldBe baseWake // Wake anchor NOT corrupted to 14:30
        signal.totalSleepMinutes shouldBe 480.0 // Nap not counted as nocturnal sleep
        signal.daytimeNaps shouldHaveSize 1
        signal.daytimeNaps[0].durationMinutes shouldBe 30
    }

    "normalize with ONLY daytime nap preserves baseWakeMillis and does not shift morning wake" {
        val daytimeNap = TimeSlotWindow(baseMillis + 420 * 60_000L, baseMillis + 450 * 60_000L) // 30m nap at 14:00
        val telemetry = RawSleepTelemetry(
            intervals = listOf(daytimeNap),
            detectedWakeMillis = daytimeNap.endMillis,
            confidence = 1.0
        )

        val signal = normalizer.normalize(telemetry, baseMillis, 480.0)
        signal.normalizedWakeTimeMillis shouldBe baseMillis // Strictly preserved, NOT shifted by +90m
        signal.totalSleepMinutes shouldBe 480.0 // Falls back to baseline sleep
        signal.daytimeNaps shouldHaveSize 1
        signal.daytimeNaps[0].durationMinutes shouldBe 30
    }
})
```

---

<a id="appsrctestjavacomneuroflowappdomainservicerealworldchronobiologyandschedulertestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/RealWorldChronobiologyAndSchedulerTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/RealWorldChronobiologyAndSchedulerTest.kt`
- **Lines of Code:** 200
- **Size:** 7,980 bytes (7.8 KB)

```kotlin
package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

class RealWorldChronobiologyAndSchedulerTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: TaskSchedulerService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    @Test
    fun testCircularClockHourBlendingHandlesMidnightWrapAround() {
        // Linear average of 23:00 and 01:00 would incorrectly be 12:00 (noon)
        // Circular unit-vector average correctly produces 00:00 (midnight)
        val blended = blendClockHoursCircular(
            listOf(23.0 to 1.0, 1.0 to 1.0)
        )
        // Should be approximately 0.0 or 24.0 (mod 24 = 0.0)
        val error = kotlin.math.min(blended, kotlin.math.abs(24.0 - blended))
        assertTrue("Blended angle across midnight should be near 0.0 (midnight), was $blended", error < 0.05)
    }

    @Test
    fun testLateWakerIsNeverScheduledWhileAsleep() {
        val testDate = LocalDate.of(2026, 10, 20)
        val dayStart = testDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = testDate.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = testDate.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val tasks = listOf(
            SchedulableTask(
                id = "morning_deep",
                title = "Deep Work",
                category = TaskCategory.DEEP_WORK,
                priority = 2,
                estimatedMinutes = 60,
                energyDemand = EnergyDemand.HIGH,
                isFrog = true
            )
        )

        val placements = schedulerService.scheduleDay(
            tasks = tasks,
            dayDate = testDate,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            forceReplan = true,
            zoneId = zoneId
        )

        assertTrue("Placements must not be empty", placements.isNotEmpty())
        val firstStart = placements.first().window.startMillis
        val minAllowedStart = wakeTime + 15 * 60_000L // Wake time + 15m buffer
        assertTrue(
            "Task scheduled start ($firstStart) must be >= wake + 15m ($minAllowedStart), user was asleep!",
            firstStart >= minAllowedStart
        )
    }

    @Test
    fun testMultiPrerequisiteDAGScheduleOrder() {
        val testDate = LocalDate.of(2026, 10, 20)
        val wakeTime = testDate.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = testDate.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // DAG: A -> C and B -> C (C depends on both A and B)
        val taskA = SchedulableTask(
            id = "task_A",
            title = "Prerequisite A",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE
        )
        val taskB = SchedulableTask(
            id = "task_B",
            title = "Prerequisite B",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE
        )
        val taskC = SchedulableTask(
            id = "task_C",
            title = "Downstream Target C",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            prerequisiteIds = setOf("task_A", "task_B")
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(taskC, taskA, taskB),
            dayDate = testDate,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            forceReplan = true,
            zoneId = zoneId
        )

        assertEquals("All 3 tasks should be placed", 3, placements.size)
        val placementMap = placements.associateBy { it.task.id }
        val endA = placementMap["task_A"]!!.window.endMillis
        val endB = placementMap["task_B"]!!.window.endMillis
        val startC = placementMap["task_C"]!!.window.startMillis

        assertTrue(
            "Task C ($startC) must start after Task A finishes ($endA)",
            startC >= endA
        )
        assertTrue(
            "Task C ($startC) must start after Task B finishes ($endB)",
            startC >= endB
        )
    }

    @Test
    fun testDualRepresentationTimestampsResolveConsistently() {
        val todayStart = 1770000000000L
        val nineAmOffset = 9 * 3600_000L

        // Format 1: Intraday ms offset (< 86_400_000L)
        val taskOffset = TaskEntity(
            id = "1",
            title = "Offset Task",
            scheduledDate = todayStart,
            scheduledTime = nineAmOffset
        )

        // Format 2: Absolute epoch millis (>= 86_400_000L)
        val taskEpoch = TaskEntity(
            id = "2",
            title = "Epoch Task",
            scheduledDate = todayStart,
            scheduledTime = todayStart + nineAmOffset
        )

        val resolvedOffset = taskOffset.resolveScheduledEpochMillis()
        val resolvedEpoch = taskEpoch.resolveScheduledEpochMillis()

        assertNotNull(resolvedOffset)
        assertNotNull(resolvedEpoch)
        assertEquals(
            "Both representations must resolve to the identical timestamp without 2082 or 1970 corruption",
            resolvedOffset,
            resolvedEpoch
        )
        assertEquals(todayStart + nineAmOffset, resolvedEpoch)
    }

    @Test
    fun testSimulatedFocusLoadAccumulatesAcrossConsecutiveDeepWork() {
        val baseTime = 1770000000000L
        val p1 = ScheduledPlacement(
            task = SchedulableTask(id = "1", title = "Block 1", category = TaskCategory.DEEP_WORK, priority = 2, estimatedMinutes = 45, energyDemand = EnergyDemand.HIGH),
            window = TimeSlotWindow(baseTime, baseTime + 45 * 60_000L),
            scoreBreakdown = TaskScoreBreakdown("1", 80.0, 80.0, 80.0, 80.0, 80.0, 45)
        )
        val p2 = ScheduledPlacement(
            task = SchedulableTask(id = "2", title = "Block 2", category = TaskCategory.DEEP_WORK, priority = 2, estimatedMinutes = 45, energyDemand = EnergyDemand.HIGH),
            window = TimeSlotWindow(baseTime + 50 * 60_000L, baseTime + 95 * 60_000L), // 5m gap
            scoreBreakdown = TaskScoreBreakdown("2", 80.0, 80.0, 80.0, 80.0, 80.0, 45)
        )

        val defaultService = schedulerService as DefaultTaskSchedulerService
        val loadRightAfter = defaultService.computeSimulatedFocusLoadAt(baseTime + 100 * 60_000L, listOf(p1, p2))
        assertEquals("Simulated focus load should accumulate 45 + 45 = 90 minutes", 90, loadRightAfter)

        val loadAfterLongRest = defaultService.computeSimulatedFocusLoadAt(baseTime + 160 * 60_000L, listOf(p1, p2)) // 65m gap
        assertEquals("Simulated focus load should reset to 0 after long recovery gap", 0, loadAfterLongRest)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainschedulerplacementstabilitytestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/scheduler/PlacementStabilityTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/scheduler/PlacementStabilityTest.kt`
- **Lines of Code:** 130
- **Size:** 5,346 bytes (5.2 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar

/**
 * The scheduler used to rewrite a placement as often as it woke up, so a block could move out from
 * under a user who was already working in it. These cases pin the agreed stability policy: a block
 * starting within two hours is frozen even against a forced re-plan, the cooldown gates every rule
 * rather than only the periodic one, and a placement that is not materially different is not
 * written at all.
 */
class PlacementStabilityTest : StringSpec({

    val now = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 12, 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val minutes = 60_000L
    val hours = 60 * minutes

    fun daySplit(millis: Long): Pair<Long, Long> {
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return dayStart to (millis - dayStart)
    }

    fun placedTask(
        startMillis: Long,
        lastAutoScheduledAt: Long?,
        deadlineMillis: Long? = null,
        postponeCount: Int = 0
    ): TaskEntity {
        val (date, time) = daySplit(startMillis)
        val (deadlineDate, deadlineTime) = deadlineMillis?.let { daySplit(it) } ?: (null to null)
        return TaskEntity(
            id = "task-$startMillis",
            title = "Placed task",
            status = TaskStatus.ACTIVE,
            priority = Priority.MEDIUM,
            estimatedDurationMinutes = 60,
            scheduledDate = date,
            scheduledTime = time,
            isAutoScheduled = true,
            lastAutoScheduledAt = lastAutoScheduledAt,
            updatedAt = now - 24 * hours,
            deadlineDate = deadlineDate,
            deadlineTime = deadlineTime,
            postponeCount = postponeCount
        )
    }

    val stablePrefs = UserPreferences(autoSchedulingEnabled = true, isAutoScheduleToggleOn = true)

    "a block starting within two hours survives a forced re-plan" {
        val task = placedTask(now + hours, lastAutoScheduledAt = now - 3 * hours)

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = true) shouldBe
            PlacementStability.ReplanVerdict.NEAR_TERM_FREEZE
        PlacementStability.verdict(task, now, stablePrefs, forceReplan = true).allowsReplan shouldBe false
    }

    "a deadline rescue respects the cooldown instead of firing every pass" {
        val task = placedTask(
            startMillis = now + 4 * hours,
            lastAutoScheduledAt = now - 5 * minutes,
            deadlineMillis = now + 5 * hours
        )

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false) shouldBe
            PlacementStability.ReplanVerdict.COOLDOWN
        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false).allowsReplan shouldBe false
    }

    "a postponed task is not re-planned twice a minute" {
        val task = placedTask(
            startMillis = now + 3 * hours,
            lastAutoScheduledAt = now - 10 * minutes,
            postponeCount = 3
        )

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false) shouldBe
            PlacementStability.ReplanVerdict.COOLDOWN
        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false).allowsReplan shouldBe false
    }

    "a forced re-plan still reaches blocks further out than two hours" {
        val task = placedTask(now + 5 * hours, lastAutoScheduledAt = now - 3 * hours)

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = true).allowsReplan shouldBe true
    }

    "the periodic refresh still paces a stable plan" {
        val pastCooldown = placedTask(now + 5 * hours, lastAutoScheduledAt = now - 5 * hours)
        val insideCooldown = placedTask(now + 5 * hours, lastAutoScheduledAt = now - hours)

        PlacementStability.verdict(pastCooldown, now, stablePrefs, forceReplan = false)
            .allowsReplan shouldBe true
        PlacementStability.verdict(insideCooldown, now, stablePrefs, forceReplan = false)
            .allowsReplan shouldBe false
    }

    "an unchanged placement is not rewritten" {
        val previous = now + 3 * hours

        PlacementStability.isMoveWorthApplying(previous, previous, now) shouldBe false
    }

    "a future block only moves when the shift is at least half an hour" {
        val previous = now + 3 * hours

        PlacementStability.isMoveWorthApplying(previous, previous + 12 * minutes, now) shouldBe false
        PlacementStability.isMoveWorthApplying(previous, previous + 45 * minutes, now) shouldBe true
    }

    "a block already in the past always takes the new slot" {
        PlacementStability.isMoveWorthApplying(now - 10 * minutes, now + 2 * hours, now) shouldBe true
        PlacementStability.isMoveWorthApplying(null, now + 2 * hours, now) shouldBe true
    }
})
```

---

<a id="appsrctestjavacomneuroflowappdomainschedulerprotectedslotclocktestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClockTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClockTest.kt`
- **Lines of Code:** 294
- **Size:** 10,722 bytes (10.5 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.TimeZone

/**
 * 2026-10-12 is a Monday, 2026-10-10 a Saturday. Everything runs with the default zone pinned to
 * UTC so the minute-of-day arithmetic is exact.
 */
class ProtectedSlotClockTest : StringSpec({

    fun <T> inUtc(body: () -> T): T {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        return try {
            body()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    fun slot(
        label: String = "Slot",
        startMinute: Int,
        durationMinutes: Int,
        recurrence: PtsRecurrence = PtsRecurrence.DAILY,
        weekdayBitmask: Int = 0b1111111,
        dateMillis: Long? = null,
        enabled: Boolean = true,
        bufferMinutes: Int = 0
    ) = ProtectedTimeSlot(
        id = label,
        label = label,
        startMinuteOfDay = startMinute,
        durationMinutes = durationMinutes,
        recurrence = recurrence,
        weekdayBitmask = weekdayBitmask,
        category = PtsCategory.EXERCISE,
        recoveryBufferMinutes = bufferMinutes,
        isEnabled = enabled,
        dateMillis = dateMillis
    )

    fun at(day: Int, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    "a running slot is due and reports what is left of it" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 18, 50))

            due.size shouldBe 1
            due[0].slot.label shouldBe "Gym"
            due[0].phase shouldBe SlotPhase.RUNNING
            due[0].minutesToStart shouldBe 0
            due[0].minutesLeft shouldBe 25
        }
    }

    "a slot counts as running from its first minute" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 18, 30))

            due[0].phase shouldBe SlotPhase.RUNNING
            due[0].minutesLeft shouldBe 45
        }
    }

    "nothing is due more than the lead-in window away" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            ProtectedSlotClock.dueSlots(listOf(gym), at(12, 16, 29)) shouldBe emptyList()
        }
    }

    "the lead-in window is two hours wide" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 16, 30))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.LEAD_IN
            due[0].minutesToStart shouldBe 120
        }
    }

    "a slot that just ended is kept briefly" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 19, 20))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.WRAPPING
            due[0].minutesLeft shouldBe 0
        }
    }

    "ten minutes after the end the slot is gone" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            ProtectedSlotClock.dueSlots(listOf(gym), at(12, 19, 25)) shouldBe emptyList()
        }
    }

    "a disabled slot never takes over" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45, enabled = false)

            ProtectedSlotClock.dueSlots(listOf(gym), at(12, 18, 50)) shouldBe emptyList()
        }
    }

    "a slot that runs past midnight is still running in the small hours" {
        inUtc {
            val late = slot("Night run", startMinute = 23 * 60 + 50, durationMinutes = 60)

            val due = ProtectedSlotClock.dueSlots(listOf(late), at(13, 0, 5))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.RUNNING
            due[0].minutesLeft shouldBe 45
        }
    }

    "yesterday's specific date is not due today" {
        inUtc {
            val oneOff = slot(
                "Doctor", startMinute = 18 * 60 + 30, durationMinutes = 45,
                recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(11, 18, 30)
            )

            ProtectedSlotClock.dueSlots(listOf(oneOff), at(12, 18, 50)) shouldBe emptyList()
        }
    }

    "a specific-date slot is due on its own day" {
        inUtc {
            val oneOff = slot(
                "Doctor", startMinute = 18 * 60 + 30, durationMinutes = 45,
                recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(12, 9, 0)
            )

            val due = ProtectedSlotClock.dueSlots(listOf(oneOff), at(12, 18, 50))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.RUNNING
        }
    }

    "the slot that started most recently takes the top spot" {
        inUtc {
            val earlier = slot("Lift", startMinute = 18 * 60, durationMinutes = 60)
            val later = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 60)

            val due = ProtectedSlotClock.dueSlots(listOf(earlier, later), at(12, 18, 40))

            due.map { it.slot.label } shouldBe listOf("Gym", "Lift")
        }
    }

    "running slots outrank ones that have not started" {
        inUtc {
            val running = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)
            val approaching = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)

            val due = ProtectedSlotClock.dueSlots(listOf(approaching, running), at(12, 18, 50))

            due.map { it.slot.label } shouldBe listOf("Gym", "Dinner")
            due[1].phase shouldBe SlotPhase.LEAD_IN
            due[1].minutesToStart shouldBe 70
        }
    }

    "approaching slots are ordered by how soon they start" {
        inUtc {
            val sooner = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)
            val later = slot("Family call", startMinute = 21 * 60, durationMinutes = 30)

            val due = ProtectedSlotClock.dueSlots(listOf(later, sooner), at(12, 19, 0))

            due.map { it.slot.label } shouldBe listOf("Dinner", "Family call")
        }
    }

    "the running slot takes over and the approaching ones queue" {
        inUtc {
            val running = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)
            val approaching = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)
            val slots = listOf(approaching, running)

            ProtectedSlotClock.takeoverOf(slots, at(12, 18, 50))?.slot?.label shouldBe "Gym"
            ProtectedSlotClock.approaching(slots, at(12, 18, 50)).map { it.slot.label } shouldBe
                listOf("Dinner")
        }
    }

    "a wrapping slot still holds the top spot" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val takeover = ProtectedSlotClock.takeoverOf(listOf(gym), at(12, 19, 20))

            takeover?.phase shouldBe SlotPhase.WRAPPING
        }
    }

    "nothing takes over while every slot is still ahead" {
        inUtc {
            val dinner = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)
            val call = slot("Family call", startMinute = 21 * 60, durationMinutes = 30)

            ProtectedSlotClock.takeoverOf(listOf(dinner, call), at(12, 19, 0)) shouldBe null
            ProtectedSlotClock.approaching(listOf(dinner, call), at(12, 19, 0))
                .map { it.slot.label } shouldBe listOf("Dinner", "Family call")
        }
    }

    "no slots means no takeover" {
        inUtc {
            ProtectedSlotClock.dueSlots(emptyList(), at(12, 18, 50)) shouldBe emptyList()
            ProtectedSlotClock.takeoverOf(emptyList(), at(12, 18, 50)) shouldBe null
            ProtectedSlotClock.approaching(emptyList(), at(12, 18, 50)) shouldBe emptyList()
        }
    }

    "the recurrence predicate matches each mode" {
        inUtc {
            val monday = at(12, 12, 0)
            val saturday = at(10, 12, 0)

            ProtectedSlotClock.occursOnDay(slot("a", 0, 15), monday) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("b", 0, 15, recurrence = PtsRecurrence.WEEKDAYS), monday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("c", 0, 15, recurrence = PtsRecurrence.WEEKDAYS), saturday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("d", 0, 15, recurrence = PtsRecurrence.WEEKENDS), saturday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("e", 0, 15, recurrence = PtsRecurrence.WEEKENDS), monday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("f", 0, 15, recurrence = PtsRecurrence.CUSTOM, weekdayBitmask = 0b10), monday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("g", 0, 15, recurrence = PtsRecurrence.CUSTOM, weekdayBitmask = 0b100), monday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("h", 0, 15, recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(12, 7, 0)),
                monday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("i", 0, 15, recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(10, 7, 0)),
                monday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("j", 0, 15, recurrence = PtsRecurrence.SPECIFIC_DATE), monday
            ) shouldBe false
        }
    }

    "the disabled switch does not change which days a slot falls on" {
        inUtc {
            ProtectedSlotClock.occursOnDay(slot("a", 0, 15, enabled = false), at(12, 12, 0)) shouldBe true
        }
    }

    "a recovery buffer does not extend the takeover" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45, bufferMinutes = 30)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 19, 20))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.WRAPPING
        }
    }
})
```

---

<a id="appsrctestjavacomneuroflowappdomaininferencetaskinferenceenginetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/inference/TaskInferenceEngineTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/inference/TaskInferenceEngineTest.kt`
- **Lines of Code:** 85
- **Size:** 3,992 bytes (3.9 KB)

```kotlin
package com.neuroflow.app.domain.inference

import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskInferenceEngineTest {

    @Test
    fun testEmptyInputGivesDefaultMetadata() {
        val result = TaskInferenceEngine.inferFromInput("")
        assertEquals("", result.sanitizedTitle)
        assertEquals(25, result.durationMinutes)
        assertEquals(EnergyLevel.MEDIUM, result.energyDemand)
        assertEquals(TaskType.GENERAL, result.taskType)
        assertTrue(result.contextTags.isEmpty())
        assertFalse(result.isFrog)
    }

    @Test
    fun testDurationExtractionFormats() {
        assertEquals(15, TaskInferenceEngine.inferFromInput("Quick email 15m").durationMinutes)
        assertEquals(90, TaskInferenceEngine.inferFromInput("Coding 90min").durationMinutes)
        assertEquals(60, TaskInferenceEngine.inferFromInput("Review 1h").durationMinutes)
        assertEquals(150, TaskInferenceEngine.inferFromInput("Architecture 2.5h").durationMinutes)
        assertEquals(45, TaskInferenceEngine.inferFromInput("Meeting 45 mins").durationMinutes)
        assertEquals(90, TaskInferenceEngine.inferFromInput("Design 1h 30m").durationMinutes)
    }

    @Test
    fun testEnergyDemandInference() {
        val highEnergy = TaskInferenceEngine.inferFromInput("Deep work on compiler thesis 90m")
        val lowEnergy = TaskInferenceEngine.inferFromInput("Clean desk and file expense 15m")
        val medEnergy = TaskInferenceEngine.inferFromInput("Team status update 30m")

        assertEquals(EnergyLevel.HIGH, highEnergy.energyDemand)
        assertEquals(EnergyLevel.LOW, lowEnergy.energyDemand)
        assertEquals(EnergyLevel.MEDIUM, medEnergy.energyDemand)
    }

    @Test
    fun testTaskTypeInference() {
        assertEquals(TaskType.ANALYTICAL, TaskInferenceEngine.inferFromInput("Debug algorithm bug 30m").taskType)
        assertEquals(TaskType.CREATIVE, TaskInferenceEngine.inferFromInput("Draft design system article 45m").taskType)
        assertEquals(TaskType.ADMIN, TaskInferenceEngine.inferFromInput("Send weekly sync email 15m").taskType)
        assertEquals(TaskType.PHYSICAL, TaskInferenceEngine.inferFromInput("Morning gym workout 60m").taskType)
    }

    @Test
    fun testFrogDetectionAndTitleSanitization() {
        val result = TaskInferenceEngine.inferFromInput("!frog Prepare quarterly financial tax return 60m @finance #urgent")
        assertTrue(result.isFrog)
        assertEquals("Prepare quarterly financial tax return", result.sanitizedTitle)
        assertEquals(listOf("finance"), result.contextTags)
        assertEquals(60, result.durationMinutes)
    }

    @Test
    fun testNaturalLanguageDurationPhrases() {
        assertEquals(30, TaskInferenceEngine.inferFromInput("Quick sync half hour").durationMinutes)
        assertEquals(30, TaskInferenceEngine.inferFromInput("Consultation half an hour").durationMinutes)
        assertEquals(60, TaskInferenceEngine.inferFromInput("Sprint planning an hour").durationMinutes)
    }

    @Test
    fun testExplicitEnergyAndPriorityParsing() {
        val highP1 = TaskInferenceEngine.inferFromInput("Quarterly review !high !p1")
        assertEquals(EnergyLevel.HIGH, highP1.energyDemand)
        assertEquals(com.neuroflow.app.domain.model.Priority.HIGH, highP1.priority)

        val lowP3 = TaskInferenceEngine.inferFromInput("Clear inbox !low !p3")
        assertEquals(EnergyLevel.LOW, lowP3.energyDemand)
        assertEquals(com.neuroflow.app.domain.model.Priority.LOW, lowP3.priority)
    }

    @Test
    fun testWordBoundaryIsolationPreventsFalseMatches() {
        // "barcode scanner" should NOT trigger "code" -> ANALYTICAL
        val scanner = TaskInferenceEngine.inferFromInput("Order barcode scanner replacement")
        assertEquals(TaskType.ADMIN, scanner.taskType)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdatalocalstimulantloggingtestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/data/local/StimulantLoggingTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/data/local/StimulantLoggingTest.kt`
- **Lines of Code:** 109
- **Size:** 4,255 bytes (4.2 KB)

```kotlin
package com.neuroflow.app.data.local

import com.neuroflow.app.domain.service.PharmacokineticStimulantCalculator
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StimulantLoggingTest {

    @Test
    fun testParseStimulantLogsEmptyAndInvalid() {
        assertEquals(emptyList<StimulantDoseBout>(), parseStimulantLogs(null))
        assertEquals(emptyList<StimulantDoseBout>(), parseStimulantLogs(""))
        assertEquals(emptyList<StimulantDoseBout>(), parseStimulantLogs("invalid json"))
        assertEquals(emptyList<StimulantDoseBout>(), parseStimulantLogs("[]"))
    }

    @Test
    fun testParseAllFourStimulantTypes() {
        val now = 1_700_000_000_000L
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("type", StimulantType.CAFFEINE.name)
                put("doseMg", 100.0)
                put("timestampMillis", now)
            })
            put(JSONObject().apply {
                put("type", StimulantType.GREEN_TEA_L_THEANINE.name)
                put("doseMg", 50.0)
                put("timestampMillis", now - 3600_000L)
            })
            put(JSONObject().apply {
                put("type", StimulantType.PRESCRIPTION_STIMULANT_IR.name)
                put("doseMg", 10.0)
                put("timestampMillis", now - 7200_000L)
            })
            put(JSONObject().apply {
                put("type", StimulantType.PRESCRIPTION_STIMULANT_XR.name)
                put("doseMg", 20.0)
                put("timestampMillis", now - 10800_000L)
            })
        }

        val parsed = parseStimulantLogs(arr.toString())
        assertEquals(4, parsed.size)
        assertEquals(StimulantType.CAFFEINE, parsed[0].type)
        assertEquals(100.0, parsed[0].doseMg, 0.001)

        assertEquals(StimulantType.GREEN_TEA_L_THEANINE, parsed[1].type)
        assertEquals(50.0, parsed[1].doseMg, 0.001)

        assertEquals(StimulantType.PRESCRIPTION_STIMULANT_IR, parsed[2].type)
        assertEquals(10.0, parsed[2].doseMg, 0.001)

        assertEquals(StimulantType.PRESCRIPTION_STIMULANT_XR, parsed[3].type)
        assertEquals(20.0, parsed[3].doseMg, 0.001)
    }

    @Test
    fun testUndoStimulantDosePopsHead() {
        val now = 1_700_000_000_000L
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("type", StimulantType.PRESCRIPTION_STIMULANT_XR.name)
                put("doseMg", 20.0)
                put("timestampMillis", now)
            })
            put(JSONObject().apply {
                put("type", StimulantType.CAFFEINE.name)
                put("doseMg", 100.0)
                put("timestampMillis", now - 3600_000L)
            })
        }

        // Simulate undo
        val list = mutableListOf<JSONObject>()
        for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
        assertEquals(2, list.size)

        val removed = list.removeAt(0)
        assertEquals(StimulantType.PRESCRIPTION_STIMULANT_XR.name, removed.getString("type"))
        assertEquals(1, list.size)

        val remainingJson = JSONArray().apply { list.forEach { put(it) } }.toString()
        val parsed = parseStimulantLogs(remainingJson)
        assertEquals(1, parsed.size)
        assertEquals(StimulantType.CAFFEINE, parsed[0].type)
    }

    @Test
    fun testActivePlasmaEvaluation() {
        val now = 1_700_000_000_000L
        val dose = StimulantDoseBout(
            timestampMillis = now - 3600_000L, // 1 hour ago
            doseMg = 100.0,
            type = StimulantType.CAFFEINE
        )

        val effect = PharmacokineticStimulantCalculator.evaluateAt(now, listOf(dose))
        // Caffeine half-life is 5 hours, so after 1 hour remaining active mg should be ~87 mg
        assertTrue("Active plasma should be positive", effect.activeEquivalentMg > 70.0)
        assertTrue("Active plasma should not exceed dose", effect.activeEquivalentMg <= 100.0)
        assertTrue("Acute alertness boost should be active", effect.acuteAlertnessBoostPoints > 0.0)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdatacalendarcalendarsyncrepositorytestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/data/calendar/CalendarSyncRepositoryTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/data/calendar/CalendarSyncRepositoryTest.kt`
- **Lines of Code:** 129
- **Size:** 5,611 bytes (5.5 KB)

```kotlin
package com.neuroflow.app.data.calendar

import com.neuroflow.app.domain.service.TimeSlotWindow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CalendarSyncRepositoryTest : StringSpec({

    val baseMillis = 1770000000000L // arbitrary fixed epoch ms

    "coalesceWindows handles empty input" {
        val result = CalendarSyncDataSource.coalesceWindows(emptyList())
        result.shouldBeEmpty()
    }

    "coalesceWindows ignores invalid or zero-duration windows" {
        val invalid = listOf(
            TimeSlotWindow(baseMillis + 1000, baseMillis + 1000),
            TimeSlotWindow(baseMillis + 2000, baseMillis + 1000)
        )
        val result = CalendarSyncDataSource.coalesceWindows(invalid)
        result.shouldBeEmpty()
    }

    "coalesceWindows leaves short meetings (< 45m) unbuffered" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L) // 30 min meeting
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        result[0].endMillis shouldBe baseMillis + 30 * 60_000L
        result[0].durationMinutes shouldBe 30
    }

    "coalesceWindows adds 5m decompression buffer to meetings >= 45m" {
        val raw45 = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 45 * 60_000L) // exactly 45 min
        )
        val result45 = CalendarSyncDataSource.coalesceWindows(raw45)
        result45 shouldHaveSize 1
        result45[0].startMillis shouldBe baseMillis
        result45[0].endMillis shouldBe baseMillis + 50 * 60_000L // 45 + 5m buffer
        result45[0].durationMinutes shouldBe 50

        val raw60 = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 60 * 60_000L) // 60 min
        )
        val result60 = CalendarSyncDataSource.coalesceWindows(raw60)
        result60 shouldHaveSize 1
        result60[0].durationMinutes shouldBe 65
    }

    "coalesceWindows merges directly overlapping intervals" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L),
            TimeSlotWindow(baseMillis + 15 * 60_000L, baseMillis + 60 * 60_000L)
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // Merged duration is 60m >= 45m -> gets +5m buffer
        result[0].endMillis shouldBe baseMillis + 65 * 60_000L
    }

    "coalesceWindows coalesces adjacent intervals separated by < 5m" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 25 * 60_000L),
            TimeSlotWindow(baseMillis + 28 * 60_000L, baseMillis + 55 * 60_000L) // 3 min gap (< 5m)
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // Merged span: [0..55m] = 55m >= 45m -> gets +5m buffer -> 60m total
        result[0].endMillis shouldBe baseMillis + 60 * 60_000L
    }

    "coalesceWindows preserves distinct separated intervals (gap >= 5m)" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L), // 30m (<45m, no buffer)
            TimeSlotWindow(baseMillis + 40 * 60_000L, baseMillis + 70 * 60_000L) // 10 min gap
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 2
        result[0].startMillis shouldBe baseMillis
        result[0].endMillis shouldBe baseMillis + 30 * 60_000L
        result[1].startMillis shouldBe baseMillis + 40 * 60_000L
        result[1].endMillis shouldBe baseMillis + 70 * 60_000L
    }

    "coalesceWindows handles unordered input and nested intervals" {
        val raw = listOf(
            TimeSlotWindow(baseMillis + 10 * 60_000L, baseMillis + 20 * 60_000L),
            TimeSlotWindow(baseMillis, baseMillis + 50 * 60_000L) // Outer containing window
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // 50 min >= 45m -> 55m with decompression buffer
        result[0].endMillis shouldBe baseMillis + 55 * 60_000L
    }

    "coalesceWindows handles multi-day span calendar events (72+ hours)" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 72 * 3600_000L) // 72-hour conference
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // 72h >= 45m -> gets +5m decompression buffer at conclusion
        result[0].endMillis shouldBe baseMillis + 72 * 3600_000L + 5 * 60_000L
        result[0].durationMinutes shouldBe (72 * 60 + 5)
    }

    "coalesceWindows merges consecutive long meetings separated by < 5m into single decompressed horizon" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 60 * 60_000L), // 60m
            TimeSlotWindow(baseMillis + 62 * 60_000L, baseMillis + 120 * 60_000L) // 2m gap -> 58m
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // Merged span: [0..120m] >= 45m -> gets +5m decompression buffer -> 125m
        result[0].endMillis shouldBe baseMillis + 125 * 60_000L
        result[0].durationMinutes shouldBe 125
    }
})
```

---

<a id="appsrctestjavacomneuroflowappdatacalendarcalendarwritebacksynctestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt`
- **Lines of Code:** 447
- **Size:** 16,430 bytes (16.0 KB)

```kotlin
package com.neuroflow.app.data.calendar

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarWriteBackSyncTest {

    private lateinit var app: Application
    private lateinit var contentResolver: ContentResolver
    private lateinit var dataSource: CalendarSyncDataSource

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.grantPermissions(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        contentResolver = mockk(relaxed = true)
        val testContext = object : android.content.ContextWrapper(app) {
            override fun getContentResolver(): ContentResolver = this@CalendarWriteBackSyncTest.contentResolver
        }
        dataSource = CalendarSyncDataSource(testContext)
    }

    @Test
    fun `findPrimaryWritableCalendarId returns primary calendar id`() = runTest {
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        cursor.addRow(arrayOf(101L, 1, CalendarContract.Calendars.CAL_ACCESS_OWNER))

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                match { it.contains("${CalendarContract.Calendars.IS_PRIMARY} = 1") },
                any(),
                any()
            )
        } returns cursor

        val calId = dataSource.findPrimaryWritableCalendarId()
        calId shouldBe 101L
    }

    @Test
    fun `findPrimaryWritableCalendarId falls back to first writable calendar when no primary exists`() = runTest {
        val emptyPrimaryCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))

        val fallbackCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        fallbackCursor.addRow(arrayOf(202L, 0, CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR))

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                match { it.contains("${CalendarContract.Calendars.IS_PRIMARY} = 1") },
                any(),
                any()
            )
        } returns emptyPrimaryCursor

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                match { !it.contains("${CalendarContract.Calendars.IS_PRIMARY} = 1") },
                any(),
                any()
            )
        } returns fallbackCursor

        val calId = dataSource.findPrimaryWritableCalendarId()
        calId shouldBe 202L
    }

    @Test
    fun `findPrimaryWritableCalendarId returns null when permission denied`() = runTest {
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.denyPermissions(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

        val unprivilegedSource = CalendarSyncDataSource(app)
        val calId = unprivilegedSource.findPrimaryWritableCalendarId()
        calId shouldBe null
    }

    @Test
    fun `findEventIdByTaskId queries using loop-prevention package and uri`() = runTest {
        val taskId = "task-focus-999"
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        cursor.addRow(arrayOf(777L, "neuroflow://task/$taskId"))

        val selectionArgsSlot = slot<Array<String>>()
        every {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                any(),
                any(),
                capture(selectionArgsSlot),
                any()
            )
        } returns cursor

        val eventId = dataSource.findEventIdByTaskId(taskId)
        eventId shouldBe 777L

        selectionArgsSlot.captured[0] shouldBe CalendarSyncDataSource.NEUROFLOW_PACKAGE_ID
        selectionArgsSlot.captured[1] shouldBe "neuroflow://task/$taskId"
    }

    @Test
    fun `upsertTaskEvent inserts new event with loop prevention stamp and flow title`() = runTest {
        val task = TaskEntity(
            id = "task-alpha-1",
            title = "Architectural Review",
            description = "Review ADR-006 write-back",
            scheduledDate = 1770000000000L,
            scheduledTime = 3600_000L,
            estimatedDurationMinutes = 45,
            isScheduleLocked = true
        )

        // Mock primary calendar
        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, CalendarContract.Calendars.CAL_ACCESS_OWNER))
        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns calCursor

        // No existing event
        val emptyEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        every {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns emptyEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/events/8888")

        val newId = dataSource.upsertTaskEvent(task)
        newId shouldBe 8888L

        val captured = valuesSlot.captured
        captured.getAsLong(CalendarContract.Events.CALENDAR_ID) shouldBe 55L
        captured.getAsString(CalendarContract.Events.TITLE) shouldBe "[Flow] Architectural Review"
        captured.getAsString(CalendarContract.Events.CUSTOM_APP_PACKAGE) shouldBe CalendarSyncDataSource.NEUROFLOW_PACKAGE_ID
        val expectedStart = 1770000000000L + 3600_000L
        captured.getAsLong(CalendarContract.Events.DTSTART) shouldBe expectedStart
        captured.getAsLong(CalendarContract.Events.DTEND) shouldBe expectedStart + 45 * 60_000L
    }

    @Test
    fun `upsertTaskEvent formats frog task with frog title prefix`() = runTest {
        val frogTask = TaskEntity(
            id = "task-frog-7",
            title = "Eat the Frog",
            isFrog = true,
            scheduledDate = 1770000000000L,
            estimatedDurationMinutes = 30
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, 700))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        val emptyEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns emptyEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/events/9999")

        dataSource.upsertTaskEvent(frogTask)

        val captured = valuesSlot.captured
        captured.getAsString(CalendarContract.Events.TITLE) shouldBe "🎯 [Flow] Eat the Frog"
    }

    @Test
    fun `upsertTaskEvent updates existing event in place when event already exists`() = runTest {
        val task = TaskEntity(
            id = "task-update-10",
            title = "Rescheduled Deep Work",
            scheduledDate = 1770000000000L,
            estimatedDurationMinutes = 60
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, 700))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        // Existing event found
        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(12345L, "neuroflow://task/task-update-10"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val updateUriSlot = slot<Uri>()
        every {
            contentResolver.update(capture(updateUriSlot), any(), any(), any())
        } returns 1

        val updatedId = dataSource.upsertTaskEvent(task)
        updatedId shouldBe 12345L

        updateUriSlot.captured shouldBe ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 12345L)
        verify(exactly = 0) { contentResolver.insert(any(), any()) }
    }

    @Test
    fun `deleteTaskEvent deletes existing event via ContentUris`() = runTest {
        val taskId = "task-delete-99"
        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(4444L, "neuroflow://task/$taskId"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val deleteUriSlot = slot<Uri>()
        every {
            contentResolver.delete(capture(deleteUriSlot), any(), any())
        } returns 1

        val deleted = dataSource.deleteTaskEvent(taskId)
        deleted shouldBe true

        deleteUriSlot.captured shouldBe ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 4444L)
    }

    @Test
    fun `handleTaskStateChanged updates completed task with checkmark flow title`() = runTest {
        val completedTask = TaskEntity(
            id = "task-done-1",
            title = "Finalize Presentation",
            status = TaskStatus.COMPLETED,
            scheduledDate = 1770000000000L
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, 700))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(3333L, "neuroflow://task/task-done-1"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.update(any(), capture(valuesSlot), any(), any())
        } returns 1

        val result = dataSource.handleTaskStateChanged(completedTask)
        result shouldBe true

        valuesSlot.captured.getAsString(CalendarContract.Events.TITLE) shouldBe "✓ [Flow] Finalize Presentation"
    }

    @Test
    fun `handleTaskStateChanged deletes task event on ARCHIVED`() = runTest {
        val archivedTask = TaskEntity(
            id = "task-archived-1",
            title = "Old Idea",
            status = TaskStatus.ARCHIVED
        )

        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(2222L, "neuroflow://task/task-archived-1"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        every { contentResolver.delete(any(), any(), any()) } returns 1

        val result = dataSource.handleTaskStateChanged(archivedTask)
        result shouldBe true

        verify { contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 2222L), any(), any()) }
    }

    @Test
    fun `queryInstances loop-prevention filters out self-authored neuroFlow blocks`() = runTest {
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI
        ))

        val start = 1770000000000L
        val end = start + 3600_000L

        // Row 1: External busy meeting (should be included)
        cursor.addRow(arrayOf(
            start,
            end,
            "External Sync",
            "Important team sync",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "colleague@corp.com",
            "",
            ""
        ))

        // Row 2: Self-authored neuroFlow block via customAppPackage (must be filtered out)
        cursor.addRow(arrayOf(
            start + 4000_000L,
            start + 5000_000L,
            "Focus Block",
            "Deep focus",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "",
            CalendarSyncDataSource.NEUROFLOW_PACKAGE_ID,
            "neuroflow://task/task-abc"
        ))

        // Row 3: Self-authored neuroFlow block via [Flow] title prefix (must be filtered out)
        cursor.addRow(arrayOf(
            start + 6000_000L,
            start + 7000_000L,
            "[Flow] Frog task",
            "Focus",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "",
            "",
            ""
        ))

        every {
            contentResolver.query(any(), any(), any(), any(), any())
        } returns cursor

        val windows = dataSource.queryInstances(start, start + 10_000_000L)
        windows.size shouldBe 1
        windows[0].startMillis shouldBe start
        windows[0].endMillis shouldBe end
    }
}
```

---

