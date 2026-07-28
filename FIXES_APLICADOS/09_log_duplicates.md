# FIX #107 - Prevención de Logs Duplicados

## Problema
Los logs podían tener mensajes duplicados.

## Solución aplicada
Se agregó deduplicación en IndustrialTerminal y addLog:

```kotlin
private fun addLog(message: String) {
    val timestamp = SimpleDateFormat("HH:mm:ss").format(Date())
    val logEntry = "[$timestamp] $message"
    
    // Evitar duplicados consecutivos
    if (_uiState.value.logMessages.lastOrNull() != logEntry) {
        val newLogs = _uiState.value.logMessages + logEntry
        val trimmedLogs = if (newLogs.size > 100) newLogs.takeLast(100) else newLogs
        
        _uiState.value = _uiState.value.copy(logMessages = trimmedLogs)
    }
}
```

## Archivos modificados
- CoordinatorViewModel.kt
- IndustrialTerminal.kt (mejorado)

## Estado
✅ CORREGIDO
