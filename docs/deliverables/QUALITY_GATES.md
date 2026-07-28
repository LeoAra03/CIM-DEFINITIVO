# Quality Gates — Entrega CIM

**Autor:** Leonardo Araya  
**Carrera:** IECI  
**Institución:** Universidad del Bío-Bío  

Este documento define las condiciones que deben cumplirse antes de declarar el producto, el informe técnico o la bitácora como finales.

## Gate 0 — Integridad de repositorio

- [ ] Rama de entrega limpia y sincronizada.
- [ ] Sin secretos, keystores ni APKs versionadas por error.
- [ ] Sin documentación activa que declare resultados no verificables.
- [ ] Código activo separado de `archive/`.

## Gate 1 — Build reproducible

- [ ] GitHub Actions verde en el commit de entrega.
- [ ] `testAllModules` verde.
- [ ] `buildAllApks` verde.
- [ ] Seis APK debug exportadas como artefactos.
- [ ] Checksums de artefactos registrados.

## Gate 2 — Red e identidad

- [ ] UUID, MAC, tipo, modelo, firmware y capacidades visibles en Coordinador.
- [ ] Política de lista permitida/bloqueada probada.
- [ ] Un UUID/tipo/capacidad incompatible se bloquea.
- [ ] Reconexión y pérdida de enlace ensayadas.

## Gate 3 — Flujo CIM simulado

- [ ] Evento de pallet registrado y trazable.
- [ ] Máquina de estados rechaza saltos imposibles.
- [ ] Visualización arcade refleja eventos aceptados.
- [ ] PASS, FAIL, BLOCKED y REVIEW_REQUIRED visibles.

## Gate 4 — Visión

- [ ] Modelo YOLO inspeccionado, con hash y clases documentadas.
- [ ] TFLite generado y validado contra el checkpoint fuente.
- [ ] Cámara asocia detección a pallet/ArUco.
- [ ] Predicciones de baja confianza no habilitan automatización.

## Gate 5 — Hardware y seguridad

- [ ] Firmware Wemos compilado/flashado y versión confirmada.
- [ ] E-stop físico probado de forma independiente.
- [ ] Relé inicia en estado seguro.
- [ ] Sensor GPIO34 tiene interfaz eléctrica protegida y sin flotación.
- [ ] Robot/láser cuentan con límites, interlocks y procedimiento de laboratorio.

## Gate 6 — Entrega documental

- [ ] Informe técnico cita únicamente evidencia disponible.
- [ ] Bitácora incluye fecha, commit, prueba, resultado y evidencia.
- [ ] PDF generado desde fuentes Markdown actuales.
- [ ] No se declaran horas, resultados o certificaciones no verificadas.
