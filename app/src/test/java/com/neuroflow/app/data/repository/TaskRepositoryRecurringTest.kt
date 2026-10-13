package com.neuroflow.app.data.repository

import androidx.work.WorkManager
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import dagger.Lazy
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.Calendar
import java.util.TimeZone

class TaskRepositoryRecurringTest : StringSpec({

    val taskDao = mockk<TaskDao>(relaxed = true)
    val hyperFocusManager = mockk<HyperFocusManager>(relaxed = true)
    val lazyHyperFocus = Lazy { hyperFocusManager }
    val workManager = mockk<WorkManager>(relaxed = true)

    val repository = TaskRepository(taskDao, lazyHyperFocus, workManager)

    val now = System.currentTimeMillis()
    val todayStart = startOfLocalDay(now)
    val nineAmOffset = 9 * 3600_000L

    "completeAndRecur completes recurring task and inserts next occurrence for DAILY recurrence" {
        val originalTask = TaskEntity(
            id = "habit-1",
            title = "Morning Workout",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            scheduledDate = todayStart,
            scheduledTime = nineAmOffset,
            habitStreak = 3,
            isHabitual = true
        )

        coEvery { taskDao.getById("habit-1") } returns originalTask

        val insertedTaskSlot = slot<TaskEntity>()
        coEvery { taskDao.insert(capture(insertedTaskSlot)) } returns Unit

        val newId = repository.completeAndRecur(originalTask, now)

        newId shouldNotBe null

        // Verify original task was completed
        coVerify {
            taskDao.update(match {
                it.id == "habit-1" &&
                it.status == TaskStatus.COMPLETED &&
                it.habitStreak == 4 &&
                it.isHabitual
            })
        }

        // Verify next occurrence inserted
        val newOccurrence = insertedTaskSlot.captured
        newOccurrence.id shouldBe newId
        newOccurrence.status shouldBe TaskStatus.ACTIVE
        newOccurrence.habitStreak shouldBe 4
        newOccurrence.isHabitual shouldBe true
        newOccurrence.scheduledTime shouldBe nineAmOffset
    }

    "completeAndRecur returns null for non-recurring tasks" {
        val oneOffTask = TaskEntity(
            id = "oneoff-1",
            title = "File taxes",
            recurrence = Recurrence.NONE
        )

        val newId = repository.completeAndRecur(oneOffTask, now)
        newId shouldBe null

        coVerify {
            taskDao.update(match {
                it.id == "oneoff-1" && it.status == TaskStatus.COMPLETED
            })
        }
    }

    "completeAndRecur preserves local deadline date across DST" {
        val originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        try {
            fun localDay(year: Int, month: Int, day: Int): Long =
                Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

            val anchor = localDay(2026, Calendar.MARCH, 7)
            val deadline = localDay(2026, Calendar.MARCH, 8)
            val now = Calendar.getInstance().apply {
                set(2026, Calendar.MARCH, 7, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val recurringTask = TaskEntity(
                id = "habit-dst",
                title = "Daily Habit",
                recurrence = Recurrence.DAILY,
                habitDate = anchor,
                deadlineDate = deadline,
                deadlineTime = 0L
            )
            coEvery { taskDao.getById("habit-dst") } returns recurringTask
            val insertedTaskSlot = slot<TaskEntity>()
            coEvery { taskDao.insert(capture(insertedTaskSlot)) } returns Unit

            repository.completeAndRecur(recurringTask, now)

            insertedTaskSlot.captured.deadlineDate shouldBe localDay(2026, Calendar.MARCH, 9)
            insertedTaskSlot.captured.deadlineTime shouldBe 0L
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    "splitTaskIntoSessions splits 120m task into sequential chained sessions and archives parent" {
        val parentTask = TaskEntity(
            id = "parent-120",
            title = "Deep Research Project",
            estimatedDurationMinutes = 120,
            impactScore = 80,
            isFrog = true
        )

        val insertedTasks = mutableListOf<TaskEntity>()
        coEvery { taskDao.insert(capture(insertedTasks)) } returns Unit

        val sessions = repository.splitTaskIntoSessions(parentTask, sessionDurationMinutes = 45)

        sessions.size shouldBe 3 // 120 / 45 -> 3 chunks (40 min each)
        sessions[0].title shouldBe "Deep Research Project (Part 1/3)"
        sessions[0].estimatedDurationMinutes shouldBe 40
        sessions[0].parentTaskId shouldBe "parent-120"
        sessions[0].dependsOnTaskIds shouldBe ""
        sessions[0].isFrog shouldBe true

        sessions[1].title shouldBe "Deep Research Project (Part 2/3)"
        sessions[1].estimatedDurationMinutes shouldBe 40
        sessions[1].parentTaskId shouldBe "parent-120"
        sessions[1].dependsOnTaskIds shouldBe sessions[0].id
        sessions[1].isFrog shouldBe false

        sessions[2].title shouldBe "Deep Research Project (Part 3/3)"
        sessions[2].estimatedDurationMinutes shouldBe 40
        sessions[2].parentTaskId shouldBe "parent-120"
        sessions[2].dependsOnTaskIds shouldBe sessions[1].id
        sessions[2].isFrog shouldBe false

        // Verify parent is updated to COMPLETED
        coVerify {
            taskDao.update(match {
                it.id == "parent-120" && it.status == TaskStatus.COMPLETED
            })
        }
    }
})
