package com.neuroflow.app.worker

import android.app.PendingIntent
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.flowOf

class NotificationWorkerAuditTest : StringSpec({

    val context = mockk<Context>(relaxed = true)
    val workerParams = mockk<WorkerParameters>(relaxed = true)
    val taskRepo = mockk<TaskRepository>(relaxed = true)
    val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    val notificationManager = mockk<android.app.NotificationManager>(relaxed = true)

    beforeSpec {
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns mockk(relaxed = true)
    }

    afterSpec {
        unmockkStatic(PendingIntent::class)
    }

    beforeTest {
        clearAllMocks()
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { context.applicationContext } returns context
        every { context.checkPermission(any(), any(), any()) } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        every { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                deadlineEscalationNotificationsEnabled = true,
                deadlineReminderNotificationsEnabled = true
            )
        )
    }

    "BUG-256: DeadlineEscalationWorker escalates approaching tasks to Quadrant.DO_FIRST in repository" {
        val now = System.currentTimeMillis()
        val deadlineIn24h = now + 24 * 3600_000L

        val urgentTask = TaskEntity(
            id = "urgent-task-1",
            title = "Finalize Tax Returns",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.SCHEDULE,
            recurrence = Recurrence.NONE,
            deadlineDate = deadlineIn24h,
            deadlineTime = 0L
        )

        val alreadyDoFirstTask = TaskEntity(
            id = "already-q1",
            title = "Critical Bugfix",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST,
            recurrence = Recurrence.NONE,
            deadlineDate = deadlineIn24h,
            deadlineTime = 0L
        )

        coEvery { taskRepo.getActiveTasks() } returns listOf(urgentTask, alreadyDoFirstTask)

        val worker = DeadlineEscalationWorker(context, workerParams, taskRepo, prefsDataStore)
        val result = worker.doWork()

        result shouldBe ListenableWorker.Result.success()

        // Verify urgent task is escalated to Quadrant.DO_FIRST
        coVerify(exactly = 1) {
            taskRepo.update(match {
                it.id == "urgent-task-1" && it.quadrant == Quadrant.DO_FIRST
            })
        }

        // Verify already DO_FIRST task is not touched
        coVerify(exactly = 0) {
            taskRepo.update(match {
                it.id == "already-q1"
            })
        }
    }

    "BUG-256: DeadlineEscalationWorker is a no-op when all approaching tasks are already DO_FIRST" {
        val now = System.currentTimeMillis()
        val deadlineIn12h = now + 12 * 3600_000L

        val q1Task = TaskEntity(
            id = "q1-task",
            title = "Already Handled",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST,
            recurrence = Recurrence.NONE,
            deadlineDate = deadlineIn12h,
            deadlineTime = 0L
        )

        coEvery { taskRepo.getActiveTasks() } returns listOf(q1Task)

        val worker = DeadlineEscalationWorker(context, workerParams, taskRepo, prefsDataStore)
        val result = worker.doWork()

        result shouldBe ListenableWorker.Result.success()
        coVerify(exactly = 0) { taskRepo.update(any()) }
    }

    "BUG-260: TaskReminderWorker uses updated title from repository instead of stale inputData snapshot" {
        val taskId = "task-remind-1"
        val now = System.currentTimeMillis()
        val targetMs = now + 15 * 60_000L

        every { workerParams.inputData } returns workDataOf(
            "taskId" to taskId,
            "taskTitle" to "Old Stale Draft Title",
            "minutesBefore" to 15L,
            "targetMs" to targetMs
        )

        val freshTask = TaskEntity(
            id = taskId,
            title = "Final Polished Document Title",
            status = TaskStatus.ACTIVE,
            scheduledDate = targetMs,
            scheduledTime = 0L
        )
        coEvery { taskRepo.getById(taskId) } returns freshTask

        val worker = TaskReminderWorker(context, workerParams, taskRepo, prefsDataStore)
        val result = worker.doWork()

        result shouldBe ListenableWorker.Result.success()
        // The worker loads freshTask and prioritizes freshTask.title
        freshTask.title shouldBe "Final Polished Document Title"
    }

    "BUG-306: DailyPlanWorker includes eligible Part 2 continuation task when Part 1 is completed and uses plannedMinutes" {
        val now = System.currentTimeMillis()
        val today = com.neuroflow.app.data.local.entity.startOfLocalDay(now)

        val completedPart1 = TaskEntity(
            id = "deep-work-1",
            title = "Deep Work Project (Part 1/2)",
            status = TaskStatus.COMPLETED,
            completedAt = today - 3600_000L
        )

        val activePart2 = TaskEntity(
            id = "deep-work-1_part2",
            title = "Deep Work Project (Part 2/2)",
            status = TaskStatus.ACTIVE,
            parentTaskId = "deep-work-1",
            dependsOnTaskIds = "deep-work-1",
            scheduledDate = today,
            scheduledTime = 10 * 3600_000L,
            plannedBlockMinutes = 45,
            estimatedDurationMinutes = 0
        )

        coEvery { taskRepo.getActiveTasks() } returns listOf(activePart2)
        coEvery { taskRepo.getAllTasks() } returns listOf(completedPart1, activePart2)

        val schedService = mockk<com.neuroflow.app.domain.service.TaskSchedulerService>(relaxed = true)
        val circadianService = mockk<com.neuroflow.app.domain.service.CircadianEnergyService>(relaxed = true)
        every { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(dailyPlanNotificationsEnabled = true)
        )
        every { schedService.evaluateTaskScore(any(), any()) } returns com.neuroflow.app.domain.service.TaskScoreBreakdown(
            taskId = "deep-work-1_part2",
            totalScore = 95.0,
            urgencyScore = 80.0,
            impactScore = 80.0,
            energyFitScore = 90.0,
            momentumScore = 70.0,
            bufferedDurationMinutes = 45
        )

        val worker = DailyPlanWorker(
            context,
            workerParams,
            taskRepo,
            prefsDataStore,
            schedService,
            circadianService
        )
        val result = worker.doWork()

        result shouldBe ListenableWorker.Result.success()
        // Verify evaluateTaskScore was called with plannedMinutes = 45 (not estimatedDurationMinutes = 0)
        verify(atLeast = 1) {
            schedService.evaluateTaskScore(
                match { it.id == "deep-work-1_part2" && it.estimatedMinutes == 45 },
                any()
            )
        }
    }

    "BUG-343: DeadlineEscalationWorker escalates recently overdue tasks to Quadrant.DO_FIRST" {
        val now = System.currentTimeMillis()
        val overdueDeadline = now - 3600_000L // Overdue by 1 hour

        val overdueTask = TaskEntity(
            id = "overdue-task-1",
            title = "Overdue Client Report",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.SCHEDULE,
            recurrence = Recurrence.NONE,
            deadlineDate = overdueDeadline,
            deadlineTime = 0L
        )

        coEvery { taskRepo.getActiveTasks() } returns listOf(overdueTask)

        val worker = DeadlineEscalationWorker(context, workerParams, taskRepo, prefsDataStore)
        val result = worker.doWork()

        result shouldBe ListenableWorker.Result.success()

        // Verify overdue task is escalated to Quadrant.DO_FIRST
        coVerify(exactly = 1) {
            taskRepo.update(match {
                it.id == "overdue-task-1" && it.quadrant == Quadrant.DO_FIRST
            })
        }
    }

    "BUG-343: TaskReminderWorker schedules at-target reminder when task is scheduled within 15 minutes" {
        val workManager = mockk<androidx.work.WorkManager>(relaxed = true)
        val now = System.currentTimeMillis()
        val dueIn10Min = now + 10 * 60_000L

        val taskDueSoon = TaskEntity(
            id = "due-soon-task",
            title = "Meeting Soon",
            status = TaskStatus.ACTIVE,
            scheduledDate = dueIn10Min,
            scheduledTime = 0L
        )

        TaskReminderWorker.scheduleReminder(workManager, taskDueSoon, minutesBefore = 15L)

        // Verify reminder is enqueued for target time (minutesBefore = 0L) rather than cancelled
        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "task_reminder_due-soon-task_0",
                androidx.work.ExistingWorkPolicy.REPLACE,
                any<androidx.work.OneTimeWorkRequest>()
            )
        }
    }

    "BUG-382: DeadlineEscalationWorker ignores tasks in Quadrant.ELIMINATE" {
        val now = System.currentTimeMillis()
        val overdueDeadline = now - 3600_000L

        val iceboxTask = TaskEntity(
            id = "icebox-task-1",
            title = "Eliminated Task",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.ELIMINATE,
            recurrence = Recurrence.NONE,
            deadlineDate = overdueDeadline,
            deadlineTime = 0L
        )

        coEvery { taskRepo.getActiveTasks() } returns listOf(iceboxTask)

        val worker = DeadlineEscalationWorker(context, workerParams, taskRepo, prefsDataStore)
        val result = worker.doWork()

        result shouldBe ListenableWorker.Result.success()
        // Verify eliminated task is NOT escalated to DO_FIRST
        coVerify(exactly = 0) { taskRepo.update(any()) }
    }

    "BUG-384: DailyPlanWorker blocks subtask when parentTaskId is still active in repository" {
        val now = System.currentTimeMillis()
        val today = com.neuroflow.app.data.local.entity.startOfLocalDay(now)

        val activeParent = TaskEntity(
            id = "parent-1",
            title = "Parent Task",
            status = TaskStatus.ACTIVE
        )

        val childSubtask = TaskEntity(
            id = "subtask-1",
            title = "Subtask of Parent 1",
            status = TaskStatus.ACTIVE,
            parentTaskId = "parent-1",
            scheduledDate = today,
            scheduledTime = 10 * 3600_000L,
            plannedBlockMinutes = 30
        )

        coEvery { taskRepo.getActiveTasks() } returns listOf(activeParent, childSubtask)
        coEvery { taskRepo.getAllTasks() } returns listOf(activeParent, childSubtask)

        val schedService = mockk<com.neuroflow.app.domain.service.TaskSchedulerService>(relaxed = true)
        val circadianService = mockk<com.neuroflow.app.domain.service.CircadianEnergyService>(relaxed = true)
        every { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(dailyPlanNotificationsEnabled = true)
        )

        val worker = DailyPlanWorker(
            context,
            workerParams,
            taskRepo,
            prefsDataStore,
            schedService,
            circadianService
        )
        val result = worker.doWork()

        result shouldBe ListenableWorker.Result.success()
        // Child subtask whose parent is still active must NOT be evaluated / scheduled
        verify(exactly = 0) {
            schedService.evaluateTaskScore(
                match { it.id == "subtask-1" },
                any()
            )
        }
    }

    "BUG-424: DailyPlanWorker permits dependencies with trailing commas or whitespace" {
        val now = System.currentTimeMillis()
        val today = com.neuroflow.app.data.local.entity.startOfLocalDay(now)

        val completedDep = TaskEntity(
            id = "completed-dep",
            title = "Completed Dependency",
            status = TaskStatus.COMPLETED,
            completedAt = now - 3600_000L
        )

        val activeTask = TaskEntity(
            id = "active-with-trailing-comma",
            title = "Ready Task",
            status = TaskStatus.ACTIVE,
            dependsOnTaskIds = "completed-dep, ",
            scheduledDate = today,
            scheduledTime = 11 * 3600_000L,
            plannedBlockMinutes = 45
        )

        coEvery { taskRepo.getActiveTasks() } returns listOf(activeTask)
        coEvery { taskRepo.getAllTasks() } returns listOf(completedDep, activeTask)

        val schedService = mockk<com.neuroflow.app.domain.service.TaskSchedulerService>(relaxed = true)
        val circadianService = mockk<com.neuroflow.app.domain.service.CircadianEnergyService>(relaxed = true)
        every { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(dailyPlanNotificationsEnabled = true)
        )

        val worker = DailyPlanWorker(
            context,
            workerParams,
            taskRepo,
            prefsDataStore,
            schedService,
            circadianService
        )
        val result = worker.doWork()

        result shouldBe ListenableWorker.Result.success()
        // Active task with trailing comma dependency must be evaluated
        verify(atLeast = 1) {
            schedService.evaluateTaskScore(
                match { it.id == "active-with-trailing-comma" && it.prerequisiteIds.contains("completed-dep") },
                any()
            )
        }
    }

    "BUG-424: TaskReminderWorker uses starts now/in vs due now/in based on whether scheduledTime or deadlineTime is set" {
        val now = System.currentTimeMillis()
        val targetEpoch = now + 15 * 60_000L

        val scheduledOnlyTask = TaskEntity(
            id = "sched-only",
            title = "Morning Standup",
            status = TaskStatus.ACTIVE,
            scheduledDate = now,
            scheduledTime = targetEpoch,
            deadlineDate = null,
            deadlineTime = null
        )

        coEvery { taskRepo.getById("sched-only") } returns scheduledOnlyTask
        every { workerParams.inputData } returns workDataOf(
            "taskId" to "sched-only",
            "minutesBefore" to 15L,
            "targetMs" to targetEpoch
        )

        val worker = TaskReminderWorker(context, workerParams, taskRepo, prefsDataStore)
        val result = worker.doWork()

        result shouldBe ListenableWorker.Result.success()
        // Verify title formatting distinguishes start vs deadline reminders
        TaskReminderWorker.resolveNotificationTitle(
            actualTargetMs = targetEpoch,
            now = now,
            timeLabel = "15 minutes",
            isScheduledStart = true
        ) shouldBe "⏰ Task starts in 15 minutes"

        TaskReminderWorker.resolveNotificationTitle(
            actualTargetMs = targetEpoch,
            now = now,
            timeLabel = "15 minutes",
            isScheduledStart = false
        ) shouldBe "⏰ Task due in 15 minutes"

        TaskReminderWorker.resolveNotificationTitle(
            actualTargetMs = now,
            now = now,
            timeLabel = "15 minutes",
            isScheduledStart = true
        ) shouldBe "⏰ Task starts now"

        TaskReminderWorker.resolveNotificationTitle(
            actualTargetMs = now,
            now = now,
            timeLabel = "15 minutes",
            isScheduledStart = false
        ) shouldBe "⏰ Task is due now"
    }
})
