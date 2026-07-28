# CIM-DEFINITIVO

Sistema CIM (Computer Integrated Manufacturing) con cinco aplicaciones Android, una biblioteca de red compartida y firmware ESP32 para las estaciones de Coordinación, PLC, Manufactura, Calidad y Almacenamiento.

> **Estado de validación:** la integración continua está configurada, pero la última ejecución no superó los tests. No se debe interpretar este repositorio como una entrega validada hasta que CI esté en verde y las pruebas con hardware se documenten.

## Estructura activa

```text
CIM-DEFINITIVO/
├── android/                 # Código Android: 5 apps + core-network
├── config/                  # Build Gradle centralizado
├── esp32/                   # Firmware ESP32 y utilidades de estación activas
├── tools/                   # Scripts operativos y simuladores
├── docs/                    # Guías, arquitectura, auditorías y quickstart
├── entrega/                 # Informes seleccionados para la entrega
├── logs/                    # Convenciones de registro
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

## Compilación

Requiere JDK 17, Android SDK y licencias Android aceptadas.

```bash
cd config
./gradlew testAllModules
./gradlew buildAllApks
```

Las APK debug se exportan en `config/output-apks/`.

Consulta [la guía de inicio](docs/quickstart/README.md) y los resultados de CI en la pestaña **Actions** del repositorio.

## Convenciones importantes

- `archive/` contiene material histórico y **no** es una fuente operativa.
- `esp32/firmware/README.md` define el firmware a utilizar; no flashear archivos desde `archive/`.
- No incluir APK, keystores, claves, modelos duplicados ni archivos generados en commits.
- Los informes deben describir resultados verificables de CI y pruebas de hardware; no declarar funcionalidades no probadas.
