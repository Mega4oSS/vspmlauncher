package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VersionInfoBar(
    currentVersion: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Актуальная версия сборки: $currentVersion",
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 12.sp,
        modifier = modifier.padding(8.dp)
    )
}
