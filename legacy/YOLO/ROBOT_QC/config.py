#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ROBOT QC - CONFIGURACIÓN Y CONSTANTES
======================================

Archivo centralizado con toda la configuración del sistema ROBOT QC.

"""

import os

# ===== CONFIGURACIÓN SERIAL =====
ROBOT_PORT = 'COM6'
ROBOT_BAUDRATE = 9600
ROBOT_TIMEOUT = 2.0
ROBOT_BYTESIZE = 8
ROBOT_PARITY = 'N'
ROBOT_STOPBITS = 1

# ===== CONFIGURACIÓN DE CÁMARA =====
DEFAULT_CAMERA_INDEX = 0
CAMERA_RESOLUTION = (1280, 720)
CAMERA_FPS = 30
CAMERA_BUFFER_SIZE = 1      # Tamaño mínimo del buffer (frames frescos)
CAMERA_SKIP_FRAMES = 2      # Procesar YOLO cada N frames (1=todos, 2=cada otro, etc.)
CAMERA_YOLO_INPUT_SIZE = (640, 480)  # Tamaño de entrada para YOLO
CAMERA_DISPLAY_WIDTH = 900  # Ancho máximo de display
CAMERA_DISPLAY_HEIGHT = 500 # Alto máximo de display

# ===== CONFIGURACIÓN YOLO =====
YOLO_MODEL_PATH = os.path.join(os.path.dirname(__file__), '..', 'bestMH.pt')
YOLO_CONFIDENCE_THRESHOLD = 0.5
YOLO_IOU_THRESHOLD = 0.45

# ===== PROGRAMAS DEL ROBOT =====
ROBOT_PROGRAMS = {
    'SR1': '🔵 SR1 - Programa 1 (Posicionar)',
    'SR2': '🟢 SR2 - Programa 2 (Traer)',
    'SR3': '🟡 SR3 - Programa 3 (Ensamblar)',
    'SR4': '🔴 SR4 - Programa 4 (Finalizar)',
}

# ===== COMANDOS DEL ROBOT =====
ROBOT_COMMANDS = {
    'READY': 'Verificar si el robot está listo',
    'COFF': 'Apagar servomotores',
    'HOME': 'Ir a posición de inicio',
    'ABORT': 'Abortar movimiento actual',
    'OPEN': 'Abrir gripper',
    'CLOSE': 'Cerrar gripper',
}

# ===== PAREJAS MACHO-HEMBRA =====
PIECE_PAIRS = {
    'hacha_macho': 'hacha_hembra',
    'lomotoro_macho': 'lomotoro_hembra',
    'caballo_macho': 'caballo_hembra',
    'craneo_macho': 'craneo_hembra'
}

# ===== SECUENCIA AUTOMÁTICA =====
# Tiempos en segundos
SEQUENCE_STEP1_TIMEOUT = 150  # SR1
SEQUENCE_WAIT_AFTER_SR1 = 2.0  # Esperar antes de detectar
SEQUENCE_DETECT_FIRST_PIECE_TIMEOUT = 6.0
SEQUENCE_WAIT_BEFORE_SR2 = 9  # Esperar antes de SR2
SEQUENCE_STEP2_TIMEOUT = 150  # SR2
SEQUENCE_WAIT_FOR_CORRESPONDING_PIECE_TIMEOUT = 60.0
SEQUENCE_WAIT_BEFORE_SR3 = 15  # Esperar antes de SR3
SEQUENCE_STEP3_TIMEOUT = 150  # SR3 (Ensamblaje)
SEQUENCE_STEP4_TIMEOUT = 150  # SR4 (Finalización)
SEQUENCE_STEP2_FINAL_TIMEOUT = 150  # SR2 final (Retorno)

# ===== INTERFAZ GRÁFICA =====
WINDOW_WIDTH = 1400
WINDOW_HEIGHT = 900
WINDOW_MIN_WIDTH = 1000
WINDOW_MIN_HEIGHT = 700

# ===== FUENTES =====
FONT_TITLE = ('Arial', 16, 'bold')
FONT_SUBTITLE = ('Arial', 11, 'bold')
FONT_NORMAL = ('Arial', 10)
FONT_SMALL = ('Arial', 9)
FONT_MONO = ('Consolas', 9)

# ===== COLORES =====
COLOR_CONNECTED = 'green'
COLOR_DISCONNECTED = 'red'
COLOR_WARNING = 'orange'
COLOR_SUCCESS = 'green'
COLOR_ERROR = 'red'

# ===== LÍMITES DE COLA =====
ROBOT_COMMAND_QUEUE_MAX_SIZE = 100

# ===== TIMEOUTS GENERALES =====
SERIAL_READ_TIMEOUT = 0.5
SERIAL_WRITE_TIMEOUT = 1.0
DEFAULT_TIMEOUT = 30.0

# ===== RUTAS DE ARCHIVOS =====
def get_model_path() -> str:
    """Obtiene la ruta del modelo YOLO."""
    if os.path.exists(YOLO_MODEL_PATH):
        return YOLO_MODEL_PATH
    # Fallback: buscar en el directorio padre
    parent_dir = os.path.dirname(os.path.dirname(__file__))
    fallback = os.path.join(parent_dir, 'bestMH.pt')
    if os.path.exists(fallback):
        return fallback
    return 'bestMH.pt'

# ===== MENSAJES DE LOG =====
MESSAGES = {
    'ROBOT_CONNECTED': '✓ Robot conectado en COM6',
    'ROBOT_DISCONNECTED': '✓ Robot desconectado',
    'CAMERA_STARTED': '📷 Cámara iniciada',
    'CAMERA_STOPPED': '📷 Cámara detenida',
    'SEQUENCE_STARTED': '▶ Secuencia automática iniciada',
    'SEQUENCE_COMPLETED': '✓✓✓ Secuencia completada exitosamente ✓✓✓',
    'SEQUENCE_ERROR': '✗ Error en secuencia automática',
    'PIECE_DETECTED': '✓ Pieza detectada: {}',
    'PIECE_NOT_FOUND': '✗ Pieza no encontrada: {}',
}
