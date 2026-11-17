package com.neuroflow.app.presentation.v2.flow

import android.content.Context
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class FlowViewModelV2RegressionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(AutonomyNudgeEngine)
        every { AutonomyNudgeEngine.scheduleNudge(any<Context>(), any<TaskEntity>()) } returns Unit
        every { AutonomyNudgeEngine.cancelNudge(any<Context>(), any<String>()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(AutonomyNudgeEngine)
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleMicroStep updates both Room micro-step entity and serialized ifThenPlan`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        val step0 = TaskMicroStepEntity(id = "ms-0", taskId = "t-ms", title = "Open file", sortOrder = 0, isCompleted = false)
        val step1 = TaskMicroStepEntity(id = "ms-1", taskId = "t-ms", title = "Write spec", sortOrder = 1, isCompleted = false)
        val task = TaskEntity(
            id = "t-ms",
            title = "Write Architecture",
            ifThenPlan = "[ ] Open file\n[ ] Write spec"
        )

        coEvery { taskRepo.getById("t-ms") } returns task
        coEvery { taskRepo.getMicroSteps("t-ms") } returns listOf(step0, step1)
        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(task))
        every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val vm = FlowViewModelV2(taskRepository = taskRepo, preferencesDataStore = prefsDataStore)
        vm.toggleMicroStep("t-ms", 1)
        advanceUntilIdle()

        coVerify(exactly = 1) { taskRepo.toggleMicroStepCompleted(step1) }
        val updatedSlot = slot<TaskEntity>()
        coVerify { taskRepo.update(capture(updatedSlot)) }
        val parsed = StepByStepPlanCodec.parse(updatedSlot.captured.ifThenPlan)
        assertEquals(2, parsed.size)
        assertFalse(parsed[0].completed)
        assertTrue(parsed[1].completed)
    }

    @Test
    fun `undoCompleteTask restores pre-complete habitStreak habitDate and deletes spawned occurrence`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val recurringTask = TaskEntity(
            id = "habit-undo-1",
            title = "Daily Mobility",
            recurrence = Recurrence.DAILY,
            habitStreak = 7,
            habitDate = todayStart,
            isHabitual = true
        )
        val spawnedTask = recurringTask.copy(id = "habit-undo-1-next", habitStreak = 8)

        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(recurringTask))
        every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences(totalTasksCompleted = 10))
        coEvery { taskRepo.completeAndRecur(recurringTask, any()) } returns "habit-undo-1-next"
        coEvery { taskRepo.getById("habit-undo-1-next") } returns spawnedTask

        val vm = FlowViewModelV2(taskRepository = taskRepo, preferencesDataStore = prefsDataStore)
        vm.completeTask(recurringTask)
        advanceUntilIdle()

        vm.undoCompleteTask()
        advanceUntilIdle()

        coVerify(exactly = 1) { taskRepo.delete(spawnedTask) }
        coVerify {
            taskRepo.update(match {
                it.id == "habit-undo-1" &&
                    it.status == TaskStatus.ACTIVE &&
                    it.habitStreak == 7 &&
                    it.habitDate == todayStart &&
                    it.completedAt == null
            })
        }
    }

    @Test
    fun `isEveningShutdownTime dynamically respects user workDayEnd hour`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val prefsFlow = MutableStateFlow(UserPreferences(workDayEnd = 19))

        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
        every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
        every { prefsDataStore.preferencesFlow } returns prefsFlow

        val fixed1930 = LocalDate.of(2026, 4, 15).atTime(19, 30)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val vm = FlowViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            nowProvider = { fixed1930 }
        )

        val stateAtWorkEnd19 = vm.uiState.filter { !it.isLoading }.first()
        assertTrue(stateAtWorkEnd19.isEveningShutdownTime)

        prefsFlow.value = UserPreferences(workDayEnd = 21)
        val stateAtWorkEnd21 = vm.uiState.filter { !it.isLoading && it.preferences.workDayEnd == 21 }.first()
        assertFalse(stateAtWorkEnd21.isEveningShutdownTime)
    }

    @Test
    fun `unscheduled backlog tasks with intraday scheduledTime hint are not falsely flagged overdue`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        val nowAtNoon = LocalDate.of(2026, 4, 15).atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val backlogTasks = (1..4).map { idx ->
            TaskEntity(
                id = "backlog-hint-$idx",
                title = "Backlog Task $idx",
                scheduledDate = null,
                scheduledTime = 6 * 3600_000L, // < ONE_DAY_MILLIS time hint, not an epoch date
                status = TaskStatus.ACTIVE
            )
        }

        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(backlogTasks)
        every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val vm = FlowViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            nowProvider = { nowAtNoon }
        )

        val state = vm.uiState.filter { !it.isLoading }.first()
        assertFalse(
            "Unscheduled backlog tasks with intraday time hints must not trigger false overdue prompt",
            state.showZeroGuiltPrompt
        )

        vm.freshStartRollover()
        advanceUntilIdle()

        coVerify(exactly = 0) { taskRepo.update(any()) }
    }
}
