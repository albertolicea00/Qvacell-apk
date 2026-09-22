package com.qvacell.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.UssdCodeGroup
import com.qvacell.app.ui.components.CodeRow
import com.qvacell.app.ui.components.SearchableTopAppBar
import com.qvacell.app.ui.components.rememberCodeActionHandler
// import com.qvacell.app.ui.resolveAndroidIcon // unused while the group icon below is commented out

@Composable
fun CategoryListScreen(categoryId: String, title: String, onOpenCodeOptions: (String) -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val catalog = remember { repository.loadCatalog() }
    val category = remember(categoryId) { catalog.categories.firstOrNull { it.id == categoryId } }

    // Compras-only, and never persisted — it starts off on every fresh visit, same as iOS.
    var isQuickActionEnabled by remember { mutableStateOf(false) }
    val onCodeClick = rememberCodeActionHandler(
        useNoConfirmCode = categoryId == "purchase" && isQuickActionEnabled,
        onOpenCodeOptions = { code -> onOpenCodeOptions(code.id) }
    )

    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    val filteredGroups = remember(category, query) {
        val q = query.trim()
        val groups = category?.groups.orEmpty()
        if (q.isEmpty()) {
            groups
        } else {
            groups.mapNotNull { group ->
                val matches = group.codes.filter {
                    it.title.value.contains(q, ignoreCase = true) ||
                        it.details.value.contains(q, ignoreCase = true)
                }
                if (matches.isEmpty()) null else UssdCodeGroup(name = group.name, icon = group.icon, codes = matches)
            }
        }
    }

    Scaffold(
        topBar = {
            SearchableTopAppBar(
                title = title,
                query = query,
                onQueryChange = { query = it },
                searching = searching,
                onSearchingChange = { searching = it }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (categoryId == "purchase" && isQuickActionEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Acción sin Confirmación activada — las compras se marcan de una vez, sin pedir confirmación",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (categoryId == "purchase") {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Card {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Text("Acción sin Confirmación", modifier = Modifier.weight(1f))
                                    Switch(checked = isQuickActionEnabled, onCheckedChange = { isQuickActionEnabled = it })
                                }
                            }
                            Text(
                                "Marca el código saltando el paso de confirmación de ETECSA, por si acaso confías en la selección y quieres ahorrarte un paso.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                filteredGroups.forEach { group ->
                    item {
                        Column {
                            if (group.name != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    // Temporarily disabled to compare the look without a group icon — codes.json still has it.
                                    // if (group.icon != null) {
                                    //     Icon(
                                    //         imageVector = resolveAndroidIcon(group.icon),
                                    //         contentDescription = null,
                                    //         tint = MaterialTheme.colorScheme.primary,
                                    //         modifier = Modifier.padding(end = 8.dp)
                                    //     )
                                    // }
                                    Text(
                                        group.name.value,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                group.codes.forEachIndexed { index, code ->
                                    CodeRow(
                                        code = code,
                                        onClick = { onCodeClick(code) },
                                        showIcon = categoryId !in setOf("purchase", "sms")
                                    )
                                    if (index != group.codes.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
