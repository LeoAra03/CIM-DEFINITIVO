# Guía de Entrega Final — CIM v6.0

> **Documento consolidado:** El contenido canónico de entrega está en [`ENTREGA_FINAL_LEONARDO_ARAYA.md`](ENTREGA_FINAL_LEONARDO_ARAYA.md). La guía de laboratorio presencial está en [`GUIA_LABORATORIO_MANANA.md`](GUIA_LABORATORIO_MANANA.md). Este archivo se conserva como referencia temática.

> **Practica_2** | Computer Integrated Manufacturing | Versión **6.0.0**  
> Fecha de auditoría: 31 de mayo de 2026

---

## 1. Inicio rápido (entrega hoy)

| Paso | Acción | Comando / Ruta |
|------|--------|----------------|
| 1 | Flashear ESP32 con firmware CIM | `.\scripts\hardware-testing\flash_and_monitor_esp32.ps1` |
| 2 | Instalar las 5 APKs en dispositivos Android | `.\docs\logs\Install-CIM.ps1` o `adb install output-apks\*.apk` |
| 3 | Abrir **app-coordinador** → iniciar **Servidor Hub** (TCP :8888) | Primera app en arrancar |
| 4 | En cada estación → pestaña **SINCRO** → **Vincular al Hub** | IP local del coordinador |
| 5 | Conceder permisos BT, ubicación y cámara | Obligatorio en Android 12+ |
| 6 | Emparejar ESP32 desde FAB Bluetooth | Filtra dispositivos CIM/ESP32/NODO |

**Compilar desde cero:**

```powershell
.\gradlew testAllModules buildAllApks
cd firmware\Firmware_Support; pio run
```

---

## 2. Tabla maestra de entregables

| Archivo / Carpeta | Propósito | Cuándo usar |
|-------------------|-----------|-------------|
| `output-apks/app-coordinador.apk` | Hub maestro: TCP, autorización, multiconexión BT | 1 dispositivo Android (tablet/celular central) |
| `output-apks/app-plc.apk` | Estación cinta transportadora / PLC | Dispositivo en estación de transporte |
| `output-apks/app-manufactura.apk` | Robot Scorbot + láser CNC | Dispositivo en celda de manufactura |
| `output-apks/app-calidad.apk` | Inspección visual ArUco/QR (OpenCV) | Dispositivo con cámara en QC |
| `output-apks/app-almacen.apk` | Gestión de posiciones de almacén | Dispositivo en estación de almacén |
| `output-apks/cim_esp32_firmware_v6.bin` | Binario listo para flash (copia del build PIO) | Flashear nodos ESP32 sin compilar |
| `firmware/Firmware_Support/` | Proyecto PlatformIO firmware híbrido BLE+SPP | Modificar o recompilar firmware |
| `simulacion_esp32/` | Simulación Wokwi (DHT22 + LCD + HTTP) | Demo sin hardware físico |
| `core-network/` | Librería compartida Android (TCP, BT, visión) | Desarrollo / no instalar directamente |
| `scripts/hardware-testing/` | Flash, monitor serial, simulación ESP32 | Laboratorio y pruebas E2E |
| `scripts/automation/` | Secuencias automatizadas de prueba | CI local / demos |
| `scripts/python/` | Utilidades ArUco, servidor, simuladores | Prototipado y scripts auxiliares |
| `docs/manuals/` | Manuales técnicos numerados 01–06 | Referencia de arquitectura y protocolo |
| `docs/GUIA_PROFESIONAL_CIM.md` | Guía integral con diagramas | Onboarding del equipo |
| `docs/INFORME_FUNCIONALIDAD.md` | Matriz de % funcional y riesgos | Evaluación y roadmap |
| `docs/ESP32_SIMULACION_Y_HARDWARE.md` | Wokwi vs hardware real | Elegir modo de prueba |
| `docs/logs/Install-CIM.ps1` | Instalación masiva vía ADB | Despliegue rápido en múltiples devices |
| `binarios_particionados/` | APKs particionados para GitHub (>100 MB) | Descarga sin compilar |
| `release.keystore` | Keystore para firma release (opcional) | Builds de producción |

---

## 3. Archivos ESP32 — rutas exactas

| Elemento | Ruta absoluta (relativa al repo) |
|----------|----------------------------------|
| Código fuente principal | `firmware/Firmware_Support/src/main.ino` |
| Fuente alternativa | `firmware/Firmware_Support/src/main/cim_esp32_firmware_v6.ino` |
| Configuración PlatformIO | `firmware/Firmware_Support/platformio.ini` |
| Tabla de particiones | `firmware/Firmware_Support/huge_app.csv` |
| Binario compilado (PIO) | `firmware/Firmware_Support/.pio/build/esp32dev/firmware.bin` |
| Binario de entrega | `output-apks/cim_esp32_firmware_v6.bin` |
| Script de build | `firmware/Firmware_Support/build_firmware.ps1` |
| Script flash + monitor | `scripts/hardware-testing/flash_and_monitor_esp32.ps1` |
| Simulación Wokwi | `simulacion_esp32/src/main.cpp` + `simulacion_esp32/diagram.json` |

### Comandos de flash

```powershell
# Opción A — script automatizado
.\scripts\hardware-testing\flash_and_monitor_esp32.ps1

# Opción B — PlatformIO manual
cd firmware\Firmware_Support
pio run -t upload
pio device monitor -b 115200

# Opción C — esptool directo (ajustar COMx)
esptool.py --chip esp32 --port COM3 write_flash 0x10000 output-apks\cim_esp32_firmware_v6.bin
```

---

## 4. APKs — estación, orden e instalación

| Orden | APK | Estación | Package ID |
|-------|-----|----------|------------|
| **1** | `app-coordinador.apk` | Hub / Coordinador | `com.industria.coordinacion` |
| 2 | `app-plc.apk` | Cinta / PLC | `com.industria.plc` |
| 3 | `app-manufactura.apk` | Robot + Láser | `com.industria.manufactura` |
| 4 | `app-calidad.apk` | Control de calidad | `com.industria.calidad` |
| 5 | `app-almacen.apk` | Almacén | `com.industria.almacenamiento` |

> **Nota:** Las APKs debug (~165–177 MB) incluyen OpenCV nativo. Las variantes `*-release.apk` en `output-apks/` son builds anteriores más pequeñas; usar las debug recientes para la entrega auditada.

### Instalación

```powershell
# Todas las apps (requiere ADB)
adb install -r output-apks\app-coordinador.apk
adb install -r output-apks\app-plc.apk
adb install -r output-apks\app-manufactura.apk
adb install -r output-apks\app-calidad.apk
adb install -r output-apks\app-almacen.apk

# O script automatizado
.\docs\logs\Install-CIM.ps1
```

---

## 5. Índice de manuales y documentación

| Documento | Descripción |
|-----------|-------------|
| [01_ARQUITECTURA_SISTEMA.md](manuals/01_ARQUITECTURA_SISTEMA.md) | Topología hub-and-spoke, capas, módulos |
| [02_PROTOCOLO_COMUNICACION_CIM.md](manuals/02_PROTOCOLO_COMUNICACION_CIM.md) | Formato CimMessage, comandos R:/L:/C:/STO: |
| [03_MOTOR_BLUETOOTH_HIBRIDO.md](manuals/03_MOTOR_BLUETOOTH_HIBRIDO.md) | BLE UART + SPP Classic, MTU, multiconexión |
| [04_SISTEMA_VISION_ARTIFICIAL.md](manuals/04_SISTEMA_VISION_ARTIFICIAL.md) | ArUco, QR, pipeline OpenCV |
| [05_GUIA_ESTACIONES_TRABAJO.md](manuals/05_GUIA_ESTACIONES_TRABAJO.md) | Operación por estación |
| [06_DESPLIEGUE_Y_CONFIGURACION.md](manuals/06_DESPLIEGUE_Y_CONFIGURACION.md) | Red, permisos, troubleshooting |
| [GUIA_PROFESIONAL_CIM.md](GUIA_PROFESIONAL_CIM.md) | Guía integral v6.0 con Mermaid |
| [ESP32_SIMULACION_Y_HARDWARE.md](ESP32_SIMULACION_Y_HARDWARE.md) | Wokwi vs ESP32 físico |
| [TEST_MATRIX.md](TEST_MATRIX.md) | Matriz de 30 tests documentados |
| [EXTENSIONS_AND_TOOLING.md](EXTENSIONS_AND_TOOLING.md) | Toolchain IDE y extensiones |
| [INFORME_FUNCIONALIDAD.md](INFORME_FUNCIONALIDAD.md) | Auditoría funcional y roadmap |
| [CHANGELOG_FIXES.md](../CHANGELOG_FIXES.md) | Registro de correcciones v6.0 |
| [INFORME_FINAL_PRACTICA_2_LEO_ARAYA.md](INFORME_FINAL_PRACTICA_2_LEO_ARAYA.md) | Informe académico del proyecto |
| [logs/VERIFICACION_SISTEMA_CIM.md](logs/VERIFICACION_SISTEMA_CIM.md) | Checklist de verificación |
| [logs/CERTIFICADO_SISTEMA_OPERACIONAL.md](logs/CERTIFICADO_SISTEMA_OPERACIONAL.md) | Certificación operacional |

---

## 6. Estructura del repositorio (post-reorganización)

```
Practica_2/
├── app-coordinador/     # Hub Android
├── app-plc/             # Estación PLC
├── app-manufactura/     # Estación robot/láser
├── app-calidad/         # Estación visión
├── app-almacen/         # Estación almacén
├── core-network/        # Librería compartida
├── firmware/
│   └── Firmware_Support/   # ESP32 PlatformIO (estilo Espressif)
├── simulacion_esp32/    # Wokwi / demo sensores
├── output-apks/         # APKs + firmware.bin de entrega
├── scripts/             # Automatización, Python, hardware-testing
├── docs/
│   ├── manuals/         # Manuales técnicos 01–06
│   ├── logs/            # Certificados, install, verificación
│   └── *.md             # Guías de entrega y auditoría
├── binarios_particionados/  # APKs para distribución GitHub
├── build.gradle.kts     # Tareas: buildAllApks, cleanBuildAll, testAllModules
└── settings.gradle.kts  # Módulos Gradle (no mover)
```

---

## 7. Tareas Gradle útiles

| Tarea | Descripción |
|-------|-------------|
| `./gradlew buildAllApks` | Compila y exporta 5 APKs a `output-apks/` |
| `./gradlew cleanBuildAll` | Limpia módulos + rebuild completo |
| `./gradlew testAllModules` | Tests unitarios (core-network, coordinador, plc) |
| `./gradlew buildRelease` | Test + build + validación + reporte |
| `./gradlew buildFirmware` | Invoca script PIO de firmware |

---

*Documento generado en auditoría final CIM v6.0 — Practica_2 UBB*
