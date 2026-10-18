package com.neuroflow.app.presentation.v2.insights

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.domain.model.EnergyLevel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelV2Test : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    val taskRepository = mockk<TaskRepository>(relaxed = true)
    val sessionRepository = mockk<SessionRepository>(relaxed = true)
    val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    val contractRepository = mockk<UlyssesContractRepository>(relaxed = true)

    "computeSmapeAndCircadianDensity accurately calculates 7d and 30d velocity and focus hours" {
        val viewModel = InsightsViewModelV2(
            taskRepository = taskRepository,
            sessionRepository = sessionRepository,
            preferencesDataStore = preferencesDataStore,
            contractRepository = contractRepository
        )

        val now = 1_000_000_000L
        val dayMillis = 86400_000L

        val tasks = listOf(
            TaskEntity(
                id = "t1",
                title = "Task 3 days ago",
                completedAt = now - 3 * dayMillis,
                actualDurationMinutes = 60f,
                estimatedDurationMinutes = 50,
                energyLevel = EnergyLevel.HIGH
            ),
            TaskEntity(
                id = "t2",
                title = "Task 15 days ago",
                completedAt = now - 15 * dayMillis,
                actualDurationMinutes = 120f,
                estimatedDurationMinutes = 100,
                energyLevel = EnergyLevel.MEDIUM
            ),
            TaskEntity(
                id = "t3",
                title = "Task 40 days ago",
                completedAt = now - 40 * dayMillis,
                actualDurationMinutes = 90f,
                estimatedDurationMinutes = 90,
                energyLevel = EnergyLevel.LOW
            )
        )

        val metrics = viewModel.computeSmapeAndCircadianDensity(
            allTasks = tasks,
            prefs = UserPreferences(peakEnergyStart = 9, peakEnergyEnd = 12),
            now = now
        )

        // 7 days window: only t1 (3 days ago)
        metrics.velocity7d shouldBe 1
        metrics.focusHours7d shouldBe 1.0f

        // 30 days window: t1 and t2 (3 and 15 days ago)
        metrics.velocity30d shouldBe 2
        metrics.focusHours30d shouldBe 3.0f

        // SMAPE and grade should be computed
        metrics.weightedSmape shouldNotBe 0f
        metrics.smapeGrade shouldNotBe ""

        // Hourly density should have 24 hours
        metrics.hourlyDensity shouldHaveSize 24
    }

    "insightsMetricsFlow emits off-thread model upon data stream update" {
        val now = System.currentTimeMillis()
        val tasks = listOf(
            TaskEntity(
                id = "t1",
                title = "Completed today",
                completedAt = now - 1000L,
                actualDurationMinutes = 45f,
                estimatedDurationMinutes = 45
            )
        )
        every { taskRepository.observeAll() } returns flowOf(tasks)
        every { preferencesDataStore.preferencesFlow } returns flowOf(UserPreferences())

        val viewModel = InsightsViewModelV2(
            taskRepository = taskRepository,
            sessionRepository = sessionRepository,
            preferencesDataStore = preferencesDataStore,
            contractRepository = contractRepository
        )

        runTest(testDispatcher) {
            val metrics = viewModel.insightsMetricsFlow.filter { it.hourlyDensity.isNotEmpty() }.first()
            metrics.velocity7d shouldBe 1
            metrics.velocity30d shouldBe 1
            metrics.hourlyDensity shouldHaveSize 24
        }
    }
})
