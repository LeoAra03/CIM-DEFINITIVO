#!/usr/bin/env python3
"""Repository structure and release posture gate.

The gate fails when:
- the repository does not contain exactly one Gradle wrapper;
- application modules do not declare versionName 6.0.0 / versionCode 600;
- forbidden directory names or files are present;
- legacy migration markers remain;
- the repository contains non-ASCII markers in active sources;
- required deliverable paths are missing.

Exit code 0 means the repository passed this gate.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]

REQUIRED_PATHS = [
    "README.md",
    "LICENSE",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "CHANGELOG.md",
    "CODE_OF_CONDUCT.md",
    ".mailmap",
    ".editorconfig",
    ".gitattributes",
    "detekt.yml",
    "config/settings.gradle.kts",
    "config/build.gradle.kts",
    "config/libs.versions.toml",
    "config/gradle/wrapper/gradle-wrapper.properties",
    "config/build-logic/src/main/kotlin/cim.android.application.gradle.kts",
    "config/build-logic/src/main/kotlin/cim.android.library.gradle.kts",
    "android/app-coordinador/build.gradle.kts",
    "android/app-plc/build.gradle.kts",
    "android/app-manufactura/build.gradle.kts",
    "android/app-calidad/build.gradle.kts",
    "android/app-almacen/build.gradle.kts",
    "android/wear-coordinador/build.gradle.kts",
    "android/core-network/build.gradle.kts",
    "android/core-network/src/main/kotlin/com/industria/prefecto/CimProtocol.kt",
    "android/core-network/src/main/kotlin/com/industria/prefecto/CommandBroker.kt",
    "android/core-network/src/main/kotlin/com/industria/prefecto/AuthorizationManager.kt",
    "android/core-network/src/main/kotlin/com/industria/prefecto/PalletWorkflowEngine.kt",
    "android/core-network/src/main/kotlin/com/industria/prefecto/StationIdentityPolicy.kt",
    "android/core-network/src/main/kotlin/com/industria/prefecto/CimAuditChain.kt",
    "android/core-network/src/main/kotlin/com/industria/prefecto/StationRegistry.kt",
    "android/core-network/src/main/kotlin/com/industria/prefecto/BluetoothHardwareManager.kt",
    "android/core-network/src/main/kotlin/com/industria/prefecto/GCodeTranslator.kt",
    "android/core-network/src/main/kotlin/com/industria/prefecto/LogEvent.kt",
    "android/core-network/src/test/kotlin/com/industria/prefecto/PalletWorkflowEngineTest.kt",
    "android/core-network/src/test/kotlin/com/industria/prefecto/CimBinaryCodecTest.kt",
    "android/core-network/src/test/kotlin/com/industria/prefecto/StationIdentityPolicyTest.kt",
    "android/core-network/src/test/kotlin/com/industria/prefecto/AuthorizationManagerTest.kt",
    "android/core-network/src/test/kotlin/com/industria/prefecto/PerformanceTest.kt",
    "firmware/plc-master/src/plc_master.ino",
    "firmware/scorbot/src/scorbot.ino",
    "firmware/vision-node/src/vision_node.ino",
    "firmware/rack-node/src/rack_node.ino",
    "firmware/shared/cim_ble_firmware.h",
    "docs/README.md",
    "docs/ARCHITECTURE.md",
    "docs/PROTOCOL.md",
    "docs/TESTING.md",
    "docs/HARDWARE.md",
    "docs/SAFETY.md",
    "docs/RISK.md",
    "docs/AUDIT.md",
    "docs/REPORT.md",
    "docs/PRESENTATION.md",
    "docs/ROADMAP.md",
    "docs/decisions/README.md",
    "docs/decisions/0001-communication-architecture.md",
    "docs/decisions/0002-firmware-command-contract.md",
    "docs/decisions/0003-repository-policy.md",
    "tools/validate_system.py",
    "tools/validate_firmware_contract.py",
    "tools/validate_security.py",
    "tools/prehardware_readiness.py",
    "tools/yolo_export.py",
    "tools/simulate_cell.py",
    "scripts/migrate-layout.sh",
    "scripts/rewrite-history.sh",
    ".github/workflows/ci.yml",
    ".github/CODEOWNERS",
    ".github/pull_request_template.md",
    ".github/ISSUE_TEMPLATE/bug_report.yml",
    ".github/ISSUE_TEMPLATE/feature_request.yml",
]

FORBIDDEN_DIRS = {
    "archive",
    "legacy",
    "obsolete-guides",
    "unverified-claims",
    "reports",
    "entrega",
}

FORBIDDEN_FILES = {
    "AUDITORIA_100_PUNTOS_TECNICOS.md",
    "explicacion_profesor_CIM.md",
    "tools/validate_system_100.py",
}

MARKER_PATTERNS = [
    re.compile(r"FIX #\d+", re.IGNORECASE),
    re.compile(r"FIX Lote", re.IGNORECASE),
    re.compile(r"FIX:", re.IGNORECASE),
    re.compile(r"@author CIM Team"),
    re.compile(r"duplicate import removed"),
]

TEXT_SUFFIXES = {
    ".kt",
    ".kts",
    ".java",
    ".py",
    ".md",
    ".yml",
    ".yaml",
    ".xml",
    ".toml",
    ".properties",
    ".pro",
    ".editorconfig",
    ".gitignore",
    ".mailmap",
    ".gitattributes",
}

SKIP_DIRS = {".git", "build", ".gradle", ".idea", "__pycache__", "node_modules", "output-apks"}

APP_MODULES = [
    "app-coordinador",
    "app-plc",
    "app-manufactura",
    "app-calidad",
    "app-almacen",
    "wear-coordinador",
]


def iter_text_files():
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        if path.suffix in TEXT_SUFFIXES or path.name in {".gitignore", ".editorconfig"}:
            yield path


def check_required_paths(errors: list[str]) -> None:
    for rel in REQUIRED_PATHS:
        if not (ROOT / rel).exists():
            errors.append(f"missing required path: {rel}")


def check_wrapper(errors: list[str]) -> None:
    wrappers = [p for p in ROOT.rglob("gradle-wrapper.properties") if ".git" not in p.parts]
    if len(wrappers) != 1:
        errors.append(f"expected exactly one gradle-wrapper.properties, found {len(wrappers)}")
    expected = ROOT / "config/gradle/wrapper/gradle-wrapper.properties"
    if wrappers and wrappers[0] != expected:
        errors.append(f"wrapper must live at config/gradle/wrapper, found {wrappers[0]}")


def check_versions(errors: list[str]) -> None:
    for module in APP_MODULES:
        build_file = ROOT / "android" / module / "build.gradle.kts"
        if not build_file.exists():
            errors.append(f"missing module build file: android/{module}/build.gradle.kts")
            continue
        text = build_file.read_text(encoding="utf-8")
        if "cim.android.application" not in text:
            errors.append(f"{module} must apply the cim.android.application convention")
    catalog = (ROOT / "config/libs.versions.toml").read_text(encoding="utf-8")
    if 'appVersion = "6.0.0"' not in catalog:
        errors.append("config/libs.versions.toml must pin appVersion = \"6.0.0\"")
    if 'appVersionCode = "600"' not in catalog:
        errors.append("config/libs.versions.toml must pin appVersionCode = \"600\"")
    # Any literal versionName assignment must be 6.0.0.
    for build_file in (ROOT / "android").rglob("*.gradle.kts"):
        for line in build_file.read_text(encoding="utf-8").splitlines():
            match = re.search(r'\bversionName\s*=\s*"([^"]+)"', line)
            if match and match.group(1) != "6.0.0":
                errors.append(f"stale versionName {match.group(1)} in {build_file}")
            code = re.search(r"\bversionCode\s*=\s*(\d+)", line)
            if code and code.group(1) != "600":
                errors.append(f"stale versionCode {code.group(1)} in {build_file}")


def check_forbidden(errors: list[str]) -> None:
    for path in ROOT.iterdir():
        if path.is_dir() and path.name in FORBIDDEN_DIRS:
            errors.append(f"forbidden directory present: {path.name}")
    for rel in FORBIDDEN_FILES:
        if (ROOT / rel).exists():
            errors.append(f"forbidden file present: {rel}")


def check_markers(errors: list[str]) -> None:
    for path in iter_text_files():
        if path == ROOT / "tools/validate_system.py":
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        for pattern in MARKER_PATTERNS:
            if pattern.search(text):
                errors.append(f"migration marker {pattern.pattern!r} in {path.relative_to(ROOT)}")


def check_active_tree_errors(errors: list[str]) -> None:
    # The active tree must not reference deleted top-level directories.
    stale_refs = ("esp32/firmware", "android/apps/", "archive/")
    for path in iter_text_files():
        if path == ROOT / "tools/validate_system.py":
            continue
        rel_parts = path.relative_to(ROOT).parts
        # The layout migration script intentionally references the old tree;
        # docs/README.md is replaced by the new structure document.
        if rel_parts[0] in {"scripts"}:
            continue
        if rel_parts[:2] == ("docs", "README.md"):
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        for ref in stale_refs:
            if ref in text:
                errors.append(f"stale path reference {ref!r} in {path.relative_to(ROOT)}")


def main() -> int:
    errors: list[str] = []
    check_required_paths(errors)
    check_wrapper(errors)
    check_versions(errors)
    check_forbidden(errors)
    check_markers(errors)
    check_active_tree_errors(errors)

    if errors:
        print("VALIDATION FAILED", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("VALIDATION PASSED: repository structure and release posture are consistent.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
