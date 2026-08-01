package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ui/components/WindowBackdrop.kt
@Composable
fun WindowBackdrop(blurRadius: Dp = 18.dp) {
    Box(Modifier.fillMaxSize()) {
        AnimatedBackground(blurRadius = blurRadius) // заглушка, если снапшота нет (Wayland/ошибка)
    }
}