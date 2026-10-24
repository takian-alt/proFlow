package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.ScheduleConflict
import com.neuroflow.app.domain.service.TaskEnrichmentResult
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2

@Composable
fun ScheduleConflictResolverStrip(
    conflicts: List<ScheduleConflict>,
    onResolveConflictClick: (ScheduleConflict) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = conflicts.isNotEmpty(), modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            conflicts.take(2).forEach { conflict ->
                Surface(
                    color = NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.RecoveryRose, 0.9f)
                    ),
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
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = NeuroFlowPaletteV2.RecoveryRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = conflict.taskTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = NeuroFlowPaletteV2.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = conflict.conflictDescription,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowPaletteV2.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (conflict.suggestedNewStartMinute != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = NeuroFlowPaletteV2.RecoveryRose,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { onResolveConflictClick(conflict) }
                            ) {
                                Text(
                                    text = "Shift to ${ConflictResolver.formatMinutesToTime(conflict.suggestedNewStartMinute)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactEisenhowerMatrixFilter(
    countsByQuadrant: Map<Quadrant, Int>,
    selectedQuadrant: Quadrant?,
    onSelectQuadrant: (Quadrant?) -> Unit,
    modifier: Modifier = Modifier
) {
    val quadrants = listOf(
        Triple(Quadrant.DO_FIRST, "Do First", NeuroFlowPaletteV2.RecoveryRose),
        Triple(Quadrant.SCHEDULE, "Schedule", NeuroFlowPaletteV2.ReboundCyan),
        Triple(Quadrant.DELEGATE, "Quick / Delegate", NeuroFlowPaletteV2.PeakAmber),
        Triple(Quadrant.ELIMINATE, "Someday / Cut", NeuroFlowPaletteV2.WindDownSlate)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            quadrants.take(2).forEach { (quad, title, accent) ->
                QuadrantFilterTile(
                    title = title,
                    count = countsByQuadrant[quad] ?: 0,
                    accent = accent,
                    isSelected = selectedQuadrant == quad,
                    onClick = { onSelectQuadrant(if (selectedQuadrant == quad) null else quad) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            quadrants.drop(2).forEach { (quad, title, accent) ->
                QuadrantFilterTile(
                    title = title,
                    count = countsByQuadrant[quad] ?: 0,
                    accent = accent,
                    isSelected = selectedQuadrant == quad,
                    onClick = { onSelectQuadrant(if (selectedQuadrant == quad) null else quad) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuadrantFilterTile(
    title: String,
    count: Int,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) accent.copy(alpha = 0.16f) else NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            brush = NeuroFlowPaletteV2.specularBorderBrush(
                if (isSelected) accent else NeuroFlowPaletteV2.BorderSubtle,
                if (isSelected) 1f else 0.7f
            )
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) accent else NeuroFlowPaletteV2.TextPrimary,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
            }
        }
    }
}

@Composable
fun ComposerAiEnrichmentPreviewCard(
    enrichment: TaskEnrichmentResult?,
    isLoading: Boolean,
    onApplyEnrichmentClick: (TaskEnrichmentResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isLoading && enrichment == null) return

    Surface(
        color = NeuroFlowPaletteV2.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.ReboundCyan, 0.85f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLoading) "BREAKING DOWN INITIATION FRICTION..." else "AI EXECUTIVE FUNCTION BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (enrichment != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onApplyEnrichmentClick(enrichment) }
                    ) {
                        Text(
                            text = "Apply Steps",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (enrichment != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 ${enrichment.activationTip}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                enrichment.microSteps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}. ${step.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (idx == 0) NeuroFlowPaletteV2.OptimalEmerald else NeuroFlowPaletteV2.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${step.estimatedMinutes}m",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
