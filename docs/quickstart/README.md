# Inicio rápido

La fuente activa se compila desde `config/` y los resultados verificables se obtienen en GitHub Actions.

## Requisitos

- JDK 17
- Android SDK compatible con `compileSdk 35`
- Licencias Android aceptadas

## Validación local

```bash
cd config
./gradlew testAllModules
./gradlew buildAllApks
```

Las APK de depuración se exportan a `config/output-apks/` sólo si la compilación termina correctamente.

## Estado de confianza

No utilices guías bajo `archive/` para compilar, instalar, flashear firmware ni declarar una entrega. Son snapshots históricos que pueden contener rutas personales, dependencias ausentes o afirmaciones no verificadas.
