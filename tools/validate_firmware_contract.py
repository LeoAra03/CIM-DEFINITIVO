#!/usr/bin/env python3
"""Firmware command contract gate.

Scans `firmware/**/*.ino` and `firmware/**/*.h` for `handle...("<COMMAND>")`
dispatch literals, then compares them against the first command token of
every table row in `docs/PROTOCOL.md`.

Exit code 0 means documentation and firmware agree.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
FIRMWARE = ROOT / "firmware"
PROTOCOL = ROOT / "docs" / "PROTOCOL.md"

IMPLEMENTED_PATTERN = re.compile(r'handle[A-Za-z]*\(\s*"([A-Z][A-Z0-9_:]+)"')
DOCUMENTED_PATTERN = re.compile(r"^\|\s*`([A-Z][A-Z0-9_:]*)`")


def implemented_commands() -> set[str]:
    found: set[str] = set()
    sources = list(FIRMWARE.rglob("*.ino")) + list(FIRMWARE.rglob("*.h"))
    for source in sources:
        text = source.read_text(encoding="utf-8")
        found.update(IMPLEMENTED_PATTERN.findall(text))
    return {command.rstrip(":") for command in found}


def documented_commands() -> set[str]:
    if not PROTOCOL.exists():
        raise SystemExit(f"missing protocol document: {PROTOCOL}")
    found: set[str] = set()
    for line in PROTOCOL.read_text(encoding="utf-8").splitlines():
        match = DOCUMENTED_PATTERN.match(line)
        if match:
            found.add(match.group(1).rstrip(":"))
    return found


def main() -> int:
    if not FIRMWARE.is_dir():
        print("FIRMWARE CONTRACT FAILED: firmware directory is missing.", file=sys.stderr)
        return 1

    implemented = implemented_commands()
    documented = documented_commands()

    errors: list[str] = []
    for command in sorted(documented - implemented):
        errors.append(f"documented but not implemented: {command}")
    for command in sorted(implemented - documented):
        errors.append(f"implemented but not documented: {command}")

    if errors:
        print("FIRMWARE CONTRACT FAILED", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "FIRMWARE CONTRACT PASSED: "
        f"{len(documented)} documented command(s) match the firmware dispatch table."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
