# neuroFlow UI Codebase — Insights Hub, Chronobiology Analytics & Velocity Reports

**Description:** InsightsScreenV2, InsightsViewModelV2, InsightsTabbedUpgradesV2, AnalyticsScreen/ViewModel, and HistoryScreen.  
**Generated for Vice Head Review:** 2026-10-20 16:59:40  
**Files:** 7  
**Lines of Code:** 4,385  
**Uncompressed Size:** 189.4 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/presentation/analytics/AnalyticsScreen.kt`](#appsrcmainjavacomneuroflowapppresentationanalyticsanalyticsscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/analytics/AnalyticsViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationanalyticsanalyticsviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/history/HistoryScreen.kt`](#appsrcmainjavacomneuroflowapppresentationhistoryhistoryscreenkt)
- [`app/src/main/java/com/neuroflow/app/presentation/history/HistoryViewModel.kt`](#appsrcmainjavacomneuroflowapppresentationhistoryhistoryviewmodelkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsScreenV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2insightsinsightsscreenv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsTabbedUpgradesV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2insightsinsightstabbedupgradesv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsViewModelV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2insightsinsightsviewmodelv2kt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowapppresentationanalyticsanalyticsscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/analytics/AnalyticsScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/analytics/AnalyticsScreen.kt`
- **Lines of Code:** 1,705
- **Size:** 78,455 bytes (76.6 KB)

```kotlin
package com.neuroflow.app.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.ContractOutcome
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EffectivePeakProfile
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.presentation.common.EnergyInsight
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import java.util.Calendar

enum class AnalyticsMode {
    ALL,
    OVERVIEW,
    INSIGHTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    hideTopBar: Boolean = false,
    mode: AnalyticsMode = AnalyticsMode.ALL,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.summary

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                TopAppBar(
                    title = { Text("Analytics") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    ) { innerPadding ->
        if (summary == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val showOverview = mode == AnalyticsMode.ALL || mode == AnalyticsMode.OVERVIEW
        val showInsights = mode == AnalyticsMode.ALL || mode == AnalyticsMode.INSIGHTS

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showInsights) {
                uiState.energy?.let { energy ->
                    EnergyPulseCard(energy)
                    MorningCalibrationCard(
                        profile = energy.effectivePeakProfile,
                        manualOverrideEnabled = uiState.preferences.manualPeakProfileEnabled,
                        tuneSleep = uiState.preferences.morningTuneSleepWeight,
                        tuneWake = uiState.preferences.morningTuneWakeWeight,
                        tuneBehavior = uiState.preferences.morningTuneBehaviorWeight,
                        tuneBase = uiState.preferences.morningTuneBaseWeight,
                        abstentionTriggerCount = uiState.preferences.peakConfidenceAbstentionTriggerCount,
                        abstentionRecoveryCount = uiState.preferences.peakConfidenceAbstentionRecoveryCount,
                        abstentionReason = uiState.preferences.peakConfidenceAbstentionReason,
                        abstentionReasonFreezeCount = uiState.preferences.peakConfidenceAbstentionReasonFreezeCount,
                        abstentionReasonLowSamplesCount = uiState.preferences.peakConfidenceAbstentionReasonLowSamplesCount,
                        abstentionReasonLowCoverageCount = uiState.preferences.peakConfidenceAbstentionReasonLowCoverageCount,
                        abstentionReasonWakeVarianceCount = uiState.preferences.peakConfidenceAbstentionReasonWakeVarianceCount,
                        abstentionReasonDivergenceCount = uiState.preferences.peakConfidenceAbstentionReasonDivergenceCount,
                        abstentionReasonOtherCount = uiState.preferences.peakConfidenceAbstentionReasonOtherCount,
                        abstentionRateTrend7d = uiState.abstentionRateTrend7d
                    )
                    InterruptionTelemetryCard(
                        pauseResumeCount = uiState.interruptionPauseResumeCount,
                        appSwitchCount = uiState.interruptionAppSwitchCount,
                        burstCount = uiState.interruptionBurstCount,
                        trend7d = uiState.interruptionTrend7d,
                        ratePerHourTrend7d = uiState.interruptionRatePerHourTrend7d
                    )
                }
            }

            if (showOverview) {
                TodayCard(summary)
                XpCard(summary)
                OverallProgressCard(summary)
                FocusTimeCard(summary)
                SevenDayTrendCard(summary)
                StreakCard(summary, uiState.preferences.identityLabel, uiState.preferences.topGoal)
                QuadrantCard(summary)
                PriorityCard(summary)
                HabitsCard(summary)
                EstimatedTimeCard(summary)
            }

            if (showInsights) {
                val useQuizPeak = uiState.preferences.quizPeakEnabled &&
                    uiState.preferences.effectivePeakStart >= 0 &&
                    uiState.preferences.effectivePeakEnd >= 0
                // Use effective (blended) peak if available, otherwise fall back to manual.
                val displayPeakStart = if (useQuizPeak)
                    uiState.preferences.effectivePeakStart else uiState.preferences.peakEnergyStart
                val displayPeakEnd = if (useQuizPeak)
                    uiState.preferences.effectivePeakEnd else uiState.preferences.peakEnergyEnd

                // Peak info insight
                val detectedStart = uiState.preferences.detectedPeakStart
                val detectedEnd = uiState.preferences.detectedPeakEnd
                val detectedPeakMinuteOfDay = uiState.preferences.detectedPeakMinuteOfDay
                val effectivePeakMinuteOfDay = if (useQuizPeak && uiState.preferences.effectivePeakMinuteOfDay >= 0) {
                    uiState.preferences.effectivePeakMinuteOfDay
                } else {
                    displayPeakStart * 60
                }
                val confidence = uiState.preferences.peakDetectionConfidence
                val hasDetectedPeak =
                    detectedStart >= 0 &&
                        detectedPeakMinuteOfDay in 0 until (24 * 60) &&
                        confidence > 0f

                DynamicPeakCard(
                    chronotype = uiState.preferences.quizChronotype ?: uiState.preferences.manualChronotype,
                    manualStart = uiState.preferences.peakEnergyStart,
                    manualEnd = uiState.preferences.peakEnergyEnd,
                    detectedStart = detectedStart,
                    detectedEnd = detectedEnd,
                    detectedPeakMinuteOfDay = detectedPeakMinuteOfDay,
                    effectiveStart = displayPeakStart,
                    effectiveEnd = displayPeakEnd,
                    effectivePeakMinuteOfDay = effectivePeakMinuteOfDay,
                    quizPeakEnabled = uiState.preferences.quizPeakEnabled,
                    confidence = confidence,
                    hasDetectedPeak = hasDetectedPeak,
                    effectiveProfile = uiState.energy?.effectivePeakProfile
                )

                NeuroBoostCard(summary)
                if (summary.topProcrastinatedTasks.isNotEmpty()) ProcrastinationCard(summary)
                PeakHourCard(summary, displayPeakStart, displayPeakEnd)
                MapeCard(summary) { viewModel.resetEstimationData() }
                SmapeCard(summary)
                RecurrenceAndScheduleCard(summary)
                if (uiState.activeContracts.isNotEmpty() || uiState.archivedContracts.isNotEmpty()) {
                    CommitmentsCard(uiState.activeContracts, uiState.archivedContracts)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Individual cards ──────────────────────────────────────────────────────────

@Composable
private fun EnergyPulseCard(energy: EnergyUiModel) {
    val fatigueColor = when (energy.fatigueZone) {
        FatigueZone.RESTED -> Color(0xFF43A047)
        FatigueZone.MODERATE -> Color(0xFFF9A825)
        FatigueZone.HIGH -> Color(0xFFF57C00)
        FatigueZone.CRITICAL -> Color(0xFFE53935)
    }
    val peakDecreasedPct = if (energy.peakValue > 0) {
        ((energy.peakDrop * 100f) / energy.peakValue.toFloat()).coerceIn(0f, 100f)
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NeuroFlowColors.Purple.copy(alpha = 0.16f),
                            Color(0xFF1565C0).copy(alpha = 0.14f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Energy Score", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${energy.availableEnergy}/100",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeuroFlowColors.Purple
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Adjusted Raw", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        String.format("%.1f", energy.rawEnergy),
                        fontWeight = FontWeight.Bold,
                        color = if (energy.rawEnergy >= 0f) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (energy.availableEnergy / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = NeuroFlowColors.Purple,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                EnergyMetricChip("Peak Score", String.format("%.1f", energy.peakScore), Color(0xFF1565C0))
                EnergyMetricChip("Fatigue Penalty", String.format("%.1f", energy.fatiguePenalty), fatigueColor)
                EnergyMetricChip("Sleep Pressure", "${energy.sleepPressurePoints}", fatigueColor)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                EnergyMetricChip("Baseline", String.format("%.1f", energy.baselineRawEnergy), Color(0xFF1565C0))
                EnergyMetricChip(
                    "Moment Δ",
                    String.format("%+.1f", energy.momentAdjustment),
                    if (energy.momentAdjustment >= 0f) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                EnergyMetricChip(
                    "Moment Conf",
                    "${(energy.momentConfidence * 100).toInt()}%",
                    NeuroFlowColors.Purple
                )
            }

            val weightedMoment = energy.momentAdjustment * energy.momentConfidence
            Text(
                "Formula: baseline = peak score - fatigue penalty. adjusted = baseline + (moment Δ × moment conf).",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Peak score = circadian capacity. Fatigue penalty = sleep-pressure drag. Moment Δ = short-term context boost/drag.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Current moment contribution: ${String.format("%+.1f", weightedMoment)}",
                fontSize = 11.sp,
                color = if (weightedMoment >= 0f) Color(0xFF2E7D32) else Color(0xFFC62828)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Peak now", fontSize = 13.sp)
                Text(
                    "${energy.currentPeakValue}/${energy.peakValue}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Peak decreased", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${energy.peakDrop} (${peakDecreasedPct.toInt()}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Since peak", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatMinutes(energy.minutesSincePeak)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Until peak reset", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatMinutes(energy.minutesUntilPeakReset)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Fatigue", fontSize = 13.sp)
                Text(
                    "${energy.fatiguePercent}% (${FatigueZone.fatigueZoneLabel(energy.fatigueZone)})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = fatigueColor
                )
            }
            LinearProgressIndicator(
                progress = { (energy.fatiguePercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = fatigueColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Circadian ${(energy.circadianFactor * 100).toInt()}% • Reservoir ${(energy.reservoirFactor * 100).toInt()}% • Confidence ${(energy.confidenceFactor * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val freshness = "${EnergyInsight.ageLabel(energy.overallFreshnessAgeMillis)} (${EnergyInsight.freshnessLabel(energy.overallFreshnessAgeMillis)})"
            val stability = EnergyInsight.stabilityScore(
                momentConfidence = energy.momentConfidence,
                peakConfidence = energy.confidenceFactor,
                freshnessAgeMillis = energy.overallFreshnessAgeMillis
            )
            Text(
                "Freshness $freshness • Stability ${stability}% • ${EnergyInsight.backtestSummary(energy.effectivePeakProfile)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                EnergyInsight.confidenceTierRationale(
                    momentConfidence = energy.momentConfidence,
                    peakConfidence = energy.confidenceFactor,
                    freshnessAgeMillis = energy.overallFreshnessAgeMillis
                ),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Why energy changed: ${EnergyInsight.whyEnergyChanged(energy.momentAdjustment, energy.momentSignalSummary)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "What to do now: ${EnergyInsight.whatToDoNow(energy.availableEnergy, energy.fatigueZone, energy.hasRecentData)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnergyMetricChip(label: String, value: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(0)
    val hours = safe / 60
    val minutes = safe % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
private fun XpCard(s: AnalyticsEngine.AnalyticsSummary) {
    if (s.totalXp == 0) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NeuroFlowColors.Purple.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("⚡ XP & Points", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                StatColumn("${s.totalXp}", "Total XP")
                StatColumn("+${s.xpToday}", "Today")
                StatColumn("+${s.xpThisWeek}", "This Week")
            }
            if (s.topXpTasks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Top earners", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                s.topXpTasks.forEach { (title, pts) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(
                            "+$pts XP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowColors.Purple
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayCard(s: AnalyticsEngine.AnalyticsSummary) {    AnalyticsCard("Today's Snapshot") {
        val h = (s.focusMinutesToday / 60).toInt()
        val m = (s.focusMinutesToday % 60).toInt()
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            StatColumn(if (h > 0) "${h}h ${m}m" else "${m}m", "Focus Time")
            StatColumn("${s.completedToday}", "Completed")
            StatColumn("${s.sessionsToday}", "Sessions")
        }
    }
}

@Composable
private fun OverallProgressCard(s: AnalyticsEngine.AnalyticsSummary) {
    AnalyticsCard("Overall Progress") {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            StatColumn("${s.totalTasks}", "Total")
            StatColumn("${s.completedTasks}", "Done")
            StatColumn("${s.remainingTasks}", "Remaining")
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Completion Rate")
            Text("${String.format("%.1f", s.completionRate)}%", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (s.completionRate / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Color(0xFFFFA726)
        )
    }
}

@Composable
private fun FocusTimeCard(s: AnalyticsEngine.AnalyticsSummary) {
    AnalyticsCard("Focus Time") {
        val totalH = (s.focusMinutesTotal / 60).toInt()
        val totalM = (s.focusMinutesTotal % 60).toInt()
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            StatColumn(if (totalH > 0) "${totalH}h ${totalM}m" else "${totalM}m", "All Time")
            StatColumn("${s.avgSessionMinutes.toInt()}m", "Avg Session")
            StatColumn("${s.totalSessions}", "Sessions")
        }
        if (s.mostFocusedTaskTitle != null) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Most focused on:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(s.mostFocusedTaskTitle, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun SevenDayTrendCard(s: AnalyticsEngine.AnalyticsSummary) {
    if (s.sevenDayTrend.none { it.second > 0f }) return
    AnalyticsCard("7-Day Focus Trend") {
        val maxMins = s.sevenDayTrend.maxOfOrNull { it.second }?.coerceAtLeast(1f) ?: 1f
        Row(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            s.sevenDayTrend.forEach { (label, mins) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val fraction = (mins / maxMins).coerceIn(0f, 1f)
                    val barH = (fraction * 72f).dp.coerceAtLeast(2.dp)
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(barH)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (mins > 0f) NeuroFlowColors.Purple
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (mins > 0f) {
                        Text(
                            "${mins.toInt()}m",
                            fontSize = 9.sp,
                            color = NeuroFlowColors.Purple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantCard(s: AnalyticsEngine.AnalyticsSummary) {
    AnalyticsCard("Tasks by Quadrant") {
        val quadrantData = listOf(
            "DO" to (s.tasksByQuadrant[Quadrant.DO_FIRST] ?: 0) to NeuroFlowColors.DoFirstBg,
            "PLAN" to (s.tasksByQuadrant[Quadrant.SCHEDULE] ?: 0) to NeuroFlowColors.ScheduleBg,
            "DELEGATE" to (s.tasksByQuadrant[Quadrant.DELEGATE] ?: 0) to NeuroFlowColors.DelegateBg,
            "DELETE" to (s.tasksByQuadrant[Quadrant.ELIMINATE] ?: 0) to NeuroFlowColors.EliminateBg
        )
        val maxCount = quadrantData.maxOfOrNull { it.first.second }?.coerceAtLeast(1) ?: 1
        quadrantData.forEach { (pair, color) ->
            val (label, count) = pair
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, modifier = Modifier.width(72.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier.weight(1f).height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight()
                            .fillMaxWidth(count.toFloat() / maxCount)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
                Text("$count", modifier = Modifier.width(32.dp), textAlign = TextAlign.End, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PriorityCard(s: AnalyticsEngine.AnalyticsSummary) {
    AnalyticsCard("Tasks by Priority") {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            PriorityCircle(s.tasksByPriority[Priority.HIGH] ?: 0, "High", NeuroFlowColors.DoFirstBg, NeuroFlowColors.DoFirstText)
            PriorityCircle(s.tasksByPriority[Priority.MEDIUM] ?: 0, "Medium", NeuroFlowColors.DelegateBg, NeuroFlowColors.DelegateText)
            PriorityCircle(s.tasksByPriority[Priority.LOW] ?: 0, "Low", Color(0xFFFFF9C4), Color(0xFFF9A825))
        }
    }
}

@Composable
private fun EstimatedTimeCard(s: AnalyticsEngine.AnalyticsSummary) {
    AnalyticsCard("Remaining Work") {
        val h = s.totalRemainingMinutes / 60
        val m = s.totalRemainingMinutes % 60
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Estimated remaining:")
            Text(
                if (h > 0) "$h hours $m mins" else "$m mins",
                color = Color(0xFF1565C0), fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MapeCard(s: AnalyticsEngine.AnalyticsSummary, onReset: () -> Unit) {
    val hasData = s.overallMape != 0f || s.weightedMape != 0f
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Reset Estimation Data?") },
            text = { Text("This will clear MAPE, SMAPE and actual duration from all completed tasks. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onReset(); showConfirm = false }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NeuroFlowColors.DeadlineCard)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Time Estimation Accuracy (MAPE)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            if (!hasData) {
                Text(
                    "No estimation data yet. Complete tasks with time estimates to see accuracy.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Overall MAPE:")
                    val mapeColor = when {
                        s.overallMape < 10f -> NeuroFlowColors.MapeGood
                        s.overallMape < 30f -> NeuroFlowColors.MapeMedium
                        else -> NeuroFlowColors.MapeBad
                    }
                    Text("${String.format("%.1f", s.overallMape)}%", color = mapeColor, fontWeight = FontWeight.Bold)
                }
                Text(AnalyticsEngine.mapeGrade(s.overallMape), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Weighted MAPE:", fontSize = 13.sp)
                    Text("${String.format("%.1f", s.weightedMape)}%", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("Estimation Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Underestimated tasks:", fontSize = 13.sp)
                    Text("${String.format("%.1f", s.underestimatedPct)}%", color = NeuroFlowColors.MapeBad, fontSize = 13.sp)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Overestimated tasks:", fontSize = 13.sp)
                    Text("${String.format("%.1f", s.overestimatedPct)}%", color = NeuroFlowColors.MapeGood, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showConfirm = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Reset Estimation Data", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SmapeCard(s: AnalyticsEngine.AnalyticsSummary) {
    val hasData = s.overallSmape != 0f || s.weightedSmape != 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NeuroFlowColors.ScheduledCard)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Symmetric Estimation (SMAPE)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            if (!hasData) {
                Text(
                    "No SMAPE data yet. Reset clears this along with MAPE.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Overall SMAPE:")
                    val smapeColor = when {
                        s.overallSmape < 10f -> NeuroFlowColors.MapeGood
                        s.overallSmape < 30f -> NeuroFlowColors.MapeMedium
                        else -> NeuroFlowColors.MapeBad
                    }
                    Text("${String.format("%.1f", s.overallSmape)}%", color = smapeColor, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Weighted SMAPE:")
                    val wSmapeColor = when {
                        s.weightedSmape < 10f -> NeuroFlowColors.MapeGood
                        s.weightedSmape < 30f -> NeuroFlowColors.MapeMedium
                        else -> NeuroFlowColors.MapeBad
                    }
                    Text("${String.format("%.1f", s.weightedSmape)}%", color = wSmapeColor, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "SMAPE is more balanced for small tasks that significantly overrun.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "To reset estimation data, use the button in the MAPE card above.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HabitsCard(s: AnalyticsEngine.AnalyticsSummary) {
    if (s.habitTasksTotal == 0) return
    AnalyticsCard("Habits & Consistency") {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            StatColumn("${s.habitTasksTotal}", "Habit Tasks")
            StatColumn("${s.habitTasksCompleted}", "Completed")
            StatColumn("${s.longestHabitStreak} 🔥", "Best Streak")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Habit completion rate")
            Text("${String.format("%.1f", s.habitCompletionRate)}%", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (s.habitCompletionRate / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = NeuroFlowColors.Purple
        )
        if (s.activeHabitStreaks.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Active streaks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            s.activeHabitStreaks.forEach { (title, streak) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("🔥 $streak", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeuroFlowColors.Purple)
                }
            }
        }
    }
}

@Composable
private fun RecurrenceAndScheduleCard(s: AnalyticsEngine.AnalyticsSummary) {
    if (s.recurringTasksTotal == 0 && s.lockedScheduleTasksTotal == 0) return
    AnalyticsCard("Recurrence & Locked Schedule") {
        if (s.recurringTasksTotal > 0) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Recurring tasks completed", fontSize = 13.sp)
                Text(
                    "${s.recurringTasksCompleted}/${s.recurringTasksTotal}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (s.lockedScheduleTasksTotal > 0) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Locked-schedule tasks completed", fontSize = 13.sp)
                Text(
                    "${s.lockedScheduleTasksCompleted}/${s.lockedScheduleTasksTotal}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        val recurringRate = if (s.recurringTasksTotal > 0)
            s.recurringTasksCompleted.toFloat() / s.recurringTasksTotal else 0f
        val lockedRate = if (s.lockedScheduleTasksTotal > 0)
            s.lockedScheduleTasksCompleted.toFloat() / s.lockedScheduleTasksTotal else 0f

        if (s.recurringTasksTotal > 0 || s.lockedScheduleTasksTotal > 0) {
            Text(
                "Completion reliability",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            if (s.recurringTasksTotal > 0) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Recurring", fontSize = 12.sp)
                    Text("${(recurringRate * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { recurringRate.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = NeuroFlowColors.Purple
                )
                Spacer(Modifier.height(6.dp))
            }
            if (s.lockedScheduleTasksTotal > 0) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Locked schedule", fontSize = 12.sp)
                    Text("${(lockedRate * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { lockedRate.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}

@Composable
private fun StreakCard(s: AnalyticsEngine.AnalyticsSummary, identityLabel: String, topGoal: String) {
    AnalyticsCard("Streak & Consistency") {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            StatColumn("${s.currentStreak} 🔥", "Current Streak")
            StatColumn("${s.longestStreak}", "Longest Streak")
        }
        if (identityLabel.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("You are a $identityLabel", fontWeight = FontWeight.Medium, color = NeuroFlowColors.Purple)
        }
        if (topGoal.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("Goal: $topGoal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PeakHourCard(s: AnalyticsEngine.AnalyticsSummary, peakStart: Int, peakEnd: Int) {
    if (s.peakHourFocusMinutes == 0f && s.offPeakFocusMinutes == 0f) return
    val total = (s.peakHourFocusMinutes + s.offPeakFocusMinutes).coerceAtLeast(1f)
    val peakFraction = s.peakHourFocusMinutes / total
    fun fmt(mins: Float): String {
        val h = (mins / 60).toInt(); val m = (mins % 60).toInt()
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
    val peakLabel = when {
        peakStart == 0  -> "12 am"
        peakStart < 12  -> "$peakStart am"
        peakStart == 12 -> "12 pm"
        else            -> "${peakStart - 12} pm"
    }
    val peakEndLabel = when {
        peakEnd == 0  -> "12 am"
        peakEnd < 12  -> "$peakEnd am"
        peakEnd == 12 -> "12 pm"
        else          -> "${peakEnd - 12} pm"
    }
    AnalyticsCard("⚡ Peak Hour Productivity ($peakLabel–$peakEndLabel)") {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            StatColumn(fmt(s.peakHourFocusMinutes), "Peak Focus")
            StatColumn(fmt(s.offPeakFocusMinutes), "Off-Peak Focus")
            StatColumn("${s.peakHourTasksCompleted}", "Tasks in Peak")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Peak focus ratio", fontSize = 13.sp)
            Text("${(peakFraction * 100).toInt()}%", fontWeight = FontWeight.Bold, color = NeuroFlowColors.Purple)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { peakFraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
            color = NeuroFlowColors.Purple
        )
        Spacer(Modifier.height(6.dp))
        val advice = when {
            peakFraction >= 0.6f -> "Great — you're using your peak hours well."
            peakFraction >= 0.35f -> "Try to schedule more deep work during your peak window."
            else -> "Most focus is happening off-peak. Protect your ${peakLabel}–${peakEndLabel} window."
        }
        Text(advice, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            "This card tracks your primary peak window. See Peak Info for all peak windows and timings.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProcrastinationCard(s: AnalyticsEngine.AnalyticsSummary) {
    AnalyticsCard("Procrastination Radar") {
        s.topProcrastinatedTasks.forEach { task ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(task.title, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("Deferred ${task.postponeCount}×", fontSize = 12.sp, color = NeuroFlowColors.MapeBad)
                }
            }
        }
    }
}

@Composable
private fun NeuroBoostCard(s: AnalyticsEngine.AnalyticsSummary) {
    val hasData = s.frogTasksTotal > 0 || s.anxietyTasksTotal > 0 ||
        s.publicCommitmentTotal > 0 || s.contextTagBreakdown.isNotEmpty()
    if (!hasData) return

    AnalyticsCard("🧠 Neuro Boost Insights") {
        // Frog tasks
        if (s.frogTasksTotal > 0) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("🐸 Frog tasks completed", fontSize = 13.sp)
                Text("${s.frogTasksCompleted}/${s.frogTasksTotal}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { if (s.frogTasksTotal > 0) s.frogTasksCompleted.toFloat() / s.frogTasksTotal else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = NeuroFlowColors.DoFirstText
            )
            Spacer(Modifier.height(10.dp))
        }

        // Anxiety tasks
        if (s.anxietyTasksTotal > 0) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("😰 Anxiety tasks faced", fontSize = 13.sp)
                Text("${s.anxietyTasksCompleted}/${s.anxietyTasksTotal}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
        }

        // Public commitments
        if (s.publicCommitmentTotal > 0) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("📢 Public commitments kept", fontSize = 13.sp)
                Text("${s.publicCommitmentCompleted}/${s.publicCommitmentTotal}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
        }

        // Step-by-step plan usage
        if (s.ifThenPlanUsageRate > 0f) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("📌 Step-by-step plan usage", fontSize = 13.sp)
                Text("${String.format("%.0f", s.ifThenPlanUsageRate)}%", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = NeuroFlowColors.Purple)
            }
            Spacer(Modifier.height(10.dp))
        }

        // Task type distribution
        if (s.taskTypeDistribution.isNotEmpty()) {
            Text("Task Type Distribution", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            val total = s.taskTypeDistribution.values.sum().coerceAtLeast(1)
            s.taskTypeDistribution.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                val label = when (type) {
                    "ANALYTICAL" -> "🧠 Analytical"
                    "CREATIVE"   -> "🎨 Creative"
                    "ADMIN"      -> "📋 Admin"
                    "PHYSICAL"   -> "💪 Physical"
                    else         -> type
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.width(100.dp), fontSize = 12.sp)
                    Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(count.toFloat() / total)
                            .clip(RoundedCornerShape(3.dp)).background(NeuroFlowColors.Purple.copy(alpha = 0.7f)))
                    }
                    Text("$count", modifier = Modifier.width(28.dp), fontSize = 12.sp,
                        textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Context tag breakdown
        if (s.contextTagBreakdown.isNotEmpty()) {
            Text("Context Tag Breakdown", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                s.contextTagBreakdown.entries.sortedByDescending { it.value }.take(5).forEach { (tag, count) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NeuroFlowColors.Purple.copy(alpha = 0.12f)
                    ) {
                        Text("$tag ($count)", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp, color = NeuroFlowColors.Purple, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (s.taskTagBreakdown.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Task Tag Breakdown", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                s.taskTagBreakdown.entries.sortedByDescending { it.value }.take(6).forEach { (tag, count) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1565C0).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "$tag ($count)",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicPeakCard(
    chronotype: String?,
    manualStart: Int, manualEnd: Int,
    detectedStart: Int, detectedEnd: Int,
    detectedPeakMinuteOfDay: Int,
    effectiveStart: Int, effectiveEnd: Int,
    effectivePeakMinuteOfDay: Int,
    quizPeakEnabled: Boolean,
    confidence: Float,
    hasDetectedPeak: Boolean,
    effectiveProfile: EffectivePeakProfile?
) {
    fun normalizeMinuteOfDay(minuteOfDay: Int): Int {
        val normalized = minuteOfDay % (24 * 60)
        return if (normalized < 0) normalized + (24 * 60) else normalized
    }

    fun resolvePeakMinuteOfDay(minuteOfDay: Int, fallbackHour: Int): Int {
        return if (minuteOfDay in 0 until (24 * 60)) minuteOfDay else normalizeMinuteOfDay(fallbackHour * 60)
    }

    fun fmtHour(h: Int): String {
        val amPm = if (h < 12) "am" else "pm"
        val display = if (h == 0 || h == 12) 12 else h % 12
        return "$display$amPm"
    }

    fun fmtMinuteOfDay(minuteOfDay: Int): String {
        val normalized = normalizeMinuteOfDay(minuteOfDay)
        val hour = normalized / 60
        val minute = normalized % 60
        val amPm = if (hour < 12) "am" else "pm"
        val displayHour = if (hour == 0 || hour == 12) 12 else hour % 12
        return String.format("%d:%02d%s", displayHour, minute, amPm)
    }

    fun minuteIsInsideWindow(minute: Int, startMinute: Int, durationMinutes: Int): Boolean {
        if (durationMinutes <= 0) return false
        val safeMinute = normalizeMinuteOfDay(minute)
        val safeStart = normalizeMinuteOfDay(startMinute)
        val endExclusive = safeStart + durationMinutes
        return if (endExclusive <= 24 * 60) {
            safeMinute in safeStart until endExclusive
        } else {
            safeMinute >= safeStart || safeMinute < (endExclusive % (24 * 60))
        }
    }

    fun minutesUntil(targetMinute: Int, nowMinute: Int): Int {
        val safeTarget = normalizeMinuteOfDay(targetMinute)
        val safeNow = normalizeMinuteOfDay(nowMinute)
        val forward = safeTarget - safeNow
        return if (forward >= 0) forward else forward + (24 * 60)
    }

    fun windowIntensityLabel(amplitude: Float): String {
        return when {
            amplitude >= 0.9f -> "Hard-work"
            amplitude >= 0.7f -> "Deep-work"
            else -> "Light-focus"
        }
    }

    fun windowConfidenceColor(confidenceValue: Float): Color {
        return when {
            confidenceValue >= 0.8f -> Color(0xFF2E7D32)
            confidenceValue >= 0.6f -> Color(0xFF1565C0)
            else -> Color(0xFFC62828)
        }
    }

    data class PeakWindowUi(
        val index: Int,
        val startMinute: Int,
        val endMinute: Int,
        val durationMinutes: Int,
        val amplitude: Float,
        val confidence: Float,
        val isActiveNow: Boolean,
        val minutesUntilStart: Int
    )

    val detectionConfidence = confidence.coerceIn(0f, 1f)
    val detectedPeakMinute = if (hasDetectedPeak) {
        resolvePeakMinuteOfDay(detectedPeakMinuteOfDay, detectedStart)
    } else {
        normalizeMinuteOfDay(manualStart * 60)
    }
    val effectivePeakMinute = resolvePeakMinuteOfDay(effectivePeakMinuteOfDay, effectiveStart)
    val pct = (detectionConfidence * 100).toInt()
    val manualPeakMinute = normalizeMinuteOfDay(manualStart * 60)
    val shifted = hasDetectedPeak && circularMinuteDistance(detectedPeakMinute, manualPeakMinute) >= 120
    val isMorningType = EnergyInsight.isMorningType(chronotype)
    val nowMinuteOfDay = Calendar.getInstance().let {
        it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
    }
    val profileWindows = effectiveProfile?.windows?.mapIndexed { index, window ->
        val startMinute = normalizeMinuteOfDay(effectivePeakMinute + window.startMinuteOffset)
        val endMinute = normalizeMinuteOfDay(startMinute + window.durationMinutes)
        val activeNow = minuteIsInsideWindow(
            minute = nowMinuteOfDay,
            startMinute = startMinute,
            durationMinutes = window.durationMinutes
        )
        PeakWindowUi(
            index = index,
            startMinute = startMinute,
            endMinute = endMinute,
            durationMinutes = window.durationMinutes,
            amplitude = window.amplitude,
            confidence = (effectiveProfile?.windowConfidences?.getOrNull(index) ?: detectionConfidence).coerceIn(0f, 1f),
            isActiveNow = activeNow,
            minutesUntilStart = if (activeNow) 0 else minutesUntil(startMinute, nowMinuteOfDay)
        )
    }.orEmpty()
    val activeWindow = profileWindows.firstOrNull { it.isActiveNow }
    val nextWindow = profileWindows.minByOrNull { if (it.isActiveNow) 0 else it.minutesUntilStart }

    AnalyticsCard("⚡ Peak Info") {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Manual setting", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${fmtHour(manualStart)}–${fmtHour(manualEnd)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Detected from MEQ quiz", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (hasDetectedPeak) {
                Text(
                    fmtMinuteOfDay(detectedPeakMinute),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (shifted) NeuroFlowColors.Purple else MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    "Not available yet",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (hasDetectedPeak && detectedEnd >= 0) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Detected hour window", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${fmtHour(detectedStart)}–${fmtHour(detectedEnd)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Effective (scoring uses)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                fmtMinuteOfDay(effectivePeakMinute),
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = NeuroFlowColors.Purple
            )
        }
        if (profileWindows.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Today peak windows",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            profileWindows.forEach { window ->
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    val intensity = windowIntensityLabel(window.amplitude)
                    val label = "Peak ${window.index + 1} ($intensity)"
                    Text(
                        label,
                        fontSize = 12.sp,
                        color = if (window.isActiveNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${fmtMinuteOfDay(window.startMinute)}–${fmtMinuteOfDay(window.endMinute)} (${formatMinutes(window.durationMinutes)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (window.isActiveNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    Text(
                        "${(window.confidence * 100).toInt()}% ${EnergyInsight.windowConfidenceTier(effectiveProfile, window.index)} confidence",
                        fontSize = 11.sp,
                        color = windowConfidenceColor(window.confidence)
                    )
                }
            }
            val peak2Confidence = profileWindows.getOrNull(1)?.confidence
            val peak3Confidence = profileWindows.getOrNull(2)?.confidence
            if (peak2Confidence != null || peak3Confidence != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    buildString {
                        append("Peak 2 confidence ")
                        append(peak2Confidence?.let { "${(it * 100).toInt()}%" } ?: "n/a")
                        append(" • Peak 3 confidence ")
                        append(peak3Confidence?.let { "${(it * 100).toInt()}%" } ?: "n/a")
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            when {
                activeWindow != null -> {
                    Text(
                        "You are in Peak ${activeWindow.index + 1} now. Peak 1 is usually best for hardest work; later peaks are better for medium or lighter execution.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                nextWindow != null -> {
                    Text(
                        "Next peak: Peak ${nextWindow.index + 1} in ${formatMinutes(nextWindow.minutesUntilStart)} at ${fmtMinuteOfDay(nextWindow.startMinute)}.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (isMorningType) {
            Spacer(Modifier.height(6.dp))
            Text(
                EnergyInsight.profileSummary(effectiveProfile),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                EnergyInsight.adaptiveHint(effectiveProfile),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Detection confidence", fontSize = 12.sp)
            Text("$pct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeuroFlowColors.Purple)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { detectionConfidence },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = NeuroFlowColors.Purple
        )
        Spacer(Modifier.height(8.dp))
        Text(
            EnergyInsight.profileConfidenceLine(effectiveProfile),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val confidenceActions = EnergyInsight.confidenceImprovementActions(effectiveProfile)
        if (confidenceActions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "How to raise confidence",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            confidenceActions.forEach { action ->
                Text(
                    "• $action",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!hasDetectedPeak) {
            Text(
                "Detected peak data is not available yet. Peak Info is using your manual peak window and adaptive profile windows when available.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (!quizPeakEnabled) {
            Text(
                "Quiz peak is turned off. Scoring is using your manual peak window.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (shifted) {
            Text(
                "Your quiz-derived peak point (${fmtMinuteOfDay(detectedPeakMinute)}) differs from your manual setting window (${fmtHour(manualStart)}–${fmtHour(manualEnd)}). " +
                "Scoring is already blending both. Update your setting in Onboarding to fully align.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Your manual setting aligns with your quiz-derived peak point.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun circularMinuteDistance(a: Int, b: Int): Int {
    val day = 24 * 60
    val aa = ((a % day) + day) % day
    val bb = ((b % day) + day) % day
    val diff = kotlin.math.abs(aa - bb)
    return minOf(diff, day - diff)
}

internal fun abstentionReasonMix(
    freezeCount: Int,
    lowSamplesCount: Int,
    lowCoverageCount: Int,
    wakeVarianceCount: Int,
    divergenceCount: Int,
    otherCount: Int
): List<Pair<String, Int>> {
    return listOf(
        "Freeze safety" to freezeCount.coerceAtLeast(0),
        "Low samples" to lowSamplesCount.coerceAtLeast(0),
        "Low coverage" to lowCoverageCount.coerceAtLeast(0),
        "Wake variance" to wakeVarianceCount.coerceAtLeast(0),
        "High divergence" to divergenceCount.coerceAtLeast(0),
        "Other" to otherCount.coerceAtLeast(0)
    )
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
}

@Composable
private fun MorningCalibrationCard(
    profile: EffectivePeakProfile?,
    manualOverrideEnabled: Boolean,
    tuneSleep: Float,
    tuneWake: Float,
    tuneBehavior: Float,
    tuneBase: Float,
    abstentionTriggerCount: Int,
    abstentionRecoveryCount: Int,
    abstentionReason: String,
    abstentionReasonFreezeCount: Int,
    abstentionReasonLowSamplesCount: Int,
    abstentionReasonLowCoverageCount: Int,
    abstentionReasonWakeVarianceCount: Int,
    abstentionReasonDivergenceCount: Int,
    abstentionReasonOtherCount: Int,
    abstentionRateTrend7d: List<Pair<String, Float>>
) {
    AnalyticsCard("Morning Calibration Diagnostics") {
        if (profile == null) {
            Text("No calibration data yet.", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Complete MEQ and a few focus sessions to unlock adaptive diagnostics.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@AnalyticsCard
        }
        Text(EnergyInsight.profileSummary(profile), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(
            EnergyInsight.profileModeLabel(manualOverrideEnabled = manualOverrideEnabled, profile = profile),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(EnergyInsight.profileConfidenceLine(profile), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(EnergyInsight.backtestSummary(profile), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(
            "Abstention telemetry: triggered ${abstentionTriggerCount.coerceAtLeast(0)}x • recovered ${abstentionRecoveryCount.coerceAtLeast(0)}x",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (profile.confidenceGatedAbstention || abstentionReason.isNotBlank()) {
            Text(
                "Latest abstention reason: ${abstentionReason.ifBlank { "insufficient confidence for personalized prediction" }}",
                fontSize = 11.sp,
                color = if (profile.confidenceGatedAbstention) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val reasonBreakdown = abstentionReasonMix(
            freezeCount = abstentionReasonFreezeCount,
            lowSamplesCount = abstentionReasonLowSamplesCount,
            lowCoverageCount = abstentionReasonLowCoverageCount,
            wakeVarianceCount = abstentionReasonWakeVarianceCount,
            divergenceCount = abstentionReasonDivergenceCount,
            otherCount = abstentionReasonOtherCount
        )
        val reasonTotal = reasonBreakdown.sumOf { it.second }
        if (reasonTotal > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Abstention reason mix",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            reasonBreakdown.take(4).forEach { (label, count) ->
                val pct = ((count.toFloat() / reasonTotal.toFloat()) * 100f).toInt().coerceIn(0, 100)
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("• $label", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$count ($pct%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val trend = abstentionRateTrend7d.takeIf { it.size == 7 } ?: emptyList()
        if (trend.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "7-day abstention rate",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            val maxRate = trend.maxOfOrNull { it.second }?.coerceAtLeast(5f) ?: 5f
            Row(
                modifier = Modifier.fillMaxWidth().height(66.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEach { (label, rateValue) ->
                    val safeRate = rateValue.coerceIn(0f, 100f)
                    val fraction = (safeRate / maxRate).coerceIn(0f, 1f)
                    val barHeight = (fraction * 44f).dp.coerceAtLeast(2.dp)
                    val barColor = when {
                        safeRate >= 50f -> Color(0xFFC62828)
                        safeRate >= 20f -> Color(0xFFEF6C00)
                        else -> Color(0xFF2E7D32)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor.copy(alpha = 0.78f))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val latestRate = trend.lastOrNull()?.second?.coerceIn(0f, 100f) ?: 0f
            val avgRate = trend.map { it.second.coerceIn(0f, 100f) }.average().toFloat()
            Spacer(Modifier.height(4.dp))
            Text(
                "Latest ${latestRate.toInt()}% • Avg ${avgRate.toInt()}%",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Drift status", fontSize = 12.sp)
            Text(profile.driftStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Confidence sleep", fontSize = 12.sp)
            Text("${(profile.confidence.sleepCoverage * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Confidence wake", fontSize = 12.sp)
            Text("${(profile.confidence.wakeConsistency * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Confidence behavior", fontSize = 12.sp)
            Text("${(profile.confidence.behaviorPerformance * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Sleep = sleep-log coverage and duration quality. Wake = wake-time regularity. Behavior = how task outcomes match predicted slots.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text("Auto-tune weights", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("Sleep ${(tuneSleep * 100).toInt()}%", fontSize = 12.sp)
            Text("Wake ${(tuneWake * 100).toInt()}%", fontSize = 12.sp)
            Text("Behavior ${(tuneBehavior * 100).toInt()}%", fontSize = 12.sp)
            Text("Base ${(tuneBase * 100).toInt()}%", fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "These weights control how strongly each signal can move your detected peak profile.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val actions = EnergyInsight.confidenceImprovementActions(profile)
        if (actions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Confidence boosters", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            actions.forEach { action ->
                Text(
                    "• $action",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InterruptionTelemetryCard(
    pauseResumeCount: Int,
    appSwitchCount: Int,
    burstCount: Int,
    trend7d: List<Pair<String, Int>>,
    ratePerHourTrend7d: List<Pair<String, Float>>
) {
    var showRatePerHour by remember { mutableStateOf(false) }
    AnalyticsCard("Interruption Telemetry") {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            StatColumn("$pauseResumeCount", "Pause/Resume")
            StatColumn("$appSwitchCount", "App Switches")
            StatColumn("$burstCount", "Burst Events")
        }
        Spacer(Modifier.height(8.dp))
        val total = (pauseResumeCount + appSwitchCount + burstCount).coerceAtLeast(1)
        val burstRatio = (burstCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        Text("Burst share ${(burstRatio * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(
            progress = { burstRatio },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = NeuroFlowColors.Purple
        )
        if (trend7d.isNotEmpty() && trend7d.any { it.second > 0 }) {
            Spacer(Modifier.height(10.dp))
            Text("7-day interruption trend", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showRatePerHour,
                    onClick = { showRatePerHour = false },
                    label = { Text("Count") }
                )
                FilterChip(
                    selected = showRatePerHour,
                    onClick = { showRatePerHour = true },
                    label = { Text("Rate/hr") }
                )
            }
            Spacer(Modifier.height(6.dp))
            val countMax = trend7d.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            val rateMax = ratePerHourTrend7d.maxOfOrNull { it.second }?.coerceAtLeast(0.1f) ?: 0.1f
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                trend7d.forEachIndexed { idx, (label, countValue) ->
                    val rateValue = ratePerHourTrend7d.getOrNull(idx)?.second ?: 0f
                    val fraction = if (showRatePerHour) {
                        (rateValue / rateMax).coerceIn(0f, 1f)
                    } else {
                        (countValue.toFloat() / countMax.toFloat()).coerceIn(0f, 1f)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val barH = (fraction * 56f).dp.coerceAtLeast(2.dp)
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(barH)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(NeuroFlowColors.Purple.copy(alpha = 0.75f))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            val latestCount = trend7d.lastOrNull()?.second ?: 0
            val latestRate = ratePerHourTrend7d.lastOrNull()?.second ?: 0f
            Text(
                if (showRatePerHour) "Latest: ${String.format("%.1f", latestRate)} interruptions/hr"
                else "Latest: $latestCount interruptions",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (pauseResumeCount == 0 && appSwitchCount == 0 && burstCount == 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "No interruption telemetry yet. Start focus sessions to populate this card.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun CommitmentsCard(
    active: List<com.neuroflow.app.data.local.entity.UlyssesContractEntity>,
    archived: List<com.neuroflow.app.data.local.entity.UlyssesContractEntity>
) {
    val wins = archived.count { it.outcome == ContractOutcome.WIN }
    val losses = archived.count { it.outcome == ContractOutcome.LOSS }
    val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())

    AnalyticsCard("⚔️ Ulysses Contracts") {
        if (active.isNotEmpty()) {
            Text("Active", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            active.forEach { contract ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(contract.consequence, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("Due ${sdf.format(java.util.Date(contract.deadlineAt))}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (archived.isNotEmpty()) {
            Text("Results", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                StatColumn("$wins ✅", "Wins")
                StatColumn("$losses ❌", "Losses")
            }
        }
        if (active.isEmpty() && archived.isEmpty()) {
            Text("No contracts yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnalyticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PriorityCircle(count: Int, label: String, bgColor: Color, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text("$count", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

```

---

<a id="appsrcmainjavacomneuroflowapppresentationanalyticsanalyticsviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/analytics/AnalyticsViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/analytics/AnalyticsViewModel.kt`
- **Lines of Code:** 212
- **Size:** 9,916 bytes (9.7 KB)

```kotlin
package com.neuroflow.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.ContractOutcome
import com.neuroflow.app.data.local.entity.EnergyPredictionEntity
import com.neuroflow.app.data.local.entity.UlyssesContractEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.model.buildEnergyUiModel
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.repository.EnergyMetricsRepository
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val summary: AnalyticsEngine.AnalyticsSummary? = null,
    val preferences: UserPreferences = UserPreferences(),
    val energy: EnergyUiModel? = null,
    val isLoading: Boolean = true,
    val activeContracts: List<UlyssesContractEntity> = emptyList(),
    val archivedContracts: List<UlyssesContractEntity> = emptyList(),
    val interruptionPauseResumeCount: Int = 0,
    val interruptionAppSwitchCount: Int = 0,
    val interruptionBurstCount: Int = 0,
    val interruptionTrend7d: List<Pair<String, Int>> = emptyList(),
    val interruptionRatePerHourTrend7d: List<Pair<String, Float>> = emptyList(),
    val abstentionRateTrend7d: List<Pair<String, Float>> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val contractRepository: UlyssesContractRepository,
    private val energyMetricsRepository: EnergyMetricsRepository,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        combine(
            taskRepository.observeAll(),
            sessionRepository.observeAll(),
            preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
                old.quizPeakEnabled == new.quizPeakEnabled &&
                old.effectivePeakStart == new.effectivePeakStart &&
                old.effectivePeakEnd == new.effectivePeakEnd &&
                old.peakEnergyStart == new.peakEnergyStart &&
                old.peakEnergyEnd == new.peakEnergyEnd &&
                old.dailyStreak == new.dailyStreak &&
                old.longestStreak == new.longestStreak
            }
        ) { tasks, sessions, prefs ->
            val summary = AnalyticsEngine.buildSummary(
                allTasks = tasks,
                allSessions = sessions,
                prefs = prefs
            )
            AnalyticsUiState(
                summary = summary,
                preferences = prefs,
                isLoading = false,
                interruptionPauseResumeCount = sessions.sumOf { it.pauseResumeCount },
                interruptionAppSwitchCount = sessions.sumOf { it.appSwitchCount },
                interruptionBurstCount = sessions.sumOf { it.interruptionBurstCount },
                interruptionTrend7d = interruptionTrend7d(sessions),
                interruptionRatePerHourTrend7d = interruptionRatePerHourTrend7d(sessions)
            )
        }.onEach { state ->
            _uiState.update {
                it.copy(
                    summary = state.summary,
                    preferences = state.preferences,
                    isLoading = state.isLoading,
                    interruptionPauseResumeCount = state.interruptionPauseResumeCount,
                    interruptionAppSwitchCount = state.interruptionAppSwitchCount,
                    interruptionBurstCount = state.interruptionBurstCount,
                    interruptionTrend7d = state.interruptionTrend7d,
                    interruptionRatePerHourTrend7d = state.interruptionRatePerHourTrend7d
                )
            }
        }.launchIn(viewModelScope)

        contractRepository.observeActive().onEach { contracts ->
            _uiState.update { it.copy(activeContracts = contracts) }
        }.launchIn(viewModelScope)

        contractRepository.observeArchived().onEach { contracts ->
            _uiState.update { it.copy(archivedContracts = contracts) }
        }.launchIn(viewModelScope)

        combine(
            preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
                old.wakeUpHour == new.wakeUpHour
            },
            sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())
        ) { prefs, sleepLogs ->
            val now = System.currentTimeMillis()
            val (_, model) = computeEnergyUiState(
                circadianEnergyService = circadianEnergyService,
                sleepTelemetryRepository = sleepTelemetryRepository,
                prefs = prefs,
                sleepLogs = sleepLogs,
                nowMillis = now
            )
            _uiState.update { it.copy(energy = model) }
        }.launchIn(viewModelScope)

        val sevenDaysMillis = 7L * 24L * 60L * 60L * 1000L
        val cutoffMillis = System.currentTimeMillis() - sevenDaysMillis
        energyMetricsRepository.observeRecentPredictions(afterMillis = cutoffMillis).onEach { predictions ->
            _uiState.update {
                it.copy(abstentionRateTrend7d = buildAbstentionRateTrend7d(predictions))
            }
        }.launchIn(viewModelScope)
    }

    fun resetEstimationData() {
        viewModelScope.launch {
            taskRepository.resetEstimationErrors()
        }
    }

    private fun interruptionTrend7d(
        sessions: List<com.neuroflow.app.data.local.entity.TimeSessionEntity>
    ): List<Pair<String, Int>> {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
        val counts = mutableMapOf<java.time.LocalDate, Int>()
        sessions.forEach { s ->
            val date = Instant.ofEpochMilli(s.startedAt).atZone(zone).toLocalDate()
            val score = s.pauseResumeCount + s.appSwitchCount + s.interruptionBurstCount
            counts[date] = (counts[date] ?: 0) + score
        }
        return (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val label = date.dayOfWeek.name.take(3)
            label.lowercase().replaceFirstChar { it.uppercase() } to (counts[date] ?: 0)
        }
    }

    private fun interruptionRatePerHourTrend7d(
        sessions: List<com.neuroflow.app.data.local.entity.TimeSessionEntity>
    ): List<Pair<String, Float>> {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
        val interruptionByDate = mutableMapOf<java.time.LocalDate, Int>()
        val minutesByDate = mutableMapOf<java.time.LocalDate, Float>()
        sessions.forEach { s ->
            val date = Instant.ofEpochMilli(s.startedAt).atZone(zone).toLocalDate()
            val interruptions = s.pauseResumeCount + s.appSwitchCount + s.interruptionBurstCount
            interruptionByDate[date] = (interruptionByDate[date] ?: 0) + interruptions
            minutesByDate[date] = (minutesByDate[date] ?: 0f) + s.durationMinutes.coerceAtLeast(0f)
        }
        return (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val label = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val interruptions = interruptionByDate[date] ?: 0
            val hours = (minutesByDate[date] ?: 0f) / 60f
            val rate = if (hours <= 0.05f) 0f else interruptions.toFloat() / hours
            label to rate
        }
    }
}

internal fun buildAbstentionRateTrend7d(
    predictions: List<EnergyPredictionEntity>,
    nowMillis: Long = System.currentTimeMillis()
): List<Pair<String, Float>> {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val totalsByDate = mutableMapOf<java.time.LocalDate, Int>()
    val abstainedByDate = mutableMapOf<java.time.LocalDate, Int>()

    predictions.forEach { prediction ->
        val date = Instant.ofEpochMilli(prediction.predictedAtMillis).atZone(zone).toLocalDate()
        val daysDelta = java.time.temporal.ChronoUnit.DAYS.between(date, today)
        if (daysDelta !in 0L..6L) return@forEach

        totalsByDate[date] = (totalsByDate[date] ?: 0) + 1
        if (prediction.peakConfidence <= 0.4f) {
            abstainedByDate[date] = (abstainedByDate[date] ?: 0) + 1
        }
    }

    return (6 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val label = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val total = totalsByDate[date] ?: 0
        val abstained = abstainedByDate[date] ?: 0
        val ratePercent = if (total == 0) 0f else (abstained.toFloat() / total.toFloat()) * 100f
        label to ratePercent
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationhistoryhistoryscreenkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/history/HistoryScreen.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/history/HistoryScreen.kt`
- **Lines of Code:** 479
- **Size:** 21,455 bytes (21.0 KB)

```kotlin
package com.neuroflow.app.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.presentation.common.TagChip
import com.neuroflow.app.presentation.common.formatDurationFloat
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    hideTopBar: Boolean = false,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var selectedSessions by remember { mutableStateOf<List<TimeSessionEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedTask = selectedTaskId?.let { id -> uiState.completedTasks.find { it.id == id } }

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                TopAppBar(
                    title = { Text("History") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.completedTasks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No completed tasks yet.\nStart completing tasks to see them here!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    HistoryDateRangeFilters(
                        selected = uiState.selectedDateRange,
                        onSelected = viewModel::updateDateRange
                    )

                    if (uiState.availableTags.isNotEmpty()) {
                        HistoryTagFilters(
                            tags = uiState.availableTags,
                            selectedTag = uiState.selectedTag,
                            onSelected = viewModel::updateTagFilter
                        )
                    }

                    if (uiState.completedTasks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No completed tasks in this date range.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(uiState.completedTasks, key = { it.id }) { task ->
                                HistoryTaskRow(
                                    task = task,
                                    onClick = {
                                        scope.launch {
                                            selectedSessions = viewModel.getSessionsForTask(task.id)
                                            selectedTaskId = task.id
                                        }
                                    },
                                    onDelete = {
                                        viewModel.deleteTask(task)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Task deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreTask(task)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTask?.let { task ->
        HistoryDetailSheet(
            task = task,
            sessions = selectedSessions,
            onDismiss = { selectedTaskId = null }
        )
    }
}

@Composable
private fun HistoryTagFilters(
    tags: List<String>,
    selectedTag: String?,
    onSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedTag == null,
            onClick = { onSelected(null) },
            label = { Text("All tags") }
        )
        tags.forEach { tag ->
            FilterChip(
                selected = selectedTag == tag,
                onClick = { onSelected(tag) },
                label = { Text(tag) }
            )
        }
    }
}

@Composable
private fun HistoryDateRangeFilters(
    selected: HistoryDateRange,
    onSelected: (HistoryDateRange) -> Unit
) {
    val options = listOf(
        HistoryDateRange.ALL to "All",
        HistoryDateRange.TODAY to "Today",
        HistoryDateRange.LAST_7_DAYS to "7 Days",
        HistoryDateRange.LAST_30_DAYS to "30 Days"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (range, label) ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelected(range) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun HistoryTaskRow(task: TaskEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                "Completed",
                tint = NeuroFlowColors.ScheduleText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.LineThrough
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (task.estimationErrorMape != null || task.totalTimeTrackedMinutes > 0) {
                    // MAPE
                    task.estimationErrorMape?.let { mape ->
                        val color = when {
                            mape < 20f -> NeuroFlowColors.MapeGood
                            mape < 50f -> NeuroFlowColors.MapeMedium
                            else -> NeuroFlowColors.MapeBad
                        }
                        Text(
                            text = "MAPE: ${String.format("%.1f", mape)}%",
                            fontSize = 12.sp,
                            color = color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "⏱ Time Tracking: ${formatDurationFloat(task.totalTimeTrackedMinutes)} spent",
                        fontSize = 12.sp,
                        color = Color(0xFF1565C0)
                    )
                    Text(
                        text = "📊 Sessions: ${task.sessionCount} session${if (task.sessionCount != 1) "s" else ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "No time tracking",
                        fontSize = 12.sp,
                        color = NeuroFlowColors.MapeGood.copy(alpha = 0.7f)
                    )
                }

                if (task.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        task.tags.split(",").filter { it.isNotBlank() }.take(3).forEach { tag ->
                            TagChip(tag.trim())
                        }
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp).align(Alignment.CenterVertically)) {
                Icon(Icons.Filled.Delete, "Delete", tint = NeuroFlowColors.MapeBad, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDetailSheet(
    task: TaskEntity,
    sessions: List<TimeSessionEntity>,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(task.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(task.description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))

            // ── Classification ────────────────────────────────────────────────
            DetailSection("Classification") {
                DetailRow("Quadrant", when (task.quadrant.name) {
                    "DO_FIRST"  -> "Q1: Do First"
                    "SCHEDULE"  -> "Q2: Schedule"
                    "DELEGATE"  -> "Q3: Delegate"
                    "ELIMINATE" -> "Q4: On Hold"
                    else        -> task.quadrant.name
                })
                DetailRow("Priority", task.priority.name)
                DetailRow("Task Type", when (task.taskType) {
                    TaskType.ANALYTICAL  -> "🧠 Analytical"
                    TaskType.CREATIVE    -> "🎨 Creative"
                    TaskType.ADMIN       -> "📋 Admin"
                    TaskType.PHYSICAL    -> "💪 Physical"
                    TaskType.GENERAL     -> "📌 General"
                    TaskType.MINDFULNESS -> "🧘 Mindfulness"
                })
                DetailRow("Energy Level", when (task.energyLevel) {
                    EnergyLevel.HIGH   -> "🔴 High"
                    EnergyLevel.MEDIUM -> "🟡 Medium"
                    EnergyLevel.LOW    -> "🟢 Low"
                })
                if (task.contextTag.isNotBlank()) DetailRow("Context", task.contextTag)
                if (task.tags.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Tags", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        task.tags.split(",").filter { it.isNotBlank() }.forEach { TagChip(it.trim()) }
                    }
                }
            }

            // ── Scoring ───────────────────────────────────────────────────────
            DetailSection("Scoring") {
                DetailRow("Impact", "${task.impactScore}/100")
                DetailRow("Value", "${task.valueScore}/100")
                DetailRow("Effort", "${task.effortScore}/100")
                DetailRow("Enjoyment", "${task.enjoymentScore}/100")
            }

            // ── Timing ────────────────────────────────────────────────────────
            DetailSection("Timing") {
                task.deadlineDate?.let { DetailRow("Deadline", sdf.format(Date(it))) }
                task.scheduledDate?.let { DetailRow("Scheduled", sdf.format(Date(it))) }
                if (task.estimatedDurationMinutes > 0)
                    DetailRow("Estimated", "${task.estimatedDurationMinutes} min")
                if (task.recurrence.name != "NONE") {
                    val recLabel = when (task.recurrence.name) {
                        "DAILY"   -> "Daily"
                        "WEEKLY"  -> "Weekly"
                        "MONTHLY" -> "Monthly"
                        "CUSTOM"  -> "Every ${task.recurrenceIntervalDays} day(s)"
                        else      -> task.recurrence.name
                    }
                    DetailRow("Recurrence", recLabel)
                }
                if (task.isScheduleLocked) DetailRow("Schedule", "🔒 Locked")
            }

            // ── Neuro Boost ───────────────────────────────────────────────────
            DetailSection("Neuro Boost") {
                if (task.isFrog) DetailRow("Frog", "🐸 Yes — hardest task first")
                if (task.isPublicCommitment) DetailRow("Public Commitment", "📢 Yes")
                if (task.isAnxietyTask) DetailRow("Anxiety Task", "😰 Yes")
                if (task.goalRiskLevel > 0) DetailRow("Goal Risk", when (task.goalRiskLevel) {
                    1 -> "⚠ At Risk"
                    2 -> "🚨 Critical"
                    else -> "None"
                })
                if (task.ifThenPlan.isNotBlank()) DetailRow("Step-by-step Plan", task.ifThenPlan)
                if (task.waitingFor.isNotBlank()) DetailRow("Waiting For", task.waitingFor)
                if (task.dependsOnTaskIds.isNotBlank()) DetailRow("Depends On", task.dependsOnTaskIds)
            }

            // ── Time Tracking ─────────────────────────────────────────────────
            DetailSection("Time Tracking") {
                DetailRow("Total Time", formatDurationFloat(task.totalTimeTrackedMinutes))
                DetailRow("Sessions", "${sessions.size}")
                if (sessions.isNotEmpty()) {
                    val avg = task.totalTimeTrackedMinutes / sessions.size
                    DetailRow("Avg Session", formatDurationFloat(avg))
                }
                task.actualDurationMinutes?.let { DetailRow("Actual Duration", formatDurationFloat(it)) }
                task.estimationErrorMape?.let {
                    val color = when {
                        it < 20f -> NeuroFlowColors.MapeGood
                        it < 50f -> NeuroFlowColors.MapeMedium
                        else     -> NeuroFlowColors.MapeBad
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("MAPE", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%.1f", it)}%", fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
                    }
                }
                if (task.habitStreak > 0) DetailRow("Habit Streak", "🔥 ${task.habitStreak}")
                if (task.postponeCount > 0) DetailRow("Deferred", "${task.postponeCount}×")
                task.completedAt?.let { DetailRow("Completed", sdf.format(Date(it))) }
            }

            // ── Session Details ───────────────────────────────────────────────
            if (sessions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Session Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                sessions.forEachIndexed { index, session ->
                    val dateFormatted = remember(session.startedAt) {
                        sdf.format(Date(session.startedAt))
                    }
                    com.neuroflow.app.presentation.v2.common.UpgradedHistorySessionRow(
                        taskTitle = "Session ${index + 1} • ${task.title}",
                        dateAndTimeText = dateFormatted,
                        durationMinutes = session.durationMinutes,
                        estimatedMinutes = task.estimatedDurationMinutes,
                        interruptionCount = session.interruptionBurstCount + session.appSwitchCount,
                        affectiveForecastError = task.affectiveForecastError,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowColors.Purple)
            ) { Text("Close") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(Modifier.height(6.dp))
    content()
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.5f))
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationhistoryhistoryviewmodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/history/HistoryViewModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/history/HistoryViewModel.kt`
- **Lines of Code:** 132
- **Size:** 4,645 bytes (4.5 KB)

```kotlin
package com.neuroflow.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class HistoryDateRange {
    ALL,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS
}

internal fun filterTasksByDateRange(
    tasks: List<TaskEntity>,
    range: HistoryDateRange,
    nowMillis: Long = System.currentTimeMillis()
): List<TaskEntity> {
    if (range == HistoryDateRange.ALL) return tasks

    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis
    val fromMillis = when (range) {
        HistoryDateRange.TODAY -> todayStart
        HistoryDateRange.LAST_7_DAYS -> Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis
        HistoryDateRange.LAST_30_DAYS -> Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -29)
        }.timeInMillis
        HistoryDateRange.ALL -> Long.MIN_VALUE
    }

    return tasks.filter { task ->
        val completedAt = task.completedAt ?: return@filter false
        completedAt >= fromMillis
    }
}

data class HistoryUiState(
    val completedTasks: List<TaskEntity> = emptyList(),
    val selectedDateRange: HistoryDateRange = HistoryDateRange.ALL,
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val selectedDateRange = MutableStateFlow(HistoryDateRange.ALL)
    private val selectedTag = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                taskRepository.observeCompletedTasks(),
                userPreferencesDataStore.preferencesFlow.map { it.tagCatalog }.distinctUntilChanged(),
                selectedDateRange,
                selectedTag
            ) { tasks, catalogTags, range, tag ->
                val dateFiltered = filterTasksByDateRange(tasks, range)
                val tagFiltered = if (tag.isNullOrBlank()) {
                    dateFiltered
                } else {
                    dateFiltered.filter { task ->
                        task.tags.split(",").map { it.trim() }.any { it.equals(tag, ignoreCase = true) }
                    }
                }
                val allTags = (catalogTags + tasks
                    .flatMap { it.tags.split(",") }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                )
                    .distinctBy { it.lowercase() }
                    .sortedWith(String.CASE_INSENSITIVE_ORDER)
                Triple(tagFiltered, range, allTags)
            }.collect { (tasks, range, tags) ->
                _uiState.update {
                    it.copy(
                        completedTasks = tasks,
                        selectedDateRange = range,
                        availableTags = tags,
                        selectedTag = selectedTag.value,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateDateRange(range: HistoryDateRange) {
        selectedDateRange.value = range
    }

    fun updateTagFilter(tag: String?) {
        selectedTag.value = tag
    }

    suspend fun getSessionsForTask(taskId: String): List<TimeSessionEntity> {
        return sessionRepository.getByTaskId(taskId)
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.delete(task) }
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.insert(task) }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2insightsinsightsscreenv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsScreenV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsScreenV2.kt`
- **Lines of Code:** 1,181
- **Size:** 50,579 bytes (49.4 KB)

```kotlin
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
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2insightsinsightstabbedupgradesv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsTabbedUpgradesV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsTabbedUpgradesV2.kt`
- **Lines of Code:** 362
- **Size:** 14,711 bytes (14.4 KB)

```kotlin
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
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2insightsinsightsviewmodelv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsViewModelV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsViewModelV2.kt`
- **Lines of Code:** 314
- **Size:** 14,229 bytes (13.9 KB)

```kotlin
package com.neuroflow.app.presentation.v2.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.scheduler.BehavioralInsightsEngine
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class InsightsMetricsUiModel(
    val velocity7d: Int = 0,
    val velocity30d: Int = 0,
    val focusHours7d: Float = 0f,
    val focusHours30d: Float = 0f,
    val weightedSmape: Float = 0f,
    val smapeGrade: String = "Calibrating",
    val hourlyDensity: List<Float> = emptyList()
)

data class InsightsUiStateV2(
    val energyUi: EnergyUiModel? = null,
    val analyticsSummary: AnalyticsEngine.AnalyticsSummary? = null,
    val planningCalibrationFactor: Float = 1.0f,
    val preferences: UserPreferences = UserPreferences(),
    val circadianAlignmentRate: Int = 85,
    val interruptionPauseResumeCount: Int = 0,
    val interruptionAppSwitchCount: Int = 0,
    val interruptionBurstCount: Int = 0,
    val focusEfficiencyPercent: Int = 92,
    val dailyStreak: Int = 0,
    val frogsEatenCount: Int = 0,
    val totalFocusMinutesToday: Int = 0,
    val totalFocusMinutesWeek: Int = 0,
    val totalTasksCompletedToday: Int = 0,
    val activeContractsCount: Int = 0,
    val neuroscienceInterventions: List<String> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class InsightsViewModelV2 @Inject constructor(
    val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    val contractRepository: UlyssesContractRepository,
    val workManager: WorkManager? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val geminiAiOnlineService: com.neuroflow.app.domain.service.GeminiAiOnlineService = com.neuroflow.app.domain.service.DefaultGeminiAiOnlineService()
) : ViewModel() {

    private val _isCalibratingCoach = MutableStateFlow(false)
    val isCalibratingCoach: StateFlow<Boolean> = _isCalibratingCoach.asStateFlow()

    fun calibrateAiCoach() {
        viewModelScope.launch {
            _isCalibratingCoach.value = true
            try {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val allTasks = taskRepository.getAllTasks()
                val metrics = computeSmapeAndCircadianDensity(allTasks, prefs)

                val telemetryJson = org.json.JSONObject().apply {
                    put("completionRatePercent", if (allTasks.isNotEmpty()) (allTasks.count { it.completedAt != null } * 100 / allTasks.size) else 0)
                    put("weightedSmape", metrics.weightedSmape)
                    put("velocity7d", metrics.velocity7d)
                    put("focusHours7d", metrics.focusHours7d)
                    put("circadianPeakStart", prefs.peakEnergyStart)
                    put("circadianPeakEnd", prefs.peakEnergyEnd)
                }.toString()

                val deletedJson = prefs.recentlyDeletedTasksJson.ifBlank { "[]" }
                val postponedTasks = allTasks.filter { it.postponeCount >= 2 && it.status.name == "ACTIVE" }.take(10)
                val postponedJson = org.json.JSONArray().apply {
                    postponedTasks.forEach { t ->
                        put(org.json.JSONObject().apply {
                            put("title", t.title)
                            put("estimatedMinutes", t.estimatedDurationMinutes)
                            put("category", t.taskType.name)
                            put("postponeCount", t.postponeCount)
                        })
                    }
                }.toString()

                val result = geminiAiOnlineService.conductWeeklyAuditAndDirective(
                    weeklyTelemetryJson = telemetryJson,
                    recentlyDeletedTasksJson = deletedJson,
                    chronicallyPostponedTasksJson = postponedJson,
                    apiKey = prefs.geminiApiKey,
                    useAi = prefs.geminiAiEnabled
                )

                preferencesDataStore.updateAdaptiveSystemDirective(
                    directive = result.newSystemDirective,
                    auditJson = result.avoidanceAuditJson
                )
            } catch (e: Exception) {
                android.util.Log.e("InsightsViewModelV2", "Failed to calibrate AI coach", e)
            } finally {
                _isCalibratingCoach.value = false
            }
        }
    }

    val uiState: StateFlow<InsightsUiStateV2> = combine(
        taskRepository.observeAll(),
        sessionRepository.observeAll(),
        preferencesDataStore.preferencesFlow,
        contractRepository.observeActive(),
        sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())
    ) { allTasks, allSessions, prefs, activeContracts, sleepLogs ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        val startOfWeek = startOfToday - 6 * 86400_000L

        val todaySessions = allSessions.filter { it.startedAt >= startOfToday || (it.endedAt != null && it.endedAt!! >= startOfToday) }
        val weekSessions = allSessions.filter { it.startedAt >= startOfWeek }

        val todayFocusMinutes = todaySessions.sumOf { it.trackedMinutes(now).toInt() }
        val weekFocusMinutes = weekSessions.sumOf { it.trackedMinutes(now).toInt() }

        val pauses = todaySessions.sumOf { it.pauseResumeCount }
        val appSwitches = todaySessions.sumOf { it.appSwitchCount }
        val bursts = todaySessions.sumOf { it.interruptionBurstCount }

        val completedToday = allTasks.filter {
            it.completedAt != null && it.completedAt >= startOfToday
        }
        val frogsToday = completedToday.count { it.isFrog }

        // Circadian alignment: % of high energy tasks executed or completed during peak window
        val peakStart = prefs.peakEnergyStart.takeIf { it > 0 } ?: 9
        val peakEnd = prefs.peakEnergyEnd.takeIf { it > 0 } ?: 12
        val highEnergyTasks = allTasks.filter { it.energyLevel == EnergyLevel.HIGH }
        val alignedHighEnergy = highEnergyTasks.count { task ->
            val scheduledH = task.scheduledTime?.let { (it / 3600_000L).toInt() }
            scheduledH == null || scheduledH in peakStart until peakEnd
        }
        val alignmentRate = if (highEnergyTasks.isNotEmpty()) {
            ((alignedHighEnergy.toFloat() / highEnergyTasks.size) * 100).toInt().coerceIn(40, 100)
        } else 85

        val totalMinutes = todaySessions.sumOf { it.trackedMinutes(now).toDouble() }
        val penaltyMinutes = (pauses * 0.5 + appSwitches * 0.75)
        val efficiency = if (totalMinutes > 0.0) {
            (((totalMinutes - penaltyMinutes).coerceAtLeast(0.0) / totalMinutes) * 100).toInt().coerceIn(30, 99)
        } else 95

        // Calculate continuous Borbély circadian snapshot & energyUi
        val (_, energyUi) = computeEnergyUiState(
            circadianEnergyService = circadianEnergyService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            prefs = prefs,
            sleepLogs = sleepLogs,
            nowMillis = now
        )

        // Compute full AnalyticsSummary with real sessions off the main thread
        val analyticsSummary = AnalyticsEngine.buildSummary(
            allTasks = allTasks,
            allSessions = allSessions,
            prefs = prefs
        )
        val planningCalibrationFactor = AnalyticsEngine.PlanningCalibration.from(allTasks).globalFactor

        // Behavioral interventions
        val interventions = mutableListOf<String>()
        val pressure = energyUi.sleepPressurePoints
        val zone = energyUi.fatigueZone

        when (zone) {
            FatigueZone.CRITICAL, FatigueZone.HIGH -> {
                interventions.add("⚠️ Process S Sleep Pressure is critical ($pressure pts). Adenosine accumulation is impairing working memory. Prioritize sleep or 20m Non-Sleep Deep Rest (NSDR).")
            }
            FatigueZone.MODERATE -> {
                interventions.add("☕ Moderate cognitive fatigue detected. Shift to structured low-effort tasks or take a 10m movement break.")
            }
            FatigueZone.RESTED -> {
                interventions.add("⚡ High neurochemical capacity available. Excellent window for deep focus, creative breakthroughs, and Frogs.")
            }
        }

        if (appSwitches > 5) {
            interventions.add("📱 Context-switching penalty: $appSwitches app switches today. Consider parking intrusive thoughts in the Thought Parking Lot to maintain flow state.")
        }

        if (frogsToday > 0) {
            interventions.add("🐸 Frogs conquered today: $frogsToday! Dopamine pathways are primed for sustained momentum.")
        }

        InsightsUiStateV2(
            energyUi = energyUi,
            analyticsSummary = analyticsSummary,
            planningCalibrationFactor = planningCalibrationFactor,
            preferences = prefs,
            circadianAlignmentRate = alignmentRate,
            interruptionPauseResumeCount = pauses,
            interruptionAppSwitchCount = appSwitches,
            interruptionBurstCount = bursts,
            focusEfficiencyPercent = efficiency,
            dailyStreak = prefs.dailyStreak,
            frogsEatenCount = frogsToday,
            totalFocusMinutesToday = todayFocusMinutes,
            totalFocusMinutesWeek = weekFocusMinutes,
            totalTasksCompletedToday = completedToday.size,
            activeContractsCount = activeContracts.size,
            neuroscienceInterventions = interventions,
            isLoading = false
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            InsightsUiStateV2()
        )

    val insightsMetricsFlow: StateFlow<InsightsMetricsUiModel> = combine(
        taskRepository.observeAll(),
        preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
            old.peakEnergyStart == new.peakEnergyStart &&
                old.peakEnergyEnd == new.peakEnergyEnd
        }
    ) { allTasks, prefs ->
        computeSmapeAndCircadianDensity(allTasks, prefs)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            InsightsMetricsUiModel()
        )

    fun computeSmapeAndCircadianDensity(
        allTasks: List<TaskEntity>,
        prefs: UserPreferences,
        now: Long = System.currentTimeMillis()
    ): InsightsMetricsUiModel {
        val sevenDaysAgo = now - 7L * 86400_000L
        val thirtyDaysAgo = now - 30L * 86400_000L

        val completed7d = allTasks.filter { it.completedAt != null && it.completedAt >= sevenDaysAgo }
        val completed30d = allTasks.filter { it.completedAt != null && it.completedAt >= thirtyDaysAgo }

        val velocity7d = completed7d.size
        val velocity30d = completed30d.size
        val focusHours7d = completed7d.sumOf { (it.actualDurationMinutes ?: it.totalTimeTrackedMinutes).toDouble() }.toFloat() / 60f
        val focusHours30d = completed30d.sumOf { (it.actualDurationMinutes ?: it.totalTimeTrackedMinutes).toDouble() }.toFloat() / 60f

        val weightedSmape = AnalyticsEngine.computeWeightedSmape(allTasks)
        val smapeGrade = AnalyticsEngine.mapeGrade(weightedSmape)

        val map = mutableMapOf<Int, Int>()
        allTasks.forEach { task ->
            val hour = when {
                task.completedAt != null -> {
                    Calendar.getInstance().apply { timeInMillis = task.completedAt }.get(Calendar.HOUR_OF_DAY)
                }
                task.scheduledTime != null -> {
                    ((task.scheduledTime / 3600_000L) % 24).toInt()
                }
                else -> {
                    Calendar.getInstance().apply { timeInMillis = task.updatedAt }.get(Calendar.HOUR_OF_DAY)
                }
            }
            val mins = (task.actualDurationMinutes ?: task.totalTimeTrackedMinutes).toInt()
            if (mins > 0) {
                map[hour] = (map[hour] ?: 0) + mins
            }
        }
        if (map.isEmpty()) {
            val pStart = prefs.peakEnergyStart.takeIf { it > 0 } ?: 9
            val pEnd = prefs.peakEnergyEnd.takeIf { it > 0 } ?: 12
            for (h in pStart until pEnd) {
                map[h] = 45
            }
        }
        val hourlyDensity = BehavioralInsightsEngine.calculateHourlyCircadianDensity(map)

        return InsightsMetricsUiModel(
            velocity7d = velocity7d,
            velocity30d = velocity30d,
            focusHours7d = focusHours7d,
            focusHours30d = focusHours30d,
            weightedSmape = weightedSmape,
            smapeGrade = smapeGrade,
            hourlyDensity = hourlyDensity
        )
    }
}
```

---

