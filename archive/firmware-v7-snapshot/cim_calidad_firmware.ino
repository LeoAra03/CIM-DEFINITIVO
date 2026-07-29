/*
 * CIM_CALIDAD_FIRMWARE.ino - v7.1
 * Firmware para Estación de Calidad (ArUco + YOLO simulado)
 */

#define DEVICE_NAME "CIM_CALIDAD_VISION"
#define BLE_NAME "CIM_QUALITY_03"

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID        "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

BLEServer* pServer = NULL;
BLECharacteristic* pTxCharacteristic = NULL;
bool deviceConnected = false;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) { deviceConnected = true; }
    void onDisconnect(BLEServer* pServer) { deviceConnected = false; }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
      if (rxValue.length() > 0) {
        String cmd = String(rxValue.c_str());
        handleCommand(cmd);
      }
    }
};

void setup() {
  Serial.begin(115200);
  BLEDevice::init(BLE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE);
  pRxCharacteristic->setCallbacks(new MyCallbacks());

  pService->start();
  pServer->getAdvertising()->start();
  Serial.println("CIM Calidad Firmware listo - Modo simulado");
}

void loop() {
  delay(200);
}

void handleCommand(String cmd) {
  cmd.trim();
  if (cmd.startsWith("ARUCO:DETECT")) {
    sendResponse("ARUCO_DETECTED:ID=42");
  } else if (cmd.startsWith("YOLO:DETECT")) {
    sendResponse("YOLO_DETECTED:CLASS=pieza");
  } else if (cmd == "VAL:PASS") {
    sendResponse("VAL:PASS;OK");
  } else if (cmd == "VAL:FAIL") {
    sendResponse("VAL:FAIL;OK");
  } else if (cmd.startsWith("ARUCO:GEN")) {
    sendResponse("ARUCO_GENERATED:ID=42");
  } else {
    sendResponse("ACK");
  }
}

void sendResponse(String response) {
  if (deviceConnected && pTxCharacteristic != NULL) {
    pTxCharacteristic->setValue(response.c_str());
    pTxCharacteristic->notify();
  }
}