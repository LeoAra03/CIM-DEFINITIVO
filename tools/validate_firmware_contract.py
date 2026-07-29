#!/usr/bin/env python3
"""Valida el contrato estático mínimo de firmware CIM pre-hardware.
No compila ni flashea firmware, ni certifica comportamiento eléctrico."""
from __future__ import annotations
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIRMWARE = ROOT / "esp32/firmware"
REQUIRED = {
    "esp32_plc_master.ino": ("GPIO5", "GPIO34", "STATION_UUID", "#include \"cim_ble_firmware.h\""),
    "esp32_scorbot_almacen.ino": ("STATION_UUID", "#include \"cim_ble_firmware.h\""),
    "esp32_scorbot_calidad.ino": ("STATION_UUID", "#include \"cim_ble_firmware.h\""),
    "esp32_scorbot_manufactura.ino": ("STATION_UUID", "#include \"cim_ble_firmware.h\""),
    "cim_ble_firmware.h": ("sendBleResponse", "CIM_ID", "STATION_UUID"),
}
def main() -> int:
    parser=argparse.ArgumentParser(description=__doc__); parser.add_argument('--quiet',action='store_true'); args=parser.parse_args()
    failures=[]
    for name, tokens in REQUIRED.items():
        path=FIRMWARE/name
        if not path.is_file(): failures.append(f"falta {path.relative_to(ROOT)}"); continue
        text=path.read_text(encoding='utf-8',errors='ignore')
        missing=[t for t in tokens if t not in text]
        if missing: failures.append(f"{path.relative_to(ROOT)}: faltan {', '.join(missing)}")
    if not failures and not args.quiet: print(f"PASS: contrato estático de {len(REQUIRED)} archivos de firmware")
    if failures:
        print("FAIL: " + "; ".join(failures)); return 1
    return 0
if __name__ == '__main__': raise SystemExit(main())
