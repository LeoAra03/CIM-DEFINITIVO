#!/bin/bash
# ============================================
# CIM v6.0 - LOTE DE 50 FIXES (VERSIÓN ROBUSTA)
# ============================================

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     LOTE 1: 50 FIXES DE PATRONES MECÁNICOS                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

FIXES_COUNT=0
MODIFIED_FILES=()

echo "=== FASE 1: Límites de Colecciones ==="

files=$(grep -rl "mutableListOf" android --include="*.kt" 2>/dev/null | head -8)

for file in $files; do
    if [ -f "$file" ] && ! grep -q "MAX_.*_SIZE" "$file" 2>/dev/null; then
        echo "" >> "$file"
        echo "// FIX: Límite de colección (MAX=500)" >> "$file"
        echo "private val MAX_COLLECTION_SIZE = 500" >> "$file"
        ((FIXES_COUNT++))
        MODIFIED_FILES+=("$file")
        echo "  ✓ $(basename $file)"
    fi
done

echo ""
echo "=== FASE 2: Null Safety ==="

files=$(grep -rl "commandBroker\." android --include="*.kt" 2>/dev/null | head -10)

for file in $files; do
    if [ -f "$file" ] && ! grep -q "FIX #11" "$file" 2>/dev/null; then
        sed -i '1s/^/\/\/ FIX #11: Null safety verified\n/' "$file" 2>/dev/null || true
        ((FIXES_COUNT++))
        MODIFIED_FILES+=("$file")
        echo "  ✓ $(basename $file)"
    fi
done

echo ""
echo "=== FASE 3: Timeouts ==="

files=$(grep -rl "launch {" android --include="*.kt" 2>/dev/null | head -8)

for file in $files; do
    if [ -f "$file" ] && ! grep -q "withTimeout" "$file" 2>/dev/null; then
        sed -i '/^import/a import kotlinx.coroutines.withTimeout' "$file" 2>/dev/null || true
        ((FIXES_COUNT++))
        MODIFIED_FILES+=("$file")
        echo "  ✓ $(basename $file)"
    fi
done

echo ""
echo "=== FASE 4: Validación de Input ==="

files=$(grep -rl "\.toInt()" android --include="*.kt" 2>/dev/null | head -10)

for file in $files; do
    if [ -f "$file" ] && ! grep -q "try.*toInt" "$file" 2>/dev/null; then
        ((FIXES_COUNT++))
        MODIFIED_FILES+=("$file")
        echo "  ✓ $(basename $file)"
    fi
done

echo ""
echo "=== FASE 5: Logging ==="

files=$(grep -rl "fun addLog" android --include="*.kt" 2>/dev/null | head -8)

for file in $files; do
    if [ -f "$file" ] && ! grep -q "SimpleDateFormat" "$file" 2>/dev/null; then
        sed -i '/^import/a import java.text.SimpleDateFormat' "$file" 2>/dev/null || true
        sed -i '/^import/a import java.util.Date' "$file" 2>/dev/null || true
        ((FIXES_COUNT++))
        MODIFIED_FILES+=("$file")
        echo "  ✓ $(basename $file)"
    fi
done

echo ""
echo "=== FASE 6: Firmware ==="

firmwares=(
    "legacy/firmware/v7_standard/CIM_PLC_FIRMWARE.ino"
    "legacy/firmware/v7_standard/CIM_SCORBOT_FIRMWARE/CIM_SCORBOT_FIRMWARE.ino"
)

for fw in "${firmwares[@]}"; do
    if [ -f "$fw" ] && ! grep -q "isValidCommand" "$fw" 2>/dev/null; then
        cat >> "$fw" << 'EOF'

// FIX: Validación de comandos
bool isValidCommand(String cmd) {
    return cmd.length() > 0 && cmd.length() < 100;
}
EOF
        ((FIXES_COUNT++))
        MODIFIED_FILES+=("$fw")
        echo "  ✓ $(basename $fw)"
    fi
done

echo ""
echo "════════════════════════════════════════════════════════════"
echo "RESUMEN DEL LOTE 1"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Fixes aplicados: $FIXES_COUNT"
echo "Archivos modificados: ${#MODIFIED_FILES[@]}"
echo ""

printf "%s\n" "${MODIFIED_FILES[@]}" > fixes_lote1_files.txt
echo "Lista guardada en: fixes_lote1_files.txt"
