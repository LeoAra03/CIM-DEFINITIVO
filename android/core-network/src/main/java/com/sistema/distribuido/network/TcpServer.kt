package com.sistema.distribuido.network

import java.io.*
import java.net.*
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import com.sistema.distribuido.network.AuthorizationManager
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CimProtocol

class TcpServer(private val port: Int, private val collisionPolicy: CollisionPolicy = CollisionPolicy.PREFER_NEW) {
    private var serverSocket: ServerSocket? = null
    // connectionId -> Socket (connectionId = "ip:remotePort")
    private val clients = ConcurrentHashMap<String, Socket>()
    // connectionId -> ClientInfo
    private val clientInfos = ConcurrentHashMap<String, ClientInfo>()
    // MAC -> connectionId para lookup O(1) cuando se necesita enviar por MAC
    private val macToConnId = ConcurrentHashMap<String, String>()
    // Opcional: limitar número de conexiones concurrentes
    private val maxClients = 200
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    var onMessageReceived: ((String, String) -> Unit)? = null
    var onClientConnected: ((String) -> Unit)? = null
    var onClientDisconnected: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    data class ClientInfo(
        val ip: String,
        var mac: String = "",
        var appType: AppType = AppType.UNKNOWN,
        var lastSeen: Long = System.currentTimeMillis()
    )

    enum class CollisionPolicy {
        PREFER_NEW,
        REJECT_NEW
    }

    private fun handleMacMapping(mac: String, connId: String, socket: Socket): Boolean {
        val prev = macToConnId[mac]
        if (prev == null) {
            macToConnId[mac] = connId
            return true
        }
        if (prev == connId) return true

        return when (collisionPolicy) {
            CollisionPolicy.PREFER_NEW -> {
                // Cerrar la conexión previa y reemplazar por la nueva
                clients[prev]?.let { try { it.close() } catch (_: Exception) {} }
                clients.remove(prev)
                clientInfos.remove(prev)
                macToConnId[mac] = connId
                true
            }
            CollisionPolicy.REJECT_NEW -> {
                // Rechazar la nueva conexión
                try { socket.close() } catch (_: Exception) {}
                try { Log.w("TcpServer", "Rechazando nueva conexión $connId para MAC $mac debido a política REJECT_NEW") } catch (_: Exception) {}
                false
            }
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                // Verificar que el puerto se vinculó correctamente
                if (serverSocket == null || !serverSocket!!.isBound) {
                    val errMsg = "❌ ERROR: Puerto $port no se pudo vincular - puede estar en uso"
                    onError?.invoke(errMsg)
                    Log.e("TcpServer", errMsg)
                    isRunning = false
                    return@launch
                }
                Log.d("TcpServer", "✓ TCP Server escuchando en puerto ${serverSocket!!.localPort}")

                // Broadcaster de clientes cada 2s y limpieza de conexiones stale cada 5s
                launch {
                    while (isRunning) {
                        broadcastClientList()
                        delay(2000)
                    }
                }

                launch {
                    while (isRunning) {
                        cleanupStaleConnections()
                        delay(5000)
                    }
                }

                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    val ip = socket.inetAddress.hostAddress ?: "unknown"
                    val connId = "$ip:${socket.port}"

                    // Rechazar si excede límite
                    if (clients.size >= maxClients) {
                        try {
                            Log.w("TcpServer", "Límite de conexiones alcanzado: $maxClients - rechazando $connId")
                            socket.close()
                        } catch (_: Exception) {}
                        continue
                    }

                    clients[connId] = socket
                    clientInfos[connId] = ClientInfo(ip = ip)
                    onClientConnected?.invoke(connId)
                    handleClient(socket, connId, ip)
                }
            } catch (e: Exception) {
                // Log unexpected errors to help debugging network crashouts
                try { Log.e("TcpServer", "Error en TcpServer.start: ${e.message}", e) } catch (_: Exception) {}
                isRunning = false
            }
        }
    }
    private fun handleClient(socket: Socket, connId: String, ip: String) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (isRunning) {
                    val msg = reader.readLine() ?: break
                    // Actualizar lastSeen
                    clientInfos[connId]?.lastSeen = System.currentTimeMillis()

                    // Identificar handshake CIM del cliente TCP y actualizar su MAC/AppType
                    try {
                        val cim = CimMessage.fromTransportString(msg)
                        if (cim != null) {
                            val oldMac = clientInfos[connId]?.mac
                            clientInfos[connId]?.mac = cim.sourceMac
                            clientInfos[connId]?.appType = cim.sourceApp
                            if (cim.sourceMac.isNotBlank()) {
                                // Manejar colisión de MAC según la política
                                val accepted = handleMacMapping(cim.sourceMac, connId, socket)
                                if (!accepted) {
                                    // si no se aceptó la nueva conexión, salir del loop y limpiar
                                    break
                                }
                            }
                        } else if (msg.startsWith(CimProtocol.RED_VALIDA)) {
                            val tokens = msg.split(";")
                            if (tokens.size >= 5) {
                                val stationName = tokens[1]
                                val mac = tokens[3]
                                val appType = AppType.values().firstOrNull { it.name.equals(stationName, ignoreCase = true) } ?: AppType.UNKNOWN
                                clientInfos[connId]?.mac = mac
                                clientInfos[connId]?.appType = appType
                                if (mac.isNotBlank()) {
                                    val accepted = handleMacMapping(mac, connId, socket)
                                    if (!accepted) {
                                        break
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignorar parse errors
                    }

                    onMessageReceived?.invoke(connId, msg)
                }
            } catch (e: Exception) {
                try { Log.e("TcpServer", "Error manejando cliente $connId: ${e.message}", e) } catch (_: Exception) {}
            } finally {
                disconnectClient(connId)
            }
        }
    }

    fun disconnectClient(connId: String) {
        clients[connId]?.let { try { it.close() } catch (_: Exception) {} }
        clients.remove(connId)
        clientInfos[connId]?.mac?.let { AuthorizationManager.revoke(it) }
        macToConnId.entries.removeIf { it.value == connId }
        clientInfos.remove(connId)
        onClientDisconnected?.invoke(connId)
    }

    fun broadcast(msg: String) {
        scope.launch {
            clients.values.forEach { socket ->
                try {
                    val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                    writer.println(msg)
                } catch (e: Exception) {}
            }
        }
    }

    private fun broadcastClientList() {
        val list = clientInfos.entries.map { (connId, info) -> "$connId:${info.appType.name}" }.joinToString(",")
        if (list.isNotEmpty()) broadcast("CLIENTS|$list")
    }

    private fun cleanupStaleConnections() {
        val now = System.currentTimeMillis()
        val inactive = clientInfos.filter { (_, info) -> (now - info.lastSeen) > 15000 }.keys
        if (inactive.isNotEmpty()) {
            inactive.forEach { ip ->
                onError?.invoke(CimProtocol.formatLog("TcpServer", "Conexión stale detectada y eliminada: $ip", false))
                disconnectClient(ip)
            }
        }
        macToConnId.entries.removeIf { (_, connId) -> !clients.containsKey(connId) }
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        clients.values.forEach { it.close() }
        clients.clear()
    }

    /**
     * Envía un mensaje a un cliente identificado por MAC. Busca el cliente con esa MAC y escribe en su socket.
     */
    fun sendToClientByMac(mac: String, msg: String): Boolean {
        // Intentar lookup directo por MAC -> connectionId
        var connId = macToConnId[mac]
        // Fallback: buscar en clientInfos por si el mapeo no existe
        if (connId == null) {
            connId = clientInfos.entries.firstOrNull { (_, info) -> info.mac == mac }?.key
        }

        if (connId != null) {
            clients[connId]?.let { socket ->
                try {
                    val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                    writer.println(msg)
                    return true
                } catch (e: Exception) {
                    try { Log.e("TcpServer", "Error enviando a $connId: ${e.message}", e) } catch (_: Exception) {}
                    return false
                }
            }
        }

        return false
    }
}
