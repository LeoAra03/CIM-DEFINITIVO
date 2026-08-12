package com.sistema.distribuido.network.prefecto

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*

/**
 * Paleta HMI industrial.
 *
 * Los valores están calibrados sobre las figuras de referencia del proyecto
 * (docs/assets/imagenes/ui_app_*.png): fondo casi negro neutro, superficies
 * ligeramente más claras, acento VERDE para "activo / OK", rojo para paro,
 * azul reservado a Bluetooth / información y ámbar para advertencias.
 *
 * Los nombres de los tokens no cambian: cualquier pantalla existente sigue
 * compilando y simplemente se repinta con la nueva paleta.
 */
object IndustrialTheme {
    // --- Superficies -------------------------------------------------------
    /** Fondo base de la app (casi negro, neutro). */
    val Fondo = Color(0xFF0A0B0D)
    /** Extremo superior del degradado del fondo (muy sutil). */
    val FondoTop = Color(0xFF101317)
    /** Superficie de tarjeta. */
    val Tarjeta = Color(0xFF13171C)
    /** Superficie elevada (filas internas, chips seleccionados). */
    val TarjetaAlta = Color(0xFF1B2027)

    // --- Acentos -----------------------------------------------------------
    /** Acento principal: verde HMI (activo / seleccionado). */
    val Primario = Color(0xFF4CAF50)
    /** Verde más luminoso para realces puntuales. */
    val PrimarioBrillante = Color(0xFF6FD46F)
    /** Azul de información / Bluetooth / red. */
    val Secundario = Color(0xFF2F86E8)
    /** Estado correcto (verde algo más profundo que el acento). */
    val Exito = Color(0xFF3B8E35)
    /** Paro de emergencia / fallo. */
    val Error = Color(0xFFD23936)
    /** Advertencia / espera. */
    val Advertencia = Color(0xFFE0A526)

    // --- Bordes y texto ----------------------------------------------------
    val TextoPrincipal = Color(0xFFF2F4F6)
    val TextoSecundario = Color(0xFF8A939E)
    val TextoTenue = Color(0xFF5B646E)
    val Borde = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    val BordeFuerte = Color(0xFFFFFFFF).copy(alpha = 0.18f)

    // --- Métricas ----------------------------------------------------------
    val RadioTarjeta = 12.dp
    val RadioControl = 10.dp
    val PaddingTarjeta = 14.dp
    val EspacioSeccion = 12.dp
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
    /** Chip de estado opcional a la derecha del título (RUNNING, ONLINE…). */
    estado: (@Composable () -> Unit)? = null,
    /** Barra de navegación inferior opcional (bottom-nav del mockup HMI). */
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                color = IndustrialTheme.Fondo,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = IndustrialTheme.Borde,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                titulo.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = IndustrialTheme.TextoPrincipal,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                subtitulo.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = IndustrialTheme.TextoSecundario,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    navigationIcon = navigationIcon ?: {},
                    actions = {
                        estado?.let {
                            it()
                            Spacer(Modifier.width(4.dp))
                        }
                        actions()
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        bottomBar = bottomBar,
        containerColor = Color.Transparent,
        floatingActionButton = floatingActionButton,
        content = { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(IndustrialTheme.FondoTop, IndustrialTheme.Fondo)
                        )
                    )
            ) {
                content(padding)
            }
        }
    )
}

@Composable
fun IndustrialCard(
    titulo: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    headerColor: Color = IndustrialTheme.Primario,
    borderColor: Color = IndustrialTheme.Borde,
    /** Línea gris pequeña bajo el título (como en las figuras de referencia). */
    subtitulo: String? = null,
    /** Contenido a la derecha de la cabecera: normalmente un [IndustrialStatusChip]. */
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = IndustrialTheme.Tarjeta),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(IndustrialTheme.RadioTarjeta),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(IndustrialTheme.PaddingTarjeta)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(headerColor.copy(alpha = 0.14f), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, null, Modifier.size(16.dp), tint = headerColor)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        titulo.uppercase(),
                        color = IndustrialTheme.TextoPrincipal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.6.sp
                    )
                    if (subtitulo != null) {
                        Text(
                            subtitulo,
                            color = IndustrialTheme.TextoSecundario,
                            fontSize = 10.sp,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * Chip de estado HMI: punto de color + etiqueta (RUNNING, ONLINE, OFFLINE).
 * Es el indicador que aparece en la esquina superior derecha de las tarjetas
 * en las figuras de referencia.
 */
@Composable
fun IndustrialStatusChip(
    texto: String,
    color: Color = IndustrialTheme.Primario,
    modifier: Modifier = Modifier,
    parpadeo: Boolean = false
) {
    val alpha = if (parpadeo) {
        val transition = rememberInfiniteTransition(label = "chip")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "chipAlpha"
        ).value
    } else 1f

    Row(
        modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(color.copy(alpha = alpha), CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            texto.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
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

    // Texto negro sobre acentos claros, blanco sobre los oscuros (contraste HMI).
    val contenido = if (colorFondo.luminance() > 0.45f) IndustrialTheme.Fondo else IndustrialTheme.TextoPrincipal

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = finalModifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorFondo,
            contentColor = contenido,
            disabledContainerColor = IndustrialTheme.TextoPrincipal.copy(alpha = 0.06f),
            disabledContentColor = IndustrialTheme.TextoTenue
        ),
        shape = RoundedCornerShape(IndustrialTheme.RadioControl),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = contenido, strokeWidth = 2.dp)
        } else {
            Icon(icono, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                texto.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/**
 * Variante "outline" del botón de acción (READY / HOME en las figuras):
 * mismo tamaño y tipografía, fondo transparente y borde del color de acento.
 */
@Composable
fun IndustrialOutlinedButton(
    texto: String,
    icono: ImageVector? = null,
    modifier: Modifier = Modifier,
    color: Color = IndustrialTheme.Primario,
    enabled: Boolean = true,
    buttonHeight: Dp = 48.dp,
    fillMaxWidth: Boolean = true,
    onClick: () -> Unit
) {
    var finalModifier = modifier.then(Modifier.height(buttonHeight))
    if (fillMaxWidth) finalModifier = finalModifier.fillMaxWidth()

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = finalModifier,
        shape = RoundedCornerShape(IndustrialTheme.RadioControl),
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.55f) else IndustrialTheme.Borde),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = color,
            disabledContentColor = IndustrialTheme.TextoTenue
        )
    ) {
        if (icono != null) {
            Icon(icono, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            texto.uppercase(),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp
        )
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
            focusedContainerColor = IndustrialTheme.TextoPrincipal.copy(alpha = 0.03f)
        )
    )
}

@Composable
fun IndustrialStatusRow(label: String, valor: String, activo: Boolean = false) {
    val color = when {
        activo -> IndustrialTheme.Exito
        valor.contains("NO", ignoreCase = true) || valor.contains("OFF", ignoreCase = true) || valor.contains("DESCONECT", ignoreCase = true) -> IndustrialTheme.Error
        else -> IndustrialTheme.Advertencia
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(IndustrialTheme.TarjetaAlta, RoundedCornerShape(IndustrialTheme.RadioControl))
            .border(1.dp, IndustrialTheme.Borde, RoundedCornerShape(IndustrialTheme.RadioControl))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Luz piloto HMI con halo
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(color = color.copy(alpha = 0.20f), radius = size.minDimension * 0.9f)
                        drawCircle(color = color, radius = size.minDimension * 0.45f)
                    }
            )
            Spacer(Modifier.width(12.dp))
            Text(label, color = IndustrialTheme.TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            valor.uppercase(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )
    }
}

/** Fila clave–valor sobria (sin luz piloto), como las listas de datos del mockup. */
@Composable
fun IndustrialKeyValueRow(
    label: String,
    valor: String,
    valorColor: Color = IndustrialTheme.TextoPrincipal,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Text(label.uppercase(), color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, letterSpacing = 0.5.sp)
        Text(valor, color = valorColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Fila de chips de selección (STEP 0.1 / 1.0 / 10.0, presets LOW/MEDIUM/HIGH…).
 * El chip activo se rellena con el color de acento.
 */
@Composable
fun IndustrialChipRow(
    opciones: List<String>,
    seleccion: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = IndustrialTheme.Primario
) {
    Row(modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
        opciones.forEach { opcion ->
            val activo = opcion == seleccion
            Box(
                Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(IndustrialTheme.RadioControl))
                    .background(if (activo) color.copy(alpha = 0.18f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (activo) color else IndustrialTheme.Borde,
                        RoundedCornerShape(IndustrialTheme.RadioControl)
                    )
                    .clickable { onSelect(opcion) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    opcion.uppercase(),
                    color = if (activo) color else IndustrialTheme.TextoSecundario,
                    fontSize = 11.sp,
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Control numérico HMI: botones −/+ a los lados y valor grande con unidad
 * en el centro (velocidad, potencia, altura… en las figuras de referencia).
 */
@Composable
fun IndustrialStepper(
    label: String,
    valor: Float,
    unidad: String,
    onValorChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    paso: Float = 1f,
    minimo: Float = 0f,
    maximo: Float = 100f,
    color: Color = IndustrialTheme.Primario,
    decimales: Int = 0
) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            StepperButton(Icons.Default.Remove, color) {
                onValorChange((valor - paso).coerceIn(minimo, maximo))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (decimales == 0) valor.toInt().toString() else String.format("%.${decimales}f", valor),
                    color = IndustrialTheme.TextoPrincipal,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Text(unidad, color = IndustrialTheme.TextoSecundario, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
            StepperButton(Icons.Default.Add, color) {
                onValorChange((valor + paso).coerceIn(minimo, maximo))
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(IndustrialTheme.TextoPrincipal.copy(alpha = 0.07f), RoundedCornerShape(2.dp))
        ) {
            val f = if (maximo > minimo) ((valor - minimo) / (maximo - minimo)).coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun StepperButton(icono: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(IndustrialTheme.RadioControl))
            .background(IndustrialTheme.TarjetaAlta)
            .border(1.dp, IndustrialTheme.Borde, RoundedCornerShape(IndustrialTheme.RadioControl))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, null, Modifier.size(18.dp), tint = color)
    }
}

/** Indicador circular de porcentaje (donut) tipo "REJECTION RATE" del mockup. */
@Composable
fun IndustrialDonut(
    fraction: Float,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = IndustrialTheme.Primario,
    diametro: Dp = 108.dp,
    grosor: Dp = 10.dp
) {
    val safe = fraction.coerceIn(0f, 1f)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(diametro), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = grosor.toPx(), cap = StrokeCap.Round)
                val inset = grosor.toPx() / 2f
                val arcSize = Size(size.width - grosor.toPx(), size.height - grosor.toPx())
                drawArc(
                    color = IndustrialTheme.TextoPrincipal.copy(alpha = 0.07f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * safe,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke
                )
            }
            Text(
                "${(safe * 100).toInt()}%",
                color = IndustrialTheme.TextoPrincipal,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label.uppercase(), color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, letterSpacing = 0.8.sp)
    }
}

/** Medidor de barra estilo HMI (porcentaje 0..1). */
@Composable
fun IndustrialGauge(
    label: String,
    fraction: Float,
    color: Color = IndustrialTheme.Primario,
    modifier: Modifier = Modifier
) {
    val safe = fraction.coerceIn(0f, 1f)
    Column(modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label.uppercase(), color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, letterSpacing = 0.5.sp)
            Text("${(safe * 100).toInt()}%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(IndustrialTheme.TextoPrincipal.copy(alpha = 0.07f), RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(safe)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun IndustrialTerminal(logs: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(IndustrialTheme.RadioTarjeta))
            .background(Color(0xFF07080A))
            .border(1.dp, IndustrialTheme.Borde, RoundedCornerShape(IndustrialTheme.RadioTarjeta))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(IndustrialTheme.TextoPrincipal.copy(alpha = 0.04f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Terminal, null, Modifier.size(13.dp), tint = IndustrialTheme.TextoSecundario)
            Spacer(Modifier.width(8.dp))
            Text("LOGS DEL SISTEMA", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
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
                    color = IndustrialTheme.PrimarioBrillante.copy(alpha = 0.9f),
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

/** Descriptor de una entrada de navegación (tabs superiores o barra inferior). */
data class IndustrialNavItem(
    val etiqueta: String,
    val icono: ImageVector? = null
)

/**
 * Barra de pestañas superior con subrayado de acento, como en las figuras.
 * Sustituye visualmente al `ScrollableTabRow` por defecto sin cambiar la lógica:
 * se le pasa el índice seleccionado y el callback.
 */
@Composable
fun IndustrialTabBar(
    items: List<String>,
    seleccion: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = IndustrialTheme.Primario,
    scrollable: Boolean = true
) {
    val contenido: @Composable RowScope.() -> Unit = {
        items.forEachIndexed { index, etiqueta ->
            val activo = index == seleccion
            Column(
                Modifier
                    .then(if (scrollable) Modifier else Modifier.weight(1f))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    etiqueta.uppercase(),
                    color = if (activo) color else IndustrialTheme.TextoSecundario,
                    fontSize = 11.sp,
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (activo) color else Color.Transparent)
                )
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .background(IndustrialTheme.Fondo)
            .drawBehind {
                drawLine(
                    color = IndustrialTheme.Borde,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        if (scrollable) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Bottom,
                content = contenido
            )
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, content = contenido)
        }
    }
}

/**
 * Barra de navegación inferior HMI (icono + etiqueta, acento verde en el activo).
 * Pensada para pasarse a `IndustrialScaffold(bottomBar = { ... })`.
 */
@Composable
fun IndustrialBottomNav(
    items: List<IndustrialNavItem>,
    seleccion: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = IndustrialTheme.Primario,
    scrollable: Boolean = false
) {
    val contenido: @Composable RowScope.() -> Unit = {
        items.forEachIndexed { index, item ->
            val activo = index == seleccion
            val tinte = if (activo) color else IndustrialTheme.TextoTenue
            Column(
                Modifier
                    .then(if (scrollable) Modifier.width(76.dp) else Modifier.weight(1f))
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (item.icono != null) {
                    Icon(item.icono, item.etiqueta, Modifier.size(20.dp), tint = tinte)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    item.etiqueta.uppercase(),
                    color = tinte,
                    fontSize = 9.sp,
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                    maxLines = 1
                )
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .background(IndustrialTheme.Fondo)
            .drawBehind {
                drawLine(
                    color = IndustrialTheme.Borde,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 4.dp)
    ) {
        if (scrollable) {
            Row(Modifier.horizontalScroll(rememberScrollState()), content = contenido)
        } else {
            Row(Modifier.fillMaxWidth(), content = contenido)
        }
    }
}

/**
 * Fila de dispositivo Bluetooth como en la figura `ui_bluetooth_connect.png`:
 * avatar circular, nombre + MAC y acción a la derecha (CONECTAR / CONECTADO).
 */
@Composable
fun IndustrialDeviceRow(
    nombre: String,
    direccion: String,
    conectado: Boolean,
    conectando: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val acento = if (conectado) IndustrialTheme.Primario else IndustrialTheme.Secundario
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(IndustrialTheme.RadioControl))
            .background(IndustrialTheme.TarjetaAlta)
            .border(
                1.dp,
                if (conectado) acento.copy(alpha = 0.45f) else IndustrialTheme.Borde,
                RoundedCornerShape(IndustrialTheme.RadioControl)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(acento.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (conectado) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    null,
                    Modifier.size(18.dp),
                    tint = acento
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(nombre, color = IndustrialTheme.TextoPrincipal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(direccion, color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            conectando -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = IndustrialTheme.Advertencia
            )
            else -> IndustrialOutlinedButton(
                texto = if (conectado) "DESCONECTAR" else "CONECTAR",
                color = if (conectado) IndustrialTheme.Error else IndustrialTheme.Primario,
                buttonHeight = 34.dp,
                fillMaxWidth = false,
                onClick = onClick
            )
        }
    }
}

/** Cabecera de sección en mayúsculas (agrupa listas, como en la pantalla BT). */
@Composable
fun IndustrialSectionHeader(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = IndustrialTheme.TextoSecundario,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        trailing?.invoke()
    }
}
