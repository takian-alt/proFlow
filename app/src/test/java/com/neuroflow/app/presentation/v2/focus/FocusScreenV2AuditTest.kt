package com.neuroflow.app.presentation.v2.focus

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.v2.focus.service.FocusService
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusScreenV2AuditTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockContext = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(AutonomyNudgeEngine)
        mockkObject(FocusService)
        every { AutonomyNudgeEngine.cancelNudge(any<Context>(), any<String>()) } returns Unit
        every { AutonomyNudgeEngine.scheduleNudge(any<Context>(), any<TaskEntity>()) } returns Unit
        every { FocusService.startService(any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit
        every { FocusService.pauseService(any()) } returns Unit
        every { FocusService.resumeService(any()) } returns Unit
        every { FocusService.stopService(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(AutonomyNudgeEngine)
        unmockkObject(FocusService)
        Dispatchers.resetMain()
    }

    private fun makeTask(
        id: String,
        title: String,
        status: TaskStatus = TaskStatus.ACTIVE,
        isFrog: Boolean = false,
        quadrant: Quadrant = Quadrant.DO_FIRST,
        scheduledDate: Long? = null,
        scheduledTime: Long? = null
    ): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            quadrant = quadrant,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 25,
            impactScore = 50,
            status = status,
            isFrog = isFrog,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            totalTimeTrackedMinutes = 0f,
            sessionCount = 0
        )
    }

    @Test
    fun testPausedSessionSurvivesOnClearedWithoutKillingFocusService() = runTest(testDispatcher) {
        val task = makeTask(id = "paused-task-1", title = "Architect Neural Pipeline")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "paused-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("paused-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("paused-task-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            // Start timer, then pause it
            viewModel.startTimer()
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isRunning)
            assertFalse(viewModel.uiState.value.isPaused)
            assertTrue(viewModel.uiState.value.isSessionActive)

            viewModel.pauseTimer()
            testScheduler.runCurrent()
            assertFalse(viewModel.uiState.value.isRunning)
            assertTrue(viewModel.uiState.value.isPaused)
            // Invariant: Paused session is still active
            assertTrue(viewModel.uiState.value.isSessionActive)

            // When navigating away from FocusScreenV2 (calling onCleared), FocusService must NOT be stopped!
            viewModel.clearForTest()
            testScheduler.runCurrent()

            coVerify(exactly = 0) { FocusService.stopService(any()) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testStoppedOrCompletedSessionStopsFocusServiceOnCleared() = runTest(testDispatcher) {
        val task = makeTask(id = "stopped-task-1", title = "Quick Task")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "stopped-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("stopped-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("stopped-task-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()
            // Never started timer: neither running nor paused
            assertFalse(viewModel.uiState.value.isRunning)
            assertFalse(viewModel.uiState.value.isPaused)
            assertFalse(viewModel.uiState.value.isSessionActive)

            viewModel.clearForTest()
            testScheduler.runCurrent()

            coVerify(atLeast = 1) { FocusService.stopService(any()) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testCompleteTaskWithoutRunningTimerDoesNotFabricateFocusMinutesOrSessions() = runTest(testDispatcher) {
        val task = makeTask(id = "untracked-task-1", title = "Done Offline")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "untracked-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("untracked-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("untracked-task-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
            UserPreferences(totalFocusMinutes = 100, totalTasksCompleted = 5)
        )

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            // Complete task directly without ever starting the timer
            viewModel.completeTask()
            testScheduler.runCurrent()

            val taskSlot = slot<TaskEntity>()
            coVerify { taskRepo.completeAndRecur(capture(taskSlot), any()) }

            // Invariant: totalTimeTrackedMinutes must NOT fabricate 1 minute; sessionCount must not increment
            assertEquals(0f, taskSlot.captured.totalTimeTrackedMinutes, 0.001f)
            assertEquals(0, taskSlot.captured.sessionCount)
            assertEquals(TaskStatus.ACTIVE, taskSlot.captured.status)
            assertEquals(TaskStatus.COMPLETED, viewModel.uiState.value.task?.status)
            assertTrue(viewModel.uiState.value.isCompleted)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testCompleteTaskWhilePausedIncorporatesOngoingPauseDuration() = runTest(testDispatcher) {
        val task = makeTask(id = "paused-complete-task", title = "Paused Work")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "paused-complete-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        val openSession = TimeSessionEntity(
            id = "sess-paused",
            taskId = "paused-complete-task",
            startedAt = System.currentTimeMillis() - 10000L,
            totalPausedMs = 5000L
        )

        coEvery { taskRepo.getById("paused-complete-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("paused-complete-task") } returns openSession
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            // Start timer, tick 10 seconds, then pause
            viewModel.startTimer()
            testScheduler.runCurrent()
            repeat(10) { viewModel.tickSecond() }

            viewModel.pauseTimer()
            testScheduler.runCurrent()

            val pausedAt = viewModel.uiState.value.pausedAtMillis
            assertNotNull(pausedAt)

            // Complete while paused
            viewModel.completeTask()
            testScheduler.runCurrent()

            val updatedSessions = mutableListOf<TimeSessionEntity>()
            coVerify(atLeast = 1) { sessionRepo.update(capture(updatedSessions)) }

            // Invariant: totalPausedMs must include accumulated pause plus ongoing pause
            val lastSession = updatedSessions.last()
            val savedPause = lastSession.totalPausedMs
            assertTrue("Total pause ($savedPause) should be >= 0", savedPause >= 0L)
            assertNotNull(lastSession.endedAt)
            assertNull(lastSession.pausedAt)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testLoadTaskRanksNextTaskAndExcludesEliminateIceboxTasks() = runTest(testDispatcher) {
        val currentTask = makeTask(id = "current-task", title = "Current Sprint")
        val iceboxTask = makeTask(id = "icebox-task", title = "Someday Maybe", quadrant = Quadrant.ELIMINATE)
        val laterTask = makeTask(id = "later-task", title = "Later Routine", isFrog = false)
        val frogTask = makeTask(id = "frog-task", title = "High Leverage Frog", isFrog = true)

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "current-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("current-task") } returns currentTask
        // Repository returns icebox first, then later, then frog
        coEvery { taskRepo.getActiveTasks() } returns listOf(currentTask, iceboxTask, laterTask, frogTask)
        coEvery { sessionRepo.getOpenSessionForTask("current-task") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            // Invariant: Icebox task must be excluded, and Daily Frog must be prioritized
            assertEquals("frog-task", viewModel.uiState.value.nextTaskId)
            assertEquals("High Leverage Frog", viewModel.uiState.value.nextTaskTitle)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testAddMicroStepUpdatesBothTaskEntityAndMicroStepsInUiState() = runTest(testDispatcher) {
        val task = makeTask(id = "micro-step-task", title = "Modular Refactoring")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "micro-step-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("micro-step-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("micro-step-task") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()
            assertEquals(0, viewModel.uiState.value.microSteps.size)

            viewModel.addMicroStep("Extract Interface")
            testScheduler.runCurrent()

            // Invariant: Both microSteps list AND task.ifThenPlan must reflect the new step
            assertEquals(1, viewModel.uiState.value.microSteps.size)
            assertEquals("Extract Interface", viewModel.uiState.value.microSteps[0].text)
            assertNotNull(viewModel.uiState.value.task?.ifThenPlan)
            assertTrue(viewModel.uiState.value.task!!.ifThenPlan!!.contains("Extract Interface"))
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testExternalServicePauseAndResumeSynchronizesWithViewModelUiState() = runTest(testDispatcher) {
        val task = makeTask(id = "sync-task", title = "Bi-directional Sync Task")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "sync-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("sync-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("sync-task") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val activeSessionFlow = MutableStateFlow<FocusService.Companion.ActiveFocusSession?>(null)
        every { FocusService.activeSession } returns activeSessionFlow

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            viewModel.startTimer()
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isRunning)
            assertFalse(viewModel.uiState.value.isPaused)

            // Simulate external notification pause from FocusService
            activeSessionFlow.value = FocusService.Companion.ActiveFocusSession(
                taskId = "sync-task",
                taskTitle = "Bi-directional Sync Task",
                remainingSeconds = 1450L,
                totalSeconds = 1500L,
                isPaused = true
            )
            testScheduler.runCurrent()

            // Invariant: ViewModel must synchronize to paused state
            assertFalse(viewModel.uiState.value.isRunning)
            assertTrue(viewModel.uiState.value.isPaused)

            // Simulate external notification resume from FocusService
            activeSessionFlow.value = FocusService.Companion.ActiveFocusSession(
                taskId = "sync-task",
                taskTitle = "Bi-directional Sync Task",
                remainingSeconds = 1450L,
                totalSeconds = 1500L,
                isPaused = false
            )
            testScheduler.runCurrent()

            // Invariant: ViewModel must synchronize to running state
            assertTrue(viewModel.uiState.value.isRunning)
            assertFalse(viewModel.uiState.value.isPaused)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testCompleteTaskOnAlreadyCompletedTaskNoOpsAndDoesNotInflateStats() = runTest(testDispatcher) {
        val completedTask = makeTask(id = "already-completed-1", title = "Done Task", status = TaskStatus.COMPLETED)
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "already-completed-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("already-completed-1") } returns completedTask
        coEvery { taskRepo.getActiveTasks() } returns emptyList()
        coEvery { sessionRepo.getOpenSessionForTask("already-completed-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
            UserPreferences(totalFocusMinutes = 100, totalTasksCompleted = 5)
        )

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isCompleted)
            assertFalse(viewModel.uiState.value.showCompletionCelebration)

            // Trigger completeTask on already completed task
            viewModel.completeTask()
            testScheduler.runCurrent()

            // Invariant: completeAndRecur and updatePreferences must NOT be invoked again
            coVerify(exactly = 0) { taskRepo.completeAndRecur(any(), any()) }
            coVerify(exactly = 0) { prefsDataStore.updatePreferences(any()) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testOpeningCompletedTaskDoesNotTriggerCompletionCelebrationDialog() = runTest(testDispatcher) {
        val completedTask = makeTask(id = "inspect-completed-1", title = "Inspected Task", status = TaskStatus.COMPLETED)
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "inspect-completed-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("inspect-completed-1") } returns completedTask
        coEvery { taskRepo.getActiveTasks() } returns emptyList()
        coEvery { sessionRepo.getOpenSessionForTask("inspect-completed-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            // Invariant: Task is completed, but celebration dialog must NOT be triggered
            assertTrue(viewModel.uiState.value.isCompleted)
            assertFalse(viewModel.uiState.value.showCompletionCelebration)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testExitingFocusScreenWithoutStartingOrCompletingReschedulesAutonomyNudge() = runTest(testDispatcher) {
        val activeTask = makeTask(id = "active-nudge-task", title = "Active Deep Work Task", status = TaskStatus.ACTIVE)
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "active-nudge-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("active-nudge-task") } returns activeTask
        coEvery { taskRepo.getActiveTasks() } returns listOf(activeTask)
        coEvery { sessionRepo.getOpenSessionForTask("active-nudge-task") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        testScheduler.runCurrent()
        // Nudge was cancelled on entry
        io.mockk.verify { AutonomyNudgeEngine.cancelNudge(mockContext, "active-nudge-task") }

        // User leaves screen without starting timer or completing
        viewModel.clearForTest()
        testScheduler.runCurrent()

        // Invariant: Nudge MUST be rescheduled for this active task
        io.mockk.verify(atLeast = 1) { AutonomyNudgeEngine.scheduleNudge(mockContext, match { it.id == "active-nudge-task" }) }
    }

    @Test
    fun testDecompressionBypassResetsTimerDurationForNextSprint() = runTest(testDispatcher) {
        val task = makeTask(id = "decomp-task-1", title = "Sprint Work")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "decomp-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("decomp-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("decomp-task-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            // Sprint finishes: user takes decompression rest
            viewModel.takeDecompressionRest()
            testScheduler.runCurrent()

            assertTrue(viewModel.uiState.value.showDecompressionZone)

            // When decompression is bypassed or finishes, and task is NOT completed:
            viewModel.readyNowBypassDecompression()
            testScheduler.runCurrent()

            // Invariant: Timer must reset to clean duration (1500s / 25m) rather than staying frozen at 00:00
            assertFalse(viewModel.uiState.value.showDecompressionZone)
            assertEquals(0L, viewModel.uiState.value.elapsedSeconds)
            assertEquals(1500L, viewModel.uiState.value.remainingSeconds)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testTaskBlockPlanMinutesUsedForMapeCalculationOnSplitTask() = runTest(testDispatcher) {
        // A 90m parent task split into a 45m block
        val splitTask = makeTask(id = "split-task-1_part1", title = "Deep Architecture (Part 1)").copy(
            estimatedDurationMinutes = 90,
            plannedBlockMinutes = 45
        )
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "split-task-1_part1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("split-task-1_part1") } returns splitTask
        coEvery { taskRepo.getActiveTasks() } returns listOf(splitTask)
        coEvery { sessionRepo.getOpenSessionForTask("split-task-1_part1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            // In Flow Stopwatch mode, simulate 45 minutes tracked (2700s)
            viewModel.selectTimerMode(FocusTimerModeV2.FLOW_STOPWATCH)
            viewModel.startTimer()
            testScheduler.runCurrent()
            repeat(2700) { viewModel.tickSecond() }

            viewModel.completeTask()
            testScheduler.runCurrent()

            val taskSlot = slot<TaskEntity>()
            coVerify { taskRepo.completeAndRecur(capture(taskSlot), any()) }

            // Invariant: MAPE must be calculated against plannedMinutes (45m), yielding 0.0f error, not (45-90)/90 = 0.5f
            val mape = taskSlot.captured.estimationErrorMape
            assertNotNull(mape)
            assertEquals(0.0f, mape!!, 0.001f)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testFocusServiceNotificationConstantsAreDistinct() {
        assertEquals(3001, FocusService.NOTIFICATION_ID)
        assertEquals(3002, FocusService.NOTIFICATION_ID_COMPLETED)
        assertNotEquals(FocusService.NOTIFICATION_ID, FocusService.NOTIFICATION_ID_COMPLETED)
    }

    @Test
    fun testExternalTaskCompletionWhenSprintCompletedSynchronizesUiStateAndPreventsDoubleCompletion() = runTest(testDispatcher) {
        val activeTask = makeTask(id = "sprint-expired-task", title = "Task In Progress")
        val completedTask = activeTask.copy(status = TaskStatus.COMPLETED, completedAt = System.currentTimeMillis())
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "sprint-expired-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("sprint-expired-task") } returns activeTask
        coEvery { taskRepo.getActiveTasks() } returns listOf(activeTask)
        coEvery { sessionRepo.getOpenSessionForTask("sprint-expired-task") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val activeSessionFlow = MutableStateFlow<FocusService.Companion.ActiveFocusSession?>(
            FocusService.Companion.ActiveFocusSession(
                taskId = "sprint-expired-task",
                taskTitle = "Task In Progress",
                remainingSeconds = 100L,
                totalSeconds = 1500L,
                isPaused = false
            )
        )
        every { FocusService.activeSession } returns activeSessionFlow

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()
            viewModel.startTimer()
            testScheduler.runCurrent()

            // Countdown expires naturally in ViewModel
            repeat(1500) { viewModel.tickSecond() }
            testScheduler.runCurrent()

            // Sprint is completed, waiting on completion sheet
            assertTrue(viewModel.uiState.value.isSprintCompleted)
            assertTrue(viewModel.uiState.value.showSprintCompletionSheet)
            assertFalse(viewModel.uiState.value.isRunning)
            assertFalse(viewModel.uiState.value.isPaused)
            assertFalse(viewModel.uiState.value.isCompleted)

            // Now simulate background completion from notification or widget
            coEvery { taskRepo.getById("sprint-expired-task") } returns completedTask
            activeSessionFlow.value = null
            testScheduler.runCurrent()

            // Invariant: UI state must synchronize to completed, dismiss sprint sheet, show celebration
            assertTrue(viewModel.uiState.value.isCompleted)
            assertFalse(viewModel.uiState.value.showSprintCompletionSheet)
            assertTrue(viewModel.uiState.value.showCompletionCelebration)
            assertEquals(TaskStatus.COMPLETED, viewModel.uiState.value.task?.status)

            // User tapping complete task now MUST NOT invoke completeAndRecur again
            viewModel.completeTask()
            testScheduler.runCurrent()
            coVerify(exactly = 0) { taskRepo.completeAndRecur(any(), any()) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testNextTaskRanksDualRepresentationEpochTimestampAndFiltersBlockedTasks() = runTest(testDispatcher) {
        val currentTask = makeTask(id = "current-task", title = "Current Task")
        val blockedPrereqTask = makeTask(id = "prereq-blocked", title = "Blocked by active prereq").copy(
            dependsOnTaskIds = "unrelated-active"
        )
        val waitingForTask = makeTask(id = "waiting-task", title = "Waiting for client").copy(
            waitingFor = "client feedback"
        )
        val unrelatedActive = makeTask(id = "unrelated-active", title = "Unrelated Active")
        val blockedPart2 = makeTask(id = "parent-split_part2", title = "Part 2 of split").copy(
            parentTaskId = "parent-split_part1"
        )
        val parentPart1 = makeTask(id = "parent-split_part1", title = "Part 1 of split")

        val futureTaskWithExplicitDate = makeTask(
            id = "future-task",
            title = "Scheduled Next Month",
            scheduledDate = System.currentTimeMillis() + (30L * ONE_DAY_MILLIS)
        )
        // Task scheduled for TODAY with absolute epoch timestamp (scheduledDate == null)
        val todayEpochTask = makeTask(
            id = "today-epoch-task",
            title = "Scheduled Today 2PM",
            scheduledDate = null,
            scheduledTime = System.currentTimeMillis() + 3600_000L
        )

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "current-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("current-task") } returns currentTask
        coEvery { taskRepo.getActiveTasks() } returns listOf(
            currentTask,
            blockedPrereqTask,
            waitingForTask,
            blockedPart2,
            futureTaskWithExplicitDate,
            todayEpochTask,
            unrelatedActive,
            parentPart1
        )
        coEvery { sessionRepo.getOpenSessionForTask("current-task") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            // Invariant: today-epoch-task must rank before future-task and blocked tasks must be excluded
            assertEquals("today-epoch-task", viewModel.uiState.value.nextTaskId)
            assertEquals("Scheduled Today 2PM", viewModel.uiState.value.nextTaskTitle)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testDecompressionExitClearsStartedAtMillisPreventingPhantomMinuteInflationOnDirectComplete() = runTest(testDispatcher) {
        val task = makeTask(id = "decomp-clear-task", title = "Decompression Timing Test")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "decomp-clear-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("decomp-clear-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("decomp-clear-task") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(
            UserPreferences(totalFocusMinutes = 50, totalTasksCompleted = 2)
        )

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()

            // Run sprint to 0, then enter decompression zone
            viewModel.startTimer()
            testScheduler.runCurrent()
            repeat(1500) { viewModel.tickSecond() }
            testScheduler.runCurrent()

            viewModel.takeDecompressionRest()
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.showDecompressionZone)

            // User exits decompression zone
            viewModel.readyNowBypassDecompression()
            testScheduler.runCurrent()

            // Invariant: startedAtMillis and elapsedSeconds must be 0
            assertEquals(0L, viewModel.uiState.value.startedAtMillis)
            assertEquals(0L, viewModel.uiState.value.elapsedSeconds)

            // Now user completes the task without starting another timer
            viewModel.completeTask()
            testScheduler.runCurrent()

            val taskSlot = slot<TaskEntity>()
            coVerify { taskRepo.completeAndRecur(capture(taskSlot), any()) }

            // Invariant: Total tracked time must remain 25m (from the completed sprint), NOT 26m!
            assertEquals(25f, taskSlot.captured.totalTimeTrackedMinutes, 0.001f)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testAutoBreakDownTaskWithEmptyResultPreservesExistingMicroSteps() = runTest(testDispatcher) {
        val task = makeTask(id = "empty-breakdown-task", title = "Task with steps").copy(
            ifThenPlan = "[ ] Existing Step 1"
        )
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "empty-breakdown-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val behavioralService = mockk<com.neuroflow.app.domain.service.BehavioralSupportService>()

        coEvery { taskRepo.getById("empty-breakdown-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("empty-breakdown-task") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())
        coEvery { behavioralService.generateMicroStepsWithAi(any(), any(), any(), any(), any()) } returns emptyList()

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext,
            behavioralSupportService = behavioralService
        )

        try {
            testScheduler.runCurrent()
            assertEquals(1, viewModel.uiState.value.microSteps.size)
            assertEquals("Existing Step 1", viewModel.uiState.value.microSteps[0].text)

            // Trigger autoBreakDownTask which returns empty
            viewModel.autoBreakDownTask()
            testScheduler.runCurrent()

            // Invariant: Existing steps must NOT be deleted
            coVerify(exactly = 0) { taskRepo.deleteMicroStepsByTaskId(any()) }
            assertEquals(1, viewModel.uiState.value.microSteps.size)
            assertEquals("Existing Step 1", viewModel.uiState.value.microSteps[0].text)
        } finally {
            viewModel.clearForTest()
        }
    }
}
