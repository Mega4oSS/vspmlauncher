package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import ru.artem.alaverdyan.vspmlauncher.network.LauncherApi
import ru.artem.alaverdyan.vspmlauncher.ui.theme.airGlass

/**
 * Диалог пароля для входа в AdminScreen (открывается после 6 кликов по лого).
 * Стиль — Air Glass, как весь остальной интерфейс, а не стоковый Material Surface.
 * На локе (5 неудачных попыток -> сервер уже забанил IP) лаунчер закрывается — см. onLockedOut в App.kt.
 */
@Composable
fun AdminAuthDialog(
    onDismiss: () -> Unit,
    onAuthorized: (token: String) -> Unit,
    onLockedOut: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isChecking) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .airGlass(cornerRadius = 20.dp, fillAlpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Админ-доступ", color = Color.White, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Пароль", color = Color.White.copy(alpha = 0.6f)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !isChecking,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        backgroundColor = Color.White.copy(alpha = 0.08f),
                        focusedBorderColor = Color.White.copy(alpha = 0.7f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        cursorColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.caption)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    GlassButton(text = "Отмена", onClick = onDismiss, enabled = !isChecking)
                    Spacer(Modifier.width(8.dp))
                    GlassButton(
                        text = if (isChecking) "..." else "Войти",
                        enabled = !isChecking && password.isNotBlank(),
                        onClick = {
                            isChecking = true
                            scope.launch {
                                try {
                                    val resp = LauncherApi.adminAuth(password)
                                    when {
                                        resp.success && resp.token != null -> onAuthorized(resp.token)
                                        resp.lockedUntil != null -> onLockedOut()
                                        else -> error = "Неверный пароль" +
                                                (resp.attemptsLeft?.let { left -> " — осталось попыток: $left" } ?: "")
                                    }
                                } catch (e: Exception) {
                                    error = "Нет связи с сервером"
                                } finally {
                                    isChecking = false
                                    password = ""
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}