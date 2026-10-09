package com.qvacell.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.qvacell.app.service.ContactEntry
import com.qvacell.app.service.ContactsRepository
import com.qvacell.app.service.DeviceContact
import com.qvacell.app.service.DialService

import com.qvacell.app.ui.components.ContactOptionsSheet
import com.qvacell.app.ui.components.ContactRow
import com.qvacell.app.ui.components.GroupHeader
import com.qvacell.app.ui.components.SearchableTopAppBar
import com.qvacell.app.ui.components.SimSelectorIcon


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsListScreen() {
    val context = LocalContext.current
    val repository = remember { ContactsRepository(context) }
    var selectedEntry by remember { mutableStateOf<ContactEntry?>(null) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            contacts = repository.loadContacts()
            repository.syncWrappedCallers(context, contacts)
        }
    }

    Scaffold(
        topBar = {
            SearchableTopAppBar(
                title = "Contactos",
                query = query,
                onQueryChange = { query = it },
                searching = searching,
                onSearchingChange = { searching = it },
                showSearchAction = hasPermission && contacts.isNotEmpty(),
                extraActions = { SimSelectorIcon() }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!hasPermission) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Contacts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Text(
                        "Sin acceso a tus contactos",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Qvacell necesita acceso a tus contactos para identificar números cubanos y facilitar llamadas desde la app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { launcher.launch(Manifest.permission.READ_CONTACTS) }) {
                        Text("Permitir acceso a contactos")
                    }
                }
            } else if (contacts.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Contacts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Text(
                        "No se encontraron números cubanos",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Solo se muestran contactos con un número en formato cubano (+53). Revisa que tus contactos tengan el código de país correcto.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val entries = remember(contacts) { repository.flattenToEntries(contacts) }
                val filteredEntries = remember(entries, query) {
                    val q = query.trim()
                    if (q.isEmpty()) entries else entries.filter {
                        it.contact.name.contains(q, ignoreCase = true) || it.number.contains(q)
                    }
                }

                if (filteredEntries.isEmpty() && query.isNotBlank()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                } else {
                    val grouped = remember(filteredEntries) {
                        filteredEntries
                            .sortedBy { it.contact.name.lowercase() }
                            .groupBy { entry ->
                                entry.contact.name.firstOrNull()?.uppercaseChar()
                                    ?.takeIf { it.isLetter() }
                                    ?.toString() ?: "#"
                            }
                            .toSortedMap()
                    }

                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        grouped.forEach { (letter, group) ->
                            stickyHeader(key = "header_$letter") { GroupHeader(letter) }
                            items(group, key = { "${it.contact.id}_${it.number}" }) { entry ->
                                ContactRow(
                                    entry = entry,
                                    onClick = { selectedEntry = entry },
                                    onCallCollect = { DialService.dial(context, "*99${entry.number}") },
                                    onCallAnonymous = { DialService.dial(context, "#31#${entry.number}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        ContactOptionsSheet(entry = entry, onDismiss = { selectedEntry = null })
    }
}
