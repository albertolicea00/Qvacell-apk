package com.qvacell.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.ui.components.PriceChip
import com.qvacell.app.ui.components.dialCodeOption

/**
 * Full-screen list of a code's variant/option labels — e.g. Servicios por SMS's "Pelota Cubana"
 * (Resultados/Posiciones) or "Horóscopo" (one row per sign) — each shown like a regular
 * CodeRow-style row with the parent code's price chip, instead of a plain AlertDialog picker.
 */
@Composable
fun CodeOptionsScreen(codeId: String) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val code = remember(codeId) { repository.findCodeById(codeId) }
    val labels = remember(code) { code?.variants?.map { it.label.value } ?: code?.options.orEmpty() }

    Scaffold(topBar = { TopAppBar(title = { Text(code?.title?.value ?: "") }) }) { padding ->
        if (code != null) {
            LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    Card(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        labels.forEachIndexed { index, label ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dialCodeOption(context, code, label) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                if (code.price != null) {
                                    PriceChip(price = code.price)
                                }
                            }
                            if (index != labels.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
