    package ru.artem.alaverdyan.vspmlauncher.ui.components

    import androidx.compose.foundation.Image
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.blur
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.unit.Dp
    import androidx.compose.ui.unit.dp

    @Composable
    fun AnimatedBackground(
        imageResPath: String = "images/bg_placeholder.png",
        blurRadius: Dp = 0.dp,
        windAngleDegrees: Float = -20f
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(imageResPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(blurRadius)
            )

            WindEffect(
                modifier = Modifier.fillMaxSize(),
                angleDegrees = windAngleDegrees
            )
        }
    }