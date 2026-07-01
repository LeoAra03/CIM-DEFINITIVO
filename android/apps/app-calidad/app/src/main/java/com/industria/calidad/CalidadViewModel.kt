package com.industria.calidad

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sistema.distribuido.network.BluetoothHardwareManager
import com.sistema.distribuido.network.ArucoDictionary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalidadViewModel @Inject constructor(
    application: Application,
    private val bluetoothManager: BluetoothHardwareManager
) : AndroidViewModel(application) {

    private val _arucoBitmap = MutableStateFlow<Bitmap?>(null)
    val arucoBitmap: StateFlow<Bitmap?> = _arucoBitmap.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _status = MutableStateFlow("Listo")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _gcodeCommands = MutableStateFlow<List<String>>(emptyList())
    val gcodeCommands: StateFlow<List<String>> = _gcodeCommands.asStateFlow()

    fun generateArUco(markerId: Int = 7, sizeMm: Int = 100, dictionary: ArucoDictionary = ArucoDictionary.DICT_4X4_50) {
        val bitmap = ArUcoGenerator.buildBitmapMm(sizeMm, markerId, dictionary)
        if (bitmap == null) {
            _status.value = "Error generando ArUco"
            return
        }
        _arucoBitmap.value = bitmap
        _gcodeCommands.value = GCodeTranslator.translate(bitmap)
        _progress.value = 0f
        _status.value = "ArUco #$markerId generado (${dictionary.label})"
    }

    fun sendLaserJob() = viewModelScope.launch {
        val commands = _gcodeCommands.value
        if (commands.isEmpty()) {
            _status.value = "Sin comandos G-code"
            return@launch
        }

        _status.value = "Enviando trabajo"
        commands.forEachIndexed { index, command ->
            bluetoothManager.sendCommand(command)
            _progress.value = ((index + 1).toFloat() / commands.size.toFloat())
            _status.value = "Ejecutando ${index + 1}/${commands.size}"
            kotlinx.coroutines.delay(120)
        }
        _status.value = "Grabado completo"
    }
}
