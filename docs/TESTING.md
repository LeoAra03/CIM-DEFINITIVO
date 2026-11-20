# CIM testing and simulation

## Test layers

| Layer | Location | Runner |
| --- | --- | --- |
| Repository gates | `tools/*.py` | Python 3.10+ |
| JVM unit tests | `android/*/src/test` | `./gradlew testAllModules` |
| Static analysis | whole repository | `./gradlew detekt ktlintCheck` |
| Continuous integration | `.github/workflows/ci.yml` | GitHub Actions |
| Firmware contract | `firmware/` + `docs/PROTOCOL.md` | `tools/validate_firmware_contract.py` |

## Python gates

```bash
python3 tools/validate_system.py
python3 tools/validate_firmware_contract.py
python3 tools/validate_security.py
python3 tools/prehardware_readiness.py
```

- `validate_system.py` enforces the target tree, single Gradle wrapper,
  version `6.0.0`/`600`, and the absence of migration markers.
- `validate_firmware_contract.py` fails when documentation and firmware
  disagree on a command.
- `validate_security.py` fails when signing material or inline secrets are
  tracked.
- `prehardware_readiness.py` fails when the safety and testing documents are
  incomplete.

## JVM unit tests

Core coverage lives in `core-network/src/test/kotlin`:

- `PalletWorkflowEngineTest` — happy path, illegal transitions, block/reset.
- `CimBinaryCodecTest` — round trip, CRC corruption, oversize, delimiter.
- `StationIdentityPolicyTest` — configured UUID, MAC normalization, rejects.
- `AuthorizationManagerTest` — unauthorized, validate/deny, deny/validate,
  blocked-not-observable, all with an in-memory `FakePermissions`.
- `PerformanceTest` — count-based dispatch throughput gate: 1000 frames must
  complete inside the CI budget; the 5000-iteration run is the local
  benchmark bound.

Run everything:

```bash
cd config
./gradlew testAllModules detekt ktlintCheck
```

## Simulation

`tools/simulate_cell.py` runs a headless cell simulation against a local
coordinator without hardware. `tools/yolo_export.py` converts a YOLO
checkpoint to TensorFlow Lite for the quality station.

## Bench workflow

1. Run all Python gates.
2. Run `testAllModules detekt ktlintCheck`.
3. Build and verify APKs: `./gradlew buildAllApks validateApks
   writeApkChecksums`.
4. Flash one firmware target and run `tools/simulate_cell.py`.
5. Only then connect physical stations, following `docs/SAFETY.md`.
