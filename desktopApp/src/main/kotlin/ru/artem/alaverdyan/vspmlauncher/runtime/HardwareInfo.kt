package ru.artem.alaverdyan.vspmlauncher.runtime

import oshi.SystemInfo

object HardwareInfo {
    private val systemInfo = SystemInfo()
    private val hal = systemInfo.hardware

    val cpuModel: String by lazy {
        hal.processor.processorIdentifier.name.trim().takeIf { it.isNotBlank() } ?: "unknown"
    }

    val gpuModel: String by lazy {
        hal.graphicsCards.firstOrNull()?.name?.trim()?.takeIf { it.isNotBlank() } ?: "unknown"
    }

    val ramTotalMb: Long by lazy {
        runCatching { hal.memory.total / (1024 * 1024) }.getOrDefault(-1L)
    }

    val cpuCores: Int by lazy { hal.processor.logicalProcessorCount }
    val osVersion: String by lazy { System.getProperty("os.version") ?: "unknown" }
    val launcherJavaVersion: String by lazy { System.getProperty("java.version") ?: "unknown" }

    val screenInfo: Pair<Int, Int> by lazy {
        runCatching {
            val bounds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            bounds.width to bounds.height
        }.getOrDefault(-1 to -1)
    }

    val dpiScale: Float by lazy {
        runCatching { java.awt.Toolkit.getDefaultToolkit().screenResolution / 96f }.getOrDefault(1f)
    }
}