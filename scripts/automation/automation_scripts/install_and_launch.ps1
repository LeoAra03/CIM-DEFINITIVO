# Instala el APK debug de app-coordinador y lanza la Activity principal
# Uso: PowerShell -ExecutionPolicy Bypass -File automation_scripts\install_and_launch.ps1

Write-Host "Instalando APK (gradle installDebug) desde la raíz del repo..."
Push-Location (Join-Path $PSScriptRoot "..\..")
try {
	if (Test-Path "./gradlew.bat") {
		Write-Host "Ejecutando gradlew.bat :app-coordinador:app:installDebug"
		& .\gradlew.bat ":app-coordinador:app:installDebug"
	} else {
		Write-Host "No se encontró gradlew.bat en la raíz; omitiendo instalación por gradle."
	}
} finally {
	Pop-Location
}

# Pausa breve para que adb detecte
Start-Sleep -Seconds 2

# Lanzar activity principal (ajusta el package/class si es necesario)
if (Get-Command adb -ErrorAction SilentlyContinue) {
	adb shell am start -n com.industria.coordinacion/.MainActivity -W
	Write-Host "APK instalado y activity lanzada (si había dispositivo y adb en PATH)."
} else {
	Write-Host "adb no encontrado en PATH. Si deseas lanzar la app, instala adb o ejecuta el comando manualmente."
}
