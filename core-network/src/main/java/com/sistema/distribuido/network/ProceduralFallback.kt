package com.sistema.distribuido.network

import android.content.Context
import java.io.File

/**
 * Sistema de Recuperación por Procedimientos (TXT Fallback).
 * Si una función dinámica falla, busca en el archivo local de requerimientos
 * para ejecutar la lógica de emergencia predefinida.
 */
object ProceduralFallback {
    
    private const val FALLBACK_FILE = "industrial_requirements.txt"
    private val procedures = mutableMapOf<String, String>()

    /**
     * Carga los procedimientos desde un archivo local o assets.
     */
    fun initialize(context: Context) {
        try {
            val content = context.assets.open(FALLBACK_FILE).bufferedReader().use { it.readText() }
            parseProcedures(content)
        } catch (e: Exception) {
            // Si no hay assets, crear uno por defecto en cache para evitar crash
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
     * Obtiene una función de respaldo si la principal falla.
     */
    fun getBackupAction(failedKey: String): String {
        return procedures[failedKey] ?: "DEFAULT_RECOVERY"
    }

    /**
     * Ejecuta una acción de emergencia si hay un delay detectado.
     */
    fun executeEmergencyBypass(key: String, onExecute: (String) -> Unit) {
        val action = getBackupAction(key)
        PerformanceProfiler.logExecution("FALLBACK_EXEC: $key", 0)
        onExecute(action)
    }
}
