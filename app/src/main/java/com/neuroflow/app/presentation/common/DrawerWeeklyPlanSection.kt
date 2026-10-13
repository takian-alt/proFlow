package com.neuroflow.app.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.startOfNextLocalDay
import com.neuroflow.app.data.local.entity.effectiveScheduleAnchorMillis
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun getWeekDays(referenceMillis: Long = System.currentTimeMillis()): List<Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        add(Calendar.DAY_OF_YEAR, -daysFromMonday)
    }

    val days = mutableListOf<Long>()
    for (i in 0 until 7) {
        days.add(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return days
}

fun isSameCalendarDay(m1: Long, m2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = m1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = m2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

fun isTaskOnDay(task: TaskEntity, dayStart: Long, dayEnd: Long): Boolean {
    val anchor = task.effectiveScheduleAnchorMillis()
    if (anchor != null && anchor in dayStart until dayEnd) return true
    val deadline = task.deadlineDate
    if (anchor == null && deadline != null && deadline in dayStart until dayEnd) return true
    return false
}

fun isProtectedSlotOnDay(slot: ProtectedTimeSlot, dayMillis: Long): Boolean {
    return slot.isEnabled && ProtectedSlotClock.occursOnDay(slot, dayMillis)
}

@Composable
fun DrawerWeeklyPlanSection(
    weeklyIntent: String,
    activeTasks: List<TaskEntity>,
    protectedSlots: List<ProtectedTimeSlot>,
    onSaveWeeklyIntent: (String) -> Unit,
    onOpenWeeklySchedule: () -> Unit,
    onNavigateToSchedule: (Long) -> Unit = {}
) {
    val weekDays = remember { getWeekDays() }
    var showIntentEditDialog by remember { mutableStateOf(false) }
    var intentInputText by remember { mutableStateOf(weeklyIntent) }

    val startFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val endFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val weekRangeText = "${startFormat.format(Date(weekDays.first()))} – ${endFormat.format(Date(weekDays.last()))}"

    val weekTotalTasks = remember(activeTasks, weekDays) {
        val weekStart = weekDays.first()
        val weekEnd = startOfNextLocalDay(weekDays.last())
        activeTasks.count { isTaskOnDay(it, weekStart, weekEnd) }
    }
    val enabledSlotsCount = remember(protectedSlots) {
        protectedSlots.count { it.isEnabled }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenWeeklySchedule() }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarViewWeek,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Weekly Plan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = weekRangeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Weekly Schedule",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Weekly Focus / Intention banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎯", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (weeklyIntent.isNotBlank()) weeklyIntent else "Set weekly focus...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (weeklyIntent.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                                color = if (weeklyIntent.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                intentInputText = weeklyIntent
                                showIntentEditDialog = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Weekly Intent",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick stats & button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$weekTotalTasks tasks • $enabledSlotsCount protected blocks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "View Grid →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Weekly Intent Edit Dialog
    if (showIntentEditDialog) {
        AlertDialog(
            onDismissRequest = { showIntentEditDialog = false },
            title = { Text("Weekly Focus / Intention") },
            text = {
                Column {
                    Text(
                        "Set your core goal or priority for this week to keep planning aligned.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = intentInputText,
                        onValueChange = { intentInputText = it },
                        placeholder = { Text("e.g. Deep work on feature launch, submit report") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveWeeklyIntent(intentInputText.trim())
                        showIntentEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntentEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
