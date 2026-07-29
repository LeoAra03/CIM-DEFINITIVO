---
title: Entrega pre-hardware — Sistema CIM
author: Leonardo Araya
institution: Universidad del Bío-Bío
date: 2026-07-29
status: Preparada para banco; no certificada en hardware
---

# Entrega pre-hardware — Leonardo Araya

## Propósito y alcance
Esta entrega deja trazables el software, la simulación y el plan de puesta en marcha antes de energizar equipos. El alcance es **pre-hardware**: una puerta automática aprobada sólo demuestra coherencia del repositorio y de los contratos revisados; no demuestra seguridad funcional, comunicación en radio, potencia, movimiento, láser ni calidad de visión en piezas reales.

## Línea base revisable

| Área | Línea base |
|---|---|
| Aplicaciones | Coordinador, PLC, Manufactura, Calidad, Almacén y Wear |
| Red | `android/core-network`, identidad, capacidades y política de admisión |
| Firmware | `esp32/firmware/` y cabecera común `cim_ble_firmware.h` |
| Simulación | `hub_simulator.py`, `vision_safety_simulator.py` |
| Modelo | `assets/models/bestMH.pt`; conversión TFLite pendiente de evidencia |

## Controles ejecutables
Ejecutar desde la raíz, antes de un ensayo y en el commit que se lleve al banco:

```bash
python3 tools/validate_firmware_contract.py --quiet
python3 tools/validate_system_100.py --quiet
python3 tools/prehardware_readiness.py --quiet
python3 -m compileall -q tools
git diff --check
```

Los tres primeros comandos verifican contratos estáticos, estructura de la entrega y preparación documental. Una salida satisfactoria no autoriza a conectar actuadores.

## Decisión de preparación
**Estado: preparado para revisión de banco, condicionado.** Se puede preparar el banco sin carga y revisar identidad/telemetría. No se autoriza operación de relé con carga, robot, láser o ciclo autónomo hasta cerrar los ítems del protocolo y registrar la evidencia en `BITACORA_VALIDACION.md`.

## Paquete y navegación
- Proceso: `PROCESO_VALIDACION_PRE_HARDWARE.md`.
- Operación segura: `MANUAL_OPERATIVO_LABORATORIO.md`.
- Pruebas: `PROTOCOLO_PRUEBAS_HARDWARE.md`.
- Riesgos: `FALENCIAS_RIESGOS_Y_PLAN.md`.
- Presentación: `GUIA_PRESENTACION_TESIS.md`.

## Declaración de evidencia
No se inventaron mediciones, horas de laboratorio, certificaciones ni resultados de hardware. Las casillas pendientes se mantienen pendientes hasta disponer de logs, fotos o actas identificadas por fecha, operador y commit.
