# =============================================================================
# CIM v6.0 - SCRIPT DE FLASHEO ESP32
# =============================================================================
# Requiere: Arduino CLI o PlatformIO instalado
# Uso: .\Flashear-ESP32.ps1 [puerto]

param(
    [string]$Port = "COM3",
    [string]$FirmwareDir = "..\3_FIRMWARE_ESP32"
)

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   CIM v6.0 - FLASHEADOR ESP32" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

$firmwares = @(
    "cim_scorbot_firmware.ino",
    "cim_plc_firmware.ino",
    "cim_calidad_firmware.ino"
)

Write-Host "`nPuerto seleccionado: $Port" -ForegroundColor Yellow

foreach ($fw in $firmwares) {
    $fullPath = Join-Path $FirmwareDir $fw
    if (Test-Path $fullPath) {
        Write-Host "`nFlasheando $fw..." -ForegroundColor Green
        
        # Usar arduino-cli si está disponible
        if (Get-Command arduino-cli -ErrorAction SilentlyContinue) {
            arduino-cli compile --fqbn esp32:esp32:esp32 $fullPath --upload -p $Port
        } 
        # Usar platformio si está disponible
        elseif (Get-Command pio -ErrorAction SilentlyContinue) {
            pio run -d $FirmwareDir --target upload --upload-port $Port
        } 
        else {
            Write-Host "  ⚠ arduino-cli o platformio no encontrado. Copia manualmente el .ino a Arduino IDE" -ForegroundColor Yellow
            Write-Host "  Archivo: $fullPath" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ⚠ No se encontró $fw" -ForegroundColor Yellow
    }
}

Write-Host "`n==============================================" -ForegroundColor Cyan
Write-Host "   FLASHEO COMPLETADO" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

Write-Host "`nNotas:"
Write-Host "- Cada ESP32 debe tener un nombre único (DEVICE_NAME en el .ino)"
Write-Host "- Reinicia el ESP32 después de flashear"
Write-Host "- El LED parpadeará cuando reciba comandos BLE"