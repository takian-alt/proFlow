package com.neuroflow.app.presentation.v2.mindmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.GoalEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.scheduler.TagPreferredWindow
import com.neuroflow.app.domain.scheduler.TaskTagSchedulingProfile
import com.neuroflow.app.presentation.v2.composer.SMART_TAG_PRESETS
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalEditorSheet(
    existingGoal: GoalEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        targetDate: Long?,
        priority: Priority,
        tagsCsv: String,
        syncTagsToSteps: Boolean
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditing = existingGoal != null
    var title by remember(existingGoal?.id) { mutableStateOf(existingGoal?.title.orEmpty()) }
    var description by remember(existingGoal?.id) { mutableStateOf(existingGoal?.description.orEmpty()) }
    var priority by remember(existingGoal?.id) { mutableStateOf(existingGoal?.priority ?: Priority.MEDIUM) }
    var targetDate by remember(existingGoal?.id) { mutableStateOf(existingGoal?.targetDate) }
    var selectedTags by remember(existingGoal?.id) {
        mutableStateOf(
            existingGoal?.tags
                .orEmpty()
                .split(',')
                .map { it.trim().trimStart('#', '@') }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
        )
    }
    var customTagInput by remember { mutableStateOf("") }
    var syncTagsToExistingSteps by remember(existingGoal?.id) { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun addCustomTag() {
        val cleaned = customTagInput.trim().trimStart('#', '@')
        if (cleaned.isNotBlank() && selectedTags.none { it.equals(cleaned, ignoreCase = true) }) {
            selectedTags = selectedTags + cleaned
        }
        customTagInput = ""
    }

    val tagsCsv = selectedTags.joinToString(",")
    val autoScheduleHint = remember(tagsCsv, priority) {
        buildGoalAutoScheduleHint(priority = priority, tagsCsv = tagsCsv)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeuroFlowPaletteV2.SurfaceCard,
        contentColor = NeuroFlowThemeV2Colors.TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isEditing) "Edit Goal" else "New Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close goal editor",
                        tint = NeuroFlowThemeV2Colors.TextTertiary
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Goal title") },
                placeholder = { Text("e.g. Launch Portfolio, Exam Prep, Marathon") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_editor_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Why / notes (optional)") },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Priority selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Goal Priority",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(Priority.HIGH, "High", NeuroFlowThemeV2Colors.RoseCoral),
                        Triple(Priority.MEDIUM, "Medium", NeuroFlowThemeV2Colors.WarmAmber),
                        Triple(Priority.LOW, "Low", NeuroFlowPaletteV2.ReboundCyan)
                    ).forEach { (prio, label, color) ->
                        val selected = priority == prio
                        FilterChip(
                            selected = selected,
                            onClick = { priority = prio },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("goal_editor_priority_${prio.name.lowercase()}")
                        )
                    }
                }
            }

            // Tags section (quick scheduling presets + custom tag entry)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Goal Tags (flow to steps & guide Auto-Schedule)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )

                if (selectedTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectedTags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { selectedTags = selectedTags.filterNot { it.equals(tag, ignoreCase = true) } },
                                label = { Text("#$tag", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove $tag tag",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.heightIn(min = 36.dp)
                            )
                        }
                    }
                }

                // Quick-pick smart scheduling tags
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SMART_TAG_PRESETS.forEach { preset ->
                        val isSelected = selectedTags.any { it.equals(preset.tag, ignoreCase = true) }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTags = if (isSelected) {
                                    selectedTags.filterNot { it.equals(preset.tag, ignoreCase = true) }
                                } else {
                                    selectedTags + preset.tag
                                }
                            },
                            label = {
                                Text(
                                    "${preset.emoji} ${preset.tag}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("goal_tag_preset_${preset.tag}")
                        )
                    }
                }

                // Custom tag input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customTagInput,
                        onValueChange = { customTagInput = it },
                        placeholder = { Text("Add custom tag (e.g. thesis, fitness)...", fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addCustomTag() }),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_custom_tag_input")
                    )
                    FilledTonalIconButton(
                        onClick = { addCustomTag() },
                        enabled = customTagInput.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("goal_custom_tag_add_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add custom tag")
                    }
                }

                if (isEditing && selectedTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Also apply these tags to existing steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = syncTagsToExistingSteps,
                            onCheckedChange = { syncTagsToExistingSteps = it }
                        )
                    }
                }
            }

            // Live Auto-Schedule behavior preview
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = autoScheduleHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        lineHeight = 17.sp
                    )
                }
            }

            // Target horizon
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Target Horizon (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )
                val now = remember { System.currentTimeMillis() }
                val horizons = listOf(
                    "2 Wks" to (14L * 86_400_000L),
                    "1 Mo" to (30L * 86_400_000L),
                    "3 Mo" to (90L * 86_400_000L),
                    "6 Mo" to (180L * 86_400_000L),
                    "None" to null
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    horizons.forEach { (label, offset) ->
                        val isSelected = if (offset == null) {
                            targetDate == null
                        } else {
                            targetDate != null && kotlin.math.abs((targetDate!! - now) - offset) < 2L * 86_400_000L
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { targetDate = offset?.let { now + it } },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                }
            }

            // Save & Delete buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing && onDelete != null) {
                    if (confirmDelete) {
                        OutlinedButton(
                            onClick = { onDelete() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeuroFlowThemeV2Colors.RoseCoral),
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.RoseCoral),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Confirm Delete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        IconButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete goal",
                                tint = NeuroFlowThemeV2Colors.RoseCoral
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                title.trim(),
                                description.trim(),
                                targetDate,
                                priority,
                                tagsCsv,
                                isEditing && syncTagsToExistingSteps
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("goal_editor_save_btn")
                ) {
                    Text(
                        text = if (isEditing) "Save Goal" else "Create Goal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

internal fun buildGoalAutoScheduleHint(priority: Priority, tagsCsv: String): String {
    val profiles = TaskTagSchedulingProfile.profilesFor(tagsCsv)
    val windowLabel = when (profiles.firstOrNull()?.preferredWindow) {
        TagPreferredWindow.MORNING -> "Peak Morning focus blocks"
        TagPreferredWindow.MIDDAY -> "Midday plateau windows"
        TagPreferredWindow.EVENING -> "Afternoon / Evening rebound windows"
        else -> "best open energy-matched slots"
    }
    val boostLabel = when (priority) {
        Priority.HIGH -> "High-priority boost (+6 strategic score)"
        Priority.MEDIUM -> "Standard goal alignment boost"
        Priority.LOW -> "Flexible background progress"
    }
    return "Auto-Schedule: $boostLabel · routes steps into $windowLabel."
}
