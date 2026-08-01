package ru.artem.alaverdyan.vspmlauncher.update

import ru.artem.alaverdyan.vspmlauncher.network.FileEntryDto
import ru.artem.alaverdyan.vspmlauncher.network.ManifestDto
import java.io.File
import java.security.MessageDigest

data class UpdateDiff(
    val needsUpdate: Boolean,
    val filesToDownload: List<FileEntryDto>
)

object UpdateChecker {

    fun diff(installDir: File, manifests: List<ManifestDto>): UpdateDiff {
        val cache = LocalFileHashCache.load(installDir)
        val allFiles = manifests.flatMap { it.files }

        val toDownload = allFiles.filter { entry ->
            val local = File(installDir, entry.path)

            if (!local.exists()) {
                println("[UpdateChecker] ${entry.path}: нужен — файла нет на диске")
                return@filter true
            }
            if (local.length() != entry.size) {
                println("[UpdateChecker] ${entry.path}: нужен — размер не совпадает (локально ${local.length()}, в манифесте ${entry.size})")
                return@filter true
            }

            val mtime = local.lastModified()
            val cached = cache[entry.path]

            val actualHash = if (cached != null && cached.size == local.length() && cached.mtime == mtime) {
                cached.sha256 // файл не менялся с прошлой проверки — не трогаем диск заново
            } else {
                val computed = sha256Of(local)
                cache[entry.path] = CachedFileHash(local.length(), mtime, computed)
                computed
            }

            val needsUpdate = actualHash != entry.sha256
            if (needsUpdate) {
                println("[UpdateChecker] ${entry.path}: нужен — хэш не совпадает (локально $actualHash, в манифесте ${entry.sha256})")
            }
            needsUpdate
        }
        println("[UpdateChecker] итого к докачке: ${toDownload.size} из ${allFiles.size}")

        LocalFileHashCache.save(installDir, cache)
        return UpdateDiff(needsUpdate = toDownload.isNotEmpty(), filesToDownload = toDownload)
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Быстрый путь удаления мусора: бэкенд сам знает, какие файлы пропали
     * между поколениями сборки (Manifest.removed), и присылает список путей.
     * Не нужно обходить весь install-dir, чтобы их найти.
     */
    fun findKnownRemovedFiles(installDir: File, manifests: List<ManifestDto>): List<File> {
        val removedPaths = manifests.flatMap { it.removed }.toSet()
        val result = removedPaths.mapNotNull { path ->
            val file = File(installDir, path)
            if (file.exists()) file else null
        }
        if (result.isNotEmpty()) {
            println("[UpdateChecker] known-removed (пришло в Manifest.removed от бэкенда): ${result.map { it.relativeTo(installDir).path }}")
        }
        return result
    }

    /**
     * Полный обход диска — оставлен как подстраховка на случай файлов, которые
     * оказались в install-dir не через манифест (ручная установка мода и т.п.)
     * и поэтому не попадут в Manifest.removed ни одного канала.
     */
    fun findOrphanFiles(installDir: File, manifests: List<ManifestDto>): List<File> {
        val manifestPaths = manifests.flatMap { it.files }
            .map { it.path.replace('\\', '/') }
            .toSet()

        val orphans = installDir.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                val relativePath = file.relativeTo(installDir).path.replace('\\', '/')
                relativePath !in manifestPaths && !IgnoreRules.isIgnored(relativePath)
            }
            .toList()

        if (orphans.isNotEmpty()) {
            println("[UpdateChecker] orphan-файлы (не найдены ни в одном манифесте, не в IgnoreRules):")
            orphans.forEach { println("[UpdateChecker]   ${it.relativeTo(installDir).path.replace('\\', '/')}") }
        }
        return orphans
    }
}