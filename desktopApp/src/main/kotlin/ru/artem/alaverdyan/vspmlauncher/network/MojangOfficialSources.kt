@file:Suppress("unused")

package ru.artem.alaverdyan.vspmlauncher.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

object MojangOfficialSources {
    private const val VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    private const val RESOURCES_BASE = "https://resources.download.minecraft.net"

    private val mutex = Mutex()
    private var cache: Map<String, String>? = null

    suspend fun resolveUrl(minecraftVersion: String, relativePath: String): String? {
        val map = cache ?: mutex.withLock {
            cache ?: buildMap(minecraftVersion).also { cache = it }
        }
        return map[relativePath]
    }

    private suspend fun buildMap(minecraftVersion: String): Map<String, String> {
        val manifest: VersionManifestDto = LauncherApi.jsonClientPublic.get(VERSION_MANIFEST_URL).body()
        val versionEntry = manifest.versions.find { it.id == minecraftVersion } ?: return emptyMap()
        val detail: VersionDetailDto = LauncherApi.jsonClientPublic.get(versionEntry.url).body()

        val result = mutableMapOf<String, String>()

        result["versions/$minecraftVersion/$minecraftVersion.jar"] = detail.downloads.client.url

        detail.libraries.forEach { lib ->
            lib.downloads.artifact?.let { artifact ->
                result["libraries/${artifact.path}"] = artifact.url
            }
        }

        result["assets/indexes/${detail.assetIndex.id}.json"] = detail.assetIndex.url

        val assetIndex: AssetIndexContentDto = LauncherApi.jsonClientPublic.get(detail.assetIndex.url).body()
        assetIndex.objects.values.forEach { obj ->
            val prefix = obj.hash.take(2)
            result["assets/objects/$prefix/${obj.hash}"] = "$RESOURCES_BASE/$prefix/${obj.hash}"
        }

        return result
    }

    @Serializable
    private data class VersionManifestDto(val versions: List<VersionEntryDto>)
    @Serializable
    private data class VersionEntryDto(val id: String, val url: String)
    @Serializable
    private data class VersionDetailDto(
        val downloads: DownloadsDto,
        val libraries: List<LibraryDto>,
        val assetIndex: AssetIndexEntryDto
    )
    @Serializable
    private data class DownloadsDto(val client: ArtifactDto)
    @Serializable
    private data class LibraryDto(val downloads: LibraryDownloadsDto)
    @Serializable
    private data class LibraryDownloadsDto(val artifact: ArtifactDto? = null)
    @Serializable
    private data class ArtifactDto(val path: String = "", val url: String, val sha1: String = "", val size: Long = 0)
    @Serializable
    private data class AssetIndexEntryDto(val id: String, val url: String)
    @Serializable
    private data class AssetIndexContentDto(val objects: Map<String, AssetObjectDto>)
    @Serializable
    private data class AssetObjectDto(val hash: String, val size: Long)
}