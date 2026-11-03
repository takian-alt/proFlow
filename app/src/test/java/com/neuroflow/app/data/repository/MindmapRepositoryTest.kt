package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.dao.MindmapDao
import com.neuroflow.app.data.local.entity.MindmapEdgeEntity
import com.neuroflow.app.data.local.entity.MindmapNodePositionEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*

class MindmapRepositoryTest : StringSpec({
    val mindmapDao = mockk<MindmapDao>(relaxed = true)
    val taskRepository = mockk<TaskRepository>(relaxed = true)
    val goalRepository = mockk<GoalRepository>(relaxed = true)
    val repository = MindmapRepository(mindmapDao, taskRepository, goalRepository)

    beforeTest {
        clearMocks(mindmapDao, taskRepository, goalRepository)
    }

    "savePosition upserts node position entity with exact coordinates" {
        repository.savePosition("task-1", "TASK", 150f, 250f)

        val slot = slot<MindmapNodePositionEntity>()
        coVerify { mindmapDao.upsertPosition(capture(slot)) }
        slot.captured.entityId shouldBe "task-1"
        slot.captured.entityType shouldBe "TASK"
        slot.captured.x shouldBe 150f
        slot.captured.y shouldBe 250f
    }

    "createEdge inserts edge entity with specified edgeType" {
        repository.createEdge("goal-1", "task-2", "GOAL_LINK")

        val slot = slot<MindmapEdgeEntity>()
        coVerify { mindmapDao.insertEdge(capture(slot)) }
        slot.captured.sourceId shouldBe "goal-1"
        slot.captured.targetId shouldBe "task-2"
        slot.captured.edgeType shouldBe "GOAL_LINK"
    }

    "toggleTaskCompletion marks active task as completed" {
        val activeTask = TaskEntity(id = "t-1", title = "Test Task", status = TaskStatus.ACTIVE)
        coEvery { taskRepository.getById("t-1") } returns activeTask

        repository.toggleTaskCompletion("t-1")

        coVerify { taskRepository.markTaskCompleted("t-1", any()) }
    }

    "toggleTaskCompletion reverts completed task back to active" {
        val completedTask = TaskEntity(
            id = "t-2",
            title = "Done Task",
            status = TaskStatus.COMPLETED,
            completedAt = 1000L
        )
        coEvery { taskRepository.getById("t-2") } returns completedTask

        repository.toggleTaskCompletion("t-2")

        val slot = slot<TaskEntity>()
        coVerify { taskRepository.update(capture(slot)) }
        slot.captured.status shouldBe TaskStatus.ACTIVE
        slot.captured.completedAt shouldBe null
    }

    "linkTaskToGoal updates task goalId" {
        val task = TaskEntity(id = "t-3", title = "Target Task", goalId = null)
        coEvery { taskRepository.getById("t-3") } returns task

        repository.linkTaskToGoal("t-3", "goal-99")

        val slot = slot<TaskEntity>()
        coVerify { taskRepository.update(capture(slot)) }
        slot.captured.goalId shouldBe "goal-99"
    }

    "addTaskDependency appends new dependency ID without dropping existing ones" {
        val task = TaskEntity(id = "t-4", title = "Dependent Task", dependsOnTaskIds = "dep-1, dep-2")
        coEvery { taskRepository.getById("t-4") } returns task

        repository.addTaskDependency("t-4", "dep-3")

        val slot = slot<TaskEntity>()
        coVerify { taskRepository.update(capture(slot)) }
        slot.captured.dependsOnTaskIds shouldContain "dep-1"
        slot.captured.dependsOnTaskIds shouldContain "dep-2"
        slot.captured.dependsOnTaskIds shouldContain "dep-3"
    }
})
