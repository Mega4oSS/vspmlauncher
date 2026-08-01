package ru.artem.alaverdyan.vspmlauncher.ui

sealed interface Screen {
    data object Onboarding : Screen
    data object Main : Screen
    data object Settings : Screen
    data object Admin : Screen
}