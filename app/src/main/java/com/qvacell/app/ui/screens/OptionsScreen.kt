package com.qvacell.app.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.BuildConfig
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.ThemeMode
import com.qvacell.app.service.DialService
import com.qvacell.app.service.UpdateChecker
import com.qvacell.app.service.UpdateCheckResult
import com.qvacell.app.service.UpdateInfo
import com.qvacell.app.service.SimUtils
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

private val INTEREST_LINKS = listOf(
    "Login Nauta Hogar" to "https://secure.etecsa.net:8443/",
    "Portal Nauta" to "https://www.nauta.cu/",
    "Tienda Web ETECSA" to "https://tienda.etecsa.cu/",
    "Páginas Amarillas de Cuba" to "https://pamarillas.cu/",
    "Webmail Correo Nauta" to "https://webmail.nauta.cu/",
    "Webmail Correo ENET" to "https://webmail.enet.cu/",
    "Portal Nauta (antiguo)" to "https://www.portal.nauta.cu/",
)

sealed class OptionsDestination {
    data object Reminders : OptionsDestination()
    data object SmsServices : OptionsDestination()
    data object WifiRooms : OptionsDestination()
    data object DirectorySearch : OptionsDestination()
    data object Help : OptionsDestination()
    data object YellowPagesSearch : OptionsDestination()
    data object FriendsPlanManage : OptionsDestination()
    data object TransferPinManage : OptionsDestination()
    data object HomeWidgets : OptionsDestination()
    data object VoiceShortcuts : OptionsDestination()
    data object DashboardSettings : OptionsDestination()
    data object CallerID : OptionsDestination()
}

@Composable
fun OptionsScreen(onNavigate: (OptionsDestination) -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsDataStore(context) }
    val repository = remember { CatalogRepository(context) }
    val onCodeClick = rememberCodeActionHandler()

    val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val defaultTab by settings.defaultTab.collectAsStateWithLifecycle(initialValue = "home")
    val accentColor by settings.accentColor.collectAsStateWithLifecycle(
        initialValue = SettingsDataStore.DEFAULT_ACCENT_COLOR
    )
    val quickPurchaseNoConfirm by settings.quickPurchaseNoConfirmDefault.collectAsStateWithLifecycle(initialValue = false)
    val showNetworkStatus by settings.showNetworkStatus.collectAsStateWithLifecycle(initialValue = false)
    val contactCallMode by settings.contactCallMode.collectAsStateWithLifecycle(initialValue = "dialer")
    val quickActionsStyle by settings.quickActionsStyle.collectAsStateWithLifecycle(initialValue = "filled")
    val manualHiddenSections by settings.manualHiddenSections.collectAsStateWithLifecycle(initialValue = emptySet())
    // Persisted (not local @State) so it survives across launches.
    // 5 taps in 3 seconds on the version text toggles unlock / hide.
    val dbSearchUnlocked by settings.debugDatabaseSearchEnabled.collectAsStateWithLifecycle(initialValue = false)
    val ussdCaptureEnabled by settings.ussdCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)

    val isCallerIdRoleHeld = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else false
    }

    val ussdCapturePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch { settings.setUssdCaptureEnabled(granted) }
    }

    val selectedSimSlot by settings.selectedSimSlot.collectAsStateWithLifecycle(initialValue = -1)
    val autoUpdateCheck by settings.autoUpdateCheck.collectAsStateWithLifecycle(initialValue = "unlocked")
    val activeSims = remember { SimUtils.getActiveSubscriptions(context) }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showDefaultTabSheet by remember { mutableStateOf(false) }
    var showContactCallModeSheet by remember { mutableStateOf(false) }
    var showAccentColorSheet by remember { mutableStateOf(false) }
    var showQuickActionsStyleSheet by remember { mutableStateOf(false) }
    var showManualVisibilitySheet by remember { mutableStateOf(false) }
    var showAdvanceBalanceSheet by remember { mutableStateOf(false) }
    var showInterestLinksSheet by remember { mutableStateOf(false) }
    var showSimSheet by remember { mutableStateOf(false) }
    var showAutoUpdateSheet by remember { mutableStateOf(false) }

    var updateChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(autoUpdateCheck) {
        if (autoUpdateCheck == "none") return@LaunchedEffect
        val result = UpdateChecker.check(targetMode = autoUpdateCheck)
        if (result is UpdateCheckResult.Available) {
            updateInfo = result.info
            showUpdateDialog = true
        }
    }

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
    val payPerUseCode = remember { repository.findCodeById("data-pay-per-use") }
    val advanceBalanceCodes = remember {
        listOf("advance-balance-25", "advance-balance-50").mapNotNull { repository.findCodeById(it) }
    }

    fun onVersionTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 3000) versionTapCount = 0
        lastTapTime = now
        versionTapCount++
        if (versionTapCount >= 5) {
            versionTapCount = 0
            val newState = !dbSearchUnlocked
            scope.launch { settings.setDebugDatabaseSearchEnabled(newState) }
            val message = if (newState) "Buscar en BBDD desbloqueada" else "Buscar en BBDD ocultada"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val themeModeLabel = when (themeMode) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }
    val defaultTabLabel = bottomTabs.firstOrNull { it.route == defaultTab }?.label ?: "Inicio"
    val quickActionsStyleLabel = when (quickActionsStyle) {
        "outline" -> "Contorno"
        "list" -> "Lista"
        else -> "Rellenas"
    }
    val manualVisibilityLabel = if (manualHiddenSections.isEmpty()) "Todas visibles"
        else "${manualHiddenSections.size} acción${if (manualHiddenSections.size > 1) "es" else ""} oculta${if (manualHiddenSections.size > 1) "s" else ""}"
    val accentColorLabel = ACCENT_COLOR_OPTIONS.firstOrNull { it.first.equals(accentColor, ignoreCase = true) }?.second
        ?: accentColor
    val autoUpdateLabel = when (autoUpdateCheck) {
        "playstore" -> "Versión Play Store"
        "none" -> "No buscar"
        else -> "Versión Unlocked"
    }
    val simLabel = if (activeSims.size < 2) null
    else {
        val idx = activeSims.indexOfFirst { it.simSlotIndex == selectedSimSlot }
        if (idx >= 0) SimUtils.simLabel(context, activeSims[idx]) else "Predeterminada del sistema"
    }

    Scaffold(topBar = { TopAppBar(modifier = Modifier.padding(top = 12.dp), title = { Text("Opciones") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OptionsSection(header = "Cuenta") {
                    if (payPerUseCode != null) {
                        OptionsRow(
                            headline = payPerUseCode.title.value,
                            icon = Icons.Filled.DataUsage,
                            accentHeadline = true,
                            trailingContent = {
                                Icon(
                                    Icons.Filled.ArrowOutward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = { DialService.dialDirect(context, payPerUseCode.resolvedCode()) }
                        )
                        OptionsDivider()
                    }
                    configuracionesCodes.forEach { code ->
                        val isAccentLink = code.id in setOf("sms-2266-lte", "sms-2266-imei", "sms-4222-mms-config")
                        OptionsRow(
                            headline = code.title.value,
                            icon = resolveAndroidIcon(code.icon),
                            accentHeadline = isAccentLink,
                            trailingContent = if (isAccentLink) ({
                                val arrowIcon = if (code.id == "sms-2266-lte") Icons.Filled.ArrowOutward
                                               else Icons.AutoMirrored.Filled.ArrowForward
                                Icon(
                                    arrowIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }) else null,
                            onClick = { onCodeClick(code) }
                        )
                        OptionsDivider()
                    }
                    if (advanceBalanceCodes.isNotEmpty()) {
                        OptionsRow(
                            headline = "Adelanta Saldo",
                            supporting = "Solicita un adelanto de saldo a ETECSA",
                            icon = Icons.Filled.Payments,
                            onClick = { showAdvanceBalanceSheet = true }
                        )
                        OptionsDivider()
                    }
                    OptionsRow(
                        headline = "Gestionar Plan Amigo",
                        supporting = "Activa, desactiva o modifica tu Plan Amigo",
                        icon = Icons.Filled.People,
                        onClick = { onNavigate(OptionsDestination.FriendsPlanManage) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Gestionar PIN de Transferencia",
                        supporting = "Cambia o guarda tu PIN de transferencia",
                        icon = Icons.Filled.Lock,
                        onClick = { onNavigate(OptionsDestination.TransferPinManage) }
                    )
                }
            }

            item {
                OptionsSection(header = "Utilidades") {
                    OptionsRow(
                        headline = "Recordatorios",
                        supporting = "Avisos para recargar o comprar paquetes",
                        icon = Icons.Filled.Notifications,
                        onClick = { onNavigate(OptionsDestination.Reminders) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Servicios por SMS",
                        supporting = "Horóscopos, noticias, recetas y más por SMS",
                        icon = Icons.Filled.Sms,
                        onClick = { onNavigate(OptionsDestination.SmsServices) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Salas de Navegación WiFi",
                        supporting = "Ubica salas y puntos de acceso por provincia",
                        icon = Icons.Filled.Wifi,
                        onClick = { onNavigate(OptionsDestination.WifiRooms) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Buscar en Directorio",
                        supporting = "Directorio telefónico de ETECSA (en desarrollo)",
                        icon = Icons.Filled.Search,
                        onClick = { onNavigate(OptionsDestination.YellowPagesSearch) }
                    )
                    if (dbSearchUnlocked) {
                        OptionsDivider()
                        OptionsRow(
                            headline = "Buscar en BBDD",
                            supporting = "Búsqueda inversa por número en una base de datos local",
                            icon = Icons.Filled.Storage,
                            onClick = { onNavigate(OptionsDestination.DirectorySearch) }
                        )
                    }
                }
            }

            item {
                OptionsSection(header = "Preferencias") {
                    OptionsRow(
                        headline = "Tema",
                        supporting = themeModeLabel,
                        onClick = { showThemeSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
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
                    OptionsDivider()
                    OptionsRow(
                        headline = "Pestaña predeterminada",
                        supporting = defaultTabLabel,
                        onClick = { showDefaultTabSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Llamadas *99 y ocultas en Contactos",
                        supporting = if (contactCallMode == "direct") "Marcación directa" else "App Teléfono",
                        onClick = { showContactCallModeSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
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
                    OptionsDivider()
                    OptionsRow(
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
                    OptionsDivider()
                    OptionsRow(
                        headline = "Buscar actualizaciones",
                        supporting = autoUpdateLabel,
                        onClick = { showAutoUpdateSheet = true }
                    )
                    if (simLabel != null) {
                        OptionsDivider()
                        val selectedSim = activeSims.firstOrNull { it.simSlotIndex == selectedSimSlot }
                        OptionsRow(
                            headline = "SIM predeterminada",
                            supporting = simLabel,
                            trailingContent = selectedSim?.let { sim ->
                                {
                                    Icon(
                                        Icons.Filled.SimCard,
                                        contentDescription = null,
                                        tint = SimUtils.simColor(sim.simSlotIndex),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            onClick = { showSimSheet = true }
                        )
                    }
                }
            }

            item {
                OptionsSection(header = "Dashboards") {
                    OptionsRow(
                        headline = "Estilo de acciones",
                        supporting = quickActionsStyleLabel,
                        onClick = { showQuickActionsStyleSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Acciones visibles",
                        supporting = manualVisibilityLabel,
                        onClick = { showManualVisibilitySheet = true }
                    )
                    if (BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                        OptionsDivider()
                        OptionsRow(
                            headline = "Funcionamiento del Dashboard",
                            supporting = "Conoce y configura cómo funciona nuestro dashboard dinámico",
                            onClick = { onNavigate(OptionsDestination.DashboardSettings) }
                        )
                    }
                }
            }

            item {
                OptionsSection(header = "Acerca de") {
                    OptionsRow(
                        headline = "Enlaces de interés",
                        supporting = "Portales y servicios en línea de ETECSA",
                        icon = Icons.Filled.Link,
                        onClick = { showInterestLinksSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Identificador de Llamadas",
                        supporting = if (isCallerIdRoleHeld) "Activo" else "Solicitar rol de selección de llamadas",
                        icon = if (isCallerIdRoleHeld) Icons.Filled.PhoneInTalk else Icons.Filled.PhoneDisabled,
                        onClick = { onNavigate(OptionsDestination.CallerID) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Widgets de Inicio",
                        supporting = "Widgets para la pantalla de inicio de tu teléfono",
                        icon = Icons.Filled.Widgets,
                        onClick = { onNavigate(OptionsDestination.HomeWidgets) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Atajos de Voz (Gemini)",
                        supporting = "Controla Qvacell con tu voz mediante Gemini",
                        icon = Icons.Filled.Mic,
                        onClick = { onNavigate(OptionsDestination.VoiceShortcuts) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Ayuda",
                        supporting = "Preguntas frecuentes y recursos de soporte",
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = { onNavigate(OptionsDestination.Help) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = "Buscar Actualizaciones",
                        supporting = if (updateChecking) "Buscando..." else "Verificar si hay una nueva versión disponible",
                        icon = Icons.Filled.SystemUpdate,
                        trailingContent = if (updateChecking) ({
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }) else null,
                        onClick = {
                            if (!updateChecking) {
                                updateChecking = true
                                scope.launch {
                                    val result = UpdateChecker.check(targetMode = autoUpdateCheck)
                                    updateChecking = false
                                    when (result) {
                                        is UpdateCheckResult.Available -> {
                                            updateInfo = result.info
                                            showUpdateDialog = true
                                        }
                                        UpdateCheckResult.UpToDate -> Toast.makeText(
                                            context, "Estás usando la versión más reciente", Toast.LENGTH_SHORT
                                        ).show()
                                        UpdateCheckResult.Failed -> Toast.makeText(
                                            context, "No se pudo comprobar la versión. Revisa tu conexión e inténtalo de nuevo.", Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    )
                    OptionsDivider()
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp).padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "No afiliado, respaldado ni patrocinado por ETECSA. Los códigos pueden cambiar en cualquier momento a discreción del operador.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVersionTap() }
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "by @albertolicea00",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showSimSheet && activeSims.size >= 2) {
        ModalBottomSheet(onDismissRequest = { showSimSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "SIM predeterminada",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { settings.setSelectedSimSlot(-1) }
                            showSimSheet = false
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedSimSlot < 0, onClick = null)
                    Icon(
                        Icons.Filled.SimCard,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("Predeterminada", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Usar ajuste o selección del sistema",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                activeSims.forEachIndexed { _, sim ->
                    val bitmap = remember(sim.subscriptionId) {
                        try { sim.createIconBitmap(context) } catch (_: Exception) { null }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settings.setSelectedSimSlot(sim.simSlotIndex) }
                                showSimSheet = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedSimSlot == sim.simSlotIndex, onClick = null)
                        Icon(
                            Icons.Filled.SimCard,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp).size(28.dp),
                            tint = SimUtils.simColor(sim.simSlotIndex)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(SimUtils.simLabel(sim), style = MaterialTheme.typography.bodyLarge)
                            val carrier = sim.carrierName?.toString()?.takeIf { it.isNotBlank() }
                            val number = SimUtils.getPhoneNumber(context, sim)
                            val subtitle = listOfNotNull(carrier, number).joinToString(" · ")
                            if (subtitle.isNotBlank() && subtitle != SimUtils.simLabel(sim)) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
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

    if (showContactCallModeSheet) {
        ModalBottomSheet(onDismissRequest = { showContactCallModeSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Llamadas *99 y ocultas en Contactos",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf(
                    "dialer" to "Abrir App Teléfono",
                    "direct" to "Marcación directa"
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settings.setContactCallMode(mode) }
                                showContactCallModeSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = contactCallMode == mode, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
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

    if (showQuickActionsStyleSheet) {
        ModalBottomSheet(onDismissRequest = { showQuickActionsStyleSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Estilo de acciones",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf(
                    "filled" to "Rellenas",
                    "outline" to "Contorno",
                    "list" to "Lista"
                ).forEach { (style, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settings.setQuickActionsStyle(style) }
                                showQuickActionsStyleSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = quickActionsStyle == style, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showManualVisibilitySheet) {
        ModalBottomSheet(onDismissRequest = { showManualVisibilitySheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Acciones visibles",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf(
                    // "consultas" to "Consultas",
                    "transferir" to "Transferir Saldo",
                    "recargar" to "Recargar Saldo",
                    "adelanta" to "Adelanta Saldo"
                ).forEach { (key, label) ->
                    val isHidden = key in manualHiddenSections
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = if (isHidden) manualHiddenSections - key
                                    else manualHiddenSections + key
                                scope.launch { settings.setManualHiddenSections(updated) }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = !isHidden, onCheckedChange = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showAutoUpdateSheet) {
        ModalBottomSheet(onDismissRequest = { showAutoUpdateSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Buscar actualizaciones",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf(
                    "unlocked" to "Versión Unlocked",
                    "playstore" to "Versión Play Store",
                    "none" to "No buscar"
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settings.setAutoUpdateCheck(mode) }
                                showAutoUpdateSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = autoUpdateCheck == mode, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        val downloadUrl = when (autoUpdateCheck) {
            "playstore" -> info.storeApkUrl ?: info.htmlUrl
            else -> info.unlockedApkUrl ?: info.htmlUrl
        }
        val titleText = if (info.isFlavorUpgrade) "Versión Unlocked disponible" else "Nueva actualización disponible"
        val bodyText = if (info.isFlavorUpgrade) {
            "Pasa a la edición Unlocked para disfrutar de la experiencia completa de Qvacell.\n\nInstalada: v${BuildConfig.VERSION_NAME}  →  Disponible: v${info.versionName}"
        } else {
            "Hay una nueva versión lista para instalar.\n\nInstalada: v${BuildConfig.VERSION_NAME}  →  Nueva: v${info.versionName}"
        }
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            icon = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
            title = { Text(titleText) },
            text = { Text(bodyText) },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                    context.startActivity(intent)
                }) {
                    Text("Descargar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Ahora no")
                }
            }
        )
    }

    if (showAdvanceBalanceSheet && advanceBalanceCodes.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showAdvanceBalanceSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Adelanta Saldo",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column {
                        advanceBalanceCodes.forEachIndexed { index, code ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        DialService.dialDirect(context, code.resolvedCode())
                                        showAdvanceBalanceSheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        code.price ?: code.title.value,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        code.details.value,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Filled.ArrowOutward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInterestLinksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInterestLinksSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Enlaces de interés",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column {
                        INTEREST_LINKS.forEachIndexed { index, (title, url) ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showInterestLinksSheet = false
                                    uriHandler.openUri(url)
                                }.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(url, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(
                                    Icons.Filled.ArrowOutward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Section header + surrounding card — same grouping style as Compras/Servicios por SMS. */
@Composable
internal fun OptionsSection(header: String, content: @Composable ColumnScope.() -> Unit) {
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
internal fun OptionsRow(
    headline: String,
    supporting: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    accentHeadline: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(
                headline,
                color = if (accentHeadline) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
        },
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
internal fun OptionsDivider() {
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


