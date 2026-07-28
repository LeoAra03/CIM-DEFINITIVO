package com.sistema.distribuido.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PalletWorkflowEngineTest {
    @Test
    fun validLifecycleReachesStored() {
        val engine = PalletWorkflowEngine()
        assertTrue(engine.register("PAL-001", arucoId = 42).accepted)
        listOf(
            PalletEvent.RELEASED_FROM_STORAGE,
            PalletEvent.ARRIVED_MANUFACTURING,
            PalletEvent.MANUFACTURING_COMPLETED,
            PalletEvent.ARRIVED_QUALITY,
            PalletEvent.QUALITY_PASS,
            PalletEvent.STORED
        ).forEach { assertTrue(engine.apply("PAL-001", it).accepted) }
        assertEquals(PalletStage.STORED, engine.get("PAL-001")?.stage)
    }

    @Test
    fun impossibleJumpBlocksPallet() {
        val engine = PalletWorkflowEngine()
        engine.register("PAL-002")
        val result = engine.apply("PAL-002", PalletEvent.QUALITY_PASS)
        assertFalse(result.accepted)
        assertEquals(PalletStage.BLOCKED, result.snapshot.stage)
    }

    @Test
    fun faultAlwaysBlocksAndRequiresExplicitReset() {
        val engine = PalletWorkflowEngine()
        engine.register("PAL-003")
        assertEquals(PalletStage.BLOCKED, engine.apply("PAL-003", PalletEvent.FAULT, "SENSOR_TIMEOUT").snapshot.stage)
        assertEquals(PalletStage.REGISTERED, engine.apply("PAL-003", PalletEvent.RESET_AFTER_REVIEW).snapshot.stage)
    }
}
