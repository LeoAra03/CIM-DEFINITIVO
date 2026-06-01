# PROTOCOLO DE COMUNICACIÓN CIM v7.0

## 1. HANDSHAKE Y AUTORIZACIÓN CENTRALIZADA

### Fase 1: Identificación Inicial

```
┌─────────────────────────────────────────────────────────────────┐
│ ESP32 (FreeRTOS) — Tarea Heartbeat                              │
│ ════════════════════════════════════════════════════════════════ │
│                                                                  │
│  1. Al arrancar:                                                 │
│     → Inicializa BLE/SPP                                         │
│     → Construye identidad: MAC + VERSION + NOMBRE               │
│     → Envía IDENTIFY cada 2s hasta recibir respuesta            │
│                                                                  │
│  TX: "IDENTIFY|AA:BB:CC:DD:EE:FF|6.0.0"                        │
│                                                                  │
│  Tiempo: T0 (0ms)                                                │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ BLE / SPP
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ BluetoothHardwareManager (app-coordinador)                       │
│ ════════════════════════════════════════════════════════════════ │
│                                                                  │
│  1. Recibe IDENTIFY                                              │
│  2. Extrae MAC + VERSION                                         │
│  3. Llama: CommunicationCoordinator.registerSession()           │
│     - Crea SessionState(MAC, "ESP32_EEFF", PENDING)            │
│                                                                  │
│  Tiempo: T1 (T0 + ~10ms)                                          │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ CommunicationCoordinator.registerSession()                       │
│ ════════════════════════════════════════════════════════════════ │
│                                                                  │
│  1. Crea SessionState: authState = AUTH_PENDING                │
│  2. Llama: PermissionManager.requestPermission()               │
│                                                                  │
│  Tiempo: T2 (T1 + ~5ms)                                           │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ PermissionManager                                                │
│ ════════════════════════════════════════════════════════════════ │
│                                                                  │
│  1. Verifica decisión recordada:                                │
│     - Si existe en remembereddecisions → devolver inmediatamente│
│     - Si no existe → mostrar dialog                             │
│                                                                  │
│  2. Crea PermissionRequest                                      │
│  3. Dispara: listeners.onPermissionRequested()                 │
│  4. Espera respuesta CON TIMEOUT (5 segundos)                  │
│                                                                  │
│  Tiempo: T3 (T2 + ~20ms) — Dialog visible                       │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ Usuario hace click en:
                           │ ┌─ APPROVE ─┐
                           │ └─ REJECT ──┘
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ PermissionManager.approve() o .reject()                          │
│ ════════════════════════════════════════════════════════════════ │
│                                                                  │
│  1. Marca PermissionRequest.approved = true/false              │
│  2. Opcionalmente guarda decisión (remembereddecisions)        │
│  3. Actualiza: AuthorizationManager.authorize(mac)             │
│  4. Dispara: listeners.onPermissionApproved(mac)               │
│                                                                  │
│  Tiempo: T4 (T3 + ~10 a 3000ms, según el usuario)              │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ CommunicationCoordinator.requestAuthorizationInternal()         │
│ ════════════════════════════════════════════════════════════════ │
│                                                                  │
│  1. Recibe decision = APPROVED                                  │
│  2. Llama: AuthorizationManager.authorize(mac)                 │
│  3. Actualiza: SessionState.authState = AUTH_AUTHORIZED       │
│  4. Log: "[COORD] ✓ AUTORIZADO: AA:BB:CC:DD:EE:FF"            │
│                                                                  │
│  Tiempo: T5 (T4 + ~5ms)                                           │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ BLE Response
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ BluetoothHardwareManager.sendAuthorization()                     │
│ ════════════════════════════════════════════════════════════════ │
│                                                                  │
│  TX: "AUTHORIZED|AA:BB:CC:DD:EE:FF"                            │
│                                                                  │
│  Tiempo: T6 (T5 + ~10ms)                                          │
└─────────────────────────────────────────────────────────────────┘
                           │
                           │ BLE / SPP
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ ESP32 — CommTask (FreeRTOS)                                      │
│ ════════════════════════════════════════════════════════════════ │
│                                                                  │
│  RX: "AUTHORIZED|AA:BB:CC:DD:EE:FF"                            │
│                                                                  │
│  → parseAndDispatch()                                            │
│     • Extrae status = "AUTHORIZED"                              │
│     • xSemaphoreTake(authMutex) ← MUTEX para proteger         │
│     • authState = AUTH_STATE_AUTHORIZED ← ESTADO SEGURO       │
│     • xSemaphoreGive(authMutex)                                 │
│  → Envía ACK: "ACK|AUTHORIZED|ESP32_EEFF"                     │
│                                                                  │
│  Tiempo: T7 (T6 + ~15ms)                                          │
│                                                                  │
│  🟢 AUTORIZACIÓN COMPLETADA — ESP32 AHORA PUEDE EJECUTAR       │
└─────────────────────────────────────────────────────────────────┘

Total tiempo de handshake: ~100-3100ms (depende del usuario)
```

---

## 2. FLUJO DE COMANDO AUTORIZADO

```
┌──────────────────────────────────────────────────────────────┐
│ app-scorbot (Esclava)                                         │
│ ══════════════════════════════════════════════════════════════ │
│                                                               │
│ Usuario presiona: "Extender Brazo"                           │
│                                                               │
│ → ViewModel verifica AuthorizationManager.isAuthorized()     │
│   Si NO → UI bloqueada, mensaje "Awaiting Authorization"    │
│   Si SÍ → Continuar                                          │
│                                                               │
│ → Construye comando:                                         │
│    CimMessageBuilder.createCommand(                          │
│      sourceMac = "app-scorbot-MAC",                          │
│      targetMac = "esp32-scorbot-MAC",                        │
│      command = "R:EXTEND|10",      // Rotar 10 grados       │
│      commandId = UUID()                                      │
│    )                                                         │
│                                                               │
│ → Llama: CommunicationCoordinator.routeCommand(...)         │
│                                                               │
│ TX: "COMMAND;CMD_001|R:EXTEND|10"                           │
└──────────────────────────────────────────────────────────────┘
                      │
                      │ BLE / SPP
                      ▼
┌──────────────────────────────────────────────────────────────┐
│ ESP32 Scorbot — CommTask (FreeRTOS)                          │
│ ══════════════════════════════════════════════════════════════ │
│                                                               │
│ RX: "COMMAND;CMD_001|R:EXTEND|10"                           │
│                                                               │
│ → CommTask recibe en commQueue                               │
│ → parseAndDispatch(message)                                  │
│ → Verifica: strncmp("COMMAND;", 8) == 0 ✓                  │
│ → Extrae commandId = "CMD_001"                              │
│                                                               │
│ → Mutex check (authState):                                   │
│    xSemaphoreTake(authMutex)                                 │
│    if (authState == AUTH_STATE_AUTHORIZED) {                │
│      → xQueueSend(actuatorQueue, frame) ← Pasa a tarea     │
│      → xSemaphoreGive(authMutex)                            │
│      → TX: "ACK|CMD_001|ESP32_SCORBOT"                      │
│    } else {                                                  │
│      → TX: "NACK|CMD_001|NOT_AUTHORIZED"                   │
│    }                                                         │
│                                                               │
│ Tiempo: ~20-50ms                                             │
└──────────────────────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────────────────┐
│ ESP32 Scorbot — ActuatorTask (FreeRTOS)                      │
│ ══════════════════════════════════════════════════════════════ │
│                                                               │
│ Dequeue: "CMD_001|R:EXTEND|10"                              │
│                                                               │
│ → parseMotorCommand("R:EXTEND|10")                           │
│ → Extrae: motor = ROTATE, param = EXTEND, value = 10       │
│ → Envía PWM a motor via GPIO                                │
│                                                               │
│ → Monitorea encoder para confirmar movimiento               │
│ → Si éxito:  TX: "STATUS|MOTOR_EXTENDED|READY"             │
│ → Si error:  TX: "STATUS|MOTOR_ERROR|STALL"                │
│                                                               │
│ Tiempo: ~100-500ms (depende del actuador físico)            │
└──────────────────────────────────────────────────────────────┘
                      │
                      │ BLE Response
                      ▼
┌──────────────────────────────────────────────────────────────┐
│ app-scorbot — ViewModel                                      │
│ ══════════════════════════════════════════════════════════════ │
│                                                               │
│ RX: "STATUS|MOTOR_EXTENDED|READY"                           │
│                                                               │
│ → onDataReceived(mac, message)                               │
│ → Actualiza UI: armState.motorState = EXTENDED              │
│ → Notifica: collectAsState() → Recompose                    │
│ → UI muestra: "✓ Brazo extendido - Listo"                  │
│                                                               │
│ Tiempo: ~10-20ms (Compose rendering)                         │
│                                                               │
│ 🟢 COMANDO EJECUTADO Y CONFIRMADO                           │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. FORMATO DE MENSAJES (Wire Protocol)

### 3.1 IDENTIFY (Handshake)
```
TX (ESP32 → app-coordinador):
  "IDENTIFY|AA:BB:CC:DD:EE:FF|6.0.0"
  
Campos:
  [0] = "IDENTIFY"
  [1] = MAC Address
  [2] = Firmware Version

RX (Respuesta):
  "AUTHORIZED|AA:BB:CC:DD:EE:FF"
  o
  "REJECTED|AA:BB:CC:DD:EE:FF|RATE_LIMITED"
```

### 3.2 STATUS (Heartbeat)
```
TX (ESP32 → app-coordinador):
  "STATUS|ESP32_SCORBOT|READY"
  
Campos:
  [0] = "STATUS"
  [1] = Device Name
  [2] = State (READY | BUSY | ERROR | WAITING_AUTH)

Intervalo: Cada 10 segundos (HeartbeatTask)
```

### 3.3 COMMAND (Ejecutable)
```
TX (app-scorbot → ESP32):
  "COMMAND;CMD_ID_001|R:EXTEND|10"
  
Campos:
  [0] = "COMMAND"
  [1] = Command ID (para correlación de ACK/NACK)
  [2+] = Payload específico del actor

RX (ESP32 → app-scorbot):
  "ACK|CMD_ID_001|ESP32_SCORBOT"
  o
  "NACK|CMD_ID_001|STALL_DETECTED"
```

### 3.4 EVENT (Notificación)
```
TX (ESP32 → app-coordinador):
  "EVENT|SENSOR_TRIGGERED|PROXIMITY"
  
Campos:
  [0] = "EVENT"
  [1] = Event Type
  [2] = Additional Data (opcional)

Ejemplos:
  "EVENT|PACKAGE_DETECTED|PALLET_05"
  "EVENT|LASER_READY|POWER_80"
  "EVENT|VISION_DEFECT|CRACK_AT_45DEG"
```

---

## 4. MÁQUINA DE ESTADOS - ESP32

```
                ┌──────────────────┐
                │   BOOT           │
                │                  │
                │ • Init BLE/SPP    │
                │ • Init FreeRTOS   │
                │ • Start tasks     │
                └─────────┬────────┘
                          │
                          ▼
            ┌─────────────────────────────┐
            │ UNVERIFIED (IDENTIFYING)    │
            │                             │
            │ • CommTask envía IDENTIFY   │
            │   cada 2 segundos           │
            │ • Espera AUTHORIZED/REJECT  │
            └──────┬────────────────┬─────┘
                   │                │
                   │ Recibe         │ Recibe
                   │ AUTHORIZED     │ REJECTED
                   ▼                ▼
        ┌──────────────────┐   ┌──────────────────┐
        │ AUTHORIZED       │   │ REJECTED         │
        │                  │   │                  │
        │ • ActuatorTask   │   │ • Bloquea todos  │
        │   acepta comandos│   │   los comandos   │
        │ • Heartbeat      │   │ • Espera revocación
        │   = READY        │   │ • HeartbeatTask  │
        │                  │   │   = BLOCKED      │
        │                  │   │                  │
        │ ← → ← → ← →      │   │ ← → ← → ← →      │
        └──────────────────┘   └──────────────────┘
                   │                ▲
                   │ Recibe REJECTED │ Recibe AUTHORIZED
                   └────────┬────────┘
                            │
                    ┌───────▼──────────┐
                    │ TIMEOUT / ERROR  │
                    │                  │
                    │ • Reconecta BLE  │
                    │ • Reintenta Auth │
                    │ • Backoff exp.   │
                    └──────────────────┘
```

---

## 5. TIMEOUTS Y REINTENTOS

### ESP32 Firmware
```
Identificación (IDENTIFY):
  - Intervalo: 2 segundos
  - Max intentos: Infinito hasta autorización
  - Backoff: Ninguno

Comando (COMMAND):
  - Timeout respuesta (ACK/NACK): 10 segundos
  - Max reintentos: 3 (con backoff: 1s, 2s, 4s)

Heartbeat (STATUS):
  - Intervalo: 10 segundos
  - Timeout de inactividad antes de reconectar: 30 segundos
```

### app-coordinador (Android)
```
Escaneo BLE:
  - Duración: 10 segundos
  - Intervalo de reintentos: 5 segundos

Autorización (PermissionManager):
  - Timeout de diálogo: 5 segundos
  - Si timeout → DENIED automáticamente

Reconexión (BLE):
  - Backoff inicial: 1 segundo
  - Backoff máximo: 30 segundos
  - Multiplicador: 2x

Comando saliente:
  - Timeout: 10 segundos (configurable)
  - Max reintentos: 3
```

---

## 6. EJEMPLO COMPLETO: ENVIAR COMANDO LÁSER

### Escenario
- Usuario en app-laser presiona "Grabar diseño"
- Diseño es: "SQUARE|50MM|100W"
- ESP32 láser necesita autorizarse primero

### Timeline
```
T0:     ESP32 Laser bootea
        → TX: "IDENTIFY|AA:BB:CC:05:06|6.0.0"

T10ms:  app-coordinador recibe IDENTIFY
        → CommunicationCoordinator.registerSession()
        → PermissionManager.requestPermission()
        → Dialog aparece en UI

T50ms:  Usuario ve: "Nueva solicitud de LaserPrinter"
        → Botón APPROVE resaltado

T2000ms: Usuario presiona APPROVE
        → PermissionManager.approve(mac)
        → AuthorizationManager.authorize(mac)

T2010ms: BLE TX: "AUTHORIZED|AA:BB:CC:05:06"

T2025ms: ESP32 recibe:
        → authState = AUTH_AUTHORIZED
        → Status = READY
        → TX: "STATUS|LaserPrinter|READY"

T3000ms: Usuario en app-laser presiona "Grabar"
        → ViewModel verifica: isAuthorized = true ✓
        → Construye mensaje:
           "COMMAND;LASER_001|ENGRAVE|SQUARE|50MM|100W"
        → BLE TX

T3015ms: ESP32 Laser recibe
        → Extrae: cmdId=LASER_001, payload="ENGRAVE|SQUARE|50MM|100W"
        → xSemaphoreTake(authMutex)
        → authState == AUTHORIZED ✓
        → xQueueSend(actuatorQueue)
        → TX: "ACK|LASER_001|LaserPrinter"

T3050ms: app-laser recibe ACK
        → UI muestra: "✓ Grabado iniciado..."

T3100ms: ActuatorTask en ESP32
        → Extrae comando de queue
        → Inicia laser (PWM, servo)
        → Monitorea progreso

T5000ms: Laser finaliza
        → TX: "EVENT|ENGRAVE_COMPLETE|SQUARE_50MM"

T5020ms: app-laser recibe EVENT
        → ViewModel actualiza estado
        → UI muestra: "✓ Grabado completado"
        → Opcionalmente guarda en base de datos

🟢 FLUJO COMPLETO: 5000ms aprox
```

---

## 7. DIAGRAMA DE CAPAS

```
┌──────────────────────────────────────────────────────────┐
│                   UI LAYER (Jetpack Compose)            │
│  ┌─────────────┬──────────────┬──────────┬────────────┐
│  │ HubScreen   │ ScorbotUI    │ LaserUI  │ ConveyorUI │
│  └──────┬──────┴──────┬───────┴────┬─────┴──────┬──────┘
└─────────┼──────────────┼────────────┼────────────┼──────┘
          │              │            │            │
          └──────────────┴────────────┴────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│             VIEWMODEL & STATE LAYER                      │
│  ┌──────────────┬─────────────┬──────────────┐          │
│  │ HubViewModel │ LaserVModel │ ConveyorVModel          │
│  └──────┬───────┴──────┬──────┴──────────┬───┘          │
└─────────┼──────────────┼─────────────────┼──────────────┘
          │              │                 │
┌─────────▼──────────────▼─────────────────▼──────────────┐
│        COORDINATION LAYER (core-network)                │
│  ┌────────────────────────────────────────────────────┐ │
│  │ CommunicationCoordinator                           │ │
│  │  • AuthorizationManager                            │ │
│  │  • PermissionManager                               │ │
│  │  • SessionState tracking                           │ │
│  └──────────┬────────────────────────────┬────────────┘ │
└─────────────┼────────────────────────────┼───────────────┘
              │                            │
┌─────────────▼────────────────────────────▼───────────────┐
│         TRANSPORT LAYER                                  │
│  ┌──────────────┬─────────────┬──────────────────────┐   │
│  │ BLE GATT     │ BLE SPP     │ TCP/IP (si aplica)  │   │
│  │ Manager      │ Manager     │ Manager              │   │
│  └──────────────┴─────────────┴──────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                         │
        ╔════════════════╩═════════════════╗
        ║                                  ║
    ┌───▼────────┐              ┌────────▼───┐
    │   Kernel BT│              │   WiFi     │
    │  Subsystem │              │  Subsystem │
    └────────────┘              └────────────┘
        │                            │
        ▼                            ▼
    ┌─────────────┐         ┌─────────────┐
    │ ESP32 (BLE) │         │  WiFi AP    │
    │ + SPP       │         │  (futuro)   │
    └─────────────┘         └─────────────┘
        │ Inalámbrico
        ▼
    ┌──────────────────────────────────────┐
    │      ESP32 Actors (FreeRTOS)         │
    │ ┌──────────────────────────────────┐ │
    │ │ CommTask | ActuatorTask | ...   │ │
    │ │ with Queues & Semaphores        │ │
    │ └──────────────────────────────────┘ │
    │               │                       │
    │               ▼                       │
    │   ┌─────────────────────────┐        │
    │   │ Motor/Laser/Camera GPIO │        │
    │   │ & Sensor Inputs         │        │
    │   └─────────────────────────┘        │
    │               │                       │
    │               ▼                       │
    │   ┌─────────────────────────┐        │
    │   │  Robotic Arm / Laser    │        │
    │   │  Conveyor / Vision      │        │
    │   └─────────────────────────┘        │
    └──────────────────────────────────────┘
```

---

## 8. SEGURIDAD Y VALIDACIÓN

### Validaciones en cada capa

**CommunicationCoordinator**:
- ✓ Verificar AuthorizationManager antes de routeCommand
- ✓ Validar formato de mensaje (COMMAND|ID|PAYLOAD)
- ✓ Timeout de comandos pendientes
- ✓ Limit rate de intentos (max 3 por comando)

**BluetoothHardwareManager**:
- ✓ Validar MAC format
- ✓ Validar tamaño de MTU (max 512 bytes)
- ✓ Encripción TLS si aplica
- ✓ Timeouts de escritura per-device

**ESP32 FreeRTOS**:
- ✓ Proteger authState con Mutex
- ✓ Validar comandos contra whitelist
- ✓ Timeout de inactividad de autorización (revoke después de 30min)
- ✓ Stack overflow protection

---

## 9. DEBUGGING & LOGGING

### Log format estándar
```kotlin
"[${timestamp}] [${component}] [${level}] ${message}"

Ejemplos:
"[2024-01-15 14:23:45] [BLE] [INFO] ✓ CONECTADO: AA:BB:CC:DD:EE:FF"
"[2024-01-15 14:23:50] [COORD] [INFO] → ENVÍO: AA:BB:CC:DD:EE:FF | COMMAND;CMD_001|R:EXTEND|10"
"[2024-01-15 14:23:52] [COORD] [ERROR] ✗ NACK: CMD_001 | Razón: STALL_DETECTED"
```

### Rutas de log
- **Android**: `Logcat` + Firebase Crashlytics
- **ESP32**: `Serial Monitor` @ 115200 baud

---

## 10. PRÓXIMOS PASOS DE IMPLEMENTACIÓN

1. ✅ Crear `CommunicationCoordinator.kt` (HECHO)
2. ✅ Refactorizar ESP32 firmware a FreeRTOS (HECHO)
3. ✅ Mejorar `BluetoothHardwareManager` (EN PROGRESO)
4. ⏳ Integrar `CommunicationCoordinator` en `BluetoothHardwareManager.onDataReceived()`
5. ⏳ Crear UI de autorización con `AuthorizationDialog` Compose
6. ⏳ Implementar app-scorbot, app-vision, app-laser, app-conveyor
7. ⏳ Testing E2E: handshake → autorización → comando → confirmación
8. ⏳ Optimizar MTU, retry logic y timeouts basado en telemetría real
