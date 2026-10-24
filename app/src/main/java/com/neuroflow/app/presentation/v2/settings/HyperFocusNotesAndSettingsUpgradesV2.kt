package com.neuroflow.app.presentation.v2.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.domain.service.QuickNoteSynthesis
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2

@Composable
fun DopamineSafeBlockingGateCard(
    blockedAppName: String,
    completedTasksInSession: Int,
    nextRewardTier: RewardTier,
    topTaskTitle: String,
    topTaskFirstStep: String,
    onStartTwoMinuteStepClick: () -> Unit,
    onRedeemEarnedPassClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tasksNeeded = (nextRewardTier.taskThreshold - completedTasksInSession).coerceAtLeast(1)

    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.5.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.DipIndigo, 1f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "HYPERFOCUS DOPAMINE SHIELD",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = NeuroFlowPaletteV2.ReboundCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$blockedAppName is paused right now",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Complete $tasksNeeded more task(s) to unlock a ${nextRewardTier.unlockMinutes}-minute ${nextRewardTier.name} reward pass.",
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowPaletteV2.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = NeuroFlowPaletteV2.SurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "NEXT MICRO-STEP (2 MIN):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.OptimalEmerald
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = topTaskFirstStep.ifBlank { topTaskTitle },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStartTwoMinuteStepClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeuroFlowPaletteV2.OptimalEmerald,
                    contentColor = Color(0xFF0A0B0E)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Do 2-Min Starter Step Now",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            if (onRedeemEarnedPassClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Or redeem an earned reward pass",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeuroFlowPaletteV2.ReboundCyan,
                    modifier = Modifier
                        .clickable { onRedeemEarnedPassClick() }
                        .padding(6.dp)
                )
            }
        }
    }
}

@Composable
fun QuickNoteAiSynthesizerCard(
    synthesis: QuickNoteSynthesis,
    onConvertToTaskClick: (QuickNoteSynthesis) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.ReboundCyan, 0.85f)
        ),
        modifier = modifier.fillMaxWidth()
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
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "AI TASK EXTRACTION (${synthesis.estimatedMinutes}m · ${synthesis.suggestedCategory})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = synthesis.extractedTaskTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.TextPrimary
                )
                Text(
                    text = "Starter: ${synthesis.firstActionStep}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowPaletteV2.TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                color = NeuroFlowPaletteV2.ReboundCyan,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { onConvertToTaskClick(synthesis) }
            ) {
                Text(
                    text = "+ Add Task",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun CategorizedSettingsHubV2(
    chronotypeSummary: String,
    schedulerSummary: String,
    hyperFocusSummary: String,
    aiAndSyncSummary: String,
    onOpenChronobiologyAndSleep: () -> Unit,
    onOpenSchedulerAndWeights: () -> Unit,
    onOpenHyperFocusAndPermissions: () -> Unit,
    onOpenAiAndCloudSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        SettingsCategoryRowCard(
            icon = Icons.Default.Bedtime,
            accent = NeuroFlowPaletteV2.PeakAmber,
            title = "Chronobiology, MEQ & Sleep Logs",
            subtitle = chronotypeSummary,
            onClick = onOpenChronobiologyAndSleep
        )
        SettingsCategoryRowCard(
            icon = Icons.Default.Tune,
            accent = NeuroFlowPaletteV2.ReboundCyan,
            title = "Auto-Scheduler, Protected Slots & Weights",
            subtitle = schedulerSummary,
            onClick = onOpenSchedulerAndWeights
        )
        SettingsCategoryRowCard(
            icon = Icons.Default.LockClock,
            accent = NeuroFlowPaletteV2.DipIndigo,
            title = "HyperFocus Gate, Allowlist & Kiosk Mode",
            subtitle = hyperFocusSummary,
            onClick = onOpenHyperFocusAndPermissions
        )
        SettingsCategoryRowCard(
            icon = Icons.Default.CloudSync,
            accent = NeuroFlowPaletteV2.OptimalEmerald,
            title = "Gemini AI Coach, Calendar & Cloud Backup",
            subtitle = aiAndSyncSummary,
            onClick = onOpenAiAndCloudSync
        )
    }
}

@Composable
private fun SettingsCategoryRowCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush(accent, 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowPaletteV2.TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NeuroFlowPaletteV2.TextTertiary
            )
        }
    }
}
