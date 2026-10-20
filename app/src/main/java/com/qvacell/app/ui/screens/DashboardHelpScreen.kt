package com.qvacell.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qvacell.app.R
import com.qvacell.app.ui.components.BackNavigationIcon

@Composable
fun DashboardHelpScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text(stringResource(R.string.title_dashboard_help)) },
                navigationIcon = { BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            DashboardHelpSection(stringResource(R.string.help_dashboard_compare_q)) {
                DashboardHelpInfoRow(
                    title = stringResource(R.string.help_dashboard_compare_q),
                    text = stringResource(R.string.help_dashboard_compare_a)
                )
            }

            DashboardHelpSection(stringResource(R.string.dashboard_help_how)) {
                DashboardHelpInfoRow(
                    title = stringResource(R.string.dashboard_help_data_title),
                    text = stringResource(R.string.dashboard_help_data_desc)
                )
                DashboardHelpInfoRow(
                    title = stringResource(R.string.dashboard_help_line_title),
                    text = stringResource(R.string.dashboard_help_line_desc)
                )
                DashboardHelpInfoRow(
                    title = stringResource(R.string.dashboard_help_refresh_title),
                    text = stringResource(R.string.dashboard_help_refresh_desc)
                )
                DashboardHelpInfoRow(
                    title = stringResource(R.string.dashboard_help_cards_title),
                    text = stringResource(R.string.dashboard_help_cards_desc)
                )
                DashboardHelpInfoRow(
                    title = stringResource(R.string.dashboard_help_status_title),
                    text = stringResource(R.string.dashboard_help_status_desc)
                )
            }

            DashboardHelpSection(stringResource(R.string.dashboard_help_capture_title)) {
                DashboardHelpInfoRow(
                    title = stringResource(R.string.dashboard_help_ussd_capture_title),
                    text = stringResource(R.string.dashboard_help_ussd_capture_desc)
                )
                DashboardHelpInfoRow(
                    title = stringResource(R.string.dashboard_help_sms_capture_title),
                    text = stringResource(R.string.dashboard_help_sms_capture_desc)
                )
            }

            DashboardHelpSection(stringResource(R.string.dashboard_help_storage_title)) {
                DashboardHelpInfoRow(
                    title = stringResource(R.string.dashboard_help_storage_local_title),
                    text = stringResource(R.string.dashboard_help_storage_desc)
                )
                DashboardHelpInfoRow(
                    title = stringResource(R.string.dashboard_help_delete_title),
                    text = stringResource(R.string.dashboard_help_delete_desc)
                )
            }

            DashboardHelpSection(stringResource(R.string.dashboard_help_settings_title)) {
                DashboardHelpLinkRow(
                    label = stringResource(R.string.dashboard_help_open_settings),
                    icon = Icons.Filled.Settings,
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun DashboardHelpSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun DashboardHelpInfoRow(title: String, text: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun DashboardHelpLinkRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
            Text(label, color = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider()
}
