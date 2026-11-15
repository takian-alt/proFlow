package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.dao.MindmapDao
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.MindmapEdgeEntity
import com.neuroflow.app.data.local.entity.MindmapNodePositionEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionType {
    PARENT_LINK,
    GOAL_LINK,
    DEPENDENCY,
    REVERSE_DEPENDENCY,
    CHILD_LINK,
    VISUAL_EDGE
}

data class NodeConnection(
    val type: ConnectionType,
    val label: String,
    val otherNodeId: String,
    val edgeId: String? = null
)

@Singleton
class MindmapRepository @Inject constructor(
    private val mindmapDao: MindmapDao,
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository
) {
    fun observePositions(): Flow<List<MindmapNodePositionEntity>> =
        mindmapDao.observeAllPositions()

    fun observeEdges(): Flow<List<MindmapEdgeEntity>> =
        mindmapDao.observeAllEdges()

    fun observeGoals(): Flow<List<GoalEntity>> =
        goalRepository.observeActiveGoals()

    fun observeTasks(showCompleted: Boolean): Flow<List<TaskEntity>> {
        if (!showCompleted) {
            return taskRepository.observeActiveTasks()
        }
        return combine(
            taskRepository.observeActiveTasks(),
            taskRepository.observeCompletedTasks()
        ) { active, completed ->
            val cutoff = System.currentTimeMillis() - (24L * 60 * 60 * 1000)
            val recentCompleted = completed.filter { (it.completedAt ?: 0L) >= cutoff }
            active + recentCompleted
        }
    }

    suspend fun savePosition(entityId: String, entityType: String, x: Float, y: Float) {
        mindmapDao.upsertPosition(
            MindmapNodePositionEntity(
                entityId = entityId,
                entityType = entityType,
                x = x,
                y = y,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun savePositions(positions: List<MindmapNodePositionEntity>) {
        mindmapDao.upsertPositions(positions)
    }

    suspend fun createEdge(sourceId: String, targetId: String, edgeType: String = "VISUAL") {
        mindmapDao.insertEdge(
            MindmapEdgeEntity(
                id = UUID.randomUUID().toString(),
                sourceId = sourceId,
                targetId = targetId,
                edgeType = edgeType,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteEdge(edgeId: String) {
        mindmapDao.deleteEdgeById(edgeId)
    }

    suspend fun toggleTaskCompletion(taskId: String) {
        val task = taskRepository.getById(taskId) ?: return
        if (task.status == TaskStatus.COMPLETED) {
            taskRepository.update(
                task.copy(
                    status = TaskStatus.ACTIVE,
                    completedAt = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            taskRepository.markTaskCompleted(taskId)
        }
    }

    suspend fun linkTaskToGoal(taskId: String, goalId: String) {
        val task = taskRepository.getById(taskId) ?: return
        taskRepository.update(
            task.copy(
                goalId = goalId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addTaskDependency(taskId: String, dependsOnId: String) {
        val task = taskRepository.getById(taskId) ?: return
        val current = task.dependsOnTaskIds
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
        current.add(dependsOnId)
        taskRepository.update(
            task.copy(
                dependsOnTaskIds = current.joinToString(","),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getTaskById(taskId: String): TaskEntity? = taskRepository.getById(taskId)

    suspend fun addTask(task: TaskEntity, x: Float, y: Float) {
        taskRepository.insert(task)
        savePosition(task.id, "TASK", x, y)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskRepository.update(task)
    }

    suspend fun deleteTask(taskId: String) {
        val task = taskRepository.getById(taskId)
        if (task != null) {
            taskRepository.delete(task)
        }
        mindmapDao.deletePositionByEntityId(taskId)
        mindmapDao.deleteEdgesForEntity(taskId)
    }

    suspend fun disconnectNode(nodeId: String) {
        mindmapDao.deleteEdgesForEntity(nodeId)
        val task = taskRepository.getById(nodeId)
        if (task != null) {
            taskRepository.update(
                task.copy(
                    goalId = null,
                    parentTaskId = null,
                    dependsOnTaskIds = "",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        val allTasks = taskRepository.getAllTasks()
        allTasks.forEach { other ->
            if (other.dependsOnTaskIds.contains(nodeId)) {
                val newDeps = other.dependsOnTaskIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it != nodeId }
                    .joinToString(",")
                taskRepository.update(
                    other.copy(
                        dependsOnTaskIds = newDeps,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Returns a structured list of every individual connection a node has,
     * suitable for letting the user choose which ones to remove.
     */
    suspend fun getConnectionsForNode(nodeId: String): List<NodeConnection> {
        val connections = mutableListOf<NodeConnection>()
        val task = taskRepository.getById(nodeId)

        if (task != null) {
            // 1. Parent link
            if (task.parentTaskId != null) {
                val parentTask = taskRepository.getById(task.parentTaskId)
                connections.add(
                    NodeConnection(
                        type = ConnectionType.PARENT_LINK,
                        label = "Parent: ${parentTask?.title ?: task.parentTaskId}",
                        otherNodeId = task.parentTaskId
                    )
                )
            }

            // 2. Goal link
            if (task.goalId != null) {
                val goal = goalRepository.getById(task.goalId)
                connections.add(
                    NodeConnection(
                        type = ConnectionType.GOAL_LINK,
                        label = "Goal: ${goal?.title ?: task.goalId}",
                        otherNodeId = task.goalId
                    )
                )
            }

            // 3. Dependencies (tasks this task depends on)
            val depIds = task.dependsOnTaskIds.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            for (depId in depIds) {
                val depTask = taskRepository.getById(depId)
                connections.add(
                    NodeConnection(
                        type = ConnectionType.DEPENDENCY,
                        label = "Depends on: ${depTask?.title ?: depId}",
                        otherNodeId = depId
                    )
                )
            }

            // 4. Reverse dependencies (tasks that depend on this task)
            val allTasks = taskRepository.getAllTasks()
            allTasks.forEach { other ->
                val otherDeps = other.dependsOnTaskIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                if (nodeId in otherDeps) {
                    connections.add(
                        NodeConnection(
                            type = ConnectionType.REVERSE_DEPENDENCY,
                            label = "Depended on by: ${other.title}",
                            otherNodeId = other.id
                        )
                    )
                }
            }

            // 5. Child links (tasks that have this task as parent)
            allTasks.filter { it.parentTaskId == nodeId }.forEach { child ->
                connections.add(
                    NodeConnection(
                        type = ConnectionType.CHILD_LINK,
                        label = "Child: ${child.title}",
                        otherNodeId = child.id
                    )
                )
            }
        }

        // 6. Custom edges (VISUAL edges from mindmap_edges table)
        val customEdges = mindmapDao.getEdgesForEntity(nodeId)
        customEdges.forEach { edge ->
            val otherId = if (edge.sourceId == nodeId) edge.targetId else edge.sourceId
            val otherTask = taskRepository.getById(otherId)
            val otherGoal = goalRepository.getById(otherId)
            val otherTitle = otherTask?.title ?: otherGoal?.title ?: otherId
            connections.add(
                NodeConnection(
                    type = ConnectionType.VISUAL_EDGE,
                    label = "Visual link: $otherTitle",
                    otherNodeId = otherId,
                    edgeId = edge.id
                )
            )
        }

        return connections
    }

    /**
     * Removes a single specific connection from a node.
     */
    suspend fun removeSingleConnection(nodeId: String, connection: NodeConnection) {
        val task = taskRepository.getById(nodeId)
        when (connection.type) {
            ConnectionType.PARENT_LINK -> {
                if (task != null) {
                    taskRepository.update(
                        task.copy(
                            parentTaskId = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            ConnectionType.GOAL_LINK -> {
                if (task != null) {
                    taskRepository.update(
                        task.copy(
                            goalId = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            ConnectionType.DEPENDENCY -> {
                if (task != null) {
                    val newDeps = task.dependsOnTaskIds.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it != connection.otherNodeId }
                        .joinToString(",")
                    taskRepository.update(
                        task.copy(
                            dependsOnTaskIds = newDeps,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            ConnectionType.REVERSE_DEPENDENCY -> {
                val otherTask = taskRepository.getById(connection.otherNodeId)
                if (otherTask != null) {
                    val newDeps = otherTask.dependsOnTaskIds.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it != nodeId }
                        .joinToString(",")
                    taskRepository.update(
                        otherTask.copy(
                            dependsOnTaskIds = newDeps,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            ConnectionType.CHILD_LINK -> {
                val childTask = taskRepository.getById(connection.otherNodeId)
                if (childTask != null) {
                    taskRepository.update(
                        childTask.copy(
                            parentTaskId = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            ConnectionType.VISUAL_EDGE -> {
                if (connection.edgeId != null) {
                    mindmapDao.deleteEdgeById(connection.edgeId)
                } else {
                    mindmapDao.deleteEdgeBetween(nodeId, connection.otherNodeId)
                }
            }
        }
    }

    suspend fun createGoal(title: String, description: String = "", targetDate: Long? = null): GoalEntity {
        val goal = GoalEntity(
            title = title,
            description = description,
            targetDate = targetDate,
            createdAt = System.currentTimeMillis()
        )
        goalRepository.insert(goal)
        return goal
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalRepository.update(goal)
    }

    suspend fun deleteGoal(goalId: String) {
        val goal = goalRepository.getById(goalId) ?: return
        goalRepository.delete(goal)
    }

    suspend fun getGoalById(goalId: String): GoalEntity? {
        return goalRepository.getById(goalId)
    }

    suspend fun reparentTask(taskId: String, newParentId: String) {
        val task = taskRepository.getById(taskId) ?: return
        val goal = goalRepository.getById(newParentId)
        if (goal != null) {
            taskRepository.update(
                task.copy(
                    goalId = goal.id,
                    parentTaskId = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            mindmapDao.deletePositionByEntityId(taskId)
            return
        }
        val parentTask = taskRepository.getById(newParentId)
        if (parentTask != null) {
            taskRepository.update(
                task.copy(
                    parentTaskId = parentTask.id,
                    goalId = parentTask.goalId,
                    updatedAt = System.currentTimeMillis()
                )
            )
            mindmapDao.deletePositionByEntityId(taskId)
        }
    }

    suspend fun detachTask(taskId: String) {
        val task = taskRepository.getById(taskId) ?: return
        taskRepository.update(
            task.copy(
                parentTaskId = null,
                goalId = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun spawnChildTask(
        parentId: String,
        title: String,
        priority: com.neuroflow.app.domain.model.Priority = com.neuroflow.app.domain.model.Priority.MEDIUM
    ): TaskEntity {
        val goal = goalRepository.getById(parentId)
        val newTask = if (goal != null) {
            TaskEntity(
                title = title,
                goalId = goal.id,
                parentTaskId = null,
                priority = priority
            )
        } else {
            val parentTask = taskRepository.getById(parentId)
            TaskEntity(
                title = title,
                parentTaskId = parentId,
                goalId = parentTask?.goalId,
                priority = priority
            )
        }
        taskRepository.insert(newTask)
        return newTask
    }

    suspend fun spawnSiblingTask(
        referenceTaskId: String,
        title: String,
        priority: com.neuroflow.app.domain.model.Priority = com.neuroflow.app.domain.model.Priority.MEDIUM
    ): TaskEntity {
        val refTask = taskRepository.getById(referenceTaskId)
        val newTask = if (refTask != null) {
            TaskEntity(
                title = title,
                parentTaskId = refTask.parentTaskId,
                goalId = refTask.goalId,
                priority = priority
            )
        } else {
            TaskEntity(
                title = title,
                priority = priority
            )
        }
        taskRepository.insert(newTask)
        return newTask
    }
}
