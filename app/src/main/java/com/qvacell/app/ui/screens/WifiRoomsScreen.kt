package com.qvacell.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.service.DialService
import com.qvacell.app.ui.components.SearchableTopAppBar

@Composable
fun WifiProvinceListScreen(onProvinceSelected: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val provinces = remember { repository.loadWifiProvinces() }

    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    val filteredProvinces = remember(provinces, query) {
        val q = query.trim()
        if (q.isEmpty()) provinces else provinces.filter { it.province.contains(q, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            SearchableTopAppBar(
                title = "Salas de Navegación",
                query = query,
                onQueryChange = { query = it },
                searching = searching,
                onSearchingChange = { searching = it }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    filteredProvinces.forEachIndexed { index, province ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProvinceSelected(province.province) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(province.province, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${province.rooms.size} salas · ${province.hotspots.size} municipios con WiFi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index != filteredProvinces.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WifiProvinceDetailScreen(provinceName: String) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val province = remember { repository.loadWifiProvinces().firstOrNull { it.province == provinceName } }

    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var expandedMunicipalities by remember { mutableStateOf(setOf<String>()) }
    val filteredRooms = remember(province, query) {
        val q = query.trim()
        val rooms = province?.rooms.orEmpty()
        if (q.isEmpty()) rooms else rooms.filter { it.name.contains(q, ignoreCase = true) }
    }
    val filteredHotspots = remember(province, query) {
        val q = query.trim()
        val groups = province?.hotspots.orEmpty()
        if (q.isEmpty()) {
            groups
        } else {
            groups.mapNotNull { group ->
                val matches = group.spots.filter { it.contains(q, ignoreCase = true) }
                if (matches.isEmpty() && !group.municipality.contains(q, ignoreCase = true)) {
                    null
                } else {
                    group.copy(spots = if (matches.isEmpty()) group.spots else matches)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SearchableTopAppBar(
                title = provinceName,
                subtitle = "Salas de Navegación",
                query = query,
                onQueryChange = { query = it },
                searching = searching,
                onSearchingChange = { searching = it }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (filteredRooms.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            "Salas de Navegación",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Card(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            filteredRooms.forEachIndexed { index, room ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(room.name, style = MaterialTheme.typography.bodyLarge)
                                        val details = buildString {
                                            if (!room.address.isNullOrBlank()) append(room.address)
                                            room.positions?.let { append(" · $it puestos") }
                                        }
                                        if (details.isNotBlank()) {
                                            Text(
                                                details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        DialService.openMapsSearch(context, "${room.name} $provinceName")
                                    }) {
                                        Icon(
                                            Icons.Filled.Place,
                                            contentDescription = "Ver en mapa",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (index != filteredRooms.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
            if (filteredHotspots.isNotEmpty()) {
                item {
                    Text(
                        "Puntos WiFi por Municipio",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                filteredHotspots.forEach { group ->
                    // Auto-expanded while a search query narrows this group down, so matches
                    // are visible without also having to tap it open.
                    val isExpanded = query.isNotBlank() || group.municipality in expandedMunicipalities
                    item {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedMunicipalities = if (group.municipality in expandedMunicipalities) {
                                            expandedMunicipalities - group.municipality
                                        } else {
                                            expandedMunicipalities + group.municipality
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${group.municipality} (${group.spots.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isExpanded) "Contraer" else "Expandir"
                                )
                            }
                            if (isExpanded) {
                                Card(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    group.spots.forEachIndexed { index, spot ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 0.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(spot, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    DialService.openMapsSearch(context, "$spot $provinceName")
                                                },
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Place,
                                                    contentDescription = "Ver en mapa",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (index != group.spots.lastIndex) {
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
}
