package com.industria.prefecto

import android.util.Log

/**
 * Monitor de Rendimiento Industrial.
 * Measures execution time of critical functions and detects bottlenecks
 * on low-performance devices.
 */
object PerformanceProfiler {
    const val TAG = "CIM_PERF"
    const val THRESHOLD_MS = 100 // Alerta si una operación UI toma más de 100ms

    /**
     * Times an operation and records it when the threshold is exceeded.
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
     * Simplified version for industrial logs.
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
