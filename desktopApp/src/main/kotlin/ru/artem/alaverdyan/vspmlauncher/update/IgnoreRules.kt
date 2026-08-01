package ru.artem.alaverdyan.vspmlauncher.update

/**
 * Пути (относительно корня установки), которые чистка мусора никогда не трогает —
 * пользовательские данные, а не файлы сборки. Указывай верхнеуровневую папку или
 * конкретный файл, префиксное совпадение по сегментам пути.
 */
object IgnoreRules {
    val IGNORED_PREFIXES: Set<String> = setOf(
        "saves",
        "screenshots",
        "options.txt",
        "resourcepacks",
        "logs",
        ".launcher" // служебные файлы лаунчера (hash-cache.json и т.п.) — не мусор сборки
    )

    fun isIgnored(relativePath: String): Boolean {
        val normalized = relativePath.replace('\\', '/')
        return IGNORED_PREFIXES.any { prefix ->
            normalized == prefix || normalized.startsWith("$prefix/")
        }
    }
}