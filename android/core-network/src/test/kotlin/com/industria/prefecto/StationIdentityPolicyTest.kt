package com.industria.prefecto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationIdentityPolicyTest {

    private val registry = StationRegistry(capacity = 8)

    private fun policy(): StationIdentityPolicy = StationIdentityPolicy(registry)

    private fun register(uuid: String, mac: String) {
        registry.register(StationRecord(macAddress = mac, stationUuid = uuid, stationType = "TEST"))
    }

    @Test
    fun acceptsConfiguredUuid() {
        register("CIM-ST-ALM-X1", "AA:BB:CC:DD:EE:FF")
        assertTrue(policy().isKnown("CIM-ST-ALM-X1", "AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun acceptsLowercaseMac() {
        register("CIM-ST-ALM-X1", "aa:bb:cc:dd:ee:ff")
        assertTrue(policy().isKnown("CIM-ST-ALM-X1", "aa:bb:cc:dd:ee:ff"))
    }

    @Test
    fun rejectsMalformedMac() {
        assertFalse(policy().isKnown("CIM-ST-ALM-X1", "not-a-mac"))
    }

    @Test
    fun rejectsUnknownStation() {
        assertFalse(policy().isKnown("CIM-ST-UNKNOWN", "AA:BB:CC:DD:EE:FF"))
    }
}
