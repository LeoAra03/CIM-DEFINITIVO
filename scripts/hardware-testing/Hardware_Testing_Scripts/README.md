# 🔧 Hardware Testing Scripts - CIM v6.0

Carpeta con scripts de testing E2E (End-to-End) para validar la funcionalidad completa del sistema CIM v6.0 con hardware real.

## 📋 Contenido

- **e2e_test.sh** - Script Bash para Linux/macOS
- **e2e_test.ps1** - Script PowerShell para Windows (recomendado)
- **README.md** - Este archivo (instrucciones)

---

## ⚙️ Prerequisitos

### Hardware
- **1x Coordinador**: Dispositivo Android o emulador con app `app-coordinador`
- **4x Estaciones**: Dispositivos Android con apps:
  - `app-plc` (Estación PLC)
  - `app-manufactura` (Estación Manufactura)
  - `app-calidad` (Estación QC)
  - `app-almacen` (Estación Storage)
- **5x ESP32 (opcional)**: Con firmware CIM v6.0 flasheado
- **Router WiFi 2.4GHz**: Preferente para TCP (puerto 8888)

### Software
- **Android SDK + ADB** - Conectar dispositivos
- **PowerShell 5.0+** (Windows) o **Bash** (Linux/macOS)
- **APKs compiladas**: `./output-apks/` debe contener 5 APKs

---

## 🚀 Cómo Ejecutar (Windows)

### 1. Preparación Previa

```powershell
# Copiar esta carpeta al PC donde está ADB instalado
cd Hardware_Testing_Scripts

# Verificar que ADB ve los dispositivos
adb devices

# Ejemplo salida:
# List of attached devices
# emulator-5554          device
# 192.168.1.100:5555     device
# 192.168.1.101:5555     device
# 192.168.1.102:5555     device
# 192.168.1.103:5555     device
```

### 2. Instalar APKs en Dispositivos

```powershell
# En la carpeta raíz del proyecto:
cd output-apks

# Instalar coordinador en primer device
adb -s emulator-5554 install app-coordinador.apk

# Instalar estaciones en los otros 4 devices
adb -s 192.168.1.100:5555 install app-plc.apk
adb -s 192.168.1.101:5555 install app-manufactura.apk
adb -s 192.168.1.102:5555 install app-calidad.apk
adb -s 192.168.1.103:5555 install app-almacen.apk
```

### 3. Ejecutar Testing E2E

```powershell
# Navegar a carpeta de scripts
cd Hardware_Testing_Scripts

# Ejecutar script (requiere PowerShell Ejecutor habilitado)
powershell -ExecutionPolicy Bypass -File ./e2e_test.ps1

# O en Linux/macOS:
bash ./e2e_test.sh
```

### 4. Monitorear Logs

```powershell
# Ver log en tiempo real
Get-Content e2e_test_YYYYMMDD_HHMMSS.log -Wait

# O abrir en editor
code e2e_test_YYYYMMDD_HHMMSS.log
```

---

## 📊 Fases de Testing

El script ejecuta **10 fases de validación**:

| Fase | Descripción | Duración |
|------|-------------|----------|
| 1 | Verificación de dispositivos ADB | ~10s |
| 2 | Conectividad BLE (scan + discovery) | ~10s |
| 3 | Handshake (IDENTIFY/IDENTIFIED) | ~10s |
| 4 | Solicitud de permisos (dialogs) | ~10s |
| 5 | Enrutamiento de comandos (BLE→TCP→SPP) | ~5s |
| 6 | Heartbeat y keep-alive (5s interval) | ~20s |
| 7 | TCP broadcaster (CLIENTS cada 2s) | ~10s |
| 8 | Stress test (20 comandos rápidos) | ~10s |
| 9 | Validación de firmware (ver siones) | ~5s |
| 10 | Cleanup y desconexión | ~5s |

**Tiempo total aproximado**: ~95 segundos

---

## ✅ Criterios de PASS

El testing se considera exitoso si:

- ✓ Todos los 5 dispositivos son detectados por ADB
- ✓ 4 estaciones responden IDENTIFIED al coordinador
- ✓ Se otorgan permisos a las 4 estaciones
- ✓ Se enrutan exitosamente 4 comandos (ACK recibido)
- ✓ Se reciben heartbeats cada 5s sin timeout
- ✓ El broadcaster CLIENTS funciona cada 2s
- ✓ Stress test completa sin bloqueos
- ✓ Versiones de firmware validadas
- ✓ Desconexión limpia sin crashes

---

## ❌ Troubleshooting

### "No devices detected"
```powershell
# Verificar que TCP/IP debugging está habilitado en dispositivos
# Settings → Developer Options → ADB over Network

# Reconectar dispositivos
adb connect 192.168.1.100:5555
```

### "Bluetooth not enabled"
```
- Encender Bluetooth en todos los dispositivos
- Verificar permisos de localización (requerido por Android 6+)
```

### "TCP Port 8888 already in use"
```powershell
# Encontrar qué proceso usa puerto 8888
netstat -ano | findstr :8888

# Terminar proceso por PID
taskkill /PID <PID> /F

# O cambiar puerto en el script (editar TCP_PORT = 9999)
```

### "Permission denied (13)" en Linux
```bash
# Hacer script ejecutable
chmod +x e2e_test.sh
./e2e_test.sh
```

---

## 📝 Logs

Cada ejecución genera un log con timestamp:
```
e2e_test_20260519_143022.log
```

Contiene:
- Timestamp de cada evento
- Nivel (INFO/SUCCESS/WARNING/ERROR)
- Detalles de cada fase
- Resultado final (.PASS/.FAIL)

---

## 🔍 Validación Manual (Alternativa)

Si los scripts no funcionan, ejecutar manualmente:

```powershell
# 1. Verificar dispositivos
adb devices

# 2. Ver logs del coordinador
adb -s <DEVICE_ID> logcat | findstr "CIM\|BLE\|TCP"

# 3. Ver logs de estación
adb -s <DEVICE_ID> logcat | findstr "CIM\|IDENTIFY\|ACK"

# 4. Enviar comando de test
adb -s <DEVICE_ID> shell am broadcast -a "com.example.test.TEST_CMD" \
  --es "cmd" "MOTOR:START"
```

---

## 📦 Próximos Pasos

1. **Compilación de APKs**: `./gradlew buildAllApks` en raíz del proyecto
2. **Instalar APKs**: Usar scripts de instalación arriba
3. **Ejecutar testing**: `powershell -ExecutionPolicy Bypass -File e2e_test.ps1`
4. **Validación exitosa**: Verificar que todos los PASS en el log
5. **Correcciones**: Si hay FAILs, consultar troubleshooting

---

## 📞 Soporte

Para problemas, revisar:
- Manual/GUIA_DEBUGGING_BLUETOOTH.md
- Manual/MEMORIA_TECNICA_INDUSTRIAL_CIM_v5.md  
- Logs de compilación en `build/`

---

**Última actualización**: 2026-05-19
**Versión**: CIM v6.0

