# CIM architecture

## Overview

CIM coordinates up to five Android station applications through one
coordinator application, with four ESP32 firmware targets attached as field
devices. All station logic shares the `core-network` JVM module.

```
+-------------------+        TCP hub :8888        +-------------------+
| app-coordinador   | <-------------------------> | app-plc           |
| (hub, dashboard)  | <-------------------------> | app-manufactura   |
+---------+---------+ <-------------------------> | app-calidad       |
          | BLE / SPP                           | app-almacen       |
          +-----------------------------------> +---------+---------+
                                                          |
                                                    ESP32 firmware
                                                    (BLE NUS link)
```

## Modules

### core-network

`com.industria.prefecto` contains the frozen wire constants (`CimProtocol`),
the compact frame codec (`CimBinaryCodec`), the pallet workflow state
machine (`PalletWorkflowEngine`), per-device authorization
(`AuthorizationManager`), the station identity policy, the audit chain, the
command broker, and the Bluetooth transport managers.

### Applications

| Module | Package | Role |
| --- | --- | --- |
| `app-coordinador` | `com.industria.coordinacion` | TCP hub, dashboard, operator authorization |
| `app-plc` | `com.industria.plc` | Relay control, local event log, E-stop |
| `app-manufactura` | `com.industria.manufactura` | Robot lifecycle and handoff |
| `app-calidad` | `com.industria.calidad` | Vision, pallet validation, ARUCO, G-code |
| `app-almacen` | `com.industria.almacenamiento` | Pallet intake, storage, rack verification |
| `wear-coordinador` | `com.industria.wear` | Wear OS emergency interface |

### Firmware

| Target | Path | Device |
| --- | --- | --- |
| PLC master | `firmware/plc-master/src/plc_master.ino` | Conveyor relay and proximity sensor |
| Scorbot | `firmware/scorbot/src/scorbot.ino` | Robot arm UART bridge |
| Vision node | `firmware/vision-node/src/vision_node.ino` | Camera and quality laser |
| Rack node | `firmware/rack-node/src/rack_node.ino` | Pallet rack storage actuator |

All targets include `firmware/shared/cim_ble_firmware.h`, which owns BLE
transport, the command dispatcher, and the authorization watchdog.

## Session lifecycle

1. The station opens a TCP connection and sends the CIM handshake.
2. The hub validates the pairing secret and records the session.
3. The operator approves or rejects the permission request.
4. The station receives `PERMISSION_GRANTED` and may send commands.
5. On disconnect, the hub revokes the session authorization.

## Build layout

Gradle lives in `config/` with a single wrapper, the version catalog
`config/libs.versions.toml`, and the convention plugins under
`config/build-logic/`. Application modules only declare
`id("cim.android.application")` plus their extra dependencies; the shared
library applies `id("cim.android.library")`.
