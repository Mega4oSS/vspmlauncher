package ru.artem.alaverdyan.vspmlauncher.update

import java.io.File

object Cleaner {
    fun removeOrphans(
        installDir: File,
        orphans: List<File>,
        onProgress: (DownloadProgress) -> Unit = {}
    ) {
        if (orphans.isNotEmpty()) {
            println("[Cleaner] удаляю ${orphans.size} файл(ов) как мусор:")
        }
        orphans.forEachIndexed { index, file ->
            val relativePath = file.relativeTo(installDir).path.replace('\\', '/')
            println("[Cleaner]   DELETE $relativePath (${file.length()} байт, mtime=${file.lastModified()})")
            file.delete()

            onProgress(
                DownloadProgress(
                    phase = ProgressPhase.CLEANING,
                    currentFile = relativePath,
                    fileIndex = index + 1,
                    totalFiles = orphans.size,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    bytesPerSecond = 0L
                )
            )
        }
        removeEmptyDirs(installDir)
    }

    private fun removeEmptyDirs(dir: File) {
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                removeEmptyDirs(child)
                val relativePath = child.relativeTo(dir.parentFile ?: dir).path.replace('\\', '/')
                if (child.listFiles()?.isEmpty() == true && !IgnoreRules.isIgnored(relativePath)) {
                    println("[Cleaner]   DELETE-DIR $relativePath (пустая)")
                    child.delete()
                }
            }
        }
    }
}