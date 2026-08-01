package ru.artem.alaverdyan.vspmlauncher.runtime

import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.artem.alaverdyan.vspmlauncher.data.SettingsStorage
import ru.artem.alaverdyan.vspmlauncher.network.LauncherApi
import ru.artem.alaverdyan.vspmlauncher.network.RuntimeEntryDto
import ru.artem.alaverdyan.vspmlauncher.update.DownloadProgress
import ru.artem.alaverdyan.vspmlauncher.update.ProgressPhase
import ru.artem.alaverdyan.vspmlauncher.update.withRetry
import java.io.File

private val RUNTIMES_ROOT = File(System.getProperty("user.home"), ".vspmlauncher/runtimes")

sealed interface RuntimeStatus {
    data object Missing : RuntimeStatus
    data class Installed(val javaBinary: File, val version: String) : RuntimeStatus
}

object RuntimeManager {

    private fun runtimeDir(id: String) = File(RUNTIMES_ROOT, id)
    private fun versionMarker(id: String) = File(runtimeDir(id), ".installed_version")

    suspend fun ensureInstalled(
        id: String,
        forceReinstall: Boolean = false,
        onProgress: (DownloadProgress?) -> Unit = {}
    ): RuntimeStatus = withContext(Dispatchers.IO) {
        val manifest = LauncherApi.getRuntimes()
        val entry = manifest.runtimes.find {
            it.id == id && it.os == PlatformInfo.os && it.arch == PlatformInfo.arch
        } ?: return@withContext RuntimeStatus.Missing

        val dir = runtimeDir(id)
        val marker = versionMarker(id)

        if (!forceReinstall && marker.exists() && marker.readText().trim() == entry.version) {
            findJavaBinary(dir)?.let { return@withContext RuntimeStatus.Installed(it, entry.version) }
        }

        val downloadUrl = resolveDownloadUrl(entry)
        val archiveExt = if (downloadUrl.endsWith(".zip")) "zip" else "tar.gz"
        val archive = File(RUNTIMES_ROOT, "$id-download.$archiveExt")
        archive.parentFile?.mkdirs()

        withRetry("скачивание рантайма ${entry.id} ${entry.version}") {
            downloadArchive(entry, downloadUrl, archive, onProgress)
        }

        // Раньше архив вообще не сверялся по sha256 — битая/оборванная закачка молча
        // распаковывалась как есть. Проверяем, и если мимо — пробуем зеркало лаунчера
        // (тот же приём, что и в Downloader.downloadAll для файлов сборки).
        if (sha256Of(archive) != entry.sha256) {
            val mirrorUrl = LauncherApi.resolveUrl(entry.url)
            if (mirrorUrl != downloadUrl) {
                withRetry("скачивание рантайма ${entry.id} ${entry.version} (зеркало)") {
                    downloadArchive(entry, mirrorUrl, archive, onProgress)
                }
            }
            check(sha256Of(archive) == entry.sha256) {
                "Скачанный архив рантайма ${entry.id} не совпадает с ожидаемым sha256"
            }
        }

        onProgress(
            DownloadProgress(
                phase = ProgressPhase.RUNTIME,
                currentFile = "Распаковка ${entry.id}...",
                fileIndex = 1,
                totalFiles = 1,
                downloadedBytes = entry.size,
                totalBytes = entry.size,
                bytesPerSecond = 0
            )
        )

        dir.deleteRecursively()
        dir.mkdirs()
        extractArchive(archive, dir)
        archive.delete()

        marker.writeText(entry.version)
        onProgress(null)

        val javaBinary = findJavaBinary(dir) ?: return@withContext RuntimeStatus.Missing
        RuntimeStatus.Installed(javaBinary, entry.version)
    }

    private fun sha256Of(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // Официальный источник пробуем только если тумблер включён и для этого id/версии
    // есть предсказуемый паттерн URL (Temurin/GraalVM) — иначе тихо остаёмся на зеркале.
    private fun resolveDownloadUrl(entry: RuntimeEntryDto): String {
        if (SettingsStorage.loadDownloadJreFromOfficial()) {
            OfficialSources.resolveUrl(entry)?.let { return it }
        }
        return LauncherApi.resolveUrl(entry.url)
    }

    private suspend fun downloadArchive(
        entry: RuntimeEntryDto,
        url: String,
        target: File,
        onProgress: (DownloadProgress?) -> Unit
    ) {
        val totalBytes = entry.size
        var downloaded = 0L
        val startTime = System.currentTimeMillis()
        var lastEmit = 0L

        LauncherApi.downloadClient.prepareGet(url).execute { response ->
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(64 * 1024)

            target.outputStream().use { output ->
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read

                    val now = System.currentTimeMillis()
                    val isLastChunk = downloaded == totalBytes
                    if (now - lastEmit >= 150 || isLastChunk) {
                        lastEmit = now
                        val elapsedSec = (now - startTime) / 1000.0
                        val speed = if (elapsedSec > 0) (downloaded / elapsedSec).toLong() else 0L

                        onProgress(
                            DownloadProgress(
                                phase = ProgressPhase.RUNTIME,
                                currentFile = "${entry.id} (${entry.version})",
                                fileIndex = 1,
                                totalFiles = 1,
                                downloadedBytes = downloaded,
                                totalBytes = totalBytes,
                                bytesPerSecond = speed
                            )
                        )
                    }
                }
            }
        }
    }

    private fun extractArchive(archive: File, destDir: File) {
        // bsdtar (доступен на Linux/macOS, и как tar.exe в Windows 10 1803+) умеет
        // и tar.gz, и zip — но флаг -z жёстко предполагает gzip, поэтому для .zip
        // используем отдельную ветку без -z, иначе распаковка молча ломается.
        val isZip = archive.extension.equals("zip", ignoreCase = true)

        val command = if (isZip) {
            listOf("tar", "-xf", archive.absolutePath, "-C", destDir.absolutePath, "--strip-components=1")
        } else {
            listOf("tar", "-xzf", archive.absolutePath, "-C", destDir.absolutePath, "--strip-components=1")
        }

        val process = ProcessBuilder(command).redirectErrorStream(true).start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val output = process.inputStream.bufferedReader().readText()
            throw IllegalStateException("Не удалось распаковать $archive (код $exitCode): $output")
        }
    }

    private fun findJavaBinary(dir: File): File? {
        return dir.walkTopDown()
            .firstOrNull { it.isFile && it.name == PlatformInfo.javaBinaryName && it.path.contains("bin") }
    }
}