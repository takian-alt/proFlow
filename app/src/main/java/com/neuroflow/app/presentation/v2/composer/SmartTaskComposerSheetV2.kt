package com.neuroflow.app.presentation.v2.composer

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.presentation.v2.theme.LocalNeuroFlowColors
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface SheetMode {
    data object Create : SheetMode
    data class Edit(val task: TaskEntity) : SheetMode
}

data class ComposerQuadrantOption(
    val quadrant: Quadrant,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

data class ComposerTagPreset(
    val tag: String,
    val emoji: String,
    val shortBoost: String,
    val category: String,
    val energyDemand: String,
    val biologicalWindow: String,
    val boostDescription: String,
    val fragmentationTolerance: String
)

val SMART_TAG_PRESETS: List<ComposerTagPreset> = listOf(
    ComposerTagPreset("deepwork", "⚡", "+8.5pt Batching", "Analytical", "High (80)", "Peak Morning", "+8.5pt Attentional Batching, places in 90m peak focus window", "Low (0.20) — Protected from splits"),
    ComposerTagPreset("coding", "💻", "+8.5pt Batching", "Analytical", "High (80)", "Peak Morning", "+8.5pt Same-Context Batching, shields deep focus state", "Low (0.25) — Protected from splits"),
    ComposerTagPreset("study", "📚", "+8.5pt Batching", "Hard Work", "Medium (50)", "Morning Focus", "+8.5pt Batching, optimal memory encoding", "Moderate (0.35)"),
    ComposerTagPreset("creative", "🎨", "+15pt Rebound", "Creative", "Medium (50)", "Evening Rebound Phase", "+15pt Divergent Thinking Rebound Bonus when inhibition lowers", "Moderate (0.40)"),
    ComposerTagPreset("writing", "✍️", "+6pt Batching", "Creative", "Medium (50)", "Morning / Rebound", "+6.0pt Context Affinity, continuous narrative flow", "Moderate (0.40)"),
    ComposerTagPreset("admin", "📋", "Protects Peak", "Routine", "Low (25)", "Afternoon Post-Lunch Dip", "Zero Peak Drain — slot-filled during low biological dip", "High (0.95) — Safe to interrupt"),
    ComposerTagPreset("meetings", "🤝", "Midday Coalesce", "Routine", "Medium (50)", "Midday Plateau", "Coalesces meetings with 5m buffers, protects morning blocks", "High (0.80)"),
    ComposerTagPreset("exercise", "🏃", "+12pt Endorphin", "Physical", "High (Exertion)", "Morning / Afternoon", "+10-12pt Alertness & BDNF boost for 45m post-workout", "Flexible (0.50)"),
    ComposerTagPreset("mindfulness", "🧘", "+100pt Reset", "Mindfulness", "Low (Restorative)", "Recovery Needed / Night", "+100pt Affinity during high cognitive exhaustion / depletion", "High (0.85) — Non-split"),
    ComposerTagPreset("chores", "🧹", "Active Rest", "Physical", "Low (25)", "Flexible / Dips", "Active Rest — physical movement during circadian dips", "High (0.90)"),
    ComposerTagPreset("quickwin", "⏱️", "+15pt Momentum", "Low Friction", "Low (25)", "Immediate / Any", "+15pt Momentum Pull, low initiation barrier for ADHD activation", "High (1.00)")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartTaskComposerSheetV2(
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
    mode: SheetMode = SheetMode.Create,
    circadianSummary: DailyCircadianSummary? = null,
    completedTasks: List<TaskEntity> = emptyList()
) {
    val existingTask = (mode as? SheetMode.Edit)?.task
    var rawInput by remember(existingTask) { mutableStateOf(existingTask?.title ?: "") }
    var userDurationOverride by remember(existingTask) { mutableStateOf<Int?>(existingTask?.estimatedDurationMinutes) }
    var userEnergyOverride by remember(existingTask) { mutableStateOf<EnergyLevel?>(existingTask?.energyLevel) }
    var userTaskTypeOverride by remember(existingTask) { mutableStateOf<TaskType?>(existingTask?.taskType) }
    var userFrogOverride by remember(existingTask) { mutableStateOf<Boolean?>(existingTask?.isFrog) }
    var userPriorityOverride by remember(existingTask) { mutableStateOf<Priority?>(existingTask?.priority) }
    var userQuadrantOverride by remember(existingTask) { mutableStateOf<Quadrant?>(existingTask?.quadrant) }

    val initialTags = remember(existingTask) {
        (existingTask?.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()).toSet()
    }
    var activeCustomTags by remember(existingTask) { mutableStateOf(initialTags) }

    // Strategic criteria & scheduling state
    var impactScore by remember(existingTask) { mutableStateOf(existingTask?.impactScore?.toFloat() ?: 50f) }
    var effortScore by remember(existingTask) { mutableStateOf(existingTask?.effortScore?.toFloat() ?: 50f) }
    var userScheduledDate by remember(existingTask) { mutableStateOf<Long?>(existingTask?.scheduledDate) }
    var scheduledDateCleared by remember(existingTask) { mutableStateOf(false) }
    var userScheduledTime by remember(existingTask) { mutableStateOf<Long?>(existingTask?.scheduledTime) }
    var scheduledTimeCleared by remember(existingTask) { mutableStateOf(false) }
    var userDeadlineDate by remember(existingTask) { mutableStateOf<Long?>(existingTask?.deadlineDate) }
    var deadlineDateCleared by remember(existingTask) { mutableStateOf(false) }
    var userDeadlineTime by remember(existingTask) { mutableStateOf<Long?>(existingTask?.deadlineTime) }
    var deadlineTimeCleared by remember(existingTask) { mutableStateOf(false) }
    var isScheduleLocked by remember(existingTask) { mutableStateOf(existingTask?.isScheduleLocked ?: false) }
    var recurrence by remember(existingTask) { mutableStateOf(existingTask?.recurrence ?: Recurrence.NONE) }

    var isExpanded by remember { mutableStateOf(existingTask != null) }
    var showSyntaxGuide by remember { mutableStateOf(false) }
    var showTagCatalogGuide by remember { mutableStateOf(false) }
    var description by remember(existingTask) { mutableStateOf(existingTask?.description ?: "") }

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
    val effectiveQuadrant = userQuadrantOverride ?: existingTask?.quadrant ?: inferred.quadrant
    val effectiveDeadlineDate = if (deadlineDateCleared) null else (userDeadlineDate ?: inferred.deadlineDate)
    val effectiveDeadlineTime = if (deadlineTimeCleared) null else (userDeadlineTime ?: inferred.deadlineTime)
    val effectiveScheduledDate = if (scheduledDateCleared) null else (userScheduledDate ?: inferred.scheduledDate)
    val effectiveScheduledTime = if (scheduledTimeCleared) null else (userScheduledTime ?: inferred.scheduledTime)

    val allActiveTags = remember(activeCustomTags, inferred.contextTags) {
        (activeCustomTags + inferred.contextTags).distinct()
    }

    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                rawInput = spokenText
                if (mode is SheetMode.Create) {
                    userDurationOverride = null
                    userEnergyOverride = null
                    userTaskTypeOverride = null
                    userFrogOverride = null
                    userPriorityOverride = null
                    userQuadrantOverride = null
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun toggleTag(tagName: String) {
        val regex = Regex("""(?i)\b#$tagName\b""")
        val isExplicitInInput = rawInput.contains(regex)
        val isInActiveCustom = activeCustomTags.any { it.equals(tagName, ignoreCase = true) }

        if (isExplicitInInput || isInActiveCustom) {
            if (isExplicitInInput) {
                rawInput = rawInput.replace(regex, "").replace(Regex("""\s{2,}"""), " ").trim()
            }
            activeCustomTags = activeCustomTags.filterNot { it.equals(tagName, ignoreCase = true) }.toSet()
        } else {
            activeCustomTags = activeCustomTags + tagName
        }
        if (mode is SheetMode.Create) {
            userDurationOverride = null
            userEnergyOverride = null
            userTaskTypeOverride = null
            userFrogOverride = null
            userPriorityOverride = null
            userQuadrantOverride = null
        }
    }

    fun removeTag(tagName: String) {
        val regex = Regex("""(?i)\b#$tagName\b""")
        if (rawInput.contains(regex)) {
            rawInput = rawInput.replace(regex, "").replace(Regex("""\s{2,}"""), " ").trim()
        }
        activeCustomTags = activeCustomTags.filterNot { it.equals(tagName, ignoreCase = true) }.toSet()
    }

    fun handleSave() {
        val title = inferred.sanitizedTitle.ifBlank { rawInput.trim() }
        if (title.isBlank()) return

        val effectiveTags = allActiveTags.joinToString(",")

        val taskToSave = if (existingTask != null) {
            existingTask.copy(
                title = title,
                description = description.trim(),
                tags = effectiveTags,
                estimatedDurationMinutes = effectiveDuration,
                energyLevel = effectiveEnergy,
                taskType = effectiveTaskType,
                isFrog = effectiveFrog,
                quadrant = effectiveQuadrant,
                priority = effectivePriority,
                impactScore = impactScore.toInt(),
                effortScore = effortScore.toInt(),
                deadlineDate = effectiveDeadlineDate,
                deadlineTime = effectiveDeadlineTime,
                scheduledDate = effectiveScheduledDate,
                scheduledTime = effectiveScheduledTime,
                isScheduleLocked = isScheduleLocked,
                recurrence = recurrence,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            TaskEntity(
                title = title,
                description = description.trim(),
                estimatedDurationMinutes = effectiveDuration,
                energyLevel = effectiveEnergy,
                taskType = effectiveTaskType,
                tags = effectiveTags,
                isFrog = effectiveFrog,
                quadrant = effectiveQuadrant,
                priority = effectivePriority,
                impactScore = impactScore.toInt(),
                effortScore = effortScore.toInt(),
                deadlineDate = effectiveDeadlineDate,
                deadlineTime = effectiveDeadlineTime,
                scheduledDate = effectiveScheduledDate,
                scheduledTime = effectiveScheduledTime,
                isScheduleLocked = isScheduleLocked,
                recurrence = recurrence
            )
        }
        onSave(taskToSave)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (mode is SheetMode.Edit) "Edit Task" else "Smart Task Composer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    IconButton(
                        onClick = { showSyntaxGuide = !showSyntaxGuide },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Formatting Guide",
                            tint = if (showSyntaxGuide) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NeuroFlowThemeV2Colors.TextSecondary)
                }
            }

            // Syntax Guide & Inspiration Examples (Expandable)
            AnimatedVisibility(visible = showSyntaxGuide) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = NeuroFlowThemeV2Colors.WarmAmber, modifier = Modifier.size(16.dp))
                            Text(
                                "Natural Language Formatting Guide",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Type naturally! The engine automatically detects durations, deadlines, priorities, and tags:",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• ⏱️ Duration: '30m', '45m', '90m', '1h 30m'", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                        Text("• 📅 Deadline: 'tomorrow', 'Friday 10am', 'today at 3pm'", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                        Text("• 🐸 Crucial: '!frog' (sets daily frog), '#high', '!urgent'", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                        Text("• 🏛️ Eisenhower: '!q1' / '!dofirst', '!q2' / '!schedule', '!q3' / '!delegate', '!q4' / '!eliminate'", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                        Text("• 🏷️ Tags & Boosts: '#deepwork', '#coding', '#creative', '#admin'", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                    }
                }
            }

            // Quick Preset Starters (when input is empty)
            if (rawInput.isBlank()) {
                Text(
                    "💡 Quick Starters (Tap to try):",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowThemeV2Colors.TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Ship core patch 45m #coding #deepwork",
                        "Workout & cardio 30m #exercise tomorrow",
                        "Inbox zero 15m #admin",
                        "Roadmap brainstorm 60m #creative",
                        "Mindful reset 10m #mindfulness"
                    ).forEach { sample ->
                        Surface(
                            onClick = { rawInput = sample },
                            shape = RoundedCornerShape(50),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                        ) {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // NLP Input Field
            OutlinedTextField(
                value = rawInput,
                onValueChange = {
                    rawInput = it
                    if (mode is SheetMode.Create) {
                        userDurationOverride = null
                        userEnergyOverride = null
                        userTaskTypeOverride = null
                        userFrogOverride = null
                        userPriorityOverride = null
                        userQuadrantOverride = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        "e.g. Design core mockups 90m #high !frog tomorrow",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowThemeV2Colors.TextTertiary
                    )
                },
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak task (e.g. 'Ship quarterly patch 45m #high')")
                                }
                                speechLauncher.launch(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Dictation",
                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                    unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                )
            )

            // Interactive Inferred & Override Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Duration Chip with Calibration Transparency
                val durationLabel = if (inferred.isDurationExplicit) {
                    "⏱ ${effectiveDuration}m"
                } else {
                    "⏱ ${effectiveDuration}m (${inferred.calibrationFactorApplied}x)"
                }
                FilterChip(
                    selected = true,
                    onClick = {
                        val cycles = listOf(15, 25, 45, 60, 90, 120)
                        val nextIdx = (cycles.indexOf(effectiveDuration) + 1) % cycles.size
                        userDurationOverride = cycles[nextIdx]
                    },
                    label = { Text(durationLabel) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Energy Chip
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

                // Priority Chip (Interactive cycle)
                FilterChip(
                    selected = true,
                    onClick = {
                        userPriorityOverride = when (effectivePriority) {
                            Priority.HIGH -> Priority.MEDIUM
                            Priority.MEDIUM -> Priority.LOW
                            Priority.LOW -> Priority.HIGH
                        }
                    },
                    label = {
                        val pLabel = when (effectivePriority) {
                            Priority.HIGH -> "🔴 High Priority"
                            Priority.MEDIUM -> "🟡 Med Priority"
                            Priority.LOW -> "🟢 Low Priority"
                        }
                        Text(pLabel)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Eisenhower Quadrant Chip (Interactive cycle)
                val qChipLabel = when (effectiveQuadrant) {
                    Quadrant.DO_FIRST -> "🎯 Do First (Q1)"
                    Quadrant.SCHEDULE -> "🔵 Schedule (Q2)"
                    Quadrant.DELEGATE -> "🟢 Delegate (Q3)"
                    Quadrant.ELIMINATE -> "⚪ Eliminate (Q4)"
                }
                FilterChip(
                    selected = true,
                    onClick = {
                        userQuadrantOverride = when (effectiveQuadrant) {
                            Quadrant.DO_FIRST -> Quadrant.SCHEDULE
                            Quadrant.SCHEDULE -> Quadrant.DELEGATE
                            Quadrant.DELEGATE -> Quadrant.ELIMINATE
                            Quadrant.ELIMINATE -> Quadrant.DO_FIRST
                        }
                    },
                    label = { Text(qChipLabel) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Frog Chip
                FilterChip(
                    selected = effectiveFrog,
                    onClick = { userFrogOverride = !effectiveFrog },
                    label = { Text(if (effectiveFrog) "🐸 Frog (Crucial)" else "🐸 Normal") },
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                // Context Tags Chips
                allActiveTags.forEach { tag ->
                    SuggestionChip(
                        onClick = { removeTag(tag) },
                        label = { Text("#$tag ✕") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Available Tags & Boosts Section (User Guidance & 1-Tap Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "🏷️ Available Tags & Boosts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "(Tap to add)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextTertiary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                TextButton(
                    onClick = { showTagCatalogGuide = !showTagCatalogGuide },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (showTagCatalogGuide) "Hide Details" else "View Boost Guide",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontally Scrollable Tag Presets with Boost Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SMART_TAG_PRESETS.forEach { preset ->
                    val isSelected = allActiveTags.any { it.equals(preset.tag, ignoreCase = true) }

                    Surface(
                        onClick = { toggleTag(preset.tag) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f) else NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.SlateBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(preset.emoji, fontSize = 12.sp)
                            Text(
                                text = "#${preset.tag}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigoGlow else NeuroFlowThemeV2Colors.TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f) else NeuroFlowThemeV2Colors.SlateSurface
                            ) {
                                Text(
                                    text = preset.shortBoost,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else NeuroFlowThemeV2Colors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Detailed Tag Catalog Guide Table (Expandable)
            AnimatedVisibility(visible = showTagCatalogGuide) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "⚡ Biophysical Scheduling Boost Matrix",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        SMART_TAG_PRESETS.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("${p.emoji} #${p.tag}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow, modifier = Modifier.width(90.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${p.biologicalWindow} • ${p.energyDemand} Energy",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeuroFlowThemeV2Colors.TextPrimary
                                    )
                                    Text(
                                        text = "${p.boostDescription} (${p.fragmentationTolerance})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowThemeV2Colors.TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (p != SMART_TAG_PRESETS.last()) {
                                HorizontalDivider(color = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            // Inferred Scheduling Strategy Preview Banner
            val matchedPresets = SMART_TAG_PRESETS.filter { p ->
                allActiveTags.any { it.equals(p.tag, ignoreCase = true) }
            }
            if (matchedPresets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow, modifier = Modifier.size(15.dp))
                            Text(
                                "Active Scheduling Strategy:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        matchedPresets.forEach { p ->
                            Text(
                                text = "• #${p.tag}: ${p.boostDescription} • ${p.biologicalWindow}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                        val peakHour = circadianSummary?.peakWindowStartHour ?: 9
                        val dipHour = circadianSummary?.dipWindowStartHour ?: 14
                        val plateauHour = ((peakHour + 5) % 24).coerceIn(13, 17)

                        fun formatHour(hour: Int): String {
                            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                            val amPm = if (hour < 12) "AM" else "PM"
                            return String.format("%02d:00 %s", h, amPm)
                        }

                        val slotRecommendation = when (effectiveEnergy) {
                            EnergyLevel.HIGH -> "Recommended Slot: ${formatHour(peakHour)} (Peak Biological Alertness)"
                            EnergyLevel.MEDIUM -> "Recommended Slot: ${formatHour(plateauHour)} (Focus Plateau)"
                            EnergyLevel.LOW -> "Recommended Slot: ${formatHour(dipHour)} (Recovery & Admin)"
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
                    text = if (isExpanded) "Hide Detailed Criteria" else "Detailed Criteria (Eisenhower, Calendar, Impact, Effort)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                    // Eisenhower Matrix Quadrant Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🏛️ Eisenhower Matrix Quadrant",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val qDescriptor = when (effectiveQuadrant) {
                            Quadrant.DO_FIRST -> "Urgent & Important"
                            Quadrant.SCHEDULE -> "Important, Not Urgent"
                            Quadrant.DELEGATE -> "Urgent, Low Impact"
                            Quadrant.ELIMINATE -> "Neither (Deprioritized)"
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = qDescriptor,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val quadrantOptions = listOf(
                        ComposerQuadrantOption(Quadrant.DO_FIRST, "🎯 Do First", "Urgent & Important", NeuroFlowThemeV2Colors.RoseCoral),
                        ComposerQuadrantOption(Quadrant.SCHEDULE, "🔵 Schedule", "Important, Not Urgent", NeuroFlowThemeV2Colors.ElectricIndigoGlow),
                        ComposerQuadrantOption(Quadrant.DELEGATE, "🟢 Delegate", "Urgent, Low Impact", NeuroFlowThemeV2Colors.SageGreen),
                        ComposerQuadrantOption(Quadrant.ELIMINATE, "⚪ Eliminate", "Backlog / Someday", NeuroFlowThemeV2Colors.TextTertiary)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quadrantOptions.take(2).forEach { option ->
                            val isSelected = effectiveQuadrant == option.quadrant
                            Surface(
                                onClick = { userQuadrantOverride = option.quadrant },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) option.accentColor.copy(alpha = 0.16f) else NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) option.accentColor else NeuroFlowThemeV2Colors.SlateBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 54.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) option.accentColor else NeuroFlowThemeV2Colors.TextPrimary,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = option.accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = if (isSelected) option.accentColor.copy(alpha = 0.85f) else NeuroFlowThemeV2Colors.TextTertiary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quadrantOptions.drop(2).forEach { option ->
                            val isSelected = effectiveQuadrant == option.quadrant
                            Surface(
                                onClick = { userQuadrantOverride = option.quadrant },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) option.accentColor.copy(alpha = 0.16f) else NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) option.accentColor else NeuroFlowThemeV2Colors.SlateBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 54.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) option.accentColor else NeuroFlowThemeV2Colors.TextPrimary,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = option.accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = if (isSelected) option.accentColor.copy(alpha = 0.85f) else NeuroFlowThemeV2Colors.TextTertiary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigo, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (effectiveDeadlineDate != null) SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(effectiveDeadlineDate)) else "Pick Due Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (effectiveDeadlineDate != null) {
                                    IconButton(
                                        onClick = {
                                            userDeadlineDate = null
                                            deadlineDateCleared = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear deadline date",
                                            tint = NeuroFlowThemeV2Colors.TextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigo, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (effectiveDeadlineTime != null) {
                                        val h = (effectiveDeadlineTime / 3_600_000L).toInt()
                                        val m = ((effectiveDeadlineTime % 3_600_000L) / 60_000L).toInt()
                                        String.format("%02d:%02d", h, m)
                                    } else "Pick Due Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (effectiveDeadlineTime != null) {
                                    IconButton(
                                        onClick = {
                                            userDeadlineTime = null
                                            deadlineTimeCleared = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear deadline time",
                                            tint = NeuroFlowThemeV2Colors.TextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scheduled Slot & Calendar
                    Text("⏰ Scheduled Slot Date & Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                                    datePickerTarget = "scheduled"
                                    showDatePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (effectiveScheduledDate != null) SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(effectiveScheduledDate)) else "Pick Slot Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (effectiveScheduledDate != null) {
                                    IconButton(
                                        onClick = {
                                            userScheduledDate = null
                                            scheduledDateCleared = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear scheduled date",
                                            tint = NeuroFlowThemeV2Colors.TextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    timePickerTarget = "scheduled"
                                    showTimePicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (effectiveScheduledTime != null) {
                                        val h = (effectiveScheduledTime / 3_600_000L).toInt()
                                        val m = ((effectiveScheduledTime % 3_600_000L) / 60_000L).toInt()
                                        String.format("%02d:%02d", h, m)
                                    } else "Pick Slot Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (effectiveScheduledTime != null) {
                                    IconButton(
                                        onClick = {
                                            userScheduledTime = null
                                            scheduledTimeCleared = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear scheduled time",
                                            tint = NeuroFlowThemeV2Colors.TextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recurrence / Habit
                    Text("🔁 Recurrence / Habit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Recurrence.NONE to "None",
                            Recurrence.DAILY to "Daily",
                            Recurrence.WEEKLY to "Weekly",
                            Recurrence.MONTHLY to "Monthly",
                            Recurrence.CUSTOM to "Custom"
                        ).forEach { (recOption, label) ->
                            val isSelected = recurrence == recOption
                            FilterChip(
                                selected = isSelected,
                                onClick = { recurrence = recOption },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f),
                                    selectedLabelColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                    labelColor = NeuroFlowThemeV2Colors.TextSecondary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lock Schedule Toggle (Pin to Slot)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isScheduleLocked) NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f) else NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(1.dp, if (isScheduleLocked) NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.5f) else NeuroFlowThemeV2Colors.SlateBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isScheduleLocked = !isScheduleLocked }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isScheduleLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isScheduleLocked) NeuroFlowThemeV2Colors.WarmAmber else NeuroFlowThemeV2Colors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isScheduleLocked) "🔒 Schedule Locked (Pinned)" else "Schedule Unlocked (Flexible)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isScheduleLocked) NeuroFlowThemeV2Colors.WarmAmber else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isScheduleLocked) "Auto-scheduler will never displace this task" else "Auto-scheduler can optimize slot placement",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowThemeV2Colors.TextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = isScheduleLocked,
                                onCheckedChange = { isScheduleLocked = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NeuroFlowThemeV2Colors.WarmAmber
                                )
                            )
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
                Icon(if (mode is SheetMode.Edit) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (mode is SheetMode.Edit) "Save Changes" else "Schedule Into Energy Stream",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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
                            if (datePickerTarget == "deadline") {
                                userDeadlineDate = selected
                                deadlineDateCleared = false
                            } else {
                                userScheduledDate = selected
                                scheduledDateCleared = false
                            }
                        }
                        showDatePicker = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            if (datePickerTarget == "deadline") {
                                userDeadlineDate = null
                                deadlineDateCleared = true
                            } else {
                                userScheduledDate = null
                                scheduledDateCleared = true
                            }
                            showDatePicker = false
                        }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
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
                        if (timePickerTarget == "deadline") {
                            userDeadlineTime = millis
                            deadlineTimeCleared = false
                        } else {
                            userScheduledTime = millis
                            scheduledTimeCleared = false
                        }
                        showTimePicker = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            if (timePickerTarget == "deadline") {
                                userDeadlineTime = null
                                deadlineTimeCleared = true
                            } else {
                                userScheduledTime = null
                                scheduledTimeCleared = true
                            }
                            showTimePicker = false
                        }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                    }
                }
            )
        }
    }
}

@Composable
fun MetadataPill(
    icon: String,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = icon, fontSize = 11.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
            )
        }
    }
}
