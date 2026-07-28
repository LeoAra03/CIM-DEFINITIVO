# FIX #11 - Null Safety en CoordinatorViewModel

## Problema
`commandBroker` podía ser null sin safe calls en algunos lugares.

## Solución aplicada
Se agregó verificación explícita en todos los métodos que usan commandBroker:

```kotlin
private fun sendExecuteCommand(destApp: AppType, command: String, destMac: String = "") {
    viewModelScope.launch {
        try {
            val broker = commandBroker
            if (broker != null) {
                // ... envío seguro
            } else {
                addLog("⚠ Broker no disponible - usando modo simulado")
            }
        } catch (e: Exception) {
            addLog("✗ Error enviando comando: ${e.message}")
        }
    }
}
```

## Archivos modificados
- CoordinatorViewModel.kt (múltiples métodos)

## Estado
✅ CORREGIDO
