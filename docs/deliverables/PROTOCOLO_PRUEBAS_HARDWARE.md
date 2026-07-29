# Protocolo de pruebas de hardware

## Reglas
Ejecutar en orden, con E-stop accesible, un operador y un supervisor. Detener ante cualquier resultado inesperado. Registrar cada caso en la bitácora; “no ejecutado” es un resultado válido para esta entrega.

| ID | Prueba | Precondición | Resultado esperado | Evidencia requerida |
|---|---|---|---|---|
| HW-01 | Inspección sin energía | Actuadores aislados | Cableado/polaridad/masa e interfaz GPIO34 revisados | Lista firmada/foto |
| HW-02 | E-stop | Banco seguro | Corta energía de actuadores independientemente del software | Medición/foto y acta |
| HW-03 | Identidad | Una placa conectada | `CIM_ID` coincide con estación, UUID y capacidades | Log serie/BLE |
| HW-04 | Admisión BLE | App y placa identificadas | Acepta contrato válido; rechaza incompatible | Logs de ambos casos |
| HW-05 | Sensor GPIO34 | Interfaz segura | Lecturas estables en reposo y activación | Tabla de mediciones |
| HW-06 | Relé GPIO5 sin carga | E-stop probado | Arranque en estado seguro; conmutación ordenada | Log y medición |
| HW-07 | Reconexión | Telemetría activa | Pérdida/reingreso deja estado seguro | Log con tiempos |
| HW-08 | Visión | Cámara/pieza/ArUco | Resultado asociado a contexto trazable | Capturas y parámetros |
| HW-09 | Actuadores | HW-01..08 aprobadas | Sólo con autorización institucional | Acta específica |

HW-09 está explícitamente fuera del alcance pre-hardware hasta completar los controles anteriores.
