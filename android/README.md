# Android

Código Android activo del proyecto CIM.

## Contenido

- `core-network/`: biblioteca compartida de protocolo, red, BLE, visión y utilidades.
- `apps/app-coordinador/`: hub de coordinación.
- `apps/app-plc/`: estación PLC/cinta.
- `apps/app-manufactura/`: estación robot/láser.
- `apps/app-calidad/`: estación de visión/calidad.
- `apps/app-almacen/`: estación de almacenamiento.
- `apps/wear-coordinador/`: app Wear OS de supervisión.
- `apks/`: documentación de instalación; no versiona APKs.

## Build rápido

```bash
cd config
./gradlew testAllModules lintAll buildAllApks validateApks writeApkChecksums
```

Los artefactos quedan en `config/output-apks/`.

## Instalación

Usa `tools/powershell/Instalar-APKs.ps1` o instala manualmente con ADB desde `config/output-apks/`.
