package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.GoalDao
import com.neuroflow.app.data.local.entity.GoalEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.flowOf

class GoalRepositoryTest : StringSpec({

    val goalDao = mockk<GoalDao>(relaxed = true)
    val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    val repository = GoalRepository(goalDao, preferencesDataStore)

    beforeTest {
        clearMocks(goalDao, preferencesDataStore)
    }

    "checkAndMigrateLegacyGoals does nothing when active goals already exist and markers are initialized" {
        coEvery { goalDao.getActiveGoals() } returns listOf(
            GoalEntity(id = "existing-1", title = "Existing Objective")
        )
        every { preferencesDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                lastYearlyGoalHandledYear = 2026,
                lastWeeklyGoalHandledWeek = 202642,
                yearlyGoals = emptyList(),
                weeklyGoals = emptyList()
            )
        )

        repository.checkAndMigrateLegacyGoals()

        coVerify(exactly = 0) { goalDao.insert(any()) }
        coVerify(exactly = 0) { preferencesDataStore.clearGoalStringsAndMarkHandled(any(), any()) }
    }

    "checkAndMigrateLegacyGoals initializes markers when active goals exist but markers are zero" {
        coEvery { goalDao.getActiveGoals() } returns listOf(
            GoalEntity(id = "existing-1", title = "Existing Objective")
        )
        every { preferencesDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                lastYearlyGoalHandledYear = 0,
                lastWeeklyGoalHandledWeek = 0
            )
        )
        coEvery { preferencesDataStore.clearGoalStringsAndMarkHandled(any(), any()) } returns Unit

        repository.checkAndMigrateLegacyGoals()

        coVerify(exactly = 0) { goalDao.insert(any()) }
        coVerify(exactly = 1) { preferencesDataStore.clearGoalStringsAndMarkHandled(any(), any()) }
    }

    "checkAndMigrateLegacyGoals migrates yearly and weekly goals, marks handled, and clears DataStore strings" {
        coEvery { goalDao.getActiveGoals() } returns emptyList()
        every { preferencesDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                yearlyGoals = listOf("Run marathon", "Launch neuroFlow"),
                weeklyGoals = listOf("Ship v2.0.0")
            )
        )
        coEvery { goalDao.insert(any()) } returns Unit
        coEvery { preferencesDataStore.clearGoalStringsAndMarkHandled(any(), any()) } returns Unit

        repository.checkAndMigrateLegacyGoals()

        val capturedGoals = mutableListOf<GoalEntity>()
        coVerify(exactly = 3) { goalDao.insert(capture(capturedGoals)) }
        capturedGoals.map { it.title } shouldBe listOf("Run marathon", "Launch neuroFlow", "Ship v2.0.0")
        capturedGoals.map { it.description } shouldBe listOf("Yearly Objective", "Yearly Objective", "Weekly Objective")

        coVerify(exactly = 1) { preferencesDataStore.clearGoalStringsAndMarkHandled(any(), any()) }
    }

    "checkAndMigrateLegacyGoals does nothing when legacy DataStore goals are empty" {
        coEvery { goalDao.getActiveGoals() } returns emptyList()
        every { preferencesDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                yearlyGoals = emptyList(),
                weeklyGoals = emptyList()
            )
        )

        repository.checkAndMigrateLegacyGoals()

        coVerify(exactly = 0) { goalDao.insert(any()) }
        coVerify(exactly = 0) { preferencesDataStore.clearGoalStringsAndMarkHandled(any(), any()) }
    }

    "delegation methods forward to GoalDao" {
        val testGoal = GoalEntity(id = "g-1", title = "Deep Work")
        coEvery { goalDao.getById("g-1") } returns testGoal
        coEvery { goalDao.getActiveGoals() } returns listOf(testGoal)

        repository.getById("g-1") shouldBe testGoal
        repository.getActiveGoals() shouldBe listOf(testGoal)

        repository.insert(testGoal)
        coVerify(exactly = 1) { goalDao.insert(testGoal) }

        repository.update(testGoal)
        coVerify(exactly = 1) { goalDao.update(testGoal) }

        repository.delete(testGoal)
        coVerify(exactly = 1) { goalDao.delete(testGoal) }

        repository.deleteAll()
        coVerify(exactly = 1) { goalDao.deleteAll() }
    }
})
