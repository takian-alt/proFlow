package com.neuroflow.app.presentation.v2.common

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import kotlin.math.roundToInt

@Composable
fun UpgradedUlyssesContractCard(
    taskTitle: String,
    commitmentStatement: String,
    deadlineFormatted: String,
    consequenceText: String,
    onSignContractClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.5.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.PeakAmber, 1f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = NeuroFlowPaletteV2.PeakAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ULYSSES PRE-COMMITMENT PACT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = NeuroFlowPaletteV2.PeakAmber,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = taskTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"$commitmentStatement\"",
                style = MaterialTheme.typography.bodyMedium,
                color = NeuroFlowPaletteV2.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeuroFlowPaletteV2.SurfaceElevated)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LOCKED DEADLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextTertiary
                    )
                    Text(
                        text = deadlineFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "IF MISSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextTertiary
                    )
                    Text(
                        text = consequenceText.ifBlank { "Mandatory schedule lock" },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.RecoveryRose,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSignContractClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeuroFlowPaletteV2.PeakAmber,
                    contentColor = Color(0xFF0A0B0E)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Seal Ulysses Contract",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

@Composable
fun UpgradedIdentityAndGoalsCard(
    identityStatement: String,
    activeGoals: List<String>,
    onEditIdentityClick: () -> Unit,
    onRefillGoalsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.ReboundCyan, 0.85f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "IDENTITY & NORTH-STAR GOALS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        letterSpacing = 0.9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.ReboundCyan,
                    modifier = Modifier.clickable { onEditIdentityClick() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = identityStatement.ifBlank { "I am someone who protects my peak focus hours and finishes what matters." },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NeuroFlowPaletteV2.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (activeGoals.isEmpty()) {
                Surface(
                    color = NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRefillGoalsClick() }
                ) {
                    Text(
                        text = "+ Set your Top 3 Weekly Focus Goals",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.PeakAmber,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                activeGoals.take(3).forEachIndexed { index, goal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.22f))
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeuroFlowPaletteV2.DipIndigo
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = goal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeuroFlowPaletteV2.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradedHistorySessionRow(
    taskTitle: String,
    dateAndTimeText: String,
    durationMinutes: Float,
    estimatedMinutes: Int,
    interruptionCount: Int,
    affectiveForecastError: Float?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush()),
        modifier = modifier.fillMaxWidth()
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = taskTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dateAndTimeText · Est ${estimatedMinutes}m → Actual ${durationMinutes.roundToInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (interruptionCount == 0) {
                    Surface(
                        color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Unbroken",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.OptimalEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        color = NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${interruptionCount}x Int",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.RecoveryRose,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                if (affectiveForecastError != null) {
                    val sign = if (affectiveForecastError > 0f) "+" else ""
                    Surface(
                        color = NeuroFlowPaletteV2.SurfaceElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$sign${"%.1f".format(affectiveForecastError)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.PeakAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradedNoteCardV2(
    title: String,
    contentPreview: String,
    noteAccentColor: Color,
    updatedAtFormatted: String,
    onNoteClick: () -> Unit,
    onExtractTaskWithAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            NeuroFlowPaletteV2.specularBorderBrush(noteAccentColor, 0.85f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNoteClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = noteAccentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onExtractTaskWithAiClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddTask,
                            contentDescription = "Extract Task",
                            tint = noteAccentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "To Task",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = noteAccentColor
                        )
                    }
                }
            }
            if (contentPreview.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = contentPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = updatedAtFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowPaletteV2.TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}
