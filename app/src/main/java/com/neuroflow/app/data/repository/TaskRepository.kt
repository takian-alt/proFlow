package com.neuroflow.app.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.presentation.launcher.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.presentation.launcher.work.ScheduleAutoTasksWorker
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
    if (recurrence == Recurrence.NONE) return currentAnchor
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
    private val hyperFocusManager: Lazy<HyperFocusManager>,
    private val workManager: WorkManager
) {
    fun observeAll(): Flow<List<TaskEntity>> = taskDao.observeAll()
    fun observeActiveTasks(): Flow<List<TaskEntity>> = taskDao.observeActiveTasks()
    fun observeCompletedTasks(): Flow<List<TaskEntity>> = taskDao.observeCompletedTasks()
    fun observeByQuadrant(quadrant: Quadrant): Flow<List<TaskEntity>> = taskDao.observeByQuadrant(quadrant)
    fun observeByStatus(status: TaskStatus): Flow<List<TaskEntity>> = taskDao.observeByStatus(status)
    fun observeTasksForDate(date: Long): Flow<List<TaskEntity>> {
        val dayStart = date.toDayStart()
        val dayEnd = Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
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

    suspend fun insert(task: TaskEntity) {
        taskDao.insert(task)
        if (!AutoSchedulingContracts.hasManualScheduleData(task) && task.status != TaskStatus.COMPLETED) {
            enqueueAutoSchedulingRecheck()
        }
    }
    suspend fun update(task: TaskEntity) {
        val old = taskDao.getById(task.id)
        // plannedBlockMinutes is the length the planner reserved, and TaskBlockPlan prefers it over
        // the estimate. Left stale it silently overrides a new duration until the task happens to be
        // replanned, so a reschedule that never fires keeps blocking and expiring the old block.
        val estimateChanged = old != null && old.estimatedDurationMinutes != task.estimatedDurationMinutes
        val toWrite = if (estimateChanged) task.copy(plannedBlockMinutes = 0) else task
        taskDao.update(toWrite)

        // Trigger rescheduling if relevant fields changed
        if (old != null && shouldTriggerReschedule(old, toWrite)) {
            enqueueAutoSchedulingRecheck()
        }
    }

    private fun shouldTriggerReschedule(old: TaskEntity, new: TaskEntity): Boolean {
        return old.deadlineDate != new.deadlineDate ||
               old.deadlineTime != new.deadlineTime ||
               old.priority != new.priority ||
               old.estimatedDurationMinutes != new.estimatedDurationMinutes ||
               old.effortScore != new.effortScore ||
               old.tags != new.tags ||
               old.taskType != new.taskType ||
               old.energyLevel != new.energyLevel ||
               // Task was unscheduled (manual or incomplete)
               (old.scheduledDate != null && new.scheduledDate == null)
    }

    suspend fun markIncomplete(task: TaskEntity, timeSpentMinutes: Int) {
        update(task.copy(
            scheduledDate = null,
            scheduledTime = null,
            totalTimeTrackedMinutes = task.totalTimeTrackedMinutes + timeSpentMinutes,
            updatedAt = System.currentTimeMillis()
        ))
        // Rescheduling trigger already called in update()
    }

    suspend fun postponeTask(task: TaskEntity) {
        update(task.copy(
            postponeCount = task.postponeCount + 1,
            scheduledDate = null,
            scheduledTime = null,
            updatedAt = System.currentTimeMillis()
        ))
        // Rescheduling trigger already called in update()
    }

    /**
     * Moves a task to tomorrow immediately, releasing the time-of-day so the auto-scheduler
     * claims it. A date-only change is not covered by shouldTriggerReschedule, so the recheck
     * is enqueued here.
     */
    suspend fun rescheduleToTomorrow(task: TaskEntity) {
        val tomorrowStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        update(task.copy(
            postponeCount = task.postponeCount + 1,
            scheduledDate = tomorrowStart,
            scheduledTime = null,
            isAutoScheduled = false,
            updatedAt = System.currentTimeMillis()
        ))
        enqueueAutoSchedulingRecheck()
    }
    suspend fun delete(task: TaskEntity) {
        if (hyperFocusManager.get().isTaskDeletionBlocked(task.id)) return
        taskDao.delete(task)
        // Clean up dependencies referencing this deleted task to avoid orphan deadlocks
        val activeTasks = taskDao.getActiveTasks()
        activeTasks.forEach { other ->
            if (other.dependsOnTaskIds.isNotBlank()) {
                val currentDeps = other.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (task.id in currentDeps) {
                    val remainingDeps = currentDeps.filter { it != task.id }
                    taskDao.update(
                        other.copy(
                            dependsOnTaskIds = remainingDeps.joinToString(","),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        enqueueAutoSchedulingRecheck()
    }
    suspend fun deleteAll() = taskDao.deleteAll()
    suspend fun resetEstimationErrors() = taskDao.resetEstimationErrors()

    /**
     * Splits a task into multiple sequential sessions (e.g. 45-min chunks).
     * Connects sessions via `parentTaskId` and sequential `dependsOnTaskIds`.
     * Marks the parent task as completed/archived and triggers auto-scheduling.
     */
    suspend fun splitTaskIntoSessions(
        task: TaskEntity,
        sessionDurationMinutes: Int = 45
    ): List<TaskEntity> {
        val totalMinutes = if (task.estimatedDurationMinutes > 0) {
            task.estimatedDurationMinutes
        } else {
            90
        }

        val effectiveSessionMinutes = sessionDurationMinutes.coerceIn(15, 120)
        val sessionCount = maxOf(2, ((totalMinutes + effectiveSessionMinutes - 1) / effectiveSessionMinutes))
        val minutesPerSession = (totalMinutes / sessionCount).coerceAtLeast(15)
        val remainder = totalMinutes - (minutesPerSession * (sessionCount - 1))

        val now = System.currentTimeMillis()
        val createdSessions = mutableListOf<TaskEntity>()
        var previousSessionId = ""

        for (i in 1..sessionCount) {
            val duration = if (i == sessionCount) remainder.coerceAtLeast(15) else minutesPerSession
            val sessionTask = TaskEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = "${task.title} (Part $i/$sessionCount)",
                description = task.description,
                quadrant = task.quadrant,
                priority = task.priority,
                deadlineDate = task.deadlineDate,
                deadlineTime = task.deadlineTime,
                estimatedDurationMinutes = duration,
                parentTaskId = task.id,
                dependsOnTaskIds = previousSessionId,
                impactScore = task.impactScore,
                valueScore = task.valueScore,
                effortScore = task.effortScore,
                taskType = task.taskType,
                energyLevel = task.energyLevel,
                contextTag = task.contextTag,
                goalId = task.goalId,
                isFrog = if (i == 1) task.isFrog else false,
                tags = task.tags,
                ifThenPlan = task.ifThenPlan,
                status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
                createdAt = now + i,
                updatedAt = now + i
            )
            createdSessions.add(sessionTask)
            taskDao.insert(sessionTask)
            previousSessionId = sessionTask.id
        }

        // Archive parent task as completed
        taskDao.update(
            task.copy(
                status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED,
                completedAt = now,
                updatedAt = now
            )
        )

        enqueueAutoSchedulingRecheck()
        return createdSessions
    }

    private fun enqueueAutoSchedulingRecheck() {
        val request = OneTimeWorkRequestBuilder<ScheduleAutoTasksWorker>().build()
        workManager.enqueueUniqueWork(
            "${ScheduleAutoTasksWorker.WORK_NAME}_trigger",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

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
        enqueueAutoSchedulingRecheck()

        if (task.recurrence == Recurrence.NONE) return null

        // Anchor for recurrence progression: prefer habitDate, then scheduled/deadline with time, then now.
        val currentAnchor =
            task.habitDate
                ?: task.scheduledDate?.let { it + (task.scheduledTime ?: 0L) }
                ?: task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) }
                ?: now
        val nextAnchor = nextRecurringAnchorAfter(
            recurrence = task.recurrence,
            customDays = task.recurrenceIntervalDays,
            currentAnchor = currentAnchor,
            now = now
        )
        val nextDayStart = nextAnchor.toDayStart()
        val (nextScheduledDate, nextScheduledTime) = if (task.scheduledDate != null) {
            nextDayStart to task.scheduledTime
        } else {
            null to null
        }
        val (nextDeadlineDate, nextDeadlineTime) = if (task.deadlineDate != null) {
            val anchorDate = Instant.ofEpochMilli(startOfLocalDay(currentAnchor))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val deadlineDate = Instant.ofEpochMilli(startOfLocalDay(task.deadlineDate))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val deadlineDayOffset = ChronoUnit.DAYS.between(anchorDate, deadlineDate)
                .coerceAtLeast(0L)
            val nextDeadline = Calendar.getInstance().apply {
                timeInMillis = nextDayStart
                add(Calendar.DAY_OF_YEAR, deadlineDayOffset.toInt())
            }.timeInMillis
            nextDeadline to task.deadlineTime
        } else {
            null to null
        }

        val newId = UUID.randomUUID().toString()
        insert(
            task.copy(
                id = newId,
                status = TaskStatus.ACTIVE,
                completedAt = null,
                habitDate = nextAnchor.toDayStart(),
                deadlineDate = nextDeadlineDate,
                deadlineTime = nextDeadlineTime,
                scheduledDate = nextScheduledDate,
                scheduledTime = nextScheduledTime,
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
