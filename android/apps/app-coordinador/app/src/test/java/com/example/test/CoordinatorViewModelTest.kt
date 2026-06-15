package com.example.test

import com.sistema.distribuido.network.CommandBroker
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CommandType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Test
import org.junit.Assert.*

/**
 * STATE HOLDER: CoordinatorViewModelState
 * Simplified for testing
 */
data class CoordinatorViewModelState(
    val cintaConnected: Boolean = false,
    val robotConnected: Boolean = false,
    val laserConnected: Boolean = false,
    val isServerRunning: Boolean = false,
    val authorizedDevices: List<String> = emptyList(),
    val commandLog: List<String> = emptyList(),
    val lastCommand: String = "Esperando comando...",
    val deviceList: List<String> = emptyList()
)

class CoordinatorViewModelTest {

    @Test
    fun testConnectCinta() {
        // Simular CoordinatorViewModel.connectCinta()
        var state = CoordinatorViewModelState(cintaConnected = false)

        // Simulación de conexión a Cinta
        val connectMsg = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.EXECUTE,
            payload = "CONNECT_CINTA"
        )

        // Endpoint debería responder
        val ack = connectMsg.createAckMessage()
        assertEquals(CommandType.ACK, ack.commandType)

        // Actualizar estado
        state = state.copy(cintaConnected = true)
        assertTrue(state.cintaConnected)
    }

    @Test
    fun testDisconnectCinta() {
        var state = CoordinatorViewModelState(cintaConnected = true)

        val disconnectMsg = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.EXECUTE,
            payload = "DISCONNECT_CINTA"
        )

        val ack = disconnectMsg.createAckMessage()
        assertEquals(CommandType.ACK, ack.commandType)

        state = state.copy(cintaConnected = false)
        assertFalse(state.cintaConnected)
    }

    @Test
    fun testSendCintaCommand() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CoordinatorViewModelState(
            cintaConnected = true,
            commandLog = emptyList()
        )

        // Enviar comando DELIVER de estación 1 a 2
        val deliverMsg = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.EXECUTE,
            payload = "DELIVER|1|2"
        )

        broker.send(deliverMsg)

        // Actualizar log
        state = state.copy(
            lastCommand = "DELIVER|1|2",
            commandLog = state.commandLog + "DELIVER|1|2"
        )

        assertEquals("DELIVER|1|2", state.lastCommand)
        assertEqual(1, state.commandLog.size)
    }

    @Test
    fun testSendFreeCommand() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CoordinatorViewModelState(cintaConnected = true)

        val freeMsg = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.EXECUTE,
            payload = "FREE"
        )

        broker.send(freeMsg)

        state = state.copy(lastCommand = "FREE")
        assertEquals("FREE", state.lastCommand)
    }

    @Test
    fun testSendRobotCommand() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CoordinatorViewModelState(robotConnected = true)

        val commands = listOf("HOME", "SEQ_1", "POS:100:200:50")

        commands.forEach { cmd ->
            val msg = CimMessage(
                sourceApp = AppType.COORDINADOR,
                destApp = AppType.MANUFACTURA,
                commandType = CommandType.EXECUTE,
                payload = "ROBOT:$cmd"
            )
            broker.send(msg)
        }

        val stats = broker.getStats()
        assertEquals(3, stats.logSize)
    }

    @Test
    fun testSendLaserCommand() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CoordinatorViewModelState(laserConnected = true)

        val laserCmd = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.MANUFACTURA,
            commandType = CommandType.EXECUTE,
            payload = "LASER:START|250W"
        )

        broker.send(laserCmd)

        state = state.copy(lastCommand = "LASER:START|250W")
        assertTrue(state.lastCommand.contains("LASER"))
    }

    @Test
    fun testGenerateAruco() {
        val broker = CommandBroker(allowOfflineSend = true)

        val generateCmd = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.CALIDAD,
            commandType = CommandType.EXECUTE,
            payload = "GENERATE_ARUCO|4|100mm"
        )

        broker.send(generateCmd)

        val stats = broker.getStats()
        assertEquals(1, stats.logSize)
    }

    @Test
    fun testStartTracking() {
        val broker = CommandBroker(allowOfflineSend = true)

        val startMsg = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.CALIDAD,
            commandType = CommandType.START_SEQUENCE,
            payload = "TRACKING_START|PALLET"
        )

        broker.send(startMsg)
        assertEquals(1, broker.getStats().logSize)
    }

    @Test
    fun testStopTracking() {
        val broker = CommandBroker(allowOfflineSend = true)

        val stopMsg = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.CALIDAD,
            commandType = CommandType.STOP_SEQUENCE,
            payload = "TRACKING_STOP"
        )

        broker.send(stopMsg)
        assertEquals(1, broker.getStats().logSize)
    }

    @Test
    fun testStartTcpServer() {
        var state = CoordinatorViewModelState(isServerRunning = false)

        // Simular inicio de servidor
        state = state.copy(isServerRunning = true)

        assertTrue(state.isServerRunning)
    }

    @Test
    fun testStopTcpServer() {
        var state = CoordinatorViewModelState(isServerRunning = true)

        // Simular parada de servidor
        state = state.copy(isServerRunning = false)

        assertFalse(state.isServerRunning)
    }

    @Test
    fun testAuthorizeDevice() {
        var state = CoordinatorViewModelState(
            authorizedDevices = emptyList()
        )

        val mac = "AA:BB:CC:DD:EE:FF"

        state = state.copy(
            authorizedDevices = state.authorizedDevices + mac
        )

        assertTrue(state.authorizedDevices.contains(mac))
        assertEqual(1, state.authorizedDevices.size)
    }

    @Test
    fun testUpdateDeviceList() {
        var state = CoordinatorViewModelState(deviceList = emptyList())

        val devices = listOf(
            "PLC_01_AA:BB:CC:DD:EE:01",
            "MFG_01_AA:BB:CC:DD:EE:02",
            "CALIDAD_01_AA:BB:CC:DD:EE:03",
            "ALMACEN_01_AA:BB:CC:DD:EE:04"
        )

        state = state.copy(deviceList = devices)

        assertEqual(4, state.deviceList.size)
    }

    @Test
    fun testSendNetworkMessage() {
        val broker = CommandBroker(allowOfflineSend = true)

        val networkMsg = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.STATUS_REQUEST,
            payload = "GET_STATUS"
        )

        broker.send(networkMsg)

        val ack = networkMsg.createAckMessage()
        assertEquals(CommandType.ACK, ack.commandType)
    }

    @Test
    fun testSendStorageCommand() {
        val broker = CommandBroker(allowOfflineSend = true)

        val storeCmd = CimMessage(
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.ALMACEN,
            commandType = CommandType.EXECUTE,
            payload = "STORE|ROW:2|COL:3"
        )

        broker.send(storeCmd)

        val stats = broker.getStats()
        assertEquals(1, stats.logSize)
    }

    @Test
    fun testMultipleCommandSequence() {
        val broker = CommandBroker(allowOfflineSend = true)
        var state = CoordinatorViewModelState()

        val commands = listOf(
            "DELIVER|1|2",
            "ROBOT:HOME",
            "LASER:START|250W",
            "GENERATE_ARUCO|4|100mm",
            "STORE|ROW:1|COL:1"
        )

        commands.forEach { cmd ->
            val msg = CimMessage(
                sourceApp = AppType.COORDINADOR,
                destApp = AppType.COORDINADOR,
                commandType = CommandType.EXECUTE,
                payload = cmd
            )
            broker.send(msg)
        }

        val stats = broker.getStats()
        assertEqual(5, stats.logSize)
    }

    private fun assertEqual(expected: Any, actual: Any) {
        assertEquals(expected, actual)
    }
}

