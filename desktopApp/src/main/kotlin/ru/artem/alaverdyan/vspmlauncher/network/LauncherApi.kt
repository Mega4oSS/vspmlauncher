package ru.artem.alaverdyan.vspmlauncher.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import java.security.MessageDigest

object LauncherConfig {
    const val BASE_URL = "http://168.222.203.250:8080"
    //const val BASE_URL = "http://0.0.0.0:8080"

    const val MINECRAFT_VERSION = "1.21.1"

    val GAME_CHANNELS: List<String> = listOf(
        "reallyBuild",
        "game-$MINECRAFT_VERSION-common"
    )
}

object LauncherApi {
    val jsonClientPublic get() = jsonClient
    private val jsonClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    val downloadClient = HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10 * 60 * 1000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 60_000
        }
    }

    suspend fun getRuntimes(): RuntimeManifestDto =
        jsonClient.get("${LauncherConfig.BASE_URL}/api/v1/runtimes").body()

    suspend fun getNews(): List<NewsItemDto> =
        jsonClient.get("${LauncherConfig.BASE_URL}/api/v1/news").body()

    suspend fun getStatus(): ServerStatusDto =
        jsonClient.get("${LauncherConfig.BASE_URL}/api/v1/status").body()

    suspend fun getManifest(channel: String): ManifestDto =
        jsonClient.get("${LauncherConfig.BASE_URL}/api/v1/manifest/$channel").body()

    suspend fun getClientMods(): ClientModsManifestDto =
        jsonClient.get("${LauncherConfig.BASE_URL}/api/v1/client-mods").body()

    suspend fun adminSetMaintenance(token: String, maintenance: Boolean, message: String?): Pair<Boolean, String> {
        val response = jsonClient.post("${LauncherConfig.BASE_URL}/api/v1/admin/status") {
            header("X-Admin-Token", token)
            contentType(ContentType.Application.Json)
            setBody(AdminMaintenanceRequestDto(maintenance, message?.takeIf { it.isNotBlank() }))
        }
        val ok = response.status.value in 200..299
        val text = if (ok) (if (maintenance) "Тех. работы включены" else "Тех. работы выключены") else response.bodyAsText()
        return ok to text
    }

    suspend fun getUpdatePlan(channel: String, fromVersion: String?): UpdatePlanDto? {
        val url = if (fromVersion != null) {
            "${LauncherConfig.BASE_URL}/api/v1/update/$channel?fromVersion=$fromVersion"
        } else {
            "${LauncherConfig.BASE_URL}/api/v1/update/$channel"
        }
        return try {
            val response = jsonClient.get(url)
            if (response.status.value !in 200..299) {
                println("getUpdatePlan($channel): HTTP ${response.status}, body=${response.bodyAsText()}")
                return null
            }
            response.body()
        } catch (e: Exception) {
            println("getUpdatePlan($channel): не удалось распарсить ответ — ${e.message}")
            null
        }
    }

    suspend fun getUpdatePlans(
        installedVersions: Map<String, String?>,
        channels: List<String> = LauncherConfig.GAME_CHANNELS
    ): List<UpdatePlanDto> = coroutineScope {
        channels
            .map { channel -> async { getUpdatePlan(channel, installedVersions[channel]) } }
            .awaitAll()
            .filterNotNull()
    }

    fun resolveUrl(relativeOrAbsoluteUrl: String): String =
        if (relativeOrAbsoluteUrl.startsWith("http")) relativeOrAbsoluteUrl
        else "${LauncherConfig.BASE_URL}$relativeOrAbsoluteUrl"


    private fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun adminAuth(password: String): AdminAuthResponseDto =
        jsonClient.post("${LauncherConfig.BASE_URL}/api/v1/admin/auth") {
            contentType(ContentType.Application.Json)
            setBody(AdminAuthRequestDto(sha256Hex(password)))
        }.body()

    suspend fun adminVersions(token: String, channel: String): ChannelVersionsDto =
        jsonClient.get("${LauncherConfig.BASE_URL}/api/v1/admin/versions/$channel") {
            header("X-Admin-Token", token)
        }.body()

    suspend fun adminDiff(token: String, channel: String, from: String?, to: String): BuildDiffDto {
        val fromPart = from?.let { "&from=$it" } ?: ""
        return jsonClient.get("${LauncherConfig.BASE_URL}/api/v1/admin/diff/$channel?to=$to$fromPart") {
            header("X-Admin-Token", token)
        }.body()
    }

    suspend fun adminPublish(token: String, channel: String, version: String): Pair<Boolean, String> {
        val response = jsonClient.post("${LauncherConfig.BASE_URL}/api/v1/admin/publish/$channel") {
            header("X-Admin-Token", token)
            contentType(ContentType.Application.Json)
            setBody(AdminPublishRequestDto(version))
        }
        val ok = response.status.value in 200..299
        val text = if (ok) "Версия $version опубликована" else response.bodyAsText()
        return ok to text
    }

    suspend fun adminPublishNews(token: String, title: String, body: String, pinned: Boolean): Pair<Boolean, String> {
        val response = jsonClient.post("${LauncherConfig.BASE_URL}/api/v1/admin/news") {
            header("X-Admin-Token", token)
            contentType(ContentType.Application.Json)
            setBody(AdminNewsPublishRequestDto(title, body, pinned))
        }
        val ok = response.status.value in 200..299
        val text = if (ok) "Новость опубликована" else response.bodyAsText()
        return ok to text
    }

    suspend fun adminDeleteNews(token: String, id: Int): Pair<Boolean, String> {
        val response = jsonClient.delete("${LauncherConfig.BASE_URL}/api/v1/admin/news/$id") {
            header("X-Admin-Token", token)
        }
        val ok = response.status.value in 200..299
        val text = if (ok) "Новость удалена" else response.bodyAsText()
        return ok to text
    }
}