# Arquitectura de aseguramiento de seguridad y validación

**Estado:** diseño objetivo — no equivale a certificación ni a prueba de seguridad física.  
**Ámbito:** célula CIM con Android, Wemos D1 R32/ESP32, actuadores, cinta, robot y láser.

## Límite fundamental

No es posible demostrar honestamente `ε = 0` ni `P(fallo) < 10^-50` para un sistema físico abierto. Sensores, relés, alimentación, personas, software de terceros, cableado y entorno tienen modos de fallo no enumerables. Una placa Wemos D1 R32 no es un controlador de seguridad certificado.

El objetivo verificable debe ser **fail-safe**: ante pérdida de confianza, energía, comunicación, reloj, integridad o rango operativo, la energía de movimiento se deshabilita por hardware y el sistema exige rearme humano controlado.

La certificación real requiere un análisis de riesgo y diseño conforme a las normas y normativa aplicable, realizado y firmado por profesionales competentes. Referencias habituales: ISO 12100, ISO 13849-1/-2, IEC 60204-1, IEC 61508, IEC 62061, IEC 62443 y normas locales aplicables.

## 1. Arquitectura por capas

```text
Operador / Android CIM (no safety-rated)
        │ comandos con autorización y auditoría
        ▼
Gateway de proceso / Wemos ESP32 (no safety-rated)
        │ sólo control de proceso, nunca autoridad de seguridad
        ▼
PLC o relé de seguridad certificado
        │ dos canales, contactos guiados, diagnóstico
        ▼
Contactores / STO de variadores / alimentación de actuadores
        │
        ▼
Cinta, Scorbot, láser y periféricos
```

El canal E-stop no depende de Android, Wi-Fi, BLE, TCP, ESP32, firmware ni software de UI.

## 2. Triple redundancia y verificación cruzada

Para cada acción de proceso crítica se requieren tres observaciones independientes:

1. **Orden de proceso:** comando CIM firmado/autorizado.
2. **Realimentación eléctrica:** contacto auxiliar del relé/contactor o señal de variador.
3. **Realimentación física:** sensor de movimiento, encoder, barrera o proximidad.

La discrepancia entre orden, contacto y sensor provoca transición a `FAULT_LOCKED`. Ningún software debe reintentar una orden de movimiento automáticamente después de un fallo.

## 3. E-stop independiente

Implementación mínima propuesta:

- Dos pulsadores E-stop de doble canal, cableados a relé/PLC de seguridad.
- Circuito normalmente cerrado, supervisado para apertura, corto y cable roto.
- Salida de seguridad que corta energía de movimiento o activa STO.
- Contacto auxiliar leído por el ESP32 sólo como telemetría; no como fuente de verdad de seguridad.
- Rearme manual local, con inspección de causa y sin arranque automático.
- Disparadores adicionales: puerta de resguardo, sobretemperatura, pérdida de alimentación de control, pérdida de presión si aplica y watchdog del controlador de seguridad.

## 4. Matriz de cinco verificaciones por componente

| Componente | V1 | V2 | V3 | V4 | V5 |
|---|---|---|---|---|---|
| Wemos/ESP32 | build reproducible | test de protocolo | watchdog | brown-out test | prueba en banco | 
| Relé/cinta | comando proceso | contacto auxiliar | sensor físico | E-stop cableado | prueba de pérdida de energía |
| Scorbot | límite software | límite hardware | encoder/posición | E-stop | prueba de colisión sin carga |
| Láser | interlock | llave física | tapa/puerta | señal de emisión | E-stop |
| Red | CRC/firma | timeout | lista permitida | aislamiento VLAN | pérdida de enlace |

No se debe afirmar que estas cinco capas sean independientes sin un análisis de causa común: fuente compartida, masa, firmware común, operador común o cableado común pueden invalidar la independencia.

## 5. Protocolo de identidad y admisión

Antes de autorizar una estación, el Coordinador debe validar:

```text
stationUuid + appType + modelo + versión firmware + MAC física + capacidades + nonce
```

Políticas:

- lista permitida explícita; por defecto, denegar;
- bloqueo persistente con motivo, operador y fecha;
- una MAC/UUID no puede reemplazar una sesión activa sin aprobación humana;
- discrepancia UUID/tipo/capacidad bloquea el nodo;
- la UI debe mostrar MAC, UUID, nombre, RSSI, dirección IP/BLE, firmware y estado de confianza;
- retirar/bloquear desconecta el transporte, revoca la sesión y deja evidencia de auditoría.

## 6. Pruebas físicas por componente

### Wemos D1 R32 / ESP32

1. Flasheo con checksum de firmware y lectura posterior de versión.
2. Encendido/apagado repetido 100 veces; verificar que GPIO de relé inicia en estado seguro.
3. Inyección de tramas BLE fragmentadas, vacías, demasiado largas y corruptas.
4. Pérdida de conexión BLE durante transmisión y reconexión controlada.
5. Ensayo de sensor GPIO34 con señal válida, desconectada, ruido y tensión fuera de rango mediante interfaz protegida.

### Relé y cinta

1. Verificar estado de contacto auxiliar contra orden de proceso.
2. Simular bobina abierta, contacto pegado y pérdida de alimentación.
3. Verificar que E-stop corta energía aun con ESP32 congelado.
4. Verificar que el reinicio del ESP32 no activa el relé.
5. Medir temperatura, corriente y tiempo de parada.

### Robot/láser

Las pruebas deben hacerse sin carga, con zona delimitada, interlocks físicos activos y procedimiento del fabricante. No ejecutar pruebas destructivas de movimiento en un entorno no controlado.

## 7. Estrés y chaos testing

El factor 10x se aplica sólo en banco y nunca sobre energía/velocidad/movimiento que exceda la especificación del fabricante.

- 10x tasa nominal de mensajes simulados;
- 10x reconexiones de red/BLE;
- 10x tamaño máximo aceptado de entrada para comprobar rechazo;
- 10x duración de ejecución de telemetría;
- no 10x tensión, corriente, temperatura, velocidad, carga mecánica o potencia láser.

Criterios de aceptación: no crash, no fuga de memoria observable, no ejecución de comando incompleto, no activación de actuador no autorizado y recuperación a estado seguro.

## 8. Verificación formal: alcance realista

La verificación formal puede aplicarse a modelos de software/protocolo, no demostrar propiedades absolutas de la electrónica física completa.

Propiedades a modelar y probar:

```text
P1: un nodo bloqueado nunca recibe comando EXECUTE.
P2: un comando crítico requiere autorización vigente.
P3: una trama sin integridad válida no cambia estado.
P4: pérdida de enlace deriva en estado seguro antes de un límite temporal.
P5: dos nodos con el mismo UUID no pueden quedar simultáneamente autorizados.
P6: E-stop implica que ningún comando de software puede habilitar movimiento.
```

Herramientas potenciales: TLA+/PlusCal para estados distribuidos, property-based testing para codecs y Kotlin, y pruebas de modelo para la máquina de estados. La evidencia debe incluir modelo, invariantes, resultados y versión de herramientas.

## 9. Auditoría inmutable

Una cadena hash local es útil para detectar alteración, pero no es blockchain ni reemplaza un sistema de auditoría industrial.

Cada evento debe registrar:

```text
id, timestamp monotónico, operador, estación, UUID, MAC, comando,
resultado, hash anterior, hash actual, versión de firmware y versión de app
```

Para resistencia real a manipulación, exportar eventos a un servidor de sólo anexado con control de acceso, retención y reloj sincronizado. Firmar lotes con clave protegida; no guardar la clave privada en APK o ESP32.

## 10. Mantenimiento, calibración y monitorización

- Definir mantenimiento con datos del fabricante primero; Weibull se usa cuando exista población de fallos y datos suficientes.
- Calibrar instrumentos de medida contra patrones trazables por un laboratorio competente; no declarar trazabilidad nacional sin certificados.
- Registrar temperatura, tensión, corriente, RSSI, tasa de reintentos, pérdida de paquetes, reinicios, horas de operación y activaciones E-stop.
- El análisis predictivo es una alerta de mantenimiento, no una barrera de seguridad.
- Una tasa de detección de 99.9999999999% no se puede declarar sin un estudio estadístico con volumen de datos y límites de confianza adecuados.

## 11. Criterio de liberación

No liberar la célula para operación física hasta cumplir todos:

- CI verde, APKs trazables y firmware con checksum;
- pruebas de banco aprobadas y documentadas;
- E-stop cableado y probado con pérdida de energía/control;
- análisis de riesgo aprobado;
- lista permitida/bloqueada y roles de operador activos;
- capacitación de operador y procedimiento de recuperación;
- evidencia de pruebas de comunicación, pérdida de enlace y retorno seguro.
