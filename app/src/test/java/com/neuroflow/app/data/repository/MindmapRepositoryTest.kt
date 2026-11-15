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

    "spawnChildTask under goal sets goalId and null parentTaskId" {
        val goal = com.neuroflow.app.data.local.entity.GoalEntity(id = "g-1", title = "Launch Product")
        coEvery { goalRepository.getById("g-1") } returns goal

        val created = repository.spawnChildTask("g-1", "Design System", com.neuroflow.app.domain.model.Priority.HIGH)

        val slot = slot<TaskEntity>()
        coVerify { taskRepository.insert(capture(slot)) }
        slot.captured.title shouldBe "Design System"
        slot.captured.goalId shouldBe "g-1"
        slot.captured.parentTaskId shouldBe null
        slot.captured.priority shouldBe com.neuroflow.app.domain.model.Priority.HIGH
    }

    "spawnChildTask under parent task sets parentTaskId and inherits goalId" {
        val parentTask = TaskEntity(id = "p-1", title = "Parent Feature", goalId = "g-1")
        coEvery { goalRepository.getById("p-1") } returns null
        coEvery { taskRepository.getById("p-1") } returns parentTask

        val created = repository.spawnChildTask("p-1", "Subtask 1")

        val slot = slot<TaskEntity>()
        coVerify { taskRepository.insert(capture(slot)) }
        slot.captured.title shouldBe "Subtask 1"
        slot.captured.parentTaskId shouldBe "p-1"
        slot.captured.goalId shouldBe "g-1"
    }

    "spawnSiblingTask creates task sharing same parentTaskId and goalId" {
        val siblingTask = TaskEntity(id = "sib-1", title = "Sibling", parentTaskId = "p-1", goalId = "g-1")
        coEvery { taskRepository.getById("sib-1") } returns siblingTask

        val created = repository.spawnSiblingTask("sib-1", "New Sibling", com.neuroflow.app.domain.model.Priority.LOW)

        val slot = slot<TaskEntity>()
        coVerify { taskRepository.insert(capture(slot)) }
        slot.captured.title shouldBe "New Sibling"
        slot.captured.parentTaskId shouldBe "p-1"
        slot.captured.goalId shouldBe "g-1"
        slot.captured.priority shouldBe com.neuroflow.app.domain.model.Priority.LOW
    }

    "reparentTask reparents child under new parent task and clears manual position" {
        val child = TaskEntity(id = "child-1", title = "Child", parentTaskId = null, goalId = null)
        val newParent = TaskEntity(id = "parent-2", title = "New Parent", goalId = "g-2")
        coEvery { taskRepository.getById("child-1") } returns child
        coEvery { goalRepository.getById("parent-2") } returns null
        coEvery { taskRepository.getById("parent-2") } returns newParent

        repository.reparentTask("child-1", "parent-2")

        val slot = slot<TaskEntity>()
        coVerify { taskRepository.update(capture(slot)) }
        slot.captured.parentTaskId shouldBe "parent-2"
        slot.captured.goalId shouldBe "g-2"
        coVerify { mindmapDao.deletePositionByEntityId("child-1") }
    }

    "getConnectionsForNode collects parent, goal, dependencies and reverse dependencies" {
        val target = TaskEntity(
            id = "t-target",
            title = "Target Task",
            parentTaskId = "p-100",
            goalId = "g-200",
            dependsOnTaskIds = "dep-1, dep-2"
        )
        val parentTask = TaskEntity(id = "p-100", title = "Parent Feature")
        val goal = com.neuroflow.app.data.local.entity.GoalEntity(id = "g-200", title = "Target Goal")
        val dep1 = TaskEntity(id = "dep-1", title = "Prerequisite 1")
        val dep2 = TaskEntity(id = "dep-2", title = "Prerequisite 2")
        val dependent = TaskEntity(id = "other-task", title = "Dependent Task", dependsOnTaskIds = "t-target")

        coEvery { taskRepository.getById("t-target") } returns target
        coEvery { taskRepository.getById("p-100") } returns parentTask
        coEvery { goalRepository.getById("g-200") } returns goal
        coEvery { taskRepository.getById("dep-1") } returns dep1
        coEvery { taskRepository.getById("dep-2") } returns dep2
        coEvery { taskRepository.getAllTasks() } returns listOf(target, parentTask, dep1, dep2, dependent)
        coEvery { mindmapDao.getEdgesForEntity("t-target") } returns emptyList()

        val connections = repository.getConnectionsForNode("t-target")

        connections.any { it.type == ConnectionType.PARENT_LINK && it.otherNodeId == "p-100" } shouldBe true
        connections.any { it.type == ConnectionType.GOAL_LINK && it.otherNodeId == "g-200" } shouldBe true
        connections.any { it.type == ConnectionType.DEPENDENCY && it.otherNodeId == "dep-1" } shouldBe true
        connections.any { it.type == ConnectionType.DEPENDENCY && it.otherNodeId == "dep-2" } shouldBe true
        connections.any { it.type == ConnectionType.REVERSE_DEPENDENCY && it.otherNodeId == "other-task" } shouldBe true
    }

    "removeSingleConnection removes only specified dependency" {
        val task = TaskEntity(id = "t-5", title = "Task", dependsOnTaskIds = "dep-1, dep-2, dep-3")
        coEvery { taskRepository.getById("t-5") } returns task

        val conn = NodeConnection(
            type = ConnectionType.DEPENDENCY,
            label = "Depends on: dep-2",
            otherNodeId = "dep-2"
        )

        repository.removeSingleConnection("t-5", conn)

        val slot = slot<TaskEntity>()
        coVerify { taskRepository.update(capture(slot)) }
        slot.captured.dependsOnTaskIds shouldBe "dep-1,dep-3"
    }

    "createGoal, updateGoal and deleteGoal delegate correctly to goalRepository" {
        repository.createGoal("New Objective", "Description", 123456L)
        val insertSlot = slot<com.neuroflow.app.data.local.entity.GoalEntity>()
        coVerify { goalRepository.insert(capture(insertSlot)) }
        insertSlot.captured.title shouldBe "New Objective"
        insertSlot.captured.description shouldBe "Description"
        insertSlot.captured.targetDate shouldBe 123456L

        val existing = com.neuroflow.app.data.local.entity.GoalEntity(id = "g-del", title = "To Delete")
        coEvery { goalRepository.getById("g-del") } returns existing
        repository.deleteGoal("g-del")
        coVerify { goalRepository.delete(existing) }
    }
})
