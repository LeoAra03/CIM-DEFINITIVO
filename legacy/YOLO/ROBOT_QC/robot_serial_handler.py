#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ROBOT SERIAL HANDLER - Manejo de comunicación serial del robot SCORBOT-ER V plus
==================================================================================

Este módulo contiene toda la lógica para:
- Conectar/desconectar el robot por puerto COM
- Enviar comandos al robot
- Leer respuestas
- Gestionar la cola de comandos

Autor: Sistema de Manufactura
Fecha: 2024
"""

import serial
import time
import threading
from typing import Optional


class RobotSerialHandler:
    """Manejador de comunicación serial del robot SCORBOT-ER V plus."""
    
    def __init__(self, port: str = 'COM6', baudrate: int = 9600, timeout: float = 2.0):
        """
        Inicializa el manejador serial del robot.
        
        Args:
            port: Puerto serial (ej: 'COM6')
            baudrate: Velocidad en baudios (por defecto 9600)
            timeout: Timeout de lectura en segundos
        """
        self.port = port
        self.baudrate = baudrate
        self.timeout = timeout
        self.serial_port: Optional[serial.Serial] = None
        self._lock = threading.Lock()
    
    def connect(self) -> bool:
        """
        Conecta al robot a través del puerto serial.
        
        Returns:
            True si la conexión fue exitosa, False en caso contrario
        """
        try:
            with self._lock:
                if self.serial_port is not None and self.serial_port.is_open:
                    self.serial_port.close()
                
                self.serial_port = serial.Serial(
                    port=self.port,
                    baudrate=self.baudrate,
                    bytesize=serial.EIGHTBITS,
                    parity=serial.PARITY_NONE,
                    stopbits=serial.STOPBITS_ONE,
                    timeout=self.timeout
                )
                
                # Limpiar buffers
                self.serial_port.reset_input_buffer()
                self.serial_port.reset_output_buffer()
                
                time.sleep(0.5)
                return True
        except Exception as e:
            print(f"✗ Error de conexión: {e}")
            self.serial_port = None
            return False
    
    def disconnect(self) -> bool:
        """
        Desconecta del robot.
        
        Returns:
            True si la desconexión fue exitosa
        """
        try:
            with self._lock:
                if self.serial_port is not None and self.serial_port.is_open:
                    try:
                        # Enviar comando de parada
                        self.send_command_raw('COFF')
                        time.sleep(0.3)
                    except Exception:
                        pass
                    
                    self.serial_port.close()
                    self.serial_port = None
                    return True
        except Exception as e:
            print(f"✗ Error de desconexión: {e}")
        
        return False
    
    def is_connected(self) -> bool:
        """
        Verifica si el robot está conectado.
        
        Returns:
            True si está conectado, False en caso contrario
        """
        try:
            with self._lock:
                return self.serial_port is not None and self.serial_port.is_open
        except Exception:
            return False
    
    def send_command(self, command: str) -> bool:
        """
        Envía un comando al robot y lee la respuesta.
        
        Args:
            command: Comando a enviar (ej: 'RUN SR1')
        
        Returns:
            True si el comando fue enviado exitosamente
        """
        try:
            with self._lock:
                if self.serial_port is None or not self.serial_port.is_open:
                    return False
                
                # Formatear comando
                cmd_clean = command.strip().upper()
                payload = (cmd_clean + '\r').encode('ascii', errors='ignore')
                
                # Limpiar buffer de entrada
                self.serial_port.reset_input_buffer()
                
                # Enviar comando
                self.serial_port.write(payload)
                self.serial_port.flush()
                
                # Leer respuesta
                time.sleep(0.1)
                waiting = self.serial_port.in_waiting
                if waiting > 0:
                    response = self.serial_port.read(waiting).decode('ascii', errors='ignore')
                    print(f"  Respuesta: {response.strip()}")
                
                return True
        except Exception as e:
            print(f"✗ Error en send_command: {e}")
            return False
    
    def send_command_raw(self, command: str) -> bool:
        """
        Envía un comando sin lock (para uso interno).
        
        Args:
            command: Comando a enviar
        
        Returns:
            True si fue exitoso
        """
        try:
            if self.serial_port is None or not self.serial_port.is_open:
                return False
            
            cmd_clean = command.strip().upper()
            payload = (cmd_clean + '\r').encode('ascii', errors='ignore')
            
            self.serial_port.write(payload)
            self.serial_port.flush()
            
            return True
        except Exception:
            return False
    
    def read_response(self, timeout: Optional[float] = None) -> Optional[str]:
        """
        Lee la respuesta del robot.
        
        Args:
            timeout: Timeout en segundos (usa el timeout por defecto si es None)
        
        Returns:
            String con la respuesta o None si hay error
        """
        try:
            with self._lock:
                if self.serial_port is None or not self.serial_port.is_open:
                    return None
                
                old_timeout = self.serial_port.timeout
                if timeout is not None:
                    self.serial_port.timeout = timeout
                
                try:
                    data = self.serial_port.read_all()
                    return data.decode('ascii', errors='ignore')
                finally:
                    if timeout is not None:
                        self.serial_port.timeout = old_timeout
        except Exception as e:
            print(f"✗ Error en read_response: {e}")
            return None
    
    def send_and_wait_for_ok(self, command: str, timeout: float = 30.0) -> bool:
        """
        Envía un comando y espera a recibir una respuesta que contenga 'OK'.
        
        Args:
            command: Comando a enviar
            timeout: Timeout máximo en segundos
        
        Returns:
            True si se recibió OK, False si timeout o error
        """
        try:
            if not self.send_command(command):
                return False
            
            start_time = time.time()
            while time.time() - start_time < timeout:
                response = self.read_response(timeout=0.5)
                if response and 'OK' in response.upper():
                    return True
                time.sleep(0.1)
            
            return False
        except Exception as e:
            print(f"✗ Error en send_and_wait_for_ok: {e}")
            return False
    
    def get_port_info(self) -> dict:
        """
        Retorna información de la conexión actual.
        
        Returns:
            Diccionario con información del puerto
        """
        return {
            'port': self.port,
            'baudrate': self.baudrate,
            'timeout': self.timeout,
            'is_connected': self.is_connected(),
            'serial_port': str(self.serial_port) if self.serial_port else None
        }


# Ejemplos de comandos SCORBOT-ER V plus:
"""
READY        - Verifica si el robot está listo
COFF         - Apaga los servomotores
HOME         - Va a la posición de inicio
ABORT        - Aborta el movimiento actual
RUN SR1      - Ejecuta el programa SR1
RUN SR2      - Ejecuta el programa SR2
RUN SR3      - Ejecuta el programa SR3
RUN SR4      - Ejecuta el programa SR4
OPEN         - Abre el gripper
CLOSE        - Cierra el gripper
MJ 1 10      - Mueve eje 1 (base) 10 grados
"""
