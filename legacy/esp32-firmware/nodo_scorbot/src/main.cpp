#include <Arduino.h>
#include <BluetoothSerial.h>
#include <WiFi.h>
#include <esp_now.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>
#include <string.h>

#define DEBUG_TAG "[SCORBOT-ESP32]"
#define MAX_MSG_LEN 160
#define QUEUE_SIZE 16

BluetoothSerial SerialBT;

struct CimMessage {
    char origin[16];
    char destination[16];
    char command[24];
    char payload[96];
};

QueueHandle_t messageQueue = nullptr;

String buildAclCommand(const String& cmd) {
    return cmd + "\r";
}

void debugLog(const String& msg) {
    Serial.println(msg);
}

void clearMessage(CimMessage& msg) {
    memset(&msg, 0, sizeof(msg));
}

String serializeMessage(const CimMessage& msg) {
    return String(msg.origin) + "|" + String(msg.destination) + "|" + String(msg.command) + "|" + String(msg.payload);
}

bool deserializeMessage(const String& raw, CimMessage& msg) {
    clearMessage(msg);
    int part1 = raw.indexOf('|');
    int part2 = raw.indexOf('|', part1 + 1);
    int part3 = raw.indexOf('|', part2 + 1);

    if (part1 < 0 || part2 < 0 || part3 < 0) {
        return false;
    }

    String origin = raw.substring(0, part1);
    String destination = raw.substring(part1 + 1, part2);
    String command = raw.substring(part2 + 1, part3);
    String payload = raw.substring(part3 + 1);

    origin.toCharArray(msg.origin, sizeof(msg.origin));
    destination.toCharArray(msg.destination, sizeof(msg.destination));
    command.toCharArray(msg.command, sizeof(msg.command));
    payload.toCharArray(msg.payload, sizeof(msg.payload));
    return true;
}

void sendToBluetooth(const CimMessage& msg) {
    String frame = serializeMessage(msg);
    SerialBT.println(frame);
    debugLog(String(DEBUG_TAG) + " BT TX -> " + frame);
}

void sendToEspNow(const CimMessage& msg) {
    String frame = serializeMessage(msg);
    esp_now_send(nullptr, reinterpret_cast<const uint8_t*>(frame.c_str()), frame.length());
    debugLog(String(DEBUG_TAG) + " ESP-NOW TX -> " + frame);
}

void forwardToScorbot(const String& command) {
    String aclCommand = buildAclCommand(command);
    Serial2.print(aclCommand);
    Serial2.flush();
    debugLog(String(DEBUG_TAG) + " Serial2 TX -> " + aclCommand);
}

void onDataRecv(const uint8_t* macAddr, const uint8_t* data, int len) {
    char buffer[MAX_MSG_LEN + 1] = {0};
    memcpy(buffer, data, min(len, MAX_MSG_LEN));

    CimMessage incoming{};
    if (!deserializeMessage(String(buffer), incoming)) {
        debugLog(String(DEBUG_TAG) + " ESP-NOW frame inválido");
        return;
    }

    if (messageQueue != nullptr) {
        xQueueSend(messageQueue, &incoming, portMAX_DELAY);
    }
}

void onDataSent(const uint8_t* macAddr, esp_now_send_status_t status) {
    if (status != ESP_NOW_SEND_SUCCESS) {
        debugLog(String(DEBUG_TAG) + " ESP-NOW delivery failed");
    }
}

void bluetoothTask(void*) {
    while (true) {
        if (SerialBT.hasClient() && SerialBT.available()) {
            String line = SerialBT.readStringUntil('\n');
            line.trim();
            if (line.length() > 0) {
                CimMessage incoming{};
                if (deserializeMessage(line, incoming)) {
                    strncpy(incoming.origin, "BLUETOOTH", sizeof(incoming.origin) - 1);
                    if (messageQueue != nullptr) {
                        xQueueSend(messageQueue, &incoming, portMAX_DELAY);
                    }
                    debugLog(String(DEBUG_TAG) + " BT RX <- " + line);
                }
            }
        }
        vTaskDelay(pdMS_TO_TICKS(25));
    }
}

void routingTask(void*) {
    CimMessage message{};
    while (true) {
        if (xQueueReceive(messageQueue, &message, portMAX_DELAY) == pdPASS) {
            if (strlen(message.origin) == 0 || strlen(message.command) == 0) {
                continue;
            }

            String payload = String(message.payload);
            String command = payload.length() > 0 ? payload : String(message.command);

            debugLog(String(DEBUG_TAG) + " Recibido -> " + command);
            forwardToScorbot(command);

            if (strcmp(message.origin, "BLUETOOTH") == 0) {
                sendToEspNow(message);
            } else {
                sendToBluetooth(message);
            }
        }
    }
}

void setup() {
    Serial.begin(115200);
    Serial2.begin(9600, SERIAL_8N1, 16, 17);
    delay(500);
    debugLog(String(DEBUG_TAG) + " Arrancando nodo Scorbot");

    messageQueue = xQueueCreate(QUEUE_SIZE, sizeof(CimMessage));

    WiFi.mode(WIFI_STA);
    WiFi.disconnect();

    if (!SerialBT.begin("CIM_SCORBOT")) {
        debugLog(String(DEBUG_TAG) + " No se pudo iniciar BluetoothSerial");
    } else {
        debugLog(String(DEBUG_TAG) + " BluetoothSerial listo");
    }

    if (esp_now_init() == ESP_OK) {
        esp_now_register_send_cb(onDataSent);
        esp_now_register_recv_cb(onDataRecv);
        debugLog(String(DEBUG_TAG) + " ESP-NOW listo");
    } else {
        debugLog(String(DEBUG_TAG) + " Error inicializando ESP-NOW");
    }

    xTaskCreatePinnedToCore(bluetoothTask, "btTask", 4096, nullptr, 2, nullptr, 1);
    xTaskCreatePinnedToCore(routingTask, "routeTask", 4096, nullptr, 2, nullptr, 1);
}

void loop() {
    vTaskDelay(pdMS_TO_TICKS(1000));
}
