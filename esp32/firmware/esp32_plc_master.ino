/*
 * CIM PLC Cinta - Wemos D1 ESP32 R32
 * Relay GPIO5, Sensor GPIO34
 */
#define DEVICE_NAME "CIM_PLC_MASTER"
#define CIM_IS_PLC
#include "cim_ble_firmware.h"

void setup() {
  Serial.begin(115200);
  cimBleSetup();
}

void loop() {
  cimBleLoop();
}
