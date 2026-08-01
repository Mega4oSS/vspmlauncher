package ru.artem.alaverdyan.vspmlauncher.launch

import java.io.File

object ClasspathBuilder {

    fun build(installDir: File, mcVersion: String, libraryPaths: List<String>): List<File> {
        val gameJar = File(installDir, "versions/$mcVersion/$mcVersion.jar")
        if (!gameJar.exists()) error("$gameJar не найден")

        val librariesDir = File(installDir, "libraries")
        val libraryJars = libraryPaths.map { File(librariesDir, it) }.filter { it.exists() }

        return listOf(gameJar) + libraryJars
    }

    fun buildClasspathString(installDir: File, mcVersion: String, libraryPaths: List<String>): String {
        val separator = if (System.getProperty("os.name").lowercase().contains("win")) ";" else ":"
        return build(installDir, mcVersion, libraryPaths).joinToString(separator) { it.absolutePath }
    }
}