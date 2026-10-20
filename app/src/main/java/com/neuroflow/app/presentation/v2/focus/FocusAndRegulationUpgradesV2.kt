package com.neuroflow.app.presentation.v2.focus

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.engine.WoopEngine
import com.neuroflow.app.domain.model.StepByStepPlanStep
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import kotlinx.coroutines.delay

/**
 * 1. Zen Micro-Step Spotlight Card for FocusScreenV2:
 * Shows ONLY the active micro-step in bold typography to prevent ADHD working-memory overload.
 */
@Composable
fun ZenMicroStepSpotlightCard(
    currentStepIndex: Int,
    totalSteps: Int,
    currentStepTitle: String,
    currentStepEstimatedMinutes: Int,
    onCompleteCurrentStep: () -> Unit,
    onSkipStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        color = NeuroFlowPaletteV2.SurfaceCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NeuroFlowPaletteV2.ReboundCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "STEP ${currentStepIndex + 1} OF ${totalSteps.coerceAtLeast(1)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = NeuroFlowPaletteV2.ReboundCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "~${currentStepEstimatedMinutes}m target",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = currentStepTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompleteCurrentStep()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeuroFlowPaletteV2.OptimalEmerald,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Complete Step",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .height(44.dp)
                        .clickable { onSkipStep() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Step",
                            tint = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. Interactive Physiological Sigh Guide Sheet (Double Inhale + Long Exhale)
 */
@Composable
fun InteractivePhysiologicalSighSheet(
    totalCycles: Int = 3,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var cycle by remember { mutableIntStateOf(1) }
    var breathStage by remember { mutableIntStateOf(0) }

    LaunchedEffect(cycle, breathStage) {
        when (breathStage) {
            0 -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(2400L)
                breathStage = 1
            }
            1 -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(1100L)
                breathStage = 2
            }
            2 -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(5200L)
                if (cycle < totalCycles) {
                    cycle++
                    breathStage = 0
                } else {
                    onCompleted()
                }
            }
        }
    }

    val targetScale = when (breathStage) {
        0 -> 0.76f
        1 -> 1.0f
        else -> 0.34f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = when (breathStage) {
                0 -> 2300
                1 -> 950
                else -> 5000
            },
            easing = FastOutSlowInEasing
        ),
        label = "sigh_lung_scale"
    )
    val stageLabel = when (breathStage) {
        0 -> "1. Deep Nasal Inhale..."
        1 -> "2. Quick Top-Off Sniff In!"
        else -> "3. Long, Slow Mouth Exhale..."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(NeuroFlowPaletteV2.SurfaceCard)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PHYSIOLOGICAL SIGH DECOMPRESSION",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = NeuroFlowPaletteV2.ReboundCyan,
            letterSpacing = 1.1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cycle $cycle of $totalCycles",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(190.dp)
        ) {
            val reboundCyan = NeuroFlowPaletteV2.ReboundCyan
            val dipIndigo = NeuroFlowPaletteV2.DipIndigo
            Canvas(modifier = Modifier.size(190.dp)) {
                val maxRadius = size.minDimension / 2f
                drawCircle(
                    color = reboundCyan.copy(alpha = 0.10f),
                    radius = maxRadius
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            reboundCyan.copy(alpha = 0.55f),
                            dipIndigo.copy(alpha = 0.20f)
                        )
                    ),
                    radius = maxRadius * animatedScale
                )
            }
            Text(
                text = stageLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Two inhales maximally inflate alveoli; the extended exhale triggers vagal nerve slowing and clears acute executive friction.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 3. One-Tap Haptic Affordance Rating Sheet (AffordanceRatingSheet.kt)
 */
@Composable
fun UpgradedAffordanceRatingSheet(
    taskTitle: String,
    onRatingSelected: (Float) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val options = listOf(
        Triple(-2.0f, "😫", "Much Harder"),
        Triple(-1.0f, "😓", "Draining"),
        Triple(0.0f, "🎯", "As Expected"),
        Triple(1.0f, "✨", "Smooth"),
        Triple(2.0f, "⚡", "Energizing")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(NeuroFlowPaletteV2.SurfaceCard)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CALIBRATE YOUR RHYTHM",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = NeuroFlowPaletteV2.PeakAmber,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "How did \"$taskTitle\" feel compared to what you expected?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "1 tap updates your personal circadian curve and future task scheduling.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (score, emoji, label) ->
                Surface(
                    color = NeuroFlowPaletteV2.SurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRatingSelected(score)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Skip for now",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier
                .clickable { onSkip() }
                .padding(8.dp)
        )
    }
}

/**
 * 4. Guided WOOP Obstacle & If-Then Plan Sheet (WoopPromptSheet.kt)
 */
@Composable
fun UpgradedWoopPromptSheet(
    taskTitle: String,
    dreadedTaskInsight: String?,
    onSaveWoopPlan: (wish: String, outcome: String, obstacle: String, ifThenPlan: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presetObstacles = listOf(
        "I feel overwhelmed by how big it is",
        "I get distracted by my phone",
        "I don't know where to start",
        "I'm worried it won't be perfect"
    )
    var obstacle by remember { mutableStateOf(presetObstacles.first()) }
    var thenAction by remember { mutableStateOf("open the file and work for just 2 minutes.") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(NeuroFlowPaletteV2.SurfaceCard)
            .padding(22.dp)
    ) {
        Text(
            text = "WOOP FRICTION SHIELD",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = NeuroFlowPaletteV2.ReboundCyan,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Unblock \"$taskTitle\"",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        if (!dreadedTaskInsight.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "🏆 $dreadedTaskInsight",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.OptimalEmerald,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "1. Tap your main internal obstacle:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        presetObstacles.forEach { preset ->
            val selected = obstacle == preset
            Surface(
                color = if (selected) NeuroFlowPaletteV2.DipIndigo.copy(alpha = 0.22f) else NeuroFlowPaletteV2.SurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (selected) NeuroFlowPaletteV2.DipIndigo else NeuroFlowPaletteV2.BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { obstacle = preset }
            ) {
                Text(
                    text = preset,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "2. Your automatic If-Then reflex:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = thenAction,
            onValueChange = { thenAction = it },
            prefix = {
                Text(
                    text = WoopEngine.generateIfThenPlan(obstacle),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeuroFlowPaletteV2.ReboundCyan
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val fullPlan = WoopEngine.generateIfThenPlan(obstacle) + thenAction.trim()
                onSaveWoopPlan(
                    "Complete $taskTitle",
                    "Feel relieved and build momentum",
                    obstacle,
                    fullPlan
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = NeuroFlowPaletteV2.ReboundCyan,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(
                text = "Lock In If-Then Plan",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}
