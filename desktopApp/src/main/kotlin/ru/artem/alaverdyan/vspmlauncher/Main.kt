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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.artem.alaverdyan.vspmlauncher.data.SettingsStorage
import ru.artem.alaverdyan.vspmlauncher.data.TransparencyMode
import ru.artem.alaverdyan.vspmlauncher.launch.LaunchResult
import ru.artem.alaverdyan.vspmlauncher.runtime.PlatformInfo
import ru.artem.alaverdyan.vspmlauncher.ui.App
import ru.artem.alaverdyan.vspmlauncher.ui.ConsoleWindow
import ru.artem.alaverdyan.vspmlauncher.ui.components.AnimatedBackground
import ru.artem.alaverdyan.vspmlauncher.ui.components.CustomTitleBar
import ru.artem.alaverdyan.vspmlauncher.ui.theme.GlassConfig
import ru.artem.alaverdyan.vspmlauncher.ui.theme.windowFrame

private const val WM_ENV = "_JAVA_AWT_WM_NONREPARENTING"

fun main() {
    // "Декоратор приложения" — своё оформление окна (скруглённые углы, своя
    // шапка, стеклянный эффект) вместо обычного системного окна. Настоящая
    // прозрачность (TransparencyMode.REAL) на Linux рендерится через аппаратный
    // (OpenGL) рендер Skiko с ошибками — окно остаётся видимым прямоугольником
    // вместо прозрачных углов. Программный рендер это чинит, но переключатель
    // должен быть выставлен САМЫМ первым делом в main(), до того как Skiko
    // создаст графический контекст.
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
    var consoleLaunchResult by remember { mutableStateOf<LaunchResult?>(null) }

    val mainWindowState = rememberWindowState(width = 1000.dp, height = 650.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "ВСПМ 5 — Launcher",
        state = mainWindowState,
        resizable = true,
        undecorated = decoratorEnabled,
        icon = painterResource("logo/icon_32x32.png"),
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
        // Без настоящей прозрачности (fake-режим на Linux) скругление не рисуем —
        // окно непрозрачное, обрезанные Compose'ом углы превратились бы в чёрные
        // "уголки". Поэтому в fake-режиме окно прямоугольное, "прозрачность"
        // видна только на стеклянных панелях внутри — просвечивают собственный
        // статичный фон лаунчера, а не рабочий стол.
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
                                mainWindowState.placement =
                                    if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                            },
                            isMaximized = isMaximized,
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

private fun relaunchWithWaylandFix(): Nothing {
    val info = ProcessHandle.current().info()
    val cmd = buildList {
        add(info.command().orElse("java"))
        addAll(info.arguments().orElse(emptyArray()))
    }
    val pb = ProcessBuilder(cmd).inheritIO()
    pb.environment()[WM_ENV] = "1"
    val code = runCatching { pb.start().waitFor() }.getOrDefault(1)
    kotlin.system.exitProcess(code)
}