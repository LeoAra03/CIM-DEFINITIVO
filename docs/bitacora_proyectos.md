---
title: "Bitácora de Proyectos — Método Científico CIM v6.0"
author: "Practica_2"
date: "2026-05-31"
version: "1.0"
css: styles/industrial_pdf.css
toc: true
---

# Bitácora de Proyectos — Practica_2 CIM v6.0

> **Documento consolidado:** Ver sección 14 en [`ENTREGA_FINAL_LEONARDO_ARAYA.md`](ENTREGA_FINAL_LEONARDO_ARAYA.md).

> Registro cronológico según método científico: observación → hipótesis → experimento → análisis → conclusión.

---

## Registro de actividades

| Fase / Fecha | Actividad Crítica | Recursos Técnicos | Hallazgos / Errores / Observaciones |
|--------------|-------------------|-------------------|-------------------------------------|
| **Fase 0 — 2026-05-28** | Auditoría inicial del monorepo Gradle (5 apps + `core-network`) | JDK 17, Gradle 9.3.1, Android SDK API 35 | Estructura hub-and-spoke confirmada; deuda en API Bluetooth incompleta respecto a UI Compose |
| **Fase 1 — 2026-05-29** | Diagnóstico de fallos de compilación post-refactor UI | `BluetoothComponents.kt`, `CommandBroker.kt`, logs Gradle | Referencias a métodos inexistentes en manager legacy; tests esperaban `broker.send(CimMessage)` síncrono |
| **Fase 1 — 2026-05-29** | Hipótesis: wrapper síncrono en CommandBroker restaura compatibilidad tests | `CommandBroker.kt`, suite unitaria existente | **Confirmada** — `send(CimMessage)` añadido; tests de protocolo PASS sin romper async |
| **Fase 2 — 2026-05-30** | Reescritura Bluetooth multiconexión v6.0 | `BluetoothHardwareManager.kt`, `DiscoveredBluetoothDevice.kt`, manual `03_MOTOR_BLUETOOTH_HIBRIDO.md` | Implementado `ConcurrentHashMap` GATT por MAC; `StateFlow connectionStates`; escaneo BLE+Classic; backoff reconexión 1s→30s |
| **Fase 2 — 2026-05-30** | Experimento fragmentación MTU BLE | ESP32 Nordic UART UUIDs, paquetes 20 B, delay 20 ms | Comandos largos (`R:`, `L:`, `STO:`) transmitidos sin truncamiento en simulación |
| **Fase 2 — 2026-05-30** | Integración SPP fallback | `BluetoothSppManager.kt`, `CommandBroker` routing | SPP activo cuando BLE no disponible; servidor SPP en coordinador al conceder permisos BT |
| **Fase 3 — 2026-05-30** | Firmware ESP32 híbrido BLE+SPP+ESP-NOW | `firmware/Firmware_Support`, `platformio.ini`, `huge_app.csv` | `pio run` SUCCESS; IDENTIFY en boot; monitor 115200 operativo |
| **Fase 3 — 2026-05-30** | Verificación AuthorizationManager O(1) | `AuthorizationManagerTest`, `CimStressAndAcceptanceTest` | Estados PENDING/AUTHORIZED/BLOCKED coherentes; `canSendCommand` bloquea comandos no autorizados |
| **Fase 4 — 2026-05-31** | Ejecución matriz 30 tests | `./gradlew testAllModules` | **30/30 PASS** documentados en `docs/TEST_MATRIX.md` (core-network, app-coordinador, app-plc) |
| **Fase 4 — 2026-05-31** | Build APKs industriales | `./gradlew buildAllApks` | APKs en `output-apks/`: coordinador, plc, calidad, manufactura, almacen |
| **Fase 4 — 2026-05-31** | Tests de estrés destructivos PLC | `IndustrialStressTests.kt` | PASS: spam comandos, BT apagado mid-transmission, bypass password |
| **Fase 4 — 2026-05-31** | Tests aceptación tesis — Gatekeeper BT | `CoordinatorThesisTests.kt` | PASS: requisito gatekeeper Bluetooth inicial cumplido |
| **Fase 5 — 2026-05-31** | Documentación profesional entrega | MPE, Mermaid, `GUIA_PROFESIONAL_CIM.md`, `CHANGELOG_FIXES.md` | Diagrama arquitectura v6; guía despliegue; matriz tests publicada |
| **Fase 5 — 2026-05-31** | Scripts hardware Windows | `simulate_esp32.ps1`, `flash_and_monitor_esp32.ps1` | Simulación Wokwi/PIO/Python; upload ESP32 automatizado |
| **Fase 6 — 2026-05-31** | Automatización entorno Cursor CIM v6.0 | Cursor CLI extensiones, `.cursorrules`, `industrial_pdf.css` | 12/15 extensiones OK; fallos: paste-image original, ltex, platformio-ide, android-adb — ver `EXTENSIONS_AND_TOOLING.md` |
| **Fase 6 — 2026-05-31** | Simulación Wokwi ESP32+DHT22+LCD | `simulacion_esp32/diagram.json`, `main.cpp`, `wokwi.toml` | Proyecto Wokwi independiente; firmware bin enlazado a build PlatformIO |
| **Fase 6 — 2026-05-31** | Despliegue multitarea emuladores | `entorno_mobile/deploy_multitask.ps1`, `adb` | Instalación paralela APKs a emulator-5554/5556 detectados dinámicamente |
| **Fase 6 — 2026-05-31** | Manual industrial seguridad | `docs/manual_industrial_seguridad.md` | LaTeX Bcrypt/RS256, secuencias JWT, matriz troubleshooting, mapeo AuthorizationManager |

---

## Análisis consolidado (2026-05-31)

### Objetivos cumplidos

1. Build Android **BUILD SUCCESSFUL** (`testAllModules`, `buildAllApks`).
2. Firmware **pio run SUCCESS**.
3. Multiconexión Bluetooth documentada e implementada.
4. Zero handlers vacíos en botones UI de las 5 apps.
5. Documentación trazable con fechas mayo 2026.

### Limitaciones no resueltas (hardware físico)

| Limitación | Impacto | Mitigación aplicada |
|------------|---------|---------------------|
| Sin 2+ ESP32 físicos en banco | No medida latencia RF real | Simulación Wokwi + scripts PIO |
| Sin red industrial aislada | TCP hub solo en LAN dev | Documentado puerto 8888 y firewall |
| Emparejamiento BLE campo | Interferencia no modelada | Auto-reconexión backoff en manager |

### Próximos experimentos sugeridos

1. Medir $T(c)$ Bcrypt en dispositivo coordinador para elegir cost factor producción.
2. Prototipo middleware JWT RS256 delante de `TcpServer` sin romper protocolo CIM legacy.
3. Capturas Paste Image en bitácora de pruebas de campo.

---

## Referencias cruzadas

- `CHANGELOG_FIXES.md` — detalle técnico de fixes
- `docs/TEST_MATRIX.md` — IDs CIM-PROTO-01 … CIM-THESIS-01
- `docs/manual_industrial_seguridad.md` — marco de seguridad
- `docs/EXTENSIONS_AND_TOOLING.md` — toolchain mayo 2026

---

*Bitácora cerrada — ciclo entrega 2026-05-31*
