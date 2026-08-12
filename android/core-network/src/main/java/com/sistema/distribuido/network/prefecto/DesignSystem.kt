package com.sistema.distribuido.network.prefecto

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*

object IndustrialTheme {
    val Fondo = Color(0xFF0F111A)
    val Tarjeta = Color(0xFF1A1D2D)
    val Primario = Color(0xFF00E5FF)
    val Secundario = Color(0xFF7C4DFF)
    val Exito = Color(0xFF00E676)
    val Error = Color(0xFFFF5252)
    val Advertencia = Color(0xFFFFD600)
    val Borde = Color.White.copy(alpha = 0.08f)
    val TextoPrincipal = Color.White
    val TextoSecundario = Color(0xFF94A3B8)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndustrialScaffold(
    titulo: String,
    subtitulo: String,
    onTestToggle: (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                color = Color.Black.copy(alpha = 0.95f),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = IndustrialTheme.Primario.copy(alpha = 0.3f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                titulo.uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = IndustrialTheme.TextoPrincipal,
                                letterSpacing = 1.sp
                            )
                            Text(
                                subtitulo,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = IndustrialTheme.Primario,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    navigationIcon = navigationIcon ?: {},
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = IndustrialTheme.Fondo,
        floatingActionButton = floatingActionButton,
        content = content
    )
}

@Composable
fun IndustrialCard(
    titulo: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    headerColor: Color = IndustrialTheme.Primario,
    borderColor: Color = IndustrialTheme.Borde,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = IndustrialTheme.Tarjeta),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icono,
                    null,
                    Modifier
                        .size(24.dp)
                        .background(headerColor.copy(alpha = 0.1f), CircleShape)
                        .padding(4.dp),
                    tint = headerColor
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    titulo.uppercase(),
                    color = IndustrialTheme.TextoPrincipal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun IndustrialActionButton(
    texto: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    colorFondo: Color = IndustrialTheme.Primario,
    enabled: Boolean = true,
    loading: Boolean = false,
    buttonHeight: Dp = 52.dp,
    fillMaxWidth: Boolean = true,
    onClick: () -> Unit
) {
    var finalModifier = modifier.then(Modifier.height(buttonHeight))
    if (fillMaxWidth) {
        finalModifier = finalModifier.fillMaxWidth()
    }

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = finalModifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorFondo,
            contentColor = Color.Black,
            disabledContainerColor = Color.DarkGray.copy(alpha = 0.3f),
            disabledContentColor = Color.Gray
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black, strokeWidth = 3.dp)
        } else {
            Icon(icono, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                texto.uppercase(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun IndustrialTextButton(
    texto: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = IndustrialTheme.TextoSecundario,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(texto.uppercase(), color = textColor)
    }
}

@Composable
fun IndustrialTextField(valor: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(color = IndustrialTheme.TextoPrincipal, fontWeight = FontWeight.Medium),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = IndustrialTheme.Primario,
            unfocusedBorderColor = IndustrialTheme.Borde,
            focusedLabelColor = IndustrialTheme.Primario,
            unfocusedLabelColor = IndustrialTheme.TextoSecundario,
            cursorColor = IndustrialTheme.Primario,
            focusedContainerColor = Color.White.copy(alpha = 0.03f)
        )
    )
}

@Composable
fun IndustrialStatusRow(label: String, valor: String, activo: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(
                        if (activo) IndustrialTheme.Exito else IndustrialTheme.Error,
                        CircleShape
                    )
                    .drawBehind {
                        drawCircle(
                            color = (if (activo) IndustrialTheme.Exito else IndustrialTheme.Error).copy(alpha = 0.3f),
                            radius = size.minDimension * 0.8f
                        )
                    }
            )
            Spacer(Modifier.width(16.dp))
            Text(label, color = IndustrialTheme.TextoSecundario, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            valor.uppercase(),
            color = if (activo) IndustrialTheme.Exito else IndustrialTheme.TextoPrincipal,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun IndustrialTerminal(logs: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, IndustrialTheme.Borde, RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Terminal, null, Modifier.size(14.dp), tint = IndustrialTheme.TextoSecundario)
            Spacer(Modifier.width(8.dp))
            Text("LOGS DEL SISTEMA", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            reverseLayout = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            items(logs) { log ->
                Text(
                    log,
                    color = IndustrialTheme.Exito.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Consola de programas Scorbot estilo "RUN" (hyperterminal).
 * [presets] son pares etiqueta -> nombre de programa. [onRun] recibe el nombre del
 * programa (sin envoltura) para que el llamador lo mande como "R:RUN <prog>".
 */
@Composable
fun ScorbotRunConsole(
    enabled: Boolean,
    presets: List<Pair<String, String>>,
    onRun: (String) -> Unit,
    onAuto: () -> Unit,
    initialProgram: String = "",
    descripcion: String = "Ejecuta programas cargados en el controlador (estilo hyperterminal)",
    manualLabel: String = "Programa (ej: ARU)"
) {
    var runProgram by remember { mutableStateOf(initialProgram) }
    IndustrialCard("Programas Scorbot (RUN)", Icons.Default.Terminal, headerColor = IndustrialTheme.Secundario) {
        Text(descripcion, color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        presets.chunked(3).forEach { fila ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                fila.forEach { (label, prog) ->
                    IndustrialActionButton(label, Icons.Default.PlayArrow, Modifier.weight(1f), enabled = enabled, onClick = { onRun(prog) })
                }
                repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        IndustrialActionButton("AUTO", Icons.Default.Autorenew, colorFondo = IndustrialTheme.Exito, enabled = enabled, onClick = onAuto)
        Spacer(Modifier.height(12.dp))
        Text("COMANDO RUN MANUAL", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
        IndustrialTextField(valor = runProgram, onValueChange = { runProgram = it.uppercase() }, label = manualLabel)
        Spacer(Modifier.height(8.dp))
        IndustrialActionButton(
            texto = "EJECUTAR RUN",
            icono = Icons.Default.PlayCircle,
            colorFondo = IndustrialTheme.Primario,
            enabled = enabled,
            onClick = {
                val prog = runProgram.trim()
                if (prog.isNotEmpty()) onRun(prog)
            }
        )
    }
}

@Composable
fun ModoDemoBanner(activo: Boolean = true, texto: String = "MODO DEMO — SIN HARDWARE") {
    if (activo) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = IndustrialTheme.Secundario.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, IndustrialTheme.Secundario),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Science, null, Modifier.size(16.dp), tint = IndustrialTheme.Secundario)
                Spacer(Modifier.width(8.dp))
                Text(
                    texto.uppercase(),
                    color = IndustrialTheme.Secundario,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun IndustrialEmptyState(icono: ImageVector, texto: String, detalle: String? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icono, null, Modifier.size(40.dp), tint = IndustrialTheme.TextoSecundario.copy(alpha = 0.45f))
        Spacer(Modifier.height(8.dp))
        Text(texto, color = IndustrialTheme.TextoSecundario, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (detalle != null) {
            Spacer(Modifier.height(4.dp))
            Text(detalle, color = IndustrialTheme.TextoSecundario.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}
