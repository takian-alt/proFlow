package com.neuroflow.app.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickTaskModalBottomSheet(
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
    completedTasks: List<TaskEntity> = emptyList()
) {
    var rawInput by remember { mutableStateOf("") }
    var userDurationOverride by remember { mutableStateOf<Int?>(null) }
    var userEnergyOverride by remember { mutableStateOf<EnergyLevel?>(null) }
    var userTaskTypeOverride by remember { mutableStateOf<TaskType?>(null) }
    var userFrogOverride by remember { mutableStateOf<Boolean?>(null) }
    var userPriorityOverride by remember { mutableStateOf<Priority?>(null) }

    // Progressive disclosure state
    var isExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var selectedQuadrant by remember { mutableStateOf(Quadrant.DO_FIRST) }

    // Strategic criteria & scheduling state
    var impactScore by remember { mutableStateOf(50f) }
    var effortScore by remember { mutableStateOf(50f) }
    var userScheduledDate by remember { mutableStateOf<Long?>(null) }
    var userScheduledTime by remember { mutableStateOf<Long?>(null) }
    var userDeadlineDate by remember { mutableStateOf<Long?>(null) }
    var userDeadlineTime by remember { mutableStateOf<Long?>(null) }
    var isScheduleLocked by remember { mutableStateOf(false) }

    // Picker dialog states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf<String>("deadline") } // "deadline" or "scheduled"
    var timePickerTarget by remember { mutableStateOf<String>("deadline") }

    val inferred = remember(rawInput, completedTasks) {
        TaskInferenceEngine.inferFromInput(rawInput, completedTasks)
    }
    val effectiveDuration = userDurationOverride ?: inferred.durationMinutes
    val effectiveEnergy = userEnergyOverride ?: inferred.energyDemand
    val effectiveTaskType = userTaskTypeOverride ?: inferred.taskType
    val effectiveFrog = userFrogOverride ?: inferred.isFrog
    val effectivePriority = userPriorityOverride ?: inferred.priority
    val effectiveDeadlineDate = userDeadlineDate ?: inferred.deadlineDate
    val effectiveScheduledDate = userScheduledDate ?: inferred.scheduledDate

    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        delay(150)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun handleSave() {
        val title = inferred.sanitizedTitle.ifBlank { rawInput.trim() }
        if (title.isBlank()) return

        val task = TaskEntity(
            title = title,
            description = description.trim(),
            estimatedDurationMinutes = effectiveDuration,
            energyLevel = effectiveEnergy,
            taskType = effectiveTaskType,
            tags = inferred.contextTags.joinToString(","),
            isFrog = effectiveFrog,
            quadrant = selectedQuadrant,
            priority = effectivePriority,
            impactScore = impactScore.toInt(),
            effortScore = effortScore.toInt(),
            deadlineDate = effectiveDeadlineDate,
            deadlineTime = userDeadlineTime,
            scheduledDate = effectiveScheduledDate,
            scheduledTime = userScheduledTime,
            isScheduleLocked = isScheduleLocked
        )
        onSave(task)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Task Capture",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close sheet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Single Quick Capture Input Field
            OutlinedTextField(
                value = rawInput,
                onValueChange = {
                    rawInput = it
                    // Reset manual overrides if user edits text
                    userDurationOverride = null
                    userEnergyOverride = null
                    userTaskTypeOverride = null
                    userFrogOverride = null
                    userPriorityOverride = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        "e.g. Write grant proposal 90m @desk !frog",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Smart Inferred Pills with Progressive Tap Overrides
            Text(
                text = "Auto-Inferred Attributes (tap to adjust):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Duration Pill
                val isCalibrated = inferred.calibrationFactorApplied != 1.0f && userDurationOverride == null
                val durationLabel = if (isCalibrated) "⏱ ${effectiveDuration}m (calibrated)" else "⏱ ${effectiveDuration}m"
                FilterChip(
                    selected = true,
                    onClick = {
                        val cycles = listOf(15, 25, 45, 60, 90, 120)
                        val nextIdx = (cycles.indexOf(effectiveDuration) + 1) % cycles.size
                        userDurationOverride = cycles[nextIdx]
                    },
                    label = { Text(durationLabel) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { role = Role.Button }
                )

                // Energy Demand Pill
                FilterChip(
                    selected = true,
                    onClick = {
                        userEnergyOverride = when (effectiveEnergy) {
                            EnergyLevel.HIGH -> EnergyLevel.MEDIUM
                            EnergyLevel.MEDIUM -> EnergyLevel.LOW
                            EnergyLevel.LOW -> EnergyLevel.HIGH
                        }
                    },
                    label = {
                        val emoji = when (effectiveEnergy) {
                            EnergyLevel.HIGH -> "⚡ High"
                            EnergyLevel.MEDIUM -> "🔋 Med"
                            EnergyLevel.LOW -> "☕ Low"
                        }
                        Text(emoji)
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { role = Role.Button }
                )

                // Task Type Pill
                FilterChip(
                    selected = true,
                    onClick = {
                        userTaskTypeOverride = when (effectiveTaskType) {
                            TaskType.ANALYTICAL -> TaskType.CREATIVE
                            TaskType.CREATIVE -> TaskType.ADMIN
                            TaskType.ADMIN -> TaskType.PHYSICAL
                            TaskType.PHYSICAL -> TaskType.ANALYTICAL
                        }
                    },
                    label = {
                        val label = when (effectiveTaskType) {
                            TaskType.ANALYTICAL -> "🧠 Analytical"
                            TaskType.CREATIVE -> "🎨 Creative"
                            TaskType.ADMIN -> "📋 Admin"
                            TaskType.PHYSICAL -> "🏃 Physical"
                        }
                        Text(label)
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { role = Role.Button }
                )

                // Frog Priority Pill
                FilterChip(
                    selected = effectiveFrog,
                    onClick = {
                        userFrogOverride = !effectiveFrog
                    },
                    label = { Text(if (effectiveFrog) "🐸 Frog (Top Priority)" else "🐸 Normal") },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { role = Role.Button }
                )

                // Tags Pills
                inferred.contextTags.forEach { tag ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text("#$tag") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progressive Disclosure Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 8.dp)
                    .semantics { role = Role.Button },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Detailed Criteria" else "More Options (Calendar, Impact, Schedule, Quadrant)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse options" else "Expand options",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Deadline & Calendar Section
                    Text(
                        text = "📅 Deadline & Calendar",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (effectiveDeadlineDate != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (effectiveDeadlineDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    datePickerTarget = "deadline"
                                    showDatePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = if (effectiveDeadlineDate != null) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(effectiveDeadlineDate)) else "Due Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (effectiveDeadlineDate != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable { userDeadlineDate = -1L }, // clear sentinel
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Due Time Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (userDeadlineTime != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (userDeadlineTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    timePickerTarget = "deadline"
                                    showTimePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                val timeText = if (userDeadlineTime != null) {
                                    val hours = (userDeadlineTime!! / 3_600_000L).toInt()
                                    val minutes = ((userDeadlineTime!! % 3_600_000L) / 60_000L).toInt()
                                    String.format("%02d:%02d", hours, minutes)
                                } else "Due Time"
                                Text(text = timeText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                if (userDeadlineTime != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable { userDeadlineTime = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Schedule Date & Lock Section
                    Text(
                        text = "🗓 Schedule Slot & Lock",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (effectiveScheduledDate != null) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (effectiveScheduledDate != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    datePickerTarget = "scheduled"
                                    showDatePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = if (effectiveScheduledDate != null) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(effectiveScheduledDate)) else "Schedule Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (effectiveScheduledDate != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable { userScheduledDate = -1L },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Schedule Lock Toggle
                        FilterChip(
                            selected = isScheduleLocked,
                            onClick = { isScheduleLocked = !isScheduleLocked },
                            label = { Text(if (isScheduleLocked) "🔒 Locked" else "🔓 Flexible") },
                            leadingIcon = {
                                Icon(if (isScheduleLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Strategic Impact Slider (0-100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐ Strategic Impact: ${impactScore.toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (impactScore >= 80f) "Crucial / High Leverage" else if (impactScore >= 50f) "Moderate Impact" else "Low Leverage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = impactScore,
                        onValueChange = { impactScore = it },
                        valueRange = 0f..100f,
                        steps = 9
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Effort / Cognitive Demand Slider (0-100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💪 Effort & Complexity: ${effortScore.toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (effortScore >= 75f) "Deep Strain" else if (effortScore >= 40f) "Moderate Work" else "Quick Win",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = effortScore,
                        onValueChange = { effortScore = it },
                        valueRange = 0f..100f,
                        steps = 9
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description / Notes
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes / Description") },
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quadrant Picker
                    Text(
                        text = "Eisenhower Quadrant",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Quadrant.entries.forEach { q ->
                            FilterChip(
                                selected = selectedQuadrant == q,
                                onClick = { selectedQuadrant = q },
                                label = { Text(q.name.replace("_", " ")) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics { role = Role.Button }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Priority Picker
                    Text(
                        text = "Priority Level",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Priority.entries.forEach { p ->
                            FilterChip(
                                selected = effectivePriority == p,
                                onClick = { userPriorityOverride = p },
                                label = { Text(p.name) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics { role = Role.Button }
                            )
                        }
                    }
                }
            }

            // DatePickerDialog Component
            if (showDatePicker) {
                val currentTimestamp = if (datePickerTarget == "deadline") effectiveDeadlineDate else effectiveScheduledDate
                val initialPickerDate = if (currentTimestamp != null && currentTimestamp > 0L) currentTimestamp else System.currentTimeMillis()
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialPickerDate)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val selectedUtc = datePickerState.selectedDateMillis
                                if (selectedUtc != null) {
                                    val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = selectedUtc }
                                    val localCal = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.YEAR, utcCal.get(java.util.Calendar.YEAR))
                                        set(java.util.Calendar.MONTH, utcCal.get(java.util.Calendar.MONTH))
                                        set(java.util.Calendar.DAY_OF_MONTH, utcCal.get(java.util.Calendar.DAY_OF_MONTH))
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }
                                    if (datePickerTarget == "deadline") {
                                        userDeadlineDate = localCal.timeInMillis
                                    } else {
                                        userScheduledDate = localCal.timeInMillis
                                    }
                                }
                                showDatePicker = false
                            }
                        ) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // TimePickerDialog Component
            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(initialHour = 12, initialMinute = 0)
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text(if (timePickerTarget == "deadline") "Set Due Time" else "Set Schedule Time") },
                    text = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TimePicker(state = timePickerState)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val millis = timePickerState.hour * 3_600_000L + timePickerState.minute * 60_000L
                                if (timePickerTarget == "deadline") {
                                    userDeadlineTime = millis
                                } else {
                                    userScheduledTime = millis
                                }
                                showTimePicker = false
                            }
                        ) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Action Button
            Button(
                onClick = ::handleSave,
                enabled = rawInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { role = Role.Button },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Task", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
