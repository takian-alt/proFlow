package com.neuroflow.app.presentation.v2.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.engine.FreshStartEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.scheduler.DueSlot
import com.neuroflow.app.domain.scheduler.SlotPhase
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.remember
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import kotlin.math.roundToInt

@Composable
fun FlowSpotlightHeroCard(
    activeTakeoverSlot: DueSlot?,
    topTaskId: String?,
    topTaskTitle: String?,
    topTaskFirstMicroStep: String?,
    topTaskDurationMinutes: Int,
    topTaskEnergyLevel: EnergyLevel,
    isTopTaskFrog: Boolean,
    currentPhase: EnergyPhase,
    onStartFocusClick: (String) -> Unit,
    onCompleteQuickClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    approachingSlot: DueSlot? = null,
    topTaskMicroSteps: List<StepByStepPlanStep> = emptyList()
) {
    val phaseAccent = NeuroFlowPaletteV2.accentForPhase(currentPhase)
    val cardShape = RoundedCornerShape(24.dp)

    if (activeTakeoverSlot != null) {
        Surface(
            color = NeuroFlowPaletteV2.SurfaceCard,
            shape = cardShape,
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.OptimalEmerald, 1f),
                    shape = cardShape
                )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = NeuroFlowPaletteV2.OptimalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeTakeoverSlot.phase == SlotPhase.RUNNING) "PROTECTED RECOVERY ACTIVE" else "WRAPPING PROTECTED SLOT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NeuroFlowPaletteV2.OptimalEmerald,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${activeTakeoverSlot.minutesLeft}m left",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activeTakeoverSlot.slot.label,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = NeuroFlowPaletteV2.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deep work is paused to protect your ${activeTakeoverSlot.slot.category.name.lowercase()} window and discharge synaptic fatigue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuroFlowPaletteV2.TextSecondary
                )
            }
        }
        return
    }

    if (topTaskId == null || topTaskTitle.isNullOrBlank()) return

    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = cardShape,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = NeuroFlowPaletteV2.specularBorderBrush(phaseAccent, 1f),
                shape = cardShape
            )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(phaseAccent.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(20.dp)
        ) {
            if (approachingSlot != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🛡️", fontSize = 14.sp)
                        Text(
                            text = "${approachingSlot.slot.label} starts in ${approachingSlot.minutesLeft}m — fit a quick doorway step before transition",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NeuroFlowPaletteV2.PeakAmber
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTopTaskFrog) "🐸 DAILY FROG SPOTLIGHT" else "⚡ OPTIMAL NEXT ACTION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = phaseAccent,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                ) {
                    Text(
                        text = "${topTaskDurationMinutes}m · ${topTaskEnergyLevel.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NeuroFlowPaletteV2.TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = topTaskTitle,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (topTaskMicroSteps.isNotEmpty()) {
                val completedCount = topTaskMicroSteps.count { it.completed }
                val totalCount = topTaskMicroSteps.size
                val currentStep = topTaskMicroSteps.firstOrNull { !it.completed } ?: topTaskMicroSteps.first()
                val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeuroFlowPaletteV2.SurfaceElevated)
                        .border(1.dp, NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🪜 2m Doorway: Step ${completedCount + 1} of $totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NeuroFlowPaletteV2.ReboundCyan,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}% completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        trackColor = NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.3f),
                    )
                    Text(
                        text = currentStep.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowPaletteV2.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (!topTaskFirstMicroStep.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeuroFlowPaletteV2.SurfaceElevated)
                        .border(1.dp, NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2m Doorway:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = topTaskFirstMicroStep,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowPaletteV2.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onStartFocusClick(topTaskId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = phaseAccent,
                        contentColor = Color(0xFF0A0B0E)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Launch Focus",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Surface(
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCompleteQuickClick(topTaskId) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = "Complete Task",
                            tint = NeuroFlowPaletteV2.OptimalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FreshStartRecoveryBanner(
    visible: Boolean,
    resetPlan: FreshStartEngine.EmergencyResetPlan?,
    onApplyEmergencyReset: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && resetPlan != null && resetPlan.tasksPushedToBacklog.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val plan = resetPlan ?: return@AnimatedVisibility
        val shape = RoundedCornerShape(18.dp)
        Surface(
            color = NeuroFlowPaletteV2.SurfaceCard,
            shape = shape,
            border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.DipIndigo, 0.9f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.10f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShieldMoon,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Schedule Feeling Heavy?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Keep ${plan.tasksToReschedule.size} essential task(s) today, move ${plan.tasksPushedToBacklog.size} to backlog, and freeze your streak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowPaletteV2.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Not Now",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextSecondary
                        )
                    }
                    Surface(
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onApplyEmergencyReset() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Fresh Start",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskSlotEnergyFitBadge(
    taskScheduledMillis: Long,
    taskEnergyDemand: EnergyLevel,
    timeline: List<CircadianSnapshot>,
    onShiftToPeakClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (timeline.isEmpty()) return
    val slotSnapshot = timeline.minByOrNull { kotlin.math.abs(it.timestampMillis - taskScheduledMillis) } ?: return
    val isMismatch = taskEnergyDemand == EnergyLevel.HIGH && slotSnapshot.usableEnergy < 52
    val isOptimalFit = (taskEnergyDemand == EnergyLevel.HIGH && slotSnapshot.usableEnergy >= 70) ||
        (taskEnergyDemand == EnergyLevel.LOW && slotSnapshot.phase == EnergyPhase.DIP)
    val badgeColor = when {
        isMismatch -> NeuroFlowPaletteV2.RecoveryRose
        isOptimalFit -> NeuroFlowPaletteV2.OptimalEmerald
        else -> NeuroFlowPaletteV2.ReboundCyan
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor.copy(alpha = 0.12f))
                .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${slotSnapshot.usableEnergy}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = badgeColor
            )
            if (slotSnapshot.focusDepletionPenalty >= 0.04) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(Load -${(slotSnapshot.focusDepletionPenalty * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowPaletteV2.PeakAmber
                )
            }
        }
        if (isMismatch && onShiftToPeakClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.16f))
                    .border(1.dp, NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .clickable { onShiftToPeakClick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠ Dip Mismatch • Shift to Peak",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = NeuroFlowPaletteV2.PeakAmber
                )
            }
        }
    }
}

@Composable
fun UltradianRecoveryDivider(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = NeuroFlowPaletteV2.SurfaceCard,
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("☕", fontSize = 13.sp)
                Text(
                    text = "15m Ultradian Rest Buffer",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.OptimalEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Replenish Prefrontal Glucose",
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowPaletteV2.TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActiveBiophysicalImpulseStrip(
    cognitiveBlocks: List<CognitiveBlockWindow>,
    activePlasmaMg: Double,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val activeBlocks = remember(cognitiveBlocks, now) {
        cognitiveBlocks.filter { block ->
            val start = block.explicitStartMillis ?: return@filter false
            val end = block.explicitEndMillis ?: return@filter false
            now in start..end
        }
    }
    val hasActiveImpulse = activeBlocks.isNotEmpty() || activePlasmaMg > 0.05
    if (!hasActiveImpulse) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = NeuroFlowPaletteV2.SurfaceElevated.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🧬", fontSize = 12.sp)
            Text(
                text = "Active Impulses:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NeuroFlowPaletteV2.TextSecondary
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activePlasmaMg > 0.05) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Stimulant PK (~${activePlasmaMg.roundToInt().coerceAtLeast(1)}mg)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.OptimalEmerald,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                activeBlocks.forEach { block ->
                    val (label: String, color: Color) = when (block.category) {
                        CognitiveBlockCategory.MEAL -> "Postprandial Dip" to NeuroFlowPaletteV2.PeakAmber
                        CognitiveBlockCategory.EXERCISE -> "Post-Exercise BDNF" to NeuroFlowPaletteV2.ReboundCyan
                        CognitiveBlockCategory.SLEEP -> "Rest Recovery" to NeuroFlowPaletteV2.OptimalEmerald
                        else -> block.category.name to NeuroFlowPaletteV2.TextSecondary
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}


