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
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.ProtectedLocalSlot
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * ADR-002: ScheduleAutoTasksWorker
 *
 * Background worker executing modern TaskSchedulerService decisions transactionally.
 * Eliminates legacy split-brain scheduling bugs by sharing identical Borbély math,
 * 4-vector scoring, and topological interval DAG scheduling as the UI presentation layer.
 */
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
    private val calendarSyncRepository: CalendarSyncRepository? = null
) : CoroutineWorker(context, params) {

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

        internal fun calculateWakeTimeMillis(nowMillis: Long, wakeHour: Int): Long {
            val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
            val hoursAwake = (currentHour - wakeHour).let { if (it < 0) it + 24 else it }
            return nowMillis - (hoursAwake * 3600_000L)
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
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    task.scheduledDate != null &&
                    task.scheduledTime != null &&
                    (task.scheduledDate + task.scheduledTime) >= nowMillis &&
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
                val sDate = task.scheduledDate ?: return@mapNotNull null
                val sTime = task.scheduledTime ?: return@mapNotNull null
                val start = sDate + sTime
                val duration = TaskBlockPlan.plannedMinutes(task)
                val end = start + duration * 60_000L
                if (end > nowMillis) TimeSlotWindow(start, end) else null
            }

            // Fetch external busy windows from Android Native Calendar Sync
            val dayStart = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = dayStart + 24 * 3600_000L - 1
            val externalBusy = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd) ?: emptyList()
            val totalBusyWindows = externalBusy + fixedWindows

            // Resolve protected slots
            val protectedSlots = prefs.protectedTimeSlots.filter { it.isEnabled }.map {
                ProtectedLocalSlot(
                    startHour = it.startMinuteOfDay / 60,
                    startMinute = it.startMinuteOfDay % 60,
                    durationMinutes = it.durationMinutes
                )
            }

            // Map target tasks to SchedulableTask
            val schedulableTasks = targetTasks.map { task ->
                SchedulableTask(
                    id = task.id,
                    title = task.title,
                    category = when (task.taskType) {
                        TaskType.ANALYTICAL -> TaskCategory.DEEP_WORK
                        TaskType.CREATIVE -> TaskCategory.CREATIVE
                        TaskType.ADMIN -> TaskCategory.ADMIN
                        TaskType.PHYSICAL -> TaskCategory.EXERCISE
                    },
                    priority = when (task.priority) {
                        Priority.HIGH -> 2
                        Priority.MEDIUM -> 1
                        else -> 0
                    },
                    estimatedMinutes = task.estimatedDurationMinutes,
                    energyDemand = when (task.energyLevel) {
                        EnergyLevel.HIGH -> EnergyDemand.HIGH
                        EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                        else -> EnergyDemand.LOW
                    },
                    isFrog = task.isFrog,
                    scheduledStartMillis = task.scheduledTime?.let { (task.scheduledDate ?: dayStart) + it },
                    deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) },
                    isLocked = task.isScheduleLocked,
                    parentTaskId = task.parentTaskId
                )
            }

            val preparedTasks = if (forceReplan) {
                behavioralSupportService.resetScheduleAnchors(schedulableTasks)
            } else {
                schedulableTasks
            }

            // Wake time calculation
            val wakeTimeMillis = calculateWakeTimeMillis(nowMillis, prefs.wakeUpHour)

            val placements = taskSchedulerService.scheduleDay(
                tasks = preparedTasks,
                dayDate = dayDate,
                nowMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = 480,
                busyWindows = totalBusyWindows,
                protectedSlots = protectedSlots,
                calibrationRatio = 1.20,
                forceReplan = forceReplan,
                zoneId = zoneId
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

        for (placement in placements) {
            val taskId = placement.task.id
            val freshTask = taskRepository.getById(taskId) ?: continue

            if (taskId in focusedTaskIds) {
                continue
            }

            if (freshTask.status != TaskStatus.ACTIVE ||
                !AutoSchedulingContracts.isMutableByAutoScheduler(freshTask, nowMillis)
            ) {
                continue
            }

            val (scheduledDate, scheduledTime) = splitMillisToDateAndTime(placement.window.startMillis)
            val previousStartMillis = freshTask.scheduledDate?.let { it + (freshTask.scheduledTime ?: 0L) }
            val blockLengthChanged = freshTask.plannedBlockMinutes != placement.window.durationMinutes

            if (!PlacementStability.isMoveWorthApplying(
                    previousStartMillis = previousStartMillis,
                    nextStartMillis = placement.window.startMillis,
                    nowMillis = nowMillis
                ) && !blockLengthChanged
            ) {
                continue
            }

            val updatedTask = freshTask.copy(
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                isAutoScheduled = true,
                plannedBlockMinutes = placement.window.durationMinutes,
                lastAutoScheduledAt = nowMillis,
                updatedAt = nowMillis
            )

            taskRepository.update(updatedTask)
            if (updatedTask.isFrog) {
                calendarSyncRepository?.upsertTaskEvent(
                    updatedTask,
                    startMillis = placement.window.startMillis,
                    durationMinutes = placement.window.durationMinutes
                )
            }
        }
    }

    private fun buildBlockedTaskIds(
        tasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity> = emptyList()
    ): Set<String> {
        val taskMap = (allKnownTasks.ifEmpty { tasks }).associateBy { it.id }
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
                depTask == null || depTask.status != TaskStatus.COMPLETED
            }
        }.map { it.id }.toSet()
    }
}
