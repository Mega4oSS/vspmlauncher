package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.EventQueue
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Window

object ScreenBehindCapture {
    private val robot: Robot? = runCatching { Robot() }.getOrNull()

    fun capture(window: Window): ImageBitmap? {
        val r = robot ?: return null
        val bounds = window.bounds
        if (bounds.width <= 0 || bounds.height <= 0) return null

        val wasVisible = window.isVisible
        return runCatching {
            if (wasVisible) {
                // isVisible нельзя дёргать с постороннего потока — это EDT-операция.
                // invokeAndWait гарантирует, что скрытие реально применилось
                // ДО того как Robot начнёт фотографировать экран.
                EventQueue.invokeAndWait { window.isVisible = false }
            }
            Thread.sleep(80) // дать WM/композитору перерисовать освободившуюся область
            r.createScreenCapture(Rectangle(bounds.x, bounds.y, bounds.width, bounds.height))
                .toComposeImageBitmap()
        }.also {
            if (wasVisible) {
                EventQueue.invokeAndWait { window.isVisible = true }
            }
        }.getOrNull()
    }
}