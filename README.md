# CIM - Sistema de Control Industrial Inteligente

Proyecto completamente **minimalista** y **organizado por funcionalidad**.

## 📁 Estructura Principal

Solo **5 carpetas principales**:

```
📦 Practica_2/
├── 📱 android/          ← Apps Android + core-network
├── 🔧 esp32/            ← Firmware + scripts ESP32
├── 📚 docs/             ← Documentación del proyecto
├── 📋 logs/             ← Logs de sistema y apps
├── ⚙️ config/            ← Configuración (Gradle, scripts, etc.)
├── 🗂️ legacy/            ← Archivos antiguos (no usar)
├── 🔨 build/            ← Outputs de compilación
└── README.md
```

## ⚡ Acceso Rápido

| Necesito... | Voy a... |
|---|---|
| Instalar una app | `android/apks/` |
| Cargar firmware ESP32 | `esp32/firmware/` |
| Leer documentación | `docs/project/` |
| Ver logs | `logs/` |
| Configurar o buildear | `config/` |

## 🏗️ Contenido Específico

### **android/**
- `apps/` - Todos los módulos Android del proyecto
- `apks/` - APKs compiladas listas para instalar
- `core-network/` - Librería compartida de comunicación

### **esp32/**
- `firmware/` - Código del microcontrolador
  - `Firmware_Support/` - Herramientas de soporte
  - `v7_standard/` - Versión estándar
- `scripts/` - Scripts Python y auxiliares

### **docs/**
- `project/` - Documentación oficial del proyecto
- `quickstart/` - Guías de inicio rápido

### **config/**
- `settings.gradle.kts` - Configuración de módulos
- `build.gradle.kts` - Configuración de build
- `gradle-wrapper/` - Gradle portable
- `scripts/` - Scripts de instalación y deploy

## 🚀 Para Empezar

1. **Compilar apps Android:**
   ```powershell
   cd config
   .\gradle-wrapper\gradlew.bat :app-coordinador:app:assembleDebug
   ```

2. **Flashear ESP32:**
   ```
   Abre PlatformIO → esp32/firmware/ → upload
   ```

3. **Instalar APKs:**
   ```
   Ve a: android/apks/ → copia los .apk a tu teléfono
   ```

## ✅ Todo está donde debe estar
- ✓ Apps Android consolidadas en una carpeta
- ✓ Firmware ESP32 en su propio lugar
- ✓ Documentación centralizada
- ✓ Sin clutter de configuraciones sueltas
- ✓ Archivos antiguos en legacy/ (ocultos)
