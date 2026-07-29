# Instructivo de uso del proyecto CIM

<p align="center"><img src="assets/ubb_logo.png" alt="Universidad del Bío-Bío" width="260"></p>

Este instructivo conduce una ejecución reproducible del software y una preparación segura para el laboratorio. Lea primero los límites: una compilación o simulación aprobada **no** habilita actuadores físicos.

## 1. Requisitos

| Recurso | Uso |
|---|---|
| Git y Python 3 | Obtener el código y ejecutar validadores. |
| JDK 17 | Gradle/Android. |
| Android SDK con `compileSdk 35` y licencias aceptadas | Compilar e instalar las aplicaciones. |
| Android Debug Bridge (ADB) | Instalar APKs en equipos de prueba. |
| ESP32/Wemos, red y E-stop | Sólo para el protocolo de laboratorio. |

No utilice archivos de `archive/` como fuente de firmware o build.

## 2. Verificación inicial del repositorio

Desde la raíz, antes de modificar o entregar:

```bash
python3 tools/validate_firmware_contract.py --quiet
python3 tools/validate_system_100.py --quiet
python3 tools/prehardware_readiness.py --quiet
python3 -m compileall -q tools
git diff --check
```

Los comandos deben finalizar sin errores. Si uno falla, corrija el archivo indicado y vuelva a ejecutar el conjunto completo.

## 3. Compilar y probar las aplicaciones Android

En un equipo con JDK 17 y Android SDK configurados:

```bash
cd config
./gradlew testAllModules
./gradlew lintAll
./gradlew buildAllApks validateApks writeApkChecksums
```

- `testAllModules`: ejecuta las pruebas JVM configuradas en los módulos.
- `lintAll`: ejecuta Android Lint en los módulos activos.
- `buildAllApks`: construye las APK debug.
- `validateApks`: verifica presencia/tamaño esperado de APKs.
- `writeApkChecksums`: genera `config/output-apks/SHA256SUMS.txt`.

En Windows, use `gradlew.bat`. Si no cuenta con JDK/SDK local, consulte el workflow **Android CIM CI** de GitHub Actions; no trate un build no ejecutado como aprobado.

## 4. Instalar y abrir el sistema

1. Verifique los artefactos en `config/output-apks/` y sus hashes.
2. Conecte el dispositivo Android de prueba y habilite depuración USB según la política del laboratorio.
3. Instale mediante ADB o use:

   ```powershell
   .\tools\powershell\Instalar-APKs.ps1
   ```

4. Abra primero **Coordinador** e inicie el hub.
5. Abra las estaciones PLC, Manufactura, Calidad y Almacén. Solicite admisión; no omita la validación de identidad.
6. Use Wear sólo como supervisión complementaria, no como control de seguridad.

## 5. Uso en simulación

Use los scripts bajo `tools/` para revisar contratos y lógica antes del banco. Registre en la bitácora el commit, escenario, resultado y logs generados. La simulación permite revisar decisiones, pero no mide cobertura radioeléctrica, cámara, potencia ni movimiento real.

## 6. Paso a laboratorio

Antes de energizar, siga exactamente:

1. [Proceso pre-hardware](deliverables/PROCESO_VALIDACION_PRE_HARDWARE.md).
2. [Manual operativo de laboratorio](deliverables/MANUAL_OPERATIVO_LABORATORIO.md).
3. [Protocolo de pruebas hardware](deliverables/PROTOCOLO_PRUEBAS_HARDWARE.md).

Comience con lógica y actuadores aislados. E-stop independiente, interlocks, supervisor y registro de evidencia son obligatorios. Detenga la prueba si la identidad no coincide, se pierde el enlace en condición de riesgo, un relé arranca activo o se observa movimiento inesperado.

## 7. Evidencia y cierre

Para cada prueba física registre en `deliverables/BITACORA_VALIDACION.md`: fecha, operador, commit, APK/firmware, dispositivo, escenario, esperado, observado, logs/capturas, incidencia y aprobación. Consulte [validación y cobertura](VALIDACION_Y_COBERTURA.md) antes de comunicar porcentajes de éxito.
