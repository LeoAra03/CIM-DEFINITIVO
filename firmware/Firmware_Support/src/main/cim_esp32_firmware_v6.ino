#include <Arduino.h>
#include <WiFi.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <BluetoothSerial.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/queue.h>
#include <freertos/semphr.h>
#include <string>

/*
 * FIRMWARE INDUSTRIAL CIM v6.0
 * ESP32 con multitarea FreeRTOS, BLE + SPP y flujo de autorización.
 */

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error "Bluetooth no habilitado. Verifique sdkconfig / build_flags."
#endif

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
#define LOG_TAG_BLE "[BLE]"
#define LOG_TAG_SPP "[SPP]"
#define LOG_TAG_CMD "[CMD]"
#define MAX_MESSAGE_LEN 256
#define COMM_QUEUE_SIZE 12
#define ACT_QUEUE_SIZE 8

enum AuthState {
  AUTH_STATE_UNVERIFIED,
  AUTH_STATE_AUTHORIZED,
  AUTH_STATE_REJECTED
};

enum CommSource {
  SOURCE_BLE,
  SOURCE_SPP,
  SOURCE_SERIAL
};

struct CimFrame {
  char payload[MAX_MESSAGE_LEN];
  CommSource source;
};

BluetoothSerial SerialBT;
BLEServer* pServer = nullptr;
BLECharacteristic* pTxCharacteristic = nullptr;

QueueHandle_t commQueue = nullptr;
QueueHandle_t actuatorQueue = nullptr;
SemaphoreHandle_t authMutex = nullptr;
volatile AuthState authState = AUTH_STATE_UNVERIFIED;
char deviceNameBuf[64] = {0};
char deviceVersionBuf[32] = "6.0.0";

bool bleConnected = false;
bool sppClientConnected = false;

void sendBleTx(const char* msg);
void sendSppTx(const char* msg);
void forwardResponse(const char* msg);
void handleIncomingFrame(const char* rawMessage, CommSource source);
void processActuatorCommand(const char* command);
void reportStatus(const char* status);

class BleServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    bleConnected = true;
    Serial.println(LOG_TAG_BLE " Cliente BLE conectado");
  }

  void onDisconnect(BLEServer* server) override {
    bleConnected = false;
    Serial.println(LOG_TAG_BLE " Cliente BLE desconectado");
    BLEDevice::startAdvertising();
  }
};

class RxCharacteristicCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* characteristic) override {
    std::string rxValue = characteristic->getValue();
    if (rxValue.empty()) return;
    if (commQueue == nullptr) return;

    CimFrame frame{};
    strncpy(frame.payload, rxValue.c_str(), MAX_MESSAGE_LEN - 1);
    frame.source = SOURCE_BLE;
    xQueueSend(commQueue, &frame, 0);
  }
};

void sendBleTx(const char* msg) {
  if (bleConnected && pTxCharacteristic != nullptr) {
    pTxCharacteristic->setValue(msg);
    pTxCharacteristic->notify();
  }
}

void sendSppTx(const char* msg) {
  if (SerialBT.hasClient()) {
    SerialBT.println(msg);
  }
}

void forwardResponse(const char* msg) {
  Serial.print(LOG_TAG_CMD " TX: ");
  Serial.println(msg);
  sendBleTx(msg);
  sendSppTx(msg);
}

void reportStatus(const char* status) {
  char buffer[MAX_MESSAGE_LEN];
  snprintf(buffer, sizeof(buffer), "STATUS|%s|%s", deviceNameBuf, status);
  forwardResponse(buffer);
}

void sendIdentifiedResponse(const char* status) {
  char buffer[MAX_MESSAGE_LEN];
  snprintf(buffer, sizeof(buffer), "IDENTIFIED|%s|%s", status, deviceNameBuf);
  forwardResponse(buffer);
}

void sendAck(const char* id) {
  char buffer[MAX_MESSAGE_LEN];
  snprintf(buffer, sizeof(buffer), "ACK|%s|%s", id, deviceNameBuf);
  forwardResponse(buffer);
}

void sendNack(const char* id, const char* reason) {
  char buffer[MAX_MESSAGE_LEN];
  snprintf(buffer, sizeof(buffer), "NACK|%s|%s", id, reason);
  forwardResponse(buffer);
}

void parseAndDispatch(const char* message, CommSource source) {
  if (message == nullptr || strlen(message) == 0) return;

  if (strncmp(message, "IDENTIFY|", 9) == 0) {
    xSemaphoreTake(authMutex, portMAX_DELAY);
    authState = AUTH_STATE_UNVERIFIED;
    xSemaphoreGive(authMutex);
    sendIdentifiedResponse("PENDING");
    return;
  }

  if (strcmp(message, "AUTHORIZED") == 0) {
    xSemaphoreTake(authMutex, portMAX_DELAY);
    authState = AUTH_STATE_AUTHORIZED;
    xSemaphoreGive(authMutex);
    forwardResponse("ACK|AUTHORIZED|ESP32");
    return;
  }

  if (strcmp(message, "REJECTED") == 0) {
    xSemaphoreTake(authMutex, portMAX_DELAY);
    authState = AUTH_STATE_REJECTED;
    xSemaphoreGive(authMutex);
    forwardResponse("ACK|REJECTED|ESP32");
    return;
  }

  if (strncmp(message, "COMMAND;", 8) == 0) {
    xSemaphoreTake(authMutex, portMAX_DELAY);
    bool authorized = authState == AUTH_STATE_AUTHORIZED;
    xSemaphoreGive(authMutex);
    if (!authorized) {
      sendNack(message + 8, "NOT_AUTHORIZED");
      return;
    }
    CimFrame frame{};
    strncpy(frame.payload, message + 8, MAX_MESSAGE_LEN - 1);
    frame.source = source;
    xQueueSend(actuatorQueue, &frame, 0);
    return;
  }

  if (strncmp(message, "EXECUTE|", 8) == 0) {
    xSemaphoreTake(authMutex, portMAX_DELAY);
    bool authorized = authState == AUTH_STATE_AUTHORIZED;
    xSemaphoreGive(authMutex);
    if (!authorized) {
      sendNack(message, "NOT_AUTHORIZED");
      return;
    }
    CimFrame frame{};
    strncpy(frame.payload, message, MAX_MESSAGE_LEN - 1);
    frame.source = source;
    xQueueSend(actuatorQueue, &frame, 0);
    return;
  }

  // Comandos libres del firmware, se procesan de forma local
  processActuatorCommand(message);
}

void handleIncomingFrame(const char* rawMessage, CommSource source) {
  parseAndDispatch(rawMessage, source);
}

void processActuatorCommand(const char* command) {
  if (strncmp(command, "R:", 2) == 0) {
    sendAck(command);
    return;
  }
  if (strncmp(command, "L:", 2) == 0) {
    sendAck(command);
    return;
  }
  if (strncmp(command, "STO:", 4) == 0) {
    sendAck(command);
    return;
  }
  if (strncmp(command, "CAM:", 4) == 0) {
    sendAck(command);
    return;
  }
  if (strncmp(command, "FREE:", 5) == 0) {
    sendAck(command);
    return;
  }
  if (strncmp(command, "CHK:", 4) == 0) {
    sendAck(command);
    return;
  }
  // TODO: agregar integración real con Scorbot, láser, cinta y sensores
  sendNack(command, "UNKNOWN_ACTOR");
}

void initBle() {
  uint8_t mac[6];
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(true);
  WiFi.macAddress(mac);
  snprintf(deviceNameBuf, sizeof(deviceNameBuf), "CIM_ESP32_%02X%02X%02X", mac[3], mac[4], mac[5]);

  BLEDevice::init(deviceNameBuf);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new BleServerCallbacks());

  BLEService* service = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = service->createCharacteristic(
      CHARACTERISTIC_UUID_TX,
      BLECharacteristic::PROPERTY_NOTIFY
  );
  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic* rxChar = service->createCharacteristic(
      CHARACTERISTIC_UUID_RX,
      BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR
  );
  rxChar->setCallbacks(new RxCharacteristicCallbacks());
  service->start();

  BLEAdvertising* advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(true);
  advertising->setMinPreferred(0x06);
  advertising->setMaxPreferred(0x12);
  BLEDevice::startAdvertising();

  Serial.print(LOG_TAG_BLE " Activo: ");
  Serial.println(deviceNameBuf);
}

void initSpp() {
  if (!SerialBT.begin(deviceNameBuf)) {
    Serial.println(LOG_TAG_SPP " Error al iniciar SPP");
    return;
  }
  Serial.println(LOG_TAG_SPP " Servidor SPP activo");
}

void commTask(void* pvParameters) {
  CimFrame frame;
  while (true) {
    if (xQueueReceive(commQueue, &frame, pdMS_TO_TICKS(100)) == pdPASS) {
      handleIncomingFrame(frame.payload, frame.source);
    }
    vTaskDelay(pdMS_TO_TICKS(20));
  }
}

void sppTask(void* pvParameters) {
  while (true) {
    if (SerialBT.hasClient() && SerialBT.available()) {
      String line = SerialBT.readStringUntil('\n');
      line.trim();
      if (line.length() > 0) {
        CimFrame frame{};
        strncpy(frame.payload, line.c_str(), MAX_MESSAGE_LEN - 1);
        frame.source = SOURCE_SPP;
        xQueueSend(commQueue, &frame, 0);
      }
    }
    vTaskDelay(pdMS_TO_TICKS(50));
  }
}

void actuatorTask(void* pvParameters) {
  CimFrame frame;
  while (true) {
    if (xQueueReceive(actuatorQueue, &frame, portMAX_DELAY) == pdPASS) {
      processActuatorCommand(frame.payload);
    }
  }
}

void heartbeatTask(void* pvParameters) {
  while (true) {
    xSemaphoreTake(authMutex, portMAX_DELAY);
    bool authorized = authState == AUTH_STATE_AUTHORIZED;
    xSemaphoreGive(authMutex);
    reportStatus(authorized ? "READY" : "WAITING_AUTH");
    vTaskDelay(pdMS_TO_TICKS(10000));
  }
}

void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.println("\n=== CIM ESP32 Firmware v6.0 (BLE + SPP) ===");

  commQueue = xQueueCreate(COMM_QUEUE_SIZE, sizeof(CimFrame));
  actuatorQueue = xQueueCreate(ACT_QUEUE_SIZE, sizeof(CimFrame));
  authMutex = xSemaphoreCreateMutex();

  initBle();
  initSpp();

  xTaskCreatePinnedToCore(commTask, "CommTask", 4096, nullptr, 2, nullptr, 1);
  xTaskCreatePinnedToCore(sppTask, "SppTask", 4096, nullptr, 2, nullptr, 1);
  xTaskCreatePinnedToCore(actuatorTask, "ActuatorTask", 4096, nullptr, 1, nullptr, 1);
  xTaskCreatePinnedToCore(heartbeatTask, "HeartbeatTask", 3072, nullptr, 1, nullptr, 1);

  delay(500);
  char identifyMsg[MAX_MESSAGE_LEN];
  snprintf(identifyMsg, sizeof(identifyMsg), "IDENTIFY|%s|%s", deviceNameBuf, deviceVersionBuf);
  forwardResponse(identifyMsg);
  Serial.println("[BOOT] Sistema listo");
}

void loop() {
  vTaskDelay(pdMS_TO_TICKS(1000));
}
