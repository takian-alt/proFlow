package com.neuroflow.app.presentation.v2.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.scheduler.BehavioralInsightsEngine
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class InsightsMetricsUiModel(
    val velocity7d: Int = 0,
    val velocity30d: Int = 0,
    val focusHours7d: Float = 0f,
    val focusHours30d: Float = 0f,
    val weightedSmape: Float = 0f,
    val smapeGrade: String = "Calibrating",
    val hourlyDensity: List<Float> = emptyList()
)

data class InsightsUiStateV2(
    val energyUi: EnergyUiModel? = null,
    val analyticsSummary: AnalyticsEngine.AnalyticsSummary? = null,
    val planningCalibrationFactor: Float = 1.0f,
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
    private val preferencesDataStore: UserPreferencesDataStore,
    val contractRepository: UlyssesContractRepository,
    val workManager: WorkManager? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val geminiAiOnlineService: com.neuroflow.app.domain.service.GeminiAiOnlineService = com.neuroflow.app.domain.service.DefaultGeminiAiOnlineService()
) : ViewModel() {

    private val _isCalibratingCoach = MutableStateFlow(false)
    val isCalibratingCoach: StateFlow<Boolean> = _isCalibratingCoach.asStateFlow()

    fun calibrateAiCoach() {
        viewModelScope.launch {
            _isCalibratingCoach.value = true
            try {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val allTasks = taskRepository.getAllTasks()
                val metrics = computeSmapeAndCircadianDensity(allTasks, prefs)

                val telemetryJson = org.json.JSONObject().apply {
                    put("completionRatePercent", if (allTasks.isNotEmpty()) (allTasks.count { it.completedAt != null } * 100 / allTasks.size) else 0)
                    put("weightedSmape", metrics.weightedSmape)
                    put("velocity7d", metrics.velocity7d)
                    put("focusHours7d", metrics.focusHours7d)
                    put("circadianPeakStart", prefs.peakEnergyStart)
                    put("circadianPeakEnd", prefs.peakEnergyEnd)
                }.toString()

                val deletedJson = prefs.recentlyDeletedTasksJson.ifBlank { "[]" }
                val postponedTasks = allTasks.filter { it.postponeCount >= 2 && it.status.name == "ACTIVE" }.take(10)
                val postponedJson = org.json.JSONArray().apply {
                    postponedTasks.forEach { t ->
                        put(org.json.JSONObject().apply {
                            put("title", t.title)
                            put("estimatedMinutes", t.estimatedDurationMinutes)
                            put("category", t.taskType.name)
                            put("postponeCount", t.postponeCount)
                        })
                    }
                }.toString()

                val result = geminiAiOnlineService.conductWeeklyAuditAndDirective(
                    weeklyTelemetryJson = telemetryJson,
                    recentlyDeletedTasksJson = deletedJson,
                    chronicallyPostponedTasksJson = postponedJson,
                    apiKey = prefs.geminiApiKey,
                    useAi = prefs.geminiAiEnabled
                )

                preferencesDataStore.updateAdaptiveSystemDirective(
                    directive = result.newSystemDirective,
                    auditJson = result.avoidanceAuditJson
                )
            } catch (e: Exception) {
                android.util.Log.e("InsightsViewModelV2", "Failed to calibrate AI coach", e)
            } finally {
                _isCalibratingCoach.value = false
            }
        }
    }

    val uiState: StateFlow<InsightsUiStateV2> = combine(
        taskRepository.observeAll(),
        sessionRepository.observeAll(),
        preferencesDataStore.preferencesFlow,
        contractRepository.observeActive(),
        sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())
    ) { allTasks, allSessions, prefs, activeContracts, sleepLogs ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
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
        val peakStart = prefs.peakEnergyStart.coerceIn(0, 23)
        val peakEnd = prefs.peakEnergyEnd.coerceIn(0, 23)
        val highEnergyTasks = allTasks.filter { it.energyLevel == EnergyLevel.HIGH }
        val evaluatedTasks = highEnergyTasks.filter {
            (it.scheduledTime != null && it.resolveScheduledEpochMillis() != null) || it.completedAt != null
        }
        val alignedHighEnergy = evaluatedTasks.count { task ->
            val timestamp = if (task.completedAt != null) {
                task.completedAt
            } else if (task.scheduledTime != null) {
                task.resolveScheduledEpochMillis()
            } else null
            val taskHour = timestamp?.let {
                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).hour
            }
            when {
                taskHour == null -> false
                peakStart <= peakEnd -> taskHour in peakStart until peakEnd
                else -> taskHour >= peakStart || taskHour < peakEnd
            }
        }
        val alignmentRate = if (evaluatedTasks.isNotEmpty()) {
            ((alignedHighEnergy.toFloat() / evaluatedTasks.size) * 100).toInt().coerceIn(40, 100)
        } else 85

        val totalMinutes = todaySessions.sumOf { it.trackedMinutes(now).toDouble() }
        val penaltyMinutes = (pauses * 0.5 + appSwitches * 0.75)
        val efficiency = if (totalMinutes > 0.0) {
            (((totalMinutes - penaltyMinutes).coerceAtLeast(0.0) / totalMinutes) * 100).toInt().coerceIn(30, 99)
        } else 95

        // Calculate continuous Borbély circadian snapshot & energyUi
        val (_, energyUi) = computeEnergyUiState(
            circadianEnergyService = circadianEnergyService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            prefs = prefs,
            sleepLogs = sleepLogs,
            nowMillis = now
        )

        // Compute full AnalyticsSummary with real sessions off the main thread
        val analyticsSummary = AnalyticsEngine.buildSummary(
            allTasks = allTasks,
            allSessions = allSessions,
            prefs = prefs
        )
        val planningCalibrationFactor = AnalyticsEngine.PlanningCalibration.from(allTasks).globalFactor

        // Behavioral interventions
        val interventions = mutableListOf<String>()
        val pressure = energyUi.sleepPressurePoints
        val zone = energyUi.fatigueZone

        when (zone) {
            FatigueZone.CRITICAL, FatigueZone.HIGH -> {
                interventions.add("⚠️ Process S Sleep Pressure is critical ($pressure pts). Adenosine accumulation is impairing working memory. Prioritize sleep or 20m Non-Sleep Deep Rest (NSDR).")
            }
            FatigueZone.MODERATE -> {
                interventions.add("☕ Moderate cognitive fatigue detected. Shift to structured low-effort tasks or take a 10m movement break.")
            }
            FatigueZone.RESTED -> {
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
            analyticsSummary = analyticsSummary,
            planningCalibrationFactor = planningCalibrationFactor,
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
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            InsightsUiStateV2()
        )

    val insightsMetricsFlow: StateFlow<InsightsMetricsUiModel> = combine(
        taskRepository.observeAll(),
        preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
            old.peakEnergyStart == new.peakEnergyStart &&
                old.peakEnergyEnd == new.peakEnergyEnd
        }
    ) { allTasks, prefs ->
        computeSmapeAndCircadianDensity(allTasks, prefs)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            InsightsMetricsUiModel()
        )

    fun computeSmapeAndCircadianDensity(
        allTasks: List<TaskEntity>,
        prefs: UserPreferences,
        now: Long = System.currentTimeMillis()
    ): InsightsMetricsUiModel {
        val sevenDaysAgo = now - 7L * 86400_000L
        val thirtyDaysAgo = now - 30L * 86400_000L

        val completed7d = allTasks.filter { it.completedAt != null && it.completedAt >= sevenDaysAgo }
        val completed30d = allTasks.filter { it.completedAt != null && it.completedAt >= thirtyDaysAgo }

        val velocity7d = completed7d.size
        val velocity30d = completed30d.size
        val focusHours7d = completed7d.sumOf { (it.actualDurationMinutes ?: it.totalTimeTrackedMinutes).toDouble() }.toFloat() / 60f
        val focusHours30d = completed30d.sumOf { (it.actualDurationMinutes ?: it.totalTimeTrackedMinutes).toDouble() }.toFloat() / 60f

        val weightedSmape = AnalyticsEngine.computeWeightedSmape(allTasks)
        val smapeGrade = AnalyticsEngine.mapeGrade(weightedSmape)

        val map = mutableMapOf<Int, Int>()
        allTasks.forEach { task ->
            val hour = when {
                task.completedAt != null -> {
                    Calendar.getInstance().apply { timeInMillis = task.completedAt }.get(Calendar.HOUR_OF_DAY)
                }
                task.scheduledTime != null -> {
                    val epoch = task.resolveScheduledEpochMillis()
                    epoch?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).hour
                    } ?: ((task.scheduledTime / 3600_000L) % 24).toInt()
                }
                else -> {
                    Calendar.getInstance().apply { timeInMillis = task.updatedAt }.get(Calendar.HOUR_OF_DAY)
                }
            }
            val mins = (task.actualDurationMinutes ?: task.totalTimeTrackedMinutes).toInt()
            if (mins > 0) {
                map[hour] = (map[hour] ?: 0) + mins
            }
        }
        if (map.isEmpty()) {
            val pStart = prefs.peakEnergyStart.coerceIn(0, 23)
            val pEnd = prefs.peakEnergyEnd.coerceIn(0, 23)
            if (pStart <= pEnd) {
                for (h in pStart until pEnd) {
                    map[h] = 45
                }
            } else {
                for (h in pStart..23) map[h] = 45
                for (h in 0 until pEnd) map[h] = 45
            }
        }
        val hourlyDensity = BehavioralInsightsEngine.calculateHourlyCircadianDensity(map)

        return InsightsMetricsUiModel(
            velocity7d = velocity7d,
            velocity30d = velocity30d,
            focusHours7d = focusHours7d,
            focusHours30d = focusHours30d,
            weightedSmape = weightedSmape,
            smapeGrade = smapeGrade,
            hourlyDensity = hourlyDensity
        )
    }
}
