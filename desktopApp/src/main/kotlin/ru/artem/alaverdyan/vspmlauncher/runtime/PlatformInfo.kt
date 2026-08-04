package ru.artem.alaverdyan.vspmlauncher.runtime

@Suppress("unused")
object PlatformInfo {
    val os: String by lazy {
        val name = System.getProperty("os.name").lowercase()
        when {
            name.contains("win") -> "windows"
            name.contains("mac") || name.contains("darwin") -> "mac"
            else -> "linux"
        }
    }

    val arch: String by lazy {
        val a = System.getProperty("os.arch").lowercase()
        when {
            a.contains("aarch64") || a.contains("arm64") -> "aarch64"
            else -> "x64"
        }
    }

    val javaBinaryName: String get() = if (os == "windows") "java.exe" else "java"
}