# Guía Profesional CIM v6.0

> **Documento consolidado:** Ver secciones 3 y 6 en [`ENTREGA_FINAL_LEONARDO_ARAYA.md`](ENTREGA_FINAL_LEONARDO_ARAYA.md).

> **Computer Integrated Manufacturing** — Sistema distribuido Android + ESP32  
> Autor del sistema: Practica_2 UBB | Versión **6.0.0**

---

## Tabla de contenidos

1. [Visión general](#1-visión-general)
2. [Arquitectura del sistema](#2-arquitectura-del-sistema)
3. [Flujo Bluetooth multiconexión](#3-flujo-bluetooth-multiconexión)
4. [Protocolo TCP CIM](#4-protocolo-tcp-cim)
5. [Sincronización de estaciones](#5-sincronización-de-estaciones)
6. [Glosario de objetos de referencia](#6-glosario-de-objetos-de-referencia)
7. [Complejidad y rendimiento](#7-complejidad-y-rendimiento)
8. [Despliegue rápido](#8-despliegue-rápido)

---

## 1. Visión general

El ecosistema CIM v6.0 implementa una **línea de producción virtual** con cinco aplicaciones Android y nodos ESP32. El **Coordinador (Hub)** centraliza autorización, enrutamiento de comandos y visibilidad de la malla.

![Arquitectura CIM v6.0](assets/imagenes/cim_arquitectura_v6.png)

### Principios de diseño

| Principio | Implementación |
|-----------|----------------|
| **Hub-and-Spoke** | Coordinador TCP:8888 + estaciones cliente |
| **Separación de capas** | Apps → `core-network` → firmware |
| **Fail-safe BT** | BLE primario, SPP Classic como fallback |
| **Autorización explícita** | MAC + handshake antes de ejecutar |
| **O(1) en hot paths** | `ConcurrentHashMap` por MAC/IP |

Manuales detallados: [docs/manuals/](manuals/)

---

## 2. Arquitectura del sistema

```mermaid
flowchart TB
    subgraph Hub["Capa Hub — Coordinador"]
        VM[CoordinatorViewModel]
        TS[TcpServer :8888]
        BR[CommandBroker]
        AR[AuthorizationManager]
        DR[MobileDeviceRegistry]
    end

    subgraph Stations["Capa Estaciones Android"]
        PLC[app-plc]
        MAN[app-manufactura]
        CAL[app-calidad]
        ALM[app-almacen]
    end

    subgraph Network["core-network"]
        BHM[BluetoothHardwareManager]
        SPP[BluetoothSppManager]
        SC[StationClient]
    end

    subgraph HW["Capa Hardware ESP32"]
        ESP1[ESP32 Cinta]
        ESP2[ESP32 Robot]
        ESP3[ESP32 Sensores]
    end

    VM --> TS
    VM --> BR
    TS --> AR
    TS --> DR
    PLC --> SC
    MAN --> SC
    CAL --> SC
    ALM --> SC
    SC -->|TCP| TS
    PLC --> BHM
    BHM -->|BLE GATT| ESP1
    SPP -->|Classic RFCOMM| ESP1
    MAN --> BHM
    BHM --> ESP2
```

### Módulos Gradle

```
Practica_2/
├── core-network/          # Librería compartida (TCP, BT, protocolo, visión)
├── app-coordinador/       # Hub central
├── app-plc/               # Cinta transportadora
├── app-manufactura/       # Robot + láser
├── app-calidad/           # ArUco / tracking
├── app-almacen/           # Grid de almacenamiento
└── firmware/Firmware_Support/  # ESP32 PlatformIO
```

---

## 3. Flujo Bluetooth multiconexión

```mermaid
sequenceDiagram
    participant UI as App Estación (Compose)
    participant BHM as BluetoothHardwareManager
    participant GATT as BLE GATT (por MAC)
    participant ESP as ESP32 Nodo

    UI->>BHM: startScan()
    BHM->>BHM: BLE scan + Classic discovery
    BHM-->>UI: discoveredDevicesMap (StateFlow)

    UI->>BHM: connect(mac)
    BHM->>GATT: connectGatt(mac)
    GATT-->>BHM: onServicesDiscovered
    BHM->>GATT: enable notifications (TX char)

    ESP->>GATT: IDENTIFY|CIM-ST-PLC-X4
    GATT-->>BHM: onCharacteristicChanged
    BHM->>BHM: handleIdentifyResponse()
    BHM-->>UI: onDataReceived(mac, payload)

    UI->>BHM: sendToDevice(mac, "R:100")
    BHM->>GATT: fragment MTU 20B + delay 20ms
    GATT->>ESP: comando serial
    ESP-->>GATT: ACK|OK
```

### Estados de conexión

```
┌─────────────┐    scan     ┌──────────────┐   connect   ┌─────────────┐
│  IDLE       │ ──────────► │  DISCOVERED  │ ──────────► │  CONNECTING │
└─────────────┘             └──────────────┘             └──────┬──────┘
                                                                │
                    ┌───────────────────────────────────────────┘
                    ▼
             ┌─────────────┐   disconnect   ┌─────────────┐
             │  CONNECTED  │ ◄────────────► │  RECONNECT  │
             │ (por MAC)   │   backoff exp  │  1s → 30s   │
             └─────────────┘                └─────────────┘
```

**Multiconexión:** cada MAC mantiene su propio `BluetoothGatt` en `ConcurrentHashMap<String, BluetoothGatt>`. La UI observa `connectionStates: StateFlow<Map<String, Boolean>>`.

---

## 4. Protocolo TCP CIM

```mermaid
flowchart LR
    A[Estación Android] -->|CimMessage transport string| B[TcpServer Hub]
    B --> C{CommandType?}
    C -->|IDENTIFY| D[AuthorizationManager]
    C -->|REQUIRE_PERMISSION| D
    C -->|EXECUTE| E[CommandBroker.route]
    C -->|HEARTBEAT| F[MobileDeviceRegistry.ping]
    D -->|GRANTED/DENIED| A
    E -->|sendToClientByMac| A
```

### Formato de mensaje (transporte)

```
CIM|id|srcMac|srcApp|destMac|destApp|cmdType|priority|sessionId|payload
```

- **Escapado:** `\|` → `\|`, `\n` → `\n` en payload
- **Puerto hub:** `8888` (configurable en coordinador)
- **Lookup cliente:** `macToConnId[mac]` → socket (**O(1)**)

Comandos industriales ESP32 (serial): `R:`, `L:`, `C:`, `STO:`, `CAM:` — ver [02_PROTOCOLO_COMUNICACION_CIM.md](manuals/02_PROTOCOLO_COMUNICACION_CIM.md).

---

## 5. Sincronización de estaciones

```mermaid
sequenceDiagram
    participant ST as Estación (ej. PLC)
    participant HUB as Coordinador
    participant REG as MobileDeviceRegistry

    ST->>HUB: TCP connect
    ST->>HUB: IDENTIFY|CIM-ST-PLC-X4|MAC
    HUB->>REG: register(mac, deviceInfo)
    ST->>HUB: REQUIRE_PERMISSION|nombre|pass|mac|uuid
    Note over HUB: Operador autoriza en UI
    HUB->>ST: GRANTED
    HUB->>REG: authorize(mac)

    loop Operación normal
        HUB->>ST: EXECUTE|DELIVER|1|2
        ST->>ST: traduce → Bluetooth → ESP32
        ST->>HUB: ACK|OK
    end

    HUB->>REG: broadcastClientList() cada 2s
```

### Modo demo (sin hardware)

Los botones **"Simular *"** registran comandos en el terminal local de cada app. Útil para demos académicas y tests de UI.

---

## 6. Glosario de objetos de referencia

| Objeto | Paquete | Responsabilidad |
|--------|---------|-----------------|
| `CimMessage` | `protocol` | DTO del mensaje CIM; serialización transport |
| `CommandBroker` | `network` | Enrutamiento central; log de comandos |
| `TcpServer` | `network` | Servidor hub; mapa MAC→socket |
| `StationClient` | `network` | Cliente TCP de estaciones |
| `BluetoothHardwareManager` | `network` | BLE multiconexión + escaneo híbrido |
| `BluetoothSppManager` | `network` | Fallback Classic RFCOMM |
| `AuthorizationManager` | `network` | Estado AUTH por MAC |
| `MobileDeviceRegistry` | `network` | Registro O(1) dispositivos móviles |
| `DeviceRegistry` | `network` | Registro legacy por IP (ESP32 WiFi) |
| `DiscoveredBluetoothDevice` | `network` | DTO escaneo BT |
| `PermissionManager` | `network` | Permisos Android runtime |
| `CoordinatorViewModel` | `app-coordinador` | Estado UI del hub |

### Tipos de app (`AppType`)

`COORDINADOR`, `PLC`, `MANUFACTURA`, `CALIDAD`, `ALMACEN`, `UNKNOWN`

### Tipos de comando (`CommandType`)

`IDENTIFY`, `IDENTIFIED`, `EXECUTE`, `ACK`, `ERROR`, `HEARTBEAT`, `REQUIRE_PERMISSION`, `START_SEQUENCE`, `STOP_SEQUENCE`, `STATUS_RESPONSE`, `TIMEOUT`

---

## 7. Complejidad y rendimiento

Ver auditoría completa en [TEST_MATRIX.md](TEST_MATRIX.md#auditoría-de-complejidad-temporal-o1).

```
Operación                          │ Estructura              │ Big-O
───────────────────────────────────┼─────────────────────────┼──────
getDeviceByMac(mac)                │ ConcurrentHashMap       │ O(1)
isAuthorized(mac)                  │ ConcurrentHashMap       │ O(1)
sendToClientByMac(mac)             │ macToConnId             │ O(1)
sendToDevice(mac, cmd)             │ connectedDevices        │ O(1)
getDevicesByType(type)             │ índice + k lookups      │ O(k)
send(cmd) legacy single-target     │ keys.firstOrNull        │ O(n) ⚠
```

---

## 8. Despliegue rápido

```powershell
# 1. Compilar y testear
.\gradlew testAllModules buildAllApks

# 2. Flashear ESP32
.\scripts\hardware-testing\flash_and_monitor_esp32.ps1

# 3. Instalar APKs
adb install -r output-apks\app-coordinador.apk
# ... resto de estaciones

# 4. En coordinador: iniciar Servidor Hub (TCP:8888)
# 5. Autorizar estaciones desde UI
```

---

## Documentación relacionada

| Documento | Contenido |
|-----------|-----------|
| [01_ARQUITECTURA_SISTEMA.md](manuals/01_ARQUITECTURA_SISTEMA.md) | Capas y componentes |
| [02_PROTOCOLO_COMUNICACION_CIM.md](manuals/02_PROTOCOLO_COMUNICACION_CIM.md) | Formato mensajes |
| [03_MOTOR_BLUETOOTH_HIBRIDO.md](manuals/03_MOTOR_BLUETOOTH_HIBRIDO.md) | BLE + SPP |
| [TEST_MATRIX.md](TEST_MATRIX.md) | 30 tests + O(1) audit |
| [EXTENSIONS_AND_TOOLING.md](EXTENSIONS_AND_TOOLING.md) | Entorno Cursor |
| [ESP32_SIMULACION_Y_HARDWARE.md](ESP32_SIMULACION_Y_HARDWARE.md) | Simulación y flash |
| [CHANGELOG_FIXES.md](../CHANGELOG_FIXES.md) | Historial de correcciones |

---

*CIM v6.0 — Documentación de entrega profesional. Espressif-style technical documentation.*
