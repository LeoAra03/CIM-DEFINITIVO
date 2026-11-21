# CIM documentation

This folder is the documentation source of truth for the CIM industrial cell
platform. All documents are written in English.

## Document set

| Document | Purpose |
| --- | --- |
| [ARCHITECTURE](ARCHITECTURE.md) | System components and runtime topology |
| [PROTOCOL](PROTOCOL.md) | Wire protocol, framing, and command contract |
| [TESTING](TESTING.md) | Test strategy, simulation, and performance gates |
| [HARDWARE](HARDWARE.md) | ESP32 targets, wiring, and bench setup |
| [SAFETY](SAFETY.md) | Safety boundaries and non-certification statement |
| [RISK](RISK.md) | Risk register and mitigations |
| [AUDIT](AUDIT.md) | Audit evidence and traceability |
| [REPORT](REPORT.md) | Current status report |
| [PRESENTATION](PRESENTATION.md) | Stakeholder presentation overview |
| [ROADMAP](ROADMAP.md) | Delivery plan and milestones |

## Decision records

Accepted architectural choices live in [decisions/](decisions/README.md).
Each decision is immutable once accepted; superseding work creates a new
record.

## Assets

`assets/imagenes/` holds the application screenshots referenced by the root
README.

## Related material

The pre-hardware readiness gate (`tools/prehardware_readiness.py`) checks the
presence and key content of this document set before any physical station is
connected.
