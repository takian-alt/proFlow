package com.neuroflow.app.presentation.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdentityViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val goalRepository: GoalRepository? = null
) : ViewModel() {

    val preferences = preferencesDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.neuroflow.app.data.local.UserPreferences())

    val activeGoals: StateFlow<List<GoalEntity>> =
        goalRepository?.observeActiveGoals()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            ?: MutableStateFlow(emptyList())

    val affirmations = preferencesDataStore.preferencesFlow
        .map { it.affirmations }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Emits true when a duplicate was rejected
    private val _duplicateEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val duplicateEvent = _duplicateEvent.asSharedFlow()

    fun addAffirmation(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            var isDuplicate = false
            preferencesDataStore.updatePreferences { prefs ->
                if (prefs.affirmations.any { it.equals(trimmed, ignoreCase = true) }) {
                    isDuplicate = true
                    return@updatePreferences prefs
                }
                prefs.copy(affirmations = prefs.affirmations + trimmed)
            }
            if (isDuplicate) _duplicateEvent.tryEmit(Unit)
        }
    }

    fun removeAffirmation(index: Int) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { prefs ->
                if (index < 0 || index >= prefs.affirmations.size) return@updatePreferences prefs
                prefs.copy(affirmations = prefs.affirmations.toMutableList().also { it.removeAt(index) })
            }
        }
    }

    fun updateIdentityLabel(label: String) {
        viewModelScope.launch {
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(identityLabel = label.trim())
            }
        }
    }

    fun updateWeeklyGoals(goals: List<String>) {
        viewModelScope.launch {
            val cleanedGoals = goals.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(weeklyGoals = cleanedGoals)
            }
            if (goalRepository != null) {
                val existingGoals = goalRepository.getActiveGoals()
                val existingWeeklyGoals = existingGoals.filter { it.description == "Weekly Objective" || (it.description != "Yearly Objective" && it.description.isBlank()) }
                val cleanedLowerSet = cleanedGoals.map { it.lowercase() }.toSet()
                // Synchronize deleted/removed weekly goals in Room without wiping Yearly Objectives (BUG-180)
                existingWeeklyGoals.forEach { existing ->
                    if (existing.title.trim().lowercase() !in cleanedLowerSet) {
                        goalRepository.delete(existing)
                    }
                }
                val existingMap = existingWeeklyGoals.associateBy { it.title.trim().lowercase() }
                cleanedGoals.forEach { title ->
                    if (title.lowercase() !in existingMap) {
                        goalRepository.insert(
                            GoalEntity(
                                title = title,
                                description = "Weekly Objective"
                            )
                        )
                    }
                }
            }
        }
    }
}
