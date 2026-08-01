package ru.artem.alaverdyan.vspmlauncher.data

data class ModItem(
    val id: String,
    val name: String,
    val description: String
)

// Стандартный набор клиентских модов — дополняйте под реальный список сервера
val DEFAULT_CLIENT_MODS = listOf(
    ModItem("sodium", "Sodium", "Оптимизация рендеринга"),
    ModItem("lithium", "Lithium", "Оптимизация игровой логики"),
    ModItem("iris", "Iris Shaders", "Поддержка шейдеров"),
    ModItem("modmenu", "Mod Menu", "Управление модами в игре"),
    ModItem("fabric_api", "Fabric API", "Базовая библиотека для модов")
)