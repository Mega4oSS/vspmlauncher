import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.0.20" // TODO: свериться с версией Kotlin из libs.plugins.kotlinJvm
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(compose.materialIconsExtended)
    implementation(libs.compose.uiToolingPreview)
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-cio:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("io.ktor:ktor-client-okhttp:3.3.3")
    implementation("media.kamel:kamel-image-default:1.0.9")
}

kotlin {
    jvmToolchain(22)
}

compose.desktop {
    application {
        mainClass = "ru.artem.alaverdyan.vspmlauncher.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            // Имя, которое видит пользователь (exe/ярлык/папка установки) —
            // НЕ то же самое, что application ID. Без точек, покороче.
            packageName = "VSPM Launcher"
            packageVersion = "1.0.0" // строго X.Y.Z, без суффиксов типа "-beta" — иначе MSI не соберётся

            description = "Лаунчер сервера ВСПМ 5"
            copyright = "© 2026 Artem Alaverdyan"
            vendor = "Artem Alaverdyan"

            windows {
                // Нужен мультиразмерный .ico (16/32/48/256), не голый 32x32 —
                // иначе Windows будет растягивать иконку там, где нужен крупный размер.
                iconFile.set(project.file("logo/icon.ico"))

                menuGroup = "ВСПМ 5"
                shortcut = true          // ярлык на рабочем столе
                dirChooser = true        // разрешить выбрать папку установки
                perUserInstall = true    // ставится в %LOCALAPPDATA%, без прав администратора
                console = false          // true — только для отладки, покажет консольное окно

                // Сгенерировать ОДИН раз (например: [System.Guid]::NewGuid() в PowerShell
                // или любой online GUID generator) и закоммитить навсегда.
                // Менять НЕЛЬЗЯ между релизами — иначе апгрейд сломается,
                // Windows будет ставить каждую версию как отдельное приложение.
                upgradeUuid = "513f7603-b6b9-4515-9655-08c62bd47dd2"
            }

            macOS {
                iconFile.set(project.file("logo/icon.icns"))
                bundleID = "ru.artem.alaverdyan.vspmlauncher"
                // Без Apple Developer ID / code signing / notarization
                // Gatekeeper покажет предупреждение "unidentified developer"
                // при первом запуске на чужих машинах. Актуально только
                // если планируется официальный релиз под macOS.
            }

            linux {
                iconFile.set(project.file("logo/icon-512.png")) // обычный png, 512x512+
                packageName = "vspmlauncher"
                menuGroup = "ВСПМ 5"
                debMaintainer = "mega4osss@gmail.com" // TODO: заменить на реальный контакт
            }
        }

        buildTypes.release {
            proguard {
                isEnabled.set(false) // Отключено для проверки — не забыть включить перед релизом
            }
        }
    }
}

// --- Линукс-специфичный фикс: LD_PRELOAD для libfreetype ---
// jpackage не даёт задать env-переменные напрямую через DSL, поэтому
// патчим сгенерированный launcher-скрипт после сборки app-image / deb.
// Актуально только на Linux — задача сама не пересобирается на других ОС,
// т.к. соответствующие таски createDistributable/packageDeb там просто не существуют
// в нужном виде, а finalizedBy молча ничего не делает при отсутствии совпадений.

val ldPreloadPath = "/usr/lib64/libfreetype.so.6.20.2"

fun patchLauncherScript(binDir: File, packageName: String) {
    val script = binDir.resolve(packageName)
    if (!script.exists()) {
        logger.warn("LD_PRELOAD patch: launcher script не найден по пути ${script.absolutePath}, пропускаю")
        return
    }
    val original = script.readText()
    if ("LD_PRELOAD" in original) {
        logger.lifecycle("LD_PRELOAD patch: уже применён к ${script.name}, пропускаю")
        return
    }
    // Вставляем export сразу после шебанга (#!/bin/sh и т.п.)
    val lines = original.lines().toMutableList()
    val shebangIndex = lines.indexOfFirst { it.startsWith("#!") }
    val insertAt = if (shebangIndex >= 0) shebangIndex + 1 else 0
    lines.add(insertAt, "export LD_PRELOAD=$ldPreloadPath")
    script.writeText(lines.joinToString("\n"))
    script.setExecutable(true)
    logger.lifecycle("LD_PRELOAD patch: применён к ${script.absolutePath}")
}

tasks.register("patchLinuxLauncherEnv") {
    doLast {
        // app-image (createDistributable) — путь вида build/compose/binaries/main/app/<packageName>/bin/<packageName>
        val appImageBin = layout.buildDirectory
            .dir("compose/binaries/main/app/VSPM Launcher/bin")
            .get().asFile
        patchLauncherScript(appImageBin, "VSPM Launcher")
    }
}

// Автоматически патчим после сборки app-image и после сборки .deb
// (deb собирается ИЗ app-image, поэтому патч app-image "наследуется" в пакет,
// если patchLinuxLauncherEnv отработает раньше упаковки — см. ordering ниже).
afterEvaluate {
    tasks.findByName("createDistributable")?.finalizedBy("patchLinuxLauncherEnv")
    tasks.findByName("packageDeb")?.let { debTask ->
        tasks.findByName("patchLinuxLauncherEnv")?.let { patchTask ->
            debTask.dependsOn(patchTask)
        }
    }
}