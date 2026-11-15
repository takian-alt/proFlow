package com.neuroflow.app.presentation.v2.mindmap

import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.MindmapRepository
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class MindmapViewModelTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    "MindmapViewModel filters search queries and highlights matching nodes" {
        runTest(testDispatcher) {
            val repository = mockk<MindmapRepository>(relaxed = true)
            val goal1 = GoalEntity(id = "g-1", title = "Launch Distributed Cache")
            val task1 = TaskEntity(id = "t-1", title = "Write Raft Consensus Engine", tags = "distributed,consensus")
            val task2 = TaskEntity(id = "t-2", title = "Setup CI/CD Pipeline", tags = "devops")

            every { repository.observeGoals() } returns flowOf(listOf(goal1))
            every { repository.observeTasks(any()) } returns flowOf(listOf(task1, task2))
            every { repository.observePositions() } returns flowOf(emptyList())
            every { repository.observeEdges() } returns flowOf(emptyList())

            val viewModel = MindmapViewModel(repository)
            advanceUntilIdle()

            viewModel.uiState.value.highlightedNodeIds.isEmpty() shouldBe true

            viewModel.setSearchQuery("Raft")
            advanceUntilIdle()

            val stateWithQuery = viewModel.uiState.value
            stateWithQuery.searchQuery shouldBe "Raft"
            stateWithQuery.highlightedNodeIds shouldContain "t-1"
            stateWithQuery.highlightedNodeIds shouldNotContain "t-2"
            stateWithQuery.highlightedNodeIds shouldNotContain "g-1"

            viewModel.setSearchQuery("Cache")
            advanceUntilIdle()

            viewModel.uiState.value.highlightedNodeIds shouldContain "g-1"
            viewModel.uiState.value.highlightedNodeIds shouldNotContain "t-1"
        }
    }

    "spawnSiblingTask invokes repository and uncollapses parent" {
        runTest(testDispatcher) {
            val repository = mockk<MindmapRepository>(relaxed = true)
            val refTask = TaskEntity(id = "ref-1", title = "Ref Task", parentTaskId = "p-1", goalId = "g-1")
            coEvery { repository.getTaskById("ref-1") } returns refTask

            every { repository.observeGoals() } returns flowOf(emptyList())
            every { repository.observeTasks(any()) } returns flowOf(listOf(refTask))
            every { repository.observePositions() } returns flowOf(emptyList())
            every { repository.observeEdges() } returns flowOf(emptyList())

            val viewModel = MindmapViewModel(repository)
            advanceUntilIdle()

            viewModel.spawnSiblingTask("ref-1", "New Sibling", Priority.HIGH)
            advanceUntilIdle()

            coVerify { repository.spawnSiblingTask("ref-1", "New Sibling", Priority.HIGH) }
        }
    }

    "collapseAll and expandAll toggle all node IDs" {
        runTest(testDispatcher) {
            val repository = mockk<MindmapRepository>(relaxed = true)
            val goal = GoalEntity(id = "g-1", title = "Goal 1")
            val parent = TaskEntity(id = "p-1", title = "Parent", goalId = goal.id)
            val child = TaskEntity(id = "c-1", title = "Child", parentTaskId = parent.id)

            every { repository.observeGoals() } returns flowOf(listOf(goal))
            every { repository.observeTasks(any()) } returns flowOf(listOf(parent, child))
            every { repository.observePositions() } returns flowOf(emptyList())
            every { repository.observeEdges() } returns flowOf(emptyList())

            val viewModel = MindmapViewModel(repository)
            advanceUntilIdle()

            viewModel.collapseAll()
            advanceUntilIdle()

            viewModel.uiState.value.collapsedNodeIds shouldContain "g-1"
            viewModel.uiState.value.collapsedNodeIds shouldContain "p-1"

            viewModel.expandAll()
            advanceUntilIdle()

            viewModel.uiState.value.collapsedNodeIds.isEmpty() shouldBe true
        }
    }

    "onNodeDragEnd with target triggers reparentTask" {
        runTest(testDispatcher) {
            val repository = mockk<MindmapRepository>(relaxed = true)
            val task1 = TaskEntity(id = "t-1", title = "Task 1")
            val task2 = TaskEntity(id = "t-2", title = "Task 2")

            every { repository.observeGoals() } returns flowOf(emptyList())
            every { repository.observeTasks(any()) } returns flowOf(listOf(task1, task2))
            every { repository.observePositions() } returns flowOf(emptyList())
            every { repository.observeEdges() } returns flowOf(emptyList())

            val viewModel = MindmapViewModel(repository)
            advanceUntilIdle()

            // Simulate dragging t-1 close to t-2
            val t2Pos = viewModel.uiState.value.tasks.find { it.id == "t-2" }
            if (t2Pos != null) {
                viewModel.onNodeDrag("t-1", androidx.compose.ui.geometry.Offset(t2Pos.x, t2Pos.y))
                advanceUntilIdle()

                viewModel.onNodeDragEnd("t-1", "TASK", androidx.compose.ui.geometry.Offset(t2Pos.x, t2Pos.y))
                advanceUntilIdle()

                coVerify { repository.reparentTask("t-1", "t-2") }
            }
        }
    }

    "openDisconnectPicker populates connections and removeSingleConnection calls repository" {
        runTest(testDispatcher) {
            val repository = mockk<MindmapRepository>(relaxed = true)
            val task = TaskEntity(id = "t-picker", title = "Task with Link")
            val conn = com.neuroflow.app.data.repository.NodeConnection(
                type = com.neuroflow.app.data.repository.ConnectionType.PARENT_LINK,
                label = "Parent: P",
                otherNodeId = "p-1"
            )

            coEvery { repository.getConnectionsForNode("t-picker") } returns listOf(conn)
            every { repository.observeGoals() } returns flowOf(emptyList())
            every { repository.observeTasks(any()) } returns flowOf(listOf(task))
            every { repository.observePositions() } returns flowOf(emptyList())
            every { repository.observeEdges() } returns flowOf(emptyList())

            val viewModel = MindmapViewModel(repository)
            advanceUntilIdle()

            val taskNode = viewModel.uiState.value.tasks.first { it.id == "t-picker" }
            viewModel.openDisconnectPicker(taskNode)
            advanceUntilIdle()

            viewModel.uiState.value.disconnectTargetNode?.id shouldBe "t-picker"
            viewModel.uiState.value.disconnectConnections.size shouldBe 1

            viewModel.removeSingleConnection("t-picker", conn)
            advanceUntilIdle()

            coVerify { repository.removeSingleConnection("t-picker", conn) }
        }
    }

    "createGoal, updateGoal and deleteGoal trigger repository methods and auto-layout" {
        runTest(testDispatcher) {
            val repository = mockk<MindmapRepository>(relaxed = true)
            every { repository.observeGoals() } returns flowOf(emptyList())
            every { repository.observeTasks(any()) } returns flowOf(emptyList())
            every { repository.observePositions() } returns flowOf(emptyList())
            every { repository.observeEdges() } returns flowOf(emptyList())

            val viewModel = MindmapViewModel(repository)
            advanceUntilIdle()

            viewModel.createGoal("Scale Backend", "Reach 100k DAU", 5555L)
            advanceUntilIdle()

            coVerify { repository.createGoal("Scale Backend", "Reach 100k DAU", 5555L) }
            viewModel.uiState.value.isGoalCreateOpen shouldBe false

            val existingGoal = GoalEntity(id = "g-edit", title = "Old Title")
            coEvery { repository.getGoalById("g-edit") } returns existingGoal

            viewModel.updateGoal("g-edit", "New Title", "New Desc", null)
            advanceUntilIdle()

            coVerify { repository.updateGoal(match { it.id == "g-edit" && it.title == "New Title" }) }

            viewModel.deleteGoal("g-edit")
            advanceUntilIdle()

            coVerify { repository.deleteGoal("g-edit") }
        }
    }
})
