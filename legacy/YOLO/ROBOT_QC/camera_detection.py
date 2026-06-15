#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CAMERA DETECTION MODULE - Módulo de detección con cámara y YOLO
================================================================

Este módulo contiene toda la lógica para:
- Captura de video de la cámara
- Detección de objetos con YOLO
- Procesamiento de frames
- Identificación de piezas (macho/hembra)

"""

import cv2
import numpy as np
import threading
import time
from typing import Optional, List, Tuple
from ultralytics import YOLO


class CameraDetectionModule:
    """Módulo de detección de objetos con cámara y YOLO."""
    
    def __init__(self, yolo_model: Optional[YOLO] = None, camera_index: int = 0):
        """
        Inicializa el módulo de detección de cámara.
        
        Args:
            yolo_model: Modelo YOLO cargado (puede ser None)
            camera_index: Índice de la cámara (0 o 1)
        """
        self.yolo_model = yolo_model
        self.camera_index = camera_index
        self.cap: Optional[cv2.VideoCapture] = None
        self.current_frame: Optional[np.ndarray] = None
        self.annotated_frame: Optional[np.ndarray] = None
        self.detections: List[dict] = []
        self._lock = threading.Lock()
    
    def set_camera_index(self, index: int):
        """
        Cambia el índice de la cámara.
        
        Args:
            index: Índice de la cámara (0 o 1)
        """
        self.camera_index = index
        if self.cap is not None:
            self.cap.release()
            self.cap = None
    
    def open_camera(self) -> bool:
        """
        Abre la cámara.
        
        Returns:
            True si se abrió exitosamente
        """
        try:
            self.cap = cv2.VideoCapture(self.camera_index)
            return self.cap.isOpened()
        except Exception as e:
            print(f"✗ Error abriendo cámara {self.camera_index}: {e}")
            return False
    
    def release_camera(self):
        """Libera la cámara."""
        try:
            if self.cap is not None:
                self.cap.release()
                self.cap = None
        except Exception:
            pass
    
    def capture_frame(self) -> Optional[np.ndarray]:
        """
        Captura un frame de la cámara.
        
        Returns:
            Frame capturado o None si hay error
        """
        try:
            with self._lock:
                if self.cap is None or not self.cap.isOpened():
                    return None
                
                ret, frame = self.cap.read()
                if ret:
                    self.current_frame = frame
                    return frame
                else:
                    return None
        except Exception as e:
            print(f"✗ Error capturando frame: {e}")
            return None
    
    def detect_objects(self, frame: np.ndarray) -> Tuple[List[dict], np.ndarray]:
        """
        Detecta objetos en un frame usando YOLO.
        
        Args:
            frame: Frame de entrada
        
        Returns:
            Tupla (lista de detecciones, frame anotado)
        """
        try:
            if self.yolo_model is None:
                return [], frame
            
            # Ejecutar YOLO
            results = self.yolo_model(frame, verbose=False)
            
            # Anotar frame
            annotated = results[0].plot()
            
            # Extraer detecciones
            detections = []
            if results[0].boxes is not None:
                for box in results[0].boxes:
                    detection = {
                        'x1': float(box.xyxy[0][0]),
                        'y1': float(box.xyxy[0][1]),
                        'x2': float(box.xyxy[0][2]),
                        'y2': float(box.xyxy[0][3]),
                        'confidence': float(box.conf[0]),
                        'class': int(box.cls[0]),
                        'class_name': results[0].names[int(box.cls[0])]
                    }
                    detections.append(detection)
            
            with self._lock:
                self.detections = detections
                self.annotated_frame = annotated
            
            return detections, annotated
        except Exception as e:
            print(f"✗ Error en detección: {e}")
            return [], frame
    
    def get_detected_classes(self) -> List[str]:
        """
        Retorna las clases detectadas en el último frame.
        
        Returns:
            Lista de nombres de clases detectadas
        """
        with self._lock:
            return [d['class_name'] for d in self.detections]
    
    def get_highest_confidence_detection(self) -> Optional[dict]:
        """
        Retorna la detección con mayor confianza.
        
        Returns:
            Diccionario con la detección o None
        """
        with self._lock:
            if not self.detections:
                return None
            return max(self.detections, key=lambda d: d['confidence'])
    
    def get_detected_piece_type(self) -> Optional[str]:
        """
        Retorna el tipo de pieza detectada (macho/hembra o la más confiable).
        
        Returns:
            Nombre de la clase o None si no hay detecciones
        """
        detection = self.get_highest_confidence_detection()
        if detection:
            return detection['class_name']
        return None
    
    def get_current_frame(self) -> Optional[np.ndarray]:
        """
        Retorna el frame actual.
        
        Returns:
            Frame o None
        """
        with self._lock:
            return self.current_frame
    
    def get_annotated_frame(self) -> Optional[np.ndarray]:
        """
        Retorna el frame anotado con detecciones.
        
        Returns:
            Frame anotado o None
        """
        with self._lock:
            return self.annotated_frame
    
    def detect_specific_class(self, class_name: str, timeout: float = 10.0) -> bool:
        """
        Espera a detectar una clase específica dentro de un timeout.
        
        Args:
            class_name: Nombre de la clase a detectar
            timeout: Timeout en segundos
        
        Returns:
            True si se detectó, False si timeout
        """
        start_time = time.time()
        
        while time.time() - start_time < timeout:
            detected_classes = self.get_detected_classes()
            if class_name in detected_classes:
                return True
            time.sleep(0.1)
        
        return False
    
    def count_detections(self) -> int:
        """
        Retorna el número de objetos detectados.
        
        Returns:
            Número de detecciones
        """
        with self._lock:
            return len(self.detections)
    
    def release(self):
        """Libera todos los recursos."""
        self.release_camera()
    
    def get_frame_with_detections(self, frame: Optional[np.ndarray] = None) -> Optional[np.ndarray]:
        """
        Retorna un frame con las detecciones dibujadas.
        
        Args:
            frame: Frame a procesar (usa el actual si es None)
        
        Returns:
            Frame anotado o None
        """
        if frame is None:
            frame = self.get_current_frame()
        
        if frame is None:
            return None
        
        _, annotated = self.detect_objects(frame)
        return annotated


class YOLOConfig:
    """Configuración del modelo YOLO para detección."""
    
    # Nombres de clases esperadas (se obtienen del modelo)
    CLASSES = {
        'macho': 'hacha_macho',
        'hembra': 'hacha_hembra',
        'lomotoro_macho': 'lomotoro_macho',
        'lomotoro_hembra': 'lomotoro_hembra',
        'caballo_macho': 'caballo_macho',
        'caballo_hembra': 'caballo_hembra',
        'craneo_macho': 'craneo_macho',
        'craneo_hembra': 'craneo_hembra'
    }
    
    # Umbrales de confianza
    MIN_CONFIDENCE = 0.5
    
    # Parámetros de detección
    CONF_THRESHOLD = 0.5
    IOU_THRESHOLD = 0.45
    
    @staticmethod
    def get_corresponding_piece(piece_name: str) -> Optional[str]:
        """
        Obtiene la pieza correspondiente (pareja macho-hembra).
        
        Args:
            piece_name: Nombre de la pieza
        
        Returns:
            Nombre de la pieza correspondiente o None
        """
        pairs = {
            'hacha_macho': 'hacha_hembra',
            'hacha_hembra': 'hacha_macho',
            'lomotoro_macho': 'lomotoro_hembra',
            'lomotoro_hembra': 'lomotoro_macho',
            'caballo_macho': 'caballo_hembra',
            'caballo_hembra': 'caballo_macho',
            'craneo_macho': 'craneo_hembra',
            'craneo_hembra': 'craneo_macho'
        }
        return pairs.get(piece_name)
    
    @staticmethod
    def is_macho(piece_name: str) -> bool:
        """Verifica si una pieza es tipo macho."""
        return 'macho' in piece_name.lower()
    
    @staticmethod
    def is_hembra(piece_name: str) -> bool:
        """Verifica si una pieza es tipo hembra."""
        return 'hembra' in piece_name.lower()
