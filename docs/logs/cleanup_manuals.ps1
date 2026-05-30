# Script para limpiar manuales obsoletos

$baseDir = "C:\Users\Leo\Desktop\Test Practica2\Practica_2\docs\manuals"

$filesToRemove = @(
    "ANALISIS_LOGICO_MATEMATICO_CIM_v5.md",
    "ARQUITECTURA_CIM_v6.md",
    "GUIA_DEBUGGING_BLUETOOTH.md",
    "GUIA_DEBUGGING_BLUETOOTH_v2.md",
    "GUIA_IMPLEMENTACION_BOTONES.md",
    "GUIA_MAPEO_BOTONES_COMANDOS.md",
    "MEMORIA_TECNICA_INDUSTRIAL_CIM_v5.md",
    "MEMORIA_TECNICA_V5_INDUSTRIAL.md"
)

$count = 0

foreach($file in $filesToRemove) {
    $fullPath = Join-Path $baseDir $file
    if(Test-Path $fullPath) {
        try {
            Remove-Item $fullPath -Force -Confirm:$false
            $count++
            Write-Host "✅ Eliminado: $file"
        }
        catch {
            Write-Host "❌ Error eliminando $file : $_"
        }
    }
    else {
        Write-Host "⚠️ No encontrado: $file"
    }
}

Write-Host "`n📊 RESUMEN:"
Write-Host "Total eliminados: $count archivos"
Write-Host "`nArchivos restantes en manuals:"
Get-ChildItem $baseDir -Filter "*.md" | Select-Object Name

