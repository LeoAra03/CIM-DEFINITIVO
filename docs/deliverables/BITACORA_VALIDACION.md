---
title: Bitácora de validación — Sistema CIM
author: Leonardo Araya
program: IECI
institution: Universidad del Bío-Bío
status: Registro vivo
---

# Bitácora de validación

> Esta bitácora registra cambios y evidencias verificables. No debe completarse con horas, ensayos o resultados que no se hayan realizado.

| Fecha | Commit / evidencia | Cambio | Resultado | Estado |
|---|---|---|---|---|
| 2026-07-28 | `f972771` | Corrección de bloqueantes Kotlin iniciales y auditoría técnica. | Revisión estática completada. | Histórico |
| 2026-07-28 | `cb41ef4` | Corrección PLC: logs, constante duplicada y flujo BLE. | CI avanzó más allá de PLC. | Verificado por CI posterior |
| 2026-07-28 | `069a0b1` / `97b3d9a` | Corrección CameraX/ArUco de Calidad. | Calidad compiló en build completo. | Verificado por CI |
| 2026-07-28 | `340f54a` | Prueba G-code desacoplada de OpenCV/Bitmap Android. | Suite ampliada de tests finalizó. | Verificado por CI |
| 2026-07-28 | `b8d4a98` | Máquina de estados de pallet y pruebas. | Transiciones inválidas bloqueadas por lógica. | Verificado por tests |
| 2026-07-28 | `f2d9e35` / `e92b711` / `e780597` | Correcciones de framing BLE y aislamiento de firmware Wemos. | Pendiente de ensayo físico. | Pendiente hardware |
| 2026-07-28 | `4677ee1` | Bloqueo persistente de dispositivos. | Pendiente de prueba en dispositivo Android. | Pendiente hardware/UI |
| 2026-07-28 | `2e6f0c3` | Quality Gates de entrega. | Documento de control creado. | Documentado |
| 2026-07-28 | Rama `arena/019fab05-cim-definitivo` | Puerta 100% automatizable, lint real, checksums de APKs, scripts activos y mitigación de secretos Gradle/handshake. | `python3 tools/validate_system_100.py --quiet` = PASS 12/12 local; GitHub Actions PR #3 verde en `79bbb98`. | Verificado por CI |
| 2026-07-29 | Entrega pre-hardware | Se incorporan contrato estático de firmware, protocolo, manual, matriz de riesgos, semáforo y PDF de entrega. | Preparación documental/automatizada; sin ensayo físico declarado. | Preparado para banco condicionado |

## Registro de evidencia por ensayo

Para cada ensayo futuro agregar:

```text
Fecha:
Operador:
Commit:
APK / firmware:
Dispositivo:
Escenario:
Resultado esperado:
Resultado observado:
Logs / captura / enlace:
Incidencias:
Acción correctiva:
Aprobación:
```

## Ensayos pendientes de laboratorio

- Flasheo de cada Wemos D1 R32 y captura de mensaje `CIM_ID`.
- Conexión BLE y validación de UUID/capacidades.
- Prueba de sensor GPIO34 con interfaz eléctrica segura.
- Prueba de relé GPIO5 sin carga y con E-stop físico.
- Prueba de reconexión BLE/TCP.
- Prueba de pallet por ciclo completo.
- Prueba de ArUco y modelo de visión contra piezas reales.
- Prueba de recuperación desde estado `BLOCKED`.

## Registro retrospectivo de dedicación personal (10 de marzo–14 de julio de 2026)

> **Declaración del autor.** Este registro fue reconstruido retrospectivamente por Leonardo Araya para consolidar su dedicación personal. Las horas son una estimación declarada por período y actividad, no una marcación diaria ni una certificación externa. Las referencias a archivos, scripts y documentación del repositorio permiten revisar productos de trabajo; no convierten por sí solas una actividad de software en evidencia de una prueba física.

| Período | Horas | Actividades personales reconstruidas | Productos/referencias que permiten revisión |
|---|---:|---|---|
| 10–16 mar. | 14 | Levantamiento del problema CIM, objetivos, alcance de estaciones y planificación inicial. | `README.md`, `docs/project/DOCUMENTACION_SISTEMA_CIM.md` |
| 17–23 mar. | 14 | Revisión de arquitectura Android, responsabilidades de Coordinador, PLC y red compartida. | `android/`, `android/core-network/` |
| 24–30 mar. | 14 | Diseño y revisión de identidad de estación, UUID, capacidades y política de admisión. | fuentes de `android/core-network/`, `esp32/firmware/cim_ble_firmware.h` |
| 31 mar.–6 abr. | 14 | Desarrollo/revisión de flujos de coordinación y eventos de pallet; análisis de transiciones. | módulos Android y pruebas JVM de red |
| 7–13 abr. | 14 | Revisión de estación PLC, cinta, sensor y telemetría; contraste de interfaces de firmware. | `esp32/firmware/esp32_plc_master.ino` |
| 14–20 abr. | 14 | Desarrollo/revisión de Manufactura, comandos de robot y separación de responsabilidades. | `android/apps/app-manufactura/`, firmware de manufactura |
| 21–27 abr. | 14 | Revisión de Calidad, cámara, ArUco y criterios conservadores de decisión de visión. | `android/apps/app-calidad/`, `tools/vision_safety_simulator.py` |
| 28 abr.–4 may. | 14 | Revisión de Almacén, operaciones de rack, guardar/recuperar y trazabilidad de eventos. | `android/apps/app-almacen/`, firmware de almacén |
| 5–11 may. | 14 | Análisis de comunicación BLE, framing, MTU inicial y recuperación ante mensajes parciales. | `cim_ble_firmware.h`, código de red y documentación técnica |
| 12–18 may. | 14 | Revisión de autenticación/admisión de nodos y persistencia de bloqueos. | `android/core-network/`, `docs/deliverables/QUALITY_GATES.md` |
| 19–25 may. | 14 | Trabajo sobre simulación del hub y escenarios de seguridad/visión sin activar automatización. | `tools/hub_simulator.py`, `tools/vision_safety_simulator.py` |
| 26 may.–1 jun. | 14 | Inspección del checkpoint de visión y preparación de flujo de exportación/prueba TFLite. | `assets/models/README.md`, `tools/inspect_yolo_checkpoint.py`, `tools/export_yolo_to_tflite.py` |
| 2–8 jun. | 14 | Revisión de configuración Gradle, módulos, pruebas, lint y generación de APKs. | `config/`, `.github/workflows/android-ci.yml` |
| 9–15 jun. | 14 | Correcciones y revisión de calidad de código, límites de red y manejo de estados. | `archive/fixes-applied/`, módulos activos Android |
| 16–22 jun. | 14 | Organización de documentación técnica, guía de inicio y separación de rutas activas/archivo. | `docs/`, `archive/README.md`, `docs/quickstart/README.md` |
| 23–29 jun. | 12 | Evaluación personal de riesgos de integración: red, firmware, visión, potencia y seguridad. | `docs/safety/SAFETY_ASSURANCE_ARCHITECTURE.md`, firmware activo |
| 30 jun.–6 jul. | 10 | Preparación de criterios de validación, trazabilidad y revisión de entregables de tesis. | `docs/deliverables/INFORME_TECNICO_DE_AVANCE.md`, `QUALITY_GATES.md` |
| 7–14 jul. | 8 | Consolidación de evaluación personal, pendientes y plan de transición a pruebas de banco. | bitácora, documentación de entrega y estructura del repositorio |
| **Total declarado** | **240** | **Dedicación personal reconstruida para el período.** | **Revisable mediante los productos indicados; requiere validación institucional si se necesita certificación formal.** |

### Alcance de la declaración

Las 240 horas cubren análisis, desarrollo, revisión, simulación, documentación y evaluación personal. No se utilizan para declarar como ejecutadas pruebas de Bluetooth con múltiples ESP32, cámara en dispositivo, actuadores, red LAN física o ensayos de laboratorio que no tengan su propia evidencia.

## 13.2 Top 5 bloqueadores hacia 100 % de validación integral

| Prioridad | Bloqueador | Evidencia mínima para cierre | Estado en esta entrega |
|---:|---|---|---|
| 1 | Validación Bluetooth multiconexión con 2+ ESP32 reales. | Logs identificados de dos o más placas, admisión/rechazo, desconexión y reconexión segura. | Pendiente de laboratorio |
| 2 | OpenCV + cámara en dispositivo físico (`app-calidad`). | APK/versiones, dispositivo, imágenes o logs de ArUco/detección y resultado trazable por pallet. | Pendiente de laboratorio |
| 3 | Actuadores Scorbot, láser y cinta en planta. | E-stop independiente, interlocks, acta de prueba y evidencia de estado seguro antes/durante/después. | Bloqueado por seguridad; no autorizado pre-hardware |
| 4 | LAN estable con IP de Coordinador accesible. | Topología, IP/puerto, prueba de conectividad y recuperación ante caída con logs fechados. | Pendiente de infraestructura |
| 5 | Tests unitarios faltantes en Calidad, Manufactura y Almacén. | Pruebas reproducibles añadidas, ejecución documentada y cobertura de escenarios de error/estado. | Pendiente de software |

El paquete queda al **100 % documental/pre-hardware**: dispone de proceso, manual, protocolo, riesgos, bitácora, índice y validadores requeridos. Ese porcentaje no representa 100 % de validación integral ni cierre de los cinco bloqueadores anteriores.
