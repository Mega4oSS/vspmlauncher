package ru.artem.alaverdyan.vspmlauncher.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.artem.alaverdyan.vspmlauncher.network.FileEntryDto
import java.io.File
import java.security.MessageDigest

object FileIntegrityVerifier {

    suspend fun findBroken(
        installDir: File,
        files: List<FileEntryDto>,
        onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> }
    ): List<FileEntryDto> = withContext(Dispatchers.IO) {
        val broken = mutableListOf<FileEntryDto>()
        files.forEachIndexed { index, entry ->
            val local = File(installDir, entry.path)

            val ok = when {
                !local.exists() -> false
                ConfigFormats.isMergeablePath(entry.path) -> {
                    runCatching { ConfigFormats.parse(entry.path, local.readText()) }.getOrNull() != null
                }

                else -> local.length() == entry.size && sha256Of(local) == entry.sha256
            }

            if (!ok) broken += entry
            onProgress(index + 1, files.size)
        }
        broken
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}