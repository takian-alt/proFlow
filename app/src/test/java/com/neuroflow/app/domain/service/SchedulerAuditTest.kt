package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.AutoSchedulingEngineCoordinator
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.DeferralReasonCode
import com.neuroflow.app.domain.scheduler.DependencyGraphValidator
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SchedulerAuditTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: TaskSchedulerService
    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    @Test
    fun testCrossDayPrerequisitesPreservedWhenMultipleTasksScheduled() {
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Task B depends on external Task A (which is scheduled for tomorrow / in activeUncompletedTaskIds)
        val taskB = SchedulableTask(
            id = "task_b",
            title = "Dependent Task B",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW,
            prerequisiteIds = setOf("task_a_external")
        )

        // Task C is an independent task scheduled for today
        val taskC = SchedulableTask(
            id = "task_c",
            title = "Independent Task C",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW
        )

        // 1. Verify DependencyGraphValidator preserves task_a_external even when multiple tasks are passed
        val sanitized = DependencyGraphValidator.sanitizeDag(listOf(taskB, taskC))
        val sanitizedB = sanitized.tasks.first { it.id == "task_b" }
        assertTrue(
            "External prerequisite 'task_a_external' must be preserved across multi-task sanitizeDag",
            sanitizedB.prerequisiteIds.contains("task_a_external")
        )

        // 2. Verify TaskSchedulerService blocks task_b when task_a_external is in activeUncompletedTaskIds
        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(taskB, taskC),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            activeUncompletedTaskIds = setOf("task_a_external")
        )

        assertTrue("Task C should be placed", outcome.placements.any { it.task.id == "task_c" })
        assertFalse("Task B must NOT be placed while prerequisite is active on future date", outcome.placements.any { it.task.id == "task_b" })
        val deferralB = outcome.deferrals.firstOrNull { it.taskId == "task_b" }
        assertEquals(DeferralReasonCode.DEPENDENCY_BLOCKED, deferralB?.reasonCode)
    }

    @Test
    fun testOverdueTaskScheduledTodayWithDeadlinePenalty() {
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Overdue task whose deadline expired yesterday
        val overdueDeadline = today.minusDays(1).atTime(LocalTime.of(17, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val overdueTask = SchedulableTask(
            id = "overdue_1",
            title = "Critical Tax Filing",
            category = TaskCategory.ADMIN,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.MODERATE,
            deadlineMillis = overdueDeadline,
            createdAtMillis = nowMillis - 5 * 86_400_000L
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(overdueTask),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        assertEquals("Overdue task must be scheduled today rather than permanently dropped", 1, outcome.placements.size)
        val placement = outcome.placements.first()
        assertEquals("overdue_1", placement.task.id)
        assertTrue("Overdue task must be placed after current time", placement.window.startMillis >= nowMillis)
    }

    @Test
    fun testAutoSchedulingSlotBudgetOverflowPrevented() {
        val tasks = listOf(
            TaskEntity(id = "t1", title = "Task 1", estimatedDurationMinutes = 20),
            TaskEntity(id = "t2", title = "Task 2", estimatedDurationMinutes = 20),
            TaskEntity(id = "t3", title = "Task 3", estimatedDurationMinutes = 20),
            TaskEntity(id = "t4", title = "Task 4", estimatedDurationMinutes = 20)
        )

        // Only 50 minutes of available slots, but 200 minutes of cognitive capacity
        val report = AutoSchedulingEngineCoordinator.planSchedule(
            tasks = tasks,
            availableSlotsMinutes = 50,
            dailyCognitiveCapacityMinutes = 200
        )

        // t1 (20m) -> 30m remaining -> t2 (20m) -> 10m remaining -> t3 (20m) cannot fit -> deferred!
        assertEquals("Exactly 2 tasks should be placed within 50m slot budget", 2, report.placedCount)
        assertEquals("Remaining 2 tasks must be deferred", 2, report.deferredCount)
        assertEquals(4, report.placedCount + report.deferredCount)
        assertEquals(DeferralReasonCode.INSUFFICIENT_TIME, report.deferrals.first().reasonCode)
        assertTrue(report.deferrals.first().explanation.contains("exceeds available slot (10m)"))
    }

    @Test
    fun testConflictResolverDetectsCollisionsOnFutureDatesForDatelessTimedTasks() {
        val today = LocalDate.of(2026, 10, 28)
        val tomorrow = today.plusDays(1)
        val tomorrowStart = tomorrow.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Two tasks with explicit 10:00 AM time of day (10 hours = 36_000_000 ms) but scheduledDate = null
        val tenAmOffset = 10 * 3600_000L
        val task1 = TaskEntity(
            id = "conf_1",
            title = "Morning Meeting",
            scheduledDate = null,
            scheduledTime = tenAmOffset,
            estimatedDurationMinutes = 60,
            priority = Priority.HIGH
        )
        val task2 = TaskEntity(
            id = "conf_2",
            title = "Client Call",
            scheduledDate = null,
            scheduledTime = tenAmOffset,
            estimatedDurationMinutes = 30,
            priority = Priority.MEDIUM
        )

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(task1, task2),
            dayMillis = tomorrowStart,
            zoneId = zoneId
        )

        assertEquals("Must detect collision on tomorrow between dateless timed tasks", 1, conflicts.size)
        val conflict = conflicts.first()
        assertEquals("conf_2", conflict.taskId)
        assertEquals("Client Call", conflict.taskTitle)
        assertEquals("Morning Meeting", conflict.conflictingTargetName)
    }

    @Test
    fun testUltradianSplitSchedulesBothPartsAtomically() {
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val longTask = SchedulableTask(
            id = "long_deep",
            title = "Extensive Architecture Review",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 80,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.5f
        )

        // Provide two disjoint slots of 55 minutes each so that the 80m (96m buffered) task cannot
        // fit in any single slot, strictly requiring an ultradian split into part 1 (48m) and part 2 (48m).
        val busy1Start = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val busy1End = today.atTime(LocalTime.of(9, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val busy2Start = today.atTime(LocalTime.of(10, 25)).atZone(zoneId).toInstant().toEpochMilli()
        val busy2End = today.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()

        val busyWindows = listOf(
            TimeSlotWindow(busy1Start, busy1End),
            TimeSlotWindow(busy2Start, busy2End)
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(longTask),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = busyWindows,
            zoneId = zoneId
        )

        assertEquals("Ultradian split should place both part 1 and part 2", 2, outcome.placements.size)
        val p1 = outcome.placements.first { it.task.id.endsWith("#part1") }
        val p2 = outcome.placements.first { it.task.id.endsWith("#part2") }
        assertTrue("Part 2 must start after Part 1 ends + recovery buffer", p2.window.startMillis >= p1.window.endMillis + 15 * 60_000L)
    }

    @Test
    fun testUltradianSplitPhase1CeilingLeavesRoomForPart2BeforeDeadline() {
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // 80m task (explicitly uncalibrated ratio 1.0) with deadline at 11:00 AM
        // Split into two 40m parts with a 15m buffer in between (total span = 40 + 15 + 40 = 95m).
        // Busy windows leave two slots: 08:05..08:55 (50m) and 10:00..10:55 (55m).
        // Neither slot alone can hold the unsplit 80m task, strictly requiring ultradian split.
        val deadlineMillis = today.atTime(LocalTime.of(11, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val busy1Start = today.atTime(LocalTime.of(8, 55)).atZone(zoneId).toInstant().toEpochMilli()
        val busy1End = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val busy2Start = today.atTime(LocalTime.of(10, 55)).atZone(zoneId).toInstant().toEpochMilli()
        val busy2End = today.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()

        val splitTask = SchedulableTask(
            id = "split_deadline_task",
            title = "Complex Analysis",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 80,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.5f,
            deadlineMillis = deadlineMillis
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(splitTask),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(
                TimeSlotWindow(busy1Start, busy1End),
                TimeSlotWindow(busy2Start, busy2End)
            ),
            calibrationRatio = 1.0,
            zoneId = zoneId
        )

        assertEquals("Both parts must be placed before deadline", 2, outcome.placements.size)
        val p1 = outcome.placements.first { it.task.id.endsWith("#part1") }
        val p2 = outcome.placements.first { it.task.id.endsWith("#part2") }
        assertTrue("Part 1 must end with at least 15m buffer + Part 2 duration before deadline", p1.window.endMillis <= deadlineMillis - (15 * 60_000L + 40 * 60_000L))
        assertTrue("Part 2 must finish at or before deadline", p2.window.endMillis <= deadlineMillis)
        assertEquals("Part 2 parentTaskId must match original task id", "split_deadline_task", p2.task.parentTaskId)
    }

    @Test
    fun testSchedulePreparationOrchestratorFutureDateAnchorsDatelessTimedTasks() {
        val tomorrow = LocalDate.of(2026, 10, 29)
        val tomorrowStart = tomorrow.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nowMillis = LocalDate.of(2026, 10, 28).atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Task with intraday scheduledTime (10:00 AM = 36_000_000 ms) and scheduledDate = null
        val tenAmOffset = 10 * 3600_000L
        val datelessTask = TaskEntity(
            id = "future_dateless",
            title = "Morning Sync",
            scheduledDate = null,
            scheduledTime = tenAmOffset,
            isScheduleLocked = true,
            estimatedDurationMinutes = 30
        )

        val behavioralService = DefaultBehavioralSupportService()
        val orchestrator = SchedulePreparationOrchestrator(
            taskSchedulerService = schedulerService,
            behavioralSupportService = behavioralService
        )

        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = listOf(datelessTask),
            allKnownTasks = listOf(datelessTask),
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = UserPreferences(),
            dayDate = tomorrow,
            nowMillis = nowMillis,
            busyWindows = emptyList(),
            forceReplan = false,
            zoneId = zoneId
        )

        assertEquals("Locked dateless task on tomorrow must be placed on tomorrow, not deferred as conflict", 1, outcome.placements.size)
        val placement = outcome.placements.first()
        val expectedStart = tomorrowStart + tenAmOffset
        assertEquals("Placement must be on tomorrow at 10:00 AM", expectedStart, placement.window.startMillis)
    }

    @Test
    fun testPlacementStabilityRespectsParameterizedNowMillis() {
        val tomorrow = LocalDate.of(2026, 10, 29)
        val tenAmOffset = 10 * 3600_000L
        val datelessTask = TaskEntity(
            id = "stability_task",
            title = "Client Call",
            scheduledDate = null,
            scheduledTime = tenAmOffset,
            estimatedDurationMinutes = 30,
            lastAutoScheduledAt = 0L,
            updatedAt = 0L
        )

        // Simulated nowMillis is tomorrow at 09:30 AM (30 minutes before 10:00 AM scheduled start)
        val tomorrowNowMillis = tomorrow.atTime(LocalTime.of(9, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val prefs = UserPreferences(isAutoScheduleToggleOn = true)

        val verdict = PlacementStability.verdict(
            task = datelessTask,
            nowMillis = tomorrowNowMillis,
            prefs = prefs,
            forceReplan = false
        )

        assertEquals("Task 30m away from scheduled time on parameterized nowMillis must trigger NEAR_TERM_FREEZE", PlacementStability.ReplanVerdict.NEAR_TERM_FREEZE, verdict)
    }

    @Test
    fun testAutoSchedulingEngineCoordinatorPlannedMinutesForSplitPart2() {
        // A split task that has plannedBlockMinutes = 45, but unsplit estimatedDurationMinutes = 90
        val splitPartTask = TaskEntity(
            id = "part2_task",
            title = "Deep Research (Part 2/2)",
            estimatedDurationMinutes = 90,
            plannedBlockMinutes = 45
        )

        // Available slots = 50 minutes.
        // Under raw estimatedDurationMinutes (90m), it would be deferred (90 > 50).
        // Under TaskBlockPlan.plannedMinutes (45m), it fits within the 50m slot (45 <= 50)!
        val report = AutoSchedulingEngineCoordinator.planSchedule(
            tasks = listOf(splitPartTask),
            availableSlotsMinutes = 50,
            dailyCognitiveCapacityMinutes = 200
        )

        assertEquals("Split Part 2 task must be placed using plannedBlockMinutes (45m)", 1, report.placedCount)
        assertEquals(0, report.deferredCount)
    }

    @Test
    fun testSchedulePreparationOrchestratorPreservesSplitPart2DurationAndTitle() {
        val today = LocalDate.of(2026, 10, 28)
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Part 2 continuation task with inherited estimatedDurationMinutes = 90 but plannedBlockMinutes = 45
        val part2Task = TaskEntity(
            id = "paper_task_part2",
            title = "Write Paper (Part 2/2)",
            parentTaskId = "paper_task",
            estimatedDurationMinutes = 90,
            plannedBlockMinutes = 45,
            priority = Priority.HIGH
        )

        val behavioralService = DefaultBehavioralSupportService()
        val orchestrator = SchedulePreparationOrchestrator(
            taskSchedulerService = schedulerService,
            behavioralSupportService = behavioralService
        )

        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = listOf(part2Task),
            allKnownTasks = listOf(part2Task),
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = UserPreferences(),
            dayDate = today,
            nowMillis = morning,
            busyWindows = emptyList(),
            forceReplan = false,
            zoneId = zoneId
        )

        assertEquals("Part 2 continuation task must be placed", 1, outcome.placements.size)
        val placement = outcome.placements.first()
        assertEquals("paper_task_part2", placement.task.id)
        assertEquals("Write Paper (Part 2/2)", placement.task.title)
        assertTrue("Part 2 continuation task must have explicit duration set", placement.task.isDurationExplicit)
        assertEquals("Part 2 block must be exactly 45 minutes, not inflated to 90m or calibrated", 45, placement.window.durationMinutes)
    }

    @Test
    fun testTaskSchedulerServiceUltradianRecoveryBufferAndEndMapWithUnderscorePart2() {
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val taskA = SchedulableTask(
            id = "task_a",
            title = "Code Implementation (Part 1/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH
        )
        val taskAPart2 = SchedulableTask(
            id = "task_a_part2",
            title = "Code Implementation (Part 2/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "task_a",
            prerequisiteIds = setOf("task_a")
        )
        val taskB = SchedulableTask(
            id = "task_b",
            title = "Code Review",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 30,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            prerequisiteIds = setOf("task_a")
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(taskA, taskAPart2, taskB),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            zoneId = zoneId
        )

        assertEquals("All 3 tasks (Part 1, Part 2, and downstream) must be scheduled", 3, outcome.placements.size)
        val p1 = outcome.placements.first { it.task.id == "task_a" }
        val p2 = outcome.placements.first { it.task.id == "task_a_part2" }
        val pB = outcome.placements.first { it.task.id == "task_b" }

        assertTrue("Part 2 must start at least 15m after Part 1 ends", p2.window.startMillis >= p1.window.endMillis + 15 * 60_000L)
        assertTrue("Downstream Task B must start after Part 2 ends", pB.window.startMillis >= p2.window.endMillis)
    }

    @Test
    fun testDownstreamTasksWaitUntilContinuationPart2CompletesInSanitizeDag() {
        val taskA = SchedulableTask(
            id = "doc_task",
            title = "Documentation (Part 1/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH
        )
        val taskAPart2 = SchedulableTask(
            id = "doc_task_part2",
            title = "Documentation (Part 2/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "doc_task",
            prerequisiteIds = setOf("doc_task")
        )
        val taskB = SchedulableTask(
            id = "publish_task",
            title = "Publish Documentation",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 20,
            energyDemand = EnergyDemand.HIGH,
            prerequisiteIds = setOf("doc_task")
        )

        val sanitized = DependencyGraphValidator.sanitizeDag(listOf(taskA, taskAPart2, taskB))
        val sanitizedPublish = sanitized.tasks.first { it.id == "publish_task" }

        assertTrue(
            "Downstream task waiting on split parent task must wait for continuation part",
            sanitizedPublish.prerequisiteIds.contains("doc_task_part2")
        )
    }

    @Test
    fun testResolveDeadlineEpochMillisIntradayFallbackWithoutExplicitDeadlineDate() {
        val today = LocalDate.of(2026, 10, 28)
        val todayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowStart = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val fivePmOffset = 17 * 3600_000L

        // Task with intraday deadlineTime and scheduledDate, but deadlineDate == null
        val scheduledTask = TaskEntity(
            id = "t_sched",
            title = "Due with Scheduled Date",
            scheduledDate = todayStart,
            deadlineTime = fivePmOffset,
            deadlineDate = null
        )
        assertEquals(todayStart + fivePmOffset, scheduledTask.resolveDeadlineEpochMillis())

        // Recurring habit with habitDate, but deadlineDate == null
        val habitTask = TaskEntity(
            id = "t_habit",
            title = "Due Recurring Habit",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            deadlineTime = fivePmOffset,
            deadlineDate = null
        )
        assertEquals(todayStart + fivePmOffset, habitTask.resolveDeadlineEpochMillis())

        // Dateless task with parameterized reference date
        val datelessTask = TaskEntity(
            id = "t_dateless",
            title = "Due Dateless",
            deadlineTime = fivePmOffset,
            deadlineDate = null
        )
        assertEquals(tomorrowStart + fivePmOffset, datelessTask.resolveDeadlineEpochMillis(tomorrowStart))
    }

    @Test
    fun testHysteresisAnchoredPart1ReservesUltradianRecoveryBuffer() {
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val eightAm = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Task 1 is Part 1 anchored via hysteresis (already scheduled for 9:00 - 9:45 AM)
        val part1Duration = 45
        val task1 = SchedulableTask(
            id = "hys_1",
            title = "Writing Module (Part 1/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = part1Duration,
            isDurationExplicit = true,
            isLocked = true,
            scheduledStartMillis = morning,
            energyDemand = EnergyDemand.HIGH
        )

        // Task 2 is an unplaced task
        val task2 = SchedulableTask(
            id = "other_task",
            title = "Follow-up Task",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 30,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(task1, task2),
            dayDate = today,
            nowMillis = eightAm,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(TimeSlotWindow(eightAm, morning)),
            zoneId = zoneId
        )

        assertEquals(2, outcome.placements.size)
        val p1 = outcome.placements.first { it.task.id == "hys_1" }
        val p2 = outcome.placements.first { it.task.id == "other_task" }

        val p1End = p1.window.endMillis
        assertTrue(
            "Task placed after hysteresis-anchored Part 1 must respect 15m ultradian buffer (expected >= ${p1End + 15 * 60_000L}, got ${p2.window.startMillis})",
            p2.window.startMillis >= p1End + 15 * 60_000L
        )
    }
}
