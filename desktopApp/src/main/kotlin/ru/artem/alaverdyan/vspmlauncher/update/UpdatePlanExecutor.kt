package ru.artem.alaverdyan.vspmlauncher.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.artem.alaverdyan.vspmlauncher.network.UpdatePlanDto
import java.io.File

object UpdatePlanExecutor {

    suspend fun apply(
        installDir: File,
        plan: UpdatePlanDto,
        onProgress: (DownloadProgress) -> Unit = {}
    ): List<ConflictInfo> = withContext(Dispatchers.IO) {
        if (plan.upToDate) return@withContext emptyList()

        plan.removed.forEach { path ->
            File(installDir, path).delete()
        }

        val toDownload = plan.added + plan.changedFull
        if (toDownload.isNotEmpty()) {
            Downloader.downloadAll(installDir, toDownload, onProgress)
        }

        val conflicts = mutableListOf<ConflictInfo>()
        plan.changedPatch.forEach { patch ->
            val local = File(installDir, patch.path)
            if (local.exists()) {
                conflicts += ConfigMerger.applyPatch(local, patch)
            }
        }

        VersionStorage.set(installDir, plan.channel, plan.toVersion)
        conflicts
    }
}