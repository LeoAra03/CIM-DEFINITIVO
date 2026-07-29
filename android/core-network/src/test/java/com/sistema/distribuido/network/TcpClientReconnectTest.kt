package com.sistema.distribuido.network

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class TcpClientReconnectTest {
    @Test
    fun reconnectsAfterServerClosesAcceptedSocket() {
        val server = ServerSocket(0)
        val acceptedConnections = CountDownLatch(2)
        val observedStates = CopyOnWriteArrayList<Boolean>()

        val serverThread = thread(start = true, isDaemon = true) {
            repeat(2) { index ->
                val socket = server.accept()
                acceptedConnections.countDown()
                Thread.sleep(if (index == 0) 200 else 300)
                socket.close()
            }
        }

        val client = TcpClient("127.0.0.1", server.localPort, maxRetries = 2).apply {
            onConnectionStateChanged = { observedStates += it }
        }

        try {
            client.connect()
            assertTrue(
                "TcpClient debe reconectar después de un cierre remoto",
                acceptedConnections.await(7, TimeUnit.SECONDS)
            )
            assertTrue("Debe registrar al menos un estado conectado", observedStates.contains(true))
            assertTrue("Debe registrar al menos un estado desconectado", observedStates.contains(false))
        } finally {
            client.disconnect()
            server.close()
            serverThread.join(1000)
        }
    }
}
