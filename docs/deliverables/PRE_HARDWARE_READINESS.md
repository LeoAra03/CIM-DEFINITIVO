# Pre-hardware readiness

## Semáforo de preparación

| Dominio | Estado | Condición |
|---|---|---|
| Repositorio y documentación | Verde | Entregables y validadores presentes. |
| Contrato firmware | Verde condicionado | Contrato estático revisado; falta captura desde cada placa. |
| Simulación/software | Verde condicionado | Cobertura automatizable; no sustituye banco. |
| Banco eléctrico | Ámbar | Requiere inspección de fuente, masas, GPIO34 y relé. |
| E-stop e interlocks | Rojo | No hay evidencia física adjunta. |
| Robot/láser | Rojo | No autorizado en esta entrega. |
| Visión real | Ámbar | Herramientas presentes; falta evidencia con piezas. |

## Regla de avance
Sólo se avanza de verde condicionado/ámbar a la siguiente prueba cuando las casillas previas del protocolo estén firmadas en la bitácora. Rojo bloquea cualquier prueba que involucre actuador o peligro asociado.

## Evidencia mínima para cambiar estado
Fecha, operador, commit, dispositivo, versión/hash, configuración, resultado esperado/observado y enlace a log/captura. Una captura sin contexto no cierra el ítem.
