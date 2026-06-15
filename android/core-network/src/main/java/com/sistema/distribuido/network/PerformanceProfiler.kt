package com.sistema.distribuido.network

import android.util.Log
import kotlin.system.measureTimeMillis

/**
 * Monitor de Rendimiento Industrial.
 * Permite medir el tiempo de ejecución de funciones críticas y detectar cuellos de botella
 * en dispositivos de bajo rendimiento.
 */
object PerformanceProfiler {
    const val TAG = "CIM_PERF"
    const val THRESHOLD_MS = 100 // Alerta si una operación UI toma más de 100ms

    /**
     * Mide el tiempo de una operación y lo registra si excede el umbral.
     */
    inline fun <T> trace(name: String, block: () -> T): T {
        var result: T
        val time = measureTimeMillis {
            result = block()
        }
        
        if (time > THRESHOLD_MS) {
            Log.w(TAG, "⚠️ LATENCIA DETECTADA: $name tomó ${time}ms")
        } else {
            Log.d(TAG, "PROF: $name -> ${time}ms")
        }
        return result
    }

    /**
     * Versión simplificada para logs industriales.
     */
    fun logExecution(name: String, time: Long) {
        if (time > THRESHOLD_MS) {
            Log.e(TAG, "CRITICAL DELAY: $name ($time ms)")
        }
    }
}
