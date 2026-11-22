package com.neuroflow.app.presentation.v2.mindmap

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.assistant.GoalBreakdownMilestone
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.v2.composer.SheetMode
import com.neuroflow.app.presentation.v2.composer.SmartTaskComposerSheetV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

private data class OutlineRowItem(
    val task: TaskEntity,
    val depth: Int,
    val previousSiblingId: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsHubScreenV2(
    onStartFocus: (String) -> Unit,
    viewModel: MindmapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val composerPrefs by viewModel.userPreferences.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showGoalEditorForCreate by remember { mutableStateOf(false) }
    var editingGoalEntity by remember { mutableStateOf<GoalEntity?>(null) }
    var editingTaskEntity by remember { mutableStateOf<TaskEntity?>(null) }

    LaunchedEffect(state.actionMessage) {
        val msg = state.actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearActionMessage()
        }
    }

    // Back press from single-goal workspace returns cleanly to Goals Overview
    BackHandler(enabled = state.selectedGoalId != null) {
        viewModel.clearSelectedGoal()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val activeSummary = state.goalSummaries.firstOrNull { it.goal.id == state.selectedGoalId }
            if (activeSummary == null) {
                GoalsOverviewContent(
                    summaries = state.goalSummaries,
                    selectedTagFilter = state.selectedTagFilter,
                    onSelectTagFilter = { viewModel.setGoalTagFilter(it) },
                    onQuickCreateGoal = { title -> viewModel.quickCreateGoalAndOpen(title) },
                    onOpenCreateGoalEditor = { showGoalEditorForCreate = true },
                    onEditGoal = { goal -> editingGoalEntity = goal },
                    onOpenGoal = { goalId -> viewModel.selectGoal(goalId) },
                    onPushNextToToday = { taskId -> viewModel.pushTaskToToday(taskId) },
                    onStartFocus = onStartFocus
                )
            } else {
                SingleGoalWorkspaceContent(
                    summary = activeSummary,
                    state = state,
                    onBackToOverview = { viewModel.clearSelectedGoal() },
                    onEditGoal = { editingGoalEntity = activeSummary.goal },
                    onSelectWorkspaceMode = { viewModel.setWorkspaceMode(it) },
                    onToggleShowAllGoalsInMap = { viewModel.toggleShowAllGoalsInMap() },
                    onQuickAddStep = { title, parentId, priority ->
                        viewModel.quickAddOutlineStep(activeSummary.goal.id, title, parentId, priority)
                    },
                    onToggleTaskCompletion = { viewModel.onToggleTaskCompletion(it) },
                    onCycleTaskPriority = { viewModel.cycleTaskPriority(it) },
                    onEditTask = { editingTaskEntity = it },
                    onIndentTask = { taskId, newParent -> viewModel.indentOutlineTask(taskId, newParent) },
                    onOutdentTask = { taskId -> viewModel.outdentOutlineTask(taskId) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onPushToToday = { viewModel.pushTaskToToday(it) },
                    onAutoScheduleGoal = { viewModel.autoScheduleGoalNextSteps(activeSummary.goal.id) },
                    onRequestSmartBreakdown = { viewModel.requestSmartBreakdown(activeSummary.goal.id) },
                    onStartFocus = onStartFocus,
                    viewModel = viewModel
                )
            }
        }
    }

    // Create / Edit Goal Modal Bottom Sheet (Priority, Tags, Target Horizon, Auto-Schedule preview)
    if (showGoalEditorForCreate || editingGoalEntity != null) {
        val targetGoal = editingGoalEntity
        GoalEditorSheet(
            existingGoal = targetGoal,
            onDismiss = {
                showGoalEditorForCreate = false
                editingGoalEntity = null
            },
            onSave = { title, description, targetDate, priority, tagsCsv, syncTagsToSteps ->
                if (targetGoal == null) {
                    viewModel.createGoal(
                        title = title,
                        description = description,
                        targetDate = targetDate,
                        priority = priority,
                        tags = tagsCsv
                    )
                } else {
                    viewModel.updateGoal(
                        goalId = targetGoal.id,
                        title = title,
                        description = description,
                        targetDate = targetDate,
                        priority = priority,
                        tags = tagsCsv,
                        syncTagsToExistingTasks = syncTagsToSteps
                    )
                }
                showGoalEditorForCreate = false
                editingGoalEntity = null
            },
            onDelete = targetGoal?.let { g ->
                {
                    viewModel.deleteGoal(g.id)
                    editingGoalEntity = null
                }
            }
        )
    }

    // Full Task Step Editor Sheet (Priority, Tags, Energy, Duration, Deadline, Recommended Slot)
    editingTaskEntity?.let { taskToEdit ->
        SmartTaskComposerSheetV2(
            mode = SheetMode.Edit(taskToEdit),
            activeTasks = state.allRawTasks,
            preferences = composerPrefs,
            onDismiss = { editingTaskEntity = null },
            onSave = { updatedTask ->
                viewModel.updateTask(updatedTask)
                editingTaskEntity = null
            }
        )
    }

    // Smart Breakdown Preview Modal Sheet
    val breakdown = state.breakdownPreview
    val activeGoalId = state.selectedGoalId
    if (breakdown != null && activeGoalId != null) {
        SmartBreakdownPreviewSheet(
            milestones = breakdown,
            onApply = { selected -> viewModel.applySmartBreakdown(activeGoalId, selected) },
            onDismiss = { viewModel.dismissSmartBreakdown() }
        )
    }
}

@Composable
private fun GoalsOverviewContent(
    summaries: List<GoalSummaryUi>,
    selectedTagFilter: String?,
    onSelectTagFilter: (String?) -> Unit,
    onQuickCreateGoal: (String) -> Unit,
    onOpenCreateGoalEditor: () -> Unit,
    onEditGoal: (GoalEntity) -> Unit,
    onOpenGoal: (String) -> Unit,
    onPushNextToToday: (String) -> Unit,
    onStartFocus: (String) -> Unit
) {
    var newGoalTitle by remember { mutableStateOf("") }

    val availableTags = remember(summaries) {
        summaries.flatMap { parseTagsList(it.goal.tags) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    val filteredSummaries = remember(summaries, selectedTagFilter) {
        if (selectedTagFilter.isNullOrBlank()) {
            summaries
        } else {
            summaries.filter { summary ->
                parseTagsList(summary.goal.tags).any { it.equals(selectedTagFilter, ignoreCase = true) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("goals_overview_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Goals & Mind Maps",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeuroFlowThemeV2Colors.TextPrimary
                )
                Text(
                    text = "Tag and prioritize goals, re-rank steps anytime, and auto-schedule them into energy-matched slots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )
            }
        }

        // Paper-fast "+ New Goal" capture bar + Full Editor trigger (Priority & Tags)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = newGoalTitle,
                        onValueChange = { newGoalTitle = it },
                        placeholder = {
                            Text(
                                "New goal (e.g. Launch Portfolio, Exam Prep)...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeuroFlowThemeV2Colors.TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newGoalTitle.isNotBlank()) {
                                    onQuickCreateGoal(newGoalTitle)
                                    newGoalTitle = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_new_goal_input")
                    )
                    FilledTonalIconButton(
                        onClick = onOpenCreateGoalEditor,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("goals_open_editor_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Create goal with priority and tags")
                    }
                    FilledIconButton(
                        onClick = {
                            if (newGoalTitle.isNotBlank()) {
                                onQuickCreateGoal(newGoalTitle)
                                newGoalTitle = ""
                            }
                        },
                        enabled = newGoalTitle.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("quick_new_goal_submit")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create goal")
                    }
                }
            }
        }

        // Tag Filter Strip when goals have tags
        if (availableTags.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTagFilter == null,
                        onClick = { onSelectTagFilter(null) },
                        label = {
                            Text(
                                text = "All (${summaries.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTagFilter == null) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("goals_filter_tag_all")
                    )
                    availableTags.forEach { tag ->
                        val isSelected = selectedTagFilter.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTagFilter(if (isSelected) null else tag) },
                            label = {
                                Text(
                                    text = "#$tag",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("goals_filter_tag_${tag.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filteredSummaries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.16f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(24.dp)
                            )
                        }
                        Text(
                            text = if (selectedTagFilter != null) "No goals tagged #$selectedTagFilter" else "Start your first goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                        Text(
                            text = if (selectedTagFilter != null) {
                                "Tap All above to see all goals, or edit a goal to assign #$selectedTagFilter."
                            } else {
                                "Type a goal above or tap the options icon to set Priority & Smart Tags for Auto-Schedule."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredSummaries, key = { it.goal.id }) { summary ->
                GoalOverviewCard(
                    summary = summary,
                    onOpen = { onOpenGoal(summary.goal.id) },
                    onEdit = { onEditGoal(summary.goal) },
                    onPushNextToToday = {
                        summary.nextActionableTask?.let { onPushNextToToday(it.id) }
                    },
                    onStartNextFocus = {
                        summary.nextActionableTask?.let { onStartFocus(it.id) }
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun GoalOverviewCard(
    summary: GoalSummaryUi,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onPushNextToToday: () -> Unit,
    onStartNextFocus: () -> Unit
) {
    val goalTags = remember(summary.goal.tags) { parseTagsList(summary.goal.tags) }

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${summary.goal.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = priorityAccentColor(summary.goal.priority),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = summary.goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PriorityBadgePill(priority = summary.goal.priority)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                    ) {
                        Text(
                            text = "${summary.completedTasks}/${summary.totalTasks}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("goal_edit_btn_${summary.goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit goal priority and tags",
                            tint = NeuroFlowThemeV2Colors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (goalTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    goalTags.forEach { tag ->
                        GoalTagPill(tag = tag)
                    }
                }
            }

            LinearProgressIndicator(
                progress = { summary.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeuroFlowPaletteV2.OptimalEmerald,
                trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
            )

            val nextTask = summary.nextActionableTask
            if (nextTask != null) {
                val nextTaskTags = remember(nextTask.tags) { parseTagsList(nextTask.tags).take(2) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(priorityAccentColor(nextTask.priority))
                                )
                                Text(
                                    text = "NEXT STEP · ${priorityShortLabel(nextTask.priority)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeuroFlowPaletteV2.ReboundCyan,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = nextTask.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (nextTaskTags.isNotEmpty()) {
                                Text(
                                    text = nextTaskTags.joinToString(" ") { "#$it" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextTertiary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = onPushNextToToday,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.22f),
                                    contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("goal_push_today_${summary.goal.id}")
                            ) {
                                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Today", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = onStartNextFocus,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start focus on next step",
                                    tint = NeuroFlowPaletteV2.OptimalEmerald
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = if (summary.totalTasks == 0) "Tap to break this goal into steps →" else "All steps completed!",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (summary.totalTasks == 0) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowPaletteV2.OptimalEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleGoalWorkspaceContent(
    summary: GoalSummaryUi,
    state: MindmapUiState,
    onBackToOverview: () -> Unit,
    onEditGoal: () -> Unit,
    onSelectWorkspaceMode: (GoalWorkspaceMode) -> Unit,
    onToggleShowAllGoalsInMap: () -> Unit,
    onQuickAddStep: (String, String?, Priority) -> Unit,
    onToggleTaskCompletion: (String) -> Unit,
    onCycleTaskPriority: (String) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onIndentTask: (String, String) -> Unit,
    onOutdentTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onPushToToday: (String) -> Unit,
    onAutoScheduleGoal: () -> Unit,
    onRequestSmartBreakdown: () -> Unit,
    onStartFocus: (String) -> Unit,
    viewModel: MindmapViewModel
) {
    val goalTags = remember(summary.goal.tags) { parseTagsList(summary.goal.tags) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Focused Workspace Top Bar
        Surface(
            color = NeuroFlowThemeV2Colors.SlateSurface,
            border = BorderStroke(0.5.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onBackToOverview,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("goal_workspace_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to all goals",
                            tint = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onEditGoal() }
                    ) {
                        Text(
                            text = summary.goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeuroFlowThemeV2Colors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${summary.completedTasks} of ${summary.totalTasks} steps done",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onEditGoal,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("goal_workspace_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit goal priority and tags",
                            tint = NeuroFlowThemeV2Colors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (state.workspaceMode == GoalWorkspaceMode.MINDMAP) {
                        FilterChip(
                            selected = state.showAllGoalsInMap,
                            onClick = onToggleShowAllGoalsInMap,
                            label = {
                                Text(
                                    if (state.showAllGoalsInMap) "All Goals" else "This Goal",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    } else {
                        FilledTonalButton(
                            onClick = onRequestSmartBreakdown,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f),
                                contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("goal_smart_breakdown_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Break Down", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Tappable Goal Priority & Tags strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .clickable { onEditGoal() }
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PriorityBadgePill(priority = summary.goal.priority)
                    if (goalTags.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "+ Add tags for Auto-Schedule",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        goalTags.forEach { tag ->
                            GoalTagPill(tag = tag)
                        }
                    }
                }

                // Segmented Switcher: Outline | Mind Map
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_workspace_mode_switcher")
                ) {
                    SegmentedButton(
                        selected = state.workspaceMode == GoalWorkspaceMode.OUTLINE,
                        onClick = { onSelectWorkspaceMode(GoalWorkspaceMode.OUTLINE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("goal_mode_outline")
                    ) {
                        Text("Outline", fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = state.workspaceMode == GoalWorkspaceMode.MINDMAP,
                        onClick = { onSelectWorkspaceMode(GoalWorkspaceMode.MINDMAP) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("goal_mode_mindmap")
                    ) {
                        Text("Mind Map", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (state.workspaceMode == GoalWorkspaceMode.MINDMAP) {
            Box(modifier = Modifier.weight(1f)) {
                MindmapCanvasScreen(
                    viewModel = viewModel,
                    onNavigateBack = { onSelectWorkspaceMode(GoalWorkspaceMode.OUTLINE) },
                    onTaskClick = onStartFocus
                )
            }
        } else {
            GoalOutlineModeContent(
                goal = summary.goal,
                allGoalTasks = state.allRawTasks.filter { it.goalId == summary.goal.id },
                onQuickAddStep = onQuickAddStep,
                onToggleTaskCompletion = onToggleTaskCompletion,
                onCycleTaskPriority = onCycleTaskPriority,
                onEditTask = onEditTask,
                onIndentTask = onIndentTask,
                onOutdentTask = onOutdentTask,
                onDeleteTask = onDeleteTask,
                onPushToToday = onPushToToday,
                onAutoScheduleGoal = onAutoScheduleGoal,
                onRequestSmartBreakdown = onRequestSmartBreakdown,
                onStartFocus = onStartFocus,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GoalOutlineModeContent(
    goal: GoalEntity,
    allGoalTasks: List<TaskEntity>,
    onQuickAddStep: (String, String?, Priority) -> Unit,
    onToggleTaskCompletion: (String) -> Unit,
    onCycleTaskPriority: (String) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onIndentTask: (String, String) -> Unit,
    onOutdentTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onPushToToday: (String) -> Unit,
    onAutoScheduleGoal: () -> Unit,
    onRequestSmartBreakdown: () -> Unit,
    onStartFocus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draftStepText by remember { mutableStateOf("") }
    var targetParentTaskId by remember(goal.id) { mutableStateOf<String?>(null) }
    var draftPriority by remember(goal.id, goal.priority) { mutableStateOf(goal.priority) }

    val outlineRows = remember(allGoalTasks) {
        buildOutlineHierarchy(allGoalTasks)
    }

    val unscheduledActiveCount = remember(allGoalTasks) {
        val active = allGoalTasks.filter { it.status != TaskStatus.COMPLETED }
        val parentIds = active.mapNotNull { it.parentTaskId }.toSet()
        val leaves = active.filter { it.id !in parentIds }.ifEmpty { active }
        leaves.count { it.scheduledDate == null }
    }

    val goalTags = remember(goal.tags) { parseTagsList(goal.tags) }
    val autoScheduleHint = remember(goal.priority, goal.tags) {
        buildGoalAutoScheduleHint(priority = goal.priority, tagsCsv = goal.tags)
    }

    val targetParentTitle = remember(targetParentTaskId, allGoalTasks) {
        allGoalTasks.firstOrNull { it.id == targetParentTaskId }?.title
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("goal_outline_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Auto-Schedule Goal Banner (explains circadian routing + 1-tap schedule)
            if (outlineRows.isNotEmpty()) {
                item(key = "goal_auto_schedule_banner") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (unscheduledActiveCount > 0) {
                                        "$unscheduledActiveCount unscheduled step${if (unscheduledActiveCount == 1) "" else "s"} ready"
                                    } else {
                                        "All active steps scheduled"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                                Text(
                                    text = autoScheduleHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                            FilledTonalButton(
                                onClick = onAutoScheduleGoal,
                                enabled = unscheduledActiveCount > 0,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("goal_auto_schedule_btn")
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Auto-Schedule", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (outlineRows.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurface,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Blank Sheet Ready",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                            Text(
                                text = "Brain-dump steps below with a priority — tap any step later to edit tags, duration, energy, or schedule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary
                            )
                            Button(
                                onClick = onRequestSmartBreakdown,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Smart Break Down", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(outlineRows, key = { it.task.id }) { row ->
                    val isDone = row.task.status == TaskStatus.COMPLETED
                    val indentDp = (row.depth.coerceIn(0, 3) * 18).dp
                    val taskTags = remember(row.task.tags) { parseTagsList(row.task.tags).take(2) }
                    val prioColor = priorityAccentColor(row.task.priority)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (row.depth == 0) NeuroFlowThemeV2Colors.SlateSurface else NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(
                            width = 0.75.dp,
                            color = if (targetParentTaskId == row.task.id) {
                                NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            } else {
                                NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.55f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indentDp)
                            .testTag("outline_step_card_${row.task.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            // Top Row: Completion Toggle + Tappable Title (opens Full Editor) + 1-Tap Priority Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleTaskCompletion(row.task.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                        contentDescription = "Toggle step completion",
                                        tint = if (isDone) NeuroFlowPaletteV2.OptimalEmerald else NeuroFlowThemeV2Colors.TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Text(
                                    text = row.task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (row.depth == 0) FontWeight.Bold else FontWeight.Medium,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else null
                                    ),
                                    color = if (isDone) NeuroFlowThemeV2Colors.TextTertiary else NeuroFlowThemeV2Colors.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onEditTask(row.task) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .testTag("outline_edit_task_${row.task.id}")
                                )

                                // 1-Tap Priority Cycler (LOW -> MEDIUM -> HIGH -> LOW)
                                Surface(
                                    onClick = { onCycleTaskPriority(row.task.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = prioColor.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, prioColor.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 36.dp)
                                        .testTag("outline_priority_${row.task.id}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = priorityShortLabel(row.task.priority),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = prioColor,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Bottom Strip: Tags / Duration / Scheduled status + Compact Actions
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 4.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onEditTask(row.task) }
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (row.task.scheduledDate != null) {
                                        Text(
                                            text = "Scheduled",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NeuroFlowPaletteV2.OptimalEmerald,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (row.task.estimatedDurationMinutes > 0) {
                                        Text(
                                            text = "${row.task.estimatedDurationMinutes}m",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = NeuroFlowThemeV2Colors.TextTertiary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    taskTags.forEach { tag ->
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (row.task.scheduledDate == null && taskTags.isEmpty()) {
                                        Text(
                                            text = "Tap to edit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.TextTertiary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isDone) {
                                        IconButton(
                                            onClick = { onStartFocus(row.task.id) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Start focus on ${row.task.title}",
                                                tint = NeuroFlowPaletteV2.OptimalEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Add child under this step
                                    IconButton(
                                        onClick = {
                                            targetParentTaskId = if (targetParentTaskId == row.task.id) null else row.task.id
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add sub-step under ${row.task.title}",
                                            tint = if (targetParentTaskId == row.task.id) {
                                                NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                            } else {
                                                NeuroFlowThemeV2Colors.TextTertiary
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Indent / Outdent
                                    if (row.depth > 0) {
                                        IconButton(
                                            onClick = { onOutdentTask(row.task.id) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                                                contentDescription = "Outdent step",
                                                tint = NeuroFlowThemeV2Colors.TextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else if (row.previousSiblingId != null) {
                                        IconButton(
                                            onClick = { onIndentTask(row.task.id, row.previousSiblingId) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                                                contentDescription = "Indent under previous step",
                                                tint = NeuroFlowThemeV2Colors.TextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // One-tap "→ Today" or Delete
                                    if (!isDone) {
                                        IconButton(
                                            onClick = { onPushToToday(row.task.id) },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("outline_push_today_${row.task.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Today,
                                                contentDescription = "Schedule step for Today",
                                                tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { onDeleteTask(row.task.id) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete step",
                                                tint = NeuroFlowThemeV2Colors.TextTertiary,
                                                modifier = Modifier.size(18.dp)
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

        // Sticky Paper-Fast Rapid-Entry Bar with inline Priority Picker & Goal Tag Inheritance Hint
        Surface(
            color = NeuroFlowThemeV2Colors.SlateSurface,
            border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (targetParentTitle != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Adding sub-step under: $targetParentTitle",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { targetParentTaskId = null },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Top level", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Inline Priority Quick-Picker + Inherited Goal Tags Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(Priority.HIGH, Priority.MEDIUM, Priority.LOW).forEach { prio ->
                        val selected = draftPriority == prio
                        val color = priorityAccentColor(prio)
                        Surface(
                            onClick = { draftPriority = prio },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) color.copy(alpha = 0.22f) else NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(
                                1.dp,
                                if (selected) color else NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .testTag("outline_draft_priority_${prio.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = priorityShortLabel(prio),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selected) color else NeuroFlowThemeV2Colors.TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    if (goalTags.isNotEmpty()) {
                        Text(
                            text = "Inherits ${goalTags.take(2).joinToString(" ") { "#$it" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draftStepText,
                        onValueChange = { draftStepText = it },
                        placeholder = {
                            Text(
                                text = if (targetParentTitle != null) "Type sub-step & tap Done..." else "Type step & tap Done...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeuroFlowThemeV2Colors.TextTertiary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (draftStepText.isNotBlank()) {
                                    onQuickAddStep(draftStepText, targetParentTaskId, draftPriority)
                                    draftStepText = ""
                                }
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("outline_rapid_entry_input")
                    )
                    FilledIconButton(
                        onClick = {
                            if (draftStepText.isNotBlank()) {
                                onQuickAddStep(draftStepText, targetParentTaskId, draftPriority)
                                draftStepText = ""
                            }
                        },
                        enabled = draftStepText.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("outline_rapid_entry_add")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add step")
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityBadgePill(priority: Priority) {
    val color = priorityAccentColor(priority)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(0.75.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = priorityShortLabel(priority),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun GoalTagPill(tag: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.14f),
        border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.35f))
    ) {
        Text(
            text = "#$tag",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun priorityAccentColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> NeuroFlowThemeV2Colors.RoseCoral
    Priority.MEDIUM -> NeuroFlowThemeV2Colors.WarmAmber
    Priority.LOW -> NeuroFlowPaletteV2.ReboundCyan
}

private fun priorityShortLabel(priority: Priority): String = when (priority) {
    Priority.HIGH -> "HIGH"
    Priority.MEDIUM -> "MED"
    Priority.LOW -> "LOW"
}

private fun parseTagsList(tagsCsv: String): List<String> =
    tagsCsv.split(",")
        .map { it.trim().trimStart('#', '@') }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }

private fun outlinePriorityRank(priority: Priority): Int = when (priority) {
    Priority.HIGH -> 0
    Priority.MEDIUM -> 1
    Priority.LOW -> 2
}

private fun buildOutlineHierarchy(tasks: List<TaskEntity>): List<OutlineRowItem> {
    if (tasks.isEmpty()) return emptyList()
    val byId = tasks.associateBy { it.id }
    val childrenByParent = tasks.groupBy { task ->
        val p = task.parentTaskId
        if (p != null && byId.containsKey(p)) p else null
    }

    val result = mutableListOf<OutlineRowItem>()
    val visited = mutableSetOf<String>()

    fun traverse(parentId: String?, depth: Int) {
        val siblings = childrenByParent[parentId].orEmpty()
            .sortedWith(
                compareBy<TaskEntity> { it.status == TaskStatus.COMPLETED }
                    .thenBy { outlinePriorityRank(it.priority) }
                    .thenBy { it.createdAt }
            )
        var prevId: String? = null
        siblings.forEach { task ->
            if (visited.add(task.id)) {
                result.add(OutlineRowItem(task = task, depth = depth, previousSiblingId = prevId))
                traverse(task.id, depth + 1)
                prevId = task.id
            }
        }
    }

    traverse(null, 0)
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartBreakdownPreviewSheet(
    milestones: List<GoalBreakdownMilestone>,
    onApply: (List<GoalBreakdownMilestone>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndices by remember(milestones) {
        mutableStateOf(milestones.indices.toSet())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeuroFlowPaletteV2.SurfaceCard,
        contentColor = NeuroFlowPaletteV2.TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("smart_breakdown_preview_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Smart Goal Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeuroFlowPaletteV2.TextPrimary
                )
            }
            Text(
                text = "Choose which milestones and action steps to add to your Outline & Mind Map:",
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowPaletteV2.TextSecondary
            )

            milestones.forEachIndexed { index, milestone ->
                val isChecked = index in selectedIndices
                Surface(
                    onClick = {
                        selectedIndices = if (isChecked) selectedIndices - index else selectedIndices + index
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (isChecked) NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.6f)
                        else NeuroFlowPaletteV2.BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedIndices = if (checked) selectedIndices + index else selectedIndices - index
                            }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = milestone.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowPaletteV2.TextPrimary
                            )
                            milestone.subtasks.forEach { sub ->
                                Text(
                                    text = "• $sub",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowPaletteV2.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val chosen = milestones.filterIndexed { i, _ -> i in selectedIndices }
                        onApply(chosen)
                    },
                    enabled = selectedIndices.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 48.dp)
                        .testTag("apply_smart_breakdown_button")
                ) {
                    Text("Add Selected", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
