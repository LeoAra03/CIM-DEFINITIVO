import time
import random

"""
SIMULADOR DE TRACKING DE PALLETS - CIM V2.0
Este script simula el movimiento de pallets a través de la cinta transportadora
y genera eventos para el Coordinador.
"""

class PalletTracker:
    def __init__(self):
        self.stations = ["PLC", "ALMACEN", "MANUFACTURA", "CALIDAD"]
        self.current_pallet_pos = -1

    def simulate_cycle(self):
        pallet_id = f"PAL-{random.randint(100, 999)}"
        print(f"[START] Iniciando ciclo para {pallet_id}")

        for station in self.stations:
            print(f"[STATUS] {pallet_id} ingresando a {station}...")
            time.sleep(2)
            print(f"[EVENT] {station} detectó Pallet ID: {pallet_id}")
            time.sleep(1)
            print(f"[OK] {station} completó operación.")

        print(f"[FINISH] {pallet_id} almacenado correctamente.")

if __name__ == "__main__":
    tracker = PalletTracker()
    try:
        while True:
            tracker.simulate_cycle()
            print("-" * 30)
            time.sleep(5)
    except KeyboardInterrupt:
        print("\n[STOP] Simulador detenido por el usuario.")
