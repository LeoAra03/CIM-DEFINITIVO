param(
    [string]$ProjectRoot = (Split-Path -Path $PSScriptRoot -Parent),
    [string]$SourceDir = "$ProjectRoot\output-apks",
    [string]$TargetDir = "$ProjectRoot\.para-maniana\APKS_INSTALABLES"
)

if (-not (Test-Path $SourceDir)) {
    Write-Error "No se encontró la carpeta de orígen: $SourceDir"
    exit 1
}

if (-not (Test-Path $TargetDir)) {
    New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
}

$files = @(
    'app-coordinador.apk',
    'app-plc.apk',
    'app-manufactura.apk',
    'app-calidad.apk',
    'app-almacen.apk',
    'cim_esp32_firmware_v6.bin'
)

foreach ($file in $files) {
    $source = Join-Path $SourceDir $file
    if (Test-Path $source) {
        Copy-Item -Path $source -Destination $TargetDir -Force
        Write-Host "Copiado: $file"
    } else {
        Write-Warning "No existe: $source"
    }
}

Write-Host "`nCarpeta personal actualizada: $TargetDir"
