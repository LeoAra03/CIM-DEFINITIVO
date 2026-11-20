# 0003. Repository policy

Date: 2026-01-14. Status: accepted.

## Context

The repository mixed languages, nested Gradle trees, duplicate wrappers,
generated artifacts, and migration markers, which blocked professional
presentation and reliable CI.

## Decision

- Active tree contains only source, docs, tools, scripts, and config; no
  legacy, report-dump, or delivery-staging directories.
- Exactly one Gradle wrapper lives at `config/gradle/wrapper/`.
- Production code, comments, KDoc, and documentation are English; UI text
  stays in `strings.xml`; no emoji anywhere.
- Versioning is normalized to `6.0.0` / `600` through
  `config/libs.versions.toml`.
- Bot and agent identities are mapped to a single maintainer identity via
  `.mailmap`, with history consolidation performed by
  `scripts/rewrite-history.sh`.

## Consequences

- `tools/validate_system.py` enforces the policy in CI.
- Contributors see one build entry point and one dependency catalog.
- History cleanup is explicit, manual, and documented rather than
  implicit.
