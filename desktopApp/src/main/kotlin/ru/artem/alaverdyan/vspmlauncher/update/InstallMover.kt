package ru.artem.alaverdyan.vspmlauncher.update

import java.io.File

object InstallMover {

    sealed interface MoveResult {
        data object Success : MoveResult
        data class Error(val message: String) : MoveResult
    }

    fun move(
        oldBaseDir: File,
        newBaseDir: File,
        onProgress: (DownloadProgress?) -> Unit
    ): MoveResult {
        if (!oldBaseDir.exists()) {
            return MoveResult.Error("Исходная папка ${oldBaseDir.absolutePath} не найдена")
        }
        val oldCanonical = oldBaseDir.canonicalFile
        val newCanonical = newBaseDir.canonicalFile
        if (oldCanonical == newCanonical) {
            return MoveResult.Error("Новый путь совпадает с текущим")
        }
        if (newCanonical.path.startsWith(oldCanonical.path + File.separator)) {
            return MoveResult.Error("Нельзя переместить папку внутрь самой себя")
        }

        val allFiles = oldBaseDir.walkTopDown().filter { it.isFile }.toList()
        val totalBytes = allFiles.sumOf { it.length() }
        val totalFiles = allFiles.size
        var movedBytes = 0L

        newBaseDir.mkdirs()

        allFiles.forEachIndexed { index, file ->
            val relative = file.relativeTo(oldBaseDir)
            val target = File(newBaseDir, relative.path)
            target.parentFile?.mkdirs()

            val copyResult = runCatching {
                copyFileWithProgress(file, target) { copiedInFile ->
                    onProgress(
                        DownloadProgress(
                            phase = ProgressPhase.MOVING,
                            currentFile = relative.path,
                            fileIndex = index + 1,
                            totalFiles = totalFiles,
                            downloadedBytes = movedBytes + copiedInFile,
                            totalBytes = totalBytes,
                            bytesPerSecond = 0
                        )
                    )
                }
            }
            if (copyResult.isFailure) {
                return MoveResult.Error(
                    "Не удалось скопировать ${relative.path}: ${copyResult.exceptionOrNull()?.message}"
                )
            }

            movedBytes += file.length()
        }

        onProgress(null)

        if (!oldBaseDir.deleteRecursively()) {
            return MoveResult.Error(
                "Файлы скопированы в ${newBaseDir.absolutePath}, но старую папку " +
                        "${oldBaseDir.absolutePath} не удалось удалить — удали её вручную"
            )
        }

        return MoveResult.Success
    }

    private fun copyFileWithProgress(source: File, target: File, onChunk: (Long) -> Unit) {
        source.inputStream().buffered().use { input ->
            target.outputStream().buffered().use { output ->
                val buffer = ByteArray(1 * 1024 * 1024)
                var copied = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    copied += read
                    onChunk(copied)
                }
            }
        }
    }
}