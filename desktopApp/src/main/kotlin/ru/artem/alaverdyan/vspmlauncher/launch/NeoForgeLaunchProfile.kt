package ru.artem.alaverdyan.vspmlauncher.launch

import kotlinx.serialization.json.*
import java.io.File

data class NeoForgeLaunchProfile(
    val mainClass: String,
    val jvmArgTemplates: List<String>,
    val gameArgTemplates: List<String>,
    val libraryPaths: List<String>
)

object NeoForgeLaunchProfileReader {
    private val json = Json { ignoreUnknownKeys = true }

    fun read(installDir: File): NeoForgeLaunchProfile {
        val file = File(installDir, "version.json")
        if (!file.exists()) error("version.json не найден в $installDir")
        val root = json.parseToJsonElement(file.readText()).jsonObject

        val mainClass = root["mainClass"]?.jsonPrimitive?.content
            ?: error("В version.json нет mainClass")

        val parentJvmArgs = mutableListOf<String>()
        val parentGameArgs = mutableListOf<String>()
        val libraryPaths = mutableListOf<String>()

        root["inheritsFrom"]?.jsonPrimitive?.content?.let { parentId ->
            val parentFile = File(installDir, "versions/$parentId/$parentId.json")
            if (parentFile.exists()) {
                val parentRoot = json.parseToJsonElement(parentFile.readText()).jsonObject
                val parentArgs = parentRoot["arguments"]?.jsonObject
                parentArgs?.get("jvm")?.jsonArray?.let { parentJvmArgs += flattenArgs(it) }
                parentArgs?.get("game")?.jsonArray?.let { parentGameArgs += flattenArgs(it) }
                parentRoot["libraries"]?.jsonArray?.let { libraryPaths += extractLibraryPaths(it) }
            }
        }

        libraryPaths += root["libraries"]?.jsonArray?.let { extractLibraryPaths(it) } ?: emptyList()

        val argumentsObj = root["arguments"]?.jsonObject
        val jvmArgs = parentJvmArgs + (argumentsObj?.get("jvm")?.jsonArray?.let { flattenArgs(it) } ?: emptyList())
        val gameArgs = parentGameArgs + (argumentsObj?.get("game")?.jsonArray?.let { flattenArgs(it) } ?: emptyList())

        val dedupedPaths = LinkedHashMap<String, String>()
        libraryPaths.forEach { path ->
            dedupedPaths[dedupKey(path)] = path
        }

        return NeoForgeLaunchProfile(mainClass, jvmArgs, gameArgs, dedupedPaths.values.toList())
    }

    private fun dedupKey(path: String): String {
        val segs = path.split("/")
        val filename = segs.last().substringBeforeLast(".") // без расширения
        val version = segs.getOrNull(segs.size - 2) ?: return path
        val artifact = segs.getOrNull(segs.size - 3) ?: return path
        val group = segs.take((segs.size - 3).coerceAtLeast(0)).joinToString("/")
        val prefix = "$artifact-$version"
        val classifier = if (filename.startsWith(prefix)) {
            filename.removePrefix(prefix).removePrefix("-").ifEmpty { null }
        } else null
        return "$group/$artifact" + (classifier?.let { ":$it" } ?: "")
    }

    private fun extractLibraryPaths(array: JsonArray): List<String> {
        val result = mutableListOf<String>()
        for (element in array) {
            val lib = element.jsonObject
            val name = lib["name"]?.jsonPrimitive?.content ?: continue
            val artifact = lib["downloads"]?.jsonObject?.get("artifact")?.jsonObject
            val path = artifact?.get("path")?.jsonPrimitive?.content ?: mavenCoordToPath(name)
            result.add(path)
        }
        return result
    }

    private fun mavenCoordToPath(name: String): String {
        val parts = name.split("@", limit = 2)
        val coord = parts[0]
        val ext = parts.getOrElse(1) { "jar" }
        val segs = coord.split(":")
        val group = segs[0]; val artifact = segs[1]; val version = segs[2]
        val classifier = segs.getOrNull(3)
        val filename = "$artifact-$version" + (classifier?.let { "-$it" } ?: "") + ".$ext"
        return "${group.replace('.', '/')}/$artifact/$version/$filename"
    }

    private fun flattenArgs(array: JsonArray): List<String> {
        val currentOs = when {
            System.getProperty("os.name").lowercase().contains("win") -> "windows"
            System.getProperty("os.name").lowercase().contains("mac") -> "osx"
            else -> "linux"
        }
        val result = mutableListOf<String>()
        for (element in array) {
            when (element) {
                is JsonPrimitive -> result.add(element.content)
                is JsonObject -> {
                    val rules = element["rules"]?.jsonArray
                    if (rules != null) {
                        val allowed = rules.all { ruleEl ->
                            val rule = ruleEl.jsonObject
                            val action = rule["action"]?.jsonPrimitive?.content ?: "allow"
                            val osName = rule["os"]?.jsonObject?.get("name")?.jsonPrimitive?.content
                            val features = rule["features"]?.jsonObject
                            when {
                                features != null -> false
                                osName != null -> (osName == currentOs) == (action == "allow")
                                else -> action == "allow"
                            }
                        }
                        if (!allowed) continue
                    }
                    when (val value = element["value"]) {
                        is JsonArray -> value.forEach { result.add(it.jsonPrimitive.content) }
                        is JsonPrimitive -> result.add(value.content)
                        else -> {}
                    }
                }
                else -> {}
            }
        }
        return result
    }
}