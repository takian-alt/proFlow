package com.neuroflow.app.presentation.v2.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.model.PredictedEnergyCurveUiModel
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.util.Locale

private enum class EnergyChartRangeMode(val label: String) {
    WAKING("06:00–22:00"),
    FULL_DAY("24 Hours")
}

/**
 * Vico-powered card visualizing the user's predicted hourly daily energy curve alongside
 * historical task completion intensity, tailored to guide circadian auto-scheduling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PredictedDailyEnergyChart(
    curve: PredictedEnergyCurveUiModel?,
    onAutoScheduleClick: (() -> Unit)? = null,
    isAutoScheduling: Boolean = false,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (curve == null || curve.hourlyPoints.isEmpty()) return

    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    var rangeMode by remember { mutableStateOf(EnergyChartRangeMode.WAKING) }

    val visiblePoints = remember(curve.hourlyPoints, rangeMode) {
        when (rangeMode) {
            EnergyChartRangeMode.WAKING -> curve.hourlyPoints.filter { it.hour in 6..22 }
            EnergyChartRangeMode.FULL_DAY -> curve.hourlyPoints
        }
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(visiblePoints, curve.hasHistoricalData, curve.recoveryThresholdPercent) {
        if (visiblePoints.isNotEmpty()) {
            val xHours = visiblePoints.map { it.hour }
            val predictedY = visiblePoints.map { it.predictedEnergy }
            val thresholdY = List(xHours.size) { curve.recoveryThresholdPercent }
            val completionY = visiblePoints.map { it.historicalActivityScore }
            modelProducer.runTransaction {
                columnSeries {
                    series(x = xHours, y = completionY)
                }
                lineSeries {
                    series(x = xHours, y = predictedY)
                    series(x = xHours, y = thresholdY)
                }
            }
        }
    }

    val energyLineColor = NeuroFlowPaletteV2.OptimalEmerald
    val recoveryLineColor = NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.85f)
    val completionBarColor = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.34f)
    val axisLabelColor = NeuroFlowThemeV2Colors.TextSecondary
    val guidelineColor = NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.25f)

    val fixedRangeProvider = remember(rangeMode) {
        val minX = if (rangeMode == EnergyChartRangeMode.WAKING) 6.0 else 0.0
        val maxX = if (rangeMode == EnergyChartRangeMode.WAKING) 22.0 else 23.0
        CartesianLayerRangeProvider.fixed(
            minX = minX,
            maxX = maxX,
            minY = 0.0,
            maxY = 100.0
        )
    }

    val xFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            val hour = value.toInt().coerceIn(0, 23)
            String.format(Locale.ROOT, "%02d", hour)
        }
    }

    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            "${value.toInt()}%"
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("predicted_daily_energy_chart"),
        shape = cardShape,
        color = NeuroFlowPaletteV2.SurfaceCard,
        border = BorderStroke(1.dp, NeuroFlowPaletteV2.specularBorderBrush(energyLineColor, 0.75f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            energyLineColor.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(energyLineColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = energyLineColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Predicted Daily Energy",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeuroFlowThemeV2Colors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitleText = if (curve.hasHistoricalData) {
                                "${curve.modelConfidencePercent}% model confidence · ${curve.historicalCompletionCount} completions"
                            } else {
                                "Baseline forecast · Complete tasks to personalize"
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = NeuroFlowThemeV2Colors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("predicted_energy_chart_toggle")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse energy chart" else "Expand energy chart",
                        tint = NeuroFlowThemeV2Colors.TextSecondary
                    )
                }
            }

            // Key summary chips always visible even when collapsed
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EnergyInsightPill(
                    text = String.format(
                        Locale.ROOT,
                        "Best Auto-Slot %02d:00",
                        curve.bestAutoScheduleHour
                    ),
                    accent = energyLineColor
                )
                EnergyInsightPill(
                    text = String.format(
                        Locale.ROOT,
                        "Peak %02d:00–%02d:00 (%d%%)",
                        curve.peakStartHour,
                        curve.peakEndHour,
                        curve.peakEnergyValue
                    ),
                    accent = NeuroFlowPaletteV2.PeakAmber
                )
                EnergyInsightPill(
                    text = String.format(
                        Locale.ROOT,
                        "Dip %02d:00–%02d:00",
                        curve.dipStartHour,
                        curve.dipEndHour
                    ),
                    accent = NeuroFlowThemeV2Colors.TextSecondary
                )
                EnergyInsightPill(
                    text = "Recovery <${curve.recoveryThresholdPercent}%",
                    accent = NeuroFlowPaletteV2.RecoveryRose
                )
            }

            if (curve.currentHourBelowRecoveryThreshold) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.RecoveryRose.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("predicted_energy_recovery_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = "Recovery break recommended",
                            tint = NeuroFlowPaletteV2.RecoveryRose,
                            modifier = Modifier.size(16.dp)
                        )
                        val currentPct = curve.currentHourEnergyPercent ?: curve.recoveryThresholdPercent
                        Text(
                            text = "Energy at $currentPct% (below ${curve.recoveryThresholdPercent}% recovery threshold) — take a ${curve.recommendedBreakMinutes}-min break.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = NeuroFlowThemeV2Colors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Range selector + series legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(label = "Predicted", color = energyLineColor)
                            LegendDot(label = "Recovery", color = NeuroFlowPaletteV2.RecoveryRose)
                            LegendDot(label = "Done", color = NeuroFlowThemeV2Colors.ElectricIndigoGlow)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            EnergyChartRangeMode.entries.forEach { mode ->
                                val selected = rangeMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { rangeMode = mode },
                                    label = {
                                        Text(
                                            text = if (mode == EnergyChartRangeMode.WAKING) "Day" else "24h",
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = energyLineColor.copy(alpha = 0.18f),
                                        selectedLabelColor = energyLineColor,
                                        containerColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                        labelColor = NeuroFlowThemeV2Colors.TextSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("energy_chart_range_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // Vico Chart Host
                    val axisLabel = rememberAxisLabelComponent(
                        color = axisLabelColor,
                        textSize = 10.sp
                    )
                    val gridLine = rememberLineComponent(
                        fill = fill(guidelineColor),
                        thickness = 0.75.dp
                    )
                    val axisLine = rememberLineComponent(
                        fill = fill(NeuroFlowThemeV2Colors.SlateBorder.copy(alpha = 0.5f)),
                        thickness = 1.dp
                    )

                    val columnLayer = rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            rememberLineComponent(
                                fill = fill(completionBarColor),
                                thickness = 7.dp,
                                shape = CorneredShape.rounded(topLeftPercent = 40, topRightPercent = 40)
                            )
                        ),
                        rangeProvider = fixedRangeProvider
                    )

                    val lineLayer = rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(fill(energyLineColor)),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill = fill(energyLineColor.copy(alpha = 0.16f))
                                ),
                                thickness = 2.5.dp
                            ),
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(fill(recoveryLineColor)),
                                areaFill = null,
                                thickness = 1.5.dp
                            )
                        ),
                        rangeProvider = fixedRangeProvider
                    )

                    val scrollState = rememberVicoScrollState(scrollEnabled = false)

                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            columnLayer,
                            lineLayer,
                            startAxis = VerticalAxis.rememberStart(
                                label = axisLabel,
                                line = axisLine,
                                guideline = gridLine,
                                valueFormatter = yFormatter,
                                itemPlacer = remember { VerticalAxis.ItemPlacer.step(step = { 25.0 }) }
                            ),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                label = axisLabel,
                                line = axisLine,
                                guideline = null,
                                valueFormatter = xFormatter,
                                itemPlacer = remember(rangeMode) {
                                    HorizontalAxis.ItemPlacer.aligned(
                                        spacing = if (rangeMode == EnergyChartRangeMode.WAKING) 2 else 3,
                                        addExtremeLabelPadding = true
                                    )
                                }
                            )
                        ),
                        modelProducer = modelProducer,
                        scrollState = scrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .testTag("predicted_energy_vico_chart")
                    )

                    // Auto-Schedule Guidance Bar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                        border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = buildAutoScheduleGuidanceText(curve),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = NeuroFlowThemeV2Colors.TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (onAutoScheduleClick != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onAutoScheduleClick,
                                    enabled = !isAutoScheduling,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("energy_chart_auto_schedule_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Auto-Plan",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

private fun buildAutoScheduleGuidanceText(curve: PredictedEnergyCurveUiModel): String {
    val bestHourStr = String.format(Locale.ROOT, "%02d:00", curve.bestAutoScheduleHour)
    val bestEnergy = curve.hourlyPoints.getOrNull(curve.bestAutoScheduleHour)?.predictedEnergy ?: curve.peakEnergyValue
    return if (curve.hasHistoricalData) {
        "Place heavy/frog tasks around $bestHourStr ($bestEnergy% predicted energy from completion history)."
    } else {
        "Reserve $bestHourStr ($bestEnergy% energy) for deep focus. Complete tasks to refine this curve."
    }
}

@Composable
private fun EnergyInsightPill(
    text: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = accent,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LegendDot(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NeuroFlowThemeV2Colors.TextSecondary,
            fontSize = 11.sp
        )
    }
}
