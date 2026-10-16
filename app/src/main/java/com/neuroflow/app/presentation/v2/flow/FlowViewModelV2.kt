package com.neuroflow.app.presentation.v2.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlowUiStateV2(
    val activeTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val energyUi: EnergyScoreRepository.EnergyUiModel? = null,
    val recommendedTask: TaskEntity? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FlowViewModelV2 @Inject constructor(
    private val taskRepository: TaskRepository,
    private val energyScoreRepository: EnergyScoreRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val uiState: StateFlow<FlowUiStateV2> = combine(
        taskRepository.observeActiveTasks(),
        taskRepository.observeCompletedTasks(),
        energyScoreRepository.observeEnergy(),
        preferencesDataStore.preferencesFlow
    ) { activeTasks, completedTasks, energyUi, prefs ->
        val battery = energyUi?.availableEnergy ?: 50
        val currentEnergyZone = when {
            battery >= 65 -> EnergyLevel.HIGH
            battery >= 35 -> EnergyLevel.MEDIUM
            else -> EnergyLevel.LOW
        }

        // Recommend highest-impact task matching current biological zone
        val recommended = activeTasks
            .filter { it.energyLevel == currentEnergyZone }
            .maxByOrNull { (if (it.isFrog) 1000 else 0) + it.impactScore }
            ?: activeTasks.maxByOrNull { (if (it.isFrog) 1000 else 0) + it.impactScore }

        FlowUiStateV2(
            activeTasks = activeTasks,
            completedTasks = completedTasks,
            energyUi = energyUi,
            recommendedTask = recommended,
            preferences = prefs,
            isLoading = false
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
            taskRepository.completeAndRecur(task, System.currentTimeMillis())
        }
    }

    fun deferTask(task: TaskEntity, hours: Int = 2) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newScheduledTime = (task.scheduledTime ?: now) + hours * 3600_000L
            taskRepository.update(
                task.copy(
                    scheduledTime = newScheduledTime,
                    postponeCount = task.postponeCount + 1,
                    updatedAt = now
                )
            )
        }
    }
}

