#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RUN.PY - Script para ejecutar el ROBOT QC
==========================================

Ejecutar con: python run.py

Este script:
1. Verifica e instala automáticamente las dependencias si faltan
2. Verifica la configuración
3. Inicia la aplicación ROBOT QC
"""

import sys
import os
import subprocess
from pathlib import Path

# Agregar directorio actual al path
sys.path.insert(0, str(Path(__file__).parent))


def install_dependencies():
    """Instala las dependencias automáticamente."""
    print("Instalando dependencias necesarias...\n")
    
    req_file = Path(__file__).parent / "requirements.txt"
    
    if not req_file.exists():
        print("✗ Error: No se encontró requirements.txt")
        return False
    
    try:
        subprocess.check_call([
            sys.executable, "-m", "pip", "install", 
            "-r", str(req_file), "-q"  # -q para modo silencioso
        ])
        print("✓ Dependencias instaladas correctamente\n")
        return True
    except Exception as e:
        print(f"✗ Error instalando dependencias: {e}")
        return False


def main():
    """Función principal."""
    print("\n" + "="*60)
    print("  ROBOT QC - Sistema de Control de Calidad")
    print("  SCORBOT-ER V plus")
    print("="*60 + "\n")
    
    # Verificar imports críticos
    print("Verificando dependencias...")
    
    critical_modules = [
        ("tkinter", "Interfaz gráfica"),
        ("cv2", "Procesamiento de video"),
        ("serial", "Comunicación serial"),
        ("ultralytics", "YOLO"),
        ("PIL", "Procesamiento de imágenes"),
    ]
    
    missing = []
    for module, description in critical_modules:
        try:
            __import__(module)
            print(f"  ✓ {description}")
        except ImportError:
            print(f"  ✗ {description} - FALTA INSTALAR")
            missing.append(module)
    
    # Si faltan dependencias, intentar instalar
    if missing:
        print("\n⚠ Faltan dependencias. Intentando instalar automáticamente...\n")
        if not install_dependencies():
            print("\nNo se pudieron instalar las dependencias automáticamente.")
            print("Intenta ejecutar manualmente:")
            print("  python install.py")
            print("  o")
            print("  pip install -r requirements.txt")
            return 1
    else:
        print("\n✓ Todas las dependencias están instaladas\n")
    
    # Verificar archivo de configuración
    config_file = Path(__file__).parent / "config.py"
    if not config_file.exists():
        print("✗ Error: No se encontró config.py")
        return 1
    
    # Verificar modelo YOLO
    robot_qc_dir = Path(__file__).parent
    model_path = robot_qc_dir / "bestMH.pt"
    
    if not model_path.exists():
        # Fallback: buscar en carpeta padre
        model_path = robot_qc_dir.parent / "bestMH.pt"
        
        if not model_path.exists():
            print("⚠ Advertencia: Modelo YOLO no encontrado")
            print(f"  Esperado en: {robot_qc_dir / 'bestMH.pt'} (preferido)")
            print(f"  O en: {model_path} (fallback)")
            print("\nAsegúrate de copiar el archivo bestMH.pt a la carpeta ROBOT_QC\n")
    else:
        print("✓ Modelo YOLO encontrado\n")
    
    print("Iniciando aplicación...\n")
    
    try:
        from robot_qc_main import main as robot_qc_main
        robot_qc_main()
        return 0
    except Exception as e:
        print(f"\n✗ Error al iniciar la aplicación:")
        print(f"  {str(e)}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
