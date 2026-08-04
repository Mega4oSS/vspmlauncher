package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import ru.artem.alaverdyan.vspmlauncher.ui.theme.GlassConfig
import ru.artem.alaverdyan.vspmlauncher.ui.theme.airGlass

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassConfig.CornerRadius,
    fillAlpha: Float = GlassConfig.FILL_ALPHA,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.airGlass(cornerRadius = cornerRadius, fillAlpha = fillAlpha)) {
        content()
    }
}
