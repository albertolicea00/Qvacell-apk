package com.qvacell.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qvacell.app.R
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.qvacell.app.service.ContactEntry
import com.qvacell.app.service.ContactsRepository
import com.qvacell.app.service.AppTelemetry
import com.qvacell.app.service.DeviceContact

import com.qvacell.app.ui.components.ContactOptionsSheet
import com.qvacell.app.ui.components.ContactRow
import com.qvacell.app.ui.components.GroupHeader
import com.qvacell.app.ui.components.SearchableTopAppBar
import com.qvacell.app.ui.components.SimSelectorIcon
import com.qvacell.app.ui.components.rememberContactCallAction
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsListScreen() {
    val context = LocalContext.current
    val repository = remember { ContactsRepository(context) }
    val scope = rememberCoroutineScope()
    val callContact = rememberContactCallAction()
    var selectedEntry by remember { mutableStateOf<ContactEntry?>(null) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(hasPermission) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            val loadTrace = AppTelemetry.startTrace(context, "contacts_load")
            try {
                contacts = repository.loadContacts()
            } finally {
                loadTrace?.stop()
                isLoading = false
            }
        } else {
            contacts = emptyList()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            SearchableTopAppBar(
                title = stringResource(R.string.title_contacts),
                query = query,
                onQueryChange = { query = it },
                searching = searching,
                onSearchingChange = { searching = it },
                showSearchAction = hasPermission && !isLoading && contacts.isNotEmpty(),
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
                        stringResource(R.string.contacts_no_permission_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.contacts_no_permission_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { launcher.launch(Manifest.permission.READ_CONTACTS) }) {
                        Text(stringResource(R.string.contacts_allow_permission))
                    }
                }
            } else if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.contacts_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        stringResource(R.string.contacts_no_cuban_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.contacts_no_cuban_desc),
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
                            stringResource(R.string.no_results_for, query),
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
                    val showScrollToTop by remember(listState) {
                        derivedStateOf { listState.firstVisibleItemIndex > 3 }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        grouped.forEach { (letter, group) ->
                            stickyHeader(key = "header_$letter") { GroupHeader(letter) }
                            // A contact may contain the same normalized number more than once.
                            itemsIndexed(group, key = { index, entry -> "${entry.contact.id}_${entry.number}_$index" }) { _, entry ->
                                ContactRow(
                                    entry = entry,
                                    onClick = { selectedEntry = entry },
                                    onCallCollect = { callContact("*99${entry.number}") },
                                    onCallAnonymous = { callContact("#31#${entry.number}") }
                                )
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = showScrollToTop,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    val visibleItemSize = listState.layoutInfo.visibleItemsInfo
                                        .maxOfOrNull { it.size } ?: 80
                                    val distance = (listState.firstVisibleItemIndex + 1) *
                                        visibleItemSize * 1.5f + listState.firstVisibleItemScrollOffset
                                    listState.animateScrollBy(
                                        -distance,
                                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                    )
                                    if (listState.firstVisibleItemIndex > 0) listState.scrollToItem(0)
                                }
                            },
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.action_scroll_to_top))
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
