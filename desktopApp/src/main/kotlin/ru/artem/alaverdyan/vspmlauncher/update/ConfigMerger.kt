package ru.artem.alaverdyan.vspmlauncher.update

import ru.artem.alaverdyan.vspmlauncher.network.ConfigPatchDto
import java.io.File

data class ConflictInfo(
    val path: String,
    val key: String,
    val localValue: String?,      // что реально было у игрока на диске
    val serverOldValue: String?,  // что ожидалось (значение в fromVersion на сервере)
    val serverNewValue: String?   // чем принудительно заменили (значение в toVersion на сервере)
)

/**
 * Накладывает патч на локальный файл, не трогая ключи, которых патч не касается —
 * локальные правки игрока/мода вне изменённых сервером ключей сохраняются как есть.
 * Если по конкретному ключу локальное значение разошлось с тем, что сервер ожидал
 * увидеть (fromVersion), это конфликт — но патч всё равно применяется, побеждает сервер.
 */
object ConfigMerger {

    fun applyPatch(localFile: File, patch: ConfigPatchDto): List<ConflictInfo> {
        val original = localFile.readText()
        val localState = ConfigFormats.parse(patch.path, original)
            ?: return emptyList() // локальный файл вдруг не распарсился — не мёрджим, оставляем как есть

        val conflicts = mutableListOf<ConflictInfo>()
        val newValues = localState.toMutableMap()

        patch.keyDiffs.forEach { kd ->
            val localValue = localState[kd.key]
            if (localValue != kd.oldValue) {
                conflicts += ConflictInfo(patch.path, kd.key, localValue, kd.oldValue, kd.newValue)
            }
            if (kd.newValue == null) newValues.remove(kd.key) else newValues[kd.key] = kd.newValue
        }

        localFile.writeText(ConfigFormats.serialize(patch.path, original, newValues))
        return conflicts
    }
}
