package com.industria.prefecto

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bounded registry of CIM station identity facts.
 *
 * @param capacity maximum number of stations; adding beyond the limit drops
 * the oldest entry.
 */
@Singleton
class StationRegistry @Inject constructor(
    private val capacity: Int = 64,
) {
    private val stations = ConcurrentHashMap<String, StationRecord>()

    fun register(record: StationRecord) {
        val mac = record.macAddress.uppercase(java.util.Locale.ROOT)
        stations[mac] = record.copy(macAddress = mac)
        while (stations.size > capacity) {
            stations.remove(stations.keys.first())
        }
    }

    fun resolve(macAddress: String): String? =
        stations[macAddress.uppercase(java.util.Locale.ROOT)]?.stationUuid

    fun find(stationUuid: String): StationRecord? =
        stations.values.firstOrNull { it.stationUuid == stationUuid }

    fun remove(macAddress: String) {
        stations.remove(macAddress.uppercase(java.util.Locale.ROOT))
    }

    fun snapshot(): List<StationRecord> = stations.values.sortedBy { it.stationUuid }
}

/** Facts known about one physical station. */
data class StationRecord(
    val macAddress: String,
    val stationUuid: String,
    val stationType: String,
    val appType: AppType = AppType.UNKNOWN,
    val hardwareModel: String = "",
    val firmwareVersion: String = "",
    val capabilities: String = "",
)
