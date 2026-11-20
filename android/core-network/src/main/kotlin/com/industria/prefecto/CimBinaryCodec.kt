package com.industria.prefecto

/**
 * Compact CIM frame codec.
 *
 * Layout:
 * ```
 * SOH 1B | version 1B | length u16 | body N bytes | CRC32 u32 | EOF 1B
 * ```
 *
 * [body] is UTF-8; the legacy `;`-delimited format remains accepted by
 * `CimMessage.fromTransportString`.
 *
 * Malformed, oversized, or corrupted frames throw [MalformedFrameException].
 */
object CimBinaryCodec {
    const val VERSION: Int = 1
    const val SOH: Byte = 0x02
    const val EOF: Byte = 0x03
    const val MAX_BODY_BYTES: Int = 65_535

    fun encode(body: ByteArray): ByteArray {
        require(body.size <= MAX_BODY_BYTES) { "Frame body exceeds ${MAX_BODY_BYTES} bytes" }
        val crc = java.util.zip.CRC32()
        crc.update(body)
        val length = body.size
        return ByteArray(HEADER_SIZE + length + TRAILER_SIZE).also { frame ->
            frame[0] = SOH
            frame[1] = VERSION.toByte()
            frame[2] = (length shr 8).toByte()
            frame[3] = length.toByte()
            body.copyInto(frame, destinationOffset = HEADER_SIZE)
            val crcValue = crc.value.toInt()
            val crcOffset = HEADER_SIZE + length
            frame[crcOffset] = (crcValue shr 24).toByte()
            frame[crcOffset + 1] = (crcValue shr 16).toByte()
            frame[crcOffset + 2] = (crcValue shr 8).toByte()
            frame[crcOffset + 3] = crcValue.toByte()
            frame[frame.lastIndex] = EOF
        }
    }

    fun decode(frame: ByteArray): ByteArray {
        require(frame.size >= HEADER_SIZE + TRAILER_SIZE) { "Malformed frame" }
        require(frame[0] == SOH && frame[frame.lastIndex] == EOF) { "Missing frame delimiters" }
        require((frame[1].toInt() and 0xFF) == VERSION) { "Unsupported frame version" }
        val length = ((frame[2].toInt() and 0xFF) shl 8) or (frame[3].toInt() and 0xFF)
        require(frame.size == HEADER_SIZE + length + TRAILER_SIZE) { "Frame length mismatch" }
        val body = frame.copyOfRange(HEADER_SIZE, HEADER_SIZE + length)
        val crc = java.util.zip.CRC32()
        crc.update(body)
        val expected = crc.value.toInt()
        val actual = readInt(frame, HEADER_SIZE + length)
        require(expected == actual) { "CRC mismatch" }
        return body
    }

    private fun readInt(frame: ByteArray, offset: Int): Int {
        return ((frame[offset].toInt() and 0xFF) shl 24) or
            ((frame[offset + 1].toInt() and 0xFF) shl 16) or
            ((frame[offset + 2].toInt() and 0xFF) shl 8) or
            (frame[offset + 3].toInt() and 0xFF)
    }

    private const val HEADER_SIZE = 4
    private const val TRAILER_SIZE = 5
}

/** Thrown when a frame cannot be parsed or fails validation. */
class MalformedFrameException(message: String) : IllegalArgumentException(message)
