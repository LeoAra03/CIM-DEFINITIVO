# Security Policy

## Reporting a vulnerability

Report suspected vulnerabilities privately. Open a GitHub security advisory at
`LeoAra03/CIM-DEFINITIVO` when possible, or email the address registered on
the owner's GitHub profile. Do not open a public issue for exploitable
vulnerabilities.

Include:

- A description of the issue and its impact.
- Reproduction steps or a proof of concept.
- Affected components, commits, or versions.

You should receive an acknowledgement within seven days. Valid reports will be
coordinated with you on disclosure timing.

## Scope

The coordinator application, the station applications, the ESP32 firmware
sources under `firmware/`, and the protocol documentation under `docs/` are in
scope.

## Security baseline

- BLE pairing rejects the default pairing secret when a custom secret is set.
- TCP station authorization is enforced before any actuator command is
  forwarded.
- Releases are signed with a local keystore that is never committed;
  `release.properties` and `*.jks` / `*.keystore` files are git-ignored.
- The release manifest enables cleartext traffic only through the lab network
  security configuration.
- The firmware rejects actuator commands coming from an unauthorized peer
  inside the `handleCommand` dispatch.
