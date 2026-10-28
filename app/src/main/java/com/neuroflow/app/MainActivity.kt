package com.neuroflow.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.distinctUntilChanged
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.engine.FreshStartEngine
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.presentation.common.GoalPeriod
import com.neuroflow.app.presentation.common.TopGoalsRefillCard
import com.neuroflow.app.presentation.v2.navigation.NeuroFlowAppV2
import com.neuroflow.app.presentation.common.NewChapterCard
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2
import com.neuroflow.app.presentation.onboarding.OnboardingScreen
import com.neuroflow.app.worker.AutonomyNudgeWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesDataStore: UserPreferencesDataStore
    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var sleepPressureRepository: SleepPressureRepository
    @Inject lateinit var goalRepository: com.neuroflow.app.data.repository.GoalRepository

    private var initialFocusTaskId by mutableStateOf<String?>(null)
    private var initialWoopTaskId by mutableStateOf<String?>(null)
    private var initialGuideOpen by mutableStateOf(false)
    private var initialNewTaskOpen by mutableStateOf(false)
    private var initialScheduleOpen by mutableStateOf(false)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)  // Update the intent so onCreate can also handle it
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: intent.getStringExtra("task_id")
        val notificationId = intent.getIntExtra("notificationId", Int.MIN_VALUE)
        if (notificationId != Int.MIN_VALUE) {
            NotificationManagerCompat.from(this).cancel(notificationId)
        }
        val openWoop = intent.getBooleanExtra("openWoop", false)
        val action = intent.action

        // Clean up intent to prevent stale action/extras from re-triggering on configuration changes (BUG-311)
        intent.action = null
        intent.removeExtra("taskId")
        intent.removeExtra("task_id")
        intent.removeExtra("openWoop")
        intent.removeExtra("notificationId")

        if (action == null && !taskId.isNullOrBlank()) {
            if (openWoop) {
                initialWoopTaskId = taskId
            } else {
                initialFocusTaskId = taskId
            }
            return
        }
        when (action) {
            "com.procus.ACTION_OPEN_NEW_TASK", "com.neuroflow.app.ACTION_OPEN_NEW_TASK" -> {
                initialNewTaskOpen = true
                return
            }
            "com.procus.ACTION_OPEN_FOCUS", "com.neuroflow.app.ACTION_OPEN_FOCUS" -> {
                if (!taskId.isNullOrBlank()) {
                    if (openWoop) {
                        initialWoopTaskId = taskId
                    } else {
                        initialFocusTaskId = taskId
                    }
                } else {
                    lifecycleScope.launch {
                        runCatching {
                            val activeTasks = taskRepository.getActiveTasks()
                            val activeIds = activeTasks.map { it.id }.toSet()
                            val firstTask = activeTasks
                                .filter {
                                    it.status == TaskStatus.ACTIVE &&
                                    it.quadrant != Quadrant.ELIMINATE &&
                                    (it.dependsOnTaskIds.isBlank() || it.dependsOnTaskIds.split(",").none { id -> id.trim() in activeIds })
                                }
                                .sortedWith(
                                    compareByDescending<TaskEntity> { it.isFrog }
                                        .thenBy { it.quadrant != Quadrant.DO_FIRST }
                                        .thenBy { it.priority.ordinal }
                                )
                                .firstOrNull()
                            if (firstTask != null) {
                                initialFocusTaskId = firstTask.id
                            } else {
                                initialFocusTaskId = "tab_focus"
                            }
                        }.onFailure {
                            initialFocusTaskId = "tab_focus"
                        }
                    }
                }
                return
            }
            "com.procus.ACTION_OPEN_GUIDE", "com.neuroflow.app.ACTION_OPEN_GUIDE" -> {
                initialGuideOpen = true
                return
            }
            "com.procus.ACTION_OPEN_SCHEDULE", "com.neuroflow.app.ACTION_OPEN_SCHEDULE" -> {
                initialScheduleOpen = true
                return
            }
            "RESCHEDULE_NUDGE" -> {
                if (taskId == null) return
                val uniqueWorkName = AutonomyNudgeEngine.uniqueWorkName(taskId)
                // Re-enqueue AutonomyNudgeWorker with 1-hour delay
                val request = OneTimeWorkRequestBuilder<AutonomyNudgeWorker>()
                    .setInitialDelay(1, TimeUnit.HOURS)
                    .setInputData(workDataOf("taskId" to taskId))
                    .addTag(AutonomyNudgeEngine.workTag(taskId))
                    .addTag(AutonomyNudgeEngine.globalTag())
                    .build()
                WorkManager.getInstance(this).enqueueUniqueWork(
                    uniqueWorkName,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
            "WOOP_REFLECT" -> {
                initialWoopTaskId = taskId
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("saved_focus_task_id", initialFocusTaskId)
        outState.putString("saved_woop_task_id", initialWoopTaskId)
        outState.putBoolean("saved_guide_open", initialGuideOpen)
        outState.putBoolean("saved_schedule_open", initialScheduleOpen)
        outState.putBoolean("saved_new_task_open", initialNewTaskOpen)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            goalRepository.checkAndMigrateLegacyGoals()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()

        val hasPendingAction = !intent.action.isNullOrBlank() || intent.hasExtra("taskId") || intent.hasExtra("task_id")
        if (hasPendingAction) {
            handleIntent(intent)
        } else if (savedInstanceState != null) {
            initialFocusTaskId = savedInstanceState.getString("saved_focus_task_id")
            initialWoopTaskId = savedInstanceState.getString("saved_woop_task_id")
            initialGuideOpen = savedInstanceState.getBoolean("saved_guide_open", false)
            initialScheduleOpen = savedInstanceState.getBoolean("saved_schedule_open", false)
            initialNewTaskOpen = savedInstanceState.getBoolean("saved_new_task_open", false)
        }

        setContent {
            val preferencesFlow = remember(preferencesDataStore) {
                preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
                    old.theme == new.theme &&
                    old.dynamicColorEnabled == new.dynamicColorEnabled &&
                    old.amoledDarkEnabled == new.amoledDarkEnabled &&
                    old.onboardingCompleted == new.onboardingCompleted &&
                    old.lastAppOpenMillis == new.lastAppOpenMillis &&
                    old.dailyStreak == new.dailyStreak &&
                    old.lastActiveDate == new.lastActiveDate &&
                    old.lastFreshStartShownWeek == new.lastFreshStartShownWeek &&
                    old.lastFreshStartShownYear == new.lastFreshStartShownYear &&
                    old.yearlyGoals == new.yearlyGoals &&
                    old.weeklyGoals == new.weeklyGoals &&
                    old.lastYearlyGoalHandledYear == new.lastYearlyGoalHandledYear &&
                    old.lastWeeklyGoalHandledWeek == new.lastWeeklyGoalHandledWeek &&
                    old.firstAppOpenMillis == new.firstAppOpenMillis
                }
            }
            val preferences by preferencesFlow.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            // Track whether fresh-start card has been handled this session (persisted across rotation - BUG-311)
            var freshStartHandled by rememberSaveable { mutableStateOf(false) }
            // Track whether goals refill has been handled this session (skip = session-only, persisted across rotation - BUG-311)
            var yearlyGoalHandled by rememberSaveable { mutableStateOf(false) }
            var weeklyGoalHandled by rememberSaveable { mutableStateOf(false) }

            val pendingDirectIntent = initialFocusTaskId != null ||
                initialWoopTaskId != null ||
                initialGuideOpen ||
                initialScheduleOpen ||
                initialNewTaskOpen
            var directIntentSessionHandled by rememberSaveable { mutableStateOf(pendingDirectIntent) }
            if (pendingDirectIntent && !directIntentSessionHandled) {
                directIntentSessionHandled = true
            }
            val hasDirectIntent = pendingDirectIntent || directIntentSessionHandled

            val isDark = when (preferences?.theme) {
                com.neuroflow.app.domain.model.AppTheme.DARK -> true
                com.neuroflow.app.domain.model.AppTheme.LIGHT -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            NeuroFlowThemeV2(
                darkTheme = isDark,
                amoledDark = preferences?.amoledDarkEnabled ?: false,
                dynamicColor = preferences?.dynamicColorEnabled ?: false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val prefs = preferences
                    when {
                        // Still loading — render nothing to avoid flash
                        prefs == null -> Unit
                        // First launch — show onboarding
                        !prefs.onboardingCompleted -> OnboardingScreen(
                            onComplete = { data ->
                                scope.launch {
                                    // Bug 6 Fix (ADR-025): Delegate directly to MEQChronotypeDetector to prevent peak window contradiction
                                    val chronotype = try {
                                        com.neuroflow.app.domain.engine.MEQChronotypeDetector.Chronotype.valueOf(data.manualChronotype)
                                    } catch (e: Exception) {
                                        com.neuroflow.app.domain.engine.MEQChronotypeDetector.Chronotype.INTERMEDIATE
                                    }
                                    val (peakStart, peakEnd) = com.neuroflow.app.domain.engine.MEQChronotypeDetector.baselinePeakWindow(chronotype)
                                    preferencesDataStore.updatePreferences { it.copy(
                                        identityLabel      = data.identityLabel,
                                        topGoal            = data.topGoal,
                                        wakeUpHour         = data.wakeUpHour,
                                        sleepHour          = if (data.sleepHour >= 24) data.sleepHour % 24 else data.sleepHour,
                                        peakEnergyStart    = peakStart,
                                        peakEnergyEnd      = peakEnd,
                                        manualChronotype   = data.manualChronotype,
                                        onboardingCompleted = true,
                                        firstAppOpenMillis = if (it.firstAppOpenMillis == 0L) System.currentTimeMillis() else it.firstAppOpenMillis
                                    )}
                                    // Insert the "first task" as a DO_FIRST task if provided, using inference
                                    if (data.firstTask.isNotBlank()) {
                                        val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(data.firstTask)
                                        taskRepository.insert(
                                            TaskEntity(
                                                title                   = inferred.sanitizedTitle.ifBlank { data.firstTask.trim() },
                                                estimatedDurationMinutes = inferred.durationMinutes,
                                                energyLevel             = inferred.energyDemand,
                                                taskType                = inferred.taskType,
                                                tags                    = inferred.contextTags.joinToString(","),
                                                priority                = inferred.priority,
                                                deadlineDate            = inferred.deadlineDate ?: inferred.scheduledDate,
                                                deadlineTime            = inferred.deadlineTime ?: inferred.scheduledTime,
                                                quadrant                = Quadrant.DO_FIRST,
                                                impactScore             = 70,
                                                isFrog                  = true
                                            )
                                        )
                                    }
                                }
                            }
                        )
                        // Fresh-start check — show NewChapterCard if applicable and no direct intent action pending
                        run {
                            !hasDirectIntent && !freshStartHandled && FreshStartEngine.isFreshStart(
                                nowMillis = System.currentTimeMillis(),
                                lastOpenMillis = prefs.lastAppOpenMillis,
                                dailyStreak = prefs.dailyStreak,
                                lastActiveDate = prefs.lastActiveDate,
                                lastFreshStartShownWeek = prefs.lastFreshStartShownWeek,
                                lastFreshStartShownYear = prefs.lastFreshStartShownYear
                            )
                        } -> NewChapterCard(
                            onConfirm = { intent ->
                                freshStartHandled = true
                                scope.launch {
                                    val now = System.currentTimeMillis()
                                    val anchorRef = FreshStartEngine.sundayWeekAnchorMillis(now)
                                    val isoWeek = FreshStartEngine.isoWeekNumber(anchorRef)
                                    val isoYear = FreshStartEngine.isoYear(anchorRef)
                                    preferencesDataStore.updatePreferences { p ->
                                        p.copy(
                                            weeklyIntent = intent,
                                            weeklyIntentIsoWeek = isoWeek,
                                            weeklyIntentIsoYear = isoYear,
                                            lastFreshStartShownWeek = isoWeek,
                                            lastFreshStartShownYear = isoYear,
                                            lastAppOpenMillis = now
                                        )
                                    }
                                }
                            },
                            onDismiss = {
                                freshStartHandled = true
                                scope.launch {
                                    val now = System.currentTimeMillis()
                                    val anchorRef = FreshStartEngine.sundayWeekAnchorMillis(now)
                                    val isoWeek = FreshStartEngine.isoWeekNumber(anchorRef)
                                    val isoYear = FreshStartEngine.isoYear(anchorRef)
                                    preferencesDataStore.updatePreferences { p ->
                                        p.copy(
                                            lastFreshStartShownWeek = isoWeek,
                                            lastFreshStartShownYear = isoYear,
                                            lastAppOpenMillis = now
                                        )
                                    }
                                }
                            }
                        )
                        // Normal app
                        else -> {
                            // Update lastAppOpenMillis on normal launch inside LaunchedEffect to prevent side effects in composition
                            LaunchedEffect(Unit) {
                                if (!freshStartHandled) {
                                    freshStartHandled = true
                                    preferencesDataStore.updatePreferences { p ->
                                        p.copy(
                                            lastAppOpenMillis = System.currentTimeMillis(),
                                            firstAppOpenMillis = if (p.firstAppOpenMillis == 0L) {
                                                if (p.onboardingCompleted) {
                                                    System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
                                                } else {
                                                    System.currentTimeMillis()
                                                }
                                            } else {
                                                p.firstAppOpenMillis
                                            }
                                        )
                                    }
                                }
                            }
                            // Compute once per composition — stable within a session
                            val nowMillis = remember { System.currentTimeMillis() }
                            val weekAnchorMillis = remember(nowMillis) { FreshStartEngine.sundayWeekAnchorMillis(nowMillis) }
                            val currentYear = remember(weekAnchorMillis) { FreshStartEngine.isoYear(weekAnchorMillis) }
                            val currentWeek = remember(weekAnchorMillis) { FreshStartEngine.isoWeekNumber(weekAnchorMillis) }
                            val yearWeekKey = currentYear * 100 + currentWeek
                            val activeGoals by goalRepository.observeActiveGoals().collectAsState(initial = emptyList())
                            val activeYearlyGoalTitles = remember(activeGoals) {
                                activeGoals.filter { it.description == "Yearly Objective" }
                                    .map { it.title }.filter { it.isNotBlank() }
                            }
                            val activeWeeklyGoalTitles = remember(activeGoals) {
                                activeGoals.filter { it.description == "Weekly Objective" || (it.description != "Yearly Objective" && it.description.isBlank()) }
                                    .map { it.title }.filter { it.isNotBlank() }
                            }
                            val needsYearlyRefill = !hasDirectIntent && !yearlyGoalHandled &&
                                (prefs.lastYearlyGoalHandledYear != currentYear)
                            val needsWeeklyRefill = !hasDirectIntent && !weeklyGoalHandled && !needsYearlyRefill &&
                                (prefs.lastWeeklyGoalHandledWeek != yearWeekKey)

                            when {
                                needsYearlyRefill -> TopGoalsRefillCard(
                                    period = GoalPeriod.YEARLY,
                                    existingGoals = activeYearlyGoalTitles.ifEmpty { prefs.yearlyGoals },
                                    onConfirm = { goals ->
                                        yearlyGoalHandled = true
                                        scope.launch {
                                            val existingGoals = goalRepository.getActiveGoals()
                                            val existingYearlyGoals = existingGoals.filter { it.description == "Yearly Objective" }
                                            val validGoals = goals.filter { it.isNotBlank() }
                                                .distinctBy { it.trim().lowercase() }
                                            val validLowerSet = validGoals.map { it.trim().lowercase() }.toSet()
                                            // Reconcile and delete removed yearly goals in Room without wiping weekly goals (BUG-180)
                                            existingYearlyGoals.forEach { existing ->
                                                if (existing.title.trim().lowercase() !in validLowerSet) {
                                                    goalRepository.delete(existing)
                                                }
                                            }
                                            val existingTitles = existingYearlyGoals.map { it.title.trim().lowercase() }.toSet()
                                            validGoals.forEach { title ->
                                                val lower = title.trim().lowercase()
                                                if (lower !in existingTitles) {
                                                    goalRepository.insert(
                                                        com.neuroflow.app.data.local.entity.GoalEntity(
                                                            title = title.trim(),
                                                            description = "Yearly Objective"
                                                        )
                                                    )
                                                }
                                            }
                                            preferencesDataStore.setLastYearlyGoalHandledYear(currentYear)
                                        }
                                    },
                                    onSkip = {
                                        yearlyGoalHandled = true
                                        scope.launch {
                                            preferencesDataStore.setLastYearlyGoalHandledYear(currentYear)
                                        }
                                    }
                                )
                                needsWeeklyRefill -> TopGoalsRefillCard(
                                    period = GoalPeriod.WEEKLY,
                                    existingGoals = activeWeeklyGoalTitles.ifEmpty { prefs.weeklyGoals },
                                    onConfirm = { goals ->
                                        weeklyGoalHandled = true
                                        scope.launch {
                                            val existingGoals = goalRepository.getActiveGoals()
                                            val existingWeeklyGoals = existingGoals.filter { it.description == "Weekly Objective" || (it.description != "Yearly Objective" && it.description.isBlank()) }
                                            val validGoals = goals.filter { it.isNotBlank() }
                                                .distinctBy { it.trim().lowercase() }
                                            val validLowerSet = validGoals.map { it.trim().lowercase() }.toSet()
                                            // Reconcile and delete removed weekly goals in Room without wiping yearly goals (BUG-180)
                                            existingWeeklyGoals.forEach { existing ->
                                                if (existing.title.trim().lowercase() !in validLowerSet) {
                                                    goalRepository.delete(existing)
                                                }
                                            }
                                            val existingTitles = existingWeeklyGoals.map { it.title.trim().lowercase() }.toSet()
                                            validGoals.forEach { title ->
                                                val lower = title.trim().lowercase()
                                                if (lower !in existingTitles) {
                                                    goalRepository.insert(
                                                        com.neuroflow.app.data.local.entity.GoalEntity(
                                                            title = title.trim(),
                                                            description = "Weekly Objective"
                                                        )
                                                    )
                                                }
                                            }
                                            preferencesDataStore.setLastWeeklyGoalHandledWeek(yearWeekKey)
                                        }
                                    },
                                    onSkip = {
                                        weeklyGoalHandled = true
                                        scope.launch {
                                            preferencesDataStore.setLastWeeklyGoalHandledWeek(yearWeekKey)
                                        }
                                    }
                                )
                                else -> {
                                    NeuroFlowAppV2(
                                        initialTaskId = initialFocusTaskId,
                                        onInitialTaskConsumed = { initialFocusTaskId = null },
                                        initialOpenComposer = initialNewTaskOpen,
                                        onInitialComposerConsumed = { initialNewTaskOpen = false },
                                        initialWoopTaskId = initialWoopTaskId,
                                        onInitialWoopConsumed = { initialWoopTaskId = null },
                                        initialGuideOpen = initialGuideOpen,
                                        onInitialGuideConsumed = { initialGuideOpen = false },
                                        initialScheduleOpen = initialScheduleOpen,
                                        onInitialScheduleConsumed = { initialScheduleOpen = false },
                                        darkTheme = isDark,
                                        amoledDark = prefs.amoledDarkEnabled,
                                        dynamicColor = prefs.dynamicColorEnabled
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            sleepPressureRepository.refreshCurrentPressure()
        }
    }
}
