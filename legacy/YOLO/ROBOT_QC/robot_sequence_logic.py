#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ROBOT SEQUENCE LOGIC - Lógica de secuencia automática del robot QC
===================================================================

Este módulo contiene la lógica para:
- Ejecutar secuencias automáticas del robot
- Coordinar detección de piezas macho/hembra
- Esperar confirmaciones del robot
- Manejar eventos de ensamblaje

Autor: Sistema de Manufactura
Fecha: 2024
"""

import time
import threading
from typing import Optional
from robot_serial_handler import RobotSerialHandler
from camera_detection import CameraDetectionModule, YOLOConfig


class RobotSequenceLogic:
    """Lógica de secuencia automática del robot SCORBOT-ER V plus."""
    
    def __init__(self, serial_handler: RobotSerialHandler, camera_module: CameraDetectionModule):
        """
        Inicializa la lógica de secuencia.
        
        Args:
            serial_handler: Manejador serial del robot
            camera_module: Módulo de detección de cámara
        """
        self.serial_handler = serial_handler
        self.camera_module = camera_module
        
        # Estado de la secuencia
        self.sequence_running = False
        self.sequence_paused = False
        self.detected_first_piece: Optional[str] = None
        
        # Parejas macho-hembra
        self.macho_hembra_pairs = {
            'hacha_macho': 'hacha_hembra',
            'lomotoro_macho': 'lomotoro_hembra',
            'caballo_macho': 'caballo_hembra',
            'craneo_macho': 'craneo_hembra'
        }
        
        self.hembra_macho_pairs = {v: k for k, v in self.macho_hembra_pairs.items()}
        
        # Callbacks
        self.on_log_message = None
    
    def log(self, message: str):
        """Registra un mensaje de log."""
        if self.on_log_message:
            self.on_log_message(message)
        else:
            print(message)
    
    def execute_automatic_sequence(self):
        """
        Ejecuta la secuencia automática completa:
        1. SR1 - Posicionar
        2. Detectar primera pieza (macho)
        3. Esperar 9 segundos
        4. SR2 - Traer
        5. Esperar y detectar pieza hembra
        6. Esperar 15 segundos
        7. SR3 - Ensamblar
        8. SR4 - Finalizar
        9. SR2 - Retornar
        
        Returns:
            True si la secuencia se completó, False si hubo error
        """
        try:
            self.sequence_running = True
            
            self.log('═══════════════════════════════════════')
            self.log('▶ INICIANDO SECUENCIA AUTOMÁTICA COMPLETA')
            self.log('═══════════════════════════════════════')
            
            # PASO 1: Ejecutar SR1
            self.log('📍 PASO 1: Ejecutando SR1 (Posicionar)...')
            if not self._execute_program_and_wait('SR1', timeout=150):
                self.log('✗ Error: No se completó SR1')
                return False
            self.log('✓ SR1 completado')
            
            # PASO 2: Detectar primera pieza
            self.log('📍 PASO 2: Detectando primera pieza (macho)...')
            time.sleep(2.0)
            
            detected_class = self._detect_piece_with_timeout(timeout=6.0)
            if not detected_class:
                self.log('✗ Error: No se detectó ninguna pieza en PASO 2')
                return False
            
            self.detected_first_piece = detected_class
            self.log(f'✓ Primera pieza detectada: {detected_class}')
            
            # Obtener pieza correspondiente
            corresponding_piece = self._get_corresponding_piece(detected_class)
            if not corresponding_piece:
                self.log(f'✗ Error: No hay pareja para {detected_class}')
                return False
            
            self.log(f'🔍 Pieza correspondiente esperada: {corresponding_piece}')
            
            # PASO 3: Esperar 9 segundos
            delay = 9
            self.log(f'⏱ Esperando {delay} segundos antes de SR2...')
            time.sleep(delay)
            
            # PASO 4: Ejecutar SR2
            self.log('📍 PASO 4: Ejecutando SR2 (Traer)...')
            if not self._execute_program_and_wait('SR2', timeout=150):
                self.log('✗ Error: No se completó SR2')
                return False
            self.log('✓ SR2 completado')
            
            # PASO 5: Esperar detectar pieza hembra
            self.log(f'📍 PASO 5: Esperando detectar {corresponding_piece}...')
            if not self._wait_for_piece(corresponding_piece, timeout=60):
                self.log(f'✗ Error: No se detectó {corresponding_piece}')
                return False
            self.log(f'✓ Pieza correspondiente detectada: {corresponding_piece}')
            
            # PASO 6: Esperar 15 segundos
            delay = 15
            self.log(f'⏱ Esperando {delay} segundos antes de SR3...')
            time.sleep(delay)
            
            # PASO 7: Ejecutar SR3 (Ensamblaje)
            self.log('📍 PASO 7: Ejecutando SR3 (Ensamblar)...')
            if not self._execute_program_and_wait('SR3', timeout=150):
                self.log('✗ Error: No se completó SR3')
                return False
            self.log('✓ SR3 completado - Ensamblaje exitoso')
            
            # PASO 8: Ejecutar SR4 (Finalización)
            self.log('📍 PASO 8: Ejecutando SR4 (Finalizar)...')
            if not self._execute_program_and_wait('SR4', timeout=150):
                self.log('✗ Error: No se completó SR4')
                return False
            self.log('✓ SR4 completado')
            
            # PASO 9: Ejecutar SR2 final (Retornar)
            self.log('📍 PASO 9: Ejecutando SR2 (Retornar)...')
            if not self._execute_program_and_wait('SR2', timeout=150):
                self.log('✗ Error: No se completó SR2 final')
                return False
            self.log('✓ SR2 completado - Retorno')
            
            self.log('═══════════════════════════════════════')
            self.log('✓✓✓ SECUENCIA COMPLETADA EXITOSAMENTE ✓✓✓')
            self.log('═══════════════════════════════════════')
            
            return True
            
        except Exception as e:
            self.log(f'✗ Error general en secuencia: {str(e)}')
            return False
        finally:
            self.sequence_running = False
    
    def _execute_program_and_wait(self, program_name: str, timeout: float = 150.0) -> bool:
        """
        Ejecuta un programa del robot y espera a que se complete.
        
        Args:
            program_name: Nombre del programa (SR1, SR2, SR3, SR4)
            timeout: Timeout máximo en segundos
        
        Returns:
            True si el programa se ejecutó, False si timeout
        """
        try:
            # Enviar comando
            command = f'RUN {program_name}'
            if not self.serial_handler.send_command(command):
                self.log(f'✗ Error enviando {program_name}')
                return False
            
            # Esperar a que se complete
            start_time = time.time()
            while time.time() - start_time < timeout:
                time.sleep(0.5)
                
                if self.sequence_paused:
                    self.log(f'⏸ Secuencia pausada durante {program_name}')
                    return False
            
            return True
            
        except Exception as e:
            self.log(f'✗ Error en _execute_program_and_wait: {str(e)}')
            return False
    
    def _detect_piece_with_timeout(self, timeout: float = 6.0) -> Optional[str]:
        """
        Detecta una pieza dentro de un timeout.
        
        Args:
            timeout: Timeout en segundos
        
        Returns:
            Nombre de la clase detectada o None
        """
        try:
            start_time = time.time()
            
            while time.time() - start_time < timeout:
                piece_type = self.camera_module.get_detected_piece_type()
                if piece_type:
                    self.log(f'  → Pieza detectada: {piece_type}')
                    return piece_type
                
                time.sleep(0.2)
            
            return None
            
        except Exception as e:
            self.log(f'✗ Error en _detect_piece_with_timeout: {str(e)}')
            return None
    
    def _wait_for_piece(self, piece_name: str, timeout: float = 60.0) -> bool:
        """
        Espera a detectar una pieza específica.
        
        Args:
            piece_name: Nombre de la pieza a detectar
            timeout: Timeout en segundos
        
        Returns:
            True si se detectó, False si timeout
        """
        try:
            start_time = time.time()
            
            while time.time() - start_time < timeout:
                detected_classes = self.camera_module.get_detected_classes()
                
                if piece_name in detected_classes:
                    self.log(f'  → Pieza detectada: {piece_name}')
                    return True
                
                time.sleep(0.2)
            
            self.log(f'  ✗ Timeout esperando: {piece_name}')
            return False
            
        except Exception as e:
            self.log(f'✗ Error en _wait_for_piece: {str(e)}')
            return False
    
    def _get_corresponding_piece(self, piece_name: str) -> Optional[str]:
        """
        Obtiene la pieza correspondiente (pareja macho-hembra).
        
        Args:
            piece_name: Nombre de la pieza
        
        Returns:
            Nombre de la pieza correspondiente o None
        """
        try:
            # Buscar en parejas macho-hembra
            if piece_name in self.macho_hembra_pairs:
                return self.macho_hembra_pairs[piece_name]
            
            # Buscar en parejas hembra-macho
            if piece_name in self.hembra_macho_pairs:
                return self.hembra_macho_pairs[piece_name]
            
            return None
            
        except Exception as e:
            self.log(f'✗ Error en _get_corresponding_piece: {str(e)}')
            return None
    
    def pause_sequence(self):
        """Pausa la secuencia."""
        self.sequence_paused = True
        self.log('⏸ Secuencia pausada')
    
    def resume_sequence(self):
        """Reanuda la secuencia."""
        self.sequence_paused = False
        self.log('▶ Secuencia reanudada')
    
    def stop_sequence(self):
        """Detiene la secuencia."""
        self.sequence_running = False
        self.sequence_paused = False
        self.log('⏹ Secuencia detenida')
        
        # Enviar comando de parada al robot
        try:
            self.serial_handler.send_command('COFF')
        except Exception:
            pass
    
    def get_status(self) -> dict:
        """
        Retorna el estado actual de la secuencia.
        
        Returns:
            Diccionario con el estado
        """
        return {
            'running': self.sequence_running,
            'paused': self.sequence_paused,
            'first_piece': self.detected_first_piece,
            'pairs_count': len(self.macho_hembra_pairs)
        }
