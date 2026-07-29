/*
  ESP32 TCP client example (plantilla)
  - Conecta a la red WiFi y se conecta al coordinador TCP en puerto 8888
  - Envía/recibe mensajes texto (CIM)

  Ajustar WIFI_SSID, WIFI_PASS y COORD_ADDRESS
*/

#include <WiFi.h>

const char* WIFI_SSID = "REPLACE_SSID";
const char* WIFI_PASS = "REPLACE_PASS";
const char* COORD_ADDRESS = "192.168.1.100"; // IP del coordinador
const uint16_t COORD_PORT = 8888;

WiFiClient client;

void setup() {
  Serial.begin(115200);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("Conectando WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" conectado");

  if (client.connect(COORD_ADDRESS, COORD_PORT)) {
    Serial.println("Conectado al coordinador TCP");
  } else {
    Serial.println("Error conectando al coordinador");
  }
}

void loop() {
  if (!client.connected()) {
    // intentar reconectar
    if (client.connect(COORD_ADDRESS, COORD_PORT)) {
      Serial.println("Reconectado al coordinador TCP");
    } else {
      delay(2000);
      return;
    }
  }

  // Enviar heartbeat o solicitud
  String msg = "PING|" + String(millis());
  client.println(msg);

  // Leer respuesta si hay
  while (client.available()) {
    String line = client.readStringUntil('\n');
    line.trim();
    if (line.length() > 0) {
      Serial.println(String("RX: ") + line);
      // parsear CIM y actuar
    }
  }

  delay(2000);
}
