package ru.artem.alaverdyan.vspmlauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.SharedFlow
import ru.artem.alaverdyan.vspmlauncher.ui.components.AnimatedBackground

/**
 * Отдельное окно консоли игры. Живёт независимо от процесса игры —
 * закрытие/сворачивание игры не должно закрывать это окно (см. вызывающий
 * код: Window для консоли не должен быть дочерним/условным от launchResult).
 */

private enum class LogLevel(val color: Color) {
    DEBUG(Color(0xFF8A8F98)),
    INFO(Color(0xFFB9F6CA)),
    WARN(Color(0xFFFFD54F)),
    ERROR(Color(0xFFFF6E6E)),
    FATAL(Color(0xFFFF1744)),
    UNKNOWN(Color(0xFFE0E0E0))
}

private fun detectLevel(line: String): LogLevel = when {
    "FATAL" in line -> LogLevel.FATAL
    "ERROR" in line -> LogLevel.ERROR
    "WARN" in line -> LogLevel.WARN
    "DEBUG" in line -> LogLevel.DEBUG
    "INFO" in line -> LogLevel.INFO
    else -> LogLevel.UNKNOWN
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConsoleWindow(
    outputFlow: SharedFlow<String>
) {
    val lines = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    var autoScroll by remember { mutableStateOf(true) }
    var justCopied by remember { mutableStateOf(false) }

    LaunchedEffect(outputFlow) {
        outputFlow.collect { line ->
            lines.add(line)
            if (lines.size > 3000) lines.removeAt(0)
        }
    }

    // Автоскролл только если пользователь и так был внизу списка —
    // не выдёргиваем его, если он проскроллил вверх почитать лог.
    LaunchedEffect(lines.size) {
        if (autoScroll && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.layoutInfo.visibleItemsInfo) {
        val layoutInfo = listState.layoutInfo
        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItems = layoutInfo.totalItemsCount
        autoScroll = totalItems == 0 || lastVisible >= totalItems - 2
    }

    MaterialTheme {
        Surface(color = Color.Transparent) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedBackground(
                    imageResPath = "images/bg_placeholder.png",
                    blurRadius = 18.dp,
                    windAngleDegrees = -20f
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Консоль игры",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(lines.joinToString("\n")))
                                justCopied = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(if (justCopied) "Скопировано" else "Скопировать лог", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        SelectionContainer {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                items(lines) { line ->
                                    Text(
                                        text = line,
                                        color = detectLevel(line).color,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}