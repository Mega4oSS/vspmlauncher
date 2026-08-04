package ru.artem.alaverdyan.vspmlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.artem.alaverdyan.vspmlauncher.ui.components.*
import ru.artem.alaverdyan.vspmlauncher.update.DownloadProgress

@Composable
fun MainScreen(
    buildVersion: String,
    needsUpdate: Boolean,
    hasExistingInstall: Boolean,
    isGameRunning: Boolean,
    news: List<NewsItem>,
    downloadProgress: DownloadProgress?,
    isCheckingUpdates: Boolean,
    isMaintenance: Boolean,
    isBusy: Boolean,
    launchError: String?,
    backendStatus: BackendStatus,
    onDismissLaunchError: () -> Unit,
    onLaunchOrUpdate: () -> Unit,
    onCloseGame: () -> Unit,
    onShipClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogoClick: () -> Unit
) {

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scale = (maxWidth / 1000.dp).coerceIn(0.5f, 2.2f)

        val logoSize = 200.dp * scale
        val shipSize = 700.dp * scale
        val panelWidth = 320.dp * scale
        val edgePadding = 24.dp * scale

        AnimatedBackground(
            imageResPath = "images/bg_placeholder.png",
            blurRadius = 2.dp,
            windAngleDegrees = -20f
        )

        AnimatedLogo(
            imageResPath = "images/logo.png",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(edgePadding)
                .width(logoSize)
                .aspectRatio(1.9f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLogoClick
                )
        )

        AnimatedShip(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = edgePadding, bottom = edgePadding)
                .width(shipSize)
                .aspectRatio(1.78f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onShipClick
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(edgePadding)
                .width(panelWidth),
            verticalArrangement = Arrangement.spacedBy(20.dp * scale)
        ) {
            NewsWidget(
                news = news,
                scale = scale,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            BackendStatusBanner(
                status = backendStatus,
                scale = scale,
                modifier = Modifier.fillMaxWidth()
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp * scale),
                    verticalArrangement = Arrangement.spacedBy(10.dp * scale)
                ) {
                    Text(
                        text = "Версия сборки: $buildVersion",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = (12 * scale).sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp * scale),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        SmartLaunchButton(
                            needsUpdate = needsUpdate,
                            hasExistingInstall = hasExistingInstall,
                            isRunning = isGameRunning,
                            isMaintenance = isMaintenance,
                            isCheckingUpdates = isCheckingUpdates,
                            isBusy = isBusy,
                            scale = scale,
                            onClick = if (isGameRunning) onCloseGame else onLaunchOrUpdate,
                            modifier = Modifier.weight(1f)
                        )
                        SettingsButton(onClick = onOpenSettings, scale = scale)
                    }

                    if (downloadProgress != null) {
                        DownloadProgressBar(
                            progress = downloadProgress,
                            scale = scale,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (launchError != null) {
            val clipboardManager = LocalClipboardManager.current
            var justCopied by remember(launchError) { mutableStateOf(false) }

            LaunchedEffect(justCopied) {
                if (justCopied) {
                    delay(1500)
                    justCopied = false
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(edgePadding)
                    .widthIn(max = 520.dp * scale)
                    .fillMaxWidth(0.42f)
                    .background(Color(0xFFFF5252).copy(alpha = 0.18f), RoundedCornerShape(14.dp * scale))
                    .padding(16.dp * scale)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp * scale),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (justCopied) "✓" else "⚠",
                            color = if (justCopied) Color(0xFF4CD97B) else Color(0xFFFF8A80),
                            fontSize = (18 * scale).sp
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(launchError))
                                    justCopied = true
                                }
                        ) {
                            Text(
                                text = launchError,
                                color = Color(0xFFFFCDD2),
                                fontSize = (13 * scale).sp
                            )
                            Spacer(Modifier.height(4.dp * scale))
                            Text(
                                text = if (justCopied) "Скопировано" else "Нажми, чтобы скопировать",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = (10 * scale).sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissLaunchError,
                        modifier = Modifier.size(28.dp * scale)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}