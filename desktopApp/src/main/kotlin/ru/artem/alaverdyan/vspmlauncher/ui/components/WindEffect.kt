package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin

private data class WindParticle(
    val lane: Float,
    val phase: Float,
    val length: Float,
    val alpha: Float,
    val strokeWidth: Float
)

@Composable
fun WindEffect(
    modifier: Modifier = Modifier,
    angleDegrees: Float = -20f,
    minLength: Float = 50f,
    maxLength: Float = 160f,
    color: Color = Color.White.copy(alpha = 0.35f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wind")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (5000 / 1.4f).toInt().coerceAtLeast(500), easing = LinearEasing)
        ),
        label = "windProgress"
    )

    val particles = remember(60, minLength, maxLength) {
        List(60) {
            WindParticle(
                lane = Random.nextFloat(),
                phase = Random.nextFloat(),
                length = Random.nextFloat() * (maxLength - minLength) + minLength,
                alpha = 0.15f + Random.nextFloat() * 0.45f,
                strokeWidth = 1.2f + Random.nextFloat() * 1.6f
            )
        }
    }

    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val dirX = cos(angleRad).toFloat()
    val dirY = sin(angleRad).toFloat()
    val perpX = -dirY
    val perpY = dirX

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val travelDistance = w + h

        particles.forEach { p ->
            val t = (progress + p.phase) % 1f
            val distance = t * travelDistance
            val startX = w * 0.5f - dirX * travelDistance * 0.6f + perpX * (p.lane - 0.5f) * travelDistance
            val startY = h * 0.5f - dirY * travelDistance * 0.6f + perpY * (p.lane - 0.5f) * travelDistance

            val x = startX + dirX * distance
            val y = startY + dirY * distance

            drawLine(
                color = color.copy(alpha = p.alpha),
                start = Offset(x, y),
                end = Offset(x - dirX * p.length, y - dirY * p.length),
                strokeWidth = p.strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
