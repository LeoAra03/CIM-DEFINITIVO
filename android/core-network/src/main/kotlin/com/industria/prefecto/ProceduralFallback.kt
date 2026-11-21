package com.industria.prefecto

import android.content.Context
import java.io.File

/**
 * Procedure-based recovery system (TXT fallback).
 * When a dynamic function fails, it looks up the local requirements file
 * to run the predefined emergency logic.
 */
object ProceduralFallback {
    
    private const val FALLBACK_FILE = "industrial_requirements.txt"
    private val procedures = mutableMapOf<String, String>()

    /**
     * Loads procedures from a local file or assets.
     */
    fun initialize(context: Context) {
        try {
            val content = context.assets.open(FALLBACK_FILE).bufferedReader().use { it.readText() }
            parseProcedures(content)
        } catch (e: Exception) {
            // If no assets exist, create a default one in cache to avoid a crash
            val defaultContent = """
                # REQUISITOS INDUSTRIALES DE EMERGENCIA
                CMD:HOME=RESET_MOTORS:0;MOVE_TO:0,0,0
                CMD:ABORT=KILL_ALL_PROCESS:1
                NET:FAIL=RECONNECT_ATTEMPT:3
                UI:DELAY=REDUCE_GRAPHICS:TRUE
            """.trimIndent()
            parseProcedures(defaultContent)
        }
    }

    private fun parseProcedures(content: String) {
        content.lines().forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                val parts = line.split("=")
                if (parts.size == 2) {
                    procedures[parts[0].trim()] = parts[1].trim()
                }
            }
        }
    }

    /**
     * Returns a fallback function when the primary one fails.
     */
    fun getBackupAction(failedKey: String): String {
        return procedures[failedKey] ?: "DEFAULT_RECOVERY"
    }

    /**
     * Runs an emergency action when a delay is detected.
     */
    fun executeEmergencyBypass(key: String, onExecute: (String) -> Unit) {
        val action = getBackupAction(key)
        PerformanceProfiler.logExecution("FALLBACK_EXEC: $key", 0)
        onExecute(action)
    }
}
