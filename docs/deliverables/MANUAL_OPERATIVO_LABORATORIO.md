# Manual operativo de laboratorio

> Seguridad primero: este manual no sustituye procedimientos institucionales, fichas de seguridad ni supervisión competente.

## Antes de energizar
- [ ] Operador y supervisor identificados; área delimitada y sin personas en zona de movimiento.
- [ ] E-stop físico independiente probado y accesible.
- [ ] Robot/láser sin permiso de operación; relé GPIO5 sin carga durante pruebas iniciales.
- [ ] Fuente, fusible, masas, conectores y polaridad inspeccionados.
- [ ] GPIO34 con interfaz eléctrica definida (no aplicar tensión fuera del rango del ESP32 ni dejar flotante).
- [ ] Firmware tomado de `esp32/firmware/`; commit y puertos anotados.

## Arranque controlado
1. Energizar lógica con actuadores aislados.
2. Abrir monitor serie y confirmar el anuncio `CIM_ID` esperado.
3. Conectar una estación BLE y comprobar UUID/tipo/capacidades.
4. Ejecutar sólo comandos de lectura/telemetría. Registrar timeout, desconexión o rechazo.
5. Probar sensor y relé sin carga, con una persona observando el E-stop.

## Operación y parada
Nunca usar la UI como único control de seguridad. Ante anomalía, accionar E-stop, retirar energía de actuadores, conservar logs y registrar la incidencia. Para reiniciar, investigar causa, inspeccionar el banco y repetir precondiciones; no rearmar automáticamente.

## Cierre
Desenergizar actuadores, cerrar aplicaciones/consolas, respaldar logs con fecha y completar la bitácora. Marcar expresamente toda prueba no ejecutada.
