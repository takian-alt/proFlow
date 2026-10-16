package com.neuroflow.app.presentation.v2.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.domain.engine.SleepPressureDetector
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

@Composable
fun InsightsScreenV2(
    viewModel: InsightsViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = NeuroFlowThemeV2Colors.Obsidian
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Neuro Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    Text(
                        text = "Borbély Two-Process Model & Cognitive Biometrics",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = NeuroFlowThemeV2Colors.WarmAmber, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${state.dailyStreak}d Streak",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.WarmAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Two-Process Model Deep Dive Card
            BorbelyTwoProcessDeepDiveCard(state = state)

            Spacer(modifier = Modifier.height(14.dp))

            // Circadian Synchrony & Focus Efficiency Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circadian Alignment
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎯 Circadian Match", style = MaterialTheme.typography.labelMedium, color = NeuroFlowThemeV2Colors.TextSecondary)
                        Text(
                            text = "${state.circadianAlignmentRate}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = NeuroFlowThemeV2Colors.SageGreen
                        )
                        LinearProgressIndicator(
                            progress = { state.circadianAlignmentRate / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = NeuroFlowThemeV2Colors.SageGreen,
                            trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                        )
                        Text("High-effort tasks in peak window", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary, fontSize = 11.sp)
                    }
                }

                // Focus Efficiency
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⚡ Flow Efficiency", style = MaterialTheme.typography.labelMedium, color = NeuroFlowThemeV2Colors.TextSecondary)
                        Text(
                            text = "${state.focusEfficiencyPercent}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                        LinearProgressIndicator(
                            progress = { state.focusEfficiencyPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = NeuroFlowThemeV2Colors.ElectricIndigo,
                            trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                        )
                        Text("Active time vs pause penalty", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interruption & Distraction Metrics
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🧠 Cognitive Friction & Interruption Radar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricTile(label = "App Switches", value = "${state.interruptionAppSwitchCount}", subtitle = "Context jumps today")
                        MetricTile(label = "Pauses", value = "${state.interruptionPauseResumeCount}", subtitle = "Flow interruptions")
                        MetricTile(label = "Burst Interr.", value = "${state.interruptionBurstCount}", subtitle = "Rapid derailments")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Daily & Weekly Momentum Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🏆 Executive Momentum & Habits",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricTile(label = "Focus Today", value = "${state.totalFocusMinutesToday}m", subtitle = "Deep work logged")
                        MetricTile(label = "Focus 7d", value = "${state.totalFocusMinutesWeek}m", subtitle = "Weekly volume")
                        MetricTile(label = "Frogs Eaten", value = "${state.frogsEatenCount}", subtitle = "Crucial priorities")
                    }
                }
            }

            // Actionable Neuroscience Interventions
            if (state.neuroscienceInterventions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔬 Evidence-Based Recommendations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )

                    state.neuroscienceInterventions.forEach { tip ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                        ) {
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun BorbelyTwoProcessDeepDiveCard(state: InsightsUiStateV2) {
    val pressure = state.energyUi?.sleepPressurePoints ?: 450
    val zone = state.energyUi?.fatigueZone ?: SleepPressureDetector.FatigueZone.RESTED
    val circadianFactor = state.energyUi?.circadianFactor ?: 0.85f
    val battery = state.energyUi?.availableEnergy ?: 75

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                    Text("Borbély Two-Process Model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "$battery% Usable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = when {
                        battery >= 65 -> NeuroFlowThemeV2Colors.SageGreen
                        battery >= 35 -> NeuroFlowThemeV2Colors.WarmAmber
                        else -> NeuroFlowThemeV2Colors.RoseCoral
                    }
                )
            }

            // Process C (Circadian Alertness)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Process C (Circadian Alertness): ${(circadianFactor * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                    Text("Suprachiasmatic Nucleus", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                }
                LinearProgressIndicator(
                    progress = { circadianFactor.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                    trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                )
            }

            // Process S (Homeostatic Sleep Pressure)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Process S (Sleep Pressure): $pressure pts", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = NeuroFlowThemeV2Colors.WarmAmber)
                    Text("Zone: ${zone.name}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = when (zone) {
                        SleepPressureDetector.FatigueZone.RESTED -> NeuroFlowThemeV2Colors.SageGreen
                        SleepPressureDetector.FatigueZone.MODERATE -> NeuroFlowThemeV2Colors.WarmAmber
                        else -> NeuroFlowThemeV2Colors.RoseCoral
                    })
                }
                val pressureFraction = (pressure / 3000f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { pressureFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = when (zone) {
                        SleepPressureDetector.FatigueZone.RESTED -> NeuroFlowThemeV2Colors.SageGreen
                        SleepPressureDetector.FatigueZone.MODERATE -> NeuroFlowThemeV2Colors.WarmAmber
                        else -> NeuroFlowThemeV2Colors.RoseCoral
                    },
                    trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                )
            }

            Text(
                text = "Chronotype: ${state.preferences.manualChronotype?.replace('_', ' ') ?: "Intermediate"} • Peak hours: ${String.format("%02d:00", state.preferences.peakEnergyStart)} - ${String.format("%02d:00", state.preferences.peakEnergyEnd)}",
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowThemeV2Colors.TextSecondary
            )
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = NeuroFlowThemeV2Colors.TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary, fontSize = 10.sp)
    }
}
