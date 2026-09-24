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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.BuildConfig
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.ThemeMode
import com.qvacell.app.service.TransferPinStore
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
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SectionHeader("Preferencias") }
            item {
                ListItem(
                    headlineContent = { Text("Tema") },
                    supportingContent = { Text(themeModeLabel) },
                    modifier = Modifier.clickable { showThemeSheet = true }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Pestaña predeterminada") },
                    supportingContent = { Text(defaultTabLabel) },
                    modifier = Modifier.clickable { showDefaultTabSheet = true }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Color de acento") },
                    supportingContent = { Text(accentColorLabel) },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(runCatching { Color(android.graphics.Color.parseColor(accentColor)) }.getOrDefault(MaterialTheme.colorScheme.primary))
                        )
                    },
                    modifier = Modifier.clickable { showAccentColorSheet = true }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Acción sin Confirmación") },
                    supportingContent = { Text("En Compras, marca el código saltando el paso de confirmación de ETECSA.") },
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
            item { HorizontalDivider() }

            item { SectionHeader("Utilidades") }
            item {
                ListItem(
                    headlineContent = { Text("Recordatorios") },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.Reminders) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Servicios por SMS") },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.SmsServices) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Salas de Navegación WiFi") },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.WifiRooms) }
                )
            }
            if (debugDbSearchVisible) {
                item {
                    ListItem(
                        headlineContent = { Text("Búsqueda en Base de Datos") },
                        supportingContent = { Text("Función de depuración") },
                        modifier = Modifier.clickable { onNavigate(SettingsDestination.DirectorySearch) }
                    )
                }
            }
            item { HorizontalDivider() }

            item { SectionHeader("Cuenta") }
            item {
                ListItem(
                    headlineContent = { Text("Clave de Transferencia") },
                    modifier = Modifier.clickable { showTransferPinSheet = true }
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader("Acerca de") }
            item {
                ListItem(
                    headlineContent = { Text("Identificador de Llamadas") },
                    supportingContent = { Text("Solicitar rol de selección de llamadas") },
                    modifier = Modifier.clickable { requestCallScreeningRole(context) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Ayuda") },
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.Help) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Código fuente en GitHub") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Versión") },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                    modifier = Modifier.clickable { onVersionTap() }
                )
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
        var customHex by remember { mutableStateOf(accentColor) }

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
                        val previewColor = runCatching { Color(android.graphics.Color.parseColor(customHex)) }.getOrNull()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(previewColor ?: MaterialTheme.colorScheme.surfaceVariant)
                            )
                            RoundedTextField(
                                value = customHex,
                                onValueChange = { customHex = it },
                                label = "Color personalizado (#RRGGBB)",
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showCustomColorInput = false },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancelar") }
                            Button(
                                onClick = {
                                    scope.launch { settings.setAccentColor(customHex) }
                                    showCustomColorInput = false
                                    showAccentColorSheet = false
                                },
                                enabled = previewColor != null,
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
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
