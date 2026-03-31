package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.launcher.hyperfocus.domain.HyperFocusManager
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private fun Long.toDayStart(): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = this@toDayStart }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun Calendar.addRecurrenceStep(recurrence: Recurrence, customDays: Int) {
    when (recurrence) {
        Recurrence.DAILY -> add(Calendar.DAY_OF_YEAR, 1)
        Recurrence.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
        Recurrence.MONTHLY -> add(Calendar.MONTH, 1)
        Recurrence.CUSTOM -> add(Calendar.DAY_OF_YEAR, customDays.coerceAtLeast(1))
        Recurrence.NONE -> Unit
    }
}

private fun nextRecurringAnchorAfter(
    recurrence: Recurrence,
    customDays: Int,
    currentAnchor: Long,
    now: Long
): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = currentAnchor }
    // Always move to the next cycle, then roll forward if the anchor is still in the past.
    cal.addRecurrenceStep(recurrence, customDays)
    while (cal.timeInMillis <= now) {
        cal.addRecurrenceStep(recurrence, customDays)
    }
    return cal.timeInMillis
}

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val hyperFocusManager: Lazy<HyperFocusManager>
) {
    fun observeAll(): Flow<List<TaskEntity>> = taskDao.observeAll()
    fun observeActiveTasks(): Flow<List<TaskEntity>> = taskDao.observeActiveTasks()
    fun observeCompletedTasks(): Flow<List<TaskEntity>> = taskDao.observeCompletedTasks()
    fun observeByQuadrant(quadrant: Quadrant): Flow<List<TaskEntity>> = taskDao.observeByQuadrant(quadrant)
    fun observeByStatus(status: TaskStatus): Flow<List<TaskEntity>> = taskDao.observeByStatus(status)
    fun observeTasksForDate(date: Long): Flow<List<TaskEntity>> {
        val dayStart = date.toDayStart()
        val dayEnd = dayStart + 86_400_000L
        return taskDao.observeTasksForDate(dayStart, dayEnd)
    }
    fun observeQuadrantCount(quadrant: Quadrant): Flow<Int> = taskDao.observeQuadrantCount(quadrant)
    fun observeById(id: String): Flow<TaskEntity?> = taskDao.observeById(id)
    fun observeByContextTag(tag: String): Flow<List<TaskEntity>> = taskDao.observeByContextTag(tag)
    fun observeByGoalId(goalId: String): Flow<List<TaskEntity>> = taskDao.observeByGoalId(goalId)
    fun observeSubtasks(parentId: String): Flow<List<TaskEntity>> = taskDao.observeSubtasks(parentId)

    suspend fun getById(id: String): TaskEntity? = taskDao.getById(id)
    suspend fun getActiveTasks(): List<TaskEntity> = taskDao.getActiveTasks()
    suspend fun getAllTasks(): List<TaskEntity> = taskDao.getAllTasks()
    suspend fun getCompletedTasks(): List<TaskEntity> = taskDao.getCompletedTasks()

    suspend fun insert(task: TaskEntity) = taskDao.insert(task)
    suspend fun update(task: TaskEntity) = taskDao.update(task)
    suspend fun delete(task: TaskEntity) = taskDao.delete(task)
    suspend fun deleteAll() = taskDao.deleteAll()
    suspend fun resetEstimationErrors() = taskDao.resetEstimationErrors()

    /**
     * Marks [task] as completed and, if it has recurrence, inserts the next occurrence.
     * Uses calendar-aware stepping (daily/weekly/monthly/custom days) and guarantees
     * the new occurrence anchor lands strictly in the future.
     */
    suspend fun completeAndRecur(task: TaskEntity, now: Long): String? {
        update(
            task.copy(
                status = TaskStatus.COMPLETED,
                completedAt = now,
                isHabitual = task.recurrence != Recurrence.NONE,
                habitStreak = if (task.recurrence != Recurrence.NONE) task.habitStreak + 1 else task.habitStreak,
                updatedAt = now
            )
        )
        hyperFocusManager.get().onTaskCompleted()

        if (task.recurrence == Recurrence.NONE) return null

        // Anchor for recurrence progression: prefer habitDate, then scheduled/deadline, then now.
        val currentAnchor = task.habitDate ?: task.scheduledDate ?: task.deadlineDate ?: now
        val nextAnchor = nextRecurringAnchorAfter(
            recurrence = task.recurrence,
            customDays = task.recurrenceIntervalDays,
            currentAnchor = currentAnchor,
            now = now
        )
        val deltaMs = nextAnchor - currentAnchor

        val newId = UUID.randomUUID().toString()
        insert(
            task.copy(
                id = newId,
                status = TaskStatus.ACTIVE,
                completedAt = null,
                habitDate = nextAnchor,
                deadlineDate = task.deadlineDate?.plus(deltaMs),
                scheduledDate = task.scheduledDate?.plus(deltaMs),
                isScheduleLocked = task.isScheduleLocked,
                totalTimeTrackedMinutes = 0f,
                sessionCount = 0,
                lastSessionDurationMinutes = null,
                actualDurationMinutes = null,
                estimationErrorMape = null,
                estimationErrorSmape = null,
                focusModePoints = 0,
                postponeCount = 0,
                habitStreak = task.habitStreak + 1,
                isHabitual = true,
                createdAt = now,
                updatedAt = now
            )
        )
        return newId
    }
}
