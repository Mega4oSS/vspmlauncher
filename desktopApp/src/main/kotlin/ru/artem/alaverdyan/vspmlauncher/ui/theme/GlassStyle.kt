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

/**
 * Все параметры стиля Air Glass собраны здесь — правь только этот объект,
 * чтобы поменять "толщину стекла", прозрачность, скругления и т.д. по всему приложению.
 */
object GlassConfig {
    val CornerRadius: Dp = 20.dp
    val BorderWidth: Dp = 1.dp
    val FillAlpha: Float = 0.32f
    // Было 0xffb4c9da (светло-голубой) — на нём белый текст читался плохо.
    // Тёмно-синяя база с той же прозрачностью даёт нужный контраст под белый текст,
    // но сохраняет "стеклянность" (не превращается в сплошную плашку).
    val FillColor: Color = Color(0xFFAEBFCE)
    val BorderColorTop: Color = Color.White.copy(alpha = 0.55f)
    val BorderColorBottom: Color = Color.White.copy(alpha = 0.08f)

    // Акцентный синий для "выбранного" состояния (кнопка канала/версии)
    val AccentColor: Color = Color(0xFF3E9CFF)
}

fun Modifier.airGlass(
    cornerRadius: Dp = GlassConfig.CornerRadius,
    fillAlpha: Float = GlassConfig.FillAlpha,
    fillColor: Color = GlassConfig.FillColor, // ← новый параметр, по умолчанию прежнее поведение
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

/**
 * Рамка САМОГО окна: скругление + тонкая обводка без заливки.
 * Нужна отдельно от airGlass, т.к. окно undecorated+transparent —
 * без явного clip() Compose Desktop рисует контент прямоугольником
 * до самых краёв, и никакие скругления/рамки внутри не видно
 * снаружи как "рамку окна". Вешать на самый внешний Box в Window { }.
 */
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