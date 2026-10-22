package com.qvacell.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.res.stringResource
import com.qvacell.app.R
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.qvacell.app.BuildConfig
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.SimAppearance
import com.qvacell.app.data.ThemeMode
import com.qvacell.app.service.DialService
import com.qvacell.app.service.UpdateChecker
import com.qvacell.app.service.UpdateCheckResult
import com.qvacell.app.service.UpdateInfo
import com.qvacell.app.service.SimUtils
import com.qvacell.app.ui.components.SimSelectorIcon
import com.qvacell.app.ui.components.ColorWheelPicker
import com.qvacell.app.ui.components.DialogActionRow
import com.qvacell.app.ui.components.UpdateAvailableDialog
import com.qvacell.app.ui.components.SimAppearanceEditor
import com.qvacell.app.ui.components.SimBadge
import com.qvacell.app.ui.components.SimCardGlyph
import com.qvacell.app.ui.components.rememberCodeActionHandler
import com.qvacell.app.ui.resolveAndroidIcon
import com.qvacell.app.ui.navigation.bottomTabs
import kotlinx.coroutines.launch

private val ACCENT_COLOR_OPTIONS = listOf(
    "#0099CC" to R.string.options_color_cyan,
    "#1976D2" to R.string.options_color_blue,
    "#6750A4" to R.string.options_color_purple,
    "#2E7D32" to R.string.options_color_green,
    "#EF6C00" to R.string.options_color_orange,
    "#D81B60" to R.string.options_color_pink
)

private val INTEREST_LINKS = listOf(
    R.string.options_link_nauta_home to "https://secure.etecsa.net:8443/",
    R.string.options_link_nauta_portal to "https://www.nauta.cu/",
    R.string.options_link_corporate_portal to "https://www.portalcorporativo.etecsa.cu/",
    R.string.options_link_etecsa_store to "https://tienda.etecsa.cu/",
    R.string.options_link_yellow_pages to "https://pamarillas.cu/",
    R.string.options_link_nauta_mail to "https://webmail.nauta.cu/",
    R.string.options_link_enet_mail to "https://webmail.enet.cu/",
    R.string.options_link_old_portal to "https://www.portal.nauta.cu/",
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
    data object RadioNetworkHelp : OptionsDestination()
}

@Composable
fun OptionsScreen(onNavigate: (OptionsDestination) -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsDataStore(context) }
    val repository = remember { CatalogRepository(context) }
    val onCodeClick = rememberCodeActionHandler()
    val simSettingsCodes = remember {
        listOf("data-pay-per-use", "sms-4222-mms-config", "sms-2266-imei", "sms-2266-lte")
            .mapNotNull { repository.findCodeById(it) }
    }

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

    val ussdCapturePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch { settings.setUssdCaptureEnabled(granted) }
    }

    val selectedSimSlot by settings.selectedSimSlot.collectAsStateWithLifecycle(initialValue = -1)
    val simAppearances by settings.simAppearances.collectAsStateWithLifecycle(initialValue = emptyMap())
    val simSwitchAction by settings.simSwitchAction.collectAsStateWithLifecycle(initialValue = "sheet")
    val autoUpdateCheck by settings.autoUpdateCheck.collectAsStateWithLifecycle(initialValue = "unlocked")
    val effectiveAutoUpdateCheck = if (BuildConfig.DASHBOARD_CAPTURE_ENABLED && autoUpdateCheck == "playstore") {
        "unlocked"
    } else autoUpdateCheck
    val isPlayStoreInstall = remember(context) { UpdateChecker.isPlayStoreInstall(context) }
    var activeSims by remember { mutableStateOf(SimUtils.getActiveSubscriptions(context)) }
    var phoneStateGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
    }
    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        phoneStateGranted = granted
        activeSims = SimUtils.getActiveSubscriptions(context)
    }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showDefaultTabSheet by remember { mutableStateOf(false) }
    var showContactCallModeSheet by remember { mutableStateOf(false) }
    var showAccentColorSheet by remember { mutableStateOf(false) }
    var showQuickActionsStyleSheet by remember { mutableStateOf(false) }
    var showManualVisibilitySheet by remember { mutableStateOf(false) }
    var showAdvanceBalanceSheet by remember { mutableStateOf(false) }
    var showInterestLinksSheet by remember { mutableStateOf(false) }
    var showSimSettingsSheet by remember { mutableStateOf(false) }
    var showSimSheet by remember { mutableStateOf(false) }
    var showSimSwitchActionSheet by remember { mutableStateOf(false) }
    var editingSim by remember { mutableStateOf<android.telephony.SubscriptionInfo?>(null) }
    var showAutoUpdateSheet by remember { mutableStateOf(false) }

    var updateChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateTargetMode by remember { mutableStateOf("unlocked") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(isPlayStoreInstall, autoUpdateCheck) {
        if (isPlayStoreInstall && autoUpdateCheck != "none") settings.setAutoUpdateCheck("none")
        else if (BuildConfig.DASHBOARD_CAPTURE_ENABLED && autoUpdateCheck == "playstore") {
            settings.setAutoUpdateCheck("unlocked")
        }
    }
    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var versionTapHandled by remember { mutableStateOf(false) }

    val advanceBalanceCodes = remember {
        listOf("advance-balance-25", "advance-balance-50").mapNotNull { repository.findCodeById(it) }
    }

    fun onVersionTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 3000) {
            versionTapCount = 0
            versionTapHandled = false
        }
        lastTapTime = now
        if (versionTapHandled) return
        versionTapCount++
        if (versionTapCount >= 5) {
            versionTapHandled = true
            val newState = !dbSearchUnlocked
            scope.launch { settings.setDebugDatabaseSearchEnabled(newState) }
            val message = context.getString(if (newState) R.string.options_db_unlocked else R.string.options_db_hidden)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val themeModeLabel = when (themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.options_theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.options_theme_light)
        ThemeMode.DARK -> stringResource(R.string.options_theme_dark)
    }
    val defaultTabLabel = bottomTabs.firstOrNull { it.route == defaultTab }?.labelRes?.let { stringResource(it) } ?: stringResource(R.string.tab_home)
    val simSwitchActionLabel = when (simSwitchAction) {
        "cycle" -> stringResource(R.string.options_sim_switch_cycle)
        "menu" -> stringResource(R.string.options_sim_switch_menu)
        else -> stringResource(R.string.options_sim_switch_sheet)
    }
    val quickActionsStyleLabel = when (quickActionsStyle) {
        "outline" -> stringResource(R.string.options_quick_actions_outline)
        "list" -> stringResource(R.string.options_quick_actions_list)
        else -> stringResource(R.string.options_quick_actions_filled)
    }
    val manualVisibilityLabel = if (manualHiddenSections.isEmpty()) stringResource(R.string.options_dashboards_visible_all)
        else if (manualHiddenSections.size == 1) stringResource(R.string.options_hidden_action_singular)
        else stringResource(R.string.options_hidden_action_plural, manualHiddenSections.size)
    val accentColorLabel = ACCENT_COLOR_OPTIONS.firstOrNull { it.first.equals(accentColor, ignoreCase = true) }?.second?.let { stringResource(it) }
        ?: accentColor
    val autoUpdateLabel = when (effectiveAutoUpdateCheck) {
        "playstore" -> stringResource(R.string.options_auto_update_playstore)
        "none" -> stringResource(R.string.options_auto_update_none)
        else -> stringResource(R.string.options_unlocked_version)
    }
    val effectiveSimSlot = SimUtils.resolveSelectedSlot(context, selectedSimSlot)
    val simLabel = if (activeSims.size < 2) null
    else {
        val idx = activeSims.indexOfFirst { it.simSlotIndex == effectiveSimSlot }
        if (idx >= 0) SimUtils.simLabel(context, activeSims[idx], simAppearances[activeSims[idx].subscriptionId.toString()])
        else null
    }

    Scaffold(topBar = {
        TopAppBar(
            modifier = Modifier.padding(top = 12.dp),
            title = { Text(stringResource(R.string.tab_options)) },
            actions = { SimSelectorIcon() }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OptionsSection(header = stringResource(R.string.options_section_account_short)) {
                    OptionsRow(
                        headline = stringResource(R.string.options_about_links),
                        supporting = stringResource(R.string.options_about_links_desc),
                        icon = Icons.Filled.Link,
                        onClick = { showInterestLinksSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.options_sim_settings_title),
                        supporting = stringResource(R.string.options_sim_settings_desc),
                        icon = Icons.Filled.SimCard,
                        onClick = { showSimSettingsSheet = true }
                    )
                    OptionsDivider()
                    if (advanceBalanceCodes.isNotEmpty()) {
                        OptionsRow(
                            headline = stringResource(R.string.options_account_advance_balance),
                            supporting = stringResource(R.string.options_account_advance_balance_desc),
                            icon = Icons.Filled.Payments,
                            onClick = { showAdvanceBalanceSheet = true }
                        )
                        OptionsDivider()
                    }
                    OptionsRow(
                        headline = stringResource(R.string.help_friends_plan_q),
                        supporting = stringResource(R.string.options_account_friends_plan_desc),
                        icon = Icons.Filled.People,
                        onClick = { onNavigate(OptionsDestination.FriendsPlanManage) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.options_manage_transfer_pin),
                        supporting = stringResource(R.string.options_account_transfer_pin_desc),
                        icon = Icons.Filled.Lock,
                        onClick = { onNavigate(OptionsDestination.TransferPinManage) }
                    )
                }
            }

            item {
                OptionsSection(header = stringResource(R.string.options_section_tools_short)) {
                    OptionsRow(
                        headline = stringResource(R.string.options_force_lte_title),
                        supporting = stringResource(R.string.options_force_lte_desc),
                        icon = Icons.Filled.Wifi,
                        onClick = { onNavigate(OptionsDestination.RadioNetworkHelp) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.reminders_title),
                        supporting = stringResource(R.string.options_tools_reminders_desc),
                        icon = Icons.Filled.Notifications,
                        onClick = { onNavigate(OptionsDestination.Reminders) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.help_sms_title),
                        supporting = stringResource(R.string.options_tools_sms_desc),
                        icon = Icons.Filled.Sms,
                        onClick = { onNavigate(OptionsDestination.SmsServices) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.options_wifi_rooms),
                        supporting = stringResource(R.string.options_tools_wifi_desc),
                        icon = Icons.Filled.Wifi,
                        onClick = { onNavigate(OptionsDestination.WifiRooms) }
                    )
                    // OptionsDivider()
                    // OptionsRow(
                    //     headline = stringResource(R.string.options_tools_directory),
                    //     supporting = stringResource(R.string.options_tools_directory_desc),
                    //     icon = Icons.Filled.Search,
                    //     onClick = { onNavigate(OptionsDestination.YellowPagesSearch) }
                    // )
                    if (dbSearchUnlocked) {
                        OptionsDivider()
                        OptionsRow(
                            headline = stringResource(R.string.options_tools_db_search),
                            supporting = stringResource(R.string.options_tools_db_search_desc),
                            icon = Icons.Filled.Storage,
                            onClick = { onNavigate(OptionsDestination.DirectorySearch) }
                        )
                    }
                }
            }

            item {
                OptionsSection(header = stringResource(R.string.options_section_preferences)) {
                    OptionsRow(
                        headline = stringResource(R.string.options_theme),
                        supporting = themeModeLabel,
                        onClick = { showThemeSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.options_accent_color),
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
                        headline = stringResource(R.string.options_default_tab),
                        supporting = defaultTabLabel,
                        onClick = { showDefaultTabSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.options_pref_call_contacts),
                        supporting = if (contactCallMode == "direct") stringResource(R.string.options_pref_call_contacts_direct) else stringResource(R.string.options_pref_call_contacts_dialer),
                        onClick = { showContactCallModeSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.options_pref_network_title),
                        supporting = stringResource(R.string.options_pref_network_desc),
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
                        headline = stringResource(R.string.options_pref_quick_title),
                        supporting = stringResource(R.string.options_pref_quick_desc),
                        trailingContent = {
                            Switch(
                                checked = quickPurchaseNoConfirm,
                                onCheckedChange = { checked ->
                                    scope.launch { settings.setQuickPurchaseNoConfirmDefault(checked) }
                                }
                            )
                        }
                    )
                    if (!isPlayStoreInstall) {
                        OptionsDivider()
                        OptionsRow(
                            headline = stringResource(R.string.options_auto_update),
                            supporting = autoUpdateLabel,
                            onClick = { showAutoUpdateSheet = true }
                        )
                    }
                }
            }

            item {
                OptionsSection(header = stringResource(R.string.options_sim_section)) {
                    if (activeSims.size >= 2) {
                        OptionsRow(
                            headline = stringResource(R.string.options_sim_title),
                            supporting = simLabel ?: SimUtils.simLabel(context, activeSims.first()),
                            onClick = { showSimSheet = true }
                        )
                        OptionsDivider()
                        OptionsRow(
                            headline = stringResource(R.string.options_sim_switch_action),
                            supporting = simSwitchActionLabel,
                            onClick = { showSimSwitchActionSheet = true }
                        )
                        OptionsDivider()
                    }
                    when {
                        activeSims.isNotEmpty() -> activeSims.forEachIndexed { index, sim ->
                            if (index > 0) OptionsDivider()
                            val appearance = simAppearances[sim.subscriptionId.toString()]
                            val simName = appearance?.name?.trim()?.takeIf { it.isNotEmpty() }
                                ?: SimUtils.simLabel(context, sim)
                            OptionsRow(
                                headline = stringResource(R.string.sim_named_slot, simName, sim.simSlotIndex + 1),
                                supporting = stringResource(
                                    if (appearance?.isCorporate == true) R.string.sim_corporate_line else R.string.sim_personal_line
                                ),
                                leadingContent = {
                                    SimCardGlyph(
                                        slotIndex = sim.simSlotIndex,
                                        icon = appearance?.icon ?: "number",
                                        color = SimUtils.simColor(sim.simSlotIndex, appearance),
                                        width = 34.dp,
                                        height = 40.dp
                                    )
                                },
                                onClick = { editingSim = sim }
                            )
                        }
                        !phoneStateGranted -> OptionsRow(
                            headline = stringResource(R.string.options_sim_type),
                            supporting = stringResource(R.string.onboarding_sim_permission),
                            icon = Icons.Filled.SimCard,
                            onClick = { phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
                        )
                        else -> OptionsRow(
                            headline = stringResource(R.string.options_sim_type),
                            supporting = stringResource(R.string.options_sim_not_detected),
                            icon = Icons.Filled.SimCard
                        )
                    }
                }
            }

            item {
                OptionsSection(header = stringResource(R.string.options_dashboards_section)) {
                    OptionsRow(
                        headline = stringResource(R.string.options_dashboards_style),
                        supporting = quickActionsStyleLabel,
                        onClick = { showQuickActionsStyleSheet = true }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.options_dashboards_visible),
                        supporting = manualVisibilityLabel,
                        onClick = { showManualVisibilitySheet = true }
                    )
                    if (BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                        OptionsDivider()
                        OptionsRow(
                            headline = stringResource(R.string.options_dashboards_how_it_works),
                            supporting = stringResource(R.string.options_dashboards_how_it_works_desc),
                            onClick = { onNavigate(OptionsDestination.DashboardSettings) }
                        )
                    }
                }
            }

            item {
                OptionsSection(header = stringResource(R.string.options_section_about)) {
                    OptionsRow(
                        headline = stringResource(R.string.options_about_widgets),
                        supporting = stringResource(R.string.options_about_widgets_desc),
                        icon = Icons.Filled.Widgets,
                        onClick = { onNavigate(OptionsDestination.HomeWidgets) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.options_about_gemini),
                        supporting = stringResource(R.string.options_about_gemini_desc),
                        icon = Icons.Filled.Mic,
                        onClick = { onNavigate(OptionsDestination.VoiceShortcuts) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.help_title),
                        supporting = stringResource(R.string.options_about_help_desc),
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = { onNavigate(OptionsDestination.Help) }
                    )
                    OptionsDivider()
                    OptionsRow(
                        headline = stringResource(R.string.options_about_updates),
                        supporting = if (updateChecking) stringResource(R.string.options_checking) else stringResource(R.string.options_about_updates_desc),
                        icon = Icons.Filled.SystemUpdate,
                        trailingContent = if (updateChecking) ({
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }) else null,
                        onClick = {
                            if (!updateChecking) {
                                updateChecking = true
                                scope.launch {
                                    val result = UpdateChecker.check(targetMode = "unlocked")
                                    updateChecking = false
                                    when (result) {
                                        is UpdateCheckResult.Available -> {
                                            updateInfo = result.info
                                            updateTargetMode = "unlocked"
                                            showUpdateDialog = true
                                        }
                                        UpdateCheckResult.UpToDate -> Toast.makeText(
                                            context, context.getString(R.string.options_update_latest), Toast.LENGTH_SHORT
                                        ).show()
                                        UpdateCheckResult.Failed -> Toast.makeText(
                                            context, context.getString(R.string.options_update_failed), Toast.LENGTH_LONG
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
                            stringResource(R.string.options_about_disclaimer),
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
                        stringResource(R.string.options_version_label, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
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
                    stringResource(R.string.options_sim_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                activeSims.forEach { sim ->
                    val appearance = simAppearances[sim.subscriptionId.toString()]
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
                        RadioButton(selected = effectiveSimSlot == sim.simSlotIndex, onClick = null)
                        SimBadge(sim.simSlotIndex, appearance, modifier = Modifier.padding(start = 8.dp))
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(SimUtils.simLabel(sim, appearance), style = MaterialTheme.typography.bodyLarge)
                            val carrier = sim.carrierName?.toString()?.takeIf { it.isNotBlank() }
                            val number = SimUtils.getPhoneNumber(context, sim)
                            val subtitle = listOfNotNull(carrier, number).joinToString(" · ")
                            if (subtitle.isNotBlank() && subtitle != SimUtils.simLabel(sim, appearance)) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                stringResource(if (appearance?.isCorporate == true) R.string.sim_corporate_line else R.string.sim_personal_line),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = {
                            showSimSheet = false
                            editingSim = sim
                        }) { Text(stringResource(R.string.sim_customize)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showSimSwitchActionSheet && activeSims.size >= 2) {
        ModalBottomSheet(onDismissRequest = { showSimSwitchActionSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.options_sim_switch_action),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                listOf(
                    Triple("sheet", R.string.options_sim_switch_sheet, R.string.options_sim_switch_sheet_desc),
                    Triple("cycle", R.string.options_sim_switch_cycle, R.string.options_sim_switch_cycle_desc),
                    Triple("menu", R.string.options_sim_switch_menu, R.string.options_sim_switch_menu_desc)
                ).forEach { (action, titleRes, descRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settings.setSimSwitchAction(action) }
                                showSimSwitchActionSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = simSwitchAction == action, onClick = null)
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    editingSim?.let { sim ->
        SimAppearanceEditor(
            sim = sim,
            appearance = simAppearances[sim.subscriptionId.toString()] ?: SimAppearance(),
            showDefaultOption = activeSims.size >= 2,
            isDefault = sim.simSlotIndex == effectiveSimSlot,
            onMakeDefault = { scope.launch { settings.setSelectedSimSlot(sim.simSlotIndex) } },
            onSave = { appearance ->
                scope.launch {
                    settings.setSimAppearance(sim.subscriptionId, appearance)
                    editingSim = null
                }
            },
            onDismiss = { editingSim = null }
        )
    }

    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.options_theme), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.options_theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.options_theme_light),
                    ThemeMode.DARK to stringResource(R.string.options_theme_dark)
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
                    stringResource(R.string.options_default_tab),
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
                        Text(stringResource(tab.labelRes), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (showContactCallModeSheet) {
        ModalBottomSheet(onDismissRequest = { showContactCallModeSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.options_pref_call_contacts),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf(
                    "dialer" to stringResource(R.string.options_open_phone_app),
                    "direct" to stringResource(R.string.options_pref_call_contacts_direct)
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
                    stringResource(R.string.options_accent_color),
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
                                Icon(Icons.Filled.Check, contentDescription = stringResource(label), tint = Color.White)
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
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.recurrence_custom))
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
                    cancelText = stringResource(R.string.action_cancel),
                    confirmText = stringResource(R.string.action_apply),
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
                    stringResource(R.string.options_dashboards_style),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf(
                    "filled" to stringResource(R.string.options_quick_actions_filled),
                    "outline" to stringResource(R.string.options_quick_actions_outline),
                    "list" to stringResource(R.string.options_quick_actions_list)
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
                    stringResource(R.string.options_dashboards_visible),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf(
                    // "consultas" to "Consultas",
                    "transferir" to stringResource(R.string.reminders_template_transfer_title),
                    "recargar" to stringResource(R.string.template_recharge_card),
                    "adelanta" to stringResource(R.string.options_account_advance_balance)
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

    if (showAutoUpdateSheet && !isPlayStoreInstall) {
        ModalBottomSheet(onDismissRequest = { showAutoUpdateSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.options_auto_update),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val updateModes = buildList {
                    add("unlocked" to stringResource(R.string.options_unlocked_version))
                    // The Play Store channel only applies to the store flavor.
                    if (!BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                        add("playstore" to stringResource(R.string.options_auto_update_playstore))
                    }
                    add("none" to stringResource(R.string.options_auto_update_none))
                }
                updateModes.forEach { (mode, label) ->
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
                        RadioButton(selected = effectiveAutoUpdateCheck == mode, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdateAvailableDialog(
            info = updateInfo!!,
            targetMode = updateTargetMode,
            onDismiss = { showUpdateDialog = false }
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
                    stringResource(R.string.options_account_advance_balance),
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

    if (showSimSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSimSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.options_sim_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column {
                        simSettingsCodes.forEachIndexed { index, code ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showSimSettingsSheet = false
                                    if (code.id == "data-pay-per-use") {
                                        DialService.dialDirect(context, code.resolvedCode())
                                    } else {
                                        onCodeClick(code)
                                    }
                                }.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(resolveAndroidIcon(code.icon), contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(code.title.value, style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(code.details.value, style = MaterialTheme.typography.bodySmall,
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
                    stringResource(R.string.options_about_links),
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
                                    Text(stringResource(title), style = MaterialTheme.typography.bodyLarge,
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
    trailingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null
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
        leadingContent = leadingContent ?: icon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}

@Composable
internal fun OptionsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
