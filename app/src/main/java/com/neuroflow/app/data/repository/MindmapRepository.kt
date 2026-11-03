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
}
