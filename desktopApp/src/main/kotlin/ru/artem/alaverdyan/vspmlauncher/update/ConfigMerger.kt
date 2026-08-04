package ru.artem.alaverdyan.vspmlauncher.update

import ru.artem.alaverdyan.vspmlauncher.network.ConfigPatchDto
import java.io.File

data class ConflictInfo(
    val path: String,
    val key: String,
    val localValue: String?,
    val serverOldValue: String?,
    val serverNewValue: String?
)

object ConfigMerger {

    fun applyPatch(localFile: File, patch: ConfigPatchDto): List<ConflictInfo> {
        val original = localFile.readText()
        val localState = ConfigFormats.parse(patch.path, original)
            ?: return emptyList()

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
