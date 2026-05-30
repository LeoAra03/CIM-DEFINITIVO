# 🏭 Sistema CIM v6.0 - Control Industrial Distribuido

Bienvenido a la versión definitiva del sistema **Computer Integrated Manufacturing (CIM)**. Este ecosistema coordina una planta de manufactura flexible mediante una red híbrida de aplicaciones Android y nodos ESP32.

---

## 🛠️ Estructura del Repositorio

El proyecto está organizado bajo principios de **Clean Architecture**:

- **`app-coordinador`**: Nodo maestro (Hub). Orquestra la red y la seguridad.
- **`app-plc`**: Control de la cinta transportadora y logística.
- **`app-almacen`**: Gestión inteligente de inventario.
- **`app-manufactura`**: Control de robot Scorbot y láser CNC.
- **`app-calidad`**: Visión artificial con OpenCV para inspección.
- **`core-network`**: Librería compartida (TCP, Bluetooth Híbrido, Visión).
- **`firmware`**: Código C++ para los nodos ESP32.

---

## 📦 Binarios y APKs

Debido a que las APKs incluyen **OpenCV nativo** y superan el límite de 100MB de GitHub, se han subido en partes dentro de la carpeta `/binarios_particionados`.

### Cómo reconstruir el paquete completo:
Si deseas obtener el ZIP original con todas las APKs y manuales, ejecuta este comando en PowerShell dentro de la carpeta del proyecto:
```powershell
Get-Content ./binarios_particionados/CIM_V6_PART_* -Raw | Set-Content CIM_V6_ENTREGA_FINAL.zip
```

---

## 📚 Documentación Técnica (Manuales Pro)

La documentación completa se encuentra en la carpeta `docs/logs/` (o `docs/manuals/` si prefieres navegar por los fuentes):

1.  [**Arquitectura y Topología**](docs/manuals/01_ARQUITECTURA_SISTEMA.md)
2.  [**Protocolo de Mensajería CIM**](docs/manuals/02_PROTOCOLO_COMUNICACION_CIM.md)
3.  [**Motor Bluetooth Híbrido (BLE/SPP)**](docs/manuals/03_MOTOR_BLUETOOTH_HIBRIDO.md)
4.  [**Sistema de Visión ArUco/QR**](docs/manuals/04_SISTEMA_VISION_ARTIFICIAL.md)
5.  [**Guía Operativa por Estación**](docs/manuals/05_GUIA_ESTACIONES_TRABAJO.md)
6.  [**Configuración y Despliegue**](docs/manuals/06_DESPLIEGUE_Y_CONFIGURACION.md)

---

## 🚀 Guía de Inicio Rápido

1. **Compilar**: Usa `./gradlew buildAllApks` para generar nuevas versiones.
2. **Instalar**: Usa el script en `docs/logs/Install-CIM.ps1` para desplegar automáticamente en tu celular.
3. **Sincronizar**: Inicia el Hub, activa 'NODOS' y vincula tus estaciones mediante la IP local.

---
*Desarrollado con Gemini Pro en Android Studio para el Proyecto de Práctica Profesional.*
