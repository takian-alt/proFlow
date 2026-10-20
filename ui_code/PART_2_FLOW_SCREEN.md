# neuroFlow UI Codebase — Flow Execution Cockpit & Circadian Barometer

**Description:** FlowScreenV2, FlowViewModelV2, EnhancedCircadianEnergyGraph, CircadianHeroBarometer, and EnergyTimelineStream.  
**Generated for Vice Head Review:** 2026-10-20 16:59:40  
**Files:** 6  
**Lines of Code:** 2,821  
**Uncompressed Size:** 116.9 KB  

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
- **Lines of Code:** 313
- **Size:** 11,614 bytes (11.3 KB)

```kotlin
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
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2flowenergytimelinestreamkt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt`
- **Lines of Code:** 521
- **Size:** 22,137 bytes (21.6 KB)

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.presentation.common.ManualTimeLogSheet
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

import com.neuroflow.app.domain.service.CircadianSnapshot

@Composable
fun EnergyTimelineStream(
    scheduledTasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onCompleteTask: (TaskEntity) -> Unit,
    onDeferTask: (TaskEntity) -> Unit = {},
    onBreakDownTask: (TaskEntity) -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {},
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
            scheduledTasks.forEach { task ->
                key(task.id) {
                    TimelineTaskCard(
                        task = task,
                        onTaskClick = { onTaskClick(task) },
                        onComplete = { onCompleteTask(task) },
                        onDefer = { onDeferTask(task) },
                        onBreakDown = { onBreakDownTask(task) },
                        onEdit = { onEditTask(task) },
                        onLogPastTime = { taskToLogTime = task },
                        timeline = timeline
                    )
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
                    }

                    val scheduledMillis = task.scheduledTime ?: task.scheduledDate
                    if (scheduledMillis != null && timeline.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TaskSlotEnergyFitBadge(
                            taskScheduledMillis = scheduledMillis,
                            taskEnergyDemand = task.energyLevel,
                            timeline = timeline
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
- **Lines of Code:** 653
- **Size:** 27,317 bytes (26.7 KB)

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    modifier: Modifier = Modifier
) {
    if (timeline.size < 2) return
    val haptics = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }
    var lastHapticPhase by remember { mutableStateOf<EnergyPhase?>(null) }

    val startMillis = timeline.first().timestampMillis
    val endMillis = timeline.last().timestampMillis
    val totalSpanMillis = (endMillis - startMillis).coerceAtLeast(1L)

    val activeSnapshot = scrubbedIndex?.let { timeline.getOrNull(it) }
        ?: timeline.minByOrNull { abs(it.timestampMillis - currentMillis) }
        ?: timeline.first()

    val PeakGold = Color(0xFFFFB300)
    val ReboundCyan = Color(0xFF00E5FF)
    val DipIndigo = Color(0xFF7C4DFF)
    val RecoveryRose = Color(0xFFFF5252)
    val SurfaceDark = Color(0xFF12131A)
    val GridLineColor = Color(0xFF262938)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // 1. Header & Calibration Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (scrubbedIndex != null) "INSPECTING TIMELINE" else "BIOLOGICAL & LOAD PACING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${activeSnapshot.usableEnergy}%",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = phaseColor(activeSnapshot.phase)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = phaseLabel(activeSnapshot.phase),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            // Fast-Learner Calibration Pill
            val calibrationText = when {
                isFallbackSleep -> "Est. Wake • Tap to Log"
                learnedSampleCount >= 3.0 -> "AI Calibrated (${(activeSnapshot.predictionConfidence * 100).roundToInt()}%)"
                learnedSampleCount > 0.0 -> "Learning Rhythm (${learnedSampleCount.roundToInt()}/3)"
                else -> "Baseline Chronotype"
            }
            Surface(
                color = if (isFallbackSleep) RecoveryRose.copy(alpha = 0.16f) else ReboundCyan.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    if (isFallbackSleep) RecoveryRose.copy(alpha = 0.4f) else ReboundCyan.copy(alpha = 0.35f)
                )
            ) {
                Text(
                    text = calibrationText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isFallbackSleep) RecoveryRose else ReboundCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Interactive Factor Decomposition Callout
        AnimatedVisibility(
            visible = scrubbedIndex != null || activeSnapshot.focusDepletionPenalty > 0.01 || abs(activeSnapshot.learnedAdjustmentPoints) >= 1.5,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FactorDecompositionStrip(
                snapshot = activeSnapshot,
                zoneId = zoneId
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Interactive Dual-Curve Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
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
                val padTop = 16.dp.toPx()
                val padBottom = 26.dp.toPx()
                val chartHeight = (size.height - padTop - padBottom).coerceAtLeast(1f)
                val chartWidth = size.width

                fun timeToX(millis: Long): Float =
                    (((millis - startMillis).toFloat() / totalSpanMillis.toFloat()) * chartWidth).coerceIn(0f, chartWidth)

                fun energyToY(score: Float): Float =
                    padTop + chartHeight * (1f - (score.coerceIn(0f, 100f) / 100f))

                // A. Horizontal Reference Grid (25%, 50%, 75%)
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

                // B. Peak & Dip Phase Background Bands
                drawPhaseBands(timeline, chartWidth, padTop, chartHeight, PeakGold, DipIndigo)

                // C. Scheduled Cognitive Task & Nap Blocks on Bottom Track
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
                    val x2 = timeToX(bEndMillis).coerceAtLeast(x1 + 4.dp.toPx())
                    val blockEnergy = timeline.minByOrNull { abs(it.timestampMillis - bStartMillis) }?.usableEnergy ?: 60
                    val isMismatch = block.demand == EnergyDemand.HIGH && blockEnergy < 50
                    val blockColor = when {
                        isMismatch -> RecoveryRose
                        block.isFrog -> PeakGold
                        block.demand == EnergyDemand.HIGH -> ReboundCyan
                        else -> DipIndigo
                    }
                    drawRoundRect(
                        color = blockColor.copy(alpha = if (block.isCompleted) 0.45f else 0.85f),
                        topLeft = Offset(x1, blockTrackY),
                        size = Size(x2 - x1, 5.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }

                naps.forEach { nap ->
                    val x1 = timeToX(nap.startMillis)
                    val x2 = timeToX(nap.endMillis).coerceAtLeast(x1 + 4.dp.toPx())
                    drawRoundRect(
                        color = Color(0xFF00E676).copy(alpha = 0.75f),
                        topLeft = Offset(x1, blockTrackY),
                        size = Size(x2 - x1, 5.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }

                // Build coordinates for Monotone Cubic Hermite Splines
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

                // D. Uncertainty Confidence Ribbon around Usable Energy
                val ribbonPath = Path().apply {
                    val upperPoints = timeline.mapIndexed { i, snap ->
                        val spread = (1.0 - snap.predictionConfidence).toFloat() * 16f
                        Offset(usablePoints[i].x, energyToY((snap.usableEnergy + spread).coerceAtMost(100f)))
                    }
                    val lowerPoints = timeline.mapIndexed { i, snap ->
                        val spread = (1.0 - snap.predictionConfidence).toFloat() * 16f
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
                    color = ReboundCyan.copy(alpha = 0.08f)
                )

                // E. Ghosted Biological Potential Curve (Dashed)
                val baselinePath = buildMonotoneCubicPath(baselinePoints)
                drawPath(
                    path = baselinePath,
                    color = Color.White.copy(alpha = 0.28f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                )

                // F. Primary Load-Adjusted Usable Energy Curve & Area Gradient
                val usablePath = buildMonotoneCubicPath(usablePoints)
                val fillPath = Path().apply {
                    addPath(usablePath)
                    lineTo(chartWidth, padTop + chartHeight)
                    lineTo(0f, padTop + chartHeight)
                    close()
                }
                val horizontalBrush = Brush.horizontalGradient(
                    colors = timeline.map { phaseColor(it.phase) }
                )
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            phaseColor(activeSnapshot.phase).copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        startY = padTop,
                        endY = padTop + chartHeight
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

                // G. Current Time ("NOW") or Active Scrubber Indicator
                val targetMillis = activeSnapshot.timestampMillis
                val markerX = timeToX(targetMillis)
                val markerY = energyToY(activeSnapshot.usableEnergy.toFloat())

                drawLine(
                    color = Color.White.copy(alpha = if (scrubbedIndex != null) 0.55f else 0.28f),
                    start = Offset(markerX, padTop),
                    end = Offset(markerX, padTop + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = if (scrubbedIndex == null) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null
                )

                drawCircle(
                    color = phaseColor(activeSnapshot.phase).copy(alpha = 0.3f),
                    radius = 9.dp.toPx(),
                    center = Offset(markerX, markerY)
                )
                drawCircle(
                    color = phaseColor(activeSnapshot.phase),
                    radius = 5.dp.toPx(),
                    center = Offset(markerX, markerY)
                )
                drawCircle(
                    color = SurfaceDark,
                    radius = 2.dp.toPx(),
                    center = Offset(markerX, markerY)
                )

                // H. X-Axis Hour Labels (Every 4 hours)
                drawHourAxisLabels(
                    startMillis = startMillis,
                    totalSpanMillis = totalSpanMillis,
                    chartWidth = chartWidth,
                    labelY = padTop + chartHeight + 6.dp.toPx(),
                    zoneId = zoneId,
                    textMeasurer = textMeasurer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Compact Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = ReboundCyan, label = "Usable Energy")
            LegendItem(color = Color.White.copy(alpha = 0.45f), label = "Bio Potential (Unloaded)", isDashed = true)
            LegendItem(color = PeakGold, label = "Frog / Deep Block")
        }
    }
}

@Composable
private fun FactorDecompositionStrip(
    snapshot: CircadianSnapshot,
    zoneId: ZoneId
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
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        FactorChip(label = "Circadian", value = "+$cPoints", color = Color(0xFF00E5FF))
        FactorChip(label = "Sleep Debt", value = "$sDebtPenalty", color = Color(0xFFFF8A65))
        if (focusPenalty < 0) {
            FactorChip(label = "Focus Load", value = "$focusPenalty", color = Color(0xFFFF5252))
        }
        if (learnedAdj != 0) {
            val signPrefix = if (learnedAdj > 0) "+" else ""
            FactorChip(label = "Learned", value = "$signPrefix$learnedAdj", color = Color(0xFFB388FF))
        }
    }
}

@Composable
private fun FactorChip(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isDashed: Boolean = false) {
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
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

/**
 * Builds a Monotone Cubic Hermite Spline (Fritsch-Carlson method) so the energy curve
 * never overshoots [0..100] around sharp transitions like morning wake or power naps.
 */
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
                    color = peakColor.copy(alpha = 0.045f),
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
    textMeasurer: TextMeasurer
) {
    val labelStyle = TextStyle(
        color = Color.White.copy(alpha = 0.45f),
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

private fun phaseColor(phase: EnergyPhase): Color = when (phase) {
    EnergyPhase.PEAK -> Color(0xFFFFB300)
    EnergyPhase.REBOUND -> Color(0xFF00E5FF)
    EnergyPhase.DIP -> Color(0xFF7C4DFF)
    EnergyPhase.RECOVERY_NEEDED -> Color(0xFFFF5252)
    EnergyPhase.WIND_DOWN -> Color(0xFF5C6BC0)
}

private fun phaseLabel(phase: EnergyPhase): String = when (phase) {
    EnergyPhase.PEAK -> "Peak Cognitive Zone"
    EnergyPhase.REBOUND -> "Associative Rebound"
    EnergyPhase.DIP -> "Circadian Afternoon Dip"
    EnergyPhase.RECOVERY_NEEDED -> "Synaptic Recovery Needed"
    EnergyPhase.WIND_DOWN -> "Melatonin Wind-Down"
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2flowflowhubupgradesv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowHubUpgradesV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowHubUpgradesV2.kt`
- **Lines of Code:** 425
- **Size:** 18,632 bytes (18.2 KB)

```kotlin
package com.neuroflow.app.presentation.v2.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2

/**
 * Spotlight Hero Card for FlowScreenV2:
 * Automatically switches between a running ProtectedTimeSlot takeover (Meal, Exercise, Mindfulness)
 * and the highest-affinity Next Action / Daily Frog task so the user never faces choice paralysis.
 */
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
    modifier: Modifier = Modifier
) {
    val phaseAccent = NeuroFlowPaletteV2.accentForPhase(currentPhase)
    if (activeTakeoverSlot != null) {
        Surface(
            color = NeuroFlowPaletteV2.SurfaceCard,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.45f)),
            modifier = modifier.fillMaxWidth()
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
                    .padding(18.dp)
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
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activeTakeoverSlot.slot.label,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deep work is paused to protect your ${activeTakeoverSlot.slot.category.name.lowercase()} window and discharge synaptic fatigue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }
        }
        return
    }

    if (topTaskId == null || topTaskTitle.isNullOrBlank()) return

    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, phaseAccent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(phaseAccent.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isTopTaskFrog) "🐸 DAILY FROG SPOTLIGHT" else "⚡ OPTIMAL NEXT ACTION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = phaseAccent,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${topTaskDurationMinutes}m · ${topTaskEnergyLevel.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = topTaskTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 2-Minute Activation Doorway preview
            if (!topTaskFirstMicroStep.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2m Starter:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeuroFlowPaletteV2.ReboundCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = topTaskFirstMicroStep,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onStartFocusClick(topTaskId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = phaseAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
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
                    color = Color.White.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier
                        .height(46.dp)
                        .clickable { onCompleteQuickClick(topTaskId) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
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
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * One-Tap Guilt-Free Schedule Recovery Banner powered by FreshStartEngine.buildEmergencyResetPlan()
 */
@Composable
fun FreshStartRecoveryBanner(
    visible: Boolean,
    resetPlan: FreshStartEngine.EmergencyResetPlan?,
    onApplyEmergencyReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && resetPlan != null && resetPlan.tasksPushedToBacklog.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val plan = resetPlan ?: return@AnimatedVisibility
        Surface(
            color = NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.14f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                            color = Color.White
                        )
                        Text(
                            text = "Keep ${plan.tasksToReschedule.size} essential task(s) today, move ${plan.tasksPushedToBacklog.size} to backlog, and freeze your streak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
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
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fresh Start",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the projected usable energy at a task's scheduled start time,
 * any active synaptic/meal/exercise delta, and a 1-tap "Shift to Peak" chip
 * if a HIGH-energy task is scheduled during an energy dip (< 50%).
 */
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
        isMismatch -> Color(0xFFFF5252)
        isOptimalFit -> Color(0xFF00E676)
        else -> Color(0xFF00E5FF)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Slot Usable Energy Pill
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
                    color = Color(0xFFFF8A65)
                )
            }
        }
        // Actionable Mismatch Warning Chip
        if (isMismatch && onShiftToPeakClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFB300).copy(alpha = 0.16f))
                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .clickable { onShiftToPeakClick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠ Dip Mismatch • Shift to Peak",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFFFB300)
                )
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2flowflowscreenv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt`
- **Lines of Code:** 388
- **Size:** 16,925 bytes (16.5 KB)

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
import com.neuroflow.app.domain.service.EnergyPhase
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
                if (msg.startsWith("Completed")) {
                    viewModel.undoCompleteTask()
                } else if (msg.startsWith("Deferred")) {
                    viewModel.undoDeferTask()
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
                    contentColor = Color.White,
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
                    tint = Color.White,
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
                learnedSampleCount = state.learnedSampleCount
            )

            // Flow Spotlight Hero Card (Takeover or Top Action)
            val topTask = state.recommendedTask ?: state.activeTasks.firstOrNull { it.isFrog } ?: state.activeTasks.firstOrNull()
            if (topTask != null) {
                val nowMillis = System.currentTimeMillis()
                val currentPhase = state.energyTimeline.minByOrNull {
                    kotlin.math.abs(it.timestampMillis - nowMillis)
                }?.phase ?: EnergyPhase.PEAK

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FlowSpotlightHeroCard(
                        activeTakeoverSlot = null,
                        topTaskId = topTask.id,
                        topTaskTitle = topTask.title,
                        topTaskFirstMicroStep = topTask.ifThenPlan?.let { com.neuroflow.app.domain.model.StepByStepPlanCodec.parse(it).firstOrNull()?.text },
                        topTaskDurationMinutes = topTask.estimatedDurationMinutes,
                        topTaskEnergyLevel = topTask.energyLevel,
                        isTopTaskFrog = topTask.isFrog,
                        currentPhase = currentPhase,
                        onStartFocusClick = { taskId -> onStartFocus(taskId) },
                        onCompleteQuickClick = { taskId ->
                            val taskToComplete = state.activeTasks.find { it.id == taskId } ?: topTask
                            viewModel.completeTask(taskToComplete)
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

            if (state.showZeroGuiltPrompt || (resetPlan != null && resetPlan.tasksPushedToBacklog.isNotEmpty())) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FreshStartRecoveryBanner(
                        visible = true,
                        resetPlan = resetPlan,
                        onApplyEmergencyReset = { viewModel.freshStartRollover() }
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
                            selectedLabelColor = Color.White,
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
                onCompleteTask = { task -> viewModel.completeTask(task) },
                onDeferTask = { task -> viewModel.deferTask(task) },
                onBreakDownTask = { task -> viewModel.breakDownTask(task) },
                onEditTask = { task -> editingTask = task },
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
            onOpenFullNotes = onNavigateToNotes
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowapppresentationv2flowflowviewmodelv2kt"></a>
### File: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt`
- **Lines of Code:** 521
- **Size:** 23,033 bytes (22.5 KB)

```kotlin
package com.neuroflow.app.presentation.v2.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.StepByStepPlanCodec
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeEnergyUiState
import com.neuroflow.app.domain.model.EnergyUiModel
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
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
    val selectedTag: String? = null,
    val availableTags: List<String> = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin"),
    val isEveningShutdownTime: Boolean = false,
    val eveningShutdownDismissed: Boolean = false,
    val lastCompletedTask: TaskEntity? = null,
    val lastDeferredTask: Pair<TaskEntity, Long>? = null,
    val lastActionMessage: String? = null
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
    private val behavioralSupportService: BehavioralSupportService = DefaultBehavioralSupportService()
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
                    old.sleepPressurePoints == new.sleepPressurePoints &&
                    old.onboardingCompleted == new.onboardingCompleted &&
                    old.tagCatalog == new.tagCatalog
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
        val naps = signal?.daytimeNaps ?: emptyList()
        val isFallback = sleepLogs.isEmpty() || (signal != null && signal.consolidatedSessions.isEmpty())

        val sevenDaysAgo = now - 7 * 86_400_000L
        val recentSleepLogs = sleepLogs.filter { it.startAt >= sevenDaysAgo || it.endAt >= sevenDaysAgo }
        val rollingAverageSleepMinutes = if (recentSleepLogs.isNotEmpty()) {
            recentSleepLogs.map { if (it.durationMinutes > 0) it.durationMinutes.toDouble() else (it.endAt - it.startAt) / 60_000.0 }.average()
        } else {
            480.0
        }

        val dailyBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = zonedDateTime.toLocalDate(),
            zoneId = systemZone
        )

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
            nowMillis = now
        )
        val battery = snapshot.usableEnergy
        val currentEnergyZone = when {
            battery >= 65 -> EnergyLevel.HIGH
            battery >= 35 -> EnergyLevel.MEDIUM
            else -> EnergyLevel.LOW
        }

        val overdueTasks = activeTasks.filter { task ->
            task.postponeCount >= 3 ||
                (task.scheduledTime != null && task.scheduledTime!! < now - 2 * 3600_000L) ||
                (task.scheduledDate != null && task.scheduledDate!! < now - 86_400_000L)
        }
        val showZeroGuilt = overdueTasks.size >= 3 && !dismissedGuilt

        // Dynamic Tag Catalog Merge
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
                postponeCount = task.postponeCount
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
                rollingAverageSleepMinutes = rollingAverageSleepMinutes
            )
        }.getOrNull()

        val dayStart = zonedDateTime.toLocalDate().atStartOfDay(systemZone).toInstant().toEpochMilli()
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
                rollingAverageSleepMinutes = rollingAverageSleepMinutes
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
            selectedTag = tag,
            availableTags = mergedTags,
            isEveningShutdownTime = isEvening,
            eveningShutdownDismissed = shutdownDismissed,
            lastCompletedTask = lastCompleted,
            lastDeferredTask = lastDeferred,
            lastActionMessage = actionMsg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FlowUiStateV2(isLoading = true)
    )

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insert(task)
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
            _lastActionMessage.value = "Completed: ${task.title}"
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
            val previousTime = task.scheduledTime ?: now
            _lastDeferredTask.value = Pair(task, previousTime)
            val newScheduledTime = previousTime + hours * 3600_000L
            taskRepository.update(
                task.copy(
                    scheduledTime = newScheduledTime,
                    postponeCount = task.postponeCount + 1,
                    updatedAt = now
                )
            )
            _lastActionMessage.value = "Deferred by ${hours}h: ${task.title}"
        }
    }

    fun undoDeferTask() {
        val pair = _lastDeferredTask.value ?: return
        val (originalTask, prevTime) = pair
        viewModelScope.launch {
            taskRepository.update(
                originalTask.copy(
                    scheduledTime = prevTime,
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
                    (task.scheduledTime != null && task.scheduledTime!! < now - 2 * 3600_000L) ||
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

    fun logPastTime(task: TaskEntity, minutes: Float) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TimeSessionEntity(
                taskId = task.id,
                startedAt = now - (minutes * 60_000f).toLong(),
                endedAt = now,
                durationMinutes = minutes,
                sessionType = "MANUAL_LOG"
            )
            sessionRepository?.insert(session)
            val newTracked = task.totalTimeTrackedMinutes + minutes
            val newCount = task.sessionCount + 1
            taskRepository.update(
                task.copy(
                    totalTimeTrackedMinutes = newTracked,
                    sessionCount = newCount,
                    lastSessionDurationMinutes = minutes,
                    actualDurationMinutes = newTracked,
                    updatedAt = now
                )
            )
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalFocusMinutes = prefs.totalFocusMinutes + minutes.toInt())
            }
            _lastActionMessage.value = "Logged ${minutes.toInt()}m past time for ${task.title}"
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
```

---

