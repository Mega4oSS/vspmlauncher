package ru.artem.alaverdyan.vspmlauncher.data

import java.io.File

// ВАЖНО: это должна быть та же папка, откуда NeoForge реально грузит моды (game_directory/mods,
// см. GameLauncher.placeholders["game_directory"] = installDir в App.kt). Раньше тут была
// отдельная ~/.vspmlauncher/client-mods, никак не связанная с папкой установки — из-за этого
// и "Мои моды", и серверные клиентские моды физически лежали не там, откуда их читает игра.
// Теперь путь берётся из AppPaths, которая следует за настраиваемым путём установки.
val CLIENT_MODS_DIR: File get() = AppPaths.clientModsDir()

// SERVER — поставлен через ClientModsManager.sync() по чекбоксу в "Клиентские моды сервера".
// Такие файлы управляются автоматически (докачка/удаление при (де)активации) и не должны
// попадать в список "Мои моды" — см. ClientModsManager.listInstalled().
enum class LocalModSource { MODRINTH, LOCAL_FILE, SERVER }

data class LocalClientMod(
    val id: String,
    val name: String,
    val description: String,
    val jarFile: File,
    val source: LocalModSource
)