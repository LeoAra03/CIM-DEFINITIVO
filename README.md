# Sistema CIM v6.0 — Control Industrial Distribuido

**Computer Integrated Manufacturing** — monorepo Android (5 apps) + `core-network` + firmware ESP32.

| | |
|---|---|
| **Autor** | Leonardo Araya Labarca — IECI, Universidad del Biobío |
| **Versión** | 6.0.0 |
| **Stack** | Kotlin 2.0 · Jetpack Compose · OpenCV · PlatformIO · ESP32 |

---

## Documentos de entrega

| Documento | Descripción |
|-----------|-------------|
| [docs/ENTREGA_FINAL_LEONARDO_ARAYA.pdf](docs/ENTREGA_FINAL_LEONARDO_ARAYA.pdf) | Informe completo (entrega académica) |
| [docs/ENTREGA_FINAL_LEONARDO_ARAYA.md](docs/ENTREGA_FINAL_LEONARDO_ARAYA.md) | Fuente Markdown del informe |
| [docs/GUIA_LABORATORIO_MANANA.md](docs/GUIA_LABORATORIO_MANANA.md) | Checklist para sesión en planta |
| [docs/INFORME_FUNCIONALIDAD.md](docs/INFORME_FUNCIONALIDAD.md) | Auditoría funcional (~83 %) y riesgos |
| [CHANGELOG_FIXES.md](CHANGELOG_FIXES.md) | Registro de correcciones v6.0 |

---

## Estructura del repositorio

```
Practica_2/
├── app-coordinador/     # Hub maestro (TCP :8888, autorización BT)
├── app-plc/             # Cinta transportadora / PLC
├── app-manufactura/     # Robot Scorbot + láser CNC
├── app-calidad/         # Visión ArUco/QR (OpenCV)
├── app-almacen/         # Rack 18 posiciones
├── core-network/        # Librería compartida (TCP, BT, protocolo CIM)
├── firmware/Firmware_Support/   # Firmware ESP32 (PlatformIO)
├── simulacion_esp32/    # Demo Wokwi (telemetría)
├── scripts/             # Flash ESP32, ADB, automatización
├── docs/                # Manuales, PDF, imágenes, guías
├── binarios_particionados/  # APKs particionadas (>100 MB GitHub)
└── entorno_mobile/      # Deploy multi-emulador (ADB)
```

> **Nota:** Las APKs compiladas (`output-apks/`) no están en el repo por tamaño. Compílalas localmente o reconstruye desde `binarios_particionados/`.

---

## Requisitos

- **JDK 17**
- **Android SDK** API 35 (`compileSdk 35`)
- **Gradle** (wrapper incluido: `gradlew`)
- **PlatformIO CLI** (`pip install platformio`) — firmware ESP32
- **ADB** — instalar APKs en dispositivos

Extensiones recomendadas para Cursor/VS Code: ver [.vscode/extensions.json](.vscode/extensions.json).

---

## Compilar e instalar

```powershell
# Tests + 5 APKs → output-apks/
.\gradlew testAllModules buildAllApks

# Firmware ESP32
cd firmware\Firmware_Support
pio run

# Instalar apps en dispositivos (ADB)
.\docs\logs\Install-CIM.ps1

# Flashear ESP32
.\scripts\hardware-testing\flash_and_monitor_esp32.ps1
```

Regenerar PDF del informe:

```powershell
cd docs
npm install
npm run pdf
```

---

## Despliegue en laboratorio (5 pasos)

1. Flashear cada ESP32 con `cim_esp32_firmware_v6.bin` (mismo firmware en todos los nodos).
2. Instalar las 5 APKs en dispositivos Android (una app por estación + hub).
3. Abrir **app-coordinador** → **START** hub → anotar IP.
4. En cada estación → pestaña **SINCRO** → vincular IP del coordinador.
5. FAB Bluetooth → emparejar ESP32 → autorizar MAC en el hub.

Detalle completo: [docs/GUIA_LABORATORIO_MANANA.md](docs/GUIA_LABORATORIO_MANANA.md).

---

## Manuales técnicos

| # | Tema |
|---|------|
| [01](docs/manuals/01_ARQUITECTURA_SISTEMA.md) | Arquitectura hub-and-spoke |
| [02](docs/manuals/02_PROTOCOLO_COMUNICACION_CIM.md) | Protocolo CIM |
| [03](docs/manuals/03_MOTOR_BLUETOOTH_HIBRIDO.md) | Bluetooth BLE + SPP |
| [04](docs/manuals/04_SISTEMA_VISION_ARTIFICIAL.md) | ArUco / QR / OpenCV |
| [05](docs/manuals/05_GUIA_ESTACIONES_TRABAJO.md) | UI por estación |
| [06](docs/manuals/06_DESPLIEGUE_Y_CONFIGURACION.md) | Red, permisos, troubleshooting |

---

## APKs grandes en GitHub

Las APKs con OpenCV superan el límite de 100 MB. Si incluyes `binarios_particionados/`:

```powershell
Get-Content ./binarios_particionados/CIM_V6_PART_* -Raw | Set-Content CIM_V6_ENTREGA_FINAL.zip
```

---

## Estado del proyecto

- **Build Android:** OK (`buildAllApks`, `testAllModules`)
- **Firmware:** OK (`pio run` en `firmware/Firmware_Support`)
- **Tests documentados:** 30/30 PASS (core-network, coordinador, plc)
- **Validación en planta física:** pendiente de sesión con ESP32 y actuadores reales

---

*Universidad del Biobío — Ingeniería de Ejecución en Computación e Informática — 2026*
