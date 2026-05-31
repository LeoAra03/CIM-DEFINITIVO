# export_entrega_pdf.ps1 — Genera PDF A4 del manual de entrega CIM v6.0
# Uso: .\docs\export_entrega_pdf.ps1  (requiere npm install en docs/ la primera vez)

$ErrorActionPreference = "Stop"
$DocsDir = $PSScriptRoot
Set-Location $DocsDir

if (-not (Test-Path (Join-Path $DocsDir "node_modules"))) {
    Write-Host "Instalando dependencias (marked + puppeteer-core)..." -ForegroundColor Yellow
    npm install
}

npm run pdf
