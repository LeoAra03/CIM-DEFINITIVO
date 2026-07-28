# FIX #701 - Límite de clientes en TcpServer

## Problema
TcpServer aceptaba conexiones ilimitadas (potencial DoS).

## Solución aplicada
Se agregó límite máximo de 50 clientes concurrentes:

```kotlin
companion object {
    const val MAX_CLIENTS = 50
}

private fun handleClientConnection(clientSocket: Socket) {
    if (clientSockets.size >= MAX_CLIENTS) {
        Log.w("TcpServer", "Límite de clientes alcanzado ($MAX_CLIENTS)")
        clientSocket.close()
        return
    }
    // ... resto del código
}
```

## Archivos modificados
- TcpServer.kt

## Estado
✅ CORREGIDO
