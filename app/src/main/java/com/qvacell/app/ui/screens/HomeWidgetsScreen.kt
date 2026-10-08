package com.qvacell.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qvacell.app.ui.components.BackNavigationIcon
import com.qvacell.app.ui.components.GradientSlider
import com.qvacell.app.ui.resolveAndroidIcon
import com.qvacell.app.widget.WidgetSettings
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val swatchPresets = listOf(
    Color(0xFFFFFFFF),
    Color(0xFF000000),
    Color(0xFF0099CC),
    Color(0xFFF44336),
    Color(0xFF4CAF50),
    Color(0xFF9C27B0),
)

@Composable
private fun TransparentSwatch(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val sq = size.width / 6f
            for (row in 0..5) {
                for (col in 0..5) {
                    val c = if ((row + col) % 2 == 0) Color(0xFFCCCCCC) else Color(0xFF999999)
                    drawRect(color = c, topLeft = Offset(col * sq, row * sq), size = Size(sq, sq))
                }
            }
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}


@Composable
private fun ColorWheelCanvas(
    hue: Float,
    sat: Float,
    bri: Float,
    onPickHS: (hue: Float, sat: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val sizePx = with(density) { minOf(maxWidth, maxHeight).toPx() }
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val r = sizePx / 2f - 8f

        Canvas(
            modifier = Modifier
                .size(minOf(maxWidth, maxHeight))
                .pointerInput(cx, cy, r) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun process(pos: Offset) {
                            val dx = pos.x - cx
                            val dy = pos.y - cy
                            val h = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                            val s = (sqrt(dx * dx + dy * dy) / r).coerceIn(0f, 1f)
                            onPickHS(h, s)
                        }
                        process(down.position)
                        down.consume()
                        var change = awaitDragOrCancellation(down.id)
                        while (change != null) {
                            process(change.position)
                            change.consume()
                            change = awaitDragOrCancellation(change.id)
                        }
                    }
                }
        ) {
            val center = Offset(cx, cy)
            val circlePath = Path().apply {
                addOval(Rect(cx - r, cy - r, cx + r, cy + r))
            }

            clipPath(circlePath) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Red,
                            Color(0xFFFF8000),
                            Color.Yellow,
                            Color(0xFF80FF00),
                            Color.Green,
                            Color(0xFF00FF80),
                            Color.Cyan,
                            Color(0xFF0080FF),
                            Color.Blue,
                            Color(0xFF8000FF),
                            Color.Magenta,
                            Color(0xFFFF0080),
                            Color.Red,
                        ),
                        center = center
                    ),
                    radius = r,
                    center = center
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0x00FFFFFF)),
                        center = center,
                        radius = r
                    ),
                    radius = r,
                    center = center
                )
                if (bri < 1f) {
                    drawCircle(
                        color = Color.Black.copy(alpha = 1f - bri),
                        radius = r,
                        center = center
                    )
                }
            }

            val angle = hue * PI.toFloat() / 180f
            val ix = cx + cos(angle) * sat * r
            val iy = cy + sin(angle) * sat * r
            drawCircle(color = Color.White, radius = 11f, center = Offset(ix, iy), style = Stroke(width = 3f))
            drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = 11f, center = Offset(ix, iy), style = Stroke(width = 1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerSheet(
    title: String,
    initialColor: Color,
    showAlpha: Boolean = false,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor.toArgb() or 0xFF000000.toInt(), initHsv)

    var hue by remember { mutableFloatStateOf(initHsv[0]) }
    var sat by remember { mutableFloatStateOf(initHsv[1]) }
    var bri by remember { mutableFloatStateOf(initHsv[2]) }
    var alpha by remember {
        mutableFloatStateOf(if (showAlpha) (initialColor.toArgb() ushr 24 and 0xFF) / 255f else 1f)
    }
    var showWheel by remember { mutableStateOf(false) }

    val rgbInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri))
    val hexLabel = "#%06X".format(rgbInt and 0x00FFFFFF)
    val alphaInt = if (showAlpha) (alpha * 255).toInt() else 255
    val currentArgb = (alphaInt shl 24) or (rgbInt and 0x00FFFFFF)
    val currentColor = Color(currentArgb)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Text(title, style = MaterialTheme.typography.titleMedium)

            // Preset swatches + transparent swatch (when alpha) + "+" button
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                swatchPresets.forEach { preset ->
                    val selected = preset.toArgb() == initialColor.toArgb()
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(preset)
                            .then(
                                if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable {
                                onColorSelected(preset)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (preset.red > 0.7f && preset.green > 0.7f && preset.blue > 0.7f) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (showAlpha) {
                    TransparentSwatch(
                        selected = initialColor.alpha == 0f,
                        onClick = { onColorSelected(Color.Transparent); onDismiss() }
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (showWheel) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .then(
                            if (showWheel) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                        .clickable { showWheel = !showWheel },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showWheel) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        tint = if (showWheel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showWheel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Text(hexLabel, style = MaterialTheme.typography.bodyMedium)
                }

                ColorWheelCanvas(
                    hue = hue, sat = sat, bri = bri,
                    onPickHS = { h, s -> hue = h; sat = s },
                    modifier = Modifier
                        .size(260.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Brillo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(bri * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    GradientSlider(
                        value = bri,
                        onValueChange = { bri = it },
                        colors = listOf(
                            Color.Black,
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f)))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showAlpha) {
                    val opaqueColor = Color((0xFF000000.toInt()) or (rgbInt and 0x00FFFFFF))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Opacidad", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(alpha * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        GradientSlider(
                            value = alpha,
                            onValueChange = { alpha = it },
                            colors = listOf(opaqueColor.copy(alpha = 0f), opaqueColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            } // end inner Column

            if (showWheel) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Cancelar", textAlign = TextAlign.Center)
                    }
                    VerticalDivider(modifier = Modifier.height(52.dp))
                    TextButton(
                        onClick = { onColorSelected(currentColor); onDismiss() },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Aplicar", color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPreview(
    bgColor: Color,
    iconColor: Color,
    textColor: Color,
    iconShapeBgColor: Color,
    iconShape: String,
    contentStyle: String
) {
    val showIcon = contentStyle in listOf("icon_only", "icon_code", "icon_text")
    val showText = contentStyle in listOf("icon_text", "text_only", "text_code")
    val showCode = contentStyle in listOf("icon_code", "text_code")

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (showIcon) {
                    val shapeClip = when (iconShape) {
                        "circle" -> CircleShape
                        "square" -> RoundedCornerShape(0.dp)
                        else -> RoundedCornerShape(6.dp)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(shapeClip)
                            .background(iconShapeBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolveAndroidIcon("Filled.AccountBalanceWallet"),
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (showText || showCode) Spacer(Modifier.height(3.dp))
                }
                if (showText) {
                    Text("Saldo", color = textColor, fontSize = 9.sp, style = MaterialTheme.typography.labelSmall)
                }
                if (showCode) {
                    Text("*222#", color = textColor.copy(alpha = 0.7f), fontSize = 7.sp, style = MaterialTheme.typography.labelSmall)
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeWidgetsScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current

    var contentStyle by remember { mutableStateOf(WidgetSettings.getContentStyle(context)) }
    var bgColor by remember { mutableStateOf(Color(WidgetSettings.getBackgroundColor(context))) }
    var iconColor by remember { mutableStateOf(Color(WidgetSettings.getIconColor(context))) }
    var textColor by remember { mutableStateOf(Color(WidgetSettings.getTextColor(context))) }
    var iconShape by remember { mutableStateOf(WidgetSettings.getIconShape(context)) }
    var iconShapeBgColor by remember { mutableStateOf(Color(WidgetSettings.getIconShapeBgColor(context))) }

    var showContentStyleSheet by remember { mutableStateOf(false) }
    var showBgColorSheet by remember { mutableStateOf(false) }
    var showIconColorSheet by remember { mutableStateOf(false) }
    var showTextColorSheet by remember { mutableStateOf(false) }
    var showShapeBgColorSheet by remember { mutableStateOf(false) }

    val contentStyleOptions = listOf(
        "icon_only" to "Solo icono",
        "icon_code" to "Icono + código USSD",
        "icon_text" to "Icono + texto",
        "text_only" to "Solo texto",
        "text_code" to "Texto + código USSD"
    )
    val currentStyleLabel = contentStyleOptions.firstOrNull { it.first == contentStyle }?.second ?: contentStyle
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text("Widgets de Inicio") },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                WidgetPreview(
                    bgColor = bgColor,
                    iconColor = iconColor,
                    textColor = textColor,
                    iconShapeBgColor = iconShapeBgColor,
                    iconShape = iconShape,
                    contentStyle = contentStyle
                )
            }

            item {
                Text(
                    "Estilo Widget 1×1",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Contenido") },
                            supportingContent = { Text(currentStyleLabel, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.clickable { showContentStyleSheet = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Color de fondo") },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                ) {
                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        val cell = 4.dp.toPx()
                                        val cols = (size.width / cell).toInt() + 1
                                        val rows = (size.height / cell).toInt() + 1
                                        for (row in 0 until rows) {
                                            for (col in 0 until cols) {
                                                drawRect(
                                                    color = if ((row + col) % 2 == 0) Color(0xFFCCCCCC) else Color(0xFFFFFFFF),
                                                    topLeft = Offset(col * cell, row * cell),
                                                    size = Size(cell, cell)
                                                )
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.matchParentSize().background(bgColor))
                                }
                            },
                            modifier = Modifier.clickable { showBgColorSheet = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Color del icono") },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(iconColor)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                            },
                            modifier = Modifier.clickable { showIconColorSheet = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Color del texto") },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(textColor)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                            },
                            modifier = Modifier.clickable { showTextColorSheet = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Color fondo de forma del icono") },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                ) {
                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        val cell = 4.dp.toPx()
                                        val cols = (size.width / cell).toInt() + 1
                                        val rows = (size.height / cell).toInt() + 1
                                        for (row in 0 until rows) {
                                            for (col in 0 until cols) {
                                                drawRect(
                                                    color = if ((row + col) % 2 == 0) Color(0xFFCCCCCC) else Color(0xFFFFFFFF),
                                                    topLeft = Offset(col * cell, row * cell),
                                                    size = Size(cell, cell)
                                                )
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.matchParentSize().background(iconShapeBgColor))
                                }
                            },
                            modifier = Modifier.clickable { showShapeBgColorSheet = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Forma del icono") },
                            supportingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    listOf(
                                        "circle" to "Círculo",
                                        "square" to "Cuadrado",
                                        "rounded_square" to "Redondeado"
                                    ).forEach { (value, label) ->
                                        FilterChip(
                                            selected = iconShape == value,
                                            onClick = {
                                                iconShape = value
                                                WidgetSettings.setIconShape(context, value)
                                                WidgetSettings.refreshAllWidgets(context)
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            item {
                Text(
                    "Cómo agregar",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "1. Mantén pulsado en la pantalla de inicio de tu teléfono.",
                            "2. Toca \"Widgets\" y busca Qvacell.",
                            "3. Arrastra \"Consultas Rápidas\" a la pantalla y selecciona la acción."
                        ).forEach {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showContentStyleSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showContentStyleSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Text("Contenido del widget", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                contentStyleOptions.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                contentStyle = value
                                WidgetSettings.setContentStyle(context, value)
                                WidgetSettings.refreshAllWidgets(context)
                                showContentStyleSheet = false
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = contentStyle == value, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showBgColorSheet) {
        ColorPickerSheet(
            title = "Color de fondo",
            initialColor = bgColor,
            showAlpha = true,
            onColorSelected = { color ->
                bgColor = color
                WidgetSettings.setBackgroundColor(context, color.toArgb())
                WidgetSettings.refreshAllWidgets(context)
            },
            onDismiss = { showBgColorSheet = false }
        )
    }

    if (showIconColorSheet) {
        ColorPickerSheet(
            title = "Color del icono",
            initialColor = iconColor,
            showAlpha = false,
            onColorSelected = { color ->
                iconColor = color
                WidgetSettings.setIconColor(context, color.toArgb())
                WidgetSettings.refreshAllWidgets(context)
            },
            onDismiss = { showIconColorSheet = false }
        )
    }

    if (showTextColorSheet) {
        ColorPickerSheet(
            title = "Color del texto",
            initialColor = textColor,
            showAlpha = false,
            onColorSelected = { color ->
                textColor = color
                WidgetSettings.setTextColor(context, color.toArgb())
                WidgetSettings.refreshAllWidgets(context)
            },
            onDismiss = { showTextColorSheet = false }
        )
    }

    if (showShapeBgColorSheet) {
        ColorPickerSheet(
            title = "Color fondo de forma del icono",
            initialColor = iconShapeBgColor,
            showAlpha = true,
            onColorSelected = { color ->
                iconShapeBgColor = color
                WidgetSettings.setIconShapeBgColor(context, color.toArgb())
                WidgetSettings.refreshAllWidgets(context)
            },
            onDismiss = { showShapeBgColorSheet = false }
        )
    }
}
