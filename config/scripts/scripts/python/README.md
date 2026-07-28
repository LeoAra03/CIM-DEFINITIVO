# 🏭 Sistema de Manufactura Integrada por Computador (CIM) - Backend y Herramientas

Este repositorio contiene el backend de control, los scripts de simulación y las herramientas de soporte para el sistema CIM. El objetivo principal de este conjunto de herramientas es proporcionar una base de control robusta en Python para la orquestación de hardware industrial, incluyendo robots Scorbot, un PLC para cinta transportadora y un sistema de visión artificial.

## 🏛️ Arquitectura del Software (Componentes Python)

El ecosistema Python está compuesto por varias aplicaciones y scripts independientes que trabajan en conjunto:

- **`integrated_panel.py`**: Es el panel de control maestro y la aplicación más completa. Integra la gestión de la cinta transportadora (PLC), el control del robot de manufactura, el sistema de visión (YOLO y ArUco) y la gestión de red en una única interfaz gráfica (Tkinter). **Es el punto de partida recomendado para operar el sistema completo.**

- **`servidor.py`**: Un servidor TCP/IP que actúa como *Coordinador de Red*. Las diferentes estaciones (representadas por otras instancias de `integrated_panel.py` o las futuras APKs de Android) se conectan a él para sincronizar operaciones.

- **`cinta.py`**: Una herramienta de utilidad para controlar y depurar la comunicación serial con el PLC de la cinta transportadora de forma aislada.

- **`usuario.py`**: Una interfaz de cliente TCP avanzada que también integra control serial para el robot Scorbot y el láser, diseñada para actuar como una estación de trabajo remota.

- **`aruco_generador.py`**: Una utilidad gráfica para generar y guardar marcadores ArUco, que son utilizados por el sistema de calidad.

- **`esp32_master_controller.py`**: Un script de línea de comandos para enviar comandos directos a los microcontroladores ESP32 a través de TCP.

## 🚀 Puesta en Marcha (Setup)

Sigue estos pasos para configurar y ejecutar el entorno de control en tu máquina de desarrollo.

### 1. Prerrequisitos

- Python 3.8 o superior.
- `pip` (el gestor de paquetes de Python).
- Git.

### 2. Clonar el Repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd <NOMBRE_DEL_REPOSITORIO>
```

### 3. Instalar Dependencias

Se ha creado un fichero `requirements.txt` para estandarizar la instalación. Ejecuta el siguiente comando en la carpeta `scripts`:

```bash
# Navega a la carpeta de scripts
cd scripts

# Instala todas las dependencias
pip install -r requirements.txt
```

### 4. Ejecución del Sistema

El orden de ejecución recomendado para una simulación completa es:

1.  **Iniciar el Servidor Coordinador:**
    ```bash
    # Desde la carpeta esp32/scripts/coordinador/esp32_scripts/
    python servidor.py
    ```
    Haz clic en "Iniciar" en la interfaz del servidor.

2.  **Iniciar el Panel de Control Integrado:**
    ```bash
    # Desde la carpeta esp32/scripts/coordinador/esp32_scripts/
    python integrated_panel.py
    ```
    Utiliza las diferentes pestañas para controlar los subsistemas. Conéctate a los puertos COM correspondientes para el hardware físico.

## 📄 Documentación Adicional

- **Protocolos de Comunicación**: Consulta `docs/PROTOCOLO_COMUNICACION.md` para ver los detalles de las tramas de datos TCP y los comandos seriales.
- **Bitácora de Cambios**: Consulta `BITACORA_DE_CAMBIOS.md` para un registro de las mejoras y refactorizaciones realizadas.
- **Arquitectura Android**: La documentación para la migración a microservicios Android se encuentra en `docs/project/DOCUMENTACION_SISTEMA_CIM.md`.

---
*Este proyecto es parte de un sistema de manufactura flexible desarrollado con fines académicos y de demostración.*