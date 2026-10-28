package com.neuroflow.app.presentation.v2.insights

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.domain.model.EnergyLevel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsDateOnlyEvaluationTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    "InsightsViewModelV2 excludes date-only untimed tasks from circadian alignment check" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val sessionRepo = mockk<SessionRepository>(relaxed = true)
            val contractRepo = mockk<UlyssesContractRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            val prefs = UserPreferences(
                peakEnergyStart = 9,
                peakEnergyEnd = 12
            )
            val now = System.currentTimeMillis()
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)

            // High energy task 1: Untimed date-only task (scheduledDate set, scheduledTime = null)
            val dateOnlyTask = TaskEntity(
                id = "date-only-1",
                title = "Date Only High Energy Task",
                energyLevel = EnergyLevel.HIGH,
                scheduledDate = todayStart,
                scheduledTime = null,
                completedAt = null
            )
            // High energy task 2: Timed at 10:00 AM (inside 9-12 peak)
            val alignedTask = TaskEntity(
                id = "aligned-1",
                title = "Peak Window Task",
                energyLevel = EnergyLevel.HIGH,
                scheduledDate = todayStart,
                scheduledTime = 10 * 3600_000L
            )

            every { taskRepo.observeAll() } returns MutableStateFlow(listOf(dateOnlyTask, alignedTask))
            coEvery { taskRepo.getAllTasks() } returns listOf(dateOnlyTask, alignedTask)
            every { sessionRepo.observeAll() } returns MutableStateFlow(emptyList())
            every { contractRepo.observeActive() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(prefs)

            val viewModel = InsightsViewModelV2(
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                preferencesDataStore = prefsDataStore,
                contractRepository = contractRepo
            )
            advanceUntilIdle()

            val state = viewModel.uiState.filter { !it.isLoading }.first()
            // Exactly 1 timed task evaluated (alignedTask). 1 of 1 = 100%!
            // If dateOnlyTask was evaluated at 00:00 midnight, it would have failed and reported 50%.
            state.circadianAlignmentRate shouldBe 100
        }
    }
})
