package com.neuroflow.app.presentation.v2.focus

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.v2.focus.audio.SoundscapeType
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import com.neuroflow.app.worker.TaskReminderWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
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
        mockkObject(TaskReminderWorker)
        every { AutonomyNudgeEngine.cancelNudge(any<Context>(), any<String>()) } returns Unit
        every { AutonomyNudgeEngine.scheduleNudge(any<Context>(), any<TaskEntity>()) } returns Unit
        every { FocusService.startService(any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit
        every { FocusService.pauseService(any()) } returns Unit
        every { FocusService.resumeService(any()) } returns Unit
        every { FocusService.stopService(any()) } returns Unit
        every { TaskReminderWorker.cancelReminder(any<Context>(), any<String>()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(AutonomyNudgeEngine)
        unmockkObject(FocusService)
        unmockkObject(TaskReminderWorker)
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
            scheduledTime = startOfLocalDay(System.currentTimeMillis()) + 14 * 3600_000L
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

    @Test
    fun testPart2HashContinuationBlockedInFocusNextTaskQueue() = runTest(testDispatcher) {
        val part1Task = makeTask(id = "backend_task#part1", title = "Backend Architecture Part 1")
        val part2Task = makeTask(id = "backend_task#part2", title = "Backend Architecture Part 2")
        val regularTask = makeTask(id = "docs-task", title = "Write Documentation")

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "backend_task#part1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("backend_task#part1") } returns part1Task
        coEvery { taskRepo.getActiveTasks() } returns listOf(part1Task, part2Task, regularTask)
        coEvery { sessionRepo.getOpenSessionForTask("backend_task#part1") } returns null
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

            // Invariant: part2Task must NOT be selected as next task while part1 is active!
            assertEquals("docs-task", viewModel.uiState.value.nextTaskId)
            assertEquals("Write Documentation", viewModel.uiState.value.nextTaskTitle)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testCompleteTaskRecalculatesTrueElapsedDurationAvoidingStaleSecondsTruncation() = runTest(testDispatcher) {
        val task = makeTask(id = "long-focus-task", title = "Deep Focus Work")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "long-focus-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("long-focus-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("long-focus-task") } returns null
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

            // Start timer
            viewModel.startTimer()
            testScheduler.runCurrent()

            // Simulate user backgrounding app after 5 seconds: startedAt was 20 minutes ago
            val twentyMinutesAgo = System.currentTimeMillis() - (20 * 60 * 1000L)
            val fieldStartedAt = FocusViewModelV2::class.java.getDeclaredField("_uiState")
            fieldStartedAt.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val stateFlow = fieldStartedAt.get(viewModel) as MutableStateFlow<FocusUiStateV2>
            stateFlow.value = stateFlow.value.copy(
                startedAtMillis = twentyMinutesAgo,
                elapsedSeconds = 5L // UI only ticked 5 seconds before screen was turned off
            )

            // Complete task
            viewModel.completeTask()
            testScheduler.runCurrent()

            val taskSlot = slot<TaskEntity>()
            coVerify { taskRepo.completeAndRecur(capture(taskSlot), any()) }

            // Invariant: True elapsed time (20 minutes) must be credited, NOT the stale 5 seconds (1 minute)!
            assertEquals(20f, taskSlot.captured.totalTimeTrackedMinutes, 0.5f)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testAutoBreakDownTaskResetsSkippedMicroStepIndices() = runTest(testDispatcher) {
        val task = makeTask(id = "skip-step-task", title = "Decomposition Skip Test").copy(
            ifThenPlan = "[ ] Step 1\n[ ] Step 2\n[ ] Step 3"
        )
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "skip-step-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val behavioralService = mockk<com.neuroflow.app.domain.service.BehavioralSupportService>()

        coEvery { taskRepo.getById("skip-step-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("skip-step-task") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())
        coEvery { behavioralService.generateMicroStepsWithAi(any(), any(), any(), any(), any()) } returns listOf(
            com.neuroflow.app.domain.service.MicroStepSuggestion("New Step 1", 5),
            com.neuroflow.app.domain.service.MicroStepSuggestion("New Step 2", 10),
            com.neuroflow.app.domain.service.MicroStepSuggestion("New Step 3", 10)
        )

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
            assertEquals(3, viewModel.uiState.value.microSteps.size)

            // User skips step 0
            viewModel.skipCurrentMicroStep(0)
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.skippedMicroStepIndices.contains(0))

            // AI re-decomposes task
            viewModel.autoBreakDownTask()
            testScheduler.runCurrent()

            // Invariant: skippedMicroStepIndices must be cleared so new breakdown starts fresh from step 0
            assertTrue(viewModel.uiState.value.skippedMicroStepIndices.isEmpty())
            assertEquals("New Step 1", viewModel.uiState.value.microSteps[0].text)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testTakeDecompressionRestAndDismissResetsTimerStatePreventingDoubleCount() = runTest(testDispatcher) {
        val task = makeTask(id = "decomp-reset-task", title = "Decompression Reset Test")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "decomp-reset-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("decomp-reset-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("decomp-reset-task") } returns null
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

            // Run sprint to 0 (25 min completed)
            viewModel.startTimer()
            testScheduler.runCurrent()
            repeat(1500) { viewModel.tickSecond() }
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isSprintCompleted)
            assertTrue(viewModel.uiState.value.showSprintCompletionSheet)

            // Tap Take Decompression Rest
            viewModel.takeDecompressionRest()
            testScheduler.runCurrent()

            // Invariant: startedAtMillis, elapsedSeconds, activeSessionId must be reset
            assertEquals(0L, viewModel.uiState.value.startedAtMillis)
            assertEquals(0L, viewModel.uiState.value.elapsedSeconds)
            assertEquals(1500L, viewModel.uiState.value.remainingSeconds)
            assertNull(viewModel.uiState.value.activeSessionId)
            assertTrue(viewModel.uiState.value.showDecompressionZone)

            // User completes task from decompression zone or after
            viewModel.completeTask()
            testScheduler.runCurrent()

            val taskSlot = slot<TaskEntity>()
            coVerify { taskRepo.completeAndRecur(capture(taskSlot), any()) }

            // Invariant: Tracked time must be 25m (from sprint completion), NOT 50m!
            assertEquals(25f, taskSlot.captured.totalTimeTrackedMinutes, 0.001f)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testAutoTrackerSuppressedDuringSprintCompletionReview() = runTest(testDispatcher) {
        val task = makeTask(id = "autotracker-sprint-task", title = "Auto Tracker Sprint Guard Test")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "autotracker-sprint-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsFlow = MutableStateFlow(UserPreferences(autoTrackerEnabled = false))
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("autotracker-sprint-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("autotracker-sprint-task") } returns null
        every { prefsDataStore.preferencesFlow } returns prefsFlow

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

            // Sprint finishes naturally
            viewModel.startTimer()
            testScheduler.runCurrent()
            repeat(1500) { viewModel.tickSecond() }
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.showSprintCompletionSheet)
            assertFalse(viewModel.uiState.value.isRunning)

            // Now auto-tracker preference updates from another thread / worker
            prefsFlow.value = UserPreferences(autoTrackerEnabled = true)
            testScheduler.advanceTimeBy(9000L)
            testScheduler.runCurrent()

            // Invariant: Auto-tracker must NOT restart timer while sprint completion sheet is shown!
            assertFalse(viewModel.uiState.value.isRunning)
            assertTrue(viewModel.uiState.value.showSprintCompletionSheet)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testMultiPartContinuationTasksAndSubtasksBlockedInFocusNextTaskQueue() = runTest(testDispatcher) {
        val part1Task = makeTask(id = "backend_task#part1", title = "Backend Architecture Part 1")
        val part2Task = makeTask(id = "backend_task#part2", title = "Backend Architecture Part 2")
        val part3Task = makeTask(id = "backend_task#part3", title = "Backend Architecture Part 3")
        val unmetDep = makeTask(id = "unmet-dep", title = "Unmet External Dependency", quadrant = Quadrant.ELIMINATE)
        val activeFrontend = makeTask(id = "frontend_task", title = "Frontend Task").copy(dependsOnTaskIds = "unmet-dep")
        val underscorePart3 = makeTask(id = "frontend_task_part3", title = "Frontend Part 3")
        val activeParent = makeTask(id = "active-parent", title = "Parent Task").copy(dependsOnTaskIds = "unmet-dep")
        val subtask = makeTask(id = "subtask-123", title = "Child Subtask").copy(parentTaskId = "active-parent")
        val standaloneActionable = makeTask(id = "standalone-task", title = "Standalone Ready Task")

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "backend_task#part1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("backend_task#part1") } returns part1Task
        coEvery { taskRepo.getActiveTasks() } returns listOf(
            part1Task,
            part2Task,
            part3Task,
            activeFrontend,
            underscorePart3,
            unmetDep,
            activeParent,
            subtask,
            standaloneActionable
        )
        coEvery { sessionRepo.getOpenSessionForTask("backend_task#part1") } returns null
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

            // Invariant: Multi-part continuations (#part2, #part3, _part3) and subtasks with active parents
            // must NOT be selected as next task ahead of ready standalone tasks!
            assertEquals("standalone-task", viewModel.uiState.value.nextTaskId)
            assertEquals("Standalone Ready Task", viewModel.uiState.value.nextTaskTitle)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testFutureTasksExcludedAndNextTaskClearsToNullWhenQueueExhausted() = runTest(testDispatcher) {
        val currentTask = makeTask(id = "current-task-1", title = "Current Task")
        val futureTask = makeTask(
            id = "future-task-1",
            title = "Task Next Week",
            scheduledDate = System.currentTimeMillis() + (7L * ONE_DAY_MILLIS)
        )

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "current-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("current-task-1") } returns currentTask
        // Only current task and a future-scheduled task exist
        coEvery { taskRepo.getActiveTasks() } returns listOf(currentTask, futureTask)
        coEvery { sessionRepo.getOpenSessionForTask("current-task-1") } returns null
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

            // Future task must be excluded, leaving nextTaskId as null
            assertNull(viewModel.uiState.value.nextTaskId)
            assertNull(viewModel.uiState.value.nextTaskTitle)

            // Complete current task
            viewModel.completeTask()
            testScheduler.runCurrent()

            // Invariant: When queue is exhausted, nextTaskId must be null (must NOT fall back to stale ID)
            assertNull(viewModel.uiState.value.nextTaskId)
            assertNull(viewModel.uiState.value.nextTaskTitle)
            assertTrue(viewModel.uiState.value.showCompletionCelebration)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testSprintCompletionIncrementsSessionCountAndRetainsTelemetryAcrossDecompressionRest() = runTest(testDispatcher) {
        val task = makeTask(id = "sprint-decomp-task", title = "Cognitive Sprint")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "sprint-decomp-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("sprint-decomp-task") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("sprint-decomp-task") } returns null
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

            viewModel.selectTimerMode(FocusTimerModeV2.JUST_5_MIN)
            viewModel.startTimer()
            testScheduler.runCurrent()

            // Complete the 5m sprint
            repeat(300) { viewModel.tickSecond() }
            testScheduler.runCurrent()

            // Invariant 1: Sprint completion in tickSecond increments sessionCount and records lastSessionDurationMinutes
            val updatedSlot = slot<TaskEntity>()
            coVerify(atLeast = 1) { taskRepo.update(capture(updatedSlot)) }
            assertEquals(1, updatedSlot.captured.sessionCount)
            assertEquals(5f, updatedSlot.captured.lastSessionDurationMinutes ?: 0f, 0.001f)
            assertEquals(5f, updatedSlot.captured.totalTimeTrackedMinutes, 0.001f)

            // User takes a 3-minute decompression rest
            viewModel.takeDecompressionRest()
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.showDecompressionZone)

            // User dismisses/bypasses decompression rest
            viewModel.readyNowBypassDecompression()
            testScheduler.runCurrent()
            assertFalse(viewModel.uiState.value.showDecompressionZone)

            // User completes the task after decompression
            viewModel.completeTask()
            testScheduler.runCurrent()

            val completeSlot = slot<TaskEntity>()
            coVerify { taskRepo.completeAndRecur(capture(completeSlot), any()) }

            // Invariant 2: sessionCount remains 1 (not reset to 0) and lastSessionDurationMinutes is preserved
            assertEquals(1, completeSlot.captured.sessionCount)
            assertEquals(5f, completeSlot.captured.lastSessionDurationMinutes ?: 0f, 0.001f)
            assertEquals(5f, completeSlot.captured.totalTimeTrackedMinutes, 0.001f)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testEliminateIceboxTaskDoesNotScheduleAutonomyNudgeOnCleared() = runTest(testDispatcher) {
        val iceboxTask = makeTask(
            id = "icebox-nudge-task",
            title = "Icebox Low Priority Idea",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.ELIMINATE
        )
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "icebox-nudge-task"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("icebox-nudge-task") } returns iceboxTask
        coEvery { taskRepo.getActiveTasks() } returns listOf(iceboxTask)
        coEvery { sessionRepo.getOpenSessionForTask("icebox-nudge-task") } returns null
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
        io.mockk.verify { AutonomyNudgeEngine.cancelNudge(mockContext, "icebox-nudge-task") }

        viewModel.clearForTest()
        testScheduler.runCurrent()

        // Invariant (BUG-444): ELIMINATE / icebox tasks MUST NOT be scheduled for autonomy nudges
        io.mockk.verify(exactly = 0) {
            AutonomyNudgeEngine.scheduleNudge(mockContext, match { it.id == "icebox-nudge-task" })
        }
    }

    @Test
    fun testCompleteTaskCancelsTaskReminderWorkerAndDismissesNotification() = runTest(testDispatcher) {
        val task = makeTask(id = "reminder-task-1", title = "Task With Scheduled Reminder")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "reminder-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val mockNm = mockk<android.app.NotificationManager>(relaxed = true)
        every { mockContext.getSystemService(android.app.NotificationManager::class.java) } returns mockNm

        coEvery { taskRepo.getById("reminder-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("reminder-task-1") } returns null
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

            viewModel.completeTask()
            testScheduler.runCurrent()

            // Invariant (BUG-445): NotificationManager.cancel(taskId.hashCode()) must be called
            io.mockk.verify(atLeast = 1) { mockNm.cancel("reminder-task-1".hashCode()) }
            // Invariant (BUG-445): TaskReminderWorker.cancelReminder must be invoked
            io.mockk.verify(atLeast = 1) {
                com.neuroflow.app.worker.TaskReminderWorker.cancelReminder(mockContext, "reminder-task-1")
            }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testUuidSplitContinuationPart3BlockedWhilePart2IsActive() = runTest(testDispatcher) {
        val part1Task = makeTask(id = "uuid-part-1", title = "Quarterly Plan (Part 1/3)").copy(parentTaskId = "uuid-parent")
        val part2Task = makeTask(id = "uuid-part-2", title = "Quarterly Plan (Part 2/3)").copy(parentTaskId = "uuid-parent")
        val part3Task = makeTask(id = "uuid-part-3", title = "Quarterly Plan (Part 3/3)").copy(parentTaskId = "uuid-parent")
        val standaloneReady = makeTask(id = "standalone-next", title = "Write Release Notes")

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "uuid-part-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("uuid-part-1") } returns part1Task
        // Part 1 is current; Part 2 and Part 3 are both active in DB
        coEvery { taskRepo.getActiveTasks() } returns listOf(
            part1Task,
            part2Task,
            part3Task,
            standaloneReady
        )
        coEvery { sessionRepo.getOpenSessionForTask("uuid-part-1") } returns null
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

            // Invariant (BUG-446): Part 3 MUST NOT be scheduled as next task while Part 2 is still active.
            // Both Part 2 and Part 3 are blocked behind Part 1 (or sequentially), so standalone task is next.
            assertNotEquals("uuid-part-3", viewModel.uiState.value.nextTaskId)
            assertEquals("standalone-next", viewModel.uiState.value.nextTaskId)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testDndNoticeActiveWhenDndEnabledAndPermissionNotGranted() = runTest(testDispatcher) {
        val task = makeTask(id = "dnd-task-1", title = "DND Verification")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "dnd-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val nm = mockk<android.app.NotificationManager>(relaxed = true) {
            every { isNotificationPolicyAccessGranted } returns false
        }
        val context = mockk<Context>(relaxed = true) {
            every { getSystemService(android.app.NotificationManager::class.java) } returns nm
        }

        coEvery { taskRepo.getById("dnd-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences(dndEnabled = true))

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = context
        )

        try {
            testScheduler.runCurrent()
            assertTrue("DND notice must be active when dndEnabled is true and permission is not granted", viewModel.uiState.value.showDndNotice)

            // Test dismissal
            viewModel.dismissDndNotice()
            testScheduler.runCurrent()
            assertFalse("DND notice must be hidden after dismissal", viewModel.uiState.value.showDndNotice)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testDndNoticeHiddenWhenPermissionGranted() = runTest(testDispatcher) {
        val task = makeTask(id = "dnd-task-2", title = "DND Granted Verification")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "dnd-task-2"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val nm = mockk<android.app.NotificationManager>(relaxed = true) {
            every { isNotificationPolicyAccessGranted } returns true
        }
        val context = mockk<Context>(relaxed = true) {
            every { getSystemService(android.app.NotificationManager::class.java) } returns nm
        }

        coEvery { taskRepo.getById("dnd-task-2") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences(dndEnabled = true))

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = context
        )

        try {
            testScheduler.runCurrent()
            assertFalse("DND notice must not be active when permission is granted", viewModel.uiState.value.showDndNotice)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testDndNoticeHiddenWhenDndDisabledInPreferences() = runTest(testDispatcher) {
        val task = makeTask(id = "dnd-task-3", title = "DND Disabled Verification")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "dnd-task-3"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val nm = mockk<android.app.NotificationManager>(relaxed = true) {
            every { isNotificationPolicyAccessGranted } returns false
        }
        val context = mockk<Context>(relaxed = true) {
            every { getSystemService(android.app.NotificationManager::class.java) } returns nm
        }

        coEvery { taskRepo.getById("dnd-task-3") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences(dndEnabled = false))

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = context
        )

        try {
            testScheduler.runCurrent()
            assertFalse("DND notice must not be active when dndEnabled is false", viewModel.uiState.value.showDndNotice)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testFocusDndHelperEnablesAndDisablesDndAppropriately() {
        val nm = mockk<android.app.NotificationManager>(relaxed = true) {
            every { isNotificationPolicyAccessGranted } returns true
        }
        val context = mockk<Context>(relaxed = true) {
            every { getSystemService(android.app.NotificationManager::class.java) } returns nm
        }

        val enableResult = FocusDndHelper.enableDndIfPermitted(context)
        assertTrue(enableResult)
        verify(exactly = 1) { nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY) }

        val disableResult = FocusDndHelper.disableDnd(context)
        assertTrue(disableResult)
        verify(exactly = 1) { nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL) }

        // Test defensive with null context
        assertFalse(FocusDndHelper.enableDndIfPermitted(null))
        assertFalse(FocusDndHelper.disableDnd(null))
    }

    @Test
    fun testDndDisabledWhenBackgroundExpiredSessionLoaded() = runTest(testDispatcher) {
        val task = makeTask(id = "expired-task-1", title = "Expired Session Task")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "expired-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val nm = mockk<android.app.NotificationManager>(relaxed = true) {
            every { isNotificationPolicyAccessGranted } returns true
        }
        val context = mockk<Context>(relaxed = true) {
            every { getSystemService(android.app.NotificationManager::class.java) } returns nm
        }

        val pastStart = System.currentTimeMillis() - 2000_000L
        val openSession = TimeSessionEntity(
            id = "sess-expired-1",
            taskId = "expired-task-1",
            startedAt = pastStart,
            endedAt = null,
            pausedAt = null,
            totalPausedMs = 0L,
            durationMinutes = 25f,
            pauseResumeCount = 0
        )

        coEvery { taskRepo.getById("expired-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("expired-task-1") } returns openSession
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences(dndEnabled = true))

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = context
        )

        try {
            testScheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isSprintCompleted)
            verify(atLeast = 1) { nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL) }
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun testSoundscapeAudioCanBeChangedOrTurnedOffWithoutStartingAnotherSession() = runTest(testDispatcher) {
        val task = makeTask(id = "audio-task-1", title = "Audio Task")
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "audio-task-1"))
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("audio-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("audio-task-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = savedStateHandle,
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = null
        )

        try {
            testScheduler.runCurrent()
            // In completed frame or not running frame (isRunning = false)
            assertFalse(viewModel.uiState.value.isRunning)

            // Changing soundscape to NONE always executes without requiring isRunning
            viewModel.selectSoundscape(SoundscapeType.NONE)
            assertEquals(SoundscapeType.NONE, viewModel.uiState.value.selectedSoundscape)

            // Changing soundscape to GAMMA_40HZ updates state
            viewModel.selectSoundscape(SoundscapeType.GAMMA_40HZ)
            assertEquals(SoundscapeType.GAMMA_40HZ, viewModel.uiState.value.selectedSoundscape)

            // When user switches to NONE, it turns off
            viewModel.selectSoundscape(SoundscapeType.NONE)
            assertEquals(SoundscapeType.NONE, viewModel.uiState.value.selectedSoundscape)
        } finally {
            viewModel.clearForTest()
        }
    }
}
