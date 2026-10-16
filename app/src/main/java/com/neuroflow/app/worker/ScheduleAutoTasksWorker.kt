package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.EnergyScoreEngine
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import com.neuroflow.app.domain.repository.PeakEnergyRepository
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.PlacementStability.ReplanVerdict
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
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
    private val peakEnergyRepository: PeakEnergyRepository,
    private val sessionRepository: SessionRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "schedule_auto_tasks_work"
        const val FOREGROUND_TICK_WORK_NAME = "schedule_auto_tasks_foreground_tick"

        // Concurrent worker protection: track active worker runs
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
    }

    override suspend fun doWork(): Result {
        // FIX #2: Concurrent worker protection - prevent overlapping runs
        synchronized(workerLock) {
            if (isWorkerRunning) {
                android.util.Log.w(
                    "ScheduleAutoTasks",
                    "Worker already running - skipping this execution to prevent concurrent conflicts"
                )
                return Result.success()
            }
            isWorkerRunning = true
        }

        return try {
            val forceReplan = inputData.getBoolean("FORCE_REPLAN", false)
            // Check if auto-scheduling is enabled
            val prefs = preferencesDataStore.preferencesFlow.first()
            if (!prefs.autoSchedulingEnabled) {
                return Result.success()
            }

            // Query all tasks for unscheduled and missed scheduled assignments
            val allKnownTasks = taskRepository.getAllTasks()
            val allTasks = allKnownTasks.filter { it.status == TaskStatus.ACTIVE }
            val nowMillis = System.currentTimeMillis()
            val blockedTaskIds = buildBlockedTaskIds(allTasks, allKnownTasks)

            // A task with an open focus session is being lived in, not planned. Moving it would
            // rewrite the block the user is already working inside.
            val focusedTaskIds = sessionRepository.getOpenSessions().map { it.taskId }.toSet()

            // Separate tasks into categories
            val unscheduledTasks = allTasks.filter { task ->
                (task.scheduledDate == null || task.scheduledTime == null) &&
                !AutoSchedulingContracts.hasManualScheduleData(task)
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            // Post-scheduled-end grace period threshold: 5 min when auto-schedule toggle is ON, 30 min default
            val gracePeriodMillis = if (prefs.isAutoScheduleToggleOn) 5 * 60_000L else 30 * 60_000L

            // Past / Missed incomplete tasks (scheduled in past or past end + grace period)
            // Only auto-reschedule tasks that were previously auto-scheduled, to protect manual schedules
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

            // Auto-scheduled tasks eligible for replanning (future active tasks)
            val replanCandidates = allTasks.filter { task ->
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    task.scheduledDate != null &&
                    task.scheduledTime != null &&
                    (task.scheduledDate + task.scheduledTime) >= nowMillis &&
                    shouldReplanAutoScheduledTask(task, nowMillis, prefs, forceReplan)
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            // Combine all tasks needing assignment into a single unified queue
            val targetTasksMap = LinkedHashMap<String, com.neuroflow.app.data.local.entity.TaskEntity>()
            (unscheduledTasks + missedTasks + replanCandidates).forEach { t ->
                // A manual date with no time keeps its day: the scheduler owns the hour, not the day.
                val isDateOnlyAnchor = t.scheduledDate != null && t.scheduledTime == null && !t.isAutoScheduled
                targetTasksMap[t.id] = if (isDateOnlyAnchor) t else t.copy(scheduledDate = null, scheduledTime = null)
            }
            val targetTasks = targetTasksMap.values.toList()

            if (targetTasks.isEmpty()) {
                return Result.success()
            }

            // Fixed tasks are tasks NOT being rescheduled (e.g. locked tasks, manual future schedules, completed tasks)
            val fixedTasks = allTasks.filterNot { targetTasksMap.containsKey(it.id) }
            val currentBusySlotStartMillis = buildBusySlotIndex(fixedTasks, nowMillis).toMutableSet()
            val currentBusyIntervals = buildBusyIntervals(fixedTasks, nowMillis).toMutableList()

            val peakDetection = peakEnergyRepository.getPeakEnergyDetection()

            // Capture energy snapshot at worker start for consistency
            val energySnapshotMillis = System.currentTimeMillis()
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

                val hoursAhead = ((slotMillis - energySnapshotMillis).coerceAtLeast(0L) / 3_600_000f)
                val liveWeight = when {
                    hoursAhead <= 2f -> 0.35f
                    hoursAhead <= 6f -> 0.20f
                    hoursAhead <= 12f -> 0.10f
                    else -> 0.0f
                }

                val SIGNAL_FRESHNESS_THRESHOLD_MILLIS = 5 * 60_000L
                val isLiveDataStale = liveEnergyModel.overallFreshnessAgeMillis > SIGNAL_FRESHNESS_THRESHOLD_MILLIS

                val effectiveLiveWeight = if (isLiveDataStale) {
                    val staleFactor = if (liveEnergyModel.overallFreshnessAgeMillis > 10 * 60_000L) {
                        0.25f
                    } else {
                        0.5f
                    }
                    liveWeight * staleFactor
                } else {
                    liveWeight
                }

                val blendedEnergy = (
                    projectedEnergy * (1f - effectiveLiveWeight) +
                        (liveEnergyModel.availableEnergy.toFloat() * effectiveLiveWeight)
                    ).coerceIn(0f, 100f)

                val blendedConfidence = (
                    peakDetection.confidence.coerceIn(0.2f, 1f) * (1f - effectiveLiveWeight * 0.5f) +
                        liveEnergyModel.momentConfidence.coerceIn(0f, 1f) * (effectiveLiveWeight * 0.5f)
                    ).coerceIn(0.2f, 1f)

                Pair(blendedEnergy.roundToInt(), blendedConfidence)
            }

            // Generate scheduling decisions for all target tasks in a single prioritized pass
            val decisions = autoSchedulingEngine.planAutoSchedule(
                unscheduledTasks = targetTasks,
                nowMillis = nowMillis,
                energyScoreFn = energyScoreFn,
                busySlotStartMillis = currentBusySlotStartMillis,
                busyIntervals = currentBusyIntervals,
                allActiveTasks = fixedTasks,
                allKnownTasks = allKnownTasks
            )

            if (decisions.isNotEmpty()) {
                applyDecisionsTransactionally(decisions, allTasks)
            }

            Result.success()
        } catch (e: Exception) {
            // Log exception (in production, send to telemetry service)
            e.printStackTrace()
            // Retry on transient failures (bounded to prevent infinite background churn)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally {
            // FIX #2: Release worker lock
            synchronized(workerLock) {
                isWorkerRunning = false
            }
        }
    }

    private fun buildBlockedTaskIds(
        tasks: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        allKnownTasks: List<com.neuroflow.app.data.local.entity.TaskEntity> = emptyList()
    ): Set<String> {
        val taskMap = (allKnownTasks.ifEmpty { tasks }).associateBy { it.id }
        return tasks.filter { task ->
            // Check for external dependency (waitingFor field)
            if (task.waitingFor.isNotBlank()) {
                return@filter true
            }

            // Check for task dependencies
            val deps = dependencyIds(task)
            deps.any { depId ->
                val depTask = taskMap[depId]
                // Block if dependency doesn't exist OR is not completed
                depTask == null || depTask.status != TaskStatus.COMPLETED
            }
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
        val nowMillis = System.currentTimeMillis()
        // A session can open while this pass is planning, so pin again against fresh state.
        val focusedTaskIds = sessionRepository.getOpenSessions().map { it.taskId }.toSet()
        decisions.forEach { decision ->
            // Fetch fresh task state from DB to avoid overwriting user edits or completed status during worker execution
            val freshTask = taskRepository.getById(decision.taskId) ?: return@forEach

            if (decision.taskId in focusedTaskIds) {
                android.util.Log.i(
                    "ScheduleAutoTasks",
                    "Keeping placement for task ${decision.taskId}: focus session is running"
                )
                return@forEach
            }

            // Re-verify that task is still active and mutable by auto-scheduler
            if (freshTask.status != TaskStatus.ACTIVE ||
                !AutoSchedulingContracts.isMutableByAutoScheduler(freshTask, nowMillis)
            ) {
                android.util.Log.w(
                    "ScheduleAutoTasks",
                    "Skipping auto-schedule update for task ${decision.taskId}: state changed concurrently or immutable"
                )
                return@forEach
            }

            // Extract date and time from scheduled start millis
            val (scheduledDate, scheduledTime) = splitMillisToDateAndTime(decision.scheduledStartMillis)

            val previousStartMillis =
                freshTask.scheduledDate?.let { it + (freshTask.scheduledTime ?: 0L) }
            val blockLengthChanged =
                freshTask.plannedBlockMinutes != decision.estimatedDurationMinutes
            if (!PlacementStability.isMoveWorthApplying(
                    previousStartMillis = previousStartMillis,
                    nextStartMillis = decision.scheduledStartMillis,
                    nowMillis = nowMillis
                ) && !blockLengthChanged
            ) {
                android.util.Log.d(
                    "ScheduleAutoTasks",
                    "Keeping placement for task ${decision.taskId}: new start is not materially different"
                )
                return@forEach
            }

            val updatedTask = freshTask.copy(
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                isAutoScheduled = true,
                plannedBlockMinutes = decision.estimatedDurationMinutes,
                lastAutoScheduledAt = nowMillis,  // Track when task was auto-scheduled for replanning cooldown
                updatedAt = nowMillis
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
        // FIX #5: DST-safe date/time splitting
        // Use Calendar.getInstance() which respects system timezone and DST rules
        // This ensures correct behavior during DST transitions (spring forward/fall back)
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
        }

        // Extract date (start of day) - Calendar handles DST automatically
        val dateStart = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val dateMillis = dateStart.timeInMillis

        // Extract time of day offset
        // During DST transitions, this correctly captures the actual time offset
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

    private fun buildTaskOccupiedInterval(task: com.neuroflow.app.data.local.entity.TaskEntity): AutoSchedulingEngine.TimeInterval? {
        val scheduledDate = task.scheduledDate ?: return null
        val scheduledTime = task.scheduledTime ?: return null
        val startMillis = scheduledDate + scheduledTime
        val estimated = TaskBlockPlan.plannedMinutes(task)
        val endMillis = startMillis + (estimated * 60_000L)
        return AutoSchedulingEngine.TimeInterval(startMillis, endMillis)
    }

    private fun buildDecisionOccupiedInterval(decision: AutoSchedulingEngine.ScheduleDecision): AutoSchedulingEngine.TimeInterval {
        val start = decision.scheduledStartMillis
        val end = start + (decision.estimatedDurationMinutes * 60_000L)
        return AutoSchedulingEngine.TimeInterval(start, end)
    }

    private fun buildTaskOccupiedSlots(task: com.neuroflow.app.data.local.entity.TaskEntity): Set<Long> {
        val scheduledDate = task.scheduledDate ?: return emptySet()
        val scheduledTime = task.scheduledTime ?: return emptySet()
        val startMillis = scheduledDate + scheduledTime

        val roundedStart = roundDownToHour(startMillis)
        val estimated = TaskBlockPlan.plannedMinutes(task)
        val slotCount = ((estimated + 59) / 60).coerceAtLeast(1)

        val slots = mutableSetOf<Long>()

        // FIX #5: Handle midnight boundary correctly
        // Use Calendar to advance hours, which correctly handles day transitions
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = roundedStart }

        repeat(slotCount) { idx ->
            slots += cal.timeInMillis
            cal.add(java.util.Calendar.HOUR_OF_DAY, 1)  // DST-safe hour advancement
        }
        return slots
    }

    private fun buildDecisionOccupiedSlots(decision: AutoSchedulingEngine.ScheduleDecision): Set<Long> {
        val (scheduledDate, scheduledTime) = splitMillisToDateAndTime(decision.scheduledStartMillis)
        val task = com.neuroflow.app.data.local.entity.TaskEntity(
            id = decision.taskId,
            title = "",
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            estimatedDurationMinutes = decision.estimatedDurationMinutes
        )
        return buildTaskOccupiedSlots(task)
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
            val estimated = TaskBlockPlan.plannedMinutes(task)
            val slotCount = ((estimated + 59) / 60).coerceAtLeast(1)

            // FIX #5: Handle midnight boundary correctly
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = roundedStart }

            repeat(slotCount) { idx ->
                busy += cal.timeInMillis
                cal.add(java.util.Calendar.HOUR_OF_DAY, 1)  // DST-safe hour advancement
            }
        }
        return busy
    }

    private fun buildBusyIntervals(
        tasks: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        nowMillis: Long
    ): List<AutoSchedulingEngine.TimeInterval> {
        val busy = mutableListOf<AutoSchedulingEngine.TimeInterval>()
        tasks.forEach { task ->
            buildTaskOccupiedInterval(task)?.let { interval ->
                if (interval.endMillis >= nowMillis - 60 * 60 * 1000L) {
                    busy.add(interval)
                }
            }
        }
        return busy
    }

    private fun roundDownToHour(millis: Long): Long {
        // FIX #5: DST-safe hour rounding
        // Calendar.getInstance() respects timezone and DST rules
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Whether an already-placed block is worth re-solving. The policy itself lives in
     * [PlacementStability] so the ordering of the stability guards can be tested directly.
     */
    private fun shouldReplanAutoScheduledTask(
        task: com.neuroflow.app.data.local.entity.TaskEntity,
        nowMillis: Long,
        prefs: com.neuroflow.app.data.local.UserPreferences,
        forceReplan: Boolean
    ): Boolean {
        val verdict = PlacementStability.verdict(task, nowMillis, prefs, forceReplan)
        val scheduledMillis = task.scheduledDate?.let { it + (task.scheduledTime ?: 0L) }
        val hoursUntilScheduled = scheduledMillis?.let { (it - nowMillis) / 3_600_000f }
        val minutesSincePlaced = (nowMillis - (task.lastAutoScheduledAt ?: task.updatedAt)) / 60_000
        val hoursOut = hoursUntilScheduled?.let { " (${String.format("%.1f", it)} hours out)" } ?: ""

        when (verdict) {
            ReplanVerdict.NEAR_TERM_FREEZE -> android.util.Log.d(
                "ScheduleAutoTasks",
                "Replanning skipped for task ${task.id}: block starts$hoursOut"
            )
            ReplanVerdict.COOLDOWN -> android.util.Log.d(
                "ScheduleAutoTasks",
                "Replanning skipped for task ${task.id}: $minutesSincePlaced min since last placement"
            )
            ReplanVerdict.KEEP -> Unit
            else -> android.util.Log.i(
                "ScheduleAutoTasks",
                "Replanning task ${task.id}: ${verdict.name.lowercase().replace('_', ' ')}$hoursOut"
            )
        }

        return verdict.allowsReplan
    }
}
