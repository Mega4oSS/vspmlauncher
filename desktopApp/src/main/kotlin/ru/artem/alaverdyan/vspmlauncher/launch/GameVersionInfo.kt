@file:Suppress("unused")

package ru.artem.alaverdyan.vspmlauncher.launch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Suppress("unused")
@Serializable
data class GameVersionInfo(
    val mcVersion: String,
    val neoForgeVersion: String,
    val assetIndexId: String
)

object GameVersionInfoReader {
    private val json = Json { ignoreUnknownKeys = true }

    fun read(installDir: File): GameVersionInfo {
        val file = File(installDir, "meta.json")
        if (!file.exists()) {
            error("meta.json не найден в $installDir — проверь манифест канала common")
        }
        return json.decodeFromString(GameVersionInfo.serializer(), file.readText())
    }
}