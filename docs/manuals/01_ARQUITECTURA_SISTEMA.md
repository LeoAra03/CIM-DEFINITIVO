# Arquitectura del Sistema CIM v6.0

Este documento detalla la estructura de alto nivel del sistema **CIM (Computer Integrated Manufacturing)**, diseñado para la coordinación distribuida de estaciones industriales mediante tecnologías Android y microcontroladores ESP32.

---

## 1. Visión General

El sistema CIM v6.0 está basado en una topología **Hub-and-Spoke** (Maestro-Esclavo) de alta disponibilidad. Su objetivo es automatizar y monitorear una línea de producción completa, incluyendo transporte, almacenamiento, manufactura y control de calidad.

### Componentes Principales
- **CIM Hub (Coordinador):** El cerebro central que orquestra todas las operaciones. Gestiona la base de datos de dispositivos, la seguridad y la lógica de automatización.
- **Estaciones de Trabajo (Workstations):** Apps Android dedicadas para cada proceso físico (PLC, Almacén, Manufactura, Calidad).
- **Nodos de Hardware (ESP32):** Dispositivos embebidos que interactúan directamente con actuadores y sensores físicos.

---

## 2. Diagrama de Capas

El proyecto está organizado en una arquitectura de software modular:

### A. Capa de Aplicación (Apps)
Cinco aplicaciones independientes desarrolladas en **Jetpack Compose**:
- `app-coordinador`: Interfaz de comando global.
- `app-plc`: Control de cinta transportadora.
- `app-almacen`: Gestión de racks y stock.
- `app-manufactura`: Control de Robot Scorbot y Grabado Láser.
- `app-calidad`: Visión artificial y validación de piezas.

### B. Capa de Red (`core-network`)
Librería compartida que encapsula toda la complejidad técnica:
- **TCP Engine:** Servidor y clientes para comunicación por Wi-Fi.
- **Bluetooth Hybrid Engine:** Soporte unificado para BLE (Low Energy) y SPP (Classic).
- **Security Manager:** Sistema de autorización manual y automática por dirección MAC.
- **Industrial Vision:** Analizador integrado de OpenCV y ML Kit.

### C. Capa de Hardware (Firmware)
Scripts desarrollados en C++ (Arduino/PlatformIO) para ESP32:
- Handshake automático de identificación.
- Ejecución de comandos CIM seriales.
- Reporte de eventos de sensores en tiempo real.

---

## 3. Flujo de Control E2E

1. **Vínculo:** El ESP32 se conecta por Bluetooth a su Estación (App Android).
2. **Handshake:** El ESP32 envía un comando `IDENTIFY`. La Estación solicita permiso al Coordinador por TCP.
3. **Autorización:** El Coordinador valida la MAC del dispositivo y autoriza el canal.
4. **Operación:** El Coordinador envía una secuencia de comandos (ej: `DELIVER 1->5`).
5. **Ejecución:** La App de Estación recibe el comando por TCP y lo traduce a Bluetooth para el ESP32.
6. **Feedback:** El ESP32 confirma la acción y la Estación actualiza al Coordinador.
