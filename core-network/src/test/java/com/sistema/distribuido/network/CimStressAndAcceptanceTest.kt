package com.sistema.distribuido.network

import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CommandType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Escenarios destructivos, aceptación PO y routing de comandos.
 */
class CimStressAndAcceptanceTest {

    private val mac = "AA:BB:CC:DD:EE:FF"

    @Before
    fun setup() {
        AuthorizationManager.revoke(mac)
    }

    @Test
    fun unauthorizedCommand_blockedByAuthManager() {
        assertFalse(AuthorizationManager.canSendCommand(mac))
        AuthorizationManager.deny(mac)
        assertFalse(AuthorizationManager.canSendCommand(mac))
    }

    @Test
    fun invalidTransportString_returnsNull() {
        assertNull(CimMessage.fromTransportString("GARBAGE|NOT|CIM"))
    }

    @Test
    fun disconnectStorm_brokerSurvives100Messages() {
        val broker = CommandBroker(allowOfflineSend = true)
        repeat(100) {
            broker.send(
                CimMessage(
                    sourceApp = AppType.PLC,
                    destApp = AppType.COORDINADOR,
                    commandType = CommandType.ERROR,
                    payload = "BT_DISCONNECT|$it"
                )
            )
        }
        assertEquals(100, broker.getStats().logSize)
    }

    @Test
    fun happyPath_identifyAuthorizeExecute() {
        AuthorizationManager.authorize(mac)
        assertTrue(AuthorizationManager.isAuthorized(mac))

        val broker = CommandBroker(allowOfflineSend = true)
        val identify = CimMessage(
            sourceMac = mac,
            sourceApp = AppType.PLC,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.IDENTIFY,
            payload = "CIM-ST-PLC-X4"
        )
        broker.send(identify)
        val execute = CimMessage(
            sourceMac = mac,
            sourceApp = AppType.COORDINADOR,
            destApp = AppType.PLC,
            commandType = CommandType.EXECUTE,
            payload = "DELIVER|1|2"
        )
        broker.send(execute)
        assertEquals(2, broker.getStats().logSize)
    }

    @Test
    fun permissionDenial_revokeResetsToPending() {
        AuthorizationManager.authorize(mac)
        AuthorizationManager.revoke(mac)
        assertEquals(
            com.sistema.distribuido.network.protocol.CimProtocol.AUTH_PENDING,
            AuthorizationManager.getAuthorizationState(mac)
        )
    }

    @Test
    fun commandBrokerRouting_offlineSendAllowed() {
        val broker = CommandBroker(allowOfflineSend = true)
        val msg = CimMessage(
            sourceApp = AppType.ALMACEN,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "STORE|ROW:0|COL:0"
        )
        broker.send(msg)
        assertTrue(broker.getStats().logSize >= 1)
    }
}
