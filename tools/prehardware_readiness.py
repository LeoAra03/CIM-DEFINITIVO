#!/usr/bin/env python3
"""Pre-hardware release readiness gate.

Verifies that the documentation set required before any physical station is
connected is present, that the safety document carries the lab-only
disclaimer, and that the protocol document defines the CIM handshake.

Exit code 0 means the repository is ready for the pre-hardware review.
"""
from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]

REQUIRED_DOCS = [
    "docs/TESTING.md",
    "docs/SAFETY.md",
    "docs/HARDWARE.md",
    "docs/PROTOCOL.md",
    "docs/ROADMAP.md",
]

SAFETY_PHRASES = [
    "not certified",
    "laboratory",
]

PROTOCOL_PHRASES = [
    "CIM_MASTER_HUB_V1",
    "8888",
]


def main() -> int:
    errors: list[str] = []

    for rel in REQUIRED_DOCS:
        if not (ROOT / rel).exists():
            errors.append(f"missing required document: {rel}")

    safety = ROOT / "docs/SAFETY.md"
    if safety.exists():
        text = safety.read_text(encoding="utf-8").lower()
        for phrase in SAFETY_PHRASES:
            if phrase.lower() not in text:
                errors.append(f"docs/SAFETY.md must state that the system {phrase}")

    protocol = ROOT / "docs/PROTOCOL.md"
    if protocol.exists():
        text = protocol.read_text(encoding="utf-8")
        for phrase in PROTOCOL_PHRASES:
            if phrase not in text:
                errors.append(f"docs/PROTOCOL.md must document {phrase}")

    tests = ROOT / "docs/TESTING.md"
    if tests.exists():
        text = tests.read_text(encoding="utf-8")
        if "1000" not in text and "1,000" not in text:
            errors.append("docs/TESTING.md must reference the 1000-iteration performance run")

    if errors:
        print("PRE-HARDWARE READINESS FAILED", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("PRE-HARDWARE READINESS PASSED: documentation gates are in place.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
