# FIX #82 - Límite de Event Log

## Problema
eventLog en PlcController podía crecer indefinidamente.

## Solución aplicada
Se agregó límite máximo de 500 eventos:

```kotlin
private val MAX_LOG_SIZE = 500

fun addEvent(type: PlcEvent.Type, message: String) {
    eventLog.add(PlcEvent(...))
    
    // Limitar tamaño del log
    while (eventLog.size > MAX_LOG_SIZE) {
        eventLog.removeAt(0)
    }
}
```

## Archivos modificados
- PlcController.kt

## Estado
✅ CORREGIDO
