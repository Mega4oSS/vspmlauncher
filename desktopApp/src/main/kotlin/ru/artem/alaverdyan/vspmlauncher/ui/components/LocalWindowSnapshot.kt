package ru.artem.alaverdyan.vspmlauncher.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** Скриншот того, что было под окном (fake-transparency на Linux).
 *  Прокидывается через дерево композиции — экранам не нужно принимать
 *  параметр вручную, достаточно оборачивать App() в Provider ниже. */
val LocalWindowSnapshot = compositionLocalOf<ImageBitmap?> { null }