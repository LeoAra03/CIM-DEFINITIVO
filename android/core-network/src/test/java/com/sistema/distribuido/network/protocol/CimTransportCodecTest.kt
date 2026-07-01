package com.sistema.distribuido.network.protocol

import org.junit.Assert.*
import org.junit.Test

class CimTransportCodecTest {

    @Test
    fun wrapAndUnwrapPreservesPayload() {
        val payload = "uuid|123|AA:BB|MANUFACTURA|COORD|EXECUTE|NORMAL|sess|test\\|pipe"
        val wire = CimTransportCodec.wrap(payload)
        assertTrue(wire.startsWith(CimTransportCodec.PREFIX))
        assertEquals(payload, CimTransportCodec.unwrap(wire))
    }

    @Test
    fun tryUnwrapLegacyMessage() {
        val legacy = "STATUS;UUID;READY"
        assertEquals(legacy, CimTransportCodec.tryUnwrap(legacy))
    }

    @Test
    fun secureCimMessageRoundTrip() {
        val msg = CimMessage(
            sourceMac = "AA:BB:CC:DD:EE:FF",
            sourceApp = AppType.PLC,
            commandType = CommandType.HEARTBEAT,
            payload = "ping"
        )
        val wire = msg.toSecureTransportString()
        val parsed = CimMessage.fromTransportString(wire)
        assertNotNull(parsed)
        assertEquals(msg.commandType, parsed!!.commandType)
        assertEquals(msg.payload, parsed.payload)
    }

    @Test
    fun binaryCodecRoundTrip() {
        val msg = CimMessage(
            commandType = CommandType.STATUS_RESPONSE,
            priority = MessagePriority.HIGH,
            payload = "SENSOR_OK"
        )
        val bytes = CimBinaryCodec.encode(msg)
        val frame = CimBinaryCodec.decode(bytes)
        assertNotNull(frame)
        assertEquals(CommandType.STATUS_RESPONSE, frame!!.commandType)
        assertEquals("SENSOR_OK", frame.payload)
    }
}
