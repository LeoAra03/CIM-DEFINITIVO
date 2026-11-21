package com.industria.prefecto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PalletWorkflowEngineTest {

    @Test
    fun happyPathProducesLegalHistory() {
        val transitions = mutableListOf<PalletTransition>()
        val engine = PalletWorkflowEngine(onTransition = { transitions += it })
        engine.register("PAL-001")

        val path = listOf(
            PalletWorkflowEngine.Stage.STORAGE_RELEASED,
            PalletWorkflowEngine.Stage.CONVEYOR_TO_MANUFACTURING,
            PalletWorkflowEngine.Stage.MANUFACTURING,
            PalletWorkflowEngine.Stage.CONVEYOR_TO_QUALITY,
            PalletWorkflowEngine.Stage.QUALITY_INSPECTION,
            PalletWorkflowEngine.Stage.APPROVED,
            PalletWorkflowEngine.Stage.STORED,
        )
        for (stage in path) {
            val transition = engine.apply("PAL-001", stage, "path")
            assertTrue("Expected $stage to be accepted", transition.accepted)
        }
        assertEquals(PalletWorkflowEngine.Stage.STORED, engine.stageOf("PAL-001"))
        assertTrue(engine.isTerminal("PAL-001"))
        assertEquals(path.size, transitions.size)
        assertEquals(path, engine.history().map { it.to })
    }

    @Test
    fun invalidTransitionKeepsStage() {
        val engine = PalletWorkflowEngine()
        engine.register("PAL-002")
        engine.apply("PAL-002", PalletWorkflowEngine.Stage.STORAGE_RELEASED, "release")
        val transition = engine.apply(
            "PAL-002",
            PalletWorkflowEngine.Stage.STORAGE_RELEASED,
            "skip",
        )
        val other = engine.apply(
            "PAL-002",
            PalletWorkflowEngine.Stage.STORAGE_RELEASED,
            "back",
        )
        assertFalse(transition.accepted)
        assertFalse(other.accepted)
        assertEquals(PalletWorkflowEngine.Stage.STORAGE_RELEASED, engine.stageOf("PAL-002"))
    }

    @Test
    fun blockedPalletOnlyAcceptsReset() {
        val engine = PalletWorkflowEngine()
        engine.register("PAL-003")
        val blocked = engine.apply(
            "PAL-003",
            PalletWorkflowEngine.Stage.BLOCKED,
            "fault",
        )
        assertTrue(blocked.accepted)
        val illegal = engine.apply(
            "PAL-003",
            PalletWorkflowEngine.Stage.STORAGE_RELEASED,
            "release",
        )
        assertFalse(illegal.accepted)
        val reset = engine.apply(
            "PAL-003",
            PalletWorkflowEngine.Stage.REGISTERED,
            "review",
        )
        assertTrue(reset.accepted)
        assertEquals(PalletWorkflowEngine.Stage.REGISTERED, engine.stageOf("PAL-003"))
    }
}
