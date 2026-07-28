package com.sistema.distribuido.network

import com.sistema.distribuido.network.protocol.AppType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationIdentityPolicyTest {
    @Test
    fun acceptsExpectedPlcIdentity() {
        val verdict = StationIdentityPolicy.evaluate(
            StationIdentity(
                stationUuid = "CIM-ST-PLC-X4",
                appType = AppType.PLC,
                stationType = "PLC_CONTROLLER",
                hardwareModel = "Wemos D1 R32",
                capabilities = setOf("BLE_NUS", "RELAY", "PROXIMITY_SENSOR")
            )
        )
        assertTrue(verdict.accepted)
    }

    @Test
    fun rejectsUuidMutation() {
        val verdict = StationIdentityPolicy.evaluate(
            StationIdentity(
                stationUuid = "CIM-ST-CAL-X3",
                appType = AppType.PLC,
                stationType = "PLC_CONTROLLER",
                hardwareModel = "Wemos D1 R32",
                capabilities = setOf("BLE_NUS", "RELAY", "PROXIMITY_SENSOR")
            )
        )
        assertFalse(verdict.accepted)
        assertTrue(verdict.reason.contains("UUID_MISMATCH"))
    }

    @Test
    fun rejectsMissingSafetyRelevantCapability() {
        val verdict = StationIdentityPolicy.evaluate(
            StationIdentity(
                stationUuid = "CIM-ST-PLC-X4",
                appType = AppType.PLC,
                stationType = "PLC_CONTROLLER",
                hardwareModel = "Wemos D1 R32",
                capabilities = setOf("BLE_NUS")
            )
        )
        assertFalse(verdict.accepted)
        assertTrue(verdict.reason.contains("MISSING_CAPABILITIES"))
    }

    @Test
    fun rejectsStationTypeMutation() {
        val verdict = StationIdentityPolicy.evaluate(
            StationIdentity(
                stationUuid = "CIM-ST-MAN-X2",
                appType = AppType.MANUFACTURA,
                stationType = "QUALITY_STATION",
                hardwareModel = "Wemos D1 R32",
                capabilities = setOf("BLE_NUS", "UART_SCORBOT")
            )
        )
        assertFalse(verdict.accepted)
        assertTrue(verdict.reason.contains("STATION_TYPE_MISMATCH"))
    }
}
