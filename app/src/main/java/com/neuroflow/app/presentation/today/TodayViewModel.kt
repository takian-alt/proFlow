package com.neuroflow.app.presentation.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.effectiveScheduleAnchorMillis
import com.neuroflow.app.data.local.entity.isRecurringWithAnchor
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfNextLocalDay
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.DistractionEngine
import com.neuroflow.app.domain.engine.TaskScoringEngine
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.DueSlot
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class UndoAction(
    val task: TaskEntity,
    val actionType: UndoActionType,
    val job: Job
)

enum class UndoActionType {
    COMPLETE,
    POSTPONE
}

data class DailyDistractionState(
    val permissionGranted: Boolean,
    val screenMinutes: Float,
    val distractingMinutes: Float,
    /** Deep work net of the flagged time that sat inside those same blocks */
    val focusMinutes: Float,
    val distractionScore: Float,
    val hasEnoughSignal: Boolean,
    val flaggedAppCount: Int,
    val overlapMinutes: Float,
    val topApps: List<Pair<String, Float>>
)

data class TodayUiState(
    val recommendedTask: TaskEntity? = null,
    val recommendationReason: String = "",
    val overdueTasks: List<TaskEntity> = emptyList(),
    val upNextTasks: List<TaskEntity> = emptyList(),
    val laterTodayTasks: List<TaskEntity> = emptyList(),
    val completedTodayCount: Int = 0,
    val focusMinutesToday: Float = 0f,
    val deepWorkTargetMinutes: Int = 240,
    val deepWorkSessionCount: Int = 0,
    val distractingAppCount: Int = 0,
    val unscheduledCount: Int = 0,
    val showEveningWrapUp: Boolean = false,
    val weeklyIntent: String = "",
    val pendingUndoAction: UndoAction? = null,
    val allRemainingTodayTasks: List<TaskEntity> = emptyList(),
    val allActiveTasks: List<TaskEntity> = emptyList(),
    /** A protected block that is happening now; it holds the hero position while it lasts. */
    val dueProtectedSlot: DueSlot? = null,
    /** Protected blocks starting within the lead-in window, soonest first. */
    val approachingProtectedSlots: List<DueSlot> = emptyList(),
    val todayStartMillis: Long = 0L
)

private data class TodayTaskSource(
    val activeTasks: List<TaskEntity>,
    val completedTasks: List<TaskEntity>,
    val preferences: UserPreferences
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val pendingDismissIds = MutableStateFlow<Set<String>>(emptySet())
    private val pendingUndoActionState = MutableStateFlow<UndoAction?>(null)

    /**
     * Re-checks the day on every minute boundary: protected blocks flip from "starts in 5 min" to
     * "in progress" on the wall clock, and a screen left open overnight must stop showing yesterday.
     */
    private val minuteTick = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                delay((MINUTE_MILLIS - now % MINUTE_MILLIS).coerceAtLeast(1_000L))
                val previousDay = startOfLocalDay(minuteTick.value)
                minuteTick.value = System.currentTimeMillis()
                if (startOfLocalDay(minuteTick.value) != previousDay) refreshDailyDistraction()
            }
        }
    }

    private val taskSource = combine(
        taskRepository.observeActiveTasks(),
        taskRepository.observeCompletedTasks(),
        preferencesDataStore.preferencesFlow
    ) { active, completed, prefs -> TodayTaskSource(active, completed, prefs) }

    val uiState: StateFlow<TodayUiState> = combine(
        taskSource,
        sessionRepository.observeAll(),
        pendingDismissIds,
        pendingUndoActionState,
        minuteTick
    ) { source, sessions, dismissIds, undoAction, _ ->
        buildUiState(source.activeTasks, source.completedTasks, source.preferences, sessions, dismissIds, undoAction)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TodayUiState()
    )

    private val _dailyDistraction = MutableStateFlow<DailyDistractionState?>(null)
    val dailyDistraction: StateFlow<DailyDistractionState?> = _dailyDistraction.asStateFlow()

    private var distractionRefreshJob: Job? = null

    /**
     * Reads a day of UsageEvents, which is real disk/binder work over potentially thousands of
     * events, so it stays off the combine path and only runs when the screen asks for it. A new
     * request supersedes the one in flight, otherwise two resumes can land out of order.
     */
    fun refreshDailyDistraction() {
        distractionRefreshJob?.cancel()
        distractionRefreshJob = viewModelScope.launch(Dispatchers.Default) {
            val prefs = preferencesDataStore.preferencesFlow.first()
            val now = System.currentTimeMillis()
            // Read the sessions straight from the store rather than uiState.value: on the first
            // frame the combined state is still the empty default, which would make the day look
            // 100% distracted because its focus minutes read as zero.
            val sessionsToday = todayFocusSessions(sessionRepository.getAllSessions(), now)
            val focusMinutes = focusMinutesOf(sessionsToday, now)

            val usage = DistractionEngine.dailyScreenUsage(
                context = context,
                distractingPackages = prefs.distractingPackages,
                startOfDayMillis = startOfLocalDay(now),
                endMillis = now,
                focusWindows = focusWindows(sessionsToday, now)
            )
            val focusMs = (focusMinutes * 60_000f).toLong()
            val overlapMs = usage.distractingInsideFocusMs.coerceAtMost(focusMs)
            val netFocusMinutes = focusMinutes - overlapMs / 60_000f
            val trackedMs = usage.distractingMs + (focusMs - overlapMs)

            _dailyDistraction.value = DailyDistractionState(
                permissionGranted = usage.permissionGranted,
                screenMinutes = usage.totalScreenMs / 60_000f,
                distractingMinutes = usage.distractingMs / 60_000f,
                focusMinutes = netFocusMinutes,
                distractionScore = DistractionEngine.dailyDistractionScore(usage.distractingMs, netFocusMinutes),
                hasEnoughSignal = trackedMs >= DistractionEngine.MIN_TRACKED_MS_FOR_VERDICT,
                flaggedAppCount = prefs.distractingPackages.size,
                overlapMinutes = overlapMs / 60_000f,
                topApps = usage.distractingByApp.take(3)
                    .map { (pkg, ms) -> DistractionEngine.appLabel(context, pkg) to (ms / 60_000f) }
            )
        }
    }

    private fun buildUiState(
        allActive: List<TaskEntity>,
        allCompleted: List<TaskEntity>,
        prefs: UserPreferences,
        allSessions: List<TimeSessionEntity>,
        dismissIds: Set<String>,
        undoAction: UndoAction?
    ): TodayUiState {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val calMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calMidnight.timeInMillis
        val todayEndExclusive = startOfNextLocalDay(todayStart)

        // Filter visible active tasks (excluding currently pending dismissed ones)
        val visibleActive = allActive.filter { it.id !in dismissIds }

        // Today's completed tasks
        val completedToday = allCompleted.filter { task ->
            task.completedAt?.let { it >= todayStart && it < todayEndExclusive } == true
        }

        // Sessions, not task totals: a task's totalTimeTrackedMinutes carries time from every
        // day it was worked on, and time logged against still-open tasks never lands on a
        // completed task at all.
        val sessionsToday = todayFocusSessions(allSessions, now)
        val focusMinutesToday = focusMinutesOf(sessionsToday, now)

        // Filter tasks that belong to Today
        val todayTasks = visibleActive.filter { task ->
            val hasTodaySchedule = task.scheduledDate?.let {
                it >= todayStart && it < todayEndExclusive
            } == true
            val isOverdue = (task.scheduledDate != null && task.scheduledDate < todayStart) ||
                    (task.deadlineDate != null && task.deadlineDate < todayStart)
            val isDeadlineToday = task.deadlineDate?.let {
                it >= todayStart && it < todayEndExclusive
            } == true
            val isDoFirstUnscheduled = task.scheduledDate == null && task.quadrant == Quadrant.DO_FIRST
            val isRecurringToday = task.isRecurringWithAnchor() &&
                    task.effectiveScheduleAnchorMillis()?.let {
                        it >= todayStart && it < todayEndExclusive
                    } == true

            hasTodaySchedule || isOverdue || isDeadlineToday || isDoFirstUnscheduled || isRecurringToday
        }

        // Unscheduled backlog count
        val unscheduledCount = visibleActive.count { it.scheduledDate == null && it !in todayTasks }

        // Categorize into Overdue, Up Next, Later Today
        val overdue = mutableListOf<TaskEntity>()
        val upNext = mutableListOf<TaskEntity>()
        val laterToday = mutableListOf<TaskEntity>()

        val upNextThresholdMs = now + (2 * 60 * 60 * 1000L) // Next 2 hours

        todayTasks.forEach { task ->
            val isOverdue = (task.scheduledDate != null && task.scheduledDate < todayStart) ||
                    (task.deadlineDate != null && task.deadlineDate < todayStart)
            if (isOverdue) {
                overdue.add(task)
            } else {
                val scheduleTime = task.scheduledTime ?: 0L
                val taskEffectiveTime = (task.scheduledDate ?: todayStart) + scheduleTime
                if (taskEffectiveTime <= upNextThresholdMs) {
                    upNext.add(task)
                } else {
                    laterToday.add(task)
                }
            }
        }

        // Score tasks to find hero recommendation
        val scoredTasks = todayTasks.map { task ->
            task to TaskScoringEngine.score(task, prefs, visibleActive, now)
        }.sortedByDescending { it.second }

        val topRecommendation = scoredTasks.firstOrNull()?.first
        val recommendationReason = topRecommendation?.let { getRecommendationReason(it, now) } ?: ""

        val dueProtectedSlot = ProtectedSlotClock.takeoverOf(prefs.protectedTimeSlots, now)
        val approachingProtectedSlots = ProtectedSlotClock.approaching(prefs.protectedTimeSlots, now)

        // When a block holds the hero position the recommendation moves down into the queue rather
        // than disappearing with the card, so it stays the first thing tasks can be read against.
        val upNextOrdered = if (dueProtectedSlot != null && topRecommendation != null && upNext.any { it.id == topRecommendation.id }) {
            listOf(topRecommendation) + upNext.filter { it.id != topRecommendation.id }
        } else {
            upNext
        }

        val wrapUpStartHour = maxOf(19, prefs.sleepHour - 2)
        val showEveningWrapUp = hour >= wrapUpStartHour && hour < 24

        return TodayUiState(
            recommendedTask = topRecommendation,
            recommendationReason = recommendationReason,
            overdueTasks = overdue,
            upNextTasks = upNextOrdered,
            laterTodayTasks = laterToday,
            completedTodayCount = completedToday.size,
            focusMinutesToday = focusMinutesToday,
            deepWorkTargetMinutes = prefs.dailyDeepWorkTargetMinutes,
            deepWorkSessionCount = sessionsToday.size,
            distractingAppCount = prefs.distractingPackages.size,
            unscheduledCount = unscheduledCount,
            showEveningWrapUp = showEveningWrapUp,
            weeklyIntent = prefs.weeklyIntent,
            pendingUndoAction = undoAction,
            allRemainingTodayTasks = todayTasks,
            allActiveTasks = visibleActive,
            dueProtectedSlot = dueProtectedSlot,
            approachingProtectedSlots = approachingProtectedSlots,
            todayStartMillis = todayStart
        )
    }

    private fun getRecommendationReason(task: TaskEntity, now: Long): String {
        return when {
            task.isFrog -> "🐸 Eat the Frog (Daily Priority)"
            task.deadlineDate != null && task.deadlineDate < now -> "Overdue deadline"
            task.deadlineDate != null && task.deadlineDate <= now + (4 * 3600 * 1000L) -> "Due very soon"
            task.quadrant == Quadrant.DO_FIRST -> "Important & Urgent"
            task.impactScore >= 80 -> "High strategic impact"
            task.scheduledDate != null -> "Scheduled for today"
            else -> "Top algorithmic recommendation"
        }
    }

    fun quickBrainDump(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val newTask = TaskEntity(
                title = title.trim(),
                quadrant = Quadrant.SCHEDULE,
                scheduledDate = cal.timeInMillis,
                impactScore = 50,
                status = TaskStatus.ACTIVE
            )
            taskRepository.insert(newTask)
        }
    }

    fun completeTaskWithUndo(task: TaskEntity) {
        pendingUndoActionState.value?.job?.cancel() // cancel any prior pending undo

        pendingDismissIds.value = pendingDismissIds.value + task.id

        val job = viewModelScope.launch {
            delay(4000)
            taskRepository.completeAndRecur(task, System.currentTimeMillis())
            pendingDismissIds.value = pendingDismissIds.value - task.id
            if (pendingUndoActionState.value?.task?.id == task.id) {
                pendingUndoActionState.value = null
            }
        }

        pendingUndoActionState.value = UndoAction(task, UndoActionType.COMPLETE, job)
    }

    fun postponeTaskWithUndo(task: TaskEntity) {
        pendingUndoActionState.value?.job?.cancel()

        pendingDismissIds.value = pendingDismissIds.value + task.id

        val job = viewModelScope.launch {
            delay(4000)
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            taskRepository.update(
                task.copy(
                    scheduledDate = cal.timeInMillis,
                    postponeCount = task.postponeCount + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
            pendingDismissIds.value = pendingDismissIds.value - task.id
            if (pendingUndoActionState.value?.task?.id == task.id) {
                pendingUndoActionState.value = null
            }
        }

        pendingUndoActionState.value = UndoAction(task, UndoActionType.POSTPONE, job)
    }

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insert(task)
        }
    }

    fun rescheduleToTomorrow(task: TaskEntity) {
        pendingUndoActionState.value?.job?.cancel()
        viewModelScope.launch {
            taskRepository.rescheduleToTomorrow(task)
        }
    }

    fun undo() {
        val currentAction = pendingUndoActionState.value ?: return
        currentAction.job.cancel()
        pendingDismissIds.value = pendingDismissIds.value - currentAction.task.id
        pendingUndoActionState.value = null
    }

    fun pushRemainingToTomorrow() {
        val remaining = uiState.value.allRemainingTodayTasks
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val tomorrowMidnight = cal.timeInMillis
            remaining.forEach { task ->
                taskRepository.update(
                    task.copy(
                        scheduledDate = tomorrowMidnight,
                        postponeCount = task.postponeCount + 1,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun setFrogTask(taskId: String) {
        viewModelScope.launch {
            // First clear any existing frog task
            val active = taskRepository.getActiveTasks()
            active.filter { it.isFrog }.forEach {
                taskRepository.update(it.copy(isFrog = false, updatedAt = System.currentTimeMillis()))
            }
            val target = taskRepository.getById(taskId)
            if (target != null) {
                taskRepository.update(target.copy(isFrog = true, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun updateWeeklyIntent(intent: String) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { it.copy(weeklyIntent = intent) }
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
