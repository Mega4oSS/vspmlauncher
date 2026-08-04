package ru.artem.alaverdyan.vspmlauncher.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.artem.alaverdyan.vspmlauncher.BuildInfo
import ru.artem.alaverdyan.vspmlauncher.data.AnalyticsIdStorage
import ru.artem.alaverdyan.vspmlauncher.runtime.HardwareInfo
import ru.artem.alaverdyan.vspmlauncher.runtime.PlatformInfo

object AnalyticsClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clientId get() = AnalyticsIdStorage.clientId()

    private fun send(
        type: String,
        nickname: String? = null,
        ramMb: Int? = null,
        jreSource: String? = null,
        assetsSource: String? = null,
        runtimeId: String? = null,
        channel: String? = null,
        version: String? = null,
        extra: Map<String, String>? = null
    ) {
        scope.launch {
            LauncherApi.sendEvent(
                ClientEventRequestDto(
                    clientId, type, nickname, ramMb, jreSource, assetsSource,
                    runtimeId, channel, version, extra
                )
            )
        }
    }

    fun trackAppOpen(nickname: String?) = send("app_open", nickname = nickname)
    fun trackAppClose() = send("app_close")
    fun trackLogoClick() = send("logo_click")
    fun trackShipClick() = send("ship_click")
    fun trackSettingsOpened() = send("settings_opened")
    fun trackWindowMoved() = send("window_moved")
    fun trackAdminFormReached() = send("admin_form_reached")
    fun trackNicknameChanged(nickname: String) = send("nickname_changed", nickname = nickname)
    fun trackModrinthDialogOpened() = send("modrinth_dialog_opened")
    fun trackEvent(type: String) = send(type)

    fun trackDownloadCompleted(channel: String, version: String) =
        send("download_completed", channel = channel, version = version)

    fun trackUpdateCompleted(channel: String, version: String) =
        send("update_completed", channel = channel, version = version)

    fun trackClientState(
        nickname: String?, ramMb: Int, jreSource: String, assetsSource: String, runtimeId: String
    ) = send(
        "client_state", nickname = nickname, ramMb = ramMb, jreSource = jreSource,
        assetsSource = assetsSource, runtimeId = runtimeId
    )

    fun trackGameLaunched(timeToFirstLaunchMs: Long? = null) = send(
        "game_launched",
        extra = timeToFirstLaunchMs?.let { mapOf("timeToFirstLaunchMs" to it.toString()) }
    )

    fun trackGameExited(durationMs: Long, exitCode: Int) = send(
        "game_exited",
        extra = mapOf("durationMs" to durationMs.toString(), "exitCode" to exitCode.toString())
    )

    fun trackEnvironment() = send(
        "environment",
        extra = mapOf(
            "os" to PlatformInfo.os,
            "osVersion" to HardwareInfo.osVersion,
            "arch" to PlatformInfo.arch,
            "launcherVersion" to BuildInfo.LAUNCHER_VERSION,
            "launcherJavaVersion" to HardwareInfo.launcherJavaVersion,
            "cpuModel" to HardwareInfo.cpuModel,
            "cpuCores" to HardwareInfo.cpuCores.toString(),
            "gpuModel" to HardwareInfo.gpuModel,
            "ramTotalMb" to HardwareInfo.ramTotalMb.toString(),
            "screenWidth" to HardwareInfo.screenInfo.first.toString(),
            "screenHeight" to HardwareInfo.screenInfo.second.toString(),
            "dpiScale" to HardwareInfo.dpiScale.toString()
        )
    )
}