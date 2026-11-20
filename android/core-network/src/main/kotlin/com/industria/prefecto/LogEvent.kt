package com.industria.prefecto

import java.time.Clock
import java.time.format.DateTimeFormatter

/**
 * Structured log entry for the station and coordinator terminals.
 *
 * @param severity severity of the event.
 * @param message human readable message.
 * @param stationUuid station that originated the event.
 */
data class LogEvent(
    val severity: Severity,
    val message: String,
    val stationUuid: String = "",
) {
    enum class Severity { INFO, OK, WARN, ERROR }

    fun format(clock: Clock = Clock.systemDefaultZone()): String {
        val time = clock.instant().atZone(clock.zone).format(TIME_FORMAT)
        val station = if (stationUuid.isBlank()) "" else " [$stationUuid]"
        return "[$time] [${severity.name}]$station $message"
    }

    companion object {
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
