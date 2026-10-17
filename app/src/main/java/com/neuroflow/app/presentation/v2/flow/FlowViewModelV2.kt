package com.neuroflow.app.presentation.v2.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class FlowUiStateV2(
    val activeTasks: List<TaskEntity> = emptyList(),
    val filteredTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val energyUi: EnergyScoreRepository.EnergyUiModel? = null,
    val recommendedTask: TaskEntity? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val overdueTasksCount: Int = 0,
    val showZeroGuiltPrompt: Boolean = false,
    val selectedTag: String? = null,
    val availableTags: List<String> = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin"),
    val isEveningShutdownTime: Boolean = false,
    val eveningShutdownDismissed: Boolean = false,
    val lastCompletedTask: TaskEntity? = null,
    val lastDeferredTask: Pair<TaskEntity, Long>? = null,
    val lastActionMessage: String? = null
)

@HiltViewModel
class FlowViewModelV2 @Inject constructor(
    private val taskRepository: TaskRepository,
    private val energyScoreRepository: EnergyScoreRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val noteRepository: NoteRepository? = null,
    private val sessionRepository: SessionRepository? = null
) : ViewModel() {

    private val _dismissedZeroGuilt = MutableStateFlow(false)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _eveningShutdownDismissed = MutableStateFlow(false)
    private val _lastCompletedTask = MutableStateFlow<TaskEntity?>(null)
    private val _lastDeferredTask = MutableStateFlow<Pair<TaskEntity, Long>?>(null)
    private val _lastActionMessage = MutableStateFlow<String?>(null)

    val recentNotes: Flow<List<NoteEntity>> = noteRepository?.allNotes ?: flowOf(emptyList())

    val uiState: StateFlow<FlowUiStateV2> = combine(
        combine(
            taskRepository.observeActiveTasks(),
            taskRepository.observeCompletedTasks(),
            energyScoreRepository.observeEnergy(),
            preferencesDataStore.preferencesFlow
        ) { active, completed, energy, prefs ->
            Quad(active, completed, energy, prefs)
        },
        combine(
            _dismissedZeroGuilt,
            _selectedTag,
            _eveningShutdownDismissed,
            _lastCompletedTask
        ) { dismissedGuilt, tag, shutdownDismissed, lastCompleted ->
            Quad(dismissedGuilt, tag, shutdownDismissed, lastCompleted)
        },
        combine(
            _lastDeferredTask,
            _lastActionMessage
        ) { lastDeferred, actionMsg ->
            Pair(lastDeferred, actionMsg)
        }
    ) { (activeTasks, completedTasks, energyUi, prefs), (dismissedGuilt, tag, shutdownDismissed, lastCompleted), (lastDeferred, actionMsg) ->
        val battery = energyUi?.availableEnergy ?: 50
        val currentEnergyZone = when {
            battery >= 65 -> EnergyLevel.HIGH
            battery >= 35 -> EnergyLevel.MEDIUM
            else -> EnergyLevel.LOW
        }

        // Overdue check for ADHD Zero-Guilt Safety Net
        val now = System.currentTimeMillis()
        val overdueTasks = activeTasks.filter { task ->
            task.postponeCount >= 3 ||
                    (task.scheduledTime != null && task.scheduledTime!! < now - 2 * 3600_000L) ||
                    (task.scheduledDate != null && task.scheduledDate!! < now - 86_400_000L)
        }
        val showZeroGuilt = overdueTasks.size >= 3 && !dismissedGuilt

        // Recommend highest-impact task matching current biological zone
        val recommended = activeTasks
            .filter { it.energyLevel == currentEnergyZone }
            .maxByOrNull { (if (it.isFrog) 1000 else 0) + it.impactScore }
            ?: activeTasks.maxByOrNull { (if (it.isFrog) 1000 else 0) + it.impactScore }

        // Filter tasks by selected tag
        val filtered = if (tag.isNullOrBlank() || tag.equals("All", ignoreCase = true)) {
            activeTasks
        } else {
            val query = tag.removePrefix("#").trim()
            activeTasks.filter { task ->
                task.tags.contains(query, ignoreCase = true) ||
                        task.title.contains(query, ignoreCase = true)
            }
        }

        // Evening shutdown after 19:00 (7 PM)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isEvening = currentHour >= 19

        FlowUiStateV2(
            activeTasks = activeTasks,
            filteredTasks = filtered,
            completedTasks = completedTasks,
            energyUi = energyUi,
            recommendedTask = recommended,
            preferences = prefs,
            isLoading = false,
            overdueTasksCount = overdueTasks.size,
            showZeroGuiltPrompt = showZeroGuilt,
            selectedTag = tag,
            isEveningShutdownTime = isEvening,
            eveningShutdownDismissed = shutdownDismissed,
            lastCompletedTask = lastCompleted,
            lastDeferredTask = lastDeferred,
            lastActionMessage = actionMsg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FlowUiStateV2(isLoading = true)
    )

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insert(task)
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            _lastCompletedTask.value = task
            _lastActionMessage.value = "Completed: ${task.title}"
            taskRepository.completeAndRecur(task, System.currentTimeMillis())
        }
    }

    fun undoCompleteTask() {
        val task = _lastCompletedTask.value ?: return
        viewModelScope.launch {
            taskRepository.update(
                task.copy(
                    status = TaskStatus.ACTIVE,
                    completedAt = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastCompletedTask.value = null
            _lastActionMessage.value = "Restored: ${task.title}"
        }
    }

    fun deferTask(task: TaskEntity, hours: Int = 2) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val previousTime = task.scheduledTime ?: now
            _lastDeferredTask.value = Pair(task, previousTime)
            val newScheduledTime = previousTime + hours * 3600_000L
            taskRepository.update(
                task.copy(
                    scheduledTime = newScheduledTime,
                    postponeCount = task.postponeCount + 1,
                    updatedAt = now
                )
            )
            _lastActionMessage.value = "Deferred by ${hours}h: ${task.title}"
        }
    }

    fun undoDeferTask() {
        val pair = _lastDeferredTask.value ?: return
        val (originalTask, prevTime) = pair
        viewModelScope.launch {
            taskRepository.update(
                originalTask.copy(
                    scheduledTime = prevTime,
                    postponeCount = (originalTask.postponeCount - 1).coerceAtLeast(0),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastDeferredTask.value = null
            _lastActionMessage.value = "Undid deferral for: ${originalTask.title}"
        }
    }

    fun clearActionMessage() {
        _lastActionMessage.value = null
    }

    /**
     * Zero-Guilt Fresh Start:
     * Moves all overdue tasks to the Icebox (Quadrant.ELIMINATE) with zero shame and resets postpone counters.
     */
    fun freshStartRollover() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val overdue = uiState.value.activeTasks.filter { task ->
                task.postponeCount >= 3 ||
                        (task.scheduledTime != null && task.scheduledTime!! < now - 2 * 3600_000L) ||
                        (task.scheduledDate != null && task.scheduledDate!! < now - 86_400_000L)
            }
            overdue.forEach { task ->
                taskRepository.update(
                    task.copy(
                        quadrant = Quadrant.ELIMINATE,
                        scheduledTime = null,
                        scheduledDate = null,
                        postponeCount = 0,
                        updatedAt = now
                    )
                )
            }
            _dismissedZeroGuilt.value = true
            _lastActionMessage.value = "Rolled ${overdue.size} tasks into Icebox. Fresh start active! 🌱"
        }
    }

    fun dismissZeroGuiltPrompt() {
        _dismissedZeroGuilt.value = true
    }

    /**
     * "Break It Down" Magic Wand:
     * Decomposes task into 3 microscopic actionable steps to overcome executive inertia.
     */
    fun breakDownTask(task: TaskEntity) {
        viewModelScope.launch {
            val cleanTitle = task.title.trim().take(30)
            val microSteps = listOf(
                StepByStepPlanStep("Open workspace & gather materials for $cleanTitle", false),
                StepByStepPlanStep("Spend 5 uninterrupted minutes on the very first sub-step", false),
                StepByStepPlanStep("Review progress & polish final output", false)
            )
            val serialized = StepByStepPlanCodec.serialize(microSteps)
            taskRepository.update(
                task.copy(
                    ifThenPlan = serialized,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastActionMessage.value = "Decomposed '${cleanTitle}' into 3 micro-steps 🪄"
        }
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun completeEveningShutdown() {
        _eveningShutdownDismissed.value = true
        _lastActionMessage.value = "Evening shutdown complete. Time to rest and restore 🌙"
    }

    fun saveQuickNote(content: String, title: String = "") {
        if (content.isBlank() || noteRepository == null) return
        viewModelScope.launch {
            noteRepository.saveNote(
                NoteEntity(
                    title = title.ifBlank { "Quick Thought" },
                    content = content.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastActionMessage.value = "Note saved to scratchpad 📝"
        }
    }

    fun logPastTime(task: TaskEntity, minutes: Float) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TimeSessionEntity(
                taskId = task.id,
                startedAt = now - (minutes * 60_000f).toLong(),
                endedAt = now,
                durationMinutes = minutes,
                sessionType = "MANUAL_LOG"
            )
            sessionRepository?.insert(session)
            val newTracked = task.totalTimeTrackedMinutes + minutes
            val newCount = task.sessionCount + 1
            taskRepository.update(
                task.copy(
                    totalTimeTrackedMinutes = newTracked,
                    sessionCount = newCount,
                    lastSessionDurationMinutes = minutes,
                    actualDurationMinutes = newTracked,
                    updatedAt = now
                )
            )
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalFocusMinutes = prefs.totalFocusMinutes + minutes.toInt())
            }
            _lastActionMessage.value = "Logged ${minutes.toInt()}m past time for ${task.title}"
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}


