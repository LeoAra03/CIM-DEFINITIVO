package com.example.test.plantillas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * MODO DE USO DE LA PLANTILLA
 * Este es un ejemplo de cómo implementar una nueva pantalla siguiendo el diseño base.
 */
@Composable
fun EjemploPantallaCIM() {
    IndustrialScaffold(
        titulo = "Nombre del Módulo",
        subtitulo = "Subtítulo Descriptivo",
        barraInferior = { /* Menu de navegación */ }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(IndustrialTheme.PaddingEstándar),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IndustrialCard(titulo = "Sección de Control", icono = Icons.Default.Build) {
                    IndustrialStatusRow("Parámetro 1", "Activo", activo = true)
                    IndustrialStatusRow("Parámetro 2", "Inactivo", activo = false)
                }
            }

            item {
                IndustrialActionButton(
                    texto = "EJECUTAR ACCIÓN",
                    icono = Icons.AutoMirrored.Filled.Send,
                    onClick = { Log.d("EjemploUso", "Acción de plantilla ejecutada") }
                )
            }
        }
    }
}
