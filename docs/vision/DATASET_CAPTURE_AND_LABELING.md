# Captura rápida y etiquetado de secuencias para visión de Calidad

## Estado actual

La aplicación detecta ArUco, QR y las clases que el modelo TFLite entregue. El modelo actual declara únicamente `pieza`, `pallet`, `defecto` y `aruco_obj`; por tanto **no está entrenado para distinguir macho y hembra**. Renombrar etiquetas en código no crea esa capacidad: se necesita evidencia etiquetada y un modelo entrenado con ella.

## Taxonomía inicial propuesta

Usar exactamente estas etiquetas durante la captura para evitar conjuntos incompatibles:

```text
macho
hembra
ensamble_correcto
ensamble_incorrecto
defecto_superficial
pieza_ausente
ocluida
no_clasificable
```

La clase `no_clasificable` es obligatoria: evita forzar al modelo a clasificar una imagen borrosa, con reflejo, fuera de foco o con objeto parcialmente cubierto.

## Identidad y trazabilidad

Cada secuencia debe tener un identificador único y un ArUco/lote asociado:

```text
sequenceId: 20260728T183000Z_PAL-001
palletId: PAL-001
productId: PRODUCTO-XYZ
arucoId: 42
operatorId: operador-01
stationUuid: CIM-ST-CAL-X3
```

Cada frame se registra en un manifiesto JSONL con:

```json
{
  "frameId": "20260728T183000Z_PAL-001_000123",
  "sequenceId": "20260728T183000Z_PAL-001",
  "file": "frames/000123.jpg",
  "capturedAtUtc": "2026-07-28T18:30:05.123Z",
  "palletId": "PAL-001",
  "productId": "PRODUCTO-XYZ",
  "arucoId": 42,
  "label": "macho",
  "operatorId": "operador-01",
  "stationUuid": "CIM-ST-CAL-X3",
  "cameraProfile": "CAM-01_1920x1080",
  "reviewStatus": "PENDING"
}
```

Nunca usar sólo el nombre del archivo como fuente de verdad de la etiqueta.

## Flujo para operador sin experiencia en YOLO

1. Seleccionar `ID de pallet`, `ID de producto` y etiqueta inicial en Calidad.
2. Confirmar el ArUco visible; la app asocia el ID leído a la secuencia.
3. Pulsar **Iniciar captura**. La app toma frames a una cadencia controlada, no todos los frames de cámara.
4. Mover o girar la pieza lentamente para cubrir frente, reverso, orientación, sombras y distancia.
5. Pulsar **Finalizar captura**.
6. Revisar la secuencia: aprobar, corregir etiqueta o marcar `no_clasificable`.
7. Exportar únicamente secuencias aprobadas para entrenamiento.

## Cantidad mínima orientativa

No entrenar con pocas imágenes. Como punto de partida de prototipo:

| Clase | Mínimo inicial | Objetivo recomendado |
|---|---:|---:|
| macho | 300 imágenes variadas | 2.000+ |
| hembra | 300 imágenes variadas | 2.000+ |
| ensamble correcto | 200 | 1.000+ |
| ensamble incorrecto | 200 | 1.000+ |
| defecto superficial | 200 | 1.000+ |
| no clasificable | 150 | 800+ |

Cada conjunto debe incluir variación de iluminación, orientación, distancia, fondo, lote y condición de cámara. No deben existir frames casi idénticos de una misma secuencia repartidos entre entrenamiento y prueba.

## División obligatoria

Separar por secuencia/pallet, nunca por frame individual:

```text
70% entrenamiento
15% validación
15% prueba final bloqueada
```

Una secuencia completa sólo puede estar en una partición. Esto evita que el modelo parezca preciso porque vio casi la misma pieza durante el entrenamiento.

## Criterios antes de usar una decisión automática

- Métricas por clase: precision, recall, F1 y matriz de confusión.
- Revisar falsos positivos macho/hembra y falsos negativos de defecto.
- Definir umbral de confianza por clase.
- Confianza baja debe dar `REVIEW_REQUIRED`, nunca una orden automática de robot.
- La primera liberación debe ser modo asistido: el operador confirma el resultado.

## Seguridad del flujo macho/hembra

La visión sólo puede recomendar la pareja. La acción de juntar piezas requiere además:

```text
ArUco correcto
posición/orientación confirmada
pinza/herramienta disponible
zona libre
sin E-stop
robot en HOME o posición conocida
confirmación de agarre y liberación
```

No permitir que un resultado YOLO aislado mueva robot, cinta o láser.
