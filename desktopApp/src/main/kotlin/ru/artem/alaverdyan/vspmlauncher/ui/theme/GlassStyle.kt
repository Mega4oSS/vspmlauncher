package ru.artem.alaverdyan.vspmlauncher.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GlassConfig {
    val CornerRadius: Dp = 20.dp
    val BorderWidth: Dp = 1.dp
    const val FILL_ALPHA: Float = 0.32f
    val FillColor: Color = Color(0xFFAEBFCE)
    val BorderColorTop: Color = Color.White.copy(alpha = 0.55f)
    val BorderColorBottom: Color = Color.White.copy(alpha = 0.08f)
    val AccentColor: Color = Color(0xFF3E9CFF)
}

fun Modifier.airGlass(
    cornerRadius: Dp = GlassConfig.CornerRadius,
    fillAlpha: Float = GlassConfig.FILL_ALPHA,
    fillColor: Color = GlassConfig.FillColor,
    borderWidth: Dp = GlassConfig.BorderWidth,
    borderColorTop: Color = GlassConfig.BorderColorTop
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(fillColor.copy(alpha = fillAlpha))
    .border(
        width = borderWidth,
        brush = Brush.verticalGradient(listOf(borderColorTop, GlassConfig.BorderColorBottom)),
        shape = RoundedCornerShape(cornerRadius)
    )

fun Modifier.windowFrame(
    cornerRadius: Dp = GlassConfig.CornerRadius,
    borderWidth: Dp = GlassConfig.BorderWidth
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .border(
        width = borderWidth,
        brush = Brush.verticalGradient(
            listOf(GlassConfig.BorderColorTop, GlassConfig.BorderColorBottom)
        ),
        shape = RoundedCornerShape(cornerRadius)
    )