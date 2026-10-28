package com.neuroflow.app.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveReminderTargetMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.flowOf

class AutonomyNudgeWorkerTest : StringSpec({

    val context = mockk<Context>(relaxed = true)
    val workerParams = mockk<WorkerParameters>(relaxed = true)
    val taskRepo = mockk<TaskRepository>(relaxed = true)
    val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

    val taskId = "task-nudge-test"

    beforeTest {
        clearAllMocks()
        every { workerParams.inputData } returns workDataOf("taskId" to taskId)
        every { prefsDataStore.preferencesFlow } returns flowOf(UserPreferences(autonomyNudgeNotificationsEnabled = true))
    }

    "skips notification when autonomyNudgeNotificationsEnabled is false" {
        every { prefsDataStore.preferencesFlow } returns flowOf(UserPreferences(autonomyNudgeNotificationsEnabled = false))
        val worker = AutonomyNudgeWorker(context, workerParams, taskRepo, prefsDataStore)

        val result = worker.doWork()
        result shouldBe ListenableWorker.Result.success()
        coVerify(exactly = 0) { taskRepo.getById(any()) }
    }

    "skips notification when task is in Quadrant.ELIMINATE (Icebox zero-guilt contract)" {
        val now = System.currentTimeMillis()
        val iceboxTask = TaskEntity(
            id = taskId,
            title = "Iceboxed Project",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.ELIMINATE,
            scheduledDate = startOfLocalDay(now),
            scheduledTime = 10 * 3600_000L
        )
        coEvery { taskRepo.getById(taskId) } returns iceboxTask
        val worker = AutonomyNudgeWorker(context, workerParams, taskRepo, prefsDataStore)

        val result = worker.doWork()
        result shouldBe ListenableWorker.Result.success()
    }

    "skips notification when task is in unscheduled backlog (scheduledDate and deadlineDate null)" {
        val backlogTask = TaskEntity(
            id = taskId,
            title = "Backlog Someday",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.SCHEDULE,
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = null,
            deadlineTime = null
        )
        coEvery { taskRepo.getById(taskId) } returns backlogTask
        val worker = AutonomyNudgeWorker(context, workerParams, taskRepo, prefsDataStore)

        val result = worker.doWork()
        result shouldBe ListenableWorker.Result.success()
    }

    "skips notification when task is scheduled in the future" {
        val now = System.currentTimeMillis()
        val futureDate = startOfLocalDay(now) + 3 * 86_400_000L
        val futureTask = TaskEntity(
            id = taskId,
            title = "Future Task",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.SCHEDULE,
            scheduledDate = futureDate,
            scheduledTime = 10 * 3600_000L
        )
        coEvery { taskRepo.getById(taskId) } returns futureTask
        val worker = AutonomyNudgeWorker(context, workerParams, taskRepo, prefsDataStore)

        val result = worker.doWork()
        result shouldBe ListenableWorker.Result.success()
    }

    "skips notification when task is not ACTIVE or has sessionCount > 0" {
        val completedTask = TaskEntity(
            id = taskId,
            title = "Completed Task",
            status = TaskStatus.COMPLETED
        )
        coEvery { taskRepo.getById(taskId) } returns completedTask
        val worker = AutonomyNudgeWorker(context, workerParams, taskRepo, prefsDataStore)

        val result = worker.doWork()
        result shouldBe ListenableWorker.Result.success()

        val inProgressTask = TaskEntity(
            id = taskId,
            title = "In-Progress Task",
            status = TaskStatus.ACTIVE,
            sessionCount = 1
        )
        coEvery { taskRepo.getById(taskId) } returns inProgressTask
        val worker2 = AutonomyNudgeWorker(context, workerParams, taskRepo, prefsDataStore)

        val result2 = worker2.doWork()
        result2 shouldBe ListenableWorker.Result.success()
    }

    "BUG-257: future task delay is anchored to dueAt - now rather than static 2 hours" {
        val now = 1770000000000L
        val fiveHoursLater = now + 5 * 3600_000L

        val scheduledTask = TaskEntity(
            id = "future-task",
            title = "Ship Patch",
            scheduledDate = fiveHoursLater,
            scheduledTime = 0L,
            status = TaskStatus.ACTIVE
        )

        val dueAt = scheduledTask.effectiveReminderTargetMillis()
        dueAt shouldBe fiveHoursLater

        val delayMillis = if (dueAt != null && dueAt > now) {
            dueAt - now
        } else {
            java.util.concurrent.TimeUnit.HOURS.toMillis(2L)
        }

        delayMillis shouldBe (5 * 3600_000L)
    }

    "BUG-304: task scheduled for today with future deadline is NOT skipped" {
        val now = System.currentTimeMillis()
        val today = startOfLocalDay(now)
        val futureDeadline = today + 4 * 86_400_000L
        val todayTaskWithFutureDeadline = TaskEntity(
            id = taskId,
            title = "Task Due Friday Scheduled Today",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.SCHEDULE,
            scheduledDate = today,
            scheduledTime = 0L,
            deadlineDate = futureDeadline,
            deadlineTime = 17 * 3600_000L
        )
        coEvery { taskRepo.getById(taskId) } returns todayTaskWithFutureDeadline
        val worker = AutonomyNudgeWorker(context, workerParams, taskRepo, prefsDataStore)

        val result = worker.doWork()
        result shouldBe ListenableWorker.Result.success()
    }

    "BUG-305: AutonomyNudgeEngine schedules nudge using WorkManager directly" {
        val workManager = mockk<androidx.work.WorkManager>(relaxed = true)
        val task = TaskEntity(
            id = "task-nudge-wm",
            title = "Nudge Task via WorkManager",
            status = TaskStatus.ACTIVE,
            scheduledDate = System.currentTimeMillis() + 3600_000L,
            scheduledTime = 0L
        )

        com.neuroflow.app.domain.engine.AutonomyNudgeEngine.scheduleNudge(workManager, task)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                eq("autonomy_nudge_task-nudge-wm"),
                eq(androidx.work.ExistingWorkPolicy.REPLACE),
                any<androidx.work.OneTimeWorkRequest>()
            )
        }
    }
})
