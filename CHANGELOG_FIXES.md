# CHANGELOG_FIXES — CIM v6.0 (2026-05-31)

> **Documento consolidado:** Ver Anexo E en [`docs/ENTREGA_FINAL_LEONARDO_ARAYA.md`](docs/ENTREGA_FINAL_LEONARDO_ARAYA.md).

## Resumen
Auditoría y corrección del proyecto Practica_2 para entrega. Build Android y firmware ESP32 verificados en local.

## Correcciones críticas

### CommandBroker — API tests
- Añadido **`send(CimMessage)`** wrapper síncrono para tests y modo offline; corrige desalineación con suite de tests existente (`broker.send`).

### core-network — Bluetooth multiconexión (v6.0)
- **`BluetoothHardwareManager` reescrito** con soporte real de múltiples GATT simultáneos (`ConcurrentHashMap` por MAC).
- Añadidos **`connectionStates`** (`StateFlow`) y **`discoveredDevicesMap`** requeridos por la UI Compose.
- Escaneo **híbrido BLE + Classic Discovery** con filtro industrial (ESP32/CIM/NODO).
- Métodos **`connect(address)`**, **`disconnect(address)`**, **`reconnect(address)`**, **`disconnectAll()`**, **`sendToDevice(mac, cmd)`**.
- **Auto-reconexión** con backoff exponencial (1s → 30s máx.).
- Fragmentación MTU BLE (20 bytes, delay 20 ms) según manual `03_MOTOR_BLUETOOTH_HIBRIDO.md`.
- Nueva clase **`DiscoveredBluetoothDevice`**.

### CommandBroker
- Enrutamiento **SPP (Bluetooth Classic)** como fallback tras BLE.
- Compatible con `disconnectBleDevice` / `reconnectBleDevice` del coordinador.

### app-coordinador
- **Servidor SPP** arranca al conceder permisos BT y al iniciar hub TCP.
- Limpieza correcta en `onDestroy` (TCP + SPP + BLE).

### Firmware ESP32 (`firmware/Firmware_Support`)
- **`src/main/cim_esp32_firmware_v6.ino`**: firmware híbrido **BLE UART + SPP** alineado con Espressif/Arduino.
- **`huge_app.csv`**: tabla de particiones referenciada por `platformio.ini`.
- **`pio run`**: compilación exitosa (monitor 115200).

## Errores de compilación resueltos
| Archivo | Error | Fix |
|---------|-------|-----|
| `BluetoothComponents.kt` | Referencias a API inexistente en manager | API v6.0 implementada |
| `CommandBroker.kt` | `disconnect(mac)` / `reconnect(mac)` | Métodos añadidos al manager |
| `MainActivity.kt` (coordinador) | `disconnectAll()` | Alias añadido |

## Botones UI (ghost buttons)
- Revisión de `onClick` en las 5 apps: **sin handlers vacíos ni TODO**.
- Botones de hardware deshabilitados **solo cuando** `!isConnectedBt || !isAuthorized` (comportamiento esperado).
- Botones "Simular *" registran en terminal local (modo demo sin hardware).

## Verificación de build

```
./gradlew testAllModules buildAllApks  → BUILD SUCCESSFUL
pio run (Firmware_Support)             → SUCCESS
```

APKs generados en: `output-apks/`
- app-coordinador.apk
- app-plc.apk
- app-calidad.apk
- app-manufactura.apk
- app-almacen.apk

## Pendiente de hardware físico (no verificable en CI)
- Emparejamiento BLE/SPP real con 2+ ESP32 simultáneos.
- Latencia y reconexión en campo con interferencia RF.
- Ejecución de comandos R:/L:/C:/STO:/CAM: en actuadores reales.
- Cámara ArUco/QR en dispositivo con permisos concedidos.
- Red TCP entre estaciones y hub (IP real del coordinador).

## Notas de despliegue
1. Flashear firmware: `.\scripts\hardware-testing\flash_and_monitor_esp32.ps1` o `cd firmware/Firmware_Support && pio run -t upload`
2. Simular ESP32: `.\scripts\hardware-testing\simulate_esp32.ps1`
3. Instalar APKs desde `output-apks/`
4. En coordinador: iniciar **Servidor Hub** (TCP:8888) antes de vincular estaciones.
5. Conceder permisos Bluetooth, ubicación y cámara en cada app.

---

## Sesión de entrega profesional (2026-05-31 — polish final)

### Documentación nueva
| Archivo | Descripción |
|---------|-------------|
| `docs/GUIA_PROFESIONAL_CIM.md` | Guía completa con diagramas Mermaid + imagen arquitectura |
| `docs/TEST_MATRIX.md` | Matriz de 30 tests multi-tipo con estado PASS |
| `docs/EXTENSIONS_AND_TOOLING.md` | Extensiones Cursor/VS Code y toolchain |
| `docs/ESP32_SIMULACION_Y_HARDWARE.md` | Simulación Wokwi/PIO + flash real |
| `docs/images/cim_arquitectura_v6.png` | Diagrama de arquitectura generado |

### Scripts hardware
- `scripts/hardware-testing/simulate_esp32.ps1` — simulación Wokwi / PIO / Python
- `scripts/hardware-testing/flash_and_monitor_esp32.ps1` — upload + monitor 115200

### Tests añadidos (core-network)
- `DeviceRegistryTest` — O(1) registry + performance 1000 lookups
- `BluetoothFilterTest` — validación MAC y filtro industrial
- `CimStressAndAcceptanceTest` — stress, auth denial, happy path PO

### Build / QA
- `testAllModules` ampliado: core-network + app-coordinador + app-plc
- `.gitignore` actualizado: `.pio/`, `espessif/`, artefactos root duplicados

## Sesión auditoría final de entrega (2026-05-31)

### Correcciones build / firmware
| Issue | Fix |
|-------|-----|
| `cleanBuildAll` fallaba (`Task 'clean' not found`) | Nueva tarea `cleanAllModules` + `mustRunAfter` |
| `buildFirmware` ruta obsoleta | Apunta a `firmware/Firmware_Support/build_firmware.ps1` |
| `simulacion_esp32` no compilaba | `main.cpp` movido a `src/main.cpp` |
| `build_firmware.ps1` fuente inexistente | Prioriza `src/main.ino` |

### Documentación nueva
- `docs/GUIA_ENTREGA_FINAL.md` — manual de entrega con tablas ESP32/APK/manuales
- `docs/INFORME_FUNCIONALIDAD.md` — matriz % funcional, riesgos, roadmap 100%

### Reorganización
- Duplicados root → eliminados (copias canónicas en `docs/logs/`)
- `README.md` actualizado con estructura Espressif-style y pointers de entrega
- `output-apks/cim_esp32_firmware_v6.bin` refrescado desde build PIO

### Verificación
```
./gradlew cleanBuildAll              → BUILD SUCCESSFUL (8m 10s)
pio run (Firmware_Support)           → SUCCESS
pio run (simulacion_esp32)           → SUCCESS
Puntuación global ponderada: 83 %
```

- Documentado en `TEST_MATRIX.md` y `GUIA_PROFESIONAL_CIM.md`
