package com.neuroflow.app.domain.scheduler

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.worker.ScheduleAutoTasksWorker
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SchedulingDateSanityValidatorTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 11, 24)
    private val yesterday: LocalDate = today.minusDays(1)
    private val tomorrow: LocalDate = today.plusDays(1)
    private val nextWeek: LocalDate = today.plusDays(7)

    private val todayStart: Long = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val todayEnd: Long = tomorrow.atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
    private val yesterdayStart: Long = yesterday.atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val tomorrowStart: Long = tomorrow.atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val nextWeekStart: Long = nextWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val nowMillis: Long = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()

    private val circadianService = DefaultCircadianEnergyService()
    private val schedulerService = DefaultTaskSchedulerService(circadianService)
    private val orchestrator = SchedulePreparationOrchestrator(
        taskSchedulerService = schedulerService,
        behavioralSupportService = DefaultBehavioralSupportService()
    )

    @Test
    fun validateEntitiesForDay_rejectsAllFutureAnchorsAndAcceptsOnlyEligibleTodayTasks() {
        val tomorrowAutoTask = TaskEntity(
            id = "tmrw-auto",
            title = "Tomorrow Auto Task",
            scheduledDate = tomorrowStart,
            scheduledTime = 10 * 3600_000L,
            isAutoScheduled = true,
            status = TaskStatus.ACTIVE
        )
        val nextWeekDateOnly = TaskEntity(
            id = "next-week-date",
            title = "Next Week Milestone",
            scheduledDate = nextWeekStart,
            scheduledTime = null,
            status = TaskStatus.ACTIVE
        )
        val tomorrowDualEpoch = TaskEntity(
            id = "tmrw-dual-epoch",
            title = "Tomorrow Dual Epoch Task",
            scheduledDate = null,
            scheduledTime = tomorrowStart + 14 * 3600_000L,
            status = TaskStatus.ACTIVE
        )
        val tomorrowHabit = TaskEntity(
            id = "tmrw-habit",
            title = "Tomorrow Recurring Habit",
            recurrence = Recurrence.DAILY,
            habitDate = tomorrowStart,
            scheduledDate = null,
            status = TaskStatus.ACTIVE
        )
        val futureDeadlineOnly = TaskEntity(
            id = "future-deadline-backlog",
            title = "Next Week Report",
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = nextWeekStart,
            status = TaskStatus.ACTIVE
        )
        val todayTask = TaskEntity(
            id = "today-task",
            title = "Today Scheduled Task",
            scheduledDate = todayStart,
            scheduledTime = 15 * 3600_000L,
            deadlineDate = nextWeekStart, // Scheduled today even though deadline is later -> eligible!
            isAutoScheduled = true,
            status = TaskStatus.ACTIVE
        )
        val overdueTask = TaskEntity(
            id = "overdue-task",
            title = "Yesterday Missed Task",
            scheduledDate = yesterdayStart,
            scheduledTime = 11 * 3600_000L,
            isAutoScheduled = true,
            status = TaskStatus.ACTIVE
        )
        val inboxTask = TaskEntity(
            id = "inbox-task",
            title = "Dateless Inbox Task",
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = null,
            status = TaskStatus.ACTIVE
        )

        val result = SchedulingDateSanityValidator.validateEntitiesForDay(
            tasks = listOf(
                tomorrowAutoTask,
                nextWeekDateOnly,
                tomorrowDualEpoch,
                tomorrowHabit,
                futureDeadlineOnly,
                todayTask,
                overdueTask,
                inboxTask
            ),
            targetDate = today,
            nowMillis = nowMillis,
            zoneId = zoneId,
            allowFutureDeadlinedBacklog = false
        )

        assertEquals(
            setOf("today-task", "overdue-task", "inbox-task"),
            result.eligibleTasks.map { it.id }.toSet()
        )
        val reasonsById = result.rejectedTasks.associate { it.taskId to it.reason }
        assertEquals(DateIneligibilityReason.FUTURE_SCHEDULED_DATE, reasonsById["tmrw-auto"])
        assertEquals(DateIneligibilityReason.FUTURE_SCHEDULED_DATE, reasonsById["next-week-date"])
        assertEquals(DateIneligibilityReason.FUTURE_SCHEDULED_TIMESTAMP, reasonsById["tmrw-dual-epoch"])
        assertEquals(DateIneligibilityReason.FUTURE_HABIT_DATE, reasonsById["tmrw-habit"])
        assertEquals(DateIneligibilityReason.FUTURE_ONLY_DEADLINE, reasonsById["future-deadline-backlog"])
    }

    @Test
    fun inspectEntityForDay_nonTodayTargetRejectsOtherDayAnchors() {
        val todayTask = TaskEntity(
            id = "today-task",
            title = "Today Task",
            scheduledDate = todayStart,
            scheduledTime = 11 * 3600_000L,
            status = TaskStatus.ACTIVE
        )
        val tomorrowTask = TaskEntity(
            id = "tmrw-task",
            title = "Tomorrow Task",
            scheduledDate = tomorrowStart,
            scheduledTime = 11 * 3600_000L,
            status = TaskStatus.ACTIVE
        )

        assertEquals(
            DateIneligibilityReason.OTHER_DAY_ANCHOR,
            SchedulingDateSanityValidator.inspectEntityForDay(
                task = todayTask,
                targetDate = tomorrow,
                nowMillis = nowMillis,
                zoneId = zoneId,
                allowFutureDeadlinedBacklog = true
            )
        )
        assertNull(
            SchedulingDateSanityValidator.inspectEntityForDay(
                task = tomorrowTask,
                targetDate = tomorrow,
                nowMillis = nowMillis,
                zoneId = zoneId,
                allowFutureDeadlinedBacklog = true
            )
        )
    }

    @Test
    fun coreSchedulerService_stripsFutureAnchoredSchedulableTasksUnlessHardDeadlineIsToday() {
        val wakeMillis = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val todayDeadline = today.atTime(LocalTime.of(17, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val futurePreferred = SchedulableTask(
            id = "future-preferred",
            title = "Tomorrow Deep Work",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            preferredDayStartMillis = tomorrowStart
        )
        val futureTimestamp = SchedulableTask(
            id = "future-timestamp",
            title = "Next Week Sync",
            category = TaskCategory.ADMIN,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW,
            scheduledStartMillis = nextWeekStart + 10 * 3600_000L
        )
        val todayEligible = SchedulableTask(
            id = "today-eligible",
            title = "Today Eligible Task",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.MODERATE,
            preferredDayStartMillis = todayStart
        )
        val hardDeadlineRescue = SchedulableTask(
            id = "hard-deadline-today",
            title = "Due Today Rescue",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.HIGH,
            preferredDayStartMillis = tomorrowStart,
            deadlineMillis = todayDeadline
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(futurePreferred, futureTimestamp, todayEligible, hardDeadlineRescue),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeMillis,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            workDayStartHour = 8,
            workDayEndHour = 20
        )

        val placedIds = outcome.placements.map { it.task.id }.toSet()
        val deferredIds = outcome.deferrals.map { it.taskId }.toSet()

        assertFalse("Future preferred-day task must not be placed on Today", "future-preferred" in placedIds)
        assertFalse("Future scheduled-timestamp task must not be placed on Today", "future-timestamp" in placedIds)
        assertFalse("Future tasks must not appear as Today deferrals", "future-preferred" in deferredIds)
        assertFalse("Future tasks must not appear as Today deferrals", "future-timestamp" in deferredIds)
        assertTrue("Eligible Today task must be placed", "today-eligible" in placedIds)
        assertTrue("Hard deadline strictly Today must be rescued onto Today", "hard-deadline-today" in placedIds)
    }

    @Test
    fun schedulePreparationOrchestrator_rejectsFutureEntitiesBeforeScheduling() {
        val tomorrowTask = TaskEntity(
            id = "tmrw-orch",
            title = "Tomorrow Orchestrator Task",
            scheduledDate = tomorrowStart,
            scheduledTime = 10 * 3600_000L,
            isAutoScheduled = true,
            estimatedDurationMinutes = 45,
            status = TaskStatus.ACTIVE
        )
        val futureDeadlineBacklog = TaskEntity(
            id = "future-dl-orch",
            title = "Future Deadline Backlog",
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = nextWeekStart,
            estimatedDurationMinutes = 30,
            status = TaskStatus.ACTIVE
        )
        val todayTask = TaskEntity(
            id = "today-orch",
            title = "Today Orchestrator Task",
            scheduledDate = todayStart,
            scheduledTime = null,
            estimatedDurationMinutes = 30,
            status = TaskStatus.ACTIVE
        )

        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = listOf(tomorrowTask, futureDeadlineBacklog, todayTask),
            allKnownTasks = listOf(tomorrowTask, futureDeadlineBacklog, todayTask),
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = UserPreferences(wakeTimeHour = 7, workDayStart = 8, workDayEnd = 20),
            dayDate = today,
            nowMillis = nowMillis,
            busyWindows = emptyList(),
            forceReplan = true,
            zoneId = zoneId,
            allowFutureDeadlinedBacklog = false
        )

        val placedIds = outcome.placements.map { it.task.id }.toSet()
        assertEquals(setOf("today-orch"), placedIds)
    }

    @Test
    fun writeTimeSanityGate_blocksOverwritingFutureTaskOntoTodayAndAllowsEligibleUpdates() {
        val todayProposedSlot = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val tomorrowProposedSlot = tomorrow.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val futureScheduledTask = TaskEntity(
            id = "future-write",
            title = "Tomorrow Task",
            scheduledDate = tomorrowStart,
            scheduledTime = 10 * 3600_000L,
            isAutoScheduled = true,
            status = TaskStatus.ACTIVE
        )
        val futureHabitTask = TaskEntity(
            id = "future-habit-write",
            title = "Tomorrow Habit",
            recurrence = Recurrence.DAILY,
            habitDate = tomorrowStart,
            status = TaskStatus.ACTIVE
        )
        val futureDeadlinedBacklog = TaskEntity(
            id = "future-dl-write",
            title = "Next Week Unscheduled",
            scheduledDate = null,
            deadlineDate = nextWeekStart,
            status = TaskStatus.ACTIVE
        )
        val todayTask = TaskEntity(
            id = "today-write",
            title = "Today Task",
            scheduledDate = todayStart,
            scheduledTime = 16 * 3600_000L,
            isAutoScheduled = true,
            status = TaskStatus.ACTIVE
        )
        val unscheduledInboxTask = TaskEntity(
            id = "inbox-write",
            title = "Inbox Task",
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = null,
            status = TaskStatus.ACTIVE
        )

        assertFalse(
            "Write-time gate must refuse moving a Tomorrow task onto Today",
            SchedulingDateSanityValidator.canApplyPlacementToEntity(
                existingTask = futureScheduledTask,
                proposedStartMillis = todayProposedSlot,
                nowMillis = nowMillis,
                zoneId = zoneId,
                allowFutureDeadlinedBacklog = false
            )
        )
        assertFalse(
            "Write-time gate must refuse moving a Tomorrow habit onto Today",
            SchedulingDateSanityValidator.canApplyPlacementToEntity(
                existingTask = futureHabitTask,
                proposedStartMillis = todayProposedSlot,
                nowMillis = nowMillis,
                zoneId = zoneId,
                allowFutureDeadlinedBacklog = false
            )
        )
        assertFalse(
            "Background write-time gate must refuse pulling future-deadlined backlog onto Today",
            SchedulingDateSanityValidator.canApplyPlacementToEntity(
                existingTask = futureDeadlinedBacklog,
                proposedStartMillis = todayProposedSlot,
                nowMillis = nowMillis,
                zoneId = zoneId,
                allowFutureDeadlinedBacklog = false
            )
        )
        assertTrue(
            "Write-time gate must allow rescheduling Today's task on Today",
            SchedulingDateSanityValidator.canApplyPlacementToEntity(
                existingTask = todayTask,
                proposedStartMillis = todayProposedSlot,
                nowMillis = nowMillis,
                zoneId = zoneId,
                allowFutureDeadlinedBacklog = false
            )
        )
        assertTrue(
            "Write-time gate must allow placing dateless inbox task onto Today",
            SchedulingDateSanityValidator.canApplyPlacementToEntity(
                existingTask = unscheduledInboxTask,
                proposedStartMillis = todayProposedSlot,
                nowMillis = nowMillis,
                zoneId = zoneId,
                allowFutureDeadlinedBacklog = false
            )
        )
        assertTrue(
            "Write-time gate must allow rescheduling Tomorrow's task on Tomorrow",
            SchedulingDateSanityValidator.canApplyPlacementToEntity(
                existingTask = futureScheduledTask,
                proposedStartMillis = tomorrowProposedSlot,
                nowMillis = nowMillis,
                zoneId = zoneId,
                allowFutureDeadlinedBacklog = false
            )
        )
    }

    @Test
    fun deletingTodayTaskTriggersRecheck_futureTasksRemainUntouchedAndOnlyEligibleTasksReschedule() = runTest {
        val taskDao = mockk<TaskDao>(relaxed = true)
        val workManager = mockk<WorkManager>(relaxed = true)
        val hyperFocusManager = mockk<HyperFocusManager>(relaxed = true)
        coEvery { hyperFocusManager.isTaskDeletionBlocked(any()) } returns false

        val repo = TaskRepository(
            taskDao = taskDao,
            workManager = workManager,
            hyperFocusManager = Lazy { hyperFocusManager }
        )

        val deletedTodayTask = TaskEntity(
            id = "deleted-today-morning",
            title = "Today 10 AM Block",
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L,
            isAutoScheduled = true,
            estimatedDurationMinutes = 60,
            status = TaskStatus.ACTIVE
        )
        val tomorrowAutoTask = TaskEntity(
            id = "tmrw-auto-block",
            title = "Tomorrow Deep Work",
            scheduledDate = tomorrowStart,
            scheduledTime = 9 * 3600_000L,
            isAutoScheduled = true,
            lastAutoScheduledAt = nowMillis - 12 * 3600_000L,
            estimatedDurationMinutes = 60,
            priority = Priority.HIGH,
            isFrog = true,
            status = TaskStatus.ACTIVE
        )
        val nextWeekAutoTask = TaskEntity(
            id = "next-week-auto-block",
            title = "Next Week Strategy",
            scheduledDate = nextWeekStart,
            scheduledTime = 11 * 3600_000L,
            isAutoScheduled = true,
            lastAutoScheduledAt = nowMillis - 24 * 3600_000L,
            estimatedDurationMinutes = 45,
            status = TaskStatus.ACTIVE
        )
        val futureHabit = TaskEntity(
            id = "tmrw-habit-block",
            title = "Tomorrow Habit",
            recurrence = Recurrence.DAILY,
            habitDate = tomorrowStart,
            estimatedDurationMinutes = 30,
            status = TaskStatus.ACTIVE
        )
        val futureDeadlinedBacklog = TaskEntity(
            id = "next-week-deadline-only",
            title = "Next Week Unscheduled Deadline",
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = nextWeekStart,
            estimatedDurationMinutes = 45,
            status = TaskStatus.ACTIVE
        )
        val eligibleTodayAfternoon = TaskEntity(
            id = "today-afternoon-eligible",
            title = "Today Afternoon Task",
            scheduledDate = todayStart,
            scheduledTime = 16 * 3600_000L,
            isAutoScheduled = true,
            lastAutoScheduledAt = nowMillis - 6 * 3600_000L,
            estimatedDurationMinutes = 45,
            status = TaskStatus.ACTIVE
        )
        val eligibleDatelessInbox = TaskEntity(
            id = "inbox-eligible",
            title = "Unscheduled Inbox Task",
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = null,
            estimatedDurationMinutes = 30,
            status = TaskStatus.ACTIVE
        )

        // 1. User deletes today's morning task -> triggers background auto-scheduling recheck
        repo.delete(deletedTodayTask)
        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "${ScheduleAutoTasksWorker.WORK_NAME}_trigger",
                any<ExistingWorkPolicy>(),
                any<OneTimeWorkRequest>()
            )
        }

        // 2. Worker runs with remaining active tasks after deletion
        val remainingTasks = listOf(
            tomorrowAutoTask,
            nextWeekAutoTask,
            futureHabit,
            futureDeadlinedBacklog,
            eligibleTodayAfternoon,
            eligibleDatelessInbox
        )
        val prefs = UserPreferences(
            autoSchedulingEnabled = true,
            isAutoScheduleToggleOn = true,
            wakeTimeHour = 7,
            workDayStart = 8,
            workDayEnd = 20
        )

        val buckets = ScheduleAutoTasksWorker.selectCandidateBuckets(
            allTasks = remainingTasks,
            dayStart = todayStart,
            dayEnd = todayEnd,
            nowMillis = nowMillis,
            prefs = prefs,
            forceReplan = true,
            zoneId = zoneId
        )
        val candidateTasks = (buckets.unscheduledTasks + buckets.missedTasks + buckets.replanCandidates)
            .distinctBy { it.id }

        assertEquals(
            "Only today's eligible task and dateless inbox task may enter candidate selection",
            setOf("today-afternoon-eligible", "inbox-eligible"),
            candidateTasks.map { it.id }.toSet()
        )

        // 3. Orchestrator + core scheduler run on candidates -> only eligible tasks get placed on Today
        val outcome = orchestrator.orchestrateDayScheduleWithOutcome(
            candidateTasks = candidateTasks,
            allKnownTasks = remainingTasks,
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = prefs,
            dayDate = today,
            nowMillis = nowMillis,
            busyWindows = emptyList(),
            forceReplan = true,
            zoneId = zoneId,
            allowFutureDeadlinedBacklog = false
        )

        val placedIds = outcome.placements.map { it.task.id }.toSet()
        assertEquals(setOf("today-afternoon-eligible", "inbox-eligible"), placedIds)
        assertTrue(
            "Every placement must pass the write-time sanity gate",
            outcome.placements.all { placement ->
                val entity = remainingTasks.first { it.id == placement.task.id }
                SchedulingDateSanityValidator.canApplyPlacementToEntity(
                    existingTask = entity,
                    proposedStartMillis = placement.window.startMillis,
                    nowMillis = nowMillis,
                    zoneId = zoneId,
                    allowFutureDeadlinedBacklog = false
                )
            }
        )
    }
}
