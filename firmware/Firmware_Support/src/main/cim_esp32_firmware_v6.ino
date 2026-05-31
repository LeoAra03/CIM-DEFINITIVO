#include <Arduino.h>
#include <WiFi.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <BluetoothSerial.h>
#include <string>

/*
 * FIRMWARE INDUSTRIAL CIM v6.0 (ESPRESSIF ESP32 / PlatformIO Arduino)
 * Híbrido BLE UART (Nordic) + Bluetooth Classic SPP
 * Protocolo CIM v6.0 — monitor serial 115200
 */

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error "Bluetooth no habilitado. Verifique sdkconfig / build_flags."
#endif

BluetoothSerial SerialBT;

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

#define LOG_TAG_BLE "[BLE]"
#define LOG_TAG_SPP "[SPP]"
#define LOG_TAG_CMD "[CMD]"
#define MAX_BUFFER_LEN 512

BLEServer* pServer = NULL;
BLECharacteristic* pTxCharacteristic = NULL;
bool bleConnected = false;
bool oldBleConnected = false;
std::string bleRxBuffer = "";
std::string sppRxBuffer = "";

const char* DEVICE_NAME = "CIM_ESP32";
const char* DEVICE_VERSION = "6.0.0";
char deviceNameBuf[64] = {0};

void forwardResponse(const std::string& msg);
void handleCommand(const std::string& cmdLine);

class BleServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* server) {
        bleConnected = true;
        Serial.println(LOG_TAG_BLE " Cliente BLE conectado");
    }
    void onDisconnect(BLEServer* server) {
        bleConnected = false;
        Serial.println(LOG_TAG_BLE " Cliente BLE desconectado");
    }
};

class RxCharacteristicCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* characteristic) {
        std::string rxValue = characteristic->getValue();
        if (rxValue.empty()) return;
        bleRxBuffer += rxValue;
        if (bleRxBuffer.find('\n') != std::string::npos ||
            bleRxBuffer.find('*') != std::string::npos ||
            bleRxBuffer.length() >= MAX_BUFFER_LEN) {
            handleCommand(bleRxBuffer);
            bleRxBuffer.clear();
        }
    }
};

void sendBleTx(const std::string& msg) {
    if (bleConnected && pTxCharacteristic != NULL) {
        pTxCharacteristic->setValue(msg);
        pTxCharacteristic->notify();
    }
}

void forwardResponse(const std::string& msg) {
    Serial.print(LOG_TAG_CMD " TX: ");
    Serial.println(msg.c_str());
    sendBleTx(msg);
    if (SerialBT.hasClient()) {
        SerialBT.println(msg.c_str());
    }
}

void sendAck(const std::string& originalId) {
    forwardResponse("ACK|" + originalId + "|" + std::string(deviceNameBuf));
}

void sendNack(const std::string& originalId, const std::string& errorMsg) {
    forwardResponse("NACK|" + originalId + "|" + errorMsg);
}

void sendIdentified(const std::string& statusPayload) {
    forwardResponse("IDENTIFIED|" + statusPayload + "|" + std::string(deviceNameBuf));
}

void handleCommand(const std::string& cmdLine) {
    if (cmdLine.empty()) return;

    Serial.print(LOG_TAG_CMD " RX: ");
    Serial.println(cmdLine.c_str());

    if (cmdLine.find("IDENTIFY") != std::string::npos) {
        sendIdentified("AUTHORIZED");
        return;
    }

    if (cmdLine.find("R:") == 0 || cmdLine.find("L:") == 0 ||
        cmdLine.find("C:") == 0 || cmdLine.find("M:") == 0 ||
        cmdLine.find("STO:") == 0 || cmdLine.find("CAM:") == 0 ||
        cmdLine.find("FREE:") == 0 || cmdLine.find("CHK:") == 0) {
        sendAck(cmdLine);
        return;
    }

    size_t pos1 = cmdLine.find('|');
    if (pos1 == std::string::npos) {
        sendNack(cmdLine, "INVALID_FORMAT");
        return;
    }

    std::string cmdType = cmdLine.substr(0, pos1);
    size_t pos2 = cmdLine.find('|', pos1 + 1);
    std::string cmdId = (pos2 == std::string::npos)
        ? cmdLine.substr(pos1 + 1)
        : cmdLine.substr(pos1 + 1, pos2 - pos1 - 1);

    if (cmdType == "EXECUTE" || cmdType == "STATUS_REQUEST") {
        sendAck(cmdId.empty() ? cmdLine : cmdId);
    } else {
        sendNack(cmdId.empty() ? cmdLine : cmdId, "UNKNOWN_COMMAND");
    }
}

void initBle() {
    uint8_t mac[6];
    WiFi.mode(WIFI_STA);
    WiFi.disconnect(true);
    WiFi.macAddress(mac);
    snprintf(deviceNameBuf, sizeof(deviceNameBuf), "%s_%02X%02X%02X", DEVICE_NAME, mac[3], mac[4], mac[5]);

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

void setup() {
    Serial.begin(115200);
    delay(500);
    Serial.println("\n=== CIM ESP32 Firmware v6.0 (BLE + SPP) ===");
    Serial.printf("IDF: %s\n", esp_get_idf_version());

    initBle();
    initSpp();

    delay(500);
    forwardResponse(String("IDENTIFY|") + deviceNameBuf + "|" + DEVICE_VERSION);
    Serial.println("[BOOT] Sistema listo");
}

void loop() {
    if (!bleConnected && oldBleConnected) {
        delay(500);
        BLEDevice::startAdvertising();
        oldBleConnected = bleConnected;
    }
    if (bleConnected && !oldBleConnected) {
        oldBleConnected = bleConnected;
    }

    if (SerialBT.available()) {
        String line = SerialBT.readStringUntil('\n');
        line.trim();
        if (line.length() > 0) {
            handleCommand(std::string(line.c_str()));
        }
    }

    if (Serial.available()) {
        String cmd = Serial.readStringUntil('\n');
        cmd.trim();
        if (cmd.equalsIgnoreCase("IDENTIFY")) {
            forwardResponse(String("IDENTIFY|") + deviceNameBuf + "|" + DEVICE_VERSION);
        }
    }

    delay(20);
}
