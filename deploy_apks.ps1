# Despliegue de APKs a output-apks/
# Este script copia todos los APKs compilados a la carpeta de salida

$ProjectRoot = "c:\Users\Leo\Desktop\Test Practica2\Practica_2"
$OutputDir = "$ProjectRoot\output-apks"

# Crear directorio si no existe
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
    Write-Host "✓ Directorio $OutputDir creado"
}

# Funciones APK a copiar
$apps = @(
    @{
        name = "Coordinador"
        source = "$ProjectRoot\app-coordinador\app\build\outputs\apk\debug\app-debug.apk"
        destination = "$OutputDir\CIM_Coordinador_V6_DEBUG.apk"
    },
    @{
        name = "Manufactura"
        source = "$ProjectRoot\app-manufactura\app\build\outputs\apk\debug\app-debug.apk"
        destination = "$OutputDir\CIM_Manufactura_V6_DEBUG.apk"
    },
    @{
        name = "Calidad"
        source = "$ProjectRoot\app-calidad\app\build\outputs\apk\debug\app-debug.apk"
        destination = "$OutputDir\CIM_Calidad_V6_DEBUG.apk"
    },
    @{
        name = "Almacén"
        source = "$ProjectRoot\app-almacen\app\build\outputs\apk\debug\app-debug.apk"
        destination = "$OutputDir\CIM_Almacen_V6_DEBUG.apk"
    },
    @{
        name = "PLC"
        source = "$ProjectRoot\app-plc\app\build\outputs\apk\debug\app-debug.apk"
        destination = "$OutputDir\CIM_PLC_V6_DEBUG.apk"
    }
)

# Copiar todos los APKs
$successful = 0
$failed = 0

foreach ($app in $apps) {
    if (Test-Path $app.source) {
        Copy-Item -Path $app.source -Destination $app.destination -Force
        Write-Host "✓ $($app.name): Copiado exitosamente"
        $successful++
    } else {
        Write-Host "✗ $($app.name): No encontrado en $($app.source)"
        $failed++
    }
}

# Resumen
Write-Host "`n========== RESUMEN =========="
Write-Host "Exitosos: $successful"
Write-Host "Fallidos: $failed"
Write-Host "Ubicación: $OutputDir"
Write-Host "============================`n"

# Listar archivos copiados
Get-ChildItem $OutputDir -Filter "*.apk" | Select-Object Name, @{N="Tamaño";E={[math]::Round($_.Length/1MB, 2)}} | Format-Table -AutoSize
