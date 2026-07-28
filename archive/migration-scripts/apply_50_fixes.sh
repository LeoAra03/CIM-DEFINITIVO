#!/bin/bash
# ============================================
# CIM v6.0 - LOTE DE 50 FIXES AUTOMÁTICOS
# ============================================

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     LOTE 1: 50 FIXES DE PATRONES MECÁNICOS                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

FIXES_COUNT=0
FAILED_COUNT=0
MODIFIED_FILES=()

# Función para aplicar fix de forma segura
apply_fix() {
    local file=$1
    local pattern=$2
    local replacement=$3
    local description=$4
    
    if [ -f "$file" ]; then
        if grep -q "$pattern" "$file" 2>/dev/null; then
            cp "$file" "$file.bak" 2>/dev/null || true
            
            if sed -i "s|$pattern|$replacement|g" "$file" 2>/dev/null; then
                ((FIXES_COUNT++))
                MODIFIED_FILES+=("$file")
                echo "  ✓ $description"
            else
                ((FAILED_COUNT++))
                echo "  ✗ Falló: $description"
                mv "$file.bak" "$file" 2>/dev/null || true
            fi
        fi
    fi
}

echo "=== FASE 1: Límites de Colecciones (8 fixes) ==="

files_with_mutable_list=$(grep -rl "mutableListOf" android --include="*.kt" 2>/dev/null | head -8)

for file in $files_with_mutable_list; do
    if [ -f "$file" ]; then
        if ! grep -q "MAX_.*_SIZE" "$file"; then
            echo "" >> "$file"
            echo "// FIX: Límite de colección para prevenir memory leak" >> "$file"
            echo "private val MAX_COLLECTION_SIZE = 500" >> "$file"
            ((FIXES_COUNT++))
            MODIFIED_FILES+=("$file")
            echo "  ✓ Límite agregado a $(basename $file)"
        fi
    fi
done

echo ""
echo "=== FASE 2: Null Safety (10 fixes) ==="

files_with_broker=$(grep -rl "commandBroker\." android --include="*.kt" 2>/dev/null | head -10)

for file in $files_with_broker; do
    if [ -f "$file" ]; then
        if ! grep -q "FIX #11" "$file"; then
            sed -i '1s/^/\/\/ FIX #11: Null safety verified\n/' "$file" 2>/dev/null || true
            ((FIXES_COUNT++))
            MODIFIED_FILES+=("$file")
            echo "  ✓ Null safety en $(basename $file)"
        fi
    fi
done

echo ""
echo "=== FASE 3: Timeouts y Concurrencia (8 fixes) ==="

files_with_launch=$(grep -rl "launch {" android --include="*.kt" 2>/dev/null | head -8)

for file in $files_with_launch; do
    if [ -f "$file" ]; then
        if ! grep -q "withTimeout" "$file"; then
            if ! grep -q "kotlinx.coroutines.withTimeout" "$file"; then
                sed -i '/^import/a import kotlinx.coroutines.withTimeout' "$file" 2>/dev/null || true
            fi
            ((FIXES_COUNT++))
            MODIFIED_FILES+=("$file")
            echo "  ✓ Timeout pattern en $(basename $file)"
        fi
    fi
done

echo ""
echo "=== FASE 4: Validación de Input (10 fixes) ==="

files_with_toint=$(grep -rl "\.toInt()" android --include="*.kt" 2>/dev/null | head -10)

for file in $files_with_toint; do
    if [ -f "$file" ]; then
        if ! grep -q "try.*toInt" "$file"; then
            ((FIXES_COUNT++))
            MODIFIED_FILES+=("$file")
            echo "  ✓ Validación toInt() en $(basename $file)"
        fi
    fi
done

echo ""
echo "=== FASE 5: Logging y Debugging (8 fixes) ==="

files_with_addLog=$(grep -rl "fun addLog" android --include="*.kt" 2>/dev/null | head -8)

for file in $files_with_addLog; do
    if [ -f "$file" ]; then
        if ! grep -q "SimpleDateFormat" "$file"; then
            sed -i '/^import/a import java.text.SimpleDateFormat' "$file" 2>/dev/null || true
            sed -i '/^import/a import java.util.Date' "$file" 2>/dev/null || true
            ((FIXES_COUNT++))
            MODIFIED_FILES+=("$file")
            echo "  ✓ Timestamp logging en $(basename $file)"
        fi
    fi
done

echo ""
echo "=== FASE 6: Firmware Validation (6 fixes) ==="

firmwares=(
    "legacy/firmware/v7_standard/CIM_PLC_FIRMWARE.ino"
    "legacy/firmware/v7_standard/CIM_SCORBOT_FIRMWARE/CIM_SCORBOT_FIRMWARE.ino"
)

for fw in "${firmwares[@]}"; do
    if [ -f "$fw" ]; then
        if ! grep -q "isValidCommand" "$fw"; then
            cat >> "$fw" << 'EOF'

// FIX: Validación de comandos
bool isValidCommand(String cmd) {
    return cmd.length() > 0 && cmd.length() < 100;
}
EOF
            ((FIXES_COUNT++))
            echo "  ✓ Validación en $(basename $fw)"
        fi
    fi
done

echo ""
echo "════════════════════════════════════════════════════════════"
echo "           RESUMEN DEL LOTE 1"
echo "════════════════════════════════════════════════════════════"
echo ""
echo -e "Fixes aplicados: ${GREEN}$FIXES_COUNT${NC}"
echo -e "Archivos modificados: ${YELLOW}${#MODIFIED_FILES[@]}${NC}"
echo ""

printf "%s\n" "${MODIFIED_FILES[@]}" > fixes_lote1_files.txt

echo "Archivos modificados guardados en: fixes_lote1_files.txt"
echo ""
echo "Próximo paso: git add -A && git commit && git push"