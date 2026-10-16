package com.neuroflow.app.presentation.v2.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.SleepPressureDetector
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visualizes the Borbély Two-Process Model:
 * 1. Process C: Circadian alertness rhythm (oscillates sinusoidally via suprachiasmatic nucleus).
 * 2. Process S: Homeostatic sleep pressure (adenosine accumulation from wakefulness, cleared during sleep).
 * 3. Composite Bio-Battery: Resulting usable cognitive energy.
 */
@Composable
fun CircadianHeroBarometer(
    rawEnergyScore: Float,
    batteryPercent: Int,
    energyZone: EnergyLevel,
    sleepPressurePoints: Int = 450,
    fatigueZone: SleepPressureDetector.FatigueZone = SleepPressureDetector.FatigueZone.RESTED,
    circadianFactor: Float = 0.85f,
    recommendedTask: TaskEntity? = null,
    onStartRecommendedTask: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showExplainer by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = NeuroFlowThemeV2Colors.SlateSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Status Badge & Battery Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val (badgeText, badgeColor) = when (energyZone) {
                        EnergyLevel.HIGH -> "⚡ Process C Peak Alertness" to NeuroFlowThemeV2Colors.WarmAmber
                        EnergyLevel.MEDIUM -> "🔋 Sustained Focus Plateau" to NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        EnergyLevel.LOW -> "☕ Rest & Recovery Mode" to NeuroFlowThemeV2Colors.SageGreen
                    }
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    IconButton(
                        onClick = { showExplainer = !showExplainer },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Borbély Two-Process Model Explainer",
                            tint = NeuroFlowThemeV2Colors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$batteryPercent% Energy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            batteryPercent >= 65 -> NeuroFlowThemeV2Colors.SageGreen
                            batteryPercent >= 35 -> NeuroFlowThemeV2Colors.WarmAmber
                            else -> NeuroFlowThemeV2Colors.RoseCoral
                        }
                    )
                    Text(
                        text = "Sleep Pressure: $sleepPressurePoints pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = showExplainer) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "🔬 Borbély Two-Process Model",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                        Text(
                            text = "• Process C (Indigo line): Endogenous 24h circadian pacemaker driving alertness peaks and natural wind-downs.\n" +
                                    "• Process S (Amber dashed line): Homeostatic sleep pressure driven by adenosine accumulation during wakefulness.\n" +
                                    "• Bio-Battery ($batteryPercent%): Usable cognitive capacity determined by the gap between Circadian Alertness and Sleep Pressure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowThemeV2Colors.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual-Process Canvas Visualization
            val cal = Calendar.getInstance()
            val currentMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val currentFraction = currentMinute / 1440f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val processCPath = Path()
                    val processCFill = Path()
                    val processSPath = Path()

                    val points = 72
                    for (i in 0..points) {
                        val frac = i / points.toFloat()
                        val x = frac * w

                        // Process C: Dual-peak circadian oscillation
                        val cWave = 0.5f - 0.35f * cos(frac * 2 * Math.PI).toFloat() + 0.15f * sin(frac * 4 * Math.PI).toFloat()
                        val yC = (1f - cWave.coerceIn(0.12f, 0.88f)) * h

                        // Process S: Homeostatic accumulation (rises exponentially from wake hour ~06:00 to ~23:00)
                        val wakeFraction = (frac - 0.25f).coerceIn(0f, 0.75f) / 0.75f
                        val sCurve = (0.2f + 0.65f * (1f - kotlin.math.exp(-2.2f * wakeFraction))).coerceIn(0.15f, 0.85f)
                        val yS = (1f - sCurve) * h

                        if (i == 0) {
                            processCPath.moveTo(x, yC)
                            processCFill.moveTo(x, h)
                            processCFill.lineTo(x, yC)
                            processSPath.moveTo(x, yS)
                        } else {
                            processCPath.lineTo(x, yC)
                            processCFill.lineTo(x, yC)
                            processSPath.lineTo(x, yS)
                        }
                    }
                    processCFill.lineTo(w, h)
                    processCFill.close()

                    // Draw gradient under Process C
                    drawPath(
                        path = processCFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw Process C line (Indigo)
                    drawPath(
                        path = processCPath,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Process S line (Amber dashed)
                    drawPath(
                        path = processSPath,
                        color = NeuroFlowThemeV2Colors.WarmAmber.copy(alpha = 0.75f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                            cap = StrokeCap.Round
                        )
                    )

                    // Current Time Cursor & Beacon
                    val cursorX = currentFraction * w
                    val cursorCWave = 0.5f - 0.35f * cos(currentFraction * 2 * Math.PI).toFloat() + 0.15f * sin(currentFraction * 4 * Math.PI).toFloat()
                    val cursorCY = (1f - cursorCWave.coerceIn(0.12f, 0.88f)) * h

                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(cursorX, 0f),
                        end = Offset(cursorX, h),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Halo Beacon
                    drawCircle(
                        color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.35f),
                        radius = 8.dp.toPx(),
                        center = Offset(cursorX, cursorCY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(cursorX, cursorCY)
                    )
                    drawCircle(
                        color = NeuroFlowThemeV2Colors.ElectricIndigo,
                        radius = 2.5.dp.toPx(),
                        center = Offset(cursorX, cursorCY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Time horizon indicators & Curve legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(NeuroFlowThemeV2Colors.ElectricIndigoGlow, CircleShape))
                        Text("Process C (Alertness)", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(NeuroFlowThemeV2Colors.WarmAmber, CircleShape))
                        Text("Process S (Sleep Pressure)", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                    }
                }

                Text(
                    text = "Zone: ${fatigueZone.name}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when (fatigueZone) {
                        SleepPressureDetector.FatigueZone.RESTED -> NeuroFlowThemeV2Colors.SageGreen
                        SleepPressureDetector.FatigueZone.MODERATE -> NeuroFlowThemeV2Colors.WarmAmber
                        SleepPressureDetector.FatigueZone.HIGH, SleepPressureDetector.FatigueZone.CRITICAL -> NeuroFlowThemeV2Colors.RoseCoral
                    }
                )
            }

            // Optimal Next Biological Action Hero Recommendation
            if (recommendedTask != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🎯", fontSize = 14.sp)
                                Text(
                                    text = "Optimal Next Action",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                                )
                                if (recommendedTask.isFrog) {
                                    Text("🐸 Frog", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.SageGreen)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = recommendedTask.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${recommendedTask.estimatedDurationMinutes}m • Matched to your current $batteryPercent% battery",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeuroFlowThemeV2Colors.TextSecondary
                            )
                        }

                        // 1-Tap Launch Button (touch target >= 48dp)
                        Button(
                            onClick = { onStartRecommendedTask(recommendedTask.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Focus", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

