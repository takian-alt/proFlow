package com.neuroflow.app.presentation.v2.composer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
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
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartTaskComposerSheetV2(
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

    // Strategic criteria & scheduling state
    var impactScore by remember { mutableStateOf(50f) }
    var effortScore by remember { mutableStateOf(50f) }
    var userScheduledDate by remember { mutableStateOf<Long?>(null) }
    var userScheduledTime by remember { mutableStateOf<Long?>(null) }
    var userDeadlineDate by remember { mutableStateOf<Long?>(null) }
    var userDeadlineTime by remember { mutableStateOf<Long?>(null) }
    var isScheduleLocked by remember { mutableStateOf(false) }

    var isExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var selectedQuadrant by remember { mutableStateOf(Quadrant.DO_FIRST) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf("deadline") }
    var timePickerTarget by remember { mutableStateOf("deadline") }

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
        containerColor = NeuroFlowThemeV2Colors.SlateSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Smart Task Composer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.TextPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NeuroFlowThemeV2Colors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // NLP Input Field
            OutlinedTextField(
                value = rawInput,
                onValueChange = {
                    rawInput = it
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
                        "e.g. Design core mockups 90m #high !impact tomorrow",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowThemeV2Colors.TextTertiary
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                    unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AI Slot Placement Preview Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🤖", fontSize = 16.sp)
                    Column {
                        val slotRecommendation = when (effectiveEnergy) {
                            EnergyLevel.HIGH -> "Recommended Slot: 09:30 AM (Peak Biological Alertness)"
                            EnergyLevel.MEDIUM -> "Recommended Slot: 02:00 PM (Focus Plateau)"
                            EnergyLevel.LOW -> "Recommended Slot: 05:00 PM (Recovery & Admin)"
                        }
                        Text(
                            text = slotRecommendation,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inferred Pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Duration Pill
                FilterChip(
                    selected = true,
                    onClick = {
                        val cycles = listOf(15, 25, 45, 60, 90, 120)
                        val nextIdx = (cycles.indexOf(effectiveDuration) + 1) % cycles.size
                        userDurationOverride = cycles[nextIdx]
                    },
                    label = { Text("⏱ ${effectiveDuration}m") },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Energy Pill
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
                        val label = when (effectiveEnergy) {
                            EnergyLevel.HIGH -> "⚡ High Energy"
                            EnergyLevel.MEDIUM -> "🔋 Med Energy"
                            EnergyLevel.LOW -> "☕ Low Energy"
                        }
                        Text(label)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Frog Pill
                FilterChip(
                    selected = effectiveFrog,
                    onClick = { userFrogOverride = !effectiveFrog },
                    label = { Text(if (effectiveFrog) "🐸 Frog (Crucial)" else "🐸 Normal") },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progressive Disclosure: Detailed Sliders & Calendar
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
                    text = if (isExpanded) "Hide Detailed Criteria" else "Detailed Criteria (Calendar, Impact, Schedule, Effort)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Deadline & Calendar
                    Text("📅 Deadline Date & Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    datePickerTarget = "deadline"
                                    showDatePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigo)
                                Text(
                                    text = if (effectiveDeadlineDate != null) SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(effectiveDeadlineDate)) else "Pick Due Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    timePickerTarget = "deadline"
                                    showTimePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigo)
                                Text(
                                    text = if (userDeadlineTime != null) {
                                        val h = (userDeadlineTime!! / 3_600_000L).toInt()
                                        val m = ((userDeadlineTime!! % 3_600_000L) / 60_000L).toInt()
                                        String.format("%02d:%02d", h, m)
                                    } else "Pick Due Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Strategic Impact Slider (0-100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⭐ Strategic Impact: ${impactScore.toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(if (impactScore >= 75) "Crucial" else "Standard", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.WarmAmber)
                    }
                    Slider(
                        value = impactScore,
                        onValueChange = { impactScore = it },
                        valueRange = 0f..100f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Effort Slider (0-100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("💪 Cognitive Effort: ${effortScore.toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(if (effortScore >= 70) "Deep Focus" else "Light", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.SageGreen)
                    }
                    Slider(
                        value = effortScore,
                        onValueChange = { effortScore = it },
                        valueRange = 0f..100f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Description / Notes
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes & Objectives") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = ::handleSave,
                enabled = rawInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Schedule Into Energy Stream", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // DatePickerDialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            if (datePickerTarget == "deadline") userDeadlineDate = selected
                            else userScheduledDate = selected
                        }
                        showDatePicker = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // TimePickerDialog
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(initialHour = 12, initialMinute = 0)
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Time") },
                text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = timePickerState.hour * 3_600_000L + timePickerState.minute * 60_000L
                        if (timePickerTarget == "deadline") userDeadlineTime = millis
                        else userScheduledTime = millis
                        showTimePicker = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            )
        }
    }
}
