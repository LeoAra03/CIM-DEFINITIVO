package com.sistema.distribuido.network

import java.security.MessageDigest

/**
 * Cadena de auditoría inmutable (blockchain-lite).
 * Cada evento incluye SHA-256 del evento anterior.
 */
object CimAuditChain {

    data class AuditEvent(
        val timestamp: Long,
        val operator: String,
        val command: String,
        val hash: String,
        val previousHash: String
    )

    private val chain = mutableListOf<AuditEvent>()
    private var lastHash: String = genesisHash()

    @Synchronized
    fun record(operator: String, command: String): AuditEvent {
        val timestamp = System.currentTimeMillis()
        val payload = "$timestamp|$operator|$command|$lastHash"
        val hash = sha256(payload)
        val event = AuditEvent(timestamp, operator, command, hash, lastHash)
        chain.add(event)
        lastHash = hash
        android.util.Log.d("CimAudit", "[$operator] $command → $hash")
        return event
    }

    @Synchronized
    fun verify(): Boolean {
        var prev = genesisHash()
        for (event in chain) {
            if (event.previousHash != prev) return false
            val expected = sha256("${event.timestamp}|${event.operator}|${event.command}|${prev}")
            if (expected != event.hash) return false
            prev = event.hash
        }
        return true
    }

    @Synchronized
    fun getChain(): List<AuditEvent> = chain.toList()

    private fun genesisHash(): String = sha256("CIM_GENESIS_BLOCK_v1")

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
