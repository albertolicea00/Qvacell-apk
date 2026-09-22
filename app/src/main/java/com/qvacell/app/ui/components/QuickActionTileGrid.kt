package com.qvacell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class QuickActionTileData(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private const val COLUMNS = 4

/**
 * Square icon+label tiles laid out 4-per-row, wrapping to as many rows as needed —
 * mirrors the iOS Home screen's "Consultas" quick-action grid.
 */
@Composable
fun QuickActionTileGrid(tiles: List<QuickActionTileData>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { tile ->
                    QuickActionTile(tile = tile, modifier = Modifier.weight(1f))
                }
                repeat(COLUMNS - row.size) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(tile: QuickActionTileData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = tile.onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tile.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            tile.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
