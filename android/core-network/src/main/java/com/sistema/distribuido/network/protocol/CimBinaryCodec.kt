package com.sistema.distribuido.network.protocol

import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Codificador binario compacto estilo Protobuf para estados CIM.
 * Reduce parseo vs strings pipe-delimited en mensajes de alto volumen.
 *
 * Wire format (little-endian):
 * [magic:2=CIM][version:1=1][cmd:1][priority:1][payloadLen:2][payload:N]
 */
object CimBinaryCodec {

    private val MAGIC = byteArrayOf(0x43, 0x49) // "CI"
    private const val VERSION: Byte = 1
    private val UTF8: Charset = Charsets.UTF_8

    data class BinaryFrame(
        val commandType: CommandType,
        val priority: MessagePriority,
        val payload: String
    )

    fun encode(message: CimMessage): ByteArray {
        val payloadBytes = message.payload.toByteArray(UTF8)
        require(payloadBytes.size <= 65535) { "Payload demasiado grande para frame binario" }
        val buffer = ByteBuffer.allocate(7 + payloadBytes.size)
        buffer.put(MAGIC)
        buffer.put(VERSION)
        buffer.put(message.commandType.ordinal.toByte())
        buffer.put(message.priority.ordinal.toByte())
        buffer.putShort(payloadBytes.size.toShort())
        buffer.put(payloadBytes)
        return buffer.array()
    }

    fun decode(bytes: ByteArray): BinaryFrame? {
        if (bytes.size < 7) return null
        val buffer = ByteBuffer.wrap(bytes)
        val m0 = buffer.get()
        val m1 = buffer.get()
        if (m0 != MAGIC[0] || m1 != MAGIC[1]) return null
        val version = buffer.get()
        if (version != VERSION) return null
        val cmdOrd = buffer.get().toInt().and(0xFF)
        val priOrd = buffer.get().toInt().and(0xFF)
        val len = buffer.short.toInt().and(0xFFFF)
        if (buffer.remaining() < len) return null
        val payloadBytes = ByteArray(len)
        buffer.get(payloadBytes)
        val cmd = CommandType.entries.getOrNull(cmdOrd) ?: return null
        val pri = MessagePriority.entries.getOrNull(priOrd) ?: MessagePriority.NORMAL
        return BinaryFrame(cmd, pri, String(payloadBytes, UTF8))
    }

    fun encodeToBase64(message: CimMessage): String {
        return android.util.Base64.encodeToString(encode(message), android.util.Base64.NO_WRAP)
    }

    fun decodeFromBase64(b64: String): BinaryFrame? {
        return try {
            decode(android.util.Base64.decode(b64, android.util.Base64.NO_WRAP))
        } catch (_: Exception) {
            null
        }
    }

    /** Prefijo wire para mezclar binario sobre TCP texto: CIMB|base64 */
    fun wrapBase64Wire(message: CimMessage): String = "CIMB|${encodeToBase64(message)}"

    fun unwrapBase64Wire(wire: String): CimMessage? {
        if (!wire.startsWith("CIMB|")) return null
        val frame = decodeFromBase64(wire.removePrefix("CIMB|")) ?: return null
        return CimMessage(
            commandType = frame.commandType,
            priority = frame.priority,
            payload = frame.payload
        )
    }
}
