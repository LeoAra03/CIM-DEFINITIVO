# simulate_esp32.ps1 — Simulación ESP32 CIM v6.0 (Wokwi / PlatformIO / Python fallback)
# Uso: .\scripts\hardware-testing\simulate_esp32.ps1 [-Mode wokwi|pio|python]

param(
    [ValidateSet("wokwi", "pio", "python", "auto")]
    [string]$Mode = "auto"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$FirmwareDir = Join-Path $Root "firmware\Firmware_Support"
$WokwiDir = Join-Path $FirmwareDir ".wokwi"
$PythonSim = Join-Path $Root "scripts\automation\automation_scripts\esp32_simulator.py"

function Write-Header($msg) {
    Write-Host ""
    Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan
}

function Test-Command($name) {
    return [bool](Get-Command $name -ErrorAction SilentlyContinue)
}

Write-Header "CIM v6.0 — Simulación ESP32"

if ($Mode -eq "auto") {
    if (Test-Command "wokwi-cli") { $Mode = "wokwi" }
    elseif (Test-Command "pio") { $Mode = "pio" }
    else { $Mode = "python" }
}

switch ($Mode) {
    "wokwi" {
        Write-Host "Modo: Wokwi CLI" -ForegroundColor Green
        if (-not (Test-Path $WokwiDir)) {
            New-Item -ItemType Directory -Force -Path $WokwiDir | Out-Null
            @"
{
  "version": 1,
  "author": "CIM v6.0",
  "editor": "wokwi",
  "parts": [
    { "type": "wokwi-esp32-devkit-v1", "id": "esp", "top": 0, "left": 0 }
  ],
  "connections": []
}
"@ | Set-Content (Join-Path $WokwiDir "diagram.json") -Encoding UTF8
            Write-Host "  Creado diagram.json base en $WokwiDir" -ForegroundColor Yellow
        }
        Set-Location $FirmwareDir
        wokwi-cli start --timeout 0
    }
    "pio" {
        Write-Host "Modo: PlatformIO (build + monitor simulado)" -ForegroundColor Green
        Set-Location $FirmwareDir
        pio run
        Write-Host "  Compilación OK. Para monitor serial: pio device monitor -b 115200" -ForegroundColor Yellow
        if (Test-Command "pio") {
            pio device monitor -b 115200
        }
    }
    "python" {
        Write-Host "Modo: Simulador Python (TCP/BT virtual)" -ForegroundColor Green
        if (-not (Test-Path $PythonSim)) {
            Write-Error "No se encontró $PythonSim"
        }
        python $PythonSim
    }
}

Write-Host "Simulación finalizada." -ForegroundColor Green
