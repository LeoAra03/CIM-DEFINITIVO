package com.industria.coordinacion.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sistema.distribuido.network.prefecto.IndustrialTheme
import com.sistema.distribuido.network.prefecto.IndustrialCard
import com.sistema.distribuido.network.prefecto.IndustrialActionButton
import com.sistema.distribuido.network.prefecto.IndustrialStatusRow

@Composable
fun StorageTab(
    onStorageCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IndustrialCard("Gestión Central de Almacén", Icons.Default.Inventory) {
                IndustrialActionButton(texto = "Reseteo General Racks", icono = Icons.Default.RestartAlt, colorFondo = IndustrialTheme.Error, onClick = { onStorageCommand("RESET_ALL") })
                Spacer(Modifier.height(8.dp))
                IndustrialStatusRow("Capacidad Total", "18/18 POSICIONES", true)
            }
        }

        item {
            Text("MATRIZ DE RACKS (6 COLUMNAS X 3 NIVELES)", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // 6 columns x 3 rows = 18 positions
        repeat(3) { rowIdx ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { colGroupIdx ->
                        // Displaying 2 positions per group to fit nicely
                        val pos1 = rowIdx * 6 + colGroupIdx * 2 + 1
                        val pos2 = rowIdx * 6 + colGroupIdx * 2 + 2
                        
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StorageUnit(id = pos1, onCommand = onStorageCommand)
                            StorageUnit(id = pos2, onCommand = onStorageCommand)
                        }
                    }
                }
            }
        }

        item {
            IndustrialCard("Comando Manual Directo", Icons.Default.Terminal) {
                var manualCmd by remember { mutableStateOf("") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = manualCmd,
                        onValueChange = { manualCmd = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("CMD|PARAM...", fontSize = 12.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onStorageCommand(manualCmd); manualCmd = "" }) {
                        Icon(Icons.Default.Send, "Enviar comando manual", tint = IndustrialTheme.Primario)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageUnit(id: Int, onCommand: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(IndustrialTheme.Tarjeta, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .border(1.dp, IndustrialTheme.Borde, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("POS $id", color = IndustrialTheme.TextoPrincipal, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            Text("LIBRE", color = IndustrialTheme.Exito, fontSize = 9.sp)
        }
        
        if (expanded) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { expanded = false }) {
                Card(colors = CardDefaults.cardColors(containerColor = IndustrialTheme.Tarjeta)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ACCIONES POSICIÓN $id", color = Color.White, fontWeight = FontWeight.Bold)
                        IndustrialActionButton(texto = "Almacenar (STO)", icono = Icons.Default.AddBox, onClick = { onCommand("STO:$id"); expanded = false })
                        IndustrialActionButton(texto = "Liberar (FREE)", icono = Icons.Default.Output, colorFondo = IndustrialTheme.Advertencia, onClick = { onCommand("FREE:$id"); expanded = false })
                        IndustrialActionButton(texto = "Verificar (CHK)", icono = Icons.Default.Search, colorFondo = IndustrialTheme.Secundario, onClick = { onCommand("CHK:$id"); expanded = false })
                    }
                }
            }
        }
    }
}
