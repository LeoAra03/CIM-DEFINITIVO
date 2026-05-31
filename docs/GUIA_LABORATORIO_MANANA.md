---
title: "Guía de Laboratorio — CIM v6.0 (Sesión de mañana)"
author: "Leonardo Araya Labarca — IECI — UBB"
date: "2026-05-31"
version: "6.0.0"
lang: es
css: styles/industrial_pdf.css
print_background: true
---

# Guía de laboratorio — CIM v6.0

**Estudiante:** Leonardo Araya Labarca · **RUT:** 21.290.314-0 · **Carrera:** IECI · **Universidad del Biobío**  
**Versión sistema:** 6.0.0 · **Fecha sesión:** 31 de mayo de 2026

> Checklist imprimible para demostración en laboratorio. Orden estricto: **ESP32 → APKs → Hub → SINCRO → Bluetooth → prueba funcional**.

---

## 1. Qué traer (checklist material)

| # | Ítem | Cantidad | Notas |
|---|------|----------|-------|
| ☐ | Laptop Windows con repo `Practica_2` | 1 | JDK 17, Android SDK, ADB, PlatformIO (`pio`) |
| ☐ | Cable USB **datos** (no solo carga) | 2+ | CP210x o CH340 para ESP32 |
| ☐ | ESP32 DevKit (WROOM-32 o S3) | 1–3 | Flasheados o listos para flash |
| ☐ | Teléfonos/tablets Android **API 29+** | 5 | Mín. 4 GB RAM; 1 tablet = coordinador |
| ☐ | Router Wi‑Fi 2.4 GHz (sin aislamiento AP) | 1 | Todas las apps en la **misma subred** |
| ☐ | Cargadores y regletas | — | Sesión larga (~2 h) |
| ☐ | Etiquetas con IP del coordinador | 1 | Escribir IP tras iniciar Hub |
| ☐ | Marcadores ArUco impresos (DICT_4X4_50) | 3–5 | Para estación Calidad |
| ☐ | Copia local `output-apks/*.apk` | 5 APKs | Por si falla compilación en sitio |
| ☐ | `output-apks/cim_esp32_firmware_v6.bin` | 1 | Firmware v6 listo |
| ☐ | Esta guía impresa o en PDF | 1 | `docs/GUIA_LABORATORIO_MANANA.md` |

**Software en laptop (verificar antes de salir):**

```powershell
java -version          # 17+
adb version
pio --version          # PlatformIO 6.x
.\gradlew --version    # Gradle 9.3.1
```

---

## 2. Mapa físico de colocación (planta piloto)

Vista recomendada desde la entrada del laboratorio:

```
                    [ PIZARRA / PROYECTOR ]
    ┌─────────────────────────────────────────────────────────┐
    │                                                         │
    │   [COORDINADOR]          [ROUTER Wi‑Fi]                 │
    │   Tablet central         (centro, altura media)         │
    │   TCP :8888                                               │
    │                                                         │
    │   [PLC + ESP32 cinta]     [MANUFACTURA + ESP32 robot]   │
    │   Izquierda               Derecha fondo                 │
    │                                                         │
    │   [CALIDAD + cámara]      [ALMACÉN + ESP32 rack]        │
    │   Centro-izquierda        Centro-derecha                │
    │                                                         │
    │   [LAPTOP flash/monitor]  — cable corto a ESP32 #1     │
    └─────────────────────────────────────────────────────────┘
```

| Zona | Dispositivo | Rol |
|------|-------------|-----|
| Centro frontal | Tablet coordinador | Hub TCP, autorización MAC, lista de nodos |
| Izquierda | Celular PLC + ESP32 | Cinta / sensores |
| Derecha fondo | Celular manufactura + ESP32 | Robot / láser (comandos `R:` / `L:`) |
| Centro | Celular calidad + cámara | ArUco / QR |
| Derecha | Celular almacén + ESP32 | Posiciones `STO:n` |

**Regla de oro:** el coordinador y las cinco apps deben ver la **misma red Wi‑Fi** (sin “aislamiento de cliente” en el router).

---

## 3. Orden paso a paso (cronograma sugerido)

| Hora relativa | Paso | Acción |
|---------------|------|--------|
| T+0 min | **A** | Encender router; conectar laptop y coordinador |
| T+5 min | **B** | Flashear ESP32(s) — sección 4 |
| T+15 min | **C** | Instalar 5 APKs — sección 5 |
| T+25 min | **D** | Iniciar Hub + anotar IP — sección 6 |
| T+35 min | **E** | SINCRO en las 4 estaciones — sección 7 |
| T+45 min | **F** | Bluetooth: escanear, conectar, autorizar — sección 8 |
| T+60 min | **G** | Prueba funcional mínima — sección 9 |
| T+90 min | **H** | Demo visión / secuencia completa (opcional) |

---

## 4. Flash ESP32 (firmware CIM v6)

### 4.1 Pre-vuelo

- [ ] ESP32 conectado por USB; driver CP210x/CH340 instalado
- [ ] Binario presente: `output-apks/cim_esp32_firmware_v6.bin`
- [ ] Ninguna app Android escaneando BT aún (evita interferencia durante primer boot)

### 4.2 Opción A — Script automatizado (recomendado)

```powershell
cd "c:\Users\Leo\Desktop\Test Practica2\Practica_2"
.\scripts\hardware-testing\flash_and_monitor_esp32.ps1
# Si falla detección de puerto:
.\scripts\hardware-testing\flash_and_monitor_esp32.ps1 -Port COM3
```

### 4.3 Opción B — PlatformIO manual

```powershell
cd firmware\Firmware_Support
pio run -t upload
pio device monitor -b 115200
```

### 4.4 Salida esperada en monitor (115200 baud)

```
[CIM-ESP32] Boot v6.0
[CIM-ESP32] BLE UART + SPP ready
[CIM-ESP32] Waiting IDENTIFY...
```

- [ ] Mensaje de boot visible
- [ ] Sin bucle de reinicio (brownout → revisar cable/alimentación)

---

## 5. Instalación APKs (orden obligatorio)

| Orden | APK | Package | Dispositivo físico |
|-------|-----|---------|-------------------|
| **1** | `app-coordinador.apk` | `com.industria.coordinacion` | Tablet central |
| 2 | `app-plc.apk` | `com.industria.plc` | Estación cinta |
| 3 | `app-manufactura.apk` | `com.industria.manufactura` | Celda robot/láser |
| 4 | `app-calidad.apk` | `com.industria.calidad` | QC con cámara |
| 5 | `app-almacen.apk` | `com.industria.almacenamiento` | Almacén |

```powershell
# Desde raíz del repo
.\docs\logs\Install-CIM.ps1

# O manual por ADB
adb devices
adb install -r output-apks\app-coordinador.apk
adb install -r output-apks\app-plc.apk
adb install -r output-apks\app-manufactura.apk
adb install -r output-apks\app-calidad.apk
adb install -r output-apks\app-almacen.apk
```

**Permisos en cada dispositivo (Android 12+):**

- [ ] Bluetooth (incl. dispositivos cercanos)
- [ ] Ubicación (requerido para escaneo BLE)
- [ ] Cámara (solo `app-calidad`)

---

## 6. Hub — Servidor coordinador (TCP :8888)

En **app-coordinador**:

1. [ ] Abrir app → conceder permisos
2. [ ] Pestaña **Nodos** / Hub → pulsar **Iniciar Servidor Hub** (START)
3. [ ] Anotar **IP local** mostrada en pantalla (ej. `192.168.1.105`)
4. [ ] Escribir IP en etiqueta visible para estaciones
5. [ ] Verificar que el puerto **8888** no esté bloqueado por firewall Windows del laptop (si el hub corre en emulador, usar IP del host)

**Firewall (solo si estaciones no conectan):**

```powershell
# Ejecutar como administrador si es necesario
New-NetFirewallRule -DisplayName "CIM Hub 8888" -Direction Inbound -Protocol TCP -LocalPort 8888 -Action Allow
```

---

## 7. SINCRO — Vincular estaciones al Hub

Repetir en **app-plc**, **app-manufactura**, **app-calidad**, **app-almacen**:

1. [ ] Pestaña **SINCRO**
2. [ ] Ingresar IP del coordinador (la anotada en paso 6)
3. [ ] Pulsar **Vincular al Hub** / **VINCULAR**
4. [ ] Estado: conectado / ONLINE en coordinador (lista de clientes)

| Estación | Contraseña demo (si aplica) | Verificación en Hub |
|----------|----------------------------|---------------------|
| PLC | Según configuración local | Solicitud de permiso visible |
| Manufactura | idem | MAC registrada |
| Calidad | idem | MAC + tipo CALIDAD |
| Almacén | idem | MAC + tipo ALMACEN |

En coordinador: [ ] **Autorizar** cada MAC que aparezca en pendientes (o modo AUTO si está habilitado para demo).

---

## 8. Bluetooth — Escaneo, conexión, multiconexión

Por cada estación con ESP32 asignado:

1. [ ] Pulsar **FAB azul** (Bluetooth) en la app de estación
2. [ ] Iniciar escaneo → filtrar nombres `ESP32`, `CIM`, `NODO`
3. [ ] Seleccionar el ESP32 de esa mesa → **Conectar**
4. [ ] Esperar handshake `IDENTIFY|CIM-ST-...`
5. [ ] En coordinador: confirmar **AUTORIZADO** para esa MAC
6. [ ] En estación: botones de hardware **habilitados** (no grisados)

**Multiconexión (2+ ESP32):**

- Cada MAC mantiene su propio GATT; usar siempre acciones dirigidas por dispositivo en UI.
- Si un comando no llega: verificar que no se use el método legacy `send(cmd)` sin MAC.

**SPP fallback:** si BLE falla, emparejar ESP32 desde Ajustes Android → Bluetooth clásico, luego reconectar desde la app.

---

## 9. Prueba funcional mínima (15 minutos)

| # | Prueba | Pasos | Éxito si… |
|---|--------|-------|-----------|
| 1 | Heartbeat TCP | Hub abierto, estación vinculada | Nodo ONLINE en lista |
| 2 | Comando PLC | En Hub o PLC: `DELIVER` / mover cinta | Log ACK en terminal; ESP32 recibe por BT |
| 3 | Almacén | `STO:3` o UI celda 3 | ACK en monitor serial ESP32 |
| 4 | Manufactura | `R:HOME` o botón HOME | Respuesta `ACK\|OK` |
| 5 | Calidad | Apuntar cámara a ArUco | Recuadro verde / ID detectado |
| 6 | Desconexión | Apagar BT del teléfono 5 s y encender | Reconexión automática ≤ 30 s |

**Modo demo sin ESP32:** botones **Simular *** en cada app generan log local (útil si falla hardware).

---

## 10. Troubleshooting rápido (laboratorio)

| Síntoma | Causa probable | Solución inmediata |
|---------|----------------|-------------------|
| `pio` no encontrado | PlatformIO no en PATH | `pip install platformio` |
| Upload timeout ESP32 | Puerto COM incorrecto | `-Port COMx` en script flash |
| Estación no vincula | IP mal escrita / otra subred | `ipconfig` en coordinador; ping desde celular |
| Puerto 8888 en uso | Hub ya corriendo o otra app | Cerrar app y reiniciar coordinador |
| BLE no aparece | ESP32 sin flash / permisos | Re-flash; ubicación + BT concedidos |
| APK no instala | Espacio o ABI | Liberar espacio; `adb install -r` |
| OpenCV crash calidad | Permiso cámara denegado | Ajustes → App → Cámara → Permitir |
| ArUco no detecta | Poca luz / marcador borroso | > 300 lux; imprimir marcador DICT_4X4_50 |
| Comando no ejecuta | MAC no autorizada | Autorizar en Hub antes de EXECUTE |
| APK enorme (~170 MB) | OpenCV nativo incluido | Normal en debug; usar `output-apks/` del repo |

**Recompilar en sitio (último recurso, ~8–10 min):**

```powershell
.\gradlew testAllModules buildAllApks
```

---

## 11. Cierre de sesión (checklist salida)

- [ ] Captura de pantalla: Hub con 4 estaciones ONLINE
- [ ] Foto o log: monitor serial ESP32 con ACK
- [ ] (Opcional) Exportar log coordinador desde terminal integrado
- [ ] Apagar ESP32 y regletas en orden inverso
- [ ] Documento maestro de entrega: `docs/ENTREGA_FINAL_LEONARDO_ARAYA.md`

---

## 12. Referencias rápidas

| Recurso | Ruta |
|---------|------|
| Documento maestro entrega | `docs/ENTREGA_FINAL_LEONARDO_ARAYA.md` |
| APKs | `output-apks/` |
| Firmware bin | `output-apks/cim_esp32_firmware_v6.bin` |
| Matriz 30 tests | `docs/TEST_MATRIX.md` |
| Informe % funcional | `docs/INFORME_FUNCIONALIDAD.md` |

---

*Guía de laboratorio CIM v6.0 — Leonardo Araya Labarca — UBB IECI — 2026-05-31*
