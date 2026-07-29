## Resumen

<!-- Explique qué cambia y por qué. -->

## Validación

- [ ] `python3 tools/validate_firmware_contract.py --quiet`
- [ ] `python3 tools/validate_system_100.py --quiet`
- [ ] `python3 tools/prehardware_readiness.py --quiet`
- [ ] `python3 -m compileall -q tools`
- [ ] `git diff --check`
- [ ] Si cambia Android: CI, tests y build de APK revisados.

## Impacto y evidencia

- [ ] No incorpora secretos, APKs ni artefactos generados.
- [ ] Actualicé documentación/bitácora si cambió un contrato, protocolo o comportamiento.
- [ ] No afirma ensayos físicos que no tengan evidencia trazable.
- [ ] Si afecta hardware/seguridad, adjunté plan de laboratorio y revisión de E-stop/interlocks.
