package ru.artem.alaverdyan.vspmlauncher.data

import java.io.File

val CLIENT_MODS_DIR: File get() = AppPaths.clientModsDir()

enum class LocalModSource { MODRINTH, LOCAL_FILE }

data class LocalClientMod(
    val id: String,
    val name: String,
    val description: String,
    val jarFile: File,
    val source: LocalModSource
)