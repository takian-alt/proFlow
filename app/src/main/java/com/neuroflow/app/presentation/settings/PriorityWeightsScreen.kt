package com.neuroflow.app.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityWeightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Priority Weights & Strategy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Fine-tune the mathematical prioritization formula or choose a tailored 1-click strategy preset.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── STRATEGY PRESETS ─────────────────────────────────────────────
            Text(
                "Strategy Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    title = "⚖ Balanced",
                    description = "Default science-backed blend",
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(
                                weightQuadrant = 1.0f,
                                weightDeadlineUrgency = 1.0f,
                                weightPriorityLevel = 1.0f,
                                weightDuration = 1.0f,
                                weightImpact = 1.0f,
                                weightFocusMode = 1.0f,
                                weightCircadianMatch = 1.0f,
                                weightFrogBonus = 1.0f,
                                weightPlanBoost = 1.0f,
                                weightSessionProgress = 1.0f,
                                weightEnjoyment = 1.0f
                            )
                        }
                    }
                )

                PresetChip(
                    title = "🚨 Deadline Driven",
                    description = "Urgency & critical deadlines first",
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(
                                weightQuadrant = 1.4f,
                                weightDeadlineUrgency = 1.8f,
                                weightPriorityLevel = 1.4f,
                                weightDuration = 0.8f,
                                weightImpact = 0.8f,
                                weightFocusMode = 0.8f,
                                weightCircadianMatch = 0.7f,
                                weightFrogBonus = 0.7f,
                                weightPlanBoost = 0.8f,
                                weightSessionProgress = 0.8f,
                                weightEnjoyment = 0.5f
                            )
                        }
                    }
                )

                PresetChip(
                    title = "⚡ Momentum / ADHD",
                    description = "Quick wins, frogs & plans first",
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(
                                weightQuadrant = 0.8f,
                                weightDeadlineUrgency = 0.8f,
                                weightPriorityLevel = 0.8f,
                                weightDuration = 1.8f,
                                weightImpact = 1.0f,
                                weightFocusMode = 1.2f,
                                weightCircadianMatch = 1.4f,
                                weightFrogBonus = 1.4f,
                                weightPlanBoost = 1.6f,
                                weightSessionProgress = 1.8f,
                                weightEnjoyment = 1.5f
                            )
                        }
                    }
                )

                PresetChip(
                    title = "🏛 Pure Eisenhower",
                    description = "Strict Quadrant & Impact ranking",
                    onClick = {
                        viewModel.updatePreferences { p ->
                            p.copy(
                                weightQuadrant = 2.0f,
                                weightDeadlineUrgency = 1.2f,
                                weightPriorityLevel = 1.6f,
                                weightDuration = 0.5f,
                                weightImpact = 1.6f,
                                weightFocusMode = 0.5f,
                                weightCircadianMatch = 0.5f,
                                weightFrogBonus = 0.5f,
                                weightPlanBoost = 0.5f,
                                weightSessionProgress = 0.5f,
                                weightEnjoyment = 0.5f
                            )
                        }
                    }
                )
            }

            HorizontalDivider()

            // ── CORE PRIORITY WEIGHTS ─────────────────────────────────────────
            Text(
                "Core Priority Weights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            WeightSlider(
                label = "Quadrant (DO FIRST > SCHEDULE > DELEGATE)",
                value = prefs.weightQuadrant,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightQuadrant = it) } }
            )

            WeightSlider(
                label = "Deadline Urgency (exponential proximity boost)",
                value = prefs.weightDeadlineUrgency,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightDeadlineUrgency = it) } }
            )

            WeightSlider(
                label = "Priority Level (High > Medium > Low)",
                value = prefs.weightPriorityLevel,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightPriorityLevel = it) } }
            )

            WeightSlider(
                label = "Quick-Win Duration (shorter tasks boost)",
                value = prefs.weightDuration,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightDuration = it) } }
            )

            WeightSlider(
                label = "Strategic Impact & Value",
                value = prefs.weightImpact,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightImpact = it) } }
            )

            WeightSlider(
                label = "Focus Mode & Effort Alignment",
                value = prefs.weightFocusMode,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightFocusMode = it) } }
            )

            HorizontalDivider()

            // ── BEHAVIORAL & MOTIVATION HEURISTICS ────────────────────────────
            Text(
                "Behavioral & Motivation Heuristics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            WeightSlider(
                label = "Circadian Energy Match (Task Type vs Time-of-Day)",
                value = prefs.weightCircadianMatch,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightCircadianMatch = it) } }
            )

            WeightSlider(
                label = "Eat the Frog Bonus (Hardest task morning boost)",
                value = prefs.weightFrogBonus,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightFrogBonus = it) } }
            )

            WeightSlider(
                label = "Implementation Plan Boost (If-Then concrete steps)",
                value = prefs.weightPlanBoost,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightPlanBoost = it) } }
            )

            WeightSlider(
                label = "Progress Momentum (Started tasks boost)",
                value = prefs.weightSessionProgress,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightSessionProgress = it) } }
            )

            WeightSlider(
                label = "Enjoyment & Motivation (Temptation bundling)",
                value = prefs.weightEnjoyment,
                onValueChange = { viewModel.updatePreferences { p -> p.copy(weightEnjoyment = it) } }
            )

            // Reset to defaults
            OutlinedButton(
                onClick = {
                    viewModel.updatePreferences { p ->
                        p.copy(
                            weightQuadrant = 1.0f,
                            weightDeadlineUrgency = 1.0f,
                            weightPriorityLevel = 1.0f,
                            weightDuration = 1.0f,
                            weightImpact = 1.0f,
                            weightFocusMode = 1.0f,
                            weightCircadianMatch = 1.0f,
                            weightFrogBonus = 1.0f,
                            weightPlanBoost = 1.0f,
                            weightSessionProgress = 1.0f,
                            weightEnjoyment = 1.0f
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset All to Defaults")
            }

            // Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NeuroFlowColors.PurpleLight)
            ) {
                Text(
                    text = "💡 Tip: Increase weights for factors that matter most to your workflow. If you prefer pure urgency over energy matching, lower the Circadian and Enjoyment weights.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    color = NeuroFlowColors.PurpleDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PresetChip(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeightSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowColors.Purple
                ) {
                    Text(
                        "${String.format("%.1f", value)}x",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = { onValueChange(it) },
                valueRange = 0f..2f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = NeuroFlowColors.Purple,
                    activeTrackColor = NeuroFlowColors.Purple
                )
            )
        }
    }
}
