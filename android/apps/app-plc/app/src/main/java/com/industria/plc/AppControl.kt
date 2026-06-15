package com.example.plc

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sistema.distribuido.network.StationClient
import com.sistema.distribuido.network.BluetoothIdentityHelper
import com.sistema.distribuido.network.IndustrialErrorManager
import com.sistema.distribuido.network.protocol.CimProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AppControl(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    var ip = "192.168.1.15"
    var macAddress = "00:00:00:00:00:00"
    
    val estaciones = mutableStateListOf(false, false, false)
    val historialLogs = mutableStateListOf<String>()
    var estadoConexion = mutableStateOf("Offline")
    var authorizationState = mutableStateOf(CimProtocol.AUTH_STATE_DISCONNECTED)
    var stationClient: StationClient? = null

    init {
        BluetoothIdentityHelper(context).getStationMac { mac ->
            macAddress = mac
        }
    }

    fun initAndConnect() {
        log("CONECTANDO A MALLA CIM...")
        viewModelScope.launch(Dispatchers.IO) {
            stationClient = StationClient(
                host = ip,
                port = 8888,
                stationName = "PLC",
                password = CimProtocol.PASSWORD_ACTUAL,
                stationUuid = "CIM-ST-PLC-X4",
                macAddress = macAddress
            ).apply {
                onLog = { log(it) }
                onStatusChanged = { connected -> 
                    estadoConexion.value = if (connected) "Online" else "Offline"
                }
                onAuthorizationStateChanged = { newState -> 
                    this@AppControl.authorizationState.value = newState
                    log("TCP AUTH: $newState")
                }
                onCommandReceived = { processCommand(it) }
                connect()
            }
        }
    }

    fun processCommand(cmd: String) {
        log("MESH <- $cmd")
        when (cmd) {
            "START" -> runPlcTriggerSequence()
            "RESET" -> {
                estaciones[0] = false; estaciones[1] = false; estaciones[2] = false
            }
        }
    }

    fun runPlcTriggerSequence() {
        viewModelScope.launch {
            if (authorizationState.value != CimProtocol.AUTH_STATE_VALIDATED) {
                log("✗ No autorizado para enviar evento PLC: ${authorizationState.value}")
                return@launch
            }
            log("EJECUTANDO MOTOR...")
            delay(1000)
            estaciones[0] = true
            log("SENSOR 1 ACTIVADO")
            stationClient?.sendEvent("PALLET_DETECTED:S1")
        }
    }

    fun log(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val time = java.text.SimpleDateFormat("HH:mm:ss").format(Date())
            historialLogs.add(0, "[$time] $msg")
            if (historialLogs.size > 50) historialLogs.removeAt(50)
        }
    }
}
