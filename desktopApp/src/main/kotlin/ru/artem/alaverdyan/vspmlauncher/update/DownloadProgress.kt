package ru.artem.alaverdyan.vspmlauncher.update

enum class ProgressPhase { DOWNLOADING, CLEANING, RUNTIME, MODS, VERIFYING, MOVING }

data class DownloadProgress(
    val phase: ProgressPhase = ProgressPhase.DOWNLOADING,
    val currentFile: String,
    val fileIndex: Int,
    val totalFiles: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long
)