# Matriz de Tests CIM v6.0

> **Documento consolidado:** Ver sección 9 en [`ENTREGA_FINAL_LEONARDO_ARAYA.md`](ENTREGA_FINAL_LEONARDO_ARAYA.md).

> **Suite oficial:** 30 casos representativos | **Última ejecución:** 2026-05-31  
> **Comando:** `./gradlew testAllModules`

## Resumen de ejecución

| Módulo | Tarea Gradle | Estado |
|--------|--------------|--------|
| core-network | `:core-network:testDebugUnitTest` | PASS |
| app-coordinador | `:app-coordinador:app-coordinador:testDebugUnitTest` | PASS |
| app-plc | `:app-plc:app-plc:testDebugUnitTest` | PASS |

---

## Matriz de 30 tests (multi-tipo)

| # | ID | Categoría | Módulo | Clase de test | Estado |
|---|-----|-----------|--------|---------------|--------|
| 1 | CIM-PROTO-01 | Unit — Protocolo | core-network | `CimMessageTest.testTransportSerializationAndDeserialization` | PASS |
| 2 | CIM-PROTO-02 | Unit — Protocolo | core-network | `CimMessageTest.testBackslashAndNewlineEscaping` | PASS |
| 3 | CIM-PROTO-03 | Unit — Protocolo | core-network | `CimMessageTest.testPermissionHandshakePayload` | PASS |
| 4 | CIM-AUTH-01 | Unit — Autorización | core-network | `AuthorizationManagerTest.testDefaultAuthorizationIsPending` | PASS |
| 5 | CIM-AUTH-02 | Unit — Autorización | core-network | `AuthorizationManagerTest.testAuthorizeAndDenyTransitions` | PASS |
| 6 | CIM-AUTH-03 | Unit — Autorización | core-network | `AuthorizationManagerTest.testRevokeRemovesState` | PASS |
| 7 | CIM-REG-01 | Unit — Registry O(1) | core-network | `DeviceRegistryTest.registerAndLookupByMac_isO1` | PASS |
| 8 | CIM-REG-02 | Performance O(1) | core-network | `DeviceRegistryTest.registryPerformance_1000Lookups_under50ms` | PASS |
| 9 | CIM-REG-03 | Unit — Registry | core-network | `DeviceRegistryTest.authorizeDevice_setsAuthorizedFlag` | PASS |
| 10 | CIM-BT-01 | Unit — Bluetooth lógica | core-network | `BluetoothFilterTest.validMacFormat_accepted` | PASS |
| 11 | CIM-BT-02 | Unit — Bluetooth lógica | core-network | `BluetoothFilterTest.industrialFilter_matchesEsp32` | PASS |
| 12 | CIM-BT-03 | Unit — Bluetooth lógica | core-network | `BluetoothFilterTest.industrialFilter_rejectsGeneric` | PASS |
| 13 | CIM-STRESS-01 | Stress — Desconexiones | core-network | `CimStressAndAcceptanceTest.disconnectStorm_brokerSurvives100Messages` | PASS |
| 14 | CIM-STRESS-02 | Stress — Auth denial | core-network | `CimStressAndAcceptanceTest.unauthorizedCommand_blockedByAuthManager` | PASS |
| 15 | CIM-STRESS-03 | Stress — Payload inválido | core-network | `CimStressAndAcceptanceTest.invalidTransportString_returnsNull` | PASS |
| 16 | CIM-PO-01 | Aceptación — Happy path | core-network | `CimStressAndAcceptanceTest.happyPath_identifyAuthorizeExecute` | PASS |
| 17 | CIM-PO-02 | Aceptación — Coordinador | app-coordinador | `CoordinatorViewModelTest.testConnectCinta` | PASS |
| 18 | CIM-PO-03 | Aceptación — PLC deliver | app-plc | `PlcStationManagerTest.testSendDeliverCommand` | PASS |
| 19 | CIM-INT-01 | Integración — Manufactura | app-coordinador | `ManufacturaStationTest.testManufacturaCompleteFlow` | PASS |
| 20 | CIM-INT-02 | Integración — Calidad | app-coordinador | `CalidadStationTest.testCalidadCompleteFlow` | PASS |
| 21 | CIM-INT-03 | Integración — Almacén | app-coordinador | `AlmacenStationTest.testAlmacenCompleteFlow` | PASS |
| 22 | CIM-PERF-01 | Performance — Throughput | app-coordinador | `PerformanceTests.testHighThroughputMessaging` | PASS |
| 23 | CIM-PERF-02 | Performance — Serialización | app-coordinador | `PerformanceTests.testSerializationPerformance` | PASS |
| 24 | CIM-PERF-03 | Performance — Concurrencia | app-coordinador | `PerformanceTests.testConcurrentMessageSending` | PASS |
| 25 | CIM-REL-01 | Resiliencia — Recovery | app-coordinador | `ReliabilityTests.testRecoverySequence` | PASS |
| 26 | CIM-REL-02 | Resiliencia — Heartbeat | app-coordinador | `ReliabilityTests.testHeartbeatFailureDetection` | PASS |
| 27 | CIM-DEST-01 | Destructivo — Spam clicks | app-plc | `IndustrialStressTests.Usuario Destructivo - Spam de comandos rapidos` | PASS |
| 28 | CIM-DEST-02 | Destructivo — BT apagado | app-plc | `IndustrialStressTests.Destructivo - Apagar Bluetooth mientras se transmite` | PASS |
| 29 | CIM-DEST-03 | Destructivo — Bypass password | app-plc | `IndustrialStressTests.Seguridad - Intento de bypass de password` | PASS |
| 30 | CIM-THESIS-01 | Aceptación — Gatekeeper BT | app-coordinador | `CoordinatorThesisTests.Requisito de Tesis - Gatekeeper Bluetooth Inicial` | PASS |

---

## Auditoría de complejidad temporal (O(1))

| Componente | Operación crítica | Estructura | Complejidad |
|------------|-------------------|------------|-------------|
| `MobileDeviceRegistry` | `getDeviceByMac(mac)` | `ConcurrentHashMap<String, DeviceInfo>` | **O(1)** |
| `MobileDeviceRegistry` | `getDevicesByType(type)` | Índice `AppType → Set<MAC>` + lookup | **O(k)** k = devices del tipo |
| `DeviceRegistry` (legacy) | lookup por IP | `ConcurrentHashMap` | **O(1)** |
| `AuthorizationManager` | `isAuthorized(mac)` | `ConcurrentHashMap` | **O(1)** |
| `TcpServer` | `sendToClientByMac(mac)` | `macToConnId` primario | **O(1)** (fallback O(n) solo si mapping ausente) |
| `BluetoothHardwareManager` | `sendToDevice(address, cmd)` | `connectedDevices[address]` | **O(1)** |
| `BluetoothHardwareManager` | `send(cmd)` legacy | `firstOrNull()` | **O(n)** — usar `sendToDevice` en multiconexión |

**Recomendación:** En escenarios multiconexión, siempre invocar `sendToDevice(mac, cmd)` en lugar del método legacy `send(cmd)`.

---

## Cómo re-ejecutar

```powershell
cd Practica_2
.\gradlew testAllModules --no-daemon
```

Reportes HTML:
- `core-network/build/reports/tests/testDebugUnitTest/index.html`
- `app-coordinador/app/build/reports/tests/testDebugUnitTest/index.html`
- `app-plc/app/build/reports/tests/testDebugUnitTest/index.html`
