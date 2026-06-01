# Para mañana

Esta guía es la versión corta para preparar la sesión de mañana.

## Carpeta personal

- La carpeta `.para-maniana/` contiene los manuales clave para el laboratorio y está ignorada por Git.
- No hay que subir esa carpeta al repositorio.

## Qué llevar

- Laptop con el proyecto `Practica_2` y acceso a `output-apks/`
- Cable USB de datos y cargador
- ESP32 con `cim_esp32_firmware_v6.bin` listo
- 5 dispositivos Android con APKs o capacidad para instalarlas
- Router Wi-Fi 2.4 GHz sin aislamiento de cliente
- Marcadores ArUco impresos (para la estación Calidad)
- Cuaderno o etiquetas para anotar IP y MAC autorizadas

## Documentos clave en `.para-maniana/`

- `GUIA_LABORATORIO_MANANA.md`
- `GUIA_ENTREGA_FINAL.md`
- `GUIA_ESTACIONES_TRABAJO.md`
- `DESPLIEGUE_Y_CONFIGURACION.md`
- `README.md` (resumen de qué usar)

## Comandos rápidos

```powershell
cd "c:\Users\Leo\Desktop\Test Practica2\Practica_2"
.\gradlew testAllModules buildAllApks
.\scripts\hardware-testing\flash_and_monitor_esp32.ps1
```

## Nota

- La carpeta `.para-maniana` es tu espacio personal para mañana; úsala como referencia principal y evita confundirla con otras carpetas de entrega.
