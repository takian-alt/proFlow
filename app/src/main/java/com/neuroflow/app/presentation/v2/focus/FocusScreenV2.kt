package com.neuroflow.app.presentation.v2.focus

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.presentation.v2.focus.components.AffordanceRatingSheet
import com.neuroflow.app.presentation.v2.focus.components.KeepScreenOnEffect
import com.neuroflow.app.presentation.v2.focus.components.WoopPromptSheet
import com.neuroflow.app.presentation.v2.focus.audio.SoundscapeType
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreenV2(
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
    var showWoopPromptSheet by remember { mutableStateOf(false) }

    KeepScreenOnEffect(enabled = state.isSessionActive && !state.isPaused)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenExit()
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
            if (!state.isRunning && !state.isCompleted) {
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
                                selectedLabelColor = Color.White,
                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                labelColor = NeuroFlowThemeV2Colors.TextSecondary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            // Bioluminescent Radial Timer Ring
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
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Complete Task Button (touch target >= 48dp)
                Button(
                    onClick = { viewModel.completeTask() },
                    modifier = Modifier.heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.SageGreenDark)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Complete", fontWeight = FontWeight.Bold, color = Color.White)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "🎧 Neural Soundscapes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                            if (state.selectedSoundscape != SoundscapeType.NONE) {
                                Text(
                                    state.selectedSoundscape.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

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
                                    selectedLabelColor = Color.White,
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
                        Text("🪜 Micro-Steps Checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (totalSteps > 0) {
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
        PhysiologicalSighGuideSheet(onDismiss = { showSighSheet = false })
    }

    if (state.showAffordanceRating && !state.affordanceRatingSubmitted) {
        AffordanceRatingSheet(
            onSubmit = { rating -> viewModel.submitAffordanceRating(rating) },
            onSkip = { viewModel.dismissAffordanceRating() }
        )
    }

    if (showWoopPromptSheet) {
        WoopPromptSheet(
            onSubmit = { wish, outcome, obstacle, plan ->
                viewModel.saveWoop(wish, outcome, obstacle, plan)
                showWoopPromptSheet = false
            },
            onSkip = { showWoopPromptSheet = false }
        )
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
    val progress = (currentSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val minutes = currentSeconds / 60
    val seconds = currentSeconds % 60
    val timeDisplay = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .size(230.dp)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        val trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
        val ringColor = NeuroFlowThemeV2Colors.ElectricIndigo
        val ringGlow = NeuroFlowThemeV2Colors.ElectricIndigoGlow

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated progress ring
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        ringColor,
                        ringGlow,
                        ringColor
                    )
                ),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeDisplay,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = NeuroFlowThemeV2Colors.TextPrimary,
                letterSpacing = 1.sp
            )
            val statusLabel = when {
                !isRunning -> "READY"
                isPaused -> "PAUSED"
                else -> "FLOW IN PROGRESS"
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    !isRunning -> NeuroFlowThemeV2Colors.TextSecondary
                    isPaused -> NeuroFlowThemeV2Colors.WarmAmber
                    else -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
                }
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
