package com.sistema.distribuido.network

import org.junit.Assert.assertEquals
import org.junit.Test

class BluetoothMessageParserTest {

    @Test
    fun splitIncomingPayload_emitsCompletedLines() {
        val payload = "OK\nNEXT\r\nFINAL"

        val result = BluetoothMessageParser.splitIncomingPayload(payload)

        assertEquals(listOf("OK", "NEXT", "FINAL"), result)
    }

    @Test
    fun splitIncomingPayload_keepsPartialLineForNextChunk() {
        val payload = "INCOMPLETE"

        val result = BluetoothMessageParser.splitIncomingPayload(payload)

        assertEquals(emptyList<String>(), result)
    }
}
