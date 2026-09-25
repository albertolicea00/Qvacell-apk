package com.qvacell.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Helplines : BottomTab("helplines", "Ayuda", Icons.Filled.Headset)
    data object Contacts : BottomTab("contacts", "Contactos", Icons.Filled.Person)
    data object Home : BottomTab("home", "Inicio", Icons.Filled.Home)
    data object Purchase : BottomTab("purchase", "Compras", Icons.Filled.ShoppingCart)
    data object Settings : BottomTab("settings", "Ajustes", Icons.Filled.Settings)
}

val bottomTabs = listOf(
    BottomTab.Helplines,
    BottomTab.Contacts,
    BottomTab.Home,
    BottomTab.Purchase,
    BottomTab.Settings
)

object Routes {
    const val REMINDERS = "settings/reminders"
    const val REMINDER_EDIT = "settings/reminders/edit"
    const val SMS_SERVICES = "sms"
    const val WIFI_PROVINCES = "settings/wifi"
    const val WIFI_PROVINCE_DETAIL = "settings/wifi/{province}"
    const val DIRECTORY_SEARCH = "settings/directory"
    const val HELP = "settings/help"
}
