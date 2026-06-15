package com.sistema.distribuido.network.protocol

import org.junit.Assert.*
import org.junit.Test

class CimMessageTest {

    @Test
    fun testTransportSerializationAndDeserialization() {
        val message = CimMessage(
            sourceMac = "AA:BB:CC:DD:EE:FF",
            sourceApp = AppType.MANUFACTURA,
            destMac = "11:22:33:44:55:66",
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = "TEST|PAYLOAD\nWITH_NEWLINE",
            priority = MessagePriority.HIGH,
            sessionId = "SESSION123"
        )

        val transport = message.toTransportString()
        assertTrue("Serialized message should contain escaped payload", transport.contains("TEST\\|PAYLOAD\\nWITH_NEWLINE"))

        val parsed = CimMessage.fromTransportString(transport)
        assertNotNull(parsed)
        assertEquals(message.id, parsed!!.id)
        assertEquals(message.sourceMac, parsed.sourceMac)
        assertEquals(message.sourceApp, parsed.sourceApp)
        assertEquals(message.destMac, parsed.destMac)
        assertEquals(message.destApp, parsed.destApp)
        assertEquals(message.commandType, parsed.commandType)
        assertEquals(message.priority, parsed.priority)
        assertEquals(message.sessionId, parsed.sessionId)
        assertEquals(message.payload, parsed.payload)
    }

    @Test
    fun testBackslashAndNewlineEscaping() {
        val payload = "LINE1\\LINE2\nLINE3|END"
        val message = CimMessage(
            sourceMac = "AA:BB:CC:DD:EE:FF",
            sourceApp = AppType.ALMACEN,
            destMac = "11:22:33:44:55:66",
            destApp = AppType.COORDINADOR,
            commandType = CommandType.EXECUTE,
            payload = payload,
            priority = MessagePriority.NORMAL,
            sessionId = "SESSION456"
        )

        val transport = message.toTransportString()
        val parsed = CimMessage.fromTransportString(transport)

        assertNotNull(parsed)
        assertEquals(payload, parsed!!.payload)
    }

    @Test
    fun testPermissionHandshakePayload() {
        val handshake = CimMessageBuilder.createPermissionHandshake(
            sourceMac = "AA:BB:CC:DD:EE:FF",
            sourceApp = AppType.CALIDAD,
            stationName = "CALIDAD",
            password = "SECRET",
            stationUuid = "CIM-CAL-03"
        )

        assertEquals(CommandType.REQUIRE_PERMISSION, handshake.commandType)
        assertTrue(handshake.payload.contains("CALIDAD|SECRET|AA:BB:CC:DD:EE:FF|CIM-CAL-03"))
        assertEquals(AppType.CALIDAD, handshake.sourceApp)
        assertEquals(AppType.COORDINADOR, handshake.destApp)
    }
}
