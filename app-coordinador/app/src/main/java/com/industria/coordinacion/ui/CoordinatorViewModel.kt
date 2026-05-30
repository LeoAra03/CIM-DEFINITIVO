package com.industria.coordinacion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sistema.distribuido.network.GlobalDeviceRegistry
import com.sistema.distribuido.network.GlobalPermissionManager
import com.sistema.distribuido.network.CommandBroker
import com.sistema.distribuido.network.GlobalCommandBroker
import com.sistema.distribuido.network.PermissionRequest
import com.sistema.distribuido.network.AppIdentifier
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

data class CoordinatorUiState(
    val currentTabIndex: Int = 0,
    val cintaState: CintaPanelState = CintaPanelState(),
    val networkState: NetworkTabState = NetworkTabState(),
    val trackingState: TrackingState = TrackingState(),
    val qcState: QcProgramState = QcProgramState(),
    val pendingPermissionRequest: PermissionRequest? = null,
    val isAutoModeEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val logMessages: List<String> = emptyList()
)

class CoordinatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CoordinatorUiState())
    val uiState: StateFlow<CoordinatorUiState> = _uiState.asStateFlow()

    private val commandBroker: CommandBroker? = GlobalCommandBroker.getInstanceOrNull()

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
                    rssi = device.rssi
                )
            }
            val activeBluetooth = connectedDevices.count { it.isConnected }
            val bestRssi = connectedDevices.maxOfOrNull { it.rssi }

            _uiState.value = _uiState.value.copy(
                networkState = _uiState.value.networkState.copy(
                    connectedDevices = connectedDevices,
                    totalConnected = connectedDevices.size,
                    bluetoothSummary = "Bluetooth: $activeBluetooth conectados · Mejor RSSI: ${bestRssi ?: 0} dBm",
                    isAutoModeEnabled = _uiState.value.isAutoModeEnabled
                )
            )
        } catch (e: Exception) {
            addLog("⚠ Error actualizar devices: ${e.message}")
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
                addLog("✗ Error enviando ArUco detectado: ${e.message}")
            }
        }
    }

    // ============= TRACKING =============
    fun startTracking() {
        viewModelScope.launch {
            try {
                addLog("⟳ Iniciando Tracking...")
                val palletList = listOf(
                    PaletaTracking("PAL-001", "ALMACÉN L3", "${currentTime()}", "DISPONIBLE"),
                    PaletaTracking("PAL-002", "ROBOT SCORBOT", "${currentTime(offsetMinutes = -1)}", "PROCESANDO"),
                    PaletaTracking("PAL-003", "CINTA POS 5", "${currentTime(offsetMinutes = -3)}", "EN TRÁNSITO"),
                    PaletaTracking("PAL-004", "ESTACIÓN QC", "${currentTime(offsetMinutes = -8)}", "VALIDADO")
                )
                _uiState.value = _uiState.value.copy(
                    trackingState = _uiState.value.trackingState.copy(
                        isTracking = true,
                        pallets = palletList
                    )
                )
                addLog("✓ Tracking activo")
            } catch (e: Exception) {
                addLog("✗ Error Tracking: ${e.message}")
            }
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    trackingState = _uiState.value.trackingState.copy(isTracking = false)
                )
                addLog("✗ Tracking detenido")
            } catch (e: Exception) {
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
                addLog("✗ Error autorizar: ${e.message}")
            }
        }
    }

    fun rejectDevice(mac: String) {
        viewModelScope.launch {
            try {
                GlobalPermissionManager.getInstance().reject(mac, rememberDecision = true)
                GlobalDeviceRegistry.registry.disconnect(mac)
                addLog("✗ Rechazado y desconectado: $mac")
            } catch (e: Exception) {
                addLog("✗ Error rechazar: ${e.message}")
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






