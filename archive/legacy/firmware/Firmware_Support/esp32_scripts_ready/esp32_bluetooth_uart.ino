/*
  ESP32 BLE UART example (plantilla)
  - Recibe líneas desde BLE Serial
  - Interpreta mensajes tipo CIM y escribe respuesta

  NOTA: Este es un sketch de ejemplo, adaptar pines y lógica de ejecución.
*/

#include <Arduino.h>
#include "BLEDevice.h"
#include "BLEUtils.h"
#include "BLEServer.h"

static BLECharacteristic *pCharacteristic;
bool deviceConnected = false;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

void setup() {
  Serial.begin(115200);
  BLEDevice::init("ESP32-CIM-PLC");
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
  pCharacteristic = pService->createCharacteristic(
                      "6E400003-B5A3-F393-E0A9-E50E24DCCA9E",
                      BLECharacteristic::PROPERTY_NOTIFY
                    );

  pService->start();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
  pAdvertising->start();

  Serial.println("BLE UART started. Esperando comandos CIM...");
}

void loop() {
  // Ejemplo: leer desde Serial (USB) y simular recepción
  if (Serial.available()) {
    String line = Serial.readStringUntil('\n');
    line.trim();
    if (line.length() > 0) {
      Serial.println(String("Recibido: ") + line);
      // Aquí parsear CIM y ejecutar comandos de salida (pines, motores)
      // Simular ACK por notificación
      if (deviceConnected) {
        pCharacteristic->setValue((uint8_t*)line.c_str(), line.length());
        pCharacteristic->notify();
      }
    }
  }

  delay(50);
}
