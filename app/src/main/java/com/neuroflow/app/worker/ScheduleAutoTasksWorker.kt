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
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import com.neuroflow.app.domain.scheduler.SchedulingDateSanityValidator
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.presentation.v2.focus.FocusTimerModeV2
import com.neuroflow.app.presentation.v2.focus.service.FocusService
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
            val anchorMillis = anchor.toInstant().toEpochMilli()
            if (anchorMillis > nowMillis) {
                // If within immediate morning pre-wake window (<= 2 hours before wake time, e.g. 6:30am for 7:00am wake),
                // anchor to today's wake time so morning notifications/schedules don't diagnose severe sleep deprivation.
                // Otherwise (e.g. 2:00 AM past midnight), wrap back to yesterday's wake time.
                if (anchorMillis - nowMillis > 2 * 3600_000L) {
                    anchor = anchor.minusDays(1)
                }
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

        internal fun buildBlockedTaskIds(
            tasks: List<TaskEntity>,
            allKnownTasks: List<TaskEntity> = emptyList(),
            dayEnd: Long = Long.MAX_VALUE
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
                    val depStart = depTask.resolveScheduledEpochMillis()
                    if (depStart != null && depStart > dayEnd) return@any true
                    // Candidate tasks scheduled together are ordered by the topological DAG scheduler
                    if (depTask.status == TaskStatus.ACTIVE && depId in candidateIds) return@any false
                    // Split continuation parts are not blocked by their active parent task
                    if (task.parentTaskId == depId && depTask.status == TaskStatus.ACTIVE) return@any false
                    true
                }
            }.map { it.id }.toSet()
        }

        internal data class WorkerCandidateBuckets(
            val unscheduledTasks: List<TaskEntity>,
            val missedTasks: List<TaskEntity>,
            val replanCandidates: List<TaskEntity>
        )

        /**
         * Partitions active tasks for Today's single-day background scheduling pass.
         * Strictly isolates future-dated tasks (including future auto-scheduled blocks,
         * user-dated future tasks, and future recurring habits) so they are never pulled
         * backward into Today or wiped during reconciliation.
         */
        internal fun selectCandidateBuckets(
            allTasks: List<TaskEntity>,
            dayStart: Long,
            dayEnd: Long,
            nowMillis: Long,
            prefs: com.neuroflow.app.data.local.UserPreferences,
            forceReplan: Boolean,
            blockedTaskIds: Set<String> = emptySet(),
            focusedTaskIds: Set<String> = emptySet(),
            isPartBlocked: (TaskEntity) -> Boolean = { false },
            zoneId: ZoneId = ZoneId.systemDefault()
        ): WorkerCandidateBuckets {
            val dayDate = Instant.ofEpochMilli(dayStart).atZone(zoneId).toLocalDate()

            val unscheduledTasks = allTasks.filter { task ->
                val sDate = task.effectiveScheduledDateMillis()
                SchedulingDateSanityValidator.isEntityEligibleForDay(
                    task = task,
                    targetDate = dayDate,
                    nowMillis = nowMillis,
                    zoneId = zoneId,
                    allowFutureDeadlinedBacklog = false
                ) &&
                    (sDate == null || task.scheduledTime == null) &&
                    !AutoSchedulingContracts.hasManualScheduleData(task) &&
                    AutoSchedulingContracts.isMutableByAutoScheduler(task, nowMillis)
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds || isPartBlocked(it) }

            val gracePeriodMillis = if (prefs.isAutoScheduleToggleOn) 5 * 60_000L else 30 * 60_000L
            val missedTasks = allTasks.filter { task ->
                val scheduledStart = task.resolveScheduledEpochMillis(dayStart)
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    task.isAutoScheduled &&
                    AutoSchedulingContracts.isMutableByAutoScheduler(task, nowMillis) &&
                    SchedulingDateSanityValidator.isEntityEligibleForDay(
                        task = task,
                        targetDate = dayDate,
                        nowMillis = nowMillis,
                        zoneId = zoneId,
                        allowFutureDeadlinedBacklog = false
                    ) &&
                    task.scheduledTime != null &&
                    scheduledStart != null &&
                    scheduledStart <= dayEnd &&
                    let {
                        TaskBlockPlan.blockHasExpired(
                            task = task,
                            startMillis = scheduledStart,
                            nowMillis = nowMillis,
                            gracePeriodMillis = gracePeriodMillis
                        ) || (forceReplan && scheduledStart < nowMillis)
                    }
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds || isPartBlocked(it) }

            val missedTaskIds = missedTasks.map { it.id }.toSet()
            val replanCandidates = allTasks.filter { task ->
                val scheduledMillis = task.resolveScheduledEpochMillis(dayStart)
                val effectiveDate = task.effectiveScheduledDateMillis()
                val isScheduledForTodayOnly = scheduledMillis != null &&
                    scheduledMillis in dayStart..dayEnd &&
                    (effectiveDate == null || effectiveDate in dayStart..dayEnd)
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    AutoSchedulingContracts.isMutableByAutoScheduler(task, nowMillis) &&
                    SchedulingDateSanityValidator.isEntityEligibleForDay(
                        task = task,
                        targetDate = dayDate,
                        nowMillis = nowMillis,
                        zoneId = zoneId,
                        allowFutureDeadlinedBacklog = false
                    ) &&
                    isScheduledForTodayOnly &&
                    PlacementStability.verdict(task, nowMillis, prefs, forceReplan, zoneId).allowsReplan
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds || isPartBlocked(it) || it.id in missedTaskIds }

            return WorkerCandidateBuckets(
                unscheduledTasks = unscheduledTasks,
                missedTasks = missedTasks,
                replanCandidates = replanCandidates
            )
        }

        /**
         * Identifies replan candidates whose previous slot [prevStart, prevEnd) is no longer safe to
         * retain via hysteresis because it overlaps:
         * 1. Any external/fixed busy window or protected slot,
         * 2. Another task's newly proposed placement in [placements], or
         * 3. Another replan candidate's previous window (defensive guard against stale overlaps).
         *
         * Marking such candidates as conflicting bypasses the utility-gain hysteresis hold so
         * keeping a task in its old slot can never create a double-booked collision.
         */
        internal fun findConflictingPreviousSlotIds(
            replanCandidates: List<TaskEntity>,
            placements: List<ScheduledPlacement>,
            busyWindows: List<TimeSlotWindow>,
            nowMillis: Long
        ): Set<String> {
            val prevWindows = replanCandidates.mapNotNull { candidate ->
                val prevStart = candidate.resolveScheduledEpochMillis(nowMillis) ?: return@mapNotNull null
                val prevDuration = TaskBlockPlan.plannedMinutes(candidate).coerceAtLeast(5)
                candidate.id to TimeSlotWindow(prevStart, prevStart + prevDuration * 60_000L)
            }.toMap()

            fun overlaps(a: TimeSlotWindow, b: TimeSlotWindow): Boolean =
                maxOf(a.startMillis, b.startMillis) < minOf(a.endMillis, b.endMillis)

            val conflicting = mutableSetOf<String>()
            for ((candidateId, prevWin) in prevWindows) {
                val overlapsBusy = busyWindows.any { b -> overlaps(b, prevWin) }
                val overlapsOtherPlacement = placements.any { p ->
                    val baseId = if (p.task.id.contains("#part")) p.task.id.substringBefore("#part") else p.task.id
                    baseId != candidateId && overlaps(p.window, prevWin)
                }
                val overlapsOtherPrev = prevWindows.any { (otherId, otherWin) ->
                    otherId != candidateId && overlaps(otherWin, prevWin)
                }
                if (overlapsBusy || overlapsOtherPlacement || overlapsOtherPrev) {
                    conflicting.add(candidateId)
                }
            }
            return conflicting
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

            val nowMillis = System.currentTimeMillis()

            // Inbound external calendar delta synchronization
            calendarSyncRepository?.let { calRepo ->
                try {
                    val deltas = calRepo.detectExternalModifications(prefs.lastCalendarInboundSyncAt)
                    for (delta in deltas) {
                        val existing = taskRepository.getById(delta.taskId)
                        if (existing != null && existing.status == TaskStatus.ACTIVE) {
                            when (delta.type) {
                                com.neuroflow.app.data.calendar.ExternalDeltaType.MOVED -> {
                                    if (delta.newStartMillis != null) {
                                        val (newDate, newTime) = splitMillisToDateAndTime(delta.newStartMillis)
                                        taskRepository.update(
                                            existing.copy(
                                                scheduledDate = newDate,
                                                scheduledTime = newTime,
                                                isScheduleLocked = true,
                                                updatedAt = nowMillis
                                            )
                                        )
                                    }
                                }
                                com.neuroflow.app.data.calendar.ExternalDeltaType.DELETED -> {
                                    taskRepository.update(
                                        existing.copy(
                                            scheduledDate = null,
                                            scheduledTime = null,
                                            isAutoScheduled = false,
                                            isScheduleLocked = false,
                                            updatedAt = nowMillis
                                        )
                                    )
                                }
                                com.neuroflow.app.data.calendar.ExternalDeltaType.EVENT_ADDED -> {
                                    // Handled during calendar reconciliation
                                }
                            }
                        }
                    }
                    preferencesDataStore.setLastCalendarInboundSyncAt(nowMillis)
                } catch (e: Exception) {
                    android.util.Log.w("ScheduleAutoTasksWorker", "Inbound calendar sync failed", e)
                }
            }

            val allKnownTasks = taskRepository.getAllTasks()
            val allTasks = allKnownTasks.filter { it.status == TaskStatus.ACTIVE }

            val zoneId = ZoneId.systemDefault()
            val dayDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
            val dayStart = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(dayStart) - 1

            val blockedTaskIds = buildBlockedTaskIds(allTasks, allKnownTasks, dayEnd)
            val activeTaskIds = allTasks.map { it.id }.toSet()
            val isPartBlocked: (TaskEntity) -> Boolean = { task ->
                val partNumMatch = Regex("""[#_]part(\d+)""").find(task.id)
                val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("""\(Part (\d+)/""").find(task.title)?.groupValues?.get(1)?.toIntOrNull()
                val isContinuation = (partNum != null && partNum >= 2) ||
                    task.id.contains(Regex("""[#_]part\d+""")) ||
                    task.parentTaskId != null
                if (!isContinuation) {
                    false
                } else {
                    val parentSlug = task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
                    val isParentActive = (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
                        (parentSlug != task.id && parentSlug in activeTaskIds)
                    val isPart1Active = task.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1") in activeTaskIds
                    val isPrecedingPartActive = partNum != null && (1 until partNum).any { prevPart ->
                        task.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart") in activeTaskIds
                    }
                    isParentActive || isPart1Active || isPrecedingPartActive
                }
            }
            val openSessions = sessionRepository.getOpenSessions()
            val focusedTaskIds = openSessions.filter { session ->
                val activeServiceTaskId = FocusService.activeSession.value?.taskId
                if (activeServiceTaskId != null && activeServiceTaskId == session.taskId) return@filter true
                val modeDurationSec = FocusTimerModeV2.entries
                    .firstOrNull { it.name == session.sessionType }?.durationSeconds
                val maxDurationMs = if (modeDurationSec != null && modeDurationSec > 0L) {
                    modeDurationSec * 1000L
                } else {
                    120 * 60_000L
                }
                val elapsedMs = (nowMillis - session.startedAt - session.totalPausedMs).coerceAtLeast(0L)
                elapsedMs < maxDurationMs
            }.map { it.taskId }.toSet()

            // Separate tasks into unscheduled, missed, and replan candidates (strictly scoped to Today)
            val buckets = selectCandidateBuckets(
                allTasks = allTasks,
                dayStart = dayStart,
                dayEnd = dayEnd,
                nowMillis = nowMillis,
                prefs = prefs,
                forceReplan = forceReplan,
                blockedTaskIds = blockedTaskIds,
                focusedTaskIds = focusedTaskIds,
                isPartBlocked = isPartBlocked,
                zoneId = zoneId
            )
            val unscheduledTasks = buckets.unscheduledTasks
            val missedTasks = buckets.missedTasks
            val replanCandidates = buckets.replanCandidates
            val replanVerdicts = replanCandidates.associate { candidate ->
                candidate.id to PlacementStability.verdict(
                    task = candidate,
                    nowMillis = nowMillis,
                    prefs = prefs,
                    forceReplan = forceReplan,
                    zoneId = zoneId
                )
            }

            val targetTasksMap = LinkedHashMap<String, TaskEntity>()
            unscheduledTasks.forEach { targetTasksMap[it.id] = it }
            missedTasks.forEach { targetTasksMap[it.id] = it.copy(scheduledDate = null, scheduledTime = null) }
            replanCandidates.forEach { targetTasksMap[it.id] = it }
            val targetTasks = targetTasksMap.values.toList()

            if (targetTasks.isEmpty()) {
                return Result.success()
            }

            // Fixed tasks are active tasks NOT being rescheduled (locked or manual future tasks)
            val fixedTasks = allTasks
                .filter { it.quadrant != com.neuroflow.app.domain.model.Quadrant.ELIMINATE }
                .filterNot { task ->
                    if (targetTasksMap.containsKey(task.id)) return@filterNot true
                    val parentId = task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
                    if (parentId != task.id && targetTasksMap.containsKey(parentId)) return@filterNot true
                    false
                }
            val fixedWindows = fixedTasks.mapNotNull { task ->
                if (task.scheduledTime == null) return@mapNotNull null
                val start = task.resolveScheduledEpochMillis() ?: return@mapNotNull null
                val duration = TaskBlockPlan.plannedMinutes(task)
                val end = start + duration * 60_000L
                if (end > dayStart && start < dayEnd) TimeSlotWindow(start, end) else null
            }

            // Fetch external busy windows from Android Native Calendar Sync
            val externalBusy = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd) ?: emptyList()
            val totalBusyWindows = externalBusy + fixedWindows

            val allSessions = runCatching {
                sessionRepository.getAllSessions()
            }.getOrDefault(emptyList())
            val closedSessions = allSessions.filter { it.endedAt != null }
            val activeTaskId = focusedTaskIds.firstOrNull()
            val microStepCounts = targetTasks.associate { task ->
                val steps = taskRepository.getMicroSteps(task.id)
                task.id to Pair(steps.count { it.isCompleted }, steps.size)
            }
            val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.first() ?: emptyList()

            fun isCognitiveDemand(t: TaskEntity): Boolean =
                t.energyLevel != com.neuroflow.app.domain.model.EnergyLevel.LOW ||
                    t.isFrog ||
                    t.taskType == com.neuroflow.app.domain.model.TaskType.ANALYTICAL

            val completedMinutesToday = allKnownTasks
                .filter {
                    it.status == TaskStatus.COMPLETED &&
                        (it.effectiveScheduledDateMillis()?.let { d -> d in dayStart..dayEnd } == true ||
                            it.completedAt?.let { c -> c in dayStart..dayEnd } == true) &&
                        isCognitiveDemand(it)
                }
                .sumOf { TaskBlockPlan.plannedMinutes(it) }
            val fixedCommittedMinutes = fixedTasks
                .filter { task ->
                    if (task.scheduledTime == null) return@filter false
                    val start = task.resolveScheduledEpochMillis() ?: return@filter false
                    val duration = TaskBlockPlan.plannedMinutes(task)
                    val end = start + duration * 60_000L
                    end > dayStart && start < dayEnd && isCognitiveDemand(task)
                }
                .sumOf { TaskBlockPlan.plannedMinutes(it) }
            val capacityMinutes = prefs.dailyDeepWorkTargetMinutes.takeIf { it > 0 } ?: 240
            val effectiveRemainingCapacity = (capacityMinutes - (completedMinutesToday + fixedCommittedMinutes)).coerceAtLeast(0)

            val outcome = scheduleOrchestrator.orchestrateDayScheduleWithOutcome(
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
                microStepCounts = microStepCounts,
                dailyCognitiveCapacityMinutes = effectiveRemainingCapacity,
                allowFutureDeadlinedBacklog = false
            )
            val placements = outcome.placements

            if (placements.isNotEmpty()) {
                val horizonCtx = com.neuroflow.app.domain.scheduler.SchedulingContextFactory.buildHorizonContext(
                    allKnownTasks = allKnownTasks,
                    closedSessions = closedSessions,
                    sleepLogs = sleepLogs,
                    prefs = prefs,
                    referenceNowMillis = nowMillis,
                    zoneId = zoneId
                )
                val dayCtx = com.neuroflow.app.domain.scheduler.SchedulingContextFactory.buildDayContext(
                    horizon = horizonCtx,
                    dayDate = dayDate,
                    prefs = prefs,
                    referenceNowMillis = nowMillis,
                    zoneId = zoneId,
                    sleepTelemetryRepository = sleepTelemetryRepository
                )
                val protectedBusyWindows = dayCtx.protectedSlots.map { it.resolveForDate(dayDate, zoneId) }
                val conflictingPreviousIds = findConflictingPreviousSlotIds(
                    replanCandidates = replanCandidates,
                    placements = placements,
                    busyWindows = totalBusyWindows + protectedBusyWindows,
                    nowMillis = nowMillis
                )
                val previousSlotUtilities = mutableMapOf<String, Double>()
                for (candidate in replanCandidates) {
                    if (candidate.id in conflictingPreviousIds) continue
                    val prevStart = candidate.resolveScheduledEpochMillis(nowMillis) ?: continue
                    val prevDuration = TaskBlockPlan.plannedMinutes(candidate).coerceAtLeast(5)
                    val placementMatch = placements.firstOrNull { p ->
                        val baseId = if (p.task.id.contains("#part")) p.task.id.substringBefore("#part") else p.task.id
                        baseId == candidate.id
                    } ?: continue
                    val prevSnap = circadianEnergyService.evaluateSnapshotWithStimulants(
                        timestampMillis = prevStart + (prevDuration * 30_000L),
                        wakeTimeMillis = dayCtx.wakeTimeMillis,
                        lastSleepDurationMinutes = dayCtx.lastSleepDurationMinutes,
                        continuousActiveFocusMinutes = 0,
                        zoneId = zoneId,
                        naps = dayCtx.naps,
                        chronotype = horizonCtx.chronotype,
                        manualPeakStartHour = horizonCtx.manualPeakStartHour,
                        learnedProfile = horizonCtx.learnedProfile,
                        rollingAverageSleepMinutes = horizonCtx.rollingAverageSleepMinutes,
                        cognitiveBlocks = dayCtx.physiologicalBlocks,
                        stimulantDoses = dayCtx.stimulantDoses,
                        socialJetlagLagHours = horizonCtx.socialJetlagLagHours
                    )
                    val prevScore = taskSchedulerService.evaluateTaskScoreWithFatigue(
                        task = placementMatch.task,
                        snapshot = prevSnap,
                        calibrationRatio = horizonCtx.calibrationRatioFor(placementMatch.task.category),
                        weights = horizonCtx.priorityWeights,
                        referenceNowMillis = nowMillis
                    ).totalScore
                    val prevHour = Instant.ofEpochMilli(prevStart).atZone(zoneId).hour
                    val prevIsWeekend = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.isWeekendDay(prevStart, zoneId)
                    val prevFeedbackDelta = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.computeAffinityDelta(
                        json = prefs.schedulingRejectionsJson,
                        category = placementMatch.task.category.name,
                        hour = prevHour,
                        nowMillis = nowMillis,
                        targetIsWeekend = prevIsWeekend
                    )
                    previousSlotUtilities[candidate.id] = prevScore + prevFeedbackDelta
                }

                applyPlacementsTransactionally(
                    placements = placements,
                    prefs = prefs,
                    replanVerdicts = replanVerdicts,
                    previousSlotUtilities = previousSlotUtilities,
                    conflictingPreviousIds = conflictingPreviousIds,
                    zoneId = zoneId
                )
                FocusWidgetUpdateWorker.enqueueImmediateUpdate(context)
            }

            // ADR-031: Deferral handling & reconciliation cleanup in background worker to prevent orphaned or stale blocks
            val placedBaseIds = placements.map {
                if (it.task.id.contains("#part")) it.task.id.substringBefore("#part") else it.task.id
            }.toSet()
            val deferredTaskIds = outcome.deferrals.map { it.taskId }.toSet()
            val tasksToReconcile = targetTasks.filter { task ->
                val baseId = if (task.id.contains("#part")) task.id.substringBefore("#part") else task.id
                baseId !in placedBaseIds || task.id in deferredTaskIds
            }

            for (taskToDefer in tasksToReconcile) {
                val fresh = taskRepository.getById(taskToDefer.id) ?: taskToDefer
                if (fresh.isScheduleLocked || !AutoSchedulingContracts.isMutableByAutoScheduler(fresh, nowMillis)) continue
                if (!SchedulingDateSanityValidator.isEntityEligibleForDay(
                        task = fresh,
                        targetDate = dayDate,
                        nowMillis = nowMillis,
                        zoneId = zoneId,
                        allowFutureDeadlinedBacklog = false
                    )
                ) {
                    continue
                }
                val cleanTitle = fresh.title.replace(Regex("""\s*\(Part \d+/\d+\)"""), "").trim()
                if (fresh.isAutoScheduled && (fresh.scheduledDate != null || fresh.scheduledTime != null || fresh.title != cleanTitle)) {
                    taskRepository.update(
                        fresh.copy(
                            title = cleanTitle,
                            scheduledDate = null,
                            scheduledTime = null,
                            isAutoScheduled = false,
                            plannedBlockMinutes = 0,
                            updatedAt = nowMillis
                        )
                    )
                    calendarSyncRepository?.deleteTaskEvent(fresh.id)
                }
                val part2Id = "${fresh.id}#part2"
                val existingPart2 = taskRepository.getById(part2Id) ?: taskRepository.getById("${fresh.id}_part2")
                if (existingPart2 != null && existingPart2.status != TaskStatus.COMPLETED && existingPart2.id !in focusedTaskIds) {
                    taskRepository.delete(existingPart2)
                    calendarSyncRepository?.deleteTaskEvent(existingPart2.id)
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("ScheduleAutoTasksWorker", "Failed in auto-scheduling pass", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally {
            synchronized(workerLock) {
                isWorkerRunning = false
            }
        }
    }

    private suspend fun applyPlacementsTransactionally(
        placements: List<ScheduledPlacement>,
        prefs: com.neuroflow.app.data.local.UserPreferences,
        replanVerdicts: Map<String, PlacementStability.ReplanVerdict> = emptyMap(),
        previousSlotUtilities: Map<String, Double> = emptyMap(),
        conflictingPreviousIds: Set<String> = emptySet(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        val nowMillis = System.currentTimeMillis()
        val focusedTaskIds = sessionRepository.getOpenSessions().filter { session ->
            val activeServiceTaskId = FocusService.activeSession.value?.taskId
            if (activeServiceTaskId != null && activeServiceTaskId == session.taskId) return@filter true
            val modeDurationSec = FocusTimerModeV2.entries
                .firstOrNull { it.name == session.sessionType }?.durationSeconds
            val maxDurationMs = if (modeDurationSec != null && modeDurationSec > 0L) {
                modeDurationSec * 1000L
            } else {
                120 * 60_000L
            }
            val elapsedMs = (nowMillis - session.startedAt - session.totalPausedMs).coerceAtLeast(0L)
            elapsedMs < maxDurationMs
        }.map { it.taskId }.toSet()
        val placementsByBaseId = placements.groupBy { placement ->
            if (placement.task.id.contains("#part")) {
                placement.task.id.substringBefore("#part")
            } else {
                placement.task.id
            }
        }

        for ((baseTaskId, taskPlacements) in placementsByBaseId) {
            val primaryPlacement = taskPlacements.first()
            val freshTask = taskRepository.getById(baseTaskId) ?: continue
            if (baseTaskId in focusedTaskIds) continue

            if (freshTask.status != TaskStatus.ACTIVE ||
                !AutoSchedulingContracts.isMutableByAutoScheduler(freshTask, nowMillis) ||
                !SchedulingDateSanityValidator.canApplyPlacementToEntity(
                    existingTask = freshTask,
                    proposedStartMillis = primaryPlacement.window.startMillis,
                    nowMillis = nowMillis,
                    zoneId = zoneId,
                    allowFutureDeadlinedBacklog = false
                )
            ) {
                continue
            }

            val (scheduledDate, scheduledTime) = splitMillisToDateAndTime(primaryPlacement.window.startMillis)
            val previousStartMillis = freshTask.resolveScheduledEpochMillis()
            val blockLengthChanged = freshTask.plannedBlockMinutes != primaryPlacement.window.durationMinutes

            val cleanTitle = freshTask.title.replace(Regex("""\s*\(Part \d+/\d+\)"""), "").trim()
            val isSplit = taskPlacements.size > 1 || primaryPlacement.task.id.endsWith("#part1")
            val isExistingContinuation = freshTask.parentTaskId != null ||
                freshTask.id.contains(Regex("""[#_]part\d+""")) ||
                freshTask.title.contains(Regex("""\(Part [2-9]/"""))
            val titleToUse = when {
                isSplit -> "$cleanTitle (Part 1/2)"
                isExistingContinuation -> freshTask.title
                else -> cleanTitle
            }
            val titleChanged = freshTask.title != titleToUse

            val part2Id = "${baseTaskId}#part2"
            val part2Placement = taskPlacements.getOrNull(1)
            val existingPart2 = if (!isExistingContinuation) (taskRepository.getById(part2Id) ?: taskRepository.getById("${baseTaskId}_part2")) else null
            val part2NeedsUpdate = if (isSplit && part2Placement != null && part2Placement.task.id.endsWith("#part2")) {
                (existingPart2 == null ||
                    existingPart2.resolveScheduledEpochMillis() != part2Placement.window.startMillis ||
                    existingPart2.plannedBlockMinutes != part2Placement.window.durationMinutes) &&
                    existingPart2?.status != TaskStatus.COMPLETED
            } else {
                existingPart2 != null && existingPart2.status != TaskStatus.COMPLETED
            }

            val verdict = replanVerdicts[baseTaskId]
            val isRoutineRefresh = verdict == PlacementStability.ReplanVerdict.PERIODIC_REFRESH &&
                baseTaskId !in conflictingPreviousIds
            val previousUtility = if (isRoutineRefresh) previousSlotUtilities[baseTaskId] else null
            val nextUtility = if (previousUtility != null) {
                val nextHour = Instant.ofEpochMilli(primaryPlacement.window.startMillis).atZone(zoneId).hour
                val nextIsWeekend = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.isWeekendDay(
                    primaryPlacement.window.startMillis,
                    zoneId
                )
                val nextFeedbackDelta = com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore.computeAffinityDelta(
                    json = prefs.schedulingRejectionsJson,
                    category = primaryPlacement.task.category.name,
                    hour = nextHour,
                    nowMillis = nowMillis,
                    targetIsWeekend = nextIsWeekend
                )
                primaryPlacement.scoreBreakdown.totalScore + nextFeedbackDelta
            } else null

            if (!part2NeedsUpdate &&
                !titleChanged &&
                !blockLengthChanged &&
                !PlacementStability.isMoveWorthApplying(
                    previousStartMillis = previousStartMillis,
                    nextStartMillis = primaryPlacement.window.startMillis,
                    nowMillis = nowMillis,
                    previousUtility = previousUtility,
                    nextUtility = nextUtility,
                    bypassUtilityThreshold = !isRoutineRefresh
                )
            ) {
                continue
            }

            val nextStartHour = Instant.ofEpochMilli(primaryPlacement.window.startMillis)
                .atZone(ZoneId.systemDefault()).hour
            val clearedRejection = freshTask.userSchedulingRating == -1 &&
                freshTask.schedulingRejectedSlotHour != nextStartHour
            val updatedTask = freshTask.copy(
                title = titleToUse,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                habitDate = if (freshTask.recurrence != Recurrence.NONE) scheduledDate else freshTask.habitDate,
                isAutoScheduled = true,
                plannedBlockMinutes = primaryPlacement.window.durationMinutes,
                userSchedulingRating = if (clearedRejection) 0 else freshTask.userSchedulingRating,
                schedulingRejectedSlotHour = if (clearedRejection) null else freshTask.schedulingRejectedSlotHour,
                lastAutoScheduledAt = nowMillis,
                lastStabilizedAt = nowMillis,
                updatedAt = nowMillis
            )

            taskRepository.update(updatedTask)
            val shouldSyncToCalendar = when (prefs.calendarSyncScope) {
                "ALL_SCHEDULED" -> true
                "DISABLED" -> false
                else -> updatedTask.isFrog || updatedTask.isScheduleLocked
            }
            if (shouldSyncToCalendar) {
                calendarSyncRepository?.upsertTaskEvent(
                    updatedTask,
                    startMillis = primaryPlacement.window.startMillis,
                    durationMinutes = primaryPlacement.window.durationMinutes,
                    targetCalendarId = prefs.calendarSyncTargetId
                )
            }

            // Persist Ultradian Split Part 2 as a linked continuation block if split occurred, otherwise clean up orphaned part2
            if (isSplit && part2Placement != null && part2Placement.task.id.endsWith("#part2") && existingPart2?.status != TaskStatus.COMPLETED) {
                val (p2Date, p2Time) = splitMillisToDateAndTime(part2Placement.window.startMillis)
                val part2Title = "$cleanTitle (Part 2/2)"
                val part2Entity = (existingPart2 ?: freshTask).copy(
                    id = existingPart2?.id ?: part2Id,
                    title = part2Title,
                    parentTaskId = baseTaskId,
                    dependsOnTaskIds = baseTaskId,
                    estimatedDurationMinutes = part2Placement.window.durationMinutes,
                    plannedBlockMinutes = part2Placement.window.durationMinutes,
                    scheduledDate = p2Date,
                    scheduledTime = p2Time,
                    isAutoScheduled = true,
                    isFrog = false,
                    recurrence = Recurrence.NONE,
                    recurrenceIntervalDays = 0,
                    isHabitual = false,
                    habitDate = null,
                    sessionCount = existingPart2?.sessionCount ?: 0,
                    totalTimeTrackedMinutes = existingPart2?.totalTimeTrackedMinutes ?: 0f,
                    actualDurationMinutes = existingPart2?.actualDurationMinutes ?: 0f,
                    lastSessionDurationMinutes = existingPart2?.lastSessionDurationMinutes ?: 0f,
                    lastAutoScheduledAt = nowMillis,
                    lastStabilizedAt = nowMillis,
                    updatedAt = nowMillis
                )
                if (existingPart2 != null) {
                    taskRepository.update(part2Entity)
                } else {
                    taskRepository.insert(part2Entity)
                }
                if (shouldSyncToCalendar) {
                    calendarSyncRepository?.upsertTaskEvent(
                        part2Entity,
                        startMillis = part2Placement.window.startMillis,
                        durationMinutes = part2Placement.window.durationMinutes,
                        targetCalendarId = prefs.calendarSyncTargetId
                    )
                }
            } else if (!isExistingContinuation) {
                if (existingPart2 != null && existingPart2.status != TaskStatus.COMPLETED) {
                    taskRepository.delete(existingPart2)
                    calendarSyncRepository?.deleteTaskEvent(existingPart2.id)
                }
                val legacyPart2 = taskRepository.getById("${baseTaskId}_part2")
                if (legacyPart2 != null && legacyPart2.status != TaskStatus.COMPLETED) {
                    taskRepository.delete(legacyPart2)
                    calendarSyncRepository?.deleteTaskEvent(legacyPart2.id)
                }
            }
        }
    }
}
