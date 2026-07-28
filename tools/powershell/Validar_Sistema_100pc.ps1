#!/usr/bin/env pwsh
# =============================================================================
# CIM v6.0 - VALIDACIÓN FINAL 100% AUTOMATIZABLE EN MODO SIMULADO
# =============================================================================
# Esta validación no certifica hardware físico: verifica estructura activa,
# herramientas, firmware canónico, documentación y preparación de CI.

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..\..")
$PythonValidator = Join-Path $ProjectRoot "tools\validate_system_100.py"

Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  VALIDACIÓN 100% AUTOMATIZABLE - CIM v6.0 SIMULADO        ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

$python = Get-Command python3 -ErrorAction SilentlyContinue
if (-not $python) { $python = Get-Command python -ErrorAction SilentlyContinue }

if ($python -and (Test-Path $PythonValidator)) {
    & $python.Source $PythonValidator
    exit $LASTEXITCODE
}

Write-Host "`n⚠ Python no disponible; ejecutando comprobación PowerShell básica." -ForegroundColor Yellow

$checks = @(
    @{ Name = "Apps Android activas"; Paths = @("android\apps\app-coordinador\app", "android\apps\app-plc\app", "android\apps\app-manufactura\app", "android\apps\app-calidad\app", "android\apps\app-almacen\app", "android\apps\wear-coordinador\app") },
    @{ Name = "core-network"; Paths = @("android\core-network\src\main\java", "android\core-network\src\test\java") },
    @{ Name = "Firmware ESP32 activo"; Paths = @("esp32\firmware\esp32_plc_master.ino", "esp32\firmware\esp32_scorbot_almacen.ino", "esp32\firmware\esp32_scorbot_calidad.ino", "esp32\firmware\esp32_scorbot_manufactura.ino") },
    @{ Name = "Herramientas"; Paths = @("tools\hub_simulator.py", "tools\vision_safety_simulator.py", "tools\powershell\Simular_Ciclo_Completo.ps1") },
    @{ Name = "Documentación activa"; Paths = @("README.md", "docs\deliverables\QUALITY_GATES.md", "docs\quickstart\README.md") },
    @{ Name = "Build Gradle centralizado"; Paths = @("config\gradlew", "config\settings.gradle.kts", "config\build.gradle.kts") }
)

$passed = 0
foreach ($check in $checks) {
    $missing = @()
    foreach ($path in $check.Paths) {
        if (-not (Test-Path (Join-Path $ProjectRoot $path))) { $missing += $path }
    }
    if ($missing.Count -eq 0) {
        Write-Host "✓ $($check.Name)" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "✗ $($check.Name) - faltan: $($missing -join ', ')" -ForegroundColor Red
    }
}

$percent = [Math]::Round(($passed / $checks.Count) * 100, 2)
Write-Host "`n════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "RESULTADO: $passed/$($checks.Count) verificaciones ($percent%)" -ForegroundColor $(if($passed -eq $checks.Count){"Green"}else{"Yellow"})

if ($passed -eq $checks.Count) {
    Write-Host "`n✅ SISTEMA 100% LISTO PARA VALIDACIÓN AUTOMATIZADA EN SIMULACIÓN" -ForegroundColor Green
    exit 0
}

Write-Host "`n⚠ Algunas verificaciones fallaron. Revisa la estructura activa del repositorio." -ForegroundColor Yellow
exit 1
