# Contributing

This repository accepts contributions that improve the Android applications,
the ESP32 firmware, the shared protocol, or the documentation set.

## Ground rules

- Keep production code, comments, KDoc, and documentation in English.
- Keep UI-facing text in `strings.xml` resources; string literals in Kotlin
  are reserved for protocol and machine-readable values.
- Do not add emoji, dead code, commented-out blocks, or debugging markers.
- Prefer explicit imports over wildcard imports.
- Keep the Gradle version catalog in `config/libs.versions.toml` as the
  single source of truth for dependency versions.

## Development environment

Prerequisites:

- Android Studio
- JDK 17
- Android SDK 35
- Gradle 8.13 through the wrapper in `config/`
- Python 3.10+

Install Python tooling before running the simulation utilities:

```bash
python -m pip install -r tools/requirements.txt
```

## Workflow

1. Create a feature branch from `CIM-DEFINITIVO`.
2. Make the change with focused commits.
3. Run the local gates:

```bash
python3 tools/validate_system.py
python3 tools/validate_firmware_contract.py
python3 tools/validate_security.py
python3 tools/prehardware_readiness.py
```

4. Run the Android checks:

```bash
cd config
./gradlew testAllModules detekt ktlintCheck
./gradlew validateApks
```

5. Open a pull request using `.github/pull_request_template.md`.

## Firmware changes

- ESP32 sources live under `firmware/<target>/src`.
- Add or change dispatch commands in `firmware/shared/cim_ble_firmware.h`.
- Document every command in `docs/PROTOCOL.md`; the contract gate fails when
  documentation and firmware disagree.

## Documentation changes

- Update `docs/ARCHITECTURE.md`, `docs/PROTOCOL.md`, or `docs/TESTING.md`
  when behavior changes.
- Record accepted architectural choices in `docs/decisions/`.

## Commit expectations

- One logical change per commit.
- Reference the affected module, for example `core-network:` or
  `firmware/plc-master:`.
- Never commit keystores, `release.properties`, generated APKs, model
  weights, or build outputs.
