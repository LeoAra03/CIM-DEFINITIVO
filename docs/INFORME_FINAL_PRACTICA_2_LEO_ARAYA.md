# INFORME FINAL DE PRÁCTICA PROFESIONAL II
## Ingeniería de Ejecución en Computación e Informática (IECI)

---

**PROYECTO:** SISTEMA CIM V6.0 - CONTROL INDUSTRIAL DISTRIBUIDO MULTI-ESTACIÓN  
**ESTUDIANTE:** Leonardo Enrique Araya Labarca  
**RUN:** 21.290.314-0  
**INSTITUCIÓN:** Universidad del Bío-Bío (UBB)  
**FECHA:** 30 de Mayo de 2026  
**LUGAR:** Concepción, Chile  

---

## 📖 ÍNDICE GENERAL

1.  **INTRODUCCIÓN**
2.  **OBJETIVOS DEL PROYECTO**
3.  **PROCESO CREATIVO Y ARQUITECTURA**
    *   Analogía del Sistema
4.  **DESARROLLO TÉCNICO POR MÓDULOS**
    *   Coordinador Hub
    *   Estaciones de Trabajo (PLC, Manufactura, Almacén, Calidad)
5.  **RELACIÓN CON ESPRESSIF Y HARDWARE**
6.  **DESAFÍOS Y PROBLEMAS ENFRENTADOS**
7.  **DISTRIBUCIÓN DE HORAS DE TRABAJO**
8.  **CONCLUSIÓN**
9.  **REFERENCIAS (FORMATO APA)**

---

## 1. INTRODUCCIÓN

El concepto de *Computer Integrated Manufacturing* (CIM) representa la integración total de la producción mediante sistemas computacionales. En el contexto de esta Práctica Profesional II, se ha desarrollado el **Sistema CIM v6.0**, un ecosistema de software industrial diseñado para orquestar procesos de manufactura flexible. Este trabajo no solo abarca la programación de aplicaciones móviles en Android, sino que se extiende a la comunicación de bajo nivel con microcontroladores, visión artificial aplicada y protocolos de red robustos, cumpliendo con los estándares de formación técnica y profesional exigidos por la Universidad del Bío-Bío.

---

## 2. OBJETIVOS DEL PROYECTO

*   **Objetivo General:** Implementar una red distribuida de estaciones de trabajo industriales controladas centralmente por un Hub maestro.
*   **Objetivos Específicos:**
    1.  Desarrollar un motor de comunicación Bluetooth Híbrido (BLE y Clásico) estable.
    2.  Integrar visión artificial para la detección de marcadores ArUco y códigos QR.
    3.  Estandarizar un protocolo de mensajería asíncrono para el control de hardware (Robot Scorbot, Láser CNC).
    4.  Documentar la arquitectura bajo normativas académicas IECI.

---

## 3. PROCESO CREATIVO Y ARQUITECTURA

El proceso creativo se basó en el principio de **Clean Architecture**. Se decidió separar la lógica de red de la lógica de negocio, creando el módulo `core-network` como el "corazón" del sistema.

### 🎭 Analogía: "La Orquesta Sinfónica Industrial"
Para entender el sistema CIM v6.0, podemos compararlo con una orquesta:
*   **El Coordinador Hub es el Director:** No toca los instrumentos (hardware), pero tiene la partitura (flujo de trabajo) y decide cuándo entra cada músico.
*   **Las Estaciones son los Músicos:** Cada una (PLC, Manufactura) tiene una habilidad específica pero necesita la señal del director para actuar en armonía.
*   **El Protocolo CIM es la Partitura:** Es el lenguaje común que asegura que un `R:HOME` signifique lo mismo para el director y para el músico.

---

## 4. DESARROLLO TÉCNICO POR MÓDULOS

### 4.1 Coordinador Hub (Nodo Maestro)
El cerebro del sistema. Gestiona un servidor TCP en el puerto 8888 y administra el registro de dispositivos mediante una tabla hash O(1). Implementa seguridad perimetral basada en direcciones MAC.

### 4.2 Estaciones de Trabajo
*   **PLC (Cinta):** Gestiona la logística de transporte entre 10 estaciones.
*   **Manufactura (Scorbot/Láser):** Traduce comandos lógicos a señales PWM y digitales para actuadores físicos.
*   **Calidad (Visión):** Utiliza OpenCV 4.9.0 para inspección técnica en tiempo real.

---

## 5. RELACIÓN CON ESPRESSIF Y HARDWARE

El uso de **Espressif (ESP32)** fue fundamental. Se desarrolló un firmware v6.0 personalizado en C++ que permite a estos dispositivos actuar como puentes UART-Bluetooth. La relación técnica se centró en optimizar el stack BLE de Espressif para evitar la fragmentación de paquetes MTU, logrando una latencia inferior a los 50ms en la ejecución de comandos críticos como la parada de emergencia del láser.

---

## 6. DESAFÍOS Y PROBLEMAS ENFRENTADOS

Durante el desarrollo se enfrentaron obstáculos críticos:
1.  **Detección de MAC en Android 13+:** Debido a las restricciones de privacidad de Google, fue necesario implementar un sistema de permisos dinámicos complejo para obtener la identidad real de los dispositivos.
2.  **Concurrencia en Visión:** El procesamiento de imágenes con OpenCV tendía a bloquear la UI. Se solucionó mediante el uso de **Corrutinas de Kotlin** y el dispatching en hilos de computación (IO/Default).
3.  **Límites de GitHub:** El tamaño de las librerías nativas de visión superaba los 100MB. Se resolvió mediante un sistema de particionado de archivos binarios para el repositorio final.

---

## 7. DISTRIBUCIÓN DE HORAS DE TRABAJO

El proyecto demandó un total estimado de **480 horas cronológicas**, distribuidas de la siguiente manera:

| Actividad | Horas Invertidas |
|-----------|------------------|
| Diseño de Arquitectura y Sockets | 80 hrs |
| Desarrollo de Apps (Compose/UI) | 150 hrs |
| Integración de OpenCV y Visión | 100 hrs |
| Firmware C++ (ESP32) y Protocolo | 70 hrs |
| Pruebas de Campo y Debugging | 50 hrs |
| Documentación Técnica y Manuales | 30 hrs |

---

## 8. CONCLUSIÓN

El sistema CIM v6.0 cumple con éxito los requerimientos de una plataforma industrial moderna. La integración de tecnologías de consumo (Android) con hardware de bajo costo (ESP32) demuestra que es posible crear sistemas de manufactura flexible potentes, escalables y económicos. Este proyecto consolida los conocimientos adquiridos en la carrera de IECI en la UBB, especialmente en las áreas de sistemas distribuidos, redes y programación de alto rendimiento.

---

## 9. REFERENCIAS (APA)

*   Android Developers. (2024). *Bluetooth Low Energy Overview*. Recuperado de https://developer.android.com/guide/topics/connectivity/bluetooth-le
*   Espressif Systems. (2024). *ESP32 Technical Reference Manual*.
*   OpenCV.org. (2024). *ArUco Marker Detection with OpenCV*.
*   Universidad del Bío-Bío. (2024). *Perfil de Egreso Ingeniería de Ejecución en Computación e Informática*.
