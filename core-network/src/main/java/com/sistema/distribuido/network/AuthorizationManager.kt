package com.sistema.distribuido.network

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
