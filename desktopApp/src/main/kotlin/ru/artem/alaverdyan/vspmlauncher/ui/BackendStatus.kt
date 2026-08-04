package ru.artem.alaverdyan.vspmlauncher.ui

sealed interface BackendStatus {
    data object Ok : BackendStatus
    data class Maintenance(val message: String) : BackendStatus
    data class Unavailable(val message: String) : BackendStatus
}