package com.neuroflow.app.presentation.v2.focus

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.neuroflow.app.presentation.v2.focus.audio.SoundscapeType
import com.neuroflow.app.presentation.common.FrogConfettiOverlay
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
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
    var showFrogConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(state.task, state.preferences, initialOpenWoop) {
        val task = state.task
        val prefs = state.preferences
        if (task != null) {
            if (initialOpenWoop || (prefs.woopEnabled && !task.woopPromptShown && state.woopData == null)) {
                showWoopPromptSheet = true
                viewModel.markWoopPromptShown()
            }
        }
    }

    KeepScreenOnEffect(enabled = state.isSessionActive && !state.isPaused)

    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            if (activity?.isChangingConfigurations != true) {
                viewModel.onScreenExit()
            }
        }
    }

    Scaffold(
        containerColor = NeuroFlowThemeV2Colors.Obsidian,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.task?.title ?: "Focus Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary,
                            maxLines = 1
                        )
                        if (state.task?.isFrog == true) {
                            Text("🐸 Daily Frog (High Biological Leverage)", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.SageGreen)
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
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            contentColor = NeuroFlowThemeV2Colors.SageGreen
                        ),
                        modifier = Modifier.padding(end = 8.dp).heightIn(min = 40.dp)
                    ) {
                        Text("🫁 60s Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeuroFlowThemeV2Colors.Obsidian)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector Chips
            if (!state.isRunning && !state.isPaused && !state.isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusTimerModeV2.entries.forEach { mode ->
                        FilterChip(
                            selected = state.timerMode == mode,
                            onClick = { viewModel.selectTimerMode(mode) },
                            label = { Text(mode.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                selectedLabelColor = NeuroFlowThemeV2Colors.TextPrimary,
                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                labelColor = NeuroFlowThemeV2Colors.TextSecondary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            // Hero Task Card: Full prominence for Task Title, Frog status, and Next Micro-Step
            HeroTaskCard(
                task = state.task,
                nextStep = if (state.isRunning && !state.isPaused) null else state.microSteps.firstOrNull { !it.completed }
            )

            // Bioluminescent Radial Timer Ring (focused strictly on digits & flow status)
            RadialTimerGauge(
                timerMode = state.timerMode,
                remainingSeconds = state.remainingSeconds,
                elapsedSeconds = state.elapsedSeconds,
                isRunning = state.isRunning,
                isPaused = state.isPaused
            )

            // Primary Control Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button (min 64dp)
                Button(
                    onClick = {
                        if (!state.isRunning) {
                            viewModel.startTimer()
                        } else if (state.isPaused) {
                            viewModel.resumeTimer()
                        } else {
                            viewModel.pauseTimer()
                        }
                    },
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
                ) {
                    Icon(
                        imageVector = if (state.isRunning && !state.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isRunning && !state.isPaused) "Pause" else "Start",
                        tint = NeuroFlowThemeV2Colors.TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Complete Task Button (touch target >= 48dp)
                Button(
                    onClick = {
                        if (state.task?.isFrog == true) showFrogConfetti = true
                        viewModel.completeTask()
                    },
                    modifier = Modifier.heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.SageGreenDark)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = NeuroFlowThemeV2Colors.TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Complete", fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.TextPrimary)
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Park Thought Button (touch target >= 48dp)
                OutlinedIconButton(
                    onClick = { showParkThoughtSheet = true },
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.WarmAmber)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Park Thought", tint = NeuroFlowThemeV2Colors.WarmAmber)
                }
            }

            // Bioluminescent Waveform Visualizer (Active during sprint with soundscape enabled)
            val isAudioActive = state.selectedSoundscape != SoundscapeType.NONE && state.isRunning && !state.isPaused
            if (isAudioActive) {
                BioluminescentSoundscapeWaveformCard(soundscape = state.selectedSoundscape)
            }

            // Pinned Active Doorway Step Spotlight Card
            val activeMicroSteps = state.microSteps
            val firstIncompleteIndex = activeMicroSteps.indices.firstOrNull { idx ->
                !activeMicroSteps[idx].completed && idx !in state.skippedMicroStepIndices
            } ?: activeMicroSteps.indexOfFirst { !it.completed }.takeIf { it != -1 } ?: 0
            val currentMicroStep = activeMicroSteps.getOrNull(firstIncompleteIndex)

            if (state.isRunning && !state.isPaused && currentMicroStep != null) {
                ZenMicroStepSpotlightCard(
                    currentStepIndex = firstIncompleteIndex,
                    totalSteps = activeMicroSteps.size,
                    currentStepTitle = currentMicroStep.text,
                    currentStepEstimatedMinutes = (state.task?.estimatedDurationMinutes ?: 25) / activeMicroSteps.size.coerceAtLeast(1),
                    onCompleteCurrentStep = { viewModel.toggleMicroStep(firstIncompleteIndex) },
                    onSkipStep = { viewModel.skipCurrentMicroStep(firstIncompleteIndex) }
                )
            }

            // Neural Soundscapes Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
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
                            Text(
                                "🎧 Neural Soundscapes",
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
                        Spacer(modifier = Modifier.width(8.dp))

                        val isAudioActive = state.selectedSoundscape != SoundscapeType.NONE && state.isRunning && !state.isPaused
                        ActiveAudioIndicator(isActive = isAudioActive)
                    }

                    // Soundscape Selector Chips (Off, 40Hz Gamma 🧠, Deep Brown 🌊, Pink Rain 🌧️)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SoundscapeType.entries.forEach { type ->
                            val isSelected = state.selectedSoundscape == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectSoundscape(type) },
                                label = {
                                    Text(
                                        type.shortChipLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                    selectedLabelColor = NeuroFlowThemeV2Colors.TextPrimary,
                                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    labelColor = NeuroFlowThemeV2Colors.TextSecondary
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    // Headphone hint for 40Hz Gamma waves
                    AnimatedVisibility(visible = state.selectedSoundscape == SoundscapeType.GAMMA_40HZ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Stereo headphones recommended for 40 Hz binaural entrainment",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
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
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val completedCount = state.microSteps.count { it.completed }
                        val totalSteps = state.microSteps.size

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🪜 Micro-Steps Checklist",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (totalSteps > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$completedCount/$totalSteps Done", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (totalSteps > 0) {
                            LinearProgressIndicator(
                                progress = { completedCount.toFloat() / totalSteps },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = NeuroFlowThemeV2Colors.SageGreen,
                                trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                            )
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.autoBreakDownTask() },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Break It Down (Auto-Scaffold 3 Steps) 🪄", fontSize = 12.sp)
                            }
                        }

                    state.microSteps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleMicroStep(index) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                textDecoration = if (step.completed) TextDecoration.LineThrough else null
                            )
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
                            shape = RoundedCornerShape(10.dp),
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

    // Executive Transition Buffer (Decompression Zone)
    LaunchedEffect(state.showDecompressionZone) {
        if (state.showDecompressionZone) {
            viewModel.startDecompressionTicker()
        }
    }

    if (state.showDecompressionZone) {
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
                    style = MaterialTheme.typography.displayMedium,
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(
                        if (state.decompressionRemainingSeconds > 0) "⚡ Ready Now (Skip Buffer)" else "Continue",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Completion Celebration Overlay
    if (state.isCompleted && !state.showDecompressionZone) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = {
                Text("🎉 Focus Session Completed!", fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.SageGreen)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Outstanding focus. You conquered '${state.task?.title}' and earned +${state.pointsEarned} dopamine points.", style = MaterialTheme.typography.bodyMedium)
                    if (!state.completionAffirmation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("✨ \"${state.completionAffirmation}\"", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                    }
                    if (state.nextTaskId != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Next in queue: ${state.nextTaskTitle}", fontWeight = FontWeight.SemiBold, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                    }
                }
            },
            confirmButton = {
                if (state.nextTaskId != null) {
                    Button(
                        onClick = { onNavigateToNextTask(state.nextTaskId!!) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
                    ) {
                        Text("Start Next Task")
                    }
                } else {
                    Button(onClick = onNavigateBack) {
                        Text("Return to Flow")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onNavigateBack) {
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
                Text("💡 Distraction Parking Lot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.WarmAmber)
                ) {
                    Text("Park Thought & Resume Flow", fontWeight = FontWeight.Bold, color = Color.Black)
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
                onKeepGoing = { viewModel.keepGoing(FocusTimerModeV2.POMODORO_25) },
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
    isPaused: Boolean
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

    val minutes = currentSeconds / 60
    val seconds = currentSeconds % 60
    val timeDisplay = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
    val ringColor = if (isPaused) NeuroFlowThemeV2Colors.WarmAmber else NeuroFlowThemeV2Colors.ElectricIndigo
    val ringGlow = if (isPaused) NeuroFlowThemeV2Colors.WarmAmber else NeuroFlowThemeV2Colors.ElectricIndigoGlow

    Box(
        modifier = Modifier
            .size(248.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val glowStrokeWidth = 24.dp.toPx()
            val diameter = size.minDimension - glowStrokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = diameter / 2f

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
                    fontSize = 46.sp,
                    letterSpacing = (-1).sp
                ),
                color = NeuroFlowThemeV2Colors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            val statusLabel = when {
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
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.5f)),
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
                Text(
                    text = "🎧 ${soundscape.displayName}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Soundscape Stream",
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
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
                        shape = RoundedCornerShape(6.dp),
                        color = NeuroFlowThemeV2Colors.SageGreenDark.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SageGreen)
                    ) {
                        Text(
                            "🐸 Daily Frog",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.SageGreen
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                ) {
                    Text(
                        "${task.estimatedDurationMinutes}m",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NeuroFlowThemeV2Colors.TextPrimary,
                maxLines = 3
            )

            if (nextStep != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎯", fontSize = 12.sp)
                        Text(
                            text = "Next: ${nextStep.text}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                            maxLines = 2
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
    val title = if (isMicroStart) "⚡ Dopamine Micro-Start Complete!" else "🎯 Focus Sprint Complete!"
    val subtitle = if (isMicroStart) {
        "You successfully broke initial task paralysis on \"$taskTitle\". Momentum is active!"
    } else {
        "Solid work on \"$taskTitle\"! Decide your next cognitive step below."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
            colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeuroFlowThemeV2Colors.TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Keep Going (+25m Pomodoro)",
                fontWeight = FontWeight.Bold,
                color = NeuroFlowThemeV2Colors.TextPrimary
            )
        }

        // Action 2: Decompress (Rest)
        Button(
            onClick = onDecompress,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.SageGreenDark)
        ) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = NeuroFlowThemeV2Colors.TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "3-Minute Rest (Decompression)",
                fontWeight = FontWeight.Bold,
                color = NeuroFlowThemeV2Colors.TextPrimary
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stay on Focus Screen", color = NeuroFlowThemeV2Colors.TextSecondary)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

