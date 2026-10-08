package com.qvacell.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qvacell.app.service.DirectoryDatabase
import com.qvacell.app.service.DirectoryEntry
import com.qvacell.app.service.ImportResult
import com.qvacell.app.ui.components.BackNavigationIcon
import com.qvacell.app.ui.components.DirectoryEntryRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MIN_QUERY_LENGTH = 3

@Composable
fun DirectorySearchScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val database = remember { DirectoryDatabase(context) }
    val scope = rememberCoroutineScope()
    var imported by remember { mutableStateOf(database.isImported()) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DirectoryEntry>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                val result = database.importFrom(uri)
                isImporting = false
                when (result) {
                    is ImportResult.Success -> {
                        imported = true
                        Toast.makeText(context, "Base de datos importada", Toast.LENGTH_SHORT).show()
                    }
                    is ImportResult.Truncated -> {
                        Toast.makeText(
                            context,
                            "Copia incompleta (${result.copied / 1_000_000}MB de ${result.expected / 1_000_000}MB) — inténtalo de nuevo",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is ImportResult.InvalidSchema -> {
                        Toast.makeText(context, "Archivo no reconocido como base de datos válida", Toast.LENGTH_LONG).show()
                    }
                    is ImportResult.Error -> {
                        Toast.makeText(context, "Error al importar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(query, imported) {
        if (!imported || query.length < MIN_QUERY_LENGTH) {
            results = emptyList()
            hasSearched = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(400)
        results = database.search(query)
        isSearching = false
        hasSearched = true
        if (!database.isImported()) {
            imported = false
            results = emptyList()
            Toast.makeText(context, "Base de datos corrupta, importa de nuevo", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text("Buscar en BBDD") },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!imported) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "Sin base de datos",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            "Importa un archivo .db para buscar números",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = { importLauncher.launch("*/*") },
                            enabled = !isImporting,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Importar base de datos")
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Número") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                when {
                    isSearching -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    query.length < MIN_QUERY_LENGTH -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Storage,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    "Buscar en Base de Datos",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                Text(
                                    "Escribe un número para buscar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    hasSearched && results.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    "Sin resultados para \"$query\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 16.dp, start = 32.dp, end = 32.dp)
                                )
                                Text(
                                    "Revisa el número o intenta otra búsqueda",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn {
                            items(results) { entry ->
                                DirectoryEntryRow(entry = entry, onClick = {})
                            }
                        }
                    }
                }
            }
        }
    }
}
