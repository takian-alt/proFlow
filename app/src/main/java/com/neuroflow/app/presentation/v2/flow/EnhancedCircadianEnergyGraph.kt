package com.neuroflow.app.presentation.v2.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
            Column(modifier = Modifier.weight(1f, fill = false)) {
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
            Spacer(modifier = Modifier.width(8.dp))

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
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
