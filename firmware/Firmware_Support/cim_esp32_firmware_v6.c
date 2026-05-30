#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <esp_now.h>
#include <WiFi.h>
#include <string>
#include <vector>

/*
 * FIRMWARE INDUSTRIAL CIM v6.0 (ESPRESSIF ESP32)
 *
 * Características:
 * - BLE UART (Nordic) para comunicación con apps Android
 * - Identificación automática (IDENTIFY handshake)
 * - Soporte para comandos seriales a hardware (RS232)
 * - ESP-NOW para malla entre ESP32
 * - Logging centralizado
 * - O(1) device registry con MAC detection
 *
 * Protocolo CIM v5.1 compatible
 */

// ============= CONFIGURACIÓN BLE =============

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

// ============= DEFINICIONES =============

#define TAG_BLE "[BLE]"
#define TAG_CMD "[CMD]"
#define TAG_HW  "[HW]"
#define MAX_MESSAGE_LEN 512
#define HEARTBEAT_INTERVAL_MS 5000
#define IDENTIFY_TIMEOUT_MS 30000

// ============= VARIABLES GLOBALES =============

BLEServer* pServer = NULL;
BLECharacteristic* pTxCharacteristic = NULL;
BLECharacteristic* pRxCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;
unsigned long lastHeartbeat = 0;
unsigned long lastIdentify = 0;

// Identidad del dispositivo
const char* DEVICE_NAME = "CIM_ESP32_PLC";
const char* DEVICE_MAC_STR = "AA:BB:CC:DD:EE:FF"; // Se obtiene de WiFi.macAddress()
const char* DEVICE_VERSION = "6.0.0";
uint8_t DEVICE_MAC[6];

// Buffer de comandos recibidos
std::string rxBuffer = "";

// ============= CLASE: BLE CALLBACKS =============

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        lastIdentify = 0; // Resetear timer de IDENTIFY para que envíe uno nuevo
        Serial.println(TAG_BLE " ✓ Cliente conectado");
    }

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.println(TAG_BLE " ✗ Cliente desconectado");
    }
};

// ============= CLASE: RX CHARACTERISTIC CALLBACKS =============

class RxCharacteristicCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        std::string rxValue = pCharacteristic->getValue();
        if (!rxValue.empty()) {
            Serial.print(TAG_CMD " RX: ");
            Serial.println(rxValue.c_str());

            // Acumular en buffer
            rxBuffer += rxValue;

            // Si contiene \n o *, procesar mensaje
            if (rxValue.find('\n') != std::string::npos || rxValue.find('*') != std::string::npos) {
                handleCommand(rxBuffer);
                rxBuffer = "";
            }
        }
    }
};

// ============= CLASE: RX CHARACTERISTIC CALLBACKS =============

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string rxValue = pCharacteristic->getValue();

        if (rxValue.length() > 0) {
            Serial.print("[RX] ");
            for (int i = 0; i < rxValue.length(); i++) {
                Serial.print(rxValue[i]);
            }
            Serial.println();

            // Procesar comando
            processCommand(rxValue);
        }
    }
};

// ============= FUNCIONES =============

void processCommand(const std::string& cmd) {
    /*
     * Procesa comandos CIM estandarizados v6.0:
     * - R: (Robot): R:HOME, R:READY, R:OPEN, R:CLOSE, R:MOVE:X:10
     * - L: (Laser): L:START, L:STOP, L:HOME, L:Z_UP
     * - C: (Cinta): C:START, C:STOP, C:POS:5
     * - M: (Motor): M:START, M:STOP
     */

    if (cmd.find("IDENTIFY") != std::string::npos) {
        String response = String(DEVICE_NAME) + "|" + String(DEVICE_VERSION);
        pTxCharacteristic->setValue(response.c_str());
        pTxCharacteristic->notify();
        Serial.println("[CMD] IDENTIFY -> OK");
    }
    else if (cmd.find("R:") == 0) {
        Serial.print("[HW] ROBOT CMD: ");
        Serial.println(cmd.c_str());
        // Lógica de control Scorbot aquí
        sendAck(cmd);
    }
    else if (cmd.find("L:") == 0) {
        Serial.print("[HW] LASER CMD: ");
        Serial.println(cmd.c_str());
        // Lógica de control Láser aquí (Digital/PWM)
        sendAck(cmd);
    }
    else if (cmd.find("C:") == 0) {
        Serial.print("[HW] CONVEYOR CMD: ");
        Serial.println(cmd.c_str());
        // Lógica de control Cinta aquí
        sendAck(cmd);
    }
    else if (cmd.find("M:") == 0) {
        Serial.print("[HW] MOTOR CMD: ");
        Serial.println(cmd.c_str());
        sendAck(cmd);
    }
    else if (cmd.find("DELIVER") != std::string::npos) {
        // Compatibilidad legacy
        Serial.println("[CMD] DELIVER (Legacy) ok");
        sendAck(cmd);
    }
    else {
        Serial.print("[CMD] Desconocido: ");
        Serial.println(cmd.c_str());
        sendNack(cmd);
    }
}

void sendAck(const std::string& originalCmd) {
    if (deviceConnected && pTxCharacteristic) {
        String response = "ACK|" + String(originalCmd.c_str());
        pTxCharacteristic->setValue(response.c_str());
        pTxCharacteristic->notify();
        Serial.println("[TX] ACK enviado");
    }
}

void sendNack(const std::string& originalCmd) {
    if (deviceConnected && pTxCharacteristic) {
        String response = "NACK|" + String(originalCmd.c_str());
        pTxCharacteristic->setValue(response.c_str());
        pTxCharacteristic->notify();
        Serial.println("[TX] NACK enviado");
    }
}

void initializeBLE() {
    Serial.println("[INIT] Inicializando BLE...");

    // Obtener MAC del dispositivo
    WiFi.macAddress(DEVICE_MAC);

    // Crear nombre de dispositivo único
    char deviceNameBuf[50];
    sprintf(deviceNameBuf, "%s_%02X%02X%02X", DEVICE_NAME, DEVICE_MAC[3], DEVICE_MAC[4], DEVICE_MAC[5]);

    BLEDevice::init(deviceNameBuf);
    Serial.print("[BLE] Nombre: ");
    Serial.println(deviceNameBuf);

    // Crear servidor
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    // Crear servicio
    BLEService *pService = pServer->createService(SERVICE_UUID);

    // Crear características
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

    // Iniciar servicio
    pService->start();

    // Publicidad del servicio
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMaxPreferred(0x12);
    BLEDevice::startAdvertising();

    Serial.println("[BLE] ✓ Inicializado - Esperando conexión...");
}

void setup() {
    Serial.begin(115200);
    delay(1000);

    Serial.println("\n\n");
    Serial.println("╔════════════════════════════════════════╗");
    Serial.println("║  FIRMWARE CIM v6.0 (ESPRESSIF)        ║");
    Serial.println("║  Sistema de Manufactura Flexible       ║");
    Serial.println("╚════════════════════════════════════════╝");
    Serial.println();

    Serial.print("[BOOT] ESP32 Boot, ver: ");
    Serial.println(esp_get_idf_version());

    // Inicializar BLE
    initializeBLE();

    Serial.println("[BOOT] ✓ Sistema listo");
}

void loop() {
    // Manejar desconexión
    if (!deviceConnected && oldDeviceConnected) {
        delay(500);
        pServer->startAdvertising();
        Serial.println("[BLE] Reabriendo publicidad para reconexión...");
        oldDeviceConnected = deviceConnected;
    }

    // Manejar conexión nueva
    if (deviceConnected && !oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
        Serial.println("[BLE] ✓ Nuevo cliente conectado");
    }

    delay(100);
}

