package com.neuroflow.app.presentation.v2.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreenV2(
    onNavigateBack: () -> Unit,
    onNavigateToNextTask: (String) -> Unit = {},
    viewModel: FocusViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showParkThoughtSheet by remember { mutableStateOf(false) }
    var newThoughtText by remember { mutableStateOf("") }
    var newStepText by remember { mutableStateOf("") }
    var showWoopDetails by remember { mutableStateOf(false) }

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

    // Completion Celebration Overlay
    if (state.isCompleted) {
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
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
                        NeuroFlowThemeV2Colors.ElectricIndigo,
                        NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        NeuroFlowThemeV2Colors.ElectricIndigo
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
