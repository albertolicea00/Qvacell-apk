package com.qvacell.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.model.UssdCode
import com.qvacell.app.model.UssdCodeGroup
import com.qvacell.app.ui.components.CodeOptionsSheet
import com.qvacell.app.ui.components.CodeRow
import com.qvacell.app.ui.components.GroupHeader

import com.qvacell.app.ui.components.SearchableTopAppBar
import com.qvacell.app.ui.components.SimSelectorIcon
import com.qvacell.app.ui.components.rememberCodeActionHandler
// import com.qvacell.app.ui.resolveAndroidIcon // unused while the group icon below is commented out

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryListScreen(categoryId: String, title: String, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val catalog = remember { repository.loadCatalog() }
    val category = remember(categoryId) { catalog.categories.firstOrNull { it.id == categoryId } }

    val settings = remember { SettingsDataStore(context) }
    val quickActionEnabled by settings.quickPurchaseNoConfirmDefault.collectAsStateWithLifecycle(initialValue = false)
    // Session-only toggle — resets to false on every app start, not persisted.
    var sessionNoConfirm by remember { mutableStateOf(false) }
    var codeForOptionsSheet by remember { mutableStateOf<UssdCode?>(null) }
    val onCodeClick = rememberCodeActionHandler(
        useNoConfirmCode = categoryId == "purchase" && (quickActionEnabled || sessionNoConfirm),
        isPurchaseCategory = categoryId == "purchase",
        onOpenCodeOptions = { code -> codeForOptionsSheet = code }
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
                onSearchingChange = { searching = it },
                showSearchAction = categoryId != "purchase",
                onBack = onBack,
                extraActions = { if (onBack == null) SimSelectorIcon() }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (categoryId == "purchase" && !quickActionEnabled) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Marcar sin Confirmación",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Marca el código saltando el paso de confirmación de ETECSA",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = quickActionEnabled || sessionNoConfirm,
                            onCheckedChange = { sessionNoConfirm = it },
                            enabled = !quickActionEnabled,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            if (filteredGroups.isEmpty() && query.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Sin resultados para \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else if (categoryId == "helplines") {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    filteredGroups.forEach { group ->
                        if (group.name != null) {
                            stickyHeader(key = "header_${group.name.value}") {
                                GroupHeader(group.name.value)
                            }
                        }
                        items(group.codes, key = { it.id }) { code ->
                            CodeRow(
                                code = code,
                                onClick = { onCodeClick(code) },
                                contactStyle = true
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredGroups.forEach { group ->
                        Column {
                            if (group.name != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
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
                                        showIcon = categoryId !in setOf("purchase", "sms"),
                                        showDescription = categoryId != "purchase",
                                        plainPrice = categoryId in setOf("purchase", "sms")
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

    codeForOptionsSheet?.let { code ->
        CodeOptionsSheet(code = code, onDismiss = { codeForOptionsSheet = null })
    }
}
