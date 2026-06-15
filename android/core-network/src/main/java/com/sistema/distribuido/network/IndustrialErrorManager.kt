package com.sistema.distribuido.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlin.system.exitProcess

/**
 * Manager de Errores de Grado Industrial.
 * Diseñado para evitar que la aplicación se cierre ante excepciones no controladas
 * y proporcionar recuperación automática de estados.
 */
object IndustrialErrorManager {

    private var isInitialized = false

    fun install(context: Context, onRecover: () -> Unit) {
        if (isInitialized) return
        
        // Inicializar Fallback de Procedimientos (TXT)
        ProceduralFallback.initialize(context)
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
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
     * Sanitiza entradas de usuario para evitar inyecciones o caracteres que rompan protocolos
     */
    fun sanitizeInput(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9.\\-_:]"), "").trim()
    }
}
