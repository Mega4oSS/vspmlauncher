package ru.artem.alaverdyan.vspmlauncher.data

import java.io.File

/**
 * Единая точка правды о том, куда лаунчер кладёт данные на диске.
 *
 * Раньше корень "~/.vspmlauncher" был захардкожен по отдельности в App.kt (INSTALL_DIR),
 * RuntimeManager.kt (RUNTIMES_ROOT), ClientModsManager.kt (STATE_FILE) и LocalClientMod.kt
 * (CLIENT_MODS_DIR) — теперь все они читают путь отсюда. baseDir() всегда читает актуальное
 * значение из SettingsStorage (не кэширует), поэтому смена пути установки в настройках
 * (или через диалог при первой установке) сразу видна всем частям лаунчера без перезапуска.
 */
object AppPaths {
    fun baseDir(): File = File(SettingsStorage.loadInstallDir())
    fun gameDir(): File = File(baseDir(), "game")
    fun runtimesDir(): File = File(baseDir(), "runtimes")
    fun clientModsStateFile(): File = File(baseDir(), "client-mods-managed.json")
    fun clientModsDir(): File = File(gameDir(), "mods")
}