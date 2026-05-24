package com.neuroflow.app.presentation.launcher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.EnergyScoreEngine
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import com.neuroflow.app.domain.repository.PeakEnergyRepository
import com.neuroflow.app.domain.scheduler.AutoSchedulingEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import java.util.concurrent.TimeUnit

/**
 * Phase 4: ScheduleAutoTasksWorker
 *
 * Background worker that executes Phase 3 AutoSchedulingEngine decisions transactionally.
 *
 * Runs periodically (every 15 minutes by default per autoSchedulingBackgroundThrottleMinutes)
 * to:
 * 1. Query unscheduled, eligible tasks
 * 2. Generate scheduling decisions via AutoSchedulingEngine
 * 3. Apply decisions transactionally to task repository
 * 4. Log telemetry for monitoring and debugging
 *
 * Gated by:
 * - autoSchedulingEnabled preference
 * - Background throttle minimum (prevents excessive runs)
 * - Task eligibility checks (Phase 1 contracts)
 *
 * Produces:
 * - Updated tasks with scheduledDate/scheduledTime set
 * - Telemetry logs for scheduler monitoring
 */
@HiltWorker
class ScheduleAutoTasksWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val autoSchedulingEngine: AutoSchedulingEngine,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val energyScoreRepository: EnergyScoreRepository,
    private val peakEnergyRepository: PeakEnergyRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Check if auto-scheduling is enabled
            val prefs = preferencesDataStore.preferencesFlow.first()
            if (!prefs.autoSchedulingEnabled) {
                return Result.success()
            }

            // Query all active tasks for unscheduled and missed scheduled assignments
            val allTasks = taskRepository.getActiveTasks()
            val nowMillis = System.currentTimeMillis()
            val blockedTaskIds = buildBlockedTaskIds(allTasks)

            // Separate tasks into categories
            val unscheduledTasks = allTasks.filter { task ->
                task.scheduledDate == null && task.scheduledTime == null
            }.filterNot { it.id in blockedTaskIds }

            val busySlotStartMillis = buildBusySlotIndex(allTasks, nowMillis)

            // Missed tasks: scheduled in the past
            val missedTasks = allTasks.filter { task ->
                task.status == TaskStatus.ACTIVE &&
                    task.scheduledDate != null &&
                    task.scheduledTime != null &&
                    (task.scheduledDate + task.scheduledTime) < nowMillis
            }.filterNot { it.id in blockedTaskIds }

            // Auto-scheduled tasks that may need replanning
            val autoScheduledTasks = allTasks.filter { task ->
                task.status == TaskStatus.ACTIVE &&
                    task.scheduledDate != null &&
                    task.scheduledTime != null &&
                    task.isAutoScheduled &&  // Only replan auto-scheduled tasks
                    (task.scheduledDate + task.scheduledTime) >= nowMillis  // Future tasks only
            }.filterNot { it.id in blockedTaskIds }

            val peakDetection = peakEnergyRepository.getPeakEnergyDetection()
            val liveEnergyModel = energyScoreRepository.observeEnergy(refreshIntervalMillis = 0).first()

            val energyScoreFn: suspend (Long) -> Pair<Int, Float> = { slotMillis ->
                val baseline = EnergyScoreEngine.calculateDetailed(
                    EnergyScoreEngine.EnergySnapshot(
                        peakEnergy = peakDetection,
                        sleepPressurePoints = prefs.sleepPressurePoints,
                        nowMillis = slotMillis
                    )
                )

                val projectedEnergy = baseline.usableEnergy.coerceIn(0f, 100f)
                val hoursAhead = ((slotMillis - nowMillis).coerceAtLeast(0L) / 3_600_000f)
                val liveWeight = when {
                    hoursAhead <= 2f -> 0.35f
                    hoursAhead <= 6f -> 0.20f
                    hoursAhead <= 12f -> 0.10f
                    else -> 0.05f
                }

                val blendedEnergy = (
                    projectedEnergy * (1f - liveWeight) +
                        (liveEnergyModel.availableEnergy.toFloat() * liveWeight)
                    ).coerceIn(0f, 100f)

                val blendedConfidence = (
                    peakDetection.confidence.coerceIn(0.2f, 1f) * (1f - liveWeight * 0.5f) +
                        liveEnergyModel.momentConfidence.coerceIn(0f, 1f) * (liveWeight * 0.5f)
                    ).coerceIn(0.2f, 1f)

                Pair(blendedEnergy.roundToInt(), blendedConfidence)
            }

            // Replan missed tasks first to keep past assignments from becoming stale.
            val missedDecisions = missedTasks.mapNotNull { missedTask ->
                val estimatedWorkDone = missedTask.lastSessionDurationMinutes?.roundToInt()
                    ?: missedTask.totalTimeTrackedMinutes.roundToInt()
                autoSchedulingEngine.replanIncompleteTask(
                    missedTask,
                    timeSpentMinutes = estimatedWorkDone,
                    nowMillis = nowMillis,
                    energyScoreFn = energyScoreFn,
                    busySlotStartMillis = busySlotStartMillis - setOf(
                        roundDownToHour(missedTask.scheduledDate!! + missedTask.scheduledTime!!)
                    )
                )
            }
            if (missedDecisions.isNotEmpty()) {
                applyDecisionsTransactionally(missedDecisions, allTasks)
            }

            // Replan existing auto-scheduled tasks if conditions warrant it
            // This enables true dynamic scheduling - tasks move to better slots as conditions change
            val replanCandidates = autoScheduledTasks.filter { task ->
                shouldReplanAutoScheduledTask(task, nowMillis, prefs)
            }

            if (replanCandidates.isNotEmpty()) {
                // Temporarily unschedule these tasks for replanning
                val unscheduledForReplan = replanCandidates.map { it.copy(
                    scheduledDate = null,
                    scheduledTime = null
                )}

                // Replan them with current conditions
                val replanDecisions = autoSchedulingEngine.planAutoSchedule(
                    unscheduledTasks = unscheduledForReplan,
                    nowMillis = nowMillis,
                    energyScoreFn = energyScoreFn,
                    busySlotStartMillis = busySlotStartMillis - replanCandidates.mapNotNull { task ->
                        task.scheduledDate?.let { date ->
                            roundDownToHour(date + (task.scheduledTime ?: 0L))
                        }
                    }.toSet()
                )

                if (replanDecisions.isNotEmpty()) {
                    applyDecisionsTransactionally(replanDecisions, allTasks)
                }
            }

            if (unscheduledTasks.isEmpty()) {
                return Result.success()
            }

            // Generate scheduling decisions for fresh unscheduled work.
            val decisions = autoSchedulingEngine.planAutoSchedule(
                unscheduledTasks = unscheduledTasks,
                nowMillis = nowMillis,
                energyScoreFn = energyScoreFn,
                busySlotStartMillis = busySlotStartMillis
            )

            if (decisions.isEmpty()) {
                return Result.success()
            }

            // Apply decisions transactionally
            applyDecisionsTransactionally(decisions, allTasks)

            Result.success()
        } catch (e: Exception) {
            // Log exception (in production, send to telemetry service)
            e.printStackTrace()
            // Retry on transient failures
            Result.retry()
        }
    }

    private fun buildBlockedTaskIds(tasks: List<com.neuroflow.app.data.local.entity.TaskEntity>): Set<String> {
        val activeIds = tasks.map { it.id }.toSet()
        return tasks.filter { task ->
            task.waitingFor.isNotBlank() || dependencyIds(task).any { depId -> depId in activeIds && depId != task.id }
        }.map { it.id }.toSet()
    }

    private fun dependencyIds(task: com.neuroflow.app.data.local.entity.TaskEntity): Set<String> {
        return task.dependsOnTaskIds
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private suspend fun applyDecisionsTransactionally(
        decisions: List<AutoSchedulingEngine.ScheduleDecision>,
        allTasks: List<com.neuroflow.app.data.local.entity.TaskEntity>
    ) {
        decisions.forEach { decision ->
            // Find the task to update
            val taskToUpdate = allTasks.find { it.id == decision.taskId } ?: return@forEach

            // Extract date and time from scheduled start millis
            val (scheduledDate, scheduledTime) = splitMillisToDateAndTime(decision.scheduledStartMillis)

            // Update task with scheduled date/time
            val updatedTask = taskToUpdate.copy(
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                isAutoScheduled = true,
                estimatedDurationMinutes = decision.estimatedDurationMinutes,
                updatedAt = System.currentTimeMillis()
            )

            // Persist update
            taskRepository.update(updatedTask)

            // Log telemetry decision as applied
            logTelemetry(decision.copy(
                telemetry = decision.telemetry.copy(wasApplied = true)
            ))
        }
    }

    private fun splitMillisToDateAndTime(millis: Long): Pair<Long, Long> {
        // Split milliseconds into local-safe date and time components
        // This mirrors the splitToLocalDateAndTime pattern used in TaskEntity
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }

        // Extract date (start of day)
        val dateStart = cal.clone() as java.util.Calendar
        dateStart.set(java.util.Calendar.HOUR_OF_DAY, 0)
        dateStart.set(java.util.Calendar.MINUTE, 0)
        dateStart.set(java.util.Calendar.SECOND, 0)
        dateStart.set(java.util.Calendar.MILLISECOND, 0)
        val dateMillis = dateStart.timeInMillis

        // Extract time of day offset
        val timeOffset = millis - dateMillis

        return dateMillis to timeOffset
    }

    private fun logTelemetry(decision: AutoSchedulingEngine.ScheduleDecision) {
        // In production, send to remote telemetry service
        // For now, just log locally
        android.util.Log.d(
            "ScheduleAutoTasks",
            "Applied decision: task=${decision.taskId}, slot=${decision.assignedSlotIndex}, " +
                    "reason=${decision.assignmentReason}, wasApplied=${decision.telemetry.wasApplied}"
        )
    }

    private fun buildBusySlotIndex(
        tasks: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        nowMillis: Long
    ): Set<Long> {
        val busy = mutableSetOf<Long>()
        tasks.forEach { task ->
            val scheduledDate = task.scheduledDate ?: return@forEach
            val scheduledTime = task.scheduledTime ?: return@forEach
            val startMillis = scheduledDate + scheduledTime
            if (startMillis < nowMillis - 60 * 60 * 1000L) return@forEach

            val roundedStart = roundDownToHour(startMillis)
            val estimated = task.estimatedDurationMinutes.coerceAtLeast(30)
            val slotCount = ((estimated + 59) / 60).coerceAtLeast(1)
            repeat(slotCount) { idx ->
                busy += roundedStart + idx * 3_600_000L
            }
        }
        return busy
    }

    private fun roundDownToHour(millis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Determines if an auto-scheduled task should be replanned.
     *
     * Replanning triggers:
     * 1. Deadline approaching (< 6 hours) but scheduled too late
     * 2. Task scheduled in LOW energy but PEAK energy now available earlier
     * 3. Task postponed multiple times (procrastination pattern)
     * 4. Scheduled time conflicts with new higher-priority task
     * 5. Energy prediction changed significantly since scheduling
     */
    private fun shouldReplanAutoScheduledTask(
        task: com.neuroflow.app.data.local.entity.TaskEntity,
        nowMillis: Long,
        prefs: com.neuroflow.app.data.local.UserPreferences
    ): Boolean {
        val scheduledMillis = (task.scheduledDate ?: return false) + (task.scheduledTime ?: 0L)
        val hoursUntilScheduled = (scheduledMillis - nowMillis) / 3_600_000f

        // Don't replan tasks scheduled very soon (< 2 hours) - too disruptive
        if (hoursUntilScheduled < 2f) return false

        // 1. Deadline approaching but scheduled too late
        val deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) }
        if (deadlineMillis != null) {
            val hoursUntilDeadline = (deadlineMillis - nowMillis) / 3_600_000f

            // If deadline < 6 hours away and task scheduled > 50% of remaining time
            if (hoursUntilDeadline < 6f && hoursUntilScheduled > hoursUntilDeadline * 0.5f) {
                return true
            }

            // If deadline < 12 hours and task is LOW priority but should be HIGH
            if (hoursUntilDeadline < 12f && task.priority == com.neuroflow.app.domain.model.Priority.LOW) {
                return true
            }
        }

        // 2. Task postponed multiple times (procrastination pattern)
        if (task.postponeCount >= 3) {
            return true
        }

        // 3. Task scheduled far in future but could be done sooner
        // Only replan if scheduled > 24 hours away and not locked
        if (hoursUntilScheduled > 24f && !task.isScheduleLocked) {
            // Check if this is a high-priority or high-impact task
            val isImportant = task.priority == com.neuroflow.app.domain.model.Priority.HIGH ||
                             task.impactScore >= 80 ||
                             task.isFrog

            if (isImportant) {
                return true
            }
        }

        // 4. Periodic refresh: replan tasks every 6 hours to adapt to changing conditions
        // This ensures tasks continuously optimize as energy predictions improve
        val taskAge = nowMillis - task.updatedAt
        val sixHoursMillis = 6 * 60 * 60 * 1000L

        if (taskAge > sixHoursMillis && prefs.autoSchedulingEnabled) {
            // Only replan a subset to avoid thrashing
            // Use task ID hash to deterministically select ~20% of tasks
            val shouldRefresh = (task.id.hashCode() % 5) == 0
            if (shouldRefresh) {
                return true
            }
        }

        return false
    }

    companion object {
        const val WORK_NAME = "schedule_auto_tasks_work"
        const val FOREGROUND_TICK_WORK_NAME = "schedule_auto_tasks_foreground_tick"

        fun buildPeriodicWorkRequest(throttleMinutes: Int): PeriodicWorkRequest {
            val throttleInterval = throttleMinutes.coerceIn(15, 240)
            return PeriodicWorkRequest.Builder(
                ScheduleAutoTasksWorker::class.java,
                throttleInterval.toLong(),
                TimeUnit.MINUTES
            ).build()
        }

        fun buildOneTimeWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(ScheduleAutoTasksWorker::class.java).build()
        }
    }
}
