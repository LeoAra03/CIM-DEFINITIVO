# CIM protocol

Protocol source of truth for every CIM station, the coordinator, and the
ESP32 firmware. Constants live in `CimProtocol` (`core-network`); the
firmware contract gate compares this document against the firmware dispatch
table.

## Transport

| Layer | Value |
| --- | --- |
| TCP hub port | `8888` |
| Handshake version | `CIM_MASTER_HUB_V1` |
| Pairing secret | `cim-cell-2026` |
| BLE service | `6e400001-b5a3-f393-e0a9-e50e24dcca9e` |
| BLE RX characteristic | `6e400002-b5a3-f393-e0a9-e50e24dcca9e` |
| BLE TX characteristic | `6e400003-b5a3-f393-e0a9-e50e24dcca9e` |
| SPP profile UUID | `00001101-0000-1000-8000-00805f9b34fb` |

Stations connect as TCP clients; the coordinator is the hub. TLS is
lab-disabled (`USE_TLS = false`); the Android network security configuration
restricts cleartext traffic to the lab subnet.

## Station identity

| Station | UUID |
| --- | --- |
| Storage | `CIM-ST-ALM-X1` |
| Manufacturing | `CIM-ST-MAN-X2` |
| Quality | `CIM-ST-CAL-X3` |
| PLC | `CIM-ST-PLC-X4` |

The ESP32 firmware writes the configured `STATION_UUID` into its
identification response; the hub accepts the configured UUID without local
guessing.

## Framing

`CimBinaryCodec` frame layout:

```
SOH (0x02) | version (1) | length (u16, big endian) | body | CRC32 (u32, big endian) | EOF (0x03)
```

- `body` is UTF-8.
- `MAX_BODY_BYTES = 65535`.
- Malformed, oversized, or corrupted frames throw `MalformedFrameException`.
- The legacy `;`-delimited string form is still accepted by
  `CimMessage.fromTransportString` during the migration window.

## Handshake

Station to hub:

```
CIM|1|CIM_MASTER_HUB_V1|<sourceMac>|<sourceApp>
```

The permission handshake payload carries `stationName|password|mac|uuid`;
the hub validates the password against the pairing secret with plain equality
(`CimProtocol.isPairingSecretValid`). The hub answers with
`PERMISSION_GRANTED` or `PERMISSION_DENIED`.

## Station status

Station to hub heartbeats use `STATUS;<UUID>;<STATE>` with states `IDLE`,
`BUSY`, or `ERROR`.

## Command surface

Station to hub events:

| Command | Payload |
| --- | --- |
| `STATUS;<UUID>;<STATE>` | liveness and state |
| `REQ_PERM` | permission request |
| `ARUCO_DETECTED:<id>` | marker sighting |
| `PALLET:<id>\|EVENT:<stage>` | workflow transition |

Hub to station commands:

| Command | Meaning |
| --- | --- |
| `GRANTED`, `ABORT` | authorization answer, abort all motion |
| `STO:<pos>`, `FREE:<pos>`, `CHK:<pos>` | rack slot control |
| `R:HOME`, `R:READY`, `R:MOVE:X:<delta>`, `R:RUN <prog>`, `R:DISCARD` | robot control |
| `L:HOME`, `L:START`, `L:STOP`, `L:Z_UP`, `L:Z_DOWN` | laser control |
| `CAM:SNAP`, `CAM:YOLO` | camera capture |
| `VAL:PASS`, `VAL:FAIL` | inspection result |
| `PLC:START`, `PLC:STOP` | conveyor relay |
| `DELIVER:<from>:<to>`, `FREE:<from>:<to>` | pallet flow |
| `E_STOP` | immediate safe stop |
| `STATS:RESET` | reset cell counters |

The first token of every table row above is extracted by
`tools/validate_firmware_contract.py` and must exist as a
`handle...("<COMMAND>")` entry in `firmware/shared/cim_ble_firmware.h`.

## Authorization states

Per device, `AuthorizationManager` moves through `PENDING`, `VALIDATED`,
`DENIED`, and `BLOCKED`. Only `VALIDATED` permits command routing; `BLOCKED`
is persisted by `PermissionManager` and takes precedence on reconnection.
