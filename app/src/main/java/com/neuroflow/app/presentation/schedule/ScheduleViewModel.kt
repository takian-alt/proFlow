package com.neuroflow.app.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.neuroflow.app.presentation.launcher.work.ScheduleAutoTasksWorker

data class ScheduleUiState(
    val selectedDate: Long = todayStartMillis(),
    val tasksForDay: List<TaskEntity> = emptyList(),
    val lockedTasks: List<TaskEntity> = emptyList(),
    val allActiveTasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val workDayStart: Int = 8,
    val workDayEnd: Int = 20,
    val isAutoScheduleOn: Boolean = false,
    val isAutoScheduling: Boolean = false,
    val autoScheduleResultMessage: String? = null,
    val protectedTimeSlots: List<ProtectedTimeSlot> = emptyList()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val selectedDateFlow = MutableStateFlow(todayStartMillis())

    init {
        observeTasksForSelectedDate()
        observeAllActive()
        observeWorkHoursAndPreferences()
    }

    private fun observeWorkHoursAndPreferences() {
        viewModelScope.launch {
            preferencesDataStore.preferencesFlow.collect { prefs ->
                _uiState.update {
                    it.copy(
                        workDayStart = prefs.workDayStart,
                        workDayEnd = prefs.workDayEnd,
                        isAutoScheduleOn = prefs.autoSchedulingEnabled,
                        protectedTimeSlots = prefs.protectedTimeSlots
                    )
                }
            }
        }
    }

    fun toggleAutoSchedule() {
        viewModelScope.launch {
            val newToggleState = !_uiState.value.isAutoScheduleOn
            preferencesDataStore.updatePreferences {
                it.copy(
                    autoSchedulingEnabled = newToggleState,
                    isAutoScheduleToggleOn = newToggleState
                )
            }
            if (newToggleState) {
                triggerAutoScheduleNow()
            }
        }
    }

    fun triggerAutoScheduleNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAutoScheduling = true) }
            val workRequest = ScheduleAutoTasksWorker.buildOneTimeWorkRequest(forceReplan = true)
            workManager.enqueueUniqueWork(
                ScheduleAutoTasksWorker.FOREGROUND_TICK_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            workManager.getWorkInfoByIdFlow(workRequest.id)
                .firstOrNull { it != null && it.state.isFinished }
            _uiState.update {
                it.copy(
                    isAutoScheduling = false,
                    autoScheduleResultMessage = "Auto-scheduling complete"
                )
            }
        }
    }

    fun clearAutoScheduleResultMessage() {
        _uiState.update { it.copy(autoScheduleResultMessage = null) }
    }

    private fun observeAllActive() {
        viewModelScope.launch {
            taskRepository.observeActiveTasks().collect { tasks ->
                _uiState.update { it.copy(allActiveTasks = tasks) }
            }
        }
    }

    private fun observeTasksForSelectedDate() {
        viewModelScope.launch {
            selectedDateFlow
                .flatMapLatest { date ->
                    taskRepository.observeTasksForDate(date)
                }
                .collect { tasks ->
                    val sortedTasks = tasks.sortedWith(
                        compareBy<TaskEntity> { it.timelineStartMinuteOfDay() }
                            .thenBy { it.createdAt }
                    )
                    val locked = sortedTasks.filter { it.isScheduleLocked }
                    _uiState.update {
                        it.copy(
                            selectedDate = selectedDateFlow.value,
                            tasksForDay = sortedTasks,
                            lockedTasks = locked,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun selectDate(date: Long) {
        selectedDateFlow.value = date
        _uiState.update { it.copy(selectedDate = date, isLoading = true) }
    }

    fun selectToday() {
        selectDate(todayStartMillis())
    }

    fun nextDay() {
        val next = Calendar.getInstance().apply {
            timeInMillis = selectedDateFlow.value
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        selectDate(next)
    }

    fun previousDay() {
        val prev = Calendar.getInstance().apply {
            timeInMillis = selectedDateFlow.value
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        selectDate(prev)
    }

    /** Assigns an existing task to a specific hour slot on the selected date. Locked tasks are skipped. */
    fun scheduleTask(task: TaskEntity, hour: Int, minute: Int = 0) {
        if (task.isScheduleLocked) return
        viewModelScope.launch {
            // Build the full scheduled timestamp using Calendar (DST-safe)
            val cal = Calendar.getInstance().apply {
                timeInMillis = _uiState.value.selectedDate
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val scheduledMillis = cal.timeInMillis

            // Split into date and time components (DST-safe)
            val dateCal = Calendar.getInstance().apply {
                timeInMillis = scheduledMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val scheduledDate = dateCal.timeInMillis
            val scheduledTime = scheduledMillis - scheduledDate

            taskRepository.update(
                task.copy(
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime,
                    isAutoScheduled = false,
                    lastAutoScheduledAt = null,  // Reset cooldown when user manually reschedules
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** Inserts a brand-new task (from the NewTaskSheet). */
    fun insertTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.insert(task) }
    }

    fun addProtectedSlot(slot: ProtectedTimeSlot) {
        viewModelScope.launch {
            val updated = _uiState.value.protectedTimeSlots.filterNot { it.id == slot.id } + slot
            preferencesDataStore.updateProtectedTimeSlots(updated)
            if (_uiState.value.isAutoScheduleOn) {
                triggerAutoScheduleNow()
            }
        }
    }

    fun removeProtectedSlot(slotId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.protectedTimeSlots.filterNot { it.id == slotId }
            preferencesDataStore.updateProtectedTimeSlots(updated)
            if (_uiState.value.isAutoScheduleOn) {
                triggerAutoScheduleNow()
            }
        }
    }

    fun toggleProtectedSlot(slotId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.protectedTimeSlots.map {
                if (it.id == slotId) it.copy(isEnabled = !it.isEnabled) else it
            }
            preferencesDataStore.updateProtectedTimeSlots(updated)
            if (_uiState.value.isAutoScheduleOn) {
                triggerAutoScheduleNow()
            }
        }
    }
}

fun todayStartMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
