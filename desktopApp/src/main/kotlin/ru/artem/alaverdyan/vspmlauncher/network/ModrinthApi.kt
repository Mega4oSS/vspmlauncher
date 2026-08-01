package ru.artem.alaverdyan.vspmlauncher.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val MODRINTH_BASE = "https://api.modrinth.com/v2"

object ModrinthApi {

    // null — сетевая ошибка / Modrinth недоступен у части пользователей; UI должен отличать
    // это от "ничего не найдено".
    suspend fun search(query: String, category: String? = null): ModrinthSearchResponseDto? = runCatching {
        val facetGroups = mutableListOf("""["project_type:mod"]""")
        if (category != null) facetGroups += """["categories:$category"]"""
        LauncherApi.jsonClientPublic.get("$MODRINTH_BASE/search") {
            parameter("query", query)
            parameter("facets", "[" + facetGroups.joinToString(",") + "]")
            parameter("limit", 30)
        }.body<ModrinthSearchResponseDto>()
    }.getOrNull()

    suspend fun projectDetails(projectId: String): ModrinthProjectDto? = runCatching {
        LauncherApi.jsonClientPublic.get("$MODRINTH_BASE/project/$projectId").body<ModrinthProjectDto>()
    }.getOrNull()

    suspend fun versionsFor(projectId: String, gameVersion: String, loader: String): List<ModrinthVersionDto>? = runCatching {
        LauncherApi.jsonClientPublic.get("$MODRINTH_BASE/project/$projectId/version") {
            parameter("game_versions", """["$gameVersion"]""")
            parameter("loaders", """["$loader"]""")
        }.body<List<ModrinthVersionDto>>()
    }.getOrNull()

    suspend fun downloadTo(url: String, target: File) = withContext(Dispatchers.IO) {
        LauncherApi.downloadClient.prepareGet(url).execute { response ->
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(64 * 1024)
            target.outputStream().use { output ->
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
            }
        }
    }
}