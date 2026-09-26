package com.qvacell.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Flat two-button footer — plain text, no pill/outline, split evenly by a hairline divider. Used
 * in place of an OutlinedButton/Button pair for every form's Cancelar/confirm row, matching the
 * system-dialog look (e.g. Android's own permission prompts) instead of two separate rounded
 * buttons floating side by side.
 */
@Composable
fun DialogActionRow(
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    HorizontalDivider()
    Row(modifier = modifier.fillMaxWidth().height(48.dp)) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Text(cancelText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        VerticalDivider()
        TextButton(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Text(confirmText, color = MaterialTheme.colorScheme.primary)
        }
    }
}
