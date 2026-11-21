# CIM audit and engineering evidence

## Automated gates

| Gate | Command | Evidence |
| --- | --- | --- |
| Structure and versions | `python3 tools/validate_system.py` | target tree, single wrapper, `6.0.0`/`600` |
| Firmware contract | `python3 tools/validate_firmware_contract.py` | documented = implemented commands |
| Security posture | `python3 tools/validate_security.py` | no tracked keystores or inline secrets |
| Pre-hardware readiness | `python3 tools/prehardware_readiness.py` | safety and testing docs present |
| Unit tests | `cd config && ./gradlew testAllModules` | workflow, codec, identity, authorization, performance |
| Static analysis | `cd config && ./gradlew detekt ktlintCheck` | style and complexity rules |
| APK verification | `cd config && ./gradlew buildAllApks validateApks` | twelve expected APK artifacts |

## Runtime audit trail

`CimAuditChain` maintains an append-only, SHA-256-linked record of
authorization decisions and command routing outcomes. Entries carry
sequence, timestamp, action, actor, subject, result, and the previous hash;
`verify()` walks the chain and fails on any mutation.

## Traceability

- Wire constants: `CimProtocol` <-> `docs/PROTOCOL.md`.
- Firmware commands: `firmware/shared/cim_ble_firmware.h` <->
  `docs/PROTOCOL.md` via the contract gate.
- Architecture choices: `docs/decisions/`.
- Status history: `CHANGELOG.md`.

## Evidence retention

CI logs from `.github/workflows/ci.yml` and local gate output are retained
for the duration of the academic term. Audit chain entries are in-memory by
design; deployments that need durable audit export must add a storage sink.
