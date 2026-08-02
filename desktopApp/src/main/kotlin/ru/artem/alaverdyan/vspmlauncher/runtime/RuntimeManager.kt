package ru.artem.alaverdyan.vspmlauncher.runtime

import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.artem.alaverdyan.vspmlauncher.data.AppPaths
import ru.artem.alaverdyan.vspmlauncher.data.SettingsStorage
import ru.artem.alaverdyan.vspmlauncher.network.LauncherApi
import ru.artem.alaverdyan.vspmlauncher.network.RuntimeEntryDto
import ru.artem.alaverdyan.vspmlauncher.update.DownloadProgress
import ru.artem.alaverdyan.vspmlauncher.update.ProgressPhase
import ru.artem.alaverdyan.vspmlauncher.update.withRetry
import java.io.File

private val RUNTIMES_ROOT: File get() = AppPaths.runtimesDir()

sealed interface RuntimeStatus {
    // reason — по-человечески объясняет, ПОЧЕМУ рантайм не найден: нет подходящей записи
    // в манифесте для этой платформы или архив скачался, но java-бинарник внутри не нашёлся.
    // Раньше это был data object без подробностей, и на Windows пользователь видел одно и то
    // же общее "Java Runtime не найден" что при отсутствии записи в манифесте, что при битом
    // архиве — невозможно было понять, что чинить.
    data class Missing(val reason: String) : RuntimeStatus
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
        // Сравнение регистронезависимое: бэкенд отдаёт os/arch как обычные строки, и если
        // там когда-то попадёт "Windows" вместо "windows" (или наоборот), раньше find() молча
        // не находил запись и лаунчер говорил "рантайм не найден", хотя на сервере он есть.
        val platformEntry = manifest.runtimes.find {
            it.id.equals(id, ignoreCase = true) &&
                    it.os.equals(PlatformInfo.os, ignoreCase = true) &&
                    it.arch.equals(PlatformInfo.arch, ignoreCase = true)
        }

        // ВАЖНО: официальному источнику (Adoptium/GraalVM) не нужно, чтобы ЛОКАЛЬНЫЙ бэкенд уже
        // держал зеркало именно под текущую платформу — Adoptium/GraalVM сами прекрасно знают,
        // что у них есть под Windows/Linux/macOS. С сервера реально нужна только версия (какой
        // фича-релиз считается актуальным для id=$id) — а её можно взять из ЛЮБОЙ платформы, для
        // которой этот id вообще опубликован. Раньше при отсутствии platformEntry код возвращал
        // Missing СРАЗУ, ещё до того, как вообще пробовал официальный источник — поэтому тумблер
        // "качать с официальных серверов" не спасал, даже если на Adoptium архив 100% есть.
        val useOfficial = SettingsStorage.loadDownloadJreFromOfficial()
        val referenceEntry = platformEntry ?: manifest.runtimes.find { it.id.equals(id, ignoreCase = true) }

        if (platformEntry == null && (!useOfficial || referenceEntry == null)) {
            return@withContext RuntimeStatus.Missing(
                "на сервере нет сборки рантайма \"$id\" для платформы ${PlatformInfo.os}-${PlatformInfo.arch}. " +
                        "Доступные варианты для этой платформы: " +
                        (manifest.runtimes.filter {
                            it.os.equals(PlatformInfo.os, ignoreCase = true) && it.arch.equals(PlatformInfo.arch, ignoreCase = true)
                        }.map { it.id }.ifEmpty { listOf("нет ни одного") }.joinToString(", ")) +
                        if (!useOfficial) ". Можно попробовать включить в настройках \"Java Runtime с официальных серверов\"." else ""
            )
        }

        // entry.version берём хоть с чужой платформы (referenceEntry), если под нашу платформу
        // на сервере ничего нет — но sha256/size/url из неё валидны ТОЛЬКО когда это
        // platformEntry (т.е. реально с сервера под нашу платформу). Если мы тут оказались через
        // referenceEntry — проверять архив по контрольной сумме не по чему, доверяем TLS-соединению
        // напрямую с Adoptium/GraalVM (verifiedBySha256 = false отключает сверку ниже).
        val entry = platformEntry ?: referenceEntry!!
        val verifiedBySha256 = platformEntry != null

        val dir = runtimeDir(id)
        val marker = versionMarker(id)

        if (!forceReinstall && marker.exists() && marker.readText().trim() == entry.version) {
            findJavaBinary(dir)?.let { return@withContext RuntimeStatus.Installed(it, entry.version) }
        }

        val downloadUrl = if (platformEntry != null) {
            resolveDownloadUrl(entry)
        } else {
            // Своего зеркала под эту платформу на сервере нет — единственный вариант это
            // официальный источник, строим ссылку под РЕАЛЬНУЮ платформу пользователя (entry
            // могла прийти с другой платформы, поэтому явно подставляем свои os/arch).
            OfficialSources.resolveUrl(entry.copy(os = PlatformInfo.os, arch = PlatformInfo.arch))
                ?: return@withContext RuntimeStatus.Missing(
                    "не удалось построить ссылку на официальный источник для \"$id\" ${PlatformInfo.os}-${PlatformInfo.arch}"
                )
        }
        val archiveExt = if (downloadUrl.endsWith(".zip")) "zip" else "tar.gz"
        val archive = File(RUNTIMES_ROOT, "$id-download.$archiveExt")
        archive.parentFile?.mkdirs()

        withRetry("скачивание рантайма ${entry.id} ${entry.version}") {
            downloadArchive(entry, downloadUrl, archive, onProgress)
        }

        // Раньше архив вообще не сверялся по sha256 — битая/оборванная закачка молча
        // распаковывалась как есть. Проверяем, и если мимо — пробуем зеркало лаунчера
        // (тот же приём, что и в Downloader.downloadAll для файлов сборки).
        // Сверять есть по чему только если entry реально с сервера под нашу платформу
        // (platformEntry) — иначе (архив взят напрямую с Adoptium/GraalVM по referenceEntry
        // с чужой платформы) эталонного sha256 просто нет, доверяем TLS-соединению.
        if (verifiedBySha256 && sha256Of(archive) != entry.sha256) {
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

        val javaBinary = findJavaBinary(dir) ?: return@withContext RuntimeStatus.Missing(
            "архив рантайма \"$id\" скачался и распаковался (в ${dir.absolutePath}), но внутри не " +
                    "нашёлся исполняемый файл \"${PlatformInfo.javaBinaryName}\" в подпапке bin — похоже, " +
                    "архив для этой платформы собран неправильно"
        )
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