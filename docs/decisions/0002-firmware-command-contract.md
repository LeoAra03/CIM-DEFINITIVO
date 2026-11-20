# 0002. Firmware command contract

Date: 2026-01-14. Status: accepted.

## Context

Firmware and documentation previously evolved independently; commands
existed in one place but not the other, and nothing failed when they
diverged.

## Decision

- Every hub-to-station command in `docs/PROTOCOL.md` must appear in
  `firmware/shared/cim_ble_firmware.h` as a `handle...("<COMMAND>")` entry.
- `tools/validate_firmware_contract.py` extracts both sets and fails CI on
  any difference, in either direction.
- The dispatcher keeps non-documented housekeeping frames (IDENTIFY,
  HEARTBEAT, STATUS) as plain comparisons so they stay out of the
  contract surface.

## Consequences

- A protocol change without a firmware change breaks the build, which is
  the intended forcing function.
- New commands require a table row and a handler in the same change.
- The gate scans `.ino` and `.h` sources because the shared header owns the
  dispatcher.
