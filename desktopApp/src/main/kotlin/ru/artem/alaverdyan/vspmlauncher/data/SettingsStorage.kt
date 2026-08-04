package ru.artem.alaverdyan.vspmlauncher.data

import java.io.File
import java.util.prefs.Preferences

enum class LaunchBehavior {
    MINIMIZE, HIDE, CLOSE, SHOW_CONSOLE
}

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

    fun defaultInstallDir(): String = File(System.getProperty("user.home"), ".vspmlauncher").absolutePath
    fun loadInstallDir(): String = prefs.get(KEY_INSTALL_DIR, defaultInstallDir())
    fun saveInstallDir(path: String) {
        prefs.put(KEY_INSTALL_DIR, path)
    }

    fun loadJvmArgs(): String = prefs.get(KEY_JVM_ARGS, "")
    fun saveJvmArgs(args: String) {
        prefs.put(KEY_JVM_ARGS, args)
    }

    fun loadDownloadJreFromOfficial(): Boolean = prefs.getBoolean(KEY_DOWNLOAD_JRE_FROM_OFFICIAL, false)
    fun saveDownloadJreFromOfficial(enabled: Boolean) {
        prefs.putBoolean(KEY_DOWNLOAD_JRE_FROM_OFFICIAL, enabled)
    }

    fun loadDownloadMinecraftFromOfficial(): Boolean = prefs.getBoolean(KEY_DOWNLOAD_MINECRAFT_FROM_OFFICIAL, false)
    fun saveDownloadMinecraftFromOfficial(enabled: Boolean) {
        prefs.putBoolean(KEY_DOWNLOAD_MINECRAFT_FROM_OFFICIAL, enabled)
    }

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