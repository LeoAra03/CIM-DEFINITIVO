package com.industria.prefecto

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Industrial-grade error manager.
 * Designed to keep the application from crashing on unhandled exceptions
 * y proporcionar recuperación automática de estados.
 * sanitizeInput now preserves CIM delimiters and validates path traversal.
 */
object IndustrialErrorManager {

    private var isInitialized = false

    fun install(context: Context, onRecover: () -> Unit) {
        if (isInitialized) return
        
        // Inicializar Fallback de Procedimientos (TXT)
        ProceduralFallback.initialize(context)
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Record the error in the industrial log safely
            android.util.Log.e("CIM_FATAL", "CRITICAL ERROR en ${thread.name}: ${throwable.message}")
            
            // Try to run an emergency procedure from the TXT file
            ProceduralFallback.executeEmergencyBypass("EVENT:CRITICAL_FAIL") { action ->
                android.util.Log.i("CIM_RECOVERY", "Ejecutando acción de respaldo: $action")
            }

            // Notify the user without crashing when possible
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Sistema Industrial: Reiniciando servicios por error crítico", Toast.LENGTH_LONG).show()
                onRecover()
            }
        }
        
        isInitialized = true
    }

    /**
     * Sanitizes user input to prevent injection and path traversal,
     * while preserving CIM protocol delimiters (| ; : , _ - .)
     * The previous version stripped '|' and ';' and broke the handshake.
     */
    fun sanitizeInput(input: String, maxLen: Int = 1024): String {
        if (input.isBlank()) return ""
        // Remove control characters only
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
