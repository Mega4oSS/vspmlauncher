package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalMinimumTouchTargetEnforcement
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import ru.artem.alaverdyan.vspmlauncher.ui.theme.GlassConfig
import java.awt.MouseInfo
import kotlin.math.abs

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun WindowScope.CustomTitleBar(
    title: String,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onWindowMoved: () -> Unit = {},
    onMaximizeToggle: (() -> Unit)? = null,
    isMaximized: Boolean = false,
    onRestoreFromMaximizedDrag: ((targetX: Int, targetY: Int) -> Unit)? = null,
    restoredWidthPx: Int = 1000,
    cornerRadius: Dp = GlassConfig.CornerRadius,
    modifier: Modifier = Modifier
) {
    val topShape: Shape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    var lastClickTime by remember { mutableStateOf(0L) }

    val isMaximizedState = rememberUpdatedState(isMaximized)
    val restoreCallbackState = rememberUpdatedState(onRestoreFromMaximizedDrag)
    val restoredWidthState = rememberUpdatedState(restoredWidthPx)
    val maximizeToggleState = rememberUpdatedState(onMaximizeToggle)

    val dragThresholdPx = 4

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(topShape)
            .background(GlassConfig.FillColor.copy(alpha = 0.55f))
            .border(width = 1.dp, color = GlassConfig.BorderColorTop, shape = topShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Экранные координаты точки захвата — не зависят от перемещений окна
                    val downScreen = MouseInfo.getPointerInfo()?.location
                    val downScreenX = downScreen?.x ?: (window.x + down.position.x.toInt())
                    val downScreenY = downScreen?.y ?: (window.y + down.position.y.toInt())

                    val startWasMaximized = isMaximizedState.value
                    val grabRelX = down.position.x / window.width.coerceAtLeast(1)
                    val grabOffsetY = down.position.y.toInt()

                    var restoredFromMaximize = false
                    var dragging = false
                    var offsetX = 0
                    var offsetY = 0

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                            ?: break

                        if (change.positionChanged()) {
                            val screen = MouseInfo.getPointerInfo()?.location

                            if (screen != null) {
                                val dx = screen.x - downScreenX
                                val dy = screen.y - downScreenY

                                if (!dragging && (abs(dx) > dragThresholdPx || abs(dy) > dragThresholdPx)) {
                                    dragging = true

                                    if (startWasMaximized) {
                                        val width = restoredWidthState.value
                                        val targetX = (screen.x - width * grabRelX).toInt()
                                        val targetY = screen.y - grabOffsetY

                                        restoreCallbackState.value?.invoke(targetX, targetY)
                                        restoredFromMaximize = true

                                        // окно уже стоит там, где нужно — дальше просто следуем за курсором
                                        offsetX = screen.x - targetX
                                        offsetY = screen.y - targetY
                                    } else {
                                        offsetX = screen.x - window.x
                                        offsetY = screen.y - window.y
                                    }
                                }

                                if (dragging && (!startWasMaximized || restoredFromMaximize)) {
                                    window.setLocation(screen.x - offsetX, screen.y - offsetY)
                                }
                            }

                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    if (!dragging) {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime < 300) {
                            maximizeToggleState.value?.invoke()
                            lastClickTime = 0L
                        } else {
                            lastClickTime = now
                        }
                    } else {
                        onWindowMoved()
                    }
                }
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp
        )

        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TitleBarButton(onClick = onMinimize) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Свернуть",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (onMaximizeToggle != null) {
                    TitleBarButton(onClick = onMaximizeToggle) {
                        Icon(
                            if (isMaximized) Icons.Filled.FilterNone else Icons.Filled.CropSquare,
                            contentDescription = if (isMaximized) "Восстановить" else "Развернуть",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                TitleBarButton(onClick = onClose, hoverColor = Color(0xFFE81123)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TitleBarButton(
    onClick: () -> Unit,
    hoverColor: Color = Color.White.copy(alpha = 0.18f),
    content: @Composable () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isHovered) hoverColor else Color.Transparent)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
    ) {
        content()
    }
}