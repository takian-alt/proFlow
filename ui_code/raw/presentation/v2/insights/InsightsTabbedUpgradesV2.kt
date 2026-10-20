package com.neuroflow.app.presentation.v2.insights

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EffectivePeakProfile
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import org.json.JSONObject
import kotlin.math.roundToInt

enum class InsightsSubTab(val title: String) {
    RHYTHM("Rhythm & Energy"),
    ESTIMATION("Velocity & Time"),
    FORENSIC_AUDIT("AI Coach & Mindset")
}

@Composable
fun UpgradedInsightsHubV2(
    summary: AnalyticsEngine.AnalyticsSummary,
    effectivePeakProfile: EffectivePeakProfile?,
    planningCalibrationFactor: Float,
    avoidanceAuditJson: String?,
    adaptiveDirectiveText: String?,
    onRunWeeklyAuditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = InsightsSubTab.entries

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NeuroFlowThemeV2Colors.SlateSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { idx, tab ->
                val active = selectedTab == idx
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) NeuroFlowThemeV2Colors.ElectricIndigo else Color.Transparent)
                        .clickable { selectedTab = idx }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium
                        ),
                        color = if (active) Color.White else NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (tabs[selectedTab]) {
            InsightsSubTab.RHYTHM -> RhythmAndConfidenceDigest(
                summary = summary,
                profile = effectivePeakProfile
            )
            InsightsSubTab.ESTIMATION -> TimeCalibrationDigest(
                summary = summary,
                planningCalibrationFactor = planningCalibrationFactor
            )
            InsightsSubTab.FORENSIC_AUDIT -> ForensicAvoidanceAuditDigest(
                avoidanceAuditJson = avoidanceAuditJson,
                adaptiveDirectiveText = adaptiveDirectiveText,
                onRunWeeklyAuditClick = onRunWeeklyAuditClick
            )
        }
    }
}

@Composable
private fun RhythmAndConfidenceDigest(
    summary: AnalyticsEngine.AnalyticsSummary,
    profile: EffectivePeakProfile?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val totalFocus = (summary.peakHourFocusMinutes + summary.offPeakFocusMinutes).coerceAtLeast(1f)
        val peakRatio = (summary.peakHourFocusMinutes / totalFocus).coerceIn(0f, 1f)

        Surface(
            color = NeuroFlowThemeV2Colors.SlateSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CIRCADIAN ALIGNMENT EFFICIENCY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.PeakAmber,
                    letterSpacing = 0.9.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${(peakRatio * 100).roundToInt()}% of Deep Focus in Peak Hours",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = NeuroFlowThemeV2Colors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${summary.peakHourFocusMinutes.roundToInt()}m in peak window · ${summary.offPeakFocusMinutes.roundToInt()}m off-peak · ${summary.peakHourTasksCompleted} peak tasks finished",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )

                if (profile != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile.explanation.ifBlank { "Calibrated to your active chronotype & sleep logs." },
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ConfidenceMetricPill("Sleep Data", profile.confidence.sleepCoverage)
                        ConfidenceMetricPill("Wake Stability", profile.confidence.wakeConsistency)
                        ConfidenceMetricPill("Session Fit", profile.confidence.behaviorPerformance)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfidenceMetricPill(label: String, value: Float) {
    Surface(
        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.OptimalEmerald
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowThemeV2Colors.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TimeCalibrationDigest(
    summary: AnalyticsEngine.AnalyticsSummary,
    planningCalibrationFactor: Float
) {
    Surface(
        color = NeuroFlowThemeV2Colors.SlateSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TIME BLINDNESS & ESTIMATION CALIBRATION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NeuroFlowPaletteV2.ReboundCyan,
                letterSpacing = 0.9.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = AnalyticsEngine.mapeGrade(summary.weightedMape),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NeuroFlowThemeV2Colors.TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    title = "Weighted Error",
                    value = "${summary.weightedMape.roundToInt()}%",
                    accent = NeuroFlowPaletteV2.PeakAmber,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "Auto-Buffer Multiplier",
                    value = "${"%.2f".format(planningCalibrationFactor)}x",
                    accent = NeuroFlowPaletteV2.ReboundCyan,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = "Underestimated",
                    value = "${summary.underestimatedPct.roundToInt()}%",
                    accent = NeuroFlowPaletteV2.RecoveryRose,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowThemeV2Colors.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ForensicAvoidanceAuditDigest(
    avoidanceAuditJson: String?,
    adaptiveDirectiveText: String?,
    onRunWeeklyAuditClick: () -> Unit
) {
    val parsed = remember(avoidanceAuditJson) {
        runCatching {
            if (avoidanceAuditJson.isNullOrBlank()) null else JSONObject(avoidanceAuditJson)
        }.getOrNull()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = NeuroFlowThemeV2Colors.SlateSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FORENSIC AVOIDANCE AUDIT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                    Surface(
                        color = NeuroFlowPaletteV2.DipIndigo,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onRunWeeklyAuditClick() }
                    ) {
                        Text(
                            text = "Refresh Audit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (parsed != null) {
                    AuditRow("Avoidance Pattern", parsed.optString("primaryAvoidancePattern"))
                    AuditRow("Circadian Mismatch", parsed.optString("biologicalCircadianMismatch"))
                    AuditRow("Executive Trigger", parsed.optString("neuropsychologicalTrigger"))
                } else {
                    Text(
                        text = "Run a weekly audit to diagnose postponed tasks and calibrate your 120-second initiation protocols.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
                if (!adaptiveDirectiveText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = adaptiveDirectiveText,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.TextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditRow(label: String, text: String) {
    if (text.isBlank()) return
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = NeuroFlowPaletteV2.PeakAmber
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = NeuroFlowThemeV2Colors.TextPrimary
        )
    }
}
