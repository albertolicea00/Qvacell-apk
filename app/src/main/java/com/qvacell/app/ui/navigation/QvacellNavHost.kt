package com.qvacell.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qvacell.app.ui.screens.CategoryListScreen
import com.qvacell.app.ui.screens.CodeOptionsScreen
import com.qvacell.app.ui.screens.ContactsListScreen
import com.qvacell.app.ui.screens.DirectorySearchScreen
import com.qvacell.app.ui.screens.HelpScreen
import com.qvacell.app.ui.screens.HomeQuickActionsScreen
import com.qvacell.app.ui.screens.RechargeFlowScreen
import com.qvacell.app.ui.screens.ReminderEditScreen
import com.qvacell.app.ui.screens.ReminderListScreen
import com.qvacell.app.ui.screens.SettingsDestination
import com.qvacell.app.ui.screens.SettingsScreen
import com.qvacell.app.ui.screens.TransferFlowScreen
import com.qvacell.app.ui.screens.TransferPinScreen
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
    Routes.TRANSFER_PIN,
    Routes.HELP
)

@Composable
fun QvacellNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Used both to keep the Ajustes tab highlighted while inside one of its nested destinations,
    // and to know when tapping it should just pop back to its root instead of navigating like a
    // normal tab switch.
    val isInSettingsSection = currentRoute in SETTINGS_NESTED_ROUTES

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = currentRoute == tab.route ||
                        (tab is BottomTab.Settings && isInSettingsSection)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab is BottomTab.Settings && isInSettingsSection) {
                                navController.popBackStack(BottomTab.Settings.route, inclusive = false)
                            } else {
                                navController.navigate(tab.route) {
                                    popUpTo(BottomTab.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Home.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(BottomTab.Helplines.route) {
                CategoryListScreen(
                    categoryId = "helplines",
                    title = "Líneas de Ayuda",
                    onOpenCodeOptions = { codeId -> navController.navigate(Routes.codeOptions(codeId)) }
                )
            }
            composable(BottomTab.Contacts.route) {
                ContactsListScreen()
            }
            composable(BottomTab.Home.route) {
                HomeQuickActionsScreen(
                    onOpenTransfer = { navController.navigate(Routes.TRANSFER) },
                    onOpenRecharge = { navController.navigate(Routes.RECHARGE) }
                )
            }
            composable(BottomTab.Purchase.route) {
                CategoryListScreen(
                    categoryId = "purchase",
                    title = "Compras",
                    onOpenCodeOptions = { codeId -> navController.navigate(Routes.codeOptions(codeId)) }
                )
            }
            composable(BottomTab.Settings.route) {
                SettingsScreen(onNavigate = { destination ->
                    val route = when (destination) {
                        SettingsDestination.Reminders -> Routes.REMINDERS
                        SettingsDestination.SmsServices -> Routes.SMS_SERVICES
                        SettingsDestination.WifiRooms -> Routes.WIFI_PROVINCES
                        SettingsDestination.DirectorySearch -> Routes.DIRECTORY_SEARCH
                        SettingsDestination.TransferPin -> Routes.TRANSFER_PIN
                        SettingsDestination.Help -> Routes.HELP
                    }
                    navController.navigate(route)
                })
            }

            composable(Routes.TRANSFER) {
                TransferFlowScreen(
                    onPickContact = { navController.navigate(BottomTab.Contacts.route) },
                    onDone = { navController.popBackStack() }
                )
            }
            composable(
                Routes.TRANSFER_WITH_NUMBER,
                arguments = listOf(navArgument("number") { type = NavType.StringType })
            ) { backStack ->
                TransferFlowScreen(
                    prefilledNumber = backStack.arguments?.getString("number"),
                    onPickContact = { navController.navigate(BottomTab.Contacts.route) },
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Routes.RECHARGE) {
                RechargeFlowScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.REMINDERS) {
                ReminderListScreen(
                    onAdd = { navController.navigate(Routes.REMINDER_EDIT) },
                    onEdit = {}
                )
            }
            composable(Routes.REMINDER_EDIT) {
                ReminderEditScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.SMS_SERVICES) {
                CategoryListScreen(
                    categoryId = "sms",
                    title = "Servicios por SMS",
                    onOpenCodeOptions = { codeId -> navController.navigate(Routes.codeOptions(codeId)) }
                )
            }
            composable(
                Routes.CODE_OPTIONS,
                arguments = listOf(navArgument("codeId") { type = NavType.StringType })
            ) { backStack ->
                CodeOptionsScreen(codeId = backStack.arguments?.getString("codeId") ?: "")
            }
            composable(Routes.WIFI_PROVINCES) {
                WifiProvinceListScreen(onProvinceSelected = { province ->
                    navController.navigate("settings/wifi/${java.net.URLEncoder.encode(province, "UTF-8")}")
                })
            }
            composable(
                Routes.WIFI_PROVINCE_DETAIL,
                arguments = listOf(navArgument("province") { type = NavType.StringType })
            ) { backStack ->
                val encoded = backStack.arguments?.getString("province") ?: ""
                WifiProvinceDetailScreen(provinceName = java.net.URLDecoder.decode(encoded, "UTF-8"))
            }
            composable(Routes.DIRECTORY_SEARCH) { DirectorySearchScreen() }
            composable(Routes.TRANSFER_PIN) { TransferPinScreen() }
            composable(Routes.HELP) { HelpScreen() }
        }
    }
}
