package ru.artem.alaverdyan.vspmlauncher

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform