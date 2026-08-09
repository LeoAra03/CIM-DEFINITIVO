# CIM Visual Preview - Sistema Multiconectado Intuitivo

Este directorio contiene un simulador visual web del sistema CIM que replica las 5 APKs Android + Wear + Hub Coordinador.

## 🚀 Cómo usar (1-click)

1. Abre `index.html` en tu navegador o usa el servidor preview ya corriendo en `:8000`
2. Click **▶ Iniciar Hub :8888**
3. Click **🔗 Conectar Todo (1-click)** - conecta 5 estaciones simultáneamente
4. Si AUTO MODE está OFF, autoriza cada estación en el diálogo que aparece
5. Si AUTO MODE está ON, se autoriza automáticamente (solo laboratorio)
6. Ya puedes enviar comandos: Cinta, Robot, Laser, Visión, Almacén

## 📱 Vistas por APK

Cada APK tiene su índice visual independiente que imita el diseño Compose original:

- **Coordinador**: `apps/coordinador.html` - Hub con tabs EXEC, CINTA, ROBOT, ARUCO, MAPA, NODOS, RACKS
- **PLC Cinta**: `apps/plc.html` - Matriz 3x10 DELIVER, tracking pallet
- **Manufactura**: `apps/manufactura.html` - Control Scorbot + Laser + SINCRO
- **Calidad**: `apps/calidad.html` - Camera + ArUco + YOLO TFLite
- **Almacén**: `apps/almacen.html` - Rack 3x3 + Scorbot storage
- **Wear**: `apps/wear.html` - Supervisión compacta redonda

## 🎨 Diseño Industrial

Colores idénticos a las apps reales:
- Fondo #080808, Cards #1E1E1E, Borde #2A2A2A
- Primario #FF6D00 (naranja industrial), Success #4CAF50, Error #B00020
- Terminal monoespaciada JetBrains Mono con logs en tiempo real
- Visual wiring SVG con líneas animadas Hub ↔ Estaciones

## 🔧 Fixes de seguridad aplicados

Este visual demuestra los fixes críticos ya aplicados en código real:

- ✓ TLS sin trust-all (usa system CAs)
- ✓ PLC relay con auth requerida + watchdog 10s
- ✓ G-code sanitizado (no path traversal)
- ✓ Auto-approve solo DEBUG
- ✓ Token validación SHA-256

## 📂 Servidor

```bash
cd web-preview
python3 -m http.server 8000 --bind 0.0.0.0
# Abre https://8000-XXXX.e2b.app
```

## 🔗 Flujo visual

```
[Hub :8888] -- NSD _cim-hub._tcp. -->
   |-> PLC (Cinta) -- C:DELIVER:1:3 -->
   |-> Manufactura (Robot) -- R:RUN ARU1 -->
   |-> Calidad (Vision) -- ARUCO:DETECT -->
   |-> Almacén (Rack) -- STO:STORE -->
   └-> Wear (Monitor)
```

Todos los comandos pasan por validación de autorización y se loggean en terminal central.
