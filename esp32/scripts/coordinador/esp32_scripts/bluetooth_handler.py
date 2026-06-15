# bluetooth_handler.py - MicroPython
import bluetooth
import binascii

class BLEHandler:
    def __init__(self, name="ESP32-C3-Panel"):
        self.ble = bluetooth.BLE()
        self.ble.active(True)
        self.ble.config(gap_name=name)
        # Aquí se añadiría la configuración de servicios y características para comandos

    def send_log(self, message):
        print(f"[LOG] {message}")
        # Lógica para enviar vía BLE
