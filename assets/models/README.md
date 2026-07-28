# Modelos de visión

## Modelo canónico

| Archivo | Formato | Uso actual | Estado |
|---|---|---|---|
| `bestMH.pt` | PyTorch / Ultralytics YOLO11s | Inspección y evaluación fuera de Android | Pendiente de validación |

SHA-256 esperado:

```text
1305a2f90f86e8d9a50581d07c87d63efa1d721048d1a0a251a5ed9c5a607733
```

Clases extraídas de metadatos del checkpoint:

```text
caballo_hembra
caballo_macho
craneo_hembra
craneo_macho
hacha_hembra
hacha_macho
lomoToro_hembra
lomoToro_macho
tuerca_macho
```

## Perfiles de visión

```text
PyTorch validation profile
  bestMH.pt
  └─ prueba/evaluación en PC con Ultralytics

Android portable profile
  bestMH_float32_640.tflite
  └─ integración funcional; no es optimización final

Android production candidate
  bestMH_int8_640.tflite
  └─ sólo después de calibración con data.yaml real y métricas aprobadas
```

Los archivos exportados se escriben en `assets/models/generated/` y no se deben añadir a assets Android hasta completar la validación de clases, precisión, recall, falsos positivos y comportamiento con imágenes reales de laboratorio.

## Herramientas

```bash
python -m pip install -r tools/requirements.txt
python tools/inspect_yolo_checkpoint.py assets/models/bestMH.pt imagen.jpg
python tools/export_yolo_to_tflite.py assets/models/bestMH.pt --profile float32
```

Para INT8 se requiere un conjunto de calibración representativo:

```bash
python tools/export_yolo_to_tflite.py assets/models/bestMH.pt \
  --profile int8 \
  --data datasets/cim_parts/data.yaml
```
