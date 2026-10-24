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
