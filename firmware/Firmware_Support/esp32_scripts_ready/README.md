ESP32 scripts list - instrucciones rápidas

Contenido:
- esp32_bluetooth_uart.ino    -> Ejemplo de sketch BLE UART/Serial que recibe/manda mensajes CIM
- esp32_tcp_client.ino        -> Ejemplo de cliente TCP que se conecta al coordinador (puerto 8888)

Instrucciones:
1) Abrir cada archivo en el IDE de Arduino o PlatformIO.
2) Ajustar SSID/PASSWORD en la sección WIFI (si usa TCP)
3) Compilar y subir al ESP32.

Formato de mensajes CIM (ejemplo en texto plano):
ID|TS|SRC_MAC|SRC_APP|DST_MAC|DST_APP|CMD|PRIO|PAYLOAD

Ejemplo de uso:
- El coordinador envía: 123|...|COORD_MAC|COORDINADOR|PLC_MAC|PLC|EXECUTE|1|DELIVER:1:2
- El ESP32 por BLE/TCP decodifica y ejecuta la acción en la cinta.

Notas:
- Estos sketches son plantillas minimalistas pensadas para pruebas. Adaptar I/O y pines según la electrónica de la cinta/robot/laser.
