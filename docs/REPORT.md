# CIM status report

Report date: 2026-01-14. Version: `6.0.0` (version code `600`).

## Delivered

- Repository restructured to the target tree: `android/<module>/src`,
  `firmware/<target>/src`, single Gradle wrapper under `config/`.
- Package namespace unified under `com.industria.*` with the shared
  `core-network` module at `com.industria.prefecto`.
- Frozen wire constants, binary frame codec, pallet workflow engine,
  station identity policy, authorization manager, audit chain, station
  registry, and G-code translator in `core-network`.
- Four replacement JVM test suites plus the count-based `PerformanceTest`.
- Python gates for structure, firmware contract, security posture, and
  pre-hardware readiness.
- English documentation set with three decision records.
- Continuous integration workflow with structural, analysis, test, and APK
  stages.

## Verified locally

- `tools/validate_system.py` — pass.
- `tools/validate_firmware_contract.py` — pass (8 documented commands).
- `tools/validate_security.py` — pass.
- `tools/prehardware_readiness.py` — pass.

## Remaining before physical stations

1. Run the full Gradle suite on a machine with JDK 17 and the Android SDK.
2. Flash one firmware target and complete the bench checklist in
   `docs/HARDWARE.md`.
3. Execute the simulated cell run in `docs/TESTING.md`.
4. Perform the history consolidation with `scripts/rewrite-history.sh`
   (manual, destructive step owned by the maintainer).

## Known limitations

- TLS is disabled for the lab network; see `docs/PROTOCOL.md`.
- The audit chain is in-memory; persistence is a deployment concern.
- The wear module targets `minSdk 30` because of Wear Compose
  requirements.
