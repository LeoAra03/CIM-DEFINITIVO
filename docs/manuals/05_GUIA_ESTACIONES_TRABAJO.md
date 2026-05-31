# Guía de Estaciones de Trabajo

> **Documento consolidado:** Ver sección 4 en [`../ENTREGA_FINAL_LEONARDO_ARAYA.md`](../ENTREGA_FINAL_LEONARDO_ARAYA.md).

Cada aplicación del sistema CIM está especializada en un subsistema físico. Esta guía explica el panel de control de cada una.

---

## 1. CIM Hub (Coordinador)
Es el panel maestro. Permite enviar scripts de automatización.
- **Pestaña Nodos:** Monitorea quién está ONLINE y permite dar permisos.
- **Pestaña ArUco:** Muestra el mapa de visión global.
- **Modo AUTO:** Si está activo, el hub autoriza automáticamente cualquier nodo que conozca la contraseña.

## 2. PLC Master
Controla el transporte físico entre estaciones.
- **Control Cinta:** Botones de arranque y parada.
- **Matriz de Distribución:** Permite ordenar entregas específicas (ej: Estación 1 -> Estación 5).
- **Simulador:** Incluye un disparador de sensores para probar la lógica sin hardware real.

## 3. Logística Pro (Almacén)
Gestiona el rack de almacenamiento de 18 posiciones (3 niveles x 6 columnas).
- **Pestaña Posiciones:** Visualización del estado del rack.
- **Acción:** Seleccionar una celda y pulsar para que el hardware almacene/recupere la pieza.

## 4. Quality Pro (Calidad)
Control de validación mediante visión YOLO/ArUco.
- **Validación:** Feed de cámara con detección en tiempo real.
- **Contadores:** Mantiene estadísticas de piezas aprobadas vs rechazadas.

## 5. Manufactura Pro
La estación más compleja, controla el Scorbot y el Grabado Láser.
- **Robot:** Interfaz de "Jogging" (X, Y) y posiciones predefinidas (HOME, READY).
- **Láser:** Configuración de potencia y velocidad de grabado.
- **G-Code:** Recibe archivos `.gcode` desde el Coordinador para ejecuciones personalizadas.
