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
