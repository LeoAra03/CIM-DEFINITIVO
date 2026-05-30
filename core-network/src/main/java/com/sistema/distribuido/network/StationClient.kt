package com.sistema.distribuido.network

import kotlinx.coroutines.*
import android.util.Log
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CimMessageBuilder
import com.sistema.distribuido.network.protocol.CimProtocol
import com.sistema.distribuido.network.protocol.CommandType

/**
 * Cliente Industrial Estandarizado para Estaciones CIM.
 * Encapsula TcpClient y maneja el protocolo de Handshake y Status.
 * 
 * IMPORTANTE: Todos los métodos de envío ahora validan conexión antes de enviar
 * para evitar crashouts en operaciones de red.
 */
class StationClient(
    private val host: String,
    private val port: Int,
    private val stationName: String,
    private val password: String,
    private val stationUuid: String,
    private val macAddress: String = "00:00:00:00:00:00"
) {
    private val tcpClient = TcpClient(host, port)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectDelayBase = 2000L
    private var reconnectDelayMax = 30000L
    
    var onCommandReceived: ((String) -> Unit)? = null
    var onStatusChanged: ((Boolean) -> Unit)? = null
    var onAuthorizationStateChanged: ((String) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null
    private var authorizationState = CimProtocol.AUTH_STATE_DISCONNECTED

    val isAuthorized: Boolean
        get() = authorizationState == CimProtocol.AUTH_STATE_VALIDATED

    fun canSendCommand(): Boolean = isAuthorized

    private fun setAuthorizationState(newState: String) {
        authorizationState = newState
        onAuthorizationStateChanged?.invoke(newState)
    }

    init {
        tcpClient.onConnectionStateChanged = { connected ->
            onStatusChanged?.invoke(connected)
            if (connected) {
                // Cancelar reintentos y arrancar handshake + heartbeat
                reconnectJob?.cancel()
                reconnectJob = null
                setAuthorizationState(CimProtocol.AUTH_STATE_PENDING)
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Conectado al coordinador", true))
                scope.launch { performHandshakeSafe() }
                startHeartbeat()
            } else {
                setAuthorizationState(CimProtocol.AUTH_STATE_DISCONNECTED)
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Desconectado - iniciando reintentos...", false))
                stopHeartbeat()
                scheduleReconnect()
            }
        }

        tcpClient.onMessageReceived = { msg ->
            try {
                val cim = CimMessage.fromTransportString(msg)
                if (cim != null) {
                    when (cim.commandType) {
                        CommandType.PERMISSION_GRANTED -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_VALIDATED)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Autorización exitosa recibida", true))
                        }
                        CommandType.PERMISSION_DENIED -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_REJECTED)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Autorización denegada por coordinador", false))
                        }
                        else -> {
                            if (msg.startsWith("COMMAND;")) {
                                val cmd = msg.removePrefix("COMMAND;")
                                onCommandReceived?.invoke(cmd)
                                onLog?.invoke(CimProtocol.formatLog("StationClient", "Comando recibido: $cmd", true))
                            } else {
                                onCommandReceived?.invoke(cim.payload)
                                onLog?.invoke(CimProtocol.formatLog("StationClient", "Payload recibido: ${cim.payload.take(80)}", true))
                            }
                        }
                    }
                } else {
                    when {
                        msg.startsWith(CimProtocol.RESPONSE_AUTHORIZED) -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_VALIDATED)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Autorización exitosa recibida", true))
                        }
                        msg.startsWith(CimProtocol.RESPONSE_DENIED) -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_REJECTED)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Autorización denegada por coordinador", false))
                        }
                        msg.startsWith(CimProtocol.RESPONSE_WAITING) -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_PENDING)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Esperando autorización del coordinador...", null))
                        }
                        msg.startsWith("COMMAND;") -> {
                            val cmd = msg.removePrefix("COMMAND;")
                            onCommandReceived?.invoke(cmd)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Comando recibido: $cmd", true))
                        }
                        else -> onCommandReceived?.invoke(msg)
                    }
                }
            } catch (e: Exception) {
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Error procesando mensaje: ${e.message}", false))
                Log.e("StationClient", "Error procesando mensaje", e)
            }
        }
    }

    private var lastSentMsg: String = ""
    private var lastSentTime: Long = 0

    fun connect() {
        onLog?.invoke("→ Iniciando conexión a $host:$port...")
        tcpClient.connect()
    }

    /**
     * Envía un mensaje con anti-spam y sanitización (no-bloqueante)
     */
    private fun sendSecure(msg: String) {
        if (!tcpClient.isSocketConnected()) {
            onLog?.invoke("✗ No conectado - No se puede enviar: $msg")
            return
        }
        // Bloquear comandos críticos hasta autorización
        if ((msg.startsWith("COMMAND;") || msg.startsWith("CMD;")) && !isAuthorized) {
            onLog?.invoke("✗ Comando bloqueado: estación no autorizada")
            return
        }
        
        val now = System.currentTimeMillis()
        // Idempotencia y anti-spam (0.3s entre mensajes idénticos)
        if (msg == lastSentMsg && (now - lastSentTime) < 300) {
            onLog?.invoke("⟳ Mensaje duplicado ignorado (anti-spam)")
            return
        }
        
        lastSentMsg = msg
        lastSentTime = now
        
        val cleanMsg = IndustrialErrorManager.sanitizeInput(msg)
        tcpClient.send(cleanMsg)
    }

    /**
     * Envía mensaje de forma SEGURA y SÍNCRONA con manejo de error completo
     * Útil para operaciones críticas
     */
    suspend fun sendSafe(msg: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!tcpClient.isSocketConnected()) {
                onLog?.invoke("✗ sendSafe: Socket NO conectado")
                return@withContext false
            }
            // Bloquear comandos críticos hasta autorización
            if ((msg.startsWith("COMMAND;") || msg.startsWith("CMD;")) && !isAuthorized) {
                onLog?.invoke("✗ sendSafe: comando bloqueado - estación no autorizada")
                return@withContext false
            }
            
            val now = System.currentTimeMillis()
            // Anti-spam
            if (msg == lastSentMsg && (now - lastSentTime) < 300) {
                return@withContext true // Considerar como exitoso si es spam (ignorado)
            }
            
            lastSentMsg = msg
            lastSentTime = now
            
            val cleanMsg = IndustrialErrorManager.sanitizeInput(msg)
            val success = tcpClient.sendSafe(cleanMsg)
            
            if (success) {
                onLog?.invoke("✓ Enviado: $msg")
            } else {
                onLog?.invoke("✗ Fallo al enviar: $msg")
            }
            
            success
        } catch (e: Exception) {
            onLog?.invoke("✗ Excepción en sendSafe: ${e.message}")
            Log.e("StationClient", "Error en sendSafe", e)
            false
        }
    }

    private fun performHandshake() {
        val handshake = CimMessageBuilder.createPermissionHandshake(
            sourceMac = macAddress,
            sourceApp = AppType.values().firstOrNull { it.name.equals(stationName, ignoreCase = true) } ?: AppType.UNKNOWN,
            stationName = stationName,
            password = password,
            stationUuid = stationUuid
        ).toTransportString()
        sendSecure(handshake)
        onLog?.invoke(CimProtocol.formatLog("StationClient", "Handshake CIM enviado", true))
    }

    /**
     * Realiza handshake de forma segura
     */
    private suspend fun performHandshakeSafe() {
        try {
            val handshake = CimMessageBuilder.createPermissionHandshake(
                sourceMac = macAddress,
                sourceApp = AppType.values().firstOrNull { it.name.equals(stationName, ignoreCase = true) } ?: AppType.UNKNOWN,
                stationName = stationName,
                password = password,
                stationUuid = stationUuid
            ).toTransportString()
            val success = sendSafe(handshake)
            if (success) {
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Handshake completado", true))
            } else {
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Fallo en handshake - reintentando...", false))
                delay(2000)
                performHandshakeSafe()
            }
        } catch (e: Exception) {
            onLog?.invoke(CimProtocol.formatLog("StationClient", "Excepción en handshake: ${e.message}", false))
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob != null && reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            var delayMs = reconnectDelayBase
            while (isActive) {
                try {
                    onLog?.invoke(CimProtocol.formatLog("StationClient", "Intentando reconectar en ${delayMs}ms...", false))
                    delay(delayMs)
                    tcpClient.connect()
                    // Esperar un poco para ver si conecta (TcpClient invocará onConnectionStateChanged)
                    delay(3000)
                    // Si conectó, salir del loop
                    if (tcpClient.isSocketConnected()) {
                        break
                    }
                } catch (_: Exception) {}
                delayMs = (delayMs * 2).coerceAtMost(reconnectDelayMax)
            }
        }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive && tcpClient.isSocketConnected()) {
                try {
                    // Enviar heartbeat o estado cada 10s
                    val status = if (isAuthorized) CimProtocol.READY else CimProtocol.IDLE
                    sendStatusSafe(status)
                } catch (e: Exception) {
                    onLog?.invoke(CimProtocol.formatLog("StationClient", "Heartbeat error: ${e.message}", false))
                }
                delay(10000)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun sendStatus(status: String) {
        // Formato: STATUS;UUID;ESTADO
        sendSecure("STATUS;$stationUuid;$status")
    }

    /**
     * Envía status de forma segura
     */
    suspend fun sendStatusSafe(status: String): Boolean {
        return sendSafe("STATUS;$stationUuid;$status")
    }

    fun sendEvent(event: String) {
        // Formato: EVENT;UUID;DATA
        sendSecure("EVENT;$stationUuid;$event")
    }

    /**
     * Envía evento de forma segura
     */
    suspend fun sendEventSafe(event: String): Boolean {
        return sendSafe("EVENT;$stationUuid;$event")
    }

    fun disconnect() {
        onLog?.invoke("→ Desconectando...")
        // Detener reconexión/heartbeat y desconectar el cliente, pero no cancelar scope interno
        reconnectJob?.cancel()
        reconnectJob = null
        stopHeartbeat()
        tcpClient.disconnect()
    }
}
