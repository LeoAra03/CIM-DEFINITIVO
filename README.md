# Sistema CIM v6.0 — Control Industrial Distribuido

Proyecto personal de entrega: 5 apps Android + `core-network` + firmware ESP32.

## Qué hay aquí

- `app-coordinador/` — Hub maestro con autorización Bluetooth y red TCP
- `app-plc/` — estación de cinta transportadora / PLC
- `app-manufactura/` — estación de robot + láser CNC
- `app-calidad/` — estación de visión OpenCV ArUco/QR
- `app-almacen/` — estación de logística y almacenamiento
- `core-network/` — lógica compartida de comunicación
- `firmware/Firmware_Support/` — firmware ESP32
- `output-apks/` — APKs debug actuales y firmware listo para instalar
- `.para-maniana/` — mi carpeta personal para la demo de mañana (ignorada por Git)

## Documentos clave

- `docs/GUIA_LABORATORIO_MANANA.md` — checklist y pasos de laboratorio
- `docs/ENTREGA_FINAL_LEONARDO_ARAYA.md` — informe de entrega
- `CHANGELOG_FIXES.md` — correcciones recientes
- `.para-maniana/README.md` — guía personal de mañana

## Compilación rápida

```powershell
# Compila todos los módulos y exporta las APKs a output-apks/
.\gradlew testAllModules buildAllApks
```

Para firmware ESP32:

```powershell
cd firmware\Firmware_Support
pio run
```

Para copiar las APKs actuales a tu carpeta personal:

```powershell
.\scripts\copy_outputs_for_tomorrow.ps1
```

## Instalación rápida

- Usa `output-apks/` o `.para-maniana/APKS_INSTALABLES/`
- Instala con `adb install -r`
- Abre primero `app-coordinador`, luego las demás estaciones
- Autoriza dispositivos desde el hub y prueba la sincronización

## Estado actual

- APKs listas en `output-apks/`
- Carpeta personal `.para-maniana/` preparada para la entrega
- `.para-maniana/` está en `.gitignore`, no se sube al repositorio

---

*Entrega personal preparada para la demo.*
