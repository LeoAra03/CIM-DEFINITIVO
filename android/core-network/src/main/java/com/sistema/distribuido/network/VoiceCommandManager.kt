package com.sistema.distribuido.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Control por voz industrial (manos libres).
 * Comandos: "pausa general", "estado almacen", "parada emergencia"
 */
class VoiceCommandManager(
    context: Context,
    private val onCommand: (VoiceCommand) -> Unit,
    private val onPartial: (String) -> Unit = {}
) {
    enum class VoiceCommand(val keywords: List<String>) {
        EMERGENCY_STOP(listOf("parada emergencia", "emergencia", "e stop", "detener todo")),
        PAUSE_PLANT(listOf("pausa general", "pausar planta", "pausa")),
        RESUME(listOf("reanudar", "continuar", "reanudar ciclo")),
        STATUS_STORAGE(listOf("estado almacen", "estado almacén", "almacen")),
        STATUS_QUALITY(listOf("estado calidad", "calidad")),
        UNKNOWN(emptyList())
    }

    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            Log.w(TAG, "Speech recognition no disponible")
            return
        }
        if (listening) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false }
                override fun onError(error: Int) {
                    listening = false
                    Log.w(TAG, "Speech error: $error")
                }
                override fun onResults(results: Bundle?) {
                    listening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: return
                    onCommand(parseCommand(text))
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (text != null) onPartial(text)
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        listening = true
        recognizer?.startListening(intent)
    }

    fun stop() {
        listening = false
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    companion object {
        private const val TAG = "VoiceCommandManager"

        fun parseCommand(text: String): VoiceCommand {
            val normalized = text.lowercase(Locale.getDefault())
            return VoiceCommand.entries.firstOrNull { cmd ->
                cmd != VoiceCommand.UNKNOWN && cmd.keywords.any { normalized.contains(it) }
            } ?: VoiceCommand.UNKNOWN
        }
    }
}
