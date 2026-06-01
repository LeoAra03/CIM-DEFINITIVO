# ARQUITECTURA REFACTORIZADA CIM v7.0 — ESTRUCTURA DE CARPETAS

## 1. MONOREPO REORGANIZADO

```
CIM-DEFINITIVO/
├── .github/
│   ├── workflows/           # CI/CD
│   └── CODEOWNERS
├── docs/
│   ├── ARCHITECTURE.md      # Diagrama centralizado
│   ├── COMMUNICATION.md     # Protocolos BLE/SPP/TCP
│   ├── FIRMWARE_GUIDE.md    # Guía FreeRTOS
│   ├── UI_PATTERNS.md       # MVVM/Clean Architecture
│   └── API_REFERENCE.md
│
├── core-network/            # Shared Bluetooth + Comunicación
│   ├── src/main/java/com/sistema/distribuido/network/
│   │   ├── BluetoothHardwareManager.kt
│   │   ├── AuthorizationManager.kt     # Centralizado: autorización por MAC
│   │   ├── PermissionManager.kt        # Lifecycle de permisos
│   │   ├── CommunicationCoordinator.kt # [NUEVO] Orquesta mensajes
│   │   └── protocol/
│   │       ├── CimMessage.kt
│   │       ├── CimProtocol.kt
│   │       ├── CommandType.kt
│   │       ├── AppType.kt
│   │       └── TransportFrame.kt       # [NUEVO] Encapsulación BLE/SPP/TCP
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── app-coordinador/         # MAESTRO: Gestiona autorización centralizada
│   ├── app/src/main/
│   │   ├── java/com/sistema/distribuido/coordinador/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HubScreen.kt                # Dashboard central
│   │   │   │   │   ├── DeviceListScreen.kt          # Malla BLE descubierta
│   │   │   │   │   ├── AuthorizationScreen.kt       # Solicitudes pendientes
│   │   │   │   │   └── DeviceControlScreen.kt       # Control de cada nodo
│   │   │   │   ├── components/
│   │   │   │   │   ├── DeviceStatusCard.kt
│   │   │   │   │   ├── AuthorizationDialog.kt       # [NUEVO] UI centralizada
│   │   │   │   │   └── RealTimeMetrics.kt
│   │   │   │   └── theme/
│   │   │   ├── viewmodels/
│   │   │   │   ├── HubViewModel.kt                  # Estado maestro
│   │   │   │   ├── DeviceViewModel.kt
│   │   │   │   └── AuthorizationViewModel.kt        # [NUEVO] Lógica central
│   │   │   ├── services/
│   │   │   │   ├── CoordinatorService.kt            # [NUEVO] Service para autorización
│   │   │   │   └── DeviceMonitorService.kt
│   │   │   └── utils/
│   │   └── res/
│   └── build.gradle.kts
│
├── app-scorbot/             # ESTACIÓN 1: Control de brazo robótico
│   ├── app/src/main/
│   │   ├── java/com/sistema/distribuido/scorbot/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/screens/
│   │   │   │   ├── ArmControlScreen.kt              # UI de control del brazo
│   │   │   │   ├── TrajectoryPlannerScreen.kt       # Trayectorias
│   │   │   │   ├── CalibrationScreen.kt             # Calibración
│   │   │   │   └── JointMonitorScreen.kt            # Monitoreo articulaciones
│   │   │   ├── services/
│   │   │   │   └── ScorbotCommandService.kt         # Integración con coordinador
│   │   │   ├── viewmodels/
│   │   │   │   └── ArmViewModel.kt
│   │   │   └── models/
│   │   │       ├── ArmState.kt
│   │   │       └── JointData.kt
│   │   └── res/
│   └── build.gradle.kts
│
├── app-vision/              # ESTACIÓN 2: Aruco + Calidad
│   ├── app/src/main/
│   │   ├── java/com/sistema/distribuido/vision/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/screens/
│   │   │   │   ├── CameraPreviewScreen.kt           # Preview en vivo
│   │   │   │   ├── ArucoDetectionScreen.kt          # Generación/detección Aruco
│   │   │   │   ├── QualityCheckScreen.kt            # Inspección de calidad
│   │   │   │   └── ReportScreen.kt                  # Reportes de defectos
│   │   │   ├── services/
│   │   │   │   ├── CameraService.kt
│   │   │   │   ├── ArucoProcessorService.kt         # [NUEVO] OpenCV/ArUco
│   │   │   │   └── QualityAnalysisService.kt        # [NUEVO] Visión de calidad
│   │   │   ├── viewmodels/
│   │   │   │   └── VisionViewModel.kt
│   │   │   └── models/
│   │   │       ├── ArucoMarker.kt
│   │   │       └── QualityDefect.kt
│   │   └── res/
│   └── build.gradle.kts
│
├── app-laser/               # ESTACIÓN 3: Grabado/corte láser
│   ├── app/src/main/
│   │   ├── java/com/sistema/distribuido/laser/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/screens/
│   │   │   │   ├── LaserControlScreen.kt             # Control de potencia/velocidad
│   │   │   │   ├── DesignEditorScreen.kt             # Editor de diseños
│   │   │   │   ├── PreviewScreen.kt                  # Preview antes de grabar
│   │   │   │   └── JobHistoryScreen.kt               # Historial de trabajos
│   │   │   ├── services/
│   │   │   │   ├── LaserCommandService.kt            # Integración láser
│   │   │   │   └── FileEncodingService.kt            # Encoding de diseños
│   │   │   ├── viewmodels/
│   │   │   │   └── LaserViewModel.kt
│   │   │   └── models/
│   │   │       ├── LaserSettings.kt
│   │   │       └── LaserJob.kt
│   │   └── res/
│   └── build.gradle.kts
│
├── app-conveyor/            # ESTACIÓN 4: Cinta y pallets
│   ├── app/src/main/
│   │   ├── java/com/sistema/distribuido/conveyor/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/screens/
│   │   │   │   ├── ConveyorControlScreen.kt          # Control de velocidad/dirección
│   │   │   │   ├── PalletManagementScreen.kt         # Gestión de pallets
│   │   │   │   ├── LockingSystemScreen.kt            # Bloqueos físicos
│   │   │   │   └── SensorMonitorScreen.kt            # Sensores de posición
│   │   │   ├── services/
│   │   │   │   ├── ConveyorCommandService.kt
│   │   │   │   └── PalletTrackingService.kt          # [NUEVO] Tracking de pallets
│   │   │   ├── viewmodels/
│   │   │   │   └── ConveyorViewModel.kt
│   │   │   └── models/
│   │   │       ├── PalletState.kt
│   │   │       └── ConveyorStatus.kt
│   │   └── res/
│   └── build.gradle.kts
│
├── app-plc/                 # Interfaz PLC/Supervisory (si aplica)
├── app-manufactura/         # Dashboard general
├── app-calidad/             # Dashboard de calidad
├── app-almacen/             # Gestión de almacén
│
├── firmware/                # ESP32 Firmware
│   ├── Firmware_Support/
│   │   ├── src/main/
│   │   │   └── cim_esp32_firmware_v7.ino           # [REFACTORIZADO] FreeRTOS multitask
│   │   ├── platformio.ini
│   │   └── lib/
│   │       ├── CimProtocol/
│   │       │   ├── cim_protocol.h
│   │       │   └── cim_protocol.cpp
│   │       └── FreeRTOSConfig/
│   │           └── FreeRTOSConfig.h                 # [NUEVO] Config central
│   ├── esp32_actor_scorbot/                         # Firmware dedicado para Scorbot
│   ├── esp32_actor_vision/                          # Firmware dedicado para cámara
│   ├── esp32_actor_laser/                           # Firmware dedicado para láser
│   └── esp32_actor_conveyor/                        # Firmware dedicado para cinta
│
├── scripts/
│   ├── deploy_coordinator.ps1                       # [NUEVO] Deploy coordinador
│   ├── deploy_stations.ps1                          # [NUEVO] Deploy estaciones
│   ├── firmware/
│   │   ├── build_firmware.sh
│   │   └── flash_esp32.sh
│   └── database/
│       └── init_schema.sql
│
├── docs-generated/          # [NUEVO] Documentación autogenerada
│   ├── Architecture_Diagram.md
│   ├── API_Swagger.yaml
│   └── Changelog.md
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 2. SEPARACIÓN DE RESPONSABILIDADES POR APK

### **app-coordinador** (Maestro centralizado)
- ✅ Gestión de autorización **centralizada**
- ✅ Descubrimiento y escaneo BLE/SPP hybrid
- ✅ Dashboard en tiempo real de todos los nodos
- ✅ Aceptar/rechazar solicitudes de comando de estaciones
- ✅ Monitoreo de salud del sistema

### **app-scorbot** (Estación independiente)
- ✅ Control de brazo robótico Scorbot
- ✅ UI de trayectorias y posicionamiento
- ✅ Solicitud de autorización al coordinador antes de ejecutar
- ✅ Monitoreo local de articulaciones
- ❌ No gestiona autorizaciones de otras estaciones

### **app-vision** (Estación independiente)
- ✅ Generación de marcadores ArUco
- ✅ Detección de marcadores en stream de cámara
- ✅ Análisis de calidad (defectos, dimensiones)
- ✅ Solicitud de autorización al coordinador
- ❌ No está acoplada a Scorbot o láser

### **app-laser** (Estación independiente)
- ✅ Control de potencia y velocidad del láser
- ✅ Editor de diseños (SVG/vector)
- ✅ Preview antes de grabar
- ✅ Solicitud de autorización al coordinador
- ❌ No interfiere con otras estaciones

### **app-conveyor** (Estación independiente)
- ✅ Control de cinta transportadora
- ✅ Gestión de pallets (bloqueos, tracking)
- ✅ Sensores de posición
- ✅ Solicitud de autorización al coordinador
- ❌ No acoplada a otras estaciones

---

## 3. FLUJO DE AUTORIZACIÓN CENTRALIZADO

```
┌──────────────────────────────────────┐
│   ESP32 Actor (Scorbot/Laser/etc)    │
│   [FreeRTOS Multitask]               │
│   ├─ CommTask                        │
│   ├─ ActuatorTask                    │
│   ├─ SensorTask                      │
│   └─ HeartbeatTask                   │
│                                      │
│   SEND: IDENTIFY | MAC | VERSION     │
└────────┬─────────────────────────────┘
         │
         │ BLE/SPP
         │
┌────────▼─────────────────────────────┐
│   app-coordinador (Maestro)          │
│                                      │
│  PermissionManager                   │
│   ├─ Almacena solicitudes pendientes │
│   ├─ Timeout automático (5s)         │
│   └─ Recuerda decisiones             │
│                                      │
│  AuthorizationManager                │
│   ├─ Mantiene estado por MAC         │
│   ├─ Autorizado / Rechazado / Pendiente
│   └─ Revocación en tiempo real       │
│                                      │
│  CommunicationCoordinator            │
│   ├─ Encamina mensajes               │
│   ├─ Valida autorización             │
│   └─ Multiplex BLE/SPP/TCP           │
│                                      │
│  UI: AuthorizationDialog             │
│   └─ Aprueba/rechaza con 1 click     │
└─────────────────────────────────────┘
         │
         │ Decisión: AUTHORIZED / REJECTED
         │
┌────────▼─────────────────────────────┐
│   app-scorbot / app-laser / etc      │
│   (Estación esclava)                 │
│                                      │
│   Recibe: AUTHORIZED                 │
│   → Habilita UI de control           │
│   → Ejecuta comandos                 │
│                                      │
│   Recibe: REJECTED                   │
│   → Deshabilita UI                   │
│   → Muestra motivo                   │
└──────────────────────────────────────┘
```

---

## 4. MAPEO DE HARDWARE

| **Dispositivo**       | **MAC Address** | **Firmware**            | **APK Controladora**  |
|-----------------------|-----------------|-------------------------|----------------------|
| ESP32 Scorbot         | AA:BB:CC:01:02  | `esp32_actor_scorbot`   | `app-scorbot`        |
| ESP32 Visión/Aruco    | AA:BB:CC:03:04  | `esp32_actor_vision`    | `app-vision`         |
| ESP32 Láser           | AA:BB:CC:05:06  | `esp32_actor_laser`     | `app-laser`          |
| ESP32 Cinta/Pallets   | AA:BB:CC:07:08  | `esp32_actor_conveyor`  | `app-conveyor`       |

---

## 5. NUEVOS ARCHIVOS DE CONFIGURACIÓN

### `gradle/libs.versions.toml` (Central de dependencias)
```toml
[versions]
kotlin = "1.9.0"
coroutines = "1.7.1"
compose = "1.6.0"
ble = "1.1.1"
opencv = "4.8.0"

[libraries]
# ... todas las deps compartidas
```

### `firebase/remote_config.json` (Configuración remota)
```json
{
  "authorization_timeout_ms": 5000,
  "reconnect_backoff_max_ms": 30000,
  "heartbeat_interval_ms": 10000,
  "mtu_ble_max": 512,
  "max_concurrent_devices": 8
}
```

---

## 6. BUILD MATRIX (Deployment)

```bash
# Coordinador (principal)
./gradlew app-coordinador:assembleRelease

# Estaciones (paralelo)
./gradlew app-scorbot:assembleRelease \
           app-vision:assembleRelease \
           app-laser:assembleRelease \
           app-conveyor:assembleRelease

# Todo en uno
./gradlew assembleRelease
```

---

## 7. MIGRACIONES PENDIENTES

- [ ] Separar `app-manufactura` → Dashboard de coordinador
- [ ] Separar `app-calidad` → Integrar en `app-vision` como QA
- [ ] Refactorizar `app-plc` si aplica
- [ ] Eliminar `app-almacen` si no es crítico
- [ ] Centralizar `core-network` (ya existe, solo añadir `CommunicationCoordinator`)

---

## 8. TIMELINE DE IMPLEMENTACIÓN

1. **Fase 1**: Refactorizar `BluetoothHardwareManager` + centralizar autorización (Ya iniciado)
2. **Fase 2**: Reescribir firmware ESP32 en FreeRTOS (Completado en v6.0)
3. **Fase 3**: Crear app-scorbot, app-vision, app-laser, app-conveyor como módulos independientes
4. **Fase 4**: Implementar UI de coordinador con AuthorizationDialog
5. **Fase 5**: Testing E2E y optimización
6. **Fase 6**: Documentación final y release v7.0
