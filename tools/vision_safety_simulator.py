#!/usr/bin/env python3
"""Simulador determinista/fuzz de la decisión visión → ensamblaje.

No pretende sustituir una cámara, el modelo YOLO ni hardware. Verifica que la
lógica de orquestación nunca emita AUTO_ASSEMBLE con datos incompletos,
ambiguos o incompatibles. Ejecuta 1.000+ combinaciones aleatorias sin
bibliotecas externas.
"""
from __future__ import annotations

import argparse
import json
import random
from dataclasses import asdict, dataclass
from pathlib import Path

KNOWN_CLASSES = (
    "caballo_hembra", "caballo_macho",
    "craneo_hembra", "craneo_macho",
    "hacha_hembra", "hacha_macho",
    "lomoToro_hembra", "lomoToro_macho",
    "tuerca_macho",
)
MIN_CONFIDENCE = 0.85


@dataclass(frozen=True)
class VisionInput:
    pallet_id: str | None
    aruco_id: int | None
    detected_class: str
    confidence: float
    counterpart_class: str | None
    robot_homed: bool
    zone_clear: bool
    estop_active: bool


def family_and_sex(label: str) -> tuple[str, str] | None:
    if label.endswith("_macho"):
        return label.removesuffix("_macho"), "macho"
    if label.endswith("_hembra"):
        return label.removesuffix("_hembra"), "hembra"
    return None


def decide(item: VisionInput) -> str:
    """Contrato conservador: ante duda, revisión humana; nunca movimiento."""
    if item.estop_active:
        return "FAULT_ESTOP"
    if not item.pallet_id or item.aruco_id is None:
        return "REVIEW_REQUIRED_MISSING_ID"
    if item.detected_class not in KNOWN_CLASSES or item.confidence < MIN_CONFIDENCE:
        return "REVIEW_REQUIRED_LOW_CONFIDENCE"
    if not item.robot_homed or not item.zone_clear:
        return "HOLD_FOR_SAFETY"
    part = family_and_sex(item.detected_class)
    pair = family_and_sex(item.counterpart_class or "")
    if part is None or pair is None:
        return "REVIEW_REQUIRED_UNKNOWN_PAIR"
    if part[0] != pair[0] or part[1] == pair[1]:
        return "REJECT_INCOMPATIBLE_PAIR"
    return "AUTO_ASSEMBLE_CANDIDATE"


def random_case(rng: random.Random, index: int) -> VisionInput:
    labels = KNOWN_CLASSES + ("unknown", "", "pieza")
    return VisionInput(
        pallet_id=f"PAL-{index:04d}" if rng.random() > 0.12 else None,
        aruco_id=rng.randrange(0, 50) if rng.random() > 0.1 else None,
        detected_class=rng.choice(labels),
        confidence=rng.uniform(0.0, 1.0),
        counterpart_class=rng.choice(labels + (None,)),
        robot_homed=rng.random() > 0.15,
        zone_clear=rng.random() > 0.18,
        estop_active=rng.random() < 0.05,
    )


def assert_invariant(case: VisionInput, decision: str) -> None:
    if decision != "AUTO_ASSEMBLE_CANDIDATE":
        return
    assert case.confidence >= MIN_CONFIDENCE
    assert case.pallet_id and case.aruco_id is not None
    assert case.robot_homed and case.zone_clear and not case.estop_active
    left, right = family_and_sex(case.detected_class) or ("", "")
    other_left, other_right = family_and_sex(case.counterpart_class or "") or ("", "")
    assert left == other_left and {right, other_right} == {"macho", "hembra"}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cases", type=int, default=1000)
    parser.add_argument("--seed", type=int, default=20260728)
    parser.add_argument("--output", type=Path, default=Path("vision_safety_simulation.json"))
    args = parser.parse_args()
    if args.cases < 1:
        parser.error("--cases debe ser mayor que cero")

    rng = random.Random(args.seed)
    counts: dict[str, int] = {}
    examples: dict[str, dict] = {}
    for index in range(args.cases):
        case = random_case(rng, index)
        decision = decide(case)
        assert_invariant(case, decision)
        counts[decision] = counts.get(decision, 0) + 1
        examples.setdefault(decision, {"input": asdict(case), "decision": decision})

    report = {
        "cases": args.cases,
        "seed": args.seed,
        "minimumConfidence": MIN_CONFIDENCE,
        "invariant": "AUTO_ASSEMBLE_CANDIDATE only occurs for a known compatible male/female pair, valid pallet+ArUco IDs, clear zone, homed robot, no E-stop and confidence >= threshold.",
        "decisionCounts": counts,
        "examples": examples,
    }
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"\n✓ {args.cases} escenarios evaluados sin violar el contrato de seguridad.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
