package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.artem.alaverdyan.vspmlauncher.update.ConflictInfo

@Composable
fun ConflictDialog(
    conflicts: List<ConflictInfo>,
    onDismiss: () -> Unit
) {
    if (conflicts.isEmpty()) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Обновление конфигов") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Часть твоих локальных настроек была перезаписана обновлением, потому что сервер поменял те же ключи:")
                conflicts.groupBy { it.path }.forEach { (path, keys) ->
                    Text(path, style = androidx.compose.material.MaterialTheme.typography.subtitle2)
                    keys.forEach { c ->
                        Text("  ${c.key}: у тебя было \"${c.localValue}\" -> стало \"${c.serverNewValue}\"")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Понятно") }
        }
    )
}