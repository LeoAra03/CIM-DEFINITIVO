#!/usr/bin/env python3
"""
YOLO TFLite Inference Test
Ejecuta inferencia sobre imagen y genera JSON con detecciones
"""
import sys
import json
import time
from pathlib import Path
import numpy as np
import cv2

try:
    import tensorflow as tf
    Interpreter = tf.lite.Interpreter
except Exception:
    try:
        from tflite_runtime.interpreter import Interpreter
    except Exception as e:
        raise RuntimeError("Instala 'tensorflow' o 'tflite-runtime': pip install tensorflow") from e

def preprocess(img_path, input_shape):
    """Preprocesar imagen a formato esperado por TFLite"""
    img = cv2.imread(str(img_path))
    if img is None:
        raise FileNotFoundError(f"Imagen no encontrada: {img_path}")

    H, W = input_shape[1], input_shape[2]
    img = cv2.resize(img, (W, H))
    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    # Normalizar a [0,1]
    arr = img_rgb.astype(np.float32) / 255.0
    arr = np.expand_dims(arr, axis=0)

    return arr, img_rgb

def nms(boxes, scores, iou_thres=0.45):
    """Non-Maximum Suppression"""
    idxs = np.argsort(scores)[::-1]
    keep = []

    while len(idxs) > 0:
        keep.append(int(idxs[0]))
        if len(idxs) == 1:
            break

        x1 = np.maximum(boxes[idxs[0], 0], boxes[idxs[1:], 0])
        y1 = np.maximum(boxes[idxs[0], 1], boxes[idxs[1:], 1])
        x2 = np.minimum(boxes[idxs[0], 2], boxes[idxs[1:], 2])
        y2 = np.minimum(boxes[idxs[0], 3], boxes[idxs[1:], 3])

        inter = np.maximum(0, x2 - x1) * np.maximum(0, y2 - y1)
        area1 = (boxes[idxs[0], 2] - boxes[idxs[0], 0]) * (boxes[idxs[0], 3] - boxes[idxs[0], 1])
        area2 = (boxes[idxs[1:], 2] - boxes[idxs[1:], 0]) * (boxes[idxs[1:], 3] - boxes[idxs[1:], 1])

        iou = inter / (area1 + area2 - inter + 1e-6)
        idxs = idxs[1:][iou <= iou_thres]

    return keep

def run_inference(model_path, image_path):
    """Ejecutar inferencia y retornar detecciones"""
    model_path = Path(model_path)
    image_path = Path(image_path)

    if not model_path.exists():
        raise FileNotFoundError(f"Modelo no encontrado: {model_path}")
    if not image_path.exists():
        raise FileNotFoundError(f"Imagen no encontrada: {image_path}")

    # Cargar intérprete
    interpreter = Interpreter(model_path=str(model_path))
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    input_shape = input_details[0]['shape']
    input_dtype = input_details[0]['dtype']

    # Preprocesar imagen
    input_data, _ = preprocess(image_path, input_shape)

    # Convertir a dtype esperado
    if input_dtype == np.uint8:
        input_data = (input_data * 255).astype(np.uint8)
    else:
        input_data = input_data.astype(np.float32)

    # Ejecutar inferencia
    interpreter.set_tensor(input_details[0]['index'], input_data)

    t0 = time.time()
    interpreter.invoke()
    inference_ms = (time.time() - t0) * 1000

    # Obtener outputs
    outputs = {}
    for out_detail in output_details:
        tensor = interpreter.get_tensor(out_detail['index'])
        name = out_detail.get('name', str(out_detail['index']))
        outputs[name] = tensor

    # Procesar salida (asumir formato YOLO típico)
    output_tensor = next(iter(outputs.values()))
    arr = output_tensor.squeeze()

    if arr.ndim == 1:
        arr = np.expand_dims(arr, axis=0)

    # Parsear detecciones
    detections = []
    for row in arr:
        if len(row) >= 5:
            conf = float(row[4])
            if conf > 0.2:  # Threshold
                detection = {
                    "x": float(row[0]),
                    "y": float(row[1]),
                    "w": float(row[2]),
                    "h": float(row[3]),
                    "confidence": conf,
                    "class": int(row[5]) if len(row) > 5 else 0
                }
                detections.append(detection)

    # Aplicar NMS
    if len(detections) > 0:
        boxes = np.array([[d["x"]-d["w"]/2, d["y"]-d["h"]/2, d["x"]+d["w"]/2, d["y"]+d["h"]/2] for d in detections])
        scores = np.array([d["confidence"] for d in detections])
        keep_idxs = nms(boxes, scores)
        detections = [detections[i] for i in keep_idxs]

    result = {
        "model": str(model_path),
        "image": str(image_path),
        "inference_ms": round(inference_ms, 2),
        "input_shape": list(input_shape),
        "num_detections": len(detections),
        "detections": detections,
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
    }

    return result

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Uso: python tflite_yolo_test.py path/to/model.tflite path/to/image.jpg")
        print("\nEjemplo:")
        print("  python tflite_yolo_test.py model.tflite test.jpg")
        sys.exit(1)

    model_path = sys.argv[1]
    image_path = sys.argv[2]

    try:
        print(f"Cargando modelo: {model_path}")
        print(f"Procesando imagen: {image_path}")
        result = run_inference(model_path, image_path)

        # Guardar resultado
        output_path = Path("tflite_inference_result.json")
        output_path.write_text(json.dumps(result, indent=2))

        print("\n" + "="*60)
        print(f"✓ Inferencia completada en {result['inference_ms']} ms")
        print(f"✓ Detecciones: {result['num_detections']}")
        print(f"✓ Resultado guardado: {output_path}")
        print("="*60)
        print(json.dumps(result, indent=2))

    except Exception as e:
        print(f"\n✗ ERROR: {e}", file=sys.stderr)
        sys.exit(1)

