package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
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
            parentTaskId = "task_a"
        )
        val taskC = SchedulableTask(
            id = "task_c",
            title = "Step C: Integration Tests",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "task_b"
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
}
