package com.neuroflow.app.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val inferred = remember(rawInput, completedTasks) {
        TaskInferenceEngine.inferFromInput(rawInput, completedTasks)
    }
    val effectiveDuration = userDurationOverride ?: inferred.durationMinutes
    val effectiveEnergy = userEnergyOverride ?: inferred.energyDemand
    val effectiveTaskType = userTaskTypeOverride ?: inferred.taskType
    val effectiveFrog = userFrogOverride ?: inferred.isFrog
    val effectivePriority = userPriorityOverride ?: inferred.priority

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
            priority = effectivePriority
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
                    text = if (isExpanded) "Hide Advanced Options" else "More Options (Quadrant, Notes)",
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
