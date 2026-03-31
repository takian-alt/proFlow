package com.neuroflow.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.GoalRepository
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.AppTheme
import com.neuroflow.app.worker.scheduleNotificationWorkers
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesDataStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            val current = preferences.value
            val updated = update(current)
            preferencesDataStore.updatePreferences { updated }

            val scheduleChanged =
                current.dailyPlanNotificationsEnabled != updated.dailyPlanNotificationsEnabled ||
                    current.streakNotificationsEnabled != updated.streakNotificationsEnabled ||
                    current.deadlineEscalationNotificationsEnabled != updated.deadlineEscalationNotificationsEnabled ||
                    current.dailyPlanNotificationHour != updated.dailyPlanNotificationHour ||
                    current.streakCheckNotificationHour != updated.streakCheckNotificationHour

            if (scheduleChanged) {
                scheduleNotificationWorkers(context, updated)
            }

            if (current.autonomyNudgeNotificationsEnabled && !updated.autonomyNudgeNotificationsEnabled) {
                WorkManager.getInstance(context).cancelAllWorkByTag(AutonomyNudgeEngine.globalTag())
            }
            if (current.deadlineReminderNotificationsEnabled && !updated.deadlineReminderNotificationsEnabled) {
                WorkManager.getInstance(context).cancelAllWorkByTag("task_reminder_all")
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        updatePreferences { it.copy(theme = theme) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            taskRepository.deleteAll()
            sessionRepository.deleteAll()
            goalRepository.deleteAll()
        }
    }
}
