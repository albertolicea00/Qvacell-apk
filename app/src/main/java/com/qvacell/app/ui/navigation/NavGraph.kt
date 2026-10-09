package com.qvacell.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * SF Symbol "square.grid.2x2" equivalent in Material Icons ([Icons.Filled.GridView]).
 */
val SquareGrid2x2: ImageVector = Icons.Filled.GridView

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Helplines : BottomTab("helplines", "Ayuda", Icons.Filled.Headset)
    data object Contacts : BottomTab("contacts", "Contactos", Icons.Filled.Person)
    data object Home : BottomTab("home", "Inicio", Icons.Filled.Home)
    data object Purchase : BottomTab("purchase", "Compras", Icons.Filled.ShoppingCart)
    data object Options : BottomTab("options", "Opciones", SquareGrid2x2)

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
    const val WIFI_PROVINCE_DETAIL = "settings/wifi/{province}"
    const val DIRECTORY_SEARCH = "settings/directory"
    const val HELP = "settings/help"
    const val YELLOW_PAGES_SEARCH = "settings/yellow-pages"
    const val FRIENDS_PLAN_MANAGE = "settings/friends-plan"
    const val TRANSFER_PIN_MANAGE = "settings/transfer-pin"
    const val HOME_WIDGETS = "settings/widgets"
    const val VOICE_SHORTCUTS = "settings/voice-shortcuts"
    const val CALLER_ID = "settings/caller-id"
}
