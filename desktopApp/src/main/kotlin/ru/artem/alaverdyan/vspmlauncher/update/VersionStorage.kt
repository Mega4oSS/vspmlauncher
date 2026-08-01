package ru.artem.alaverdyan.vspmlauncher.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class InstalledVersions(val versions: Map<String, String> = emptyMap())

/**
 * Хранит installDir/.launcher/versions.json — какая версия каждого канала сейчас установлена.
 * Это и есть новый триггер "нужно обновление": не хэши файлов на диске, а сравнение
 * этой версии с последней версией на бэкенде.
 */
object VersionStorage {
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(installDir: File) = File(installDir, ".launcher/versions.json")

    fun load(installDir: File): Map<String, String> {
        val f = file(installDir)
        if (!f.exists()) return emptyMap()
        return try {
            json.decodeFromString<InstalledVersions>(f.readText()).versions
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun get(installDir: File, channel: String): String? = load(installDir)[channel]

    fun set(installDir: File, channel: String, version: String) {
        val current = load(installDir).toMutableMap()
        current[channel] = version
        val f = file(installDir)
        f.parentFile?.mkdirs()
        f.writeText(json.encodeToString(InstalledVersions(current)))
    }
}
