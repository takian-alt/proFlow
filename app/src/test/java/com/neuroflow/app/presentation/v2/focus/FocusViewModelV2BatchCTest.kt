package com.neuroflow.app.presentation.v2.focus

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import com.neuroflow.app.worker.TaskReminderWorker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelV2BatchCTest {

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
        every { FocusService.activeSession } returns MutableStateFlow(null)
        every { TaskReminderWorker.cancelReminder(any<Context>(), any<String>()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(AutonomyNudgeEngine)
        unmockkObject(FocusService)
        unmockkObject(TaskReminderWorker)
        Dispatchers.resetMain()
    }

    @Test
    fun `savedStateHandle mode JUST_5_MIN initializes 2m-5m doorway mode when no open session exists`() = runTest(testDispatcher) {
        val task = TaskEntity(id = "doorway-1", title = "Start Tax Docs")
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("doorway-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("doorway-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = SavedStateHandle(mapOf("taskId" to "doorway-1", "mode" to "JUST_5_MIN")),
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext
        )

        try {
            testScheduler.runCurrent()
            assertEquals(FocusTimerModeV2.JUST_5_MIN, viewModel.uiState.value.timerMode)
            assertEquals(300L, viewModel.uiState.value.remainingSeconds)
        } finally {
            viewModel.clearForTest()
        }
    }

    @Test
    fun `toggleBodyDoubling flips flag and two minute transition cue fires once per countdown`() = runTest(testDispatcher) {
        val task = TaskEntity(id = "cue-task-1", title = "Design Review")
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val woopRepo = mockk<WoopRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val notifier = mockk<FocusCompletionNotifier>(relaxed = true)

        coEvery { taskRepo.getById("cue-task-1") } returns task
        coEvery { taskRepo.getActiveTasks() } returns listOf(task)
        coEvery { sessionRepo.getOpenSessionForTask("cue-task-1") } returns null
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

        val viewModel = FocusViewModelV2(
            savedStateHandle = SavedStateHandle(mapOf("taskId" to "cue-task-1")),
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            woopRepository = woopRepo,
            preferencesDataStore = prefsDataStore,
            context = mockContext,
            focusCompletionNotifier = notifier
        )

        try {
            testScheduler.runCurrent()

            assertFalse(viewModel.uiState.value.bodyDoublingEnabled)
            viewModel.toggleBodyDoubling()
            assertTrue(viewModel.uiState.value.bodyDoublingEnabled)
            viewModel.toggleBodyDoubling()
            assertFalse(viewModel.uiState.value.bodyDoublingEnabled)

            viewModel.selectTimerMode(FocusTimerModeV2.POMODORO_25)
            viewModel.startTimer()
            testScheduler.runCurrent()

            // First 1379 seconds (remaining = 121s): warning cue must not fire yet
            repeat(1379) { viewModel.tickSecond() }
            assertEquals(121L, viewModel.uiState.value.remainingSeconds)
            verify(exactly = 0) { notifier.playTwoMinuteWarningCue(any()) }

            // Crossing into 120s remaining fires cue once, and subsequent ticks down to 1s do not re-trigger
            repeat(5) { viewModel.tickSecond() }
            assertEquals(116L, viewModel.uiState.value.remainingSeconds)
            verify(exactly = 1) { notifier.playTwoMinuteWarningCue(any()) }
        } finally {
            viewModel.clearForTest()
        }
    }
}
