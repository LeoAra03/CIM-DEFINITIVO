# 🏭 Sistema CIM v6.0 - Control Industrial Distribuido

Bienvenido a la versión definitiva del sistema **Computer Integrated Manufacturing (CIM)**. Este ecosistema está diseñado para coordinar una planta de manufactura flexible mediante una red híbrida de aplicaciones Android y nodos ESP32.

---

## 🛠️ Estructura del Proyecto

El repositorio está organizado bajo principios de **Clean Architecture** y modularización industrial:

- **`app-coordinador`**: El nodo maestro (Hub). Orquestra la red y la seguridad.
- **`app-plc`**: Control de la cinta transportadora y logística de transporte.
- **`app-almacen`**: Gestión inteligente de inventario y racks.
- **`app-manufactura`**: Control de brazo robótico Scorbot y grabado láser CNC.
- **`app-calidad`**: Visión artificial con OpenCV para inspección técnica.
- **`core-network`**: Librería compartida que gestiona TCP, Bluetooth Híbrido y Visión.

---

## 🚀 Guía de Inicio Rápido

### 1. Preparación del Hardware
- Asegúrese de tener un celular con **Android 10+**.
- Conecte sus nodos **ESP32** (Soporta BLE y Bluetooth Clásico).

### 2. Despliegue
- Las APKs finales están en `output-apks/`.
- Instale las aplicaciones usando ADB o el paquete ZIP de entrega.

### 3. Sincronización
1. Inicie **CIM Hub**, presione **START** en la pestaña 'NODOS'.
2. Inicie una estación (ej: **PLC Master**), ingrese la IP del Hub y presione **VINCULAR**.
3. Use el **botón azul flotante** para conectar el hardware físico.

---

## 📚 Documentación Técnica (Manuales)

He preparado una suite de documentación completa para su presentación:

1.  [**Arquitectura y Topología**](docs/manuals/01_ARQUITECTURA_SISTEMA.md)
2.  [**Protocolo de Mensajería CIM**](docs/manuals/02_PROTOCOLO_COMUNICACION_CIM.md)
3.  [**Motor Bluetooth Híbrido (BLE/SPP)**](docs/manuals/03_MOTOR_BLUETOOTH_HIBRIDO.md)
4.  [**Sistema de Visión ArUco/QR**](docs/manuals/04_SISTEMA_VISION_ARTIFICIAL.md)
5.  [**Guía Operativa por Estación**](docs/manuals/05_GUIA_ESTACIONES_TRABAJO.md)
6.  [**Configuración y Despliegue**](docs/manuals/06_DESPLIEGUE_Y_CONFIGURACION.md)

---

## ✅ Certificación de Calidad
- **Conectividad:** Handshake de seguridad de 3 vías verificado.
- **Estabilidad:** Gestión asíncrona de hilos para evitar bloqueos en UI.
- **Visión:** Detección real optimizada a 5-10 FPS para hardware móvil.

---
*Desarrollado con Gemini Pro en Android Studio para el Proyecto de Práctica Profesional.*
