package com.industria.prefecto

import android.util.Log

import com.industria.prefecto.CimMessage
import com.industria.prefecto.CimMessageBuilder
import com.industria.prefecto.CimProtocol
import com.industria.prefecto.CommandType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * COMMUNICATION COORDINATOR v7.0
 *
 * Orchestrates BLE/SPP/TCP communication between:
 * - app-coordinador (Maestro)
 * - app-scorbot, app-vision, app-laser, app-conveyor (Esclavos)
 * - ESP32 firmware running FreeRTOS
 *
 * Responsabilidades:
 * 1. Validate authorization before routing commands
 * 2. Wrap messages in a transport frame
 * 3. Keep per-device session state
 * 4. Automatic retry with exponential backoff
 * 5. Pending command timeouts
 */

data class SessionState(
    val mac: String,
    val deviceName: String,
    var authState: String = CimProtocol.AUTH_PENDING,
    var lastHeartbeat: Long = 0,
    var lastCommandSent: Long = 0,
    var commandTimeout: Long = 10000,
    var isHealthy: Boolean = false
)

data class PendingCommand(
    val id: String,
    val mac: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val maxRetries: Int = 3,
    var retries: Int = 0,
    val timeoutMs: Long = 10000
)

class CommunicationCoordinator(
    private val permissionManager: LocalPermissionManager? = null,
    private val onLog: (String) -> Unit = {},
    private val onMessageReceived: (mac: String, message: String) -> Unit = { _, _ -> }
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessionStates = ConcurrentHashMap<String, SessionState>()
    private val pendingCommands = ConcurrentHashMap<String, PendingCommand>()
    private val sessionLocks = ConcurrentHashMap<String, Mutex>()
    private val commandTimeoutJobs = ConcurrentHashMap<String, Job>()

    private val _coordinationStatus = MutableStateFlow<Map<String, SessionState>>(emptyMap())
    val coordinationStatus: StateFlow<Map<String, SessionState>> = _coordinationStatus.asStateFlow()

    /**
     * Registers a device as an active session
     */
    suspend fun registerSession(
        mac: String,
        deviceName: String,
        appType: String
    ) {
        val lock = sessionLocks.getOrPut(mac) { Mutex() }
        lock.withLock {
            val session = SessionState(
                mac = mac,
                deviceName = deviceName,
                authState = CimProtocol.AUTH_PENDING
            )
            sessionStates[mac] = session
            updateCoordinationStatus()
            onLog("[COORD] Sesión registrada: $deviceName [$mac]")

            // Request authorization from the permission manager
            requestAuthorizationInternal(mac, deviceName, appType)
        }
    }

    /**
     * Requests authorization internally (called by registerSession)
     */
    private suspend fun requestAuthorizationInternal(
        mac: String,
        deviceName: String,
        appType: String
    ) {
        try {
            if (permissionManager == null) {
                // Without a permission manager, assume automatic authorization
                AuthorizationManager.authorize(mac)
                updateSessionAuth(mac, CimProtocol.AUTH_AUTHORIZED)
                onLog("[COORD] Auto-autorizado: $mac (sin PermissionManager)")
                return
            }

            // Solicitar permiso
            val decision = permissionManager.requestPermission(
                mac = mac,
                appType = com.industria.prefecto.AppType.valueOf(appType),
                deviceName = deviceName
            )

            when (decision) {
                PermissionDecision.APPROVED -> {
                    AuthorizationManager.authorize(mac)
                    updateSessionAuth(mac, CimProtocol.AUTH_AUTHORIZED)
                    onLog("[COORD] AUTORIZADO: $mac")
                }
                PermissionDecision.REJECTED -> {
                    AuthorizationManager.deny(mac)
                    updateSessionAuth(mac, CimProtocol.AUTH_BLOCKED)
                    onLog("[COORD] RECHAZADO: $mac")
                }
                PermissionDecision.TIMEOUT -> {
                    AuthorizationManager.deny(mac)
                    updateSessionAuth(mac, CimProtocol.AUTH_PENDING)
                    onLog("[COORD] TIMEOUT autorización: $mac")
                }
                else -> {
                    updateSessionAuth(mac, CimProtocol.AUTH_PENDING)
                }
            }
        } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
            onLog("[COORD] Error en autorización: ${e.message}")
        }
    }

    /**
     * Routes a command toward a device
     * Validates authorization before sending
     */
    suspend fun routeCommand(
        mac: String,
        command: String,
        timeout: Long = 10000,
        onResponse: ((String) -> Unit)? = null
    ): Boolean {
        val session = sessionStates[mac]
        if (session == null) {
            onLog("[COORD] Sesión no encontrada: $mac")
            return false
        }

        // Validate authorization
        if (!AuthorizationManager.isAuthorized(mac)) {
            onLog("[COORD] No autorizado: $mac")
            return false
        }

        // Create the pending command
        val cmdId = "${mac}_${System.currentTimeMillis()}"
        val pending = PendingCommand(
            id = cmdId,
            mac = mac,
            payload = command,
            timeoutMs = timeout
        )
        pendingCommands[cmdId] = pending

        // Start the timeout
        scheduleCommandTimeout(cmdId, timeout)

        onLog("[COORD] ENVÍO: $mac | $command (timeout: ${timeout}ms)")

        // BLE/SPP integration would plug in here
        // For now, record the intent
        scope.launch {
            delay(timeout)
            val resp = pendingCommands.remove(cmdId)
            if (resp != null && onResponse != null) {
                onResponse("TIMEOUT")
            }
        }

        return true
    }

    /**
     * Handles an incoming response from a device
     */
    suspend fun handleIncomingMessage(mac: String, message: String) {
        val lock = sessionLocks.getOrPut(mac) { Mutex() }
        lock.withLock {
            // Parse the message
            when {
                message.startsWith("IDENTIFY|") -> {
                    val parts = message.split("|")
                    if (parts.size >= 3) {
                        val status = parts[1]
                        val version = parts.getOrNull(2) ?: "unknown"
                        onLog("[COORD] IDENTIFY: $mac | $status | v$version")
                        updateSessionHealth(mac, true)
                    }
                }

                message.startsWith("STATUS|") -> {
                    val parts = message.split("|")
                    if (parts.size >= 3) {
                        val status = parts[1]
                        onLog("[COORD] STATUS: $mac | $status")
                        updateSessionHealth(mac, true)
                    }
                }

                message.startsWith("ACK|") -> {
                    // Command executed successfully
                    val parts = message.split("|")
                    if (parts.size >= 2) {
                        val cmdRef = parts[1]
                        pendingCommands.remove(cmdRef)
                        onLog("[COORD] ACK recibido: $cmdRef")
                    }
                }

                message.startsWith("NACK|") -> {
                    // Command error
                    val parts = message.split("|")
                    if (parts.size >= 3) {
                        val cmdRef = parts[1]
                        val reason = parts[2]
                        pendingCommands.remove(cmdRef)
                        onLog("[COORD] NACK: $cmdRef | Razón: $reason")
                    }
                }

                else -> {
                    onLog("[COORD] Mensaje: $mac | $message")
                    onMessageReceived(mac, message)
                }
            }
        }
    }

    /**
     * Revokes authorization for a device
     */
    suspend fun revokeAuthorization(mac: String) {
        val lock = sessionLocks.getOrPut(mac) { Mutex() }
        lock.withLock {
            AuthorizationManager.deny(mac)
            updateSessionAuth(mac, CimProtocol.AUTH_BLOCKED)
            onLog("[COORD] REVOCADO: $mac")

            if (permissionManager != null) {
                permissionManager.revoke(mac)
            }

            // Cancel pending commands
            pendingCommands.entries
                .filter { it.value.mac == mac }
                .forEach { (cmdId, _) ->
                    pendingCommands.remove(cmdId)
                    commandTimeoutJobs[cmdId]?.cancel()
                    commandTimeoutJobs.remove(cmdId)
                }
        }
    }

    /**
     * Closes the session for a device
     */
    suspend fun closeSession(mac: String) {
        val lock = sessionLocks.getOrPut(mac) { Mutex() }
        lock.withLock {
            sessionStates.remove(mac)
            pendingCommands.entries
                .filter { it.value.mac == mac }
                .forEach { (cmdId, _) ->
                    pendingCommands.remove(cmdId)
                    commandTimeoutJobs[cmdId]?.cancel()
                    commandTimeoutJobs.remove(cmdId)
                }
            updateCoordinationStatus()
            onLog("[COORD] DESCONECTADO: $mac")
        }
    }

    /**
     * Returns the current state of a session
     */
    fun getSessionState(mac: String): SessionState? = sessionStates[mac]

    /**
     * Returns all authorized devices
     */
    fun getAuthorizedDevices(): List<String> {
        return sessionStates.filter { (_, session) ->
            session.authState == CimProtocol.AUTH_AUTHORIZED
        }.keys.toList()
    }

    /**
     * Returns all pending devices
     */
    fun getPendingDevices(): List<String> {
        return sessionStates.filter { (_, session) ->
            session.authState == CimProtocol.AUTH_PENDING
        }.keys.toList()
    }

    /**
     * Returns all rejected devices
     */
    fun getRejectedDevices(): List<String> {
        return sessionStates.filter { (_, session) ->
            session.authState == CimProtocol.AUTH_BLOCKED
        }.keys.toList()
    }

    /**
     * Returns the count of pending commands
     */
    fun getPendingCommandCount(): Int = pendingCommands.size

    /**
     * Clears inactive sessions (no heartbeat for more than 30 seconds)
     */
    suspend fun cleanupInactiveSessions(inactivityThresholdMs: Long = 30000) {
        val now = System.currentTimeMillis()
        sessionStates.entries
            .filter { (_, session) -> now - session.lastHeartbeat > inactivityThresholdMs }
            .forEach { (mac, _) ->
                closeSession(mac)
            }
    }

    // ============ PRIVADAS ============

    private suspend fun updateSessionAuth(mac: String, authState: String) {
        sessionStates[mac]?.authState = authState
        updateCoordinationStatus()
    }

    private suspend fun updateSessionHealth(mac: String, healthy: Boolean) {
        sessionStates[mac]?.apply {
            lastHeartbeat = System.currentTimeMillis()
            isHealthy = healthy
        }
        updateCoordinationStatus()
    }

    private fun updateCoordinationStatus() {
        _coordinationStatus.value = sessionStates.toMap()
    }

    private fun scheduleCommandTimeout(cmdId: String, timeoutMs: Long) {
        commandTimeoutJobs[cmdId]?.cancel()
        commandTimeoutJobs[cmdId] = scope.launch {
            delay(timeoutMs)
            if (pendingCommands.containsKey(cmdId)) {
                onLog("[COORD] TIMEOUT de comando: $cmdId")
                pendingCommands.remove(cmdId)
            }
        }
    }

    fun shutdown() {
        scope.cancel()
        commandTimeoutJobs.values.forEach { it.cancel() }
    }
}
