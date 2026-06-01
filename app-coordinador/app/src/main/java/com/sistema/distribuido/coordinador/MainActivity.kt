package com.sistema.distribuido.coordinador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.sistema.distribuido.coordinador.ui.screens.HubScreen
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.tag("CoordinadorApp").i("Iniciando app Coordinador")

        setContent {
            MaterialTheme {
                HubScreen()
            }
        }
    }
}
