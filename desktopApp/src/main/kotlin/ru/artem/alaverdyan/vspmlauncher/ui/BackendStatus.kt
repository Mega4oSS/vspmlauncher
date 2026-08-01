package ru.artem.alaverdyan.vspmlauncher.ui

/**
 * Статус связи с бэкендом — управляет баннером между новостями и блоком версии/кнопки.
 */
sealed interface BackendStatus {
    data object Ok : BackendStatus
    data class Maintenance(val message: String) : BackendStatus
    data class Unavailable(val message: String) : BackendStatus
}