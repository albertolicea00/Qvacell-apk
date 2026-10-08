package com.qvacell.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fax
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.qvacell.app.service.DirectoryEntry

@Composable
fun DirectoryEntryRow(entry: DirectoryEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    ListItem(
        headlineContent = {
            Text(entry.displayName, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                entry.number,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            if (entry.isMobile) {
                Icon(
                    Icons.Filled.PhoneAndroid,
                    contentDescription = "Móvil",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Filled.Fax,
                    contentDescription = "Fijo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        trailingContent = {
            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("número", entry.number))
                Toast.makeText(context, "Número copiado", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copiar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entry.number}"))
            context.startActivity(intent)
        }
    )
}
