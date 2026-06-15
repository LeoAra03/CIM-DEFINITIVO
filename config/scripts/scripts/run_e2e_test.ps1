# Run E2E test harness (Windows PowerShell)
# Compila y ejecuta CoordinatorSimulator y StationSimulator

$base = Split-Path -Parent $MyInvocation.MyCommand.Definition
$src = Join-Path $base "..\tools\test-harness"
Push-Location $src

param(
	[int]$Count = 0
)

# Compilar
javac -d . CoordinatorSimulator.java StationSimulator.java StressTest.java
if ($LASTEXITCODE -ne 0) { Write-Error "javac failed"; Pop-Location; exit 1 }

# Ejecutar el coordinador en background
Start-Process -NoNewWindow -FilePath java -ArgumentList 'CoordinatorSimulator' -WorkingDirectory $src
Start-Sleep -Seconds 1

if ($Count -gt 0) {
	Write-Host "Running stress test with $Count stations..."
	java -cp . StressTest localhost 8888 $Count 50
} else {
	# Ejecutar el simulador de estaciones (main lanza 4 estaciones)
	java -cp . StationSimulator
}
Pop-Location

Write-Host "If you want to run the real TcpServer from the Kotlin module, build the project with Gradle and then run the Java launcher in tools/test-harness/RealTcpServerLauncher.java on the classpath that includes core-network. Example Gradle tasks: `./gradlew :core-network:assemble` and then run with classpath including core-network.jar or use the IDE to run the module."

# Nota: el coordinador seguirá corriendo en otra ventana; terminar manualmente si es necesario.
Pop-Location
