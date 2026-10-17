package com.neuroflow.app.presentation.settings

import android.content.Context
import com.neuroflow.app.data.local.DatabaseCleaner
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.domain.model.AppTheme
import com.neuroflow.app.domain.repository.EnergyMetricsRepository
import com.neuroflow.app.domain.repository.SleepPressureRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelPreferencePersistenceTest : StringSpec({
    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    fun viewModel(
        preferencesDataStore: UserPreferencesDataStore,
        sleepPressureRepository: SleepPressureRepository = mockk<SleepPressureRepository>(relaxed = true).apply {
            every { observeSleepLogs() } returns emptyFlow<List<SleepLogEntity>>()
        }
    ) = SettingsViewModel(
        context = mockk<Context>(relaxed = true),
        preferencesDataStore = preferencesDataStore,
        sleepPressureRepository = sleepPressureRepository,
        energyMetricsRepository = mockk<EnergyMetricsRepository>(relaxed = true),
        databaseCleaner = mockk<DatabaseCleaner>(relaxed = true)
    )

    "a settings edit keeps every field the screen has not observed yet" {
        val stored = UserPreferences(
            autoSchedulingEnabled = false,
            isAutoScheduleToggleOn = false,
            wakeUpHour = 6,
            sleepHour = 22,
            dailyStreak = 12,
            totalTasksCompleted = 40
        )
        val state = MutableStateFlow(stored)
        // The screen's flow has not delivered its first emission, so its snapshot is still
        // the all-defaults initialValue while the store holds real data.
        val unobservedFlow = MutableSharedFlow<UserPreferences>()
        val preferencesDataStore = mockk<UserPreferencesDataStore>().apply {
            every { preferencesFlow } returns unobservedFlow
            coEvery { updatePreferences(any()) } coAnswers {
                val update = firstArg<(UserPreferences) -> UserPreferences>()
                state.value = update(state.value)
            }
        }

        viewModel(preferencesDataStore).setTheme(AppTheme.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        state.value.theme shouldBe AppTheme.DARK
        state.value.autoSchedulingEnabled shouldBe false
        state.value.wakeUpHour shouldBe 6
        state.value.sleepHour shouldBe 22
        state.value.dailyStreak shouldBe 12
        state.value.totalTasksCompleted shouldBe 40
    }

    "a settings edit does not revert an auto-schedule toggle committed from another screen" {
        val staleSnapshot = UserPreferences(autoSchedulingEnabled = false, isAutoScheduleToggleOn = false)
        val published = MutableStateFlow(staleSnapshot)
        var stored = staleSnapshot.copy(autoSchedulingEnabled = true, isAutoScheduleToggleOn = true)

        val preferencesDataStore = mockk<UserPreferencesDataStore>().apply {
            every { preferencesFlow } returns published
            coEvery { updatePreferences(any()) } coAnswers {
                val update = firstArg<(UserPreferences) -> UserPreferences>()
                stored = update(stored)
            }
        }

        val viewModel = viewModel(preferencesDataStore)
        // Subscribe with the stale snapshot, then write as if another screen had since
        // committed the auto-schedule toggle to the store.
        val collectorScope = CoroutineScope(testDispatcher + Job())
        collectorScope.launch { viewModel.preferences.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setTheme(AppTheme.DARK)
        testDispatcher.scheduler.advanceUntilIdle()
        collectorScope.cancel()

        stored.autoSchedulingEnabled shouldBe true
        stored.isAutoScheduleToggleOn shouldBe true
        stored.theme shouldBe AppTheme.DARK
    }
})
