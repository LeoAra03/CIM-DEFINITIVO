/*
 * CIM_ALMACEN_FIRMWARE.ino - v7.1
 * Firmware para Estación de Almacenamiento (Rack 3x6)
 */

#define DEVICE_NAME "CIM_ALMACEN_RACK"
#define BLE_NAME "CIM_STORAGE_05"

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
  Serial.println("CIM Almacén Firmware listo (Rack 3x6 = 18 posiciones)");
}

void loop() {
  delay(300);
}

void handleCommand(String cmd) {
  cmd.trim();
  if (cmd.startsWith("STO:")) {
    // STO:07
    String pos = cmd.substring(4);
    sendResponse("STO:" + pos + ";OK");
  } else if (cmd.startsWith("R:RUN STORE")) {
    sendResponse("STORE;COMPLETED");
  } else if (cmd.startsWith("R:RUN RETRIEVE")) {
    sendResponse("RETRIEVE;COMPLETED");
  } else if (cmd == "R:HOME") {
    sendResponse("HOME;OK");
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