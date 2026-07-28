/*
 * CIM_PLC_FIRMWARE.ino - v7.1
 * Firmware para Estación PLC (Cinta + Sensores)
 */

#define DEVICE_NAME "CIM_PLC_MASTER"
#define BLE_NAME "CIM_PLC_04"

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

#define MOTOR_RELAY_PIN 5
#define PROXIMITY_SENSOR_PIN 34

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
  pinMode(MOTOR_RELAY_PIN, OUTPUT);
  pinMode(PROXIMITY_SENSOR_PIN, INPUT);
  digitalWrite(MOTOR_RELAY_PIN, LOW);

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
  Serial.println("CIM PLC Firmware listo");
}

void loop() {
  // Simulación de sensor
  if (digitalRead(PROXIMITY_SENSOR_PIN) == HIGH) {
    if (deviceConnected) {
      String msg = "SENSOR_ACTIVATED|POS:5";
      pTxCharacteristic->setValue(msg.c_str());
      pTxCharacteristic->notify();
    }
    delay(2000);
  }
  delay(100);
}

void handleCommand(String cmd) {
  cmd.trim();
  if (cmd == "PLC:START") {
    digitalWrite(MOTOR_RELAY_PIN, HIGH);
    sendResponse("PLC:START;OK");
  } else if (cmd == "PLC:STOP") {
    digitalWrite(MOTOR_RELAY_PIN, LOW);
    sendResponse("PLC:STOP;OK");
  } else if (cmd.startsWith("C:DELIVER")) {
    sendResponse("DELIVER;OK");
  } else if (cmd.startsWith("C:STOP")) {
    sendResponse("STOP;OK");
  } else if (cmd.startsWith("C:FREE")) {
    sendResponse("FREE;OK");
  } else {
    sendResponse("UNKNOWN");
  }
}

void sendResponse(String response) {
  if (deviceConnected && pTxCharacteristic != NULL) {
    pTxCharacteristic->setValue(response.c_str());
    pTxCharacteristic->notify();
  }
}