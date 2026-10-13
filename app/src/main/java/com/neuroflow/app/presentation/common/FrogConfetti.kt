package com.neuroflow.app.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.random.Random

private data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val speedY: Float,
    val speedX: Float,
    val size: Float,
    val color: Color,
    val isFrogEmoji: Boolean
)

@Composable
fun FrogConfettiOverlay(
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    if (!trigger) return

    val progress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(
            Color(0xFF4CAF50), // Frog Green
            Color(0xFF81C784), // Light Green
            Color(0xFFFFD54F), // Gold
            Color(0xFF9C27B0), // NeuroFlow Purple
            Color(0xFFE91E63)  // Accent Pink
        )
        List(40) {
            ConfettiParticle(
                initialX = Random.nextFloat(),
                initialY = -Random.nextFloat() * 200f,
                speedY = Random.nextFloat() * 800f + 500f,
                speedX = (Random.nextFloat() - 0.5f) * 200f,
                size = Random.nextFloat() * 12f + 8f,
                color = colors.random(),
                isFrogEmoji = it % 6 == 0 // Every 6th particle is a 🐸
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
        )
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val currentProgress = progress.value
            val alpha = (1f - (currentProgress - 0.7f).coerceAtLeast(0f) / 0.3f).coerceIn(0f, 1f)

            particles.forEach { p ->
                val x = (p.initialX * canvasWidth + p.speedX * currentProgress).coerceIn(0f, canvasWidth)
                val y = p.initialY + p.speedY * currentProgress

                if (y in -50f..canvasHeight + 50f) {
                    if (p.isFrogEmoji) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "🐸",
                            x,
                            y,
                            android.graphics.Paint().apply {
                                textSize = p.size * 2.2f
                                this.alpha = (alpha * 255).toInt()
                            }
                        )
                    } else {
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.size / 2f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}
