#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="../output-apks"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if command -v platformio >/dev/null 2>&1; then
  echo "[FW] PlatformIO detected. Building..."
  platformio run
  if [ $? -eq 0 ]; then
    echo "[FW] PlatformIO build successful"
    mkdir -p "$OUT_DIR"
    # Copy firmware bin(s) from .pio/build
    find . -path "./.pio/build/*/firmware.bin" -exec cp {} "$OUT_DIR/" \;
  else
    echo "[FW] PlatformIO build failed"
    exit 1
  fi
elif command -v arduino-cli >/dev/null 2>&1; then
  echo "[FW] arduino-cli detected. Attempting build..."
  TMP_SKETCH="/tmp/cim_tmp_sketch"
  rm -rf "$TMP_SKETCH" || true
  mkdir -p "$TMP_SKETCH"
  cp src/main/cim_esp32_firmware_v6.ino "$TMP_SKETCH/cim_esp32_firmware_v6.ino"
  cd "$TMP_SKETCH"
  arduino-cli compile --fqbn esp32:esp32:esp32
  cd -
else
  echo "[FW] No PlatformIO or arduino-cli found. Generating placeholder .bin"
  mkdir -p "$OUT_DIR"
  timestamp=$(date +"%Y%m%d_%H%M%S")
  outFile="$OUT_DIR/cim_esp32_firmware_v6_${timestamp}.bin"
  cat src/main/cim_esp32_firmware_v6.ino > "$outFile"
  echo "[FW] Firmware placeholder created at: $outFile"
fi

echo "[FW] Done."

