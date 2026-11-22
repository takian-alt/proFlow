package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.scheduler.BlueprintRecoveryBuffer
import com.neuroflow.app.domain.scheduler.BlueprintTaskItem
import com.neuroflow.app.domain.scheduler.DailyBlueprint
import com.neuroflow.app.domain.scheduler.SchedulableTaskMapper
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.TaskFatigueScoreUi
import com.neuroflow.app.presentation.v2.common.FatigueAwareScoreBadge
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

internal fun formatBlueprintDate(dateMillis: Long): String {
    val zoneId = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    val monthFormatted = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return when {
        date == today -> "Today, $monthFormatted ${date.dayOfMonth}"
        date == today.plusDays(1) -> "Tomorrow, $monthFormatted ${date.dayOfMonth}"
        else -> "${date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}, $monthFormatted ${date.dayOfMonth}"
    }
}

internal fun formatStartEndTime(startMillis: Long, endMillis: Long): String {
    val zoneId = ZoneId.systemDefault()
    val startZdt = Instant.ofEpochMilli(startMillis).atZone(zoneId)
    val endZdt = Instant.ofEpochMilli(endMillis).atZone(zoneId)
    return String.format(Locale.getDefault(), "%02d:%02d – %02d:%02d", startZdt.hour, startZdt.minute, endZdt.hour, endZdt.minute)
}

internal fun parseSplitBadge(task: TaskEntity): String? {
    val isUltradianPart = task.id.contains("#part") || task.id.contains("_part") || task.title.contains("(Part ")
    if (!isUltradianPart) return null
    val titleMatch = Regex("""\(Part (\d+/\d+)\)""").find(task.title)
    val idPartMatch = Regex("""[#_]part(\d+)""").find(task.id)
    return when {
        titleMatch != null -> "Part ${titleMatch.groupValues[1]}"
        idPartMatch != null -> "Part ${idPartMatch.groupValues[1]}"
        task.id.contains("#part2") || task.id.contains("_part2") || task.title.contains("(Part 2/2)") -> "Part 2/2"
        else -> "Part 1/2"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyBlueprintBottomSheet(
    blueprint: DailyBlueprint?,
    isLoading: Boolean,
    includedTaskIds: Set<String>,
    onToggleTask: (taskId: String, isIncluded: Boolean) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    fatigueScoresByTaskId: Map<String, TaskFatigueScoreUi> = emptyMap(),
    onAutoBalanceWeek: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeuroFlowThemeV2Colors.SlateSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NeuroFlowThemeV2Colors.SlateBorder) },
        modifier = modifier
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = "✨ Synthesizing Your Daily Blueprint...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                    Text(
                        text = "Harmonizing tasks against your peak biological energy windows",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowPaletteV2.TextSecondary
                    )
                }
            }
            return@ModalBottomSheet
        }

        if (blueprint == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No blueprint available for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuroFlowPaletteV2.TextSecondary
                )
            }
            return@ModalBottomSheet
        }

        val dynamicPlannedMinutes = remember(blueprint, includedTaskIds) {
            blueprint.items
                .filter { it.task.id in includedTaskIds }
                .sumOf { item ->
                    if (parseSplitBadge(item.task) != null) {
                        val slotDuration = ((item.proposedEndMillis - item.proposedStartMillis) / 60_000L).toInt()
                        if (slotDuration > 0) slotDuration else TaskBlockPlan.plannedMinutes(item.task)
                    } else {
                        TaskBlockPlan.plannedMinutes(item.task)
                    }
                }
        }
        val totalCommittedMinutes = blueprint.precommittedMinutes + dynamicPlannedMinutes
        val capacityTarget = blueprint.capacityTargetMinutes
        val utilizationPercent = if (capacityTarget > 0) {
            ((totalCommittedMinutes.toFloat() / capacityTarget) * 100).roundToInt()
        } else 0
        val progressFraction = if (capacityTarget > 0) {
            (totalCommittedMinutes.toFloat() / capacityTarget).coerceIn(0f, 1f)
        } else 0f

        val timelineEntries = remember(blueprint.items, blueprint.recoveryBuffers) {
            val combined = mutableListOf<Pair<Long, Any>>()
            blueprint.items.forEach { combined.add(it.proposedStartMillis to it) }
            blueprint.recoveryBuffers.forEach { combined.add(it.startMillis to it) }
            combined.sortedBy { it.first }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header: AI Badge, Date, Cognitive Capacity Bar
                item(key = "header") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                NeuroFlowPaletteV2.PeakAmber
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f),
                                                NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.15f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "✨ DAILY BLUEPRINT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onRegenerate != null) {
                                    IconButton(
                                        onClick = onRegenerate,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("blueprint_regenerate_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Re-optimize blueprint",
                                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = NeuroFlowPaletteV2.TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = formatBlueprintDate(blueprint.targetDateMillis),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeuroFlowPaletteV2.TextPrimary
                        )

                        // Capacity Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeuroFlowPaletteV2.SurfaceElevated, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cognitive Capacity",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowPaletteV2.TextSecondary
                                )
                                Text(
                                    text = "${totalCommittedMinutes}m / ${capacityTarget}m • ${utilizationPercent}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (utilizationPercent > 100) NeuroFlowPaletteV2.RecoveryRose else NeuroFlowPaletteV2.OptimalEmerald
                                )
                            }
                            if (blueprint.precommittedMinutes > 0) {
                                Text(
                                    text = "Fixed & completed: ${blueprint.precommittedMinutes}m • New blueprint slots: ${dynamicPlannedMinutes}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowPaletteV2.TextSecondary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (utilizationPercent > 100) NeuroFlowPaletteV2.RecoveryRose else NeuroFlowPaletteV2.PeakAmber,
                                trackColor = NeuroFlowThemeV2Colors.SlateBorder
                            )
                            if (blueprint.burnoutPacingSummary.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.testTag("blueprint_burnout_pacing_summary")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = null,
                                        tint = NeuroFlowPaletteV2.OptimalEmerald,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = blueprint.burnoutPacingSummary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeuroFlowPaletteV2.OptimalEmerald
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. AI Strategy Callout
                item(key = "ai_strategy") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NeuroFlowPaletteV2.SurfaceElevated,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeuroFlowPaletteV2.PeakAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "AI Strategy Rationale",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowPaletteV2.PeakAmber
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = blueprint.aiStrategyRationale,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeuroFlowPaletteV2.TextPrimary
                                )
                            }
                        }
                    }
                }

                // 3. Proposed Timeline List
                item(key = "timeline_heading") {
                    Text(
                        text = "Proposed Schedule (${blueprint.items.size} tasks)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowPaletteV2.TextSecondary
                    )
                }

                if (blueprint.items.isEmpty()) {
                    item(key = "empty_blueprint") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NeuroFlowPaletteV2.SurfaceElevated,
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (blueprint.precommittedMinutes >= capacityTarget) {
                                        "Today's cognitive budget is already full from fixed or completed blocks."
                                    } else {
                                        "No additional tasks fit into remaining focus windows for this day."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeuroFlowPaletteV2.TextPrimary
                                )
                                if (onAutoBalanceWeek != null) {
                                    OutlinedButton(
                                        onClick = onAutoBalanceWeek,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("blueprint_auto_balance_week_button")
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Spread Tasks Across 7-Day Week",
                                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(
                    items = timelineEntries,
                    key = { (_, entry) ->
                        when (entry) {
                            is BlueprintTaskItem -> "task_${entry.task.id}"
                            is BlueprintRecoveryBuffer -> "buf_${entry.id}"
                            else -> entry.hashCode().toString()
                        }
                    }
                ) { (_, entry) ->
                    if (entry is BlueprintRecoveryBuffer) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.11f),
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("blueprint_recovery_buffer_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelfImprovement,
                                    contentDescription = "Recovery buffer",
                                    tint = NeuroFlowPaletteV2.OptimalEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formatStartEndTime(entry.startMillis, entry.endMillis),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            fontWeight = FontWeight.Bold,
                                            color = NeuroFlowPaletteV2.OptimalEmerald
                                        )
                                        Text(
                                            text = "· ${entry.label}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = NeuroFlowPaletteV2.TextPrimary
                                        )
                                    }
                                    Text(
                                        text = entry.reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowPaletteV2.TextSecondary
                                    )
                                }
                            }
                        }
                        return@items
                    }

                    val item = entry as BlueprintTaskItem
                    val isIncluded = item.task.id in includedTaskIds
                    val phaseAccent = NeuroFlowPaletteV2.accentForPhase(item.energyPhase)
                    val splitBadge = parseSplitBadge(item.task)
                    val cleanTitle = item.task.title.replace(Regex("""\s*\(Part \d+/\d+\)"""), "").trim()
                    val plannedMinutes = if (splitBadge != null) {
                        ((item.proposedEndMillis - item.proposedStartMillis) / 60_000L).toInt()
                            .takeIf { it > 0 } ?: TaskBlockPlan.plannedMinutes(item.task)
                    } else {
                        TaskBlockPlan.plannedMinutes(item.task)
                    }
                    val baseId = SchedulableTaskMapper.cleanSplitTaskId(item.task.id)
                    val fatigueScore = fatigueScoresByTaskId[item.task.id] ?: fatigueScoresByTaskId[baseId]

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isIncluded) NeuroFlowPaletteV2.SurfaceElevated else NeuroFlowPaletteV2.SurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (isIncluded) phaseAccent.copy(alpha = 0.5f) else NeuroFlowPaletteV2.BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isIncluded) 1f else 0.55f)
                            .clickable { onToggleTask(item.task.id, !isIncluded) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = isIncluded,
                                onCheckedChange = { onToggleTask(item.task.id, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NeuroFlowPaletteV2.PeakAmber,
                                    uncheckedColor = NeuroFlowPaletteV2.TextSecondary
                                )
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Start-end time chip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NeuroFlowPaletteV2.BackgroundDeep,
                                        border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                                    ) {
                                        Text(
                                            text = formatStartEndTime(item.proposedStartMillis, item.proposedEndMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NeuroFlowPaletteV2.TextPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Circadian energy pill
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = phaseAccent.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, phaseAccent.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = item.energyPhase.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = phaseAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Split badge if present
                                    if (splitBadge != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = splitBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = cleanTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeuroFlowPaletteV2.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (fatigueScore != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        FatigueAwareScoreBadge(
                                            score = fatigueScore,
                                            compact = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "⚡ ${plannedMinutes}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeuroFlowPaletteV2.PeakAmber
                                    )
                                }

                                if (item.conflictWarning != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚠️ ${item.conflictWarning}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeuroFlowPaletteV2.RecoveryRose
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Overflow Tasks Section (if any)
                if (blueprint.overflowTasks.isNotEmpty()) {
                    item(key = "overflow_section") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = NeuroFlowPaletteV2.PeakAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${blueprint.overflowTasks.size} task(s) deferred to upcoming days to protect your deep work capacity.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeuroFlowPaletteV2.TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = blueprint.overflowTasks.joinToString(", ") { it.title },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowPaletteV2.TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (onAutoBalanceWeek != null) {
                                    OutlinedButton(
                                        onClick = onAutoBalanceWeek,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("blueprint_overflow_balance_button")
                                    ) {
                                        Text(
                                            text = "Spread Overflow Across 7-Day Week",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = NeuroFlowPaletteV2.PeakAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Sticky Bottom Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel", color = NeuroFlowPaletteV2.TextSecondary)
                }

                Button(
                    onClick = onApply,
                    enabled = includedTaskIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("apply_blueprint_all_button")
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply Schedule (1-Tap)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }
    }
}
