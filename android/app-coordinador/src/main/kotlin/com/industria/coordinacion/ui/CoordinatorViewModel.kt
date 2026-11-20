package com.industria.coordinacion.ui

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.industria.prefecto.GlobalDeviceRegistry
import com.industria.prefecto.GlobalPermissionManager
import com.industria.prefecto.CommandBroker
import com.industria.prefecto.GlobalCommandBroker
import com.industria.prefecto.PermissionRequest
import com.industria.prefecto.AppIdentifier
import com.industria.prefecto.LogEvent
import com.industria.prefecto.PalletWorkflowEngine
import com.industria.prefecto.AppType
import com.industria.prefecto.CommandType as CimCommandType
import com.industria.prefecto.CimMessage
import com.industria.prefecto.CimMessageBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

/**
 * COORDINATOR VIEWMODEL
 *
 * Manages all central state:
 * - Connected devices
 * - Sent commands
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
                GlobalDeviceRegistry.registry.addListener(object : com.industria.prefecto.MobileDeviceRegistry.RegistryListener {
                    override suspend fun onDeviceAdded(device: com.industria.prefecto.DeviceInfo) {
                        updateDeviceList()
                        addLog(LogEvent.Severity.OK, "Dispositivo agregado: ${device.nombre} [${device.mac}]")
                    }

                    override suspend fun onDeviceRemoved(mac: String) {
                        updateDeviceList()
                        addLog(LogEvent.Severity.ERROR, "Dispositivo desconectado: $mac")
                    }

                    override suspend fun onDeviceUpdated(device: com.industria.prefecto.DeviceInfo) {
                        updateDeviceList()
                    }

                    override suspend fun onAuthorizationChanged(mac: String, authorized: Boolean) {
                        updateDeviceList()
                        addLog(if (authorized) LogEvent.Severity.OK else LogEvent.Severity.ERROR, "Autorización: $mac")
                    }
                })

                commandBroker?.addCommandReceivedListener { message ->
                    handleCommandResponse(message)
                }

                commandBroker?.addErrorListener { errorMsg ->
                    addLog(LogEvent.Severity.ERROR, "BROKER ERROR: $errorMsg")
                }
            } catch (e: Exception) {
                Log.w("CoordinatorVM", "Listener init error: ${e.message}", e)
                addLog(LogEvent.Severity.WARN, "Error inicializando listeners: ${e.message}")
            }
        }
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    updateDeviceList()
                    // Poll pending permission requests
                    val pending = try {
                        GlobalPermissionManager.getInstance().getPendingRequests()
                    } catch (e: Exception) {
                        Log.w("CoordinatorVM", "Error polling perms: ${e.message}")
                        emptyList()
                    }
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
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("CoordinatorVM", "Monitoring error: ${e.message}")
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun handleCommandResponse(response: CimMessage) {
        val statusMessage = "RESP ${response.commandType} de ${response.sourceApp}: ${response.payload}"
        addLog(LogEvent.Severity.INFO, statusMessage)
        _uiState.value = _uiState.value.copy(
            networkState = _uiState.value.networkState.copy(lastMessage = statusMessage)
        )

        // Update QC state when we receive an ACK/NACK for a program
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
                addLog(LogEvent.Severity.INFO, "Iniciando QC $program")
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
                addLog(LogEvent.Severity.ERROR, "Error QC $program: ${e.message}")
            }
        }
    }

    fun stopQcProgram(program: String) {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.ERROR, "Deteniendo QC $program")
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
                addLog(LogEvent.Severity.ERROR, "Error detener QC $program: ${e.message}")
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
            addLog(LogEvent.Severity.WARN, "Error actualizar devices: ${e.message}")
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
            addLog(LogEvent.Severity.INFO, "FLUJO: Almacén reportó pallet liberado; Manufactura preparada")
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
                addLog(LogEvent.Severity.INFO, "FLUJO: Inicio de planta completa")
                sendExecuteCommand(AppType.MANUFACTURA, "PLANTA_START")
                sendExecuteCommand(AppType.PLC, "PLANTA_START")
                sendExecuteCommand(AppType.ALMACEN, "PLANTA_START")
                sendExecuteCommand(AppType.CALIDAD, "PLANTA_START")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error inicio planta completa: ${e.message}")
            }
        }
    }

    fun calibrateGlobal() {
        viewModelScope.launch {
            try {
                updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.READY, "Calibración global OK", "CALIBRACIÓN")
                updateExecutiveStation("CALIDAD", ExecutiveStationStatus.READY, "Visión calibrada", "CALIBRACIÓN")
                updateExecutiveStation("CINTA", ExecutiveStationStatus.ONLINE, "Cinta calibrada", "CALIBRACIÓN")
                addLog(LogEvent.Severity.INFO, "FLUJO: Calibración global ejecutada")
                sendExecuteCommand(AppType.MANUFACTURA, "CALIBRATE_GLOBAL")
                sendExecuteCommand(AppType.PLC, "CALIBRATE_GLOBAL")
                sendExecuteCommand(AppType.CALIDAD, "CALIBRATE_GLOBAL")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error calibración global: ${e.message}")
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
                addLog(LogEvent.Severity.WARN, "E-STOP activado: se frenan rutinas Scorbot y pallets Omron")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error E-STOP: ${e.message}")
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
                    addLog(LogEvent.Severity.OK, "Modo AUTO activado: se aprobarán permisos automáticamente")
                } else {
                    addLog(LogEvent.Severity.ERROR, "Modo AUTO desactivado: autorizaciones manuales habilitadas")
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error cambiando modo AUTO: ${e.message}")
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
                addLog(LogEvent.Severity.ERROR, "Error enviando comando $command: ${e.message}")
            }
        }
    }

    // ============= CONVEYOR (system) =============
    fun sendCintaCommand(fromStation: Int, toStation: Int) {
        viewModelScope.launch {
            try {
                val cmd = "DELIVER:$fromStation:$toStation"
                addLog(LogEvent.Severity.INFO, "CINTA DELIVER: $fromStation -> $toStation")
                // Send the command through the CommandBroker when available
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = com.industria.prefecto.CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = com.industria.prefecto.AppType.COORDINADOR,
                        destMac = "",
                        destApp = com.industria.prefecto.AppType.PLC,
                        command = cmd
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error Cinta: ${e.message}")
            }
        }
    }

    fun sendFreeCommand(fromStation: Int, toStation: Int) {
        viewModelScope.launch {
            try {
                val cmd = "FREE:$fromStation:$toStation"
                addLog(LogEvent.Severity.INFO, "CINTA FREE: $fromStation -> $toStation")
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = com.industria.prefecto.CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = com.industria.prefecto.AppType.COORDINADOR,
                        destMac = "",
                        destApp = com.industria.prefecto.AppType.PLC,
                        command = cmd
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error: ${e.message}")
            }
        }
    }

    fun connectCinta() {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "Conectando Cinta...")
                val newCintaState = _uiState.value.cintaState.copy(isConnected = true)
                _uiState.value = _uiState.value.copy(cintaState = newCintaState)
                addLog(LogEvent.Severity.OK, "Cinta conectada")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error conexión Cinta: ${e.message}")
            }
        }
    }

    fun disconnectCinta() {
        viewModelScope.launch {
            try {
                val newCintaState = _uiState.value.cintaState.copy(isConnected = false)
                _uiState.value = _uiState.value.copy(cintaState = newCintaState)
                addLog(LogEvent.Severity.ERROR, "Cinta desconectada")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error desconexión: ${e.message}")
            }
        }
    }

    fun resetCinta() {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "Reseteando Cinta...")
                addLog(LogEvent.Severity.OK, "Cinta reseteada")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error reset: ${e.message}")
            }
        }
    }

    // ============= ROBOT & LASER =============
    fun sendRobotCommand(command: String) {
        addLog(LogEvent.Severity.INFO, "ROBOT: $command")
        sendExecuteCommand(AppType.MANUFACTURA, command)
    }

    fun sendLaserCommand(command: String) {
        addLog(LogEvent.Severity.INFO, "LASER: $command")
        sendExecuteCommand(AppType.MANUFACTURA, command)
    }

    fun sendLaserLoadFile(filename: String, base64Content: String) {
        addLog(LogEvent.Severity.INFO, "LASER LOAD: $filename")
        sendExecuteCommand(AppType.MANUFACTURA, "LASER_LOAD:$filename:$base64Content")
    }

    // ============= ARUCO =============
    fun generateAruco(seed: String) {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "Generando ArUco: $seed")
                sendExecuteCommand(AppType.MANUFACTURA, "ARUCO_GENERATE:$seed")
                addLog(LogEvent.Severity.OK, "Solicitud ArUco enviada")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error ArUco: ${e.message}")
            }
        }
    }

    fun handleArucoDetected(aruco: DetectedArUco) {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "ArUco detectado: ID=${aruco.id} centro=(${aruco.center.first.toInt()},${aruco.center.second.toInt()}) rot=${aruco.rotation.toInt()}°")
                val payload = "ARUCO_DETECTED:${aruco.id}|X:${aruco.center.first.toInt()}|Y:${aruco.center.second.toInt()}|R:${aruco.rotation.toInt()}"
                sendExecuteCommand(AppType.MANUFACTURA, payload)
                addLog(LogEvent.Severity.OK, "Enviado evento ArUco detectado")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error enviando ArUco detectado: ${e.message}")
            }
        }
    }

    // ============= TRACKING =============
    fun startTracking() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                trackingState = _uiState.value.trackingState.copy(isTracking = true)
            )
            addLog(LogEvent.Severity.OK, "Tracking activo: esperando eventos de estaciones")
        }
    }

    /** Processes traceable events: PALLET:<id>|EVENT:<Stage>|ARUCO:<id>|PRODUCT:<id>. */
    private fun handlePalletWireEvent(rawEvent: String) {
        val fields = rawEvent.split("|").associate { token ->
            val parts = token.split(":", limit = 2)
            (parts.getOrNull(0)?.uppercase() ?: "") to (parts.getOrNull(1) ?: "")
        }
        val palletId = fields["PALLET"]?.trim().orEmpty()
        val stageName = fields["EVENT"]?.trim().orEmpty()
        if (palletId.isBlank() || stageName.isBlank()) return

        viewModelScope.launch {
            if (palletWorkflow.stageOf(palletId) == null) {
                palletWorkflow.register(palletId)
            }
            val stage = runCatching {
                PalletWorkflowEngine.Stage.valueOf(stageName.uppercase())
            }.getOrNull()
            if (stage == null) {
                addLog(LogEvent.Severity.WARN, "Evento de pallet inválido: $stageName")
                return@launch
            }
            val result = palletWorkflow.apply(palletId, stage, rawEvent)
            refreshPalletTracking()
            addLog(
                if (result.accepted) LogEvent.Severity.OK else LogEvent.Severity.ERROR,
                "PALLET $palletId: ${result.to.name}",
            )
        }
    }

    private fun refreshPalletTracking() {
        val pallets = palletWorkflow.palletIds().map { palletId ->
            val stage = palletWorkflow.stageOf(palletId) ?: PalletWorkflowEngine.Stage.REGISTERED
            PaletaTracking(
                id = palletId,
                ubicacion = when (stage) {
                    PalletWorkflowEngine.Stage.REGISTERED -> "Registro / Almacén"
                    PalletWorkflowEngine.Stage.STORAGE_RELEASED -> "Salida de Almacén"
                    PalletWorkflowEngine.Stage.CONVEYOR_TO_MANUFACTURING -> "Cinta a Manufactura"
                    PalletWorkflowEngine.Stage.MANUFACTURING -> "Manufactura"
                    PalletWorkflowEngine.Stage.CONVEYOR_TO_QUALITY -> "Cinta a Calidad"
                    PalletWorkflowEngine.Stage.QUALITY_INSPECTION -> "Calidad"
                    PalletWorkflowEngine.Stage.APPROVED -> "Aprobado, esperando almacenamiento"
                    PalletWorkflowEngine.Stage.REJECTED -> "Rechazado, esperando almacenamiento"
                    PalletWorkflowEngine.Stage.STORED -> "Almacenado"
                    PalletWorkflowEngine.Stage.BLOCKED ->
                        "BLOQUEADO: ${palletWorkflow.reasonOf(palletId) ?: "revisión requerida"}"
                },
                timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date()),
                estado = stage.name,
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
                addLog(LogEvent.Severity.ERROR, "Tracking detenido")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error: ${e.message}")
            }
        }
    }

    private fun currentTime(offsetMinutes: Int = 0): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MINUTE, offsetMinutes)
        return java.text.SimpleDateFormat("HH:mm").format(cal.time)
    }

    // ============= TCP SERVER / NETWORK =============
    fun startTcpServer() {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "Iniciando TCP Server (Puerto 8888)...")
                val newNetworkState = _uiState.value.networkState.copy(isServerRunning = true)
                _uiState.value = _uiState.value.copy(networkState = newNetworkState)
                addLog(LogEvent.Severity.OK, "TCP Server activo")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error TCP: ${e.message}")
            }
        }
    }

    fun simulateDemoStation() {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "Conectando estación demo (simulación, sin hardware)...")
                val demoDevice = ConnectedDevice(
                    mac = "AA:BB:CC:00:11:22",
                    appType = "PLC",
                    name = "CINTA (DEMO)",
                    isConnected = true,
                    isAuthorized = true,
                    rssi = -45,
                    ip = "192.168.1.101",
                    stationUuid = "CIM-ST-DEMO-1",
                    version = "6.0.0",
                    hardwareModel = "SIMULADA",
                    capabilities = "CINTA,SENSORES"
                )
                val current = _uiState.value.networkState
                val newDevices = current.connectedDevices.filterNot { it.mac == demoDevice.mac } + demoDevice
                _uiState.value = _uiState.value.copy(
                    networkState = current.copy(
                        isServerRunning = true,
                        connectedDevices = newDevices,
                        totalConnected = newDevices.size,
                        pendingRequestCount = 0,
                        pendingRequestSummary = "Sin solicitudes pendientes",
                        lastMessage = "Estación demo conectada"
                    )
                )
                updateExecutiveStation("CINTA", ExecutiveStationStatus.READY, "Conectada (demo)", "Estación demo en línea")
                addLog(LogEvent.Severity.OK, "Estación demo conectada (1 dispositivo simulado) — modo presentación listo")
            } catch (e: Exception) {
                Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.WARN, "Error simulando estación: ${e.message}")
            }
        }
    }

    fun stopTcpServer() {
        viewModelScope.launch {
            try {
                val newNetworkState = _uiState.value.networkState.copy(isServerRunning = false)
                _uiState.value = _uiState.value.copy(networkState = newNetworkState)
                addLog(LogEvent.Severity.ERROR, "TCP Server detenido")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error: ${e.message}")
            }
        }
    }

    fun sendNetworkMessage(message: String) {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "RED MSG: $message")
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = com.industria.prefecto.CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = com.industria.prefecto.AppType.COORDINADOR,
                        destMac = "",
                        destApp = com.industria.prefecto.AppType.UNKNOWN,
                        command = message
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error: ${e.message}")
            }
        }
    }

    fun refreshBluetoothDevices() {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "Escaneando dispositivos Bluetooth...")
                _uiState.value = _uiState.value.copy(
                    networkState = _uiState.value.networkState.copy(
                        bluetoothSummary = "Escaneando Bluetooth...",
                        isScanning = true
                    )
                )
                updateDeviceList()
                addLog(LogEvent.Severity.OK, "Bluetooth actualizado")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error Bluetooth: ${e.message}")
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
                    addLog(LogEvent.Severity.WARN, "No hay datos de tracking para exportar")
                    return@launch
                }
                addLog(LogEvent.Severity.OK, "Tracking exportado como CSV")
                addLog(LogEvent.Severity.INFO, csv)
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error exportando CSV: ${e.message}")
            }
        }
    }

    /**
     * Runs a simple automation script. Supported basic commands:
     * - SEND_CINTA <from> <to>
     * - SEND_ROBOT <command...>
     * - SEND_LASER <command...>
     * - WAIT <ms>
     * - STOP
     * Cualquier línea desconocida se registra como advertencia.
     */
    fun runScript(script: String) {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "Ejecutando script de automatización...")
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
                                addLog(LogEvent.Severity.WARN, "SEND_CINTA requiere 2 parámetros: from to")
                            }
                        }
                        "SEND_ROBOT" -> {
                            val cmd = parts.drop(1).joinToString(" ")
                            if (cmd.isNotBlank()) sendRobotCommand(cmd) else addLog(LogEvent.Severity.WARN, "SEND_ROBOT sin comando")
                        }
                        "SEND_LASER" -> {
                            val cmd = parts.drop(1).joinToString(" ")
                            if (cmd.isNotBlank()) sendLaserCommand(cmd) else addLog(LogEvent.Severity.WARN, "SEND_LASER sin comando")
                        }
                        "WAIT" -> {
                            val ms = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                            if (ms > 0) kotlinx.coroutines.delay(ms)
                        }
                        "STOP" -> {
                            addLog(LogEvent.Severity.ERROR, "Script detenido por comando STOP")
                            break
                        }
                        else -> addLog(LogEvent.Severity.WARN, "Línea de script desconocida: $line")
                    }
                }
                addLog(LogEvent.Severity.OK, "Script finalizado")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error ejecutando script: ${e.message}")
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
        addLog(LogEvent.Severity.INFO, message)
    }

    // ============= FULL CYCLE SIMULATION - REAL COMMAND SENDER =============
    fun simulateFullCycle() {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "+============================================================+")
                addLog(LogEvent.Severity.INFO, "|     CICLO COMPLETO CIM v6.0 - CONTROL REAL                  |")
                addLog(LogEvent.Severity.INFO, "+============================================================+")

                val broker = commandBroker
                
                // ============================================
                // STEP 1: PLC - start the conveyor
                // ============================================
                updateExecutiveStation("CINTA", ExecutiveStationStatus.BUSY, "Cinta iniciada", "PLC:START")
                addLog(LogEvent.Severity.INFO, "[1/9] -> PLC: Enviando PLC:START")
                if (broker != null) {
                    sendExecuteCommand(AppType.PLC, "PLC:START")
                } else {
                    addLog(LogEvent.Severity.INFO, "   (Modo simulado - sin broker)")
                }
                kotlinx.coroutines.delay(700)

                // ============================================
                // PASO 2: Simular detección de pallet
                // ============================================
                updateExecutiveStation("CINTA", ExecutiveStationStatus.BUSY, "Pallet detectado POS:1", "SENSOR")
                addLog(LogEvent.Severity.INFO, "[2/9] -> SENSOR: Pallet detectado en estación 1")
                if (broker != null) {
                    sendExecuteCommand(AppType.PLC, "SENSOR_ACTIVATED|POS:1")
                }
                kotlinx.coroutines.delay(600)

                // ============================================
                // PASO 3: Enrutar a Manufactura
                // ============================================
                updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.BUSY, "Robot HOME", "R:HOME")
                addLog(LogEvent.Severity.INFO, "[3/9] -> MANUFACTURA: Enviando R:HOME")
                sendExecuteCommand(AppType.MANUFACTURA, "R:HOME")
                kotlinx.coroutines.delay(900)

                // ============================================
                // PASO 4: Ejecutar robot
                // ============================================
                addLog(LogEvent.Severity.INFO, "[4/9] -> MANUFACTURA: Ejecutando R:RUN")
                sendExecuteCommand(AppType.MANUFACTURA, "R:RUN")
                updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.BUSY, "Procesando pieza", "R:RUN")
                kotlinx.coroutines.delay(1300)

                // ============================================
                // PASO 5: Láser
                // ============================================
                addLog(LogEvent.Severity.INFO, "[5/9] -> MANUFACTURA: Láser CNC (L:START)")
                sendExecuteCommand(AppType.MANUFACTURA, "L:START")
                kotlinx.coroutines.delay(1000)

                // ============================================
                // PASO 6: Enrutar a Calidad
                // ============================================
                updateExecutiveStation("CALIDAD", ExecutiveStationStatus.BUSY, "Analizando ArUco+YOLO", "ARUCO+YOLO")
                addLog(LogEvent.Severity.INFO, "[6/9] -> CALIDAD: Enviando solicitud de validación")
                sendExecuteCommand(AppType.CALIDAD, "ARUCO:DETECT")
                kotlinx.coroutines.delay(800)

                // ============================================
                // STEP 7: validation succeeded
                // ============================================
                addLog(LogEvent.Severity.INFO, "[7/9] -> CALIDAD: PIEZA APROBADA (VAL:PASS)")
                sendExecuteCommand(AppType.CALIDAD, "VAL:PASS")
                updateExecutiveStation("CALIDAD", ExecutiveStationStatus.READY, "Pieza VALIDADA", "PASS")
                kotlinx.coroutines.delay(700)

                // ============================================
                // PASO 8: Almacén
                // ============================================
                updateExecutiveStation("ALMACEN", ExecutiveStationStatus.BUSY, "Almacenando en rack 07", "STO:07")
                addLog(LogEvent.Severity.INFO, "[8/9] -> ALMACEN: Enviando STO:07")
                sendExecuteCommand(AppType.ALMACEN, "STO:07")
                kotlinx.coroutines.delay(1100)

                // ============================================
                // PASO 9: Finalizar
                // ============================================
                addLog(LogEvent.Severity.INFO, "[9/9] -> PLC: Deteniendo cinta (PLC:STOP)")
                if (broker != null) {
                    sendExecuteCommand(AppType.PLC, "PLC:STOP")
                }
                updateExecutiveStation("CINTA", ExecutiveStationStatus.READY, "Ciclo completado", "PLC:STOP")

                // Update final states
                updateExecutiveStation("MANUFACTURA", ExecutiveStationStatus.READY, "Listo para nuevo ciclo", "IDLE")
                updateExecutiveStation("ALMACEN", ExecutiveStationStatus.READY, "Rack actualizado (pos 07)", "STO:07 OK")

                addLog(LogEvent.Severity.INFO, "============================================================")
                addLog(LogEvent.Severity.INFO, "CICLO COMPLETO FINALIZADO - PIEZA PROCESADA Y ALMACENADA")
                addLog(LogEvent.Severity.INFO, "============================================================")

                // Notify every connected station
                addLog(LogEvent.Severity.INFO, "Notificación global enviada a estaciones conectadas")

            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error en ciclo completo: ${e.message}")
                updateExecutiveStation("CINTA", ExecutiveStationStatus.WARNING, "Error en ciclo", "ERROR")
            }
        }
    }

    // ============= STORAGE =============
    fun sendStorageCommand(command: String) {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "STORAGE: $command")
                val broker = commandBroker
                if (broker != null) {
                    val appId = AppIdentifier.getInstance()
                    val msg = com.industria.prefecto.CimMessageBuilder.createExecuteCommand(
                        sourceMac = appId.deviceMac,
                        sourceApp = com.industria.prefecto.AppType.COORDINADOR,
                        destMac = "",
                        destApp = com.industria.prefecto.AppType.ALMACEN,
                        command = command
                    )
                    broker.sendCommand(msg)
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error Storage: ${e.message}")
            }
        }
    }

    fun authorizeDevice(mac: String, rememberDecision: Boolean = true) {
        viewModelScope.launch {
            try {
                GlobalDeviceRegistry.registry.authorize(mac)
                GlobalPermissionManager.getInstance().approve(mac, rememberDecision = rememberDecision)
                addLog(LogEvent.Severity.OK, "Autorizado: $mac")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error autorizar: ${e.message}")
            }
        }
    }

    fun rejectDevice(mac: String) {
        viewModelScope.launch {
            try {
                GlobalPermissionManager.getInstance().ban(mac, "Rechazado desde Coordinador")
                commandBroker?.disconnectBleDevice(mac)
                GlobalDeviceRegistry.registry.disconnect(mac)
                addLog(LogEvent.Severity.INFO, "Bloqueado y desconectado: $mac")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error rechazar: ${e.message}")
            }
        }
    }

    fun unbanDevice(mac: String) {
        viewModelScope.launch {
            try {
                GlobalPermissionManager.getInstance().unban(mac)
                updateDeviceList()
                addLog(LogEvent.Severity.OK, "Dispositivo desbloqueado: $mac; requiere autorización nueva")
            } catch (e: Exception) {
                Log.e("Coordinator", "Error desbloqueando dispositivo", e)
                addLog(LogEvent.Severity.ERROR, "Error desbloqueando $mac: ${e.message}")
            }
        }
    }

    fun disconnectDevice(mac: String) {
        viewModelScope.launch {
            try {
                commandBroker?.disconnectBleDevice(mac)
                GlobalDeviceRegistry.registry.disconnect(mac)
                addLog(LogEvent.Severity.ERROR, "Desconectado: $mac")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error desconectar: ${e.message}")
            }
        }
    }

    /** Force an IDENTIFY handshake toward a device (manual attempt). */
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
                addLog(LogEvent.Severity.INFO, "Forzando IDENTIFY a $mac")
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error forzando IDENTIFY: ${e.message}")
            }
        }
    }

    /** Try to reconnect and rehabilitate a device: disconnect and force IDENTIFY. */
    fun reconnectDevice(mac: String) {
        viewModelScope.launch {
            try {
                addLog(LogEvent.Severity.INFO, "Intentando reconectar dispositivo: $mac")
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
                        addLog(if (success) LogEvent.Severity.OK else LogEvent.Severity.ERROR, "Reconectado $mac")
                        updateDeviceList()
                    }
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                addLog(LogEvent.Severity.ERROR, "Error reconectando: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    networkState = _uiState.value.networkState.copy(
                        isBluetoothReconnecting = false,
                        reconnectingMac = null
                    )
                )
            }
        }
    }

    private fun addLog(severity: LogEvent.Severity, message: String) {
        val logEntry = LogEvent(severity, message).format()
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






