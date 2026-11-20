# CIM presentation overview

## One-liner

CIM is an academic industrial-cell simulator: one coordinator, five station
apps, four ESP32 targets, and a shared protocol core that keeps every
command authorized, traceable, and contract-checked.

## Problem

Industrial cells mix PLCs, robots, vision, and storage. Teaching their
integration usually means either expensive hardware or disconnected
simulators that never share one protocol.

## Solution

- A single TCP/BLE protocol defined in one place (`CimProtocol`) and one
  document (`docs/PROTOCOL.md`).
- Deterministic workflow state machines instead of ad-hoc event handling.
- Authorization as a first-class state machine with persistent blocking.
- Firmware and documentation locked together by an automated contract gate.

## Architecture at a glance

| Layer | Technology |
| --- | --- |
| UI | Jetpack Compose, Material 3, dark industrial theme |
| Station apps | Kotlin, Hilt, CameraX, ML Kit, TensorFlow Lite |
| Shared core | `core-network` (protocol, workflow, authorization, transports) |
| Field devices | ESP32 BLE NUS firmware with auth watchdog |
| Quality gates | Python gates + detekt + ktlint + JUnit in CI |

## Evidence

- 1000-iteration dispatch performance gate in `PerformanceTest`.
- Firmware contract gate green against `docs/PROTOCOL.md`.
- Security gate proving no signing material in the repository.
- Audit chain with SHA-256 linkage for authorization decisions.

## Demo script

1. Show `validate_system.py` and `validate_firmware_contract.py` passing.
2. Launch the coordinator dashboard and one station app.
3. Approve a permission request and route a command.
4. Trigger a pallet workflow event and watch the tracking tab advance.
5. Fire the wear E-stop and show the stations abort.

## Talking points

- Why one protocol source of truth matters more than any single feature.
- How the state machine removes whole classes of race conditions.
- Why the contract gate is the difference between a demo and a system.
