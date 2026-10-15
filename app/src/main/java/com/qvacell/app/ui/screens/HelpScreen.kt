package com.qvacell.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qvacell.app.BuildConfig
import com.qvacell.app.R
import com.qvacell.app.ui.components.BackNavigationIcon

private const val REPO_URL = "https://github.com/albertolicea00/Qvacell-apk"
private const val DEVELOPER_URL = "https://github.com/albertolicea00"

@Composable
fun HelpScreen(
    onBack: (() -> Unit)? = null,
    onStartTour: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text(stringResource(R.string.title_help)) },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
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
            HelpSection(stringResource(R.string.help_what_is_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_what_does_app_q),
                    text = stringResource(R.string.help_what_does_app_a)
                )
                if (onStartTour != null) {
                    HelpLinkRow(
                        label = stringResource(R.string.help_take_tour),
                        icon = Icons.Filled.TravelExplore,
                        onClick = onStartTour
                    )
                }
            }

            HelpSection(stringResource(R.string.help_language_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_language_q),
                    text = stringResource(R.string.help_language_a)
                )
                HelpLinkRow(
                    label = stringResource(R.string.help_language_button),
                    icon = Icons.Filled.Language,
                    onClick = { openLanguageSettings(context) }
                )
            }

            HelpSection(stringResource(R.string.help_ussd_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_ussd_q),
                    text = stringResource(R.string.help_ussd_a)
                )
                HelpInfoRow(
                    title = stringResource(R.string.help_ussd_input_q),
                    text = stringResource(R.string.help_ussd_input_a)
                )
                HelpLinkRow(
                    label = stringResource(R.string.help_download_codes),
                    icon = Icons.Filled.Download,
                    onClick = { uriHandler.openUri("$REPO_URL/blob/main/app/src/main/assets/codes.json") }
                )
            }

            if (BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                HelpSection(stringResource(R.string.help_dashboard_title)) {
                    HelpInfoRow(
                        title = stringResource(R.string.help_dashboard_compare_q),
                        text = stringResource(R.string.help_dashboard_compare_a)
                    )
                    if (onOpenSettings != null) {
                        HelpLinkRow(
                            label = stringResource(R.string.help_dashboard_help_link),
                            icon = Icons.Filled.Settings,
                            onClick = onOpenSettings
                        )
                    }
                }
            }

            HelpSection(stringResource(R.string.help_purchase_quick_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_purchase_quick_q),
                    text = stringResource(R.string.help_purchase_quick_a)
                )
            }

            HelpSection(stringResource(R.string.help_reminders_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_reminders_q),
                    text = stringResource(R.string.help_reminders_a)
                )
            }

            HelpSection(stringResource(R.string.help_friends_plan_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_friends_plan_q),
                    text = stringResource(R.string.help_friends_plan_a)
                )
            }

            HelpSection(stringResource(R.string.help_transfer_pin_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_transfer_pin_q),
                    text = stringResource(R.string.help_transfer_pin_a)
                )
            }

            HelpSection(stringResource(R.string.help_sms_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_sms_q),
                    text = stringResource(R.string.help_sms_a)
                )
            }

            HelpSection(stringResource(R.string.help_wifi_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_wifi_q),
                    text = stringResource(R.string.help_wifi_a)
                )
                HelpLinkRow(
                    label = stringResource(R.string.help_download_wifi_json),
                    icon = Icons.Filled.Download,
                    onClick = {
                        uriHandler.openUri("$REPO_URL/blob/main/app/src/main/assets/wifi_navigation_rooms.json")
                    }
                )
            }

            HelpSection(stringResource(R.string.help_directory_title)) {
                HelpInfoRow(
                    title = stringResource(R.string.help_directory_q),
                    text = stringResource(R.string.help_directory_a)
                )
            }

            HelpSection(stringResource(R.string.help_developer)) {
                HelpLinkRow(
                    label = "@albertolicea00",
                    icon = Icons.Filled.Code,
                    onClick = { uriHandler.openUri(DEVELOPER_URL) }
                )
                HelpLinkRow(
                    label = stringResource(R.string.help_source_code),
                    icon = Icons.Filled.Code,
                    onClick = { uriHandler.openUri(REPO_URL) }
                )
            }

            HelpSection(stringResource(R.string.options_version)) {
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable () -> Unit) {
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
private fun HelpInfoRow(title: String, text: String) {
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
private fun HelpLinkRow(label: String, icon: ImageVector, onClick: () -> Unit) {
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

private fun openLanguageSettings(context: android.content.Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
    } else {
        Intent(Settings.ACTION_LOCALE_SETTINGS)
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
