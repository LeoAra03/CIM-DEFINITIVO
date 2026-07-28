# FIX #301 - Seguridad de Password (Mitigación)

## Problema
Password se envía en texto plano durante el handshake.

## Solución aplicada (Mitigación)
Se agregó documentación de seguridad y se mejoró el mensaje de error:

```kotlin
if (password != CimProtocol.PASSWORD_ACTUAL) {
    addLog("⚠ Intento de conexión con password inválido desde $mac")
    // ... rechazo
}
```

**NOTA IMPORTANTE**: Para una solución completa se requiere:
1. Implementar hash SHA-256 del password
2. Usar TLS/SSL en el TcpServer
3. Implementar challenge-response

## Archivos modificados
- StationClient.kt (mejor logging)
- TcpServer.kt (mejor logging)

## Estado
⚠️ MITIGADO (solución completa requiere cambio de protocolo)
