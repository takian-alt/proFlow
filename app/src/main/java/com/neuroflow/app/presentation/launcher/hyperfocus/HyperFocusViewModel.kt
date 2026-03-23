package com.neuroflow.app.presentation.launcher.hyperfocus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.presentation.launcher.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.presentation.launcher.hyperfocus.domain.RewardEngine
import com.neuroflow.app.presentation.launcher.hyperfocus.domain.UnlockCodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HyperFocusProgress(
    val completedSinceActivation: Int,
    val totalTarget: Int,
    val currentTier: RewardTier,
    val fraction: Float,
    val tasksToNextTier: Int
)

@HiltViewModel
class HyperFocusViewModel @Inject constructor(
    private val hyperFocusManager: HyperFocusManager,
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val unlockCodeRepository: UnlockCodeRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    val hyperFocusPrefs: StateFlow<HyperFocusPreferences> = hyperFocusDataStore.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HyperFocusPreferences())

    val progress: StateFlow<HyperFocusProgress> = hyperFocusDataStore.flow
        .flatMapLatest { prefs ->
            flow {
                val totalCompleted = taskRepository.getAllTasks().count { it.status == TaskStatus.COMPLETED }
                val completedSinceActivation = totalCompleted - prefs.tasksCompletedAtActivation
                val fraction = completedSinceActivation.toFloat() / prefs.dailyTaskTarget.coerceAtLeast(1)
                val currentTier = RewardEngine.computeTier(completedSinceActivation, prefs.dailyTaskTarget)
                val tasksToNextTier = RewardEngine.tasksToNextTier(completedSinceActivation, prefs.dailyTaskTarget)
                emit(
                    HyperFocusProgress(
                        completedSinceActivation = completedSinceActivation,
                        totalTarget = prefs.dailyTaskTarget,
                        currentTier = currentTier,
                        fraction = fraction.coerceIn(0f, 1f),
                        tasksToNextTier = tasksToNextTier
                    )
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HyperFocusProgress(0, 0, RewardTier.NONE, 0f, 1)
        )

    val isUnlockActive: StateFlow<Boolean> = hyperFocusDataStore.flow
        .map { RewardEngine.isUnlockActive(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val tickerFlow = flow {
        while (true) {
            emit(Unit)
            delay(1000)
        }
    }

    val unlockSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        RewardEngine.secondsRemaining(prefs)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val claimedCodeToShow: MutableStateFlow<String?> = MutableStateFlow(null)

    // Emits the result of the last submitCode call so the UI can react
    val submitCodeResult: MutableStateFlow<com.neuroflow.app.presentation.launcher.hyperfocus.domain.UnlockResult?> =
        MutableStateFlow(null)

    val lockoutSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        val expiresAt = prefs.lockoutExpiresAt ?: return@combine null
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000
        if (remaining > 0) remaining else null
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun activate(blockedPackages: Set<String>) {
        viewModelScope.launch {
            val dailyTaskTarget = taskRepository.getActiveTasks().size
            hyperFocusManager.activate(blockedPackages, dailyTaskTarget)
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            val current = progress.value
            // Only allow deactivation when the daily task target has been met
            if (current.completedSinceActivation >= current.totalTarget) {
                hyperFocusManager.deactivate()
            }
        }
    }

    fun claimReward() {
        viewModelScope.launch {
            val prefs = hyperFocusDataStore.current()
            val sessionId = prefs.sessionId ?: return@launch
            // Use the live-computed tier from progress, not the potentially-stale prefs.currentTier
            val liveTier = progress.value.currentTier
            val code = unlockCodeRepository.getClaimableCode(sessionId, liveTier)
            claimedCodeToShow.value = code
        }
    }

    fun dismissClaimedCode() {
        claimedCodeToShow.value = null
    }

    fun submitCode(entered: String) {
        viewModelScope.launch {
            val result = hyperFocusManager.submitCode(entered)
            submitCodeResult.value = result
        }
    }

    fun clearSubmitCodeResult() {
        submitCodeResult.value = null
    }

    fun completePlanning(tomorrowTaskTitles: List<String>) {
        viewModelScope.launch {
            hyperFocusManager.completePlanning()
        }
    }
}
