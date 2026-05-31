#Requires -Version 5.1
<#
.SYNOPSIS
    Despliegue multitarea de APKs CIM v6.0 en multiples emuladores Android.

.DESCRIPTION
    Flujo de datos:
      adb devices → filtra emuladores (emulator-XXXX) → Jobs paralelos
      → adb -s <serial> install -r → output-apks/*.apk

    APKs esperados (generados por .\gradlew buildAllApks):
      - app-coordinador.apk  (hub TCP :8888)
      - app-plc.apk
      - app-manufactura.apk
      - app-calidad.apk
      - app-almacen.apk

.EXAMPLE
    .\entorno_mobile\deploy_multitask.ps1
    .\entorno_mobile\deploy_multitask.ps1 -ApkFilter "coordinador,plc"
#>
[CmdletBinding()]
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$ApkDir = "",
    [string[]]$ApkFilter = @(),
    [switch]$SkipBuildCheck
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ApkDir)) {
    $ApkDir = Join-Path $ProjectRoot "output-apks"
}

# Mapa APK → rol en malla CIM
$ApkMap = [ordered]@{
    "app-coordinador.apk"  = "Hub central — TcpServer :8888 + AuthorizationManager"
    "app-plc.apk"          = "Estacion PLC — StationClient + BLE"
    "app-manufactura.apk"  = "Estacion Manufactura"
    "app-calidad.apk"      = "Estacion Calidad"
    "app-almacen.apk"      = "Estacion Almacen"
}

function Find-Adb {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($adb) { return $adb.Source }

    $sdkRoots = @(
        $env:ANDROID_HOME,
        $env:ANDROID_SDK_ROOT,
        "$env:LOCALAPPDATA\Android\Sdk"
    ) | Where-Object { $_ -and (Test-Path $_) }

    foreach ($root in $sdkRoots) {
        $candidate = Join-Path $root "platform-tools\adb.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    throw "adb no encontrado. Instale Android SDK platform-tools o extension adelphes.android-dev-ext."
}

function Get-EmulatorDevices {
    param([string]$AdbPath)
    $raw = & $AdbPath devices 2>&1
    $serials = @()
    foreach ($line in $raw) {
        if ($line -match '^(emulator-\d+)\s+device$') {
            $serials += $Matches[1]
        }
    }
    return $serials
}

function Install-ApkOnDevice {
    param(
        [string]$AdbPath,
        [string]$Serial,
        [string]$ApkPath,
        [string]$Role
    )
    Write-Host "[$Serial] Instalando $(Split-Path $ApkPath -Leaf) — $Role"
    $output = & $AdbPath -s $Serial install -r $ApkPath 2>&1
    if ($LASTEXITCODE -ne 0 -or ($output -join " ") -notmatch "Success") {
        throw "[$Serial] Fallo instalacion: $ApkPath — $output"
    }
    return "OK"
}

Write-Host "=== CIM v6.0 deploy_multitask ===" -ForegroundColor Cyan
Write-Host "Proyecto: $ProjectRoot"
Write-Host "APK dir:  $ApkDir"

$adbPath = Find-Adb
Write-Host "adb:      $adbPath"

if (-not (Test-Path $ApkDir)) {
    if (-not $SkipBuildCheck) {
        Write-Warning "Directorio $ApkDir no existe. Ejecute: .\gradlew buildAllApks"
    }
    New-Item -ItemType Directory -Force -Path $ApkDir | Out-Null
}

$apkFiles = Get-ChildItem -Path $ApkDir -Filter "*.apk" -ErrorAction SilentlyContinue
if ($ApkFilter.Count -gt 0) {
    $apkFiles = $apkFiles | Where-Object {
        $name = $_.Name
        ($ApkFilter | ForEach-Object { $name -like "*$_*" }) -contains $true
    }
}

if (-not $apkFiles -or $apkFiles.Count -eq 0) {
    Write-Warning "No hay APKs en $ApkDir. Generar con .\gradlew buildAllApks"
    # Continuar para mostrar emuladores detectados aunque falten APKs
}

$emulators = Get-EmulatorDevices -AdbPath $adbPath
if ($emulators.Count -eq 0) {
    Write-Warning "No se detectaron emuladores activos (emulator-XXXX). Inicie AVD desde Android Studio."
    & $adbPath devices
    exit 1
}

Write-Host "Emuladores detectados: $($emulators -join ', ')" -ForegroundColor Green

# Instalacion paralela: cada combinacion (emulador × APK) en Job separado
$jobs = @()
foreach ($serial in $emulators) {
    foreach ($apk in $apkFiles) {
        $role = $ApkMap[$apk.Name]
        if (-not $role) { $role = "Modulo CIM" }

        $jobs += Start-Job -ScriptBlock {
            param($Adb, $Ser, $Path, $Rol)
            function local-Install { param($a,$s,$p,$r)
                $o = & $a -s $s install -r $p 2>&1
                if ($LASTEXITCODE -ne 0) { throw "$o" }
                return @{ Serial = $s; Apk = (Split-Path $p -Leaf); Role = $r; Status = "Success" }
            }
            local-Install $Adb $Ser $Path $Rol
        } -ArgumentList $adbPath, $serial, $apk.FullName, $role
    }
}

Write-Host "Jobs paralelos iniciados: $($jobs.Count)" -ForegroundColor Yellow
$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job -Force

$results | ForEach-Object {
    Write-Host "  $($_.Serial) ← $($_.Apk) [$($_.Status)]" -ForegroundColor Green
}

Write-Host "`n=== Despliegue completado ===" -ForegroundColor Cyan
Write-Host "Siguiente paso: abrir app-coordinador en $($emulators[0]) e iniciar Servidor Hub TCP:8888"
