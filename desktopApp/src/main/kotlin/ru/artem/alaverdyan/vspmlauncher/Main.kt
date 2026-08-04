package ru.artem.alaverdyan.vspmlauncher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.artem.alaverdyan.vspmlauncher.data.NicknameStorage
import ru.artem.alaverdyan.vspmlauncher.data.SettingsStorage
import ru.artem.alaverdyan.vspmlauncher.data.TransparencyMode
import ru.artem.alaverdyan.vspmlauncher.launch.LaunchResult
import ru.artem.alaverdyan.vspmlauncher.network.AnalyticsClient
import ru.artem.alaverdyan.vspmlauncher.runtime.PlatformInfo
import ru.artem.alaverdyan.vspmlauncher.ui.App
import ru.artem.alaverdyan.vspmlauncher.ui.ConsoleWindow
import ru.artem.alaverdyan.vspmlauncher.ui.components.AnimatedBackground
import ru.artem.alaverdyan.vspmlauncher.ui.components.CustomTitleBar
import ru.artem.alaverdyan.vspmlauncher.ui.theme.GlassConfig
import ru.artem.alaverdyan.vspmlauncher.ui.theme.windowFrame

fun main() {
    val decoratorEnabled = SettingsStorage.loadAppDecoratorEnabled()
    val transparencyMode = SettingsStorage.loadTransparencyMode()
    val isLinux = PlatformInfo.os == "linux"
    val useRealTransparency = decoratorEnabled && (!isLinux || transparencyMode == TransparencyMode.REAL)

    if (useRealTransparency && isLinux) {
        System.setProperty("skiko.renderApi", "SOFTWARE")
    }

    launcherApp(decoratorEnabled = decoratorEnabled, useRealTransparency = useRealTransparency)
}

private fun launcherApp(decoratorEnabled: Boolean, useRealTransparency: Boolean) = application {
    LaunchedEffect(Unit) { AnalyticsClient.trackAppOpen(NicknameStorage.load()) }
    var consoleLaunchResult by remember { mutableStateOf<LaunchResult?>(null) }

    val mainWindowState = rememberWindowState(width = 1000.dp, height = 650.dp)

    Window(
        onCloseRequest = { AnalyticsClient.trackAppClose(); exitApplication() },
        title = "ВСПМ 5 — Launcher",
        state = mainWindowState,
        resizable = true,
        undecorated = decoratorEnabled,
        icon = painterResource("logo/icon-128.png"),
        transparent = useRealTransparency
    ) {
        LaunchedEffect(Unit) {
            window.isVisible = false
            window.isVisible = true
            window.toFront()
            window.repaint()
        }
        window.minimumSize = java.awt.Dimension(760, 480)

        val isMaximized = mainWindowState.placement == WindowPlacement.Maximized
        var restoredSize by remember { mutableStateOf(mainWindowState.size) }
        var restoredPosition by remember { mutableStateOf(mainWindowState.position) }
        val density = LocalDensity.current
        val restoredWidthPx = with(density) { restoredSize.width.toPx() }.toInt()
        val activeCornerRadius = if (!decoratorEnabled || isMaximized || !useRealTransparency) {
            0.dp
        } else {
            GlassConfig.CornerRadius
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (!useRealTransparency) {
                AnimatedBackground(blurRadius = 18.dp)
            }

            Box(
                modifier = if (decoratorEnabled) {
                    Modifier.fillMaxSize().windowFrame(cornerRadius = activeCornerRadius)
                } else {
                    Modifier.fillMaxSize()
                }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (decoratorEnabled) {
                        CustomTitleBar(
                            title = "ВСПМ 5 — Launcher",
                            onMinimize = { window.isMinimized = true },
                            onClose = ::exitApplication,
                            onMaximizeToggle = {
                                if (isMaximized) {
                                    mainWindowState.placement = WindowPlacement.Floating
                                    mainWindowState.size = restoredSize
                                    mainWindowState.position = restoredPosition
                                } else {
                                    restoredSize = mainWindowState.size
                                    restoredPosition = mainWindowState.position
                                    mainWindowState.placement = WindowPlacement.Maximized
                                }
                            },
                            onWindowMoved = { AnalyticsClient.trackWindowMoved() },
                            isMaximized = isMaximized,
                            onRestoreFromMaximizedDrag = { targetXPx, targetYPx ->
                                mainWindowState.placement = WindowPlacement.Floating
                                mainWindowState.size = restoredSize
                                with(density) {
                                    mainWindowState.position = WindowPosition(
                                        targetXPx.toDp(),
                                        targetYPx.toDp()
                                    )
                                }
                            },
                            restoredWidthPx = restoredWidthPx,
                            cornerRadius = activeCornerRadius
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        App(
                            onClose = ::exitApplication,
                            onMinimize = { window.isMinimized = true },
                            onHide = { window.isVisible = false },
                            onGameLaunched = { result -> consoleLaunchResult = result },
                            onGameExited = {
                            }
                        )
                    }
                }
            }
        }
    }

    consoleLaunchResult?.let { result ->
        Window(
            onCloseRequest = { consoleLaunchResult = null },
            title = "ВСПМ 5 — Консоль",
            state = androidx.compose.ui.window.WindowState(width = 800.dp, height = 500.dp),
            undecorated = decoratorEnabled,
            icon = painterResource("logo/icon-128.png"),
            transparent = useRealTransparency
        ) {
            val consoleCornerRadius = if (decoratorEnabled && useRealTransparency) GlassConfig.CornerRadius else 0.dp

            Box(modifier = Modifier.fillMaxSize()) {
                if (!useRealTransparency) {
                    AnimatedBackground(blurRadius = 18.dp)
                }

                Box(
                    modifier = if (decoratorEnabled) {
                        Modifier.fillMaxSize().windowFrame(cornerRadius = consoleCornerRadius)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (decoratorEnabled) {
                            CustomTitleBar(
                                title = "ВСПМ 5 — Консоль",
                                onMinimize = { window.isMinimized = true },
                                onClose = { consoleLaunchResult = null }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            ConsoleWindow(outputFlow = result.outputFlow)
                        }
                    }
                }
            }
        }
    }
}