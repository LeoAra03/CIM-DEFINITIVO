// Plugin versions are provided by config/build-logic (includeBuild),
// which is the single source of truth for the Android toolchain.

tasks.register("testAllModules") {
    group = "verification"
    description = "Runs unit tests for all Android modules."
    dependsOn(
        ":app-coordinador:testDebugUnitTest",
        ":app-plc:testDebugUnitTest",
        ":app-manufactura:testDebugUnitTest",
        ":app-calidad:testDebugUnitTest",
        ":app-almacen:testDebugUnitTest",
        ":core-network:testDebugUnitTest",
    )
}

tasks.register("lintAll") {
    group = "verification"
    description = "Runs Android lint for all application modules."
    dependsOn(
        ":app-coordinador:lintDebug",
        ":app-plc:lintDebug",
        ":app-manufactura:lintDebug",
        ":app-calidad:lintDebug",
        ":app-almacen:lintDebug",
        ":core-network:lintDebug",
    )
}

tasks.register("buildAllApks") {
    group = "build"
    description = "Builds debug and release APKs for all application modules."
    dependsOn(
        ":app-coordinador:assembleDebug",
        ":app-coordinador:assembleRelease",
        ":app-plc:assembleDebug",
        ":app-plc:assembleRelease",
        ":app-manufactura:assembleDebug",
        ":app-manufactura:assembleRelease",
        ":app-calidad:assembleDebug",
        ":app-calidad:assembleRelease",
        ":app-almacen:assembleDebug",
        ":app-almacen:assembleRelease",
        ":wear-coordinador:assembleDebug",
        ":wear-coordinador:assembleRelease",
    )
}

val apkOutputDir = layout.projectDirectory.dir("output-apks")

tasks.register("validateApks") {
    group = "verification"
    description = "Verifies the expected debug and release APK artifacts exist."
    doLast {
        val expected = listOf(
            "app-coordinador-debug.apk",
            "app-coordinador-release.apk",
            "app-plc-debug.apk",
            "app-plc-release.apk",
            "app-manufactura-debug.apk",
            "app-manufactura-release.apk",
            "app-calidad-debug.apk",
            "app-calidad-release.apk",
            "app-almacen-debug.apk",
            "app-almacen-release.apk",
            "wear-coordinador-debug.apk",
            "wear-coordinador-release.apk",
        )
        val missing = expected.filter {
            !apkOutputDir.file(it).asFile.exists()
        }
        require(missing.isEmpty()) { "Missing expected APKs: $missing" }
        logger.lifecycle("Validated ${expected.size} APKs in ${apkOutputDir.asFile}")
    }
}

tasks.register("writeApkChecksums") {
    group = "build"
    description = "Writes SHA-256 checksums for all generated APKs."
    doLast {
        val out = apkOutputDir.file("APK_CHECKSUMS.txt").asFile
        out.parentFile.mkdirs()
        val apks = apkOutputDir.asFile.listFiles { file ->
            file.isFile && file.extension == "apk"
        }?.sortedBy { it.name } ?: emptyList()
        require(apks.isNotEmpty()) { "No APKs found in ${apkOutputDir.asFile}" }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val lines = apks.joinToString(separator = "\n") { apk ->
            val hash = digest.digest(apk.readBytes())
                .joinToString(separator = "") { byte -> "%02x".format(byte) }
            "$hash  ${apk.name}"
        }
        out.writeText(lines + "\n")
        logger.lifecycle("Wrote checksums for ${apks.size} APKs to $out")
    }
}
