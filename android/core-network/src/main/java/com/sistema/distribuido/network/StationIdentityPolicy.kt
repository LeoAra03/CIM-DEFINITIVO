package com.sistema.distribuido.network

import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimProtocol

data class StationIdentity(
    val stationUuid: String,
    val appType: AppType,
    val stationType: String,
    val hardwareModel: String,
    val capabilities: Set<String>
)

data class IdentityVerdict(val accepted: Boolean, val reason: String)

/**
 * Allow-list policy for physical CIM stations. Unknown or contradictory
 * identities are rejected before entering the registry.
 */
object StationIdentityPolicy {
    private val expectedStationType = mapOf(
        AppType.PLC to "PLC_CONTROLLER",
        AppType.MANUFACTURA to "ROBOT_ARM",
        AppType.CALIDAD to "QUALITY_STATION",
        AppType.ALMACEN to "STORAGE_STATION"
    )

    private val requiredCapabilities = mapOf(
        AppType.PLC to setOf("BLE_NUS", "RELAY", "PROXIMITY_SENSOR"),
        AppType.MANUFACTURA to setOf("BLE_NUS", "UART_SCORBOT"),
        AppType.CALIDAD to setOf("BLE_NUS"),
        AppType.ALMACEN to setOf("BLE_NUS")
    )

    fun evaluate(identity: StationIdentity): IdentityVerdict {
        val expectedUuid = CimProtocol.STATION_UUIDS[identity.appType.name]
            ?: return IdentityVerdict(false, "APP_TYPE_NOT_ALLOWED")
        if (identity.stationUuid != expectedUuid) return IdentityVerdict(false, "UUID_MISMATCH")
        if (identity.stationType != expectedStationType[identity.appType]) return IdentityVerdict(false, "STATION_TYPE_MISMATCH")
        if (identity.hardwareModel != "Wemos D1 R32") return IdentityVerdict(false, "HARDWARE_MODEL_NOT_ALLOWED")
        val missing = requiredCapabilities[identity.appType].orEmpty() - identity.capabilities
        if (missing.isNotEmpty()) return IdentityVerdict(false, "MISSING_CAPABILITIES:${missing.sorted().joinToString(",")}")
        return IdentityVerdict(true, "IDENTITY_ACCEPTED")
    }
}
