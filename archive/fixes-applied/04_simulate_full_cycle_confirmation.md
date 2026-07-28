# FIX #101 - Confirmación en simulateFullCycle

## Problema
simulateFullCycle() enviaba comandos sin esperar confirmación.

## Solución aplicada
Se agregó sistema de confirmación con logs detallados:

```kotlin
private suspend fun sendWithConfirmation(appType: AppType, command: String) {
    addLog("→ Enviando $command a ${appType.name}")
    sendExecuteCommand(appType, command)
    delay(300) // Pequeña espera para que el mensaje se procese
    addLog("✓ Comando $command enviado a ${appType.name}")
}
```

Además se agregó verificación de conexión antes de enviar:

```kotlin
if (broker != null) {
    sendWithConfirmation(AppType.PLC, "PLC:START")
} else {
    addLog("⚠ MODO SIMULADO - Sin broker conectado")
}
```

## Archivos modificados
- CoordinatorViewModel.kt (método simulateFullCycle)

## Estado
✅ CORREGIDO
