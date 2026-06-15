#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <string>

/*
 * FIRMWARE INDUSTRIAL CIM v6.0 (ESPRESSIF ESP32)
 *
 * Protocolo: CIM v5.1 compatible
 * BLE UART (Nordic) service con RX/TX characteristics
 * Handshake IDENTIFY/IDENTIFIED automático
 * Respuestas ACK/NACK y manejo de comandos
 */

// ============= CONFIGURACIÓN BLE =============

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"  // Write
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"  // Notify (TX to client)

#define LOG_TAG_BLE "[BLE]"
#define LOG_TAG_CMD "[CMD]"
#define MAX_BUFFER_LEN 256

// ============= VARIABLES GLOBALES =============

BLEServer* pServer = NULL;
BLECharacteristic* pTxCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

// Identidad del dispositivo
const char* DEVICE_NAME = "CIM_ESP32";
const char* DEVICE_VERSION = "6.0.0";
uint8_t DEVICE_MAC[6];

// Buffer de RX
std::string rxBuffer = "";

// ============= CLASE: SERVER CALLBACKS =============

class BleServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        Serial.println(LOG_TAG_BLE " ✓ Cliente conectado");
    }

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.println(LOG_TAG_BLE " ✗ Cliente desconectado");
    }
};

// ============= CLASE: RX CHARACTERISTIC CALLBACKS =============

class RxCharacteristicCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        std::string rxValue = pCharacteristic->getValue();
        if (!rxValue.empty()) {
            Serial.print(LOG_TAG_CMD " RX: ");
            Serial.println(rxValue.c_str());

            // Acumular en buffer
            rxBuffer += rxValue;

            // Si contiene delimitador, procesar
            if (rxBuffer.find('\n') != std::string::npos ||
                rxBuffer.find('*') != std::string::npos ||
                rxBuffer.length() >= MAX_BUFFER_LEN) {
                handleCommand(rxBuffer);
                rxBuffer = "";
            }
        }
    }
};

// ============= FUNCIONES DE COMANDO =============

void sendTx(const std::string& msg) {
    if (deviceConnected && pTxCharacteristic != NULL) {
        pTxCharacteristic->setValue(msg);
        pTxCharacteristic->notify();
        Serial.print(LOG_TAG_CMD " TX: ");
        Serial.println(msg.c_str());
    }
}

void sendAck(const std::string& originalId) {
    std::string response = "ACK|" + originalId + "|" + std::string(DEVICE_NAME);
    sendTx(response);
}

void sendNack(const std::string& originalId, const std::string& errorMsg) {
    std::string response = "NACK|" + originalId + "|" + errorMsg;
    sendTx(response);
}

void sendIdentified(const std::string& statusPayload) {
    std::string response = "IDENTIFIED|";
    response += statusPayload;
    response += "|" + std::string(DEVICE_NAME);
    sendTx(response);
}

void handleCommand(const std::string& cmdLine) {
    // Parsear formato: CMD_TYPE|ID|PAYLOAD...
    // Ejemplo: IDENTIFY|uuid123
    //          EXECUTE|cmd456|MOTOR:START

    if (cmdLine.empty()) return;

    size_t pos1 = cmdLine.find('|');
    if (pos1 == std::string::npos) {
        Serial.println(LOG_TAG_CMD " Formato inválido");
        return;
    }

    std::string cmdType = cmdLine.substr(0, pos1);
    size_t pos2 = cmdLine.find('|', pos1 + 1);
    std::string cmdId = (pos2 == std::string::npos) ?
                        cmdLine.substr(pos1 + 1) :
                        cmdLine.substr(pos1 + 1, pos2 - pos1 - 1);

    if (cmdType == "IDENTIFY") {
        Serial.println(LOG_TAG_CMD " IDENTIFY recibido");
        sendIdentified("AUTHORIZED");
    }
    else if (cmdType == "EXECUTE") {
        Serial.print(LOG_TAG_CMD " EXECUTE: ");
        Serial.println(cmdId.c_str());
        sendAck(cmdId);
    }
    else if (cmdType == "STATUS_REQUEST") {
        Serial.println(LOG_TAG_CMD " STATUS_REQUEST recibido");
        std::string status = "READY|" + std::string(DEVICE_NAME);
        sendTx(status);
    }
    else {
        Serial.print(LOG_TAG_CMD " Comando desconocido: ");
        Serial.println(cmdType.c_str());
        sendNack(cmdId, "UNKNOWN_COMMAND");
    }
}

// ============= INICIALIZACIÓN BLE =============

void initBLE() {
    Serial.println(LOG_TAG_BLE " Inicializando BLE...");

    // Obtener MAC
    uint8_t mac[6];
    esp_read_mac(mac, ESP_MAC_WIFI_STA);
    sprintf((char*)DEVICE_MAC, "%02X:%02X:%02X:%02X:%02X:%02X",
            mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

    // Crear nombre único
    char deviceNameBuf[64];
    sprintf(deviceNameBuf, "%s_%02X%02X%02X", DEVICE_NAME, mac[3], mac[4], mac[5]);

    // Inicializar BLE
    BLEDevice::init(deviceNameBuf);

    // Crear servidor
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new BleServerCallbacks());

    // Crear servicio
    BLEService *pService = pServer->createService(SERVICE_UUID);

    // TX Characteristic (Notify)
    pTxCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_TX,
        BLECharacteristic::PROPERTY_NOTIFY
    );
    pTxCharacteristic->addDescriptor(new BLE2902());

    // RX Characteristic (Write)
    BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_RX,
        BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR
    );
    pRxCharacteristic->setCallbacks(new RxCharacteristicCallbacks());

    // Iniciar servicio
    pService->start();

    // Configurar publicidad
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMaxPreferred(0x12);
    BLEDevice::startAdvertising();

    Serial.print(LOG_TAG_BLE " ✓ Inicializado como: ");
    Serial.println(deviceNameBuf);
}

// ============= SETUP Y LOOP =============

void setup() {
    Serial.begin(115200);
    delay(1000);

    Serial.println("\n\n╔════════════════════════════════════════╗");
    Serial.println("║  CIM v6.0 FIRMWARE (ESP32)            ║");
    Serial.println("║  Sistema Manufactura Flexible         ║");
    Serial.println("╚════════════════════════════════════════╝\n");

    Serial.printf("IDF Version: %s\n", esp_get_idf_version());
    Serial.printf("Chip: %s\n", CONFIG_IDF_TARGET);

    // Inicializar BLE
    initBLE();

    Serial.println("[BOOT] ✓ Sistema listo\n");
}

void loop() {
    // Manejar reconexión: si desconecta, reabrir publicidad
    if (!deviceConnected && oldDeviceConnected) {
        delay(500);
        pServer->startAdvertising();
        Serial.println(LOG_TAG_BLE " Reabriendo publicidad...");
        oldDeviceConnected = deviceConnected;
    }

    // Registrar conexión nueva
    if (deviceConnected && !oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
        Serial.println(LOG_TAG_BLE " ✓ Nuevo cliente");
    }

    delay(100);
}

