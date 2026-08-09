package com.sistema.distribuido.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Manager de Errores de Grado Industrial.
 * Diseñado para evitar que la aplicación se cierre ante excepciones no controladas
 * y proporcionar recuperación automática de estados.
 * CORREGIDO: sanitizeInput ahora preserva delimitadores CIM y valida path traversal.
 */
object IndustrialErrorManager {

    private var isInitialized = false

    fun install(context: Context, onRecover: () -> Unit) {
        if (isInitialized) return
        
        // Inicializar Fallback de Procedimientos (TXT)
        ProceduralFallback.initialize(context)
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Registrar error en log industrial de forma segura
            android.util.Log.e("CIM_FATAL", "CRITICAL ERROR en ${thread.name}: ${throwable.message}")
            
            // Intentar ejecutar un procedimiento de emergencia desde el TXT
            ProceduralFallback.executeEmergencyBypass("EVENT:CRITICAL_FAIL") { action ->
                android.util.Log.i("CIM_RECOVERY", "Ejecutando acción de respaldo: $action")
            }

            // Notificar al usuario sin crashear si es posible
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "⚠️ Sistema Industrial: Reiniciando servicios por error crítico", Toast.LENGTH_LONG).show()
                onRecover()
            }
        }
        
        isInitialized = true
    }

    /**
     * Sanitiza entradas de usuario para evitar inyecciones y path traversal,
     * pero preservando delimitadores del protocolo CIM (| ; : , _ - .)
     * CORREGIDO: versión anterior eliminaba '|' y ';' rompiendo handshake.
     */
    fun sanitizeInput(input: String, maxLen: Int = 1024): String {
        if (input.isBlank()) return ""
        // Eliminar solo caracteres de control
        val filtered = input.filter { it.code >= 32 && it.code != 127 }
        val truncated = filtered.take(maxLen).trim()
        require(!truncated.contains("..")) { "Path traversal detectado" }
        return truncated
    }

    fun sanitizeFileName(raw: String, allowedExts: Set<String> = setOf(".gcode", ".nc", ".txt")): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\').substringAfterLast(':')
        val clean = base.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(64)
        require(allowedExts.any { clean.lowercase().endsWith(it) }) { "Extension no permitida: $clean" }
        require(clean.isNotBlank()) { "Nombre vacío tras sanitización" }
        return clean.ifBlank { "file_${System.currentTimeMillis()}.gcode" }
    }

    fun validateGcodeSize(bytes: ByteArray, maxBytes: Int = 5 * 1024 * 1024) {
        require(bytes.size <= maxBytes) { "G-code excede $maxBytes bytes" }
        require(bytes.isNotEmpty()) { "G-code vacío" }
    }
}
