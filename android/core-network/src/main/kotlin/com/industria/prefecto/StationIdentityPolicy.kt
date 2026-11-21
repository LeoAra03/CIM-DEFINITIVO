package com.industria.prefecto

/**
 * Central source of truth for CIM station identities.
 *
 * The ESP32 firmware writes the configured `STATION_UUID` into its
 * identification response; the hub accepts the configured UUID without local
 * guessing. A malformed or unknown identity is rejected with a clear reason.
 */
class StationIdentityPolicy(private val stationRegistry: StationRegistry) {

    fun isKnown(stationUuid: String, mac: String): Boolean =
        rejectionReason(stationUuid, mac) == null

    fun rejectionReason(stationUuid: String, mac: String): String? {
        if (stationUuid.isBlank()) return "Station UUID is missing"
        val macAddress = normaliseMac(mac)
        if (!MAC_PATTERN.matches(macAddress)) return "Invalid station MAC"
        val registered = stationRegistry.resolve(macAddress)
        if (registered != null) {
            if (registered != stationUuid) {
                return "Station '$stationUuid' is registered as '$registered'"
            }
            return null
        }
        if (stationUuid !in CONFIGURED_UUIDS) {
            return "Unknown station '$stationUuid' ($macAddress)"
        }
        return null
    }

    companion object {
        val MAC_PATTERN = Regex("([0-9A-F]{2}:){5}[0-9A-F]{2}")

        /** UUIDs written by the shipped firmware configurations. */
        val CONFIGURED_UUIDS: Set<String> = setOf(
            CimProtocol.UUID_STORAGE,
            CimProtocol.UUID_MANUFACTURING,
            CimProtocol.UUID_QUALITY,
            CimProtocol.UUID_PLC,
        )

        fun normaliseMac(mac: String): String = mac.trim().uppercase(java.util.Locale.ROOT)
    }
}

/** Identity payload reported by a station during identification. */
data class StationIdentity(
    val stationUuid: String,
    val appType: AppType,
    val stationType: String,
    val hardwareModel: String,
    val capabilities: Set<String>,
)

/** Result of evaluating a station identity. */
data class IdentityVerdict(val accepted: Boolean, val reason: String = "")
