# 📱 VISUAL GUIDE - CIM v6.0 APKs

## Vista General de las 5 Aplicaciones

---

## 1️⃣ **app-coordinador.apk** - HUB Central

### Pantalla Principal (Pestaña EXEC)

```
┌─────────────────────────────────────────────────────────────┐
│  CIM HUB v6.0                                    [⚙️] [🔵] │
│  SISTEMA DE COORDINACIÓN GLOBAL                             │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │  AUTO MODE: AUTORIZACIÓN AUTOMÁTICA ACTIVADA       │   │
│  │  Solicitudes pendientes: 2                         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐                      │
│  │   🛑 E-STOP  │  │  ▶ SIMULAR   │                      │
│  │   (ROJO)     │  │    CICLO     │                      │
│  └──────────────┘  └──────────────┘                      │
│                                                             │
│  📊 Dashboard Ejecutivo                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│  │ MANUFACT │ │ CALIDAD  │ │ ALMACÉN  │ │  CINTA   │    │
│  │ 🟢 READY │ │ 🟡 BUSY  │ │ 🟢 READY │ │ 🔵 BUSY  │    │
│  │ Listo    │ │ Analiz.  │ │ Pallets  │ │ Transp.  │    │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │
│                                                             │
│  Flujo activo: PLC → MANUFACTURA → CALIDAD → ALMACÉN      │
│  [EMERGENCIA ACTIVA]                                      │
│                                                             │
│  ═══════════════════════════════════════════════════════  │
│  [23:45:12] → FLUJO: Inicio de planta completa            │
│  [23:45:13] → MANUFACTURA: Robot HOME                     │
│  [23:45:14] → CALIDAD: ArUco + YOLO validando             │
│  [23:45:15] → ALMACEN: STO:07 completado                  │
│  ═══════════════════════════════════════════════════════  │
└─────────────────────────────────────────────────────────────┘

NAVEGACIÓN: [EXEC] [CINTA] [ROBOT] [ARUCO] [MAPA] [NODOS] [RACKS]
```

### Pestaña NODOS (Red)

```
┌─────────────────────────────────────────────────────────────┐
│  📡 NODOS - Estado de Red                                   │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │  TCP Server: 🟢 ACTIVO (Puerto 8888)               │   │
│  │  Clientes conectados: 4                            │   │
│  │  Bluetooth: 🟢 3 dispositivos                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Dispositivos Conectados:                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 🟢 PLC-04          192.168.1.105    AUTORIZADO     │   │
│  │ 🟢 MANUFACTURA-02  192.168.1.106    AUTORIZADO     │   │
│  │ 🟢 CALIDAD-03      192.168.1.107    AUTORIZADO     │   │
│  │ 🟢 ALMACEN-05      192.168.1.108    AUTORIZADO     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Solicitudes Pendientes:                                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ⚠️ 192.168.1.109 - PLC-04 (nuevo)                   │   │
│  │    [AUTORIZAR] [RECHAZAR]                           │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 2️⃣ **app-plc.apk** - Estación PLC

### Pantalla Principal (Pestaña CONTROL)

```
┌─────────────────────────────────────────────────────────────┐
│  PLC Master v6.0           [🔵 BT] [🟢 NET]               │
│  CONTROL DE CINTA TRANSPORTADORA                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Estado Operativo: SISTEMA VINCULADO 🟢            │   │
│  │  Modo Autónomo: [ACTIVO 🟢]                        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ⚡ ENERGÍA Y SISTEMA                                       │
│  ┌──────────────┐  ┌──────────────┐                      │
│  │   ▶ Arranque │  │   ⏹ Parada   │                      │
│  │   (VERDE)    │  │   (ROJO)     │                      │
│  └──────────────┘  └──────────────┘                      │
│                                                             │
│  📊 Matriz de Distribución (3×10)                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 1>1 1>2 1>3 1>4 1>5 1>6 1>7 1>8 1>9 1>10           │   │
│  │ 2>1 2>2 2>3 2>4 2>5 2>6 2>7 2>8 2>9 2>10           │   │
│  │ 3>1 3>2 3>3 3>4 3>5 3>6 3>7 3>8 3>9 3>10           │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  📡 TRACKING DE PALLETS                                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 1 · ALMACÉN     ○ vacío      [STOP] [Liberar]      │   │
│  │ 2 · MANUFACTURA ● PALLET     [STOP] [Liberar]      │   │
│  │ 3 · CALIDAD     ○ vacío      [STOP] [Liberar]      │   │
│  │ 4 · PLC/SALIDA  ● PALLET     [STOP] [Liberar]      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ═══════════════════════════════════════════════════════  │
│  [23:45:10] PLC: START                                    │
│  [23:45:12] SENSOR_ACTIVATED | POS:2                      │
│  [23:45:15] PALLET DETENIDO en estación 2                 │
│  ═══════════════════════════════════════════════════════  │
└─────────────────────────────────────────────────────────────┘

NAVEGACIÓN: [CONTROL] [TRACKING] [SINCRO]
```

---

## 3️⃣ **app-manufactura.apk** - Estación Manufactura

### Pantalla Principal

```
┌─────────────────────────────────────────────────────────────┐
│  Manufactura v6.0          [🔵 BT] [🟢 NET]               │
│  ROBOT SCORBOT + LÁSER CNC                                  │
├─────────────────────────────────────────────────────────────┤
│  🤖 CONTROL DE ROBOT SCORBOT                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Estado: 🟢 READY                                    │   │
│  │  Posición: (0.0, 0.0, 0.0)                          │   │
│  │  Gripper: CERRADO                                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐           │
│  │ HOME │ │READY │ │ABORT │ │ABRIR │ │CERRAR│           │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘           │
│                                                             │
│  X-  X+  Y-  Y+  Z-  Z+                                    │
│  [◀] [▶] [▼] [▲] [⬇] [⬆]                                  │
│                                                             │
│  🔥 CONTROL DE LÁSER CNC                                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Estado: 🟢 IDLE     Potencia: [████░░░░] 50%      │   │
│  │                                                      │   │
│  │  Potencia del Láser:                                 │   │
│  │  [━━━━━━━━━━━━━━━━━━━━] 50%                         │   │
│  │                                                      │   │
│  │  ┌──────────┐  ┌──────────┐                        │   │
│  │  │ INICIAR  │  │ DETENER  │                        │   │
│  │  └──────────┘  └──────────┘                        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  📄 GESTIÓN DE G-CODE                                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Archivo: [seleccionar.gcode ▼]  [CARGAR]          │   │
│  │                                                      │   │
│  │  [EJECUTAR G-CODE]                                   │   │
│  │  Progreso: [████████░░░░] 80%                       │   │
│  │  8/10 líneas completadas                            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ═══════════════════════════════════════════════════════  │
│  [23:45:10] R:HOME ejecutado                              │
│  [23:45:12] R:RUN iniciado                                │
│  [23:45:15] L:START - Láser activo                        │
│  ═══════════════════════════════════════════════════════  │
└─────────────────────────────────────────────────────────────┘
```

---

## 4️⃣ **app-calidad.apk** - Estación Calidad

### Pestaña VISIÓN (con Cámara)

```
┌─────────────────────────────────────────────────────────────┐
│  Quality Pro v6.0          [🔵 BT] [🟢 NET]               │
│  CONTROL DE CALIDAD & VISIÓN                                │
├─────────────────────────────────────────────────────────────┤
│  📷 Análisis ArUco / YOLO                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                                                      │   │
│  │              ┌─────────────────────┐                │   │
│  │              │                     │                │   │
│  │              │   📷 CÁMARA ACTIVA  │                │   │
│  │              │                     │                │   │
│  │              │   [ArUco #42]       │                │   │
│  │              │   ████████████      │                │   │
│  │              │   █  █████  █      │                │   │
│  │              │   ████████████      │                │   │
│  │              │                     │                │   │
│  │              │   ✅ ID: 42         │                │   │
│  │              │   Conf: 98.5%       │                │   │
│  │              └─────────────────────┘                │   │
│  │                                                      │   │
│  │  YOLO ACTIVO 🟢                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Estado: Esperando análisis...                            │
│  Progreso: [████████████] 100%                            │
│                                                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐     │
│  │ Generar ArUco│ │   Grabar     │ │Capturar y    │     │
│  │              │ │   (Láser)    │ │  Validar     │     │
│  └──────────────┘ └──────────────┘ └──────────────┘     │
│                                                             │
│  ┌──────────────┐ ┌──────────────┐                      │
│  │  ✅ PASS     │ │  ❌ FAIL     │                      │
│  │  (VERDE)     │ │  (ROJO)      │                      │
│  └──────────────┘ └──────────────┘                      │
│                                                             │
│  RECONOCIMIENTO DE PATRÓN ArUco                             │
│  ArUco esperado: [42  ]                                     │
│  Último ArUco leído: #42                                    │
│  Coincidencia patrón: ✅ OK                                 │
│                                                             │
│  ═══════════════════════════════════════════════════════  │
│  [23:45:10] VISIÓN: Detectado ArUco #42                   │
│  [23:45:11] ✓ PATRÓN ArUco OK                             │
│  [23:45:12] RESULT: APPROVED                              │
│  ═══════════════════════════════════════════════════════  │
└─────────────────────────────────────────────────────────────┘

NAVEGACIÓN: [VISIÓN] [BRAZO] [STATS] [SINCRO]
```

---

## 5️⃣ **app-almacen.apk** - Estación Almacén

### Pantalla Principal

```
┌─────────────────────────────────────────────────────────────┐
│  Almacén v6.0              [🔵 BT] [🟢 NET]               │
│  GESTIÓN DE RACKS (3×6 = 18 posiciones)                     │
├─────────────────────────────────────────────────────────────┤
│  🤖 CONTROL DE ROBOT DE PICKING                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Estado: 🟢 READY                                    │   │
│  │  Posición actual: HOME                              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌──────┐ ┌──────┐ ┌──────┐                              │
│  │ HOME │ │READY │ │ABORT │                              │
│  └──────┘ └──────┘ └──────┘                              │
│                                                             │
│  📦 ESTADO DE RACKS                                         │
│                                                             │
│  Nivel 1:                                                  │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐              │
│  │ 01 │ │ 02 │ │ 03 │ │ 04 │ │ 05 │ │ 06 │              │
│  │ 🟢 │ │ 🟢 │ │ ⚪ │ │ 🟢 │ │ ⚪ │ │ 🟢 │              │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘              │
│                                                             │
│  Nivel 2:                                                  │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐              │
│  │ 07 │ │ 08 │ │ 09 │ │ 10 │ │ 11 │ │ 12 │              │
│  │ 🟢 │ │ ⚪ │ │ 🟢 │ │ ⚪ │ │ 🟢 │ │ ⚪ │              │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘              │
│                                                             │
│  Nivel 3:                                                  │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐              │
│  │ 13 │ │ 14 │ │ 15 │ │ 16 │ │ 17 │ │ 18 │              │
│  │ ⚪ │ │ 🟢 │ │ ⚪ │ │ 🟢 │ │ ⚪ │ │ 🟢 │              │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘              │
│                                                             │
│  🟢 = Ocupado    ⚪ = Vacío                                 │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐                      │
│  │  ALMACENAR   │  │  RECUPERAR   │                      │
│  └──────────────┘  └──────────────┘                      │
│                                                             │
│  ═══════════════════════════════════════════════════════  │
│  [23:45:10] STO:07 - Pieza almacenada                     │
│  [23:45:12] R:RUN STORE 07 completado                     │
│  ═══════════════════════════════════════════════════════  │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Resumen Visual

| App | Color Principal | Icono | Pestañas | FAB |
|-----|-----------------|-------|----------|-----|
| **Coordinador** | Azul Industrial | 🧠 | 7 pestañas | Bluetooth |
| **PLC** | Verde | 🚦 | 3 pestañas | Bluetooth |
| **Manufactura** | Naranja | 🤖 | 1 pantalla | Bluetooth |
| **Calidad** | Púrpura | 📷 | 4 pestañas | Bluetooth |
| **Almacén** | Cyan | 📦 | 1 pantalla | Bluetooth |

---

## 🎨 Tema Industrial Común

```
Colores:
├── Primario:    #1565C0 (Azul Industrial)
├── Secundario:  #7B1FA2 (Púrpura)
├── Éxito:       #2E7D32 (Verde)
├── Advertencia: #F57C00 (Naranja)
├── Error:       #C62828 (Rojo)
└── Fondo:       #121212 (Negro Industrial)

Componentes:
├── IndustrialScaffold
├── IndustrialCard
├── IndustrialActionButton
├── IndustrialStatusRow
├── IndustrialTextField
└── BluetoothConnectionFAB
```

---

**Todas las apps comparten el mismo diseño industrial profesional con tema oscuro.**