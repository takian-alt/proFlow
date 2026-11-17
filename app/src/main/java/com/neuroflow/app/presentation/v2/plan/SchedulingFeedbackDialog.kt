package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.service.SlotRemedyResult
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatRemedyTime(millis: Long): String {
    return if (millis < 86_400_000L) {
        val totalMinutes = (millis / 60_000L).toInt()
        val hour = (totalMinutes / 60) % 24
        val minute = totalMinutes % 60
        val isPm = hour >= 12
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (isPm) "PM" else "AM"
        String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
    } else {
        try {
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
        } catch (_: Exception) {
            val totalMinutes = ((millis / 60_000L) % 1440).toInt()
            val hour = (totalMinutes / 60) % 24
            val minute = totalMinutes % 60
            val isPm = hour >= 12
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val amPm = if (isPm) "PM" else "AM"
            String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
        }
    }
}

enum class FeedbackReason(val tag: String, val label: String) {
    TOO_LOW_ENERGY("TOO_LOW_ENERGY", "⚡ Too low energy"),
    TOO_EARLY("TOO_EARLY", "⏰ Too early in the day"),
    NEEDS_LONGER_BLOCK("NEEDS_LONGER_BLOCK", "⏱️ Needs longer block"),
    ROUTINE_CONFLICT("ROUTINE_CONFLICT", "🛑 Conflicts with routine")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchedulingFeedbackDialog(
    task: TaskEntity,
    remedyProposal: SlotRemedyResult?,
    isLoading: Boolean,
    onSelectReason: (String) -> Unit,
    onAcceptRemedy: (newSlotMillis: Long, directivePatch: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Tune Your Schedule Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Why does this slot not work for \"${task.title}\"?",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Computing instant chronobiological remedy...",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeuroFlowThemeV2Colors.TextTertiary
                            )
                        }
                    }
                } else if (remedyProposal != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurface,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Proposed Slot Remedy",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = formatRemedyTime(remedyProposal.recommendedStartMillis),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                            }

                            Text(
                                text = remedyProposal.reasoning,
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary,
                                lineHeight = 18.sp
                            )

                            if (!remedyProposal.directivePatch.isNullOrBlank()) {
                                Text(
                                    text = "💡 ${remedyProposal.directivePatch}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.WarmAmber,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedbackReason.entries.forEach { reason ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeuroFlowThemeV2Colors.SlateSurface,
                                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectReason(reason.tag) }
                            ) {
                                Text(
                                    text = reason.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeuroFlowThemeV2Colors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (remedyProposal != null && !isLoading) {
                Button(
                    onClick = {
                        onAcceptRemedy(
                            remedyProposal.recommendedStartMillis,
                            remedyProposal.directivePatch
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Accept & Move",
                        fontWeight = FontWeight.SemiBold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Dismiss / Re-schedule",
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )
            }
        }
    )
}
