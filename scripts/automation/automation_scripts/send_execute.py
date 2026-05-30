#!/usr/bin/env python3
"""
Simple sender to inject EXECUTE CIM messages into the TCP server.
Usage:
    python automation_scripts/send_execute.py --host 127.0.0.1 --port 8888 --mac TEST_CLIENT --destMac ESP32-01 --command "QC_PROGRAM_SR1_START"
"""
import socket
import argparse
import uuid
import time


def build_execute(source_mac, source_app, dest_mac, dest_app, command):
    msg_id = str(uuid.uuid4())
    ts = str(int(time.time() * 1000))
    parts = [msg_id, ts, source_mac, source_app, dest_mac, dest_app, 'EXECUTE', 'NORMAL', '', command]
    return '|'.join(parts) + '\n'


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8888)
    parser.add_argument('--mac', default='TEST_CLIENT')
    parser.add_argument('--app', default='COORDINADOR')
    parser.add_argument('--destMac', default='')
    parser.add_argument('--destApp', default='MANUFACTURA')
    parser.add_argument('--command', default='TEST_MESSAGE')
    args = parser.parse_args()

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(5.0)
    s.connect((args.host, args.port))
    msg = build_execute(args.mac, args.app, args.destMac, args.destApp, args.command)
    print('Sending:', msg.strip())
    s.sendall(msg.encode('utf-8'))
    s.close()
