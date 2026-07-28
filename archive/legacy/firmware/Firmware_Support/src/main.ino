#include <Arduino.h>
#include <BluetoothSerial.h>
#include <WiFi.h>
#include <esp_now.h>
#include <ArduinoJson.h>

// Simple ESP32 firmware skeleton supporting SPP (BluetoothSerial), ESP-NOW and Serial
// Sends an IDENTIFY message on boot and on SPP connection. Intended for PlatformIO/Arduino.

BluetoothSerial SerialBT;

const char* APP_ID = "ESP32_PLC_v1";
const char* APP_VERSION = "6.0";

void onDataRecv(const uint8_t *mac_addr, const uint8_t *data, int len) {
  // ESP-NOW message received (from peer ESP32)
  char macStr[18];
  snprintf(macStr, sizeof(macStr), "%02X:%02X:%02X:%02X:%02X:%02X",
           mac_addr[0], mac_addr[1], mac_addr[2], mac_addr[3], mac_addr[4], mac_addr[5]);
  Serial.printf("[ESP-NOW RX] from %s: %.*s\n", macStr, len, (const char*)data);
}

void onDataSent(const uint8_t *mac_addr, esp_now_send_status_t status) {
  // Delivery report
}

String getDeviceMac() {
  // Return primary WiFi MAC
  return WiFi.macAddress();
}

void sendIdentifyOverAll() {
  String mac = getDeviceMac();
  String payload = String("IDENTIFY|") + APP_ID + String("|") + mac + String("|") + APP_VERSION;
  // Send over Serial
  Serial.println(payload);
  // Send over SPP if connected
  if (SerialBT.hasClient()) {
    SerialBT.println(payload);
  }
  // Broadcast over ESP-NOW to peers
  esp_now_send(NULL, (const uint8_t*)payload.c_str(), payload.length());

  Serial.printf("→ SENT IDENTIFY: %s\n", payload.c_str());
}

void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.printf("[FW] CIM ESP32 v6.0 starting...\n");

  // Init Bluetooth SPP
  if (!SerialBT.begin("CIM_ESP32")) {
    Serial.println("⚠ BT SPP init failed");
  } else {
    Serial.println("✓ Bluetooth SPP initialized");
  }

  // Init WiFi for ESP-NOW (station mode)
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  if (esp_now_init() == ESP_OK) {
    Serial.println("✓ ESP-NOW init");
    esp_now_register_send_cb(onDataSent);
    esp_now_register_recv_cb(onDataRecv);
  } else {
    Serial.println("⚠ ESP-NOW init failed");
  }

  // Send identification after startup
  delay(1000);
  sendIdentifyOverAll();
}

void loop() {
  // Handle SPP incoming
  if (SerialBT.available()) {
    String line = SerialBT.readStringUntil('\n');
    line.trim();
    if (line.length() > 0) {
      Serial.printf("< SPP RX: %s\n", line.c_str());
      // Echo back ACK for commands
      if (line.startsWith("EXECUTE") || line.startsWith("CMD") || line.startsWith("MOTOR") ) {
        String ack = String("ACK|") + String(line);
        SerialBT.println(ack);
        Serial.println(String("→ SPP ACK: ") + ack);
      }
    }
  }

  // Handle Serial USB commands (for debugging)
  if (Serial.available()) {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();
    if (cmd.equalsIgnoreCase("IDENTIFY")) {
      sendIdentifyOverAll();
    }
  }

  delay(50);
}

