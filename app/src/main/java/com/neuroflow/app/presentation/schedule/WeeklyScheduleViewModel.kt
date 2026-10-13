package com.neuroflow.app.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class WeeklyScheduleUiState(
    val weekDays: List<Long> = emptyList(), // 7 days from Sunday to Saturday
    val activeTasks: List<TaskEntity> = emptyList(),
    val protectedSlots: List<ProtectedTimeSlot> = emptyList(),
    val weeklyIntent: String = "",
    val workDayStart: Int = 8,
    val workDayEnd: Int = 20,
    val isLoading: Boolean = false
)

fun getSundayWeekDays(referenceMillis: Long = System.currentTimeMillis()): List<Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysFromSunday = currentDayOfWeek - Calendar.SUNDAY
        add(Calendar.DAY_OF_YEAR, -daysFromSunday)
    }

    val days = mutableListOf<Long>()
    for (i in 0 until 7) {
        days.add(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return days
}

@HiltViewModel
class WeeklyScheduleViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val currentReferenceMillis = MutableStateFlow(System.currentTimeMillis())

    private val _uiState = MutableStateFlow(
        WeeklyScheduleUiState(
            weekDays = getSundayWeekDays()
        )
    )
    val uiState: StateFlow<WeeklyScheduleUiState> = _uiState.asStateFlow()

    init {
        observeWeekDays()
        observeTasks()
        observePreferences()
    }

    private fun observeWeekDays() {
        viewModelScope.launch {
            currentReferenceMillis.collect { refMillis ->
                _uiState.update { it.copy(weekDays = getSundayWeekDays(refMillis)) }
            }
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            taskRepository.observeActiveTasks().collect { tasks ->
                _uiState.update { it.copy(activeTasks = tasks) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesDataStore.preferencesFlow.collect { prefs ->
                _uiState.update {
                    it.copy(
                        protectedSlots = prefs.protectedTimeSlots,
                        weeklyIntent = prefs.weeklyIntent,
                        workDayStart = prefs.workDayStart,
                        workDayEnd = prefs.workDayEnd
                    )
                }
            }
        }
    }

    fun nextWeek() {
        val next = Calendar.getInstance().apply {
            timeInMillis = currentReferenceMillis.value
            add(Calendar.WEEK_OF_YEAR, 1)
        }.timeInMillis
        currentReferenceMillis.value = next
    }

    fun previousWeek() {
        val prev = Calendar.getInstance().apply {
            timeInMillis = currentReferenceMillis.value
            add(Calendar.WEEK_OF_YEAR, -1)
        }.timeInMillis
        currentReferenceMillis.value = prev
    }

    fun goToCurrentWeek() {
        currentReferenceMillis.value = System.currentTimeMillis()
    }

    fun saveWeeklyIntent(intent: String) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { it.copy(weeklyIntent = intent) }
        }
    }

    fun addProtectedSlot(slot: ProtectedTimeSlot) {
        viewModelScope.launch {
            val updated = _uiState.value.protectedSlots.filterNot { it.id == slot.id } + slot
            preferencesDataStore.updateProtectedTimeSlots(updated)
        }
    }

    fun removeProtectedSlot(slotId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.protectedSlots.filterNot { it.id == slotId }
            preferencesDataStore.updateProtectedTimeSlots(updated)
        }
    }

    fun toggleProtectedSlot(slotId: String) {
        viewModelScope.launch {
            val updated = _uiState.value.protectedSlots.map {
                if (it.id == slotId) it.copy(isEnabled = !it.isEnabled) else it
            }
            preferencesDataStore.updateProtectedTimeSlots(updated)
        }
    }
}
