# Abre la guía rápida en el explorador
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Quickstart = Join-Path $ProjectRoot "docs\quickstart\README.md"
if (Test-Path $Quickstart) {
    Start-Process $Quickstart
} else {
    Write-Host "No se encontró la guía rápida"
}
