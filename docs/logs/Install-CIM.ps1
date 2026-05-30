#!/usr/bin/env powershell
<#
.SYNOPSIS
    Script de instalación automática del Sistema CIM v6.0
.DESCRIPTION
    Instala todas las APKs en dispositivos Android conectados via USB
    y verifica la conectividad del sistema completo
#>

param(
    [string]$Mode = "install",  # install, uninstall, verify, logs
    [string]$Device = $null
)

$ProjectPath = "C:\Users\Leo\Desktop\Test Practica2\Practica_2"
$OutputDir = "$ProjectPath\output-apks"

# Colores para output
function Write-Success { Write-Host $args -ForegroundColor Green -BackgroundColor Black }
function Write-Error { Write-Host $args -ForegroundColor Red -BackgroundColor Black }
function Write-Info { Write-Host $args -ForegroundColor Cyan -BackgroundColor Black }
function Write-Warning { Write-Host $args -ForegroundColor Yellow -BackgroundColor Black }

# ===== FUNCIONES =====

function Check-ADB {
    Write-Info "🔍 Verificando ADB..."
    try {
        $version = adb version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Success "✅ ADB disponible"
            return $true
        }
    } catch {
        Write-Error "❌ ADB no encontrado. Instala Android SDK Platform Tools."
        return $false
    }
}

function List-Devices {
    Write-Info "📱 Dispositivos conectados:"
    $devices = adb devices | Select-Object -Skip 1 | Where-Object { $_.Length -gt 0 }
    
    if ($devices.Count -eq 0) {
        Write-Error "❌ No hay dispositivos conectados"
        return $null
    }
    
    $devices | ForEach-Object {
        $parts = $_ -split '\s+'
        Write-Host "  • $($parts[0]) - $($parts[1])"
    }
    
    return $devices[0] -split '\s+' | Select-Object -First 1
}

function Install-APKs {
    param([string]$DeviceId)
    
    if (-not (Test-Path $OutputDir)) {
        Write-Error "❌ Directorio de APKs no encontrado: $OutputDir"
        return $false
    }
    
    $apks = @(
        "app-coordinador.apk",
        "app-plc.apk",
        "app-calidad.apk",
        "app-manufactura.apk",
        "app-almacen.apk"
    )
    
    Write-Info "📦 Iniciando instalación..."
    
    $success = $true
    foreach ($apk in $apks) {
        $apkPath = "$OutputDir\$apk"
        
        if (-not (Test-Path $apkPath)) {
            Write-Warning "⚠️  $apk no encontrado (saltando)"
            continue
        }
        
        Write-Info "⏳ Instalando $apk..."
        
        if ($DeviceId) {
            adb -s $DeviceId install -r $apkPath 2>&1 | Out-Null
        } else {
            adb install -r $apkPath 2>&1 | Out-Null
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "✅ $apk instalado"
        } else {
            Write-Error "❌ Error al instalar $apk"
            $success = $false
        }
    }
    
    return $success
}

function Verify-Installation {
    param([string]$DeviceId)
    
    Write-Info "🔍 Verificando instalación..."
    
    $cmd = "shell pm list packages | grep industria"
    $packages = adb $cmd 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "✅ Apps instaladas:"
        $packages | ForEach-Object { Write-Host "  • $_" }
        return $true
    } else {
        Write-Error "❌ No se encontraron apps instaladas"
        return $false
    }
}

function Uninstall-APKs {
    param([string]$DeviceId)
    
    $packages = @(
        "com.industria.coordinacion",
        "com.industria.plc",
        "com.industria.calidad",
        "com.industria.manufactura",
        "com.industria.almacenamiento"
    )
    
    Write-Warning "🗑️  Desinstalando apps..."
    
    foreach ($pkg in $packages) {
        if ($DeviceId) {
            adb -s $DeviceId uninstall $pkg 2>&1 | Out-Null
        } else {
            adb uninstall $pkg 2>&1 | Out-Null
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "✅ $pkg desinstalado"
        }
    }
}

function Start-Logs {
    param([string]$DeviceId)
    
    Write-Info "📋 Capturando logs..."
    Write-Info "Presiona Ctrl+C para detener"
    
    $timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
    $logFile = "$ProjectPath\logs_cim_$timestamp.txt"
    
    if ($DeviceId) {
        adb -s $DeviceId logcat -v time *:V | Tee-Object -FilePath $logFile
    } else {
        adb logcat -v time *:V | Tee-Object -FilePath $logFile
    }
    
    Write-Success "📁 Logs guardados en: $logFile"
}

function Test-Connectivity {
    param([string]$DeviceId)
    
    Write-Info "🔗 Verificando conectividad..."
    
    if ($DeviceId) {
        $result = adb -s $DeviceId shell getprop ro.build.version.release 2>&1
    } else {
        $result = adb shell getprop ro.build.version.release 2>&1
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "✅ Conexión exitosa"
        Write-Host "  Versión Android: $result"
        return $true
    } else {
        Write-Error "❌ No se pudo conectar al dispositivo"
        return $false
    }
}

# ===== MAIN =====

Write-Host "`n" -NoNewline
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         CIM v6.0 - INSTALLER AUTOMÁTICO                   ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host "`n"

# Verificar ADB
if (-not (Check-ADB)) {
    exit 1
}

# Listar dispositivos
$deviceId = List-Devices

if (-not $deviceId) {
    Write-Error "❌ Por favor, conecta un dispositivo Android vía USB"
    exit 1
}

# Ejecutar modo seleccionado
switch ($Mode.ToLower()) {
    "install" {
        Write-Host "`n"
        Test-Connectivity -DeviceId $deviceId
        Write-Host "`n"
        Install-APKs -DeviceId $deviceId
        Write-Host "`n"
        Verify-Installation -DeviceId $deviceId
    }
    
    "uninstall" {
        Write-Host "`n"
        Uninstall-APKs -DeviceId $deviceId
    }
    
    "verify" {
        Write-Host "`n"
        Verify-Installation -DeviceId $deviceId
    }
    
    "logs" {
        Write-Host "`n"
        Start-Logs -DeviceId $deviceId
    }
    
    default {
        Write-Error "❌ Modo desconocido: $Mode"
        Write-Info "Modos disponibles: install, uninstall, verify, logs"
    }
}

Write-Host "`n"
Write-Success "✅ Completado"
Write-Host "`n"
