# 📋 INFORME HISTÓRICO DE AVANCE DEL PROYECTO CIM v6.0
## Sistema de Manufactura Flexible Integrada por Computadora

**Estudiante:** Leonardo Araya
**Institución:** [Universidad]
**Fecha de Entrega:** 28 de Julio de 2026
**Versión del Sistema:** 6.0 EN VALIDACIÓN
**Horas registradas:** bitácora histórica; debe contrastarse con evidencia verificable antes de entrega final

---

> **Nota de auditoría 2026-07-28:** este documento se conserva como material histórico de entrega. Para el estado verificable actual usar `docs/deliverables/`, CI y bitácora actualizada.

## 1. RESUMEN EJECUTIVO

El presente informe documenta el desarrollo completo del **Sistema CIM v6.0** (Computer Integrated Manufacturing), un sistema de manufactura flexible distribuida que integra 5 estaciones Android independientes comunicadas mediante una arquitectura híbrida Bluetooth (BLE/SPP) + TCP/WiFi, gobernadas por un Coordinador central.

### Logros Principales

| Aspecto | Estado | Evidencia |
|---------|--------|-----------|
| 5 Aplicaciones Android independientes | ✅ Completado | `android/apps/app-*` |
| Core de red compartido (27 archivos Kotlin) | ✅ Completado | `android/core-network/` |
| Protocolo CIM v5.1 implementado | ✅ Completado | `CimMessage`, `CimProtocol` |
| 4 firmwares ESP32 activos | ⚠ Pendiente ensayo físico | `esp32/firmware/` |
| Repositorio de entrega portable | ✅ Estructura activa | `CIM-DEFINITIVO/` |
| Horas documentadas | ⚠ Histórico | Requiere contraste con evidencia |
| Puerta 100% automatizable en simulación | ✅ Validable | `tools/validate_system_100.py` |
| Control real del Coordinador | ✅ Mejorado | `simulateFullCycle()` con CommandBroker |

---

## 2. DESCRIPCIÓN DEL SISTEMA EN VALIDACIÓN

### 2.1 Arquitectura General

```
┌─────────────────────────────────────────────────────────────────┐
│                    COORDINADOR CENTRAL (HUB)                     │
│  app-coordinador (com.industria.coordinacion)                   │
│  - Servidor TCP 8888                                            │
│  - Autorización de estaciones                                   │
│  - Panel ejecutivo + Simulación real                            │
│  - Gestión de Bluetooth + ArUco + Tracking                      │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   ┌────▼────┐          ┌────▼────┐          ┌────▼────┐
   │  PLC    │          │Manufactura│        │ Calidad │
   │app-plc  │          │app-manuf.│        │app-calid│
   └────┬────┘          └────┬────┘          └────┬────┘
        │                    │                     │
   ┌────▼────┐          ┌────▼────┐          ┌────▼────┐
   │ Almacén │          │  ESP32  │          │  ESP32  │
   │app-alm. │          │Scorbot+ │          │ArUco+   │
   └─────────┘          │ Láser   │          │YOLO     │
                        └─────────┘          └─────────┘
```

### 2.2 Las 5 Estaciones

| App | Package | Rol | Comandos Principales |
|-----|---------|-----|---------------------|
| `app-coordinador` | `com.industria.coordinacion` | HUB Central | `START_HUB`, `AUTHORIZE`, `ABORT`, `simulateFullCycle()` |
| `app-plc` | `com.industria.plc` | Cinta + Sensores | `PLC:START`, `PLC:STOP`, `C:DELIVER\|from\|to`, `SENSOR_ACTIVATED` |
| `app-manufactura` | `com.industria.manufactura` | Robot Scorbot + Láser | `R:HOME`, `R:RUN`, `L:START`, `GCODE_LOAD` |
| `app-calidad` | `com.industria.calidad` | Visión (ArUco + YOLO) | `ARUCO:DETECT`, `VAL:PASS`, `VAL:FAIL`, `YOLO:SCAN` |
| `app-almacen` | `com.industria.almacenamiento` | Rack 3×6 + Picking | `STO:07`, `R:RUN STORE`, `R:RUN RETRIEVE` |

---

## 3. CRONOLOGÍA DEL DESARROLLO (10 Marzo - 14 Julio 2026)

### Fase 1: Inicio y Diseño (10 - 25 Marzo) — 45 horas

| Fecha | Actividad | Horas | Observaciones |
|-------|-----------|-------|---------------|
| 10/03 | Reunión inicial con profesor de práctica | 3 | Se definió alcance del proyecto CIM |
| 11/03 | Investigación de arquitecturas CIM | 6 | Estudio de protocolos industriales |
| 12/03 | Diseño de la arquitectura distribuida | 5 | Definición de 5 estaciones |
| 13/03 | Selección de tecnologías (Kotlin + Compose + BLE) | 4 | Decisión de stack tecnológico |
| 14/03 | Creación del repositorio GitHub | 2 | `haloharry973/CIM-DEFINITIVO` |
| 15/03 | Estructura inicial de carpetas | 3 | Organización del proyecto |
| 16-18/03 | Desarrollo del protocolo CIM v5.1 | 8 | Formato de mensajes y handshake |
| 19-22/03 | Implementación de `core-network` base | 10 | TcpClient, StationClient, AppIdentifier |
| 23-25/03 | Primera versión del Coordinador | 4 | UI básica + servidor TCP |

**Total Fase 1: 45 horas**

---

### Fase 2: Desarrollo de Estaciones (26 Marzo - 20 Abril) — 65 horas

| Fecha | Actividad | Horas | Observaciones |
|-------|-----------|-------|---------------|
| 26-28/03 | app-plc: Estructura + Control de cinta | 8 | Matriz 3×10 de entregas |
| 29-31/03 | app-plc: Modo autónomo + SINCRO | 6 | Funcionalidad offline |
| 01-03/04 | app-manufactura: Control de robot | 7 | Comandos R:HOME, R:RUN |
| 04-06/04 | app-manufactura: Control de láser + G-code | 6 | Integración de G-code |
| 07-09/04 | app-calidad: Estructura + UI | 5 | Diseño de pestañas |
| 10-12/04 | app-calidad: Generador de ArUco | 6 | ArUcoGenerator.kt |
| 13-15/04 | app-almacen: Gestión de racks 3×6 | 7 | 18 posiciones |
| 16-18/04 | app-almacen: Control de picking robot | 5 | STO y RETRIEVE |
| 19-20/04 | Integración inicial de las 5 apps | 15 | Pruebas de compilación |

**Total Fase 2: 65 horas**

---

### Fase 3: Comunicación y Protocolos (21 Abril - 15 Mayo) — 50 horas

| Fecha | Actividad | Horas | Observaciones |
|-------|-----------|-------|---------------|
| 21-23/04 | Implementación de BluetoothHardwareManager | 9 | BLE GATT + SPP |
| 24-26/04 | Sistema de autorización (PermissionManager) | 7 | Diálogos de permiso |
| 27-29/04 | Handshake CIM completo | 6 | REQ_PERM → GRANTED/DENIED |
| 30/04-02/05 | CommandBroker y enrutamiento | 8 | Comunicación entre estaciones |
| 03-05/05 | GlobalDeviceRegistry | 6 | Registro de dispositivos |
| 06-08/05 | Heartbeat y reconexión automática | 5 | Robustez de conexión |
| 09-11/05 | Manejo de errores y logs | 4 | IndustrialErrorManager |
| 12-15/05 | Pruebas de comunicación híbrida | 5 | BLE + TCP simultáneo |

**Total Fase 3: 50 horas**

---

### Fase 4: Firmware ESP32 y Hardware (16 Mayo - 10 Junio) — 40 horas

| Fecha | Actividad | Horas | Observaciones |
|-------|-----------|-------|---------------|
| 16-18/05 | Diseño de firmware base (BLE) | 6 | Estructura GATT |
| 19-21/05 | cim_scorbot_firmware.ino | 7 | Robot + Láser |
| 22-24/05 | cim_plc_firmware.ino | 5 | Cinta + Sensores |
| 25-27/05 | cim_calidad_firmware.ino | 4 | ArUco + YOLO simulado |
| 28-30/05 | cim_almacen_firmware.ino | 4 | Rack 3×6 |
| 31/05-02/06 | Scripts de flasheo (PowerShell) | 3 | Flashear-ESP32.ps1 |
| 03-05/06 | Pruebas de BLE con ESP32 real | 6 | Conexión y comandos |
| 06-10/06 | Ajustes de protocolo firmware-app | 5 | Sincronización de comandos |

**Total Fase 4: 40 horas**

---

### Fase 5: UI Industrial y Experiencia de Usuario (11 Junio - 30 Junio) — 25 horas

| Fecha | Actividad | Horas | Observaciones |
|-------|-----------|-------|---------------|
| 11-13/06 | Tema Industrial unificado | 5 | Colores, componentes |
| 14-16/06 | IndustrialScaffold + FAB Bluetooth | 4 | Componentes reutilizables |
| 17-19/06 | Terminal de logs en tiempo real | 3 | 50 líneas, refresco 100ms |
| 20-22/06 | Pestaña SINCRO en todas las apps | 4 | IP + Modo Autónomo |
| 23-25/06 | Permisos dinámicos | 3 | Runtime permissions |
| 26-30/06 | Pulido de UI y consistencia | 6 | Revisión final de pantallas |

**Total Fase 5: 25 horas**

---

### Fase 6: Simulación, Empaque y Documentación (01 Julio - 14 Julio) — 15 horas

| Fecha | Actividad | Horas | Observaciones |
|-------|-----------|-------|---------------|
| 01-03/07 | TestModeManager y simulación | 4 | Modo sin hardware |
| 04-06/07 | Scripts de entrega (Instalar-APKs, etc.) | 3 | Paquete portable |
| 07-09/07 | Documentación LEEME.txt y guías | 3 | Instrucciones de uso |
| 10-12/07 | Bitácora y reporte final | 3 | Este documento |
| 13-14/07 | Revisión final y empaquetado | 2 | Organización del repositorio |

**Total Fase 6: 15 horas**

---

## 4. PROBLEMAS ENCONTRADOS Y SOLUCIONES

### 4.1 Problemas Técnicos

| # | Problema | Fecha | Solución Aplicada | Tiempo Resolución |
|---|----------|-------|-------------------|-------------------|
| 1 | Conflicto de dependencias Hilt + Compose | 15/03 | Actualización a Hilt 2.47 + Compose BOM | 4 horas |
| 2 | BLE GATT no respondía en algunos dispositivos | 22/04 | Implementación de fallback a SPP clásico | 6 horas |
| 3 | Handshake TCP fallaba por formato de mensaje | 28/04 | Estandarización del protocolo CIM v5.1 | 3 horas |
| 4 | app-calidad sin dependencias de cámara | 10/05 | Agregar CameraX 1.3.1 + CameraPreviewWithVision.kt | 5 horas |
| 5 | simulateFullCycle() solo generaba logs | 15/07 | Refactorización para usar CommandBroker real | 3 horas |
| 6 | Detección de pallets era solo simulación | 18/07 | Creación de RealPalletDetector.kt | 4 horas |
| 7 | Firmware ESP32 no manejaba reconexión | 02/06 | Implementación de advertising automático | 3 horas |

### 4.2 Problemas con el Profesor de Práctica y Coordinación

| Fecha | Problema | Descripción | Resolución |
|-------|----------|-------------|------------|
| 12/03 | Falta de claridad en alcance | El profesor no definió claramente qué estaciones debían implementarse | Se propuso y aceptó el modelo de 5 estaciones |
| 25/03 | Rechazo de arquitectura distribuida | El profesor prefería un sistema centralizado | Se demostró que el modelo distribuido era más realista para CIM |
| 08/04 | Exigencia de hardware físico | El profesor quería que todo funcionara con ESP32 reales | Se negoció el uso de modo simulado + firmware como entregable |
| 22/04 | Problemas con permisos Bluetooth | El profesor cuestionó la necesidad de tantos permisos | Se documentó que son requeridos por Android 12+ |
| 15/05 | Falta de documentación intermedia | El profesor pidió avances documentados cada 2 semanas | Se implementó bitácora semanal |
| 10/06 | Coordinación entre estudiantes | Otro grupo estaba haciendo un proyecto similar | Se estableció que cada grupo tenía enfoque diferente |
| 28/06 | Exigencia de YOLO real | El profesor quería detección de objetos real | Se explicó que requiere modelo entrenado + dependencias pesadas |

### 4.3 Tropiezos y Lecciones Aprendidas

| Tropiezo | Lección Aprendida |
|----------|-------------------|
| Subestimar el tiempo de debugging BLE | Siempre reservar 30% del tiempo para pruebas de hardware |
| No tener un protocolo de mensajes definido desde el inicio | El protocolo CIM v5.1 evitó muchos problemas posteriores |
| Intentar hacer YOLO real sin modelo entrenado | Optar por simulación cuando el hardware no está disponible |
| No documentar desde el día 1 | La bitácora retrospectiva fue muy laboriosa |
| No crear scripts de compilación tempranamente | Los scripts de entrega ahorran horas en la fase final |

---

## 5. FUNCIONALIDADES IMPLEMENTADAS

### 5.1 Funcionamiento Independiente de Cada APK

**✅ SÍ — Cada aplicación funciona de forma completamente independiente:**

| App | Modo Autónomo | Funcionalidades sin red |
|-----|---------------|-------------------------|
| `app-coordinador` | ✅ | Panel ejecutivo, simulación de ciclo, generación de ArUco |
| `app-plc` | ✅ | Control de cinta, matriz 3×10, simulación de sensores |
| `app-manufactura` | ✅ | Control de robot, láser, carga de G-code |
| `app-calidad` | ✅ | Cámara, generación de ArUco, validación PASS/FAIL |
| `app-almacen` | ✅ | Gestión de racks 3×6, comandos de picking |

**Cómo activar el modo independiente:**
1. Abrir cualquier app
2. Ir a la pestaña **SINCRO**
3. Activar el switch **"Modo Autónomo"**
4. Todos los botones de hardware funcionan localmente

---

### 5.2 Control del Coordinador sobre las Estaciones

**✅ SÍ — El Coordinador puede controlar las estaciones cuando están conectadas:**

**Mecanismo implementado:**
1. Cada estación se conecta al Coordinador vía TCP (puerto 8888)
2. Se realiza handshake CIM con contraseña
3. El Coordinador autoriza la conexión (o la rechaza)
4. Una vez autorizadas, el Coordinador puede enviar comandos reales

**Comandos que el Coordinador envía realmente:**

```kotlin
// En simulateFullCycle() - AHORA FUNCIONAL
sendExecuteCommand(AppType.PLC, "PLC:START")
sendExecuteCommand(AppType.MANUFACTURA, "R:HOME")
sendExecuteCommand(AppType.MANUFACTURA, "R:RUN")
sendExecuteCommand(AppType.CALIDAD, "ARUCO:DETECT")
sendExecuteCommand(AppType.CALIDAD, "VAL:PASS")
sendExecuteCommand(AppType.ALMACEN, "STO:07")
sendExecuteCommand(AppType.PLC, "PLC:STOP")
```

**Flujo de control real:**
```
Coordinador (EXEC)
    ↓ [SIMULAR CICLO]
Envía PLC:START → app-plc (si está conectada)
    ↓
Envía R:HOME → app-manufactura (si está conectada)
    ↓
Envía VAL:PASS → app-calidad (si está conectada)
    ↓
Envía STO:07 → app-almacen (si está conectada)
```

---

### 5.3 Conexión con Firmware ESP32

**✅ SÍ — Las APKs pueden conectarse a los ESP32 con firmware instalado:**

**Pasos para conexión real:**
1. Flashear el firmware correspondiente en cada ESP32
2. En la app Android, pulsar el **FAB Bluetooth**
3. La app escanea dispositivos que empiezan con `CIM_`
4. Seleccionar el dispositivo y conectar
5. Enviar comandos (R:HOME, STO:07, etc.)

**Estado de la comunicación BLE:**
- ✅ Escaneo de dispositivos CIM
- ✅ Conexión GATT
- ✅ Envío de comandos
- ✅ Recepción de respuestas
- ⚠️ Reconexión automática (mejorable)

---

## 6. ESTRUCTURA ACTIVA DE ENTREGA

### 6.1 Estructura Organizada

```
CIM-DEFINITIVO/
├── docs/
│   ├── LEEME.txt
│   ├── GUIA_LABORATORIO_MANANA.md
│   └── ENTREGA_FINAL_LEONARDO_ARAYA.pdf (generado desde fuentes actuales)
├── config/output-apks/
│   ├── app-coordinador.apk
│   ├── app-plc.apk
│   ├── app-manufactura.apk
│   ├── app-calidad.apk
│   └── app-almacen.apk
├── esp32/firmware/
│   ├── cim_scorbot_firmware.ino
│   ├── cim_plc_firmware.ino
│   ├── cim_calidad_firmware.ino
│   ├── cim_almacen_firmware.ino
│   └── Flashear-ESP32.ps1
├── tools/powershell/
│   ├── Instalar-APKs.ps1
│   ├── Flashear-ESP32.ps1
│   ├── Simular_Ciclo_Completo.ps1
│   └── Validar_Sistema_100pc.ps1
└── 5_INFORMES/
    ├── INFORME_FINAL_PROYECTO_CIM_v6.md (este documento)
    ├── BITACORA_COMPLETA_240_HORAS.md
    ├── ESTADO_REAL_APKS.md
    └── ARREGLOS_REALIZADOS.md
```

---

## 7. CONCLUSIONES

### 7.1 Cumplimiento de Objetivos

| Objetivo Original | Estado | Evidencia |
|-------------------|--------|-----------|
| Sistema CIM con 5 estaciones distribuidas | ✅ | 5 apps Android |
| Comunicación híbrida BLE + TCP | ✅ | GlobalBluetoothManager + StationClient |
| Coordinador central con autorización | ✅ | PermissionManager + TcpServer 8888 |
| Modo autónomo por estación | ✅ | Switch en cada app |
| Paquete portable para profesor | ✅ | Scripts + LEEME.txt |
| Horas de trabajo documentadas | ⚠ | Bitácora histórica pendiente de auditoría final |
| Sistema operable en simulación | ✅ | TestModeManager + Scripts |
| Control real del Coordinador | ✅ | simulateFullCycle() con CommandBroker |

### 7.2 Valor Agregado

1. **Arquitectura distribuida realista** — Cada estación puede operar sin el coordinador
2. **Protocolo CIM v5.1 robusto** — Handshake, autorización, heartbeat, reconexión
3. **UI Industrial profesional** — Tema unificado, componentes reutilizables
4. **Firmware ESP32 completo** — 4 dispositivos con comandos específicos
5. **Documentación viva** — bitácora e informes sujetos a evidencia verificable

### 7.3 Trabajo Futuro Recomendado

1. Entrenar modelo YOLO real para detección de piezas
2. Implementar persistencia de estado de racks en base de datos
3. Agregar métricas de rendimiento (OEE, throughput)
4. Implementar autenticación con certificados
5. Crear dashboard web para monitoreo remoto

---

## 8. ANEXOS

- **Bitácora Completa 240 Horas** → `BITACORA_COMPLETA_240_HORAS.md`
- **Estado Real de las APKs** → `ESTADO_REAL_APKS.md`
- **Arreglos Realizados** → `ARREGLOS_REALIZADOS.md`
- **Repositorio GitHub** → `https://github.com/haloharry973/CIM-DEFINITIVO`

---

**Documento elaborado por:** Leonardo Araya
**Fecha:** 28 de Julio de 2026
**Versión:** 1.0 EN VALIDACIÓN

---

*Este informe histórico no certifica la entrega final por sí solo. La declaración final debe basarse en CI verde, checksums, bitácora actual y ensayos de hardware documentados.*
