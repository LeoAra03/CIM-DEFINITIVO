---
title: Informe técnico de avance — Sistema CIM
author: Leonardo Araya
program: IECI
institution: Universidad del Bío-Bío
date: 2026-07-28
status: En validación
---

# Informe técnico de avance

## Resumen

El proyecto CIM integra aplicaciones Android para Coordinación, PLC, Manufactura, Calidad, Almacén y Wear, una biblioteca de red compartida y firmware ESP32/Wemos D1 R32. El objetivo actual es obtener una plataforma reproducible, trazable y preparada para validación de laboratorio.

Este documento informa únicamente resultados verificables. No constituye certificación de seguridad funcional, validación de hardware ni aprobación académica.

## Identidad del proyecto

| Campo | Valor |
|---|---|
| Estudiante | Leonardo Araya |
| Carrera | IECI |
| Institución | Universidad del Bío-Bío |
| Proyecto | Sistema CIM v6.0 |
| Estado | En validación por CI y simulación |

## Componentes

| Componente | Rol |
|---|---|
| Coordinador | Autorización, registro de nodos, red y orquestación |
| PLC | Cinta, sensor de proximidad y eventos de pallet |
| Manufactura | Robot, posiciones, ArUco y orden de láser |
| Calidad | Cámara, ArUco, validación y visión asistida |
| Almacén | Operaciones de rack, guardar y recuperar |
| Wear | Interfaz compacta de supervisión |
| Wemos D1 R32 | Firmware BLE de estación y telemetría de proceso |

## Evidencia de compilación

La validación automática definida para el commit de entrega es:

- `python3 tools/validate_system_100.py` para la puerta estructural 100% automatizable en simulación.
- Workflow: `Android CIM CI` ejecutable manualmente con `workflow_dispatch` para la rama de entrega.
- Pruebas: `testAllModules`.
- Análisis estático Android: `lintAll`.
- Compilación: `buildAllApks`.
- Validación de artefactos: `validateApks` y `writeApkChecksums`.
- Resultado esperado: seis APK debug como artefactos de CI y `SHA256SUMS.txt`.

La evidencia específica debe vincularse desde la ejecución de Actions correspondiente al commit de entrega. Esta evidencia no reemplaza pruebas de hardware físico.

## Mejoras verificables incorporadas

- Corrección de bloqueantes Kotlin en PLC y Calidad.
- Compilación de las seis APKs mediante CI.
- Identidad de estación con UUID canónico.
- Registro de MAC, UUID, IP, versión, modelo y capacidades.
- Bloqueo persistente de dispositivos rechazados.
- Política de admisión de Wemos D1 R32 por UUID, tipo y capacidades.
- Fragmentación y reensamblado BLE para MTU inicial de 20 bytes.
- Máquina de estados de pallet que bloquea transiciones imposibles.
- Visualización arcade basada en eventos aceptados.
- Herramientas de inspección/exportación del checkpoint YOLO `bestMH.pt`.
- Puerta 100% automatizable para estructura activa y modo simulado.
- Firma release sin contraseñas embebidas en Gradle.
- Handshake Android con token transportado como hash SHA-256.
- Checksums SHA-256 generados para las APKs debug.

## Limitaciones vigentes

- No existe prueba física documentada con Wemos, relé, Scorbot ni láser.
- El modelo YOLO aún no ha sido convertido y validado como TFLite dentro de Android.
- El E-stop físico independiente no está implementado en hardware.
- El movimiento del robot y potencia del láser no cuentan aún con realimentación física validada.
- La simulación no reemplaza pruebas eléctricas, mecánicas ni de seguridad.

## Próximos pasos

1. Mantener CI verde en cada cambio.
2. Generar y validar TFLite desde el modelo `bestMH.pt`.
3. Capturar evidencia de visión vinculada a pallet y ArUco.
4. Probar firmware Wemos D1 R32 en banco.
5. Ejecutar el protocolo de seguridad y E-stop físico.
6. Completar los Quality Gates antes de elaborar el informe final.
