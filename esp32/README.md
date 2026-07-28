# ESP32 / Wemos D1 R32

Firmware activo de las estaciones físicas CIM.

## Contenido

- `firmware/esp32_plc_master.ino`
- `firmware/esp32_scorbot_manufactura.ino`
- `firmware/esp32_scorbot_calidad.ino`
- `firmware/esp32_scorbot_almacen.ino`
- `firmware/cim_ble_firmware.h`
- `firmware/README.md`

## Uso rápido

1. Revisa `esp32/firmware/README.md` para elegir el firmware correcto.
2. Flashea con Arduino IDE, Arduino CLI, PlatformIO o:

```powershell
.\tools\powershell\Flashear-ESP32.ps1 -Port COM3
```

3. Registra versión, placa, puerto y resultado en `docs/deliverables/BITACORA_VALIDACION.md`.

No conectes actuadores reales sin E-stop físico, límites e interlocks validados.
