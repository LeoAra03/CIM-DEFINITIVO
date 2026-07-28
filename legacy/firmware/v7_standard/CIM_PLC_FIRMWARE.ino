/*
 * CIM INDUSTRIAL SYSTEM - PLC (CONVEYOR) STANDARDIZED FIRMWARE v7.0
 * For Wemos D1 R32 (ESP32)
 */

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define DEVICE_NAME "CIM_PLC_MASTER"

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

BLEServer* pServer = NULL;
BLECharacteristic* pTxCharacteristic;
int relayPin = 5; // Example relay for conveyor motor
int sensorPin = 34; // Example proximity sensor

void sendCimMessage(String cmdType, String payload) {
    String msg = "ESP32_PLC|" + String(millis()) + "|AA:BB:CC:DD:EE:FF|PLC|||" + cmdType + "|NORMAL|SESSION_01|" + payload + "\n";
    pTxCharacteristic->setValue(msg.c_str());
    pTxCharacteristic->notify();
}

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
      if (rxValue.length() > 0) {
        String data = "";
        for (int i = 0; i < rxValue.length(); i++) data += rxValue[i];
        Serial.print("<<< PLC CMD: "); Serial.println(data);

        if (data.indexOf("IDENTIFY") != -1) {
            sendCimMessage("IDENTIFIED", "PLC_CONTROLLER|1.0");
        }

        if (data.indexOf("PLC:START") != -1) {
            digitalWrite(relayPin, HIGH);
            Serial.println("CONVEYOR: RUNNING");
        }

        if (data.indexOf("PLC:STOP") != -1) {
            digitalWrite(relayPin, LOW);
            Serial.println("CONVEYOR: STOPPED");
        }
      }
    }
};

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) { Serial.println("PLC Connected"); }
    void onDisconnect(BLEServer* pServer) { pServer->getAdvertising()->start(); }
};

void setup() {
  Serial.begin(115200);
    Serial.println("DEVICE: " + String(DEVICE_NAME));
  pinMode(relayPin, OUTPUT);
  digitalWrite(relayPin, LOW);
  pinMode(sensorPin, INPUT);

  BLEDevice::init(DEVICE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
  pTxCharacteristic->addDescriptor(new BLE2902());
  BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE);
  pRxCharacteristic->setCallbacks(new MyCallbacks());
  pService->start();
  pServer->getAdvertising()->start();
  Serial.println("PLC CONVEYOR READY.");
}

void loop() {
    // Basic sensor logic
    if (digitalRead(sensorPin) == HIGH) {
        // notify app about sensor
        static unsigned long lastSent = 0;
        if (millis() - lastSent > 2000) {
            sendCimMessage("STATUS_RESPONSE", "SENSOR_TRIPPED");
            lastSent = millis();
        }
    }
    delay(100);
}

// FIX #144: Validación de posición STO
bool isValidPosition(int pos) {
    return pos >= 1 && pos <= 18;
}

// FIX #144: Validación de posición en comandos STO
if (cmd.startsWith("STO:")) {
    int pos = cmd.substring(4).toInt();
    if (pos < 1 || pos > 18) {
        sendCimMessage("STO", "ERROR:INVALID_POSITION");
        return;
    }
}
