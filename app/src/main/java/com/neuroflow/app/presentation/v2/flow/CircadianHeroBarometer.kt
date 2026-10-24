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
import androidx.compose.ui.text.style.TextOverflow
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
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(emoji, fontSize = 16.sp)
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = "$typeName (~${dose.doseMg.roundToInt()}mg)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeuroFlowPaletteV2.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Logged at $timeStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeuroFlowPaletteV2.TextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
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

@OptIn(ExperimentalLayoutApi::class)
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

        // Active Plasma Status, SWS Suppression & Safe Cutoff Bar
        if (activePlasmaMg > 0.05 || stimulantDoses.isNotEmpty() || latestSafeStimulantCutoffFormatted.isNotBlank()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status & Risk Badges
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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

                // Dose Management Actions (Guaranteed visible, wrapped if needed, with high-contrast surfaces)
                if (activePlasmaMg > 0.05 || stimulantDoses.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (stimulantDoses.isNotEmpty()) {
                            Surface(
                                onClick = { showDoseHistoryDialog = true },
                                shape = RoundedCornerShape(6.dp),
                                color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("📋", fontSize = 10.sp)
                                    Text(
                                        text = "Doses (${stimulantDoses.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowPaletteV2.OptimalEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Surface(
                            onClick = onClearStimulants,
                            shape = RoundedCornerShape(6.dp),
                            color = NeuroFlowPaletteV2.SurfaceElevated,
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🧹", fontSize = 10.sp)
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuroFlowPaletteV2.TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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

            // Quick Dose History Chip
            if (stimulantDoses.isNotEmpty()) {
                Surface(
                    onClick = { showDoseHistoryDialog = true },
                    shape = RoundedCornerShape(50),
                    color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📋", fontSize = 12.sp)
                        Text(
                            text = "Doses (${stimulantDoses.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.OptimalEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Quick Clear Doses Chip
            if (stimulantDoses.isNotEmpty() || activePlasmaMg > 0.05) {
                Surface(
                    onClick = onClearStimulants,
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
                            text = "Clear Doses",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
