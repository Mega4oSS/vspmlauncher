package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.artem.alaverdyan.vspmlauncher.ui.theme.airGlass

@Composable
fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(46.dp * scale)
            .airGlass(cornerRadius = 23.dp * scale)
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Настройки",
            tint = Color.White,
            modifier = Modifier.size(24.dp * scale)
        )
    }
}