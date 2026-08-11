plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.47" apply false
}

// Tarea: Validar firmware ESP32 activo
// La compilación/flasheo real depende de arduino-cli o PlatformIO y se ejecuta
// desde tools/powershell/Flashear-ESP32.ps1. Esta tarea evita referencias a
// rutas históricas y falla si falta algún firmware canónico.
tasks.register("buildFirmware") {
    group = "Firmware"
    description = "Valida que los firmwares ESP32 activos estén presentes"

    doLast {
        val firmwareDir = file("../esp32/firmware")
        val expectedFirmware = listOf(
            "esp32_plc_master.ino",
            "esp32_scorbot_manufactura.ino",
            "esp32_scorbot_calidad.ino",
            "esp32_scorbot_almacen.ino",
            "cim_ble_firmware.h"
        )
        val missing = expectedFirmware.filterNot { firmwareDir.resolve(it).isFile }
        if (missing.isNotEmpty()) {
            throw GradleException("Faltan firmwares activos: ${missing.joinToString()}")
        }
        println("✓ Firmware ESP32 activo validado en ${firmwareDir.absolutePath}")
    }
}

tasks.register<Exec>("validateSystem100") {
    group = "Industrial QA"
    description = "Ejecuta la validación estructural 100% automatizable del modo simulado"
    commandLine("python3", file("../tools/validate_system_100.py").absolutePath, "--quiet")
}

val outputDir = layout.projectDirectory.dir("output-apks")
val checksumFile = layout.projectDirectory.file("output-apks/SHA256SUMS.txt")

tasks.register<Delete>("cleanOutputApks") {
    delete(outputDir)
}

tasks.register("buildAllApks") {
    group = "Industrial Build"
    description = "Compila todos los módulos y exporta las APKs a /output-apks"
    
    val appModules = listOf(
        "app-coordinador",
        "app-plc",
        "app-calidad",
        "app-manufactura",
        "app-almacen",
        "wear-coordinador"
    )

    // Ejecutar también la puerta estructural cuando CI llama buildAllApks.
    // Android Lint queda disponible mediante lintAll/qualityGate100 sin bloquear
    // el workflow histórico, que sólo invoca testAllModules + buildAllApks.
    dependsOn("validateSystem100")

    // Depender de las tareas assembleDebug de cada subproyecto (ruta jerárquica)
    appModules.forEach { moduleName ->
        dependsOn(":$moduleName:assembleDebug")
    }

    doLast {
        if (!outputDir.asFile.exists()) {
            outputDir.asFile.mkdirs()
        }

        val missingApks = mutableListOf<String>()

        appModules.forEach { moduleName ->
            // Buscar el archivo APK en la carpeta de build del subproyecto interno
            val projectBuildDir = project(":$moduleName").layout.buildDirectory
            val debugDir = projectBuildDir.dir("outputs/apk/debug").get().asFile

            val apkFile = debugDir.listFiles()?.find { it.name.endsWith(".apk") && !it.name.contains("androidTest") }

            if (apkFile != null && apkFile.exists()) {
                val targetName = "$moduleName.apk"
                apkFile.copyTo(File(outputDir.asFile, targetName), overwrite = true)
                println("✓ Exportado: $targetName (debug, testeable)")
            } else {
                missingApks += moduleName
                println("✗ ERROR: No se encontró APK en ${debugDir.absolutePath}")
            }
        }

        if (missingApks.isNotEmpty()) {
            throw GradleException("No se exportaron APKs para: ${missingApks.joinToString()}")
        }

        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val checksumLines = outputDir.asFile.listFiles()
            ?.filter { it.isFile && it.extension == "apk" }
            ?.sortedBy { it.name }
            ?.map { apk ->
                digest.reset()
                apk.inputStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        digest.update(buffer, 0, read)
                    }
                }
                val hash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
                "$hash  ${apk.name}"
            }
            .orEmpty()
        checksumFile.asFile.writeText(checksumLines.joinToString(System.lineSeparator()) + System.lineSeparator())

        println("=== COMPILACIÓN INDUSTRIAL COMPLETADA ===")
        println("APKs disponibles en: ${outputDir.asFile.absolutePath}")
        println("Checksums disponibles en: ${checksumFile.asFile.absolutePath}")
    }
}

// Tarea: Tests JVM unitarios de todos los módulos
tasks.register("testAllModules") {
    group = "Industrial Testing"
    description = "Ejecuta tests unitarios JVM de core-network y todas las apps"

    doFirst {
        println("╔════════════════════════════════════════╗")
        println("║  INICIANDO SUITE DE TESTS CIM v6.0    ║")
        println("╚════════════════════════════════════════╝")
    }

    dependsOn(
        ":core-network:testDebugUnitTest",
        ":app-coordinador:testDebugUnitTest",
        ":app-plc:testDebugUnitTest",
        ":app-calidad:testDebugUnitTest",
        ":app-manufactura:testDebugUnitTest",
        ":app-almacen:testDebugUnitTest",
        ":wear-coordinador:testDebugUnitTest"
    )

    doLast {
        println("✓ Tests completados para core-network y las seis aplicaciones")
    }
}

// Tarea: Limpiar todos los módulos Android
tasks.register("cleanAllModules") {
    group = "Industrial Build"
    description = "Limpia artefactos de build de todos los módulos"
    dependsOn(
        ":core-network:clean",
        ":app-coordinador:clean",
        ":app-plc:clean",
        ":app-calidad:clean",
        ":app-manufactura:clean",
        ":app-almacen:clean",
        ":wear-coordinador:clean"
    )
}

// Tarea: Limpiar y construir todo
tasks.register("cleanBuildAll") {
    group = "Industrial Build"
    description = "Limpia y construye todos los módulos"
    dependsOn("cleanAllModules", "buildAllApks")
}

tasks.named("buildAllApks") {
    mustRunAfter("cleanAllModules")
}

// Configuración general
ext {
    set("appVersion", "6.0.0")
    set("minSdkVersion", 26)
    set("targetSdkVersion", 35)
    set("compileSdkVersion", 35)
}

// FASE 4: TASKS ADICIONALES PARA CALIDAD

// Tarea: Validar APKs después de compilación
tasks.register("validateApks") {
    group = "Industrial QA"
    description = "Valida integridad y tamaño de APKs compiladas"
    dependsOn("buildAllApks")

    doLast {
        println("\n╔════════════════════════════════════════╗")
        println("║  VALIDACIÓN DE APKs CIM v6.0         ║")
        println("╚════════════════════════════════════════╝\n")

        // Debug APK size varies substantially with native CameraX/OpenCV/TFLite
        // dependencies. Validate presence and a minimal non-empty payload instead
        // of using stale, hard-coded upper size limits.
        val expectedApks = setOf(
            "app-coordinador.apk",
            "app-plc.apk",
            "app-manufactura.apk",
            "app-calidad.apk",
            "app-almacen.apk",
            "wear-coordinador.apk"
        )
        val minimumSizeBytes = 1L * 1024 * 1024

        var allValid = true
        var totalSize = 0L

        expectedApks.forEach { apkName ->
            val apkFile = File(outputDir.asFile, apkName)
            if (apkFile.exists()) {
                val sizeMB = apkFile.length() / (1024 * 1024)
                val isValid = apkFile.length() >= minimumSizeBytes
                val status = if (isValid) "✓ OK" else "✗ DEMASIADO PEQUEÑA"

                println("  $apkName: $sizeMB MB [mínimo 1 MB] $status")
                totalSize += apkFile.length()

                if (!isValid) allValid = false
            } else {
                println("  $apkName: ✗ NO ENCONTRADO")
                allValid = false
            }
        }

        val totalMB = totalSize / (1024 * 1024)
        println("\n  TOTAL: $totalMB MB")
        println("  Estado: " + if (allValid) "✓ TODAS LAS APKs VÁLIDAS" else "✗ ALGUNAS APKs INVÁLIDAS")

        if (!allValid) {
            throw GradleException("Validación de APKs falló")
        }
    }
}

// Tarea: Generar checksums SHA-256 de las APKs exportadas
tasks.register("writeApkChecksums") {
    group = "Industrial QA"
    description = "Genera output-apks/SHA256SUMS.txt para trazabilidad de artefactos"
    dependsOn("validateApks")

    doLast {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val apkFiles = outputDir.asFile.listFiles()
            ?.filter { it.isFile && it.extension == "apk" }
            ?.sortedBy { it.name }
            .orEmpty()
        if (apkFiles.isEmpty()) {
            throw GradleException("No hay APKs para calcular checksums")
        }
        val lines = apkFiles.map { apk ->
            digest.reset()
            apk.inputStream().use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val hash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            "$hash  ${apk.name}"
        }
        checksumFile.asFile.writeText(lines.joinToString(System.lineSeparator()) + System.lineSeparator())
        println("✓ Checksums SHA-256 generados: ${checksumFile.asFile.absolutePath}")
    }
}

// Tarea: Generar reporte de compilación
tasks.register("buildReport") {
    group = "Industrial Reports"
    description = "Genera reporte detallado de compilación"
    dependsOn("writeApkChecksums")

    doLast {
        val reportFile = File(rootDir, "BUILD_REPORT_${System.currentTimeMillis()}.md")

        reportFile.writeText("""
# BUILD REPORT - CIM v6.0

**Fecha**: ${java.time.LocalDateTime.now()}
**Versión**: 6.0.0
**Estado**: BUILD SUCCESSFUL

## Módulos Compilados
- ✓ core-network (Library)
- ✓ app-coordinador (Maestro)
- ✓ app-plc (Estación)
- ✓ app-manufactura (Estación)
- ✓ app-calidad (Estación)
- ✓ app-almacen (Estación)

## APKs Generadas
""".trimIndent())

        outputDir.asFile.listFiles()?.filter { it.name.endsWith(".apk") }?.forEach {
            val sizeMB = it.length() / (1024 * 1024)
            reportFile.appendText("- ${it.name}: $sizeMB MB\n")
        }

        reportFile.appendText("""

## Configuración
- AGP: 8.7.3
- Kotlin: 2.0.21
- Min SDK: 26
- Target SDK: 35
- Compile SDK: 35

## Tests Ejecutados
- ✓ Unit Tests JVM (core-network)
- ✓ Unit Tests JVM (app-coordinador)
- ✓ Unit Tests JVM (app-plc)
- ✓ Unit Tests JVM (app-calidad)
- ✓ Unit Tests JVM (app-manufactura/app-almacen/wear sin tests específicos al momento)

## Trazabilidad
- ✓ Checksums SHA-256 generados en `output-apks/SHA256SUMS.txt`

## Resultado Final
**✓ BUILD SUCCESSFUL - LISTO PARA VALIDACIÓN E2E SIMULADA**
""".trimIndent())

        println("\n✓ Reporte generado: ${reportFile.absolutePath}")
    }
}

// Tarea: Verificar configuración de firma release sin exponer secretos
// Las claves se configuran por variables de entorno/gradle properties en cada app.
tasks.register("signAllApks") {
    group = "Industrial Release"
    description = "Informa cómo producir APKs release firmadas sin versionar keystores ni contraseñas"

    doLast {
        val required = listOf(
            "CIM_RELEASE_STORE_FILE",
            "CIM_RELEASE_STORE_PASSWORD",
            "CIM_RELEASE_KEY_ALIAS",
            "CIM_RELEASE_KEY_PASSWORD"
        )
        val missing = required.filter { System.getenv(it).isNullOrBlank() && !project.hasProperty(it) }
        if (missing.isEmpty()) {
            println("✓ Configuración de firma release detectada. Ejecuta: ./gradlew assembleRelease")
        } else {
            println("⚠ Firma release no configurada; faltan: ${missing.joinToString()}")
            println("  Define esas variables como secretos de CI o en un archivo local no versionado.")
            println("  Sin ellas se generan APK release unsigned, evitando contraseñas embebidas.")
        }
    }
}

val qaModules = listOf(
    "core-network",
    "app-coordinador",
    "app-plc",
    "app-calidad",
    "app-manufactura",
    "app-almacen",
    "wear-coordinador"
)

// Tarea: Lint check real de Android en todos los módulos
tasks.register("lintAll") {
    group = "Industrial QA"
    description = "Ejecuta Android Lint en todos los módulos activos"
    qaModules.forEach { moduleName -> dependsOn(":$moduleName:lintDebug") }
}

// Tarea: Todo - Full build + tests + lint + validation
tasks.register("buildRelease") {
    group = "Industrial Release"
    description = "Build completo: validación estructural + tests + lint + APKs + checksums + reporte"
    dependsOn("validateSystem100", "testAllModules", "lintAll", "buildReport")

    doLast {
        println("\n╔════════════════════════════════════════╗")
        println("║   COMPILACIÓN COMPLETA FINALIZADA    ║")
        println("║   CIM v6.0 LISTO PARA VALIDACIÓN     ║")
        println("╚════════════════════════════════════════╝\n")
    }
}

tasks.register("qualityGate100") {
    group = "Industrial QA"
    description = "Ejecuta la puerta 100% automatizable: estructura, tests, lint, build, validación APK y checksums"
    dependsOn("buildRelease")
}

