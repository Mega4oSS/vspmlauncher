package ru.artem.alaverdyan.vspmlauncher.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class InstalledVersions(val versions: Map<String, String> = emptyMap())

@Suppress("unused")
object VersionStorage {
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(installDir: File) = File(installDir, ".launcher/versions.json")

    fun load(installDir: File): Map<String, String> {
        val f = file(installDir)
        if (!f.exists()) return emptyMap()
        return try {
            json.decodeFromString<InstalledVersions>(f.readText()).versions
        } catch (_: Exception) {
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
