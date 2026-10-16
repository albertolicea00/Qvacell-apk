package com.qvacell.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.qvacell.app.BuildConfig
import com.qvacell.app.R
import com.qvacell.app.service.UpdateInfo

@Composable
fun UpdateAvailableDialog(info: UpdateInfo, targetMode: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val downloadUrl = when (targetMode) {
        "playstore" -> info.storeApkUrl ?: info.htmlUrl
        else -> info.unlockedApkUrl ?: info.htmlUrl
    }
    val title = if (info.isFlavorUpgrade) {
        stringResource(R.string.options_update_unlocked_title)
    } else {
        stringResource(R.string.options_update_available_title_new)
    }
    val description = if (info.isFlavorUpgrade) {
        stringResource(R.string.options_update_upgrade_desc, BuildConfig.VERSION_NAME, info.versionName)
    } else {
        stringResource(R.string.options_update_new_desc, BuildConfig.VERSION_NAME, info.versionName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
            }) {
                Text(stringResource(R.string.options_update_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.options_update_not_now))
            }
        }
    )
}
