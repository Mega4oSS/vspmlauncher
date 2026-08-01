package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NicknameField(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = nickname,
        onValueChange = onNicknameChange,
        singleLine = true,
        placeholder = { Text("Введите ник", color = Color.White.copy(alpha = 0.55f)) },
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.White,
            backgroundColor = Color.White.copy(alpha = 0.08f),
            focusedBorderColor = Color.White.copy(alpha = 0.7f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
            cursorColor = Color.White
        ),
        modifier = modifier
    )
}

/**
 * Умная кнопка запуска.
 * @param needsUpdate true — версия/моды не совпадают с актуальными, кнопка становится "Обновить"/"Установить"
 * @param hasExistingInstall false — игра ещё ни разу не скачивалась (нет папок) → кнопка "Установить" вместо "Обновить"
 * @param isRunning true — процесс игры сейчас запущен → кнопка "Закрыть", клик завершает процесс
 * @param isMaintenance true — на сервере тех.работы: кнопка остаётся кликабельной, но над ней предупреждение
 */
@Composable
fun SmartLaunchButton(
    needsUpdate: Boolean,
    hasExistingInstall: Boolean = true,
    isRunning: Boolean = false,
    isMaintenance: Boolean = false,
    isBusy: Boolean,
    isCheckingUpdates: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    Column(modifier = modifier) {
        AnimatedVisibility(visible = isMaintenance) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp * scale)
                    .background(Color(0xFFFFA000).copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp * scale, vertical = 8.dp * scale),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
            ) {
                Text("⚠", color = Color(0xFFFFC947), fontSize = (14 * scale).sp)
                Text(
                    text = "На сервере ведутся технические работы",
                    color = Color(0xFFFFE0A3),
                    fontSize = (12 * scale).sp
                )
            }
        }

        val label = when {
            isRunning -> "Закрыть"
            needsUpdate && !hasExistingInstall -> "Установить"
            needsUpdate -> "Обновить"
            else -> "Запуск"
        }

        val backgroundColor = when {
            isRunning -> Color(0xFFFF5252)   // красная — действие "закрыть"
            needsUpdate -> Color(0xFF3E9CFF) // синяя — обновить/установить
            else -> Color(0xFF4CD97B)        // зелёная — запуск
        }

        Button(
            onClick = onClick,
            // во время isRunning кнопка НЕ дизейблится — иначе закрыть игру будет нельзя
            enabled = isRunning || (!isBusy && !isCheckingUpdates),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = backgroundColor),
            modifier = Modifier.fillMaxWidth().height(52.dp * scale)
        ) {
            if (!isRunning && (isBusy || isCheckingUpdates)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp * scale),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp * scale))
                Text(
                    text = if (isCheckingUpdates) "Проверка обновлений" else "",
                    color = Color.White,
                    fontSize = (14 * scale).sp
                )
            } else {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = (16 * scale).sp
                )
            }
        }
    }
}