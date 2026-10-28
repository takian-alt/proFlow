package com.neuroflow.app.presentation.logtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogTimeUiState(
    val allActiveTasks: List<TaskEntity> = emptyList()
)

@HiltViewModel
class LogTimeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val uiState: StateFlow<LogTimeUiState> = taskRepository.observeActiveTasks()
        .map { LogTimeUiState(allActiveTasks = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogTimeUiState()
        )

    fun logTimeOnly(task: TaskEntity, manualMinutes: Float) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TimeSessionEntity(
                taskId = task.id,
                startedAt = now - (manualMinutes * 60_000f).toLong(),
                endedAt = now,
                durationMinutes = manualMinutes,
                sessionType = "MANUAL_LOG"
            )
            sessionRepository.insert(session)
            taskRepository.update(
                task.copy(
                    totalTimeTrackedMinutes = task.totalTimeTrackedMinutes + manualMinutes,
                    sessionCount = task.sessionCount + 1,
                    updatedAt = now
                )
            )
            preferencesDataStore.updatePreferences { prefs ->
                val lastActive = prefs.lastActiveDate
                val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
                val isNewDay = lastActive < todayStart
                val isConsecutive = lastActive >= com.neuroflow.app.data.local.entity.startOfPreviousLocalDay(todayStart) && isNewDay
                val newStreak = when {
                    isConsecutive -> prefs.dailyStreak + 1
                    isNewDay -> 1
                    else -> prefs.dailyStreak
                }
                val longest = maxOf(prefs.longestStreak, newStreak)
                prefs.copy(
                    totalFocusMinutes = prefs.totalFocusMinutes + manualMinutes.toInt(),
                    lastActiveDate = now,
                    dailyStreak = newStreak,
                    longestStreak = longest
                )
            }
        }
    }
}
