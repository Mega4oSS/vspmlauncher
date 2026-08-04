package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.artem.alaverdyan.vspmlauncher.ui.BackendStatus

@Composable
fun BackendStatusBanner(
    status: BackendStatus,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    val (accent, textColor, icon, message) = when (status) {
        BackendStatus.Ok -> return
        is BackendStatus.Maintenance -> BannerStyle(Color(0xFFFFA000), Color(0xFFFFE0A3), "⚠", status.message)
        is BackendStatus.Unavailable -> BannerStyle(Color(0xFFFF5252), Color(0xFFFFCDD2), "⛔", status.message)
    }

    AnimatedVisibility(visible = true, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp * scale, vertical = 10.dp * scale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
        ) {
            Text(icon, color = textColor, fontSize = (14 * scale).sp)
            Text(message, color = textColor, fontSize = (12 * scale).sp)
        }
    }
}

private data class BannerStyle(
    val accent: Color,
    val textColor: Color,
    val icon: String,
    val message: String
)