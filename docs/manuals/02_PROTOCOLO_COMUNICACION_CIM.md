# Protocolo de Comunicación CIM (CimProtocol)

> **Documento consolidado:** Ver sección 6 en [`../ENTREGA_FINAL_LEONARDO_ARAYA.md`](../ENTREGA_FINAL_LEONARDO_ARAYA.md).

El protocolo CIM v6.0 es un estándar de mensajería ligero y robusto diseñado para entornos industriales con alta latencia o pérdida de paquetes ocasional.

---

## 1. Estructura del Mensaje (Transporte)

Todos los mensajes se serializan en una cadena de texto plana con delimitadores de tubería (`|`), lo que facilita el debug y la compatibilidad con microcontroladores.

**Formato:**
`ID|TIMESTAMP|SOURCE_MAC|SOURCE_APP|DEST_MAC|DEST_APP|CMD_TYPE|PRIORITY|SESSION_ID|PAYLOAD`

### Campos:
- **ID:** UUID único para seguimiento de transacciones.
- **SOURCE_MAC / DEST_MAC:** Dirección física del dispositivo (Bluetooth o IP).
- **SOURCE_APP / DEST_APP:** Identificador de la estación (PLC, ALMACEN, etc.).
- **CMD_TYPE:** Tipo de operación (EXECUTE, ACK, IDENTIFY, STATUS).
- **PAYLOAD:** Los datos específicos del comando (ej: `ROBOT_MOVE:X:10`).

---

## 2. El Handshake de Seguridad

Para evitar conexiones no autorizadas, el sistema implementa un handshake de 3 vías:

1. **IDENTIFY:** El nodo (ESP32 o App) se presenta enviando su MAC y tipo de estación.
2. **REQUIRE_PERMISSION:** La app receptora consulta al Coordinador Central si ese dispositivo está en la lista blanca.
3. **IDENTIFIED / PERMISSION_GRANTED:** El Coordinador responde. Si es positivo, el canal de datos se abre; de lo contrario, se cierra el socket/vínculo.

---

## 3. Redes Soportadas

### Wi-Fi (TCP/IP)
- **Puerto:** 8888 (Servidor en el Coordinador).
- **Manejo de Colisiones:** Política `PREFER_NEW` (si un dispositivo reconecta con la misma MAC, se cierra la conexión antigua).
- **Keep-Alive:** Heartbeats cada 10 segundos.

### Bluetooth (Clásico + BLE)
- **SPP (Clásico):** Usado para ESP32 estándar. Provee un flujo de bytes continuo.
- **BLE (Nordic UART):** Usado para módulos de bajo consumo. Implementa fragmentación automática de paquetes (MTU 20 bytes) para mensajes CIM largos.

---

## 4. Tipos de Comandos Industriales

| Comando | Función | Ejemplo de Payload |
| :--- | :--- | :--- |
| `DELIVER` | Movimiento de pieza en cinta | `DELIVER:1:5` |
| `STORE` | Almacenamiento en rack | `STO:12` |
| `SCAN` | Disparo de cámara/visión | `CAM:SNAP` |
| `MOVE` | Control de ejes robot | `R:MOVE:X:100` |
| `STATUS` | Reporte de estado operativo | `STATUS;READY` |
