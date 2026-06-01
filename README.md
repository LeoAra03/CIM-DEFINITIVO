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
| [docs/ENTREGA_FINAL_LEONARDO_ARAYA.pdf](docs/ENTREGA_FINAL_LEONARDO_ARAYA.pdf) | Informe completo de la entrega académica |
| [docs/ENTREGA_FINAL_LEONARDO_ARAYA.md](docs/ENTREGA_FINAL_LEONARDO_ARAYA.md) | Fuente Markdown del informe |
| [docs/GUIA_LABORATORIO_MANANA.md](docs/GUIA_LABORATORIO_MANANA.md) | Checklist de instalación y prueba en planta |
| [docs/INFORME_FUNCIONALIDAD.md](docs/INFORME_FUNCIONALIDAD.md) | Auditoría funcional y riesgos |
| [CHANGELOG_FIXES.md](CHANGELOG_FIXES.md) | Registro de correcciones y ajustes v6.0 |

---

## Organización del proyecto

```
Practica_2/
├── app-coordinador/     # Hub maestro (TCP, autorización Bluetooth)
├── app-plc/             # Cinta transportadora / PLC
├── app-manufactura/     # Robot Scorbot + láser CNC
├── app-calidad/         # Visión ArUco/QR (OpenCV)
├── app-almacen/         # Rack de almacenamiento y logística
├── core-network/        # Librería compartida (TCP, BT, protocolo CIM)
├── firmware/Firmware_Support/   # Firmware ESP32 (PlatformIO)
├── simulacion_esp32/    # Demo Wokwi de telemetría
├── scripts/             # Automatización, flashing y pruebas hardware
├── docs/                # Manuales, guías y entrega académica
├── binarios_particionados/  # APKs divididas para GitHub
└── output-apks/         # APKs finales + firmware para entrega
```

> `output-apks/` ahora contiene las 5 APKs de entrega y el binario de firmware canónico.
> Las variantes de release se conservan en `output-apks/release/` y los bins históricos en `output-apks/firmware-archive/`.

---

## Requisitos

- **JDK 17**
- **Android SDK** API 35 (`compileSdk 35`)
- **Gradle wrapper** incluido (`gradlew`)
- **PlatformIO CLI** (`pip install platformio`)
- **ADB** para instalar APKs en dispositivos Android

Extensiones recomendadas: ver `.vscode/extensions.json`.

---

## Compilación y entrega

```powershell
# Compila todos los módulos y exporta las APKs a output-apks/
.\gradlew testAllModules buildAllApks

# Compila firmware ESP32
cd firmware\Firmware_Support
pio run

# Empaqueta la entrega
cd ..\..
.\entrega\crear_paquete_zip.ps1
```

> El archivo `output-apks/cim_esp32_firmware_v6.bin` es el firmware CANÓNICO para flashear los nodos.

---

## Instalación en laboratorio

1. Flashear cada ESP32 con `output-apks/cim_esp32_firmware_v6.bin`.
2. Instalar las 5 APKs desde `output-apks/`.
3. Iniciar **app-coordinador** y copiar la IP del hub.
4. En cada estación, ir a la pestaña **SINCRO** y vincular la IP.
5. Autorizar desde Bluetooth/Hub para permitir la comunicación entre estaciones.

Detalle completo: [docs/GUIA_LABORATORIO_MANANA.md](docs/GUIA_LABORATORIO_MANANA.md).

---

## Documentación técnica

| # | Tema |
|---|------|
| [01](docs/manuals/01_ARQUITECTURA_SISTEMA.md) | Arquitectura del sistema |
| [02](docs/manuals/02_PROTOCOLO_COMUNICACION_CIM.md) | Protocolo CIM |
| [03](docs/manuals/03_MOTOR_BLUETOOTH_HIBRIDO.md) | Bluetooth BLE + SPP |
| [04](docs/manuals/04_SISTEMA_VISION_ARTIFICIAL.md) | Visión artificial y OpenCV |
| [05](docs/manuals/05_GUIA_ESTACIONES_TRABAJO.md) | UI y operación de estaciones |
| [06](docs/manuals/06_DESPLIEGUE_Y_CONFIGURACION.md) | Red, permisos y troubleshooting |

---

## Resumen de entrega

- **5 APKs de estación** listas en `output-apks/`
- **Binario de firmware** en `output-apks/cim_esp32_firmware_v6.bin`
- **Release antiguos** en `output-apks/release/`
- **Firmware histórico** en `output-apks/firmware-archive/`

---

## Estado actual

- **Android:** OK (`buildAllApks`, `testAllModules`)
- **Firmware:** OK (`pio run` en `firmware/Firmware_Support`)
- **Pruebas:** 30/30 documentadas y automatizadas en core-network y apps
- **Entrega:** lista para paquete final y validación en planta

---

*Universidad del Biobío — Ingeniería de Ejecución en Computación e Informática — 2026*
