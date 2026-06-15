package com.sistema.distribuido.network.protocol

import java.io.Serializable
import java.util.UUID

/**
 * ESTRUCTURA DE MENSAJE CIM v5.1
 * Protocolo unificado para comunicación entre Coordinador y Estaciones.
 * Serializable para transporte por BLE + TCP.
 */

// ============= ENUMERACIONES =============

enum class AppType {
    COORDINADOR,
    PLC,
    MANUFACTURA,
    CALIDAD,
    ALMACEN,
    UNKNOWN
}

enum class CommandType {
    // Handshake
    IDENTIFY,
    IDENTIFIED,
    REQUIRE_PERMISSION,
    PERMISSION_GRANTED,
    PERMISSION_DENIED,
    
    // Operación
    EXECUTE,
    ACK,
    NACK,
    
    // Secuencias
    START_SEQUENCE,
    STOP_SEQUENCE,
    PAUSE_SEQUENCE,
    
    // Status
    HEARTBEAT,
    STATUS_REQUEST,
    STATUS_RESPONSE,
    
    // Error handling
    TIMEOUT,
    ERROR,
    
    // Network
    DEVICE_DISCOVERED,
    DEVICE_LOST,
    
    // Testing
    TEST_MESSAGE,
    TEST_RESPONSE
}

enum class MessagePriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

// ============= DATA CLASSES =============

data class CimMessage(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val sourceMac: String = "",
    val sourceApp: AppType = AppType.UNKNOWN,
    val destMac: String = "",
    val destApp: AppType = AppType.UNKNOWN,
    val commandType: CommandType = CommandType.EXECUTE,
    val payload: String = "",
    val priority: MessagePriority = MessagePriority.NORMAL,
    val sessionId: String = "",
    val retryCount: Int = 0
) : Serializable {
    
    /**
     * Serializa el mensaje a formato string para transporte.
     * Formato: ID|TIMESTAMP|SOURCE_MAC|SOURCE_APP|DEST_MAC|DEST_APP|CMD|PRIORITY|PAYLOAD
     */
    fun toTransportString(): String {
        return "$id|$timestamp|$sourceMac|$sourceApp|$destMac|$destApp|$commandType|$priority|$sessionId|${payloadEscaped()}"
    }
    
    /**
     * Escapa caracteres especiales en payload.
     */
    private fun payloadEscaped(): String {
        return payload
            .replace("\\", "\\\\")
            .replace("|", "\\|")
            .replace("\n", "\\n")
    }
    
    /**
     * Retorna si el mensaje requiere ACK.
     */
    fun requiresAck(): Boolean {
        return commandType in listOf(
            CommandType.EXECUTE,
            CommandType.START_SEQUENCE,
            CommandType.STOP_SEQUENCE,
            CommandType.STATUS_REQUEST
        )
    }
    
    /**
     * Genera un mensaje de ACK para este mensaje.
     */
    fun createAckMessage(): CimMessage {
        return CimMessage(
            id = this.id + "_ACK",
            timestamp = System.currentTimeMillis(),
            sourceMac = this.destMac,
            sourceApp = this.destApp,
            destMac = this.sourceMac,
            destApp = this.sourceApp,
            commandType = CommandType.ACK,
            sessionId = this.sessionId,
            priority = this.priority
        )
    }
    
    /**
     * Genera un mensaje de error.
     */
    fun createErrorMessage(errorMsg: String): CimMessage {
        return CimMessage(
            id = this.id + "_ERR",
            timestamp = System.currentTimeMillis(),
            sourceMac = this.destMac,
            sourceApp = this.destApp,
            destMac = this.sourceMac,
            destApp = this.sourceApp,
            commandType = CommandType.ERROR,
            payload = errorMsg,
            sessionId = this.sessionId,
            priority = this.priority
        )
    }
    
    companion object {
        /**
         * Deserializa un mensaje desde string de transporte.
         */
        fun fromTransportString(data: String): CimMessage? {
            return try {
                val parts = splitUnescaped(data, '|', limit = 10)
                if (parts.size < 9) return null

                CimMessage(
                    id = parts[0],
                    timestamp = parts[1].toLong(),
                    sourceMac = parts[2],
                    sourceApp = AppType.valueOf(parts[3]),
                    destMac = parts[4],
                    destApp = AppType.valueOf(parts[5]),
                    commandType = CommandType.valueOf(parts[6]),
                    priority = MessagePriority.valueOf(parts[7]),
                    sessionId = parts[8],
                    payload = if (parts.size > 9) unescapePayload(parts[9]) else ""
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun splitUnescaped(input: String, delimiter: Char, limit: Int): List<String> {
            val tokens = mutableListOf<String>()
            val current = StringBuilder()
            var escaped = false
            for (ch in input) {
                if (escaped) {
                    current.append('\\').append(ch)
                    escaped = false
                    continue
                }
                if (ch == '\\') {
                    escaped = true
                    continue
                }
                if (ch == delimiter && tokens.size < limit - 1) {
                    tokens.add(current.toString())
                    current.clear()
                } else {
                    current.append(ch)
                }
            }
            tokens.add(current.toString())
            return tokens
        }

        private fun unescapePayload(raw: String): String {
            return raw
                .replace("\\\\", "\\")
                .replace("\\|", "|")
                .replace("\\n", "\n")
        }
    }
}

// ============= COMMAND BUILDERS =============

object CimMessageBuilder {
    
    fun createIdentifyMessage(mac: String, appType: AppType, version: String = "1.0"): CimMessage {
        return CimMessage(
            sourceMac = mac,
            sourceApp = appType,
            commandType = CommandType.IDENTIFY,
            payload = "$appType|$version",
            priority = MessagePriority.HIGH
        )
    }
    
    fun createPermissionRequest(mac: String, appType: AppType): CimMessage {
        return CimMessage(
            destApp = AppType.COORDINADOR,
            commandType = CommandType.REQUIRE_PERMISSION,
            payload = "$mac|$appType",
            priority = MessagePriority.HIGH
        )
    }

    fun createPermissionHandshake(
        sourceMac: String,
        sourceApp: AppType,
        stationName: String,
        password: String,
        stationUuid: String
    ): CimMessage {
        return CimMessage(
            sourceMac = sourceMac,
            sourceApp = sourceApp,
            destApp = AppType.COORDINADOR,
            commandType = CommandType.REQUIRE_PERMISSION,
            payload = "$stationName|$password|$sourceMac|$stationUuid",
            priority = MessagePriority.HIGH
        )
    }
    
    fun createExecuteCommand(
        sourceMac: String,
        sourceApp: AppType,
        destMac: String,
        destApp: AppType,
        command: String
    ): CimMessage {
        return CimMessage(
            sourceMac = sourceMac,
            sourceApp = sourceApp,
            destMac = destMac,
            destApp = destApp,
            commandType = CommandType.EXECUTE,
            payload = command,
            priority = MessagePriority.NORMAL
        )
    }
    
    fun createHeartbeat(mac: String, appType: AppType): CimMessage {
        return CimMessage(
            sourceMac = mac,
            sourceApp = appType,
            commandType = CommandType.HEARTBEAT,
            priority = MessagePriority.LOW
        )
    }
    
    fun createStatusRequest(mac: String, appType: AppType): CimMessage {
        return CimMessage(
            sourceMac = mac,
            sourceApp = appType,
            commandType = CommandType.STATUS_REQUEST,
            priority = MessagePriority.NORMAL
        )
    }

    fun createIdentifiedResponse(
        sourceMac: String,
        destMac: String,
        destApp: AppType,
        payload: String
    ): CimMessage {
        return CimMessage(
            sourceMac = sourceMac,
            sourceApp = AppType.COORDINADOR,
            destMac = destMac,
            destApp = destApp,
            commandType = CommandType.IDENTIFIED,
            payload = payload,
            priority = MessagePriority.HIGH
        )
    }
}

