/**
 * simulacion_esp32/src/main.cpp — CIM v6.0
 *
 * Flujo de datos:
 *   DHT22 (GPIO4) → lectura temp/humedad → LCD I2C (SDA=21, SCL=22)
 *   → HTTP POST JSON → gateway WiFi virtual Wokwi (192.168.4.1)
 *
 * Compatible con diagram.json y extensión wokwi.wokwi-vscode.
 */

#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <DHT.h>

// --- Pines según diagram.json ---
static const uint8_t DHT_PIN = 4;
static const uint8_t I2C_SDA = 21;
static const uint8_t I2C_SCL = 22;
static const uint8_t DHT_TYPE = DHT22;

// Red virtual Wokwi (SSID/password estándar del simulador)
static const char* WIFI_SSID = "Wokwi-GUEST";
static const char* WIFI_PASS = "";
// Gateway interno Wokwi para HTTP saliente simulado
static const char* TELEMETRY_URL = "http://192.168.4.1/telemetry";

DHT dht(DHT_PIN, DHT_TYPE);
LiquidCrystal_I2C lcd(0x27, 16, 2);

unsigned long lastReadMs = 0;
const unsigned long READ_INTERVAL_MS = 3000;

void connectWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("[WiFi] Conectando");
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  Serial.println();
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("[WiFi] OK IP=%s\n", WiFi.localIP().toString().c_str());
  } else {
    Serial.println("[WiFi] Fallo — reintentará en loop");
  }
}

void displayOnLcd(float temp, float hum) {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("CIM ESP32 v6.0");
  lcd.setCursor(0, 1);
  lcd.printf("T:%.1f H:%.0f%%", temp, hum);
}

bool postTelemetry(float temp, float hum) {
  if (WiFi.status() != WL_CONNECTED) return false;

  HTTPClient http;
  http.begin(TELEMETRY_URL);
  http.addHeader("Content-Type", "application/json");

  // Payload JSON alineado con telemetría industrial CIM
  String body = "{";
  body += "\"device\":\"CIM_ESP32_SIM\",";
  body += "\"mac\":\"" + WiFi.macAddress() + "\",";
  body += "\"temperature\":" + String(temp, 2) + ",";
  body += "\"humidity\":" + String(hum, 2) + ",";
  body += "\"version\":\"6.0\"";
  body += "}";

  int code = http.POST(body);
  Serial.printf("[HTTP] POST %s → %d\n", TELEMETRY_URL, code);
  if (code > 0) {
    String response = http.getString();
    if (response.length() > 0) Serial.println(response);
  }
  http.end();
  return code > 0 && code < 400;
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println("[FW] CIM simulacion_esp32 — DHT22 + LCD + HTTP");

  Wire.begin(I2C_SDA, I2C_SCL);
  lcd.init();
  lcd.backlight();
  lcd.print("Iniciando...");

  dht.begin();
  connectWiFi();
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
  }

  unsigned long now = millis();
  if (now - lastReadMs >= READ_INTERVAL_MS) {
    lastReadMs = now;

    float humidity = dht.readHumidity();
    float temperature = dht.readTemperature();

    if (isnan(humidity) || isnan(temperature)) {
      Serial.println("[DHT22] Lectura invalida");
      lcd.clear();
      lcd.print("Sensor error");
      return;
    }

    Serial.printf("[DHT22] T=%.2f C H=%.1f%%\n", temperature, humidity);
    displayOnLcd(temperature, humidity);
    postTelemetry(temperature, humidity);
  }

  delay(50);
}
