# crear_paquete_zip.ps1 — Regenera CIM_V6_PAQUETE_ENTREGA.zip
# Uso: .\entrega\crear_paquete_zip.ps1
# Requiere: output-apks/ con APKs y docs/ENTREGA_FINAL_LEONARDO_ARAYA.pdf

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

$base = "entrega\CIM_V6_PAQUETE_ENTREGA"
$plantilla = "entrega\plantilla"

if (Test-Path $base) { Remove-Item $base -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$base\1_DOCUMENTACION", "$base\2_APK_ANDROID", "$base\3_FIRMWARE_ESP32", "$base\4_SCRIPTS" | Out-Null

Copy-Item "$plantilla\LEEME.txt" $base -Force
Copy-Item "$plantilla\3_FIRMWARE_ESP32\*" "$base\3_FIRMWARE_ESP32\" -Force
Copy-Item "$plantilla\4_SCRIPTS\*" "$base\4_SCRIPTS\" -Force

Copy-Item "docs\ENTREGA_FINAL_LEONARDO_ARAYA.pdf" "$base\1_DOCUMENTACION\" -Force
Copy-Item "docs\GUIA_LABORATORIO_MANANA.md" "$base\1_DOCUMENTACION\" -Force

$apks = @("app-coordinador", "app-plc", "app-manufactura", "app-calidad", "app-almacen")
foreach ($a in $apks) {
    $src = "output-apks\$a.apk"
    if (-not (Test-Path $src)) { Write-Error "Falta $src — ejecute .\gradlew buildAllApks" }
    Copy-Item $src "$base\2_APK_ANDROID\" -Force
}
Copy-Item "output-apks\cim_esp32_firmware_v6.bin" "$base\3_FIRMWARE_ESP32\" -Force

$zipPath = "CIM_V6_PAQUETE_ENTREGA.zip"
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Compress-Archive -Path "$base\*" -DestinationPath $zipPath -CompressionLevel Optimal

$mb = [math]::Round((Get-Item $zipPath).Length / 1MB, 1)
Write-Host "ZIP creado: $((Resolve-Path $zipPath).Path) ($mb MB)" -ForegroundColor Green
