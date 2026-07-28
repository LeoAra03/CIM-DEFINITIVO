package com.sistema.distribuido.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.testModeDataStore: DataStore<Preferences> by preferencesDataStore(name = "cim_test_mode")

/**
 * Manager de Modo Test para validación sin hardware.
 * Persiste el flag en DataStore; activar con 5 taps rápidos en el logo (ver TestModeGesture).
 */
class TestModeManager private constructor(private val context: Context) {

    private val enabledKey = booleanPreferencesKey("test_mode_enabled")

    val isEnabledFlow: Flow<Boolean> = context.testModeDataStore.data.map { prefs ->
        prefs[enabledKey] ?: false
    }

    fun isEnabled(): Boolean = runBlocking {
        context.testModeDataStore.data.map { it[enabledKey] ?: false }.first()
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.testModeDataStore.edit { prefs -> prefs[enabledKey] = enabled }
        android.util.Log.i("CIM_TEST", "MODO TEST: ${if (enabled) "ACTIVADO" else "DESACTIVADO"}")
    }

    suspend fun toggle() {
        context.testModeDataStore.edit { prefs ->
            val current = prefs[enabledKey] ?: false
            prefs[enabledKey] = !current
            android.util.Log.i("CIM_TEST", "MODO TEST: ${if (!current) "ACTIVADO" else "DESACTIVADO"}")
        }
    }

    suspend fun simulateResponse(command: String, onResponse: (String) -> Unit): Boolean {
        val enabled = context.testModeDataStore.data.map { it[enabledKey] ?: false }.first()
        if (enabled) {
            android.util.Log.d("CIM_TEST", "Simulando respuesta para: $command")
            when {
                command.contains("VALIDATE") -> onResponse("VALIDADO;SUCCESS")
                command.contains("STATUS") -> onResponse("STATUS;UUID;IDLE")
                else -> onResponse("ACK;RECEIVED")
            }
            return true
        }
        return false
    }

    companion object {
        @Volatile
        private var instance: TestModeManager? = null

        fun getInstance(context: Context): TestModeManager {
            return instance ?: synchronized(this) {
                instance ?: TestModeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
