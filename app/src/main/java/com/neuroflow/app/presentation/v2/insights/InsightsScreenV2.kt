package com.neuroflow.app.presentation.v2.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkManager
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.scheduler.BehavioralInsightsEngine
import com.neuroflow.app.presentation.common.UlyssesContractSheet
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.util.Calendar

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
    var showUlyssesSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val workManager = remember(context) { viewModel.workManager ?: WorkManager.getInstance(context) }
    val allTasks by viewModel.taskRepository.observeAll().collectAsState(initial = emptyList())

    val now = remember { System.currentTimeMillis() }
    val sevenDaysAgo = remember(now) { now - 7L * 86400_000L }
    val thirtyDaysAgo = remember(now) { now - 30L * 86400_000L }

    val completed7d = remember(allTasks, sevenDaysAgo) {
        allTasks.filter { it.completedAt != null && it.completedAt >= sevenDaysAgo }
    }
    val completed30d = remember(allTasks, thirtyDaysAgo) {
        allTasks.filter { it.completedAt != null && it.completedAt >= thirtyDaysAgo }
    }

    val velocity7d = completed7d.size
    val velocity30d = completed30d.size
    val focusHours7d = remember(completed7d) {
        completed7d.sumOf { (it.actualDurationMinutes ?: it.totalTimeTrackedMinutes).toDouble() }.toFloat() / 60f
    }
    val focusHours30d = remember(completed30d) {
        completed30d.sumOf { (it.actualDurationMinutes ?: it.totalTimeTrackedMinutes).toDouble() }.toFloat() / 60f
    }

    val weightedSmape = remember(allTasks) {
        AnalyticsEngine.computeWeightedSmape(allTasks)
    }
    val smapeGrade = remember(weightedSmape) {
        AnalyticsEngine.mapeGrade(weightedSmape)
    }

    val hourlyDensity = remember(allTasks, state.preferences) {
        val map = mutableMapOf<Int, Int>()
        allTasks.forEach { task ->
            val ts = task.completedAt ?: task.scheduledTime ?: task.updatedAt
            val cal = Calendar.getInstance().apply { timeInMillis = ts }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val mins = (task.actualDurationMinutes ?: task.totalTimeTrackedMinutes).toInt()
            if (mins > 0) {
                map[hour] = (map[hour] ?: 0) + mins
            }
        }
        if (map.isEmpty()) {
            val pStart = state.preferences.peakEnergyStart.takeIf { it > 0 } ?: 9
            val pEnd = state.preferences.peakEnergyEnd.takeIf { it > 0 } ?: 12
            for (h in pStart until pEnd) {
                map[h] = 45
            }
        }
        BehavioralInsightsEngine.calculateHourlyCircadianDensity(map)
    }

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
            BorbelyTwoProcessDeepDiveCard(
                state = state,
                onNavigateToMEQ = onNavigateToMEQ,
                onNavigateToSleepLogs = onNavigateToSleepLogs
            )

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

            Spacer(modifier = Modifier.height(14.dp))

            // Ulysses Pre-Commitments Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚔️", fontSize = 16.sp)
                            Text(
                                text = "Ulysses Pre-Commitment Contracts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary
                            )
                        }
                        Text(
                            text = "${state.activeContractsCount} Active",
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
                        onClick = { showUlyssesSheet = true },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Velocity & Estimation Accuracy (sMAPE)
            VelocityAndAccuracyCard(
                velocity7d = velocity7d,
                velocity30d = velocity30d,
                focusHours7d = focusHours7d,
                focusHours30d = focusHours30d,
                weightedSmape = weightedSmape,
                smapeGrade = smapeGrade
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 24-hr Circadian Density Heatmap
            CircadianDensityHeatmapCard(
                density = hourlyDensity,
                peakStart = state.preferences.peakEnergyStart.takeIf { it > 0 } ?: 9,
                peakEnd = state.preferences.peakEnergyEnd.takeIf { it > 0 } ?: 12
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subliminal Identity Affirmations
            IdentityAffirmationsCard(
                affirmations = state.preferences.affirmations,
                onNavigateToIdentity = onNavigateToIdentity
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Full Deep Analytics & History Records Hub
            DeepHubNavigationCard(
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToHistory = onNavigateToHistory
            )

            Spacer(modifier = Modifier.height(80.dp))
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
fun BorbelyTwoProcessDeepDiveCard(
    state: InsightsUiStateV2,
    onNavigateToMEQ: () -> Unit = {},
    onNavigateToSleepLogs: () -> Unit = {}
) {
    val pressure = state.energyUi?.sleepPressurePoints ?: 450
    val zone = state.energyUi?.fatigueZone ?: FatigueZone.RESTED
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
                        FatigueZone.RESTED -> NeuroFlowThemeV2Colors.SageGreen
                        FatigueZone.MODERATE -> NeuroFlowThemeV2Colors.WarmAmber
                        else -> NeuroFlowThemeV2Colors.RoseCoral
                    })
                }
                val pressureFraction = (pressure / 3000f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { pressureFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = when (zone) {
                        FatigueZone.RESTED -> NeuroFlowThemeV2Colors.SageGreen
                        FatigueZone.MODERATE -> NeuroFlowThemeV2Colors.WarmAmber
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
    smapeGrade: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚡", fontSize = 16.sp)
                Text(
                    text = "Execution Velocity & Estimation Calibration",
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // Hour labels (00, 06, 12, 18, 23)
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🧘", fontSize = 16.sp)
                    Text(
                        text = "Subliminal Identity Affirmations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
                TextButton(onClick = onNavigateToIdentity) {
                    Text("Manage", color = NeuroFlowThemeV2Colors.ElectricIndigoGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Full Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
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

