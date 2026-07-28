/*
 * CIM INDUSTRIAL SYSTEM - SCORBOT STANDARDIZED FIRMWARE v7.1
 * Corregido para compatibilidad con ESP32 Core 3.0+
 * Para placa: Wemos D1 R32
 */

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// CAMBIAR SEGÚN LA ESTACIÓN: "CIM_SCORBOT_MAN", "CIM_SCORBOT_CAL", o "CIM_SCORBOT_ALM"
#define DEVICE_NAME "CIM_SCORBOT_MAN"

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

BLEServer* pServer = NULL;
BLECharacteristic* pTxCharacteristic;
bool deviceConnected = false;
int statusLed = 2; // LED interno de la Wemos R32

void sendCimMessage(String cmdType, String payload) {
    // Formato de protocolo CIM v7.0
    String msg = "ESP32_R32|" + String(millis()) + "|AA:BB:CC:DD:EE:FF|PLC|||" + cmdType + "|NORMAL|SESSION_01|" + payload + "\n";
    pTxCharacteristic->setValue(msg.c_str());
    pTxCharacteristic->notify();
    Serial.println(">>> ENVIADO A APP: " + cmdType);
}

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      // CORRECCIÓN: Se obtiene el valor directamente como String de Arduino
      String data = pCharacteristic->getValue(); 
      
      if (data.length() > 0) {
        Serial.print("<<< RECIBIDO DE APP: "); 
        Serial.println(data);

        // Handshake: Si la App pregunta quién eres
        if (data.indexOf("IDENTIFY") != -1) {
            sendCimMessage("IDENTIFIED", "ROBOT_ARM|1.0");
        }

        // Comandos de Robot Scorbot
        if (data.indexOf("R:HOME") != -1) {
            Serial.println("ROBOT: Ejecutando HOME...");
            blink(3); // Feedback visual
        }

        if (data.indexOf("R:MOVE") != -1) {
            Serial.println("ROBOT: Moviendo ejes...");
            digitalWrite(statusLed, HIGH); 
            delay(500); 
            digitalWrite(statusLed, LOW);
        }
      }
    }

    void blink(int times) {
        for(int i=0; i<times; i++) {
            digitalWrite(statusLed, HIGH); delay(150);
            digitalWrite(statusLed, LOW); delay(150);
        }
    }
};

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) { 
        deviceConnected = true; 
        Serial.println("¡Android Conectado!"); 
    }
    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.println("Android Desconectado. Reiniciando visibilidad...");
        pServer->getAdvertising()->start();
    }
};

void setup() {
  Serial.begin(115200);
    Serial.println("DEVICE: " + String(DEVICE_NAME));
  pinMode(statusLed, OUTPUT);
  digitalWrite(statusLed, LOW);

  // Inicializar Bluetooth con el nombre del dispositivo
  BLEDevice::init(DEVICE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Característica para enviar datos (TX)
  pTxCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID_TX,
                        BLECharacteristic::PROPERTY_NOTIFY
                      );
  pTxCharacteristic->addDescriptor(new BLE2902());

  // Característica para recibir datos (RX)
  BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID_RX,
                                         BLECharacteristic::PROPERTY_WRITE
                                       );
  pRxCharacteristic->setCallbacks(new MyCallbacks());

  pService->start();
  pServer->getAdvertising()->start();
  
  Serial.println("========================================");
  Serial.println("SISTEMA CIM v7.1 - FIRMWARE SCORBOT");
  Serial.print("Dispositivo: "); Serial.println(DEVICE_NAME);
  Serial.println("Esperando conexión de la App...");
  Serial.println("========================================");
}

void loop() {
  delay(10);
}
// FIX: Validación de comandos
bool isValidCommand(String cmd) {
    return cmd.length() > 0 && cmd.length() < 100;
}
