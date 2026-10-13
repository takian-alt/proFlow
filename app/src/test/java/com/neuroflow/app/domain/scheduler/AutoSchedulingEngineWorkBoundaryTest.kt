package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class AutoSchedulingEngineWorkBoundaryTest : StringSpec({
    "a contiguous task is placed today and stays inside the sleep-guarded window" {
        runTest {
            val preferences = UserPreferences(
                autoSchedulingEnabled = true,
                workDayStart = 8,
                workDayEnd = 18,
                wakeUpHour = 7,
                sleepHour = 23
            )
            val store = mockk<UserPreferencesDataStore>()
            every { store.preferencesFlow } returns MutableStateFlow(preferences)
            val engine = AutoSchedulingEngine(store)
            val now = Calendar.getInstance().apply {
                set(2026, Calendar.APRIL, 25, 16, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val task = TaskEntity(
                title = "Three-hour report",
                priority = Priority.HIGH,
                taskType = TaskType.ADMIN,
                effortScore = 40,
                estimatedDurationMinutes = 180,
                status = TaskStatus.ACTIVE
            )

            val decision = engine.planAutoSchedule(
                unscheduledTasks = listOf(task),
                nowMillis = now,
                energyScoreFn = { 75 to 1f }
            ).single()

            // Late in the workday with free hours left before sleep, the task belongs to today
            val startCalendar = Calendar.getInstance().apply { timeInMillis = decision.scheduledStartMillis }
            val startDay = startCalendar.get(Calendar.DAY_OF_MONTH)
            val end = decision.scheduledStartMillis + decision.estimatedDurationMinutes * 60_000L
            val endCalendar = Calendar.getInstance().apply { timeInMillis = end }
            val endMinuteOfDay = endCalendar.get(Calendar.HOUR_OF_DAY) * 60 +
                endCalendar.get(Calendar.MINUTE)
            startDay shouldBe 25
            (endMinuteOfDay <= 23 * 60) shouldBe true
        }
    }

    "a task with a chosen date but no time keeps that date and gains an hour" {
        runTest {
            val preferences = UserPreferences(
                autoSchedulingEnabled = true,
                workDayStart = 8,
                workDayEnd = 18,
                wakeUpHour = 7,
                sleepHour = 23
            )
            val store = mockk<UserPreferencesDataStore>()
            every { store.preferencesFlow } returns MutableStateFlow(preferences)
            val engine = AutoSchedulingEngine(store)
            val now = Calendar.getInstance().apply {
                set(2026, Calendar.APRIL, 25, 16, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val chosenDay = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val task = TaskEntity(
                title = "Chosen day, no hour",
                priority = Priority.HIGH,
                effortScore = 40,
                estimatedDurationMinutes = 60,
                scheduledDate = chosenDay,
                status = TaskStatus.ACTIVE
            )

            val decision = engine.planAutoSchedule(
                unscheduledTasks = listOf(task),
                nowMillis = now,
                energyScoreFn = { 85 to 1f }
            ).single()

            val startCalendar = Calendar.getInstance().apply { timeInMillis = decision.scheduledStartMillis }
            startCalendar.get(Calendar.DAY_OF_MONTH) shouldBe 26
            (decision.scheduledStartMillis >= chosenDay) shouldBe true
        }
    }
})
