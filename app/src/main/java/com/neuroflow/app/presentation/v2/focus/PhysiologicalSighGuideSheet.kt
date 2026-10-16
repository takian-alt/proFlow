package com.neuroflow.app.presentation.v2.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysiologicalSighGuideSheet(
    onDismiss: () -> Unit
) {
    var secondsRemaining by remember { mutableIntStateOf(60) }
    var isFinished by remember { mutableStateOf(false) }

    // 60-second countdown timer
    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
        isFinished = true
    }

    // 9-second Huberman cycle:
    // 0.0s - 2.5s: Inhale 1 (deep)
    // 2.5s - 3.5s: Inhale 2 (sharp top-off)
    // 3.5s - 9.0s: Exhale (slow physiological sigh)
    val infiniteTransition = rememberInfiniteTransition(label = "PhysiologicalSigh")
    val cycleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycleProgress"
    )

    val (phaseTitle, phaseInstruction, targetScale) = when {
        cycleProgress < 2.5f -> {
            val progress = cycleProgress / 2.5f
            Triple("1. Inhale Deeply", "Deep inhalation through the nose, filling your lungs...", 0.4f + 0.35f * progress)
        }
        cycleProgress < 3.5f -> {
            val progress = (cycleProgress - 2.5f) / 1.0f
            Triple("2. Sharp Top-Off Inhale", "Sniff in maximum air to re-open collapsed alveoli!", 0.75f + 0.25f * progress)
        }
        else -> {
            val progress = (cycleProgress - 3.5f) / 5.5f
            Triple("3. Slow Sigh Out", "Long, unforced sigh through the mouth to dump CO2...", 1.0f - 0.6f * progress)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeuroFlowThemeV2Colors.SlateSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NeuroFlowThemeV2Colors.SlateBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🫁 60-Second Physiological Sigh",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary
                    )
                    Text(
                        text = "Fastest neurobiological reset for cortisol and autonomic arousal",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NeuroFlowThemeV2Colors.TextSecondary)
                }
            }

            // Animated Breathing Sphere
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = (size.minDimension / 2f) * targetScale
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Outer soft glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeuroFlowThemeV2Colors.SageGreen.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.3f
                        ),
                        radius = radius * 1.3f,
                        center = center
                    )

                    // Inner core circle
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                NeuroFlowThemeV2Colors.SageGreen,
                                NeuroFlowThemeV2Colors.ElectricIndigoGlow
                            )
                        ),
                        radius = radius,
                        center = center
                    )

                    // Accent stroke ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Text(
                    text = "${secondsRemaining}s",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Phase instructions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = phaseTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeuroFlowThemeV2Colors.SageGreen
                )
                Text(
                    text = phaseInstruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeuroFlowThemeV2Colors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Evidence citation
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
            ) {
                Text(
                    text = "Huberman / Stanford Protocol: Two rapid nasal inhales inflate collapsed alveoli in the lungs; the extended oral exhale maximizes CO2 offload and slows heart rate via the vagus nerve.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowThemeV2Colors.TextTertiary,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 16.sp
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFinished) NeuroFlowThemeV2Colors.SageGreenDark else NeuroFlowThemeV2Colors.ElectricIndigo
                )
            ) {
                Text(if (isFinished) "Reset Complete — Resume Focus" else "Dismiss", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
