package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
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

    @Test
    fun testDependentTaskDeferredWhenPrerequisiteFailsPlacement() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Block entire day except a 10m window (10:00-10:10)
        val busy1 = TimeSlotWindow(
            today.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        )
        val busy2 = TimeSlotWindow(
            today.atTime(LocalTime.of(10, 10)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()
        )

        // Prerequisite A requires 60m (won't fit in 10m)
        val taskA = SchedulableTask(
            id = "task_A",
            title = "Prerequisite A",
            category = TaskCategory.GENERAL,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.MODERATE
        )
        // Dependent B requires 10m and depends on A
        val taskB = SchedulableTask(
            id = "task_B",
            title = "Dependent B",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 10,
            energyDemand = EnergyDemand.LOW,
            prerequisiteIds = setOf("task_A")
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(taskA, taskB),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(busy1, busy2),
            zoneId = zoneId
        )

        assertEquals("Neither task should be placed", 0, outcome.placements.size)
        val deferralMap = outcome.deferrals.associateBy { it.taskId }
        assertTrue("Task A must be in deferrals", deferralMap.containsKey("task_A"))
        assertEquals(com.neuroflow.app.domain.scheduler.DeferralReasonCode.INSUFFICIENT_TIME, deferralMap["task_A"]?.reasonCode)

        assertTrue("Dependent Task B must NOT be silently dropped; must be in deferrals", deferralMap.containsKey("task_B"))
        assertEquals(com.neuroflow.app.domain.scheduler.DeferralReasonCode.DEPENDENCY_BLOCKED, deferralMap["task_B"]?.reasonCode)
    }

    @Test
    fun testFutureDaySchedulerAlignsWakeTimeAndAvoidsSleepDeprivation() {
        val today = LocalDate.of(2026, 10, 18)
        val tomorrow = today.plusDays(1)
        val todayNow = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val todayWake = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // SchedulableTask on tomorrow: deep work frog task
        val task = SchedulableTask(
            id = "future_deep",
            title = "Future Deep Work",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(task),
            dayDate = tomorrow,
            nowMillis = todayNow,
            wakeTimeMillis = todayWake, // Raw today's wake time passed in
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        assertEquals("Task should be placed on tomorrow", 1, outcome.placements.size)
        val placement = outcome.placements[0]
        val placementMidpoint = placement.window.startMillis + 30 * 60_000L
        val placementMidZdt = java.time.Instant.ofEpochMilli(placementMidpoint).atZone(zoneId)
        assertEquals(tomorrow, placementMidZdt.toLocalDate())

        // Ensure the task was scheduled during reasonable hours
        assertTrue("Placement hour should be in reasonable work hours", placementMidZdt.hour in 7..21)
    }

    @Test
    fun testUltradianSplitGuardsAgainstRecursiveSplittingOfParts() {
        val defaultScheduler = schedulerService as DefaultTaskSchedulerService

        val normalTask = SchedulableTask(
            id = "normal_80",
            title = "Big Task",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 80,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.5f
        )
        assertNotNull("Normal 80m task can be split", defaultScheduler.splitIntoUltradianParts(normalTask, 80))

        val part1Task = normalTask.copy(id = "normal_80#part1", title = "Big Task (Part 1/2)")
        val part2Task = normalTask.copy(id = "normal_80#part2", title = "Big Task (Part 2/2)")
        val part2UnderScore = normalTask.copy(id = "normal_80_part2", title = "Big Task (Part 2/2)")
        val subtask = normalTask.copy(parentTaskId = "parent_task")

        assertEquals("Cannot split #part1", null, defaultScheduler.splitIntoUltradianParts(part1Task, 80))
        assertEquals("Cannot split #part2", null, defaultScheduler.splitIntoUltradianParts(part2Task, 80))
        assertEquals("Cannot split _part2", null, defaultScheduler.splitIntoUltradianParts(part2UnderScore, 80))
        assertEquals("Cannot split subtask with parentTaskId", null, defaultScheduler.splitIntoUltradianParts(subtask, 80))
    }

    @Test
    fun testReverseDagCeilingAllowsLockedUnplacedTaskToSchedule() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val slot11am = today.atTime(LocalTime.of(11, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Prerequisite unplaced in Step 3
        val taskA = SchedulableTask(
            id = "prereq_A",
            title = "Prerequisite Step",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW
        )

        // Task B is locked at 11:00 AM and depends on task A
        val taskB = SchedulableTask(
            id = "locked_B",
            title = "Locked Dependent",
            category = TaskCategory.GENERAL,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW,
            isLocked = true,
            scheduledStartMillis = slot11am,
            prerequisiteIds = setOf("prereq_A")
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(taskB, taskA),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        assertEquals("Both tasks must be placed", 2, outcome.placements.size)
        val placementB = outcome.placements.first { it.task.id == "locked_B" }
        assertEquals(slot11am, placementB.window.startMillis)
    }

    @Test
    fun testCustomWorkDayBoundsEnforcedStrictly() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val task1 = SchedulableTask(
            id = "t1",
            title = "Morning Task",
            category = TaskCategory.GENERAL,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH
        )
        val task2 = SchedulableTask(
            id = "t2",
            title = "Afternoon Task",
            category = TaskCategory.GENERAL,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.MODERATE
        )

        // Custom work day: 9:00 AM (hour 9) to 5:00 PM (hour 17)
        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(task1, task2),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 17
        )

        val dayStartEpoch = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val dayEndEpoch = today.atTime(LocalTime.of(17, 0)).atZone(zoneId).toInstant().toEpochMilli()

        assertTrue("Placements must not be empty", outcome.placements.isNotEmpty())
        for (placement in outcome.placements) {
            assertTrue("Task start (${placement.window.startMillis}) must be >= workDayStart ($dayStartEpoch)", placement.window.startMillis >= dayStartEpoch)
            assertTrue("Task end (${placement.window.endMillis}) must be <= workDayEnd ($dayEndEpoch)", placement.window.endMillis <= dayEndEpoch)
        }
    }

    @Test
    fun testLockedTaskFallbackNeverDisplacesToArbitraryCircadianWindow() {
        // BUG-010: Locked task with calendar conflict must NEVER be displaced to arbitrary available circadian slot
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val pinnedTime = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val lockedTask = SchedulableTask(
            id = "locked_1",
            title = "Pinned Client Presentation",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isLocked = true,
            scheduledStartMillis = pinnedTime
        )

        // Add a busy window exactly colliding with the pinned 10:00-11:00 AM slot
        val busyWindow = TimeSlotWindow(pinnedTime, pinnedTime + 60 * 60_000L)

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(lockedTask),
            dayDate = today,
            nowMillis = wakeTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(busyWindow),
            zoneId = zoneId
        )

        // Pinned task must NOT be placed into an arbitrary circadian window in the afternoon
        assertTrue("Locked task must not be placed into another arbitrary slot", outcome.placements.none { it.task.id == "locked_1" })
        assertTrue("Locked task must be deferred when pinned slot is unavailable", outcome.deferrals.any { it.taskId == "locked_1" })
    }

    @Test
    fun testSweepRationaleDoesNotAccuseCompletedPrerequisites() {
        // BUG-015: Completed prerequisites must not be cited as missing blockers in sweep
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Prereq A is COMPLETED (not in activeUncompletedTaskIds)
        // Prereq B is UNCOMPLETED and schedulable today, but impossible to place due to busy windows
        // Dependent task C depends on [A, B]
        val prereqB = SchedulableTask(
            id = "prereq_B",
            title = "Prerequisite B",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 120,
            energyDemand = EnergyDemand.HIGH
        )
        val dependentC = SchedulableTask(
            id = "dep_C",
            title = "Dependent C",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW,
            prerequisiteIds = setOf("prereq_A", "prereq_B")
        )

        // Entire day is blocked so prereq_B cannot be placed
        val fullDayBusy = listOf(
            TimeSlotWindow(
                today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli(),
                today.atTime(LocalTime.of(22, 0)).atZone(zoneId).toInstant().toEpochMilli()
            )
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(prereqB, dependentC),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = fullDayBusy,
            zoneId = zoneId,
            activeUncompletedTaskIds = setOf("prereq_B", "dep_C") // prereq_A is NOT active/uncompleted (already finished)
        )

        val depDeferral = outcome.deferrals.firstOrNull { it.taskId == "dep_C" }
        assertNotNull("Dependent C must have a deferral", depDeferral)
        // Must accuse prereq_B, NOT already-completed prereq_A
        assertTrue(
            "Deferral should cite unsatisfied prereq_B, but got: ${depDeferral!!.explanation}",
            depDeferral.explanation.contains("prereq_B")
        )
        assertFalse(
            "Deferral must NOT cite completed prereq_A",
            depDeferral.explanation.contains("prereq_A")
        )
    }

    @Test
    fun testWorkDayEndHourZeroResolvesToEndOfDay() {
        // BUG-026: workDayEndHour == 0 must resolve to end of day, not beginning of day (00:00)
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val task = SchedulableTask(
            id = "evening_task",
            title = "Evening Study",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.MODERATE
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(task),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            workDayStartHour = 8,
            workDayEndHour = 0 // midnight at close of day
        )

        assertEquals("Task should be placed when workDayEndHour is 0 (midnight close)", 1, outcome.placements.size)
        val placement = outcome.placements[0]
        val startZdt = Instant.ofEpochMilli(placement.window.startMillis).atZone(zoneId)
        assertEquals(today, startZdt.toLocalDate())
        assertTrue("Placement start should be >= 8:00 AM", startZdt.hour >= 8)
    }
}
