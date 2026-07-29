# Modelo YOLO TFLite

Esta carpeta puede contener el modelo TFLite aprobado para Android. Nombre esperado por la integración:

```text
yolov8n-int8.tflite
```

Flujo recomendado:

1. Inspeccionar el checkpoint fuente:

```bash
python3 tools/inspect_yolo_checkpoint.py assets/models/bestMH.pt
```

2. Exportar de forma trazable:

```bash
python3 tools/export_yolo_to_tflite.py assets/models/bestMH.pt --profile int8 --data datasets/cim_parts/data.yaml
```

3. Registrar hashes, clases y métricas antes de copiar el `.tflite` a assets Android.

Si el archivo no existe, Calidad/Manufactura deben operar con el fallback de visión y no habilitar decisiones automáticas de baja confianza.
