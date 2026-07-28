# YOLO TFLite model placeholder

Place a quantized YOLOv8-tiny model here named:

`yolov8n-int8.tflite`

Export example (Python):

```python
from ultralytics import YOLO
model = YOLO('yolov8n.pt')
model.export(format='tflite', int8=True)
```

Without this file, Calidad/Manufactura use OpenCV contour fallback automatically.
