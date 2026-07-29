#include <Arduino.h>
#include <BluetoothSerial.h>
#include <WiFi.h>
#include <esp_now.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>
#include <string.h>

#define DEBUG_TAG "[LASER-ESP32]"
#define MAX_MSG_LEN 256
#define QUEUE_SIZE 32

BluetoothSerial SerialBT;

struct CimMessage {
    char origin[16];
    char destination[16];
    char command[24];
    char payload[160];
};

QueueHandle_t messageQueue = nullptr;

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

void sendToBluetooth(const String& text) {
    SerialBT.println(text);
    debugLog(String(DEBUG_TAG) + " BT TX -> " + text);
}

void forwardToGrbl(const String& line) {
    String framedLine = line + "\r\n";
    Serial2.print(framedLine);
    Serial2.flush();
    debugLog(String(DEBUG_TAG) + " Serial2 TX -> " + framedLine);
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

            String line = String(message.payload);
            if (line.length() == 0) {
                line = String(message.command);
            }

            forwardToGrbl(line);

            String response;
            unsigned long start = millis();
            while (millis() - start < 3000) {
                if (Serial2.available()) {
                    response = Serial2.readStringUntil('\n');
                    response.trim();
                    if (response.length() > 0) {
                        break;
                    }
                }
                vTaskDelay(pdMS_TO_TICKS(10));
            }

            if (response.equalsIgnoreCase("ok")) {
                sendToBluetooth("ok");
                debugLog(String(DEBUG_TAG) + " Handshake OK");
            } else {
                debugLog(String(DEBUG_TAG) + " GRBL response: " + response);
            }
        }
    }
}

void setup() {
    Serial.begin(115200);
    Serial2.begin(115200, SERIAL_8N1, 16, 17);
    delay(500);
    debugLog(String(DEBUG_TAG) + " Arrancando nodo laser");

    messageQueue = xQueueCreate(QUEUE_SIZE, sizeof(CimMessage));

    WiFi.mode(WIFI_STA);
    WiFi.disconnect();

    if (!SerialBT.begin("CIM_LASER")) {
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
