# Changelog

All notable changes to this project are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [6.0.0] - 2026-01-14

### Added

- `validate_firmware_contract.py` for documented-versus-implemented command
  checks.
- `validate_security.py` for repository signing and secret posture checks.
- `prehardware_readiness.py` for pre-hardware gate evidence.
- `scripts/migrate-layout.sh` for the repository layout migration.
- `scripts/rewrite-history.sh` for the authorized history consolidation.
- Decision records for the communication architecture, firmware contract, and
  repository policy.

### Changed

- Application packages now use the `com.industria.*` namespaces.
- Shared communication logic moved into `core-network`.
- Application sources use a flattened `android/<module>/src` layout.
- Repository language policy is English for source, comments, KDoc, and
  documentation.
- Release versioning is normalized to `6.0.0` with Android version code
  `600`.
- Continuous integration now uses `.github/workflows/ci.yml`.

### Fixed

- Firmware dispatch now contains explicit handlers for every documented
  command.
- Removed repository history markers, duplicate wrappers, and stale
  generated artifacts from the active tree.

## [5.0.0] - 2026-01-14

### Changed

- Expanded the Android simulator and documentation set.
- Hardened BLE identity validation.
- Added pre-hardware release gates and CI workflow.

### Fixed

- Cleaned repository history, documentation, and binary artifacts.

## [4.1.0] - 2026-01-14

### Changed

- Renamed application packages to `com.industria.*`.
- Added Android instrumented smoke tests.
- Centralized Gradle version management.

## [4.0.0] - 2026-01-14

### Added

- Hilt-based dependency injection and Bluetooth manager lifecycle.
- BLE, SPP, and TCP communication with station authorization.
- Android Studio development environment.

### Changed

- Renamed application packages and updated dependencies.
