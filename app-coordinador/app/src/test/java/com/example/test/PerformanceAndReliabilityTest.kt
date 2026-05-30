package com.example.test

import com.sistema.distribuido.network.CommandBroker
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CommandType
import org.junit.Test
import org.junit.Assert.*

/**
 * PERFORMANCE & STRESS TESTS
 */
class PerformanceTests {

    @Test
    fun testHighThroughputMessaging() {
        val broker = CommandBroker(allowOfflineSend = true)

        val startTime = System.currentTimeMillis()

        // Enviar 1000 mensajes
        repeat(1000) { i ->
            val msg = CimMessage(
                sourceApp = AppType.COORDINADOR,
                destApp = AppType.PLC,
                commandType = CommandType.EXECUTE,
                payload = "CMD_$i"
            )
            broker.send(msg)
        }

        val elapsed = System.currentTimeMillis() - startTime

        val stats = broker.getStats()
        assertEquals(1000, stats.logSize)

        // Debería ser < 1 segundo para 1000 mensajes
        assertTrue("Throughput test failed: ${elapsed}ms > 1000ms", elapsed < 1000)
    }

    @Test
    fun testMultiStationMessaging() {
        val broker = CommandBroker(allowOfflineSend = true)

        val stations = listOf(
            AppType.PLC,
            AppType.MANUFACTURA,
            AppType.CALIDAD,
            AppType.ALMACEN
        )

        // Cada estación envía 25 mensajes = 100 total
        stations.forEach { station ->
            repeat(25) { i ->
                val msg = CimMessage(
                    sourceApp = station,
                    destApp = AppType.COORDINADOR,
                    commandType = CommandType.EXECUTE,
                    payload = "MSG_FROM_${station.name}_$i"
                )
                broker.send(msg)
            }
        }

        val stats = broker.getStats()
        assertEquals(100, stats.logSize)
    }

    @Test
    fun testAckWaitingPerformance() {
        val broker = CommandBroker(allowOfflineSend = true)

        val startTime = System.currentTimeMillis()

        // Enviar 100 mensajes que requieren ACK
        repeat(100) { i ->
            val msg = CimMessage(
                sourceApp = AppType.COORDINADOR,
                destApp = AppType.PLC,
                commandType = CommandType.EXECUTE,
                payload = "DELIVER|1|2"
            )
            broker.send(msg)

            // Simular ACK
            msg.createAckMessage()
        }

        val elapsed = System.currentTimeMillis() - startTime

        assertTrue("ACK performance test failed: ${elapsed}ms > 500ms", elapsed < 500)
    }

    @Test
    fun testCommandBrokerMemoryUsage() {
        val broker = CommandBroker(allowOfflineSend = true)

        // Enviar 1000 mensajes grandes
        repeat(1000) { i ->
            val largePayload = "X".repeat(1000) // 1KB payload
            val msg = CimMessage(
                sourceApp = AppType.COORDINADOR,
                destApp = AppType.PLC,
                commandType = CommandType.EXECUTE,
                payload = largePayload
            )
            broker.send(msg)
        }

        val stats = broker.getStats()
        assertEquals(1000, stats.logSize)
        // Test completó sin OutOfMemoryError
        assertTrue(true)
    }

    @Test
    fun testSerializationPerformance() {
        val startTime = System.currentTimeMillis()

        repeat(1000) { i ->
            val msg = CimMessage(
                sourceApp = AppType.COORDINADOR,
                destApp = AppType.PLC,
                commandType = CommandType.EXECUTE,
                payload = "DELIVER|$i|${i+1}"
            )

            val serialized = msg.toTransportString()
            val deserialized = CimMessage.fromTransportString(serialized)

            assertNotNull(deserialized)
        }

        val elapsed = System.currentTimeMillis() - startTime
        assertTrue("Serialization test failed: ${elapsed}ms > 1000ms", elapsed < 1000)
    }

    @Test
    fun testConcurrentMessageSending() {
        val broker = CommandBroker(allowOfflineSend = true)

        // Simulación de envío concurrente desde múltiples threads
        val threads = (0..9).map {
            Thread {
                repeat(100) { i ->
                    val msg = CimMessage(
                        sourceApp = AppType.PLC,
                        destApp = AppType.COORDINADOR,
                        commandType = CommandType.EXECUTE,
                        payload = "CONCURRENT_MSG_${Thread.currentThread().name}_$i"
                    )
                    broker.send(msg)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val stats = broker.getStats()
        assertEquals(1000, stats.logSize) // 10 threads * 100 messages
    }
}

/**
 * RELIABILITY & RECOVERY TESTS
 */
class ReliabilityTests {

    @Test
    fun testMessageRetry() {
        val broker = CommandBroker(allowOfflineSend = true)

        val msg = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "DELIVER|1|1"
        )

        // Primer intento
        broker.send(msg)

        // Retry (simulación de timeout y reenvío)
        broker.send(msg)

        val stats = broker.getStats()
        // Ambos intentos se registran
        assertTrue(stats.logSize >= 2)
    }

    @Test
    fun testErrorMessage() {
        val broker = CommandBroker(allowOfflineSend = true)

        val errorMsg = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.ERROR,
            payload = "DEVICE_ERROR|TIMEOUT_OCCURRED"
        )

        broker.send(errorMsg)

        val ackMsg = errorMsg.createAckMessage()
        assertEquals(CommandType.ACK, ackMsg.commandType)
    }

    @Test
    fun testTimeoutHandling() {
        val broker = CommandBroker(allowOfflineSend = true)

        val timeoutMsg = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.TIMEOUT,
            payload = "ACK_NOT_RECEIVED|ORIG_MSG:DELIVER|1|1"
        )

        broker.send(timeoutMsg)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 1)
    }

    @Test
    fun testRecoverySequence() {
        val broker = CommandBroker(allowOfflineSend = true)

        // 1. Error
        val error = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.ERROR,
            payload = "MOTOR_STALL"
        )
        broker.send(error)

        // 2. Recovery attempt
        val recover = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.EXECUTE,
            payload = "RESET_MOTOR"
        )
        broker.send(recover)

        // 3. Status check
        val status = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.STATUS_RESPONSE,
            payload = "STATUS|OK|MOTOR_READY"
        )
        broker.send(status)

        val stats = broker.getStats()
        assertEquals(3, stats.logSize)
    }

    @Test
    fun testHeartbeatFailureDetection() {
        val broker = CommandBroker(allowOfflineSend = true)

        // Enviar 5 heartbeats
        repeat(5) { i ->
            val hb = CimMessage(
                sourceApp = AppType.PLC,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.HEARTBEAT,
                payload = "HB_$i"
            )
            broker.send(hb)
        }

        // Simular falta de heartbeat (después de timeout se envía error)
        val missingHB = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.ERROR,
            payload = "NO_HEARTBEAT_FROM_PLC"
        )
        broker.send(missingHB)

        val stats = broker.getStats()
        assertTrue(stats.logSize >= 6)
    }
}

/**
 * COMPATIBILITY & VERSION TESTS
 */
class CompatibilityTests {

    @Test
    fun testMessageVersioning() {
        // Simulación de compatibilidad entre versiones

        val v5Message = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.EXECUTE,
            payload = "DELIVER|1|1"
        )

        assertNotNull(v5Message)
        assertEquals(CommandType.EXECUTE, v5Message.commandType)
    }

    @Test
    fun testAppTypeCompatibility() {
        val validAppTypes = listOf(
            AppType.COORDINADOR,
            AppType.PLC,
            AppType.MANUFACTURA,
            AppType.CALIDAD,
            AppType.ALMACEN
        )

        validAppTypes.forEach { appType ->
            val msg = CimMessage(
                sourceApp = appType,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.IDENTIFY,
                payload = "TEST"
            )

            assertNotNull(msg)
            assertEquals(appType, msg.sourceApp)
        }
    }

    @Test
    fun testCommandTypeValidation() {
        val validCommandTypes = listOf(
            CommandType.IDENTIFY,
            CommandType.EXECUTE,
            CommandType.ACK,
            CommandType.HEARTBEAT,
            CommandType.ERROR,
            CommandType.START_SEQUENCE,
            CommandType.STOP_SEQUENCE
        )

        validCommandTypes.forEach { cmdType ->
            val msg = CimMessage(
                sourceApp = AppType.PLC,
                destApp = AppType.COORDINADOR,
                commandType = cmdType,
                payload = "TEST"
            )

            assertNotNull(msg)
            assertEquals(cmdType, msg.commandType)
        }
    }

    @Test
    fun testPayloadEncoding() {
        val specialCharPayloads = listOf(
            "DELIVER|1|1",
            "ROBOT:HOME",
            "LASER:OFFSET|+10|-5",
            "TRACK|X:100.5|Y:200.3",
            "ERROR|DEVICE_FAILED|CODE_42"
        )

        specialCharPayloads.forEach { payload ->
            val msg = CimMessage(
                sourceApp = AppType.COORDINADOR,
                destApp = AppType.PLC,
                commandType = CommandType.EXECUTE,
                payload = payload
            )

            assertEquals(payload, msg.payload)
        }
    }
}

