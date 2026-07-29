# Guía de presentación de tesis — entrega pre-hardware

## Mensaje central
El sistema CIM tiene una línea base de software, simulación y documentación trazable; la validación de hardware se planifica de forma segura y aún no se declara completada.

## Estructura sugerida (10–12 min)
1. Problema y objetivo del CIM (1 min).
2. Arquitectura: seis clientes Android, red compartida y estaciones ESP32 (2 min).
3. Flujo de pallet, identidad y controles de admisión (2 min).
4. Evidencia automática: validadores, tests, lint/build y límites de alcance (2 min).
5. Riesgos reales: E-stop, relé, sensor, robot/láser, BLE y visión (2 min).
6. Protocolo de banco y criterios de detención (2 min).
7. Plan de cierre y solicitud de revisión (1 min).

## Demostración responsable
Mostrar simuladores y reportes como evidencia de software. Decir “validado automáticamente/en simulación” y no “validado en hardware” si no existe registro físico. No energizar actuadores durante una presentación sin protocolo institucional, E-stop y supervisor.

## Preguntas esperables
- **¿Qué significa 100%?** Cobertura de la puerta automatizable, no seguridad física.
- **¿Qué falta?** Ensayos de identidad, E-stop, sensor, relé, reconexión, robot/láser y visión real.
- **¿Cómo se controla el riesgo?** Secuencia de bajo riesgo, criterios de detención y bitácora con evidencia.
