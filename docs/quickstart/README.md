# Inicio rápido

La fuente activa se compila desde `config/` y los resultados verificables se obtienen en GitHub Actions.

## Requisitos

- Python 3 para la validación estructural.
- JDK 17.
- Android SDK compatible con `compileSdk 35`.
- Licencias Android aceptadas.

## Validación local automatizable

Desde la raíz del repositorio:

```bash
python3 tools/validate_system_100.py
```

Desde `config/`:

```bash
./gradlew testAllModules
./gradlew lintAll
./gradlew buildAllApks validateApks writeApkChecksums
```

Las APK debug se exportan a `config/output-apks/` y los hashes a `config/output-apks/SHA256SUMS.txt` sólo si la compilación termina correctamente.

## Instalación rápida

```powershell
.\tools\powershell\Instalar-APKs.ps1
```

O manualmente con ADB apuntando a `config/output-apks/*.apk`.

## Estado de confianza

No utilices guías bajo `archive/` para compilar, instalar, flashear firmware ni declarar una entrega. Son snapshots históricos que pueden contener rutas personales, dependencias ausentes o afirmaciones no verificadas. Las pruebas de hardware real deben quedar registradas en la bitácora.
