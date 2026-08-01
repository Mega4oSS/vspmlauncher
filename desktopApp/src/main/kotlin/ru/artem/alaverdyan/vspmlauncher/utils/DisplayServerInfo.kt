package ru.artem.alaverdyan.vspmlauncher.utils

object DisplayServerInfo {
    val isWayland: Boolean by lazy {
        System.getenv("WAYLAND_DISPLAY") != null ||
            System.getenv("XDG_SESSION_TYPE")?.lowercase() == "wayland"
    }
}