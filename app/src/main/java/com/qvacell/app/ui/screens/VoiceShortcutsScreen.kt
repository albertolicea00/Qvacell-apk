package com.qvacell.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.qvacell.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qvacell.app.ui.components.BackNavigationIcon

@Composable
fun VoiceShortcutsScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text(stringResource(R.string.voice_shortcuts_title)) },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.voice_shortcuts_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.voice_shortcuts_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // ¿Cómo funciona?
            SectionHeader(stringResource(R.string.voice_shortcuts_how_it_works))
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow(
                        icon = Icons.Filled.AutoAwesome,
                        text = stringResource(R.string.voice_shortcuts_info1)
                    )
                    InfoRow(
                        icon = Icons.Filled.Security,
                        text = stringResource(R.string.voice_shortcuts_info2)
                    )
                }
            }

            // Llamar Oculto o por Cobrar
            SectionHeader(stringResource(R.string.voice_call_section))
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    VoicePhraseRow(
                        phrase = stringResource(R.string.voice_collect_phrase),
                        subtitle = stringResource(R.string.voice_collect_desc)
                    )
                    PhraseDivider()
                    VoicePhraseRow(
                        phrase = stringResource(R.string.voice_hidden_phrase),
                        subtitle = stringResource(R.string.voice_hidden_desc)
                    )
                }
            }

            // Consultar Saldo
            SectionHeader(stringResource(R.string.voice_check_section))
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    VoicePhraseRow(
                        phrase = stringResource(R.string.voice_balance_phrase),
                        subtitle = stringResource(R.string.voice_balance_desc)
                    )
                    PhraseDivider()
                    VoicePhraseRow(
                        phrase = stringResource(R.string.voice_bonuses_phrase),
                        subtitle = stringResource(R.string.voice_bonuses_desc)
                    )
                }
            }

            // Comprar Planes
            SectionHeader(stringResource(R.string.voice_buy_section))
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    VoicePhraseRow(
                        phrase = stringResource(R.string.voice_data_phrase),
                        subtitle = stringResource(R.string.voice_data_desc)
                    )
                    PhraseDivider()
                    VoicePhraseRow(
                        phrase = stringResource(R.string.voice_combo_phrase),
                        subtitle = stringResource(R.string.voice_combo_desc)
                    )
                    PhraseDivider()
                    VoicePhraseRow(
                        phrase = stringResource(R.string.voice_sms_phrase),
                        subtitle = stringResource(R.string.voice_sms_desc)
                    )
                }
            }
            Text(
                stringResource(R.string.voice_purchase_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Atajos del lanzador
            SectionHeader(stringResource(R.string.voice_shortcuts_launcher_title))
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Text(
                    stringResource(R.string.voice_shortcuts_launcher_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Open settings button
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.voice_shortcuts_open_settings))
            }
            Text(
                stringResource(R.string.voice_shortcuts_settings_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VoicePhraseRow(phrase: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                phrase,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, top = 2.dp)
        )
    }
}

@Composable
private fun PhraseDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
