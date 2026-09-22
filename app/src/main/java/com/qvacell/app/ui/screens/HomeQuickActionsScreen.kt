package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.qvacell.app.ui.components.CodeRow
import com.qvacell.app.ui.components.rememberCodeActionHandler

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
    val onCodeClick = rememberCodeActionHandler()

    Scaffold(topBar = { TopAppBar(title = { Text("Qvacell") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
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
            item {
                Text(
                    "Saldo y Planes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            balanceGroup?.codes?.let { codes ->
                items(codes) { code ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
                        CodeRow(code = code, onClick = { onCodeClick(code) })
                    }
                }
            }
        }
    }
}
