package com.neuroflow.app.presentation.v2.flow

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.CircadianEnergySummary
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.v2.theme.NeuroFlowExtendedColors
import com.neuroflow.app.presentation.v2.theme.phaseAccentColor
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 24-Hour Interactive Circadian Energy Hero Barometer (ADR-008).
 * Renders cubic-bezier curve of usable biological energy with touch-scrubbing,
 * tactile haptics, and ADHD-tailored behavioral guidance.
 */
@Composable
fun CircadianHeroBarometer(
    summary: CircadianEnergySummary,
    dayTimeline: List<CircadianSnapshot>,
    modifier: Modifier = Modifier,
    cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
    naps: List<DaytimeNapBout> = emptyList(),
    isFallbackSleep: Boolean = false,
    learnedSampleCount: Double = 0.0,
) {
    val colors = com.neuroflow.app.presentation.v2.theme.LocalNeuroFlowColors.current

    val activeSnapshot = dayTimeline.minByOrNull {
        kotlin.math.abs(it.timestampMillis - System.currentTimeMillis())
    }

    val activeScore = activeSnapshot?.usableEnergy ?: summary.currentEnergyScore
    val activePhase = activeSnapshot?.phase ?: summary.currentPhase
    val accent = phaseAccentColor(activePhase, colors)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = colors.surfaceElevated,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            // Header: Current/Scrubbed Score + Phase Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$activeScore",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent,
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(bottom = 3.dp, start = 2.dp),
                        )
                    }
                    Text(
                        text = "Current Biological Energy",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }

                PhaseBadge(phase = activePhase, accent = accent)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dayTimeline.size >= 2) {
                EnhancedCircadianEnergyGraph(
                    timeline = dayTimeline,
                    currentMillis = System.currentTimeMillis(),
                    cognitiveBlocks = cognitiveBlocks,
                    naps = naps,
                    isFallbackSleep = isFallbackSleep,
                    learnedSampleCount = learnedSampleCount,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Behavioral Recommendation Footer
            PhaseGuidanceRow(
                phase = activePhase,
                cognitiveBudget = summary.cognitiveBudgetMinutesRemaining,
                peakStart = summary.peakStart,
                peakEnd = summary.peakEnd,
                dipStart = summary.dipStart,
                dipEnd = summary.dipEnd,
                colors = colors,
            )
        }
    }
}

/**
 * Backward compatibility overload for CircadianHeroBarometer.
 */
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
        learnedSampleCount = learnedSampleCount
    )
}

@Composable
fun PhaseBadge(phase: EnergyPhase, accent: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                fontWeight = FontWeight.Bold,
                color = accent,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceSubtle, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = when (phase) {
                EnergyPhase.PEAK -> "⚡"
                EnergyPhase.REBOUND -> "📈"
                EnergyPhase.DIP -> "☕"
                EnergyPhase.WIND_DOWN -> "🌙"
                EnergyPhase.RECOVERY_NEEDED -> "🧘"
            },
            fontSize = 16.sp,
        )
        Text(
            text = guidance,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
            lineHeight = 16.sp,
        )
        Text(
            text = "${cognitiveBudget}m left",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
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
        EnergyPhase.REBOUND -> "Secondary alertness peak. Great for collaborative work and execution."
        EnergyPhase.DIP -> "Biological low (${dipStart.format(fmt)}-${dipEnd.format(fmt)}). Switch to administrative tasks or rest."
        EnergyPhase.RECOVERY_NEEDED -> "Synaptic fatigue detected. Take a 15-min NSDR or walk before resuming."
        EnergyPhase.WIND_DOWN -> "Melatonin rising. Avoid high-stress tasks; transition to shutdown mode."
    }
}
