#!/usr/bin/env python3
"""Exporta un checkpoint YOLO a TFLite de forma trazable.

No usa la salida para producción automáticamente. Genera un manifiesto con hash,
clases y parámetros para que la APK sólo consuma un modelo revisado.

Ejemplos:
  # Perfil portátil para probar la integración Android
  python tools/export_yolo_to_tflite.py assets/models/bestMH.pt --profile float32

  # INT8 requiere dataset YAML real para calibración representativa
  python tools/export_yolo_to_tflite.py assets/models/bestMH.pt --profile int8 \
      --data datasets/cim_parts/data.yaml
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from datetime import datetime, timezone
from pathlib import Path


def file_hash(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description="Exportación trazable YOLO → TensorFlow Lite")
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--profile", choices=("float32", "int8"), default="float32")
    parser.add_argument("--data", type=Path, help="data.yaml requerido para calibración INT8")
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--output-dir", type=Path, default=Path("assets/models/generated"))
    args = parser.parse_args()

    if not args.checkpoint.is_file():
        parser.error(f"Checkpoint no encontrado: {args.checkpoint}")
    if args.profile == "int8" and (not args.data or not args.data.is_file()):
        parser.error("El perfil INT8 exige --data con un data.yaml de calibración real")

    try:
        from ultralytics import YOLO
    except ImportError:
        print("Falta ultralytics. Instala: python -m pip install ultralytics", file=sys.stderr)
        return 2

    model = YOLO(str(args.checkpoint))
    output = model.export(
        format="tflite",
        imgsz=args.imgsz,
        int8=args.profile == "int8",
        data=str(args.data) if args.data else None,
    )
    output_path = Path(output)
    if not output_path.is_file():
        raise RuntimeError(f"Ultralytics no produjo TFLite: {output_path}")

    args.output_dir.mkdir(parents=True, exist_ok=True)
    canonical_name = f"bestMH_{args.profile}_{args.imgsz}.tflite"
    target = args.output_dir / canonical_name
    shutil.copy2(output_path, target)

    names = {str(index): label for index, label in dict(model.names).items()}
    manifest = {
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "sourceCheckpoint": str(args.checkpoint),
        "sourceSha256": file_hash(args.checkpoint),
        "outputModel": str(target),
        "outputSha256": file_hash(target),
        "profile": args.profile,
        "imageSize": args.imgsz,
        "classes": names,
        "classCount": len(names),
        "calibrationDataset": str(args.data) if args.data else None,
        "approval": "PENDING_VALIDATION",
        "warning": "No copiar a Android assets ni habilitar decisiones automáticas sin métricas y aprobación humana."
    }
    manifest_path = target.with_suffix(".manifest.json")
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(manifest, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
