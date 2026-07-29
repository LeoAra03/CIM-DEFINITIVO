# Proceso de validación pre-hardware

## Objetivo
Establecer una secuencia repetible para pasar del repositorio al banco sin confundir validación automatizada con aprobación física.

## Roles y precondiciones
- **Operador:** ejecuta pasos y registra evidencia.
- **Observador/supervisor:** confirma límites, E-stop y condición segura antes de potencia.
- Usar el commit identificado; árbol limpio o cambios anotados; no usar `archive/` como firmware.

## Secuencia
1. Identificar commit, operador, estación y versiones de APK/firmware en la bitácora.
2. Ejecutar los cinco controles listados en la entrega pre-hardware y guardar su salida.
3. Revisar `validate_firmware_contract.py`: UUID, nombre, tipo, versión y capacidades deben concordar entre firmware y documentación.
4. Con potencia deshabilitada, inspeccionar cableado, tierra común, interfaz de GPIO34 y relé GPIO5; confirmar que no hay carga peligrosa conectada.
5. Probar E-stop independiente y verificar que corta la energía de actuadores, no sólo una orden de software.
6. Flashear **una** estación a la vez y capturar `CIM_ID` por consola/BLE.
7. Validar admisión, rechazo y reconexión BLE con telemetría; no habilitar movimiento.
8. Ejecutar pruebas de sensor y relé primero sin carga. Sólo continuar con aprobación explícita.
9. Documentar resultado observado, log/captura, incidente y decisión de continuar/detener.

## Criterios de detención
Detener, desenergizar y abrir incidencia ante identidad distinta, relé activo al arranque, E-stop ineficaz, GPIO flotante, pérdida de enlace en estado de riesgo, movimiento inesperado o ausencia de supervisor.
