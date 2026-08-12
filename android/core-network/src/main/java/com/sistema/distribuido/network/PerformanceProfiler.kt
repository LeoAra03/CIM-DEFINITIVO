package com.sistema.distribuido.network

import android.util.Log

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
        val start = System.currentTimeMillis()
        try {
            return block()
        } finally {
            val time = System.currentTimeMillis() - start
            if (time > THRESHOLD_MS) {
                safeWarn("LATENCIA DETECTADA: $name tomó ${time}ms")
            } else {
                safeDebug("PROF: $name -> ${time}ms")
            }
        }
    }

    /**
     * Versión simplificada para logs industriales.
     */
    fun logExecution(name: String, time: Long) {
        if (time > THRESHOLD_MS) {
            safeError("CRITICAL DELAY: $name ($time ms)")
        }
    }

    fun safeDebug(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: Throwable) {
            // android.util.Log is not available in plain JVM unit tests.
        }
    }

    fun safeWarn(message: String) {
        try {
            Log.w(TAG, message)
        } catch (_: Throwable) {
            // android.util.Log is not available in plain JVM unit tests.
        }
    }

    fun safeError(message: String) {
        try {
            Log.e(TAG, message)
        } catch (_: Throwable) {
            // android.util.Log is not available in plain JVM unit tests.
        }
    }
}
