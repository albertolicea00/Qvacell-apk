package com.qvacell.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.ui.components.ConnectionBanner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qvacell.app.ui.screens.CategoryListScreen
import com.qvacell.app.ui.screens.ContactsListScreen
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
    Routes.VOICE_SHORTCUTS
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
    Routes.CALLER_ID to Icons.Filled.PhoneInTalk
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

    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val showNetworkStatus by settings.showNetworkStatus.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        bottomBar = {
            if (currentRoute == Routes.ONBOARDING) return@Scaffold
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
                            (tab is BottomTab.Options && isInOptionsSection)
                        val tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = selected,
                                    role = Role.Tab,
                                    onClick = {
                                        if (tab is BottomTab.Options && isInOptionsSection) {
                                            navController.popBackStack(BottomTab.Options.route, inclusive = false)
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
                            val icon = if (tab is BottomTab.Options && isInOptionsSection) {
                                OPTIONS_ROUTE_ICONS[currentRoute] ?: tab.icon
                            } else {
                                tab.icon
                            }
                            Icon(
                                icon,
                                contentDescription = tab.label,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                tab.label,
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
                CategoryListScreen(categoryId = "helplines", title = "Líneas de Ayuda")
            }
            composable(BottomTab.Contacts.route) {
                ContactsListScreen()
            }
            composable(BottomTab.Home.route) {
                HomeQuickActionsScreen()
            }
            composable(BottomTab.Purchase.route) {
                CategoryListScreen(categoryId = "purchase", title = "Compras")
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
                    OptionsDestination.CallerID -> Routes.CALLER_ID
                }
                navController.navigate(route)
            }
            composable(BottomTab.Options.route) {
                OptionsScreen(onNavigate = navigateFromOptions)
            }
            composable(Routes.ONBOARDING) {
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
                    title = "Servicios por SMS",
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
                    onStartTour = { navController.navigate(Routes.ONBOARDING) }
                )
            }
            composable(Routes.YELLOW_PAGES_SEARCH) {
                PlaceholderScreen(
                    title = "Buscar en Directorio",
                    description = "Búsqueda en el directorio telefónico de ETECSA — función en desarrollo.",
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.FRIENDS_PLAN_MANAGE) {
                FriendsPlanManageScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TRANSFER_PIN_MANAGE) {
                TransferPinManageScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HOME_WIDGETS) {
                HomeWidgetsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.VOICE_SHORTCUTS) {
                VoiceShortcutsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CALLER_ID) {
                PlaceholderScreen(
                    title = "Identificador de Llamadas",
                    description = "Identifica llamadas entrantes con información del directorio — función en desarrollo.",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
