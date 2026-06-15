# Script simple para compilar las apps principales

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot

$apps = @(
    ":app-coordinador:app:assembleDebug",
    ":app-calidad:app:assembleDebug",
    ":app-almacen:app:assembleDebug",
    ":app-manufactura:app:assembleDebug",
    ":app-plc:app:assembleDebug"
)

foreach ($app in $apps) {
    Write-Host "Compilando $app..."
    & "$ProjectRoot\gradlew.bat" $app --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Falló: $app"
    }
}

Write-Host "Todas las apps compiladas correctamente."
