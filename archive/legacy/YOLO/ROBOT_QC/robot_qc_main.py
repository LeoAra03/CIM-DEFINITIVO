#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ROBOT QC - VENTANA PRINCIPAL DE CONTROL DE CALIDAD
====================================================
Sistema completo de control del robot SCORBOT-ER V plus con detección YOLO.

Módulos incluidos:
- Control serial del robot SCORBOT-ER V plus
- Detección de objetos con YOLO (bestMH.pt)
- Captura de cámara en tiempo real
- Secuencia automática de ensamblaje
- Registro de ejecución y eventos

Autor: Sistema de Manufactura
Fecha: 2024
"""

import threading
import time
import tkinter as tk
from tkinter import ttk, messagebox
from PIL import Image, ImageTk
from ultralytics import YOLO
import os
import datetime
import queue
import cv2
import numpy as np

try:
    import serial
    import serial.tools.list_ports
except Exception:
    serial = None

# Importar módulos del ROBOT QC
try:
    from robot_serial_handler import RobotSerialHandler
    from camera_detection import CameraDetectionModule
    from robot_sequence_logic import RobotSequenceLogic
    import config
except ImportError as e:
    print(f"⚠ Error al importar módulos: {e}")
    print("Asegúrate de que todos los módulos estén en la misma carpeta")
    config = None


class RobotQCApp:
    """Aplicación principal del ROBOT QC con interfaz gráfica."""
    
    def __init__(self, root):
        self.root = root
        self.root.title("ROBOT QC - Sistema de Manufactura")
        self.root.geometry("1400x900")
        self.root.minsize(1000, 700)
        
        # ===== INICIALIZACIÓN DE COMPONENTES =====
        
        # Cargar modelo YOLO
        try:
            # Buscar bestMH.pt primero dentro de ROBOT_QC, luego en carpeta padre
            yolo_path = os.path.join(os.path.dirname(__file__), 'bestMH.pt')
            if not os.path.exists(yolo_path):
                # Fallback: buscar en carpeta padre
                yolo_path = os.path.join(os.path.dirname(__file__), '..', 'bestMH.pt')
            
            if os.path.exists(yolo_path):
                self.model_yolo = YOLO(yolo_path)
                print(f"✓ Modelo YOLO cargado: {yolo_path}")
            else:
                print(f"⚠ Error: No se encontró bestMH.pt en:")
                print(f"   {os.path.dirname(__file__)}/bestMH.pt")
                print(f"   {os.path.dirname(__file__)}/../bestMH.pt")
                self.model_yolo = None
        except Exception as e:
            print(f"⚠ Error cargando YOLO: {e}")
            self.model_yolo = None
        
        # Inicializar serial handler
        self.serial_handler = RobotSerialHandler(
            port='COM6',
            baudrate=9600,
            timeout=2
        )
        
        # Inicializar módulo de cámara
        self.camera_module = CameraDetectionModule(
            yolo_model=self.model_yolo,
            camera_index=0
        )
        
        # Inicializar lógica de secuencia
        self.sequence_logic = RobotSequenceLogic(
            serial_handler=self.serial_handler,
            camera_module=self.camera_module
        )
        
        # ===== VARIABLES DE ESTADO =====
        self.cam_running = False
        self.sequence_running = False
        self.robot_queue_running = False
        self.current_port = 'COM6'
        
        # Parámetros de optimización de cámara
        self.frame_skip = 1  # Procesar cada frame (sin skip)
        self.frame_counter = 0
        self.yolo_input_size = config.CAMERA_YOLO_INPUT_SIZE if config else (640, 480)
        self.display_max_width = config.CAMERA_DISPLAY_WIDTH if config else 900
        self.display_max_height = config.CAMERA_DISPLAY_HEIGHT if config else 500
        
        # Thread y cola para YOLO asincrónico
        self.yolo_queue = queue.Queue(maxsize=2)  # Pequeña cola para frames a procesar
        self.yolo_results_cache = None  # Cachea última detección
        self.yolo_thread_running = False
        self.last_cam_img = None  # Caché de última imagen para UI
        
        # Cola de comandos del robot
        self.robot_command_queue = queue.Queue(maxsize=100)
        self.robot_queue_thread = None
        
        # ===== CONSTRUIR INTERFAZ =====
        self._build_ui()
        
        self._append_log("Sistema listo. Selecciona un puerto COM y haz clic en 'Conectar'.")
    
    def _get_available_ports(self):
        """Detecta y retorna los puertos COM disponibles."""
        if serial is None:
            return ['COM6']
        
        try:
            ports = [port.device for port in serial.tools.list_ports.comports()]
            return ports if ports else ['COM6']
        except Exception as e:
            print(f"⚠ Error detectando puertos: {e}")
            return ['COM6']
    
    def _build_ui(self):
        """Construye la interfaz gráfica del ROBOT QC."""
        
        # ===== MARCO PRINCIPAL =====
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.pack(fill=tk.BOTH, expand=True)
        
        # ===== TÍTULO =====
        title_frame = ttk.Frame(main_frame)
        title_frame.pack(fill=tk.X, pady=(0, 10))
        
        ttk.Label(title_frame, text="SCORBOT-ER V plus - Control de Calidad (QC)",
                 font=('Arial', 16, 'bold')).pack(side=tk.LEFT)
        
        ttk.Label(title_frame, text="Puerto: COM6",
                 font=('Arial', 10)).pack(side=tk.LEFT, padx=20)
        
        # ===== CONTENIDO PRINCIPAL (2 COLUMNAS) =====
        content_frame = ttk.Frame(main_frame)
        content_frame.pack(fill=tk.BOTH, expand=True)
        
        # ===== COLUMNA IZQUIERDA: CONTROLES =====
        left_frame = ttk.LabelFrame(content_frame, text="Controles del Robot", padding=15)
        left_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=False, padx=(0, 10))
        
        # Estado del robot
        status_frame = ttk.Frame(left_frame)
        status_frame.pack(fill=tk.X, pady=10)
        
        ttk.Label(status_frame, text="Estado:", font=('Arial', 10, 'bold')).pack(side=tk.LEFT)
        self.lbl_robot_status = ttk.Label(status_frame, text="Desconectado",
                                         font=('Arial', 10), foreground='red')
        self.lbl_robot_status.pack(side=tk.LEFT, padx=10)
        
        # Botones de conexión
        conn_frame = ttk.Frame(left_frame)
        conn_frame.pack(fill=tk.X, pady=10)
        
        # Dropdown de puertos COM
        ttk.Label(conn_frame, text='Puerto COM:').pack(side=tk.LEFT, padx=4)
        self.port_combo = ttk.Combobox(conn_frame, values=self._get_available_ports(),
                                       state='readonly', width=10)
        self.port_combo.set(self.current_port)
        self.port_combo.pack(side=tk.LEFT, padx=4)
        
        self.btn_connect = ttk.Button(conn_frame, text="Conectar",
                                     command=self._connect_robot, width=15)
        self.btn_connect.pack(side=tk.LEFT, padx=5)
        
        self.btn_disconnect = ttk.Button(conn_frame, text="Desconectar",
                                        command=self._disconnect_robot, state="disabled", width=15)
        self.btn_disconnect.pack(side=tk.LEFT, padx=5)
        
        # Separador
        ttk.Separator(left_frame, orient='horizontal').pack(fill=tk.X, pady=10)
        
        # Programas de movimiento
        ttk.Label(left_frame, text="Programas de Movimiento", 
                 font=('Arial', 11, 'bold')).pack(anchor=tk.W, pady=(10, 5))
        
        programs = [
            ('SR1', '🔵 SR1 - Programa 1'),
            ('SR2', '🟢 SR2 - Programa 2'),
            ('SR3', '🟡 SR3 - Programa 3 (Ensamblaje)'),
            ('SR4', '🔴 SR4 - Programa 4 (Finalización)'),
        ]
        
        self.status_labels = {}
        for prog_code, display_name in programs:
            btn_frame = ttk.Frame(left_frame)
            btn_frame.pack(fill=tk.X, pady=5)
            
            btn = ttk.Button(btn_frame, text=display_name,
                           command=lambda code=prog_code: self._execute_program(code),
                           width=30)
            btn.pack(side=tk.LEFT, fill=tk.X, expand=True)
            
            status_label = ttk.Label(btn_frame, text="○", font=('Arial', 12))
            status_label.pack(side=tk.LEFT, padx=8)
            
            self.status_labels[prog_code] = status_label
        
        # Separador
        ttk.Separator(left_frame, orient='horizontal').pack(fill=tk.X, pady=10)
        
        # Secuencia automática
        ttk.Label(left_frame, text="Secuencia Automática", 
                 font=('Arial', 11, 'bold')).pack(anchor=tk.W, pady=(10, 5))
        
        self.btn_auto_sequence = ttk.Button(left_frame, text="▶ Iniciar Secuencia",
                                           command=self._start_automatic_sequence,
                                           style='Accent.TButton', width=30)
        self.btn_auto_sequence.pack(fill=tk.X, pady=5)
        
        # Separador
        ttk.Separator(left_frame, orient='horizontal').pack(fill=tk.X, pady=10)
        
        # Controles adicionales
        ttk.Label(left_frame, text="Controles Adicionales",
                 font=('Arial', 11, 'bold')).pack(anchor=tk.W, pady=(10, 5))
        
        extra_frame = ttk.Frame(left_frame)
        extra_frame.pack(fill=tk.X, pady=5)
        
        ttk.Button(extra_frame, text="Home",
                  command=lambda: self._send_command('HOME'), width=14).pack(side=tk.LEFT, padx=2, fill=tk.X, expand=True)
        ttk.Button(extra_frame, text="Detener",
                  command=lambda: self._send_command('COFF'), width=14).pack(side=tk.LEFT, padx=2, fill=tk.X, expand=True)
        ttk.Button(extra_frame, text="Reset",
                  command=lambda: self._send_command('ABORT'), width=14).pack(side=tk.LEFT, padx=2, fill=tk.X, expand=True)
        
        # ===== COLUMNA DERECHA: CÁMARA Y LOG =====
        right_frame = ttk.Frame(content_frame)
        right_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)
        
        # ===== FRAME DE CÁMARA =====
        cam_frame = ttk.LabelFrame(right_frame, text="Cámara del Robot (YOLO)", padding=10)
        cam_frame.pack(fill=tk.BOTH, expand=True, pady=(0, 10))
        
        # Controles de cámara
        cam_controls = ttk.Frame(cam_frame)
        cam_controls.pack(fill=tk.X, pady=(0, 8))
        
        ttk.Label(cam_controls, text='Índice Cámara:').pack(side=tk.LEFT, padx=4)
        self.camera_index_var = tk.StringVar(value='0')
        self.camera_entry = ttk.Entry(cam_controls, textvariable=self.camera_index_var, width=5)
        self.camera_entry.pack(side=tk.LEFT, padx=4)
        
        ttk.Button(cam_controls, text='Cambiar',
                  command=self._change_camera_index).pack(side=tk.LEFT, padx=4)
        ttk.Button(cam_controls, text='Iniciar Cámara',
                  command=self._start_camera).pack(side=tk.LEFT, padx=4)
        ttk.Button(cam_controls, text='Detener Cámara',
                  command=self._stop_camera).pack(side=tk.LEFT, padx=4)
        
        # Mostrador de cámara
        self.cam_label = tk.Label(cam_frame, text='Cam preview', width=90, height=20,
                                 anchor='center', relief=tk.SUNKEN, bg='#222222')
        self.cam_label.pack(fill=tk.BOTH, expand=True)
        
        # ===== FRAME DE LOG =====
        log_frame = ttk.LabelFrame(right_frame, text="Registro de Ejecución", padding=10)
        log_frame.pack(fill=tk.BOTH, expand=True, padx=0)
        
        self.text_log = tk.Text(log_frame, height=8, width=80,
                               font=('Consolas', 9), wrap=tk.WORD)
        self.text_log.pack(fill=tk.BOTH, expand=True)
        
        # Scrollbar para el log
        scrollbar = ttk.Scrollbar(log_frame, orient='vertical', command=self.text_log.yview)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.text_log.config(yscrollcommand=scrollbar.set)
        
        # Botón de limpiar log
        ttk.Button(log_frame, text="Limpiar Log",
                  command=self._clear_log).pack(fill=tk.X, pady=5)
        
        # ===== INSTRUCCIONES =====
        instr_frame = ttk.LabelFrame(left_frame, text="Instrucciones", padding=10)
        instr_frame.pack(fill=tk.X, pady=10)
        
        instructions = """1. Seleccionar puerto COM
2. Conectar robot
3. Ingresar índice cámara
4. Iniciar cámara
5. Seleccionar programa o secuencia"""
        
        ttk.Label(instr_frame, text=instructions, font=('Arial', 9),
                 justify=tk.LEFT).pack(anchor=tk.W)
    
    # ===== MÉTODOS DE CONEXIÓN =====
    
    def _connect_robot(self):
        """Conecta el robot a través del puerto seleccionado."""
        try:
            # Obtener puerto seleccionado
            selected_port = self.port_combo.get()
            if not selected_port:
                messagebox.showerror('Error', 'Por favor selecciona un puerto COM')
                return
            
            # Actualizar puerto en el handler
            self.serial_handler.port = selected_port
            self.current_port = selected_port
            
            if not self.serial_handler.connect():
                messagebox.showerror('Error', f'No se pudo conectar al robot en {selected_port}')
                return
            
            # Iniciar cola de comandos
            if not self.robot_queue_running:
                self.robot_queue_running = True
                self.robot_queue_thread = threading.Thread(target=self._robot_queue_worker, daemon=True)
                self.robot_queue_thread.start()
            
            # Actualizar UI
            self.lbl_robot_status.config(text=f'Conectado en {selected_port}', foreground='green')
            self.btn_connect.config(state='disabled')
            self.btn_disconnect.config(state='normal')
            self.port_combo.config(state='disabled')
            
            self._append_log(f'✓ Robot conectado en {selected_port}')
            
            # Enviar comando inicial
            time.sleep(0.3)
            self._send_command('READY')
            
        except Exception as e:
            messagebox.showerror('Error de Conexión', f'{str(e)}')
            self.lbl_robot_status.config(text='Error', foreground='red')
    
    def _disconnect_robot(self):
        """Desconecta el robot."""
        try:
            self.robot_queue_running = False
            
            if not self.serial_handler.disconnect():
                self._append_log('⚠ Error al desconectar')
                return
            
            # Actualizar UI
            self.lbl_robot_status.config(text='Desconectado', foreground='red')
            self.btn_connect.config(state='normal')
            self.btn_disconnect.config(state='disabled')
            self.port_combo.config(state='readonly')
            
            self._append_log('✓ Robot desconectado')
            
        except Exception as e:
            self._append_log(f'⚠ Error: {str(e)}')
    
    # ===== MÉTODOS DE COMANDOS =====
    
    def _send_command(self, command):
        """Envía un comando al robot."""
        if not self.serial_handler.is_connected():
            self._append_log('⚠ Robot no conectado')
            return False
        
        try:
            cmd_clean = command.strip().upper()
            if self.serial_handler.send_command(cmd_clean):
                self._append_log(f'→ Enviado: {cmd_clean}')
                return True
            else:
                self._append_log(f'✗ Error enviando: {cmd_clean}')
                return False
        except Exception as e:
            self._append_log(f'✗ Error: {str(e)}')
            return False
    
    def _execute_program(self, program_name):
        """Ejecuta un programa específico del robot."""
        if not self.serial_handler.is_connected():
            messagebox.showwarning('Robot QC', 'El robot no está conectado')
            return
        
        self._append_log(f'▶ Iniciando programa {program_name}...')
        command = f'RUN {program_name}'
        self._send_command(command)
    
    def _robot_queue_worker(self):
        """Worker que procesa la cola de comandos del robot."""
        while self.robot_queue_running:
            try:
                command = self.robot_command_queue.get(timeout=1)
                self._send_command(command)
            except queue.Empty:
                continue
            except Exception as e:
                self._append_log(f'⚠ Error en cola: {str(e)}')
    
    # ===== MÉTODOS DE CÁMARA =====
    
    def _change_camera_index(self):
        """Cambia el índice de la cámara."""
        try:
            camera_index = int(self.camera_index_var.get())
            self.camera_module.set_camera_index(camera_index)
            self._append_log(f'📷 Índice de cámara cambiado a: {camera_index}')
        except ValueError:
            messagebox.showerror('Error', 'Por favor ingresa un número válido')
            self.camera_index_var.set('0')
    
    def _start_camera(self):
        """Inicia la captura de cámara."""
        if self.cam_running:
            messagebox.showwarning('Cámara', 'La cámara ya está en ejecución')
            return
        
        self.cam_running = True
        threading.Thread(target=self._camera_thread_worker, daemon=True).start()
        self._append_log('📷 Cámara iniciada')
    
    def _stop_camera(self):
        """Detiene la captura de cámara."""
        self.cam_running = False
        self.yolo_thread_running = False
        self.cam_label.config(image='', text='Cam preview')
        self._append_log('📷 Cámara detenida')
    
    def _camera_thread_worker(self):
        """Worker que captura frames - YOLO en thread separado para no bloquear."""
        try:
            # Usar CAP_DSHOW para mejor rendimiento en Windows
            cap = cv2.VideoCapture(self.camera_module.camera_index, cv2.CAP_DSHOW)
            
            # Optimizaciones para captura rápida
            cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)  # Buffer mínimo
            cap.set(cv2.CAP_PROP_FPS, 30)        # 30 FPS
            cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
            cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
            
            if not cap.isOpened():
                self._append_log('✗ Error: No se pudo abrir la cámara')
                self.cam_running = False
                return
            
            # Iniciar thread separado para YOLO
            self.yolo_thread_running = True
            yolo_thread = threading.Thread(target=self._yolo_processing_worker, daemon=True)
            yolo_thread.start()
            
            frame_counter = 0
            try:
                while self.cam_running:
                    ret, frame = cap.read()
                    if not ret:
                        time.sleep(0.01)
                        continue
                    
                    frame_counter += 1
                    
                    # Enviar frame a YOLO de forma no bloqueante
                    try:
                        self.yolo_queue.put_nowait(frame)
                    except queue.Full:
                        pass  # Si la cola está llena, descartar frame (es normal)
                    
                    # Mostrar frame con detecciones en caché
                    display_frame = self.yolo_results_cache if self.yolo_results_cache is not None else frame
                    
                    # Convertir a PIL Image y redimensionar eficientemente
                    frame_rgb = cv2.cvtColor(display_frame, cv2.COLOR_BGR2RGB)
                    frame_rgb = cv2.resize(frame_rgb, (960, 540))
                    img_pil = Image.fromarray(frame_rgb)
                    img_tk = ImageTk.PhotoImage(img_pil)
                    
                    # Actualizar UI en hilo principal usando after()
                    def _update_cam(imgtk=img_tk):
                        self.last_cam_img = imgtk
                        try:
                            self.cam_label.config(image=imgtk, text='')
                            self.cam_label.image = imgtk
                        except Exception:
                            pass
                    
                    self.root.after(0, _update_cam)
            finally:
                cap.release()
                self.yolo_thread_running = False
                self.cam_running = False
        except Exception as e:
            self._append_log(f'✗ Error en captura: {str(e)}')
    
    def _yolo_processing_worker(self):
        """Thread separado para procesar YOLO sin bloquear captura."""
        while self.yolo_thread_running:
            try:
                frame = self.yolo_queue.get(timeout=1)
                
                if self.model_yolo is not None:
                    try:
                        # Reducir resolución para inferencia más rápida
                        frame_small = cv2.resize(frame, self.yolo_input_size)
                        results = self.model_yolo(frame_small, verbose=False, conf=0.5)
                        
                        # Redimensionar anotaciones de vuelta
                        self.yolo_results_cache = cv2.resize(results[0].plot(), 
                                                            (frame.shape[1], frame.shape[0]))
                    except Exception as e:
                        self._append_log(f'⚠ Error YOLO: {str(e)}')
                        self.yolo_results_cache = frame
                else:
                    self.yolo_results_cache = frame
                    
            except queue.Empty:
                continue
            except Exception as e:
                self._append_log(f'⚠ Error thread YOLO: {str(e)}')
    
    def _display_frame(self, frame):
        """Muestra un frame en el label de la cámara."""
        try:
            # Convertir BGR a RGB
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            
            # Redimensionar de forma más eficiente
            h, w = frame_rgb.shape[:2]
            target_h = self.display_max_height
            target_w = int(w * (target_h / h))
            if target_w > self.display_max_width:
                target_w = self.display_max_width
            
            frame_resized = cv2.resize(frame_rgb, (target_w, target_h), interpolation=cv2.INTER_LINEAR)
            
            # Convertir a PIL Image
            img_pil = Image.fromarray(frame_resized)
            img_tk = ImageTk.PhotoImage(img_pil)
            
            # Actualizar label
            self.cam_label.config(image=img_tk, text='')
            self.cam_label.image = img_tk  # Guardar referencia
        except Exception as e:
            self._append_log(f'⚠ Error mostrando frame: {str(e)}')
    
    # ===== MÉTODOS DE SECUENCIA AUTOMÁTICA =====
    
    def _start_automatic_sequence(self):
        """Inicia la secuencia automática en un thread separado."""
        if self.sequence_running:
            messagebox.showwarning('Secuencia', 'Ya hay una secuencia en ejecución')
            return
        
        if not self.serial_handler.is_connected():
            messagebox.showerror('Secuencia', 'Debe conectar el robot primero')
            return
        
        if not self.cam_running:
            messagebox.showerror('Secuencia', 'Debe iniciar la cámara primero')
            return
        
        self.sequence_running = True
        threading.Thread(target=self._automatic_sequence_worker, daemon=True).start()
    
    def _automatic_sequence_worker(self):
        """Worker que ejecuta la secuencia automática."""
        try:
            self._append_log('═══════════════════════════════════════')
            self._append_log('▶ INICIANDO SECUENCIA AUTOMÁTICA')
            self._append_log('═══════════════════════════════════════')
            
            # Implementar lógica de secuencia desde RobotSequenceLogic
            # Esta es una versión simplificada
            
            self._append_log('📍 PASO 1: Ejecutando SR1...')
            self._send_command('RUN SR1')
            time.sleep(5)
            
            self._append_log('📍 PASO 2: Ejecutando SR2...')
            self._send_command('RUN SR2')
            time.sleep(5)
            
            self._append_log('📍 PASO 3: Ejecutando SR3...')
            self._send_command('RUN SR3')
            time.sleep(5)
            
            self._append_log('📍 PASO 4: Ejecutando SR4...')
            self._send_command('RUN SR4')
            time.sleep(5)
            
            self._append_log('✓ Secuencia completada exitosamente')
            self._append_log('═══════════════════════════════════════')
            
        except Exception as e:
            self._append_log(f'✗ Error en secuencia: {str(e)}')
        finally:
            self.sequence_running = False
    
    # ===== MÉTODOS DE LOG =====
    
    def _append_log(self, message):
        """Añade un mensaje al log con timestamp."""
        timestamp = datetime.datetime.now().strftime('%H:%M:%S')
        log_message = f'[{timestamp}] {message}\n'
        
        self.text_log.insert(tk.END, log_message)
        self.text_log.see(tk.END)
        self.root.update_idletasks()
    
    def _clear_log(self):
        """Limpia el log."""
        self.text_log.delete(1.0, tk.END)
        self._append_log('Log limpiado')
    
    # ===== CIERRE DE LA APLICACIÓN =====
    
    def _on_close(self):
        """Maneja el cierre de la aplicación."""
        try:
            self.cam_running = False
            self.robot_queue_running = False
            self.sequence_running = False
            
            self.serial_handler.disconnect()
            self.camera_module.release()
            
        except Exception:
            pass
        finally:
            try:
                self.root.quit()
                self.root.destroy()
            except Exception:
                pass
    
    def run(self):
        """Inicia la aplicación."""
        self.root.protocol('WM_DELETE_WINDOW', self._on_close)
        self.root.mainloop()


def main():
    """Función principal."""
    root = tk.Tk()
    app = RobotQCApp(root)
    app.run()


if __name__ == '__main__':
    main()
