package com.neuroflow.app.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NewTaskSheet(
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    editTask: TaskEntity? = null,
    prefilledQuadrant: Quadrant? = null,
    availableTasks: List<TaskEntity> = emptyList()
) {
    val flowViewModel: NewTaskFlowViewModel = hiltViewModel()
    val tagViewModel: TaskTagViewModel = hiltViewModel()

    val ui by flowViewModel.uiState.collectAsStateWithLifecycle()
    val prefs by flowViewModel.preferences.collectAsStateWithLifecycle()
    val catalogTags by tagViewModel.tags.collectAsStateWithLifecycle()

    val taskTags = remember(availableTasks) {
        availableTasks
            .flatMap { task -> task.tags.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
    val suggestedTags = remember(catalogTags, taskTags) {
        (catalogTags + taskTags)
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    // Keyed on the sheet identity only: the active-task list can grow while this sheet is open,
    // and re-initializing then would wipe the draft the user is typing.
    LaunchedEffect(editTask?.id, prefilledQuadrant) {
        flowViewModel.initialize(editTask, prefilledQuadrant, availableTasks)
    }

    val todayMidnight = remember { startOfLocalDay(System.currentTimeMillis()) }
    val tomorrowMidnight = remember {
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var isExpanded by remember { mutableStateOf(editTask != null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (editTask == null) {
            kotlinx.coroutines.delay(120)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val hasDraftContent = ui.title.isNotBlank() ||
        ui.description.isNotBlank() ||
        ui.tags.isNotEmpty() ||
        ui.waitingFor.isNotBlank() ||
        ui.stepByStepPlan.isNotBlank() ||
        ui.selectedDepIds.isNotEmpty() ||
        ui.deadlineDate != null ||
        ui.scheduledDate != null ||
        ui.habitDate != null

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var dateTarget by remember { mutableStateOf("scheduled") }
    var timeTarget by remember { mutableStateOf("scheduled") }

    fun requestDismiss() {
        if (hasDraftContent && editTask == null) {
            showDiscardDialog = true
        } else {
            flowViewModel.reset()
            onDismiss()
        }
    }

    fun handleSave() {
        if (ui.title.isBlank()) return
        tagViewModel.addTags(ui.tags)
        onSave(flowViewModel.buildTaskPayload(editTask))
        // The flow ViewModel outlives this dialog, so without a reset the next "New Task"
        // opens on the previous draft and Create re-submits it.
        flowViewModel.reset()
    }

    Dialog(onDismissRequest = { requestDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { requestDismiss() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                    Text(
                        text = if (ui.isEditing) "Edit Task" else "New Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { handleSave() },
                        enabled = ui.title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (ui.isEditing) "Save" else "Create")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Task Title Input
                    OutlinedTextField(
                        value = ui.title,
                        onValueChange = flowViewModel::updateTitle,
                        label = { Text("Task Title") },
                        placeholder = { Text("What needs to be done?") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuroFlowColors.Purple,
                            focusedLabelColor = NeuroFlowColors.Purple
                        )
                    )

                    // Quick Schedule Date Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "When",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val scheduledDay = ui.scheduledDate?.let(::startOfLocalDay)
                            val isToday = scheduledDay == todayMidnight
                            FilterChip(
                                selected = isToday,
                                onClick = {
                                    if (isToday) flowViewModel.updateScheduledDate(null)
                                    else flowViewModel.setDateToday()
                                },
                                label = { Text("Today") }
                            )

                            val isTomorrow = scheduledDay == tomorrowMidnight
                            FilterChip(
                                selected = isTomorrow,
                                onClick = {
                                    if (isTomorrow) flowViewModel.updateScheduledDate(null)
                                    else flowViewModel.setDateTomorrow()
                                },
                                label = { Text("Tomorrow") }
                            )

                            FilterChip(
                                selected = false,
                                onClick = { flowViewModel.setDateWeekend() },
                                label = { Text("This Weekend") }
                            )

                            val hasCustomDate = ui.scheduledDate != null && !isToday && !isTomorrow
                            FilterChip(
                                selected = hasCustomDate,
                                onClick = {
                                    dateTarget = "scheduled"
                                    showDatePicker = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = {
                                    Text(if (hasCustomDate) formatDate(ui.scheduledDate!!) else "Pick Date...")
                                }
                            )
                        }
                    }

                    // Quick Duration Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Estimated Duration",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                val selected = ui.estimatedDurationMinutes == mins
                                FilterChip(
                                    selected = selected,
                                    onClick = { flowViewModel.toggleQuickDuration(mins) },
                                    label = { Text("${mins}m") }
                                )
                            }
                        }
                    }

                    // Modifiers (Frog & High Impact)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Focus & Impact",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = ui.isFrog,
                                onClick = { flowViewModel.updateIsFrog(!ui.isFrog) },
                                label = { Text("🐸 Eat the Frog") }
                            )

                            val isHighImpact = ui.impactScore >= 85f
                            FilterChip(
                                selected = isHighImpact,
                                onClick = { flowViewModel.toggleHighImpact() },
                                label = { Text("⭐ High Impact") }
                            )
                        }
                    }

                    // Eisenhower Quadrants
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Eisenhower Quadrant",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Quadrant.entries.forEach { q ->
                                FilterChip(
                                    selected = ui.quadrant == q,
                                    onClick = { flowViewModel.updateQuadrant(q) },
                                    label = { Text(quadrantDisplayName(q)) }
                                )
                            }
                        }
                    }

                    // Primary CTA Save Button
                    Button(
                        onClick = { handleSave() },
                        enabled = ui.title.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (ui.isEditing) "Save Changes" else "Create Task",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Collapsible "More Details & Options" Accordion
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "More Details & Options",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (ui.description.isNotBlank() || ui.tags.isNotEmpty() || ui.recurrence != Recurrence.NONE || ui.deadlineDate != null) {
                                    Surface(
                                        shape = CircleShape,
                                        color = NeuroFlowColors.Purple.copy(alpha = 0.2f)
                                    ) {
                                        Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(
                                                "Configured",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeuroFlowColors.Purple
                                            )
                                        }
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Description Field
                            OutlinedTextField(
                                value = ui.description,
                                onValueChange = flowViewModel::updateDescription,
                                label = { Text("Description & Notes") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeuroFlowColors.Purple,
                                    focusedLabelColor = NeuroFlowColors.Purple
                                )
                            )

                            // Priority, Energy & Task Type
                            Text("Priority & Energy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            DropdownField(
                                label = "Priority",
                                value = priorityDisplayName(ui.priority),
                                items = Priority.entries,
                                itemLabel = ::priorityDisplayName,
                                onSelected = flowViewModel::updatePriority
                            )

                            Text("Energy Level", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EnergyLevel.entries.forEach { level ->
                                    FilterChip(
                                        selected = ui.energyLevel == level,
                                        onClick = { flowViewModel.updateEnergyLevel(level) },
                                        label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }

                            Text("Task Type", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TaskType.entries.forEach { type ->
                                    FilterChip(
                                        selected = ui.taskType == type,
                                        onClick = { flowViewModel.updateTaskType(type) },
                                        label = { Text(type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }

                            // Timing & Reminders
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            TimingStep(
                                recurrence = ui.recurrence,
                                customIntervalDays = ui.customIntervalDays,
                                deadlineDate = ui.deadlineDate,
                                deadlineTime = ui.deadlineTime,
                                scheduledDate = ui.scheduledDate,
                                scheduledTime = ui.scheduledTime,
                                habitDate = ui.habitDate,
                                habitTime = ui.habitTime,
                                reminderFlags = ui.reminderFlags,
                                isScheduleLocked = ui.isScheduleLocked,
                                isAutoScheduleMode = ui.isAutoScheduleMode,
                                onRecurrenceChange = flowViewModel::updateRecurrence,
                                onCustomIntervalChange = flowViewModel::updateCustomIntervalDays,
                                onDeadlineDateClick = { dateTarget = "deadline"; showDatePicker = true },
                                onDeadlineDateClear = { flowViewModel.updateDeadlineDate(null) },
                                onDeadlineTimeClick = { timeTarget = "deadline"; showTimePicker = true },
                                onDeadlineTimeClear = { flowViewModel.updateDeadlineTime(null) },
                                onScheduledDateClick = { dateTarget = "scheduled"; showDatePicker = true },
                                onScheduledDateClear = { flowViewModel.updateScheduledDate(null) },
                                onScheduledTimeClick = { timeTarget = "scheduled"; showTimePicker = true },
                                onScheduledTimeClear = { flowViewModel.updateScheduledTime(null) },
                                onHabitDateClick = { dateTarget = "habit"; showDatePicker = true },
                                onHabitDateClear = { flowViewModel.updateHabitDate(null) },
                                onHabitTimeClick = { timeTarget = "habit"; showTimePicker = true },
                                onHabitTimeClear = { flowViewModel.updateHabitTime(null) },
                                onReminderFlagsChange = flowViewModel::updateReminderFlags,
                                onScheduleLockedChange = flowViewModel::updateScheduleLocked
                            )

                            // Effort & Strategic Sliders
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            EffortStep(
                                estimatedDurationMinutes = ui.estimatedDurationMinutes,
                                impactScore = ui.impactScore,
                                valueScore = ui.valueScore,
                                effortScore = ui.effortScore,
                                enjoymentScore = ui.enjoymentScore,
                                contextTag = ui.contextTag,
                                isPublicCommitment = ui.isPublicCommitment,
                                isAnxietyTask = ui.isAnxietyTask,
                                goalRiskLevel = ui.goalRiskLevel,
                                includeExecutionStep = ui.includeExecutionStep,
                                onDurationChange = flowViewModel::updateEstimatedDuration,
                                onImpactChange = flowViewModel::updateImpact,
                                onValueChange = flowViewModel::updateValue,
                                onEffortChange = flowViewModel::updateEffort,
                                onEnjoymentChange = flowViewModel::updateEnjoyment,
                                onContextChange = flowViewModel::updateContextTag,
                                onPublicCommitmentChange = flowViewModel::updatePublicCommitment,
                                onAnxietyChange = flowViewModel::updateAnxietyTask,
                                onGoalRiskChange = flowViewModel::updateGoalRiskLevel,
                                onIncludeExecutionChange = flowViewModel::setIncludeExecutionStep
                            )

                            // Execution & Dependencies — revealed by the toggle in EffortStep
                            val hasExecutionContent = ui.waitingFor.isNotBlank() ||
                                ui.stepByStepPlan.isNotBlank() ||
                                ui.selectedDepIds.isNotEmpty()
                            if (ui.includeExecutionStep || hasExecutionContent) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ExecutionStep(
                                    waitingFor = ui.waitingFor,
                                    stepByStepPlan = ui.stepByStepPlan,
                                    selectedDepIds = ui.selectedDepIds,
                                    availableTasks = availableTasks,
                                    editTaskId = editTask?.id,
                                    onWaitingForChange = flowViewModel::updateWaitingFor,
                                    onPlanChange = flowViewModel::updatePlan,
                                    onToggleDependency = flowViewModel::toggleDependency
                                )
                            }

                            // Tags & Context Taxonomy
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            TagsStep(
                                selectedTags = ui.tags,
                                suggestedTags = suggestedTags,
                                onAddTag = flowViewModel::addTag,
                                onToggleTag = flowViewModel::toggleTag,
                                onRemoveTag = flowViewModel::removeTag
                            )

                            // Auto Scheduling Review
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ReviewStep(
                                uiState = ui,
                                autoSchedulePreview = flowViewModel.autoSchedulePreview(),
                                autoSchedulingEnabled = prefs.autoSchedulingEnabled
                            )

                            // Save CTA inside accordion bottom
                            Button(
                                onClick = { handleSave() },
                                enabled = ui.title.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (ui.isEditing) "Save Changes" else "Create Task",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard task draft?") },
            text = { Text("You have unsaved changes. Keep editing or discard this draft.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        flowViewModel.reset()
                        onDismiss()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    if (showDatePicker) {
        val currentDateVal = when (dateTarget) {
            "scheduled" -> ui.scheduledDate
            "habit" -> ui.habitDate
            else -> ui.deadlineDate
        }
        val initial = currentDateVal ?: System.currentTimeMillis()
        androidx.compose.runtime.key(dateTarget, currentDateVal) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (currentDateVal != null) {
                            TextButton(onClick = {
                                when (dateTarget) {
                                    "scheduled" -> flowViewModel.updateScheduledDate(null)
                                    "habit" -> flowViewModel.updateHabitDate(null)
                                    else -> flowViewModel.updateDeadlineDate(null)
                                }
                                showDatePicker = false
                            }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                        }
                        TextButton(onClick = {
                            val selected = datePickerState.selectedDateMillis?.let(::utcMidnightToLocalMidnight)
                            when (dateTarget) {
                                "scheduled" -> flowViewModel.updateScheduledDate(selected)
                                "habit" -> flowViewModel.updateHabitDate(selected)
                                else -> flowViewModel.updateDeadlineDate(selected)
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) { DatePicker(state = datePickerState) }
        }
    }

    if (showTimePicker) {
        val currentTimeVal = when (timeTarget) {
            "scheduled" -> ui.scheduledTime
            "habit" -> ui.habitTime
            else -> ui.deadlineTime
        }
        val initialMillis = currentTimeVal ?: 0L
        androidx.compose.runtime.key(timeTarget, currentTimeVal) {
            val timePickerState = rememberTimePickerState(
                initialHour = (initialMillis / 3_600_000L).toInt().coerceIn(0, 23),
                initialMinute = ((initialMillis % 3_600_000L) / 60_000L).toInt().coerceIn(0, 59)
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Time") },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (currentTimeVal != null) {
                            TextButton(onClick = {
                                when (timeTarget) {
                                    "scheduled" -> flowViewModel.updateScheduledTime(null)
                                    "habit" -> flowViewModel.updateHabitTime(null)
                                    else -> flowViewModel.updateDeadlineTime(null)
                                }
                                showTimePicker = false
                            }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                        }
                        TextButton(onClick = {
                            val selected = timePickerState.hour * 3_600_000L + timePickerState.minute * 60_000L
                            when (timeTarget) {
                                "scheduled" -> flowViewModel.updateScheduledTime(selected)
                                "habit" -> flowViewModel.updateHabitTime(selected)
                                else -> flowViewModel.updateDeadlineTime(selected)
                            }
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BasicInfoStep(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Basic Info", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Task Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeuroFlowColors.Purple,
                focusedLabelColor = NeuroFlowColors.Purple
            )
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeuroFlowColors.Purple,
                focusedLabelColor = NeuroFlowColors.Purple
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassificationStep(
    quadrant: Quadrant,
    priority: Priority,
    energyLevel: EnergyLevel,
    taskType: TaskType,
    isFrog: Boolean,
    onQuadrantChange: (Quadrant) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onEnergyChange: (EnergyLevel) -> Unit,
    onTaskTypeChange: (TaskType) -> Unit,
    onFrogChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Classification", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        DropdownField(
            label = "Quadrant",
            value = quadrantDisplayName(quadrant),
            items = Quadrant.entries,
            itemLabel = ::quadrantDisplayName,
            onSelected = onQuadrantChange
        )
        DropdownField(
            label = "Priority",
            value = priorityDisplayName(priority),
            items = Priority.entries,
            itemLabel = ::priorityDisplayName,
            onSelected = onPriorityChange
        )

        Text("Energy level", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyLevel.entries.forEach { level ->
                FilterChip(
                    selected = energyLevel == level,
                    onClick = { onEnergyChange(level) },
                    label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Text("Task type", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskType.entries.forEach { type ->
                FilterChip(
                    selected = taskType == type,
                    onClick = { onTaskTypeChange(type) },
                    label = { Text(type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = isFrog,
                onCheckedChange = onFrogChange,
                colors = SwitchDefaults.colors(checkedTrackColor = NeuroFlowColors.Purple)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mark as frog task")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimingStep(
    recurrence: Recurrence,
    customIntervalDays: Int,
    deadlineDate: Long?,
    deadlineTime: Long?,
    scheduledDate: Long?,
    scheduledTime: Long?,
    habitDate: Long?,
    habitTime: Long?,
    reminderFlags: Int,
    isScheduleLocked: Boolean,
    isAutoScheduleMode: Boolean,
    onRecurrenceChange: (Recurrence) -> Unit,
    onCustomIntervalChange: (Int) -> Unit,
    onDeadlineDateClick: () -> Unit,
    onDeadlineDateClear: () -> Unit,
    onDeadlineTimeClick: () -> Unit,
    onDeadlineTimeClear: () -> Unit,
    onScheduledDateClick: () -> Unit,
    onScheduledDateClear: () -> Unit,
    onScheduledTimeClick: () -> Unit,
    onScheduledTimeClear: () -> Unit,
    onHabitDateClick: () -> Unit,
    onHabitDateClear: () -> Unit,
    onHabitTimeClick: () -> Unit,
    onHabitTimeClear: () -> Unit,
    onReminderFlagsChange: (Int) -> Unit,
    onScheduleLockedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Timing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        DropdownField(
            label = "Recurrence",
            value = recurrenceDisplayName(recurrence),
            items = Recurrence.entries,
            itemLabel = ::recurrenceDisplayName,
            onSelected = onRecurrenceChange
        )

        if (recurrence == Recurrence.CUSTOM) {
            OutlinedTextField(
                value = customIntervalDays.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let(onCustomIntervalChange)
                },
                label = { Text("Custom interval days") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (recurrence == Recurrence.NONE) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isAutoScheduleMode) NeuroFlowColors.Purple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (isAutoScheduleMode) NeuroFlowColors.Purple.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ ", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Auto-Schedule Optimal Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (isAutoScheduleMode)
                                "Auto-scheduler will automatically place this task into your best energy window."
                            else
                                "Manual schedule set. Turn ON to let AI pick the best time slot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAutoScheduleMode,
                        onCheckedChange = { enableAuto ->
                            if (enableAuto) {
                                onScheduledDateClear()
                                onScheduledTimeClear()
                                onScheduleLockedChange(false)
                            } else {
                                onScheduleLockedChange(true)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = NeuroFlowColors.Purple)
                    )
                }
            }

            Text("Deadline", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateChip(deadlineDate, "Date", onDeadlineDateClick, onClear = onDeadlineDateClear)
                TimeChip(deadlineTime, "Time", onDeadlineTimeClick, onClear = onDeadlineTimeClear)
            }

            Text(
                text = if (isAutoScheduleMode && (scheduledDate != null || scheduledTime != null))
                    "Auto-scheduled time (editing it makes the schedule manual)"
                else
                    "Manual Scheduled Time (Optional)",
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateChip(scheduledDate, "Date", onScheduledDateClick, onClear = onScheduledDateClear)
                TimeChip(scheduledTime, "Time", onScheduledTimeClick, onClear = onScheduledTimeClear)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isScheduleLocked,
                    onCheckedChange = onScheduleLockedChange
                )
                Text("Lock manual schedule")
            }
        } else {
            Text("Recurring start", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateChip(habitDate, "Start date", onHabitDateClick, onClear = onHabitDateClear)
                TimeChip(habitTime, "Start time", onHabitTimeClick, onClear = onHabitTimeClear)
            }
        }

        Text("Reminders", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderChip("15 min", 1, reminderFlags, onReminderFlagsChange)
            ReminderChip("30 min", 2, reminderFlags, onReminderFlagsChange)
            ReminderChip("1 hour", 4, reminderFlags, onReminderFlagsChange)
            ReminderChip("1 day", 8, reminderFlags, onReminderFlagsChange)
        }
    }
}

@Composable
private fun RecurrenceStep(
    recurrence: Recurrence,
    customIntervalDays: Int,
    habitDate: Long?,
    habitTime: Long?,
    onHabitDateClick: () -> Unit,
    onHabitDateClear: () -> Unit,
    onHabitTimeClick: () -> Unit,
    onHabitTimeClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Recurrence Anchor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Pick the first occurrence. Anchor is timezone-safe and used for all recurrence cycles.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Pattern: ${recurrenceDisplayName(recurrence)}${if (recurrence == Recurrence.CUSTOM) " every $customIntervalDays days" else ""}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateChip(habitDate, "Start date", onHabitDateClick, onClear = onHabitDateClear)
            TimeChip(habitTime, "Start time", onHabitTimeClick, onClear = onHabitTimeClear)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffortStep(
    estimatedDurationMinutes: Int,
    impactScore: Float,
    valueScore: Float,
    effortScore: Float,
    enjoymentScore: Float,
    contextTag: String,
    isPublicCommitment: Boolean,
    isAnxietyTask: Boolean,
    goalRiskLevel: Int,
    includeExecutionStep: Boolean,
    onDurationChange: (Int) -> Unit,
    onImpactChange: (Float) -> Unit,
    onValueChange: (Float) -> Unit,
    onEffortChange: (Float) -> Unit,
    onEnjoymentChange: (Float) -> Unit,
    onContextChange: (String) -> Unit,
    onPublicCommitmentChange: (Boolean) -> Unit,
    onAnxietyChange: (Boolean) -> Unit,
    onGoalRiskChange: (Int) -> Unit,
    onIncludeExecutionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Effort + Estimate", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        DropdownField(
            label = "Estimated duration",
            value = durationDisplayName(estimatedDurationMinutes),
            items = durationOptions,
            itemLabel = { it.first },
            onSelected = { onDurationChange(it.second) }
        )

        SliderField("Strategic impact", impactScore, onImpactChange)
        SliderField("Intrinsic value", valueScore, onValueChange)
        SliderField("Effort required", effortScore, onEffortChange)
        SliderField("Enjoyment", enjoymentScore, onEnjoymentChange)

        Text("Context", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("@work", "@home", "@phone", "@computer", "@errands").forEach { tag ->
                FilterChip(
                    selected = contextTag == tag,
                    onClick = { onContextChange(if (contextTag == tag) "" else tag) },
                    label = { Text(tag) }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = isPublicCommitment, onCheckedChange = onPublicCommitmentChange)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Public commitment")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = isAnxietyTask, onCheckedChange = onAnxietyChange)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Anxiety task")
        }

        Text("Goal risk", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "None", 1 to "At Risk", 2 to "Critical").forEach { (level, label) ->
                FilterChip(
                    selected = goalRiskLevel == level,
                    onClick = { onGoalRiskChange(level) },
                    label = { Text(label) }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = includeExecutionStep, onCheckedChange = onIncludeExecutionChange)
            Text("Add execution/dependency details")
        }
    }
}

@Composable
private fun ExecutionStep(
    waitingFor: String,
    stepByStepPlan: String,
    selectedDepIds: Set<String>,
    availableTasks: List<TaskEntity>,
    editTaskId: String?,
    onWaitingForChange: (String) -> Unit,
    onPlanChange: (String) -> Unit,
    onToggleDependency: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val candidates = remember(availableTasks, editTaskId) { availableTasks.filter { it.id != editTaskId } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Execution", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = waitingFor,
            onValueChange = onWaitingForChange,
            label = { Text("Waiting for") },
            placeholder = { Text("External dependency") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = stepByStepPlan,
            onValueChange = onPlanChange,
            label = { Text("Step-by-step plan") },
            placeholder = { Text("One step per line") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Dependencies", style = MaterialTheme.typography.labelLarge)
        if (candidates.isEmpty()) {
            Text("No other active tasks available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(candidates, key = { it.id }) { task ->
                    val selected = selectedDepIds.contains(task.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDependency(task.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onToggleDependency(task.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(task.title)
                            Text(
                                "${task.quadrant.name.replace('_', ' ')} · ${task.priority.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsStep(
    selectedTags: List<String>,
    suggestedTags: List<String>,
    onAddTag: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newTag by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tags & Context", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Tags auto-match energy profiles, peak windows, and context to place your task at the ideal time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Custom Tag Input Field
        OutlinedTextField(
            value = newTag,
            onValueChange = { newTag = it },
            label = { Text("Add custom tag (e.g. #coding, @home)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeuroFlowColors.Purple,
                focusedLabelColor = NeuroFlowColors.Purple
            ),
            trailingIcon = {
                if (newTag.isNotBlank()) {
                    IconButton(onClick = {
                        onAddTag(newTag)
                        newTag = ""
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Tag", tint = NeuroFlowColors.Purple)
                    }
                }
            }
        )

        // Active / Selected Tags Container
        if (selectedTags.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Selected Tags (${selectedTags.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedTags.forEach { tag ->
                        val displayTag = if (tag.startsWith("#") || tag.startsWith("@")) tag else "#$tag"
                        InputChip(
                            selected = true,
                            onClick = { onRemoveTag(tag) },
                            label = { Text(displayTag, fontWeight = FontWeight.Medium) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeuroFlowColors.Purple.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        // Categorized Suggested Taxonomy
        Text("Popular Taxonomy & Energy Profiles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        val taxonomyGroups = remember(suggestedTags) {
            listOf(
                "🧠 High Focus & Deep Work" to listOf("deep work", "coding", "study", "learning", "writing", "finance"),
                "🏃 Health & Wellness" to listOf("exercise", "workout", "gym", "mindfulness", "meditation", "yoga"),
                "💻 Work & Admin" to listOf("admin", "meetings", "planning", "review", "@computer", "@work"),
                "🏡 Life & Errands" to listOf("chores", "household", "errands", "physical", "@home", "@errands")
            )
        }

        taxonomyGroups.forEach { (groupTitle, groupTags) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(groupTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    groupTags.forEach { tag ->
                        val isSelected = selectedTags.any { it.equals(tag, ignoreCase = true) }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleTag(tag) },
                            label = { Text(tag) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        val otherSuggested = suggestedTags.filterNot { tag ->
            taxonomyGroups.any { (_, group) -> group.any { g -> g.equals(tag, ignoreCase = true) } }
        }
        if (otherSuggested.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("More Suggested", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    otherSuggested.forEach { tag ->
                        val isSelected = selectedTags.any { it.equals(tag, ignoreCase = true) }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleTag(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(
    uiState: NewTaskFlowUiState,
    autoSchedulePreview: AutoSchedulePreview,
    autoSchedulingEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        ReviewItem("Title", uiState.title)
        if (uiState.description.isNotBlank()) ReviewItem("Description", uiState.description)
        ReviewItem("Quadrant", quadrantDisplayName(uiState.quadrant))
        ReviewItem("Priority", priorityDisplayName(uiState.priority))
        ReviewItem("Recurrence", recurrenceDisplayName(uiState.recurrence))
        if (uiState.recurrence != Recurrence.NONE) {
            ReviewItem(
                "Anchor",
                "${uiState.habitDate?.let(::formatDate) ?: "Not set"} ${uiState.habitTime?.let(::formatTime) ?: ""}".trim()
            )
        }
        if (uiState.recurrence == Recurrence.NONE) {
            ReviewItem(
                "Manual schedule",
                listOfNotNull(
                    uiState.scheduledDate?.let(::formatDate),
                    uiState.scheduledTime?.let(::formatTime)
                ).joinToString(" ").ifBlank { "Not set" }
            )
        }
        ReviewItem("Tags", if (uiState.tags.isEmpty()) "None" else uiState.tags.joinToString(", "))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            color = if (autoSchedulePreview.willAutoSchedule) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (autoSchedulePreview.willAutoSchedule) "Auto-scheduling preview" else "Auto-scheduling not applied",
                    fontWeight = FontWeight.SemiBold
                )
                Text(autoSchedulePreview.reason)
                if (autoSchedulingEnabled && autoSchedulePreview.willAutoSchedule && uiState.recurrence == Recurrence.NONE && uiState.scheduledDate == null && uiState.scheduledTime == null) {
                    Text(
                        "Task has no manual schedule; scheduler will attempt placement when worker runs.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    value: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SliderField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label (${value.toInt()})", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = NeuroFlowColors.Purple,
                activeTrackColor = NeuroFlowColors.Purple
            )
        )
    }
}

@Composable
private fun DateChip(
    dateMillis: Long?,
    placeholder: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (dateMillis != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (dateMillis != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = "Date",
                modifier = Modifier.size(16.dp),
                tint = if (dateMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (dateMillis != null) formatDate(dateMillis) else placeholder,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (dateMillis != null) FontWeight.SemiBold else FontWeight.Normal
            )
            if (dateMillis != null && onClear != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear Date",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeChip(
    timeMillis: Long?,
    placeholder: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (timeMillis != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (timeMillis != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = "Time",
                modifier = Modifier.size(16.dp),
                tint = if (timeMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (timeMillis != null) formatTime(timeMillis) else placeholder,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (timeMillis != null) FontWeight.SemiBold else FontWeight.Normal
            )
            if (timeMillis != null && onClear != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear Time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderChip(
    label: String,
    flag: Int,
    currentFlags: Int,
    onFlagsChange: (Int) -> Unit
) {
    val selected = (currentFlags and flag) != 0
    FilterChip(
        selected = selected,
        onClick = {
            onFlagsChange(if (selected) currentFlags xor flag else currentFlags or flag)
        },
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null
    )
}

private fun quadrantDisplayName(q: Quadrant) = when (q) {
    Quadrant.DO_FIRST -> "Q1: Do First"
    Quadrant.SCHEDULE -> "Q2: Schedule"
    Quadrant.DELEGATE -> "Q3: Delegate"
    Quadrant.ELIMINATE -> "Q4: On Hold"
}

private fun priorityDisplayName(p: Priority) = when (p) {
    Priority.HIGH -> "High"
    Priority.MEDIUM -> "Medium"
    Priority.LOW -> "Low"
}

private fun recurrenceDisplayName(r: Recurrence) = when (r) {
    Recurrence.NONE -> "No Repeat"
    Recurrence.DAILY -> "Daily"
    Recurrence.WEEKLY -> "Weekly"
    Recurrence.MONTHLY -> "Monthly"
    Recurrence.CUSTOM -> "Custom"
}

private val durationOptions = listOf(
    "0 Mins" to 0,
    "1 Min" to 1,
    "5 Mins" to 5,
    "15 Mins" to 15,
    "30 Mins" to 30,
    "45 Mins" to 45,
    "1 Hour" to 60,
    "1.5 Hours" to 90,
    "2 Hours" to 120,
    "3 Hours" to 180,
    "4 Hours" to 240,
    "6 Hours" to 360,
    "8 Hours" to 480,
    "10 Hours" to 600
)

private fun durationDisplayName(minutes: Int): String = durationOptions
    .firstOrNull { it.second == minutes }
    ?.first
    ?: "$minutes Mins"

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatTime(millis: Long): String {
    val hours = (millis / 3_600_000L).toInt()
    val minutes = ((millis % 3_600_000L) / 60_000L).toInt()
    val amPm = if (hours < 12) "AM" else "PM"
    val displayHour = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours
    return String.format("%d:%02d %s", displayHour, minutes, amPm)
}

private fun utcMidnightToLocalMidnight(utcMillis: Long): Long {
    val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utcCal.timeInMillis = utcMillis

    val localCal = java.util.Calendar.getInstance()
    localCal.set(java.util.Calendar.YEAR, utcCal.get(java.util.Calendar.YEAR))
    localCal.set(java.util.Calendar.MONTH, utcCal.get(java.util.Calendar.MONTH))
    localCal.set(java.util.Calendar.DAY_OF_MONTH, utcCal.get(java.util.Calendar.DAY_OF_MONTH))
    localCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    localCal.set(java.util.Calendar.MINUTE, 0)
    localCal.set(java.util.Calendar.SECOND, 0)
    localCal.set(java.util.Calendar.MILLISECOND, 0)
    return localCal.timeInMillis
}
