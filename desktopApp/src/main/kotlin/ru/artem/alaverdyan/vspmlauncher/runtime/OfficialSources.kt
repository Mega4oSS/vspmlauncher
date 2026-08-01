package ru.artem.alaverdyan.vspmlauncher.runtime

import ru.artem.alaverdyan.vspmlauncher.network.RuntimeEntryDto

/**
 * Прямые ссылки на официальные источники JRE — используются вместо зеркала
 * лаунчера, когда в настройках включено "Скачивать JRE с официальных серверов".
 *
 * Temurin (Adoptium) — публичный API с резолвом "последней сборки под фичу-версию".
 * GraalVM CE — GitHub Releases с URL, завязанным на конкретный тег jdk-{version};
 * предполагается, что entry.version хранит именно такую версию (например "21.0.2"),
 * как её кладёт RuntimeGenerator из version.txt на бэкенде.
 */
object OfficialSources {

    fun resolveUrl(entry: RuntimeEntryDto): String? = when (entry.id) {
        "standard" -> resolveAdoptiumJreUrl(entry)
        "graalvm" -> resolveGraalvmJreUrl(entry)
        else -> null
    }

    private fun resolveAdoptiumJreUrl(entry: RuntimeEntryDto): String? {
        val featureVersion = entry.version
            .substringBefore('.')
            .substringBefore('+')
            .toIntOrNull() ?: return null

        // PlatformInfo.os/.arch ("windows"/"linux"/"mac", "x64"/"aarch64") совпадают
        // с неймингом Adoptium API один в один — доп. маппинг не нужен.
        return "https://api.adoptium.net/v3/binary/latest/$featureVersion/ga/" +
                "${entry.os}/${entry.arch}/jre/hotspot/normal/eclipse"
    }

    private fun resolveGraalvmJreUrl(entry: RuntimeEntryDto): String? {
        val version = entry.version.trim()
        if (version.isEmpty()) return null

        // GraalVM использует свой нейминг ОС ("macos", не "mac") — доп. маппинг нужен.
        val os = when (entry.os) {
            "mac" -> "macos"
            else -> entry.os // "windows" / "linux" совпадают напрямую
        }
        val ext = if (entry.os == "windows") "zip" else "tar.gz"

        return "https://github.com/graalvm/graalvm-ce-builds/releases/download/" +
                "jdk-$version/graalvm-community-jdk-${version}_${os}-${entry.arch}_bin.$ext"
    }
}