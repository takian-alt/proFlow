package com.neuroflow.app.presentation.v2.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.ScheduleConflict
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
    private val workManager: WorkManager
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(startOfTodayMillis())
    private val _viewMode = MutableStateFlow(PlanViewModeV2.TIMELINE)
    private val _isAutoScheduling = MutableStateFlow(false)

    private data class PlanNavParams(
        val dateMillis: Long,
        val mode: PlanViewModeV2,
        val autoScheduling: Boolean
    )

    val uiState: StateFlow<PlanUiStateV2> = combine(
        combine(_selectedDate, _viewMode, _isAutoScheduling) { date, mode, autoScheduling ->
            PlanNavParams(date, mode, autoScheduling)
        },
        taskRepository.observeActiveTasks(),
        preferencesDataStore.preferencesFlow,
        energyScoreRepository.observeEnergy()
    ) { params, allTasks, prefs, energyUi ->
        val dateMillis = params.dateMillis
        val mode = params.mode
        val autoScheduling = params.autoScheduling

        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val dayStart = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000L - 1

        val tasksForDay = allTasks.filter { task ->
            val sDate = task.scheduledDate
            if (sDate != null) {
                sDate in dayStart..dayEnd
            } else {
                task.deadlineDate != null && task.deadlineDate in dayStart..dayEnd
            }
        }.sortedBy { it.scheduledTime ?: it.timelineStartMinuteOfDay()?.toLong() ?: Long.MAX_VALUE }

        val unscheduled = allTasks.filter { it.scheduledDate == null }

        val byQuadrant = Quadrant.entries.associateWith { q ->
            allTasks.filter { it.quadrant == q }
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

    fun toggleAutoSchedule() {
        viewModelScope.launch {
            val current = uiState.value.isAutoScheduleOn
            preferencesDataStore.updatePreferences { it.copy(autoSchedulingEnabled = !current) }
        }
    }

    fun triggerAutoScheduleNow() {
        viewModelScope.launch {
            _isAutoScheduling.value = true
            val request = OneTimeWorkRequestBuilder<ScheduleAutoTasksWorker>().build()
            workManager.enqueueUniqueWork("AutoScheduleNowV2", ExistingWorkPolicy.REPLACE, request)
            kotlinx.coroutines.delay(1200)
            _isAutoScheduling.value = false
        }
    }

    fun moveTaskToQuadrant(task: TaskEntity, newQuadrant: Quadrant) {
        viewModelScope.launch {
            taskRepository.update(task.copy(quadrant = newQuadrant, updatedAt = System.currentTimeMillis()))
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
}
