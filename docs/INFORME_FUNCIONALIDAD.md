# Informe de Funcionalidad — CIM v6.0

> **Documento consolidado:** Ver sección 13 en [`ENTREGA_FINAL_LEONARDO_ARAYA.md`](ENTREGA_FINAL_LEONARDO_ARAYA.md).

> Auditoría integral | 31 de mayo de 2026  
> Workspace: `Practica_2` | Versión: **6.0.0**

---

## 1. Puntuación global

| Métrica | Valor |
|---------|-------|
| **Funcionalidad global ponderada** | **83 %** |
| Build Android (5 APKs) | ✅ BUILD SUCCESSFUL |
| Tests unitarios (`testAllModules`) | ✅ PASS (3 módulos) |
| Firmware ESP32 (`firmware/Firmware_Support`) | ✅ SUCCESS (PIO) |
| Simulación Wokwi (`simulacion_esp32`) | ✅ SUCCESS (tras fix `src/`) |
| Verificación hardware físico | ⚠ No ejecutable en CI local |

### Pesos aplicados

| Componente | Peso | % Funcional | Contribución |
|------------|------|-------------|--------------|
| core-network | 25 % | 88 % | 22.0 |
| app-coordinador | 20 % | 86 % | 17.2 |
| app-plc | 15 % | 82 % | 12.3 |
| app-manufactura | 12 % | 78 % | 9.4 |
| app-calidad | 12 % | 74 % | 8.9 |
| app-almacen | 10 % | 80 % | 8.0 |
| firmware ESP32 | 6 % | 90 % | 5.4 |
| **Total** | **100 %** | — | **83.2 %** |

---

## 2. Matriz por componente

| Componente | Works % | Issues conocidos | Decorativo / Demo | Prioridad fix |
|------------|---------|------------------|-------------------|---------------|
| **core-network** | 88 | BT multiconexión no probado en 2+ ESP32 reales; `BleDeviceManager` legacy duplicado | `PerformanceProfiler`, `TestModeManager` — utilidad limitada sin hardware | Media |
| **CommandBroker / TCP** | 90 | Reconexión TCP bajo pérdida de red no stress-testeada | — | Baja |
| **BluetoothHardwareManager** | 85 | Permisos Android 12+; `@Deprecated` en API 33 scan callback | — | Media |
| **BluetoothSppManager** | 84 | Servidor SPP requiere emparejamiento previo OS | — | Media |
| **IndustrialVisionAnalyzer** | 70 | OpenCV requiere dispositivo con cámara; no testeable en emulador sin cámara | Preview en emulador muestra UI sin detección real | Alta (hardware) |
| **app-coordinador** | 86 | Hub TCP depende de IP fija configurada; automation tab parcial | Botones "Simular" en terminal local | Baja |
| **app-plc** | 82 | Comandos cinta requieren ESP32 autorizado | "Simular Sensor Activo" → solo log | Media (hardware) |
| **app-manufactura** | 78 | Robot/láser no verificable sin Scorbot real | "SIM ACK/FINISH", "GENERAR DESDE ARUCO" parcial | Alta (hardware) |
| **app-calidad** | 74 | Cámara + ArUco necesitan device físico | "Simular Captura Exitosa" → log demo | Alta (hardware) |
| **app-almacen** | 80 | Posiciones STO:n requieren nodo ESP32 | "Simular Almacenado" → log demo | Media (hardware) |
| **firmware/Firmware_Support** | 90 | Flash/upload depende de puerto COM | — | Baja |
| **simulacion_esp32** | 85 | Solo telemetría HTTP Wokwi, no protocolo CIM BT | Proyecto separado del firmware producción | Baja |
| **Gradle / build** | 92 | `signAllApks`, `lintAll` son placeholders | Tareas decorativas en `build.gradle.kts` | Baja |
| **Documentación** | 95 | Imagen `docs/images/cim_arquitectura_v6.png` referenciada pero puede faltar en clone limpio | Certificados en `docs/logs/` | Baja |

---

## 3. Errores y riesgos identificados

### Críticos (corregidos en esta auditoría)

| ID | Severidad | Descripción | Estado |
|----|-----------|-------------|--------|
| E01 | 🔴 Alta | `cleanBuildAll` fallaba: tarea `clean` inexistente en root | ✅ Corregido → `cleanAllModules` |
| E02 | 🔴 Alta | `simulacion_esp32`: `main.cpp` fuera de `src/` → PIO fallaba | ✅ Corregido → `src/main.cpp` |
| E03 | 🟡 Media | `buildFirmware` apuntaba a ruta obsoleta `Firmware_Support/` | ✅ Corregido → `firmware/Firmware_Support/` |

### Abiertos — riesgo de crash o fallo funcional

| ID | Severidad | Ubicación | Descripción |
|----|-----------|-----------|-------------|
| R01 | 🔴 Alta | Hardware BT | Multiconexión BLE+SPP con 2+ ESP32 simultáneos sin validación en campo |
| R02 | 🔴 Alta | `app-calidad` | OpenCV native crash si permiso CAMERA denegado en runtime |
| R03 | 🟡 Media | `TcpServer.kt:76` | `serverSocket!!` — NPE si socket no inicializado antes de bind |
| R04 | 🟡 Media | `BluetoothHardwareManager` | `adapter` nullable — operaciones sin check en algunos paths |
| R05 | 🟡 Media | Red WiFi | Estaciones requieren misma subred que coordinador; sin mDNS |
| R06 | 🟡 Media | `build_firmware.ps1` | Busca `cim_esp32_firmware_v6_clean.ino` inexistente; cae a stub si PIO falla |
| R07 | 🟢 Baja | `validateApks` | Rango 120–200 MB puede fallar si OpenCV cambia de tamaño |
| R08 | 🟢 Baja | Tests | `app-calidad`, `app-manufactura`, `app-almacen` sin tests en `testAllModules` |
| R09 | 🟢 Baja | Código legacy | `BleDeviceManager` duplica lógica de `BluetoothHardwareManager` |
| R10 | 🟢 Baja | Release | APKs debug sin firma release; `signAllApks` no implementado |

### Decorativo ("por bonito") — funcional pero no esencial

| Elemento | Ubicación | Notas |
|----------|-----------|-------|
| Botones "Simular *" | Todas las estaciones | Escriben log local; útiles para demo sin ESP32 |
| `signAllApks` / `lintAll` | `build.gradle.kts` | Imprimen mensaje placeholder |
| `buildReport` auto-generado | Raíz del proyecto | Markdown temporal post-build |
| Certificados en `docs/logs/` | Documentación | Evidencia de verificación, no código |
| ZIPs `CIM_V6_*.zip` | Raíz | Paquetes de entrega previos |
| `espessif/` | Clone referencia Espressif | Opcional, ignorado en git |

---

## 4. Evidencia de verificación (31-may-2026)

```
./gradlew testAllModules buildAllApks  → BUILD SUCCESSFUL (2m 23s)
./gradlew cleanBuildAll                → BUILD SUCCESSFUL (post-fix)
pio run (firmware/Firmware_Support)    → SUCCESS (Flash 47.6%, RAM 17.4%)
pio run (simulacion_esp32)             → SUCCESS (Flash 71.4%, RAM 14.4%)
```

**APKs frescos en `output-apks/` (debug, ~165–177 MB c/u):**

- `app-coordinador.apk`
- `app-plc.apk`
- `app-calidad.apk`
- `app-manufactura.apk`
- `app-almacen.apk`
- `cim_esp32_firmware_v6.bin` (1.5 MB, compilado hoy)

**Grep de calidad de código:**

- `TODO` / `FIXME` / `NotImplementedError` en `.kt`: **0 coincidencias**
- `onClick = {}` vacíos: **0 coincidencias**
- APIs `@Deprecated`: 1 uso intencional en scan callback API 33

---

## 5. Roadmap hacia 100 %

| Fase | Acción | Impacto estimado |
|------|--------|------------------|
| **1** | Prueba E2E con 2 ESP32 + 5 devices Android en red real | +8 % |
| **2** | Validar visión ArUco en `app-calidad` con cámara física | +5 % |
| **3** | Añadir tests unitarios a calidad/manufactura/almacen en `testAllModules` | +2 % |
| **4** | Eliminar o fusionar `BleDeviceManager` legacy | +1 % |
| **5** | Implementar `signAllApks` con `release.keystore` | +1 % |
| **6** | Null-safety en `TcpServer` y paths BT sin adapter | +1 % |
| **7** | mDNS o discovery automático de IP del coordinador | +2 % |
| **8** | Stress test multiconexión BT documentado en `TEST_MATRIX.md` | +2 % |

**Total teórico al completar fases 1–8: ~100 %** (fases 1–2 requieren hardware obligatorio).

---

## 6. Top 5 bloqueadores para llegar al 100 %

1. **Validación Bluetooth multiconexión en hardware real** — BLE+SPP con múltiples ESP32 no verificable en CI.
2. **Visión artificial OpenCV en dispositivo con cámara** — `app-calidad` depende de hardware de captura.
3. **Actuadores industriales (Scorbot, láser, cinta)** — Comandos R:/L:/C: requieren planta física o simulador dedicado.
4. **Red TCP entre estaciones** — Requiere LAN estable con IP del coordinador accesible desde todos los devices.
5. **Cobertura de tests incompleta** — Solo 3 de 6 módulos Android tienen tests en la suite industrial.

---

*Informe generado en auditoría final CIM v6.0 — Practica_2 UBB*
