package com.qvacell.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.qvacell.app.R
import com.qvacell.app.ui.components.BackNavigationIcon

@Composable
fun RadioNetworkHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text(stringResource(R.string.options_force_lte_title)) },
                navigationIcon = { BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.radio_network_help_intro),
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(R.string.radio_network_help_warning),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            val restoreText = stringResource(R.string.radio_network_help_restore)
            val emphasizedOptions = listOf("“LTE/TD-SCDMA/UMTS”", "“LTE/TD-SCDMA/WCDMA”")
            val annotatedRestoreText = buildAnnotatedString {
                append(restoreText)
                emphasizedOptions.forEach { option ->
                    val start = restoreText.indexOf(option)
                    if (start >= 0) addStyle(
                        SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                        start,
                        start + option.length
                    )
                }
            }
            Text(annotatedRestoreText, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.radio_network_help_device_support),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                onClick = {
                    if (!openRadioInfoMenu(context)) {
                        Toast.makeText(context, R.string.radio_network_help_unavailable, Toast.LENGTH_LONG).show()
                    }
                }
            ) {
                Text(
                    stringResource(R.string.radio_network_help_open_menu),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

private fun openRadioInfoMenu(context: android.content.Context): Boolean {
    val candidates = listOf(
        ComponentName("com.android.phone", "com.android.phone.settings.RadioInfo"),
        ComponentName("com.android.phone", "com.android.phone.RadioInfo")
    )
    candidates.forEach { component ->
        try {
            context.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the other common Android component name.
        } catch (_: SecurityException) {
            // Some manufacturers block third-party launches into this hidden screen.
        }
    }
    return false
}
