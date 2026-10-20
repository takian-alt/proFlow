package com.neuroflow.app.presentation.v2.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.inference.InferredTaskMetadata
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.service.EnergyPhase
import kotlin.math.roundToInt

import androidx.compose.runtime.ReadOnlyComposable

object NeuroFlowPaletteV2 {
    val BackgroundDeep: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.background
    val SurfaceCard: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.surface
    val SurfaceElevated: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.surfaceElevated
    val BorderSubtle: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.border.copy(alpha = 0.5f)

    val PeakAmber: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.energyModerateAmber
    val ReboundCyan: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.primaryIndigo
    val DipIndigo: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.hyperFocusViolet
    val WindDownSlate: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.textSecondary
    val RecoveryRose: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.energyLowCoral
    val OptimalEmerald: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.energyPeakMint

    @Composable
    @ReadOnlyComposable
    fun accentForPhase(phase: EnergyPhase): Color = when (phase) {
        EnergyPhase.PEAK -> PeakAmber
        EnergyPhase.REBOUND -> ReboundCyan
        EnergyPhase.DIP -> DipIndigo
        EnergyPhase.WIND_DOWN -> WindDownSlate
        EnergyPhase.RECOVERY_NEEDED -> RecoveryRose
    }
}

/**
 * 1. Phase-Adaptive Ambient Backdrop
 * Wrap top-level screen content in this Box so the top aura shifts subtly with the user's biological phase.
 */
@Composable
fun PhaseAmbientScreenContainer(
    phase: EnergyPhase,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val targetAccent by animateColorAsState(
        targetValue = NeuroFlowPaletteV2.accentForPhase(phase),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "phase_ambient_color"
    )

    Box(
        modifier = modifier
            .background(NeuroFlowPaletteV2.BackgroundDeep)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        targetAccent.copy(alpha = 0.11f),
                        targetAccent.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 680f
                )
            )
    ) {
        content()
    }
}

/**
 * 2. Daily Cognitive Capacity Meter (For PlanScreenV2 & FlowScreenV2)
 * Prevents ADHD over-scheduling by visualizing scheduled load against recommended deep-work capacity.
 */
@Composable
fun CognitiveBudgetMeterCard(
    scheduledMinutesToday: Int,
    recommendedCapacityMinutes: Int,
    conflictCount: Int = 0,
    onAutoBalanceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val safeCapacity = recommendedCapacityMinutes.coerceAtLeast(60)
    val ratio = (scheduledMinutesToday.toFloat() / safeCapacity.toFloat()).coerceIn(0f, 1.25f)
    val animatedProgress by animateFloatAsState(
        targetValue = ratio.coerceAtMost(1f),
        animationSpec = tween(durationMillis = 500),
        label = "capacity_progress"
    )

    val isOverbooked = scheduledMinutesToday > safeCapacity
    val barColor = when {
        isOverbooked || conflictCount > 0 -> NeuroFlowPaletteV2.RecoveryRose
        ratio >= 0.85f -> NeuroFlowPaletteV2.PeakAmber
        else -> NeuroFlowPaletteV2.ReboundCyan
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NeuroFlowPaletteV2.SurfaceCard)
            .border(1.dp, NeuroFlowPaletteV2.BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isOverbooked || conflictCount > 0) Icons.Default.WarningAmber else Icons.Default.Bolt,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOverbooked) "COGNITIVE OVERLOAD RISK" else "DAILY COGNITIVE BUDGET",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "${scheduledMinutesToday}m / ${safeCapacity}m",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor)
            )
        }

        if ((isOverbooked || conflictCount > 0) && onAutoBalanceClick != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val warningText = when {
                    conflictCount > 0 -> "$conflictCount schedule overlap(s) detected."
                    else -> "Booked ${scheduledMinutesToday - safeCapacity}m past your high-focus budget."
                }
                Text(
                    text = warningText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = barColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, barColor.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onAutoBalanceClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = barColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Auto-Rebalance",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = barColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Live NLP Inference Token Strip (For SmartTaskComposerSheetV2)
 * Surfaces TaskInferenceEngine's real-time parsing as interactive pills while the user types.
 */
@Composable
fun LiveNlpInferenceStrip(
    inferred: InferredTaskMetadata,
    rawInputNotEmpty: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = rawInputNotEmpty,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Duration + Calibration Pill
            val isCalibrated = !inferred.isDurationExplicit && kotlin.math.abs(inferred.calibrationFactorApplied - 1.0f) >= 0.08f
            val durationLabel = if (isCalibrated) {
                "⏱ ${inferred.durationMinutes}m (${(inferred.calibrationFactorApplied * 100).roundToInt()}% bias-adjusted)"
            } else {
                "⏱ ${inferred.durationMinutes}m"
            }
            NlpTokenChip(
                text = durationLabel,
                accent = NeuroFlowPaletteV2.ReboundCyan
            )

            // Energy Demand Pill
            val (energyText, energyColor) = when (inferred.energyDemand) {
                EnergyLevel.HIGH -> "⚡ High Energy" to NeuroFlowPaletteV2.PeakAmber
                EnergyLevel.MEDIUM -> "⚡ Med Energy" to NeuroFlowPaletteV2.ReboundCyan
                EnergyLevel.LOW -> "⚡ Low Energy" to NeuroFlowPaletteV2.OptimalEmerald
            }
            NlpTokenChip(text = energyText, accent = energyColor)

            // Frog Badge
            if (inferred.isFrog) {
                NlpTokenChip(text = "🐸 Daily Frog", accent = NeuroFlowPaletteV2.PeakAmber)
            }

            // Priority Badge (if elevated)
            if (inferred.priority == Priority.HIGH) {
                NlpTokenChip(text = "🔥 P1 High", accent = NeuroFlowPaletteV2.RecoveryRose)
            }

            // Context Tags
            inferred.contextTags.forEach { tag ->
                NlpTokenChip(text = "#$tag", accent = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
private fun NlpTokenChip(
    text: String,
    accent: Color
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.32f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * 4. Persistent Mini-Focus Session Bar (For NeuroFlowAppV2 shell when navigating away from Focus tab)
 */
@Composable
fun MiniFocusSessionBar(
    remainingSeconds: Long,
    totalSeconds: Long,
    taskTitle: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = (remainingSeconds / 60).coerceAtLeast(0)
    val secs = (remainingSeconds % 60).coerceAtLeast(0)
    val timeFormatted = "%02d:%02d".format(mins, secs)
    MiniFocusSessionBar(
        activeTaskTitle = taskTitle,
        remainingTimeFormatted = timeFormatted,
        currentMicroStepTitle = null,
        onReturnToFocusClick = onTap,
        modifier = modifier
    )
}

@Composable
fun MiniFocusSessionBar(
    activeTaskTitle: String,
    remainingTimeFormatted: String,
    currentMicroStepTitle: String?,
    onReturnToFocusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.4f)
        ),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onReturnToFocusClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeuroFlowPaletteV2.OptimalEmerald)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = activeTaskTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!currentMicroStepTitle.isNullOrBlank()) {
                        Text(
                            text = "Step: $currentMicroStepTitle",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = NeuroFlowPaletteV2.ReboundCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = remainingTimeFormatted,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = NeuroFlowPaletteV2.ReboundCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Return to Focus",
                    tint = NeuroFlowPaletteV2.ReboundCyan,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
