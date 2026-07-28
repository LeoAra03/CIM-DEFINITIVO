#!/usr/bin/env python3
"""
Hub Simulador CIM
Servidor TCP que simula el Coordinador CIM (8888)
Responde a comandos de estaciones: AUTH, PING, ARUCO, LASER, etc.
"""
import socket
import threading
import sys
from datetime import datetime

HOST = "0.0.0.0"
PORT = 8888
TIMEOUT = 60

def log(msg, level="INFO"):
    """Log con timestamp"""
    ts = datetime.now().strftime("%H:%M:%S")
    print(f"[{ts}] [{level}] {msg}")

def handle_client(conn, addr):
    """Manejar conexión de una estación"""
    log(f"Conexión aceptada desde {addr[0]}:{addr[1]}")
    conn.settimeout(TIMEOUT)

    try:
        while True:
            data = conn.recv(4096)
            if not data:
                log(f"Conexión cerrada por {addr[0]}", "WARN")
                break

            command = data.decode(errors='ignore').strip()
            log(f"RX de {addr[0]}: {command}", "CMD")

            response = process_command(command, addr[0])
            if response:
                conn.sendall((response + "\n").encode())
                log(f"TX a {addr[0]}: {response}", "RESP")

    except socket.timeout:
        log(f"Timeout: {addr[0]} inactivo por {TIMEOUT}s", "WARN")
    except Exception as e:
        log(f"Error en {addr[0]}: {e}", "ERROR")
    finally:
        conn.close()
        log(f"Conexión cerrada: {addr[0]}", "INFO")

def process_command(cmd, client_ip):
    """Procesar comando y retornar respuesta"""

    if cmd.startswith("AUTH"):
        # Comando: AUTH:station_name:password
        return "AUTH:VALIDATED"

    elif cmd.startswith("PING"):
        return "PONG"

    elif cmd.startswith("ARUCO_GENERATE"):
        # Comando: ARUCO_GENERATE:id:size:dict
        parts = cmd.split(":")
        aruco_id = parts[1] if len(parts) > 1 else "0"
        return f"ARUCO_GENERATED:{aruco_id}:OK"

    elif cmd.startswith("LASER_LOAD"):
        # Comando: LASER_LOAD:filename:base64_data
        return "LASER:LOADED"

    elif cmd.startswith("GCODE_LOAD"):
        # Comando: GCODE_LOAD:filename:base64_data
        return "GCODE:LOADED"

    elif cmd.startswith("R:"):
        # Robot command (R:HOME, R:READY, R:RUN program, etc.)
        subcmd = cmd[2:]
        return f"ROBOT:ACK:{subcmd}"

    elif cmd.startswith("L:"):
        # Laser command (L:START, L:STOP, etc.)
        subcmd = cmd[2:]
        return f"LASER:ACK:{subcmd}"

    elif cmd.startswith("M:"):
        # Manufactura command
        subcmd = cmd[2:]
        return f"MANUFACTURA:ACK:{subcmd}"

    elif cmd.startswith("C:"):
        # Calidad command
        subcmd = cmd[2:]
        return f"CALIDAD:ACK:{subcmd}"

    else:
        # Default: echo
        return f"ACK:{cmd}"

def main():
    """Servidor principal"""
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        server_socket.bind((HOST, PORT))
        server_socket.listen(5)
        log(f"Hub simulador escuchando en {HOST}:{PORT}")
        log("Esperando conexiones de estaciones (CTRL+C para terminar)...")

        while True:
            try:
                conn, addr = server_socket.accept()
                thread = threading.Thread(
                    target=handle_client,
                    args=(conn, addr),
                    daemon=True
                )
                thread.start()
            except KeyboardInterrupt:
                raise
            except Exception as e:
                log(f"Error aceptando conexión: {e}", "ERROR")

    except KeyboardInterrupt:
        log("Cerrando servidor...", "WARN")
    except Exception as e:
        log(f"Error servidor: {e}", "ERROR")
        sys.exit(1)
    finally:
        server_socket.close()
        log("Servidor cerrado", "INFO")

if __name__ == "__main__":
    main()

