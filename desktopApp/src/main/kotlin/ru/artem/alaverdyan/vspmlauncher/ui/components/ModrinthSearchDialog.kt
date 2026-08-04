package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.artem.alaverdyan.vspmlauncher.network.LauncherConfig
import ru.artem.alaverdyan.vspmlauncher.network.ModrinthApi
import ru.artem.alaverdyan.vspmlauncher.network.ModrinthHitDto
import ru.artem.alaverdyan.vspmlauncher.network.ModrinthProjectDto
import ru.artem.alaverdyan.vspmlauncher.ui.theme.airGlass
import ru.artem.alaverdyan.vspmlauncher.update.ClientModsManager

private val CATEGORIES = listOf(
    null to "Все",
    "optimization" to "Оптимизация",
    "technology" to "Техника",
    "magic" to "Магия",
    "adventure" to "Приключения",
    "decoration" to "Декор",
    "library" to "Библиотеки",
    "utility" to "Утилиты",
    "food" to "Еда"
)

private const val MOD_LOADER = "fabric"
private const val SEARCH_DEBOUNCE_MS = 400L
private val DIALOG_REFERENCE_WIDTH = 880.dp
private val DIALOG_MIN_WIDTH = 360.dp
private val DIALOG_MIN_HEIGHT = 420.dp
private val COMPACT_LAYOUT_THRESHOLD = 620.dp
private val SIDEBAR_WIDTH = 260.dp
private val ErrorBg = Color(0xFFFF5252).copy(alpha = 0.18f)
private val ErrorIcon = Color(0xFFFF8A80)
private val ErrorText = Color(0xFFFFCDD2)
private val AccentGreen = Color(0xFF4CD97B)

@Composable
fun ModrinthSearchDialog(onDismiss: () -> Unit, onInstalled: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<ModrinthHitDto>>(emptyList()) }
    var selectedHit by remember { mutableStateOf<ModrinthHitDto?>(null) }
    var selectedDetails by remember { mutableStateOf<ModrinthProjectDto?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var justInstalled by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var installError by remember { mutableStateOf<String?>(null) }
    var hasSearchedOnce by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var detailsJob by remember { mutableStateOf<Job?>(null) }
    var detailsRequestId by remember { mutableStateOf(0) }

    fun runSearch(debounce: Boolean = false) {
        searchJob?.cancel()
        searchJob = scope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            isSearching = true
            searchError = null
            val response = ModrinthApi.search(query, selectedCategory)
            hasSearchedOnce = true
            if (response == null) {
                searchError = "Modrinth недоступен — проверьте сеть или добавьте мод из файла."
                results = emptyList()
            } else {
                results = response.hits
            }
            isSearching = false
        }
    }

    fun selectHit(hit: ModrinthHitDto) {
        selectedHit = hit
        selectedDetails = null
        installError = null
        justInstalled = false
        val requestId = ++detailsRequestId
        detailsJob?.cancel()
        detailsJob = scope.launch {
            isLoadingDetails = true
            val details = ModrinthApi.projectDetails(hit.project_id)
            if (requestId == detailsRequestId) {
                selectedDetails = details
                isLoadingDetails = false
            }
        }
    }

    fun installSelected(hit: ModrinthHitDto) {
        scope.launch {
            isInstalling = true
            installError = null
            val versions = ModrinthApi.versionsFor(
                hit.project_id,
                LauncherConfig.MINECRAFT_VERSION,
                MOD_LOADER
            )
            val file = versions?.firstOrNull()?.files?.firstOrNull { it.primary }
                ?: versions?.firstOrNull()?.files?.firstOrNull()
            if (file != null) {
                ClientModsManager.addFromModrinth(file.url, file.filename)
                justInstalled = true
                isInstalling = false
                delay(500)
                onInstalled()
            } else {
                installError =
                    "Нет сборки под ${LauncherConfig.MINECRAFT_VERSION} / $MOD_LOADER для «${hit.title}»"
                isInstalling = false
            }
        }
    }

    LaunchedEffect(Unit) { runSearch() }
    LaunchedEffect(query) {
        if (hasSearchedOnce) runSearch(debounce = true)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val dialogWidth = (maxWidth * 0.88f).coerceAtLeast(DIALOG_MIN_WIDTH.coerceAtMost(maxWidth))
            val dialogHeight = (maxHeight * 0.85f).coerceAtLeast(DIALOG_MIN_HEIGHT.coerceAtMost(maxHeight))
            val scale = (dialogWidth / DIALOG_REFERENCE_WIDTH).coerceIn(0.55f, 1.6f)
            val isCompact = dialogWidth < COMPACT_LAYOUT_THRESHOLD

            GlassPanel(modifier = Modifier.width(dialogWidth).height(dialogHeight)) {
                if (isCompact) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp * scale)
                    ) {
                        Box(modifier = Modifier.weight(0.55f).fillMaxWidth()) {
                            SearchListColumn(
                                query = query,
                                onQueryChange = { query = it },
                                onSearch = { runSearch() },
                                isSearching = isSearching,
                                selectedCategory = selectedCategory,
                                onCategorySelect = { value ->
                                    if (selectedCategory != value) {
                                        selectedCategory = value
                                        runSearch()
                                    }
                                },
                                searchError = searchError,
                                results = results,
                                hasSearchedOnce = hasSearchedOnce,
                                selectedHit = selectedHit,
                                onSelectHit = { selectHit(it) },
                                onDismiss = onDismiss,
                                scale = scale
                            )
                        }
                        Spacer(Modifier.height(10.dp * scale))
                        Box(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            DetailsColumn(
                                selectedHit = selectedHit,
                                selectedDetails = selectedDetails,
                                isLoadingDetails = isLoadingDetails,
                                isInstalling = isInstalling,
                                justInstalled = justInstalled,
                                installError = installError,
                                scale = scale,
                                onInstallClick = { hit -> installSelected(hit) }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp * scale)
                    ) {
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            SearchListColumn(
                                query = query,
                                onQueryChange = { query = it },
                                onSearch = { runSearch() },
                                isSearching = isSearching,
                                selectedCategory = selectedCategory,
                                onCategorySelect = { value ->
                                    if (selectedCategory != value) {
                                        selectedCategory = value
                                        runSearch()
                                    }
                                },
                                searchError = searchError,
                                results = results,
                                hasSearchedOnce = hasSearchedOnce,
                                selectedHit = selectedHit,
                                onSelectHit = { selectHit(it) },
                                onDismiss = onDismiss,
                                scale = scale
                            )
                        }

                        Spacer(Modifier.width(16.dp * scale))

                        GlassPanel(
                            modifier = Modifier
                                .width((dialogWidth * 0.38f).coerceIn(SIDEBAR_WIDTH, 480.dp))
                                .fillMaxHeight(),
                            cornerRadius = 12.dp * scale
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp * scale)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                DetailsColumn(
                                    selectedHit = selectedHit,
                                    selectedDetails = selectedDetails,
                                    isLoadingDetails = isLoadingDetails,
                                    isInstalling = isInstalling,
                                    justInstalled = justInstalled,
                                    installError = installError,
                                    scale = scale,
                                    onInstallClick = { hit -> installSelected(hit) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchListColumn(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    searchError: String?,
    results: List<ModrinthHitDto>,
    hasSearchedOnce: Boolean,
    selectedHit: ModrinthHitDto?,
    onSelectHit: (ModrinthHitDto) -> Unit,
    onDismiss: () -> Unit,
    scale: Float
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Поиск модов на Modrinth",
                color = Color.White,
                fontSize = (16 * scale).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            Box(
                modifier = Modifier
                    .size(32.dp * scale)
                    .airGlass(cornerRadius = 16.dp * scale)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp * scale)
                )
            }
        }
        Spacer(Modifier.height(12.dp * scale))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Название мода...", color = Color.White.copy(alpha = 0.55f)) },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    focusedBorderColor = Color.White.copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    cursorColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp * scale))
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp * scale).padding(end = 8.dp * scale),
                    strokeWidth = 2.dp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            GlassButton(
                text = "Найти",
                onClick = onSearch,
                scale = scale,
                enabled = !isSearching
            )
        }

        Spacer(Modifier.height(10.dp * scale))
        val categoryRows = remember { CATEGORIES.chunked(5) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp * scale)) {
            categoryRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp * scale)) {
                    row.forEach { (value, label) ->
                        GlassButton(
                            text = label,
                            selected = selectedCategory == value,
                            scale = scale * 0.85f,
                            onClick = { onCategorySelect(value) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp * scale))

        searchError?.let { ErrorBanner(it, scale) }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .padding(top = if (searchError != null) 8.dp * scale else 0.dp)
        ) {
            when {
                isSearching && results.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGreen)
                    }
                }

                !isSearching && results.isEmpty() && searchError == null && hasSearchedOnce -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Ничего не найдено",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = (13 * scale).sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp * scale),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(results, key = { it.project_id }) { hit ->
                            val isSelected = selectedHit?.project_id == hit.project_id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color.White.copy(alpha = if (isSelected) 0.14f else 0.06f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectHit(hit) }
                                    .padding(10.dp * scale),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ModIcon(hit.icon_url, size = 40.dp * scale)
                                Spacer(Modifier.width(10.dp * scale))
                                Column {
                                    Text(hit.title, color = Color.White, fontSize = (14 * scale).sp)
                                    Text(
                                        hit.description,
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = (11 * scale).sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsColumn(
    selectedHit: ModrinthHitDto?,
    selectedDetails: ModrinthProjectDto?,
    isLoadingDetails: Boolean,
    isInstalling: Boolean,
    justInstalled: Boolean,
    installError: String?,
    scale: Float,
    onInstallClick: (ModrinthHitDto) -> Unit
) {
    val hit = selectedHit
    if (hit == null) {
        Text(
            "Выберите мод из списка слева",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = (12 * scale).sp
        )
        return
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModIcon(selectedDetails?.icon_url ?: hit.icon_url, size = 36.dp * scale)
            Spacer(Modifier.width(8.dp * scale))
            Text(
                hit.title,
                color = Color.White,
                fontSize = (15 * scale).sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(6.dp * scale))
        Text(
            hit.description,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = (12 * scale).sp
        )
        Spacer(Modifier.height(10.dp * scale))
        Text(
            "Загрузок: ${hit.downloads}",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = (11 * scale).sp
        )
        if (hit.categories.isNotEmpty()) {
            Text(
                "Категории: ${hit.categories.joinToString(", ")}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = (11 * scale).sp
            )
        }
        Text(
            "Клиент: ${hit.client_side} · Сервер: ${hit.server_side}",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = (11 * scale).sp
        )

        if (isLoadingDetails) {
            Spacer(Modifier.height(10.dp * scale))
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp * scale),
                strokeWidth = 2.dp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        selectedDetails?.body?.takeIf { it.isNotBlank() }?.let { body ->
            Spacer(Modifier.height(10.dp * scale))
            ParsedBodyContent(body = body, scale = scale)
        }

        installError?.let {
            Spacer(Modifier.height(10.dp * scale))
            ErrorBanner(it, scale)
        }

        Spacer(Modifier.height(14.dp * scale))
        GlassButton(
            text = when {
                justInstalled -> "Установлено ✓"
                isInstalling -> "Устанавливаю..."
                else -> "Установить"
            },
            selected = justInstalled,
            enabled = !isInstalling && !justInstalled,
            scale = scale,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onInstallClick(hit) }
        )
    }
}

@Composable
private fun ModIcon(url: String?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            KamelImage(
                resource = asyncPainterResource(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onLoading = { CircularProgressIndicator(modifier = Modifier.size(size * 0.4f), strokeWidth = 2.dp, color = Color.White.copy(alpha = 0.4f)) },
                onFailure = { Text("?", color = Color.White.copy(alpha = 0.3f)) }
            )
        } else {
            Text("?", color = Color.White.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun ErrorBanner(text: String, scale: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ErrorBg, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp * scale, vertical = 8.dp * scale),
        horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
    ) {
        Text("⚠", color = ErrorIcon, fontSize = (13 * scale).sp)
        Text(text, color = ErrorText, fontSize = (11 * scale).sp)
    }
}


private sealed class BodySegment {
    data class Text(val text: String) : BodySegment()
    data class Header(val text: String) : BodySegment()
    data class Image(val url: String) : BodySegment()
}

private val LINKED_HTML_IMAGE_REGEX = Regex(
    """\[\s*<img[^>]*\ssrc=["']([^"']+)["'][^>]*>\s*]\([^)]*\)""",
    RegexOption.IGNORE_CASE
)

private val HTML_IMAGE_REGEX = Regex("""<img[^>]*\ssrc=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
private val MARKDOWN_IMAGE_REGEX = Regex("""!\[[^\]]*]\((\S+?)(?:\s+"[^"]*")?\)""")
private val BARE_IMAGE_URL_REGEX = Regex("""https?://\S+\.(?:png|jpe?g|gif|webp)(?:\?\S*)?""", RegexOption.IGNORE_CASE)
private val MARKDOWN_LINK_REGEX = Regex("""\[([^\]]*)]\(([^)]*)\)""")

private val ALL_IMAGE_PATTERNS = listOf(
    LINKED_HTML_IMAGE_REGEX,
    HTML_IMAGE_REGEX,
    MARKDOWN_IMAGE_REGEX,
    BARE_IMAGE_URL_REGEX
)

private fun cleanMarkdownText(text: String): String {
    var result = text
    result = MARKDOWN_LINK_REGEX.replace(result) { it.groupValues[1] } // [текст](url) -> текст
    result = result.replace(Regex("""\*\*([^*]+)\*\*"""), "$1")       // **bold**
    result = result.replace(Regex("""\*([^*]+)\*"""), "$1")           // *italic*
    result = result.replace(Regex("""`([^`]+)`"""), "$1")             // `code`
    result = result.replace(Regex("""^\s*[-*+]\s+""", RegexOption.MULTILINE), "• ") // списки
    result = result.replace(Regex("""^>\s*""", RegexOption.MULTILINE), "")          // цитаты
    return result.trim()
}

private fun parseBodySegments(body: String): List<BodySegment> {
    val imageRegex = Regex(ALL_IMAGE_PATTERNS.joinToString("|") { "(?:${it.pattern})" }, RegexOption.IGNORE_CASE)
    val segments = mutableListOf<BodySegment>()
    var lastIndex = 0

    imageRegex.findAll(body).forEach { match ->
        if (match.range.first > lastIndex) {
            val rawChunk = body.substring(lastIndex, match.range.first)
            segments.addAll(splitIntoTextAndHeaders(rawChunk))
        }
        val url = match.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: match.value
        segments.add(BodySegment.Image(url))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < body.length) {
        segments.addAll(splitIntoTextAndHeaders(body.substring(lastIndex)))
    }
    return segments
}

private fun splitIntoTextAndHeaders(chunk: String): List<BodySegment> {
    val result = mutableListOf<BodySegment>()
    val paragraphs = chunk.split(Regex("\n\\s*\n"))
    for (para in paragraphs) {
        val trimmed = para.trim()
        if (trimmed.isEmpty()) continue
        val headerMatch = Regex("""^#{1,6}\s*(.+)$""").find(trimmed)
        if (headerMatch != null) {
            val headerText = cleanMarkdownText(headerMatch.groupValues[1])
            if (headerText.isNotEmpty()) result.add(BodySegment.Header(headerText))
        } else {
            val cleaned = cleanMarkdownText(trimmed)
            if (cleaned.isNotEmpty()) result.add(BodySegment.Text(cleaned))
        }
    }
    return result
}

@Composable
private fun ParsedBodyContent(body: String, scale: Float) {
    val segments = remember(body) { parseBodySegments(body).take(30) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp * scale)) {
        segments.forEach { segment ->
            when (segment) {
                is BodySegment.Header -> Text(
                    segment.text,
                    color = Color.White,
                    fontSize = (13 * scale).sp,
                    fontWeight = FontWeight.Medium
                )
                is BodySegment.Text -> Text(
                    segment.text.take(600),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = (11 * scale).sp
                )
                is BodySegment.Image -> KamelImage(
                    resource = asyncPainterResource(segment.url),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp * scale)
                        .clip(RoundedCornerShape(8.dp)),
                    onLoading = {
                        Box(
                            Modifier.fillMaxWidth().height(80.dp * scale)
                                .background(Color.White.copy(alpha = 0.06f))
                        )
                    },
                    onFailure = {
                        Box(
                            Modifier.fillMaxWidth().height(80.dp * scale)
                                .background(Color.White.copy(alpha = 0.06f))
                        )
                    }
                )
            }
        }
    }
}