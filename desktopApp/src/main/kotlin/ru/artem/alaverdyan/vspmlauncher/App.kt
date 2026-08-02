package ru.artem.alaverdyan.vspmlauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.artem.alaverdyan.vspmlauncher.data.LaunchBehavior
import ru.artem.alaverdyan.vspmlauncher.data.NicknameStorage
import ru.artem.alaverdyan.vspmlauncher.data.SettingsStorage
import ru.artem.alaverdyan.vspmlauncher.data.TransparencyMode
import ru.artem.alaverdyan.vspmlauncher.launch.GameLauncher
import ru.artem.alaverdyan.vspmlauncher.launch.LaunchParams
import ru.artem.alaverdyan.vspmlauncher.launch.LaunchResult
import ru.artem.alaverdyan.vspmlauncher.network.ClientModEntryDto
import ru.artem.alaverdyan.vspmlauncher.network.FileEntryDto
import ru.artem.alaverdyan.vspmlauncher.network.LauncherApi
import ru.artem.alaverdyan.vspmlauncher.network.LauncherConfig
import ru.artem.alaverdyan.vspmlauncher.network.UpdatePlanDto
import ru.artem.alaverdyan.vspmlauncher.runtime.RuntimeManager
import ru.artem.alaverdyan.vspmlauncher.runtime.RuntimeStatus
import ru.artem.alaverdyan.vspmlauncher.ui.components.AnimatedBackground
import ru.artem.alaverdyan.vspmlauncher.ui.components.AnimatedLogo
import ru.artem.alaverdyan.vspmlauncher.ui.components.ConflictDialog
import ru.artem.alaverdyan.vspmlauncher.ui.components.NewsItem
import ru.artem.alaverdyan.vspmlauncher.update.ClientModsManager
import ru.artem.alaverdyan.vspmlauncher.update.ConflictInfo
import ru.artem.alaverdyan.vspmlauncher.update.Downloader
import ru.artem.alaverdyan.vspmlauncher.update.DownloadProgress
import ru.artem.alaverdyan.vspmlauncher.update.FileIntegrityVerifier
import ru.artem.alaverdyan.vspmlauncher.update.ProgressPhase
import ru.artem.alaverdyan.vspmlauncher.update.UpdatePlanExecutor
import ru.artem.alaverdyan.vspmlauncher.update.VersionStorage
import ru.artem.alaverdyan.vspmlauncher.ui.components.AdminAuthDialog
import kotlin.system.exitProcess
import java.io.File

private const val POLL_INTERVAL_MS = 60_000L
private const val LAUNCH_CHECK_THROTTLE_MS = 60 * 60 * 1000L
private val screenOrder: Map<Screen, Int> = mapOf(
    Screen.Onboarding to 0,
    Screen.Main to 1,
    Screen.Settings to 2,
    Screen.Admin to 3
)
private const val LOGO_CLICKS_TO_OPEN_ADMIN = 6
private const val LOGO_CLICK_WINDOW_MS = 3_000L

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun App(
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onHide: () -> Unit = {}, // ← полностью скрыть окно (LaunchBehavior.HIDE)
    onGameLaunched: (LaunchResult) -> Unit = {},
    onGameExited: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var lastCheckedAt by remember { mutableStateOf(0L) }
    var nickname by remember { mutableStateOf(NicknameStorage.load()) }
    var screen by remember {
        mutableStateOf(if (nickname == null) Screen.Onboarding else Screen.Main)
    }

    var downloadJreFromOfficial by remember { mutableStateOf(SettingsStorage.loadDownloadJreFromOfficial()) }
    var downloadMinecraftFromOfficial by remember { mutableStateOf(SettingsStorage.loadDownloadMinecraftFromOfficial()) }
    var isLoading by remember { mutableStateOf(true) }
    var news by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var isMaintenance by remember { mutableStateOf(false) }
    var buildVersion by remember { mutableStateOf("—") }
    var needsUpdate by remember { mutableStateOf(false) }
    var hasExistingInstall by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<DownloadProgress?>(null) }
    var launchError by remember { mutableStateOf<String?>(null) }
    var runningProcess by remember { mutableStateOf<Process?>(null) }
    val isGameRunning by remember { derivedStateOf { runningProcess != null } }

    // Фаза 3: статус бэкенда для баннера в MainScreen
    var backendStatus by remember { mutableStateOf<BackendStatus>(BackendStatus.Ok) }

    var serverMods by remember { mutableStateOf<List<ClientModEntryDto>>(emptyList()) }
    // Раньше это всегда стартовало с emptySet() и на первом ответе сервера перезатиралось
    // на "все id" — выбор пользователя никогда не переживал перезапуск лаунчера.
    val persistedModIds = remember { SettingsStorage.loadEnabledModIds() }
    var modsSeeded by remember { mutableStateOf(persistedModIds != null) }
    var selectedModIds by remember { mutableStateOf(persistedModIds ?: emptySet()) }
    // Раньше ошибки sync() тонули в printStackTrace() в никуда (у desktop-приложения без
    // консоли их физически некому было увидеть) — теперь показываем прямо в настройках.
    var modsSyncError by remember { mutableStateOf<String?>(null) }
    // true, если то, что реально скачано в game/mods, не совпадает с чекбоксами в настройках.
    // Только это (а не сам факт клика по чекбоксу) решает, показывать ли кнопку "Обновить".
    var modsNeedSync by remember { mutableStateOf(false) }

    var runtimeId by remember { mutableStateOf(SettingsStorage.loadRuntimeId()) }
    var ramMb by remember { mutableStateOf(SettingsStorage.loadRamMb()) }
    var jrePath by remember { mutableStateOf(SettingsStorage.loadJrePath()) }
    // Корневая папка установки (родитель game/). По умолчанию — путь в домашней папке
    // пользователя (SettingsStorage.defaultInstallDir()). Меняется либо в настройках, либо
    // через диалог выбора папки при первой установке (см. onLaunchOrUpdate ниже).
    var installBaseDir by remember { mutableStateOf(SettingsStorage.loadInstallDir()) }
    fun currentInstallDir(): File = File(installBaseDir, "game")
    var jvmArgs by remember { mutableStateOf(SettingsStorage.loadJvmArgs()) }
    var launchBehavior by remember { mutableStateOf(SettingsStorage.loadLaunchBehavior()) }
    var appDecoratorEnabled by remember { mutableStateOf(SettingsStorage.loadAppDecoratorEnabled()) }
    var transparencyMode by remember { mutableStateOf(SettingsStorage.loadTransparencyMode()) }

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var pendingPlans by remember { mutableStateOf<List<UpdatePlanDto>>(emptyList()) }
    // Принудительная сверка сборки/mojang-файлов/рантайма с сервером по sha256 — независимо
    // от того, что думает delta-update система (VersionStorage может считать версию актуальной,
    // а файл на диске быть битым/удалённым руками — обычное обновление этого не увидит).
    var isVerifyingFiles by remember { mutableStateOf(false) }
    var verifyResult by remember { mutableStateOf<String?>(null) }
    var conflicts by remember { mutableStateOf<List<ConflictInfo>>(emptyList()) }

    // Скрытый вход в AdminScreen: 6 кликов по лого за 3 секунды -> диалог пароля.
    var logoClickCount by remember { mutableStateOf(0) }
    var lastLogoClickAt by remember { mutableStateOf(0L) }
    var showAdminAuthDialog by remember { mutableStateOf(false) }
    var adminSessionToken by remember { mutableStateOf<String?>(null) }

    fun onLogoClicked() {
        val now = System.currentTimeMillis()
        logoClickCount = if (now - lastLogoClickAt <= LOGO_CLICK_WINDOW_MS) logoClickCount + 1 else 1
        lastLogoClickAt = now
        if (logoClickCount >= LOGO_CLICKS_TO_OPEN_ADMIN) {
            logoClickCount = 0
            showAdminAuthDialog = true
        }
    }

    // Дешёвая проверка "нужно ли обновление" — вызывается на КАЖДОЕ изменение тоглов и на
    // каждый checkForUpdates(). Никакой сети/скачивания, только сверка с локальным реестром.
    suspend fun refreshModsNeedSync() {
        modsNeedSync = ClientModsManager.hasPendingChanges(serverMods, selectedModIds)
    }

    // Реально качает/удаляет файлы — вызывается ТОЛЬКО из шага "Обновить" (onLaunchOrUpdate),
    // с видимым прогрессом через тот же downloadProgress, что и у обычного обновления сборки.
    suspend fun applyModsSync() {
        runCatching {
            ClientModsManager.sync(serverMods, selectedModIds) { progress -> downloadProgress = progress }
        }.onSuccess { result ->
            modsSyncError = if (result.hasFailures) {
                result.failed.joinToString("\n") { (name, err) -> "$name: $err" }
            } else {
                null
            }
        }.onFailure { e ->
            e.printStackTrace()
            modsSyncError = e.message ?: e.toString()
        }
        refreshModsNeedSync()
    }

    suspend fun checkForUpdates() {
        isCheckingUpdates = true
        try {
            val newsDto = LauncherApi.getNews()
            val status = LauncherApi.getStatus()
            runCatching { LauncherApi.getClientMods() }.getOrNull()?.let { dto ->
                serverMods = dto.mods
                if (!modsSeeded) {
                    // Только для НОВОГО пользователя, у которого ещё нет сохранённого выбора —
                    // если он уже сохранён (persistedModIds != null), modsSeeded = true с самого
                    // старта и сюда мы не попадаем, старый выбор не перетирается.
                    selectedModIds = dto.mods.map { it.id }.toSet()
                    SettingsStorage.saveEnabledModIds(selectedModIds)
                    modsSeeded = true
                }
                refreshModsNeedSync()
            }
            val installedVersions = withContext(Dispatchers.IO) { VersionStorage.load(currentInstallDir()) }
            val plans = LauncherApi.getUpdatePlans(installedVersions)

            news = newsDto.map { NewsItem(title = it.title, body = it.body) }
            isMaintenance = status.maintenance
            buildVersion = plans.firstOrNull { it.channel == "reallyBuild" }?.toVersion ?: "—"

            backendStatus = if (status.maintenance) {
                BackendStatus.Maintenance(status.maintenanceMessage ?: "На сервере ведутся технические работы")
            } else {
                BackendStatus.Ok
            }

            pendingPlans = plans
            // Кнопка "Обновить" загорается и от обычного обновления сборки, и от изменённых
            // тоглов клиентских модов — пользователю неважно, что именно неактуально.
            needsUpdate = plans.any { !it.upToDate } || modsNeedSync
            lastCheckedAt = System.currentTimeMillis()

            hasExistingInstall = withContext(Dispatchers.IO) {
                File(currentInstallDir(), "client.jar").exists()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            backendStatus = BackendStatus.Unavailable("Не удалось подключиться к серверу — проверьте соединение")
        } finally {
            isCheckingUpdates = false
        }
    }

    // Полная сверка с сервером по sha256 — сборка (reallyBuild) + mojang-файлы
    // (game-<version>-common) через ЖИВОЙ манифест (LauncherApi.getManifest, не
    // getUpdatePlan — тот сверяется с VersionStorage, а не с диском), плюс рантайм
    // (принудительная перекачка/распаковка с проверкой архива по sha256).
    suspend fun verifyAllFiles() {
        isVerifyingFiles = true
        verifyResult = null
        try {
            var totalChecked = 0
            var totalRepaired = 0
            val failures = mutableListOf<String>()

            for (channel in LauncherConfig.GAME_CHANNELS) {
                val manifest = runCatching { LauncherApi.getManifest(channel) }.getOrNull()
                if (manifest == null) {
                    failures += "$channel: манифест недоступен"
                    continue
                }

                totalChecked += manifest.files.size
                val broken = FileIntegrityVerifier.findBroken(currentInstallDir(), manifest.files) { checked, total ->
                    downloadProgress = DownloadProgress(
                        phase = ProgressPhase.VERIFYING,
                        currentFile = channel,
                        fileIndex = checked,
                        totalFiles = total,
                        downloadedBytes = 0,
                        totalBytes = 0,
                        bytesPerSecond = 0
                    )
                }

                if (broken.isNotEmpty()) {
                    runCatching {
                        Downloader.downloadAll(currentInstallDir(), broken) { progress -> downloadProgress = progress }
                    }.onSuccess {
                        totalRepaired += broken.size
                    }.onFailure { e ->
                        failures += "$channel: ${e.message ?: e.toString()}"
                    }
                }
            }

            runCatching {
                val status = RuntimeManager.ensureInstalled(runtimeId, forceReinstall = true) { progress ->
                    downloadProgress = progress
                }
                if (status is RuntimeStatus.Missing) {
                    failures += "Java Runtime: ${status.reason}"
                }
            }.onFailure { e ->
                failures += "Java Runtime: ${e.message ?: e.toString()}"
            }

            downloadProgress = null
            verifyResult = if (failures.isEmpty()) {
                "Проверка завершена. Файлов проверено: $totalChecked, исправлено: $totalRepaired."
            } else {
                "Проверка завершена с ошибками (проверено: $totalChecked, исправлено: $totalRepaired):\n" +
                        failures.joinToString("\n")
            }

            checkForUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
            downloadProgress = null
            verifyResult = "Не удалось выполнить проверку: ${e.message ?: e.toString()}"
        } finally {
            isVerifyingFiles = false
        }
    }

    fun launchGameAndWatch(javaBinary: File) {
        scope.launch {
            val launchResult: LaunchResult = withContext(Dispatchers.IO) {
                GameLauncher.launch(
                    LaunchParams(
                        installDir = currentInstallDir(),
                        javaBinary = javaBinary,
                        ramMb = ramMb.toInt(),
                        nickname = nickname ?: "Player",
                        extraJvmArgs = jvmArgs
                    )
                )
            }
            runningProcess = launchResult.process

            when (launchBehavior) {
                LaunchBehavior.MINIMIZE -> onMinimize()
                LaunchBehavior.HIDE -> onHide()
                LaunchBehavior.CLOSE -> onClose()
                LaunchBehavior.SHOW_CONSOLE -> onGameLaunched(launchResult)
            }

            val exitCode = withContext(Dispatchers.IO) {
                launchResult.process.waitFor()
            }

            runningProcess = null
            onGameExited()

            if (exitCode != 0) {
                val tail = withContext(Dispatchers.IO) {
                    launchResult.logFile.readLines().takeLast(20).joinToString("\n")
                }
                launchError = "Игра упала (код $exitCode):\n$tail"
            }
        }
    }

    LaunchedEffect(Unit) {
        currentInstallDir().mkdirs()
        checkForUpdates()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(POLL_INTERVAL_MS)
            if (!isBusy) {
                checkForUpdates()
            }
        }
    }

    // При каждом (пере)заходе в настройки — переподгружаем список клиентских модов с сервера,
    // а не полагаемся на то, что осталось в памяти с последнего checkForUpdates().
    LaunchedEffect(screen) {
        if (screen == Screen.Settings) {
            runCatching { LauncherApi.getClientMods() }.getOrNull()?.let { dto ->
                serverMods = dto.mods
                refreshModsNeedSync()
                needsUpdate = pendingPlans.any { !it.upToDate } || modsNeedSync
            }
        }
    }

    MaterialTheme {
        Surface(color = Color.Transparent) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedBackground(
                        imageResPath = "images/bg_placeholder.png",
                        blurRadius = 6.dp,
                        windAngleDegrees = -20f
                    )

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AnimatedLogo(
                            imageResPath = "images/logo.png",
                            modifier = Modifier
                                .width(180.dp)
                                .aspectRatio(1.9f)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        CircularProgressIndicator(color = Color.White)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Загрузка лаунчера...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
                return@Surface
            }

            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    // Направление "вглубь" (Onboarding → Main → Settings) или "назад"
                    // определяем по порядку объявления в enum Screen — доп. флаг не нужен.
                    val goingDeeper = (screenOrder[targetState] ?: 0) > (screenOrder[initialState] ?: 0)
                    val dir = if (goingDeeper) 1 else -1

                    val enter = fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(
                                animationSpec = tween(420, easing = FastOutSlowInEasing)
                            ) { fullWidth -> dir * fullWidth / 3 } +
                            scaleIn(
                                animationSpec = tween(420, easing = FastOutSlowInEasing),
                                initialScale = 0.94f
                            )

                    val exit = fadeOut(tween(260, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(
                                animationSpec = tween(360, easing = FastOutSlowInEasing)
                            ) { fullWidth -> -dir * fullWidth / 4 } +
                            scaleOut(
                                animationSpec = tween(360, easing = FastOutSlowInEasing),
                                targetScale = 1.05f
                            )

                    enter.togetherWith(exit)
                },
                modifier = Modifier.fillMaxSize()
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.Onboarding -> NicknameSetupScreen(
                        onConfirm = { entered ->
                            NicknameStorage.save(entered)
                            nickname = entered
                            screen = Screen.Main
                        }
                    )

                    Screen.Main -> MainScreen(
                        buildVersion = buildVersion,
                        needsUpdate = needsUpdate,
                        hasExistingInstall = hasExistingInstall,
                        isGameRunning = isGameRunning,
                        isMaintenance = isMaintenance,
                        news = news,
                        isCheckingUpdates = isCheckingUpdates,
                        isBusy = isBusy,
                        launchError = launchError,
                        backendStatus = backendStatus,
                        onDismissLaunchError = { launchError = null },
                        downloadProgress = downloadProgress,
                        onCloseGame = {
                            runningProcess?.destroy()
                        },
                        onLaunchOrUpdate = {
                            scope.launch {
                                isBusy = true
                                launchError = null
                                try {
                                    if (needsUpdate) {
                                        // Первая установка (папки игры ещё нет) — сперва спрашиваем,
                                        // куда ставить, предлагая текущий настроенный путь (по умолчанию —
                                        // путь в домашней папке пользователя). Если человек передумал —
                                        // отменяем клик, ничего не качаем.
                                        if (!hasExistingInstall) {
                                            val chosen = withContext(Dispatchers.IO) {
                                                pickInstallDirectory(File(installBaseDir))
                                            }
                                            if (chosen == null) {
                                                return@launch
                                            }
                                            installBaseDir = chosen.absolutePath
                                            SettingsStorage.saveInstallDir(installBaseDir)
                                            currentInstallDir().mkdirs()
                                        }

                                        val allConflicts = mutableListOf<ConflictInfo>()
                                        pendingPlans.filter { !it.upToDate }.forEach { plan ->
                                            val planConflicts = UpdatePlanExecutor.apply(currentInstallDir(), plan) { progress ->
                                                downloadProgress = progress
                                            }
                                            allConflicts += planConflicts
                                        }
                                        // Тот же клик "Обновить" применяет и план по клиентским
                                        // модам (докачка включённых / удаление выключенных) —
                                        // тем же индикатором прогресса, что и обычные файлы сборки.
                                        if (modsNeedSync) {
                                            applyModsSync()
                                        }
                                        downloadProgress = null
                                        conflicts = allConflicts

                                        checkForUpdates()
                                    } else {
                                        val sinceLastCheck = System.currentTimeMillis() - lastCheckedAt
                                        if (sinceLastCheck >= LAUNCH_CHECK_THROTTLE_MS) {
                                            checkForUpdates()
                                        }

                                        if (!needsUpdate) {
                                            val status = RuntimeManager.ensureInstalled(runtimeId) { progress ->
                                                downloadProgress = progress
                                            }

                                            when (status) {
                                                is RuntimeStatus.Installed -> {
                                                    launchGameAndWatch(status.javaBinary)
                                                }
                                                is RuntimeStatus.Missing -> {
                                                    launchError = "Java Runtime не найден для текущей платформы/варианта ($runtimeId): ${status.reason}"
                                                }
                                            }
                                        }
                                    }
                                } finally {
                                    isBusy = false
                                }
                            }
                        },
                        onOpenSettings = { screen = Screen.Settings },
                        onLogoClick = { onLogoClicked() },
                        onClose = onClose,
                        onMinimize = onMinimize
                    )

                    Screen.Admin -> adminSessionToken?.let { token ->
                        AdminScreen(sessionToken = token, onBack = { screen = Screen.Main })
                    } ?: run { screen = Screen.Main }

                    Screen.Settings -> SettingsScreen(
                        currentNickname = nickname ?: "",
                        onNicknameChange = { updated ->
                            NicknameStorage.save(updated)
                            nickname = updated
                        },
                        ramMb = ramMb,
                        onRamChange = { updated ->
                            ramMb = updated
                            SettingsStorage.saveRamMb(updated)
                        },
                        runtimeId = runtimeId,
                        onRuntimeIdChange = { id ->
                            runtimeId = id
                            SettingsStorage.saveRuntimeId(id)
                        },
                        jrePath = jrePath,
                        onJrePathChange = { updated ->
                            jrePath = updated
                            SettingsStorage.saveJrePath(updated)
                        },
                        installDir = installBaseDir,
                        hasExistingInstall = hasExistingInstall,
                        onInstallDirChange = { updated ->
                            installBaseDir = updated
                            SettingsStorage.saveInstallDir(updated)
                        },
                        jvmArgs = jvmArgs,
                        onJvmArgsChange = { updated ->
                            jvmArgs = updated
                            SettingsStorage.saveJvmArgs(updated)
                        },
                        launchBehavior = launchBehavior,
                        onLaunchBehaviorChange = { updated ->
                            launchBehavior = updated
                            SettingsStorage.saveLaunchBehavior(updated)
                        },
                        appDecoratorEnabled = appDecoratorEnabled,
                        onAppDecoratorEnabledChange = { updated ->
                            appDecoratorEnabled = updated
                            SettingsStorage.saveAppDecoratorEnabled(updated)
                        },
                        transparencyMode = transparencyMode,
                        onTransparencyModeChange = { updated ->
                            transparencyMode = updated
                            SettingsStorage.saveTransparencyMode(updated)
                        },
                        enabledModIds = selectedModIds,
                        onModToggle = { id, checked ->
                            selectedModIds = if (checked) selectedModIds + id else selectedModIds - id
                            SettingsStorage.saveEnabledModIds(selectedModIds)
                            // Только план — ни скачивания, ни удаления тут. Реально применится
                            // по клику "Обновить" на главном экране, с прогрессом.
                            scope.launch {
                                refreshModsNeedSync()
                                needsUpdate = pendingPlans.any { !it.upToDate } || modsNeedSync
                            }
                        },
                        downloadJreFromOfficial = downloadJreFromOfficial,
                        onDownloadJreFromOfficialChange = { updated ->
                            downloadJreFromOfficial = updated
                            SettingsStorage.saveDownloadJreFromOfficial(updated)
                        },
                        downloadMinecraftFromOfficial = downloadMinecraftFromOfficial,
                        onDownloadMinecraftFromOfficialChange = { updated ->
                            downloadMinecraftFromOfficial = updated
                            SettingsStorage.saveDownloadMinecraftFromOfficial(updated)
                        },
                        serverMods = serverMods,
                        modsSyncError = modsSyncError,
                        isVerifyingFiles = isVerifyingFiles,
                        verifyResult = verifyResult,
                        verifyProgress = if (isVerifyingFiles) downloadProgress else null,
                        onVerifyFiles = { scope.launch { verifyAllFiles() } },
                        onDismissVerifyResult = { verifyResult = null },
                        onBack = { screen = Screen.Main }
                    )
                }
            }

            ConflictDialog(
                conflicts = conflicts,
                onDismiss = { conflicts = emptyList() }
            )

            if (showAdminAuthDialog) {
                AdminAuthDialog(
                    onDismiss = { showAdminAuthDialog = false },
                    onAuthorized = { token ->
                        adminSessionToken = token
                        showAdminAuthDialog = false
                        screen = Screen.Admin
                    },
                    onLockedOut = {
                        // Сервер уже забанил IP на 30 минут после 5 неудачных попыток — лаунчеру тут делать нечего.
                        exitProcess(0)
                    }
                )
            }
        }
    }
}