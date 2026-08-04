package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DirectoryPickerDialog(
    initialDir: File,
    title: String = "Выбрать папку",
    scale: Float = 1f,
    onDismiss: () -> Unit,
    onConfirm: (File) -> Unit
) {
    var currentDir by remember {
        mutableStateOf<File?>(
            initialDir.takeIf { it.exists() && it.isDirectory }
                ?: initialDir.parentFile
                ?: File(System.getProperty("user.home"))
        )
    }
    var manualPath by remember { mutableStateOf(currentDir?.absolutePath ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var subDirs by remember { mutableStateOf<List<File>>(emptyList()) }

    fun navigateTo(dir: File?) {
        currentDir = dir
        manualPath = dir?.absolutePath ?: ""
        errorMsg = null
    }

    LaunchedEffect(currentDir) {
        val dir = currentDir
        if (dir == null) {
            subDirs = File.listRoots().toList()
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        subDirs = emptyList()
        val result = withContext(Dispatchers.IO) {
            try {
                dir.listFiles { f -> f.isDirectory && !f.isHidden }
                    ?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                    ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
        subDirs = result
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(modifier = Modifier.width(560.dp * scale).height(480.dp * scale)) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp * scale)) {
                Text(title, color = Color.White, fontSize = (16 * scale).sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp * scale))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = manualPath,
                        onValueChange = { manualPath = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = (13 * scale).sp),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            textColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp * scale))
                    TextButton(onClick = {
                        val f = File(manualPath)
                        when {
                            f.isDirectory -> navigateTo(f)
                            f.isFile -> f.parentFile?.let { navigateTo(it) }
                            else -> errorMsg = "Такой папки не существует"
                        }
                    }) {
                        Text("Перейти", color = Color.White.copy(alpha = 0.8f), fontSize = (13 * scale).sp)
                    }
                }

                errorMsg?.let {
                    Spacer(Modifier.height(4.dp * scale))
                    Text(it, color = Color(0xFFFF8A80), fontSize = (12 * scale).sp)
                }

                Spacer(Modifier.height(12.dp * scale))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { navigateTo(currentDir?.parentFile) },
                        enabled = currentDir != null,
                        modifier = Modifier.size(32.dp * scale)
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.width(4.dp * scale))
                    Text(
                        currentDir?.absolutePath ?: "Компьютер",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = (12 * scale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp * scale))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp * scale))
                ) {
                    when {
                        isLoading -> CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.6f),
                            strokeWidth = 2.dp,
                            modifier = Modifier.align(Alignment.Center).size(28.dp * scale)
                        )
                        subDirs.isEmpty() -> Text(
                            if (currentDir == null) "Нет доступных дисков" else "Нет вложенных папок",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = (13 * scale).sp,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp * scale)
                        )
                        else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(4.dp * scale)) {
                            items(items = subDirs, key = { it.absolutePath }) { dir ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navigateTo(dir) }
                                        .padding(horizontal = 10.dp * scale, vertical = 8.dp * scale),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (currentDir == null) Icons.Filled.Storage else Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = if (currentDir == null) Color(0xFF90CAF9) else Color(0xFFFFD54F),
                                        modifier = Modifier.size(18.dp * scale)
                                    )
                                    Spacer(Modifier.width(8.dp * scale))
                                    Text(
                                        if (currentDir == null) dir.absolutePath.removeSuffix(File.separator).ifEmpty { dir.absolutePath } else dir.name,
                                        color = Color.White,
                                        fontSize = (13 * scale).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp * scale))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = Color.White.copy(alpha = 0.7f), fontSize = (13 * scale).sp)
                    }
                    Spacer(Modifier.width(8.dp * scale))
                    GlassButton(
                        text = "Выбрать эту папку",
                        selected = true,
                        onClick = { currentDir?.let { onConfirm(it) } }
                    )
                }
            }
        }
    }
}