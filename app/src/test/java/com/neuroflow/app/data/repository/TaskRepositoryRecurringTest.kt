package com.neuroflow.app.data.repository

import androidx.work.WorkManager
import com.neuroflow.app.data.calendar.CalendarSyncRepository
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
    val calendarSyncRepository = mockk<CalendarSyncRepository>(relaxed = true)

    val repository = TaskRepository(
        taskDao = taskDao,
        hyperFocusManager = lazyHyperFocus,
        workManager = workManager,
        taskMicroStepDao = null,
        calendarSyncRepository = calendarSyncRepository
    )

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

    "completeAndRecur ignores continuation part2 and does NOT trigger recurrence (BUG-020)" {
        val part2Task = TaskEntity(
            id = "parent-120_part2",
            title = "Deep Research Project (Part 2/2)",
            recurrence = Recurrence.DAILY,
            parentTaskId = "parent-120"
        )
        val result = repository.completeAndRecur(part2Task, now)
        result shouldBe null
    }

    "completeAndRecur sanitizes partial title and resets plannedBlockMinutes on recurring instance (BUG-020)" {
        val splitPart1 = TaskEntity(
            id = "habit-split-1",
            title = "Deep Work Session (Part 1/2)",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            plannedBlockMinutes = 40,
            estimatedDurationMinutes = 80
        )
        val nextOccurrenceSlot = slot<TaskEntity>()
        coEvery { taskDao.insert(capture(nextOccurrenceSlot)) } returns Unit

        val newId = repository.completeAndRecur(splitPart1, now)
        newId shouldNotBe null

        nextOccurrenceSlot.captured.title shouldBe "Deep Work Session"
        nextOccurrenceSlot.captured.plannedBlockMinutes shouldBe 0
    }

    "delete cascades to continuation part2 and triggers calendar sync deletion (BUG-012, BUG-022)" {
        val parentTask = TaskEntity(
            id = "parent-delete-1",
            title = "Task with continuation"
        )
        val continuationPart2 = TaskEntity(
            id = "parent-delete-1_part2",
            title = "Task with continuation (Part 2/2)",
            parentTaskId = "parent-delete-1"
        )

        coEvery { taskDao.getById("parent-delete-1_part2") } returns continuationPart2

        repository.delete(parentTask)

        coVerify { taskDao.delete(parentTask) }
        coVerify { taskDao.delete(continuationPart2) }
        coVerify { calendarSyncRepository.deleteTaskEvent("parent-delete-1") }
        coVerify { calendarSyncRepository.deleteTaskEvent("parent-delete-1_part2") }
    }

    "direct delete of continuation part2 restores parent title and resets plannedBlockMinutes (BUG-029)" {
        val parentTask = TaskEntity(
            id = "parent-split-1",
            title = "Big Task (Part 1/2)",
            status = TaskStatus.ACTIVE,
            plannedBlockMinutes = 40,
            estimatedDurationMinutes = 80
        )
        val part2Task = TaskEntity(
            id = "parent-split-1_part2",
            title = "Big Task (Part 2/2)",
            parentTaskId = "parent-split-1",
            status = TaskStatus.ACTIVE,
            plannedBlockMinutes = 40,
            estimatedDurationMinutes = 40
        )

        coEvery { taskDao.getById("parent-split-1") } returns parentTask
        coEvery { taskDao.getById("parent-split-1_part2") } returns part2Task

        repository.delete(part2Task)

        coVerify { taskDao.delete(part2Task) }
        coVerify {
            taskDao.update(match {
                it.id == "parent-split-1" &&
                    it.title == "Big Task" &&
                    it.plannedBlockMinutes == 0
            })
        }
    }

    "completeAndRecur delegates to handleTaskStateChanged rather than deleteTaskEvent (BUG-137)" {
        val completedTaskSlots = mutableListOf<TaskEntity>()
        coEvery { calendarSyncRepository.handleTaskStateChanged(capture(completedTaskSlots)) } returns true

        val task = TaskEntity(
            id = "recurring-calendar-task-1",
            title = "Daily Focus Session",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            scheduledDate = todayStart,
            scheduledTime = nineAmOffset,
            status = TaskStatus.ACTIVE
        )

        repository.completeAndRecur(task, now)

        // Verify handleTaskStateChanged received completed task
        val completedTask = completedTaskSlots.find { it.id == "recurring-calendar-task-1" }
        completedTask shouldNotBe null
        completedTask!!.status shouldBe TaskStatus.COMPLETED

        // Verify deleteTaskEvent was NOT called for this task on completion
        coVerify(exactly = 0) {
            calendarSyncRepository.deleteTaskEvent("recurring-calendar-task-1")
        }
    }

    "BUG-149: insert, update, and delete correctly schedule and cancel reminders via workManager" {
        val taskWithDeadline = TaskEntity(
            id = "task-with-deadline-1",
            title = "Submit Paper",
            deadlineDate = todayStart + 86_400_000L,
            deadlineTime = 14 * 3600_000L,
            status = TaskStatus.ACTIVE
        )

        // 1. insert schedules a reminder via workManager
        repository.insert(taskWithDeadline)
        coVerify {
            workManager.enqueueUniqueWork(
                match { it.startsWith("task_reminder_${taskWithDeadline.id}") },
                any(),
                any<androidx.work.OneTimeWorkRequest>()
            )
        }

        // 2. update to COMPLETED cancels the reminder
        val completedTask = taskWithDeadline.copy(status = TaskStatus.COMPLETED)
        repository.update(completedTask)
        coVerify {
            workManager.cancelUniqueWork("task_reminder_${taskWithDeadline.id}_15")
            workManager.cancelAllWorkByTag("task_reminder_${taskWithDeadline.id}")
        }

        // 3. delete cancels the reminder and autonomy nudge (BUG-202)
        repository.delete(taskWithDeadline)
        coVerify(atLeast = 2) {
            workManager.cancelUniqueWork("task_reminder_${taskWithDeadline.id}_15")
        }
        coVerify {
            workManager.cancelUniqueWork("autonomy_nudge_${taskWithDeadline.id}")
            workManager.cancelAllWorkByTag("autonomy_nudge_${taskWithDeadline.id}")
        }
    }

    "BUG-175: completeAndRecur with dual-representation scheduledTime does not double timestamps to year 2081" {
        val absoluteEpochTime = todayStart + nineAmOffset // >= ONE_DAY_MILLIS
        val taskWithDualRepTime = TaskEntity(
            id = "habit-dual-rep-1",
            title = "Daily Review",
            recurrence = Recurrence.DAILY,
            habitDate = null,
            scheduledDate = todayStart,
            scheduledTime = absoluteEpochTime,
            status = TaskStatus.ACTIVE
        )

        val nextTaskSlot = slot<TaskEntity>()
        coEvery { taskDao.insert(capture(nextTaskSlot)) } returns Unit

        val newId = repository.completeAndRecur(taskWithDualRepTime, now)
        newId shouldNotBe null

        val nextOccurrence = nextTaskSlot.captured
        val oneYearFromNow = now + 365L * 86_400_000L
        (nextOccurrence.scheduledDate!! < oneYearFromNow) shouldBe true
        (nextOccurrence.habitDate!! < oneYearFromNow) shouldBe true
    }

    "completeAndRecur syncs newly spawned recurring occurrence to calendarSyncRepository" {
        val originalTask = TaskEntity(
            id = "habit-sync-1",
            title = "Daily Study",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            scheduledDate = todayStart,
            scheduledTime = nineAmOffset,
            isScheduleLocked = true
        )

        val syncedTasks = mutableListOf<TaskEntity>()
        coEvery { calendarSyncRepository.handleTaskStateChanged(capture(syncedTasks)) } returns true

        val newId = repository.completeAndRecur(originalTask, now)
        newId shouldNotBe null

        // One call for the completed original task, and one call for the newly spawned active occurrence
        syncedTasks.size shouldBe 2
        val completedSync = syncedTasks.find { it.id == "habit-sync-1" }
        val spawnedSync = syncedTasks.find { it.id == newId }
        completedSync shouldNotBe null
        completedSync!!.status shouldBe TaskStatus.COMPLETED
        spawnedSync shouldNotBe null
        spawnedSync!!.status shouldBe TaskStatus.ACTIVE
    }

    "BUG-191: completeAndRecur preserves scheduledTime and deadlineTime when scheduledDate is null" {
        val now = 1792396800000L // 2026-10-18 08:00:00 UTC
        val todayStart = startOfLocalDay(now)
        val nineAmOffset = 9 * 3600_000L // 09:00 AM
        val fivePmOffset = 17 * 3600_000L // 05:00 PM

        val originalTask = TaskEntity(
            id = "habit-null-date-1",
            title = "Morning Meditation",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            scheduledDate = null,
            scheduledTime = nineAmOffset,
            deadlineDate = null,
            deadlineTime = fivePmOffset
        )

        val spawnedSlot = slot<TaskEntity>()
        coEvery { taskDao.insert(capture(spawnedSlot)) } returns Unit

        val newId = repository.completeAndRecur(originalTask, now)
        newId shouldNotBe null

        spawnedSlot.isCaptured shouldBe true
        val spawned = spawnedSlot.captured
        spawned.scheduledDate shouldBe null
        spawned.scheduledTime shouldBe nineAmOffset
        spawned.deadlineDate shouldBe null
        spawned.deadlineTime shouldBe fivePmOffset
    }
})
