#!/usr/bin/env pwsh
# =============================================================================
# CIM v6.0 - SCRIPT DE FLASHEO ESP32/Wemos D1 R32
# =============================================================================
# Requiere: Arduino CLI o PlatformIO instalado.
# Uso: .\Flashear-ESP32.ps1 -Port COM3 [-Firmware esp32_plc_master.ino]

param(
    [string]$Port = "COM3",
    [string]$FirmwareDir,
    [string]$Firmware = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..\..")
if ([string]::IsNullOrWhiteSpace($FirmwareDir)) {
    $FirmwareDir = Join-Path $ProjectRoot "esp32\firmware"
}

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   CIM v6.0 - FLASHEADOR ESP32/Wemos" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "`nPuerto seleccionado: $Port" -ForegroundColor Yellow
Write-Host "Directorio firmware: $FirmwareDir" -ForegroundColor Yellow

$firmwares = if ([string]::IsNullOrWhiteSpace($Firmware)) {
    @(
        "esp32_plc_master.ino",
        "esp32_scorbot_manufactura.ino",
        "esp32_scorbot_calidad.ino",
        "esp32_scorbot_almacen.ino"
    )
} else {
    @($Firmware)
}

$arduinoCli = Get-Command arduino-cli -ErrorAction SilentlyContinue
$platformIo = Get-Command pio -ErrorAction SilentlyContinue

foreach ($fw in $firmwares) {
    $fullPath = Join-Path $FirmwareDir $fw
    if (-not (Test-Path $fullPath)) {
        Write-Host "  ✗ No se encontró $fw en $FirmwareDir" -ForegroundColor Red
        continue
    }

    Write-Host "`nPreparando $fw..." -ForegroundColor Green
    if ($arduinoCli) {
        & $arduinoCli.Source compile --fqbn esp32:esp32:esp32 $fullPath --upload -p $Port
        if ($LASTEXITCODE -ne 0) { throw "arduino-cli falló para $fw" }
    } elseif ($platformIo) {
        & $platformIo.Source run -d $FirmwareDir --target upload --upload-port $Port
        if ($LASTEXITCODE -ne 0) { throw "PlatformIO falló para $fw" }
    } else {
        Write-Host "  ⚠ arduino-cli o platformio no encontrado." -ForegroundColor Yellow
        Write-Host "  Abre manualmente este archivo en Arduino IDE: $fullPath" -ForegroundColor Yellow
    }
}

Write-Host "`n==============================================" -ForegroundColor Cyan
Write-Host "   PROCESO DE FLASHEO FINALIZADO" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "`nNotas:"
Write-Host "- Verifica el nombre BLE y UUID canónico en esp32/firmware/README.md."
Write-Host "- Reinicia el ESP32 después de flashear."
Write-Host "- No conectes actuadores reales sin E-stop físico e interlocks verificados."
