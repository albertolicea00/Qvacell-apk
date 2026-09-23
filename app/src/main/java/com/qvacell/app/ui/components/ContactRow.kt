package com.qvacell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qvacell.app.service.DeviceContact

/**
 * One row per contact. Swiping left dials *99 (collect call), swiping right dials #31# (hidden
 * caller ID) — same as the native Samsung Contacts app's swipe-to-call — while tapping the row
 * fires [onClick] (used to open the full options sheet, see `ContactOptionsSheet`). Neither swipe
 * actually removes the row: `confirmValueChange` always returns false so it just fires the call
 * and springs back.
 */
@Composable
fun ContactRow(
    contact: DeviceContact,
    onClick: () -> Unit,
    onCallCollect: () -> Unit,
    onCallAnonymous: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> onCallCollect()
                SwipeToDismissBoxValue.StartToEnd -> onCallAnonymous()
                SwipeToDismissBoxValue.Settled -> {}
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val (color, icon, label, alignment) = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd ->
                    SwipeBackground(MaterialTheme.colorScheme.secondary, Icons.Filled.Security, "Oculto", Alignment.CenterStart)
                SwipeToDismissBoxValue.EndToStart ->
                    SwipeBackground(MaterialTheme.colorScheme.primary, Icons.Filled.Call, "*99", Alignment.CenterEnd)
                SwipeToDismissBoxValue.Settled ->
                    SwipeBackground(Color.Transparent, null, "", Alignment.Center)
            }
            Box(
                modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = Color.White)
                        Text(label, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    contact.cubanNumbers.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class SwipeBackground(
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val label: String,
    val alignment: Alignment
)
