# Despliegue y Configuración

> **Documento consolidado:** Ver sección 10 en [`../ENTREGA_FINAL_LEONARDO_ARAYA.md`](../ENTREGA_FINAL_LEONARDO_ARAYA.md). Laboratorio: [`../GUIA_LABORATORIO_MANANA.md`](../GUIA_LABORATORIO_MANANA.md).

Siga estos pasos para poner en marcha el sistema CIM v6.0 en un entorno real.

---

## 1. Requisitos de Hardware

- **Dispositivos Android:** Mínimo Android 10 (API 29). Recomendado: Pantalla 1080p y 4GB RAM.
- **Nodos ESP32:** Módulos ESP32-WROOM-32 o ESP32-S3.
- **Red:** Wi-Fi local estable (no requiere internet).

---

## 2. Instalación de Aplicaciones

Las APKs actualizadas se encuentran en la carpeta `output-apks/`.

### Instalación mediante ADB:
```powershell
./gradlew buildAllApks  # Solo si desea recompilar
adb install -r output-apks/app-coordinador.apk
adb install -r output-apks/app-plc.apk
adb install -r output-apks/app-almacen.apk
adb install -r output-apks/app-calidad.apk
adb install -r output-apks/app-manufactura.apk
```

---

## 3. Configuración Inicial

1. **Iniciar el Coordinador:** Abra la App Coordinador y pulse **START** en la pestaña de Nodos. Anote la IP que se muestra.
2. **Configurar Estaciones:**
   - Abra cada app de estación (ej: PLC).
   - Vaya a la pestaña **SINCRO**.
   - Ingrese la IP del Coordinador.
   - Pulse **VINCULAR**.
3. **Conectar Hardware:**
   - Pulse el botón flotante azul de Bluetooth.
   - Seleccione su nodo ESP32.
   - Verifique en el Coordinador que aparezca la solicitud de permiso.

---

## 4. Troubleshooting Común

- **Error de Puerto 8888:** Asegúrese de que ninguna otra app de servidor esté corriendo. Reinicie el Coordinador si es necesario.
- **No se ven ArUcos:** Limpie el lente de la cámara y asegúrese de que haya iluminación adecuada (> 300 lux).
- **Desconexión Bluetooth:** Verifique la alimentación del ESP32. El sistema intentará reconectar automáticamente por 30 segundos.
