#!/usr/bin/env python3
"""Repository signing and secret posture gate.

The gate fails when:
- a keystore, JKS, or private key file is tracked by git;
- `release.properties` or an inline store password appears in tracked files;
- an application manifest drops the network security configuration;
- signing-related files are not covered by `.gitignore`.

Exit code 0 means the repository passed this gate.
"""
from __future__ import annotations

import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]

FORBIDDEN_SUFFIXES = (".jks", ".keystore", ".p12", ".pfx", ".key")
FORBIDDEN_NAMES = {"release.properties", "local.properties"}
SECRET_MARKERS = (
    "CIM_RELEASE_STORE_PASSWORD=",
    "storePassword=",
    "keyPassword=",
)
APP_MODULES = [
    "app-coordinador",
    "app-plc",
    "app-manufactura",
    "app-calidad",
    "app-almacen",
    "wear-coordinador",
]


def tracked_files() -> list[str]:
    result = subprocess.run(
        ["git", "ls-files"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise SystemExit("git ls-files failed; run this gate inside the repository")
    return [line for line in result.stdout.splitlines() if line]


def main() -> int:
    errors: list[str] = []
    files = tracked_files()

    for rel in files:
        lower = rel.lower()
        if lower.endswith(FORBIDDEN_SUFFIXES):
            errors.append(f"signing material must not be tracked: {rel}")
        if pathlib.Path(lower).name in FORBIDDEN_NAMES:
            errors.append(f"local configuration must not be tracked: {rel}")

    gitignore = (ROOT / ".gitignore").read_text(encoding="utf-8")
    for pattern in ("*.jks", "*.keystore", "release.properties"):
        if pattern not in gitignore:
            errors.append(f".gitignore must cover {pattern}")

    for rel in files:
        path = ROOT / rel
        if path.suffix in {".png", ".jpg", ".jar", ".apk", ".pt", ".tflite"}:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        for marker in SECRET_MARKERS:
            if marker in text and not rel.endswith("validate_security.py"):
                errors.append(f"inline signing secret reference {marker!r} in {rel}")

    for module in APP_MODULES:
        manifest = ROOT / "android" / module / "src/main/AndroidManifest.xml"
        if not manifest.exists():
            errors.append(f"missing manifest for {module}")
            continue
        if "networkSecurityConfig" not in manifest.read_text(encoding="utf-8"):
            errors.append(f"{module} manifest must reference @xml/network_security_config")
        nsc = ROOT / "android" / module / "src/main/res/xml/network_security_config.xml"
        if not nsc.exists():
            errors.append(f"{module} is missing res/xml/network_security_config.xml")

    if errors:
        print("SECURITY VALIDATION FAILED", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("SECURITY VALIDATION PASSED: no tracked signing material or inline secrets.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
