package ru.artem.alaverdyan.vspmlauncher.update

import kotlinx.serialization.json.*

object ConfigFormats {

    private enum class Format { JSON, PROPERTIES }

    fun isMergeablePath(path: String): Boolean {
        if (!path.startsWith("config/")) return false
        return detectFormat(path) != null
    }

    private fun detectFormat(path: String): Format? = when {
        path.endsWith(".json") -> Format.JSON
        path.endsWith(".properties") -> Format.PROPERTIES
        path.endsWith(".txt") -> Format.PROPERTIES
        path.endsWith(".toml") -> Format.PROPERTIES
        else -> null
    }

    fun parse(path: String, content: String): Map<String, String>? {
        val format = detectFormat(path) ?: return null
        return try {
            when (format) {
                Format.JSON -> flattenJson(Json.parseToJsonElement(content))
                Format.PROPERTIES -> parseProperties(content)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun serialize(path: String, original: String, values: Map<String, String>): String {
        val format = detectFormat(path) ?: return original
        return when (format) {
            Format.JSON -> serializeJson(original, values)
            Format.PROPERTIES -> serializeProperties(original, values)
        }
    }

    private fun parseProperties(content: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return@forEach
            val sepIndex = line.indexOfFirst { it == '=' || it == ':' }
            if (sepIndex <= 0) return@forEach
            result[line.substring(0, sepIndex).trim()] = line.substring(sepIndex + 1).trim()
        }
        return result
    }

    private fun serializeProperties(original: String, values: Map<String, String>): String {
        val remaining = values.toMutableMap()
        val lines = original.lines().toMutableList()

        for (i in lines.indices) {
            val trimmed = lines[i].trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue
            val sepIndex = trimmed.indexOfFirst { it == '=' || it == ':' }
            if (sepIndex <= 0) continue
            val key = trimmed.substring(0, sepIndex).trim()
            if (remaining.containsKey(key)) {
                lines[i] = "$key${trimmed[sepIndex]}${remaining[key]}"
                remaining.remove(key)
            }
        }
        remaining.forEach { (k, v) -> lines.add("$k=$v") }
        return lines.joinToString("\n")
    }

    private fun flattenJson(element: JsonElement): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        fun walk(el: JsonElement, path: String) {
            when (el) {
                is JsonObject -> el.entries.forEach { (k, v) -> walk(v, if (path.isEmpty()) k else "$path.$k") }
                is JsonArray -> el.forEachIndexed { i, v -> walk(v, "$path[$i]") }
                is JsonNull -> result[path] = "null"
                is JsonPrimitive -> result[path] = el.content
            }
        }
        walk(element, "")
        return result
    }

    private fun serializeJson(original: String, values: Map<String, String>): String {
        val root = Json.parseToJsonElement(original)
        val patched = applyJson(root, "", values)
        return Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), patched)
    }

    private fun applyJson(element: JsonElement, path: String, values: Map<String, String>): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { (k, v) -> applyJson(v, if (path.isEmpty()) k else "$path.$k", values) })
        is JsonArray -> JsonArray(element.mapIndexed { i, v -> applyJson(v, "$path[$i]", values) })
        else -> values[path]?.let { toJsonPrimitive(it) } ?: element
    }

    private fun toJsonPrimitive(raw: String): JsonPrimitive =
        raw.toLongOrNull()?.let { JsonPrimitive(it) }
            ?: raw.toDoubleOrNull()?.let { JsonPrimitive(it) }
            ?: raw.toBooleanStrictOrNull()?.let { JsonPrimitive(it) }
            ?: JsonPrimitive(raw)
}