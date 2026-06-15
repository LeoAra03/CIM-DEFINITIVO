import socket
import time
import sys

"""
SISTEMA DE CONTROL MAESTRO ESP32 - CIM V2.0
Este script actúa como puente entre el Coordinador Android y los ESP32 físicos.
Implementa lógica de detección de Pallet ID basada en los protocolos industriales definidos.
"""

class ESP32Controller:
    def __init__(self, ip, port=80):
        self.ip = ip
        self.port = port
        self.pallet_registry = {
            "1": "@00WD000900015B*",
            "2": "@00WD0010000153*",
            "3": "@00WD0011000152*",
            "5": "@00WD0013000150*",
            "6": "@00WD0014000157*"
        }

    def send_command(self, command):
        try:
            print(f"[TCP] Conectando a ESP32 en {self.ip}:{self.port}...")
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.settimeout(5)
                s.connect((self.ip, self.port))
                full_cmd = f"{command}\r\n"
                s.sendall(full_cmd.encode())
                print(f"[OK] Comando enviado: {command}")
                return True
        except Exception as e:
            print(f"[ERROR] No se pudo comunicar con el hardware: {e}")
            return False

    def deliver_pallet(self, pallet_id):
        if pallet_id in self.pallet_registry:
            cmd = self.pallet_registry[pallet_id]
            print(f"[LOGIC] Ejecutando DELIVER para PALLET_{pallet_id}")
            return self.send_command(cmd)
        else:
            print(f"[WARN] ID de Pallet {pallet_id} no reconocido en el protocolo.")
            return False

    def emergency_stop(self):
        print("[CRITICAL] ENVIANDO PARADA DE EMERGENCIA")
        return self.send_command("STOP*")

if __name__ == "__main__":
    # Ejemplo de ejecución
    if len(sys.argv) < 3:
        print("Uso: python esp32_master_controller.py <IP_ESP32> <COMANDO/PALLET_ID>")
        sys.exit(1)

    ip_target = sys.argv[1]
    action = sys.argv[2]

    controller = ESP32Controller(ip_target)

    if action.isdigit():
        controller.deliver_pallet(action)
    elif action == "STOP":
        controller.emergency_stop()
    else:
        controller.send_command(action)
