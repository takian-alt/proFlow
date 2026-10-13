package com.neuroflow.app.presentation.matrix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.engine.TaskScoringEngine
import com.neuroflow.app.domain.model.Quadrant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatrixUiState(
    val tasksByQuadrant: Map<Quadrant, List<TaskEntity>> = emptyMap(),
    val quadrantCounts: Map<Quadrant, Int> = emptyMap(),
    val topScoredTaskId: String? = null,
    val allActiveTasks: List<TaskEntity> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MatrixViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatrixUiState())
    val uiState: StateFlow<MatrixUiState> = _uiState.asStateFlow()

    /**
     * Ranking depends on the task table and the user's weights, both of which change rarely, so it
     * is kept in one flow and re-computed only when either of them moves.
     */
    private data class RankedTasks(
        val tasksByQuadrant: Map<Quadrant, List<TaskEntity>>,
        val quadrantCounts: Map<Quadrant, Int>,
        val topScoredTaskId: String?,
        val allActiveTasks: List<TaskEntity>,
        val preferences: UserPreferences
    )

    private val rankedTasks = combine(
        taskRepository.observeActiveTasks(),
        preferencesDataStore.preferencesFlow
    ) { tasks, prefs ->
        val byQuadrant = Quadrant.entries.associateWith { q -> tasks.filter { it.quadrant == q } }
        val sorted = TaskScoringEngine.sortedByScore(tasks, prefs)
        RankedTasks(
            tasksByQuadrant = byQuadrant,
            quadrantCounts = byQuadrant.mapValues { it.value.size },
            topScoredTaskId = sorted.firstOrNull()?.id,
            allActiveTasks = tasks,
            preferences = prefs
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        RankedTasks(emptyMap(), emptyMap(), null, emptyList(), UserPreferences())
    )

    init {
        viewModelScope.launch {
            rankedTasks.collect { ranked ->
                _uiState.value = MatrixUiState(
                    tasksByQuadrant = ranked.tasksByQuadrant,
                    quadrantCounts = ranked.quadrantCounts,
                    topScoredTaskId = ranked.topScoredTaskId,
                    allActiveTasks = ranked.allActiveTasks,
                    preferences = ranked.preferences,
                    isLoading = false
                )
            }
        }
    }

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            val autoQuadrant = TaskScoringEngine.autoAssignQuadrant(task, System.currentTimeMillis())
            taskRepository.insert(task.copy(quadrant = autoQuadrant))
        }
    }

    fun insertTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insert(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.delete(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.update(task.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun moveTask(task: TaskEntity, targetQuadrant: Quadrant) {
        viewModelScope.launch {
            taskRepository.update(task.copy(quadrant = targetQuadrant, updatedAt = System.currentTimeMillis()))
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            taskRepository.completeAndRecur(task, now)
            preferencesDataStore.updatePreferences { prefs ->
                val todayStart = startOfLocalDay(now)
                val yesterdayStart = startOfPreviousLocalDay(todayStart)
                val newStreak = when {
                    prefs.lastActiveDate >= todayStart -> prefs.dailyStreak
                    prefs.lastActiveDate >= yesterdayStart -> prefs.dailyStreak + 1
                    else -> 1
                }
                prefs.copy(
                    totalTasksCompleted = prefs.totalTasksCompleted + 1,
                    dailyStreak = newStreak,
                    lastActiveDate = now,
                    longestStreak = maxOf(prefs.longestStreak, newStreak)
                )
            }
        }
    }

    fun completeTaskWithTime(task: TaskEntity, manualMinutes: Float) {
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
            val mape = if (task.estimatedDurationMinutes > 0)
                AnalyticsEngine.computeMape(task.estimatedDurationMinutes.toFloat(), manualMinutes)
            else null
            val smape = if (task.estimatedDurationMinutes > 0)
                AnalyticsEngine.computeSmape(task.estimatedDurationMinutes.toFloat(), manualMinutes)
            else null
            val taskWithTime = task.copy(
                actualDurationMinutes = manualMinutes,
                totalTimeTrackedMinutes = task.totalTimeTrackedMinutes + manualMinutes,
                sessionCount = task.sessionCount + 1,
                estimationErrorMape = mape,
                estimationErrorSmape = smape,
                updatedAt = now
            )
            taskRepository.completeAndRecur(taskWithTime, now)
            preferencesDataStore.updatePreferences { prefs ->
                val todayStart = startOfLocalDay(now)
                val yesterdayStart = startOfPreviousLocalDay(todayStart)
                val newStreak = when {
                    prefs.lastActiveDate >= todayStart -> prefs.dailyStreak
                    prefs.lastActiveDate >= yesterdayStart -> prefs.dailyStreak + 1
                    else -> 1
                }
                prefs.copy(
                    totalTasksCompleted = prefs.totalTasksCompleted + 1,
                    totalFocusMinutes = prefs.totalFocusMinutes + manualMinutes.toInt(),
                    dailyStreak = newStreak,
                    lastActiveDate = now,
                    longestStreak = maxOf(prefs.longestStreak, newStreak)
                )
            }
        }
    }

    /** Logs time against a task without completing it (used from Log Time screen). */
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
                prefs.copy(totalFocusMinutes = prefs.totalFocusMinutes + manualMinutes.toInt())
            }
        }
    }
}
