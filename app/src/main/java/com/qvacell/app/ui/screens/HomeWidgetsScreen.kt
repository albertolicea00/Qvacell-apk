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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
    contentColor: Color,
    iconShape: String,
    contentStyle: String,
    alignment: String
) {
    val showIcon = contentStyle in listOf("icon_only", "icon_text")
    val showText = contentStyle in listOf("icon_text", "text_only")
    val shapeClip: Shape = when (iconShape) {
        "circle"   -> CircleShape
        "square"   -> RoundedCornerShape(0.dp)
        "squircle" -> RoundedCornerShape(40)
        "hexagon"  -> object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val path = Path(); val cx = size.width / 2f; val cy = size.height / 2f; val r = minOf(cx, cy) * 0.96f
                for (i in 0 until 6) { val a = (PI * i / 3 - PI / 6).toFloat(); if (i == 0) path.moveTo(cx + r * cos(a), cy + r * sin(a)) else path.lineTo(cx + r * cos(a), cy + r * sin(a)) }
                path.close(); return Outline.Generic(path)
            }
        }
        "star" -> object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val path = Path(); val cx = size.width / 2f; val cy = size.height / 2f
                val outerR = minOf(cx, cy) * 0.96f; val innerR = outerR * 0.45f
                for (i in 0 until 10) { val a = (PI * i / 5 - PI / 2).toFloat(); val r = if (i % 2 == 0) outerR else innerR; if (i == 0) path.moveTo(cx + cos(a) * r, cy + sin(a) * r) else path.lineTo(cx + cos(a) * r, cy + sin(a) * r) }
                path.close(); return Outline.Generic(path)
            }
        }
        "flower" -> object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val path = Path(); val cx = size.width / 2f; val cy = size.height / 2f
                val r = minOf(cx, cy) * 0.60f; val cr = minOf(cx, cy) * 0.84f
                path.moveTo(cx, cy - r)
                path.cubicTo(cx + cr, cy - r, cx + r, cy - cr, cx + r, cy)
                path.cubicTo(cx + r, cy + cr, cx + cr, cy + r, cx, cy + r)
                path.cubicTo(cx - cr, cy + r, cx - r, cy + cr, cx - r, cy)
                path.cubicTo(cx - r, cy - cr, cx - cr, cy - r, cx, cy - r)
                path.close(); return Outline.Generic(path)
            }
        }
        "diamond" -> object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val path = Path()
                path.moveTo(size.width / 2f, 0f); path.lineTo(size.width, size.height / 2f)
                path.lineTo(size.width / 2f, size.height); path.lineTo(0f, size.height / 2f)
                path.close(); return Outline.Generic(path)
            }
        }
        "badge" -> object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val path = Path()
                val cx = size.width / 2f; val cy = size.height / 2f
                val outerR = minOf(cx, cy) * 0.96f; val innerR = minOf(cx, cy) * 0.63f
                val pts = Array(16) { i ->
                    val a = (PI * i / 8 - PI / 2).toFloat()
                    val r = if (i % 2 == 0) outerR else innerR
                    floatArrayOf(cx + r * cos(a), cy + r * sin(a))
                }
                fun mid(a: FloatArray, b: FloatArray) = floatArrayOf((a[0]+b[0])/2f, (a[1]+b[1])/2f)
                val mids = Array(16) { i -> mid(pts[i], pts[(i+1) % 16]) }
                path.moveTo(mids[15][0], mids[15][1])
                for (i in 0 until 16) path.quadraticBezierTo(pts[i][0], pts[i][1], mids[i][0], mids[i][1])
                path.close(); return Outline.Generic(path)
            }
        }
        "rounded_diamond" -> object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val path = Path()
                val cx = size.width / 2f; val cy = size.height / 2f
                val d = size.width * 0.10f / sqrt(2f)
                path.moveTo(cx - d, d); path.quadraticBezierTo(cx, 0f, cx + d, d)
                path.lineTo(size.width - d, cy - d); path.quadraticBezierTo(size.width, cy, size.width - d, cy + d)
                path.lineTo(cx + d, size.height - d); path.quadraticBezierTo(cx, size.height, cx - d, size.height - d)
                path.lineTo(d, cy + d); path.quadraticBezierTo(0f, cy, d, cy - d)
                path.close(); return Outline.Generic(path)
            }
        }
        "rounded_hexagon" -> object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val path = Path()
                val cx = size.width / 2f; val cy = size.height / 2f
                val r = minOf(cx, cy) * 0.94f; val cr = minOf(cx, cy) * 0.14f
                val verts = Array(6) { i ->
                    val a = (PI * i / 3 - PI / 6).toFloat()
                    floatArrayOf(cx + r * cos(a), cy + r * sin(a))
                }
                for (i in 0 until 6) {
                    val prev = verts[(i + 5) % 6]; val curr = verts[i]; val next = verts[(i + 1) % 6]
                    val dx1 = curr[0] - prev[0]; val dy1 = curr[1] - prev[1]; val l1 = sqrt(dx1 * dx1 + dy1 * dy1)
                    val dx2 = next[0] - curr[0]; val dy2 = next[1] - curr[1]; val l2 = sqrt(dx2 * dx2 + dy2 * dy2)
                    val fx = curr[0] - cr * dx1 / l1; val fy = curr[1] - cr * dy1 / l1
                    val tx = curr[0] + cr * dx2 / l2; val ty = curr[1] + cr * dy2 / l2
                    if (i == 0) path.moveTo(fx, fy) else path.lineTo(fx, fy)
                    path.quadraticBezierTo(curr[0], curr[1], tx, ty)
                }
                path.close(); return Outline.Generic(path)
            }
        }
        else -> RoundedCornerShape(16.dp) // rounded_square
    }

    val previewAlignment = when (alignment) {
        "top" -> Alignment.TopCenter
        "bottom" -> Alignment.BottomCenter
        else -> Alignment.Center
    }

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val cellBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
        ) {
            // Cuadrícula que simula las celdas del launcher
            Canvas(modifier = Modifier.matchParentSize()) {
                val step = 20.dp.toPx()
                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        color = gridLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += step
                }
            }

            // Celda 1x1 del launcher (área rectangular típica)
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(120.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 1.dp,
                        color = cellBorderColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                contentAlignment = previewAlignment
            ) {
                val sizeModifier = if (alignment == "fill") {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.size(80.dp)
                }

                Box(
                    modifier = sizeModifier
                        .clip(shapeClip)
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
                                val iconSize = if (showText) 30.dp else 46.dp
                                Icon(
                                    imageVector = resolveAndroidIcon("Filled.AccountBalanceWallet"),
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(iconSize)
                                )
                                if (showText) Spacer(Modifier.height(3.dp))
                            }
                            if (showText) {
                                val textSize = if (showIcon) 9.sp else 13.sp
                                Text(
                                    "Saldo",
                                    color = contentColor,
                                    fontSize = textSize,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeWidgetsScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current

    // Ensure content style is fixed to icon_only
    var contentStyle = "icon_only"
    var bgColor by remember { mutableStateOf(Color(WidgetSettings.getBackgroundColor(context))) }
    var contentColor by remember { mutableStateOf(Color(WidgetSettings.getIconColor(context))) }
    var iconShape by remember { mutableStateOf(WidgetSettings.getIconShape(context)) }
    var alignment by remember { mutableStateOf(WidgetSettings.getAlignment(context)) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (WidgetSettings.getContentStyle(context) != "icon_only") {
            WidgetSettings.setContentStyle(context, "icon_only")
            WidgetSettings.refreshAllWidgets(context)
        }
    }

    var showIconShapeSheet by remember { mutableStateOf(false) }
    var showBgColorSheet by remember { mutableStateOf(false) }
    var showContentColorSheet by remember { mutableStateOf(false) }
    var showAlignmentSheet by remember { mutableStateOf(false) }

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
                    contentColor = contentColor,
                    iconShape = iconShape,
                    contentStyle = contentStyle,
                    alignment = alignment
                )
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

            item {
                Text(
                    "Estilo Widget 1×1",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
                    Column {
                        /*
                        ListItem(
                            headlineContent = { Text("Contenido") },
                            supportingContent = { Text("Solo icono", style = MaterialTheme.typography.labelSmall) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        */

                        ListItem(
                            headlineContent = { Text("Color de fondo") },
                            supportingContent = { Text("Fondo del área del widget", style = MaterialTheme.typography.labelSmall) },
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
                            headlineContent = { Text("Color del contenido") },
                            supportingContent = { Text("Color del ícono y texto", style = MaterialTheme.typography.labelSmall) },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(contentColor)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                            },
                            modifier = Modifier.clickable { showContentColorSheet = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        val shapeOptions = listOf(
                            "rounded_square"   to "Redondeado",
                            "squircle"         to "Squircle (Pixel)",
                            "circle"           to "Círculo",
                            "square"           to "Cuadrado",
                            "hexagon"          to "Hexágono",
                            "rounded_hexagon"  to "Hexágono redondeado",
                            "diamond"          to "Diamante",
                            "rounded_diamond"  to "Diamante redondeado",
                            "badge"            to "Sello",
                        )
                        val shapeLabel = shapeOptions.firstOrNull { it.first == iconShape }?.second ?: "Redondeado"
                        ListItem(
                            headlineContent = { Text("Forma del widget") },
                            supportingContent = { Text(shapeLabel, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.clickable { showIconShapeSheet = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        val alignmentOptions = listOf(
                            "fill"   to "Llenar cuadrícula (Completo)",
                            "center" to "Centrado (Mantener proporción)",
                            "top"    to "Arriba",
                            "bottom" to "Abajo",
                        )
                        val alignmentLabel = alignmentOptions.firstOrNull { it.first == alignment }?.second ?: "Llenar cuadrícula"
                        ListItem(
                            headlineContent = { Text("Alineación y tamaño") },
                            supportingContent = { Text(alignmentLabel, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.clickable { showAlignmentSheet = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }


            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }


    if (showIconShapeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showIconShapeSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Text("Forma del widget", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                listOf(
                    "rounded_square"  to "Redondeado",
                    "squircle"        to "Squircle (Pixel)",
                    "circle"          to "Círculo",
                    "square"          to "Cuadrado",
                    "hexagon"         to "Hexágono",
                    "rounded_hexagon" to "Hexágono redondeado",
                    "diamond"         to "Diamante",
                    "rounded_diamond" to "Diamante redondeado",
                    "badge"           to "Sello",
                ).forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                iconShape = value
                                WidgetSettings.setIconShape(context, value)
                                WidgetSettings.refreshAllWidgets(context)
                                showIconShapeSheet = false
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = iconShape == value, onClick = null)
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

    if (showContentColorSheet) {
        ColorPickerSheet(
            title = "Color del contenido",
            initialColor = contentColor,
            showAlpha = false,
            onColorSelected = { color ->
                contentColor = color
                WidgetSettings.setIconColor(context, color.toArgb())
                WidgetSettings.setTextColor(context, color.toArgb())
                WidgetSettings.refreshAllWidgets(context)
            },
            onDismiss = { showContentColorSheet = false }
        )
    }

    if (showAlignmentSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showAlignmentSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Text("Alineación y tamaño", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                listOf(
                    "fill"   to "Llenar cuadrícula (Completo)",
                    "center" to "Centrado (Mantener proporción)",
                    "top"    to "Arriba (Mantener proporción)",
                    "bottom" to "Abajo (Mantener proporción)",
                ).forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                alignment = value
                                WidgetSettings.setAlignment(context, value)
                                WidgetSettings.refreshAllWidgets(context)
                                showAlignmentSheet = false
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = alignment == value, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

}

