package com.neuroflow.app.presentation.v2.focus

import androidx.lifecycle.SavedStateHandle
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusSessionRecoveryTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeTask(id: String, title: String, status: TaskStatus = TaskStatus.ACTIVE): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            quadrant = Quadrant.DO_FIRST,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 25,
            impactScore = 50,
            status = status
        )
    }

    @Test
    fun testRecoveryRestoresUltradian90ModeWithoutPrematureExpiration() = runTest(testDispatcher) {
        val task = makeTask(id = "ultra-task-1", title = "Write Core Architecture Spec")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "ultra-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        // Session started 30 minutes (1800s) ago. ULTRADIAN_90 duration is 90m (5400s).
        val now = System.currentTimeMillis()
        val startedAt = now - (30 * 60 * 1000L)
        val openSession = TimeSessionEntity(
            id = "ultra-session-uuid",
            taskId = "ultra-task-1",
            startedAt = startedAt,
            totalPausedMs = 0L,
            pausedAt = null,
            sessionType = FocusTimerModeV2.ULTRADIAN_90.name
        )

        coEvery { taskRepo.getById("ultra-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("ultra-task-1") } returns openSession
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(savedStateHandle, taskRepo, sessionRepo, woopRepo, prefsDataStore)
        try {
            testScheduler.runCurrent()

            val state = viewModel.uiState.value
            assertEquals(FocusTimerModeV2.ULTRADIAN_90, state.timerMode)
            assertTrue(state.isRunning)
            assertFalse(state.isPaused)
            assertFalse(state.isSprintCompleted)
            assertFalse(state.showSprintCompletionSheet)
            assertEquals(1800L, state.elapsedSeconds)
            assertEquals(3600L, state.remainingSeconds) // 5400L - 1800L = 3600s
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testRecoveryRestoresFlowStopwatchMode() = runTest(testDispatcher) {
        val task = makeTask(id = "flow-task-1", title = "Exploratory Coding")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "flow-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        // Session started 45 minutes (2700s) ago in FLOW_STOPWATCH mode
        val now = System.currentTimeMillis()
        val startedAt = now - (45 * 60 * 1000L)
        val openSession = TimeSessionEntity(
            id = "flow-session-uuid",
            taskId = "flow-task-1",
            startedAt = startedAt,
            totalPausedMs = 0L,
            pausedAt = null,
            sessionType = FocusTimerModeV2.FLOW_STOPWATCH.name
        )

        coEvery { taskRepo.getById("flow-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("flow-task-1") } returns openSession
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(savedStateHandle, taskRepo, sessionRepo, woopRepo, prefsDataStore)
        try {
            testScheduler.runCurrent()

            val state = viewModel.uiState.value
            assertEquals(FocusTimerModeV2.FLOW_STOPWATCH, state.timerMode)
            assertTrue(state.isRunning)
            assertFalse(state.isPaused)
            assertFalse(state.isSprintCompleted)
            assertEquals(0L, state.remainingSeconds)
            assertEquals(2700L, state.elapsedSeconds)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testStartTimerPersistsTimerModeNameToSessionType() = runTest(testDispatcher) {
        val task = makeTask(id = "start-task-1", title = "Write Unit Tests")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "start-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("start-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("start-task-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(savedStateHandle, taskRepo, sessionRepo, woopRepo, prefsDataStore)
        try {
            testScheduler.runCurrent()

            viewModel.selectTimerMode(FocusTimerModeV2.JUST_5_MIN)
            viewModel.startTimer()
            testScheduler.runCurrent()

            val sessionSlot = slot<TimeSessionEntity>()
            coVerify { sessionRepo.insert(capture(sessionSlot)) }
            assertEquals(FocusTimerModeV2.JUST_5_MIN.name, sessionSlot.captured.sessionType)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testKeepGoingGeneratesUniqueSessionUuidAndNeverOverwrites() = runTest(testDispatcher) {
        val task = makeTask(id = "keep-going-task-1", title = "Sprint Optimization")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "keep-going-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("keep-going-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("keep-going-task-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(savedStateHandle, taskRepo, sessionRepo, woopRepo, prefsDataStore)
        try {
            testScheduler.runCurrent()

            viewModel.selectTimerMode(FocusTimerModeV2.JUST_5_MIN)
            viewModel.startTimer()
            testScheduler.runCurrent()

            val firstSessionId = viewModel.uiState.value.activeSessionId
            assertNotNull(firstSessionId)

            // Complete 5m sprint
            repeat(300) { viewModel.tickSecond() }
            testScheduler.runCurrent()

            assertTrue(viewModel.uiState.value.showSprintCompletionSheet)
            assertTrue(viewModel.uiState.value.isSprintCompleted)

            // Trigger keep going for a 25m Pomodoro
            viewModel.keepGoing(FocusTimerModeV2.POMODORO_25)
            testScheduler.runCurrent()

            val secondSessionId = viewModel.uiState.value.activeSessionId
            assertNotNull(secondSessionId)
            assertNotEquals(firstSessionId, secondSessionId)

            // Verify two distinct inserts occurred
            val insertedSessions = mutableListOf<TimeSessionEntity>()
            coVerify(atLeast = 2) { sessionRepo.insert(capture(insertedSessions)) }
            val distinctIds = insertedSessions.map { it.id }.distinct()
            assertEquals(2, distinctIds.size)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testAutoTrackerDoesNotFireOnCompletedTask() = runTest(testDispatcher) {
        val task = makeTask(id = "completed-task-1", title = "Already Done Task", status = TaskStatus.COMPLETED)
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "completed-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("completed-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns emptyList()
        coEvery { sessionRepo.getOpenSessionForTask("completed-task-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
            UserPreferences(autoTrackerEnabled = true)
        )

        val viewModel = FocusViewModelV2(savedStateHandle, taskRepo, sessionRepo, woopRepo, prefsDataStore)
        try {
            testScheduler.runCurrent()

            assertTrue(viewModel.uiState.value.isCompleted)

            // Advance past the 8-second auto-tracker delay
            testScheduler.advanceTimeBy(10_000L)
            testScheduler.runCurrent()

            // Timer must NOT start on completed task
            assertFalse(viewModel.uiState.value.isRunning)
            assertTrue(viewModel.uiState.value.isCompleted)
            coVerify(exactly = 0) { sessionRepo.insert(any()) }
        } finally {
            viewModel.clearForTest()
        }
    }
}
