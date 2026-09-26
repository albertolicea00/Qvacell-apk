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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.qvacell.app.ui.screens.PlaceholderScreen
import com.qvacell.app.ui.screens.ReminderEditScreen
import com.qvacell.app.ui.screens.ReminderListScreen
import com.qvacell.app.ui.screens.SettingsDestination
import com.qvacell.app.ui.screens.SettingsScreen
import com.qvacell.app.ui.screens.TransferPinManageScreen
import com.qvacell.app.ui.screens.WifiProvinceDetailScreen
import com.qvacell.app.ui.screens.WifiProvinceListScreen

// Every destination pushed from within Ajustes — not all share the "settings/..." route prefix
// (SMS_SERVICES is just "sms"), so this is spelled out explicitly rather than string-matched.
private val SETTINGS_NESTED_ROUTES = setOf(
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

@Composable
fun QvacellNavHost(startTabRoute: String = BottomTab.Home.route) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // A stale/unrecognized stored value (e.g. from a future version) falls back to Home rather
    // than crashing NavHost with an unknown start destination.
    val validStartRoute = if (bottomTabs.any { it.route == startTabRoute }) startTabRoute else BottomTab.Home.route

    // Used both to keep the Ajustes tab highlighted while inside one of its nested destinations,
    // and to know when tapping it should just pop back to its root instead of navigating like a
    // normal tab switch.
    val isInSettingsSection = currentRoute in SETTINGS_NESTED_ROUTES

    Scaffold(
        bottomBar = {
            // Custom bar instead of Material3 NavigationBar: that component has a fixed 80dp
            // height with no public override, and its selected-item indicator draws a filled
            // pill behind the icon — both unwanted here. This gives full control over height
            // and drops the indicator, using tint color alone to show the active tab.
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column {
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
                            (tab is BottomTab.Settings && isInSettingsSection)
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
                                        if (tab is BottomTab.Settings && isInSettingsSection) {
                                            navController.popBackStack(BottomTab.Settings.route, inclusive = false)
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
                            Icon(
                                tab.icon,
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
            composable(BottomTab.Settings.route) {
                SettingsScreen(onNavigate = { destination ->
                    val route = when (destination) {
                        SettingsDestination.Reminders -> Routes.REMINDERS
                        SettingsDestination.SmsServices -> Routes.SMS_SERVICES
                        SettingsDestination.WifiRooms -> Routes.WIFI_PROVINCES
                        SettingsDestination.DirectorySearch -> Routes.DIRECTORY_SEARCH
                        SettingsDestination.Help -> Routes.HELP
                        SettingsDestination.YellowPagesSearch -> Routes.YELLOW_PAGES_SEARCH
                        SettingsDestination.FriendsPlanManage -> Routes.FRIENDS_PLAN_MANAGE
                        SettingsDestination.TransferPinManage -> Routes.TRANSFER_PIN_MANAGE
                        SettingsDestination.HomeWidgets -> Routes.HOME_WIDGETS
                        SettingsDestination.VoiceShortcuts -> Routes.VOICE_SHORTCUTS
                    }
                    navController.navigate(route)
                })
            }

            composable(Routes.REMINDERS) {
                ReminderListScreen(
                    onAdd = { navController.navigate(Routes.REMINDER_EDIT) },
                    onEdit = {},
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.REMINDER_EDIT) {
                ReminderEditScreen(
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
                HelpScreen(onBack = { navController.popBackStack() })
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
                PlaceholderScreen(
                    title = "Widgets de Inicio",
                    description = "Widgets para la pantalla de inicio de tu teléfono — función en desarrollo.",
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.VOICE_SHORTCUTS) {
                PlaceholderScreen(
                    title = "Atajos de Voz (Gemini)",
                    description = "Controla Qvacell con tu voz mediante Gemini — función en desarrollo.",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
