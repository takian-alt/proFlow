package com.neuroflow.app.presentation.v2.mindmap

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Today
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
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.assistant.GoalBreakdownMilestone
import com.neuroflow.app.domain.model.TaskStatus
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
    val snackbarHostState = remember { SnackbarHostState() }

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
                    onQuickCreateGoal = { title -> viewModel.quickCreateGoalAndOpen(title) },
                    onOpenGoal = { goalId -> viewModel.selectGoal(goalId) },
                    onPushNextToToday = { taskId -> viewModel.pushTaskToToday(taskId) },
                    onStartFocus = onStartFocus
                )
            } else {
                SingleGoalWorkspaceContent(
                    summary = activeSummary,
                    state = state,
                    onBackToOverview = { viewModel.clearSelectedGoal() },
                    onSelectWorkspaceMode = { viewModel.setWorkspaceMode(it) },
                    onToggleShowAllGoalsInMap = { viewModel.toggleShowAllGoalsInMap() },
                    onQuickAddStep = { title, parentId ->
                        viewModel.quickAddOutlineStep(activeSummary.goal.id, title, parentId)
                    },
                    onToggleTaskCompletion = { viewModel.onToggleTaskCompletion(it) },
                    onIndentTask = { taskId, newParent -> viewModel.indentOutlineTask(taskId, newParent) },
                    onOutdentTask = { taskId -> viewModel.outdentOutlineTask(taskId) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onPushToToday = { viewModel.pushTaskToToday(it) },
                    onRequestSmartBreakdown = { viewModel.requestSmartBreakdown(activeSummary.goal.id) },
                    onStartFocus = onStartFocus,
                    viewModel = viewModel
                )
            }
        }
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
    onQuickCreateGoal: (String) -> Unit,
    onOpenGoal: (String) -> Unit,
    onPushNextToToday: (String) -> Unit,
    onStartFocus: (String) -> Unit
) {
    var newGoalTitle by remember { mutableStateOf("") }

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
                    text = "Sketch ideas faster than paper, view live mind maps, and send your next step to Today.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )
            }
        }

        // Paper-fast "+ New Goal" capture bar
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        if (summaries.isEmpty()) {
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
                            text = "Start your first goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                        Text(
                            text = "Type a goal above to open a paper-fast outline and instant visual mind map.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }
                }
            }
        } else {
            items(summaries, key = { it.goal.id }) { summary ->
                GoalOverviewCard(
                    summary = summary,
                    onOpen = { onOpenGoal(summary.goal.id) },
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
    onPushNextToToday: () -> Unit,
    onStartNextFocus: () -> Unit
) {
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
                        tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
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
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                ) {
                    Text(
                        text = "${summary.completedTasks}/${summary.totalTasks}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NEXT STEP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeuroFlowPaletteV2.ReboundCyan,
                                fontSize = 10.sp
                            )
                            Text(
                                text = nextTask.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
    onSelectWorkspaceMode: (GoalWorkspaceMode) -> Unit,
    onToggleShowAllGoalsInMap: () -> Unit,
    onQuickAddStep: (String, String?) -> Unit,
    onToggleTaskCompletion: (String) -> Unit,
    onIndentTask: (String, String) -> Unit,
    onOutdentTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onPushToToday: (String) -> Unit,
    onRequestSmartBreakdown: () -> Unit,
    onStartFocus: (String) -> Unit,
    viewModel: MindmapViewModel
) {
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
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
                goalId = summary.goal.id,
                allGoalTasks = state.allRawTasks.filter { it.goalId == summary.goal.id },
                onQuickAddStep = onQuickAddStep,
                onToggleTaskCompletion = onToggleTaskCompletion,
                onIndentTask = onIndentTask,
                onOutdentTask = onOutdentTask,
                onDeleteTask = onDeleteTask,
                onPushToToday = onPushToToday,
                onRequestSmartBreakdown = onRequestSmartBreakdown,
                onStartFocus = onStartFocus,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GoalOutlineModeContent(
    goalId: String,
    allGoalTasks: List<TaskEntity>,
    onQuickAddStep: (String, String?) -> Unit,
    onToggleTaskCompletion: (String) -> Unit,
    onIndentTask: (String, String) -> Unit,
    onOutdentTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onPushToToday: (String) -> Unit,
    onRequestSmartBreakdown: () -> Unit,
    onStartFocus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draftStepText by remember { mutableStateOf("") }
    var targetParentTaskId by remember(goalId) { mutableStateOf<String?>(null) }

    val outlineRows = remember(allGoalTasks) {
        buildOutlineHierarchy(allGoalTasks)
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
                                text = "Brain-dump steps below like paper — press Done to add continuously, or let Smart Assistant sketch milestones for you.",
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
                    val indentDp = (row.depth.coerceIn(0, 3) * 20).dp
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
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
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

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onStartFocus(row.task.id) }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = row.task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (row.depth == 0) FontWeight.Bold else FontWeight.Medium,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else null
                                    ),
                                    color = if (isDone) NeuroFlowThemeV2Colors.TextTertiary else NeuroFlowThemeV2Colors.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (row.task.scheduledDate != null) {
                                    Text(
                                        text = "Scheduled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowPaletteV2.OptimalEmerald,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Add child under this step
                            IconButton(
                                onClick = {
                                    targetParentTaskId = if (targetParentTaskId == row.task.id) null else row.task.id
                                },
                                modifier = Modifier.size(44.dp)
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

                            // One-tap "→ Today"
                            if (!isDone) {
                                IconButton(
                                    onClick = { onPushToToday(row.task.id) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("outline_push_today_${row.task.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = "Schedule step for Today",
                                        tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { onDeleteTask(row.task.id) },
                                    modifier = Modifier.size(44.dp)
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

        // Sticky Paper-Fast Rapid-Entry Bar
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                    onQuickAddStep(draftStepText, targetParentTaskId)
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
                                onQuickAddStep(draftStepText, targetParentTaskId)
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
            .sortedWith(compareBy<TaskEntity> { it.status == TaskStatus.COMPLETED }.thenBy { it.createdAt })
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
