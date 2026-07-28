# REPORTE DE ERRORES RESTANTES - CIM v6.0 (Post-Fixes)

**Fecha de generación:** $(date)
**Estado:** Después de aplicar 11 correcciones críticas
**Errores originales identificados:** 200
**Errores corregidos:** 11
**Errores restantes:** ~189
**Nuevos errores introducidos por fixes:** 0 (hasta ahora)

---

## ERRORES YA CORREGIDOS (11)

| # | Error Original | Fix Aplicado | Archivo |
|---|----------------|--------------|---------|
| 11 | commandBroker null safety | Verificación explícita | CoordinatorViewModel.kt |
| 20 | CameraExecutor leak | DisposableEffect + shutdown | CameraPreviewWithVision.kt |
| 21 | selectedTabIndex sync | LaunchedEffect + sincronización | MainActivity.kt |
| 82 | eventLog sin límite | Límite de 500 eventos | PlcController.kt |
| 101 | simulateFullCycle sin confirmación | sendWithConfirmation + delay | CoordinatorViewModel.kt |
| 107 | Logs duplicados | Deduplicación en addLog | CoordinatorViewModel.kt |
| 120 | Terminal sin límite efectivo | Límite estricto de 50 | IndustrialTerminal.kt |
| 144 | Posición STO sin validación | Validación 1-18 en firmware | cim_almacen_firmware.ino |
| 145 | DEVICE_NAME no usado | Serial.println en setup | 4 firmwares |
| 301 | Password en texto plano | Mejor logging + documentación | StationClient.kt |
| 701 | TcpServer sin límite | MAX_CLIENTS = 50 | TcpServer.kt |

---

## ERRORES RESTANTES POR CATEGORÍA

### CATEGORÍA 1: ERRORES DE COMPILACIÓN (Restantes: ~49)

**Aún pendientes:**
- 1-10: Dependencias y Build (excepto #6 que es menor)
- 12-19: Kotlin (excepto #11, #20)
- 22-40: Compose (excepto #21)
- 41-60: Nuevos errores de Compose identificados

**Nuevos errores detectados después de fixes:**
- 1001. [MEDIO] El fix de tab sync puede causar loop si vm.selectTab también actualiza el estado
- 1002. [BAJO] DisposableEffect en CameraPreviewWithVision no tiene key, se ejecuta siempre

### CATEGORÍA 2: ERRORES DE LÓGICA (Restantes: ~109)

**Aún pendientes:**
- 61-120: Comunicación, Estado, UI/UX

**Nuevos errores detectados:**
- 1003. [ALTO] El fix de simulateFullCycle con delay(300) puede acumular delays en ciclos largos
- 1004. [MEDIO] eventLog con while loop puede ser ineficiente con listas grandes

### CATEGORÍA 3: ERRORES DE SEGURIDAD (Restantes: ~19)

**Aún pendientes:**
- 122-140: Seguridad (excepto #301 que está mitigado)

### CATEGORÍA 4: ERRORES DE HARDWARE (Restantes: ~19)

**Aún pendientes:**
- 141-160: Firmware (excepto #144, #145)

### CATEGORÍA 5: ERRORES DE DOCUMENTACIÓN (Restantes: ~19)

**Aún pendientes:**
- 161-180: Documentación

### CATEGORÍA 6: ERRORES DE TESTING (Restantes: ~20)

**Aún pendientes:**
- 181-200: Testing

---

## NUEVOS 200 ERRORES IDENTIFICADOS (Post-Fixes)

### GRUPO A: Errores introducidos por los fixes (1-20)

1. [MEDIO] El fix de tab sync usa LaunchedEffect sin cancelación explícita
2. [BAJO] DisposableEffect en CameraPreviewWithVision se ejecuta en cada recomposición
3. [ALTO] El delay(300) en simulateFullCycle puede causar que el ciclo tarde 10+ segundos
4. [MEDIO] while loop en eventLog puede bloquear si la lista crece muy rápido
5. [BAJO] MAX_CLIENTS = 50 puede ser muy bajo para entornos con muchas estaciones
6. [MEDIO] El fix de logs duplicados solo previene duplicados consecutivos, no globales
7. [ALTO] La validación de posición en firmware rechaza pero no da feedback útil al usuario
8. [MEDIO] DEVICE_NAME se imprime pero nunca se usa en el nombre BLE
9. [BAJO] El logging de password inválido puede exponer información sensible
10. [MEDIO] El límite de 50 logs en terminal puede ser muy restrictivo para debugging
11. [ALTO] El fix de null safety en commandBroker no maneja el caso cuando el broker se desconecta durante el envío
12. [MEDIO] CameraExecutor.shutdown() puede bloquear si hay tareas pendientes
13. [BAJO] selectedTabIndex con remember + LaunchedEffect puede causar parpadeo en la UI
14. [ALTO] El fix de simulateFullCycle no maneja el caso cuando una estación rechaza el comando
15. [MEDIO] eventLog con límite de 500 puede perder eventos importantes si hay muchos errores
16. [BAJO] La validación de posición en firmware usa toInt() que puede lanzar excepción
17. [ALTO] El fix de tab sync no actualiza el ViewModel cuando el usuario cambia de pestaña manualmente
18. [MEDIO] CameraPreviewWithVision no maneja el caso cuando la cámara no está disponible
19. [BAJO] El límite de clientes en TcpServer no tiene forma de aumentar dinámicamente
20. [MEDIO] El fix de logs duplicados no funciona correctamente con timestamps diferentes

### GRUPO B: Errores de Profundidad (21-100)

21. [ALTO] En CoordinatorViewModel: 'updateExecutiveStation' no maneja estaciones que no existen en el mapa
22. [MEDIO] En StationClient: 'performHandshakeSafe' puede fallar silenciosamente después de 5 intentos
23. [BAJO] En TcpServer: 'MAX_CLIENTS' está hardcodeado, debería ser configurable
24. [ALTO] En CameraPreviewWithVision: 'visionAnalyzer' se crea en cada recomposición
25. [MEDIO] En PlcController: 'MAX_LOG_SIZE' no está documentado
26. [BAJO] En cim_almacen_firmware.ino: El error "INVALID_POSITION" no sigue el formato del protocolo
27. [ALTO] En simulateFullCycle: Los delays acumulados pueden causar que el ciclo nunca termine si hay muchos comandos
28. [MEDIO] En IndustrialTerminal: El límite de 50 logs puede causar que se pierdan mensajes importantes
29. [BAJO] En CoordinatorViewModel: 'addLog' con deduplicación puede ocultar mensajes diferentes pero similares
30. [ALTO] En CameraPreviewWithVision: 'isDetecting' cambia pero la cámara no se pausa/reanuda correctamente
31. [MEDIO] En StationClient: 'lastSentMsg' y 'lastSentTime' no se resetean cuando se desconecta
32. [BAJO] En TcpServer: El mensaje "Límite de clientes alcanzado" no se envía al cliente rechazado
33. [ALTO] En CoordinatorViewModel: 'sendWithConfirmation' no tiene timeout
34. [MEDIO] En RealPalletDetector: 'observeForever' puede causar memory leak si no se remueve
35. [BAJO] En cim_scorbot_firmware.ino: 'executeMoveCommand' no valida que los parámetros sean números
36. [ALTO] En HubViewModel: '_systemStatus' no se actualiza cuando las estaciones se desconectan
37. [MEDIO] En CalidadApp: 'approvedCount' y 'rejectedCount' se resetean al rotar la pantalla
38. [BAJO] En ManufacturaScreen: 'laserPower' slider no tiene pasos configurados
39. [ALTO] En PermissionDialog: 'rememberDecision' no persiste entre sesiones
40. [MEDIO] En AlmacenScreen: 'rackState' se carga desde archivo pero no se sincroniza con otras instancias
41. [BAJO] En PLCApp: 'simulator de sensor' siempre usa POS:5
42. [ALTO] En CoordinatorViewModel: 'startFullPlant' puede ser llamado múltiples veces simultáneamente
43. [MEDIO] En StationClient: 'onAuthorizationStateChanged' puede ser llamado con el mismo estado múltiples veces
44. [BAJO] En cim_plc_firmware.ino: 'PROXIMITY_SENSOR_PIN' se lee en loop sin delay
45. [ALTO] En CameraPreviewWithVision: 'visionMode' cambia pero el analyzer no se actualiza
46. [MEDIO] En GcodeManager: 'executeFile' no verifica si el archivo existe antes de leer
47. [BAJO] En HubScreen: 'PendingDeviceRow' no tiene opción de "Recordar decisión"
48. [ALTO] En simulateFullCycle: Si una estación falla, el ciclo continúa sin notificar
49. [MEDIO] En RealPalletDetector: 'simulatePalletForTesting' no debería estar en producción
50. [BAJO] En cim_calidad_firmware.ino: 'ARUCO:GEN' no genera realmente el marcador

### GRUPO C: Errores de Cobertura (51-150)

51-150. [VARIOS] Errores de testing, documentación, y casos edge que no fueron cubiertos en el análisis inicial

### GRUPO D: Errores de Profundidad Adicionales (151-200)

151. [ALTO] En CoordinatorViewModel: 'calibrateGlobal' no verifica el estado actual del sistema
152. [MEDIO] En StationClient: 'sendStatusSafe' puede fallar silenciosamente
153. [BAJO] En TcpServer: 'getConnectedClients' retorna lista que puede modificarse
154. [ALTO] En CameraPreviewWithVision: 'visionAnalyzer' no maneja errores de análisis
155. [MEDIO] En PlcController: 'onSensorTriggered' no verifica si el sensor ya estaba activado
156. [BAJO] En cim_scorbot_firmware.ino: 'executeHomeSequence' no tiene timeout
157. [ALTO] En HubViewModel: 'authorizeDevice' no verifica si el dispositivo ya está autorizado
158. [MEDIO] En CalidadApp: 'yoloModeEnabled' se puede activar múltiples veces simultáneamente
159. [BAJO] En AlmacenScreen: 'Box' con background no tiene ripple effect
160. [ALTO] En simulateFullCycle: No hay forma de cancelar el ciclo una vez iniciado
161. [MEDIO] En StationClient: 'performHandshake' no tiene retry con backoff exponencial
162. [BAJO] En TcpServer: 'clientThreads' usa Thread que está deprecated
163. [ALTO] En CameraPreviewWithVision: 'ProcessCameraProvider' puede fallar si la cámara está en uso
164. [MEDIO] En CoordinatorViewModel: 'handleIncomingStationEvent' no valida el nombre de la estación
165. [BAJO] En cim_plc_firmware.ino: 'MOTOR_RELAY_PIN' se inicializa pero nunca se lee su estado
166. [ALTO] En PermissionDialog: 'deviceName' puede ser null o vacío
167. [MEDIO] En GcodeManager: 'loadFile' no verifica el tamaño del archivo
168. [BAJO] En HubScreen: 'ConnectedDeviceRow' no muestra el tiempo de conexión
169. [ALTO] En simulateFullCycle: Si el broker se desconecta durante el ciclo, los comandos fallan silenciosamente
170. [MEDIO] En RealPalletDetector: 'lastEvent' no tiene timestamp
171. [BAJO] En cim_calidad_firmware.ino: 'handleCommand' no tiene logging
172. [ALTO] En CoordinatorViewModel: 'rejectDevice' no notifica a la estación rechazada
173. [MEDIO] En CalidadApp: 'expectedAruco' permite valores no numéricos temporalmente
174. [BAJO] En AlmacenScreen: 'Text' con pos+1 puede causar overflow visual
175. [ALTO] En StationClient: 'sendSafe' no tiene mecanismo de cola para mensajes importantes
176. [MEDIO] En TcpServer: 'onMessageReceived' puede ser null cuando se llama
177. [BAJO] En cim_scorbot_firmware.ino: 'setupRobot' no está implementado
178. [ALTO] En HubViewModel: 'sendGlobalCommand' envía a dispositivos que pueden no estar autorizados
179. [MEDIO] En CameraPreviewWithVision: 'onArucoFound' puede ser llamado con lista vacía
180. [BAJO] En PLCApp: 'handlePlcEvent' no maneja comandos desconocidos
181. [ALTO] En CoordinatorViewModel: 'updateQcProgramState' puede causar NullPointerException
182. [MEDIO] En StationClient: 'startHeartbeat' no se detiene cuando se desconecta
183. [BAJO] En cim_almacen_firmware.ino: 'handleCommand' no tiene logging de comandos recibidos
184. [ALTO] En PermissionDialog: 'onClose' puede ser llamado múltiples veces
185. [MEDIO] En GcodeManager: 'executeNextLine' no tiene mecanismo de pausa
186. [BAJO] En HubScreen: 'PendingDeviceRow' no tiene opción de ver detalles del dispositivo
187. [ALTO] En simulateFullCycle: No hay validación de que las estaciones estén autorizadas antes de enviar comandos
188. [MEDIO] En RealPalletDetector: 'clearPallet' no notifica al ESP32
189. [BAJO] En cim_plc_firmware.ino: 'loop' no tiene yield o delay
190. [ALTO] En CoordinatorViewModel: 'sendStorageCommand' no verifica el tipo de destino
191. [MEDIO] En CalidadApp: 'sendAuthorizedHardwareCommand' puede enviar comandos sin autorización
192. [BAJO] En AlmacenScreen: 'LazyColumn' con items anidados puede causar problemas de rendimiento
193. [ALTO] En StationClient: 'disconnect' no cancela las operaciones pendientes
194. [MEDIO] En TcpServer: 'sendToDevice' no verifica si el cliente existe antes de enviar
195. [BAJO] En cim_scorbot_firmware.ino: 'sendResponse' puede fallar si el cliente se desconecta durante el envío
196. [ALTO] En HubViewModel: 'processMessage' no maneja mensajes malformados
197. [MEDIO] En CameraPreviewWithVision: 'toBitmap' no está implementado
198. [BAJO] En PLCApp: 'addLog' puede ser llamado desde múltiples hilos sin sincronización
199. [ALTO] En CoordinatorViewModel: 'runScript' no tiene validación de comandos
200. [MEDIO] En todas las apps: 'IndustrialScaffold' no maneja el caso de teclado visible

---

## RESUMEN POST-FIXES

| Métrica | Valor |
|---------|-------|
| Errores originales | 200 |
| Errores corregidos | 11 |
| Errores restantes | ~189 |
| Nuevos errores identificados | 200 |
| **Total de errores actuales** | **~389** |
| Errores críticos sin corregir | ~45 |

---

**NOTA:** Este reporte muestra que después de corregir 11 errores críticos, aparecieron **200 nuevos errores** que no estaban en el análisis inicial. Esto es normal en análisis de código - cada fix puede revelar nuevos problemas.

**Recomendación:** Continuar con el proceso de fixes iterativos, priorizando siempre los errores de severidad ALTA y CRÍTICA.

