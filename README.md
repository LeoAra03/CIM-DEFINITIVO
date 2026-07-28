# 🏭 CIM v6.0 - Sistema de Manufactura Flexible

[![Estado](https://img.shields.io/badge/Estado-FINAL-green)](https://github.com/haloharry973/CIM-DEFINITIVO)
[![Horas](https://img.shields.io/badge/Horas-240-blue)](https://github.com/haloharry973/CIM-DEFINITIVO)
[![Universidad](https://img.shields.io/badge/Universidad-B%C3%ADo--B%C3%ADo-orange)](https://www.ubiobio.cl)

> **Sistema CIM (Computer Integrated Manufacturing) v6.0** - 5 estaciones distribuidas con comunicación híbrida BLE + TCP/WiFi, gobernadas por un Coordinador central. **100% operable en simulación** y con control real del Coordinador.

---

## 📦 Estructura del Repositorio (Ordenada)

```
CIM-DEFINITIVO/
├── 📁 01_CODIGO_FUENTE/          ← Código fuente de las 5 apps + core
│   └── android/
│       ├── apps/
│       │   ├── app-coordinador/
│       │   ├── app-plc/
│       │   ├── app-manufactura/
│       │   ├── app-calidad/
│       │   └── app-almacen/
│       └── core-network/         ← Librería compartida (27 archivos)
│
├── 📁 02_DOCUMENTACION/          ← Guías y manuales
│   ├── LEEME.txt                 ← Instrucciones principales
│   └── GUIA_LABORATORIO_MANANA.md
│
├── 📁 03_ENTREGA_FINAL/          ← Paquete listo para profesor
│   └── PAQUETE_FINAL_ENTREGA/
│       ├── 1_DOCUMENTACION/
│       ├── 2_APKS/               ← (Copiar APKs aquí)
│       ├── 3_FIRMWARE/           ← 4 firmwares ESP32
│       ├── 4_SCRIPTS/            ← Scripts de instalación
│       └── 5_INFORMES/
│           ├── INFORME_UBB_CIM_v6.html    ← INFORME UBB (PDF)
│           ├── BITACORA_240_HORAS.md
│           └── INFORME_FINAL.md
│
├── 📁 04_FIRMWARE/               ← Firmware ESP32
│   └── 3_FIRMWARE_ESP32/
│       ├── cim_scorbot_firmware.ino
│       ├── cim_plc_firmware.ino
│       ├── cim_calidad_firmware.ino
│       └── cim_almacen_firmware.ino
│
├── 📁 05_SCRIPTS/                ← Scripts de automatización
│   └── 4_SCRIPTS/
│       ├── Instalar-APKs.ps1
│       ├── Flashear-ESP32.ps1
│       ├── Simular_Ciclo_Completo.ps1
│       └── Validar_Sistema_100pc.ps1
│
├── 📁 06_INFORMES/               ← Reportes académicos
│   ├── INFORME_FINAL_PROYECTO_CIM_v6.md
│   ├── BITACORA_COMPLETA_240_HORAS.md
│   ├── ESTADO_REAL_APKS.md
│   └── ARREGLOS_REALIZADOS.md
│
├── android/                      ← Código fuente original
├── esp32/                        ← Firmware original
├── config/                       ← Gradle portable
├── docs/                         ← Documentación adicional
└── legacy/                       ← Archivos antiguos

---

## 🚀 Inicio Rápido

### 1. Clonar e instalar

```bash
git clone https://github.com/haloharry973/CIM-DEFINITIVO.git
cd CIM-DEFINITIVO
```

### 2. Ver el Informe UBB (PDF)

```bash
# Abre en navegador y guarda como PDF:
PAQUETE_FINAL_ENTREGA/5_INFORMES/INFORME_UBB_CIM_v6.html
```

### 3. Ejecutar simulación

```powershell
cd 4_SCRIPTS
.\Simular_Ciclo_Completo.ps1
```

### 4. Compilar APKs reales

```bash
cd config
./gradlew buildAllApks
```

---

## 📊 Estado del Proyecto

| Componente | Estado |
|------------|--------|
| 5 Apps Android | ✅ Completas |
| Core Network (27 archivos) | ✅ Implementado |
| 4 Firmwares ESP32 | ✅ Completos |
| Protocolo CIM v5.1 | ✅ Funcional |
| Control Real del Coordinador | ✅ Implementado |
| Cámara Real (CameraX) | ✅ Implementado |
| Detección Real de Pallets | ✅ Implementado |
| Paquete de Entrega | ✅ Listo |
| Informe UBB + Bitácora 240h | ✅ Completos |

---

## 📞 Contacto

- **Estudiante:** Leonardo Araya
- **Universidad:** Universidad del Bío-Bío
- **Repositorio:** https://github.com/haloharry973/CIM-DEFINITIVO
- **Branch de entrega:** `arena/019fa6a4-cim-definitivo`

---

**© 2026 - Universidad del Bío-Bío - Ingeniería Civil en Informática**
