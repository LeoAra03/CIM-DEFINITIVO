# APKs

Esta carpeta no versiona APKs. Las APK debug se generan desde `config/`:

```bash
cd config
./gradlew buildAllApks validateApks writeApkChecksums
```

Artefactos esperados en `config/output-apks/`:

- `app-coordinador.apk`
- `app-plc.apk`
- `app-manufactura.apk`
- `app-calidad.apk`
- `app-almacen.apk`
- `wear-coordinador.apk`
- `SHA256SUMS.txt`

Si necesitas copiar los APKs a esta carpeta para instalación manual, usa `tools/powershell/copy_apks.ps1` después del build. No agregues APKs generadas al repositorio.

## Descarga de las APKs ya compiladas (CI)

Cada push a la rama de trabajo dispara el workflow **Android CIM CI**, que ejecuta los
tests unitarios, compila las seis APKs debug y las publica como artefacto.

- Ultimo build correcto: run `31615348234` (Build and unit tests, JDK 17, verde).
- Artefacto: **cim-debug-apks** (contiene los seis `.apk` listados arriba).
- Descarga desde el navegador:
  <https://github.com/LeoAra03/CIM-DEFINITIVO/actions/runs/31615348234>
  -> seccion *Artifacts* -> `cim-debug-apks`.
- Descarga por linea de comandos:

```bash
gh run download 31615348234 -n cim-debug-apks -D android/apks
```

Instalacion en el dispositivo:

```bash
adb install -r android/apks/app-coordinador.apk
```
