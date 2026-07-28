// FIX Lote 9: Edge case handling
/**
 * CalidadViewModel
 * FIX: Documentación agregada
 */
// FIX #11: Additional null safety
package com.industria.calidad
import android.util.Log

import android.app.Application
import kotlinx.coroutines.withTimeout
import android.graphics.Bitmap
import kotlinx.coroutines.withTimeout
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.withTimeout
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.withTimeout
import com.sistema.distribuido.network.BluetoothHardwareManager
import kotlinx.coroutines.withTimeout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlinx.coroutines.withTimeout

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

    fun generateArUco(markerId: Int = 7, size: Int = 48) {
        val bitmap = ArUcoGenerator.buildBitmap(size = size, markerId = markerId)
        _arucoBitmap.value = bitmap
        _gcodeCommands.value = GCodeTranslator.translate(bitmap)
        _progress.value = 0f
        _status.value = "ArUco generado"
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
