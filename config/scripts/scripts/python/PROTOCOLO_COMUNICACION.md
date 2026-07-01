# 📡 Protocolos de Comunicación del Sistema CIM

Este documento centraliza las especificaciones de todos los protocolos de comunicación utilizados en el ecosistema CIM, tanto para la comunicación entre software (TCP) como para la comunicación con hardware (Serial).

---

## 1. Protocolo de Red TCP (Coordinador ↔ Estaciones)

Este protocolo se utiliza para la comunicación entre las aplicaciones (paneles de Python o APKs de Android) y el `servidor.py` (Coordinador).

### 1.1. Conexión y Handshake

- **IP del Servidor**: La que se muestra en la interfaz del Coordinador.
- **Puerto**: `8888` (TCP).

Al establecer la conexión, el cliente **debe** enviar inmediatamente una trama de identificación.

**Formato de Handshake (Ejemplo para APKs):**
`CIM_MASTER_HUB_V1;[Nombre_App];[Password];[MAC_Dispositivo];[UUID_Oficial]`

**Formato de Handshake (Ejemplo para Paneles Python):**
Al conectar, el panel envía su nombre de usuario (ej. `UsuarioPLC`).

### 1.2. Comandos del Servidor al Cliente

- `ABORT`: El cliente debe detener todas sus operaciones inmediatamente.
- `GRANTED`: El cliente tiene permiso para iniciar su ciclo de trabajo.

### 1.3. Mensajes del Cliente al Servidor

- `REQ_PERM`: El cliente solicita permiso para operar.
- `STATUS;[UUID];[ESTADO]`: El cliente informa su estado (`IDLE`, `BUSY`, `ERROR`).
- `PLC,[ESTACION],[PALLET],deliver`: Informa que se ha enviado un pallet a una estación.
- `PLC,[ESTACION],[PALLET],free`: Informa que se ha liberado un pallet de una estación.

---

## 2. Protocolo Serial - PLC (Cinta Transportadora)

Comunicación entre el software de control (`integrated_panel.py`, `cinta.py`) y el PLC que gestiona la cinta.

- **Configuración Serial**: `9600` baud, 7 data bits, Even parity, 2 stop bits (9600 7E2).
- **Terminador de Comando**: `\r\n\r\n`.

### 2.1. Comandos `DELIVER`

Mueve un pallet a una estación específica. El formato es `@00WD[REG_PALLET][REG_ESTACION][CHECKSUM]*`.

**Ejemplo**: Mover pallet `1` a la estación `1`.
- Comando: `@00WD000900015B*`

### 2.2. Comandos `FREE`

Libera una estación y confirma la salida de un pallet. Es una secuencia de dos comandos.

**Ejemplo**: Liberar estación `1` y confirmar salida de pallet `1`.
1.  Liberar estación: `@00WD004800015E*`
2.  Confirmar salida: `@00WD000900995A*`

---

## 3. Protocolo Serial - Robot Scorbot

Comunicación con los brazos robóticos Scorbot.

- **Configuración Serial**: `9600` baud, 8 data bits, No parity, 1 stop bit (9600 8N1).
- **Terminador de Comando**: `\r` (retorno de carro).

### 3.1. Comandos

Los comandos son texto plano en mayúsculas.
- `RUN [NOMBRE_PROGRAMA]`: Ejecuta un programa precargado en el robot (ej. `RUN ARU1`).
- `HERE [NOMBRE_POSICION]`: Define la posición actual con un nombre.
- `MOVE [NOMBRE_POSICION]`: Mueve el robot a una posición previamente definida.
- `MJ [EJE] [GRADOS]`: Mueve un eje específico un número de grados.
- `HOME`: Mueve el robot a su posición de origen.
- `OPEN` / `CLOSE`: Controla la pinza.
- `COFF`: Apaga los servomotores.