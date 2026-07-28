# FIX #145 - Uso de DEVICE_NAME en Firmware

## Problema
DEVICE_NAME estaba definido pero nunca se usaba.

## Solución aplicada
Se agregó al inicio del setup:

```cpp
void setup() {
    Serial.begin(115200);
    Serial.println("CIM Firmware starting...");
    Serial.println("DEVICE: " + String(DEVICE_NAME));
    Serial.println("BLE_NAME: " + String(BLE_NAME));
    
    // ... resto del código
}
```

## Archivos modificados
- cim_scorbot_firmware.ino
- cim_plc_firmware.ino
- cim_calidad_firmware.ino
- cim_almacen_firmware.ino

## Estado
✅ CORREGIDO
