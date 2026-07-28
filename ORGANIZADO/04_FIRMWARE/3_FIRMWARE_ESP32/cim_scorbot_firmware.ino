/*
 * CIM_SCORBOT_FIRMWARE.ino - v7.1
 * Firmware para Robot Scorbot + Láser CNC
 * Dispositivo: Wemos D1 R32 (ESP32)
 */

#define DEVICE_NAME "CIM_SCORBOT_MAN"
#define BLE_NAME "CIM_ROBOT_01"

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
bool oldDeviceConnected = false;

String lastCommand = "";

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("BLE: Cliente conectado");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("BLE: Cliente desconectado");
    }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();

      if (rxValue.length() > 0) {
        String cmd = String(rxValue.c_str());
        lastCommand = cmd;
        handleCommand(cmd);
      }
    }
};

void setup() {
  Serial.begin(115200);
  Serial.println("CIM Scorbot Firmware v7.1 iniciando...");

  // Inicializar BLE
  BLEDevice::init(BLE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);

  pTxCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID_TX,
                        BLECharacteristic::PROPERTY_NOTIFY
                      );
  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(
                                          CHARACTERISTIC_UUID_RX,
                                          BLECharacteristic::PROPERTY_WRITE
                                        );
  pRxCharacteristic->setCallbacks(new MyCallbacks());

  pService->start();
  pServer->getAdvertising()->start();
  
  Serial.println("BLE listo. Esperando conexiones...");
  Serial.println("DEVICE_NAME: " + String(DEVICE_NAME));
}

void loop() {
  if (deviceConnected) {
    // Enviar estado periódico
    if (lastCommand.length() > 0) {
      String status = "STATUS;ROBOT;READY;" + lastCommand;
      pTxCharacteristic->setValue(status.c_str());
      pTxCharacteristic->notify();
      lastCommand = "";
    }
  }
  
  // Reconexión BLE
  if (!deviceConnected && oldDeviceConnected) {
    delay(500);
    pServer->startAdvertising();
    Serial.println("Reiniciando advertising BLE...");
    oldDeviceConnected = deviceConnected;
  }
  
  if (deviceConnected && !oldDeviceConnected) {
    oldDeviceConnected = deviceConnected;
  }
  
  delay(100);
}

void handleCommand(String cmd) {
  cmd.trim();
  Serial.println("CMD recibido: " + cmd);

  if (cmd.startsWith("R:HOME")) {
    executeHomeSequence();
  } else if (cmd.startsWith("R:MOVE")) {
    executeMoveCommand(cmd);
  } else if (cmd.startsWith("R:RUN")) {
    executeRunCommand(cmd);
  } else if (cmd.startsWith("L:START")) {
    startLaser();
  } else if (cmd.startsWith("L:STOP")) {
    stopLaser();
  } else if (cmd.startsWith("L:POWER")) {
    setLaserPower(cmd);
  } else if (cmd.startsWith("GCODE_LOAD")) {
    loadGCode(cmd);
  } else if (cmd == "STATUS") {
    sendStatus();
  } else {
    Serial.println("Comando no reconocido: " + cmd);
  }
}

void executeHomeSequence() {
  Serial.println("Ejecutando secuencia HOME...");
  // Simulación de movimiento del robot
  delay(800);
  sendResponse("R:HOME;OK");
}

void executeMoveCommand(String cmd) {
  // Formato: R:MOVE:x,y,z
  Serial.println("Moviendo robot: " + cmd);
  delay(600);
  sendResponse("R:MOVE;OK");
}

void executeRunCommand(String cmd) {
  Serial.println("Ejecutando RUN: " + cmd);
  delay(1200);
  sendResponse("R:RUN;COMPLETED");
}

void startLaser() {
  Serial.println("Láser iniciado");
  sendResponse("L:START;OK");
}

void stopLaser() {
  Serial.println("Láser detenido");
  sendResponse("L:STOP;OK");
}

void setLaserPower(String cmd) {
  // L:POWER:80
  int power = cmd.substring(8).toInt();
  Serial.println("Potencia láser ajustada a: " + String(power) + "%");
  sendResponse("L:POWER;" + String(power) + ";OK");
}

void loadGCode(String cmd) {
  Serial.println("Cargando G-code...");
  sendResponse("GCODE;LOADED");
}

void sendStatus() {
  sendResponse("STATUS;ROBOT;READY;POS:0,0,0");
}

void sendResponse(String response) {
  if (deviceConnected && pTxCharacteristic != NULL) {
    pTxCharacteristic->setValue(response.c_str());
    pTxCharacteristic->notify();
    Serial.println("Respuesta enviada: " + response);
  }
}