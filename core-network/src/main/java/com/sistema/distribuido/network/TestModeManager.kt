package com.sistema.distribuido.network

/**
 * Manager de Modo Test para validación sin hardware.
 * Permite a los desarrolladores y tesistas probar la lógica de la UI y Red
 * simulando respuestas positivas del hardware.
 */
object TestModeManager {
    var isEnabled: Boolean = false

    fun toggle() {
        isEnabled = !isEnabled
        android.util.Log.i("CIM_TEST", "MODO TEST: ${if(isEnabled) "ACTIVADO" else "DESACTIVADO"}")
    }

    /**
     * Simula una respuesta de red si el modo test está activo.
     */
    fun simulateResponse(command: String, onResponse: (String) -> Unit): Boolean {
        if (isEnabled) {
            android.util.Log.d("CIM_TEST", "Simulando respuesta para: $command")
            when {
                command.contains("VALIDATE") -> onResponse("VALIDADO;SUCCESS")
                command.contains("STATUS") -> onResponse("STATUS;UUID;IDLE")
                else -> onResponse("ACK;RECEIVED")
            }
            return true
        }
        return false
    }
}
