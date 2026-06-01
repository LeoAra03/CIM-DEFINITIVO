# Flashear-ESP32.ps1 — CIM v6.0 firmware portable (sin clonar repo completo)
# Uso: .\Flashear-ESP32.ps1 -Port COM3
# Requiere: esptool (pip install esptool) o PlatformIO (pio)

param(
    [Parameter(Mandatory = $true)]
    [string]$Port,
    [switch]$MonitorOnly,
    [int]$Baud = 115200
)

$ErrorActionPreference = "Stop"
$Bin = Join-Path $PSScriptRoot "cim_esp32_firmware_v6.bin"

if (-not (Test-Path $Bin)) {
    Write-Error "No se encuentra cim_esp32_firmware_v6.bin en $PSScriptRoot"
}

Write-Host "`n=== CIM v6.0 — Flash ESP32 ===" -ForegroundColor Cyan
Write-Host "Binario: $Bin" -ForegroundColor Gray
Write-Host "Puerto:  $Port" -ForegroundColor Gray

if ($MonitorOnly) {
    if (Get-Command pio -ErrorAction SilentlyContinue) {
        & pio device monitor --port $Port -b $Baud
        exit $LASTEXITCODE
    }
    Write-Error "Monitor requiere PlatformIO (pio). Instale: pip install platformio"
}

# Intentar esptool.py
$esptool = $null
foreach ($cmd in @("esptool.py", "esptool", "python -m esptool")) {
    if ($cmd -eq "python -m esptool") {
        try {
            python -m esptool version 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) { $esptool = "python -m esptool"; break }
        } catch {}
    } elseif (Get-Command ($cmd -split ' ')[0] -ErrorAction SilentlyContinue) {
        $esptool = $cmd
        break
    }
}

if ($esptool) {
    Write-Host "Flasheando con esptool (0x10000)..." -ForegroundColor Yellow
    if ($esptool -eq "python -m esptool") {
        python -m esptool --chip esp32 --port $Port write_flash 0x10000 $Bin
    } else {
        & $esptool --chip esp32 --port $Port write_flash 0x10000 $Bin
    }
    if ($LASTEXITCODE -ne 0) { Write-Error "esptool falló — verifique puerto y driver USB" }
    Write-Host "Firmware flasheado correctamente." -ForegroundColor Green
    Write-Host "Monitor serial (115200): use Arduino IDE o 'pio device monitor -b 115200 --port $Port'" -ForegroundColor Gray
    exit 0
}

# Fallback: PlatformIO si tiene firmware fuente en otro lugar
if (Get-Command pio -ErrorAction SilentlyContinue) {
    Write-Warning "esptool no encontrado. Instale: pip install esptool"
    Write-Host "Alternativa: copie cim_esp32_firmware_v6.bin a su proyecto PlatformIO y use 'pio run -t upload'" -ForegroundColor Yellow
    exit 1
}

Write-Error @"
No se encontró esptool ni PlatformIO.

Instale esptool:
  pip install esptool

Luego ejecute de nuevo:
  .\Flashear-ESP32.ps1 -Port COM3
"@
