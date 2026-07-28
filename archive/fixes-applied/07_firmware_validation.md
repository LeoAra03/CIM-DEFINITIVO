# FIX #144 - Validación de Posiciones en Firmware

## Problema
cim_almacen_firmware.ino aceptaba cualquier posición sin validación.

## Solución aplicada
Se agregó validación de rango:

```cpp
void handleCommand(String cmd) {
    if (cmd.startsWith("STO:")) {
        int pos = cmd.substring(4).toInt();
        if (pos < 1 || pos > 18) {
            sendResponse("STO:ERROR:INVALID_POSITION");
            return;
        }
        sendResponse("STO:" + String(pos) + ";OK");
    }
    // ... resto del código
}
```

## Archivos modificados
- cim_almacen_firmware.ino

## Estado
✅ CORREGIDO
