# BUILD_AND_CONSOLIDATE.ps1
# Script para compilar APKs y consolidarlas en config/output-apks

param(
    [switch]$SkipGradle,
    [switch]$QuietMode
)

$root = "C:\Users\Leo\Desktop\Test Practica2\Practica_2\CIM-DEFINITIVO"
$config_dir = Join-Path $root "config"
$output_apks = Join-Path $config_dir "output-apks"
$android_apks = Join-Path $root "android\apks"
$legacy_model = Join-Path $root "legacy\yolov8n-int8.tflite"

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    if (-not $QuietMode) {
        $ts = (Get-Date).ToString("HH:mm:ss")
        $prefix = "[$ts]"
        switch ($Level) {
            "SUCCESS" { Write-Host "$prefix ✓ $Message" -ForegroundColor Green }
            "ERROR" { Write-Host "$prefix ✗ $Message" -ForegroundColor Red }
            "WARN" { Write-Host "$prefix ⚠ $Message" -ForegroundColor Yellow }
            default { Write-Host "$prefix ℹ $Message" -ForegroundColor Cyan }
        }
    }
}

Write-Log "════════════════════════════════════════════════════" "INFO"
Write-Log "CIM v6.0 - BUILD & CONSOLIDATE" "INFO"
Write-Log "════════════════════════════════════════════════════" "INFO"

# 1. COPIAR MODELO YOLO
Write-Log "PASO 1: Copiando modelo YOLO..."

if (-not (Test-Path $legacy_model)) {
    Write-Log "Modelo no encontrado en $legacy_model" "ERROR"
    exit 1
}

$dst_manufactura = "$root\android\apps\app-manufactura\app\src\main\assets"
$dst_calidad = "$root\android\apps\app-calidad\app\src\main\assets"

New-Item -ItemType Directory -Path $dst_manufactura -Force | Out-Null
New-Item -ItemType Directory -Path $dst_calidad -Force | Out-Null

Copy-Item -Path $legacy_model -Destination "$dst_manufactura\yolov8n-int8.tflite" -Force
Copy-Item -Path $legacy_model -Destination "$dst_calidad\yolov8n-int8.tflite" -Force

Write-Log "✓ Modelo copiado a app-manufactura y app-calidad" "SUCCESS"

# 2. COMPILAR GRADLE
if (-not $SkipGradle) {
    Write-Log "PASO 2: Compilando APKs con Gradle..."

    Push-Location $config_dir

    $begin_time = Get-Date
    Write-Log "Ejecutando: .\gradlew.bat buildAllApks (puede tomar 5-10 min)" "WARN"

    $log_file = Join-Path $config_dir "build_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"
    .\gradlew.bat buildAllApks 2>&1 | Tee-Object -FilePath $log_file | Select-Object -Last 30

    $elapsed = ((Get-Date) - $begin_time).TotalSeconds
    Write-Log "Compilación completada en $($elapsed)s" "SUCCESS"
    Write-Log "Log guardado: $log_file" "INFO"

    Pop-Location
} else {
    Write-Log "Saltando compilación (--SkipGradle)" "WARN"
}

# 3. CONSOLIDAR APKs EN config/output-apks
Write-Log "PASO 3: Consolidando APKs en config/output-apks..."

New-Item -ItemType Directory -Path $output_apks -Force | Out-Null

$apk_files = @()
Get-ChildItem -Path "$root\android\apps" -Recurse -Filter "*-debug.apk" -ErrorAction SilentlyContinue | ForEach-Object {
    $clean_name = $_.Name -replace "-debug", ""
    $dst_path = Join-Path $output_apks $clean_name
    Copy-Item -Path $_.FullName -Destination $dst_path -Force

    $size_mb = [Math]::Round($_.Length / 1MB, 1)
    Write-Log "  • $clean_name ($size_mb MB)" "SUCCESS"

    $apk_files += [PSCustomObject]@{
        Name = $clean_name
        Path = $dst_path
        Size = $size_mb
        FullPath = $_.FullName
    }
}

if ($apk_files.Count -eq 0) {
    Write-Log "ERROR: No se encontraron APKs compilados" "ERROR"
    Write-Log "Verifica que buildAllApks se ejecutó correctamente" "ERROR"
    exit 1
}

# 4. COPIAR A android/apks PARA ANDROID STUDIO
Write-Log "PASO 4: Copiando APKs a android/apks para Android Studio..."

$apk_files | ForEach-Object {
    Copy-Item -Path $_.Path -Destination (Join-Path $android_apks $_.Name) -Force
    Write-Log "  • $($_.Name)" "SUCCESS"
}

# 5. GENERAR MANIFEST.JSON
Write-Log "PASO 5: Generando manifest.json..."

$manifest = @()
$total_size = 0

$apk_files | ForEach-Object {
    $size_bytes = (Get-Item $_.Path).Length
    $total_size += $size_bytes

    $hash = (Get-FileHash -Path $_.Path -Algorithm MD5 -ErrorAction SilentlyContinue).Hash

    $manifest += [PSCustomObject]@{
        name = $_.Name
        size_mb = $_.Size
        size_bytes = $size_bytes
        path = $_.Path
        md5 = $hash
        timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    }
}

$manifest | ConvertTo-Json -Depth 3 | Set-Content -Path "$output_apks\manifest.json" -Encoding UTF8
Write-Log "Manifest guardado: $output_apks\manifest.json" "SUCCESS"

# 6. RESUMEN FINAL
Write-Log ""
Write-Log "═════════════════════════════════��══════════════════" "SUCCESS"
Write-Log "✅ BUILD COMPLETADO 100%" "SUCCESS"
Write-Log "════════════════════════════════════════════════════" "SUCCESS"

Write-Log "APKs compiladas: $($apk_files.Count)"
Write-Log "Tamaño total: $([Math]::Round($total_size / 1MB, 1)) MB"
Write-Log "Ubicación output: $output_apks"
Write-Log ""

Write-Log "APKs generadas:" "INFO"
$apk_files | ForEach-Object {
    Write-Log "  ✓ $($_.Name) - $($_.Size) MB" "INFO"
}

Write-Log ""
Write-Log "📋 Próximos pasos:" "WARN"
Write-Log "  1. Prueba en Android Studio: adb install -r config/output-apks/*.apk" "WARN"
Write-Log "  2. Test YOLO offline: python tools/tflite_yolo_test.py <model> <image>" "WARN"
Write-Log "  3. Hub simulador: python tools/hub_simulator.py" "WARN"
Write-Log ""

