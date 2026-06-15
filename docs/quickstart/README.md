# Guía rápida de uso

## 1. Instalar APKs

Ejecuta desde la raíz del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install_apks.ps1
```

## 2. Flashear ESP32

Desde la carpeta de firmware:

```powershell
cd firmware\Firmware_Support
pio run -t upload -e esp32dev
```

## 3. Ver logs del ESP32

```powershell
cd firmware\Firmware_Support
pio device monitor -b 115200
```

## 4. Compilar una app Android

```powershell
./gradlew.bat :app-coordinador:app:assembleDebug
```
