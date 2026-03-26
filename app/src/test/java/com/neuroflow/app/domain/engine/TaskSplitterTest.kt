package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSplitterTest {

    @Test
    fun `split creates three subtasks and archives parent`() = runTest {
        val repository = mockk<TaskRepository>()
        val inserted = mutableListOf<TaskEntity>()

        coEvery { repository.insert(any()) } answers {
            inserted += firstArg<TaskEntity>()
            Unit
        }
        coEvery { repository.update(any()) } returns Unit

        val parent = TaskEntity(id = "parent-1", title = "Big task")

        val result = TaskSplitter.split(parent, repository, sequentialDependencies = false)

        assertEquals(3, result.size)
        assertEquals(3, inserted.size)
        assertTrue(result.all { it.parentTaskId == parent.id })
        assertTrue(result.all { it.dependsOnTaskIds.isBlank() })

        coVerify(exactly = 3) { repository.insert(any()) }
        coVerify(exactly = 1) {
            repository.update(match { it.id == parent.id && it.status == TaskStatus.ARCHIVED })
        }
    }

    @Test
    fun `split creates sequential dependencies when enabled`() = runTest {
        val repository = mockk<TaskRepository>()

        coEvery { repository.insert(any()) } returns Unit
        coEvery { repository.update(any()) } returns Unit

        val parent = TaskEntity(id = "parent-2", title = "Complex task")

        val result = TaskSplitter.split(parent, repository, sequentialDependencies = true)

        assertEquals(3, result.size)
        assertTrue(result[0].dependsOnTaskIds.isBlank())
        assertEquals(result[0].id, result[1].dependsOnTaskIds)
        assertEquals(result[1].id, result[2].dependsOnTaskIds)
    }
}
