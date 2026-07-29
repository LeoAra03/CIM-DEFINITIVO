# Lluvia de ideas y decisiones de diseño

| Tema | Alternativas consideradas | Decisión actual | Motivo / revisión pendiente |
|---|---|---|---|
| Identidad estación | nombre libre, MAC sola, UUID + capacidades | UUID canónico, tipo y capacidades | Evita aceptar nodos incompatibles; comprobar en banco. |
| Transporte BLE | mensajes sin fragmentación, framing | reensamblado/framing para MTU inicial | Debe medirse bajo interferencia real. |
| Visión | aprobar por defecto, rechazo conservador | FAIL/REVIEW ante datos incompletos | Favorece seguridad; calibrar umbrales con dataset real. |
| Seguridad | interlock sólo software, E-stop físico | E-stop físico independiente requerido | No cerrado hasta ensayo físico. |
| Firmware | snapshots históricos, carpeta activa | sólo `esp32/firmware/` | Reduce ambigüedad de flasheo. |
| Entrega | afirmar “100% probado”, separar alcance | separar CI/simulación de hardware | Conserva trazabilidad académica. |

## Ideas descartadas por ahora
- Activar movimiento o láser por una confirmación exclusivamente remota.
- Declarar listo un modelo TFLite sin hash, conversión y prueba documentada.
- Usar archivos de `archive/` para una puesta en marcha.

## Próxima revisión
Revisar estas decisiones luego de las pruebas de identidad, E-stop, sensor, relé y reconexión; toda modificación debe referenciar evidencia en la bitácora.
