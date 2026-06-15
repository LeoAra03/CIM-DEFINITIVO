#!/usr/bin/env python3
"""
ESP32 TCP Simulator for Integrated Panel testing
- Connects to a TCP server (default localhost:8888)
- Sends IDENTIFY message (CIM transport string)
- Periodically sends HEARTBEAT messages
- Responds to EXECUTE messages with ACK

Usage:
    python automation_scripts/esp32_simulator.py --host 127.0.0.1 --port 8888 --mac ESP32-01 --app PLC

"""
import socket
import argparse
import uuid
import time
import threading


def build_identify(mac, app='PLC', version='1.0'):
    msg_id = str(uuid.uuid4())
    ts = str(int(time.time() * 1000))
    # Format: ID|TIMESTAMP|SOURCE_MAC|SOURCE_APP|DEST_MAC|DEST_APP|CMD|PRIORITY|SESSIONID|PAYLOAD
    payload = f"{app}|{version}"
    parts = [msg_id, ts, mac, app, '', 'COORDINADOR', 'IDENTIFY', 'HIGH', '', payload]
    return '|'.join(parts) + '\n'


def build_heartbeat(mac, app='PLC'):
    msg_id = str(uuid.uuid4())
    ts = str(int(time.time() * 1000))
    parts = [msg_id, ts, mac, app, '', 'COORDINADOR', 'HEARTBEAT', 'LOW', '', '']
    return '|'.join(parts) + '\n'


def build_ack(original_line, our_mac, our_app='PLC'):
    # try to parse the original to extract id and fields
    try:
        parts = original_line.strip().split('|')
        orig_id = parts[0]
        dest_mac = parts[2] if len(parts) > 2 else ''
        dest_app = parts[3] if len(parts) > 3 else our_app
    except Exception:
        orig_id = str(uuid.uuid4())
        dest_mac = our_mac
        dest_app = our_app

    ack_id = orig_id + '_ACK'
    ts = str(int(time.time() * 1000))
    # sourceMac should be our MAC and sourceApp our app
    parts = [ack_id, ts, our_mac, our_app, '', 'COORDINADOR', 'ACK', 'NORMAL', '', '']
    return '|'.join(parts) + '\n'


class Esp32Sim:
    def __init__(self, host, port, mac, app='PLC', heartbeat_interval=5.0):
        self.host = host
        self.port = port
        self.mac = mac
        self.app = app
        self.heartbeat_interval = heartbeat_interval
        self.sock = None
        self.running = False

    def connect(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.settimeout(5.0)
        self.sock.connect((self.host, self.port))
        self.running = True
        print(f"Connected to {self.host}:{self.port}")

    def send_line(self, line):
        try:
            self.sock.sendall(line.encode('utf-8'))
            print("SENT:", line.strip())
        except Exception as e:
            print("Send error:", e)
            self.running = False

    def recv_loop(self):
        buf = b''
        while self.running:
            try:
                data = self.sock.recv(4096)
                if not data:
                    print("Connection closed by server")
                    self.running = False
                    break
                buf += data
                while b'\n' in buf:
                    line, buf = buf.split(b'\n', 1)
                    line_s = line.decode('utf-8')
                    print("RECV:", line_s)
                    self.handle_line(line_s)
            except socket.timeout:
                continue
            except Exception as e:
                print("Recv error:", e)
                self.running = False
                break

    def handle_line(self, line):
        # If we receive an EXECUTE command, reply ACK
        try:
            parts = line.split('|')
            if len(parts) > 6:
                cmd = parts[6]
                if cmd == 'EXECUTE':
                    ack = build_ack(line, self.mac, self.app)
                    self.send_line(ack)
        except Exception as e:
            print("Handle line error:", e)

    def heartbeat_loop(self):
        while self.running:
            try:
                hb = build_heartbeat(self.mac, self.app)
                self.send_line(hb)
            except Exception as e:
                print("Heartbeat error:", e)
                self.running = False
                break
            time.sleep(self.heartbeat_interval)

    def run(self):
        try:
            self.connect()
            # send IDENTIFY right away
            id_msg = build_identify(self.mac, self.app)
            self.send_line(id_msg)

            # start recv thread
            t_recv = threading.Thread(target=self.recv_loop, daemon=True)
            t_recv.start()

            # start heartbeat
            t_hb = threading.Thread(target=self.heartbeat_loop, daemon=True)
            t_hb.start()

            # keep main alive while threads run
            while self.running:
                time.sleep(0.5)

        except Exception as e:
            print("Simulator error:", e)
        finally:
            try:
                if self.sock:
                    self.sock.close()
            except Exception:
                pass


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8888)
    parser.add_argument('--mac', default='ESP32-01')
    parser.add_argument('--app', default='PLC')
    args = parser.parse_args()

    sim = Esp32Sim(args.host, args.port, args.mac, args.app)
    sim.run()
