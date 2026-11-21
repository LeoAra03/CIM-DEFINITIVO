# CIM hardware

## ESP32 targets

All four targets are based on the Wemos D1 ESP32 R32 (WROOM-32) and include
`firmware/shared/cim_ble_firmware.h`.

| Target | Sketch | Station type | Key pins |
| --- | --- | --- | --- |
| PLC master | `firmware/plc-master/src/plc_master.ino` | `PLC_CONTROLLER` | Relay `GPIO5`, proximity `GPIO34` |
| Scorbot | `firmware/scorbot/src/scorbot.ino` | `ROBOT_ARM` | UART2 `RX=16 TX=17` @ 9600 8N1 |
| Vision node | `firmware/vision-node/src/vision_node.ino` | `QUALITY_STATION` | Camera, laser control via UART |
| Rack node | `firmware/rack-node/src/rack_node.ino` | `STORAGE_STATION` | Rack actuator UART |

Shared firmware features:

- Nordic UART Service (NUS) BLE transport with chunked notifications.
- Command dispatcher with documented `handle...("<COMMAND>")` entries.
- Authorization watchdog: critical commands require `CIM_AUTH`, and the
  relay drops on auth expiry.
- Heartbeat refresh of the auth and relay timers.
- LED blink patterns for identification and acknowledgements.

## Android station hardware assumptions

- Bluetooth Low Energy (API 31+ uses `BLUETOOTH_SCAN` with
  `neverForLocation`).
- Bluetooth Classic SPP for the legacy serial profile.
- TCP/IP on the lab Wi-Fi subnet (`192.168.0.0/16`).
- Camera for the quality station (CameraX).
- Vibration motor for the wear emergency interface.

## Bench setup

1. Flash one target with Arduino IDE or `arduino-cli` using the sketch under
   `firmware/<target>/src`.
2. Power the board over USB; the LED on `GPIO2` blinks on identification.
3. Pair the station application over BLE and verify the `CIM_ID` response.
4. Run `tools/simulate_cell.py` before involving a second station.

## Toolchain

- Arduino ESP32 core with BLE and UART support.
- Python 3.10+ for `tools/`.
- Android Studio + JDK 17 for the application modules.
