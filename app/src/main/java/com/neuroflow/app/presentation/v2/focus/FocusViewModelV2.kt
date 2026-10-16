package com.neuroflow.app.presentation.v2.focus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.WoopEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.presentation.focus.StepByStepPlanCodec
import com.neuroflow.app.presentation.focus.StepByStepPlanStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class FocusTimerModeV2(val durationSeconds: Long, val title: String) {
    JUST_5_MIN(300L, "5m Micro-Start"),
    POMODORO_25(1500L, "25m Pomodoro"),
    ULTRADIAN_90(5400L, "90m Ultradian Block"),
    FLOW_STOPWATCH(0L, "Flow Stopwatch")
}

data class FocusUiStateV2(
    val task: TaskEntity? = null,
    val timerMode: FocusTimerModeV2 = FocusTimerModeV2.POMODORO_25,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val remainingSeconds: Long = 1500L,
    val activeSessionId: String? = null,
    val woopData: WoopEntity? = null,
    val microSteps: List<StepByStepPlanStep> = emptyList(),
    val isCompleted: Boolean = false,
    val pointsEarned: Int = 0,
    val nextTaskId: String? = null,
    val nextTaskTitle: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FocusViewModelV2 @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val woopRepository: WoopRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(FocusUiStateV2())
    val uiState: StateFlow<FocusUiStateV2> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTimeMillis: Long = 0L
    private var pauseCount: Int = 0

    init {
        loadTask()
        observePreferences()
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = taskRepository.getById(taskId)
            val woop = woopRepository.getByTaskId(taskId)
            val steps = task?.let { StepByStepPlanCodec.parse(it.ifThenPlan) } ?: emptyList()

            // Find next task
            val activeTasks = taskRepository.getActiveTasks()
            val next = activeTasks.firstOrNull { it.id != taskId }

            _uiState.update {
                it.copy(
                    task = task,
                    woopData = woop,
                    microSteps = steps,
                    nextTaskId = next?.id,
                    nextTaskTitle = next?.title,
                    isLoading = false
                )
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesDataStore.preferencesFlow.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    fun selectTimerMode(mode: FocusTimerModeV2) {
        if (_uiState.value.isRunning) return
        _uiState.update {
            it.copy(
                timerMode = mode,
                remainingSeconds = mode.durationSeconds,
                elapsedSeconds = 0L
            )
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning && !_uiState.value.isPaused) return

        val now = System.currentTimeMillis()
        if (sessionStartTimeMillis == 0L) {
            sessionStartTimeMillis = now
            val sessionId = UUID.randomUUID().toString()
            _uiState.update { it.copy(activeSessionId = sessionId) }
            viewModelScope.launch {
                sessionRepository.insert(
                    TimeSessionEntity(
                        id = sessionId,
                        taskId = taskId,
                        startedAt = now
                    )
                )
            }
        }

        _uiState.update { it.copy(isRunning = true, isPaused = false) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isRunning && !_uiState.value.isPaused) {
                delay(1000)
                if (!isActive || !_uiState.value.isRunning || _uiState.value.isPaused) break
                tickSecond()
            }
        }
    }

    fun tickSecond() {
        if (!_uiState.value.isRunning || _uiState.value.isPaused) return
        _uiState.update { current ->
            if (!current.isRunning || current.isPaused) return@update current
            val newElapsed = current.elapsedSeconds + 1
            val newRemaining = if (current.timerMode == FocusTimerModeV2.FLOW_STOPWATCH) {
                0L
            } else {
                (current.remainingSeconds - 1).coerceAtLeast(0)
            }
            current.copy(
                elapsedSeconds = newElapsed,
                remainingSeconds = newRemaining
            )
        }
    }

    fun pauseTimer() {
        pauseCount++
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isPaused = true, isRunning = false) }
        updateSessionRecord(isOpen = true)
    }

    fun resumeTimer() {
        startTimer()
    }

    fun completeTask() {
        timerJob?.cancel()
        val now = System.currentTimeMillis()
        updateSessionRecord(isOpen = false)

        val task = _uiState.value.task ?: return
        val points = 50 + if (task.isFrog) 50 else 0 + (task.impactScore / 2)

        viewModelScope.launch {
            taskRepository.completeAndRecur(task, now)
            _uiState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    isCompleted = true,
                    pointsEarned = points
                )
            }
        }
    }

    private fun updateSessionRecord(isOpen: Boolean) {
        val duration = _uiState.value.elapsedSeconds
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                sessionRepository.update(
                    session.copy(
                        endedAt = if (isOpen) null else now,
                        durationMinutes = duration / 60f,
                        pauseResumeCount = pauseCount
                    )
                )
            }
        }
    }

    fun toggleMicroStep(index: Int) {
        val currentSteps = _uiState.value.microSteps
        if (index !in currentSteps.indices) return

        val updated = currentSteps.toMutableList()
        val step = updated[index]
        updated[index] = step.copy(completed = !step.completed)

        val serialized = StepByStepPlanCodec.serialize(updated)
        _uiState.update { it.copy(microSteps = updated) }

        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
        }
    }

    fun addMicroStep(stepText: String) {
        if (stepText.isBlank()) return
        val currentSteps = _uiState.value.microSteps
        val updated = currentSteps + StepByStepPlanStep(stepText.trim(), false)
        val serialized = StepByStepPlanCodec.serialize(updated)

        _uiState.update { it.copy(microSteps = updated) }

        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
        }
    }

    fun parkIntrusiveThought(thoughtText: String) {
        if (thoughtText.isBlank()) return
        viewModelScope.launch {
            val newTask = TaskEntity(
                title = thoughtText.trim(),
                quadrant = Quadrant.DELEGATE,
                energyLevel = EnergyLevel.LOW,
                estimatedDurationMinutes = 10,
                impactScore = 30
            )
            taskRepository.insert(newTask)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        if (_uiState.value.isRunning && !_uiState.value.isCompleted) {
            updateSessionRecord(isOpen = false)
        }
    }
}
