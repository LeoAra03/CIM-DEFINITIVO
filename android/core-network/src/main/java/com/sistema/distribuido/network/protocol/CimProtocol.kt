package com.sistema.distribuido.network.protocol

/**
 * Protocolo Prefecto CIM - Versión Definitiva "Industrial Hub" 2024
 * Optimizado para Seguridad de Red y Complejidad O(1)
 */
object CimProtocol {
    // Configuración de Servidor
    const val WIFI_PORT = 8888
    const val NSD_SERVICE_TYPE = "_cim-hub._tcp."
    const val USE_CRC_V2 = true
    const val USE_TLS = false
    val SPP_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Token de emparejamiento de laboratorio (editable desde el Maestro).
    // No es un secreto de producción: el transporte usa SHA-256 y la credencial
    // real debe aprovisionarse fuera del repositorio para ensayos reales.
    const val DEFAULT_PAIRING_TOKEN = "CIM_LAB_PAIRING_TOKEN_CHANGE_ME"
    private const val HASH_PREFIX = "sha256:"
    private const val LEGACY_PAIRING_TOKEN_SHA256 = "eb960e3a5f90678554d2aa25c81ce1004e68d6f0aba598d6613e1691b92dd7dd"
    @Volatile
    var PASSWORD_ACTUAL = DEFAULT_PAIRING_TOKEN

    fun pairingSecretForTransport(token: String = PASSWORD_ACTUAL): String {
        return if (token.startsWith(HASH_PREFIX)) token else HASH_PREFIX + sha256Hex(token)
    }

    fun isPairingSecretValid(received: String): Boolean {
        val receivedHash = if (received.startsWith(HASH_PREFIX)) {
            received.removePrefix(HASH_PREFIX)
        } else {
            sha256Hex(received)
        }
        val expectedHash = sha256Hex(PASSWORD_ACTUAL)
        return constantTimeEquals(receivedHash, expectedHash) ||
            constantTimeEquals(receivedHash, LEGACY_PAIRING_TOKEN_SHA256)
    }

    private fun sha256Hex(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        return java.security.MessageDigest.isEqual(left.toByteArray(Charsets.UTF_8), right.toByteArray(Charsets.UTF_8))
    }

    // Handshake Token (validación de red CIM)
    const val RED_VALIDA = "CIM_MASTER_HUB_V1"
    const val RESPONSE_AUTHORIZED = "VALIDADO"
    const val RESPONSE_DENIED = "DENIED"
    const val RESPONSE_WAITING = "ESPERANDO"

    // Estados de Autorización legibles para UI
    const val AUTH_STATE_DISCONNECTED = "DESCONECTADO"
    const val AUTH_STATE_PENDING = "ESPERANDO AUTORIZACIÓN"
    const val AUTH_STATE_VALIDATED = "VALIDADO"
    const val AUTH_STATE_REJECTED = "RECHAZADO"

    // Estados de Operación
    const val READY = "READY"
    const val BUSY = "BUSY"
    const val ERROR = "ERROR"
    const val STOP = "STOP"
    const val IDLE = "IDLE" // Mantenido por compatibilidad previa

    // Señalización
    const val REQ_PERM = "REQ_PERM"
    const val GRANTED = "GRANTED"
    const val DENIED = "DENIED"
    const val ABORT = "ABORT"

    // Estados de Autorización por MAC
    const val AUTH_PENDING = "PENDING"
    const val AUTH_AUTHORIZED = "AUTHORIZED"
    const val AUTH_BLOCKED = "BLOCKED"
    const val AUTH_REMOVED = "REMOVED"

    // Comandos de Hardware (Basados en scripts de Python)
    object Hardware {
        const val SCORBOT_HOME = "HOME\r"
        const val SCORBOT_READY = "READY\r"
        const val SCORBOT_ABORT = "ABORT\r"
        const val SCORBOT_OPEN = "OPEN\r"
        const val SCORBOT_CLOSE = "CLOSE\r"
        const val SCORBOT_COFF = "COFF\r"

        const val CONVEYOR_START = "RUN_CINTA\n"
        const val CONVEYOR_STOP = "STOP_CINTA\n"

        const val SENSOR_SCAN = "SCAN_NOW\n"
    }

    // Mapeo de Identificadores de Estación (UUIDs de Software)
    val STATION_UUIDS = mapOf(
        "ALMACEN" to "CIM-ST-ALM-X1",
        "MANUFACTURA" to "CIM-ST-MAN-X2",
        "CALIDAD" to "CIM-ST-CAL-X3",
        "PLC" to "CIM-ST-PLC-X4"
    )

    /**
     * Formato Handshake Esperado:
     * CIM_MASTER_HUB_V1;NOMBRE_ESTACION;PASSWORD;MAC_DISPOSITIVO;UUID_STATION
     */

    fun formatLog(module: String, message: String, success: Boolean? = null): String {
        val time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
        val status = when (success) {
            true -> "✓"
            false -> "✗"
            else -> "•"
        }
        return "[$time] [$status] [$module] $message"
    }
}

// FIX CRÍTICO: Lista blanca de comandos permitidos
val ALLOWED_COMMANDS = setOf(
    "PLC:START", "PLC:STOP", "C:DELIVER", "C:STOP", "C:FREE",
    "R:HOME", "R:RUN", "R:MOVE", "L:START", "L:STOP",
    "ARUCO:DETECT", "VAL:PASS", "VAL:FAIL", "YOLO:DETECT",
    "STO:", "R:RUN STORE", "R:RUN RETRIEVE",
    "STATUS", "HEARTBEAT", "AUTH_REQ", "REGISTER"
)

fun isCommandAllowed(cmd: String): Boolean {
    return ALLOWED_COMMANDS.any { cmd.startsWith(it) }
}
