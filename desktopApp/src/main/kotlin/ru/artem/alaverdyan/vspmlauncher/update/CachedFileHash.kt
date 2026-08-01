package ru.artem.alaverdyan.vspmlauncher.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CachedFileHash(val size: Long, val mtime: Long, val sha256: String)

@Serializable
private data class HashCacheData(val entries: Map<String, CachedFileHash> = emptyMap())

/**
 * Кэш sha256 локальных файлов установки, ключ — относительный путь.
 * Хранится в installDir/.launcher/hash-cache.json. Запись валидна, пока
 * совпадают size и mtime — иначе файл перехешируется.
 */
object LocalFileHashCache {
    private val json = Json { ignoreUnknownKeys = true }

    private fun cacheFile(installDir: File) = File(installDir, ".launcher/hash-cache.json")

    fun load(installDir: File): MutableMap<String, CachedFileHash> {
        val file = cacheFile(installDir)
        if (!file.exists()) return mutableMapOf()
        return try {
            json.decodeFromString<HashCacheData>(file.readText()).entries.toMutableMap()
        } catch (e: Exception) {
            mutableMapOf() // повреждённый кэш — просто пересчитаем всё заново в этот раз
        }
    }

    fun save(installDir: File, entries: Map<String, CachedFileHash>) {
        val file = cacheFile(installDir)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(HashCacheData(entries)))
    }

    /**
     * После успешной докачки файлов их sha256 уже известен из манифеста —
     * сразу кладём в кэш, не заставляя следующую проверку перехешировать их с диска.
     */
    fun updateFromDownloaded(installDir: File, downloaded: List<ru.artem.alaverdyan.vspmlauncher.network.FileEntryDto>) {
        val cache = load(installDir)
        downloaded.forEach { entry ->
            val local = File(installDir, entry.path)
            if (local.exists()) {
                cache[entry.path] = CachedFileHash(local.length(), local.lastModified(), entry.sha256)
            }
        }
        save(installDir, cache)
    }
}