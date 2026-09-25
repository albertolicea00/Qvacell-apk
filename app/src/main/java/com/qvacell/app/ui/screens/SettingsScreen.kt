package com.qvacell.app.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.BuildConfig
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.ThemeMode
import com.qvacell.app.service.TransferPinStore
import com.qvacell.app.ui.components.ColorWheelPicker
import com.qvacell.app.ui.components.RoundedTextField
import com.qvacell.app.ui.navigation.bottomTabs
import kotlinx.coroutines.launch

private val ACCENT_COLOR_OPTIONS = listOf(
    "#0099CC" to "Cian (predeterminado)",
    "#1976D2" to "Azul",
    "#6750A4" to "Morado",
    "#2E7D32" to "Verde",
    "#EF6C00" to "Naranja",
    "#D81B60" to "Rosa"
)

sealed class SettingsDestination {
    data object Reminders : SettingsDestination()
    data object SmsServices : SettingsDestination()
    data object WifiRooms : SettingsDestination()
    data object DirectorySearch : SettingsDestination()
    data object Help : SettingsDestination()
}

@Composable
fun SettingsScreen(onNavigate: (SettingsDestination) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsDataStore(context) }

    val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val defaultTab by settings.defaultTab.collectAsStateWithLifecycle(initialValue = "home")
    val accentColor by settings.accentColor.collectAsStateWithLifecycle(
        initialValue = SettingsDataStore.DEFAULT_ACCENT_COLOR
    )
    val quickPurchaseNoConfirm by settings.quickPurchaseNoConfirmDefault.collectAsStateWithLifecycle(initialValue = false)

    var showThemeSheet by remember { mutableStateOf(false) }
    var showDefaultTabSheet by remember { mutableStateOf(false) }
    var showAccentColorSheet by remember { mutableStateOf(false) }
    var showTransferPinSheet by remember { mutableStateOf(false) }

    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var debugDbSearchVisible by remember { mutableStateOf(false) }

    fun onVersionTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 3000) versionTapCount = 0
        lastTapTime = now
        versionTapCount++
        if (versionTapCount >= 5) {
            debugDbSearchVisible = true
            versionTapCount = 0
        }
    }

    val themeModeLabel = when (themeMode) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }
    val defaultTabLabel = bottomTabs.firstOrNull { it.route == defaultTab }?.label ?: "Inicio"
    val accentColorLabel = ACCENT_COLOR_OPTIONS.firstOrNull { it.first.equals(accentColor, ignoreCase = true) }?.second
        ?: accentColor

    Scaffold(topBar = { TopAppBar(title = { Text("Ajustes") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(header = "Preferencias") {
                    SettingsRow(
                        headline = "Tema",
                        supporting = themeModeLabel,
                        onClick = { showThemeSheet = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        headline = "Pestaña predeterminada",
                        supporting = defaultTabLabel,
                        onClick = { showDefaultTabSheet = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        headline = "Color de acento",
                        supporting = accentColorLabel,
                        onClick = { showAccentColorSheet = true },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(runCatching { Color(android.graphics.Color.parseColor(accentColor)) }.getOrDefault(MaterialTheme.colorScheme.primary))
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        headline = "Acción sin Confirmación",
                        supporting = "En Compras, marca el código saltando el paso de confirmación de ETECSA.",
                        trailingContent = {
                            Switch(
                                checked = quickPurchaseNoConfirm,
                                onCheckedChange = { checked ->
                                    scope.launch { settings.setQuickPurchaseNoConfirmDefault(checked) }
                                }
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(header = "Utilidades") {
                    SettingsRow(
                        headline = "Recordatorios",
                        supporting = "Avisos para recargar o comprar paquetes",
                        icon = Icons.Filled.Notifications,
                        onClick = { onNavigate(SettingsDestination.Reminders) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        headline = "Servicios por SMS",
                        supporting = "Horóscopos, noticias, recetas y más por SMS",
                        icon = Icons.Filled.Sms,
                        onClick = { onNavigate(SettingsDestination.SmsServices) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        headline = "Salas de Navegación WiFi",
                        supporting = "Ubica salas y puntos de acceso por provincia",
                        icon = Icons.Filled.Wifi,
                        onClick = { onNavigate(SettingsDestination.WifiRooms) }
                    )
                    if (debugDbSearchVisible) {
                        SettingsDivider()
                        SettingsRow(
                            headline = "Búsqueda en Base de Datos",
                            supporting = "Función de depuración",
                            onClick = { onNavigate(SettingsDestination.DirectorySearch) }
                        )
                    }
                }
            }

            item {
                SettingsSection(header = "Cuenta") {
                    SettingsRow(headline = "Clave de Transferencia", onClick = { showTransferPinSheet = true })
                }
            }

            item {
                SettingsSection(header = "Acerca de") {
                    SettingsRow(
                        headline = "Identificador de Llamadas",
                        supporting = "Solicitar rol de selección de llamadas",
                        onClick = { requestCallScreeningRole(context) }
                    )
                    SettingsDivider()
                    SettingsRow(headline = "Ayuda", onClick = { onNavigate(SettingsDestination.Help) })
                    SettingsDivider()
                    SettingsRow(headline = "Código fuente en GitHub")
                    SettingsDivider()
                    SettingsRow(
                        headline = "Versión",
                        supporting = BuildConfig.VERSION_NAME,
                        onClick = { onVersionTap() }
                    )
                }
            }
        }
    }

    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                listOf(
                    ThemeMode.SYSTEM to "Sistema",
                    ThemeMode.LIGHT to "Claro",
                    ThemeMode.DARK to "Oscuro"
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settings.setThemeMode(mode) }
                                showThemeSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == mode, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (showDefaultTabSheet) {
        ModalBottomSheet(onDismissRequest = { showDefaultTabSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Pestaña predeterminada",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                bottomTabs.forEach { tab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settings.setDefaultTab(tab.route) }
                                showDefaultTabSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = defaultTab == tab.route, onClick = null)
                        Text(tab.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (showAccentColorSheet) {
        var showCustomColorInput by remember { mutableStateOf(false) }
        val fallbackAccent = MaterialTheme.colorScheme.primary
        var customColor by remember {
            mutableStateOf(
                runCatching { Color(android.graphics.Color.parseColor(accentColor)) }
                    .getOrDefault(fallbackAccent)
            )
        }

        ModalBottomSheet(onDismissRequest = { showAccentColorSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Color de acento",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ACCENT_COLOR_OPTIONS.forEach { (hex, label) ->
                        val isSelected = hex.equals(accentColor, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable {
                                    scope.launch { settings.setAccentColor(hex) }
                                    showAccentColorSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = label, tint = Color.White)
                            }
                        }
                    }
                    // Personalizado — pick any hex color instead of the fixed palette above.
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showCustomColorInput = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Personalizado")
                    }
                }

                if (showCustomColorInput) {
                    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(customColor)
                            )
                            Text(
                                "#%06X".format(0xFFFFFF and customColor.toArgb()),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        ColorWheelPicker(
                            color = customColor,
                            onColorChange = { customColor = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showCustomColorInput = false },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancelar") }
                            Button(
                                onClick = {
                                    val hex = "#%06X".format(0xFFFFFF and customColor.toArgb())
                                    scope.launch { settings.setAccentColor(hex) }
                                    showCustomColorInput = false
                                    showAccentColorSheet = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Aplicar") }
                        }
                    }
                }
            }
        }
    }

    if (showTransferPinSheet) {
        val pinStore = remember { TransferPinStore(context) }
        var pin by remember { mutableStateOf(pinStore.load() ?: "") }

        ModalBottomSheet(onDismissRequest = { showTransferPinSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Clave de Transferencia", style = MaterialTheme.typography.titleLarge)
                RoundedTextField(value = pin, onValueChange = { pin = it }, label = "Clave")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            pinStore.delete()
                            pin = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Eliminar") }
                    Button(
                        onClick = {
                            pinStore.save(pin)
                            showTransferPinSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Guardar") }
                }
            }
        }
    }
}

/** Section header + surrounding card — same grouping style as Compras/Servicios por SMS. */
@Composable
private fun SettingsSection(header: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            header,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    headline: String,
    supporting: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

private fun requestCallScreeningRole(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
