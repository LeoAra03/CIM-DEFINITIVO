#!/usr/bin/env pwsh
<#
.SYNOPSIS
Copia APKs compiladas a android/apks/ y genera un resumen

.DESCRIPTION
Script que:
1. Espera a que Gradle termine (si aún está corriendo)
2. Copia las APKs de config/output-apks/ a android/apks/
3. Genera un manifest de instalación
4. Reporta el estado final
#>

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$OutputDir = Join-Path $ProjectRoot "config\output-apks"
$ApkDir = Join-Path $ProjectRoot "android\apks"
$BuildLog = Join-Path $ProjectRoot "build_log.txt"

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  CIM v6.0 - DEPLOY APKs a android/apks/" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan

# Esperar a que Gradle termine
Write-Host "`n⏳ Verificando si Gradle está activo..." -ForegroundColor Yellow
$maxWait = 0
while ((Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object {$_.CommandLine -like "*gradle*"}).Count -gt 0 -and $maxWait -lt 120) {
    Write-Host "   Gradle aún corriendo... ($maxWait/120s)" -ForegroundColor Gray
    Start-Sleep -Seconds 10
    $maxWait += 10
}

if ($maxWait -ge 120) {
    Write-Host "   ⚠ Timeout esperando Gradle (2 min)" -ForegroundColor Yellow
}

# Verificar si output-apks existe
if (-not (Test-Path $OutputDir)) {
    Write-Host "`n❌ ERROR: No se encontró output-apks en config/" -ForegroundColor Red
    exit 1
}

# Listar APKs compiladas
$apks = Get-ChildItem -Path $OutputDir -Filter "*.apk" -ErrorAction SilentlyContinue

if ($apks.Count -eq 0) {
    Write-Host "`n❌ ERROR: No hay APKs compiladas en $OutputDir" -ForegroundColor Red
    Write-Host "`n📋 Últimas líneas del log:" -ForegroundColor Gray
    if (Test-Path $BuildLog) {
        Get-Content $BuildLog -Tail 20 | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
    }
    exit 1
}

# Crear android/apks si no existe
if (-not (Test-Path $ApkDir)) {
    New-Item -ItemType Directory -Path $ApkDir | Out-Null
}

Write-Host "`n✓ Encontradas $($apks.Count) APKs compiladas:" -ForegroundColor Green
$totalSize = 0
$apks | ForEach-Object {
    $sizeMB = [Math]::Round($_.Length / 1MB, 2)
    $totalSize += $_.Length
    Write-Host "   📦 $($_.Name) ($sizeMB MB)" -ForegroundColor Cyan
}
Write-Host "   TOTAL: $([Math]::Round($totalSize / 1MB, 2)) MB" -ForegroundColor Cyan

# Copiar APKs
Write-Host "`n📤 Copiando APKs a android/apks/..." -ForegroundColor Yellow
$copied = 0
$apks | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination (Join-Path $ApkDir $_.Name) -Force | Out-Null
    Write-Host "   ✓ Copiado: $($_.Name)" -ForegroundColor Green
    $copied++
}

# Generar manifest JSON
$manifest = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    version = "6.0.0"
    apks = @()
}

Get-ChildItem -Path $ApkDir -Filter "*.apk" | ForEach-Object {
    $manifest.apks += @{
        name = $_.Name
        size_mb = [Math]::Round($_.Length / 1MB, 2)
        path = $_.FullName
        md5 = (Get-FileHash $_.FullName -Algorithm MD5).Hash
    }
}

$manifest | ConvertTo-Json | Set-Content (Join-Path $ApkDir "manifest.json") -Encoding UTF8

# Generar INSTALL_GUIDE.md
$installGuide = @"
# 📦 Instalación de APKs CIM v6.0

## APKs Disponibles

| App | Módulo | Descripción |
|-----|--------|------------|
| app-coordinador.apk | Maestro | Hub central de coordinación |
| app-plc.apk | Estación | Control de PLC y E-STOP |
| app-manufactura.apk | Estación | Mecanizado, Robot Scorbot + Láser |
| app-calidad.apk | Estación | Visión + QC |
| app-almacen.apk | Estación | Gestión de inventario |
| wear-coordinador.apk (opcional) | Wearable | Control desde reloj inteligente |

## Instalación Rápida

### 1. Desde Android Studio
\`\`\`bash
adb install -r app-coordinador.apk
adb install -r app-plc.apk
adb install -r app-manufactura.apk
adb install -r app-calidad.apk
adb install -r app-almacen.apk
\`\`\`

### 2. Desde Teléfono (transferencia manual)
1. Conectar dispositivo a PC
2. Copiar .apk a carpeta de descargas del teléfono
3. Permitir instalación de fuentes desconocidas
4. Abrir archivo y instalar

## Verificación Post-Instalación

- ✓ APP-COORDINADOR: Debe iniciar un Hub en 192.168.1.100:8888
- ✓ Otras apps: Deben conectarse al coordinador automáticamente (NSD)
- ✓ Logs en cada app mostrarán "[NET]" cuando se conecten

## Información Técnica

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Soporte**: Hilt DI, Jetpack Compose, BLE + WiFi
- **Tamaño típico**: 150-180 MB por app (incluye todas las librerías)

---
*Generado: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")*
"@

$installGuide | Set-Content (Join-Path $ApkDir "INSTALL_GUIDE.md") -Encoding UTF8

# Generar resumen final
Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  ✓ DEPLOY COMPLETADO EXITOSAMENTE" -ForegroundColor Green
Write-Host "║" -ForegroundColor Green
Write-Host "║  📦 APKs copias: $copied" -ForegroundColor Green
Write-Host "║  📁 Ubicación: android/apks/" -ForegroundColor Green
Write-Host "║  📋 Manifest: manifest.json" -ForegroundColor Green
Write-Host "║  📖 Guía: INSTALL_GUIDE.md" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green

Write-Host "`n✓ Listo para instalar en dispositivos reales" -ForegroundColor Green
