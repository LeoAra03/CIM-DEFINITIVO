#!/usr/bin/env pwsh
# =============================================================================
# CIM v6.0 - SCRIPT DE INSTALACIÓN DE APKs
# =============================================================================
# Requiere: ADB instalado y dispositivos Android conectados por USB o WiFi.
# Uso: .\Instalar-APKs.ps1 [-ApkDir ruta]

param(
    [string]$ApkDir = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..\..")
if ([string]::IsNullOrWhiteSpace($ApkDir)) {
    $candidate = Join-Path $ProjectRoot "config\output-apks"
    $fallback = Join-Path $ProjectRoot "android\apks"
    $ApkDir = if (Test-Path $candidate) { $candidate } else { $fallback }
}

$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    Write-Host "✗ ADB no está instalado o no está en PATH." -ForegroundColor Red
    exit 1
}

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   CIM v6.0 - INSTALADOR DE APKs" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "Directorio APK: $ApkDir" -ForegroundColor Yellow

$apks = @(
    "app-coordinador.apk",
    "app-plc.apk",
    "app-manufactura.apk",
    "app-calidad.apk",
    "app-almacen.apk",
    "wear-coordinador.apk"
)

if (-not (Test-Path $ApkDir)) {
    Write-Host "✗ No existe el directorio de APKs: $ApkDir" -ForegroundColor Red
    Write-Host "Ejecuta primero desde config/: ./gradlew buildAllApks" -ForegroundColor Yellow
    exit 1
}

Write-Host "`nBuscando dispositivos Android..." -ForegroundColor Yellow
& $adb.Source devices

$installed = 0
foreach ($apk in $apks) {
    $fullPath = Join-Path $ApkDir $apk
    if (Test-Path $fullPath) {
        Write-Host "`nInstalando $apk..." -ForegroundColor Green
        & $adb.Source install -r $fullPath
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✓ $apk instalado correctamente" -ForegroundColor Green
            $installed++
        } else {
            Write-Host "  ✗ Error instalando $apk" -ForegroundColor Red
        }
    } else {
        Write-Host "  ⚠ No se encontró $apk en $ApkDir" -ForegroundColor Yellow
    }
}

Write-Host "`n==============================================" -ForegroundColor Cyan
Write-Host "   INSTALACIÓN FINALIZADA ($installed/$($apks.Count))" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

Write-Host "`nPasos siguientes:"
Write-Host "1. Abre app-coordinador y pulsa START HUB."
Write-Host "2. En cada estación, usa la IP descubierta o ingresa la IP del HUB."
Write-Host "3. Autoriza las conexiones desde el Coordinador."
Write-Host "4. Para pruebas sin hardware, activa el modo autónomo/simulado."
