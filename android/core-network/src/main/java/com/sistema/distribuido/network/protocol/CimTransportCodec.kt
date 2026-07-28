package com.sistema.distribuido.network.protocol

import java.util.zip.CRC32

/**
 * CIM Transport v2.0 — envoltorio de integridad CRC32.
 * Formato wire: CIM2|PAYLOAD|CRC32_HEX
 * Compatible hacia atrás: mensajes sin prefijo CIM2| se pasan tal cual.
 */
object CimTransportCodec {

    const val PREFIX = "CIM2|"

    fun wrap(payload: String): String {
        val crc = computeCrc32Hex(payload)
        return "$PREFIX$payload|$crc"
    }

    fun unwrap(wire: String): String {
        if (!wire.startsWith(PREFIX)) return wire
        val body = wire.removePrefix(PREFIX)
        val crcSep = body.lastIndexOf('|')
        if (crcSep <= 0) return wire
        val payload = body.substring(0, crcSep)
        val crc = body.substring(crcSep + 1)
        val expected = computeCrc32Hex(payload)
        if (!crc.equals(expected, ignoreCase = true)) {
            throw CimTransportException("CRC32 inválido: esperado=$expected recibido=$crc")
        }
        return payload
    }

    fun tryUnwrap(wire: String): String {
        return try {
            if (wire.startsWith(PREFIX)) unwrap(wire) else wire
        } catch (_: CimTransportException) {
            wire
        }
    }

    fun computeCrc32Hex(data: String): String {
        val crc = CRC32()
        crc.update(data.toByteArray(Charsets.UTF_8))
        return "%08X".format(crc.value)
    }
}

class CimTransportException(message: String) : Exception(message)
