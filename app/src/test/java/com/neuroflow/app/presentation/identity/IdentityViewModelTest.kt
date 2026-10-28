package com.neuroflow.app.presentation.identity

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.repository.GoalRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class IdentityViewModelTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    "BUG-154: updateWeeklyGoals deletes removed goals and inserts newly added goals in Room" {
        runTest(testDispatcher) {
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val goalRepo = mockk<GoalRepository>(relaxed = true)

            val prefsFlow = MutableStateFlow(UserPreferences())
            every { prefsDataStore.preferencesFlow } returns prefsFlow

            val existing1 = GoalEntity(id = "g1", title = "Study Kotlin", isActive = true)
            val existing2 = GoalEntity(id = "g2", title = "Read Book", isActive = true)
            val existing3 = GoalEntity(id = "g3", title = "Exercise", isActive = true)

            coEvery { goalRepo.getActiveGoals() } returns listOf(existing1, existing2, existing3)
            every { goalRepo.observeActiveGoals() } returns MutableStateFlow(listOf(existing1, existing2, existing3))

            val viewModel = IdentityViewModel(prefsDataStore, goalRepo)

            // User updates goals, deleting "Read Book" and adding "Ship Architecture"
            viewModel.updateWeeklyGoals(listOf("Study Kotlin", "Exercise", "Ship Architecture"))
            advanceUntilIdle()

            // Verify "Read Book" was deleted
            coVerify(exactly = 1) { goalRepo.delete(existing2) }
            // Verify retained goals were not deleted
            coVerify(exactly = 0) { goalRepo.delete(existing1) }
            coVerify(exactly = 0) { goalRepo.delete(existing3) }

            // Verify new goal was inserted
            val inserted = slot<GoalEntity>()
            coVerify(exactly = 1) { goalRepo.insert(capture(inserted)) }
            inserted.captured.title shouldBe "Ship Architecture"
        }
    }

    "BUG-158: updateWeeklyGoals deduplicates case-insensitive goals and preserves Room integrity" {
        runTest(testDispatcher) {
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val goalRepo = mockk<GoalRepository>(relaxed = true)

            coEvery { goalRepo.getActiveGoals() } returns emptyList()
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val viewModel = IdentityViewModel(prefsDataStore, goalRepo)
            viewModel.updateWeeklyGoals(listOf("Sleep Early", "sleep early", " SLEEP EARLY "))
            advanceUntilIdle()

            coVerify(exactly = 1) { goalRepo.insert(any()) }
        }
    }

    "BUG-180: updateWeeklyGoals preserves Yearly Objective goals and only reconciles Weekly Objectives" {
        runTest(testDispatcher) {
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val goalRepo = mockk<GoalRepository>(relaxed = true)

            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val yearlyGoal1 = GoalEntity(id = "yg1", title = "Master Distributed Systems", description = "Yearly Objective", isActive = true)
            val yearlyGoal2 = GoalEntity(id = "yg2", title = "Run Half-Marathon", description = "Yearly Objective", isActive = true)
            val weeklyGoal1 = GoalEntity(id = "wg1", title = "Finish Sprint 4", description = "Weekly Objective", isActive = true)
            val weeklyGoal2 = GoalEntity(id = "wg2", title = "Read Chapter 5", description = "Weekly Objective", isActive = true)

            coEvery { goalRepo.getActiveGoals() } returns listOf(yearlyGoal1, yearlyGoal2, weeklyGoal1, weeklyGoal2)
            every { goalRepo.observeActiveGoals() } returns MutableStateFlow(listOf(yearlyGoal1, yearlyGoal2, weeklyGoal1, weeklyGoal2))

            val viewModel = IdentityViewModel(prefsDataStore, goalRepo)

            // User updates weekly goals: keeps "Finish Sprint 4", removes "Read Chapter 5", adds "Draft ADR 035"
            viewModel.updateWeeklyGoals(listOf("Finish Sprint 4", "Draft ADR 035"))
            advanceUntilIdle()

            // Yearly goals MUST NOT be touched or deleted
            coVerify(exactly = 0) { goalRepo.delete(yearlyGoal1) }
            coVerify(exactly = 0) { goalRepo.delete(yearlyGoal2) }

            // Stale weekly goal was deleted
            coVerify(exactly = 1) { goalRepo.delete(weeklyGoal2) }
            // Retained weekly goal was preserved
            coVerify(exactly = 0) { goalRepo.delete(weeklyGoal1) }

            // New weekly goal was inserted with "Weekly Objective" description
            val inserted = slot<GoalEntity>()
            coVerify(exactly = 1) { goalRepo.insert(capture(inserted)) }
            inserted.captured.title shouldBe "Draft ADR 035"
            inserted.captured.description shouldBe "Weekly Objective"
        }
    }
})
