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
})
