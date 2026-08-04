package ru.artem.alaverdyan.vspmlauncher.runtime

import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

/**
 * Best-effort сбор железа/окружения только для статистики. Ничего в лаунчере
 * от этих данных не зависит — любое поле может уйти как "unknown"/-1.
 * Внешние команды (wmic/lspci/sysctl) — с таймаутом, чтобы зависший процесс
 * (например lspci без прав) не подвесил лаунчер.
 */
object HardwareInfo {

    private fun execOut(vararg command: String, timeoutSec: Long = 3): String? = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        if (!process.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@runCatching null
        }
        process.inputStream.bufferedReader().readText().trim().takeIf { it.isNotBlank() }
    }.getOrNull()

    val cpuModel: String by lazy {
        when (PlatformInfo.os) {
            "windows" -> execOut("wmic", "cpu", "get", "name")
                ?.lines()?.map { it.trim() }?.firstOrNull { it.isNotBlank() && it != "Name" }
            "mac" -> execOut("sysctl", "-n", "machdep.cpu.brand_string")
            else -> runCatching {
                java.io.File("/proc/cpuinfo").readLines()
                    .firstOrNull { it.startsWith("model name") }
                    ?.substringAfter(":")?.trim()
            }.getOrNull()
        } ?: "unknown"
    }

    val gpuModel: String by lazy {
        when (PlatformInfo.os) {
            "windows" -> execOut("wmic", "path", "win32_VideoController", "get", "name")
                ?.lines()?.map { it.trim() }?.firstOrNull { it.isNotBlank() && it != "Name" }
            "mac" -> execOut("system_profiler", "SPDisplaysDataType")
                ?.lineSequence()?.firstOrNull { it.contains("Chipset Model") }
                ?.substringAfter(":")?.trim()
            else -> execOut("sh", "-c", "lspci | grep -i vga")
                ?.substringAfter(":")?.trim()
        } ?: "unknown"
    }

    // reflection, а не com.sun.management.OperatingSystemMXBean напрямую — метод переименовался
    // между версиями JDK (getTotalPhysicalMemorySize -> getTotalMemorySize), так надёжнее.
    val ramTotalMb: Long by lazy {
        runCatching {
            val bean = ManagementFactory.getOperatingSystemMXBean()
            val method = bean.javaClass.methods.firstOrNull {
                it.name == "getTotalMemorySize" || it.name == "getTotalPhysicalMemorySize"
            }
            method?.isAccessible = true
            (method?.invoke(bean) as? Long)?.let { it / (1024 * 1024) } ?: -1L
        }.getOrDefault(-1L)
    }

    val cpuCores: Int by lazy { Runtime.getRuntime().availableProcessors() }
    val osVersion: String by lazy { System.getProperty("os.version") ?: "unknown" }
    val launcherJavaVersion: String by lazy { System.getProperty("java.version") ?: "unknown" }

    val screenInfo: Pair<Int, Int> by lazy {
        runCatching {
            val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            bounds.width to bounds.height
        }.getOrDefault(-1 to -1)
    }

    val dpiScale: Float by lazy {
        runCatching { Toolkit.getDefaultToolkit().screenResolution / 96f }.getOrDefault(1f)
    }
}