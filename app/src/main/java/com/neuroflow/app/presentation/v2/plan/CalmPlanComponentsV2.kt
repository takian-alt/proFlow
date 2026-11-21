package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.scheduler.ScheduleConflict
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.presentation.v2.flow.formatDurationChip
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private data class OpenSlotGap(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val durationMinutes: Int,
    val isPeakWindow: Boolean
)

private sealed interface AgendaEntry {
    val sortMinute: Int

    data class TaskRow(val task: TaskEntity, override val sortMinute: Int) : AgendaEntry
    data class GapRow(val gap: OpenSlotGap, override val sortMinute: Int) : AgendaEntry
}

private enum class DayPeriod(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    OVERTIME("Late / Overtime")
}

private fun formatMinuteOfDay12h(minuteOfDay: Int): String {
    val clamped = minuteOfDay.coerceIn(0, 23 * 60 + 59)
    val h = clamped / 60
    val m = clamped % 60
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    val amPm = if (h < 12) "AM" else "PM"
    return if (m == 0) "$h12 $amPm" else String.format(Locale.US, "%d:%02d %s", h12, m, amPm)
}

private fun formatHoursCompact(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    return when {
        safe < 60 -> "${safe}m"
        safe % 60 == 0 -> "${safe / 60}h"
        else -> String.format(Locale.US, "%.1fh", safe / 60.0)
    }
}

@Composable
fun CollapsibleDailyPulseCard(
    state: PlanUiStateV2,
    onAutoBalanceWeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val plannedMinutes = state.tasksForDay.sumOf { TaskBlockPlan.plannedMinutes(it) }
    val capacityMinutes = state.recommendedCapacityMinutes.coerceAtLeast(120)
    val peakStart = state.preferences.effectivePeakStart.takeIf { it in 0..23 } ?: state.preferences.peakEnergyStart.coerceIn(0, 23)
    val peakEnd = state.preferences.effectivePeakEnd.takeIf { it in 0..23 } ?: state.preferences.peakEnergyEnd.coerceIn(0, 23)

    val loadTone = when {
        plannedMinutes == 0 -> "Open day"
        plannedMinutes <= (capacityMinutes * 0.75).toInt() -> "Light day"
        plannedMinutes <= (capacityMinutes * 1.1).toInt() -> "Balanced day"
        else -> "Packed day"
    }
    val toneColor = when {
        plannedMinutes == 0 -> NeuroFlowPaletteV2.ReboundCyan
        plannedMinutes <= (capacityMinutes * 0.75).toInt() -> NeuroFlowPaletteV2.OptimalEmerald
        plannedMinutes <= (capacityMinutes * 1.1).toInt() -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
        else -> NeuroFlowPaletteV2.PeakAmber
    }

    val summaryLine = buildString {
        append("${formatHoursCompact(plannedMinutes)} planned")
        append(" · Peak ${formatMinuteOfDay12h(peakStart * 60)}–${formatMinuteOfDay12h(peakEnd * 60)}")
        append(" · $loadTone")
    }

    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("plan_daily_pulse_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(toneColor, CircleShape)
                    )
                    Text(
                        text = summaryLine,
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse daily pulse" else "Expand daily pulse",
                    tint = NeuroFlowThemeV2Colors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.35f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily capacity: ${formatHoursCompact(plannedMinutes)} / ${formatHoursCompact(capacityMinutes)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                        if (state.energyUi != null) {
                            Text(
                                text = "${state.energyUi.availableEnergy}% battery now",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = toneColor
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (plannedMinutes.toFloat() / capacityMinutes.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = toneColor,
                        trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7-day workload distribution",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.TextTertiary
                        )
                        TextButton(
                            onClick = onAutoBalanceWeek,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Balance Week",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        }
                    }

                    SevenDayCapacityBar(
                        loadDistribution = state.sevenDayLoadDistribution,
                        selectedDateMillis = state.selectedDateMillis,
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmDayAgendaView(
    state: PlanUiStateV2,
    onResolveConflict: (ScheduleConflict) -> Unit,
    onStartFocus: (String) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onBreakDownTask: (TaskEntity) -> Unit,
    onAssignTaskTime: (TaskEntity) -> Unit,
    onLogPastTime: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onTapOpenSlot: (startMinuteOfDay: Int) -> Unit,
    onOpenSmartPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTaskForSheet by remember { mutableStateOf<TaskEntity?>(null) }

    val timedTasks = remember(state.tasksForDay) {
        state.tasksForDay
            .filter { it.scheduledTime != null }
            .sortedBy { it.timelineStartMinuteOfDay() }
    }
    val untimedTasks = remember(state.tasksForDay) {
        state.tasksForDay.filter { it.scheduledTime == null }
    }

    val peakStartMin = (state.preferences.effectivePeakStart.takeIf { it in 0..23 }
        ?: state.preferences.peakEnergyStart.coerceIn(0, 23)) * 60
    val peakEndMin = (state.preferences.effectivePeakEnd.takeIf { it in 0..23 }
        ?: state.preferences.peakEnergyEnd.coerceIn(0, 23)) * 60
    val workStartMin = state.workDayStart.coerceIn(6, 12) * 60
    val workEndMin = state.workDayEnd.coerceIn(16, 23) * 60

    val agendaEntriesByPeriod = remember(timedTasks, workStartMin, workEndMin, peakStartMin, peakEndMin) {
        val entries = mutableListOf<AgendaEntry>()
        var cursor = workStartMin

        timedTasks.forEach { task ->
            val startMin = task.timelineStartMinuteOfDay().coerceIn(0, 23 * 60 + 50)
            val duration = TaskBlockPlan.plannedMinutes(task).coerceAtLeast(10)
            val endMin = (startMin + duration).coerceAtMost(24 * 60)

            if (startMin - cursor >= 30) {
                val gapMid = (cursor + startMin) / 2
                entries.add(
                    AgendaEntry.GapRow(
                        gap = OpenSlotGap(
                            startMinuteOfDay = cursor,
                            endMinuteOfDay = startMin,
                            durationMinutes = startMin - cursor,
                            isPeakWindow = gapMid in peakStartMin until peakEndMin
                        ),
                        sortMinute = cursor
                    )
                )
            }
            entries.add(AgendaEntry.TaskRow(task = task, sortMinute = startMin))
            cursor = maxOf(cursor, endMin)
        }

        if (workEndMin - cursor >= 30 && timedTasks.isNotEmpty()) {
            val gapMid = (cursor + workEndMin) / 2
            entries.add(
                AgendaEntry.GapRow(
                    gap = OpenSlotGap(
                        startMinuteOfDay = cursor,
                        endMinuteOfDay = workEndMin,
                        durationMinutes = workEndMin - cursor,
                        isPeakWindow = gapMid in peakStartMin until peakEndMin
                    ),
                    sortMinute = cursor
                )
            )
        }

        entries.groupBy { entry ->
            when {
                entry.sortMinute < 12 * 60 -> DayPeriod.MORNING
                entry.sortMinute < 17 * 60 -> DayPeriod.AFTERNOON
                entry.sortMinute < workEndMin -> DayPeriod.EVENING
                else -> DayPeriod.OVERTIME
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calm_plan_agenda_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Gentle conflict notice only when conflicts exist
        if (state.conflicts.isNotEmpty()) {
            item(key = "conflict_banner") {
                val firstConflict = state.conflicts.first()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = NeuroFlowPaletteV2.RecoveryRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${state.conflicts.size} schedule overlap${if (state.conflicts.size == 1) "" else "s"} detected",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                        }
                        TextButton(
                            onClick = { onResolveConflict(firstConflict) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "Fix",
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowPaletteV2.RecoveryRose
                            )
                        }
                    }
                }
            }
        }

        if (timedTasks.isEmpty() && untimedTasks.isEmpty()) {
            item(key = "calm_empty_day") {
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
                        Text(
                            text = "Wide Open Day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                        Text(
                            text = "Nothing scheduled for this day yet. Tap Smart Plan to fit your highest-priority work, or add a task directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onOpenSmartPlan,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Smart Plan", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onTapOpenSlot(peakStartMin) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Fill Peak Slot")
                            }
                        }
                    }
                }
            }
        } else {
            DayPeriod.entries.forEach { period ->
                val rows = agendaEntriesByPeriod[period].orEmpty()
                if (rows.isNotEmpty()) {
                    item(key = "period_${period.name}") {
                        Text(
                            text = period.label.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeuroFlowThemeV2Colors.TextTertiary,
                            letterSpacing = 0.9.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 2.dp)
                        )
                    }
                    items(rows, key = { row ->
                        when (row) {
                            is AgendaEntry.TaskRow -> "task_${row.task.id}"
                            is AgendaEntry.GapRow -> "gap_${row.gap.startMinuteOfDay}_${row.gap.endMinuteOfDay}"
                        }
                    }) { row ->
                        when (row) {
                            is AgendaEntry.TaskRow -> QuietAgendaTaskCard(
                                task = row.task,
                                onTapCard = { selectedTaskForSheet = row.task },
                                onComplete = { onCompleteTask(row.task) },
                                onStartFocus = { onStartFocus(row.task.id) }
                            )
                            is AgendaEntry.GapRow -> OpenSlotGapCard(
                                gap = row.gap,
                                onTap = { onTapOpenSlot(row.gap.startMinuteOfDay) }
                            )
                        }
                    }
                }
            }

            if (untimedTasks.isNotEmpty()) {
                item(key = "period_anytime") {
                    Text(
                        text = "ANYTIME TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeuroFlowThemeV2Colors.TextTertiary,
                        letterSpacing = 0.9.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 2.dp)
                    )
                }
                items(untimedTasks, key = { "untimed_${it.id}" }) { task ->
                    QuietAgendaTaskCard(
                        task = task,
                        onTapCard = { selectedTaskForSheet = task },
                        onComplete = { onCompleteTask(task) },
                        onStartFocus = { onStartFocus(task.id) }
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Quiet Tap-to-Expand Task Action Sheet
    selectedTaskForSheet?.let { task ->
        ModalBottomSheet(
            onDismissRequest = { selectedTaskForSheet = null },
            containerColor = NeuroFlowPaletteV2.SurfaceCard,
            contentColor = NeuroFlowPaletteV2.TextPrimary,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeuroFlowPaletteV2.TextPrimary
                )
                val timeDesc = if (task.scheduledTime != null) {
                    "${formatMinuteOfDay12h(task.timelineStartMinuteOfDay())} · ${formatDurationChip(TaskBlockPlan.plannedMinutes(task))} · ${task.energyLevel.name}"
                } else {
                    "Anytime today · ${formatDurationChip(TaskBlockPlan.plannedMinutes(task))} · ${task.energyLevel.name}"
                }
                Text(
                    text = timeDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.TextSecondary
                )

                Button(
                    onClick = {
                        selectedTaskForSheet = null
                        onStartFocus(task.id)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Focus", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedTaskForSheet = null
                            onAssignTaskTime(task)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Time")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedTaskForSheet = null
                            onEditTask(task)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedTaskForSheet = null
                            onBreakDownTask(task)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Break Down")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedTaskForSheet = null
                            onLogPastTime(task)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Time")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            selectedTaskForSheet = null
                            onCompleteTask(task)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeuroFlowPaletteV2.OptimalEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Done", color = NeuroFlowPaletteV2.OptimalEmerald, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            selectedTaskForSheet = null
                            onDeleteTask(task)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = NeuroFlowPaletteV2.RecoveryRose, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", color = NeuroFlowPaletteV2.RecoveryRose, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuietAgendaTaskCard(
    task: TaskEntity,
    onTapCard: () -> Unit,
    onComplete: () -> Unit,
    onStartFocus: () -> Unit
) {
    val energyColor = when (task.energyLevel) {
        EnergyLevel.HIGH -> NeuroFlowThemeV2Colors.WarmAmber
        EnergyLevel.MEDIUM -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
        EnergyLevel.LOW -> NeuroFlowThemeV2Colors.SageGreen
    }
    val timeText = if (task.scheduledTime != null) {
        formatMinuteOfDay12h(task.timelineStartMinuteOfDay())
    } else {
        "Anytime"
    }

    Surface(
        onClick = onTapCard,
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(0.75.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onComplete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = "Complete ${task.title}",
                    tint = NeuroFlowThemeV2Colors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = NeuroFlowThemeV2Colors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$timeText · ${formatDurationChip(TaskBlockPlan.plannedMinutes(task))}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(energyColor, CircleShape)
                        )
                        Text(
                            text = when (task.energyLevel) {
                                EnergyLevel.HIGH -> "High"
                                EnergyLevel.MEDIUM -> "Mod"
                                EnergyLevel.LOW -> "Low"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = energyColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            IconButton(
                onClick = onStartFocus,
                modifier = Modifier
                    .size(48.dp)
                    .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.14f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start Focus on ${task.title}",
                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun OpenSlotGapCard(
    gap: OpenSlotGap,
    onTap: () -> Unit
) {
    val accent = if (gap.isPeakWindow) NeuroFlowPaletteV2.OptimalEmerald else NeuroFlowThemeV2Colors.TextTertiary
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(14.dp),
        color = if (gap.isPeakWindow) NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.06f) else Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Open · ${formatMinuteOfDay12h(gap.startMinuteOfDay)} – ${formatMinuteOfDay12h(gap.endMinuteOfDay)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = if (gap.isPeakWindow) NeuroFlowThemeV2Colors.TextPrimary else NeuroFlowThemeV2Colors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (gap.isPeakWindow) "Peak · ${formatHoursCompact(gap.durationMinutes)}" else formatHoursCompact(gap.durationMinutes),
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSmartPlanSheet(
    unscheduledCount: Int,
    onFitToday: () -> Unit,
    onQuickSweep: () -> Unit,
    onBalanceWeek: () -> Unit,
    onDismiss: () -> Unit
) {
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
                .testTag("unified_smart_plan_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Smart Plan Assistant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = NeuroFlowPaletteV2.TextPrimary
            )
            Text(
                text = "Choose how you want NeuroFlow to organize your schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowPaletteV2.TextSecondary
            )

            SmartPlanActionTile(
                icon = Icons.Default.AutoAwesome,
                accent = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                title = "Fit Today's Tasks",
                subtitle = "Preview the best energy-matched slots for today and confirm with one tap.",
                onClick = {
                    onDismiss()
                    onFitToday()
                }
            )
            SmartPlanActionTile(
                icon = Icons.Default.CleaningServices,
                accent = NeuroFlowPaletteV2.PeakAmber,
                title = "Quick 1-Min Sweep" + if (unscheduledCount > 0) " ($unscheduledCount)" else "",
                subtitle = "Rapidly decide which unscheduled items happen today, later, or go to Notes.",
                onClick = {
                    onDismiss()
                    onQuickSweep()
                }
            )
            SmartPlanActionTile(
                icon = Icons.Default.CalendarMonth,
                accent = NeuroFlowPaletteV2.OptimalEmerald,
                title = "Balance My Week",
                subtitle = "Spread tasks evenly across the next 7 days to avoid overload.",
                onClick = {
                    onDismiss()
                    onBalanceWeek()
                }
            )
        }
    }
}

@Composable
private fun SmartPlanActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowPaletteV2.SurfaceElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowPaletteV2.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnscheduledInboxBottomSheet(
    unscheduledTasks: List<TaskEntity>,
    onScheduleToday: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
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
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Inbox, contentDescription = null, tint = NeuroFlowPaletteV2.PeakAmber, modifier = Modifier.size(20.dp))
                Text(
                    text = "Unscheduled (${unscheduledTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeuroFlowPaletteV2.TextPrimary
                )
            }
            if (unscheduledTasks.isEmpty()) {
                Text(
                    text = "All tasks have a scheduled day!",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.TextSecondary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(unscheduledTasks, key = { it.id }) { task ->
                        Surface(
                            onClick = {
                                onDismiss()
                                onEditTask(task)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = NeuroFlowPaletteV2.SurfaceElevated,
                            border = BorderStroke(0.75.dp, NeuroFlowPaletteV2.BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeuroFlowPaletteV2.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatDurationChip(TaskBlockPlan.plannedMinutes(task))} · ${task.energyLevel.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowPaletteV2.TextSecondary
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onScheduleToday(task) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Add Today", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
