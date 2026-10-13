package com.neuroflow.app.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val preferences = preferencesDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeTasks: StateFlow<List<TaskEntity>> = taskRepository.observeActiveTasks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveYearlyGoals(goals: List<String>) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { it.copy(yearlyGoals = goals) }
        }
    }

    fun saveWeeklyGoals(goals: List<String>) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { it.copy(weeklyGoals = goals) }
        }
    }

    fun saveWeeklyIntent(intent: String) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { it.copy(weeklyIntent = intent) }
        }
    }

    fun markUserGuidePromptShown() {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { it.copy(userGuidePromptShown = true) }
        }
    }
}
