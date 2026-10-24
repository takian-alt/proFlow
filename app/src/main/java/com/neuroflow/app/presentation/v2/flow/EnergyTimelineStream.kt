package com.neuroflow.app.presentation.v2.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.presentation.common.ManualTimeLogSheet
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase

@Composable
fun EnergyTimelineStream(
    scheduledTasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onDeferTask: (TaskEntity) -> Unit = {},
    onBreakDownTask: (TaskEntity) -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {},
    onShiftToPeak: ((TaskEntity) -> Unit)? = null,
    timeline: List<CircadianSnapshot> = emptyList(),
    modifier: Modifier = Modifier,
    viewModel: FlowViewModelV2 = hiltViewModel()
) {
    var taskToLogTime by remember { mutableStateOf<TaskEntity?>(null) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Energy Stream",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeuroFlowThemeV2Colors.TextPrimary
            )
            Text(
                text = "${scheduledTasks.size} blocks",
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowThemeV2Colors.TextSecondary
            )
        }

        if (scheduledTasks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🧘 Quiet Waters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "No tasks scheduled for today. Tap the '+' composer to plan your next hyperfocus session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }
        } else {
            val nowMillis = System.currentTimeMillis()
            val activeOrNextIndex = scheduledTasks.indexOfFirst { task ->
                val start = task.resolveScheduledEpochMillis() ?: task.scheduledDate ?: 0L
                val end = start + task.estimatedDurationMinutes * 60_000L
                end >= nowMillis
            }
            var cumulativeMinutes = 0

            scheduledTasks.forEachIndexed { index, task ->
                val scheduledMillis = task.resolveScheduledEpochMillis() ?: task.scheduledDate
                val taskPhase = if (scheduledMillis != null && timeline.isNotEmpty()) {
                    timeline.minByOrNull { kotlin.math.abs(it.timestampMillis - scheduledMillis) }?.phase ?: EnergyPhase.PEAK
                } else EnergyPhase.PEAK
                val phaseColor = NeuroFlowPaletteV2.accentForPhase(taskPhase)

                if (index == activeOrNextIndex) {
                    // Pulsing "NOW" playhead indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeuroFlowPaletteV2.ReboundCyan, CircleShape)
                        )
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NeuroFlowPaletteV2.ReboundCyan,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeuroFlowPaletteV2.ReboundCyan, Color.Transparent)
                                    )
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Phase-colored vertical time-rail node & track
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 18.dp)
                                .size(10.dp)
                                .background(phaseColor, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f, fill = false)
                                .heightIn(min = 40.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(phaseColor.copy(alpha = 0.5f), phaseColor.copy(alpha = 0.15f))
                                    )
                                )
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        key(task.id) {
                            TimelineTaskCard(
                                task = task,
                                onTaskClick = { onTaskClick(task) },
                                onComplete = { onCompleteTask(task) },
                                onDefer = { onDeferTask(task) },
                                onBreakDown = { onBreakDownTask(task) },
                                onEdit = { onEditTask(task) },
                                onShiftToPeak = onShiftToPeak?.let { shift -> { shift(task) } },
                                onLogPastTime = { taskToLogTime = task },
                                timeline = timeline
                            )
                        }
                    }
                }

                cumulativeMinutes += task.estimatedDurationMinutes
                val hasNext = index < scheduledTasks.size - 1
                if (hasNext) {
                    val nextTask = scheduledTasks[index + 1]
                    val isBetweenSplitParts = (task.id.contains("#part1") || task.id.contains("_part1") || task.title.contains("(Part 1/2)")) &&
                            (nextTask.id.contains("#part2") || nextTask.id.contains("_part2") || nextTask.title.contains("(Part 2/2)"))
                    if (isBetweenSplitParts || cumulativeMinutes >= 90) {
                        UltradianRecoveryDivider()
                        cumulativeMinutes = 0
                    }
                }
            }
        }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTaskCard(
    task: TaskEntity,
    onTaskClick: () -> Unit,
    onComplete: () -> Unit,
    onDefer: () -> Unit,
    onBreakDown: () -> Unit = {},
    onEdit: () -> Unit = {},
    onShiftToPeak: (() -> Unit)? = null,
    onLogPastTime: () -> Unit = {},
    timeline: List<CircadianSnapshot> = emptyList()
) {
    val energyBadgeColor = when (task.energyLevel) {
        EnergyLevel.HIGH -> NeuroFlowThemeV2Colors.WarmAmber
        EnergyLevel.MEDIUM -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
        EnergyLevel.LOW -> NeuroFlowThemeV2Colors.SageGreen
    }

    val microSteps = StepByStepPlanCodec.parse(task.ifThenPlan)
    val hasMicroSteps = microSteps.isNotEmpty()
    var showDetailSheet by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.55f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onComplete()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDefer()
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
                SwipeToDismissBoxValue.EndToStart -> NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.25f)
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.CheckCircle
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.MoreTime
                SwipeToDismissBoxValue.Settled -> null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { showDetailSheet = true }),
            shape = RoundedCornerShape(16.dp),
            color = NeuroFlowThemeV2Colors.SlateSurface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Leading Complete Button (48dp touch target)
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Complete task",
                        tint = NeuroFlowThemeV2Colors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Middle: Frog emoji + Title (up to 2 lines) + Metadata badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (task.isFrog) {
                            Text("🐸", fontSize = 14.sp)
                        }
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${task.estimatedDurationMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )

                        Box(
                            modifier = Modifier
                                .background(energyBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.energyLevel.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = energyBadgeColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (hasMicroSteps) {
                            Text(
                                text = "🪜 ${microSteps.size} steps",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (task.impactScore >= 70) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.WarmAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "High Leverage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.WarmAmber
                                )
                            }
                        }

                        val isUltradianPart = task.id.contains("#part") || task.title.contains("(Part ")
                        if (isUltradianPart) {
                            val partLabel = if (task.id.contains("#part2") || task.title.contains("(Part 2/2)")) "🔗 Part 2/2" else "🔗 Part 1/2"
                            Box(
                                modifier = Modifier
                                    .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = partLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (task.postponeCount >= 3) {
                            Box(
                                modifier = Modifier
                                    .background(NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⚡ Dopamine Warm-Up",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.WarmAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    val scheduledMillis = task.resolveScheduledEpochMillis() ?: task.scheduledDate
                    if (scheduledMillis != null && timeline.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TaskSlotEnergyFitBadge(
                            taskScheduledMillis = scheduledMillis,
                            taskEnergyDemand = task.energyLevel,
                            timeline = timeline,
                            onShiftToPeakClick = onShiftToPeak
                        )
                    }
                }

                // Trailing Quick Focus start button (48dp touch target)
                IconButton(
                    onClick = onTaskClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Focus",
                        tint = NeuroFlowThemeV2Colors.ElectricIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showDetailSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
            dragHandle = { BottomSheetDefaults.DragHandle(color = NeuroFlowThemeV2Colors.TextSecondary.copy(alpha = 0.5f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (task.isFrog) {
                            Text("🐸", fontSize = 20.sp)
                        }
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            showDetailSheet = false
                            onEdit()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit task", tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏱ ${task.estimatedDurationMinutes} mins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                    Text("•", color = NeuroFlowThemeV2Colors.TextSecondary)
                    Text(
                        text = "Energy: ${task.energyLevel.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = energyBadgeColor
                    )
                }

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }

                if (hasMicroSteps) {
                    Text(
                        text = "Micro-steps (${microSteps.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        microSteps.forEachIndexed { index, step ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = step.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showDetailSheet = false
                            onEdit()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Task")
                    }

                    OutlinedButton(
                        onClick = {
                            showDetailSheet = false
                            onBreakDown()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Breakdown")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showDetailSheet = false
                            onDefer()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    ) {
                        Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Defer +2h")
                    }

                    OutlinedButton(
                        onClick = {
                            showDetailSheet = false
                            onLogPastTime()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Log Time")
                    }
                }

                Button(
                    onClick = {
                        showDetailSheet = false
                        onTaskClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start Focus", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

