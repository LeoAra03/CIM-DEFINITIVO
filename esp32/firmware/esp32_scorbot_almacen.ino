#include <Arduino.h>
#include <BluetoothSerial.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define DEVICE_NAME "CIM_SCORBOT_ALM"
#define SERVICE_UUID "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHAR_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHAR_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

BluetoothSerial SerialBT;
BLEServer *pServer = nullptr;
BLECharacteristic *pTxCharacteristic = nullptr;
BLECharacteristic *pRxCharacteristic = nullptr;

bool deviceConnected = false;
String incomingCommand = "";

class BLECallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) override {
    deviceConnected = true;
    Serial.println("BLE connected");
  }

  void onDisconnect(BLEServer* pServer) override {
    deviceConnected = false;
    Serial.println("BLE disconnected");
  }
};

class RxCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) override {
    String value = pCharacteristic->getValue();
    if (value.length() > 0) {
      incomingCommand = value;
      Serial.print("RX: ");
      Serial.println(incomingCommand);
    }
  }
};

void sendStatus(const String &text) {
  if (deviceConnected && pTxCharacteristic) {
    pTxCharacteristic->setValue(text.c_str());
    pTxCharacteristic->notify();
  }
  if (Serial) {
    Serial.print("TX: ");
    Serial.println(text);
  }
}

void handleCommand(const String &command) {
  if (command.startsWith("STO:")) {
    sendStatus("ACTUATOR: STORE AT " + command.substring(4));
  } else if (command.startsWith("R:")) {
    sendStatus("ACTUATOR: " + command);
  } else {
    sendStatus("RECEIVED: " + command);
  }
}

void setupBLE() {
  BLEDevice::init(DEVICE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new BLECallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = pService->createCharacteristic(CHAR_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
  pTxCharacteristic->addDescriptor(new BLE2902());
  pRxCharacteristic = pService->createCharacteristic(CHAR_UUID_RX, BLECharacteristic::PROPERTY_WRITE);
  pRxCharacteristic->setCallbacks(new RxCallbacks());

  pService->start();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->start();

  Serial.println("BLE service started");
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println(DEVICE_NAME);
  SerialBT.begin(DEVICE_NAME);
  setupBLE();
}

void loop() {
  if (SerialBT.available()) {
    String command = SerialBT.readStringUntil('\n');
    command.trim();
    if (command.length() > 0) {
      handleCommand(command);
    }
  }

  if (incomingCommand.length() > 0) {
    handleCommand(incomingCommand);
    incomingCommand = "";
  }

  delay(50);
}
