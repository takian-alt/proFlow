package com.neuroflow.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class HistoryDateRange {
    ALL,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS
}

internal fun filterTasksByDateRange(
    tasks: List<TaskEntity>,
    range: HistoryDateRange,
    nowMillis: Long = System.currentTimeMillis()
): List<TaskEntity> {
    if (range == HistoryDateRange.ALL) return tasks

    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis
    val fromMillis = when (range) {
        HistoryDateRange.TODAY -> todayStart
        HistoryDateRange.LAST_7_DAYS -> Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis
        HistoryDateRange.LAST_30_DAYS -> Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -29)
        }.timeInMillis
        HistoryDateRange.ALL -> Long.MIN_VALUE
    }

    return tasks.filter { task ->
        val completedAt = task.completedAt ?: return@filter false
        completedAt >= fromMillis
    }
}

data class HistoryUiState(
    val completedTasks: List<TaskEntity> = emptyList(),
    val selectedDateRange: HistoryDateRange = HistoryDateRange.ALL,
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val isLoading: Boolean = true,
    val hasAnyCompletedTasks: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val calendarSyncRepository: com.neuroflow.app.data.calendar.CalendarSyncRepository? = null
) : ViewModel() {

    private val selectedDateRange = MutableStateFlow(HistoryDateRange.ALL)
    private val selectedTag = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                taskRepository.observeCompletedTasks(),
                userPreferencesDataStore.preferencesFlow.map { it.tagCatalog }.distinctUntilChanged(),
                selectedDateRange,
                selectedTag
            ) { tasks, catalogTags, range, tag ->
                val dateFiltered = filterTasksByDateRange(tasks, range)
                val query = tag?.removePrefix("#")?.removePrefix("@")?.trim()
                val tagFiltered = if (query.isNullOrBlank() || query.equals("All", ignoreCase = true)) {
                    dateFiltered
                } else {
                    val wordRegex = Regex("""(?i)(?:^|\s)[#@]?\Q$query\E(?=\s|$)""")
                    dateFiltered.filter { task ->
                        val hasMatchingTag = task.tags.split(",")
                            .map { it.trim().removePrefix("#").removePrefix("@") }
                            .any { it.equals(query, ignoreCase = true) }
                        hasMatchingTag || wordRegex.containsMatchIn(task.title)
                    }
                }
                val allTags = (catalogTags + tasks
                    .flatMap { it.tags.split(",") }
                    .map { it.trim().removePrefix("#").removePrefix("@") }
                    .filter { it.isNotBlank() }
                )
                    .distinctBy { it.lowercase() }
                    .sortedWith(String.CASE_INSENSITIVE_ORDER)
                val hasAny = tasks.isNotEmpty()
                Triple(tagFiltered, range, allTags) to hasAny
            }.collect { (params, hasAny) ->
                val (tasks, range, tags) = params
                _uiState.update {
                    it.copy(
                        completedTasks = tasks,
                        selectedDateRange = range,
                        availableTags = tags,
                        selectedTag = selectedTag.value,
                        isLoading = false,
                        hasAnyCompletedTasks = hasAny
                    )
                }
            }
        }
    }

    fun updateDateRange(range: HistoryDateRange) {
        selectedDateRange.value = range
    }

    fun updateTagFilter(tag: String?) {
        selectedTag.value = tag
    }

    suspend fun getSessionsForTask(taskId: String): List<TimeSessionEntity> {
        return sessionRepository.getByTaskId(taskId)
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.delete(task) }
    }

    fun undoDeleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insert(task)
            calendarSyncRepository?.handleTaskStateChanged(task)
        }
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            val restored = task.copy(
                status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
                completedAt = null,
                updatedAt = System.currentTimeMillis()
            )
            val existing = taskRepository.getById(restored.id)
            if (existing != null) {
                taskRepository.update(restored)
            } else {
                taskRepository.insert(restored)
            }
            calendarSyncRepository?.handleTaskStateChanged(restored)
            userPreferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalTasksCompleted = maxOf(0, prefs.totalTasksCompleted - 1))
            }
        }
    }
}
