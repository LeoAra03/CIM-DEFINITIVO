/*
 * CIM Manufactura - Wemos D1 ESP32 R32
 * BLE Nordic UART + Scorbot Serial2 (GPIO16/17)
 */
#define DEVICE_NAME "CIM_SCORBOT_MAN"
#define STATION_TYPE "ROBOT_ARM"
#define CIM_HAS_SCORBOT
#include <BluetoothSerial.h>
#include "cim_ble_firmware.h"

void setup() {
  Serial.begin(115200);
  cimBleSetup();
}

void loop() {
  cimBleLoop();
}
