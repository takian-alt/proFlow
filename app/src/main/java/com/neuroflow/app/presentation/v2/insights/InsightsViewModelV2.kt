package com.neuroflow.app.presentation.v2.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.domain.engine.SleepPressureDetector
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.repository.EnergyScoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

import androidx.work.WorkManager

data class InsightsUiStateV2(
    val energyUi: EnergyScoreRepository.EnergyUiModel? = null,
    val preferences: UserPreferences = UserPreferences(),
    val circadianAlignmentRate: Int = 85,
    val interruptionPauseResumeCount: Int = 0,
    val interruptionAppSwitchCount: Int = 0,
    val interruptionBurstCount: Int = 0,
    val focusEfficiencyPercent: Int = 92,
    val dailyStreak: Int = 0,
    val frogsEatenCount: Int = 0,
    val totalFocusMinutesToday: Int = 0,
    val totalFocusMinutesWeek: Int = 0,
    val totalTasksCompletedToday: Int = 0,
    val activeContractsCount: Int = 0,
    val neuroscienceInterventions: List<String> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class InsightsViewModelV2 @Inject constructor(
    val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val energyScoreRepository: EnergyScoreRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    val contractRepository: UlyssesContractRepository,
    val workManager: WorkManager? = null
) : ViewModel() {

    val uiState: StateFlow<InsightsUiStateV2> = combine(
        taskRepository.observeAll(),
        sessionRepository.observeAll(),
        energyScoreRepository.observeEnergy(),
        preferencesDataStore.preferencesFlow,
        contractRepository.observeActive()
    ) { allTasks, allSessions, energyUi, prefs, activeContracts ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        val startOfWeek = startOfToday - 6 * 86400_000L

        val todaySessions = allSessions.filter { it.startedAt >= startOfToday || (it.endedAt != null && it.endedAt!! >= startOfToday) }
        val weekSessions = allSessions.filter { it.startedAt >= startOfWeek }

        val todayFocusMinutes = todaySessions.sumOf { it.trackedMinutes(now).toInt() }
        val weekFocusMinutes = weekSessions.sumOf { it.trackedMinutes(now).toInt() }

        val pauses = todaySessions.sumOf { it.pauseResumeCount }
        val appSwitches = todaySessions.sumOf { it.appSwitchCount }
        val bursts = todaySessions.sumOf { it.interruptionBurstCount }

        val completedToday = allTasks.filter {
            it.completedAt != null && it.completedAt >= startOfToday
        }
        val frogsToday = completedToday.count { it.isFrog }

        // Circadian alignment: % of high energy tasks executed or completed during peak window
        val peakStart = prefs.peakEnergyStart.takeIf { it > 0 } ?: 9
        val peakEnd = prefs.peakEnergyEnd.takeIf { it > 0 } ?: 12
        val highEnergyTasks = allTasks.filter { it.energyLevel == EnergyLevel.HIGH }
        val alignedHighEnergy = highEnergyTasks.count { task ->
            val scheduledH = task.scheduledTime?.let { (it / 3600_000L).toInt() }
            scheduledH == null || scheduledH in peakStart until peakEnd
        }
        val alignmentRate = if (highEnergyTasks.isNotEmpty()) {
            ((alignedHighEnergy.toFloat() / highEnergyTasks.size) * 100).toInt().coerceIn(40, 100)
        } else 85

        val totalMinutes = todaySessions.sumOf { it.trackedMinutes(now).toDouble() }
        val penaltyMinutes = (pauses * 0.5 + appSwitches * 0.75)
        val efficiency = if (totalMinutes > 0.0) {
            (((totalMinutes - penaltyMinutes).coerceAtLeast(0.0) / totalMinutes) * 100).toInt().coerceIn(30, 99)
        } else 95

        // Behavioral interventions
        val interventions = mutableListOf<String>()
        val pressure = energyUi?.sleepPressurePoints ?: 400
        val zone = energyUi?.fatigueZone ?: SleepPressureDetector.FatigueZone.RESTED

        when (zone) {
            SleepPressureDetector.FatigueZone.CRITICAL, SleepPressureDetector.FatigueZone.HIGH -> {
                interventions.add("⚠️ Process S Sleep Pressure is critical ($pressure pts). Adenosine accumulation is impairing working memory. Prioritize sleep or 20m Non-Sleep Deep Rest (NSDR).")
            }
            SleepPressureDetector.FatigueZone.MODERATE -> {
                interventions.add("☕ Moderate cognitive fatigue detected. Shift to structured low-effort tasks or take a 10m movement break.")
            }
            SleepPressureDetector.FatigueZone.RESTED -> {
                interventions.add("⚡ High neurochemical capacity available. Excellent window for deep focus, creative breakthroughs, and Frogs.")
            }
        }

        if (appSwitches > 5) {
            interventions.add("📱 Context-switching penalty: $appSwitches app switches today. Consider parking intrusive thoughts in the Thought Parking Lot to maintain flow state.")
        }

        if (frogsToday > 0) {
            interventions.add("🐸 Frogs conquered today: $frogsToday! Dopamine pathways are primed for sustained momentum.")
        }

        InsightsUiStateV2(
            energyUi = energyUi,
            preferences = prefs,
            circadianAlignmentRate = alignmentRate,
            interruptionPauseResumeCount = pauses,
            interruptionAppSwitchCount = appSwitches,
            interruptionBurstCount = bursts,
            focusEfficiencyPercent = efficiency,
            dailyStreak = prefs.dailyStreak,
            frogsEatenCount = frogsToday,
            totalFocusMinutesToday = todayFocusMinutes,
            totalFocusMinutesWeek = weekFocusMinutes,
            totalTasksCompletedToday = completedToday.size,
            activeContractsCount = activeContracts.size,
            neuroscienceInterventions = interventions,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        InsightsUiStateV2()
    )
}
