#!/usr/bin/env python3
"""Inspecciona un checkpoint YOLO .pt sin convertirlo ni usarlo en producción.

Uso:
  python tools/inspect_yolo_checkpoint.py ruta/a/bestMH.pt [imagen_de_prueba]

La conversión a TFLite debe hacerse sólo después de revisar las clases, el
conjunto de validación y las métricas. Un archivo .pt no puede ser cargado
por Android/TensorFlow Lite directamente.
"""
from __future__ import annotations

import argparse
import json
import hashlib
import sys
from datetime import datetime, timezone
from pathlib import Path


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description="Inspecciona clases y metadatos de un checkpoint YOLO")
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("image", type=Path, nargs="?", help="Imagen opcional para una inferencia de humo")
    parser.add_argument("--output", type=Path, default=Path("yolo_checkpoint_report.json"))
    args = parser.parse_args()

    if not args.checkpoint.is_file():
        parser.error(f"Checkpoint no encontrado: {args.checkpoint}")
    if args.image and not args.image.is_file():
        parser.error(f"Imagen no encontrada: {args.image}")

    try:
        from ultralytics import YOLO
    except ImportError:
        print("Falta ultralytics. Instala en un entorno aislado: pip install ultralytics", file=sys.stderr)
        return 2

    model = YOLO(str(args.checkpoint))
    names = model.names
    classes = {str(key): value for key, value in dict(names).items()}
    report: dict[str, object] = {
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "checkpoint": str(args.checkpoint),
        "sizeBytes": args.checkpoint.stat().st_size,
        "sha256": sha256(args.checkpoint),
        "classes": classes,
        "classCount": len(classes),
        "smokeInference": None,
    }

    if args.image:
        results = model(str(args.image), verbose=False)
        result = results[0]
        detections = []
        for box in result.boxes:
            cls = int(box.cls.item())
            detections.append({
                "classId": cls,
                "label": classes.get(str(cls), f"unknown_{cls}"),
                "confidence": round(float(box.conf.item()), 6),
                "xyxy": [round(float(value), 2) for value in box.xyxy[0].tolist()],
            })
        report["smokeInference"] = {
            "image": str(args.image),
            "detectionCount": len(detections),
            "detections": detections,
        }

    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"\nReporte guardado en: {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
