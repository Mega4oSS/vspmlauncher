package ru.artem.alaverdyan.vspmlauncher.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.zip.ZipFile

data class ParsedModInfo(val id: String, val name: String, val description: String)

object ModJarInspector {
    private val json = Json { ignoreUnknownKeys = true }

    fun inspect(jar: File): ParsedModInfo? = runCatching {
        ZipFile(jar).use { zip ->
            zip.getEntry("fabric.mod.json")?.let { entry ->
                val obj = json.parseToJsonElement(zip.getInputStream(entry).bufferedReader().readText()).jsonObject
                return@use ParsedModInfo(
                    id = obj["id"]?.jsonPrimitive?.content ?: jar.nameWithoutExtension,
                    name = obj["name"]?.jsonPrimitive?.content ?: jar.nameWithoutExtension,
                    description = obj["description"]?.jsonPrimitive?.content ?: ""
                )
            }
            zip.getEntry("META-INF/mods.toml")?.let { entry ->
                val text = zip.getInputStream(entry).bufferedReader().readText()
                fun field(key: String) = Regex("$key\\s*=\\s*\"([^\"]*)\"").find(text)?.groupValues?.get(1)
                return@use ParsedModInfo(
                    id = field("modId") ?: jar.nameWithoutExtension,
                    name = field("displayName") ?: jar.nameWithoutExtension,
                    description = field("description") ?: ""
                )
            }
            null
        }
    }.getOrNull()
}