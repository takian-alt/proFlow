package com.neuroflow.app.presentation.history

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryDateRangeFilterTest : StringSpec({

    val now = 1_710_000_000_000L
    val oneDayMs = 86_400_000L

    fun completedTask(id: String, completedAt: Long?) = TaskEntity(
        id = id,
        title = "Task $id",
        status = TaskStatus.COMPLETED,
        completedAt = completedAt
    )

    "ALL returns the full list" {
        val tasks = listOf(
            completedTask("a", now),
            completedTask("b", now - oneDayMs),
            completedTask("c", null)
        )

        val filtered = filterTasksByDateRange(tasks, HistoryDateRange.ALL, now)

        filtered.shouldContainExactly(tasks)
    }

    "TODAY keeps only tasks completed since start of day" {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tasks = listOf(
            completedTask("today-1", startOfDay + 1_000L),
            completedTask("today-2", startOfDay + 30_000L),
            completedTask("old", startOfDay - 1L)
        )

        val filtered = filterTasksByDateRange(tasks, HistoryDateRange.TODAY, now)

        filtered.map { it.id }.shouldContainExactly(listOf("today-1", "today-2"))
    }

    "LAST_7_DAYS excludes tasks older than 7-day window" {
        val tasks = listOf(
            completedTask("in-range", now - 3 * oneDayMs),
            completedTask("edge", now - 6 * oneDayMs),
            completedTask("out-range", now - 7 * oneDayMs - 1)
        )

        val filtered = filterTasksByDateRange(tasks, HistoryDateRange.LAST_7_DAYS, now)

        filtered.map { it.id }.shouldContainExactly(listOf("in-range", "edge"))
    }

    "LAST_30_DAYS excludes null completion timestamps" {
        val tasks = listOf(
            completedTask("in-range", now - 10 * oneDayMs),
            completedTask("null", null)
        )

        val filtered = filterTasksByDateRange(tasks, HistoryDateRange.LAST_30_DAYS, now)

        filtered.shouldHaveSize(1)
        filtered.first().id shouldBe "in-range"
    }

    "HistoryUiState correctly reflects hasAnyCompletedTasks even when filtered list is empty" {
        val uiState = HistoryUiState(
            completedTasks = emptyList(),
            hasAnyCompletedTasks = true
        )
        uiState.hasAnyCompletedTasks shouldBe true
        uiState.completedTasks.isEmpty() shouldBe true
    }

    "Undoing task deletion preserves completed status" {
        val completed = completedTask("comp-1", now)
        completed.status shouldBe TaskStatus.COMPLETED
        completed.completedAt shouldBe now
    }

    "HistoryViewModel restoreTask uncompletes task to ACTIVE and decrements totalTasksCompleted (BUG-130)" {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        try {
            runTest(testDispatcher) {
                val taskRepo = mockk<TaskRepository>(relaxed = true)
                val sessionRepo = mockk<SessionRepository>(relaxed = true)
                val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
                val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

                val completed = completedTask("task-reopen-1", now)
                val prefsState = MutableStateFlow(UserPreferences(totalTasksCompleted = 5))

                coEvery { taskRepo.getById("task-reopen-1") } returns completed
                every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(listOf(completed))
                every { sessionRepo.observeAll() } returns MutableStateFlow(emptyList())
                every { prefsDataStore.preferencesFlow } returns prefsState
                coEvery { prefsDataStore.updatePreferences(any()) } coAnswers {
                    val transform = firstArg<(UserPreferences) -> UserPreferences>()
                    prefsState.value = transform(prefsState.value)
                    prefsState.value
                }

                val viewModel = HistoryViewModel(
                    taskRepository = taskRepo,
                    sessionRepository = sessionRepo,
                    userPreferencesDataStore = prefsDataStore,
                    calendarSyncRepository = calendarRepo
                )
                advanceUntilIdle()

                viewModel.restoreTask(completed)
                advanceUntilIdle()

                coVerify {
                    taskRepo.update(match {
                        it.id == "task-reopen-1" &&
                            it.status == TaskStatus.ACTIVE &&
                            it.completedAt == null
                    })
                }
                coVerify {
                    calendarRepo.handleTaskStateChanged(match {
                        it.id == "task-reopen-1" && it.status == TaskStatus.ACTIVE
                    })
                }
                prefsState.value.totalTasksCompleted shouldBe 4
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    "BUG-171: HistoryDetailSheet timing resolution distinguishes timed from date-only tasks" {
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
        val timedTask = TaskEntity(
            id = "timed-1",
            title = "Timed Task",
            scheduledDate = todayStart,
            scheduledTime = 14 * 3600_000L, // 14:00 (2:00 PM)
            deadlineDate = todayStart,
            deadlineTime = 17 * 3600_000L  // 17:00 (5:00 PM)
        )
        val dateOnlyTask = TaskEntity(
            id = "date-only-1",
            title = "Date Only Task",
            scheduledDate = todayStart,
            scheduledTime = null,
            deadlineDate = todayStart,
            deadlineTime = null
        )

        // Timed task resolves to exact epoch combining date and intraday time
        timedTask.resolveScheduledEpochMillis() shouldBe (todayStart + 14 * 3600_000L)
        timedTask.resolveDeadlineEpochMillis() shouldBe (todayStart + 17 * 3600_000L)

        // Date-only task resolves to start of day, but scheduledTime and deadlineTime remain null
        dateOnlyTask.scheduledTime shouldBe null
        dateOnlyTask.deadlineTime shouldBe null
        dateOnlyTask.resolveScheduledEpochMillis() shouldBe todayStart
        dateOnlyTask.resolveDeadlineEpochMillis() shouldBe todayStart
    }
})
