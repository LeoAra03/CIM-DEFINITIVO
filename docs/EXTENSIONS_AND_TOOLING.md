# Extensiones y Tooling — CIM v6.0

> **Documento consolidado:** Ver sección 8 en [`ENTREGA_FINAL_LEONARDO_ARAYA.md`](ENTREGA_FINAL_LEONARDO_ARAYA.md).

> Guía de entorno de desarrollo para **Practica_2** en Cursor / VS Code (Windows).  
> **Última actualización:** 2026-05-31 — Fase automatización CIM v6.0

---

## 1. Extensiones instaladas (Cursor CLI + fallback VS Code)

Comando principal:

```powershell
& "C:\Program Files\Cursor\resources\app\bin\cursor.cmd" --install-extension <id> --force
```

Fallback si no está en marketplace Cursor:

```powershell
code --install-extension <id> --force
```

### 1.1 Documentación Markdown / PDF / diagramas

| Extensión | ID | Estado Cursor | Propósito en CIM |
|-----------|-----|---------------|------------------|
| **Markdown Preview Enhanced** | `shd101wyy.markdown-preview-enhanced` | ✅ Ya instalada | LaTeX `$...$` / `$$...$$`, Mermaid, export PDF Puppeteer con `docs/styles/industrial_pdf.css` |
| **Markdown All in One** | `yzhang.markdown-all-in-one` | ✅ Ya instalada | TOC, numeración, edición AST en `docs/*.md` |
| **Marp for VS Code** | `marp-team.marp-vscode` | ✅ Ya instalada | Presentaciones defensa / demo docente |
| **Markdown PDF** | `yzane.markdown-pdf` | ✅ Instalada v1.5.0 | Exportación rápida PDF manual industrial |
| **Paste Image** (original) | `mushanshitiancai.paste-image` | ❌ **No encontrada** en marketplace Cursor | — |
| **Markdown Paste Image** (alternativa) | `telesoho.vscode-markdown-paste-image` | ✅ Disponible en Cursor | Capturas → `docs/assets/imagenes/` |
| **Draw.io Integration** | `hediet.vscode-drawio` | ✅ Ya instalada | Diagramas editables en `docs/assets/diagramas/` |

### 1.2 Formateo y comentarios

| Extensión | ID | Estado | Propósito |
|-----------|-----|--------|-----------|
| **Biome** | `biomejs.biome` | ✅ Ya instalada | Lint/format JSON, JS en configs |
| **Prettier** | `esbenp.prettier-vscode` | ✅ Ya instalada | Formateo complementario |
| **Better Comments** | `aaron-bond.better-comments` | ✅ Ya instalada | Anotaciones `//!` `//?` en Kotlin/C++ |
| **Project Manager** | `alefragnani.project-manager` | ✅ Ya instalada | Cambio rápido entre workspaces Practica_2 |

### 1.3 Ortografía técnica español

| Extensión | ID | Estado | Notas |
|-----------|-----|--------|-------|
| **LTeX** | `valentjn.vscode-ltex` | ❌ **No encontrada** en Cursor | Revisión manual + `.cursorrules` tono español |

### 1.4 IoT y Android

| Extensión | ID | Estado | Propósito |
|-----------|-----|--------|-----------|
| **Wokwi for VS Code** | `wokwi.wokwi-vscode` | ✅ Instalada v3.5.0 | Simulación `simulacion_esp32/diagram.json` |
| **PlatformIO IDE** | `platformio.platformio-ide` | ❌ **No encontrada** en Cursor | Usar **PlatformIO Core CLI** (`pip install platformio`) |
| **Android ADB** (solicitada) | `jamesfenn.android-adb` | ❌ **No encontrada** | Usar **`adelphes.android-dev-ext`** (ya instalada) + `entorno_mobile/deploy_multitask.ps1` |

### 1.5 Extensiones Android/Kotlin preexistentes

| Extensión | ID | Estado |
|-----------|-----|--------|
| Kotlin | `fwcd.kotlin` | ✅ |
| Gradle for Java | `vscjava.vscode-gradle` | ✅ |
| Extension Pack for Java | `vscjava.vscode-java-pack` | ✅ |
| Android Dev Ext | `adelphes.android-dev-ext` | ✅ |
| APKLab | `surendrajat.apklab` | ✅ |
| Python | `ms-python.python` | ✅ |
| PowerShell | `ms-vscode.powershell` | ✅ |

---

## 2. Cómo se aplican al proyecto CIM v6.0

| Área | Herramienta | Archivo / acción |
|------|-------------|------------------|
| Reglas documentación | `.cursorrules` | LaTeX, Mermaid, tono industrial español |
| PDF industrial | MPE + `docs/styles/industrial_pdf.css` | A4, márgenes 2.5cm/2cm, numeración |
| Manual seguridad | MPE preview | `docs/manual_industrial_seguridad.md` |
| Bitácora | Markdown All in One TOC | `docs/bitacora_proyectos.md` |
| Diagramas arquitectura | Mermaid + Draw.io + imágenes | `docs/assets/imagenes/` |
| Simulación ESP32 | Wokwi + PIO CLI | `simulacion_esp32/` |
| Deploy multi-emulador | adb + PowerShell jobs | `entorno_mobile/deploy_multitask.ps1` |
| Firmware producción | PlatformIO CLI | `firmware/Firmware_Support/` |

---

## 3. Toolchain del proyecto

| Herramienta | Versión mínima | Uso |
|-------------|----------------|-----|
| JDK | 17 | Gradle, Kotlin |
| Android SDK | API 35 | Compilación APK |
| Gradle | 9.3.1 (wrapper) | `./gradlew` |
| PlatformIO | 6.x (CLI) | `firmware/Firmware_Support`, `simulacion_esp32/` |
| Python 3 | 3.10+ | Simuladores y visión |
| Cursor / VS Code | actual | Extensiones anteriores |

Instalar PlatformIO Core (sin extensión IDE):

```powershell
pip install platformio
pio --version
cd firmware/Firmware_Support
pio run
```

---

## 4. Tareas Gradle clave

```powershell
.\gradlew testAllModules      # 30 tests — core-network + coordinador + plc
.\gradlew buildAllApks        # APKs → output-apks/
.\gradlew buildFirmware       # Wrapper firmware (requiere pio)
```

---

## 5. Resumen instalación 2026-05-31

| Resultado | Extensiones |
|-----------|-------------|
| ✅ OK (12) | MPE, Markdown All in One, Marp, Markdown PDF, Draw.io, Biome, Prettier, Better Comments, Project Manager, Wokwi, + stack Android/Kotlin previo |
| ⚠️ Alternativa (1) | Paste Image → `telesoho.vscode-markdown-paste-image` |
| ❌ Fallo (4) | `mushanshitiancai.paste-image`, `valentjn.vscode-ltex`, `platformio.platformio-ide`, `jamesfenn.android-adb` |

---

## 6. Flujo de trabajo recomendado

1. Abrir workspace `Practica_2` en Cursor (respeta `.cursorrules`).
2. Sincronizar Gradle (panel Gradle → Refresh).
3. Editar Kotlin en `core-network` o apps; ejecutar `.\gradlew testAllModules`.
4. Firmware: `cd firmware/Firmware_Support && pio run`.
5. Simulación Wokwi: abrir carpeta `simulacion_esp32/` → F1 → Wokwi: Start Simulator.
6. Emuladores: `.\entorno_mobile\deploy_multitask.ps1`.
7. Exportar manual: MPE → Export PDF (stylesheet `docs/styles/industrial_pdf.css`).

---

## 7. Referencias

- [Manual industrial seguridad](manual_industrial_seguridad.md)
- [Bitácora proyectos](bitacora_proyectos.md)
- [Guía profesional CIM](GUIA_PROFESIONAL_CIM.md)
- [ESP32 simulación y hardware](ESP32_SIMULACION_Y_HARDWARE.md)
- [Manual arquitectura](manuals/01_ARQUITECTURA_SISTEMA.md)
