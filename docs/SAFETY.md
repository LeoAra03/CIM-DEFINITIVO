# CIM safety

## Scope statement

This system is a laboratory simulation. It is **not certified** for
industrial use, is not a medical or safety-rated device, and must never be
connected to production machinery without a separate, qualified safety
review.

## Safety boundaries

- The lab network security configuration permits cleartext traffic only on
  the lab subnet.
- Critical firmware commands require authorization; the relay drops to OFF
  when the authorization watchdog expires.
- `E_STOP` is handled without authorization so a stop is always possible;
  it drops the PLC relay and blanks the LED.
- The coordinator exposes an operator E-stop that broadcasts `ABORT` to all
  stations.
- The wear interface exists solely to trigger the emergency stop from the
  wrist.

## Physical rules

- Keep the cell enclosed during robot motion.
- Never bypass the proximity sensor on the conveyor.
- Power the relay coil from the supply the firmware assumes; do not drive
  loads from the ESP32 GPIO directly.
- Disconnect USB power before rewiring.

## Operator checklist

1. Read this document and `docs/RISK.md` before first power-on.
2. Verify the E-stop path with `simulate_cell.py` before enabling relays.
3. Keep the default pairing secret only inside the isolated lab network.
4. Record every firmware flash in the lab notebook.

## Incident handling

Stop the cell, power the relay supply, and record the event in
`docs/AUDIT.md` context (audit chain entries are emitted by the coordinator
for authorization and command decisions).
