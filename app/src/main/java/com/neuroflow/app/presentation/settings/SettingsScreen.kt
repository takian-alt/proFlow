package com.neuroflow.app.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.BuildConfig
import com.neuroflow.app.domain.model.AppTheme
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.presentation.common.EnergyInsight
import com.neuroflow.app.presentation.common.TaskTagViewModel
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPriorityWeights: () -> Unit,
    onNavigateToHyperFocusAllowlist: () -> Unit = {},
    onNavigateToAppGuide: () -> Unit = {},
    onNavigateToPrivacyPermissions: () -> Unit = {},
    onNavigateToMEQQuiz: () -> Unit = {},
    onNavigateToSleepLogs: () -> Unit = {},
    onNavigateToAccountSync: () -> Unit = {},
    onNavigateToLogTime: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val circadianSummary by viewModel.circadianSummary.collectAsStateWithLifecycle()
    val aiTestStatus by viewModel.aiTestStatus.collectAsStateWithLifecycle()
    val isTestingAi by viewModel.isTestingAi.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showClearTelemetryDialog by remember { mutableStateOf(false) }
    var showDistractingAppsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Categorized Settings Hub (Chronobiology, Schedulers, HyperFocus, AI & Cloud Sync)
            com.neuroflow.app.presentation.v2.settings.CategorizedSettingsHubV2(
                chronotypeSummary = prefs.quizChronotype ?: "Anchor: ${prefs.wakeUpHour}:00 AM",
                schedulerSummary = "Auto-Schedule & Prioritization Weights",
                hyperFocusSummary = "App blocking, emergency exits, rewards",
                aiAndSyncSummary = if (prefs.geminiApiKey.isNotBlank()) "Gemini Flash BYOK Active" else "Offline Heuristics Active",
                onOpenChronobiologyAndSleep = onNavigateToSleepLogs,
                onOpenSchedulerAndWeights = onNavigateToPriorityWeights,
                onOpenHyperFocusAndPermissions = onNavigateToHyperFocusAllowlist,
                onOpenAiAndCloudSync = onNavigateToAccountSync
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Appearance & Theming
            SettingsSection("Appearance") {
                Text(
                    "Theme Mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        AppTheme.SYSTEM to "System",
                        AppTheme.LIGHT to "Light",
                        AppTheme.DARK to "Dark"
                    )
                    themes.forEach { (themeOption, label) ->
                        val isSelected = prefs.theme == themeOption
                        OutlinedButton(
                            onClick = { viewModel.setTheme(themeOption) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingsToggleRow(
                    label = "Pure Black AMOLED",
                    description = "Use pitch-black backgrounds for OLED battery savings",
                    checked = prefs.amoledDarkEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(amoledDarkEnabled = it) } }
                )
            }

            // AI & Task Decomposition
            SettingsSection("Gemini Flash AI Engine") {
                SettingsToggleRow(
                    label = "Gemini Flash AI Services",
                    description = "Uses gemini-flash-latest for task enrichment, circadian slot matching, and weekly avoidance audits",
                    checked = prefs.geminiAiEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(geminiAiEnabled = it) } }
                )
                if (prefs.geminiAiEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Engine status badge
                    val isKeyConfigured = prefs.geminiApiKey.isNotBlank()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isKeyConfigured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isKeyConfigured) "● Active: gemini-flash-latest (Personal BYOK Key)" else "○ Offline Mode: Built-in Neuroscience Heuristics Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isKeyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    var apiKey by rememberSaveable(prefs.geminiApiKey) { mutableStateOf(prefs.geminiApiKey) }
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            viewModel.updatePreferences { p -> p.copy(geminiApiKey = it.trim()) }
                        },
                        label = { Text("Gemini API Key (BYOK)") },
                        placeholder = { Text("Enter your Gemini API key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text(
                                "Stored 100% privately on-device. Uses gemini-flash-latest with single-call enrichment and 20-min TTL caching to conserve quota.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.testGeminiConnection() },
                            enabled = !isTestingAi && prefs.geminiApiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isTestingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test AI Connection", fontSize = 11.sp)
                            }
                        }
                    }

                    if (!aiTestStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val isSuccess = aiTestStatus?.startsWith("✅") == true
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiTestStatus ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (prefs.adaptiveSystemDirective.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🧠 Calibrated Adaptive Directive",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prefs.adaptiveSystemDirective,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Profile & Identity
            SettingsSection("Profile & Identity") {

                // Sync local state when prefs load from DataStore (avoids stale initial value)
                var identityLabel by remember { mutableStateOf(prefs.identityLabel) }
                LaunchedEffect(prefs.identityLabel) { identityLabel = prefs.identityLabel }
                OutlinedTextField(
                    value = identityLabel,
                    onValueChange = {
                        identityLabel = it
                        viewModel.updatePreferences { p -> p.copy(identityLabel = it) }
                    },
                    label = { Text("I am a...") },
                    placeholder = { Text("e.g. Deep Worker, Consistent Learner") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                var topGoal by remember { mutableStateOf(prefs.topGoal) }
                LaunchedEffect(prefs.topGoal) { topGoal = prefs.topGoal }
                OutlinedTextField(
                    value = topGoal,
                    onValueChange = {
                        topGoal = it
                        viewModel.updatePreferences { p -> p.copy(topGoal = it) }
                    },
                    label = { Text("My top goal") },
                    placeholder = { Text("What are you working towards?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsNumberRow("Default Wake Hour", prefs.wakeUpHour, 0, 23, formatHour = true) {
                    viewModel.updatePreferences { p -> p.copy(wakeUpHour = it) }
                }
                SettingsNumberRow("Default Sleep Hour", prefs.sleepHour, 0, 23, formatHour = true) {
                    viewModel.updatePreferences { p -> p.copy(sleepHour = it) }
                }
                SettingsNumberRow("Peak Energy Start", prefs.peakEnergyStart, 0, 23, formatHour = true) {
                    viewModel.updatePreferences { p -> p.copy(peakEnergyStart = it) }
                }
                SettingsNumberRow("Peak Energy End", prefs.peakEnergyEnd, 0, 23, formatHour = true) {
                    // Must be after peak start
                    viewModel.updatePreferences { p -> p.copy(peakEnergyEnd = maxOf(it, p.peakEnergyStart + 1)) }
                }
                if (prefs.peakEnergyEnd <= prefs.peakEnergyStart) {
                    Text(
                        "Peak end must be after peak start",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }

            // Work Schedule
            SettingsCompactBox(title = "Work Schedule", collapsedByDefault = true) {
                SettingsNumberRow("Work Day Start", prefs.workDayStart, 0, 23, formatHour = true) {
                    viewModel.updatePreferences { p -> p.copy(workDayStart = it) }
                }
                SettingsNumberRow("Work Day End", prefs.workDayEnd, 0, 23, formatHour = true) {
                    // Must be after work start
                    viewModel.updatePreferences { p -> p.copy(workDayEnd = maxOf(it, p.workDayStart + 1)) }
                }
                if (prefs.workDayEnd <= prefs.workDayStart) {
                    Text(
                        "Work end must be after work start",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        "Work window: ${hourLabel(prefs.workDayStart)} – ${hourLabel(prefs.workDayEnd)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                SettingsNumberRow("Pomodoro (minutes)", prefs.defaultPomodoroMinutes, 5, 60) {
                    viewModel.updatePreferences { p -> p.copy(defaultPomodoroMinutes = it) }
                }
                SettingsNumberRow("Break (minutes)", prefs.defaultBreakMinutes, 1, 30) {
                    viewModel.updatePreferences { p -> p.copy(defaultBreakMinutes = it) }
                }
            }

            // Focus Behaviour + Neuro Booster automation
            SettingsSection("Focus Behaviour") {
                SettingsToggleRow(
                    label = "WOOP planning prompt",
                    description = "Show the WOOP sheet when opening a task for the first time",
                    checked = prefs.woopEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(woopEnabled = it) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    label = "Auto-start tracker",
                    description = "Automatically starts tracking after 8 seconds on the focus screen",
                    checked = prefs.autoTrackerEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(autoTrackerEnabled = it) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    label = "Do Not Disturb (DND) during focus",
                    description = "Automatically mute notifications during active focus sprints",
                    checked = prefs.dndEnabled,
                    onCheckedChange = { viewModel.updatePreferences { p -> p.copy(dndEnabled = it) } }
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingsDeepWorkTargetRow(
                    minutes = prefs.dailyDeepWorkTargetMinutes,
                    onChange = { target ->
                        viewModel.updatePreferences { p -> p.copy(dailyDeepWorkTargetMinutes = target) }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToLogTime() },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text("Manual Time Logging", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Log past offline sessions and deep work time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            SettingsSection("Task Tags Catalog") {
                val tagViewModel: TaskTagViewModel = hiltViewModel()
                val tags by tagViewModel.tags.collectAsStateWithLifecycle()
                var newTagInput by remember { mutableStateOf("") }

                Text(
                    "Manage your custom task tags and labels used across planning filters.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = { Text("Add tag (e.g. #deepwork)", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            if (newTagInput.isNotBlank()) {
                                tagViewModel.addTags(listOf(newTagInput.trim()))
                                newTagInput = ""
                            }
                        },
                        enabled = newTagInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add")
                    }
                }

                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(tag, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove tag",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { tagViewModel.removeTag(tag) }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            SettingsSection("Distraction Tracking") {
                DistractionPermissionRow()
                Spacer(modifier = Modifier.height(10.dp))
                DistractingAppsRow(
                    count = prefs.distractingPackages.size,
                    onOpen = {
                        viewModel.loadLaunchableApps()
                        showDistractingAppsDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "The apps you flag here are what the Today screen weighs against your deep-work time.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(distractingPackages = BehavioralSupportService.DEFAULT_DISTRACTING_PACKAGES)
                        }
                    }
                ) {
                    Text("Reset to default list", fontSize = 12.sp)
                }
            }

            SettingsSection("Notifications") {
                SettingsToggleRow(
                    label = "Daily plan notification",
                    description = "Send your top-3 planning notification each morning",
                    checked = prefs.dailyPlanNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(dailyPlanNotificationsEnabled = it) }
                    }
                )
                if (prefs.dailyPlanNotificationsEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsNumberRow(
                        label = "Daily plan hour",
                        value = prefs.dailyPlanNotificationHour,
                        min = 0,
                        max = 23,
                        formatHour = true
                    ) {
                        viewModel.updatePreferences { p -> p.copy(dailyPlanNotificationHour = it) }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    label = "Streak check notification",
                    description = "Warn when your streak is at risk in the evening",
                    checked = prefs.streakNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(streakNotificationsEnabled = it) }
                    }
                )
                if (prefs.streakNotificationsEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsNumberRow(
                        label = "Streak check hour",
                        value = prefs.streakCheckNotificationHour,
                        min = 0,
                        max = 23,
                        formatHour = true
                    ) {
                        viewModel.updatePreferences { p -> p.copy(streakCheckNotificationHour = it) }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    label = "Autonomy nudge notifications",
                    description = "Allow 2-hour nudges for untouched tasks",
                    checked = prefs.autonomyNudgeNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(autonomyNudgeNotificationsEnabled = it) }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    label = "Deadline reminder notifications",
                    description = "Allow 15m/30m/1h/1d deadline reminders",
                    checked = prefs.deadlineReminderNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(deadlineReminderNotificationsEnabled = it) }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    label = "Deadline escalation alerts",
                    description = "Allow schedule-to-do-first escalation notifications",
                    checked = prefs.deadlineEscalationNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences { p -> p.copy(deadlineEscalationNotificationsEnabled = it) }
                    }
                )
            }

            // Features → Energy
            SettingsSection("Energy") {
                SettingsToggleRow(
                    label = "Use MEQ quiz peak",
                    description = "When off, manual Peak Energy Start/End are used even if quiz results exist",
                    checked = prefs.quizPeakEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updatePreferences { p -> p.copy(quizPeakEnabled = enabled) }
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onNavigateToMEQQuiz,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (prefs.quizChronotype != null) "Retake MEQ Chronotype Quiz" else "Take MEQ Chronotype Quiz")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (prefs.quizPeakEnabled) {
                        "Scoring source: MEQ quiz peak when available"
                    } else {
                        "Scoring source: manual peak window"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val peakStart = circadianSummary?.peakWindowStartHour ?: prefs.peakEnergyStart
                val peakEnd = circadianSummary?.peakWindowEndHour ?: prefs.peakEnergyEnd
                val dipStart = circadianSummary?.dipWindowStartHour ?: 14
                val dipEnd = circadianSummary?.dipWindowEndHour ?: 16
                val peakScore = circadianSummary?.peakEnergyScore ?: 80
                val deepWorkMins = circadianSummary?.recommendedDeepWorkMinutes ?: 180

                Text(
                    text = "Effective peak window: ${EnergyInsight.minuteLabel(peakStart * 60)} – ${EnergyInsight.minuteLabel(peakEnd * 60)} ($peakScore score)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Afternoon dip window: ${EnergyInsight.minuteLabel(dipStart * 60)} – ${EnergyInsight.minuteLabel(dipEnd * 60)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Recommended deep work capacity: ${deepWorkMins}m",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!prefs.quizPeakEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tip: turn quiz peak on to use personalized morning adjustments from sleep behavior.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingsCompactBox(title = "Profile Controls", collapsedByDefault = true) {
                    SettingsToggleRow(
                        label = "Manual profile override",
                        description = "Replace adaptive profile with your custom anchor, windows, and amplitudes",
                        checked = prefs.manualPeakProfileEnabled,
                        onCheckedChange = {
                            viewModel.updatePreferences { p -> p.copy(manualPeakProfileEnabled = it) }
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Profile type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("AUTO", "WORKDAY", "WEEKEND").forEach { mode ->
                            FilterChip(
                                selected = prefs.manualPeakProfileType == mode,
                                enabled = prefs.manualPeakProfileEnabled,
                                onClick = { viewModel.updatePreferences { p -> p.copy(manualPeakProfileType = mode) } },
                                label = { Text(mode.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                            )
                        }
                    }

                    val selectedChronotype = prefs.quizChronotype ?: prefs.manualChronotype
                    val chronotypePreset = manualProfilePresetForChronotype(
                        chronotype = selectedChronotype,
                        wakeUpHour = prefs.wakeUpHour
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Chronotype presets", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Current preset: ${chronotypePreset.label}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = prefs.manualPeakProfileEnabled,
                            onClick = {
                                val preset = manualProfilePresetForChronotype(
                                    chronotype = "INTERMEDIATE",
                                    wakeUpHour = prefs.wakeUpHour
                                )
                                viewModel.updatePreferences { p ->
                                    p.copy(
                                        manualPeakProfileType = "AUTO",
                                        manualPeakAnchorMinuteOfDay = preset.anchorMinuteOfDay,
                                        manualPeakWindow1StartOffsetMinutes = preset.window1StartOffsetMinutes,
                                        manualPeakWindow2StartOffsetMinutes = preset.window2StartOffsetMinutes,
                                        manualPeakWindow3StartOffsetMinutes = preset.window3StartOffsetMinutes,
                                        manualPeakWindow1DurationMinutes = preset.window1DurationMinutes,
                                        manualPeakWindow2DurationMinutes = preset.window2DurationMinutes,
                                        manualPeakWindow3DurationMinutes = preset.window3DurationMinutes,
                                        manualPeakWindow1Amplitude = preset.window1Amplitude,
                                        manualPeakWindow2Amplitude = preset.window2Amplitude,
                                        manualPeakWindow3Amplitude = preset.window3Amplitude
                                    )
                                }
                            }
                        ) {
                            Text("Apply Intermediate")
                        }
                        OutlinedButton(
                            enabled = prefs.manualPeakProfileEnabled,
                            onClick = {
                                val preset = manualProfilePresetForChronotype(
                                    chronotype = "DEFINITE_EVENING",
                                    wakeUpHour = prefs.wakeUpHour
                                )
                                viewModel.updatePreferences { p ->
                                    p.copy(
                                        manualPeakProfileType = "AUTO",
                                        manualPeakAnchorMinuteOfDay = preset.anchorMinuteOfDay,
                                        manualPeakWindow1StartOffsetMinutes = preset.window1StartOffsetMinutes,
                                        manualPeakWindow2StartOffsetMinutes = preset.window2StartOffsetMinutes,
                                        manualPeakWindow3StartOffsetMinutes = preset.window3StartOffsetMinutes,
                                        manualPeakWindow1DurationMinutes = preset.window1DurationMinutes,
                                        manualPeakWindow2DurationMinutes = preset.window2DurationMinutes,
                                        manualPeakWindow3DurationMinutes = preset.window3DurationMinutes,
                                        manualPeakWindow1Amplitude = preset.window1Amplitude,
                                        manualPeakWindow2Amplitude = preset.window2Amplitude,
                                        manualPeakWindow3Amplitude = preset.window3Amplitude
                                    )
                                }
                            }
                        ) {
                            Text("Apply Night Owl")
                        }
                    }
                    Text(
                        text = "Presets use wake-up anchored peaks. You can edit each window start offset below.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val anchorHour = (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
                    val anchorMinute = (prefs.manualPeakAnchorMinuteOfDay % 60).coerceIn(0, 59)
                    SettingsNumberRow(
                        "Anchor Hour",
                        anchorHour,
                        0,
                        23,
                        formatHour = true,
                        enabled = prefs.manualPeakProfileEnabled
                    ) { hour ->
                        viewModel.updatePreferences { p ->
                            p.copy(manualPeakAnchorMinuteOfDay = hour * 60 + anchorMinute)
                        }
                    }
                    SettingsNumberRow(
                        "Anchor Minute",
                        anchorMinute,
                        0,
                        59,
                        enabled = prefs.manualPeakProfileEnabled
                    ) { minute ->
                        viewModel.updatePreferences { p ->
                            p.copy(manualPeakAnchorMinuteOfDay = anchorHour * 60 + minute)
                        }
                    }
                    SettingsNumberRow(
                        "Window 1 Offset (min)",
                        prefs.manualPeakWindow1StartOffsetMinutes,
                        0,
                        1439,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow1StartOffsetMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 2 Offset (min)",
                        prefs.manualPeakWindow2StartOffsetMinutes,
                        0,
                        1439,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow2StartOffsetMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 3 Offset (min)",
                        prefs.manualPeakWindow3StartOffsetMinutes,
                        0,
                        1439,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow3StartOffsetMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 1 Duration (min)",
                        prefs.manualPeakWindow1DurationMinutes,
                        30,
                        360,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow1DurationMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 2 Duration (min)",
                        prefs.manualPeakWindow2DurationMinutes,
                        30,
                        360,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow2DurationMinutes = it) }
                    }
                    SettingsNumberRow(
                        "Window 3 Duration (min)",
                        prefs.manualPeakWindow3DurationMinutes,
                        30,
                        360,
                        enabled = prefs.manualPeakProfileEnabled
                    ) {
                        viewModel.updatePreferences { p -> p.copy(manualPeakWindow3DurationMinutes = it) }
                    }
                    SettingsFloatSliderRow(
                        label = "Window 1 Amplitude",
                        value = prefs.manualPeakWindow1Amplitude,
                        enabled = prefs.manualPeakProfileEnabled,
                        onValueChange = { viewModel.updatePreferences { p -> p.copy(manualPeakWindow1Amplitude = it) } }
                    )
                    SettingsFloatSliderRow(
                        label = "Window 2 Amplitude",
                        value = prefs.manualPeakWindow2Amplitude,
                        enabled = prefs.manualPeakProfileEnabled,
                        onValueChange = { viewModel.updatePreferences { p -> p.copy(manualPeakWindow2Amplitude = it) } }
                    )
                    SettingsFloatSliderRow(
                        label = "Window 3 Amplitude",
                        value = prefs.manualPeakWindow3Amplitude,
                        enabled = prefs.manualPeakProfileEnabled,
                        onValueChange = { viewModel.updatePreferences { p -> p.copy(manualPeakWindow3Amplitude = it) } }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Profile shape preview", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ProfileWindowPreviewRow(
                        label = "W1",
                        startOffsetMinutes = prefs.manualPeakWindow1StartOffsetMinutes,
                        durationMinutes = prefs.manualPeakWindow1DurationMinutes,
                        amplitude = prefs.manualPeakWindow1Amplitude
                    )
                    ProfileWindowPreviewRow(
                        label = "W2",
                        startOffsetMinutes = prefs.manualPeakWindow2StartOffsetMinutes,
                        durationMinutes = prefs.manualPeakWindow2DurationMinutes,
                        amplitude = prefs.manualPeakWindow2Amplitude
                    )
                    ProfileWindowPreviewRow(
                        label = "W3",
                        startOffsetMinutes = prefs.manualPeakWindow3StartOffsetMinutes,
                        durationMinutes = prefs.manualPeakWindow3DurationMinutes,
                        amplitude = prefs.manualPeakWindow3Amplitude
                    )
                    OutlinedButton(
                        enabled = prefs.manualPeakProfileEnabled,
                        onClick = {
                            val preset = manualProfilePresetForChronotype(
                                chronotype = selectedChronotype,
                                wakeUpHour = prefs.wakeUpHour
                            )
                            viewModel.updatePreferences { p ->
                                p.copy(
                                    manualPeakProfileType = "AUTO",
                                    manualPeakAnchorMinuteOfDay = preset.anchorMinuteOfDay,
                                    manualPeakWindow1StartOffsetMinutes = preset.window1StartOffsetMinutes,
                                    manualPeakWindow2StartOffsetMinutes = preset.window2StartOffsetMinutes,
                                    manualPeakWindow3StartOffsetMinutes = preset.window3StartOffsetMinutes,
                                    manualPeakWindow1DurationMinutes = preset.window1DurationMinutes,
                                    manualPeakWindow2DurationMinutes = preset.window2DurationMinutes,
                                    manualPeakWindow3DurationMinutes = preset.window3DurationMinutes,
                                    manualPeakWindow1Amplitude = preset.window1Amplitude,
                                    manualPeakWindow2Amplitude = preset.window2Amplitude,
                                    manualPeakWindow3Amplitude = preset.window3Amplitude
                                )
                            }
                        }
                    ) {
                        Text("Reset to chronotype defaults")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.updatePreferences { p ->
                                p.copy(
                                    morningTuneSleepWeight = 0.30f,
                                    morningTuneWakeWeight = 0.25f,
                                    morningTuneBehaviorWeight = 0.25f,
                                    morningTuneBaseWeight = 0.20f,
                                    morningTuneUpdatedAtMillis = 0L
                                )
                            }
                        }
                    ) {
                        Text("Reset auto-tune weights")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingsCompactBox(title = "Moment Sensitivity", collapsedByDefault = true) {
                    Text(
                        "Tune how strongly live context signals affect moment energy adjustments.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingsFloatSliderRow(
                        label = "Interruption sensitivity",
                        value = prefs.momentInterruptionSensitivity,
                        minValue = 0.5f,
                        maxValue = 2.0f,
                        onValueChange = {
                            viewModel.updatePreferences { p -> p.copy(momentInterruptionSensitivity = it) }
                        }
                    )
                    SettingsFloatSliderRow(
                        label = "Notification sensitivity",
                        value = prefs.momentNotificationSensitivity,
                        minValue = 0.5f,
                        maxValue = 2.0f,
                        onValueChange = {
                            viewModel.updatePreferences { p -> p.copy(momentNotificationSensitivity = it) }
                        }
                    )
                    SettingsFloatSliderRow(
                        label = "Task pressure sensitivity",
                        value = prefs.momentTaskPressureSensitivity,
                        minValue = 0.5f,
                        maxValue = 2.0f,
                        onValueChange = {
                            viewModel.updatePreferences { p -> p.copy(momentTaskPressureSensitivity = it) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingsCompactBox(title = "Telemetry & Privacy", collapsedByDefault = true) {
                    SettingsToggleRow(
                        label = "Energy telemetry (local only)",
                        description = "Store prediction snapshots on-device for calibration and diagnostics",
                        checked = prefs.energyTelemetryEnabled,
                        onCheckedChange = {
                            viewModel.updatePreferences { p -> p.copy(energyTelemetryEnabled = it) }
                        }
                    )
                    if (prefs.energyTelemetryEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Retention (days)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(7, 14, 30, 60, 90).forEach { days ->
                                FilterChip(
                                    selected = prefs.energyTelemetryRetentionDays == days,
                                    onClick = {
                                        viewModel.updatePreferences { p -> p.copy(energyTelemetryRetentionDays = days) }
                                    },
                                    label = { Text("$days") }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Telemetry remains on this device. It is never required for live scoring.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showClearTelemetryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeuroFlowColors.TrackingRed)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, "Clear telemetry")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Energy Telemetry")
                    }
                }
            }

            // User Guide
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = onNavigateToAppGuide
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text("neuroFlow V2 Operating Manual", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Circadian biophysics, auto-scheduling & executive protocols",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Filled.ChevronRight, "Navigate")
                }
            }

            // Privacy & Permissions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = onNavigateToPrivacyPermissions
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text("Privacy & Permissions", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Why the app asks for powerful Android permissions",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Filled.ChevronRight, "Navigate")
                }
            }

            // Data
            SettingsSection("Data") {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeuroFlowColors.TrackingRed)
                ) {
                    Icon(Icons.Filled.DeleteForever, "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Data")
                }
            }

            // About
            SettingsSection("About") {
                Text("neuroFlow v${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDistractingAppsDialog) {
        DistractingAppsDialog(
            initialSelected = prefs.distractingPackages,
            viewModel = viewModel,
            onDismiss = { showDistractingAppsDialog = false },
            onSave = { packages ->
                viewModel.updatePreferences { p -> p.copy(distractingPackages = packages) }
                showDistractingAppsDialog = false
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?") },
            text = {
                Text(
                    "This will permanently delete all app data on this device and reset backup state so old data is not restored after reinstall. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                }) { Text("Delete", color = NeuroFlowColors.TrackingRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearTelemetryDialog) {
        AlertDialog(
            onDismissRequest = { showClearTelemetryDialog = false },
            title = { Text("Clear Energy Telemetry?") },
            text = {
                Text("This removes saved energy predictions used for diagnostics and calibration. Live scoring will continue.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearEnergyTelemetry()
                    showClearTelemetryDialog = false
                }) { Text("Clear", color = NeuroFlowColors.TrackingRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearTelemetryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDeepWorkTargetRow(minutes: Int, onChange: (Int) -> Unit) {
    val presets = listOf(
        60 to "1 h",
        90 to "1.5 h",
        120 to "2 h",
        180 to "3 h",
        240 to "4 h",
        300 to "5 h",
        360 to "6 h"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Daily deep-work target", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(
            "The Today card measures your logged focus blocks against this and says whether another one still fits.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            presets.forEach { (value, label) ->
                FilterChip(
                    selected = minutes == value,
                    onClick = { onChange(value) },
                    label = { Text(label, fontSize = 12.sp) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun DistractionPermissionRow() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(BehavioralSupportService.hasUsagePermission(context)) }

    // The grant happens in the system settings page, which never calls back into this
    // composition — poll until it flips, the same way the Hyper Focus permission screen does.
    LaunchedEffect(granted) {
        while (!granted) {
            delay(1000)
            granted = BehavioralSupportService.hasUsagePermission(context)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Usage access", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                if (granted) "Granted — today's screen time is readable"
                else "Not granted — distraction readings stay empty",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (granted) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Usage access granted",
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Button(
                onClick = { BehavioralSupportService.openUsagePermissionSettings(context) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DistractingAppsRow(count: Int, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Distracting apps", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("$count flagged", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = "Edit distracting apps")
    }
}

@Composable
private fun DistractingAppsDialog(
    initialSelected: Set<String>,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val apps by viewModel.launchableApps.collectAsStateWithLifecycle()
    val appsLoading by viewModel.launchableAppsLoading.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(initialSelected) }
    var query by remember { mutableStateOf("") }
    var flaggedOnly by remember { mutableStateOf(false) }

    val visible = apps.filter { app ->
        val matchesQuery = query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
        val matchesFlagFilter = !flaggedOnly || app.packageName in selected
        matchesQuery && matchesFlagFilter
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                    Text(
                        "Distracting apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${selected.size}")
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onSave(selected) }) { Text("Save") }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    singleLine = true,
                    placeholder = { Text("Search installed apps") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = flaggedOnly,
                        onClick = { flaggedOnly = !flaggedOnly },
                        label = { Text("Flagged only", fontSize = 12.sp) }
                    )
                    Text(
                        "Apps you don't see here can still stay in the list.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (apps.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (appsLoading) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Reading installed apps…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Couldn't read the installed apps.", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.loadLaunchableApps() }) {
                                Text("Try again")
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(visible, key = { it.packageName }) { app ->
                            AppPickRow(
                                packageName = app.packageName,
                                label = app.label,
                                checked = app.packageName in selected,
                                onToggle = {
                                    selected = if (app.packageName in selected) selected - app.packageName
                                    else selected + app.packageName
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickRow(
    packageName: String,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(72, 72).asImageBitmap()
        }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                packageName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

private data class ManualProfilePreset(
    val label: String,
    val anchorMinuteOfDay: Int,
    val window1StartOffsetMinutes: Int,
    val window2StartOffsetMinutes: Int,
    val window3StartOffsetMinutes: Int,
    val window1DurationMinutes: Int,
    val window2DurationMinutes: Int,
    val window3DurationMinutes: Int,
    val window1Amplitude: Float,
    val window2Amplitude: Float,
    val window3Amplitude: Float
)

private fun manualProfilePresetForChronotype(
    chronotype: String?,
    wakeUpHour: Int
): ManualProfilePreset {
    val wakeMinute = wakeUpHour.coerceIn(0, 23) * 60
    fun anchored(offsetMinutes: Int): Int = (wakeMinute + offsetMinutes).mod(24 * 60)

    return when (chronotype) {
        "INTERMEDIATE" -> ManualProfilePreset(
            label = "Intermediate",
            anchorMinuteOfDay = anchored(150), // +2.5h
            window1StartOffsetMinutes = 0,
            window2StartOffsetMinutes = 480,
            window3StartOffsetMinutes = 720,
            window1DurationMinutes = 210,      // 3.5h
            window2DurationMinutes = 150,      // 2.5h
            window3DurationMinutes = 60,       // 1h
            window1Amplitude = 1.0f,
            window2Amplitude = 0.78f,
            window3Amplitude = 0.58f
        )

        "MODERATE_EVENING", "DEFINITE_EVENING" -> ManualProfilePreset(
            label = "Night Owl",
            anchorMinuteOfDay = anchored(330), // +5.5h
            window1StartOffsetMinutes = 0,
            window2StartOffsetMinutes = 330,
            window3StartOffsetMinutes = 600,
            window1DurationMinutes = 180,      // 3h
            window2DurationMinutes = 240,      // 4h
            window3DurationMinutes = 60,       // 1h
            window1Amplitude = 1.0f,
            window2Amplitude = 0.85f,
            window3Amplitude = 0.6f
        )

        else -> ManualProfilePreset(
            label = "Morning",
            anchorMinuteOfDay = anchored(0),
            window1StartOffsetMinutes = 0,
            window2StartOffsetMinutes = 570,
            window3StartOffsetMinutes = 810,
            window1DurationMinutes = 210,
            window2DurationMinutes = 150,
            window3DurationMinutes = 60,
            window1Amplitude = 1.0f,
            window2Amplitude = 0.8f,
            window3Amplitude = 0.6f
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsCompactBox(
    title: String,
    collapsedByDefault: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title, collapsedByDefault) { mutableStateOf(!collapsedByDefault) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(2.dp))
                content()
            }
        }
    }
}

@Composable
private fun SettingsNumberRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    formatHour: Boolean = false,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit
) {
    val displayValue = if (formatHour) hourLabel(value) else "$value"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (enabled && value > min) onValueChange(value - 1) }, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.Filled.Remove, "Decrease", modifier = Modifier.size(18.dp))
            }
            Text(displayValue, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 13.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = { if (enabled && value < max) onValueChange(value + 1) }, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(Icons.Filled.Add, "Increase", modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun hourLabel(hour: Int) = when {
    hour == 0  -> "12 am"
    hour < 12  -> "$hour am"
    hour == 12 -> "12 pm"
    else       -> "${hour - 12} pm"
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsFloatSliderRow(
    label: String,
    value: Float,
    enabled: Boolean = true,
    minValue: Float = 0.2f,
    maxValue: Float = 1f,
    onValueChange: (Float) -> Unit
) {
    val safe = value.coerceIn(minValue, maxValue)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format("%.2f", safe), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = safe,
            enabled = enabled,
            onValueChange = { onValueChange(it.coerceIn(minValue, maxValue)) },
            valueRange = minValue..maxValue
        )
    }
}

@Composable
private fun ProfileWindowPreviewRow(
    label: String,
    startOffsetMinutes: Int,
    durationMinutes: Int,
    amplitude: Float
) {
    val widthWeight = durationMinutes.coerceIn(30, 360).toFloat() / 360f
    val alpha = amplitude.coerceIn(0.2f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label +${startOffsetMinutes}m", fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(widthWeight)
                    .padding(end = 4.dp)
                    .height(8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    shape = RoundedCornerShape(4.dp)
                ) {}
            }
        }
        Text(String.format("%.2f", alpha), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
