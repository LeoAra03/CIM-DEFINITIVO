# Sistema de Visión Artificial Industrial

> **Documento consolidado:** Ver sección 7 en [`../ENTREGA_FINAL_LEONARDO_ARAYA.md`](../ENTREGA_FINAL_LEONARDO_ARAYA.md).

El módulo de visión utiliza procesamiento de imágenes en el dispositivo para la localización de pallets y la identificación automática de piezas.

---

## 1. Tecnologías Integradas

- **CameraX (Jetpack):** Control del ciclo de vida de la cámara y abstracción de hardware.
- **OpenCV Android SDK 4.9.0:** Detección de marcadores ArUco y análisis geométrico.
- **Google ML Kit:** Escaneo de códigos QR de alta velocidad y precisión.

---

## 2. Analizador de Frames (IndustrialVisionAnalyzer)

El procesamiento no ocurre sobre fotos guardadas, sino directamente sobre el flujo de video en memoria (`ImageProxy`).

### Detección ArUco
Utilizamos el diccionario `DICT_4X4_50` (50 marcadores de 4x4 bits).
- **Entrada:** Matriz de grises convertida desde YUV.
- **Salida:** ID del marcador y coordenadas (X, Y) del centro.
- **Frecuencia:** Limitada a 5 FPS para balancear precisión y consumo térmico.

### Detección QR
Orientada a la configuración rápida.
- El QR puede contener instrucciones de red (ej: `BT_CONNECT:MAC_ADDR`).
- Permite vincular estaciones al hub simplemente apuntando la cámara a la etiqueta del equipo.

---

## 3. Uso en la Interfaz (Compose)

El componente `CameraPreviewWithVision` provee un "visor industrial" que:
1. Muestra el feed de la cámara.
2. Dibuja recuadros verdes sobre los ArUcos detectados.
3. Notifica al sistema mediante callbacks cuando se encuentra un objetivo válido.

---

## 4. Casos de Uso Industriales

1. **Tracking de Pallets:** Cada pallet tiene un marcador ArUco. El Coordinador sabe exactamente dónde está cada unidad en la planta.
2. **Validación de Calidad:** La app de Calidad dispara una foto cuando el ArUco entra en el área de inspección.
3. **Seguridad:** Si un marcador de "Zona de Peligro" es detectado, se envía un comando `ABORT` inmediato al robot.
