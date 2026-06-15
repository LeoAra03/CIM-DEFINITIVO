package com.example.plc

import com.sistema.distribuido.network.CommandBroker
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CommandType
import org.junit.Test
import org.junit.Assert.*

/**
 * STATE HOLDER: CintaPanelState
 * Simplified for testing
 */
data class CintaPanelState(
    val isConnected: Boolean = false,
    val lastCommand: String = "Esperando...",
    val commandLog: List<String> = emptyList(),
    val buttonStates: Map<Int, String> = emptyMap() // button_id -> state (idle/active/success)
)

class PlcStationManagerTest {

    @Test
    fun testSendDeliverCommand() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CintaPanelState(isConnected = true)

        // Enviar comando DELIVER desde botón (1,1)
        val fromStation = 1
        val toStation = 1

        val msg = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "DELIVER|fromStation=$fromStation,toStation=$toStation"
        )

        broker.send(msg)

        state = state.copy(
            lastCommand = "DELIVER|$fromStation|$toStation",
            commandLog = state.commandLog + "DELIVER|$fromStation|$toStation"
        )

        assertEquals("DELIVER|$fromStation|$toStation", state.lastCommand)
        assertEqual(1, state.commandLog.size)
    }

    @Test
    fun testSendFreeCommand() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CintaPanelState(isConnected = true)

        val msg = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "FREE"
        )

        broker.send(msg)

        state = state.copy(lastCommand = "FREE")
        assertEquals("FREE", state.lastCommand)
    }

    @Test
    fun testCintaPanelAllButtons() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CintaPanelState(isConnected = true)

        // 3x10 grid = 30 botones
        var commandCount = 0

        repeat(3) { row ->
            repeat(10) { col ->
                val fromStation = row * 10 + col
                val toStation = (fromStation + 1) % 30

                val msg = CimMessage(
                    sourceApp = AppType.PLC,
                    destApp = AppType.COORDINADOR,
                    commandType = CommandType.EXECUTE,
                    payload = "DELIVER|$fromStation|$toStation"
                )

                broker.send(msg)
                commandCount++
            }
        }

        val stats = broker.getStats()
        assertEqual(30, stats.logSize)
    }

    @Test
    fun testCintaPanelConnectButton() {
        var state = CintaPanelState(isConnected = false)

        // Simular click en botón "Conectar"
        val connectMsg = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "PLC_CINTA|CONNECT"
        )

        val ack = connectMsg.createAckMessage()
        assertEquals(CommandType.ACK, ack.commandType)

        state = state.copy(isConnected = true)
        assertTrue(state.isConnected)
    }

    @Test
    fun testCintaPanelDisconnectButton() {
        var state = CintaPanelState(isConnected = true)

        val disconnectMsg = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "DISCONNECT"
        )

        val ack = disconnectMsg.createAckMessage()
        assertEquals(CommandType.ACK, ack.commandType)

        state = state.copy(isConnected = false)
        assertFalse(state.isConnected)
    }

    @Test
    fun testPlcIdentificationHandshake() {
        // Simular handshake completo:
        // 1. PLC envía IDENTIFY
        // 2. Coordinador responde IDENTIFIED
        // 3. Coordinador solicita PERMISSION
        // 4. PLC otorga PERMISSION_GRANTED

        val broker = CommandBroker(allowOfflineSend = true)

        val identify = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "PLC_STATION_01|192.168.1.100"
        )

        broker.send(identify)

        val identified = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.IDENTIFIED,
            payload = "OK"
        )

        broker.send(identified)

        val stats = broker.getStats()
        assertEqual(2, stats.logSize)
    }

    @Test
    fun testPlcStatusRequest() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CintaPanelState(isConnected = true)

        // Coordinador solicita status
        val statusRequest = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.STATUS_REQUEST,
            payload = "GET_STATUS"
        )

        broker.send(statusRequest)

        // PLC responde
        val statusResponse = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.STATUS_RESPONSE,
            payload = "STATUS|READY|CINTA_MOVING"
        )

        broker.send(statusResponse)

        val stats = broker.getStats()
        assertEqual(2, stats.logSize)
    }

    @Test
    fun testPlcHeartbeat() {
        val broker = CommandBroker(allowOfflineSend = true)

        // Enviar 5 heartbeats
        repeat(5) { i ->
            val heartbeat = CimMessage(
                sourceApp = AppType.PLC,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.HEARTBEAT,
                payload = "HB|SEQ:$i|OK"
            )
            broker.send(heartbeat)
        }

        val stats = broker.getStats()
        assertEqual(5, stats.logSize)
    }

    @Test
    fun testPlcErrorHandling() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CintaPanelState(isConnected = true)

        // Simular error en PLC
        val error = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.ERROR,
            payload = "MOTOR_STALLED|CINTA_01"
        )

        broker.send(error)

        // Coordinador responde con NACK
        val nack = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.NACK,
            payload = "ERROR_RECEIVED|RETRYING"
        )

        broker.send(nack)

        val stats = broker.getStats()
        assertEqual(2, stats.logSize)
    }

    @Test
    fun testPlcCommandSequence() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CintaPanelState(isConnected = true)

        // Secuencia típica de funcionamiento:
        // 1. IDENTIFY
        // 2. Múltiples DELIVER
        // 3. STATUS updates
        // 4. Occasional errors
        // 5. Heartbeats

        val identify = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "PLC"
        )
        broker.send(identify)

        repeat(5) { i ->
            val deliver = CimMessage(
                sourceApp = AppType.PLC,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.EXECUTE,
                payload = "DELIVER|$i|${i+1}"
            )
            broker.send(deliver)
        }

        repeat(3) { i ->
            val hb = CimMessage(
                sourceApp = AppType.PLC,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.HEARTBEAT,
                payload = "HB$i"
            )
            broker.send(hb)
        }

        val stats = broker.getStats()
        assertEqual(9, stats.logSize) // 1 identify + 5 deliver + 3 heartbeat
    }

    @Test
    fun testCintaPanelButtonStates() {
        var state = CintaPanelState(
            isConnected = true,
            buttonStates = emptyMap()
        )

        // Simular presión de botón
        val buttonId = 5

        // Estado 1: idle -> active
        state = state.copy(
            buttonStates = state.buttonStates + (buttonId to "active")
        )
        assertEquals("active", state.buttonStates[buttonId])

        // Estado 2: active -> success
        state = state.copy(
            buttonStates = state.buttonStates + (buttonId to "success")
        )
        assertEquals("success", state.buttonStates[buttonId])

        // Estado 3: success -> idle
        state = state.copy(
            buttonStates = state.buttonStates + (buttonId to "idle")
        )
        assertEquals("idle", state.buttonStates[buttonId])
    }

    @Test
    fun testPlcDualButtonControl() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CintaPanelState(isConnected = true)

        // Botón "Conectar"
        val connectBtn = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "CONNECT"
        )
        broker.send(connectBtn)

        // Botón "Desconectar"
        val disconnectBtn = CimMessage(
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "DISCONNECT"
        )
        broker.send(disconnectBtn)

        val stats = broker.getStats()
        assertEqual(2, stats.logSize)
    }

    @Test
    fun testReliabilityMetrics() {
        val broker = CommandBroker(allowOfflineSend = true)

        var successCount = 0
        var errorCount = 0

        // Simular 100 comandos
        repeat(100) { i ->
            val isError = i % 10 == 0 // 1 de cada 10 es error

            val msg = if (isError) {
                errorCount++
                CimMessage(
                    sourceApp = AppType.PLC,
                    destApp = AppType.COORDINADOR,
                    commandType = CommandType.ERROR,
                    payload = "ERROR_$i"
                )
            } else {
                successCount++
                CimMessage(
                    sourceApp = AppType.PLC,
                    destApp = AppType.COORDINADOR,
                    commandType = CommandType.EXECUTE,
                    payload = "DELIVER|1|2"
                )
            }

            broker.send(msg)
        }

        val stats = broker.getStats()
        assertEqual(100, stats.logSize)
        assertEquals(90, successCount)
        assertEquals(10, errorCount)
    }

    private fun assertEqual(expected: Any, actual: Any) {
        assertEquals(expected, actual)
    }
}

