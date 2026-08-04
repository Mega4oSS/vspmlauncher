package ru.artem.alaverdyan.vspmlauncher.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

data class LaunchParams(
    val installDir: File,
    val javaBinary: File,
    val ramMb: Int,
    val nickname: String,
    val extraJvmArgs: String = ""
)

data class LaunchResult(
    val process: Process,
    val logFile: File,
    val outputFlow: SharedFlow<String>
)

object GameLauncher {

    fun launch(params: LaunchParams): LaunchResult {
        val meta = GameVersionInfoReader.read(params.installDir)
        val profile = NeoForgeLaunchProfileReader.read(params.installDir)
        val nativesDir = NativesExtractor.ensureExtracted(params.installDir)
        val classpath = ClasspathBuilder.buildClasspathString(params.installDir, meta.mcVersion, profile.libraryPaths)

        val offlineUuid = UUID.nameUUIDFromBytes(
            "OfflinePlayer:${params.nickname}".toByteArray(Charsets.UTF_8)
        )
        val assetsDir = File(params.installDir, "assets")
        val librariesDir = File(params.installDir, "libraries")

        val placeholders = mapOf(
            "auth_player_name" to params.nickname,
            "version_name" to meta.mcVersion,
            "game_directory" to params.installDir.absolutePath,
            "assets_root" to assetsDir.absolutePath,
            "assets_index_name" to meta.assetIndexId,
            "auth_uuid" to offlineUuid.toString().replace("-", ""),
            "auth_access_token" to "0",
            "user_type" to "legacy",
            "version_type" to "ВСПМ 5",
            "natives_directory" to nativesDir.absolutePath,
            "launcher_name" to "ВСПМ",
            "launcher_version" to "5",
            "classpath" to classpath,
            "library_directory" to librariesDir.absolutePath,
            "classpath_separator" to if (System.getProperty("os.name").lowercase().contains("win")) ";" else ":"
        )

        fun substitute(t: String) = placeholders.entries.fold(t) { acc, (k, v) -> acc.replace($$"${$$k}", v) }

        val command = mutableListOf<String>().apply {
            add(params.javaBinary.absolutePath)
            add("-Xmx${params.ramMb}M")
            add("-Xms${(params.ramMb / 2).coerceAtLeast(512)}M")
            if (params.extraJvmArgs.isNotBlank()) {
                addAll(params.extraJvmArgs.trim().split(Regex("\\s+")))
            }
            addAll(profile.jvmArgTemplates.map(::substitute))
            add(profile.mainClass)
            addAll(profile.gameArgTemplates.map(::substitute))
        }

        val logsDir = File(params.installDir, "logs/launcher")
        logsDir.mkdirs()
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
        val logFile = File(logsDir, "launch_$stamp.log")

        File(logsDir, "last_command_$stamp.txt").writeText(command.joinToString("\n"))

        val process = ProcessBuilder(command)
            .directory(params.installDir)
            .redirectErrorStream(true)
            .start()

        val outputFlow = MutableSharedFlow<String>(replay = 500, extraBufferCapacity = 2000)

        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            logFile.bufferedWriter().use { writer ->
                process.inputStream.bufferedReader().forEachLine { line ->
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                    outputFlow.tryEmit(line)
                }
            }
        }

        return LaunchResult(process, logFile, outputFlow.asSharedFlow())
    }
}