package com.neuroflow.app.presentation.schedule

import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.TaskRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelAutoScheduleTest : StringSpec({
    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    "Auto reflects enabled-by-default scheduler and turning it off disables worker scheduling" {
        val preferenceState = MutableStateFlow(UserPreferences())
        val preferencesDataStore = mockk<UserPreferencesDataStore>()
        every { preferencesDataStore.preferencesFlow } returns preferenceState
        coEvery { preferencesDataStore.updatePreferences(any()) } coAnswers {
            val update = firstArg<(UserPreferences) -> UserPreferences>()
            preferenceState.value = update(preferenceState.value)
        }

        val taskRepository = mockk<TaskRepository>()
        every { taskRepository.observeActiveTasks() } returns flowOf(emptyList())
        every { taskRepository.observeTasksForDate(any()) } returns flowOf(emptyList())

        val viewModel = ScheduleViewModel(
            taskRepository = taskRepository,
            preferencesDataStore = preferencesDataStore,
            workManager = mockk<WorkManager>(relaxed = true)
        )

        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.value.isAutoScheduleOn shouldBe true

        viewModel.toggleAutoSchedule()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.value.isAutoScheduleOn shouldBe false
        preferenceState.value.autoSchedulingEnabled shouldBe false
        preferenceState.value.isAutoScheduleToggleOn shouldBe false
    }
})
