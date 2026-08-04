package ru.artem.alaverdyan.vspmlauncher.data

import java.io.File

object AppPaths {
    fun baseDir(): File = File(SettingsStorage.loadInstallDir())
    fun gameDir(): File = File(baseDir(), "game")
    fun runtimesDir(): File = File(baseDir(), "runtimes")
    fun clientModsStateFile(): File = File(baseDir(), "client-mods-managed.json")
    fun clientModsDir(): File = File(gameDir(), "mods")
}