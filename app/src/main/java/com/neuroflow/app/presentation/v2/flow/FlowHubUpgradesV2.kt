package com.neuroflow.app.presentation.v2.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShieldMoon
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
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2

/**
 * Spotlight Hero Card for FlowScreenV2:
 * Automatically switches between a running ProtectedTimeSlot takeover (Meal, Exercise, Mindfulness)
 * and the highest-affinity Next Action / Daily Frog task so the user never faces choice paralysis.
 */
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
    modifier: Modifier = Modifier
) {
    val phaseAccent = NeuroFlowPaletteV2.accentForPhase(currentPhase)
    if (activeTakeoverSlot != null) {
        Surface(
            color = NeuroFlowPaletteV2.SurfaceCard,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.45f)),
            modifier = modifier.fillMaxWidth()
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
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "${activeTakeoverSlot.minutesLeft}m left",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activeTakeoverSlot.slot.label,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deep work is paused to protect your ${activeTakeoverSlot.slot.category.name.lowercase()} window and discharge synaptic fatigue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }
        }
        return
    }

    if (topTaskId == null || topTaskTitle.isNullOrBlank()) return

    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, phaseAccent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(phaseAccent.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isTopTaskFrog) "🐸 DAILY FROG SPOTLIGHT" else "⚡ OPTIMAL NEXT ACTION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = phaseAccent,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${topTaskDurationMinutes}m · ${topTaskEnergyLevel.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = topTaskTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 2-Minute Activation Doorway preview
            if (!topTaskFirstMicroStep.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2m Starter:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = topTaskFirstMicroStep,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onStartFocusClick(topTaskId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = phaseAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
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
                    color = Color.White.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier
                        .height(46.dp)
                        .clickable { onCompleteQuickClick(topTaskId) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
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
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * One-Tap Guilt-Free Schedule Recovery Banner powered by FreshStartEngine.buildEmergencyResetPlan()
 */
@Composable
fun FreshStartRecoveryBanner(
    visible: Boolean,
    resetPlan: FreshStartEngine.EmergencyResetPlan?,
    onApplyEmergencyReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && resetPlan != null && resetPlan.tasksPushedToBacklog.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val plan = resetPlan ?: return@AnimatedVisibility
        Surface(
            color = NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.14f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    Column {
                        Text(
                            text = "Schedule Feeling Heavy?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Keep ${plan.tasksToReschedule.size} essential task(s) today, move ${plan.tasksPushedToBacklog.size} to backlog, and freeze your streak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
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
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fresh Start",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the projected usable energy at a task's scheduled start time,
 * any active synaptic/meal/exercise delta, and a 1-tap "Shift to Peak" chip
 * if a HIGH-energy task is scheduled during an energy dip (< 50%).
 */
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
        isMismatch -> Color(0xFFFF5252)
        isOptimalFit -> Color(0xFF00E676)
        else -> Color(0xFF00E5FF)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Slot Usable Energy Pill
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
                    color = Color(0xFFFF8A65)
                )
            }
        }
        // Actionable Mismatch Warning Chip
        if (isMismatch && onShiftToPeakClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFB300).copy(alpha = 0.16f))
                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .clickable { onShiftToPeakClick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠ Dip Mismatch • Shift to Peak",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFFFB300)
                )
            }
        }
    }
}
