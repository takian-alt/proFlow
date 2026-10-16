package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreenV2(
    onNavigateToFocus: (String) -> Unit,
    viewModel: PlanViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = NeuroFlowThemeV2Colors.Obsidian
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Executive Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    Text(
                        text = "Circadian Time-Blocking & Prioritization",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }

                // AI Auto-Schedule quick trigger
                FilledTonalButton(
                    onClick = { viewModel.triggerAutoScheduleNow() },
                    enabled = !state.isAutoScheduling,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    if (state.isAutoScheduling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Plan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Mode Selector Segmented Tabs
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                PlanViewModeV2.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.viewMode == mode,
                        onClick = { viewModel.setViewMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = PlanViewModeV2.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            activeContentColor = Color.White,
                            inactiveContainerColor = NeuroFlowThemeV2Colors.SlateSurface,
                            inactiveContentColor = NeuroFlowThemeV2Colors.TextSecondary
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(mode.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            // Date Carousel (Today, Tomorrow, Day+2, Day+3...)
            DateCarouselStrip(
                selectedDateMillis = state.selectedDateMillis,
                onSelectDate = { viewModel.selectDate(it) }
            )

            // Conflict Warning Banner if any
            if (state.conflicts.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = NeuroFlowThemeV2Colors.RoseCoralContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.RoseCoral)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NeuroFlowThemeV2Colors.RoseCoral)
                        Text(
                            text = "${state.conflicts.size} schedule conflict(s) detected. Tap Auto-Plan to resolve.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                }
            }

            // Content Area based on ViewMode
            when (state.viewMode) {
                PlanViewModeV2.TIMELINE -> TimelineScheduleView(
                    state = state,
                    onStartFocus = onNavigateToFocus,
                    onCompleteTask = { viewModel.completeTask(it) }
                )
                PlanViewModeV2.MATRIX -> MatrixPlanView(
                    state = state,
                    onStartFocus = onNavigateToFocus,
                    onMoveQuadrant = { task, q -> viewModel.moveTaskToQuadrant(task, q) },
                    onCompleteTask = { viewModel.completeTask(it) }
                )
                PlanViewModeV2.AUTOPILOT -> AutopilotConfigView(
                    state = state,
                    onToggleAutoSchedule = { viewModel.toggleAutoSchedule() },
                    onTriggerNow = { viewModel.triggerAutoScheduleNow() },
                    onToggleSlot = { id, enabled -> viewModel.toggleProtectedSlot(id, enabled) }
                )
            }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    .width(64.dp)
                    .heightIn(min = 52.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else NeuroFlowThemeV2Colors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else NeuroFlowThemeV2Colors.TextPrimary
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

@Composable
fun TimelineScheduleView(
    state: PlanUiStateV2,
    onStartFocus: (String) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit
) {
    val peakStart = state.preferences.peakEnergyStart.takeIf { it > 0 } ?: 9
    val peakEnd = state.preferences.peakEnergyEnd.takeIf { it > 0 } ?: 12

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Circadian Peak indicator badge
        item {
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
                    Text("⚡", fontSize = 16.sp)
                    Text(
                        text = "Peak Circadian Window: ${String.format("%02d:00", peakStart)} – ${String.format("%02d:00", peakEnd)} (Optimal for Frogs & High Effort)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.WarmAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Scheduled Tasks
        if (state.tasksForDay.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📅 Clear Horizon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "No tasks scheduled for this day. Switch to Matrix or tap 'Auto-Plan' to organize your backlog.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }
                }
            }
        } else {
            items(state.tasksForDay, key = { it.id }) { task ->
                val timeLabel = task.scheduledTime?.let { millis ->
                    val h = (millis / 3_600_000L).toInt()
                    val m = ((millis % 3_600_000L) / 60_000L).toInt()
                    String.format("%02d:%02d", h, m)
                } ?: "Anytime"

                val isDuringPeak = task.scheduledTime?.let { millis ->
                    val h = (millis / 3_600_000L).toInt()
                    h in peakStart until peakEnd
                } ?: false

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
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Time Pill
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(52.dp)
                        ) {
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDuringPeak) NeuroFlowThemeV2Colors.WarmAmber else NeuroFlowThemeV2Colors.TextSecondary
                            )
                            if (isDuringPeak) {
                                Text("PEAK", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = NeuroFlowThemeV2Colors.WarmAmber)
                            }
                        }

                        // Complete check
                        IconButton(
                            onClick = { onCompleteTask(task) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Complete", tint = NeuroFlowThemeV2Colors.TextSecondary)
                        }

                        // Title & Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (task.isFrog) Text("🐸", fontSize = 14.sp)
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${task.estimatedDurationMinutes}m • ${task.energyLevel.name} • ${task.quadrant.name.replace('_', ' ')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary
                            )
                        }

                        // Focus Button (>=48dp)
                        IconButton(
                            onClick = { onStartFocus(task.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Focus", tint = NeuroFlowThemeV2Colors.ElectricIndigo)
                        }
                    }
                }
            }
        }

        // Protected Time Slots Section
        if (state.protectedSlots.any { it.isEnabled }) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🛡️ Protected Deep Work & Rest Slots",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                )
            }

            items(state.protectedSlots.filter { it.isEnabled }, key = { it.id }) { slot ->
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
                        Icon(Icons.Default.Lock, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigo, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(slot.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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

@Composable
fun MatrixPlanView(
    state: PlanUiStateV2,
    onStartFocus: (String) -> Unit,
    onMoveQuadrant: (TaskEntity, Quadrant) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        val quadrants = listOf(
            Quadrant.DO_FIRST to ("🔴 Do First (Urgent & High Impact / Frog)" to NeuroFlowThemeV2Colors.RoseCoral),
            Quadrant.SCHEDULE to ("🔵 Schedule (High Impact, Deep Focus)" to NeuroFlowThemeV2Colors.ElectricIndigoGlow),
            Quadrant.DELEGATE to ("🟢 Quick Wins / Low Energy" to NeuroFlowThemeV2Colors.SageGreen),
            Quadrant.ELIMINATE to ("⚪ Backlog / Low Priority" to NeuroFlowThemeV2Colors.TextTertiary)
        )

        items(quadrants) { (quadrant, info) ->
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
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                        Box(
                            modifier = Modifier
                                .background(color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("${tasks.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
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
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Complete", tint = NeuroFlowThemeV2Colors.TextSecondary)
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (task.isFrog) Text("🐸", fontSize = 12.sp)
                                            Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(
                                            "${task.estimatedDurationMinutes}m • ${task.energyLevel.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowThemeV2Colors.TextSecondary
                                        )
                                    }

                                    // Move to next quadrant shortcut (touch target >= 48dp)
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
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Cycle Quadrant", tint = NeuroFlowThemeV2Colors.TextSecondary, modifier = Modifier.size(18.dp))
                                    }

                                    // Focus launch (touch target >= 48dp)
                                    IconButton(
                                        onClick = { onStartFocus(task.id) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Focus", tint = NeuroFlowThemeV2Colors.ElectricIndigo, modifier = Modifier.size(20.dp))
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
    onToggleSlot: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Autopilot Master Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🤖 Circadian Autopilot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Auto-Schedule Optimization Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Algorithmic Principles
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🧠 Neuroscience Auto-Scheduling Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                    Text("• Process C Alertness Matching: High cognitive effort and Frog tasks are assigned to peak circadian windows.", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextSecondary)
                    Text("• Protected Buffer Preservation: Deep work contracts and rest intervals are strictly respected.", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextSecondary)
                    Text("• Cognitive Fatigue Guard: Tasks exceeding 60m are scheduled with automatic recharge buffers.", style = MaterialTheme.typography.bodySmall, color = NeuroFlowThemeV2Colors.TextSecondary)
                }
            }
        }

        // Protected Slots Management
        item {
            Text("🛡️ Manage Protected Time Slots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        Text(slot.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
