package com.neuroflow.app.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.presentation.common.NewTaskSheet
import com.neuroflow.app.presentation.common.getQuadrantBgColor
import com.neuroflow.app.presentation.common.getQuadrantTextColor
import java.text.SimpleDateFormat
import java.util.*

private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR
private val HOUR_SLOT_HEIGHT = 64.dp
private val MIN_SEGMENT_HEIGHT = 12.dp

private data class HourSegment(
    val task: TaskEntity,
    val offsetMinutesInHour: Int,
    val overlapMinutes: Int,
    val startsInThisHour: Boolean,
    val continuesFromPreviousHour: Boolean,
    val continuesToNextHour: Boolean
)

private data class ProtectedSlotSegment(
    val slot: ProtectedTimeSlot,
    val offsetMinutesInHour: Int,
    val overlapMinutes: Int,
    val startsInThisHour: Boolean,
    val continuesFromPreviousHour: Boolean,
    val continuesToNextHour: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    initialDateMillis: Long? = null,
    onNavigateToFocus: (String) -> Unit,
    hideTopBar: Boolean = false,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialDateMillis) {
        if (initialDateMillis != null && initialDateMillis > 0L) {
            viewModel.selectDate(initialDateMillis)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.autoScheduleResultMessage) {
        uiState.autoScheduleResultMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearAutoScheduleResultMessage()
        }
    }

    // Sheet states: null = closed, -1 = new task (FAB), 0..23 = slot tap
    var addSheetForHour by remember { mutableStateOf<Int?>(null) }
    var addSheetForMinute by remember { mutableStateOf(0) }
    var pickerHour by remember { mutableStateOf<Int?>(null) }
    var pickerMinute by remember { mutableStateOf(0) }
    var showProtectedSlotsSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
    val isViewingToday = isToday(uiState.selectedDate)

    val listState = rememberLazyListState()
    LaunchedEffect(uiState.selectedDate) {
        val targetHour = if (isViewingToday) {
            (currentHour - 1).coerceIn(0, 23)
        } else {
            (uiState.workDayStart - 1).coerceIn(0, 23)
        }
        // Index 0 of the list is the day chrome, so every hour sits one position later.
        listState.animateScrollToItem(targetHour + 1)
    }

    val activeProtectedSlotsToday = remember(uiState.protectedTimeSlots, uiState.selectedDate) {
        uiState.protectedTimeSlots.filter { ProtectedSlotClock.occursOnDay(it, uiState.selectedDate) && it.isEnabled }
    }

    val totalPlannedMinutes = remember(uiState.tasksForDay) {
        uiState.tasksForDay.sumOf { taskDurationMinutes(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (!hideTopBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Schedule",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isViewingToday) "Today's Agenda" else "Time Blocking",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        if (!isViewingToday) {
                            FilledTonalButton(
                                onClick = { viewModel.selectToday() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    "Today",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        FilterChip(
                            selected = uiState.isAutoScheduleOn,
                            onClick = { viewModel.toggleAutoSchedule() },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (uiState.isAutoScheduling) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = if (uiState.isAutoScheduleOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else {
                                        Text("⚡ ")
                                    }
                                    Text("Auto", fontWeight = FontWeight.SemiBold)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        if (uiState.isAutoScheduleOn) {
                            IconButton(
                                onClick = { viewModel.triggerAutoScheduleNow() },
                                enabled = !uiState.isAutoScheduling
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Re-schedule Now",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        val enabledSlotCount = uiState.protectedTimeSlots.count { it.isEnabled }
                        IconButton(onClick = { showProtectedSlotsSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (enabledSlotCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(enabledSlotCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Protected Time Slots",
                                    tint = if (enabledSlotCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    addSheetForHour = -1
                    addSheetForMinute = 0
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Block Time", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Sleek Date Header with Calendar Dialog trigger
            DateNavigationHeader(
                selectedDate = uiState.selectedDate,
                onPreviousDay = { viewModel.previousDay() },
                onNextDay = { viewModel.nextDay() },
                onOpenDatePicker = { showDatePicker = true }
            )

            // 2. Timeline, with the day chrome as the first row so it scrolls away instead of
            // permanently eating the upper part of the screen.
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    Column {
                        WeekDayStrip(
                            selectedDate = uiState.selectedDate,
                            allActiveTasks = uiState.allActiveTasks,
                            onSelectDate = { viewModel.selectDate(it) }
                        )

                        DaySummaryBar(
                            tasksCount = uiState.tasksForDay.size,
                            totalPlannedMinutes = totalPlannedMinutes,
                            protectedSlotsCount = activeProtectedSlotsToday.size,
                            workDayStart = uiState.workDayStart,
                            workDayEnd = uiState.workDayEnd
                        )

                        if (uiState.conflictsForDay.isNotEmpty()) {
                            ConflictGuidanceCard(
                                conflicts = uiState.conflictsForDay,
                                onResolveConflict = { taskId, suggestedMin ->
                                    viewModel.resolveConflictByMoving(taskId, suggestedMin)
                                },
                                onTriggerAutoSchedule = {
                                    viewModel.triggerAutoScheduleNow()
                                }
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        if (uiState.lockedTasks.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "LOCKED SCHEDULE (${uiState.lockedTasks.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    uiState.lockedTasks.forEach { task ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { onNavigateToFocus(task.id) }
                                                .padding(vertical = 3.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                task.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                "Focus →",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(HOURS_PER_DAY) { hour ->
                    TimelineRow(
                        hour = hour,
                        tasks = uiState.tasksForDay,
                        protectedSlots = activeProtectedSlotsToday,
                        selectedDate = uiState.selectedDate,
                        isCurrentHour = isViewingToday && hour == currentHour,
                        currentMinuteOfHour = if (isViewingToday && hour == currentHour) currentMinute else null,
                        isWorkHour = hour in uiState.workDayStart until uiState.workDayEnd,
                        onTaskClick = onNavigateToFocus,
                        onSlotClick = { minute ->
                            pickerHour = hour
                            pickerMinute = minute
                        }
                    )
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { pickedMillis ->
                        viewModel.selectDate(pickedMillis)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Task picker bottom sheet — tap an empty slot to assign an existing task
    pickerHour?.let { hour ->
        val unscheduledTasks = uiState.allActiveTasks.filter {
            !AutoSchedulingContracts.hasManualScheduleData(it) && it.habitDate == null && !it.isScheduleLocked
        }
        TaskPickerSheet(
            hour = hour,
            minute = pickerMinute,
            tasks = unscheduledTasks,
            onDismiss = { pickerHour = null },
            onPick = { task ->
                viewModel.scheduleTask(task, hour, pickerMinute)
                pickerHour = null
            },
            onNewTask = {
                addSheetForHour = hour
                addSheetForMinute = pickerMinute
                pickerHour = null
            }
        )
    }

    // New task sheet — FAB or "new task" from picker
    addSheetForHour?.let { hour ->
        NewTaskSheet(
            onDismiss = { addSheetForHour = null },
            onSave = { task ->
                val scheduledTask = if (hour >= 0) {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = uiState.selectedDate
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, addSheetForMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val fullMillis = cal.timeInMillis
                    val dateCal = Calendar.getInstance().apply {
                        timeInMillis = fullMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val scheduledDate = dateCal.timeInMillis
                    val scheduledTime = fullMillis - scheduledDate
                    task.copy(
                        scheduledDate = scheduledDate,
                        scheduledTime = scheduledTime,
                        isAutoScheduled = false,
                        lastAutoScheduledAt = null
                    )
                } else task
                viewModel.insertTask(scheduledTask)
                addSheetForHour = null
            },
            availableTasks = uiState.allActiveTasks
        )
    }

    if (showProtectedSlotsSheet) {
        ProtectedSlotBottomSheet(
            protectedSlots = uiState.protectedTimeSlots,
            initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            initialMinute = 0,
            initialDate = uiState.selectedDate,
            onDismiss = { showProtectedSlotsSheet = false },
            onSaveSlot = { slot -> viewModel.addProtectedSlot(slot) },
            onDeleteSlot = { slotId -> viewModel.removeProtectedSlot(slotId) },
            onToggleSlot = { slotId -> viewModel.toggleProtectedSlot(slotId) }
        )
    }
}

@Composable
private fun DateNavigationHeader(
    selectedDate: Long,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = remember(selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        when {
            isToday(selectedDate) -> "Today • " + SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
            isYesterday(selectedDate) -> "Yesterday • " + SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
            isTomorrow(selectedDate) -> "Tomorrow • " + SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
            else -> SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(cal.time)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                onClick = onOpenDatePicker,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Select Date",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onNextDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekDayStrip(
    selectedDate: Long,
    allActiveTasks: List<TaskEntity>,
    onSelectDate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDays = remember(selectedDate) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        val mondayMillis = cal.timeInMillis
        (0..6).map { offset ->
            Calendar.getInstance().apply {
                timeInMillis = mondayMillis
                add(Calendar.DAY_OF_MONTH, offset)
            }.timeInMillis
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
            val numFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }

            weekDays.forEach { dayMillis ->
                val isSelected = isSameDay(dayMillis, selectedDate)
                val isCurrentDay = isToday(dayMillis)
                val dayStart = dayMillis
                val dayEnd = dayMillis + 86400000L

                val tasksCount = remember(allActiveTasks, dayMillis) {
                    allActiveTasks.count { task ->
                        isTaskOnDate(task, dayStart, dayEnd)
                    }
                }

                Surface(
                    onClick = { onSelectDate(dayMillis) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isCurrentDay -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    },
                    border = if (isCurrentDay && !isSelected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    } else null
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayFormat.format(Date(dayMillis)).take(3),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isCurrentDay -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = numFormat.format(Date(dayMillis)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected || isCurrentDay) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Task density indicator dot(s)
                        Box(
                            modifier = Modifier.height(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (tasksCount > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val dotCount = minOf(tasksCount, 3)
                                    repeat(dotCount) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                                    } else {
                                                        MaterialTheme.colorScheme.primary
                                                    },
                                                    shape = RoundedCornerShape(2.dp)
                                                )
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
}

@Composable
private fun DaySummaryBar(
    tasksCount: Int,
    totalPlannedMinutes: Int,
    protectedSlotsCount: Int,
    workDayStart: Int,
    workDayEnd: Int,
    modifier: Modifier = Modifier
) {
    val hrs = totalPlannedMinutes / 60
    val mins = totalPlannedMinutes % 60
    val plannedStr = if (hrs > 0 && mins > 0) "${hrs}h ${mins}m" else if (hrs > 0) "${hrs}h" else "${mins}m"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryChip(
            icon = "📋",
            text = "$tasksCount ${if (tasksCount == 1) "task" else "tasks"}",
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
        if (totalPlannedMinutes > 0) {
            SummaryChip(
                icon = "⏱️",
                text = "$plannedStr planned",
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        }
        if (protectedSlotsCount > 0) {
            SummaryChip(
                icon = "🔒",
                text = "$protectedSlotsCount protected",
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            )
        }
        SummaryChip(
            icon = "⚡",
            text = "Work: ${formatHourOnly(workDayStart)} - ${formatHourOnly(workDayEnd)}",
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun SummaryChip(
    icon: String,
    text: String,
    containerColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ConflictGuidanceCard(
    conflicts: List<com.neuroflow.app.domain.scheduler.ScheduleConflict>,
    onResolveConflict: (taskId: String, suggestedMin: Int) -> Unit,
    onTriggerAutoSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SCHEDULE CONFLICTS (${conflicts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                TextButton(
                    onClick = onTriggerAutoSchedule,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Auto-Resolve", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            conflicts.take(3).forEach { conflict ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${conflict.taskTitle}: ${conflict.conflictDescription}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "💡 ${conflict.actionableSuggestion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (conflict.suggestedNewStartMinute != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            FilledTonalButton(
                                onClick = { onResolveConflict(conflict.taskId, conflict.suggestedNewStartMinute) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Shift to suggested time", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskPickerSheet(
    hour: Int,
    minute: Int = 0,
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onPick: (TaskEntity) -> Unit,
    onNewTask: () -> Unit
) {
    val hourLabel = hourMinuteLabel(hour, minute)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Schedule at $hourLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Create new task option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNewTask() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Create new task for this slot",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Or pick an unscheduled task (${tasks.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(task) },
                            shape = RoundedCornerShape(10.dp),
                            color = getQuadrantBgColor(task.quadrant),
                            border = BorderStroke(0.5.dp, getQuadrantTextColor(task.quadrant).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        task.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = getQuadrantTextColor(task.quadrant),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        task.quadrant.name.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = getQuadrantTextColor(task.quadrant).copy(alpha = 0.7f)
                                    )
                                }
                                if (task.estimatedDurationMinutes > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = getQuadrantTextColor(task.quadrant).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${task.estimatedDurationMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = getQuadrantTextColor(task.quadrant),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No unscheduled tasks found. Create a new one above!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    hour: Int,
    tasks: List<TaskEntity>,
    protectedSlots: List<ProtectedTimeSlot> = emptyList(),
    selectedDate: Long = 0L,
    isCurrentHour: Boolean,
    currentMinuteOfHour: Int?,
    isWorkHour: Boolean,
    onTaskClick: (String) -> Unit,
    onSlotClick: (minute: Int) -> Unit
) {
    val hourStartMinute = hour * MINUTES_PER_HOUR
    val hourEndMinute = hourStartMinute + MINUTES_PER_HOUR

    val protectedSegments = protectedSlots.mapNotNull { slot ->
        val slotStartMinute = slot.startMinuteOfDay
        val slotEndMinute = (slotStartMinute + slot.durationMinutes).coerceAtMost(MINUTES_PER_DAY)
        val overlapStart = maxOf(slotStartMinute, hourStartMinute)
        val overlapEnd = minOf(slotEndMinute, hourEndMinute)
        if (overlapEnd <= overlapStart) null
        else ProtectedSlotSegment(
            slot = slot,
            offsetMinutesInHour = overlapStart - hourStartMinute,
            overlapMinutes = overlapEnd - overlapStart,
            startsInThisHour = slotStartMinute in hourStartMinute until hourEndMinute,
            continuesFromPreviousHour = slotStartMinute < hourStartMinute,
            continuesToNextHour = slotEndMinute > hourEndMinute
        )
    }

    val segments = tasks.mapNotNull { task ->
        val taskStartMinute = taskStartMinuteOfDay(task)
        val taskEndMinute = (taskStartMinute + taskDurationMinutes(task)).coerceAtMost(MINUTES_PER_DAY)
        val overlapStart = maxOf(taskStartMinute, hourStartMinute)
        val overlapEnd = minOf(taskEndMinute, hourEndMinute)
        if (overlapEnd <= overlapStart) null
        else HourSegment(
            task = task,
            offsetMinutesInHour = overlapStart - hourStartMinute,
            overlapMinutes = overlapEnd - overlapStart,
            startsInThisHour = taskStartMinute in hourStartMinute until hourEndMinute,
            continuesFromPreviousHour = taskStartMinute < hourStartMinute,
            continuesToNextHour = taskEndMinute > hourEndMinute
        )
    }.sortedWith(compareBy<HourSegment>({ taskStartMinuteOfDay(it.task) }, { it.task.id }))

    val workHourBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
    val gutterWidth = 56.dp
    val halfHourLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    val indicatorColor = Color(0xFFE53935)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HOUR_SLOT_HEIGHT)
            .background(if (isWorkHour) workHourBg else Color.Transparent)
            .drawBehind {
                val gutterPx = gutterWidth.toPx()
                // Faint 30-minute guide line across the timeline
                val halfY = size.height * 0.5f
                drawLine(
                    color = halfHourLineColor,
                    start = Offset(gutterPx, halfY),
                    end = Offset(size.width, halfY),
                    strokeWidth = 0.5.dp.toPx()
                )

                // Google Calendar-style current time indicator line & circle
                if (isCurrentHour && currentMinuteOfHour != null) {
                    val yOffset = HOUR_SLOT_HEIGHT.toPx() * (currentMinuteOfHour.toFloat() / MINUTES_PER_HOUR.toFloat())
                    drawCircle(
                        color = indicatorColor,
                        radius = 4.dp.toPx(),
                        center = Offset(gutterPx, yOffset)
                    )
                    drawLine(
                        color = indicatorColor,
                        start = Offset(gutterPx, yOffset),
                        end = Offset(size.width, yOffset),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
    ) {
        // Hour label in gutter
        Box(
            modifier = Modifier
                .width(gutterWidth)
                .fillMaxHeight()
                .padding(start = 8.dp, top = 6.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = hourLabel(hour),
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrentHour) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
        }

        // Vertical divider between gutter and timeline
        HorizontalDivider(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )

        // Timeline content area
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
                .height(HOUR_SLOT_HEIGHT)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = (offset.y / size.height.toFloat()).coerceIn(0f, 0.99f)
                        val minute = (fraction * 4).toInt() * 15 // 0, 15, 30, 45
                        onSlotClick(minute)
                    }
                }
        ) {
            // Draw Protected Time Slots background overlays
            protectedSegments.forEach { pSeg ->
                val topOffset = HOUR_SLOT_HEIGHT * (pSeg.offsetMinutesInHour / MINUTES_PER_HOUR.toFloat())
                val rawHeight = HOUR_SLOT_HEIGHT * (pSeg.overlapMinutes / MINUTES_PER_HOUR.toFloat())
                val maxAvailableHeight = HOUR_SLOT_HEIGHT - topOffset
                val blockHeight = maxOf(rawHeight, MIN_SEGMENT_HEIGHT).coerceAtMost(maxAvailableHeight)
                val blockShape = when {
                    pSeg.continuesFromPreviousHour && pSeg.continuesToNextHour -> RoundedCornerShape(0.dp)
                    pSeg.continuesFromPreviousHour -> RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    pSeg.continuesToNextHour -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    else -> RoundedCornerShape(8.dp)
                }
                val categoryColor = getPtsCategoryColor(pSeg.slot.category)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = topOffset)
                        .height(blockHeight),
                    shape = blockShape,
                    color = categoryColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category color accent stripe on the left
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(categoryColor)
                        )

                        if (pSeg.startsInThisHour && blockHeight >= 18.dp) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${getPtsCategoryEmoji(pSeg.slot.category)} ${pSeg.slot.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PROTECTED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor
                                )
                            }
                        }
                    }
                }
            }

            if (segments.isEmpty() && protectedSegments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "+ Tap to schedule",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    segments.forEach { segment ->
                        val topOffset = HOUR_SLOT_HEIGHT * (segment.offsetMinutesInHour / MINUTES_PER_HOUR.toFloat())
                        val rawHeight = HOUR_SLOT_HEIGHT * (segment.overlapMinutes / MINUTES_PER_HOUR.toFloat())
                        val maxAvailableHeight = HOUR_SLOT_HEIGHT - topOffset
                        val blockHeight = maxOf(rawHeight, MIN_SEGMENT_HEIGHT).coerceAtMost(maxAvailableHeight)
                        val seamOverlap = 1.dp
                        val topLift = if (segment.continuesFromPreviousHour) seamOverlap else 0.dp
                        val bottomExtend = if (segment.continuesToNextHour) seamOverlap else 0.dp
                        val adjustedTopOffset = if (topOffset > topLift) topOffset - topLift else 0.dp
                        val adjustedHeight = (blockHeight + topLift + bottomExtend)
                            .coerceAtMost(HOUR_SLOT_HEIGHT - adjustedTopOffset)
                        val blockShape = when {
                            segment.continuesFromPreviousHour && segment.continuesToNextHour -> RoundedCornerShape(0.dp)
                            segment.continuesFromPreviousHour -> RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                            segment.continuesToNextHour -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            else -> RoundedCornerShape(8.dp)
                        }

                        // Check if segment conflicts with another task or a protected slot
                        val hasTaskConflict = segments.size > 1 && segments.any { other ->
                            other !== segment &&
                            maxOf(taskStartMinuteOfDay(segment.task), taskStartMinuteOfDay(other.task)) <
                            minOf(taskStartMinuteOfDay(segment.task) + taskDurationMinutes(segment.task),
                                  taskStartMinuteOfDay(other.task) + taskDurationMinutes(other.task))
                        }
                        val hasProtectedConflict = protectedSegments.any { pSeg ->
                            val segStart = taskStartMinuteOfDay(segment.task)
                            val segEnd = segStart + taskDurationMinutes(segment.task)
                            val pStart = pSeg.slot.startMinuteOfDay
                            val pEnd = pStart + pSeg.slot.durationMinutes
                            maxOf(segStart, pStart) < minOf(segEnd, pEnd)
                        }
                        val hasConflict = hasTaskConflict || hasProtectedConflict

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = adjustedTopOffset)
                                    .height(adjustedHeight)
                                    .clickable { onTaskClick(segment.task.id) },
                                shape = blockShape,
                                border = if (hasConflict) {
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                                } else {
                                    BorderStroke(0.5.dp, getQuadrantTextColor(segment.task.quadrant).copy(alpha = 0.3f))
                                },
                                shadowElevation = if (hasConflict) 2.dp else 1.dp,
                                color = if (hasConflict) {
                                    getQuadrantBgColor(segment.task.quadrant).copy(alpha = 0.9f)
                                } else {
                                    getQuadrantBgColor(segment.task.quadrant)
                                }
                            ) {
                                if (segment.startsInThisHour && adjustedHeight >= 20.dp) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = segment.task.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = getQuadrantTextColor(segment.task.quadrant),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            if (segment.task.isScheduleLocked) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.Lock,
                                                    contentDescription = "Locked",
                                                    tint = getQuadrantTextColor(segment.task.quadrant),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }

                                            if (hasConflict) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.Warning,
                                                    contentDescription = "Conflict",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }

                                            if (segment.task.isAutoScheduled) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                ) {
                                                    Text(
                                                        text = "AUTO",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = getQuadrantTextColor(segment.task.quadrant),
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (adjustedHeight >= 34.dp) {
                                            val startMinTotal = taskStartMinuteOfDay(segment.task)
                                            val endMinTotal = startMinTotal + taskDurationMinutes(segment.task)
                                            val startH = startMinTotal / 60
                                            val startM = startMinTotal % 60
                                            val endH = endMinTotal / 60
                                            val endM = endMinTotal % 60
                                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d–%02d:%02d", startH, startM, endH, endM)

                                            Text(
                                                text = if (hasConflict) "⚠️ $timeStr" else "$timeStr • ${segment.task.quadrant.name.replace("_", " ")}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (hasConflict) MaterialTheme.colorScheme.error else getQuadrantTextColor(segment.task.quadrant).copy(alpha = 0.75f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { onSlotClick(0) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add task in this hour",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun isTaskOnDate(task: TaskEntity, dayStart: Long, dayEnd: Long): Boolean {
    val scheduled = task.scheduledDate
    if (scheduled != null && scheduled in dayStart until dayEnd) return true
    val habit = task.habitDate
    if (task.recurrence != Recurrence.NONE && habit != null && habit in dayStart until dayEnd) return true
    return false
}

private fun taskStartMinuteOfDay(task: TaskEntity): Int {
    return task.timelineStartMinuteOfDay()
}

private fun taskDurationMinutes(task: TaskEntity): Int =
    if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 30

private fun hourLabel(hour: Int): String {
    return when {
        hour == 0  -> "12 AM"
        hour < 12  -> "$hour AM"
        hour == 12 -> "12 PM"
        else       -> "${hour - 12} PM"
    }
}

private fun hourMinuteLabel(hour: Int, minute: Int = 0): String {
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val ampm = if (hour < 12) "AM" else "PM"
    val minStr = minute.toString().padStart(2, '0')
    return "$h:$minStr $ampm"
}

private fun formatHourOnly(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}

private fun isToday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis }
    val cal2 = Calendar.getInstance()
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis }
    val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isTomorrow(millis: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis }
    val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
