package com.neuroflow.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class ProgressSummaryUiState(
    val completedThisWeek: Int = 0,
    val deltaTasksVsLastWeek: Int = 0,
    val focusMinutesThisWeek: Int = 0,
    val weeklyTrendObservation: String = "Keep building momentum!"
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    taskRepository: TaskRepository,
    sessionRepository: SessionRepository
) : ViewModel() {

    val summaryState: StateFlow<ProgressSummaryUiState> = combine(
        taskRepository.observeCompletedTasks(),
        sessionRepository.observeAll()
    ) { completedList, sessions ->
        calculateWeeklyProgressSummary(completedList, sessions, System.currentTimeMillis())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProgressSummaryUiState()
    )
}

internal fun calculateWeeklyProgressSummary(
    completed: List<TaskEntity>,
    sessions: List<TimeSessionEntity>,
    nowMillis: Long
): ProgressSummaryUiState {
    val thisWeekStartCalendar = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val daysFromWeekStart = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
        add(Calendar.DAY_OF_YEAR, -daysFromWeekStart)
    }
    val thisWeekStart = thisWeekStartCalendar.timeInMillis
    val lastWeekStart = Calendar.getInstance().apply {
        timeInMillis = thisWeekStart
        add(Calendar.WEEK_OF_YEAR, -1)
    }.timeInMillis
    val nextWeekStart = Calendar.getInstance().apply {
        timeInMillis = thisWeekStart
        add(Calendar.WEEK_OF_YEAR, 1)
    }.timeInMillis

    var thisWeekCount = 0
    var lastWeekCount = 0

    completed.forEach { task ->
        val compAt = task.completedAt ?: return@forEach

        if (compAt >= thisWeekStart && compAt < nextWeekStart) {
            thisWeekCount++
        } else if (compAt >= lastWeekStart && compAt < thisWeekStart) {
            lastWeekCount++
        }
    }

    // Totalled from the sessions themselves: a task's lifetime total would credit this week
    // with hours spent weeks ago, and time logged on a task left unfinished would vanish.
    val thisWeekFocusMinutes = sessions
        .filter { it.startedAt in thisWeekStart until nextWeekStart }
        .sumOf { it.trackedMinutes(nowMillis).toDouble() }
        .toFloat()

    val delta = thisWeekCount - lastWeekCount
    val observation = when {
        delta > 2 -> "🔥 You've completed $delta more tasks than this time last week!"
        delta > 0 -> "📈 Ahead of last week's pace. Nice work!"
        delta == 0 && thisWeekCount > 0 -> "⚡ On track matching last week's productive output."
        thisWeekCount == 0 -> "🎯 Start small with one focused sprint today."
        else -> "💪 Consistency beats intensity. Every session counts."
    }

    return ProgressSummaryUiState(
        completedThisWeek = thisWeekCount,
        deltaTasksVsLastWeek = delta,
        focusMinutesThisWeek = thisWeekFocusMinutes.toInt(),
        weeklyTrendObservation = observation
    )
}
