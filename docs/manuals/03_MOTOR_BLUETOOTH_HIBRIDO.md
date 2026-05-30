# Motor Bluetooth Híbrido v6.0

Este módulo es el núcleo de la conectividad física con las estaciones. Resuelve el problema de fragmentación de hardware permitiendo que una misma interfaz controle dispositivos BLE y Clásicos simultáneamente.

---

## 1. Funcionamiento del Escáner

El botón de búsqueda en las aplicaciones dispara un escaneo dual:

- **BLE Scan:** Busca dispositivos que anuncien el servicio de UART de Nordic (`6E400001-...`).
- **Classic Discovery:** Inicia el proceso estándar de Android para encontrar dispositivos SPP tradicionales.

**Filtrado Industrial:** El motor solo muestra dispositivos cuyos nombres contengan "ESP32", "CIM", o "NODO", eliminando el ruido de otros dispositivos (ej: relojes inteligentes, otros teléfonos).

---

## 2. Gestión de Conexiones

El `BluetoothHardwareManager` mantiene un mapa de conexiones activas:

```kotlin
private val connectedGatts = ConcurrentHashMap<String, BluetoothGatt>()
private val connectedSockets = ConcurrentHashMap<String, ClassicConnectedThread>()
```

### Sincronización de Estado (Anti-Ghost UI)
Se utiliza un `StateFlow` reactivo que emite el mapa de estados en cada cambio. Esto permite que los componentes de la interfaz (como los botones de acción) se habiliten o deshabiliten automáticamente en milisegundos tras una desconexión física.

---

## 3. Fragmentación MTU (BLE)

Debido a que Bluetooth Low Energy tiene un límite nativo de ~20 bytes por paquete, los mensajes CIM (que pueden medir >150 bytes por el ID y SessionID) son fragmentados automáticamente por el manager:

1. El mensaje se divide en "chunks" de 20 bytes.
2. Se envían secuencialmente con un retraso de 20ms entre ellos.
3. El ESP32 reconstruye el mensaje hasta detectar el carácter terminador (`\n` o `*`).

---

## 4. Solución a Desconexiones

El sistema implementa un **Auto-Reconnect** con "Exponential Backoff":
- Intento 1: 1 segundo después.
- Intento 2: 2 segundos después.
- ... hasta un máximo de 30 segundos.
Esto asegura que si una estación se apaga momentáneamente, la app recuperará el control sin intervención humana.
