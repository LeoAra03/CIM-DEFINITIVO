package com.industria.prefecto

/**
 * Frozen wire constants for the CIM cell.
 *
 * Protocol source of truth: `docs/PROTOCOL.md`. Nothing in this object is
 * allowed to change without a matching protocol document update and a
 * firmware contract gate pass.
 */
object CimProtocol {
    // --- Protocol identity -------------------------------------------------
    const val HANDSHAKE_VERSION: String = "CIM_MASTER_HUB_V1"
    const val PAIRING_SECRET: String = "cim-cell-2026"

    // --- Station identity --------------------------------------------------
    const val UUID_STORAGE: String = "CIM-ST-ALM-X1"
    const val UUID_MANUFACTURING: String = "CIM-ST-MAN-X2"
    const val UUID_QUALITY: String = "CIM-ST-CAL-X3"
    const val UUID_PLC: String = "CIM-ST-PLC-X4"

    /** Station display name to CIM UUID. */
    val STATION_UUIDS: Map<String, String> = mapOf(
        "ALMACEN" to UUID_STORAGE,
        "MANUFACTURA" to UUID_MANUFACTURING,
        "CALIDAD" to UUID_QUALITY,
        "PLC" to UUID_PLC,
    )

    // --- Transport ---------------------------------------------------------
    const val TCP_PORT: Int = 8888
    const val MAX_FRAME_BYTES: Int = 64 * 1024
    const val FRAME_SOH: Int = 0x02
    const val FRAME_EOF: Int = 0x03
    const val BLE_SERVICE_UUID: String = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    const val BLE_TX_UUID: String = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
    const val BLE_RX_UUID: String = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

    // --- Authorization -----------------------------------------------------
    const val RESPONSE_PERMISSION_GRANTED: String = "PERMISSION_GRANTED"
    const val RESPONSE_PERMISSION_DENIED: String = "PERMISSION_DENIED"

    fun buildHandshake(sourceMac: String, sourceApp: String): String {
        return "CIM|1|$HANDSHAKE_VERSION|$sourceMac|$sourceApp"
    }

    fun parseHandshake(raw: String): Pair<String, String>? {
        val parts = raw.split("|")
        if (parts.size < 5 || parts[0] != "CIM" || parts[2] != HANDSHAKE_VERSION) return null
        return parts[3] to parts[4]
    }

    fun buildStatus(stationUuid: String, state: String): String {
        require(state == STATUS_IDLE || state == STATUS_BUSY || state == STATUS_ERROR) {
            "Unsupported station state '$state'"
        }
        return "STATUS;$stationUuid;$state"
    }

    fun parseStatus(raw: String): Pair<String, String>? {
        val parts = raw.split(";")
        if (parts.size != 3 || parts[0] != "STATUS") return null
        if (parts[2] != STATUS_IDLE && parts[2] != STATUS_BUSY && parts[2] != STATUS_ERROR) return null
        return parts[1] to parts[2]
    }

    fun isPairingSecretValid(secret: String): Boolean = secret == PAIRING_SECRET

    fun statusFrame(stationUuid: String, state: String): String = buildStatus(stationUuid, state)

    // --- Station status values ---------------------------------------------
    const val STATUS_IDLE: String = "IDLE"
    const val STATUS_BUSY: String = "BUSY"
    const val STATUS_ERROR: String = "ERROR"

    // --- Session states (compatibility with the station UI) ----------------
    const val AUTH_STATE_DISCONNECTED: String = "DESCONECTADO"
    const val AUTH_STATE_PENDING: String = "ESPERANDO AUTORIZACIÓN"
    const val AUTH_STATE_VALIDATED: String = "VALIDADO"
    const val AUTH_STATE_REJECTED: String = "RECHAZADO"

    // --- Authorization states used by the coordinator UI -------------------
    const val AUTH_PENDING: String = "PENDING"
    const val AUTH_AUTHORIZED: String = "AUTHORIZED"
    const val AUTH_BLOCKED: String = "BLOCKED"

    // --- Legacy permission responses (kept for the station state machine) --
    const val RESPONSE_AUTHORIZED: String = "VALIDADO"
    const val RESPONSE_DENIED: String = "DENIED"
    const val RESPONSE_WAITING: String = "ESPERANDO"
    const val USE_TLS: Boolean = false

    // --- Bluetooth SPP (legacy serial profile) -----------------------------
    val SPP_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    // --- Legacy CRC string framing (still accepted by TcpClient/TcpServer) --
    const val USE_CRC_V2: Boolean = true

    /**
     * Builds a timestamped terminal line: `[HH:mm:ss] [OK|ERR|INFO] [module] message`.
     */
    fun formatLog(module: String, message: String, success: Boolean? = null): String {
        val time = java.time.LocalTime.now().format(TIME_FORMAT)
        val status = when (success) {
            true -> "OK"
            false -> "ERR"
            else -> "INFO"
        }
        return "[$time] [$status] [$module] $message"
    }

    private val TIME_FORMAT: java.time.format.DateTimeFormatter =
        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
}
