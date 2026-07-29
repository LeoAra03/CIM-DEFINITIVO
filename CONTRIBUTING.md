# Contribuir al Sistema CIM

## Principios

- Mantener separadas las rutas activas de `archive/`.
- No versionar APKs, claves, keystores, contraseñas, logs personales ni artefactos de build.
- No declarar una prueba de hardware sin fecha, operador, versión/commit y evidencia en `docs/deliverables/BITACORA_VALIDACION.md`.
- No habilitar actuadores como consecuencia de una validación puramente automática.

## Antes de solicitar integración

Ejecute desde la raíz:

```bash
python3 tools/validate_firmware_contract.py --quiet
python3 tools/validate_system_100.py --quiet
python3 tools/prehardware_readiness.py --quiet
python3 -m compileall -q tools
git diff --check
```

Para cambios Android, ejecute además en un entorno con JDK 17 y Android SDK:

```bash
cd config
./gradlew testAllModules
./gradlew lintAll
./gradlew buildAllApks validateApks writeApkChecksums
```

## Cambios de firmware y documentación

Todo cambio de identidad BLE, UUID, capacidades, pines o protocolo exige actualizar el contrato, los documentos afectados y la evidencia de validación. Los cambios que afecten a seguridad deben revisar primero `docs/deliverables/PROTOCOLO_PRUEBAS_HARDWARE.md` y `docs/safety/`.
