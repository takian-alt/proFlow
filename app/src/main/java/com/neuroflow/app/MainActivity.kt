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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.presentation.common.GoalPeriod
import com.neuroflow.app.presentation.common.TopGoalsRefillCard
import com.neuroflow.app.presentation.v2.navigation.NeuroFlowAppV2
import com.neuroflow.app.presentation.common.NewChapterCard
import com.neuroflow.app.presentation.common.theme.NeuroFlowTheme
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
        when (intent.action) {
            "com.procus.ACTION_OPEN_NEW_TASK" -> {
                initialNewTaskOpen = true
                return
            }
            "com.procus.ACTION_OPEN_FOCUS" -> {
                if (!taskId.isNullOrBlank()) {
                    initialFocusTaskId = taskId
                } else {
                    lifecycleScope.launch {
                        val firstTask = taskRepository.getActiveTasks().firstOrNull()
                        if (firstTask != null) {
                            initialFocusTaskId = firstTask.id
                        }
                    }
                }
                return
            }
            "com.procus.ACTION_OPEN_GUIDE" -> {
                initialGuideOpen = true
                return
            }
            "com.procus.ACTION_OPEN_SCHEDULE" -> {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            sleepPressureRepository.refreshCurrentPressure()
            goalRepository.checkAndMigrateLegacyGoals()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()

        // Handle ACTION_OPEN_FOCUS intent
        handleIntent(intent)

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
                    old.lastYearlyGoalShownYear == new.lastYearlyGoalShownYear &&
                    old.lastWeeklyGoalShownWeek == new.lastWeeklyGoalShownWeek &&
                    old.lastWeeklyGoalShownYear == new.lastWeeklyGoalShownYear &&
                    old.lastYearlyGoalHandledYear == new.lastYearlyGoalHandledYear &&
                    old.lastWeeklyGoalHandledWeek == new.lastWeeklyGoalHandledWeek &&
                    old.firstAppOpenMillis == new.firstAppOpenMillis
                }
            }
            val preferences by preferencesFlow.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            // Track whether fresh-start card has been handled this session
            var freshStartHandled by remember { mutableStateOf(false) }
            // Track whether goals refill has been handled this session (skip = session-only)
            var yearlyGoalHandled by remember { mutableStateOf(false) }
            var weeklyGoalHandled by remember { mutableStateOf(false) }

            val isDark = when (preferences?.theme) {
                com.neuroflow.app.domain.model.AppTheme.DARK -> true
                com.neuroflow.app.domain.model.AppTheme.LIGHT -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            NeuroFlowTheme(
                darkTheme = isDark,
                dynamicColor = preferences?.dynamicColorEnabled ?: true,
                amoledDark = preferences?.amoledDarkEnabled ?: false
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
                                    // Map chronotype to peak hour range for UI display
                                    val (peakStart, peakEnd) = when (data.manualChronotype) {
                                        "MODERATE_MORNING", "DEFINITE_MORNING" -> 9 to 12
                                        "INTERMEDIATE" -> 12 to 17
                                        "MODERATE_EVENING", "DEFINITE_EVENING" -> 17 to 21
                                        else -> 9 to 12  // default
                                    }
                                    preferencesDataStore.updatePreferences { it.copy(
                                        identityLabel      = data.identityLabel,
                                        topGoal            = data.topGoal,
                                        wakeUpHour         = data.wakeUpHour,
                                        sleepHour          = data.sleepHour,
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
                                                quadrant                = Quadrant.DO_FIRST,
                                                impactScore             = 70,
                                                isFrog                  = true
                                            )
                                        )
                                    }
                                }
                            }
                        )
                        // Fresh-start check — show NewChapterCard if applicable
                        !freshStartHandled && FreshStartEngine.isFreshStart(
                            nowMillis = System.currentTimeMillis(),
                            lastOpenMillis = prefs.lastAppOpenMillis,
                            dailyStreak = prefs.dailyStreak,
                            lastActiveDate = prefs.lastActiveDate,
                            lastFreshStartShownWeek = prefs.lastFreshStartShownWeek,
                            lastFreshStartShownYear = prefs.lastFreshStartShownYear
                        ) -> NewChapterCard(
                            onConfirm = { intent ->
                                freshStartHandled = true
                                scope.launch {
                                    val now = System.currentTimeMillis()
                                    preferencesDataStore.updatePreferences { p ->
                                        p.copy(
                                            weeklyIntent = intent,
                                            weeklyIntentIsoWeek = FreshStartEngine.isoWeekNumber(now),
                                            weeklyIntentIsoYear = FreshStartEngine.isoYear(now),
                                            lastFreshStartShownWeek = FreshStartEngine.isoWeekNumber(now),
                                            lastFreshStartShownYear = FreshStartEngine.isoYear(now),
                                            lastAppOpenMillis = now
                                        )
                                    }
                                }
                            },
                            onDismiss = {
                                freshStartHandled = true
                                scope.launch {
                                    val now = System.currentTimeMillis()
                                    preferencesDataStore.updatePreferences { p ->
                                        p.copy(
                                            lastFreshStartShownWeek = FreshStartEngine.isoWeekNumber(now),
                                            lastFreshStartShownYear = FreshStartEngine.isoYear(now),
                                            lastAppOpenMillis = now
                                        )
                                    }
                                }
                            }
                        )
                        // Normal app
                        else -> {
                            // Update lastAppOpenMillis on normal launch — set flag synchronously
                            // to prevent repeated writes on every recompose
                            if (!freshStartHandled) {
                                freshStartHandled = true
                                scope.launch {
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
                            val currentYear = remember(nowMillis) { FreshStartEngine.isoYear(nowMillis) }
                            val currentWeek = remember(nowMillis) { FreshStartEngine.isoWeekNumber(nowMillis) }
                            val yearWeekKey = currentYear * 100 + currentWeek
                            val needsYearlyRefill = !yearlyGoalHandled &&
                                (prefs.lastYearlyGoalHandledYear != currentYear)
                            val needsWeeklyRefill = !weeklyGoalHandled && !needsYearlyRefill &&
                                (prefs.lastWeeklyGoalHandledWeek != yearWeekKey)

                            when {
                                needsYearlyRefill -> TopGoalsRefillCard(
                                    period = GoalPeriod.YEARLY,
                                    existingGoals = prefs.yearlyGoals,
                                    onConfirm = { goals ->
                                        yearlyGoalHandled = true
                                        scope.launch {
                                            goals.filter { it.isNotBlank() }.forEach { title ->
                                                goalRepository.insert(
                                                    com.neuroflow.app.data.local.entity.GoalEntity(
                                                        title = title,
                                                        description = "Yearly Objective"
                                                    )
                                                )
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
                                    existingGoals = prefs.weeklyGoals,
                                    onConfirm = { goals ->
                                        weeklyGoalHandled = true
                                        scope.launch {
                                            goals.filter { it.isNotBlank() }.forEach { title ->
                                                goalRepository.insert(
                                                    com.neuroflow.app.data.local.entity.GoalEntity(
                                                        title = title,
                                                        description = "Weekly Objective"
                                                    )
                                                )
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
                                        amoledDark = prefs.amoledDarkEnabled
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
