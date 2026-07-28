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
