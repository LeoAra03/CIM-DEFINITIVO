package com.sistema.distribuido.network

import com.sistema.distribuido.network.protocol.CimProtocol
import com.sistema.distribuido.network.protocol.CimTransportCodec
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class TcpClient(private val host: String, private val port: Int, private val maxRetries: Int = 3) {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var heartbeatJob: Job? = null

    var onMessageReceived: ((String) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null

    fun connect() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            var attempts = 0

            while (isRunning && attempts < maxRetries) {
                var disconnectedAfterSuccessfulConnection = false

                try {
                    PerformanceProfiler.trace("TCP_CONNECT") {
                        val activeSocket = TlsSocketHelper.createClientSocket(host, port, 2000)
                        activeSocket.soTimeout = 2000
                        socket = activeSocket
                        writer = PrintWriter(activeSocket.getOutputStream(), true)
                    }

                    onConnectionStateChanged?.invoke(true)
                    attempts = 0
                    startHeartbeat()

                    val inputStream = socket?.getInputStream()
                        ?: throw IllegalStateException("Socket sin InputStream después de conectar")
                    val reader = BufferedReader(InputStreamReader(inputStream))

                    while (isRunning) {
                        try {
                            val inputLine = reader.readLine() ?: break
                            onMessageReceived?.invoke(CimTransportCodec.tryUnwrap(inputLine))
                        } catch (_: java.net.SocketTimeoutException) {
                            // Keep looping so heartbeat and explicit disconnect can be processed.
                            continue
                        }
                    }
                    disconnectedAfterSuccessfulConnection = true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    if (isRunning) {
                        attempts++
                        onConnectionStateChanged?.invoke(false)
                        delay(2000)
                    }
                } finally {
                    stopHeartbeat()
                    closeSocketQuietly()
                }

                if (isRunning && disconnectedAfterSuccessfulConnection) {
                    attempts++
                    onConnectionStateChanged?.invoke(false)
                    delay(2000)
                }
            }

            if (isRunning) {
                onConnectionStateChanged?.invoke(false)
            }
            isRunning = false
        }
    }

    private fun wrapOutgoing(message: String): String {
        return if (CimProtocol.USE_CRC_V2 && !message.startsWith(CimTransportCodec.PREFIX)) {
            CimTransportCodec.wrap(message)
        } else message
    }

    fun send(message: String) {
        scope.launch {
            try {
                writer?.println(wrapOutgoing(message))
                writer?.flush()
            } catch (_: Exception) {
                // Keep network failures non-fatal for UI callers.
            }
        }
    }

    fun isSocketConnected(): Boolean {
        val s = socket
        return s != null && s.isConnected && !s.isClosed
    }

    suspend fun sendSafe(message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val s = socket
            val w = writer
            if (s != null && s.isConnected && !s.isClosed && w != null) {
                w.println(wrapOutgoing(message))
                w.flush()
                return@withContext !w.checkError()
            }
        } catch (_: Exception) {
            // Keep network failures non-fatal for UI callers.
        }
        return@withContext false
    }

    fun disconnect() {
        isRunning = false
        stopHeartbeat()
        scope.launch {
            closeSocketQuietly()
            onConnectionStateChanged?.invoke(false)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    val heartbeat = "HEARTBEAT|${System.currentTimeMillis()}"
                    writer?.println(wrapOutgoing(heartbeat))
                    writer?.flush()
                } catch (_: Exception) {
                    // Heartbeat failures are handled by read/send loops.
                }
                delay(5000)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun closeSocketQuietly() {
        try {
            socket?.close()
        } catch (_: Exception) {
            // Ignore close failures.
        } finally {
            socket = null
            writer = null
        }
    }
}
