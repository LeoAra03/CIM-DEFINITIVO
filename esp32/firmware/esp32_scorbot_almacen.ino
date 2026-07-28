/*
 * CIM Almacen - Wemos D1 ESP32 R32
 */
#define DEVICE_NAME "CIM_SCORBOT_ALM"
#define STATION_TYPE "STORAGE_STATION"
#define STATION_UUID "CIM-ST-ALM-X1"
#include "cim_ble_firmware.h"

void setup() {
  Serial.begin(115200);
  cimBleSetup();
}

void loop() {
  cimBleLoop();
}
