package com.neuroflow.app.presentation.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.presentation.common.NewTaskSheet
import com.neuroflow.app.presentation.common.getQuadrantBgColor
import com.neuroflow.app.presentation.common.getQuadrantTextColor
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.filled.Refresh

private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR
private val HOUR_SLOT_HEIGHT = 60.dp
private val MIN_SEGMENT_HEIGHT = 8.dp

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
    onNavigateToFocus: (String) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = SimpleDateFormat("d - M - yyyy", Locale.getDefault())

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.autoScheduleResultMessage) {
        uiState.autoScheduleResultMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearAutoScheduleResultMessage()
        }
    }

    // null = closed, -1 = new task (FAB), 0..23 = slot tap
    var addSheetForHour by remember { mutableStateOf<Int?>(null) }
    var addSheetForMinute by remember { mutableStateOf(0) }
    // Task picker: which hour slot was tapped to pick an existing task
    var pickerHour by remember { mutableStateOf<Int?>(null) }
    var pickerMinute by remember { mutableStateOf(0) }
    var showProtectedSlotsSheet by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Time Blocking") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addSheetForHour = -1
                    addSheetForMinute = 0
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add task", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Date navigation + Auto-Schedule Controls
            Surface(
                color = Color(0xFFE3F2FD),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.previousDay() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                        }
                        Text(
                            text = dateFormat.format(Date(uiState.selectedDate)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.nextDay() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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

                        IconButton(
                            onClick = { showProtectedSlotsSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Protected Time Slots",
                                tint = if (uiState.protectedTimeSlots.any { it.isEnabled }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.isAutoScheduleOn) {
                            IconButton(
                                onClick = { viewModel.triggerAutoScheduleNow() },
                                enabled = !uiState.isAutoScheduling
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Re-schedule Now",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Locked tasks banner
            if (uiState.lockedTasks.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "🔒 LOCKED SCHEDULE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        uiState.lockedTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToFocus(task.id) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Lock, "Locked",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
            val isToday = isToday(uiState.selectedDate)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(HOURS_PER_DAY) { hour ->
                    TimelineRow(
                        hour = hour,
                        tasks = uiState.tasksForDay,
                        protectedSlots = uiState.protectedTimeSlots,
                        selectedDate = uiState.selectedDate,
                        isCurrentHour = isToday && hour == currentHour,
                        currentMinuteOfHour = if (isToday && hour == currentHour) currentMinute else null,
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
            onDismiss = { showProtectedSlotsSheet = false },
            onSaveSlot = { slot -> viewModel.addProtectedSlot(slot) },
            onDeleteSlot = { slotId -> viewModel.removeProtectedSlot(slotId) },
            onToggleSlot = { slotId -> viewModel.toggleProtectedSlot(slotId) }
        )
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
    val hourLabel = hourLabel(hour, minute)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Schedule task at $hourLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Create new task option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNewTask() },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create new task", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                }
            }

            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Or pick an existing task",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                tasks.forEach { task ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onPick(task) },
                        shape = RoundedCornerShape(8.dp),
                        color = getQuadrantBgColor(task.quadrant)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = getQuadrantTextColor(task.quadrant),
                                maxLines = 1
                            )
                            Text(
                                task.quadrant.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = getQuadrantTextColor(task.quadrant).copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No unscheduled tasks — create a new one above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
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

    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

    val activeProtectedSlotsToday = protectedSlots.filter { slot ->
        slot.isEnabled && when (slot.recurrence) {
            PtsRecurrence.DAILY -> true
            PtsRecurrence.WEEKDAYS -> dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
            PtsRecurrence.WEEKENDS -> dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            PtsRecurrence.CUSTOM -> (slot.weekdayBitmask and (1 shl (dayOfWeek - 1))) != 0
        }
    }

    val protectedSegments = activeProtectedSlotsToday.mapNotNull { slot ->
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

    val segments = tasks.map { task ->
        val taskStartMinute = taskStartMinuteOfDay(task)
        val taskEndMinute = (taskStartMinute + taskDurationMinutes(task)).coerceAtMost(MINUTES_PER_DAY)
        val overlapStart = maxOf(taskStartMinute, hourStartMinute)
        val overlapEnd = minOf(taskEndMinute, hourEndMinute)
        if (overlapEnd <= overlapStart) return@map null
        HourSegment(
            task = task,
            offsetMinutesInHour = overlapStart - hourStartMinute,
            overlapMinutes = overlapEnd - overlapStart,
            startsInThisHour = taskStartMinute in hourStartMinute until hourEndMinute,
            continuesFromPreviousHour = taskStartMinute < hourStartMinute,
            continuesToNextHour = taskEndMinute > hourEndMinute
        )
    }.filterNotNull()
        .sortedWith(compareBy<HourSegment>({ taskStartMinuteOfDay(it.task) }, { it.task.id }))

    val workHourBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(if (isWorkHour) workHourBg else Color.Transparent)
            .then(
                if (isCurrentHour && currentMinuteOfHour != null) Modifier.drawBehind {
                    val yOffset = 2.dp.toPx() + (HOUR_SLOT_HEIGHT.toPx() * (currentMinuteOfHour.toFloat() / MINUTES_PER_HOUR.toFloat()))
                    drawLine(
                        color = Color.Red,
                        start = Offset(0f, yOffset),
                        end = Offset(size.width, yOffset),
                        strokeWidth = 2.dp.toPx()
                    )
                } else Modifier
            )
    ) {
        // Hour label
        Text(
            text = hourLabel(hour),
            modifier = Modifier
                .width(56.dp)
                .padding(start = 8.dp, top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )

        // Divider line
        HorizontalDivider(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Task blocks — tapping empty space opens the picker with sub-hour precision
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = topOffset)
                        .height(blockHeight),
                    shape = blockShape,
                    color = getPtsCategoryColor(pSeg.slot.category).copy(alpha = 0.25f)
                ) {
                    if (pSeg.startsInThisHour && blockHeight >= 20.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${getPtsCategoryEmoji(pSeg.slot.category)} ${pSeg.slot.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (segments.isEmpty() && protectedSegments.isEmpty()) {
                Text(
                    "+ add",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
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

                        // Check if segment conflicts in time with another segment in the same hour
                        val hasTimeConflict = segments.size > 1 && segments.any { other ->
                            other !== segment &&
                            maxOf(taskStartMinuteOfDay(segment.task), taskStartMinuteOfDay(other.task)) <
                            minOf(taskStartMinuteOfDay(segment.task) + taskDurationMinutes(segment.task),
                                  taskStartMinuteOfDay(other.task) + taskDurationMinutes(other.task))
                        }

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
                                border = if (hasTimeConflict) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null,
                                color = if (hasTimeConflict) {
                                    getQuadrantBgColor(segment.task.quadrant).copy(alpha = 0.85f)
                                } else {
                                    getQuadrantBgColor(segment.task.quadrant)
                                }
                            ) {
                                if (segment.startsInThisHour && adjustedHeight >= 24.dp) {
                                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = segment.task.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = getQuadrantTextColor(segment.task.quadrant),
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (hasTimeConflict) {
                                                Spacer(modifier = Modifier.width(4.dp))
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
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        text = "AUTO",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = getQuadrantTextColor(segment.task.quadrant),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (adjustedHeight >= 36.dp) {
                                            val startMin = segment.offsetMinutesInHour
                                            val endMin = startMin + segment.overlapMinutes
                                            val timeRangeStr = String.format(Locale.getDefault(), "%02d:%02d–%02d:%02d", hour, startMin, hour + (endMin / 60), endMin % 60)
                                            Text(
                                                text = if (hasTimeConflict) "⚠️ $timeRangeStr" else segment.task.quadrant.name.replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (hasTimeConflict) MaterialTheme.colorScheme.error else getQuadrantTextColor(segment.task.quadrant).copy(alpha = 0.7f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "+ add",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clickable { onSlotClick(0) }
                        .padding(end = 2.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    }
}

private fun taskStartMinuteOfDay(task: TaskEntity): Int {
    return task.timelineStartMinuteOfDay()
}

private fun taskDurationMinutes(task: TaskEntity): Int = task.estimatedDurationMinutes.coerceAtLeast(1)

private fun hourLabel(hour: Int, minute: Int = 0): String {
    val minStr = if (minute > 0) ":${minute.toString().padStart(2, '0')}" else ""
    return when {
        hour == 0  -> "12$minStr am"
        hour < 12  -> "$hour$minStr am"
        hour == 12 -> "12$minStr pm"
        else       -> "${hour - 12}$minStr pm"
    }
}

private fun isToday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis }
    val cal2 = Calendar.getInstance()
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
