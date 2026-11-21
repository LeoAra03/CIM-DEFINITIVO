#!/usr/bin/env bash
# One-shot migration from the nested android/apps tree to the flattened
# android/<module>/src layout. Run once from the repository root, then
# delete this script or keep it only as historical reference.
set -euo pipefail

cd "$(dirname "$0")/.."

MODULES=(
  app-coordinador
  app-plc
  app-manufactura
  app-calidad
  app-almacen
  wear-coordinador
)

for module in "${MODULES[@]}"; do
  source_dir="android/apps/${module}/app/src"
  target_dir="android/${module}/src"
  if [[ -d "${source_dir}" ]]; then
    mkdir -p "android/${module}"
    git mv "${source_dir}" "${target_dir}"
    git mv "android/apps/${module}/app/build.gradle.kts" "android/${module}/build.gradle.kts"
    git rm -r "android/apps/${module}/app" "android/apps/${module}/gradle" 2>/dev/null || true
    git rm -f "android/apps/${module}/.gitignore" 2>/dev/null || true
  fi
  if [[ -d "${target_dir}" ]]; then
    find "${target_dir}" -type d -name java | while read -r dir; do
      git mv "${dir}" "${dir%/java}/kotlin"
    done
  fi
done

# core-network: java -> kotlin source roots.
find android/core-network/src -type d -name java | while read -r dir; do
  git mv "${dir}" "${dir%/java}/kotlin"
done

# Flatten the network packages into com.industria.prefecto.
mkdir -p android/core-network/src/main/kotlin/com/industria/prefecto
mkdir -p android/core-network/src/test/kotlin/com/industria/prefecto
find android/core-network/src/main/kotlin/com/sistema -name '*.kt' \
  -exec git mv {} android/core-network/src/main/kotlin/com/industria/prefecto/ \;
find android/core-network/src/test/kotlin/com/sistema -name '*.kt' \
  -exec git mv {} android/core-network/src/test/kotlin/com/industria/prefecto/ \;
rm -rf android/core-network/src/main/kotlin/com/sistema \
       android/core-network/src/test/kotlin/com/sistema

# Package rename across all Kotlin sources (longest prefixes first).
find android -name '*.kt' -print0 | xargs -0 sed -i \
  -e 's/com\.sistema\.distribuido\.network\.protocol\./com.industria.prefecto./g' \
  -e 's/com\.sistema\.distribuido\.network\.prefecto\./com.industria.prefecto./g' \
  -e 's/com\.sistema\.distribuido\.network\.di\./com.industria.prefecto./g' \
  -e 's/com\.sistema\.distribuido\.network\b/com.industria.prefecto/g' \
  -e 's/com\.sistema\.distribuido\.coordinador\b/com.industria.coordinacion/g' \
  -e 's/^package com\.industria\.prefecto\.protocol$/package com.industria.prefecto/' \
  -e 's/^package com\.industria\.prefecto\.di$/package com.industria.prefecto/' \
  -e 's/^package com\.industria\.prefecto\.prefecto$/package com.industria.prefecto/'

# Firmware moves to the root firmware/ tree.
mkdir -p firmware/shared firmware/plc-master/src firmware/scorbot/src \
         firmware/vision-node/src firmware/rack-node/src
if [[ -d esp32/firmware ]]; then
  git mv esp32/firmware/esp32_plc_master.ino firmware/plc-master/src/plc_master.ino
  git mv esp32/firmware/esp32_scorbot_manufactura.ino firmware/scorbot/src/scorbot.ino
  git mv esp32/firmware/esp32_scorbot_calidad.ino firmware/vision-node/src/vision_node.ino
  git mv esp32/firmware/esp32_scorbot_almacen.ino firmware/rack-node/src/rack_node.ino
  git mv esp32/firmware/cim_ble_firmware.h firmware/shared/cim_ble_firmware.h
  git rm -r esp32
fi
for sketch in firmware/*/src/*.ino; do
  sed -i 's|#include "cim_ble_firmware.h"|#include "../../shared/cim_ble_firmware.h"|' "${sketch}"
done

# Single wrapper under config/.
git rm -r config/gradle-wrapper 2>/dev/null || true

echo "Layout migration complete. Review the result with git status and run:"
echo "  python3 tools/validate_system.py"
