/*
 * CIM Calidad - Wemos D1 ESP32 R32
 */
#define DEVICE_NAME "CIM_SCORBOT_CAL"
#define STATION_TYPE "QUALITY_STATION"
#include "cim_ble_firmware.h"

void setup() {
  Serial.begin(115200);
  cimBleSetup();
}

void loop() {
  cimBleLoop();
}
