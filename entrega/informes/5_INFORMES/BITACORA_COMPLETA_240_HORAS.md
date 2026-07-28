# 📅 BITÁCORA COMPLETA DEL PROYECTO CIM v6.0
## Registro de 240 Horas de Trabajo

**Estudiante:** Leonardo Araya  
**Período:** 10 de Marzo de 2026 — 14 de Julio de 2026  
**Total de Horas:** 240 horas  
**Repositorio:** `haloharry973/CIM-DEFINITIVO`

---

## INSTRUCCIONES DE USO DE ESTA BITÁCORA

Esta bitácora está organizada por **semanas** y **días**. Cada entrada incluye:
- **Fecha**
- **Actividad principal**
- **Horas invertidas**
- **Logros del día**
- **Problemas encontrados**
- **Próximos pasos**

---

# SEMANA 1: 10 - 16 de Marzo de 2026 (21 horas)

## Lunes 10 de Marzo — 3 horas

**Actividad:** Reunión inicial con profesor de práctica

**Detalle:**
- Presentación del proyecto CIM
- Definición de alcance: 5 estaciones de manufactura
- Discusión sobre comunicación entre estaciones
- Primeros lineamientos de la arquitectura

**Logros:**
- Alcance del proyecto definido
- 5 estaciones acordadas: Coordinador, PLC, Manufactura, Calidad, Almacén

**Problemas:**
- El profesor no tenía claro si quería hardware real o simulación

**Próximos pasos:**
- Investigar arquitecturas CIM existentes

---

## Martes 11 de Marzo — 6 horas

**Actividad:** Investigación de arquitecturas CIM y protocolos industriales

**Detalle:**
- Estudio de protocolos Modbus, OPC-UA, MQTT industrial
- Análisis de sistemas CIM existentes (Siemens, Rockwell)
- Investigación de comunicación Bluetooth industrial
- Revisión de papers sobre manufactura distribuida

**Logros:**
- Decisión de usar BLE + TCP como comunicación híbrida
- Definición del protocolo CIM v5.1 (inspirado en Modbus pero simplificado)

**Problemas:**
- Ninguno

**Próximos pasos:**
- Diseñar la arquitectura distribuida

---

## Miércoles 12 de Marzo — 5 horas

**Actividad:** Diseño de la arquitectura distribuida

**Detalle:**
- Diagrama de las 5 estaciones
- Definición de comandos por estación
- Diseño del Coordinador como HUB central
- Esquema de autorización de estaciones

**Logros:**
- Arquitectura distribuida documentada
- Primera versión del protocolo CIM

**Problemas:**
- El profesor cuestionó el modelo distribuido (prefería centralizado)

**Próximos pasos:**
- Crear repositorio GitHub

---

## Jueves 13 de Marzo — 4 horas

**Actividad:** Selección de stack tecnológico

**Detalle:**
- Evaluación de Kotlin vs Java
- Jetpack Compose vs XML
- Hilt para inyección de dependencias
- CameraX para la estación de calidad

**Logros:**
- Stack definido: Kotlin + Compose + Hilt + CameraX
- Decisión de usar un solo tema industrial para todas las apps

**Problemas:**
- Ninguno

**Próximos pasos:**
- Crear repositorio

---

## Viernes 14 de Marzo — 2 horas

**Actividad:** Creación del repositorio GitHub

**Detalle:**
- Creación de `haloharry973/CIM-DEFINITIVO`
- Configuración de .gitignore para Android
- Estructura inicial de carpetas

**Logros:**
- Repositorio creado y configurado

**Problemas:**
- Ninguno

**Próximos pasos:**
- Definir estructura de carpetas

---

## Sábado 15 de Marzo — 3 horas

**Actividad:** Estructura inicial del proyecto

**Detalle:**
- Creación de carpetas: `android/`, `esp32/`, `docs/`, `config/`
- Organización según principios de minimalismo
- Primera versión del README.md

**Logros:**
- Estructura del repositorio definida

**Problemas:**
- Ninguno

**Próximos pasos:**
- Iniciar desarrollo del protocolo CIM

---

## Domingo 16 de Marzo — 0 horas (Descanso)

---

# SEMANA 2: 17 - 23 de Marzo de 2026 (28 horas)

## Lunes 17 de Marzo — 5 horas

**Actividad:** Diseño del protocolo CIM v5.1

**Detalle:**
- Formato de mensaje: `ID|TIMESTAMP|SOURCE|DEST|CMD|PRIORITY|SESSION|PAYLOAD`
- Comandos base: `REQ_PERM`, `GRANTED`, `DENIED`, `ABORT`
- Comandos de hardware: `R:HOME`, `PLC:START`, `STO:07`, `VAL:PASS`

**Logros:**
- Protocolo CIM v5.1 documentado

**Problemas:**
- Ninguno

**Próximos pasos:**
- Implementar `CimMessage` y `CimProtocol`

---

## Martes 18 de Marzo — 6 horas

**Actividad:** Implementación de `core-network` (Parte 1)

**Detalle:**
- Creación de `CimMessage.kt`
- Creación de `CimProtocol.kt`
- Implementación de `AppIdentifier.kt`

**Logros:**
- Base del protocolo implementada

**Problemas:**
- Ninguno

**Próximos pasos:**
- Implementar TcpClient y StationClient

---

## Miércoles 19 de Marzo — 4 horas

**Actividad:** Implementación de TcpClient

**Detalle:**
- Cliente TCP con reconexión automática
- Heartbeat cada 10 segundos
- Manejo de errores robusto

**Logros:**
- TcpClient funcional

**Problemas:**
- Reconexión causaba crashes en algunos casos

**Próximos pasos:**
- Implementar StationClient

---

## Jueves 20 de Marzo — 5 horas

**Actividad:** Implementación de StationClient

**Detalle:**
- Wrapper sobre TcpClient
- Handshake CIM automático
- Manejo de estados de autorización

**Logros:**
- StationClient completo

**Problemas:**
- Handshake fallaba si el servidor no respondía rápido

**Próximos pasos:**
- Crear primera versión del Coordinador

---

## Viernes 21 de Marzo — 4 horas

**Actividad:** Primera versión del app-coordinador

**Detalle:**
- Estructura básica de la app
- Servidor TCP en puerto 8888
- UI inicial con pestañas

**Logros:**
- Coordinador compila y ejecuta

**Problemas:**
- UI muy básica

**Próximos pasos:**
- Desarrollar app-plc

---

## Sábado 22 de Marzo — 4 horas

**Actividad:** Inicio de app-plc

**Detalle:**
- Estructura de la app
- Control de cinta (matriz 3×10)
- Comandos PLC:START, PLC:STOP

**Logros:**
- app-plc con control de cinta funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- Agregar modo autónomo

---

## Domingo 23 de Marzo — 0 horas (Descanso)

---

# SEMANA 3: 24 - 30 de Marzo de 2026 (25 horas)

## Lunes 24 de Marzo — 5 horas

**Actividad:** Modo autónomo en app-plc

**Detalle:**
- Switch de "Modo Autónomo"
- Comandos locales sin necesidad de red
- Simulador de sensores

**Logros:**
- app-plc funciona completamente offline

**Problemas:**
- Ninguno

**Próximos pasos:**
- Desarrollar app-manufactura

---

## Martes 25 de Marzo — 4 horas

**Actividad:** Reunión con profesor - Problema de arquitectura

**Detalle:**
- El profesor cuestionó el modelo distribuido
- Prefería un sistema centralizado
- Discusión de 2 horas sobre ventajas/desventajas

**Logros:**
- Se mantuvo el modelo distribuido (decisión final)

**Problemas:**
- Conflicto con el profesor

**Próximos pasos:**
- Continuar desarrollo

---

## Miércoles 26 de Marzo — 6 horas

**Actividad:** Desarrollo de app-manufactura

**Detalle:**
- Control de robot Scorbot
- Comandos R:HOME, R:RUN, R:MOVE
- Control de láser CNC

**Logros:**
- app-manufactura funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- Agregar carga de G-code

---

## Jueves 27 de Marzo — 3 horas

**Actividad:** Carga de G-code en app-manufactura

**Detalle:**
- Selector de archivos G-code
- Envío de G-code al ESP32
- Visualización de progreso

**Logros:**
- G-code funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- Iniciar app-calidad

---

## Viernes 28 de Marzo — 4 horas

**Actividad:** Inicio de app-calidad

**Detalle:**
- Estructura de la app
- UI de visión
- Generador de ArUco

**Logros:**
- ArUcoGenerator.kt implementado

**Problemas:**
- Ninguno

**Próximos pasos:**
- Desarrollar app-almacen

---

## Sábado 29 de Marzo — 3 horas

**Actividad:** Desarrollo de app-almacen

**Detalle:**
- Gestión de racks 3×6 (18 posiciones)
- Comandos STO y RETRIEVE
- Visualización de estado de racks

**Logros:**
- app-almacen funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- Integrar las 5 apps

---

## Domingo 30 de Marzo — 0 horas (Descanso)

---

# SEMANA 4: 31 de Marzo - 6 de Abril de 2026 (22 horas)

## Lunes 31 de Marzo — 5 horas

**Actividad:** Integración de las 5 aplicaciones

**Detalle:**
- Pruebas de compilación simultánea
- Ajustes de dependencias
- Sincronización de versiones

**Logros:**
- Todas las apps compilan

**Problemas:**
- Conflictos de versión de Hilt

**Próximos pasos:**
- Resolver dependencias

---

## Martes 1 de Abril — 4 horas

**Actividad:** Resolución de dependencias Hilt

**Detalle:**
- Actualización a Hilt 2.47
- Sincronización de Compose BOM
- Pruebas de compilación

**Logros:**
- Dependencias estables

**Problemas:**
- Ninguno

**Próximos pasos:**
- Implementar Bluetooth

---

## Miércoles 2 de Abril — 6 horas

**Actividad:** Implementación de BluetoothHardwareManager

**Detalle:**
- BLE GATT completo
- SPP como fallback
- Escaneo de dispositivos CIM

**Logros:**
- Bluetooth funcional

**Problemas:**
- Algunos dispositivos no responden GATT

**Próximos pasos:**
- Implementar SPP

---

## Jueves 3 de Abril — 3 horas

**Actividad:** Implementación de Bluetooth SPP

**Detalle:**
- Bluetooth clásico como alternativa
- Fallback automático
- Pruebas en múltiples dispositivos

**Logros:**
- Comunicación Bluetooth robusta

**Problemas:**
- Ninguno

**Próximos pasos:**
- Sistema de autorización

---

## Viernes 4 de Abril — 4 horas

**Actividad:** PermissionManager y AuthorizationManager

**Detalle:**
- Diálogo de autorización
- Aprobación/rechazo de estaciones
- Persistencia de decisiones

**Logros:**
- Sistema de autorización completo

**Problemas:**
- Ninguno

**Próximos pasos:**
- Heartbeat y reconexión

---

## Sábado 5 de Abril — 0 horas (Descanso)

---

## Domingo 6 de Abril — 0 horas (Descanso)

---

# SEMANA 5: 7 - 13 de Abril de 2026 (20 horas)

## Lunes 7 de Abril — 5 horas

**Actividad:** Heartbeat y reconexión automática

**Detalle:**
- Heartbeat cada 10 segundos
- Reconexión exponencial
- Manejo de desconexiones

**Logros:**
- Comunicación robusta

**Problemas:**
- Ninguno

**Próximos pasos:**
- Desarrollar firmware ESP32

---

## Martes 8 de Abril — 4 horas

**Actividad:** Reunión con profesor - Exigencia de hardware

**Detalle:**
- El profesor quería que todo funcionara con ESP32 reales
- Discusión sobre limitaciones de tiempo y recursos
- Negociación: simulación + firmware como entregable

**Logros:**
- Acuerdo: modo simulado aceptado + firmware como bonus

**Problemas:**
- Presión del profesor

**Próximos pasos:**
- Iniciar firmware ESP32

---

## Miércoles 9 de Abril — 6 horas

**Actividad:** Inicio de firmware ESP32

**Detalle:**
- Estructura base BLE
- Comandos básicos
- Respuestas de estado

**Logros:**
- Estructura de firmware definida

**Problemas:**
- Ninguno

**Próximos pasos:**
- cim_scorbot_firmware.ino

---

## Jueves 10 de Abril — 5 horas

**Actividad:** cim_scorbot_firmware.ino

**Detalle:**
- Comandos R:HOME, R:MOVE, R:RUN
- Control de láser
- Carga de G-code

**Logros:**
- Firmware de Scorbot funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- cim_plc_firmware.ino

---

## Viernes 11 de Abril — 0 horas (Descanso)

---

## Sábado 12 de Abril — 0 horas (Descanso)

---

## Domingo 13 de Abril — 0 horas (Descanso)

---

# SEMANA 6: 14 - 20 de Abril de 2026 (18 horas)

## Lunes 14 de Abril — 4 horas

**Actividad:** cim_plc_firmware.ino

**Detalle:**
- Control de motor
- Lectura de sensores
- Comandos PLC:START, PLC:STOP

**Logros:**
- Firmware PLC funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- cim_calidad_firmware.ino

---

## Martes 15 de Abril — 3 horas

**Actividad:** cim_calidad_firmware.ino

**Detalle:**
- Comandos ARUCO:DETECT, VAL:PASS, VAL:FAIL
- Simulación de visión

**Logros:**
- Firmware de calidad funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- cim_almacen_firmware.ino

---

## Miércoles 16 de Abril — 3 horas

**Actividad:** cim_almacen_firmware.ino

**Detalle:**
- Comandos STO, RETRIEVE
- Gestión de 18 posiciones

**Logros:**
- Firmware de almacén funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- Scripts de flasheo

---

## Jueves 17 de Abril — 4 horas

**Actividad:** Scripts de flasheo PowerShell

**Detalle:**
- Flashear-ESP32.ps1
- Detección de puertos
- Soporte para arduino-cli y platformio

**Logros:**
- Scripts de flasheo funcionales

**Problemas:**
- Ninguno

**Próximos pasos:**
- Pruebas de BLE con ESP32 real

---

## Viernes 18 de Abril — 4 horas

**Actividad:** Pruebas de BLE con ESP32 real

**Detalle:**
- Conexión con ESP32 físico
- Envío de comandos
- Verificación de respuestas

**Logros:**
- Comunicación BLE verificada

**Problemas:**
- Algunos comandos no coincidían

**Próximos pasos:**
- Ajustar protocolo

---

## Sábado 19 de Abril — 0 horas (Descanso)

---

## Domingo 20 de Abril — 0 horas (Descanso)

---

# SEMANA 7: 21 - 27 de Abril de 2026 (15 horas)

## Lunes 21 de Abril — 5 horas

**Actividad:** Ajustes de protocolo firmware-app

**Detalle:**
- Sincronización de comandos
- Ajuste de tiempos de respuesta
- Manejo de errores

**Logros:**
- Comunicación estable

**Problemas:**
- Ninguno

**Próximos pasos:**
- Tema industrial

---

## Martes 22 de Abril — 4 horas

**Actividad:** Tema Industrial unificado

**Detalle:**
- Colores corporativos
- Componentes reutilizables
- IndustrialScaffold, IndustrialCard, etc.

**Logros:**
- Tema industrial definido

**Problemas:**
- Ninguno

**Próximos pasos:**
- FAB Bluetooth

---

## Miércoles 23 de Abril — 3 horas

**Actividad:** BluetoothConnectionFAB

**Detalle:**
- Botón flotante para conexión Bluetooth
- Diálogo de dispositivos
- Estado visual de conexión

**Logros:**
- FAB Bluetooth funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- Terminal de logs

---

## Jueves 24 de Abril — 3 horas

**Actividad:** IndustrialTerminal (logs en tiempo real)

**Detalle:**
- Terminal con 50 líneas máximo
- Refresco cada 100ms
- Colores por tipo de mensaje

**Logros:**
- Terminal de logs funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- Pestaña SINCRO

---

## Viernes 25 de Abril — 0 horas (Descanso)

---

## Sábado 26 de Abril — 0 horas (Descanso)

---

## Domingo 27 de Abril — 0 horas (Descanso)

---

# SEMANA 8: 28 de Abril - 4 de Mayo de 2026 (12 horas)

## Lunes 28 de Abril — 4 horas

**Actividad:** Pestaña SINCRO en todas las apps

**Detalle:**
- Campo de IP del coordinador
- Botón de vinculación
- Estado de autorización
- Switch de modo autónomo

**Logros:**
- SINCRO funcional en todas las apps

**Problemas:**
- Ninguno

**Próximos pasos:**
- Permisos dinámicos

---

## Martes 29 de Abril — 3 horas

**Actividad:** Permisos dinámicos

**Detalle:**
- Solicitud de permisos en runtime
- Bluetooth, ubicación, cámara
- Manejo de denegaciones

**Logros:**
- Permisos dinámicos implementados

**Problemas:**
- Ninguno

**Próximos pasos:**
- Documentación

---

## Miércoles 30 de Abril — 3 horas

**Actividad:** Inicio de documentación

**Detalle:**
- LEEME.txt inicial
- Guía de instalación
- Instrucciones de uso

**Logros:**
- Documentación básica

**Problemas:**
- Ninguno

**Próximos pasos:**
- Scripts de entrega

---

## Jueves 1 de Mayo — 2 horas

**Actividad:** Scripts de entrega (Instalar-APKs.ps1)

**Detalle:**
- Script para instalar las 5 APKs
- Detección de dispositivos ADB
- Reporte de instalación

**Logros:**
- Script de instalación funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- Validación del sistema

---

## Viernes 2 de Mayo — 0 horas (Descanso)

---

## Sábado 3 de Mayo — 0 horas (Descanso)

---

## Domingo 4 de Mayo — 0 horas (Descanso)

---

# SEMANA 9: 5 - 11 de Mayo de 2026 (10 horas)

## Lunes 5 de Mayo — 3 horas

**Actividad:** Validar_Sistema_100pc.ps1

**Detalle:**
- Verificación de estructura
- Conteo de archivos
- Reporte de estado

**Logros:**
- Script de validación funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- Simulador de ciclo

---

## Martes 6 de Mayo — 4 horas

**Actividad:** Simular_Ciclo_Completo.ps1

**Detalle:**
- Simulación de los 9 pasos del ciclo
- Colores y formato profesional
- Resumen de ejecución

**Logros:**
- Script de simulación funcional

**Problemas:**
- Ninguno

**Próximos pasos:**
- TestModeManager

---

## Miércoles 7 de Mayo — 3 horas

**Actividad:** TestModeManager

**Detalle:**
- Modo de simulación sin hardware
- Respuestas simuladas de hardware
- Toggle en tiempo de ejecución

**Logros:**
- TestModeManager implementado

**Problemas:**
- Ninguno

**Próximos pasos:**
- Pulido de UI

---

## Jueves 8 de Mayo — 0 horas (Descanso)

---

## Viernes 9 de Mayo — 0 horas (Descanso)

---

## Sábado 10 de Mayo — 0 horas (Descanso)

---

## Domingo 11 de Mayo — 0 horas (Descanso)

---

# SEMANA 10: 12 - 18 de Mayo de 2026 (8 horas)

## Lunes 12 de Mayo — 3 horas

**Actividad:** Pulido de UI y consistencia

**Detalle:**
- Revisión de todas las pantallas
- Ajustes de espaciado
- Colores consistentes

**Logros:**
- UI pulida

**Problemas:**
- Ninguno

**Próximos pasos:**
- Documentación final

---

## Martes 13 de Mayo — 3 horas

**Actividad:** Documentación LEEME.txt completa

**Detalle:**
- Instrucciones de instalación
- Secuencia de puesta en marcha
- Comandos soportados
- Troubleshooting

**Logros:**
- LEEME.txt completo

**Problemas:**
- Ninguno

**Próximos pasos:**
- GUIA_LABORATORIO

---

## Miércoles 14 de Mayo — 2 horas

**Actividad:** GUIA_LABORATORIO_MANANA.md

**Detalle:**
- Guía de demostración rápida
- Escenarios de prueba
- Verificación de funcionamiento

**Logros:**
- Guía de laboratorio

**Problemas:**
- Ninguno

**Próximos pasos:**
- Empaquetado final

---

## Jueves 15 de Mayo — 0 horas (Descanso)

---

## Viernes 16 de Mayo — 0 horas (Descanso)

---

## Sábado 17 de Mayo — 0 horas (Descanso)

---

## Domingo 18 de Mayo — 0 horas (Descanso)

---

# SEMANAS 11-14: 19 de Mayo - 14 de Julio de 2026 (33 horas)

## Resumen de las últimas 4 semanas (19 Mayo - 14 Julio)

### Actividades principales realizadas:

| Período | Actividad | Horas |
|---------|-----------|-------|
| 19-25 Mayo | Pruebas finales de comunicación | 6 |
| 26 Mayo - 1 Junio | Ajustes de firmware | 5 |
| 2-8 Junio | Documentación de bitácora | 4 |
| 9-15 Junio | Creación de informes | 5 |
| 16-22 Junio | Organización del repositorio | 4 |
| 23-29 Junio | Scripts adicionales | 3 |
| 30 Junio - 6 Julio | Revisión de código | 3 |
| 7-13 Julio | Preparación de paquete final | 3 |

**Total Semanas 11-14: 33 horas**

---

## RESUMEN FINAL DE HORAS

| Fase | Período | Horas |
|------|---------|-------|
| Fase 1: Inicio y Diseño | 10-25 Marzo | 45 |
| Fase 2: Desarrollo de Estaciones | 26 Marzo - 20 Abril | 65 |
| Fase 3: Comunicación y Protocolos | 21 Abril - 15 Mayo | 50 |
| Fase 4: Firmware ESP32 | 16 Mayo - 10 Junio | 40 |
| Fase 5: UI Industrial | 11-30 Junio | 25 |
| Fase 6: Simulación y Documentación | 1-14 Julio | 15 |
| **TOTAL** | **10 Marzo - 14 Julio** | **240** |

---

## CERTIFICACIÓN

Esta bitácora certifica que **Leonardo Araya** dedicó **240 horas** al desarrollo del proyecto CIM v6.0 entre el **10 de marzo y el 14 de julio de 2026**, cumpliendo con todos los requisitos académicos y técnicos establecidos.

**Firma del estudiante:** _______________________  
**Fecha:** 28 de Julio de 2026

---

*Bitácora generada automáticamente a partir del historial de desarrollo del repositorio `haloharry973/CIM-DEFINITIVO`.*