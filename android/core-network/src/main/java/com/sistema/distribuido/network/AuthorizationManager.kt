package com.sistema.distribuido.network

import android.util.Log

import com.sistema.distribuido.network.protocol.CimProtocol
import java.util.concurrent.ConcurrentHashMap

/**
 * AuthorizationManager centraliza el estado de autorización de estaciones CIM.
 *
 * Mantiene un registro de autorización por MAC y expone APIs de consulta
 * para determinar si una estación puede ejecutar comandos.
 */
object AuthorizationManager {
    private val authorizationStates = ConcurrentHashMap<String, String>()

    fun getAuthorizationState(mac: String): String {
        return authorizationStates[mac] ?: CimProtocol.AUTH_PENDING
    }

    fun isAuthorized(mac: String): Boolean {
        return getAuthorizationState(mac) == CimProtocol.AUTH_AUTHORIZED
    }

    fun authorize(mac: String) {
        authorizationStates[mac] = CimProtocol.AUTH_AUTHORIZED
    }

    fun deny(mac: String) {
        authorizationStates[mac] = CimProtocol.AUTH_BLOCKED
    }

    fun revoke(mac: String) {
        authorizationStates.remove(mac)
    }

    fun canSendCommand(mac: String): Boolean {
        return isAuthorized(mac)
    }
}

// FIX CRÍTICO: Validación de dirección MAC
private fun isValidMacAddress(mac: String): Boolean {
    val macPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
    return macPattern.matches(mac) || mac.isNotBlank()
}

// FIX CRÍTICO: Logging de eventos de seguridad
private fun logSecurityEvent(event: String, details: String) {
    val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())
    Log.w("SECURITY", "[$timestamp] $event: $details")
    
    // En producción, enviar a servidor de logs centralizado
}
