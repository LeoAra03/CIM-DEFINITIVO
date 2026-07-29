# Índice del repositorio CIM

## Código operativo

| Ubicación | Contenido |
|---|---|
| `android/` | Aplicaciones Coordinador, PLC, Manufactura, Calidad, Almacén, Wear y `core-network`. |
| `esp32/firmware/` | Firmware canónico de estaciones ESP32/Wemos. |
| `tools/` | Simulación, validadores, inspección de visión y utilidades de operación. |
| `config/` | Configuración Gradle centralizada. |
| `docs/` | Documentación activa; comenzar en [`docs/README.md`](README.md). |

## Uso y validación

- [Instructivo de uso completo](INSTRUCTIVO_USO_PROYECTO.md): preparación, build, instalación, simulación y paso a laboratorio.
- [Estado de validación y cobertura](VALIDACION_Y_COBERTURA.md): alcance exacto de CI, UI y hardware.
- [Manual de implementación (Markdown)](project/DOCUMENTACION_SISTEMA_CIM.md) · [PDF](project/MANUAL_IMPLEMENTACION_CIM.pdf).

## Entrega pre-hardware

| Documento | Uso |
|---|---|
| `deliverables/ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.md` y `.pdf` | Resumen, alcance y declaración de límites. |
| `deliverables/PROCESO_VALIDACION_PRE_HARDWARE.md` | Secuencia controlada de puesta en marcha. |
| `deliverables/MANUAL_OPERATIVO_LABORATORIO.md` | Reglas de operación y parada segura. |
| `deliverables/PROTOCOLO_PRUEBAS_HARDWARE.md` | Casos HW-01 a HW-09 y evidencia requerida. |
| `deliverables/PRE_HARDWARE_READINESS.md` | Semáforo y criterios de avance. |
| `deliverables/BITACORA_VALIDACION.md` | Registro vivo de evidencia. |
| `deliverables/FALENCIAS_RIESGOS_Y_PLAN.md` | Riesgos abiertos y mitigación. |
| `deliverables/LLUVIA_IDEAS_Y_DECISIONES.md` | Decisiones de diseño y revisiones. |
| `deliverables/EXPECTATIVA_VS_RESULTADO.md` | Brechas verificables. |
| `deliverables/GUIA_PRESENTACION_TESIS.md` | Relato responsable para exposición. |

## Comandos de control

```bash
python3 tools/validate_firmware_contract.py --quiet
python3 tools/validate_system_100.py --quiet
python3 tools/prehardware_readiness.py --quiet
python3 -m compileall -q tools
git diff --check
```

Los controles son estáticos/automatizados. No habilitan robot, láser, relé con carga ni operación autónoma: esas actividades requieren el protocolo de laboratorio y evidencia física.

## Historial
`archive/` conserva snapshots y documentos históricos; no es fuente para compilar ni flashear. `entrega/` contiene informes históricos seleccionados. Consultar siempre las rutas activas anteriores.
