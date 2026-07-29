# Auditoría técnica — CIM v6.0

**Fecha:** 28 de julio de 2026
**Alcance revisado:** configuración Gradle, código Kotlin Android, red TCP/BLE, protocolo CIM, manifiestos, pruebas, scripts operativos, CI y firmware ESP32.
**Limitación local:** este entorno de agente no incluye JDK/Android SDK, por lo que Gradle se valida mediante GitHub Actions y revisión estática local. Las comprobaciones Python sí se ejecutan localmente.

## Correcciones aplicadas inicialmente

1. Restauración de bloques `catch` Kotlin inválidos y logs `Log.e`.
2. Importaciones `android.util.Log` faltantes.
3. Eliminación de código residual inválido en `TcpServer.kt`.
4. Reducción de importaciones duplicadas masivas.
5. Alineación del paquete `AppControl.kt` con `com.industria.plc`.
6. Corrección de pruebas instrumentadas con package names antiguos.

## Correcciones aplicadas en esta revisión

1. **Puerta 100% automatizable.** Se agregó `tools/validate_system_100.py` y el wrapper `tools/powershell/Validar_Sistema_100pc.ps1`, apuntando a la estructura activa del repositorio.
2. **CI verificable por ejecución manual.** El workflow conserva `workflow_dispatch`; para esta rama se dispara manualmente y ejecuta `testAllModules` y `buildAllApks`. La validación estructural/lint/checksums quedan disponibles en Gradle y scripts locales.
3. **Scripts operativos actualizados.** Instalación de APKs, copiado de APKs y flasheo ESP32 usan `config/output-apks`, `android/apks` y `esp32/firmware` en vez de rutas históricas.
4. **Firma release sin secretos embebidos.** Los módulos Coordinador y Almacén ya no contienen keystore ni contraseñas hardcodeadas; usan `CIM_RELEASE_*` si se desea firmar release.
5. **Handshake sin token en texto plano desde Android.** `StationClient` envía el token de emparejamiento como `sha256:<hash>` y el Coordinador valida con comparación constante.
6. **Manifiestos y permisos.** Se declaró `CAMERA` donde se solicita, se acotaron permisos legacy por SDK y se evitó pedir ubicación en Android 12+ cuando se usan permisos Bluetooth modernos.
7. **Gradle QA real.** `lintAll` dejó de ser placeholder; `writeApkChecksums` genera `SHA256SUMS.txt`; `buildFirmware` valida la carpeta canónica de firmware.

## Validaciones realizadas localmente

- `python3 tools/validate_system_100.py --quiet`: **PASS 12/12 (100%)**.
- `python3 -m compileall -q tools`: sin errores de sintaxis Python.
- `python3 tools/vision_safety_simulator.py --cases 1000`: 1.000 escenarios sin violar el contrato conservador de seguridad.
- Búsqueda estática de rutas históricas en README/tools/deliverables: sin referencias activas.

## Riesgos pendientes priorizados

### Críticos / laboratorio

| Hallazgo | Estado | Recomendación |
|---|---|---|
| Ensayos físicos de E-stop, relé, sensor GPIO34, robot y láser no documentados | Pendiente | No energizar actuadores reales sin E-stop independiente, límites, interlocks y bitácora de pruebas. |
| Tráfico TCP de laboratorio permite cleartext en Coordinador/Almacén | Pendiente | Para despliegue real usar TLS/mTLS con certificados gestionados; el modo cleartext debe limitarse a banco aislado/simulación. |
| Identidad por MAC/payload puede suplantarse si la red no está controlada | Parcialmente mitigado | Asociar identidad criptográfica al canal autenticado antes de usarlo fuera de laboratorio. |

### Altos

| Hallazgo | Estado | Recomendación |
|---|---|---|
| Token de emparejamiento por defecto es apto sólo para laboratorio | Mitigado para texto plano | Aprovisionar token único por estación fuera del repositorio antes de pruebas reales. |
| Firmware no compilado/flasheado en esta ejecución | Pendiente hardware | Compilar con Arduino CLI/PlatformIO y registrar versión en bitácora. |
| YOLO aún no validado como TFLite de producción | Pendiente visión | Inspeccionar clases, generar TFLite, registrar hashes y métricas antes de habilitar automatización. |

### Medios

| Hallazgo | Estado | Recomendación |
|---|---|---|
| Lint/CI dependen de entorno Android externo | Controlado por GitHub Actions | Mantener Actions verde en el commit final y guardar artefactos/checksums. |
| Documentos históricos en `entrega/` pueden contener afirmaciones antiguas | Pendiente revisión editorial | Preferir `docs/deliverables/` como fuente viva y regenerar PDF desde Markdown actual. |

## Siguiente paso recomendado

1. Ejecutar GitHub Actions en el commit final de esta rama.
2. Registrar URL del run, checksums de APKs y resultado de `system_100_validation.json` en la bitácora.
3. Ejecutar pruebas de laboratorio sólo después de revisar el procedimiento eléctrico/mecánico y E-stop físico.
