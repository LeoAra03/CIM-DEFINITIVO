# ESP32 — Simulación y Hardware Real (CIM v6.0)

> **Documento consolidado:** Ver sección 5 en [`ENTREGA_FINAL_LEONARDO_ARAYA.md`](ENTREGA_FINAL_LEONARDO_ARAYA.md).

> Firmware: `firmware/Firmware_Support/` | Monitor: **115200 baud**

---

## 1. Opciones de simulación

| Modo | Herramienta | Cuándo usar |
|------|-------------|-------------|
| **Wokwi** | [wokwi.com](https://wokwi.com) + `wokwi-cli` | Prototipo sin hardware, diagrama visual |
| **PlatformIO build** | `pio run` | Verificar compilación antes de flashear |
| **Python sim** | `scripts/automation/automation_scripts/esp32_simulator.py` | Emular respuestas BT/TCP en CI |

### Script unificado

```powershell
.\scripts\hardware-testing\simulate_esp32.ps1
.\scripts\hardware-testing\simulate_esp32.ps1 -Mode pio
.\scripts\hardware-testing\simulate_esp32.ps1 -Mode python
```

El script detecta automáticamente (`-Mode auto`):
1. `wokwi-cli` → simulación Wokwi
2. `pio` → compilación + monitor
3. Python → simulador local

---

## 2. Wokwi (investigación e integración)

Wokwi permite simular ESP32 en el navegador o vía CLI:

1. Instalar CLI: `npm install -g @wokwi/cli` (opcional)
2. El script crea `firmware/Firmware_Support/.wokwi/diagram.json` si no existe
3. Abrir [Wokwi ESP32](https://wokwi.com/projects/new/esp32) e importar el diagrama
4. Copiar el `.ino` desde `src/main/cim_esp32_firmware_v6.ino`

**Limitaciones:** BLE/SPP no se simulan con fidelidad RF completa; validar handshake en hardware real.

---

## 3. Hardware real — Flash y monitor

### Requisitos

- ESP32 DevKit (CP210x o CH340)
- Cable USB datos
- PlatformIO Core: `pip install platformio`

### Script de producción

```powershell
# Auto-detect puerto
.\scripts\hardware-testing\flash_and_monitor_esp32.ps1

# Puerto específico (Windows)
.\scripts\hardware-testing\flash_and_monitor_esp32.ps1 -Port COM3

# Solo monitor (sin upload)
.\scripts\hardware-testing\flash_and_monitor_esp32.ps1 -SkipUpload -Port COM3
```

Equivalente manual:

```powershell
cd firmware\Firmware_Support
pio run -t upload
pio device monitor -b 115200
```

---

## 4. Verificación post-flash

En el monitor serial debería aparecer:

```
[CIM-ESP32] Boot v6.0
[CIM-ESP32] BLE UART + SPP ready
[CIM-ESP32] Waiting IDENTIFY...
```

Desde la app Android:
1. Conceder permisos Bluetooth + ubicación
2. Escanear → seleccionar nodo `ESP32-CIM-*`
3. Enviar `IDENTIFY` → coordinador autoriza MAC

---

## 5. Troubleshooting

| Síntoma | Causa probable | Solución |
|---------|----------------|----------|
| `pio: command not found` | CLI no instalado | `pip install platformio` |
| Upload timeout | Puerto incorrecto / driver | `-Port COMx`, reinstalar driver CP210x |
| BLE no visible | ESP32 no flasheado | Re-ejecutar `flash_and_monitor_esp32.ps1` |
| SPP fallback | BLE MTU limitado | Normal; ver manual BT híbrido |

---

## 6. Referencias

- [03_MOTOR_BLUETOOTH_HIBRIDO.md](manuals/03_MOTOR_BLUETOOTH_HIBRIDO.md)
- [06_DESPLIEGUE_Y_CONFIGURACION.md](manuals/06_DESPLIEGUE_Y_CONFIGURACION.md)
- [GUIA_PROFESIONAL_CIM.md](GUIA_PROFESIONAL_CIM.md)
