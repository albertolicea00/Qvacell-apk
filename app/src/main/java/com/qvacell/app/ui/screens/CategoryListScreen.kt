package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.UssdCodeGroup
import com.qvacell.app.ui.components.CodeRow
import com.qvacell.app.ui.components.SearchableTopAppBar
import com.qvacell.app.ui.components.rememberCodeActionHandler

@Composable
fun CategoryListScreen(categoryId: String, title: String) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val catalog = remember { repository.loadCatalog() }
    val category = remember(categoryId) { catalog.categories.firstOrNull { it.id == categoryId } }
    val onCodeClick = rememberCodeActionHandler()

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
                if (matches.isEmpty()) null else UssdCodeGroup(name = group.name, codes = matches)
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
            LazyColumn {
                filteredGroups.forEach { group ->
                    if (group.name != null) {
                        item {
                            Text(
                                group.name.value,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(group.codes) { code ->
                        Column {
                            CodeRow(code = code, onClick = { onCodeClick(code) }, showIcon = categoryId != "purchase")
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
