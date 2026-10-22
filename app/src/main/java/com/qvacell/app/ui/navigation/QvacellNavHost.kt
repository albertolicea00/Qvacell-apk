package com.qvacell.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.ui.components.ConnectionBanner
import com.qvacell.app.ui.components.hasCellularService
import com.qvacell.app.service.DashboardRefreshService
import com.qvacell.app.service.LineTypeSuggestion
import com.qvacell.app.service.SimUtils
import com.qvacell.app.service.AppTelemetry
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.qvacell.app.R
import com.qvacell.app.BuildConfig
import com.qvacell.app.service.UpdateCheckResult
import com.qvacell.app.service.UpdateChecker
import com.qvacell.app.service.UpdateInfo
import com.qvacell.app.ui.components.UpdateAvailableDialog
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.qvacell.app.ui.screens.CategoryListScreen
import com.qvacell.app.ui.screens.ContactsListScreen
import com.qvacell.app.ui.screens.DashboardHelpScreen
import com.qvacell.app.ui.screens.DashboardEventsScreen
import com.qvacell.app.ui.screens.DashboardSettingsScreen
import com.qvacell.app.ui.screens.DirectorySearchScreen
import com.qvacell.app.ui.screens.FriendsPlanManageScreen
import com.qvacell.app.ui.screens.HelpScreen
import com.qvacell.app.ui.screens.HomeQuickActionsScreen
import com.qvacell.app.ui.screens.OnboardingScreen
import com.qvacell.app.ui.screens.HomeWidgetsScreen
import com.qvacell.app.ui.screens.PlaceholderScreen
import com.qvacell.app.ui.screens.OptionsDestination
import com.qvacell.app.ui.screens.OptionsScreen
import com.qvacell.app.ui.screens.ReminderEditScreen
import com.qvacell.app.ui.screens.ReminderListScreen
import com.qvacell.app.ui.screens.RadioNetworkHelpScreen
import com.qvacell.app.ui.screens.TransferPinManageScreen
import com.qvacell.app.ui.screens.VoiceShortcutsScreen
import com.qvacell.app.ui.screens.WifiProvinceDetailScreen
import com.qvacell.app.ui.screens.WifiProvinceListScreen

// Every destination pushed from within Options — not all share the "options/..." or "settings/..." route prefix
// (SMS_SERVICES is just "sms"), so this is spelled out explicitly rather than string-matched.
private val OPTIONS_NESTED_ROUTES = setOf(
    Routes.REMINDERS,
    Routes.REMINDER_EDIT,
    Routes.SMS_SERVICES,
    Routes.WIFI_PROVINCES,
    Routes.WIFI_PROVINCE_DETAIL,
    Routes.DIRECTORY_SEARCH,
    Routes.HELP,
    Routes.YELLOW_PAGES_SEARCH,
    Routes.FRIENDS_PLAN_MANAGE,
    Routes.TRANSFER_PIN_MANAGE,
    Routes.HOME_WIDGETS,
    Routes.VOICE_SHORTCUTS,
    Routes.RADIO_NETWORK_HELP,
    Routes.DASHBOARD_SETTINGS,
    Routes.DASHBOARD_HELP,
)

private val HOME_NESTED_ROUTES = setOf(
    Routes.DASHBOARD_EVENTS,
)

// Shown on the bottom bar's Options tab instead of the generic icon while inside one of
// these nested screens, so the tab reflects where you actually are (Recordatorios, SMS, WiFi...).
private val OPTIONS_ROUTE_ICONS: Map<String, ImageVector> = mapOf(
    Routes.REMINDERS to Icons.Filled.Notifications,
    Routes.REMINDER_EDIT to Icons.Filled.Notifications,
    Routes.SMS_SERVICES to Icons.Filled.Sms,
    Routes.WIFI_PROVINCES to Icons.Filled.Wifi,
    Routes.WIFI_PROVINCE_DETAIL to Icons.Filled.Wifi,
    Routes.DIRECTORY_SEARCH to Icons.Filled.Storage,
    Routes.HELP to Icons.AutoMirrored.Filled.Help,
    Routes.YELLOW_PAGES_SEARCH to Icons.Filled.Search,
    Routes.FRIENDS_PLAN_MANAGE to Icons.Filled.People,
    Routes.TRANSFER_PIN_MANAGE to Icons.Filled.Key,
    Routes.HOME_WIDGETS to Icons.Filled.Widgets,
    Routes.VOICE_SHORTCUTS to Icons.Filled.Mic,
    Routes.RADIO_NETWORK_HELP to Icons.Filled.Wifi,
    Routes.DASHBOARD_SETTINGS to Icons.Filled.Dashboard,
    Routes.DASHBOARD_HELP to Icons.AutoMirrored.Filled.Help,
)

private val HOME_ROUTE_ICONS: Map<String, ImageVector> = mapOf(
    Routes.DASHBOARD_EVENTS to Icons.Filled.CalendarMonth,
)

@Composable
fun QvacellNavHost(startTabRoute: String = BottomTab.Home.route) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // A stale/unrecognized stored value (e.g. from a future version) falls back to Home rather
    // than crashing NavHost with an unknown start destination. Captured once (no recompute key) —
    // NavHost's startDestination must never change after first composition, or it force-navigates
    // there immediately, which used to yank the user out of Options the moment `startTabRoute`
    // changed (e.g. picking a new "Pestaña predeterminada" while sitting in Options).
    val validStartRoute = remember {
        when {
            startTabRoute == Routes.ONBOARDING -> Routes.ONBOARDING
            bottomTabs.any { it.route == startTabRoute } -> startTabRoute
            else -> BottomTab.Home.route
        }
    }

    // Used both to keep the Options tab highlighted while inside one of its nested destinations,
    // and to know when tapping it should just pop back to its root instead of navigating like a
    // normal tab switch.
    val isInOptionsSection = currentRoute in OPTIONS_NESTED_ROUTES
    val isInHomeSection = currentRoute in HOME_NESTED_ROUTES

    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()
    var foregroundOpenCount by remember { mutableIntStateOf(0) }
    var automaticUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var automaticUpdateMode by remember { mutableStateOf("unlocked") }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { foregroundOpenCount++ }
    LaunchedEffect(foregroundOpenCount, currentRoute != null && currentRoute != Routes.ONBOARDING) {
        if (foregroundOpenCount == 0 || currentRoute == null || currentRoute == Routes.ONBOARDING) {
            return@LaunchedEffect
        }
        if (UpdateChecker.isPlayStoreInstall(context)) return@LaunchedEffect

        val configuredMode = settings.autoUpdateCheck.first()
        val mode = if (BuildConfig.DASHBOARD_CAPTURE_ENABLED && configuredMode == "playstore") {
            settings.setAutoUpdateCheck("unlocked")
            "unlocked"
        } else configuredMode
        when (val result = UpdateChecker.checkIfDue(settings, mode)) {
            is UpdateCheckResult.Available -> {
                automaticUpdateMode = mode
                automaticUpdateInfo = result.info
            }
            else -> Unit // Automatic failures are silent; the next app opening retries.
        }
    }
    val showNetworkStatus by settings.showNetworkStatus.collectAsStateWithLifecycle(initialValue = false)
    val refreshFailure by DashboardRefreshService.refreshFailure.collectAsStateWithLifecycle(initialValue = null)
    val lineTypeSuggestion by DashboardRefreshService.lineTypeSuggestion.collectAsStateWithLifecycle(initialValue = null)
    val selectedSimSlot by settings.selectedSimSlot.collectAsStateWithLifecycle(initialValue = -1)
    val simAppearances by settings.simAppearances.collectAsStateWithLifecycle(initialValue = emptyMap())
    var confirmingLineType by remember { mutableStateOf<LineTypeSuggestion?>(null) }
    var isDynamicDashboard by remember { mutableStateOf(false) }
    var hasSignal by remember { mutableStateOf(true) }
    val showDynamicWarning = currentRoute == BottomTab.Home.route && isDynamicDashboard
    val selectedSubscriptionId = remember(selectedSimSlot) {
        SimUtils.getActiveSubscriptions(context)
            .firstOrNull { it.simSlotIndex == selectedSimSlot }
            ?.subscriptionId
    }
    val visibleLineTypeSuggestion = lineTypeSuggestion?.takeIf {
        it.subscriptionId == selectedSubscriptionId
    }
    val visibleRefreshFailure = if (lineTypeSuggestion != null && visibleLineTypeSuggestion == null) {
        null
    } else refreshFailure
    LaunchedEffect(lineTypeSuggestion, simAppearances) {
        val suggestion = lineTypeSuggestion ?: return@LaunchedEffect
        if (simAppearances[suggestion.subscriptionId.toString()]?.isCorporate == suggestion.isCorporate) {
            DashboardRefreshService.clearLineTypeSuggestion()
        }
    }
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && currentRoute != BottomTab.Home.route) {
            AppTelemetry.screen(context, currentRoute)
        }
    }
    LaunchedEffect(showDynamicWarning) {
        if (!showDynamicWarning) {
            hasSignal = true
        } else {
            while (true) {
                hasSignal = hasCellularService(context)
                delay(5000)
            }
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute != null && currentRoute != Routes.ONBOARDING,
                enter = expandVertically(animationSpec = tween(320), expandFrom = Alignment.Bottom) +
                    fadeIn(animationSpec = tween(320)),
                exit = shrinkVertically(animationSpec = tween(220), shrinkTowards = Alignment.Bottom) +
                    fadeOut(animationSpec = tween(220))
            ) {
            // Custom bar instead of Material3 NavigationBar: that component has a fixed 80dp
            // height with no public override, and its selected-item indicator draws a filled
            // pill behind the icon — both unwanted here. This gives full control over height
            // and drops the indicator, using tint color alone to show the active tab.
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column {
                    if (showNetworkStatus) {
                        ConnectionBanner()
                    }
                    if (showDynamicWarning && (visibleRefreshFailure != null || !hasSignal)) {
                        val warningColor = Color(0xFFFB8C00)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(warningColor.copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = warningColor, modifier = Modifier.size(16.dp))
                            val warningMessage = visibleRefreshFailure ?: stringResource(R.string.network_no_service_pause)
                            val lineTypeAction = visibleLineTypeSuggestion?.let { suggestion ->
                                stringResource(
                                    if (suggestion.isCorporate) R.string.dashboard_line_type_action_corporate
                                    else R.string.dashboard_line_type_action_personal
                                )
                            }
                            val warningText = buildAnnotatedString {
                                append(warningMessage)
                                if (lineTypeAction != null) {
                                    append(" ")
                                    pushStringAnnotation(tag = "change_line_type", annotation = "change")
                                    withStyle(SpanStyle(color = warningColor, textDecoration = TextDecoration.Underline)) {
                                        append(lineTypeAction)
                                    }
                                    pop()
                                }
                            }
                            ClickableText(
                                text = warningText,
                                style = MaterialTheme.typography.labelMedium.copy(color = warningColor),
                                modifier = Modifier.padding(start = 8.dp),
                                onClick = { offset ->
                                    if (warningText.getStringAnnotations("change_line_type", offset, offset).isNotEmpty()) {
                                        confirmingLineType = visibleLineTypeSuggestion
                                    }
                                }
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route ||
                            (tab is BottomTab.Options && isInOptionsSection) ||
                            (tab is BottomTab.Home && isInHomeSection)
                        val tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val tabLabel = stringResource(tab.labelRes)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = selected,
                                    role = Role.Tab,
                                    onClick = {
                                        if (tab is BottomTab.Home && isInHomeSection) {
                                            navController.popBackStack(BottomTab.Home.route, inclusive = false)
                                        } else if (tab is BottomTab.Options && isInOptionsSection) {
                                            val popped = navController.popBackStack(BottomTab.Options.route, inclusive = false)
                                            if (!popped) {
                                                navController.navigate(tab.route) {
                                                    popUpTo(validStartRoute) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        } else {
                                            navController.navigate(tab.route) {
                                                popUpTo(validStartRoute) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val icon = when {
                                tab is BottomTab.Home && isInHomeSection ->
                                    HOME_ROUTE_ICONS[currentRoute] ?: tab.icon
                                tab is BottomTab.Options && isInOptionsSection ->
                                    OPTIONS_ROUTE_ICONS[currentRoute] ?: tab.icon
                                else -> tab.icon
                            }
                            Icon(
                                icon,
                                contentDescription = tabLabel,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                tabLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = tint,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
            }
        }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = validStartRoute,
            // consumeWindowInsets tells Compose the bottom inset this padding represents is
            // already accounted for — without it, every nested screen's own Scaffold (topBar
            // only, no bottomBar) reserves that same system navigation-bar inset again on its
            // own, adding a phantom gap above this bottom bar on every single screen.
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(padding)
        ) {
            composable(BottomTab.Helplines.route) {
                CategoryListScreen(categoryId = "helplines", title = stringResource(R.string.title_helplines))
            }
            composable(BottomTab.Contacts.route) {
                ContactsListScreen()
            }
            composable(
                BottomTab.Home.route,
                enterTransition = {
                    if (initialState.destination.route == Routes.ONBOARDING) {
                        fadeIn(animationSpec = tween(420, delayMillis = 70)) +
                            slideInVertically(initialOffsetY = { it / 12 }, animationSpec = tween(420))
                    } else null
                }
            ) {
                HomeQuickActionsScreen(
                    onNavigateToReminders = { navController.navigate(Routes.REMINDERS) },
                    onNavigateToFriendsPlan = { navController.navigate(Routes.FRIENDS_PLAN_MANAGE) },
                    onNavigateToEvents = { navController.navigate(Routes.DASHBOARD_EVENTS) },
                    onNavigateToSettings = { navController.navigate(Routes.DASHBOARD_SETTINGS) },
                    onNavigateToHelp = { navController.navigate(Routes.DASHBOARD_HELP) },
                    onDynamicDashboardChanged = { isDynamicDashboard = it }
                )
            }
            composable(BottomTab.Purchase.route) {
                CategoryListScreen(
                    categoryId = "purchase",
                    title = stringResource(R.string.title_purchase),
                    onNavigateToSms = { navController.navigate(Routes.SMS_SERVICES) }
                )
            }
            val navigateFromOptions: (OptionsDestination) -> Unit = { destination ->
                val route = when (destination) {
                    OptionsDestination.Reminders -> Routes.REMINDERS
                    OptionsDestination.SmsServices -> Routes.SMS_SERVICES
                    OptionsDestination.WifiRooms -> Routes.WIFI_PROVINCES
                    OptionsDestination.DirectorySearch -> Routes.DIRECTORY_SEARCH
                    OptionsDestination.Help -> Routes.HELP
                    OptionsDestination.YellowPagesSearch -> Routes.YELLOW_PAGES_SEARCH
                    OptionsDestination.FriendsPlanManage -> Routes.FRIENDS_PLAN_MANAGE
                    OptionsDestination.TransferPinManage -> Routes.TRANSFER_PIN_MANAGE
                    OptionsDestination.HomeWidgets -> Routes.HOME_WIDGETS
                    OptionsDestination.VoiceShortcuts -> Routes.VOICE_SHORTCUTS
                    OptionsDestination.DashboardSettings -> Routes.DASHBOARD_SETTINGS
                    OptionsDestination.RadioNetworkHelp -> Routes.RADIO_NETWORK_HELP
                }
                navController.navigate(route)
            }
            composable(BottomTab.Options.route) {
                OptionsScreen(onNavigate = navigateFromOptions)
            }
            composable(
                Routes.ONBOARDING,
                exitTransition = {
                    if (targetState.destination.route == BottomTab.Home.route) {
                        fadeOut(animationSpec = tween(260))
                    } else null
                }
            ) {
                val isReplay = navController.previousBackStackEntry != null
                OnboardingScreen(
                    onComplete = {
                        if (isReplay) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(BottomTab.Home.route) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    },
                    onSkipTour = if (isReplay) {
                        { navController.popBackStack() }
                    } else null
                )
            }

            composable(Routes.REMINDERS) {
                ReminderListScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Routes.REMINDER_EDIT,
                arguments = listOf(
                    navArgument("templateKey") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("reminderId") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                ReminderEditScreen(
                    templateKey = backStackEntry.arguments?.getString("templateKey"),
                    reminderId = backStackEntry.arguments?.getString("reminderId"),
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SMS_SERVICES) {
                CategoryListScreen(
                    categoryId = "sms",
                    title = stringResource(R.string.title_sms_services),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.WIFI_PROVINCES) {
                WifiProvinceListScreen(
                    onProvinceSelected = { province ->
                        navController.navigate("settings/wifi/${java.net.URLEncoder.encode(province, "UTF-8")}")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Routes.WIFI_PROVINCE_DETAIL,
                arguments = listOf(navArgument("province") { type = NavType.StringType })
            ) { backStack ->
                val encoded = backStack.arguments?.getString("province") ?: ""
                WifiProvinceDetailScreen(
                    provinceName = java.net.URLDecoder.decode(encoded, "UTF-8"),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.DIRECTORY_SEARCH) {
                DirectorySearchScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HELP) {
                HelpScreen(
                    onBack = { navController.popBackStack() },
                    onStartTour = { navController.navigate(Routes.ONBOARDING) },
                    onOpenSettings = { navController.navigate(Routes.DASHBOARD_HELP) }
                )
            }
            composable(Routes.YELLOW_PAGES_SEARCH) {
                PlaceholderScreen(
                    title = stringResource(R.string.title_directory_search),
                    description = stringResource(R.string.title_directory_search_placeholder),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.FRIENDS_PLAN_MANAGE) {
                FriendsPlanManageScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TRANSFER_PIN_MANAGE) {
                TransferPinManageScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DASHBOARD_SETTINGS) {
                DashboardSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToHelp = { navController.navigate(Routes.DASHBOARD_HELP) }
                )
            }
            composable(Routes.DASHBOARD_HELP) {
                DashboardHelpScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Routes.DASHBOARD_SETTINGS) }
                )
            }
            composable(Routes.DASHBOARD_EVENTS) {
                DashboardEventsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.RADIO_NETWORK_HELP) {
                RadioNetworkHelpScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HOME_WIDGETS) {
                HomeWidgetsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.VOICE_SHORTCUTS) {
                VoiceShortcutsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
    confirmingLineType?.let { suggestion ->
        AlertDialog(
            onDismissRequest = { confirmingLineType = null },
            title = { Text(stringResource(R.string.dashboard_line_type_confirm_title)) },
            text = {
                Text(stringResource(
                    if (suggestion.isCorporate) R.string.dashboard_line_type_confirm_corporate
                    else R.string.dashboard_line_type_confirm_personal,
                    suggestion.failedCode
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingLineType = null
                    scope.launch {
                        val selectedSlot = settings.selectedSimSlot.first()
                        val selectedSim = SimUtils.getActiveSubscriptions(context)
                            .firstOrNull { it.simSlotIndex == selectedSlot }
                        if (selectedSim?.subscriptionId == suggestion.subscriptionId) {
                            settings.setSimLineType(suggestion.subscriptionId, suggestion.isCorporate)
                        }
                        DashboardRefreshService.clearLineTypeSuggestion()
                    }
                }) { Text(stringResource(R.string.action_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingLineType = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    automaticUpdateInfo?.let { info ->
        UpdateAvailableDialog(
            info = info,
            targetMode = automaticUpdateMode,
            onDismiss = { automaticUpdateInfo = null }
        )
    }
}
