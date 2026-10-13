package com.neuroflow.app.presentation.focus

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.WoopEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.WoopRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.engine.WoopEngine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class FocusWoopManager @Inject constructor(
    private val woopRepository: WoopRepository,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) {

    data class WoopLoadResult(
        val showWoopPrompt: Boolean,
        val woopData: WoopEntity?,
        val dreadedTaskInsight: String?,
        val affectiveForecastError: Float?
    )

    suspend fun load(taskId: String): WoopLoadResult {
        val woopData = woopRepository.getByTaskId(taskId)
        val task = taskRepository.getById(taskId)
        val prefs = preferencesDataStore.preferencesFlow.first()
        val showWoopPrompt = prefs.woopEnabled && WoopEngine.shouldShowPrompt(woopData, task?.woopPromptShown ?: false)
        val completedTasks = taskRepository.getCompletedTasks()
        val dreadedTaskInsight = WoopEngine.dreadedTaskInsight(completedTasks)
        syncEnjoymentForecastBias(completedTasks, prefs)

        return WoopLoadResult(
            showWoopPrompt = showWoopPrompt,
            woopData = woopData,
            dreadedTaskInsight = dreadedTaskInsight,
            affectiveForecastError = task?.affectiveForecastError
        )
    }

    /**
     * Ratings are only ever displayed by WOOP; task priority reads the learned correction from
     * preferences. Re-derived whenever the focus screen opens so a user's existing ratings count
     * before their next rating arrives.
     */
    private suspend fun syncEnjoymentForecastBias(
        completedTasks: List<TaskEntity>,
        prefs: UserPreferences
    ) {
        val bias = AnalyticsEngine.EnjoymentCalibration.from(completedTasks).offsetPoints
        if (abs(bias - prefs.enjoymentForecastBias) >= 0.5f) {
            preferencesDataStore.updatePreferences { it.copy(enjoymentForecastBias = bias) }
        }
    }

    suspend fun submit(taskId: String, wish: String, outcome: String, obstacle: String, plan: String): WoopEntity? {
        val woop = WoopEntity(taskId = taskId, wish = wish, outcome = outcome, obstacle = obstacle, plan = plan)
        woopRepository.upsert(woop)
        val task = taskRepository.getById(taskId) ?: return null
        taskRepository.update(task.copy(woopPromptShown = true, updatedAt = System.currentTimeMillis()))
        return woop
    }

    suspend fun dismiss(taskId: String): Boolean {
        val task = taskRepository.getById(taskId) ?: return false
        taskRepository.update(task.copy(woopPromptShown = true, updatedAt = System.currentTimeMillis()))
        return true
    }

    suspend fun submitAffordanceRating(taskId: String, rating: Float): Boolean {
        val task = taskRepository.getById(taskId) ?: return false
        taskRepository.update(task.copy(affectiveForecastError = rating, updatedAt = System.currentTimeMillis()))
        syncEnjoymentForecastBias(
            completedTasks = taskRepository.getCompletedTasks(),
            prefs = preferencesDataStore.preferencesFlow.first()
        )
        return true
    }
}
