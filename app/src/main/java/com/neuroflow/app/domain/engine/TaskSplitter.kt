package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.TaskStatus

object TaskSplitter {
    /**
     * Splits [task] into 3 sub-tasks with titles "Part 1/2/3 of [title]".
     * Each sub-task inherits the parent's quadrant, priority, and impact score,
     * and has [task.id] set as [parentTaskId].
     * The original task is archived so it no longer competes in the active list.
     */
    suspend fun split(
        task: TaskEntity,
        taskRepository: TaskRepository,
        sequentialDependencies: Boolean = false
    ): List<TaskEntity> {
        val now = System.currentTimeMillis()
        val baseSubtasks = (1..3).map { part ->
            TaskEntity(
                title = "Part $part/3 of ${task.title}",
                quadrant = task.quadrant,
                priority = task.priority,
                impactScore = task.impactScore / 3,
                parentTaskId = task.id,
                createdAt = now,
                updatedAt = now
            )
        }
        val subtasks = if (!sequentialDependencies) {
            baseSubtasks
        } else {
            baseSubtasks.mapIndexed { index, subtask ->
                if (index == 0) subtask else subtask.copy(dependsOnTaskIds = baseSubtasks[index - 1].id)
            }
        }
        subtasks.forEach { taskRepository.insert(it) }
        // Archive the parent so it doesn't remain active alongside its subtasks
        taskRepository.update(task.copy(status = TaskStatus.ARCHIVED, updatedAt = System.currentTimeMillis()))
        return subtasks
    }
}

