package com.neuroflow.app.presentation.launcher.components

/**
 * FocusTaskCard - Premium task card for the launcher home screen.
 *
 * Usage example:
 * ```kotlin
 * @Composable
 * fun HomeScreen(viewModel: LauncherViewModel = hiltViewModel()) {
 *     val topTask by viewModel.topTask.collectAsStateWithLifecycle()
 *     val ulyssesContract by viewModel.topTaskUlyssesContract.collectAsStateWithLifecycle()
 *     val woopEntity by viewModel.topTaskWoopEntity.collectAsStateWithLifecycle()
 *     val focusActive by viewModel.focusActive.collectAsStateWithLifecycle()
 *     val context = LocalContext.current
 *
 *     Column(modifier = Modifier.fillMaxSize()) {
 *         FocusTaskCard(
 *             topTask = topTask,
 *             ulyssesContract = ulyssesContract,
 *             woopEntity = woopEntity,
 *             focusActive = focusActive,
 *             onSkip = { taskId -> viewModel.skipTask(taskId) },
 *             onStartFocus = { taskId ->
 *                 val intent = Intent(context, MainActivity::class.java).apply {
 *                     action = "com.procus.ACTION_OPEN_FOCUS"
 *                     putExtra("task_id", taskId)
 *                     flags = Intent.FLAG_ACTIVITY_NEW_TASK
 *                 }
 *                 context.startActivity(intent)
 *             }
 *         )
 *         // Other home screen components...
 *     }
 * }
 * ```
 */

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.UlyssesContractEntity
import com.neuroflow.app.data.local.entity.WoopEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.presentation.launcher.theme.LocalLauncherTheme
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Focus Task Card - displays the highest-priority task on the launcher home screen.
 *
 * Displays:
 * - Task title, Eisenhower quadrant badge, deadline (relative + absolute), estimated duration, energy level
 * - Frog badge when isFrog = true
 * - Contract badge when UlyssesContractEntity exists
 * - WOOP obstacle and plan when WoopEntity has non-empty obstacle
 * - Raw TaskScoringEngine score when showTaskScore = true
 * - "In progress" state with elapsed timer when focusActive = true (Phase 1: always false)
 *
 * Actions:
 * - "Start Focus" button: sends Intent to MainActivity with action com.procus.ACTION_OPEN_FOCUS
 * - "Skip" button: calls viewModel.skipTask(taskId) with horizontal slide animation
 * - Empty state: shows message with button to open NewTaskSheet
 *
 * @param topTask The highest-scored active task (null if none)
 * @param ulyssesContract Ulysses contract for the task (null if none)
 * @param woopEntity WOOP entity for the task (null if none)
 * @param focusActive Whether a focus session is currently active
 * @param onSkip Callback when skip button is tapped
 * @param onStartFocus Callback when start focus button is tapped
 * @param modifier Modifier for the card container
 */
@Composable
fun FocusTaskCard(
    topTask: TaskEntity?,
    ulyssesContract: UlyssesContractEntity?,
    woopEntity: WoopEntity?,
    focusActive: Boolean,
    focusElapsedSeconds: Int = 0,
    hasActiveTasks: Boolean = true,
    onSkip: (String) -> Unit,
    onStartFocus: (String) -> Unit,
    onClearSkipped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = LocalLauncherTheme.current
    val context = LocalContext.current

    // Track current task ID for animation
    var currentTaskId by remember { mutableStateOf(topTask?.id) }
    var visible by remember { mutableStateOf(true) }

    // Update visibility when task changes
    LaunchedEffect(topTask?.id) {
        if (topTask?.id != currentTaskId) {
            visible = false
            kotlinx.coroutines.delay(300) // Wait for slide-out animation
            currentTaskId = topTask?.id
            visible = true
        }
    }

    // Get user preferences for score calculation
    val prefs = remember {
        // This is a simplified approach - ideally we'd inject this via ViewModel
        // For now, we'll pass null and handle it in TaskScoreBadge
        null as com.neuroflow.app.data.local.UserPreferences?
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("focus_task_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = theme.cardAlpha)
        ),
        elevation = when (theme.taskCardStyle) {
            com.neuroflow.app.presentation.launcher.domain.CardStyle.ELEVATED -> CardDefaults.cardElevation(defaultElevation = 4.dp)
            com.neuroflow.app.presentation.launcher.domain.CardStyle.FLAT -> CardDefaults.cardElevation(defaultElevation = 0.dp)
            com.neuroflow.app.presentation.launcher.domain.CardStyle.OUTLINED -> CardDefaults.cardElevation(defaultElevation = 0.dp)
        }
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            )
        ) {
            if (topTask == null) {
                // Empty state - check if it's because all tasks are skipped
                EmptyTaskState(
                    hasActiveTasks = hasActiveTasks,
                    onClearSkipped = onClearSkipped
                )
            } else {
                // Task content
                TaskContent(
                    task = topTask,
                    ulyssesContract = ulyssesContract,
                    woopEntity = woopEntity,
                    focusActive = focusActive,
                    focusElapsedSeconds = focusElapsedSeconds,
                    showTaskScore = theme.showTaskScore,
                    prefs = prefs,
                    onSkip = { onSkip(topTask.id) },
                    onStartFocus = { onStartFocus(topTask.id) }
                )
            }
        }
    }
}

/**
 * Empty state when no active tasks exist.
 * Shows message with button to open NewTaskSheet or unskip all if tasks are just skipped.
 */
@Composable
private fun EmptyTaskState(
    hasActiveTasks: Boolean = false,
    onClearSkipped: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        if (hasActiveTasks) {
            // All tasks are skipped
            Text(
                text = "All tasks skipped",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "You've skipped all your tasks. Show them again or create a new one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onClearSkipped
            ) {
                Text("Show All Tasks")
            }
            OutlinedButton(
                onClick = {
                    // Send Intent to MainActivity to open NewTaskSheet
                    val intent = Intent(context, com.neuroflow.app.MainActivity::class.java).apply {
                        action = "com.procus.ACTION_OPEN_NEW_TASK"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Task")
            }
        } else {
            // No active tasks at all
            Text(
                text = "No active tasks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Create your first task to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    // Send Intent to MainActivity to open NewTaskSheet
                    val intent = Intent(context, com.neuroflow.app.MainActivity::class.java).apply {
                        action = "com.procus.ACTION_OPEN_NEW_TASK"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Task")
            }
        }
    }
}

/**
 * Task content displaying all task information and actions.
 */
@Composable
private fun TaskContent(
    task: TaskEntity,
    ulyssesContract: UlyssesContractEntity?,
    woopEntity: WoopEntity?,
    focusActive: Boolean,
    focusElapsedSeconds: Int,
    showTaskScore: Boolean,
    prefs: com.neuroflow.app.data.local.UserPreferences?,
    onSkip: () -> Unit,
    onStartFocus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row: quadrant badge, frog badge, contract badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quadrant badge
            QuadrantBadge(quadrant = task.quadrant)

            // Frog badge
            if (task.isFrog) {
                FrogBadge()
            }

            // Contract badge
            if (ulyssesContract != null) {
                ContractBadge()
            }

            Spacer(Modifier.weight(1f))

            // Task score (if enabled)
            if (showTaskScore) {
                TaskScoreBadge(task = task, prefs = prefs)
            }
        }

        // Task title
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        // WOOP obstacle and plan (if present)
        if (woopEntity != null && woopEntity.obstacle.isNotBlank()) {
            WoopReminder(woopEntity = woopEntity)
        }

        // Task metadata row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Deadline
            if (task.deadlineDate != null) {
                DeadlineInfo(deadlineDate = task.deadlineDate, deadlineTime = task.deadlineTime)
            }

            // Estimated duration
            if (task.estimatedDurationMinutes > 0) {
                DurationInfo(durationMinutes = task.estimatedDurationMinutes)
            }

            // Energy level
            EnergyLevelIndicator(energyLevel = task.energyLevel)
        }

        // Action buttons
        if (focusActive) {
            // In progress state with elapsed timer from real session
            InProgressState(elapsedSeconds = focusElapsedSeconds, onOpenFocus = onStartFocus)
        } else {
            // Start Focus and Skip buttons
            ActionButtons(
                onStartFocus = onStartFocus,
                onSkip = onSkip
            )
        }
    }
}

/**
 * Quadrant badge showing Eisenhower matrix quadrant.
 */
@Composable
private fun QuadrantBadge(quadrant: Quadrant) {
    val (label, color) = when (quadrant) {
        Quadrant.DO_FIRST -> "Do First" to MaterialTheme.colorScheme.error
        Quadrant.SCHEDULE -> "Schedule" to MaterialTheme.colorScheme.primary
        Quadrant.DELEGATE -> "Delegate" to MaterialTheme.colorScheme.tertiary
        Quadrant.ELIMINATE -> "Eliminate" to MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Frog badge indicating "eat the frog" task.
 */
@Composable
private fun FrogBadge() {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "🐸 Frog",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Contract badge indicating Ulysses contract exists.
 */
@Composable
private fun ContractBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "📜 Contract",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Task score badge showing raw TaskScoringEngine score.
 */
@Composable
private fun TaskScoreBadge(
    task: TaskEntity,
    prefs: com.neuroflow.app.data.local.UserPreferences?
) {
    // Calculate actual score using TaskScoringEngine
    val score = remember(task, prefs) {
        if (prefs != null) {
            com.neuroflow.app.domain.engine.TaskScoringEngine.score(
                task = task,
                prefs = prefs,
                allActiveTasks = emptyList(), // Simplified - not passing all tasks
                nowMillis = System.currentTimeMillis()
            ).toInt()
        } else {
            0
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "Score: $score",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * WOOP reminder showing obstacle and plan.
 */
@Composable
private fun WoopReminder(woopEntity: WoopEntity) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Obstacle: ${woopEntity.obstacle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
            if (woopEntity.plan.isNotBlank()) {
                Text(
                    text = "Plan: ${woopEntity.plan}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Deadline information showing relative and absolute time.
 */
@Composable
private fun DeadlineInfo(deadlineDate: Long, deadlineTime: Long?) {
    val now = System.currentTimeMillis()
    val deadline = deadlineTime ?: deadlineDate
    val relativeTime = formatRelativeTime(deadline, now)
    val absoluteTime = formatAbsoluteTime(deadline)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = relativeTime,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = absoluteTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Duration information showing estimated time.
 */
@Composable
private fun DurationInfo(durationMinutes: Int) {
    val durationText = when {
        durationMinutes < 60 -> "${durationMinutes}m"
        durationMinutes % 60 == 0 -> "${durationMinutes / 60}h"
        else -> "${durationMinutes / 60}h ${durationMinutes % 60}m"
    }

    Text(
        text = "⏱️ $durationText",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Energy level indicator.
 */
@Composable
private fun EnergyLevelIndicator(energyLevel: EnergyLevel) {
    val (icon, color) = when (energyLevel) {
        EnergyLevel.HIGH -> "⚡" to MaterialTheme.colorScheme.error
        EnergyLevel.MEDIUM -> "💡" to MaterialTheme.colorScheme.primary
        EnergyLevel.LOW -> "🌙" to MaterialTheme.colorScheme.tertiary
    }

    Text(
        text = icon,
        style = MaterialTheme.typography.bodyMedium,
        color = color
    )
}

/**
 * In progress state showing elapsed timer from the real open session.
 * elapsedSeconds comes from LauncherViewModel.focusElapsedSeconds — survives swipes.
 */
@Composable
private fun InProgressState(elapsedSeconds: Int, onOpenFocus: () -> Unit) {
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeText = if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "In progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onOpenFocus,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Open Focus", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Action buttons for Start Focus and Skip.
 */
@Composable
private fun ActionButtons(
    onStartFocus: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
    ) {
        // Skip button
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Skip")
        }

        // Start Focus button
        Button(
            onClick = onStartFocus,
            modifier = Modifier.weight(1f)
        ) {
            Text("Start Focus")
        }
    }
}

/**
 * Format relative time (e.g., "in 2 hours", "tomorrow", "overdue by 3 days").
 */
private fun formatRelativeTime(deadline: Long, now: Long): String {
    val diff = deadline - now
    val absDiff = kotlin.math.abs(diff)

    return when {
        diff < 0 -> {
            // Overdue
            val days = TimeUnit.MILLISECONDS.toDays(absDiff)
            val hours = TimeUnit.MILLISECONDS.toHours(absDiff)
            when {
                days > 0 -> "Overdue by $days day${if (days > 1) "s" else ""}"
                hours > 0 -> "Overdue by $hours hour${if (hours > 1) "s" else ""}"
                else -> "Overdue"
            }
        }
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            "in $minutes minute${if (minutes > 1) "s" else ""}"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "in $hours hour${if (hours > 1) "s" else ""}"
        }
        diff < TimeUnit.DAYS.toMillis(2) -> "tomorrow"
        else -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "in $days day${if (days > 1) "s" else ""}"
        }
    }
}

/**
 * Format absolute time (e.g., "Mon, Jan 15 at 2:30 PM").
 */
private fun formatAbsoluteTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
