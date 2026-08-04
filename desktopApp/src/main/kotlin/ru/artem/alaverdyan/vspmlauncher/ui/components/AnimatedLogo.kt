package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AnimatedLogo(
    imageResPath: String = "logo/logo_placeholder.png",
    flyInFromX: Float = -420f,
    flyInFromY: Float = -220f,
    flyInDurationMs: Int = 900,
    floatAmplitudeDp: Float = 6f,
    floatDurationMs: Int = 2600,
    modifier: Modifier = Modifier
) {
    val offsetX = remember { Animatable(flyInFromX) }
    val offsetY = remember { Animatable(flyInFromY) }
    val scale = remember { Animatable(0.55f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            offsetX.animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            offsetY.animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            scale.animateTo(1f, animationSpec = tween(flyInDurationMs, easing = FastOutSlowInEasing))
        }
        launch {
            alpha.animateTo(1f, animationSpec = tween(flyInDurationMs / 2))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "logoFloat")
    val floatOffsetDp by infiniteTransition.animateFloat(
        initialValue = -floatAmplitudeDp,
        targetValue = floatAmplitudeDp,
        animationSpec = infiniteRepeatable(
            animation = tween(floatDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoFloatOffset"
    )
    val floatRotationDeg by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(floatDurationMs + 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoFloatRotation"
    )

    Image(
        painter = painterResource(imageResPath),
        contentDescription = "ВСПМ 5 logo",
        modifier = modifier
            .offset(x = offsetX.value.dp, y = (offsetY.value + floatOffsetDp).dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = floatRotationDeg
                this.alpha = alpha.value
            }
    )
}
