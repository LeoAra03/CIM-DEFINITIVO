#!/usr/bin/env python3
"""Puerta documental pre-hardware; no habilita actuadores ni reemplaza ensayos físicos."""
from __future__ import annotations
import argparse
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REQUIRED=[
 'docs/deliverables/ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.pdf',
 'docs/deliverables/ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.md',
 'docs/deliverables/BITACORA_VALIDACION.md',
 'docs/deliverables/MANUAL_OPERATIVO_LABORATORIO.md',
 'docs/deliverables/PROCESO_VALIDACION_PRE_HARDWARE.md',
 'docs/deliverables/LLUVIA_IDEAS_Y_DECISIONES.md',
 'docs/deliverables/FALENCIAS_RIESGOS_Y_PLAN.md',
 'docs/deliverables/GUIA_PRESENTACION_TESIS.md',
 'docs/deliverables/EXPECTATIVA_VS_RESULTADO.md',
 'docs/deliverables/PRE_HARDWARE_READINESS.md',
 'docs/deliverables/PROTOCOLO_PRUEBAS_HARDWARE.md', 'docs/INDEX_REPOSITORIO.md']
def main():
 p=argparse.ArgumentParser(description=__doc__); p.add_argument('--quiet',action='store_true'); a=p.parse_args()
 missing=[x for x in REQUIRED if not (ROOT/x).is_file()]
 unsafe=[]
 for x in REQUIRED:
  path=ROOT/x
  if path.suffix=='.md' and path.exists() and not any(phrase in path.read_text(encoding='utf8',errors='ignore').lower() for phrase in ('no sustituye', 'no demuestra', 'no autoriza')) and x.endswith('ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.md'):
   unsafe.append(x+' no declara límite de hardware')
 errors=missing+unsafe
 if errors: print('FAIL: '+ '; '.join(errors)); return 1
 if not a.quiet: print(f'PASS: readiness pre-hardware documental ({len(REQUIRED)} archivos); no autoriza actuadores')
 return 0
if __name__=='__main__': raise SystemExit(main())
