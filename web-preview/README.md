# CIM visual preview

This directory contains a web simulator of the CIM system that mirrors the
five Android APKs plus the wear app and the coordinator hub.

## How to use (one click)

1. Open `index.html` in your browser or use the preview server on port
   `:8000`.
2. Click **Start Hub :8888**.
3. Click **Connect all (one click)** to connect five stations at once.
4. If AUTO MODE is off, approve each station in the dialog that appears.
5. If AUTO MODE is on, stations are approved automatically (lab only).
6. Send commands: conveyor, robot, laser, vision, storage.

## Views per APK

Each APK has its own visual index that imitates the original Compose
design:

- **Coordinator**: `apps/coordinador.html` - hub with EXEC, CONVEYOR,
  ROBOT, ARUCO, MAP, NODES, RACKS tabs
- **PLC conveyor**: `apps/plc.html` - 3x10 DELIVER matrix, pallet tracking
- **Manufacturing**: `apps/manufactura.html` - Scorbot + laser + sync
- **Quality**: `apps/calidad.html` - camera + ArUco + YOLO TFLite
- **Storage**: `apps/almacen.html` - 3x3 rack + Scorbot storage
- **Wear**: `apps/wear.html` - compact round supervision view

## Industrial design

Colors match the real applications:

- Background `#080808`, cards `#1E1E1E`, border `#2A2A2A`
- Primary `#FF6D00` (industrial orange), success `#4CAF50`,
  error `#B00020`
- JetBrains Mono monospace terminal with real-time logs
- SVG wiring diagram with animated hub-to-station lines

## Security behavior demonstrated

This visual demo reflects the protections implemented in the real code:

- TLS without trust-all (system CAs)
- PLC relay requires authorization with a 10s watchdog
- G-code sanitization (no path traversal)
- Auto-approve only on debug builds
- Pairing secret validation

## Server

```bash
cd web-preview
python3 -m http.server 8000 --bind 0.0.0.0
# Open https://8000-XXXX.e2b.app
```

## Visual flow

```
[Hub :8888] -- NSD _cim-hub._tcp. -->
   |-> PLC (conveyor) -- C:DELIVER:1:3 -->
   |-> Manufacturing (robot) -- R:RUN ARU1 -->
   |-> Quality (vision) -- ARUCO:DETECT -->
   |-> Storage (rack) -- STO:STORE -->
   └-> Wear (monitor)
```

Every command passes authorization validation and is written to the
central terminal.
