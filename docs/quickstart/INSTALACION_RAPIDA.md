# 🚀 INSTALACIÓN RÁPIDA - CIM v6.0

## Prerrequisitos

- **Android Studio** (o Gradle 8.7+)
- **Java 17+**
- **Dispositivos Android** (opcional - funciona en simulación)

---

## ⚡ Instalación en 3 Pasos

### Paso 1: Clonar el repositorio

```bash
git clone https://github.com/haloharry973/CIM-DEFINITIVO.git
cd CIM-DEFINITIVO
```

### Paso 2: Compilar las 5 APKs

```bash
cd config
chmod +x gradlew
./gradlew buildAllApks
```

**Las APKs se generarán en:** `config/output-apks/`

### Paso 3: Instalar en dispositivos (opcional)

```bash
cd ../4_SCRIPTS
powershell -ExecutionPolicy Bypass -File Instalar-APKs.ps1
```

---

## 🎮 Ejecutar en Modo Simulación (Sin Hardware)

### Opción A: Desde el Coordinador

1. Instalar `app-coordinador.apk`
2. Abrir la app
3. Ir a pestaña **EXEC**
4. Pulsar botón **"SIMULAR CICLO"**
5. Ver el ciclo completo ejecutarse

### Opción B: Scripts PowerShell

```powershell
cd 4_SCRIPTS
.\Validar_Sistema_100pc.ps1          # Verificar instalación
.\Simular_Ciclo_Completo.ps1         # Ejecutar simulación
```

---

## 📱 Cada App Funciona Independiente

| App | Cómo activar modo independiente |
|-----|--------------------------------|
| Todas | Pestaña **SINCRO** → Activar **"Modo Autónomo"** |

---

## 🔧 Flashear ESP32 (Opcional)

```powershell
cd 4_SCRIPTS
.\Flashear-ESP32.ps1 -Port COM3
```

---

## 📋 Estructura de Entrega

Todo lo necesario para el profesor está en:

```
PAQUETE_FINAL_ENTREGA/
├── 1_DOCUMENTACION/     ← LEEME.txt + Guía
├── 2_APKS/              ← APKs compiladas
├── 3_FIRMWARE/          ← 4 firmwares ESP32
├── 4_SCRIPTS/           ← Scripts de instalación
└── 5_INFORMES/          ← Informe UBB + Bitácora 240h
```

---

## ✅ Verificación Rápida

```powershell
cd 4_SCRIPTS
.\Validar_Sistema_100pc.ps1
```

Debe mostrar: **"✅ SISTEMA 100% LISTO PARA USO EN ENTORNOS SIMULADOS"**

---

**¡Listo para demostrar en 30 minutos!**
