# =============================================================================
# CIM v6.0 - SCRIPT DE INSTALACIÓN DE APKs
# =============================================================================
# Ejecutar como Administrador en Windows
# Requiere: ADB instalado y teléfonos conectados por USB (o WiFi debugging)

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   CIM v6.0 - INSTALADOR DE APKs" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

$apkDir = "..\2_APK_ANDROID"
$apks = @(
    "app-coordinador.apk",
    "app-plc.apk",
    "app-manufactura.apk",
    "app-calidad.apk",
    "app-almacen.apk"
)

Write-Host "`nBuscando dispositivos Android..." -ForegroundColor Yellow
adb devices

foreach ($apk in $apks) {
    $fullPath = Join-Path $apkDir $apk
    if (Test-Path $fullPath) {
        Write-Host "`nInstalando $apk..." -ForegroundColor Green
        adb install -r $fullPath
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✓ $apk instalado correctamente" -ForegroundColor Green
        } else {
            Write-Host "  ✗ Error instalando $apk" -ForegroundColor Red
        }
    } else {
        Write-Host "  ⚠ No se encontró $apk en $apkDir" -ForegroundColor Yellow
    }
}

Write-Host "`n==============================================" -ForegroundColor Cyan
Write-Host "   INSTALACIÓN COMPLETADA" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

Write-Host "`nPasos siguientes:"
Write-Host "1. Abre app-coordinador en un dispositivo y pulsa START HUB"
Write-Host "2. En cada estación, ve a la pestaña SINCRO e ingresa la IP del HUB"
Write-Host "3. Autoriza las conexiones desde el Coordinador"
Write-Host "4. (Opcional) Activa Modo Autónomo para pruebas sin red"

Read-Host "`nPresiona Enter para salir"