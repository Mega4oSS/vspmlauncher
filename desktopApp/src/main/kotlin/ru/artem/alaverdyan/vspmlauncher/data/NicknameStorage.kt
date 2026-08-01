package ru.artem.alaverdyan.vspmlauncher.data

import java.util.prefs.Preferences

object NicknameStorage {
    private val prefs = Preferences.userRoot().node("ru/artem/alaverdyan/vspmlauncher")
    private const val KEY_NICKNAME = "nickname"

    fun load(): String? = prefs.get(KEY_NICKNAME, null)

    fun save(nickname: String) {
        prefs.put(KEY_NICKNAME, nickname)
    }

    fun clear() {
        prefs.remove(KEY_NICKNAME)
    }
}