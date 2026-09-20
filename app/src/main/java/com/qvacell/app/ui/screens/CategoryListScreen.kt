package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
fun CategoryListScreen(categoryId: String, title: String) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val catalog = remember { repository.loadCatalog() }
    val category = remember(categoryId) { catalog.categories.firstOrNull { it.id == categoryId } }
    val onCodeClick = rememberCodeActionHandler()

    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            category?.groups?.forEach { group ->
                if (group.name != null) {
                    item {
                        Text(
                            group.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                items(group.codes) { code ->
                    Column {
                        CodeRow(code = code, onClick = { onCodeClick(code) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
