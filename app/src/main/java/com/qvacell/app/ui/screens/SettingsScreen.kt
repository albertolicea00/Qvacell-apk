package com.qvacell.app.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.ThemeMode
import com.qvacell.app.service.DashboardCapture
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.DialService
import com.qvacell.app.ui.components.ColorWheelPicker
import com.qvacell.app.ui.components.DialogActionRow
import com.qvacell.app.ui.components.rememberCodeActionHandler
import com.qvacell.app.ui.navigation.bottomTabs
import com.qvacell.app.ui.resolveAndroidIcon
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
    data object YellowPagesSearch : SettingsDestination()
    data object FriendsPlanManage : SettingsDestination()
    data object TransferPinManage : SettingsDestination()
    data object HomeWidgets : SettingsDestination()
    data object VoiceShortcuts : SettingsDestination()
}

@Composable
fun SettingsScreen(onNavigate: (SettingsDestination) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsDataStore(context) }
    val repository = remember { CatalogRepository(context) }
    val dashboardRepository = remember { DashboardDataRepository(context) }
    val onCodeClick = rememberCodeActionHandler()

    val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val defaultTab by settings.defaultTab.collectAsStateWithLifecycle(initialValue = "home")
    val accentColor by settings.accentColor.collectAsStateWithLifecycle(
        initialValue = SettingsDataStore.DEFAULT_ACCENT_COLOR
    )
    val quickPurchaseNoConfirm by settings.quickPurchaseNoConfirmDefault.collectAsStateWithLifecycle(initialValue = false)
    val showNetworkStatus by settings.showNetworkStatus.collectAsStateWithLifecycle(initialValue = false)
    // Persisted (not local @State) so the unlock survives across launches, and one-way only —
    // once found, it stays found. Matches iOS's 5-taps-in-3-seconds gesture on the version text.
    val dbSearchUnlocked by settings.debugDatabaseSearchEnabled.collectAsStateWithLifecycle(initialValue = false)
    val ussdCaptureEnabled by settings.ussdCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    val smsCaptureEnabled by settings.smsCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    val hasRunSmsBackfill by settings.hasRunSmsBackfill.collectAsStateWithLifecycle(initialValue = false)

    val ussdCapturePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch { settings.setUssdCaptureEnabled(granted) }
    }

    val smsCallLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        scope.launch { settings.setSmsCaptureEnabled(granted) }
        DashboardCapture.scheduleEstimationIfEnabled(context, granted)
        if (granted && !hasRunSmsBackfill) {
            DashboardCapture.triggerSmsBackfill(context, dashboardRepository)
            scope.launch { settings.setHasRunSmsBackfill(true) }
        }
    }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showDefaultTabSheet by remember { mutableStateOf(false) }
    var showAccentColorSheet by remember { mutableStateOf(false) }

    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    val configuracionesCodes = remember {
        repository.loadCatalog().categories
            .firstOrNull { it.id == "home" }
            ?.groups
            ?.firstOrNull { it.name?.value == "Configuraciones" }
            ?.codes
            .orEmpty()
    }

    fun onVersionTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 3000) versionTapCount = 0
        lastTapTime = now
        versionTapCount++
        if (versionTapCount >= 5) {
            versionTapCount = 0
            if (!dbSearchUnlocked) {
                scope.launch { settings.setDebugDatabaseSearchEnabled(true) }
                Toast.makeText(context, "Buscar en BBDD desbloqueada", Toast.LENGTH_SHORT).show()
            }
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
                    SettingsDivider()
                    SettingsRow(
                        headline = "Aviso de señal celular",
                        supporting = "Muestra un banner con la calidad de la señal antes de marcar",
                        trailingContent = {
                            Switch(
                                checked = showNetworkStatus,
                                onCheckedChange = { checked ->
                                    scope.launch { settings.setShowNetworkStatus(checked) }
                                }
                            )
                        }
                    )
                }
            }

            if (BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                item {
                    SettingsSection(header = "Datos del Dashboard (experimental)") {
                        SettingsRow(
                            headline = "Consulta automática de saldo",
                            supporting = "Lee la respuesta USSD sin abrir el marcador, usando el mismo permiso de Compras.",
                            trailingContent = {
                                Switch(
                                    checked = ussdCaptureEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked && !DialService.hasCallPermission(context)) {
                                            ussdCapturePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                        } else {
                                            scope.launch { settings.setUssdCaptureEnabled(checked) }
                                        }
                                    }
                                )
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            headline = "Detección automática por SMS",
                            supporting = "Lee los SMS de saldo de ETECSA (nuevos e historial) y el registro de llamadas para estimar tu consumo.",
                            trailingContent = {
                                Switch(
                                    checked = smsCaptureEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            smsCallLogPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.RECEIVE_SMS,
                                                    Manifest.permission.READ_SMS,
                                                    Manifest.permission.READ_CALL_LOG
                                                )
                                            )
                                        } else {
                                            scope.launch { settings.setSmsCaptureEnabled(false) }
                                        }
                                    }
                                )
                            }
                        )
                    }
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
                    SettingsDivider()
                    SettingsRow(
                        headline = "Buscar en Directorio",
                        supporting = "Directorio telefónico de ETECSA (en desarrollo)",
                        icon = Icons.Filled.Search,
                        onClick = { onNavigate(SettingsDestination.YellowPagesSearch) }
                    )
                    if (dbSearchUnlocked) {
                        SettingsDivider()
                        SettingsRow(
                            headline = "Buscar en BBDD",
                            supporting = "Búsqueda inversa por número en una base de datos local",
                            onClick = { onNavigate(SettingsDestination.DirectorySearch) }
                        )
                    }
                }
            }

            item {
                SettingsSection(header = "Cuenta") {
                    configuracionesCodes.forEach { code ->
                        SettingsRow(
                            headline = code.title.value,
                            icon = resolveAndroidIcon(code.icon),
                            onClick = { onCodeClick(code) }
                        )
                        SettingsDivider()
                    }
                    SettingsRow(
                        headline = "Gestionar Plan Amigo",
                        onClick = { onNavigate(SettingsDestination.FriendsPlanManage) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        headline = "Gestionar PIN de Transferencia",
                        onClick = { onNavigate(SettingsDestination.TransferPinManage) }
                    )
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
                    SettingsRow(
                        headline = "Widgets de Inicio",
                        onClick = { onNavigate(SettingsDestination.HomeWidgets) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        headline = "Atajos de Voz (Gemini)",
                        onClick = { onNavigate(SettingsDestination.VoiceShortcuts) }
                    )
                    SettingsDivider()
                    SettingsRow(headline = "Ayuda", onClick = { onNavigate(SettingsDestination.Help) })
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
                    }
                }
            }
            if (showCustomColorInput) {
                DialogActionRow(
                    cancelText = "Cancelar",
                    confirmText = "Aplicar",
                    onCancel = { showCustomColorInput = false },
                    onConfirm = {
                        val hex = "#%06X".format(0xFFFFFF and customColor.toArgb())
                        scope.launch { settings.setAccentColor(hex) }
                        showCustomColorInput = false
                        showAccentColorSheet = false
                    }
                )
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
        supportingContent = supporting?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
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
