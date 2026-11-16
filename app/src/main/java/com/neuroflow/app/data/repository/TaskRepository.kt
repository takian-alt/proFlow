package com.neuroflow.app.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.dao.FocusSessionDao
import com.neuroflow.app.data.local.dao.MindmapDao
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.dao.TimeSessionDao
import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.worker.ScheduleAutoTasksWorker
import com.neuroflow.app.worker.TaskReminderWorker
import com.neuroflow.app.data.local.dao.TaskMicroStepDao
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
    private val workManager: WorkManager,
    private val taskMicroStepDao: TaskMicroStepDao? = null,
    private val calendarSyncRepository: CalendarSyncRepository? = null,
    private val timeSessionDao: TimeSessionDao? = null,
    private val focusSessionDao: FocusSessionDao? = null,
    private val mindmapDao: MindmapDao? = null
) {
    // Secondary constructors for tests or callers providing subsets of dependencies
    constructor(
        taskDao: TaskDao,
        hyperFocusManager: Lazy<HyperFocusManager>,
        workManager: WorkManager
    ) : this(taskDao, hyperFocusManager, workManager, null, null, null, null)

    constructor(
        taskDao: TaskDao,
        hyperFocusManager: Lazy<HyperFocusManager>,
        workManager: WorkManager,
        taskMicroStepDao: TaskMicroStepDao?
    ) : this(taskDao, hyperFocusManager, workManager, taskMicroStepDao, null, null, null)

    constructor(
        taskDao: TaskDao,
        hyperFocusManager: Lazy<HyperFocusManager>,
        workManager: WorkManager,
        taskMicroStepDao: TaskMicroStepDao?,
        calendarSyncRepository: CalendarSyncRepository?
    ) : this(taskDao, hyperFocusManager, workManager, taskMicroStepDao, calendarSyncRepository, null, null)

    constructor(
        taskDao: TaskDao,
        hyperFocusManager: Lazy<HyperFocusManager>,
        workManager: WorkManager,
        taskMicroStepDao: TaskMicroStepDao?,
        calendarSyncRepository: CalendarSyncRepository?,
        timeSessionDao: TimeSessionDao?
    ) : this(taskDao, hyperFocusManager, workManager, taskMicroStepDao, calendarSyncRepository, timeSessionDao, null)

    fun observeMicroSteps(taskId: String): Flow<List<TaskMicroStepEntity>> =
        taskMicroStepDao?.observeByTaskId(taskId) ?: flowOf(emptyList())

    suspend fun getMicroSteps(taskId: String): List<TaskMicroStepEntity> {
        val dbSteps = taskMicroStepDao?.getByTaskId(taskId) ?: emptyList()
        if (dbSteps.isNotEmpty()) return dbSteps

        // Fallback: lazily extract micro-steps from task.ifThenPlan if task has plan steps but not yet stored in DAO
        val task = taskDao.getById(taskId) ?: return emptyList()
        if (task.ifThenPlan.isBlank()) return emptyList()
        val parsed = com.neuroflow.app.domain.model.StepByStepPlanCodec.parse(task.ifThenPlan)
        if (parsed.isEmpty()) return emptyList()
        val entities = parsed.mapIndexed { index, step ->
            TaskMicroStepEntity(
                id = "${task.id}_step_$index",
                taskId = task.id,
                title = step.text,
                sortOrder = index,
                isCompleted = step.completed,
                estimatedMinutes = 5
            )
        }
        runCatching { taskMicroStepDao?.insertAll(entities) }
        return entities
    }

    suspend fun insertMicroStep(step: TaskMicroStepEntity) {
        taskMicroStepDao?.insert(step)
    }

    suspend fun insertMicroSteps(steps: List<TaskMicroStepEntity>) {
        taskMicroStepDao?.insertAll(steps)
    }

    suspend fun updateMicroStep(step: TaskMicroStepEntity) {
        taskMicroStepDao?.update(step)
    }

    suspend fun deleteMicroStep(step: TaskMicroStepEntity) {
        taskMicroStepDao?.delete(step)
    }

    suspend fun deleteMicroStepsByTaskId(taskId: String) {
        taskMicroStepDao?.deleteByTaskId(taskId)
    }

    suspend fun toggleMicroStepCompleted(step: TaskMicroStepEntity) {
        taskMicroStepDao?.update(step.copy(isCompleted = !step.isCompleted))
    }
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
    suspend fun getTasksForDate(date: Long): List<TaskEntity> {
        val dayStart = date.toDayStart()
        val dayEnd = Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        return taskDao.getTasksForDate(dayStart, dayEnd)
    }
    fun observeQuadrantCount(quadrant: Quadrant): Flow<Int> = taskDao.observeQuadrantCount(quadrant)
    fun observeById(id: String): Flow<TaskEntity?> = taskDao.observeById(id)
    fun observeByContextTag(tag: String): Flow<List<TaskEntity>> = taskDao.observeByContextTag(tag)
    fun observeByGoalId(goalId: String): Flow<List<TaskEntity>> = taskDao.observeByGoalId(goalId)
    fun observeSubtasks(parentId: String): Flow<List<TaskEntity>> = taskDao.observeSubtasks(parentId)

    suspend fun getById(id: String): TaskEntity? {
        val existing = taskDao.getById(id)
        if (existing != null) return existing
        if (id == TaskEntity.TASK_UNTRACKED_FOCUS) {
            return ensureUntrackedFocusTask()
        }
        return null
    }

    suspend fun ensureUntrackedFocusTask(): TaskEntity {
        val existing = taskDao.getById(TaskEntity.TASK_UNTRACKED_FOCUS)
        if (existing != null) return existing
        val synthetic = TaskEntity.createUntrackedFocusTask()
        taskDao.insert(synthetic)
        return synthetic
    }
    suspend fun getActiveTasks(): List<TaskEntity> = taskDao.getActiveTasks()
    suspend fun getAllTasks(): List<TaskEntity> = taskDao.getAllTasks()
    suspend fun getCompletedTasks(): List<TaskEntity> = taskDao.getCompletedTasks()

    suspend fun insert(task: TaskEntity) {
        taskDao.insert(task)
        if (task.status == TaskStatus.ACTIVE) {
            TaskReminderWorker.scheduleReminder(workManager, task)
            AutonomyNudgeEngine.scheduleNudge(workManager, task)
        }
        calendarSyncRepository?.handleTaskStateChanged(task)
        if (!AutoSchedulingContracts.hasManualScheduleData(task) && task.status != TaskStatus.COMPLETED && !task.isAutoScheduled) {
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

        if (toWrite.status == TaskStatus.ACTIVE) {
            TaskReminderWorker.scheduleReminder(workManager, toWrite)
            AutonomyNudgeEngine.scheduleNudge(workManager, toWrite)
        } else {
            TaskReminderWorker.cancelReminder(workManager, toWrite.id)
            AutonomyNudgeEngine.cancelNudge(workManager, toWrite.id)
            if (toWrite.status == TaskStatus.COMPLETED || toWrite.status == TaskStatus.ARCHIVED) {
                // Clean up orphaned continuation blocks when parent task is completed or archived
                val part2Underscore = taskDao.getById("${toWrite.id}_part2")
                if (part2Underscore != null && part2Underscore.status != TaskStatus.COMPLETED) {
                    taskDao.delete(part2Underscore)
                    calendarSyncRepository?.deleteTaskEvent(part2Underscore.id)
                }
                val part2Hash = taskDao.getById("${toWrite.id}#part2")
                if (part2Hash != null && part2Hash.status != TaskStatus.COMPLETED) {
                    taskDao.delete(part2Hash)
                    calendarSyncRepository?.deleteTaskEvent(part2Hash.id)
                }
            }
        }

        calendarSyncRepository?.handleTaskStateChanged(toWrite)

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
               old.quadrant != new.quadrant ||
               old.isScheduleLocked != new.isScheduleLocked ||
               old.isFrog != new.isFrog ||
               old.dependsOnTaskIds != new.dependsOnTaskIds ||
               old.waitingFor != new.waitingFor ||
               old.scheduledDate != new.scheduledDate ||
               old.scheduledTime != new.scheduledTime ||
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
            habitDate = if (task.recurrence != Recurrence.NONE) tomorrowStart else task.habitDate,
            updatedAt = System.currentTimeMillis()
        ))
        enqueueAutoSchedulingRecheck()
    }
    suspend fun delete(task: TaskEntity) {
        if (hyperFocusManager.get().isTaskDeletionBlocked(task.id)) return

        val deletedIds = mutableSetOf(task.id)

        // Find continuation Part 2 or Part 1 if present (BUG-012, BUG-329)
        val part2Underscore = taskDao.getById("${task.id}_part2")
        if (part2Underscore != null) deletedIds.add(part2Underscore.id)

        val part2Hash = taskDao.getById("${task.id}#part2")
        if (part2Hash != null) deletedIds.add(part2Hash.id)

        val part1Hash = taskDao.getById("${task.id}#part1")
        if (part1Hash != null) deletedIds.add(part1Hash.id)

        val part1Underscore = taskDao.getById("${task.id}_part1")
        if (part1Underscore != null) deletedIds.add(part1Underscore.id)

        // Find all subtasks referencing this task as parent
        val subtasks = taskDao.getSubtasks(task.id)
        deletedIds.addAll(subtasks.map { it.id })

        // Find all multi-part continuation parts and subtasks dynamically (BUG-428)
        val continuationPartsAndSubtasks = runCatching { taskDao.getContinuationPartsAndSubtasks(task.id) }.getOrDefault(emptyList())
        deletedIds.addAll(continuationPartsAndSubtasks.map { it.id })

        // Clean up external resources and reminders for all deleted tasks
        deletedIds.forEach { id ->
            taskMicroStepDao?.deleteByTaskId(id)
            calendarSyncRepository?.deleteTaskEvent(id)
            TaskReminderWorker.cancelReminder(workManager, id)
            AutonomyNudgeEngine.cancelNudge(workManager, id)
            timeSessionDao?.deleteOpenSessionsByTaskId(id)
            runCatching { focusSessionDao?.deleteOpenSessionsByTaskId(id) }
            mindmapDao?.deletePositionByEntityId(id)
            mindmapDao?.deleteEdgesForEntity(id)
        }

        taskDao.delete(task)
        if (part2Underscore != null) taskDao.delete(part2Underscore)
        if (part2Hash != null) taskDao.delete(part2Hash)
        if (part1Hash != null) taskDao.delete(part1Hash)
        if (part1Underscore != null) taskDao.delete(part1Underscore)
        continuationPartsAndSubtasks.forEach { related ->
            if (related.id !in setOf(task.id, part2Underscore?.id, part2Hash?.id, part1Hash?.id, part1Underscore?.id)) {
                taskDao.delete(related)
            }
        }
        taskDao.deleteSubtasksByParentId(task.id)
        runCatching { taskDao.deleteContinuationPartsAndSubtasks(task.id) }

        // Inverse restore parent task if a continuation part is directly deleted (BUG-029, BUG-394)
        if (task.id.contains("#part") || task.id.contains("_part") || task.parentTaskId != null) {
            val parentId = task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
            val parent = taskDao.getById(parentId)
            if (parent != null && parent.status == TaskStatus.ACTIVE && parent.title.contains("(Part ")) {
                val cleanTitle = parent.title.replace(Regex("""\s*\(Part \d+/\d+\)"""), "").trim()
                taskDao.update(
                    parent.copy(
                        title = cleanTitle,
                        plannedBlockMinutes = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        // Clean up dependencies referencing any deleted tasks (parent, part2, or subtasks)
        val activeTasks = taskDao.getActiveTasks()
        activeTasks.forEach { other ->
            if (other.dependsOnTaskIds.isNotBlank() && other.id !in deletedIds) {
                val currentDeps = other.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (currentDeps.any { it in deletedIds }) {
                    val remainingDeps = currentDeps.filter { it !in deletedIds }
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
    suspend fun deleteAll() {
        taskDao.deleteAll()
        runCatching { taskMicroStepDao?.deleteAll() }
        runCatching { timeSessionDao?.deleteAll() }
    }
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
                scheduledDate = if (i == 1) task.scheduledDate else null,
                scheduledTime = if (i == 1) task.scheduledTime else null,
                estimatedDurationMinutes = duration,
                plannedBlockMinutes = duration,
                parentTaskId = task.id,
                dependsOnTaskIds = if (i == 1) task.dependsOnTaskIds else previousSessionId,
                waitingFor = if (i == 1) task.waitingFor else "",
                impactScore = task.impactScore,
                valueScore = task.valueScore,
                effortScore = task.effortScore,
                taskType = task.taskType,
                energyLevel = task.energyLevel,
                contextTag = task.contextTag,
                goalId = task.goalId,
                isFrog = if (i == 1) task.isFrog else false,
                tags = task.tags,
                ifThenPlan = if (i == 1) task.ifThenPlan else "",
                recurrence = Recurrence.NONE,
                isHabitual = false,
                status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
                createdAt = now + i,
                updatedAt = now + i
            )
            createdSessions.add(sessionTask)
            taskDao.insert(sessionTask)
            TaskReminderWorker.scheduleReminder(workManager, sessionTask)
            AutonomyNudgeEngine.scheduleNudge(workManager, sessionTask)
            calendarSyncRepository?.handleTaskStateChanged(sessionTask)
            previousSessionId = sessionTask.id
        }

        // Close any open session on the parent task to prevent orphaned session time (BUG-393, BUG-428)
        val openSession = timeSessionDao?.getOpenSessionForTask(task.id)
        if (openSession != null) {
            val tracked = openSession.trackedMinutes(now)
            timeSessionDao.update(openSession.copy(endedAt = now, durationMinutes = tracked))
            runCatching {
                focusSessionDao?.getById(openSession.id)?.let { fs ->
                    focusSessionDao.update(
                        fs.copy(
                            endedAt = now,
                            durationSeconds = (tracked * 60).toInt(),
                            wasCompleted = true
                        )
                    )
                }
            }
        }
        runCatching {
            val openFocusSession = focusSessionDao?.getOpenSessionForTask(task.id)
            if (openFocusSession != null && openFocusSession.endedAt == null) {
                val durationSec = maxOf(0, ((now - openFocusSession.startedAt) / 1000).toInt())
                focusSessionDao.update(
                    openFocusSession.copy(
                        endedAt = now,
                        durationSeconds = durationSec,
                        wasCompleted = true
                    )
                )
            }
        }

        // Clone micro-steps from parent task to Part 1 if present (BUG-396, BUG-431)
        val parentSteps = getMicroSteps(task.id)
        if (parentSteps.isNotEmpty()) {
            val part1Id = createdSessions.first().id
            val cloned = parentSteps.map { step ->
                step.copy(
                    id = UUID.randomUUID().toString(),
                    taskId = part1Id,
                    isCompleted = step.isCompleted
                )
            }
            taskMicroStepDao?.insertAll(cloned)
        }

        // Archive parent task as completed and advance recurrence if recurring
        completeAndRecur(task, now)

        // Redirect dependencies from parent task to final split session so downstream tasks don't unlock prematurely
        val lastSessionId = createdSessions.last().id
        val activeTasks = taskDao.getActiveTasks()
        val createdSessionIds = createdSessions.map { it.id }.toSet()
        activeTasks.forEach { other ->
            if (other.dependsOnTaskIds.isNotBlank() && other.id !in createdSessionIds) {
                val currentDeps = other.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (task.id in currentDeps) {
                    val redirectedDeps = currentDeps.map { if (it == task.id) lastSessionId else it }
                    taskDao.update(
                        other.copy(
                            dependsOnTaskIds = redirectedDeps.joinToString(","),
                            updatedAt = now
                        )
                    )
                }
            }
        }

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

    suspend fun markTaskCompleted(taskId: String, now: Long = System.currentTimeMillis()): Boolean {
        val task = getById(taskId) ?: return false
        if (task.status == TaskStatus.COMPLETED) return false
        completeAndRecur(task, now)
        return true
    }

    /**
     * Marks [task] as completed and, if it has recurrence, inserts the next occurrence.
     * Uses calendar-aware stepping (daily/weekly/monthly/custom days) and guarantees
     * the new occurrence anchor lands strictly in the future.
     */
    suspend fun completeAndRecur(task: TaskEntity, now: Long): String? {
        if (task.status == TaskStatus.COMPLETED) return null

        // Close any open session on the task to prevent zombie open sessions and preserve tracked focus minutes (BUG-393, BUG-428)
        val openSession = timeSessionDao?.getOpenSessionForTask(task.id)
        if (openSession != null) {
            val tracked = openSession.trackedMinutes(now)
            timeSessionDao.update(openSession.copy(endedAt = now, durationMinutes = tracked))
            runCatching {
                focusSessionDao?.getById(openSession.id)?.let { fs ->
                    focusSessionDao.update(
                        fs.copy(
                            endedAt = now,
                            durationSeconds = (tracked * 60).toInt(),
                            wasCompleted = true
                        )
                    )
                }
            }
        }
        runCatching {
            val openFocusSession = focusSessionDao?.getOpenSessionForTask(task.id)
            if (openFocusSession != null && openFocusSession.endedAt == null) {
                val durationSec = maxOf(0, ((now - openFocusSession.startedAt) / 1000).toInt())
                focusSessionDao.update(
                    openFocusSession.copy(
                        endedAt = now,
                        durationSeconds = durationSec,
                        wasCompleted = true
                    )
                )
            }
        }

        val completedTask = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = task.completedAt ?: now,
            isHabitual = task.recurrence != Recurrence.NONE,
            habitStreak = if (task.recurrence != Recurrence.NONE) task.habitStreak + 1 else task.habitStreak,
            updatedAt = now
        )
        update(completedTask)
        hyperFocusManager.get().onTaskCompleted()
        enqueueAutoSchedulingRecheck()

        // Continuation parts and sub-tasks must NEVER advance recurrence (BUG-020, BUG-395, BUG-427)
        if (task.id.contains(Regex("""[#_]part[2-9]\d*""")) ||
            task.title.contains(Regex("""\((Part|part) [2-9]\d*/\d+\)""")) ||
            task.parentTaskId != null
        ) return null
        if (task.recurrence == Recurrence.NONE) return null

        // Anchor for recurrence progression: prefer habitDate, then scheduled/deadline with time, then now.
        val currentAnchor =
            task.habitDate
                ?: task.resolveScheduledEpochMillis()
                ?: task.resolveDeadlineEpochMillis()
                ?: now
        val nextAnchor = nextRecurringAnchorAfter(
            recurrence = task.recurrence,
            customDays = task.recurrenceIntervalDays,
            currentAnchor = currentAnchor,
            now = now
        )
        val nextDayStart = nextAnchor.toDayStart()
        val normalizedScheduledTime = task.scheduledTime?.let { raw ->
            if (raw >= ONE_DAY_MILLIS) {
                val zdt = Instant.ofEpochMilli(raw).atZone(ZoneId.systemDefault())
                (zdt.hour * 3600_000L + zdt.minute * 60_000L + zdt.second * 1000L)
            } else raw
        }
        val nextScheduledDate = if (task.scheduledDate != null || (task.scheduledTime != null && task.scheduledTime >= ONE_DAY_MILLIS)) nextDayStart else null
        val nextScheduledTime = normalizedScheduledTime
        val (nextDeadlineDate, nextDeadlineTime) = if (task.deadlineDate != null || (task.deadlineTime != null && task.deadlineTime >= ONE_DAY_MILLIS)) {
            val effectiveDeadlineDateMillis = task.deadlineDate
                ?: (if (task.deadlineTime != null && task.deadlineTime >= ONE_DAY_MILLIS) startOfLocalDay(task.deadlineTime) else null)
                ?: currentAnchor
            val anchorDate = Instant.ofEpochMilli(startOfLocalDay(currentAnchor))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val deadlineDate = Instant.ofEpochMilli(startOfLocalDay(effectiveDeadlineDateMillis))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val deadlineDayOffset = ChronoUnit.DAYS.between(anchorDate, deadlineDate)
                .coerceAtLeast(0L)
            val nextDeadline = Calendar.getInstance().apply {
                timeInMillis = nextDayStart
                add(Calendar.DAY_OF_YEAR, deadlineDayOffset.toInt())
            }.timeInMillis
            val normalizedDeadlineTime = task.deadlineTime?.let { raw ->
                if (raw >= ONE_DAY_MILLIS) {
                    val zdt = Instant.ofEpochMilli(raw).atZone(ZoneId.systemDefault())
                    (zdt.hour * 3600_000L + zdt.minute * 60_000L + zdt.second * 1000L)
                } else raw
            }
            nextDeadline to normalizedDeadlineTime
        } else {
            val normalizedDeadlineTime = task.deadlineTime?.let { raw ->
                if (raw >= ONE_DAY_MILLIS) {
                    val zdt = Instant.ofEpochMilli(raw).atZone(ZoneId.systemDefault())
                    (zdt.hour * 3600_000L + zdt.minute * 60_000L + zdt.second * 1000L)
                } else raw
            }
            null to normalizedDeadlineTime
        }

        val newId = UUID.randomUUID().toString()
        val cleanTitle = task.title
            .replace(Regex("""\s*\(Part \d+/\d+\)"""), "")
            .replace(Regex("""\s*#part\d+"""), "")
            .trim()
        val resetIfThenPlan = if (task.ifThenPlan.isNotBlank()) {
            val steps = com.neuroflow.app.domain.model.StepByStepPlanCodec.parse(task.ifThenPlan)
            val resetSteps = steps.map { it.copy(completed = false) }
            com.neuroflow.app.domain.model.StepByStepPlanCodec.serialize(resetSteps)
        } else task.ifThenPlan

        val spawnedTask = task.copy(
            id = newId,
            title = cleanTitle,
            plannedBlockMinutes = 0,
            ifThenPlan = resetIfThenPlan,
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
        insert(spawnedTask)
        taskMicroStepDao?.let { dao ->
            val existingSteps = dao.getByTaskId(task.id)
            if (existingSteps.isNotEmpty()) {
                val cloned = existingSteps.map { step ->
                    step.copy(
                        id = UUID.randomUUID().toString(),
                        taskId = newId,
                        isCompleted = false
                    )
                }
                dao.insertAll(cloned)
            } else if (task.ifThenPlan.isNotBlank()) {
                val parsed = com.neuroflow.app.domain.model.StepByStepPlanCodec.parse(task.ifThenPlan)
                if (parsed.isNotEmpty()) {
                    val seeded = parsed.mapIndexed { index, step ->
                        TaskMicroStepEntity(
                            id = "${newId}_step_$index",
                            taskId = newId,
                            title = step.text,
                            sortOrder = index,
                            isCompleted = false,
                            estimatedMinutes = 5
                        )
                    }
                    dao.insertAll(seeded)
                }
            }
        }
        return newId
    }
}
