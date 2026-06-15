# Script simple para instalar las APKs principales

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$AppsDir = Join-Path $ProjectRoot "apps"
$OutputDir = Join-Path $ProjectRoot "output-apks"

if (-not (Test-Path $AppsDir)) { New-Item -ItemType Directory -Path $AppsDir -Force | Out-Null }
if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null }

$apkMap = @(
    @{ Name = "Coordinador"; Source = Join-Path $ProjectRoot "output-apks\app-coordinador.apk"; Destination = Join-Path $AppsDir "app-coordinador.apk" },
    @{ Name = "Calidad"; Source = Join-Path $ProjectRoot "output-apks\app-calidad.apk"; Destination = Join-Path $AppsDir "app-calidad.apk" },
    @{ Name = "Almacén"; Source = Join-Path $ProjectRoot "output-apks\app-almacen.apk"; Destination = Join-Path $AppsDir "app-almacen.apk" },
    @{ Name = "Manufactura"; Source = Join-Path $ProjectRoot "output-apks\app-manufactura.apk"; Destination = Join-Path $AppsDir "app-manufactura.apk" },
    @{ Name = "PLC"; Source = Join-Path $ProjectRoot "output-apks\app-plc.apk"; Destination = Join-Path $AppsDir "app-plc.apk" }
)

foreach ($item in $apkMap) {
    if (Test-Path $item.Source) {
        Copy-Item -Path $item.Source -Destination $item.Destination -Force
        Write-Host "✓ $($item.Name): listo en $($item.Destination)"
    } else {
        Write-Host "⚠ $($item.Name): no encontrado en $($item.Source)"
    }
}

Write-Host "\nCarpeta lista: $AppsDir"
