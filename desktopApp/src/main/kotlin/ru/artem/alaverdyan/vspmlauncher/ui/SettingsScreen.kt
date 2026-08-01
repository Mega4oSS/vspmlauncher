package ru.artem.alaverdyan.vspmlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.artem.alaverdyan.vspmlauncher.data.LaunchBehavior
import ru.artem.alaverdyan.vspmlauncher.data.TransparencyMode
import ru.artem.alaverdyan.vspmlauncher.network.ClientModEntryDto
import ru.artem.alaverdyan.vspmlauncher.runtime.PlatformInfo
import ru.artem.alaverdyan.vspmlauncher.ui.components.AnimatedBackground
import ru.artem.alaverdyan.vspmlauncher.ui.components.DownloadProgressBar
import ru.artem.alaverdyan.vspmlauncher.ui.components.GlassButton
import ru.artem.alaverdyan.vspmlauncher.ui.components.GlassPanel
import ru.artem.alaverdyan.vspmlauncher.ui.components.ModrinthSearchDialog
import ru.artem.alaverdyan.vspmlauncher.update.ClientModsManager
import ru.artem.alaverdyan.vspmlauncher.update.DownloadProgress
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

// Референсная ширина окна, от которой считается scale — подобрана под типичное окно лаунчера.
// Ниже этой ширины шрифты уменьшаются (не сильнее FONT_SCALE_MIN), выше — растут (не сильнее FONT_SCALE_MAX).
private val FONT_SCALE_REFERENCE_WIDTH = 1100.dp
private const val FONT_SCALE_MIN = 0.9f
private const val FONT_SCALE_MAX = 1.35f

@Composable
fun SettingsScreen(
    currentNickname: String,
    onNicknameChange: (String) -> Unit,
    ramMb: Float,
    serverMods: List<ClientModEntryDto>,
    modsSyncError: String? = null,
    onRamChange: (Float) -> Unit,
    runtimeId: String,
    onRuntimeIdChange: (String) -> Unit,
    jrePath: String,
    onJrePathChange: (String) -> Unit,
    jvmArgs: String,
    downloadJreFromOfficial: Boolean,
    onDownloadJreFromOfficialChange: (Boolean) -> Unit,
    downloadMinecraftFromOfficial: Boolean,
    onDownloadMinecraftFromOfficialChange: (Boolean) -> Unit,
    onJvmArgsChange: (String) -> Unit,
    launchBehavior: LaunchBehavior,
    onLaunchBehaviorChange: (LaunchBehavior) -> Unit,
    appDecoratorEnabled: Boolean,
    onAppDecoratorEnabledChange: (Boolean) -> Unit,
    transparencyMode: TransparencyMode,
    onTransparencyModeChange: (TransparencyMode) -> Unit,
    enabledModIds: Set<String>,
    onModToggle: (String, Boolean) -> Unit,
    isVerifyingFiles: Boolean = false,
    verifyResult: String? = null,
    verifyProgress: DownloadProgress? = null,
    onVerifyFiles: () -> Unit = {},
    onDismissVerifyResult: () -> Unit = {},
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(
            imageResPath = "images/bg_placeholder.png",
            blurRadius = 18.dp,
            windAngleDegrees = -20f
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Считаем от реальной ширины окна, а не от панели — панель сама fillMaxWidth(0.6f),
            // так что она растёт вместе с окном, и шрифты должны расти вместе с ней.
            val scale = (maxWidth / FONT_SCALE_REFERENCE_WIDTH).coerceIn(FONT_SCALE_MIN, FONT_SCALE_MAX)

            GlassPanel(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.6f)
                    .fillMaxHeight(0.85f)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
                    Text(
                        "Настройки",
                        color = Color.White,
                        fontSize = (24 * scale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(20.dp))

                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        NicknameSection(currentNickname, onNicknameChange, scale)
                        RamSection(ramMb, onRamChange, scale)
                        RuntimeSection(runtimeId, onRuntimeIdChange, scale)
                        LaunchBehaviorSection(launchBehavior, onLaunchBehaviorChange, scale)
                        AppDecoratorSection(
                            enabled = appDecoratorEnabled,
                            onEnabledChange = onAppDecoratorEnabledChange,
                            transparencyMode = transparencyMode,
                            onTransparencyModeChange = onTransparencyModeChange,
                            scale = scale
                        )
                        JreSection(jrePath, onJrePathChange, scale)
                        SourcesSection(
                            downloadJreFromOfficial = downloadJreFromOfficial,
                            onDownloadJreFromOfficialChange = onDownloadJreFromOfficialChange,
                            downloadMinecraftFromOfficial = downloadMinecraftFromOfficial,
                            onDownloadMinecraftFromOfficialChange = onDownloadMinecraftFromOfficialChange,
                            scale = scale
                        )
                        JvmArgsSection(jvmArgs, onJvmArgsChange, scale)
                        MaintenanceSection(
                            isVerifying = isVerifyingFiles,
                            result = verifyResult,
                            progress = verifyProgress,
                            onVerify = onVerifyFiles,
                            onDismissResult = onDismissVerifyResult,
                            scale = scale
                        )
                        ModsSection(serverMods, enabledModIds, onModToggle, modsSyncError, scale)
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onBack) {
                            Text("Назад", color = Color.White.copy(alpha = 0.8f), fontSize = (14 * scale).sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceSection(
    isVerifying: Boolean,
    result: String?,
    progress: DownloadProgress?,
    onVerify: () -> Unit,
    onDismissResult: () -> Unit,
    scale: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Обслуживание", scale)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Сверяет сборку, mojang-файлы и Java Runtime с сервером по контрольным суммам " +
                        "и докачивает/восстанавливает всё повреждённое или отсутствующее. Изменённые " +
                        "конфиги игроком/авто-тюнингом не трогает.",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = (12 * scale).sp
            )

            GlassButton(
                text = if (isVerifying) "Проверка..." else "Проверить файлы",
                onClick = onVerify,
                enabled = !isVerifying,
                modifier = Modifier.fillMaxWidth()
            )

            if (progress != null) {
                DownloadProgressBar(
                    progress = progress,
                    scale = scale,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!result.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = result,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = (12 * scale).sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissResult,
                        modifier = Modifier.size(18.dp * scale)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModsSection(
    serverMods: List<ClientModEntryDto>,
    enabledModIds: Set<String>,
    onModToggle: (String, Boolean) -> Unit,
    modsSyncError: String?,
    scale: Float
) {
    val scope = rememberCoroutineScope()
    var localMods by remember { mutableStateOf(ClientModsManager.listInstalled()) }
    var showModrinthDialog by remember { mutableStateOf(false) }

    fun refreshLocal() { localMods = ClientModsManager.listInstalled() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Клиентские моды сервера", scale)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (serverMods.isEmpty()) {
                Text(
                    "Список пуст или сервер недоступен",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = (13 * scale).sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
            serverMods.forEach { mod ->
                ModRow(
                    name = mod.name,
                    description = mod.description,
                    checked = enabledModIds.contains(mod.id),
                    onCheckedChange = { onModToggle(mod.id, it) },
                    scale = scale
                )
            }
        }
        if (!modsSyncError.isNullOrBlank()) {
            Text(
                "Не удалось синхронизировать моды:\n$modsSyncError",
                color = Color(0xFFFF8A80),
                fontSize = (12 * scale).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFF8A80).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }

        SectionLabel("Мои моды", scale)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (localMods.isEmpty()) {
                Text(
                    "Пока ничего не добавлено",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = (13 * scale).sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
            localMods.forEach { mod ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mod.name, color = Color.White, fontSize = (15 * scale).sp)
                        Text(mod.description, color = Color.White.copy(alpha = 0.55f), fontSize = (12 * scale).sp)
                    }
                    Text(
                        "Удалить",
                        color = Color(0xFFFF8A80),
                        fontSize = (13 * scale).sp,
                        modifier = Modifier
                            .clickable { ClientModsManager.remove(mod); refreshLocal() }
                            .padding(4.dp)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton(
                text = "Добавить файл",
                scale = scale,
                onClick = {
                    scope.launch {
                        val picked = withContext(Dispatchers.IO) { pickModJarFile() }
                        if (picked != null) {
                            withContext(Dispatchers.IO) { ClientModsManager.addFromLocalFile(picked) }
                            refreshLocal()
                        }
                    }
                }
            )
            GlassButton(
                text = "Скачать с Modrinth",
                selected = true, // акцент — основное действие в этом блоке
                scale = scale,
                onClick = { showModrinthDialog = true }
            )
        }
    }

    if (showModrinthDialog) {
        ModrinthSearchDialog(
            onDismiss = { showModrinthDialog = false },
            onInstalled = { refreshLocal(); showModrinthDialog = false }
        )
    }
}

@Composable
private fun ModRow(
    name: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    scale: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4CD97B),
                uncheckedColor = Color.White.copy(alpha = 0.4f),
                checkmarkColor = Color.White
            )
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(name, color = Color.White, fontSize = (15 * scale).sp)
            Text(description, color = Color.White.copy(alpha = 0.55f), fontSize = (12 * scale).sp)
        }
    }
}

private fun pickModJarFile(): File? {
    val dialog = FileDialog(null as Frame?, "Выбрать jar мода", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".jar") }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val name = dialog.file ?: return null
    return File(dir, name)
}

// --- Источники загрузки: официальные сервера vs собственный бэкенд лаунчера ---
@Composable
private fun SourcesSection(
    downloadJreFromOfficial: Boolean,
    onDownloadJreFromOfficialChange: (Boolean) -> Unit,
    downloadMinecraftFromOfficial: Boolean,
    onDownloadMinecraftFromOfficialChange: (Boolean) -> Unit,
    scale: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Источники загрузки", scale)

        SourceToggleRow(
            title = "Java Runtime с официальных серверов",
            description = "Скачивать JRE напрямую у поставщика (Adoptium/GraalVM) вместо " +
                    "зеркала лаунчера. Может быть медленнее или быстрее, но гарантирует официальную сборку.",
            checked = downloadJreFromOfficial,
            onCheckedChange = onDownloadJreFromOfficialChange,
            scale = scale
        )

        SourceToggleRow(
            title = "Minecraft с официальных серверов Mojang",
            description = "Скачивать клиент и ассеты напрямую у Mojang вместо зеркала лаунчера. " +
                    "Может быть медленнее или быстрее в зависимости от региона.",
            checked = downloadMinecraftFromOfficial,
            onCheckedChange = onDownloadMinecraftFromOfficialChange,
            scale = scale
        )
    }
}

@Composable
private fun SourceToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    scale: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = (15 * scale).sp)
            Text(description, color = Color.White.copy(alpha = 0.55f), fontSize = (12 * scale).sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4CD97B),
                checkedTrackColor = Color(0xFF4CD97B).copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )
    }
}

// --- Поведение лаунчера при запуске игры ---
@Composable
private fun LaunchBehaviorSection(
    behavior: LaunchBehavior,
    onBehaviorChange: (LaunchBehavior) -> Unit,
    scale: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("При запуске игры", scale)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RuntimeOption(
                label = "Свернуть лаунчер",
                selected = behavior == LaunchBehavior.MINIMIZE,
                onClick = { onBehaviorChange(LaunchBehavior.MINIMIZE) },
                scale = scale,
                modifier = Modifier.fillMaxWidth()
            )
            RuntimeOption(
                label = "Скрыть лаунчер",
                selected = behavior == LaunchBehavior.HIDE,
                onClick = { onBehaviorChange(LaunchBehavior.HIDE) },
                scale = scale,
                modifier = Modifier.fillMaxWidth()
            )
            RuntimeOption(
                label = "Закрыть лаунчер",
                selected = behavior == LaunchBehavior.CLOSE,
                onClick = { onBehaviorChange(LaunchBehavior.CLOSE) },
                scale = scale,
                modifier = Modifier.fillMaxWidth()
            )
            RuntimeOption(
                label = "Открыть консоль игры",
                selected = behavior == LaunchBehavior.SHOW_CONSOLE,
                onClick = { onBehaviorChange(LaunchBehavior.SHOW_CONSOLE) },
                scale = scale,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- Оформление окна лаунчера ---
@Composable
private fun AppDecoratorSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    transparencyMode: TransparencyMode,
    onTransparencyModeChange: (TransparencyMode) -> Unit,
    scale: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Оформление окна", scale)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .clickable { onEnabledChange(!enabled) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Декоратор приложения", color = Color.White, fontSize = (15 * scale).sp)
                Text(
                    "Красивое окно лаунчера: скруглённые края и своя шапка вместо системной. " +
                            "Если окно отображается криво или мигает — выключи эту опцию, " +
                            "лаунчер откроется в обычном окне.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = (12 * scale).sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CD97B),
                    checkedTrackColor = Color(0xFF4CD97B).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.25f)
                )
            )
        }

        // Выбор реальной/имитированной прозрачности показываем только на Linux —
        // на Windows и macOS настоящая прозрачность и так работает без проблем.
        if (enabled && PlatformInfo.os == "linux") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Прозрачность окна на Linux",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = (13 * scale).sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RuntimeOption(
                        label = "Настоящая",
                        selected = transparencyMode == TransparencyMode.REAL,
                        onClick = { onTransparencyModeChange(TransparencyMode.REAL) },
                        scale = scale,
                        modifier = Modifier.weight(1f)
                    )
                    RuntimeOption(
                        label = "Имитация",
                        selected = transparencyMode == TransparencyMode.FAKE,
                        onClick = { onTransparencyModeChange(TransparencyMode.FAKE) },
                        scale = scale,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    when (transparencyMode) {
                        TransparencyMode.REAL ->
                            "Окно по-настоящему прозрачное (видно то, что позади него). " +
                                    "Работает не на всех сборках Linux — если окно выглядит сломанным, " +
                                    "переключись на «Имитация»."
                        TransparencyMode.FAKE ->
                            "Окно остаётся обычным непрозрачным прямоугольником — так стабильнее " +
                                    "работает на большинстве сборок Linux. Стеклянные панели внутри " +
                                    "лаунчера всё ещё полупрозрачные, просто просвечивают не рабочий " +
                                    "стол, а собственный фон лаунчера."
                    },
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = (12 * scale).sp
                )
            }
        }

        Text(
            "Изменения вступят в силу после перезапуска лаунчера.",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = (12 * scale).sp
        )
    }
}

@Composable
private fun RuntimeSection(runtimeId: String, onRuntimeIdChange: (String) -> Unit, scale: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Java Runtime", scale)
        Text(
            "Этот параметр влияет на то, насколько плавно и без сбоев будет работать игра. " +
                    "Если игра тормозит или вылетает — попробуй переключить на другой вариант.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = (12 * scale).sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RuntimeOption(
                label = "Стандартный (Temurin)",
                selected = runtimeId == "standard",
                onClick = { onRuntimeIdChange("standard") },
                scale = scale,
                modifier = Modifier.weight(1f)
            )
            RuntimeOption(
                label = "GraalVM",
                selected = runtimeId == "graalvm",
                onClick = { onRuntimeIdChange("graalvm") },
                scale = scale,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            when (runtimeId) {
                "graalvm" ->
                    "Может давать чуть больше FPS на мощных ПК, но запускается медленнее и ест больше памяти. " +
                            "Подойдёт, если у тебя современный компьютер и хочется выжать максимум производительности."
                else ->
                    "Надёжный вариант по умолчанию — работает стабильно почти на любом компьютере. " +
                            "Рекомендуем, если не уверен, что выбрать."
            },
            color = Color.White.copy(alpha = 0.45f),
            fontSize = (12 * scale).sp
        )
    }
}

@Composable
private fun RuntimeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                if (selected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4CD97B), unselectedColor = Color.White.copy(alpha = 0.4f))
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = (14 * scale).sp)
    }
}

@Composable
private fun SectionLabel(text: String, scale: Float) {
    Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = (14 * scale).sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun NicknameSection(
    currentNickname: String,
    onNicknameChange: (String) -> Unit,
    scale: Float
) {
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf(currentNickname) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Ник", scale)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editing) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = (15 * scale).sp),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        textColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = currentNickname,
                    color = Color.White,
                    fontSize = (15 * scale).sp,
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                )
            }

            IconButton(onClick = {
                if (editing) {
                    val trimmed = input.trim()
                    if (trimmed.isNotBlank()) onNicknameChange(trimmed)
                    editing = false
                } else {
                    input = currentNickname
                    editing = true
                }
            }) {
                Icon(
                    imageVector = if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                    contentDescription = if (editing) "Сохранить" else "Редактировать",
                    tint = if (editing) Color(0xFF4CD97B) else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp * scale)
                )
            }
        }
    }
}

@Composable
private fun RamSection(ramMb: Float, onRamChange: (Float) -> Unit, scale: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Оперативная память: ${ramMb.toInt()} МБ", scale)
        Slider(
            value = ramMb,
            onValueChange = onRamChange,
            valueRange = 1024f..16384f,
            steps = 14,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.8f),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun JreSection(jrePath: String, onJrePathChange: (String) -> Unit, scale: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Java (JRE)", scale)
        GlassTextField(
            value = jrePath,
            onValueChange = onJrePathChange,
            placeholder = "Путь к java.exe / java",
            scale = scale
        )
        // TODO: кнопка "Обзор..." с системным file picker
    }
}

@Composable
private fun JvmArgsSection(jvmArgs: String, onJvmArgsChange: (String) -> Unit, scale: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Аргументы JVM", scale)
        GlassTextField(
            value = jvmArgs,
            onValueChange = onJvmArgsChange,
            placeholder = "-Xmx4G -XX:+UseG1GC ...",
            scale = scale
        )
    }
}

@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    scale: Float
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = (14 * scale).sp),
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.4f), fontSize = (14 * scale).sp) },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.White.copy(alpha = 0.08f),
            textColor = Color.White,
            cursorColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    )
}