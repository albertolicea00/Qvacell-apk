package com.qvacell.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.qvacell.app.ui.components.BackNavigationIcon
import com.qvacell.app.widget.WidgetSettings

// ---------------------------------------------------------------------------
// Local helpers (mirrors OptionsScreen pattern, but defined privately here)
// ---------------------------------------------------------------------------

@Composable
private fun WidgetSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        content()
    }
}

private val presetColors = listOf(
    Color(0xFFFFFFFF), // White
    Color(0xFF000000), // Black
    Color(0xFF0099CC), // Cyan
    Color(0xFFF44336), // Red
    Color(0xFF4CAF50), // Green
    Color(0xFFFFEB3B), // Yellow
    Color(0xFFFF9800), // Orange
    Color(0xFF9C27B0), // Purple
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerSheet(
    title: String,
    currentColor: Color,
    showTransparencyNote: Boolean = false,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hexInput by remember { mutableStateOf("#%06X".format(currentColor.toArgb() and 0x00FFFFFF)) }
    var hexError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presetColors.forEach { color ->
                    val selected = (color.toArgb() and 0x00FFFFFF) == (currentColor.toArgb() and 0x00FFFFFF)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable {
                                // Preserve alpha from currentColor, apply new RGB
                                val alpha = currentColor.toArgb() and 0xFF000000.toInt()
                                val rgb = color.toArgb() and 0x00FFFFFF
                                onColorSelected(Color(alpha or rgb))
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = hexInput,
                onValueChange = { raw ->
                    val v = raw.uppercase().take(7)
                    hexInput = v
                    hexError = false
                },
                label = { Text("Hex personalizado (#RRGGBB)") },
                singleLine = true,
                isError = hexError,
                supportingText = if (hexError) { { Text("Formato inválido. Usa #RRGGBB") } } else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    val hex = hexInput.trim()
                    val regex = Regex("^#[0-9A-Fa-f]{6}$")
                    if (regex.matches(hex)) {
                        val alpha = currentColor.toArgb() and 0xFF000000.toInt()
                        val rgb = android.graphics.Color.parseColor(hex) and 0x00FFFFFF
                        onColorSelected(Color(alpha or rgb))
                        hexError = false
                    } else {
                        hexError = true
                    }
                }),
                modifier = Modifier.fillMaxWidth()
            )

            if (showTransparencyNote) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ajusta la transparencia con el deslizador en la pantalla principal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeWidgetsScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current

    // --- State mirrors current WidgetSettings values ---
    var showSms by remember { mutableStateOf(WidgetSettings.getShowSmsServices(context)) }
    var contentStyle by remember { mutableStateOf(WidgetSettings.getContentStyle(context)) }
    var bgColor by remember { mutableStateOf(Color(WidgetSettings.getBackgroundColor(context))) }
    var iconColor by remember { mutableStateOf(Color(WidgetSettings.getIconColor(context))) }
    var textColor by remember { mutableStateOf(Color(WidgetSettings.getTextColor(context))) }
    var iconShape by remember { mutableStateOf(WidgetSettings.getIconShape(context)) }

    // Alpha is stored in bgColor; expose as 0f..1f float
    var bgAlpha by remember { mutableFloatStateOf((bgColor.toArgb() ushr 24 and 0xFF) / 255f) }

    // Sheet visibility state
    var showContentStyleSheet by remember { mutableStateOf(false) }
    var showBgColorSheet by remember { mutableStateOf(false) }
    var showIconColorSheet by remember { mutableStateOf(false) }
    var showTextColorSheet by remember { mutableStateOf(false) }

    val contentStyleOptions = listOf(
        "icon_only" to "Solo icono",
        "icon_code" to "Icono + código",
        "icon_text" to "Icono + texto",
        "text_only" to "Solo texto",
        "text_code" to "Texto + código"
    )
    val currentStyleLabel = contentStyleOptions.firstOrNull { it.first == contentStyle }?.second ?: contentStyle

    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    val transparencyPct = (bgAlpha * 100).toInt()

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
            // ----------------------------------------------------------------
            // Section: Cómo agregar
            // ----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(4.dp))
                WidgetSection("Cómo agregar") {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1. Mantén pulsado en la pantalla de inicio de tu teléfono.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("2. Toca \"Widgets\" y busca Qvacell.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("3. Arrastra el widget \"Consultas Rápidas\" a la pantalla.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ----------------------------------------------------------------
            // Section: Configuración
            // ----------------------------------------------------------------
            item {
                WidgetSection("Configuración") {
                    // SMS toggle
                    ListItem(
                        headlineContent = { Text("Mostrar servicios SMS") },
                        trailingContent = {
                            Switch(
                                checked = showSms,
                                onCheckedChange = { v ->
                                    showSms = v
                                    WidgetSettings.setShowSmsServices(context, v)
                                    WidgetSettings.refreshAllWidgets(context)
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider()
                    // Content style
                    ListItem(
                        headlineContent = { Text("Contenido") },
                        supportingContent = { Text(currentStyleLabel) },
                        modifier = Modifier.clickable { showContentStyleSheet = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // ----------------------------------------------------------------
            // Section: Estilo
            // ----------------------------------------------------------------
            item {
                WidgetSection("Estilo") {
                    // Background color
                    ListItem(
                        headlineContent = { Text("Color de fondo") },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(bgColor.copy(alpha = 1f))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                        },
                        modifier = Modifier.clickable { showBgColorSheet = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider()
                    // Transparency slider
                    ListItem(
                        headlineContent = { Text("Transparencia") },
                        supportingContent = {
                            Column {
                                Text("$transparencyPct%")
                                Slider(
                                    value = bgAlpha,
                                    onValueChange = { v ->
                                        bgAlpha = v
                                        val newArgb = ((v * 255).toInt() shl 24) or (bgColor.toArgb() and 0x00FFFFFF)
                                        bgColor = Color(newArgb)
                                        WidgetSettings.setBackgroundColor(context, newArgb)
                                        WidgetSettings.refreshAllWidgets(context)
                                    },
                                    valueRange = 0f..1f,
                                    steps = 9,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider()
                    // Icon color
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
                    HorizontalDivider()
                    // Text color
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
                    HorizontalDivider()
                    // Icon shape chips
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

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ----------------------------------------------------------------
    // Bottom sheets
    // ----------------------------------------------------------------

    if (showContentStyleSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showContentStyleSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Text("Contenido", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
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
            currentColor = bgColor,
            showTransparencyNote = true,
            onColorSelected = { color ->
                // Preserve current alpha when changing color
                val alpha = bgColor.toArgb() and 0xFF000000.toInt()
                val rgb = color.toArgb() and 0x00FFFFFF
                val newArgb = alpha or rgb
                bgColor = Color(newArgb)
                WidgetSettings.setBackgroundColor(context, newArgb)
                WidgetSettings.refreshAllWidgets(context)
            },
            onDismiss = { showBgColorSheet = false }
        )
    }

    if (showIconColorSheet) {
        ColorPickerSheet(
            title = "Color del icono",
            currentColor = iconColor,
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
            currentColor = textColor,
            onColorSelected = { color ->
                textColor = color
                WidgetSettings.setTextColor(context, color.toArgb())
                WidgetSettings.refreshAllWidgets(context)
            },
            onDismiss = { showTextColorSheet = false }
        )
    }
}
