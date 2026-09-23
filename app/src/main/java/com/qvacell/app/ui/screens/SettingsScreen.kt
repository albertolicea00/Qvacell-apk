package com.qvacell.app.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.BuildConfig
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.ThemeMode
import com.qvacell.app.service.DialService
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
    data object TransferPin : SettingsDestination()
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

    var showThemeDialog by remember { mutableStateOf(false) }
    var showDefaultTabDialog by remember { mutableStateOf(false) }
    var showAccentColorDialog by remember { mutableStateOf(false) }

    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var debugDbSearchVisible by remember { mutableStateOf(false) }

    var directDialEnabled by remember { mutableStateOf(DialService.isDirectDialEnabled(context)) }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Denied: leave the setting off rather than silently having no effect later.
        DialService.setDirectDialEnabled(context, granted)
        directDialEnabled = granted
    }

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
                    modifier = Modifier.clickable { showThemeDialog = true }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Pestaña predeterminada") },
                    supportingContent = { Text(defaultTabLabel) },
                    modifier = Modifier.clickable { showDefaultTabDialog = true }
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
                    modifier = Modifier.clickable { showAccentColorDialog = true }
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
                    modifier = Modifier.clickable { onNavigate(SettingsDestination.TransferPin) }
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
                    headlineContent = { Text("Marcar Directamente") },
                    supportingContent = {
                        Text(
                            if (directDialEnabled) {
                                "La app marca el número directamente, sin pasar por el marcador del teléfono."
                            } else {
                                "Los códigos se abren en el marcador del teléfono para confirmar antes de llamar."
                            }
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = directDialEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val alreadyGranted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.CALL_PHONE
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (alreadyGranted) {
                                        DialService.setDirectDialEnabled(context, true)
                                        directDialEnabled = true
                                    } else {
                                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                    }
                                } else {
                                    DialService.setDirectDialEnabled(context, false)
                                    directDialEnabled = false
                                }
                            }
                        )
                    }
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

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Tema") },
            text = {
                Column {
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
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeMode == mode, onClick = null)
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cerrar") }
            }
        )
    }

    if (showDefaultTabDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultTabDialog = false },
            title = { Text("Pestaña predeterminada") },
            text = {
                Column {
                    bottomTabs.forEach { tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { settings.setDefaultTab(tab.route) }
                                    showDefaultTabDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = defaultTab == tab.route, onClick = null)
                            Text(tab.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultTabDialog = false }) { Text("Cerrar") }
            }
        )
    }

    if (showAccentColorDialog) {
        AlertDialog(
            onDismissRequest = { showAccentColorDialog = false },
            title = { Text("Color de acento") },
            text = {
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
                                    showAccentColorDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = label, tint = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccentColorDialog = false }) { Text("Cerrar") }
            }
        )
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
