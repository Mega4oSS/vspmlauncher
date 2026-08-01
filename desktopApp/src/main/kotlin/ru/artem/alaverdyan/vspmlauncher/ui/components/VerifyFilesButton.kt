package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.artem.alaverdyan.vspmlauncher.ui.theme.airGlass

/**
 * Кнопка "Проверить файлы" — принудительная сверка сборки/mojang-файлов/рантайма с сервером
 * по sha256 (в обход обычного delta-update, который сверяется не с диском, а с версией).
 */
@Composable
fun VerifyFilesButton(
    onClick: () -> Unit,
    isVerifying: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !isVerifying,
        modifier = modifier
            .size(46.dp * scale)
            .airGlass(cornerRadius = 23.dp * scale)
    ) {
        if (isVerifying) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp * scale),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = "Проверить файлы",
                tint = Color.White,
                modifier = Modifier.size(22.dp * scale)
            )
        }
    }
}