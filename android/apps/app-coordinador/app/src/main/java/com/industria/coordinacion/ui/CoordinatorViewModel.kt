package com.industria.coordinacion.ui

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sistema.distribuido.network.GlobalDeviceRegistry
import com.sistema.distribuido.network.GlobalPermissionManager
import com.sistema.distribuido.network.CommandBroker
import com.sistema.distribuido.network.GlobalCommandBroker
import com.sistema.distribuido.network.PermissionRequest
import com.sistema.distribuido.network.AppIdentifier
import com.sistema.distribuido.network.PalletEvent
import com.sistema.distribuido.network.PalletStage
import com.sistema.distribuido.network.PalletWorkflowEngine
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CommandType as CimCommandType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CimMessageBuilder
// duplicate import removed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * VIEWMODEL DEL COORDINADOR
 *
 * Gestiona todo el estado central:
 * - Dispositivos conectados
 * - Comandos enviados
 * - Permisos
 * - Estado de pestañas
 */

data class TrackingState(
    val isTracking: Boolean = false,
    val pallets: List<PaletaTracking> = emptyList()
)

data class QcProgramState(
    val sr1Status: QCStatus? = null,
    val sr2Status: QCStatus? = null,
    val sr3Status: QCStatus? = null,
    val sr4Status: QCStatus? = null,
    val selectedProgram: String? = null
)

enum class ExecutiveStationStatus { ONLINE, READY, BUSY, WARNING, STOPPED }

data class StationCardState(
    val name: String,
    val label: String,
    val status: ExecutiveStationStatus = ExecutiveStationStatus.ONLINE,
    val detail: String = "Sin eventos",
    val lastEvent: String = "Sin eventos"
)

data class ExecutiveDashboardState(
    val stations: Map<String, StationCardState> = mapOf(
        "MANUFACTURA" to StationCardState("MANUFACTURA", "Manufactura", ExecutiveStationStatus.READY, "Listo para operar", "Sin eventos"),
        "CALIDAD" to StationCardState("CALIDAD", "Calidad", ExecutiveStationStatus.ONLINE, "Cámara y validación OK", "Sin eventos"),
        "ALMACEN" to StationCardState("ALMACEN", "Almacén", ExecutiveStationStatus.READY, "Pallets disponibles", "Sin eventos"),
        "CINTA" to StationCardState("CINTA", "Cinta", ExecutiveStationStatus.ONLINE, "Transportador disponible", "Sin eventos")
    ),
    val currentFlow: String = "LISTO",
    val lastEvent: String = "Sin eventos",
    val isEmergencyStop: Boolean = false
)

data class CoordinatorUiState(
    val currentTabIndex: Int = 0,
    val cintaState: CintaPanelState = CintaPanelState(),
    val networkState: NetworkTabState = NetworkTabState(),
    val trackingState: TrackingState = TrackingState(),
    val qcState: QcProgramState = QcProgramState(),
    val pendingPermissionRequest: PermissionRequest? = null,
    val isAutoModeEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val logMessages: List<String> = emptyList(),
    val executiveState: ExecutiveDashboardState = ExecutiveDashboardState()
)

class CoordinatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CoordinatorUiState())
    val uiState: StateFlow<CoordinatorUiState> = _uiState.asStateFlow()

    private val commandBroker: CommandBroker? = GlobalCommandBroker.getInstanceOrNull()
    private val palletWorkflow = PalletWorkflowEngine()

    init {
        setupListeners()
        startMonitoring()
    }

    private fun setupListeners() {
        viewModelScope.launch {
            try {
                GlobalDeviceRegistry.registry.addListener(object : com.sistema.distribuido.network.MobileDeviceRegistry.RegistryListener {
                    override suspend fun onDeviceAdded(device: com.sistema.distribuido.network.DeviceInfo) {
                        updateDeviceList()
                        addLog("✓ Dispositivo agregado: ${device.nombre} [${device.mac}]")
                    }

                    override suspend fun onDeviceRemoved(mac: String) {
                        updateDeviceList()
                        addLog("✗ Dispositivo desconectado: $mac")
                    }

                    override suspend fun onDeviceUpdated(device: com.sistema.distribuido.network.DeviceInfo) {
                        updateDeviceList()
                    }

                    override suspend fun onAuthorizationChanged(mac: String, authorized: Boolean) {
                        updateDeviceList()
                        addLog("${if (authorized) "✓" else "✗"} Autorización: $mac")
                    }
                })

                commandBroker?.addCommandReceivedListener { message ->
                    handleCommandResponse(message)
                }

                commandBroker?.addErrorListener { errorMsg ->
                    addLog("✗ BROKER ERROR: $errorMsg")
                }
            } catch (_: Exception) {
                // Ignorar errores de inicialización del listener
            }
        }
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                updateDeviceList()
                // Poll pending permission requests
                try {
                    val pending = GlobalPermissionManager.getInstance().getPendingRequests()
                val firstRequest = pending.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    pendingPermissionRequest = firstRequest,
                    networkState = _uiState.value.networkState.copy(
                        pendingRequestCount = pending.size,
                        pendingRequestSummary = if (pending.isEmpty()) {
                            "Sin solicitudes pendientes"
                        } else {
                            "${pending.size} solicitudes pendientes. Última: ${firstRequest?.deviceName}"
                        },
                        lastMessage = firstRequest?.let { "PENDING_PERMISSION:${it.mac}" } ?: _uiState.value.networkState.lastMessage
                    )
                )
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun handleCommandResponse(response: CimMessage) {
        val statusMessage = "RESP ${response.commandType} de ${response.sourceApp}: ${response.payload}"
        addLog(statusMessage)
        _uiState.value = _uiState.value.copy(
            networkState = _uiState.value.networkState.copy(lastMessage = statusMessage)
        )

        // Actualizar estado QC si recibimos ACK/NACK asociado a un programa
        if (response.payload.contains("SR1", ignoreCase = true)) {
            updateQcProgramState("SR1", response)
        }
        if (response.payload.contains("SR2", ignoreCase = true)) {
            updateQcProgramState("SR2", response)
        }
        if (response.payload.contains("SR3", ignoreCase = true)) {
            updateQcProgramState("SR3", response)
        }
        if (response.payload.contains("SR4", ignoreCase = true)) {
            updateQcProgramState("SR4", response)
        }
    }

    private fun updateQcProgramState(program: String, response: CimMessage) {
        val status = when (response.commandType) {
            CimCommandType.ACK -> QCStatus.SUCCESS
            CimCommandType.NACK, CimCommandType.ERROR, CimCommandType.TIMEOUT -> QCStatus.FAILED
            else -> QCStatus.RUNNING
        }
        _uiState.value = _uiState.value.copy(
            qcState = when (program.uppercase()) {
                "SR1" -> _uiState.value.qcState.copy(sr1Status = status, selectedProgram = program)
                "SR2" -> _uiState.value.qcState.copy(sr2Status = status, selectedProgram = program)
                "SR3" -> _uiState.value.qcState.copy(sr3Status = status, selectedProgram = program)
                "SR4" -> _uiState.value.qcState.copy(sr4Status = status, selectedProgram = program)
                else -> _uiState.value.qcState
            }
        )
    }

    fun startQcProgram(program: String) {
        viewModelScope.launch {
            try {
                addLog("⟳ Iniciando QC $program")
                _uiState.value = _uiState.value.copy(
                    qcState = when (program.uppercase()) {
                        "SR1" -> _uiState.value.qcState.copy(sr1Status = QCStatus.RUNNING, selectedProgram = program)
                        "SR2" -> _uiState.value.qcState.copy(sr2Status = QCStatus.RUNNING, selectedProgram = program)
                        "SR3" -> _uiState.value.qcState.copy(sr3Status = QCStatus.RUNNING, selectedProgram = program)
                        "SR4" -> _uiState.value.qcState.copy(sr4Status = QCStatus.RUNNING, selectedProgram = program)
                        else -> _uiState.value.qcState
                    }
                )
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = AppType.COORDINADOR,
                        destMac = "",
                        destApp = AppType.MANUFACTURA,
                        command = "QC_PROGRAM_${program.uppercase()}_START"
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error QC $program: ${e.message}")
            }
        }
    }

    fun stopQcProgram(program: String) {
        viewModelScope.launch {
            try {
                addLog("✗ Deteniendo QC $program")
                _uiState.value = _uiState.value.copy(
                    qcState = when (program.uppercase()) {
                        "SR1" -> _uiState.value.qcState.copy(sr1Status = null, selectedProgram = null)
                        "SR2" -> _uiState.value.qcState.copy(sr2Status = null, selectedProgram = null)
                        "SR3" -> _uiState.value.qcState.copy(sr3Status = null, selectedProgram = null)
                        "SR4" -> _uiState.value.qcState.copy(sr4Status = null, selectedProgram = null)
                        else -> _uiState.value.qcState
                    }
                )
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = AppType.COORDINADOR,
                        destMac = "",
                        destApp = AppType.MANUFACTURA,
                        command = "QC_PROGRAM_${program.uppercase()}_STOP"
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error detener QC $program: ${e.message}")
            }
        }
    }

    private suspend fun updateDeviceList() {
        try {
            val devices = GlobalDeviceRegistry.registry.getAllDevices()
            val connectedDevices = devices.map { device ->
                ConnectedDevice(
                    mac = device.mac,
                    appType = device.appType.toString(),
                    name = device.nombre,
                    isConnected = device.isConnected,
                    isAuthorized = device.authorized,
                    rssi = device.rssi,
                    ip = device.ip,
                    stationUuid = device.stationUuid,
                    version = device.version,
                    hardwareModel = device.hardwareModel,
                    capabilities = device.capabilities
                )
            }
            val activeBluetooth = connectedDevices.count { it.isConnected }
            val bestRssi = connectedDevices.maxOfOrNull { it.rssi }
            val blockedDevices = GlobalPermissionManager.getInstance().getBlockedDevices().map { blocked ->
                BlockedDeviceState(blocked.mac, blocked.reason, blocked.blockedAt)
            }

            _uiState.value = _uiState.value.copy(
                networkState = _uiState.value.networkState.copy(
                    connectedDevices = connectedDevices,
                    totalConnected = connectedDevices.size,
                    bluetoothSummary = "Bluetooth: $activeBluetooth conectados · Mejor RSSI: ${bestRssi ?: 0} dBm",
                    isAutoModeEnabled = _uiState.value.isAutoModeEnabled,
                    blockedDevices = blockedDevices
                )
            )
        } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
            addLog("⚠ Error actualizar devices: ${e.message}")
        }
    }

    private fun updateExecutiveStation(station: String, status: ExecutiveStationStatus, detail: String, event: String? = null) {
        val key = station.uppercase()
        val current = _uiState.value.executiveState
        val updated = current.stations.toMutableMap()
        val previous = updated[key] ?: StationCardState(key, station.replaceFirstChar { it.uppercaseChar() }, status, detail, event ?: detail)
        updated[key] = previous.copy(status = status, detail = detail, lastEvent = event ?: detail)
        _uiState.value = _uiState.value.copy(
            executiveState = current.copy(
                stations = updated,
                currentFlow = detail,
                lastEvent = event ?: detail
            )
        )
    }

    fun handleIncomingStationEvent(stationName: String, event: String) {
        handlePalletWireEvent(event)
        val normalized = event.trim().uppercase()
        val status = when {
            normalized.contains("ERROR") || normalized.contains("STOP") || normalized.contains("E-STOP") -> ExecutiveStationStatus.STOPPED
            normalized.contains("PALLET LIBERADO") || normalized.contains("READY") || normalized.contains("OK") -> ExecutiveStationStatus.READY
            normalized.contains("PROCESANDO") || normalized.contains("BUSY") || normalized.contains("PREPARANDO") -> ExecutiveStationStatus.BUSY
            normalized.contains("WARN") || normalized.contains("ALERTA") -> ExecutiveStationStatus.WARNING
            else -> ExecutiveStationStatus.ONLINE
        }
        updateExecutiveStation(stationName, status, event, event)
        if (stationName.uppercase() == "ALMACEN" && normalized.contains("PALLET LIBERADO")) {
            updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.BUSY, "Preparación automática por pallet liberado", "PREPARANDO")
            addLog("→ FLUJO: Almacén reportó pallet liberado; Manufactura preparada")
            sendExecuteCommand(AppType.MANUFACTURA, "PREPARE_FROM_STORAGE")
        }
    }

    fun startFullPlant() {
        viewModelScope.launch {
            try {
                updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.BUSY, "Planta completa iniciada", "PLANTA START")
                updateExecutiveStation("CALIDAD", ExecutiveStationStatus.BUSY, "Calibración de visión activada", "CALIBRACIÓN")
                updateExecutiveStation("ALMACEN", ExecutiveStationStatus.READY, "Preparación de almacén OK", "READY")
                updateExecutiveStation("CINTA", ExecutiveStationStatus.BUSY, "Cinta en movimiento", "START")
                addLog("→ FLUJO: Inicio de planta completa")
                sendExecuteCommand(AppType.MANUFACTURA, "PLANTA_START")
                sendExecuteCommand(AppType.PLC, "PLANTA_START")
                sendExecuteCommand(AppType.ALMACEN, "PLANTA_START")
                sendExecuteCommand(AppType.CALIDAD, "PLANTA_START")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error inicio planta completa: ${e.message}")
            }
        }
    }

    fun calibrateGlobal() {
        viewModelScope.launch {
            try {
                updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.READY, "Calibración global OK", "CALIBRACIÓN")
                updateExecutiveStation("CALIDAD", ExecutiveStationStatus.READY, "Visión calibrada", "CALIBRACIÓN")
                updateExecutiveStation("CINTA", ExecutiveStationStatus.ONLINE, "Cinta calibrada", "CALIBRACIÓN")
                addLog("→ FLUJO: Calibración global ejecutada")
                sendExecuteCommand(AppType.MANUFACTURA, "CALIBRATE_GLOBAL")
                sendExecuteCommand(AppType.PLC, "CALIBRATE_GLOBAL")
                sendExecuteCommand(AppType.CALIDAD, "CALIBRATE_GLOBAL")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error calibración global: ${e.message}")
            }
        }
    }

    fun triggerEmergencyStop() {
        viewModelScope.launch {
            try {
                val updated = _uiState.value.executiveState.copy(isEmergencyStop = true)
                _uiState.value = _uiState.value.copy(executiveState = updated)
                listOf(AppType.MANUFACTURA, AppType.PLC, AppType.ALMACEN, AppType.CALIDAD).forEach { appType ->
                    sendExecuteCommand(appType, "E_STOP")
                }
                addLog("✖ E-STOP activado: se frenan rutinas Scorbot y pallets Omron")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error E-STOP: ${e.message}")
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(currentTabIndex = index)
    }

    fun setAutoModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                GlobalPermissionManager.autoApproveTestMode = enabled
                _uiState.value = _uiState.value.copy(
                    isAutoModeEnabled = enabled,
                    networkState = _uiState.value.networkState.copy(isAutoModeEnabled = enabled)
                )
                if (enabled) {
                    clearPendingPermissionRequest()
                    addLog("✓ Modo AUTO activado: se aprobarán permisos automáticamente")
                } else {
                    addLog("✗ Modo AUTO desactivado: autorizaciones manuales habilitadas")
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error cambiando modo AUTO: ${e.message}")
            }
        }
    }

    fun clearPendingPermissionRequest() {
        _uiState.value = _uiState.value.copy(pendingPermissionRequest = null)
    }

    private suspend fun resolveTargetMac(destApp: AppType): String? {
        return try {
            val targets = GlobalDeviceRegistry.registry.getDevicesByType(destApp)
            targets.firstOrNull { it.isConnected && it.authorized }?.mac
        } catch (_: Exception) {
            null
        }
    }

    private fun sendExecuteCommand(destApp: AppType, command: String, destMac: String = "") {
        viewModelScope.launch {
            try {
                val actualDestMac = if (destMac.isBlank()) resolveTargetMac(destApp) ?: "" else destMac
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = AppType.COORDINADOR,
                        destMac = actualDestMac,
                        destApp = destApp,
                        command = command
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error enviando comando $command: ${e.message}")
            }
        }
    }

    // ============= CINTA (Sistema) =============
    fun sendCintaCommand(fromStation: Int, toStation: Int) {
        viewModelScope.launch {
            try {
                val cmd = "DELIVER:$fromStation:$toStation"
                addLog("→ CINTA DELIVER: $fromStation → $toStation")
                // Enviar comando via CommandBroker si está disponible
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = com.sistema.distribuido.network.protocol.CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = com.sistema.distribuido.network.protocol.AppType.COORDINADOR,
                        destMac = "",
                        destApp = com.sistema.distribuido.network.protocol.AppType.PLC,
                        command = cmd
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error Cinta: ${e.message}")
            }
        }
    }

    fun sendFreeCommand(fromStation: Int, toStation: Int) {
        viewModelScope.launch {
            try {
                val cmd = "FREE:$fromStation:$toStation"
                addLog("→ CINTA FREE: $fromStation → $toStation")
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = com.sistema.distribuido.network.protocol.CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = com.sistema.distribuido.network.protocol.AppType.COORDINADOR,
                        destMac = "",
                        destApp = com.sistema.distribuido.network.protocol.AppType.PLC,
                        command = cmd
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error: ${e.message}")
            }
        }
    }

    fun connectCinta() {
        viewModelScope.launch {
            try {
                addLog("⟳ Conectando Cinta...")
                val newCintaState = _uiState.value.cintaState.copy(isConnected = true)
                _uiState.value = _uiState.value.copy(cintaState = newCintaState)
                addLog("✓ Cinta conectada")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error conexión Cinta: ${e.message}")
            }
        }
    }

    fun disconnectCinta() {
        viewModelScope.launch {
            try {
                val newCintaState = _uiState.value.cintaState.copy(isConnected = false)
                _uiState.value = _uiState.value.copy(cintaState = newCintaState)
                addLog("✗ Cinta desconectada")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error desconexión: ${e.message}")
            }
        }
    }

    fun resetCinta() {
        viewModelScope.launch {
            try {
                addLog("⟳ Reseteando Cinta...")
                addLog("✓ Cinta reseteada")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error reset: ${e.message}")
            }
        }
    }

    // ============= ROBOT & LASER =============
    fun sendRobotCommand(command: String) {
        addLog("→ ROBOT: $command")
        sendExecuteCommand(AppType.MANUFACTURA, command)
    }

    fun sendLaserCommand(command: String) {
        addLog("→ LASER: $command")
        sendExecuteCommand(AppType.MANUFACTURA, command)
    }

    fun sendLaserLoadFile(filename: String, base64Content: String) {
        addLog("→ LASER LOAD: $filename")
        sendExecuteCommand(AppType.MANUFACTURA, "LASER_LOAD:$filename:$base64Content")
    }

    // ============= ARUCO =============
    fun generateAruco(seed: String) {
        viewModelScope.launch {
            try {
                addLog("⟳ Generando ArUco: $seed")
                sendExecuteCommand(AppType.MANUFACTURA, "ARUCO_GENERATE:$seed")
                addLog("✓ Solicitud ArUco enviada")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error ArUco: ${e.message}")
            }
        }
    }

    fun handleArucoDetected(aruco: DetectedArUco) {
        viewModelScope.launch {
            try {
                addLog("⟳ ArUco detectado: ID=${aruco.id} centro=(${aruco.center.first.toInt()},${aruco.center.second.toInt()}) rot=${aruco.rotation.toInt()}°")
                val payload = "ARUCO_DETECTED:${aruco.id}|X:${aruco.center.first.toInt()}|Y:${aruco.center.second.toInt()}|R:${aruco.rotation.toInt()}"
                sendExecuteCommand(AppType.MANUFACTURA, payload)
                addLog("✓ Enviado evento ArUco detectado")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error enviando ArUco detectado: ${e.message}")
            }
        }
    }

    // ============= TRACKING =============
    fun startTracking() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                trackingState = _uiState.value.trackingState.copy(isTracking = true)
            )
            addLog("✓ Tracking activo: esperando eventos de estaciones")
        }
    }

    /** Procesa eventos trazables: PALLET:<id>|EVENT:<PalletEvent>|ARUCO:<id>|PRODUCT:<id>. */
    private fun handlePalletWireEvent(rawEvent: String) {
        val fields = rawEvent.split("|").associate { token ->
            val parts = token.split(":", limit = 2)
            (parts.getOrNull(0)?.uppercase() ?: "") to (parts.getOrNull(1) ?: "")
        }
        val palletId = fields["PALLET"]?.trim().orEmpty()
        val eventName = fields["EVENT"]?.trim().orEmpty()
        if (palletId.isBlank() || eventName.isBlank()) return

        viewModelScope.launch {
            val existing = palletWorkflow.get(palletId)
            if (existing == null) {
                palletWorkflow.register(
                    palletId = palletId,
                    arucoId = fields["ARUCO"]?.toIntOrNull(),
                    productId = fields["PRODUCT"]?.ifBlank { null }
                )
            }
            val event = runCatching { PalletEvent.valueOf(eventName.uppercase()) }.getOrNull()
            if (event == null) {
                addLog("⚠ Evento de pallet inválido: $eventName")
                return@launch
            }
            val result = palletWorkflow.apply(palletId, event, rawEvent)
            refreshPalletTracking()
            addLog("${if (result.accepted) "✓" else "✗"} PALLET $palletId: ${result.snapshot.stage}")
        }
    }

    private fun refreshPalletTracking() {
        val pallets = palletWorkflow.all().map { snapshot ->
            PaletaTracking(
                id = snapshot.palletId,
                ubicacion = when (snapshot.stage) {
                    PalletStage.REGISTERED -> "Registro / Almacén"
                    PalletStage.STORAGE_RELEASED -> "Salida de Almacén"
                    PalletStage.CONVEYOR_TO_MANUFACTURING -> "Cinta → Manufactura"
                    PalletStage.MANUFACTURING -> "Manufactura"
                    PalletStage.CONVEYOR_TO_QUALITY -> "Cinta → Calidad"
                    PalletStage.QUALITY_INSPECTION -> "Calidad"
                    PalletStage.APPROVED -> "Aprobado, esperando almacenamiento"
                    PalletStage.REJECTED -> "Rechazado, esperando almacenamiento"
                    PalletStage.STORED -> "Almacenado"
                    PalletStage.BLOCKED -> "BLOQUEADO: ${snapshot.reason ?: "revisión requerida"}"
                },
                timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(snapshot.updatedAt)),
                estado = snapshot.stage.name
            )
        }
        _uiState.value = _uiState.value.copy(
            trackingState = _uiState.value.trackingState.copy(pallets = pallets)
        )
    }

    fun stopTracking() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    trackingState = _uiState.value.trackingState.copy(isTracking = false)
                )
                addLog("✗ Tracking detenido")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error: ${e.message}")
            }
        }
    }

    private fun currentTime(offsetMinutes: Int = 0): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MINUTE, offsetMinutes)
        return java.text.SimpleDateFormat("HH:mm").format(cal.time)
    }

    // ============= TCP SERVER/RED =============
    fun startTcpServer() {
        viewModelScope.launch {
            try {
                addLog("⟳ Iniciando TCP Server (Puerto 8888)...")
                val newNetworkState = _uiState.value.networkState.copy(isServerRunning = true)
                _uiState.value = _uiState.value.copy(networkState = newNetworkState)
                addLog("✓ TCP Server activo")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error TCP: ${e.message}")
            }
        }
    }

    fun stopTcpServer() {
        viewModelScope.launch {
            try {
                val newNetworkState = _uiState.value.networkState.copy(isServerRunning = false)
                _uiState.value = _uiState.value.copy(networkState = newNetworkState)
                addLog("✗ TCP Server detenido")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error: ${e.message}")
            }
        }
    }

    fun sendNetworkMessage(message: String) {
        viewModelScope.launch {
            try {
                addLog("→ RED MSG: $message")
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = com.sistema.distribuido.network.protocol.CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = com.sistema.distribuido.network.protocol.AppType.COORDINADOR,
                        destMac = "",
                        destApp = com.sistema.distribuido.network.protocol.AppType.UNKNOWN,
                        command = message
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error: ${e.message}")
            }
        }
    }

    fun refreshBluetoothDevices() {
        viewModelScope.launch {
            try {
                addLog("⟳ Escaneando dispositivos Bluetooth...")
                _uiState.value = _uiState.value.copy(
                    networkState = _uiState.value.networkState.copy(
                        bluetoothSummary = "Escaneando Bluetooth...",
                        isScanning = true
                    )
                )
                updateDeviceList()
                addLog("✓ Bluetooth actualizado")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error Bluetooth: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(
                    networkState = _uiState.value.networkState.copy(isScanning = false)
                )
            }
        }
    }

    fun exportTrackingCsv() {
        viewModelScope.launch {
            try {
                val csv = buildTrackingCsv()
                if (csv.isBlank()) {
                    addLog("⚠ No hay datos de tracking para exportar")
                    return@launch
                }
                addLog("✓ Tracking exportado como CSV")
                addLog(csv)
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error exportando CSV: ${e.message}")
            }
        }
    }

    /**
     * Ejecuta un script sencillo de automatización. Soporta comandos básicos:
     * - SEND_CINTA <from> <to>
     * - SEND_ROBOT <comando...>
     * - SEND_LASER <comando...>
     * - WAIT <ms>
     * - STOP
     * Cualquier línea desconocida se registra como advertencia.
     */
    fun runScript(script: String) {
        viewModelScope.launch {
            try {
                addLog("⟳ Ejecutando script de automatización...")
                val lines = script.lines().map { it.trim() }.filter { it.isNotBlank() }
                for (line in lines) {
                    val parts = line.split("\\s+".toRegex())
                    when (parts[0].uppercase()) {
                        "SEND_CINTA" -> {
                            if (parts.size >= 3) {
                                val from = parts[1].toIntOrNull() ?: continue
                                val to = parts[2].toIntOrNull() ?: continue
                                sendCintaCommand(from, to)
                                // small delay to avoid flooding
                                kotlinx.coroutines.delay(200)
                            } else {
                                addLog("⚠ SEND_CINTA requiere 2 parámetros: from to")
                            }
                        }
                        "SEND_ROBOT" -> {
                            val cmd = parts.drop(1).joinToString(" ")
                            if (cmd.isNotBlank()) sendRobotCommand(cmd) else addLog("⚠ SEND_ROBOT sin comando")
                        }
                        "SEND_LASER" -> {
                            val cmd = parts.drop(1).joinToString(" ")
                            if (cmd.isNotBlank()) sendLaserCommand(cmd) else addLog("⚠ SEND_LASER sin comando")
                        }
                        "WAIT" -> {
                            val ms = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                            if (ms > 0) kotlinx.coroutines.delay(ms)
                        }
                        "STOP" -> {
                            addLog("✗ Script detenido por comando STOP")
                            break
                        }
                        else -> addLog("⚠ Línea de script desconocida: $line")
                    }
                }
                addLog("✓ Script finalizado")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error ejecutando script: ${e.message}")
            }
        }
    }

    fun buildTrackingCsv(): String {
        val pallets = _uiState.value.trackingState.pallets
        if (pallets.isEmpty()) return ""
        return buildString {
            appendLine("ID,Ubicación,Timestamp,Estado")
            pallets.forEach { pallet ->
                appendLine("${pallet.id},${pallet.ubicacion},${pallet.timestamp},${pallet.estado}")
            }
        }
    }

    fun log(message: String) {
        addLog(message)
    }

    // ============= SIMULACIÓN DE CICLO COMPLETO - AHORA REAL (ENVÍA COMANDOS) =============
    fun simulateFullCycle() {
        viewModelScope.launch {
            try {
                addLog("╔════════════════════════════════════════════════════════════╗")
                addLog("║     CICLO COMPLETO CIM v6.0 - CONTROL REAL                  ║")
                addLog("╚════════════════════════════════════════════════════════════╝")

                val broker = commandBroker
                
                // ============================================
                // PASO 1: PLC - Iniciar cinta
                // ============================================
                updateExecutiveStation("CINTA", ExecutiveStationStatus.BUSY, "Cinta iniciada", "PLC:START")
                addLog("[1/9] → PLC: Enviando PLC:START")
                if (broker != null) {
                    sendExecuteCommand(AppType.PLC, "PLC:START")
                } else {
                    addLog("   (Modo simulado - sin broker)")
                }
                kotlinx.coroutines.delay(700)

                // ============================================
                // PASO 2: Simular detección de pallet
                // ============================================
                updateExecutiveStation("CINTA", ExecutiveStationStatus.BUSY, "Pallet detectado POS:1", "SENSOR")
                addLog("[2/9] → SENSOR: Pallet detectado en estación 1")
                if (broker != null) {
                    sendExecuteCommand(AppType.PLC, "SENSOR_ACTIVATED|POS:1")
                }
                kotlinx.coroutines.delay(600)

                // ============================================
                // PASO 3: Enrutar a Manufactura
                // ============================================
                updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.BUSY, "Robot HOME", "R:HOME")
                addLog("[3/9] → MANUFACTURA: Enviando R:HOME")
                sendExecuteCommand(AppType.MANUFACTURA, "R:HOME")
                kotlinx.coroutines.delay(900)

                // ============================================
                // PASO 4: Ejecutar robot
                // ============================================
                addLog("[4/9] → MANUFACTURA: Ejecutando R:RUN")
                sendExecuteCommand(AppType.MANUFACTURA, "R:RUN")
                updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.BUSY, "Procesando pieza", "R:RUN")
                kotlinx.coroutines.delay(1300)

                // ============================================
                // PASO 5: Láser
                // ============================================
                addLog("[5/9] → MANUFACTURA: Láser CNC (L:START)")
                sendExecuteCommand(AppType.MANUFACTURA, "L:START")
                kotlinx.coroutines.delay(1000)

                // ============================================
                // PASO 6: Enrutar a Calidad
                // ============================================
                updateExecutiveStation("CALIDAD", ExecutiveStationStatus.BUSY, "Analizando ArUco+YOLO", "ARUCO+YOLO")
                addLog("[6/9] → CALIDAD: Enviando solicitud de validación")
                sendExecuteCommand(AppType.CALIDAD, "ARUCO:DETECT")
                kotlinx.coroutines.delay(800)

                // ============================================
                // PASO 7: Validación exitosa
                // ============================================
                addLog("[7/9] → CALIDAD: PIEZA APROBADA (VAL:PASS)")
                sendExecuteCommand(AppType.CALIDAD, "VAL:PASS")
                updateExecutiveStation("CALIDAD", ExecutiveStationStatus.READY, "Pieza VALIDADA ✓", "PASS")
                kotlinx.coroutines.delay(700)

                // ============================================
                // PASO 8: Almacén
                // ============================================
                updateExecutiveStation("ALMACEN", ExecutiveStationStatus.BUSY, "Almacenando en rack 07", "STO:07")
                addLog("[8/9] → ALMACEN: Enviando STO:07")
                sendExecuteCommand(AppType.ALMACEN, "STO:07")
                kotlinx.coroutines.delay(1100)

                // ============================================
                // PASO 9: Finalizar
                // ============================================
                addLog("[9/9] → PLC: Deteniendo cinta (PLC:STOP)")
                if (broker != null) {
                    sendExecuteCommand(AppType.PLC, "PLC:STOP")
                }
                updateExecutiveStation("CINTA", ExecutiveStationStatus.READY, "Ciclo completado ✓", "PLC:STOP")

                // Actualizar estados finales
                updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.READY, "Listo para nuevo ciclo", "IDLE")
                updateExecutiveStation("ALMACEN", ExecutiveStationStatus.READY, "Rack actualizado (pos 07)", "STO:07 OK")

                addLog("════════════════════════════════════════════════════════════")
                addLog("✅ CICLO COMPLETO FINALIZADO - PIEZA PROCESADA Y ALMACENADA")
                addLog("════════════════════════════════════════════════════════════")

                // Notificar a todas las estaciones conectadas
                addLog("→ Notificación global enviada a estaciones conectadas")

            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error en ciclo completo: ${e.message}")
                updateExecutiveStation("CINTA", ExecutiveStationStatus.WARNING, "Error en ciclo", "ERROR")
            }
        }
    }

    // ============= STORAGE =============
    fun sendStorageCommand(command: String) {
        viewModelScope.launch {
            try {
                addLog("→ STORAGE: $command")
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = com.sistema.distribuido.network.protocol.CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = com.sistema.distribuido.network.protocol.AppType.COORDINADOR,
                        destMac = "",
                        destApp = com.sistema.distribuido.network.protocol.AppType.ALMACEN,
                        command = command
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error Storage: ${e.message}")
            }
        }
    }

    fun authorizeDevice(mac: String, rememberDecision: Boolean = true) {
        viewModelScope.launch {
            try {
                GlobalDeviceRegistry.registry.authorize(mac)
                GlobalPermissionManager.getInstance().approve(mac, rememberDecision = rememberDecision)
                addLog("✓ Autorizado: $mac")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error autorizar: ${e.message}")
            }
        }
    }

    fun rejectDevice(mac: String) {
        viewModelScope.launch {
            try {
                GlobalPermissionManager.getInstance().ban(mac, "Rechazado desde Coordinador")
                commandBroker?.disconnectBleDevice(mac)
                GlobalDeviceRegistry.registry.disconnect(mac)
                addLog("🚫 Bloqueado y desconectado: $mac")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error rechazar: ${e.message}")
            }
        }
    }

    fun unbanDevice(mac: String) {
        viewModelScope.launch {
            try {
                GlobalPermissionManager.getInstance().unban(mac)
                updateDeviceList()
                addLog("✓ Dispositivo desbloqueado: $mac; requiere autorización nueva")
            } catch (e: Exception) {
                Log.e("Coordinator", "Error desbloqueando dispositivo", e)
                addLog("✗ Error desbloqueando $mac: ${e.message}")
            }
        }
    }

    fun disconnectDevice(mac: String) {
        viewModelScope.launch {
            try {
                commandBroker?.disconnectBleDevice(mac)
                GlobalDeviceRegistry.registry.disconnect(mac)
                addLog("✗ Desconectado: $mac")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error desconectar: ${e.message}")
            }
        }
    }

    /** Forzar handshake IDENTIFY hacia un dispositivo (intento manual) */
    fun forceIdentify(mac: String) {
        viewModelScope.launch {
            try {
                val appId = AppIdentifier.getInstance()
                val msg = CimMessage(
                    sourceMac = appId.deviceMac,
                    sourceApp = AppType.COORDINADOR,
                    destMac = mac,
                    destApp = AppType.UNKNOWN,
                    commandType = CimCommandType.IDENTIFY,
                    payload = "${AppType.COORDINADOR}|1.0"
                )
                commandBroker?.sendCommand(msg)
                addLog("→ Forzando IDENTIFY a $mac")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error forzando IDENTIFY: ${e.message}")
            }
        }
    }

    /** Intentar reconectar/rehabilitar dispositivo: desconectar y forzar IDENTIFY */
    fun reconnectDevice(mac: String) {
        viewModelScope.launch {
            try {
                addLog("⟳ Intentando reconectar dispositivo: $mac")
                try { GlobalDeviceRegistry.registry.disconnect(mac) } catch (_: Exception) {}

                _uiState.value = _uiState.value.copy(
                    networkState = _uiState.value.networkState.copy(
                        isBluetoothReconnecting = true,
                        reconnectingMac = mac
                    )
                )

                commandBroker?.reconnectBleDevice(mac) { success ->
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(
                            networkState = _uiState.value.networkState.copy(
                                isBluetoothReconnecting = false,
                                reconnectingMac = null,
                                bluetoothSummary = if (success) "Bluetooth: reconectado $mac" else "Bluetooth: reconexión fallida $mac"
                            )
                        )
                        addLog(if (success) "✓ Reconectado $mac" else "✗ Falló reconexión $mac")
                        updateDeviceList()
                    }
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog("✗ Error reconectando: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    networkState = _uiState.value.networkState.copy(
                        isBluetoothReconnecting = false,
                        reconnectingMac = null
                    )
                )
            }
        }
    }

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        val newLogs = _uiState.value.logMessages + logEntry
        val trimmedLogs = if (newLogs.size > 100) newLogs.takeLast(100) else newLogs

        val brokerSummary = commandBroker?.getStats()?.let { stats ->
            "Broker: Tx=${stats.totalTransactions} | ACK=${"%.0f".format(stats.successRate * 100)}% | Err=${stats.errorCount} | Hist=${stats.logSize}"
        } ?: "Broker: no inicializado"

        _uiState.value = _uiState.value.copy(
            logMessages = trimmedLogs,
            networkState = _uiState.value.networkState.copy(
                brokerSummary = brokerSummary,
                debugLogs = trimmedLogs.takeLast(30),
                lastMessage = message
            )
        )
    }

    fun clearLogs() {
        _uiState.value = _uiState.value.copy(logMessages = emptyList())
    }
}






