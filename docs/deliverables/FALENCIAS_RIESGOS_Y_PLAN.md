# Falencias, riesgos y plan de mitigación

| Riesgo/falencia | Impacto | Control actual | Acción antes de operación | Estado |
|---|---|---|---|---|
| E-stop no validado físicamente | Crítico | Procedimiento documentado | Ensayo independiente con corte verificable | Abierto |
| Relé GPIO5 / carga no ensayados | Crítico | Inicio con carga aislada | Medir estado seguro y probar sin carga | Abierto |
| GPIO34 flotante o nivel incorrecto | Alto | Advertencia de interfaz | Revisar pull-up/down, nivel y aislamiento | Abierto |
| Pérdida BLE | Alto | Políticas y simulación | Ensayar desconexión/reconexión y estado seguro | Abierto |
| Robot/láser sin interlocks verificados | Crítico | Prohibición pre-hardware | Validar límites, permiso y supervisión | Abierto |
| Visión con piezas reales no validada | Medio/alto | Decisión conservadora | Dataset, iluminación y trazabilidad ArUco | Abierto |
| Deriva de versión APK/firmware | Medio | Contrato y bitácora | Registrar hashes/versiones en cada ensayo | Abierto |

## Plan de escalamiento
La severidad crítica obliga a detener y desenergizar. Corregir, revisar por supervisor y repetir desde el control afectado. No cerrar un riesgo con una afirmación: adjuntar log, fotografía, medición o acta reproducible.
