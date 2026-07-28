# =============================================================================
# CIM v6.0 - SIMULADOR DE CICLO COMPLETO (Sin Hardware)
# =============================================================================
# Este script simula el flujo completo de manufactura usando solo software.
# Útil para demostraciones y validación sin ESP32 ni dispositivos físicos.

Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     CIM v6.0 - SIMULADOR DE CICLO DE MANUFACTURA COMPLETO   ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host "`n[1/6] Iniciando Simulación de Ciclo CIM..." -ForegroundColor Yellow

$steps = @(
    @{ Station = "PLC";        Cmd = "PLC:START";           Desc = "Cinta transportadora iniciada" },
    @{ Station = "PLC";        Cmd = "SENSOR_ACTIVATED|POS:1"; Desc = "Pallet detectado en estación 1" },
    @{ Station = "COORDINADOR"; Cmd = "ROUTE|MANUFACTURA";    Desc = "Pallet enrutado a Manufactura" },
    @{ Station = "MANUFACTURA"; Cmd = "R:HOME";               Desc = "Robot Scorbot en HOME" },
    @{ Station = "MANUFACTURA"; Cmd = "R:RUN";                Desc = "Robot procesando pieza" },
    @{ Station = "MANUFACTURA"; Cmd = "L:START";              Desc = "Láser CNC iniciado" },
    @{ Station = "MANUFACTURA"; Cmd = "GCODE_EXECUTED";       Desc = "G-code completado" },
    @{ Station = "COORDINADOR"; Cmd = "ROUTE|CALIDAD";        Desc = "Pieza enviada a Control de Calidad" },
    @{ Station = "CALIDAD";     Cmd = "ARUCO:DETECT";         Desc = "Marcador ArUco detectado" },
    @{ Station = "CALIDAD";     Cmd = "YOLO:DETECT";          Desc = "Objeto validado por YOLO" },
    @{ Station = "CALIDAD";     Cmd = "VAL:PASS";             Desc = "Pieza APROBADA (PASS)" },
    @{ Station = "COORDINADOR"; Cmd = "ROUTE|ALMACEN";        Desc = "Pieza enrutada a Almacén" },
    @{ Station = "ALMACEN";     Cmd = "STO:07";               Desc = "Pieza almacenada en rack posición 07" },
    @{ Station = "PLC";         Cmd = "PLC:STOP";             Desc = "Cinta detenida - Ciclo completado" }
)

foreach ($step in $steps) {
    Write-Host "`n[$($step.Station)] $($step.Cmd)" -ForegroundColor Green
    Write-Host "   → $($step.Desc)" -ForegroundColor White
    Start-Sleep -Milliseconds 800
}

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║           ✓ CICLO COMPLETO SIMULADO EXITOSAMENTE           ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green

Write-Host "`nResumen:"
Write-Host "  - 5 estaciones participaron"
Write-Host "  - 14 comandos ejecutados"
Write-Host "  - Tiempo total simulado: ~11 segundos"
Write-Host "  - Resultado: PIEZA APROBADA Y ALMACENADA"

Write-Host "`nPara simular en las apps Android:"
Write-Host "  1. Abre el Coordinador → pestaña EXEC → botón 'SIMULAR CICLO COMPLETO'"
Write-Host "  2. Activa 'Modo Autónomo' en cada estación"
Write-Host "  3. Usa los botones de simulación de sensores"