package com.neuroflow.app.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.data.local.entity.startOfNextLocalDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.presentation.common.getQuadrantBgColor
import com.neuroflow.app.presentation.common.getQuadrantTextColor
import com.neuroflow.app.presentation.common.isProtectedSlotOnDay
import com.neuroflow.app.presentation.common.isSameCalendarDay
import com.neuroflow.app.presentation.common.isTaskOnDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val HOUR_HEIGHT = 64.dp
private val DAY_COLUMN_WIDTH = 110.dp
private val TIME_GUTTER_WIDTH = 52.dp

private fun formatHourLabel(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDay: (Long) -> Unit,
    onNavigateToFocus: (String) -> Unit,
    hideTopBar: Boolean = false,
    viewModel: WeeklyScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showProtectedSlotsSheet by remember { mutableStateOf(false) }
    var showIntentEditDialog by remember { mutableStateOf(false) }
    var intentInputText by remember { mutableStateOf("") }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val currentCal = Calendar.getInstance()
    val currentHour = currentCal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCal.get(Calendar.MINUTE)
    val currentMinuteOfDay = currentHour * 60 + currentMinute
    val todayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val density = LocalDensity.current

    // Auto-scroll to current time and today on initial load
    LaunchedEffect(Unit) {
        val targetHour = (currentHour - 1).coerceAtLeast(0)
        val targetY = with(density) { (targetHour * HOUR_HEIGHT.toPx()).toInt() }
        verticalScrollState.scrollTo(targetY)

        val todayIndex = uiState.weekDays.indexOfFirst { isSameCalendarDay(it, todayMillis) }
        if (todayIndex > 0) {
            val targetX = with(density) { ((todayIndex - 1).coerceAtLeast(0) * DAY_COLUMN_WIDTH.toPx()).toInt() }
            horizontalScrollState.scrollTo(targetX)
        }
    }

    val weekStartFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val weekEndFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val weekRangeText = if (uiState.weekDays.isNotEmpty()) {
        "${weekStartFormat.format(Date(uiState.weekDays.first()))} – ${weekEndFormat.format(Date(uiState.weekDays.last()))}"
    } else {
        "Weekly Schedule"
    }

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Weekly Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.goToCurrentWeek()
                                val targetHour = (currentHour - 1).coerceAtLeast(0)
                                val targetY = with(density) { (targetHour * HOUR_HEIGHT.toPx()).toInt() }
                                verticalScrollState.dispatchRawDelta((targetY - verticalScrollState.value).toFloat())
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = "This Week",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showProtectedSlotsSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Protected Slots",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Week switcher bar & Focus banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    // Navigation controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousWeek() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Week")
                        }

                        Text(
                            text = weekRangeText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        IconButton(
                            onClick = { viewModel.nextWeek() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next Week")
                        }
                    }

                    // Weekly Focus / Intention
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                intentInputText = uiState.weeklyIntent
                                showIntentEditDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎯", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.weeklyIntent.isNotBlank()) uiState.weeklyIntent else "Tap to set weekly focus / goal...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (uiState.weeklyIntent.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                                    color = if (uiState.weeklyIntent.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit weekly goal",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Timetable Header: Frozen vertically, scrolls horizontally in sync with grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Top-left corner spacer aligned with time gutter
                Box(
                    modifier = Modifier
                        .width(TIME_GUTTER_WIDTH)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                // 7 Day Headers: Sunday to Saturday
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    val dayHeaderNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    val dayHeaderNumFormat = SimpleDateFormat("d", Locale.getDefault())

                    uiState.weekDays.forEach { dayMillis ->
                        val isToday = isSameCalendarDay(dayMillis, todayMillis)
                        val dayDate = Date(dayMillis)

                        Surface(
                            modifier = Modifier
                                .width(DAY_COLUMN_WIDTH)
                                .height(48.dp)
                                .clickable { onNavigateToDay(dayMillis) },
                            color = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayHeaderNameFormat.format(dayDate).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = dayHeaderNumFormat.format(dayDate),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Timetable Body: Left time gutter + 7 day columns, scrolls vertically & horizontally
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
            ) {
                // Left Time Axis: 24 Hours
                Column(
                    modifier = Modifier
                        .width(TIME_GUTTER_WIDTH)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    for (hour in 0 until 24) {
                        Box(
                            modifier = Modifier
                                .width(TIME_GUTTER_WIDTH)
                                .height(HOUR_HEIGHT)
                                .padding(end = 4.dp, top = 2.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = formatHourLabel(hour),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // 7 Day Grid Columns: Sunday to Saturday
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    uiState.weekDays.forEach { dayMillis ->
                        val isToday = isSameCalendarDay(dayMillis, todayMillis)
                        val dayStart = dayMillis
                        val dayEnd = startOfNextLocalDay(dayStart)

                        // Filter tasks and protected slots for this day
                        val dayTasks = uiState.activeTasks.filter { isTaskOnDay(it, dayStart, dayEnd) }
                        val daySlots = uiState.protectedSlots.filter { isProtectedSlotOnDay(it, dayMillis) }

                        Box(
                            modifier = Modifier
                                .width(DAY_COLUMN_WIDTH)
                                .height(HOUR_HEIGHT * 24)
                                .background(if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.02f) else Color.Transparent)
                        ) {
                            // Hour divider lines & clickable hour cells
                            Column(modifier = Modifier.fillMaxSize()) {
                                for (hour in 0 until 24) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(HOUR_HEIGHT)
                                            .clickable { onNavigateToDay(dayMillis) }
                                    ) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.align(Alignment.TopCenter)
                                        )
                                    }
                                }
                            }

                            // Right vertical column border
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                    .align(Alignment.CenterEnd)
                            )

                            // Render Protected Time Slots
                            daySlots.forEach { slot ->
                                val slotOffsetY = (slot.startMinuteOfDay / 60f) * HOUR_HEIGHT.value
                                val slotHeight = ((slot.durationMinutes / 60f) * HOUR_HEIGHT.value).coerceAtLeast(24f)
                                val categoryColor = getPtsCategoryColor(slot.category)

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = categoryColor.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, categoryColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp)
                                        .offset(y = slotOffsetY.dp)
                                        .height(slotHeight.dp)
                                        .clickable { showProtectedSlotsSheet = true }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = getPtsCategoryEmoji(slot.category),
                                                fontSize = 10.sp
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = slot.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 10.sp,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                        }
                                        if (slotHeight >= 36f) {
                                            val startH = slot.startMinuteOfDay / 60
                                            val startM = slot.startMinuteOfDay % 60
                                            val endM = (slot.startMinuteOfDay + slot.durationMinutes) % 60
                                            val endH = ((slot.startMinuteOfDay + slot.durationMinutes) / 60) % 24
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d:%02d-%02d:%02d", startH, startM, endH, endM),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Black.copy(alpha = 0.75f),
                                                fontSize = 8.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Render Scheduled Tasks
                            dayTasks.forEach { task ->
                                val startMinute = task.timelineStartMinuteOfDay()
                                val durationMins = (if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 45).coerceIn(15, 360)
                                val taskOffsetY = (startMinute / 60f) * HOUR_HEIGHT.value
                                val taskHeight = ((durationMins / 60f) * HOUR_HEIGHT.value).coerceAtLeast(24f)

                                val bgColor = getQuadrantBgColor(task.quadrant)
                                val textColor = getQuadrantTextColor(task.quadrant)

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = bgColor,
                                    border = BorderStroke(0.8.dp, textColor.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp)
                                        .offset(y = taskOffsetY.dp)
                                        .height(taskHeight.dp)
                                        .clickable { onNavigateToFocus(task.id) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 10.sp
                                        )
                                        if (taskHeight >= 36f) {
                                            val startH = startMinute / 60
                                            val startM = startMinute % 60
                                            val endTotal = startMinute + durationMins
                                            val endH = (endTotal / 60) % 24
                                            val endM = endTotal % 60
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d:%02d (%dm)", startH, startM, durationMins),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.75f),
                                                fontSize = 8.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Today current time indicator
                            if (isToday) {
                                val indicatorY = (currentMinuteOfDay / 60f) * HOUR_HEIGHT.value
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = indicatorY.dp)
                                ) {
                                    // Red line across day
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.error,
                                        thickness = 2.dp,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                    // Dot at start
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                            .align(Alignment.CenterStart)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Protected Slots Bottom Sheet
    if (showProtectedSlotsSheet) {
        ProtectedSlotBottomSheet(
            protectedSlots = uiState.protectedSlots,
            onDismiss = { showProtectedSlotsSheet = false },
            onSaveSlot = { slot -> viewModel.addProtectedSlot(slot) },
            onDeleteSlot = { slotId -> viewModel.removeProtectedSlot(slotId) },
            onToggleSlot = { slotId -> viewModel.toggleProtectedSlot(slotId) }
        )
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
                        placeholder = { Text("e.g. Finish feature launch, deep study") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveWeeklyIntent(intentInputText.trim())
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
