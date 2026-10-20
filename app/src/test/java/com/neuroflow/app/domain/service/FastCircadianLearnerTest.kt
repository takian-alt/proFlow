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
