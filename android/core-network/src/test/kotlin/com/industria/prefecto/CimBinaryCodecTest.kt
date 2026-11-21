package com.industria.prefecto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CimBinaryCodecTest {

    private val body = "R:HOME".toByteArray(Charsets.UTF_8)

    @Test
    fun encodeThenDecodeReturnsOriginalBody() {
        val frame = CimBinaryCodec.encode(body)
        assertEquals(CimBinaryCodec.SOH, frame[0])
        assertEquals(CimBinaryCodec.EOF, frame[frame.lastIndex])
        assertArrayEquals(body, CimBinaryCodec.decode(frame))
    }

    @Test
    fun corruptedPayloadFailsTheCrc() {
        val frame = CimBinaryCodec.encode(body)
        frame[4] = 'X'.code.toByte()
        assertThrows(IllegalArgumentException::class.java) {
            CimBinaryCodec.decode(frame)
        }
    }

    @Test
    fun oversizedBodyFailsFast() {
        val oversized = ByteArray(CimBinaryCodec.MAX_BODY_BYTES + 1)
        assertThrows(IllegalArgumentException::class.java) {
            CimBinaryCodec.encode(oversized)
        }
    }

    @Test
    fun missingTerminatorIsRejected() {
        val frame = byteArrayOf(0x02, 0x00, 0x01, 0x41, 0x00, 0x00)
        assertThrows(IllegalArgumentException::class.java) {
            CimBinaryCodec.decode(frame)
        }
    }
}
