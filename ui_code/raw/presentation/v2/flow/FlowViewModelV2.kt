package com.neuroflow.app.presentation.v2.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToInt

data class FlowUiStateV2(
    val activeTasks: List<TaskEntity> = emptyList(),
    val filteredTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val energyUi: EnergyUiModel? = null,
    val circadianSummary: DailyCircadianSummary? = null,
    val dayTimeline: List<CircadianSnapshot> = emptyList(),
    val energyTimeline: List<CircadianSnapshot> = emptyList(),
    val dailyCognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
    val daytimeNaps: List<DaytimeNapBout> = emptyList(),
    val isFallbackSleep: Boolean = false,
    val learnedSampleCount: Double = 0.0,
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
    private val preferencesDataStore: UserPreferencesDataStore,
    private val noteRepository: NoteRepository? = null,
    private val sessionRepository: SessionRepository? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val taskSchedulerService: TaskSchedulerService = DefaultTaskSchedulerService(circadianEnergyService),
    private val behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService()
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
            preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
                old.wakeUpHour == new.wakeUpHour &&
                    old.sleepPressurePoints == new.sleepPressurePoints &&
                    old.onboardingCompleted == new.onboardingCompleted &&
                    old.tagCatalog == new.tagCatalog
            },
            sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())
        ) { active, completed, prefs, sleepLogs ->
            Quad(active, completed, prefs, sleepLogs)
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
    ) { (activeTasks, completedTasks, prefs, sleepLogs), (dismissedGuilt, tag, shutdownDismissed, lastCompleted), (lastDeferred, actionMsg) ->
        val now = System.currentTimeMillis()
        val currentHour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        val systemZone = java.time.ZoneId.systemDefault()
        val zonedDateTime = java.time.Instant.ofEpochMilli(now).atZone(systemZone)
        var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(systemZone)
        if (anchor.toInstant().toEpochMilli() > now) {
            anchor = anchor.minusDays(1)
        }
        val baseWakeMillis = anchor.toInstant().toEpochMilli()

        val signal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )

        val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
        val naps = signal?.daytimeNaps ?: emptyList()
        val isFallback = sleepLogs.isEmpty() || (signal != null && signal.consolidatedSessions.isEmpty())

        val sevenDaysAgo = now - 7 * 86_400_000L
        val recentSleepLogs = sleepLogs.filter { it.startAt >= sevenDaysAgo || it.endAt >= sevenDaysAgo }
        val rollingAverageSleepMinutes = if (recentSleepLogs.isNotEmpty()) {
            recentSleepLogs.map { if (it.durationMinutes > 0) it.durationMinutes.toDouble() else (it.endAt - it.startAt) / 60_000.0 }.average()
        } else {
            480.0
        }

        val dailyBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = zonedDateTime.toLocalDate(),
            zoneId = systemZone
        )

        val behavioralObservations = buildBehavioralObservations(completedTasks = completedTasks, zoneId = systemZone)
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (behavioralObservations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(behavioralObservations, initialPeakHour = initialPeak)
        } else null
        val sampleCount = learnedProfile?.sampleCount ?: 0.0

        val (snapshot, energyUi) = computeEnergyUiState(
            circadianEnergyService = circadianEnergyService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            prefs = prefs,
            sleepLogs = sleepLogs,
            completedTasks = completedTasks,
            nowMillis = now
        )
        val battery = snapshot.usableEnergy
        val currentEnergyZone = when {
            battery >= 65 -> EnergyLevel.HIGH
            battery >= 35 -> EnergyLevel.MEDIUM
            else -> EnergyLevel.LOW
        }

        val overdueTasks = activeTasks.filter { task ->
            task.postponeCount >= 3 ||
                (task.scheduledTime != null && task.scheduledTime!! < now - 2 * 3600_000L) ||
                (task.scheduledDate != null && task.scheduledDate!! < now - 86_400_000L)
        }
        val showZeroGuilt = overdueTasks.size >= 3 && !dismissedGuilt

        // Dynamic Tag Catalog Merge
        val defaultTags = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin")
        val dynamicTags = (prefs.tagCatalog + activeTasks.flatMap { it.tags.split(",") })
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .map { "#$it" }
        val mergedTags = (defaultTags + dynamicTags).distinctBy { it.lowercase() }

        val matchingTasks = activeTasks.filter { it.energyLevel == currentEnergyZone }
        val candidatePool = if (matchingTasks.isNotEmpty()) matchingTasks else activeTasks
        val effectiveSnapshot = snapshot.copy(usableEnergy = battery)
        val recommended = candidatePool.maxByOrNull { task ->
            val schedulable = SchedulableTask(
                id = task.id,
                title = task.title,
                category = when (task.taskType) {
                    TaskType.ANALYTICAL -> TaskCategory.DEEP_WORK
                    TaskType.CREATIVE -> TaskCategory.CREATIVE
                    TaskType.ADMIN -> TaskCategory.ADMIN
                    TaskType.PHYSICAL -> TaskCategory.EXERCISE
                    TaskType.GENERAL -> TaskCategory.GENERAL
                    TaskType.MINDFULNESS -> TaskCategory.MINDFULNESS
                },
                priority = when (task.priority) {
                    Priority.HIGH -> 2
                    Priority.MEDIUM -> 1
                    else -> 0
                },
                estimatedMinutes = task.estimatedDurationMinutes,
                energyDemand = when (task.energyLevel) {
                    EnergyLevel.HIGH -> EnergyDemand.HIGH
                    EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                    else -> EnergyDemand.LOW
                },
                isFrog = task.isFrog,
                scheduledStartMillis = task.resolveScheduledEpochMillis(),
                deadlineMillis = task.resolveDeadlineEpochMillis(),
                isLocked = task.isScheduleLocked,
                parentTaskId = task.parentTaskId,
                prerequisiteIds = task.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet(),
                impactScore = task.impactScore,
                estimatedEnjoyment = task.enjoymentScore / 10,
                postponeCount = task.postponeCount
            )
            val baseLeverage = (if (task.isFrog) 1000 else 0) + task.impactScore
            baseLeverage + taskSchedulerService.evaluateTaskScore(schedulable, effectiveSnapshot).totalScore
        }

        val filtered = if (tag.isNullOrBlank() || tag.equals("All", ignoreCase = true)) {
            activeTasks
        } else {
            val query = tag.removePrefix("#").trim()
            activeTasks.filter { task ->
                task.tags.contains(query, ignoreCase = true) ||
                    task.title.contains(query, ignoreCase = true)
            }
        }

        val isEvening = currentHour >= 19

        val manualPeakHour = if (prefs.manualPeakProfileEnabled) (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
        val chronotype = if (!prefs.quizChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.quizChronotype)
        } else if (!prefs.manualChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.manualChronotype)
        } else if (prefs.meqScore > 0) {
            when {
                prefs.meqScore >= 70 -> ChronotypeProfile.DEFINITE_MORNING
                prefs.meqScore >= 59 -> ChronotypeProfile.MODERATE_MORNING
                prefs.meqScore >= 42 -> ChronotypeProfile.INTERMEDIATE
                prefs.meqScore >= 31 -> ChronotypeProfile.MODERATE_EVENING
                else -> ChronotypeProfile.DEFINITE_EVENING
            }
        } else {
            ChronotypeProfile.INTERMEDIATE
        }

        val circadianSummary = runCatching {
            circadianEnergyService.computeDailySummary(
                timestampMillis = now,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = systemZone,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakHour,
                cognitiveBlocks = dailyBlocks,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes
            )
        }.getOrNull()

        val dayStart = zonedDateTime.toLocalDate().atStartOfDay(systemZone).toInstant().toEpochMilli()
        val dayTimeline: List<CircadianSnapshot> = runCatching {
            circadianEnergyService.projectDayTimeline(
                dayStartMillis = dayStart,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                stepMinutes = 15,
                zoneId = systemZone,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakHour,
                cognitiveBlocks = dailyBlocks,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes
            )
        }.getOrElse { emptyList() }

        FlowUiStateV2(
            activeTasks = activeTasks,
            filteredTasks = filtered,
            completedTasks = completedTasks,
            energyUi = energyUi,
            circadianSummary = circadianSummary,
            dayTimeline = dayTimeline,
            energyTimeline = dayTimeline,
            dailyCognitiveBlocks = dailyBlocks,
            daytimeNaps = naps,
            isFallbackSleep = isFallback,
            learnedSampleCount = sampleCount,
            recommendedTask = recommended,
            preferences = prefs,
            isLoading = false,
            overdueTasksCount = overdueTasks.size,
            showZeroGuiltPrompt = showZeroGuilt,
            selectedTag = tag,
            availableTags = mergedTags,
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

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.update(task)
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

    fun breakDownTask(task: TaskEntity) {
        viewModelScope.launch {
            val cleanTitle = task.title.trim().take(30)
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
            taskRepository.deleteMicroStepsByTaskId(task.id)
            taskRepository.insertMicroSteps(microStepEntities)

            val planSteps = generated.map { StepByStepPlanStep(it.title, false) }
            val serialized = StepByStepPlanCodec.serialize(planSteps)
            taskRepository.update(
                task.copy(
                    ifThenPlan = serialized,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastActionMessage.value = "Decomposed '${cleanTitle}' into ${generated.size} micro-steps 🪄"
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
