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

    @Test
    fun testPlacementStabilityDeadlineEpochResolvesWithParameterizedNowMillis() {
        val today = LocalDate.of(2026, 10, 28)
        val futureDate = today.plusDays(4)
        val fivePmOffset = 17 * 3600_000L
        val nineAmOffset = 9 * 3600_000L
        val futureNineAm = futureDate.atStartOfDay(zoneId).toInstant().toEpochMilli() + nineAmOffset

        // Task with scheduledTime at 10:00 AM (1 hour away) and deadlineTime at 5:00 PM (8 hours away)
        val tenAmOffset = 10 * 3600_000L
        val task = TaskEntity(
            id = "future_deadline_task",
            title = "Project Brief",
            scheduledDate = null,
            scheduledTime = tenAmOffset,
            deadlineDate = null,
            deadlineTime = fivePmOffset,
            estimatedDurationMinutes = 30,
            priority = Priority.MEDIUM,
            lastAutoScheduledAt = 0L,
            updatedAt = 0L
        )

        val prefs = UserPreferences(isAutoScheduleToggleOn = true)
        val verdict = PlacementStability.verdict(
            task = task,
            nowMillis = futureNineAm,
            prefs = prefs,
            forceReplan = false
        )

        // 10:00 AM is 1 hour away from 09:00 AM on future date -> within NEAR_TERM_FREEZE_MILLIS (2 hours)
        assertEquals(PlacementStability.ReplanVerdict.NEAR_TERM_FREEZE, verdict)
    }

    @Test
    fun testHysteresisAnchoredPart2UpdatesCompletionEndMapForDownstreamTasks() {
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val nineAm = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val tenAm = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Part 1 anchored at 9:00 - 9:45
        val taskPart1 = SchedulableTask(
            id = "deep_task",
            title = "Deep Task (Part 1/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            isDurationExplicit = true,
            isLocked = true,
            scheduledStartMillis = nineAm,
            energyDemand = EnergyDemand.HIGH
        )
        // Part 2 anchored at 10:00 - 10:45
        val taskPart2 = SchedulableTask(
            id = "deep_task_part2",
            title = "Deep Task (Part 2/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            isDurationExplicit = true,
            isLocked = true,
            scheduledStartMillis = tenAm,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "deep_task",
            prerequisiteIds = setOf("deep_task")
        )
        // Downstream Task B depends on parent "deep_task"
        val taskDownstream = SchedulableTask(
            id = "downstream_b",
            title = "Downstream Task B",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 30,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            prerequisiteIds = setOf("deep_task")
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(taskPart1, taskPart2, taskDownstream),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        assertEquals(3, outcome.placements.size)
        val p2 = outcome.placements.first { it.task.id == "deep_task_part2" }
        val pDownstream = outcome.placements.first { it.task.id == "downstream_b" }

        assertTrue(
            "Downstream task waiting on parent task must be scheduled after Part 2 completes (expected >= ${p2.window.endMillis}, got ${pDownstream.window.startMillis})",
            pDownstream.window.startMillis >= p2.window.endMillis
        )
    }

    @Test
    fun testSplitIntoUltradianPartsPhase1CeilingCappedByWorkdayEnd() {
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // 80m task without deadline (deadlineMillis = null).
        // Workday ends at 17:00 (5:00 PM).
        // Busy windows leave slots:
        // 09:00 - 09:50 (50m)
        // 10:15 - 11:05 (50m)
        // 16:20 - 17:00 (40m) -> cannot fit Part 1 (40m) + 15m buffer + Part 2 (40m) before 17:00!
        // None of the slots can fit the unsplit 80m task, forcing ultradian split.
        val busy0Start = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val busy0End = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val busy1Start = today.atTime(LocalTime.of(9, 50)).atZone(zoneId).toInstant().toEpochMilli()
        val busy1End = today.atTime(LocalTime.of(10, 15)).atZone(zoneId).toInstant().toEpochMilli()
        val busy2Start = today.atTime(LocalTime.of(11, 5)).atZone(zoneId).toInstant().toEpochMilli()
        val busy2End = today.atTime(LocalTime.of(16, 20)).atZone(zoneId).toInstant().toEpochMilli()

        val splitTask = SchedulableTask(
            id = "split_end_day",
            title = "Comprehensive System Design",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 80,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.5f,
            deadlineMillis = null
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(splitTask),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(
                TimeSlotWindow(busy0Start, busy0End),
                TimeSlotWindow(busy1Start, busy1End),
                TimeSlotWindow(busy2Start, busy2End)
            ),
            calibrationRatio = 1.0,
            workDayStartHour = 8,
            workDayEndHour = 17,
            zoneId = zoneId
        )

        assertEquals("Both ultradian split parts must be scheduled within workday", 2, outcome.placements.size)
        val p1 = outcome.placements.first { it.task.id.endsWith("#part1") }
        val p2 = outcome.placements.first { it.task.id.endsWith("#part2") }
        val dayEnd = today.atTime(LocalTime.of(17, 0)).atZone(zoneId).toInstant().toEpochMilli()
        assertTrue("Part 1 must end before Part 2", p1.window.endMillis < p2.window.startMillis)
        assertTrue("Part 2 must finish before workday end", p2.window.endMillis <= dayEnd)
    }

    @Test
    fun testCleanSplitTaskIdDoesNotTruncateWordsWithPartSubstring() {
        val scheduler = schedulerService as DefaultTaskSchedulerService
        assertEquals("team_party", scheduler.cleanSplitTaskId("team_party"))
        assertEquals("project_partner_sync", scheduler.cleanSplitTaskId("project_partner_sync"))
        assertEquals("partial_review", scheduler.cleanSplitTaskId("partial_review"))
        assertEquals("task_a", scheduler.cleanSplitTaskId("task_a_part2"))
        assertEquals("task_a", scheduler.cleanSplitTaskId("task_a#part1"))
        assertEquals("task_a", scheduler.cleanSplitTaskId("task_a#part2"))

        // Also verify DependencyGraphValidator doesn't conflate tasks with words containing part
        val tParty = SchedulableTask(
            id = "team_party",
            title = "Team Party",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW
        )
        val sanitized = DependencyGraphValidator.sanitizeDag(listOf(tParty))
        assertEquals("team_party", sanitized.tasks.first().id)
        assertTrue(sanitized.brokenCycleEdges.isEmpty())
    }

    @Test
    fun testSchedulePreparationOrchestratorIncludesParentTaskIdInPrerequisites() {
        val today = LocalDate.of(2026, 10, 28)
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val parentTask = TaskEntity(
            id = "parent_task_1",
            title = "Module Build",
            estimatedDurationMinutes = 30,
            priority = Priority.HIGH
        )
        // Subtask with parentTaskId set but empty dependsOnTaskIds
        val subtask = TaskEntity(
            id = "subtask_1",
            title = "Subtask 1",
            parentTaskId = "parent_task_1",
            dependsOnTaskIds = "",
            estimatedDurationMinutes = 20,
            priority = Priority.MEDIUM
        )

        val behavioralService = DefaultBehavioralSupportService()
        val orchestrator = SchedulePreparationOrchestrator(
            taskSchedulerService = schedulerService,
            behavioralSupportService = behavioralService
        )

        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = listOf(subtask, parentTask),
            allKnownTasks = listOf(subtask, parentTask),
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = UserPreferences(),
            dayDate = today,
            nowMillis = morning,
            busyWindows = emptyList(),
            forceReplan = false,
            zoneId = zoneId
        )

        assertEquals(2, outcome.placements.size)
        val pParent = outcome.placements.first { it.task.id == "parent_task_1" }
        val pSub = outcome.placements.first { it.task.id == "subtask_1" }

        assertTrue(
            "Subtask must start after parent task finishes (expected >= ${pParent.window.endMillis}, got ${pSub.window.startMillis})",
            pSub.window.startMillis >= pParent.window.endMillis
        )
    }

    @Test
    fun testMultiPartContinuationFinalPartSelectedInDependencyGraphValidator() {
        // BUG-414: Tasks generated by splitTaskIntoSessions have random UUIDs.
        // Part 1 UUID begins with 'z', Part 3 UUID begins with 'a'.
        // Lexicographical ID max would pick Part 1!
        // DependencyGraphValidator must pick Part 3 as the final part.
        val part1 = SchedulableTask(
            id = "z_uuid_part1",
            title = "Large Task (Part 1/3)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "parent_task_split"
        )
        val part2 = SchedulableTask(
            id = "m_uuid_part2",
            title = "Large Task (Part 2/3)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "parent_task_split",
            prerequisiteIds = setOf("z_uuid_part1")
        )
        val part3 = SchedulableTask(
            id = "a_uuid_part3",
            title = "Large Task (Part 3/3)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "parent_task_split",
            prerequisiteIds = setOf("m_uuid_part2")
        )
        // Downstream task waiting on the parent task
        val downstream = SchedulableTask(
            id = "downstream_task",
            title = "Review Results",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW,
            prerequisiteIds = setOf("parent_task_split")
        )

        val sanitized = DependencyGraphValidator.sanitizeDag(listOf(part1, part2, part3, downstream))
        val sanitizedDownstream = sanitized.tasks.first { it.id == "downstream_task" }

        assertTrue(
            "Downstream task must wait for the final continuation part (a_uuid_part3)",
            sanitizedDownstream.prerequisiteIds.contains("a_uuid_part3")
        )
        assertFalse(
            "Downstream task must NOT wait only for part 1 (z_uuid_part1)",
            sanitizedDownstream.prerequisiteIds.contains("z_uuid_part1") && !sanitizedDownstream.prerequisiteIds.contains("a_uuid_part3")
        )
    }

    @Test
    fun testContinuationPartInferredPrerequisitesInOrchestrator() {
        // BUG-415: Continuation task with #part2 and null parentTaskId must infer parent and preceding part
        val today = LocalDate.of(2026, 10, 28)
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val part1 = TaskEntity(
            id = "task_analysis#part1",
            title = "Data Analysis (Part 1/2)",
            estimatedDurationMinutes = 40,
            priority = Priority.HIGH
        )
        val part2 = TaskEntity(
            id = "task_analysis#part2",
            title = "Data Analysis (Part 2/2)",
            estimatedDurationMinutes = 40,
            parentTaskId = null,
            dependsOnTaskIds = "",
            priority = Priority.HIGH
        )

        val behavioralService = DefaultBehavioralSupportService()
        val orchestrator = SchedulePreparationOrchestrator(
            taskSchedulerService = schedulerService,
            behavioralSupportService = behavioralService
        )

        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = listOf(part2, part1), // Passed out-of-order
            allKnownTasks = listOf(part1, part2),
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = UserPreferences(),
            dayDate = today,
            nowMillis = morning,
            busyWindows = emptyList(),
            forceReplan = false,
            zoneId = zoneId
        )

        assertEquals(2, outcome.placements.size)
        val p1 = outcome.placements.first { it.task.id == "task_analysis#part1" }
        val p2 = outcome.placements.first { it.task.id == "task_analysis#part2" }

        assertTrue(
            "Part 2 must start after Part 1 completes (p1 end: ${p1.window.endMillis}, p2 start: ${p2.window.startMillis})",
            p2.window.startMillis >= p1.window.endMillis
        )
    }

    @Test
    fun testCrossDayParentActiveDoesNotBlockSameDayContinuationParts() {
        // BUG-416: Parent task is in activeUncompletedTaskIds (allKnownTasks has parent active).
        // Part 1 is anchored via hysteresis or scheduled today.
        // Part 2 must NOT be deferred as "pending on a future date"!
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val slot1Start = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val slot2Start = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val part1 = SchedulableTask(
            id = "parent_doc#part1",
            title = "Document Architecture (Part 1/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 40,
            energyDemand = EnergyDemand.HIGH,
            scheduledStartMillis = slot1Start,
            isDurationExplicit = true
        )
        val part2 = SchedulableTask(
            id = "parent_doc#part2",
            title = "Document Architecture (Part 2/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 40,
            energyDemand = EnergyDemand.HIGH,
            scheduledStartMillis = slot2Start,
            isDurationExplicit = true,
            parentTaskId = "parent_doc",
            prerequisiteIds = setOf("parent_doc", "parent_doc#part1")
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(part1, part2),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            // "parent_doc" is active in activeUncompletedTaskIds
            activeUncompletedTaskIds = setOf("parent_doc")
        )

        assertEquals("Both Part 1 and Part 2 must be scheduled without being blocked by parent_doc", 2, outcome.placements.size)
        assertTrue(outcome.deferrals.none { it.taskId == "parent_doc" || it.taskId == "parent_doc#part2" })
        val p1 = outcome.placements.first { it.task.id == "parent_doc#part1" }
        val p2 = outcome.placements.first { it.task.id == "parent_doc#part2" }
        assertTrue("Part 2 must start after Part 1", p2.window.startMillis >= p1.window.endMillis)
    }

    @Test
    fun testPart1TaskNotTreatedAsPart2ContinuationInOrchestrator() {
        // BUG-417: When a task is split into sessions, Part 1 has parentTaskId set to parent.
        // It must NOT be marked as isPart2Continuation = true.
        val today = LocalDate.of(2026, 10, 28)
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val sessionPart1 = TaskEntity(
            id = "part1_session_uuid",
            title = "Deep Refactoring (Part 1/2)",
            estimatedDurationMinutes = 45,
            plannedBlockMinutes = 45,
            parentTaskId = "parent_refactor_id",
            priority = Priority.HIGH
        )

        val behavioralService = DefaultBehavioralSupportService()
        val orchestrator = SchedulePreparationOrchestrator(
            taskSchedulerService = schedulerService,
            behavioralSupportService = behavioralService
        )

        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = listOf(sessionPart1),
            allKnownTasks = listOf(sessionPart1),
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = UserPreferences(),
            dayDate = today,
            nowMillis = morning,
            busyWindows = emptyList(),
            forceReplan = false,
            zoneId = zoneId
        )

        assertEquals(1, outcome.placements.size)
        val p1 = outcome.placements.first()
        assertEquals("part1_session_uuid", p1.task.id)
    }

    @Test
    fun testPart1DoesNotPrematurelyUnlockParentChildren() {
        // BUG-449: Placing Part 1 of a multi-part split task must NOT update completionEndMap[parentId]
        // or prematurely unlock downstream tasks that depend on the parent task before Part 2 finishes.
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val part1 = SchedulableTask(
            id = "parent_task#part1",
            title = "Complex Analysis (Part 1/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "parent_task"
        )
        val part2 = SchedulableTask(
            id = "parent_task#part2",
            title = "Complex Analysis (Part 2/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "parent_task",
            prerequisiteIds = setOf("parent_task#part1")
        )
        val downstreamDependent = SchedulableTask(
            id = "downstream_review",
            title = "Review Complex Analysis",
            category = TaskCategory.ADMIN,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW,
            prerequisiteIds = setOf("parent_task")
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(part1, part2, downstreamDependent),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        assertEquals("All 3 tasks must be placed", 3, outcome.placements.size)
        val p1 = outcome.placements.first { it.task.id == "parent_task#part1" }
        val p2 = outcome.placements.first { it.task.id == "parent_task#part2" }
        val pDownstream = outcome.placements.first { it.task.id == "downstream_review" }

        assertTrue("Part 2 must start after Part 1 ends", p2.window.startMillis >= p1.window.endMillis)
        assertTrue("Downstream task must start after Part 2 ends (not after Part 1)", pDownstream.window.startMillis >= p2.window.endMillis)
    }

    @Test
    fun testMultiPartContinuationWithUuidPrerequisitesNotBlocked() {
        // BUG-450: Multi-part tasks split with random UUIDs and sharing parentTaskId must not be blocked
        // by the parent task being active in activeUncompletedTaskIds.
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val p1Uuid = "uuid_part1_888"
        val p2Uuid = "uuid_part2_999"
        val parentUuid = "uuid_parent_777"

        val part1 = SchedulableTask(
            id = p1Uuid,
            title = "Quarterly Plan (Part 1/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 40,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = parentUuid
        )
        val part2 = SchedulableTask(
            id = p2Uuid,
            title = "Quarterly Plan (Part 2/2)",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 40,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = parentUuid,
            prerequisiteIds = setOf(parentUuid, p1Uuid)
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(part1, part2),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            activeUncompletedTaskIds = setOf(parentUuid)
        )

        assertEquals("Both UUID Part 1 and Part 2 must be scheduled", 2, outcome.placements.size)
        assertTrue(outcome.deferrals.none { it.taskId == p1Uuid || it.taskId == p2Uuid || it.taskId == parentUuid })
        val placedP1 = outcome.placements.first { it.task.id == p1Uuid }
        val placedP2 = outcome.placements.first { it.task.id == p2Uuid }
        assertTrue("Part 2 must start after Part 1", placedP2.window.startMillis >= placedP1.window.endMillis)
    }

    @Test
    fun testUntimedDateOnlyTaskDoesNotResolveToMidnightAndPart1HasNoParentPrereq() {
        // BUG-451: Untimed date-only tasks must not have scheduledStartMillis anchored to midnight (00:00).
        // Part 1 must not have parentTaskId injected as a prerequisite.
        val today = LocalDate.of(2026, 10, 28)
        val dayStartMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val untimedTask = TaskEntity(
            id = "untimed_task_1",
            title = "Untimed General Work",
            scheduledDate = dayStartMillis,
            scheduledTime = null,
            estimatedDurationMinutes = 30,
            priority = Priority.MEDIUM
        )
        val part1Task = TaskEntity(
            id = "part1_task_entity",
            title = "Big Task (Part 1/2)",
            scheduledDate = dayStartMillis,
            scheduledTime = null,
            estimatedDurationMinutes = 45,
            parentTaskId = "parent_active_entity",
            priority = Priority.HIGH
        )

        val behavioralService = DefaultBehavioralSupportService()
        val orchestrator = SchedulePreparationOrchestrator(
            taskSchedulerService = schedulerService,
            behavioralSupportService = behavioralService
        )

        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = listOf(untimedTask, part1Task),
            allKnownTasks = listOf(untimedTask, part1Task, TaskEntity(id = "parent_active_entity", title = "Parent Active", status = TaskStatus.ACTIVE)),
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = UserPreferences(workDayStart = 8, workDayEnd = 20),
            dayDate = today,
            nowMillis = morning,
            busyWindows = emptyList(),
            forceReplan = false,
            zoneId = zoneId
        )

        assertEquals("Both tasks must be placed", 2, outcome.placements.size)
        val untimedPlacement = outcome.placements.first { it.task.id == "untimed_task_1" }
        val p1Placement = outcome.placements.first { it.task.id == "part1_task_entity" }

        assertTrue("Untimed task must be scheduled during daylight work hours (>= 8:00 AM)", untimedPlacement.window.startMillis >= morning)
        assertTrue("Part 1 must be scheduled during daylight work hours", p1Placement.window.startMillis >= morning)
        assertFalse("Part 1 must not depend on parent_active_entity", p1Placement.task.prerequisiteIds.contains("parent_active_entity"))
    }

    @Test
    fun testDualEpochScheduledTimeTreatedAsManualSchedule() {
        // BUG-452: Tasks with absolute epoch scheduledTime (>= ONE_DAY_MILLIS) and scheduledDate = null
        // must be recognized as having manual schedule data and be immutable by auto-scheduler when !isAutoScheduled.
        val today = LocalDate.of(2026, 10, 28)
        val targetEpoch = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val manualEpochTask = TaskEntity(
            id = "manual_epoch_task",
            title = "Client Presentation",
            scheduledDate = null,
            scheduledTime = targetEpoch,
            isAutoScheduled = false
        )
        val autoScheduledEpochTask = TaskEntity(
            id = "auto_epoch_task",
            title = "Routine Email Review",
            scheduledDate = null,
            scheduledTime = targetEpoch,
            isAutoScheduled = true
        )

        assertTrue(
            "Manual epoch task must be recognized as hasManualScheduleData",
            com.neuroflow.app.domain.scheduler.AutoSchedulingContracts.hasManualScheduleData(manualEpochTask)
        )
        assertFalse(
            "Manual epoch task must NOT be mutable by auto-scheduler",
            com.neuroflow.app.domain.scheduler.AutoSchedulingContracts.isMutableByAutoScheduler(manualEpochTask)
        )
        assertTrue(
            "Auto-scheduled epoch task MUST be mutable by auto-scheduler",
            com.neuroflow.app.domain.scheduler.AutoSchedulingContracts.isMutableByAutoScheduler(autoScheduledEpochTask)
        )
    }

    @Test
    fun testDagChildUnlockDoesNotDoubleAdmitWhenSplitCompletes() {
        // BUG-AUDIT-001: When an ultradian split completes, downstream children inDegree
        // must be decremented and admitted ONLY when newDeg == 0 (not <= 0).
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // 80m task requiring split (slots are 50m each)
        val splitTask = SchedulableTask(
            id = "task_split_parent",
            title = "Large Architecture Document",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 80,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.5f
        )

        // Multiple children depending on the split task
        val child1 = SchedulableTask(
            id = "child_task_1",
            title = "Review Architecture Part 1",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 20,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.LOW,
            prerequisiteIds = setOf("task_split_parent")
        )
        val child2 = SchedulableTask(
            id = "child_task_2",
            title = "Review Architecture Part 2",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 20,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.LOW,
            prerequisiteIds = setOf("task_split_parent")
        )

        // Multi-parent child: depends on both a normal prerequisite and the split task
        val normalPrereq = SchedulableTask(
            id = "normal_prereq",
            title = "Quick Prep",
            category = TaskCategory.ADMIN,
            priority = 3,
            estimatedMinutes = 15,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.LOW
        )
        val multiChild = SchedulableTask(
            id = "child_multi_parent",
            title = "Final Sign-off",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 15,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.LOW,
            prerequisiteIds = setOf("normal_prereq", "task_split_parent")
        )

        // Busy windows leaving two 50m slots in morning and a wide slot in afternoon:
        // 08:00..08:50 (50m), 08:50..10:00 (busy), 10:00..10:50 (50m), 10:50..12:00 (busy), 12:00..18:00 (free)
        val busy1Start = today.atTime(LocalTime.of(8, 50)).atZone(zoneId).toInstant().toEpochMilli()
        val busy1End = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val busy2Start = today.atTime(LocalTime.of(10, 50)).atZone(zoneId).toInstant().toEpochMilli()
        val busy2End = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(splitTask, child1, child2, normalPrereq, multiChild),
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

        // Verify no duplicate placements: every placement taskId must appear exactly once
        val placementTaskIds = outcome.placements.map { it.task.id }
        assertEquals(
            "All placed tasks must have unique IDs with zero duplicates: $placementTaskIds",
            placementTaskIds.size,
            placementTaskIds.distinct().size
        )

        // Verify that child1, child2, and multiChild each appear exactly once
        assertEquals(1, outcome.placements.count { it.task.id == "child_task_1" })
        assertEquals(1, outcome.placements.count { it.task.id == "child_task_2" })
        assertEquals(1, outcome.placements.count { it.task.id == "child_multi_parent" })
    }

    @Test
    fun testDeadlineBreachHardConstraintPreventsPostDeadlineSlotFromWinning() {
        // BUG-AUDIT-002: In primary circadian window search, candidates ending past deadline
        // must be hard skipped so high-priority tasks cannot place after deadline when pre-deadline slots exist.
        val today = LocalDate.of(2026, 10, 28)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val deadlineMillis = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val urgentTask = SchedulableTask(
            id = "urgent_deadline_task",
            title = "Urgent Client Deliverable",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            isFrog = true,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isDurationExplicit = true,
            deadlineMillis = deadlineMillis
        )

        // Empty busy windows: entire day 08:00..22:00 is available.
        // Alertness may peak in afternoon, but task MUST be scheduled before 12:00 PM deadline.
        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(urgentTask),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            calibrationRatio = 1.0,
            zoneId = zoneId
        )

        val placement = outcome.placements.firstOrNull { it.task.id == "urgent_deadline_task" }
        org.junit.Assert.assertNotNull("Task must be placed", placement)
        assertTrue(
            "Task placement must end on or before its deadline (ended at ${placement!!.window.endMillis}, deadline was $deadlineMillis)",
            placement.window.endMillis <= deadlineMillis
        )
    }

    @Test
    fun testResolveDeadlineEpochMillisDateOnlyResolvesToEndOfDay() {
        // BUG-AUDIT-003: Date-only deadline (deadlineTime == null) must resolve to end of day (23:59:59.999),
        // not midnight (00:00:00), so tasks are not considered prematurely overdue.
        val today = LocalDate.of(2026, 10, 28)
        val todayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val oneDayMillis = com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS

        val dateOnlyTask = TaskEntity(
            id = "date_only_deadline",
            title = "Due Today Sometime",
            deadlineDate = todayStart,
            deadlineTime = null
        )

        val resolved = dateOnlyTask.resolveDeadlineEpochMillis()
        assertEquals(
            "Date-only deadline must resolve to end-of-day (todayStart + 86_400_000 - 1)",
            todayStart + oneDayMillis - 1L,
            resolved
        )

        // When deadlineTime is provided, it should use the explicit time offset
        val timedTask = TaskEntity(
            id = "timed_deadline",
            title = "Due at 3 PM",
            deadlineDate = todayStart,
            deadlineTime = 15 * 3600_000L
        )
        assertEquals(
            todayStart + 15 * 3600_000L,
            timedTask.resolveDeadlineEpochMillis()
        )
    }

    @Test
    fun testDagCycleBreakingVictimComparatorProtectsFrogAndHighImpactPrerequisites() {
        // BUG-AUDIT-007: Cycle victim comparator must pick the least important (most expendable) task
        // to have its incoming edges severed, preserving high-impact frog tasks' dependency ordering.
        val frogTask = SchedulableTask(
            id = "frog_task",
            title = "Crucial Frog Milestone",
            category = TaskCategory.DEEP_WORK,
            priority = 5,
            impactScore = 100,
            isFrog = true,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            prerequisiteIds = setOf("helper_task")
        )
        val helperTask = SchedulableTask(
            id = "helper_task",
            title = "Low Priority Helper",
            category = TaskCategory.GENERAL,
            priority = 1,
            impactScore = 5,
            isFrog = false,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW,
            prerequisiteIds = setOf("frog_task")
        )

        val result = DependencyGraphValidator.sanitizeDag(listOf(frogTask, helperTask))
        assertEquals(2, result.tasks.size)
        assertEquals("Exactly 1 cyclic edge must be severed", 1, result.brokenCycleEdges.size)

        // The edge severed must be helper_task's incoming edge from frog_task ("frog_task" -> "helper_task")
        val brokenEdge = result.brokenCycleEdges.first()
        assertEquals(
            "Helper task must be chosen as cycle victim; helper_task's incoming edge from frog_task must be severed",
            "frog_task" to "helper_task",
            brokenEdge
        )

        val sanitizedFrog = result.tasks.first { it.id == "frog_task" }
        val sanitizedHelper = result.tasks.first { it.id == "helper_task" }

        assertTrue(
            "Frog task must preserve its prerequisite on helper_task",
            sanitizedFrog.prerequisiteIds.contains("helper_task")
        )
        assertFalse(
            "Helper task must have had its prerequisite on frog_task severed",
            sanitizedHelper.prerequisiteIds.contains("frog_task")
        )
    }

    @Test
    fun testAutoSchedulingEngineCoordinatorCapacityCheckConsistency() {
        // BUG-AUDIT-009: planSchedule checks remaining cognitive capacity and available slots consistently
        val task1 = TaskEntity(
            id = "task_1",
            title = "Task 1",
            estimatedDurationMinutes = 60
        )
        val task2 = TaskEntity(
            id = "task_2",
            title = "Task 2",
            estimatedDurationMinutes = 60
        )

        // Case 1: Cognitive budget is 90m, slots = 180m -> task 2 deferred for COGNITIVE_BUDGET_EXCEEDED
        val reportCognitiveOverflow = AutoSchedulingEngineCoordinator.planSchedule(
            tasks = listOf(task1, task2),
            availableSlotsMinutes = 180,
            dailyCognitiveCapacityMinutes = 90
        )
        assertEquals(1, reportCognitiveOverflow.placedCount)
        assertEquals(1, reportCognitiveOverflow.deferredCount)
        assertEquals(
            com.neuroflow.app.domain.scheduler.DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
            reportCognitiveOverflow.deferrals.first().reasonCode
        )

        // Case 2: Cognitive budget is 180m, slots = 90m -> task 2 deferred for INSUFFICIENT_TIME
        val reportSlotOverflow = AutoSchedulingEngineCoordinator.planSchedule(
            tasks = listOf(task1, task2),
            availableSlotsMinutes = 90,
            dailyCognitiveCapacityMinutes = 180
        )
        assertEquals(1, reportSlotOverflow.placedCount)
        assertEquals(1, reportSlotOverflow.deferredCount)
        assertEquals(
            com.neuroflow.app.domain.scheduler.DeferralReasonCode.INSUFFICIENT_TIME,
            reportSlotOverflow.deferrals.first().reasonCode
        )
    }

    @Test
    fun testPart2DelimiterConsistencyBetweenSchedulerAndPersistence() {
        // BUG-AUDIT-011: Scheduler splits into #part1 and #part2
        val circadian = DefaultCircadianEnergyService()
        val scheduler = DefaultTaskSchedulerService(circadian)
        val longTask = SchedulableTask(
            id = "long_deep_task",
            title = "Big Architectural Design",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 180,
            energyDemand = EnergyDemand.HIGH
        )
        val splitPair = scheduler.splitIntoUltradianParts(longTask, longTask.estimatedMinutes)
        org.junit.Assert.assertNotNull("Task exceeding ultradian limit must be split", splitPair)
        val (part1, part2) = splitPair!!
        assertEquals("long_deep_task#part1", part1.id)
        assertEquals("long_deep_task#part2", part2.id)
        assertTrue(part2.prerequisiteIds.contains("long_deep_task#part1"))
    }
}
