#!/usr/bin/env python3
"""Genera PDFs A4 autocontenidos desde los documentos Markdown activos.
No requiere dependencias externas; está pensado para conservar un PDF de entrega
cuando Chrome/Puppeteer no está disponible en el equipo de documentación.
"""
from __future__ import annotations

from pathlib import Path
import re
import textwrap

ROOT = Path(__file__).resolve().parents[1]
DOCUMENTS = {
    ROOT / "docs/project/DOCUMENTACION_SISTEMA_CIM.md": ROOT / "docs/project/MANUAL_IMPLEMENTACION_CIM.pdf",
    ROOT / "docs/deliverables/ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.md": ROOT / "docs/deliverables/ENTREGA_PRE_HARDWARE_LEONARDO_ARAYA.pdf",
}
REPLACEMENTS = str.maketrans({"—": "-", "–": "-", "“": '"', "”": '"', "‘": "'", "’": "'", "•": "*", "…": "...", "✓": "OK", "⚠": "!"})


def markdown_lines(source: Path) -> list[str]:
    lines: list[str] = []
    for raw in source.read_text(encoding="utf-8").splitlines():
        raw = raw.strip().translate(REPLACEMENTS)
        if raw == "---" or raw.startswith("title:") or raw.startswith("author:") or raw.startswith("institution:") or raw.startswith("date:") or raw.startswith("status:"):
            continue
        raw = re.sub(r"!\[([^]]*)\]\([^)]*\)", r"[imagen: \1]", raw)
        raw = re.sub(r"\[([^]]+)\]\([^)]+\)", r"\1", raw)
        raw = raw.replace("`", "")
        raw = re.sub(r"^#{1,6}\s*", "", raw)
        raw = re.sub(r"^[-*+]\s+", "- ", raw)
        raw = raw.replace("|", "  |  ").strip()
        if not raw:
            lines.append("")
            continue
        prefix = ""
        if raw.startswith(("Arquitectura activa", "Comunicación e identidad", "Firmware canónico", "Construcción, entrega e instalación", "Secuencia operativa de alto nivel", "Seguridad y alcance de validación")):
            prefix = "[SECCIÓN] "
        width = 96
        lines.extend(prefix + item for item in textwrap.wrap(raw, width=width, break_long_words=False, break_on_hyphens=False) or [""])
    return lines


def escape_pdf(value: str) -> bytes:
    value = value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
    return value.encode("cp1252", errors="replace")


def make_pdf(lines: list[str], output: Path, title: str) -> None:
    # 48 lines per A4 page at 10pt / 12pt leading, including footer space.
    chunks = [lines[i:i + 48] for i in range(0, max(len(lines), 1), 48)]
    objects: list[bytes] = []
    def add(data: bytes) -> int:
        objects.append(data); return len(objects)
    font = add(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
    bold = add(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>")
    page_items: list[tuple[int, int]] = []
    for number, chunk in enumerate(chunks, 1):
        stream = [b"BT", b"/F2 14 Tf", b"50 800 Td", escape_pdf(title), b" Tj", b"/F1 9 Tf", b"0 -22 Td", b"12 TL"]
        for line in chunk:
            if line.startswith("[SECCIÓN] "):
                stream.extend([b"/F2 11 Tf", escape_pdf(line[9:]), b" Tj", b"/F1 9 Tf", b"T*"])
            else:
                stream.extend([escape_pdf(line), b" Tj", b"T*"])
        stream.extend([b"ET", b"BT /F1 8 Tf 260 25 Td", escape_pdf(f"Página {number} de {len(chunks)}"), b" Tj ET"])
        data = b"\n".join(stream)
        content = add(b"<< /Length " + str(len(data)).encode() + b" >>\nstream\n" + data + b"\nendstream")
        page_items.append((content, add(b"PLACEHOLDER")))
    pages = add(b"PLACEHOLDER")
    for content, page in page_items:
        objects[page - 1] = (b"<< /Type /Page /Parent " + str(pages).encode() + b" 0 R /MediaBox [0 0 595 842] "
                              b"/Resources << /Font << /F1 " + str(font).encode() + b" 0 R /F2 " + str(bold).encode() +
                              b" 0 R >> >> /Contents " + str(content).encode() + b" 0 R >>")
    kids = b" ".join(str(page).encode() + b" 0 R" for _, page in page_items)
    objects[pages - 1] = b"<< /Type /Pages /Kids [" + kids + b"] /Count " + str(len(page_items)).encode() + b" >>"
    catalog = add(b"<< /Type /Catalog /Pages " + str(pages).encode() + b" 0 R >>")
    out = b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n"; offsets = [0]
    for index, item in enumerate(objects, 1):
        offsets.append(len(out)); out += f"{index} 0 obj\n".encode() + item + b"\nendobj\n"
    startxref = len(out); out += f"xref\n0 {len(objects)+1}\n0000000000 65535 f\n".encode()
    out += b"".join(f"{offset:010d} 00000 n\n".encode() for offset in offsets[1:])
    out += (b"trailer\n<< /Size " + str(len(objects)+1).encode() + b" /Root " + str(catalog).encode() +
            b" 0 R /Title (" + escape_pdf(title) + b") >>\nstartxref\n" + str(startxref).encode() + b"\n%%EOF\n")
    output.write_bytes(out)


def main() -> int:
    for source, output in DOCUMENTS.items():
        make_pdf(markdown_lines(source), output, source.stem.replace("_", " "))
        print(f"Generado: {output.relative_to(ROOT)}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
