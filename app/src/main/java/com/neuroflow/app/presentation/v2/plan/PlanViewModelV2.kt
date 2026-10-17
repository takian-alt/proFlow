package com.neuroflow.app.presentation.v2.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.ScheduleConflict
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.entity.QuickNoteEntity
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.worker.ScheduleAutoTasksWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class PlanViewModeV2(val title: String) {
    TIMELINE("Timeline"),
    MATRIX("Matrix"),
    AUTOPILOT("AI Autopilot")
}

data class PlanUiStateV2(
    val selectedDateMillis: Long = startOfTodayMillis(),
    val viewMode: PlanViewModeV2 = PlanViewModeV2.TIMELINE,
    val tasksForDay: List<TaskEntity> = emptyList(),
    val unscheduledTasks: List<TaskEntity> = emptyList(),
    val tasksByQuadrant: Map<Quadrant, List<TaskEntity>> = emptyMap(),
    val protectedSlots: List<ProtectedTimeSlot> = emptyList(),
    val conflicts: List<ScheduleConflict> = emptyList(),
    val isAutoScheduleOn: Boolean = false,
    val isAutoScheduling: Boolean = false,
    val selectedTag: String? = null,
    val availableTags: List<String> = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin"),
    val workDayStart: Int = 8,
    val workDayEnd: Int = 20,
    val energyUi: EnergyScoreRepository.EnergyUiModel? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true
)

private fun startOfTodayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@HiltViewModel
class PlanViewModelV2 @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val energyScoreRepository: EnergyScoreRepository,
    private val workManager: WorkManager,
    private val noteRepository: NoteRepository? = null,
    private val sessionRepository: SessionRepository? = null,
    private val quickNoteDao: QuickNoteDao? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val taskSchedulerService: TaskSchedulerService = DefaultTaskSchedulerService(circadianEnergyService),
    private val behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService(),
    private val calendarSyncRepository: com.neuroflow.app.data.calendar.CalendarSyncRepository? = null
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(startOfTodayMillis())
    private val _viewMode = MutableStateFlow(PlanViewModeV2.TIMELINE)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _isAutoScheduling = MutableStateFlow(false)

    val recentNotes: Flow<List<NoteEntity>> = noteRepository?.allNotes ?: flowOf(emptyList())

    private data class PlanNavParams(
        val dateMillis: Long,
        val mode: PlanViewModeV2,
        val autoScheduling: Boolean,
        val tag: String?
    )

    val uiState: StateFlow<PlanUiStateV2> = combine(
        combine(_selectedDate, _viewMode, _isAutoScheduling, _selectedTag) { date, mode, autoScheduling, tag ->
            PlanNavParams(date, mode, autoScheduling, tag)
        },
        taskRepository.observeActiveTasks(),
        preferencesDataStore.preferencesFlow,
        energyScoreRepository.observeEnergy()
    ) { params, allTasks, prefs, energyUi ->
        val dateMillis = params.dateMillis
        val mode = params.mode
        val autoScheduling = params.autoScheduling
        val tag = params.tag

        val tagFilteredAll = if (tag.isNullOrBlank() || tag.equals("All", ignoreCase = true)) {
            allTasks
        } else {
            val q = tag.removePrefix("#").trim()
            allTasks.filter { it.tags.contains(q, ignoreCase = true) || it.title.contains(q, ignoreCase = true) }
        }

        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val dayStart = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000L - 1

        val tasksForDay = tagFilteredAll.filter { task ->
            val sDate = task.scheduledDate
            if (sDate != null) {
                sDate in dayStart..dayEnd
            } else {
                task.deadlineDate != null && task.deadlineDate in dayStart..dayEnd
            }
        }.sortedBy { it.scheduledTime ?: it.timelineStartMinuteOfDay()?.toLong() ?: Long.MAX_VALUE }

        val unscheduled = tagFilteredAll.filter { it.scheduledDate == null }

        val byQuadrant = Quadrant.entries.associateWith { q ->
            tagFilteredAll.filter { it.quadrant == q }
        }

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = tasksForDay,
            protectedSlots = prefs.protectedTimeSlots,
            workDayStart = prefs.workDayStart,
            workDayEnd = prefs.workDayEnd,
            dayMillis = dateMillis
        )

        PlanUiStateV2(
            selectedDateMillis = dateMillis,
            viewMode = mode,
            tasksForDay = tasksForDay,
            unscheduledTasks = unscheduled,
            tasksByQuadrant = byQuadrant,
            protectedSlots = prefs.protectedTimeSlots,
            conflicts = conflicts,
            isAutoScheduleOn = prefs.autoSchedulingEnabled,
            isAutoScheduling = autoScheduling,
            selectedTag = tag,
            workDayStart = prefs.workDayStart,
            workDayEnd = prefs.workDayEnd,
            energyUi = energyUi,
            preferences = prefs,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlanUiStateV2()
    )

    fun setViewMode(mode: PlanViewModeV2) {
        _viewMode.value = mode
    }

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun toggleAutoSchedule() {
        viewModelScope.launch {
            val current = uiState.value.isAutoScheduleOn
            preferencesDataStore.updatePreferences { it.copy(autoSchedulingEnabled = !current) }
        }
    }

    fun autoScheduleDay(dateMillis: Long = _selectedDate.value, forceReplan: Boolean = false) {
        viewModelScope.launch {
            _isAutoScheduling.value = true
            try {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val allTasks = taskRepository.getActiveTasks()
                val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                val dayStart = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val dayEnd = dayStart + 24 * 3600_000L - 1

                val externalBusyWindows = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd) ?: emptyList()

                val zoneId = java.time.ZoneId.systemDefault()
                val dayDate = java.time.Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate()
                val nowMillis = System.currentTimeMillis()

                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                val wakeHour = prefs.wakeUpHour
                val hoursAwake = (currentHour - wakeHour).let { if (it < 0) it + 24 else it }
                val wakeTimeMillis = nowMillis - (hoursAwake * 3600_000L)

                val candidateTasks = allTasks.filter { task ->
                    val sDate = task.scheduledDate
                    sDate == null || (sDate in dayStart..dayEnd && !task.isScheduleLocked)
                }

                val schedulableTasks = candidateTasks.map { task ->
                    com.neuroflow.app.domain.service.SchedulableTask(
                        id = task.id,
                        title = task.title,
                        category = when (task.taskType) {
                            com.neuroflow.app.domain.model.TaskType.ANALYTICAL -> com.neuroflow.app.domain.service.TaskCategory.DEEP_WORK
                            com.neuroflow.app.domain.model.TaskType.CREATIVE -> com.neuroflow.app.domain.service.TaskCategory.CREATIVE
                            com.neuroflow.app.domain.model.TaskType.ADMIN -> com.neuroflow.app.domain.service.TaskCategory.ADMIN
                            com.neuroflow.app.domain.model.TaskType.PHYSICAL -> com.neuroflow.app.domain.service.TaskCategory.EXERCISE
                        },
                        priority = when (task.priority) {
                            com.neuroflow.app.domain.model.Priority.HIGH -> 2
                            com.neuroflow.app.domain.model.Priority.MEDIUM -> 1
                            else -> 0
                        },
                        estimatedMinutes = task.estimatedDurationMinutes,
                        energyDemand = when (task.energyLevel) {
                            com.neuroflow.app.domain.model.EnergyLevel.HIGH -> com.neuroflow.app.domain.service.EnergyDemand.HIGH
                            com.neuroflow.app.domain.model.EnergyLevel.MEDIUM -> com.neuroflow.app.domain.service.EnergyDemand.MODERATE
                            else -> com.neuroflow.app.domain.service.EnergyDemand.LOW
                        },
                        isFrog = task.isFrog,
                        scheduledStartMillis = task.scheduledTime?.let { (task.scheduledDate ?: dayStart) + it },
                        deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) },
                        isLocked = task.isScheduleLocked,
                        parentTaskId = task.parentTaskId
                    )
                }

                val protectedSlots = prefs.protectedTimeSlots.filter { it.isEnabled }.map {
                    com.neuroflow.app.domain.service.ProtectedLocalSlot(
                        startHour = it.startMinuteOfDay / 60,
                        startMinute = it.startMinuteOfDay % 60,
                        durationMinutes = it.durationMinutes
                    )
                }

                val placements = taskSchedulerService.scheduleDay(
                    tasks = schedulableTasks,
                    dayDate = dayDate,
                    nowMillis = nowMillis,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = 480,
                    busyWindows = externalBusyWindows,
                    protectedSlots = protectedSlots,
                    calibrationRatio = 1.20,
                    forceReplan = forceReplan,
                    zoneId = zoneId
                )

                for (placement in placements) {
                    val fresh = taskRepository.getById(placement.task.id) ?: continue
                    if (fresh.status != com.neuroflow.app.domain.model.TaskStatus.ACTIVE || fresh.isScheduleLocked) continue
                    val calSlot = Calendar.getInstance().apply { timeInMillis = placement.window.startMillis }
                    val startOfDay = calSlot.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val timeOffset = placement.window.startMillis - startOfDay

                    taskRepository.update(
                        fresh.copy(
                            scheduledDate = startOfDay,
                            scheduledTime = timeOffset,
                            isAutoScheduled = true,
                            plannedBlockMinutes = placement.window.durationMinutes,
                            lastAutoScheduledAt = nowMillis,
                            updatedAt = nowMillis
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PlanViewModelV2", "Auto-schedule failed", e)
            } finally {
                _isAutoScheduling.value = false
            }
        }
    }

    fun triggerAutoScheduleNow() {
        viewModelScope.launch {
            autoScheduleDay(_selectedDate.value, forceReplan = false)
            val request = OneTimeWorkRequestBuilder<ScheduleAutoTasksWorker>().build()
            workManager.enqueueUniqueWork("AutoScheduleNowV2", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun moveTaskToQuadrant(task: TaskEntity, newQuadrant: Quadrant) {
        viewModelScope.launch {
            taskRepository.update(task.copy(quadrant = newQuadrant, updatedAt = System.currentTimeMillis()))
        }
    }

    fun breakDownTask(task: TaskEntity) {
        viewModelScope.launch {
            val generated = behavioralSupportService.generateMicroSteps(task.title, task.estimatedDurationMinutes)
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
            taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
        }
    }

    fun saveQuickNote(content: String, title: String = "") {
        if (content.isBlank()) return
        viewModelScope.launch {
            val trimmed = content.trim()
            quickNoteDao?.insert(
                QuickNoteEntity(
                    content = trimmed,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            noteRepository?.saveNote(
                NoteEntity(
                    title = title.ifBlank { "Quick Thought" },
                    content = trimmed,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun assignTaskTime(task: TaskEntity, hour: Int, minute: Int) {
        viewModelScope.launch {
            val millisInDay = hour * 3600_000L + minute * 60_000L
            taskRepository.update(
                task.copy(
                    scheduledDate = uiState.value.selectedDateMillis,
                    scheduledTime = millisInDay,
                    isScheduleLocked = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.completeAndRecur(task, System.currentTimeMillis())
        }
    }

    fun toggleProtectedSlot(slotId: String, enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { prefs ->
                val updatedSlots = prefs.protectedTimeSlots.map { slot ->
                    if (slot.id == slotId) slot.copy(isEnabled = enabled) else slot
                }
                prefs.copy(protectedTimeSlots = updatedSlots)
            }
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
        }
    }
}
