package ru.artem.alaverdyan.vspmlauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.artem.alaverdyan.vspmlauncher.network.BuildDiffDto
import ru.artem.alaverdyan.vspmlauncher.network.LauncherApi
import ru.artem.alaverdyan.vspmlauncher.network.LauncherConfig
import ru.artem.alaverdyan.vspmlauncher.network.NewsItemDto
import ru.artem.alaverdyan.vspmlauncher.ui.components.AnimatedBackground
import ru.artem.alaverdyan.vspmlauncher.ui.components.GlassButton
import ru.artem.alaverdyan.vspmlauncher.ui.components.GlassPanel

private val NARROW_LAYOUT_BREAKPOINT = 640.dp

@Composable
fun AdminScreen(sessionToken: String, onBack: () -> Unit) {
    var channel by remember { mutableStateOf(LauncherConfig.GAME_CHANNELS.first()) }
    var versions by remember { mutableStateOf<List<String>>(emptyList()) }
    var newVersion by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var diff by remember { mutableStateOf<BuildDiffDto?>(null) }
    var fromVersion by remember { mutableStateOf<String?>(null) }
    var toVersion by remember { mutableStateOf<String?>(null) }
    var maintenanceEnabled by remember { mutableStateOf(false) }
    var maintenanceMessage by remember { mutableStateOf("") }
    var isMaintenanceBusy by remember { mutableStateOf(false) }
    var maintenanceStatusMessage by remember { mutableStateOf<String?>(null) }

    var newsList by remember { mutableStateOf<List<NewsItemDto>>(emptyList()) }
    var newsTitle by remember { mutableStateOf("") }
    var newsBody by remember { mutableStateOf("") }
    var newsPinned by remember { mutableStateOf(false) }
    var isNewsBusy by remember { mutableStateOf(false) }
    var newsStatusMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun reloadVersions() {
        try {
            versions = LauncherApi.adminVersions(sessionToken, channel).versions
            toVersion = versions.lastOrNull()
            fromVersion = versions.dropLast(1).lastOrNull()
            diff = null
        } catch (e: Exception) {
            statusMessage = "Сессия истекла или сервер недоступен — вернись в меню и войди заново"
        }
    }

    suspend fun reloadNews() {
        try {
            newsList = LauncherApi.getNews()
        } catch (e: Exception) {
            newsStatusMessage = "Не удалось загрузить список новостей"
        }
    }

    LaunchedEffect(channel) { reloadVersions() }
    LaunchedEffect(Unit) { reloadNews() }
    LaunchedEffect(Unit) {
        try {
            val status = LauncherApi.getStatus()
            maintenanceEnabled = status.maintenance
            maintenanceMessage = status.maintenanceMessage.orEmpty()
        } catch (_: Exception) { }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BoxWithConstraints( modifier = Modifier
        .fillMaxSize()
        .focusRequester(focusRequester)
        .focusTarget()
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                onBack()
                true
            } else false
        }
    ) {
        val scale = (maxWidth / 1000.dp).coerceIn(0.6f, 1.6f)
        val isNarrow = maxWidth < NARROW_LAYOUT_BREAKPOINT
        val edgePadding = 24.dp * scale

        AnimatedBackground(imageResPath = "images/bg_placeholder.png", blurRadius = 18.dp, windAngleDegrees = -20f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(edgePadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp * scale)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassButton(text = "← Назад", onClick = onBack, scale = scale)
                Spacer(Modifier.width(12.dp * scale))
                Text("Админ-панель сборок", fontSize = (20 * scale).sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp * scale)) {
                    Text("Канал", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp)
                    Spacer(Modifier.height(8.dp * scale))
                    ChipRow(isNarrow, scale) {
                        LauncherConfig.GAME_CHANNELS.forEach { ch ->
                            GlassButton(text = ch, selected = ch == channel, onClick = { channel = ch }, scale = scale)
                        }
                    }
                    Spacer(Modifier.height(6.dp * scale))
                    Text(
                        "Подсказка: reallyBuild — общий канал сборки, остальные — платформенные наборы (${LauncherConfig.MINECRAFT_VERSION}).",
                        color = Color.White.copy(alpha = 0.6f), fontSize = (12 * scale).sp
                    )
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp * scale)) {
                    Text("Опубликовать новую версию", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp)
                    Spacer(Modifier.height(4.dp * scale))
                    Text(
                        "Версия — semver (1.2.3), строго больше текущей (${versions.lastOrNull() ?: "ещё не публиковалась"}). " +
                                "Публикация читает файлы из data/builds/$channel на сервере в текущем виде — " +
                                "убедись, что сборка там уже полностью разложена, ПЕРЕД тем как жать кнопку.",
                        color = Color.White.copy(alpha = 0.6f), fontSize = (12 * scale).sp
                    )
                    Spacer(Modifier.height(10.dp * scale))
                    ChipRow(isNarrow, scale) {
                        GlassTextField(
                            value = newVersion,
                            onValueChange = { newVersion = it },
                            placeholder = "Новая версия",
                            enabled = !isBusy,
                            modifier = Modifier.width(160.dp * scale)
                        )
                        GlassButton(
                            text = if (isBusy) "..." else "Опубликовать",
                            enabled = !isBusy && newVersion.isNotBlank(),
                            scale = scale,
                            onClick = {
                                isBusy = true
                                scope.launch {
                                    val (ok, message) = LauncherApi.adminPublish(sessionToken, channel, newVersion)
                                    statusMessage = message
                                    if (ok) {
                                        newVersion = ""
                                        reloadVersions()
                                    }
                                    isBusy = false
                                }
                            }
                        )
                    }
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp * scale)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Технические работы",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (14 * scale).sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = maintenanceEnabled,
                            onCheckedChange = { maintenanceEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CD97B),
                                checkedTrackColor = Color(0xFF4CD97B).copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.25f)
                            )
                        )
                    }
                    if (maintenanceEnabled) {
                        Spacer(Modifier.height(8.dp * scale))
                        GlassTextField(
                            value = maintenanceMessage,
                            onValueChange = { maintenanceMessage = it },
                            placeholder = "Сообщение для игроков",
                            enabled = !isMaintenanceBusy,
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth().height(80.dp * scale)
                        )
                    }
                    Spacer(Modifier.height(8.dp * scale))
                    GlassButton(
                        text = if (isMaintenanceBusy) "..." else "Сохранить",
                        enabled = !isMaintenanceBusy,
                        scale = scale,
                        onClick = {
                            isMaintenanceBusy = true
                            scope.launch {
                                val (_, message) = LauncherApi.adminSetMaintenance(
                                    sessionToken, maintenanceEnabled, maintenanceMessage
                                )
                                maintenanceStatusMessage = message
                                isMaintenanceBusy = false
                            }
                        }
                    )
                    maintenanceStatusMessage?.let {
                        Spacer(Modifier.height(6.dp * scale))
                        Text(it, color = Color(0xFFFFC107), fontSize = (13 * scale).sp)
                    }
                }
            }

            statusMessage?.let {
                Text(it, color = Color(0xFFFFC107), fontSize = (13 * scale).sp)
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp * scale)) {
                    Text("Новости", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp)
                    Spacer(Modifier.height(4.dp * scale))
                    Text(
                        "Показываются в лаунчере на главном экране, закреплённые — сверху.",
                        color = Color.White.copy(alpha = 0.6f), fontSize = (12 * scale).sp
                    )
                    Spacer(Modifier.height(10.dp * scale))

                    GlassTextField(
                        value = newsTitle,
                        onValueChange = { newsTitle = it },
                        placeholder = "Заголовок",
                        enabled = !isNewsBusy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp * scale))
                    GlassTextField(
                        value = newsBody,
                        onValueChange = { newsBody = it },
                        placeholder = "Текст новости",
                        enabled = !isNewsBusy,
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().height(100.dp * scale)
                    )
                    Spacer(Modifier.height(8.dp * scale))
                    ChipRow(isNarrow, scale) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = newsPinned,
                                onCheckedChange = { newsPinned = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CD97B))
                            )
                            Text("Закрепить", color = Color.White, fontSize = (13 * scale).sp)
                        }
                        GlassButton(
                            text = if (isNewsBusy) "..." else "Опубликовать",
                            enabled = !isNewsBusy && newsTitle.isNotBlank(),
                            scale = scale,
                            onClick = {
                                isNewsBusy = true
                                scope.launch {
                                    val (ok, message) = LauncherApi.adminPublishNews(sessionToken, newsTitle, newsBody, newsPinned)
                                    newsStatusMessage = message
                                    if (ok) {
                                        newsTitle = ""
                                        newsBody = ""
                                        newsPinned = false
                                        reloadNews()
                                    }
                                    isNewsBusy = false
                                }
                            }
                        )
                    }

                    newsStatusMessage?.let {
                        Spacer(Modifier.height(6.dp * scale))
                        Text(it, color = Color(0xFFFFC107), fontSize = (13 * scale).sp)
                    }

                    Spacer(Modifier.height(14.dp * scale))
                    if (newsList.isEmpty()) {
                        Text("Новостей пока нет", color = Color.White.copy(alpha = 0.5f), fontSize = (13 * scale).sp)
                    }
                    newsList.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    (if (item.pinned) "📌 " else "") + item.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (13 * scale).sp
                                )
                                if (item.body.isNotBlank()) {
                                    Text(item.body, color = Color.White.copy(alpha = 0.7f), fontSize = (12 * scale).sp)
                                }
                            }
                            GlassButton(
                                text = "Удалить",
                                scale = scale,
                                onClick = {
                                    scope.launch {
                                        val (ok, message) = LauncherApi.adminDeleteNews(sessionToken, item.id)
                                        newsStatusMessage = message
                                        if (ok) reloadNews()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp * scale)) {
                    Text("Что изменилось между сборками", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp)
                    Spacer(Modifier.height(4.dp * scale))
                    Text(
                        "Подсказка: сравнение — по опубликованным версиям канала, не по сырым файлам на диске. " +
                                "Если только что опубликовал версию и не видишь её тут — нажми «Обновить».",
                        color = Color.White.copy(alpha = 0.6f), fontSize = (12 * scale).sp
                    )
                    Spacer(Modifier.height(10.dp * scale))

                    Text("От", color = Color.White.copy(alpha = 0.7f), fontSize = (12 * scale).sp)
                    Spacer(Modifier.height(4.dp * scale))
                    ChipRow(isNarrow, scale) {
                        GlassButton(text = "— (с нуля)", selected = fromVersion == null, scale = scale, onClick = { fromVersion = null })
                        versions.forEach { v -> GlassButton(text = v, selected = v == fromVersion, scale = scale, onClick = { fromVersion = v }) }
                    }

                    Spacer(Modifier.height(10.dp * scale))
                    Text("До", color = Color.White.copy(alpha = 0.7f), fontSize = (12 * scale).sp)
                    Spacer(Modifier.height(4.dp * scale))
                    ChipRow(isNarrow, scale) {
                        versions.forEach { v -> GlassButton(text = v, selected = v == toVersion, scale = scale, onClick = { toVersion = v }) }
                    }

                    Spacer(Modifier.height(12.dp * scale))
                    ChipRow(isNarrow, scale) {
                        GlassButton(
                            text = "Сравнить",
                            enabled = toVersion != null,
                            scale = scale,
                            onClick = {
                                scope.launch {
                                    try {
                                        diff = LauncherApi.adminDiff(sessionToken, channel, fromVersion, toVersion!!)
                                    } catch (e: Exception) {
                                        statusMessage = "Не удалось получить дифф"
                                    }
                                }
                            }
                        )
                        GlassButton(text = "Обновить список", scale = scale, onClick = { scope.launch { reloadVersions() } })
                    }

                    diff?.let { d -> Spacer(Modifier.height(14.dp * scale)); DiffView(d, scale) }
                }
            }
        }
    }
}

@Composable
private fun ChipRow(isNarrow: Boolean, scale: Float, content: @Composable () -> Unit) {
    if (isNarrow) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp * scale)) { content() }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp * scale),
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }
}

@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.5f)) },
        singleLine = singleLine,
        enabled = enabled,
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

@Composable
private fun DiffView(diff: BuildDiffDto, scale: Float) {
    Column {
        Text(
            "${diff.fromVersion ?: "(начало)"} → ${diff.toVersion}",
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp
        )
        Spacer(Modifier.height(8.dp * scale))

        if (diff.added.isEmpty() && diff.removed.isEmpty() && diff.changedFull.isEmpty() && diff.changedPatch.isEmpty()) {
            Text("Файлы идентичны — изменений нет", color = Color.White.copy(alpha = 0.7f), fontSize = (13 * scale).sp)
            return
        }

        DiffSection("Добавлено", diff.added.map { it.path }, Color(0xFF69F0AE), scale)
        DiffSection("Удалено", diff.removed, Color(0xFFFF8A80), scale)
        DiffSection("Заменено целиком", diff.changedFull.map { it.path }, Color(0xFFFFD54F), scale)

        if (diff.changedPatch.isNotEmpty()) {
            Text("Изменения в конфигах", color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold, fontSize = (13 * scale).sp)
            diff.changedPatch.forEach { patch ->
                Text(patch.path, color = Color.White, fontSize = (13 * scale).sp)
                patch.keyDiffs.forEach { kd ->
                    Text(
                        "   ${kd.key}: ${kd.oldValue ?: "∅"} → ${kd.newValue ?: "∅"}",
                        color = Color.White.copy(alpha = 0.7f), fontSize = (12 * scale).sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp * scale))
        }
    }
}

@Composable
private fun DiffSection(title: String, paths: List<String>, color: Color, scale: Float) {
    if (paths.isEmpty()) return
    Text("$title (${paths.size})", color = color, fontWeight = FontWeight.Bold, fontSize = (13 * scale).sp)
    paths.forEach { Text("   $it", color = Color.White.copy(alpha = 0.85f), fontSize = (12 * scale).sp) }
    Spacer(Modifier.height(8.dp * scale))
}