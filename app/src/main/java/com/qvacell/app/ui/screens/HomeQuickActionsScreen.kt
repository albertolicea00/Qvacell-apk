package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.UssdCode
import com.qvacell.app.ui.components.QuickActionTileData
import com.qvacell.app.ui.components.QuickActionTileGrid
import com.qvacell.app.ui.components.rememberCodeActionHandler
import com.qvacell.app.ui.sfSymbolToMaterialIcon

private val QUICK_ACTION_CODE_IDS = listOf(
    "main-balance",
    "data-plan",
    "voice-balance",
    "sms-balance",
    "national-recharge-limit",
    "friends-plan",
    "bonus-usd-plans",
    "postpaid-balance"
)

@Composable
fun HomeQuickActionsScreen(
    onOpenTransfer: () -> Unit,
    onOpenRecharge: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val catalog = remember { repository.loadCatalog() }
    val homeCategory = remember { catalog.categories.firstOrNull { it.id == "home" } }
    val balanceGroup = remember { homeCategory?.groups?.firstOrNull { it.name?.value == "Saldo y Planes" } }
    val codesById = remember(balanceGroup) { balanceGroup?.codes.orEmpty().associateBy(UssdCode::id) }
    val onCodeClick = rememberCodeActionHandler()

    val tiles = remember(codesById) {
        QUICK_ACTION_CODE_IDS.mapNotNull { id ->
            codesById[id]?.let { code ->
                QuickActionTileData(
                    label = code.title.value,
                    icon = sfSymbolToMaterialIcon(code.icon),
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
                Card(onClick = onOpenTransfer, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Transferir") },
                        supportingContent = { Text("Envía saldo a otro número") },
                        leadingContent = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) }
                    )
                }
            }
            item {
                Card(onClick = onOpenRecharge, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Recargar") },
                        supportingContent = { Text("Recarga con tarjeta prepago") },
                        leadingContent = { Icon(Icons.Filled.CreditCard, contentDescription = null) }
                    )
                }
            }
        }
    }
}
