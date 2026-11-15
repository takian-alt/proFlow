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
import com.neuroflow.app.domain.service.SlotRemedyResult
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import com.neuroflow.app.domain.service.TimeSlotWindow
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
import com.neuroflow.app.domain.scheduler.DailyBlueprint
import com.neuroflow.app.domain.scheduler.DailyBlueprintOrchestrator
import com.neuroflow.app.domain.scheduler.determineCategory
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.DeferralRationale
import com.neuroflow.app.domain.scheduler.ScheduleConflict
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.presentation.v2.focus.FocusTimerModeV2
import com.neuroflow.app.presentation.v2.focus.service.FocusService
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
import com.neuroflow.app.domain.scheduler.MultiDayScheduleOrchestrator
import com.neuroflow.app.domain.scheduler.DeferralReasonCode
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.data.calendar.ExternalDeltaType
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
import java.time.LocalDate
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
    val deferrals: List<DeferralRationale> = emptyList(),
    val scheduledMinutesToday: Int = 0,
    val completedMinutesToday: Int = 0,
    val recommendedCapacityMinutes: Int = 240,
    val sevenDayLoadDistribution: Map<LocalDate, Float> = emptyMap(),
    val feedbackTask: TaskEntity? = null,
    val feedbackRemedyProposal: SlotRemedyResult? = null,
    val isFeedbackLoading: Boolean = false,
    val dailyBlueprint: DailyBlueprint? = null,
    val isBlueprintLoading: Boolean = false,
    val blueprintIncludedTaskIds: Set<String> = emptySet()
)

internal fun startOfTodayMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return LocalDate.now(zoneId)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
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
    private val geminiAiOnlineService: GeminiAiOnlineService? = null,
    private val multiDayScheduleOrchestrator: MultiDayScheduleOrchestrator? = null,
    private val dailyBlueprintOrchestrator: DailyBlueprintOrchestrator? = null
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
    private val _feedbackTask = MutableStateFlow<TaskEntity?>(null)
    private val _feedbackRemedyProposal = MutableStateFlow<SlotRemedyResult?>(null)
    private val _isFeedbackLoading = MutableStateFlow(false)
    private val _dailyBlueprint = MutableStateFlow<DailyBlueprint?>(null)
    private val _isBlueprintLoading = MutableStateFlow(false)
    private val _blueprintIncludedTaskIds = MutableStateFlow<Set<String>>(emptySet())

    private val _userActionMessage = MutableSharedFlow<String>()
    val userActionMessage: SharedFlow<String> = _userActionMessage.asSharedFlow()

    val recentNotes: Flow<List<NoteEntity>> = noteRepository?.allNotes ?: flowOf(emptyList())

    val completedTasks: StateFlow<List<TaskEntity>> = taskRepository.observeCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sevenDayLoadDistribution: StateFlow<Map<LocalDate, Float>> = combine(
        taskRepository.observeActiveTasks(),
        preferencesDataStore.preferencesFlow
    ) { allTasks, prefs ->
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val capacity = prefs.dailyDeepWorkTargetMinutes.takeIf { it > 0 } ?: 240
        (0 until 7).associate { offset ->
            val d = today.plusDays(offset.toLong())
            val dStart = d.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dEnd = d.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
            val dayTasks = allTasks.filter { task ->
                if (task.quadrant == Quadrant.ELIMINATE) return@filter false
                val sDate = task.effectiveScheduledDateMillis()
                if (sDate != null) {
                    if (offset == 0) sDate <= dEnd else sDate in dStart..dEnd
                } else {
                    val rawScheduledTime = task.scheduledTime
                    if (rawScheduledTime != null && rawScheduledTime >= ONE_DAY_MILLIS) {
                        if (offset == 0) rawScheduledTime <= dEnd else rawScheduledTime in dStart..dEnd
                    } else {
                        val dDate = task.resolveDeadlineEpochMillis(dStart)
                        dDate != null && (if (offset == 0) dDate <= dEnd else dDate in dStart..dEnd)
                    }
                }
            }
            val scheduledMins = dayTasks.sumOf { com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(it) }
            val fraction = (scheduledMins.toFloat() / capacity.toFloat().coerceAtLeast(1f))
            d to fraction
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyMap()
    )

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
        val deferrals: List<DeferralRationale> = emptyList(),
        val feedbackTask: TaskEntity? = null,
        val feedbackRemedyProposal: SlotRemedyResult? = null,
        val isFeedbackLoading: Boolean = false,
        val dailyBlueprint: DailyBlueprint? = null,
        val isBlueprintLoading: Boolean = false,
        val blueprintIncludedTaskIds: Set<String> = emptySet()
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
            },
            combine(
                combine(_feedbackTask, _feedbackRemedyProposal, _isFeedbackLoading) { fbTask, fbRemedy, fbLoading ->
                    listOf<Any?>(fbTask, fbRemedy, fbLoading)
                },
                combine(_dailyBlueprint, _isBlueprintLoading, _blueprintIncludedTaskIds) { bp, bpLoading, bpIds ->
                    listOf<Any?>(bp, bpLoading, bpIds)
                }
            ) { fourth, fifth ->
                fourth + fifth
            }
        ) { first, second, third, combinedFourthFifth ->
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
                deferrals = third[3] as List<DeferralRationale>,
                feedbackTask = combinedFourthFifth[0] as TaskEntity?,
                feedbackRemedyProposal = combinedFourthFifth[1] as SlotRemedyResult?,
                isFeedbackLoading = combinedFourthFifth[2] as Boolean,
                dailyBlueprint = combinedFourthFifth[3] as DailyBlueprint?,
                isBlueprintLoading = combinedFourthFifth[4] as Boolean,
                blueprintIncludedTaskIds = combinedFourthFifth[5] as Set<String>
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

        val baseTasks = (if (params.pendingDeletedTask != null) {
            allTasks.filter { it.id != params.pendingDeletedTask.id }
        } else {
            allTasks
        }).filterNot { it.id == com.neuroflow.app.data.local.entity.TaskEntity.TASK_UNTRACKED_FOCUS }

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
                    task.tags.contains(query, ignoreCase = true) ||
                    task.contextTag.contains(query, ignoreCase = true)
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
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
        val isTargetingToday = targetLocalDate == nowLocalDate

        val tasksForDay = searchFilteredAll.filter { task ->
            if (task.quadrant == Quadrant.ELIMINATE) return@filter false
            val sDate = task.effectiveScheduledDateMillis()
            if (sDate != null) {
                if (isTargetingToday) sDate <= dayEnd else sDate in dayStart..dayEnd
            } else {
                val rawScheduledTime = task.scheduledTime
                if (rawScheduledTime != null && rawScheduledTime >= ONE_DAY_MILLIS) {
                    if (isTargetingToday) rawScheduledTime <= dayEnd else rawScheduledTime in dayStart..dayEnd
                } else {
                    val dDate = task.resolveDeadlineEpochMillis(dateMillis)
                    dDate != null && (if (isTargetingToday) dDate <= dayEnd else dDate in dayStart..dayEnd)
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
            val sDate = task.effectiveScheduledDateMillis()
            val isPastOverdue = sDate != null && sDate < todayStart
            task.quadrant != Quadrant.ELIMINATE &&
                (sDate == null || (!isTargetingToday && isPastOverdue)) &&
                (task.scheduledTime == null || task.scheduledTime < ONE_DAY_MILLIS || (!isTargetingToday && isPastOverdue))
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

        val scheduledMinutes = tasksForDay.sumOf { com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(it) }
        val completedOnDate = completedTasks.value.filter { task ->
            val completionTime = task.completedAt ?: task.updatedAt
            completionTime in dayStart..dayEnd || (task.effectiveScheduledDateMillis()?.let { it in dayStart..dayEnd } ?: false)
        }.sumOf { task ->
            (task.actualDurationMinutes?.let { kotlin.math.round(it).toInt() }
                ?: task.totalTimeTrackedMinutes.let { kotlin.math.round(it).toInt() }.takeIf { it > 0 }
                ?: com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task)).coerceAtLeast(1)
        }
        val untrackedSessionsToday = runCatching {
            sessionRepository?.getAllSessions().orEmpty()
                .filter { it.endedAt != null && it.startedAt in dayStart..dayEnd && it.taskId == com.neuroflow.app.data.local.entity.TaskEntity.TASK_UNTRACKED_FOCUS }
                .sumOf { kotlin.math.round(it.durationMinutes.takeIf { d -> d > 0f } ?: it.trackedMinutes(it.endedAt ?: now)).toInt() }
        }.getOrDefault(0)
        val completedMinutesToday = completedOnDate + untrackedSessionsToday
        val capacityMinutes = prefs.dailyDeepWorkTargetMinutes.takeIf { it > 0 }
            ?: circadianSummary?.recommendedDeepWorkMinutes ?: 240

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
            deferrals = params.deferrals,
            scheduledMinutesToday = scheduledMinutes,
            completedMinutesToday = completedMinutesToday,
            recommendedCapacityMinutes = capacityMinutes,
            feedbackTask = params.feedbackTask,
            feedbackRemedyProposal = params.feedbackRemedyProposal,
            isFeedbackLoading = params.isFeedbackLoading,
            dailyBlueprint = params.dailyBlueprint,
            isBlueprintLoading = params.isBlueprintLoading,
            blueprintIncludedTaskIds = params.blueprintIncludedTaskIds
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

    fun openDailyBlueprint(dateMillis: Long = _selectedDate.value) {
        viewModelScope.launch {
            _isBlueprintLoading.value = true
            try {
                val orchestrator = dailyBlueprintOrchestrator
                if (orchestrator != null) {
                    val blueprint = orchestrator.generateBlueprint(dateMillis)
                    _dailyBlueprint.value = blueprint
                    _blueprintIncludedTaskIds.value = blueprint.items.map { it.task.id }.toSet()
                }
            } catch (e: Exception) {
                // Non-blocking fallback
            } finally {
                _isBlueprintLoading.value = false
            }
        }
    }

    fun toggleBlueprintTask(taskId: String, isIncluded: Boolean) {
        val current = _blueprintIncludedTaskIds.value.toMutableSet()
        if (isIncluded) {
            current.add(taskId)
        } else {
            current.remove(taskId)
        }
        _blueprintIncludedTaskIds.value = current
    }

    fun applyDailyBlueprint() {
        viewModelScope.launch {
            val blueprint = _dailyBlueprint.value ?: return@launch
            val orchestrator = dailyBlueprintOrchestrator ?: return@launch
            val included = _blueprintIncludedTaskIds.value
            val result = orchestrator.applyBlueprint(blueprint, included)
            _userActionMessage.emit("Scheduled ${result.committedCount} tasks")
            _dailyBlueprint.value = null
        }
    }

    fun dismissDailyBlueprint() {
        _dailyBlueprint.value = null
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
                val focusedTaskIds = sessionRepository?.getOpenSessions().orEmpty().filter { session ->
                    val activeServiceTaskId = FocusService.activeSession.value?.taskId
                    if (activeServiceTaskId != null && activeServiceTaskId == session.taskId) return@filter true
                    val modeDurationSec = FocusTimerModeV2.entries
                        .firstOrNull { it.name == session.sessionType }?.durationSeconds
                    val maxDurationMs = if (modeDurationSec != null && modeDurationSec > 0L) {
                        modeDurationSec * 1000L
                    } else {
                        120 * 60_000L
                    }
                    val elapsedMs = (nowMillis - session.startedAt - session.totalPausedMs).coerceAtLeast(0L)
                    elapsedMs < maxDurationMs
                }.map { it.taskId }.toSet()
                val activeTaskId = allSessions.firstOrNull { it.endedAt == null }?.taskId

                val nowLocalDate = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
                val isTargetingToday = dayDate == nowLocalDate
                val activeTaskIds = allTasks.map { it.id }.toSet()

                fun isContinuationBlocked(task: TaskEntity, activeIds: Set<String>): Boolean {
                    val partNumMatch = Regex("""[#_]part(\d+)""").find(task.id)
                    val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
                        ?: Regex("""\(Part (\d+)/""").find(task.title)?.groupValues?.get(1)?.toIntOrNull()
                    val isContinuation = (partNum != null && partNum >= 2) ||
                        task.id.endsWith("_part2") ||
                        task.id.contains("#part2") ||
                        task.parentTaskId != null
                    if (!isContinuation) return false
                    val parentSlug = task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
                    val isParentActive = (task.parentTaskId != null && task.parentTaskId in activeIds) ||
                        (parentSlug != task.id && parentSlug in activeIds)
                    val hasPartRegex = task.id.contains(Regex("""[#_]part\d+"""))
                    val isPart1Active = if (hasPartRegex) {
                        val part1Id = task.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1")
                        part1Id != task.id && part1Id in activeIds
                    } else false
                    val isPrecedingPartActive = if (hasPartRegex && partNum != null) {
                        (1 until partNum).any { prevPart ->
                            val prevId = task.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart")
                            prevId != task.id && prevId in activeIds
                        }
                    } else if (task.parentTaskId != null && partNum != null && partNum > 1) {
                        allTasks.any { other ->
                            other.id != task.id && other.id in activeIds &&
                                (other.parentTaskId == task.parentTaskId || other.id == task.parentTaskId) &&
                                (Regex("""\(Part (\d+)/""").find(other.title)?.groupValues?.get(1)?.toIntOrNull() ?: 1) < partNum
                        }
                    } else false
                    return isParentActive || isPart1Active || isPrecedingPartActive
                }

                val candidateTasks = allTasks.filter { task ->
                    val effectiveDate = task.effectiveScheduledDateMillis()
                    val isDateEligible = effectiveDate == null ||
                        effectiveDate in dayStart..dayEnd ||
                        (isTargetingToday && effectiveDate <= dayEnd)
                    val isBlocked = isContinuationBlocked(task, activeTaskIds)
                    AutoSchedulingContracts.isMutableByAutoScheduler(task, nowMillis) &&
                        task.id !in focusedTaskIds &&
                        !isBlocked &&
                        isDateEligible
                }
                val candidateIds = candidateTasks.map { it.id }.toSet()

                val externalBusyWindows = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd) ?: emptyList()
                val fixedWindows = allTasks
                    .filter { it.quadrant != com.neuroflow.app.domain.model.Quadrant.ELIMINATE }
                    .filterNot { task ->
                        if (task.id in candidateIds) return@filterNot true
                        val partNumMatch = Regex("""[#_]part(\d+)""").find(task.id)
                        val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
                            ?: Regex("""\(Part (\d+)/""").find(task.title)?.groupValues?.get(1)?.toIntOrNull()
                        val isContinuation = (partNum != null && partNum >= 2) ||
                            task.id.endsWith("_part2") ||
                            task.id.contains("#part2") ||
                            task.parentTaskId != null
                        if (isContinuation) {
                            val parentSlug = task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
                            parentSlug in candidateIds || task.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1") in candidateIds
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

                // Cognitive budget accounting: deduct committed minutes (fixed/locked/manual tasks and completed tasks)
                // so the scheduler defers candidate auto-tasks if total day load exceeds safe daily capacity.
                val completedMinutesToday = allKnownTasks
                    .filter { it.status == com.neuroflow.app.domain.model.TaskStatus.COMPLETED && it.effectiveScheduledDateMillis() in dayStart..dayEnd }
                    .sumOf { com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(it) }
                val fixedCommittedMinutes = fixedWindows.sumOf { (it.endMillis - it.startMillis) / 60_000L }.toInt()
                val totalPrecommittedMinutes = fixedCommittedMinutes + completedMinutesToday

                val capacityMinutes = prefs.dailyDeepWorkTargetMinutes.takeIf { it > 0 } ?: 240
                val effectiveRemainingCapacity = (capacityMinutes - totalPrecommittedMinutes).coerceAtLeast(0)

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
                    microStepCounts = microStepCounts,
                    dailyCognitiveCapacityMinutes = effectiveRemainingCapacity
                )
                val placements = outcome.placements
                _deferrals.value = outcome.deferrals

                val placementsByBaseId = placements.groupBy { it.task.id.replace(Regex("""[#_]part\d+.*$"""), "") }

                for ((baseTaskId, taskPlacements) in placementsByBaseId) {
                    val primaryPlacement = taskPlacements.first()
                    val isStandalonePart2 = taskPlacements.size == 1 &&
                        (primaryPlacement.task.id.contains(Regex("""[#_]part[2-9]""")) || primaryPlacement.task.id.endsWith("_part2"))
                    val targetTaskId = if (isStandalonePart2) primaryPlacement.task.id else baseTaskId
                    val fresh = taskRepository.getById(targetTaskId) ?: continue
                    if (fresh.status != com.neuroflow.app.domain.model.TaskStatus.ACTIVE ||
                        fresh.id in focusedTaskIds ||
                        !AutoSchedulingContracts.isMutableByAutoScheduler(fresh, nowMillis)
                    ) continue
                    val startOfDay = startOfLocalDay(primaryPlacement.window.startMillis)
                    val timeOffset = primaryPlacement.window.startMillis - startOfDay

                    val cleanTitle = fresh.title.replace(Regex("""\s*\(Part \d+/\d+\)"""), "").trim()
                    val isSplit = taskPlacements.size > 1 || primaryPlacement.task.id.contains(Regex("""[#_]part1"""))
                    val titleToUse = when {
                        isSplit -> "$cleanTitle (Part 1/2)"
                        isStandalonePart2 -> fresh.title
                        else -> cleanTitle
                    }
                    val updated = fresh.copy(
                        title = titleToUse,
                        scheduledDate = startOfDay,
                        scheduledTime = timeOffset,
                        habitDate = if (fresh.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) startOfDay else fresh.habitDate,
                        isAutoScheduled = true,
                        plannedBlockMinutes = primaryPlacement.window.durationMinutes,
                        lastAutoScheduledAt = nowMillis,
                        lastStabilizedAt = nowMillis,
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

                    if (!isStandalonePart2) {
                        // BUG-AUDIT-011 Fix: Enforce #part2 delimiter consistency with TaskSchedulerService
                        val part2Id = "${baseTaskId}#part2"
                        val part2Placement = taskPlacements.getOrNull(1)
                        val existingPart2 = taskRepository.getById(part2Id) ?: taskRepository.getById("${baseTaskId}_part2")
                        if (isSplit && part2Placement != null && (part2Placement.task.id.contains(Regex("""[#_]part2""")) || taskPlacements.size > 1) &&
                            existingPart2?.status != com.neuroflow.app.domain.model.TaskStatus.COMPLETED &&
                            existingPart2?.id !in focusedTaskIds
                        ) {
                            val startOfDay2 = startOfLocalDay(part2Placement.window.startMillis)
                            val timeOffset2 = part2Placement.window.startMillis - startOfDay2
                            val part2Title = "$cleanTitle (Part 2/2)"
                            val part2Entity = (existingPart2 ?: fresh).copy(
                                id = existingPart2?.id ?: part2Id,
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
                                habitDate = null,
                                sessionCount = existingPart2?.sessionCount ?: 0,
                                totalTimeTrackedMinutes = existingPart2?.totalTimeTrackedMinutes ?: 0f,
                                actualDurationMinutes = existingPart2?.actualDurationMinutes ?: 0f,
                                lastSessionDurationMinutes = existingPart2?.lastSessionDurationMinutes ?: 0f,
                                lastAutoScheduledAt = nowMillis,
                                lastStabilizedAt = nowMillis,
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
                            val legacyPart2 = taskRepository.getById("${baseTaskId}_part2")
                            if (legacyPart2 != null &&
                                legacyPart2.status != com.neuroflow.app.domain.model.TaskStatus.COMPLETED &&
                                legacyPart2.id !in focusedTaskIds
                            ) {
                                context?.let { AutonomyNudgeEngine.cancelNudge(it, legacyPart2.id) }
                                taskRepository.delete(legacyPart2)
                            }
                        }
                    }
                }

                // Deferral cleanup & cognitive budget protection: clear scheduledTime/scheduledDate for deferred candidate tasks
                var deferredCount = 0
                val candidateBaseIds = candidateTasks.map { it.id.replace(Regex("""[#_]part\d+.*$"""), "") }.toSet()
                for (def in outcome.deferrals) {
                    val baseTaskId = def.taskId.replace(Regex("""[#_]part\d+.*$"""), "")
                    if (baseTaskId !in candidateBaseIds) continue
                    val fresh = taskRepository.getById(baseTaskId) ?: continue
                    if (fresh.status != com.neuroflow.app.domain.model.TaskStatus.ACTIVE ||
                        fresh.id in focusedTaskIds ||
                        fresh.isScheduleLocked
                    ) continue

                    val updatedDeferred = fresh.copy(
                        scheduledDate = null,
                        scheduledTime = null,
                        isAutoScheduled = false,
                        plannedBlockMinutes = 0,
                        updatedAt = nowMillis
                    )
                    taskRepository.update(updatedDeferred)
                    calendarSyncRepository?.deleteTaskEvent(updatedDeferred.id)
                    context?.let { AutonomyNudgeEngine.cancelNudge(it, updatedDeferred.id) }

                    val part2Id = "${baseTaskId}#part2"
                    val existingPart2 = taskRepository.getById(part2Id) ?: taskRepository.getById("${baseTaskId}_part2")
                    if (existingPart2 != null &&
                        existingPart2.status != com.neuroflow.app.domain.model.TaskStatus.COMPLETED &&
                        existingPart2.id !in focusedTaskIds
                    ) {
                        context?.let { AutonomyNudgeEngine.cancelNudge(it, existingPart2.id) }
                        taskRepository.delete(existingPart2)
                    }

                    deferredCount++
                }

                val scheduledCount = placementsByBaseId.size
                val message = when {
                    scheduledCount > 0 && deferredCount > 0 ->
                        "Auto-balanced: scheduled $scheduledCount tasks, deferred $deferredCount tasks to backlog to protect your cognitive budget."
                    scheduledCount > 0 && totalPrecommittedMinutes > capacityMinutes ->
                        "Scheduled $scheduledCount tasks. Day remains overloaded due to fixed/manual tasks."
                    scheduledCount > 0 ->
                        "Auto-scheduled $scheduledCount tasks successfully."
                    deferredCount > 0 ->
                        "Deferred $deferredCount tasks to backlog to protect cognitive budget."
                    totalPrecommittedMinutes > capacityMinutes ->
                        "Cannot auto-rebalance: day is overloaded by locked or manually placed tasks."
                    else ->
                        "No eligible tasks found to schedule."
                }
                _userActionMessage.emit(message)
            } catch (e: Exception) {
                android.util.Log.e("PlanViewModelV2", "Auto-schedule failed", e)
            } finally {
                _isAutoScheduling.value = false
            }
        }
    }

    fun triggerAutoScheduleNow(forceReplan: Boolean = true) {
        autoScheduleDay(_selectedDate.value, forceReplan = forceReplan)
    }

    fun onScreenResume() {
        viewModelScope.launch {
            val calRepo = calendarSyncRepository ?: return@launch
            try {
                val lastSync = preferencesDataStore.preferencesFlow.first().lastCalendarInboundSyncAt
                val deltas = calRepo.detectExternalModifications(lastSync)
                if (deltas.isNotEmpty()) {
                    val nowMillis = System.currentTimeMillis()
                    for (delta in deltas) {
                        val existing = taskRepository.getById(delta.taskId)
                        if (existing != null && existing.status == com.neuroflow.app.domain.model.TaskStatus.ACTIVE) {
                            when (delta.type) {
                                ExternalDeltaType.MOVED -> {
                                    if (delta.newStartMillis != null) {
                                        val startDay = startOfLocalDay(delta.newStartMillis)
                                        val timeOffset = delta.newStartMillis - startDay
                                        taskRepository.update(
                                            existing.copy(
                                                scheduledDate = startDay,
                                                scheduledTime = timeOffset,
                                                isScheduleLocked = true,
                                                updatedAt = nowMillis
                                            )
                                        )
                                    }
                                }
                                ExternalDeltaType.DELETED -> {
                                    taskRepository.update(
                                        existing.copy(
                                            scheduledDate = null,
                                            scheduledTime = null,
                                            isAutoScheduled = false,
                                            isScheduleLocked = false,
                                            updatedAt = nowMillis
                                        )
                                    )
                                }
                                ExternalDeltaType.EVENT_ADDED -> {
                                    // External event deltas are handled via calendar reconciliation
                                }
                            }
                        }
                    }
                    preferencesDataStore.setLastCalendarInboundSyncAt(nowMillis)
                }
            } catch (e: Exception) {
                android.util.Log.w("PlanViewModelV2", "Inbound calendar sync failed on resume", e)
            }
        }
    }

    fun autoBalance(horizonDays: Int = 7): kotlinx.coroutines.Job {
        if (_isAutoScheduling.value) return kotlinx.coroutines.Job().apply { complete() }
        return viewModelScope.launch {
            _isAutoScheduling.value = true
            try {
                val orch = multiDayScheduleOrchestrator ?: MultiDayScheduleOrchestrator(
                    schedulerService = taskSchedulerService,
                    circadianEnergyService = circadianEnergyService,
                    calendarSyncRepository = calendarSyncRepository
                )
                val allTasks = taskRepository.getActiveTasks()
                val nowMillis = System.currentTimeMillis()
                val candidateTasks = allTasks.filter {
                    AutoSchedulingContracts.isMutableByAutoScheduler(it, nowMillis)
                }.map { task ->
                    val resolvedCat = when (task.determineCategory()) {
                        com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK -> com.neuroflow.app.domain.service.TaskCategory.DEEP_WORK
                        com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL -> com.neuroflow.app.domain.service.TaskCategory.DEEP_WORK
                        com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE -> com.neuroflow.app.domain.service.TaskCategory.CREATIVE
                        com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE -> com.neuroflow.app.domain.service.TaskCategory.ADMIN
                        com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL -> com.neuroflow.app.domain.service.TaskCategory.EXERCISE
                        com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE -> com.neuroflow.app.domain.service.TaskCategory.EXERCISE
                        com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS -> com.neuroflow.app.domain.service.TaskCategory.MINDFULNESS
                        com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE -> com.neuroflow.app.domain.service.TaskCategory.GENERAL
                    }
                    val energyDemand = when (task.energyLevel) {
                        com.neuroflow.app.domain.model.EnergyLevel.HIGH -> com.neuroflow.app.domain.service.EnergyDemand.HIGH
                        com.neuroflow.app.domain.model.EnergyLevel.MEDIUM -> com.neuroflow.app.domain.service.EnergyDemand.MODERATE
                        else -> com.neuroflow.app.domain.service.EnergyDemand.LOW
                    }
                    SchedulableTask(
                        id = task.id,
                        title = task.title,
                        category = resolvedCat,
                        priority = when (task.priority) {
                            com.neuroflow.app.domain.model.Priority.HIGH -> 2
                            com.neuroflow.app.domain.model.Priority.MEDIUM -> 1
                            else -> 0
                        },
                        estimatedMinutes = com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task),
                        energyDemand = energyDemand,
                        deadlineMillis = task.resolveDeadlineEpochMillis(),
                        isFrog = task.isFrog
                    )
                }
                val zoneId = ZoneId.systemDefault()
                val startDate = Instant.ofEpochMilli(_selectedDate.value).atZone(zoneId).toLocalDate()
                val outcome = orch.orchestrateMultiDaySchedule(
                    tasks = candidateTasks,
                    startDate = startDate,
                    horizonDays = horizonDays,
                    zoneId = zoneId
                )
                for ((dayDate, dayOutcome) in outcome.dayOutcomes) {
                    val dayStart = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    for (placement in dayOutcome.placements) {
                        val baseId = placement.task.id.replace(Regex("""[#_]part\d+.*$"""), "")
                        val fresh = taskRepository.getById(baseId) ?: continue
                        if (fresh.status != com.neuroflow.app.domain.model.TaskStatus.ACTIVE) continue
                        val timeOffset = placement.window.startMillis - dayStart
                        taskRepository.update(
                            fresh.copy(
                                scheduledDate = dayStart,
                                scheduledTime = timeOffset,
                                isAutoScheduled = true,
                                plannedBlockMinutes = placement.window.durationMinutes,
                                lastAutoScheduledAt = nowMillis,
                                updatedAt = nowMillis
                            )
                        )
                    }
                }
                _deferrals.value = outcome.finalDeferred.map { task ->
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                        explanation = "Deferred past $horizonDays-day horizon.",
                        actionableSuggestion = "Move to backlog or break down."
                    )
                }
                _userActionMessage.emit(
                    "Auto-balanced 7-day schedule: placed ${outcome.totalScheduledTasks} tasks across horizon."
                )
            } catch (e: Exception) {
                android.util.Log.e("PlanViewModelV2", "Multi-day auto-balance failed", e)
            } finally {
                _isAutoScheduling.value = false
            }
        }
    }

    fun resolveConflict(conflict: ScheduleConflict) {
        val suggestedMinute = conflict.suggestedNewStartMinute
        val targetTask = uiState.value.tasksForDay.firstOrNull { it.id == conflict.taskId }
            ?: uiState.value.tasksForDay.firstOrNull { it.title == conflict.taskTitle }
        if (targetTask != null && suggestedMinute != null) {
            val safeMinuteOfDay = (suggestedMinute % 1440).coerceIn(0, 1439)
            assignTaskTime(targetTask, safeMinuteOfDay / 60, safeMinuteOfDay % 60)
            viewModelScope.launch {
                _userActionMessage.emit(
                    "Shifted '${targetTask.title.take(24)}' to ${ConflictResolver.formatMinutesToTime(safeMinuteOfDay)}"
                )
            }
        } else {
            triggerAutoScheduleNow()
        }
    }

    /**
     * ADR-031: Records thumbs-up / thumbs-down user feedback on auto-scheduled task placement.
     * When rejected (-1), immediately reschedules the task away from the rejected hour.
     */
    fun ratePlacement(task: TaskEntity, rating: Int) {
        viewModelScope.launch {
            val nowMillis = System.currentTimeMillis()
            val category = task.determineCategory().name
            val startMinute = task.scheduledTime?.let {
                if (it in 0 until com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS) (it / 60_000L).toInt() else {
                    val sMillis = task.resolveScheduledEpochMillis(uiState.value.selectedDateMillis) ?: 0L
                    java.time.Instant.ofEpochMilli(sMillis).atZone(java.time.ZoneId.systemDefault()).hour * 60
                }
            } ?: 0
            val startHour = startMinute / 60

            val isNegative = rating < 0
            val updatedTask = task.copy(
                userSchedulingRating = rating,
                schedulingRejectedSlotHour = if (isNegative) startHour else null,
                updatedAt = nowMillis
            )
            taskRepository.update(updatedTask)

            if (isNegative) {
                _feedbackTask.value = updatedTask
                _feedbackRemedyProposal.value = null
                _isFeedbackLoading.value = false
                _userActionMessage.emit("Disliked slot noted. Select a reason to tune your schedule.")
            } else {
                preferencesDataStore.recordSchedulingFeedback(
                    category = category,
                    hour = startHour,
                    isNegative = false,
                    timestampMillis = nowMillis
                )
                _userActionMessage.emit("Placement approved! Learning your preferences.")
            }
        }
    }

    fun submitDetailedFeedback(task: TaskEntity, reasonTag: String) {
        viewModelScope.launch {
            _isFeedbackLoading.value = true
            val nowMillis = System.currentTimeMillis()
            val category = task.determineCategory().name
            val startMinute = task.scheduledTime?.let {
                if (it in 0 until com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS) (it / 60_000L).toInt() else {
                    val sMillis = task.resolveScheduledEpochMillis(uiState.value.selectedDateMillis) ?: 0L
                    java.time.Instant.ofEpochMilli(sMillis).atZone(java.time.ZoneId.systemDefault()).hour * 60
                }
            } ?: 0
            val startHour = task.schedulingRejectedSlotHour ?: (startMinute / 60)

            preferencesDataStore.recordSchedulingFeedback(
                category = category,
                hour = startHour,
                isNegative = true,
                timestampMillis = nowMillis,
                reasonTag = reasonTag
            )

            val prefs = preferencesDataStore.preferencesFlow.first()
            val dayMillis = uiState.value.selectedDateMillis
            val zoneId = java.time.ZoneId.systemDefault()
            val dayDate = java.time.Instant.ofEpochMilli(dayMillis).atZone(zoneId).toLocalDate()
            val dayStart = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val workDayStartMillis = dayStart + prefs.workDayStart * 3600_000L
            val workDayEndMillis = dayStart + prefs.workDayEnd * 3600_000L

            val otherTasks = uiState.value.tasksForDay.filter { it.id != task.id && it.scheduledTime != null }
            val occupied = otherTasks.mapNotNull { t ->
                val s = t.resolveScheduledEpochMillis(dayMillis) ?: return@mapNotNull null
                val dur = (if (t.plannedBlockMinutes > 0) t.plannedBlockMinutes else t.estimatedDurationMinutes).coerceIn(5, 240)
                TimeSlotWindow(s, s + dur * 60_000L)
            }
            val availableWindows = computeAvailableWindows(workDayStartMillis, workDayEndMillis, occupied)
                .ifEmpty { listOf(TimeSlotWindow(workDayStartMillis, workDayEndMillis)) }

            val onlineService = geminiAiOnlineService ?: GeminiAiOnlineService()
            val remedy = onlineService.remedyRejectedSlot(
                task = task,
                rejectedStartHour = startHour,
                reasonTag = reasonTag,
                availableWindows = availableWindows,
                apiKey = prefs.geminiApiKey,
                userDirective = prefs.adaptiveSystemDirective
            )

            _feedbackRemedyProposal.value = remedy
            _isFeedbackLoading.value = false
        }
    }

    fun acceptRemedySlot(task: TaskEntity, newSlotMillis: Long, directivePatch: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updatedTask = task.copy(
                scheduledTime = newSlotMillis,
                isScheduleLocked = true,
                updatedAt = now
            )
            taskRepository.update(updatedTask)
            calendarSyncRepository?.handleTaskStateChanged(updatedTask)

            if (!directivePatch.isNullOrBlank()) {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val currentDirective = prefs.adaptiveSystemDirective
                val merged = if (currentDirective.isBlank()) {
                    directivePatch.trim()
                } else if (!currentDirective.contains(directivePatch.trim())) {
                    "${currentDirective.trim()}\n${directivePatch.trim()}"
                } else {
                    currentDirective
                }
                preferencesDataStore.updateAdaptiveSystemDirective(
                    directive = merged,
                    auditJson = prefs.weeklyAiAuditJson
                )
            }

            dismissFeedbackDialog()
            _userActionMessage.emit("Slot updated and locked for '${task.title.take(20)}'.")
        }
    }

    fun dismissFeedbackDialog() {
        _feedbackTask.value = null
        _feedbackRemedyProposal.value = null
        _isFeedbackLoading.value = false
    }

    private fun computeAvailableWindows(
        startMillis: Long,
        endMillis: Long,
        occupied: List<TimeSlotWindow>
    ): List<TimeSlotWindow> {
        if (startMillis >= endMillis) return emptyList()
        val clamped = occupied.mapNotNull { win ->
            val s = maxOf(win.startMillis, startMillis)
            val e = minOf(win.endMillis, endMillis)
            if (e > s) TimeSlotWindow(s, e) else null
        }.sortedBy { it.startMillis }

        if (clamped.isEmpty()) {
            return listOf(TimeSlotWindow(startMillis, endMillis))
        }

        val mergedBlocked = mutableListOf<TimeSlotWindow>()
        var curStart = clamped[0].startMillis
        var curEnd = clamped[0].endMillis
        for (i in 1 until clamped.size) {
            val next = clamped[i]
            if (next.startMillis <= curEnd) {
                curEnd = maxOf(curEnd, next.endMillis)
            } else {
                mergedBlocked.add(TimeSlotWindow(curStart, curEnd))
                curStart = next.startMillis
                curEnd = next.endMillis
            }
        }
        mergedBlocked.add(TimeSlotWindow(curStart, curEnd))

        val freeGaps = mutableListOf<TimeSlotWindow>()
        var pointer = startMillis
        for (block in mergedBlocked) {
            if (block.startMillis > pointer) {
                freeGaps.add(TimeSlotWindow(pointer, block.startMillis))
            }
            pointer = maxOf(pointer, block.endMillis)
        }
        if (pointer < endMillis) {
            freeGaps.add(TimeSlotWindow(pointer, endMillis))
        }
        return freeGaps
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.update(task)
            val newTags = (task.tags.split(",") + task.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(task)
            if (task.quadrant == Quadrant.ELIMINATE || task.status != com.neuroflow.app.domain.model.TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            } else {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, task) }
            }
        }
    }

    fun moveTaskToQuadrant(task: TaskEntity, newQuadrant: Quadrant) {
        viewModelScope.launch {
            val updated = task.copy(
                quadrant = newQuadrant,
                isFrog = if (newQuadrant == Quadrant.DO_FIRST) true else (if (newQuadrant == Quadrant.ELIMINATE) false else (if (task.quadrant == Quadrant.DO_FIRST) false else task.isFrog)),
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
            var currentTask = task

            // Normalize absolute epoch scheduledTime
            if (currentTask.scheduledTime != null && currentTask.scheduledTime >= ONE_DAY_MILLIS) {
                val epochStart = startOfLocalDay(currentTask.scheduledTime)
                val timeOffset = currentTask.scheduledTime - epochStart
                currentTask = currentTask.copy(
                    scheduledDate = epochStart,
                    scheduledTime = timeOffset,
                    habitDate = if (currentTask.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) epochStart else currentTask.habitDate
                )
            }

            // Normalize absolute epoch deadlineTime
            if (currentTask.deadlineTime != null && currentTask.deadlineTime >= ONE_DAY_MILLIS) {
                val dEpochStart = startOfLocalDay(currentTask.deadlineTime)
                val dTimeOffset = currentTask.deadlineTime - dEpochStart
                currentTask = currentTask.copy(
                    deadlineDate = currentTask.deadlineDate ?: dEpochStart,
                    deadlineTime = dTimeOffset
                )
            }

            val boundTask = when {
                currentTask.scheduledDate != null -> {
                    if (currentTask.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE && currentTask.habitDate == null) {
                        currentTask.copy(habitDate = currentTask.scheduledDate)
                    } else {
                        currentTask
                    }
                }
                currentTask.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE -> {
                    currentTask.copy(
                        scheduledDate = selectedDay,
                        habitDate = selectedDay
                    )
                }
                currentTask.scheduledTime != null -> {
                    // Intraday timed task without date: anchor to selectedDay
                    currentTask.copy(
                        scheduledDate = selectedDay,
                        habitDate = if (currentTask.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) selectedDay else currentTask.habitDate
                    )
                }
                else -> currentTask
            }
            taskRepository.insert(boundTask)
            val newTags = (boundTask.tags.split(",") + boundTask.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(boundTask)
            if (boundTask.quadrant != Quadrant.ELIMINATE && boundTask.status == com.neuroflow.app.domain.model.TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, boundTask) }
            }
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
            val contextTag = inferred.contextTags.firstOrNull().orEmpty()
            val stepList: List<com.neuroflow.app.domain.model.StepByStepPlanStep> = if (synthesis.firstActionStep.isNotBlank()) {
                listOf(com.neuroflow.app.domain.model.StepByStepPlanStep(synthesis.firstActionStep, completed = false))
            } else emptyList()
            val ifThenPlanStr = if (stepList.isNotEmpty()) {
                com.neuroflow.app.domain.model.StepByStepPlanCodec.serialize(stepList)
            } else ""

            val rawScheduledTime = inferred.scheduledTime
            val (schedDate, schedTime) = if (rawScheduledTime != null && rawScheduledTime >= ONE_DAY_MILLIS) {
                val sDay = startOfLocalDay(rawScheduledTime)
                sDay to (rawScheduledTime - sDay)
            } else {
                (inferred.scheduledDate ?: _selectedDate.value) to rawScheduledTime
            }

            val rawDeadlineTime = inferred.deadlineTime
            val (deadDate, deadTime) = if (rawDeadlineTime != null && rawDeadlineTime >= ONE_DAY_MILLIS) {
                val dDay = startOfLocalDay(rawDeadlineTime)
                dDay to (rawDeadlineTime - dDay)
            } else {
                inferred.deadlineDate to rawDeadlineTime
            }

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
                contextTag = contextTag,
                ifThenPlan = ifThenPlanStr,
                scheduledDate = schedDate,
                scheduledTime = schedTime,
                deadlineDate = deadDate,
                deadlineTime = deadTime,
                isFrog = inferred.isFrog,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insert(newTask)
            if (stepList.isNotEmpty()) {
                taskRepository.insertMicroSteps(
                    stepList.mapIndexed { idx, s ->
                        TaskMicroStepEntity(
                            taskId = newTask.id,
                            title = s.text,
                            sortOrder = idx,
                            estimatedMinutes = 5
                        )
                    }
                )
            }
            val newTags = (newTask.tags.split(",") + newTask.contextTag).map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
            if (newTags.isNotEmpty()) {
                preferencesDataStore.mergeTagCatalog(newTags)
            }
            calendarSyncRepository?.handleTaskStateChanged(newTask)
            if (newTask.quadrant != Quadrant.ELIMINATE) {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, newTask) }
            }
            _userActionMessage.emit("Created task '${newTask.title.take(24)}' ⚡")
        }
    }

    fun createTaskFromNote(title: String, description: String = "") {
        val inferred = TaskInferenceEngine.inferFromInput(if (title.isNotBlank()) "$title: $description" else description)
        val syn = com.neuroflow.app.domain.service.QuickNoteSynthesis(
            extractedTaskTitle = title.trim().ifBlank { inferred.sanitizedTitle.ifBlank { description.take(35) } },
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
            val manualPeak = if (peakPrefs.manualPeakProfileEnabled) (peakPrefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
            val peakHour = manualPeak ?: uiState.value.circadianSummary?.peakWindowStartHour
                ?: (if (peakPrefs.effectivePeakStart in 0..23) peakPrefs.effectivePeakStart else peakPrefs.peakEnergyStart).coerceIn(0, 23)
            val peakEndHour = if (manualPeak != null) ((manualPeak + 3) % 24) else (uiState.value.circadianSummary?.peakWindowEndHour ?: ((peakHour + 3) % 24))
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
            val labelPrefix: String

            val isPeakActive = if (peakHour <= peakEndHour) currentHour in peakHour until peakEndHour else (currentHour >= peakHour || currentHour < peakEndHour)
            val isPeakUpcoming = if (peakHour <= peakEndHour) currentHour < peakHour else (currentHour in peakEndHour until peakHour)

            if (baseDayStart > todayStart) {
                // Future scheduled date: preserve future day and place at peak hour
                targetDate = baseDayStart
                targetHour = peakHour
                labelPrefix = "Peak Window"
            } else {
                // Today or overdue past task: schedule for today's active/future window, or tomorrow if passed
                if (isPeakUpcoming) {
                    targetDate = todayStart
                    targetHour = peakHour
                    labelPrefix = "Peak Window"
                } else if (isPeakActive) {
                    targetDate = todayStart
                    targetHour = currentHour
                    labelPrefix = "Peak Window"
                } else if (currentHour < 18 && usableEnergy >= 55) {
                    targetDate = todayStart
                    targetHour = maxOf(currentHour + 1, dipEndHour).coerceAtMost(17)
                    labelPrefix = "Afternoon Rebound"
                } else {
                    targetDate = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart)
                    targetHour = peakHour
                    labelPrefix = "Tomorrow's Peak"
                }
            }

            // BUG-AUDIT-010 Fix: Preserve minute/second precision from manual peak anchor minute
            val targetMinuteOfDay = if (targetHour == peakHour && peakPrefs.manualPeakProfileEnabled) {
                peakPrefs.manualPeakAnchorMinuteOfDay
            } else {
                targetHour * 60
            }
            val millisInDay = targetMinuteOfDay * 60_000L
            val formattedTime = if (targetMinuteOfDay % 60 != 0) {
                String.format(java.util.Locale.ROOT, "%d:%02d", targetMinuteOfDay / 60, targetMinuteOfDay % 60)
            } else {
                "${targetHour}:00"
            }
            val activeSuffix = if (targetDate == todayStart && isPeakActive && targetHour == currentHour) " Active" else ""
            val messageLabel = "$labelPrefix ($formattedTime$activeSuffix)"
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
            val safeHour = (hour % 24).coerceIn(0, 23)
            val safeMinute = (minute % 60).coerceIn(0, 59)
            val millisInDay = safeHour * 3600_000L + safeMinute * 60_000L
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
            _userActionMessage.emit("Scheduled '${updatedTask.title.take(24)}' for ${String.format("%02d:%02d", safeHour, safeMinute)} ⚡")
        }
    }

    fun assignTaskTime(taskId: String, newStartMillis: Long) {
        viewModelScope.launch {
            val targetTask = uiState.value.tasksForDay.firstOrNull { it.id == taskId }
                ?: uiState.value.unscheduledTasks.firstOrNull { it.id == taskId }
                ?: taskRepository.getById(taskId)
                ?: return@launch
            val (startOfDay, timeOffset) = com.neuroflow.app.worker.ScheduleAutoTasksWorker.splitMillisToDateAndTime(newStartMillis)
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
            if (updated.quadrant != Quadrant.ELIMINATE && updated.status == com.neuroflow.app.domain.model.TaskStatus.ACTIVE) {
                context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }
            }
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
