# Quality Gates — Entrega CIM

**Autor:** Leonardo Araya  
**Carrera:** IECI  
**Institución:** Universidad del Bío-Bío  
**Alcance:** validación automática de software/simulación y evidencias pendientes de laboratorio.

Este documento define las condiciones que deben cumplirse antes de declarar el producto, el informe técnico o la bitácora como finales. El 100% automático sólo cubre estructura, CI, pruebas JVM, lint, build de APKs y simulación; no certifica hardware físico.

## Gate 0 — Integridad de repositorio

- [x] Código activo separado de `archive/`.
- [x] Sin APKs versionadas en el repositorio activo.
- [x] Firma release sin keystore ni contraseña hardcodeada; se usan variables/propiedades locales `CIM_RELEASE_*`.
- [x] Validador estructural activo: `python3 tools/validate_system_100.py`.
- [ ] Rama de entrega limpia, sincronizada y con CI verde en el commit final.
- [ ] Documentación final revisada contra evidencia real de laboratorio.

## Gate 1 — Build reproducible

- [x] `testAllModules` cubre core-network y todos los módulos Android activos.
- [x] `lintAll` ejecuta Android Lint real en los siete módulos.
- [x] `buildAllApks` exporta seis APK debug.
- [x] `validateApks` valida presencia y tamaño mínimo de las seis APK.
- [x] `writeApkChecksums` registra `config/output-apks/SHA256SUMS.txt`.
- [ ] GitHub Actions verde en el commit de entrega.
- [ ] Artefactos descargados y checksums archivados en la bitácora final.

## Gate 2 — Red e identidad

- [x] UUID, MAC, tipo, modelo, firmware y capacidades modelados en la capa de red.
- [x] Política de lista permitida/bloqueada cubierta por pruebas JVM.
- [x] UUID/tipo/capacidad incompatible se bloquea por `StationIdentityPolicy`.
- [x] Token de emparejamiento se transporta como hash SHA-256, no como texto plano en el handshake Android.
- [ ] Reconexión, pérdida de enlace y rechazo persistente ensayados en dispositivos reales.

## Gate 3 — Flujo CIM simulado

- [x] Máquina de estados de pallet bloquea saltos imposibles por pruebas JVM.
- [x] Simulador de seguridad de visión valida 1.000 casos sin habilitar automatización ante datos incompletos.
- [ ] Evento de pallet registrado y trazable en ejecución Android real.
- [ ] Visualización arcade verificada con logs/capturas de dispositivo.
- [ ] PASS, FAIL, BLOCKED y REVIEW_REQUIRED evidenciados en bitácora.

## Gate 4 — Visión

- [x] Herramientas de inspección/exportación YOLO disponibles en `tools/`.
- [x] Decisión conservadora de visión simulada ante baja confianza o datos incompletos.
- [ ] Modelo YOLO inspeccionado con hash y clases documentadas para el commit final.
- [ ] TFLite generado y validado contra el checkpoint fuente.
- [ ] Cámara asocia detección a pallet/ArUco en laboratorio.

## Gate 5 — Hardware y seguridad

- [x] Carpeta canónica de firmware activa: `esp32/firmware/`.
- [x] Script de flasheo apunta al firmware activo y advierte no conectar actuadores sin E-stop.
- [ ] Firmware Wemos compilado/flashado y versión confirmada.
- [ ] E-stop físico probado de forma independiente.
- [ ] Relé inicia en estado seguro.
- [ ] Sensor GPIO34 tiene interfaz eléctrica protegida y sin flotación.
- [ ] Robot/láser cuentan con límites, interlocks y procedimiento de laboratorio.

## Gate 6 — Entrega documental

- [x] README diferencia validación automática de validación de hardware.
- [x] Bitácora mantiene resultados verificables y pendientes explícitos.
- [ ] Informe técnico cita únicamente evidencia disponible del commit final.
- [ ] PDF generado desde fuentes Markdown actuales.
- [ ] No se declaran horas, resultados o certificaciones no verificadas.
