package ru.artem.alaverdyan.vspmlauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.artem.alaverdyan.vspmlauncher.ui.components.AnimatedBackground
import ru.artem.alaverdyan.vspmlauncher.ui.components.AnimatedLogo
import ru.artem.alaverdyan.vspmlauncher.ui.components.GlassPanel
import ru.artem.alaverdyan.vspmlauncher.ui.components.NicknameField
import ru.artem.alaverdyan.vspmlauncher.ui.components.SmartLaunchButton

@Composable
fun NicknameSetupScreen(
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(
            imageResPath = "images/bg_placeholder.png",
            blurRadius = 18.dp,
            windAngleDegrees = -20f
        )

        // Логотип и панель ввода теперь в одной колонке — не могут перекрыться
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val logoSize = (maxWidth * 0.18f).coerceIn(100.dp, 160.dp)
            val panelWidth = (maxWidth * 0.4f).coerceIn(280.dp, 420.dp)

            Column(
                modifier = Modifier.fillMaxSize().padding(top = 32.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedLogo(imageResPath = "images/logo.png", modifier = Modifier.size(logoSize))

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassPanel(modifier = Modifier.width(panelWidth)) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Добро пожаловать!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Введите ник — он понадобится один раз",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )

                        NicknameField(
                            nickname = input,
                            onNicknameChange = { input = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        SmartLaunchButton(
                            needsUpdate = false,
                            isBusy = false,
                            onClick = {
                                val trimmed = input.trim()
                                if (trimmed.isNotBlank()) onConfirm(trimmed)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    }
                }
            }
        }
    }
}