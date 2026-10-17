package com.neuroflow.app.presentation.v2.flow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.presentation.v2.composer.SmartTaskComposerSheetV2
import com.neuroflow.app.presentation.v2.notes.QuickNotesBottomSheet
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowScreenV2(
    onNavigateToSettings: () -> Unit,
    onStartFocus: (String) -> Unit,
    onNavigateToMEQ: () -> Unit = {},
    onNavigateToSleepLogs: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    initialOpenComposer: Boolean = false,
    onInitialComposerConsumed: () -> Unit = {},
    viewModel: FlowViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    var showComposer by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialOpenComposer) {
        if (initialOpenComposer) {
            showComposer = true
            onInitialComposerConsumed()
        }
    }

    // 4-Second Non-Destructive Undo Snackbar
    LaunchedEffect(state.lastActionMessage) {
        val msg = state.lastActionMessage
        if (!msg.isNullOrBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                if (msg.startsWith("Completed")) {
                    viewModel.undoCompleteTask()
                } else if (msg.startsWith("Deferred")) {
                    viewModel.undoDeferTask()
                }
            }
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        containerColor = NeuroFlowThemeV2Colors.Obsidian,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 72.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    contentColor = Color.White,
                    actionColor = NeuroFlowThemeV2Colors.WarmAmber,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showComposer = true },
                containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick Add Task", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "neuroFlow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Biological High Performance",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showNotesSheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Quick Scratchpad Notes",
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }
                }
            }

            // Hero Circadian Barometer (Borbély Two-Process Model)
            val energyScore = state.energyUi?.rawEnergy ?: 50f
            val battery = state.energyUi?.availableEnergy ?: 72
            val currentEnergyLevel = when {
                battery >= 65 -> EnergyLevel.HIGH
                battery >= 35 -> EnergyLevel.MEDIUM
                else -> EnergyLevel.LOW
            }

            CircadianHeroBarometer(
                rawEnergyScore = energyScore,
                batteryPercent = battery,
                energyZone = currentEnergyLevel,
                sleepPressurePoints = state.energyUi?.sleepPressurePoints ?: 450,
                fatigueZone = state.energyUi?.fatigueZone ?: com.neuroflow.app.domain.engine.SleepPressureDetector.FatigueZone.RESTED,
                circadianFactor = state.energyUi?.circadianFactor ?: 0.85f,
                recommendedTask = state.recommendedTask,
                onStartRecommendedTask = { taskId -> onStartFocus(taskId) },
                onNavigateToMEQ = onNavigateToMEQ,
                onNavigateToSleepLogs = onNavigateToSleepLogs
            )

            // Zero-Guilt Fresh Start Card (ADHD Safety Net)
            if (state.showZeroGuiltPrompt) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🌱", fontSize = 18.sp)
                            Text(
                                text = "Zero-Guilt Fresh Start",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.SageGreen
                            )
                        }
                        Text(
                            text = "You have ${state.overdueTasksCount} stale tasks. Life happens, and shame doesn't help you execute. 1-tap rollover moves them safely into your Icebox so you can start fresh today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.freshStartRollover() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.SageGreenDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("Reset My Day (Move to Icebox)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { viewModel.dismissZeroGuiltPrompt() },
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("Keep for Today", color = NeuroFlowThemeV2Colors.TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Evening Shutdown Ritual Card
            if (state.isEveningShutdownTime && !state.eveningShutdownDismissed) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🌙", fontSize = 18.sp)
                            Text(
                                text = "Evening Shutdown Ritual",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        }
                        Text(
                            text = "It's past 7:00 PM. Protect your circadian melatonin surge by closing cognitive loops. You conquered ${state.completedTasks.size} tasks today. Time to wind down.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                        Button(
                            onClick = { viewModel.completeEveningShutdown() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text("Complete Shutdown", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tag Filter Strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.availableTags) { tag ->
                    val isSelected = if (tag == "All") {
                        state.selectedTag == null || state.selectedTag == "All"
                    } else {
                        state.selectedTag == tag
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectTag(if (tag == "All") null else tag) },
                        label = { Text(tag, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            selectedLabelColor = Color.White,
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            labelColor = NeuroFlowThemeV2Colors.TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.heightIn(min = 44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Energy-Mapped Day Stream
            EnergyTimelineStream(
                scheduledTasks = state.filteredTasks,
                onTaskClick = { task -> onStartFocus(task.id) },
                onCompleteTask = { task -> viewModel.completeTask(task) },
                onDeferTask = { task -> viewModel.deferTask(task) },
                onBreakDownTask = { task -> viewModel.breakDownTask(task) }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showComposer) {
        SmartTaskComposerSheetV2(
            onDismiss = { showComposer = false },
            onSave = { task ->
                showComposer = false
                viewModel.addTask(task)
            },
            completedTasks = state.completedTasks
        )
    }

    if (showNotesSheet) {
        QuickNotesBottomSheet(
            notes = recentNotes,
            onSaveNote = { title, content -> viewModel.saveQuickNote(content, title) },
            onDismiss = { showNotesSheet = false },
            onOpenFullNotes = onNavigateToNotes
        )
    }
}

