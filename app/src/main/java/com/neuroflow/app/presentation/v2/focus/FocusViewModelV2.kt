package com.neuroflow.app.presentation.v2.focus

import android.content.Context
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
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.v2.focus.audio.NeuralSoundscapePlayer
import com.neuroflow.app.presentation.v2.focus.audio.SoundscapeType
import com.neuroflow.app.presentation.v2.focus.engine.FocusTimingCalculator
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.neuroflow.app.data.local.entity.startOfLocalDay
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

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
    val startedAtMillis: Long = 0L,
    val accumulatedPauseMillis: Long = 0L,
    val pausedAtMillis: Long? = null,
    val woopData: WoopEntity? = null,
    val microSteps: List<StepByStepPlanStep> = emptyList(),
    val isCompleted: Boolean = false,
    val pointsEarned: Int = 0,
    val nextTaskId: String? = null,
    val nextTaskTitle: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val showAffordanceRating: Boolean = false,
    val affordanceRatingSubmitted: Boolean = false,
    val selectedSoundscape: SoundscapeType = SoundscapeType.NONE,
    val soundscapeVolume: Float = 0.7f,
    val showDecompressionZone: Boolean = false,
    val decompressionRemainingSeconds: Int = 180,
    val isDecompressionSighActive: Boolean = false,
    val decompressionSighRemainingSeconds: Int = 0,
    val isDecompressionBypassed: Boolean = false,
    val skippedMicroStepIndices: Set<Int> = emptySet(),
    val showSprintCompletionSheet: Boolean = false,
    val isSprintCompleted: Boolean = false,
    val completionAffirmation: String? = null
) {
    val isSessionActive: Boolean get() = isRunning
}

@HiltViewModel
class FocusViewModelV2 @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val woopRepository: WoopRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    @ApplicationContext private val context: Context? = null,
    private val behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService(),
    private val focusCompletionNotifier: FocusCompletionNotifier? = null
) : ViewModel() {

    val taskId: String = checkNotNull(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(FocusUiStateV2())
    val uiState: StateFlow<FocusUiStateV2> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var pauseCount: Int = 0
    private var hasNotifiedCompletion = false
    private var autoTrackerJob: Job? = null
    private var autoTrackerTriggered = false

    val soundscapePlayer: NeuralSoundscapePlayer by lazy {
        NeuralSoundscapePlayer(context)
    }

    init {
        loadTask()
        observePreferences()
    }

    private fun loadTask() {
        context?.let { AutonomyNudgeEngine.cancelNudge(it, taskId) }
        viewModelScope.launch {
            val task = taskRepository.getById(taskId)
            val woop = woopRepository.getByTaskId(taskId)
            var steps = task?.let { StepByStepPlanCodec.parse(it.ifThenPlan) } ?: emptyList()
            if (steps.isEmpty() && task != null) {
                val dbSteps = taskRepository.getMicroSteps(task.id)
                if (dbSteps.isNotEmpty()) {
                    steps = dbSteps.map { StepByStepPlanStep(it.title, it.isCompleted) }
                }
            }

            // Find next task
            val activeTasks = taskRepository.getActiveTasks()
            val next = activeTasks.firstOrNull { it.id != taskId }

            val openSession = sessionRepository.getOpenSessionForTask(taskId)?.takeIf { it.id.isNotBlank() && it.startedAt > 0L }
            if (openSession != null) {
                val startedAt = openSession.startedAt
                val accPause = openSession.totalPausedMs
                val pausedAt = openSession.pausedAt
                val isPaused = pausedAt != null
                val isRunning = !isPaused
                pauseCount = openSession.pauseResumeCount

                val now = System.currentTimeMillis()
                val timing = FocusTimingCalculator.calculate(
                    startedAtMillis = startedAt,
                    accumulatedPauseMillis = accPause,
                    pausedAtMillis = pausedAt,
                    nowMillis = now,
                    targetDurationSeconds = _uiState.value.timerMode.durationSeconds
                )

                _uiState.update {
                    it.copy(
                        task = task,
                        woopData = woop,
                        microSteps = steps,
                        nextTaskId = next?.id,
                        nextTaskTitle = next?.title,
                        activeSessionId = openSession.id,
                        startedAtMillis = startedAt,
                        accumulatedPauseMillis = accPause,
                        pausedAtMillis = pausedAt,
                        isRunning = isRunning,
                        isPaused = isPaused,
                        elapsedSeconds = timing.elapsedSeconds,
                        remainingSeconds = timing.remainingSeconds,
                        isLoading = false
                    )
                }

                if (isRunning) {
                    startTicker()
                    FocusService.startService(
                        context = context,
                        taskId = taskId,
                        taskTitle = task?.title ?: "Deep Focus",
                        durationSeconds = _uiState.value.timerMode.durationSeconds,
                        startedAtMillis = startedAt
                    )
                }
            } else {
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
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesDataStore.preferencesFlow
                .collect { prefs ->
                    _uiState.update { it.copy(preferences = prefs) }
                    checkAutoTracker(prefs)
                }
        }
    }

    private fun checkAutoTracker(prefs: UserPreferences) {
        if (!prefs.autoTrackerEnabled || autoTrackerTriggered || _uiState.value.isRunning || _uiState.value.isPaused) return
        if (autoTrackerJob == null || autoTrackerJob?.isActive == false) {
            autoTrackerJob = viewModelScope.launch {
                delay(8000L)
                if (!_uiState.value.isRunning && !_uiState.value.isPaused && !autoTrackerTriggered) {
                    autoTrackerTriggered = true
                    startTimer()
                }
            }
        }
    }

    fun markWoopPromptShown() {
        viewModelScope.launch {
            val task = _uiState.value.task ?: return@launch
            if (!task.woopPromptShown) {
                val updated = task.copy(woopPromptShown = true, updatedAt = System.currentTimeMillis())
                taskRepository.update(updated)
                _uiState.update { it.copy(task = updated) }
            }
        }
    }

    fun selectTimerMode(mode: FocusTimerModeV2) {
        if (_uiState.value.isRunning || _uiState.value.isPaused) return
        autoTrackerJob?.cancel()
        autoTrackerJob = null
        hasNotifiedCompletion = false
        _uiState.update {
            it.copy(
                timerMode = mode,
                remainingSeconds = mode.durationSeconds,
                elapsedSeconds = 0L,
                startedAtMillis = 0L,
                accumulatedPauseMillis = 0L,
                pausedAtMillis = null,
                isSprintCompleted = false,
                showSprintCompletionSheet = false
            )
        }
    }

    fun selectSoundscape(type: SoundscapeType) {
        _uiState.update { it.copy(selectedSoundscape = type) }
        if (_uiState.value.isRunning && !_uiState.value.isPaused) {
            if (type == SoundscapeType.NONE) {
                soundscapePlayer.pauseWithFadeOut()
            } else {
                soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
                soundscapePlayer.play(type)
            }
        }
    }

    fun setSoundscapeVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(soundscapeVolume = clamped) }
        soundscapePlayer.setVolume(clamped)
    }

    fun onScreenExit() {
        autoTrackerJob?.cancel()
        autoTrackerJob = null
        if (!_uiState.value.isSessionActive && _uiState.value.selectedSoundscape != SoundscapeType.NONE) {
            soundscapePlayer.pauseWithFadeOut()
        }
    }

    fun saveWoop(wish: String, outcome: String, obstacle: String, plan: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = woopRepository.getByTaskId(taskId)
            val entity = existing?.copy(
                wish = wish,
                outcome = outcome,
                obstacle = obstacle,
                plan = plan,
                updatedAt = now
            ) ?: WoopEntity(
                taskId = taskId,
                wish = wish,
                outcome = outcome,
                obstacle = obstacle,
                plan = plan,
                createdAt = now,
                updatedAt = now
            )
            woopRepository.upsert(entity)
            _uiState.update { it.copy(woopData = entity) }

            val task = _uiState.value.task
            if (task != null && (task.ifThenPlan.isNullOrBlank() || !task.ifThenPlan!!.contains("["))) {
                taskRepository.update(task.copy(ifThenPlan = plan, updatedAt = now))
            }
        }
    }

    fun startTimer() {
        autoTrackerJob?.cancel()
        autoTrackerJob = null
        autoTrackerTriggered = true
        if (_uiState.value.isRunning && !_uiState.value.isPaused) return

        if (_uiState.value.isPaused) {
            resumeTimer()
            return
        }

        val now = System.currentTimeMillis()
        val isFreshOrFinished = _uiState.value.isSprintCompleted || _uiState.value.isCompleted || _uiState.value.activeSessionId == null
        val sessionId = if (isFreshOrFinished) UUID.randomUUID().toString() else (_uiState.value.activeSessionId ?: UUID.randomUUID().toString())
        hasNotifiedCompletion = false

        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                activeSessionId = sessionId,
                startedAtMillis = now,
                accumulatedPauseMillis = 0L,
                pausedAtMillis = null,
                elapsedSeconds = 0L,
                remainingSeconds = if (it.timerMode == FocusTimerModeV2.FLOW_STOPWATCH) 0L else it.timerMode.durationSeconds,
                isSprintCompleted = false,
                showSprintCompletionSheet = false
            )
        }

        viewModelScope.launch {
            val existingSession = sessionRepository.getOpenSessionForTask(taskId)
            if (existingSession == null) {
                sessionRepository.insert(
                    TimeSessionEntity(
                        id = sessionId,
                        taskId = taskId,
                        startedAt = now
                    )
                )
            }
        }

        enableDndIfPermitted()
        if (_uiState.value.selectedSoundscape != SoundscapeType.NONE) {
            soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
            soundscapePlayer.play(_uiState.value.selectedSoundscape)
        }

        FocusService.startService(
            context = context,
            taskId = taskId,
            taskTitle = _uiState.value.task?.title ?: "Deep Focus",
            durationSeconds = _uiState.value.timerMode.durationSeconds,
            startedAtMillis = now
        )

        startTicker()
    }

    private fun startTicker() {
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
        var countdownHitZero = false
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            if (!current.isRunning || current.isPaused) return@update current
            val timing = FocusTimingCalculator.calculate(
                startedAtMillis = current.startedAtMillis,
                accumulatedPauseMillis = current.accumulatedPauseMillis,
                pausedAtMillis = current.pausedAtMillis,
                nowMillis = now,
                targetDurationSeconds = current.timerMode.durationSeconds
            )
            val elapsed = maxOf(current.elapsedSeconds + 1, timing.elapsedSeconds)
            val isStopwatch = current.timerMode == FocusTimerModeV2.FLOW_STOPWATCH
            val remaining = if (isStopwatch) 0L else maxOf(0L, current.timerMode.durationSeconds - elapsed)
            val isExpired = (!isStopwatch) && (elapsed >= current.timerMode.durationSeconds)

            if (isExpired) {
                countdownHitZero = true
                current.copy(
                    isRunning = false,
                    isPaused = false,
                    elapsedSeconds = current.timerMode.durationSeconds,
                    remainingSeconds = 0L,
                    isSprintCompleted = true,
                    showSprintCompletionSheet = true
                )
            } else {
                current.copy(
                    elapsedSeconds = elapsed,
                    remainingSeconds = remaining
                )
            }
        }
        if (countdownHitZero) {
            timerJob?.cancel()
            timerJob = null
            disableDnd()
            soundscapePlayer.pauseWithFadeOut()
            FocusService.stopService(context)

            val durationSec = _uiState.value.elapsedSeconds
            val actualMinutes = max(1, (durationSec / 60).toInt())

            updateSessionRecord(isOpen = false)

            viewModelScope.launch {
                val currentTask = _uiState.value.task
                if (currentTask != null) {
                    val updatedTask = currentTask.copy(
                        totalTimeTrackedMinutes = currentTask.totalTimeTrackedMinutes + actualMinutes
                    )
                    taskRepository.update(updatedTask)
                }
            }

            if (!hasNotifiedCompletion) {
                hasNotifiedCompletion = true
                focusCompletionNotifier?.playCompletionChimeAndHaptics(viewModelScope)
            }
        }
    }

    fun pauseTimer() {
        if (!_uiState.value.isRunning) return
        val now = System.currentTimeMillis()
        val currentPausedAt = _uiState.value.pausedAtMillis
        val newPausedAt = FocusTimingCalculator.onPause(currentPausedAt, now)
        pauseCount++

        timerJob?.cancel()
        timerJob = null
        disableDnd()
        soundscapePlayer.pauseWithFadeOut()

        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = _uiState.value.startedAtMillis,
            accumulatedPauseMillis = _uiState.value.accumulatedPauseMillis,
            pausedAtMillis = newPausedAt,
            nowMillis = now,
            targetDurationSeconds = _uiState.value.timerMode.durationSeconds
        )

        val elapsed = maxOf(_uiState.value.elapsedSeconds, timing.elapsedSeconds)
        val isStopwatch = _uiState.value.timerMode == FocusTimerModeV2.FLOW_STOPWATCH
        val remaining = if (isStopwatch) 0L else maxOf(0L, _uiState.value.timerMode.durationSeconds - elapsed)

        _uiState.update {
            it.copy(
                isPaused = true,
                isRunning = false,
                pausedAtMillis = newPausedAt,
                elapsedSeconds = elapsed,
                remainingSeconds = remaining
            )
        }

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                sessionRepository.update(
                    session.copy(
                        pausedAt = newPausedAt,
                        totalPausedMs = _uiState.value.accumulatedPauseMillis,
                        durationMinutes = elapsed / 60f,
                        pauseResumeCount = pauseCount
                    )
                )
            }
        }

        FocusService.pauseService(context)
    }

    fun resumeTimer() {
        if (!_uiState.value.isPaused) {
            startTimer()
            return
        }

        val now = System.currentTimeMillis()
        val pausedAt = _uiState.value.pausedAtMillis
        val currentAcc = _uiState.value.accumulatedPauseMillis
        val newAcc = if (pausedAt != null) {
            val (resAcc, _) = FocusTimingCalculator.onResume(pausedAt, currentAcc, now)
            resAcc
        } else {
            currentAcc
        }

        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = _uiState.value.startedAtMillis,
            accumulatedPauseMillis = newAcc,
            pausedAtMillis = null,
            nowMillis = now,
            targetDurationSeconds = _uiState.value.timerMode.durationSeconds
        )

        val elapsed = maxOf(_uiState.value.elapsedSeconds, timing.elapsedSeconds)
        val isStopwatch = _uiState.value.timerMode == FocusTimerModeV2.FLOW_STOPWATCH
        val remaining = if (isStopwatch) 0L else maxOf(0L, _uiState.value.timerMode.durationSeconds - elapsed)

        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                pausedAtMillis = null,
                accumulatedPauseMillis = newAcc,
                elapsedSeconds = elapsed,
                remainingSeconds = remaining
            )
        }

        enableDndIfPermitted()
        if (_uiState.value.selectedSoundscape != SoundscapeType.NONE) {
            soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
            soundscapePlayer.play(_uiState.value.selectedSoundscape)
        }

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                sessionRepository.update(
                    session.copy(
                        pausedAt = null,
                        totalPausedMs = newAcc,
                        durationMinutes = timing.elapsedSeconds / 60f
                    )
                )
            }
        }

        FocusService.resumeService(context)
        startTicker()
    }

    fun completeTask() {
        timerJob?.cancel()
        timerJob = null
        disableDnd()
        soundscapePlayer.pauseWithFadeOut()
        FocusService.stopService(context)
        context?.let { AutonomyNudgeEngine.cancelNudge(it, taskId) }

        val now = System.currentTimeMillis()
        val durationSec = _uiState.value.elapsedSeconds
        val actualMinutes = max(1, (durationSec / 60).toInt())

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                sessionRepository.update(
                    session.copy(
                        endedAt = now,
                        pausedAt = null,
                        totalPausedMs = _uiState.value.accumulatedPauseMillis,
                        durationMinutes = durationSec / 60f,
                        pauseResumeCount = pauseCount
                    )
                )
            }

            val currentTask = _uiState.value.task ?: return@launch
            val finalTracked = currentTask.totalTimeTrackedMinutes + actualMinutes
            val est = currentTask.estimatedDurationMinutes.toFloat()
            val mape = if (est > 0f) Math.abs(finalTracked - est) / est else null
            val smape = if (est > 0f || finalTracked > 0f) (2f * Math.abs(finalTracked - est)) / (est + finalTracked) else null

            val points = 50 + (if (currentTask.isFrog) 50 else 0) + (currentTask.impactScore / 2)

            val updatedTask = currentTask.copy(
                totalTimeTrackedMinutes = finalTracked,
                actualDurationMinutes = finalTracked,
                sessionCount = currentTask.sessionCount + 1,
                lastSessionDurationMinutes = actualMinutes.toFloat(),
                estimationErrorMape = mape,
                estimationErrorSmape = smape,
                focusModePoints = points,
                status = TaskStatus.COMPLETED,
                completedAt = now,
                updatedAt = now
            )
            taskRepository.update(updatedTask)
            taskRepository.completeAndRecur(updatedTask, now)

            preferencesDataStore.updatePreferences { prefs ->
                val newFocusMinutes = prefs.totalFocusMinutes + actualMinutes
                val newCompleted = prefs.totalTasksCompleted + 1
                val lastActive = prefs.lastActiveDate
                val todayStart = startOfLocalDay(now)
                val isNewDay = lastActive < todayStart
                val isConsecutive = lastActive >= todayStart - 86_400_000L && isNewDay
                val newStreak = when {
                    isConsecutive -> prefs.dailyStreak + 1
                    isNewDay -> 1
                    else -> prefs.dailyStreak
                }
                val longest = maxOf(prefs.longestStreak, newStreak)
                prefs.copy(
                    totalFocusMinutes = newFocusMinutes,
                    totalTasksCompleted = newCompleted,
                    dailyStreak = newStreak,
                    longestStreak = longest,
                    lastActiveDate = now
                )
            }

            val prefs = preferencesDataStore.preferencesFlow.first()
            val affirmation = if (prefs.affirmations.isNotEmpty()) prefs.affirmations.random() else null

            _uiState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    isCompleted = true,
                    showAffordanceRating = true,
                    pointsEarned = points,
                    showDecompressionZone = true,
                    decompressionRemainingSeconds = 180,
                    isDecompressionSighActive = false,
                    decompressionSighRemainingSeconds = 0,
                    isDecompressionBypassed = false,
                    completionAffirmation = affirmation
                )
            }
        }
    }

    fun submitAffordanceRating(rating: Float) {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            taskRepository.update(
                task.copy(
                    affectiveForecastError = rating,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(showAffordanceRating = false, affordanceRatingSubmitted = true) }
        }
    }

    fun dismissAffordanceRating() {
        _uiState.update { it.copy(showAffordanceRating = false) }
    }

    fun keepGoing(nextMode: FocusTimerModeV2 = FocusTimerModeV2.POMODORO_25) {
        _uiState.update {
            it.copy(
                timerMode = nextMode,
                showSprintCompletionSheet = false,
                isSprintCompleted = false
            )
        }
        startTimer()
    }

    fun takeDecompressionRest() {
        _uiState.update {
            it.copy(
                showSprintCompletionSheet = false,
                isSprintCompleted = false,
                showDecompressionZone = true,
                decompressionRemainingSeconds = 180,
                isDecompressionSighActive = false,
                decompressionSighRemainingSeconds = 0,
                isDecompressionBypassed = false
            )
        }
        startDecompressionTicker()
    }

    fun dismissSprintCompletionSheet() {
        _uiState.update { it.copy(showSprintCompletionSheet = false) }
    }

    private var decompressionJob: Job? = null

    fun startDecompressionTicker() {
        decompressionJob?.cancel()
        decompressionJob = viewModelScope.launch {
            while (isActive && _uiState.value.showDecompressionZone && _uiState.value.decompressionRemainingSeconds > 0) {
                delay(1000)
                if (!isActive || !_uiState.value.showDecompressionZone) break
                tickDecompressionSecond()
            }
        }
    }

    fun tickDecompressionSecond() {
        _uiState.update { current ->
            if (!current.showDecompressionZone) return@update current
            val newRemaining = maxOf(0, current.decompressionRemainingSeconds - 1)
            val newSighRemaining = if (current.isDecompressionSighActive) maxOf(0, current.decompressionSighRemainingSeconds - 1) else 0
            val newSighActive = current.isDecompressionSighActive && newSighRemaining > 0
            current.copy(
                decompressionRemainingSeconds = newRemaining,
                isDecompressionSighActive = newSighActive,
                decompressionSighRemainingSeconds = newSighRemaining
            )
        }
    }

    fun triggerDecompressionSigh() {
        _uiState.update {
            it.copy(
                isDecompressionSighActive = true,
                decompressionSighRemainingSeconds = 60
            )
        }
    }

    fun readyNowBypassDecompression() {
        decompressionJob?.cancel()
        decompressionJob = null
        _uiState.update {
            it.copy(
                decompressionRemainingSeconds = 0,
                isDecompressionSighActive = false,
                decompressionSighRemainingSeconds = 0,
                isDecompressionBypassed = true,
                showDecompressionZone = false
            )
        }
    }

    fun dismissDecompressionZone() {
        decompressionJob?.cancel()
        decompressionJob = null
        _uiState.update { it.copy(showDecompressionZone = false) }
    }

    fun autoBreakDownTask() {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            val generated = behavioralSupportService.generateMicroStepsWithAi(
                taskTitle = task.title,
                estimatedMinutes = task.estimatedDurationMinutes,
                apiKey = prefs.geminiApiKey,
                useAi = prefs.geminiAiEnabled
            )
            val microStepEntities = generated.mapIndexed { index, suggestion ->
                TaskMicroStepEntity(
                    taskId = task.id,
                    title = suggestion.title,
                    sortOrder = index,
                    estimatedMinutes = suggestion.estimatedMinutes
                )
            }
            val planSteps = generated.map { StepByStepPlanStep(it.title, false) }
            val serialized = StepByStepPlanCodec.serialize(planSteps)
            _uiState.update { it.copy(microSteps = planSteps) }
            taskRepository.deleteMicroStepsByTaskId(task.id)
            taskRepository.insertMicroSteps(microStepEntities)
            taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
        }
    }

    private fun enableDndIfPermitted() {
        try {
            if (!_uiState.value.preferences.dndEnabled || context == null) return
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            if (nm != null && nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        } catch (_: Exception) {}
    }

    private fun disableDnd() {
        try {
            if (context == null) return
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            if (nm != null && nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (_: Exception) {}
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
                        pausedAt = _uiState.value.pausedAtMillis,
                        totalPausedMs = _uiState.value.accumulatedPauseMillis,
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
        _uiState.update { it.copy(microSteps = updated, skippedMicroStepIndices = it.skippedMicroStepIndices - index) }

        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            val roomSteps = taskRepository.getMicroSteps(task.id)
            if (index in roomSteps.indices) {
                taskRepository.toggleMicroStepCompleted(roomSteps[index])
            }
            taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
        }
    }

    fun skipCurrentMicroStep(index: Int) {
        _uiState.update { current ->
            val nextSkipped = current.skippedMicroStepIndices + index
            val remainingIncomplete = current.microSteps.indices.filter { !current.microSteps[it].completed }
            if (remainingIncomplete.all { it in nextSkipped }) {
                current.copy(skippedMicroStepIndices = setOf(index))
            } else {
                current.copy(skippedMicroStepIndices = nextSkipped)
            }
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
            val roomSteps = taskRepository.getMicroSteps(task.id)
            val nextOrder = (roomSteps.maxOfOrNull { it.sortOrder } ?: -1) + 1
            taskRepository.insertMicroStep(
                TaskMicroStepEntity(
                    taskId = task.id,
                    title = stepText.trim(),
                    sortOrder = nextOrder,
                    isCompleted = false,
                    estimatedMinutes = 5
                )
            )
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
        decompressionJob?.cancel()
        disableDnd()
        if (!_uiState.value.isRunning || _uiState.value.isCompleted) {
            soundscapePlayer.release()
            FocusService.stopService(context)
        } else {
            soundscapePlayer.pauseWithFadeOut()
        }
    }
}
