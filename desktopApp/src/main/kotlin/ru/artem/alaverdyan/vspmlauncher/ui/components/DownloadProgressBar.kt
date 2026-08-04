package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.artem.alaverdyan.vspmlauncher.update.DownloadProgress
import ru.artem.alaverdyan.vspmlauncher.update.ProgressPhase
import kotlin.math.roundToInt

private fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "%.2f ГБ".format(bytes / gb)
        bytes >= mb -> "%.1f МБ".format(bytes / mb)
        bytes >= kb -> "%.0f КБ".format(bytes / kb)
        else -> "$bytes Б"
    }
}

@Composable
fun DownloadProgressBar(
    progress: DownloadProgress,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    val fraction = if (progress.totalBytes > 0) {
        (progress.downloadedBytes.toFloat() / progress.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percent = (fraction * 100).roundToInt()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp * scale)
    ) {
        when (progress.phase) {
            ProgressPhase.DOWNLOADING -> {
                Text(
                    text = "Скачивание: ${progress.currentFile} (${progress.fileIndex}/${progress.totalFiles})",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = (12 * scale).sp
                )

                LinearProgressIndicator(
                    progress = fraction,
                    modifier = Modifier.fillMaxWidth().height(6.dp * scale),
                    color = Color(0xFF3E9CFF),
                    backgroundColor = Color.White.copy(alpha = 0.15f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)} ($percent%)",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = (11 * scale).sp
                    )
                    Text(
                        text = "${formatBytes(progress.bytesPerSecond)}/с",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = (11 * scale).sp
                    )
                }
            }

            ProgressPhase.CLEANING -> {
                Text(
                    text = "Удаление мусора: ${progress.currentFile} (${progress.fileIndex}/${progress.totalFiles})",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = (12 * scale).sp
                )

                val cleanFraction = if (progress.totalFiles > 0) {
                    progress.fileIndex.toFloat() / progress.totalFiles.toFloat()
                } else 0f

                LinearProgressIndicator(
                    progress = cleanFraction,
                    modifier = Modifier.fillMaxWidth().height(6.dp * scale),
                    color = Color(0xFFFF6B6B),
                    backgroundColor = Color.White.copy(alpha = 0.15f)
                )
            }

            ProgressPhase.RUNTIME -> {
                Text(
                    text = "Установка Java: ${progress.currentFile}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = (12 * scale).sp
                )

                LinearProgressIndicator(
                    progress = fraction,
                    modifier = Modifier.fillMaxWidth().height(6.dp * scale),
                    color = Color(0xFF4CD97B),
                    backgroundColor = Color.White.copy(alpha = 0.15f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)} ($percent%)",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = (11 * scale).sp
                    )
                    Text(
                        text = "${formatBytes(progress.bytesPerSecond)}/с",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = (11 * scale).sp
                    )
                }
            }

            ProgressPhase.MODS -> {
                Text(
                    text = "Клиентские моды: ${progress.currentFile} (${progress.fileIndex}/${progress.totalFiles})",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = (12 * scale).sp
                )

                LinearProgressIndicator(
                    progress = fraction,
                    modifier = Modifier.fillMaxWidth().height(6.dp * scale),
                    color = Color(0xFF3E9CFF),
                    backgroundColor = Color.White.copy(alpha = 0.15f)
                )

                Text(
                    text = "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)} ($percent%)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = (11 * scale).sp
                )
            }

            ProgressPhase.VERIFYING -> {
                Text(
                    text = "Проверка: ${progress.currentFile} (${progress.fileIndex}/${progress.totalFiles})",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = (12 * scale).sp
                )

                val checkFraction = if (progress.totalFiles > 0) {
                    progress.fileIndex.toFloat() / progress.totalFiles.toFloat()
                } else 0f

                LinearProgressIndicator(
                    progress = checkFraction,
                    modifier = Modifier.fillMaxWidth().height(6.dp * scale),
                    color = Color(0xFFB388FF),
                    backgroundColor = Color.White.copy(alpha = 0.15f)
                )
            }

            ProgressPhase.MOVING -> {
                Text(
                    text = "Перемещение: ${progress.currentFile} (${progress.fileIndex}/${progress.totalFiles})",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = (12 * scale).sp
                )

                LinearProgressIndicator(
                    progress = fraction,
                    modifier = Modifier.fillMaxWidth().height(6.dp * scale),
                    color = Color(0xFFFFD54F),
                    backgroundColor = Color.White.copy(alpha = 0.15f)
                )

                Text(
                    text = "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)} ($percent%)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = (11 * scale).sp
                )
            }
        }
    }
}