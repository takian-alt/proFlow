package com.neuroflow.app.presentation.v2.plan

import androidx.work.WorkManager
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.SlotRemedyResult
import com.neuroflow.app.domain.service.TaskSchedulerService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulingFeedbackViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)
    private val schedulerService = mockk<TaskSchedulerService>(relaxed = true)
    private val circadianService: CircadianEnergyService = DefaultCircadianEnergyService()
    private val geminiAiService = mockk<GeminiAiOnlineService>(relaxed = true)

    private val defaultPrefs = UserPreferences(
        autoSchedulingEnabled = true,
        dailyDeepWorkTargetMinutes = 240,
        workDayStart = 8,
        workDayEnd = 18,
        geminiApiKey = "test-api-key",
        adaptiveSystemDirective = "Initial directive"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { preferencesDataStore.preferencesFlow } returns flowOf(defaultPrefs)
        every { taskRepository.observeActiveTasks() } returns flowOf(emptyList())
        every { taskRepository.observeCompletedTasks() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(): PlanViewModelV2 {
        val vm = PlanViewModelV2(
            taskRepository = taskRepository,
            preferencesDataStore = preferencesDataStore,
            workManager = workManager,
            circadianEnergyService = circadianService,
            taskSchedulerService = schedulerService,
            calendarSyncRepository = calendarRepo,
            geminiAiOnlineService = geminiAiService
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }
        return vm
    }

    @Test
    fun testRatePlacement_negativeRating_opensFeedbackDialog() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val task = TaskEntity(
            id = "task-1",
            title = "Code Review",
            scheduledDate = 1000L,
            scheduledTime = 36_000_000L,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        )

        viewModel.ratePlacement(task, rating = -1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.feedbackTask)
        assertEquals("task-1", state.feedbackTask?.id)
        assertNull(state.feedbackRemedyProposal)
        assertFalse(state.isFeedbackLoading)
    }

    @Test
    fun testSubmitDetailedFeedback_requestsRemedyAndUpdatesPreferences() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val task = TaskEntity(
            id = "task-1",
            title = "Write Architecture Spec",
            scheduledDate = 1000L,
            scheduledTime = 36_000_000L,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.SCHEDULE
        )

        val remedyProposal = SlotRemedyResult(
            recommendedStartMillis = 50_400_000L,
            reasoning = "Better fit in afternoon peak",
            directivePatch = "Prefer afternoon for deep coding"
        )

        coEvery {
            geminiAiService.remedyRejectedSlot(
                task = any(),
                rejectedStartHour = any(),
                reasonTag = any(),
                availableWindows = any(),
                apiKey = any(),
                userDirective = any()
            )
        } returns remedyProposal

        viewModel.submitDetailedFeedback(task, reasonTag = "TOO_LOW_ENERGY")
        advanceUntilIdle()

        // Verify preferences updated with reason tag
        coVerify {
            preferencesDataStore.recordSchedulingFeedback(
                category = any(),
                hour = any(),
                isNegative = true,
                timestampMillis = any(),
                reasonTag = "TOO_LOW_ENERGY"
            )
        }

        // Verify remedy proposal in state
        val state = viewModel.uiState.value
        assertEquals(remedyProposal, state.feedbackRemedyProposal)
        assertFalse(state.isFeedbackLoading)
    }

    @Test
    fun testAcceptRemedySlot_updatesTaskWithScheduleLockAndDirective() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val task = TaskEntity(
            id = "task-1",
            title = "Write Architecture Spec",
            scheduledDate = 1000L,
            scheduledTime = 36_000_000L,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.SCHEDULE
        )

        viewModel.acceptRemedySlot(
            task = task,
            newSlotMillis = 50_400_000L,
            directivePatch = "Prefer afternoon for deep coding"
        )
        advanceUntilIdle()

        // Verify task updated with new scheduledTime and isScheduleLocked = true
        coVerify {
            taskRepository.update(
                match {
                    it.id == "task-1" &&
                        it.scheduledTime == 50_400_000L &&
                        it.isScheduleLocked
                }
            )
        }

        // Verify directive patch merged
        coVerify {
            preferencesDataStore.updateAdaptiveSystemDirective(
                directive = match { it.contains("Prefer afternoon for deep coding") },
                auditJson = any()
            )
        }

        // Verify feedback state cleared
        val state = viewModel.uiState.value
        assertNull(state.feedbackTask)
        assertNull(state.feedbackRemedyProposal)
        assertFalse(state.isFeedbackLoading)
    }

    @Test
    fun testDismissFeedbackDialog_clearsFeedbackState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val task = TaskEntity(
            id = "task-1",
            title = "Code Review",
            scheduledDate = 1000L,
            scheduledTime = 36_000_000L
        )

        viewModel.ratePlacement(task, rating = -1)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.feedbackTask)

        viewModel.dismissFeedbackDialog()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.feedbackTask)
        assertNull(state.feedbackRemedyProposal)
        assertFalse(state.isFeedbackLoading)
    }

    @Test
    fun testFormatRemedyTime_formatsDayMillisAndEpochMillis() {
        // 14:30 in day millis = (14 * 60 + 30) * 60_000L = 52_200_000L
        val formattedDayMillis = formatRemedyTime(52_200_000L)
        assertEquals("02:30 PM", formattedDayMillis)

        // 09:15 in day millis = (9 * 60 + 15) * 60_000L = 33_300_000L
        val morningFormatted = formatRemedyTime(33_300_000L)
        assertEquals("09:15 AM", morningFormatted)
    }
}
