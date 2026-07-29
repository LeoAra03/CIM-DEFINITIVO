# CIM-DEFINITIVO

Sistema CIM (Computer Integrated Manufacturing) con cinco aplicaciones Android, una app Wear, una biblioteca de red compartida y firmware ESP32/Wemos D1 R32 para las estaciones de Coordinación, PLC, Manufactura, Calidad y Almacenamiento.

> **Estado de validación automática:** el proyecto cuenta con CI para `testAllModules` y `buildAllApks`; además dispone de puertas locales/Gradle para `lintAll`, validación de APKs, checksums y `tools/validate_system_100.py`. La validación estructural de simulación debe marcar 100% antes de declarar una entrega automatizable. Las pruebas físicas de hardware, E-stop, relés, robot y láser siguen requiriendo evidencia de laboratorio.

## Estructura activa

```text
CIM-DEFINITIVO/
├── android/                 # Código Android: 5 apps + Wear + core-network
├── config/                  # Build Gradle centralizado
├── esp32/                   # Firmware ESP32/Wemos activo de estación
├── tools/                   # Scripts operativos, validadores y simuladores
├── docs/                    # Guías, arquitectura, auditorías y quickstart
├── entrega/                 # Informes seleccionados para la entrega
├── logs/                    # Convenciones y reportes de validación
└── archive/                 # Historial y snapshots; no se compilan ni ejecutan
```

## Aplicaciones Android

| Módulo | Application ID | Responsabilidad |
|---|---|---|
| `app-coordinador` | `com.industria.coordinacion` | Hub, autorización y orquestación CIM |
| `app-plc` | `com.industria.plc` | Cinta, sensores y eventos de pallet |
| `app-manufactura` | `com.industria.manufactura` | Robot, láser y G-code |
| `app-calidad` | `com.industria.calidad` | Visión, ArUco y control de calidad |
| `app-almacen` | `com.industria.almacenamiento` | Rack, almacenamiento y retiro |
| `wear-coordinador` | `com.industria.wear` | Supervisión compacta desde Wear OS |

## Validación 100% automatizable (simulación)

```bash
python3 tools/validate_system_100.py
```

Esta puerta verifica estructura activa, herramientas, firmware canónico, documentación, ausencia de APKs versionadas y configuración de CI. **No sustituye** ensayos con hardware real.

## Compilación

Requiere JDK 17, Android SDK y licencias Android aceptadas.

```bash
cd config
./gradlew testAllModules
./gradlew lintAll
./gradlew buildAllApks validateApks writeApkChecksums
```

Las APK debug se exportan en `config/output-apks/` junto con `SHA256SUMS.txt`.

Consulta [la guía de inicio](docs/quickstart/README.md) y los resultados de CI en la pestaña **Actions** del repositorio.

## Convenciones importantes

- `archive/` contiene material histórico y **no** es una fuente operativa.
- `esp32/firmware/README.md` define el firmware a utilizar; no flashear archivos desde `archive/`.
- No incluir APK, keystores, claves, modelos duplicados ni archivos generados en commits.
- La firma release usa variables/propiedades locales (`CIM_RELEASE_*`); nunca hardcodear contraseñas en Gradle.
- Los informes deben describir resultados verificables de CI y pruebas de hardware; no declarar funcionalidades no probadas.

## Entrega pre-hardware y laboratorio

La entrega pre-hardware de Leonardo Araya se encuentra en [docs/deliverables/ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.md](docs/deliverables/ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.md) (con su PDF). Reúne el protocolo de pruebas, el manual de laboratorio, la matriz de riesgos y el semáforo de preparación. El índice navegable está en [docs/INDEX_REPOSITORIO.md](docs/INDEX_REPOSITORIO.md).

Antes de llevar un commit al banco, ejecute:

```bash
python3 tools/validate_firmware_contract.py --quiet
python3 tools/validate_system_100.py --quiet
python3 tools/prehardware_readiness.py --quiet
python3 -m compileall -q tools
git diff --check
```

Estos controles sólo verifican contratos, estructura, documentación y sintaxis. **No autorizan energizar actuadores ni reemplazan E-stop, interlocks, supervisión ni pruebas de hardware.** Para cualquier ensayo físico siga `docs/deliverables/MANUAL_OPERATIVO_LABORATORIO.md` y registre evidencia en la bitácora.
