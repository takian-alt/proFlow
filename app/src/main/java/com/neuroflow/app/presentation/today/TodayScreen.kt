package com.neuroflow.app.presentation.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.DistractionEngine
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.presentation.common.NewTaskSheet
import com.neuroflow.app.presentation.common.QuickTaskModalBottomSheet
import com.neuroflow.app.presentation.common.ProtectedSlotQueueRow
import com.neuroflow.app.presentation.common.ProtectedSlotTakeoverCard
import com.neuroflow.app.presentation.common.formatFullDate
import com.neuroflow.app.presentation.common.getQuadrantLabel
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenMoreMenu: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToFocus: (String) -> Unit,
    onNavigateToSchedule: (Long) -> Unit = {},
    initialNewTaskOpen: Boolean = false,
    onInitialNewTaskConsumed: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val distractionState by viewModel.dailyDistraction.collectAsStateWithLifecycle()
    val appContext = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var brainDumpText by remember { mutableStateOf("") }
    var showNewTaskSheet by remember { mutableStateOf(false) }
    var showWeeklyIntentDialog by remember { mutableStateOf(false) }
    var showFrogPickerSheet by remember { mutableStateOf(false) }

    // Focus minutes change only when a block closes, and the flagged count only when Settings is
    // edited, so together these re-read the day's usage exactly when the card's inputs move.
    LaunchedEffect(uiState.focusMinutesToday, uiState.distractingAppCount) {
        viewModel.refreshDailyDistraction()
    }
    // Returning from the system usage-access page does not recompose this screen, and the grant has
    // to be picked up there or the card stays stuck on its permission state until the next session.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDailyDistraction()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(initialNewTaskOpen) {
        if (initialNewTaskOpen) {
            showNewTaskSheet = true
            onInitialNewTaskConsumed()
        }
    }

    // React to pending undo actions with snackbar
    LaunchedEffect(uiState.pendingUndoAction) {
        uiState.pendingUndoAction?.let { undoAction ->
            val message = if (undoAction.actionType == UndoActionType.COMPLETE) {
                "\"${undoAction.task.title}\" completed"
            } else {
                "\"${undoAction.task.title}\" pushed to tomorrow"
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val dateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateFormat.format(Date()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMoreMenu) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewTaskSheet = true },
                containerColor = NeuroFlowColors.Purple,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weekly Intent Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWeeklyIntentDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (uiState.weeklyIntent.isNotBlank()) {
                                "Focus this week: ${uiState.weeklyIntent}"
                            } else {
                                "Set this week's primary focus or intention..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit Intent",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Quick Brain Dump Bar
            item {
                OutlinedTextField(
                    value = brainDumpText,
                    onValueChange = { brainDumpText = it },
                    placeholder = { Text("Quick thought or task... (press Enter)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (brainDumpText.isNotBlank()) {
                                viewModel.quickBrainDump(brainDumpText)
                                brainDumpText = ""
                                focusManager.clearFocus()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    ),
                    trailingIcon = {
                        if (brainDumpText.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.quickBrainDump(brainDumpText)
                                brainDumpText = ""
                                focusManager.clearFocus()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }) {
                                Icon(Icons.Filled.AddCircle, contentDescription = "Quick Add", tint = NeuroFlowColors.Purple)
                            }
                        }
                    }
                )
            }

            // Hero Recommendation Card
            item {
                val dueSlot = uiState.dueProtectedSlot
                if (dueSlot != null) {
                    ProtectedSlotTakeoverCard(
                        due = dueSlot,
                        onOpenTimeline = { onNavigateToSchedule(uiState.todayStartMillis) }
                    )
                } else {
                    val heroTask = uiState.recommendedTask
                    if (heroTask != null) {
                        HeroTaskCard(
                            task = heroTask,
                            reason = uiState.recommendationReason,
                            onStartFocus = { onNavigateToFocus(heroTask.id) },
                            onComplete = { viewModel.completeTaskWithUndo(heroTask) },
                            onPostpone = { viewModel.rescheduleToTomorrow(heroTask) }
                        )
                    } else {
                        EmptyHeroCard(onAddTask = { showNewTaskSheet = true })
                    }
                }
            }

            // Deep work ledger + distraction read, one card: both answer "should I go again?"
            item {
                DailyLoadCard(
                    doneMinutes = uiState.focusMinutesToday,
                    targetMinutes = uiState.deepWorkTargetMinutes,
                    sessionCount = uiState.deepWorkSessionCount,
                    distraction = distractionState,
                    onGrantPermission = { DistractionEngine.openUsagePermissionSettings(appContext) }
                )
            }

            // Evening Wrap-Up Card
            if (uiState.showEveningWrapUp) {
                item {
                    EveningWrapUpCard(
                        completedCount = uiState.completedTodayCount,
                        focusMinutes = uiState.focusMinutesToday,
                        onPushRemaining = { viewModel.pushRemainingToTomorrow() },
                        onPickFrog = { showFrogPickerSheet = true }
                    )
                }
            }

            // Overdue Tasks Section (ADHD-supportive low-anxiety presentation)
            if (uiState.overdueTasks.isNotEmpty()) {
                item {
                    SectionHeader("Ready When You Are", badge = "${uiState.overdueTasks.size} ready", badgeColor = NeuroFlowColors.MapeBad)
                }
                items(uiState.overdueTasks, key = { it.id }) { task ->
                    SwipeableTaskCard(
                        task = task,
                        onStartFocus = { onNavigateToFocus(task.id) },
                        onComplete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.completeTaskWithUndo(task)
                        },
                        onPostpone = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.postponeTaskWithUndo(task)
                        },
                        onMoveToTomorrow = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.rescheduleToTomorrow(task)
                        }
                    )
                }
            }

            // Up Next Tasks Section
            if (uiState.upNextTasks.isNotEmpty() || uiState.approachingProtectedSlots.isNotEmpty()) {
                item {
                    SectionHeader("Up Next", badge = "${uiState.upNextTasks.size}")
                }
                items(
                    uiState.approachingProtectedSlots,
                    key = { "slot-${it.slot.id}-${it.startMillis}" }
                ) { due ->
                    ProtectedSlotQueueRow(
                        due = due,
                        onOpenTimeline = { onNavigateToSchedule(uiState.todayStartMillis) }
                    )
                }
                items(uiState.upNextTasks, key = { it.id }) { task ->
                    SwipeableTaskCard(
                        task = task,
                        onStartFocus = { onNavigateToFocus(task.id) },
                        onComplete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.completeTaskWithUndo(task)
                        },
                        onPostpone = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.postponeTaskWithUndo(task)
                        },
                        onMoveToTomorrow = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.rescheduleToTomorrow(task)
                        }
                    )
                }
            }

            // Later Today Tasks Section
            if (uiState.laterTodayTasks.isNotEmpty()) {
                item {
                    SectionHeader("Later Today", badge = "${uiState.laterTodayTasks.size}")
                }
                items(uiState.laterTodayTasks, key = { it.id }) { task ->
                    SwipeableTaskCard(
                        task = task,
                        onStartFocus = { onNavigateToFocus(task.id) },
                        onComplete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.completeTaskWithUndo(task)
                        },
                        onPostpone = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.postponeTaskWithUndo(task)
                        },
                        onMoveToTomorrow = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.rescheduleToTomorrow(task)
                        }
                    )
                }
            }

            // Unscheduled Backlog Peek Pill
            if (uiState.unscheduledCount > 0) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToPlan),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📋", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${uiState.unscheduledCount} unscheduled backlog tasks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                "View Plan →",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Weekly Intent Edit Dialog
    if (showWeeklyIntentDialog) {
        var editedIntent by remember { mutableStateOf(uiState.weeklyIntent) }
        AlertDialog(
            onDismissRequest = { showWeeklyIntentDialog = false },
            title = { Text("Weekly Focus Intention") },
            text = {
                OutlinedTextField(
                    value = editedIntent,
                    onValueChange = { editedIntent = it },
                    placeholder = { Text("What is your top goal this week?") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateWeeklyIntent(editedIntent.trim())
                    showWeeklyIntentDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showWeeklyIntentDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Pick Frog Task Bottom Sheet
    if (showFrogPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFrogPickerSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text("🐸 Pick Tomorrow's Frog", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Choose the single most impactful task you will tackle first thing in the morning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                val candidates = uiState.allRemainingTodayTasks
                if (candidates.isEmpty()) {
                    Text("No tasks left today. Add tasks to set a frog.")
                } else {
                    candidates.forEach { task ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setFrogTask(task.id)
                                    showFrogPickerSheet = false
                                }
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(task.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                if (task.isFrog) {
                                    Text("🐸 Current Frog", color = NeuroFlowColors.ScheduleText, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Streamlined Quick Task Capture Sheet
    if (showNewTaskSheet) {
        QuickTaskModalBottomSheet(
            onDismiss = { showNewTaskSheet = false },
            onSave = { task ->
                showNewTaskSheet = false
                viewModel.addTask(task)
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, badge: String? = null, badgeColor: Color = MaterialTheme.colorScheme.primary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (badge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HeroTaskCard(
    task: TaskEntity,
    reason: String,
    onStartFocus: () -> Unit,
    onComplete: () -> Unit,
    onPostpone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeuroFlowColors.Purple.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = reason,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowColors.Purple,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = getQuadrantLabel(task.quadrant),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Start Focus CTA
            Button(
                onClick = onStartFocus,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Focus", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (task.estimatedDurationMinutes > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("(${task.estimatedDurationMinutes}m)", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done")
                }
                OutlinedButton(
                    onClick = onPostpone,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tomorrow")
                }
            }
        }
    }
}

@Composable
private fun EmptyHeroCard(onAddTask: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("All Clear for Today!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "You have no pending tasks scheduled for today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAddTask,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple)
            ) {
                Text("+ Add a Task")
            }
        }
    }
}

private val SlateCard = Color(0xFF1E293B)
private val SlateMuted = Color(0xFF94A3B8)
private val SlateTrack = Color(0xFF334155)

private fun distractionColor(score: Float) = when {
    score >= 75f -> Color(0xFFEF4444)
    score >= 50f -> Color(0xFFF97316)
    score >= 25f -> Color(0xFFFACC15)
    else -> Color(0xFF22C55E)
}

@Composable
private fun DailyLoadCard(
    doneMinutes: Float,
    targetMinutes: Int,
    sessionCount: Int,
    distraction: DailyDistractionState?,
    onGrantPermission: () -> Unit
) {
    val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val progress = deepWorkProgress(doneMinutes, targetMinutes)
    val remaining = (targetMinutes - doneMinutes).coerceAtLeast(0f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧠", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Deep Work Today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "$sessionCount block${if (sessionCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SlateMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "${formatDurationMinutes(doneMinutes)}  /  ${formatDurationMinutes(targetMinutes.toFloat())}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress >= 1f) Color(0xFF22C55E) else NeuroFlowColors.Purple,
                trackColor = SlateTrack
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                deepWorkVerdict(doneMinutes, targetMinutes, hourOfDay),
                style = MaterialTheme.typography.bodyMedium,
                color = SlateMuted
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = SlateTrack
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📵", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Distraction",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (distraction != null && distraction.permissionGranted && distraction.hasEnoughSignal) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${distraction.distractionScore.toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = distractionColor(distraction.distractionScore)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                distraction == null -> Text(
                    "Reading today's screen time…",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateMuted
                )

                !distraction.permissionGranted -> {
                    Text(
                        "Screen time needs usage access. Grant it and this shows where today actually went.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onGrantPermission,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("Grant usage access", fontSize = 12.sp)
                    }
                }

                else -> {
                    Text(
                        distractionVerdict(distraction),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // "App time" rather than "screen time": this app and the system UI are not
                        // in the total, so it will always read below the OS dashboard.
                        LoadStat("App time", distraction.screenMinutes, Color.White, Modifier.weight(1f))
                        LoadStat("Flagged apps", distraction.distractingMinutes,
                            distractionColor(distraction.distractionScore), Modifier.weight(1f))
                        LoadStat("Deep work", distraction.focusMinutes, Color(0xFF22C55E), Modifier.weight(1f))
                    }
                    if (distraction.overlapMinutes >= 1f) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${formatDurationMinutes(distraction.overlapMinutes)} of the flagged time landed " +
                                "inside a focus block, so it is subtracted from deep work rather than counted twice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted
                        )
                    }
                    if (distraction.topApps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Biggest leaks: " + distraction.topApps
                                .joinToString(" · ") { "${it.first} ${it.second.toInt()}m" },
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        distractionAdvice(distraction, remaining),
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadStat(
    label: String,
    minutes: Float,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            formatDurationMinutes(minutes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = SlateMuted)
    }
}

@Composable
private fun EveningWrapUpCard(
    completedCount: Int,
    focusMinutes: Float,
    onPushRemaining: () -> Unit,
    onPickFrog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌙", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Evening Wrap-Up", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "$completedCount tasks completed · ${focusMinutes.toInt()} focus minutes logged today.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPushRemaining,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Push Remaining →", fontSize = 12.sp, color = Color.White)
                }
                Button(
                    onClick = onPickFrog,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🐸 Set Frog", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskCard(
    task: TaskEntity,
    onStartFocus: () -> Unit,
    onComplete: () -> Unit,
    onPostpone: () -> Unit,
    onMoveToTomorrow: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        // Material's default is half the card width, which a scroll-with-thumb-drift easily trips.
        // Completing and postponing are destructive enough to require a deliberate full swipe.
        positionalThreshold = { totalDistance -> totalDistance * 0.9f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onComplete()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onPostpone()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32) // Complete (green)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFE65100) // Postpone (orange)
                    else -> Color.Transparent
                },
                label = "swipeBg"
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, contentDescription = "Complete", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Complete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tomorrow", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.Schedule, contentDescription = "Postpone", tint = Color.White)
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Focus Button
                IconButton(
                    onClick = onStartFocus,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeuroFlowColors.Purple.copy(alpha = 0.12f))
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Start Focus",
                        tint = NeuroFlowColors.Purple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.isFrog) {
                            Text("🐸 ", fontSize = 14.sp)
                        }
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getQuadrantLabel(task.quadrant),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (task.estimatedDurationMinutes > 0) {
                            Text("•", color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "${task.estimatedDurationMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (task.priority == Priority.HIGH) {
                            Text("•", color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "High",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowColors.DoFirstText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onMoveToTomorrow,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE65100).copy(alpha = 0.12f))
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = "Move to tomorrow",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
