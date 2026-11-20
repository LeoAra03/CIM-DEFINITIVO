package com.industria.prefecto

import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Singleton

/**
 * Append-only audit chain with SHA-256 linkage.
 *
 * Entries are retained in memory only; persistence is a deployment concern
 * and is intentionally not performed here.
 *
 * @param clock source of entry timestamps.
 * @param capacity maximum retained entries; the oldest entry is dropped.
 */
@Singleton
class CimAuditChain(
    private val clock: Clock = Clock.systemUTC(),
    private val capacity: Int = 10_000,
) {
    private val entries = ConcurrentLinkedDeque<AuditEntry>()
    private var lastHash: String = genesisHash()

    fun append(action: String, actor: String, subject: String = "", result: String = ""): AuditEntry {
        val entry = AuditEntry(
            sequence = entries.size + 1,
            timestamp = clock.instant().toString(),
            action = action,
            actor = actor,
            subject = subject,
            result = result,
            previousHash = lastHash,
            hash = "",
        )
        val hash = digest(entry.copy(hash = ""))
        val stored = entry.copy(hash = hash)
        entries.addLast(stored)
        lastHash = hash
        while (entries.size > capacity) {
            entries.pollFirst()
        }
        return stored
    }

    fun verify(): Boolean {
        var previous = genesisHash()
        for (entry in entries) {
            val expected = digest(entry.copy(hash = ""))
            if (entry.hash != expected || entry.previousHash != previous) return false
            previous = entry.hash
        }
        return true
    }

    fun snapshot(): List<AuditEntry> = entries.toList()

    private fun digest(entry: AuditEntry): String {
        val payload = "${entry.sequence}|${entry.timestamp}|${entry.action}|" +
            "${entry.actor}|${entry.subject}|${entry.result}|${entry.previousHash}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun genesisHash(): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("CIM-AUDIT-GENESIS".toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }
}

/** One immutable audit record. */
data class AuditEntry(
    val sequence: Int,
    val timestamp: String,
    val action: String,
    val actor: String,
    val subject: String,
    val result: String,
    val previousHash: String,
    val hash: String,
)
