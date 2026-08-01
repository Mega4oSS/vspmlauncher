package ru.artem.alaverdyan.vspmlauncher.update

import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import ru.artem.alaverdyan.vspmlauncher.data.SettingsStorage
import ru.artem.alaverdyan.vspmlauncher.network.FileEntryDto
import ru.artem.alaverdyan.vspmlauncher.network.LauncherApi
import ru.artem.alaverdyan.vspmlauncher.network.LauncherConfig
import ru.artem.alaverdyan.vspmlauncher.network.MojangOfficialSources
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private const val MAX_CONCURRENT_DOWNLOADS = 6

private fun sha256Of(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

object Downloader {
    suspend fun downloadAll(
        installDir: File,
        files: List<FileEntryDto>,
        onProgress: (DownloadProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val totalBytes = files.sumOf { it.size }
        val downloadedSoFar = AtomicLong(0L)
        val completedFiles = AtomicInteger(0)
        val startTime = System.currentTimeMillis()
        val lastEmitTime = AtomicLong(0L)

        val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)

        val jobs = files.map { entry ->
            async {
                semaphore.withPermit {
                    val target = File(installDir, entry.path)
                    target.parentFile?.mkdirs()

                    suspend fun fetchTo(url: String) {
                        withRetry("скачивание ${entry.path}") {
                            LauncherApi.downloadClient.prepareGet(url).execute { response ->
                                val channel = response.bodyAsChannel()
                                val buffer = ByteArray(64 * 1024)

                                target.outputStream().use { output ->
                                    while (true) {
                                        val read = channel.readAvailable(buffer, 0, buffer.size)
                                        if (read == -1) break

                                        output.write(buffer, 0, read)
                                        val downloaded = downloadedSoFar.addAndGet(read.toLong())

                                        val now = System.currentTimeMillis()
                                        val isLastChunkOfLastFile = downloaded == totalBytes
                                        if (now - lastEmitTime.get() >= 150 || isLastChunkOfLastFile) {
                                            lastEmitTime.set(now)
                                            val elapsedSec = (now - startTime) / 1000.0
                                            val speed = if (elapsedSec > 0) (downloaded / elapsedSec).toLong() else 0L

                                            val progress = DownloadProgress(
                                                currentFile = entry.path,
                                                fileIndex = completedFiles.get() + 1,
                                                totalFiles = files.size,
                                                downloadedBytes = downloaded,
                                                totalBytes = totalBytes,
                                                bytesPerSecond = speed
                                            )

                                            withContext(Dispatchers.Main) {
                                                onProgress(progress)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val downloadUrl = resolveDownloadUrl(entry)
                    fetchTo(downloadUrl)

                    // Official-источник (Mojang) может отдать файл, не совпадающий с тем, что
                    // ожидает манифест сборки — например путь пропатчен модлоадером на бэкенде.
                    // Без этой проверки такой файл будет вечно считаться "битым" на каждой
                    // следующей проверке обновлений и вечно перекачиваться с Mojang заново.
                    if (sha256Of(target) != entry.sha256) {
                        val mirrorUrl = LauncherApi.resolveUrl(entry.url)
                        if (mirrorUrl != downloadUrl) {
                            fetchTo(mirrorUrl)
                        }
                        check(sha256Of(target) == entry.sha256) {
                            "Скачанный файл ${entry.path} не совпадает с ожидаемым sha256 после отката на зеркало лаунчера"
                        }
                    }

                    completedFiles.incrementAndGet()
                }
            }
        }

        jobs.awaitAll()
    }

    // Официальный источник пробуем только для файлов, чей путь есть в официальном
    // version.json Mojang (libraries/…, versions/…/*.jar, assets/…) — модлоадер,
    // моды и всё из reallyBuild там просто не найдутся, resolveUrl вернёт null,
    // и мы тихо откатимся на зеркало лаунчера.
    private suspend fun resolveDownloadUrl(entry: FileEntryDto): String {
        if (SettingsStorage.loadDownloadMinecraftFromOfficial()) {
            val official = runCatching {
                MojangOfficialSources.resolveUrl(LauncherConfig.MINECRAFT_VERSION, entry.path)
            }.getOrNull()
            if (official != null) return official
        }
        return LauncherApi.resolveUrl(entry.url)
    }
}