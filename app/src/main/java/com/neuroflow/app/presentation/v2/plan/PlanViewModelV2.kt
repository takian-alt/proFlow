package com.neuroflow.app.presentation.v2.plan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.QuickNoteSynthesis
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.roundToInt
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.QuickNoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.DeferralRationale
import com.neuroflow.app.domain.scheduler.ScheduleConflict
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.scheduler.determineCategory
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.worker.ScheduleAutoTasksWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject

enum class PlanViewModeV2(val title: String) {
    TIMELINE("Timeline"),
    MATRIX("Matrix"),
    AUTOPILOT("AI Autopilot")
}

data class PlanUiStateV2(
    val selectedDateMillis: Long = startOfTodayMillis(),
    val viewMode: PlanViewModeV2 = PlanViewModeV2.TIMELINE,
    val tasksForDay: List<TaskEntity> = emptyList(),
    val unscheduledTasks: List<TaskEntity> = emptyList(),
    val tasksByQuadrant: Map<Quadrant, List<TaskEntity>> = emptyMap(),
    val activeGoals: List<com.neuroflow.app.data.local.entity.GoalEntity> = emptyList(),
    val protectedSlots: List<ProtectedTimeSlot> = emptyList(),
    val conflicts: List<ScheduleConflict> = emptyList(),
    val isAutoScheduleOn: Boolean = false,
    val isAutoScheduling: Boolean = false,
    val selectedTag: String? = null,
    val availableTags: List<String> = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin"),
    val workDayStart: Int = 8,
    val workDayEnd: Int = 20,
    val energyUi: EnergyUiModel? = null,
    val circadianSummary: DailyCircadianSummary? = null,
    val energyTimeline: List<CircadianSnapshot> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val pendingDeletedTask: TaskEntity? = null,
    val isStagingDrawerExpanded: Boolean = false,
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val decomposingTaskId: String? = null,
    val selectedQuadrant: Quadrant? = null,
    val deferrals: List<DeferralRationale> = emptyList()
)

private fun startOfTodayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@HiltViewModel
class PlanViewModelV2 @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val workManager: WorkManager,
    private val noteRepository: NoteRepository? = null,
    private val sessionRepository: SessionRepository? = null,
    private val quickNoteDao: QuickNoteDao? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val taskSchedulerService: TaskSchedulerService = DefaultTaskSchedulerService(circadianEnergyService),
    private val behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService(),
    private val calendarSyncRepository: com.neuroflow.app.data.calendar.CalendarSyncRepository? = null,
    private val goalRepository: com.neuroflow.app.data.repository.GoalRepository? = null,
    @ApplicationContext private val context: Context? = null,
    private val geminiAiOnlineService: GeminiAiOnlineService? = null
) : ViewModel() {

    private val scheduleOrchestrator = SchedulePreparationOrchestrator(
        taskSchedulerService = taskSchedulerService,
        behavioralSupportService = behavioralSupportService,
        sleepTelemetryRepository = sleepTelemetryRepository
    )

    private val _selectedDate = MutableStateFlow(startOfTodayMillis())
    private val _viewMode = MutableStateFlow(PlanViewModeV2.TIMELINE)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _isAutoScheduling = MutableStateFlow(false)
    private val _pendingDeletedTask = MutableStateFlow<TaskEntity?>(null)
    private val _isStagingDrawerExpanded = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchVisible = MutableStateFlow(false)
    private val _decomposingTaskId = MutableStateFlow<String?>(null)
    private val _selectedQuadrant = MutableStateFlow<Quadrant?>(null)
    private val _deferrals = MutableStateFlow<List<DeferralRationale>>(emptyList())

    private val _userActionMessage = MutableSharedFlow<String>()
    val userActionMessage: SharedFlow<String> = _userActionMessage.asSharedFlow()

    val recentNotes: Flow<List<NoteEntity>> = noteRepository?.allNotes ?: flowOf(emptyList())

    val completedTasks: StateFlow<List<TaskEntity>> = taskRepository.observeCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class PlanNavParams(
        val dateMillis: Long,
        val mode: PlanViewModeV2,
        val autoScheduling: Boolean,
        val tag: String?,
        val pendingDeletedTask: TaskEntity?,
        val isStagingDrawerExpanded: Boolean,
        val searchQuery: String,
        val isSearchVisible: Boolean,
        val decomposingTaskId: String?,
        val selectedQuadrant: Quadrant?,
        val deferrals: List<DeferralRationale> = emptyList()
    )

    val uiState: StateFlow<PlanUiStateV2> = combine(
        combine(
            combine(_selectedDate, _viewMode, _isAutoScheduling, _selectedTag) { date, mode, autoScheduling, tag ->
                listOf<Any?>(date, mode, autoScheduling, tag)
            },
            combine(_pendingDeletedTask, _isStagingDrawerExpanded, _searchQuery) { pendingTask, drawerExpanded, searchQ ->
                listOf<Any?>(pendingTask, drawerExpanded, searchQ)
            },
            combine(_isSearchVisible, _decomposingTaskId, _selectedQuadrant, _deferrals) { searchVis, decompId, selectedQuad, defs ->
                listOf<Any?>(searchVis, decompId, selectedQuad, defs)
            }
        ) { first, second, third ->
            @Suppress("UNCHECKED_CAST")
            PlanNavParams(
                dateMillis = first[0] as Long,
                mode = first[1] as PlanViewModeV2,
                autoScheduling = first[2] as Boolean,
                tag = first[3] as String?,
                pendingDeletedTask = second[0] as TaskEntity?,
                isStagingDrawerExpanded = second[1] as Boolean,
                searchQuery = second[2] as String,
                isSearchVisible = third[0] as Boolean,
                decomposingTaskId = third[1] as String?,
                selectedQuadrant = third[2] as Quadrant?,
                deferrals = third[3] as List<DeferralRationale>
            )
        },
        taskRepository.observeActiveTasks(),
        preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
            old.workDayStart == new.workDayStart &&
                old.workDayEnd == new.workDayEnd &&
                old.peakEnergyStart == new.peakEnergyStart &&
                old.peakEnergyEnd == new.peakEnergyEnd &&
                old.effectivePeakStart == new.effectivePeakStart &&
                old.effectivePeakEnd == new.effectivePeakEnd &&
                old.autoSchedulingEnabled == new.autoSchedulingEnabled &&
                old.protectedTimeSlots == new.protectedTimeSlots &&
                old.tagCatalog == new.tagCatalog &&
                old.wakeUpHour == new.wakeUpHour &&
                old.sleepHour == new.sleepHour &&
                old.sleepPressurePoints == new.sleepPressurePoints &&
                old.stimulantLogsJson == new.stimulantLogsJson &&
                old.quickPhysiologicalLogsJson == new.quickPhysiologicalLogsJson &&
                old.quizChronotype == new.quizChronotype &&
                old.manualChronotype == new.manualChronotype &&
                old.meqScore == new.meqScore &&
                old.manualPeakProfileEnabled == new.manualPeakProfileEnabled &&
                old.manualPeakAnchorMinuteOfDay == new.manualPeakAnchorMinuteOfDay &&
                old.dailyDeepWorkTargetMinutes == new.dailyDeepWorkTargetMinutes
        },
        sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList()),
        goalRepository?.observeActiveGoals() ?: flowOf(emptyList())
    ) { params, allTasks, prefs, sleepLogs, activeGoals ->
        val dateMillis = params.dateMillis
        val mode = params.mode
        val autoScheduling = params.autoScheduling
        val tag = params.tag

        val now = System.currentTimeMillis()
        val zoneId = java.time.ZoneId.systemDefault()
        val targetLocalDate = java.time.Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate()
        val nowLocalDate = java.time.Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        var anchor = targetLocalDate.atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
        val anchorMillis = anchor.toInstant().toEpochMilli()
        if (targetLocalDate == nowLocalDate && anchorMillis > now) {
            if (anchorMillis - now > 2 * 3600_000L) {
                anchor = anchor.minusDays(1)
            }
        }
        val baseWakeMillis = anchor.toInstant().toEpochMilli()

        val signal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )
        val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480

        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
        val quickPhysNaps = physLogs
            .filter { it.type == com.neuroflow.app.data.local.PhysiologicalEventType.NAP }
            .map { log ->
                val napDuration = log.durationMinutes.coerceIn(5, 180)
                DaytimeNapBout(
                    startMillis = log.timestampMillis,
                    endMillis = log.timestampMillis + napDuration * 60_000L
                )
            }
        val naps = ((signal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }

        val resolvedChronotype = if (!prefs.manualChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.manualChronotype)
        } else if (!prefs.quizChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.quizChronotype)
        } else if (prefs.meqScore > 0) {
            when {
                prefs.meqScore >= 70 -> ChronotypeProfile.DEFINITE_MORNING
                prefs.meqScore >= 59 -> ChronotypeProfile.MODERATE_MORNING
                prefs.meqScore >= 42 -> ChronotypeProfile.INTERMEDIATE
                prefs.meqScore >= 31 -> ChronotypeProfile.MODERATE_EVENING
                else -> ChronotypeProfile.DEFINITE_EVENING
            }
        } else {
            ChronotypeProfile.INTERMEDIATE
        }

        val allStimulants = parseStimulantLogs(prefs.stimulantLogsJson)

        val (_, energyUi) = computeEnergyUiState(
            circadianEnergyService = circadianEnergyService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            prefs = prefs,
            sleepLogs = sleepLogs,
            nowMillis = now,
            stimulantDoses = allStimulants
        )

        val baseTasks = if (params.pendingDeletedTask != null) {
            allTasks.filter { it.id != params.pendingDeletedTask.id }
        } else {
            allTasks
        }

        val defaultTags = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin")
        val dynamicTags = (prefs.tagCatalog + baseTasks.flatMap { task ->
            (task.tags.split(",") + task.contextTag)
        })
            .map { it.trim().removePrefix("#").removePrefix("@") }
            .filter { it.isNotBlank() }
            .map { "#$it" }
        val mergedTags = (defaultTags + dynamicTags).distinctBy { it.lowercase() }

        val tagFilteredAll = if (tag.isNullOrBlank() || tag.equals("All", ignoreCase = true)) {
            baseTasks
        } else {
            val q = tag.removePrefix("#").removePrefix("@").trim()
            val wordRegex = Regex("""(?i)(?:^|\s)[#@]?\Q$q\E(?=\s|$)""")
            baseTasks.filter { task ->
                val hasMatchingTag = (task.tags.split(",") + task.contextTag)
                    .map { it.trim().removePrefix("#").removePrefix("@") }
                    .filter { it.isNotBlank() }
                    .any { it.equals(q, ignoreCase = true) }
                hasMatchingTag || wordRegex.containsMatchIn(task.title)
            }
        }

        val searchFilteredAll = if (params.searchQuery.isBlank()) {
            tagFilteredAll
        } else {
            val query = params.searchQuery.trim()
            tagFilteredAll.filter { task ->
                task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.tags.contains(query, ignoreCase = true)
            }
        }

        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val dayStart = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(dayStart) - 1

        val tasksForDay = searchFilteredAll.filter { task ->
            val sDate = task.effectiveScheduledDateMillis()
            if (sDate != null) {
                sDate in dayStart..dayEnd
            } else {
                val rawScheduledTime = task.scheduledTime
                if (rawScheduledTime != null && rawScheduledTime >= ONE_DAY_MILLIS) {
                    rawScheduledTime in dayStart..dayEnd
                } else {
                    val dDate = task.resolveDeadlineEpochMillis()
                    dDate != null && dDate in dayStart..dayEnd
                }
            }
        }.sortedWith(
            // Timed tasks sort chronologically first; date-only untimed tasks (scheduledTime == null) sort after (BUG-153)
            compareBy<TaskEntity> { it.scheduledTime == null }
                .thenBy { if (it.scheduledTime != null) it.timelineStartMinuteOfDay() else Int.MAX_VALUE }
                .thenByDescending { it.isFrog }
                .thenByDescending { it.createdAt }
        )

        val unscheduled = searchFilteredAll.filter { task ->
            task.effectiveScheduledDateMillis() == null &&
                (task.scheduledTime == null || task.scheduledTime < ONE_DAY_MILLIS)
        }

        val byQuadrant = Quadrant.entries.associateWith { q ->
            searchFilteredAll.filter { it.quadrant == q }
        }

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = tasksForDay,
            protectedSlots = prefs.protectedTimeSlots,
            workDayStart = prefs.workDayStart,
            workDayEnd = prefs.workDayEnd,
            dayMillis = dateMillis
        )

        val manualPeakHour = if (prefs.manualPeakProfileEnabled) (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
        val circadianSummary = runCatching {
            circadianEnergyService.computeDailySummary(
                timestampMillis = dateMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = naps,
                chronotype = resolvedChronotype,
                manualPeakStartHour = manualPeakHour,
                stimulantDoses = allStimulants
            )
        }.getOrNull()

        val energyTimeline = runCatching {
            circadianEnergyService.projectDayTimeline(
                dayStartMillis = dateMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                stepMinutes = 30,
                zoneId = zoneId,
                naps = naps,
                chronotype = resolvedChronotype,
                manualPeakStartHour = manualPeakHour,
                stimulantDoses = allStimulants
            )
        }.getOrDefault(emptyList())

        PlanUiStateV2(
            selectedDateMillis = dateMillis,
            viewMode = mode,
            tasksForDay = tasksForDay,
            unscheduledTasks = unscheduled,
            tasksByQuadrant = byQuadrant,
            activeGoals = activeGoals,
            protectedSlots = prefs.protectedTimeSlots,
            conflicts = conflicts,
            isAutoScheduleOn = prefs.autoSchedulingEnabled,
            isAutoScheduling = autoScheduling,
            selectedTag = tag,
            availableTags = mergedTags,
            workDayStart = prefs.workDayStart,
            workDayEnd = prefs.workDayEnd,
            energyUi = energyUi,
            circadianSummary = circadianSummary,
            energyTimeline = energyTimeline,
            preferences = prefs,
            isLoading = false,
            pendingDeletedTask = params.pendingDeletedTask,
            isStagingDrawerExpanded = params.isStagingDrawerExpanded,
            searchQuery = params.searchQuery,
            isSearchVisible = params.isSearchVisible,
            decomposingTaskId = params.decomposingTaskId,
            selectedQuadrant = params.selectedQuadrant,
            deferrals = params.deferrals
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlanUiStateV2()
    )

    fun setViewMode(mode: PlanViewModeV2) {
        _viewMode.value = mode
    }

    fun selectQuadrant(quadrant: Quadrant?) {
        _selectedQuadrant.value = quadrant
    }

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun toggleAutoSchedule() {
        viewModelScope.launch {
            val current = uiState.value.isAutoScheduleOn
            val nextState = !current
            preferencesDataStore.updatePreferences {
                it.copy(
                    autoSchedulingEnabled = nextState,
                    isAutoScheduleToggleOn = nextState
                )
            }
        }
    }

    fun autoScheduleDay(dateMillis: Long = _selectedDate.value, forceReplan: Boolean = false): kotlinx.coroutines.Job {
        if (_isAutoScheduling.value) return kotlinx.coroutines.Job().apply { complete() }
        return viewModelScope.launch {
            _isAutoScheduling.value = true
            try {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val allKnownTasks = taskRepository.getAllTasks().ifEmpty { taskRepository.getActiveTasks() }
                val allTasks = taskRepository.getActiveTasks()
                val zoneId = java.time.ZoneId.systemDefault()
                val dayDate = java.time.Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate()
                val dayStart = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(dayStart) - 1
                val nowMillis = System.currentTimeMillis()

                val allSessions = runCatching {
                    sessionRepository?.getAllSessions().orEmpty()
                }.getOrDefault(emptyList())
                val closedSessions = allSessions.filter { it.endedAt != null }
                val focusedTaskIds = sessionRepository?.getOpenSessions().orEmpty().map { it.taskId }.toSet()
                val activeTaskId = allSessions.firstOrNull { it.endedAt == null }?.taskId

                val nowLocalDate = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
                val isTargetingToday = dayDate == nowLocalDate
                val candidateTasks = allTasks.filter { task ->
                    val effectiveDate = task.effectiveScheduledDateMillis()
                    val isDateEligible = effectiveDate == null ||
                        effectiveDate in dayStart..dayEnd ||
                        (isTargetingToday && effectiveDate <= dayEnd)
                    AutoSchedulingContracts.isMutableByAutoScheduler(task, nowMillis) &&
                        task.id !in focusedTaskIds &&
                        !task.id.endsWith("_part2") &&
                        isDateEligible
                }
                val candidateIds = candidateTasks.map { it.id }.toSet()

                val externalBusyWindows = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd) ?: emptyList()
                val fixedWindows = allTasks
                    .filter { it.quadrant != com.neuroflow.app.domain.model.Quadrant.ELIMINATE }
                    .filterNot { task ->
                        if (task.id in candidateIds) return@filterNot true
                        if (task.id.endsWith("_part2")) {
                            val parentId = task.id.removeSuffix("_part2")
                            parentId in candidateIds
                        } else {
                            false
                        }
                    }
                    .mapNotNull { task ->
                        if (task.scheduledTime == null) return@mapNotNull null
                        val sDate = task.effectiveScheduledDateMillis()
                        val isDualEpoch = task.scheduledTime >= ONE_DAY_MILLIS
                        // Unscheduled backlog tasks (sDate == null and not dual-epoch on this day) must not block daily slots
                        val isDateMatching = (sDate != null && sDate in dayStart..dayEnd) ||
                            (isDualEpoch && task.scheduledTime in dayStart..dayEnd)
                        if (!isDateMatching) return@mapNotNull null
                        val start = task.resolveScheduledEpochMillis(dateMillis) ?: return@mapNotNull null
                        val duration = TaskBlockPlan.plannedMinutes(task)
                        val end = start + duration * 60_000L
                        if (end > dayStart && start < dayEnd) com.neuroflow.app.domain.service.TimeSlotWindow(start, end) else null
                    }
                val totalBusyWindows = externalBusyWindows + fixedWindows

                val microStepCounts = candidateTasks.associate { task ->
                    val steps = taskRepository.getMicroSteps(task.id)
                    task.id to Pair(steps.count { it.isCompleted }, steps.size)
                }
                val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.first() ?: emptyList()

                val outcome = scheduleOrchestrator.orchestrateDayScheduleWithOutcome(
                    candidateTasks = candidateTasks,
                    allKnownTasks = allKnownTasks,
                    closedSessions = closedSessions,
                    sleepLogs = sleepLogs,
                    prefs = prefs,
                    dayDate = dayDate,
                    nowMillis = nowMillis,
                    busyWindows = totalBusyWindows,
                    forceReplan = forceReplan,
                    zoneId = zoneId,
                    activeTaskId = activeTaskId,
                    microStepCounts = microStepCounts
                )
                val placements = outcome.placements
                _deferrals.value = outcome.deferrals

                val placementsByBaseId = placements.groupBy { it.task.id.substringBefore("#part") }

                for ((baseTaskId, taskPlacements) in placementsByBaseId) {
                    val primaryPlacement = taskPlacements.first()
                    val fresh = taskRepository.getById(baseTaskId) ?: continue
                    if (fresh.status != com.neuroflow.app.domain.model.TaskStatus.ACTIVE ||
                        fresh.id in focusedTaskIds ||
                        !AutoSchedulingContracts.isMutableByAutoScheduler(fresh, nowMillis)
                    ) continue
                    val startOfDay = startOfLocalDay(primaryPlacement.window.startMillis)
                    val timeOffset = primaryPlacement.window.startMillis - startOfDay

                    val cleanTitle = fresh.title.replace(Regex("""\s*\(Part \d+/\d+\)"""), "").trim()
                    val isSplit = taskPlacements.size > 1 || primaryPlacement.task.id.endsWith("#part1")
                    val titleToUse = if (isSplit) "$cleanTitle (Part 1/2)" else cleanTitle
                    val updated = fresh.copy(
                        title = titleToUse,
                        scheduledDate = startOfDay,
                        scheduledTime = timeOffset,
                        habitDate = if (fresh.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) startOfDay else fresh.habitDate,
                        isAutoScheduled = true,
                        plannedBlockMinutes = primaryPlacement.window.durationMinutes,
                        lastAutoScheduledAt = nowMillis,
                        updatedAt = nowMillis
                    )
                    taskRepository.update(updated)
                    if (updated.isFrog) {
                        calendarSyncRepository?.upsertTaskEvent(
                            updated,
                            startMillis = primaryPlacement.window.startMillis,
                            durationMinutes = primaryPlacement.window.durationMinutes
                        )
                    }
                    context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }

                    val part2Id = "${baseTaskId}_part2"
                    val part2Placement = taskPlacements.getOrNull(1)
                    val existingPart2 = taskRepository.getById(part2Id)
                    if (isSplit && part2Placement != null && part2Placement.task.id.endsWith("#part2") &&
                        existingPart2?.status != com.neuroflow.app.domain.model.TaskStatus.COMPLETED &&
                        existingPart2?.id !in focusedTaskIds
                    ) {
                        val startOfDay2 = startOfLocalDay(part2Placement.window.startMillis)
                        val timeOffset2 = part2Placement.window.startMillis - startOfDay2
                        val part2Title = "$cleanTitle (Part 2/2)"
                        val part2Entity = (existingPart2 ?: fresh).copy(
                            id = part2Id,
                            title = part2Title,
                            parentTaskId = baseTaskId,
                            dependsOnTaskIds = baseTaskId,
                            estimatedDurationMinutes = part2Placement.window.durationMinutes,
                            plannedBlockMinutes = part2Placement.window.durationMinutes,
                            scheduledDate = startOfDay2,
                            scheduledTime = timeOffset2,
                            isAutoScheduled = true,
                            isFrog = false,
                            recurrence = com.neuroflow.app.domain.model.Recurrence.NONE,
                            recurrenceIntervalDays = 0,
                            isHabitual = false,
                            sessionCount = existingPart2?.sessionCount ?: 0,
                            totalTimeTrackedMinutes = existingPart2?.totalTimeTrackedMinutes ?: 0f,
                            actualDurationMinutes = existingPart2?.actualDurationMinutes ?: 0f,
                            lastSessionDurationMinutes = existingPart2?.lastSessionDurationMinutes ?: 0f,
                            lastAutoScheduledAt = nowMillis,
                            updatedAt = nowMillis
                        )
                        if (existingPart2 != null) {
                            taskRepository.update(part2Entity)
                        } else {
                            taskRepository.insert(part2Entity)
                        }
                        context?.let { AutonomyNudgeEngine.scheduleNudge(it, part2Entity) }
                    } else {
                        if (existingPart2 != null &&
                            existingPart2.status != com.neuroflow.app.domain.model.TaskStatus.COMPLETED &&
                            existingPart2.id !in focusedTaskIds
                        ) {
                            context?.let { AutonomyNudgeEngine.cancelNudge(it, existingPart2.id) }
                            taskRepository.delete(existingPart2)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlanViewModelV2", "Auto-schedule failed", e)
            } finally {
                _isAutoScheduling.value = false
            }
        }
    }

    fun triggerAutoScheduleNow() {
        autoScheduleDay(_selectedDate.value, forceReplan = false)
    }

    fun resolveConflict(conflict: ScheduleConflict) {
        val suggestedMinute = conflict.suggestedNewStartMinute
        val targetTask = uiState.value.tasksForDay.firstOrNull { it.id == conflict.taskId }
            ?: uiState.value.tasksForDay.firstOrNull { it.title == conflict.taskTitle }
        if (targetTask != null && suggestedMinute != null) {
            assignTaskTime(targetTask, suggestedMinute / 60, suggestedMinute % 60)
            viewModelScope.launch {
                _userActionMessage.emit(
                    "Shifted '${targetTask.title.take(24)}' to ${ConflictResolver.formatMinutesToTime(suggestedMinute)}"
                )
            }
        } else {
            triggerAutoScheduleNow()
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.update(task)
            val newTags = (task.tags.split(",") + task.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(task)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, task) }
        }
    }

    fun moveTaskToQuadrant(task: TaskEntity, newQuadrant: Quadrant) {
        viewModelScope.launch {
            val updated = task.copy(
                quadrant = newQuadrant,
                isFrog = if (newQuadrant == Quadrant.DO_FIRST) true else (if (newQuadrant == Quadrant.ELIMINATE) false else task.isFrog),
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
            if (newQuadrant == Quadrant.ELIMINATE) {
                context?.let { AutonomyNudgeEngine.cancelNudge(it, updated.id) }
            } else {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
            }
        }
    }

    fun breakDownTask(task: TaskEntity) {
        viewModelScope.launch {
            _decomposingTaskId.value = task.id
            try {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val generated = behavioralSupportService.generateMicroStepsWithAi(
                    taskTitle = task.title,
                    estimatedMinutes = task.estimatedDurationMinutes,
                    apiKey = prefs.geminiApiKey,
                    useAi = prefs.geminiAiEnabled,
                    userDirective = prefs.adaptiveSystemDirective
                )
                val microStepEntities = generated.mapIndexed { index, suggestion ->
                    TaskMicroStepEntity(
                        taskId = task.id,
                        title = suggestion.title,
                        sortOrder = index,
                        estimatedMinutes = suggestion.estimatedMinutes
                    )
                }
                taskRepository.deleteMicroStepsByTaskId(task.id)
                taskRepository.insertMicroSteps(microStepEntities)

                val planSteps = generated.map { StepByStepPlanStep(it.title, false) }
                val serialized = StepByStepPlanCodec.serialize(planSteps)
                taskRepository.update(task.copy(ifThenPlan = serialized, updatedAt = System.currentTimeMillis()))
                _userActionMessage.emit("Decomposed '${task.title.take(25)}' into ${generated.size} micro-steps 🪄")
            } catch (e: Exception) {
                _userActionMessage.emit("Breakdown completed via local heuristics.")
            } finally {
                _decomposingTaskId.value = null
            }
        }
    }

    fun saveQuickNote(content: String, title: String = "") {
        if (content.isBlank()) return
        viewModelScope.launch {
            val trimmed = content.trim()
            quickNoteDao?.insert(
                QuickNoteEntity(
                    content = trimmed,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            noteRepository?.saveNote(
                NoteEntity(
                    title = title.ifBlank { "Quick Thought" },
                    content = trimmed,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            val selectedDay = _selectedDate.value
            val boundTask = if (task.scheduledDate == null) {
                task.copy(
                    scheduledDate = selectedDay,
                    habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) selectedDay else task.habitDate
                )
            } else {
                task
            }
            taskRepository.insert(boundTask)
            val newTags = (boundTask.tags.split(",") + boundTask.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(boundTask)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, boundTask) }
            _userActionMessage.emit("Created task '${boundTask.title.take(24)}' ⚡")
        }
    }

    fun addTaskFromNote(title: String, content: String) = createTaskFromNote(title, content)

    fun createTaskFromNote(
        synthesis: com.neuroflow.app.domain.service.QuickNoteSynthesis,
        description: String = ""
    ) {
        viewModelScope.launch {
            val fullText = if (synthesis.extractedTaskTitle.isNotBlank()) "${synthesis.extractedTaskTitle}: $description" else description
            val inferred = TaskInferenceEngine.inferFromInput(fullText)
            val now = System.currentTimeMillis()
            val tagsCombined = (synthesis.tags + inferred.contextTags).distinct().filter { it.isNotBlank() }.joinToString(",")
            val stepList: List<com.neuroflow.app.domain.model.StepByStepPlanStep> = if (synthesis.firstActionStep.isNotBlank()) {
                listOf(com.neuroflow.app.domain.model.StepByStepPlanStep(synthesis.firstActionStep, completed = false))
            } else emptyList()
            val ifThenPlanStr = if (stepList.isNotEmpty()) {
                com.neuroflow.app.domain.model.StepByStepPlanCodec.serialize(stepList)
            } else ""

            val newTask = TaskEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = synthesis.extractedTaskTitle.ifBlank { inferred.sanitizedTitle.ifBlank { "Quick Task" } },
                description = description.trim(),
                estimatedDurationMinutes = if (synthesis.estimatedMinutes > 0) synthesis.estimatedMinutes else inferred.durationMinutes,
                priority = inferred.priority,
                energyLevel = inferred.energyDemand,
                taskType = inferred.taskType,
                quadrant = inferred.quadrant,
                tags = tagsCombined,
                ifThenPlan = ifThenPlanStr,
                scheduledDate = inferred.scheduledDate ?: _selectedDate.value,
                scheduledTime = inferred.scheduledTime,
                deadlineDate = inferred.deadlineDate,
                deadlineTime = inferred.deadlineTime,
                isFrog = inferred.isFrog,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insert(newTask)
            val newTags = (newTask.tags.split(",") + newTask.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(newTask)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, newTask) }
            _userActionMessage.emit("Created task '${newTask.title.take(24)}' ⚡")
        }
    }

    fun createTaskFromNote(title: String, description: String = "") {
        val inferred = TaskInferenceEngine.inferFromInput(if (title.isNotBlank()) "$title: $description" else description)
        val syn = com.neuroflow.app.domain.service.QuickNoteSynthesis(
            extractedTaskTitle = inferred.sanitizedTitle.ifBlank { title.ifBlank { description.take(35) } },
            suggestedCategory = inferred.taskType.name,
            estimatedMinutes = inferred.durationMinutes,
            tags = inferred.contextTags,
            firstActionStep = ""
        )
        createTaskFromNote(syn, description)
    }

    suspend fun synthesizeQuickNote(content: String): QuickNoteSynthesis {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return geminiAiOnlineService?.synthesizeQuickNote(
            rawContent = content,
            userDirective = prefs.adaptiveSystemDirective,
            apiKey = prefs.geminiApiKey,
            useAi = prefs.geminiAiEnabled
        ) ?: run {
            val inferred = TaskInferenceEngine.inferFromInput(content)
            QuickNoteSynthesis(
                extractedTaskTitle = inferred.sanitizedTitle.ifBlank { content.take(35) },
                suggestedCategory = inferred.taskType.name,
                estimatedMinutes = inferred.durationMinutes,
                tags = inferred.contextTags,
                firstActionStep = "Review scratchpad note and outline details"
            )
        }
    }

    fun addGoal(title: String, description: String = "Weekly Objective") {
        if (title.isBlank()) return
        viewModelScope.launch {
            goalRepository?.insert(
                com.neuroflow.app.data.local.entity.GoalEntity(
                    title = title.trim(),
                    description = description.trim().ifBlank { "Weekly Objective" },
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
            )
            _userActionMessage.emit("Goal added: ${title.take(20)}")
        }
    }

    fun completeGoal(goal: com.neuroflow.app.data.local.entity.GoalEntity) {
        viewModelScope.launch {
            goalRepository?.update(goal.copy(isActive = false))
            _userActionMessage.emit("Goal completed! 🎯")
        }
    }

    fun deleteGoal(goal: com.neuroflow.app.data.local.entity.GoalEntity) {
        viewModelScope.launch {
            goalRepository?.delete(goal)
            _userActionMessage.emit("Goal removed")
        }
    }

    fun shiftTaskToPeak(task: TaskEntity) {
        viewModelScope.launch {
            val peakPrefs = uiState.value.preferences
            val peakHour = uiState.value.circadianSummary?.peakWindowStartHour
                ?: (if (peakPrefs.effectivePeakStart in 0..23) peakPrefs.effectivePeakStart else peakPrefs.peakEnergyStart).coerceIn(0, 23)
            val peakEndHour = uiState.value.circadianSummary?.peakWindowEndHour ?: ((peakHour + 3) % 24)
            val dipEndHour = uiState.value.circadianSummary?.dipWindowEndHour ?: 16
            val usableEnergy = uiState.value.circadianSummary?.currentSnapshot?.usableEnergy ?: 60

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)

            val baseDate = task.effectiveScheduledDateMillis() ?: _selectedDate.value
            val baseCal = Calendar.getInstance().apply {
                timeInMillis = baseDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val baseDayStart = baseCal.timeInMillis

            val targetDate: Long
            val targetHour: Int
            val messageLabel: String

            val isPeakActive = if (peakHour <= peakEndHour) currentHour in peakHour until peakEndHour else (currentHour >= peakHour || currentHour < peakEndHour)

            if (baseDayStart > todayStart) {
                // Future scheduled date: preserve future day and place at peak hour
                targetDate = baseDayStart
                targetHour = peakHour
                messageLabel = "Peak Window (${peakHour}:00)"
            } else {
                // Today or overdue past task: schedule for today's active/future window, or tomorrow if passed
                if (currentHour < peakHour && peakHour <= peakEndHour) {
                    targetDate = todayStart
                    targetHour = peakHour
                    messageLabel = "Peak Window (${peakHour}:00)"
                } else if (isPeakActive) {
                    targetDate = todayStart
                    targetHour = currentHour
                    messageLabel = "Peak Window (${currentHour}:00 Active)"
                } else if (currentHour < 18 && usableEnergy >= 55) {
                    targetDate = todayStart
                    targetHour = maxOf(currentHour + 1, dipEndHour).coerceAtMost(17)
                    messageLabel = "Afternoon Rebound (${targetHour}:00)"
                } else {
                    targetDate = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart)
                    targetHour = peakHour
                    messageLabel = "Tomorrow's Peak (${peakHour}:00)"
                }
            }

            val millisInDay = targetHour * 3600_000L
            val updated = task.copy(
                scheduledDate = targetDate,
                scheduledTime = millisInDay,
                isAutoScheduled = false,
                isScheduleLocked = true,
                habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) targetDate else task.habitDate,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
            _userActionMessage.emit("Shifted '${task.title.take(20)}' to $messageLabel ⚡")
        }
    }

    fun assignTaskTime(task: TaskEntity, hour: Int, minute: Int) {
        viewModelScope.launch {
            val millisInDay = hour * 3600_000L + minute * 60_000L
            val selectedDay = _selectedDate.value
            val updatedTask = task.copy(
                scheduledDate = selectedDay,
                scheduledTime = millisInDay,
                isAutoScheduled = false,
                isScheduleLocked = true,
                habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) selectedDay else task.habitDate,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updatedTask)
            val startMillis = selectedDay + millisInDay
            calendarSyncRepository?.upsertTaskEvent(updatedTask, startMillis = startMillis)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, updatedTask) }
        }
    }

    fun assignTaskTime(taskId: String, newStartMillis: Long) {
        val targetTask = uiState.value.tasksForDay.firstOrNull { it.id == taskId }
            ?: uiState.value.unscheduledTasks.firstOrNull { it.id == taskId }
            ?: return
        val (startOfDay, timeOffset) = com.neuroflow.app.worker.ScheduleAutoTasksWorker.splitMillisToDateAndTime(newStartMillis)
        viewModelScope.launch {
            val updatedTask = targetTask.copy(
                scheduledDate = startOfDay,
                scheduledTime = timeOffset,
                isAutoScheduled = false,
                isScheduleLocked = true,
                habitDate = if (targetTask.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) startOfDay else targetTask.habitDate,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updatedTask)
            calendarSyncRepository?.upsertTaskEvent(updatedTask, startMillis = newStartMillis)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, updatedTask) }
        }
    }

    private val lastSpawnedRecurringTaskByTaskId = object : LinkedHashMap<String, String>(20, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 25
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            val now = System.currentTimeMillis()
            val spawnedId = taskRepository.completeAndRecur(task, now)
            if (spawnedId != null) {
                lastSpawnedRecurringTaskByTaskId[task.id] = spawnedId
            }
            calendarSyncRepository?.handleTaskStateChanged(
                task.copy(status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED)
            )
            val prefs = preferencesDataStore.preferencesFlow.first()
            val newCompleted = prefs.totalTasksCompleted + 1
            val lastActive = prefs.lastActiveDate
            val todayStart = startOfLocalDay(now)
            val isNewDay = lastActive < todayStart
            val isConsecutive = lastActive >= startOfPreviousLocalDay(todayStart) && isNewDay
            val newStreak = when {
                isConsecutive -> prefs.dailyStreak + 1
                isNewDay -> 1
                else -> prefs.dailyStreak
            }
            val longest = maxOf(prefs.longestStreak, newStreak)
            preferencesDataStore.updatePreferences {
                it.copy(
                    totalTasksCompleted = newCompleted,
                    dailyStreak = newStreak,
                    longestStreak = longest,
                    lastActiveDate = now
                )
            }
            val affirmationSuffix = if (prefs.affirmations.isNotEmpty()) {
                " • \"${prefs.affirmations.random()}\""
            } else ""
            _userActionMessage.emit("Completed: ${task.title}$affirmationSuffix")
        }
    }

    fun uncompleteTask(task: TaskEntity) {
        viewModelScope.launch {
            val spawnedId = lastSpawnedRecurringTaskByTaskId.remove(task.id)
            if (spawnedId != null) {
                taskRepository.getById(spawnedId)?.let { taskRepository.delete(it) }
            }
            val updated = task.copy(
                status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
                completedAt = null,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            calendarSyncRepository?.handleTaskStateChanged(updated)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalTasksCompleted = maxOf(0, prefs.totalTasksCompleted - 1))
            }
            _userActionMessage.emit("Restored '${task.title.take(24)}' ⚡")
        }
    }

    fun toggleProtectedSlot(slotId: String, enabled: Boolean) {
        viewModelScope.launch {
            val currentSlots = preferencesDataStore.preferencesFlow.first().protectedTimeSlots
            val updatedSlots = currentSlots.map { slot ->
                if (slot.id == slotId) slot.copy(isEnabled = enabled) else slot
            }
            preferencesDataStore.updateProtectedTimeSlots(updatedSlots)
        }
    }

    fun saveProtectedSlot(slot: ProtectedTimeSlot) {
        viewModelScope.launch {
            val currentSlots = preferencesDataStore.preferencesFlow.first().protectedTimeSlots
            val updatedSlots = if (currentSlots.any { it.id == slot.id }) {
                currentSlots.map { if (it.id == slot.id) slot else it }
            } else {
                currentSlots + slot
            }
            preferencesDataStore.updateProtectedTimeSlots(updatedSlots)
        }
    }

    fun deleteProtectedSlot(slotId: String) {
        viewModelScope.launch {
            val currentSlots = preferencesDataStore.preferencesFlow.first().protectedTimeSlots
            val updatedSlots = currentSlots.filterNot { it.id == slotId }
            preferencesDataStore.updateProtectedTimeSlots(updatedSlots)
        }
    }

    suspend fun enrichTaskWithAi(taskTitle: String, durationMinutes: Int = 30): com.neuroflow.app.domain.service.TaskEnrichmentResult? {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return geminiAiOnlineService?.enrichTaskWithAi(
            title = taskTitle,
            durationMinutes = durationMinutes,
            userDirective = prefs.adaptiveSystemDirective,
            apiKey = prefs.geminiApiKey,
            useAi = prefs.geminiAiEnabled
        )
    }

    fun logPastTime(task: TaskEntity, minutes: Float) {
        if (minutes.isNaN() || minutes.isInfinite()) return
        val safeMinutes = minutes.coerceAtMost(1440f)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TimeSessionEntity(
                taskId = task.id,
                startedAt = now - (safeMinutes * 60_000f).toLong(),
                endedAt = now,
                durationMinutes = safeMinutes,
                sessionType = "MANUAL_LOG"
            )
            sessionRepository?.insert(session)
            val newTracked = task.totalTimeTrackedMinutes + safeMinutes
            val newCount = task.sessionCount + 1
            taskRepository.update(
                task.copy(
                    totalTimeTrackedMinutes = newTracked,
                    sessionCount = newCount,
                    lastSessionDurationMinutes = safeMinutes,
                    actualDurationMinutes = newTracked,
                    updatedAt = now
                )
            )
            preferencesDataStore.updatePreferences { prefs ->
                val lastActive = prefs.lastActiveDate
                val todayStart = startOfLocalDay(now)
                val isNewDay = lastActive < todayStart
                val isConsecutive = lastActive >= startOfPreviousLocalDay(todayStart) && isNewDay
                val newStreak = when {
                    isConsecutive -> prefs.dailyStreak + 1
                    isNewDay -> 1
                    else -> prefs.dailyStreak
                }
                val longest = maxOf(prefs.longestStreak, newStreak)
                prefs.copy(
                    totalFocusMinutes = prefs.totalFocusMinutes + safeMinutes.toInt(),
                    lastActiveDate = now,
                    dailyStreak = newStreak,
                    longestStreak = longest
                )
            }
        }
    }

    private var pendingDeleteJob: Job? = null

    fun requestDeleteTask(task: TaskEntity) {
        val current = _pendingDeletedTask.value
        if (current != null && current.id != task.id) {
            confirmDeleteTask()
        }
        pendingDeleteJob?.cancel()
        _pendingDeletedTask.value = task
        pendingDeleteJob = viewModelScope.launch {
            delay(5000L)
            confirmDeleteTask()
        }
    }

    fun undoDeleteTask() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _pendingDeletedTask.value = null
    }

    fun confirmDeleteTask() {
        val task = _pendingDeletedTask.value ?: return
        _pendingDeletedTask.value = null
        viewModelScope.launch {
            context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            preferencesDataStore.recordDeletedTask(
                title = task.title,
                durationMinutes = task.estimatedDurationMinutes,
                category = task.determineCategory().name,
                postponeCount = task.postponeCount
            )
            taskRepository.delete(task)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val task = _pendingDeletedTask.value
        if (task != null) {
            _pendingDeletedTask.value = null
            pendingDeleteJob?.cancel()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
                preferencesDataStore.recordDeletedTask(
                    title = task.title,
                    durationMinutes = task.estimatedDurationMinutes,
                    category = task.determineCategory().name,
                    postponeCount = task.postponeCount
                )
                taskRepository.delete(task)
            }
        }
    }

    fun toggleStagingDrawer() {
        _isStagingDrawerExpanded.value = !_isStagingDrawerExpanded.value
    }

    fun toggleSearchVisible() {
        val next = !_isSearchVisible.value
        _isSearchVisible.value = next
        if (!next) _searchQuery.value = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
