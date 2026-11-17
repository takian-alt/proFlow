package com.neuroflow.app.presentation.v2.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.service.FatigueWarningLevel
import com.neuroflow.app.domain.service.TaskFatigueScoreUi
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Real-time reactive pill badge showing the current Fatigue-Aware priority score
 * for a task, with animated color-coding that escalates to coral/crimson when
 * a high cognitive-load task follows a series of long-duration sessions.
 */
@Composable
fun FatigueAwareScoreBadge(
    score: TaskFatigueScoreUi,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val targetAccent = when (score.warningLevel) {
        FatigueWarningLevel.HIGH_BURNOUT_RISK -> NeuroFlowPaletteV2.RecoveryRose
        FatigueWarningLevel.MODERATE_STRAIN -> NeuroFlowPaletteV2.PeakAmber
        FatigueWarningLevel.RESTORATIVE -> NeuroFlowPaletteV2.OptimalEmerald
        FatigueWarningLevel.OPTIMAL -> NeuroFlowPaletteV2.ReboundCyan
    }
    val accentColor by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 260),
        label = "fatigue_badge_accent"
    )
    val containerColor by animateColorAsState(
        targetValue = targetAccent.copy(
            alpha = when (score.warningLevel) {
                FatigueWarningLevel.HIGH_BURNOUT_RISK -> 0.18f
                FatigueWarningLevel.MODERATE_STRAIN -> 0.15f
                else -> 0.12f
            }
        ),
        animationSpec = tween(durationMillis = 260),
        label = "fatigue_badge_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = targetAccent.copy(
            alpha = if (score.warningLevel == FatigueWarningLevel.HIGH_BURNOUT_RISK) 0.75f else 0.40f
        ),
        animationSpec = tween(durationMillis = 260),
        label = "fatigue_badge_border"
    )

    val icon = when (score.warningLevel) {
        FatigueWarningLevel.HIGH_BURNOUT_RISK -> Icons.Default.WarningAmber
        FatigueWarningLevel.RESTORATIVE -> Icons.Default.Spa
        FatigueWarningLevel.MODERATE_STRAIN,
        FatigueWarningLevel.OPTIMAL -> Icons.Default.Bolt
    }

    val deltaRounded = score.fatigueAdjustment.roundToInt()
    val deltaLabel = when {
        deltaRounded > 0 -> "+$deltaRounded"
        deltaRounded < 0 -> "$deltaRounded"
        else -> null
    }

    val accessibilityLabel = buildString {
        append("Fatigue-aware priority score ${score.priorityScore} out of 100")
        if (deltaLabel != null) append(", adjustment $deltaLabel")
        append(". ${score.warningReason}")
    }

    val shape = RoundedCornerShape(8.dp)
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        color = containerColor,
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .testTag("fatigue_score_badge_${score.taskId}")
            .semantics { contentDescription = accessibilityLabel }
            .clip(shape)
            .then(clickModifier)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = when (score.warningLevel) {
                    FatigueWarningLevel.HIGH_BURNOUT_RISK -> "Burnout risk warning"
                    FatigueWarningLevel.MODERATE_STRAIN -> "Moderate fatigue strain"
                    FatigueWarningLevel.RESTORATIVE -> "Restorative pacing boost"
                    FatigueWarningLevel.OPTIMAL -> "Optimal priority alignment"
                },
                tint = accentColor,
                modifier = Modifier.size(if (compact) 11.dp else 13.dp)
            )

            AnimatedContent(
                targetState = score.priorityScore,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "fatigue_score_number"
            ) { animatedScore ->
                Text(
                    text = if (compact) "FP $animatedScore" else "Score $animatedScore",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = accentColor,
                    maxLines = 1
                )
            }

            if (deltaLabel != null) {
                Text(
                    text = deltaLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = accentColor.copy(alpha = 0.9f),
                    fontSize = if (compact) 9.sp else 10.sp,
                    maxLines = 1
                )
            }

            if (!compact && score.warningLevel == FatigueWarningLevel.HIGH_BURNOUT_RISK) {
                Text(
                    text = "• Burnout Risk",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * High-visibility inline callout strip rendered on task cards when a high cognitive-load task
 * follows a series of long-duration sessions or hits high cumulative fatigue.
 */
@Composable
fun FatigueBurnoutWarningBanner(
    score: TaskFatigueScoreUi,
    modifier: Modifier = Modifier
) {
    val visible = score.warningLevel == FatigueWarningLevel.HIGH_BURNOUT_RISK ||
        (score.warningLevel == FatigueWarningLevel.MODERATE_STRAIN && score.priorLongSessionCount >= 1)

    AnimatedVisibility(visible = visible) {
        val targetColor = if (score.warningLevel == FatigueWarningLevel.HIGH_BURNOUT_RISK) {
            NeuroFlowPaletteV2.RecoveryRose
        } else {
            NeuroFlowPaletteV2.PeakAmber
        }
        val bannerColor by animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(250),
            label = "fatigue_banner_color"
        )

        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(bannerColor.copy(alpha = 0.12f))
                .border(0.75.dp, bannerColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("fatigue_warning_banner_${score.taskId}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Fatigue warning",
                tint = bannerColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = score.warningReason,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = bannerColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Rich breakdown panel inside task detail bottom sheets showing how the Fatigue-Aware
 * priority score is derived from base priority, cumulative long-session load, and burnout risk.
 */
@Composable
fun FatigueScoreDetailCard(
    score: TaskFatigueScoreUi,
    modifier: Modifier = Modifier
) {
    val accentColor = when (score.warningLevel) {
        FatigueWarningLevel.HIGH_BURNOUT_RISK -> NeuroFlowPaletteV2.RecoveryRose
        FatigueWarningLevel.MODERATE_STRAIN -> NeuroFlowPaletteV2.PeakAmber
        FatigueWarningLevel.RESTORATIVE -> NeuroFlowPaletteV2.OptimalEmerald
        FatigueWarningLevel.OPTIMAL -> NeuroFlowPaletteV2.ReboundCyan
    }
    val deltaRounded = score.fatigueAdjustment.roundToInt()
    val deltaStr = when {
        deltaRounded > 0 -> "+$deltaRounded pts"
        deltaRounded < 0 -> "$deltaRounded pts"
        else -> "0 pts"
    }
    val fatiguePercent = (score.fatigueIndex * 100).roundToInt().coerceIn(0, 100)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("fatigue_detail_card_${score.taskId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (score.warningLevel == FatigueWarningLevel.HIGH_BURNOUT_RISK) {
                            Icons.Default.WarningAmber
                        } else {
                            Icons.Default.Bolt
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Fatigue-Aware Priority",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                }
                Text(
                    text = "${score.priorityScore} / 100",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = accentColor
                )
            }

            Text(
                text = score.warningReason,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricMiniLabel("Base Score", "${score.basePriorityScore}", NeuroFlowPaletteV2.TextSecondary)
                MetricMiniLabel("Fatigue Adj", deltaStr, accentColor)
                MetricMiniLabel(
                    "Prior Long Blocks",
                    "${score.priorLongSessionCount} (${score.priorSessionMinutes}m)",
                    NeuroFlowPaletteV2.TextSecondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Burnout & Allostatic Load",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextSecondary
                    )
                    Text(
                        text = "$fatiguePercent% · ${abs(score.cognitiveLoadIntensity).let { "%.2fx".format(it) }} load",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.3f))
                ) {
                    LinearProgressIndicator(
                        progress = { score.fatigueIndex.toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = accentColor,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniLabel(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NeuroFlowPaletteV2.TextTertiary,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = valueColor
        )
    }
}
