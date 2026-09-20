package com.qvacell.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.service.DirectoryDatabase
import com.qvacell.app.service.DirectoryEntry
import com.qvacell.app.ui.components.DirectoryEntryRow
import kotlinx.coroutines.launch

@Composable
fun DirectorySearchScreen() {
    val context = LocalContext.current
    val database = remember { DirectoryDatabase(context) }
    val scope = rememberCoroutineScope()
    var imported by remember { mutableStateOf(database.isImported()) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DirectoryEntry>>(emptyList()) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch { imported = database.importFrom(it) }
        }
    }

    LaunchedEffect(query, imported) {
        results = if (imported && query.isNotBlank()) database.search(query) else emptyList()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Búsqueda en Base de Datos") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (!imported) {
                Text("Importa un archivo .db para buscar números.")
                Button(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Importar base de datos") }
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar por prefijo de número") },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(results) { entry -> DirectoryEntryRow(entry = entry, onClick = {}) }
                }
            }
        }
    }
}
