package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.v2.flow.TaskSlotEnergyFitBadge
import com.neuroflow.app.presentation.v2.flow.UltradianRecoveryDivider
import com.neuroflow.app.presentation.v2.flow.formatDurationChip
import com.neuroflow.app.presentation.v2.flow.formatTimeRange
import com.neuroflow.app.presentation.v2.flow.parseSplitProgress
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDate
import java.time.ZoneId
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.presentation.common.FrogConfettiOverlay
import com.neuroflow.app.presentation.common.ManualTimeLogSheet
import com.neuroflow.app.presentation.v2.composer.SheetMode
import com.neuroflow.app.presentation.v2.composer.SmartTaskComposerSheetV2
import com.neuroflow.app.presentation.v2.notes.QuickNotesBottomSheet
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreenV2(
    onNavigateToFocus: (String) -> Unit,
    onNavigateToWeeklySchedule: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToMindmap: () -> Unit = {},
    onOpenToolsSheet: () -> Unit = {},
    viewModel: PlanViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val sevenDayLoads by viewModel.sevenDayLoadDistribution.collectAsState()
    val effectiveState = remember(state, sevenDayLoads) { state.copy(sevenDayLoadDistribution = sevenDayLoads) }
    val recentNotes by viewModel.recentNotes.collectAsState(initial = emptyList())
    val completedTasks by viewModel.completedTasks.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showNotesSheet by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(false) }
    var taskToLogTime by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToScheduleTime by remember { mutableStateOf<TaskEntity?>(null) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var isObjectivesExpanded by remember { mutableStateOf(false) }
    var newGoalTitle by remember { mutableStateOf("") }
    var showFrogConfetti by remember { mutableStateOf(false) }
    var showProtectedSlotSheet by remember { mutableStateOf(false) }
    var showInboxSweepSheet by remember { mutableStateOf(false) }

    val handleCompleteTask: (TaskEntity) -> Unit = { task ->
        if (task.isFrog) showFrogConfetti = true
        viewModel.completeTask(task)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Completed: ${task.title}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.uncompleteTask(task)
            }
        }
    }

    LaunchedEffect(state.pendingDeletedTask) {
        val task = state.pendingDeletedTask
        if (task != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted \"${task.title}\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteTask()
            } else {
                viewModel.confirmDeleteTask()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userActionMessage.collect { msg ->
            if (!msg.startsWith("Completed:")) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NeuroFlowThemeV2Colors.ElectricIndigo)
                    .clickable { showComposer = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = NeuroFlowThemeV2Colors.TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Anchored Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Executive Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Circadian Time-Blocking",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleSearchVisible() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (state.isSearchVisible || state.searchQuery.isNotBlank()) {
                                NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            } else {
                                NeuroFlowThemeV2Colors.TextSecondary
                            }
                        )
                    }

                    IconButton(
                        onClick = onOpenToolsSheet,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tools_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Tools and Subsystems",
                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }

                    Button(
                        onClick = { viewModel.openDailyBlueprint() },
                        enabled = !state.isAutoScheduling && !state.isBlueprintLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            contentColor = Color.White,
                            disabledContainerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            disabledContentColor = NeuroFlowThemeV2Colors.TextSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                            .testTag("auto_plan_day_button")
                    ) {
                        if (state.isAutoScheduling || state.isBlueprintLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Plan Day", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(
                color = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.15f),
                thickness = 0.75.dp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )

            // 2. Anchored Mode Selector Segmented Tabs (Never jumps vertically)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                border = BorderStroke(0.75.dp, NeuroFlowPaletteV2.specularBorderBrush()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PlanViewModeV2.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.viewMode == mode,
                            onClick = { viewModel.setViewMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = PlanViewModeV2.entries.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f),
                                activeContentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = NeuroFlowThemeV2Colors.TextSecondary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(mode.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 3. Collapsible Search Input Bar
            AnimatedVisibility(visible = state.isSearchVisible) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            "Search tasks, descriptions, or #tags...",
                            fontSize = 13.sp,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        unfocusedContainerColor = NeuroFlowThemeV2Colors.SlateSurface,
                        focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            }
                        }
                    }
                )
            }

            // 4. Shared Tag Filter Strip (Shown in Timeline & Matrix modes only)
            if (state.viewMode != PlanViewModeV2.AUTOPILOT) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact inline pills for Unscheduled Tray, Inbox Sweep & Active Objectives
                    if (state.unscheduledTasks.isNotEmpty()) {
                        item(key = "staging_pill") {
                            FilterChip(
                                selected = state.isStagingDrawerExpanded,
                                onClick = { viewModel.toggleStagingDrawer() },
                                label = {
                                    Text(
                                        text = "📥 Unscheduled (${state.unscheduledTasks.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.22f),
                                    selectedLabelColor = NeuroFlowThemeV2Colors.WarmAmber,
                                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    labelColor = NeuroFlowThemeV2Colors.WarmAmber
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = state.isStagingDrawerExpanded,
                                    borderColor = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.4f),
                                    selectedBorderColor = NeuroFlowThemeV2Colors.WarmAmber
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }

                        item(key = "inbox_sweep_pill") {
                            FilterChip(
                                selected = showInboxSweepSheet,
                                onClick = { showInboxSweepSheet = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                    )
                                },
                                label = {
                                    Text(
                                        text = "1-Min Sweep",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f),
                                    selectedLabelColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    labelColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("inbox_sweep_button")
                            )
                        }
                    }

                    item(key = "objectives_pill") {
                        FilterChip(
                            selected = isObjectivesExpanded,
                            onClick = { isObjectivesExpanded = !isObjectivesExpanded },
                            label = {
                                Text(
                                    text = if (state.activeGoals.isNotEmpty()) "🎯 Objectives (${state.activeGoals.size})" else "🎯 Objectives",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f),
                                selectedLabelColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                labelColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    items(state.availableTags, key = { it }) { tag ->
                        val isSelected = if (tag == "All") {
                            state.selectedTag == null || state.selectedTag == "All"
                        } else {
                            state.selectedTag == tag
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectTag(if (tag == "All" || tag == state.selectedTag) null else tag) },
                            label = {
                                Text(
                                    tag,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                selectedLabelColor = NeuroFlowThemeV2Colors.TextPrimary,
                                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                labelColor = NeuroFlowThemeV2Colors.TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Expandable Unscheduled Tray Drawer
                AnimatedVisibility(visible = state.isStagingDrawerExpanded && state.unscheduledTasks.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.unscheduledTasks, key = { it.id }) { unscheduled ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeuroFlowThemeV2Colors.SlateSurface,
                                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                                modifier = Modifier
                                    .width(210.dp)
                                    .clickable { editingTask = unscheduled }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = unscheduled.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NeuroFlowThemeV2Colors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${unscheduled.estimatedDurationMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.TextSecondary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${unscheduled.priority.name} • Tap to schedule",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.WarmAmber,
                                            fontSize = 10.sp
                                        )
                                    }
                                    val deferral = state.deferrals.firstOrNull { it.taskId == unscheduled.id }
                                    if (deferral != null) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "⏳ ${deferral.reasonCode.name.replace('_', ' ')}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeuroFlowThemeV2Colors.WarmAmber,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Expandable Objectives Drawer
                AnimatedVisibility(visible = isObjectivesExpanded) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.activeGoals.isEmpty()) {
                                Text(
                                    text = "No active objectives yet. Set a north-star goal below:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            } else {
                                state.activeGoals.forEach { goal ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.completeGoal(goal) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Complete Objective",
                                                tint = NeuroFlowThemeV2Colors.BioMint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = goal.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = NeuroFlowThemeV2Colors.TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteGoal(goal) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete Objective",
                                                tint = NeuroFlowThemeV2Colors.TextTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Inline Add Goal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newGoalTitle,
                                    onValueChange = { newGoalTitle = it },
                                    placeholder = { Text("New Objective...", fontSize = 12.sp, color = NeuroFlowThemeV2Colors.TextTertiary) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = NeuroFlowThemeV2Colors.TextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                        unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (newGoalTitle.isNotBlank()) {
                                            viewModel.addGoal(newGoalTitle)
                                            newGoalTitle = ""
                                        }
                                    },
                                    enabled = newGoalTitle.isNotBlank(),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Objective",
                                        tint = if (newGoalTitle.isNotBlank()) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Mode-Scoped Content View
            when (state.viewMode) {
                PlanViewModeV2.TIMELINE -> TimelineScheduleView(
                    state = effectiveState,
                    onSelectDate = { viewModel.selectDate(it) },
                    onResolveConflict = { viewModel.resolveConflict(it) },
                    onAutoBalanceClick = { viewModel.triggerAutoScheduleNow() },
                    onStartFocus = onNavigateToFocus,
                    onCompleteTask = handleCompleteTask,
                    onDeleteTask = { viewModel.requestDeleteTask(it) },
                    onBreakDownTask = { viewModel.breakDownTask(it) },
                    onShiftToPeak = { viewModel.shiftTaskToPeak(it) },
                    onAssignTaskTime = { task -> taskToScheduleTime = task },
                    onLogPastTime = { taskToLogTime = it },
                    onEditTask = { task -> editingTask = task },
                    onRatePlacement = { task, rating -> viewModel.ratePlacement(task, rating) }
                )
                PlanViewModeV2.MATRIX -> MatrixPlanView(
                    state = state,
                    onSelectQuadrant = { viewModel.selectQuadrant(it) },
                    onStartFocus = onNavigateToFocus,
                    onMoveQuadrant = { task, q -> viewModel.moveTaskToQuadrant(task, q) },
                    onCompleteTask = handleCompleteTask,
                    onBreakDownTask = { viewModel.breakDownTask(it) },
                    onEditTask = { task -> editingTask = task }
                )
                PlanViewModeV2.AUTOPILOT -> AutopilotConfigView(
                    state = state,
                    onToggleAutoSchedule = { viewModel.toggleAutoSchedule() },
                    onTriggerNow = { viewModel.triggerAutoScheduleNow() },
                    onToggleSlot = { id, enabled -> viewModel.toggleProtectedSlot(id, enabled) },
                    onManageSlotsClick = { showProtectedSlotSheet = true }
                )
            }
        }
    }

    if (showProtectedSlotSheet) {
        com.neuroflow.app.presentation.schedule.ProtectedSlotBottomSheet(
            protectedSlots = state.protectedSlots,
            onDismiss = { showProtectedSlotSheet = false },
            onSaveSlot = { slot -> viewModel.saveProtectedSlot(slot) },
            onDeleteSlot = { slotId -> viewModel.deleteProtectedSlot(slotId) },
            onToggleSlot = { slotId ->
                val slot = state.protectedSlots.firstOrNull { it.id == slotId }
                if (slot != null) {
                    viewModel.toggleProtectedSlot(slotId, !slot.isEnabled)
                }
            }
        )
    }

    if (showNotesSheet) {
        QuickNotesBottomSheet(
            notes = recentNotes,
            onSaveNote = { title, content -> viewModel.saveQuickNote(content, title) },
            onDismiss = { showNotesSheet = false },
            onOpenFullNotes = onNavigateToNotes,
            onCreateTaskSynthesis = { syn, content -> viewModel.createTaskFromNote(syn, content) },
            onCreateTask = { title, desc -> viewModel.createTaskFromNote(title, desc) },
            onSynthesizeNote = { viewModel.synthesizeQuickNote(it) }
        )
    }

    taskToLogTime?.let { task ->
        ManualTimeLogSheet(
            taskTitle = task.title,
            prefillMinutes = task.estimatedDurationMinutes,
            onConfirm = { minutes ->
                viewModel.logPastTime(task, minutes)
                taskToLogTime = null
            },
            onSkip = { taskToLogTime = null }
        )
    }

    if (showComposer) {
        SmartTaskComposerSheetV2(
            onDismiss = { showComposer = false },
            mode = SheetMode.Create,
            circadianSummary = state.circadianSummary,
            initialDate = state.selectedDateMillis,
            onSave = { task ->
                showComposer = false
                viewModel.addTask(task)
            },
            completedTasks = completedTasks
        )
    }

    editingTask?.let { task ->
        SmartTaskComposerSheetV2(
            onDismiss = { editingTask = null },
            mode = SheetMode.Edit(task),
            circadianSummary = state.circadianSummary,
            initialDate = state.selectedDateMillis,
            onSave = { updated ->
                editingTask = null
                viewModel.updateTask(updated)
            },
            completedTasks = completedTasks
        )
    }

    if (state.dailyBlueprint != null || state.isBlueprintLoading) {
        DailyBlueprintBottomSheet(
            blueprint = state.dailyBlueprint,
            isLoading = state.isBlueprintLoading,
            includedTaskIds = state.blueprintIncludedTaskIds,
            onToggleTask = { taskId, included -> viewModel.toggleBlueprintTask(taskId, included) },
            onApply = { viewModel.applyDailyBlueprint() },
            onDismiss = { viewModel.dismissDailyBlueprint() }
        )
    }

    taskToScheduleTime?.let { task ->
        key(task.id) {
            val hasExplicitTime = task.scheduledTime != null
            val initialMinuteOfDay = if (hasExplicitTime) task.timelineStartMinuteOfDay() else 9 * 60
            val initialHour = initialMinuteOfDay / 60
            val initialMinute = initialMinuteOfDay % 60
            val timePickerState = rememberTimePickerState(
                initialHour = if (initialHour in 0..23) initialHour else 9,
                initialMinute = if (initialMinute in 0..59) initialMinute else 0
            )
            AlertDialog(
                onDismissRequest = { taskToScheduleTime = null },
                title = {
                    Text(
                        text = "Assign Time Slot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.assignTaskTime(task, timePickerState.hour, timePickerState.minute)
                            taskToScheduleTime = null
                        }
                    ) {
                        Text("Confirm", color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskToScheduleTime = null }) {
                        Text("Cancel", color = NeuroFlowThemeV2Colors.TextSecondary)
                    }
                },
                containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
            )
        }
    }

    state.feedbackTask?.let { feedbackTask ->
        SchedulingFeedbackDialog(
            task = feedbackTask,
            remedyProposal = state.feedbackRemedyProposal,
            isLoading = state.isFeedbackLoading,
            onSelectReason = { reasonTag ->
                viewModel.submitDetailedFeedback(feedbackTask, reasonTag)
            },
            onAcceptRemedy = { newSlotMillis, directivePatch ->
                viewModel.acceptRemedySlot(feedbackTask, newSlotMillis, directivePatch)
            },
            onDismiss = {
                viewModel.dismissFeedbackDialog()
            }
        )
    }

    if (showInboxSweepSheet) {
        InboxSweepBottomSheet(
            unscheduledTasks = state.unscheduledTasks,
            onDoToday = { task -> viewModel.scheduleTaskForToday(task) },
            onAutoPlanWeek = {
                showInboxSweepSheet = false
                viewModel.autoBalance(horizonDays = 7)
            },
            onParkInNotes = { task -> viewModel.parkTaskInNotes(task) },
            onDismiss = { showInboxSweepSheet = false }
        )
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
fun SevenDayCapacityBar(
    loadDistribution: Map<LocalDate, Float>,
    selectedDateMillis: Long,
    modifier: Modifier = Modifier
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zoneId) }
    val days = remember(today) { (0..6).map { today.plusDays(it.toLong()) } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { date ->
            val load = (loadDistribution[date] ?: 0f).coerceIn(0f, 2.0f)
            val segmentColor = when {
                load > 1.0f -> NeuroFlowThemeV2Colors.PulseCoral
                load > 0.8f -> NeuroFlowThemeV2Colors.WarmAmber
                load > 0.0f -> NeuroFlowThemeV2Colors.BioMint
                else -> NeuroFlowThemeV2Colors.SlateBorder
            }
            val dateMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val isSelected = isSameDay(selectedDateMillis, dateMillis)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (isSelected) 6.dp else 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (load > 0f) segmentColor else NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
fun DateCarouselStrip(
    selectedDateMillis: Long,
    onSelectDate: (Long) -> Unit
) {
    val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
    val tmrwStart = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart)

    val days = remember(todayStart) {
        (0..6).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, offset)
            }
            cal.timeInMillis
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { dateMillis ->
            val isSelected = isSameDay(selectedDateMillis, dateMillis)
            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val dayName = when {
                isSameDay(todayStart, dateMillis) -> "Today"
                isSameDay(tmrwStart, dateMillis) -> "Tmrw"
                else -> SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
            }
            val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)

            Surface(
                onClick = { onSelectDate(dateMillis) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo else NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.SlateBorder
                ),
                modifier = Modifier
                    .width(60.dp)
                    .heightIn(min = 48.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) NeuroFlowThemeV2Colors.TextPrimary else NeuroFlowThemeV2Colors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) NeuroFlowThemeV2Colors.TextPrimary else NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
            }
        }
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun shouldShowUltradianBufferAfter(tasks: List<TaskEntity>, index: Int): Boolean {
    if (index >= tasks.size - 1) return false
    val current = tasks[index]
    val next = tasks[index + 1]

    // Untimed tasks must not trigger timeline ultradian recovery dividers
    if (current.scheduledTime == null || next.scheduledTime == null) return false

    // Always show buffer between split parts (e.g. Part 1/2 and Part 2/2, Part 2/3 and Part 3/3)
    val curPart = Regex("""[#_]part(\d+)""").find(current.id)?.groupValues?.get(1)?.toIntOrNull()
        ?: Regex("""\(Part (\d+)/""").find(current.title)?.groupValues?.get(1)?.toIntOrNull()
    val nextPart = Regex("""[#_]part(\d+)""").find(next.id)?.groupValues?.get(1)?.toIntOrNull()
        ?: Regex("""\(Part (\d+)/""").find(next.title)?.groupValues?.get(1)?.toIntOrNull()
    val isBetweenConsecutiveSplitParts = (curPart != null && nextPart != null && nextPart == curPart + 1) ||
        ((current.id.contains("#part1") || current.id.contains("_part1") || current.title.contains("(Part 1/2)")) &&
            (next.id.contains("#part2") || next.id.contains("_part2") || next.title.contains("(Part 2/2)")))
    if (isBetweenConsecutiveSplitParts) return true

    var lastTimedEndMinute: Int? = null
    var runSum = 0
    for (i in 0..index) {
        val t = tasks[i]
        if (t.scheduledTime == null) continue
        val curStart = t.timelineStartMinuteOfDay()
        if (lastTimedEndMinute != null && curStart - lastTimedEndMinute >= 15) {
            runSum = 0
        }
        runSum += com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(t)
        lastTimedEndMinute = curStart + com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(t)
        if (i == index) {
            return runSum >= 90
        }
        if (runSum >= 90) {
            runSum = 0
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScheduleView(
    state: PlanUiStateV2,
    onSelectDate: (Long) -> Unit = {},
    onResolveConflict: (com.neuroflow.app.domain.scheduler.ScheduleConflict) -> Unit = {},
    onAutoBalanceClick: () -> Unit = {},
    onStartFocus: (String) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit = {},
    onBreakDownTask: (TaskEntity) -> Unit = {},
    onShiftToPeak: (TaskEntity) -> Unit = {},
    onAssignTaskTime: (TaskEntity) -> Unit = {},
    onLogPastTime: (TaskEntity) -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {},
    onRatePlacement: (TaskEntity, Int) -> Unit = { _, _ -> }
) {
    val peakPrefs = state.preferences
    val manualPeak = if (peakPrefs.manualPeakProfileEnabled) (peakPrefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
    val peakStart = manualPeak ?: state.circadianSummary?.peakWindowStartHour
        ?: (if (peakPrefs.effectivePeakStart in 0..23) peakPrefs.effectivePeakStart else peakPrefs.peakEnergyStart).coerceIn(0, 23)
    val peakEnd = if (manualPeak != null) ((manualPeak + 3) % 24) else (state.circadianSummary?.peakWindowEndHour
        ?: (if (peakPrefs.effectivePeakEnd in 0..23) peakPrefs.effectivePeakEnd else peakPrefs.peakEnergyEnd).coerceIn(0, 23))
    val dipStart = state.circadianSummary?.dipWindowStartHour ?: 13
    val dipEnd = state.circadianSummary?.dipWindowEndHour ?: 15
    val currentMinuteOfDay = Calendar.getInstance().let {
        it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
    }
    val listState = rememberLazyListState()

    // Keep timeline anchored at top (Cognitive Budget Meter) on initial load and date changes
    LaunchedEffect(state.selectedDateMillis) {
        listState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SevenDayCapacityBar(
            loadDistribution = state.sevenDayLoadDistribution,
            selectedDateMillis = state.selectedDateMillis
        )

        DateCarouselStrip(
            selectedDateMillis = state.selectedDateMillis,
            onSelectDate = onSelectDate
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // 1. Daily Cognitive Budget Meter (Scoped inside scrollable timeline)
            item(key = "budget_meter") {
                val scheduledMinutes = if (state.scheduledMinutesToday > 0 || state.tasksForDay.isEmpty()) {
                    state.scheduledMinutesToday
                } else {
                    state.tasksForDay.sumOf { com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(it) }
                }
                val capacityMinutes = state.recommendedCapacityMinutes
                com.neuroflow.app.presentation.v2.theme.CognitiveBudgetMeterCard(
                    scheduledMinutesToday = scheduledMinutes,
                    recommendedCapacityMinutes = capacityMinutes,
                    completedMinutesToday = state.completedMinutesToday,
                    conflictCount = state.conflicts.size,
                    onAutoBalanceClick = onAutoBalanceClick
                )

                if (state.energyTimeline.isNotEmpty()) {
                    CircadianHorizonMiniBar(
                        timeline = state.energyTimeline,
                        peakStart = peakStart,
                        peakEnd = peakEnd,
                        dipStart = dipStart,
                        dipEnd = dipEnd
                    )
                }
            }

            // 2. Schedule Conflict Resolver Banner (Wired to move the conflicting task)
            if (state.conflicts.isNotEmpty()) {
                item(key = "conflicts_strip") {
                    ScheduleConflictResolverStrip(
                        conflicts = state.conflicts,
                        onResolveConflictClick = onResolveConflict
                    )
                }
            }

            // 3. Circadian Peak Indicator Badge
            item(key = "peak_window_badge") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚡", fontSize = 15.sp)
                        Text(
                            text = "Peak Circadian Window: ${String.format("%02d:00", peakStart)} – ${String.format("%02d:00", peakEnd)} (Optimal for Frogs & High Effort)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.WarmAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 4. Scheduled Tasks
            if (state.tasksForDay.isEmpty()) {
                item(key = "empty_timeline") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurface,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "📅 Clear Horizon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                            Text(
                                if (state.searchQuery.isNotBlank()) {
                                    "No scheduled tasks match \"${state.searchQuery}\" on this day."
                                } else {
                                    "No tasks scheduled for this day. Tap 'Auto-Plan' to schedule your backlog into your peak biological windows."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(state.tasksForDay, key = { _, it -> it.id }) { index, task ->
                    val hasTime = task.scheduledTime != null
                    val minuteOfDay = if (hasTime) task.timelineStartMinuteOfDay() else 0
                    val h = minuteOfDay / 60
                    val m = minuteOfDay % 60
                    val timeLabel = if (hasTime) String.format("%02d:%02d", h, m) else "All day"
                    val isDuringPeak = if (hasTime) {
                        if (peakStart <= peakEnd) h in peakStart until peakEnd else (h >= peakStart || h < peakEnd)
                    } else false

                    val currentTask by rememberUpdatedState(task)
                    val haptic = LocalHapticFeedback.current
                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { it * 0.55f },
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCompleteTask(currentTask)
                                    false
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDeleteTask(currentTask)
                                    false
                                }
                                SwipeToDismissBoxValue.Settled -> false
                            }
                        }
                    )

                    val scheduledMillis = if (task.scheduledTime != null) {
                        task.resolveScheduledEpochMillis(state.selectedDateMillis) ?: (task.scheduledDate?.let { it + (task.scheduledTime ?: 0L) })
                    } else null

                    val taskPhase = if (scheduledMillis != null && state.energyTimeline.isNotEmpty()) {
                        val timelineStart = state.energyTimeline.firstOrNull()?.timestampMillis?.let { com.neuroflow.app.data.local.entity.startOfLocalDay(it) }
                            ?: com.neuroflow.app.data.local.entity.startOfLocalDay(state.selectedDateMillis)
                        val alignedMillis = timelineStart + task.timelineStartMinuteOfDay() * 60_000L
                        state.energyTimeline.minByOrNull { kotlin.math.abs(it.timestampMillis - alignedMillis) }?.phase ?: EnergyPhase.PEAK
                    } else if (isDuringPeak) {
                        EnergyPhase.PEAK
                    } else {
                        when (task.energyLevel) {
                            com.neuroflow.app.domain.model.EnergyLevel.HIGH -> EnergyPhase.PEAK
                            com.neuroflow.app.domain.model.EnergyLevel.MEDIUM -> EnergyPhase.REBOUND
                            com.neuroflow.app.domain.model.EnergyLevel.LOW -> EnergyPhase.RECOVERY
                        }
                    }
                    val accentColor = NeuroFlowPaletteV2.accentForPhase(taskPhase)
                    val cardShape = RoundedCornerShape(14.dp)

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.25f)
                                SwipeToDismissBoxValue.EndToStart -> NeuroFlowThemeV2Colors.RoseCoralContainer.copy(alpha = 0.5f)
                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                            }
                            val alignment = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                SwipeToDismissBoxValue.Settled -> Alignment.Center
                            }
                            val icon = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.CheckCircle
                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                SwipeToDismissBoxValue.Settled -> null
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, cardShape)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                            NeuroFlowThemeV2Colors.RoseCoral
                                        } else {
                                            NeuroFlowThemeV2Colors.TextPrimary
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = cardShape,
                            color = if (isDuringPeak) NeuroFlowThemeV2Colors.SlateSurfaceAlt else NeuroFlowThemeV2Colors.SlateSurface,
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush(accentColor, 0.85f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(accentColor)
                                )
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                // Time Pill
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(48.dp)
                                ) {
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDuringPeak) NeuroFlowThemeV2Colors.WarmAmber else NeuroFlowThemeV2Colors.TextSecondary,
                                        fontSize = if (!hasTime) 11.sp else 12.sp,
                                        maxLines = 1
                                    )
                                    if (isDuringPeak) {
                                        Text(
                                            "PEAK",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = NeuroFlowThemeV2Colors.WarmAmber
                                        )
                                    }
                                }

                                // Complete check
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onCompleteTask(task)
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Complete",
                                        tint = NeuroFlowThemeV2Colors.TextSecondary
                                    )
                                }

                                // Title & Metadata (Tap to Edit)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onEditTask(task) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (task.isFrog) Text("🐸", fontSize = 14.sp)
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeuroFlowThemeV2Colors.TextPrimary,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Duration badge
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "⚡ ${formatDurationChip(com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Start and end time chip (for scheduled tasks)
                                        if (scheduledMillis != null) {
                                            val timeRangeStr = formatTimeRange(scheduledMillis, com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                                            ) {
                                                Text(
                                                    text = "⏱️ $timeRangeStr",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeuroFlowThemeV2Colors.TextSecondary,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${task.energyLevel.name} • ${task.quadrant.name.replace('_', ' ')}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.TextSecondary
                                        )

                                        val splitProgress = parseSplitProgress(task)
                                        if (splitProgress != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Part ${splitProgress.partNumber}/${splitProgress.totalParts}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    LinearProgressIndicator(
                                                        progress = { splitProgress.progress },
                                                        modifier = Modifier
                                                            .width(32.dp)
                                                            .height(4.dp)
                                                            .clip(RoundedCornerShape(2.dp)),
                                                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                        trackColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.3f)
                                                    )
                                                }
                                            }
                                        }

                                        if (task.postponeCount >= 3) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "⚡ Dopamine Warm-Up",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeuroFlowThemeV2Colors.WarmAmber,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    val scheduledMillis = if (task.scheduledTime != null) {
                                        task.resolveScheduledEpochMillis(state.selectedDateMillis) ?: (task.scheduledDate?.let { it + (task.scheduledTime ?: 0L) })
                                    } else null
                                    if (scheduledMillis != null && state.energyTimeline.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            TaskSlotEnergyFitBadge(
                                                taskScheduledMillis = scheduledMillis,
                                                taskEnergyDemand = task.energyLevel,
                                                timeline = state.energyTimeline,
                                                onShiftToPeakClick = { onShiftToPeak(task) }
                                            )
                                            TaskCircadianAlignmentBadge(
                                                task = task,
                                                scheduledMillis = scheduledMillis,
                                                timeline = state.energyTimeline,
                                                onShiftToPeak = { onShiftToPeak(task) }
                                            )
                                        }
                                    }

                                    if (task.isAutoScheduled) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.15f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "✨ AI Placed",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { onRatePlacement(task, if (task.userSchedulingRating == 1) 0 else 1) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (task.userSchedulingRating == 1) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                                        contentDescription = "Good placement",
                                                        tint = if (task.userSchedulingRating == 1) NeuroFlowThemeV2Colors.BioMint else NeuroFlowThemeV2Colors.TextSecondary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onRatePlacement(task, if (task.userSchedulingRating == -1) 0 else -1) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (task.userSchedulingRating == -1) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
                                                        contentDescription = "Disliked time window",
                                                        tint = if (task.userSchedulingRating == -1) NeuroFlowThemeV2Colors.RoseCoral else NeuroFlowThemeV2Colors.TextSecondary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val microSteps = remember(task.ifThenPlan) { StepByStepPlanCodec.parse(task.ifThenPlan) }
                                    if (microSteps.isNotEmpty()) {
                                        var showSteps by remember { mutableStateOf(false) }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.18f),
                                            modifier = Modifier.clickable { showSteps = !showSteps }
                                        ) {
                                            Text(
                                                text = "🪜 ${microSteps.size} micro-steps ${if (showSteps) "▲" else "▼"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (showSteps) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                microSteps.forEachIndexed { i, step ->
                                                    Text(
                                                        text = "${i + 1}. ${step.text}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = NeuroFlowThemeV2Colors.TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Focus Button
                                IconButton(
                                    onClick = { onStartFocus(task.id) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Start Focus",
                                        tint = NeuroFlowThemeV2Colors.ElectricIndigo
                                    )
                                }

                                // Consolidated Overflow Menu (AI Breakdown, Edit, Log Time)
                                var showItemMenu by remember { mutableStateOf(false) }
                                val isDecomposing = state.decomposingTaskId == task.id
                                Box {
                                    IconButton(
                                        onClick = { showItemMenu = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        if (isDecomposing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Task options",
                                                tint = NeuroFlowThemeV2Colors.TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showItemMenu,
                                        onDismissRequest = { showItemMenu = false },
                                        modifier = Modifier.background(NeuroFlowThemeV2Colors.SlateSurfaceAlt)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Break Down (AI Micro-Steps)", color = NeuroFlowThemeV2Colors.TextPrimary) },
                                            leadingIcon = {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                            },
                                            onClick = {
                                                showItemMenu = false
                                                onBreakDownTask(task)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Edit Task", color = NeuroFlowThemeV2Colors.TextPrimary) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                            },
                                            onClick = {
                                                showItemMenu = false
                                                onEditTask(task)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Assign Time Slot", color = NeuroFlowThemeV2Colors.TextPrimary) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Schedule, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                            },
                                            onClick = {
                                                showItemMenu = false
                                                onAssignTaskTime(task)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Log Past Time", color = NeuroFlowThemeV2Colors.TextPrimary) },
                                            leadingIcon = {
                                                Icon(Icons.Default.History, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                                            },
                                            onClick = {
                                                showItemMenu = false
                                                onLogPastTime(task)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                    if (shouldShowUltradianBufferAfter(state.tasksForDay, index)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        UltradianRecoveryDivider()
                    }
                }
            }

            // 5. Protected Time Slots Section
            if (state.protectedSlots.any { it.isEnabled }) {
                item(key = "protected_slots_header") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🛡️ Protected Deep Work & Rest Slots",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }

                items(state.protectedSlots.filter { it.isEnabled }, key = { "slot_${it.id}" }) { slot ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeuroFlowThemeV2Colors.ElectricIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    slot.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                                val startH = (slot.startMinuteOfDay % 1440) / 60
                                val startM = slot.startMinuteOfDay % 60
                                val endMinuteOfDay = (slot.startMinuteOfDay + slot.durationMinutes) % 1440
                                val endH = endMinuteOfDay / 60
                                val endM = endMinuteOfDay % 60
                                Text(
                                    text = String.format("%02d:%02d – %02d:%02d (%s)", startH, startM, endH, endM, slot.category.name),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixPlanView(
    state: PlanUiStateV2,
    onSelectQuadrant: (Quadrant?) -> Unit = {},
    onStartFocus: (String) -> Unit,
    onMoveQuadrant: (TaskEntity, Quadrant) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onBreakDownTask: (TaskEntity) -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {}
) {
    val coral = NeuroFlowThemeV2Colors.MutedCoral
    val indigoGlow = NeuroFlowThemeV2Colors.ElectricIndigoGlow
    val sage = NeuroFlowThemeV2Colors.SageGreen
    val textTertiary = NeuroFlowThemeV2Colors.TextTertiary

    val allQuadrants = remember(coral, indigoGlow, sage, textTertiary) {
        listOf(
            Quadrant.DO_FIRST to ("🎯 Do First (High Impact & Frog)" to coral),
            Quadrant.SCHEDULE to ("🔵 Schedule (High Impact, Deep Focus)" to indigoGlow),
            Quadrant.DELEGATE to ("🟢 Quick Wins / Low Energy" to sage),
            Quadrant.ELIMINATE to ("⚪ Backlog / Low Priority" to textTertiary)
        )
    }

    val countsByQuad = remember(state.tasksByQuadrant) {
        Quadrant.entries.associateWith { q -> state.tasksByQuadrant[q]?.size ?: 0 }
    }

    val visibleQuadrants = remember(allQuadrants, state.selectedQuadrant) {
        if (state.selectedQuadrant == null) {
            allQuadrants
        } else {
            allQuadrants.filter { it.first == state.selectedQuadrant }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp)
    ) {
        // Interactive 2x2 Quadrant Filter Grid (Now accurately reflects Matrix counts & filters view)
        item(key = "matrix_filter_grid") {
            CompactEisenhowerMatrixFilter(
                countsByQuadrant = countsByQuad,
                selectedQuadrant = state.selectedQuadrant,
                onSelectQuadrant = onSelectQuadrant
            )
        }

        items(visibleQuadrants, key = { it.first.name }) { (quadrant, info) ->
            val (title, color) = info
            val tasks = state.tasksByQuadrant[quadrant] ?: emptyList()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Box(
                            modifier = Modifier
                                .background(color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${tasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (tasks.isEmpty()) {
                        Text(
                            text = "No tasks in this quadrant",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextTertiary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        tasks.forEach { task ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onCompleteTask(task) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Complete",
                                            tint = NeuroFlowThemeV2Colors.TextSecondary
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onEditTask(task) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (task.isFrog) Text("🐸", fontSize = 12.sp)
                                            Text(
                                                task.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            "${com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task)}m • ${task.energyLevel.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.TextSecondary
                                        )

                                        val matrixSteps = remember(task.ifThenPlan) { StepByStepPlanCodec.parse(task.ifThenPlan) }
                                        if (matrixSteps.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "🪜 ${matrixSteps.size} steps",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    val isDecomposingMatrix = state.decomposingTaskId == task.id
                                    IconButton(
                                        onClick = { onBreakDownTask(task) },
                                        modifier = Modifier.size(40.dp),
                                        enabled = !isDecomposingMatrix
                                    ) {
                                        if (isDecomposingMatrix) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = "Break down",
                                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            val nextQ = when (quadrant) {
                                                Quadrant.DO_FIRST -> Quadrant.SCHEDULE
                                                Quadrant.SCHEDULE -> Quadrant.DELEGATE
                                                Quadrant.DELEGATE -> Quadrant.ELIMINATE
                                                Quadrant.ELIMINATE -> Quadrant.DO_FIRST
                                            }
                                            onMoveQuadrant(task, nextQ)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Cycle Quadrant",
                                            tint = NeuroFlowThemeV2Colors.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onStartFocus(task.id) },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Focus",
                                            tint = NeuroFlowThemeV2Colors.ElectricIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutopilotConfigView(
    state: PlanUiStateV2,
    onToggleAutoSchedule: () -> Unit,
    onTriggerNow: () -> Unit,
    onToggleSlot: (String, Boolean) -> Unit,
    onManageSlotsClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
    ) {
        item(key = "autopilot_master") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🤖 Circadian Autopilot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                            Text(
                                "Automatically distributes your tasks across the day matched to your biological alertness and energy curves.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary
                            )
                        }
                        Switch(
                            checked = state.isAutoScheduleOn,
                            onCheckedChange = { onToggleAutoSchedule() },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Button(
                        onClick = onTriggerNow,
                        enabled = !state.isAutoScheduling,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
                    ) {
                        if (state.isAutoScheduling) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NeuroFlowThemeV2Colors.TextPrimary)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Auto-Schedule Optimization Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item(key = "autopilot_rules") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🧠 Neuroscience Auto-Scheduling Rules",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                    Text(
                        "• Process C Alertness Matching: High cognitive effort and Frog tasks are assigned to peak circadian windows.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                    Text(
                        "• Protected Buffer Preservation: Deep work contracts and rest intervals are strictly respected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                    Text(
                        "• Cognitive Fatigue Guard: Tasks exceeding 60m are scheduled with automatic recharge buffers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }
        }

        item(key = "protected_slots_title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🛡️ Manage Protected Time Slots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.TextPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onManageSlotsClick) {
                    Text("+ Add Slot", color = NeuroFlowThemeV2Colors.ElectricIndigo, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (state.protectedSlots.isEmpty()) {
            item(key = "protected_slots_empty") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onManageSlotsClick() },
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No protected time slots configured.", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("+ Create your first protected block", style = MaterialTheme.typography.labelMedium, color = NeuroFlowThemeV2Colors.ElectricIndigo, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        items(state.protectedSlots, key = { it.id }) { slot ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            slot.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                        val startH = (slot.startMinuteOfDay % 1440) / 60
                        val startM = slot.startMinuteOfDay % 60
                        val endMinuteOfDay = (slot.startMinuteOfDay + slot.durationMinutes) % 1440
                        val endH = endMinuteOfDay / 60
                        val endM = endMinuteOfDay % 60
                        Text(
                            text = String.format("%02d:%02d – %02d:%02d (%s)", startH, startM, endH, endM, slot.category.name),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }

                    Switch(
                        checked = slot.isEnabled,
                        onCheckedChange = { onToggleSlot(slot.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CircadianHorizonMiniBar(
    timeline: List<com.neuroflow.app.domain.service.CircadianSnapshot>,
    peakStart: Int,
    peakEnd: Int,
    dipStart: Int,
    dipEnd: Int,
    modifier: Modifier = Modifier
) {
    if (timeline.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = timeline.map { snap ->
                            when (snap.phase) {
                                com.neuroflow.app.domain.service.EnergyPhase.SURGE -> Color(0xFF06B6D4)
                                com.neuroflow.app.domain.service.EnergyPhase.PEAK,
                                com.neuroflow.app.domain.service.EnergyPhase.MORNING_RAMP -> NeuroFlowPaletteV2.OptimalEmerald
                                com.neuroflow.app.domain.service.EnergyPhase.REBOUND -> Color(0xFF6366F1)
                                com.neuroflow.app.domain.service.EnergyPhase.DIP -> Color(0xFFF59E0B)
                                com.neuroflow.app.domain.service.EnergyPhase.WIND_DOWN -> Color(0xFFD97706)
                                else -> Color(0xFF1E293B)
                            }
                        }
                    )
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Peak: ${String.format(Locale.ROOT, "%02d:00", peakStart)}–${String.format(Locale.ROOT, "%02d:00", peakEnd)}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = NeuroFlowPaletteV2.OptimalEmerald,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "☕ Dip: ${String.format(Locale.ROOT, "%02d:00", dipStart)}–${String.format(Locale.ROOT, "%02d:00", dipEnd)}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = NeuroFlowPaletteV2.PeakAmber,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TaskCircadianAlignmentBadge(
    task: TaskEntity,
    scheduledMillis: Long,
    timeline: List<com.neuroflow.app.domain.service.CircadianSnapshot>,
    onShiftToPeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alignment = remember(task, scheduledMillis, timeline) {
        PlanViewModelV2.evaluateCircadianAlignmentForSlot(task, scheduledMillis, timeline)
    }
    if (alignment.status == CircadianAlignmentStatus.NEUTRAL) return

    val badgeColor = when (alignment.status) {
        CircadianAlignmentStatus.OPTIMAL_PEAK -> NeuroFlowPaletteV2.OptimalEmerald
        CircadianAlignmentStatus.DIP_MISMATCH -> NeuroFlowPaletteV2.PeakAmber
        CircadianAlignmentStatus.CANNIBALIZES_PEAK -> NeuroFlowPaletteV2.TextTertiary
        CircadianAlignmentStatus.NEUTRAL -> NeuroFlowPaletteV2.TextSecondary
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.12f))
            .border(0.75.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = alignment.label,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (alignment.status == CircadianAlignmentStatus.DIP_MISMATCH) {
            Text(
                text = "• Shift to Peak",
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onShiftToPeak() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSweepBottomSheet(
    unscheduledTasks: List<TaskEntity>,
    onDoToday: (TaskEntity) -> Unit,
    onAutoPlanWeek: () -> Unit,
    onParkInNotes: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeuroFlowThemeV2Colors.SlateSurface,
        contentColor = NeuroFlowThemeV2Colors.TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("inbox_sweep_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1-Minute Inbox Sweep",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    Text(
                        text = "${unscheduledTasks.size} unscheduled item(s) waiting — assign fast or park clutter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NeuroFlowThemeV2Colors.TextSecondary)
                }
            }

            if (unscheduledTasks.isNotEmpty()) {
                Button(
                    onClick = onAutoPlanWeek,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("inbox_sweep_auto_plan_week")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-Plan All Across Week", fontWeight = FontWeight.ExtraBold)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(unscheduledTasks, key = { it.id }) { task ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NeuroFlowThemeV2Colors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${task.estimatedDurationMinutes}m · ${task.priority.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowThemeV2Colors.WarmAmber
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { onDoToday(task) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.22f),
                                            contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                    ) {
                                        Text("Do Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onParkInNotes(task) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                    ) {
                                        Text("Park in Notes", fontSize = 12.sp, color = NeuroFlowThemeV2Colors.TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.BioMint.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Inbox Zero Achieved", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.BioMint)
                        Text("All active tasks have a scheduled day or home.", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextSecondary)
                    }
                }
            }
        }
    }
}

