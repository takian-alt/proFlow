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
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
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
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.local.entity.startOfNextLocalDay
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.dependencyIds
import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
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
    val showCompletionCelebration: Boolean = false,
    val pointsEarned: Int = 0,
    val nextTaskId: String? = null,
    val nextTaskTitle: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val showAffordanceRating: Boolean = false,
    val affordanceRatingSubmitted: Boolean = false,
    val selectedSoundscape: SoundscapeType = SoundscapeType.DEFAULT,
    val soundscapeVolume: Float = 0.7f,
    val showDecompressionZone: Boolean = false,
    val decompressionRemainingSeconds: Int = 180,
    val isDecompressionSighActive: Boolean = false,
    val decompressionSighRemainingSeconds: Int = 0,
    val isDecompressionBypassed: Boolean = false,
    val skippedMicroStepIndices: Set<Int> = emptySet(),
    val showSprintCompletionSheet: Boolean = false,
    val isSprintCompleted: Boolean = false,
    val completionAffirmation: String? = null,
    val showDndNotice: Boolean = false,
    val isUltradianLimitExceeded: Boolean = elapsedSeconds >= ULTRADIAN_LIMIT_SECONDS,
    val ultradianWarning: String? = if (isUltradianLimitExceeded) ULTRADIAN_WARNING_MESSAGE else null
) {
    val isSessionActive: Boolean get() = isRunning || isPaused

    companion object {
        const val ULTRADIAN_LIMIT_SECONDS = 5400L // 90 minutes
        const val ULTRADIAN_WARNING_MESSAGE =
            "⚠️ 90m ultradian limit reached. Neural efficiency is declining — consider wrapping up or taking a break."
    }
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

    private val _uiState = MutableStateFlow(FocusUiStateV2(selectedSoundscape = activeSoundscape))
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
        observeActiveSession()
    }

    private fun loadTask() {
        context?.let { AutonomyNudgeEngine.cancelNudge(it, taskId) }
        viewModelScope.launch {
            val isUntracked = taskId == TaskEntity.TASK_UNTRACKED_FOCUS
            var task = taskRepository.getById(taskId)
            if (task == null && isUntracked) {
                task = taskRepository.ensureUntrackedFocusTask()
            }
            val woop = if (isUntracked) null else woopRepository.getByTaskId(taskId)
            var steps = if (isUntracked) emptyList() else task?.let { StepByStepPlanCodec.parse(it.ifThenPlan) } ?: emptyList()
            if (!isUntracked && steps.isEmpty() && task != null) {
                val dbSteps = taskRepository.getMicroSteps(task.id)
                if (dbSteps.isNotEmpty()) {
                    steps = dbSteps.map { StepByStepPlanStep(it.title, it.isCompleted) }
                }
            }

            // Find next task (prioritizing frogs and actionable non-icebox tasks)
            val activeTasks = taskRepository.getActiveTasks().filterNot { it.id == TaskEntity.TASK_UNTRACKED_FOCUS }
            val next = findNextActionableTask(activeTasks, taskId)

            val openSession = sessionRepository.getOpenSessionForTask(taskId)?.takeIf { it.id.isNotBlank() && it.startedAt > 0L }
            if (openSession != null) {
                val startedAt = openSession.startedAt
                val accPause = openSession.totalPausedMs
                val pausedAt = openSession.pausedAt
                val isPaused = pausedAt != null
                pauseCount = openSession.pauseResumeCount

                val recoveredMode = FocusTimerModeV2.entries.firstOrNull { it.name == openSession.sessionType }
                    ?: _uiState.value.timerMode

                val now = System.currentTimeMillis()
                val timing = FocusTimingCalculator.calculate(
                    startedAtMillis = startedAt,
                    accumulatedPauseMillis = accPause,
                    pausedAtMillis = pausedAt,
                    nowMillis = now,
                    targetDurationSeconds = recoveredMode.durationSeconds
                )

                val isExpired = timing.isExpired && recoveredMode != FocusTimerModeV2.FLOW_STOPWATCH
                val isRunning = !isPaused && !isExpired

                if (isExpired) {
                    val durationSec = recoveredMode.durationSeconds
                    val actualMinutes = max(1, (durationSec / 60).toInt())
                    val calculatedEndAt = minOf(now, startedAt + (durationSec * 1000L) + accPause)

                    _uiState.update {
                        it.copy(
                            task = task,
                            timerMode = recoveredMode,
                            woopData = woop,
                            microSteps = steps,
                            nextTaskId = next?.id,
                            nextTaskTitle = next?.title,
                            activeSessionId = openSession.id,
                            startedAtMillis = startedAt,
                            accumulatedPauseMillis = accPause,
                            pausedAtMillis = null,
                            isRunning = false,
                            isPaused = false,
                            elapsedSeconds = durationSec,
                            remainingSeconds = 0L,
                            isSprintCompleted = true,
                            showSprintCompletionSheet = true,
                            isLoading = false
                        )
                    }

                    viewModelScope.launch {
                        sessionRepository.update(
                            openSession.copy(
                                endedAt = calculatedEndAt,
                                pausedAt = null,
                                totalPausedMs = accPause,
                                durationMinutes = durationSec / 60f,
                                pauseResumeCount = pauseCount
                            )
                        )
                    }
                    disableDnd()
                    soundscapePlayer.pauseWithFadeOut()
                    FocusService.stopService(context)

                    viewModelScope.launch {
                        val currentTask = _uiState.value.task
                        if (currentTask != null) {
                            val updatedTask = currentTask.copy(
                                totalTimeTrackedMinutes = currentTask.totalTimeTrackedMinutes + actualMinutes,
                                actualDurationMinutes = currentTask.totalTimeTrackedMinutes + actualMinutes,
                                sessionCount = currentTask.sessionCount + 1,
                                lastSessionDurationMinutes = actualMinutes.toFloat()
                            )
                            taskRepository.update(updatedTask)
                            _uiState.update { it.copy(task = updatedTask) }
                        }

                        val nowTs = System.currentTimeMillis()
                        preferencesDataStore.updatePreferences { prefs ->
                            val newFocusMinutes = prefs.totalFocusMinutes + actualMinutes
                            val lastActive = prefs.lastActiveDate
                            val todayStart = startOfLocalDay(nowTs)
                            val isNewDay = lastActive < todayStart
                            val isConsecutive = lastActive >= startOfPreviousLocalDay(todayStart) && isNewDay
                            val newStreak = when {
                                isConsecutive -> prefs.dailyStreak + 1
                                isNewDay -> 1
                                else -> prefs.dailyStreak
                            }
                            val longest = maxOf(prefs.longestStreak, newStreak)
                            prefs.copy(
                                totalFocusMinutes = newFocusMinutes,
                                dailyStreak = newStreak,
                                longestStreak = longest,
                                lastActiveDate = nowTs
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            task = task,
                            timerMode = recoveredMode,
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
                            isUltradianLimitExceeded = timing.elapsedSeconds >= FocusUiStateV2.ULTRADIAN_LIMIT_SECONDS,
                            ultradianWarning = if (timing.elapsedSeconds >= FocusUiStateV2.ULTRADIAN_LIMIT_SECONDS) FocusUiStateV2.ULTRADIAN_WARNING_MESSAGE else null,
                            isLoading = false
                        )
                    }

                    if (isRunning) {
                        startTicker()
                        FocusService.startService(
                            context = context,
                            taskId = taskId,
                            taskTitle = task?.title ?: "Deep Focus",
                            durationSeconds = recoveredMode.durationSeconds,
                            startedAtMillis = startedAt,
                            accumulatedPauseMillis = accPause,
                            pausedAtMillis = pausedAt,
                            isPaused = false
                        )
                        if (_uiState.value.selectedSoundscape != SoundscapeType.NONE) {
                            soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
                            soundscapePlayer.play(_uiState.value.selectedSoundscape)
                        }
                    } else if (isPaused) {
                        FocusService.startService(
                            context = context,
                            taskId = taskId,
                            taskTitle = task?.title ?: "Deep Focus",
                            durationSeconds = recoveredMode.durationSeconds,
                            startedAtMillis = startedAt,
                            accumulatedPauseMillis = accPause,
                            pausedAtMillis = pausedAt,
                            isPaused = true
                        )
                    }
                }
            } else {
                val isCompleted = if (isUntracked) false else task?.status == TaskStatus.COMPLETED
                _uiState.update {
                    it.copy(
                        task = task,
                        woopData = woop,
                        microSteps = steps,
                        nextTaskId = next?.id,
                        nextTaskTitle = next?.title,
                        isCompleted = isCompleted,
                        isLoading = false
                    )
                }
            }
            checkAutoTracker(_uiState.value.preferences)
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesDataStore.preferencesFlow
                .collect { prefs ->
                    _uiState.update { it.copy(preferences = prefs) }
                    checkDndNotice(prefs)
                    if (!prefs.dndEnabled) {
                        disableDnd()
                    }
                    checkAutoTracker(prefs)
                }
        }
    }

    private fun checkAutoTracker(prefs: UserPreferences) {
        if (_uiState.value.isLoading || _uiState.value.task == null) return
        if (!prefs.autoTrackerEnabled || autoTrackerTriggered || _uiState.value.isRunning || _uiState.value.isPaused ||
            _uiState.value.isCompleted || _uiState.value.isSprintCompleted || _uiState.value.showSprintCompletionSheet ||
            _uiState.value.task?.status == TaskStatus.COMPLETED) return
        if (autoTrackerJob == null || autoTrackerJob?.isActive == false) {
            autoTrackerJob = viewModelScope.launch {
                delay(8000L)
                if (!_uiState.value.isRunning && !_uiState.value.isPaused && !autoTrackerTriggered &&
                    !_uiState.value.isCompleted && !_uiState.value.isSprintCompleted && !_uiState.value.showSprintCompletionSheet &&
                    _uiState.value.task?.status != TaskStatus.COMPLETED) {
                    autoTrackerTriggered = true
                    startTimer()
                }
            }
        }
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            FocusService.activeSession.collect { serviceSession ->
                if (serviceSession == null) {
                    if (!_uiState.value.isCompleted) {
                        val task = taskRepository.getById(taskId)
                        if (task?.status == TaskStatus.COMPLETED) {
                            val freshActiveTasks = taskRepository.getActiveTasks().filter { it.id != taskId }
                            val freshNext = findNextActionableTask(freshActiveTasks, taskId)
                            _uiState.update {
                                it.copy(
                                    isRunning = false,
                                    isPaused = false,
                                    isCompleted = true,
                                    nextTaskId = freshNext?.id,
                                    nextTaskTitle = freshNext?.title,
                                    showSprintCompletionSheet = false,
                                    showCompletionCelebration = true,
                                    pointsEarned = task.focusModePoints,
                                    task = task
                                )
                            }
                            timerJob?.cancel()
                            timerJob = null
                            disableDnd()
                            soundscapePlayer.pauseWithFadeOut()
                        } else if (_uiState.value.isRunning) {
                            val now = System.currentTimeMillis()
                            val timing = FocusTimingCalculator.calculate(
                                startedAtMillis = _uiState.value.startedAtMillis,
                                accumulatedPauseMillis = _uiState.value.accumulatedPauseMillis,
                                pausedAtMillis = _uiState.value.pausedAtMillis,
                                nowMillis = now,
                                targetDurationSeconds = _uiState.value.timerMode.durationSeconds
                            )
                            if (timing.isExpired && _uiState.value.timerMode != FocusTimerModeV2.FLOW_STOPWATCH) {
                                timerJob?.cancel()
                                timerJob = null
                                disableDnd()
                                soundscapePlayer.pauseWithFadeOut()
                                _uiState.update {
                                    it.copy(
                                        isRunning = false,
                                        isPaused = false,
                                        elapsedSeconds = it.timerMode.durationSeconds,
                                        remainingSeconds = 0L,
                                        isSprintCompleted = true,
                                        showSprintCompletionSheet = true
                                    )
                                }
                            }
                        }
                    }
                } else if (serviceSession.taskId == taskId) {
                    val isStopwatch = _uiState.value.timerMode == FocusTimerModeV2.FLOW_STOPWATCH
                    if (!isStopwatch && serviceSession.remainingSeconds <= 0L && _uiState.value.isRunning) {
                        timerJob?.cancel()
                        timerJob = null
                        disableDnd()
                        soundscapePlayer.pauseWithFadeOut()
                        _uiState.update {
                            it.copy(
                                isRunning = false,
                                isPaused = false,
                                elapsedSeconds = it.timerMode.durationSeconds,
                                remainingSeconds = 0L,
                                isSprintCompleted = true,
                                showSprintCompletionSheet = true
                            )
                        }
                    } else if (serviceSession.isPaused && _uiState.value.isRunning && !_uiState.value.isPaused) {
                        val now = System.currentTimeMillis()
                        val timing = FocusTimingCalculator.calculate(
                            startedAtMillis = _uiState.value.startedAtMillis,
                            accumulatedPauseMillis = _uiState.value.accumulatedPauseMillis,
                            pausedAtMillis = now,
                            nowMillis = now,
                            targetDurationSeconds = _uiState.value.timerMode.durationSeconds
                        )
                        timerJob?.cancel()
                        timerJob = null
                        soundscapePlayer.pauseWithFadeOut()
                        val isUltradian = timing.elapsedSeconds >= FocusUiStateV2.ULTRADIAN_LIMIT_SECONDS
                        _uiState.update {
                            it.copy(
                                isRunning = false,
                                isPaused = true,
                                pausedAtMillis = now,
                                elapsedSeconds = timing.elapsedSeconds,
                                remainingSeconds = if (it.timerMode == FocusTimerModeV2.FLOW_STOPWATCH) 0L else timing.remainingSeconds,
                                isUltradianLimitExceeded = isUltradian,
                                ultradianWarning = if (isUltradian) FocusUiStateV2.ULTRADIAN_WARNING_MESSAGE else null
                            )
                        }
                    } else if (!serviceSession.isPaused && _uiState.value.isPaused) {
                        val now = System.currentTimeMillis()
                        val pausedAt = _uiState.value.pausedAtMillis ?: now
                        val (newAcc, _) = FocusTimingCalculator.onResume(pausedAt, _uiState.value.accumulatedPauseMillis, now)
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

                        val isUltradian = elapsed >= FocusUiStateV2.ULTRADIAN_LIMIT_SECONDS
                        _uiState.update {
                            it.copy(
                                isRunning = true,
                                isPaused = false,
                                pausedAtMillis = null,
                                accumulatedPauseMillis = newAcc,
                                elapsedSeconds = elapsed,
                                remainingSeconds = remaining,
                                isUltradianLimitExceeded = isUltradian,
                                ultradianWarning = if (isUltradian) FocusUiStateV2.ULTRADIAN_WARNING_MESSAGE else null
                            )
                        }
                        if (_uiState.value.selectedSoundscape != SoundscapeType.NONE) {
                            soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
                            soundscapePlayer.play(_uiState.value.selectedSoundscape)
                        }
                        startTicker()
                    }
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
                showSprintCompletionSheet = false,
                isUltradianLimitExceeded = false,
                ultradianWarning = null
            )
        }
    }

    fun selectSoundscape(type: SoundscapeType) {
        activeSoundscape = type
        _uiState.update { it.copy(selectedSoundscape = type) }
        if (type == SoundscapeType.NONE) {
            soundscapePlayer.play(SoundscapeType.NONE)
        } else if (_uiState.value.isRunning && !_uiState.value.isPaused) {
            soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
            soundscapePlayer.play(type)
        } else if (soundscapePlayer.isAudioPlaying) {
            // Audio was actively sounding (e.g. lingering from timer expiration frame); switch immediately
            soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
            soundscapePlayer.play(type)
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
        if (!_uiState.value.isSessionActive || _uiState.value.isCompleted) {
            disableDnd()
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

            val task = _uiState.value.task
            val updatedTask = if (task != null && (task.ifThenPlan.isNullOrBlank() || !task.ifThenPlan!!.contains("["))) {
                val t = task.copy(ifThenPlan = plan, updatedAt = now)
                taskRepository.update(t)
                t
            } else task
            _uiState.update { it.copy(woopData = entity, task = updatedTask) }
        }
    }

    fun startTimer() {
        autoTrackerJob?.cancel()
        autoTrackerJob = null
        autoTrackerTriggered = true
        if (_uiState.value.isCompleted || _uiState.value.task?.status == TaskStatus.COMPLETED) return
        if (_uiState.value.isRunning && !_uiState.value.isPaused) return

        if (_uiState.value.isPaused) {
            resumeTimer()
            return
        }

        val now = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()
        hasNotifiedCompletion = false
        pauseCount = 0

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
                showSprintCompletionSheet = false,
                isUltradianLimitExceeded = false,
                ultradianWarning = null
            )
        }

        viewModelScope.launch {
            val existingSession = sessionRepository.getOpenSessionForTask(taskId)
            if (existingSession == null) {
                sessionRepository.insert(
                    TimeSessionEntity(
                        id = sessionId,
                        taskId = taskId,
                        startedAt = now,
                        sessionType = _uiState.value.timerMode.name
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
                    showSprintCompletionSheet = true,
                    isUltradianLimitExceeded = current.timerMode.durationSeconds >= FocusUiStateV2.ULTRADIAN_LIMIT_SECONDS,
                    ultradianWarning = if (current.timerMode.durationSeconds >= FocusUiStateV2.ULTRADIAN_LIMIT_SECONDS) FocusUiStateV2.ULTRADIAN_WARNING_MESSAGE else null
                )
            } else {
                val isUltradian = elapsed >= FocusUiStateV2.ULTRADIAN_LIMIT_SECONDS
                current.copy(
                    elapsedSeconds = elapsed,
                    remainingSeconds = remaining,
                    isUltradianLimitExceeded = isUltradian,
                    ultradianWarning = if (isUltradian) FocusUiStateV2.ULTRADIAN_WARNING_MESSAGE else null
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
                        totalTimeTrackedMinutes = currentTask.totalTimeTrackedMinutes + actualMinutes,
                        actualDurationMinutes = currentTask.totalTimeTrackedMinutes + actualMinutes,
                        sessionCount = currentTask.sessionCount + 1,
                        lastSessionDurationMinutes = actualMinutes.toFloat()
                    )
                    taskRepository.update(updatedTask)
                    _uiState.update { it.copy(task = updatedTask) }
                }

                val now = System.currentTimeMillis()
                preferencesDataStore.updatePreferences { prefs ->
                    val newFocusMinutes = prefs.totalFocusMinutes + actualMinutes
                    val lastActive = prefs.lastActiveDate
                    val todayStart = startOfLocalDay(now)
                    val isNewDay = lastActive < todayStart
                    val isConsecutive = lastActive >= startOfPreviousLocalDay(todayStart) && isNewDay
                    val newStreak = when {
                        isConsecutive -> prefs.dailyStreak + 1
                        isNewDay -> 1
                        else -> prefs.dailyStreak
                    }
                    val longest = maxOf(prefs.longestStreak, newStreak)
                    prefs.copy(
                        totalFocusMinutes = newFocusMinutes,
                        dailyStreak = newStreak,
                        longestStreak = longest,
                        lastActiveDate = now
                    )
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

        val isUltradian = elapsed >= FocusUiStateV2.ULTRADIAN_LIMIT_SECONDS
        _uiState.update {
            it.copy(
                isPaused = true,
                isRunning = false,
                pausedAtMillis = newPausedAt,
                elapsedSeconds = elapsed,
                remainingSeconds = remaining,
                isUltradianLimitExceeded = isUltradian,
                ultradianWarning = if (isUltradian) FocusUiStateV2.ULTRADIAN_WARNING_MESSAGE else null
            )
        }

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                pauseCount = maxOf(pauseCount, session.pauseResumeCount)
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

        val isUltradian = elapsed >= FocusUiStateV2.ULTRADIAN_LIMIT_SECONDS
        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                pausedAtMillis = null,
                accumulatedPauseMillis = newAcc,
                elapsedSeconds = elapsed,
                remainingSeconds = remaining,
                isUltradianLimitExceeded = isUltradian,
                ultradianWarning = if (isUltradian) FocusUiStateV2.ULTRADIAN_WARNING_MESSAGE else null
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
        if (_uiState.value.isCompleted || _uiState.value.task?.status == TaskStatus.COMPLETED) return
        timerJob?.cancel()
        timerJob = null
        disableDnd()
        soundscapePlayer.pauseWithFadeOut()
        FocusService.stopService(context)
        context?.let { ctx ->
            runCatching { AutonomyNudgeEngine.cancelNudge(ctx, taskId) }
            runCatching { com.neuroflow.app.worker.TaskReminderWorker.cancelReminder(ctx, taskId) }
            runCatching {
                val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
                nm?.cancel(taskId.hashCode())
            }
        }

        val now = System.currentTimeMillis()
        val wasSprintCompleted = _uiState.value.isSprintCompleted
        val hasRunTimer = _uiState.value.elapsedSeconds > 0L || _uiState.value.startedAtMillis > 0L

        val timing = if (hasRunTimer && !wasSprintCompleted) {
            FocusTimingCalculator.calculate(
                startedAtMillis = _uiState.value.startedAtMillis,
                accumulatedPauseMillis = _uiState.value.accumulatedPauseMillis,
                pausedAtMillis = _uiState.value.pausedAtMillis,
                nowMillis = now,
                targetDurationSeconds = _uiState.value.timerMode.durationSeconds
            )
        } else null

        val durationSec = when {
            wasSprintCompleted -> _uiState.value.elapsedSeconds
            timing != null -> {
                val calcElapsed = maxOf(_uiState.value.elapsedSeconds, timing.elapsedSeconds)
                if (_uiState.value.timerMode != FocusTimerModeV2.FLOW_STOPWATCH && _uiState.value.timerMode.durationSeconds > 0L) {
                    minOf(calcElapsed, _uiState.value.timerMode.durationSeconds)
                } else {
                    minOf(calcElapsed, (TimeSessionEntity.LIVE_SESSION_CEILING_MINUTES * 60f).toLong())
                }
            }
            else -> _uiState.value.elapsedSeconds
        }
        val actualMinutes = if (hasRunTimer) max(1, (durationSec / 60).toInt()) else 0

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            val currentPause = if (_uiState.value.isPaused && _uiState.value.pausedAtMillis != null) {
                maxOf(0L, now - _uiState.value.pausedAtMillis!!)
            } else 0L
            val totalPause = _uiState.value.accumulatedPauseMillis + currentPause

            if (session != null) {
                pauseCount = maxOf(pauseCount, session.pauseResumeCount)
                sessionRepository.update(
                    session.copy(
                        endedAt = now,
                        pausedAt = null,
                        totalPausedMs = totalPause,
                        durationMinutes = durationSec / 60f,
                        pauseResumeCount = pauseCount
                    )
                )
            }

            val currentTask = _uiState.value.task ?: return@launch
            val finalTracked = if (wasSprintCompleted) {
                currentTask.totalTimeTrackedMinutes
            } else {
                currentTask.totalTimeTrackedMinutes + actualMinutes
            }
            val est = TaskBlockPlan.plannedMinutes(currentTask).toFloat()
            val mape = if (est > 0f) Math.abs(finalTracked - est) / est else null
            val smape = if (est > 0f || finalTracked > 0f) (2f * Math.abs(finalTracked - est)) / (est + finalTracked) else null

            val points = 50 + (if (currentTask.isFrog) 50 else 0) + (currentTask.impactScore / 2)

            val hasUnfinishedSession = hasRunTimer && !wasSprintCompleted
            val finalSessionCount = if (hasUnfinishedSession) currentTask.sessionCount + 1 else currentTask.sessionCount
            val finalLastSessionDuration = if (hasUnfinishedSession) actualMinutes.toFloat() else currentTask.lastSessionDurationMinutes

            val taskToComplete = currentTask.copy(
                totalTimeTrackedMinutes = finalTracked,
                actualDurationMinutes = finalTracked,
                sessionCount = finalSessionCount,
                lastSessionDurationMinutes = finalLastSessionDuration,
                estimationErrorMape = mape,
                estimationErrorSmape = smape,
                focusModePoints = points
            )
            val isUntracked = taskId == TaskEntity.TASK_UNTRACKED_FOCUS
            val updatedTask = if (isUntracked) {
                val updated = taskToComplete.copy(
                    status = TaskStatus.ACTIVE,
                    completedAt = null,
                    updatedAt = now
                )
                taskRepository.update(updated)
                updated
            } else {
                taskRepository.completeAndRecur(taskToComplete, now)
                taskToComplete.copy(
                    status = TaskStatus.COMPLETED,
                    completedAt = now,
                    updatedAt = now
                )
            }

            if (isUntracked) {
                if (hasRunTimer && !wasSprintCompleted) {
                    preferencesDataStore.updatePreferences { prefs ->
                        val newFocusMinutes = prefs.totalFocusMinutes + actualMinutes
                        val lastActive = prefs.lastActiveDate
                        val todayStart = startOfLocalDay(now)
                        val isNewDay = lastActive < todayStart
                        val isConsecutive = lastActive >= startOfPreviousLocalDay(todayStart) && isNewDay
                        val newStreak = when {
                            isConsecutive -> prefs.dailyStreak + 1
                            isNewDay -> 1
                            else -> prefs.dailyStreak
                        }
                        val longest = maxOf(prefs.longestStreak, newStreak)
                        prefs.copy(
                            totalFocusMinutes = newFocusMinutes,
                            dailyStreak = newStreak,
                            longestStreak = longest,
                            lastActiveDate = now
                        )
                    }
                }
            } else if (hasRunTimer && !wasSprintCompleted) {
                preferencesDataStore.updatePreferences { prefs ->
                    val newFocusMinutes = prefs.totalFocusMinutes + actualMinutes
                    val newCompleted = prefs.totalTasksCompleted + 1
                    val lastActive = prefs.lastActiveDate
                    val todayStart = startOfLocalDay(now)
                    val isNewDay = lastActive < todayStart
                    val isConsecutive = lastActive >= startOfPreviousLocalDay(todayStart) && isNewDay
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
            } else {
                preferencesDataStore.updatePreferences { prefs ->
                    val newCompleted = prefs.totalTasksCompleted + 1
                    prefs.copy(totalTasksCompleted = newCompleted)
                }
            }

            val prefs = preferencesDataStore.preferencesFlow.first()
            val affirmation = if (prefs.affirmations.isNotEmpty()) prefs.affirmations.random() else null

            val freshActiveTasks = taskRepository.getActiveTasks().filter { it.id != taskId }
            val freshNext = findNextActionableTask(freshActiveTasks, taskId)

            _uiState.update {
                it.copy(
                    task = updatedTask,
                    isRunning = false,
                    isPaused = false,
                    isCompleted = true,
                    nextTaskId = freshNext?.id,
                    nextTaskTitle = freshNext?.title,
                    showSprintCompletionSheet = false,
                    showCompletionCelebration = true,
                    showAffordanceRating = !isUntracked,
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

    fun dismissCompletionCelebration() {
        _uiState.update { it.copy(showCompletionCelebration = false) }
    }

    fun submitAffordanceRating(rating: Float) {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            val updated = task.copy(
                affectiveForecastError = rating,
                status = if (task.status == TaskStatus.COMPLETED) TaskStatus.COMPLETED else task.status,
                completedAt = task.completedAt ?: if (task.status == TaskStatus.COMPLETED) System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            _uiState.update { it.copy(task = updated, showAffordanceRating = false, affordanceRatingSubmitted = true) }
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
                isSprintCompleted = false,
                activeSessionId = null
            )
        }
        startTimer()
    }

    fun takeDecompressionRest() {
        soundscapePlayer.pauseWithFadeOut()
        _uiState.update {
            it.copy(
                showSprintCompletionSheet = false,
                isSprintCompleted = false,
                showDecompressionZone = true,
                decompressionRemainingSeconds = 180,
                isDecompressionSighActive = false,
                decompressionSighRemainingSeconds = 0,
                isDecompressionBypassed = false,
                startedAtMillis = if (!it.isCompleted) 0L else it.startedAtMillis,
                accumulatedPauseMillis = if (!it.isCompleted) 0L else it.accumulatedPauseMillis,
                pausedAtMillis = null,
                elapsedSeconds = if (!it.isCompleted) 0L else it.elapsedSeconds,
                remainingSeconds = if (!it.isCompleted && it.timerMode != FocusTimerModeV2.FLOW_STOPWATCH) it.timerMode.durationSeconds else it.remainingSeconds,
                activeSessionId = if (!it.isCompleted) null else it.activeSessionId
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
                showDecompressionZone = false,
                startedAtMillis = if (!it.isCompleted) 0L else it.startedAtMillis,
                accumulatedPauseMillis = if (!it.isCompleted) 0L else it.accumulatedPauseMillis,
                pausedAtMillis = null,
                elapsedSeconds = if (!it.isCompleted) 0L else it.elapsedSeconds,
                remainingSeconds = if (!it.isCompleted && it.timerMode != FocusTimerModeV2.FLOW_STOPWATCH) it.timerMode.durationSeconds else it.remainingSeconds
            )
        }
    }

    fun dismissDecompressionZone() {
        decompressionJob?.cancel()
        decompressionJob = null
        _uiState.update {
            it.copy(
                showDecompressionZone = false,
                startedAtMillis = if (!it.isCompleted) 0L else it.startedAtMillis,
                accumulatedPauseMillis = if (!it.isCompleted) 0L else it.accumulatedPauseMillis,
                pausedAtMillis = null,
                elapsedSeconds = if (!it.isCompleted) 0L else it.elapsedSeconds,
                remainingSeconds = if (!it.isCompleted && it.timerMode != FocusTimerModeV2.FLOW_STOPWATCH) it.timerMode.durationSeconds else it.remainingSeconds
            )
        }
    }

    fun autoBreakDownTask() {
        val task = _uiState.value.task ?: return
        if (task.id == TaskEntity.TASK_UNTRACKED_FOCUS) return
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferencesFlow.first()
            val generated = behavioralSupportService.generateMicroStepsWithAi(
                taskTitle = task.title,
                estimatedMinutes = TaskBlockPlan.plannedMinutes(task),
                apiKey = prefs.geminiApiKey,
                useAi = prefs.geminiAiEnabled,
                userDirective = prefs.adaptiveSystemDirective
            )
            if (generated.isEmpty()) return@launch
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
            val updated = task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis())
            _uiState.update { it.copy(task = updated, microSteps = planSteps, skippedMicroStepIndices = emptySet()) }
            taskRepository.deleteMicroStepsByTaskId(task.id)
            taskRepository.insertMicroSteps(microStepEntities)
            taskRepository.update(updated)
        }
    }

    private var dndNoticeDismissed = false

    fun dismissDndNotice() {
        dndNoticeDismissed = true
        _uiState.update { it.copy(showDndNotice = false) }
    }

    fun checkDndNotice(prefs: UserPreferences = _uiState.value.preferences) {
        if (dndNoticeDismissed) return
        val isDndEnabled = prefs.dndEnabled
        val ctx = context
        if (isDndEnabled && ctx != null) {
            val nm = runCatching { ctx.getSystemService(android.app.NotificationManager::class.java) }.getOrNull()
            val granted = nm?.isNotificationPolicyAccessGranted == true
            _uiState.update { it.copy(showDndNotice = !granted) }
        } else {
            _uiState.update { it.copy(showDndNotice = false) }
        }
    }

    fun onScreenResumed() {
        val ctx = context
        if (ctx != null) {
            val nm = runCatching { ctx.getSystemService(android.app.NotificationManager::class.java) }.getOrNull()
            if (nm?.isNotificationPolicyAccessGranted == true) {
                _uiState.update { it.copy(showDndNotice = false) }
                if (_uiState.value.isRunning && !_uiState.value.isPaused && _uiState.value.preferences.dndEnabled) {
                    enableDndIfPermitted()
                }
            } else if (!dndNoticeDismissed) {
                checkDndNotice()
            }
        }
    }

    private fun enableDndIfPermitted() {
        try {
            val ctx = context ?: return
            if (!_uiState.value.preferences.dndEnabled) return
            val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
            if (nm != null && nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else if (nm != null && !nm.isNotificationPolicyAccessGranted && !dndNoticeDismissed) {
                _uiState.update { it.copy(showDndNotice = true) }
            }
        } catch (_: Exception) {}
    }

    private fun disableDnd() {
        FocusDndHelper.disableDnd(context)
    }

    private fun updateSessionRecord(isOpen: Boolean) {
        val duration = _uiState.value.elapsedSeconds
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            val session = sessionRepository.getOpenSessionForTask(taskId)
            if (session != null) {
                pauseCount = maxOf(pauseCount, session.pauseResumeCount)
                sessionRepository.update(
                    session.copy(
                        endedAt = if (isOpen) null else now,
                        pausedAt = if (isOpen) _uiState.value.pausedAtMillis else null,
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
        val task = _uiState.value.task ?: return
        val updatedTask = task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis())
        _uiState.update { it.copy(task = updatedTask, microSteps = updated, skippedMicroStepIndices = it.skippedMicroStepIndices - index) }

        viewModelScope.launch {
            val roomSteps = taskRepository.getMicroSteps(task.id)
            if (index in roomSteps.indices) {
                taskRepository.toggleMicroStepCompleted(roomSteps[index])
            }
            taskRepository.update(updatedTask)
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

        val task = _uiState.value.task
        val updatedTask = task?.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis())
        _uiState.update { it.copy(task = updatedTask ?: it.task, microSteps = updated) }

        if (task == null) return
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

    fun clearForTest() {
        onCleared()
    }

    public override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        decompressionJob?.cancel()
        disableDnd()
        if ((!_uiState.value.isRunning && !_uiState.value.isPaused) || _uiState.value.isCompleted) {
            soundscapePlayer.release()
            FocusService.stopService(context)
            if (!_uiState.value.isCompleted) {
                context?.let { ctx ->
                    _uiState.value.task?.let { task ->
                        if (task.status == TaskStatus.ACTIVE && task.quadrant != Quadrant.ELIMINATE) {
                            AutonomyNudgeEngine.scheduleNudge(ctx, task)
                        }
                    }
                }
            }
        } else {
            soundscapePlayer.releaseWithFadeOut()
        }
    }

    private fun findNextActionableTask(activeTasks: List<TaskEntity>, currentTaskId: String): TaskEntity? {
        val activeTaskIds = activeTasks.map { it.id }.toSet()
        val now = System.currentTimeMillis()
        val dayEnd = startOfNextLocalDay(now) - 1

        fun isContinuationBlocked(task: TaskEntity, activeIds: Set<String>): Boolean {
            val partNumMatch = Regex("""[#_]part(\d+)""").find(task.id)
            val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""\(Part (\d+)/""").find(task.title)?.groupValues?.get(1)?.toIntOrNull()
            val isContinuation = (partNum != null && partNum >= 2) ||
                task.id.endsWith("_part2") ||
                task.id.contains("#part2") ||
                task.parentTaskId != null
            if (!isContinuation) return false
            val parentSlug = task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
            val isParentActive = (task.parentTaskId != null && task.parentTaskId in activeIds) ||
                (parentSlug != task.id && parentSlug in activeIds)
            val hasPartRegex = task.id.contains(Regex("""[#_]part\d+"""))
            val isPart1Active = if (hasPartRegex) {
                val part1Id = task.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1")
                part1Id != task.id && part1Id in activeIds
            } else false
            val isPrecedingPartActive = if (hasPartRegex && partNum != null) {
                (1 until partNum).any { prevPart ->
                    val prevId = task.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart")
                    prevId != task.id && prevId in activeIds
                }
            } else if (task.parentTaskId != null && partNum != null && partNum > 1) {
                activeTasks.any { other ->
                    other.id != task.id &&
                        (other.parentTaskId == task.parentTaskId || other.id == task.parentTaskId) &&
                        (Regex("""\(Part (\d+)/""").find(other.title)?.groupValues?.get(1)?.toIntOrNull() ?: 1) < partNum
                }
            } else false
            return isParentActive || isPart1Active || isPrecedingPartActive
        }

        return activeTasks
            .filter { task ->
                if (task.id == currentTaskId || task.status != TaskStatus.ACTIVE) return@filter false
                if (task.quadrant == Quadrant.ELIMINATE) return@filter false
                if (task.waitingFor.isNotBlank()) return@filter false

                if (isContinuationBlocked(task, activeTaskIds)) return@filter false

                val isBlockedByPrereq = task.dependencyIds().any { it in activeTaskIds }
                if (isBlockedByPrereq) return@filter false

                // Filter out tasks scheduled for future days
                val scheduledStart = task.resolveScheduledEpochMillis(now)
                val sDate = task.effectiveScheduledDateMillis()
                val isScheduledInFuture = (scheduledStart != null && scheduledStart > dayEnd) || (sDate != null && sDate > dayEnd)
                if (isScheduledInFuture) return@filter false

                true
            }
            .sortedWith(
                compareByDescending<TaskEntity> { it.isFrog }
                    .thenBy { it.effectiveScheduledDateMillis() == null && (it.scheduledTime == null || it.scheduledTime!! < ONE_DAY_MILLIS) }
                    .thenBy { it.scheduledTime == null }
                    .thenBy {
                        it.resolveScheduledEpochMillis(now) ?: Long.MAX_VALUE
                    }
                    .thenBy { it.createdAt }
            )
            .firstOrNull()
    }

    companion object {
        var activeSoundscape: SoundscapeType = SoundscapeType.DEFAULT
    }
}
