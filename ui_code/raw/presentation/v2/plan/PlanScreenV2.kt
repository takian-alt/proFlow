package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.neuroflow.app.presentation.v2.flow.TaskSlotEnergyFitBadge
import com.neuroflow.app.presentation.v2.flow.UltradianRecoveryDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: PlanViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showNotesSheet by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(false) }
    var taskToLogTime by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToScheduleTime by remember { mutableStateOf<TaskEntity?>(null) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var isObjectivesExpanded by remember { mutableStateOf(false) }
    var newGoalTitle by remember { mutableStateOf("") }
    var showFrogConfetti by remember { mutableStateOf(false) }
    var showProtectedSlotSheet by remember { mutableStateOf(false) }

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
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleSearchVisible() },
                        modifier = Modifier.size(44.dp)
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
                        onClick = onNavigateToWeeklySchedule,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Weekly Schedule",
                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }

                    IconButton(
                        onClick = { showNotesSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Quick Notes",
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.triggerAutoScheduleNow() },
                        enabled = !state.isAutoScheduling,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 42.dp)
                    ) {
                        if (state.isAutoScheduling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // 2. Anchored Mode Selector Segmented Tabs (Never jumps vertically)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
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
                            activeContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            activeContentColor = NeuroFlowThemeV2Colors.TextPrimary,
                            inactiveContainerColor = NeuroFlowThemeV2Colors.SlateSurface,
                            inactiveContentColor = NeuroFlowThemeV2Colors.TextSecondary
                        ),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text(mode.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                    // Compact inline pills for Unscheduled Tray & Active Objectives
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
                                shape = RoundedCornerShape(8.dp)
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
                            onClick = { viewModel.selectTag(if (tag == "All") null else tag) },
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
                    state = state,
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
                    onEditTask = { task -> editingTask = task }
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
            onSave = { task ->
                showComposer = false
                viewModel.addTask(task)
            },
            completedTasks = state.tasksForDay.filter { it.completedAt != null }
        )
    }

    editingTask?.let { task ->
        SmartTaskComposerSheetV2(
            onDismiss = { editingTask = null },
            mode = SheetMode.Edit(task),
            circadianSummary = state.circadianSummary,
            onSave = { updated ->
                editingTask = null
                viewModel.updateTask(updated)
            },
            completedTasks = state.tasksForDay.filter { it.completedAt != null }
        )
    }

    taskToScheduleTime?.let { task ->
        val initialMinuteOfDay = task.timelineStartMinuteOfDay()
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

    FrogConfettiOverlay(trigger = showFrogConfetti)
    LaunchedEffect(showFrogConfetti) {
        if (showFrogConfetti) {
            kotlinx.coroutines.delay(2500)
            showFrogConfetti = false
        }
    }
}

@Composable
fun DateCarouselStrip(
    selectedDateMillis: Long,
    onSelectDate: (Long) -> Unit
) {
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val days = remember {
        (0..6).map { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = todayCal.timeInMillis
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
                isSameDay(todayCal.timeInMillis, dateMillis) -> "Today"
                isSameDay(todayCal.timeInMillis + 86400000L, dateMillis) -> "Tmrw"
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
    var runSum = 0
    for (i in 0..index) {
        runSum += tasks[i].estimatedDurationMinutes
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
    onEditTask: (TaskEntity) -> Unit = {}
) {
    val peakStart = state.preferences.peakEnergyStart.takeIf { it > 0 } ?: 9
    val peakEnd = state.preferences.peakEnergyEnd.takeIf { it > 0 } ?: 12
    val currentMinuteOfDay = remember {
        val cal = Calendar.getInstance()
        cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
    val listState = rememberLazyListState()

    // Scroll to the first upcoming task for today on initial load
    LaunchedEffect(state.selectedDateMillis, state.tasksForDay.size) {
        val upcomingIdx = state.tasksForDay.indexOfFirst { it.timelineStartMinuteOfDay() >= currentMinuteOfDay - 30 }
        if (upcomingIdx > 0) {
            listState.animateScrollToItem(upcomingIdx)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                val scheduledMinutes = state.tasksForDay.sumOf { it.estimatedDurationMinutes }
                val capacityMinutes = state.preferences.dailyDeepWorkTargetMinutes.takeIf { it > 0 }
                    ?: state.circadianSummary?.recommendedDeepWorkMinutes ?: 360
                com.neuroflow.app.presentation.v2.theme.CognitiveBudgetMeterCard(
                    scheduledMinutesToday = scheduledMinutes,
                    recommendedCapacityMinutes = capacityMinutes,
                    conflictCount = state.conflicts.size,
                    onAutoBalanceClick = onAutoBalanceClick
                )
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
                    val minuteOfDay = task.timelineStartMinuteOfDay()
                    val h = minuteOfDay / 60
                    val m = minuteOfDay % 60
                    val timeLabel = String.format("%02d:%02d", h, m)
                    val isDuringPeak = h in peakStart until peakEnd

                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { it * 0.55f },
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    onCompleteTask(task)
                                    false
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    onDeleteTask(task)
                                    false
                                }
                                SwipeToDismissBoxValue.Settled -> false
                            }
                        }
                    )

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
                                    .background(color, RoundedCornerShape(14.dp))
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
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDuringPeak) NeuroFlowThemeV2Colors.SlateSurfaceAlt else NeuroFlowThemeV2Colors.SlateSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isDuringPeak) NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.5f) else NeuroFlowThemeV2Colors.SlateBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                        color = if (isDuringPeak) NeuroFlowThemeV2Colors.WarmAmber else NeuroFlowThemeV2Colors.TextSecondary
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
                                    onClick = { onCompleteTask(task) },
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
                                        Text(
                                            text = "${task.estimatedDurationMinutes}m • ${task.energyLevel.name} • ${task.quadrant.name.replace('_', ' ')}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.TextSecondary
                                        )

                                        val isUltradianPart = task.id.contains("#part") || task.title.contains("(Part ")
                                        if (isUltradianPart) {
                                            val partLabel = if (task.id.contains("#part2") || task.title.contains("(Part 2/2)")) "🔗 Part 2/2" else "🔗 Part 1/2"
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = partLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
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

                                    val scheduledMillis = task.resolveScheduledEpochMillis() ?: (task.scheduledDate?.let { it + (task.scheduledTime ?: 0L) })
                                    if (scheduledMillis != null && state.energyTimeline.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        TaskSlotEnergyFitBadge(
                                            taskScheduledMillis = scheduledMillis,
                                            taskEnergyDemand = task.energyLevel,
                                            timeline = state.energyTimeline,
                                            onShiftToPeakClick = { onShiftToPeak(task) }
                                        )
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
                                val startH = slot.startMinuteOfDay / 60
                                val startM = slot.startMinuteOfDay % 60
                                val endH = (slot.startMinuteOfDay + slot.durationMinutes) / 60
                                val endM = (slot.startMinuteOfDay + slot.durationMinutes) % 60
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
                                            "${task.estimatedDurationMinutes}m • ${task.energyLevel.name}",
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
                        val startH = slot.startMinuteOfDay / 60
                        val startM = slot.startMinuteOfDay % 60
                        val endH = (slot.startMinuteOfDay + slot.durationMinutes) / 60
                        val endM = (slot.startMinuteOfDay + slot.durationMinutes) % 60
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
