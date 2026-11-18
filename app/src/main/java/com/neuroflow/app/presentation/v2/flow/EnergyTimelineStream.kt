package com.neuroflow.app.presentation.v2.flow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.presentation.common.ManualTimeLogSheet
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.FatigueWarningLevel
import com.neuroflow.app.domain.service.TaskFatigueScoreUi
import com.neuroflow.app.presentation.v2.common.FatigueAwareScoreBadge
import com.neuroflow.app.presentation.v2.common.FatigueBurnoutWarningBanner
import com.neuroflow.app.presentation.v2.common.FatigueScoreDetailCard

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
    fatigueScores: Map<String, TaskFatigueScoreUi> = emptyMap(),
    nowMillis: Long = System.currentTimeMillis(),
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
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                border = BorderStroke(0.5.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.55f))
            ) {
                Text(
                    text = "${scheduledTasks.size} blocks",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = NeuroFlowThemeV2Colors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        if (scheduledTasks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(0.75.dp, NeuroFlowPaletteV2.specularBorderBrush())
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = NeuroFlowPaletteV2.OptimalEmerald,
                            modifier = Modifier.padding(10.dp).size(22.dp)
                        )
                    }
                    Text("Quiet Waters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.TextPrimary)
                    Text(
                        "No tasks scheduled for today. Tap the '+' composer to plan your next hyperfocus session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }
        } else {
            val activeOrNextIndex = scheduledTasks.indexOfFirst { task ->
                if (task.scheduledTime != null) {
                    val start = task.resolveScheduledEpochMillis() ?: (task.effectiveScheduledDateMillis() ?: 0L)
                    val end = start + TaskBlockPlan.plannedMinutes(task) * 60_000L
                    end >= nowMillis
                } else {
                    false
                }
            }

            scheduledTasks.forEachIndexed { index, task ->
                val scheduledMillis = if (task.scheduledTime != null) {
                    task.resolveScheduledEpochMillis() ?: task.effectiveScheduledDateMillis()
                } else null
                val taskPhase = if (scheduledMillis != null && timeline.isNotEmpty()) {
                    val timelineStart = timeline.firstOrNull()?.timestampMillis?.let { com.neuroflow.app.data.local.entity.startOfLocalDay(it) }
                        ?: com.neuroflow.app.data.local.entity.startOfLocalDay(nowMillis)
                    val alignedMillis = timelineStart + task.timelineStartMinuteOfDay() * 60_000L
                    timeline.minByOrNull { kotlin.math.abs(it.timestampMillis - alignedMillis) }?.phase ?: EnergyPhase.PEAK
                } else EnergyPhase.PEAK
                val phaseColor = NeuroFlowPaletteV2.accentForPhase(taskPhase)
                val isCurrentOrNext = index == activeOrNextIndex

                if (isCurrentOrNext) {
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
                                .size(14.dp)
                                .background(NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.22f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(NeuroFlowPaletteV2.ReboundCyan, CircleShape)
                            )
                        }
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NeuroFlowPaletteV2.ReboundCyan,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.5.dp)
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
                        modifier = Modifier.width(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 18.dp)
                                .size(if (isCurrentOrNext) 16.dp else 10.dp)
                                .background(
                                    if (isCurrentOrNext) phaseColor.copy(alpha = 0.22f) else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrentOrNext) 9.dp else 8.dp)
                                    .background(phaseColor, CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f, fill = false)
                                .heightIn(min = 40.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(phaseColor.copy(alpha = 0.55f), phaseColor.copy(alpha = 0.12f))
                                    )
                                )
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        key(task.id) {
                            val currentTask by rememberUpdatedState(task)
                            TimelineTaskCard(
                                task = task,
                                onTaskClick = { onTaskClick(currentTask) },
                                onComplete = { onCompleteTask(currentTask) },
                                onDefer = { onDeferTask(currentTask) },
                                onBreakDown = { onBreakDownTask(currentTask) },
                                onEdit = { onEditTask(currentTask) },
                                onShiftToPeak = onShiftToPeak?.let { shift -> { shift(currentTask) } },
                                onLogPastTime = { taskToLogTime = currentTask },
                                timeline = timeline,
                                fatigueScore = fatigueScores[task.id]
                            )
                        }
                    }
                }

                if (shouldShowUltradianBufferAfter(scheduledTasks, index)) {
                    UltradianRecoveryDivider()
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
    timeline: List<CircadianSnapshot> = emptyList(),
    fatigueScore: TaskFatigueScoreUi? = null
) {
    val energyBadgeColor = when (task.energyLevel) {
        EnergyLevel.HIGH -> NeuroFlowThemeV2Colors.WarmAmber
        EnergyLevel.MEDIUM -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
        EnergyLevel.LOW -> NeuroFlowThemeV2Colors.SageGreen
    }

    val microSteps = StepByStepPlanCodec.parse(task.ifThenPlan)
    val hasMicroSteps = microSteps.isNotEmpty()
    var showDetailSheet by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val currentOnComplete by rememberUpdatedState(onComplete)
    val currentOnDefer by rememberUpdatedState(onDefer)
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.55f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnComplete()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnDefer()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    val scheduledMillis = if (task.scheduledTime != null) {
        task.resolveScheduledEpochMillis() ?: task.effectiveScheduledDateMillis()
    } else null
    val taskPhase = if (scheduledMillis != null && timeline.isNotEmpty()) {
        val timelineStart = timeline.firstOrNull()?.timestampMillis?.let { com.neuroflow.app.data.local.entity.startOfLocalDay(it) }
            ?: com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
        val alignedMillis = timelineStart + task.timelineStartMinuteOfDay() * 60_000L
        timeline.minByOrNull { kotlin.math.abs(it.timestampMillis - alignedMillis) }?.phase ?: EnergyPhase.PEAK
    } else {
        when (task.energyLevel) {
            EnergyLevel.HIGH -> EnergyPhase.PEAK
            EnergyLevel.MEDIUM -> EnergyPhase.REBOUND
            EnergyLevel.LOW -> EnergyPhase.RECOVERY
        }
    }
    val accentColor = if (fatigueScore?.warningLevel == FatigueWarningLevel.HIGH_BURNOUT_RISK) {
        NeuroFlowPaletteV2.RecoveryRose
    } else {
        NeuroFlowPaletteV2.accentForPhase(taskPhase)
    }
    val cardShape = RoundedCornerShape(16.dp)

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
                    .background(color, cardShape)
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
                .border(1.dp, NeuroFlowPaletteV2.specularBorderBrush(accentColor, 0.85f), cardShape)
                .clickable(onClick = { showDetailSheet = true }),
            shape = cardShape,
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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onComplete()
                    },
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
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Daily Frog",
                                tint = NeuroFlowThemeV2Colors.SageGreen,
                                modifier = Modifier.size(16.dp)
                            )
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

                    Spacer(modifier = Modifier.height(6.dp))

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
                            color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.18f),
                            border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = formatDurationChip(TaskBlockPlan.plannedMinutes(task)),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Start and end time chip (for scheduled tasks)
                        if (scheduledMillis != null) {
                            val timeRangeStr = formatTimeRange(scheduledMillis, TaskBlockPlan.plannedMinutes(task))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = NeuroFlowThemeV2Colors.TextSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = timeRangeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = NeuroFlowThemeV2Colors.TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(energyBadgeColor.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.energyLevel.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = energyBadgeColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

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
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
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

                        if (hasMicroSteps) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${microSteps.size} steps",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (task.impactScore >= 70) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
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

                        if (task.postponeCount >= 3) {
                            Box(
                                modifier = Modifier
                                    .background(NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Dopamine Warm-Up",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.WarmAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (fatigueScore != null) {
                            FatigueAwareScoreBadge(
                                score = fatigueScore,
                                onClick = { showDetailSheet = true }
                            )
                        }
                    }

                    if (fatigueScore != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FatigueBurnoutWarningBanner(score = fatigueScore)
                    }

                    if (scheduledMillis != null && timeline.isNotEmpty()) {
                        val timelineStart = timeline.firstOrNull()?.timestampMillis?.let { com.neuroflow.app.data.local.entity.startOfLocalDay(it) }
                            ?: com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
                        val alignedMillis = timelineStart + task.timelineStartMinuteOfDay() * 60_000L
                        Spacer(modifier = Modifier.height(4.dp))
                        TaskSlotEnergyFitBadge(
                            taskScheduledMillis = alignedMillis,
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
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Daily Frog",
                                tint = NeuroFlowThemeV2Colors.SageGreen,
                                modifier = Modifier.size(20.dp)
                            )
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
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = NeuroFlowThemeV2Colors.TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${TaskBlockPlan.plannedMinutes(task)} mins",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                    Text("•", color = NeuroFlowThemeV2Colors.TextSecondary)
                    Text(
                        text = "Energy: ${task.energyLevel.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = energyBadgeColor
                    )
                }

                if (fatigueScore != null) {
                    FatigueScoreDetailCard(score = fatigueScore)
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                        contentColor = Color.White
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

internal fun shouldShowUltradianBufferAfter(tasks: List<TaskEntity>, index: Int): Boolean {
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
        runSum += TaskBlockPlan.plannedMinutes(t)
        lastTimedEndMinute = curStart + TaskBlockPlan.plannedMinutes(t)
        if (i == index) {
            return runSum >= 90
        }
        if (runSum >= 90) {
            runSum = 0
        }
    }
    return false
}


