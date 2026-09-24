package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.UssdCode
import com.qvacell.app.ui.components.QuickActionTileData
import com.qvacell.app.ui.components.QuickActionTileGrid
import com.qvacell.app.ui.components.RechargeBottomSheet
import com.qvacell.app.ui.components.TransferBottomSheet
import com.qvacell.app.ui.components.rememberCodeActionHandler

// Not stored in codes.json — same as iOS, which hardcodes these SF Symbols directly
// in HomeQuickActionsView's tile array instead of reading them from the catalog.
private val QUICK_ACTION_CODE_ICONS: List<Pair<String, ImageVector>> = listOf(
    "main-balance" to Icons.Filled.CreditCard,
    "data-plan" to Icons.Filled.NetworkCell,
    "voice-balance" to Icons.Filled.Call,
    "sms-balance" to Icons.Filled.Sms,
    "national-recharge-limit" to Icons.Filled.Warning,
    "friends-plan" to Icons.Filled.People,
    "bonus-usd-plans" to Icons.Filled.CardGiftcard,
    "postpaid-balance" to Icons.Filled.Apartment
)

@Composable
fun HomeQuickActionsScreen() {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    var showTransferSheet by remember { mutableStateOf(false) }
    var showRechargeSheet by remember { mutableStateOf(false) }
    val catalog = remember { repository.loadCatalog() }
    val homeCategory = remember { catalog.categories.firstOrNull { it.id == "home" } }
    val balanceGroup = remember { homeCategory?.groups?.firstOrNull { it.name?.value == "Saldo y Planes" } }
    val codesById = remember(balanceGroup) { balanceGroup?.codes.orEmpty().associateBy(UssdCode::id) }
    val onCodeClick = rememberCodeActionHandler()

    val tiles = remember(codesById) {
        QUICK_ACTION_CODE_ICONS.mapNotNull { (id, icon) ->
            codesById[id]?.let { code ->
                QuickActionTileData(
                    label = code.title.value,
                    icon = icon,
                    onClick = { onCodeClick(code) }
                )
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Qvacell") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    "Consultas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                QuickActionTileGrid(tiles = tiles)
            }
            item {
                Card(onClick = { showTransferSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Transferir") },
                        supportingContent = { Text("Envía saldo a otro número") },
                        leadingContent = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) }
                    )
                }
            }
            item {
                Card(onClick = { showRechargeSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Recargar") },
                        supportingContent = { Text("Recarga con tarjeta prepago") },
                        leadingContent = { Icon(Icons.Filled.CreditCard, contentDescription = null) }
                    )
                }
            }
        }
    }

    if (showTransferSheet) {
        TransferBottomSheet(onDismiss = { showTransferSheet = false })
    }
    if (showRechargeSheet) {
        RechargeBottomSheet(onDismiss = { showRechargeSheet = false })
    }
}
