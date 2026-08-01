package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AnimatedShip(
    imageResPath: String,
    modifier: Modifier = Modifier,
    entryOffsetX: Dp = 260.dp,
    entryOffsetY: Dp = 50.dp,
    entryDurationMs: Int = 900,
    entryBlurRadius: Dp = 14.dp,
    bobAmplitudeDegrees: Float = 4f,
    bobPeriodMs: Int = 2600,
    kickMinDelayMs: Long = 3500,
    kickMaxDelayMs: Long = 7000,
    kickMaxOffset: Dp = 10.dp
) {
    val density = LocalDensity.current

    // --- Фаза 1: влёт на место ---
    val entryProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = entryDurationMs, easing = EaseOutCubic)
        )
    }

    // --- Фаза 2: покачивание носа — считаем время через кадры ---
    var bobTimeMs by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        var startTime = -1L
        while (true) {
            withFrameMillis { frameTimeMs ->
                if (startTime < 0L) startTime = frameTimeMs
                bobTimeMs = frameTimeMs - startTime
            }
        }
    }

    // --- Фаза 3a: случайные толчки по X (только вправо) ---
    val kickX = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(kickMinDelayMs, kickMaxDelayMs))
            val kickPxX = with(density) { Random.nextFloat() * kickMaxOffset.toPx() }
            kickX.animateTo(kickPxX, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            kickX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow))
        }
    }

    // --- Фаза 3b: случайные толчки по Y (вверх/вниз) ---
    val kickY = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(kickMinDelayMs, kickMaxDelayMs))
            val kickPxY = with(density) { (Random.nextFloat() - 0.5f) * kickMaxOffset.toPx() * 2f }
            kickY.animateTo(kickPxY, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            kickY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow))
        }
    }

    val entryOffsetXPx = with(density) { entryOffsetX.toPx() }
    val entryOffsetYPx = with(density) { entryOffsetY.toPx() }

    Image(
        painter = painterResource(imageResPath),
        contentDescription = "Корабль",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer {
                val eased = 1f - entryProgress.value

                // Влёт слева-снизу: X стартует левее финальной позиции, Y — ниже
                translationX = -eased * entryOffsetXPx + kickX.value
                translationY = eased * entryOffsetYPx + kickY.value

                val bobPhase = (bobTimeMs % bobPeriodMs).toFloat() / bobPeriodMs
                rotationZ = sin(bobPhase * 2 * Math.PI).toFloat() * bobAmplitudeDegrees

                val scale = 0.85f + 0.15f * entryProgress.value
                scaleX = scale
                scaleY = scale

                alpha = entryProgress.value.coerceIn(0f, 1f)
            }
            .blur(entryBlurRadius * (1f - entryProgress.value))
    )
}