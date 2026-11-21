# CIM risk register

| ID | Risk | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- | --- |
| R-01 | Unauthenticated command reaches an actuator | Low | High | Firmware auth gate; `AuthorizationManager` state machine; `PermissionManager` block list |
| R-02 | Documentation drifts from firmware dispatch | Medium | High | `validate_firmware_contract.py` in CI |
| R-03 | Signing keystore committed to the repository | Low | High | `validate_security.py`; git-ignored `*.jks`, `release.properties` |
| R-04 | Station identity spoofing over BLE | Medium | Medium | `StationIdentityPolicy` with configured UUID registry |
| R-05 | Corrupt or oversized frame crashes a station | Low | Medium | `CimBinaryCodec` length and CRC validation |
| R-06 | Pallet advances through an illegal stage | Low | Medium | `PalletWorkflowEngine` deterministic transition table |
| R-07 | Relay stays energized after authorization loss | Low | High | Firmware auth watchdog drops the relay |
| R-08 | Version skew between APKs | Low | Medium | Single version catalog; `6.0.0`/`600` enforced by `validate_system.py` |
| R-09 | Operator approves an unknown device | Medium | Medium | Identity UUID check before approval; audit chain entries |
| R-10 | CI does not run after refactor | Low | Medium | `.github/workflows/ci.yml` on push and pull request |

## Review cadence

The register is reviewed at every roadmap milestone in
`docs/ROADMAP.md`. New risks are added with a decision record when the
mitigation changes architecture.
