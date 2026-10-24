# neuroFlow UI Codebase — Flow Execution Cockpit & Circadian Barometer

**Description:** FlowScreenV2, FlowViewModelV2, EnhancedCircadianEnergyGraph, CircadianHeroBarometer, and EnergyTimelineStream.  
**Generated for Vice Head Review:** 2026-10-23 22:05:10  
**Files:** 6  
**Lines of Code:** 4,197  
**Uncompressed Size:** 184.2 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/presentation/v2/flow/CircadianHeroBarometer.kt`](#appsrcmainjavacomneuroflowapppresentationv2flowcircadianherobarometerkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt`](#appsrcmainjavacomneuroflowapppresentationv2flowenergytimelinestreamkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnhancedCircadianEnergyGraph.kt`](#appsrcmainjavacomneuroflowapppresentationv2flowenhancedcircadianenergygraphkt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowHubUpgradesV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2flowflowhubupgradesv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2flowflowscreenv2kt)
- [`app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt`](#appsrcmainjavacomneuroflowapppresentationv2flowflowviewmodelv2kt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowapppresentationv2flowcircadianherobarometerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/CircadianHeroBarometer.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/CircadianHeroBarometer.kt`
- **Lines of Code:** 949
- **Size:** 43,511 bytes (42.5 KB)

```kotlin
package com.neuroflow.app.presentation.v2.flow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.CircadianEnergySummary
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.domain.service.StimulantType
import com.neuroflow.app.domain.service.PharmacokineticStimulantCalculator
import com.neuroflow.app.presentation.v2.theme.LocalNeuroFlowColors
import com.neuroflow.app.presentation.v2.theme.NeuroFlowExtendedColors
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.phaseAccentColor
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CircadianHeroBarometer(
    summary: CircadianEnergySummary,
    dayTimeline: List<CircadianSnapshot>,
    modifier: Modifier = Modifier,
    cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
    naps: List<DaytimeNapBout> = emptyList(),
    isFallbackSleep: Boolean = false,
    learnedSampleCount: Double = 0.0,
    stimulantDoses: List<StimulantDoseBout> = emptyList(),
    isCrashWindow: Boolean = false,
    activePlasmaMg: Double = 0.0,
    socialJetlagLagHours: Double = 0.0,
    predictedSwsSuppressionPercent: Int = 0,
    latestSafeStimulantCutoffFormatted: String = "16:11",
    onClearStimulants: () -> Unit = {},
    onRemoveStimulantDose: (StimulantDoseBout) -> Unit = {},
    onLogStimulant: (StimulantType) -> Unit = {},
    onLogPhysiologicalEvent: (PhysiologicalEventType) -> Unit = {},
    onClearPhysiologicalEvents: () -> Unit = {}
) {
    val colors = LocalNeuroFlowColors.current
    val activeSnapshot = dayTimeline.minByOrNull {
        kotlin.math.abs(it.timestampMillis - System.currentTimeMillis())
    }
    val activePhase = activeSnapshot?.phase ?: summary.currentPhase
    val accent = phaseAccentColor(activePhase, colors)
    val cardShape = RoundedCornerShape(24.dp)
    var isGraphExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(cardShape)
            .border(
                width = 1.dp,
                brush = NeuroFlowPaletteV2.specularBorderBrush(accent, alphaBoost = 0.85f),
                shape = cardShape
            ),
        color = NeuroFlowPaletteV2.SurfaceCard,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
        ) {
            // Header: Phase Badge + Battery Score + Collapsible 24h Curve Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhaseBadge(phase = activePhase, accent = accent)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${summary.currentEnergyScore}% Battery",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (dayTimeline.size >= 2) {
                    Surface(
                        onClick = { isGraphExpanded = !isGraphExpanded },
                        shape = RoundedCornerShape(50),
                        color = NeuroFlowPaletteV2.SurfaceElevated,
                        border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isGraphExpanded) "24h Curve ▲" else "24h Curve ▼",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NeuroFlowPaletteV2.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isGraphExpanded && dayTimeline.size >= 2) {
                EnhancedCircadianEnergyGraph(
                    timeline = dayTimeline,
                    currentMillis = System.currentTimeMillis(),
                    cognitiveBlocks = cognitiveBlocks,
                    naps = naps,
                    isFallbackSleep = isFallbackSleep,
                    learnedSampleCount = learnedSampleCount,
                    embeddedInHeroCard = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Compact 3-metric strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeuroFlowPaletteV2.SurfaceElevated.copy(alpha = 0.6f))
                        .border(1.dp, NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fmt = DateTimeFormatter.ofPattern("HH:mm")
                    Column {
                        Text("PEAK", style = MaterialTheme.typography.labelSmall, color = NeuroFlowPaletteV2.TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${summary.peakStart.format(fmt)}–${summary.peakEnd.format(fmt)}", style = MaterialTheme.typography.labelSmall, color = NeuroFlowPaletteV2.TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BUDGET", style = MaterialTheme.typography.labelSmall, color = NeuroFlowPaletteV2.TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${summary.cognitiveBudgetMinutesRemaining}m", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CUTOFF", style = MaterialTheme.typography.labelSmall, color = NeuroFlowPaletteV2.TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(latestSafeStimulantCutoffFormatted.ifBlank { "--:--" }, style = MaterialTheme.typography.labelSmall, color = NeuroFlowPaletteV2.TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            PhaseGuidanceRow(
                phase = activePhase,
                cognitiveBudget = summary.cognitiveBudgetMinutesRemaining,
                peakStart = summary.peakStart,
                peakEnd = summary.peakEnd,
                dipStart = summary.dipStart,
                dipEnd = summary.dipEnd,
                colors = colors,
            )

            Spacer(modifier = Modifier.height(12.dp))

            StimulantQuickLogRow(
                stimulantDoses = stimulantDoses,
                isCrashWindow = isCrashWindow,
                activePlasmaMg = activePlasmaMg,
                predictedSwsSuppressionPercent = predictedSwsSuppressionPercent,
                latestSafeStimulantCutoffFormatted = latestSafeStimulantCutoffFormatted,
                onClearStimulants = onClearStimulants,
                onRemoveStimulantDose = onRemoveStimulantDose,
                onLogStimulant = onLogStimulant,
                onLogPhysiologicalEvent = onLogPhysiologicalEvent,
                onClearPhysiologicalEvents = onClearPhysiologicalEvents,
                colors = colors
            )

            if (kotlin.math.abs(socialJetlagLagHours) >= 1.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🌅", fontSize = 13.sp)
                        Text(
                            text = "Weekend Drift: ${if (socialJetlagLagHours > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", socialJetlagLagHours)}h phase shift. Morning light exposure recommended.",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircadianHeroBarometer(
    rawEnergyScore: Float,
    batteryPercent: Int,
    energyZone: EnergyLevel,
    sleepPressurePoints: Int = 450,
    fatigueZone: FatigueZone = FatigueZone.RESTED,
    circadianFactor: Float = 0.85f,
    recommendedTask: TaskEntity? = null,
    onStartRecommendedTask: (String) -> Unit = {},
    onNavigateToMEQ: () -> Unit = {},
    onNavigateToSleepLogs: () -> Unit = {},
    dayTimeline: List<CircadianSnapshot> = emptyList(),
    cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
    naps: List<DaytimeNapBout> = emptyList(),
    isFallbackSleep: Boolean = false,
    learnedSampleCount: Double = 0.0,
    stimulantDoses: List<StimulantDoseBout> = emptyList(),
    isCrashWindow: Boolean = false,
    activePlasmaMg: Double = 0.0,
    socialJetlagLagHours: Double = 0.0,
    predictedSwsSuppressionPercent: Int = 0,
    latestSafeStimulantCutoffFormatted: String = "16:11",
    onClearStimulants: () -> Unit = {},
    onRemoveStimulantDose: (StimulantDoseBout) -> Unit = {},
    onLogStimulant: (StimulantType) -> Unit = {},
    onLogPhysiologicalEvent: (PhysiologicalEventType) -> Unit = {},
    onClearPhysiologicalEvents: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val phase = when {
        batteryPercent >= 75 -> EnergyPhase.PEAK
        batteryPercent >= 55 -> EnergyPhase.REBOUND
        batteryPercent >= 35 -> EnergyPhase.DIP
        batteryPercent >= 20 -> EnergyPhase.WIND_DOWN
        else -> EnergyPhase.RECOVERY_NEEDED
    }

    val summary = remember(batteryPercent, phase, sleepPressurePoints) {
        CircadianEnergySummary(
            currentEnergyScore = batteryPercent,
            currentPhase = phase,
            phaseConfidence = 0.9f,
            peakStart = LocalTime.of(9, 30),
            peakEnd = LocalTime.of(12, 30),
            dipStart = LocalTime.of(13, 30),
            dipEnd = LocalTime.of(15, 30),
            windDownStart = LocalTime.of(21, 30),
            sleepPressureNormalized = (sleepPressurePoints / 1000f).coerceIn(0f, 1f),
            cognitiveBudgetMinutesRemaining = (batteryPercent * 3).coerceIn(30, 360),
            calibrationQuality = 0.85f
        )
    }

    val effectiveTimeline = remember(dayTimeline, batteryPercent) {
        if (dayTimeline.isNotEmpty()) {
            dayTimeline
        } else {
            val baseTime = System.currentTimeMillis() - (12 * 3600_000L)
            List(25) { i ->
                val frac = i / 24f
                val cWave = 0.5f - 0.35f * cos(frac * 2 * Math.PI).toFloat() + 0.15f * sin(frac * 4 * Math.PI).toFloat()
                val score = (cWave * 100f).roundToInt().coerceIn(10, 95)
                val itemPhase = when {
                    score >= 75 -> EnergyPhase.PEAK
                    score >= 55 -> EnergyPhase.REBOUND
                    score >= 35 -> EnergyPhase.DIP
                    score >= 20 -> EnergyPhase.WIND_DOWN
                    else -> EnergyPhase.RECOVERY_NEEDED
                }
                CircadianSnapshot(
                    timestampMillis = baseTime + (i * 3600_000L),
                    usableEnergy = score,
                    homeostaticPressureS = 0.4,
                    circadianAlertnessC = cWave.toDouble(),
                    focusDepletionPenalty = 0.0,
                    phase = itemPhase,
                    sleepInertiaPenalty = 0.0
                )
            }
        }
    }

    CircadianHeroBarometer(
        summary = summary,
        dayTimeline = effectiveTimeline,
        modifier = modifier,
        cognitiveBlocks = cognitiveBlocks,
        naps = naps,
        isFallbackSleep = isFallbackSleep,
        learnedSampleCount = learnedSampleCount,
        stimulantDoses = stimulantDoses,
        isCrashWindow = isCrashWindow,
        activePlasmaMg = activePlasmaMg,
        socialJetlagLagHours = socialJetlagLagHours,
        predictedSwsSuppressionPercent = predictedSwsSuppressionPercent,
        latestSafeStimulantCutoffFormatted = latestSafeStimulantCutoffFormatted,
        onClearStimulants = onClearStimulants,
        onRemoveStimulantDose = onRemoveStimulantDose,
        onLogStimulant = onLogStimulant,
        onLogPhysiologicalEvent = onLogPhysiologicalEvent,
        onClearPhysiologicalEvents = onClearPhysiologicalEvents
    )
}

@Composable
fun PhaseBadge(phase: EnergyPhase, accent: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(accent, CircleShape)
            )
            Text(
                text = PhaseBadgeText(phase),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
fun PhaseGuidanceRow(
    phase: EnergyPhase,
    cognitiveBudget: Int,
    peakStart: LocalTime,
    peakEnd: LocalTime,
    dipStart: LocalTime,
    dipEnd: LocalTime,
    colors: NeuroFlowExtendedColors,
) {
    val guidance = PhaseRecommendation(phase, peakStart, peakEnd, dipStart, dipEnd)
    val accent = phaseAccentColor(phase, colors)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NeuroFlowPaletteV2.SurfaceElevated.copy(alpha = 0.7f))
            .border(1.dp, NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (phase) {
                    EnergyPhase.PEAK -> "⚡"
                    EnergyPhase.REBOUND -> "📈"
                    EnergyPhase.DIP -> "☕"
                    EnergyPhase.WIND_DOWN -> "🌙"
                    EnergyPhase.RECOVERY_NEEDED -> "🧘"
                },
                fontSize = 15.sp,
            )
        }
        Text(
            text = guidance,
            style = MaterialTheme.typography.bodySmall,
            color = NeuroFlowPaletteV2.TextSecondary,
            modifier = Modifier.weight(1f),
            lineHeight = 17.sp,
        )
        Surface(
            color = accent.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "${cognitiveBudget}m left",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

fun PhaseBadgeText(phase: EnergyPhase): String = when (phase) {
    EnergyPhase.PEAK -> "PEAK COGNITIVE"
    EnergyPhase.REBOUND -> "ENERGY REBOUND"
    EnergyPhase.DIP -> "CIRCADIAN DIP"
    EnergyPhase.WIND_DOWN -> "WIND-DOWN"
    EnergyPhase.RECOVERY_NEEDED -> "RECOVERY NEEDED"
}

fun PhaseRecommendation(
    phase: EnergyPhase,
    peakStart: LocalTime,
    peakEnd: LocalTime,
    dipStart: LocalTime,
    dipEnd: LocalTime,
): String {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    return when (phase) {
        EnergyPhase.PEAK -> "Tackle highest-leverage deep work. Peak window active until ${peakEnd.format(fmt)}."
        EnergyPhase.REBOUND -> "Secondary alertness wave. Ideal for collaborative execution."
        EnergyPhase.DIP -> "Biological trough (${dipStart.format(fmt)}–${dipEnd.format(fmt)}). Switch to light admin or rest."
        EnergyPhase.RECOVERY_NEEDED -> "Synaptic fatigue detected. Take a 15m NSDR or walk before resuming."
        EnergyPhase.WIND_DOWN -> "Melatonin rising. Close open loops and transition to shutdown mode."
    }
}

@Composable
fun ActiveDoseHistoryDialog(
    stimulantDoses: List<StimulantDoseBout>,
    onDismiss: () -> Unit,
    onRemoveDose: (StimulantDoseBout) -> Unit,
    zoneId: ZoneId = ZoneId.systemDefault()
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Active Stimulant Doses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeuroFlowPaletteV2.TextPrimary
            )
        },
        text = {
            if (stimulantDoses.isEmpty()) {
                Text(
                    text = "No active doses recorded in the last 24h.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuroFlowPaletteV2.TextSecondary
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tap ✕ to remove any accidentally logged dose:",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowPaletteV2.TextSecondary
                    )
                    stimulantDoses.sortedByDescending { it.timestampMillis }.forEach { dose ->
                        val timeStr = java.time.Instant.ofEpochMilli(dose.timestampMillis)
                            .atZone(zoneId)
                            .format(timeFormatter)
                        val emoji = when (dose.type) {
                            StimulantType.CAFFEINE -> "☕"
                            StimulantType.GREEN_TEA_L_THEANINE -> "🍵"
                            StimulantType.PRESCRIPTION_STIMULANT_IR, StimulantType.PRESCRIPTION_STIMULANT_XR -> "💊"
                            else -> "⚡"
                        }
                        val typeName = when (dose.type) {
                            StimulantType.CAFFEINE -> "Caffeine"
                            StimulantType.GREEN_TEA_L_THEANINE -> "Green Tea"
                            StimulantType.PRESCRIPTION_STIMULANT_IR -> "Meds IR"
                            StimulantType.PRESCRIPTION_STIMULANT_XR -> "Meds XR"
                            else -> dose.type.name
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowPaletteV2.SurfaceElevated,
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(emoji, fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = "$typeName (~${dose.doseMg.roundToInt()}mg)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeuroFlowPaletteV2.TextPrimary
                                        )
                                        Text(
                                            text = "Logged at $timeStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowPaletteV2.TextSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onRemoveDose(dose) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove dose",
                                        tint = NeuroFlowPaletteV2.RecoveryRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = NeuroFlowPaletteV2.OptimalEmerald, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun StimulantQuickLogRow(
    stimulantDoses: List<StimulantDoseBout>,
    isCrashWindow: Boolean,
    activePlasmaMg: Double = 0.0,
    predictedSwsSuppressionPercent: Int = 0,
    latestSafeStimulantCutoffFormatted: String = "16:11",
    onClearStimulants: () -> Unit = {},
    onRemoveStimulantDose: (StimulantDoseBout) -> Unit = {},
    onLogStimulant: (StimulantType) -> Unit,
    onLogPhysiologicalEvent: ((PhysiologicalEventType) -> Unit)? = null,
    onClearPhysiologicalEvents: (() -> Unit)? = null,
    colors: NeuroFlowExtendedColors,
    modifier: Modifier = Modifier
) {
    var showDoseHistoryDialog by remember { mutableStateOf(false) }

    if (showDoseHistoryDialog) {
        ActiveDoseHistoryDialog(
            stimulantDoses = stimulantDoses,
            onDismiss = { showDoseHistoryDialog = false },
            onRemoveDose = { dose ->
                onRemoveStimulantDose(dose)
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isCrashWindow) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⚡", fontSize = 13.sp)
                    Text(
                        text = "Crash Guard active: Stimulant wear-off in progress — protecting you from high-friction tasks.",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors.WarmAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Active Plasma Status, SWS Suppression & Safe Cutoff
        if (activePlasmaMg > 0.05 || stimulantDoses.isNotEmpty() || latestSafeStimulantCutoffFormatted.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (activePlasmaMg > 0.05 || stimulantDoses.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeuroFlowPaletteV2.SurfaceElevated,
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(NeuroFlowPaletteV2.OptimalEmerald, CircleShape)
                                )
                                Text(
                                    text = if (activePlasmaMg >= 1.0) "~${activePlasmaMg.roundToInt()}mg active" else "<1mg active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowPaletteV2.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Safe Stimulant Cutoff Pill
                    if (latestSafeStimulantCutoffFormatted.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeuroFlowPaletteV2.SurfaceElevated,
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🛑", fontSize = 10.sp)
                                Text(
                                    text = "Cutoff: $latestSafeStimulantCutoffFormatted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowPaletteV2.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Bedtime SWS Suppression Risk
                    if (predictedSwsSuppressionPercent > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("🌙", fontSize = 10.sp)
                                Text(
                                    text = "SWS -$predictedSwsSuppressionPercent%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors.WarmAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (activePlasmaMg > 0.05 || stimulantDoses.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (stimulantDoses.isNotEmpty()) {
                            TextButton(
                                onClick = { showDoseHistoryDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(
                                    text = "Doses (${stimulantDoses.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowPaletteV2.OptimalEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        TextButton(
                            onClick = onClearStimulants,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowPaletteV2.TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Horizontally scrollable quick-log chips (prevents screen overflow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quick Log:",
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowPaletteV2.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )

            // + Coffee (100mg)
            Surface(
                onClick = { onLogStimulant(StimulantType.CAFFEINE) },
                shape = RoundedCornerShape(50),
                color = NeuroFlowPaletteV2.SurfaceElevated,
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("☕", fontSize = 12.sp)
                    Text(
                        text = "+ Coffee (100mg)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // + Green Tea (50mg)
            Surface(
                onClick = { onLogStimulant(StimulantType.GREEN_TEA_L_THEANINE) },
                shape = RoundedCornerShape(50),
                color = NeuroFlowPaletteV2.SurfaceElevated,
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🍵", fontSize = 12.sp)
                    Text(
                        text = "+ Green Tea (50mg)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // + Meds IR (10mg)
            Surface(
                onClick = { onLogStimulant(StimulantType.PRESCRIPTION_STIMULANT_IR) },
                shape = RoundedCornerShape(50),
                color = NeuroFlowPaletteV2.SurfaceElevated,
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("💊", fontSize = 12.sp)
                    Text(
                        text = "+ Meds IR (10mg)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // + Meds XR (20mg)
            Surface(
                onClick = { onLogStimulant(StimulantType.PRESCRIPTION_STIMULANT_XR) },
                shape = RoundedCornerShape(50),
                color = NeuroFlowPaletteV2.SurfaceElevated,
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("💊", fontSize = 12.sp)
                    Text(
                        text = "+ Meds XR (20mg)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowPaletteV2.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (onLogPhysiologicalEvent != null) {
                // + Meal Dip
                Surface(
                    onClick = { onLogPhysiologicalEvent(PhysiologicalEventType.MEAL) },
                    shape = RoundedCornerShape(50),
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🍽️", fontSize = 12.sp)
                        Text(
                            text = "+ Meal Dip",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // + Workout
                Surface(
                    onClick = { onLogPhysiologicalEvent(PhysiologicalEventType.WORKOUT) },
                    shape = RoundedCornerShape(50),
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🏃", fontSize = 12.sp)
                        Text(
                            text = "+ Workout",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // + 20m Power Nap
                Surface(
                    onClick = { onLogPhysiologicalEvent(PhysiologicalEventType.NAP) },
                    shape = RoundedCornerShape(50),
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("💤", fontSize = 12.sp)
                        Text(
                            text = "+ 20m Nap",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (onClearPhysiologicalEvents != null) {
                    Surface(
                        onClick = onClearPhysiologicalEvents,
                        shape = RoundedCornerShape(50),
                        color = NeuroFlowPaletteV2.SurfaceElevated,
                        border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🧹", fontSize = 12.sp)
                            Text(
                                text = "Clear Bio",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowPaletteV2.TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2flowenergytimelinestreamkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt`
- **Lines of Code:** 650
- **Size:** 29,009 bytes (28.3 KB)

```kotlin
package com.neuroflow.app.presentation.v2.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.presentation.common.ManualTimeLogSheet
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase

@Composable
fun EnergyTimelineStream(
    scheduledTasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onDeferTask: (TaskEntity) -> Unit = {},
    onBreakDownTask: (TaskEntity) -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {},
    onShiftToPeak: ((TaskEntity) -> Unit)? = null,
    timeline: List<CircadianSnapshot> = emptyList(),
    modifier: Modifier = Modifier,
    viewModel: FlowViewModelV2 = hiltViewModel()
) {
    var taskToLogTime by remember { mutableStateOf<TaskEntity?>(null) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Energy Stream",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeuroFlowThemeV2Colors.TextPrimary
            )
            Text(
                text = "${scheduledTasks.size} blocks",
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowThemeV2Colors.TextSecondary
            )
        }

        if (scheduledTasks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NeuroFlowThemeV2Colors.SlateSurface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🧘 Quiet Waters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "No tasks scheduled for today. Tap the '+' composer to plan your next hyperfocus session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }
        } else {
            val nowMillis = System.currentTimeMillis()
            val activeOrNextIndex = scheduledTasks.indexOfFirst { task ->
                val start = task.resolveScheduledEpochMillis() ?: task.scheduledDate ?: 0L
                val end = start + task.estimatedDurationMinutes * 60_000L
                end >= nowMillis
            }
            var cumulativeMinutes = 0

            scheduledTasks.forEachIndexed { index, task ->
                val scheduledMillis = task.resolveScheduledEpochMillis() ?: task.scheduledDate
                val taskPhase = if (scheduledMillis != null && timeline.isNotEmpty()) {
                    timeline.minByOrNull { kotlin.math.abs(it.timestampMillis - scheduledMillis) }?.phase ?: EnergyPhase.PEAK
                } else EnergyPhase.PEAK
                val phaseColor = NeuroFlowPaletteV2.accentForPhase(taskPhase)

                if (index == activeOrNextIndex) {
                    // Pulsing "NOW" playhead indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeuroFlowPaletteV2.ReboundCyan, CircleShape)
                        )
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NeuroFlowPaletteV2.ReboundCyan,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeuroFlowPaletteV2.ReboundCyan, Color.Transparent)
                                    )
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Phase-colored vertical time-rail node & track
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 18.dp)
                                .size(10.dp)
                                .background(phaseColor, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f, fill = false)
                                .heightIn(min = 40.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(phaseColor.copy(alpha = 0.5f), phaseColor.copy(alpha = 0.15f))
                                    )
                                )
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        key(task.id) {
                            TimelineTaskCard(
                                task = task,
                                onTaskClick = { onTaskClick(task) },
                                onComplete = { onCompleteTask(task) },
                                onDefer = { onDeferTask(task) },
                                onBreakDown = { onBreakDownTask(task) },
                                onEdit = { onEditTask(task) },
                                onShiftToPeak = onShiftToPeak?.let { shift -> { shift(task) } },
                                onLogPastTime = { taskToLogTime = task },
                                timeline = timeline
                            )
                        }
                    }
                }

                cumulativeMinutes += task.estimatedDurationMinutes
                val hasNext = index < scheduledTasks.size - 1
                if (hasNext) {
                    val nextTask = scheduledTasks[index + 1]
                    val isBetweenSplitParts = (task.id.contains("#part1") || task.id.contains("_part1") || task.title.contains("(Part 1/2)")) &&
                            (nextTask.id.contains("#part2") || nextTask.id.contains("_part2") || nextTask.title.contains("(Part 2/2)"))
                    if (isBetweenSplitParts || cumulativeMinutes >= 90) {
                        UltradianRecoveryDivider()
                        cumulativeMinutes = 0
                    }
                }
            }
        }
    }

    taskToLogTime?.let { task ->
        ManualTimeLogSheet(
            taskTitle = task.title,
            prefillMinutes = task.estimatedDurationMinutes,
            onConfirm = { minutes ->
                viewModel.logPastTime(task, minutes)
                taskToLogTime = null
            },
            onSkip = { taskToLogTime = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTaskCard(
    task: TaskEntity,
    onTaskClick: () -> Unit,
    onComplete: () -> Unit,
    onDefer: () -> Unit,
    onBreakDown: () -> Unit = {},
    onEdit: () -> Unit = {},
    onShiftToPeak: (() -> Unit)? = null,
    onLogPastTime: () -> Unit = {},
    timeline: List<CircadianSnapshot> = emptyList()
) {
    val energyBadgeColor = when (task.energyLevel) {
        EnergyLevel.HIGH -> NeuroFlowThemeV2Colors.WarmAmber
        EnergyLevel.MEDIUM -> NeuroFlowThemeV2Colors.ElectricIndigoGlow
        EnergyLevel.LOW -> NeuroFlowThemeV2Colors.SageGreen
    }

    val microSteps = StepByStepPlanCodec.parse(task.ifThenPlan)
    val hasMicroSteps = microSteps.isNotEmpty()
    var showDetailSheet by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.55f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onComplete()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDefer()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> NeuroFlowThemeV2Colors.ElectricIndigoGlow.copy(alpha = 0.25f)
                SwipeToDismissBoxValue.EndToStart -> NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.25f)
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.CheckCircle
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.MoreTime
                SwipeToDismissBoxValue.Settled -> null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeuroFlowThemeV2Colors.TextPrimary
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { showDetailSheet = true }),
            shape = RoundedCornerShape(16.dp),
            color = NeuroFlowThemeV2Colors.SlateSurface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Leading Complete Button (48dp touch target)
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Complete task",
                        tint = NeuroFlowThemeV2Colors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Middle: Frog emoji + Title (up to 2 lines) + Metadata badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (task.isFrog) {
                            Text("🐸", fontSize = 14.sp)
                        }
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NeuroFlowThemeV2Colors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${task.estimatedDurationMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )

                        Box(
                            modifier = Modifier
                                .background(energyBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.energyLevel.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = energyBadgeColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (hasMicroSteps) {
                            Text(
                                text = "🪜 ${microSteps.size} steps",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (task.impactScore >= 70) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.WarmAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "High Leverage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.WarmAmber
                                )
                            }
                        }

                        val isUltradianPart = task.id.contains("#part") || task.title.contains("(Part ")
                        if (isUltradianPart) {
                            val partLabel = if (task.id.contains("#part2") || task.title.contains("(Part 2/2)")) "🔗 Part 2/2" else "🔗 Part 1/2"
                            Box(
                                modifier = Modifier
                                    .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = partLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (task.postponeCount >= 3) {
                            Box(
                                modifier = Modifier
                                    .background(NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⚡ Dopamine Warm-Up",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowThemeV2Colors.WarmAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    val scheduledMillis = task.resolveScheduledEpochMillis() ?: task.scheduledDate
                    if (scheduledMillis != null && timeline.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TaskSlotEnergyFitBadge(
                            taskScheduledMillis = scheduledMillis,
                            taskEnergyDemand = task.energyLevel,
                            timeline = timeline,
                            onShiftToPeakClick = onShiftToPeak
                        )
                    }
                }

                // Trailing Quick Focus start button (48dp touch target)
                IconButton(
                    onClick = onTaskClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Focus",
                        tint = NeuroFlowThemeV2Colors.ElectricIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showDetailSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
            dragHandle = { BottomSheetDefaults.DragHandle(color = NeuroFlowThemeV2Colors.TextSecondary.copy(alpha = 0.5f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (task.isFrog) {
                            Text("🐸", fontSize = 20.sp)
                        }
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.TextPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            showDetailSheet = false
                            onEdit()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit task", tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏱ ${task.estimatedDurationMinutes} mins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                    Text("•", color = NeuroFlowThemeV2Colors.TextSecondary)
                    Text(
                        text = "Energy: ${task.energyLevel.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = energyBadgeColor
                    )
                }

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }

                if (hasMicroSteps) {
                    Text(
                        text = "Micro-steps (${microSteps.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        microSteps.forEachIndexed { index, step ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = step.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeuroFlowThemeV2Colors.TextPrimary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showDetailSheet = false
                            onEdit()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Task")
                    }

                    OutlinedButton(
                        onClick = {
                            showDetailSheet = false
                            onBreakDown()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Breakdown")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showDetailSheet = false
                            onDefer()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    ) {
                        Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Defer +2h")
                    }

                    OutlinedButton(
                        onClick = {
                            showDetailSheet = false
                            onLogPastTime()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Log Time")
                    }
                }

                Button(
                    onClick = {
                        showDetailSheet = false
                        onTaskClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start Focus", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2flowenhancedcircadianenergygraphkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnhancedCircadianEnergyGraph.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnhancedCircadianEnergyGraph.kt`
- **Lines of Code:** 650
- **Size:** 26,939 bytes (26.3 KB)

```kotlin
package com.neuroflow.app.presentation.v2.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.v2.theme.LocalNeuroFlowPalette
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun EnhancedCircadianEnergyGraph(
    timeline: List<CircadianSnapshot>,
    currentMillis: Long,
    cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
    naps: List<DaytimeNapBout> = emptyList(),
    isFallbackSleep: Boolean = false,
    learnedSampleCount: Double = 0.0,
    zoneId: ZoneId = ZoneId.systemDefault(),
    embeddedInHeroCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (timeline.size < 2) return
    val haptics = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }
    var lastHapticPhase by remember { mutableStateOf<EnergyPhase?>(null) }

    val palette = LocalNeuroFlowPalette.current
    val isDark = palette.isDark

    val startMillis = timeline.first().timestampMillis
    val endMillis = timeline.last().timestampMillis
    val totalSpanMillis = (endMillis - startMillis).coerceAtLeast(1L)

    val activeSnapshot = scrubbedIndex?.let { timeline.getOrNull(it) }
        ?: timeline.minByOrNull { abs(it.timestampMillis - currentMillis) }
        ?: timeline.first()

    val PeakEmerald = palette.energyPeakMint
    val ReboundIndigo = palette.primaryIndigo
    val DipAmber = palette.energyModerateAmber
    val RecoveryRose = palette.energyLowCoral
    val WindDownViolet = palette.hyperFocusViolet
    val GridLineColor = palette.border.copy(alpha = if (isDark) 0.4f else 0.55f)
    val TextPrimary = palette.textPrimary
    val TextSecondary = palette.textSecondary

    fun resolvePhaseColor(phase: EnergyPhase): Color = when (phase) {
        EnergyPhase.PEAK -> PeakEmerald
        EnergyPhase.REBOUND -> ReboundIndigo
        EnergyPhase.DIP -> DipAmber
        EnergyPhase.RECOVERY_NEEDED -> RecoveryRose
        EnergyPhase.WIND_DOWN -> WindDownViolet
    }

    val activeAccent = resolvePhaseColor(activeSnapshot.phase)

    val containerModifier = if (embeddedInHeroCard) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface)
            .border(1.dp, NeuroFlowPaletteV2.specularBorderBrush(activeAccent), RoundedCornerShape(22.dp))
            .padding(16.dp)
    }

    Column(modifier = containerModifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = if (scrubbedIndex != null) "SCRUBBING BIO-TIMELINE" else "CIRCADIAN ENERGY HORIZON",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (scrubbedIndex != null) activeAccent else TextSecondary,
                    letterSpacing = 1.0.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${activeSnapshot.usableEnergy}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = activeAccent
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = activeAccent.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    PhaseBadge(phase = activeSnapshot.phase, accent = activeAccent)
                }
            }

            val calibrationText = when {
                isFallbackSleep -> "Est. Wake • Log Sleep"
                learnedSampleCount >= 3.0 -> "AI Calibrated (${(activeSnapshot.predictionConfidence * 100).roundToInt()}%)"
                learnedSampleCount > 0.0 -> "Learning (${learnedSampleCount.roundToInt()}/3)"
                else -> "Baseline Model"
            }
            val pillAccent = if (isFallbackSleep) RecoveryRose else ReboundIndigo
            Surface(
                color = pillAccent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, pillAccent.copy(alpha = 0.35f))
            ) {
                Text(
                    text = calibrationText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = pillAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedVisibility(
            visible = scrubbedIndex != null || activeSnapshot.focusDepletionPenalty > 0.01 || abs(activeSnapshot.learnedAdjustmentPoints) >= 1.5,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FactorDecompositionStrip(
                snapshot = activeSnapshot,
                zoneId = zoneId,
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                surfaceSubtle = palette.surfaceElevated
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .pointerInput(timeline) {
                    detectTapGestures(
                        onPress = { offset ->
                            val idx = ((offset.x / size.width) * (timeline.size - 1))
                                .roundToInt()
                                .coerceIn(0, timeline.lastIndex)
                            scrubbedIndex = idx
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tryAwaitRelease()
                            scrubbedIndex = null
                        }
                    )
                }
                .pointerInput(timeline) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val idx = ((offset.x / size.width) * (timeline.size - 1))
                                .roundToInt()
                                .coerceIn(0, timeline.lastIndex)
                            scrubbedIndex = idx
                            lastHapticPhase = timeline[idx].phase
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val idx = ((change.position.x / size.width) * (timeline.size - 1))
                                .roundToInt()
                                .coerceIn(0, timeline.lastIndex)
                            if (scrubbedIndex != idx) {
                                scrubbedIndex = idx
                                val newPhase = timeline[idx].phase
                                if (newPhase != lastHapticPhase) {
                                    lastHapticPhase = newPhase
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                        onDragEnd = { scrubbedIndex = null },
                        onDragCancel = { scrubbedIndex = null }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padTop = 18.dp.toPx()
                val padBottom = 26.dp.toPx()
                val chartHeight = (size.height - padTop - padBottom).coerceAtLeast(1f)
                val chartWidth = size.width

                fun timeToX(millis: Long): Float =
                    (((millis - startMillis).toFloat() / totalSpanMillis.toFloat()) * chartWidth).coerceIn(0f, chartWidth)

                fun energyToY(score: Float): Float =
                    padTop + chartHeight * (1f - (score.coerceIn(0f, 100f) / 100f))

                listOf(25f, 50f, 75f).forEach { level ->
                    val y = energyToY(level)
                    drawLine(
                        color = GridLineColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (level == 50f) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
                    )
                }

                drawPhaseBands(timeline, chartWidth, padTop, chartHeight, PeakEmerald, DipAmber)

                val blockTrackY = padTop + chartHeight - 6.dp.toPx()
                val dayStartMillis = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()

                cognitiveBlocks.forEach { block ->
                    val bStartMillis = if (block.startMillis > 100_000_000_000L) block.startMillis else dayStartMillis + block.startMinuteOfDay * 60_000L
                    val bEndMillis = if (block.endMillis > 100_000_000_000L) {
                        block.endMillis
                    } else {
                        val endMin = if (block.endMinuteOfDay >= block.startMinuteOfDay) block.endMinuteOfDay else block.endMinuteOfDay + 1440
                        dayStartMillis + endMin * 60_000L
                    }
                    val x1 = timeToX(bStartMillis)
                    val x2 = timeToX(bEndMillis).coerceAtLeast(x1 + 5.dp.toPx())
                    val blockEnergy = timeline.minByOrNull { abs(it.timestampMillis - bStartMillis) }?.usableEnergy ?: 60
                    val isMismatch = block.demand == EnergyDemand.HIGH && blockEnergy < 50
                    val blockColor = when {
                        isMismatch -> RecoveryRose
                        block.isFrog -> DipAmber
                        block.demand == EnergyDemand.HIGH -> ReboundIndigo
                        else -> WindDownViolet
                    }
                    drawRoundRect(
                        color = blockColor.copy(alpha = if (block.isCompleted) 0.45f else 0.9f),
                        topLeft = Offset(x1, blockTrackY),
                        size = Size(x2 - x1, 5.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }

                naps.forEach { nap ->
                    val x1 = timeToX(nap.startMillis)
                    val x2 = timeToX(nap.endMillis).coerceAtLeast(x1 + 5.dp.toPx())
                    drawRoundRect(
                        color = PeakEmerald.copy(alpha = 0.8f),
                        topLeft = Offset(x1, blockTrackY),
                        size = Size(x2 - x1, 5.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }

                val usablePoints = timeline.mapIndexed { i, snap ->
                    val x = (i.toFloat() / (timeline.size - 1)) * chartWidth
                    val y = energyToY(snap.usableEnergy.toFloat())
                    Offset(x, y)
                }
                val baselinePoints = timeline.mapIndexed { i, snap ->
                    val x = (i.toFloat() / (timeline.size - 1)) * chartWidth
                    val y = energyToY(snap.baselineBiologicalEnergy.toFloat())
                    Offset(x, y)
                }

                val ribbonPath = Path().apply {
                    val upperPoints = timeline.mapIndexed { i, snap ->
                        val spread = (1.0 - snap.predictionConfidence).toFloat() * 14f
                        Offset(usablePoints[i].x, energyToY((snap.usableEnergy + spread).coerceAtMost(100f)))
                    }
                    val lowerPoints = timeline.mapIndexed { i, snap ->
                        val spread = (1.0 - snap.predictionConfidence).toFloat() * 14f
                        Offset(usablePoints[i].x, energyToY((snap.usableEnergy - spread).coerceAtLeast(0f)))
                    }.reversed()

                    if (upperPoints.isNotEmpty()) {
                        moveTo(upperPoints.first().x, upperPoints.first().y)
                        upperPoints.drop(1).forEach { lineTo(it.x, it.y) }
                        lowerPoints.forEach { lineTo(it.x, it.y) }
                        close()
                    }
                }
                drawPath(
                    path = ribbonPath,
                    color = ReboundIndigo.copy(alpha = if (isDark) 0.08f else 0.06f)
                )

                val baselinePath = buildMonotoneCubicPath(baselinePoints)
                drawPath(
                    path = baselinePath,
                    color = TextSecondary.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))
                    )
                )

                val usablePath = buildMonotoneCubicPath(usablePoints)
                val fillPath = Path().apply {
                    addPath(usablePath)
                    lineTo(chartWidth, padTop + chartHeight)
                    lineTo(0f, padTop + chartHeight)
                    close()
                }
                val horizontalBrush = Brush.horizontalGradient(
                    colors = timeline.map { resolvePhaseColor(it.phase) }
                )
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeAccent.copy(alpha = if (isDark) 0.28f else 0.18f),
                            activeAccent.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        startY = padTop,
                        endY = padTop + chartHeight
                    )
                )

                drawPath(
                    path = usablePath,
                    brush = horizontalBrush,
                    alpha = if (isDark) 0.25f else 0.15f,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                drawPath(
                    path = usablePath,
                    brush = horizontalBrush,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (isFallbackSleep) PathEffect.dashPathEffect(floatArrayOf(16f, 8f)) else null
                    )
                )

                val targetMillis = activeSnapshot.timestampMillis
                val markerX = timeToX(targetMillis)
                val markerY = energyToY(activeSnapshot.usableEnergy.toFloat())

                drawLine(
                    color = if (scrubbedIndex != null) activeAccent.copy(alpha = 0.7f) else TextSecondary.copy(alpha = 0.35f),
                    start = Offset(markerX, padTop),
                    end = Offset(markerX, padTop + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = if (scrubbedIndex == null) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null
                )

                drawCircle(
                    color = activeAccent.copy(alpha = 0.28f),
                    radius = 11.dp.toPx(),
                    center = Offset(markerX, markerY)
                )
                drawCircle(
                    color = activeAccent,
                    radius = 5.5.dp.toPx(),
                    center = Offset(markerX, markerY)
                )
                drawCircle(
                    color = palette.surface,
                    radius = 2.2.dp.toPx(),
                    center = Offset(markerX, markerY)
                )

                drawHourAxisLabels(
                    startMillis = startMillis,
                    totalSpanMillis = totalSpanMillis,
                    chartWidth = chartWidth,
                    labelY = padTop + chartHeight + 6.dp.toPx(),
                    zoneId = zoneId,
                    labelColor = TextSecondary.copy(alpha = 0.65f),
                    textMeasurer = textMeasurer
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = PeakEmerald, label = "Usable Energy", textColor = TextSecondary)
            LegendItem(color = TextSecondary.copy(alpha = 0.5f), label = "Bio Potential", isDashed = true, textColor = TextSecondary)
            LegendItem(color = DipAmber, label = "Frog / Deep Work", textColor = TextSecondary)
        }
    }
}

@Composable
private fun FactorDecompositionStrip(
    snapshot: CircadianSnapshot,
    zoneId: ZoneId,
    textPrimary: Color,
    textSecondary: Color,
    surfaceSubtle: Color
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
    val timeText = Instant.ofEpochMilli(snapshot.timestampMillis).atZone(zoneId).format(timeFormatter)
    val cPoints = (snapshot.circadianAlertnessC * 55.0).roundToInt()
    val sDebtPenalty = -(snapshot.homeostaticPressureS * 45.0).roundToInt()
    val focusPenalty = -(snapshot.focusDepletionPenalty * 100.0).roundToInt()
    val learnedAdj = snapshot.learnedAdjustmentPoints.roundToInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = textPrimary
        )
        FactorChip(label = "Drive", value = "+$cPoints", color = NeuroFlowPaletteV2.OptimalEmerald, labelColor = textSecondary)
        FactorChip(label = "Sleep Debt", value = "$sDebtPenalty", color = NeuroFlowPaletteV2.PeakAmber, labelColor = textSecondary)
        if (focusPenalty < 0) {
            FactorChip(label = "Load", value = "$focusPenalty", color = NeuroFlowPaletteV2.RecoveryRose, labelColor = textSecondary)
        }
        if (learnedAdj != 0) {
            val signPrefix = if (learnedAdj > 0) "+" else ""
            FactorChip(label = "AI", value = "$signPrefix$learnedAdj", color = NeuroFlowPaletteV2.DipIndigo, labelColor = textSecondary)
        }
    }
}

@Composable
private fun FactorChip(label: String, value: String, color: Color, labelColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, textColor: Color, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = if (isDashed) 2.dp else 6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontSize = 10.sp
        )
    }
}

private fun buildMonotoneCubicPath(points: List<Offset>): Path {
    val path = Path()
    val n = points.size
    if (n == 0) return path
    path.moveTo(points[0].x, points[0].y)
    if (n == 1) return path
    if (n == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    val dx = FloatArray(n - 1)
    val dy = FloatArray(n - 1)
    val slopes = FloatArray(n - 1)

    for (i in 0 until n - 1) {
        dx[i] = (points[i + 1].x - points[i].x).coerceAtLeast(1e-4f)
        dy[i] = points[i + 1].y - points[i].y
        slopes[i] = dy[i] / dx[i]
    }

    val tangents = FloatArray(n)
    tangents[0] = slopes[0]
    tangents[n - 1] = slopes[n - 2]

    for (i in 1 until n - 1) {
        if (slopes[i - 1] * slopes[i] <= 0f) {
            tangents[i] = 0f
        } else {
            tangents[i] = (slopes[i - 1] + slopes[i]) * 0.5f
        }
    }

    for (i in 0 until n - 1) {
        if (abs(slopes[i]) < 1e-5f) {
            tangents[i] = 0f
            tangents[i + 1] = 0f
        } else {
            val a = tangents[i] / slopes[i]
            val b = tangents[i + 1] / slopes[i]
            val h = hypot(a, b)
            if (h > 3f) {
                val scale = 3f / h
                tangents[i] = scale * a * slopes[i]
                tangents[i + 1] = scale * b * slopes[i]
            }
        }
    }

    for (i in 0 until n - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val step = dx[i] / 3f
        val cp1x = p0.x + step
        val cp1y = p0.y + tangents[i] * step
        val cp2x = p1.x - step
        val cp2y = p1.y - tangents[i + 1] * step
        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p1.x, p1.y)
    }

    return path
}

private fun DrawScope.drawPhaseBands(
    timeline: List<CircadianSnapshot>,
    chartWidth: Float,
    padTop: Float,
    chartHeight: Float,
    peakColor: Color,
    dipColor: Color
) {
    if (timeline.size < 2) return
    val slotWidth = chartWidth / (timeline.size - 1)
    timeline.forEachIndexed { idx, snap ->
        val x = idx * slotWidth
        when (snap.phase) {
            EnergyPhase.PEAK -> {
                drawRect(
                    color = peakColor.copy(alpha = 0.05f),
                    topLeft = Offset(x, padTop),
                    size = Size(slotWidth + 1f, chartHeight)
                )
            }
            EnergyPhase.DIP -> {
                drawRect(
                    color = dipColor.copy(alpha = 0.045f),
                    topLeft = Offset(x, padTop),
                    size = Size(slotWidth + 1f, chartHeight)
                )
            }
            else -> Unit
        }
    }
}

private fun DrawScope.drawHourAxisLabels(
    startMillis: Long,
    totalSpanMillis: Long,
    chartWidth: Float,
    labelY: Float,
    zoneId: ZoneId,
    labelColor: Color,
    textMeasurer: TextMeasurer
) {
    val labelStyle = TextStyle(
        color = labelColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
    val hoursToMark = listOf(6, 10, 14, 18, 22)
    val startDate = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()

    hoursToMark.forEach { hour ->
        val tickMillis = startDate.atTime(hour, 0).atZone(zoneId).toInstant().toEpochMilli()
        if (tickMillis in startMillis..(startMillis + totalSpanMillis)) {
            val x = ((tickMillis - startMillis).toFloat() / totalSpanMillis.toFloat()) * chartWidth
            val label = when {
                hour == 0 -> "12a"
                hour < 12 -> "${hour}a"
                hour == 12 -> "12p"
                else -> "${hour - 12}p"
            }
            val measured = textMeasurer.measure(label, style = labelStyle)
            val centeredX = (x - measured.size.width / 2f).coerceIn(0f, chartWidth - measured.size.width)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(centeredX, labelY)
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2flowflowhubupgradesv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowHubUpgradesV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowHubUpgradesV2.kt`
- **Lines of Code:** 644
- **Size:** 28,659 bytes (28.0 KB)

```kotlin
package com.neuroflow.app.presentation.v2.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.engine.FreshStartEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.scheduler.DueSlot
import com.neuroflow.app.domain.scheduler.SlotPhase
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.remember
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import kotlin.math.roundToInt

@Composable
fun FlowSpotlightHeroCard(
    activeTakeoverSlot: DueSlot?,
    topTaskId: String?,
    topTaskTitle: String?,
    topTaskFirstMicroStep: String?,
    topTaskDurationMinutes: Int,
    topTaskEnergyLevel: EnergyLevel,
    isTopTaskFrog: Boolean,
    currentPhase: EnergyPhase,
    onStartFocusClick: (String) -> Unit,
    onCompleteQuickClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    approachingSlot: DueSlot? = null,
    topTaskMicroSteps: List<StepByStepPlanStep> = emptyList()
) {
    val phaseAccent = NeuroFlowPaletteV2.accentForPhase(currentPhase)
    val cardShape = RoundedCornerShape(24.dp)

    if (activeTakeoverSlot != null) {
        Surface(
            color = NeuroFlowPaletteV2.SurfaceCard,
            shape = cardShape,
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.OptimalEmerald, 1f),
                    shape = cardShape
                )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = NeuroFlowPaletteV2.OptimalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeTakeoverSlot.phase == SlotPhase.RUNNING) "PROTECTED RECOVERY ACTIVE" else "WRAPPING PROTECTED SLOT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NeuroFlowPaletteV2.OptimalEmerald,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "${activeTakeoverSlot.minutesLeft}m left",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activeTakeoverSlot.slot.label,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = NeuroFlowPaletteV2.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deep work is paused to protect your ${activeTakeoverSlot.slot.category.name.lowercase()} window and discharge synaptic fatigue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuroFlowPaletteV2.TextSecondary
                )
            }
        }
        return
    }

    if (topTaskId == null || topTaskTitle.isNullOrBlank()) return

    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = cardShape,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = NeuroFlowPaletteV2.specularBorderBrush(phaseAccent, 1f),
                shape = cardShape
            )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(phaseAccent.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(20.dp)
        ) {
            if (approachingSlot != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🛡️", fontSize = 14.sp)
                        Text(
                            text = "${approachingSlot.slot.label} starts in ${approachingSlot.minutesLeft}m — fit a quick doorway step before transition",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NeuroFlowPaletteV2.PeakAmber
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTopTaskFrog) "🐸 DAILY FROG SPOTLIGHT" else "⚡ OPTIMAL NEXT ACTION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = phaseAccent,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                ) {
                    Text(
                        text = "${topTaskDurationMinutes}m · ${topTaskEnergyLevel.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NeuroFlowPaletteV2.TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = topTaskTitle,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = NeuroFlowPaletteV2.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (topTaskMicroSteps.isNotEmpty()) {
                val completedCount = topTaskMicroSteps.count { it.completed }
                val totalCount = topTaskMicroSteps.size
                val currentStep = topTaskMicroSteps.firstOrNull { !it.completed } ?: topTaskMicroSteps.first()
                val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeuroFlowPaletteV2.SurfaceElevated)
                        .border(1.dp, NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🪜 2m Doorway: Step ${completedCount + 1} of $totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NeuroFlowPaletteV2.ReboundCyan
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        trackColor = NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.3f),
                    )
                    Text(
                        text = currentStep.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowPaletteV2.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (!topTaskFirstMicroStep.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeuroFlowPaletteV2.SurfaceElevated)
                        .border(1.dp, NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2m Doorway:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = topTaskFirstMicroStep,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowPaletteV2.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onStartFocusClick(topTaskId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = phaseAccent,
                        contentColor = Color(0xFF0A0B0E)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Launch Focus",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Surface(
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCompleteQuickClick(topTaskId) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = "Complete Task",
                            tint = NeuroFlowPaletteV2.OptimalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FreshStartRecoveryBanner(
    visible: Boolean,
    resetPlan: FreshStartEngine.EmergencyResetPlan?,
    onApplyEmergencyReset: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && resetPlan != null && resetPlan.tasksPushedToBacklog.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val plan = resetPlan ?: return@AnimatedVisibility
        val shape = RoundedCornerShape(18.dp)
        Surface(
            color = NeuroFlowPaletteV2.SurfaceCard,
            shape = shape,
            border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowPaletteV2.DipIndigo, 0.9f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.10f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShieldMoon,
                        contentDescription = null,
                        tint = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Schedule Feeling Heavy?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeuroFlowPaletteV2.TextPrimary
                        )
                        Text(
                            text = "Keep ${plan.tasksToReschedule.size} essential task(s) today, move ${plan.tasksPushedToBacklog.size} to backlog, and freeze your streak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowPaletteV2.TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Not Now",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextSecondary
                        )
                    }
                    Surface(
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onApplyEmergencyReset() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Fresh Start",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskSlotEnergyFitBadge(
    taskScheduledMillis: Long,
    taskEnergyDemand: EnergyLevel,
    timeline: List<CircadianSnapshot>,
    onShiftToPeakClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (timeline.isEmpty()) return
    val slotSnapshot = timeline.minByOrNull { kotlin.math.abs(it.timestampMillis - taskScheduledMillis) } ?: return
    val isMismatch = taskEnergyDemand == EnergyLevel.HIGH && slotSnapshot.usableEnergy < 52
    val isOptimalFit = (taskEnergyDemand == EnergyLevel.HIGH && slotSnapshot.usableEnergy >= 70) ||
        (taskEnergyDemand == EnergyLevel.LOW && slotSnapshot.phase == EnergyPhase.DIP)
    val badgeColor = when {
        isMismatch -> NeuroFlowPaletteV2.RecoveryRose
        isOptimalFit -> NeuroFlowPaletteV2.OptimalEmerald
        else -> NeuroFlowPaletteV2.ReboundCyan
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor.copy(alpha = 0.12f))
                .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${slotSnapshot.usableEnergy}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = badgeColor
            )
            if (slotSnapshot.focusDepletionPenalty >= 0.04) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(Load -${(slotSnapshot.focusDepletionPenalty * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowPaletteV2.PeakAmber
                )
            }
        }
        if (isMismatch && onShiftToPeakClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.16f))
                    .border(1.dp, NeuroFlowPaletteV2.PeakAmber.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .clickable { onShiftToPeakClick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠ Dip Mismatch • Shift to Peak",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = NeuroFlowPaletteV2.PeakAmber
                )
            }
        }
    }
}

@Composable
fun UltradianRecoveryDivider(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = NeuroFlowPaletteV2.SurfaceCard,
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("☕", fontSize = 13.sp)
                Text(
                    text = "15m Ultradian Rest Buffer",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeuroFlowPaletteV2.OptimalEmerald
                )
            }
            Text(
                text = "Replenish Prefrontal Glucose",
                style = MaterialTheme.typography.labelSmall,
                color = NeuroFlowPaletteV2.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ActiveBiophysicalImpulseStrip(
    cognitiveBlocks: List<CognitiveBlockWindow>,
    activePlasmaMg: Double,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val activeBlocks = remember(cognitiveBlocks, now) {
        cognitiveBlocks.filter { block ->
            val start = block.explicitStartMillis ?: return@filter false
            val end = block.explicitEndMillis ?: return@filter false
            now in start..end
        }
    }
    val hasActiveImpulse = activeBlocks.isNotEmpty() || activePlasmaMg > 0.05
    if (!hasActiveImpulse) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = NeuroFlowPaletteV2.SurfaceElevated.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🧬", fontSize = 12.sp)
            Text(
                text = "Active Impulses:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NeuroFlowPaletteV2.TextSecondary
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activePlasmaMg > 0.05) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Stimulant PK (~${activePlasmaMg.roundToInt().coerceAtLeast(1)}mg)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.OptimalEmerald,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                activeBlocks.forEach { block ->
                    val (label: String, color: Color) = when (block.category) {
                        CognitiveBlockCategory.MEAL -> "Postprandial Dip" to NeuroFlowPaletteV2.PeakAmber
                        CognitiveBlockCategory.EXERCISE -> "Post-Exercise BDNF" to NeuroFlowPaletteV2.ReboundCyan
                        CognitiveBlockCategory.SLEEP -> "Rest Recovery" to NeuroFlowPaletteV2.OptimalEmerald
                        else -> block.category.name to NeuroFlowPaletteV2.TextSecondary
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}


```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2flowflowscreenv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt`
- **Lines of Code:** 441
- **Size:** 20,219 bytes (19.7 KB)

```kotlin
package com.neuroflow.app.presentation.v2.flow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.FreshStartEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.common.FrogConfettiOverlay
import com.neuroflow.app.presentation.v2.composer.SheetMode
import com.neuroflow.app.presentation.v2.composer.SmartTaskComposerSheetV2
import com.neuroflow.app.presentation.v2.notes.QuickNotesBottomSheet
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FlowScreenV2(
    onNavigateToSettings: () -> Unit,
    onStartFocus: (String) -> Unit,
    onNavigateToMEQ: () -> Unit = {},
    onNavigateToSleepLogs: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    initialOpenComposer: Boolean = false,
    onInitialComposerConsumed: () -> Unit = {},
    viewModel: FlowViewModelV2 = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    var showComposer by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showFrogConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(initialOpenComposer) {
        if (initialOpenComposer) {
            showComposer = true
            onInitialComposerConsumed()
        }
    }

    // 4-Second Non-Destructive Undo Snackbar
    LaunchedEffect(state.lastActionMessage) {
        val msg = state.lastActionMessage
        if (!msg.isNullOrBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                when {
                    msg.startsWith("Completed") -> viewModel.undoCompleteTask()
                    msg.startsWith("Deferred") -> viewModel.undoDeferTask()
                    msg.startsWith("Stimulant logged:") -> viewModel.undoLogStimulant()
                    msg.startsWith("Bio-event logged:") -> viewModel.undoLogPhysiologicalEvent()
                }
            }
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 72.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    contentColor = NeuroFlowThemeV2Colors.TextPrimary,
                    actionColor = NeuroFlowThemeV2Colors.WarmAmber,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NeuroFlowThemeV2Colors.ElectricIndigo)
                    .combinedClickable(
                        onClick = { showComposer = true },
                        onLongClick = { showNotesSheet = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Quick Add Task (Long-press for Notes)",
                    tint = NeuroFlowThemeV2Colors.TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPanelPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPanelPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "neuroFlow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Biological High Performance",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { showNotesSheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Quick Scratchpad Notes",
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }
                }
            }

            // Hero Circadian Barometer (Borbély Two-Process Model)
            val energyScore = state.energyUi?.rawEnergy ?: 50f
            val battery = state.energyUi?.availableEnergy ?: 72
            val currentEnergyLevel = when {
                battery >= 65 -> EnergyLevel.HIGH
                battery >= 35 -> EnergyLevel.MEDIUM
                else -> EnergyLevel.LOW
            }

            CircadianHeroBarometer(
                rawEnergyScore = energyScore,
                batteryPercent = battery,
                energyZone = currentEnergyLevel,
                sleepPressurePoints = state.energyUi?.sleepPressurePoints ?: 450,
                fatigueZone = state.energyUi?.fatigueZone ?: com.neuroflow.app.domain.model.FatigueZone.RESTED,
                circadianFactor = state.energyUi?.circadianFactor ?: 0.85f,
                recommendedTask = state.recommendedTask,
                onStartRecommendedTask = { taskId -> onStartFocus(taskId) },
                onNavigateToMEQ = onNavigateToMEQ,
                onNavigateToSleepLogs = onNavigateToSleepLogs,
                dayTimeline = state.energyTimeline,
                cognitiveBlocks = state.dailyCognitiveBlocks,
                naps = state.daytimeNaps,
                isFallbackSleep = state.isFallbackSleep,
                learnedSampleCount = state.learnedSampleCount,
                stimulantDoses = state.stimulantDoses,
                isCrashWindow = state.isCrashWindow,
                activePlasmaMg = state.activePlasmaMg,
                socialJetlagLagHours = state.socialJetlagLagHours,
                predictedSwsSuppressionPercent = state.predictedSwsSuppressionPercent,
                latestSafeStimulantCutoffFormatted = state.latestSafeStimulantCutoffFormatted,
                onClearStimulants = { viewModel.clearAllStimulants() },
                onRemoveStimulantDose = { dose -> viewModel.removeStimulantDose(dose.timestampMillis, dose.type) },
                onLogStimulant = { type -> viewModel.logStimulant(type) },
                onLogPhysiologicalEvent = { type -> viewModel.logPhysiologicalEvent(type) },
                onClearPhysiologicalEvents = { viewModel.clearAllPhysiologicalEvents() }
            )

            // Active Biophysical Impulse Strip (Meal, Workout, Nap, Stimulant PK ticker)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ActiveBiophysicalImpulseStrip(
                    cognitiveBlocks = state.dailyCognitiveBlocks,
                    activePlasmaMg = state.activePlasmaMg
                )
            }

            // Flow Spotlight Hero Card (Takeover or Top Action)
            val nowMillis = System.currentTimeMillis()
            val activeTakeoverSlot = remember(state.preferences.protectedTimeSlots, nowMillis) {
                ProtectedSlotClock.takeoverOf(state.preferences.protectedTimeSlots, nowMillis)
            }
            val approachingSlot = remember(state.preferences.protectedTimeSlots, nowMillis) {
                ProtectedSlotClock.approaching(state.preferences.protectedTimeSlots, nowMillis, leadInMinutes = 45).firstOrNull()
            }
            val topTask = state.recommendedTask ?: state.activeTasks.firstOrNull { it.isFrog } ?: state.activeTasks.firstOrNull()
            if (topTask != null || activeTakeoverSlot != null) {
                val currentPhase = state.energyTimeline.minByOrNull {
                    kotlin.math.abs(it.timestampMillis - nowMillis)
                }?.phase ?: EnergyPhase.PEAK
                val microSteps = remember(topTask?.ifThenPlan) {
                    topTask?.ifThenPlan?.let { com.neuroflow.app.domain.model.StepByStepPlanCodec.parse(it) } ?: emptyList()
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FlowSpotlightHeroCard(
                        activeTakeoverSlot = activeTakeoverSlot,
                        approachingSlot = approachingSlot,
                        topTaskId = topTask?.id,
                        topTaskTitle = topTask?.title,
                        topTaskFirstMicroStep = microSteps.firstOrNull()?.text,
                        topTaskMicroSteps = microSteps,
                        topTaskDurationMinutes = topTask?.estimatedDurationMinutes ?: 25,
                        topTaskEnergyLevel = topTask?.energyLevel ?: EnergyLevel.MEDIUM,
                        isTopTaskFrog = topTask?.isFrog ?: false,
                        currentPhase = currentPhase,
                        onStartFocusClick = { taskId -> onStartFocus(taskId) },
                        onCompleteQuickClick = { taskId ->
                            val taskToComplete = state.activeTasks.find { it.id == taskId } ?: topTask
                            if (taskToComplete != null) {
                                if (taskToComplete.isFrog) showFrogConfetti = true
                                viewModel.completeTask(taskToComplete)
                            }
                        }
                    )
                }
            }

            // Fresh Start Recovery Banner (ADHD Safety Net)
            val resetPlan = remember(state.activeTasks) {
                val overdue = state.activeTasks.filter { it.postponeCount >= 2 }
                if (overdue.isNotEmpty()) {
                    FreshStartEngine.buildEmergencyResetPlan(
                        activeTasks = state.activeTasks,
                        nowMillis = System.currentTimeMillis()
                    )
                } else null
            }

            if (!state.zeroGuiltDismissed && (state.showZeroGuiltPrompt || (resetPlan != null && resetPlan.tasksPushedToBacklog.isNotEmpty()))) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FreshStartRecoveryBanner(
                        visible = true,
                        resetPlan = resetPlan,
                        onApplyEmergencyReset = { viewModel.freshStartRollover() },
                        onDismiss = { viewModel.dismissZeroGuiltPrompt() }
                    )
                }
            }

            // Evening Shutdown Ritual Card
            if (state.isEveningShutdownTime && !state.eveningShutdownDismissed) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurface,
                    border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🌙", fontSize = 18.sp)
                            Text(
                                text = "Evening Shutdown Ritual",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        }
                        Text(
                            text = "It's past 7:00 PM. Protect your circadian melatonin surge by closing cognitive loops. You conquered ${state.completedTasks.size} tasks today. Time to wind down.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary
                        )
                        Button(
                            onClick = { viewModel.completeEveningShutdown() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text("Complete Shutdown", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tag Filter Strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.availableTags) { tag ->
                    val isSelected = if (tag == "All") {
                        state.selectedTag == null || state.selectedTag == "All"
                    } else {
                        state.selectedTag == tag
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectTag(if (tag == "All") null else tag) },
                        label = {
                            Text(
                                tag,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            selectedLabelColor = NeuroFlowThemeV2Colors.TextPrimary,
                            containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            labelColor = NeuroFlowThemeV2Colors.TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.heightIn(min = 44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Energy-Mapped Day Stream
            EnergyTimelineStream(
                scheduledTasks = state.filteredTasks,
                onTaskClick = { task -> onStartFocus(task.id) },
                onCompleteTask = { task ->
                    if (task.isFrog) showFrogConfetti = true
                    viewModel.completeTask(task)
                },
                onDeferTask = { task -> viewModel.deferTask(task) },
                onBreakDownTask = { task -> viewModel.breakDownTask(task) },
                onEditTask = { task -> editingTask = task },
                onShiftToPeak = { task -> viewModel.shiftTaskToPeak(task) },
                timeline = state.energyTimeline
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showComposer) {
        SmartTaskComposerSheetV2(
            onDismiss = { showComposer = false },
            mode = SheetMode.Create,
            circadianSummary = state.circadianSummary,
            onSave = { task ->
                showComposer = false
                viewModel.addTask(task)
            },
            completedTasks = state.completedTasks
        )
    }

    editingTask?.let { task ->
        SmartTaskComposerSheetV2(
            onDismiss = { editingTask = null },
            mode = SheetMode.Edit(task),
            circadianSummary = state.circadianSummary,
            onSave = { updated ->
                editingTask = null
                viewModel.updateTask(updated)
            },
            completedTasks = state.completedTasks
        )
    }

    if (showNotesSheet) {
        QuickNotesBottomSheet(
            notes = recentNotes,
            onSaveNote = { title, content -> viewModel.saveQuickNote(content, title) },
            onDismiss = { showNotesSheet = false },
            onOpenFullNotes = onNavigateToNotes,
            onCreateTask = { title, desc -> viewModel.addTaskFromNote(title, desc) },
            onSynthesizeNote = { viewModel.synthesizeQuickNote(it) }
        )
    }

    FrogConfettiOverlay(trigger = showFrogConfetti)
    LaunchedEffect(showFrogConfetti) {
        if (showFrogConfetti) {
            kotlinx.coroutines.delay(2500)
            showFrogConfetti = false
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2flowflowviewmodelv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt`
- **Lines of Code:** 863
- **Size:** 40,245 bytes (39.3 KB)

```kotlin
package com.neuroflow.app.presentation.v2.flow

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.buildNormalizedScheduleTimestamps
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.parseStimulantLogs
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import java.time.Instant
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.repository.EnergyMetricsRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.model.extractStimulantDosesFromSlots
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.QuickNoteSynthesis
import dagger.hilt.android.qualifiers.ApplicationContext
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.PharmacokineticStimulantCalculator
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.domain.service.computeLatestSafeStimulantCutoffMinute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class FlowUiStateV2(
    val activeTasks: List<TaskEntity> = emptyList(),
    val filteredTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val energyUi: EnergyUiModel? = null,
    val circadianSummary: DailyCircadianSummary? = null,
    val dayTimeline: List<CircadianSnapshot> = emptyList(),
    val energyTimeline: List<CircadianSnapshot> = emptyList(),
    val dailyCognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
    val daytimeNaps: List<DaytimeNapBout> = emptyList(),
    val isFallbackSleep: Boolean = false,
    val learnedSampleCount: Double = 0.0,
    val recommendedTask: TaskEntity? = null,
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val overdueTasksCount: Int = 0,
    val showZeroGuiltPrompt: Boolean = false,
    val zeroGuiltDismissed: Boolean = false,
    val selectedTag: String? = null,
    val availableTags: List<String> = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin"),
    val isEveningShutdownTime: Boolean = false,
    val eveningShutdownDismissed: Boolean = false,
    val lastCompletedTask: TaskEntity? = null,
    val lastDeferredTask: Pair<TaskEntity, Long>? = null,
    val lastActionMessage: String? = null,
    val stimulantDoses: List<StimulantDoseBout> = emptyList(),
    val isCrashWindow: Boolean = false,
    val activePlasmaMg: Double = 0.0,
    val socialJetlagLagHours: Double = 0.0,
    val predictedSwsSuppressionPercent: Int = 0,
    val latestSafeStimulantCutoffFormatted: String = "16:11"
)

@HiltViewModel
class FlowViewModelV2 @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val noteRepository: NoteRepository? = null,
    private val sessionRepository: SessionRepository? = null,
    private val circadianEnergyService: CircadianEnergyService = DefaultCircadianEnergyService(),
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null,
    private val taskSchedulerService: TaskSchedulerService = DefaultTaskSchedulerService(circadianEnergyService),
    private val behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService(),
    @ApplicationContext private val context: Context? = null,
    private val geminiAiOnlineService: GeminiAiOnlineService? = null,
    private val energyMetricsRepository: EnergyMetricsRepository? = null
) : ViewModel() {

    private val _dismissedZeroGuilt = MutableStateFlow(false)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _eveningShutdownDismissed = MutableStateFlow(false)
    private val _lastCompletedTask = MutableStateFlow<TaskEntity?>(null)
    private val _lastDeferredTask = MutableStateFlow<Pair<TaskEntity, Long>?>(null)
    private val _lastActionMessage = MutableStateFlow<String?>(null)

    val recentNotes: Flow<List<NoteEntity>> = noteRepository?.allNotes ?: flowOf(emptyList())

    val uiState: StateFlow<FlowUiStateV2> = combine(
        combine(
            taskRepository.observeActiveTasks(),
            taskRepository.observeCompletedTasks(),
            preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new ->
                old.wakeUpHour == new.wakeUpHour &&
                    old.sleepHour == new.sleepHour &&
                    old.sleepPressurePoints == new.sleepPressurePoints &&
                    old.onboardingCompleted == new.onboardingCompleted &&
                    old.tagCatalog == new.tagCatalog &&
                    old.stimulantLogsJson == new.stimulantLogsJson &&
                    old.quickPhysiologicalLogsJson == new.quickPhysiologicalLogsJson &&
                    old.protectedTimeSlots == new.protectedTimeSlots &&
                    old.quizChronotype == new.quizChronotype &&
                    old.manualChronotype == new.manualChronotype &&
                    old.meqScore == new.meqScore &&
                    old.manualPeakProfileEnabled == new.manualPeakProfileEnabled &&
                    old.manualPeakAnchorMinuteOfDay == new.manualPeakAnchorMinuteOfDay
            },
            sleepTelemetryRepository?.observeAllSleepLogs() ?: flowOf(emptyList())
        ) { active, completed, prefs, sleepLogs ->
            Quad(active, completed, prefs, sleepLogs)
        },
        combine(
            _dismissedZeroGuilt,
            _selectedTag,
            _eveningShutdownDismissed,
            _lastCompletedTask
        ) { dismissedGuilt, tag, shutdownDismissed, lastCompleted ->
            Quad(dismissedGuilt, tag, shutdownDismissed, lastCompleted)
        },
        combine(
            _lastDeferredTask,
            _lastActionMessage
        ) { lastDeferred, actionMsg ->
            Pair(lastDeferred, actionMsg)
        }
    ) { (activeTasks, completedTasks, prefs, sleepLogs), (dismissedGuilt, tag, shutdownDismissed, lastCompleted), (lastDeferred, actionMsg) ->
        val now = System.currentTimeMillis()
        val currentHour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        val systemZone = java.time.ZoneId.systemDefault()
        val zonedDateTime = java.time.Instant.ofEpochMilli(now).atZone(systemZone)
        var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(systemZone)
        if (anchor.toInstant().toEpochMilli() > now) {
            anchor = anchor.minusDays(1)
        }
        val baseWakeMillis = anchor.toInstant().toEpochMilli()

        val signal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )

        val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
        val baseNaps = signal?.daytimeNaps ?: emptyList()
        val isFallback = sleepLogs.isEmpty() || (signal != null && signal.consolidatedSessions.isEmpty())

        val rollingAverageSleepMinutes = computeRollingDailySleepAverage(sleepLogs, now, systemZone)

        val dayStart = zonedDateTime.toLocalDate().atStartOfDay(systemZone).toInstant().toEpochMilli()
        val slotBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = zonedDateTime.toLocalDate(),
            zoneId = systemZone
        )

        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis in (dayStart - 8 * 3600_000L)..(dayStart + 26 * 3600_000L) }

        val physBlocks = physLogs.mapNotNull { log ->
            val logZdt = Instant.ofEpochMilli(log.timestampMillis).atZone(systemZone)
            val minOfDay = logZdt.hour * 60 + logZdt.minute
            val duration = log.durationMinutes.coerceIn(5, 240)
            when (log.type) {
                PhysiologicalEventType.MEAL -> CognitiveBlockWindow(
                    startMinuteOfDay = minOfDay,
                    endMinuteOfDay = (minOfDay + duration) % 1440,
                    category = CognitiveBlockCategory.MEAL,
                    intensity = log.intensity,
                    label = "Quick Logged Meal #${log.timestampMillis}",
                    explicitStartMillis = log.timestampMillis,
                    explicitEndMillis = log.timestampMillis + duration * 60_000L
                )
                PhysiologicalEventType.WORKOUT -> CognitiveBlockWindow(
                    startMinuteOfDay = minOfDay,
                    endMinuteOfDay = (minOfDay + duration) % 1440,
                    category = CognitiveBlockCategory.EXERCISE,
                    intensity = log.intensity,
                    label = "Quick Logged Workout #${log.timestampMillis}",
                    explicitStartMillis = log.timestampMillis,
                    explicitEndMillis = log.timestampMillis + duration * 60_000L
                )
                PhysiologicalEventType.NAP -> null
            }
        }

        val physNaps = physLogs.filter { it.type == PhysiologicalEventType.NAP }.map { log ->
            val napDuration = log.durationMinutes.coerceIn(5, 180)
            DaytimeNapBout(
                startMillis = log.timestampMillis,
                endMillis = log.timestampMillis + napDuration * 60_000L
            )
        }

        val dailyBlocks = slotBlocks + physBlocks
        val naps = baseNaps + physNaps

        val loggedStimulants = parseStimulantLogs(prefs.stimulantLogsJson)
        val slotStimulants = extractStimulantDosesFromSlots(
            slots = prefs.protectedTimeSlots,
            date = zonedDateTime.toLocalDate(),
            zoneId = systemZone
        )
        val allStimulants = (loggedStimulants + slotStimulants)
            .filter { it.timestampMillis in (dayStart - 8 * 3600_000L)..(dayStart + 26 * 3600_000L) }
            .distinctBy { "${it.timestampMillis}_${it.type}" }

        val stimEffect = PharmacokineticStimulantCalculator.evaluateAt(now, allStimulants)
        val isCrashWindow = allStimulants.any { bout ->
            val elapsed = (now - bout.timestampMillis) / 3_600_000.0
            elapsed in (bout.type.halfLifeHours)..(bout.type.halfLifeHours + 3.5) && bout.type.crashSeverityFactor >= 0.05
        } || (allStimulants.isNotEmpty() && stimEffect.acuteAlertnessBoostPoints < -1.0)

        // Bedtime Slow-Wave Sleep (SWS) Suppression & Safe Stimulant Cutoff
        val plannedBedtimeZdt = zonedDateTime.toLocalDate()
            .atTime(prefs.sleepHour.coerceIn(0, 23), 0)
            .atZone(systemZone)
            .let { if (it.toInstant().toEpochMilli() <= now) it.plusDays(1) else it }
        val swsSuppressionPercent = (PharmacokineticStimulantCalculator.predictBedtimeSwsSuppression(
            plannedSleepMillis = plannedBedtimeZdt.toInstant().toEpochMilli(),
            doses = allStimulants
        ) * 100.0).roundToInt().coerceIn(0, 35)

        val cutoffMin = computeLatestSafeStimulantCutoffMinute(sleepHour = prefs.sleepHour)
        val cutoffFormatted = String.format(Locale.US, "%02d:%02d", cutoffMin / 60, cutoffMin % 60)

        val sleepWindows = sleepLogs.map { TimeSlotWindow(it.startAt, it.endAt) }
        val socialJetlagReport = SocialJetlagCalculator.evaluateFromSleepWindows(sleepWindows, now, systemZone)

        val behavioralObservations = buildBehavioralObservations(completedTasks = completedTasks, zoneId = systemZone)
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (behavioralObservations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(behavioralObservations, initialPeakHour = initialPeak)
        } else null
        val sampleCount = learnedProfile?.sampleCount ?: 0.0

        val (snapshot, energyUi) = computeEnergyUiState(
            circadianEnergyService = circadianEnergyService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            prefs = prefs,
            sleepLogs = sleepLogs,
            completedTasks = completedTasks,
            nowMillis = now,
            stimulantDoses = allStimulants
        )

        if (prefs.energyTelemetryEnabled && energyMetricsRepository != null) {
            viewModelScope.launch {
                runCatching {
                    energyMetricsRepository.recordEnergyPrediction(
                        predictedAtMillis = now,
                        baselineRawEnergy = snapshot.baselineBiologicalEnergy.toFloat(),
                        peakScore = snapshot.usableEnergy.toFloat(),
                        fatiguePenalty = (snapshot.homeostaticSleepPressure * 100).toFloat(),
                        sleepPressurePoints = prefs.sleepPressurePoints,
                        fatiguePercent = (snapshot.homeostaticSleepPressure * 100).toInt().coerceIn(0, 100),
                        momentAdjustment = (snapshot.physiologicalImpulse + snapshot.learnedAdjustmentPoints).toFloat(),
                        momentConfidence = snapshot.predictionConfidence.toFloat(),
                        momentSupportScore = 50f,
                        momentPressureScore = 50f,
                        adjustedRawEnergy = snapshot.usableEnergy.toFloat(),
                        usableEnergy = snapshot.usableEnergy,
                        chronotype = prefs.quizChronotype ?: prefs.manualChronotype ?: "INTERMEDIATE",
                        wakeUpHour = prefs.wakeUpHour,
                        peakMinuteOfDay = prefs.detectedPeakMinuteOfDay.takeIf { it > 0 } ?: (prefs.peakEnergyStart * 60),
                        peakConfidence = prefs.peakDetectionConfidence.takeIf { it > 0f } ?: 0.85f,
                        activeTaskCount = activeTasks.size
                    )
                }
            }
        }
        val battery = snapshot.usableEnergy
        val currentEnergyZone = when {
            battery >= 65 -> EnergyLevel.HIGH
            battery >= 35 -> EnergyLevel.MEDIUM
            else -> EnergyLevel.LOW
        }

        val overdueTasks = activeTasks.filter { task ->
            task.postponeCount >= 3 ||
                (task.resolveScheduledEpochMillis()?.let { it < now - 2 * 3600_000L } == true) ||
                (task.scheduledDate != null && task.scheduledDate!! < now - 86_400_000L)
        }
        val showZeroGuilt = overdueTasks.size >= 3 && !dismissedGuilt

        val defaultTags = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin")
        val dynamicTags = (prefs.tagCatalog + activeTasks.flatMap { it.tags.split(",") })
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .map { "#$it" }
        val mergedTags = (defaultTags + dynamicTags).distinctBy { it.lowercase() }

        val matchingTasks = activeTasks.filter { it.energyLevel == currentEnergyZone }
        val candidatePool = if (matchingTasks.isNotEmpty()) matchingTasks else activeTasks
        val effectiveSnapshot = snapshot.copy(usableEnergy = battery)
        val recommended = candidatePool.maxByOrNull { task ->
            val schedulable = SchedulableTask(
                id = task.id,
                title = task.title,
                category = when (task.taskType) {
                    TaskType.ANALYTICAL -> TaskCategory.DEEP_WORK
                    TaskType.CREATIVE -> TaskCategory.CREATIVE
                    TaskType.ADMIN -> TaskCategory.ADMIN
                    TaskType.PHYSICAL -> TaskCategory.EXERCISE
                    TaskType.GENERAL -> TaskCategory.GENERAL
                    TaskType.MINDFULNESS -> TaskCategory.MINDFULNESS
                },
                priority = when (task.priority) {
                    Priority.HIGH -> 2
                    Priority.MEDIUM -> 1
                    else -> 0
                },
                estimatedMinutes = task.estimatedDurationMinutes,
                energyDemand = when (task.energyLevel) {
                    EnergyLevel.HIGH -> EnergyDemand.HIGH
                    EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                    else -> EnergyDemand.LOW
                },
                isFrog = task.isFrog,
                scheduledStartMillis = task.resolveScheduledEpochMillis(),
                deadlineMillis = task.resolveDeadlineEpochMillis(),
                isLocked = task.isScheduleLocked,
                parentTaskId = task.parentTaskId,
                prerequisiteIds = task.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet(),
                impactScore = task.impactScore,
                estimatedEnjoyment = task.enjoymentScore / 10,
                postponeCount = task.postponeCount,
                distractionScore = task.distractionScore
            )
            val baseLeverage = (if (task.isFrog) 1000 else 0) + task.impactScore
            baseLeverage + taskSchedulerService.evaluateTaskScore(schedulable, effectiveSnapshot).totalScore
        }

        val filtered = if (tag.isNullOrBlank() || tag.equals("All", ignoreCase = true)) {
            activeTasks
        } else {
            val query = tag.removePrefix("#").trim()
            activeTasks.filter { task ->
                task.tags.contains(query, ignoreCase = true) ||
                    task.title.contains(query, ignoreCase = true)
            }
        }

        val isEvening = currentHour >= 19

        val manualPeakHour = if (prefs.manualPeakProfileEnabled) (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
        val chronotype = if (!prefs.quizChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.quizChronotype)
        } else if (!prefs.manualChronotype.isNullOrBlank()) {
            ChronotypeProfile.fromString(prefs.manualChronotype)
        } else if (prefs.meqScore > 0) {
            when {
                prefs.meqScore >= 70 -> ChronotypeProfile.DEFINITE_MORNING
                prefs.meqScore >= 59 -> ChronotypeProfile.MODERATE_MORNING
                prefs.meqScore >= 42 -> ChronotypeProfile.INTERMEDIATE
                prefs.meqScore >= 31 -> ChronotypeProfile.MODERATE_EVENING
                else -> ChronotypeProfile.DEFINITE_EVENING
            }
        } else {
            ChronotypeProfile.INTERMEDIATE
        }

        val circadianSummary = runCatching {
            circadianEnergyService.computeDailySummary(
                timestampMillis = now,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = systemZone,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakHour,
                cognitiveBlocks = dailyBlocks,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                stimulantDoses = allStimulants,
                socialJetlagLagHours = socialJetlagReport.mondayMorningPhaseLagHours
            )
        }.getOrNull()

        val dayTimeline: List<CircadianSnapshot> = runCatching {
            circadianEnergyService.projectDayTimeline(
                dayStartMillis = dayStart,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                stepMinutes = 15,
                zoneId = systemZone,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakHour,
                cognitiveBlocks = dailyBlocks,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                stimulantDoses = allStimulants,
                socialJetlagLagHours = socialJetlagReport.mondayMorningPhaseLagHours
            )
        }.getOrElse { emptyList() }

        FlowUiStateV2(
            activeTasks = activeTasks,
            filteredTasks = filtered,
            completedTasks = completedTasks,
            energyUi = energyUi,
            circadianSummary = circadianSummary,
            dayTimeline = dayTimeline,
            energyTimeline = dayTimeline,
            dailyCognitiveBlocks = dailyBlocks,
            daytimeNaps = naps,
            isFallbackSleep = isFallback,
            learnedSampleCount = sampleCount,
            recommendedTask = recommended,
            preferences = prefs,
            isLoading = false,
            overdueTasksCount = overdueTasks.size,
            showZeroGuiltPrompt = showZeroGuilt,
            zeroGuiltDismissed = dismissedGuilt,
            selectedTag = tag,
            availableTags = mergedTags,
            isEveningShutdownTime = isEvening,
            eveningShutdownDismissed = shutdownDismissed,
            lastCompletedTask = lastCompleted,
            lastDeferredTask = lastDeferred,
            lastActionMessage = actionMsg,
            stimulantDoses = allStimulants,
            isCrashWindow = isCrashWindow,
            activePlasmaMg = stimEffect.activeEquivalentMg,
            socialJetlagLagHours = socialJetlagReport.socialJetlagHours,
            predictedSwsSuppressionPercent = swsSuppressionPercent,
            latestSafeStimulantCutoffFormatted = cutoffFormatted
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FlowUiStateV2(isLoading = true)
    )

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insert(task)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, task) }
        }
    }

    fun logStimulant(type: StimulantType) {
        viewModelScope.launch {
            preferencesDataStore.recordStimulantDose(type)
            val label = when (type) {
                StimulantType.CAFFEINE -> "Caffeine ☕"
                StimulantType.GREEN_TEA_L_THEANINE -> "Green Tea 🍵"
                StimulantType.PRESCRIPTION_STIMULANT_IR -> "Meds IR 💊"
                StimulantType.PRESCRIPTION_STIMULANT_XR -> "Meds XR 💊"
            }
            _lastActionMessage.value = "Stimulant logged: $label — Energy curve updated"
        }
    }

    fun undoLogStimulant() {
        viewModelScope.launch {
            val undone = preferencesDataStore.undoLastStimulantDose()
            if (undone) {
                _lastActionMessage.value = "Undone: Removed last stimulant dose ↩️"
            }
        }
    }

    fun clearAllStimulants() {
        viewModelScope.launch {
            preferencesDataStore.clearStimulantLogs()
            _lastActionMessage.value = "Cleared all logged stimulants 🧹"
        }
    }

    fun removeStimulantDose(timestampMillis: Long, type: StimulantType? = null) {
        viewModelScope.launch {
            val typeStr = type?.name
            val removed = preferencesDataStore.removeStimulantDose(timestampMillis, typeStr)
            if (removed) {
                _lastActionMessage.value = "Removed stimulant dose 🗑️"
            }
        }
    }

    fun logPhysiologicalEvent(type: PhysiologicalEventType) {
        viewModelScope.launch {
            preferencesDataStore.recordPhysiologicalEvent(type)
            val label = when (type) {
                PhysiologicalEventType.MEAL -> "Meal Dip 🍽️ (-12.0 pt nadir)"
                PhysiologicalEventType.WORKOUT -> "Workout 🏃 (+10 pt endorphin window)"
                PhysiologicalEventType.NAP -> "20m Power Nap 💤 (sleep pressure cleared)"
            }
            _lastActionMessage.value = "Bio-event logged: $label — Curve updated"
        }
    }

    fun undoLogPhysiologicalEvent() {
        viewModelScope.launch {
            val undone = preferencesDataStore.undoLastPhysiologicalEvent()
            if (undone) {
                _lastActionMessage.value = "Undone: Removed last physiological event ↩️"
            }
        }
    }

    fun clearAllPhysiologicalEvents() {
        viewModelScope.launch {
            preferencesDataStore.clearPhysiologicalLogs()
            _lastActionMessage.value = "Cleared all physiological events 🧹"
        }
    }

    fun addTaskFromNote(title: String, content: String) {
        viewModelScope.launch {
            val fullText = if (title.isNotBlank()) "$title: $content" else content
            val inferred = TaskInferenceEngine.inferFromInput(fullText)
            val now = System.currentTimeMillis()
            val newTask = TaskEntity(
                title = inferred.sanitizedTitle.ifBlank { title.ifBlank { "Quick Task" } },
                description = content,
                estimatedDurationMinutes = inferred.durationMinutes,
                priority = inferred.priority,
                energyLevel = inferred.energyDemand,
                taskType = inferred.taskType,
                quadrant = inferred.quadrant,
                tags = inferred.contextTags.joinToString(","),
                scheduledDate = inferred.scheduledDate ?: now,
                scheduledTime = inferred.scheduledTime,
                deadlineDate = inferred.deadlineDate,
                deadlineTime = inferred.deadlineTime,
                isFrog = inferred.isFrog,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insert(newTask)
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, newTask) }
            _lastActionMessage.value = "Created task from note: ${newTask.title.take(24)} ⚡"
        }
    }

    fun shiftTaskToPeak(task: TaskEntity) {
        viewModelScope.launch {
            val peakHour = uiState.value.circadianSummary?.peakWindowStartHour
                ?: uiState.value.preferences.peakEnergyStart.takeIf { it > 0 } ?: 10
            val peakEndHour = uiState.value.circadianSummary?.peakWindowEndHour ?: (peakHour + 3)
            val dipEndHour = uiState.value.circadianSummary?.dipWindowEndHour ?: 16
            val usableEnergy = uiState.value.circadianSummary?.currentSnapshot?.usableEnergy ?: 60

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)

            val baseDate = task.scheduledDate ?: now
            val baseCal = Calendar.getInstance().apply {
                timeInMillis = baseDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val isToday = baseCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                baseCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

            val targetDate: Long
            val targetHour: Int
            val messageLabel: String

            if (!isToday || currentHour < peakEndHour) {
                targetDate = baseCal.timeInMillis
                targetHour = peakHour
                messageLabel = "Peak Window (${peakHour}:00)"
            } else if (currentHour < 18 && usableEnergy >= 55) {
                targetDate = baseCal.timeInMillis
                targetHour = maxOf(currentHour + 1, dipEndHour).coerceAtMost(17)
                messageLabel = "Afternoon Rebound (${targetHour}:00)"
            } else {
                baseCal.add(Calendar.DAY_OF_YEAR, 1)
                targetDate = baseCal.timeInMillis
                targetHour = peakHour
                messageLabel = "Tomorrow's Peak (${peakHour}:00)"
            }

            val millisInDay = targetHour * 3600_000L
            val updated = task.copy(
                scheduledDate = targetDate,
                scheduledTime = millisInDay,
                isScheduleLocked = true,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.update(updated)
            _lastActionMessage.value = "Shifted '${task.title.take(20)}' to $messageLabel ⚡"
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.update(task)
        }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            _lastCompletedTask.value = task
            val prefs = preferencesDataStore.preferencesFlow.first()
            val affirmationSuffix = if (prefs.affirmations.isNotEmpty()) {
                " • \"${prefs.affirmations.random()}\""
            } else ""
            _lastActionMessage.value = "Completed: ${task.title}$affirmationSuffix"
            context?.let { AutonomyNudgeEngine.cancelNudge(it, task.id) }
            taskRepository.completeAndRecur(task, System.currentTimeMillis())
        }
    }

    fun undoCompleteTask() {
        val task = _lastCompletedTask.value ?: return
        viewModelScope.launch {
            taskRepository.update(
                task.copy(
                    status = TaskStatus.ACTIVE,
                    completedAt = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastCompletedTask.value = null
            _lastActionMessage.value = "Restored: ${task.title}"
        }
    }

    fun deferTask(task: TaskEntity, hours: Int = 2) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val currentEpoch = task.resolveScheduledEpochMillis(now) ?: now
            _lastDeferredTask.value = Pair(task, task.scheduledTime ?: 0L)
            val newEpoch = currentEpoch + hours * 3600_000L
            val (newDate, newTime) = buildNormalizedScheduleTimestamps(newEpoch)
            taskRepository.update(
                task.copy(
                    scheduledDate = newDate,
                    scheduledTime = newTime,
                    postponeCount = task.postponeCount + 1,
                    updatedAt = now
                )
            )
            context?.let { AutonomyNudgeEngine.scheduleNudge(it, task) }
            _lastActionMessage.value = "Deferred by ${hours}h: ${task.title}"
        }
    }

    fun undoDeferTask() {
        val pair = _lastDeferredTask.value ?: return
        val (originalTask, _) = pair
        viewModelScope.launch {
            taskRepository.update(
                originalTask.copy(
                    scheduledDate = originalTask.scheduledDate,
                    scheduledTime = originalTask.scheduledTime,
                    postponeCount = (originalTask.postponeCount - 1).coerceAtLeast(0),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastDeferredTask.value = null
            _lastActionMessage.value = "Undid deferral for: ${originalTask.title}"
        }
    }

    fun clearActionMessage() {
        _lastActionMessage.value = null
    }

    fun freshStartRollover() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val overdue = uiState.value.activeTasks.filter { task ->
                task.postponeCount >= 3 ||
                    (task.resolveScheduledEpochMillis()?.let { it < now - 2 * 3600_000L } == true) ||
                    (task.scheduledDate != null && task.scheduledDate!! < now - 86_400_000L)
            }
            overdue.forEach { task ->
                taskRepository.update(
                    task.copy(
                        quadrant = Quadrant.ELIMINATE,
                        scheduledTime = null,
                        scheduledDate = null,
                        postponeCount = 0,
                        updatedAt = now
                    )
                )
            }
            _dismissedZeroGuilt.value = true
            _lastActionMessage.value = "Rolled ${overdue.size} tasks into Icebox. Fresh start active! 🌱"
        }
    }

    fun dismissZeroGuiltPrompt() {
        _dismissedZeroGuilt.value = true
    }

    fun breakDownTask(task: TaskEntity) {
        viewModelScope.launch {
            val cleanTitle = task.title.trim().take(30)
            val prefs = preferencesDataStore.preferencesFlow.first()
            val generated = behavioralSupportService.generateMicroStepsWithAi(
                taskTitle = task.title,
                estimatedMinutes = task.estimatedDurationMinutes,
                apiKey = prefs.geminiApiKey,
                useAi = prefs.geminiAiEnabled
            )
            val microStepEntities = generated.mapIndexed { index, suggestion ->
                TaskMicroStepEntity(
                    taskId = task.id,
                    title = suggestion.title,
                    sortOrder = index,
                    estimatedMinutes = suggestion.estimatedMinutes
                )
            }
            taskRepository.deleteMicroStepsByTaskId(task.id)
            taskRepository.insertMicroSteps(microStepEntities)

            val planSteps = generated.map { StepByStepPlanStep(it.title, false) }
            val serialized = StepByStepPlanCodec.serialize(planSteps)
            taskRepository.update(
                task.copy(
                    ifThenPlan = serialized,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastActionMessage.value = "Decomposed '${cleanTitle}' into ${generated.size} micro-steps 🪄"
        }
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun completeEveningShutdown() {
        _eveningShutdownDismissed.value = true
        _lastActionMessage.value = "Evening shutdown complete. Time to rest and restore 🌙"
    }

    fun saveQuickNote(content: String, title: String = "") {
        if (content.isBlank() || noteRepository == null) return
        viewModelScope.launch {
            noteRepository.saveNote(
                NoteEntity(
                    title = title.ifBlank { "Quick Thought" },
                    content = content.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _lastActionMessage.value = "Note saved to scratchpad 📝"
        }
    }

    suspend fun synthesizeQuickNote(content: String): QuickNoteSynthesis {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return geminiAiOnlineService?.synthesizeQuickNote(
            rawContent = content,
            userDirective = prefs.adaptiveSystemDirective,
            apiKey = prefs.geminiApiKey,
            useAi = prefs.geminiAiEnabled
        ) ?: run {
            val inferred = TaskInferenceEngine.inferFromInput(content)
            QuickNoteSynthesis(
                extractedTaskTitle = inferred.sanitizedTitle.ifBlank { content.take(35) },
                suggestedCategory = inferred.taskType.name,
                estimatedMinutes = inferred.durationMinutes,
                tags = inferred.contextTags,
                firstActionStep = "Review scratchpad note and outline details"
            )
        }
    }

    suspend fun enrichTaskWithAi(taskTitle: String, durationMinutes: Int = 30): com.neuroflow.app.domain.service.TaskEnrichmentResult? {
        val prefs = preferencesDataStore.preferencesFlow.first()
        return geminiAiOnlineService?.enrichTaskWithAi(
            title = taskTitle,
            durationMinutes = durationMinutes,
            userDirective = prefs.adaptiveSystemDirective,
            apiKey = prefs.geminiApiKey,
            useAi = prefs.geminiAiEnabled
        )
    }

    fun logPastTime(task: TaskEntity, minutes: Float) {
        if (minutes.isNaN() || minutes.isInfinite()) return
        val safeMinutes = minutes.coerceAtMost(1440f)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TimeSessionEntity(
                taskId = task.id,
                startedAt = now - (safeMinutes * 60_000f).toLong(),
                endedAt = now,
                durationMinutes = safeMinutes,
                sessionType = "MANUAL_LOG"
            )
            sessionRepository?.insert(session)
            val newTracked = task.totalTimeTrackedMinutes + safeMinutes
            val newCount = task.sessionCount + 1
            taskRepository.update(
                task.copy(
                    totalTimeTrackedMinutes = newTracked,
                    sessionCount = newCount,
                    lastSessionDurationMinutes = safeMinutes,
                    actualDurationMinutes = newTracked,
                    updatedAt = now
                )
            )
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalFocusMinutes = prefs.totalFocusMinutes + safeMinutes.toInt())
            }
            _lastActionMessage.value = "Logged ${safeMinutes.toInt()}m past time for ${task.title}"
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
```

---

