# Instalar APKs — CIM v6.0 (portable)
# Uso: .\Instalar-APKs.ps1 [-Device emulator-5554]

param(
    [string]$Device = $null
)

$ErrorActionPreference = "Stop"
$ApkDir = Join-Path (Split-Path $PSScriptRoot -Parent) "2_APK_ANDROID"

$Apks = @(
    @{ File = "app-coordinador.apk"; Name = "Hub Coordinador" },
    @{ File = "app-plc.apk"; Name = "Estación PLC / Cinta" },
    @{ File = "app-manufactura.apk"; Name = "Manufactura Robot+Láser" },
    @{ File = "app-calidad.apk"; Name = "Calidad / Visión" },
    @{ File = "app-almacen.apk"; Name = "Almacén" }
)

function Test-Adb {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        Write-Error @"
ADB no está en PATH.
Instale Android SDK platform-tools o Android Studio.
Agregue al PATH: ...\Android\Sdk\platform-tools
"@
    }
    adb version | Select-Object -First 1
}

Write-Host "`n=== CIM v6.0 — Instalación de APKs ===" -ForegroundColor Cyan
Test-Adb | Out-Null

$adbBase = @("install", "-r")
if ($Device) { $adbBase = @("-s", $Device) + $adbBase }

Write-Host "Dispositivos conectados:" -ForegroundColor Gray
adb devices

foreach ($apk in $Apks) {
    $path = Join-Path $ApkDir $apk.File
    if (-not (Test-Path $path)) {
        Write-Warning "No encontrado: $($apk.File) — omitiendo"
        continue
    }
    Write-Host "`nInstalando $($apk.Name) ..." -ForegroundColor Yellow
    & adb @adbBase $path
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  OK: $($apk.File)" -ForegroundColor Green
    } else {
        Write-Warning "  Falló: $($apk.File) (código $LASTEXITCODE)"
    }
}

Write-Host "`n=== Instalación completada ===" -ForegroundColor Cyan
Write-Host "Siguiente: flashear ESP32 (carpeta 3_FIRMWARE_ESP32) y abrir app-coordinador." -ForegroundColor Gray
