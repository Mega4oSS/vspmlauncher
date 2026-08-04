@file:Suppress("unused", "PropertyName")

package ru.artem.alaverdyan.vspmlauncher.network

import kotlinx.serialization.Serializable

@Serializable
data class NewsItemDto(
    val id: Int,
    val title: String,
    val body: String,
    val publishedAt: Long,
    val pinned: Boolean = false
)

@Serializable
data class ClientEventRequestDto(
    val clientId: String,
    val type: String,
    val nickname: String? = null,
    val ramMb: Int? = null,
    val jreSource: String? = null,
    val assetsSource: String? = null,
    val runtimeId: String? = null,
    val channel: String? = null,
    val version: String? = null,
    val extra: Map<String, String>? = null
)

@Serializable
data class ClientStateSnapshotDto(
    val clientId: String,
    val updatedAt: Long,
    val nickname: String? = null,
    val ramMb: Int? = null,
    val jreSource: String? = null,
    val assetsSource: String? = null,
    val runtimeId: String? = null,
    // --- железо/окружение ---
    val os: String? = null,
    val osVersion: String? = null,
    val arch: String? = null,
    val launcherVersion: String? = null,
    val launcherJavaVersion: String? = null,
    val cpuModel: String? = null,
    val cpuCores: Int? = null,
    val gpuModel: String? = null,
    val ramTotalMb: Long? = null,
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
    val dpiScale: Double? = null
)

@Serializable
data class NicknameEntryDto(val nickname: String, val firstSeenAt: Long, val lastSeenAt: Long)

@Serializable
data class AnalyticsSummaryDto(
    val eventCounts: Map<String, Int>,
    val uniqueClients: Int,
    val adminFormReached: Int,
    val adminLoginAttempts: Int,
    val adminLoginSuccesses: Int,
    val adminLoginFailures: Int,
    val adminLoginsByIp: Map<String, Int>,
    val clientStates: List<ClientStateSnapshotDto>,
    val nicknameHistory: Map<String, List<NicknameEntryDto>>,
    // --- новое ---
    val downloadsByVersion: Map<String, Int>,
    val updatesByVersion: Map<String, Int>,
    val avgSessionDurationMs: Long? = null,
    val exitCodeCounts: Map<String, Int>,
    val avgTimeToFirstLaunchMs: Long? = null,
    val osBreakdown: Map<String, Int>,
    val launcherVersionBreakdown: Map<String, Int>
)

@Serializable
data class ServerStatusDto(
    val online: Boolean,
    val maintenance: Boolean,
    val maintenanceMessage: String? = null,
    val maintenanceUntil: Long? = null
)

@Serializable
data class FileEntryDto(
    val path: String,
    val sha256: String,
    val size: Long,
    val url: String
)

@Serializable
data class ManifestDto(
    val channel: String,
    val version: String,
    val generatedAt: Long,
    val files: List<FileEntryDto>,
    val removed: List<String> = emptyList()
)

@Serializable
data class RuntimeEntryDto(
    val id: String,
    val os: String,
    val arch: String,
    val version: String,
    val sha256: String,
    val size: Long,
    val url: String
)

@Serializable
data class RuntimeManifestDto(
    val runtimes: List<RuntimeEntryDto>
)

@Serializable
data class KeyDiffDto(
    val key: String,
    val oldValue: String?,
    val newValue: String?
)

@Serializable
data class ConfigPatchDto(
    val path: String,
    val keyDiffs: List<KeyDiffDto>
)

@Serializable
data class UpdatePlanDto(
    val channel: String,
    val fromVersion: String?,
    val toVersion: String,
    val upToDate: Boolean,
    val removed: List<String> = emptyList(),
    val added: List<FileEntryDto> = emptyList(),
    val changedFull: List<FileEntryDto> = emptyList(),
    val changedPatch: List<ConfigPatchDto> = emptyList()
)

@Serializable
data class AdminAuthRequestDto(val passwordHash: String)

@Serializable
data class AdminAuthResponseDto(
    val success: Boolean,
    val token: String? = null,
    val message: String? = null,
    val attemptsLeft: Int? = null,
    val lockedUntil: Long? = null
)

@Serializable
data class AdminPublishRequestDto(val version: String)

@Serializable
data class AdminNewsPublishRequestDto(
    val title: String,
    val body: String,
    val pinned: Boolean = false
)

@Serializable
data class AdminMaintenanceRequestDto(
    val maintenance: Boolean,
    val message: String? = null,
    val until: Long? = null
)

@Serializable
data class ChannelVersionsDto(val channel: String, val versions: List<String>)

@Serializable
data class BuildDiffDto(
    val channel: String,
    val fromVersion: String?,
    val toVersion: String,
    val removed: List<String> = emptyList(),
    val added: List<FileEntryDto> = emptyList(),
    val changedFull: List<FileEntryDto> = emptyList(),
    val changedPatch: List<ConfigPatchDto> = emptyList()
)

@Serializable
data class ClientModEntryDto(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val fileName: String,
    val sha256: String,
    val size: Long,
    val url: String
)

@Serializable
data class ClientModsManifestDto(val mods: List<ClientModEntryDto>)

@Serializable
data class ModrinthHitDto(
    val project_id: String,
    val title: String,
    val description: String,
    val icon_url: String? = null,
    val downloads: Int = 0,
    val categories: List<String> = emptyList(),
    val client_side: String = "unknown",
    val server_side: String = "unknown"
)

@Serializable
data class ModrinthSearchResponseDto(val hits: List<ModrinthHitDto>)

@Serializable
data class ModrinthProjectDto(
    val id: String,
    val title: String,
    val description: String,
    val body: String = "",
    val icon_url: String? = null,
    val gallery: List<ModrinthGalleryImageDto> = emptyList(),
    val downloads: Int = 0,
    val categories: List<String> = emptyList(),
    val client_side: String = "unknown",
    val server_side: String = "unknown"
)

@Serializable
data class ModrinthVersionDto(
    val id: String,
    val version_number: String,
    val game_versions: List<String>,
    val loaders: List<String>,
    val files: List<ModrinthVersionFileDto>
)

@Serializable
data class ModrinthGalleryImageDto(
    val url: String,
    val featured: Boolean = false,
    val title: String? = null
)

@Serializable
data class ModrinthVersionFileDto(val url: String, val filename: String, val primary: Boolean)