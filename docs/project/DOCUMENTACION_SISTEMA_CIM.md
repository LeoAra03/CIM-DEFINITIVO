# Sistema Industrial CIM v6.0 — Manual de implementación activo

<p align="center"><img src="../assets/ubb_logo.png" alt="Universidad del Bío-Bío" width="280"></p>

## Propósito

El sistema CIM integra estaciones Android, una capa de red compartida y firmware ESP32/Wemos para coordinar un flujo de manufactura, calidad y almacenamiento. Este manual describe las fuentes activas y el uso reproducible del proyecto; no certifica seguridad funcional ni reemplaza la documentación de laboratorio.

## Arquitectura activa

| Capa | Ubicación | Responsabilidad |
|---|---|---|
| Coordinación | `android/apps/app-coordinador` | Hub, autorización, orquestación y supervisión. |
| PLC | `android/apps/app-plc` | Cinta, sensores y eventos de pallet. |
| Manufactura | `android/apps/app-manufactura` | Robot, posiciones, G-code y orden de láser. |
| Calidad | `android/apps/app-calidad` | Cámara, ArUco, visión y decisión PASS/FAIL. |
| Almacén | `android/apps/app-almacen` | Rack, almacenamiento y retiro. |
| Supervisión | `android/apps/wear-coordinador` | Vista compacta Wear OS. |
| Red | `android/core-network` | Protocolo, identidad, BLE/TCP y utilidades comunes. |
| Firmware | `esp32/firmware` | Firmware canónico de las estaciones. |

## Comunicación e identidad

La comunicación combina BLE entre estación Android y hardware local, y TCP/Wi-Fi entre estaciones Android y Coordinador. El formato de mensaje principal es:

```text
ID|TIMESTAMP|SOURCE_MAC|SOURCE_APP|DEST_MAC|DEST_APP|CMD|PRIORITY|SESSION|PAYLOAD
```

Cada estación debe ser validada por UUID, tipo y capacidades. El handshake Android transporta el token de emparejamiento como hash SHA-256. El anuncio `CIM_ID` debe ser capturado durante un ensayo físico; una coincidencia estática de código no sustituye esta captura.

## Firmware canónico

Sólo utilizar `esp32/firmware/`:

- `esp32_plc_master.ino`
- `esp32_scorbot_manufactura.ino`
- `esp32_scorbot_calidad.ino`
- `esp32_scorbot_almacen.ino`
- `cim_ble_firmware.h`

Los snapshots en `archive/` no son fuente de flasheo. Antes de llevar firmware al banco, ejecute `python3 tools/validate_firmware_contract.py --quiet` y revise el protocolo de laboratorio.

## Construcción, entrega e instalación

Siga el [instructivo de uso](../INSTRUCTIVO_USO_PROYECTO.md). Las APK debug se exportan a `config/output-apks/`, y `SHA256SUMS.txt` permite comprobar su integridad. No se versionan APKs ni claves de firma.

## Secuencia operativa de alto nivel

1. Validar repositorio, contrato de firmware y documentación.
2. Compilar/probar el software con JDK 17 y Android SDK.
3. Iniciar Coordinador y el hub en una red controlada.
4. Solicitar admisión de cada estación y revisar identidad/capacidades.
5. Ejecutar simulación o telemetría sin carga antes de conectar actuadores.
6. Para banco físico, avanzar por los casos HW-01 a HW-09 con E-stop e interlocks.
7. Registrar cada resultado y cualquier incidencia en la bitácora.

## Seguridad y alcance de validación

La validación automática cubre estructura, contrato, sintaxis y las tareas de CI configuradas. No comprueba por sí sola UI en todos los dispositivos, rendimiento de LAN/BLE, cámara real, tensión eléctrica, E-stop, relé, robot, cinta ni láser. Consulte [validación y cobertura](../VALIDACION_Y_COBERTURA.md), el [manual de laboratorio](../deliverables/MANUAL_OPERATIVO_LABORATORIO.md) y el [protocolo hardware](../deliverables/PROTOCOLO_PRUEBAS_HARDWARE.md).
