# Guía Maestra de Ingeniería: Sistema de Control y Coordinación CIM
**Proyecto:** Integración de Celda de Manufactura Flexible (UBB Chile)
**Objetivo:** Manual de réplica exacta y diseño de aplicaciones derivadas para estaciones industriales.

---

## 1. VISIÓN GENERAL DEL SISTEMA (LA IDEA)
Este sistema permite que un smartphone actúe como el **Cerebro (Maestro)** y otros smartphones actúen como **Controladores de Estación (Esclavos)**. 

### El Flujo de Mando:
1. El **Coordinador** decide qué toca hacer (ej: "Almacén, entrega una pieza").
2. Se envía una señal por **Bluetooth** a la APK de la estación.
3. La APK de la estación traduce eso y lo manda al **Arduino Mega**.
4. El Arduino Mega usa su puerto **RS-232** para mover la máquina real.
5. Cuando la máquina termina, el proceso vuelve hacia atrás hasta que el Coordinador recibe un "OK".

---

## 2. REQUISITOS PREVIOS
* **Software:** Android Studio (Ladybug o superior).
* **Lenguaje:** Kotlin con Jetpack Compose (Interfaz moderna).
* **Hardware:** Teléfonos con Android 8.0 o superior y Bluetooth funcional.

---

## 3. PASO 1: PERMISOS Y CONFIGURACIÓN (EL MANIFEST)
Para que el Bluetooth funcione, Android exige "pedir permiso". Si no haces esto, la App se cerrará al intentar conectar.

En tu archivo `AndroidManifest.xml`, antes de la etiqueta `<application>`, debes pegar esto:
```xml
<!-- Permisos de Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

## 4. PASO 2: EL "ANEXO DE FRECUENCIAS" (UUIDs)
El Bluetooth funciona por canales llamados **UUID**. Si dos aplicaciones quieren hablar, deben usar el mismo código. Aquí están los códigos oficiales para cada área:

| ESTACIÓN | CÓDIGO UUID (La Frecuencia) |
| :--- | :--- |
| **COORDINADOR (General)** | `00001101-0000-1000-8000-000000000000` |
| **ALMACÉN** | `00001101-0000-1000-8000-00000000AA01` |
| **MANUFACTURA** | `00001101-0000-1000-8000-00000000AA02` |
| **CALIDAD (QC)** | `00001101-0000-1000-8000-00000000AA03` |
| **PLC / CINTA** | `00001101-0000-1000-8000-00000000AA04` |

*Nota para compañeros:* Si estás haciendo la APK de Manufactura, tu App debe usar el UUID `...AA02`.

---

## 5. PASO 3: EXPLICACIÓN DEL CÓDIGO (EL BACKEND)

### A. El Gestor de Conexión (`GestorConexion`)
Es la parte del código que no ves pero que hace todo el trabajo.
1. **Servidor (`listenUsingRfcomm`):** El teléfono se pone en modo "oreja", esperando que la otra APK lo llame.
2. **Hilos (`Coroutines`):** Como el Bluetooth es lento, lo hacemos correr "en paralelo" para que el teléfono no se trabaje.
3. **Escucha Continua (`mantenerLectura`):** Es un bucle `while` que siempre está leyendo. Si recibe un "OK", actualiza la pantalla.

### B. El Controlador (`AppControl`)
Es donde escribes la inteligencia. 
- **Función `mandarComando`:** Toma el nombre de la estación y el texto que quieras (ej: "SUBIR_BRAZO"). Lo busca en la lista de conectados y lo dispara por el aire.
- **Secuencia Automática:** Es una lista de comandos con esperas (`delay`). 

---

## 6. PASO 4: CÓMO CREAR UNA "APLICACIÓN DERIVADA" (Para tus compañeros)
Si un compañero quiere hacer la APK de **Manufactura**, debe seguir este esquema:

1. **Interfaz:** Crear un botón que diga "CONECTAR AL MAESTRO".
2. **Conexión:** Al apretarlo, la App debe buscar dispositivos Bluetooth y conectarse usando el UUID `00001101-0000-1000-8000-00000000AA02`.
3. **Recepción:** Debe tener un lector que diga: 
   * "Si recibo el texto `ACTIVAR_LASER`, entonces mando un mensaje al Arduino por el puerto Serie".
4. **Confirmación:** Una vez que el Arduino le responda que terminó, la APK debe mandar de vuelta el texto `"OK"` al Maestro.

---

## 7. PASO 5: LA CONEXIÓN CON ARDUINO (RS-232)
El Arduino Mega recibirá los mensajes desde la APK de cada estación. 

**Ejemplo de código para el Arduino:**
```cpp
void loop() {
  if (SerialBT.available()) {
    String comando = SerialBT.readStringUntil('\n');
    
    if (comando == "ACTIVAR_LASER") {
      // Aquí el Arduino le habla a la máquina por RS-232 (Serial1)
      Serial1.println("M10 ON"); 
      SerialBT.println("OK"); // Le avisa a la APK que ya cumplió
    }
  }
}
```

---

## 8. TIPS PARA LA DEFENSA DEL PROYECTO
1. **Modularidad:** Si una APK se cae, el Coordinador te avisará con una luz roja, pero el resto del sistema puede seguir funcionando.
2. **Trazabilidad:** Gracias a la "Consola de Logs", podemos ver exactamente en qué milisegundo se envió cada orden (ideal para auditoría industrial).
3. **Bajo Costo:** Estamos reemplazando costosos PLC de red por una red inalámbrica basada en Android y Bluetooth.

---
**FIN DEL MANUAL**
