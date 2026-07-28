# Sistema Industrial CIM v6.0 — Manual de implementación activo

Este documento resume la arquitectura, despliegue y operación del sistema de manufactura flexible CIM desde la estructura activa del repositorio.

## Aplicaciones Android

El sistema consta de cinco estaciones Android principales, una app Wear y una librería compartida:

1. `app-coordinador`: hub de red, autorización y supervisión.
2. `app-manufactura`: robot Scorbot, láser/G-code y visión de apoyo.
3. `app-calidad`: inspección por visión ArUco/YOLO y validación PASS/FAIL.
4. `app-almacen`: gestión de racks, almacenamiento y retiro.
5. `app-plc`: cinta transportadora, sensores y eventos de pallet.
6. `wear-coordinador`: supervisión compacta Wear OS.
7. `core-network`: protocolo, registro, BLE/TCP, visión y utilidades comunes.

## Firmware ESP32/Wemos D1 R32

La carpeta canónica es `esp32/firmware/`:

- `esp32_plc_master.ino`
- `esp32_scorbot_manufactura.ino`
- `esp32_scorbot_calidad.ino`
- `esp32_scorbot_almacen.ino`
- `cim_ble_firmware.h`

No usar firmwares desde `archive/` para nuevas pruebas.

## Protocolo de red

La comunicación es híbrida:

1. Bluetooth/BLE entre la app de estación y su hardware local ESP32.
2. TCP/Wi-Fi entre estaciones Android y Coordinador.

Formato de mensaje principal:

```text
ID|TIMESTAMP|SOURCE_MAC|SOURCE_APP|DEST_MAC|DEST_APP|CMD|PRIORITY|SESSION|PAYLOAD
```

El handshake Android transporta el token de emparejamiento como `sha256:<hash>` y requiere aprobación del Coordinador.

## Validación automática

Desde la raíz:

```bash
python3 tools/validate_system_100.py
```

Desde `config/`:

```bash
./gradlew testAllModules
./gradlew lintAll
./gradlew buildAllApks validateApks writeApkChecksums
```

## Ubicación de artefactos

- APKs debug: `config/output-apks/*.apk`
- Checksums: `config/output-apks/SHA256SUMS.txt`
- Reporte estructural opcional: `logs/system_100_validation.json`
- Scripts PowerShell: `tools/powershell/`

## Despliegue básico

1. Generar APKs con Gradle o descargar artefactos de CI.
2. Instalar APKs con ADB o `tools/powershell/Instalar-APKs.ps1`.
3. Abrir Coordinador, iniciar el hub y revisar solicitudes.
4. Abrir cada estación, usar la IP descubierta/indicada y solicitar autorización.
5. Para hardware físico, flashear desde `esp32/firmware/` y registrar evidencia en la bitácora.
6. No energizar actuadores reales sin E-stop físico, límites e interlocks validados.
