# Firmware ESP32 — Wemos D1 ESP32 R32

Firmware BLE para la placa **Wemos D1 ESP32 R32** (ESP-WROOM-32), compatible con las APKs Android CIM.

## Pinout (Wemos D1 R32)

| GPIO | Función |
|------|---------|
| 2 | LED onboard (estado) |
| 16 | Serial2 RX → Scorbot |
| 17 | Serial2 TX → Scorbot |
| 5 | Relé cinta (solo PLC) |
| 34 | Sensor proximidad (solo PLC, input) |

## Sketches

| Archivo | DEVICE_NAME | Estación |
|---------|-------------|----------|
| `esp32_scorbot_manufactura.ino` | CIM_SCORBOT_MAN | Manufactura |
| `esp32_scorbot_calidad.ino` | CIM_SCORBOT_CAL | Calidad |
| `esp32_scorbot_almacen.ino` | CIM_SCORBOT_ALM | Almacén |
| `esp32_plc_master.ino` | CIM_PLC_MASTER | PLC / Cinta |

## Flasheo (Arduino IDE)

1. Placa: **ESP32 Dev Module** (o Wemos D1 R32)
2. Abrir el `.ino` correspondiente a la estación
3. `cim_ble_firmware.h` debe estar en la misma carpeta
4. Subir por USB (115200 baud serial monitor)

## Carpeta lista para prueba (Scorbot Manufactura)

- Carpeta Arduino lista: `esp32/firmware/arduino/esp32_scorbot_manufactura/`
- Abrir en Arduino IDE: `esp32_scorbot_manufactura.ino`
- Dependencias: Core **ESP32 by Espressif Systems** (incluye la librería `BLE` usada por `BLEDevice.h`)
- Nombre BLE esperado: `CIM_SCORBOT_MAN`
- Salida serial esperada al iniciar (115200):
  - `CIM BLE Firmware - Wemos D1 ESP32 R32`
  - `Dispositivo: CIM_SCORBOT_MAN`
  - `Esperando conexion Android...`

## Protocolo BLE

- Servicio Nordic UART: `6E400001-B5A3-F393-E0A9-E50E24DCCA9E`
- La app Android envía comandos como `R:HOME`, `L:ARUCO:0`, `IDENTIFY|...`
- El firmware responde con `ROBOT_ARM|timestamp|RESP|...`

## Scorbot

Serial2 a 9600 8N1 en GPIO 16/17. Comandos reenviados: `HOME`, `HERE`, `RUN`, `MOVE`, etc.
