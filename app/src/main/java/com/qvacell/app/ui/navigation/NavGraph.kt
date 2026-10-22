package com.qvacell.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.qvacell.app.R

/**
 * SF Symbol "square.grid.2x2" equivalent in Material Icons ([Icons.Filled.GridView]).
 */
val SquareGrid2x2: ImageVector = Icons.Filled.GridView

sealed class BottomTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val label: String
) {
    data object Helplines : BottomTab("helplines", R.string.tab_helplines, Icons.Filled.Headset, "Ayuda")
    data object Contacts : BottomTab("contacts", R.string.tab_contacts, Icons.Filled.Person, "Contactos")
    data object Home : BottomTab("home", R.string.tab_home, Icons.Filled.Home, "Inicio")
    data object Purchase : BottomTab("purchase", R.string.tab_purchase, Icons.Filled.ShoppingCart, "Compras")
    data object Options : BottomTab("options", R.string.tab_options, SquareGrid2x2, "Opciones")

    @Deprecated("Renamed to Options", ReplaceWith("Options"))
    val Settings get() = Options
}

val bottomTabs = listOf(
    BottomTab.Helplines,
    BottomTab.Contacts,
    BottomTab.Home,
    BottomTab.Purchase,
    BottomTab.Options
)

object Routes {
    const val ONBOARDING = "onboarding"
    const val REMINDERS = "settings/reminders"
    const val REMINDER_EDIT = "settings/reminders/edit?templateKey={templateKey}&reminderId={reminderId}"
    fun reminderEdit(templateKey: String? = null, reminderId: String? = null): String {
        val params = mutableListOf<String>()
        if (templateKey != null) params.add("templateKey=$templateKey")
        if (reminderId != null) params.add("reminderId=$reminderId")
        return "settings/reminders/edit" + if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    }
    const val SMS_SERVICES = "sms"
    const val WIFI_PROVINCES = "settings/wifi"
    const val RADIO_NETWORK_HELP = "settings/radio-network"
    const val WIFI_PROVINCE_DETAIL = "settings/wifi/{province}"
    const val DIRECTORY_SEARCH = "settings/directory"
    const val HELP = "settings/help"
    const val YELLOW_PAGES_SEARCH = "settings/yellow-pages"
    const val FRIENDS_PLAN_MANAGE = "settings/friends-plan"
    const val TRANSFER_PIN_MANAGE = "settings/transfer-pin"
    const val HOME_WIDGETS = "settings/widgets"
    const val VOICE_SHORTCUTS = "settings/voice-shortcuts"
    const val DASHBOARD_SETTINGS = "settings/dashboard"
    const val DASHBOARD_HELP = "settings/dashboard-help"
    const val DASHBOARD_EVENTS = "dashboard/events"
}
