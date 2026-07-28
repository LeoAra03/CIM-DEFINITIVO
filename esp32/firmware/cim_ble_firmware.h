/*
 * CIM BLE Firmware - Wemos D1 ESP32 R32 (WROOM-32)
 * Placa: LED GPIO2, Scorbot Serial2 RX=16 TX=17 @ 9600 8N1
 * Protocolo: Nordic UART BLE + comandos CIM/Android planos
 *
 * Incluir desde cada .ino definiendo DEVICE_NAME y STATION_TYPE antes del include.
 */
#ifndef CIM_BLE_FIRMWARE_H
#define CIM_BLE_FIRMWARE_H

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#ifndef DEVICE_NAME
#define DEVICE_NAME "CIM_SCORBOT_MAN"
#endif

#ifndef STATION_TYPE
#define STATION_TYPE "ROBOT_ARM"
#endif

// Definir CIM_IS_PLC antes del include en firmware PLC
#ifdef CIM_IS_PLC
  static const char* const kStationType = "PLC_CONTROLLER";
#else
  static const char* const kStationType = STATION_TYPE;
#endif

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

// Wemos D1 ESP32 R32
static const int PIN_LED = 2;
static const int PIN_SCORBOT_RX = 16;
static const int PIN_SCORBOT_TX = 17;
static const int PIN_RELAY = 5;      // PLC conveyor
static const int PIN_SENSOR = 34;    // PLC proximity (input only)

static BLEServer* pServer = NULL;
static BLECharacteristic* pTxCharacteristic = NULL;
static bool deviceConnected = false;
static String rxBuffer = "";

static void blinkLed(int times, int ms = 150) {
  for (int i = 0; i < times; i++) {
    digitalWrite(PIN_LED, HIGH);
    delay(ms);
    digitalWrite(PIN_LED, LOW);
    delay(ms);
  }
}

// Android starts with a 23-byte ATT MTU (20 payload bytes). Fragment responses
// explicitly; Android reassembles them using the newline terminator.
static const size_t BLE_NOTIFY_CHUNK_SIZE = 20;

static void sendBleResponse(const String& payload) {
  if (!deviceConnected || pTxCharacteristic == NULL) return;
  const String msg = String(kStationType) + "|" + String(millis()) + "|RESP|" + payload + "\n";
  for (size_t offset = 0; offset < msg.length(); offset += BLE_NOTIFY_CHUNK_SIZE) {
    const String chunk = msg.substring(offset, offset + BLE_NOTIFY_CHUNK_SIZE);
    pTxCharacteristic->setValue(chunk.c_str());
    pTxCharacteristic->notify();
    // Give the BLE stack time to enqueue each notification on a Wemos D1 R32.
    delay(8);
  }
  Serial.print(">>> TX: ");
  Serial.println(payload);
}

static void forwardToScorbot(const String& cmd) {
  Serial2.println(cmd);
  Serial.print(">>> SCORBOT: ");
  Serial.println(cmd);
}

static String extractPayload(const String& data) {
  int lastPipe = data.lastIndexOf('|');
  if (lastPipe >= 0 && data.indexOf("EXECUTE") >= 0) {
    return data.substring(lastPipe + 1);
  }
  return data;
}

static void handleCommand(String raw) {
  raw.trim();
  if (raw.length() == 0) return;

  String data = extractPayload(raw);
  Serial.print("<<< CMD: ");
  Serial.println(data);

  if (data.indexOf("IDENTIFY") >= 0) {
    sendBleResponse("IDENTIFIED|" + String(kStationType) + "|1.0");
    blinkLed(2);
    return;
  }

  if (data.startsWith("R:")) {
    if (data == "R:HOME") {
      forwardToScorbot("HOME");
      sendBleResponse("ACTUATOR:HOME");
      blinkLed(3);
    } else if (data == "R:READY") {
      forwardToScorbot("HERE");
      sendBleResponse("ACTUATOR:READY");
    } else if (data.startsWith("R:MOVE:")) {
      forwardToScorbot(data.substring(2));
      sendBleResponse("ACTUATOR:" + data.substring(2));
      digitalWrite(PIN_LED, HIGH);
      delay(200);
      digitalWrite(PIN_LED, LOW);
    } else if (data.startsWith("R:RUN")) {
      String prog = data.substring(6);
      prog.trim();
      forwardToScorbot("RUN " + prog);
      sendBleResponse("ACTUATOR:RUN " + prog);
    } else if (data == "R:AUTO") {
      forwardToScorbot("AUTO");
      sendBleResponse("ACTUATOR:AUTO");
    } else if (data == "R:SAVE") {
      forwardToScorbot("SAVE");
      sendBleResponse("ACTUATOR:SAVE");
    } else if (data == "R:DISCARD") {
      sendBleResponse("ACTUATOR:DISCARD");
    } else {
      forwardToScorbot(data.substring(2));
      sendBleResponse("ACTUATOR:" + data.substring(2));
    }
    return;
  }

  if (data.startsWith("L:")) {
    if (data.startsWith("L:ARUCO:")) {
      sendBleResponse("LASER:ARUCO:" + data.substring(8));
      blinkLed(1, 50);
    } else if (data == "L:START") {
      forwardToScorbot("LASER START");
      sendBleResponse("LASER:START");
    } else if (data == "L:STOP") {
      forwardToScorbot("LASER STOP");
      sendBleResponse("LASER:STOP");
    } else if (data.startsWith("L:POWER:")) {
      sendBleResponse("LASER:POWER:" + data.substring(8));
    } else if (data.startsWith("L:SPEED:")) {
      sendBleResponse("LASER:SPEED:" + data.substring(8));
    } else {
      sendBleResponse("LASER:" + data.substring(2));
    }
    return;
  }

  if (data.startsWith("PLC:")) {
    if (data.indexOf("START") >= 0) {
      digitalWrite(PIN_RELAY, HIGH);
      sendBleResponse("PLC:RUNNING");
    } else if (data.indexOf("STOP") >= 0) {
      digitalWrite(PIN_RELAY, LOW);
      sendBleResponse("PLC:STOPPED");
    }
    return;
  }

  if (data.startsWith("CAM:") || data.startsWith("VAL:") || data.startsWith("STO:")) {
    sendBleResponse("RECEIVED:" + data);
    return;
  }

  sendBleResponse("ACK:" + data);
}

class CimBleServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    deviceConnected = true;
    Serial.println("BLE: Android conectado");
    blinkLed(1);
  }
  void onDisconnect(BLEServer* server) override {
    deviceConnected = false;
    Serial.println("BLE: Desconectado, re-anunciando...");
    server->getAdvertising()->start();
  }
};

class CimBleRxCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) override {
    String value = pCharacteristic->getValue();
    if (value.length() == 0) return;
    rxBuffer += value;
    int nl;
    while ((nl = rxBuffer.indexOf('\n')) >= 0) {
      String line = rxBuffer.substring(0, nl);
      rxBuffer = rxBuffer.substring(nl + 1);
      handleCommand(line);
    }
  }
};

static void cimBleSetup() {
  pinMode(PIN_LED, OUTPUT);
  digitalWrite(PIN_LED, LOW);

#ifdef CIM_IS_PLC
  pinMode(PIN_RELAY, OUTPUT);
  digitalWrite(PIN_RELAY, LOW);
  pinMode(PIN_SENSOR, INPUT);
#else
  Serial2.begin(9600, SERIAL_8N1, PIN_SCORBOT_RX, PIN_SCORBOT_TX);
#endif

  BLEDevice::init(DEVICE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new CimBleServerCallbacks());

  BLEService* pService = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID_TX,
    BLECharacteristic::PROPERTY_NOTIFY
  );
  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic* pRxCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID_RX,
    BLECharacteristic::PROPERTY_WRITE
  );
  pRxCharacteristic->setCallbacks(new CimBleRxCallbacks());

  pService->start();

  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->start();

  Serial.println("========================================");
  Serial.println("CIM BLE Firmware - Wemos D1 ESP32 R32");
  Serial.print("Dispositivo: ");
  Serial.println(DEVICE_NAME);
  Serial.print("Estacion: ");
  Serial.println(kStationType);
  Serial.println("Esperando conexion Android...");
  Serial.println("========================================");
}

static void cimBleLoop() {
#ifdef CIM_IS_PLC
  static unsigned long lastSensor = 0;
  if (digitalRead(PIN_SENSOR) == HIGH && millis() - lastSensor > 2000) {
    sendBleResponse("SENSOR:TRIPPED");
    lastSensor = millis();
  }
#else
  while (Serial2.available()) {
    String line = Serial2.readStringUntil('\n');
    line.trim();
    if (line.length() > 0) {
      sendBleResponse("SCORBOT:" + line);
    }
  }
#endif
  delay(10);
}

#endif
