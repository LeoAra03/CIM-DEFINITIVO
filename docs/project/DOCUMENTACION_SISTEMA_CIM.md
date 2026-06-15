# 🏭 SISTEMA INDUSTRIAL CIM v7.0 - MANUAL DE IMPLEMENTACIÓN

Este documento describe la arquitectura, despliegue y operación del sistema de manufactura flexible (CIM).

## 📱 APLICACIONES ANDROID (APKs)

El sistema consta de 5 estaciones principales desarrolladas en Kotlin/Compose:

1.  **`app-coordinador`**: El cerebro de la red. Autoriza conexiones y supervisa el tráfico de datos.
2.  **`app-manufactura`**: Controla el **Robot Scorbot** y el **Grabado Láser CNC**.
3.  **`app-calidad`**: Realiza inspección por visión artificial (ArUco/YOLO) y controla un **Robot Scorbot** para descartes.
4.  **`app-almacen`**: Gestiona la matriz de racks y utiliza un **Robot Scorbot** para el picking de piezas.
5.  **`app-plc`**: Controla la **Cinta Transportadora** y los sensores de proximidad.

---

## 🔧 FIRMWARE ARDUINO (WEMOS D1 R32)

Se han estandarizado los scripts en la carpeta `/firmware/v7_standard/`:

*   **`CIM_SCORBOT_FIRMWARE.ino`**: Cargar en las placas destinadas a los brazos robóticos.
    *   *Nota:* Cambiar `DEVICE_NAME` según la estación (MAN, CAL, ALM).
*   **`CIM_PLC_FIRMWARE.ino`**: Cargar en la placa que controla los relés de la cinta y los sensores.

### Configuración BLE
Todos los dispositivos usan el protocolo **CIM over BLE**:
*   **Service UUID:** `6E400001-B5A3-F393-E0A9-E50E24DCCA9E`
*   **TX (Notify):** `6E400003-...`
*   **RX (Write):** `6E400002-...`

---

## 📡 PROTOCOLO DE RED

La comunicación es híbrida:
1.  **Bluetooth (Local):** Entre la App de la estación y su hardware (ESP32).
2.  **TCP/Wi-Fi (Red):** Entre las Apps de las estaciones y el Coordinador Central.

### Formato de Mensaje CIM
`ID|TIMESTAMP|SOURCE_MAC|SOURCE_APP|DEST_MAC|DEST_APP|CMD|PRIORITY|SESSION|PAYLOAD`

---

## 🚀 PASOS PARA EL DESPLIEGUE

1.  **Limpieza:** Se han eliminado todos los archivos `.md` antiguos para evitar confusión.
2.  **Compilación:** Ejecutar `./gradlew assembleDebug` para generar las 5 APKs.
3.  **Hardware:** Cargar los archivos `.ino` en las Wemos R32 usando el Arduino IDE.
4.  **Vinculación:**
    *   Abrir Coordinador y anotar la IP de la red local.
    *   Abrir cada App de estación, ingresar la IP del Coordinador y pulsar "Sincronizar".
    *   Aprobar las solicitudes de permiso en la pantalla del Coordinador.
    *   Conectar el Bluetooth en cada estación para habilitar el control físico.

---

## 📂 UBICACIÓN DE BINARIOS

*   **APKs Finales:** `/output-apks/v7_final/`
*   **Firmware ESP32:** `/firmware/v7_standard/`
