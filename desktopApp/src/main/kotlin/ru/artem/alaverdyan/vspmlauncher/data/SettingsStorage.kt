package ru.artem.alaverdyan.vspmlauncher.data

import java.io.File
import java.util.prefs.Preferences

/**
 * Что делает лаунчер при успешном старте игры.
 * MINIMIZE — сворачивает главное окно (по умолчанию)
 * CLOSE — закрывает лаунчер полностью
 * SHOW_CONSOLE — держит главное окно как есть и дополнительно открывает окно консоли
 */
enum class LaunchBehavior {
    MINIMIZE, HIDE, CLOSE, SHOW_CONSOLE
}

/**
 * Способ отрисовки прозрачности окна. Актуально только на Linux —
 * на Windows/macOS всегда используется REAL, т.к. там настоящая
 * прозрачность окна рендерится корректно "из коробки".
 *
 * REAL — настоящая прозрачность окна (видно рабочий стол/окна позади).
 *        На Linux для этого включается программный рендер Skiko,
 *        т.к. аппаратный (OpenGL) рендер неправильно считает альфа-канал окна.
 * FAKE — без настоящей прозрачности: окно остаётся полностью непрозрачным
 *        прямоугольником (без скруглённых углов), но все "стеклянные" панели
 *        внутри лаунчера по-прежнему полупрозрачные — они просвечивают
 *        не рабочий стол, а собственный фон лаунчера.
 */
enum class TransparencyMode {
    REAL, FAKE
}

object SettingsStorage {
    private val prefs = Preferences.userRoot().node("ru/artem/alaverdyan/vspmlauncher")

    private const val KEY_RUNTIME_ID = "runtime_id"
    private const val KEY_LAUNCH_BEHAVIOR = "launch_behavior"
    private const val KEY_RAM_MB = "ram_mb"
    private const val KEY_DOWNLOAD_JRE_FROM_OFFICIAL = "download_jre_from_official"
    private const val KEY_DOWNLOAD_MINECRAFT_FROM_OFFICIAL = "download_minecraft_from_official"
    private const val KEY_JRE_PATH = "jre_path"
    private const val KEY_INSTALL_DIR = "install_dir"
    private const val KEY_JVM_ARGS = "jvm_args"
    private const val KEY_APP_DECORATOR_ENABLED = "app_decorator_enabled"
    private const val KEY_TRANSPARENCY_MODE = "transparency_mode"
    private const val KEY_ENABLED_MOD_IDS = "enabled_mod_ids"

    fun loadRuntimeId(): String = prefs.get(KEY_RUNTIME_ID, "standard")
    fun saveRuntimeId(id: String) {
        prefs.put(KEY_RUNTIME_ID, id)
    }

    fun loadLaunchBehavior(): LaunchBehavior {
        val raw = prefs.get(KEY_LAUNCH_BEHAVIOR, LaunchBehavior.MINIMIZE.name)
        return runCatching { LaunchBehavior.valueOf(raw) }.getOrDefault(LaunchBehavior.MINIMIZE)
    }
    fun saveLaunchBehavior(behavior: LaunchBehavior) {
        prefs.put(KEY_LAUNCH_BEHAVIOR, behavior.name)
    }

    fun loadRamMb(): Float = prefs.getFloat(KEY_RAM_MB, 4096f)
    fun saveRamMb(value: Float) {
        prefs.putFloat(KEY_RAM_MB, value)
    }

    fun loadJrePath(): String = prefs.get(KEY_JRE_PATH, "")
    fun saveJrePath(path: String) {
        prefs.put(KEY_JRE_PATH, path)
    }

    // Корневая папка, куда лаунчер ставит игру, рантаймы Java и служебные файлы
    // (все они лежат внутри неё в подпапках game/, runtimes/ и т.д.). По умолчанию —
    // "<домашняя папка>/.vspmlauncher", как было всегда захардкожено. Пользователь может
    // сменить путь в настройках или прямо в диалоге выбора при первой установке.
    fun defaultInstallDir(): String = File(System.getProperty("user.home"), ".vspmlauncher").absolutePath
    fun loadInstallDir(): String = prefs.get(KEY_INSTALL_DIR, defaultInstallDir())
    fun saveInstallDir(path: String) {
        prefs.put(KEY_INSTALL_DIR, path)
    }

    fun loadJvmArgs(): String = prefs.get(KEY_JVM_ARGS, "")
    fun saveJvmArgs(args: String) {
        prefs.put(KEY_JVM_ARGS, args)
    }

    // Качать JRE напрямую у поставщика (Adoptium/GraalVM) вместо зеркала лаунчера.
    fun loadDownloadJreFromOfficial(): Boolean = prefs.getBoolean(KEY_DOWNLOAD_JRE_FROM_OFFICIAL, false)
    fun saveDownloadJreFromOfficial(enabled: Boolean) {
        prefs.putBoolean(KEY_DOWNLOAD_JRE_FROM_OFFICIAL, enabled)
    }

    // Качать клиент/ассеты Minecraft напрямую у Mojang вместо зеркала лаунчера.
    fun loadDownloadMinecraftFromOfficial(): Boolean = prefs.getBoolean(KEY_DOWNLOAD_MINECRAFT_FROM_OFFICIAL, false)
    fun saveDownloadMinecraftFromOfficial(enabled: Boolean) {
        prefs.putBoolean(KEY_DOWNLOAD_MINECRAFT_FROM_OFFICIAL, enabled)
    }
    // "Декоратор приложения" — своё оформление окна лаунчера (скруглённые углы,
    // кастомная шапка окна вместо системной, эффект стекла). Если у пользователя
    // из-за этого едет отрисовка окна, он может выключить декоратор — тогда
    // лаунчер откроется в обычном окне с системной рамкой, без каких-либо
    // эффектов прозрачности.
    fun loadAppDecoratorEnabled(): Boolean = prefs.getBoolean(KEY_APP_DECORATOR_ENABLED, true)
    fun saveAppDecoratorEnabled(enabled: Boolean) {
        prefs.putBoolean(KEY_APP_DECORATOR_ENABLED, enabled)
    }

    fun loadTransparencyMode(): TransparencyMode {
        val raw = prefs.get(KEY_TRANSPARENCY_MODE, TransparencyMode.REAL.name)
        return runCatching { TransparencyMode.valueOf(raw) }.getOrDefault(TransparencyMode.REAL)
    }
    fun saveTransparencyMode(mode: TransparencyMode) {
        prefs.put(KEY_TRANSPARENCY_MODE, mode.name)
    }

    // Раньше enabledModIds жил только в памяти App.kt и на каждом старте пересоздавался как
    // "все id из serverMods" — отсюда "по умолчанию всё включено, но ничего не скачано".
    // null означает "пользователь ещё ни разу не трогал список" — тогда App.kt сам решает,
    // чем засеять набор при первой загрузке serverMods. Пустой Set — осознанный "всё выключено".
    private const val MOD_IDS_NOT_SET = "\u0000"
    fun loadEnabledModIds(): Set<String>? {
        val raw = prefs.get(KEY_ENABLED_MOD_IDS, MOD_IDS_NOT_SET)
        if (raw == MOD_IDS_NOT_SET) return null
        if (raw.isEmpty()) return emptySet()
        return raw.split(",").toSet()
    }
    fun saveEnabledModIds(ids: Set<String>) {
        prefs.put(KEY_ENABLED_MOD_IDS, ids.joinToString(","))
    }
}