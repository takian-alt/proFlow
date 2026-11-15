package com.neuroflow.app.presentation.v2.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

object NeuroFlowPaletteV2 {
    val BackgroundDeep: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.background
    val SurfaceCard: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.surface
    val SurfaceElevated: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.surfaceElevated
    val BorderSubtle: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.border.copy(alpha = 0.55f)

    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.textPrimary
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.textSecondary
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current.textTertiary

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

    /**
     * Option 3A: Melatonin-protective warm sunset amber accent for evening wind-down.
     * Prevents cold blue/indigo wavelength exposure and supports natural circadian melatonin secretion.
     */
    val WindDownAmber: Color = Color(0xFFD97706)
    val MelatoninWarmAmber: Color = Color(0xFFD97706)

    fun staticAccentForPhase(phase: EnergyPhase): Color = when (phase) {
        EnergyPhase.SURGE -> Color(0xFF06B6D4)
        EnergyPhase.PEAK -> Color(0xFF10B981)
        EnergyPhase.REBOUND -> Color(0xFF6366F1)
        EnergyPhase.DIP -> Color(0xFFF59E0B)
        EnergyPhase.WIND_DOWN -> WindDownAmber
        EnergyPhase.TROUGH -> Color(0xFF1E293B)
        EnergyPhase.RECOVERY -> Color(0xFF14B8A6)
        EnergyPhase.RECOVERY_NEEDED -> Color(0xFFE11D48)
        EnergyPhase.INERTIA -> Color(0xFFA78BFA)
        EnergyPhase.CRASH -> Color(0xFFEA580C)
    }

    @Composable
    @ReadOnlyComposable
    fun accentForPhase(phase: EnergyPhase): Color = when (phase) {
        EnergyPhase.SURGE -> Color(0xFF06B6D4)
        EnergyPhase.PEAK -> OptimalEmerald
        EnergyPhase.REBOUND -> ReboundCyan
        EnergyPhase.DIP -> PeakAmber
        EnergyPhase.WIND_DOWN -> WindDownAmber
        EnergyPhase.TROUGH -> Color(0xFF1E293B)
        EnergyPhase.RECOVERY -> Color(0xFF14B8A6)
        EnergyPhase.RECOVERY_NEEDED -> RecoveryRose
        EnergyPhase.INERTIA -> Color(0xFFA78BFA)
        EnergyPhase.CRASH -> Color(0xFFEA580C)
    }

    /**
     * Top-lit specular border brush that makes cards look tactile in Dark, AMOLED, and Light themes.
     */
    @Composable
    @ReadOnlyComposable
    fun specularBorderBrush(accent: Color = BorderSubtle, alphaBoost: Float = 1f): Brush {
        val isDark = LocalNeuroFlowPalette.current.isDark
        val topHighlight = if (isDark) {
            accent.copy(alpha = (0.42f * alphaBoost).coerceIn(0f, 1f))
        } else {
            accent.copy(alpha = (0.28f * alphaBoost).coerceIn(0f, 1f))
        }
        val bottomShadow = BorderSubtle.copy(alpha = if (isDark) 0.16f else 0.35f)
        return Brush.verticalGradient(listOf(topHighlight, bottomShadow))
    }
}

/**
 * 1. Phase-Adaptive Ambient Backdrop
 */
@Composable
fun PhaseAmbientScreenContainer(
    phase: EnergyPhase,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = LocalNeuroFlowPalette.current.isDark
    val targetAccent by animateColorAsState(
        targetValue = NeuroFlowPaletteV2.accentForPhase(phase),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "phase_ambient_color"
    )

    val topAlpha = if (isDark) 0.14f else 0.08f
    val midAlpha = if (isDark) 0.04f else 0.02f

    Box(
        modifier = modifier
            .background(NeuroFlowPaletteV2.BackgroundDeep)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        targetAccent.copy(alpha = topAlpha),
                        targetAccent.copy(alpha = midAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 760f
                )
            )
    ) {
        content()
    }
}

/**
 * 2. Daily Cognitive Capacity Meter
 */
@Composable
fun CognitiveBudgetMeterCard(
    scheduledMinutesToday: Int,
    recommendedCapacityMinutes: Int,
    completedMinutesToday: Int = 0,
    conflictCount: Int = 0,
    onAutoBalanceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalCommittedMinutes = (scheduledMinutesToday + completedMinutesToday).coerceAtLeast(0)
    val safeCapacity = recommendedCapacityMinutes.coerceAtLeast(60)
    val ratio = (totalCommittedMinutes.toFloat() / safeCapacity.toFloat()).coerceIn(0f, 2.0f)
    val completedRatio = (completedMinutesToday.toFloat() / safeCapacity.toFloat()).coerceIn(0f, 1f)
    val animatedTotalProgress by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "capacity_progress"
    )
    val animatedCompletedProgress by animateFloatAsState(
        targetValue = completedRatio,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "completed_progress"
    )

    val isOverbooked = totalCommittedMinutes > safeCapacity
    val barColor = when {
        isOverbooked -> NeuroFlowPaletteV2.RecoveryRose
        ratio >= 0.85f -> NeuroFlowPaletteV2.PeakAmber
        else -> NeuroFlowPaletteV2.OptimalEmerald
    }

    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NeuroFlowPaletteV2.SurfaceCard)
            .border(1.dp, NeuroFlowPaletteV2.specularBorderBrush(barColor, 0.75f), shape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(barColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOverbooked) Icons.Default.WarningAmber else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isOverbooked) "COGNITIVE OVERLOAD RISK" else "DAILY COGNITIVE BUDGET",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextSecondary,
                        letterSpacing = 0.9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (completedMinutesToday > 0) {
                        Text(
                            text = "${completedMinutesToday}m done · ${scheduledMinutesToday}m active",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${totalCommittedMinutes}m / ${safeCapacity}m",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.25f))
        ) {
            if (animatedTotalProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedTotalProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(barColor.copy(alpha = 0.65f), barColor)
                            )
                        )
                )
            }
            if (animatedCompletedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedCompletedProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.85f), NeuroFlowPaletteV2.OptimalEmerald)
                            )
                        )
                )
            }
        }

        if ((isOverbooked || conflictCount > 0) && onAutoBalanceClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val warningText = when {
                    isOverbooked && conflictCount > 0 -> "Overbooked by ${totalCommittedMinutes - safeCapacity}m · $conflictCount overlap(s)"
                    isOverbooked -> "Booked ${totalCommittedMinutes - safeCapacity}m past your high-focus budget."
                    conflictCount > 0 -> "$conflictCount schedule overlap(s) detected."
                    else -> ""
                }
                val actionColor = if (isOverbooked) NeuroFlowPaletteV2.RecoveryRose else if (conflictCount > 0) NeuroFlowPaletteV2.PeakAmber else barColor
                Text(
                    text = warningText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuroFlowPaletteV2.TextSecondary,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = actionColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, actionColor.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onAutoBalanceClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Auto-Rebalance",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = actionColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Live NLP Inference Token Strip
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
            val isCalibrated = !inferred.isDurationExplicit && kotlin.math.abs(inferred.calibrationFactorApplied - 1.0f) >= 0.08f
            val durationLabel = if (isCalibrated) {
                "⏱ ${inferred.durationMinutes}m (${(inferred.calibrationFactorApplied * 100).roundToInt()}% calibrated)"
            } else {
                "⏱ ${inferred.durationMinutes}m"
            }
            NlpTokenChip(
                text = durationLabel,
                accent = NeuroFlowPaletteV2.ReboundCyan
            )

            val (energyText, energyColor) = when (inferred.energyDemand) {
                EnergyLevel.HIGH -> "⚡ High Energy" to NeuroFlowPaletteV2.PeakAmber
                EnergyLevel.MEDIUM -> "⚡ Med Energy" to NeuroFlowPaletteV2.ReboundCyan
                EnergyLevel.LOW -> "⚡ Low Energy" to NeuroFlowPaletteV2.OptimalEmerald
            }
            NlpTokenChip(text = energyText, accent = energyColor)

            if (inferred.isFrog) {
                NlpTokenChip(text = "🐸 Daily Frog", accent = NeuroFlowPaletteV2.PeakAmber)
            }

            if (inferred.priority == Priority.HIGH) {
                NlpTokenChip(text = "🔥 P1 High", accent = NeuroFlowPaletteV2.RecoveryRose)
            }

            inferred.contextTags.forEach { tag ->
                NlpTokenChip(text = "#$tag", accent = NeuroFlowPaletteV2.TextSecondary)
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
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

/**
 * 4. Persistent Mini-Focus Session Bar with Live Progress Track & Breathing Pulse
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
    val progress = if (totalSeconds > 0) {
        ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    MiniFocusSessionBar(
        activeTaskTitle = taskTitle,
        remainingTimeFormatted = timeFormatted,
        currentMicroStepTitle = null,
        progress = progress,
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
    modifier: Modifier = Modifier,
    progress: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot"
    )

    val shape = RoundedCornerShape(18.dp)

    Surface(
        color = NeuroFlowPaletteV2.SurfaceElevated,
        shape = shape,
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.OptimalEmerald, 0.9f)
        ),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(shape)
            .clickable { onReturnToFocusClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.28f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeuroFlowPaletteV2.OptimalEmerald)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = activeTaskTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!currentMicroStepTitle.isNullOrBlank()) {
                            Text(
                                text = "Step: $currentMicroStepTitle",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowPaletteV2.TextSecondary,
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
                        .background(NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.14f))
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = remainingTimeFormatted,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
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

            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        NeuroFlowPaletteV2.ReboundCyan,
                                        NeuroFlowPaletteV2.OptimalEmerald
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
