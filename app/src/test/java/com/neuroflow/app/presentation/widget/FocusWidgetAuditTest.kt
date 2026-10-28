package com.neuroflow.app.presentation.widget

import android.content.Context
import android.content.Intent
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import com.neuroflow.app.worker.FocusWidgetUpdateWorker
import dagger.hilt.android.EntryPointAccessors
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class FocusWidgetAuditTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(EntryPointAccessors::class)
        mockkObject(FocusWidgetUpdateWorker.Companion)
        mockkObject(FocusService.Companion)
        every { FocusWidgetUpdateWorker.enqueueImmediateUpdate(any()) } just Runs
        every { FocusService.stopService(any()) } just Runs
    }

    afterTest {
        unmockkStatic(EntryPointAccessors::class)
        unmockkObject(FocusWidgetUpdateWorker.Companion)
        unmockkObject(FocusService.Companion)
        Dispatchers.resetMain()
    }

    "CompleteTaskWidgetAction does NOT stop FocusService when another task is actively focusing" {
        val context = mockk<Context>(relaxed = true)
        val taskRepo = mockk<TaskRepository>()
        val sessionRepo = mockk<SessionRepository>()
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.markTaskCompleted("task-widget", any()) } returns true
        coEvery { sessionRepo.getOpenSessionForTask("task-widget") } returns null

        // Active session is on a different task ("task-active-deep-work")
        val otherSession = FocusService.Companion.ActiveFocusSession(
            taskId = "task-active-deep-work",
            taskTitle = "Deep Work",
            remainingSeconds = 1200L,
            totalSeconds = 1500L,
            isPaused = false
        )
        every { FocusService.activeSession } returns kotlinx.coroutines.flow.MutableStateFlow(otherSession)

        val entryPoint = mockk<CompleteTaskWidgetAction.WidgetActionEntryPoint>()
        every { entryPoint.taskRepository() } returns taskRepo
        every { entryPoint.sessionRepository() } returns sessionRepo
        every { entryPoint.preferencesDataStore() } returns prefsDataStore
        every { EntryPointAccessors.fromApplication(any(), CompleteTaskWidgetAction.WidgetActionEntryPoint::class.java) } returns entryPoint

        val result = CompleteTaskWidgetAction.execute(context, "task-widget")
        result shouldBe true

        // FocusService MUST NOT be stopped for other task
        verify(exactly = 0) { FocusService.stopService(any()) }
    }

    "CompleteTaskWidgetAction stops FocusService when the completed task is the active focus task" {
        val context = mockk<Context>(relaxed = true)
        val taskRepo = mockk<TaskRepository>()
        val sessionRepo = mockk<SessionRepository>()
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.markTaskCompleted("task-matching", any()) } returns true
        coEvery { sessionRepo.getOpenSessionForTask("task-matching") } returns null

        val matchingSession = FocusService.Companion.ActiveFocusSession(
            taskId = "task-matching",
            taskTitle = "Matching Task",
            remainingSeconds = 500L,
            totalSeconds = 1500L,
            isPaused = false
        )
        every { FocusService.activeSession } returns kotlinx.coroutines.flow.MutableStateFlow(matchingSession)

        val entryPoint = mockk<CompleteTaskWidgetAction.WidgetActionEntryPoint>()
        every { entryPoint.taskRepository() } returns taskRepo
        every { entryPoint.sessionRepository() } returns sessionRepo
        every { entryPoint.preferencesDataStore() } returns prefsDataStore
        every { EntryPointAccessors.fromApplication(any(), CompleteTaskWidgetAction.WidgetActionEntryPoint::class.java) } returns entryPoint

        val result = CompleteTaskWidgetAction.execute(context, "task-matching")
        result shouldBe true

        // FocusService MUST be stopped when IDs match
        verify(exactly = 1) { FocusService.stopService(any()) }
    }

    "CompleteTaskWidgetAction returns false without crash when taskRepository throws exception" {
        val context = mockk<Context>(relaxed = true)
        val taskRepo = mockk<TaskRepository>()
        val sessionRepo = mockk<SessionRepository>()
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.markTaskCompleted("task-fail", any()) } throws RuntimeException("SQLite disk I/O error")
        coEvery { sessionRepo.getOpenSessionForTask("task-fail") } returns null
        every { FocusService.activeSession } returns kotlinx.coroutines.flow.MutableStateFlow(null)

        val entryPoint = mockk<CompleteTaskWidgetAction.WidgetActionEntryPoint>()
        every { entryPoint.taskRepository() } returns taskRepo
        every { entryPoint.sessionRepository() } returns sessionRepo
        every { entryPoint.preferencesDataStore() } returns prefsDataStore
        every { EntryPointAccessors.fromApplication(any(), CompleteTaskWidgetAction.WidgetActionEntryPoint::class.java) } returns entryPoint

        val result = CompleteTaskWidgetAction.execute(context, "task-fail")
        result shouldBe false
        coVerify(exactly = 0) { prefsDataStore.updatePreferences(any()) }
    }

    "TaskRepository markTaskCompleted returns false and ignores already-completed tasks" {
        val taskDao = mockk<TaskDao>()
        val completedTask = TaskEntity(
            id = "task-already-done",
            title = "Completed Task",
            status = TaskStatus.COMPLETED
        )
        coEvery { taskDao.getById("task-already-done") } returns completedTask

        val repository = TaskRepository(
            taskDao = taskDao,
            hyperFocusManager = mockk(relaxed = true),
            workManager = mockk(relaxed = true)
        )

        val success = repository.markTaskCompleted("task-already-done")
        success shouldBe false
        coVerify(exactly = 0) { taskDao.update(any()) }
    }

    "TaskRepository completeAndRecur returns null for already-completed task preventing duplicate recurrence" {
        val taskDao = mockk<TaskDao>()
        val completedTask = TaskEntity(
            id = "task-completed-recur",
            title = "Recurring Task",
            status = TaskStatus.COMPLETED,
            recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY
        )

        val repository = TaskRepository(
            taskDao = taskDao,
            hyperFocusManager = mockk(relaxed = true),
            workManager = mockk(relaxed = true)
        )

        val result = repository.completeAndRecur(completedTask, System.currentTimeMillis())
        result shouldBe null
        coVerify(exactly = 0) { taskDao.insert(any()) }
    }

    "CompleteTaskReceiver handles alternate intent extra keys (taskId and task_id)" {
        mockkObject(CompleteTaskWidgetAction)
        coEvery { CompleteTaskWidgetAction.execute(any(), any()) } returns true

        val receiver = spyk(CompleteTaskReceiver())
        every { receiver.goAsync() } returns mockk(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        // 1. Using "taskId"
        val intent1 = mockk<Intent>()
        every { intent1.action } returns CompleteTaskReceiver.ACTION_COMPLETE_TASK
        every { intent1.getStringExtra(CompleteTaskReceiver.EXTRA_TASK_ID) } returns null
        every { intent1.getStringExtra("taskId") } returns "task-alt-1"
        every { intent1.getStringExtra("task_id") } returns null

        receiver.onReceive(context, intent1)
        coVerify(timeout = 3000) { CompleteTaskWidgetAction.execute(context, "task-alt-1") }

        // 2. Using "task_id"
        val intent2 = mockk<Intent>()
        every { intent2.action } returns CompleteTaskReceiver.ACTION_COMPLETE_TASK
        every { intent2.getStringExtra(CompleteTaskReceiver.EXTRA_TASK_ID) } returns null
        every { intent2.getStringExtra("taskId") } returns null
        every { intent2.getStringExtra("task_id") } returns "task-alt-2"

        receiver.onReceive(context, intent2)
        coVerify(timeout = 3000) { CompleteTaskWidgetAction.execute(context, "task-alt-2") }

        unmockkObject(CompleteTaskWidgetAction)
    }

    "FocusWidgetProvider onReceive handles alternate intent extra keys (task_id and extra_task_id)" {
        val provider = FocusWidgetProvider()
        val context = mockk<Context>(relaxed = true)

        val intent = mockk<Intent>()
        every { intent.action } returns "com.procus.ACTION_OPEN_FOCUS"
        every { intent.getStringExtra("taskId") } returns null
        every { intent.getStringExtra("task_id") } returns "task-provider-alt"
        every { intent.getStringExtra("extra_task_id") } returns null

        provider.onReceive(context, intent)

        verify(exactly = 1) {
            context.startActivity(or(isNull(), any()))
        }
    }

    "FocusWidgetUpdateWorker filterCandidateTasks includes Part 2 continuation when parent is completed" {
        val now = System.currentTimeMillis()
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(now) - 1

        val part2Task = TaskEntity(
            id = "parent-task_part2",
            parentTaskId = "parent-task",
            title = "Deep Focus Part 2/2",
            status = TaskStatus.ACTIVE
        )

        // Only part 2 is in active tasks (parent is completed)
        val activeTasks = listOf(part2Task)
        val candidates = FocusWidgetUpdateWorker.filterCandidateTasks(activeTasks, dayEnd)

        candidates.map { it.id } shouldBe listOf("parent-task_part2")
    }

    "FocusWidgetUpdateWorker filterCandidateTasks blocks Part 2 continuation when parent is still active" {
        val now = System.currentTimeMillis()
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(now) - 1

        val parentTask = TaskEntity(
            id = "parent-task",
            title = "Deep Focus Part 1/2",
            status = TaskStatus.ACTIVE
        )
        val part2Task = TaskEntity(
            id = "parent-task_part2",
            parentTaskId = "parent-task",
            title = "Deep Focus Part 2/2",
            status = TaskStatus.ACTIVE
        )

        // Both parent and part 2 are active -> Part 2 must be blocked
        val activeTasks = listOf(parentTask, part2Task)
        val candidates = FocusWidgetUpdateWorker.filterCandidateTasks(activeTasks, dayEnd)

        candidates.map { it.id } shouldBe listOf("parent-task")
    }

    "FocusWidgetUpdateWorker filterCandidateTasks blocks task when prerequisite is active" {
        val now = System.currentTimeMillis()
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(now) - 1

        val prereq = TaskEntity(
            id = "prereq-1",
            title = "Prerequisite Task",
            status = TaskStatus.ACTIVE
        )
        val dependent = TaskEntity(
            id = "dep-1",
            title = "Dependent Task",
            dependsOnTaskIds = "prereq-1",
            status = TaskStatus.ACTIVE
        )

        val activeTasks = listOf(prereq, dependent)
        val candidates = FocusWidgetUpdateWorker.filterCandidateTasks(activeTasks, dayEnd)

        candidates.map { it.id } shouldBe listOf("prereq-1")
    }

    "FocusWidgetUpdateWorker filterCandidateTasks excludes icebox and future tasks" {
        val now = System.currentTimeMillis()
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(now) - 1

        val iceboxTask = TaskEntity(
            id = "icebox-1",
            title = "Icebox Task",
            quadrant = com.neuroflow.app.domain.model.Quadrant.ELIMINATE,
            status = TaskStatus.ACTIVE
        )
        val futureTask = TaskEntity(
            id = "future-1",
            title = "Tomorrow Task",
            scheduledDate = dayEnd + 10_000L,
            status = TaskStatus.ACTIVE
        )
        val waitingTask = TaskEntity(
            id = "waiting-1",
            title = "Waiting Task",
            waitingFor = "Reviewer feedback",
            status = TaskStatus.ACTIVE
        )
        val validTask = TaskEntity(
            id = "valid-1",
            title = "Valid Task Today",
            status = TaskStatus.ACTIVE
        )

        val activeTasks = listOf(iceboxTask, futureTask, waitingTask, validTask)
        val candidates = FocusWidgetUpdateWorker.filterCandidateTasks(activeTasks, dayEnd)

        candidates.map { it.id } shouldBe listOf("valid-1")
    }

    "BUG-303: FocusService defines distinct NOTIFICATION_ID_COMPLETED to prevent onDestroy cancellation wipeout" {
        FocusService.NOTIFICATION_ID shouldNotBe FocusService.NOTIFICATION_ID_COMPLETED
        FocusService.NOTIFICATION_ID shouldBe 3001
        FocusService.NOTIFICATION_ID_COMPLETED shouldBe 3002
    }

    "BUG-306: TaskBlockPlan.plannedMinutes is used for planned task block duration display" {
        val taskWithPlan = TaskEntity(
            id = "task-plan-widget",
            title = "Task with Plan Duration",
            status = TaskStatus.ACTIVE,
            estimatedDurationMinutes = 0,
            plannedBlockMinutes = 45
        )
        com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(taskWithPlan) shouldBe 45
    }
})