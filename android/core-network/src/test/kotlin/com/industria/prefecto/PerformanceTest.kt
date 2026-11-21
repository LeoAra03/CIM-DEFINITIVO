package com.industria.prefecto

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Count-based throughput gate for the per-station frame dispatcher.
 * The production `gradle test` task uses these bounds; the 5000-iteration
 * version is kept for local benchmarking only.
 */
class PerformanceTest {

    @Test
    fun `1000 frame dispatches complete inside the CI budget`() {
        val frames = 1_000
        val budgetMs = 500L
        val executor = Executors.newFixedThreadPool(4)
        val broker = CommandBroker()
        val written = java.util.concurrent.atomic.AtomicInteger()
        try {
            val startedAt = System.nanoTime()
            val jobs = (0 until frames).map { index ->
                executor.submit {
                    val station = "AA:BB:CC:DD:EE:${(index % 4).toString(16).padStart(2, '0').uppercase()}"
                    val frame = byteArrayOf(0x01, index.toByte())
                    broker.dispatch(station, frame, 1_000) {
                        written.incrementAndGet()
                    }
                }
            }
            jobs.forEach { it.get(5, TimeUnit.SECONDS) }
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
            assertEquals(frames, written.get())
            assertTrue("Dispatch took ${elapsedMs}ms", elapsedMs < budgetMs)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `5000 frame dispatches complete inside the local budget`() {
        val frames = 5_000
        val budgetMs = 2_000L
        val executor = Executors.newFixedThreadPool(4)
        val broker = CommandBroker()
        val written = java.util.concurrent.atomic.AtomicInteger()
        val failures = java.util.concurrent.atomic.AtomicInteger()
        val completed = CountDownLatch(frames)
        try {
            val startedAt = System.nanoTime()
            repeat(frames) { index ->
                executor.submit {
                    val station = "AA:BB:CC:DD:EE:${(index % 4).toString(16).padStart(2, '0').uppercase()}"
                    val ok = broker.dispatch(station, byteArrayOf(0x01, index.toByte()), 1_000) {
                        written.incrementAndGet()
                    }
                    if (!ok) failures.incrementAndGet()
                    completed.countDown()
                }
            }
            assertTrue(completed.await(10, TimeUnit.SECONDS))
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
            assertEquals(0, failures.get())
            assertEquals(frames, written.get())
            assertTrue("Dispatch took ${elapsedMs}ms", elapsedMs < budgetMs)
        } finally {
            executor.shutdown()
        }
    }
}
