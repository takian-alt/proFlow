package com.neuroflow.app.presentation.v2.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.domain.service.PriorityWeightProfile
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import kotlin.math.roundToInt

/**
 * 1. Sleep Pressure & Provenance Header Card (For SleepLogsScreen.kt)
 * Displays SleepPressureRepository.Snapshot alongside Health Connect / Manual / Auto-Default provenance.
 */
@Composable
fun UpgradedSleepPressureOverviewCard(
    snapshot: SleepPressureRepository.Snapshot,
    hasHealthConnectSync: Boolean,
    onSyncHealthConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.DipIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HOMEOSTATIC SLEEP PRESSURE (PROCESS S)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                }
                Surface(
                    color = if (hasHealthConnectSync)
                        NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.15f)
                    else
                        NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onSyncHealthConnectClick() }
                ) {
                    Text(
                        text = if (hasHealthConnectSync) "Health Connect Active" else "Sync Wearable",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (hasHealthConnectSync) NeuroFlowPaletteV2.OptimalEmerald else NeuroFlowPaletteV2.PeakAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SleepStatColumn("Fatigue Zone", snapshot.adjustedFatigueZone.label, NeuroFlowPaletteV2.PeakAmber)
                SleepStatColumn("7d Debt Mult", "${"%.2f".format(snapshot.chronicRestrictionMultiplier)}x", NeuroFlowPaletteV2.RecoveryRose)
                SleepStatColumn(
                    "Sleep Inertia",
                    if (snapshot.isSleepInertiaActive) "Active (-${(snapshot.sleepInertiaPenalty * 100).roundToInt()}%)" else "Cleared",
                    NeuroFlowPaletteV2.ReboundCyan
                )
            }
        }
    }
}

@Composable
private fun SleepStatColumn(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = color
        )
    }
}

/**
 * 2. Protected Slot Physiological Effect Selector (For ProtectedSlotBottomSheet.kt & WeeklyScheduleScreen.kt)
 * Explains how Meal, Exercise, and Mindfulness slots dynamically alter the circadian energy curve.
 */
@Composable
fun ProtectedSlotPhysiologicalPreviewBar(
    selectedCategory: PtsCategory,
    onSelectCategory: (PtsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        Triple(PtsCategory.MEAL, "Meal (-8.5% Dip)", Icons.Default.Restaurant),
        Triple(PtsCategory.EXERCISE, "Exercise (+9.5% Boost)", Icons.Default.FitnessCenter),
        Triple(PtsCategory.MINDFULNESS, "NSDR (2.2x Recovery)", Icons.Default.SelfImprovement)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "PHYSIOLOGICAL CURVE IMPACT",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.65f),
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (category, label, icon) ->
                val isSelected = selectedCategory == category
                Surface(
                    color = if (isSelected) NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.18f) else NeuroFlowPaletteV2.SurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) NeuroFlowPaletteV2.ReboundCyan else NeuroFlowPaletteV2.BorderSubtle
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectCategory(category) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) NeuroFlowPaletteV2.ReboundCyan else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) NeuroFlowPaletteV2.ReboundCyan else Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Scheduler Priority Weights Tuner Card (For PriorityWeightsScreen.kt)
 * Interactive normalized weight sliders for TaskSchedulerService.PriorityWeightProfile.
 */
@Composable
fun UpgradedPriorityWeightsTunerCard(
    weights: PriorityWeightProfile,
    onUpdateWeights: (PriorityWeightProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AUTO-SCHEDULER SCORING WEIGHTS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.PeakAmber,
                letterSpacing = 0.9.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            WeightSliderRow(
                label = "Deadline Urgency",
                value = weights.urgencyWeight.toFloat(),
                accent = NeuroFlowPaletteV2.RecoveryRose,
                onValueChange = { onUpdateWeights(weights.copy(urgencyWeight = it.toDouble())) }
            )
            WeightSliderRow(
                label = "Strategic Impact & Frog",
                value = weights.impactWeight.toFloat(),
                accent = NeuroFlowPaletteV2.PeakAmber,
                onValueChange = { onUpdateWeights(weights.copy(impactWeight = it.toDouble())) }
            )
            WeightSliderRow(
                label = "Circadian Energy Match",
                value = weights.circadianMatchWeight.toFloat(),
                accent = NeuroFlowPaletteV2.ReboundCyan,
                onValueChange = { onUpdateWeights(weights.copy(circadianMatchWeight = it.toDouble())) }
            )
            WeightSliderRow(
                label = "Low-Friction Momentum",
                value = weights.momentumWeight.toFloat(),
                accent = NeuroFlowPaletteV2.OptimalEmerald,
                onValueChange = { onUpdateWeights(weights.copy(momentumWeight = it.toDouble())) }
            )
        }
    }
}

@Composable
private fun WeightSliderRow(
    label: String,
    value: Float,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Text(
                text = "${"%.1f".format(value)}x",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.2f..2.5f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent
            )
        )
    }
}

/**
 * 4. Minimalist HyperFocus Home Launcher & Cloud Sync Status Bar
 * (For HyperFocusHomeScreen.kt, GateTaskScreen.kt & AccountSyncScreen.kt)
 */
@Composable
fun HyperFocusLauncherStatusHeader(
    sessionModeLabel: String,
    remainingTasksOrMinutesLabel: String,
    earnedUnlockMinutes: Int,
    cloudBackupSynced: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF07080C),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = sessionModeLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = remainingTasksOrMinutesLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+${earnedUnlockMinutes}m Earned",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.OptimalEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (cloudBackupSynced) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Synced",
                        tint = NeuroFlowPaletteV2.OptimalEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
