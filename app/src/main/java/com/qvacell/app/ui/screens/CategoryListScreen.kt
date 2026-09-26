package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.model.UssdCode
import com.qvacell.app.model.UssdCodeGroup
import com.qvacell.app.ui.components.CodeOptionsSheet
import com.qvacell.app.ui.components.CodeRow
import com.qvacell.app.ui.components.ConnectionBanner
import com.qvacell.app.ui.components.QuickPurchaseWarningBanner
import com.qvacell.app.ui.components.SearchableTopAppBar
import com.qvacell.app.ui.components.rememberCodeActionHandler
// import com.qvacell.app.ui.resolveAndroidIcon // unused while the group icon below is commented out

@Composable
fun CategoryListScreen(categoryId: String, title: String, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val catalog = remember { repository.loadCatalog() }
    val category = remember(categoryId) { catalog.categories.firstOrNull { it.id == categoryId } }

    // Persisted in Ajustes › Preferencias › "Acción sin Confirmación" — no more per-visit
    // override here, it just follows that setting.
    val settings = remember { SettingsDataStore(context) }
    val quickActionEnabled by settings.quickPurchaseNoConfirmDefault.collectAsStateWithLifecycle(initialValue = false)
    val showNetworkStatus by settings.showNetworkStatus.collectAsStateWithLifecycle(initialValue = false)
    var codeForOptionsSheet by remember { mutableStateOf<UssdCode?>(null) }
    val onCodeClick = rememberCodeActionHandler(
        useNoConfirmCode = categoryId == "purchase" && quickActionEnabled,
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
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (showNetworkStatus) {
                ConnectionBanner()
            }
            if (categoryId == "purchase" && quickActionEnabled) {
                QuickPurchaseWarningBanner()
            }
            // Column+verticalScroll instead of LazyColumn: a LazyColumn always stretches to fill
            // the viewport, leaving a permanent blank gap below the last group whenever the
            // filtered content doesn't fill the screen — catalogs here are small enough that
            // virtualization isn't worth that tradeoff.
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

    codeForOptionsSheet?.let { code ->
        CodeOptionsSheet(code = code, onDismiss = { codeForOptionsSheet = null })
    }
}
