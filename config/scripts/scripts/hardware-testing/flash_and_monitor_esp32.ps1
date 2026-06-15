# flash_and_monitor_esp32.ps1 — Flasheo y monitor serial ESP32 CIM v6.0
# Uso: .\scripts\hardware-testing\flash_and_monitor_esp32.ps1 [-Port COM3] [-SkipUpload]

param(
    [string]$Port = "",
    [switch]$SkipUpload,
    [int]$Baud = 115200
)

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$FirmwareDir = Join-Path $Root "firmware\Firmware_Support"

function Write-Header($msg) {
    Write-Host ""
    Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan
}

if (-not (Get-Command pio -ErrorAction SilentlyContinue)) {
    Write-Error "PlatformIO CLI (pio) no está en PATH. Instale PlatformIO Core o use la extensión VS Code."
}

Write-Header "CIM v6.0 — Flash + Monitor ESP32"
Set-Location $FirmwareDir

Write-Host "Directorio firmware: $FirmwareDir" -ForegroundColor Gray

# Compilar siempre
Write-Host "Compilando firmware..." -ForegroundColor Yellow
pio run
if ($LASTEXITCODE -ne 0) { Write-Error "pio run falló" }

if (-not $SkipUpload) {
    $uploadArgs = @("run", "-t", "upload")
    if ($Port) {
        $uploadArgs += @("--upload-port", $Port)
        Write-Host "Puerto de upload: $Port" -ForegroundColor Gray
    } else {
        Write-Host "Puerto: auto-detect (PlatformIO)" -ForegroundColor Gray
    }
    Write-Host "Flasheando ESP32..." -ForegroundColor Yellow
    & pio @uploadArgs
    if ($LASTEXITCODE -ne 0) { Write-Error "pio upload falló — verifique cable USB y driver CP210x/CH340" }
    Write-Host "✓ Firmware flasheado correctamente" -ForegroundColor Green
}

Write-Host "Iniciando monitor serial ($Baud baud)..." -ForegroundColor Yellow
Write-Host "  Ctrl+C para salir" -ForegroundColor Gray
$monitorArgs = @("device", "monitor", "-b", "$Baud")
if ($Port) { $monitorArgs += @("--port", $Port) }
& pio @monitorArgs
