package com.neuroflow.app.presentation.v2.focus

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.presentation.v2.focus.components.KeepScreenOnEffect
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.presentation.v2.focus.audio.SoundscapeType
import com.neuroflow.app.presentation.common.FrogConfettiOverlay
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import androidx.compose.ui.platform.testTag
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreenV2(
    initialOpenWoop: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToNextTask: (String) -> Unit = {},
    viewModel: FocusViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showParkThoughtSheet by remember { mutableStateOf(false) }
    var showSighSheet by remember { mutableStateOf(false) }
    var newThoughtText by remember { mutableStateOf("") }
    var newStepText by remember { mutableStateOf("") }
    var showWoopDetails by remember { mutableStateOf(false) }
    var showWoopPromptSheet by remember { mutableStateOf(initialOpenWoop) }
    var initialWoopHandled by remember { mutableStateOf(false) }
    var showFrogConfetti by remember { mutableStateOf(false) }
    var dndNoticeDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.task, state.preferences, initialOpenWoop) {
        val task = state.task
        val prefs = state.preferences
        if (task != null) {
            if (!initialWoopHandled && initialOpenWoop) {
                initialWoopHandled = true
                showWoopPromptSheet = true
                viewModel.markWoopPromptShown()
            } else if (prefs.woopEnabled && !task.woopPromptShown && state.woopData == null &&
                !state.isCompleted && task.status != com.neuroflow.app.domain.model.TaskStatus.COMPLETED &&
                !state.isSessionActive) {
                showWoopPromptSheet = true
                viewModel.markWoopPromptShown()
            }
        }
    }

    KeepScreenOnEffect(enabled = state.isSessionActive && !state.isPaused)

    val context = LocalContext.current
    var hasDndPermission by remember {
        mutableStateOf(
            runCatching {
                val nm = context.getSystemService(NotificationManager::class.java)
                nm?.isNotificationPolicyAccessGranted == true
            }.getOrDefault(false)
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nm = runCatching { context.getSystemService(NotificationManager::class.java) }.getOrNull()
                hasDndPermission = nm?.isNotificationPolicyAccessGranted == true
                viewModel.onScreenResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = generateSequence(context) { if (it is android.content.ContextWrapper) it.baseContext else null }
                .filterIsInstance<Activity>()
                .firstOrNull()
            if (activity?.isChangingConfigurations != true) {
                viewModel.onScreenExit()
            }
        }
    }

    val focusAmbientColor by animateColorAsState(
        targetValue = when {
            state.isCompleted -> NeuroFlowThemeV2Colors.SageGreen
            state.isPaused -> NeuroFlowThemeV2Colors.WarmAmber
            state.isRunning -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
            else -> NeuroFlowThemeV2Colors.ElectricIndigo
        },
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "focus_ambient_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        focusAmbientColor.copy(alpha = if (state.isRunning && !state.isPaused) 0.16f else 0.09f),
                        Color.Transparent
                    ),
                    radius = 950f
                )
            )
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = state.task?.title ?: "Focus Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.task?.isFrog == true) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.SageGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    "Daily Frog · High Biological Leverage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.SageGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showSighSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.14f),
                            contentColor = NeuroFlowThemeV2Colors.SageGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("60s Reset", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector Chips
            if (!state.isRunning && !state.isPaused && !state.isCompleted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusTimerModeV2.entries.forEach { mode ->
                        val isSelected = state.timerMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectTimerMode(mode) },
                            label = {
                                Text(
                                    mode.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                labelColor = NeuroFlowThemeV2Colors.TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.65f),
                                selectedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            // Do Not Disturb Access Notice (One-time notice with action button if DND is active in prefs but permission is missing)
            val shouldShowDndNotice = (state.showDndNotice || (state.preferences.dndEnabled && !hasDndPermission)) &&
                !dndNoticeDismissed && !state.isCompleted
            if (shouldShowDndNotice) {
                DndPermissionNoticeCard(
                    onOpenSettings = {
                        runCatching {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    },
                    onDismiss = {
                        dndNoticeDismissed = true
                        viewModel.dismissDndNotice()
                    }
                )
            }

            // Hero Task Card: Full prominence for Task Title, Frog status, and Next Micro-Step (deduplicated when spotlight card is active)
            val activeMicroSteps = state.microSteps
            val firstIncompleteIndex = activeMicroSteps.indices.firstOrNull { idx ->
                !activeMicroSteps[idx].completed && idx !in state.skippedMicroStepIndices
            } ?: activeMicroSteps.indexOfFirst { !it.completed }.takeIf { it != -1 }
            val currentMicroStep = if (firstIncompleteIndex != null) activeMicroSteps.getOrNull(firstIncompleteIndex) else null

            val nextStepForHero = if (state.isCompleted || (state.isSessionActive && currentMicroStep != null)) {
                null
            } else {
                currentMicroStep
            }
            HeroTaskCard(
                task = state.task,
                nextStep = nextStepForHero
            )

            // ⚠️ 90m Ultradian Limit Warning Banner (Option 1A)
            if (state.isUltradianLimitExceeded && !state.isCompleted) {
                UltradianWarningBanner(
                    message = state.ultradianWarning ?: FocusUiStateV2.ULTRADIAN_WARNING_MESSAGE
                )
            }

            // Bioluminescent Radial Timer Ring (focused strictly on digits & flow status)
            RadialTimerGauge(
                timerMode = state.timerMode,
                remainingSeconds = state.remainingSeconds,
                elapsedSeconds = state.elapsedSeconds,
                isRunning = state.isRunning,
                isPaused = state.isPaused,
                isCompleted = state.isCompleted,
                bodyDoublingEnabled = state.bodyDoublingEnabled,
                bodyDoublingPromptActive = state.bodyDoublingPromptActive
            )

            // Non-blocking Body-Doubling Anchor Check-In Card or brief confirmation pill
            AnimatedVisibility(visible = state.bodyDoublingPromptActive && !state.isCompleted) {
                BodyDoublingCheckInCard(
                    pulseNumber = state.bodyDoublingPulseCount.coerceAtLeast(1),
                    currentStepTitle = currentMicroStep?.text,
                    onLockedIn = { viewModel.confirmBodyDoublingLockedIn() },
                    onRefocus = { viewModel.confirmBodyDoublingRefocus() }
                )
            }

            AnimatedVisibility(visible = !state.bodyDoublingPromptActive && state.bodyDoublingFeedbackMessage != null && !state.isCompleted) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("body_doubling_feedback_pill")
                ) {
                    Text(
                        text = state.bodyDoublingFeedbackMessage.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NeuroFlowPaletteV2.OptimalEmerald,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Primary Control Actions
            if (state.isCompleted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = NeuroFlowThemeV2Colors.SageGreenDark.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = NeuroFlowThemeV2Colors.SageGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Task Completed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.SageGreen)
                        }
                    }
                    if (state.nextTaskId != null) {
                        Button(
                            onClick = { onNavigateToNextTask(state.nextTaskId!!) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Next: ${state.nextTaskTitle ?: "Next Task"}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface.copy(alpha = 0.92f),
                    border = BorderStroke(
                        1.dp,
                        NeuroFlowPaletteV2.specularBorderBrush(
                            if (state.isRunning && !state.isPaused) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.SlateBorder,
                            0.9f
                        )
                    ),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Park Thought Button (touch target >= 48dp)
                            OutlinedButton(
                                onClick = { showParkThoughtSheet = true },
                                modifier = Modifier.heightIn(min = 50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.08f),
                                    contentColor = NeuroFlowThemeV2Colors.WarmAmber
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = "Park Thought",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Park", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }

                            // Play / Pause Core Button (72dp)
                            Button(
                                onClick = {
                                    if (state.isPaused) {
                                        viewModel.resumeTimer()
                                    } else if (!state.isRunning) {
                                        viewModel.startTimer()
                                    } else {
                                        viewModel.pauseTimer()
                                    }
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .shadow(if (state.isRunning && !state.isPaused) 10.dp else 4.dp, CircleShape),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isPaused) NeuroFlowThemeV2Colors.WarmAmber else NeuroFlowThemeV2Colors.ElectricIndigo,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (state.isRunning && !state.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isRunning && !state.isPaused) "Pause" else "Start",
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            // Complete Task Button (touch target >= 48dp)
                            Button(
                                onClick = {
                                    if (state.task?.isFrog == true) showFrogConfetti = true
                                    viewModel.completeTask()
                                },
                                modifier = Modifier.heightIn(min = 50.dp),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeuroFlowThemeV2Colors.SageGreenDark,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Complete", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(
                            color = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.28f),
                            thickness = 0.75.dp
                        )

                        FilterChip(
                            selected = state.bodyDoublingEnabled,
                            onClick = { viewModel.toggleBodyDoubling() },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (state.bodyDoublingEnabled) {
                                        "Body-Doubling Pulse · Every ${state.bodyDoublingIntervalMinutes}m"
                                    } else {
                                        "Body-Doubling Pulse"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.20f),
                                selectedLabelColor = NeuroFlowPaletteV2.ReboundCyan,
                                selectedLeadingIconColor = NeuroFlowPaletteV2.ReboundCyan,
                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                labelColor = NeuroFlowThemeV2Colors.TextSecondary,
                                iconColor = NeuroFlowThemeV2Colors.TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.bodyDoublingEnabled,
                                borderColor = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.5f),
                                selectedBorderColor = NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("body_doubling_toggle")
                        )

                        AnimatedVisibility(visible = state.bodyDoublingEnabled) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val nextMins = state.bodyDoublingNextPulseInSeconds / 60
                                val nextSecs = state.bodyDoublingNextPulseInSeconds % 60
                                val countdownText = String.format(Locale.getDefault(), "%02d:%02d", nextMins, nextSecs)
                                val statusLine = when {
                                    state.isRunning && !state.isPaused -> {
                                        val anchorSuffix = if (state.bodyDoublingPulseCount > 0) {
                                            " · ${state.bodyDoublingLockedCount}/${state.bodyDoublingPulseCount} anchored"
                                        } else {
                                            ""
                                        }
                                        "Companion active · Next pulse in $countdownText$anchorSuffix"
                                    }
                                    state.isPaused -> "Paused · Resumes check-in pulse every ${state.bodyDoublingIntervalMinutes}m"
                                    else -> {
                                        val sensoryLabel = if (state.bodyDoublingSoundEnabled) "haptic + soft chime" else "haptic-only"
                                        "Ready · Gentle $sensoryLabel anchor every ${state.bodyDoublingIntervalMinutes}m"
                                    }
                                }
                                Text(
                                    text = statusLine,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = NeuroFlowPaletteV2.ReboundCyan,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("body_doubling_status_text")
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(5, 10, 15).forEach { mins ->
                                        val isSelected = state.bodyDoublingIntervalMinutes == mins
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setBodyDoublingInterval(mins) },
                                            label = {
                                                Text(
                                                    text = "${mins}m",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.22f),
                                                selectedLabelColor = NeuroFlowPaletteV2.ReboundCyan,
                                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                                labelColor = NeuroFlowThemeV2Colors.TextSecondary
                                            ),
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("body_doubling_interval_${mins}m")
                                        )
                                    }

                                    FilterChip(
                                        selected = state.bodyDoublingSoundEnabled,
                                        onClick = { viewModel.toggleBodyDoublingSound() },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (state.bodyDoublingSoundEnabled) {
                                                    Icons.AutoMirrored.Filled.VolumeUp
                                                } else {
                                                    Icons.AutoMirrored.Filled.VolumeMute
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (state.bodyDoublingSoundEnabled) "Chime" else "Haptic",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.22f),
                                            selectedLabelColor = NeuroFlowPaletteV2.ReboundCyan,
                                            selectedLeadingIconColor = NeuroFlowPaletteV2.ReboundCyan,
                                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                            labelColor = NeuroFlowThemeV2Colors.TextSecondary,
                                            iconColor = NeuroFlowThemeV2Colors.TextSecondary
                                        ),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("body_doubling_sound_toggle")
                                    )

                                    AssistChip(
                                        onClick = { viewModel.triggerTestBodyDoublingPulse() },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = NeuroFlowPaletteV2.ReboundCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = "Test",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = NeuroFlowPaletteV2.ReboundCyan
                                            )
                                        },
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("body_doubling_test_pulse")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bioluminescent Waveform Visualizer (Active during sprint with soundscape enabled)
            val isAudioActive = state.selectedSoundscape != SoundscapeType.NONE && state.isRunning && !state.isPaused
            if (isAudioActive) {
                BioluminescentSoundscapeWaveformCard(soundscape = state.selectedSoundscape)
            }

            // Pinned Active Doorway Step Spotlight Card (shown across active & paused states)
            if (state.isSessionActive && !state.isCompleted && currentMicroStep != null) {
                val stepIdx = firstIncompleteIndex ?: 0
                ZenMicroStepSpotlightCard(
                    currentStepIndex = stepIdx,
                    totalSteps = activeMicroSteps.size,
                    currentStepTitle = currentMicroStep.text,
                    currentStepEstimatedMinutes = maxOf(1, (state.task?.let { TaskBlockPlan.plannedMinutes(it) } ?: 25) / activeMicroSteps.size.coerceAtLeast(1)),
                    onCompleteCurrentStep = { viewModel.toggleMicroStep(stepIdx) },
                    onSkipStep = { viewModel.skipCurrentMicroStep(stepIdx) }
                )
            }

            // Neural Soundscapes Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowThemeV2Colors.ElectricIndigoGlow, 0.75f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Active Animated Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Neural Soundscapes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (state.selectedSoundscape != SoundscapeType.NONE) {
                                    Text(
                                        state.selectedSoundscape.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        val isAudioActive = state.selectedSoundscape != SoundscapeType.NONE && state.isRunning && !state.isPaused
                        ActiveAudioIndicator(isActive = isAudioActive)
                    }

                    // Soundscape Selector Chips (Off, 40Hz Gamma, Deep Brown, Pink Rain)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundscapeType.entries.forEach { type ->
                            val isSelected = state.selectedSoundscape == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectSoundscape(type) },
                                label = {
                                    Text(
                                        type.shortChipLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                    selectedLabelColor = Color.White,
                                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    labelColor = NeuroFlowThemeV2Colors.TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.55f),
                                    selectedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    // Contextual Soundscape Usage Hint (When to use & neuro-profile)
                    AnimatedVisibility(visible = state.selectedSoundscape != SoundscapeType.NONE && state.selectedSoundscape.usageTag.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.selectedSoundscape == SoundscapeType.GAMMA_40HZ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    state.selectedSoundscape.emoji.ifBlank { "🎧" },
                                    fontSize = 14.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.selectedSoundscape.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Best for: ${state.selectedSoundscape.usageTag}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                )
                            }
                        }
                    }

                    // Tactile Volume Slider (0% to 100%) with 48dp touch target
                    AnimatedVisibility(visible = state.selectedSoundscape != SoundscapeType.NONE) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Acoustic Volume",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary
                                )
                                Text(
                                    "${(state.soundscapeVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.soundscapeVolume <= 0.01f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeDown,
                                    contentDescription = "Volume Down",
                                    tint = NeuroFlowThemeV2Colors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Slider(
                                    value = state.soundscapeVolume,
                                    onValueChange = { viewModel.setSoundscapeVolume(it) },
                                    valueRange = 0f..1f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                        activeTrackColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                        inactiveTrackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                                    )
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Volume Up",
                                    tint = NeuroFlowThemeV2Colors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // WOOP Intention Card
            val woop = state.woopData
            if (woop != null && (woop.wish.isNotBlank() || woop.obstacle.isNotBlank() || woop.plan.isNotBlank())) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWoopDetails = !showWoopDetails },
                    shape = RoundedCornerShape(14.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow, modifier = Modifier.size(18.dp))
                                Text("WOOP Mental Anchor", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                            }
                            Text(if (showWoopDetails) "Hide" else "Show", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextSecondary)
                        }

                        if (showWoopDetails) {
                            if (woop.wish.isNotBlank()) Text("Wish: ${woop.wish}", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextPrimary)
                            if (woop.obstacle.isNotBlank()) Text("Obstacle: ${woop.obstacle}", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.RoseCoral)
                            if (woop.plan.isNotBlank()) Text("Plan: ${woop.plan}", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.SageGreen)
                        } else {
                            val summary = woop.plan.ifBlank { woop.wish }
                            Text(summary, style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextSecondary, maxLines = 1)
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWoopPromptSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "Set WOOP Anchor",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                                Text(
                                    "Pre-commit Wish, Obstacle, and If-Then execution plan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showWoopPromptSheet = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Set WOOP Anchor",
                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Micro-Steps Decomposition Checklist
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowThemeV2Colors.SageGreen, 0.65f))
            ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val completedCount = state.microSteps.count { it.completed }
                        val totalSteps = state.microSteps.size

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = null,
                                        tint = NeuroFlowThemeV2Colors.SageGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    "Micro-Steps Checklist",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (totalSteps > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        "$completedCount/$totalSteps Done",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = NeuroFlowThemeV2Colors.SageGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (totalSteps > 0) {
                            LinearProgressIndicator(
                                progress = { completedCount.toFloat() / totalSteps },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = NeuroFlowThemeV2Colors.SageGreen,
                                trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                            )
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.autoBreakDownTask() },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.45f))
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Break It Down (Auto-Scaffold 3 Steps)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                    state.microSteps.forEachIndexed { index, step ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (step.completed) NeuroFlowThemeV2Colors.SlateSurfaceAlt.copy(alpha = 0.55f) else NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(
                                0.75.dp,
                                if (step.completed) NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.3f) else NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { viewModel.toggleMicroStep(index) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = step.completed,
                                    onCheckedChange = { viewModel.toggleMicroStep(index) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = NeuroFlowThemeV2Colors.SageGreen,
                                        uncheckedColor = NeuroFlowThemeV2Colors.TextSecondary
                                    )
                                )
                                Text(
                                    text = step.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (step.completed) NeuroFlowThemeV2Colors.TextTertiary else NeuroFlowThemeV2Colors.TextPrimary,
                                    textDecoration = if (step.completed) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Add Step Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newStepText,
                            onValueChange = { newStepText = it },
                            placeholder = { Text("Add next small action...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                viewModel.addMicroStep(newStepText)
                                newStepText = ""
                            },
                            enabled = newStepText.isNotBlank(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add step", tint = NeuroFlowThemeV2Colors.ElectricIndigo)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
        }
    }
    }

    // Executive Transition Buffer (Decompression Zone)
    LaunchedEffect(state.showDecompressionZone, state.showAffordanceRating, state.affordanceRatingSubmitted) {
        if (state.showDecompressionZone && (!state.showAffordanceRating || state.affordanceRatingSubmitted)) {
            viewModel.startDecompressionTicker()
        }
    }

    if (state.showDecompressionZone && (!state.showAffordanceRating || state.affordanceRatingSubmitted)) {
        val mins = state.decompressionRemainingSeconds / 60
        val secs = state.decompressionRemainingSeconds % 60
        val timeDisplay = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissDecompressionZone() },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🧠 Executive Decompression Zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.SageGreen
                )

                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = if (state.decompressionRemainingSeconds > 0) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.SageGreen
                )

                Text(
                    text = if (state.decompressionRemainingSeconds > 0)
                        "Cortisol & sympathetic arousal are resetting. Take 3 minutes of passive rest before your next cognitive sprint to consolidate working memory."
                    else
                        "Rest restored. Your prefrontal cortex has recharged for your next flow state.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuroFlowThemeV2Colors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (state.isDecompressionSighActive) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "🫁 Physiological Sigh Active: ${state.decompressionSighRemainingSeconds}s",
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.SageGreen
                            )
                            Text(
                                "Inhale deeply through nose... top-off inhale... slow long exhale through mouth.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (state.decompressionRemainingSeconds > 0) {
                    FilledTonalButton(
                        onClick = { viewModel.triggerDecompressionSigh() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            contentColor = NeuroFlowThemeV2Colors.SageGreen
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text("🫁 Guided 60s Physiological Sigh", fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = { viewModel.readyNowBypassDecompression() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    if (state.decompressionRemainingSeconds > 0) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        if (state.decompressionRemainingSeconds > 0) "Ready Now (Skip Buffer)" else "Continue",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Completion Celebration Overlay
    if (state.showCompletionCelebration && !state.showDecompressionZone && (!state.showAffordanceRating || state.affordanceRatingSubmitted)) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCompletionCelebration() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeuroFlowThemeV2Colors.SageGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Focus Session Completed",
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.SageGreen
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Outstanding focus. You conquered '${state.task?.title}' and earned +${state.pointsEarned} dopamine points.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!state.completionAffirmation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "\"${state.completionAffirmation}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                    if (state.nextTaskId != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Next in queue: ${state.nextTaskTitle}",
                            fontWeight = FontWeight.SemiBold,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                }
            },
            confirmButton = {
                if (state.nextTaskId != null) {
                    Button(
                        onClick = {
                            viewModel.dismissCompletionCelebration()
                            onNavigateToNextTask(state.nextTaskId!!)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Start Next Task", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.dismissCompletionCelebration()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Return to Flow", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissCompletionCelebration() },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Thought Parking Lot Bottom Sheet
    if (showParkThoughtSheet) {
        ModalBottomSheet(
            onDismissRequest = { showParkThoughtSheet = false },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = NeuroFlowThemeV2Colors.WarmAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Distraction Parking Lot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Jot down your intrusive thought so your working memory stays clear. It will be saved as a low-energy task.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )

                OutlinedTextField(
                    value = newThoughtText,
                    onValueChange = { newThoughtText = it },
                    placeholder = { Text("e.g. Call dentist, check order status...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        viewModel.parkIntrusiveThought(newThoughtText)
                        newThoughtText = ""
                        showParkThoughtSheet = false
                    },
                    enabled = newThoughtText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.WarmAmber,
                        contentColor = Color(0xFF0A0B0E)
                    )
                ) {
                    Text("Park Thought & Resume Flow", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSighSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSighSheet = false },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            InteractivePhysiologicalSighSheet(
                totalCycles = 3,
                onCompleted = { showSighSheet = false }
            )
        }
    }

    if (state.showAffordanceRating && !state.affordanceRatingSubmitted) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissAffordanceRating() },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            UpgradedAffordanceRatingSheet(
                taskTitle = state.task?.title ?: "Focus Session",
                onRatingSelected = { rating -> viewModel.submitAffordanceRating(rating) },
                onSkip = { viewModel.dismissAffordanceRating() }
            )
        }
    }

    if (showWoopPromptSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWoopPromptSheet = false },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            UpgradedWoopPromptSheet(
                taskTitle = state.task?.title ?: "Focus Session",
                dreadedTaskInsight = null,
                onSaveWoopPlan = { wish, outcome, obstacle, ifThenPlan ->
                    viewModel.saveWoop(wish, outcome, obstacle, ifThenPlan)
                    showWoopPromptSheet = false
                },
                onDismiss = { showWoopPromptSheet = false }
            )
        }
    }

    if (state.showSprintCompletionSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSprintCompletionSheet() },
            containerColor = NeuroFlowThemeV2Colors.SlateSurface
        ) {
            SprintCompletionSheet(
                taskTitle = state.task?.title ?: "Deep Focus",
                timerMode = state.timerMode,
                onKeepGoing = {
                    val targetNextMode = if (state.timerMode == FocusTimerModeV2.JUST_5_MIN) FocusTimerModeV2.POMODORO_25 else state.timerMode
                    viewModel.keepGoing(targetNextMode)
                },
                onDecompress = { viewModel.takeDecompressionRest() },
                onCompleteTask = {
                    if (state.task?.isFrog == true) showFrogConfetti = true
                    viewModel.completeTask()
                },
                onDismiss = { viewModel.dismissSprintCompletionSheet() }
            )
        }
    }

    FrogConfettiOverlay(trigger = showFrogConfetti)
    LaunchedEffect(showFrogConfetti) {
        if (showFrogConfetti) {
            kotlinx.coroutines.delay(2500)
            showFrogConfetti = false
        }
    }
}

@Composable
fun RadialTimerGauge(
    timerMode: FocusTimerModeV2,
    remainingSeconds: Long,
    elapsedSeconds: Long,
    isRunning: Boolean,
    isPaused: Boolean,
    isCompleted: Boolean = false,
    bodyDoublingEnabled: Boolean = false,
    bodyDoublingPromptActive: Boolean = false
) {
    val totalSeconds = if (timerMode == FocusTimerModeV2.FLOW_STOPWATCH) 3600L else timerMode.durationSeconds
    val currentSeconds = if (timerMode == FocusTimerModeV2.FLOW_STOPWATCH) elapsedSeconds else remainingSeconds
    val rawProgress = (currentSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "timer_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = if (isRunning && !isPaused) 0.26f else 0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val bodyDoublingBreathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "body_doubling_breathe"
    )
    val bodyDoublingSonar by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "body_doubling_sonar"
    )

    val minutes = currentSeconds / 60
    val seconds = currentSeconds % 60
    val timeDisplay = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
    val ringColor = when {
        isCompleted -> NeuroFlowThemeV2Colors.SageGreen
        isPaused -> NeuroFlowThemeV2Colors.WarmAmber
        else -> NeuroFlowThemeV2Colors.ElectricIndigo
    }
    val ringGlow = when {
        isCompleted -> NeuroFlowThemeV2Colors.SageGreen
        isPaused -> NeuroFlowThemeV2Colors.WarmAmber
        else -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
    }

    val haloColor = NeuroFlowPaletteV2.ReboundCyan
    val tickBaseColor = NeuroFlowThemeV2Colors.SlateBorder
    Box(
        modifier = Modifier
            .size(284.dp)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val glowStrokeWidth = 44.dp.toPx()
            val diameter = size.minDimension - glowStrokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = diameter / 2f

            // Clearly visible bioluminescent Body-Doubling presence halo + expanding sonar wave
            if (bodyDoublingEnabled && !isCompleted) {
                val isActivelyPulsing = (isRunning && !isPaused) || bodyDoublingPromptActive
                val auraAlpha = when {
                    bodyDoublingPromptActive -> 0.42f + 0.22f * bodyDoublingBreathe
                    isRunning && !isPaused -> 0.22f + 0.16f * bodyDoublingBreathe
                    else -> 0.18f + 0.10f * bodyDoublingBreathe
                }
                val outerHaloRadius = radius * (1.07f + 0.05f * bodyDoublingBreathe)

                // Soft cyan outer presence glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            haloColor.copy(alpha = auraAlpha * 0.55f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.18f
                    ),
                    radius = radius * 1.18f,
                    center = center
                )

                // Crisp breathing presence ring outside the main timer ring (never hidden or clipped)
                drawCircle(
                    color = haloColor.copy(alpha = auraAlpha),
                    radius = outerHaloRadius,
                    center = center,
                    style = Stroke(width = if (bodyDoublingPromptActive) 3.5.dp.toPx() else 2.25.dp.toPx())
                )

                // Expanding sonar wave ring when actively running or pulsing
                if (isActivelyPulsing) {
                    val sonarRadius = radius * (1.03f + 0.16f * bodyDoublingSonar)
                    val sonarAlpha = ((1f - bodyDoublingSonar) * if (bodyDoublingPromptActive) 0.65f else 0.42f).coerceIn(0f, 1f)
                    drawCircle(
                        color = haloColor.copy(alpha = sonarAlpha),
                        radius = sonarRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // 1. Breathing radial core aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ringGlow.copy(alpha = pulseAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.05f
                ),
                radius = radius * 1.05f,
                center = center
            )

            // Precision 60-tick chronometer perimeter
            val innerTickR = radius - strokeWidth * 0.85f
            for (tick in 0 until 60) {
                val isMajor = tick % 5 == 0
                val angle = Math.toRadians((-90.0 + tick * 6.0))
                val cosA = kotlin.math.cos(angle).toFloat()
                val sinA = kotlin.math.sin(angle).toFloat()
                val len = if (isMajor) 5.dp.toPx() else 2.5.dp.toPx()
                val startPt = Offset(center.x + innerTickR * cosA, center.y + innerTickR * sinA)
                val endPt = Offset(center.x + (innerTickR - len) * cosA, center.y + (innerTickR - len) * sinA)
                drawLine(
                    color = tickBaseColor.copy(alpha = if (isMajor) 0.45f else 0.22f),
                    start = startPt,
                    end = endPt,
                    strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 2. Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 3. Soft outer bloom arc + crisp core ring + leading tip orb
            val sweep = animatedProgress * 360f
            if (sweep > 0.5f) {
                drawArc(
                    color = ringGlow.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = glowStrokeWidth, cap = StrokeCap.Round)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(ringColor, ringGlow, ringColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                val angleRad = Math.toRadians((-90.0 + sweep.toDouble()))
                val tipX = center.x + radius * kotlin.math.cos(angleRad).toFloat()
                val tipY = center.y + radius * kotlin.math.sin(angleRad).toFloat()
                drawCircle(
                    color = ringGlow.copy(alpha = 0.45f),
                    radius = 11.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeDisplay,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                    letterSpacing = (-1.2).sp,
                    fontFeatureSettings = "tnum"
                ),
                color = NeuroFlowThemeV2Colors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            val statusLabel = when {
                isCompleted -> "TASK COMPLETED"
                !isRunning && !isPaused -> "READY FOR DEEP WORK"
                isPaused -> "SESSION PAUSED"
                else -> "FLOW STATE ACTIVE"
            }
            Surface(
                color = ringGlow.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, ringGlow.copy(alpha = 0.35f))
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp
                    ),
                    color = ringGlow,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            val elapsedMin = elapsedSeconds / 60
            val subCaption = when {
                timerMode == FocusTimerModeV2.FLOW_STOPWATCH -> "Flow Stopwatch"
                elapsedMin > 0 -> "Target ${timerMode.durationSeconds / 60}m · ${elapsedMin}m logged"
                else -> "${timerMode.durationSeconds / 60}m Target Sprint"
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = subCaption,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = NeuroFlowThemeV2Colors.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ActiveAudioIndicator(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioBars")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 16f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(22.dp)
    ) {
        val barColor = if (isActive) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.SlateBorder
        val barHeights = if (isActive) listOf(h1, h2, h3, h4) else listOf(4f, 4f, 4f, 4f)
        barHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(barColor, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

@Composable
fun BioluminescentSoundscapeWaveformCard(
    soundscape: SoundscapeType,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Surface(
        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.75.dp, NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowThemeV2Colors.ElectricIndigo, 0.85f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = soundscape.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "· Soundscape",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowThemeV2Colors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Bioluminescent Waveform Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(24.dp)
            ) {
                val barCount = 14
                for (i in 0 until barCount) {
                    val normalizedI = i.toFloat() / barCount
                    val wave = kotlin.math.sin((normalizedI * 2 * Math.PI + phase * 2 * Math.PI).toFloat())
                    val barHeight = (6f + 14f * (0.5f + 0.5f * wave)).coerceIn(4f, 20f)
                    val barAlpha = 0.45f + 0.55f * (0.5f + 0.5f * wave)
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight.dp)
                            .background(
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = barAlpha),
                                shape = RoundedCornerShape(1.5.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun HeroTaskCard(
    task: TaskEntity?,
    nextStep: StepByStepPlanStep?,
    modifier: Modifier = Modifier
) {
    if (task == null) return
    val accentColor = if (task.isFrog) NeuroFlowThemeV2Colors.SageGreen else NeuroFlowThemeV2Colors.ElectricIndigoGlow
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(0.75.dp, NeuroFlowPaletteV2.specularBorderBrush(accentColor, 0.75f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.isFrog) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeuroFlowThemeV2Colors.SageGreenDark.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = NeuroFlowThemeV2Colors.SageGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                "Daily Frog",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.SageGreen
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(0.5.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f))
                ) {
                    Text(
                        "${TaskBlockPlan.plannedMinutes(task)}m",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NeuroFlowThemeV2Colors.TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (nextStep != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                        )
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Next: ${nextStep.text}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SprintCompletionSheet(
    taskTitle: String,
    timerMode: FocusTimerModeV2,
    onKeepGoing: () -> Unit,
    onDecompress: () -> Unit,
    onCompleteTask: () -> Unit,
    onDismiss: () -> Unit
) {
    val isMicroStart = timerMode == FocusTimerModeV2.JUST_5_MIN
    val title = if (isMicroStart) "Dopamine Micro-Start Complete" else "Focus Sprint Complete"
    val subtitle = if (isMicroStart) {
        "You broke initial task paralysis on \"$taskTitle\". Momentum is active!"
    } else {
        "Solid work on \"$taskTitle\". Choose your next cognitive step below."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = if (isMicroStart) Icons.Default.Bolt else Icons.Default.Flag,
                contentDescription = null,
                tint = NeuroFlowThemeV2Colors.WarmAmber,
                modifier = Modifier.padding(10.dp).size(24.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = NeuroFlowThemeV2Colors.WarmAmber,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = NeuroFlowThemeV2Colors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Action 1: Keep Going (Momentum)
        Button(
            onClick = onKeepGoing,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            val keepGoingText = if (isMicroStart) {
                "Keep Going (+25m Pomodoro)"
            } else {
                "Keep Going (+${timerMode.durationSeconds / 60}m ${timerMode.title})"
            }
            Text(
                keepGoingText,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Action 2: Decompress (Rest)
        Button(
            onClick = onDecompress,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeuroFlowThemeV2Colors.SageGreenDark,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "3-Minute Rest (Decompression)",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Action 3: Complete Entire Task
        OutlinedButton(
            onClick = onCompleteTask,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.6f))
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = NeuroFlowThemeV2Colors.SageGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "I'm Finished — Complete Task",
                fontWeight = FontWeight.SemiBold,
                color = NeuroFlowThemeV2Colors.SageGreen
            )
        }

        // Dismiss
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
        ) {
            Text("Stay on Focus Screen", color = NeuroFlowThemeV2Colors.TextSecondary)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DndPermissionNoticeCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = "Do Not Disturb Inactive",
                        tint = NeuroFlowThemeV2Colors.WarmAmber,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Do Not Disturb Inactive",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                        Text(
                            text = "DND is enabled for focus sessions, but Do Not Disturb access is required to automatically mute notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss notice",
                        tint = NeuroFlowThemeV2Colors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        "Dismiss",
                        fontSize = 12.sp,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.WarmAmber,
                        contentColor = Color(0xFF0A0B0E)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        "Open DND Settings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun UltradianWarningBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    val amber = NeuroFlowPaletteV2.PeakAmber
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ultradian_limit_warning_banner"),
        shape = RoundedCornerShape(14.dp),
        color = amber.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, amber.copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                ),
                color = NeuroFlowPaletteV2.TextPrimary
            )
        }
    }
}

@Composable
private fun BodyDoublingCheckInCard(
    pulseNumber: Int,
    currentStepTitle: String?,
    onLockedIn: () -> Unit,
    onRefocus: () -> Unit
) {
    val cyan = NeuroFlowPaletteV2.ReboundCyan
    val emerald = NeuroFlowPaletteV2.OptimalEmerald
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("body_doubling_checkin_card"),
        shape = RoundedCornerShape(16.dp),
        color = cyan.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, cyan.copy(alpha = 0.50f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = cyan,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anchor Pulse #$pulseNumber · Still locked in?",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = cyan
                    )
                    Text(
                        text = if (!currentStepTitle.isNullOrBlank()) {
                            "Stay with: $currentStepTitle (fades if you stay in flow)"
                        } else {
                            "Quick presence check — fades automatically if you stay in flow"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowPaletteV2.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRefocus,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("body_doubling_refocus_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Refocus",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                }

                Button(
                    onClick = onLockedIn,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = emerald,
                        contentColor = Color(0xFF042F2E)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("body_doubling_locked_in_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Locked In ✓",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}



