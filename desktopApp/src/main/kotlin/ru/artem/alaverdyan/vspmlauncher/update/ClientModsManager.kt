package ru.artem.alaverdyan.vspmlauncher.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.artem.alaverdyan.vspmlauncher.data.AppPaths
import ru.artem.alaverdyan.vspmlauncher.data.CLIENT_MODS_DIR
import ru.artem.alaverdyan.vspmlauncher.data.LocalClientMod
import ru.artem.alaverdyan.vspmlauncher.data.LocalModSource
import ru.artem.alaverdyan.vspmlauncher.network.ClientModEntryDto
import ru.artem.alaverdyan.vspmlauncher.network.LauncherApi
import ru.artem.alaverdyan.vspmlauncher.network.ModrinthApi
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import java.io.File
import java.security.MessageDigest

@Serializable
private data class LocalEntry(val fileName: String, val source: String)

@Serializable
private data class ServerEntry(val fileName: String, val sha256: String)

@Serializable
private data class ManagedState(
    val local: MutableList<LocalEntry> = mutableListOf(),
    val server: MutableMap<String, ServerEntry> = mutableMapOf()
)

private val STATE_FILE: File get() = AppPaths.clientModsStateFile()

data class ClientModsSyncResult(
    val installed: List<String> = emptyList(),
    val removed: List<String> = emptyList(),
    val failed: List<Pair<String, String>> = emptyList()
) {
    val hasFailures get() = failed.isNotEmpty()
}

object ClientModsManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun loadState(): ManagedState {
        if (!STATE_FILE.exists()) return ManagedState()
        return runCatching { json.decodeFromString<ManagedState>(STATE_FILE.readText()) }.getOrDefault(ManagedState())
    }

    private fun saveState(state: ManagedState) {
        STATE_FILE.parentFile?.mkdirs()
        STATE_FILE.writeText(json.encodeToString(state))
    }

    fun listInstalled(): List<LocalClientMod> {
        val state = loadState()
        var changed = false
        val result = state.local.mapNotNull { entry ->
            val jar = File(CLIENT_MODS_DIR, entry.fileName)
            if (!jar.exists()) {
                changed = true
                return@mapNotNull null
            }
            toLocalMod(jar).copy(
                source = if (entry.source == "MODRINTH") LocalModSource.MODRINTH else LocalModSource.LOCAL_FILE
            )
        }.sortedBy { it.name }
        if (changed) {
            state.local.retainAll { File(CLIENT_MODS_DIR, it.fileName).exists() }
            saveState(state)
        }
        return result
    }

    private fun toLocalMod(jar: File): LocalClientMod {
        val parsed = ModJarInspector.inspect(jar)
        return LocalClientMod(
            id = parsed?.id ?: jar.nameWithoutExtension,
            name = parsed?.name ?: jar.nameWithoutExtension,
            description = parsed?.description ?: "",
            jarFile = jar,
            source = LocalModSource.LOCAL_FILE
        )
    }

    suspend fun addFromModrinth(downloadUrl: String, fileName: String): LocalClientMod = withContext(Dispatchers.IO) {
        CLIENT_MODS_DIR.mkdirs()
        val target = File(CLIENT_MODS_DIR, fileName)
        ModrinthApi.downloadTo(downloadUrl, target)
        val state = loadState()
        state.local.removeAll { it.fileName == fileName }
        state.local.add(LocalEntry(fileName, "MODRINTH"))
        saveState(state)
        toLocalMod(target).copy(source = LocalModSource.MODRINTH)
    }

    fun addFromLocalFile(sourceFile: File): LocalClientMod {
        CLIENT_MODS_DIR.mkdirs()
        val target = File(CLIENT_MODS_DIR, sourceFile.name)
        sourceFile.copyTo(target, overwrite = true)
        val state = loadState()
        state.local.removeAll { it.fileName == sourceFile.name }
        state.local.add(LocalEntry(sourceFile.name, "LOCAL_FILE"))
        saveState(state)
        return toLocalMod(target).copy(source = LocalModSource.LOCAL_FILE)
    }

    fun remove(mod: LocalClientMod) {
        mod.jarFile.delete()
        val state = loadState()
        state.local.removeAll { it.fileName == mod.jarFile.name }
        saveState(state)
    }

    suspend fun hasPendingChanges(serverMods: List<ClientModEntryDto>, enabledModIds: Set<String>): Boolean =
        withContext(Dispatchers.IO) {
            val state = loadState()
            val required = serverMods.filter { it.id in enabledModIds }
            val requiredIds = required.map { it.id }.toSet()

            if (state.server.keys.any { it !in requiredIds }) return@withContext true

            required.any { mod ->
                val current = state.server[mod.id]
                current == null || current.sha256 != mod.sha256 || !File(CLIENT_MODS_DIR, mod.fileName).exists()
            }
        }


    suspend fun sync(
        serverMods: List<ClientModEntryDto>,
        enabledModIds: Set<String>,
        onProgress: (DownloadProgress) -> Unit = {}
    ): ClientModsSyncResult =
        withContext(Dispatchers.IO) {
            CLIENT_MODS_DIR.mkdirs()
            val state = loadState()
            val removed = mutableListOf<String>()
            val installed = mutableListOf<String>()
            val failed = mutableListOf<Pair<String, String>>()

            val toRemove = state.server.keys.filter { it !in enabledModIds || serverMods.none { m -> m.id == it } }
            toRemove.forEach { id ->
                state.server.remove(id)?.let { entry ->
                    File(CLIENT_MODS_DIR, entry.fileName).delete()
                    removed += entry.fileName
                }
            }

            val toInstall = serverMods.filter { it.id in enabledModIds }
            val pending = toInstall.filter { mod ->
                val target = File(CLIENT_MODS_DIR, mod.fileName)
                val current = state.server[mod.id]
                !target.exists() || current?.sha256 != mod.sha256 ||
                        runCatching { sha256Of(target) }.getOrNull() != mod.sha256
            }

            pending.forEachIndexed { index, mod ->
                val target = File(CLIENT_MODS_DIR, mod.fileName)
                val current = state.server[mod.id]

                runCatching {
                    if (current != null && current.fileName != mod.fileName) {
                        File(CLIENT_MODS_DIR, current.fileName).delete()
                    }
                    withRetry("скачивание клиентского мода ${mod.fileName}") {
                        LauncherApi.downloadClient.prepareGet(LauncherApi.resolveUrl(mod.url)).execute { response ->
                            val channel = response.bodyAsChannel()
                            val buffer = ByteArray(64 * 1024)
                            var downloaded = 0L
                            target.outputStream().use { output ->
                                while (true) {
                                    val read = channel.readAvailable(buffer, 0, buffer.size)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    onProgress(
                                        DownloadProgress(
                                            phase = ProgressPhase.MODS,
                                            currentFile = mod.fileName,
                                            fileIndex = index + 1,
                                            totalFiles = pending.size,
                                            downloadedBytes = downloaded,
                                            totalBytes = mod.size,
                                            bytesPerSecond = 0L
                                        )
                                    )
                                }
                            }
                        }
                    }
                    check(sha256Of(target) == mod.sha256) {
                        "sha256 не совпал после скачивания (${mod.fileName})"
                    }
                    state.server[mod.id] = ServerEntry(fileName = mod.fileName, sha256 = mod.sha256)
                    installed += mod.fileName
                }.onFailure { e ->
                    target.delete()
                    failed += (mod.name to (e.message ?: e.toString()))
                }
            }

            saveState(state)
            ClientModsSyncResult(installed, removed, failed)
        }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}