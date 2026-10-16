package com.neuroflow.app.presentation.v2.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
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
        energyScoreRepository.observeEnergy()
    ) { activeTasks, completedTasks, energyUi ->
        FlowUiStateV2(
            activeTasks = activeTasks,
            completedTasks = completedTasks,
            energyUi = energyUi,
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
}
