package com.industria.prefecto

import android.util.Log

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import com.industria.prefecto.AppType
import java.util.*

/**
 * IDENTIFICADOR DE APLICACIÓN
 *
 * Singleton that provides a unique identity to each application:
 * - Device MAC address
 * - Tipo de App (COORDINADOR, PLC, etc)
 * - Versión de App
 * - Unique device name
 */

class AppIdentifier private constructor(private val context: Context, val appType: AppType) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("cim_app_identifier", Context.MODE_PRIVATE)

    // Unique device identity (MAC address)
    val deviceMac: String = getOrCreateMac()

    // App version (keep in sync with AndroidManifest.xml)
    val appVersion: String = "1.0.0"

    // Friendly device name
    val deviceName: String = getOrCreateDeviceName()

    /**
     * Returns or creates a unique MAC address for this device.
     * En Android real, usaría ANDROID_ID + sanitización.
     */
    private fun getOrCreateMac(): String {
        var mac = sharedPrefs.getString("device_mac", "")

        if (mac.isNullOrEmpty()) {
            // Generar MAC partir de ANDROID_ID + AppType
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    ?: UUID.randomUUID().toString()
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                UUID.randomUUID().toString()
            }

            // Convertir a formato MAC: AA:BB:CC:DD:EE:FF
            mac = generateMacFromId(androidId, appType)
            sharedPrefs.edit().putString("device_mac", mac).apply()
        }

        return mac ?: ""
    }

    /**
     * Returns or creates the device name
     */
    private fun getOrCreateDeviceName(): String {
        var name = sharedPrefs.getString("device_name", "")

        if (name.isNullOrEmpty()) {
            name = when (appType) {
                AppType.COORDINADOR -> "Coordinador-${Build.MODEL}"
                AppType.PLC -> "PLC-Cinta-${Build.MODEL}"
                AppType.MANUFACTURA -> "Robot-${Build.MODEL}"
                AppType.CALIDAD -> "QC-Cámara-${Build.MODEL}"
                AppType.ALMACEN -> "Almacén-${Build.MODEL}"
                else -> "Device-${Build.MODEL}"
            }
            sharedPrefs.edit().putString("device_name", name).apply()
        }

        return name ?: ""
    }

    companion object {
        @Volatile
        private var instance: AppIdentifier? = null

        /**
         * Obten la instancia singleton (DEBE inicializarse en MainActivity)
         */
        fun getInstance(): AppIdentifier {
            return instance ?: throw IllegalStateException(
                "AppIdentifier no inicializado. Llama a init() en MainActivity"
            )
        }

        /**
         * Inicializa el AppIdentifier (LLAMAR EN MainActivity.onCreate())
         */
        fun init(context: Context, appType: AppType) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = AppIdentifier(context, appType)
                    }
                }
            }
        }
    }
}

/**
 * Generates a valid MAC identifier from a string (17 characters with colons)
 */
fun generateMacFromId(id: String, appType: AppType): String {
    // Hash of id + AppType to create something unique
    val combined = (id + appType.name).hashCode().toLong()

    // Convertir a bytes y formatear como MAC
    var hash = Math.abs(combined)
    val bytes = ByteArray(6)

    for (i in 0 until 6) {
        bytes[i] = (hash and 0xFF).toByte()
        hash = hash shr 8
    }

    // Formatear como AA:BB:CC:DD:EE:FF
    return bytes.joinToString(":") { byte ->
        String.format("%02X", byte.toInt() and 0xFF)
    }
}

/**
 * Obtiene el AppType actual
 */
fun getCurrentAppType(context: Context): AppType {
    return when (context.packageName) {
        "com.industria.coordinacion" -> AppType.COORDINADOR
        "com.industria.plc" -> AppType.PLC
        "com.industria.manufactura" -> AppType.MANUFACTURA
        "com.industria.calidad" -> AppType.CALIDAD
        "com.industria.almacenamiento" -> AppType.ALMACEN
        else -> AppType.UNKNOWN
    }
}

/**
 * Extension function for easy access from anywhere
 */
fun Context.getAppIdentifier(): AppIdentifier {
    return AppIdentifier.getInstance()
}


