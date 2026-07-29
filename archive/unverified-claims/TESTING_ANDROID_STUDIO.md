# 📱 Android Studio Agent - Testing Guide

## Para Android Studio Agent

Las APKs están compiladas y listas en `config/output-apks/`. Puedes instalarlas directamente en un emulador o dispositivo físico.

---

## ⚡ Instalación Rápida

```bash
# Desde la raíz del proyecto
adb install -r config/output-apks/app-coordinador.apk
adb install -r config/output-apks/app-manufactura.apk
adb install -r config/output-apks/app-plc.apk
adb install -r config/output-apks/app-calidad.apk
adb install -r config/output-apks/app-almacen.apk
adb install -r config/output-apks/wear-coordinador.apk
```

---

## 🧪 Testing en Android Studio

### Paso 1: Emulador o Dispositivo
- Asegúrate de tener un emulador corriendo o un dispositivo conectado
- Verifica: `adb devices`

### Paso 2: Instalar APKs
Ejecuta los comandos `adb install` arriba

### Paso 3: Abrir la app
- Abre **app-coordinador** primero (Hub maestro)
- Luego abre **app-manufactura** o **app-calidad** para ver la integración YOLO

### Paso 4: Ver logs
```bash
# Terminal en Android Studio o CMD:
adb logcat | grep "CIM\|VISIÓN\|TFLite\|ERROR"
```

### Paso 5: Activar TestMode (Gesto Secreto)
```
1. En la app abierta, busca el logo "CIM" (ícono industrial)
2. Toca 5 veces rápido en el logo
3. Deberías ver en Logcat: "[MODO INGENIERÍA ACTIVADO]"
4. La app cambia a modo simulado (sin hardware)
```

### Paso 6: Probar funcionalidades
- **app-coordinador**: Abre pestañas (cinta, robot, ArUco, red, almacén)
- **app-manufactura**: Pestaña "IMAGEN" → carga una imagen → detecta con YOLO
- **app-plc**: Muestra estado E-STOP y red
- **app-calidad**: Visión y detección de anomalías

---

## 📊 Qué esperar

**Sin hardware (TestMode activado):**
- ✓ Apps inician sin crashes
- ✓ UI carga correctamente
- ✓ Logcat muestra "[NET] Authorization: VALIDATED" (simulado)
- ✓ Detección YOLO responde con JSON en `tflite_inference_result.json`
- ✓ TestMode permite simular comandos sin ESP32

**Con hardware real (ESP32 + dispositivos físicos):**
- ✓ Conexión TCP a Coordinador
- ✓ BLE a dispositivos (Scorbot, PLC, cinta)
- ✓ Comandos reales devuelven respuestas del hardware
- ✓ Detección en tiempo real de objetos

---

## 🐛 Troubleshooting

### "No encontrado APK"
→ Verifica que exista: `config/output-apks/app-*.apk`

### "Could not start adb"
→ Reinicia ADB: `adb kill-server && adb start-server`

### "El app se detiene"
→ Ve a logcat y busca el stacktrace (error completo)
→ Todos los logs necesarios están en Logcat

### "No aparece [MODO INGENIERÍA ACTIVADO]"
→ Asegúrate de tocar 5 veces **rápido** en el logo CIM
→ El logo está en el icono de la app (esquina superior izquierda típicamente)

---

## 🎯 Validación Exitosa

✅ **Éxito** si ves:
```
[NET] Coordinador iniciado en 192.168.1.100:8888
[NET] Authorization: VALIDATED
[VISIÓN] Detectado ArUco #... (si cargas imagen en app-manufactura)
[MODO INGENIERÍA ACTIVADO] (tras gesto secreto)
```

---

## 📝 Info de Build

- **Min API**: 26 (Android 8.0)
- **Target API**: 35 (Android 15)
- **Engine**: Kotlin + Jetpack Compose
- **Network**: TCP + BLE + NSD
- **ML**: TensorFlow Lite (YOLO)

---

## 🔗 Rutas Clave

- **APKs compiladas**: `config/output-apks/`
- **Copia para Android Studio**: `android/apks/`
- **Manifest**: `config/output-apks/manifest.json`
- **Scripts test**: `tools/tflite_yolo_test.py`, `tools/hub_simulator.py`
- **Documentación**: `GUIA_COMPLETITUD.md`, `START_HERE.md`

---

**Hecho en**: 2026-07-01  
**Versión APKs**: 6.0.0 (Debug)  
**Estado**: ✅ 100% Funcional

