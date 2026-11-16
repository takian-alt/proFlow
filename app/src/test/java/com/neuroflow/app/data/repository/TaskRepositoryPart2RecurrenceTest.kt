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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TaskRepositoryPart2RecurrenceTest {

    private fun makeRepo(
        taskDao: TaskDao
    ): TaskRepository {
        val hyperFocusManager = mockk<HyperFocusManager>(relaxed = true)
        return TaskRepository(
            taskDao = taskDao,
            hyperFocusManager = Lazy { hyperFocusManager },
            workManager = mockk<WorkManager>(relaxed = true),
            taskMicroStepDao = null,
            calendarSyncRepository = mockk<CalendarSyncRepository>(relaxed = true)
        )
    }

    @Test
    fun `completing recurring parent with active hash part2 defers recurrence and advances streak when part2 completes`() = runTest {
        val taskDao = mockk<TaskDao>(relaxed = true)
        val repo = makeRepo(taskDao)

        val now = 1_760_000_000_000L
        val todayStart = startOfLocalDay(now)

        val parentTask = TaskEntity(
            id = "habit-split",
            title = "Deep Study (Part 1/2)",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            scheduledDate = todayStart,
            scheduledTime = 9 * 3600_000L,
            habitStreak = 4,
            isHabitual = true,
            estimatedDurationMinutes = 90,
            plannedBlockMinutes = 45
        )
        val part2Task = parentTask.copy(
            id = "habit-split#part2",
            title = "Deep Study (Part 2/2)",
            parentTaskId = "habit-split",
            dependsOnTaskIds = "habit-split",
            recurrence = Recurrence.NONE,
            isHabitual = false,
            habitDate = null,
            habitStreak = 0,
            estimatedDurationMinutes = 45,
            plannedBlockMinutes = 45
        )

        coEvery { taskDao.getById("habit-split#part2") } returns part2Task
        coEvery { taskDao.getById("habit-split_part2") } returns null

        // Step 1: Complete Part 1 while Part 2 is still ACTIVE
        val spawnedOnPart1 = repo.completeAndRecur(parentTask, now)
        assertNull("Recurrence must defer while #part2 is active", spawnedOnPart1)

        // Parent is completed, but habitStreak must NOT advance prematurely
        coVerify {
            taskDao.update(match {
                it.id == "habit-split" &&
                    it.status == TaskStatus.COMPLETED &&
                    it.habitStreak == 4
            })
        }

        // Step 2: Now complete #part2 after parent is COMPLETED
        val completedParent = parentTask.copy(status = TaskStatus.COMPLETED, completedAt = now, habitStreak = 4)
        coEvery { taskDao.getById("habit-split") } returns completedParent
        coEvery { taskDao.getActiveTasks() } returns emptyList()

        val insertedSlot = slot<TaskEntity>()
        coEvery { taskDao.insert(capture(insertedSlot)) } returns Unit

        val spawnedOnPart2 = repo.completeAndRecur(part2Task, now + 3_600_000L)
        assertNotNull("Completing #part2 must spawn next recurring occurrence", spawnedOnPart2)

        // Parent streak increments once to 5 and spawned occurrence carries streak 5
        coVerify {
            taskDao.update(match {
                it.id == "habit-split" && it.habitStreak == 5
            })
        }
        assertEquals(spawnedOnPart2, insertedSlot.captured.id)
        assertEquals(5, insertedSlot.captured.habitStreak)
        assertEquals("Deep Study", insertedSlot.captured.title)
        assertEquals(0, insertedSlot.captured.plannedBlockMinutes)
    }
}
