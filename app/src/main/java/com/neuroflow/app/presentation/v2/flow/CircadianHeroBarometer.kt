package com.neuroflow.app.presentation.v2.flow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircadianHeroBarometer(
    rawEnergyScore: Float,
    batteryPercent: Int,
    energyZone: EnergyLevel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        EnergyLevel.HIGH -> "⚡ Circadian Peak Alertness" to NeuroFlowThemeV2Colors.WarmAmber
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
                }

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
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Circadian Wave Curve Canvas
            val cal = Calendar.getInstance()
            val currentMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val currentFraction = currentMinute / 1440f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val wavePath = Path()
                    val fillPath = Path()

                    val points = 60
                    for (i in 0..points) {
                        val frac = i / points.toFloat()
                        val x = frac * w
                        // Raised cosine approximation with morning/afternoon peaks
                        val normalizedWave = 0.5f - 0.35f * cos(frac * 2 * Math.PI).toFloat() + 0.15f * sin(frac * 4 * Math.PI).toFloat()
                        val y = (1f - normalizedWave.coerceIn(0.1f, 0.9f)) * h

                        if (i == 0) {
                            wavePath.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            wavePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }
                    fillPath.lineTo(w, h)
                    fillPath.close()

                    // Draw gradient fill under circadian wave
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw the smooth sine wave stroke
                    drawPath(
                        path = wavePath,
                        color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw current time beacon
                    val cursorX = currentFraction * w
                    val cursorWave = 0.5f - 0.35f * cos(currentFraction * 2 * Math.PI).toFloat() + 0.15f * sin(currentFraction * 4 * Math.PI).toFloat()
                    val cursorY = (1f - cursorWave.coerceIn(0.1f, 0.9f)) * h

                    // Vertical dashed timeline cursor
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(cursorX, 0f),
                        end = Offset(cursorX, h),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Beacon dot
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(cursorX, cursorY)
                    )
                    drawCircle(
                        color = NeuroFlowThemeV2Colors.ElectricIndigo,
                        radius = 3.dp.toPx(),
                        center = Offset(cursorX, cursorY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Time horizon indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("06:00 Rise", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                Text("12:00 Peak", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                Text("18:00 Plateau", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
                Text("23:00 Wind-Down", style = MaterialTheme.typography.labelSmall, color = NeuroFlowThemeV2Colors.TextTertiary)
            }
        }
    }
}
