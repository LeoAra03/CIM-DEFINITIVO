================================================================================
                    ESTADO REAL DE LAS APKs CIM v6.0
                    (Análisis Honesto - 28 Julio 2026)
================================================================================

## ❌ ERRORES Y LIMITACIONES ACTUALES

### 1. app-calidad (Control de Calidad)
**PROBLEMA CRÍTICO:**
- No tiene dependencias de CameraX, ML Kit, ni OpenCV en build.gradle.kts
- El código de cámara (AndroidView + PreviewView) existe en MainActivity.kt
- Pero NO compilará sin agregar estas dependencias:
  ```kotlin
  implementation("androidx.camera:camera-camera2:1.3.1")
  implementation("androidx.camera:camera-lifecycle:1.3.1")
  implementation("androidx.camera:camera-view:1.3.1")
  implementation("com.google.mlkit:barcode-scanning:17.2.0")
  ```
- ArUcoGenerator.kt existe pero NO está conectado a la cámara real
- No hay modelo YOLO empaquetado

**Estado:** ❌ NO FUNCIONA la cámara real ni detección de IDs

---

### 2. Comunicación Real entre Estaciones

**LO QUE SÍ FUNCIONA:**
- StationClient → TcpClient (conexión TCP)
- Handshake CIM con contraseña
- AuthorizationManager (diálogo de permiso)
- Modo Autónomo (funciona sin red)

**LO QUE NO FUNCIONA (o es simulado):**
- El botón "SIMULAR CICLO" en el Coordinador **solo escribe logs**
- No hay enrutamiento real de comandos a las estaciones conectadas
- No hay lógica que diga: "si está conectado PLC → enviar PLC:START"
- simulateFullCycle() es 100% simulación (no usa CommandBroker real)

---

### 3. Conexión con Firmware ESP32

**SI instalas el firmware en el ESP32:**

**Funciona parcialmente:**
- BLE GATT básico está implementado (GlobalBluetoothManager)
- Puede escanear dispositivos que empiecen con "CIM_"
- Puede enviar comandos simples vía BLE

**NO funciona bien:**
- El protocolo de comandos del firmware (R:HOME, STO:07, etc.) 
  no está completamente sincronizado con lo que envía la app
- No hay manejo robusto de reconexión BLE
- El SPP (Bluetooth clásico) puede fallar en algunos dispositivos

---

### 4. Detección de Pallets y Retención

**Estado:** ❌ Solo simulado

En app-plc:
- Hay un mapa `palletPresent` que se actualiza manualmente
- Hay un simulador de sensor que genera "SENSOR_ACTIVATED|POS:5"
- NO hay código real que lea sensores del ESP32

---

### 5. Generación y Detección de ArUco

**Estado:** ⚠️ Parcialmente implementado

- ArUcoGenerator.kt existe
- Pero la UI de app-calidad no lo usa realmente
- No hay integración con la cámara

---

## ✅ LO QUE SÍ FUNCIONA 100% (Modo Simulado)

| Funcionalidad                    | Estado     | Cómo usarla |
|----------------------------------|------------|-------------|
| Cada app funciona de forma independiente | ✅ Sí     | Activar "Modo Autónomo" |
| Coordinador muestra UI completa  | ✅ Sí     | Abrir app-coordinador |
| Logs en tiempo real              | ✅ Sí     | Ver terminal inferior |
| Botón "SIMULAR CICLO"            | ✅ Sí     | Pestaña EXEC |
| Handshake TCP + autorización     | ✅ Sí     | Pestaña SINCRO |
| Envío de comandos locales        | ✅ Sí     | Botones de cada estación |
| Scripts de simulación PowerShell | ✅ Sí     | 4_SCRIPTS/ |

---

## 🎯 RESPUESTA DIRECTA A TUS PREGUNTAS

### ¿Cada APK funciona de forma independiente?
**SÍ** - Cada app tiene su propio `MainActivity` y puede operar en **Modo Autónomo** sin necesidad del coordinador ni red.

### ¿La de coordinación controla todo el resto?
**NO** (solo simula)  
- El botón "SIMULAR CICLO" genera logs bonitos pero **no envía comandos reales** a las otras apps conectadas.
- Para que controle realmente necesitarías agregar lógica de routing en `simulateFullCycle()`.

### Si le instalo el firmware, ¿puedo conectarme desde la APK al ESP32?
**PARCIALMENTE**  
- El escaneo BLE funciona
- El envío de comandos simples funciona
- Pero los comandos específicos (R:HOME, STO:07, VAL:PASS) pueden no coincidir exactamente con lo que espera el firmware.

### ¿Puedo hacer RUN del robot, generar ArUcos, detectar pallets?
**NO en hardware real**  
**SÍ en simulación** (usando los botones y el script de simulación)

### ¿Puedo usar la cámara y detectar IDs?
**NO** - La app-calidad no tiene las dependencias de cámara ni modelo YOLO.

---

## 🛠️ QUÉ NECESITAS PARA QUE FUNCIONE REALMENTE

### Para app-calidad (Cámara + ArUco):
Agregar al `build.gradle.kts` de app-calidad:
```kotlin
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")
implementation("com.google.mlkit:barcode-scanning:17.2.0")
```

### Para que el Coordinador controle realmente:
Modificar `simulateFullCycle()` para que use `commandBroker` y envíe comandos reales a las estaciones conectadas.

### Para firmware real:
Alinear los comandos que envía la app con los que espera el firmware (revisar ambos lados).

================================================================================
