package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
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
import com.qvacell.app.service.DialService

@Composable
fun WifiProvinceListScreen(onProvinceSelected: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val provinces = remember { repository.loadWifiProvinces() }

    Scaffold(topBar = { TopAppBar(title = { Text("Salas de Navegación") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(provinces) { province ->
                ListItem(
                    headlineContent = { Text(province.province) },
                    supportingContent = { Text("${province.rooms.size} salas · ${province.hotspots.size} municipios con WiFi") },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                HorizontalDivider()
                androidx.compose.material3.TextButton(onClick = { onProvinceSelected(province.province) }) {
                    Text("Ver detalles")
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

    Scaffold(topBar = { TopAppBar(title = { Text(provinceName) }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    "Salas de Navegación",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            province?.rooms?.let { rooms ->
                items(rooms) { room ->
                    ListItem(
                        headlineContent = { Text(room.name) },
                        supportingContent = {
                            Text(
                                buildString {
                                    if (!room.address.isNullOrBlank()) append(room.address)
                                    room.positions?.let { append(" · $it puestos") }
                                }
                            )
                        },
                        trailingContent = {
                            androidx.compose.material3.IconButton(onClick = {
                                DialService.openMapsSearch(context, "${room.name} $provinceName")
                            }) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Filled.Place,
                                    contentDescription = "Ver en mapa"
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
            item {
                Text(
                    "Puntos WiFi por Municipio",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            province?.hotspots?.forEach { group ->
                item {
                    Text(
                        group.municipality,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                items(group.spots) { spot ->
                    ListItem(
                        headlineContent = { Text(spot) },
                        trailingContent = {
                            androidx.compose.material3.IconButton(onClick = {
                                DialService.openMapsSearch(context, "$spot $provinceName")
                            }) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Filled.Place,
                                    contentDescription = "Ver en mapa"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
