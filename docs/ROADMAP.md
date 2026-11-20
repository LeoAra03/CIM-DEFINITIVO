# CIM roadmap

## Milestone 6 — repository professionalization (current)

- Target tree with flattened `android/<module>/src` and root `firmware/`.
- Single Gradle wrapper, convention plugins, unified version catalog.
- English documentation set and decision records.
- Python gates and CI workflow green.

## Milestone 7 — bench integration

- Flash the PLC master and verify relay plus proximity behavior.
- Run `tools/simulate_cell.py` against a real coordinator instance.
- Complete the pre-hardware checklist in `docs/TESTING.md`.
- Record audit chain entries for a full simulated pallet run.

## Milestone 8 — protocol hardening

- Enable TLS for non-lab deployments with the pinned CA.
- Move audit chain entries to durable storage.
- Extend `StationRegistry` with firmware version telemetry.
- Publish signed release APKs from CI with the protected keystore.

## Milestone 9 — multi-cell expansion

- Multi-hub federation over the station registry.
- Wear OS approval flows in addition to the emergency stop.
- OTA firmware update pipeline with signed images.

## Backlog

- Compose Preview test coverage for each station screen.
- Deterministic clock injection for workflow timestamps.
- Fuzzing harness for `CimBinaryCodec`.
