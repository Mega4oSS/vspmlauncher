package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.artem.alaverdyan.vspmlauncher.ui.theme.GlassConfig
import ru.artem.alaverdyan.vspmlauncher.ui.theme.airGlass

/**
 * Стеклянная кнопка — единый стиль вместо Material Button для всех второстепенных
 * действий (AdminScreen, диалоги и т.п.). SmartLaunchButton/SettingsButton не трогаем,
 * это отдельные именованные компоненты со своей семантикой.
 *
 * @param selected подсвечивает кнопку как "активную" (например, выбранный канал/версию) —
 * просто более плотная заливка стекла, без смены формы/цвета текста.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    enabled: Boolean = true,
    selected: Boolean = false,
    cornerRadius: Dp = 14.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .airGlass(
                cornerRadius = cornerRadius * scale,
                fillAlpha = if (selected) 0.45f else 0.28f,
                fillColor = if (selected) GlassConfig.AccentColor else GlassConfig.FillColor,
                borderColorTop = if (selected) GlassConfig.AccentColor.copy(alpha = 0.9f) else GlassConfig.BorderColorTop
            )
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp * scale, vertical = 10.dp * scale),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = (13 * scale).sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal // доп. подчёркивание выбора
        )
    }
}