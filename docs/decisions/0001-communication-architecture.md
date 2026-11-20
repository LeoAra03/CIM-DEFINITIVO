# 0001. Communication architecture

Date: 2026-01-14. Status: accepted.

## Context

The cell needs deterministic, low-latency messaging between one hub and
five stations, plus a BLE link to ESP32 peripherals. The previous code base
split responsibilities across ad-hoc helpers with overlapping concerns.

## Decision

- The coordinator is the single TCP hub on port `8888`; stations are
  clients that identify with a configured CIM UUID.
- All wire constants live in `CimProtocol`; `CimBinaryCodec` owns framing
  and CRC validation.
- `AuthorizationManager` is the only path from session to command
  authorization; `PermissionManager` owns the persistent block list.
- BLE, SPP, and TCP share the `CommandBroker` transport multiplexer, and
  per-station frame writes are serialised by a station lock.

## Consequences

- One protocol document (`docs/PROTOCOL.md`) can be validated against both
  Kotlin and firmware sources.
- Adding a transport means implementing it behind the broker, not touching
  station logic.
- Session revocation is centralized at the hub on disconnect.
