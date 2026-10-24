package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class ScheduleAutoTasksWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val taskSchedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService,
    private val behavioralSupportService: BehavioralSupportService,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sessionRepository: SessionRepository,
    private val calendarSyncRepository: CalendarSyncRepository? = null,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(context, params) {

    private val scheduleOrchestrator = SchedulePreparationOrchestrator(
        taskSchedulerService = taskSchedulerService,
        behavioralSupportService = behavioralSupportService,
        sleepTelemetryRepository = sleepTelemetryRepository
    )

    companion object {
        const val WORK_NAME = "schedule_auto_tasks_work"
        const val FOREGROUND_TICK_WORK_NAME = "schedule_auto_tasks_foreground_tick"

        @Volatile
        private var isWorkerRunning = false
        private val workerLock = Any()

        fun buildPeriodicWorkRequest(throttleMinutes: Int): PeriodicWorkRequest {
            val throttleInterval = throttleMinutes.coerceIn(15, 240)
            return PeriodicWorkRequest.Builder(
                ScheduleAutoTasksWorker::class.java,
                throttleInterval.toLong(),
                TimeUnit.MINUTES
            ).build()
        }

        fun buildOneTimeWorkRequest(forceReplan: Boolean = false): OneTimeWorkRequest {
            val data = Data.Builder().putBoolean("FORCE_REPLAN", forceReplan).build()
            return OneTimeWorkRequest.Builder(ScheduleAutoTasksWorker::class.java)
                .setInputData(data)
                .build()
        }

        /**
         * Anchors wake time to the exact local `:00` minute of [wakeHour] using DST-safe ZonedDateTime,
         * preventing the up-to-59-minute drift of raw hour subtraction.
         */
        internal fun calculateWakeTimeMillis(
            nowMillis: Long,
            wakeHour: Int,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val zonedNow = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
            var anchor = zonedNow.toLocalDate()
                .atTime(wakeHour.coerceIn(0, 23), 0)
                .atZone(zoneId)
            if (anchor.toInstant().toEpochMilli() > nowMillis) {
                anchor = anchor.minusDays(1)
            }
            return anchor.toInstant().toEpochMilli()
        }

        internal fun splitMillisToDateAndTime(millis: Long): Pair<Long, Long> {
            val dateStart = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateMillis = dateStart.timeInMillis
            val timeOffset = millis - dateMillis
            return dateMillis to timeOffset
        }
    }

    override suspend fun doWork(): Result {
        synchronized(workerLock) {
            if (isWorkerRunning) {
                android.util.Log.w(
                    "ScheduleAutoTasks",
                    "Worker already running - skipping execution"
                )
                return Result.success()
            }
            isWorkerRunning = true
        }

        return try {
            val forceReplan = inputData.getBoolean("FORCE_REPLAN", false)
            val prefs = preferencesDataStore.preferencesFlow.first()
            if (!prefs.autoSchedulingEnabled) {
                return Result.success()
            }

            val allKnownTasks = taskRepository.getAllTasks()
            val allTasks = allKnownTasks.filter { it.status == TaskStatus.ACTIVE }
            val nowMillis = System.currentTimeMillis()
            val blockedTaskIds = buildBlockedTaskIds(allTasks, allKnownTasks)
            val focusedTaskIds = sessionRepository.getOpenSessions().map { it.taskId }.toSet()

            val zoneId = ZoneId.systemDefault()
            val dayDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

            // Separate tasks into unscheduled, missed, and replan candidates
            val unscheduledTasks = allTasks.filter { task ->
                (task.scheduledDate == null || task.scheduledTime == null) &&
                    !AutoSchedulingContracts.hasManualScheduleData(task)
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            val gracePeriodMillis = if (prefs.isAutoScheduleToggleOn) 5 * 60_000L else 30 * 60_000L
            val missedTasks = allTasks.filter { task ->
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    task.isAutoScheduled &&
                    task.scheduledDate != null &&
                    task.scheduledTime != null &&
                    let {
                        val scheduledStart = task.scheduledDate + task.scheduledTime
                        TaskBlockPlan.blockHasExpired(
                            task = task,
                            startMillis = scheduledStart,
                            nowMillis = nowMillis,
                            gracePeriodMillis = gracePeriodMillis
                        ) || (forceReplan && scheduledStart < nowMillis)
                    }
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            val replanCandidates = allTasks.filter { task ->
                val scheduledMillis = task.resolveScheduledEpochMillis()
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    scheduledMillis != null &&
                    scheduledMillis >= nowMillis &&
                    PlacementStability.verdict(task, nowMillis, prefs, forceReplan).allowsReplan
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            val targetTasksMap = LinkedHashMap<String, TaskEntity>()
            (unscheduledTasks + missedTasks + replanCandidates).forEach { t ->
                val isDateOnlyAnchor = t.scheduledDate != null && t.scheduledTime == null && !t.isAutoScheduled
                targetTasksMap[t.id] = if (isDateOnlyAnchor) t else t.copy(scheduledDate = null, scheduledTime = null)
            }
            val targetTasks = targetTasksMap.values.toList()

            if (targetTasks.isEmpty()) {
                return Result.success()
            }

            // Fixed tasks are active tasks NOT being rescheduled (locked or manual future tasks)
            val fixedTasks = allTasks.filterNot { targetTasksMap.containsKey(it.id) }
            val fixedWindows = fixedTasks.mapNotNull { task ->
                val start = task.resolveScheduledEpochMillis() ?: return@mapNotNull null
                val duration = TaskBlockPlan.plannedMinutes(task)
                val end = start + duration * 60_000L
                if (end > nowMillis) TimeSlotWindow(start, end) else null
            }

            // Fetch external busy windows from Android Native Calendar Sync
            val dayStart = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = dayStart + 24 * 3600_000L - 1
            val externalBusy = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd) ?: emptyList()
            val totalBusyWindows = externalBusy + fixedWindows

            val allSessions = runCatching {
                sessionRepository.getAllSessions()
            }.getOrDefault(emptyList())
            val closedSessions = allSessions.filter { it.endedAt != null }
            val activeTaskId = allSessions.firstOrNull { it.endedAt == null }?.taskId
            val microStepCounts = targetTasks.associate { task ->
                val steps = taskRepository.getMicroSteps(task.id)
                task.id to Pair(steps.count { it.isCompleted }, steps.size)
            }
            val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.first() ?: emptyList()

            val placements = scheduleOrchestrator.orchestrateDaySchedule(
                candidateTasks = targetTasks,
                allKnownTasks = allKnownTasks,
                closedSessions = closedSessions,
                sleepLogs = sleepLogs,
                prefs = prefs,
                dayDate = dayDate,
                nowMillis = nowMillis,
                busyWindows = totalBusyWindows,
                forceReplan = forceReplan,
                zoneId = zoneId,
                activeTaskId = activeTaskId,
                microStepCounts = microStepCounts
            )

            if (placements.isNotEmpty()) {
                applyPlacementsTransactionally(placements)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ScheduleAutoTasksWorker", "Failed in auto-scheduling pass", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally {
            synchronized(workerLock) {
                isWorkerRunning = false
            }
        }
    }

    private suspend fun applyPlacementsTransactionally(
        placements: List<ScheduledPlacement>
    ) {
        val nowMillis = System.currentTimeMillis()
        val focusedTaskIds = sessionRepository.getOpenSessions().map { it.taskId }.toSet()
        val placementsByBaseId = placements.groupBy { it.task.id.substringBefore("#part") }

        for ((baseTaskId, taskPlacements) in placementsByBaseId) {
            val primaryPlacement = taskPlacements.first()
            val freshTask = taskRepository.getById(baseTaskId) ?: continue
            if (baseTaskId in focusedTaskIds) continue

            if (freshTask.status != TaskStatus.ACTIVE ||
                !AutoSchedulingContracts.isMutableByAutoScheduler(freshTask, nowMillis)
            ) {
                continue
            }

            val (scheduledDate, scheduledTime) = splitMillisToDateAndTime(primaryPlacement.window.startMillis)
            val previousStartMillis = freshTask.resolveScheduledEpochMillis()
            val blockLengthChanged = freshTask.plannedBlockMinutes != primaryPlacement.window.durationMinutes

            if (!PlacementStability.isMoveWorthApplying(
                    previousStartMillis = previousStartMillis,
                    nextStartMillis = primaryPlacement.window.startMillis,
                    nowMillis = nowMillis
                ) && !blockLengthChanged
            ) {
                continue
            }

            val isSplit = taskPlacements.size > 1 || primaryPlacement.task.id.endsWith("#part1")
            val titleToUse = if (isSplit && !freshTask.title.contains("(Part ")) "${freshTask.title} (Part 1/2)" else freshTask.title
            val updatedTask = freshTask.copy(
                title = titleToUse,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                isAutoScheduled = true,
                plannedBlockMinutes = primaryPlacement.window.durationMinutes,
                lastAutoScheduledAt = nowMillis,
                updatedAt = nowMillis
            )

            taskRepository.update(updatedTask)
            if (updatedTask.isFrog) {
                calendarSyncRepository?.upsertTaskEvent(
                    updatedTask,
                    startMillis = primaryPlacement.window.startMillis,
                    durationMinutes = primaryPlacement.window.durationMinutes
                )
            }

            // Persist Ultradian Split Part 2 as a linked continuation block if split occurred
            val part2Placement = taskPlacements.getOrNull(1)
            if (part2Placement != null && part2Placement.task.id.endsWith("#part2")) {
                val (p2Date, p2Time) = splitMillisToDateAndTime(part2Placement.window.startMillis)
                val part2Id = "${baseTaskId}_part2"
                val existingPart2 = taskRepository.getById(part2Id)
                val part2Title = if (!part2Placement.task.title.contains("(Part ")) "${freshTask.title.substringBefore(" (Part ")} (Part 2/2)" else part2Placement.task.title
                val part2Entity = (existingPart2 ?: freshTask).copy(
                    id = part2Id,
                    title = part2Title,
                    parentTaskId = baseTaskId,
                    dependsOnTaskIds = baseTaskId,
                    estimatedDurationMinutes = part2Placement.window.durationMinutes,
                    plannedBlockMinutes = part2Placement.window.durationMinutes,
                    scheduledDate = p2Date,
                    scheduledTime = p2Time,
                    isAutoScheduled = true,
                    isFrog = false,
                    lastAutoScheduledAt = nowMillis,
                    updatedAt = nowMillis
                )
                if (existingPart2 != null) {
                    taskRepository.update(part2Entity)
                } else {
                    taskRepository.insert(part2Entity)
                }
            }
        }
    }

    private fun buildBlockedTaskIds(
        tasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity> = emptyList()
    ): Set<String> {
        val taskMap = (allKnownTasks.ifEmpty { tasks }).associateBy { it.id }
        val candidateIds = tasks.map { it.id }.toSet()
        return tasks.filter { task ->
            if (task.waitingFor.isNotBlank()) {
                return@filter true
            }
            val deps = task.dependsOnTaskIds
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            deps.any { depId ->
                val depTask = taskMap[depId]
                if (depTask == null) return@any true
                if (depTask.status == TaskStatus.COMPLETED) return@any false
                // Candidate tasks scheduled together are ordered by the topological DAG scheduler
                if (depTask.status == TaskStatus.ACTIVE && depId in candidateIds) return@any false
                // Split continuation parts are not blocked by their active parent task
                if (task.parentTaskId == depId && depTask.status == TaskStatus.ACTIVE) return@any false
                true
            }
        }.map { it.id }.toSet()
    }
}
