#!/usr/bin/env python3
"""Validación estructural 100% para el modo simulado del Sistema CIM.

El objetivo de este script es reemplazar verificaciones antiguas con rutas
obsoletas (1_DOCUMENTACION, 3_FIRMWARE_ESP32, 4_SCRIPTS) por comprobaciones
sobre la estructura activa del repositorio. No certifica hardware físico ni
seguridad funcional; sólo valida que los entregables automatizables del modo
simulado estén presentes y sean coherentes.
"""
from __future__ import annotations

import argparse
import json
import py_compile
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable


@dataclass
class CheckResult:
    name: str
    passed: bool
    detail: str


@dataclass
class ValidationReport:
    generatedAtUtc: str
    scope: str
    passed: int
    total: int
    percent: float
    status: str
    checks: list[CheckResult]
    note: str


def repo_root() -> Path:
    return Path(__file__).resolve().parents[1]


def rel(root: Path, path: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def exists_all(root: Path, paths: list[str]) -> tuple[bool, str]:
    missing = [p for p in paths if not (root / p).exists()]
    if missing:
        return False, "Faltan: " + ", ".join(missing)
    return True, f"{len(paths)} rutas presentes"


def check_android_modules(root: Path) -> CheckResult:
    modules = [
        "android/apps/app-coordinador/app",
        "android/apps/app-plc/app",
        "android/apps/app-manufactura/app",
        "android/apps/app-calidad/app",
        "android/apps/app-almacen/app",
        "android/apps/wear-coordinador/app",
    ]
    required = []
    for module in modules:
        required.extend([
            f"{module}/build.gradle.kts",
            f"{module}/src/main/AndroidManifest.xml",
        ])
    ok, detail = exists_all(root, required)
    return CheckResult("Android: 5 apps + Wear con Gradle y Manifest", ok, detail)


def check_core_network(root: Path) -> CheckResult:
    source_files = list((root / "android/core-network/src/main/java").rglob("*.kt"))
    test_files = list((root / "android/core-network/src/test/java").rglob("*.kt"))
    ok = len(source_files) >= 30 and len(test_files) >= 8
    detail = f"{len(source_files)} fuentes Kotlin, {len(test_files)} tests JVM"
    return CheckResult("core-network implementado y cubierto por tests", ok, detail)


def check_firmware(root: Path) -> CheckResult:
    expected = [
        "esp32/firmware/esp32_plc_master.ino",
        "esp32/firmware/esp32_scorbot_almacen.ino",
        "esp32/firmware/esp32_scorbot_calidad.ino",
        "esp32/firmware/esp32_scorbot_manufactura.ino",
        "esp32/firmware/cim_ble_firmware.h",
        "esp32/firmware/README.md",
    ]
    ok, detail = exists_all(root, expected)
    return CheckResult("Firmware ESP32 activo canónico", ok, detail)


def check_tools(root: Path) -> CheckResult:
    expected = [
        "tools/hub_simulator.py",
        "tools/vision_safety_simulator.py",
        "tools/inspect_yolo_checkpoint.py",
        "tools/export_yolo_to_tflite.py",
        "tools/tflite_yolo_test.py",
        "tools/validate_firmware_contract.py",
        "tools/prehardware_readiness.py",
        "tools/powershell/Validar_Sistema_100pc.ps1",
        "tools/powershell/Simular_Ciclo_Completo.ps1",
        "tools/powershell/Instalar-APKs.ps1",
        "tools/powershell/Flashear-ESP32.ps1",
        "tools/powershell/copy_apks.ps1",
    ]
    ok, detail = exists_all(root, expected)
    return CheckResult("Herramientas operativas de simulación, visión, APK y ESP32", ok, detail)


def check_docs(root: Path) -> CheckResult:
    expected = [
        "README.md",
        "docs/README.md",
        "docs/INDEX_REPOSITORIO.md",
        "CONTRIBUTING.md",
        "SECURITY.md",
        "CHANGELOG.md",
        "docs/quickstart/README.md",
        "docs/deliverables/QUALITY_GATES.md",
        "docs/deliverables/INFORME_TECNICO_DE_AVANCE.md",
        "docs/deliverables/BITACORA_VALIDACION.md",
        "docs/deliverables/ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.pdf",
        "docs/deliverables/ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.md",
        "docs/deliverables/PROTOCOLO_PRUEBAS_HARDWARE.md",
        "android/README.md",
        "esp32/firmware/README.md",
    ]
    ok, detail = exists_all(root, expected)
    return CheckResult("Documentación activa y trazabilidad de entrega", ok, detail)


def check_python_syntax(root: Path) -> CheckResult:
    files = sorted((root / "tools").glob("*.py"))
    failures: list[str] = []
    for file in files:
        try:
            py_compile.compile(str(file), doraise=True)
        except py_compile.PyCompileError as exc:
            failures.append(f"{rel(root, file)}: {exc.msg}")
    ok = not failures
    detail = f"{len(files)} scripts Python compilados" if ok else "; ".join(failures)
    return CheckResult("Sintaxis Python de herramientas", ok, detail)


def check_no_stale_active_paths(root: Path) -> CheckResult:
    stale_tokens = ("1_DOCUMENTACION", "2_APK_ANDROID", "3_FIRMWARE_ESP32", "4_SCRIPTS")
    active_files = [
        root / "README.md",
        *sorted((root / "tools").rglob("*.ps1")),
        *sorted((root / "docs/deliverables").glob("*.md")),
    ]
    offenders: list[str] = []
    for file in active_files:
        if not file.is_file():
            continue
        text = file.read_text(encoding="utf-8", errors="ignore")
        for token in stale_tokens:
            if token in text:
                offenders.append(f"{rel(root, file)} contiene {token}")
    ok = not offenders
    detail = "sin rutas obsoletas en README/tools/deliverables" if ok else "; ".join(offenders[:10])
    return CheckResult("Sin rutas obsoletas en fuentes activas", ok, detail)


def check_no_versioned_apks(root: Path) -> CheckResult:
    try:
        completed = subprocess.run(
            ["git", "ls-files", "*.apk"],
            cwd=root,
            check=True,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        apks = [line.strip() for line in completed.stdout.splitlines() if line.strip()]
    except Exception:
        apks = [
            rel(root, path)
            for path in sorted(root.rglob("*.apk"))
            if ".git" not in path.parts and "build" not in path.parts and "output-apks" not in path.parts
        ]
    ok = not apks
    detail = "no hay APKs versionadas" if ok else ", ".join(apks[:10])
    return CheckResult("Sin binarios APK versionados", ok, detail)



def check_android_manifest_security(root: Path) -> CheckResult:
    namespace = "{http://schemas.android.com/apk/res/android}"
    manifests = sorted((root / "android/apps").glob("*/app/src/main/AndroidManifest.xml"))
    manifests += sorted((root / "android/apps/wear-coordinador/app/src/main").glob("AndroidManifest.xml"))
    offenders: list[str] = []

    for manifest in manifests:
        try:
            tree = ET.parse(manifest)
        except ET.ParseError as exc:
            offenders.append(f"{rel(root, manifest)} XML inválido: {exc}")
            continue

        app = tree.getroot().find("application")
        if app is None:
            offenders.append(f"{rel(root, manifest)} sin <application>")
            continue

        if app.attrib.get(namespace + "allowBackup") == "true":
            offenders.append(f"{rel(root, manifest)} permite allowBackup=true")
        if app.attrib.get(namespace + "usesCleartextTraffic") == "true":
            offenders.append(f"{rel(root, manifest)} permite cleartext global")

        for permission in tree.getroot().findall("uses-permission"):
            name = permission.attrib.get(namespace + "name", "")
            max_sdk = permission.attrib.get(namespace + "maxSdkVersion")
            if name == "android.permission.WRITE_EXTERNAL_STORAGE" and max_sdk != "28":
                offenders.append(f"{rel(root, manifest)} WRITE_EXTERNAL_STORAGE debe tener maxSdkVersion=28")
            if name == "android.permission.READ_EXTERNAL_STORAGE" and max_sdk != "32":
                offenders.append(f"{rel(root, manifest)} READ_EXTERNAL_STORAGE debe tener maxSdkVersion=32")
            if name in {"android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"} and max_sdk != "30":
                offenders.append(f"{rel(root, manifest)} {name.split('.')[-1]} debe tener maxSdkVersion=30")

    ok = not offenders
    detail = f"{len(manifests)} manifests sin backup/cleartext inseguro" if ok else "; ".join(offenders[:10])
    return CheckResult("Manifiestos Android endurecidos", ok, detail)


def check_no_active_security_placeholders(root: Path) -> CheckResult:
    sensitive_tokens = (
        "UBB_CIM_PRO_SECURE_2024",
        "cimkeystorepass",
        "release.keystore",
        "usesCleartextTraffic=\"true\"",
        "allowBackup=\"true\"",
        "TODO:",
    )
    scan_roots = [
        root / "README.md",
        root / "android",
        root / "config",
        root / "tools",
        root / "docs/deliverables",
    ]
    skip_parts = {".git", "archive", "build", "output-apks", "__pycache__"}
    offenders: list[str] = []
    this_file = Path(__file__).resolve()

    for scan_root in scan_roots:
        if scan_root.is_file():
            candidates = [scan_root]
        elif scan_root.is_dir():
            candidates = [path for path in scan_root.rglob("*") if path.is_file()]
        else:
            continue

        for file in candidates:
            if file.resolve() == this_file or any(part in skip_parts for part in file.parts):
                continue
            if file.suffix.lower() in {".apk", ".jar", ".png", ".jpg", ".jpeg", ".webp", ".pdf", ".pt", ".tflite"}:
                continue
            text = file.read_text(encoding="utf-8", errors="ignore")
            for token in sensitive_tokens:
                if token in text:
                    offenders.append(f"{rel(root, file)} contiene {token}")

    ok = not offenders
    detail = "sin secretos conocidos, TODO activos ni flags inseguros" if ok else "; ".join(offenders[:10])
    return CheckResult("Sin secretos/placeholders de seguridad activos", ok, detail)

def check_ci_workflow(root: Path) -> CheckResult:
    workflow = root / ".github/workflows/android-ci.yml"
    if not workflow.is_file():
        return CheckResult("Workflow CI Android", False, "falta .github/workflows/android-ci.yml")
    text = workflow.read_text(encoding="utf-8", errors="ignore")
    required_tokens = ["testAllModules", "buildAllApks", "actions/upload-artifact", "workflow_dispatch"]
    missing = [token for token in required_tokens if token not in text]
    ok = not missing
    detail = "CI cubre tests/build y permite ejecución manual por workflow_dispatch" if ok else "faltan tokens: " + ", ".join(missing)
    return CheckResult("Workflow CI Android reproducible", ok, detail)


def check_gradle_root(root: Path) -> CheckResult:
    expected = [
        "config/settings.gradle.kts",
        "config/build.gradle.kts",
        "config/gradlew",
        "config/gradle/wrapper/gradle-wrapper.jar",
        "config/gradle/wrapper/gradle-wrapper.properties",
    ]
    ok, detail = exists_all(root, expected)
    return CheckResult("Build Gradle centralizado reproducible", ok, detail)


def run_checks(root: Path) -> list[CheckResult]:
    checks: list[Callable[[Path], CheckResult]] = [
        check_gradle_root,
        check_android_modules,
        check_core_network,
        check_firmware,
        check_tools,
        check_docs,
        check_python_syntax,
        check_no_stale_active_paths,
        check_no_versioned_apks,
        check_android_manifest_security,
        check_no_active_security_placeholders,
        check_ci_workflow,
    ]
    return [check(root) for check in checks]


def build_report(checks: list[CheckResult]) -> ValidationReport:
    passed = sum(1 for check in checks if check.passed)
    total = len(checks)
    percent = round((passed / total) * 100, 2) if total else 0.0
    status = "PASS" if passed == total else "FAIL"
    return ValidationReport(
        generatedAtUtc=datetime.now(timezone.utc).isoformat(),
        scope="AUTOMATED_SIMULATION_AND_REPOSITORY_STRUCTURE",
        passed=passed,
        total=total,
        percent=percent,
        status=status,
        checks=checks,
        note=(
            "100% aquí significa que las verificaciones automáticas de estructura, "
            "simulación y preparación de CI pasaron. No certifica pruebas con hardware, "
            "seguridad funcional, E-stop físico ni desempeño de laboratorio."
        ),
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Validación 100% automatizable del Sistema CIM en modo simulado")
    parser.add_argument("--output", type=Path, help="Ruta opcional para guardar el reporte JSON")
    parser.add_argument("--quiet", action="store_true", help="Sólo imprime resumen")
    args = parser.parse_args()

    root = repo_root()
    report = build_report(run_checks(root))

    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(asdict(report), indent=2, ensure_ascii=False), encoding="utf-8")

    if args.quiet:
        print(f"{report.status}: {report.passed}/{report.total} ({report.percent:.2f}%)")
    else:
        print("╔════════════════════════════════════════════════════════════╗")
        print("║  VALIDACIÓN 100% AUTOMATIZABLE - CIM v6.0 SIMULADO        ║")
        print("╚════════════════════════════════════════════════════════════╝")
        for check in report.checks:
            mark = "✓" if check.passed else "✗"
            print(f"{mark} {check.name}: {check.detail}")
        print("─" * 60)
        print(f"RESULTADO: {report.passed}/{report.total} verificaciones ({report.percent:.2f}%) - {report.status}")
        print(report.note)

    return 0 if report.status == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
