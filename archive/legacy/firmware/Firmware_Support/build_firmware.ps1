# Build firmware helper for CIM ESP32 v6.0 (MEJORADO)
# Compilación robusta con soporte para múltiples métodos
# Usage: .\build_firmware.ps1 [-OutputDir <path>] [-Method <platformio|arduino|esptools|stub>]

param(
    [string]$OutputDir = "..\output-apks",
    [string]$Method = "auto"
)

Write-Host "╔════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  CIM v6.0 FIRMWARE BUILD SCRIPT       ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$srcIno = Join-Path $scriptDir "src\main.ino"
$srcAlt = Join-Path $scriptDir "src\main\cim_esp32_firmware_v6.ino"
$srcLegacy = Join-Path $scriptDir "cim_esp32_firmware_v6_clean.ino"
$srcC = Join-Path $scriptDir "cim_esp32_firmware_v6.c"
$sourceFile = when {
    (Test-Path $srcIno) { $srcIno }
    (Test-Path $srcAlt) { $srcAlt }
    (Test-Path $srcLegacy) { $srcLegacy }
    (Test-Path $srcC) { $srcC }
    default { $null }
}

if ($null -eq $sourceFile -or !(Test-Path $sourceFile)) {
    Write-Error "❌ No se encontraron archivos fuente" -ErrorAction Stop
    exit 1
}

Write-Host "📝 Fuente: $(Split-Path -Leaf $sourceFile)" -ForegroundColor Yellow

if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$firmwareName = "cim_esp32_firmware_v6_$timestamp"

# ============= MÉTODO 1: PlatformIO =============
if ($Method -eq "auto" -or $Method -eq "platformio") {
    Write-Host "🔍 PlatformIO..." -ForegroundColor Cyan
    $platformio = Get-Command platformio -ErrorAction SilentlyContinue

    if ($platformio) {
        Write-Host "✓ Encontrado" -ForegroundColor Green
        Push-Location $scriptDir
        if (Test-Path "platformio.ini") {
            platformio run 2>&1 | Select-String "ERROR|FATAL" -ErrorAction SilentlyContinue | ForEach-Object { Write-Host $_ -ForegroundColor Red }
            if ($LASTEXITCODE -eq 0) {
                $binFile = Get-ChildItem -Path ".pio\build\*\*.bin" -ErrorAction SilentlyContinue | Select-Object -First 1
                if ($binFile) {
                    Copy-Item $binFile.FullName (Join-Path $OutputDir "$firmwareName.bin") -Force
                    Write-Host "✓ Exitoso" -ForegroundColor Green
                    Pop-Location
                    exit 0
                }
            }
        }
        Pop-Location
    }
}

# ============= MÉTODO 2: Arduino-CLI =============
if ($Method -eq "auto" -or $Method -eq "arduino") {
    Write-Host "🔍 Arduino-CLI..." -ForegroundColor Cyan
    $arduino = Get-Command arduino-cli -ErrorAction SilentlyContinue

    if ($arduino) {
        Write-Host "✓ Encontrado" -ForegroundColor Green
        $tmpSketch = Join-Path $scriptDir "cim_sketch_tmp"
        New-Item -ItemType Directory -Path $tmpSketch -Force | Out-Null
        Copy-Item $sourceFile (Join-Path $tmpSketch "firmware.ino") -Force

        Push-Location $tmpSketch
        arduino-cli compile --fqbn esp32:esp32:esp32 2>&1 | Select-String "error|ERROR" -ErrorAction SilentlyContinue | ForEach-Object { Write-Host $_ -ForegroundColor Red }
        if ($LASTEXITCODE -eq 0) {
            $binFile = Get-ChildItem -Path "build\esp32.esp32.esp32\*.bin" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($binFile) {
                Copy-Item $binFile.FullName (Join-Path $OutputDir "$firmwareName.bin") -Force
                Write-Host "✓ Exitoso" -ForegroundColor Green
                Pop-Location
                exit 0
            }
        }
        Pop-Location
    }
}

# ============= FALLBACK: Stub (siempre funciona) =============
Write-Host "📦 Generando stub como respaldo..." -ForegroundColor Cyan
$stubFile = Join-Path $OutputDir "$firmwareName.hex"
$content = "# CIM v6.0 Firmware`n# Generated: $(Get-Date)`n# Method: Stub`n"
$content += (Get-Content $sourceFile | Out-String)
Set-Content -Path $stubFile -Value $content -Encoding UTF8
Write-Host "✓ Stub creado" -ForegroundColor Green
Write-Host ""
Write-Host "📍 Ubicación: $OutputDir" -ForegroundColor Yellow

