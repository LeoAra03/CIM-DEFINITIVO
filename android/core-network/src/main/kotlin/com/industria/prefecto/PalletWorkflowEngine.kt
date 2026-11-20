package com.industria.prefecto

/**
 * Deterministic PALLET state machine.
 *
 * Legal transitions mirror the physical cell:
 * `REGISTERED -> STORAGE_RELEASED -> CONVEYOR_TO_MANUFACTURING ->
 * MANUFACTURING -> CONVEYOR_TO_QUALITY -> QUALITY_INSPECTION ->
 * (APPROVED | REJECTED) -> STORED`. `BLOCKED` accepts only
 * `RESET_AFTER_REVIEW`; any invalid transition leaves the current stage
 * untouched.
 *
 * @param onTransition invoked for every valid transition.
 */
class PalletWorkflowEngine(
    private val onTransition: ((PalletTransition) -> Unit)? = null,
) {
    enum class Stage {
        REGISTERED,
        STORAGE_RELEASED,
        CONVEYOR_TO_MANUFACTURING,
        MANUFACTURING,
        CONVEYOR_TO_QUALITY,
        QUALITY_INSPECTION,
        APPROVED,
        REJECTED,
        STORED,
        BLOCKED,
    }

    private val stages = linkedMapOf<String, Stage>()
    private val reasons = linkedMapOf<String, String>()
    private val history = mutableListOf<PalletTransition>()

    fun register(palletId: String) {
        stages[palletId] = Stage.REGISTERED
        reasons[palletId] = "Registered"
    }

    fun apply(palletId: String, to: Stage, reason: String): PalletTransition {
        val current = stages[palletId]
            ?: return PalletTransition(palletId, null, to, reason, accepted = false, errorCode = "UNKNOWN_PALLET")
        if (current == to) {
            return PalletTransition(palletId, current, to, reason, accepted = false, errorCode = "ALREADY_IN_STAGE")
        }
        val allowed = current in terminalStages && to == Stage.REGISTERED ||
            current == Stage.REGISTERED && to == Stage.STORAGE_RELEASED ||
            current == Stage.STORAGE_RELEASED && to == Stage.CONVEYOR_TO_MANUFACTURING ||
            current == Stage.CONVEYOR_TO_MANUFACTURING && to == Stage.MANUFACTURING ||
            current == Stage.MANUFACTURING && to == Stage.CONVEYOR_TO_QUALITY ||
            current == Stage.CONVEYOR_TO_QUALITY && to == Stage.QUALITY_INSPECTION ||
            current == Stage.QUALITY_INSPECTION && to == Stage.APPROVED ||
            current == Stage.QUALITY_INSPECTION && to == Stage.REJECTED ||
            (current == Stage.APPROVED || current == Stage.REJECTED) && to == Stage.STORED ||
            current == Stage.BLOCKED && to == Stage.REGISTERED ||
            to == Stage.BLOCKED
        if (!allowed) {
            return PalletTransition(palletId, current, to, reason, accepted = false, errorCode = "ILLEGAL_TRANSITION")
        }
        if (to == Stage.BLOCKED && reason.isBlank()) {
            return PalletTransition(palletId, current, to, reason, accepted = false, errorCode = "MISSING_BLOCK_REASON")
        }
        stages[palletId] = to
        reasons[palletId] = reason
        val transition = PalletTransition(palletId, current, to, reason, accepted = true)
        history += transition
        onTransition?.invoke(transition)
        return transition
    }

    fun stageOf(palletId: String): Stage? = stages[palletId]

    fun reasonOf(palletId: String): String? = reasons[palletId]

    fun palletIds(): List<String> = stages.keys.toList()

    fun isTerminal(palletId: String): Boolean {
        val stage = stages[palletId] ?: return false
        return stage in terminalStages
    }

    fun clear() {
        stages.clear()
        reasons.clear()
        history.clear()
    }

    fun history(): List<PalletTransition> = history.toList()

    companion object {
        private val terminalStages = setOf(Stage.STORED, Stage.BLOCKED)
    }
}

/** Immutable record of a single workflow transition. */
data class PalletTransition(
    val palletId: String,
    val from: PalletWorkflowEngine.Stage?,
    val to: PalletWorkflowEngine.Stage,
    val reason: String,
    val accepted: Boolean,
    val errorCode: String = "",
)
