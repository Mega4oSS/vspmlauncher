package ru.artem.alaverdyan.vspmlauncher.runtime

import ru.artem.alaverdyan.vspmlauncher.network.RuntimeEntryDto

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

        return "https://api.adoptium.net/v3/binary/latest/$featureVersion/ga/" +
                "${entry.os}/${entry.arch}/jre/hotspot/normal/eclipse"
    }

    private fun resolveGraalvmJreUrl(entry: RuntimeEntryDto): String? {
        val version = entry.version.trim()
        if (version.isEmpty()) return null

        val os = when (entry.os) {
            "mac" -> "macos"
            else -> entry.os
        }
        val ext = if (entry.os == "windows") "zip" else "tar.gz"

        return "https://github.com/graalvm/graalvm-ce-builds/releases/download/" +
                "jdk-$version/graalvm-community-jdk-${version}_${os}-${entry.arch}_bin.$ext"
    }
}