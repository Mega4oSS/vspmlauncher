package ru.artem.alaverdyan.vspmlauncher.launch

import java.io.File
import java.util.zip.ZipFile

object NativesExtractor {
    private val NATIVE_EXTENSIONS = listOf(".so", ".dll", ".dylib")

    fun ensureExtracted(installDir: File): File {
        val nativesJarsDir = File(installDir, "natives")
        val extractDir = File(installDir, "natives-extracted")
        val marker = File(extractDir, ".extracted_from")

        val jarFiles = nativesJarsDir.listFiles { f -> f.extension == "jar" }
            ?.sortedBy { it.name }
            ?: emptyList()

        val signature = jarFiles.joinToString("|") { "${it.name}:${it.length()}:${it.lastModified()}" }

        if (marker.exists() && marker.readText() == signature && jarFiles.isNotEmpty()) {
            return extractDir
        }

        extractDir.deleteRecursively()
        extractDir.mkdirs()

        jarFiles.forEach { jarFile ->
            ZipFile(jarFile).use { zip ->
                zip.entries().asSequence()
                    .filter { entry -> !entry.isDirectory }
                    .filter { entry -> !entry.name.startsWith("META-INF") }
                    .filter { entry -> NATIVE_EXTENSIONS.any { entry.name.endsWith(it) } }
                    .forEach { entry ->
                        val outFile = File(extractDir, File(entry.name).name)
                        zip.getInputStream(entry).use { input ->
                            outFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
            }
        }

        marker.writeText(signature)
        return extractDir
    }
}