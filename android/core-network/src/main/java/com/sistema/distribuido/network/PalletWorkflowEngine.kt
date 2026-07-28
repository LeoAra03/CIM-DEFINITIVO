package com.sistema.distribuido.network

/**
 * Máquina de estados determinista para pallets CIM.
 *
 * No controla actuadores: valida el orden de eventos antes de que Coordinación
 * pueda animar o enviar el siguiente comando. Un salto inválido deja el pallet
 * bloqueado para revisión humana.
 */
enum class PalletStage {
    REGISTERED,
    STORAGE_RELEASED,
    CONVEYOR_TO_MANUFACTURING,
    MANUFACTURING,
    CONVEYOR_TO_QUALITY,
    QUALITY_INSPECTION,
    APPROVED,
    REJECTED,
    STORED,
    BLOCKED
}

enum class PalletEvent {
    RELEASED_FROM_STORAGE,
    ARRIVED_MANUFACTURING,
    MANUFACTURING_COMPLETED,
    ARRIVED_QUALITY,
    QUALITY_PASS,
    QUALITY_FAIL,
    STORED,
    FAULT,
    RESET_AFTER_REVIEW
}

data class PalletSnapshot(
    val palletId: String,
    val stage: PalletStage = PalletStage.REGISTERED,
    val arucoId: Int? = null,
    val productId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val reason: String? = null
)

data class PalletTransition(
    val snapshot: PalletSnapshot,
    val accepted: Boolean,
    val message: String
)

class PalletWorkflowEngine {
    private val pallets = linkedMapOf<String, PalletSnapshot>()

    fun register(palletId: String, arucoId: Int? = null, productId: String? = null): PalletTransition {
        require(palletId.isNotBlank()) { "palletId no puede estar vacío" }
        if (pallets.containsKey(palletId)) {
            return PalletTransition(pallets.getValue(palletId), false, "PALLET_ALREADY_REGISTERED")
        }
        val snapshot = PalletSnapshot(palletId = palletId, arucoId = arucoId, productId = productId)
        pallets[palletId] = snapshot
        return PalletTransition(snapshot, true, "PALLET_REGISTERED")
    }

    fun get(palletId: String): PalletSnapshot? = pallets[palletId]
    fun all(): List<PalletSnapshot> = pallets.values.toList()

    fun apply(palletId: String, event: PalletEvent, reason: String? = null): PalletTransition {
        val current = pallets[palletId]
            ?: return PalletTransition(PalletSnapshot(palletId, stage = PalletStage.BLOCKED, reason = "PALLET_UNKNOWN"), false, "PALLET_UNKNOWN")

        val nextStage = when (event) {
            PalletEvent.FAULT -> PalletStage.BLOCKED
            PalletEvent.RESET_AFTER_REVIEW -> if (current.stage == PalletStage.BLOCKED) PalletStage.REGISTERED else null
            PalletEvent.RELEASED_FROM_STORAGE -> current.stage.takeIf { it == PalletStage.REGISTERED }?.let { PalletStage.STORAGE_RELEASED }
            PalletEvent.ARRIVED_MANUFACTURING -> current.stage.takeIf { it == PalletStage.STORAGE_RELEASED }?.let { PalletStage.CONVEYOR_TO_MANUFACTURING }
            PalletEvent.MANUFACTURING_COMPLETED -> current.stage.takeIf { it == PalletStage.CONVEYOR_TO_MANUFACTURING }?.let { PalletStage.CONVEYOR_TO_QUALITY }
            PalletEvent.ARRIVED_QUALITY -> current.stage.takeIf { it == PalletStage.CONVEYOR_TO_QUALITY }?.let { PalletStage.QUALITY_INSPECTION }
            PalletEvent.QUALITY_PASS -> current.stage.takeIf { it == PalletStage.QUALITY_INSPECTION }?.let { PalletStage.APPROVED }
            PalletEvent.QUALITY_FAIL -> current.stage.takeIf { it == PalletStage.QUALITY_INSPECTION }?.let { PalletStage.REJECTED }
            PalletEvent.STORED -> current.stage.takeIf { it == PalletStage.APPROVED || it == PalletStage.REJECTED }?.let { PalletStage.STORED }
        }

        if (nextStage == null) {
            val blocked = current.copy(stage = PalletStage.BLOCKED, updatedAt = System.currentTimeMillis(), reason = "INVALID_${current.stage}_$event")
            pallets[palletId] = blocked
            return PalletTransition(blocked, false, "INVALID_TRANSITION")
        }

        val updated = current.copy(stage = nextStage, updatedAt = System.currentTimeMillis(), reason = reason)
        pallets[palletId] = updated
        return PalletTransition(updated, true, "TRANSITION_ACCEPTED")
    }
}
