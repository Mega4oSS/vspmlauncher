package ru.artem.alaverdyan.vspmlauncher.data

import java.util.UUID
import java.util.prefs.Preferences

object AnalyticsIdStorage {
    private val prefs = Preferences.userRoot().node("ru/artem/alaverdyan/vspmlauncher")
    private const val KEY_CLIENT_ID = "analytics_client_id"

    fun clientId(): String {
        val existing = prefs.get(KEY_CLIENT_ID, null)
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        prefs.put(KEY_CLIENT_ID, generated)
        return generated
    }
}