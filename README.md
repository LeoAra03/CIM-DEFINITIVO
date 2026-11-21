# CIM — Industrial Cell Integration Platform

CIM is a multi-station industrial automation platform: one Android
coordinator, up to five Android station applications, four ESP32 firmware
targets, and a shared JVM communication core. The lab simulates PLC control,
robotic manufacturing, vision inspection, and pallet storage inside a single
coordinated cell.

The repository also documents the simulated test plan, the ESP32
manufacturing workflow, risk controls, audit evidence, and the implementation
roadmap.

## Repository layout

- `android/` — Android applications and the `core-network` communication
  library.
- `firmware/` — ESP32 sketches for the PLC master, robot station, vision
  node, and rack node.
- `docs/` — architecture, protocol, testing, hardware, safety, audit, and
  roadmap documentation.
- `tools/` — Python validation, export, and simulation utilities.
- `config/` — Gradle wrapper, version catalog, shared build logic, and
  convention plugins.
- `scripts/` — layout migration and history rewrite helpers.
- `.github/` — continuous integration, issue, and pull request templates.
- `web-preview/` — static reference preview of the coordinator experience.

## Android modules

| Module | Package | Purpose |
| --- | --- | --- |
| `app-coordinador` | `com.industria.coordinacion` | Hub, dashboard, authorization, BLE/TCP orchestration |
| `app-plc` | `com.industria.plc` | PLC relay station, local event log, E-stop |
| `app-manufactura` | `com.industria.manufactura` | Robot lifecycle, manufacturing sequence, operator handoff |
| `app-calidad` | `com.industria.calidad` | Vision, pallet validation, ARUCO inspection, G-code |
| `app-almacen` | `com.industria.almacenamiento` | Pallet intake, storage, rack position verification |
| `wear-coordinador` | `com.industria.wear` | Wear OS emergency interface |
| `core-network` | `com.industria.prefecto` | Shared communication and domain logic |

All applications depend on `core-network`. The coordinator is the TCP hub on
port `8888`; every station connects as a client and identifies itself with a
stable CIM UUID.

## UI screenshots

### Coordinator

![Coordinator dashboard](docs/assets/imagenes/coordinador_dashboard.png)

### PLC station

![PLC station](docs/assets/imagenes/plc_station.png)

### Manufacturing station

![Manufacturing station](docs/assets/imagenes/ui_app_manufactura.png)

### Quality station

![Quality station](docs/assets/imagenes/ui_app_calidad.png)

## Local validation

Use Python 3.10 or newer:

```bash
python3 tools/validate_system.py
python3 tools/validate_firmware_contract.py
python3 tools/validate_security.py
python3 tools/prehardware_readiness.py
```

Use the Gradle wrapper from `config/`:

```bash
cd config
./gradlew testAllModules detekt ktlintCheck
./gradlew validateApks
./gradlew writeApkChecksums
```

The Python gates validate the repository structure, firmware command
contract, signing posture, and pre-hardware readiness. The Gradle tasks run
JVM unit tests, static analysis, APK builds, and APK checksum generation.
The full continuous integration workflow is defined in
`.github/workflows/ci.yml`.

## Documentation map

- [Architecture](docs/ARCHITECTURE.md)
- [CIM protocol](docs/PROTOCOL.md)
- [Testing and simulation](docs/TESTING.md)
- [Hardware](docs/HARDWARE.md)
- [Safety](docs/SAFETY.md)
- [Risk management](docs/RISK.md)
- [Audit and engineering evidence](docs/AUDIT.md)
- [Status report](docs/REPORT.md)
- [Presentation overview](docs/PRESENTATION.md)
- [Roadmap](docs/ROADMAP.md)
- [Decision log](docs/decisions/README.md)

## Build and release notes

- Android Studio and a local Java 17 toolchain are required for Gradle work.
- APK signing is configured from `config/release.properties` and the
  `CIM_RELEASE_STORE_PASSWORD` environment variable when present.
- Release versioning is `6.0.0`, with Android version code `600`.
- `web-preview/` is a static reference and does not replace the Android
  applications.

## License

Released under the MIT License. See [LICENSE](LICENSE).
