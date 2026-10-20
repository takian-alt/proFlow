package com.neuroflow.app.presentation.v2.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkManager
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.presentation.common.UlyssesContractSheet
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreenV2(
    onNavigateToMEQ: () -> Unit = {},
    onNavigateToSleepLogs: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToIdentity: () -> Unit = {},
    viewModel: InsightsViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isCalibrating by viewModel.isCalibratingCoach.collectAsState()
    val metrics by viewModel.insightsMetricsFlow.collectAsState()
    var showUlyssesSheet by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val context = LocalContext.current
    val workManager = remember(context) { viewModel.workManager ?: WorkManager.getInstance(context) }
    val tabs = InsightsSubTab.entries

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Anchored Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = NeuroFlowThemeV2Colors.WarmAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${state.dailyStreak}d Streak",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.WarmAmber
                        )
                    }
                }
            }

            // 2. Anchored Segmented Sub-Tab Bar (Controls the entire screen view)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                tabs.forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = selectedTabIndex == idx,
                        onClick = { selectedTabIndex = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            activeContentColor = Color.White,
                            inactiveContainerColor = NeuroFlowThemeV2Colors.SlateSurface,
                            inactiveContentColor = NeuroFlowThemeV2Colors.TextSecondary
                        ),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text(tab.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }

            // 3. Scoped Tab Content Area (Eliminates vertical scroll overload & duplicate cards)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (tabs[selectedTabIndex]) {
                    InsightsSubTab.RHYTHM -> {
                        // A. Borbély Two-Process Deep Dive + Model Confidence
                        BorbelyTwoProcessDeepDiveCard(
                            state = state,
                            onNavigateToMEQ = onNavigateToMEQ,
                            onNavigateToSleepLogs = onNavigateToSleepLogs
                        )

                        // B. Circadian Match & Flow Efficiency Row
                        CircadianSynchronyAndEfficiencyRow(state = state)

                        // C. 24-Hr Empirical Focus Density Heatmap
                        CircadianDensityHeatmapCard(
                            density = metrics.hourlyDensity,
                            peakStart = state.preferences.peakEnergyStart.takeIf { it > 0 } ?: 9,
                            peakEnd = state.preferences.peakEnergyEnd.takeIf { it > 0 } ?: 12
                        )

                        // D. Actionable Neuroscience Interventions
                        if (state.neuroscienceInterventions.isNotEmpty()) {
                            NeuroscienceInterventionsCard(interventions = state.neuroscienceInterventions)
                        }
                    }

                    InsightsSubTab.ESTIMATION -> {
                        // A. Executive Momentum & Habits
                        ExecutiveMomentumCard(state = state)

                        // B. Unified Execution Velocity & Time-Blindness Calibration (sMAPE + Auto-Buffer)
                        VelocityAndAccuracyCard(
                            velocity7d = metrics.velocity7d,
                            velocity30d = metrics.velocity30d,
                            focusHours7d = metrics.focusHours7d,
                            focusHours30d = metrics.focusHours30d,
                            weightedSmape = metrics.weightedSmape,
                            smapeGrade = metrics.smapeGrade,
                            planningCalibrationFactor = state.planningCalibrationFactor,
                            underestimatedPct = state.analyticsSummary?.underestimatedPct ?: 0f
                        )

                        // C. Cognitive Friction & Interruption Radar
                        CognitiveFrictionRadarCard(state = state)

                        // D. Deep Analytics & Task History Navigation
                        DeepHubNavigationCard(
                            onNavigateToAnalytics = onNavigateToAnalytics,
                            onNavigateToHistory = onNavigateToHistory
                        )
                    }

                    InsightsSubTab.FORENSIC_AUDIT -> {
                        // A. Unified AI Chrono-Coach Directive & Forensic Avoidance Audit
                        WeeklyAiFrictionAuditCard(
                            state = state,
                            isCalibrating = isCalibrating,
                            onCalibrateCoach = { viewModel.calibrateAiCoach() }
                        )

                        // B. Ulysses Pre-Commitment Contracts
                        UlyssesCommitmentsCard(
                            activeContractsCount = state.activeContractsCount,
                            onCreateContractClick = { showUlyssesSheet = true }
                        )

                        // C. Subliminal Identity Affirmations
                        IdentityAffirmationsCard(
                            affirmations = state.preferences.affirmations,
                            onNavigateToIdentity = onNavigateToIdentity
                        )
                    }
                }
            }
        }
    }

    if (showUlyssesSheet) {
        UlyssesContractSheet(
            taskRepository = viewModel.taskRepository,
            contractRepository = viewModel.contractRepository,
            workManager = workManager,
            onDismiss = { showUlyssesSheet = false }
        )
    }
}

@Composable
private fun CircadianSynchronyAndEfficiencyRow(state: InsightsUiStateV2) {
    val summary = state.analyticsSummary
    val peakMins = summary?.peakHourFocusMinutes?.roundToInt() ?: 0
    val offPeakMins = summary?.offPeakFocusMinutes?.roundToInt() ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                Text(
                    "🎯 Circadian Match",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )
                Text(
                    text = "${state.circadianAlignmentRate}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = NeuroFlowThemeV2Colors.SageGreen
                )
                LinearProgressIndicator(
                    progress = { state.circadianAlignmentRate / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = NeuroFlowThemeV2Colors.SageGreen,
                    trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                )
                Text(
                    text = if (peakMins + offPeakMins > 0) "${peakMins}m peak · ${offPeakMins}m off-peak" else "High-effort tasks in peak window",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowThemeV2Colors.TextTertiary,
                    fontSize = 11.sp
                )
            }
        }

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
                Text(
                    "⚡ Flow Efficiency",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )
                Text(
                    text = "${state.focusEfficiencyPercent}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                )
                LinearProgressIndicator(
                    progress = { state.focusEfficiencyPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = NeuroFlowThemeV2Colors.ElectricIndigo,
                    trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                )
                Text(
                    "Active time vs pause penalty",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowThemeV2Colors.TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun NeuroscienceInterventionsCard(interventions: List<String>) {
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

        interventions.forEach { tip ->
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

@Composable
private fun ExecutiveMomentumCard(state: InsightsUiStateV2) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
}

@Composable
private fun CognitiveFrictionRadarCard(state: InsightsUiStateV2) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
}

@Composable
private fun UlyssesCommitmentsCard(
    activeContractsCount: Int,
    onCreateContractClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚔️", fontSize = 16.sp)
                    Text(
                        text = "Ulysses Pre-Commitment Contracts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
                Text(
                    text = "$activeContractsCount Active",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                )
            }

            Text(
                text = "Lock in decisions ahead of time. Pre-commit stakes on high-friction tasks so future impulses cannot derail execution.",
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowThemeV2Colors.TextSecondary
            )

            Button(
                onClick = onCreateContractClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Ulysses Contract", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BorbelyTwoProcessDeepDiveCard(
    state: InsightsUiStateV2,
    onNavigateToMEQ: () -> Unit = {},
    onNavigateToSleepLogs: () -> Unit = {}
) {
    val pressure = state.energyUi?.sleepPressurePoints ?: 450
    val zone = state.energyUi?.fatigueZone ?: FatigueZone.RESTED
    val circadianFactor = state.energyUi?.circadianFactor ?: 0.85f
    val battery = state.energyUi?.availableEnergy ?: 75
    val profile = state.energyUi?.effectivePeakProfile

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                    Text(
                        "Borbély Two-Process Model",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Process C (Circadian Alertness): ${(circadianFactor * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                    Text(
                        "Suprachiasmatic Nucleus",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextTertiary
                    )
                }
                LinearProgressIndicator(
                    progress = { circadianFactor.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                    trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                )
            }

            // Process S (Homeostatic Sleep Pressure)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Process S (Sleep Pressure): $pressure pts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NeuroFlowThemeV2Colors.WarmAmber
                    )
                    Text(
                        "Zone: ${zone.name}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (zone) {
                            FatigueZone.RESTED -> NeuroFlowThemeV2Colors.SageGreen
                            FatigueZone.MODERATE -> NeuroFlowThemeV2Colors.WarmAmber
                            else -> NeuroFlowThemeV2Colors.RoseCoral
                        }
                    )
                }
                val pressureFraction = (pressure / 3000f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { pressureFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when (zone) {
                        FatigueZone.RESTED -> NeuroFlowThemeV2Colors.SageGreen
                        FatigueZone.MODERATE -> NeuroFlowThemeV2Colors.WarmAmber
                        else -> NeuroFlowThemeV2Colors.RoseCoral
                    },
                    trackColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                )
            }

            // Signal Confidence Breakdown (from EffectivePeakProfile)
            if (profile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricTile(
                        label = "Sleep Signal",
                        value = "${(profile.confidence.sleepCoverage * 100).roundToInt()}%",
                        subtitle = "Log coverage"
                    )
                    MetricTile(
                        label = "Wake Stability",
                        value = "${(profile.confidence.wakeConsistency * 100).roundToInt()}%",
                        subtitle = "Anchor rhythm"
                    )
                    MetricTile(
                        label = "Session Fit",
                        value = "${(profile.confidence.behaviorPerformance * 100).roundToInt()}%",
                        subtitle = "Empirical match"
                    )
                }
            }

            Text(
                text = "Chronotype: ${(state.preferences.quizChronotype ?: state.preferences.manualChronotype)?.replace('_', ' ') ?: "Intermediate"} • Peak hours: ${String.format("%02d:00", state.preferences.peakEnergyStart)} - ${String.format("%02d:00", state.preferences.peakEnergyEnd)}",
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowThemeV2Colors.TextSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onNavigateToMEQ,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Text("🧬 Chronotype Quiz", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onNavigateToSleepLogs,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Text("🌙 Sleep Logs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
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

@Composable
fun VelocityAndAccuracyCard(
    velocity7d: Int,
    velocity30d: Int,
    focusHours7d: Float,
    focusHours30d: Float,
    weightedSmape: Float,
    smapeGrade: String,
    planningCalibrationFactor: Float = 1.0f,
    underestimatedPct: Float = 0f
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚡", fontSize = 16.sp)
                Text(
                    text = "Execution Velocity & Time Calibration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricTile(
                    label = "7d Velocity",
                    value = "$velocity7d tasks",
                    subtitle = "${String.format(java.util.Locale.US, "%.1f", focusHours7d)}h focused"
                )
                MetricTile(
                    label = "30d Velocity",
                    value = "$velocity30d tasks",
                    subtitle = "${String.format(java.util.Locale.US, "%.1f", focusHours30d)}h focused"
                )
                MetricTile(
                    label = "sMAPE Error",
                    value = if (weightedSmape > 0f) "${String.format(java.util.Locale.US, "%.1f", weightedSmape)}%" else "Calibrating",
                    subtitle = when {
                        weightedSmape <= 0f -> "Calibrating"
                        weightedSmape <= 25f -> "High Precision"
                        else -> "Moderate Drift"
                    }
                )
            }

            HorizontalDivider(color = NeuroFlowThemeV2Colors.SlateBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricTile(
                    label = "Auto-Buffer Mult",
                    value = "${String.format(java.util.Locale.US, "%.2f", planningCalibrationFactor)}x",
                    subtitle = "NLP duration bias"
                )
                MetricTile(
                    label = "Underestimated",
                    value = "${underestimatedPct.roundToInt()}%",
                    subtitle = "Overrun tasks"
                )
                MetricTile(
                    label = "Overestimated",
                    value = if (underestimatedPct > 0f) "${(100f - underestimatedPct).coerceAtLeast(0f).roundToInt()}%" else "0%",
                    subtitle = "Early finishes"
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = smapeGrade,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowThemeV2Colors.TextSecondary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
fun CircadianDensityHeatmapCard(
    density: List<Float>,
    peakStart: Int,
    peakEnd: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔥", fontSize = 16.sp)
                    Text(
                        text = "24-Hr Circadian Focus Density Heatmap",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
                Text(
                    text = "Peak: ${String.format("%02d:00", peakStart)}–${String.format("%02d:00", peakEnd)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.WarmAmber
                )
            }

            Text(
                text = "Empirical focus distribution across 24 hourly circadian bins.",
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowThemeV2Colors.TextSecondary
            )

            val maxDensity = density.maxOrNull()?.coerceAtLeast(0.01f) ?: 1f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                density.forEachIndexed { hour, frac ->
                    val isPeakHour = if (peakStart <= peakEnd) {
                        hour in peakStart until peakEnd
                    } else {
                        hour >= peakStart || hour < peakEnd
                    }
                    val normalized = (frac / maxDensity).coerceIn(0.05f, 1f)
                    val barHeight = (normalized * 50f).dp.coerceAtLeast(4.dp)
                    val barColor = when {
                        isPeakHour -> NeuroFlowThemeV2Colors.WarmAmber
                        frac > 0.05f -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        else -> NeuroFlowThemeV2Colors.SlateSurfaceAlt
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .background(barColor, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00:00", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary, fontSize = 9.sp)
                Text("06:00", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary, fontSize = 9.sp)
                Text("12:00", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary, fontSize = 9.sp)
                Text("18:00", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary, fontSize = 9.sp)
                Text("23:00", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun IdentityAffirmationsCard(
    affirmations: List<String>,
    onNavigateToIdentity: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🧘", fontSize = 16.sp)
                    Text(
                        text = "Subliminal Identity Affirmations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
                TextButton(onClick = onNavigateToIdentity) {
                    Text(
                        "Manage",
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (affirmations.isEmpty()) {
                Text(
                    text = "No affirmations configured yet. Rewire subconscious beliefs and reduce executive resistance by defining your core identity statements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowThemeV2Colors.TextSecondary
                )
                OutlinedButton(
                    onClick = onNavigateToIdentity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Identity Affirmations", fontSize = 12.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    affirmations.take(3).forEach { affirmation ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                        ) {
                            Text(
                                text = "“$affirmation”",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeepHubNavigationCard(
    onNavigateToAnalytics: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "📊 Deep Work Analytics & Historical Records",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NeuroFlowThemeV2Colors.TextPrimary
            )
            Text(
                text = "Explore full MAPE breakdown, quadrant velocity, and searchable history logs.",
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowThemeV2Colors.TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToAnalytics,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Full Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Task History", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun WeeklyAiFrictionAuditCard(
    state: InsightsUiStateV2,
    isCalibrating: Boolean,
    onCalibrateCoach: () -> Unit
) {
    val prefs = state.preferences
    val directive = prefs.adaptiveSystemDirective
    val auditJson = prefs.weeklyAiAuditJson

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Chrono-Coach & Avoidance Audit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }

                FilledTonalButton(
                    onClick = onCalibrateCoach,
                    enabled = !isCalibrating,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    if (isCalibrating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text("Run Audit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (directive.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎯 Calibrated Executive-Function Directive",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = directive,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            if (auditJson.isNotBlank()) {
                val auditObj = remember(auditJson) {
                    runCatching { org.json.JSONObject(auditJson) }.getOrNull()
                }
                if (auditObj != null) {
                    val pattern = auditObj.optString("primaryAvoidancePattern", "")
                    val mismatch = auditObj.optString("biologicalCircadianMismatch", "")
                    val trigger = auditObj.optString("neuropsychologicalTrigger", "")
                    val prescriptionsArr = auditObj.optJSONArray("tacticalPrescriptions")

                    if (pattern.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "🔍 Avoidance Pattern Diagnosis",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.WarmAmber
                            )
                            Text(
                                text = pattern,
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                            if (mismatch.isNotBlank()) {
                                Text(
                                    text = "Circadian Mismatch: $mismatch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            if (trigger.isNotBlank()) {
                                Text(
                                    text = "Psychological Root Cause: $trigger",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (prescriptionsArr != null && prescriptionsArr.length() > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "🛠️ 120-Second Prescription",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.BioMint
                            )
                            for (i in 0 until minOf(2, prescriptionsArr.length())) {
                                val item = prescriptionsArr.getJSONObject(i)
                                val name = item.optString("strategyName", "Strategy")
                                val action = item.optString("immediateActionProtocol", "")
                                Text(
                                    text = "• $name: $action",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else if (directive.isBlank()) {
                Text(
                    text = "Tap 'Run Audit' to conduct a weekly forensic audit of postponed/deleted tasks and synthesize your personalized AI executive-function directive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowThemeV2Colors.TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
